package br.com.ebook.foobnix.pdf.info.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.graphics.ColorUtils;

import br.com.ebook.foobnix.android.utils.LOG;
import br.com.ebook.foobnix.pdf.info.model.OutlineLinkWrapper;
import br.com.ebook.foobnix.pdf.info.wrapper.AppState;
import br.com.ebook.foobnix.android.utils.Dips;
import br.com.ebook.foobnix.ext.Fb2Extractor;
import br.com.ebook.foobnix.pdf.info.wrapper.MagicHelper;

import java.util.ArrayList;
import java.util.List;

public class ProgressDraw extends View {

    private static final int max_count = 5;
    private static final int ALPHA = 200;
    Paint paint = new Paint();
    {
        paint.setColor(Color.DKGRAY);
        paint.setStyle(Style.FILL);
        paint.setStrokeWidth(Dips.dpToPx(1));
        paint.setAntiAlias(true);
        paint.setDither(true);
    }

    Paint paint1 = new Paint();
    {
        paint1.setColor(Color.DKGRAY);
        paint1.setStyle(Style.FILL);
        paint1.setStrokeWidth(Dips.dpToPx(1));
        paint1.setAntiAlias(true);
        paint1.setDither(true);
    }

    List<OutlineLinkWrapper> dividers = new ArrayList<OutlineLinkWrapper>();
    int pageCount;
    int progress;
    int color = Color.BLACK;

    public ProgressDraw(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClickable(true);
    }

    int level0count, level1count, level2count, deep = 0;

    public void updateDivs(List<OutlineLinkWrapper> dividers) {
        this.dividers = dividers;
        level0count = 0;
        level1count = 0;
        if (dividers == null || dividers.isEmpty()) {
            return;
        }

        int first = dividers.get(0).level;
        LOG.d("deep first", first);

        for (OutlineLinkWrapper link : dividers) {
            if (link.level == 0 + first) {
                level0count++;
            }
            if (link.level == 1 + first) {
                level1count++;
            }
            if (link.level == 2 + first) {
                level2count++;
            }
        }

        if (level0count >= max_count) {
            deep = 0;
        } else if (level1count >= max_count) {
            deep = 1;
        } else if (level2count >= max_count) {
            deep = 2;
        } else {
            deep = 3;
        }

        deep += first;

        invalidate();
    }

    public void updateProgress(int progress) {
        LOG.d("updateProgress", progress);
        this.progress = progress;
        invalidate();
    }

    public void updatePageCount(int pageCount) {
        LOG.d("updatePageCount", pageCount);
        this.pageCount = pageCount - 1;
        invalidate();
    }

    public void updateColor(int color) {
        this.color = ColorUtils.setAlphaComponent(color, ALPHA);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        try {
            int titleColor = AppState.get().isDayNotInvert ? MagicHelper.otherColor(AppState.get().colorDayBg, -0.05f) : MagicHelper.otherColor(AppState.get().colorNigthBg, 0.05f);
            // int titleColor = AppState.get().isInvert ? Color.WHITE :
            // Color.BLACK;
            if (AppState.get().isAlwaysOpenAsMagazine) {

            } else {
                canvas.drawColor(titleColor);
            }

            paint.setColor(color);
            paint1.setColor(titleColor);

            float k = (float) getWidth() / pageCount;
            int h = getHeight();
            int currentChapter = 0;
            if (AppState.get().isShowChaptersOnProgress && dividers != null && !dividers.isEmpty()) {
                LOG.d("deep level", deep);
                for (OutlineLinkWrapper item : dividers) {
                    int pos = item.targetPage - 1;
                    if (pos < 0 || item.level > deep || item.getTitleRaw().endsWith(Fb2Extractor.FOOTER_AFTRER_BOODY)) {
                        continue;
                    }

                    if (pos <= progress) {
                        currentChapter = pos;
                    } else {
                        canvas.drawLine(pos * k, 0, pos * k, h, paint);
                    }
                }
            }

            canvas.drawRect(0, 0, progress * k, h, paint);
            if (currentChapter != 0) {
                canvas.drawLine(currentChapter * k, 0, currentChapter * k, h, paint1);
            }

        } catch (Exception e) {
            LOG.e(e);
        }
        canvas.restore();
    }

}
