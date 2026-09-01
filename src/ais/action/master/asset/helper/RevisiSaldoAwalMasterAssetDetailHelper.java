package ais.action.master.asset.helper;
import ais.action.master.helper.GenericRevisiHelper;
import ais.database.model.asset.SaldoAwalMasterAsset;
import ais.database.model.asset.SaldoAwalMasterAssetDetail;
import org.zkoss.zk.ui.event.EventListener;
/**
 * Subclass tipis dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link SaldoAwalMasterAssetDetail} (baris detail item pada satu dokumen Saldo Awal Master
 * Asset) — lihat Javadoc class tersebut untuk penjelasan lengkap arsitektur window, alur Envers,
 * dan fitur restore.
 *
 * <p>Kekhasan: {@code SEARCH_PROPERTIES} memakai daftar property gabungan yang dipakai bersama
 * beberapa helper detail asset lain di package ini (tidak semuanya relevan untuk entity ini,
 * mis. {@code nim}/{@code nis}/{@code noRegistrasi}/{@code noUjian}/{@code va} adalah sisa
 * template dari helper akademik — dibiarkan apa adanya agar tidak mengubah perilaku pencarian
 * yang sudah berjalan). Filter utama dipasang lewat {@link GenericRevisiHelper.FixedPropertyFilter}
 * pada property {@code saldoAwal} milik entity detail ini (perhatikan: nama property Hibernate
 * adalah {@code saldoAwal}, BUKAN {@code saldoAwalMasterAsset} — beda dari pola penamaan filter
 * pada helper detail asset lainnya di package ini). Bila konstruktor diberi induk
 * {@link SaldoAwalMasterAsset} yang tidak {@code null}, riwayat yang ditampilkan hanya milik
 * detail-detail yang menunjuk ke induk tersebut; bila {@code null}, riwayat SEMUA detail (lintas
 * induk) ditampilkan tanpa pembatasan.</p>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiSaldoAwalMasterAssetDetailHelper extends GenericRevisiHelper<SaldoAwalMasterAssetDetail> {

	private static final long serialVersionUID = 6589578552710016753L;
	private static final String[] SEARCH_PROPERTIES = new String[] { "nama", "kode", "keterangan", "judul", "topik", "isi", "nim", "nis", "noRegistrasi", "noUjian", "va" };

	/**
	 * Menyusun filter {@link QueryCustomizer} untuk membatasi riwayat pada satu induk Saldo Awal
	 * Master Asset, lewat property Hibernate {@code saldoAwal} pada entity detail. Mengembalikan
	 * array kosong (tanpa filter) bila {@code saldoAwalMasterAsset} bernilai {@code null}.
	 *
	 * @param saldoAwalMasterAsset induk detail yang riwayatnya ingin dibatasi; boleh {@code null}.
	 * @return array {@link QueryCustomizer} berisi satu {@link GenericRevisiHelper.FixedPropertyFilter}
	 *         bila induk diberikan, atau array kosong bila tidak.
	 */
	private static QueryCustomizer[] buildFilters(SaldoAwalMasterAsset saldoAwalMasterAsset) {
		java.util.List<QueryCustomizer> filters = new java.util.ArrayList<QueryCustomizer>();
		if (saldoAwalMasterAsset != null) {
			filters.add(new GenericRevisiHelper.FixedPropertyFilter("saldoAwal", saldoAwalMasterAsset));
		}
		return filters.toArray(new QueryCustomizer[filters.size()]);
	}

	/**
	 * Membuka window riwayat revisi detail Saldo Awal Master Asset.
	 *
	 * @param saldoAwalMasterAsset induk yang membatasi riwayat detail yang ditampilkan; boleh
	 *                             {@code null} untuk menampilkan riwayat seluruh detail.
	 * @param eventListener        callback yang diteruskan ke {@link ais.action.master.helper.GenericRevisiHelper}.
	 */
	public RevisiSaldoAwalMasterAssetDetailHelper(SaldoAwalMasterAsset saldoAwalMasterAsset, EventListener eventListener) throws Exception {
		super(SaldoAwalMasterAssetDetail.class, "Revisi Saldo Awal Master Asset Detail", eventListener, SEARCH_PROPERTIES, buildFilters(saldoAwalMasterAsset));
	}

}
