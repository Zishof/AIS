package ais.action.master.helper;
import org.zkoss.zk.ui.event.EventListener;

import ais.database.model.akunting.GrupTransaksi;
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
@SuppressWarnings({ })
public class RevisiGrupTransaksiHelper extends GenericRevisiHelper<GrupTransaksi> {

	private static final long serialVersionUID = 6589578552710016753L;
	private static final String[] SEARCH_PROPERTIES = new String[] { "kode", "nama", "keterangan", "jenis" };

	private static QueryCustomizer[] buildFilters() {
		return new QueryCustomizer[0];
	}

	public RevisiGrupTransaksiHelper(EventListener eventListener) throws Exception {
		super(GrupTransaksi.class, "Revisi Grup Transaksi", eventListener, SEARCH_PROPERTIES, buildFilters());
	}

}
