package ais.action.report.helper.pdf;

import ais.action.report.format1.akademik.LaporanAbsensi;

/**
 * Tipe khusus untuk laporan absensi window. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * LaporanAbsensi}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan
 * yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Tipe ini sengaja tidak menambah state maupun operasi publik. Keberadaannya bukan duplikasi implementasi:
 * nama kelas dipakai sebagai penanda variasi untuk konfigurasi, binding ZK/SOAP, dependency lookup, atau
 * pemilihan perilaku polimorfik. Karena itu jangan menyalin method dari kelas induk ke sini kecuali kontraknya
 * memang berbeda.</p>
 *
 * @see LaporanAbsensi
 */
public class LaporanAbsensiWindow extends LaporanAbsensi {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8660549593751345840L;

	public LaporanAbsensiWindow() {
		super();

	}

	public LaporanAbsensiWindow(String title, String border, boolean closable)
			throws Exception {
		super(title, border, closable);

	}


}
