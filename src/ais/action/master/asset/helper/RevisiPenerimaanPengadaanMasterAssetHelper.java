package ais.action.master.asset.helper;
import ais.action.master.helper.GenericRevisiHelper;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import org.zkoss.zk.ui.event.EventListener;
/**
 * Subclass tipis dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link PenerimaanPengadaanMasterAsset} (dokumen master Penerimaan Pengadaan Master Asset) —
 * lihat Javadoc class tersebut untuk penjelasan lengkap arsitektur window, alur Envers, dan
 * fitur restore.
 *
 * <p>Kekhasan: pencarian kata kunci hanya menyaring {@code kode}, {@code nama}, dan
 * {@code keterangan}. Tidak ada {@link QueryCustomizer} tambahan ({@link #buildFilters()} selalu
 * mengembalikan array kosong), sehingga seluruh riwayat revisi entity ini ditampilkan tanpa
 * pembatasan induk. Riwayat detail item penerimaan didokumentasikan terpisah di
 * {@link RevisiPenerimaanPengadaanMasterAssetDetailHelper}.</p>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiPenerimaanPengadaanMasterAssetHelper extends GenericRevisiHelper<PenerimaanPengadaanMasterAsset> {

	private static final long serialVersionUID = 6589578552710016753L;
	private static final String[] SEARCH_PROPERTIES = new String[] { "kode", "nama", "keterangan" };

	/** Tidak ada filter tambahan untuk dokumen master ini — selalu mengembalikan array kosong. */
	private static QueryCustomizer[] buildFilters() {
		return new QueryCustomizer[0];
	}

	/**
	 * Membuka window riwayat revisi dokumen Penerimaan Pengadaan Master Asset (seluruh data,
	 * tanpa filter induk).
	 *
	 * @param eventListener callback yang diteruskan ke {@link ais.action.master.helper.GenericRevisiHelper}.
	 */
	public RevisiPenerimaanPengadaanMasterAssetHelper(EventListener eventListener) throws Exception {
		super(PenerimaanPengadaanMasterAsset.class, "Revisi Penerimaan Pengadaan Master Asset", eventListener, SEARCH_PROPERTIES, buildFilters());
	}

}
