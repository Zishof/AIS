package ais.action.report.helper.akademik;

import ais.action.master.dashboard.admin.DashboardPenilaianMahasiswa;
import ais.database.model.Perkuliahan;

/**
 * Tipe khusus untuk laporan rekap penilaian mahasiswa window. Kelas ini memberi nama dan batas
 * tanggung jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang
 * diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * DashboardPenilaianMahasiswa}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi
 * ini; perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang
 * atau tumpang tindih.</p>
 * <p>Tipe ini sengaja tidak menambah state maupun operasi publik. Keberadaannya bukan duplikasi implementasi:
 * nama kelas dipakai sebagai penanda variasi untuk konfigurasi, binding ZK/SOAP, dependency lookup, atau
 * pemilihan perilaku polimorfik. Karena itu jangan menyalin method dari kelas induk ke sini kecuali kontraknya
 * memang berbeda.</p>
 *
 * @see DashboardPenilaianMahasiswa
 */
public class LaporanRekapPenilaianMahasiswaWindow extends DashboardPenilaianMahasiswa {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5328962733937540906L;

	public LaporanRekapPenilaianMahasiswaWindow() {
		super();
		// TODO Auto-generated constructor stub
	}

	public LaporanRekapPenilaianMahasiswaWindow(Perkuliahan perkuliahan) {
		super(perkuliahan);
		// TODO Auto-generated constructor stub
	}

	public LaporanRekapPenilaianMahasiswaWindow(String title, String border, boolean closable) {
		super(title, border, closable);
		// TODO Auto-generated constructor stub
	}

	
	
	
}
