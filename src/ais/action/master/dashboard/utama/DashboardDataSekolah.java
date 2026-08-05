package ais.action.master.dashboard.utama;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.action.maintenance.MainAction;
import ais.action.master.dashboard.admin.DasboardGuru;
import ais.action.master.dashboard.admin.DasboardJadwalPelajaran;
import ais.action.master.dashboard.admin.DasboardKelulusanSiswa;
import ais.action.master.dashboard.admin.DasboardSiswa;
import ais.action.master.dashboard.admin.DasborAkademikSekolah;
import ais.action.master.dashboard.sekolah.DashboardCalonSiswa;
import ais.action.master.dashboard.sekolah.DashboardKurikulumSekolah;
import ais.common.Common;
import ais.database.model.Tbmuser;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyWindow;

public class DashboardDataSekolah extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3049796225254178236L;
	private Integer desktopHeight = 11000;

	public DashboardDataSekolah() {
		super();
		try {
			init();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/utama/DashboardDataSekolah.java:37");
		}
	}

	public DashboardDataSekolah(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/utama/DashboardDataSekolah.java:47");
		}
	}

	/**
	 * Menampilkan kotak galat + tombol "Muat Ulang" pada sebuah tabpanel yang gagal dibangun,
	 * menggantikan tab kosong tanpa keterangan.
	 */
	private void tampilkanGagalMuat(final org.zkoss.zul.Tabpanel panel, final EventListener pemuat,
			Exception e) {
		try {
			ais.common.Common.clear(panel);
			String pesan = e == null || e.getMessage() == null ? "(tanpa keterangan)" : e.getMessage();
			org.zkoss.zul.Html html = new org.zkoss.zul.Html(
					"<div style='margin:12px; padding:16px; border-radius:14px; background:#fff1f2; "
							+ "color:#991b1b; border:1px solid #fecdd3;'>"
							+ "<div style='font-weight:800; margin-bottom:6px;'>Dasbor belum dapat dimuat.</div>"
							+ "<div style='font-size:12px;'>"
							+ pesan.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + "</div>"
							+ "</div>");
			html.setParent(panel);

			ais.ui.util.MyToolbarbuttonConfig ulang = new ais.ui.util.MyToolbarbuttonConfig("Muat Ulang",
					"/img/refresh.png");
			ulang.setStyle("margin-left:12px;");
			ulang.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event ev) throws Exception {
					ais.common.Common.clear(panel);
					pemuat.onEvent(ev);
				}
			});
			ulang.setParent(panel);
		} catch (Exception ignore) {
			ais.common.ErrorAuditUtil.record(ignore, "DashboardDataSekolah.tampilkanGagalMuat");
		}
	}

	private void init() throws Exception {

		Tabbox tabbox = new Tabbox();
		tabbox.setStyle("min-height:" + desktopHeight + "px");
		tabbox.setParent(Common.tampilanScrollTabbox(this));
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");
		Tbmuser tbmuser = Common.getCurrentUser();

		if (tbmuser != null && tbmuser.getUserId() != null) {
			Integer h = MainAction.desktopHeights.get(tbmuser.getUserId());
			if (h != null) {
				desktopHeight = h;
				tabbox.setStyle("min-height:" + (desktopHeight * 0.9) + "px");
			}
		}

		desktopHeight = desktopHeight * 15;

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab1 = new MyTabConfig("Dashbord Utama", "/img/home-icon.png");
		tab1.setParent(tabs);

		MyTabConfig tab2 = new MyTabConfig("Kurikulum", "/img/Document-Text-icon.png");
		tab2.setParent(tabs);

		MyTabConfig tab3 = new MyTabConfig("Calon Siswa", "/img/offline-icon.png");
		tab3.setParent(tabs);

		MyTabConfig tab4 = new MyTabConfig("Siswa", "/img/online-red-icon_not_yet_access.png");
		tab4.setParent(tabs);

		MyTabConfig tab5 = new MyTabConfig("Guru", "/img/group.gif");
		tab5.setParent(tabs);

		MyTabConfig tab6 = new MyTabConfig("Jadwal Pelajaran", "/img/calendar-view-week-icon.png");
		tab6.setParent(tabs);

		MyTabConfig tab7 = new MyTabConfig("Alumni", "/img/education-university-icon.png");
		tab7.setParent(tabs);

		

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		final Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);
		tabpanel1.setHeight("11000px");

		/*
		 * Tab pertama dibangun EAGER (bukan lazy seperti tab lain) karena tab inilah yang
		 * aktif saat layar dibuka. Bila pembangunannya gagal, dulu galatnya hanya tertelan
		 * catch di constructor sehingga tab tampak KOSONG tanpa penjelasan. Sekarang galat
		 * ditampilkan sebagai kotak merah + tombol "Muat Ulang", dan klik pada tab juga
		 * mencoba membangun ulang bila isinya masih kosong.
		 */
		final Integer tinggiDasbor = desktopHeight;
		final EventListener muatDasborUtama = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (!tabpanel1.getChildren().isEmpty()) {
					return;
				}
				try {
					DasborAkademikSekolah dasborAkademikSekolah = new DasborAkademikSekolah();
					dasborAkademikSekolah.setHeight(tinggiDasbor + "px");
					dasborAkademikSekolah.setWidth("100%");
					tabpanel1.appendChild(dasborAkademikSekolah);
				} catch (Exception e) {
					e.printStackTrace();
					ais.common.ErrorAuditUtil.record(e,
							"DashboardDataSekolah.muatDasborUtama");
					tampilkanGagalMuat(tabpanel1, this, e);
				}
			}
		};
		muatDasborUtama.onEvent(null);
		tab1.addEventListener("onClick", muatDasborUtama);

		final Tabpanel panelKurikulum = new ais.ui.util.MyTabpanel();
		panelKurikulum.setParent(tabpanels);
		panelKurikulum.setHeight("11000px");

		tab2.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (panelKurikulum.getChildren().isEmpty()) {
					DashboardKurikulumSekolah dashboardKurikulum = new DashboardKurikulumSekolah();
					dashboardKurikulum.setHeight("11000px");
					dashboardKurikulum.setWidth("100%");

					panelKurikulum.appendChild(dashboardKurikulum);
				}
			}
		});

		final Tabpanel panelCalonMahasiswa = new ais.ui.util.MyTabpanel();
		panelCalonMahasiswa.setParent(tabpanels);
		panelCalonMahasiswa.setHeight("11000px");

		tab3.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (panelCalonMahasiswa.getChildren().isEmpty()) {
					DashboardCalonSiswa dashboardCalonMahasiswa = new DashboardCalonSiswa();
					dashboardCalonMahasiswa.setHeight("11000px");
					dashboardCalonMahasiswa.setWidth("100%");

					panelCalonMahasiswa.appendChild(dashboardCalonMahasiswa);
				}
			}
		});

		final Tabpanel panelMahasiswa = new ais.ui.util.MyTabpanel();
		panelMahasiswa.setParent(tabpanels);
		panelMahasiswa.setHeight("11000px");

		tab4.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (panelMahasiswa.getChildren().isEmpty()) {
					DasboardSiswa dashboardSiswa = new DasboardSiswa();
					dashboardSiswa.setHeight("11000px");
					dashboardSiswa.setWidth("100%");

					panelMahasiswa.appendChild(dashboardSiswa);
				}
			}
		});

		final Tabpanel panelDosen = new ais.ui.util.MyTabpanel();
		panelDosen.setParent(tabpanels);
		panelDosen.setHeight("11000px");

		tab5.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (panelDosen.getChildren().isEmpty()) {
					DasboardGuru dashboard = new DasboardGuru();
					dashboard.setHeight("11000px");
					dashboard.setWidth("100%");

					panelDosen.appendChild(dashboard);
				}
			}
		});

		final Tabpanel panelPerkuliahan = new ais.ui.util.MyTabpanel();
		panelPerkuliahan.setParent(tabpanels);
		panelPerkuliahan.setHeight("11000px");

		tab6.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (panelPerkuliahan.getChildren().isEmpty()) {
					DasboardJadwalPelajaran dashboardPerkuliahan = new DasboardJadwalPelajaran();
					dashboardPerkuliahan.setHeight("11000px");
					dashboardPerkuliahan.setWidth("100%");

					panelPerkuliahan.appendChild(dashboardPerkuliahan);
				}
			}
		});

		final Tabpanel panelkeLulusan = new ais.ui.util.MyTabpanel();
		panelkeLulusan.setParent(tabpanels);
		panelkeLulusan.setHeight("11000px");

		tab7.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (panelkeLulusan.getChildren().isEmpty()) {
					DasboardKelulusanSiswa dashboardBimbinganMahasiswa = new DasboardKelulusanSiswa();
					dashboardBimbinganMahasiswa.setHeight("11000px");
					dashboardBimbinganMahasiswa.setWidth("100%");

					panelkeLulusan.appendChild(dashboardBimbinganMahasiswa);
				}
			}
		});

		
	}

}
