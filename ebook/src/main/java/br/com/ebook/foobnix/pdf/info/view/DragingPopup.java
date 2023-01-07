package br.com.ebook.foobnix.pdf.info.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import br.com.ebook.foobnix.android.utils.Dips;
import br.com.ebook.foobnix.android.utils.LOG;
import br.com.ebook.foobnix.android.utils.TxtUtils;
import br.com.ebook.foobnix.pdf.info.TintUtil;
import br.com.ebook.foobnix.pdf.info.model.BookCSS;
import br.com.ebook.foobnix.pdf.info.widget.DraggbleTouchListener;
import br.com.ebook.foobnix.pdf.info.wrapper.AppState;
import br.com.ebook.R;

import java.util.HashMap;
import java.util.Map;

public abstract class DragingPopup {
    protected static final String PREF = "PREF";
    private static final String DRAGGING_POPUPS = "DraggingPopups";
    private static int MIN_WH = Dips.dpToPx(50);
    private FrameLayout anchor;
    private View popupView;
    private FrameLayout popupContent;
    private LayoutInflater inflater;
    private int rootWidth;
    private int width;
    private int heigth;
    private Runnable onCloseListener;

    protected String titleAction;
    protected Runnable titleRunnable;

    protected int titlePopupIcon;
    protected MyPopupMenu titlePopupMenu;

    public void beforeCreate() {

    }

    static class Place {
        public int x, y, width, height;

        @Override
        public String toString() {
            return String.format("%s,%s,%s,%s", x, y, width, height);
        }

        public static Place fromString(String str) {
            Place p = new Place();
            if (TxtUtils.isEmpty(str)) {
                return p;
            }
            try {
                String[] split = str.split(",");
                p.x = Integer.valueOf(split[0]);
                p.y = Integer.valueOf(split[1]);
                p.width = Integer.valueOf(split[2]);
                p.height = Integer.valueOf(split[3]);
                return p;
            } catch (Exception e) {
                LOG.e(e);
            }
            return p;
        }
    }

    static Map<String, Place> cache = new HashMap<String, Place>();

    public static void loadCache(final Context c) {
        try {
            cache.clear();
            SharedPreferences sp = c.getSharedPreferences(DRAGGING_POPUPS, Context.MODE_PRIVATE);
            Map<String, String> all = (Map<String, String>) sp.getAll();
            for (String key : all.keySet()) {
                cache.put(key, Place.fromString(all.get(key)));
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static void saveCache(final Context c) {
        SharedPreferences sp = c.getSharedPreferences(DRAGGING_POPUPS, Context.MODE_PRIVATE);
        sp.edit().clear().commit();
        Editor edit = sp.edit();
        for (String key : cache.keySet()) {
            edit.putString(key, cache.get(key).toString());
        }
        edit.commit();

    }

    public DragingPopup(String title, final FrameLayout anchor) {
        this(title, anchor, 250, 250);
    }

    public DragingPopup(int titleResID, final FrameLayout anchor, int width, int heigth) {
        this(anchor.getContext().getString(titleResID), anchor, width, heigth);
    }

    public DragingPopup(String title, final FrameLayout anchor, int width, int heigth) {
        this.anchor = anchor;
        if (Dips.isXLargeScreen()) {
            width = (int) (width * 1.5);
            heigth = (int) (heigth * 1.5);
        }
        if (Dips.screenWidth() > Dips.screenHeight()) {
            width = (int) (width * 1.25);
        }
        this.width = width;
        this.heigth = Math.min(Dips.dpToPx(heigth), Dips.screenHeight() - Dips.dpToPx(40));

        inflater = LayoutInflater.from(anchor.getContext());

        popupView = inflater.inflate(R.layout.drag_popup, null, false);
        ImageView appLogo = (ImageView) popupView.findViewById(R.id.droid);
        appLogo.setVisibility(View.GONE);

        View findViewById = popupView.findViewById(R.id.topLayout);
        TintUtil.setTintBgSimple(findViewById, 230);

        popupContent = (FrameLayout) popupView.findViewById(R.id.popupContent);

        TextView titleView = (TextView) popupView.findViewById(R.id.dialogTitle);
        titleView.setText(title);
        if (AppState.get().isUseTypeFace) {
            titleView.setTypeface(BookCSS.getNormalTypeFace());
        }

        popupView.findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                closeDialog();
            }
        });
        rootWidth = ((View) anchor.getParent()).getWidth();
    }

