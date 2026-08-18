package ais.action.maintenance;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.web.Attributes;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.ClientInfoEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.East;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.master.library.util.LibraryUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.SessionCounter;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.library.Anggota;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaBarcode;
import ais.database.model.library.JenisItem;
import ais.database.model.library.KunjunganAnggota;
import ais.database.model.library.Perpustakaan;
import ais.database.model.library.TipeItem;
import ais.ui.util.CheckForParentScript;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowStyled;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class PustakaAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = 187958355469911830L;

	private MyWindow main;

	private Center papanPengumuman;
	private Borderlayout pustakaLayout;

	private Image imgLogo;
	private Label namaSeleksi;
	private Label namaSekolah;
	private Label alamatSekolah;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		// Themes.setTheme(ExecutionsCtrl.getCurrent(), "silvertail");
		return super.doBeforeCompose(page, parent, compInfo);
	}

	private void initHeader() {

		if (imgLogo != null) {
			imgLogo.detach();
		}
		if (namaSeleksi != null) {
			namaSeleksi.detach();
		}
		if (namaSekolah != null) {
			namaSekolah.detach();
		}
		if (alamatSekolah != null) {
			alamatSekolah.detach();
		}
		String judul = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi().getNama();
		String image = ais.action.master.helper.util.PerguruanTinggiUtil
				.getPerguruanTinggiMedia("logo_perguruanTinggi_");
		String Alamat1 = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi().getAlamat1();
		String Telepon = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi().getTelepon();

		if (desktopWidth < ConstantValues.UKURAN_BATAS_MOBILE) {
			North north = new North();
			north.setBorder("none");
			pustakaLayout.appendChild(north);

			north.setHeight("230px");

			Vbox vbox = new Vbox();
			vbox.setWidth("100%");
			vbox.setPack("center");
			vbox.setAlign("center");
			north.appendChild(vbox);

			Image imgLogo;
			vbox.appendChild(imgLogo = new Image(image == null ? "img/logo_pmb.png" : image));
			imgLogo.setHeight("80px");

			Label namaSeleksi = new Label(
					judul == null ? Common.getKonfigurasi("label_universitas", "Nama Instansi Kampus").getNilai()
							: judul);
			vbox.appendChild(namaSeleksi);

			Label namaSekolah = new Label(
					Common.getKonfigurasi("label_pustaka_kampus", "Sistem Informasi Pustaka").getNilai());
			vbox.appendChild(namaSekolah);

			Label alamatSekolah = new Label(Alamat1 == null || Alamat1.trim().isEmpty()
					? (Common.getKonfigurasi("label_alamat_pmb", "Alamat Instansi Kampus").getNilai() + " "
							+ Common.getKonfigurasi("label_telp_kampus", "Telp.").getNilai())
					: (Alamat1 + " " + (Telepon == null ? "" : Telepon)));
			vbox.appendChild(alamatSekolah);

			namaSeleksi.setStyle(ais.common.Common.getKonfigurasi("title_style_mobile",
					"font-size: large;color:#ededed;font-weight: bold;text-shadow: -1px 0 black, 0 1px black, 1px 0 black, 0 -1px black;")
					.getNilai());
			namaSekolah.setStyle(ais.common.Common.getKonfigurasi("motto_style_mobile",
					"font-size: 12px;color:#ededed;font-weight: bold;text-shadow: -1px 0 black, 0 1px black, 1px 0 black, 0 -1px black;")
					.getNilai());
			alamatSekolah.setStyle(ais.common.Common.getKonfigurasi("alamat_style_mobile",
					"font-size: 9px;color:#ededed;font-weight: bold;text-shadow: -1px 0 black, 0 1px black, 1px 0 black, 0 -1px black;")
					.getNilai());

		} else {
			main.setStyle("border-radius:25px;");

			North north = new North();
			north.setBorder("none");
			pustakaLayout.appendChild(north);

			String background_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
					.getPerguruanTinggiMedia((javax.servlet.http.HttpServletRequest) execution.getNativeRequest(),
							"banner_perguruanTinggi_");
			if (background_PerguruanTinggi == null || background_PerguruanTinggi.trim().isEmpty()) {
				background_PerguruanTinggi = ais.common.Common.getRequestHostWithProtocol() + "/img/header.jpg";
			}

			((Window) page.getFirstRoot()).setStyle("border-radius:20px;");

			north.setHeight("100px");

			Hbox hbox = new Hbox();
			hbox.appendChild(new Space());
			hbox.appendChild(new Space());
			hbox.setStyle("background:url('" + background_PerguruanTinggi + ""
					+ "') no-repeat center center fixed;-webkit-background-size: cover;-moz-background-size: cover;background-size: cover;-o-background-size: cover;");
			Image imgLogo;
			hbox.appendChild(imgLogo = new Image(image == null ? "img/logo_pmb.png" : image));
			hbox.appendChild(new Space());
			hbox.setWidth("100%");
			north.appendChild(hbox);

			Vbox vbox = new Vbox();

			vbox.setWidth("100%");
			vbox.setPack("center");
			hbox.appendChild(vbox);

			imgLogo.setHeight("85px");

			Label namaSeleksi = new Label(
					judul == null ? Common.getKonfigurasi("label_universitas", "Nama Instansi Kampus").getNilai()
							: judul);
			vbox.appendChild(namaSeleksi);

			Label namaSekolah = new Label(
					Common.getKonfigurasi("label_pustaka_kampus", "Sistem Informasi Pustaka").getNilai());
			vbox.appendChild(namaSekolah);

			Label alamatSekolah = new Label(Alamat1 == null || Alamat1.trim().isEmpty()
					? (Common.getKonfigurasi("label_alamat_pmb", "Alamat Instansi Kampus").getNilai() + " "
							+ Common.getKonfigurasi("label_telp_kampus", "Telp.").getNilai())
					: (Alamat1 + " " + (Telepon == null ? "" : Telepon)));
			vbox.appendChild(alamatSekolah);

			namaSeleksi.setStyle(ais.common.Common.getKonfigurasi("title_style",
					"font-size: xx-large;color:#ededed;font-weight: bold;text-shadow: -1px 0 black, 0 1px black, 1px 0 black, 0 -1px black;")
					.getNilai());
			namaSekolah.setStyle(ais.common.Common.getKonfigurasi("motto_style",
					"font-size: medium;color:#ededed;font-weight: bold;text-shadow: -1px 0 black, 0 1px black, 1px 0 black, 0 -1px black;")
					.getNilai());
			alamatSekolah.setStyle(ais.common.Common.getKonfigurasi("alamat_style",
					"font-size: 11px;color:#ededed;font-weight: bold;text-shadow: -1px 0 black, 0 1px black, 1px 0 black, 0 -1px black;")
					.getNilai());

		}

	}

	private Integer desktopHeight = null;
	private Integer desktopWidth = null;

	protected Textbox searchbahasa;
	protected Textbox searchisbn;
	protected Textbox searchnama;
	protected Textbox searchtema;
	protected Textbox searchedisi;
	protected Textbox searchpengarang;
	protected Textbox searchcatatan;
	protected Textbox searchkategori;
	protected Intbox searchtahun;
	protected Textbox searchpenerbit;
	protected Combobox searchjenisItem;
	protected Combobox searchtipeItem;
	protected Textbox searchbarcode;

	boolean merupakanMobile = false;

	@SuppressWarnings("deprecation")
	public void onInfo(ClientInfoEvent evt) {

		if (!pustakaLayout.getChildren().isEmpty()) {
			return;
		}

		desktopHeight = evt.getDesktopHeight();
		desktopWidth = evt.getDesktopWidth();

		System.out.println("desktopHeight => " + desktopHeight + ", desktopWidth => " + desktopWidth);

		main.setHeight("3700px");

		Common.clear(pustakaLayout);
		papanPengumuman = new Center();
		papanPengumuman.setBorder("none");
		pustakaLayout.appendChild(papanPengumuman);

		Component menu;
		merupakanMobile = desktopWidth < ConstantValues.UKURAN_BATAS_MOBILE;

		if (merupakanMobile) {
			menu = new South();
			((South) menu).setHeight("800px");
			pustakaLayout.appendChild(menu);

			main.setHeight("12000px");
		} else {
			menu = new East();
			((East) menu).setWidth("200px");
			pustakaLayout.appendChild(menu);

		}

		MyGroupboxStyled groupboxStyled = new MyGroupboxStyled();
		groupboxStyled.setParent(menu);
		groupboxStyled.appendChild(new MyCaptionStyled("Pencarian"));

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setParent(groupboxStyled);
		grid.setWidth("90%");
		grid.setHeight("100%");
		grid.setOddRowSclass("non-odd");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setAlign(desktopWidth < ConstantValues.UKURAN_BATAS_MOBILE ? "center" : "left");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		};

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new MyRowStyled();
		row.setParent(rows);

		row.appendChild(new MyLabelBoldAja("Judul"));

		row = new MyRowStyled();
		row.setParent(rows);

		row.appendChild(searchnama = new Textbox());
		searchnama.setWidth("80%");
		searchnama.addEventListener("onOK", eventListener);

		row = new MyRowStyled();
		row.setParent(rows);

		row.appendChild(new MyLabelBoldAja("ISBN / ISSN"));

		row = new MyRowStyled();
		row.setParent(rows);

		row.appendChild(searchisbn = new Textbox());
		searchisbn.setWidth("80%");
		searchisbn.addEventListener("onOK", eventListener);

		row = new MyRowStyled();
		row.setParent(rows);

		row.appendChild(new MyLabelBoldAja("Pengarang"));

		row = new MyRowStyled();
		row.setParent(rows);

		row.appendChild(searchpengarang = new Textbox());
		searchpengarang.setWidth("80%");
		searchpengarang.addEventListener("onOK", eventListener);

		row = new MyRowStyled();
		row.setParent(rows);

		row.appendChild(new MyLabelBoldAja("Penerbit"));

		row = new MyRowStyled();
		row.setParent(rows);

		row.appendChild(searchpenerbit = new Textbox());
		searchpenerbit.setWidth("80%");
		searchpenerbit.addEventListener("onOK", eventListener);

		row = new MyRowStyled();
		row.setParent(rows);

		row.appendChild(new MyLabelBoldAja("Edisi"));

		row = new MyRowStyled();
		row.setParent(rows);

		row.appendChild(searchedisi = new Textbox());
		searchedisi.setWidth("80%");
		searchedisi.addEventListener("onOK", eventListener);

		row = new MyRowStyled();
		row.setParent(rows);

		row.appendChild(new MyLabelBoldAja("Kategori"));

		row = new MyRowStyled();
		row.setParent(rows);

		row.appendChild(searchkategori = new Textbox());
		searchkategori.setWidth("80%");
		searchkategori.addEventListener("onOK", eventListener);

		row = new MyRowStyled();
		row.setParent(rows);

		row.appendChild(new MyLabelBoldAja("Tahun"));

		row = new MyRowStyled();
		row.setParent(rows);

		row.appendChild(searchtahun = new Intbox());
		searchtahun.setWidth("80%");
		searchtahun.addEventListener("onOK", eventListener);

		row = new MyRowStyled();
		row.setParent(rows);

		row.appendChild(new MyLabelBoldAja("Tipe"));

		row = new MyRowStyled();
		row.setParent(rows);

		row.appendChild(searchtipeItem = new Combobox());
		searchtipeItem.setWidth("80%");
		Common.insertComboDanSemua(searchtipeItem, "nama", TipeItem.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		searchtipeItem.addEventListener("onChange", eventListener);

		row = new MyRowStyled();
		row.setParent(rows);

		row.appendChild(new MyLabelBoldAja("Jenis"));

		row = new MyRowStyled();
		row.setParent(rows);

		row.appendChild(searchjenisItem = new Combobox());
		searchjenisItem.setWidth("80%");
		Common.insertComboDanSemua(searchjenisItem, "nama", JenisItem.class);
		searchjenisItem.addEventListener("onChange", eventListener);

		row = new MyRowStyled();
		row.setParent(rows);

		row.appendChild(new MyLabelBoldAja("Bahasa"));

		row = new MyRowStyled();
		row.setParent(rows);

		row.appendChild(searchbahasa = new Textbox());
		searchbahasa.setWidth("80%");
		searchbahasa.addEventListener("onOK", eventListener);

		row = new MyRowStyled();
		row.setParent(rows);

		row.appendChild(new MyLabelBoldAja("Catatan"));

		row = new MyRowStyled();
		row.setParent(rows);

		row.appendChild(searchcatatan = new Textbox());
		searchcatatan.setWidth("80%");
		searchcatatan.addEventListener("onOK", eventListener);

		row = new MyRowStyled();
		row.setParent(rows);

		row.appendChild(new MyLabelBoldAja("Barcode"));

		row = new MyRowStyled();
		row.setParent(rows);

		row.appendChild(searchbarcode = new Textbox());
		searchbarcode.setWidth("80%");
		searchbarcode.addEventListener("onOK", eventListener);

		row = new MyRowStyled();
		row.setParent(rows);

		MyButtonConfig button;
		row.appendChild(button = new MyButtonConfig("Cari", "/img/svg/search.svg"));
		button.addEventListener("onClick", eventListener);

		session.setAttribute(Attributes.PREFERRED_LOCALE, new Locale("in", "ID"));
		session.removeAttribute("usersTemp");

		initHeader();

		South south = new South();
		south.setParent(pustakaLayout);
		paging = new Paging();
		paging.setParent(south);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		PustakaAction.this.grid = new Grid();
		PustakaAction.this.grid.setSclass("fgrid");
		ais.ui.util.ZkCompat.setFixedLayout(PustakaAction.this.grid, true);
		PustakaAction.this.grid.setParent(papanPengumuman);

		columns = new Columns();
		columns.setParent(PustakaAction.this.grid);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("50%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("20%");

		onSearchDefault(null);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initBahasaParameter(execution.getParameter("lang"));
		page.getFirstRoot().appendChild(new CheckForParentScript());
		// Clients.confirmClose(Common.getBahasaConfig("Apakah Anda yakin ingin keluar
		// dari aplikasi ini ?"));
		Common.setUserAccess((HttpServletRequest) execution.getNativeRequest());
		HttpServletRequest request = (HttpServletRequest) execution.getNativeRequest();
		SessionCounter.initSessionTimeout(request.getSession(), null, false);
		if (ConstantValues.AKTIF == null) {
			ConstantValues.hasbeeninit = false;
			ConstantValues.init();
		}
		Common.REAL_PATH = session.getWebApp().getRealPath("/");
		Common.initTemp();
		CommonMedia.getMediaDirectory();
		Common.REAL_PATH_REPORT_TEMP = session.getWebApp().getRealPath("/report");
		Common.ROOT = execution.getContextPath();
		Common.encripSemua();

	}

	protected Grid grid;
	protected Paging paging;

	public Criteria initCriteria(boolean order) {
		return initCriteria(order, false);
	}

	protected Criteria initCriteria(boolean order, boolean asc) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Item.class);
		if (searchbarcode != null && !searchbarcode.getValue().trim().isEmpty()) {
			criteria = session.createCriteria(ItemPunyaBarcode.class)
					.add(searchbarcode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("barcode", searchbarcode.getValue().trim(), MatchMode.ANYWHERE))
					.createCriteria("item");
		}
		if (order)
			criteria.addOrder(asc ? Order.asc("id") : Order.desc("id"));

		String isbn = PustakaAction.this.searchisbn.getValue().trim();
		isbn = org.apache.commons.lang3.StringUtils.replace(isbn, "-", "");

		criteria.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.createAlias("penerbit", "penerbit", Criteria.LEFT_JOIN).add(Restrictions.isNull("defaultSatuanKerja"))
				.add(searchnama.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchkategori.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kategories", searchkategori.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchtahun.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahun", searchtahun.getValue()))

				.add(searchbahasa.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("bahasa", searchbahasa.getValue().trim(), MatchMode.ANYWHERE))

				.add(isbn.trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("issn", isbn.trim(), MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.ilike("isbn", isbn.trim(), MatchMode.ANYWHERE),
										Restrictions.ilike("isbn10", isbn.trim(), MatchMode.ANYWHERE))))

				.add(searchedisi.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("edisi", searchedisi.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchpengarang.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("pengarangs", searchpengarang.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchcatatan.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("catatan", searchcatatan.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchpenerbit.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("penerbit.nama", searchpenerbit.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchjenisItem.getSelectedItem() == null || searchjenisItem.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenisItem", searchjenisItem.getSelectedItem().getValue()))
				.add(searchtipeItem.getSelectedItem() == null || searchtipeItem.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tipeItem", searchtipeItem.getSelectedItem().getValue()));

		return criteria;
	}

	public void onSearchDefault(Event event) {
		onSearchDefault(event, false);
	}

	class ItemRenderer extends ais.ui.util.MyRowRenderer {

		private boolean mobile = Common.isMobile();

		@SuppressWarnings({})
		@Override
		public void render(final Row r, Object arg1) throws Exception {
			r.setValign("top");
			// TODO Auto-generated method stub
			ItemPunyaBarcode itemPunyaBarcode = (ItemPunyaBarcode) ((arg1 instanceof ItemPunyaBarcode) ? arg1 : null);
			final Item item = (Item) ((arg1 instanceof Item) ? arg1 : itemPunyaBarcode.getItem());

			LibraryUtil.checkRef(item);

			PustakaAction.displayPustaka(item, r, mobile, null, null);
		}

	}

	public static void berkunjung(Perpustakaan perpustakaan, Tbmuser tbmuser) {
		if (perpustakaan != null) {
			Session session = HibernateUtil.currentSession();
			Anggota anggota = null;
			if (tbmuser != null && tbmuser.getMahasiswa() != null) {
				anggota = (Anggota) session.createCriteria(Anggota.class)
						.add(Restrictions.eq("mahasiswa", tbmuser.getMahasiswa())).setMaxResults(1).uniqueResult();
			} else if (tbmuser != null && tbmuser.ambilDosen() != null
					&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
				anggota = (Anggota) session.createCriteria(Anggota.class)
						.add(Restrictions.eq("dosen", tbmuser.ambilDosen())).setMaxResults(1).uniqueResult();
			} else if (tbmuser != null && tbmuser.getSiswa() != null) {
				anggota = (Anggota) session.createCriteria(Anggota.class)
						.add(Restrictions.eq("siswa", tbmuser.getSiswa())).setMaxResults(1).uniqueResult();
			} else if (tbmuser != null && tbmuser.ambilPegawai() != null) {
				anggota = (Anggota) session.createCriteria(Anggota.class)
						.add(Restrictions.eq("pegawai", tbmuser.ambilPegawai())).setMaxResults(1).uniqueResult();
			}

			if (anggota == null) {
				if (tbmuser != null && tbmuser.getMahasiswa() != null) {
					anggota = Common
							.checkApakahMahasiswaOtomatisMenjadiAnggotaPerpustakaan(tbmuser.getMahasiswa().getNim());
				} else if (tbmuser != null && tbmuser.ambilDosen() != null
						&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
						&& tbmuser.ambilDosen().getNidn() != null && !tbmuser.ambilDosen().getNidn().isEmpty()) {
					anggota = Common.checkApakahDosenOtomatisMenjadiAnggotaPerpustakaan(tbmuser.ambilDosen().getNidn());
				} else if (tbmuser != null && tbmuser.ambilPegawai() != null
						&& tbmuser.ambilPegawai().getMycode() != null
						&& !tbmuser.ambilPegawai().getMycode().isEmpty()) {
					anggota = Common
							.checkApakahPegawaiOtomatisMenjadiAnggotaPerpustakaan(tbmuser.ambilPegawai().getMycode());
				} else if (tbmuser != null && tbmuser.getSiswa() != null) {
					anggota = Common.checkApakahSiswaOtomatisMenjadiAnggotaPerpustakaan(tbmuser.getSiswa());
				}
			}

			if (anggota != null) {
				KunjunganAnggota kunjunganAnggota = (KunjunganAnggota) session.createCriteria(KunjunganAnggota.class)
						.add(Restrictions.and(
								Restrictions.and(Restrictions.eq("anggota", anggota),
										Restrictions.eq("perpustakaan", perpustakaan)),
								Restrictions.eq("tgl", WaktuUtil.getDate())))
						.setMaxResults(1).uniqueResult();
				if (kunjunganAnggota == null || kunjunganAnggota.getId() == null) {
					kunjunganAnggota = new KunjunganAnggota();
					kunjunganAnggota.setKeterangan("Berkunjung secara online");
					kunjunganAnggota.setAnggota(anggota);
					kunjunganAnggota.setPerpustakaan(perpustakaan);
					kunjunganAnggota.setTanggal(WaktuUtil.getDate());
					kunjunganAnggota.setTgl(WaktuUtil.getDate());

					session.save(kunjunganAnggota);
				}
			}
		}
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	public static void displayPustaka(final Item item, Row r, boolean mobile, final Tbmuser tbmuser, Perpustakaan p)
			throws Exception {

		final Perpustakaan perpustakaan;
		if (p == null) {
			Map<Long, Perpustakaan> pustaka = ConstantValues.ambilBerdasarClass(Perpustakaan.class);
			List<Perpustakaan> pp = new ArrayList<Perpustakaan>();
			for (Perpustakaan ppp : pustaka.values()) {
				if (ppp.getAktif()) {
					pp.add(ppp);
				}
			}
			if (pp.size() == 1) {
				perpustakaan = pp.iterator().next();
			} else {
				perpustakaan = null;
			}
		} else {
			perpustakaan = p;
		}
		MyGroupboxStyled groupboxStyled = new MyGroupboxStyled();
		groupboxStyled.setParent(r);
		groupboxStyled.appendChild(new MyCaptionStyled(item.getNama()));
		groupboxStyled.setWidth("85%");
		groupboxStyled.setStyleLangsung(
				"border: 1px solid #bdbbbb;padding: 1px 2px 2px 0px;background-color: rgba(255,255,255,0.5);border-radius: 5px 5px 5px 5px;overflow: hidden;box-shadow: 1px 1px 2px #c0c0c0;max-width: 97%;margin:auto;border-width: 1px;height: 800px;");

		Grid myGrid = new Grid();
		myGrid.setParent(groupboxStyled);
		myGrid.setSclass("fgrid");

		Columns columns = new Columns();
		columns.setParent(myGrid);

		Column column = new Column();
		column.setWidth("30%");
		columns.appendChild(column);

		column = new Column();
		columns.appendChild(column);

		Rows rows = new Rows();
		rows.setParent(myGrid);

		Row row = new MyRowStyled();
		ais.ui.util.ZkCompat.setSpans(row, "2");

		row.setParent(rows);

		Image image = LibraryUtil.generateImage(item);
		image.setWidth("85%");
		image.setParent(row);

		LampiranLain lampiranLain = LampiranLain.ambil(item.getId(), LampiranLain.ITEM);
		if (lampiranLain != null) {
			row = new MyRowStyled();
			ais.ui.util.ZkCompat.setSpans(row, "2");

			row.setParent(rows);
			Toolbarbutton downloadButton = new MyToolbarbuttonConfig("Baca Koleksi", "/img/Books-icon1.png");

			row.appendChild(downloadButton);
			downloadButton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					PustakaAction.berkunjung(perpustakaan, tbmuser);

					LampiranLain tugasFileContent = LampiranLain.ambil(item.getId(), LampiranLain.ITEM);
					if (tugasFileContent.getGdrive() != null) {
						tugasFileContent.tampilGDrive(null);
					} else {

						String link = tugasFileContent == null ? null
								: (tugasFileContent.getLink() == null || tugasFileContent.getLink().isEmpty() ? null
										: tugasFileContent.getLink());

						if (tugasFileContent != null
								&& (link == null || link.trim().isEmpty() || !link.startsWith("http"))) {

							link = tugasFileContent.createLinkUri();
							if (link != null) {
								// READONLY: koleksi pustaka hanya boleh DIBACA, tidak boleh diunduh/dicetak.
								// Untuk PDF, parameter fragment #toolbar=0 menyembunyikan toolbar viewer
								// bawaan browser (tombol Download & Print). Aman diabaikan untuk gambar.
								if (link.indexOf("#toolbar=") < 0) {
									link = link + (link.indexOf('#') >= 0 ? "&" : "#")
											+ "toolbar=0&navpanes=0&scrollbar=0";
								}
							}
						}

						if (tugasFileContent != null && link != null && !link.trim().isEmpty()) {

							if (tugasFileContent.bisaPreview()) {
								Common.displayWindow(tugasFileContent.merupakanGambar(), link, true, "95%", "95%", true,
										tugasFileContent);
							} else {

								ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
							}
						} else {
							MyMessageboxConfig.show(
									"Mohon maaf, berkas yang Bapak/Ibu akses tidak ditemukan. Langkah yang dapat dilakukan: (1) mohon periksa kembali ketersediaan berkas yang dituju; (2) pastikan berkas belum dihapus atau dipindahkan; (3) apabila berkas seharusnya tersedia, silakan hubungi administrator sistem untuk bantuan lebih lanjut.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						}
					}
				}
			});

		}

		if (item.getGoogleBookId() != null && !item.getGoogleBookId().trim().isEmpty()) {
			row = new MyRowStyled();
			ais.ui.util.ZkCompat.setSpans(row, "2");

			row.setParent(rows);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Baca di Google",
					"/img/Apps-Google-Play-Books-icon.png");
			button.setOrient("vertical");
			button.setTooltiptext("Baca Buku via Google");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (perpustakaan != null) {
						Session session = HibernateUtil.currentSession();

						Anggota anggota = null;
						if (tbmuser != null && tbmuser.getMahasiswa() != null) {
							anggota = (Anggota) session.createCriteria(Anggota.class)
									.add(Restrictions.eq("mahasiswa", tbmuser.getMahasiswa())).setMaxResults(1)
									.uniqueResult();
						} else if (tbmuser != null && tbmuser.ambilDosen() != null
								&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
							anggota = (Anggota) session.createCriteria(Anggota.class)
									.add(Restrictions.eq("dosen", tbmuser.ambilDosen())).setMaxResults(1)
									.uniqueResult();
						} else if (tbmuser != null && tbmuser.getSiswa() != null) {
							anggota = (Anggota) session.createCriteria(Anggota.class)
									.add(Restrictions.eq("siswa", tbmuser.getSiswa())).setMaxResults(1).uniqueResult();
						} else if (tbmuser != null && tbmuser.ambilPegawai() != null) {
							anggota = (Anggota) session.createCriteria(Anggota.class)
									.add(Restrictions.eq("pegawai", tbmuser.ambilPegawai())).setMaxResults(1)
									.uniqueResult();
						}

						if (anggota == null) {
							if (tbmuser != null && tbmuser.getMahasiswa() != null) {
								anggota = Common.checkApakahMahasiswaOtomatisMenjadiAnggotaPerpustakaan(
										tbmuser.getMahasiswa().getNim());
							} else if (tbmuser != null && tbmuser.ambilDosen() != null
									&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
									&& tbmuser.ambilDosen().getNidn() != null
									&& !tbmuser.ambilDosen().getNidn().isEmpty()) {
								anggota = Common.checkApakahDosenOtomatisMenjadiAnggotaPerpustakaan(
										tbmuser.ambilDosen().getNidn());
							} else if (tbmuser != null && tbmuser.ambilPegawai() != null
									&& tbmuser.ambilPegawai().getMycode() != null
									&& !tbmuser.ambilPegawai().getMycode().isEmpty()) {
								anggota = Common.checkApakahPegawaiOtomatisMenjadiAnggotaPerpustakaan(
										tbmuser.ambilPegawai().getMycode());
							} else if (tbmuser != null && tbmuser.getSiswa() != null) {
								anggota = Common.checkApakahSiswaOtomatisMenjadiAnggotaPerpustakaan(tbmuser.getSiswa());
							}
						}

						if (anggota != null) {
							KunjunganAnggota kunjunganAnggota = (KunjunganAnggota) session
									.createCriteria(KunjunganAnggota.class)
									.add(Restrictions.and(
											Restrictions.and(Restrictions.eq("anggota", anggota),
													Restrictions.eq("perpustakaan", perpustakaan)),
											Restrictions.eq("tgl", WaktuUtil.getDate())))
									.setMaxResults(1).uniqueResult();
							if (kunjunganAnggota == null || kunjunganAnggota.getId() == null) {
								kunjunganAnggota = new KunjunganAnggota();
								kunjunganAnggota.setKeterangan("Berkunjung secara online, ketika membaca koleksi");
								kunjunganAnggota.setAnggota(anggota);
								kunjunganAnggota.setPerpustakaan(perpustakaan);
								kunjunganAnggota.setTanggal(WaktuUtil.getDate());
								kunjunganAnggota.setTgl(WaktuUtil.getDate());

								session.save(kunjunganAnggota);
							}
						}
					}

					JSONObject jsonObject = new JSONObject(item.getInfoLain()).getJSONObject("volumeInfo");
					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(jsonObject.getString("previewLink"), "_blank");
					} else {
						Clients.evalJavaScript("popupCenter({url: '" + jsonObject.getString("previewLink")
								+ "', title: 'Book', w: 1200, h: 600});");
					}

				}

			});
			button.setParent(row);
		}

		row = new MyRowStyled();
		row.setVisible(!item.getIsbn().trim().isEmpty() || !item.getIsbn10().trim().isEmpty());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("ISBN"));
		row.appendChild(new Label((item.getIsbn10())
				+ (item.getIsbn().trim().isEmpty() ? "" : (item.getIsbn10().isEmpty() ? "" : " / ") + item.getIsbn())));

		if (item.getIssn() != null && !item.getIssn().isEmpty()) {
			row = new MyRowStyled();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("ISSN"));
			row.appendChild(new Label((item.getIssn())));
		}

		String d = (item.getDdcItem() == null ? item.getDeweyDecimalClass() : item.getDdcItem().getKode());
		if (d != null && !d.isEmpty()) {
			row = new MyRowStyled();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Klasifikasi"));
			row.appendChild(new Label(d));
		}

		row = new MyRowStyled();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul"));
		(new Label(item.getNama())).setParent(row);

		if (item.getJenisItem() != null) {
			row = new MyRowStyled();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Jenis"));
			new Label(item.getJenisItem() == null ? "" : item.getJenisItem().getNama()).setParent(row);
		}

		row = new MyRowStyled();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penerbit"));
		String penerbits = "<font style=\"font-size: x-small;\">";
		penerbits += item.getPenerbit() == null ? "" : item.getPenerbit().getNama() + "<br>";
		penerbits += item.getPenerbit2() == null ? "" : item.getPenerbit2().getNama() + "<br>";
		penerbits += item.getPenerbit3() == null ? "" : item.getPenerbit3().getNama() + "<br>";
		penerbits += item.getPenerbit4() == null ? "" : item.getPenerbit4().getNama() + "<br>";
		penerbits += item.getPenerbit5() == null ? "" : item.getPenerbit5().getNama() + "<br>";
		penerbits += "</font>";

		new ais.ui.util.MyHtml(penerbits).setParent(row);

		if (item.getKategories() != null && !item.getKategories().isEmpty()) {
			row = new MyRowStyled();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kategori"));
			new Label(item.getKategories()).setParent(row);
		}

		if (item.getTipeItem() != null) {
			row = new MyRowStyled();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tipe"));
			new Label(item.getTipeItem() == null ? "" : item.getTipeItem().getNama()).setParent(row);
		}

		if (item.getBahasa() != null && !item.getBahasa().isEmpty()) {
			row = new MyRowStyled();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Bahasa"));
			new Label(item.getBahasa()).setParent(row);
		}

		if (item.getPengarangs() != null && !item.getPengarangs().isEmpty()) {
			row = new MyRowStyled();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Pengarang"));
			new Label(item.getPengarangs()).setParent(row);
		}

		if (item.getEdisi() != null && !item.getEdisi().isEmpty()) {
			row = new MyRowStyled();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Edisi"));
			new Label(item.getEdisi()).setParent(row);
		}

		if (item.getLink() != null && !item.getLink().isEmpty()) {
			row = new MyRowStyled();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Link"));
			A a;
			(a = new A(item.getLink())).setParent(row);
			a.setTarget("_blank");
			a.setHref(item.getLink());
		}

