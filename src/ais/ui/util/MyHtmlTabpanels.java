package ais.ui.util;

import org.zkoss.zul.Div;

/**
 * Kontainer isi panel untuk {@link MyHtmlTabbox} — analog {@code <tabpanels>} ZK,
 * berbasis {@link org.zkoss.zul.Div} (kelas CSS {@code myhtml-tabpanels}).
 * Anak-anaknya adalah {@link MyHtmlTabpanel}; hanya satu yang tampil pada satu waktu.
 */
public class MyHtmlTabpanels extends Div {

	private static final long serialVersionUID = 1L;

	public MyHtmlTabpanels() {
		super();
		setSclass("myhtml-tabpanels");
	}
}
