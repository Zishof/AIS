package ais.action.maintenance;

import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.zkoss.web.Attributes;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.ClientInfoEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.TampilanPengumumanAkademisAction;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.recruitment.CalonPegawaiAction;
import ais.action.master.recruitment.TampilanPengumumanKarirAction;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.SessionCounter;
import ais.database.model.PengumumanAkademis;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.recruitment.CalonPegawai;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.CheckForParentScript;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk karir. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Center papanPengumuman}, {@code
 * Borderlayout karirLayout}, {@code MyWindow main}, {@code Image imgLogo}, {@code Row rowPencarian}, {@code Row
 * rowPengumuman}, {@code Row rowDicari}, {@code Tabs tabs}; inisialisasi/lifecycle ({@code doBeforeCompose()},
 * {@code initHeader()}, {@code initPencarianMenu()}, {@code doAfterCompose()}, {@code initMenuPengumuman()});
 * operasi domain lain ({@code onInfo()}, {@code onClickPMB()}, {@code onPengumuman()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class KarirAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 187958355469911830L;

	private Center papanPengumuman;
	private Borderlayout karirLayout;

	private MyWindow main;

	private Image imgLogo;

//	private Label namaSeleksi;

//	private Label namaSekolah;

//	private Label alamatSekolah;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		// Themes.setTheme(ExecutionsCtrl.getCurrent(), "silvertail");

		return super.doBeforeCompose(page, parent, compInfo);
	}

	private Row rowPencarian;
	private Row rowPengumuman;
	private Row rowDicari;
	private Tabs tabs;
	private Tabpanels tabpanels;

	private North north;

	private CalonPegawai calonPegawai;

//	private MyToolbarbutton logout;

	private MyToolbarbutton login;

	private void initHeader() {

		if (imgLogo != null) {
			imgLogo.detach();
		}

		Sekolah sekolah = SekolahUtil.getSekolah();
		Yayasan yayasan = SekolahUtil.getYayasan();
		String image = SekolahUtil.getSekolahMedia("logo_sekolah_");
		if (image == null) {
			image = SekolahUtil.getYayasanMedia("logo_yayasan_");
		}
		if (image == null) {
			image = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia("logo_perguruanTinggi_");
		}

		String label_instansi_sekolah = sekolah != null && sekolah.getId() != null ? sekolah.getNama()
				: yayasan != null && yayasan.getId() != null ? yayasan.getNama() : null;

		calonPegawai = Common.isLoginCalonPegawai();

//		logout = new MyToolbarbutton("fa-sign-out fa-big", "Logout", true);
//		logout.setSclass("user_button_profile");
//		logout.setVisible(calonPegawai != null);
		login = new MyToolbarbutton("fa-sign-in fa-big", "Login", true);
		login.setSclass("user_button_profile");
		login.setVisible(calonPegawai == null);

		if (desktopWidth < ConstantValues.UKURAN_BATAS_MOBILE) {
			north = new North();
			north.setBorder("none");
			karirLayout.appendChild(north);

			north.setHeight("150px");
			north.setSclass("headerHbox");

			Grid grid = new Grid();
			grid.setSclass("dgrid");
			grid.setHeight("100%");
			grid.setSclass("fgrid");
			grid.setStyle("border:0px;background: transparent;");
			grid.setParent(north);

			Columns columns = new Columns();
			columns.setParent(grid);

			Column column = new Column();
			column.setWidth("100%");
			column.setAlign("center");
			column.setParent(columns);

			Rows rows = new Rows();
			rows.setParent(grid);

			Row row = new Row();row.setValign("top");
			row.setStyle("border:0px;background: transparent;");
			row.setParent(rows);

			Image imgLogo;
			row.appendChild(imgLogo = new Image(image == null ? "img/logo.png" : image));
			imgLogo.setHeight("58px");

			row = new Row();
			row.setStyle("border:0px;background: transparent;");
			row.setParent(rows);

			Label namaSeleksi = new Label(label_instansi_sekolah == null
					? Common.getKonfigurasi("label_universitas", "Nama Instansi Kampus").getNilai()
					: label_instansi_sekolah);
			row.appendChild(namaSeleksi);

			row = new Row();
			row.setStyle("border:0px;background: transparent;");
			row.setParent(rows);

			Label namaSekolah = new Label(
					Common.getKonfigurasi("label_karir_header", "Bergabunglah bersama kami!").getNilai());
			row.appendChild(namaSekolah);

			namaSeleksi.setSclass("title1pmb");
			namaSekolah.setSclass("mottopmb");

			row = new Row();
			row.setStyle("border:0px;background: transparent;");
			row.setParent(rows);

			Grid gridsub = new Grid();
			gridsub.setWidth("100%");
			gridsub.setStyle("border:0px;");
			gridsub.setSclass("fgrid");
			gridsub.setParent(row);
			gridsub.setStyle("border:0px;background: transparent;");

			Columns columnssub = new Columns();
			columnssub.setParent(gridsub);

			Column columnsub = new Column();
			columnsub.setWidth("50%");
			columnsub.setAlign("left");
			columnsub.setParent(columnssub);

			columnsub = new Column();
			columnsub.setWidth("50%");
			columnsub.setAlign("right");
			columnsub.setParent(columnssub);

			Rows rowssub = new Rows();
			rowssub.setParent(gridsub);

			Row rowsub = new Row();
			rowsub.setStyle("border:0px;background: transparent;");
			rowsub.setParent(rowssub);

			rowsub = new Row();
			rowsub.setStyle("border:0px;background: transparent;");
			rowsub.setParent(rowssub);

			Hbox hbox = new Hbox();
			hbox.setWidth("100%");
			rowsub.appendChild(hbox);
			hbox.setAlign("center");
			hbox.setPack("center");

			hbox.appendChild(login);

//			hbox.appendChild(logout);

			MyToolbarbutton daftar = new MyToolbarbutton("fa-check-square-o fa-big", "Daftarkan Calon Karyawan", true);
			daftar.setVisible(login.isVisible());
			daftar.setSclass("user_button_profile");
			hbox.appendChild(daftar);
			daftar.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onClickPMB();
				}
			});

		} else {

			north = new North();
			north.setHeight("60px");
			north.setBorder("none");
			north.setSclass("headerHbox");
			karirLayout.appendChild(north);

			Hbox hbox = new Hbox();
			hbox.appendChild(new Space());
			hbox.appendChild(new Space());

			hbox.appendChild(imgLogo = new Image(image == null ? "img/logo.png" : image));
			hbox.appendChild(new Space());
			hbox.setWidth("100%");
			hbox.setHeight("90px");
			north.appendChild(hbox);

			Vbox vbox = new Vbox();

			vbox.setWidth("100%");
			vbox.setPack("center");
			hbox.appendChild(vbox);

			imgLogo.setHeight("50px");

			Label namaSeleksi = new Label(label_instansi_sekolah == null
					? Common.getKonfigurasi("label_universitas", "Nama Instansi Kampus").getNilai()
					: label_instansi_sekolah);
			vbox.appendChild(namaSeleksi);

			Label namaSekolah = new Label(
					Common.getKonfigurasi("label_karir_header", "Bergabunglah bersama kami!").getNilai());
			vbox.appendChild(namaSekolah);

			namaSeleksi.setSclass("title1");
			namaSekolah.setSclass("motto");

			Hbox hbox1 = new Hbox();
			hbox1.setWidth("100%");
			vbox.appendChild(hbox1);

			login.setStyle("position:absolute;top:20px;right:36px;");
			hbox1.appendChild(login);

//			logout.setStyle("position:absolute;top:20px;right:110px;");
//			hbox1.appendChild(logout);

			MyToolbarbutton daftar = new MyToolbarbutton("fa-check-square-o fa-big", "Daftarkan Calon Karyawan", true);
			daftar.setStyle("position:absolute;top:20px;right:110px;");
			daftar.setSclass("user_button_profile");
			daftar.setVisible(login.isVisible());
			hbox1.appendChild(daftar);
			daftar.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onClickPMB();
				}
			});

		}

		login.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.displayWindow("/pages/karir/login_karir.zul", true, "180px",
						Common.isMobile() ? "85%" : "450px");
			}
		});
