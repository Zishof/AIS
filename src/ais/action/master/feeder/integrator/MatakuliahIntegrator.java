package ais.action.master.feeder.integrator;

import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.action.master.feeder.integrator.helper.DownloadMatakuliah;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyWindow;

public class MatakuliahIntegrator extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3384689142222653374L;

	public MatakuliahIntegrator() {
		super();
		init();
	}

	public MatakuliahIntegrator(String title, String border, boolean closable) {
		super(title, border, closable);
		init();
	}

	private void init() {
		Tabbox tabbox = new Tabbox();
		tabbox.setParent(ais.common.Common.tampilanScroll(this));
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab1 = new MyTabConfig("Download Matakuliah");
		tab1.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);

		DownloadMatakuliah laporan = new DownloadMatakuliah();
		laporan.setHeight("2000px");
		laporan.setWidth("100%");
		laporan.setParent(tabpanel1);

	}

}
