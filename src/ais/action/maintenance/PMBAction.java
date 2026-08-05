package ais.action.maintenance;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.web.Attributes;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.ClientInfoEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Group;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.JurusanAction;
import ais.action.master.TampilanPengumumanAkademisAction;
import ais.action.master.helper.DaftarPenggunaOnline;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.pmb.CetakRegistrasiAction;
import ais.action.master.pmb.TampilanPaymentGateway;
import ais.action.master.helper.KegiatanHelper;
import ais.action.master.pmb.TampilanPengumumanPMBAction;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.SecurityFilter;
import ais.common.SessionCounter;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AfiliasiCalonMahasiswa;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Fakultas;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.JenisSeleksi;
import ais.database.model.Jurusan;
import ais.database.model.JurusanSekolahMahasiswaBaru;
import ais.database.model.Konfigurasi;
import ais.database.model.Paket;
import ais.database.model.PaketJurusanPmb;
import ais.database.model.PaketPunyaGelombangPendaftaran;
import ais.database.model.PaketPunyaProgram;
import ais.database.model.PengumumanAkademis;
import ais.database.model.PerguruanTinggi;
import ais.database.model.PilihanPaketPerJurusanMhsBaru;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.util.CheckForParentScript;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class PMBAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 187958355469911830L;


	private static final String KONFIG_PMB_LOGIN_LOGOUT_COOKIE = "pmb_login_logout_menggunakan_cookie";
	private static final String COOKIE_PMB_BIODATA = "biodataCalonMahasiswa";
	private static final String COOKIE_PMB_USERID = "userid";

	private static boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}

	private static boolean isKonfigurasiAktif(String key, String defaultValue) {
		try {
			Konfigurasi konfigurasi = Common.getKonfigurasi(key, defaultValue);
			return konfigurasi != null && konfigurasi.getNilai() != null
					&& Konfigurasi.AKTIF.equalsIgnoreCase(konfigurasi.getNilai().trim());
		} catch (Exception e) {
			return Konfigurasi.AKTIF.equalsIgnoreCase(defaultValue);
		}
	}

	private static String getKonfigurasiNilai(String key, String defaultValue) {
		try {
			Konfigurasi konfigurasi = Common.getKonfigurasi(key, defaultValue);
			return konfigurasi == null || konfigurasi.getNilai() == null ? defaultValue : konfigurasi.getNilai().trim();
		} catch (Exception e) {
			return defaultValue;
		}
	}

	private static String escapeJavaScript(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("\\", "\\\\").replace("'", "\\'").replace("\r", "").replace("\n", "");
	}

	private static String getUrlRedirectLogoutPMB() {
		String redirectPath = getKonfigurasiNilai("pmb_logout_redirect_path", "/logoff?forward_url=pmb");
		if (isBlank(redirectPath)) {
			redirectPath = "/logoff?forward_url=pmb";
		}
		if (redirectPath.startsWith("http://") || redirectPath.startsWith("https://")) {
			return redirectPath;
		}
		if (!redirectPath.startsWith("/")) {
			redirectPath = "/" + redirectPath;
		}
		return Common.ROOT + redirectPath;
	}

	public static void redirectSetelahLogoutPMB() {
		String url = getUrlRedirectLogoutPMB();
		try {
			if (isKonfigurasiAktif("pmb_logout_gunakan_javascript_replace", Konfigurasi.AKTIF)) {
				Clients.evalJavaScript("window.location.replace('" + escapeJavaScript(url) + "');");
			} else {
				Executions.sendRedirect(url);
			}
		} catch (Exception e) {
			try {
				Executions.sendRedirect(url);
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/maintenance/PMBAction.java:161");
			}
		}
	}

	public static boolean menggunakanCookieLoginLogoutPMB() {
		return isKonfigurasiAktif(KONFIG_PMB_LOGIN_LOGOUT_COOKIE, Konfigurasi.TIDAK_AKTIF);
	}

	public static BiodataCalonMahasiswa isLoginCalonMahasiswaPMB() {
		try {
			if (ExecutionsCtrl.getCurrent() == null) {
				return null;
			}
			return isLoginCalonMahasiswaPMB((HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest());
		} catch (Exception e) {
			return null;
		}
	}

	public static BiodataCalonMahasiswa isLoginCalonMahasiswaPMB(HttpServletRequest request) {
		if (request == null) {
			return null;
		}
		try {
			HttpSession httpSession = request.getSession(false);
			if (httpSession != null) {
				Object value = httpSession.getAttribute("BiodataCalonMahasiswa");
				if (value instanceof BiodataCalonMahasiswa) {
					return (BiodataCalonMahasiswa) value;
				}
			}

			if (!menggunakanCookieLoginLogoutPMB()) {
				return null;
			}

			String encryptedId = null;
			Cookie[] cookies = request.getCookies();
			if (cookies != null) {
				for (int i = 0; i < cookies.length; i++) {
					Cookie cookie = cookies[i];
					if (cookie != null && COOKIE_PMB_BIODATA.equals(cookie.getName())) {
						encryptedId = cookie.getValue();
						break;
					}
				}
			}

			if (isBlank(encryptedId)) {
				return null;
			}

			String idText = Common.desEncrypter.get().decrypt(encryptedId);
			if (isBlank(idText) || "-1".equals(idText.trim())) {
				return null;
			}

			Object data = ConstantValues.ambil(BiodataCalonMahasiswa.class.getName(), Long.valueOf(idText.trim()), true);
			return data instanceof BiodataCalonMahasiswa ? (BiodataCalonMahasiswa) data : null;
		} catch (Exception e) {
			return null;
		}
	}

	public static void bersihkanCookiePMBJikaTidakDipakai() {
		if (menggunakanCookieLoginLogoutPMB()) {
			return;
		}
		try {
			if (ExecutionsCtrl.getCurrent() != null) {
				hapusCookiePMB((HttpServletResponse) ExecutionsCtrl.getCurrent().getNativeResponse());
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	public static void setLogoutCalonMahasiswaPMB() {
		try {
			if (ExecutionsCtrl.getCurrent() == null) {
				return;
			}
			setLogoutCalonMahasiswaPMB((HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest(),
					(HttpServletResponse) ExecutionsCtrl.getCurrent().getNativeResponse());
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	public static void setLogoutCalonMahasiswaPMB(HttpServletRequest request, HttpServletResponse response) {
		boolean sudahLogoutViaCommon = false;
		if (isKonfigurasiAktif("pmb_logout_gunakan_common_setlogout", Konfigurasi.AKTIF)) {
			try {
				Common.setLogout(request, response);
				sudahLogoutViaCommon = true;
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		if (!sudahLogoutViaCommon) {
			try {
				HttpSession httpSession = request == null ? null : request.getSession(false);
				if (httpSession != null) {
					httpSession.removeAttribute("BiodataCalonMahasiswa");
					httpSession.removeAttribute("mytbmuser");
					httpSession.removeAttribute("usersTemp");
					httpSession.removeAttribute("user");
					try {
						httpSession.invalidate();
					} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				}
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		}
		hapusCookiePMB(response);
	}

	private static void hapusCookiePMB(HttpServletResponse response) {
		if (response == null) {
			return;
		}
		hapusCookie(response, COOKIE_PMB_BIODATA);
		hapusCookie(response, COOKIE_PMB_USERID);
	}

	private static void hapusCookie(HttpServletResponse response, String name) {
		try {
			Cookie cookie = new Cookie(name, "");
			cookie.setPath("/");
			cookie.setMaxAge(0);
			response.addCookie(cookie);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			if (Common.ROOT != null && Common.ROOT.trim().length() > 0) {
				Cookie cookie = new Cookie(name, "");
				cookie.setPath(Common.ROOT);
				cookie.setMaxAge(0);
				response.addCookie(cookie);
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}


	private Div kopBox;
	private Div container;
	private Div footer;
	private Div wacontainer;

	private Toolbar menubar;

	private MyToolbarbutton alur;
	private MyToolbarbutton formulir;
	private MyToolbarbutton informasiPembayaran;
	private MyToolbarbutton loginCalonMhs;
	private MyToolbarbutton informasiKelulusan;

	private MyToolbarbutton pembayaranViaPaymentGateway;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		// Themes.setTheme(ExecutionsCtrl.getCurrent(), "silvertail");
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public static Hbox headerBox(boolean bahasa) {
		String judul = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi().getNama();
		String image = ais.action.master.helper.util.PerguruanTinggiUtil
				.getPerguruanTinggiMedia("logo_perguruanTinggi_");
		Hbox hbox = new Hbox();
		hbox.setSclass("pmb-brand-box");
		hbox.setSpacing("4px");
		Image imgLogo;
		hbox.appendChild(imgLogo = new Image(image == null ? "img/logo_pmb.png" : image));
		hbox.setWidth("100%");
		hbox.setHeight("90px");

		Vbox vbox = new Vbox();
		vbox.setSclass("pmb-brand-text");
		vbox.setWidth("100%");
		vbox.setPack("center");
		hbox.appendChild(vbox);

		imgLogo.setHeight("60px");
		imgLogo.setSclass("pmb-brand-logo");

		Label namaSeleksi = new Label(
				judul == null ? Common.getKonfigurasi("label_universitas", "Nama Instansi Kampus").getNilai() : judul);
		vbox.appendChild(namaSeleksi);

		Label namaSekolah = new Label(Common.getBahasaConfig(
				Common.getKonfigurasi("label_pmb_kampus", "Seleksi Penerimaan Mahasiswa Baru").getNilai()));
		vbox.appendChild(namaSekolah);

		namaSeleksi.setSclass("title1");
		namaSekolah.setSclass("motto");

		if (bahasa && Common.bolehKonfigurasi("aktifkan_pemilihan_bahasa", Konfigurasi.TIDAK_AKTIF)) {

			Hbox hbox1 = new Hbox();
			hbox1.setWidth("100%");
			vbox.appendChild(hbox1);

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

			Common.selectComboItem(true, combobox, currentLang);

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

		return hbox;

	}

	public static void initHeader(Div kopBox, boolean bahasa) {
		Common.clear(kopBox);

		try {
			PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

			if ((selectedPerguruanTinggi != null && selectedPerguruanTinggi.getId() != null)) {
				LampiranLain kop = LampiranLain.ambil(selectedPerguruanTinggi.getId(), LampiranLain.KOP_PMB_PT);
				if (kop != null && kop.getId() != null) {

					Image image = new Image(kop.createLinkUri());
					image.setWidth("100%");
					kopBox.appendChild(image);

				} else {
					Hbox hbox = PMBAction.headerBox(bahasa);
					kopBox.appendChild(hbox);
				}
			} else {
				Hbox hbox = PMBAction.headerBox(bahasa);
				kopBox.appendChild(hbox);
			}
		} catch (Exception e) {
			Hbox hbox = PMBAction.headerBox(bahasa);
			kopBox.appendChild(hbox);
		}

	}

	public static void initBg(Div container) {

		container.setStyle("background:url('" + Common.ROOT
				+ "/img/bg_default.jpg') no-repeat center center fixed;-webkit-background-size: cover;-moz-background-size: cover;background-size: cover;-o-background-size: cover;");

		try {
			PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

			if ((selectedPerguruanTinggi != null && selectedPerguruanTinggi.getId() != null)) {
				LampiranLain kop = LampiranLain.ambil(selectedPerguruanTinggi.getId(), LampiranLain.BG_PMB_PT);
				if (kop != null && kop.getId() != null) {

					container.setStyle("background:url('" + kop.createLinkUri(true, true)
							+ "') no-repeat center center fixed;-webkit-background-size: cover;-moz-background-size: cover;background-size: cover;-o-background-size: cover;");

				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

	}

	public static void footer(Div parent) {
		String notelp = Common.getKonfigurasi("no_whatsapp_pmb", "0811111111111111").getNilai();

		PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		String telp = "";
		if (selectedPerguruanTinggi != null && selectedPerguruanTinggi.getId() != null
				&& !selectedPerguruanTinggi.getTelepon().isEmpty()) {
			telp = selectedPerguruanTinggi.getTelepon();
		}

		if ((selectedPerguruanTinggi != null && selectedPerguruanTinggi.getId() != null
				&& !selectedPerguruanTinggi.getWa().isEmpty())) {
			notelp = selectedPerguruanTinggi.getWa();
		}

		if (notelp != null && !notelp.trim().isEmpty()
				&& !(notelp == null || notelp.toString().trim().isEmpty()
						|| notelp.toString().trim().equals("00000000000000000000")
						|| notelp.toString().trim().equals("000000000"))) {
			notelp = notelp.startsWith("08") ? "+62" + notelp.substring(1) : notelp;
			notelp = notelp.startsWith("0") ? "+62" + notelp.substring(1) : notelp;
			notelp = !notelp.startsWith("+") ? "+62" + notelp : notelp;
		}
		String email = "";

		String styleCopyright = ais.common.Common
				.getKonfigurasi("footer_style_pmb",
						"font-family: 'Open Sans', sans-serif;font-size: 12px;color:white;font-weight: bold;")
				.getNilai();
		String styleCopyrightMotto = ais.common.Common.getKonfigurasi("footer_motto_style_pmb",
				"font-family: 'Open Sans', sans-serif;font-size: 18px;color:white;font-weight: bold;text-decoration: underline solid white;")
				.getNilai();

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setHeight("100%");
		grid.setWidth("100%");
		grid.setSclass("fgrid");
		grid.setStyle("border:0px;background: transparent;");
		grid.setParent(parent);

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

		Label motto = new Label();
		motto.setStyle(styleCopyrightMotto);
		motto.setParent(row);
		motto.setValue(selectedPerguruanTinggi == null ? "" : (selectedPerguruanTinggi.getMotto()));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		final String nama = selectedPerguruanTinggi != null && selectedPerguruanTinggi.getId() != null
				? selectedPerguruanTinggi.getNama()
				: "";

		Label alamat = new Label();
		alamat.setStyle(styleCopyright);
		alamat.setParent(row);
		alamat.setValue(selectedPerguruanTinggi == null ? "" : ("Alamat: " + selectedPerguruanTinggi.getAlamat1()));

		if (selectedPerguruanTinggi != null && selectedPerguruanTinggi.getId() != null
				&& !selectedPerguruanTinggi.getEmail().isEmpty()) {
			email = selectedPerguruanTinggi.getEmail();
		}

		if ((telp != null && !telp.trim().isEmpty()) || (notelp != null && !notelp.trim().isEmpty())
				|| (email != null && !email.trim().isEmpty())) {

			row = new Row();
			row.setStyle("border:0px;background: transparent;");
			row.setParent(rows);

			Hbox hbox = new Hbox();
			hbox.setWidth("100%");
			hbox.setPack("center");
			hbox.setAlign("center");
			hbox.setParent(row);

			if (telp != null && !telp.trim().isEmpty()) {

				Toolbarbutton phone = new MyToolbarbuttonConfig(telp, "/img/svg/phone-white.svg");
				phone.setStyle(styleCopyright);
				hbox.appendChild(phone);
				final String t = telp;
				phone.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						String hp = t;

						if (hp != null && !hp.trim().isEmpty()
								&& !(hp == null || hp.toString().trim().isEmpty()
										|| hp.toString().trim().equals("00000000000000000000")
										|| hp.toString().trim().equals("000000000"))) {
							hp = hp.startsWith("08") ? "+62" + hp.substring(1) : hp;
							hp = hp.startsWith("0") ? "+62" + hp.substring(1) : hp;
							hp = !hp.startsWith("+") ? "+62" + hp : hp;
						}

						String linkWa = "tel:" + t;

						Executions.getCurrent().sendRedirect(linkWa, "_blank");

					}
				});

			}

			if (notelp != null && !notelp.trim().isEmpty()) {

				Toolbarbutton whatsapp = new MyToolbarbuttonConfig(notelp, "/img/svg/whats-white.svg");
				whatsapp.setStyle(styleCopyright);
				hbox.appendChild(whatsapp);
				final String t = notelp;
				whatsapp.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						String text = "Kami ingin mendapatkan informasi tentang \"" + nama + "\"";

						String hp = t;

						if (hp != null && !hp.trim().isEmpty()
								&& !(hp == null || hp.toString().trim().isEmpty()
										|| hp.toString().trim().equals("00000000000000000000")
										|| hp.toString().trim().equals("000000000"))) {
							hp = hp.startsWith("08") ? "+62" + hp.substring(1) : hp;
							hp = hp.startsWith("0") ? "+62" + hp.substring(1) : hp;
							hp = !hp.startsWith("+") ? "+62" + hp : hp;
						}

						String linkWa = "https://api.whatsapp.com/send?phone=" + hp + "&text="
								+ URLEncoder.encode(text.replaceAll("<br>", "\n"), "UTF-8");

						Executions.getCurrent().sendRedirect(linkWa, "_blank");

					}
				});

			}

			if (email != null && !email.trim().isEmpty()) {

				Toolbarbutton whatsapp = new MyToolbarbuttonConfig(email, "/img/svg/mail-send-line-white.svg");
				whatsapp.setStyle(styleCopyright);
				hbox.appendChild(whatsapp);
				final String t = email;
				whatsapp.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						String text = "Kami ingin mendapatkan informasi tentang \"" + nama + "\"";

						String linkWa = "mailto:" + t + "?&subject="
								+ URLEncoder.encode(text.replaceAll("<br>", "\n"), "UTF-8") + "&body="
								+ URLEncoder.encode(text.replaceAll("<br>", "\n"), "UTF-8");

						Executions.getCurrent().sendRedirect(linkWa, "_blank");

					}
				});

			}
		}

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		Label copyright = new Label();
		copyright.setStyle(styleCopyright);
		copyright.setParent(row);

		copyright.setValue(selectedPerguruanTinggi == null ? ""
				: ("Copyright " + java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + " "
						+ selectedPerguruanTinggi.getNama()));

	}

	public static void initFooter(Div footerBox) {
		Common.clear(footerBox);

		try {
			PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

			if ((selectedPerguruanTinggi != null && selectedPerguruanTinggi.getId() != null)) {
				LampiranLain footer = LampiranLain.ambil(selectedPerguruanTinggi.getId(), LampiranLain.FOOTER_PMB_PT);
				if (footer != null && footer.getId() != null) {
					Image image = new Image(footer.createLinkUri());
					image.setWidth("100%");
					footerBox.appendChild(image);
				} else {
					PMBAction.footer(footerBox);
				}
			} else {
				PMBAction.footer(footerBox);
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

	}

	// WhatsApp icon SVG (official WA logo, white on transparent)
	private static final String WA_SVG = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\""
			+ " width=\"32\" height=\"32\" fill=\"#ffffff\" style=\"display:block;\">"
			+ "<path d=\"M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15"
			+ "-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475"
			+ "-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52"
			+ ".149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207"
			+ "-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372"
			+ "-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2"
			+ " 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719"
			+ " 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403"
			+ "h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374"
			+ "a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03"
			+ " 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884"
			+ "m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142"
			+ " 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005"
			+ "c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413Z\"/>"
			+ "</svg>";

	public static void tampilWa(Div wacontainer) {
		Common.clear(wacontainer);
		String notelp = Common.getKonfigurasi("no_whatsapp_pmb", "0811111111111111").getNilai();

		PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		if (selectedPerguruanTinggi != null && selectedPerguruanTinggi.getId() != null
				&& !selectedPerguruanTinggi.getWa().isEmpty()) {
			notelp = selectedPerguruanTinggi.getWa();
		}

		if (notelp != null && !notelp.trim().isEmpty()
				&& !notelp.trim().equals("00000000000000000000")
				&& !notelp.trim().equals("000000000")) {
			notelp = notelp.startsWith("08") ? "+62" + notelp.substring(1) : notelp;
			notelp = notelp.startsWith("0") ? "+62" + notelp.substring(1) : notelp;
			notelp = !notelp.startsWith("+") ? "+62" + notelp : notelp;
		}

		String script_spmb = Common.getKonfigurasi("script_spmb", "").getNilai();

		if (notelp != null && !notelp.trim().isEmpty()) {
			String tanya = Common.getKonfigurasi("tanya_whatsapp_pmb", "Kami siap membantu Anda!").getNilai();
			String jawab = Common.getKonfigurasi("jawab_whatsapp_pmb",
					"Halo, saya ingin menanyakan tentang informasi penerimaan mahasiswa baru.").getNilai();

			// Encode message for wa.me URL — no JS needed, pure HTML link
			String encodedMsg = jawab;
			try {
				encodedMsg = URLEncoder.encode(jawab, "UTF-8");
			} catch (Exception ex) {
				encodedMsg = jawab.replace(" ", "+").replace("&", "%26").replace("?", "%3F");
			}

			String noHp = notelp.replaceAll("[^0-9]", "");
			String waUrl = "https://wa.me/" + noHp + "?text=" + encodedMsg;

			// Escape title attribute (tooltip shown on hover/long-press)
			String titleEsc = tanya.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;");

			// Pure HTML/CSS floating button — zero JavaScript, zero jQuery plugin
			String htmlContent = "<a href=\"" + waUrl + "\" target=\"_blank\" rel=\"noopener\""
					+ " title=\"" + titleEsc + "\""
					+ " style=\"position:fixed;bottom:24px;left:20px;z-index:9999;"
					+ "background:#25D366;border-radius:50%;width:60px;height:60px;"
					+ "display:flex;align-items:center;justify-content:center;"
					+ "box-shadow:0 4px 14px rgba(0,0,0,.35);text-decoration:none;"
					+ "transition:transform .18s,box-shadow .18s;\" "
					+ "onmouseover=\"this.style.transform='scale(1.10)';this.style.boxShadow='0 6px 20px rgba(0,0,0,.45)';\""
					+ " onmouseout=\"this.style.transform='';this.style.boxShadow='0 4px 14px rgba(0,0,0,.35)';\">"
					+ WA_SVG
					+ "</a>";

			if (!script_spmb.isEmpty()) {
				htmlContent += script_spmb;
			}

			Html html = new Html(htmlContent);
			html.setParent(wacontainer);

		} else if (!script_spmb.isEmpty()) {
			Html html = new Html(script_spmb);
			html.setParent(wacontainer);
		}
	}

	EventListener eventListenerLogin = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {
//			Common.createDefaultTimer(new EventListener() {
//
//				@Override
//				public void onEvent(Event arg0) throws Exception {
//					Executions.getCurrent().sendRedirect("");
//				}
//			});
		}
	};

	private Integer desktopWidth = null;

	private Tabs tabs;
	private Tabpanels tabpanels;
	private AfiliasiCalonMahasiswa afiliasiCalonMahasiswaData = null;
	private GelombangPendaftaran gelombangPendaftaranData = null;
	private Row rowPencarian;
	private Row rowDicari;
	private Row rowPengumuman;
	private boolean mobil = false;
	private BiodataCalonMahasiswa biodataCalonMahasiswa;

	private String lang = null;

	public void onInfo(ClientInfoEvent evt) throws Exception {
		biodataCalonMahasiswa = PMBAction.isLoginCalonMahasiswaPMB();
		evt.getDesktopHeight();
		desktopWidth = evt.getDesktopWidth();
		PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		Common.clear(container);
		Common.clear(menubar);

		PMBAction.initHeader(kopBox, lang == null);
		PMBAction.initBg(container);
		PMBAction.tampilWa(wacontainer);
		PMBAction.initFooter(footer);

		final MyToolbarbuttonConfig menuBeranda = new MyToolbarbuttonConfig("Beranda", "/img/svg/home_door.svg");
		final MyToolbarbuttonConfig menuPilihan = new MyToolbarbuttonConfig("Pilihan", "/img/svg/list-check.svg");
		menuPilihan.setVisible(biodataCalonMahasiswa == null || biodataCalonMahasiswa.getId() == null);
		final MyToolbarbuttonConfig menuPengumuman = new MyToolbarbuttonConfig("Pengumuman",
				"/img/svg/information-circle-outline.svg");
		final A menuTampilanBaru = new A("Tampilan Baru", "/img/svg/information-circle-outline.svg");
		boolean versiBaruDisabled = ais.database.model.Konfigurasi.AKTIF.equalsIgnoreCase(
				ais.common.Common.getKonfigurasi("pmb_versi_baru_dinonaktifkan", ais.database.model.Konfigurasi.TIDAK_AKTIF).getNilai().trim());
		menuTampilanBaru.setVisible(!versiBaruDisabled && ais.common.Common.getKonfigurasi("tombol_tampilan_baru", ais.database.model.Konfigurasi.AKTIF).getNilai().equals(ais.database.model.Konfigurasi.AKTIF));
		menuBeranda.setStyle("font-size:12px;");
		menuPilihan.setStyle("font-size:12px;");
		menuPengumuman.setStyle("font-size:12px;");
		menuTampilanBaru.setStyle("font-size:12px;");

		menubar.appendChild(menuBeranda);
		menubar.appendChild(menuPilihan);
		menubar.appendChild(menuPengumuman);
		menubar.appendChild(menuTampilanBaru);

		if (desktopWidth < ConstantValues.UKURAN_BATAS_MOBILE) {
			mobil = true;
		} else {
			mobil = false;
		}

		// 1. Tetapkan Href
		menuTampilanBaru.setHref("/pmb?baru=true");

		// 2. [TAMBAHAN] Tetapkan target agar browser memuat URL di halaman yang sama
		menuTampilanBaru.setTarget("_self");

		// ... [Deklarasi menu Anda sebelumnya ada di sini] ...

		EventListener menuEventListener = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(container);

				if (arg0.getTarget() == menuBeranda) {
					initBeranda();
				} else if (arg0.getTarget() == menuPilihan) {
					initProdi();
				} else if (arg0.getTarget() == menuPengumuman) {
					onJalur(null);
				}
			}
		};

		// Daftarkan semua menu ke dalam event listener
		menuBeranda.addEventListener("onClick", menuEventListener);
		menuPengumuman.addEventListener("onClick", menuEventListener);
		menuPilihan.addEventListener("onClick", menuEventListener);

		// PENTING: menuTampilanBaru harus didaftarkan juga agar event kliknya terbaca
		menuTampilanBaru.addEventListener("onClick", menuEventListener);

		List<PengumumanAkademis> listPengumumanAkademisLangsung = ConstantValues.simpleList(TampilanPengumumanPMBAction
				.initCriteriaStatic(true, selectedPerguruanTinggi).add(Restrictions.eq("langsungMunculDiTab", true)),
				PengumumanAkademis.class);

		for (final PengumumanAkademis pengumumanAkademis : listPengumumanAkademisLangsung) {

			final MyToolbarbuttonConfig menuAlurPendaftaran = new MyToolbarbuttonConfig(pengumumanAkademis.getJudul(),
					"/img/svg/process.svg");
			menubar.appendChild(menuAlurPendaftaran);
			menuAlurPendaftaran.setStyle("font-size:12px;");
			menuAlurPendaftaran.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(container);
					Tbmuser tbmuser = Common.getCurrentUser();
					PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
					TampilanPengumumanAkademisAction.tampil(container, pengumumanAkademis, null, tbmuser,
							selectedPerguruanTinggi, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

								}
							}, false, false, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

								}
							});
				}
			});
		}

		alur = new MyToolbarbutton("fa-sitemap", Common.getBahasaConfig("Alur Pendaftaran"));

		alur.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onClickAlurPendaftaran();

			}
		});

		formulir = new MyToolbarbutton("fa-pencil-square-o", Common.getBahasaConfig("Formulir Pendaftaran"));
		formulir.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				BiodataCalonMahasiswa biodataCalonMahasiswa = PMBAction.isLoginCalonMahasiswaPMB();
				if (biodataCalonMahasiswa != null) {
					MyMessageboxConfig.showFormat(
							"Selamat, Bapak/Ibu telah berhasil melakukan pendaftaran. Silakan Bapak/Ibu menekan tombol \"{V1}\", kemudian ikuti langkah-langkah selanjutnya untuk melanjutkan proses.",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, loginCalonMhs.getLabel());
				} else {
					onClickPMB();
				}

			}
		});

		informasiPembayaran = new MyToolbarbutton("fa-money", Common.getBahasaConfig("Info Pembayaran"));
		informasiPembayaran.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onClickCariPembayaran();

			}
		});

		loginCalonMhs = new MyToolbarbutton("fa-paperclip",
				Common.getBahasaConfig("Lengkapi Biodata dan Berkas"));
		loginCalonMhs.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				biodataCalonMahasiswa = PMBAction.isLoginCalonMahasiswaPMB();

				CetakRegistrasiAction.onEdit(biodataCalonMahasiswa, new DataSearchDefault() {

					@Override
					public void onSearchDefault(Event event) {
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Executions.getCurrent().sendRedirect("");
							}
						});
					}
				});

			}
		});

		informasiKelulusan = new MyToolbarbutton("fa-check-square", Common.getBahasaConfig("Info Kelulusan"));
		informasiKelulusan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onClickCariDataUjian();

			}
		});

		pembayaranViaPaymentGateway = new MyToolbarbutton("fa-credit-card-alt",
				Common.getBahasaConfig("Proses Pembayaran"));
		pembayaranViaPaymentGateway.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onClickPayment();
			}
		});

		if (alur != null) {
			alur.setVisible(Common.bolehKonfigurasi("tampilkan_alur_pmb"));
		}
		if (formulir != null) {
			formulir.setVisible(Common.bolehKonfigurasi("tampilkan_formulir_pmb"));
		}

		if (informasiPembayaran != null) {
			informasiPembayaran.setVisible(Common.bolehKonfigurasi("tampilkan_informasiPembayaran_pmb"));
		}
		if (loginCalonMhs != null) {
			loginCalonMhs.setVisible(Common.bolehKonfigurasi("tampilkan_loginCalonMhs_pmb"));
		}

		if (informasiKelulusan != null) {
			informasiKelulusan.setVisible(Common.bolehKonfigurasi("tampilkan_informasiKelulusan_pmb"));
		}

		session.setAttribute(Attributes.PREFERRED_LOCALE, new Locale("in", "ID"));
		session.removeAttribute("usersTemp");

		if (pembayaranViaPaymentGateway != null) {
			pembayaranViaPaymentGateway.setVisible(TampilanPaymentGateway.adaPaymentGatewayYangAktif()
					&& Common.bolehKonfigurasi("tampilkan_payment_gateway_pmb"));
		}

		if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getId() != null) {
			buatTagihanCalonMahasiswaJikaBelumAda(biodataCalonMahasiswa);
		}
		initBeranda();
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
		Common.initBahasaParameter(lang = execution.getParameter("lang"));
		desktop.enableServerPush(false);
		page.getFirstRoot().appendChild(new CheckForParentScript());
		// Clients.confirmClose(Common.getBahasaConfig("Apakah Anda yakin ingin keluar
		// dari aplikasi ini ?"));
		Common.setUserAccess((HttpServletRequest) execution.getNativeRequest());
		HttpServletRequest request = (HttpServletRequest) execution.getNativeRequest();
		SessionCounter.initSessionTimeout(request.getSession(), null, false);
		PMBAction.bersihkanCookiePMBJikaTidakDipakai();
		if ("logout".equalsIgnoreCase(request.getParameter("auth_action")) || request.getParameter("logout") != null) {
			PMBAction.setLogoutCalonMahasiswaPMB(request, (HttpServletResponse) execution.getNativeResponse());
			PMBAction.redirectSetelahLogoutPMB();
			return;
		}
		if (ConstantValues.AKTIF == null) {
			ConstantValues.hasbeeninit = false;
			ConstantValues.init();
		}

		if (request.getParameter("uid") != null && !request.getParameter("uid").trim().isEmpty()) {

			try {
				String userId = Common.desEncrypter.get().decrypt(request.getParameter("uid").trim());
				Tbmuser tbmuser = (Tbmuser) ConstantValues.ambil(Tbmuser.class.getName(), userId);
				if (tbmuser != null && tbmuser.getUserId() != null) {

					final String linkProfile = "pmb?r=" + Common.randLong();
					SecurityFilter.doAutoLogin(tbmuser.getUserId(),
							Common.desEncrypter.get().decrypt(tbmuser.getUserPassword()), false, linkProfile);
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							ExecutionsCtrl.sendRedirect(linkProfile);
						}
					});

					return;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/PMBAction.java:1102");
				// TODO: handle exception
			}

		}

		biodataCalonMahasiswa = PMBAction.isLoginCalonMahasiswaPMB();

		Common.REAL_PATH = session.getWebApp().getRealPath("/");
		Common.initTemp();
		CommonMedia.getMediaDirectory();
		Common.REAL_PATH_REPORT_TEMP = session.getWebApp().getRealPath("/report");
		Common.ROOT = execution.getContextPath();
		Common.encripSemua();

		if (execution.getParameter("a") != null && !execution.getParameter("a").trim().isEmpty()) {
			try {
				for (Object aa : ConstantValues.ambilBerdasarClass(AfiliasiCalonMahasiswa.class).values()) {
					AfiliasiCalonMahasiswa afiliasiCalonMahasiswa = (AfiliasiCalonMahasiswa) aa;
					if (afiliasiCalonMahasiswa.getAktif()) {
						if (afiliasiCalonMahasiswa.getKode().equalsIgnoreCase(execution.getParameter("a"))) {
							afiliasiCalonMahasiswaData = afiliasiCalonMahasiswa;
						}
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/PMBAction.java:1127");
				// TODO: handle exception
			}
		}

		if (execution.getParameter("g") != null && !execution.getParameter("g").trim().isEmpty()) {
			try {
				for (Object aa : ConstantValues.ambilBerdasarClass(GelombangPendaftaran.class).values()) {
					GelombangPendaftaran gelombangPendaftaran = (GelombangPendaftaran) aa;
					if (gelombangPendaftaran.getAktif()) {
						if (gelombangPendaftaran.getKode().equalsIgnoreCase(execution.getParameter("g"))) {
							gelombangPendaftaranData = gelombangPendaftaran;
						}
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/PMBAction.java:1142");
				// TODO: handle exception
			}
		}

//		System.out.println("execution => " + execution.getParameterMap());

		if (execution.getParameter("alur") != null) {
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onClickAlurPendaftaran();
				}
			});
		} else if (execution.getParameter("formulir") != null) {
			onClickPMB();

		} else if (execution.getParameter("infopembayaran") != null) {

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onClickCariPembayaran();
				}
			});
		} else if (execution.getParameter("login") != null) {
			onClickLoginPMB();

		} else if (execution.getParameter("kelulusan") != null) {

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onClickCariDataUjian();
				}
			});
		} else if (execution.getParameter("bayar") != null) {

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onClickPayment();
				}
			});
		} else if (execution.getParameter("ujian") != null) {

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onClickLoginUjian();
				}
			});
		}

	}

	public void onLihatOnline(Event event) throws Exception {
		DaftarPenggunaOnline window = new DaftarPenggunaOnline();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		window.setVisible(true);
		window.onModal();
	}

	public void onClickPayment() throws Exception {
		Common.displayWindow("/pages/pmb/pembayaran_via_payment_gateway.zul", true, "95%",
				Common.isMobile() ? "100%" : "950px", eventListenerLogin, "", false);
	}

	public void onClickPMB() throws Exception {
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.displayWindow("/pages/master/biodata_calon_mahasiswa.zul", true, "95%",
						Common.isMobile() ? "100%" : "950px", eventListenerLogin, "", false);
			}
		});

	}

	public void onClickCariDataUjian() throws Exception {
		Common.displayWindow("/pages/pmb/cari_data_peserta_ujian.zul", true, "95%",
				Common.isMobile() ? "100%" : "950px", eventListenerLogin, "", false);
	}

	public void onClickCariPembayaran() throws Exception {
		Common.displayWindow("/pages/pmb/cari_data_pembayaran.zul", true, "95%", Common.isMobile() ? "100%" : "950px",
				eventListenerLogin, "", false);
	}

	public void onClickCariDataDaftar() throws Exception {
		Common.displayWindow("/pages/pmb/cari_data_pendaftar.zul", true, "95%", Common.isMobile() ? "100%" : "950px",
				eventListenerLogin, "", false);
	}

	public void onClickLoginPMB() throws Exception {
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.displayWindow("/pages/master/login_calon_mahasiswa.zul", true, "95%",
						Common.isMobile() ? "100%" : "950px", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								// TODO Auto-generated method stub
								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										Executions.getCurrent().sendRedirect("");
									}
								});
							}
						}, "", false);
			}
		});

	}

	public void onClickLoginUjian() throws Exception {
		Common.displayWindow("/pages/master/ujian_online_calon_mahasiswa.zul", true, "95%",
				Common.isMobile() ? "100%" : "950px", eventListenerLogin, "", false);
	}

	public void onClickAlurPendaftaran() throws Exception {
		LampiranLain alurFile = LampiranLain.ambil(LampiranLain.ID_ALUR_REGISTRASI_PMB,
				LampiranLain.ALUR_REGISTRASI_PMB);

		if (alurFile == null) {
			Common.displayWindowIframe("/img/Alur_SPMB_Mandiri.jpg", true, "95%", "95%");
		} else {

			String link = alurFile == null ? "/img/Alur_SPMB_Mandiri.jpg"
					: (alurFile.getLink() == null || alurFile.getLink().isEmpty() ? null : alurFile.getLink());

			if (alurFile != null && (link == null || link.trim().isEmpty() || !link.startsWith("http"))) {
				link = alurFile.createLinkUri();
			}

			if (alurFile != null && alurFile.bisaPreview()) {
				Common.displayWindow(alurFile.merupakanGambar(), link, true, "95%", "95%",
						!alurFile.getNama().toLowerCase().endsWith(".txt"), alurFile);
			} else {
				ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
			}
		}
	}

	private void tampilGelombang(Rows rows, PerguruanTinggi selectedPerguruanTinggi, String ta) {
		Row row = new Row();
		row.setValign("top");
		row.setParent(rows);
		GelombangPendaftaran gelombangPendaftaran = (GelombangPendaftaran) Sessions.getCurrent(true)
				.getAttribute("gelombangPendaftaran");

		if (gelombangPendaftaranData != null) {
			gelombangPendaftaran = gelombangPendaftaranData;
		}

		JenisSeleksi jenisSeleksia = (JenisSeleksi) Sessions.getCurrent(true).getAttribute("jenisSeleksia");
		biodataCalonMahasiswa = PMBAction.isLoginCalonMahasiswaPMB();
		TampilanPengumumanAkademisAction.tampilGelombang(gelombangPendaftaranData, selectedPerguruanTinggi, ta,
				gelombangPendaftaran, jenisSeleksia, afiliasiCalonMahasiswaData, row, biodataCalonMahasiswa, alur,
				informasiPembayaran, loginCalonMhs, informasiKelulusan, pembayaranViaPaymentGateway, mobil);
	}

	private void tampilPengumumanUtama(Rows rows, PerguruanTinggi selectedPerguruanTinggi) {
		List<PengumumanAkademis> listPengumumanAkademis = ConstantValues
				.simpleList(TampilanPengumumanPMBAction.initCriteriaStatic(true, selectedPerguruanTinggi)
						.add(Restrictions.eq("kategoriPengumuman.merupakanPengumumanUtama", true))
						.setMaxResults(Common.ROWS_COUNT_ON_PAGE_1), PengumumanAkademis.class);

		if (!listPengumumanAkademis.isEmpty()) {
			Tbmuser tbmuser = Common.getCurrentUser();
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			TampilanPengumumanAkademisAction.tampil(row, listPengumumanAkademis.get(0), null, tbmuser,
					selectedPerguruanTinggi, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, mobil, true, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							PengumumanAkademis akademis = (PengumumanAkademis) arg0.getTarget()
									.getAttribute("akademis");

							onJalur(akademis);

						}
					});
		}

		// Pengumuman ber-flag "Langsung tampil di Beranda": tampilkan isi PENUH inline di landing PMB
		// (tanpa harus klik judul), sama seperti Papan Pengumuman di Beranda utama.
		try {
			Long utamaId = listPengumumanAkademis.isEmpty() || listPengumumanAkademis.get(0) == null ? null
					: listPengumumanAkademis.get(0).getId();
			List<PengumumanAkademis> listLangsungBeranda = ConstantValues.simpleList(
					TampilanPengumumanPMBAction.initCriteriaStatic(true, selectedPerguruanTinggi)
							.add(Restrictions.eq("langsungTampilBeranda", true)).setMaxResults(20),
					PengumumanAkademis.class);
			final Tbmuser tbmuserBeranda = Common.getCurrentUser();
			for (final PengumumanAkademis item : listLangsungBeranda) {
				if (item == null || item.getId() == null) {
					continue;
				}
				if (utamaId != null && utamaId.equals(item.getId())) {
					continue; // sudah tampil sebagai pengumuman utama di atas
				}
				Row rowBeranda = new Row();
				rowBeranda.setValign("top");
				rowBeranda.setParent(rows);
				TampilanPengumumanAkademisAction.tampil(rowBeranda, item, null, tbmuserBeranda, selectedPerguruanTinggi,
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
							}
						}, mobil, true, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								PengumumanAkademis akademis = (PengumumanAkademis) arg0.getTarget()
										.getAttribute("akademis");
								onJalur(akademis);
							}
						});
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void initBeranda() {
		Common.clear(container);

		final PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		Grid grid = new Grid();
		grid.setSclass("fgrid");
		grid.setParent(container);
		grid.setOddRowSclass("non-odd");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("100%");

		final Rows rows = new Rows();
		rows.setParent(grid);

		String taa = (String) Sessions.getCurrent(true).getAttribute("t");

		if (!Common.bolehKonfigurasi("default_ta_pmb_adalah_semua")) {
			if (taa == null) {
				taa = Common.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik())
						.getNilai();
			}
		}

		if (!Common.bolehKonfigurasi("tampilkan_pilihan_tahun_akademik_pmb")) {
			taa = null;
		}

		final String ta = taa;

		String header = "";
		try {
			if (biodataCalonMahasiswa == null || biodataCalonMahasiswa.getId() == null) {
				if (selectedPerguruanTinggi != null && selectedPerguruanTinggi.getHeaderpmb() != null
						&& !selectedPerguruanTinggi.getHeaderpmb().trim().isEmpty()) {
					header = selectedPerguruanTinggi.getHeaderpmb();
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/PMBAction.java:1389");
			// TODO: handle exception
		}

		if (!header.trim().isEmpty()) {

			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new Html(header));

			row = new Row();
			row.setParent(rows);
			row.setAlign("center");
			MyToolbarbutton daftar = new MyToolbarbutton("fa-check-square", "Daftar Sekarang");
			daftar.getLabelC().setStyle("font-size:20px");
			row.appendChild(daftar);
			daftar.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					Common.clear(rows);

					if (Common.bolehKonfigurasi("posisi_pengumuman_pmb_dibawah_pilihan_daftar")) {
						tampilGelombang(rows, selectedPerguruanTinggi, ta);
						tampilPengumumanUtama(rows, selectedPerguruanTinggi);
					} else {
						tampilPengumumanUtama(rows, selectedPerguruanTinggi);
						tampilGelombang(rows, selectedPerguruanTinggi, ta);
					}

				}
			});

			row = new Row();
			row.setParent(rows);
			row.appendChild(new Space());

			row = new Row();
			row.setParent(rows);
			row.appendChild(new Space());

			row = new Row();
			row.setParent(rows);
			row.appendChild(new Space());

		} else {

			if (Common.bolehKonfigurasi("posisi_pengumuman_pmb_dibawah_pilihan_daftar")) {
				tampilGelombang(rows, selectedPerguruanTinggi, ta);
				tampilPengumumanUtama(rows, selectedPerguruanTinggi);
			} else {
				tampilPengumumanUtama(rows, selectedPerguruanTinggi);
				tampilGelombang(rows, selectedPerguruanTinggi, ta);
			}
		}
	}

	private void initProdi() {

		Common.clear(container);

		PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		Grid grid = new Grid();
		grid.setSclass("dgrid");

		grid.setParent(container);

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		String ta = (String) Sessions.getCurrent(true).getAttribute("t");

		if (ta == null) {
			ta = Common.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik())
					.getNilai();
		}

		if (biodataCalonMahasiswa == null) {

			Session session = HibernateUtil.currentSession();

			List<PaketJurusanPmb> paketsPaketJurusanPmbs = ConstantValues
					.simpleList(session.createCriteria(PaketJurusanPmb.class)

							.add(Restrictions.eq("pilihan1", true)).add(Restrictions.eq("pilihan2", true))
							.add(Restrictions.eq("pilihan3", true)).add(Restrictions.eq("pilihan4", true))
							.add(Restrictions.eq("pilihan5", true))

							.createAlias("paket", "paket").addOrder(Order.asc("paket.nama"))
							.add(Restrictions.isNotNull("paket")).add(Restrictions.eq("paket.aktif", true))
							.add(Restrictions.isNotNull("jurusan")), PaketJurusanPmb.class);

			List<PaketPunyaProgram> paketPunyaPrograms = ConstantValues.simpleList(
					session.createCriteria(PaketPunyaProgram.class).createAlias("paket", "paket")
							.add(Restrictions.eq("paket.aktif", true)).createAlias("program", "program")
							.addOrder(Order.asc("program.namaBaru")).add(Restrictions.isNotNull("paket")),
					PaketPunyaProgram.class);

			List<PilihanPaketPerJurusanMhsBaru> pilihanPaketPerJurusanMhsBarus = ConstantValues
					.simpleList(session.createCriteria(PilihanPaketPerJurusanMhsBaru.class)
							.createAlias("paket", "paket").add(Restrictions.eq("paket.aktif", true))
							.createAlias("jurusanSekolahMahasiswaBaru", "jurusanSekolahMahasiswaBaru")
							.addOrder(Order.asc("jurusanSekolahMahasiswaBaru.jenisSekolahMahasiswaBaru"))
							.add(Restrictions.isNotNull("paket")), PilihanPaketPerJurusanMhsBaru.class);

			List<Jurusan> jurusans = ConstantValues.simpleList(
					session.createCriteria(Jurusan.class).createAlias("fakultas", "fakultas")
							.add(Restrictions.eq("fakultas.perguruanTinggi", selectedPerguruanTinggi))
							.addOrder(Order.asc("fakultas.nama")).addOrder(Order.asc("nama"))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
					Jurusan.class);

			List<PaketPunyaGelombangPendaftaran> paketPunyaGelombangPendaftarans = ConstantValues.simpleList(
					session.createCriteria(PaketPunyaGelombangPendaftaran.class).createAlias("paket", "paket")
							.add(Restrictions.eq("paket.aktif", true))
							.createAlias("gelombangPendaftaran", "gelombangPendaftaran")
							.add(Restrictions.eq("gelombangPendaftaran.tahunAkademik", ta))
							.add(Restrictions.or(
									Restrictions.eq("gelombangPendaftaran.bisaDipilihPendaftarOnline", true),
									Restrictions.isNull("gelombangPendaftaran.bisaDipilihPendaftarOnline")))
							.add(Restrictions.and(
									Restrictions.le("gelombangPendaftaran.mulai", ais.ui.util.WaktuUtil.getDate()),
									Restrictions.ge("gelombangPendaftaran.sampai", ais.ui.util.WaktuUtil.getDate())))
							.add(Restrictions.eq("gelombangPendaftaran.perguruanTinggi", selectedPerguruanTinggi))
							.addOrder(Order.asc("gelombangPendaftaran.nama")).addOrder(Order.asc("paket.nama"))
							.add(Restrictions.eq("gelombangPendaftaran.aktif", true)),
					PaketPunyaGelombangPendaftaran.class);

			Set<Long> gel = new HashSet<Long>();
			for (PaketPunyaGelombangPendaftaran punyaGelombangPendaftaran : paketPunyaGelombangPendaftarans) {
				if (punyaGelombangPendaftaran != null && punyaGelombangPendaftaran.getGelombangPendaftaran() != null) {
					gel.add(punyaGelombangPendaftaran.getGelombangPendaftaran().getId());
				}
			}

			List<GelombangPendaftaran> gelombangPendaftarans = ConstantValues.simpleList(
					session.createCriteria(GelombangPendaftaran.class).add(Restrictions.eq("tahunAkademik", ta))
							.add(gel.isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.not(Restrictions.in("id", gel)))
							.add(Restrictions.or(Restrictions.eq("bisaDipilihPendaftarOnline", true),
									Restrictions.isNull("bisaDipilihPendaftarOnline")))
							.add(Restrictions.and(Restrictions.le("mulai", ais.ui.util.WaktuUtil.getDate()),
									Restrictions.ge("sampai", ais.ui.util.WaktuUtil.getDate())))
							.add(Restrictions.eq("perguruanTinggi", selectedPerguruanTinggi))
							.addOrder(Order.asc("nama"))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							GelombangPendaftaran.class);

			Fakultas fakultas = null;
			for (final Jurusan jurusan : jurusans) {

				if (fakultas == null || (jurusan != null && jurusan.getFakultas() != null
						&& !jurusan.getFakultas().getId().equals(fakultas.getId()))) {
					Group group = new Group(jurusan.getFakultas().getNama());
					group.setParent(rows);

					fakultas = jurusan.getFakultas();
				}

				List<Paket> pakets = new ArrayList<Paket>();
				List<Long> paketIds = new ArrayList<Long>();
				for (PaketJurusanPmb paketJurusanPmb : paketsPaketJurusanPmbs) {
					if (paketJurusanPmb.getJurusan().getId().equals(jurusan.getId())
							&& !paketIds.contains(paketJurusanPmb.getPaket().getId())) {
						if (paketJurusanPmb.getPilihan1() && paketJurusanPmb.getPilihan2()
								&& paketJurusanPmb.getPilihan3() && paketJurusanPmb.getPilihan4()
								&& paketJurusanPmb.getPilihan5()) {
							pakets.add(paketJurusanPmb.getPaket());
							paketIds.add(paketJurusanPmb.getPaket().getId());
						}
					}
				}

				Row row = new Row();
				row.setValign("top");
				row.setParent(rows);

				Groupbox vbox = new ais.ui.util.MyGroupboxStyled();
				vbox.setWidth("90%");
				vbox.setParent(row);

				Vbox vbox2 = new Vbox();
				vbox2.setParent(vbox);

				MyLabelAgakKecil aa;
				vbox2.appendChild(aa = new MyLabelAgakKecil(
						(jurusan.getJenjang() == null ? "" : jurusan.getJenjang().getNama() + " - ")
								+ jurusan.getNama()));
				aa.setStyle("font-size:16px;font-weight: bold;");
				EventListener eventListenerD = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						JurusanAction.onInfo(jurusan);
					}
				};

				vbox2.appendChild(new Html(jurusan.getDeskripsi()));

				vbox2.appendChild(new Space());

				String info = "<ul style=\"font-size: small;font-family: Poppins, Helvetica, 'sans-serif';\">";

				if (!pakets.isEmpty()) {
					String p = "";
					for (Paket paket : pakets) {
						p += p.isEmpty() ? paket.getNama() : ", " + paket.getNama();
					}
					info += "<li>Paket yang tersedia : " + p + "</li>";
				}

				TreeMap<Long, GelombangPendaftaran> pilihGelombangPendaftaran = new TreeMap<Long, GelombangPendaftaran>();
				TreeMap<Long, JenisSeleksi> pilihJenisSeleksi = new TreeMap<Long, JenisSeleksi>();
				for (GelombangPendaftaran g : gelombangPendaftarans) {
					pilihGelombangPendaftaran.put(g.getId(), g);
				}

				for (PaketPunyaGelombangPendaftaran paketPunyaGelombangPendaftaran : paketPunyaGelombangPendaftarans) {
					if (paketIds.contains(paketPunyaGelombangPendaftaran.getPaket().getId())
							&& !pilihGelombangPendaftaran
									.containsKey(paketPunyaGelombangPendaftaran.getGelombangPendaftaran().getId())) {
						pilihGelombangPendaftaran.put(paketPunyaGelombangPendaftaran.getGelombangPendaftaran().getId(),
								paketPunyaGelombangPendaftaran.getGelombangPendaftaran());
					}
				}

				if (!pilihGelombangPendaftaran.isEmpty()) {
					String p = "";
					for (GelombangPendaftaran s : pilihGelombangPendaftaran.values()) {
						p += p.isEmpty() ? s.getNama() + " (" + s.getTahunAkademik() + "/" + s.getJenisSemester() + ")"
								: ", " + s.getNama() + " (" + s.getTahunAkademik() + "/" + s.getJenisSemester() + ")";

						for (JenisSeleksi jenisSeleksi : s.ambilJenisSeleksi()) {
							if (!pilihJenisSeleksi.containsKey(jenisSeleksi.getId())) {
								pilihJenisSeleksi.put(jenisSeleksi.getId(), jenisSeleksi);
							}
						}

					}
					info += "<li>Gelombang yang tersedia : " + p + "</li>";

					p = "";
					for (JenisSeleksi jenisSeleksi : pilihJenisSeleksi.values()) {
						p += p.isEmpty() ? jenisSeleksi.getNama() : ", " + jenisSeleksi.getNama();
					}
					info += "<li>Jenis seleksi yang tersedia : " + p + "</li>";
				}

				List<String> programs = new ArrayList<String>();
				for (PaketPunyaProgram paketPunyaProgram : paketPunyaPrograms) {
					if (paketIds.contains(paketPunyaProgram.getPaket().getId())
							&& !programs.contains(paketPunyaProgram.getProgram().getNamaBaru())) {
						programs.add(paketPunyaProgram.getProgram().getNamaBaru());
					}
				}

				if (programs != null && !programs.isEmpty()) {
					String p = "";
					for (String program : programs) {
						p += p.isEmpty() ? program : ", " + program;
					}
					info += "<li>Program yang tersedia : " + p + "</li>";
				}

				List<JurusanSekolahMahasiswaBaru> jurusanSekolahMahasiswaBarus = new ArrayList<JurusanSekolahMahasiswaBaru>();
				for (PilihanPaketPerJurusanMhsBaru pilihanPaketPerJurusanMhsBaru : pilihanPaketPerJurusanMhsBarus) {
					if (paketIds.contains(pilihanPaketPerJurusanMhsBaru.getPaket().getId())
							&& !jurusanSekolahMahasiswaBarus
									.contains(pilihanPaketPerJurusanMhsBaru.getJurusanSekolahMahasiswaBaru())) {
						jurusanSekolahMahasiswaBarus
								.add(pilihanPaketPerJurusanMhsBaru.getJurusanSekolahMahasiswaBaru());
					}
				}

				if (jurusanSekolahMahasiswaBarus != null && !jurusanSekolahMahasiswaBarus.isEmpty()) {
					String p = "";
					for (JurusanSekolahMahasiswaBaru jurusanSekolahMahasiswaBaru : jurusanSekolahMahasiswaBarus) {
						p += p.isEmpty()
								? jurusanSekolahMahasiswaBaru.getJenisSekolahMahasiswaBaru().getNama() + " ("
										+ jurusanSekolahMahasiswaBaru.getNama() + ")"
								: ", " + jurusanSekolahMahasiswaBaru.getJenisSekolahMahasiswaBaru().getNama() + " ("
										+ jurusanSekolahMahasiswaBaru.getNama() + ")";
					}

					info += "<li>Tersedia untuk lulusan : " + p + "</li>";
				}

				info += "</ul>";

				vbox2.appendChild(new Html(info));

				vbox2.appendChild(new Space());

				A a;
				vbox2.appendChild(a = new A("Lihat Informasi Rinci", "/img/svg/eye.svg"));

				a.setStyle("font-size:12px;");

				a.addEventListener("onClick", eventListenerD);

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

		PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		Criteria criteria = TampilanPengumumanPMBAction.initCriteriaStatic(true, selectedPerguruanTinggi)
				.add(Restrictions.or(Restrictions.isNull("langsungMunculDiTab"),
						Restrictions.eq("langsungMunculDiTab", false)))
				.add(Restrictions.or(Restrictions.isNull("kategoriPengumuman"),
						Restrictions.or(Restrictions.isNull("kategoriPengumuman.merupakanPengumumanUtama"),
								Restrictions.eq("kategoriPengumuman.merupakanPengumumanUtama", false))));
		TampilanPengumumanAkademisAction.loadData(rows, cari.getValue().trim(), new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] obj = (Object[]) arg0.getData();
				Long p = (Long) obj[0];
				Row row = (Row) obj[1];
				Clients.scrollIntoView(row);

				PengumumanAkademis pengumumanAkademis = (PengumumanAkademis) ConstantValues
						.ambil(PengumumanAkademis.class.getName(), p);

				onJalur(pengumumanAkademis);

				if (!cari.getValue().trim().isEmpty()) {
					cari.setValue("");
					initMenuPengumuman(cari);
				}
			}
		}, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				PengumumanAkademis pengumumanAkademis = (PengumumanAkademis) arg0.getData();
				TampilanPengumumanAkademisAction.prosess(pengumumanAkademis.getId(), tabs, tabpanels, true, container);

				MyMessageboxConfig.showFormat(
						"Terdapat Polling / Jejak Pendapat \"{V1}\". Mohon Bapak/Ibu berkenan melengkapi Polling / Jejak Pendapat berikut terlebih dahulu.",
						"Polling / Jejak Pendapat", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
						pengumumanAkademis.getJudul());
			}
		}, criteria, tbmuser, null, Common.isMobile(), "border:0px;background:#F5F5F5 repeat-x 0 0;");

	}

	@SuppressWarnings({ })
	public void onJalur(PengumumanAkademis selectedPengumumanAkademis) throws Exception {

		Common.clear(container);

		Grid grid = new Grid();
		grid.setSclass("fgrid");
		grid.setParent(container);
		grid.setWidth("100%");

		Rows rows = new Rows();
		rows.setParent(grid);
		Row row = new Row();
		row.setValign("top");
		row.setParent(rows);
		row.setValign("top");

		Columns columns = new Columns();
		columns.setParent(grid);

		if (!mobil) {

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("215px");

			column = new MyColumnConfig();
			column.setParent(columns);

			rowPencarian = Common.tampilanScroll1(row);

			rowPencarian.setStyle("border:0px;background: transparent;");
			rowPencarian.getGrid().setStyle("border:0px;background: transparent;");

			rowDicari = new Row();
			rowDicari.setStyle("border:0px;background: transparent;");
			rowDicari.setParent(rowPencarian.getParent());

			rowPengumuman = new Row();
			rowPengumuman.setStyle("border:0px;background: transparent;");
			rowPengumuman.setParent(rowPencarian.getParent());

			initPencarianMenu();
		} else {
			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("100%");
		}

		PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		if (selectedPengumumanAkademis != null) {
			Tbmuser tbmuser = Common.getCurrentUser();

			TampilanPengumumanAkademisAction.tampil(row, selectedPengumumanAkademis, null, tbmuser,
					selectedPerguruanTinggi, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, mobil, true, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							PengumumanAkademis akademis = (PengumumanAkademis) arg0.getTarget()
									.getAttribute("akademis");

							onJalur(akademis);

						}
					});
		} else {

			List<PengumumanAkademis> listPengumumanAkademis = ConstantValues
					.simpleList(TampilanPengumumanPMBAction.initCriteriaStatic(true, selectedPerguruanTinggi)
							.add(Restrictions.or(Restrictions.isNull("langsungMunculDiTab"),
									Restrictions.eq("langsungMunculDiTab", false)))
							.add(Restrictions.or(Restrictions.isNull("kategoriPengumuman"),
									Restrictions.or(Restrictions.isNull("kategoriPengumuman.merupakanPengumumanUtama"),
											Restrictions.eq("kategoriPengumuman.merupakanPengumumanUtama", false))))
							.setMaxResults(Common.ROWS_COUNT_ON_PAGE_1), PengumumanAkademis.class);

			if (!listPengumumanAkademis.isEmpty()) {

				Tbmuser tbmuser = Common.getCurrentUser();

				TampilanPengumumanAkademisAction.tampil(row, listPengumumanAkademis.get(0), null, tbmuser,
						selectedPerguruanTinggi, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

							}
						}, mobil, true, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								PengumumanAkademis akademis = (PengumumanAkademis) arg0.getTarget()
										.getAttribute("akademis");

								onJalur(akademis);

							}
						});

			} else {
				Html html = new ais.ui.util.MyHtml(
						"<strong><font style='color:red'>Belum ada pengumuman peneriman mahasiswa baru</font></strong><br><br><br>");
				html.setHeight("150px");
				html.setWidth("100%");
				html.setParent(row);
			}
		}

	}

	// Guard anti double-proses: onInfo (ClientInfoEvent) pada ZK bisa terpicu LEBIH DARI SEKALI
	// per pemuatan halaman (mis. saat resize/orientation change dikirim ulang oleh client, atau
	// tab/klik ganda pada form pendaftaran PMB). Tanpa guard ini, 2 thread bisa memanggil
	// buatTagihanCalonMahasiswaJikaBelumAda utk calon mahasiswa id YANG SAMA nyaris bersamaan,
	// berebut lock baris Kegiatan/BiodataCalonMahasiswa yang sama -> lock timeout (55P03) atau
	// deadlock (40P01) di updateEntitySafe. Guard ini TAMBAHAN saja (bukan pengganti retry di
	// KegiatanHelper.updateEntitySafe) -- kalau proses lain utk id yang sama masih berjalan,
	// panggilan kedua dilewati saja (idempoten: tagihan akan tetap tersinkron pada percobaan
	// berikutnya / batch), bukan menunggu/queue.
	private static final java.util.Set<Long> CAMA_SEDANG_DIPROSES_TAGIHAN = java.util.Collections
			.synchronizedSet(new java.util.HashSet<Long>());

	private void buatTagihanCalonMahasiswaJikaBelumAda(BiodataCalonMahasiswa cama) {
		if (cama == null || cama.getId() == null) return;
		if (!CAMA_SEDANG_DIPROSES_TAGIHAN.add(cama.getId())) {
			// Sudah ada proses lain (thread/tab lain) yang sedang membuat tagihan utk calon
			// mahasiswa id ini -- lewati saja, jangan ikut berebut lock baris yang sama.
			return;
		}
		org.hibernate.Session s = null;
		try {
			s = HibernateUtil.openSession();
			BiodataCalonMahasiswa camaFresh = (BiodataCalonMahasiswa) s.get(BiodataCalonMahasiswa.class, cama.getId());
			if (camaFresh == null) return;
			if (ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null) {
				try {
					KegiatanHelper.checkKegiatanCalonMahasiswa(
							ConstantValues.PENDAFTARAN_CALON_MAHASISWA, camaFresh,
							0, camaFresh.getTahunAkademik(), Boolean.TRUE, false, null, s);
				} catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/action/maintenance/PMBAction.java:1890"); /* abaikan */ }
			}
			if (ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null) {
				try {
					KegiatanHelper.checkKegiatanCalonMahasiswa(
							ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU, camaFresh,
							1, camaFresh.getTahunAkademik(), Boolean.TRUE, false, null, s);
				} catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/maintenance/PMBAction.java:1897"); /* abaikan */ }
			}
		} catch (Exception e) {
			System.err.println("[PMBAction] buatTagihanCalonMahasiswaJikaBelumAda gagal: " + e.getMessage());
		} finally {
			if (s != null && s.isOpen()) {
				try { s.clear(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/maintenance/PMBAction.java:1903");}
				try { s.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/maintenance/PMBAction.java:1904");}
			}
			CAMA_SEDANG_DIPROSES_TAGIHAN.remove(cama.getId());
		}
	}

}