//		logout.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				Common.setLogoutCalonPegawai();
//				Common.createDefaultTimer(new EventListener() {
//
//					@Override
//					public void onEvent(Event arg0) throws Exception {
//						Executions.getCurrent().sendRedirect("");
//					}
//				});
//			}
//		});

	}

	private Integer desktopHeight = null;
	private Integer desktopWidth = null;

	private boolean mobil = false;

	public void onInfo(ClientInfoEvent evt) throws Exception {

		if (!karirLayout.getChildren().isEmpty()) {
			return;
		}

		desktopHeight = evt.getDesktopHeight();
		desktopWidth = evt.getDesktopWidth();

		System.out.println("desktopHeight => " + desktopHeight + ", desktopWidth => " + desktopWidth);

		HttpServletRequest request = (HttpServletRequest) execution.getNativeRequest();
		String tinggi = (Common.isMobile(request) ? "1800"
				: Common.getKonfigurasi("tinggi_halaman_utama_karir", "800").getNilai());
		main.setHeight(tinggi + "px");

		Common.clear(karirLayout);
		papanPengumuman = new Center();
		papanPengumuman.setBorder("none");
		karirLayout.appendChild(papanPengumuman);

		initHeader();

		if (desktopWidth < ConstantValues.UKURAN_BATAS_MOBILE) {
			mobil = true;
		} else {
			mobil = false;
			West menu = new West();
			((West) menu).setWidth("215px");
			((West) menu).setBorder("none");
			((West) menu).setSplittable(true);
			((West) menu).setCollapsible(true);
			((West) menu).setStyle("background:#F5F5F5 repeat-x 0 0;");
			((West) menu).setSclass("navigasi");
			karirLayout.appendChild(menu);
			rowPencarian = Common.tampilanScroll1(menu);

			rowPencarian.setStyle("border:0px;background: transparent;");
			rowPencarian.getGrid().setStyle("border:0px;background: transparent;");

			rowDicari = new Row();
			rowDicari.setStyle("border:0px;background: transparent;");
			rowDicari.setParent(rowPencarian.getParent());

			rowPengumuman = new Row();
			rowPengumuman.setStyle("border:0px;background: transparent;");
			rowPengumuman.setParent(rowPencarian.getParent());
		}

		session.setAttribute(Attributes.PREFERRED_LOCALE, new Locale("in", "ID"));
		session.removeAttribute("usersTemp");

		String notelp = Common.getKonfigurasi("no_whatsapp_karir", "0811111111111111").getNilai();
		String wa = SekolahUtil.getYayasan().getWa();
		if (wa != null && !wa.trim().isEmpty()) {
			notelp = wa;
		}
		wa = SekolahUtil.getSekolah().getWa();
		if (wa != null && !wa.trim().isEmpty()) {
			notelp = wa;
		}

		South footer = new South();
		footer.setBorder("none");
		karirLayout.appendChild(footer);

		String styleCopyright = ais.common.Common
				.getKonfigurasi("copyright_style_dekstop", "font-size: 11px;color:black;font-weight: bold;").getNilai();

		Hbox hbox = new Hbox();
		hbox.setWidth("100%");
		hbox.setPack("center");
		hbox.setAlign("center");

		footer.appendChild(hbox);

		Label copyright = new Label();
		copyright.setStyle(styleCopyright);
		copyright.setParent(hbox);

		PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		copyright.setValue(selectedPerguruanTinggi == null ? ""
				: ("Copyright " + java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + " "
						+ selectedPerguruanTinggi.getNama()));

		if (notelp != null && !notelp.trim().isEmpty()) {

			String tanya = Common.getKonfigurasi("tanya_whatsapp_karir", "Salamat Datang, apa yang bisa kami bantu?")
					.getNilai();
			String jawab = Common
					.getKonfigurasi("jawab_whatsapp_karir",
							"Saya ingin menanyakan tentang informasi seleksi karir, apakah Anda bisa membantu?")
					.getNilai();

			String content = "<script type=\"text/javascript\" src=\"whatsapp/jquery-3.3.1.min.js\"></script>"
					+ "<link rel=\"stylesheet\" href=\"whatsapp/floating-wpp.css\">"
					+ "<script type=\"text/javascript\" src=\"whatsapp/floating-wpp.js\"></script>"
					+ "<div id=\"myButton\">\n" +

					"<script type=\"text/javascript\">\n" + "    $(function () {\n"
					+ "        $('#myButton').floatingWhatsApp({\n" + "            phone: '" + notelp + "',\n"
					+ "            popupMessage: '" + tanya + "',\n" + "            message: \"" + jawab + "\",\n"
					+ "            showPopup: true,\n" + "            showOnIE: false,\n"
					+ "            headerTitle: 'Salamat Datang!',\n" + "            headerColor: 'crimson',\n"
					+ "            backgroundColor: 'crimson',\n"
					+ "            buttonImage: '<img src=\"whatsapp/whats.svg\" />'\n" + "        });\n" + "    });\n"
					+ "</script>";

			Html html = new Html(content);
			html.setParent(hbox);
		}

		if (!mobil) {
			initPencarianMenu();
		}

		onPengumuman(null);
	}

	private void initPencarianMenu() throws Exception {
		Hbox hbox = new Hbox();
		hbox.setParent(rowPencarian);

		hbox.setWidth("100%");
		hbox.setPack("center");
		hbox.setAlign("center");

		final Textbox cari = new Textbox();
		cari.setCols(20);
		hbox.appendChild(cari);

		MyToolbarbutton config = new MyToolbarbutton("fa-search fa-big", "");
		config.setSclass("user_button_profile");
		hbox.appendChild(config);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				initMenuPengumuman(cari);
			}
		};

		cari.addEventListener("onOK", eventListener);
		config.addEventListener("onClick", eventListener);

		eventListener.onEvent(null);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		desktop.enableServerPush(false);
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
		Common.initBahasaParameter(execution.getParameter("lang"));
		Common.REAL_PATH = session.getWebApp().getRealPath("/");
		CommonMedia.getMediaDirectory();
		Common.REAL_PATH_REPORT_TEMP = session.getWebApp().getRealPath("/report");
