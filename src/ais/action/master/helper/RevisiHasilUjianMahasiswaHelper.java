package ais.action.master.helper;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.HasilUjianMahasiswaDetail;
import org.zkoss.zk.ui.event.EventListener;
/**
 * Subclass dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link ais.database.model.HasilUjianMahasiswaDetail} (baris detail hasil ujian mahasiswa per
 * soal/komponen penilaian) — lihat Javadoc class tersebut untuk penjelasan lengkap arsitektur
 * window, alur Envers, dan fitur restore. Tidak ada override hook {@code afterRestoreInTransaction}.
 *
 * <p>Field pencarian: {@code nama}, {@code kode}, {@code keterangan}. Konstruktor menyaring lewat
 * {@link GenericRevisiHelper.FixedPropertyFilter} pada property {@code hasilUjianMahasiswa} bila
 * {@link HasilUjianMahasiswa} induk diberikan — dipakai untuk menampilkan riwayat detail hasil
 * ujian milik satu rekap ujian mahasiswa tertentu; bila {@code null}, tidak ada penyaringan
 * (seluruh riwayat detail hasil ujian tampil).
 *
 * <p>Kompatibel Java 1.7 / source 1.6.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiHasilUjianMahasiswaHelper extends GenericRevisiHelper<HasilUjianMahasiswaDetail> {

	private static final long serialVersionUID = 6589578552710016753L;
	private static final String[] SEARCH_PROPERTIES = new String[] { "nama", "kode", "keterangan" };

	/**
	 * Membangun daftar {@link QueryCustomizer} berdasarkan {@code hasilUjianMahasiswa}: jika
	 * {@code null} mengembalikan array kosong (tanpa penyaringan), selain itu mengembalikan satu
	 * {@link GenericRevisiHelper.FixedPropertyFilter} pada property {@code hasilUjianMahasiswa}.
	 */
	private static QueryCustomizer[] buildFilters(HasilUjianMahasiswa hasilUjianMahasiswa) {
		java.util.List<QueryCustomizer> filters = new java.util.ArrayList<QueryCustomizer>();
		if (hasilUjianMahasiswa != null) {
			filters.add(new GenericRevisiHelper.FixedPropertyFilter("hasilUjianMahasiswa", hasilUjianMahasiswa));
		}
		return filters.toArray(new QueryCustomizer[filters.size()]);
	}

	/**
	 * Membuka jendela riwayat revisi {@link HasilUjianMahasiswaDetail} milik satu
	 * {@link HasilUjianMahasiswa}.
	 *
	 * @param hasilUjianMahasiswa rekap hasil ujian yang membatasi riwayat yang ditampilkan; bila
	 *                            {@code null} tidak ada penyaringan (seluruh riwayat detail tampil)
	 * @param eventListener callback yang diteruskan ke {@link GenericRevisiHelper}, boleh {@code null}
	 * @throws Exception diteruskan apa adanya dari konstruktor {@link GenericRevisiHelper}
	 */
	public RevisiHasilUjianMahasiswaHelper(HasilUjianMahasiswa hasilUjianMahasiswa, EventListener eventListener) throws Exception {
		super(HasilUjianMahasiswaDetail.class, "Revisi Hasil Ujian Mahasiswa", eventListener, SEARCH_PROPERTIES, buildFilters(hasilUjianMahasiswa));
	}

}
