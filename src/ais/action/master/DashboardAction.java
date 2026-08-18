package ais.action.master;

import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Center;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Html;
import ais.ui.util.MyInclude;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.West;

import ais.action.master.sapto.LaporanAksesbilitas_A_6_2_7;
import ais.action.master.sapto.LaporanDanaInstitusi_A_6_1_4;
import ais.action.master.sapto.LaporanDanaInstitusi_A_6_1_5;
import ais.action.master.sapto.LaporanDanaInstitusi_A_6_1_6;
import ais.action.master.sapto.LaporanDanaInstitusi_A_6_1_7;
import ais.action.master.sapto.LaporanDosenInstitusi_A_4_3_1;
import ais.action.master.sapto.LaporanDosenInstitusi_A_4_3_2;
import ais.action.master.sapto.LaporanDosenInstitusi_A_4_5_1;
import ais.action.master.sapto.LaporanKaryaDosen_A_7_1_5;
import ais.action.master.sapto.LaporanKegiatanDosen_A_4_4;
import ais.action.master.sapto.LaporanKerjasamaDalamNegeri_A_7_3_2;
import ais.action.master.sapto.LaporanKerjasamaLuarNegeri_A_7_3_3;
import ais.action.master.sapto.LaporanLahanInstitusi_A_6_2_2;
import ais.action.master.sapto.LaporanLayananKepadaMahasiswa_A_3_1_8;
import ais.action.master.sapto.LaporanPenelitianDosen_A_7_1_2;
import ais.action.master.sapto.LaporanPenelitianDosen_A_7_1_3;
import ais.action.master.sapto.LaporanPenelitianDosen_A_7_1_4;
import ais.action.master.sapto.LaporanPengabdianDosen_A_7_2_2;
import ais.action.master.sapto.LaporanPrasaranaInstitusi_A_6_2_3A;
import ais.action.master.sapto.LaporanPrasaranaInstitusi_A_6_2_3B;
import ais.action.master.sapto.LaporanPrasaranaInstitusi_A_6_2_4;
import ais.action.master.sapto.LaporanPrestasiMahasiswa_A_3_1_11;
import ais.action.master.sapto.LaporanProfileMahasiswaDanLulusan_A_3_2_1;
import ais.action.master.sapto.LaporanProfileMahasiswaDanLulusan_A_3_2_2;
import ais.action.master.sapto.LaporanProfileMahasiswaDanLulusan_A_3_2_4;
import ais.action.master.sapto.LaporanProfileMahasiswa_A_3_1_5;
import ais.action.master.sapto.LaporanPustaka_A_6_2_5;
import ais.action.master.sapto.LaporanStatusAkreditasiProdi_A_2_4_6;
import ais.common.Common;
import ais.ui.util.MyWindow;

