package ais.ui.util;

import org.zkoss.zul.Html;

public class MyHtmlIframe extends Html {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7510808476684701322L;

	public MyHtmlIframe() {
		super();
		// TODO Auto-generated constructor stub
	}

	public MyHtmlIframe(String content) {
		super(content);
		// TODO Auto-generated constructor stub
		
	}

	public String getContent() {
		String c = super.getContent();
		if (c != null && c.toLowerCase().contains("script")) {
			c = c.replaceAll("(?i)script", "__S__");
		}
		if (c != null && c.toLowerCase().contains("iframe")) {
			c = org.apache.commons.lang3.StringUtils.replaceIgnoreCase(c, "iframe", "");
		}
		return c;
	}

}
