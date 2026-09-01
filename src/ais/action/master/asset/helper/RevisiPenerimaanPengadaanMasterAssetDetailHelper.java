package ais.action.master.asset.helper;
import ais.action.master.helper.GenericRevisiHelper;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAssetDetail;
import org.zkoss.zk.ui.event.EventListener;
/**
 * Subclass tipis dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link PenerimaanPengadaanMasterAssetDetail} (baris detail item pada satu Penerimaan Pengadaan
 * Master Asset) — lihat Javadoc class tersebut untuk penjelasan lengkap arsitektur window, alur
 * Envers, dan fitur restore.
 *
 * <p>Kekhasan: {@code SEARCH_PROPERTIES} memakai daftar property gabungan yang dipakai bersama
 * beberapa helper detail asset lain di package ini (tidak semuanya relevan untuk entity ini,
 * mis. {@code nim}/{@code nis}/{@code noRegistrasi}/{@code noUjian}/{@code va} adalah sisa
 * template dari helper akademik — dibiarkan apa adanya agar tidak mengubah perilaku pencarian
 * yang sudah berjalan). Filter utama dipasang lewat {@link GenericRevisiHelper.FixedPropertyFilter}
 * pada property {@code penerimaanPengadaanMasterAsset}: bila konstruktor diberi induk
 * {@link PenerimaanPengadaanMasterAsset} yang tidak {@code null}, riwayat yang ditampilkan hanya
 * milik detail-detail yang menunjuk ke induk tersebut; bila {@code null}, riwayat SEMUA detail
 * (lintas induk) ditampilkan tanpa pembatasan.</p>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiPenerimaanPengadaanMasterAssetDetailHelper extends GenericRevisiHelper<PenerimaanPengadaanMasterAssetDetail> {

	private static final long serialVersionUID = 6589578552710016753L;
	private static final String[] SEARCH_PROPERTIES = new String[] { "nama", "kode", "keterangan", "judul", "topik", "isi", "nim", "nis", "noRegistrasi", "noUjian", "va" };

	/**
	 * Menyusun filter {@link QueryCustomizer} untuk membatasi riwayat pada satu induk Penerimaan
	 * Pengadaan Master Asset. Mengembalikan array kosong (tanpa filter) bila
	 * {@code penerimaanPengadaanMasterAsset} bernilai {@code null}.
	 *
	 * @param penerimaanPengadaanMasterAsset induk detail yang riwayatnya ingin dibatasi; boleh {@code null}.
	 * @return array {@link QueryCustomizer} berisi satu {@link GenericRevisiHelper.FixedPropertyFilter}
	 *         bila induk diberikan, atau array kosong bila tidak.
	 */
	private static QueryCustomizer[] buildFilters(PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset) {
		java.util.List<QueryCustomizer> filters = new java.util.ArrayList<QueryCustomizer>();
		if (penerimaanPengadaanMasterAsset != null) {
			filters.add(new GenericRevisiHelper.FixedPropertyFilter("penerimaanPengadaanMasterAsset", penerimaanPengadaanMasterAsset));
		}
		return filters.toArray(new QueryCustomizer[filters.size()]);
	}

	/**
	 * Membuka window riwayat revisi detail Penerimaan Pengadaan Master Asset.
	 *
	 * @param penerimaanPengadaanMasterAsset induk yang membatasi riwayat detail yang ditampilkan; boleh
	 *                                       {@code null} untuk menampilkan riwayat seluruh detail.
	 * @param eventListener                  callback yang diteruskan ke {@link ais.action.master.helper.GenericRevisiHelper}.
	 */
	public RevisiPenerimaanPengadaanMasterAssetDetailHelper(PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset, EventListener eventListener) throws Exception {
		super(PenerimaanPengadaanMasterAssetDetail.class, "Revisi Penerimaan Pengadaan Master Asset Detail", eventListener, SEARCH_PROPERTIES, buildFilters(penerimaanPengadaanMasterAsset));
	}

}
