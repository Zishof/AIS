package ais.action.master.surat;


import ais.common.CommonSearchFilterHelper;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Longbox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.database.model.surat.KelompokNomorSurat;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class NomorSuratAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 *  
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;
	private Checkbox searchaktif;
	private Textbox searchnama;
	private Textbox nama;
	private Longbox mulaiUrutanKe;
	private Combobox kolom1;
	private Textbox tanda1;
	private Combobox kolom2;
	private Textbox tanda2;
	private Combobox kolom3;
	private Textbox tanda3;
	private Combobox kolom4;
	private Textbox tanda4;
	private Combobox kolom5;
	private Textbox tanda5;
	private Combobox kolom6;
	private Textbox tanda6;
	private Combobox kolom7;
	private Textbox tanda7;
	private Combobox kolom8;
	private Textbox tanda8;
	private Combobox kolom9;
	private Textbox tanda9;
	private Combobox kolom10;
	private Textbox tanda10;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private NomorSurat nomorSurat;
	private MyToolbarbuttonConfig add;
	private MyCheckboxConfig resetUrutanTiapTahun;
	private MyCheckboxConfig urutBerdasarkanKelompok;
	private MyCheckboxConfig urutBerdasarkanNomor;
	private Intbox jumlahAngkaNolDiDepanNomorUrut;
	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox kelompokNomorSurat;

	protected Combobox searchfakultas;
	protected Combobox searchjurusan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private boolean pt = false;
	private boolean ya = false;
	private Row hbFakultasLabel;
	private Row hbYayasan;
	private Combobox yayasan;
	private Combobox sekolah;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private MyCheckboxConfig resetUrutanTiapBulan;
	private MyDatebox resetTiap;
	private AmbilDataSatuanKerjaBanbox searchparent;

	private Tabpanel variableSuratKeluar;

	public void onVariableSurat(Event event) {
		if (variableSuratKeluar.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(variableSuratKeluar);
			MyInclude iframe = new MyInclude("/pages/master/surat/variable_surat_keluar.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel kelompokManajemenNomorSurat;

	public void onKelompokNomorSurat(Event event) {
		if (kelompokManajemenNomorSurat.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(kelompokManajemenNomorSurat);
			MyInclude iframe = new MyInclude("/pages/master/surat/kelompok_nomor_surat.zul");
			iframe.setParent(window);
		}
	}

	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private MyCheckboxConfig gunakanIndexUrut;
	private Longbox nomorIndex;

	private String tipe = "surat";

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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (execution.getParameter("tipe") != null && !execution.getParameter("tipe").trim().isEmpty()) {
			tipe = execution.getParameter("tipe").trim();
		}

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah, true, false);

		if (hbFakultasLabel != null) { hbFakultasLabel.setVisible(pt && searchfakultas.getChildren().size() > 1); }
		if (hbYayasan != null) { hbYayasan.setVisible(ya); }

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

		String[] contents = new String[] { "id", "nama", "contohFormat", "resetUrutanTiapTahun", "resetUrutanTiapBulan",
				"resetTiapBulan", "resetTiapTanggal", "urutBerdasarkanKelompok", "urutBerdasarkanNomor",
				"mulaiUrutanKe", "jumlahAngkaNolDiDepanNomorUrut", "jurusan", "fakultas", "yayasan", "sekolah",
				"satuanKerja", "kelompokNomorSurat", "kolom1", "tanda1", "kolom2", "tanda2", "kolom3", "tanda3",
				"kolom4", "tanda4", "kolom5", "tanda5", "kolom6", "tanda6", "kolom7", "tanda7", "kolom8", "tanda8",
				"kolom9", "tanda9", "kolom10", "tanda10", "tipe", "keterangan" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, NomorSurat.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	        FilterLanjutHelper.setup(comp);
}

	private void initKolom(Combobox combobox) {
		String[] data = new String[] { NomorSurat.KOSONG, NomorSurat.NOMOR_URUT, NomorSurat.KATA_STATIS,
				NomorSurat.TANGGAL, NomorSurat.BULAN_ROMAWI, NomorSurat.BULAN, NomorSurat.TAHUN };
		for (String s : data) {
			MyComboitemConfig comboitem = new MyComboitemConfig(s);
			comboitem.setValue(s);
			combobox.appendChild(comboitem);
		}
	}

	class NomorSuratRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final NomorSurat nomorSurat = (NomorSurat) arg1;

			if (nomorSurat.getTipe() == null) {
				nomorSurat.setTipe(tipe);
				Common.refreshUpdate(nomorSurat);
			}

			RevisiHelper.createNewRevisi(NomorSurat.class, nomorSurat, nomorSurat.getNama()).setParent(arg0);
			new Label(nomorSurat.getContohFormat()).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			vbox.appendChild(
					new Label(nomorSurat.getSatuanKerja() == null ? "" : nomorSurat.getSatuanKerja().getNama()));
			Hbox hbox = new Hbox();
			hbox.setParent(vbox);

			new Label(nomorSurat.getFakultas() == null ? "" : nomorSurat.getFakultas().getNama()).setParent(hbox);
			new Label(nomorSurat.getJurusan() == null ? "" : nomorSurat.getJurusan().getNama()).setParent(hbox);

			hbox = new Hbox();
			hbox.setParent(vbox);

			new Label(nomorSurat.getYayasan() == null ? "" : nomorSurat.getYayasan().getNama()).setParent(hbox);
			new Label(nomorSurat.getSekolah() == null ? "" : nomorSurat.getSekolah().getNama()).setParent(hbox);

			new Label(nomorSurat.getKelompokNomorSurat() == null ? "" : nomorSurat.getKelompokNomorSurat().getNama())
					.setParent(arg0);
			new Label(nomorSurat.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(nomorSurat.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					nomorSurat.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(nomorSurat);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, nomorSurat, NomorSuratAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new NomorSurat());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(NomorSurat nomorSurat) throws Exception {
		this.nomorSurat = nomorSurat;
		addWindow.setTitle(nomorSurat.getId() == null ? "Tambah Nomor Surat" : "Ubah Nomor Surat");
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
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Nomor Surat"));
		row.appendChild(nama = new Textbox(nomorSurat.getNama() == null ? "" : nomorSurat.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai Urutan Ke"));
		row.appendChild(mulaiUrutanKe = new Longbox(nomorSurat.getMulaiUrutanKe()));
		mulaiUrutanKe.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Urutankan nomor surat menggunakan indeks"));
		row.appendChild(gunakanIndexUrut = new MyCheckboxConfig());
		gunakanIndexUrut.setChecked(nomorSurat.getGunakanIndexUrut());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Saat ini indeks ke"));
		row.appendChild(nomorIndex = new Longbox(nomorSurat.getNomorIndex()));
		nomorIndex.setWidth("90%");

		EventListener eventListenerIndex = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				nomorIndex.getParent().setVisible(gunakanIndexUrut.isChecked());
			}
		};

		eventListenerIndex.onEvent(null);
		gunakanIndexUrut.addEventListener("onClick", eventListenerIndex);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Karakter Nomor Urutan"));
		row.appendChild(jumlahAngkaNolDiDepanNomorUrut = new Intbox(nomorSurat.getJumlahAngkaNolDiDepanNomorUrut()));
		jumlahAngkaNolDiDepanNomorUrut.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Urutan kembali ke-awal tiap ganti tahun"));
		row.appendChild(resetUrutanTiapTahun = new MyCheckboxConfig());
		resetUrutanTiapTahun.setChecked(nomorSurat.getResetUrutanTiapTahun());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Urutan kembali ke-awal tiap ganti bulan"));
		row.appendChild(resetUrutanTiapBulan = new MyCheckboxConfig());
		resetUrutanTiapBulan.setChecked(nomorSurat.getResetUrutanTiapBulan());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Urutan kembali ke-awal saat tanggal"));
		row.appendChild(resetTiap = new MyDatebox(nomorSurat.getResetTiap()));
		resetTiap.setCols(6);

		Tbmuser tbmuser = Common.getCurrentUser();
		if (nomorSurat.getFakultas() == null && tbmuser.ambilFakultas() != null) {
			nomorSurat.setFakultas(tbmuser.ambilFakultas());
		}

		if (nomorSurat.getSatuanKerja() == null && tbmuser.ambilSatuanKerja() != null) {
			nomorSurat.setSatuanKerja(tbmuser.ambilSatuanKerja());
		}

		if (nomorSurat.getJurusan() == null && tbmuser.ambilJurusan() != null) {
			nomorSurat.setJurusan(tbmuser.ambilJurusan());
		}

		if (nomorSurat.getYayasan() == null && tbmuser.ambilYayasan() != null) {
			nomorSurat.setYayasan(tbmuser.ambilYayasan());
		}

		if (nomorSurat.getSekolah() == null && tbmuser.ambilSekolah() != null) {
			nomorSurat.setSekolah(tbmuser.ambilSekolah());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(nomorSurat.getSatuanKerja() == null ? "" : nomorSurat.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", nomorSurat.getSatuanKerja());
		row.appendChild(satuanKerja);
		satuanKerja.setWidth("90%");

		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		MyFormRow rowFakultas = new MyFormRow();
		rowFakultas.setVisible(pt);
		rowFakultas.setStyle("border:0px;background: transparent;");
		rowFakultas.setParent(rows);
		rowFakultas.appendChild(new MyLabelConfig("Fakultas"));
		rowFakultas.appendChild(fakultas);
		Common.selectComboItem(fakultas, nomorSurat.getFakultas());
		fakultas.setWidth("90%");

		Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("fakultas",
						nomorSurat.getFakultas() == null ? tbmuser.ambilFakultas() : nomorSurat.getFakultas()));

		MyFormRow rowJurusan = new MyFormRow();
		rowJurusan.setVisible(pt);
		rowJurusan.setStyle("border:0px;background: transparent;");
		rowJurusan.setParent(rows);
		rowJurusan.appendChild(new MyLabelConfig("Jurusan"));
		rowJurusan.appendChild(jurusan);
		jurusan.setWidth("90%");
		Common.pilihJurusan(jurusan, nomorSurat.getJurusan());

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));

		Common.selectComboItem(yayasan, nomorSurat == null || nomorSurat.getYayasan() == null ? tbmuser.ambilYayasan()
				: nomorSurat.getYayasan());
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));

		Common.pilihSekolah(sekolah, nomorSurat == null || nomorSurat.getSekolah() == null ? tbmuser.ambilSekolah()
				: nomorSurat.getSekolah());
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelompok Nomor Surat"));
		row.appendChild(kelompokNomorSurat = new Combobox());
		kelompokNomorSurat.setWidth("90%");
		Common.insertComboDanSemua(kelompokNomorSurat, "nama", KelompokNomorSurat.class);
		Common.selectComboItem(kelompokNomorSurat, nomorSurat.getKelompokNomorSurat());
		kelompokNomorSurat.setReadonly(true);
		KelompokNomorSuratAction.checkKelompok(kelompokNomorSurat);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Urutankan nomor berdasar kelompok"));
		row.appendChild(urutBerdasarkanKelompok = new MyCheckboxConfig());
		urutBerdasarkanKelompok.setChecked(nomorSurat.getUrutBerdasarkanKelompok());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Urutankan nomor berdasar Nomor Surat"));
		row.appendChild(urutBerdasarkanNomor = new MyCheckboxConfig());
		urutBerdasarkanNomor.setChecked(nomorSurat.getUrutBerdasarkanNomor());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kolom ke-1"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(kolom1 = new Combobox());
		initKolom(kolom1);
		Common.selectComboItem(kolom1, nomorSurat.getKolom1());
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("di-akhiri tanda")));
		hbox.appendChild(tanda1 = new Textbox(nomorSurat.getTanda1()));
		kolom1.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kolom ke-2"));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(kolom2 = new Combobox());
		initKolom(kolom2);
		Common.selectComboItem(kolom2, nomorSurat.getKolom2());
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("di-akhiri tanda")));
		hbox.appendChild(tanda2 = new Textbox(nomorSurat.getTanda2()));
		kolom2.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kolom ke-3"));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(kolom3 = new Combobox());
		initKolom(kolom3);
		Common.selectComboItem(kolom3, nomorSurat.getKolom3());
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("di-akhiri tanda")));
		hbox.appendChild(tanda3 = new Textbox(nomorSurat.getTanda3()));
		kolom3.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kolom ke-4"));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(kolom4 = new Combobox());
		initKolom(kolom4);
		Common.selectComboItem(kolom4, nomorSurat.getKolom4());
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("di-akhiri tanda")));
		hbox.appendChild(tanda4 = new Textbox(nomorSurat.getTanda4()));
		kolom4.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kolom ke-5"));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(kolom5 = new Combobox());
		initKolom(kolom5);
		Common.selectComboItem(kolom5, nomorSurat.getKolom5());
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("di-akhiri tanda")));
		hbox.appendChild(tanda5 = new Textbox(nomorSurat.getTanda5()));
		kolom5.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kolom ke-6"));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(kolom6 = new Combobox());
		initKolom(kolom6);
		Common.selectComboItem(kolom6, nomorSurat.getKolom6());
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("di-akhiri tanda")));
		hbox.appendChild(tanda6 = new Textbox(nomorSurat.getTanda6()));
		kolom6.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kolom ke-7"));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(kolom7 = new Combobox());
		initKolom(kolom7);
		Common.selectComboItem(kolom7, nomorSurat.getKolom7());
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("di-akhiri tanda")));
		hbox.appendChild(tanda7 = new Textbox(nomorSurat.getTanda7()));
		kolom7.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kolom ke-8"));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(kolom8 = new Combobox());
		initKolom(kolom8);
		Common.selectComboItem(kolom8, nomorSurat.getKolom8());
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("di-akhiri tanda")));
		hbox.appendChild(tanda8 = new Textbox(nomorSurat.getTanda8()));
		kolom8.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kolom ke-9"));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(kolom9 = new Combobox());
		initKolom(kolom9);
		Common.selectComboItem(kolom9, nomorSurat.getKolom9());
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("di-akhiri tanda")));
		hbox.appendChild(tanda9 = new Textbox(nomorSurat.getTanda9()));
		kolom9.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kolom ke-10"));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(kolom10 = new Combobox());
		initKolom(kolom10);
		Common.selectComboItem(kolom10, nomorSurat.getKolom10());
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("di-akhiri tanda")));
		hbox.appendChild(tanda10 = new Textbox(nomorSurat.getTanda10()));
		kolom10.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(nomorSurat.getKeterangan() == null ? "" : nomorSurat.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Nomor Surat belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Nama Nomor Surat; (2) isikan nama format nomor surat secara lengkap; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaNomorSurat();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, Nama Nomor Surat sudah ada di database. Langkah yang dapat dilakukan: (1) periksa daftar nomor surat yang sudah ada; (2) gunakan nama yang berbeda dan belum terdaftar; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (nomorSurat.getId() != null) {
			nomorSurat = (NomorSurat) session.load(NomorSurat.class, nomorSurat.getId());

		}

		nomorSurat.setGunakanIndexUrut(gunakanIndexUrut.isChecked());
		nomorSurat.setNomorIndex(nomorIndex.getValue());

		nomorSurat.setKelompokNomorSurat((KelompokNomorSurat) (kelompokNomorSurat.getSelectedItem() == null ? null
				: kelompokNomorSurat.getSelectedItem().getValue()));
		nomorSurat.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		nomorSurat.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		nomorSurat.setNama(nama.getValue());
		nomorSurat.setMulaiUrutanKe(mulaiUrutanKe.getValue());
		nomorSurat.setJumlahAngkaNolDiDepanNomorUrut(jumlahAngkaNolDiDepanNomorUrut.getValue());
		nomorSurat.setResetUrutanTiapTahun(resetUrutanTiapTahun.isChecked());
		nomorSurat.setResetUrutanTiapBulan(resetUrutanTiapBulan.isChecked());
		nomorSurat.setUrutBerdasarkanKelompok(urutBerdasarkanKelompok.isChecked());

		nomorSurat.setKolom1((String) kolom1.getSelectedItem().getValue());
		nomorSurat.setTanda1(tanda1.getValue().trim());

		nomorSurat.setKolom2((String) kolom2.getSelectedItem().getValue());
		nomorSurat.setTanda2(tanda2.getValue().trim());

		nomorSurat.setKolom3((String) kolom3.getSelectedItem().getValue());
		nomorSurat.setTanda3(tanda3.getValue().trim());

		nomorSurat.setKolom4((String) kolom4.getSelectedItem().getValue());
		nomorSurat.setTanda4(tanda4.getValue().trim());

		nomorSurat.setKolom5((String) kolom5.getSelectedItem().getValue());
		nomorSurat.setTanda5(tanda5.getValue().trim());

		nomorSurat.setKolom6((String) kolom6.getSelectedItem().getValue());
		nomorSurat.setTanda6(tanda6.getValue().trim());

		nomorSurat.setKolom7((String) kolom7.getSelectedItem().getValue());
		nomorSurat.setTanda7(tanda7.getValue().trim());

		nomorSurat.setKolom8((String) kolom8.getSelectedItem().getValue());
		nomorSurat.setTanda8(tanda8.getValue().trim());

		nomorSurat.setKolom9((String) kolom9.getSelectedItem().getValue());
		nomorSurat.setTanda9(tanda9.getValue().trim());

		nomorSurat.setKolom10((String) kolom10.getSelectedItem().getValue());
		nomorSurat.setTanda10(tanda10.getValue().trim());

		nomorSurat.setKeterangan(keterangan.getValue());
		nomorSurat.setUrutBerdasarkanNomor(urutBerdasarkanNomor.isChecked());
		nomorSurat.setResetTiap(resetTiap.getValue());

		nomorSurat.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));
		nomorSurat.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue()));

		nomorSurat.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));

		nomorSurat.setTipe(tipe);

		Common.refreshUpdate(session, nomorSurat);

		return true;
	}

	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(NomorSurat.class)

				.add(Restrictions.or(Restrictions.isNull("tipe"), Restrictions.eq("tipe", tipe)))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								parent == null ? Restrictions.isNull("satuanKerja")
										: Restrictions.sqlRestriction("false"),
								Restrictions.in("satuanKerja", satuanKerjas)));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE))
				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
				.add(CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<NomorSurat> nomorSurat = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(nomorSurat);
		grid.setRowRenderer(new NomorSuratRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaNomorSurat() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(NomorSurat.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.nomorSurat.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.nomorSurat.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		nomorSurat = (NomorSurat) obj;
		init(nomorSurat);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

}
