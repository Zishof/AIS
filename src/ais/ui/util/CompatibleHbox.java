package ais.ui.util;

import org.zkoss.zul.Hbox;

/**
 * Ekstensi {@link Hbox} yang menambahkan properti ZUL {@code valign} (perataan vertikal) yang
 * tidak tersedia langsung pada {@link Hbox} bawaan versi ZK yang dipakai AIS. Nilai {@code
 * valign} yang diset diterjemahkan menjadi properti CSS inline {@code vertical-align} dan
 * disisipkan ke style komponen, sehingga dapat dipakai langsung sebagai atribut komponen di
 * berkas {@code .zul} (mis. {@code <compatiblehbox valign="middle">}) tanpa perlu menulis style
 * CSS manual di setiap pemakaian.
 */
public class CompatibleHbox extends Hbox {

	private static final long serialVersionUID = -3927952672814450229L;

	/** Nilai perataan vertikal (mis. {@code "middle"}, {@code "top"}) yang sedang diset. */
	private String valign;

	/**
	 * Mengembalikan nilai perataan vertikal yang sedang diset.
	 *
	 * @return nilai {@code valign} saat ini, atau {@code null} bila belum pernah diset
	 */
	public String getValign() {
		return valign;
	}

	/**
	 * Menetapkan perataan vertikal komponen ini dan menuliskannya sebagai properti CSS
	 * {@code vertical-align} ke style komponen. Bila style sudah memiliki deklarasi
	 * {@code vertical-align}, style yang ada TIDAK ditimpa (deklarasi baru hanya ditambahkan
	 * saat belum ada). Nilai kosong/{@code null} tidak melakukan apa-apa selain menyimpan
	 * field {@link #valign}.
	 *
	 * @param valign nilai CSS {@code vertical-align} yang diinginkan (mis. {@code "middle"})
	 */
	public void setValign(String valign) {
		this.valign = valign;
		if (valign == null || valign.trim().length() == 0) {
			return;
		}

		String style = getStyle();
		String verticalAlign = "vertical-align:" + valign.trim() + ";";
		if (style == null || style.trim().length() == 0) {
			setStyle(verticalAlign);
		} else if (style.indexOf("vertical-align") < 0) {
			setStyle(style + (style.endsWith(";") ? "" : ";") + verticalAlign);
		}
	}
}
