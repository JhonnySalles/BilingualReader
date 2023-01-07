package br.com.ebook.foobnix.pdf.info.presentation;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import br.com.ebook.foobnix.android.utils.Dips;
import br.com.ebook.foobnix.android.utils.TxtUtils;
import br.com.ebook.foobnix.pdf.info.PageUrl;
import br.com.ebook.foobnix.pdf.info.wrapper.AppState;
import br.com.ebook.universalimageloader.core.ImageLoader;
import br.com.ebook.R;
import br.com.ebook.foobnix.pdf.info.IMG;

public class PageThumbnailAdapter extends BaseAdapter {

    private LayoutInflater inflater;
    private String path;

    private int pageCount;
    private Context c;
    private int currentPage;

    public PageThumbnailAdapter(Context c, int pageCount, int currentPage) {
        this.c = c;
        this.pageCount = pageCount;
        this.currentPage = currentPage;
        inflater = LayoutInflater.from(c);
    }

    @Override
    public int getCount() {
        return pageCount;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.item_page, null);
        }
        if (currentPage == position) {
            view.setBackgroundResource(R.color.tint_selector);
        } else {
            view.setBackgroundColor(Color.TRANSPARENT);
        }

        ImageView img = (ImageView) view.findViewById(R.id.image1);
        LayoutParams lp = IMG.updateImageSizeSmall(img);
        if (AppState.get().isDouble && Dips.isHorizontal()) {
            lp.width = lp.width * 2;
        }

        PageUrl pageUrl = getPageUrl(position);
        final String url = pageUrl.toString();
        ImageLoader.getInstance().displayImage(url, img, IMG.displayImageOptionsNoDiscCache);

        TextView txt = (TextView) view.findViewById(R.id.text1);
        txt.setText(TxtUtils.deltaPage((position + 1)));

        txt.setVisibility(View.VISIBLE);
        img.setVisibility(View.VISIBLE);

        return view;
    }

    public PageUrl getPageUrl(int page) {
        return null;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

}