    public void initState() {
        String tag = getTAG() + Dips.screenWidth();
        if (cache.containsKey(tag)) {
            Place place = cache.get(tag);
            AnchorHelper.setXY(anchor, place.x, place.y);
            popupView.getLayoutParams().width = place.width;
            popupView.getLayoutParams().height = place.height;

            LOG.d("Anchor Load", tag, place.x, place.y, place.width, place.height);

            popupView.requestLayout();
        } else {
            AnchorHelper.setXY(anchor, Dips.dpToPx(Dips.screenWidthDP() - width) / 2, (Dips.screenHeight() - heigth) / 2);
            popupView.getLayoutParams().width = Dips.dpToPx(width);
            popupView.getLayoutParams().height = heigth;// Dips.dpToPx(heigth);
            popupView.requestLayout();
        }
    }

    public void requestLayout() {
        popupView.requestLayout();
    }

    public abstract View getContentView(LayoutInflater inflater);

    public DragingPopup show(String tag) {
        show(tag, false, false);
        return this;
    }

    public DragingPopup show(String tag, boolean always) {
        show(tag, always, false);
        return this;
    }

    public void realod() {
        popupContent.removeAllViews();
        popupContent.addView(getContentView(inflater));
    }

    public void setTitlePopupIcon(int icon) {
        ImageView onIconAction = (ImageView) popupView.findViewById(R.id.onIconAction);
        onIconAction.setImageResource(icon);
    }

