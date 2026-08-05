package ais.action.maintenance;

import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
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
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
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

import ais.action.master.TampilanPengumumanAkademisAction;
import ais.action.master.helper.DaftarPenggunaOnline;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.psb.TampilanPengumumanPSBAction;
import ais.action.master.sekolah.CalonSiswaAction;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.SecurityFilter;
import ais.common.SessionCounter;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.PengumumanAkademis;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.CheckForParentScript;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class PSBAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 187958355469911830L;

	private Div kopBox;
	private Div container;
	private Div footer;
	private Row wacontainer;

	private Toolbar menubar;

	private MyToolbarbutton alur;
	private MyToolbarbutton formulir;
	private MyToolbarbutton informasiPembayaran;
	private MyToolbarbutton loginCalonMhs;
	private MyToolbarbutton informasiKelulusan;

	private CalonSiswa calonSiswa = null;

	// private Vbox menubar;

	private MyToolbarbutton pembayaranViaPaymentGateway;

	private MyToolbarbutton refreshdata;

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

	public static void footer(Div parent) {
		Sekolah sekolah = SekolahUtil.getSekolah();
		Yayasan yayasan = SekolahUtil.getYayasan();
		String telp = "";
		String notelp = Common.getKonfigurasi("no_whatsapp_operator", "").getNilai();
		String email = "";

		String styleCopyright = ais.common.Common
				.getKonfigurasi("footer_style_ppdb",
						"font-family: 'Open Sans', sans-serif;font-size: 12px;color:white;font-weight: bold;")
				.getNilai();

		String styleCopyrightMotto = ais.common.Common.getKonfigurasi("footer_motto_style_ppdb_sekolah",
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

		PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		String judul = selectedPerguruanTinggi == null ? "" : selectedPerguruanTinggi.getNama();
		String Motto = selectedPerguruanTinggi == null || selectedPerguruanTinggi.getMotto() == null
				|| selectedPerguruanTinggi.getMotto().trim().isEmpty() ? "eCampus Information System"
						: selectedPerguruanTinggi.getMotto();
		String Alamat1 = selectedPerguruanTinggi == null ? "" : selectedPerguruanTinggi.getAlamat1();

		if (sekolah != null && sekolah.getId() != null) {
			judul = sekolah.getNama();

			if (sekolah.getMotto() != null && !sekolah.getMotto().trim().isEmpty()) {
				Motto = sekolah.getMotto();
			}

			if (sekolah.getAlamat() != null && !sekolah.getAlamat().trim().isEmpty()) {
				Alamat1 = sekolah.getAlamat();
			}

		}

		else if (yayasan != null && yayasan.getId() != null) {
			judul = yayasan.getNama();

			if (yayasan.getMotto() != null && !yayasan.getMotto().trim().isEmpty()) {
				Motto = yayasan.getMotto();
			}

			if (yayasan.getAlamat() != null && !yayasan.getAlamat().trim().isEmpty()) {
				Alamat1 = yayasan.getAlamat();
			}

		}

		final String nama = judul;

		Label motto = new Label();
		motto.setStyle(styleCopyrightMotto);
		motto.setParent(row);
		motto.setValue(Motto);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);

		Label alamat = new Label();
		alamat.setStyle(styleCopyright);
		alamat.setParent(row);
		alamat.setValue(Alamat1);

		if (sekolah != null && sekolah.getId() != null && !sekolah.getWa().isEmpty()) {
			notelp = sekolah.getWa();
		} else if (yayasan != null && yayasan.getId() != null && !yayasan.getWa().isEmpty()) {
			notelp = yayasan.getWa();
		} else if (selectedPerguruanTinggi != null && selectedPerguruanTinggi.getId() != null
				&& !selectedPerguruanTinggi.getWa().isEmpty()) {
			notelp = selectedPerguruanTinggi.getWa();
		}

		if (sekolah != null && sekolah.getId() != null && !sekolah.getTelp().isEmpty()) {
			telp = sekolah.getTelp();
		} else if (yayasan != null && yayasan.getId() != null && !yayasan.getTelp().isEmpty()) {
			telp = yayasan.getTelp();
		} else if (selectedPerguruanTinggi != null && selectedPerguruanTinggi.getId() != null
				&& !selectedPerguruanTinggi.getTelepon().isEmpty()) {
			telp = selectedPerguruanTinggi.getTelepon();
		}

		if (sekolah != null && sekolah.getId() != null && !sekolah.getEmail().isEmpty()) {
			email = sekolah.getEmail();
		} else if (yayasan != null && yayasan.getId() != null && !yayasan.getEmail().isEmpty()) {
			email = yayasan.getEmail();
		} else if (selectedPerguruanTinggi != null && selectedPerguruanTinggi.getId() != null
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

						String linkWa = "tel:" + hp;

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

		copyright.setValue(

				sekolah != null && sekolah.getId() != null ? ("Copyright "
						+ java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + " " + sekolah.getNama()) :

						yayasan != null && yayasan.getId() != null
								? ("Copyright " + java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + " "
										+ yayasan.getNama())
								:

								selectedPerguruanTinggi == null ? ""
										: ("Copyright " + java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
												+ " " + selectedPerguruanTinggi.getNama()));

	}

	public static void tampilWa(Row wacontainer) {
		Sekolah sekolah = SekolahUtil.getSekolah();
		Yayasan yayasan = SekolahUtil.getYayasan();
		PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		String notelp = Common.getKonfigurasi("no_whatsapp_operator", "").getNilai();
		if (sekolah != null && sekolah.getId() != null && !sekolah.getWa().isEmpty()) {
			notelp = sekolah.getWa();
		} else if (yayasan != null && yayasan.getId() != null && !yayasan.getWa().isEmpty()) {
			notelp = yayasan.getWa();
		} else if (selectedPerguruanTinggi != null && selectedPerguruanTinggi.getId() != null
				&& !selectedPerguruanTinggi.getWa().isEmpty()) {
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

		if (notelp != null && !notelp.trim().isEmpty()) {

			String tanya = Common.getKonfigurasi("tanya_whatsapp_psb", "Salamat Datang, apa yang bisa kami bantu?")
					.getNilai();

			String jawab = Common
					.getKonfigurasi("jawab_whatsapp_psb",
							"Saya ingin menanyakan tentang informasi penerimaan siswa baru, apakah Anda bisa membantu?")
					.getNilai();

			if (sekolah != null) {
				tanya = sekolah.getTanyaWhatsapp();
				jawab = sekolah.getJawabWhatsappPsb();
			}

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
			html.setParent(wacontainer);
		}

	}

	public static Grid header() {
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

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setHeight("100%");
		grid.setSclass("fgrid");
		grid.setStyle("border:0px;background: transparent;");

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
		row.appendChild(imgLogo = new Image(image == null ? "img/logo_psb.png" : image));
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
				Common.getKonfigurasi("label_psb_kampus", "Seleksi Penerimaan Siswa Baru").getNilai());
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

		return grid;
	}

	public static Hbox headerBox() {
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

		Hbox hbox = new Hbox();
		hbox.setSclass("pmb-brand-box");
		hbox.setSpacing("4px");

		Image imgLogo;
		hbox.appendChild(imgLogo = new Image(image == null ? "img/logo_psb.png" : image));
		hbox.setWidth("100%");
		hbox.setHeight("90px");

		Vbox vbox = new Vbox();
		vbox.setSclass("pmb-brand-text");
		vbox.setWidth("100%");
		vbox.setPack("center");
		hbox.appendChild(vbox);

		imgLogo.setHeight("60px");
		imgLogo.setSclass("pmb-brand-logo");

		Label namaSeleksi = new Label(label_instansi_sekolah == null
				? Common.getKonfigurasi("label_universitas", "Nama Instansi Kampus").getNilai()
				: label_instansi_sekolah);
		vbox.appendChild(namaSeleksi);

		Label namaSekolah = new Label(
				Common.getKonfigurasi("label_psb_kampus", "Seleksi Penerimaan Siswa Baru").getNilai());
		vbox.appendChild(namaSekolah);

		namaSeleksi.setSclass("title1");
		namaSekolah.setSclass("motto");

		return hbox;
	}

	public static void initHeader(Div kopBox) {
		Common.clear(kopBox);

		try {
			Sekolah sekolah = SekolahUtil.getSekolah();

			if ((sekolah != null && sekolah.getId() != null)) {
				LampiranLain kop = LampiranLain.ambil(sekolah.getId(), LampiranLain.KOP_PPDB_SEKOLAH);
				if (kop != null && kop.getId() != null) {

					Image image = new Image(kop.createLinkUri());
					image.setWidth("100%");
					kopBox.appendChild(image);

				} else {
					kop = LampiranLain.ambil(sekolah.getYayasan().getId(), LampiranLain.KOP_PPDB_YAYASAN);
					if (kop != null && kop.getId() != null) {
						Image image = new Image(kop.createLinkUri());
						image.setWidth("100%");
						kopBox.appendChild(image);
					} else {
						Hbox hbox = PSBAction.headerBox();
						kopBox.appendChild(hbox);
					}
				}
			} else {
				Yayasan yayasan = SekolahUtil.getYayasan();
				if ((yayasan != null && yayasan.getId() != null)) {
					LampiranLain kop = LampiranLain.ambil(yayasan.getId(), LampiranLain.KOP_PPDB_YAYASAN);
					if (kop != null && kop.getId() != null) {
						Image image = new Image(kop.createLinkUri());
						image.setWidth("100%");
						kopBox.appendChild(image);
					} else {
						Hbox hbox = PSBAction.headerBox();
						kopBox.appendChild(hbox);
					}
				} else {
					Hbox hbox = PSBAction.headerBox();
					kopBox.appendChild(hbox);
				}
			}
		} catch (Exception e) {
			Hbox hbox = PSBAction.headerBox();
			kopBox.appendChild(hbox);
		}

	}

	public static void initBg(Div container) {

		container.setStyle("background:url('" + Common.ROOT
				+ "/img/bg_default.jpg') no-repeat center center fixed;-webkit-background-size: cover;-moz-background-size: cover;background-size: cover;-o-background-size: cover;");

		try {
			Sekolah sekolah = SekolahUtil.getSekolah();

			if ((sekolah != null && sekolah.getId() != null)) {
				LampiranLain kop = LampiranLain.ambil(sekolah.getId(), LampiranLain.BG_PPDB_SEKOLAH);
				if (kop != null && kop.getId() != null) {

					container.setStyle("background:url('" + kop.createLinkUri(true, true)
							+ "') no-repeat center center fixed;-webkit-background-size: cover;-moz-background-size: cover;background-size: cover;-o-background-size: cover;");

				} else {
					kop = LampiranLain.ambil(sekolah.getYayasan().getId(), LampiranLain.BG_PPDB_YAYASAN);
					if (kop != null && kop.getId() != null) {
						container.setStyle("background:url('" + kop.createLinkUri(true, true)
								+ "') no-repeat center center fixed;-webkit-background-size: cover;-moz-background-size: cover;background-size: cover;-o-background-size: cover;");

					}
				}
			} else {
				Yayasan yayasan = SekolahUtil.getYayasan();
				if ((yayasan != null && yayasan.getId() != null)) {
					LampiranLain kop = LampiranLain.ambil(yayasan.getId(), LampiranLain.BG_PPDB_YAYASAN);
					if (kop != null && kop.getId() != null) {
						container.setStyle("background:url('" + kop.createLinkUri(true, true)
								+ "') no-repeat center center fixed;-webkit-background-size: cover;-moz-background-size: cover;background-size: cover;-o-background-size: cover;");

					}
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

	}

	public static void initFooter(Div footerBox) {
		Common.clear(footerBox);

		try {
			Sekolah sekolah = SekolahUtil.getSekolah();

			if ((sekolah != null && sekolah.getId() != null)) {
				LampiranLain footer = LampiranLain.ambil(sekolah.getId(), LampiranLain.FOOTER_PPDB_SEKOLAH);
				if (footer != null && footer.getId() != null) {

					Image image = new Image(footer.createLinkUri());
					image.setWidth("100%");
					footerBox.appendChild(image);

				} else {
					footer = LampiranLain.ambil(sekolah.getYayasan().getId(), LampiranLain.FOOTER_PPDB_YAYASAN);
					if (footer != null && footer.getId() != null) {
						Image image = new Image(footer.createLinkUri());
						image.setWidth("100%");
						footerBox.appendChild(image);
					} else {
						PSBAction.footer(footerBox);
					}
				}
			} else {
				Yayasan yayasan = SekolahUtil.getYayasan();
				if ((yayasan != null && yayasan.getId() != null)) {
					LampiranLain footer = LampiranLain.ambil(yayasan.getId(), LampiranLain.FOOTER_PPDB_YAYASAN);
					if (footer != null && footer.getId() != null) {
						Image image = new Image(footer.createLinkUri());
						image.setWidth("100%");
						footerBox.appendChild(image);
					} else {
						PSBAction.footer(footerBox);
					}
				} else {
					PSBAction.footer(footerBox);
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

	}

	private Integer desktopHeight = null;
	private Integer desktopWidth = null;

	private boolean mobil = false;

	@SuppressWarnings("unchecked")
	public void onInfo(ClientInfoEvent evt) throws Exception {

		if (!container.getChildren().isEmpty()) {
			return;
		}

		desktopHeight = evt.getDesktopHeight();
		desktopWidth = evt.getDesktopWidth();

		System.out.println("desktopHeight => " + desktopHeight + ", desktopWidth => " + desktopWidth);

		HttpServletRequest request = (HttpServletRequest) execution.getNativeRequest();

		Common.clear(container);

		PSBAction.initHeader(kopBox);
		PSBAction.initBg(container);
		PSBAction.tampilWa(wacontainer);
		PSBAction.initFooter(footer);

		final MyToolbarbuttonConfig menuBeranda = new MyToolbarbuttonConfig("Beranda", "/img/svg/home_door.svg");
		final MyToolbarbuttonConfig menuPengumuman = new MyToolbarbuttonConfig("Pengumuman",
				"/img/svg/information-circle-outline.svg");

		menuBeranda.setStyle("font-size:12px;");
		menuPengumuman.setStyle("font-size:12px;");

		menubar.appendChild(menuBeranda);
		menubar.appendChild(menuPengumuman);

		if (desktopWidth < ConstantValues.UKURAN_BATAS_MOBILE) {
			mobil = true;
		} else {
			mobil = false;
		}

		EventListener menuEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(container);
				if (arg0.getTarget() == menuBeranda) {
					initProdi();
				} else if (arg0.getTarget() == menuPengumuman) {
					onPengumuman(null);
				}

			}
		};

		menuBeranda.addEventListener("onClick", menuEventListener);
		menuPengumuman.addEventListener("onClick", menuEventListener);

		Sekolah sekolah = SekolahUtil.getSekolah(request);
		Yayasan yayasan = SekolahUtil.getYayasan(request);
		List<PengumumanAkademis> listPengumumanAkademisLangsung = ConstantValues.simpleList(TampilanPengumumanPSBAction
				.initCriteriaStatic(true, sekolah, yayasan).add(Restrictions.eq("langsungMunculDiTab", true)),
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
					Sekolah sekolah = SekolahUtil.getSekolah();
					Tbmuser tbmuser = Common.getCurrentUser();
					PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
					TampilanPengumumanAkademisAction.tampil(container, pengumumanAkademis, sekolah, tbmuser,
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
//		alur.setSclass("user_button_profile");
		alur.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onClickAlurPendaftaran();
			}
		});

		formulir = new MyToolbarbutton("fa-pencil-square-o", Common.getBahasaConfig("Formulir Pendaftaran"));
//		formulir.setSclass("user_button_profile");
		formulir.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onClickPMB();
			}
		});

		informasiPembayaran = new MyToolbarbutton("fa-money", Common.getBahasaConfig("Info Pembayaran"));
//		informasiPembayaran.setSclass("user_button_profile");
		informasiPembayaran.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onClickCariPembayaran();
			}
		});

		loginCalonMhs = new MyToolbarbutton("fa-paperclip",
				Common.getBahasaConfig("Lengkapi Biodata dan Berkas"));
