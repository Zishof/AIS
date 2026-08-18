package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.FormatNis;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class FormatNisAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 *  
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;
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

	private FormatNis formatNis;
	private MyToolbarbuttonConfig add;
	private MyCheckboxConfig resetUrutanTiapTahun;
	private MyCheckboxConfig urutBerdasarkanNomor;
	private Intbox jumlahAngkaNolDiDepanNomorUrut;

	private Combobox searchyayasan;
	private Combobox searchsekolah;

	private Combobox yayasan;
	private Combobox sekolah;
	private MyDatebox resetTiap;

	private MyCheckboxConfig gunakanIndexUrut;
	private Longbox nomorIndex;

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

		String[] contents = new String[] { "id", "nama", "contohFormat", "resetUrutanTiapTahun", "resetUrutanTiapBulan",
				"resetTiapBulan", "resetTiapTanggal", "urutBerdasarkanNomor", "mulaiUrutanKe",
				"jumlahAngkaNolDiDepanNomorUrut", "jurusan", "sekolah", "kolom1", "tanda1", "kolom2", "tanda2",
				"kolom3", "tanda3", "kolom4", "tanda4", "kolom5", "tanda5", "kolom6", "tanda6", "kolom7", "tanda7",
				"kolom8", "tanda8", "kolom9", "tanda9", "kolom10", "tanda10", "keterangan" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, FormatNis.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	private void initKolom(Combobox combobox) {
		String[] data = new String[] { FormatNis.KOSONG, FormatNis.NOMOR_URUT, FormatNis.KATA_STATIS,
				FormatNis.TAHUN_2_DIGIT, FormatNis.TAHUN };
		for (String s : data) {
			MyComboitemConfig comboitem = new MyComboitemConfig(s);
			comboitem.setValue(s);
			combobox.appendChild(comboitem);
		}
	}

	class FormatNisRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final FormatNis formatNis = (FormatNis) arg1;

			RevisiHelper.createNewRevisi(FormatNis.class, formatNis, formatNis.getNama()).setParent(arg0);
			new Label(formatNis.getContohFormat()).setParent(arg0);

			new Label(formatNis.getSekolah() == null ? "" : formatNis.getSekolah().getNama()).setParent(arg0);
			new Label(formatNis.getGunakanIndexUrut() ? "Ya" : "Tidak").setParent(arg0);
			new Label(Common.numberFormat.get().format(formatNis.getNomorIndex())).setParent(arg0);
			new Label(formatNis.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(formatNis.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					formatNis.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(formatNis);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, formatNis, FormatNisAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new FormatNis());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(FormatNis formatNis) throws Exception {
		this.formatNis = formatNis;
		addWindow.setTitle(formatNis.getId() == null ? "Tambah Format NIS" : "Ubah Format NIS");
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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Format NIS"));
		row.appendChild(nama = new Textbox(formatNis.getNama() == null ? "" : formatNis.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai Urutan Ke"));
		row.appendChild(mulaiUrutanKe = new Longbox(formatNis.getMulaiUrutanKe()));
		mulaiUrutanKe.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Urutankan nomor surat menggunakan indeks"));
		row.appendChild(gunakanIndexUrut = new MyCheckboxConfig());
		gunakanIndexUrut.setChecked(formatNis.getGunakanIndexUrut());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Saat ini indeks ke"));
		row.appendChild(nomorIndex = new Longbox(formatNis.getNomorIndex()));
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
		row.appendChild(jumlahAngkaNolDiDepanNomorUrut = new Intbox(formatNis.getJumlahAngkaNolDiDepanNomorUrut()));
		jumlahAngkaNolDiDepanNomorUrut.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Urutan kembali ke-awal tiap ganti tahun"));
		row.appendChild(resetUrutanTiapTahun = new MyCheckboxConfig());
		resetUrutanTiapTahun.setChecked(formatNis.getResetUrutanTiapTahun());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Urutan kembali ke-awal saat tanggal"));
		row.appendChild(resetTiap = new MyDatebox(formatNis.getResetTiap()));
		resetTiap.setCols(6);

		Tbmuser tbmuser = Common.getCurrentUser();

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));

		Common.selectComboItem(yayasan,
				formatNis == null || formatNis.getYayasan() == null ? tbmuser.ambilYayasan() : formatNis.getYayasan());
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));

		Common.pilihSekolah(sekolah,
				formatNis == null || formatNis.getSekolah() == null ? tbmuser.ambilSekolah() : formatNis.getSekolah());
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Urutankan nomor berdasar Format NIS"));
		row.appendChild(urutBerdasarkanNomor = new MyCheckboxConfig());
		urutBerdasarkanNomor.setChecked(formatNis.getUrutBerdasarkanNomor());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kolom ke-1"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(kolom1 = new Combobox());
		initKolom(kolom1);
		Common.selectComboItem(kolom1, formatNis.getKolom1());
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("di-akhiri tanda")));
		hbox.appendChild(tanda1 = new Textbox(formatNis.getTanda1()));
		kolom1.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kolom ke-2"));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(kolom2 = new Combobox());
		initKolom(kolom2);
		Common.selectComboItem(kolom2, formatNis.getKolom2());
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("di-akhiri tanda")));
		hbox.appendChild(tanda2 = new Textbox(formatNis.getTanda2()));
		kolom2.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kolom ke-3"));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(kolom3 = new Combobox());
		initKolom(kolom3);
		Common.selectComboItem(kolom3, formatNis.getKolom3());
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("di-akhiri tanda")));
		hbox.appendChild(tanda3 = new Textbox(formatNis.getTanda3()));
		kolom3.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kolom ke-4"));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(kolom4 = new Combobox());
		initKolom(kolom4);
		Common.selectComboItem(kolom4, formatNis.getKolom4());
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("di-akhiri tanda")));
		hbox.appendChild(tanda4 = new Textbox(formatNis.getTanda4()));
		kolom4.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kolom ke-5"));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(kolom5 = new Combobox());
		initKolom(kolom5);
		Common.selectComboItem(kolom5, formatNis.getKolom5());
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("di-akhiri tanda")));
		hbox.appendChild(tanda5 = new Textbox(formatNis.getTanda5()));
		kolom5.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kolom ke-6"));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(kolom6 = new Combobox());
		initKolom(kolom6);
		Common.selectComboItem(kolom6, formatNis.getKolom6());
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("di-akhiri tanda")));
		hbox.appendChild(tanda6 = new Textbox(formatNis.getTanda6()));
		kolom6.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kolom ke-7"));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(kolom7 = new Combobox());
		initKolom(kolom7);
		Common.selectComboItem(kolom7, formatNis.getKolom7());
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("di-akhiri tanda")));
		hbox.appendChild(tanda7 = new Textbox(formatNis.getTanda7()));
		kolom7.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kolom ke-8"));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(kolom8 = new Combobox());
		initKolom(kolom8);
		Common.selectComboItem(kolom8, formatNis.getKolom8());
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("di-akhiri tanda")));
		hbox.appendChild(tanda8 = new Textbox(formatNis.getTanda8()));
		kolom8.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kolom ke-9"));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(kolom9 = new Combobox());
		initKolom(kolom9);
		Common.selectComboItem(kolom9, formatNis.getKolom9());
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("di-akhiri tanda")));
		hbox.appendChild(tanda9 = new Textbox(formatNis.getTanda9()));
		kolom9.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kolom ke-10"));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(kolom10 = new Combobox());
		initKolom(kolom10);
		Common.selectComboItem(kolom10, formatNis.getKolom10());
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("di-akhiri tanda")));
		hbox.appendChild(tanda10 = new Textbox(formatNis.getTanda10()));
		kolom10.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(formatNis.getKeterangan() == null ? "" : formatNis.getKeterangan()));
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
			MyMessageboxConfig.show("Nama Format NIS harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaFormatNis();
		if (i) {
			MyMessageboxConfig.show("Nama Format NIS sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (formatNis.getId() != null) {
			formatNis = (FormatNis) session.load(FormatNis.class, formatNis.getId());

		}

		formatNis.setGunakanIndexUrut(gunakanIndexUrut.isChecked());
		formatNis.setNomorIndex(nomorIndex.getValue());

		formatNis.setNama(nama.getValue());
		formatNis.setMulaiUrutanKe(mulaiUrutanKe.getValue());
		formatNis.setJumlahAngkaNolDiDepanNomorUrut(jumlahAngkaNolDiDepanNomorUrut.getValue());
		formatNis.setResetUrutanTiapTahun(resetUrutanTiapTahun.isChecked());
		formatNis.setKolom1((String) kolom1.getSelectedItem().getValue());
		formatNis.setTanda1(tanda1.getValue().trim());

		formatNis.setKolom2((String) kolom2.getSelectedItem().getValue());
		formatNis.setTanda2(tanda2.getValue().trim());

		formatNis.setKolom3((String) kolom3.getSelectedItem().getValue());
		formatNis.setTanda3(tanda3.getValue().trim());

		formatNis.setKolom4((String) kolom4.getSelectedItem().getValue());
		formatNis.setTanda4(tanda4.getValue().trim());

		formatNis.setKolom5((String) kolom5.getSelectedItem().getValue());
		formatNis.setTanda5(tanda5.getValue().trim());

		formatNis.setKolom6((String) kolom6.getSelectedItem().getValue());
		formatNis.setTanda6(tanda6.getValue().trim());

		formatNis.setKolom7((String) kolom7.getSelectedItem().getValue());
		formatNis.setTanda7(tanda7.getValue().trim());

		formatNis.setKolom8((String) kolom8.getSelectedItem().getValue());
		formatNis.setTanda8(tanda8.getValue().trim());

		formatNis.setKolom9((String) kolom9.getSelectedItem().getValue());
		formatNis.setTanda9(tanda9.getValue().trim());

		formatNis.setKolom10((String) kolom10.getSelectedItem().getValue());
		formatNis.setTanda10(tanda10.getValue().trim());

		formatNis.setKeterangan(keterangan.getValue());
		formatNis.setUrutBerdasarkanNomor(urutBerdasarkanNomor.isChecked());
		formatNis.setResetTiap(resetTiap.getValue());

		formatNis.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));
		formatNis.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue()));

		Common.refreshUpdate(session, formatNis);

		return true;
	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(FormatNis.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE))

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

		List<FormatNis> formatNis = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(formatNis);
		grid.setRowRenderer(new FormatNisRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaFormatNis() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(FormatNis.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.formatNis.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.formatNis.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		formatNis = (FormatNis) obj;
		init(formatNis);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

}
