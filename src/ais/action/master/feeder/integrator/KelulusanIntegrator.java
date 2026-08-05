package ais.action.master.feeder.integrator;

import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.action.master.feeder.integrator.helper.DownloadKelulusan;
import ais.action.master.feeder.integrator.helper.UploadKelulusan;
import ais.common.Common;
import ais.database.model.Konfigurasi;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyWindow;

public class KelulusanIntegrator extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3384689142222653374L;

	public KelulusanIntegrator() {
		super();
		init();
	}

	public KelulusanIntegrator(String title, String border, boolean closable) {
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

		MyTabConfig tab1 = new MyTabConfig("Download Kelulusan");
		tab1.setParent(tabs);

		MyTabConfig tab2 = new MyTabConfig("Upload Kelulusan");
		tab2.setParent(tabs);

		tab2.setVisible(
				Common.bolehKonfigurasi("aktifkan_upload_kelulusan_pada_menu_feeder_integrator"));

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);

		Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
		tabpanel2.setParent(tabpanels);

		DownloadKelulusan laporan = new DownloadKelulusan();
		laporan.setHeight("2000px");
		laporan.setWidth("100%");
		laporan.setParent(tabpanel1);

		UploadKelulusan upload = new UploadKelulusan();
		upload.setHeight("2000px");
		upload.setWidth("100%");
		upload.setParent(tabpanel2);
	}

}
