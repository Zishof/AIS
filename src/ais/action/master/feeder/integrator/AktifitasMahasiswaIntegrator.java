package ais.action.master.feeder.integrator;

import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.action.master.feeder.integrator.helper.DownloadAktifitasMahasiwaKkn;
import ais.action.master.feeder.integrator.helper.DownloadAktifitasMahasiwaMahasiswaRequestTugasAkhir;
import ais.action.master.feeder.integrator.helper.DownloadAktifitasMahasiwaPkl;
import ais.action.master.feeder.integrator.helper.DownloadAktifitasMahasiwaSkripsi;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyWindow;

public class AktifitasMahasiswaIntegrator extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3384689142222653374L;

	public AktifitasMahasiswaIntegrator() {
		super();
		init();
	}

	public AktifitasMahasiswaIntegrator(String title, String border, boolean closable) {
		super(title, border, closable);
		init();
	}

	private void init() {
		Tabbox tabbox = new Tabbox();
		tabbox.setSclass("ais-aktifitas-tabbox");
		tabbox.setParent(ais.common.Common.tampilanScroll(this));
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab1 = new MyTabConfig("Download KKN");
		tab1.setParent(tabs);

		tab1 = new MyTabConfig("Download PKL");
		tab1.setParent(tabs);

		tab1 = new MyTabConfig("Download Skripsi");
		tab1.setParent(tabs);

		tab1 = new MyTabConfig("Download Bimbingan");
		tab1.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);

		DownloadAktifitasMahasiwaKkn laporan = new DownloadAktifitasMahasiwaKkn();
		laporan.setHeight("2000px");
		laporan.setWidth("100%");
		laporan.setParent(tabpanel1);

		tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);

		DownloadAktifitasMahasiwaPkl laporan1 = new DownloadAktifitasMahasiwaPkl();
		laporan1.setHeight("2000px");
		laporan1.setWidth("100%");
		laporan1.setParent(tabpanel1);

		tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);

		DownloadAktifitasMahasiwaSkripsi laporan2 = new DownloadAktifitasMahasiwaSkripsi();
		laporan2.setHeight("2000px");
		laporan2.setWidth("100%");
		laporan2.setParent(tabpanel1);

		tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);

		DownloadAktifitasMahasiwaMahasiswaRequestTugasAkhir laporan3 = new DownloadAktifitasMahasiwaMahasiswaRequestTugasAkhir();
		laporan3.setHeight("2000px");
		laporan3.setWidth("100%");
		laporan3.setParent(tabpanel1);

	}

}
