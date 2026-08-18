package ais.action.master.dashboard.admin;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyInclude;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.common.Common;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyWindow;

public class DashboardKegiatanKedosenanAdmin extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	public DashboardKegiatanKedosenanAdmin() {
		super();

		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	public DashboardKegiatanKedosenanAdmin(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	private void init() throws Exception {

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(center);
		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tabKegiatan = new MyTabConfig("Kegiatan Dosen");
		tabKegiatan.setParent(tabs);

		MyTabConfig tabOrganisasi = new MyTabConfig("Organisasi Dosen");
		tabOrganisasi.setParent(tabs);

		MyTabConfig tabPrestasi = new MyTabConfig("Prestasi Dosen");
		tabPrestasi.setParent(tabs);

		MyTabConfig tabKarya = new MyTabConfig("Karya Dosen");
		tabKarya.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelKegiatan = new ais.ui.util.MyTabpanel();
		tabpanelKegiatan.setParent(tabpanels);
		MyInclude iframe = new MyInclude("/pages/master/kegiatan_kedosenan.zul");
		tabpanelKegiatan.appendChild(iframe);

		final Tabpanel tabpanelOrganisasi = new ais.ui.util.MyTabpanel();
		tabpanelOrganisasi.setParent(tabpanels);
		tabOrganisasi.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelOrganisasi.getChildren().isEmpty()) {
					MyInclude iframe = new MyInclude("/pages/master/organisasi_dosen.zul");
					iframe.setParent(tabpanelOrganisasi);
				}
			}

		});

		final Tabpanel tabpanelPrestasi = new ais.ui.util.MyTabpanel();
		tabpanelPrestasi.setParent(tabpanels);
		tabPrestasi.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelPrestasi.getChildren().isEmpty()) {
					MyInclude iframe = new MyInclude("/pages/master/prestasi_dosen.zul");
					iframe.setParent(tabpanelPrestasi);
				}
			}

		});

		final Tabpanel tabpanelKarya = new ais.ui.util.MyTabpanel();
		tabpanelKarya.setParent(tabpanels);
		tabKarya.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelKarya.getChildren().isEmpty()) {
					MyInclude iframe = new MyInclude("/pages/master/penghargaan_dosen.zul");
					iframe.setParent(tabpanelKarya);
				}
			}

		});

	}

}
