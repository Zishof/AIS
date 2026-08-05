package ais.ui.util;

import org.zkoss.zul.Div;

/**
 * Satu panel isi untuk {@link MyHtmlTabbox} — analog {@code <tabpanel>} ZK, berbasis
 * {@link org.zkoss.zul.Div} (kelas CSS {@code myhtml-tabpanel}).
 *
 * <p>Inilah titik di mana scroll/tinggi dikendalikan PENUH lewat CSS sendiri
 * ({@code overflow:visible}, tinggi natural mengikuti konten) — tidak lagi tergantung
 * mold {@code z-tabpanel-cnt} ZK yang kerap kolaps 0px. Pemanggil boleh menimpa gaya via
 * atribut {@code style}/{@code sclass} pada ZUL bila butuh area scroll terbatas.</p>
 *
 * <p>Karena {@code MyHtmlTabpanel} adalah {@code Div} biasa, ia bisa di-autowire ke field
 * bertipe {@link org.zkoss.zk.ui.Component} (bukan {@code Tabpanel}) di Action; handler yang
 * hanya memakai {@code getChildren()}/{@code setParent(panel)} tetap berjalan.</p>
 */
public class MyHtmlTabpanel extends Div {

	private static final long serialVersionUID = 1L;

	private static final String SCLASS = "myhtml-tabpanel";

	public MyHtmlTabpanel() {
		super();
		setSclass(SCLASS);
	}

	/** Dipanggil {@link MyHtmlTabbox}: tampilkan (true) atau sembunyikan (false) panel ini. */
	void tampilkan(boolean tampil) {
		setSclass(tampil ? SCLASS : SCLASS + " " + SCLASS + "-hidden");
	}
}
