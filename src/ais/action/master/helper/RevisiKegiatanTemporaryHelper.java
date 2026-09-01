package ais.action.master.helper;
import ais.database.model.KegiatanTemporary;
import org.zkoss.zk.ui.event.EventListener;
/**
 * Subclass tipis dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link ais.database.model.KegiatanTemporary} (kegiatan sementara, mis. kegiatan PPDB/temporary
 * sebelum data resmi dipindah ke {@link ais.database.model.Kegiatan}) — lihat Javadoc class
 * tersebut untuk penjelasan lengkap arsitektur window, alur Envers, dan fitur restore.
 *
 * <p>Field pencarian: {@code nama}, {@code kode}, {@code keterangan}. Tidak ada
 * {@link GenericRevisiHelper.QueryCustomizer} tambahan ({@code buildFilters()} selalu
 * mengembalikan array kosong) dan tidak ada override hook {@code afterRestoreInTransaction}.
 *
 * <p>Kompatibel Java 1.7 / source 1.6.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiKegiatanTemporaryHelper extends GenericRevisiHelper<KegiatanTemporary> {

	private static final long serialVersionUID = 6589578552710016753L;
	private static final String[] SEARCH_PROPERTIES = new String[] { "nama", "kode", "keterangan" };

	/** Tidak ada penyaring tambahan untuk entity ini; selalu mengembalikan array {@link QueryCustomizer} kosong. */
	private static QueryCustomizer[] buildFilters() {
		return new QueryCustomizer[0];
	}

	/**
	 * Membuka jendela riwayat revisi {@link KegiatanTemporary}.
	 *
	 * @param eventListener callback yang diteruskan ke {@link GenericRevisiHelper}, boleh {@code null}
	 * @throws Exception diteruskan apa adanya dari konstruktor {@link GenericRevisiHelper}
	 */
	public RevisiKegiatanTemporaryHelper(EventListener eventListener) throws Exception {
		super(KegiatanTemporary.class, "Revisi Kegiatan Temporary", eventListener, SEARCH_PROPERTIES, buildFilters());
	}

}
