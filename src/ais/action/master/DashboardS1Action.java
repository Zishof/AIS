package ais.action.master;

import java.util.TreeMap;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Center;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.West;

import ais.action.master.sapto.LaporanDana_A_6_2_1_1;
import ais.action.master.sapto.LaporanDana_A_6_2_1_2;
import ais.action.master.sapto.LaporanDana_A_6_2_2;
import ais.action.master.sapto.LaporanDana_A_6_2_3;
import ais.action.master.sapto.LaporanDosenInstitusi_A_4_5_1;
import ais.action.master.sapto.LaporanDosenPembimbingTugasAkhir_A_5_5_1;
import ais.action.master.sapto.LaporanDosenPembimbingTugasAkhir_A_5_5_2;
import ais.action.master.sapto.LaporanDosenPembimbing_A_5_4_1;
import ais.action.master.sapto.LaporanKurikulumDanMatakuliah_A_5_1_2_1;
import ais.action.master.sapto.LaporanKurikulumDanMatakuliah_A_5_1_2_2;
import ais.action.master.sapto.LaporanKurikulumDanMatakuliah_A_5_1_3;
import ais.action.master.sapto.LaporanProfileDosen;
import ais.action.master.sapto.LaporanProfileDosen_A_4_3_1;
import ais.action.master.sapto.LaporanProfileDosen_A_4_3_2;
import ais.action.master.sapto.LaporanProfileDosen_A_4_3_3;
import ais.action.master.sapto.LaporanProfileDosen_A_4_3_4;
import ais.action.master.sapto.LaporanProfileDosen_A_4_3_5;
import ais.action.master.sapto.LaporanProfileDosen_A_4_4_1;
import ais.action.master.sapto.LaporanProfileDosen_A_4_4_2;
import ais.action.master.sapto.LaporanProfileDosen_A_4_5_1;
import ais.action.master.sapto.LaporanProfileDosen_A_4_5_2;
import ais.action.master.sapto.LaporanProfileDosen_A_4_5_3;
import ais.action.master.sapto.LaporanProfileDosen_A_4_5_4;
import ais.action.master.sapto.LaporanProfileDosen_A_4_5_5;
import ais.action.master.sapto.LaporanProfileMahasiswaDanLulusan_A_3_1_1;
import ais.action.master.sapto.LaporanProfileMahasiswaDanLulusan_A_3_1_2;
import ais.action.master.sapto.LaporanProfileMahasiswaDanLulusan_A_3_1_4;
import ais.common.Common;
import ais.ui.util.CheckForParentScript;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

