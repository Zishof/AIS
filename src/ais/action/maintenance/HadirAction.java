package ais.action.maintenance;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.zkoss.web.Attributes;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.ClientInfoEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Space;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.PengumumanAkademisAction;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.SessionCounter;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Sekolah;
import ais.ui.util.CheckForParentScript;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyWindow;

public class HadirAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 187958355469911830L;

	private MyWindow main;

	private Borderlayout hadirLayout;

	private Image imgLogo;

	/**
	 * Komponen {@link Html} yang MENAMPUNG konten kartu kehadiran (dosen/guru). Referensinya disimpan
	 * agar bisa di-<i>refresh</i> secara ringan lewat {@link #refreshKehadiran()} — cukup mengganti
	 * konten HTML-nya ({@code setContent}) TANPA memuat ulang seluruh halaman.
	 */
	private Html htmlKehadiran;

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

		String judul = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi().getNama();
		String image = ais.action.master.helper.util.PerguruanTinggiUtil
				.getPerguruanTinggiMedia("logo_perguruanTinggi_");

		North north;
		if (desktopWidth < ConstantValues.UKURAN_BATAS_MOBILE) {
			north = new North();
			north.setBorder("none");
			hadirLayout.appendChild(north);

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

			Row row = new Row();
			row.setValign("top");
			row.setStyle("border:0px;background: transparent;");
			row.setParent(rows);

			Image imgLogo;
			row.appendChild(imgLogo = new Image(image == null ? "img/logo_pmb.png" : image));
			imgLogo.setHeight("58px");

			row = new Row();
			row.setStyle("border:0px;background: transparent;");
			row.setParent(rows);

			Label namaSeleksi = new Label(
					judul == null ? Common.getKonfigurasi("label_universitas", "Nama Instansi Kampus").getNilai()
							: judul);
			row.appendChild(namaSeleksi);

			row = new Row();
			row.setStyle("border:0px;background: transparent;");
			row.setParent(rows);

			Label namaSekolah = new Label(Common.getBahasaConfig(
					Common.getKonfigurasi("label_hadir_kampus", "Informasi Kehadiran Dosen Harian").getNilai()));
			row.appendChild(namaSekolah);

			namaSeleksi.setSclass("title1pmb");
			namaSekolah.setSclass("mottopmb");

			if (Common.bolehKonfigurasi("aktifkan_pemilihan_bahasa", Konfigurasi.TIDAK_AKTIF)) {

				row = new Row();
				row.setStyle("border:0px;background: transparent;");
				row.setParent(rows);

				final Combobox combobox = new Combobox();
				row.appendChild(combobox);
				Comboitem comboitem = new Comboitem(Tbmuser.INDONESIA);
				comboitem.setValue(Tbmuser.INDONESIA);
				combobox.appendChild(comboitem);

				comboitem = new Comboitem(Tbmuser.ENGLISH);
				comboitem.setValue(Tbmuser.ENGLISH);
				combobox.appendChild(comboitem);

				combobox.setCols(10);
				combobox.setReadonly(true);

				String currentLang = null;
				try {
					currentLang = (String) Sessions.getCurrent(true).getAttribute("current_lang");
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

				if (currentLang == null) {
					currentLang = Tbmuser.INDONESIA;
				}

				Common.selectComboItem(combobox, currentLang);

				combobox.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						String s = (String) (combobox.getSelectedItem() == null ? Tbmuser.INDONESIA
								: combobox.getSelectedItem().getValue());

						Sessions.getCurrent(true).setAttribute("current_lang", s);

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								ExecutionsCtrl.sendRedirect("");
							}
						});

					}
				});

			}
		} else {

			// main.setStyle("border-radius:20px;");

			north = new North();
			north.setHeight("60px");
			north.setBorder("none");
			north.setSclass("headerHbox");
			hadirLayout.appendChild(north);

			Hbox hbox = new Hbox();
			hbox.appendChild(new Space());
			hbox.appendChild(new Space());
//			hbox.setStyle("background:url('" + background_PerguruanTinggi + ""
//					+ "') no-repeat center center fixed;-webkit-background-size: cover;-moz-background-size: cover;background-size: cover;-o-background-size: cover;");
			hbox.appendChild(imgLogo = new Image(image == null ? "img/logo_pmb.png" : image));
			hbox.appendChild(new Space());
//			hbox.appendChild(new Space());
			hbox.setWidth("100%");
			hbox.setHeight("90px");
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

			Label namaSekolah = new Label(Common.getBahasaConfig(
					Common.getKonfigurasi("label_hadir_kampus", "Informasi Kehadiran Dosen Harian").getNilai()));
			vbox.appendChild(namaSekolah);

			namaSeleksi.setSclass("title1");
			namaSekolah.setSclass("motto");

			if (Common.bolehKonfigurasi("aktifkan_pemilihan_bahasa", Konfigurasi.TIDAK_AKTIF)) {

				Hbox hbox1 = new Hbox();
				hbox1.setWidth("100%");
				vbox.appendChild(hbox1);

//				MyLabelConfig login = new MyLabelConfig("Bahasa");
//
//				login.setStyle("position:absolute;top:20px;right:36px;");
//				hbox1.appendChild(login);

				final Combobox combobox = new Combobox();
				combobox.setStyle("position:absolute;top:20px;right:110px;");
				hbox1.appendChild(combobox);
				Comboitem comboitem = new Comboitem(Tbmuser.INDONESIA);
				comboitem.setValue(Tbmuser.INDONESIA);
				combobox.appendChild(comboitem);

				comboitem = new Comboitem(Tbmuser.ENGLISH);
				comboitem.setValue(Tbmuser.ENGLISH);
				combobox.appendChild(comboitem);

				combobox.setCols(10);
				combobox.setReadonly(true);

				String currentLang = null;
				try {
					currentLang = (String) Sessions.getCurrent(true).getAttribute("current_lang");
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

				if (currentLang == null) {
					currentLang = Tbmuser.INDONESIA;
				}

				Common.selectComboItem(combobox, currentLang);

				combobox.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						String s = (String) (combobox.getSelectedItem() == null ? Tbmuser.INDONESIA
								: combobox.getSelectedItem().getValue());

						Sessions.getCurrent(true).setAttribute("current_lang", s);

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								ExecutionsCtrl.sendRedirect("");
							}
						});

					}
				});
			}
		}
	}

	private Integer desktopHeight = null;
	private Integer desktopWidth = null;

	boolean merupakanMobile = false;

	private Tbmuser tbmuser;

	private Sekolah sekolah;

	public void onInfo(ClientInfoEvent evt) {

		if (!hadirLayout.getChildren().isEmpty()) {
			return;
		}

		desktopHeight = evt.getDesktopHeight();
		desktopWidth = evt.getDesktopWidth();

		System.out.println("desktopHeight => " + desktopHeight + ", desktopWidth => " + desktopWidth);

		main.setHeight("100%");

		Common.clear(hadirLayout);
		Center papanPengumuman = new Center();
		papanPengumuman.setBorder("none");
		hadirLayout.appendChild(papanPengumuman);

		merupakanMobile = desktopWidth < ConstantValues.UKURAN_BATAS_MOBILE;

		session.setAttribute(Attributes.PREFERRED_LOCALE, new Locale("in", "ID"));
		session.removeAttribute("usersTemp");

		initHeader();

		Fakultas fakultas = (Fakultas) Sessions.getCurrent(true).getAttribute("fakultas");
		Jurusan jurusan = (Jurusan) Sessions.getCurrent(true).getAttribute("jurusan");

		final boolean tampilkanFilterProdi = Common
				.getKonfigurasi("tampilkan_filter_prodi_di_daftar_kehadiran", Konfigurasi.AKTIF).getNilai().trim()
				.equals(Konfigurasi.AKTIF);

		// Konten SELALU dibungkus Borderlayout: North = toolbar (tombol "Refresh Kehadiran" + filter
		// Fakultas/Prodi bila diaktifkan), Center = kartu kehadiran. Tombol Refresh & auto-refresh cukup
		// MEMPERBARUI KONTEN (Html.setContent) — TANPA reload halaman penuh — sehingga sangat ringan.
		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(papanPengumuman);

		North north = new North();
		north.setBorder("none");
		north.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(north);
		toolbar.setHeight("36px");

		// ── Tombol RELOAD manual: memuat ulang status kehadiran seketika (hanya konten, hemat beban) ──
		ais.ui.util.MyToolbarbuttonConfig btnReloadKehadiran = new ais.ui.util.MyToolbarbuttonConfig(
				Common.getBahasaConfig("Refresh Kehadiran"), "/img/Button-Refresh-icon.png");
		btnReloadKehadiran.setTooltiptext(Common.getBahasaConfig("Muat ulang status kehadiran sekarang"));
		btnReloadKehadiran.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				refreshKehadiran();
			}
		});
		toolbar.appendChild(btnReloadKehadiran);

		boolean[] ptYa = Common.chekPtAtauSekolah();
		boolean pt = ptYa[0];

		if (tampilkanFilterProdi && pt) {
			toolbar.appendChild(new MyLabelBoldAja("Fakultas"));
			final Combobox cariFakultas = new Combobox();
			cariFakultas.setCols(10);
			toolbar.appendChild(cariFakultas);

			toolbar.appendChild(new MyLabelBoldAja("Prodi"));
			final Combobox cariProdi = new Combobox();
			cariProdi.setCols(10);
			toolbar.appendChild(cariProdi);

			Common.initFakultasDanJurusanDanSemua(cariFakultas, cariProdi, null, null);

			// Ganti Fakultas/Prodi: simpan pilihan ke sesi lalu perbarui KONTEN saja (tanpa reload penuh).
			EventListener eventListenerCari = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Fakultas fakultasPilih = (Fakultas) (cariFakultas.getSelectedItem() == null ? null
							: cariFakultas.getSelectedItem().getValue());

					Jurusan jurusanPilih = (Jurusan) (cariProdi.getSelectedItem() == null ? null
							: cariProdi.getSelectedItem().getValue());

					Sessions.getCurrent(true).setAttribute("fakultas", fakultasPilih);
					Sessions.getCurrent(true).setAttribute("jurusan", jurusanPilih);

					refreshKehadiran();
				}
			};
			cariFakultas.addEventListener("onChange", eventListenerCari);
			cariProdi.addEventListener("onChange", eventListenerCari);

			Common.selectComboItem(cariProdi, jurusan);
			Common.selectComboItem(cariFakultas, fakultas);

			if (jurusan != null) {
				Common.selectComboItem(cariFakultas, jurusan.getFakultas());
			}
		}

		Center center = new Center();
		center.setBorder("none");
		center.setParent(borderlayout);

		htmlKehadiran = new Html(renderKehadiranHtml());
		center.appendChild(htmlKehadiran);

		// ── AUTO-REFRESH "live" hemat performa: perbarui KONTEN kehadiran tiap N detik (default 30) ──
		// Memakai timer ZK BERULANG (repeat=true) + NoBusy (tanpa overlay busy). Hanya konten yang
		// dirender ulang (bukan reload halaman) sehingga JAUH lebih ringan daripada sendRedirect penuh.
		// Interval dapat diatur admin lewat konfigurasi "interval_refresh_kehadiran_detik" (minimal 5 dtk).
		int intervalRefreshDetik = 30;
		try {
			intervalRefreshDetik = Integer
					.parseInt(Common.getKonfigurasi("interval_refresh_kehadiran_detik", "30").getNilai().trim());
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		if (intervalRefreshDetik < 5) {
			intervalRefreshDetik = 5; // batas bawah agar tidak membebani server
		}
		Common.createDefaultTimerNoBusy(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				refreshKehadiran();
			}
		}, "", true, intervalRefreshDetik * 1000);

	}

	/**
	 * Membangun HTML kartu kehadiran sesuai konteks pengguna: <b>Dosen</b> (kampus, difilter Prodi/Fakultas
	 * dari sesi) atau <b>Guru</b> (sekolah). Selalu membaca Fakultas/Jurusan TERKINI dari sesi sehingga hasil
	 * refresh mengikuti filter yang sedang aktif. Dipakai baik untuk render awal maupun {@link #refreshKehadiran()}.
	 *
	 * @return potongan HTML siap dimasukkan ke komponen {@link Html}.
	 */
	private String renderKehadiranHtml() {
		Fakultas fakultas = (Fakultas) Sessions.getCurrent(true).getAttribute("fakultas");
		Jurusan jurusan = (Jurusan) Sessions.getCurrent(true).getAttribute("jurusan");
		return sekolah == null || sekolah.getId() == null
				? PengumumanAkademisAction.tampilkanKehadiranDosen(tbmuser, jurusan, fakultas, merupakanMobile, 2)
				: PengumumanAkademisAction.tampilkanKehadiranGuru(tbmuser, merupakanMobile, 2);
	}

	/**
	 * <b>Refresh ringan status kehadiran</b> (dosen/guru) tanpa memuat ulang halaman. Cukup meng-<i>query</i>
	 * ulang data terbaru lalu mengganti konten {@link #htmlKehadiran} via {@code setContent}. Dipanggil oleh
	 * tombol "Refresh Kehadiran", saat filter Fakultas/Prodi berubah, dan oleh timer auto-refresh berkala.
	 * Aman dipanggil kapan pun (no-op bila konten belum terbentuk); galat ditelan agar timer tetap berjalan.
	 */
	private void refreshKehadiran() {
		if (htmlKehadiran == null) {
			return;
		}
		try {
			htmlKehadiran.setContent(renderKehadiranHtml());
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
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

		tbmuser = Common.getCurrentUser();
		sekolah = SekolahUtil.getSekolah(request);

		Common.createDefaultTimerNoBusy(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ExecutionsCtrl.sendRedirect("hadir");
			}
		}, "", false, 10 * 60 * 1000);

	}

}
