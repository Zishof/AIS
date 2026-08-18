package ais.ui.util;

import org.zkoss.zul.Toolbarbutton;

import ais.common.Common;

public class MyToolbarbuttonKecilConfig extends Toolbarbutton {

	private String labelLokal = null;

	public MyToolbarbuttonKecilConfig() {
		super();
		setStyle("font-size:10px;");
	}

	public MyToolbarbuttonKecilConfig(String label, String image) {
		super(Common.getBahasaConfig(label), MyMenuitem.svgIcon(label, image));
		setStyle("font-size:10px;");
		this.labelLokal = label;
	}

	public MyToolbarbuttonKecilConfig(String label) {
		super(Common.getBahasaConfig(label));
		setStyle("font-size:10px;");
		this.labelLokal = label;
	}

	public void setTooltiptext(String text) {
		super.setTooltiptext(Common.getBahasaConfig(text));
	}

	public void setLabel(String text) {
		super.setLabel(Common.getBahasaConfig(text));
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8165594983232482912L;

	@Override
	public void setImage(String src) {
		String lbl = labelLokal == null || labelLokal.trim().isEmpty()
				? ((getLabel() == null || getLabel().isEmpty()) ? getTooltiptext() : getLabel())
				: labelLokal;
		src = MyMenuitem.svgIcon(lbl, src, true);
		super.setImage(src);
	}

}
