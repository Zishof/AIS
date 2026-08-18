package ais.action.master.feeder.integrator;

import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.action.master.feeder.integrator.helper.DownloadKelas;
import ais.action.master.feeder.integrator.helper.UploadKelas;
import ais.common.Common;
import ais.database.model.Konfigurasi;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyWindow;

public class KelasIntegrator extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3384689142222653374L;

	public KelasIntegrator() {
		super();
		init();
	}

	public KelasIntegrator(String title, String border, boolean closable) {
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

		MyTabConfig tab1 = new MyTabConfig("Download Kelas");
		tab1.setParent(tabs);

		MyTabConfig tab2 = new MyTabConfig("Upload Kelas");
		tab2.setParent(tabs);

		tab2.setVisible(Common.bolehKonfigurasi("aktifkan_upload_kelas_pada_menu_feeder_integrator"));

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);

		Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
		tabpanel2.setParent(tabpanels);

		DownloadKelas laporan = new DownloadKelas();
		laporan.setHeight("2000px");
		laporan.setWidth("100%");
		laporan.setParent(tabpanel1);

		UploadKelas upload = new UploadKelas();
		upload.setHeight("2000px");
		upload.setWidth("100%");
		upload.setParent(tabpanel2);
	}

}
