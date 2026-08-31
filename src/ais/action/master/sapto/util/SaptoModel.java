package ais.action.master.sapto.util;

import java.util.List;
import java.util.Map;

/**
 * Kontrak umum bagi seluruh laporan SAPTO/borang akreditasi BAN-PT (kelas {@code Laporan*_A_X_Y}
 * pada paket {@code ais.action.master.sapto}). Setiap implementasi merekap data institusi sesuai
 * butir borang tertentu menjadi struktur baris/kolom sederhana yang siap dituangkan ke dokumen
 * SAPTO (mis. Excel/dokumen borang).
 */
public interface SaptoModel {
	/**
	 * Menghasilkan data laporan sebagai daftar baris, masing-masing baris berupa daftar nilai kolom
	 * (mentah, belum diformat tampilan).
	 *
	 * @param parameters parameter filter/konteks laporan (mis. tahun akademik, program studi),
	 *                    bergantung pada implementasi
	 * @return daftar baris data laporan; setiap baris adalah daftar nilai kolom
	 */
	@SuppressWarnings("rawtypes")
	public List<List> generateData(Map<String, Object> parameters);

}
