package ais.action.master.payroll.helper;

import ais.database.model.Pegawai;

/**
 * Helper terfokus untuk absensi kehadiran pegawai harian. Tipe ini membungkus satu variasi kecil
 * dari alur yang lebih umum agar pemanggil memakai nama domain yang jelas dan tidak menggandakan
 * implementasi.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * ais.action.master.helper.AbsensiKehadiranPegawaiHarianHelper}. Kelas ini hanya boleh memuat perbedaan yang
 * benar-benar spesifik untuk variasi ini; perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di
 * kelas induk agar fungsi tidak bercabang atau tumpang tindih.</p>
 * <p>Tipe ini sengaja tidak menambah state maupun operasi publik. Keberadaannya bukan duplikasi implementasi:
 * nama kelas dipakai sebagai penanda variasi untuk konfigurasi, binding ZK/SOAP, dependency lookup, atau
 * pemilihan perilaku polimorfik. Karena itu jangan menyalin method dari kelas induk ke sini kecuali kontraknya
 * memang berbeda.</p>
 *
 * @see ais.action.master.helper.AbsensiKehadiranPegawaiHarianHelper
 */
public class AbsensiKehadiranPegawaiHarianHelper extends ais.action.master.helper.AbsensiKehadiranPegawaiHarianHelper {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public AbsensiKehadiranPegawaiHarianHelper(Pegawai pegawai) {
		super(pegawai);
	}

}
