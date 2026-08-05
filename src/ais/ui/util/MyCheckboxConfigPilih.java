package ais.ui.util;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Checkbox;

import ais.common.Common;

public class MyCheckboxConfigPilih extends Checkbox {

	public MyCheckboxConfigPilih() {
		super();
		setChecked(true);
		setStyle("font-size:11px;");
		// TODO Auto-generated constructor stub
	}

	public MyCheckboxConfigPilih(String label, String image) {
		super(Common.getBahasaConfig(label), image);
		setChecked(true);
		setStyle("font-size:11px;");
	}

	public MyCheckboxConfigPilih(String label) {
		super(Common.getBahasaConfig(label));
		setChecked(true);
		setStyle("font-size:11px;");
	}

	public void setTooltiptext(String text) {
		super.setTooltiptext(Common.getBahasaConfig(text));
	}

	public void setLabel(String text) {
		super.setLabel(Common.getBahasaConfig(text));
	}

	@Override
	public void setParent(Component arg0) {
		// TODO Auto-generated method stub
		if (arg0 != null) {
			arg0.setAttribute("checkbox", this);
		}
		super.setParent(arg0);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8165594983232482912L;

}
