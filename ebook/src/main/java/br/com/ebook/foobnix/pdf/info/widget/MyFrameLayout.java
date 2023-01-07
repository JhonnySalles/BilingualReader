package br.com.ebook.foobnix.pdf.info.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import br.com.ebook.foobnix.android.utils.LOG;

public class MyFrameLayout extends FrameLayout {

	public MyFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public MyFrameLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		LOG.d("Size changed", w, h);
	}
}
