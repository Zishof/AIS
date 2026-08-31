package ais.action.report.helper.pdf;

import ais.action.report.format1.akademik.LaporanDaftarUjian;

/**
 * Tipe khusus untuk laporan absensi ujian window. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * LaporanDaftarUjian}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Tipe ini sengaja tidak menambah state maupun operasi publik. Keberadaannya bukan duplikasi implementasi:
 * nama kelas dipakai sebagai penanda variasi untuk konfigurasi, binding ZK/SOAP, dependency lookup, atau
 * pemilihan perilaku polimorfik. Karena itu jangan menyalin method dari kelas induk ke sini kecuali kontraknya
 * memang berbeda.</p>
 *
 * @see LaporanDaftarUjian
 */
public class LaporanAbsensiUjianWindow extends LaporanDaftarUjian {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8043209882101580146L;

	public LaporanAbsensiUjianWindow() {
		super();

	}

	public LaporanAbsensiUjianWindow(String title, String border,
			boolean closable) throws Exception {
		super(title, border, closable);

	}

}