public class DashboardAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private Center center;
	private West west;

	private Tabpanel dataKuantitatifSarjana;

	public void onSarjana(Event event) {
		if (dataKuantitatifSarjana.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(dataKuantitatifSarjana);
			MyInclude iframe = new MyInclude("/dashboard_s1.zul");
			iframe.setParent(window);
		}
	}

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
		Common.encripSemua();
		Common.initBahasaParameter(execution.getParameter("lang"));
		menus.put("01. Status Akreditasi", LaporanStatusAkreditasiProdi_A_2_4_6.class);
		menus.put("02. Profil Mahasiswa", LaporanProfileMahasiswa_A_3_1_5.class);
		menus.put("03. Layanan kpd Mahasiswa", LaporanLayananKepadaMahasiswa_A_3_1_8.class);
		menus.put("04. Prestasi Mahasiswa", LaporanPrestasiMahasiswa_A_3_1_11.class);
		menus.put("05. Profil Mahasiswa dan Lulusan", LaporanProfileMahasiswaDanLulusan_A_3_2_1.class);
		menus.put("06. Masa Studi dan IPK Lulusan", LaporanProfileMahasiswaDanLulusan_A_3_2_2.class);
		menus.put("07. Studi Pelacakan Lulusan", LaporanProfileMahasiswaDanLulusan_A_3_2_4.class);

		menus.put("08. Dosen Tetap Institusi", LaporanDosenInstitusi_A_4_3_1.class);
		menus.put("09. Dosen Tidak Tetap Institusi", LaporanDosenInstitusi_A_4_3_2.class);
		menus.put("10. Tugas Belajar Dosen", LaporanKegiatanDosen_A_4_4.class);
		menus.put("11. Tenaga Kependidikan", LaporanDosenInstitusi_A_4_5_1.class);

		menus.put("12. Penerimaan Dana", LaporanDanaInstitusi_A_6_1_4.class);
		menus.put("13. Penggunaan Dana", LaporanDanaInstitusi_A_6_1_5.class);
		menus.put("14. Dana Penelitian", LaporanDanaInstitusi_A_6_1_6.class);
		menus.put("15. Dana Pengabdian", LaporanDanaInstitusi_A_6_1_7.class);
		menus.put("16. Lahan Perguruan Tinggi", LaporanLahanInstitusi_A_6_2_2.class);
		menus.put("17. Prasarana Perguruan Tinggi", LaporanPrasaranaInstitusi_A_6_2_3A.class);
		menus.put("18. Prasarana Pendukung", LaporanPrasaranaInstitusi_A_6_2_3B.class);
		menus.put("19. Prasarana Tambahan dan Investasi", LaporanPrasaranaInstitusi_A_6_2_4.class);
		menus.put("20. Perpustakaan", LaporanPustaka_A_6_2_5.class);
		menus.put("21. Aksesibilitas Data", LaporanAksesbilitas_A_6_2_7.class);

		menus.put("22. Penelitian Dosen Tetap", LaporanPenelitianDosen_A_7_1_2.class);
		menus.put("23. Artikel/Karya Dosen Tetap", LaporanPenelitianDosen_A_7_1_3.class);
		menus.put("24. Artikel Ter-Sitasi Internasional", LaporanPenelitianDosen_A_7_1_4.class);
		menus.put("25. Karya Dosen", LaporanKaryaDosen_A_7_1_5.class);

		menus.put("26. Pengabdian Dosen Tetap", LaporanPengabdianDosen_A_7_2_2.class);

		menus.put("27. Kerjasama Dalam Negeri", LaporanKerjasamaDalamNegeri_A_7_3_2.class);
		menus.put("28. Kerjasama Luar Negeri", LaporanKerjasamaLuarNegeri_A_7_3_3.class);

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

		HttpServletRequest request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();

		String defaultValue = ""
				+ "<div style='padding: 90px;border: 1px solid #4CAF50;'><div style=\"color: rgb(0, 0, 0); font-family: arial; font-weight: 700; text-align: center;\">"
				+ "<img alt=\"\" src=\"" + request.getContextPath()
				+ "/img/logo.png\" style=\"border: 0px; width: 128px; height: 130px;\" /></div>"
				+ "<h2>Selamat Datang di Informasi Akreditasi "
				+ Common.getKonfigurasi("label_universitas", "kampus").getNilai() + "</h2>"
				+ "<p>Sistem Informasi Akreditasi ini mentediakan informasi yang dibutuhkan sesuai dengan format standard SAPTO. Sistem Akreditasi Perguruan Tinggi Online (SAPTO) adalah sistem yang "
				+ "dikembangkan Badan Akreditasi Nasional Perguruan Tinggi (BAN-PT) untuk "
				+ "meningkatkan efisiensi dan kualitas proses akreditasi perguruan tinggi yang "
				+ "diselenggarakan oleh BAN-PT. SAPTO mendukung setiap proses yang dilakukan "
				+ "dalam akreditasi seperti pengajuan usulan akreditasi oleh "
				+ Common.getKonfigurasi("label_universitas", "kampus").getNilai()
				+ ", silahkan plih menu-menu di samping.</p><br><br><p>Pusat Pangkalan Data dan Informasi<br>"
				+ Common.getKonfigurasi("label_universitas", "kampus").getNilai() + "</p><div>";

		String content = Common.getKonfigurasi("akreditasi_content", defaultValue).getNilai();

		Html info = new ais.ui.util.MyHtml();
		if (info != null) { info.setContent(content); }
		if (info != null) { info.setParent(tabpanelUtama); }

		if (west != null) { west.setAutoscroll(true); }
		ais.ui.util.ZkCompat.setFlex(west, true);

		MyGrid grid = new MyGrid();
		if (grid != null) { grid.setParent(west); }
		if (grid != null) { grid.setOddRowSclass("non-odd"); }
		Rows rows = new Rows();
		if (rows != null) { rows.setParent(grid); }
		for (final String menu : menus.keySet()) {

			Row row = new Row();
			row.setValign("top");
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
