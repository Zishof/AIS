package ais.action.master.helper;
import org.zkoss.zk.ui.event.EventListener;

import ais.database.model.akunting.GrupTransaksi;
/**
 * Subclass tipis dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link ais.database.model.akunting.GrupTransaksi} (kelompok/kategori transaksi akunting) —
 * lihat Javadoc class tersebut untuk penjelasan lengkap arsitektur window, alur Envers, dan
 * fitur restore.
 *
 * <p>Field pencarian: {@code kode}, {@code nama}, {@code keterangan}, {@code jenis}. Tidak ada
 * {@link GenericRevisiHelper.QueryCustomizer} tambahan ({@code buildFilters()} selalu
 * mengembalikan array kosong) dan tidak ada override hook {@code afterRestoreInTransaction}.
 *
 * <p>Kompatibel Java 1.7 / source 1.6.
 */
@SuppressWarnings({ })
public class RevisiGrupTransaksiHelper extends GenericRevisiHelper<GrupTransaksi> {

	private static final long serialVersionUID = 6589578552710016753L;
	private static final String[] SEARCH_PROPERTIES = new String[] { "kode", "nama", "keterangan", "jenis" };

	/** Tidak ada penyaring tambahan untuk entity ini; selalu mengembalikan array {@link QueryCustomizer} kosong. */
	private static QueryCustomizer[] buildFilters() {
		return new QueryCustomizer[0];
	}

	/**
	 * Membuka jendela riwayat revisi {@link GrupTransaksi}.
	 *
	 * @param eventListener callback yang diteruskan ke {@link GenericRevisiHelper}, boleh {@code null}
	 * @throws Exception diteruskan apa adanya dari konstruktor {@link GenericRevisiHelper}
	 */
	public RevisiGrupTransaksiHelper(EventListener eventListener) throws Exception {
		super(GrupTransaksi.class, "Revisi Grup Transaksi", eventListener, SEARCH_PROPERTIES, buildFilters());
	}

}
