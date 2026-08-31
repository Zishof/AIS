package ais.action.report.helper.pdf;

import ais.action.report.format1.akademik.LaporanRekapJumlahDosen;
import ais.action.report.format1.akademik.LaporanRekapJumlahDosenPerPendidikan;

/**
 * Tipe khusus untuk laporan rekapitulasi dosen per pendidikan window. Kelas ini memberi nama dan
 * batas tanggung jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang
 * diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * LaporanRekapJumlahDosenPerPendidikan}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk
 * variasi ini; perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak
 * bercabang atau tumpang tindih.</p>
 * <p>Tipe ini sengaja tidak menambah state maupun operasi publik. Keberadaannya bukan duplikasi implementasi:
 * nama kelas dipakai sebagai penanda variasi untuk konfigurasi, binding ZK/SOAP, dependency lookup, atau
 * pemilihan perilaku polimorfik. Karena itu jangan menyalin method dari kelas induk ke sini kecuali kontraknya
 * memang berbeda.</p>
 *
 * @see LaporanRekapJumlahDosenPerPendidikan
 */
public class LaporanRekapitulasiDosenPerPendidikanWindow extends
		LaporanRekapJumlahDosenPerPendidikan {

	/**
	 * 
	 */
	private static final long serialVersionUID = -13837562876834346L;

	public LaporanRekapitulasiDosenPerPendidikanWindow() {
		super();
		// TODO Auto-generated constructor stub
	}

	public LaporanRekapitulasiDosenPerPendidikanWindow(String title, String border,
			boolean closable) throws Exception {
		super(title, border, closable);
		// TODO Auto-generated constructor stub
	}

}
