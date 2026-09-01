package ais.action.master.asset.helper;
import ais.action.master.helper.GenericRevisiHelper;
import ais.database.model.asset.SaldoAwalMasterAsset;
import org.zkoss.zk.ui.event.EventListener;
/**
 * Subclass tipis dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link SaldoAwalMasterAsset} (dokumen master Saldo Awal Master Asset) — lihat Javadoc class
 * tersebut untuk penjelasan lengkap arsitektur window, alur Envers, dan fitur restore.
 *
 * <p>Kekhasan: pencarian kata kunci hanya menyaring {@code kode}, {@code nama}, dan
 * {@code keterangan}. Tidak ada {@link QueryCustomizer} tambahan ({@link #buildFilters()} selalu
 * mengembalikan array kosong), sehingga seluruh riwayat revisi entity ini ditampilkan tanpa
 * pembatasan induk. Riwayat detail item saldo awal didokumentasikan terpisah di
 * {@link RevisiSaldoAwalMasterAssetDetailHelper}.</p>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiSaldoAwalMasterAssetHelper extends GenericRevisiHelper<SaldoAwalMasterAsset> {

	private static final long serialVersionUID = 6589578552710016753L;
	private static final String[] SEARCH_PROPERTIES = new String[] { "kode", "nama", "keterangan" };

	/** Tidak ada filter tambahan untuk dokumen master ini — selalu mengembalikan array kosong. */
	private static QueryCustomizer[] buildFilters() {
		return new QueryCustomizer[0];
	}

	/**
	 * Membuka window riwayat revisi dokumen Saldo Awal Master Asset (seluruh data, tanpa filter
	 * induk).
	 *
	 * @param eventListener callback yang diteruskan ke {@link ais.action.master.helper.GenericRevisiHelper}.
	 */
	public RevisiSaldoAwalMasterAssetHelper(EventListener eventListener) throws Exception {
		super(SaldoAwalMasterAsset.class, "Revisi Saldo Awal Master Asset", eventListener, SEARCH_PROPERTIES, buildFilters());
	}

}
