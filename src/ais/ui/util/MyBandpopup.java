package ais.ui.util;

import org.zkoss.zul.Bandpopup;

import ais.common.Common;

public class MyBandpopup extends Bandpopup {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3794161371195657634L;

	public static boolean pakaiClose = true;

	public MyBandpopup() {
		super();
		init();
	}

	private void init() {
		initBg();

	}

	public void initBg() {

		if (Common.isMobile()) {
			super.setWidth("100%");
		}

	}

	@Override
	public void setWidth(String width) {
		try {
			if (Common.isMobile()) {
				super.setWidth("100%");
				return;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/MyBandpopup.java:41");
			// TODO: handle exception
		}
		super.setWidth(width);
	}

}
