package ais.action.master;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Div;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.action.master.sekolah.SiswaAction;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.model.Konfigurasi;
import ais.ui.util.MyButtonTabbox;
import ais.ui.util.MyInclude;

/**
 * Layar konfigurasi tampilan field-field pada modul Siswa: mengizinkan admin menentukan per-field
 * (lewat {@link SiswaAction#DATA}) apakah field tersebut tampil/wajib diisi pada form siswa,
 * disimpan sebagai baris {@link Konfigurasi} berkunci {@code siswa_tampil_<key>}. Tata letak berupa
 * tab utama "Form Siswa" (dibangun langsung, memakai inner {@link Tabbox} agar mekanisme
 * {@code createSpan()} dari kelas induk berfungsi) plus 7 tab konfigurasi sub-form terkait
 * (jenis tinggal, transportasi, penghasilan, dsb) yang masing-masing dimuat malas dari berkas
 * ZUL terpisah lewat {@link MyInclude}. Mewarisi mekanisme render baris konfigurasi generik dari
 * {@link KonfigurasiNewAction}.
 */
public class KonfigurasiTampilanSiswaAction extends KonfigurasiNewAction {

	private static final long serialVersionUID = -5779730267402400328L;

	private Div outerTabsDiv;

	/** Menjalankan pemeriksaan keamanan sebelum komponen ZK dibangun (hook siklus hidup ZK). */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/** Memverifikasi sesi login &amp; hak akses baca, lalu membangun tab utama "Form Siswa" dan 7 tab sub-konfigurasi lazy-load; mengarahkan ke logoff bila sesi/izin tidak valid. */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterComposeOri(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		int[] tabAktif = { 1 };
		MyButtonTabbox btabs = MyButtonTabbox.buat(outerTabsDiv, "100%", tabAktif);

		// Tab 1: Form Siswa — bangun inner tabbox agar createSpan() bisa dipakai
		Div panel1 = btabs.tambahTab(1, "Form Siswa");
		Tabbox innerTabbox = new Tabbox();
		innerTabbox.setWidth("100%");
		tabsKonfigurasi = new Tabs();
		tabsKonfigurasi.setParent(innerTabbox);
		tabpanelsKonfigurasi = new Tabpanels();
		tabpanelsKonfigurasi.setParent(innerTabbox);
		innerTabbox.setParent(panel1);
		onTampil();

		// Tab 2–8: lazy load saat pertama kali diklik; panel sudah visible saat
		// muatJikaBelum dipanggil sehingga createComponents mendapat tinggi yang benar.
		final String[][] subZuls = {
			{ "Jenis Tinggal",      "/pages/master/sekolah/jenis_tinggal_siswa.zul" },
			{ "Alat Transportasi",  "/pages/master/sekolah/alat_transportasi_siswa.zul" },
			{ "Penghasilan",        "/pages/master/sekolah/penghasilan_orang_tua_siswa.zul" },
			{ "Pekerjaan",          "/pages/master/sekolah/pekerjaan_ortu_siswa.zul" },
			{ "Pendidikan",         "/pages/master/sekolah/pendidikan_orang_tua_siswa.zul" },
			{ "Status Awal",        "/pages/master/sekolah/status_awal_siswa.zul" },
			{ "Status Keluar",      "/pages/master/sekolah/status_keluar_siswa.zul" },
		};
		for (int i = 0; i < subZuls.length; i++) {
			final String src = subZuls[i][1];
			btabs.tambahTabLazy(i + 2, subZuls[i][0], new MyButtonTabbox.PemuatTab() {
				@Override
				public void muat(Div panel) throws Exception {
					panel.getChildren().clear();
					MyInclude include = new MyInclude(src);
					include.setParent(panel);
				}
			});
		}
		btabs.pulihkanSeleksi(subZuls.length + 1);
	}

