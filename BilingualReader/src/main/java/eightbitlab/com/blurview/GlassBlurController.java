package eightbitlab.com.blurview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import br.com.fenix.bilingualreader.view.components.GlassRenderScheduler;

/**
 * Blur controller that consults {@link GlassRenderScheduler} before each update so
 * higher-priority animations can skip the expensive rootView.draw + blur pass
 * without blocking the frame. Keeps the last blurred bitmap when skipped.
 */
public final class GlassBlurController implements BlurController {

    private float blurRadius = DEFAULT_BLUR_RADIUS;

    private final BlurAlgorithm blurAlgorithm;
    private BlurViewCanvas internalCanvas;
    private Bitmap internalBitmap;

    final View blurView;
    private int overlayColor;
    private final ViewGroup rootView;
    private final int[] rootLocation = new int[2];
    private final int[] blurViewLocation = new int[2];

    private final ViewTreeObserver.OnPreDrawListener drawListener = new ViewTreeObserver.OnPreDrawListener() {
        @Override
        public boolean onPreDraw() {
            if (GlassRenderScheduler.shouldSkip(GlassBlurController.this)) {
                return true;
            }
            GlassRenderScheduler.beginBlurTrace();
            long t0 = System.nanoTime();
            try {
                updateBlur();
            } finally {
                GlassRenderScheduler.recordCost(GlassBlurController.this, System.nanoTime() - t0);
                GlassRenderScheduler.endBlurTrace();
            }
            return true;
        }
    };

    private boolean blurEnabled = true;
    private boolean initialized;
    private boolean listening;

    @Nullable
    private Drawable frameClearDrawable;
    private int frameClearColor = Color.TRANSPARENT;
    private boolean useEraseColor = true;

    public GlassBlurController(@NonNull View blurView,
                               @NonNull ViewGroup rootView,
                               @ColorInt int overlayColor,
                               BlurAlgorithm algorithm) {
        this.rootView = rootView;
        this.blurView = blurView;
        this.overlayColor = overlayColor;
        this.blurAlgorithm = algorithm;
        if (algorithm instanceof RenderEffectBlur) {
            // noinspection NewApi
            ((RenderEffectBlur) algorithm).setContext(blurView.getContext());
        }

        GlassRenderScheduler.INSTANCE.register(this, (BlurView) blurView);

        int measuredWidth = blurView.getMeasuredWidth();
        int measuredHeight = blurView.getMeasuredHeight();
        init(measuredWidth, measuredHeight);
    }

    void init(int measuredWidth, int measuredHeight) {
        setBlurAutoUpdate(false);
        SizeScaler sizeScaler = new SizeScaler(blurAlgorithm.scaleFactor());
        if (sizeScaler.isZeroSized(measuredWidth, measuredHeight)) {
            blurView.setWillNotDraw(true);
            return;
        }

        SizeScaler.Size bitmapSize = sizeScaler.scale(measuredWidth, measuredHeight);
        if (internalBitmap != null
                && internalBitmap.getWidth() == bitmapSize.width
                && internalBitmap.getHeight() == bitmapSize.height) {
            blurView.setWillNotDraw(false);
            initialized = true;
            return;
        }

        blurView.setWillNotDraw(false);
        internalBitmap = Bitmap.createBitmap(bitmapSize.width, bitmapSize.height, blurAlgorithm.getSupportedBitmapConfig());
        internalCanvas = new BlurViewCanvas(internalBitmap);
        initialized = true;
        updateBlur();
    }

    void updateBlur() {
        if (!blurEnabled || !initialized) {
            return;
        }

        if (useEraseColor) {
            internalBitmap.eraseColor(frameClearColor);
        } else if (frameClearDrawable == null) {
            internalBitmap.eraseColor(Color.TRANSPARENT);
        } else {
            frameClearDrawable.setBounds(0, 0, internalBitmap.getWidth(), internalBitmap.getHeight());
            frameClearDrawable.draw(internalCanvas);
        }

        internalCanvas.save();
        setupInternalCanvasMatrix();
        rootView.draw(internalCanvas);
        internalCanvas.restore();

        blurAndSave();
    }

    private void setupInternalCanvasMatrix() {
        rootView.getLocationOnScreen(rootLocation);
        blurView.getLocationOnScreen(blurViewLocation);

        int left = blurViewLocation[0] - rootLocation[0];
        int top = blurViewLocation[1] - rootLocation[1];

        float scaleFactorH = (float) blurView.getHeight() / internalBitmap.getHeight();
        float scaleFactorW = (float) blurView.getWidth() / internalBitmap.getWidth();

        float scaledLeftPosition = -left / scaleFactorW;
        float scaledTopPosition = -top / scaleFactorH;

        internalCanvas.translate(scaledLeftPosition, scaledTopPosition);
        internalCanvas.scale(1 / scaleFactorW, 1 / scaleFactorH);
    }

