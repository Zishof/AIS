package ais.action.master.helper;
import ais.database.model.sekolah.PembayaranSiswa;
import ais.database.model.sekolah.PembayaranSiswaDetail;
import org.zkoss.zk.ui.event.EventListener;
/**
 * Subclass dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link ais.database.model.sekolah.PembayaranSiswaDetail} (baris detail pembayaran siswa, modul
 * sekolah) — lihat Javadoc class tersebut untuk penjelasan lengkap arsitektur window, alur Envers,
 * dan fitur restore. Tidak ada override hook {@code afterRestoreInTransaction}.
 *
 * <p>Field pencarian: {@code nama}, {@code kode}, {@code keterangan}. Konstruktor kedua
 * ({@link #RevisiDetailPembayaranSiswaHelper(EventListener, PembayaranSiswa)}) menyaring riwayat
 * hanya untuk satu {@link PembayaranSiswa} induk lewat
 * {@link GenericRevisiHelper.FixedPropertyFilter} pada property {@code pembayaranSiswa} — dipakai
 * saat window revisi dibuka dari konteks satu data pembayaran siswa spesifik, sementara konstruktor
 * pertama menampilkan riwayat seluruh detail pembayaran tanpa penyaringan.
 *
 * <p>Kompatibel Java 1.7 / source 1.6.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiDetailPembayaranSiswaHelper extends GenericRevisiHelper<PembayaranSiswaDetail> {

	private static final long serialVersionUID = 6589578552710016753L;
	private static final String[] SEARCH_PROPERTIES = new String[] { "nama", "kode", "keterangan" };

	/**
	 * Membangun daftar {@link QueryCustomizer} berdasarkan {@code pembayaranSiswa}: jika
	 * {@code null} mengembalikan array kosong (tanpa penyaringan), selain itu mengembalikan satu
	 * {@link GenericRevisiHelper.FixedPropertyFilter} pada property {@code pembayaranSiswa}.
	 */
	private static QueryCustomizer[] buildFilters(PembayaranSiswa pembayaranSiswa) {
		java.util.List<QueryCustomizer> filters = new java.util.ArrayList<QueryCustomizer>();
		if (pembayaranSiswa != null) {
			filters.add(new GenericRevisiHelper.FixedPropertyFilter("pembayaranSiswa", pembayaranSiswa));
		}
		return filters.toArray(new QueryCustomizer[filters.size()]);
	}

	/**
	 * Membuka jendela riwayat revisi {@link PembayaranSiswaDetail} tanpa penyaringan induk
	 * pembayaran (menampilkan riwayat seluruh detail pembayaran siswa).
	 *
	 * @param eventListener callback yang diteruskan ke {@link GenericRevisiHelper}, boleh {@code null}
	 * @throws Exception diteruskan apa adanya dari konstruktor {@link GenericRevisiHelper}
	 */
	public RevisiDetailPembayaranSiswaHelper(EventListener eventListener) throws Exception {
		super(PembayaranSiswaDetail.class, "Revisi Detail Pembayaran Siswa", eventListener, SEARCH_PROPERTIES, buildFilters(null));
	}

	/**
	 * Membuka jendela riwayat revisi {@link PembayaranSiswaDetail} yang disaring hanya untuk satu
	 * {@link PembayaranSiswa} induk.
	 *
	 * @param eventListener callback yang diteruskan ke {@link GenericRevisiHelper}, boleh {@code null}
	 * @param pembayaranSiswa data pembayaran siswa induk yang membatasi riwayat yang ditampilkan;
	 *                        bila {@code null} perilaku sama seperti
	 *                        {@link #RevisiDetailPembayaranSiswaHelper(EventListener)} (tanpa filter)
	 * @throws Exception diteruskan apa adanya dari konstruktor {@link GenericRevisiHelper}
	 */
	public RevisiDetailPembayaranSiswaHelper(EventListener eventListener, PembayaranSiswa pembayaranSiswa) throws Exception {
		super(PembayaranSiswaDetail.class, "Revisi Detail Pembayaran Siswa", eventListener, SEARCH_PROPERTIES, buildFilters(pembayaranSiswa));
	}

}
