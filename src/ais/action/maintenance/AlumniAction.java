package ais.action.maintenance;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.zkoss.web.Attributes;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.ClientInfoEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import ais.ui.util.MyInclude;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Vbox;

import ais.action.master.dashboard.helper.DashboardRekapMahasiswa;
import ais.action.master.helper.DaftarPenggunaOnline;
import ais.action.report.format1.akademik.LaporanRekapitulasiAlumniJurusan;
import ais.action.report.format1.akademik.LaporanRekapitulasiAlumniJurusanBerdasarkanTahunLulus;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.SessionCounter;
import ais.database.model.Konfigurasi;
import ais.ui.util.CheckForParentScript;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyWindow;

public class AlumniAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 187958355469911830L;

	private Center papanPengumuman;

	private Borderlayout alumniLayout;
	private MyWindow main;
	// private Vbox menubar;

	private boolean tampilSederhana;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		// Themes.setTheme(ExecutionsCtrl.getCurrent(), "silvertail");
		return super.doBeforeCompose(page, parent, compInfo);
	}

	private void initHeader() {

		String judul = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi().getNama();
		String image = ais.action.master.helper.util.PerguruanTinggiUtil
				.getPerguruanTinggiMedia("logo_perguruanTinggi_");
//		String Alamat1 = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi().getAlamat1();
//		String Telepon = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi().getTelepon();
//
//		String background_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(
//				(javax.servlet.http.HttpServletRequest) execution.getNativeRequest(), "banner_perguruanTinggi_");
//		if (background_PerguruanTinggi == null || background_PerguruanTinggi.trim().isEmpty()) {
//			background_PerguruanTinggi = ais.common.Common.getRequestHostWithProtocol() + "/img/header.jpg";
//		}

		if (desktopWidth < ConstantValues.UKURAN_BATAS_MOBILE) {
			North north = new North();
			alumniLayout.appendChild(north);
//			north.setHeight("210px");

			north.setHeight("150px");
			north.setSclass("headerHbox");

			Vbox vbox = new Vbox();
			vbox.setWidth("100%");
			vbox.setPack("center");
			vbox.setAlign("center");
			north.appendChild(vbox);

//			vbox.setStyle("background:url('" + background_PerguruanTinggi + ""
//					+ "') no-repeat center center fixed;-webkit-background-size: cover;-moz-background-size: cover;background-size: cover;-o-background-size: cover;");

			Image imgLogo;
			vbox.appendChild(imgLogo = new Image(image == null ? "img/logo_pmb.png" : image));
			imgLogo.setHeight("58px");

			Label namaSeleksi = new Label(
					judul == null ? Common.getKonfigurasi("label_universitas", "Nama Instansi Kampus").getNilai()
							: judul);
			vbox.appendChild(namaSeleksi);

			Label namaSekolah = new Label(
					Common.getKonfigurasi("label_alumni_kampus", "Informasi dan Tracer Study Alumni").getNilai());
			vbox.appendChild(namaSekolah);

			namaSeleksi.setSclass("title1pmb");
			namaSekolah.setSclass("mottopmb");

//			Label alamatSekolah = new Label(Alamat1 == null || Alamat1.trim().isEmpty()
//					? (Common.getKonfigurasi("label_alamat_pmb", "Alamat Instansi Kampus").getNilai() + " "
//							+ Common.getKonfigurasi("label_telp_kampus", "Telp.").getNilai())
//					: (Alamat1 + " " + (Telepon == null ? "" : Telepon)));
//			vbox.appendChild(alamatSekolah);

//			namaSeleksi.setStyle(ais.common.Common.getKonfigurasi("title_style_mobile",
//					"font-size: large;color:#ededed;font-weight: bold;text-shadow: -1px 0 black, 0 1px black, 1px 0 black, 0 -1px black;")
//					.getNilai());
//			namaSekolah.setStyle(ais.common.Common.getKonfigurasi("motto_style_mobile",
//					"font-size: 12px;color:#ededed;font-weight: bold;text-shadow: -1px 0 black, 0 1px black, 1px 0 black, 0 -1px black;")
//					.getNilai());
//			alamatSekolah.setStyle(ais.common.Common.getKonfigurasi("alamat_style_mobile",
//					"font-size: 9px;color:#ededed;font-weight: bold;text-shadow: -1px 0 black, 0 1px black, 1px 0 black, 0 -1px black;")
//					.getNilai());

		} else {
			// main.setStyle("border-radius:20px;");

			North north = new North();
			north = new North();
			north.setHeight("60px");
			north.setBorder("none");
			north.setSclass("headerHbox");
			alumniLayout.appendChild(north);

			Hbox hbox = new Hbox();
			hbox.appendChild(new Space());
			hbox.appendChild(new Space());
//			hbox.setStyle("background:url('" + background_PerguruanTinggi + ""
//					+ "') no-repeat center center fixed;-webkit-background-size: cover;-moz-background-size: cover;background-size: cover;-o-background-size: cover;");
			Image imgLogo;
			hbox.appendChild(imgLogo = new Image(image == null ? "img/logo_pmb.png" : image));
			hbox.appendChild(new Space());
//			hbox.appendChild(new Space());
			hbox.setWidth("100%");
			north.appendChild(hbox);

			Vbox vbox = new Vbox();

			vbox.setWidth("100%");
			vbox.setPack("center");
			hbox.appendChild(vbox);

			imgLogo.setHeight("50px");

			Label namaSeleksi = new Label(
					judul == null ? Common.getKonfigurasi("label_universitas", "Nama Instansi Kampus").getNilai()
							: judul);
			vbox.appendChild(namaSeleksi);

			Label namaSekolah = new Label(
					Common.getKonfigurasi("label_alumni_kampus", "Informasi dan Tracer Study Alumni").getNilai());
			vbox.appendChild(namaSekolah);

//			Label alamatSekolah = new Label(Alamat1 == null || Alamat1.trim().isEmpty()
//					? (Common.getKonfigurasi("label_alamat_pmb", "Alamat Instansi Kampus").getNilai() + " "
//							+ Common.getKonfigurasi("label_telp_kampus", "Telp.").getNilai())
//					: (Alamat1 + " " + (Telepon == null ? "" : Telepon)));
//			vbox.appendChild(alamatSekolah);

//			namaSeleksi.setStyle(ais.common.Common.getKonfigurasi("title_style",
//					"font-size: xx-large;color:#ededed;font-weight: bold;text-shadow: -1px 0 black, 0 1px black, 1px 0 black, 0 -1px black;")
//					.getNilai());
//			namaSekolah.setStyle(ais.common.Common.getKonfigurasi("motto_style",
//					"font-size: medium;color:#ededed;font-weight: bold;text-shadow: -1px 0 black, 0 1px black, 1px 0 black, 0 -1px black;")
//					.getNilai());
//			alamatSekolah.setStyle(ais.common.Common.getKonfigurasi("alamat_style",
//					"font-size: 11px;color:#ededed;font-weight: bold;text-shadow: -1px 0 black, 0 1px black, 1px 0 black, 0 -1px black;")
//					.getNilai());

			namaSeleksi.setSclass("title1");
			namaSekolah.setSclass("motto");
		}
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initBahasaParameter(execution.getParameter("lang"));
		desktop.enableServerPush(false);
		page.getFirstRoot().appendChild(new CheckForParentScript());
		// Clients.confirmClose(Common.getBahasaConfig("Apakah Anda yakin ingin keluar
		// dari aplikasi ini ?"));
		Common.setUserAccess((HttpServletRequest) execution.getNativeRequest());
		HttpServletRequest request = (HttpServletRequest) execution.getNativeRequest();
		SessionCounter.initSessionTimeout(request.getSession(), null, false);
		Common.ROOT = execution.getContextPath();
		Common.encripSemua();
		if (session != null) { session.setAttribute(Attributes.PREFERRED_LOCALE, new Locale("in", "ID")); }
		session.removeAttribute("usersTemp");
		Common.REAL_PATH = session.getWebApp().getRealPath("/");
		Common.initTemp();
		CommonMedia.getMediaDirectory();

		Common.REAL_PATH_REPORT_TEMP = session.getWebApp().getRealPath("/report");
//		MainAction.chekHeader(papanPengumuman);
		// Common.setting(header, footer, menubar, pemberitahuan);

	}

	private Integer desktopHeight = null;
	private Integer desktopWidth = null;