public class DashboardS1Action extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private Center center;
	private West west;

	@SuppressWarnings("rawtypes")
	private TreeMap<String, Class> menus = new TreeMap<String, Class>();

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.ROOT = execution.getContextPath();
		page.getFirstRoot().appendChild(new CheckForParentScript());
		// Clients.confirmClose(Common.getBahasaConfig("Apakah Anda yakin ingin keluar dari aplikasi ini ?"));
		Common.encripSemua();

		menus.put("01. Data Dosen", LaporanProfileDosen.class);
		menus.put("02. Profil Mahasiswa Reguler", LaporanProfileMahasiswaDanLulusan_A_3_1_1.class);
		menus.put("03. Profil Mahasiswa Non Reguler", LaporanProfileMahasiswaDanLulusan_A_3_1_2.class);
		menus.put("04. Mahasiswa Reguler 7 thn", LaporanProfileMahasiswaDanLulusan_A_3_1_4.class);
		// menus.put("05. Evaluasi Lulusan",
		// LaporanProfileMahasiswaDanLulusan_A_3_2_1.class);
		menus.put("06. Dosen Sesuai Bidang", LaporanProfileDosen_A_4_3_1.class);
		menus.put("07. Dosen Tidak Sesuai Bidang", LaporanProfileDosen_A_4_3_2.class);
		menus.put("08. Aktifitas Dosen", LaporanProfileDosen_A_4_3_3.class);
		menus.put("09. Aktifitas Dosen Sesuai Bidang", LaporanProfileDosen_A_4_3_4.class);
		menus.put("10. Aktifitas Dosen Tidak Sesuai Bidang", LaporanProfileDosen_A_4_3_5.class);
		menus.put("10. Data Dosen Tidak Tetap", LaporanProfileDosen_A_4_4_1.class);
		menus.put("11. Aktifitas Dosen Tidak Tetap", LaporanProfileDosen_A_4_4_2.class);
		menus.put("12. Kegiatan Tenaga Ahli", LaporanProfileDosen_A_4_5_1.class);
		menus.put("13. Tugas Belajar Dosen", LaporanProfileDosen_A_4_5_2.class);
		menus.put("14. Kegiatan Dosen Tetap", LaporanProfileDosen_A_4_5_3.class);
		menus.put("15. Prestasi Dosen", LaporanProfileDosen_A_4_5_4.class);
		menus.put("16. Organisasi Dosen", LaporanProfileDosen_A_4_5_5.class);
		// menus.put("17. Statistis Tenaga Pendidik",
		// LaporanProfileDosen_A_4_6_1.class);
		menus.put("17. Statistis Tenaga Pendidik", LaporanDosenInstitusi_A_4_5_1.class);
		menus.put("18. Jumlah SKS", LaporanKurikulumDanMatakuliah_A_5_1_2_1.class);
		menus.put("19. Struktur Kurikulum", LaporanKurikulumDanMatakuliah_A_5_1_2_2.class);
		menus.put("20. Matakuliah Pilihan", LaporanKurikulumDanMatakuliah_A_5_1_3.class);

		menus.put("21. Dosen Pembimbing Akademik", LaporanDosenPembimbing_A_5_4_1.class);
		menus.put("22. Dosen Pembimbing Tugas Akhir", LaporanDosenPembimbingTugasAkhir_A_5_5_1.class);

		menus.put("23. Penyelesaian Tugas Akhir", LaporanDosenPembimbingTugasAkhir_A_5_5_2.class);

		menus.put("24. Perolehan Dana", LaporanDana_A_6_2_1_1.class);
		menus.put("25. Penggunaan Dana", LaporanDana_A_6_2_1_2.class);
		
		menus.put("26. Dana Penelitian", LaporanDana_A_6_2_2.class);
		menus.put("27. Dana Pengabdian", LaporanDana_A_6_2_3.class);

		Tabbox tabbox = new Tabbox();
		if (tabbox != null) { tabbox.setParent(center); }
		if (tabbox != null) { tabbox.setHeight("100%"); }
		if (tabbox != null) { tabbox.setWidth("100%"); }

		final Tabs tabs = new Tabs();
		if (tabs != null) { tabs.setParent(tabbox); }

		final Tabpanels tabpanels = new Tabpanels();
		if (tabpanels != null) { tabpanels.setParent(tabbox); }

		Tab tab = new Tab("Home");
		if (tab != null) { tab.setParent(tabs); }
		Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
		if (tabpanelUtama != null) { tabpanelUtama.setParent(tabpanels); }

		Iframe iframe = new Iframe();
		if (iframe != null) { iframe.setHeight("100%"); }
		if (iframe != null) { iframe.setWidth("100%"); }
		if (iframe != null) { iframe.setParent(tabpanelUtama); }
		if (iframe != null) { iframe.setSrc("/common/tampilan_pengumuman_akademis.zul"); }

		ais.ui.util.ZkCompat.setFlex(west, true);

		MyGrid grid = new MyGrid();
		if (grid != null) { grid.setParent(west); }
		if (grid != null) { grid.setOddRowSclass("non-odd"); }
		Rows rows = new Rows();
		if (rows != null) { rows.setParent(grid); }
		for (final String menu : menus.keySet()) {

			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			A a = new A(menu);
			a.setStyle("font-size:10px");
			a.setParent(row);
			a.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("rawtypes")
				@Override
				public void onEvent(Event arg0) throws Exception {

					Tab tab = (Tab) tabs.getAttribute(menu);
					if (tab == null) {
						tab = new Tab(menu);
						tab.setParent(tabs);
						Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
						tabpanelUtama.setParent(tabpanels);
						Class clazz = menus.get(menu);
						MyWindow saptoModel = (MyWindow) clazz.newInstance();
						tabpanelUtama.appendChild(saptoModel);
						tab.setSelected(true);
						tabs.setAttribute(menu, tab);
					} else {
						tab.setSelected(true);
					}
				}
			});
		}
	}

}
