package ais.action.master.dashboard.utama;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.action.maintenance.MainAction;
import ais.action.master.dashboard.admin.DasborPerguruanTinggiTerpadu;
import ais.action.master.dashboard.admin.DashboardBimbinganMahasiswa;
import ais.action.master.dashboard.admin.DashboardCalonMahasiswa;
import ais.action.master.dashboard.admin.DashboardDosen;
import ais.action.master.dashboard.admin.DashboardKurikulum;
import ais.action.master.dashboard.admin.DashboardLulusan;
import ais.action.master.dashboard.admin.DashboardMahasiswa;
import ais.action.master.dashboard.admin.DashboardPerkuliahan;
import ais.common.Common;
import ais.database.model.Tbmuser;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyWindow;

public class DashboardData extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3049796225254178236L;
	private Integer desktopHeight = 11000;

	public DashboardData() {
		super();
		try {
			init();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/utama/DashboardData.java:38");
		}
	}

	public DashboardData(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/utama/DashboardData.java:48");
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

		MyTabConfig tab3 = new MyTabConfig("Calon Mhs", "/img/offline-icon.png");
		tab3.setParent(tabs);

		MyTabConfig tab4 = new MyTabConfig("Mhs", "/img/online-red-icon_not_yet_access.png");
		tab4.setParent(tabs);

		MyTabConfig tab5 = new MyTabConfig("Dosen", "/img/group.gif");
		tab5.setParent(tabs);

		MyTabConfig tab6 = new MyTabConfig("Perkuliahan", "/img/calendar-view-week-icon.png");
		tab6.setParent(tabs);

		MyTabConfig tab7 = new MyTabConfig("Kelulusan", "/img/education-university-icon.png");
		tab7.setParent(tabs);

		MyTabConfig tab8 = new MyTabConfig("Alumni", "/img/online-icon_access.png");
		tab8.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);
		tabpanel1.setHeight(desktopHeight + "px");

		DasborPerguruanTinggiTerpadu dasborPerguruanTinggi = new DasborPerguruanTinggiTerpadu();
		dasborPerguruanTinggi.setHeight(desktopHeight + "px");
		dasborPerguruanTinggi.setWidth("100%");
		tabpanel1.appendChild(dasborPerguruanTinggi);

		final Tabpanel panelKurikulum = new ais.ui.util.MyTabpanel();
		panelKurikulum.setParent(tabpanels);
		panelKurikulum.setHeight(desktopHeight + "px");

		tab2.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (panelKurikulum.getChildren().isEmpty()) {
					DashboardKurikulum dashboardKurikulum = new DashboardKurikulum();
					dashboardKurikulum.setHeight(desktopHeight + "px");
					dashboardKurikulum.setWidth("100%");

					panelKurikulum.appendChild(dashboardKurikulum);
				}
			}
		});

		final Tabpanel panelCalonMahasiswa = new ais.ui.util.MyTabpanel();
		panelCalonMahasiswa.setParent(tabpanels);
		panelCalonMahasiswa.setHeight(desktopHeight + "px");

		tab3.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (panelCalonMahasiswa.getChildren().isEmpty()) {
					DashboardCalonMahasiswa dashboardCalonMahasiswa = new DashboardCalonMahasiswa();
					dashboardCalonMahasiswa.setHeight(desktopHeight + "px");
					dashboardCalonMahasiswa.setWidth("100%");

					panelCalonMahasiswa.appendChild(dashboardCalonMahasiswa);
				}
			}
		});

		final Tabpanel panelMahasiswa = new ais.ui.util.MyTabpanel();
		panelMahasiswa.setParent(tabpanels);
		panelMahasiswa.setHeight(desktopHeight + "px");

		tab4.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (panelMahasiswa.getChildren().isEmpty()) {
					DashboardMahasiswa dashboardMahasiswa = new DashboardMahasiswa();
					dashboardMahasiswa.setHeight(desktopHeight + "px");
					dashboardMahasiswa.setWidth("100%");

					panelMahasiswa.appendChild(dashboardMahasiswa);
				}
			}
		});

		final Tabpanel panelDosen = new ais.ui.util.MyTabpanel();
		panelDosen.setParent(tabpanels);
		panelDosen.setHeight(desktopHeight + "px");

		tab5.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (panelDosen.getChildren().isEmpty()) {
					DashboardDosen dashboardDosen = new DashboardDosen();
					dashboardDosen.setHeight(desktopHeight + "px");
					dashboardDosen.setWidth("100%");

					panelDosen.appendChild(dashboardDosen);
				}
			}
		});

		final Tabpanel panelPerkuliahan = new ais.ui.util.MyTabpanel();
		panelPerkuliahan.setParent(tabpanels);
		panelPerkuliahan.setHeight(desktopHeight + "px");

		tab6.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (panelPerkuliahan.getChildren().isEmpty()) {
					DashboardPerkuliahan dashboardPerkuliahan = new DashboardPerkuliahan();
					dashboardPerkuliahan.setHeight(desktopHeight + "px");
					dashboardPerkuliahan.setWidth("100%");

					panelPerkuliahan.appendChild(dashboardPerkuliahan);
				}
			}
		});

		final Tabpanel panelkeLulusan = new ais.ui.util.MyTabpanel();
		panelkeLulusan.setParent(tabpanels);
		panelkeLulusan.setHeight(desktopHeight + "px");

		tab7.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (panelkeLulusan.getChildren().isEmpty()) {
					DashboardBimbinganMahasiswa dashboardBimbinganMahasiswa = new DashboardBimbinganMahasiswa();
					dashboardBimbinganMahasiswa.setHeight(desktopHeight + "px");
					dashboardBimbinganMahasiswa.setWidth("100%");

					panelkeLulusan.appendChild(dashboardBimbinganMahasiswa);
				}
			}
		});

		final Tabpanel panelLulusan = new ais.ui.util.MyTabpanel();
		panelLulusan.setParent(tabpanels);
		panelLulusan.setHeight(desktopHeight + "px");

		tab8.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (panelLulusan.getChildren().isEmpty()) {
					DashboardLulusan dashboardLulusan = new DashboardLulusan();
					dashboardLulusan.setHeight(desktopHeight + "px");
					dashboardLulusan.setWidth("100%");

					panelLulusan.appendChild(dashboardLulusan);
				}
			}
		});
	}

}
