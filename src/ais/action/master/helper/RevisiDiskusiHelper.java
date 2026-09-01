package ais.action.master.helper;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaDiskusi;
import org.zkoss.zk.ui.event.EventListener;
/**
 * Subclass dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link ais.database.model.PertemuanPunyaDiskusi} (forum diskusi pada satu pertemuan
 * perkuliahan) — lihat Javadoc class tersebut untuk penjelasan lengkap arsitektur window, alur
 * Envers, dan fitur restore. Tidak ada override hook {@code afterRestoreInTransaction}.
 *
 * <p>Field pencarian: {@code isi}, {@code topik}, {@code nama}, {@code keterangan}. Konstruktor
 * menyaring lewat {@link GenericRevisiHelper.FixedPropertyFilter} pada property {@code pertemuan}
 * bila {@code pertemuan} diberikan — dipakai untuk menampilkan riwayat diskusi milik satu
 * {@link Pertemuan} spesifik; bila {@code null}, tidak ada penyaringan (seluruh riwayat diskusi
 * tampil).
 *
 * <p>Kompatibel Java 1.7 / source 1.6.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiDiskusiHelper extends GenericRevisiHelper<PertemuanPunyaDiskusi> {

	private static final long serialVersionUID = 6589578552710016753L;
	private static final String[] SEARCH_PROPERTIES = new String[] { "isi", "topik", "nama", "keterangan" };

	/**
	 * Membangun daftar {@link QueryCustomizer} berdasarkan {@code pertemuan}: jika {@code null}
	 * mengembalikan array kosong (tanpa penyaringan), selain itu mengembalikan satu
	 * {@link GenericRevisiHelper.FixedPropertyFilter} pada property {@code pertemuan}.
	 */
	private static QueryCustomizer[] buildFilters(Pertemuan pertemuan) {
		java.util.List<QueryCustomizer> filters = new java.util.ArrayList<QueryCustomizer>();
		if (pertemuan != null) {
			filters.add(new GenericRevisiHelper.FixedPropertyFilter("pertemuan", pertemuan));
		}
		return filters.toArray(new QueryCustomizer[filters.size()]);
	}

	/**
	 * Membuka jendela riwayat revisi {@link PertemuanPunyaDiskusi} milik satu {@link Pertemuan}.
	 *
	 * @param pertemuan pertemuan yang membatasi riwayat yang ditampilkan; bila {@code null} tidak
	 *                  ada penyaringan (seluruh riwayat diskusi tampil)
	 * @param eventListener callback yang diteruskan ke {@link GenericRevisiHelper}, boleh {@code null}
	 * @throws Exception diteruskan apa adanya dari konstruktor {@link GenericRevisiHelper}
	 */
	public RevisiDiskusiHelper(Pertemuan pertemuan, EventListener eventListener) throws Exception {
		super(PertemuanPunyaDiskusi.class, "Revisi Diskusi", eventListener, SEARCH_PROPERTIES, buildFilters(pertemuan));
	}

}
