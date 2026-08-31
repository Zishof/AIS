package ais.action.master;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.model.Konfigurasi;

/**
 * Controller/action ZK untuk konfigurasi tampilan biodata mahasiswa. Tipe ini merupakan titik
 * masuk UI yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi
 * khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * KonfigurasiNewAction}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}); pembacaan/pencarian ({@code onTampil()}); operasi domain lain ({@code keyDesc()}, {@code
 * apakahAdaTidakWajib()}, {@code wajibIsi()}, {@code statusWajibIsi()}, {@code dataYangWajibDiisi()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see KonfigurasiNewAction
 */
public class KonfigurasiTampilanBiodataMahasiswaAction extends KonfigurasiNewAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterComposeOri(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		onTampil();

	}

	public static String keyDesc(String key) {
		int index = 0;
		for (String h : BiodataMahasiswaAction.DATA) {
			if (key.equalsIgnoreCase(h)) {
				return BiodataMahasiswaAction.DATA_DESC[index];
			}
			index++;
		}
		return "";
	}

	public static boolean apakahAdaTidakWajib(String key) {
		boolean ada = false;
		for (String h : BiodataMahasiswaAction.DEFAULT_TIDAK_WAJIB) {
			if (key.equalsIgnoreCase(h)) {
				ada = true;
				break;
			}
		}
		return ada;
	}

	public static boolean wajibIsi(String key) {
		return statusWajibIsi(key).equals(Konfigurasi.AKTIF);
	}

	public static String statusWajibIsi(String key) {
		String defaultValue = apakahAdaTidakWajib(key) ? Konfigurasi.AKTIF_TIDAK_WAJIB : Konfigurasi.AKTIF;
		Konfigurasi konfigurasi = Common.getKonfigurasi("biodata_mahasiswa_tampil_" + key, defaultValue);
		return konfigurasi.getNilai();
	}

	public static List<String> dataYangWajibDiisi() {
		List<String> strings = new ArrayList<String>();

		for (String key : BiodataMahasiswaAction.DATA) {
			if (wajibIsi(key)) {
				strings.add(key);
			}
		}

		return strings;
	}

	public void onTampil() {

		Rows rows = (createSpan("Form Biodata Mahasiswa"));

		int index = 0;
		for (String key : BiodataMahasiswaAction.DATA) {
			if (apakahAdaTidakWajib(key)) {
				rows.appendChild(createRowActiveDefault(
						"Apakah \"" + BiodataMahasiswaAction.DATA_DESC[index] + "\" tampil di biodata mahasiswa ?",
						"biodata_mahasiswa_tampil_" + key, Konfigurasi.AKTIF_TIDAK_WAJIB, createComboActive(true)));
			} else {
				rows.appendChild(createRowActive(
						"Apakah \"" + BiodataMahasiswaAction.DATA_DESC[index] + "\" tampil di biodata mahasiswa ?",
						"biodata_mahasiswa_tampil_" + key, createComboActive(true)));
			}
			index++;
		}
		
		rows.appendChild(createRowActiveDefault("KTP Mahasiswa harus 16 karakter", "ktp_mahasiswa_harus_16_karakter",
				Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("NISN Mahasiswa harus 10 karakter", "nisn_mahasiswa_harus_10_karakter",
				Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault("Upload Foto mahasiswa wajib dilakukan", "foto_mahasiswa_wajib_diisi",
				Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Upload KTP/NIK mahasiswa wajib dilakukan",
				"upload_ktp_mahasiswa_wajib_diisi", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Upload Akte Kelahiran mahasiswa wajib dilakukan",
				"upload_akte_mahasiswa_wajib_diisi", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Upload NPWP mahasiswa wajib dilakukan",
				"upload_npwp_mahasiswa_wajib_diisi", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(createRowActiveDefault("Upload Ijazah Pendidikan Sebelumnya mahasiswa wajib dilakukan",
				"upload_ijazah_mahasiswa_wajib_diisi", Konfigurasi.TIDAK_AKTIF));
		rows.appendChild(
				createRowActiveDefault("Upload Transkrip Nilai Pendidikan Sebelumnya mahasiswa wajib dilakukan",
						"upload_transkrip_nilai_mahasiswa_wajib_diisi", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault("Upload Kartu Keluarga (KK) mahasiswa wajib dilakukan",
				"upload_kk_mahasiswa_wajib_diisi", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault("Upload KTP Ayah wajib dilakukan",
				"upload_ktp_ayah_mahasiswa_wajib_diisi", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault("Upload KTP Ibu wajib dilakukan",
				"upload_ktp_ibu_mahasiswa_wajib_diisi", Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault("Upload KTP Wali wajib dilakukan",
				"upload_ktp_wali_mahasiswa_wajib_diisi", Konfigurasi.TIDAK_AKTIF));

	}
}
