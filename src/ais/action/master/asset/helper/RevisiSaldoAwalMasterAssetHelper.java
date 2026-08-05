package ais.action.master.asset.helper;
import ais.action.master.helper.GenericRevisiHelper;
import ais.database.model.asset.SaldoAwalMasterAsset;
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
public class RevisiSaldoAwalMasterAssetHelper extends GenericRevisiHelper<SaldoAwalMasterAsset> {

	private static final long serialVersionUID = 6589578552710016753L;
	private static final String[] SEARCH_PROPERTIES = new String[] { "kode", "nama", "keterangan" };

	private static QueryCustomizer[] buildFilters() {
		return new QueryCustomizer[0];
	}

	public RevisiSaldoAwalMasterAssetHelper(EventListener eventListener) throws Exception {
		super(SaldoAwalMasterAsset.class, "Revisi Saldo Awal Master Asset", eventListener, SEARCH_PROPERTIES, buildFilters());
	}

}
