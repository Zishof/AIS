package ais.ui.util;

import org.zkoss.zul.Div;
import org.zkoss.zul.Label;

import ais.common.Common;

/**
 * Satu tab header untuk {@link MyHtmlTabbox} — analog {@code <tab>} ZK, berbasis
 * {@link org.zkoss.zul.Div} (kelas CSS {@code myhtml-tab}).
 *
 * <p>Label diset via atribut {@code label="..."} (di-i18n lewat
 * {@link ais.common.Common#getBahasaConfig(String)}, sama seperti {@code MyTabConfig}).
 * Atribut {@code selected="true"} menandai tab awal. Atribut {@code forward="onClick=..."}
 * bekerja apa adanya (fitur {@code Div}) sehingga handler lazy-build di Action tetap jalan.</p>
 *
 * <p>Seleksi visual (kelas {@code myhtml-tab-active}) dikelola oleh {@link MyHtmlTabbox};
 * lihat {@link #tandaiAktif(boolean)}.</p>
 */
public class MyHtmlTab extends Div {

	private static final long serialVersionUID = 1L;

	private static final String SCLASS = "myhtml-tab";

	private String labelLokal;
	private boolean selected;
	private Label labelComp;

	public MyHtmlTab() {
		super();
		setSclass(SCLASS);
	}

	public MyHtmlTab(String label) {
		this();
		setLabel(label);
	}

	/** Set teks tab (dengan terjemahan konfigurasi). Idempoten. */
	public void setLabel(String label) {
		this.labelLokal = label;
		String teks = label;
		try {
			teks = Common.getBahasaConfig(label);
		} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/ui/util/MyHtmlTab.java:46");
			// fallback ke teks asli bila konfigurasi bahasa tak tersedia
		}
		if (labelComp == null) {
			labelComp = new Label();
			appendChild(labelComp);
		}
		labelComp.setValue(teks == null ? "" : teks);
	}

	public String getLabel() {
		return labelLokal;
	}

	/** Dipakai ZUL {@code selected="true"} untuk menandai tab awal. */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public boolean isSelected() {
		return selected;
	}

	/** Dipanggil {@link MyHtmlTabbox} untuk menyalakan/mematikan gaya aktif. */
	void tandaiAktif(boolean aktif) {
		this.selected = aktif;
		setSclass(aktif ? SCLASS + " " + SCLASS + "-active" : SCLASS);
	}
}
