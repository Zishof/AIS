package ais.action.master.helper;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.model.KurikulumPunyaMatakuliahDetail;
import ais.database.model.Tbmuser;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KurikulumPunyaMatakuliahHelper {

	private DataLoader dataLoader;
	private MyWindow window;
	private KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail;

	private VideoPertemuanHelper videoPertemuanHelper;
	private AudioPertemuanHelper audioPertemuanHelper;
	private FilePerkuliahanHelper filePerkuliahanHelper;

	int index = 0;

	public KurikulumPunyaMatakuliahHelper() {
		Tbmuser tbmuser = Common.getCurrentUser();
		filePerkuliahanHelper = new FilePerkuliahanHelper(tbmuser.getMahasiswa(), null);
		videoPertemuanHelper = new VideoPertemuanHelper(
				tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null && tbmuser.ambilDosen() == null, false);
		audioPertemuanHelper = new AudioPertemuanHelper(
				tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null && tbmuser.ambilDosen() == null, false);
	}

	public void init() throws Exception {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(center);

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tabFile = new MyTabConfig("File");
		tabFile.setSelected(index == 0);
		tabFile.setParent(tabs);
		MyTabConfig tabAudio = new MyTabConfig("Audio");
		tabAudio.setSelected(index == 1);
		tabAudio.setParent(tabs);
		MyTabConfig tabVideo = new MyTabConfig("Video");
		tabVideo.setSelected(index == 2);
		tabVideo.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);
		Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
		tabpanelUtama.setParent(tabpanels);

		filePerkuliahanHelper.createFile(null, null, null, kurikulumPunyaMatakuliahDetail, tabpanelUtama, null);

		Tabpanel tabpanelAudio = new ais.ui.util.MyTabpanel();
		tabpanelAudio.setParent(tabpanels);
		audioPertemuanHelper.display(null, null, kurikulumPunyaMatakuliahDetail, tabpanelAudio, null);

		Tabpanel tabpanelVideo = new ais.ui.util.MyTabpanel();
		tabpanelVideo.setParent(tabpanels);
		videoPertemuanHelper.display(null, null, kurikulumPunyaMatakuliahDetail, tabpanelVideo, null);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				dataLoader.loadData(null);
				window.detach();
			}
		});
		cancel.setParent(toolbar);

	}

	public void display(final KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail,
			final DataLoader dataLoader, int index) throws Exception {

		this.index = index;
		window = new MyWindow();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

		Common.clear(window);
		window.setWidth("98%");
		window.setHeight("98%");

		this.dataLoader = dataLoader;

		this.kurikulumPunyaMatakuliahDetail = kurikulumPunyaMatakuliahDetail;
		init();
		this.window.setVisible(true);

		this.window.onModal();
	}

}
