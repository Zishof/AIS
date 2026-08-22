package ais.ui.util;

import org.zkoss.zul.Hbox;

public class CompatibleHbox extends Hbox {

	private static final long serialVersionUID = -3927952672814450229L;

	private String valign;

	public String getValign() {
		return valign;
	}

	public void setValign(String valign) {
		this.valign = valign;
		if (valign == null || valign.trim().length() == 0) {
			return;
		}

		String style = getStyle();
		String verticalAlign = "vertical-align:" + valign.trim() + ";";
		if (style == null || style.trim().length() == 0) {
			setStyle(verticalAlign);
		} else if (style.indexOf("vertical-align") < 0) {
			setStyle(style + (style.endsWith(";") ? "" : ";") + verticalAlign);
		}
	}
}
