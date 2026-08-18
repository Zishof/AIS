package ais.ui.util;

import org.zkoss.zul.Iframe;

public class MyIframe extends Iframe {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8735575580753564474L;

	public MyIframe() {
		super();
		init();
	}

	public MyIframe(String src) {
		super(src);
		init();
	}

	private void init() {
		setHeight("100%");
		setWidth("100%");
	}

}
