package ais.action.master.helper;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaDiskusi;
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
public class RevisiDiskusiHelper extends GenericRevisiHelper<PertemuanPunyaDiskusi> {

	private static final long serialVersionUID = 6589578552710016753L;
	private static final String[] SEARCH_PROPERTIES = new String[] { "isi", "topik", "nama", "keterangan" };

	private static QueryCustomizer[] buildFilters(Pertemuan pertemuan) {
		java.util.List<QueryCustomizer> filters = new java.util.ArrayList<QueryCustomizer>();
		if (pertemuan != null) {
			filters.add(new GenericRevisiHelper.FixedPropertyFilter("pertemuan", pertemuan));
		}
		return filters.toArray(new QueryCustomizer[filters.size()]);
	}

	public RevisiDiskusiHelper(Pertemuan pertemuan, EventListener eventListener) throws Exception {
		super(PertemuanPunyaDiskusi.class, "Revisi Diskusi", eventListener, SEARCH_PROPERTIES, buildFilters(pertemuan));
	}

}
