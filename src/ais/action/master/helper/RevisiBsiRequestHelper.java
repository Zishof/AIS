package ais.action.master.helper;
import ais.database.model.bsi.BsiRequest;
import org.zkoss.zk.ui.event.EventListener;
/**
 * Subclass tipis dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link ais.database.model.bsi.BsiRequest} (permintaan/registrasi Virtual Account BSI untuk
 * pembayaran) — lihat Javadoc class tersebut untuk penjelasan lengkap arsitektur window, alur
 * Envers, dan fitur restore.
 *
 * <p>Field pencarian yang dipakai: {@code va} (nomor virtual account), {@code nama},
 * {@code keterangan}, dan {@code kode}. Tidak ada {@link GenericRevisiHelper.QueryCustomizer}
 * tambahan ({@code buildFilters()} selalu mengembalikan array kosong) dan tidak ada override
 * hook {@code afterRestoreInTransaction} — seluruh riwayat revisi entity ini tampil apa adanya.
 *
 * <p>Kompatibel Java 1.7 / source 1.6.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiBsiRequestHelper extends GenericRevisiHelper<BsiRequest> {

	private static final long serialVersionUID = 6589578552710016753L;
	private static final String[] SEARCH_PROPERTIES = new String[] { "va", "nama", "keterangan", "kode" };

	/** Tidak ada penyaring tambahan untuk entity ini; selalu mengembalikan array {@link QueryCustomizer} kosong. */
	private static QueryCustomizer[] buildFilters() {
		return new QueryCustomizer[0];
	}

	/**
	 * Membuka jendela riwayat revisi {@link BsiRequest}.
	 *
	 * @param eventListener callback yang diteruskan ke {@link GenericRevisiHelper}, boleh {@code null}
	 * @throws Exception diteruskan apa adanya dari konstruktor {@link GenericRevisiHelper}
	 */
	public RevisiBsiRequestHelper(EventListener eventListener) throws Exception {
		super(BsiRequest.class, "Revisi Bsi Request", eventListener, SEARCH_PROPERTIES, buildFilters());
	}

}
