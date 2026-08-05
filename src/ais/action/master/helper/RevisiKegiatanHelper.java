package ais.action.master.helper;
import ais.database.model.Kegiatan;
import org.zkoss.zk.ui.event.EventListener;
/**
 * Versi generic dari helper revisi lama.
 *
 * Semua proses baca/restore revisi dipusatkan di GenericRevisiHelper<T> agar:
 * - code lebih ringkas dan mudah dirawat;
 * - semua Hibernate Session memakai openSession();
 * - semua Session ditutup di finally melalui session.clear(), session.disconnect(), dan session.close();
 * - fitur restore satu revisi dan restore massal dari tanggal tertentu tetap tersedia.
 *
 * Kompatibel Java 1.7 / source 1.6.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiKegiatanHelper extends GenericRevisiHelper<Kegiatan> {

	private static final long serialVersionUID = 6589578552710016753L;
	private static final String[] SEARCH_PROPERTIES = new String[] { "nama", "kode", "keterangan" };

	private static QueryCustomizer[] buildFilters() {
		return new QueryCustomizer[0];
	}

	public RevisiKegiatanHelper(EventListener eventListener) throws Exception {
		super(Kegiatan.class, "Revisi Kegiatan", eventListener, SEARCH_PROPERTIES, buildFilters());
	}

}
