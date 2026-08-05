package ais.ui.util;

import org.zkoss.zul.Div;

/**
 * Pengganti CE untuk org.zkoss.zkmax.zul.Portallayout (EE-only).
 *
 * Dirender sebagai baris kolom fleksibel (CSS flex, blok
 * "KOMPAT PORTALLAYOUT CE" di css_utama.css). Anak-anaknya adalah
 * {@link MyPortalchildren} yang masing-masing menjadi satu kolom berisi
 * Panel/Panelchildren biasa (Panel adalah komponen CE).
 *
 * Yang dipertahankan: tata letak kolom ber-lebar persen, panel
 * bertumpuk per kolom, responsif (kolom turun ke bawah di layar
 * sempit). Yang tidak tersedia di CE: drag-drop panel antar kolom -
 * method terkaitnya disediakan sebagai no-op agar kode lama tetap
 * kompil dan jalan.
 *
 * Kompatibel Java 1.7 dan ZK 5 CE maupun ZK 9/10 CE.
 */
public class MyPortallayout extends Div {

	private static final long serialVersionUID = 1L;

	private String maximizedMode = "whole";
	private boolean counterVisible = false;

	public MyPortallayout() {
		super.setSclass("ais-ce-portallayout");
	}

	/**
	 * Banyak pemanggil lama menimpa sclass (mis. "elearning-portal");
	 * class dasar ais-ce-portallayout wajib selalu ikut karena layout
	 * flex kolom bergantung padanya.
	 */
	public void setSclass(String sclass) {
		String tambahan = sclass == null ? "" : sclass.replace("ais-ce-portallayout", "").trim();
		super.setSclass(tambahan.isEmpty() ? "ais-ce-portallayout" : "ais-ce-portallayout " + tambahan);
	}

	/** EE API kompat: mode maximize panel; di CE hanya disimpan. */
	public void setMaximizedMode(String maximizedMode) {
		this.maximizedMode = maximizedMode;
	}

	public String getMaximizedMode() {
		return maximizedMode;
	}

	/** EE API kompat: tidak ada efek di CE. */
	public void setCounterVisible(boolean counterVisible) {
		this.counterVisible = counterVisible;
	}

	public boolean isCounterVisible() {
		return counterVisible;
	}
}
