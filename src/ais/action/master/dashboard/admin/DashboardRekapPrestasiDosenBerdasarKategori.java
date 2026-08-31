package ais.action.master.dashboard.admin;

import ais.action.master.dashboard.helper.DashboardRekapPrestasiDosen;

/**
 * Komponen dashboard khusus untuk dashboard rekap prestasi dosen berdasar kategori. Kelas ini
 * memilih variasi data atau tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan
 * dari kelas induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * DashboardRekapPrestasiDosen}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi
 * ini; perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang
 * atau tumpang tindih.</p>
 * <p>Tipe ini sengaja tidak menambah state maupun operasi publik. Keberadaannya bukan duplikasi implementasi:
 * nama kelas dipakai sebagai penanda variasi untuk konfigurasi, binding ZK/SOAP, dependency lookup, atau
 * pemilihan perilaku polimorfik. Karena itu jangan menyalin method dari kelas induk ke sini kecuali kontraknya
 * memang berbeda.</p>
 *
 * @see DashboardRekapPrestasiDosen
 */
public class DashboardRekapPrestasiDosenBerdasarKategori extends DashboardRekapPrestasiDosen {

	/**
	 * 
	 */
	private static final long serialVersionUID = -300846775128524526L;

	public DashboardRekapPrestasiDosenBerdasarKategori() {
		super("kategori_prestasi_dosen", "kategoriPrestasiDosen");
	}

}
