package ais.action.master.dashboard.admin;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Row;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.action.master.helper.MahasiswaPunyaKegiatanKemahasiswaanHelper;
import ais.action.master.helper.MahasiswaPunyaOrganisasiIntraKampusHelper;
import ais.common.Common;
import ais.database.model.Mahasiswa;
import ais.ui.util.MyInclude;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyWindow;

public class DashboardKegiatanKemahasiswaan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;
	private Mahasiswa mahasiswa;

	public DashboardKegiatanKemahasiswaan() {
		super();

		try {
			mahasiswa = Common.getCurrentUser().getMahasiswa();
			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardKegiatanKemahasiswaan(Mahasiswa mahasiswa) {
		super();
		this.mahasiswa = mahasiswa;
		try {
			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardKegiatanKemahasiswaan(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			mahasiswa = Common.getCurrentUser().getMahasiswa();
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

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(this);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Row row = Common.tampilanScroll1(center);

		Tabbox tabbox = new Tabbox();
		tabbox.setHeight("4000px");
		tabbox.setParent(row);
		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tabKegiatan = new MyTabConfig("Kegiatan Kemahasiswaan");
		tabKegiatan.setParent(tabs);

		MyTabConfig tabOrganisasi = new MyTabConfig("Organisasi");
		tabOrganisasi.setParent(tabs);

		MyTabConfig tabPrestasi = new MyTabConfig("Prestasi");
		tabPrestasi.setParent(tabs);

		MyTabConfig tabKarya = new MyTabConfig("Karya");
		tabKarya.setParent(tabs);

		MyTabConfig tabForm = new MyTabConfig("Form Kegiatan");
		tabForm.setParent(tabs);

		MyTabConfig tabCatatan = new MyTabConfig("Catatan Mahasiswa");
		tabCatatan.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelKegiatan = new ais.ui.util.MyTabpanel();
		tabpanelKegiatan.setParent(tabpanels);
		MahasiswaPunyaKegiatanKemahasiswaanHelper detailperkuliahanHelper = new MahasiswaPunyaKegiatanKemahasiswaanHelper();
		detailperkuliahanHelper.display(mahasiswa, tabpanelKegiatan);

		final Tabpanel tabpanelOrganisasi = new ais.ui.util.MyTabpanel();
		tabpanelOrganisasi.setParent(tabpanels);
		tabOrganisasi.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelOrganisasi.getChildren().isEmpty()) {
					MahasiswaPunyaOrganisasiIntraKampusHelper detailperkuliahanHelper = new MahasiswaPunyaOrganisasiIntraKampusHelper();
					detailperkuliahanHelper.display(mahasiswa, tabpanelOrganisasi);
				}
			}

		});

		final Tabpanel tabpanelPrestasi = new ais.ui.util.MyTabpanel();
		tabpanelPrestasi.setParent(tabpanels);
		tabPrestasi.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelPrestasi.getChildren().isEmpty()) {
					MyInclude iframe = new MyInclude(
							"/pages/master/prestasi_mahasiswa.zul?mahasiswa=" + mahasiswa.getId());
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
					MyInclude iframe = new MyInclude(
							"/pages/master/penghargaan_mahasiswa.zul?mahasiswa=" + mahasiswa.getId());
					iframe.setParent(tabpanelKarya);
				}
			}

		});

		final Tabpanel tabpanelForm = new ais.ui.util.MyTabpanel();
		tabpanelForm.setParent(tabpanels);
		tabForm.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelForm.getChildren().isEmpty()) {
					MyInclude iframe = new MyInclude(
							"/pages/master/formulir_kegiatan_peserta.zul?mahasiswa=" + mahasiswa.getId());
					iframe.setParent(tabpanelForm);
				}
			}

		});

		final Tabpanel tabpanelCatatan = new ais.ui.util.MyTabpanel();
		tabpanelCatatan.setParent(tabpanels);
		tabCatatan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelCatatan.getChildren().isEmpty()) {
					MyInclude iframe = new MyInclude("/pages/master/catatan_mahasiswa.zul?mahasiswa=" + mahasiswa.getId());
					iframe.setParent(tabpanelCatatan);
				}
			}

		});

	}

}
