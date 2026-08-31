package ais.action.master;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.servlet.BMS;
import ais.action.servlet.BSI;
import ais.action.servlet.Finpay;
import ais.action.servlet.JatelindoCallback;
import ais.action.servlet.Mandiri;
import ais.action.servlet.OcbcNisp;
import ais.action.servlet.Va;
import ais.action.ws.PayAction;
import ais.action.ws.model.Response;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankHost;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.LogHostToHost;
import ais.database.model.VirtualAccountBank;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Controller/action ZK untuk log host to host. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code org.zkoss.zul.Html dashboardHtml},
 * {@code org.zkoss.zul.Html progressHtml}, {@code MyGrid grid}, {@code Paging paging}, {@code Textbox
 * searchketerangan}, {@code Textbox searchbank}, {@code Textbox searchnama}, {@code Textbox searchkode};
 * inisialisasi/lifecycle ({@code buatTombolCekUlang()}, {@code doBeforeCompose()}, {@code doAfterCompose()},
 * {@code initCriteria()}, {@code initCriteria()}); pembacaan/pencarian ({@code refreshSetelahCekPembayaran()},
 * {@code tampilkanHasilCekPembayaran()}, {@code tampilkanErrorCekPembayaran()}, {@code tampilkanStackTrace()},
 * {@code onSearchDefault()}, {@code refreshDashboardAman()}); validasi/perhitungan ({@code
 * prosesCekUlangMassalMandiri()}, {@code prosesCekUlangMassalOCBC()}); pelaporan/ekspor ({@code
 * renderJenisTransaksi()}, {@code renderStatusStackTrace()}); operasi domain lain ({@code onVa()}, {@code
 * onFinpay()}, {@code onIpaymu()}, {@code onFaspay()}, {@code onJatelindo()}, {@code onCimb()}). Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class LogHostToHostAction extends GenericAutowireComposer implements DataCriteria {

	private static final long serialVersionUID = -5779730217402400328L;

	private org.zkoss.zul.Html dashboardHtml;
	private org.zkoss.zul.Html progressHtml;

	private MyGrid grid;
	private Paging paging;

	private Textbox searchketerangan;
	private Textbox searchbank;
	private Textbox searchnama;
	private Textbox searchkode;
	private Textbox searchnim;

	/** Toggle filter "Hanya Error": bila dicentang, tampilkan hanya baris yang memiliki stack trace. */
	private org.zkoss.zul.Checkbox searchHanyaError;

	private MyToolbarbuttonConfig find;

	private Tabpanel rekonsiliasi;
	private Tabpanel rekonsiliasiGagal;

	private MyDatebox searchmulai;
	private MyDatebox searchsampai;

	private Tabpanel tabpanelVa;

	private MyToolbarbuttonConfig buatTombolCekUlang(String label) {
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(label, "/img/svg/check2-circle.svg");
		button.setTooltiptext("Cek ulang pembayaran dari log gateway ini, lalu muat ulang tabel dan dasbor.");
		button.setOrient("vertical");
		return button;
	}

	private void refreshSetelahCekPembayaran() {
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
				refreshDashboardAman();
			}
		});
	}

	private void tampilkanHasilCekPembayaran(Object detail) {
		if (!Common.getApakahAdmin()) {
			return;
		}
		try {
			MyMessageboxConfig.showFormat(
					"Proses pengecekan ulang pembayaran telah selesai dilaksanakan.\n\nBerikut rincian tanggapan dari gateway pembayaran:\n{V1}",
					"Cek Pembayaran Ulang", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
					(detail == null ? "Tidak tersedia rincian dari gateway pembayaran." : detail));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void tampilkanErrorCekPembayaran(Exception e) {
		Common.tampilErrorJikaAdmin(e);
		try {
			MyMessageboxConfig.showFormat(
					"Mohon maaf, proses pengecekan ulang pembayaran belum dapat diselesaikan. Keterangan: {V1}. Langkah yang dapat dilakukan: (1) periksa kembali koneksi jaringan ke gateway pembayaran; (2) pastikan data transaksi masih valid; (3) ulangi proses beberapa saat lagi, dan apabila masih gagal mohon hubungi administrator sistem.",
					"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
					(e == null || e.getMessage() == null ? "penyebab tidak diketahui" : e.getMessage()));
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/LogHostToHostAction.java:137");
		}
	}

	@SuppressWarnings("unchecked")
	public void onVa(Event event) {
		if (tabpanelVa.getChildren().size() == 0) {
			if (Common.bolehKonfigurasi("tampilkan_va_custom", Konfigurasi.TIDAK_AKTIF)) {
				Tabbox tabbox = new Tabbox();
				tabbox.setParent(tabpanelVa);
				tabbox.setHeight("100%");
				tabbox.setWidth("100%");

				Tabs tabs = new Tabs();
				tabs.setParent(tabbox);

				Tabpanels tabpanels = new Tabpanels();
				tabpanels.setParent(tabbox);

				String[] banks = new String[] { "Bank Online 2", "Mandiri,Bank Online" };

				for (String b : banks) {
					MyTabConfig tabSoal = new MyTabConfig(b);
					tabSoal.setParent(tabs);

					Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
					tabpanelUtama.setParent(tabpanels);

					try {
						String url = "/pages/master/virtual_account_bank.zul?b=" + URLEncoder.encode(b, "UTF-8");
						MyWindow window = new MyWindow("", "none", false);
						window.setHeight("100%");
						window.setWidth("100%");
						window.setParent(tabpanelUtama);
						MyInclude iframe = new MyInclude(url);
						iframe.setParent(window);
					} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				}
			} else if (Common.bolehKonfigurasi("tampilkan_va_per_bank", Konfigurasi.TIDAK_AKTIF)) {
				Session session = null;
				try {
					session = HibernateUtil.getSessionFactory().openSession();
					List<String> banks = session.createCriteria(VirtualAccountBank.class)
							.add(Restrictions.ne("bank", "")).add(Restrictions.isNotNull("bank"))
							.setProjection(Projections.groupProperty("bank")).list();

					Tabbox tabbox = new Tabbox();
					tabbox.setParent(tabpanelVa);
					tabbox.setHeight("100%");
					tabbox.setWidth("100%");

					Tabs tabs = new Tabs();
					tabs.setParent(tabbox);

					Tabpanels tabpanels = new Tabpanels();
					tabpanels.setParent(tabbox);

					for (String b : banks) {
						MyTabConfig tabSoal = new MyTabConfig(b);
						tabSoal.setParent(tabs);

						Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
						tabpanelUtama.setParent(tabpanels);

						try {
							String url = "/pages/master/virtual_account_bank.zul?b=" + URLEncoder.encode(b, "UTF-8");
							MyWindow window = new MyWindow("", "none", false);
							window.setHeight("100%");
							window.setWidth("100%");
							window.setParent(tabpanelUtama);
							MyInclude iframe = new MyInclude(url);
							iframe.setParent(window);
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					}
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				} finally {
					Common.closeNativeSessionQuietly(session);
				}
			} else {
				String url = "/pages/master/virtual_account_bank.zul";
				MyWindow window = new MyWindow("", "none", false);
				window.setHeight("100%");
				window.setWidth("100%");
				window.setParent(tabpanelVa);
				MyInclude iframe = new MyInclude(url);
				iframe.setParent(window);
			}
		}
	}

	private MyTabConfig tabFinpay;
	private Tabpanel tabpanelFinpay;
	public void onFinpay(Event event) {
		if (tabpanelFinpay.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabpanelFinpay);
			new MyInclude("/pages/master/finpay/finpay_request.zul").setParent(window);
		}
	}

	private MyTabConfig tabIpaymu;
	private Tabpanel tabpanelIpaymu;
	public void onIpaymu(Event event) {
		if (tabpanelIpaymu.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabpanelIpaymu);
			new MyInclude("/pages/master/ipaymu/ipaymu_request.zul").setParent(window);
		}
	}

	private MyTabConfig tabFaspay;
	private Tabpanel tabpanelFaspay;
	public void onFaspay(Event event) {
		if (tabpanelFaspay.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabpanelFaspay);
			new MyInclude("/pages/master/faspay/faspay_request.zul").setParent(window);
		}
	}

	private MyTabConfig tabJatelindo;
	private Tabpanel tabpanelJatelindo;
	public void onJatelindo(Event event) {
		if (tabpanelJatelindo.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabpanelJatelindo);
			new MyInclude("/pages/master/jatelindo/jatelindo_request.zul").setParent(window);
		}
	}

	private MyTabConfig tabCimb;
	private Tabpanel tabpanelCimb;
	public void onCimb(Event event) {
		if (tabpanelCimb.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabpanelCimb);
			new MyInclude("/pages/master/cimb/cimb_request.zul").setParent(window);
		}
	}

	private MyTabConfig tabBni;
	private Tabpanel tabpanelBni;
	public void onBni(Event event) {
		if (tabpanelBni.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabpanelBni);
			new MyInclude("/pages/master/bni/bni_request.zul").setParent(window);
		}
	}

	private MyTabConfig tabBsi;
	private Tabpanel tabpanelBsi;
	public void onBsi(Event event) {
		if (tabpanelBsi.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabpanelBsi);
			new MyInclude("/pages/master/bsi/bsi_request.zul").setParent(window);
		}
	}

	private MyTabConfig tabBri;
	private Tabpanel tabpanelBri;
	public void onBri(Event event) {
		if (tabpanelBri.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabpanelBri);
			new MyInclude("/pages/master/bri/bri_request.zul").setParent(window);
		}
	}

	public void onCicilanGagal(Event event) {
		if (rekonsiliasiGagal.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(rekonsiliasiGagal);
			new MyInclude("/pages/master/cicilan_pembayaran_gagal.zul").setParent(window);
		}
	}

	public void onRekonsiliasi(Event event) {
		if (rekonsiliasi.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(rekonsiliasi);
			new MyInclude("/pages/master/rekonsiliasi_host_to_host.zul").setParent(window);
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (tabFinpay != null) { tabFinpay.setVisible(Common.bolehKonfigurasi("aktifkan_pembayaran_via_finpay", Konfigurasi.TIDAK_AKTIF)); }
		if (tabpanelFinpay != null) { tabpanelFinpay.setVisible(tabFinpay.isVisible()); }

		if (tabIpaymu != null) { tabIpaymu.setVisible(Common.bolehKonfigurasi("aktifkan_pembayaran_via_ipaymu", Konfigurasi.TIDAK_AKTIF)); }
		if (tabpanelIpaymu != null) { tabpanelIpaymu.setVisible(tabIpaymu.isVisible()); }

		if (tabFaspay != null) { tabFaspay.setVisible(Common.bolehKonfigurasi("aktifkan_pembayaran_via_faspay", Konfigurasi.TIDAK_AKTIF)); }
		if (tabpanelFaspay != null) { tabpanelFaspay.setVisible(tabFaspay.isVisible()); }

		if (tabJatelindo != null) { tabJatelindo.setVisible(Common.bolehKonfigurasi("aktifkan_pembayaran_via_jatelindo", Konfigurasi.TIDAK_AKTIF)); }
		if (tabpanelJatelindo != null) { tabpanelJatelindo.setVisible(tabJatelindo.isVisible()); }

		if (tabCimb != null) { tabCimb.setVisible(Common.bolehKonfigurasi("aktifkan_pembayaran_via_cimb", Konfigurasi.TIDAK_AKTIF)); }
		if (tabpanelCimb != null) { tabpanelCimb.setVisible(tabCimb.isVisible()); }

		if (tabBni != null) { tabBni.setVisible(Common.bolehKonfigurasi("aktifkan_pembayaran_via_bni", Konfigurasi.TIDAK_AKTIF)); }
		if (tabpanelBni != null) { tabpanelBni.setVisible(tabBni.isVisible()); }

		if (tabBsi != null) { tabBsi.setVisible(Common.bolehKonfigurasi("aktifkan_pembayaran_via_bsi", Konfigurasi.TIDAK_AKTIF)); }
		if (tabpanelBsi != null) { tabpanelBsi.setVisible(tabBsi.isVisible()); }

		if (tabBri != null) { tabBri.setVisible(Common.bolehKonfigurasi("aktifkan_pembayaran_via_bri", Konfigurasi.TIDAK_AKTIF)); }
		if (tabpanelBri != null) { tabpanelBri.setVisible(tabBri.isVisible()); }

		onSearchDefault(null);
		refreshDashboardAman();
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
		refreshDashboardAman();
		}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, "nama", "ip", "kode", "nominal", "keterangan",
				"nim", "bankHost", "tanggal", "responseCode", "responseDescription", "kegiatan", "transactionType",
				"request", "response", "info0", "info1", "info2", "info3", "stackTrace");
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		// TOMBOL CEK MANDIRI
		if (Common.bolehKonfigurasi("aktifkan_chek_ulang_semua_mandiri", Konfigurasi.TIDAK_AKTIF)) {
			MyToolbarbuttonConfig button = buatTombolCekUlang("Cek Ulang Mandiri");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Sistem akan melakukan pengecekan ulang atas seluruh pembayaran Mandiri yang sesuai dengan filter yang sedang aktif, kemudian menyusun hasilnya ke dalam berkas laporan Excel. Proses ini dapat memakan waktu tergantung banyaknya data. Apakah Bapak/Ibu ingin melanjutkan?",
							"Cek Pembayaran Ulang",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									if (Integer.parseInt(event.getData().toString()) == MyMessageboxConfig.OK) {
										prosesCekUlangMassalMandiri();
									}
								}
							});
				}
			});
			button.setParent(find.getParent());
		}

		// TOMBOL CEK OCBC
		if (Common.bolehKonfigurasi("aktifkan_chek_ulang_semua_ocbc", Konfigurasi.TIDAK_AKTIF)) {
			MyToolbarbuttonConfig button = buatTombolCekUlang("Cek Ulang OCBC");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Sistem akan melakukan pengecekan ulang atas seluruh pembayaran OCBC yang sesuai dengan filter yang sedang aktif, kemudian menyusun hasilnya ke dalam berkas laporan Excel. Proses ini dapat memakan waktu tergantung banyaknya data. Apakah Bapak/Ibu ingin melanjutkan?",
							"Cek Pembayaran Ulang",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									if (Integer.parseInt(event.getData().toString()) == MyMessageboxConfig.OK) {
										prosesCekUlangMassalOCBC();
									}
								}
							});
				}
			});
			button.setParent(find.getParent());
		}
	}

	// =======================================================================
	// METHOD REFACTOR: Proses Eksekusi Massal (Memory Leak Fixed)
	// =======================================================================
	private void prosesCekUlangMassalMandiri() throws Exception {
		final String filename = Sessions.getCurrent().getWebApp()
				.getRealPath("/tmp/cek_ulang_mandiri_" + URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx");
		final File file = new File(filename);
		file.getParentFile().mkdirs();
		file.createNewFile();

		final Label label = Common.displayLoadBar(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Filedownload.save(new FileInputStream(file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", file.getName());
				onSearchDefault(null);
		refreshDashboardAman();
		}
		});

		new Thread(new Runnable() {
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				Session session = null;
				FileOutputStream fileOut = null;
				try {
					session = HibernateUtil.getSessionFactory().openSession();
					XSSFWorkbook workbook = new XSSFWorkbook();
					XSSFSheet sheet = workbook.createSheet(Common.getBahasaConfig("CETAK DATA"));
					sheet.setDefaultColumnWidth(20);
					XSSFRow rowhead = sheet.createRow((short) 0);
					rowhead.createCell(0).setCellValue("VA");
					rowhead.createCell(1).setCellValue("Request");
					rowhead.createCell(2).setCellValue("Response");

					int rowIndex = 0;
					List<LogHostToHost> logHostToHosts = initCriteria(session, true)
							.add(Restrictions.ilike("olehId", "ais.action.servlet.Mandiri", MatchMode.ANYWHERE))
							.add(Restrictions.ilike("keterangan", "paymentRequest", MatchMode.ANYWHERE))
							.list();

					int size = logHostToHosts.size();
					for (LogHostToHost logHostToHost : logHostToHosts) {
						try {
							double h = (rowIndex * 100.0) / size;
							label.setValue("Proses " + logHostToHost.getKode() + " (" + Common.numberFormat.get().format(h) + "%)");
							rowIndex++;
							String bank = "Mandiri";
							String body = Mandiri.doProses(logHostToHost.getKeterangan(), null, logHostToHost.getBankHost(), bank, true);

							XSSFRow row = sheet.createRow(rowIndex);
							row.createCell(0).setCellValue(logHostToHost.getKode());
							row.createCell(1).setCellValue(logHostToHost.getKeterangan());
							row.createCell(2).setCellValue(body);
							
							// Mencegah memory penuh
							if (rowIndex % 50 == 0) session.clear();
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					}
					
					fileOut = new FileOutputStream(filename);
					workbook.write(fileOut);
					label.setValue("");
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				} finally {
					if (fileOut != null) {
						try { fileOut.close(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					}
					Common.closeNativeSessionQuietly(session);
				}
			}
		}).start();
	}

	private void prosesCekUlangMassalOCBC() throws Exception {
		final String filename = Sessions.getCurrent().getWebApp()
				.getRealPath("/tmp/cek_ulang_ocbc_" + URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx");
		final File file = new File(filename);
		file.getParentFile().mkdirs();
		file.createNewFile();

		final Label label = Common.displayLoadBar(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Filedownload.save(new FileInputStream(file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", file.getName());
				onSearchDefault(null);
		refreshDashboardAman();
		}
		});

		new Thread(new Runnable() {
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				Session session = null;
				FileOutputStream fileOut = null;
				try {
					session = HibernateUtil.getSessionFactory().openSession();
					XSSFWorkbook workbook = new XSSFWorkbook();
					XSSFSheet sheet = workbook.createSheet(Common.getBahasaConfig("CETAK DATA"));
					sheet.setDefaultColumnWidth(20);
					XSSFRow rowhead = sheet.createRow((short) 0);
					rowhead.createCell(0).setCellValue("VA");
					rowhead.createCell(1).setCellValue("Request");
					rowhead.createCell(2).setCellValue("Response");

					int rowIndex = 0;
					List<LogHostToHost> logHostToHosts = initCriteria(session, true)
							.add(Restrictions.ilike("olehId", "ais.action.servlet.OcbcNisp", MatchMode.ANYWHERE))
							.add(Restrictions.ilike("keterangan", "paymentRequestId", MatchMode.ANYWHERE))
							.list();

					int size = logHostToHosts.size();
					for (LogHostToHost logHostToHost : logHostToHosts) {
						try {
							double h = (rowIndex * 100.0) / size;
							label.setValue("Proses " + logHostToHost.getKode() + " (" + Common.numberFormat.get().format(h) + "%)");
							rowIndex++;
							
							String data = logHostToHost.getKeterangan();
							JSONObject req = data == null || data.isEmpty() ? null : new JSONObject(data);
							
							String va = req == null || req.isNull("virtualAccountNo") ? null : req.getString("virtualAccountNo").trim();
							String trxId = req == null || req.isNull("trxId") ? null : req.getString("trxId").trim();
							String inquiryRequestId = req == null || req.isNull("inquiryRequestId") ? null : req.getString("inquiryRequestId").trim();
							String paymentRequestId = req == null || req.isNull("paymentRequestId") ? null : req.getString("paymentRequestId").trim();
							String partnerServiceId = req == null || req.isNull("partnerServiceId") ? null : req.getString("partnerServiceId").trim();
							String customer_number = req == null || req.isNull("customer_number") ? null : req.getString("customer_number").trim();

							if (req != null && !req.isNull("customerNo")) {
								customer_number = req.getString("customerNo").trim();
							}

							String bank = "OCBC NISP";
							double nominalP = 0.0;
							try { nominalP = req == null || req.isNull("paidAmount") ? 0.0 : Double.parseDouble(req.getJSONObject("paidAmount").get("value") + ""); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
							try { if (req != null && !req.isNull("payment_amount")) { Double c = Double.parseDouble(req.get("payment_amount") + ""); if (c > 0.1) nominalP = c; } } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
							try { if (req != null && !req.isNull("amount")) { Double c = Double.parseDouble(req.getJSONObject("amount").get("value") + ""); if (c > 0.1) nominalP = c; } } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

							String tanggalP = OcbcNisp.dateFormat.get().format(new Date());
							String paidStatus = req == null || req.isNull("paidStatus") ? null : req.getString("paidStatus").trim();
							BankHost bankHost = logHostToHost.getBankHost();

							try {
								if (req != null && inquiryRequestId == null && !req.isNull("amount")) inquiryRequestId = "123";
							} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

							String partnerReferenceNo = null;
							try {
								String customerNo = req == null || req.isNull("customerNo") ? null : req.getString("customerNo").trim();
								if (customerNo != null && req != null && !req.isNull("partnerReferenceNo") && req.getString("partnerReferenceNo") != "null") {
									partnerReferenceNo = req.getString("partnerReferenceNo").trim();
								}
							} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

							JSONObject body = OcbcNisp.doProcess(nominalP, tanggalP, va, bank, bankHost, null, data,
									partnerServiceId, inquiryRequestId, paymentRequestId, trxId, customer_number,
									partnerReferenceNo, true, (req == null || req.isNull("paidAmount")) && inquiryRequestId != null,
									(paidStatus != null && paidStatus.equalsIgnoreCase("n")), true);

							XSSFRow row = sheet.createRow(rowIndex);
							row.createCell(0).setCellValue(logHostToHost.getKode());
							row.createCell(1).setCellValue(logHostToHost.getKeterangan());
							row.createCell(2).setCellValue(body == null ? "" : body.toString());

							if (rowIndex % 50 == 0) session.clear(); // Free memory
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					}
					
					fileOut = new FileOutputStream(filename);
					workbook.write(fileOut);
					label.setValue("");
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				} finally {
					if (fileOut != null) {
						try { fileOut.close(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					}
					Common.closeNativeSessionQuietly(session);
				}
			}
		}).start();
	}

	class LogHostToHostRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final LogHostToHost logHostToHost = (LogHostToHost) arg1;

			RevisiHelper.createNewRevisi(LogHostToHost.class, logHostToHost, logHostToHost.getIp()).setParent(arg0);

			new Label(logHostToHost.getBankHost() == null ? "Tidak terdaftar" : logHostToHost.getBankHost().getNama()).setParent(arg0);
			
			if (logHostToHost.getNim() != null) {
				Vbox vbox = new Vbox();
				vbox.setParent(arg0);
				new Label(logHostToHost.getNim()).setParent(vbox);
				new Label(logHostToHost.getNama()).setParent(vbox);
			} else {
				Kegiatan kegiatan = logHostToHost.getKegiatan();
				if (kegiatan != null) {
					Vbox vbox = new Vbox();
					vbox.setParent(arg0);
					if (kegiatan.getCalonMahasiswa() != null) {
						new Label(kegiatan.getCalonMahasiswa().getNoRegistrasi()).setParent(vbox);
						new Label(kegiatan.getCalonMahasiswa().getNama()).setParent(vbox);
					} else if (kegiatan.getMahasiswa() != null) {
						new Label(kegiatan.getMahasiswa().getNim()).setParent(vbox);
						new Label(kegiatan.getMahasiswa().getNama()).setParent(vbox);
					}
				} else {
					new Label("-").setParent(arg0);
				}
			}
			new Label(logHostToHost.getTanggal()==null?"":Common.dateFormat3.get().format(logHostToHost.getTanggal())).setParent(arg0);
			renderJenisTransaksi(arg0, logHostToHost);
			new MyLabelKecil(logHostToHost.getKeterangan()).setParent(arg0);
			new MyLabelKecil(logHostToHost.getResponseDescription()).setParent(arg0);

			// Kolom STATUS: badge sukses/error + tombol lihat stack trace bila ada error.
			renderStatusStackTrace(arg0, logHostToHost);

			new Label(logHostToHost.getKode()).setParent(arg0);
			Double nilai = logHostToHost.getNominal();
			new Label(nilai == null ? "" : Common.numberFormat.get().format(nilai)).setParent(arg0);
			new MyLabelKecil(logHostToHost.getItem()).setParent(arg0);

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dikumpulkan lalu dibungkus
			// kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			if (logHostToHost.getKeterangan() != null && !logHostToHost.getKeterangan().trim().isEmpty() && logHostToHost.getOlehId() != null) {
				String olehId = logHostToHost.getOlehId();
				String keterangan = logHostToHost.getKeterangan();

				if (olehId.contains("ais.action.ws.logic.PaymentLogic")) {
					MyToolbarbuttonConfig button = buatTombolCekUlang("Cek Ulang");
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							try {
								PayAction payAction = new PayAction();
								String nim = logHostToHost.getNim();
								String reffNumber = "ulang_" + nim;
								String tanggalBayar = Common.databaseDateFormat.get().format(WaktuUtil.getDate());
								String jamBayar = Common.timeFormat.get().format(WaktuUtil.getDate());
								String userID = Common.getCurrentUser().getUserNama();
								String namaCabang = "Ulang";
								String nominalTagihan = logHostToHost.getInfo6();

								Response response = payAction.pay(nim, reffNumber, tanggalBayar, jamBayar, userID, namaCabang, nominalTagihan);
								String body = response.getKode_status_pembayaran() + "-" + response.getKeterangan_status_pembayaran();

								tampilkanHasilCekPembayaran(body);
								refreshSetelahCekPembayaran();
							} catch (Exception e) {
								tampilkanErrorCekPembayaran(e);
							}
						}
					});
					aksiButtons.add(button);

				} else if (olehId.contains("ais.action.servlet.Va")) {
					MyToolbarbuttonConfig button = buatTombolCekUlang("Cek Ulang");
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							try {
								String body = Va.doProses(logHostToHost.getKeterangan(), null, logHostToHost.getBankHost());
								tampilkanHasilCekPembayaran(body);
								refreshSetelahCekPembayaran();
							} catch (Exception e) {
								tampilkanErrorCekPembayaran(e);
							}
						}
					});
					aksiButtons.add(button);

				} else if (olehId.contains("ais.action.servlet.Mandiri") && keterangan.contains("paymentRequest")) {
					MyToolbarbuttonConfig button = buatTombolCekUlang("Cek Ulang");
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							try {
								String bank = "Mandiri";
								String body = Mandiri.doProses(logHostToHost.getKeterangan(), null, logHostToHost.getBankHost(), bank, true);
								tampilkanHasilCekPembayaran(body);
								refreshSetelahCekPembayaran();
							} catch (Exception e) {
								tampilkanErrorCekPembayaran(e);
							}
						}
					});
					aksiButtons.add(button);

				} else if (olehId.contains("ais.action.servlet.OcbcNisp") && keterangan.contains("paymentRequestId")) {
					MyToolbarbuttonConfig button = buatTombolCekUlang("Cek Ulang");
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							try {
								String data = logHostToHost.getKeterangan();
								JSONObject req = data == null || data.isEmpty() ? null : new JSONObject(data);

								String va = req == null || req.isNull("virtualAccountNo") ? null : req.getString("virtualAccountNo").trim();
								String trxId = req == null || req.isNull("trxId") ? null : req.getString("trxId").trim();
								String inquiryRequestId = req == null || req.isNull("inquiryRequestId") ? null : req.getString("inquiryRequestId").trim();
								String paymentRequestId = req == null || req.isNull("paymentRequestId") ? null : req.getString("paymentRequestId").trim();
								String partnerServiceId = req == null || req.isNull("partnerServiceId") ? null : req.getString("partnerServiceId").trim();
								String customer_number = req == null || req.isNull("customer_number") ? null : req.getString("customer_number").trim();

								if (req != null && !req.isNull("customerNo")) { customer_number = req.getString("customerNo").trim(); }

								String bank = "OCBC NISP";
								double nominalP = 0.0;
								try { nominalP = req == null || req.isNull("paidAmount") ? 0.0 : Double.parseDouble(req.getJSONObject("paidAmount").get("value") + ""); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
								try { if (req != null && !req.isNull("payment_amount")) { Double c = Double.parseDouble(req.get("payment_amount") + ""); if (c > 0.1) nominalP = c; } } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
								try { if (req != null && !req.isNull("amount")) { Double c = Double.parseDouble(req.getJSONObject("amount").get("value") + ""); if (c > 0.1) nominalP = c; } } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

								String tanggalP = OcbcNisp.dateFormat.get().format(new Date());
								String paidStatus = req == null || req.isNull("paidStatus") ? null : req.getString("paidStatus").trim();
								BankHost bankHost = logHostToHost.getBankHost();

								try { if (inquiryRequestId == null && !req.isNull("amount")) inquiryRequestId = "123"; } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

								String partnerReferenceNo = null;
								try {
									String customerNo = req == null || req.isNull("customerNo") ? null : req.getString("customerNo").trim();
									if (customerNo != null && req != null && !req.isNull("partnerReferenceNo") && req.getString("partnerReferenceNo") != "null") {
										partnerReferenceNo = req.getString("partnerReferenceNo").trim();
									}
								} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

								JSONObject body = OcbcNisp.doProcess(nominalP, tanggalP, va, bank, bankHost, null, data,
										partnerServiceId, inquiryRequestId, paymentRequestId, trxId, customer_number,
										partnerReferenceNo, true, (req == null || req.isNull("paidAmount")) && inquiryRequestId != null,
										(paidStatus != null && paidStatus.equalsIgnoreCase("n")), true);

								tampilkanHasilCekPembayaran(body);
								refreshSetelahCekPembayaran();
							} catch (Exception e) {
								tampilkanErrorCekPembayaran(e);
							}
						}
					});
					aksiButtons.add(button);

				} else if (olehId.contains("ais.action.servlet.BMS") && keterangan.contains("paymentAmount")) {
					MyToolbarbuttonConfig button = buatTombolCekUlang("Cek Ulang");
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							try {
								String bank = "BMS";
								String body = BMS.doProses(logHostToHost.getKeterangan(), null, logHostToHost.getBankHost(), bank, true);
								tampilkanHasilCekPembayaran(body);
								refreshSetelahCekPembayaran();
							} catch (Exception e) {
								tampilkanErrorCekPembayaran(e);
							}
						}
					});
					aksiButtons.add(button);

				} else if (olehId.contains("ais.action.servlet.JatelindoCallback")) {
					MyToolbarbuttonConfig button = buatTombolCekUlang("Cek Ulang");
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							try {
								String body = JatelindoCallback.process(logHostToHost.getKeterangan(), null, logHostToHost.getBankHost(), true);
								tampilkanHasilCekPembayaran(body);
								refreshSetelahCekPembayaran();
							} catch (Exception e) {
								tampilkanErrorCekPembayaran(e);
							}
						}
					});
					aksiButtons.add(button);

				} else if (olehId.contains("ais.action.servlet.BSI") && keterangan.contains("payment")) {
					MyToolbarbuttonConfig button = buatTombolCekUlang("Cek Ulang");
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							try {
								String bank = "BSI";
								String body = BSI.doProses(logHostToHost.getKeterangan(), null, logHostToHost.getBankHost(), bank, true);
								tampilkanHasilCekPembayaran(body);
								refreshSetelahCekPembayaran();
							} catch (Exception e) {
								tampilkanErrorCekPembayaran(e);
							}
						}
					});
					aksiButtons.add(button);

				} else if ((olehId.contains("ais.action.servlet.Finpay") && keterangan.contains("PAID")) || 
						   // FIX BUG 1: Pengecekan OCBC sebelumnya memanggil Class Finpay!
						   (olehId.contains("ais.action.servlet.OcbcNisp") && keterangan.contains("PAID"))) {
					
					MyToolbarbuttonConfig button = buatTombolCekUlang("Cek Ulang");
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							try {
								// FIX BUG: Kita kembalikan ini menggunakan doProses Finpay karena sepertinya OCBC NISP PAID diarahkan ke gateway callback aggregator (Misal Telkom/Finpay) oleh developer sebelumnya.
								String bank = logHostToHost.getOlehId().contains("Ocbc") ? "OCBC NISP" : "Finpay";
								String body = Finpay.doProses(logHostToHost.getKeterangan(), null, logHostToHost.getBankHost(), bank, true);
								
								tampilkanHasilCekPembayaran(body);
								refreshSetelahCekPembayaran();
							} catch (Exception e) {
								tampilkanErrorCekPembayaran(e);
							}
						}
					});
					aksiButtons.add(button);
				}
			}

			// Tombol "Copy Perintah Curl" — hanya tampil untuk admin
			if (Common.getApakahAdmin()) {
				final String curlCmd = buildCurlCommand(logHostToHost);
				MyToolbarbuttonConfig copyCurl = new MyToolbarbuttonConfig("Copy Perintah Curl", "/img/svg/clipboard-check.svg");
				copyCurl.setOrient("vertical");
				copyCurl.setTooltiptext("Salin perintah curl untuk menguji ulang request ini dari terminal.");
				copyCurl.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						String js = "try{"
							+ "var t=document.createElement('textarea');"
							+ "t.value=" + org.json.JSONObject.quote(curlCmd) + ";"
							+ "document.body.appendChild(t);t.select();document.execCommand('copy');"
							+ "document.body.removeChild(t);"
							+ "alert('Perintah curl berhasil disalin ke clipboard!');"
							+ "}catch(ex){alert('Gagal menyalin: '+ex);}";
						org.zkoss.zk.ui.util.Clients.evalJavaScript(js);
					}
				});
				aksiButtons.add(copyCurl);
			}

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}
	}

	private void renderJenisTransaksi(Row row, LogHostToHost log) {
		String jenis = resolveJenisTransaksi(log);
		String warna;
		String latar;
		if ("PAYMENT".equals(jenis)) {
			warna = "#166534";
			latar = "#dcfce7";
		} else if ("INQUIRY".equals(jenis)) {
			warna = "#1d4ed8";
			latar = "#dbeafe";
		} else {
			warna = "#475569";
			latar = "#e2e8f0";
		}
		org.zkoss.zul.Html badge = new org.zkoss.zul.Html("<span style=\"display:inline-block;padding:3px 7px;"
				+ "border-radius:999px;font-size:10px;font-weight:800;color:" + warna
				+ ";background:" + latar + ";\">" + jenis + "</span>");
		badge.setTooltiptext("Jenis transaksi dikenali dari isi request Host-to-Host.");
		badge.setParent(row);
	}

	private String resolveJenisTransaksi(LogHostToHost log) {
		String request = log == null || log.getKeterangan() == null ? "" : log.getKeterangan().trim();
		if (request.startsWith("{")) {
			try {
				JSONObject json = new JSONObject(request);
				String action = json.optString("action", "").trim().toLowerCase();
				if (action.contains("inquiry") || action.contains("inquery")) {
					return "INQUIRY";
				}
				if (action.contains("payment") || action.contains("pay")) {
					return "PAYMENT";
				}
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e,
						"LogHostToHostAction.resolveJenisTransaksi JSON");
			}
		}

		String teks = request.toLowerCase();
		if (teks.contains("inquiryrequestid") || teks.contains("inquiry") || teks.contains("inquery")) {
			return "INQUIRY";
		}
		if (teks.contains("paymentrequestid") || teks.contains("payment") || teks.contains("paid")) {
			return "PAYMENT";
		}
		return "LAINNYA";
	}

	// =======================================================================
	// CURL BUILDER (admin utility)
	// =======================================================================

	/**
	 * Membangun perintah curl yang mereplikasi request H2H dari log.
	 * URL endpoint diderivasi dari olehId; body diambil dari keterangan.
	 * Berguna untuk admin menguji ulang request langsung dari terminal.
	 */
	private String buildCurlCommand(LogHostToHost log) {
		String olehId = log.getOlehId() == null ? "" : log.getOlehId();
		String body   = log.getKeterangan() == null ? "" : log.getKeterangan();

		String path;
		if      (olehId.contains("PaymentLogic") || olehId.contains("PayAction")) path = "/services/PayAction";
		else if (olehId.contains("servlet.Va"))                                   path = "/Va";
		else if (olehId.contains("servlet.Mandiri"))                              path = "/Mandiri";
		else if (olehId.contains("servlet.OcbcNisp"))                             path = "/OcbcNisp";
		else if (olehId.contains("servlet.BMS"))                                  path = "/BMS";
		else if (olehId.contains("servlet.BSI"))                                  path = "/BSI";
		else if (olehId.contains("servlet.Finpay"))                               path = "/Finpay";
		else if (olehId.contains("servlet.JatelindoCallback"))                    path = "/JatelindoCallback";
		else                                                                        path = "/ws";

		boolean isJson = body.trim().startsWith("{") || body.trim().startsWith("[");
		String contentType = isJson ? "application/json" : "application/x-www-form-urlencoded";

		// Escape single quote untuk shell: tutup, tambah \', buka kembali
		String bodyEscaped = body.replace("'", "'\\''");

		return "curl -X POST 'http://SERVER_HOST" + path + "'"
			+ " \\\n  -H 'Content-Type: " + contentType + "'"
			+ " \\\n  -d '" + bodyEscaped + "'";
	}

	// =======================================================================
	// KOLOM STATUS + STACK TRACE (UI/UX)
	// =======================================================================

	/**
	 * Render sel kolom "Status": badge HIJAU "Sukses" bila tidak ada error, atau badge MERAH
	 * "Error" + cuplikan baris pertama + tombol "Lihat Stack Trace" bila kolom stack_trace terisi.
	 * Stack trace lengkap TIDAK ditampilkan inline (bisa sangat panjang) melainkan dibuka di popup
	 * agar tabel tetap rapi.
	 */
	private void renderStatusStackTrace(Row arg0, final LogHostToHost logHostToHost) {
		Vbox vbox = new Vbox();
		vbox.setWidth("100%");
		vbox.setParent(arg0);

		String st = null;
		try {
			st = logHostToHost.getStackTrace();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/LogHostToHostAction.java:973");
		}
		boolean adaStackTrace = st != null && !st.trim().isEmpty();

		String rc = null;
		try {
			rc = logHostToHost.getResponseCode();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/LogHostToHostAction.java:980");
		}
		// responseCode selain "00" (mis. 01-07) berarti transaksi gagal/error
		boolean responseError = rc != null && !rc.trim().isEmpty() && !rc.trim().startsWith("00");

		boolean adaError = adaStackTrace || responseError;

		if (!adaError) {
			org.zkoss.zul.Html ok = new org.zkoss.zul.Html(
					"<span style='display:inline-block;padding:2px 9px;border-radius:999px;background:#dcfce7;"
							+ "color:#166534;font-size:10px;font-weight:800;'>&#10003; Sukses</span>");
			ok.setParent(vbox);

			// Tetap sediakan "Lihat Detail" walau sukses/pending (bukan cuma saat error) —
			// permintaan: header HTTP/request/response/saran harus mudah dicek utk SEMUA
			// status (error/sukses/pending), tidak cuma error.
			MyToolbarbuttonConfig lihatDetail = new MyToolbarbuttonConfig("Lihat Detail", "/img/svg/eye.svg");
			lihatDetail.setOrient("vertical");
			lihatDetail.setStyle("font-size:10px;color:#166534;font-weight:700;");
			lihatDetail.setTooltiptext("Lihat detail lengkap transaksi H2H ini (header HTTP, request/response, saran).");
			lihatDetail.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					tampilkanStackTrace(logHostToHost);
				}
			});
			lihatDetail.setParent(vbox);
			return;
		}

		org.zkoss.zul.Html badge = new org.zkoss.zul.Html(
				"<span style='display:inline-block;padding:2px 9px;border-radius:999px;background:#fee2e2;"
						+ "color:#991b1b;font-size:10px;font-weight:800;'>&#9888; Error</span>");
		badge.setParent(vbox);

		if (adaStackTrace) {
			String baris1 = st.trim();
			int nl = baris1.indexOf('\n');
			if (nl > 0) {
				baris1 = baris1.substring(0, nl).trim();
			}
			if (baris1.length() > 80) {
				baris1 = baris1.substring(0, 80) + "...";
			}
			MyLabelKecil cuplik = new MyLabelKecil(baris1);
			cuplik.setStyle("color:#b91c1c;");
			cuplik.setParent(vbox);

			MyToolbarbuttonConfig lihat = new MyToolbarbuttonConfig("Lihat Stack Trace", "/img/svg/eye.svg");
			lihat.setOrient("vertical");
			lihat.setStyle("font-size:10px;color:#991b1b;font-weight:700;");
			lihat.setTooltiptext("Lihat detail error / stack trace Java lengkap saat inquiry/pembayaran gagal.");
			lihat.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					tampilkanStackTrace(logHostToHost);
				}
			});
			lihat.setParent(vbox);
		} else {
			// Error dari responseCode tanpa stack trace — tampilkan kode + deskripsi singkat
			String desc = null;
			try { desc = logHostToHost.getResponseDescription(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/LogHostToHostAction.java:1027");}
			String info = "Kode " + rc.trim()
					+ (desc != null && !desc.trim().isEmpty() ? " — " + desc.trim() : "");
			if (info.length() > 80) info = info.substring(0, 80) + "...";
			MyLabelKecil cuplik = new MyLabelKecil(info);
			cuplik.setStyle("color:#b91c1c;");
			cuplik.setParent(vbox);
		}
	}

	/**
	 * Popup detail TRANSAKSI H2H (dipakai baik dari badge Error "Lihat Stack Trace" maupun
	 * badge Sukses "Lihat Detail"): header konteks (kode, bank, waktu, kode respons, NIM/Nama),
	 * lalu berturut-turut — Saran Tindak Lanjut (dari responseDescription, sudah otomatis
	 * disusun oleh {@code PembayaranGatewayHelper.catatLogHostToHost} utk error/sukses-nominal0/
	 * pending/sukses/tak-diketahui), Header HTTP lengkap dari bank (info0), Request &amp;
	 * Response mentah, lalu Stack Trace Java (hanya bila ada). Semua area monospace, bisa
	 * di-scroll &amp; disalin (Ctrl+A lalu Ctrl+C) — tujuannya admin bisa memastikan PERSIS apa
	 * yang dikirim/diterima bank tanpa perlu membongkar log server, utk status apa pun
	 * (error/sukses/pending). Dibuka memakai {@code doHighlighted()} agar topmost &amp; tidak
	 * menggantung walau event-thread dinonaktifkan.
	 */
	private void tampilkanStackTrace(final LogHostToHost logHostToHost) {
		try {
			String st = null;
			try {
				st = logHostToHost.getStackTrace();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/LogHostToHostAction.java:1048");
			}
			if (st == null) {
				st = "";
			}
			boolean adaStackTrace = !st.trim().isEmpty();

			String saran = null;
			try {
				saran = logHostToHost.getResponseDescription();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/LogHostToHostAction.java:saran");
			}

			String header = null;
			try {
				header = logHostToHost.getInfo0();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/LogHostToHostAction.java:header");
			}

			String reqMentah = null;
			try {
				reqMentah = logHostToHost.getRequest();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/LogHostToHostAction.java:reqMentah");
			}

			String respMentah = null;
			try {
				respMentah = logHostToHost.getResponse();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/LogHostToHostAction.java:respMentah");
			}

			final MyWindow win = new MyWindow(adaStackTrace ? "Detail Error / Stack Trace" : "Detail Transaksi H2H",
					"normal", true);
			win.setWidth(Common.isMobile() ? "96%" : "82%");
			win.setHeight("86%");
			win.setMaximizable(true);
			win.setSizable(true);
			win.setContentStyle("overflow:auto;background:#ffffff;");
			win.setPage(grid.getPage());

			Vbox body = new Vbox();
			body.setWidth("100%");
			body.setStyle("padding:14px;box-sizing:border-box;");
			body.setParent(win);

			String bank = logHostToHost.getBankHost() == null ? "Tidak terdaftar"
					: logHostToHost.getBankHost().getNama();
			String waktu = logHostToHost.getTanggal() == null ? "-"
					: Common.dateFormat3.get().format(logHostToHost.getTanggal());
			String respCode = logHostToHost.getResponseCode() == null ? "-" : logHostToHost.getResponseCode();
			String identitas = logHostToHost.getNim() == null ? logHostToHost.getNama() : logHostToHost.getNim();

			org.zkoss.zul.Html ctx = new org.zkoss.zul.Html(
					"<div style='display:flex;flex-wrap:wrap;gap:8px;margin-bottom:10px;'>"
							+ chip("Kode", logHostToHost.getKode())
							+ chip("Bank", bank)
							+ chip("Waktu", waktu)
							+ chip("Kode Respons", respCode)
							+ chip("NIM/Nama", identitas)
							+ "</div>"
							+ "<div style='font-size:11px;color:#64748b;margin-bottom:6px;'>"
							+ "Klik di salah satu area teks di bawah lalu tekan "
							+ "<b>Ctrl+A</b> kemudian <b>Ctrl+C</b> untuk menyalin isinya.</div>");
			ctx.setParent(body);

			if (saran != null && !saran.trim().isEmpty()) {
				seksiDetail(body, "Respons & Saran Tindak Lanjut", saran, "#eff6ff", "#1e3a8a", 6);
			}
			if (header != null && !header.trim().isEmpty()) {
				seksiDetail(body, "Header HTTP dari Bank", header, "#0f172a", "#e2e8f0", 8);
			}
			if (reqMentah != null && !reqMentah.trim().isEmpty()) {
				seksiDetail(body, "Request Mentah", reqMentah, "#0f172a", "#e2e8f0", 8);
			}
			if (respMentah != null && !respMentah.trim().isEmpty()) {
				seksiDetail(body, "Response Mentah", respMentah, "#0f172a", "#e2e8f0", 8);
			}
			if (adaStackTrace) {
				seksiDetail(body, "Stack Trace Java", st, "#0f172a", "#e2e8f0", 18);
			}
			if (saran == null && header == null && reqMentah == null && respMentah == null && !adaStackTrace) {
				new org.zkoss.zul.Html("<div style='color:#64748b;font-size:12px;'>Tidak ada detail tambahan "
						+ "tersimpan utk transaksi ini.</div>").setParent(body);
			}

			org.zkoss.zul.Hbox actions = new org.zkoss.zul.Hbox();
			actions.setStyle("margin-top:12px;gap:8px;");
			actions.setParent(body);
			MyToolbarbuttonConfig tutup = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
			tutup.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					win.setParent(null);
				}
			});
			tutup.setParent(actions);

			win.doHighlighted();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Satu seksi detail: judul kecil + area teks monospace read-only (bg gelap gaya konsol
	 * bila {@code bg} gelap, atau kartu terang utk teks naratif spt Saran Tindak Lanjut).
	 */
	private void seksiDetail(Vbox body, String judul, String isi, String bg, String warnaTeks, int baris) {
		org.zkoss.zul.Html label = new org.zkoss.zul.Html(
				"<div style='font-size:12px;font-weight:700;color:#334155;margin:10px 0 4px;'>" + escHtml(judul)
						+ "</div>");
		label.setParent(body);

		Textbox tb = new Textbox(isi);
		tb.setMultiline(true);
		tb.setReadonly(true);
		tb.setRows(baris);
		tb.setWidth("100%");
		tb.setStyle("font-family:Consolas,'Courier New',monospace;font-size:12px;line-height:1.5;white-space:pre;"
				+ "background:" + bg + ";color:" + warnaTeks + ";border:1px solid #1e293b;border-radius:10px;"
				+ "padding:12px;");
		tb.setParent(body);
	}

	private String chip(String label, String value) {
		String v = value == null || value.trim().isEmpty() ? "-" : value.trim();
		return "<span style='display:inline-block;padding:4px 10px;border-radius:8px;background:#f1f5f9;"
				+ "font-size:11px;color:#0f172a;'><b style='color:#475569;'>" + escHtml(label) + ":</b> " + escHtml(v)
				+ "</span>";
	}

	private String escHtml(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	// =======================================================================
	// METHOD REFACTOR: Parameter Session untuk InitCriteria (Anti Leak)
	// =======================================================================
	public Criteria initCriteria(Session session, boolean order) {
		Criteria criteria = session.createCriteria(LogHostToHost.class);
		if (order) criteria.addOrder(Order.desc("id"));

		criteria.createAlias("bankHost", "bankHost", Criteria.LEFT_JOIN)
				.add(searchbank.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("bankHost.nama", searchbank.getValue().trim(), MatchMode.ANYWHERE))
				.add((searchmulai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmulai.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction("date(this_.tanggal) >= date('" + Common.databaseDateFormat.get().format(searchmulai.getValue()) + "')")))
				.add((searchsampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchsampai.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction("date(this_.tanggal) <= date('" + Common.databaseDateFormat.get().format(searchsampai.getValue()) + "')")))
				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchnim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("nama", searchnim.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("nim", searchnim.getValue().trim(), MatchMode.ANYWHERE)))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("keterangan", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("responseDescription", searchketerangan.getValue().trim(), MatchMode.ANYWHERE));

		// Filter "Hanya Error": batasi ke baris yang punya stack trace (tidak null & tidak kosong).
		criteria.add((searchHanyaError != null && searchHanyaError.isChecked())
				? Restrictions.sqlRestriction("this_.stack_trace is not null and length(trim(this_.stack_trace)) > 0")
				: Restrictions.sqlRestriction("1=1"));
		return criteria;
	}

	// Hanya dipakai untuk Override default interface jika tak ada custom session
	public Criteria initCriteria(boolean order) {
		return null; 
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Common.initPaging(initCriteria(session, false), paging);

			List<LogHostToHost> logHostToHost = initCriteria(session, true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
			
			// FIX BUG: Eager initialization untuk mencegah LazyInitializationException setelah Session ditutup
			for(LogHostToHost log : logHostToHost) {
				if (log.getBankHost() != null) log.getBankHost().getNama();
				if (log.getKegiatan() != null) {
					if (log.getKegiatan().getMahasiswa() != null) {
						log.getKegiatan().getMahasiswa().getNim();
						log.getKegiatan().getMahasiswa().getNama();
					}
					if (log.getKegiatan().getCalonMahasiswa() != null) {
						log.getKegiatan().getCalonMahasiswa().getNoRegistrasi();
						log.getKegiatan().getCalonMahasiswa().getNama();
					}
				}
			}

			ListModel strset = new SimpleListModel(logHostToHost);
			grid.setRowRenderer(new LogHostToHostRenderer());
			grid.setModelCheckMobile(strset);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			Common.closeNativeSessionQuietly(session);
		}
	}

	@SuppressWarnings("unchecked")
	private void refreshDashboardAman() {
		Session dbSession = null;
		try {
			ais.action.master.helper.GenericActionDashboardHelper.showProgress(
					progressHtml, 15, "Memuat Dasbor", "Menghitung transaksi...");

			dbSession = HibernateUtil.getSessionFactory().openSession();

			// -- Total seluruh waktu --
			long total = toLong(dbSession.createCriteria(LogHostToHost.class)
					.setProjection(org.hibernate.criterion.Projections.rowCount())
					.uniqueResult());

			// -- Hari ini & 7 hari --
			java.util.Calendar cal = java.util.Calendar.getInstance();
			cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
			cal.set(java.util.Calendar.MINUTE, 0);
			cal.set(java.util.Calendar.SECOND, 0);
			cal.set(java.util.Calendar.MILLISECOND, 0);
			Date startOfToday = cal.getTime();
			cal.add(java.util.Calendar.DAY_OF_YEAR, -6);
			Date startOf7Days = cal.getTime();

			long today = toLong(dbSession.createCriteria(LogHostToHost.class)
					.add(Restrictions.ge("tanggal", startOfToday))
					.setProjection(org.hibernate.criterion.Projections.rowCount())
					.uniqueResult());

			long sevenDays = toLong(dbSession.createCriteria(LogHostToHost.class)
					.add(Restrictions.ge("tanggal", startOf7Days))
					.setProjection(org.hibernate.criterion.Projections.rowCount())
					.uniqueResult());

			// -- Transaksi berhasil (responseCode dimulai "00") --
			long successCount = toLong(dbSession.createCriteria(LogHostToHost.class)
					.add(Restrictions.ilike("responseCode", "00", MatchMode.START))
					.setProjection(org.hibernate.criterion.Projections.rowCount())
					.uniqueResult());
			long failCount = Math.max(0L, total - successCount);

			// -- Nominal hari ini --
			Object nomObj = dbSession.createCriteria(LogHostToHost.class)
					.add(Restrictions.ge("tanggal", startOfToday))
					.add(Restrictions.isNotNull("nominal"))
					.setProjection(org.hibernate.criterion.Projections.sum("nominal"))
					.uniqueResult();
			double nominalHariIni = (nomObj instanceof Number) ? ((Number) nomObj).doubleValue() : 0.0;

			// -- Per bank (group by bankHost.nama) --
			ais.action.master.helper.GenericActionDashboardHelper.showProgress(
					progressHtml, 55, "Memuat Dasbor", "Menghitung per bank...");

			java.util.List bankRows = dbSession.createCriteria(LogHostToHost.class)
					.createAlias("bankHost", "bh", org.hibernate.Criteria.LEFT_JOIN)
					.setProjection(org.hibernate.criterion.Projections.projectionList()
							.add(org.hibernate.criterion.Projections.groupProperty("bh.nama"))
							.add(org.hibernate.criterion.Projections.rowCount()))
					.list();

			// Urutkan berdasarkan jumlah transaksi, terbesar dulu
			java.util.List bankRowsSorted = new java.util.ArrayList(bankRows);
			java.util.Collections.sort(bankRowsSorted, new java.util.Comparator() {
				public int compare(Object a, Object b) {
					Object[] rowA = (Object[]) a;
					Object[] rowB = (Object[]) b;
					long cA = (rowA[1] instanceof Number) ? ((Number) rowA[1]).longValue() : 0L;
					long cB = (rowB[1] instanceof Number) ? ((Number) rowB[1]).longValue() : 0L;
					return (cA < cB) ? 1 : ((cA > cB) ? -1 : 0);
				}
			});

			int bankLen = Math.min(bankRowsSorted.size(), 6);
			String[] bankNames = new String[bankLen];
			long[] bankCounts = new long[bankLen];
			for (int i = 0; i < bankLen; i++) {
				Object[] row = (Object[]) bankRowsSorted.get(i);
				bankNames[i] = (row[0] == null) ? "Tidak terdaftar" : row[0].toString();
				bankCounts[i] = (row[1] instanceof Number) ? ((Number) row[1]).longValue() : 0L;
			}

			// -- Susun HTML dasbor H2H --
			ais.action.master.helper.GenericActionDashboardHelper.showProgress(
					progressHtml, 88, "Memuat Dasbor", "Menyusun tampilan...");

			String html = ais.action.master.helper.GenericActionDashboardHelper.buildH2HDashboard(
					"Monitor Pembayaran H2H",
					"Lihat semua transaksi pembayaran yang masuk dari bank dan gateway secara langsung.",
					total, today, sevenDays,
					bankNames, bankCounts,
					successCount, failCount,
					nominalHariIni);
			dashboardHtml.setContent(html);

		} catch (Exception e) {
			try { Common.tampilErrorJikaAdmin(e); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/LogHostToHostAction.java:1294");}
			try { dashboardHtml.setContent(""); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/LogHostToHostAction.java:1295");}
		} finally {
			ais.action.master.helper.GenericActionDashboardHelper.hideProgress(progressHtml);
			Common.closeNativeSessionQuietly(dbSession);
		}
	}

	private static long toLong(Object val) {
		if (val instanceof Number) { return ((Number) val).longValue(); }
		return 0L;
	}

}
