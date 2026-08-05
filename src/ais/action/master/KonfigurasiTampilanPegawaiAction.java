package ais.action.master;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Rows;

import ais.action.master.employ.util.FormBiodataPegawaiUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.model.Konfigurasi;
import ais.ui.util.MyInclude;
import ais.ui.util.MyWindow;

public class KonfigurasiTampilanPegawaiAction extends KonfigurasiNewAction {

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

		for (String h : FormBiodataPegawaiUtil.DATA) {
			if (key.equalsIgnoreCase(h)) {
				return FormBiodataPegawaiUtil.DATA_DESC[index];
			}
			index++;
		}
		return "";
	}

	public static boolean apakahAdaTidakAktif(String key) {
		boolean ada = false;
		for (String h : FormBiodataPegawaiUtil.DEFAULT_TIDAK_AKTIF) {
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
		String defaultValue = apakahAdaTidakAktif(key) ? Konfigurasi.TIDAK_AKTIF : Konfigurasi.AKTIF_TIDAK_WAJIB;
		Konfigurasi konfigurasi = Common.getKonfigurasi("biodata_pegawai_tampil__" + key, defaultValue);
		return konfigurasi.getNilai();
	}

	public static List<String> dataYangWajibDiisi() {
		List<String> strings = new ArrayList<String>();

		for (String key : FormBiodataPegawaiUtil.DATA) {
			if (wajibIsi(key)) {
				strings.add(key);
			}
		}

		return strings;
	}

	public void onTampil() {

		Rows rows = (createSpan("Form Biodata Pegawai"));

		int index = 0;
		for (String key : FormBiodataPegawaiUtil.DATA) {
			try {
				if (apakahAdaTidakAktif(key)) {
					rows.appendChild(
							createRowActiveDefault(
									"Apakah \"" + FormBiodataPegawaiUtil.DATA_DESC[index]
											+ "\" tampil di biodata pegawai ?",
									"biodata_pegawai_tampil__" + key, Konfigurasi.TIDAK_AKTIF,
									createComboActiveAndReadOnly(true)));
				} else {
					rows.appendChild(
							createRowActiveDefault(
									"Apakah \"" + FormBiodataPegawaiUtil.DATA_DESC[index]
											+ "\" tampil di biodata pegawai ?",
									"biodata_pegawai_tampil__" + key, Konfigurasi.AKTIF_TIDAK_WAJIB,
									createComboActiveAndReadOnly(true)));
				}

				index++;
			} catch (Exception e) {
				System.out.println("error key " + key);
			}
		}

		mbt.tambahTabLazy(mbtNextIdx++, "Jatah Cuti Pegawai", "/img/svg/calendar-check.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(org.zkoss.zul.Div panel) throws Exception {
						MyWindow window = new MyWindow("", "none", false);
						window.setHeight("100%");
						window.setWidth("100%");
						window.setParent(panel);
						new MyInclude("/pages/master/payroll/jatah_cuti.zul").setParent(window);
					}
				});
	}
}
