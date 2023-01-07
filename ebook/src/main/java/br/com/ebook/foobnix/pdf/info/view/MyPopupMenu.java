package br.com.ebook.foobnix.pdf.info.view;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;

import br.com.ebook.foobnix.android.utils.BaseItemLayoutAdapter;
import br.com.ebook.foobnix.android.utils.Dips;
import br.com.ebook.foobnix.android.utils.LOG;
import br.com.ebook.foobnix.pdf.info.TintUtil;
import br.com.ebook.foobnix.pdf.info.wrapper.AppState;
import br.com.ebook.R;

import java.util.ArrayList;
import java.util.List;

public class MyPopupMenu {
    Context c;
    private View anchor;
    List<Menu> list = new ArrayList<Menu>();
    private boolean isTabsActivity;

    public MyPopupMenu(Context c, View anchor) {
        this.c = c;
        this.anchor = anchor;
        //isTabsActivity = c instanceof MainTabs2;
    }

    public class Menu {
        String stringRes;
        int iconRes;
        OnMenuItemClickListener click;

        public Menu add(int res) {
            this.stringRes = c.getString(res);
            return this;
        }

        public Menu add(String name) {
            this.stringRes = name;
            return this;
        }

        public Menu setIcon(int res) {
            this.iconRes = res;
            return this;
        }

        public Menu setOnMenuItemClickListener(OnMenuItemClickListener onclick) {
            this.click = onclick;
            return this;
        }

    }

    public void show() {

        final ListPopupWindow p1 = new ListPopupWindow(c);
        p1.setModal(true);
        p1.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss() {
                p1.dismiss();
                if (isTabsActivity) {
                    if (AppState.get().isFullScreenMain) {
                        //Keyboards.hideNavigation((Activity) c);
                    }
                } else {
                    if (AppState.get().isFullScreen) {
                        // Keyboards.hideNavigation((Activity) c);
                    }
                }

            }
        });

        BaseItemLayoutAdapter<Menu> a = new BaseItemLayoutAdapter<Menu>(c, R.layout.item_dict_line, list) {
            @Override
            public void populateView(View layout, int position, final Menu item) {
                TextView textView = (TextView) layout.findViewById(R.id.text1);
                textView.setText(item.stringRes);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                ImageView imageView = (ImageView) layout.findViewById(R.id.image1);
                if (item.iconRes != 0) {
                    imageView.setImageResource(item.iconRes);
                    if (item.iconRes == R.drawable.icon_pdf_pro) {
                        TintUtil.setNoTintImage(imageView);
                    } else {
                        if (isTabsActivity) {
                            if (AppState.get().isInkMode || AppState.get().isWhiteTheme) {
                                TintUtil.setTintImageWithAlpha(imageView, TintUtil.color);
                            } else {
                                TintUtil.setTintImageWithAlpha(imageView, Color.WHITE);
                            }
                        } else {

                            if (AppState.get().isDayNotInvert) {
                                TintUtil.setTintImageWithAlpha(imageView, TintUtil.color);
                            } else {
                                TintUtil.setTintImageWithAlpha(imageView, Color.WHITE);
                            }
                        }
                    }
                } else {
                    imageView.setVisibility(View.GONE);
                }
                layout.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        item.click.onMenuItemClick(null);
                        try {
                            p1.dismiss();
                        } catch (Exception e) {
                            LOG.e(e);
                        }
                    }
                });

            }
        };

        p1.setAnchorView(anchor);
        p1.setAdapter(a);
        try {
            p1.setWidth(measureContentWidth(a, c) + Dips.dpToPx(20));
        } catch (Exception e) {
            p1.setWidth(200);
        }

        p1.show();

    }

    private int measureContentWidth(ListAdapter listAdapter, Context mContext) {
        ViewGroup mMeasureParent = null;
        int maxWidth = 0;
        View itemView = null;
        int itemType = 0;

        final ListAdapter adapter = listAdapter;
        final int widthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        final int heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        final int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            final int positionType = adapter.getItemViewType(i);
            if (positionType != itemType) {
                itemType = positionType;
                itemView = null;
            }

            if (mMeasureParent == null) {
                mMeasureParent = new FrameLayout(mContext);
            }

            itemView = adapter.getView(i, itemView, mMeasureParent);
            itemView.measure(widthMeasureSpec, heightMeasureSpec);

            final int itemWidth = itemView.getMeasuredWidth();

            if (itemWidth > maxWidth) {
                maxWidth = itemWidth;
            }
        }

        return maxWidth;
    }

    public Menu getMenu() {
        Menu m = new Menu();
        list.add(m);
        return m;
    }

    public void setAnchor(View anchor) {
        this.anchor = anchor;
    }

}