    public DragingPopup show(String tag, boolean always, boolean update) {
        if (tag != null) {
            if (!always) {
                if (tag.equals(anchor.getTag())) {
                    if (anchor.getVisibility() == View.VISIBLE) {
                        anchor.setVisibility(View.GONE);
                        return this;
                    } else {
                        anchor.setVisibility(View.VISIBLE);
                        if (!update) {
                            return this;
                        }
                    }

                }
            }
            anchor.setTag(tag);
        }

        popupContent.removeAllViews();
        beforeCreate();

        if (titleAction != null) {
            popupView.findViewById(R.id.onTitleAction).setVisibility(View.VISIBLE);
            popupView.findViewById(R.id.onTitleAction1).setVisibility(View.VISIBLE);
            popupView.findViewById(R.id.onTitleAction).setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (titleRunnable != null) {
                        titleRunnable.run();
                    }

                }
            });
        } else {
            popupView.findViewById(R.id.onTitleAction).setVisibility(View.GONE);
            popupView.findViewById(R.id.onTitleAction1).setVisibility(View.GONE);
        }

        popupContent.addView(getContentView(inflater));

        ImageView onIconAction = (ImageView) popupView.findViewById(R.id.onIconAction);
        if (titlePopupMenu == null) {
            onIconAction.setVisibility(View.GONE);
            onIconAction.setImageResource(titlePopupIcon);
        } else {
            onIconAction.setVisibility(View.VISIBLE);
            onIconAction.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    titlePopupMenu.setAnchor(v);
                    titlePopupMenu.show();

                }
            });
        }

        anchor.setVisibility(View.VISIBLE);
        anchor.removeAllViews();
        anchor.addView(popupView);
        DraggbleTouchListener draggbleTouchListener = new DraggbleTouchListener(anchor, this);
        draggbleTouchListener.setOnMoveFinish(new Runnable() {

            @Override
            public void run() {
                saveLayout();
            }
        });
        popupView.findViewById(R.id.dialogTitle).setOnTouchListener(draggbleTouchListener);
        initState();

        View right = popupView.findViewById(R.id.rigth);
        right.setOnTouchListener(new OnTouchListener() {
            float x, y, x2, y2;
            int w, h;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN && popupView.getLayoutParams() != null) {
                    x = event.getRawX();
                    y = event.getRawY();
                    w = popupView.getLayoutParams().width;
                    h = popupView.getLayoutParams().height;
                    x2 = AnchorHelper.getX(anchor);
                    y2 = AnchorHelper.getY(anchor);
                } else if (event.getAction() == MotionEvent.ACTION_MOVE && popupView.getLayoutParams() != null) {

                    int nWidth = (int) (w + (event.getRawX() - x));
                    if (nWidth > MIN_WH) {
                        popupView.getLayoutParams().width = nWidth;
                    }
                    int nHeight = (int) (h + (event.getRawY() - y));
                    if (nHeight > MIN_WH) {
                        popupView.getLayoutParams().height = nHeight;
                    }

                    AnchorHelper.setXY(anchor, x2, y2);
                    popupView.requestLayout();

                    LOG.d("Anchor WxH", Dips.pxToDp(anchor.getWidth()), Dips.pxToDp(anchor.getHeight()));

                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    saveLayout();
                }
                return true;
            }
        });
        View left = popupView.findViewById(R.id.left);
        left.setOnTouchListener(new OnTouchListener() {
            float x, y, x2, y2;
            int w, h;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN && popupView.getLayoutParams() != null) {
                    x = event.getRawX();
                    y = event.getRawY();
                    w = popupView.getLayoutParams().width;
                    h = popupView.getLayoutParams().height;
                    x2 = AnchorHelper.getX(anchor);
                    y2 = AnchorHelper.getY(anchor);
                } else if (event.getAction() == MotionEvent.ACTION_MOVE && popupView.getLayoutParams() != null) {
                    int nWidth = (int) (w + (x - event.getRawX()));
                    if (nWidth > MIN_WH) {
                        popupView.getLayoutParams().width = nWidth;
                    }

                    int nHeight = (int) (h + (event.getRawY() - y));
                    if (nHeight > MIN_WH) {
                        popupView.getLayoutParams().height = nHeight;
                    }
                    if (event.getRawX() + popupView.getLayoutParams().width < rootWidth) {
                        AnchorHelper.setX(anchor, event.getRawX());
                    }

                    AnchorHelper.setY(anchor, y2);
                    popupView.requestLayout();

                    LOG.d("Anchor WxH", Dips.pxToDp(anchor.getWidth()), Dips.pxToDp(anchor.getHeight()));

                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    saveLayout();
                }
                return true;
            }
        });
        TintUtil.setTintBg(left);
        TintUtil.setTintBg(right);
        return this;

    }

    public boolean isVisible() {
        return anchor.getVisibility() == View.VISIBLE;
    }

    public void closeDialog() {
        saveLayout();

        anchor.setVisibility(View.GONE);
        if (onCloseListener != null) {
            onCloseListener.run();
        }
        //Keyboards.close(anchor);
        //Keyboards.hideNavigation((Activity) anchor.getContext());
    }


    private void saveLayout() {
        try {
            Place place = new Place();
            place.x = (int) AnchorHelper.getX(anchor);
            place.y = (int) AnchorHelper.getY(anchor);
            place.width = popupView.getLayoutParams().width;
            place.height = popupView.getLayoutParams().height;

            String tag = getTAG() + Dips.screenWidth();
            cache.put(tag, place);
            LOG.d("Anchor Save", tag, place.x, place.y, place.width, place.height);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public String getTAG() {
        String tag = anchor.getTag().toString();
        if (tag.contains(PREF)) {
            tag = PREF;
        }
        return tag;

    }

    public DragingPopup setOnCloseListener(Runnable onCloseListener) {
        this.onCloseListener = onCloseListener;
        return this;
    }

}
