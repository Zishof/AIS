package ais.action.master;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.model.Konfigurasi;

public class KonfigurasiTampilanBiodataDosenAction extends KonfigurasiNewAction {

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

		for (String h : BiodataDosenAction.DATA) {
			if (key.equalsIgnoreCase(h)) {
				return BiodataDosenAction.DATA_DESC[index];
			}
			index++;
		}
		return "";
	}

	public static boolean apakahAdaTidakWajib(String key) {
		boolean ada = false;
		for (String h : BiodataDosenAction.DEFAULT_TIDAK_WAJIB) {
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
		Konfigurasi konfigurasi = Common.getKonfigurasi("biodata_dosen_tampil_" + key, defaultValue);
		return konfigurasi.getNilai();
	}

	public static List<String> dataYangWajibDiisi() {
		List<String> strings = new ArrayList<String>();

		for (String key : BiodataDosenAction.DATA) {
			if (wajibIsi(key)) {
				strings.add(key);
			}
		}

		return strings;
	}

	public void onTampil() {

		Rows rows = (createSpan("Form Biodata Dosen"));

		int index = 0;
		for (String key : BiodataDosenAction.DATA) {
			try {
				if (apakahAdaTidakWajib(key)) {
					rows.appendChild(createRowActiveDefault(
							"Apakah \"" + BiodataDosenAction.DATA_DESC[index] + "\" tampil di biodata dosen ?",
							"biodata_dosen_tampil_" + key, Konfigurasi.AKTIF_TIDAK_WAJIB,
							createComboActiveAndReadOnly(true)));
				} else {
					rows.appendChild(createRowActive(
							"Apakah \"" + BiodataDosenAction.DATA_DESC[index] + "\" tampil di biodata dosen ?",
							"biodata_dosen_tampil_" + key, createComboActiveAndReadOnly(true)));
				}

				index++;
			} catch (Exception e) {
				System.out.println("error key " + key);
			}
		}

	}
}
