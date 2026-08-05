package ais.action.master.asset.helper;
import ais.action.master.helper.GenericRevisiHelper;
import ais.database.model.asset.SaldoAwalMasterAsset;
import ais.database.model.asset.SaldoAwalMasterAssetDetail;
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
public class RevisiSaldoAwalMasterAssetDetailHelper extends GenericRevisiHelper<SaldoAwalMasterAssetDetail> {

	private static final long serialVersionUID = 6589578552710016753L;
	private static final String[] SEARCH_PROPERTIES = new String[] { "nama", "kode", "keterangan", "judul", "topik", "isi", "nim", "nis", "noRegistrasi", "noUjian", "va" };

	private static QueryCustomizer[] buildFilters(SaldoAwalMasterAsset saldoAwalMasterAsset) {
		java.util.List<QueryCustomizer> filters = new java.util.ArrayList<QueryCustomizer>();
		if (saldoAwalMasterAsset != null) {
			filters.add(new GenericRevisiHelper.FixedPropertyFilter("saldoAwal", saldoAwalMasterAsset));
		}
		return filters.toArray(new QueryCustomizer[filters.size()]);
	}

	public RevisiSaldoAwalMasterAssetDetailHelper(SaldoAwalMasterAsset saldoAwalMasterAsset, EventListener eventListener) throws Exception {
		super(SaldoAwalMasterAssetDetail.class, "Revisi Saldo Awal Master Asset Detail", eventListener, SEARCH_PROPERTIES, buildFilters(saldoAwalMasterAsset));
	}

}
