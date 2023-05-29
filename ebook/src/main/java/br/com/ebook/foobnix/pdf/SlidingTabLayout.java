package br.com.ebook.foobnix.pdf;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.HorizontalScrollView;


public class SlidingTabLayout extends HorizontalScrollView {


    public interface TabColorizer {

        /**
         * @return return the color of the indicator used when {@code position}
         * is selected.
         */
        int getIndicatorColor(int position);

        /**
         * @return return the color of the divider drawn to the right of
         * {@code position}.
         */
        int getDividerColor(int position);

    }

    private static final int TITLE_OFFSET_DIPS = 24;
    private static final int TAB_VIEW_PADDING_DIPS = 16;
    private static final int TAB_VIEW_TEXT_SIZE_SP = 13;

    private int mTitleOffset;

    private final SlidingTabStrip mTabStrip;

    public SlidingTabLayout(Context context) {
        this(context, null);
    }

    public SlidingTabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingTabLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // Disable the Scroll Bar
        setHorizontalScrollBarEnabled(false);
        // Make sure that the Tab Strips fills this View
        setFillViewport(true);

        mTitleOffset = (int) (TITLE_OFFSET_DIPS * getResources().getDisplayMetrics().density);

        mTabStrip = new SlidingTabStrip(context);
        mTabStrip.setDividerColors(Color.TRANSPARENT);
        addView(getmTabStrip(), LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }


    public SlidingTabStrip getmTabStrip() {
        return mTabStrip;
    }


}