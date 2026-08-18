package ais.ui.util;

import org.zkoss.zul.Treeitem;

import ais.common.Common;

public class MyTreeitemConfig extends Treeitem {

	public MyTreeitemConfig() {
		super();
		setSclass("menu_item");
	}

	public MyTreeitemConfig(String label, Object value) {
		super(Common.getBahasaConfig(label), value);
		setSclass("menu_item");
	}

	public MyTreeitemConfig(String prefix, String label, Object value) {
		super(Common.getBahasaConfig(prefix, label), value);
		setSclass("menu_item");
	}

	public MyTreeitemConfig(String label) {
		super(Common.getBahasaConfig(label));
		setSclass("menu_item"); 
	}

	public void setTooltiptext(String text) {
		super.setTooltiptext(Common.getBahasaConfig(text));
	}

	public void setLabel(String text) {
		super.setLabel(Common.getBahasaConfig(text));
	}
	
	@Override
	public void setImage(String src) {
		src = MyMenuitem.svgIcon(getLabel(), src);
		super.setImage(src);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8165594983232482912L;

}
