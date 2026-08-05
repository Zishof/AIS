package ais.ui.util;

import org.zkoss.zul.Div;

/**
 * Pengganti CE untuk org.zkoss.zkmax.zul.Portalchildren (EE-only):
 * satu kolom dalam {@link MyPortallayout}. Lebar persen yang diberikan
 * lewat setWidth() dipertahankan sebagai basis kolom oleh CSS blok
 * "KOMPAT PORTALLAYOUT CE" di css_utama.css.
 *
 * Kompatibel Java 1.7 dan ZK 5 CE maupun ZK 9/10 CE.
 */
public class MyPortalchildren extends Div {

	private static final long serialVersionUID = 1L;

	public MyPortalchildren() {
		super.setSclass("ais-ce-portalchildren");
	}

	/**
	 * Pemanggil lama bisa menimpa sclass (mis. "elearning-portal-col");
	 * class dasar ais-ce-portalchildren wajib selalu ikut karena lebar
	 * kolom flex bergantung padanya.
	 */
	public void setSclass(String sclass) {
		String tambahan = sclass == null ? "" : sclass.replace("ais-ce-portalchildren", "").trim();
		super.setSclass(tambahan.isEmpty() ? "ais-ce-portalchildren" : "ais-ce-portalchildren " + tambahan);
	}
}
