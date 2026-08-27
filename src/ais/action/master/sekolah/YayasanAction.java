package ais.action.master.sekolah;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

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
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.servlet.landing.PesantrenWebsiteConfig;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class YayasanAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	/** Hanya untuk super admin: bila dicentang, tampilkan SEMUA yayasan (abaikan filter domain). */
	private org.zkoss.zul.Checkbox abaikanDomain;

	private Textbox nama;
	private Textbox alamat;
	private Textbox email;
	private Textbox fax;
	private Textbox telp;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private Yayasan yayasan;
	private MyToolbarbuttonConfig add;
	private MyCkEditor deskripsi;
	protected LampiranLain kop;
	protected LampiranLain logo;
	protected LampiranLain background;
	private Textbox domain;
	private Textbox wa;
	private Textbox rt;
	private Textbox rw;
	private Textbox kodePos;
	private Textbox dusun;
	private Textbox kelurahan;
	private Textbox kecamatan;
	private Textbox kabupatenKota;
	private Textbox propinsi;
	private PesantrenWebsiteEditor websiteEditor;
	protected LampiranLain kop_ppdb;
	protected LampiranLain footer_ppdb;
	protected LampiranLain bg_ppdb;
	protected LampiranLain bg_utama;
	private MyCkEditor headerppdb;
	protected LampiranLain kopStempel;
	private Textbox motto;
	private Combobox piilhanTampilanYayasanCb;
	private Tbmuser tbmuser;

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
		tbmuser = Common.getCurrentUser();
		// Filter "Tampilkan semua (tanpa filter domain)" hanya tampil untuk super admin.
		if (abaikanDomain != null) {
			abaikanDomain.setVisible(Common.getApakahAdminLain(tbmuser));
		}
		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		String[] contents = new String[] { "id", "nama", "alamat", "email", "fax", "telp", "keterangan", "deskripsi",
				"domain" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, Yayasan.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class YayasanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Yayasan yayasan = (Yayasan) arg1;

			Vbox a = RevisiHelper.createNewRevisi(Yayasan.class, yayasan, yayasan.getNama());
			a.setParent(arg0);
			if (yayasan.getDomain() != null && !yayasan.getDomain().isEmpty()) {
				a.appendChild(new Label(yayasan.getDomain()));
			}
			new Label(yayasan.getAlamat()).setParent(arg0);
			new Label(yayasan.getTelp()).setParent(arg0);
			new Label(yayasan.getEmail()).setParent(arg0);
			new Label(yayasan.getFax()).setParent(arg0);

			new Label(yayasan.getKeterangan()).setParent(arg0);

			Hbox toolbar;
			(toolbar = Common.copyEditDeleteButtons(edit, delete, yayasan, YayasanAction.this)).setParent(arg0);

			GeneralValueObject.tampilKunci(toolbar, yayasan, tbmuser, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onSearchDefault(event);
				}

			}, false);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new Yayasan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		yayasan = (Yayasan) obj;
		init(yayasan);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
		addWindow.setTitle(yayasan.getId() == null ? "Tambah Yayasan" : "Ubah Yayasan");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		Tabbox tabbox = new Tabbox();
		tabbox.setWidth("100%");
		tabbox.setHeight("100%");
		tabbox.setParent(center);
		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);
		new MyTabConfig("Data Yayasan").setParent(tabs);
		new MyTabConfig("Halaman Utama Pesantren").setParent(tabs);
		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);
		Tabpanel dataPanel = new ais.ui.util.MyTabpanel();
		dataPanel.setStyle("overflow:auto;padding:4px;");
		dataPanel.setParent(tabpanels);
		Tabpanel websitePanel = new ais.ui.util.MyTabpanel();
		websitePanel.setStyle("overflow:auto;padding:8px;");
		websitePanel.setParent(tabpanels);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(dataPanel);
		grid.setWidth("100%");
		grid.setHeight("100%");
		websiteEditor = new PesantrenWebsiteEditor(PesantrenWebsiteConfig.editableJson(yayasan));
		websiteEditor.getComponent().setParent(websitePanel);

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Yayasan"));
		row.appendChild(nama = new Textbox(yayasan.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alamat Yayasan"));
		row.appendChild(alamat = new Textbox(yayasan.getAlamat()));
		alamat.setWidth("90%");
		alamat.setRows(3);

		row = new MyFormRow();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Email Yayasan"));
		row.appendChild(email = new Textbox(yayasan.getEmail()));
		email.setWidth("90%");

		row = new MyFormRow();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Telpon Yayasan"));
		row.appendChild(telp = new Textbox(yayasan.getTelp()));
		telp.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fax Yayasan"));
		row.appendChild(fax = new Textbox(yayasan.getTelp()));
		fax.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("WA Yayasan"));
		row.appendChild(wa = new Textbox(yayasan.getWa()));
		wa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Domain Yayasan"));
		row.appendChild(domain = new Textbox(yayasan.getDomain()));
		domain.setWidth("90%");
		Common.initKeterangan(rows,
				"Bisa lebih dari satu domain, dipisah tanda koma (,). Contoh: portal.a.ac.id, portal.b.ac.id, portal.c.ac.id. "
						+ "Sistem akan mencocokkan alamat yang sedang dibuka dengan salah satu domain di atas.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("RT Sekolah"));
		row.appendChild(rt = new Textbox(yayasan.getRt()));
		rt.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("RW Sekolah"));
		row.appendChild(rw = new Textbox(yayasan.getRw()));
		rw.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Pos Sekolah"));
		row.appendChild(kodePos = new Textbox(yayasan.getKodePos()));
		kodePos.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kampung/Dusun Sekolah"));
		row.appendChild(dusun = new Textbox(yayasan.getDusun()));
		dusun.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelurahan/Desa Sekolah"));
		row.appendChild(kelurahan = new Textbox(yayasan.getKelurahan()));
		kelurahan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kecamatan Sekolah"));
		row.appendChild(kecamatan = new Textbox(yayasan.getKecamatan()));
		kecamatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kota/Kabupaten Sekolah"));
		row.appendChild(kabupatenKota = new Textbox(yayasan.getKabupatenKota()));
		kabupatenKota.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Propinsi Sekolah"));
		row.appendChild(propinsi = new Textbox(yayasan.getPropinsi()));
		propinsi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Motto"));
		row.appendChild(motto = new Textbox(yayasan.getMotto()));
		motto.setWidth("90%");
		motto.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilihan Tampilan UI/UX"));
		piilhanTampilanYayasanCb = new Combobox();
		piilhanTampilanYayasanCb.setReadonly(true);
		piilhanTampilanYayasanCb.setWidth("90%");
		Comboitem ciYayasanDefault = new Comboitem("Ikuti Default (Konfigurasi Sistem)");
		ciYayasanDefault.setValue(ais.database.model.sekolah.Yayasan.TAMPILAN_DEFAULT);
		piilhanTampilanYayasanCb.appendChild(ciYayasanDefault);
		Comboitem ciYayasanKlasik = new Comboitem("Tampilan Klasik (ZKoss)");
		ciYayasanKlasik.setValue(ais.database.model.sekolah.Yayasan.TAMPILAN_KLASIK);
		piilhanTampilanYayasanCb.appendChild(ciYayasanKlasik);
		Comboitem ciYayasanBaru = new Comboitem("Tampilan Baru & Modern");
		ciYayasanBaru.setValue(ais.database.model.sekolah.Yayasan.TAMPILAN_BARU);
		piilhanTampilanYayasanCb.appendChild(ciYayasanBaru);
		String curTampilanYayasan = yayasan.getPiilhanTampilan();
		boolean yayasanTampilanSet = false;
		for (Object oItem : piilhanTampilanYayasanCb.getItems()) {
			Comboitem ci = (Comboitem) oItem;
			if (curTampilanYayasan != null && curTampilanYayasan.equals(ci.getValue())) {
				piilhanTampilanYayasanCb.setSelectedItem(ci);
				yayasanTampilanSet = true;
				break;
			}
		}
		if (!yayasanTampilanSet) piilhanTampilanYayasanCb.setSelectedIndex(0);
		row.appendChild(piilhanTampilanYayasanCb);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(yayasan.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		kop = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("KOP (JPG) "));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, yayasan.getId(), LampiranLain.KOP_YAYASAN, "KOP", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kop = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, yayasan.getDikunci() == null, null);
		hbox.setParent(row);

		kopStempel = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Stempel (JPG) "));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, yayasan.getId(), LampiranLain.STEMPEL_YAYASAN, "Stempel", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kopStempel = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, yayasan.getDikunci() == null, null);
		hbox.setParent(row);

		kop_ppdb = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("HEADER PPDB (JPG) "));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, yayasan.getId(), LampiranLain.KOP_PPDB_YAYASAN, "HEADER", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kop_ppdb = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, yayasan.getDikunci() == null, null);
		hbox.setParent(row);

		bg_ppdb = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Background PPDB (JPG) "));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, yayasan.getId(), LampiranLain.BG_PPDB_YAYASAN, "Background",
				false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						bg_ppdb = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, yayasan.getDikunci() == null, null);
		hbox.setParent(row);

		bg_utama = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Background Utama (JPG) "));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, yayasan.getId(), LampiranLain.BG_YAYASAN, "Background", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						bg_utama = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, yayasan.getDikunci() == null, null);
		hbox.setParent(row);

		footer_ppdb = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("FOOTER PPDB (JPG) "));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, yayasan.getId(), LampiranLain.FOOTER_PPDB_YAYASAN, "FOOTER",
				false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						footer_ppdb = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, yayasan.getDikunci() == null, null);
		hbox.setParent(row);

		logo = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Logo Yayasan"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, yayasan.getId(), LampiranLain.LOGO_YAYASAN, "Logo", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						logo = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, yayasan.getDikunci() == null, null);
		hbox.setParent(row);

		background = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Background Yayasan"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, yayasan.getId(), LampiranLain.BACKGROUND_YAYASAN, "Background",
				false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						background = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, yayasan.getDikunci() == null, null);
		hbox.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Deskripsi"));
		row.appendChild(deskripsi = new MyCkEditor());
		deskripsi.setValue(yayasan.getDeskripsi());
		deskripsi.setHeight("100%");
		deskripsi.setWidth("100%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Header PPDB"));
		row.appendChild(headerppdb = new MyCkEditor());
		headerppdb.setValue(yayasan.getHeaderppdb());
		headerppdb.setHeight("100%");
		headerppdb.setWidth("100%");

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

		if (yayasan.getDikunci() != null) {
			Common.freezeGanti(center, true);
		} else if (yayasan.getPendaftar() != null) {
			Common.freezeGanti(nama, alamat, telp, email, domain);
		}

	}

	public boolean onSave(Event event) throws Exception {
		String websiteValue;
		try {
			websiteValue = websiteEditor == null ? PesantrenWebsiteConfig.editableJson(yayasan)
					: websiteEditor.toJsonString();
			PesantrenWebsiteConfig.validate(websiteValue);
		} catch (Exception e) {
			MyMessageboxConfig.show("Konfigurasi Website ePesantren tidak valid: " + e.getMessage(), "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Yayasan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaYayasan();
		if (i) {
			MyMessageboxConfig.show("Nama Yayasan sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (yayasan.getId() != null) {
			yayasan = (Yayasan) session.load(Yayasan.class, yayasan.getId());

		}

		yayasan.setNama(nama.getValue());
		yayasan.setFax(fax.getValue());
		yayasan.setTelp(telp.getValue());
		yayasan.setAlamat(alamat.getValue());
		yayasan.setEmail(email.getValue());
		yayasan.setKeterangan(keterangan.getValue());
		yayasan.setDeskripsi(deskripsi.getValue());
		yayasan.setDomain(domain.getValue());
		yayasan.setWa(wa.getValue());

		yayasan.setRt(rt.getValue());
		yayasan.setRw(rw.getValue());
		yayasan.setKodePos(kodePos.getValue());
		yayasan.setDusun(dusun.getValue());
		yayasan.setKelurahan(kelurahan.getValue());
		yayasan.setKecamatan(kecamatan.getValue());
		yayasan.setKabupatenKota(kabupatenKota.getValue());
		yayasan.setPropinsi(propinsi.getValue());
		yayasan.setWebsite(websiteValue);
		yayasan.setHeaderppdb(headerppdb.getValue());
		yayasan.setMotto(motto.getValue().trim());

	if (piilhanTampilanYayasanCb != null && piilhanTampilanYayasanCb.getSelectedItem() != null) {
		yayasan.setPiilhanTampilan((String) piilhanTampilanYayasanCb.getSelectedItem().getValue());
	}
		Common.refreshSaveOrUpdate(session, yayasan);

		if (kop != null && kop.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(kop);
				kop.setRef(yayasan.getId());

				session.getTransaction().begin();
				session.update(kop);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		if (kopStempel != null && kopStempel.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(kopStempel);
				kopStempel.setRef(yayasan.getId());

				session.getTransaction().begin();
				session.update(kopStempel);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		if (kop_ppdb != null && kop_ppdb.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(kop_ppdb);
				kop_ppdb.setRef(yayasan.getId());

				session.getTransaction().begin();
				session.update(kop_ppdb);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		if (bg_ppdb != null && bg_ppdb.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(bg_ppdb);
				bg_ppdb.setRef(yayasan.getId());

				session.getTransaction().begin();
				session.update(bg_ppdb);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		if (bg_utama != null && bg_utama.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(bg_utama);
				bg_utama.setRef(yayasan.getId());

				session.getTransaction().begin();
				session.update(bg_utama);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		if (footer_ppdb != null && footer_ppdb.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(footer_ppdb);
				footer_ppdb.setRef(yayasan.getId());

				session.getTransaction().begin();
				session.update(footer_ppdb);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		if (logo != null && logo.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(logo);
				logo.setRef(yayasan.getId());

				session.getTransaction().begin();
				session.update(logo);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();

				Common.checkLogoUpload();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		if (background != null && background.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(background);
				background.setRef(yayasan.getId());

				session.getTransaction().begin();
				session.update(background);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();

				Common.checkLogoUpload();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				YayasanAction.reInitByDomain();
			}
		});

		return true;
	}

	public Criteria initCriteria(boolean order) {

		Yayasan yayasan = SekolahUtil.getYayasan();

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Yayasan.class)
				// Super admin yang mencentang "Tampilkan semua" → SEMUA yayasan, tanpa filter domain.
				.add((abaikanDomain != null && abaikanDomain.isChecked() && Common.getApakahAdminLain(tbmuser))
						|| yayasan == null || yayasan.getId() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.idEq(yayasan.getId()));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Yayasan> yayasan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(yayasan);
		grid.setRowRenderer(new YayasanRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaYayasan() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(Yayasan.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.yayasan.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.yayasan.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	public static volatile Map<String, Yayasan> yayasanByDomain = new HashMap<String, Yayasan>();

	private static final ReentrantLock REINIT_DOMAIN_LOCK = new ReentrantLock();
	private static volatile long reinitDomainTerakhir = 0L;
	/** Jeda minimum antar-rebuild peta domain (throttle anti thundering-herd query DB). */
	private static final long REINIT_DOMAIN_INTERVAL_MS = 60000L;

	/**
	 * Membangun ulang peta domain-&gt;Yayasan dengan aman: single-flight (hanya satu thread rebuild),
	 * throttle (tak query ulang &lt; {@value #REINIT_DOMAIN_INTERVAL_MS} ms), dan atomic-swap (bangun ke
	 * peta BARU lalu tukar referensi) agar pembaca tak pernah melihat peta setengah-kosong dan pool
	 * koneksi c3p0 tidak habis karena request paralel yang semuanya query saat peta kosong.
	 */
	@SuppressWarnings("unchecked")
	public static void reInitByDomain() {
		if (System.currentTimeMillis() - reinitDomainTerakhir < REINIT_DOMAIN_INTERVAL_MS) {
			return;
		}
		if (!REINIT_DOMAIN_LOCK.tryLock()) {
			return;
		}
		Session session = null;
		try {
			if (System.currentTimeMillis() - reinitDomainTerakhir < REINIT_DOMAIN_INTERVAL_MS) {
				return;
			}
			try {
				session = HibernateUtil.getSessionFactory().openSession();
				List<Yayasan> yayasans = ConstantValues.simpleList(
						session.createCriteria(Yayasan.class).add(Restrictions.isNotNull("domain"))
								.add(Restrictions.ne("domain", ""))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
						Yayasan.class);
				Map<String, Yayasan> baru = new HashMap<String, Yayasan>();
				for (Yayasan yayasan : yayasans) {
					if (yayasan != null && yayasan.getDomain() != null) {
						// MULTI-DOMAIN: satu Yayasan boleh punya beberapa domain (dipisah koma).
						for (String d : Common.pisahDomain(yayasan.getDomain())) {
							baru.put(d, yayasan);
						}
					}
				}
				yayasanByDomain = baru;
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				reinitDomainTerakhir = System.currentTimeMillis();
				try {
					if (session != null) session.clear();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "YayasanAction.reInitByDomain clear session");
				}
				try {
					if (session != null && session.isConnected()) session.disconnect();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "YayasanAction.reInitByDomain disconnect session");
				}
				try {
					if (session != null && session.isOpen()) session.close();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/sekolah/YayasanAction.java:813");
				}
			}
		} finally {
			REINIT_DOMAIN_LOCK.unlock();
		}
	}

}
