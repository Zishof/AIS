package ais.ui.util;

import org.zkoss.zul.Html;

public class MyHtml extends Html {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7510808476684701322L;

	public MyHtml() {
		super();
		// TODO Auto-generated constructor stub
	}

	public MyHtml(String content) {
		super(content);
		// TODO Auto-generated constructor stub

	}

	public String getContent() {
		String c = MyHtml.bersihkan(super.getContent());
		return c;
	}

	public static String bersihkan(String c) {
		if (c != null && c.toLowerCase().contains("script")) {
			c = c.replaceAll("(?i)script", "__S__");
		}
		return c;
	}

}
