package ais.ui.util;

import org.zkoss.zk.au.AuRequest;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zul.Tab;

import ais.common.Common;

public class MyTabConfig extends Tab {

	private String labelLokal = null;
	
	public MyTabConfig() {
		super();
	}

	public MyTabConfig(String label, String image) {
		super(Common.getBahasaConfig(label), image);
		this.labelLokal = label;
	}

	public MyTabConfig(String label) {
		super(Common.getBahasaConfig(label));
		this.labelLokal = label;
	}
	
	public MyTabConfig(String prefix, String label, String denganPrefix) {
		super(Common.getBahasaConfig(prefix, label));
		this.labelLokal = label;
	}

	public void setTooltiptext(String text) {
		super.setTooltiptext(Common.getBahasaConfig(text));
	}

	public void setLabel(String text) {
		super.setLabel(Common.getBahasaConfig(text));
	}

	/**
	 * Setel label TANPA menerjemahkan — untuk DATA DINAMIS. Lihat {@link MyLabelConfig#setValueData(String)}.
	 */
	public MyTabConfig setLabelData(String text) {
		this.labelLokal = text;
		super.setLabel(text);
		return this;
	}

	/**
	 * Telan error tampilan "Exactly one selected tab is required: []" (race klien vs
	 * tabbox server yang sudah kosong/dibangun ulang) -- sama seperti {@link MyTab#service}.
	 * Lihat memori project: tab-exactly-one-selected-guard.
	 */
	@Override
	public void service(AuRequest request, boolean everError) {
		try {
			super.service(request, everError);
		} catch (UiException e) {
			String msg = e.getMessage() == null ? "" : e.getMessage();
			if (msg.indexOf("Exactly one selected tab is required") >= 0) {
				return;
			}
			throw e;
		}
	}

}
