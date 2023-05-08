package br.com.ebook.foobnix.pdf.info.presentation;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

import br.com.ebook.foobnix.dao2.FileMeta;
import br.com.ebook.foobnix.android.utils.Dips;
import br.com.ebook.foobnix.pdf.info.TintUtil;

public class StarsWrapper {


    public static void addStars(final ImageView starIcon, final FileMeta info) {
        addStars(starIcon, info, null);
    }

    public static void addStars(final ImageView starIcon, final FileMeta info, final Runnable onClick) {
        starIcon.setVisibility(View.VISIBLE);
        TintUtil.drawStar(starIcon, info.getIsStar());
        LayoutParams layoutParams = starIcon.getLayoutParams();
        layoutParams.width = Dips.dpToPx(20);
        layoutParams.height = Dips.dpToPx(20);
        starIcon.setLayoutParams(layoutParams);
        starIcon.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                boolean changeIsStar = false;// AppSharedPreferences.get().changeIsStar(info.getPath());
                info.setIsStar(changeIsStar);
                TintUtil.drawStar(starIcon, changeIsStar);
                if (onClick != null) {
                    onClick.run();
                }
            }
        });
    }
}
