package ais.action.master.sop;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.AmbilDataFormSopBanbox;
import ais.action.master.helper.AmbilDataMenuBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataTbmuserBanyak;
import ais.action.master.sop.helper.SopUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Menu;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.sop.AktorSop;
import ais.database.model.sop.AlurSop;
import ais.database.model.sop.DokumenAlurSop;
import ais.database.model.sop.KelompokParameterTambahanAlurSop;
import ais.database.model.sop.Sop;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk alur sop. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchkode}, {@code Combobox
 * searchsop}, {@code Vbox diagramSopContainer}, {@code Checkbox searchaktif}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code setupTabbedWorkspaceAlurSop()}, {@code init()}, {@code
 * init()}, {@code initDokumen()}); pembacaan/pencarian ({@code onSearchDefault()}, {@code
 * getSopFilterTerpilih()}, {@code ambilAlurSopUntukDiagram()}, {@code ambilDokumenMapUntukDiagram()}, {@code
 * reloadDataMenu()}, {@code reloadMenu()}); mutasi data ({@code onSave()}, {@code resetPagingDanGridKosong()});
 * pelaporan/ekspor ({@code registerSopFilterAutoRender()}, {@code renderSopBelumDipilihPadaDiagram()}, {@code
 * renderSopBelumDipilihPadaTabel()}, {@code renderDiagramSopTerpilih()}, {@code renderDiagramSop()}); operasi
 * domain lain ({@code autoBukaEditAlurSopJikaDiminta()}, {@code buildDiagramScrollStyleAlurSop()}, {@code
 * buildTabelScrollStyleAlurSop()}, {@code appendStyleAlurSop()}, {@code showDiagramTab()}, {@code onAdd()}).
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class AlurSopAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 *
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkode;
	private Combobox searchsop;
	private Vbox diagramSopContainer;
	private Checkbox searchaktif;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private AlurSop alurSop;
	// Bila halaman dibuka via Executions.createComponents dengan arg "autoEditAlurSopId" (mis. dari
	// popup "Edit SOP" di TampilanAlurSopAction), id ini menandai langkah yang harus langsung dibukakan
	// form Ubah-nya setelah daftar dimuat. Lihat autoBukaEditAlurSopJikaDiminta().
	private Long autoEditAlurSopId;
	private MyToolbarbuttonConfig add;
	private Textbox kode;
	private Combobox role;
	private Textbox khususUsername;
	private Combobox sebelumnya;
	private Set<DokumenAlurSop> selectedDokumenAlurSop;
	private MyCheckboxConfig start;
	private Textbox aktor;

	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;

	private Hbox hbFakultasLabel;
	private Hbox hbFakultas;

	private Hbox hbYayasanLabel;
	private Hbox hbYayasan;
	private MyCheckboxConfig alurSetelahnyaOtomatis;
	private MyCheckboxConfig alurSetelahnyaBerupaPilihan;
	private Combobox setelahnya;
	private Combobox setelahnya2;
	private Combobox setelahnya3;
	private Combobox setelahnya4;
	private Combobox setelahnya5;
	private Textbox opsi;
	private Intbox jangkaWaktu;
	private Set<KelompokParameterTambahanAlurSop> selectedKelompokParameterTambahanAlurSop;
	private JSONArray array;
	private Combobox aktorSop;
	private MyCheckboxConfig kembaliKePengaju;
	private AmbilDataFormSopBanbox formInputan;

	private MyCheckboxConfig bekukanFormTampilan;
	private MyCheckboxConfig bolehDiisiCatatan;
	private MyCheckboxConfig catatanWajibDiisi;
	private MyCheckboxConfig jikaProsesDisetujuiMakaSelesai;
	private MyCheckboxConfig kembaliKeAktorSebelumnya;
	private Combobox setelahnya6;
	private Combobox setelahnya7;
	private Combobox setelahnya8;
	private Combobox setelahnya9;
	private Combobox setelahnya10;

	private Textbox opsiSetelahnya;
	private Textbox opsiSetelahnya2;
	private Textbox opsiSetelahnya3;
	private Textbox opsiSetelahnya4;
	private Textbox opsiSetelahnya5;

	private Textbox opsiSetelahnya6;
	private Textbox opsiSetelahnya7;
	private Textbox opsiSetelahnya8;
	private Textbox opsiSetelahnya9;
	private Textbox opsiSetelahnya10;
	private MyCheckboxConfig persetujuanAdaDiSini1;
	private MyCheckboxConfig persetujuanAdaDiSini2;
	private MyCheckboxConfig persetujuanAdaDiSini3;
	private MyCheckboxConfig persetujuanAdaDiSini4;
	private MyCheckboxConfig persetujuanAdaDiSini5;
	private MyCheckboxConfig persetujuanAdaDiSini6;
	private MyCheckboxConfig persetujuanAdaDiSini7;
	private MyCheckboxConfig persetujuanAdaDiSini8;
	private MyCheckboxConfig persetujuanAdaDiSini9;
	private MyCheckboxConfig persetujuanAdaDiSini10;
	private MyCheckboxConfig persetujuanAdaDiSini;
	private MyCheckboxConfig bekukanDokumen;
	private MyCheckboxConfig tanggalDisposisiBolehDiubah;
	private MyCheckboxConfig alurSetelahnyaTidakWajib;
	private MyCheckboxConfig penolakanAdaDiSini;
	private Combobox setelahnya11;
	private Textbox opsiSetelahnya11;
	private MyCheckboxConfig persetujuanAdaDiSini11;
	private Combobox setelahnya12;
	private Textbox opsiSetelahnya12;
	private MyCheckboxConfig persetujuanAdaDiSini12;
	private Combobox setelahnya13;
	private Textbox opsiSetelahnya13;
	private MyCheckboxConfig persetujuanAdaDiSini13;
	private Combobox setelahnya14;
	private Textbox opsiSetelahnya14;
	private MyCheckboxConfig persetujuanAdaDiSini14;
	private Combobox setelahnya15;
	private Textbox opsiSetelahnya15;
	private MyCheckboxConfig persetujuanAdaDiSini15;
	private Combobox setelahnya16;
	private Textbox opsiSetelahnya16;
	private MyCheckboxConfig persetujuanAdaDiSini16;
	private Combobox setelahnya17;
	private Textbox opsiSetelahnya17;
	private MyCheckboxConfig persetujuanAdaDiSini17;
	private Combobox setelahnya18;
	private Textbox opsiSetelahnya18;
	private MyCheckboxConfig persetujuanAdaDiSini18;
	private Combobox setelahnya19;
	private Textbox opsiSetelahnya19;
	private MyCheckboxConfig persetujuanAdaDiSini19;
	private Combobox setelahnya20;
	private Textbox opsiSetelahnya20;
	private MyCheckboxConfig persetujuanAdaDiSini20;
	private MyCheckboxConfig lampiranCatatanWajibDiisi;
	private Tabbox tabboxAlurSop;
	private Tab tabDiagramAlurSop;
	private Tab tabTabelAlurSop;
	private Tabpanel tabpanelDiagramAlurSop;
	private Tabpanel tabpanelTabelAlurSop;
	private Vbox diagramScrollContainer;
	private Vbox tabelScrollContainer;
	private Map<Long, List<DiagramDokumenInfo>> diagramDokumenByAlur = new HashMap<Long, List<DiagramDokumenInfo>>();

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah, true, false);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		if (searchyayasan != null) {
			searchyayasan.getParent().setVisible(Common.bolehKonfigurasi("user_yayasan", Konfigurasi.TIDAK_AKTIF));
		}

		if (hbFakultasLabel != null) {
			hbFakultasLabel.setVisible(
					Common.bolehKonfigurasi("user_fakultas"));
		}

		if (hbFakultas != null) {
			hbFakultas.setVisible(
					Common.bolehKonfigurasi("user_fakultas"));
		}

		if (hbYayasanLabel != null) {
			hbYayasanLabel.setVisible(Common.bolehKonfigurasi("user_yayasan", Konfigurasi.TIDAK_AKTIF));
		}

		if (hbYayasan != null) {
			hbYayasan.setVisible(Common.bolehKonfigurasi("user_yayasan", Konfigurasi.TIDAK_AKTIF));
		}

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Criterion criterion = Restrictions.eq("aktif", true);

				criterion = Restrictions.and(criterion,
						searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("jurusan"),
										CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false)));

				criterion = Restrictions.and(criterion,
						searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("fakultas"),
										CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)));

				criterion = Restrictions.and(criterion,
						searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("sekolah"),
										CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false)));

				criterion = Restrictions.and(criterion,
						searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("yayasan"),
										CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false)));

				Common.insertComboDanSemua(searchsop, "nama", Sop.class, criterion);
			}
		};

		eventListener.onEvent(null);

		setupTabbedWorkspaceAlurSop();
		registerSopFilterAutoRender();

		String[] contents = new String[] { "id", "kode", "nama", "sop", "sebelumnya", "opsi", "jangkaWaktu",
				"formInputan", "bekukanFormTampilan", "setelahnya", "setelahnya2", "setelahnya3", "setelahnya4",
				"setelahnya5", "setelahnya6", "setelahnya7", "setelahnya8", "setelahnya9", "setelahnya10",

				"setelahnya11", "setelahnya12", "setelahnya13", "setelahnya14", "setelahnya15", "setelahnya16",
				"setelahnya17", "setelahnya18", "setelahnya19", "setelahnya20",

				"opsiSetelahnya", "opsiSetelahnya2", "opsiSetelahnya3", "opsiSetelahnya4", "opsiSetelahnya5",
				"opsiSetelahnya6", "opsiSetelahnya7", "opsiSetelahnya8", "opsiSetelahnya9", "opsiSetelahnya10",

				"opsiSetelahnya11", "opsiSetelahnya12", "opsiSetelahnya13", "opsiSetelahnya14", "opsiSetelahnya15",
				"opsiSetelahnya16", "opsiSetelahnya17", "opsiSetelahnya18", "opsiSetelahnya19", "opsiSetelahnya20",

				"role", "khususUsername", "nomor", "jangkaWaktu", "halamanMenu", "bolehDiisiCatatan",
				"catatanWajibDiisi", "keterangan", "kembaliKePengaju", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(AlurSop.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, AlurSop.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		// Dukungan "buka langsung Edit" dari layar lain (mis. popup Edit SOP di TampilanAlurSopAction).
		try {
			org.zkoss.zk.ui.Execution execAutoEdit = org.zkoss.zk.ui.Executions.getCurrent();
			if (execAutoEdit != null && execAutoEdit.getArg() != null
					&& execAutoEdit.getArg().get("autoEditAlurSopId") != null) {
				autoEditAlurSopId = Long.valueOf(execAutoEdit.getArg().get("autoEditAlurSopId").toString().trim());
			}
		} catch (Exception exArg) {
			autoEditAlurSopId = null;
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
				autoBukaEditAlurSopJikaDiminta();
			}
		});
	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Bila halaman dibuka dengan arg {@code autoEditAlurSopId}, langsung buka form Ubah Alur SOP
	 * (Window modal milik AlurSopAction) untuk langkah tersebut. Dipanggil sekali setelah daftar
	 * dimuat. Semua komponen sudah ter-wire penuh sehingga simpan/refresh berjalan normal.
	 */
	private void autoBukaEditAlurSopJikaDiminta() {
		if (autoEditAlurSopId == null) {
			return;
		}
		final Long idEdit = autoEditAlurSopId;
		autoEditAlurSopId = null; // cukup sekali
		try {
			AlurSop alurSopEdit = (AlurSop) HibernateUtil.currentSession().get(AlurSop.class, idEdit);
			if (alurSopEdit != null) {
				init((GeneralValueObject) alurSopEdit);
			} else {
				MyMessageboxConfig.show("Mohon maaf, data langkah Alur SOP tidak ditemukan. Langkah yang dapat dilakukan: (1) periksa kembali langkah yang dipilih; (2) muat ulang halaman dan coba kembali; (3) hubungi Administrator jika data memang tidak tersedia.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}


	private static class DiagramDokumenInfo {
		private String kode;
		private String nama;
		private boolean wajib;

		private DiagramDokumenInfo(String kode, String nama, boolean wajib) {
			this.kode = kode;
			this.nama = nama;
			this.wajib = wajib;
		}
	}


	private void setupTabbedWorkspaceAlurSop() {
		try {
			if (grid == null || diagramSopContainer == null) {
				return;
			}
			if (tabboxAlurSop != null) {
				return;
			}

			Component parent = diagramSopContainer.getParent();
			if (parent == null) {
				parent = grid.getParent();
			}
			if (parent == null) {
				return;
			}

			try {
				diagramSopContainer.detach();
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			try {
				grid.detach();
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

			/*
			 * Area tab dibuat minimal 2600px. Isi tab tidak dipaksa melebar/meninggi
			 * mengikuti konten, sehingga ketika diagram atau tabel sangat panjang,
			 * scroll vertikal muncul di dalam tab masing-masing dan halaman utama tetap
			 * rapi.
			 */
			tabboxAlurSop = new Tabbox();
			tabboxAlurSop.setWidth("100%");
			tabboxAlurSop.setHeight("2600px");
			tabboxAlurSop.setStyle("border:0;background:#f8fafc;height:2600px;min-height:2600px;"
					+ "max-height:2600px;overflow:auto;box-sizing:border-box;");

			Tabs tabs = new Tabs();
			tabs.setParent(tabboxAlurSop);

			tabDiagramAlurSop = new Tab("Diagram Alur SOP");
			tabDiagramAlurSop.setParent(tabs);
			tabDiagramAlurSop.setSelected(true);

			tabTabelAlurSop = new Tab("Tabel Data Alur");
			tabTabelAlurSop.setParent(tabs);

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabboxAlurSop);
			tabpanels.setHeight("2560px");
			tabpanels.setStyle("height:1560px;min-height:1560px;max-height:1560px;overflow:auto;box-sizing:border-box;");

			tabpanelDiagramAlurSop = new ais.ui.util.MyTabpanel();
			tabpanelDiagramAlurSop.setParent(tabpanels);
			tabpanelDiagramAlurSop.setHeight("1560px");
			tabpanelDiagramAlurSop.setStyle("padding:0;background:#f8fafc;height:1560px;min-height:1560px;"
					+ "max-height:1560px;overflow:auto;box-sizing:border-box;");

			diagramScrollContainer = new Vbox();
			diagramScrollContainer.setWidth("100%");
			diagramScrollContainer.setHeight("2540px");
			diagramScrollContainer.setStyle(buildDiagramScrollStyleAlurSop());
			diagramScrollContainer.setParent(tabpanelDiagramAlurSop);

			diagramSopContainer.setWidth("100%");
			diagramSopContainer.setHeight("auto");
			diagramSopContainer.setStyle("width:100%;min-height:1520px;height:auto;overflow:visible;"
					+ "background:transparent;box-sizing:border-box;");
			diagramSopContainer.setParent(diagramScrollContainer);

			tabpanelTabelAlurSop = new ais.ui.util.MyTabpanel();
			tabpanelTabelAlurSop.setParent(tabpanels);
			tabpanelTabelAlurSop.setHeight("2560px");
			tabpanelTabelAlurSop.setStyle("padding:0;background:#ffffff;height:1560px;min-height:1560px;"
					+ "max-height:1560px;overflow:auto;box-sizing:border-box;");

			tabelScrollContainer = new Vbox();
			tabelScrollContainer.setWidth("100%");
			tabelScrollContainer.setHeight("2540px");
			tabelScrollContainer.setStyle(buildTabelScrollStyleAlurSop());
			tabelScrollContainer.setParent(tabpanelTabelAlurSop);

			grid.setWidth("100%");
			grid.setHeight("auto");
			grid.setStyle(appendStyleAlurSop(grid.getStyle(), "width:100%;height:auto;min-height:600px;"
					+ "box-sizing:border-box;overflow:visible;background:#ffffff;"));
			grid.setParent(tabelScrollContainer);

			tabboxAlurSop.setParent(parent);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private String buildDiagramScrollStyleAlurSop() {
		return "height:2540px;min-height:2540px;max-height:2540px;overflow-y:auto;overflow-x:auto;"
				+ "padding:10px;box-sizing:border-box;background:#f8fafc;";
	}

	private String buildTabelScrollStyleAlurSop() {
		return "height:2540px;min-height:2540px;max-height:2540px;overflow-y:auto;overflow-x:hidden;"
				+ "padding:0;box-sizing:border-box;background:#ffffff;";
	}

	private String appendStyleAlurSop(String currentStyle, String additionalStyle) {
		String style = currentStyle == null ? "" : currentStyle.trim();
		if (style.length() > 0 && !style.endsWith(";")) {
			style = style + ";";
		}
		return style + (additionalStyle == null ? "" : additionalStyle);
	}

	private void registerSopFilterAutoRender() {
		if (searchsop == null) {
			return;
		}
		EventListener listener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					if (paging != null) {
						paging.setActivePage(0);
					}
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				showDiagramTab();
				onSearchDefault(null);
			}
		};
		searchsop.addEventListener("onSelect", listener);
		searchsop.addEventListener("onChange", listener);
	}

	private void showDiagramTab() {
		try {
			if (tabDiagramAlurSop != null) {
				tabDiagramAlurSop.setSelected(true);
			}
			if (diagramScrollContainer != null) {
				diagramScrollContainer.setHeight("2540px");
				diagramScrollContainer.setStyle(buildDiagramScrollStyleAlurSop());
			}
			if (diagramSopContainer != null) {
				diagramSopContainer.setHeight("auto");
				diagramSopContainer.setStyle("width:100%;min-height:1520px;height:auto;overflow:visible;"
						+ "background:transparent;box-sizing:border-box;");
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}


	/**
	 * AktorLookup bersama untuk satu kali render grid Alur SOP. Dibuat sekali per
	 * pencarian lalu dipakai semua baris, sehingga daftar user aktif &amp; resolusi
	 * role tidak dihitung ulang per baris (mempercepat loading data Alur SOP).
	 */
	private SopUtil.AktorLookup aktorLookupRender;

	class AlurSopRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final AlurSop alurSop = (AlurSop) arg1;

			MyDetail detail = new MyDetail();
			detail.setOpen(true);
			detail.setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(detail);
			vbox.setWidth("100%");

			if (!alurSop.getDokumenAlurSops().isEmpty()) {

				vbox.appendChild(new MyLabelBoldAja("Dokumen :"));

				for (DokumenAlurSop dokumenAlurSop : alurSop.getDokumenAlurSops()) {
					if (dokumenAlurSop.getAktif()) {

						vbox.appendChild(new MyLabelKecil(dokumenAlurSop.getKode() + " " + dokumenAlurSop.getNama()
								+ " (" + (dokumenAlurSop.getWajib() ? "Wajib" : "Tidak Wajib") + ")"));

					}
				}

			}

			if (!alurSop.getKelompokParameterTambahanAlurSops().isEmpty()) {

				vbox.appendChild(new MyLabelBoldAja("Parameter :"));

				for (KelompokParameterTambahanAlurSop kelompokParameterTambahanAlurSop : alurSop
						.getKelompokParameterTambahanAlurSops()) {
					vbox.appendChild(new MyLabelKecil(kelompokParameterTambahanAlurSop.getNama()));
				}

			}

			if (!alurSop.getStart()) {
				vbox.appendChild(new MyLabelBoldAja("Disposisi oleh :"));
				Hbox hbox = new Hbox();
				vbox.appendChild(hbox);

				SopUtil.tampilAktor(null, alurSop.getKhususUsername(),
						alurSop.getAktorSop() != null ? alurSop.getAktorSop().getJenisPengguna() : "", null, alurSop,
						hbox, aktorLookupRender);

			} else {
				vbox.appendChild(new MyLabelBoldAja("Diajukan oleh :"));
				Hbox hbox = new Hbox();
				vbox.appendChild(hbox);
				SopUtil.tampilAktor(null, alurSop.getKhususUsername(),
						alurSop.getAktorSop() != null ? alurSop.getAktorSop().getJenisPengguna() : "", null, alurSop,
						hbox, aktorLookupRender);
			}
			vbox.appendChild(new Html("<hr>"));

			new Label(alurSop.getNomor() == null ? "" : alurSop.getNomor() + "").setParent(arg0);
			new MyLabelAgakKecil(alurSop.getSop().getNama()).setParent(arg0);
			new Label(alurSop.getAktor()).setParent(arg0);
			new MyLabelAgakKecil(alurSop.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(AlurSop.class, alurSop, alurSop.getNama()).setParent(arg0);

			String setelah = (alurSop.getSetelahnya() == null ? ""
					: alurSop.getSetelahnya().getKode() + (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
							: " " + (!alurSop.getOpsiSetelahnya().isEmpty() ? alurSop.getOpsiSetelahnya()
									: alurSop.getSetelahnya().getOpsi())));

			setelah += setelah.isEmpty()
					? (alurSop.getSetelahnya2() == null ? ""
							: alurSop.getSetelahnya2().getKode() + (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
									: " " + (!alurSop.getOpsiSetelahnya2().isEmpty() ? alurSop.getOpsiSetelahnya2()
											: alurSop.getSetelahnya2().getOpsi())))
					: (alurSop.getSetelahnya2() == null ? ""
							: ", " + alurSop.getSetelahnya2().getKode()
									+ (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
											: " " + (!alurSop.getOpsiSetelahnya2().isEmpty()
													? alurSop.getOpsiSetelahnya2()
													: alurSop.getSetelahnya2().getOpsi())));

			setelah += setelah.isEmpty()
					? (alurSop.getSetelahnya3() == null ? ""
							: alurSop.getSetelahnya3().getKode() + (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
									: " " + (!alurSop.getOpsiSetelahnya3().isEmpty() ? alurSop.getOpsiSetelahnya3()
											: alurSop.getSetelahnya3().getOpsi())))
					: (alurSop.getSetelahnya3() == null ? ""
							: ", " + alurSop.getSetelahnya3().getKode()
									+ (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
											: " " + (!alurSop.getOpsiSetelahnya3().isEmpty()
													? alurSop.getOpsiSetelahnya3()
													: alurSop.getSetelahnya3().getOpsi())));

			setelah += setelah.isEmpty()
					? (alurSop.getSetelahnya4() == null ? ""
							: alurSop.getSetelahnya4().getKode() + (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
									: " " + (!alurSop.getOpsiSetelahnya4().isEmpty() ? alurSop.getOpsiSetelahnya4()
											: alurSop.getSetelahnya4().getOpsi())))
					: (alurSop.getSetelahnya4() == null ? ""
							: ", " + alurSop.getSetelahnya4().getKode()
									+ (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
											: " " + (!alurSop.getOpsiSetelahnya4().isEmpty()
													? alurSop.getOpsiSetelahnya4()
													: alurSop.getSetelahnya4().getOpsi())));

			setelah += setelah.isEmpty()
					? (alurSop.getSetelahnya5() == null ? ""
							: alurSop.getSetelahnya5().getKode() + (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
									: " " + (!alurSop.getOpsiSetelahnya5().isEmpty() ? alurSop.getOpsiSetelahnya5()
											: alurSop.getSetelahnya5().getOpsi())))
					: (alurSop.getSetelahnya5() == null ? ""
							: ", " + alurSop.getSetelahnya5().getKode()
									+ (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
											: " " + (!alurSop.getOpsiSetelahnya5().isEmpty()
													? alurSop.getOpsiSetelahnya5()
													: alurSop.getSetelahnya5().getOpsi())));

			setelah += setelah.isEmpty()
					? (alurSop.getSetelahnya6() == null ? ""
							: alurSop.getSetelahnya6().getKode() + (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
									: " " + (!alurSop.getOpsiSetelahnya6().isEmpty() ? alurSop.getOpsiSetelahnya6()
											: alurSop.getSetelahnya6().getOpsi())))
					: (alurSop.getSetelahnya6() == null ? ""
							: ", " + alurSop.getSetelahnya6().getKode()
									+ (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
											: " " + (!alurSop.getOpsiSetelahnya6().isEmpty()
													? alurSop.getOpsiSetelahnya6()
													: alurSop.getSetelahnya6().getOpsi())));

			setelah += setelah.isEmpty()
					? (alurSop.getSetelahnya7() == null ? ""
							: alurSop.getSetelahnya7().getKode() + (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
									: " " + (!alurSop.getOpsiSetelahnya7().isEmpty() ? alurSop.getOpsiSetelahnya7()
											: alurSop.getSetelahnya7().getOpsi())))
					: (alurSop.getSetelahnya7() == null ? ""
							: ", " + alurSop.getSetelahnya7().getKode()
									+ (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
											: " " + (!alurSop.getOpsiSetelahnya7().isEmpty()
													? alurSop.getOpsiSetelahnya7()
													: alurSop.getSetelahnya7().getOpsi())));

			setelah += setelah.isEmpty()
					? (alurSop.getSetelahnya8() == null ? ""
							: alurSop.getSetelahnya8().getKode() + (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
									: " " + (!alurSop.getOpsiSetelahnya8().isEmpty() ? alurSop.getOpsiSetelahnya8()
											: alurSop.getSetelahnya8().getOpsi())))
					: (alurSop.getSetelahnya8() == null ? ""
							: ", " + alurSop.getSetelahnya8().getKode()
									+ (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
											: " " + (!alurSop.getOpsiSetelahnya8().isEmpty()
													? alurSop.getOpsiSetelahnya8()
													: alurSop.getSetelahnya8().getOpsi())));

			setelah += setelah.isEmpty()
					? (alurSop.getSetelahnya9() == null ? ""
							: alurSop.getSetelahnya9().getKode() + (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
									: " " + (!alurSop.getOpsiSetelahnya9().isEmpty() ? alurSop.getOpsiSetelahnya9()
											: alurSop.getSetelahnya9().getOpsi())))
					: (alurSop.getSetelahnya9() == null ? ""
							: ", " + alurSop.getSetelahnya9().getKode()
									+ (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
											: " " + (!alurSop.getOpsiSetelahnya9().isEmpty()
													? alurSop.getOpsiSetelahnya9()
													: alurSop.getSetelahnya9().getOpsi())));

			setelah += setelah.isEmpty()
					? (alurSop.getSetelahnya10() == null ? ""
							: alurSop.getSetelahnya10().getKode() + (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
									: " " + (!alurSop.getOpsiSetelahnya10().isEmpty() ? alurSop.getOpsiSetelahnya10()
											: alurSop.getSetelahnya10().getOpsi())))
					: (alurSop.getSetelahnya10() == null ? ""
							: ", " + alurSop.getSetelahnya10().getKode()
									+ (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
											: " " + (!alurSop.getOpsiSetelahnya10().isEmpty()
													? alurSop.getOpsiSetelahnya10()
													: alurSop.getSetelahnya10().getOpsi())));

			setelah += setelah.isEmpty()
					? (alurSop.getSetelahnya11() == null ? ""
							: alurSop.getSetelahnya11().getKode() + (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
									: " " + (!alurSop.getOpsiSetelahnya11().isEmpty() ? alurSop.getOpsiSetelahnya11()
											: alurSop.getSetelahnya11().getOpsi())))
					: (alurSop.getSetelahnya11() == null ? ""
							: ", " + alurSop.getSetelahnya11().getKode()
									+ (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
											: " " + (!alurSop.getOpsiSetelahnya11().isEmpty()
													? alurSop.getOpsiSetelahnya11()
													: alurSop.getSetelahnya11().getOpsi())));

			setelah += setelah.isEmpty()
					? (alurSop.getSetelahnya12() == null ? ""
							: alurSop.getSetelahnya12().getKode() + (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
									: " " + (!alurSop.getOpsiSetelahnya12().isEmpty() ? alurSop.getOpsiSetelahnya12()
											: alurSop.getSetelahnya12().getOpsi())))
					: (alurSop.getSetelahnya12() == null ? ""
							: ", " + alurSop.getSetelahnya12().getKode()
									+ (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
											: " " + (!alurSop.getOpsiSetelahnya12().isEmpty()
													? alurSop.getOpsiSetelahnya12()
													: alurSop.getSetelahnya12().getOpsi())));

			setelah += setelah.isEmpty()
					? (alurSop.getSetelahnya13() == null ? ""
							: alurSop.getSetelahnya13().getKode() + (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
									: " " + (!alurSop.getOpsiSetelahnya13().isEmpty() ? alurSop.getOpsiSetelahnya13()
											: alurSop.getSetelahnya13().getOpsi())))
					: (alurSop.getSetelahnya13() == null ? ""
							: ", " + alurSop.getSetelahnya13().getKode()
									+ (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
											: " " + (!alurSop.getOpsiSetelahnya13().isEmpty()
													? alurSop.getOpsiSetelahnya13()
													: alurSop.getSetelahnya13().getOpsi())));

			setelah += setelah.isEmpty()
					? (alurSop.getSetelahnya14() == null ? ""
							: alurSop.getSetelahnya14().getKode() + (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
									: " " + (!alurSop.getOpsiSetelahnya14().isEmpty() ? alurSop.getOpsiSetelahnya14()
											: alurSop.getSetelahnya14().getOpsi())))
					: (alurSop.getSetelahnya14() == null ? ""
							: ", " + alurSop.getSetelahnya14().getKode()
									+ (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
											: " " + (!alurSop.getOpsiSetelahnya14().isEmpty()
													? alurSop.getOpsiSetelahnya14()
													: alurSop.getSetelahnya14().getOpsi())));

			setelah += setelah.isEmpty()
					? (alurSop.getSetelahnya15() == null ? ""
							: alurSop.getSetelahnya15().getKode() + (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
									: " " + (!alurSop.getOpsiSetelahnya15().isEmpty() ? alurSop.getOpsiSetelahnya15()
											: alurSop.getSetelahnya15().getOpsi())))
					: (alurSop.getSetelahnya15() == null ? ""
							: ", " + alurSop.getSetelahnya15().getKode()
									+ (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
											: " " + (!alurSop.getOpsiSetelahnya15().isEmpty()
													? alurSop.getOpsiSetelahnya15()
													: alurSop.getSetelahnya15().getOpsi())));

			setelah += setelah.isEmpty()
					? (alurSop.getSetelahnya16() == null ? ""
							: alurSop.getSetelahnya16().getKode() + (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
									: " " + (!alurSop.getOpsiSetelahnya16().isEmpty() ? alurSop.getOpsiSetelahnya16()
											: alurSop.getSetelahnya16().getOpsi())))
					: (alurSop.getSetelahnya16() == null ? ""
							: ", " + alurSop.getSetelahnya16().getKode()
									+ (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
											: " " + (!alurSop.getOpsiSetelahnya16().isEmpty()
													? alurSop.getOpsiSetelahnya16()
													: alurSop.getSetelahnya16().getOpsi())));

			setelah += setelah.isEmpty()
					? (alurSop.getSetelahnya17() == null ? ""
							: alurSop.getSetelahnya17().getKode() + (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
									: " " + (!alurSop.getOpsiSetelahnya17().isEmpty() ? alurSop.getOpsiSetelahnya17()
											: alurSop.getSetelahnya17().getOpsi())))
					: (alurSop.getSetelahnya17() == null ? ""
							: ", " + alurSop.getSetelahnya17().getKode()
									+ (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
											: " " + (!alurSop.getOpsiSetelahnya17().isEmpty()
													? alurSop.getOpsiSetelahnya17()
													: alurSop.getSetelahnya17().getOpsi())));

			setelah += setelah.isEmpty()
					? (alurSop.getSetelahnya18() == null ? ""
							: alurSop.getSetelahnya18().getKode() + (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
									: " " + (!alurSop.getOpsiSetelahnya18().isEmpty() ? alurSop.getOpsiSetelahnya18()
											: alurSop.getSetelahnya18().getOpsi())))
					: (alurSop.getSetelahnya18() == null ? ""
							: ", " + alurSop.getSetelahnya18().getKode()
									+ (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
											: " " + (!alurSop.getOpsiSetelahnya18().isEmpty()
													? alurSop.getOpsiSetelahnya18()
													: alurSop.getSetelahnya18().getOpsi())));

			setelah += setelah.isEmpty()
					? (alurSop.getSetelahnya19() == null ? ""
							: alurSop.getSetelahnya19().getKode() + (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
									: " " + (!alurSop.getOpsiSetelahnya19().isEmpty() ? alurSop.getOpsiSetelahnya19()
											: alurSop.getSetelahnya19().getOpsi())))
					: (alurSop.getSetelahnya19() == null ? ""
							: ", " + alurSop.getSetelahnya19().getKode()
									+ (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
											: " " + (!alurSop.getOpsiSetelahnya19().isEmpty()
													? alurSop.getOpsiSetelahnya19()
													: alurSop.getSetelahnya19().getOpsi())));

			setelah += setelah.isEmpty()
					? (alurSop.getSetelahnya20() == null ? ""
							: alurSop.getSetelahnya20().getKode() + (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
									: " " + (!alurSop.getOpsiSetelahnya20().isEmpty() ? alurSop.getOpsiSetelahnya20()
											: alurSop.getSetelahnya20().getOpsi())))
					: (alurSop.getSetelahnya20() == null ? ""
							: ", " + alurSop.getSetelahnya20().getKode()
									+ (!alurSop.getAlurSetelahnyaBerupaPilihan() ? ""
											: " " + (!alurSop.getOpsiSetelahnya20().isEmpty()
													? alurSop.getOpsiSetelahnya20()
													: alurSop.getSetelahnya20().getOpsi())));

			new MyLabelAgakKecil(setelah).setParent(arg0);

			new Label(alurSop.getJangkaWaktu() + " hr").setParent(arg0);

			new Label(alurSop.getLabelFormInputan()).setParent(arg0);

			new Label(alurSop.getBekukanFormTampilan() ? "Tidak" : "Ya").setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(alurSop.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					alurSop.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(alurSop);
				}
			});

			new MyLabelAgakKecil(alurSop.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, alurSop, AlurSopAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {

		if ((searchsop.getSelectedItem() == null || searchsop.getSelectedItem().getValue() == null)) {
			MyMessageboxConfig.show("Mohon maaf, SOP belum dipilih. Langkah yang dapat dilakukan: (1) pilih SOP terlebih dahulu pada filter pencarian di atas; (2) pastikan SOP yang dipilih sudah benar; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}

		init(new AlurSop());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {

		if (obj.getId() == null
				&& (searchsop.getSelectedItem() == null || searchsop.getSelectedItem().getValue() == null)) {
			MyMessageboxConfig.show("Mohon maaf, SOP belum dipilih. Langkah yang dapat dilakukan: (1) pilih SOP terlebih dahulu pada filter pencarian di atas; (2) pastikan SOP yang dipilih sudah benar; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}

		alurSop = (AlurSop) obj;
		init(alurSop);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings({ "rawtypes", "deprecation" })
	private void init(AlurSop alurSop) throws Exception {

		if (alurSop.getSop() == null) {
			alurSop.setSop((Sop) searchsop.getSelectedItem().getValue());
		} else {
			Common.selectComboItem(true, searchsop, alurSop.getSop());
		}
		this.alurSop = alurSop;
		addWindow.setTitle(alurSop.getId() == null ? "Tambah Alur SOP" : "Ubah Alur SOP");
		addWindow.setHeight("99%");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("SOP *"));
		row.appendChild(new Label(alurSop.getSop().getKode() + " - " + alurSop.getSop().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aktor *"));
		row.appendChild(aktorSop = new Combobox());
		aktorSop.setWidth("90%");
		Common.insertComboDanSemua(aktorSop, new String[] { "kode", "nama" }, "keterangan", AktorSop.class,
				"=Aktor Kustom=", Restrictions.eq("aktif", true));
		Common.selectComboItem(true, aktorSop, alurSop.getAktorSop());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(kembaliKePengaju = new MyCheckboxConfig("Kembali ke pengaju"));
		kembaliKePengaju.setChecked(alurSop.getKembaliKePengaju());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(kembaliKeAktorSebelumnya = new MyCheckboxConfig("Kembali ke aktor sebelumnya"));
		kembaliKeAktorSebelumnya.setChecked(alurSop.getKembaliKeAktorSebelumnya());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Aktor (Kustom) *"));
		row.appendChild(aktor = new Textbox(alurSop.getAktor()));
		aktor.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Alur SOP *"));
		row.appendChild(kode = new Textbox(alurSop.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alur SOP *"));
		row.appendChild(nama = new Textbox(alurSop.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(start = new MyCheckboxConfig("Merupakan alur SOP paling awal (start)"));
		start.setChecked(alurSop.getStart());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(bolehDiisiCatatan = new MyCheckboxConfig("Ada catatan disposisi"));
		bolehDiisiCatatan.setChecked(alurSop.getBolehDiisiCatatan());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(catatanWajibDiisi = new MyCheckboxConfig("Catatan disposisi wajib diisi"));
		catatanWajibDiisi.setChecked(alurSop.getCatatanWajibDiisi());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(lampiranCatatanWajibDiisi = new MyCheckboxConfig("Lampiran catatan disposisi wajib diisi"));
		lampiranCatatanWajibDiisi.setChecked(alurSop.getLampiranCatatanWajibDiisi());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(tanggalDisposisiBolehDiubah = new MyCheckboxConfig("Tanggal Disposisi Boleh Diubah"));
		tanggalDisposisiBolehDiubah.setChecked(alurSop.getTanggalDisposisiBolehDiubah());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(
				jikaProsesDisetujuiMakaSelesai = new MyCheckboxConfig("Jika Status Disetujui Maka Proses Selesai"));
		jikaProsesDisetujuiMakaSelesai.setChecked(alurSop.getJikaProsesDisetujuiMakaSelesai());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(persetujuanAdaDiSini = new MyCheckboxConfig(
				"Persetujuan jika telah di proses tanpa disposisi selanjutnya"));
		persetujuanAdaDiSini.setChecked(alurSop.getPersetujuanAdaDiSini());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(penolakanAdaDiSini = new MyCheckboxConfig("Penolakan jika sampai pada disposisi ini"));
		penolakanAdaDiSini.setChecked(alurSop.getPenolakanAdaDiSini());

		MyFormRow rowsebelumnya = new MyFormRow();
		rowsebelumnya.setParent(rows);
		rowsebelumnya.appendChild(new ais.ui.util.MyLabelConfig("Alur sebelumnya"));
		rowsebelumnya.appendChild(sebelumnya = new Combobox());
		Common.insertComboDanSemua(sebelumnya, new String[] { "kode", "aktor", "nama", "role" }, "keterangan",
				AlurSop.class,
				Restrictions.and(Restrictions.eq("sop", alurSop.getSop()),
						alurSop.getId() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.ne("id", alurSop.getId())));
		sebelumnya.setWidth("90%");
		sebelumnya.setReadonly(true);
		Common.selectComboItem(true, sebelumnya, alurSop.getSebelumnya());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(alurSetelahnyaOtomatis = new MyCheckboxConfig("Alur setelahnya dibuat otomatis"));
		alurSetelahnyaOtomatis.setChecked(alurSop.getAlurSetelahnyaOtomatis());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(alurSetelahnyaTidakWajib = new MyCheckboxConfig("Alur Setelahnya Tidak Wajib Dipilih"));
		alurSetelahnyaTidakWajib.setChecked(alurSop.getAlurSetelahnyaTidakWajib());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(
				alurSetelahnyaBerupaPilihan = new MyCheckboxConfig("Alur Setelahnya Berupa Pilihan Salah Satu"));
		alurSetelahnyaBerupaPilihan.setChecked(alurSop.getAlurSetelahnyaBerupaPilihan());

		final MyFormRow rowsetelahnya = new MyFormRow();
		rowsetelahnya.setParent(rows);
		rowsetelahnya.appendChild(new ais.ui.util.MyLabelConfig("Alur Setelahnya I"));
		rowsetelahnya.appendChild(setelahnya = new Combobox());
		Common.insertComboDanSemua(setelahnya, new String[] { "kode", "aktor", "nama", "role" }, "keterangan",
				AlurSop.class,
				Restrictions.and(Restrictions.eq("start", false),
						Restrictions.and(Restrictions.eq("sop", alurSop.getSop()),
								alurSop.getId() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.ne("id", alurSop.getId()))));
		setelahnya.setWidth("90%");
		setelahnya.setReadonly(true);
		Common.selectComboItem(true, setelahnya, alurSop.getSetelahnya());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Opsi I"));
		row.appendChild(opsiSetelahnya = new Textbox(alurSop.getOpsiSetelahnya()));
		opsiSetelahnya.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(persetujuanAdaDiSini1 = new MyCheckboxConfig("Persetujuan jika di pilih opsi ke I"));
		persetujuanAdaDiSini1.setChecked(alurSop.getPersetujuanAdaDiSini1());

		final MyFormRow rowsetelahnya2 = new MyFormRow();
		rowsetelahnya2.setParent(rows);
		rowsetelahnya2.appendChild(new ais.ui.util.MyLabelConfig("Alur Setelahnya II"));
		rowsetelahnya2.appendChild(setelahnya2 = new Combobox());
		Common.insertComboDanSemua(setelahnya2, new String[] { "kode", "aktor", "nama", "role" }, "keterangan",
				AlurSop.class,
				Restrictions.and(Restrictions.eq("start", false),
						Restrictions.and(Restrictions.eq("sop", alurSop.getSop()),
								alurSop.getId() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.ne("id", alurSop.getId()))));
		setelahnya2.setWidth("90%");
		setelahnya2.setReadonly(true);
		Common.selectComboItem(true, setelahnya2, alurSop.getSetelahnya2());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Opsi II"));
		row.appendChild(opsiSetelahnya2 = new Textbox(alurSop.getOpsiSetelahnya2()));
		opsiSetelahnya2.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(persetujuanAdaDiSini2 = new MyCheckboxConfig("Persetujuan jika di pilih opsi ke II"));
		persetujuanAdaDiSini2.setChecked(alurSop.getPersetujuanAdaDiSini2());

		final MyFormRow rowsetelahnya3 = new MyFormRow();
		rowsetelahnya3.setParent(rows);
		rowsetelahnya3.appendChild(new ais.ui.util.MyLabelConfig("Alur Setelahnya III"));
		rowsetelahnya3.appendChild(setelahnya3 = new Combobox());
		Common.insertComboDanSemua(setelahnya3, new String[] { "kode", "aktor", "nama", "role" }, "keterangan",
				AlurSop.class,
				Restrictions.and(Restrictions.eq("start", false),
						Restrictions.and(Restrictions.eq("sop", alurSop.getSop()),
								alurSop.getId() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.ne("id", alurSop.getId()))));
		setelahnya3.setWidth("90%");
		setelahnya3.setReadonly(true);
		Common.selectComboItem(true, setelahnya3, alurSop.getSetelahnya3());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Opsi III"));
		row.appendChild(opsiSetelahnya3 = new Textbox(alurSop.getOpsiSetelahnya3()));
		opsiSetelahnya3.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(persetujuanAdaDiSini3 = new MyCheckboxConfig("Persetujuan jika di pilih opsi ke III"));
		persetujuanAdaDiSini3.setChecked(alurSop.getPersetujuanAdaDiSini3());

		final MyFormRow rowsetelahnya4 = new MyFormRow();
		rowsetelahnya4.setParent(rows);
		rowsetelahnya4.appendChild(new ais.ui.util.MyLabelConfig("Alur Setelahnya IV"));
		rowsetelahnya4.appendChild(setelahnya4 = new Combobox());
		Common.insertComboDanSemua(setelahnya4, new String[] { "kode", "aktor", "nama", "role" }, "keterangan",
				AlurSop.class,
				Restrictions.and(Restrictions.eq("start", false),
						Restrictions.and(Restrictions.eq("sop", alurSop.getSop()),
								alurSop.getId() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.ne("id", alurSop.getId()))));
		setelahnya4.setWidth("90%");
		setelahnya4.setReadonly(true);
		Common.selectComboItem(true, setelahnya4, alurSop.getSetelahnya4());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Opsi IV"));
		row.appendChild(opsiSetelahnya4 = new Textbox(alurSop.getOpsiSetelahnya4()));
		opsiSetelahnya4.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(persetujuanAdaDiSini4 = new MyCheckboxConfig("Persetujuan jika di pilih opsi ke IV"));
		persetujuanAdaDiSini4.setChecked(alurSop.getPersetujuanAdaDiSini4());

		final MyFormRow rowsetelahnya5 = new MyFormRow();
		rowsetelahnya5.setParent(rows);
		rowsetelahnya5.appendChild(new ais.ui.util.MyLabelConfig("Alur Setelahnya V"));
		rowsetelahnya5.appendChild(setelahnya5 = new Combobox());
		Common.insertComboDanSemua(setelahnya5, new String[] { "kode", "aktor", "nama", "role" }, "keterangan",
				AlurSop.class,
				Restrictions.and(Restrictions.eq("start", false),
						Restrictions.and(Restrictions.eq("sop", alurSop.getSop()),
								alurSop.getId() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.ne("id", alurSop.getId()))));
		setelahnya5.setWidth("90%");
		setelahnya5.setReadonly(true);
		Common.selectComboItem(true, setelahnya5, alurSop.getSetelahnya5());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Opsi V"));
		row.appendChild(opsiSetelahnya5 = new Textbox(alurSop.getOpsiSetelahnya5()));
		opsiSetelahnya5.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(persetujuanAdaDiSini5 = new MyCheckboxConfig("Persetujuan jika di pilih opsi ke V"));
		persetujuanAdaDiSini5.setChecked(alurSop.getPersetujuanAdaDiSini5());

		final MyFormRow rowsetelahnya6 = new MyFormRow();
		rowsetelahnya6.setParent(rows);
		rowsetelahnya6.appendChild(new ais.ui.util.MyLabelConfig("Alur Setelahnya VI"));
		rowsetelahnya6.appendChild(setelahnya6 = new Combobox());
		Common.insertComboDanSemua(setelahnya6, new String[] { "kode", "aktor", "nama", "role" }, "keterangan",
				AlurSop.class,
				Restrictions.and(Restrictions.eq("start", false),
						Restrictions.and(Restrictions.eq("sop", alurSop.getSop()),
								alurSop.getId() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.ne("id", alurSop.getId()))));
		setelahnya6.setWidth("90%");
		setelahnya6.setReadonly(true);
		Common.selectComboItem(true, setelahnya6, alurSop.getSetelahnya6());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Opsi VI"));
		row.appendChild(opsiSetelahnya6 = new Textbox(alurSop.getOpsiSetelahnya6()));
		opsiSetelahnya6.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(persetujuanAdaDiSini6 = new MyCheckboxConfig("Persetujuan jika di pilih opsi ke VI"));
		persetujuanAdaDiSini6.setChecked(alurSop.getPersetujuanAdaDiSini6());

		final MyFormRow rowsetelahnya7 = new MyFormRow();
		rowsetelahnya7.setParent(rows);
		rowsetelahnya7.appendChild(new ais.ui.util.MyLabelConfig("Alur Setelahnya VII"));
		rowsetelahnya7.appendChild(setelahnya7 = new Combobox());
		Common.insertComboDanSemua(setelahnya7, new String[] { "kode", "aktor", "nama", "role" }, "keterangan",
				AlurSop.class,
				Restrictions.and(Restrictions.eq("start", false),
						Restrictions.and(Restrictions.eq("sop", alurSop.getSop()),
								alurSop.getId() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.ne("id", alurSop.getId()))));
		setelahnya7.setWidth("90%");
		setelahnya7.setReadonly(true);
		Common.selectComboItem(true, setelahnya7, alurSop.getSetelahnya7());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Opsi VII"));
		row.appendChild(opsiSetelahnya7 = new Textbox(alurSop.getOpsiSetelahnya7()));
		opsiSetelahnya7.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(persetujuanAdaDiSini7 = new MyCheckboxConfig("Persetujuan jika di pilih opsi ke VII"));
		persetujuanAdaDiSini7.setChecked(alurSop.getPersetujuanAdaDiSini7());

		final MyFormRow rowsetelahnya8 = new MyFormRow();
		rowsetelahnya8.setParent(rows);
		rowsetelahnya8.appendChild(new ais.ui.util.MyLabelConfig("Alur Setelahnya VIII"));
		rowsetelahnya8.appendChild(setelahnya8 = new Combobox());
		Common.insertComboDanSemua(setelahnya8, new String[] { "kode", "aktor", "nama", "role" }, "keterangan",
				AlurSop.class,
				Restrictions.and(Restrictions.eq("start", false),
						Restrictions.and(Restrictions.eq("sop", alurSop.getSop()),
								alurSop.getId() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.ne("id", alurSop.getId()))));
		setelahnya8.setWidth("90%");
		setelahnya8.setReadonly(true);
		Common.selectComboItem(true, setelahnya8, alurSop.getSetelahnya8());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Opsi VIII"));
		row.appendChild(opsiSetelahnya8 = new Textbox(alurSop.getOpsiSetelahnya8()));
		opsiSetelahnya8.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(persetujuanAdaDiSini8 = new MyCheckboxConfig("Persetujuan jika di pilih opsi ke VIII"));
		persetujuanAdaDiSini8.setChecked(alurSop.getPersetujuanAdaDiSini8());

		final MyFormRow rowsetelahnya9 = new MyFormRow();
		rowsetelahnya9.setParent(rows);
		rowsetelahnya9.appendChild(new ais.ui.util.MyLabelConfig("Alur Setelahnya IX"));
		rowsetelahnya9.appendChild(setelahnya9 = new Combobox());
		Common.insertComboDanSemua(setelahnya9, new String[] { "kode", "aktor", "nama", "role" }, "keterangan",
				AlurSop.class,
				Restrictions.and(Restrictions.eq("start", false),
						Restrictions.and(Restrictions.eq("sop", alurSop.getSop()),
								alurSop.getId() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.ne("id", alurSop.getId()))));
		setelahnya9.setWidth("90%");
		setelahnya9.setReadonly(true);
		Common.selectComboItem(true, setelahnya9, alurSop.getSetelahnya9());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Opsi IX"));
		row.appendChild(opsiSetelahnya9 = new Textbox(alurSop.getOpsiSetelahnya9()));
		opsiSetelahnya9.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(persetujuanAdaDiSini9 = new MyCheckboxConfig("Persetujuan jika di pilih opsi ke IX"));
		persetujuanAdaDiSini9.setChecked(alurSop.getPersetujuanAdaDiSini9());

		final MyFormRow rowsetelahnya10 = new MyFormRow();
		rowsetelahnya10.setParent(rows);
		rowsetelahnya10.appendChild(new ais.ui.util.MyLabelConfig("Alur Setelahnya X"));
		rowsetelahnya10.appendChild(setelahnya10 = new Combobox());
		Common.insertComboDanSemua(setelahnya10, new String[] { "kode", "aktor", "nama", "role" }, "keterangan",
				AlurSop.class,
				Restrictions.and(Restrictions.eq("start", false),
						Restrictions.and(Restrictions.eq("sop", alurSop.getSop()),
								alurSop.getId() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.ne("id", alurSop.getId()))));
		setelahnya10.setWidth("90%");
		setelahnya10.setReadonly(true);
		Common.selectComboItem(true, setelahnya10, alurSop.getSetelahnya10());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Opsi X"));
		row.appendChild(opsiSetelahnya10 = new Textbox(alurSop.getOpsiSetelahnya10()));
		opsiSetelahnya10.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(persetujuanAdaDiSini10 = new MyCheckboxConfig("Persetujuan jika di pilih opsi ke X"));
		persetujuanAdaDiSini10.setChecked(alurSop.getPersetujuanAdaDiSini10());

		final MyFormRow rowsetelahnya11 = new MyFormRow();
		rowsetelahnya11.setParent(rows);
		rowsetelahnya11.appendChild(new ais.ui.util.MyLabelConfig("Alur Setelahnya XI"));
		rowsetelahnya11.appendChild(setelahnya11 = new Combobox());
		Common.insertComboDanSemua(setelahnya11, new String[] { "kode", "aktor", "nama", "role" }, "keterangan",
				AlurSop.class,
				Restrictions.and(Restrictions.eq("start", false),
						Restrictions.and(Restrictions.eq("sop", alurSop.getSop()),
								alurSop.getId() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.ne("id", alurSop.getId()))));
		setelahnya11.setWidth("90%");
		setelahnya11.setReadonly(true);
		Common.selectComboItem(true, setelahnya11, alurSop.getSetelahnya11());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Opsi XI"));
		row.appendChild(opsiSetelahnya11 = new Textbox(alurSop.getOpsiSetelahnya11()));
		opsiSetelahnya11.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(persetujuanAdaDiSini11 = new MyCheckboxConfig("Persetujuan jika di pilih opsi ke XI"));
		persetujuanAdaDiSini11.setChecked(alurSop.getPersetujuanAdaDiSini11());

		final MyFormRow rowsetelahnya12 = new MyFormRow();
		rowsetelahnya12.setParent(rows);
		rowsetelahnya12.appendChild(new ais.ui.util.MyLabelConfig("Alur Setelahnya XII"));
		rowsetelahnya12.appendChild(setelahnya12 = new Combobox());
		Common.insertComboDanSemua(setelahnya12, new String[] { "kode", "aktor", "nama", "role" }, "keterangan",
				AlurSop.class,
				Restrictions.and(Restrictions.eq("start", false),
						Restrictions.and(Restrictions.eq("sop", alurSop.getSop()),
								alurSop.getId() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.ne("id", alurSop.getId()))));
		setelahnya12.setWidth("90%");
		setelahnya12.setReadonly(true);
		Common.selectComboItem(true, setelahnya12, alurSop.getSetelahnya12());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Opsi XII"));
		row.appendChild(opsiSetelahnya12 = new Textbox(alurSop.getOpsiSetelahnya12()));
		opsiSetelahnya12.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(persetujuanAdaDiSini12 = new MyCheckboxConfig("Persetujuan jika di pilih opsi ke XII"));
		persetujuanAdaDiSini12.setChecked(alurSop.getPersetujuanAdaDiSini12());

		final MyFormRow rowsetelahnya13 = new MyFormRow();
		rowsetelahnya13.setParent(rows);
		rowsetelahnya13.appendChild(new ais.ui.util.MyLabelConfig("Alur Setelahnya XIII"));
		rowsetelahnya13.appendChild(setelahnya13 = new Combobox());
		Common.insertComboDanSemua(setelahnya13, new String[] { "kode", "aktor", "nama", "role" }, "keterangan",
				AlurSop.class,
				Restrictions.and(Restrictions.eq("start", false),
						Restrictions.and(Restrictions.eq("sop", alurSop.getSop()),
								alurSop.getId() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.ne("id", alurSop.getId()))));
		setelahnya13.setWidth("90%");
		setelahnya13.setReadonly(true);
		Common.selectComboItem(true, setelahnya13, alurSop.getSetelahnya13());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Opsi XII"));
		row.appendChild(opsiSetelahnya13 = new Textbox(alurSop.getOpsiSetelahnya13()));
		opsiSetelahnya13.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(persetujuanAdaDiSini13 = new MyCheckboxConfig("Persetujuan jika di pilih opsi ke XII"));
		persetujuanAdaDiSini13.setChecked(alurSop.getPersetujuanAdaDiSini13());

		final MyFormRow rowsetelahnya14 = new MyFormRow();
		rowsetelahnya14.setParent(rows);
		rowsetelahnya14.appendChild(new ais.ui.util.MyLabelConfig("Alur Setelahnya XIV"));
		rowsetelahnya14.appendChild(setelahnya14 = new Combobox());
		Common.insertComboDanSemua(setelahnya14, new String[] { "kode", "aktor", "nama", "role" }, "keterangan",
				AlurSop.class,
				Restrictions.and(Restrictions.eq("start", false),
						Restrictions.and(Restrictions.eq("sop", alurSop.getSop()),
								alurSop.getId() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.ne("id", alurSop.getId()))));
		setelahnya14.setWidth("90%");
		setelahnya14.setReadonly(true);
		Common.selectComboItem(true, setelahnya14, alurSop.getSetelahnya14());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Opsi XIV"));
		row.appendChild(opsiSetelahnya14 = new Textbox(alurSop.getOpsiSetelahnya14()));
		opsiSetelahnya14.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(persetujuanAdaDiSini14 = new MyCheckboxConfig("Persetujuan jika di pilih opsi ke XIV"));
		persetujuanAdaDiSini14.setChecked(alurSop.getPersetujuanAdaDiSini14());

		final MyFormRow rowsetelahnya15 = new MyFormRow();
		rowsetelahnya15.setParent(rows);
		rowsetelahnya15.appendChild(new ais.ui.util.MyLabelConfig("Alur Setelahnya XV"));
		rowsetelahnya15.appendChild(setelahnya15 = new Combobox());
		Common.insertComboDanSemua(setelahnya15, new String[] { "kode", "aktor", "nama", "role" }, "keterangan",
				AlurSop.class,
				Restrictions.and(Restrictions.eq("start", false),
						Restrictions.and(Restrictions.eq("sop", alurSop.getSop()),
								alurSop.getId() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.ne("id", alurSop.getId()))));
		setelahnya15.setWidth("90%");
		setelahnya15.setReadonly(true);
		Common.selectComboItem(true, setelahnya15, alurSop.getSetelahnya15());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Opsi XV"));
		row.appendChild(opsiSetelahnya15 = new Textbox(alurSop.getOpsiSetelahnya15()));
		opsiSetelahnya15.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(persetujuanAdaDiSini15 = new MyCheckboxConfig("Persetujuan jika di pilih opsi ke XV"));
		persetujuanAdaDiSini15.setChecked(alurSop.getPersetujuanAdaDiSini15());

		final MyFormRow rowsetelahnya16 = new MyFormRow();
		rowsetelahnya16.setParent(rows);
		rowsetelahnya16.appendChild(new ais.ui.util.MyLabelConfig("Alur Setelahnya XVI"));
		rowsetelahnya16.appendChild(setelahnya16 = new Combobox());
		Common.insertComboDanSemua(setelahnya16, new String[] { "kode", "aktor", "nama", "role" }, "keterangan",
				AlurSop.class,
				Restrictions.and(Restrictions.eq("start", false),
						Restrictions.and(Restrictions.eq("sop", alurSop.getSop()),
								alurSop.getId() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.ne("id", alurSop.getId()))));
		setelahnya16.setWidth("90%");
		setelahnya16.setReadonly(true);
		Common.selectComboItem(true, setelahnya16, alurSop.getSetelahnya16());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Opsi XVI"));
		row.appendChild(opsiSetelahnya16 = new Textbox(alurSop.getOpsiSetelahnya16()));
		opsiSetelahnya16.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(persetujuanAdaDiSini16 = new MyCheckboxConfig("Persetujuan jika di pilih opsi ke XVI"));
		persetujuanAdaDiSini16.setChecked(alurSop.getPersetujuanAdaDiSini16());

		final MyFormRow rowsetelahnya17 = new MyFormRow();
		rowsetelahnya17.setParent(rows);
		rowsetelahnya17.appendChild(new ais.ui.util.MyLabelConfig("Alur Setelahnya XVII"));
		rowsetelahnya17.appendChild(setelahnya17 = new Combobox());
		Common.insertComboDanSemua(setelahnya17, new String[] { "kode", "aktor", "nama", "role" }, "keterangan",
				AlurSop.class,
				Restrictions.and(Restrictions.eq("start", false),
						Restrictions.and(Restrictions.eq("sop", alurSop.getSop()),
								alurSop.getId() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.ne("id", alurSop.getId()))));
		setelahnya17.setWidth("90%");
		setelahnya17.setReadonly(true);
		Common.selectComboItem(true, setelahnya17, alurSop.getSetelahnya17());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Opsi XVII"));
		row.appendChild(opsiSetelahnya17 = new Textbox(alurSop.getOpsiSetelahnya17()));
		opsiSetelahnya17.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(persetujuanAdaDiSini17 = new MyCheckboxConfig("Persetujuan jika di pilih opsi ke XVII"));
		persetujuanAdaDiSini17.setChecked(alurSop.getPersetujuanAdaDiSini17());

		final MyFormRow rowsetelahnya18 = new MyFormRow();
		rowsetelahnya18.setParent(rows);
		rowsetelahnya18.appendChild(new ais.ui.util.MyLabelConfig("Alur Setelahnya XVIII"));
		rowsetelahnya18.appendChild(setelahnya18 = new Combobox());
		Common.insertComboDanSemua(setelahnya18, new String[] { "kode", "aktor", "nama", "role" }, "keterangan",
				AlurSop.class,
				Restrictions.and(Restrictions.eq("start", false),
						Restrictions.and(Restrictions.eq("sop", alurSop.getSop()),
								alurSop.getId() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.ne("id", alurSop.getId()))));
		setelahnya18.setWidth("90%");
		setelahnya18.setReadonly(true);
		Common.selectComboItem(true, setelahnya18, alurSop.getSetelahnya18());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Opsi XVIII"));
		row.appendChild(opsiSetelahnya18 = new Textbox(alurSop.getOpsiSetelahnya18()));
		opsiSetelahnya18.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(persetujuanAdaDiSini18 = new MyCheckboxConfig("Persetujuan jika di pilih opsi ke XVIII"));
		persetujuanAdaDiSini18.setChecked(alurSop.getPersetujuanAdaDiSini18());

		final MyFormRow rowsetelahnya19 = new MyFormRow();
		rowsetelahnya19.setParent(rows);
		rowsetelahnya19.appendChild(new ais.ui.util.MyLabelConfig("Alur Setelahnya XIX"));
		rowsetelahnya19.appendChild(setelahnya19 = new Combobox());
		Common.insertComboDanSemua(setelahnya19, new String[] { "kode", "aktor", "nama", "role" }, "keterangan",
				AlurSop.class,
				Restrictions.and(Restrictions.eq("start", false),
						Restrictions.and(Restrictions.eq("sop", alurSop.getSop()),
								alurSop.getId() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.ne("id", alurSop.getId()))));
		setelahnya19.setWidth("90%");
		setelahnya19.setReadonly(true);
		Common.selectComboItem(true, setelahnya19, alurSop.getSetelahnya19());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Opsi XIX"));
		row.appendChild(opsiSetelahnya19 = new Textbox(alurSop.getOpsiSetelahnya19()));
		opsiSetelahnya19.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(persetujuanAdaDiSini19 = new MyCheckboxConfig("Persetujuan jika di pilih opsi ke XIX"));
		persetujuanAdaDiSini19.setChecked(alurSop.getPersetujuanAdaDiSini19());

		final MyFormRow rowsetelahnya20 = new MyFormRow();
		rowsetelahnya20.setParent(rows);
		rowsetelahnya20.appendChild(new ais.ui.util.MyLabelConfig("Alur Setelahnya XX"));
		rowsetelahnya20.appendChild(setelahnya20 = new Combobox());
		Common.insertComboDanSemua(setelahnya20, new String[] { "kode", "aktor", "nama", "role" }, "keterangan",
				AlurSop.class,
				Restrictions.and(Restrictions.eq("start", false),
						Restrictions.and(Restrictions.eq("sop", alurSop.getSop()),
								alurSop.getId() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.ne("id", alurSop.getId()))));
		setelahnya20.setWidth("90%");
		setelahnya20.setReadonly(true);
		Common.selectComboItem(true, setelahnya20, alurSop.getSetelahnya20());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Opsi XX"));
		row.appendChild(opsiSetelahnya20 = new Textbox(alurSop.getOpsiSetelahnya20()));
		opsiSetelahnya20.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(persetujuanAdaDiSini20 = new MyCheckboxConfig("Persetujuan jika di pilih opsi ke XX"));
		persetujuanAdaDiSini20.setChecked(alurSop.getPersetujuanAdaDiSini20());

		EventListener s = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				rowsetelahnya.setVisible(!alurSetelahnyaOtomatis.isChecked());
				rowsetelahnya2.setVisible(!alurSetelahnyaOtomatis.isChecked());
				rowsetelahnya3.setVisible(!alurSetelahnyaOtomatis.isChecked());
				rowsetelahnya4.setVisible(!alurSetelahnyaOtomatis.isChecked());
				rowsetelahnya5.setVisible(!alurSetelahnyaOtomatis.isChecked());

				rowsetelahnya6.setVisible(!alurSetelahnyaOtomatis.isChecked());
				rowsetelahnya7.setVisible(!alurSetelahnyaOtomatis.isChecked());
				rowsetelahnya8.setVisible(!alurSetelahnyaOtomatis.isChecked());
				rowsetelahnya9.setVisible(!alurSetelahnyaOtomatis.isChecked());
				rowsetelahnya10.setVisible(!alurSetelahnyaOtomatis.isChecked());

				rowsetelahnya11.setVisible(!alurSetelahnyaOtomatis.isChecked());
				rowsetelahnya12.setVisible(!alurSetelahnyaOtomatis.isChecked());
				rowsetelahnya13.setVisible(!alurSetelahnyaOtomatis.isChecked());
				rowsetelahnya14.setVisible(!alurSetelahnyaOtomatis.isChecked());
				rowsetelahnya15.setVisible(!alurSetelahnyaOtomatis.isChecked());
				rowsetelahnya16.setVisible(!alurSetelahnyaOtomatis.isChecked());
				rowsetelahnya17.setVisible(!alurSetelahnyaOtomatis.isChecked());
				rowsetelahnya18.setVisible(!alurSetelahnyaOtomatis.isChecked());
				rowsetelahnya19.setVisible(!alurSetelahnyaOtomatis.isChecked());
				rowsetelahnya20.setVisible(!alurSetelahnyaOtomatis.isChecked());

				opsiSetelahnya.getParent().setVisible(setelahnya.getSelectedItem() != null
						&& setelahnya.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				opsiSetelahnya2.getParent().setVisible(setelahnya2.getSelectedItem() != null
						&& setelahnya2.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				opsiSetelahnya3.getParent().setVisible(setelahnya3.getSelectedItem() != null
						&& setelahnya3.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				opsiSetelahnya4.getParent().setVisible(setelahnya4.getSelectedItem() != null
						&& setelahnya4.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				opsiSetelahnya5.getParent().setVisible(setelahnya5.getSelectedItem() != null
						&& setelahnya5.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				opsiSetelahnya6.getParent().setVisible(setelahnya6.getSelectedItem() != null
						&& setelahnya6.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				opsiSetelahnya7.getParent().setVisible(setelahnya7.getSelectedItem() != null
						&& setelahnya7.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				opsiSetelahnya8.getParent().setVisible(setelahnya8.getSelectedItem() != null
						&& setelahnya8.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				opsiSetelahnya9.getParent().setVisible(setelahnya9.getSelectedItem() != null
						&& setelahnya9.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				opsiSetelahnya10.getParent().setVisible(setelahnya10.getSelectedItem() != null
						&& setelahnya10.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());

				opsiSetelahnya11.getParent().setVisible(setelahnya11.getSelectedItem() != null
						&& setelahnya11.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				opsiSetelahnya12.getParent().setVisible(setelahnya12.getSelectedItem() != null
						&& setelahnya12.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				opsiSetelahnya13.getParent().setVisible(setelahnya13.getSelectedItem() != null
						&& setelahnya13.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				opsiSetelahnya14.getParent().setVisible(setelahnya14.getSelectedItem() != null
						&& setelahnya14.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				opsiSetelahnya15.getParent().setVisible(setelahnya15.getSelectedItem() != null
						&& setelahnya15.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				opsiSetelahnya16.getParent().setVisible(setelahnya16.getSelectedItem() != null
						&& setelahnya16.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				opsiSetelahnya17.getParent().setVisible(setelahnya17.getSelectedItem() != null
						&& setelahnya17.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				opsiSetelahnya18.getParent().setVisible(setelahnya18.getSelectedItem() != null
						&& setelahnya18.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				opsiSetelahnya19.getParent().setVisible(setelahnya19.getSelectedItem() != null
						&& setelahnya19.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				opsiSetelahnya20.getParent().setVisible(setelahnya20.getSelectedItem() != null
						&& setelahnya20.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());

				persetujuanAdaDiSini1.getParent().setVisible(setelahnya.getSelectedItem() != null
						&& setelahnya.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				persetujuanAdaDiSini2.getParent().setVisible(setelahnya2.getSelectedItem() != null
						&& setelahnya2.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				persetujuanAdaDiSini3.getParent().setVisible(setelahnya3.getSelectedItem() != null
						&& setelahnya3.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				persetujuanAdaDiSini4.getParent().setVisible(setelahnya4.getSelectedItem() != null
						&& setelahnya4.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				persetujuanAdaDiSini5.getParent().setVisible(setelahnya5.getSelectedItem() != null
						&& setelahnya5.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				persetujuanAdaDiSini6.getParent().setVisible(setelahnya6.getSelectedItem() != null
						&& setelahnya6.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				persetujuanAdaDiSini7.getParent().setVisible(setelahnya7.getSelectedItem() != null
						&& setelahnya7.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				persetujuanAdaDiSini8.getParent().setVisible(setelahnya8.getSelectedItem() != null
						&& setelahnya8.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				persetujuanAdaDiSini9.getParent().setVisible(setelahnya9.getSelectedItem() != null
						&& setelahnya9.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				persetujuanAdaDiSini10.getParent().setVisible(setelahnya10.getSelectedItem() != null
						&& setelahnya10.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());

				persetujuanAdaDiSini11.getParent().setVisible(setelahnya11.getSelectedItem() != null
						&& setelahnya11.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				persetujuanAdaDiSini12.getParent().setVisible(setelahnya12.getSelectedItem() != null
						&& setelahnya12.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				persetujuanAdaDiSini13.getParent().setVisible(setelahnya13.getSelectedItem() != null
						&& setelahnya13.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				persetujuanAdaDiSini14.getParent().setVisible(setelahnya14.getSelectedItem() != null
						&& setelahnya14.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				persetujuanAdaDiSini15.getParent().setVisible(setelahnya15.getSelectedItem() != null
						&& setelahnya15.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				persetujuanAdaDiSini16.getParent().setVisible(setelahnya16.getSelectedItem() != null
						&& setelahnya16.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				persetujuanAdaDiSini17.getParent().setVisible(setelahnya17.getSelectedItem() != null
						&& setelahnya17.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				persetujuanAdaDiSini18.getParent().setVisible(setelahnya18.getSelectedItem() != null
						&& setelahnya18.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				persetujuanAdaDiSini19.getParent().setVisible(setelahnya19.getSelectedItem() != null
						&& setelahnya19.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
				persetujuanAdaDiSini20.getParent().setVisible(setelahnya20.getSelectedItem() != null
						&& setelahnya20.getSelectedItem().getValue() != null && !alurSetelahnyaOtomatis.isChecked());
			}
		};

		alurSetelahnyaOtomatis.addEventListener("onClick", s);

		setelahnya.addEventListener("onChange", s);
		setelahnya2.addEventListener("onChange", s);
		setelahnya3.addEventListener("onChange", s);
		setelahnya4.addEventListener("onChange", s);
		setelahnya5.addEventListener("onChange", s);
		setelahnya6.addEventListener("onChange", s);
		setelahnya7.addEventListener("onChange", s);
		setelahnya8.addEventListener("onChange", s);
		setelahnya9.addEventListener("onChange", s);
		setelahnya10.addEventListener("onChange", s);

		setelahnya11.addEventListener("onChange", s);
		setelahnya12.addEventListener("onChange", s);
		setelahnya13.addEventListener("onChange", s);
		setelahnya14.addEventListener("onChange", s);
		setelahnya15.addEventListener("onChange", s);
		setelahnya16.addEventListener("onChange", s);
		setelahnya17.addEventListener("onChange", s);
		setelahnya18.addEventListener("onChange", s);
		setelahnya19.addEventListener("onChange", s);
		setelahnya20.addEventListener("onChange", s);

		opsiSetelahnya.addEventListener("onChange", s);
		opsiSetelahnya2.addEventListener("onChange", s);
		opsiSetelahnya3.addEventListener("onChange", s);
		opsiSetelahnya4.addEventListener("onChange", s);
		opsiSetelahnya5.addEventListener("onChange", s);
		opsiSetelahnya6.addEventListener("onChange", s);
		opsiSetelahnya7.addEventListener("onChange", s);
		opsiSetelahnya8.addEventListener("onChange", s);
		opsiSetelahnya9.addEventListener("onChange", s);
		opsiSetelahnya10.addEventListener("onChange", s);

		opsiSetelahnya11.addEventListener("onChange", s);
		opsiSetelahnya12.addEventListener("onChange", s);
		opsiSetelahnya13.addEventListener("onChange", s);
		opsiSetelahnya14.addEventListener("onChange", s);
		opsiSetelahnya15.addEventListener("onChange", s);
		opsiSetelahnya16.addEventListener("onChange", s);
		opsiSetelahnya17.addEventListener("onChange", s);
		opsiSetelahnya18.addEventListener("onChange", s);
		opsiSetelahnya19.addEventListener("onChange", s);
		opsiSetelahnya20.addEventListener("onChange", s);

		s.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Opsi dari workflow sebelumnya"));
		row.appendChild(opsi = new Textbox(alurSop.getOpsi()));
		opsi.setWidth("90%");

		Common.initKeterangan(rows,
				"Jika alur ini merupakan percabangan alur sebelumnya, masukkan nilai Opsi, misal YA atau TIDAK");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jangka Waktu Tindak Lanjut (dalam hari) *"));
		row.appendChild(jangkaWaktu = new Intbox(alurSop.getJangkaWaktu()));
		jangkaWaktu.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alur diperuntukkan untuk"));
		row.appendChild(role = new Combobox());
		Map map = ConstantValues.ambilBerdasarClass(Tbmrole.class);
		for (Object o : map.values()) {
			Tbmrole tbmrole = (Tbmrole) o;
			if (tbmrole.getAktif()) {
				Comboitem comboitem = new Comboitem();
				comboitem.setLabel(tbmrole.getRoleName());
				comboitem.setValue(tbmrole);
				role.appendChild(comboitem);
			}
		}

		final MyFormRow rowUsernameDisposisi = new MyFormRow();
		rowUsernameDisposisi.setParent(rows);
		rowUsernameDisposisi.appendChild(new ais.ui.util.MyLabelConfig("Username pengguna yang melakukan disposisi *"));
		rowUsernameDisposisi.appendChild(khususUsername = new Textbox(alurSop.getKhususUsername()));
		khususUsername.setWidth("90%");
		khususUsername.setRows(2);

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Ambil Username Pengguna",
				"/img/user_male_add.png");

		final MyFormRow rowAmbilPengguna = new MyFormRow();
		rowAmbilPengguna.setParent(rows);
		rowAmbilPengguna.appendChild(new ais.ui.util.MyLabelConfig(""));
		rowAmbilPengguna.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				AmbilDataTbmuserBanyak ambil = new AmbilDataTbmuserBanyak(new ArrayList<Tbmuser>());
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
				ambil.setEventListener(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub
						List<Tbmuser> tbmusers = (List<Tbmuser>) arg0.getData();
						if (tbmusers != null && tbmusers.size() != 0) {
							for (Tbmuser tbmuser : tbmusers) {
								khususUsername.setValue(khususUsername.getValue()
										+ (khususUsername.getValue().isEmpty() ? tbmuser.getUserId()
												: "," + tbmuser.getUserId()));
							}
						}
					}
				});
				ambil.setWidth("850px");
				ambil.setHeight("97%");
				ambil.setVisible(true);
				ambil.onModal();
			}
		});

		final Row r = Common.initKeterangan(rows, "Jika lebih dari satu pengguna, pisahkan dengan tanda koma (,)");

		EventListener startEvent = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				AktorSop s = (AktorSop) (aktorSop.getSelectedItem() == null ? null
						: aktorSop.getSelectedItem().getValue());

				role.getParent().setVisible(s == null);
				aktor.getParent().setVisible(s == null);
				r.setVisible(!start.isChecked() && s == null);
				rowAmbilPengguna.setVisible(!start.isChecked() && s == null);
				rowUsernameDisposisi.setVisible(!start.isChecked() && s == null);
			}

		};

		aktorSop.addEventListener("onChange", startEvent);
		start.addEventListener("onClick", startEvent);
		startEvent.onEvent(null);

		EventListener eventListenerkembaliKePengaju = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				aktorSop.getParent().setVisible(!kembaliKePengaju.isChecked());
				kembaliKeAktorSebelumnya.getParent().setVisible(!kembaliKePengaju.isChecked());
				rowUsernameDisposisi.setVisible(!kembaliKePengaju.isChecked());
				rowAmbilPengguna.setVisible(!kembaliKePengaju.isChecked());
				r.setVisible(!kembaliKePengaju.isChecked());
			}
		};

		eventListenerkembaliKePengaju.onEvent(null);
		kembaliKePengaju.addEventListener("onClick", eventListenerkembaliKePengaju);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Form Inputan"));
		formInputan = new AmbilDataFormSopBanbox();
		row.appendChild(formInputan);
		formInputan.setValue(alurSop.getLabelFormInputan());
		formInputan.setAttribute("data", alurSop.getFormInputan());
		formInputan.setAttribute("myValue", alurSop.getFormInputan());
		formInputan.setReadonly(true);
		formInputan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(bekukanFormTampilan = new MyCheckboxConfig("Form tidak boleh diubah"));
		bekukanFormTampilan.setChecked(alurSop.getBekukanFormTampilan());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(bekukanDokumen = new MyCheckboxConfig("Dokumen tidak boleh diganti"));
		bekukanDokumen.setChecked(alurSop.getBekukanDokumen());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(alurSop.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(2);

		initDokumen(rows);

		initKelompokParameterTambahanAlurSop(rows);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Menu yang diakses saat alur SOP berjalan"));

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		array = new JSONArray(alurSop.getHalamanMenu());
		Row rowMenu = Common.tampilanScroll1(row);
		reloadMenu(rowMenu, array);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							AlurSopAction.this.alurSop.hitungNomor(HibernateUtil.currentSession());
							onSearchDefault(null);
							addWindow.setVisible(false);
						}
					});

				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	private void initDokumen(Rows rows) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		final MyGrid subGrid = new MyGrid();
		row.appendChild(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		Column c = new Column("Dokumen Alur SOP");
		subColumns.appendChild(c);

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		List<DokumenAlurSop> dokumenSopList = ConstantValues
				.simpleList(
						HibernateUtil.currentSession().createCriteria(DokumenAlurSop.class)
								.add(Restrictions.or(Restrictions.isNull("sop"),
										Restrictions.eq("sop", alurSop.getSop())))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
						DokumenAlurSop.class);

		Collections.sort(dokumenSopList);

		if (this.alurSop.getId() != null) {
			Session session = HibernateUtil.currentSession();
			AlurSop alurSop = (AlurSop) session.createCriteria(AlurSop.class)
					.add(Restrictions.idEq(this.alurSop.getId())).uniqueResult();
			selectedDokumenAlurSop = alurSop.getDokumenAlurSops();
		} else {
			selectedDokumenAlurSop = new HashSet<DokumenAlurSop>();
		}
		Set<Long> ids = new HashSet<Long>();
		for (DokumenAlurSop v : selectedDokumenAlurSop) {
			if (v.getAktif()) {
				ids.add(v.getId());
			}
		}

		System.out.println("ids ->" + ids);

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final DokumenAlurSop dokumenAlurSop : dokumenSopList) {
			if (dokumenAlurSop.getAktif()) {
				final Checkbox checkbox = new Checkbox(dokumenAlurSop.getKode() + " " + dokumenAlurSop.getNama() + " ("
						+ (dokumenAlurSop.getWajib() ? "Wajib" : "Tidak Wajib") + ")");
				checkbox.setParent(vboxSkala);
				checkbox.setChecked(ids.contains(dokumenAlurSop.getId()));
				checkbox.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (checkbox.isChecked()) {
							selectedDokumenAlurSop.add(dokumenAlurSop);
						} else {

							for (DokumenAlurSop a : selectedDokumenAlurSop) {
								if (a.getId().equals(dokumenAlurSop.getId())) {
									selectedDokumenAlurSop.remove(a);
									break;
								}
							}

						}

						System.out.println("selectedDokumenAlurSop => " + selectedDokumenAlurSop);
					}
				});
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void initKelompokParameterTambahanAlurSop(Rows rows) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		final MyGrid subGrid = new MyGrid();
		row.appendChild(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		Column c = new Column("Parameter Alur SOP");
		subColumns.appendChild(c);

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		@SuppressWarnings("unchecked")
		Map<Long, KelompokParameterTambahanAlurSop> kelompokParameterTambahanAlurSops = ConstantValues
				.ambilBerdasarClass(KelompokParameterTambahanAlurSop.class);

		if (this.alurSop.getId() != null) {
			Session session = HibernateUtil.currentSession();
			AlurSop alurSop = (AlurSop) session.createCriteria(AlurSop.class)
					.add(Restrictions.idEq(this.alurSop.getId())).uniqueResult();
			// Gunakan salinan berbasis ID. Jangan memakai TreeSet/PersistentSet secara
			// langsung karena beberapa kelompok boleh mempunyai nomor urut yang sama.
			// Selain mencegah pilihan kedua dianggap duplikat, salinan ini memastikan
			// tombol Batal tidak ikut mengubah koleksi entity yang sedang dikelola session.
			selectedKelompokParameterTambahanAlurSop = new HashSet<KelompokParameterTambahanAlurSop>(
					alurSop.getKelompokParameterTambahanAlurSops());
		} else {
			selectedKelompokParameterTambahanAlurSop = new HashSet<KelompokParameterTambahanAlurSop>();
		}

		Set<Long> ids = new HashSet<Long>();
		for (KelompokParameterTambahanAlurSop v : selectedKelompokParameterTambahanAlurSop) {
			ids.add(v.getId());
		}

		System.out.println("ids ->" + ids);

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final KelompokParameterTambahanAlurSop kelompokParameterTambahanAlurSop : kelompokParameterTambahanAlurSops
				.values()) {
			final Checkbox checkbox = new Checkbox(kelompokParameterTambahanAlurSop.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(ids.contains(kelompokParameterTambahanAlurSop.getId()));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedKelompokParameterTambahanAlurSop.add(kelompokParameterTambahanAlurSop);
					} else {

						for (KelompokParameterTambahanAlurSop a : selectedKelompokParameterTambahanAlurSop) {
							if (a.getId().equals(kelompokParameterTambahanAlurSop.getId())) {
								selectedKelompokParameterTambahanAlurSop.remove(a);
								break;
							}
						}

					}

					System.out.println(
							"selectedKelompokParameterTambahanAlurSop => " + selectedKelompokParameterTambahanAlurSop);
				}
			});
		}
	}

	public boolean onSave(Event event) throws Exception {
		AktorSop aktorSop = (AktorSop) (this.aktorSop.getSelectedItem() == null ? null
				: this.aktorSop.getSelectedItem().getValue());
		if (aktorSop == null && aktor.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Aktor Alur SOP belum diisi. Langkah yang dapat dilakukan: (1) pilih atau isi kolom Aktor Alur SOP; (2) pastikan aktor yang dipilih sesuai dengan tahapan alur ini; (3) ulangi proses menyimpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Alur SOP belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Kode Alur SOP; (2) isikan kode yang unik dan sesuai ketentuan; (3) ulangi proses menyimpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Alur SOP belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Nama Alur SOP; (2) isikan nama langkah alur SOP yang sesuai; (3) ulangi proses menyimpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (!kembaliKePengaju.isChecked() && aktorSop == null && !start.isChecked()
				&& khususUsername.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Username pengguna yang melakukan disposisi belum diisi. Langkah yang dapat dilakukan: (1) isikan username pengguna yang akan melakukan disposisi pada kolom yang tersedia; (2) pastikan username yang dimasukkan benar dan terdaftar di sistem; (3) ulangi proses menyimpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if ((searchsop.getSelectedItem() == null || searchsop.getSelectedItem().getValue() == null)) {
			MyMessageboxConfig.show("Mohon maaf, SOP belum dipilih. Langkah yang dapat dilakukan: (1) pilih SOP terlebih dahulu pada filter pencarian di atas; (2) pastikan SOP yang dipilih sudah benar; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (alurSop.getId() != null) {
			alurSop = (AlurSop) session.load(AlurSop.class, alurSop.getId());
		}
		alurSop.setAktor(aktor.getValue());
		alurSop.setStart(start.isChecked());
		alurSop.setAlurSetelahnyaOtomatis(alurSetelahnyaOtomatis.isChecked());
		alurSop.setAlurSetelahnyaBerupaPilihan(alurSetelahnyaBerupaPilihan.isChecked());
		alurSop.setPenolakanAdaDiSini(penolakanAdaDiSini.isChecked());
		alurSop.setPersetujuanAdaDiSini(persetujuanAdaDiSini.isChecked());
		alurSop.setPersetujuanAdaDiSini1(persetujuanAdaDiSini1.isChecked());
		alurSop.setPersetujuanAdaDiSini2(persetujuanAdaDiSini2.isChecked());
		alurSop.setPersetujuanAdaDiSini3(persetujuanAdaDiSini3.isChecked());
		alurSop.setPersetujuanAdaDiSini4(persetujuanAdaDiSini4.isChecked());
		alurSop.setPersetujuanAdaDiSini5(persetujuanAdaDiSini5.isChecked());
		alurSop.setPersetujuanAdaDiSini6(persetujuanAdaDiSini6.isChecked());
		alurSop.setPersetujuanAdaDiSini7(persetujuanAdaDiSini7.isChecked());
		alurSop.setPersetujuanAdaDiSini8(persetujuanAdaDiSini8.isChecked());
		alurSop.setPersetujuanAdaDiSini9(persetujuanAdaDiSini9.isChecked());
		alurSop.setPersetujuanAdaDiSini10(persetujuanAdaDiSini10.isChecked());

		alurSop.setPersetujuanAdaDiSini11(persetujuanAdaDiSini11.isChecked());
		alurSop.setPersetujuanAdaDiSini12(persetujuanAdaDiSini12.isChecked());
		alurSop.setPersetujuanAdaDiSini13(persetujuanAdaDiSini13.isChecked());
		alurSop.setPersetujuanAdaDiSini14(persetujuanAdaDiSini14.isChecked());
		alurSop.setPersetujuanAdaDiSini15(persetujuanAdaDiSini15.isChecked());
		alurSop.setPersetujuanAdaDiSini16(persetujuanAdaDiSini16.isChecked());
		alurSop.setPersetujuanAdaDiSini17(persetujuanAdaDiSini17.isChecked());
		alurSop.setPersetujuanAdaDiSini18(persetujuanAdaDiSini18.isChecked());
		alurSop.setPersetujuanAdaDiSini19(persetujuanAdaDiSini19.isChecked());
		alurSop.setPersetujuanAdaDiSini20(persetujuanAdaDiSini20.isChecked());

		alurSop.setAlurSetelahnyaTidakWajib(alurSetelahnyaTidakWajib.isChecked());
		alurSop.setKode(kode.getValue());
		alurSop.setNama(nama.getValue());
		alurSop.setKeterangan(keterangan.getValue());
		alurSop.setKhususUsername(khususUsername.getValue());
		if (searchsop.getSelectedItem().getValue() != null) {
			alurSop.setSop((Sop) searchsop.getSelectedItem().getValue());
		}
		alurSop.setSebelumnya(
				(AlurSop) (sebelumnya.getSelectedItem() == null ? null : sebelumnya.getSelectedItem().getValue()));

		alurSop.setSetelahnya(
				(AlurSop) (setelahnya.getSelectedItem() == null ? null : setelahnya.getSelectedItem().getValue()));

		alurSop.setSetelahnya2(
				(AlurSop) (setelahnya2.getSelectedItem() == null ? null : setelahnya2.getSelectedItem().getValue()));

		alurSop.setSetelahnya3(
				(AlurSop) (setelahnya3.getSelectedItem() == null ? null : setelahnya3.getSelectedItem().getValue()));

		alurSop.setSetelahnya4(
				(AlurSop) (setelahnya4.getSelectedItem() == null ? null : setelahnya4.getSelectedItem().getValue()));

		alurSop.setSetelahnya5(
				(AlurSop) (setelahnya5.getSelectedItem() == null ? null : setelahnya5.getSelectedItem().getValue()));

		alurSop.setSetelahnya6(
				(AlurSop) (setelahnya6.getSelectedItem() == null ? null : setelahnya6.getSelectedItem().getValue()));

		alurSop.setSetelahnya7(
				(AlurSop) (setelahnya7.getSelectedItem() == null ? null : setelahnya7.getSelectedItem().getValue()));

		alurSop.setSetelahnya8(
				(AlurSop) (setelahnya8.getSelectedItem() == null ? null : setelahnya8.getSelectedItem().getValue()));

		alurSop.setSetelahnya9(
				(AlurSop) (setelahnya9.getSelectedItem() == null ? null : setelahnya9.getSelectedItem().getValue()));

		alurSop.setSetelahnya10(
				(AlurSop) (setelahnya10.getSelectedItem() == null ? null : setelahnya10.getSelectedItem().getValue()));

		alurSop.setSetelahnya11(
				(AlurSop) (setelahnya11.getSelectedItem() == null ? null : setelahnya11.getSelectedItem().getValue()));
		alurSop.setSetelahnya12(
				(AlurSop) (setelahnya12.getSelectedItem() == null ? null : setelahnya12.getSelectedItem().getValue()));
		alurSop.setSetelahnya13(
				(AlurSop) (setelahnya13.getSelectedItem() == null ? null : setelahnya13.getSelectedItem().getValue()));
		alurSop.setSetelahnya14(
				(AlurSop) (setelahnya14.getSelectedItem() == null ? null : setelahnya14.getSelectedItem().getValue()));
		alurSop.setSetelahnya15(
				(AlurSop) (setelahnya15.getSelectedItem() == null ? null : setelahnya15.getSelectedItem().getValue()));
		alurSop.setSetelahnya16(
				(AlurSop) (setelahnya16.getSelectedItem() == null ? null : setelahnya16.getSelectedItem().getValue()));
		alurSop.setSetelahnya17(
				(AlurSop) (setelahnya17.getSelectedItem() == null ? null : setelahnya17.getSelectedItem().getValue()));
		alurSop.setSetelahnya18(
				(AlurSop) (setelahnya18.getSelectedItem() == null ? null : setelahnya18.getSelectedItem().getValue()));
		alurSop.setSetelahnya19(
				(AlurSop) (setelahnya19.getSelectedItem() == null ? null : setelahnya19.getSelectedItem().getValue()));
		alurSop.setSetelahnya20(
				(AlurSop) (setelahnya20.getSelectedItem() == null ? null : setelahnya20.getSelectedItem().getValue()));

		alurSop.setOpsi(opsi.getValue());
		alurSop.setJangkaWaktu(jangkaWaktu.getValue());

		alurSop.setDokumenAlurSops(selectedDokumenAlurSop);
		alurSop.setKelompokParameterTambahanAlurSops(selectedKelompokParameterTambahanAlurSop);
		alurSop.setHalamanMenu(array.toString());

		alurSop.setAktorSop(aktorSop);

		alurSop.setFormInputan((String) (formInputan.getAttribute("data")));
		alurSop.setLabelFormInputan((String) (formInputan.getValue()));

		alurSop.setBekukanFormTampilan(bekukanFormTampilan.isChecked());
		alurSop.setBekukanDokumen(bekukanDokumen.isChecked());
		alurSop.setBolehDiisiCatatan(bolehDiisiCatatan.isChecked());
		alurSop.setCatatanWajibDiisi(catatanWajibDiisi.isChecked());
		alurSop.setLampiranCatatanWajibDiisi(lampiranCatatanWajibDiisi.isChecked());
		alurSop.setJikaProsesDisetujuiMakaSelesai(jikaProsesDisetujuiMakaSelesai.isChecked());
		alurSop.setKembaliKePengaju(kembaliKePengaju.isChecked());
		alurSop.setKembaliKeAktorSebelumnya(kembaliKeAktorSebelumnya.isChecked());

		alurSop.setOpsiSetelahnya(opsiSetelahnya.getValue());
		alurSop.setOpsiSetelahnya2(opsiSetelahnya2.getValue());
		alurSop.setOpsiSetelahnya3(opsiSetelahnya3.getValue());
		alurSop.setOpsiSetelahnya4(opsiSetelahnya4.getValue());
		alurSop.setOpsiSetelahnya5(opsiSetelahnya5.getValue());
		alurSop.setOpsiSetelahnya6(opsiSetelahnya6.getValue());
		alurSop.setOpsiSetelahnya7(opsiSetelahnya7.getValue());
		alurSop.setOpsiSetelahnya8(opsiSetelahnya8.getValue());
		alurSop.setOpsiSetelahnya9(opsiSetelahnya9.getValue());
		alurSop.setOpsiSetelahnya10(opsiSetelahnya10.getValue());

		alurSop.setOpsiSetelahnya11(opsiSetelahnya11.getValue());
		alurSop.setOpsiSetelahnya12(opsiSetelahnya12.getValue());
		alurSop.setOpsiSetelahnya13(opsiSetelahnya13.getValue());
		alurSop.setOpsiSetelahnya14(opsiSetelahnya14.getValue());
		alurSop.setOpsiSetelahnya15(opsiSetelahnya15.getValue());
		alurSop.setOpsiSetelahnya16(opsiSetelahnya16.getValue());
		alurSop.setOpsiSetelahnya17(opsiSetelahnya17.getValue());
		alurSop.setOpsiSetelahnya18(opsiSetelahnya18.getValue());
		alurSop.setOpsiSetelahnya19(opsiSetelahnya19.getValue());
		alurSop.setOpsiSetelahnya20(opsiSetelahnya20.getValue());

		alurSop.setTanggalDisposisiBolehDiubah(tanggalDisposisiBolehDiubah.isChecked());

		Common.refreshSaveOrUpdate(session, alurSop);
		session.flush();

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(AlurSop.class)
				// searchaktif & combo filter bisa null bila ZUL filter tidak memuat kontrol tsb
				// (mis. tata letak filter diubah). Guard null agar pencarian tidak NPE — default
				// "tampilkan yang aktif" sama seperti saat checkbox tercentang.
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))
				.createAlias("sop", "sop").add(Restrictions.eq("sop.aktif", true));

		if (order)
			criteria.addOrder(Order.desc("sop.id")).addOrder(Order.asc("nomor")).addOrder(Order.asc("kode"))
					.addOrder(Order.asc("nama"));

		Sop sopTerpilih = getSopFilterTerpilih();
		String keyword = searchnama == null || searchnama.getValue() == null ? "" : searchnama.getValue().trim();

		criteria

				/*
				 * Jika SOP belum dipilih, tabel Alur SOP sengaja dibuat kosong.
				 * Ini mencegah query besar yang sebelumnya membaca alur dari semua SOP,
				 * sehingga halaman master Alur SOP jauh lebih ringan saat pertama dibuka.
				 */
				.add(sopTerpilih == null || sopTerpilih.getId() == null
						? Restrictions.sqlRestriction("1=0")
						: Restrictions.eq("sop", sopTerpilih))

				.add(keyword.length() == 0 ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("kode", keyword, MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.ilike("aktor", keyword, MatchMode.ANYWHERE),
										Restrictions.ilike("nama", keyword, MatchMode.ANYWHERE))));

		criteria.add(searchjurusan == null || searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("sop.jurusan"),
								CommonSearchFilterHelper.eqSelectedWithId("sop.jurusan", searchjurusan, false)))

				.add(searchfakultas == null || searchfakultas.getSelectedItem() == null
						|| searchfakultas.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("sop.fakultas"),
										CommonSearchFilterHelper.eqSelectedWithId("sop.fakultas", searchfakultas, false)))

				.add(searchsekolah == null || searchsekolah.getSelectedItem() == null
						|| searchsekolah.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("sop.sekolah"),
										CommonSearchFilterHelper.eqSelectedWithId("sop.sekolah", searchsekolah, false)))

				.add(searchyayasan == null || searchyayasan.getSelectedItem() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("sop.yayasan"),
										CommonSearchFilterHelper.eqSelectedWithId("sop.yayasan", searchyayasan, false)));

		criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
		        ? Restrictions.sqlRestriction("true")
		        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Sop sopTerpilih = getSopFilterTerpilih();
		if (sopTerpilih == null || sopTerpilih.getId() == null) {
			renderSopBelumDipilihPadaDiagram();
			renderSopBelumDipilihPadaTabel();
			resetPagingDanGridKosong();
			return;
		}

		renderDiagramSopTerpilih();
		siagaTabelDataAlurSop();
		Common.initPaging(initCriteria(false), paging);

		List<AlurSop> alurSop = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(alurSop);
		aktorLookupRender = new SopUtil.AktorLookup();
		grid.setRowRenderer(new AlurSopRenderer());
		grid.setModelCheckMobile(strset);

	}


	private void renderSopBelumDipilihPadaDiagram() {
		if (diagramSopContainer == null) {
			return;
		}
		try {
			Common.clear(diagramSopContainer);
			appendSopBelumDipilihInfo(diagramSopContainer, "Diagram Alur SOP",
					"Diagram belum ditampilkan karena SOP belum dipilih.",
					"Silakan pilih salah satu SOP pada filter di atas. Setelah SOP dipilih, sistem akan menampilkan urutan alur, petugas, dokumen yang diperlukan, serta cabang keputusan pada SOP tersebut.");
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void renderSopBelumDipilihPadaTabel() {
		if (tabelScrollContainer == null || grid == null) {
			return;
		}
		try {
			try {
				grid.detach();
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			Common.clear(tabelScrollContainer);
			appendSopBelumDipilihInfo(tabelScrollContainer, "Tabel Data Alur",
					"Tabel data alur belum ditampilkan karena SOP belum dipilih.",
					"Pilih SOP terlebih dahulu agar sistem hanya mengambil data alur yang sesuai. Cara ini membuat halaman lebih cepat karena sistem tidak perlu membaca seluruh alur dari semua SOP.");
			tabelScrollContainer.setHeight("2540px");
			tabelScrollContainer.setStyle(buildTabelScrollStyleAlurSop());
			grid.setWidth("100%");
			grid.setHeight("auto");
			grid.setStyle(appendStyleAlurSop(grid.getStyle(), "width:100%;height:auto;min-height:600px;overflow:visible;box-sizing:border-box;"));
			grid.setParent(tabelScrollContainer);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	private void resetPagingDanGridKosong() {
		try {
			if (paging != null) {
				paging.setActivePage(0);
				paging.setTotalSize(0);
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			List<AlurSop> kosong = new ArrayList<AlurSop>();
			ListModel model = new SimpleListModel(kosong);
			if (grid != null) {
				grid.setRowRenderer(new AlurSopRenderer());
				grid.setModelCheckMobile(model);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void siagaTabelDataAlurSop() {
		if (tabelScrollContainer == null || grid == null) {
			return;
		}
		try {
			try {
				grid.detach();
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			Common.clear(tabelScrollContainer);
			tabelScrollContainer.setHeight("2540px");
			tabelScrollContainer.setStyle(buildTabelScrollStyleAlurSop());
			grid.setWidth("100%");
			grid.setHeight("auto");
			grid.setStyle(appendStyleAlurSop(grid.getStyle(), "width:100%;height:auto;min-height:600px;overflow:visible;box-sizing:border-box;"));
			grid.setParent(tabelScrollContainer);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void appendSopBelumDipilihInfo(Component parent, String title, String shortMessage, String detailMessage) {
		if (parent == null) {
			return;
		}
		StringBuilder html = new StringBuilder();
		html.append("<div style='margin:10px 12px 14px 12px;padding:16px 18px;border-radius:16px;");
		html.append("background:#ffffff;border:1px solid #dbe4ef;box-shadow:0 8px 22px rgba(15,23,42,.06);");
		html.append("font-family:Arial,sans-serif;color:#334155;'>");
		html.append("<div style='display:flex;gap:12px;align-items:flex-start;'>");
		html.append("<div style='width:36px;height:36px;line-height:36px;text-align:center;border-radius:999px;background:#eff6ff;color:#1d4ed8;font-weight:900;font-size:18px;'>i</div>");
		html.append("<div style='min-width:0;'>");
		html.append("<div style='font-size:15px;font-weight:900;color:#0f172a;'>").append(escapeHtml(title)).append("</div>");
		html.append("<div style='font-size:13px;font-weight:800;color:#1e3a8a;margin-top:4px;'>").append(escapeHtml(shortMessage)).append("</div>");
		html.append("<div style='font-size:12px;line-height:1.6;color:#475569;margin-top:6px;'>").append(escapeHtml(detailMessage)).append("</div>");
		html.append("</div></div></div>");
		new Html(html.toString()).setParent(parent);
	}

	private void renderDiagramSopTerpilih() {
		if (diagramSopContainer == null) {
			return;
		}
		try {
			Common.clear(diagramSopContainer);
			Sop sopTerpilih = getSopFilterTerpilih();
			if (sopTerpilih == null || sopTerpilih.getId() == null) {
				renderSopBelumDipilihPadaDiagram();
				return;
			}
			List<AlurSop> alurList = ambilAlurSopUntukDiagram(sopTerpilih);
			renderDiagramSop(diagramSopContainer, sopTerpilih, alurList);
			if (alurList != null) {
				alurList.clear();
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private Sop getSopFilterTerpilih() {
		try {
			if (searchsop != null && searchsop.getSelectedItem() != null
					&& searchsop.getSelectedItem().getValue() instanceof Sop) {
				return (Sop) searchsop.getSelectedItem().getValue();
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private List<AlurSop> ambilAlurSopUntukDiagram(Sop sop) {
		List<AlurSop> hasil = new ArrayList<AlurSop>();
		if (sop == null || sop.getId() == null) {
			return hasil;
		}
		try {
			/*
			 * Versi sebelumnya membaca seluruh cache AlurSop lalu memfilter per SOP. Pada
			 * database besar cara tersebut berat karena semua alur dari semua SOP disentuh.
			 * Untuk diagram, cukup ambil alur milik SOP terpilih langsung dari DB.
			 * currentSession() tidak ditutup manual karena dikelola request/thread aplikasi.
			 */
			Session session = HibernateUtil.currentSession();
			Criteria criteria = session.createCriteria(AlurSop.class)
					.add(Restrictions.eq("sop", sop))
					.add(searchaktif == null || searchaktif.isChecked()
							? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
							: Restrictions.sqlRestriction("true"))
					.addOrder(Order.asc("nomor")).addOrder(Order.asc("kode")).addOrder(Order.asc("nama"));
			hasil = criteria.list();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return hasil == null ? new ArrayList<AlurSop>() : hasil;
	}


	@SuppressWarnings("unchecked")
	private Map<Long, List<DiagramDokumenInfo>> ambilDokumenMapUntukDiagram(List<AlurSop> alurList) {
		Map<Long, List<DiagramDokumenInfo>> hasil = new HashMap<Long, List<DiagramDokumenInfo>>();
		if (alurList == null || alurList.isEmpty()) {
			return hasil;
		}

		List<Long> ids = new ArrayList<Long>();
		for (AlurSop alur : alurList) {
			try {
				if (alur != null && alur.getId() != null) {
					ids.add(alur.getId());
				}
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		}
		if (ids.isEmpty()) {
			return hasil;
		}

		try {
			Session session = HibernateUtil.currentSession();
			/*
			 * Gunakan parameterList, bukan menyusun IN (...) sebagai string panjang.
			 * Lebih aman, mengurangi parsing SQL berulang, dan lebih mudah dimanfaatkan
			 * planner PostgreSQL bersama index alur_sop_has_dokumen(alur_sop, dokumen).
			 */
			String sql = "select r.alur_sop, d.kode, d.nama, coalesce(d.wajib, false) "
					+ "from public.alur_sop_has_dokumen r "
					+ "join public.dokumen_alur_sop d on d.id = r.dokumen and (d.aktif = true or d.aktif is null) "
					+ "where r.alur_sop in (:ids) "
					+ "order by r.alur_sop, d.kode, d.nama";
			org.hibernate.SQLQuery query = session.createSQLQuery(sql);
			query.setParameterList("ids", ids);
			List rows = query.list();
			for (Object o : rows) {
				try {
					Object[] data = (Object[]) o;
					Long alurId = data[0] == null ? null : Long.valueOf(data[0].toString());
					if (alurId == null) {
						continue;
					}
					List<DiagramDokumenInfo> list = hasil.get(alurId);
					if (list == null) {
						list = new ArrayList<DiagramDokumenInfo>();
						hasil.put(alurId, list);
					}
					boolean wajib = false;
					try {
						if (data[3] instanceof Boolean) {
							wajib = ((Boolean) data[3]).booleanValue();
						} else if (data[3] != null) {
							String flag = data[3].toString();
							wajib = "true".equalsIgnoreCase(flag) || "t".equalsIgnoreCase(flag) || "1".equals(flag);
						}
					} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					list.add(new DiagramDokumenInfo(data[1] == null ? "" : data[1].toString(),
							data[2] == null ? "" : data[2].toString(), wajib));
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			ids.clear();
		}
		return hasil;
	}


	/**
	 * Merender seluruh bagan alur SOP dalam bentuk diagram flowchart vertikal yang
	 * mudah dipahami oleh siapa pun, termasuk pengguna yang tidak terbiasa dengan
	 * teknologi informasi.
	 *
	 * <p><b>Tujuan tampilan:</b> Memperlihatkan urutan langkah kerja SOP dari awal
	 * hingga akhir secara visual — lengkap dengan informasi aktor (petugas yang
	 * bertanggung jawab), durasi, dokumen yang dibutuhkan, dan percabangan jika ada.</p>
	 *
	 * <p><b>Struktur visual:</b></p>
	 * <pre>
	 *     [MULAI] ─ node oval hijau
	 *        ↓
	 *  ┌────────────────────────────┐
	 *  │ [1] 001 ─ Nama Langkah    │
	 *  │  👥 Diajukan oleh: X      │
	 *  │  ⏱ 1 hari                │
	 *  │  📄 Dokumen wajib: ...    │
	 *  │  → Langkah berikutnya     │
	 *  └────────────────────────────┘
	 *        ↓
	 *  ┌────────────────────────────┐
	 *  │ [2] 002 ─ Verifikasi      │
	 *  │  ...                      │
	 *  └────────────────────────────┘
	 *        ↓
	 *     [SELESAI] ─ node oval merah
	 * </pre>
	 *
	 * <p><b>Efisiensi data:</b> Seluruh dokumen dari semua langkah dimuat dalam satu
	 * query SQL melalui {@link #ambilDokumenMapUntukDiagram(List)} sebelum loop dimulai,
	 * menghindari N+1 query. currentSession() tidak ditutup manual karena dikelola ZK
	 * per request.</p>
	 *
	 * <p><b>Komponen ZK:</b> Vbox sebagai wrapper blok tiap kartu; Html untuk konten
	 * visual; Hbox untuk tombol Edit/Hapus dengan event listener server-side.</p>
	 *
	 * @param parent   komponen ZK target; tidak boleh null
	 * @param sop      SOP yang ditampilkan; boleh null (ditangani gracefully)
	 * @param alurList daftar langkah terurut; boleh null/kosong
	 */
	private void renderDiagramSop(Component parent, Sop sop, List<AlurSop> alurList) {
		if (parent == null) {
			return;
		}
		Common.clear(parent);
		try {
			if (parent instanceof Vbox) {
				((Vbox) parent).setWidth("100%");
				((Vbox) parent).setStyle("width:100%;overflow:visible;background:transparent;");
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		// Muat semua dokumen sekaligus — mencegah N+1 query di dalam loop kartu
		diagramDokumenByAlur = ambilDokumenMapUntukDiagram(alurList);

		final String namaSop = (sop == null || sop.getNama() == null) ? "SOP" : sop.getNama();
		final String kodeSop = (sop == null || sop.getKode() == null) ? "" : sop.getKode();

		// Hitung statistik sekali saja sebelum render
		int cntAktif = 0, cntNonAktif = 0, cntCabang = 0;
		if (alurList != null) {
			for (AlurSop alur : alurList) {
				if (alur == null) continue;
				if (alur.getAktif()) cntAktif++; else cntNonAktif++;
				try {
					if (alur.ambilAlurSetelahnya() != null && alur.ambilAlurSetelahnya().size() > 1) cntCabang++;
				} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sop/AlurSopAction.java:2652"); /* hanya statistik — aman diabaikan */ }
			}
		}

		Vbox shell = new Vbox();
		shell.setWidth("100%");
		shell.setStyle("background:#eef2f7;box-sizing:border-box;overflow:visible;padding:8px;min-height:400px;");
		shell.setParent(parent);

		// CSS scoped supaya tidak bentrok dengan CSS ZK lain
		new Html(buildDiagramGlobalCss()).setParent(shell);

		// Kartu header: nama SOP + statistik
		new Html(buildDiagramHeaderHtml(namaSop, kodeSop, cntAktif, cntNonAktif, cntCabang)).setParent(shell);

		if (alurList == null || alurList.isEmpty()) {
			appendDiagramInfo(shell, "Belum ada langkah pada SOP ini. Klik Tambah untuk membuat langkah pertama.");
			return;
		}

		// Kolom vertikal utama — semua kartu langkah disusun dari atas ke bawah
		Div flowCol = new Div();
		flowCol.setWidth("100%");
		flowCol.setStyle(
				"display:flex;flex-direction:column;align-items:center;"
				+ "width:100%;padding:0 4px 24px 4px;box-sizing:border-box;overflow:visible;");
		flowCol.setParent(shell);

		// Node START
		new Html(buildFlowStartNodeHtml()).setParent(flowCol);

		int nomor = 0;
		for (AlurSop alur : alurList) {
			nomor++;
			new Html(buildFlowArrowHtml()).setParent(flowCol);    // panah ↓
			appendFlowStepCard(flowCol, alur, nomor);             // kartu langkah
		}

		// Panah + node END di akhir
		new Html(buildFlowArrowHtml()).setParent(flowCol);
		new Html(buildFlowEndNodeHtml()).setParent(flowCol);

		// Keterangan legenda warna
		new Html(buildDiagramLegendHtml()).setParent(shell);
	}

	/**
	 * Menghasilkan blok CSS yang di-inject sekali ke dalam bagan SOP.
	 * Semua selektor menggunakan prefix {@code .sopafc-} agar tidak konflik dengan
	 * CSS ZK maupun halaman lain yang aktif bersamaan.
	 *
	 * <p>CSS yang dihasilkan mencakup:</p>
	 * <ul>
	 *   <li>Gaya kartu langkah dengan shadow dan border-radius modern.</li>
	 *   <li>Header kartu berwarna berdasarkan status (biru=start, hijau=aktif, abu=nonaktif).</li>
	 *   <li>Gaya seksi informasi (aktor, durasi, dokumen, percabangan) di dalam kartu.</li>
	 *   <li>Gaya node oval START dan END.</li>
	 *   <li>Gaya koneksi panah vertikal antar-langkah.</li>
	 *   <li>Media query {@code @media(max-width:640px)} untuk tampilan ponsel.</li>
	 * </ul>
	 *
	 * @return String HTML berisi satu elemen {@code &lt;style&gt;} dengan CSS scoped
	 */
	private String buildDiagramGlobalCss() {
		return "<style>"
			+ ".sopafc-hdr{padding:10px 14px;color:#fff;font-family:Arial,Helvetica,sans-serif;}"
			+ ".sopafc-hdr-start{background:linear-gradient(135deg,#1e3a8a,#1e40af);}"
			+ ".sopafc-hdr-active{background:linear-gradient(135deg,#14532d,#15803d);}"
			+ ".sopafc-hdr-inactive{background:linear-gradient(135deg,#334155,#475569);}"
			+ ".sopafc-body{padding:10px 14px;background:#fff;font-family:Arial,Helvetica,sans-serif;}"
			+ ".sopafc-section{display:flex;gap:8px;align-items:flex-start;"
			+ "padding:7px 10px;border-radius:10px;margin-bottom:7px;box-sizing:border-box;}"
			+ ".sopafc-sec-actor{background:#f8fafc;border:1px solid #e2e8f0;}"
			+ ".sopafc-sec-time{background:#f0f9ff;border:1px solid #bae6fd;align-items:center;}"
			+ ".sopafc-sec-doc{background:#fefce8;border:1px solid #fde68a;}"
			+ ".sopafc-sec-branch{background:#fff7ed;border:1px solid #fed7aa;}"
			+ ".sopafc-label{font-size:10px;font-weight:900;color:#64748b;letter-spacing:.5px;"
			+ "text-transform:uppercase;margin-bottom:2px;}"
			+ ".sopafc-val{font-size:12px;color:#0f172a;line-height:1.45;}"
			+ ".sopafc-badge{display:inline-block;padding:2px 8px;border-radius:999px;"
			+ "font-size:10px;font-weight:800;margin-right:3px;margin-top:2px;}"
			+ ".sopafc-node-pill{display:inline-flex;align-items:center;gap:8px;padding:10px 28px;"
			+ "border-radius:999px;font-size:13px;font-weight:900;"
			+ "font-family:Arial,Helvetica,sans-serif;letter-spacing:.3px;}"
			+ ".sopafc-arrow-wrap{display:flex;flex-direction:column;align-items:center;"
			+ "width:100%;margin:0;padding:0;}"
			+ ".sopafc-arrow-line{width:2px;height:26px;"
			+ "background:linear-gradient(180deg,#94a3b8 0%,#64748b 100%);}"
			+ ".sopafc-arrow-tip{font-size:16px;color:#64748b;line-height:1;}"
			+ "@media(max-width:640px){"
			+ ".sopafc-title{font-size:11px!important;}"
			+ ".sopafc-body{padding:7px 9px!important;}"
			+ ".sopafc-val{font-size:11px!important;}"
			+ ".sopafc-node-pill{padding:8px 18px!important;font-size:12px!important;}"
			+ "}"
			+ "</style>";
	}

	/**
	 * Menghasilkan HTML untuk kartu header bagan yang menampilkan nama SOP, kode,
	 * deskripsi singkat fungsi bagan, dan statistik ringkas (langkah aktif/nonaktif/
	 * percabangan) sebagai pill berwarna.
	 *
	 * <p>Deskripsi dibuat bebas jargon agar langsung dipahami staff operasional.
	 * Statistik pill di sisi kanan memberi gambaran sekilas tanpa perlu scroll.</p>
	 *
	 * @param namaSop     nama SOP
	 * @param kodeSop     kode SOP; boleh kosong
	 * @param cntAktif    jumlah langkah aktif
	 * @param cntNonAktif jumlah langkah nonaktif
	 * @param cntCabang   jumlah langkah yang memiliki lebih dari satu arah berikutnya
	 * @return String HTML kartu header
	 */
	private String buildDiagramHeaderHtml(String namaSop, String kodeSop,
			int cntAktif, int cntNonAktif, int cntCabang) {
		StringBuilder h = new StringBuilder();
		h.append("<div style='margin:0 0 12px 0;padding:14px 16px;border-radius:16px;"
				+ "background:#ffffff;border:1px solid #dbe4ef;"
				+ "box-shadow:0 6px 18px rgba(15,23,42,.07);font-family:Arial,sans-serif;'>");
		h.append("<div style='display:flex;justify-content:space-between;gap:12px;"
				+ "align-items:flex-start;flex-wrap:wrap;'>");

		// Kiri: judul + deskripsi
		h.append("<div style='max-width:740px;min-width:0;'>");
		h.append("<div style='font-size:16px;font-weight:900;color:#0f172a;'>")
				.append(escapeHtml(namaSop)).append("</div>");
		if (kodeSop != null && kodeSop.trim().length() > 0) {
			h.append("<div style='font-size:11px;color:#64748b;margin-top:3px;'>Kode: <b>")
					.append(escapeHtml(kodeSop)).append("</b></div>");
		}
		h.append("<div style='font-size:12px;color:#475569;line-height:1.6;margin-top:6px;'>"
				+ "Urutan langkah kerja dari awal hingga selesai — termasuk siapa yang mengerjakan, "
				+ "berapa lama waktu penyelesaian, dan dokumen apa yang dibutuhkan di tiap tahap."
				+ "</div>");
		h.append("</div>");

		// Kanan: statistik
		h.append("<div style='display:flex;gap:8px;flex-wrap:wrap;align-items:flex-start;'>");
		h.append(buildDiagramStat("Langkah Aktif", String.valueOf(cntAktif), "#dcfce7", "#166534"));
		h.append(buildDiagramStat("Nonaktif", String.valueOf(cntNonAktif), "#e5e7eb", "#374151"));
		h.append(buildDiagramStat("Ada Percabangan", String.valueOf(cntCabang), "#fef3c7", "#92400e"));
		h.append("</div></div></div>");
		return h.toString();
	}

	/**
	 * Menghasilkan HTML untuk node START (Mulai) di bagian paling atas bagan.
	 * Node berbentuk oval/pill berwarna hijau tua, mengikuti konvensi diagram alur
	 * internasional (ISO 5807) di mana oval menandakan titik awal dan akhir proses.
	 *
	 * @return String HTML node START
	 */
	private String buildFlowStartNodeHtml() {
		return "<div style='display:flex;flex-direction:column;align-items:center;width:100%;padding-top:8px;'>"
			+ "<div class='sopafc-node-pill' style='background:#15803d;color:#fff;"
			+ "box-shadow:0 4px 14px rgba(21,128,61,.40);border:2px solid #166534;'>"
			+ "<span style='font-size:18px;line-height:1;'>&#9654;</span>"
			+ "<span>MULAI PROSES</span>"
			+ "</div></div>";
	}

	/**
	 * Menghasilkan HTML untuk node END (Selesai) di bagian paling bawah bagan.
	 * Node berbentuk oval/pill berwarna merah, menandai akhir rangkaian proses SOP.
	 *
	 * @return String HTML node END
	 */
	private String buildFlowEndNodeHtml() {
		return "<div style='display:flex;flex-direction:column;align-items:center;width:100%;padding-bottom:8px;'>"
			+ "<div class='sopafc-node-pill' style='background:#dc2626;color:#fff;"
			+ "box-shadow:0 4px 14px rgba(220,38,38,.35);border:2px solid #b91c1c;'>"
			+ "<span style='font-size:18px;line-height:1;'>&#9632;</span>"
			+ "<span>PROSES SELESAI</span>"
			+ "</div></div>";
	}

	/**
	 * Menghasilkan HTML untuk koneksi panah vertikal (↓) antar-langkah bagan.
	 * Terdiri dari garis vertikal bergradasi abu-abu dengan segitiga bawah (▼).
	 *
	 * @return String HTML panah penghubung
	 */
	private String buildFlowArrowHtml() {
		return "<div class='sopafc-arrow-wrap'>"
			+ "<div class='sopafc-arrow-line'></div>"
			+ "<div class='sopafc-arrow-tip'>&#9660;</div>"
			+ "</div>";
	}

	/**
	 * Merender satu kartu langkah alur SOP sebagai elemen hybrid ZK + HTML.
	 *
	 * <p>Setiap kartu terdiri dari tiga lapisan:</p>
	 * <ol>
	 *   <li><b>Header berwarna</b> — nomor urut, kode dan nama langkah, badge status
	 *       (Awal Pengajuan / Aktif / Nonaktif) dan badge tambahan bila ada. Warna
	 *       header: biru untuk langkah start, hijau untuk aktif, abu-abu untuk nonaktif.</li>
	 *   <li><b>Body informasi</b> — aktor, durasi, daftar dokumen, dan info langkah
	 *       berikutnya / percabangan.</li>
	 *   <li><b>Tombol aksi</b> (jika ada hak edit/delete) — menggunakan komponen ZK
	 *       asli agar event listener server-side bekerja dengan benar.</li>
	 * </ol>
	 *
	 * <p><b>Mengapa Vbox sebagai container?</b> Vbox merender sebagai elemen blok
	 * HTML sehingga anak-anaknya (Html visual + Hbox tombol) tersusun vertikal.
	 * Html saja tidak bisa menjadi container komponen ZK lain.</p>
	 *
	 * <p><b>Null safety:</b> Jika alur null, method return tanpa aksi. Setiap akses
	 * field entity dilindungi try-catch individu.</p>
	 *
	 * @param parent komponen ZK target
	 * @param alur   langkah alur SOP yang dirender
	 * @param nomor  nomor urut tampilan (1-based)
	 */
	private void appendFlowStepCard(Component parent, final AlurSop alur, int nomor) {
		if (parent == null || alur == null) {
			return;
		}

		boolean aktif = alur.getAktif();
		boolean isStart = alur.getStart();
		boolean hasBranch = false;
		try {
			List<AlurSop> nexts = alur.ambilAlurSetelahnya();
			hasBranch = nexts != null && nexts.size() > 1;
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sop/AlurSopAction.java:2877"); /* hanya badge — aman diabaikan */ }

		List<DiagramDokumenInfo> dokumens = null;
		try {
			if (diagramDokumenByAlur != null && alur.getId() != null) {
				dokumens = diagramDokumenByAlur.get(alur.getId());
			}
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sop/AlurSopAction.java:2884"); /* fallback: null — ditangani di buildDocumentSectionHtml */ }

		String cardBorder = aktif ? (isStart ? "#93c5fd" : "#bbf7d0") : "#cbd5e1";
		String cardBg = aktif ? (isStart ? "#eff6ff" : "#f0fdf4") : "#f8fafc";

		Vbox card = new Vbox();
		card.setWidth("100%");
		card.setStyle(
				"width:100%;max-width:700px;box-sizing:border-box;"
				+ "border-radius:16px;overflow:hidden;"
				+ "box-shadow:0 4px 18px rgba(15,23,42,.09);"
				+ "border:1.5px solid " + cardBorder + ";"
				+ "background:" + cardBg + ";");
		card.setParent(parent);

		// Konten visual (header + body)
		new Html(buildStepCardHtml(alur, nomor, aktif, isStart, hasBranch, dokumens)).setParent(card);

		// Tombol aksi ZK (event listener server-side)
		if (edit || delete) {
			try {
				Hbox actions = Common.copyEditDeleteButtons(edit, delete, alur, AlurSopAction.this);
				actions.setStyle(
						"padding:8px 14px;"
						+ "border-top:1px solid " + cardBorder + ";"
						+ "background:" + (aktif ? "#f8fafc" : "#f1f5f9") + ";");
				actions.setParent(card);
			} catch (Exception e) {
				appendFallbackActionButtons(card, alur);
			}
		}
	}

	/**
	 * Menghasilkan HTML lengkap (header + body) untuk kartu satu langkah alur SOP.
	 *
	 * <h2>Header</h2>
	 * Menampilkan: nomor urut (lingkaran), kode─nama langkah, badge status
	 * (Awal Pengajuan/Aktif/Nonaktif), badge "Selesai jika disetujui", "Titik
	 * penolakan", dan "Ada percabangan". Warna header ditentukan oleh status.
	 *
	 * <h2>Body</h2>
	 * Empat seksi informasi:
	 * <ol>
	 *   <li><b>Aktor</b> — siapa yang bertanggung jawab (dari buildActorTextFast).</li>
	 *   <li><b>Durasi</b> — jangka waktu penyelesaian dalam hari.</li>
	 *   <li><b>Dokumen</b> — daftar dokumen Wajib/Opsional.</li>
	 *   <li><b>Langkah berikutnya</b> — arah alur atau opsi percabangan.</li>
	 * </ol>
	 *
	 * <p>Semua output teks di-escape melalui {@link #escapeHtml(Object)}.</p>
	 *
	 * @param alur       entitas langkah alur SOP
	 * @param nomor      nomor urut tampilan
	 * @param aktif      apakah langkah ini aktif
	 * @param isStart    apakah ini langkah awal pengajuan
	 * @param hasBranch  apakah ada lebih dari satu kemungkinan langkah berikutnya
	 * @param dokumens   daftar dokumen; boleh null
	 * @return String HTML siap pakai untuk komponen Html ZK
	 */
	private String buildStepCardHtml(AlurSop alur, int nomor, boolean aktif, boolean isStart,
			boolean hasBranch, List<DiagramDokumenInfo> dokumens) {

		String hdrClass = aktif ? (isStart ? "sopafc-hdr sopafc-hdr-start"
				: "sopafc-hdr sopafc-hdr-active") : "sopafc-hdr sopafc-hdr-inactive";

		String statusLabel = aktif ? (isStart ? "Awal Pengajuan" : "Aktif") : "Nonaktif";
		String actorLabel = isStart ? "DIAJUKAN OLEH" : "DIPROSES OLEH";

		StringBuilder html = new StringBuilder();

		// ── Header ─────────────────────────────────────────────────────────
		html.append("<div class='").append(hdrClass).append("'>");
		html.append("<div style='display:flex;align-items:center;gap:10px;'>");

		// Nomor langkah dalam lingkaran
		html.append("<div style='min-width:34px;width:34px;height:34px;line-height:34px;text-align:center;"
				+ "border-radius:50%;background:rgba(255,255,255,.22);color:#fff;font-weight:900;"
				+ "font-size:15px;flex-shrink:0;'>").append(nomor).append("</div>");

		html.append("<div style='flex:1;min-width:0;'>");

		// Judul langkah
		html.append("<div class='sopafc-title' style='font-size:13px;font-weight:900;color:#fff;"
				+ "line-height:1.35;word-break:break-word;'>");
		html.append(escapeHtml(safe(alur.getKode(), "?")))
				.append(" &mdash; ")
				.append(escapeHtml(safe(alur.getNama(), "Langkah")));
		html.append("</div>");

		// Badge-badge status
		html.append("<div style='margin-top:5px;display:flex;flex-wrap:wrap;gap:3px;'>");
		html.append("<span class='sopafc-badge' style='background:rgba(255,255,255,.22);color:#fff;'>")
				.append(escapeHtml(statusLabel)).append("</span>");
		if (alur.getJikaProsesDisetujuiMakaSelesai()) {
			html.append("<span class='sopafc-badge' style='background:#fef3c7;color:#92400e;'>"
					+ "Selesai jika disetujui</span>");
		}
		if (alur.getPenolakanAdaDiSini()) {
			html.append("<span class='sopafc-badge' style='background:#fee2e2;color:#991b1b;'>"
					+ "Titik penolakan</span>");
		}
		if (hasBranch) {
			html.append("<span class='sopafc-badge' style='background:#fde68a;color:#78350f;'>"
					+ "Ada percabangan</span>");
		}
		html.append("</div>"); // badges
		html.append("</div>"); // title wrapper
		html.append("</div>"); // flex row
		html.append("</div>"); // header

		// ── Body ───────────────────────────────────────────────────────────
		html.append("<div class='sopafc-body'>");

		// Seksi Aktor
		html.append("<div class='sopafc-section sopafc-sec-actor'>");
		html.append("<div style='font-size:20px;line-height:1;flex-shrink:0;'>&#128101;</div>");
		html.append("<div style='min-width:0;'>");
		html.append("<div class='sopafc-label'>").append(escapeHtml(actorLabel)).append("</div>");
		html.append("<div class='sopafc-val'>").append(buildActorTextFast(alur)).append("</div>");
		html.append("</div></div>");

		// Seksi Durasi
		html.append("<div class='sopafc-section sopafc-sec-time'>");
		html.append("<div style='font-size:18px;line-height:1;flex-shrink:0;'>&#9200;</div>");
		html.append("<div class='sopafc-val' style='color:#0369a1;'>"
				+ "<b>").append(alur.getJangkaWaktu()).append(" hari</b> waktu penyelesaian</div>");
		html.append("</div>");

		// Seksi Dokumen
		html.append(buildDocumentSectionHtml(dokumens));

		// Seksi Langkah Berikutnya / Percabangan
		html.append(buildBranchSectionHtml(alur));

		html.append("</div>"); // body

		return html.toString();
	}

	/**
	 * Menghasilkan HTML seksi daftar dokumen dalam kartu langkah bagan SOP.
	 *
	 * <p>Setiap dokumen ditampilkan dengan label "Wajib" (merah) atau "Opsional" (biru).
	 * Jika tidak ada dokumen, seksi tetap ditampilkan dengan keterangan kosong agar
	 * pengguna tidak mengira ada kesalahan tampilan.</p>
	 *
	 * <p>Data diambil dari parameter {@code dokumens} yang sudah di-load sebelumnya
	 * oleh {@link #ambilDokumenMapUntukDiagram(List)} — tidak ada query baru di sini.</p>
	 *
	 * @param dokumens daftar dokumen; boleh null atau kosong
	 * @return String HTML seksi dokumen
	 */
	private String buildDocumentSectionHtml(List<DiagramDokumenInfo> dokumens) {
		StringBuilder html = new StringBuilder();
		html.append("<div class='sopafc-section sopafc-sec-doc'>");
		html.append("<div style='font-size:18px;line-height:1;flex-shrink:0;'>&#128196;</div>");
		html.append("<div style='min-width:0;flex:1;'>");
		html.append("<div class='sopafc-label'>DOKUMEN YANG DIPERLUKAN</div>");

		if (dokumens == null || dokumens.isEmpty()) {
			html.append("<div class='sopafc-val' style='color:#64748b;font-style:italic;'>"
					+ "Tidak ada dokumen khusus pada tahap ini.</div>");
		} else {
			for (DiagramDokumenInfo d : dokumens) {
				if (d == null) continue;
				String wajibStyle = d.wajib
						? "background:#fee2e2;color:#991b1b;"
						: "background:#e0f2fe;color:#075985;";
				html.append("<div style='font-size:11px;color:#374151;line-height:1.5;margin-top:3px;'>");
				html.append("&bull; <b>").append(escapeHtml(d.kode)).append("</b> ")
						.append(escapeHtml(d.nama));
				html.append(" <span class='sopafc-badge' style='font-size:9px;")
						.append(wajibStyle).append("'>")
						.append(d.wajib ? "Wajib" : "Opsional").append("</span>");
				html.append("</div>");
			}
		}
		html.append("</div></div>");
		return html.toString();
	}

	/**
	 * Menghasilkan HTML seksi percabangan dan info langkah berikutnya pada kartu bagan.
	 *
	 * <p>Tiga kondisi yang ditangani:</p>
	 * <ol>
	 *   <li><b>Tidak ada langkah berikutnya</b> — tampil tanda merah "Akhir proses".</li>
	 *   <li><b>Satu langkah berikutnya</b> — tampil panah dan nama langkah tujuan.</li>
	 *   <li><b>Lebih dari satu</b> — percabangan. Tiap opsi tampil dalam kotak berwarna
	 *       berbeda dengan keterangan kondisi yang memicu cabang tersebut.</li>
	 * </ol>
	 *
	 * <p>Semua teks di-escape melalui {@link #escapeHtml(Object)} untuk keamanan XSS.</p>
	 *
	 * @param alur langkah alur SOP
	 * @return String HTML seksi percabangan
	 */
	private String buildBranchSectionHtml(AlurSop alur) {
		if (alur == null) return "";

		List<AlurSop> nexts = null;
		List<String> opsiList = null;
		try {
			nexts = alur.ambilAlurSetelahnya();
			opsiList = alur.ambilOpsiAlurSetelahnya();
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sop/AlurSopAction.java:3090"); /* field bisa throw jika entity detached */ }

		StringBuilder html = new StringBuilder();
		html.append("<div class='sopafc-section sopafc-sec-branch'>");
		html.append("<div style='font-size:18px;line-height:1;flex-shrink:0;'>&#8644;</div>");
		html.append("<div style='min-width:0;flex:1;'>");
		html.append("<div class='sopafc-label'>LANGKAH BERIKUTNYA</div>");

		if (nexts == null || nexts.isEmpty()) {
			// Akhir proses
			html.append("<div style='font-size:12px;color:#475569;margin-top:3px;"
					+ "display:flex;align-items:center;gap:5px;'>"
					+ "<span style='color:#dc2626;font-size:14px;'>&#9673;</span>"
					+ "<span>Akhir proses &mdash; tidak ada langkah selanjutnya.</span></div>");
		} else if (nexts.size() == 1) {
			// Alur lurus (linear)
			AlurSop n = nexts.get(0);
			if (n != null) {
				html.append("<div style='font-size:12px;color:#166534;margin-top:3px;"
						+ "display:flex;align-items:center;gap:5px;'>"
						+ "<span style='font-size:14px;'>&#8594;</span>"
						+ "<span><b>").append(escapeHtml(n.getKode())).append("</b> &mdash; ")
						.append(escapeHtml(n.getNama())).append("</span></div>");
			}
		} else {
			// Percabangan — ada beberapa kemungkinan arah
			html.append("<div style='font-size:11px;color:#92400e;margin-top:3px;margin-bottom:5px;'>"
					+ "Arah proses ditentukan oleh pilihan petugas saat disposisi:</div>");
			final String[] branchBg = {
				"#dcfce7","#dbeafe","#fce7f3","#fef3c7","#e0f2fe",
				"#f3e8ff","#fef2f2","#ecfdf5","#fff7ed","#f0fdf4"
			};
			final String[] branchFg = {
				"#166534","#1e40af","#831843","#92400e","#0c4a6e",
				"#6b21a8","#991b1b","#065f46","#9a3412","#15803d"
			};
			for (int i = 0; i < nexts.size(); i++) {
				AlurSop n = nexts.get(i);
				if (n == null) continue;
				String opsiText = (opsiList != null && i < opsiList.size() && opsiList.get(i) != null)
						? opsiList.get(i).trim() : "";
				int ci = i % branchBg.length;
				html.append("<div style='margin-top:4px;padding:5px 8px;border-radius:8px;"
						+ "background:").append(branchBg[ci]).append(";"
						+ "border-left:3px solid ").append(branchFg[ci]).append(";'>");
				html.append("<span style='font-size:11px;font-weight:900;color:")
						.append(branchFg[ci]).append(";'>");
				if (opsiText.length() > 0) {
					html.append("&#10003; Jika &ldquo;").append(escapeHtml(opsiText))
							.append("&rdquo; &rarr; ");
				} else {
					html.append("&#8594; Pilihan ").append(i + 1).append(": ");
				}
				html.append("<b>").append(escapeHtml(n.getKode())).append("</b> &mdash; ")
						.append(escapeHtml(n.getNama())).append("</span></div>");
			}
		}

		if (alur.getAlurSetelahnyaTidakWajib()) {
			html.append("<div style='font-size:10px;color:#64748b;margin-top:5px;font-style:italic;'>"
					+ "* Pemilihan langkah berikutnya tidak diwajibkan.</div>");
		}

		html.append("</div></div>");
		return html.toString();
	}

	/**
	 * Menghasilkan HTML legenda di bagian bawah bagan yang menjelaskan arti warna
	 * dan simbol dalam diagram kepada pengguna.
	 *
	 * @return String HTML legenda
	 */
	private String buildDiagramLegendHtml() {
		return "<div style='margin-top:16px;padding:12px 14px;border-radius:14px;background:#ffffff;"
			+ "border:1px solid #e2e8f0;font-family:Arial,sans-serif;font-size:11px;color:#475569;'>"
			+ "<div style='font-size:12px;font-weight:900;color:#334155;margin-bottom:8px;'>"
			+ "Keterangan Simbol &amp; Warna</div>"
			+ "<div style='display:flex;flex-wrap:wrap;gap:12px;align-items:center;'>"
			+ "<div style='display:flex;align-items:center;gap:5px;'>"
			+ "<div style='width:14px;height:14px;border-radius:3px;background:#1e40af;'></div>"
			+ "<span>Awal Pengajuan</span></div>"
			+ "<div style='display:flex;align-items:center;gap:5px;'>"
			+ "<div style='width:14px;height:14px;border-radius:3px;background:#15803d;'></div>"
			+ "<span>Langkah Aktif</span></div>"
			+ "<div style='display:flex;align-items:center;gap:5px;'>"
			+ "<div style='width:14px;height:14px;border-radius:3px;background:#475569;'></div>"
			+ "<span>Langkah Nonaktif</span></div>"
			+ "<div style='display:flex;align-items:center;gap:5px;'>"
			+ "<div style='width:14px;height:14px;border-radius:3px;"
			+ "background:#fee2e2;border:1px solid #f87171;'></div>"
			+ "<span>Dokumen Wajib</span></div>"
			+ "<div style='display:flex;align-items:center;gap:5px;'>"
			+ "<div style='width:14px;height:14px;border-radius:3px;"
			+ "background:#e0f2fe;border:1px solid #7dd3fc;'></div>"
			+ "<span>Dokumen Opsional</span></div>"
			+ "<div style='display:flex;align-items:center;gap:5px;'>"
			+ "<span style='color:#dc2626;font-size:14px;'>&#9654;</span><span>Mulai Proses</span></div>"
			+ "<div style='display:flex;align-items:center;gap:5px;'>"
			+ "<span style='color:#dc2626;font-size:14px;'>&#9632;</span><span>Akhir Proses</span></div>"
			+ "</div></div>";
	}

	/**
	 * Memeriksa apakah langkah alur SOP ini memiliki setidaknya satu langkah berikutnya.
	 * Digunakan untuk menentukan apakah panah koneksi perlu ditampilkan.
	 *
	 * @param alur langkah yang diperiksa; boleh null
	 * @return true jika ada langkah berikutnya, false jika tidak ada atau terjadi error
	 */
	private boolean hasNextDiagram(AlurSop alur) {
		try {
			return alur != null && alur.ambilAlurSetelahnya() != null
					&& !alur.ambilAlurSetelahnya().isEmpty();
		} catch (Exception e) {
			return false;
		}
	}

	private String buildActorTextFast(AlurSop alur) {
		if (alur == null) {
			return "Aktor belum ditentukan";
		}
		StringBuilder sb = new StringBuilder();
		try {
			if (alur.getAktorSop() != null && hasText(alur.getAktorSop().getNama())) {
				sb.append("<b>").append(escapeHtml(alur.getAktorSop().getNama())).append("</b>");
			}
			if (hasText(alur.getAktor())) {
				if (sb.length() > 0) {
					sb.append("<br/>");
				}
				sb.append(escapeHtml(alur.getAktor()));
			}
			String khusus = alur.getKhususUsername();
			if (hasText(khusus)) {
				if (sb.length() > 0) {
					sb.append("<br/>");
				}
				sb.append("Pengguna: ").append(escapeHtml(ringkasDaftar(khusus)));
			}
			if (alur.getAktorSop() != null && hasText(alur.getAktorSop().getJenisPengguna())) {
				if (sb.length() > 0) {
					sb.append("<br/>");
				}
				sb.append("Role: ").append(escapeHtml(ringkasDaftar(alur.getAktorSop().getJenisPengguna())));
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		return sb.length() == 0 ? "Aktor belum ditentukan" : sb.toString();
	}

	private String ringkasDaftar(String text) {
		if (text == null) {
			return "";
		}
		String cleaned = text.replace(';', ',').replace("\n", ",").replace("\r", ",");
		while (cleaned.indexOf(",,") >= 0) {
			cleaned = cleaned.replace(",,", ",");
		}
		cleaned = cleaned.replaceAll("^,+", "").replaceAll(",+$", "").trim();
		return cleaned.length() == 0 ? "" : cleaned;
	}


	private void appendFallbackActionButtons(Component card, final AlurSop alur) {
		if (card == null || alur == null) {
			return;
		}
		Hbox actions = new Hbox();
		actions.setParent(card);
		actions.setStyle("margin-top:8px;border-top:1px dashed #cbd5e1;padding-top:7px;");
		try {
			actions.setSpacing("6px");
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		if (edit) {
			MyToolbarbuttonConfig editButton = new MyToolbarbuttonConfig("Edit", "/img/svg/edit-box-line.svg");
			editButton.setParent(actions);
			editButton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(alur);
				}
			});
		}
		if (delete) {
			MyToolbarbuttonConfig deleteButton = new MyToolbarbuttonConfig("Delete", "/img/svg/trash.svg");
			deleteButton.setParent(actions);
			deleteButton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Anda yakin ingin menghapus alur SOP ini?", "Konfirmasi",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									try {
										if (Integer.parseInt(arg0.getData().toString()) == MyMessageboxConfig.OK) {
											Session sessionDelete = null;
											try {
												sessionDelete = HibernateUtil.currentSession();
												sessionDelete.delete(alur);
												sessionDelete.flush();
											} finally {
												// currentSession() tidak ditutup manual karena dikelola request/thread aplikasi.
											}
											onSearchDefault(null);
										}
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}
								}
							});
				}
			});
		}
	}


	private void appendDiagramStep(StringBuilder html, AlurSop alur, int nomor) {
		String nama = safe(alur == null ? null : alur.getNama(), "Langkah");
		String kode = safe(alur == null ? null : alur.getKode(), "-");
		String aktorText = safe(alur == null ? null : alur.getAktor(), "Petugas belum ditentukan");
		boolean aktif = alur == null || alur.getAktif();
		boolean startData = alur != null && alur.getStart();
		String bg = aktif ? (startData ? "#eff6ff" : "#ffffff") : "#f1f5f9";
		String border = aktif ? (startData ? "#bfdbfe" : "#d6dce5") : "#cbd5e1";
		String badgeBg = aktif ? (startData ? "#dbeafe" : "#dcfce7") : "#e5e7eb";
		String badgeColor = aktif ? (startData ? "#1d4ed8" : "#166534") : "#374151";
		String status = aktif ? (startData ? "Awal Pengajuan" : "Aktif") : "Nonaktif";

		html.append("<div style='flex:1 1 30%;max-width:32%;min-width:210px;box-sizing:border-box;"
				+ "border-radius:14px;padding:11px 12px;background:").append(bg).append(";border:1px solid ").append(border)
				.append(";color:#0f172a;word-break:break-word;overflow-wrap:break-word;'>");
		html.append("<div style='display:flex;gap:8px;align-items:flex-start;'>");
		html.append("<span style='display:inline-block;min-width:28px;width:28px;height:28px;line-height:28px;text-align:center;"
				+ "border-radius:999px;background:#0f172a;color:#ffffff;font-weight:900;'>").append(nomor).append("</span>");
		html.append("<div style='min-width:0;width:100%;'>");
		html.append("<div style='font-size:12px;font-weight:900;color:#0f172a;'>").append(escapeHtml(kode)).append(" - ")
				.append(escapeHtml(nama)).append("</div>");
		html.append("<div style='margin-top:5px;'><span style='font-size:10px;font-weight:800;padding:3px 8px;border-radius:999px;background:")
				.append(badgeBg).append(";color:").append(badgeColor).append(";'>").append(escapeHtml(status)).append("</span></div>");
		html.append("<div style='font-size:11px;color:#475569;margin-top:7px;'>👤 ").append(escapeHtml(aktorText)).append("</div>");
		if (alur != null) {
			html.append("<div style='font-size:11px;color:#64748b;margin-top:7px;'>⏱ Jangka waktu: ")
					.append(alur.getJangkaWaktu()).append(" hari</div>");
			String cabang = buildNextSummary(alur);
			if (cabang.length() > 0) {
				html.append("<div style='font-size:11px;color:#334155;margin-top:7px;line-height:1.45;'>➡ Setelah ini: ")
						.append(cabang).append("</div>");
			} else {
				html.append("<div style='font-size:11px;color:#64748b;margin-top:7px;'>🏁 Tahap akhir / tidak ada langkah berikutnya.</div>");
			}
		}
		html.append("</div></div></div>");
	}

	private String buildNextSummary(AlurSop alur) {
		if (alur == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		try {
			List<AlurSop> nexts = alur.ambilAlurSetelahnya();
			List<String> opsi = alur.ambilOpsiAlurSetelahnya();
			for (int i = 0; nexts != null && i < nexts.size(); i++) {
				AlurSop n = nexts.get(i);
				if (n == null) {
					continue;
				}
				String opsiText = opsi != null && i < opsi.size() && opsi.get(i) != null ? opsi.get(i).trim() : "";
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append("<b>").append(escapeHtml(n.getKode())).append("</b>");
				if (opsiText.length() > 0) {
					sb.append(" <span style='color:#92400e;'>(").append(escapeHtml(opsiText)).append(")</span>");
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		return sb.toString();
	}

	private boolean hasText(String value) {
		return value != null && value.trim().length() > 0;
	}

	private String buildDiagramStat(String label, String value, String bg, String color) {
		return "<div style='min-width:90px;background:" + bg + ";color:" + color
				+ ";border:1px solid #d6dce5;border-radius:12px;padding:7px 10px;text-align:center;'>"
				+ "<div style='font-size:17px;font-weight:900;'>" + escapeHtml(value) + "</div>"
				+ "<div style='font-size:10px;font-weight:700;'>" + escapeHtml(label) + "</div></div>";
	}

	private void appendDiagramInfo(Component parent, String message) {
		new Html("<div style='margin:0 0 10px 0;padding:11px 13px;border-radius:14px;background:#f8fafc;"
				+ "border:1px solid #e2e8f0;color:#475569;font-size:12px;line-height:1.55;'>"
				+ escapeHtml(message) + "</div>").setParent(parent);
	}

	private String safe(String value, String fallback) {
		return value == null || value.trim().length() == 0 ? fallback : value.trim();
	}

	private String escapeHtml(Object value) {
		if (value == null) {
			return "";
		}
		String s = String.valueOf(value);
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
	}

	public static void reloadDataMenu(final Row rowU, final JSONArray array) throws Exception {
		Common.clear(rowU);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(rowU);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Nama Proses");
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig("Menu");
		column.setParent(columns);
		column.setWidth("40%");

		column = new MyColumnConfig("Parameter");
		column.setParent(columns);
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		Rows rows = new Rows();
		rows.setParent(grid);

		for (int i = 0; i < array.length(); i++) {
			final int index = i;
			final JSONObject jsonObject = array.getJSONObject(i);

			Long menu = null;
			String param = "";
			if (!jsonObject.isNull("param")) {
				param = jsonObject.getString("param");
			}
			String nama = "";
			if (!jsonObject.isNull("nama")) {
				nama = jsonObject.getString("nama");
			}
			if (!jsonObject.isNull("menu")) {
				menu = ais.common.CommonJSONUtil.ambilLong(jsonObject,"menu");
			}

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);

			Menu myMenu = (Menu) (menu == null ? null : ConstantValues.ambil(Menu.class.getName(), menu));

			final Textbox namaTextbox = new Textbox(nama);
			namaTextbox.setWidth("90%");
			row.appendChild(namaTextbox);

			final AmbilDataMenuBanbox menuBanbox = new AmbilDataMenuBanbox(myMenu);
			menuBanbox.setWidth("90%");
			row.appendChild(menuBanbox);
			final Textbox parameter = new Textbox(param);
			parameter.setWidth("90%");
			row.appendChild(parameter);

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Menu menu = (Menu) menuBanbox.getAttribute("menu");
					jsonObject.put("menu", menu.getId());
					String param = parameter.getValue() == null ? "" : parameter.getValue();
					jsonObject.put("param", param);
					String nama = namaTextbox.getValue() == null ? "" : namaTextbox.getValue();
					jsonObject.put("nama", nama);
				}
			};

			parameter.addEventListener("onChange", eventListener);
			namaTextbox.addEventListener("onChange", eventListener);
			menuBanbox.setEventListener(eventListener);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											array.put(index, new JSONObject());

											reloadDataMenu(rowU, array);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			button.setParent(row);

		}
	}

	public static void reloadMenu(final Row rowMenu, final JSONArray array) throws Exception {
		final MyFormRow rowU = new MyFormRow();

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Menu", "/img/svg/addthis.svg");
		button.setTooltiptext("Hapus Data");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				JSONObject jsonObject = new JSONObject();
				array.put(jsonObject);

				reloadDataMenu(rowU, array);
			}
		});
		button.setParent(rowMenu);

		rowU.setParent(rowMenu.getParent());

		reloadDataMenu(rowU, array);

	}
}