//		loginCalonMhs.setSclass("user_button_profile");
		loginCalonMhs.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				calonSiswa = Common.isLoginCalonSiswa();
				if (calonSiswa != null) {

					if (!GelombangPendaftaranPsb.chekSyaratBayar(calonSiswa)) {
						return;
					}

					EventListener eventListenerPerubahan = new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							CalonSiswa calonSiswa = (CalonSiswa) arg0.getData();
							Common.masukkanSession(CalonSiswa.class, calonSiswa);
						}
					};

					CalonSiswaAction.onAddExternal(null, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							// TODO Auto-generated method stub

						}
					}, eventListenerPerubahan, calonSiswa, null, null, null, calonSiswa.getGelombangPendaftaranPsb());
				}
			}
		});

		informasiKelulusan = new MyToolbarbutton("fa-check-square",
				Common.getBahasaConfig("Informasi Kelulusan"));
//		informasiKelulusan.setSclass("user_button_profile");
		informasiKelulusan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (!GelombangPendaftaranPsb.chekSyaratBayar(calonSiswa)) {
					return;
				}
				onClickCariDataUjian();
			}
		});

		pembayaranViaPaymentGateway = new MyToolbarbutton("fa-credit-card-alt",
				Common.getBahasaConfig("Proses Pembayaran"));