//	private Style mystyle;

	public void onInfo(ClientInfoEvent evt) {

		if (!alumniLayout.getChildren().isEmpty()) {
			return;
		}

		desktopHeight = evt.getDesktopHeight();
		desktopWidth = evt.getDesktopWidth();

		System.out.println("desktopHeight => " + desktopHeight + ", desktopWidth => " + desktopWidth);

		HttpServletRequest request = (HttpServletRequest) execution.getNativeRequest();
		String tinggi = (Common.isMobile(request) ? "8800"
				: Common.getKonfigurasi("tinggi_halaman_utama_alumni_baru", "8800").getNilai());
		main.setHeight(tinggi + "px");

		Common.clear(alumniLayout);
		papanPengumuman = new Center();
		alumniLayout.appendChild(papanPengumuman);

//		((West) menu).setSclass("navigasi");

		// Component menu;

		if (desktopWidth < ConstantValues.UKURAN_BATAS_MOBILE) {
			// menu = new South();
			// ((South) menu).setHeight("850px");
			// alumniLayout.appendChild(menu);
//			mystyle.setContent(Common.getKonfigurasi("style_content_mobile_alumni",
//					"body { padding: 0px 5px 0px 5px; border-radius:25px;background-color: #EBEBEB;background:url(img/banner_alumni.jpg) no-repeat center center fixed;background-size: 100% 100%; }")
//					.getNilai());
			main.setHeight("2000px");
		} else {
			// menu = new East();
			// ((East) menu).setWidth("200px");
			// alumniLayout.appendChild(menu);

//			mystyle.setContent(Common.getKonfigurasi("style_content_desktop_alumni",
//					"body { padding: 85px 25px 75px 25px; border-radius:" + "25px;background-color: #EBEBEB;background:"
//							+ "url(img/banner_alumni.jpg) no-repeat center center fixed;"
//							+ "background-size: 100% 100%; }")
//					.getNilai());
		}

		tampilSederhana = Common.bolehKonfigurasi("tampil_pengumuman_sederhana", Konfigurasi.TIDAK_AKTIF);

		onPengumuman(null);

		initHeader();
	}

	public void onLihatOnline(Event event) throws Exception {
		DaftarPenggunaOnline window = new DaftarPenggunaOnline();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		window.setVisible(true);
		window.onModal();
	}

	// Common.displayWindow("/pages/alumni/informasi.zul");

	public void onPengumuman(Event e) {
		if (papanPengumuman != null) {

			if (!tampilSederhana) {

				ais.ui.util.MyButtonTabbox btnTabAlumni = ais.ui.util.MyButtonTabbox.buat(papanPengumuman, "100%", new int[] { 0 });

				// Tab 0: Pengumuman - selalu tampil
				new MyInclude("/pages/tampilan_pengumuman_alumni.zul?desktopWidth=" + desktopWidth + "px").setParent(
						btnTabAlumni.tambahTab(0, "Pengumuman", "/img/svg/bell.svg"));

				// Tab dinamis berdasarkan konfigurasi
				final int[] idxAlumni = { 1 };

				if (Common.bolehKonfigurasi("tampil_galery_gambar_alumni")) {
					final int idx = idxAlumni[0]++;
					btnTabAlumni.tambahTabLazy(idx, "Galeri Gambar", "/img/svg/image.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
						@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
							new MyInclude("/pages/alumni/galeri_foto.zul?desktopWidth=" + desktopWidth + "px").setParent(panel);
						}
					});
				}
				if (Common.bolehKonfigurasi("tampil_input_tracer_study")) {
					final int idx = idxAlumni[0]++;
					btnTabAlumni.tambahTabLazy(idx, "Pengisian Tracer Study oleh Alumni", "/img/svg/person-lines-fill.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
						@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
							new MyInclude("/pages/master/login_alumni.zul?desktopWidth=" + desktopWidth + "px").setParent(panel);
						}
					});
				}
				if (Common.bolehKonfigurasi("tampil_daftar_alumni")) {
					final int idx = idxAlumni[0]++;
					btnTabAlumni.tambahTabLazy(idx, "Daftar Alumni", "/img/svg/user-graduate.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
						@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
							new MyInclude("/common/dashboard/alumni.zul?desktopWidth=" + desktopWidth + "px").setParent(panel);
						}
					});
				}
				if (Common.bolehKonfigurasi("tampil_statistik_alumni")) {
					final int idx = idxAlumni[0]++;
					btnTabAlumni.tambahTabLazy(idx, "Statistik Alumni", "/img/svg/chart-line.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
						@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
							// Nested MyButtonTabbox untuk Statistik Alumni
							ais.ui.util.MyButtonTabbox innerBtn = ais.ui.util.MyButtonTabbox.buat(panel, "100%", new int[] { 0 });

							// Inner tab 0: Data Lulusan - load immediately
							{
								org.zkoss.zul.Div panelLulusan = innerBtn.tambahTab(0, "Data Lulusan", "/img/svg/graduate-cap.svg");
								DashboardRekapMahasiswa rekap = new DashboardRekapMahasiswa(false, ConstantValues.LULUS);
								ais.ui.util.BaseDasbordPortal.mountWrapped(rekap, panelLulusan, "Rekap Alumni",
										"Jumlah dan sebaran alumni berdasarkan program studi dan tahun kelulusan.");
							}
							innerBtn.tambahTabLazy(1, "Lulusan Berdasar Tahun Angkatan", "/img/svg/dashboard-chart.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
								@Override public void muat(org.zkoss.zul.Div innerPanel) throws Exception {
									LaporanRekapitulasiAlumniJurusan laporan = new LaporanRekapitulasiAlumniJurusan();
									laporan.setHeight("100%");
									laporan.setWidth("100%");
									laporan.setParent(innerPanel);
								}
							});
							innerBtn.tambahTabLazy(2, "Lulusan Berdasar Tahun Lulus", "/img/svg/chart-line-light.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
								@Override public void muat(org.zkoss.zul.Div innerPanel) throws Exception {
									LaporanRekapitulasiAlumniJurusanBerdasarkanTahunLulus laporan = new LaporanRekapitulasiAlumniJurusanBerdasarkanTahunLulus();
									laporan.setHeight("100%");
									laporan.setWidth("100%");
									laporan.setParent(innerPanel);
								}
							});
						}
					});
				}

			} else {
				papanPengumuman.appendChild(
						new MyInclude("/pages/tampilan_pengumuman_alumni.zul?desktopWidth=" + desktopWidth + "px"));
			}

		}
	}

}
