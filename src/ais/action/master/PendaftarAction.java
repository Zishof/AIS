package ais.action.master;

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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
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

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pendaftar;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.JenisSekolah;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class PendaftarAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkode;
	private Checkbox searchaktif;
	/** Hanya untuk super admin: bila dicentang, tampilkan SEMUA pendaftar (abaikan filter domain/yayasan). */
	private Checkbox abaikanDomain;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private Pendaftar pendaftar;
	private MyToolbarbuttonConfig add;
	private Textbox kode;
	private Textbox domain;
	private Textbox alamat;
	private Textbox telp;
	private Textbox kontakperson;
	private MyCheckboxConfig merupakanSekolah;
	private Textbox email;
	private Textbox motto;
	private Textbox telpkontakperson;
	private Combobox css;
	protected LampiranLain kop;
	protected LampiranLain kopBawah;
	protected LampiranLain logo;
	protected LampiranLain background;
	protected LampiranLain backgroundLogin;
	protected LampiranLain banner;
	private Tbmuser tbmuser;
	private Textbox emailkontakperson;

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

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		tbmuser = Common.getCurrentUser();
		// Filter "Tampilkan semua (tanpa filter domain)" hanya tampil untuk super admin.
		if (abaikanDomain != null) {
			abaikanDomain.setVisible(Common.getApakahAdminLain(tbmuser));
		}
		if (tbmuser != null && tbmuser.getYayasan() != null && tbmuser.getYayasan().getPendaftar() != null) {
			add.setVisible(false);
			delete = false;
		}

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "kode", "nama", "keterangan", "domain", "email", "alamat", "telp",
				"kontakperson", "telpkontakperson", "emailkontakperson", "merupakanSekolah", "motto", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(Pendaftar.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, Pendaftar.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

	}

	class PendaftarRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Pendaftar pendaftar = (Pendaftar) arg1;
			new Label(pendaftar.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(Pendaftar.class, pendaftar, pendaftar.getNama()).setParent(arg0);
			new Label(pendaftar.getDomain()).setParent(arg0);
			new Label(pendaftar.getEmail()).setParent(arg0);
			new Label(pendaftar.getAlamat()).setParent(arg0);
			new Label(pendaftar.getTelp()).setParent(arg0);
			new Label(pendaftar.getAdmin() == null ? "" : pendaftar.getAdmin().getUserId()).setParent(arg0);
			new Label(pendaftar.getKontakperson()).setParent(arg0);
			new Label(pendaftar.getTelpkontakperson() + " " + pendaftar.getEmailkontakperson()).setParent(arg0);
			new Label(pendaftar.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit || !delete);
			checkbox.setChecked(pendaftar.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pendaftar.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(pendaftar);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, pendaftar, PendaftarAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new Pendaftar());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		pendaftar = (Pendaftar) obj;
		init(pendaftar);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final Pendaftar pendaftar) {
		this.pendaftar = pendaftar;
		addWindow.setTitle(pendaftar.getId() == null ? "Tambah Pendaftar" : "Ubah Pendaftar");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");

		if (pendaftar.getId() != null) {
			Tabbox tabbox = new Tabbox();
			tabbox.setParent(center);
			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			tabs.appendChild(new MyTabConfig("Data Pendaftaran"));
			MyTabConfig tabSekolah = new MyTabConfig("Data Sekolah");
			tabSekolah.setParent(tabs);

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
			tabpanelUtama.setParent(tabpanels);

			Borderlayout borderlayout2 = new ais.ui.util.MyBorderlayout();
			Center center2 = new Center();
			center2.setParent(borderlayout2);
			ais.ui.util.ZkCompat.setFlex(center2, true);
			grid.setParent(center2);
			tabpanelUtama.appendChild(borderlayout2);

			final Tabpanel tabpanelSekolah = new ais.ui.util.MyTabpanel();
			tabpanelSekolah.setParent(tabpanels);
			tabSekolah.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanelSekolah.getChildren().isEmpty()) {
						Session session = HibernateUtil.currentSession();
						Yayasan yayasan = (Yayasan) session.createCriteria(Yayasan.class)
								.add(Restrictions.eq("pendaftar", pendaftar)).setMaxResults(1).uniqueResult();
						if (yayasan == null) {
							yayasan = new Yayasan();
							yayasan.setPendaftar(pendaftar);
							session.save(yayasan);
							session.flush();
						}

						MyInclude iframe = new MyInclude("/pages/master/sekolah/sekolah.zul?yayasan=" + yayasan.getId()
								+ "&pendaftar=" + pendaftar.getId());
						iframe.setParent(tabpanelSekolah);
					}
				}

			});
		} else {
			grid.setParent(center);
		}

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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Sekolah/Perguruan Tinggi *"));
		row.appendChild(kode = new Textbox(pendaftar.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Sekolah/Perguruan Tinggi *"));
		row.appendChild(nama = new Textbox(pendaftar.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Domain (Alamat Akses) Sekolah/Perguruan Tinggi *"));
		row.appendChild(domain = new Textbox(pendaftar.getDomain()));
		domain.setWidth("90%");

		Common.initKeterangan(rows,
				"* Bisa lebih dari satu domain, dipisah tanda koma (,). Contoh: eschool.sekolahku.ac.id, ecampus.kampusku.ac.id. "
						+ "Sistem akan mencocokkan alamat yang sedang dibuka dengan salah satu domain di atas.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alamat Sekolah/Perguruan Tinggi *"));
		row.appendChild(alamat = new Textbox(pendaftar.getAlamat()));
		alamat.setWidth("90%");
		alamat.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Telp. Sekolah/Perguruan Tinggi *"));
		row.appendChild(telp = new Textbox(pendaftar.getTelp()));
		telp.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Email Sekolah/Perguruan Tinggi *"));
		row.appendChild(email = new Textbox(pendaftar.getEmail()));
		email.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(merupakanSekolah = new MyCheckboxConfig(
				"Jika instansi Anda adalah sekolah, pilih ini. Jika perguruan tinggi jangan di pilih"));
		merupakanSekolah.setChecked(pendaftar.getMerupakanSekolah());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Motto"));
		row.appendChild(motto = new Textbox(pendaftar.getMotto()));
		motto.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kontak Person *"));
		row.appendChild(kontakperson = new Textbox(pendaftar.getKontakperson()));
		kontakperson.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Telp/Wa Kontak Person *"));
		row.appendChild(telpkontakperson = new Textbox(pendaftar.getTelpkontakperson()));
		telpkontakperson.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Email Kontak Person *"));
		row.appendChild(emailkontakperson = new Textbox(pendaftar.getEmailkontakperson()));
		emailkontakperson.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(pendaftar.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tema"));
		row.appendChild(css = PerguruanTinggiAction.buatTema());
		Common.selectComboItem(css, pendaftar.getCss());

		kop = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("KOP Atas (JPG) "));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, pendaftar.getId(), LampiranLain.KOP_PT + "_Pendaftar", "KOP",
				false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kop = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		kopBawah = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("KOP Bawah (JPG) "));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, pendaftar.getId(), LampiranLain.KOP_BAWAH_PT + "_Pendaftar",
				"KOP", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kopBawah = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		logo = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Logo PT"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, pendaftar.getId(), LampiranLain.LOGO_PT + "_Pendaftar", "Logo",
				false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						logo = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		background = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Background Utama"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, pendaftar.getId(), LampiranLain.BACKGROUND_PT + "_Pendaftar",
				"Background", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						background = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		backgroundLogin = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Background Login"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, pendaftar.getId(),
				LampiranLain.BACKGROUND_LOGIN_PT + "_Pendaftar", "Background Login", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						backgroundLogin = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		banner = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Banner Halaman Web"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, pendaftar.getId(), LampiranLain.BANNER_UTAMA_PT + "_Pendaftar",
				"Banner", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						banner = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
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
		if (kode.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kode Sekolah/Perguruan Tinggi",
					"Kolom Kode Sekolah/Perguruan Tinggi belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kode Sekolah/Perguruan Tinggi.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Sekolah/Perguruan Tinggi",
					"Kolom Nama Sekolah/Perguruan Tinggi belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Sekolah/Perguruan Tinggi.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (domain.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Domain Sekolah/Perguruan Tinggi",
					"Kolom Domain Sekolah/Perguruan Tinggi belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Domain Sekolah/Perguruan Tinggi.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (alamat.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Alamat Sekolah/Perguruan Tinggi",
					"Kolom Alamat Sekolah/Perguruan Tinggi belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Alamat Sekolah/Perguruan Tinggi.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (email.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Email Sekolah/Perguruan Tinggi",
					"Kolom Email Sekolah/Perguruan Tinggi belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Email Sekolah/Perguruan Tinggi.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (telp.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Telp Sekolah/Perguruan Tinggi",
					"Kolom Telp Sekolah/Perguruan Tinggi belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Telp Sekolah/Perguruan Tinggi.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (kontakperson.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kontak person",
					"Kolom Nama Kontak person belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Kontak person.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (telpkontakperson.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Telp. Kontak person",
					"Kolom Telp. Kontak person belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Telp. Kontak person.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (emailkontakperson.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Email Kontak person",
					"Kolom Email Kontak person belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Email Kontak person.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (css.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tema",
					"Kolom Tema belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tema.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		boolean i = checkNamaPendaftar();
		if (i) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Domain",
					"Domain sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan Domain yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (pendaftar.getId() != null) {
			pendaftar = (Pendaftar) session.load(Pendaftar.class, pendaftar.getId());

		}

		pendaftar.setKode(kode.getValue());
		pendaftar.setNama(nama.getValue());
		pendaftar.setKeterangan(keterangan.getValue());
		pendaftar.setDomain(domain.getValue());
		pendaftar.setAlamat(alamat.getValue());
		pendaftar.setTelp(telp.getValue());
		pendaftar.setEmail(email.getValue());
		pendaftar.setMerupakanSekolah(merupakanSekolah.isChecked());
		pendaftar.setMotto(motto.getValue());
		pendaftar.setKontakperson(kontakperson.getValue());
		pendaftar.setTelpkontakperson(telpkontakperson.getValue());
		pendaftar.setEmailkontakperson(emailkontakperson.getValue());
		pendaftar.setCss((String) css.getSelectedItem().getValue());

		Common.refreshSaveOrUpdate(session, pendaftar);

		if (kop != null && kop.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(kop);
				kop.setRef(pendaftar.getId());

				session.getTransaction().begin();
				session.update(kop);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		if (kopBawah != null && kopBawah.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(kopBawah);
				kopBawah.setRef(pendaftar.getId());

				session.getTransaction().begin();
				session.update(kopBawah);
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
				logo.setRef(pendaftar.getId());

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
				background.setRef(pendaftar.getId());

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

		if (backgroundLogin != null && backgroundLogin.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(backgroundLogin);
				backgroundLogin.setRef(pendaftar.getId());

				session.getTransaction().begin();
				session.update(backgroundLogin);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();

				Common.checkLogoUpload();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		if (banner != null && banner.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(banner);
				banner.setRef(pendaftar.getId());

				session.getTransaction().begin();
				session.update(banner);
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
				Session session = HibernateUtil.currentSession();

				SatuanKerja satuanKerja = (SatuanKerja) session.createCriteria(SatuanKerja.class)
						.add(Restrictions.eq("pendaftar", pendaftar)).setMaxResults(1).uniqueResult();
				if (satuanKerja == null) {
					satuanKerja = new SatuanKerja();
					satuanKerja.setPendaftar(pendaftar);
					session.save(satuanKerja);
					session.flush();
				}

				PerguruanTinggi perguruanTinggi = (PerguruanTinggi) session.createCriteria(PerguruanTinggi.class)
						.add(Restrictions.eq("pendaftar", pendaftar)).setMaxResults(1).uniqueResult();
				if (perguruanTinggi == null) {
					perguruanTinggi = new PerguruanTinggi();
					perguruanTinggi.setSatuanKerja(satuanKerja);
					perguruanTinggi.setPendaftar(pendaftar);
					session.save(perguruanTinggi);
					session.flush();
				}

				if (perguruanTinggi.getSatuanKerja() == null) {
					perguruanTinggi.setSatuanKerja(satuanKerja);
					session.update(perguruanTinggi);
					session.flush();
				}

				Yayasan yayasan = (Yayasan) session.createCriteria(Yayasan.class)
						.add(Restrictions.eq("pendaftar", pendaftar)).setMaxResults(1).uniqueResult();
				if (yayasan == null) {
					yayasan = new Yayasan();
					yayasan.setPendaftar(pendaftar);
					session.save(yayasan);
					session.flush();
				}

				if (pendaftar.getAdmin() == null) {
					Tbmuser tbmuser = new Tbmuser();
					tbmuser.setUserId("admin_" + pendaftar.getId());
					tbmuser.setEmail(pendaftar.getEmailkontakperson());
					tbmuser.setHp(pendaftar.getTelpkontakperson());
					tbmuser.setSatuanKerja(satuanKerja);
					tbmuser.setIs_encripted(true);
					tbmuser.setRoot(true);
					tbmuser.setUserNama(pendaftar.getKontakperson());
					tbmuser.setYayasan(yayasan);
					tbmuser.setUserPassword(Common.desEncrypter.get().encrypt("admin_" + pendaftar.getId()));
					tbmuser.setUserRole(new Tbmrole(Tbmrole.ADMINISTRATOR));
					tbmuser.setUserShow(1);

					session.save(tbmuser);
					session.flush();

					pendaftar.setAdmin(tbmuser);
					Common.refreshSaveOrUpdate(session, pendaftar);
					session.flush();
				}

				if (satuanKerja.getYayasan() == null) {
					satuanKerja.setYayasan(yayasan);
					Common.refreshUpdate(session, satuanKerja);
					session.flush();
				}

				Sekolah sekolah = null;
				if (pendaftar.getMerupakanSekolah()) {

					sekolah = (Sekolah) session.createCriteria(Sekolah.class)
							.add(Restrictions.eq("pendaftar", pendaftar)).setMaxResults(1).uniqueResult();
					if (sekolah == null) {

						JenisSekolah jenisSekolah = (JenisSekolah) session.createCriteria(JenisSekolah.class)
								.setMaxResults(1).addOrder(Order.asc("id")).uniqueResult();

						sekolah = new Sekolah();
						sekolah.setSatuanKerja(satuanKerja);
						sekolah.setJenisSekolah(jenisSekolah);
						sekolah.setPerguruanTinggi(perguruanTinggi);
						sekolah.setYayasan(yayasan);
						sekolah.setPendaftar(pendaftar);
						session.save(sekolah);
						session.flush();
					}

					if (sekolah.getSatuanKerja() == null) {
						sekolah.setSatuanKerja(satuanKerja);
						session.update(sekolah);
						session.flush();
					}
				}

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						PerguruanTinggiAction.reInitByDomain();
					}
				});
			}
		});

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Pendaftar.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		// Super admin yang mencentang "Tampilkan semua" → SEMUA pendaftar, tanpa dibatasi yayasan/domain.
		boolean abaikan = abaikanDomain != null && abaikanDomain.isChecked() && Common.getApakahAdminLain(tbmuser);
		if (!abaikan && tbmuser != null && tbmuser.getYayasan() != null
				&& tbmuser.getYayasan().getPendaftar() != null) {
			criteria.add(Restrictions.eq("id", tbmuser.getYayasan().getPendaftar().getId()));
		}

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
		        ? Restrictions.sqlRestriction("true")
		        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Pendaftar> pendaftar = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pendaftar);
		grid.setRowRenderer(new PendaftarRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaPendaftar() {
		Session session = HibernateUtil.currentSession();

		Integer kotaCount = ((Number) session.createCriteria(Pendaftar.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("domain", domain.getValue().trim()))
				.add(this.pendaftar.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.pendaftar.getId()))
				.uniqueResult()).intValue();

		Integer countPt = ((Number) session.createCriteria(PerguruanTinggi.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("domain", domain.getValue().trim()))
				.add(this.pendaftar.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("pendaftar.id", this.pendaftar.getId()))
				.uniqueResult()).intValue();

		Integer countSekolah = ((Number) session.createCriteria(Sekolah.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("domain", domain.getValue().trim()))
				.add(this.pendaftar.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("pendaftar.id", this.pendaftar.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0) || !countPt.equals(0) || !countSekolah.equals(0);
	}

}
