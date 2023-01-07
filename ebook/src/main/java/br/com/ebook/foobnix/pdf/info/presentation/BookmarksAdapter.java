package br.com.ebook.foobnix.pdf.info.presentation;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.view.ViewCompat;

import br.com.ebook.foobnix.android.utils.TxtUtils;
import br.com.ebook.foobnix.pdf.info.AppSharedPreferences;
import br.com.ebook.foobnix.pdf.info.wrapper.AppState;
import br.com.ebook.foobnix.pdf.info.wrapper.AppBookmark;
import br.com.ebook.R;

import java.util.List;

public class BookmarksAdapter extends BaseAdapter {

    private final List<AppBookmark> objects;
    private final boolean submenu;
    private final Context context;

    public BookmarksAdapter(Context context, List<AppBookmark> objects, boolean submenu) {
        this.context = context;
        this.objects = objects;
        this.submenu = submenu;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView == null ? LayoutInflater.from(context).inflate(R.layout.bookmark_item, parent, false) : convertView;

        final AppBookmark bookmark = objects.get(position);

        final TextView textView = (TextView) view.findViewById(R.id.text);
        final TextView pageView = (TextView) view.findViewById(R.id.page);
        final TextView titleView = (TextView) view.findViewById(R.id.title);
        final ImageView image = (ImageView) view.findViewById(R.id.image);
        final View deleteView = view.findViewById(R.id.remove);
        ((View) image.getParent()).setVisibility(View.GONE);
        ViewCompat.setElevation(((View) image.getParent()), 0);
        view.setBackgroundColor(Color.TRANSPARENT);


        String pageNumber = TxtUtils.deltaPage(AppState.get().isCut ? bookmark.getPage() * 2 : bookmark.getPage());
        titleView.setVisibility(View.GONE);
        textView.setText(pageNumber + ": " + bookmark.getText());
        pageView.setText(pageNumber);

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) pageView.getLayoutParams();
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        pageView.setBackgroundColor(Color.TRANSPARENT);
        pageView.setLayoutParams(layoutParams);
        pageView.setTextColor(textView.getCurrentTextColor());

        deleteView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                AppSharedPreferences.get().removeBookmark(bookmark);
                objects.remove(bookmark);
                notifyDataSetChanged();
            }
        });

        return view;
    }

    @Override
    public int getCount() {
        return objects.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return 0;
    }


}