//		System.out.println("execution => " + execution.getParameterMap());
		Common.ROOT = execution.getContextPath();
		Common.encripSemua();

	}

	public void onClickPMB() throws Exception {
		CalonPegawai calonPegawai = new CalonPegawai();

		CalonPegawaiAction.onAddExternalDaftar(null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub

			}
		}, calonPegawai, null, null);
	}

	@SuppressWarnings({ })
	public void onPengumuman(PengumumanAkademis selectedPengumumanAkademis) {
		if (papanPengumuman != null) {
			Common.clear(papanPengumuman);
			PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
			Sekolah sekolah = SekolahUtil.getSekolah();
			Borderlayout borderlayout = new Borderlayout();
			borderlayout.setParent(papanPengumuman);
			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			North north = new North();
			north.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(north, true);

			CalonPegawai calonPegawai = Common.isLoginCalonPegawai();
			TampilanPengumumanAkademisAction.tampilGelombang(selectedPerguruanTinggi, north, calonPegawai, mobil);

			Tabbox tabbox = new Tabbox();
			tabbox.setHeight("100%");
			tabbox.setWidth("100%");

			tabs = new Tabs();
			tabs.setParent(tabbox);

			tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			if (selectedPengumumanAkademis != null) {
				Tbmuser tbmuser = Common.getCurrentUser();

				TampilanPengumumanAkademisAction.tampil(center, selectedPengumumanAkademis, sekolah, tbmuser,
						selectedPerguruanTinggi, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

							}
						}, mobil, true, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								PengumumanAkademis akademis = (PengumumanAkademis) arg0.getTarget()
										.getAttribute("akademis");

								onPengumuman(akademis);

							}
						});
			} else {

				Sekolah selectedSekolah = SekolahUtil.getSekolah();
				Yayasan selectedYayasan = SekolahUtil.getYayasan();

				List<PengumumanAkademis> listPengumumanAkademis = ConstantValues.simpleList(
						TampilanPengumumanKarirAction.initCriteriaStatic(true, selectedSekolah, selectedYayasan)
								.setMaxResults(Common.ROWS_COUNT_ON_PAGE_1),
						PengumumanAkademis.class);

				if (!listPengumumanAkademis.isEmpty()) {

					Tbmuser tbmuser = Common.getCurrentUser();

					TampilanPengumumanAkademisAction.tampil(center, listPengumumanAkademis.get(0), sekolah, tbmuser,
							selectedPerguruanTinggi, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

								}
							}, mobil, true, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									PengumumanAkademis akademis = (PengumumanAkademis) arg0.getTarget()
											.getAttribute("akademis");

									onPengumuman(akademis);

								}
							});

				}
			}

		}
	}

	private void initMenuPengumuman(final Textbox cari) {

		Common.clear(rowPengumuman);

		Tbmuser tbmuser = Common.getCurrentUser();

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setSclass("fgrid");
		grid.setParent(rowPengumuman);

		Rows rows = new Rows();
		rows.setParent(grid);

		Sekolah selectedSekolah = SekolahUtil.getSekolah();
		Yayasan selectedYayasan = SekolahUtil.getYayasan();

		Criteria criteria = TampilanPengumumanKarirAction.initCriteriaStatic(true, selectedSekolah, selectedYayasan);
		TampilanPengumumanAkademisAction.loadData(rows, cari.getValue().trim(), new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] obj = (Object[]) arg0.getData();
				Long p = (Long) obj[0];
				Row row = (Row) obj[1];
				Clients.scrollIntoView(row);

				PengumumanAkademis pengumumanAkademis = (PengumumanAkademis) ConstantValues
						.ambil(PengumumanAkademis.class.getName(), p);

				onPengumuman(pengumumanAkademis);

				if (!cari.getValue().trim().isEmpty()) {
					cari.setValue("");
					initMenuPengumuman(cari);
				}
			}
		}, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				PengumumanAkademis pengumumanAkademis = (PengumumanAkademis) arg0.getData();
				TampilanPengumumanAkademisAction.prosess(pengumumanAkademis.getId(), tabs, tabpanels, true, north);

				MyMessageboxConfig.showFormat(
						"Terdapat Polling / Jejak Pendapat \"{V1}\". Mohon Bapak/Ibu berkenan melengkapi Polling / Jejak Pendapat berikut terlebih dahulu.",
						"Polling / Jejak Pendapat", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
						pengumumanAkademis.getJudul());
			}
		}, criteria, tbmuser, null, Common.isMobile(), "border:0px;background:#F5F5F5 repeat-x 0 0;");

	}

}
