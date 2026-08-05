package ais.action.master.helper;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.model.GrupPertemuan;
import ais.database.model.Mahasiswa;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class GrupPertemuanHelper {

	private DataLoader dataLoader;
	private MyWindow window;
	private GrupPertemuan grupPertemuan;

	private VideoGrupPertemuanHelper videoGrupPertemuanHelper;
	private AudioGrupPertemuanHelper audioGrupPertemuanHelper;
	private FilePerkuliahanHelper filePerkuliahanHelper;

	private AbsensiGrupPertemuanHelper absensiHelper;

	private Mahasiswa mahasiswa;
	private Tabpanel tabpanelFileGrupPertemuan;

	private Textbox catatan;
	private int index = 0;

	public GrupPertemuanHelper(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
		filePerkuliahanHelper = new FilePerkuliahanHelper(mahasiswa, null);
		videoGrupPertemuanHelper = new VideoGrupPertemuanHelper(mahasiswa == null);
		audioGrupPertemuanHelper = new AudioGrupPertemuanHelper(mahasiswa == null);
		absensiHelper = new AbsensiGrupPertemuanHelper(mahasiswa, null);
	}

	@SuppressWarnings("deprecation")
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

		MyTabConfig tab = new MyTabConfig("Presensi kehadiran");
		tab.setSelected(index == 0);
		tab.setParent(tabs);
		tab = new MyTabConfig("Catatan Konsultasi");
		tab.setSelected(index == 1);
		tab.setParent(tabs);
		MyTabConfig tabFile = new MyTabConfig("File");
		tabFile.setSelected(index == 2);
		tabFile.setParent(tabs);
		MyTabConfig tabAudio = new MyTabConfig("Audio");
		tabAudio.setSelected(index == 3);
		tabAudio.setParent(tabs);
		MyTabConfig tabVideo = new MyTabConfig("Video");
		tabVideo.setSelected(index == 4);
		tabVideo.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);
		Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
		tabpanelUtama.setParent(tabpanels);

		Tabpanel tabpanelCatatan = new ais.ui.util.MyTabpanel();
		tabpanelCatatan.setParent(tabpanels);

		Borderlayout myBorderlayout = new ais.ui.util.MyBorderlayout();
		myBorderlayout.setParent(tabpanelCatatan);

		Center myCenter = new Center();
		ais.ui.util.ZkCompat.setFlex(myCenter, true);
		myCenter.setParent(myBorderlayout);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(myCenter);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setValign("top");
		row.setParent(rows);

		new MyLabelBoldAja("Catatan Konsultasi").setParent(row);

		catatan = new Textbox();
		catatan.setValue(grupPertemuan.getCatatan());
		catatan.setRows(25);
		catatan.setWidth("100%");
		catatan.setParent(row);

		catatan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				grupPertemuan.setCatatan(catatan.getValue());
				Common.refreshSaveOrUpdate(grupPertemuan);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		Hbox hbox = new Hbox();
		hbox.setParent(row);
		LampiranLain.createDownloadUploadFileLain(hbox, grupPertemuan.getId(), LampiranLain.CATATAN_KONSULTASI,
				LampiranLain.CATATAN_KONSULTASI, false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

					}
				}, null, false, false, false, false);

		tabpanelFileGrupPertemuan = new ais.ui.util.MyTabpanel();
		tabpanelFileGrupPertemuan.setParent(tabpanels);

		absensiHelper.mainInit(grupPertemuan, tabpanelUtama);
		filePerkuliahanHelper.createFile(null, grupPertemuan, null, null, tabpanelFileGrupPertemuan, null);

		Tabpanel tabpanelAudio = new ais.ui.util.MyTabpanel();
		tabpanelAudio.setParent(tabpanels);
		audioGrupPertemuanHelper.display(grupPertemuan, tabpanelAudio);

		Tabpanel tabpanelVideo = new ais.ui.util.MyTabpanel();
		tabpanelVideo.setParent(tabpanels);
		videoGrupPertemuanHelper.display(grupPertemuan, tabpanelVideo);

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
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setVisible(mahasiswa == null);
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (absensiHelper.save()) {
					dataLoader.loadData(null);
					window.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);

	}

	public void display(final GrupPertemuan grupPertemuan, final DataLoader dataLoader, int index) throws Exception {

		this.index = index;
		window = new MyWindow();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

		Common.clear(window);
		window.setTitle("Manajemen Konsultasi");
		window.setWidth("98%");
		window.setHeight("98%");

		this.dataLoader = dataLoader;

		this.grupPertemuan = grupPertemuan;
		init();
		this.window.setVisible(true);

		this.window.onModal();
	}

}