//		pembayaranViaPaymentGateway.setSclass("user_button_profile");

		pembayaranViaPaymentGateway.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onClickPayment();
			}
		});

		refreshdata = new MyToolbarbutton("fa-refresh", Common.getBahasaConfig("Refresh"));
		refreshdata.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				HibernateUtil.currentSession().refresh(calonSiswa);
				Common.setLogin(calonSiswa);
				Executions.getCurrent().sendRedirect("");
			}
		});

		if (alur != null) {
			alur.setVisible(Common.bolehKonfigurasi("tampilkan_alur_psb"));
		}
		if (formulir != null) {
			formulir.setVisible(Common.bolehKonfigurasi("tampilkan_formulir_psb"));
		}
		if (informasiPembayaran != null) {
			informasiPembayaran.setVisible(Common.bolehKonfigurasi("tampilkan_informasiPembayaran_psb"));
		}
		if (loginCalonMhs != null) {
			loginCalonMhs.setVisible(Common.bolehKonfigurasi("tampilkan_loginCalonMhs_psb"));
		}
		if (informasiKelulusan != null) {
			informasiKelulusan.setVisible(Common.bolehKonfigurasi("tampilkan_informasiKelulusan_psb"));
		}

		session.setAttribute(Attributes.PREFERRED_LOCALE, new Locale("in", "ID"));
		session.removeAttribute("usersTemp");

		initProdi();

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

	private EventListener eventListenerLogin = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {
			// TODO Auto-generated method stub

		}
	};

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

		if (request.getParameter("uid") != null && !request.getParameter("uid").trim().isEmpty()) {

			try {
				String userId = Common.desEncrypter.get().decrypt(request.getParameter("uid").trim());
				Tbmuser tbmuser = (Tbmuser) ConstantValues.ambil(Tbmuser.class.getName(), userId);
				if (tbmuser != null && tbmuser.getUserId() != null) {

					final String linkProfile = "ppdb?r=" + Common.randLong();
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
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/PSBAction.java:984");
				// TODO: handle exception
			}

		}

		calonSiswa = Common.isLoginCalonSiswa();

		Common.initBahasaParameter(execution.getParameter("lang"));
		Common.REAL_PATH = session.getWebApp().getRealPath("/");
		Common.initTemp();
		CommonMedia.getMediaDirectory();
		Common.REAL_PATH_REPORT_TEMP = session.getWebApp().getRealPath("/report");
//		System.out.println("execution => " + execution.getParameterMap());
		Common.ROOT = execution.getContextPath();
		Common.encripSemua();
		if (execution.getParameter("alur") != null) {
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onClickAlurPendaftaran();
				}
			});
		} else if (execution.getParameter("formulir") != null) {

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onClickPMB();
				}
			});
		} else if (execution.getParameter("infopembayaran") != null) {

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onClickCariPembayaran();
				}
			});
		} else if (execution.getParameter("login") != null) {

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onClickLoginPMB();
				}
			});
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
		}

	}

	public void onLihatOnline(Event event) throws Exception {
		DaftarPenggunaOnline window = new DaftarPenggunaOnline();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		window.setVisible(true);
		window.onModal();
	}

	public void onClickPayment() throws Exception {
		calonSiswa = Common.isLoginCalonSiswa();
		Common.displayWindow("/pages/master/sekolah/pembayaran_online.zul?calon_siswa=" + calonSiswa.getId(), true,
				"95%", "95%", eventListenerLogin, "", false);
	}

	public void onClickInformasi() throws Exception {
		Common.displayWindow("/pages/psb/informasi.zul", true, "95%", Common.isMobile() ? "100%" : "950px",
				eventListenerLogin, "", false);
	}

	public void onClickPMB() throws Exception {
		calonSiswa = new CalonSiswa();

		EventListener eventListenerPerubahan = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				CalonSiswa calonSiswa = (CalonSiswa) arg0.getData();
				Common.masukkanSession(CalonSiswa.class, calonSiswa);
			}
		};

		CalonSiswaAction.onAddExternal(null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub

			}
		}, eventListenerPerubahan, calonSiswa, null,
				desktopWidth < ConstantValues.UKURAN_BATAS_MOBILE ? desktopWidth - 50 : 500, desktopHeight - 50, null);

	}

	public void onClickCariDataUjian() throws Exception {
		Common.displayWindow("/pages/psb/cari_data_peserta_ujian.zul", true, "95%",
				Common.isMobile() ? "100%" : "950px", eventListenerLogin, "", false);
	}

	public void onClickCariPembayaran() throws Exception {
		Common.displayWindow("/pages/psb/cari_data_pembayaran.zul", true, "95%", Common.isMobile() ? "100%" : "950px",
				eventListenerLogin, "", false);
	}

	public void onClickCariDataDaftar() throws Exception {
		Common.displayWindow("/pages/psb/cari_data_pendaftar.zul", true);
	}

	public void onClickLoginPMB() throws Exception {
		Common.displayWindow("/pages/psb/login_calon_siswa.zul", true, "95%", Common.isMobile() ? "100%" : "950px",
				eventListenerLogin, "", false);
	}

	public void onClickAlurPendaftaran() throws Exception {

		Sekolah sekolah = SekolahUtil.getSekolah();
		calonSiswa = Common.isLoginCalonSiswa();
		if (calonSiswa != null) {
			sekolah = calonSiswa.getSekolah();
		}

		LampiranLain lampiranLain = LampiranLain.ambil(LampiranLain.ID_ALUR_REGISTRASI_PSB,
				LampiranLain.ALUR_REGISTRASI_PSB);
		if (sekolah != null) {
			lampiranLain = LampiranLain.ambil(sekolah.getId(), LampiranLain.ALUR_REGISTRASI_PSB);
		}
		if (lampiranLain == null) {
			MyMessageboxConfig.show("Mohon maaf, file alur registrasi belum diupload. Langkah yang dapat dilakukan: (1) pergi ke pengaturan dan upload file alur registrasi terlebih dahulu; (2) pastikan format file sesuai yang diizinkan sistem; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}

		Common.display(lampiranLain);
	}

	private void tampilGelombang(Rows rows, PerguruanTinggi selectedPerguruanTinggi) {

		Row row = new Row();
		row.setValign("top");
		row.setParent(rows);
		String ta = (String) Sessions.getCurrent(true).getAttribute("t");
		GelombangPendaftaranPsb gelombangPendaftaranPsb = (GelombangPendaftaranPsb) Sessions.getCurrent(true)
				.getAttribute("gelombangPendaftaranPsb");
		calonSiswa = Common.isLoginCalonSiswa();
		TampilanPengumumanAkademisAction.tampilGelombang(selectedPerguruanTinggi, ta, gelombangPendaftaranPsb, row,
				calonSiswa, alur, informasiPembayaran, loginCalonMhs, informasiKelulusan, pembayaranViaPaymentGateway,
				refreshdata, mobil);

	}

	@SuppressWarnings("unchecked")
	private void tampilPengumumanUtama(Rows rows, PerguruanTinggi selectedPerguruanTinggi) {
		Sekolah selectedSekolah = SekolahUtil.getSekolah();
		Yayasan selectedYayasan = SekolahUtil.getYayasan();

		List<PengumumanAkademis> listPengumumanAkademis = ConstantValues
				.simpleList(TampilanPengumumanPSBAction.initCriteriaStatic(true, selectedSekolah, selectedYayasan)
						.add(Restrictions.eq("kategoriPengumuman.merupakanPengumumanUtama", true))
						.setMaxResults(Common.ROWS_COUNT_ON_PAGE_1), PengumumanAkademis.class);

		if (!listPengumumanAkademis.isEmpty()) {

			Tbmuser tbmuser = Common.getCurrentUser();
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			TampilanPengumumanAkademisAction.tampil(row, listPengumumanAkademis.get(0), selectedSekolah, tbmuser,
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

		// Pengumuman ber-flag "Langsung tampil di Beranda": tampilkan isi PENUH inline di landing PSB
		// (tanpa harus klik judul), sama seperti Papan Pengumuman di Beranda utama.
		try {
			Long utamaId = listPengumumanAkademis.isEmpty() || listPengumumanAkademis.get(0) == null ? null
					: listPengumumanAkademis.get(0).getId();
			List<PengumumanAkademis> listLangsungBeranda = ConstantValues.simpleList(
					TampilanPengumumanPSBAction.initCriteriaStatic(true, selectedSekolah, selectedYayasan)
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
				TampilanPengumumanAkademisAction.tampil(rowBeranda, item, selectedSekolah, tbmuserBeranda,
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
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void initProdi() {
		final PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		Common.clear(container);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setParent(container);
		grid.setOddRowSclass("non-odd");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("100%");

		final Rows rows = new Rows();
		rows.setParent(grid);

		String header = "";
		try {
			if (calonSiswa == null || calonSiswa.getId() == null) {
				Sekolah selectedSekolah = SekolahUtil.getSekolah();
				if (selectedSekolah != null && selectedSekolah.getHeaderppdb() != null
						&& !selectedSekolah.getHeaderppdb().trim().isEmpty()) {
					header = selectedSekolah.getHeaderppdb();
				} else if (selectedSekolah != null && selectedSekolah.getYayasan() != null
						&& selectedSekolah.getYayasan().getHeaderppdb() != null
						&& !selectedSekolah.getYayasan().getHeaderppdb().trim().isEmpty()) {
					header = selectedSekolah.getYayasan().getHeaderppdb();
				}

				if (header.trim().isEmpty()) {
					Yayasan selectedYayasan = SekolahUtil.getYayasan();
					if (selectedYayasan != null && selectedYayasan.getHeaderppdb() != null
							&& !selectedYayasan.getHeaderppdb().trim().isEmpty()) {
						header = selectedYayasan.getHeaderppdb();
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/PSBAction.java:1232");
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

					if (Common.bolehKonfigurasi("posisi_pengumuman_psb_dibawah_pilihan_daftar")) {
						tampilGelombang(rows, selectedPerguruanTinggi);
						tampilPengumumanUtama(rows, selectedPerguruanTinggi);
					} else {
						tampilPengumumanUtama(rows, selectedPerguruanTinggi);
						tampilGelombang(rows, selectedPerguruanTinggi);
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

			if (Common.bolehKonfigurasi("posisi_pengumuman_psb_dibawah_pilihan_daftar")) {
				tampilGelombang(rows, selectedPerguruanTinggi);
				tampilPengumumanUtama(rows, selectedPerguruanTinggi);
			} else {
				tampilPengumumanUtama(rows, selectedPerguruanTinggi);
				tampilGelombang(rows, selectedPerguruanTinggi);
			}
		}
	}

	@SuppressWarnings({ "unchecked" })
	public void onPengumuman(PengumumanAkademis selectedPengumumanAkademis) throws Exception {

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
		Sekolah sekolah = SekolahUtil.getSekolah();

		if (selectedPengumumanAkademis != null) {
			Tbmuser tbmuser = Common.getCurrentUser();

			TampilanPengumumanAkademisAction.tampil(row, selectedPengumumanAkademis, sekolah, tbmuser,
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

			List<PengumumanAkademis> listPengumumanAkademis = ConstantValues
					.simpleList(TampilanPengumumanPSBAction.initCriteriaStatic(true, selectedSekolah, selectedYayasan)
							.add(Restrictions.or(Restrictions.isNull("langsungMunculDiTab"),
									Restrictions.eq("langsungMunculDiTab", false)))
							.add(Restrictions.or(Restrictions.isNull("kategoriPengumuman.merupakanPengumumanUtama"),
									Restrictions.eq("kategoriPengumuman.merupakanPengumumanUtama", false)))
							.setMaxResults(Common.ROWS_COUNT_ON_PAGE_1), PengumumanAkademis.class);

			if (!listPengumumanAkademis.isEmpty()) {

				Tbmuser tbmuser = Common.getCurrentUser();

				TampilanPengumumanAkademisAction.tampil(row, listPengumumanAkademis.get(0), sekolah, tbmuser,
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

		Criteria criteria = TampilanPengumumanPSBAction.initCriteriaStatic(true, selectedSekolah, selectedYayasan)
				.add(Restrictions.or(Restrictions.isNull("langsungMunculDiTab"),
						Restrictions.eq("langsungMunculDiTab", false)));
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
				TampilanPengumumanAkademisAction.prosess(pengumumanAkademis.getId(), tabs, tabpanels, true, container);

				MyMessageboxConfig.showFormat(
						"Terdapat Polling / Jejak Pendapat \"{V1}\". Mohon Bapak/Ibu berkenan melengkapi Polling / Jejak Pendapat berikut terlebih dahulu.",
						"Polling / Jejak Pendapat", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
						pengumumanAkademis.getJudul());
			}
		}, criteria, tbmuser, null, Common.isMobile(), "border:0px;background:#F5F5F5 repeat-x 0 0;");

	}

}
