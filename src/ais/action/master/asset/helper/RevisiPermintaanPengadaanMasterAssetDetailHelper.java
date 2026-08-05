package ais.action.master.asset.helper;
import ais.action.master.helper.GenericRevisiHelper;
import ais.database.model.asset.PermintaanPengadaanMasterAsset;
import ais.database.model.asset.PermintaanPengadaanMasterAssetDetail;
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
public class RevisiPermintaanPengadaanMasterAssetDetailHelper extends GenericRevisiHelper<PermintaanPengadaanMasterAssetDetail> {

	private static final long serialVersionUID = 6589578552710016753L;
	private static final String[] SEARCH_PROPERTIES = new String[] { "nama", "kode", "keterangan", "judul", "topik", "isi", "nim", "nis", "noRegistrasi", "noUjian", "va" };

	private static QueryCustomizer[] buildFilters(PermintaanPengadaanMasterAsset permintaanPengadaanMasterAsset) {
		java.util.List<QueryCustomizer> filters = new java.util.ArrayList<QueryCustomizer>();
		if (permintaanPengadaanMasterAsset != null) {
			filters.add(new GenericRevisiHelper.FixedPropertyFilter("permintaanPengadaanMasterAsset", permintaanPengadaanMasterAsset));
		}
		return filters.toArray(new QueryCustomizer[filters.size()]);
	}

	public RevisiPermintaanPengadaanMasterAssetDetailHelper(PermintaanPengadaanMasterAsset permintaanPengadaanMasterAsset, EventListener eventListener) throws Exception {
		super(PermintaanPengadaanMasterAssetDetail.class, "Revisi Permintaan Pengadaan Master Asset Detail", eventListener, SEARCH_PROPERTIES, buildFilters(permintaanPengadaanMasterAsset));
	}

}
