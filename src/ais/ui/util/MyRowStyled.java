package ais.ui.util;

/**
 * Tipe khusus untuk my row styled. Kelas ini memberi nama dan batas tanggung jawab yang eksplisit
 * pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyFormRow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code setValue}(). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 *
 * @see MyFormRow
 */
public class MyRowStyled extends MyFormRow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6482108245021185374L;

	public MyRowStyled() {
		super();
		super.setStyle("background-color: rgba(255,255,255,0.5);");
		setValign("top");
	}

	public void setValue(String value) {
		super.setValue(value);
	}

}
