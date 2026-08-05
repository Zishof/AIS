package ais.action.master.helper;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Mahasiswa;
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
public class RevisiDetailPerkuliahanDariMahasiswaHelper extends GenericRevisiHelper<Detailperkuliahan> {

	private static final long serialVersionUID = 6589578552710016753L;
	private static final String[] SEARCH_PROPERTIES = new String[] { "nama", "nim", "keterangan", "kode" };

	private static QueryCustomizer[] buildFilters(Mahasiswa mahasiswa) {
		java.util.List<QueryCustomizer> filters = new java.util.ArrayList<QueryCustomizer>();
		if (mahasiswa != null) {
			filters.add(new GenericRevisiHelper.FixedPropertyFilter("mahasiswa", mahasiswa));
		}
		return filters.toArray(new QueryCustomizer[filters.size()]);
	}

	public RevisiDetailPerkuliahanDariMahasiswaHelper(Mahasiswa mahasiswa, EventListener eventListener) throws Exception {
		super(Detailperkuliahan.class, "Revisi Detail Perkuliahan Dari Mahasiswa", eventListener, SEARCH_PROPERTIES, buildFilters(mahasiswa));
	}

}
