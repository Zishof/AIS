package ais.action.master.helper;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Mahasiswa;
import org.zkoss.zk.ui.event.EventListener;
/**
 * Subclass dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link ais.database.model.Detailperkuliahan} (baris nilai/detail perkuliahan mahasiswa per
 * matakuliah), disaring dari sudut pandang SATU {@link ais.database.model.Mahasiswa} — lihat
 * Javadoc class tersebut untuk penjelasan lengkap arsitektur window, alur Envers, dan fitur
 * restore. Tidak ada override hook {@code afterRestoreInTransaction}.
 *
 * <p>Field pencarian: {@code nama}, {@code nim}, {@code keterangan}, {@code kode}. Berbeda dari
 * {@link RevisiDetailPerkuliahanHelper} (yang menyaring per {@link ais.database.model.Perkuliahan}),
 * class ini SELALU menyaring lewat {@link GenericRevisiHelper.FixedPropertyFilter} pada property
 * {@code mahasiswa} — dipakai untuk menampilkan seluruh riwayat detail perkuliahan milik satu
 * mahasiswa lintas semua perkuliahan yang pernah diikutinya. Bila {@code mahasiswa} bernilai
 * {@code null}, tidak ada penyaringan diterapkan (perilaku sama seperti tanpa filter).
 *
 * <p>Kompatibel Java 1.7 / source 1.6.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiDetailPerkuliahanDariMahasiswaHelper extends GenericRevisiHelper<Detailperkuliahan> {

	private static final long serialVersionUID = 6589578552710016753L;
	private static final String[] SEARCH_PROPERTIES = new String[] { "nama", "nim", "keterangan", "kode" };

	/**
	 * Membangun daftar {@link QueryCustomizer} berdasarkan {@code mahasiswa}: jika {@code null}
	 * mengembalikan array kosong (tanpa penyaringan), selain itu mengembalikan satu
	 * {@link GenericRevisiHelper.FixedPropertyFilter} pada property {@code mahasiswa}.
	 */
	private static QueryCustomizer[] buildFilters(Mahasiswa mahasiswa) {
		java.util.List<QueryCustomizer> filters = new java.util.ArrayList<QueryCustomizer>();
		if (mahasiswa != null) {
			filters.add(new GenericRevisiHelper.FixedPropertyFilter("mahasiswa", mahasiswa));
		}
		return filters.toArray(new QueryCustomizer[filters.size()]);
	}

	/**
	 * Membuka jendela riwayat revisi {@link Detailperkuliahan} milik satu {@link Mahasiswa}.
	 *
	 * @param mahasiswa mahasiswa yang membatasi riwayat yang ditampilkan; bila {@code null} tidak
	 *                  ada penyaringan (seluruh riwayat detail perkuliahan tampil)
	 * @param eventListener callback yang diteruskan ke {@link GenericRevisiHelper}, boleh {@code null}
	 * @throws Exception diteruskan apa adanya dari konstruktor {@link GenericRevisiHelper}
	 */
	public RevisiDetailPerkuliahanDariMahasiswaHelper(Mahasiswa mahasiswa, EventListener eventListener) throws Exception {
		super(Detailperkuliahan.class, "Revisi Detail Perkuliahan Dari Mahasiswa", eventListener, SEARCH_PROPERTIES, buildFilters(mahasiswa));
	}

}
