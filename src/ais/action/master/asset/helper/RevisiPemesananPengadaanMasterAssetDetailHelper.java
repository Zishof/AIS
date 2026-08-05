package ais.action.master.asset.helper;
import ais.action.master.helper.GenericRevisiHelper;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PemesananPengadaanMasterAssetDetail;
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
public class RevisiPemesananPengadaanMasterAssetDetailHelper extends GenericRevisiHelper<PemesananPengadaanMasterAssetDetail> {

	private static final long serialVersionUID = 6589578552710016753L;
	private static final String[] SEARCH_PROPERTIES = new String[] { "nama", "kode", "keterangan", "judul", "topik", "isi", "nim", "nis", "noRegistrasi", "noUjian", "va" };

	private static QueryCustomizer[] buildFilters(PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset) {
		java.util.List<QueryCustomizer> filters = new java.util.ArrayList<QueryCustomizer>();
		if (pemesananPengadaanMasterAsset != null) {
			filters.add(new GenericRevisiHelper.FixedPropertyFilter("pemesananPengadaanMasterAsset", pemesananPengadaanMasterAsset));
		}
		return filters.toArray(new QueryCustomizer[filters.size()]);
	}

	public RevisiPemesananPengadaanMasterAssetDetailHelper(PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset, EventListener eventListener) throws Exception {
		super(PemesananPengadaanMasterAssetDetail.class, "Revisi Pemesanan Pengadaan Master Asset Detail", eventListener, SEARCH_PROPERTIES, buildFilters(pemesananPengadaanMasterAsset));
	}

}
