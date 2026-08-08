package eightbitlab.com.blurview;

import android.view.ViewGroup;

import androidx.annotation.NonNull;

/**
 * Installs {@link GlassBlurController} into a {@link BlurView} by writing the
 * package-private {@code blurController} field — same package as the library,
 * no fork required.
 */
public final class GlassSetup {

    private GlassSetup() {
    }

    @NonNull
    public static BlurViewFacade setupGlass(@NonNull BlurView view,
                                            @NonNull ViewGroup rootView,
                                            @NonNull BlurAlgorithm algorithm) {
        view.blurController.destroy();
        int overlayColor = PreDrawBlurController.TRANSPARENT;
        // Preserve overlay from the previous controller if BlurView already has one via XML.
        // BlurView stores overlayColor privately; PreDrawBlurController.TRANSPARENT (0) is the default.
        // The view's XML attr is applied by reading through a fresh controller — pass 0 and let
        // callers use setOverlayColor when needed (StatisticsFragment).
        try {
            java.lang.reflect.Field field = BlurView.class.getDeclaredField("overlayColor");
            field.setAccessible(true);
            overlayColor = field.getInt(view);
        } catch (Throwable ignored) {
            overlayColor = PreDrawBlurController.TRANSPARENT;
        }
        GlassBlurController controller = new GlassBlurController(view, rootView, overlayColor, algorithm);
        view.blurController = controller;
        return controller;
    }
}
