package ais.action.master.helper;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Perkuliahan;
import org.zkoss.zk.ui.event.EventListener;
/**
 * Subclass dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link ais.database.model.Detailperkuliahan} (baris nilai/detail perkuliahan mahasiswa per
 * matakuliah), disaring dari sudut pandang SATU {@link ais.database.model.Perkuliahan} — lihat
 * Javadoc class tersebut untuk penjelasan lengkap arsitektur window, alur Envers, dan fitur
 * restore. Tidak ada override hook {@code afterRestoreInTransaction}.
 *
 * <p>Field pencarian: {@code nama}, {@code nim}, {@code keterangan}, {@code kode}. Berbeda dari
 * {@link RevisiDetailPerkuliahanDariMahasiswaHelper} (yang menyaring per
 * {@link ais.database.model.Mahasiswa}), class ini SELALU menyaring lewat
 * {@link GenericRevisiHelper.FixedPropertyFilter} pada property {@code perkuliahan} — dipakai
 * untuk menampilkan seluruh riwayat detail perkuliahan pada satu kelas/perkuliahan tertentu, lintas
 * semua mahasiswa peserta. Bila {@code perkuliahan} bernilai {@code null}, tidak ada penyaringan
 * diterapkan (perilaku sama seperti tanpa filter).
 *
 * <p>Kompatibel Java 1.7 / source 1.6.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiDetailPerkuliahanHelper extends GenericRevisiHelper<Detailperkuliahan> {

	private static final long serialVersionUID = 6589578552710016753L;
	private static final String[] SEARCH_PROPERTIES = new String[] { "nama", "nim", "keterangan", "kode" };

	/**
	 * Membangun daftar {@link QueryCustomizer} berdasarkan {@code perkuliahan}: jika {@code null}
	 * mengembalikan array kosong (tanpa penyaringan), selain itu mengembalikan satu
	 * {@link GenericRevisiHelper.FixedPropertyFilter} pada property {@code perkuliahan}.
	 */
	private static QueryCustomizer[] buildFilters(Perkuliahan perkuliahan) {
		java.util.List<QueryCustomizer> filters = new java.util.ArrayList<QueryCustomizer>();
		if (perkuliahan != null) {
			filters.add(new GenericRevisiHelper.FixedPropertyFilter("perkuliahan", perkuliahan));
		}
		return filters.toArray(new QueryCustomizer[filters.size()]);
	}

	/**
	 * Membuka jendela riwayat revisi {@link Detailperkuliahan} milik satu {@link Perkuliahan}.
	 *
	 * @param perkuliahan perkuliahan yang membatasi riwayat yang ditampilkan; bila {@code null}
	 *                    tidak ada penyaringan (seluruh riwayat detail perkuliahan tampil)
	 * @param eventListener callback yang diteruskan ke {@link GenericRevisiHelper}, boleh {@code null}
	 * @throws Exception diteruskan apa adanya dari konstruktor {@link GenericRevisiHelper}
	 */
	public RevisiDetailPerkuliahanHelper(Perkuliahan perkuliahan, EventListener eventListener) throws Exception {
		super(Detailperkuliahan.class, "Revisi Detail Perkuliahan", eventListener, SEARCH_PROPERTIES, buildFilters(perkuliahan));
	}

}
