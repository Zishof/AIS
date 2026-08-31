package ais.action.master;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Rows;

import ais.action.master.sekolah.CalonSiswaAction;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;

/**
 * Controller/action ZK untuk konfigurasi tampilan biodata calon siswa. Tipe ini merupakan titik
 * masuk UI yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi
 * khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * KonfigurasiNewAction}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code List tidakdicheck};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}); pembacaan/pencarian ({@code
 * onTampil()}); operasi domain lain ({@code keyDesc()}, {@code apakahAdaTidakAktif()}, {@code
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
public class KonfigurasiTampilanBiodataCalonSiswaAction extends KonfigurasiNewAction {

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
		for (String h : CalonSiswaAction.DATA) {
			if (key.equalsIgnoreCase(h)) {
				return CalonSiswaAction.DATA_DESC[index];
			}
			index++;
		}
		return "";
	}

	public static boolean apakahAdaTidakAktif(String key) {
		boolean ada = false;
		for (String h : CalonSiswaAction.DEFAULT_TIDAK_AKTIF) {
			if (key.equalsIgnoreCase(h)) {
				ada = true;
				break;
			}
		}
		return ada;
	}

	public static boolean apakahAdaTidakWajib(String key) {
		boolean ada = false;
		for (String h : CalonSiswaAction.DEFAULT_TIDAK_WAJIB) {
			if (key.equalsIgnoreCase(h)) {
				ada = true;
				break;
			}
		}
		return ada;
	}

	public static boolean wajibIsi(Tbmuser tbmuser, String key) {
		return statusWajibIsi(tbmuser, key).equals(Konfigurasi.AKTIF);
	}

	public static String statusWajibIsi(Tbmuser tbmuser, String key) {
		String defaultValue = apakahAdaTidakAktif(key) ? Konfigurasi.TIDAK_AKTIF
				: apakahAdaTidakWajib(key) ? Konfigurasi.AKTIF_TIDAK_WAJIB : Konfigurasi.AKTIF;
		Konfigurasi konfigurasi = Common.getKonfigurasi(
				(tbmuser == null ? "calon_siswa_tampil_" : "calon_siswa_login_tampil_") + key, defaultValue);
		return konfigurasi.getNilai();
	}

	private static List<String> tidakdicheck = new ArrayList<String>();
	static {

		tidakdicheck.add("paketPsb");
		tidakdicheck.add("merupakanPindahan");
		tidakdicheck.add("tanggalPindah");
		tidakdicheck.add("keteranganPindah");
		tidakdicheck.add("pindahanDariSekolah");
		tidakdicheck.add("alamatSekolahPindahan");
		tidakdicheck.add("kelasSekolahPindahan");
		tidakdicheck.add("orangTuaPegawai");
		tidakdicheck.add("riwayatPembayaranPendaftaran");
		tidakdicheck.add("riwayatPembayaranDaftarUlang");
	}

	public static List<String> dataYangWajibDiisi(Tbmuser tbmuser) {
		List<String> strings = new ArrayList<String>();

		for (String key : CalonSiswaAction.DATA) {
			if (key.equalsIgnoreCase("id") || tidakdicheck.contains(key)) {
				continue;
			}
			if (wajibIsi(tbmuser, key)) {
				strings.add(key);
			}
		}

		return strings;
	}

	public void onTampil() {

		Rows rows = (createSpan("Form Calon Siswa"));

		int index = 0;
		for (String key : CalonSiswaAction.DATA) {
			try {
				if (key.equalsIgnoreCase("id") || tidakdicheck.contains(key)) {
					index++;
					continue;
				}
				if (apakahAdaTidakWajib(key)) {
					rows.appendChild(createRowActiveDefault(
							"Apakah \"" + CalonSiswaAction.DATA_DESC[index] + "\" tampil di calon siswa ?",
							"calon_siswa_tampil_" + key, Konfigurasi.AKTIF_TIDAK_WAJIB, createComboActive(true)));
				} else if (apakahAdaTidakAktif(key)) {
					rows.appendChild(createRowActiveDefault(
							"Apakah \"" + CalonSiswaAction.DATA_DESC[index] + "\" tampil di calon siswa ?",
							"calon_siswa_tampil_" + key, Konfigurasi.TIDAK_AKTIF, createComboActive(true)));
				} else {
					rows.appendChild(createRowActive(
							"Apakah \"" + CalonSiswaAction.DATA_DESC[index] + "\" tampil di calon siswa ?",
							"calon_siswa_tampil_" + key, createComboActive(true)));
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/KonfigurasiTampilanBiodataCalonSiswaAction.java:142");
//				Common.tampilErrorJikaAdmin(e); 
			}
			index++;
		}

		rows = (createSpan("Form Login Calon Siswa"));

		index = 0;
		for (String key : CalonSiswaAction.DATA) {
			try {
				if (key.equalsIgnoreCase("id") || tidakdicheck.contains(key)) {
					index++;
					continue;
				}
				if (apakahAdaTidakWajib(key)) {
					rows.appendChild(createRowActiveDefault(
							"Apakah \"" + CalonSiswaAction.DATA_DESC[index] + "\" tampil di login calon siswa ?",
							"calon_siswa_login_tampil_" + key, Konfigurasi.AKTIF_TIDAK_WAJIB, createComboActive(true)));
				} else if (apakahAdaTidakAktif(key)) {
					rows.appendChild(createRowActiveDefault(
							"Apakah \"" + CalonSiswaAction.DATA_DESC[index] + "\" tampil di login calon siswa ?",
							"calon_siswa_login_tampil_" + key, Konfigurasi.TIDAK_AKTIF, createComboActive(true)));
				} else {
					rows.appendChild(createRowActive(
							"Apakah \"" + CalonSiswaAction.DATA_DESC[index] + "\" tampil di login calon siswa ?",
							"calon_siswa_login_tampil_" + key, createComboActive(true)));
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/KonfigurasiTampilanBiodataCalonSiswaAction.java:170");
//				Common.tampilErrorJikaAdmin(e); 
			}
			index++;
		}

	}
}
