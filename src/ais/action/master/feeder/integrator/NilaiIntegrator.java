package ais.action.master.feeder.integrator;

import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.action.master.feeder.integrator.helper.DownloadNilai;
import ais.action.master.feeder.integrator.helper.UploadNilai;
import ais.common.Common;
import ais.database.model.Konfigurasi;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyWindow;

public class NilaiIntegrator extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3384689142222653374L;

	public NilaiIntegrator() {
		super();
		init();
	}

	public NilaiIntegrator(String title, String border, boolean closable) {
		super(title, border, closable);
		init();
	}

	private void init() {
		Tabbox tabbox = new Tabbox();
		tabbox.setParent(Common.tampilanScrollTabbox(this));
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab1 = new MyTabConfig("Download Nilai");
		tab1.setParent(tabs);

		MyTabConfig tab2 = new MyTabConfig("Upload Nilai");
		tab2.setParent(tabs);

		tab2.setVisible(Common.bolehKonfigurasi("aktifkan_upload_nilai_pada_menu_feeder_integrator"));

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);

		Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
		tabpanel2.setVisible(tab2.isVisible());
		tabpanel2.setParent(tabpanels);

		DownloadNilai laporan = new DownloadNilai();
		laporan.setHeight("2000px");
		laporan.setWidth("100%");
		laporan.setParent(tabpanel1);

		UploadNilai upload = new UploadNilai();
		upload.setHeight("2000px");
		upload.setWidth("100%");
		upload.setParent(tabpanel2);
	}

}
