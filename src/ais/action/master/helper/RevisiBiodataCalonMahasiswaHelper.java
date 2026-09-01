package ais.action.master.helper;
import ais.database.model.BiodataCalonMahasiswa;
import org.zkoss.zk.ui.event.EventListener;
/**
 * Subclass tipis dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link ais.database.model.BiodataCalonMahasiswa} (data pendaftaran/biodata calon mahasiswa,
 * mis. PPDB) — lihat Javadoc class tersebut untuk penjelasan lengkap arsitektur window, alur
 * Envers, dan fitur restore.
 *
 * <p>Field pencarian yang dipakai: {@code nama}, {@code noRegistrasi}, {@code noUjian},
 * {@code email}, {@code program}, {@code semesterMulai}, dan {@code keterangan} — mencakup
 * identitas utama calon mahasiswa sekaligus jalur pendaftarannya. Tidak ada
 * {@link GenericRevisiHelper.QueryCustomizer} tambahan ({@code buildFilters()} selalu
 * mengembalikan array kosong) sehingga seluruh riwayat revisi entity ini tampil tanpa
 * penyaringan khusus, dan tidak ada override hook {@code afterRestoreInTransaction}.
 *
 * <p>Kompatibel Java 1.7 / source 1.6.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiBiodataCalonMahasiswaHelper extends GenericRevisiHelper<BiodataCalonMahasiswa> {

	private static final long serialVersionUID = 6589578552710016753L;
	private static final String[] SEARCH_PROPERTIES = new String[] { "nama", "noRegistrasi", "noUjian", "email", "program", "semesterMulai", "keterangan" };

	/** Tidak ada penyaring tambahan untuk entity ini; selalu mengembalikan array {@link QueryCustomizer} kosong. */
	private static QueryCustomizer[] buildFilters() {
		return new QueryCustomizer[0];
	}

	/**
	 * Membuka jendela riwayat revisi {@link BiodataCalonMahasiswa}.
	 *
	 * @param eventListener callback yang diteruskan ke {@link GenericRevisiHelper}, boleh {@code null}
	 * @throws Exception diteruskan apa adanya dari konstruktor {@link GenericRevisiHelper}
	 */
	public RevisiBiodataCalonMahasiswaHelper(EventListener eventListener) throws Exception {
		super(BiodataCalonMahasiswa.class, "Revisi Biodata Calon Mahasiswa", eventListener, SEARCH_PROPERTIES, buildFilters());
	}

}
