package ais.action.master.dashboard.admin;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.common.Common;
import ais.ui.util.MyInclude;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyWindow;

public class DashboardKegiatanKemahasiswaanAdmin extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	public DashboardKegiatanKemahasiswaanAdmin() {
		super();

		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardKegiatanKemahasiswaanAdmin(String title, String border, boolean closable) {
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

		MyTabConfig tabDasbor = new MyTabConfig("Dasbor");
		tabDasbor.setParent(tabs);

		MyTabConfig tabKegiatan = new MyTabConfig("Kegiatan Mahasiswa");
		tabKegiatan.setParent(tabs);

		MyTabConfig tabOrganisasi = new MyTabConfig("Organisasi Mahasiswa");
		tabOrganisasi.setParent(tabs);

		MyTabConfig tabPrestasi = new MyTabConfig("Prestasi Mahasiswa");
		tabPrestasi.setParent(tabs);

		MyTabConfig tabKarya = new MyTabConfig("Karya Mahasiswa");
		tabKarya.setParent(tabs);

		MyTabConfig tabCatatan = new MyTabConfig("Catatan Mahasiswa");
		tabCatatan.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		final Tabpanel tabpanelDasbor = new ais.ui.util.MyTabpanel();
		tabpanelDasbor.setParent(tabpanels);
		tabpanelDasbor.setHeight("100%");

		DasboardAktivitasMahasiswa dasboardAktivitasMahasiswa = new DasboardAktivitasMahasiswa();
		dasboardAktivitasMahasiswa.setHeight("100%");
		dasboardAktivitasMahasiswa.setWidth("100%");
		dasboardAktivitasMahasiswa.setParent(tabpanelDasbor);

		final Tabpanel tabpanelKegiatan = new ais.ui.util.MyTabpanel();
		tabpanelKegiatan.setParent(tabpanels);

		tabKegiatan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelKegiatan.getChildren().isEmpty()) {
					MyInclude iframe = new MyInclude("/pages/master/kegiatan_kemahasiswaan.zul");
					iframe.setParent(tabpanelKegiatan);
				}
			}

		});

		final Tabpanel tabpanelOrganisasi = new ais.ui.util.MyTabpanel();
		tabpanelOrganisasi.setParent(tabpanels);
		tabOrganisasi.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelOrganisasi.getChildren().isEmpty()) {
					MyInclude iframe = new MyInclude("/pages/master/organisasi_intra_kampus.zul");
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
					MyInclude iframe = new MyInclude("/pages/master/prestasi_mahasiswa.zul");
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
					MyInclude iframe = new MyInclude("/pages/master/penghargaan_mahasiswa.zul");
					iframe.setParent(tabpanelKarya);
				}
			}

		});

		final Tabpanel tabpanelCatatan = new ais.ui.util.MyTabpanel();
		tabpanelCatatan.setParent(tabpanels);
		tabCatatan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelCatatan.getChildren().isEmpty()) {
					MyInclude iframe = new MyInclude("/pages/master/catatan_mahasiswa.zul");
					iframe.setParent(tabpanelCatatan);
				}
			}

		});

	}

}
