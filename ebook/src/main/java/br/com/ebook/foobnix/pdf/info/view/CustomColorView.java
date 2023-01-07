package br.com.ebook.foobnix.pdf.info.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import br.com.ebook.foobnix.StringResponse;
import br.com.ebook.foobnix.android.utils.Dips;
import br.com.ebook.foobnix.pdf.info.wrapper.MagicHelper;
import br.com.ebook.R;
import br.com.ebook.ui.HSVColorPickerDialog;

public class CustomColorView extends FrameLayout {

    private TextView text2;
    StringResponse stringResponse;
    private int initColor;
    private TextView text1;
    private LinearLayout defaultValues;

    int dp25 = Dips.dpToPx(25);

    public CustomColorView(Context context, AttributeSet attrs) {
        super(context, attrs);

        //TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomSeek);
        //String name = a.getString(R.styleable.CustomSeek_text);
        //a.recycle();

        View inflate = LayoutInflater.from(context).inflate(R.layout.custom_color_view, this, false);

        defaultValues = (LinearLayout) inflate.findViewById(R.id.defaultValues);
        defaultValues.setVisibility(View.GONE);

        text1 = (TextView) inflate.findViewById(R.id.text1);
        text2 = (TextView) inflate.findViewById(R.id.text2);

        //text1.setText(name);

        text2.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                new HSVColorPickerDialog(getContext(), initColor, new HSVColorPickerDialog.OnColorSelectedListener() {

                    @Override
                    public void colorSelected(Integer color) {
                        init(color);
                        stringResponse.onResultRecive(MagicHelper.colorToString(color));

                    }
                }).show();

            }
        });

        addView(inflate);

    }

    public void withDefaultColors(int... colors) {
        defaultValues.removeAllViews();
        defaultValues.setVisibility(View.VISIBLE);

        for (final int color : colors) {
            TextView t = new TextView(getContext());
            t.setBackgroundColor(color);
            t.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    init(color);
                    stringResponse.onResultRecive(MagicHelper.colorToString(color));
                }
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp25, dp25);
            p.leftMargin = Dips.dpToPx(4);
            defaultValues.addView(t, p);

        }

    }

    public TextView getText1() {
        return text1;
    }

    public void init(int initColor) {
        this.initColor = initColor;
        text2.setBackgroundColor(initColor);
        text2.setText(MagicHelper.colorToString(initColor));
    }

    public void setOnColorChanged(StringResponse response) {
        stringResponse = response;
    }

}
