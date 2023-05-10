package com.artifex.mu;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;

import java.util.ArrayList;

public class MuPDFCore
{
	/* load our native library */
	private static boolean gs_so_available = false;
	static {
		System.out.println("Loading dll");
		System.loadLibrary("mupdf_java");
		System.out.println("Loaded dll");
		if (gprfSupportedInternal())
		{
			try {
				System.loadLibrary("gs");
				gs_so_available = true;
			}
			catch (UnsatisfiedLinkError e) {
				gs_so_available = false;
			}
		}
	}

	/* Readable members */
	private int numPages = -1;
	private float pageWidth;
	private float pageHeight;
	private long globals;
	private byte fileBuffer[];
	private String file_format;
	private boolean isUnencryptedPDF;
	private final boolean wasOpenedFromBuffer;

	/* The native functions */
	private static native boolean gprfSupportedInternal();
	private native long openFile(String filename);
	private native long openBuffer(String magic);
	private native String fileFormatInternal();
	private native boolean isUnencryptedPDFInternal();
	private native int countPagesInternal();
	private native void gotoPageInternal(int localActionPageNum);
	private native float getPageWidth();
	private native float getPageHeight();
	private native void drawPage(Bitmap bitmap,
			int pageW, int pageH,
			int patchX, int patchY,
			int patchW, int patchH,
			long cookiePtr);
	private native void updatePageInternal(Bitmap bitmap,
			int page,
			int pageW, int pageH,
			int patchX, int patchY,
			int patchW, int patchH,
			long cookiePtr);
	private native RectF[] searchPage(String text);
	private native TextChar[][][][] text();
	private native byte[] textAsHtml();
	private native void addMarkupAnnotationInternal(PointF[] quadPoints, int type);
	private native void addInkAnnotationInternal(PointF[][] arcs);
	private native void deleteAnnotationInternal(int annot_index);
	private native int passClickEventInternal(int page, float x, float y);
	private native void setFocusedWidgetChoiceSelectedInternal(String [] selected);
	private native String [] getFocusedWidgetChoiceSelected();
	private native String [] getFocusedWidgetChoiceOptions();
	private native int getFocusedWidgetSignatureState();
	private native String checkFocusedSignatureInternal();
	private native boolean signFocusedSignatureInternal(String keyFile, String password);
	private native int setFocusedWidgetTextInternal(String text);
	private native RectF[] getWidgetAreasInternal(int page);
	private native Annotation[] getAnnotationsInternal(int page);
	//private native OutlineItem [] getOutlineInternal();
	private native boolean hasOutlineInternal();
	private native boolean needsPasswordInternal();
	private native boolean authenticatePasswordInternal(String password);
	private native void startAlertsInternal();
	private native void stopAlertsInternal();
	private native void destroying();
	private native boolean hasChangesInternal();
	private native void saveInternal();
	private native long createCookie();
	private native void destroyCookie(long cookie);
	private native void abortCookie(long cookie);

	private native String startProofInternal(int resolution);
	private native void endProofInternal(String filename);
	private native int getNumSepsOnPageInternal(int page);
	private native int controlSepOnPageInternal(int page, int sep, boolean disable);
	private native Separation getSepInternal(int page, int sep);

	public native boolean javascriptSupported();

	public class Cookie
	{
		private final long cookiePtr;

		public Cookie()
		{
			cookiePtr = createCookie();
			if (cookiePtr == 0)
				throw new OutOfMemoryError();
		}

		public void abort()
		{
			abortCookie(cookiePtr);
		}

		public void destroy()
		{
			// We could do this in finalize, but there's no guarantee that
			// a finalize will occur before the muPDF context occurs.
			destroyCookie(cookiePtr);
		}
	}

	public MuPDFCore(Context context, String filename) throws Exception
	{
		globals = openFile(filename);
		if (globals == 0)
		{
			throw new Exception(String.format("Não é possível abrir o item selecionado", filename));
		}
		file_format = fileFormatInternal();
		isUnencryptedPDF = isUnencryptedPDFInternal();
		wasOpenedFromBuffer = false;
	}

	public MuPDFCore(Context context, byte buffer[], String magic) throws Exception {
		fileBuffer = buffer;
		globals = openBuffer(magic != null ? magic : "");
		if (globals == 0)
		{
			throw new Exception("Não é possível abrir o buffer");
		}
		file_format = fileFormatInternal();
		isUnencryptedPDF = isUnencryptedPDFInternal();
		wasOpenedFromBuffer = true;
	}

	public int countPages()
	{
		if (numPages < 0)
			numPages = countPagesSynchronized();
		return numPages;
	}

	private synchronized int countPagesSynchronized() {
		return countPagesInternal();
	}

	/* Shim function */
	private void gotoPage(int page)
	{
		if (page > numPages-1)
			page = numPages-1;
		else if (page < 0)
			page = 0;
		gotoPageInternal(page);
		this.pageWidth = getPageWidth();
		this.pageHeight = getPageHeight();
	}

	public synchronized void onDestroy() {
		destroying();
		globals = 0;
	}

	public synchronized RectF [] searchPage(int page, String text) {
		gotoPage(page);
		return searchPage(text);
	}

	public synchronized byte[] html(int page) {
		gotoPage(page);
		return textAsHtml();
	}

	public synchronized TextWord [][] textLines(int page) {
		gotoPage(page);
		TextChar[][][][] chars = text();

		// The text of the page held in a hierarchy (blocks, lines, spans).
		// Currently we don't need to distinguish the blocks level or
		// the spans, and we need to collect the text into words.
		ArrayList<TextWord[]> lns = new ArrayList<TextWord[]>();

		for (TextChar[][][] bl: chars) {
			if (bl == null)
				continue;
			for (TextChar[][] ln: bl) {
				ArrayList<TextWord> wds = new ArrayList<TextWord>();
				TextWord wd = new TextWord();

				for (TextChar[] sp: ln) {
					for (TextChar tc: sp) {
						if (tc.c != ' ') {
							wd.Add(tc);
						} else if (wd.w.length() > 0) {
							wds.add(wd);
							wd = new TextWord();
						}
					}
				}

				if (wd.w.length() > 0)
					wds.add(wd);

				if (wds.size() > 0)
					lns.add(wds.toArray(new TextWord[wds.size()]));
			}
		}

		return lns.toArray(new TextWord[lns.size()][]);
	}

	public synchronized void save() {
		saveInternal();
	}

}
