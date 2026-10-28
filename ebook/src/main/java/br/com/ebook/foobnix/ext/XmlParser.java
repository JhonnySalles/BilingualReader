package br.com.ebook.foobnix.ext;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class XmlParser {

	public static XmlPullParser buildPullParser() throws XmlPullParserException {
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		factory.setValidating(false);
        try {
            factory.setFeature("http://xmlpull.org/v1/doc/features.html#relaxed", true);
        } catch (Throwable ignored) {
        }
        return factory.newPullParser();
	}

}
