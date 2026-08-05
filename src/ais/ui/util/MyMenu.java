package ais.ui.util;

import org.zkoss.zul.Menu;

import ais.common.Common;

public class MyMenu extends Menu {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6707899129552568407L;

	private String labelLokal = null;

	public MyMenu() {
		super();
		setSclass("menu_item");
	}

	public MyMenu(String prefix, String label, String src) {
		super(Common.getBahasaConfig(prefix, label), src);
		this.labelLokal = label;
		setSclass("menu_item");
	}

	public MyMenu(String label, String src) {
		super(Common.getBahasaConfig(label), src);
		this.labelLokal = label;
		setSclass("menu_item");
	}

	public MyMenu(String label) {
		super(Common.getBahasaConfig(label));
		this.labelLokal = label;
		setSclass("menu_item");
	}

	@Override
	public void setImage(String src) {
		String lbl = labelLokal == null || labelLokal.trim().isEmpty()
				? ((getLabel() == null || getLabel().isEmpty()) ? getTooltiptext() : getLabel())
				: labelLokal;
		src = MyMenuitem.svgIcon(lbl, src, true);
		super.setImage(src);
	}

}