	/** Mengembalikan deskripsi label ({@link SiswaAction#DATA_DESC}) untuk nama field {@code key} pada {@link SiswaAction#DATA}, atau string kosong bila tidak ditemukan. */
	public static String keyDesc(String key) {
		int index = 0;
		for (String h : SiswaAction.DATA) {
			if (key.equalsIgnoreCase(h)) {
				return SiswaAction.DATA_DESC[index];
			}
			index++;
		}
		return "";
	}

	/** Memeriksa apakah {@code key} termasuk dalam {@link SiswaAction#DEFAULT_TIDAK_AKTIF} (field yang defaultnya tidak aktif/tidak tampil). */
	public static boolean apakahAdaTidakAktif(String key) {
		boolean ada = false;
		for (String h : SiswaAction.DEFAULT_TIDAK_AKTIF) {
			if (key.equalsIgnoreCase(h)) {
				ada = true;
				break;
			}
		}
		return ada;
	}

	/** Memeriksa apakah {@code key} termasuk dalam {@link SiswaAction#DEFAULT_TIDAK_WAJIB} (field yang defaultnya aktif tapi tidak wajib diisi). */
	public static boolean apakahAdaTidakWajib(String key) {
		boolean ada = false;
		for (String h : SiswaAction.DEFAULT_TIDAK_WAJIB) {
			if (key.equalsIgnoreCase(h)) {
				ada = true;
				break;
			}
		}
		return ada;
	}

	/** Memeriksa apakah field {@code key} wajib diisi, berdasarkan status konfigurasi tersimpan ({@link #statusWajibIsi}) bernilai {@link Konfigurasi#AKTIF}. */
	public static boolean wajibIsi(String key) {
		return statusWajibIsi(key).equals(Konfigurasi.AKTIF);
	}

	/** Membaca status konfigurasi tampilan field {@code key} ({@code siswa_tampil_<key>}) dari database, dengan default ditentukan dari {@link #apakahAdaTidakAktif}/{@link #apakahAdaTidakWajib}. */
	public static String statusWajibIsi(String key) {
		String defaultValue = apakahAdaTidakAktif(key) ? Konfigurasi.TIDAK_AKTIF
				: apakahAdaTidakWajib(key) ? Konfigurasi.AKTIF_TIDAK_WAJIB : Konfigurasi.AKTIF;
		Konfigurasi konfigurasi = Common.getKonfigurasi("siswa_tampil_" + key, defaultValue);
		return konfigurasi.getNilai();
	}

	/** Mengembalikan daftar nama field (di luar {@code id}) dari {@link SiswaAction#DATA} yang saat ini terkonfigurasi wajib diisi. */
	public static List<String> dataYangWajibDiisi() {
		List<String> strings = new ArrayList<String>();
		for (String key : SiswaAction.DATA) {
			if (key.equalsIgnoreCase("id")) {
				continue;
			}
			if (wajibIsi(key)) {
				strings.add(key);
			}
		}
		return strings;
	}

	public void onTampil() {
		Rows rows = (createSpan("Form Calon Siswa"));
		int index = 0;
		for (String key : SiswaAction.DATA) {
			try {
				if (key.equalsIgnoreCase("id")) {
					index++;
					continue;
				}
				if (apakahAdaTidakWajib(key)) {
					rows.appendChild(
							createRowActiveDefault("Apakah \"" + SiswaAction.DATA_DESC[index] + "\" tampil di siswa ?",
									"siswa_tampil_" + key, Konfigurasi.AKTIF_TIDAK_WAJIB, createComboActive(true)));
				} else if (apakahAdaTidakAktif(key)) {
					rows.appendChild(
							createRowActiveDefault("Apakah \"" + SiswaAction.DATA_DESC[index] + "\" tampil di siswa ?",
									"siswa_tampil_" + key, Konfigurasi.TIDAK_AKTIF, createComboActive(true)));
				} else {
					rows.appendChild(
							createRowActive("Apakah \"" + SiswaAction.DATA_DESC[index] + "\" tampil di siswa ?",
									"siswa_tampil_" + key, createComboActive(true)));
				}
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/KonfigurasiTampilanSiswaAction.java");
			}
			index++;
		}
	}
}
