package ais.action.master;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Rows;

import ais.action.master.pmb.BiodataCalonMahasiswaAction;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Konfigurasi;
import ais.ui.util.MyMessageboxConfig;

public class KonfigurasiTampilanLoginCalonMahasiswaAction extends KonfigurasiNewAction {

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
		for (String h : BiodataCalonMahasiswaAction.DATA) {
			if (key.equalsIgnoreCase(h)) {
				return BiodataCalonMahasiswaAction.DATA_DESC[index];
			}
			index++;
		}
		return "";
	}

	public static boolean apakahAdaTidakAktif(String key) {
		boolean ada = false;
		for (String h : BiodataCalonMahasiswaAction.DEFAULT_TIDAK_AKTIF) {
			if (key.equalsIgnoreCase(h)) {
				ada = true;
				break;
			}
		}
		return ada;
	}

	public static boolean apakahAdaTidakWajib(String key) {
		boolean ada = false;
		for (String h : BiodataCalonMahasiswaAction.DEFAULT_TIDAK_WAJIB) {
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
		String defaultValue = apakahAdaTidakAktif(key) ? Konfigurasi.TIDAK_AKTIF
				: apakahAdaTidakWajib(key) ? Konfigurasi.AKTIF_TIDAK_WAJIB : Konfigurasi.AKTIF;
		Konfigurasi konfigurasi = Common.getKonfigurasi("login_calon_mahasiswa_tampil_" + key, defaultValue);
		return konfigurasi.getNilai();
	}

	public static List<String> dataYangWajibDiisi() {
		List<String> strings = new ArrayList<String>();

		for (String key : BiodataCalonMahasiswaAction.DATA) {
			if (wajibIsi(key)) {
				strings.add(key);
			}
		}

		return strings;
	}

	public void onTampil() {

		Rows rows = (createSpan("Form Login Calon Mahasiswa"));

		int index = 0;
		for (String key : BiodataCalonMahasiswaAction.DATA) {
			if (apakahAdaTidakWajib(key)) {
				rows.appendChild(createRowActiveDefault(
						"Apakah \"" + BiodataCalonMahasiswaAction.DATA_DESC[index]
								+ "\" tampil di login calon mahasiswa ?",
						"login_calon_mahasiswa_tampil_" + key, Konfigurasi.AKTIF_TIDAK_WAJIB, createComboActive(true)));
			} else if (apakahAdaTidakAktif(key)) {
				rows.appendChild(createRowActiveDefault(
						"Apakah \"" + BiodataCalonMahasiswaAction.DATA_DESC[index]
								+ "\" tampil di biodata calon mahasiswa ?",
						"login_calon_mahasiswa_tampil_" + key, Konfigurasi.TIDAK_AKTIF, createComboActive(true)));
			} else {
				rows.appendChild(createRowActive(
						"Apakah \"" + BiodataCalonMahasiswaAction.DATA_DESC[index]
								+ "\" tampil di login calon mahasiswa ?",
						"login_calon_mahasiswa_tampil_" + key, createComboActive(true)));
			}
			index++;
		}

	}

	public static boolean check(BiodataCalonMahasiswa biodataCalonMahasiswa) throws Exception {
		List<String> daftarWajibDiisi = dataYangWajibDiisi();
		for (String key : daftarWajibDiisi) {
			if (Common.checkIsNull(BiodataCalonMahasiswa.class, biodataCalonMahasiswa, key)) {
				// Hanya tampilkan messagebox di lingkungan ZK; di JSP/servlet hanya return false
				// agar tidak menyuntikkan <script> ke tengah-tengah HTML yang sedang di-render.
				if (org.zkoss.zk.ui.Executions.getCurrent() != null) {
					MyMessageboxConfig.show(
							"Biodata Anda harus dilengkapi. Data \""
									+ KonfigurasiTampilanBiodataCalonMahasiswaAction.keyDesc(key)
									+ "\" masih belum terisi dengan benar",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				}
				return false;
			}
		}
		return true;
	}
}