//		row = new MyRowStyled();
//		row.setParent(rows);
//		row.appendChild(new ais.ui.util.MyLabelConfig("Info"));
//		new Label(item.getPenaklikan()).setParent(row);

		row = new MyRowStyled();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun"));
		new Label(item.getTahun() + "").setParent(row);

		String jur = "";
		for (String j : item.getBy_statement().split(",")) {
			if (Common.isNumber(j)) {
				Jurusan jurusan = (Jurusan) ConstantValues.ambil(Jurusan.class.getName(), Long.parseLong(j));
				if (jurusan != null) {
					jur += jur.isEmpty() ? jurusan.getNama() : ", " + jurusan.getNama();
				}
			}
		}
		if (!jur.isEmpty()) {
			row = new MyRowStyled();
			row.setParent(rows);
			row.appendChild(new Label(Common.getBahasaConfig("Jurusan")));
			new Label(jur).setParent(row);
		}

		Session session = HibernateUtil.currentSession();
		List<Perpustakaan> itemPunyaBarcodes = ConstantValues.simpleList(
				session.createCriteria(ItemPunyaBarcode.class)
						.setProjection(Projections.groupProperty("perpustakaan.id")).add(Restrictions.eq("item", item)),
				Perpustakaan.class, false);
		String pus = "";
		for (Perpustakaan perpustakaan2 : itemPunyaBarcodes) {
			pus += pus.isEmpty() ? perpustakaan2.getNama() : ", " + perpustakaan2.getNama();
		}
		if (!pus.isEmpty()) {
			row = new MyRowStyled();
			row.setParent(rows);
			row.appendChild(new Label(Common.getBahasaConfig("Tersedia di")));
			row.appendChild(new Label(pus));
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onSearchDefault(Event event, final Boolean dontLoop) {

		if (searchnama == null) {
			return;
		}

		Common.initPaging(initCriteria(false), paging);

		final List item = initCriteria(true, dontLoop).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(item);
		grid.setRowRenderer(new ItemRenderer());
		grid.setModel(strset);
		if (Common.bolehKonfigurasi("terintegrasi_dengan_google_book_baru", Konfigurasi.TIDAK_AKTIF)) {
			if (!dontLoop) {

				Common.createDefaultTimerNoBusy(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						String isbn = PustakaAction.this.searchisbn.getValue().trim();
						isbn = org.apache.commons.lang3.StringUtils.replace(isbn, "-", "");
						String judul = PustakaAction.this.searchnama.getValue().trim();
						String tahun = PustakaAction.this.searchtahun.getValue() == null ? "_"
								: PustakaAction.this.searchtahun.getValue() + "";
						String keyword = "_";
						String catatan = PustakaAction.this.searchcatatan.getValue().trim();
						String pengarang = PustakaAction.this.searchpengarang.getValue().trim();
						String penerbit = PustakaAction.this.searchpenerbit.getValue().trim();
						String kategori = PustakaAction.this.searchkategori.getValue().trim();

						LibraryUtil.cariDiGoogleBook(isbn, judul, keyword, catatan, pengarang, penerbit, kategori,
								tahun, PustakaAction.this, paging, item, null);
					}
				}, "Mencoba mencari buku ke google book, harap menunggu, setelah beberapa saat, klik tombol cari lagi untuk me-load ulang buku pencarian",
						false, 2500);

			}
		}
	}
}
