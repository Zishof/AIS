package ais.ui.util;

import org.zkoss.zul.Div;

/**
 * Kontainer baris header tab untuk {@link MyHtmlTabbox} — analog {@code <tabs>} ZK,
 * tetapi berbasis {@link org.zkoss.zul.Div} (kelas CSS {@code myhtml-tabs}).
 * Anak-anaknya adalah {@link MyHtmlTab}. Header memakai {@code flex-wrap:wrap}
 * sehingga banyak tab membungkus ke baris berikutnya, bukan terpotong.
 */
public class MyHtmlTabs extends Div {

	private static final long serialVersionUID = 1L;

	public MyHtmlTabs() {
		super();
		setSclass("myhtml-tabs");
	}
}
