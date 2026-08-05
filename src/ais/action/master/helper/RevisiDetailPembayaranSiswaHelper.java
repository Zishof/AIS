package ais.action.master.helper;
import ais.database.model.sekolah.PembayaranSiswa;
import ais.database.model.sekolah.PembayaranSiswaDetail;
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
public class RevisiDetailPembayaranSiswaHelper extends GenericRevisiHelper<PembayaranSiswaDetail> {

	private static final long serialVersionUID = 6589578552710016753L;
	private static final String[] SEARCH_PROPERTIES = new String[] { "nama", "kode", "keterangan" };

	private static QueryCustomizer[] buildFilters(PembayaranSiswa pembayaranSiswa) {
		java.util.List<QueryCustomizer> filters = new java.util.ArrayList<QueryCustomizer>();
		if (pembayaranSiswa != null) {
			filters.add(new GenericRevisiHelper.FixedPropertyFilter("pembayaranSiswa", pembayaranSiswa));
		}
		return filters.toArray(new QueryCustomizer[filters.size()]);
	}

	public RevisiDetailPembayaranSiswaHelper(EventListener eventListener) throws Exception {
		super(PembayaranSiswaDetail.class, "Revisi Detail Pembayaran Siswa", eventListener, SEARCH_PROPERTIES, buildFilters(null));
	}

	public RevisiDetailPembayaranSiswaHelper(EventListener eventListener, PembayaranSiswa pembayaranSiswa) throws Exception {
		super(PembayaranSiswaDetail.class, "Revisi Detail Pembayaran Siswa", eventListener, SEARCH_PROPERTIES, buildFilters(pembayaranSiswa));
	}

}