    @Override
    public boolean draw(Canvas canvas) {
        if (!blurEnabled || !initialized) {
            return true;
        }
        if (canvas instanceof BlurViewCanvas) {
            return false;
        }

        float scaleFactorH = (float) blurView.getHeight() / internalBitmap.getHeight();
        float scaleFactorW = (float) blurView.getWidth() / internalBitmap.getWidth();

        canvas.save();
        canvas.scale(scaleFactorW, scaleFactorH);
        blurAlgorithm.render(canvas, internalBitmap);
        canvas.restore();
        if (overlayColor != PreDrawBlurController.TRANSPARENT) {
            canvas.drawColor(overlayColor);
        }
        return true;
    }

    private void blurAndSave() {
        internalBitmap = blurAlgorithm.blur(internalBitmap, blurRadius);
        if (!blurAlgorithm.canModifyBitmap()) {
            internalCanvas.setBitmap(internalBitmap);
        }
    }

    @Override
    public void updateBlurViewSize() {
        int measuredWidth = blurView.getMeasuredWidth();
        int measuredHeight = blurView.getMeasuredHeight();
        init(measuredWidth, measuredHeight);
    }

    @Override
    public void destroy() {
        setBlurAutoUpdate(false);
        stopListening();
        GlassRenderScheduler.INSTANCE.unregister(this);
        blurAlgorithm.destroy();
        initialized = false;
    }

    @Override
    public BlurViewFacade setBlurRadius(float radius) {
        this.blurRadius = radius;
        return this;
    }

    @Override
    public BlurViewFacade setFrameClearDrawable(@Nullable Drawable frameClearDrawable) {
        this.frameClearDrawable = frameClearDrawable;
        if (frameClearDrawable instanceof ColorDrawable) {
            frameClearColor = ((ColorDrawable) frameClearDrawable).getColor();
            useEraseColor = true;
        } else if (frameClearDrawable == null) {
            frameClearColor = Color.TRANSPARENT;
            useEraseColor = true;
        } else {
            useEraseColor = false;
        }
        return this;
    }

    @Override
    public BlurViewFacade setBlurEnabled(boolean enabled) {
        this.blurEnabled = enabled;
        if (enabled) {
            // Keep listener so on-demand / continuous updates can run.
            // Continuous vs on-demand is controlled exclusively by setBlurAutoUpdate.
            startListening();
        } else {
            stopListening();
            GlassRenderScheduler.INSTANCE.setContinuous(this, false);
        }
        blurView.invalidate();
        return this;
    }

    /**
     * Maps to continuous vs on-demand mode in {@link GlassRenderScheduler}.
     * Existing call sites keep working: true during scroll, false when idle.
     */
    @Override
    public BlurViewFacade setBlurAutoUpdate(final boolean enabled) {
        GlassRenderScheduler.INSTANCE.setContinuous(this, enabled);
        if (blurEnabled) {
            startListening();
            if (enabled) {
                // Nudge an immediate update when entering continuous mode
                GlassRenderScheduler.INSTANCE.requestUpdate((BlurView) blurView);
            }
        } else {
            stopListening();
        }
        return this;
    }

    private void startListening() {
        if (listening) return;
        rootView.getViewTreeObserver().removeOnPreDrawListener(drawListener);
        blurView.getViewTreeObserver().removeOnPreDrawListener(drawListener);
        rootView.getViewTreeObserver().addOnPreDrawListener(drawListener);
        if (rootView.getWindowId() != blurView.getWindowId()) {
            blurView.getViewTreeObserver().addOnPreDrawListener(drawListener);
        }
        listening = true;
    }

    private void stopListening() {
        if (!listening) return;
        rootView.getViewTreeObserver().removeOnPreDrawListener(drawListener);
        blurView.getViewTreeObserver().removeOnPreDrawListener(drawListener);
        listening = false;
    }

    @Override
    public BlurViewFacade setOverlayColor(int overlayColor) {
        if (this.overlayColor != overlayColor) {
            this.overlayColor = overlayColor;
            blurView.invalidate();
        }
        return this;
    }

    /** One-shot blur refresh request (used by requestUpdate path). */
    public void requestUpdate() {
        if (!blurEnabled) return;
        startListening();
        GlassRenderScheduler.INSTANCE.requestUpdate((BlurView) blurView);
    }
}
