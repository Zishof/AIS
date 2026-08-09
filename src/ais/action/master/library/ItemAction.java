package ais.action.master.library;


import ais.common.CommonSearchFilterHelper;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.East;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
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
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import com.google.api.services.books.model.Volume;

import ais.action.master.JurusanAction;
import ais.action.master.LogLoginAction;
import ais.action.master.PerguruanTinggiAction;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.RevisiItemHelper;
import ais.action.master.helper.util.GoogleBookSynchronized;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.library.helper.AmbilDataDariGoogleBookBanyak;
import ais.action.master.library.helper.AmbilDataDdcItemBanbox;
import ais.action.master.library.helper.AmbilDataLabelItemBanbox;
import ais.action.master.library.helper.AmbilDataPenerbitBanbox;
import ais.action.master.library.helper.AmbilDataUdcItemBanbox;
import ais.action.master.library.helper.FotoImagePerHalamanItemHelper;
import ais.action.master.library.helper.ImportterSenayaranHelper;
import ais.action.master.library.helper.ItemKomentarHelper;
import ais.action.master.library.helper.ItemPunyaBarcodeHelper;
import ais.action.master.library.helper.ItemPunyaFotoHelper;
import ais.action.master.library.helper.ItemPunyaGambarFotoHelper;
import ais.action.master.library.helper.ItemPunyaKategoriItemHelper;
import ais.action.master.library.helper.ItemPunyaPemeriksaHelper;
import ais.action.master.library.helper.ItemPunyaPengarangHelper;
import ais.action.master.library.helper.ItemPunyaTerbitHelper;
import ais.action.master.library.helper.TampilanHasilScanPerHalamanWindow;
import ais.action.master.library.util.LibraryUtil;
import ais.action.report.Report;
import ais.action.servlet.CheckISBN;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.DspaceInformation;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.file.FotoGambarItem;
import ais.database.model.file.FotoImagePerHalamanItem;
import ais.database.model.file.FotoItem;
import ais.database.model.file.LampiranLain;
import ais.database.model.library.BatchItemPunyaBarcode;
import ais.database.model.library.DataDdcItem;
import ais.database.model.library.DataDdcItemDetail;
import ais.database.model.library.DataUdcItem;
import ais.database.model.library.DataUdcItemDetail;
import ais.database.model.library.DdcItem;
import ais.database.model.library.DetailTransaksi;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaBarcode;
import ais.database.model.library.ItemPunyaKategoriItem;
import ais.database.model.library.ItemPunyaPemeriksa;
import ais.database.model.library.ItemPunyaPengarang;
import ais.database.model.library.ItemPunyaTerbit;
import ais.database.model.library.JenisItem;
import ais.database.model.library.KategoriItem;
import ais.database.model.library.LabelItem;
import ais.database.model.library.Penerbit;
import ais.database.model.library.PenerbitPunyaPemeriksa;
import ais.database.model.library.Pengarang;
import ais.database.model.library.Perpustakaan;
import ais.database.model.library.SaldoAwal;
import ais.database.model.library.SaldoAwalDetail;
import ais.database.model.library.TipeItem;
import ais.database.model.library.UdcItem;
import ais.ui.dspace.DspaceCommon;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDiv;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.csl.CSLItemDataBuilder;
import de.undercouch.citeproc.csl.CSLType;
import ais.action.master.helper.FilterLanjutHelper;

public class ItemAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	protected MyWindow addWindow;
	protected Paging paging;
	protected MyGrid grid;

	protected Textbox searchbahasa;
	protected Textbox searchisbn;
	protected Textbox searchissn;
	protected Textbox searchnama;
	private Checkbox searchaktif;
	private Checkbox bukanAmbilDariGoogle;
	protected Textbox searchtema;
	protected Textbox searchedisi;
	protected Textbox searchpengarang;
	protected Textbox searchcatatan;
	protected Textbox searchkategori;
	protected Intbox searchtahun;
	protected AmbilDataPenerbitBanbox searchpenerbit;
	protected Combobox searchjenisItem;
	protected Combobox searchtipeItem;
	protected Textbox searchbarcode;
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan;

	private MyCheckboxConfig tampilkan;

	protected Textbox nama;
	protected Textbox tema;
	protected Textbox keterangan;
	protected Textbox isbn;
	protected Textbox isbn10;
	protected Textbox issn;
	protected MyDatebox tanggal;
	protected MyDatebox tanggalTerbit;
	protected Textbox tempatterbit;
	protected Textbox bahasa;
	protected Textbox edisi;
	protected Checkbox cbPunggungManual;
	protected Textbox punggungKlasifikasi;
	protected Textbox punggungPengarang;
	protected Textbox punggungJudul;
	protected Textbox punggungEdisi;
	protected Textbox penaklikan;
	protected Textbox catatan;
	protected Textbox imageUrl;
	protected Combobox jenisItem;
	protected Combobox tipeItem;
	protected AmbilDataPenerbitBanbox penerbit;
	protected Textbox link;
	protected Intbox tahun;
	protected Intbox halaman;
	protected Textbox abstrak;
	private AmbilDataLabelItemBanbox labelItem;
	private AmbilDataDdcItemBanbox ddcItem;
	private AmbilDataUdcItemBanbox udcItem;

	protected boolean edit = false;
	protected boolean delete = false;

	protected Item item;
	protected MyToolbarbuttonConfig add;
	protected MyToolbarbuttonConfig addGoogle;
	protected MyGrid gridPengarang;
	protected MyGrid gridGambar;
	protected MyGrid gridDocument;
	protected MyGrid gridFotoImagePerHalamanItem;
	protected MyGrid gridBarcode;
	protected Textbox kewords;
	protected EventListener eventListener;
	protected Textbox abstrakEn;
	protected Textbox kewordsEn;
	protected AmbilDataPenerbitBanbox penerbit1;
	protected AmbilDataPenerbitBanbox penerbit2;
	protected AmbilDataPenerbitBanbox penerbit3;
	protected AmbilDataPenerbitBanbox penerbit4;
	protected MyGrid gridKomentar;
	protected MyGrid gridPemeriksa;
	protected MyGrid gridTerbit;
	protected MyGrid gridKategoriItem;

	private Perpustakaan perpustakaan = null;
	private Textbox deweyDecimalClass;
	private Row rowKlasifikasi;
	protected LampiranLain lainMahasiswa;

	private static String buildSenayanProgressHtml(int percent, String message) {
		if (percent < 0) {
			percent = 0;
		}
		if (percent > 100) {
			percent = 100;
		}
		String safeMessage = message == null ? "" : message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
		return "<div style='font-family:Arial,sans-serif;color:#1f2937;'>"
				+ "<div style='font-size:15px;font-weight:bold;margin-bottom:8px;'>Import Senayan</div>"
				+ "<div style='font-size:12px;line-height:18px;margin-bottom:10px;'>" + safeMessage + "</div>"
				+ "<div style='height:14px;background:#e5e7eb;border-radius:7px;overflow:hidden;'>"
				+ "<div style='height:14px;width:" + percent + "%;background:#2563eb;'></div></div>"
				+ "<div style='font-size:12px;color:#475569;margin-top:8px;'>Progress " + percent + "%</div>"
				+ "</div>";
	}

	public static CSLItemData generateCSLItemData(Item itemData) {
		if (itemData == null) {
			return new CSLItemDataBuilder().type(CSLType.BOOK).title("").build();
		}
		CSLItemDataBuilder builder = new CSLItemDataBuilder().type(CSLType.BOOK).title(itemData.getNama() == null ? "" : itemData.getNama());
		String daftarPengarang = itemData.getPengarangs() == null ? "" : itemData.getPengarangs();
		for (String p : daftarPengarang.split(",")) {
			if (p == null || p.trim().isEmpty()) {
				continue;
			}
			String[] pp = p.trim().split(" ", 2);
			String given = pp.length > 0 ? pp[0] : "";
			String family = pp.length > 1 ? pp[1] : "";
			builder.author(given, family);
		}
		if (itemData.getTanggalterbit() != null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(itemData.getTanggalterbit());
			builder.issued(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DATE));
		}
		if (itemData.getIsbn10() != null && !itemData.getIsbn10().trim().isEmpty()) {
			builder.ISBN(itemData.getIsbn10());
		}
		if (itemData.getIsbn() != null && !itemData.getIsbn().trim().isEmpty()) {
			builder.ISBN(itemData.getIsbn());
		}
		if (itemData.getIssn() != null && !itemData.getIssn().trim().isEmpty()) {
			builder.ISSN(itemData.getIssn());
		}
		if (itemData.getAbstrak() != null && !itemData.getAbstrak().trim().isEmpty()) {
			builder.abstrct(itemData.getAbstrak());
		}
		if (itemData.getDdcItem() != null && !itemData.getDdcItem().getKode().trim().isEmpty()) {
			builder.callNumber(itemData.getDdcItem().getKode());
		}
		if (itemData.getDewey_decimal_class() != null && !itemData.getDewey_decimal_class().trim().isEmpty()) {
			builder.callNumber(itemData.getDeweyDecimalClass());
		}
		if (itemData.getDeweyDecimalClass() != null && !itemData.getDeweyDecimalClass().trim().isEmpty()) {
			builder.callNumber(itemData.getDewey_decimal_class());
		}
		if (itemData.getKategories() != null && !itemData.getKategories().trim().isEmpty()) {
			builder.categories(itemData.getKategories());
		}
		if (itemData.getPenerbit() != null && !itemData.getPenerbit().getNama().trim().isEmpty()) {
			builder.publisher(itemData.getPenerbit().getNama());
		}
		if (itemData.getEdisi() != null && !itemData.getEdisi().trim().isEmpty()) {
			builder.volume(itemData.getEdisi().trim());
		}

		CSLItemData item = builder.build();
		return item;
	}

	public static void tampilkanKutipan(final Item itemData) throws Exception {
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final MyWindow window = new MyWindow("Kutipan", "true", true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("300px");
				window.setWidth("90%");

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
				column.setWidth("15%");

				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				CSLItemData item = ItemAction.generateCSLItemData(itemData);
				String bibl = CSL.makeAdhocBibliography("ieee", item).makeString();

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("IEEE"));
				row.appendChild(new ais.ui.util.MyHtml(bibl));

				bibl = CSL.makeAdhocBibliography("acm-siggraph", item).makeString();

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("ACM"));
				row.appendChild(new ais.ui.util.MyHtml(bibl));

				bibl = CSL.makeAdhocBibliography("apa", item).makeString();

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("APA"));
				row.appendChild(new ais.ui.util.MyHtml(bibl));

				bibl = CSL.makeAdhocBibliography("chicago-author-date", item).makeString();

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Chicago"));
				row.appendChild(new ais.ui.util.MyHtml(bibl));

				bibl = CSL.makeAdhocBibliography("council-of-science-editors", item).makeString();

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("CSE"));
				row.appendChild(new ais.ui.util.MyHtml(bibl));

				bibl = CSL.makeAdhocBibliography("modern-language-association", item).makeString();

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("MLA"));
				row.appendChild(new ais.ui.util.MyHtml(bibl));

				South south = new South();
				ais.ui.util.ZkCompat.setFlex(south, true);
				south.setParent(borderlayout);

				Toolbar toolbar = new Toolbar();
				// toolbar.setHeight("25px");
				toolbar.setParent(south);
				MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
				cancel.setTooltiptext("Tutup");
				cancel.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						window.detach();
					}
				});
				cancel.setParent(toolbar);

				borderlayout.setParent(window);

				window.onModal();
			}
		});

	}

	public static String[] contents = new String[] { "id", "isbn", "isbn10", "issn", "nama", "aktif", "tema", "bahasa",
			"edisi", "penaklikan", "catatan", "jenisItem", "tipeItem", "labelItem", "penerbit", "tahun", "halaman",
			"pengarangs", "kategories", "deweyDecimalClass", "fakultas", "jurusan", "tempatterbit", "abstrak",
			"abstrakEn", "kewords", "kewordsEn", "imageUrl", "imagePath", "lampiranPath", "kode" };
//	private boolean padaSaatpendataanItemPerpustakaanTampilkanPilihanFakultasDanProgramStudi = false;
	private Row rowDdc;
	private MyCheckboxConfig bolehDiDownload;
	private ArrayList<Checkbox> jurusans;

	private Thread createDaemonThread(Runnable runnable, String name) {
		Thread thread = new Thread(runnable);
		thread.setName(name == null || name.trim().isEmpty() ? "ais-library-item-worker" : name);
		thread.setDaemon(true);
		return thread;
	}

	private void processSenayanZip(final File file, final Component parentComp) throws Exception {
		final MyWindow progressWindow = new MyWindow();
		progressWindow.setTitle("Import Senayan");
		progressWindow.setWidth("560px");
		progressWindow.setHeight("220px");
		progressWindow.setClosable(false);
		progressWindow.setSizable(false);
		final Html progressHtml = new Html(buildSenayanProgressHtml(0, "Menyiapkan import Senayan."));
		Vbox progressBox = new Vbox();
		progressBox.setWidth("100%");
		progressBox.setStyle("padding:16px;");
		progressBox.appendChild(progressHtml);
		progressWindow.appendChild(progressBox);
		progressWindow.setParent(parentComp);
		progressWindow.doModal();

		final org.zkoss.zk.ui.Desktop desktop = org.zkoss.zk.ui.Executions.getCurrent().getDesktop();
		if (desktop != null && !desktop.isServerPushEnabled()) {
			desktop.enableServerPush(true);
		}
		createDaemonThread(new Runnable() {
			@Override
			public void run() {
				try {
					ImportterSenayaranHelper helper = new ImportterSenayaranHelper();
					final String[] lastMessage = new String[] { "" };
					ImportterSenayaranHelper.Result result = helper.importZip(file,
							new ImportterSenayaranHelper.ProgressListener() {
								@Override
								public void onProgress(int percent, String message) {
									lastMessage[0] = percent + "% - " + message;
									if (desktop == null || !desktop.isAlive()) {
										return;
									}
									try {
										org.zkoss.zk.ui.Executions.activate(desktop);
										try {
											progressHtml.setContent(buildSenayanProgressHtml(percent, message));
											Clients.showBusy(lastMessage[0]);
										} finally {
											org.zkoss.zk.ui.Executions.deactivate(desktop);
										}
									} catch (Exception e) {
										System.out.println(lastMessage[0]);
									}
								}
							});
					if (desktop == null || !desktop.isAlive()) {
						return;
					}
					org.zkoss.zk.ui.Executions.activate(desktop);
					try {
						Clients.clearBusy();
						if (progressWindow.getParent() != null) {
							progressWindow.detach();
						}
						try {
							onSearchDefault(null);
						} catch (Exception refreshError) {
							ais.common.Common.tampilErrorJikaAdmin(refreshError);
						}
						String pesan = "Import Senayan selesai.\nSchema staging: " + result.schema + "\nItem: "
								+ result.importedItems + "\nBarcode: " + result.importedBarcodes + "\nAnggota: "
								+ result.importedMembers + "\nStatement SQL gagal: " + result.failedStatements
								+ "\nLog diagnostik: "
								+ (result.reportFile == null ? "-" : result.reportFile.getAbsolutePath());
						if (result.failedStatements > 0) {
							showSenayanDiagnosticWindow(result.reportFile, pesan, parentComp);
						} else {
							MyMessageboxConfig.show(pesan, "Pemberitahuan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
						}
					} finally {
						org.zkoss.zk.ui.Executions.deactivate(desktop);
					}
				} catch (Exception e) {
					if (desktop == null || !desktop.isAlive()) {
						return;
					}
					try {
						org.zkoss.zk.ui.Executions.activate(desktop);
						try {
							Clients.clearBusy();
							if (progressWindow.getParent() != null) {
								progressWindow.detach();
							}
							File reportFile = null;
							if (e instanceof ImportterSenayaranHelper.ImportException) {
								reportFile = ((ImportterSenayaranHelper.ImportException) e).getReportFile();
							}
							if (reportFile == null) {
								reportFile = createSenayanFallbackDiagnostic(file, e);
							}
							String pesan = "Import Senayan gagal: " + e.getMessage()
									+ "\n\nLog diagnostik: "
									+ (reportFile == null ? "-" : reportFile.getAbsolutePath())
									+ "\n\nSilakan buka file log tersebut lalu copy isinya untuk dianalisis.";
							showSenayanDiagnosticWindow(reportFile, pesan, parentComp);
						} finally {
							org.zkoss.zk.ui.Executions.deactivate(desktop);
						}
					} catch (Exception uiError) {
						ais.common.Common.tampilErrorJikaAdmin(uiError);
					}
				}
			}
		}, "ais-library-senayan-import").start();
	}

	private void appendLocalSenayanZipButtons(MyToolbarbuttonConfig add, final Component parentComp) {
		File dir = new File("/opt/senayan");
		File[] files = dir.listFiles(new java.io.FilenameFilter() {
			@Override
			public boolean accept(File d, String name) {
				return name != null && name.toLowerCase().endsWith(".zip");
			}
		});
		if (files == null || files.length == 0) {
			return;
		}
		java.util.Arrays.sort(files, new java.util.Comparator<File>() {
			@Override
			public int compare(File a, File b) {
				return a.getName().compareToIgnoreCase(b.getName());
			}
		});
		for (int i = 0; i < files.length; i++) {
			final File zip = files[i];
			MyToolbarbuttonConfig processZip = new MyToolbarbuttonConfig("Proses " + zip.getName(), "/img/upload.gif");
			processZip.setVisible(Common.getCurrentUser().getMahasiswa() == null && Common.getCurrentUser().getDosen() == null);
			processZip.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					processSenayanZip(zip, parentComp);
				}
			});
			Common.appendKeToolbar(processZip, add, parentComp);
		}
	}

	private void showSenayanDiagnosticWindow(final File reportFile, String errorMessage, Component parentComp) {
		tryDownloadDiagnostic(reportFile);
		final MyWindow window = new MyWindow();
		window.setTitle("Log Diagnostik Import Senayan");
		window.setWidth("760px");
		window.setHeight("560px");
		window.setClosable(true);
		window.setSizable(true);

		Vbox box = new Vbox();
		box.setWidth("100%");
		box.setHeight("100%");
		box.setStyle("padding:12px;");

		String reportText = readDiagnosticText(reportFile);
		String intro = "Import Senayan gagal.\n\n" + (errorMessage == null ? "" : errorMessage)
				+ "\n\nFile log otomatis di-download jika browser mengizinkan.\n"
				+ "Untuk copy manual: klik area teks di bawah, tekan Ctrl+A lalu Ctrl+C.\n\n";
		final Textbox text = new Textbox();
		text.setMultiline(true);
		text.setReadonly(true);
		text.setWidth("100%");
		text.setHeight("430px");
		text.setValue(intro + reportText);
		box.appendChild(text);

		Hbox buttons = new Hbox();
		buttons.setSpacing("8px");
		buttons.setStyle("margin-top:10px;");
		MyToolbarbuttonConfig download = new MyToolbarbuttonConfig("Download Log", "/img/download.gif");
		download.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				tryDownloadDiagnostic(reportFile);
			}
		});
		download.setParent(buttons);
		MyToolbarbuttonConfig copy = new MyToolbarbuttonConfig("Copy", "/img/copy.png");
		copy.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Clients.evalJavaScript("var t=document.querySelector('textarea');"
						+ "if(t){t.focus();t.select();"
						+ "try{if(navigator.clipboard){navigator.clipboard.writeText(t.value);}else{document.execCommand('copy');}}"
						+ "catch(e){document.execCommand('copy');}}");
			}
		});
		copy.setParent(buttons);
		MyToolbarbuttonConfig close = new MyToolbarbuttonConfig("Tutup", "/img/close.png");
		close.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		close.setParent(buttons);
		box.appendChild(buttons);

		window.appendChild(box);
		window.setParent(parentComp);
		try {
			window.doModal();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		Clients.evalJavaScript("setTimeout(function(){var t=document.querySelector('textarea'); if(t){t.focus(); t.select();}},300);");
	}

	private void tryDownloadDiagnostic(File reportFile) {
		if (reportFile == null || !reportFile.exists()) {
			return;
		}
		try {
			Filedownload.save(new FileInputStream(reportFile), "text/plain", reportFile.getName());
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	private String readDiagnosticText(File reportFile) {
		if (reportFile == null || !reportFile.exists()) {
			return "File log diagnostik tidak ditemukan.";
		}
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(reportFile));
			String line;
			int lines = 0;
			while ((line = reader.readLine()) != null && lines < 1200) {
				sb.append(line).append('\n');
				lines++;
			}
			if (line != null) {
				sb.append("\n...[log dipotong untuk tampilan, gunakan file download untuk isi lengkap]...\n");
			}
		} catch (Exception e) {
			sb.append("Gagal membaca file log: ").append(e.getMessage());
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/ItemAction.java:683");
			}
		}
		return sb.toString();
	}

	private File createSenayanFallbackDiagnostic(File zipFile, Exception e) {
		File reportFile = null;
		PrintWriter writer = null;
		try {
			File baseDir = new File(System.getProperty("java.io.tmpdir"), "senayan-import");
			if (!baseDir.exists()) {
				baseDir.mkdirs();
			}
			reportFile = new File(baseDir, "senayan-ui-error-" + System.currentTimeMillis() + "-diagnostic.txt");
			writer = new PrintWriter(new FileWriter(reportFile));
			writer.println("DIAGNOSTIC IMPORT SENAYAN");
			writer.println("Status       : GAGAL DI UI/BACKGROUND");
			writer.println("ZIP          : " + (zipFile == null ? "-" : zipFile.getAbsolutePath()));
			writer.println("Error        : " + (e == null ? "-" : e.getMessage()));
			writer.println("Catatan      : Error ini terjadi di luar helper import utama, biasanya saat refresh halaman setelah import.");
			writer.println("============================================================");
			if (e != null) {
				StringWriter stringWriter = new StringWriter();
				PrintWriter stackWriter = new PrintWriter(stringWriter);
				e.printStackTrace(stackWriter);
				stackWriter.close();
				writer.println(stringWriter.toString());
			}
		} catch (Exception ignored) {
			return reportFile;
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
		return reportFile;
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@SuppressWarnings("unchecked")
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		final Component parentComp = comp;
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		searchisbn.addEventListener("onFocus", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				searchisbn.select();
			}
		});

		searchbarcode.addEventListener("onFocus", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				searchbarcode.select();
			}
		});

		searchissn.addEventListener("onFocus", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				searchissn.select();
			}
		});

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		final Tbmuser tbmuser = Common.getCurrentUser();

		try {
			Session session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			session.createSQLQuery(
					"delete from library.tipe_item where nama='Jurnal Nasional yang terakreditasi' and id not in (select min(id) from library.tipe_item where nama='Jurnal Nasional yang terakreditasi') and id not in (select tipe_item from library.item group by tipe_item);")
					.executeUpdate();
			session.getTransaction().commit();
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		HibernateUtil.closeSession();

		Session session = HibernateUtil.currentSession();

		if (execution.getParameter("perpustakaan") != null) {
			perpustakaan = (Perpustakaan) session.createCriteria(Perpustakaan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("perpustakaan").trim())))
					.uniqueResult();
		}

		createDaemonThread(new Runnable() {

			@Override
			public void run() {
				try {
				Session session = HibernateUtil.currentNativeSession();

				TipeItem tipeItem = (TipeItem) (session.createCriteria(TipeItem.class)
						.add(Restrictions.or(Restrictions.ilike("nama", "Jurnal Nasional yang terakreditasi"),
								Restrictions.or(Restrictions.ilike("nama", "Jurnal"),
										Restrictions.or(Restrictions.ilike("nama", "Jurnal Ilmiah"),
												Restrictions.ilike("nama", "Jurnal National")))))
						.setMaxResults(1).uniqueResult());
				if (tipeItem != null && !tipeItem.getNama().equals("Jurnal Nasional yang terakreditasi")) {
					tipeItem.setNama("Jurnal Nasional yang terakreditasi");
					session.getTransaction().begin();
					Common.refreshUpdate(session, tipeItem);
					session.getTransaction().commit();
				} else if (tipeItem == null) {
					tipeItem = new TipeItem();
					tipeItem.setAktif(true);
					tipeItem.setNama("Jurnal Nasional yang terakreditasi");
					session.getTransaction().begin();
					session.save(tipeItem);
					session.getTransaction().commit();
				}

				tipeItem = (TipeItem) (session.createCriteria(TipeItem.class)
						.add(Restrictions.ilike("nama", "Jurnal International")).setMaxResults(1).uniqueResult());
				if (tipeItem == null) {
					tipeItem = new TipeItem();
					tipeItem.setAktif(true);
					tipeItem.setNama("Jurnal International");
					session.getTransaction().begin();
					session.save(tipeItem);
					session.getTransaction().commit();
				}

				tipeItem = (TipeItem) (session.createCriteria(TipeItem.class)
						.add(Restrictions.ilike("nama", "Prosiding")).setMaxResults(1).uniqueResult());
				if (tipeItem == null) {
					tipeItem = new TipeItem();
					tipeItem.setAktif(true);
					tipeItem.setNama("Prosiding");
					session.getTransaction().begin();
					session.save(tipeItem);
					session.getTransaction().commit();
				}

				List<Item> items = session.createCriteria(Item.class).add(Restrictions
						.or(Restrictions.isNull("pengarangs"), Restrictions.sqlRestriction("trim(pengarangs) = ''")))
						.list();
				for (Item item : items) {
					List<String> strings = session.createCriteria(ItemPunyaPengarang.class)
							.createAlias("pengarang", "pengarang").setProjection(Projections.property("pengarang.nama"))
							.add(Restrictions.eq("item", item)).list();
					String pengarangs = strings.toString().replaceAll("\\[", "").replaceAll("\\]", "");
					System.out.println("pengarangs=> " + pengarangs);
					item.setPengarangs(pengarangs.trim().equals("") ? "None" : pengarangs);

					session.getTransaction().begin();
					session.update((item));
					session.getTransaction().commit();
				}
				// session.disconnect();
				if (session.isOpen()) {session.disconnect();session.close();}

				HibernateUtil.closeSession();
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}, "ais-library-item-normalize-tipe").start();

		Common.insertComboDanSemua(searchtipeItem, "nama", TipeItem.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.insertComboDanSemua(searchjenisItem, "nama", JenisItem.class);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		if (addGoogle != null) {
			addGoogle.setVisible((add != null && add.isVisible()));
			addGoogle.setTooltiptext("Tambah dari Google Book");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		if (!Common.bolehKonfigurasi("terintegrasi_dengan_google_book_baru", Konfigurasi.TIDAK_AKTIF)) {
			if (addGoogle != null) addGoogle.setVisible(false);
		}

		if (tbmuser.getMahasiswa() != null || tbmuser.ambilDosen() != null) {
			if (add != null) add.setVisible(false);
			if (addGoogle != null) addGoogle.setVisible(false);

			edit = false;
			delete = false;
		}

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		searchpenerbit.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		if (perpustakaan != null) {
			add.setVisible(false);
		}

		Perpustakaan p = Common.getCurrentPerpustakaan();

		final List<Perpustakaan> pustaka = HibernateUtil.currentSession().createCriteria(Perpustakaan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(p == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("id", p.getId()))
				.addOrder(Order.asc("id")).list();

		List<String> columnHeadersAddingTambahan = new ArrayList<String>();
		columnHeadersAddingTambahan.add("ID");
		columnHeadersAddingTambahan.add("KODE");
		columnHeadersAddingTambahan.add("ISBN10");
		columnHeadersAddingTambahan.add("ISBN13");
		columnHeadersAddingTambahan.add("JUDUL");

		for (Perpustakaan perpustakaan : pustaka) {
			columnHeadersAddingTambahan.add(perpustakaan.getId() + "-" + perpustakaan.getNama());
		}

		EventListener dataAdding = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] objects = (Object[]) arg0.getData();
				Item item = (Item) objects[0];

				XSSFWorkbook workbook = (XSSFWorkbook) objects[3];

				XSSFFont hlink_font = workbook.createFont();
				hlink_font.setUnderline(XSSFFont.U_SINGLE);
				hlink_font.setColor(new XSSFColor(Color.BLUE));

				final XSSFCellStyle hlink_style = workbook.createCellStyle();
				hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
				hlink_style.setFont(hlink_font);

				XSSFRow rowTambahan = (XSSFRow) objects[4];
				// XSSFRow rowheadTambahan = (XSSFRow) objects[5];

				Session session = HibernateUtil.currentNativeSession();

				rowTambahan.createCell(0).setCellValue(item.getId());

				rowTambahan.createCell(1).setCellValue(item.getKode());

				rowTambahan.createCell(2).setCellValue(item.getIsbn10());

				rowTambahan.createCell(3).setCellValue(item.getIsbn());

				rowTambahan.createCell(4).setCellValue(item.getNama());

				int i = 5;
				for (Perpustakaan perpustakaan : pustaka) {

					SaldoAwal saldoAwal = (SaldoAwal) session.createCriteria(SaldoAwal.class).add(Restrictions.eq(
							"keterangan",
							"SALDO_AWAL_OTOTAMIS_IMPORT-Data ini merupakan data yang berisi daftar buku yang otomatis diimport lewat excel"))
							.add(Restrictions.eq("perpustakaan", perpustakaan)).setMaxResults(1)
							.addOrder(Order.desc("id")).uniqueResult();

					if (saldoAwal == null) {
						saldoAwal = new SaldoAwal();
						saldoAwal.setKode(Common.getGeneratedBarCode());
						saldoAwal.setDibuatOleh(tbmuser);
						saldoAwal.setDisetujuiOleh(tbmuser);
						saldoAwal.setKeterangan(
								"SALDO_AWAL_OTOTAMIS_IMPORT-Data ini merupakan data yang berisi daftar buku yang otomatis diimport lewat excel");
						saldoAwal.setPerpustakaan(perpustakaan);
						saldoAwal.setTanggalPembuatan(ais.ui.util.WaktuUtil.getDate());
						saldoAwal.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
						saldoAwal.setIndex(-1L);
						session.getTransaction().begin();
						session.save(saldoAwal);
						session.getTransaction().commit();
					}

					Number count = ((Number) session.createCriteria(ItemPunyaBarcode.class)
							.add(Restrictions.eq("item", item)).add(Restrictions.eq("perpustakaan", perpustakaan))
							.createAlias("batchItemPunyaBarcode", "batchItemPunyaBarcode")
							.add(Restrictions.eq("batchItemPunyaBarcode.saldoAwal", saldoAwal))
							.setProjection(Projections.rowCount()).uniqueResult()).intValue();
					rowTambahan.createCell(i).setCellValue(count == null ? 0 : count.intValue());
					i++;
				}

				HibernateUtil.closeSession();
			}
		};

		List<String> columnHeadersAdding = new ArrayList<String>();
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(Item.class, this, "Download",
				"/img/print.png", columnHeadersAdding, dataAdding, true, columnHeadersAddingTambahan,
				"Jumlah Eksemplar", contents);
		cetakToolbarbutton.setVisible(
				Common.getCurrentUser().getMahasiswa() == null && Common.getCurrentUser().getDosen() == null);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload Data Item" + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		if (upload != null) { upload.setUpload(Common.ukuranFileUpload()); }
		if (upload != null) { upload.setVisible(Common.getCurrentUser().getMahasiswa() == null && Common.getCurrentUser().getDosen() == null); }
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();
				if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
					return;
				if (media.getName().toLowerCase().endsWith("xlsx")) {

					InputStream inputStream = media.getStreamData();
					// System.out.println("media = " + media);
					final File file = new File(
							Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					// System.out.println("file = " + file.getAbsolutePath());
					file.getParentFile().mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					int c;
					while ((c = inputStream.read()) != -1) {
						fileOutputStream.write(c);
					}
					fileOutputStream.close();
					inputStream.close();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							uploadDataItem(file, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									onSearchDefault(arg0);
									Clients.clearBusy();
								}
							}, contents);
						}
					}, "Harap tunggu.. sedang melakukan proses upload data..");

				} else {
					MyMessageboxConfig.show(
							"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media,
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});
		Common.appendKeToolbar(upload, add, comp);

		MyToolbarbuttonConfig uploadSenayan = new MyToolbarbuttonConfig("Upload Senayan ZIP", "/img/upload.gif");
		uploadSenayan.setUpload("true,maxsize=-1");
		uploadSenayan.setVisible(Common.getCurrentUser().getMahasiswa() == null && Common.getCurrentUser().getDosen() == null);
		uploadSenayan.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent == null ? null : uploadEvent.getMedia();
				if (media == null || media.getName() == null || !media.getName().toLowerCase().endsWith(".zip")) {
					MyMessageboxConfig.show("File yang di-upload harus berupa ZIP export Senayan berisi 1 file SQL dan folder images.",
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
					return;
				}

				final File file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/senayan-import/" 
						+ System.currentTimeMillis() + "-" + media.getName()));
				file.getParentFile().mkdirs();
				InputStream inputStream = media.getStreamData();
				FileOutputStream fileOutputStream = new FileOutputStream(file);
				try {
					int c;
					while ((c = inputStream.read()) != -1) {
						fileOutputStream.write(c);
					}
				} finally {
					fileOutputStream.close();
					inputStream.close();
				}
				processSenayanZip(file, parentComp);
			}
		});
		Common.appendKeToolbar(uploadSenayan, add, comp);
		appendLocalSenayanZipButtons(add, parentComp);

		if (Common.getCurrentUser().getMahasiswa() == null && Common.getCurrentUser().getDosen() == null) {
			MyToolbarbuttonConfig exportKeOjs = new MyToolbarbuttonConfig("Ekspor", "/img/corner.gif");
			Common.appendKeToolbar(exportKeOjs, add, comp);
			exportKeOjs.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)
					&& Common.bolehKonfigurasi("item_pustaka_terhubung_ke_dspace"));
			exportKeOjs.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					final Label label = Common.displayLoadBar(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(arg0);
							LogLoginAction.tampilDpsaceLog();
						}
					});

					new Thread(new Runnable() {

						@Override
						public void run() {
							try {
								String cookie = DspaceCommon.login();
								List<Object> objects = initCriteria(true).list();
								PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
								boolean berdasarkanTahun = Common.bolehKonfigurasi("export_buku_dspace_berdasarkan_tahun", Konfigurasi.TIDAK_AKTIF);
								int rowIndex = 1;
								for (Object o : objects) {

									Item item = (o instanceof ItemPunyaBarcode) ? ((ItemPunyaBarcode) o).getItem()
											: (Item) o;

									label.setValue("Sedang memproses data " + item.toString() + " ("
											+ Common.numberFormat.get().format((rowIndex++) * 100.0 / objects.size())
											+ " %)");

									Session session = HibernateUtil.currentSession();
									int count = ((Number) session.createCriteria(ItemPunyaPengarang.class)
											.add(Restrictions.eq("item", item)).setProjection(Projections.rowCount())
											.uniqueResult()).intValue();
									if (count > 0) {
										if (item.getJurusan() != null) {
											ItemAction.getDspace(cookie, item, null, true,
													getDspaceItem(cookie, item, null,
															berdasarkanTahun ? getDspaceItemTahunJurusan(cookie, item)
																	: getDspaceItemJurusan(cookie, item)));
										} else if (perguruanTinggi != null) {
											ItemAction.getDspace(cookie, item, null, true,
													getDspaceItem(cookie, item, null,
															berdasarkanTahun
																	? getDspaceItemTahunPT(cookie, item,
																			perguruanTinggi)
																	: PerguruanTinggiAction.getDspace(cookie,
																			perguruanTinggi, false)));
										}
									}
								}
							} catch (Exception e) {
								// TODO Auto-generated catch block
								Common.tampilErrorJikaAdmin(e);
							}
							label.setValue("");
						}
					}).start();
				}
			});

			MyToolbarbuttonConfig batalExport = new MyToolbarbuttonConfig("Batalkan Ekspor", "/img/svg/trash.svg");
			Common.appendKeToolbar(batalExport, add, comp);
			batalExport.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)
					&& Common.bolehKonfigurasi("item_pustaka_terhubung_ke_dspace"));
			batalExport.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin membatalkan ekspor data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										final Label label = Common.displayLoadBar(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												onSearchDefault(arg0);
												LogLoginAction.tampilDpsaceLog();
											}
										});

										new Thread(new Runnable() {

											@Override
											public void run() {
												try {
												try {
													String cookie = DspaceCommon.login();
													List<Object> objects = initCriteria(true).list();

													int rowIndex = 1;
													for (Object o : objects) {

														Item item = (o instanceof ItemPunyaBarcode)
																? ((ItemPunyaBarcode) o).getItem()
																: (Item) o;
														label.setValue(
																"Sedang memproses data " + item.toString() + " ("
																		+ Common.numberFormat.get().format(
																				(rowIndex++) * 100.0 / objects.size())
																		+ " %)");
														DspaceInformation dspaceInformation = DspaceInformation
																.getDspaceInformation(Item.class.getName(),
																		item.getId());
														if (dspaceInformation != null) {
															int i = DspaceInformation.delete(cookie,
																	"items/" + dspaceInformation.getUuid(),
																	dspaceInformation.getPostInfo());
															if (i == 200) {

																Session session = HibernateUtil.currentNativeSession();
																session.getTransaction().begin();
																session.delete(dspaceInformation);
																session.getTransaction().commit();
																HibernateUtil.closeSession();
															}
														}
													}
												} catch (Exception e) {
													// TODO Auto-generated catch
													// block
													Common.tampilErrorJikaAdmin(e);
												}
												label.setValue("");
																							} finally {
													ais.database.hibernate.HibernateUtil.closeSession();
												}
											}
										}).start();

									}

								}
							});
				}
			});
		}

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiItemHelper revisiHelper = new RevisiItemHelper(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						});
					}
				});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();

			}

		});
		if (button != null) { button.setParent(add.getParent()); }
	        FilterLanjutHelper.setup(comp);
}

	public static void uploadDataItem(final File file, final EventListener eventListener, final String[] contents)
			throws Exception {

		final Label peringatan = new Label("");
		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Item Perpustakaan");
		final Label downloadPath = new Label("");

		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
		Clients.showBusy(label.getValue());
		final Timer timer = new Timer(200);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Clients.showBusy(label.getValue());
				if (label.getValue().isEmpty()) {
					System.out.println("loading file " + file.getAbsolutePath());
					if (!downloadPath.getValue().isEmpty()) {
						try { org.zkoss.zul.Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain"); } catch (Exception eD) { ais.common.ErrorAuditUtil.record(eD, "auto-audit(empty-catch) ItemAction laporan-download"); }
					}
					MyMessageboxConfig.show(
							report.getRingkasan(),
							"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, eventListener);
					Clients.clearBusy();
					timer.detach();
				}

			}
		});
		timer.start();

		final Tbmuser tbmuser = Common.getCurrentUser();

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				try {

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);

					XSSFSheet sheet2 = null;
					try {
						sheet2 = workbook.getSheetAt(1);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/ItemAction.java:1324");
						// TODO: handle exception
					}

					ClassMetadata classMetadata = HibernateUtil.getClassMetadata(Item.class);

					int rowCount = (sheet.getLastRowNum() + 1);
					for (int i = 1; i < rowCount; i++) {
						Session session = HibernateUtil.currentNativeSession();
						try {

							Long id = Common.getSheetContentAsLong(sheet, 0, i);
							String kode = Common.getSheetContentAsString(sheet, 29, i);

							Item item = id == null || id.equals(-1L) ? null
									: (Item) session.createCriteria(Item.class).add(Restrictions.idEq(id))
											.uniqueResult();

							if (item == null && kode != null && !kode.trim().isEmpty()) {
								item = (Item) session.createCriteria(Item.class)
										.add(Restrictions.eq("kode", kode.trim())).addOrder(Order.desc("id"))
										.setMaxResults(1).uniqueResult();
							}

							String isbn = Common.getSheetContentAsString(sheet, 1, i);
							String isbn10 = Common.getSheetContentAsString(sheet, 2, i);
							if (item == null && isbn != null && !isbn.trim().isEmpty()) {
								item = (Item) session.createCriteria(Item.class)
										.add(Restrictions.eq("isbn", isbn.trim())).addOrder(Order.desc("id"))
										.setMaxResults(1).uniqueResult();
							}

							if (item == null && isbn10 != null && !isbn10.trim().isEmpty()) {
								item = (Item) session.createCriteria(Item.class)
										.add(Restrictions.eq("isbn10", isbn10.trim())).addOrder(Order.desc("id"))
										.setMaxResults(1).uniqueResult();
							}

							if (item == null) {
								item = new Item();
							}

							@SuppressWarnings("rawtypes")
							Map datum = Common.setObjectValues(classMetadata, item, contents, 1, sheet, i);
							if (item.getPenerbit() == null && datum.get("penerbit") != null
									&& !datum.get("penerbit").toString().trim().isEmpty()) {
								Penerbit penerbit = (Penerbit) session.createCriteria(Penerbit.class)
										.add(Restrictions.ilike("nama", datum.get("penerbit").toString().trim()))
										.setMaxResults(1).uniqueResult();
								if (penerbit == null) {
									penerbit = new Penerbit();
									penerbit.setNama(datum.get("penerbit").toString().trim());
									session.getTransaction().begin();
									session.save(penerbit);
									session.getTransaction().commit();
								}
								item.setPenerbit(penerbit);
							}

							if (LibraryUtil.TEXTBOOK != null && LibraryUtil.TEXTBOOK.getNama() != null
									&& item.getTipeItem().getId().equals(LibraryUtil.TEXTBOOK.getId())
									&& datum.get("tipeItem") != null
									&& !datum.get("tipeItem").toString().trim().isEmpty()
									&& !datum.get("tipeItem").toString().trim()
											.equalsIgnoreCase(LibraryUtil.TEXTBOOK.getNama().trim())) {
								TipeItem tipeItem = (TipeItem) session.createCriteria(TipeItem.class)
										.add(Restrictions.ilike("nama", datum.get("tipeItem").toString().trim()))
										.setMaxResults(1).uniqueResult();
								if (tipeItem == null) {
									tipeItem = new TipeItem();
									tipeItem.setNama(datum.get("tipeItem").toString().trim());
									session.getTransaction().begin();
									session.save(tipeItem);
									session.getTransaction().commit();
								}
								item.setTipeItem(tipeItem);
							}

							if (LibraryUtil.TEXT != null && LibraryUtil.TEXT.getNama() != null
									&& item.getJenisItem().getId().equals(LibraryUtil.TEXT.getId())
									&& datum.get("jenisItem") != null
									&& !datum.get("jenisItem").toString().trim().isEmpty() && !datum.get("jenisItem")
											.toString().trim().equalsIgnoreCase(LibraryUtil.TEXT.getNama().trim())) {
								JenisItem jenisItem = (JenisItem) session.createCriteria(JenisItem.class)
										.add(Restrictions.ilike("nama", datum.get("jenisItem").toString().trim()))
										.setMaxResults(1).uniqueResult();
								if (jenisItem == null) {
									jenisItem = new JenisItem();
									jenisItem.setNama(datum.get("jenisItem").toString().trim());
									session.getTransaction().begin();
									session.save(jenisItem);
									session.getTransaction().commit();
								}
								item.setJenisItem(jenisItem);
							}

							if ((item.getLabelItem() == null || (datum.get("labelItem") != null && !item.getLabelItem()
									.getNama().equalsIgnoreCase(datum.get("labelItem").toString().trim())))
									&& datum.get("labelItem") != null
									&& !datum.get("labelItem").toString().trim().isEmpty()) {
								LabelItem labelItem = (LabelItem) session.createCriteria(LabelItem.class)
										.add(Restrictions.ilike("nama", datum.get("labelItem").toString().trim()))
										.setMaxResults(1).uniqueResult();
								if (labelItem == null) {
									labelItem = new LabelItem();
									labelItem.setNama(datum.get("labelItem").toString().trim());
									session.getTransaction().begin();
									session.save(labelItem);
									session.getTransaction().commit();
								}
								item.setLabelItem(labelItem);
							}

							session.getTransaction().begin();
							session.saveOrUpdate(item);
							session.getTransaction().commit();

							if (!item.getPengarangs().trim().isEmpty()) {
								session.getTransaction().begin();
								session.createSQLQuery(
										"delete from library.item_punya_pengarang where item=" + item.getId())
										.executeUpdate();
								session.getTransaction().commit();
								for (String p : item.getPengarangs().trim().split(";")) {
									if (!p.trim().isEmpty()) {
										Pengarang pengarang = (Pengarang) session.createCriteria(Pengarang.class)
												.add(Restrictions.ilike("nama", p.trim())).setMaxResults(1)
												.uniqueResult();
										if (pengarang == null) {
											pengarang = new Pengarang();
											pengarang.setNama(p.trim());
											session.getTransaction().begin();
											session.save(pengarang);
											session.getTransaction().commit();
										}
										ItemPunyaPengarang itemPunyaPengarang = new ItemPunyaPengarang();
										itemPunyaPengarang.setPengarang(pengarang);
										itemPunyaPengarang.setItem(item);
										session.getTransaction().begin();
										session.save(itemPunyaPengarang);
										session.getTransaction().commit();
									}
								}
							}

							if (!item.getKategories().trim().isEmpty()) {
								session.getTransaction().begin();
								session.createSQLQuery(
										"delete from library.item_punya_kategori_item where item=" + item.getId())
										.executeUpdate();
								session.getTransaction().commit();
								for (String p : item.getKategories().trim().split(";")) {
									if (!p.trim().isEmpty()) {
										KategoriItem kategori = (KategoriItem) session
												.createCriteria(KategoriItem.class)
												.add(Restrictions.ilike("nama", p.trim())).setMaxResults(1)
												.uniqueResult();
										if (kategori == null) {
											kategori = new KategoriItem();
											kategori.setNama(p.trim());
											session.getTransaction().begin();
											session.save(kategori);
											session.getTransaction().commit();
										}

										ItemPunyaKategoriItem itemPunyaKategori = new ItemPunyaKategoriItem();
										itemPunyaKategori.setKategoriItem(kategori);
										itemPunyaKategori.setItem(item);
										session.getTransaction().begin();
										session.save(itemPunyaKategori);
										session.getTransaction().commit();

									}
								}
							}

							label.setValue("Upload data \"" + item + "\" ("
									+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
							report.sukses(i, item != null ? item.toString() : "Baris " + i, "");

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
							report.gagal(i, "Baris " + i, e, "Pastikan kode item/ISBN valid dan terdaftar di perpustakaan.");
						}
						HibernateUtil.closeSession();
					}

					try {

						if (sheet2 != null) {

							Session session = HibernateUtil.currentNativeSession();
							List<Perpustakaan> perpustakaans = new ArrayList<Perpustakaan>();
							for (int j = 5; j < sheet2.getRow(0).getLastCellNum(); j++) {
								Perpustakaan p = (Perpustakaan) Common.getSheetContentAsObject(sheet2, j, 0,
										Perpustakaan.class);
								if (p != null) {
									perpustakaans.add(p);
								}
							}

							System.out.println("perpustakaans => " + perpustakaans);

							List<SaldoAwal> saldoAwals = new ArrayList<SaldoAwal>();
							for (Perpustakaan perpustakaan : perpustakaans) {
								SaldoAwal saldoAwal = (SaldoAwal) session.createCriteria(SaldoAwal.class)
										.add(Restrictions.eq("keterangan",
												"SALDO_AWAL_OTOTAMIS_IMPORT-Data ini merupakan data yang berisi daftar buku yang otomatis diimport lewat excel"))
										.add(Restrictions.eq("perpustakaan", perpustakaan)).setMaxResults(1)
										.addOrder(Order.desc("id")).uniqueResult();

								if (saldoAwal == null) {
									saldoAwal = new SaldoAwal();
									saldoAwal.setKode(Common.getGeneratedBarCode());
									saldoAwal.setDibuatOleh(tbmuser);
									saldoAwal.setDisetujuiOleh(tbmuser);
									saldoAwal.setKeterangan(
											"SALDO_AWAL_OTOTAMIS_IMPORT-Data ini merupakan data yang berisi daftar buku yang otomatis diimport lewat excel");
									saldoAwal.setPerpustakaan(perpustakaan);
									saldoAwal.setTanggalPembuatan(ais.ui.util.WaktuUtil.getDate());
									saldoAwal.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
									saldoAwal.setIndex(-1L);
									session.getTransaction().begin();
									session.save(saldoAwal);
									session.getTransaction().commit();
								}
								saldoAwals.add(saldoAwal);

							}

							HibernateUtil.closeSession();

							rowCount = sheet2.getLastRowNum() + 1;
							System.out.println("rowCount => " + rowCount);
							for (int i = 1; i < rowCount; i++) {
								session = HibernateUtil.currentNativeSession();
								try {

									Long id = Common.getSheetContentAsLong(sheet2, 0, i);

									Item item = id == null || id.equals(-1L) ? null
											: (Item) session.createCriteria(Item.class).add(Restrictions.idEq(id))
													.uniqueResult();

									String kode = Common.getSheetContentAsString(sheet2, 1, i);
									if (item == null && kode != null && !kode.trim().isEmpty()) {
										item = (Item) session.createCriteria(Item.class)
												.add(Restrictions.eq("kode", kode.trim())).addOrder(Order.desc("id"))
												.setMaxResults(1).uniqueResult();
									}

									System.out.println("item => " + item);

									if (item != null) {

										int j = 5;

										for (Perpustakaan perpustakaan : perpustakaans) {
											SaldoAwal saldoAwal = saldoAwals.get(j - 5);
											Integer qty = Common.getSheetContentAsInteger(sheet2, j, i);
											System.out.println("item => " + item + ", qty = " + qty);
											if (qty != null && qty > 0) {
												int count = ((Number) session.createCriteria(ItemPunyaBarcode.class)
														.add(Restrictions.eq("item", item))
														.add(Restrictions.eq("perpustakaan", perpustakaan))
														.createAlias("batchItemPunyaBarcode", "batchItemPunyaBarcode")
														.add(Restrictions.eq("batchItemPunyaBarcode.saldoAwal",
																saldoAwal))
														.setProjection(Projections.rowCount()).uniqueResult())
														.intValue();
												System.out.println(
														"item => " + item + ", count = " + count + ", qty = " + qty);
												if (count < qty) {

													SaldoAwalDetail saldoAwalDetail = (SaldoAwalDetail) session
															.createCriteria(SaldoAwalDetail.class)
															.add(Restrictions.eq("saldoAwal", saldoAwal))
															.add(Restrictions.eq("item", item)).setMaxResults(1)
															.uniqueResult();

													if (saldoAwalDetail == null) {
														saldoAwalDetail = new SaldoAwalDetail();
														saldoAwalDetail.setItem(item);
														saldoAwalDetail.setSaldoAwal(saldoAwal);

													}

													BatchItemPunyaBarcode batchItemPunyaBarcode = (BatchItemPunyaBarcode) session
															.createCriteria(BatchItemPunyaBarcode.class)
															.add(Restrictions.eq("saldoAwal", saldoAwal))
															.add(Restrictions.eq("item", item)).setMaxResults(1)
															.uniqueResult();
													if (batchItemPunyaBarcode == null) {
														batchItemPunyaBarcode = new BatchItemPunyaBarcode();
														batchItemPunyaBarcode
																.setBerasalDari(BatchItemPunyaBarcode.SALDO_AWAL);
														batchItemPunyaBarcode.setDibuatOleh(tbmuser);
														batchItemPunyaBarcode.setItem(item);
														batchItemPunyaBarcode.setSaldoAwal(saldoAwal);
														batchItemPunyaBarcode
																.setTanggal(saldoAwal.getTanggalPersetujuan());

														session.getTransaction().begin();
														Common.refreshSaveOrUpdate(session, batchItemPunyaBarcode);
														session.getTransaction().commit();
													}

													saldoAwalDetail.setBatchItemPunyaBarcode(batchItemPunyaBarcode);
													saldoAwalDetail.setJumlah(qty.doubleValue());
													session.getTransaction().begin();
													Common.refreshSaveOrUpdate(session, saldoAwalDetail);
													session.getTransaction().commit();

													int selisih = qty - count;
													for (int k = 0; k < selisih; k++) {
														int indexKe = k + count + 1;
														ItemPunyaBarcode itemPunyaBarcode = (ItemPunyaBarcode) (session
																.createCriteria(ItemPunyaBarcode.class)
																.add(Restrictions.eq("batchItemPunyaBarcode",
																		batchItemPunyaBarcode))
																.add(Restrictions.eq("item", item))
																.add(Restrictions.eq("indexke", indexKe))
																.setMaxResults(1).uniqueResult());
														if (itemPunyaBarcode == null) {
															itemPunyaBarcode = new ItemPunyaBarcode();
															itemPunyaBarcode.setIndexke(indexKe);
															itemPunyaBarcode.setItem(item);
															itemPunyaBarcode
																	.setBatchItemPunyaBarcode(batchItemPunyaBarcode);
															itemPunyaBarcode.setBarcode(
																	BarcodeCommon.generateCode(batchItemPunyaBarcode));
															itemPunyaBarcode.setTanggal_dirubah(
																	ais.ui.util.WaktuUtil.getDate());
															itemPunyaBarcode
																	.setPerpustakaan(saldoAwal.getPerpustakaan());

															session.getTransaction().begin();
															Common.refreshSaveOrUpdate(session, itemPunyaBarcode);
															session.getTransaction().commit();
														}

														DetailTransaksi detailTransaksi = new DetailTransaksi();
														detailTransaksi.setSaldoAwalDetail(saldoAwalDetail);
														detailTransaksi.setItemPunyaBarcode(itemPunyaBarcode);
														detailTransaksi.setQtyBonus(0.0);

														detailTransaksi.setItem(saldoAwalDetail.getItem());
														detailTransaksi.setKeterangan("Transaksi Saldo Awal");
														detailTransaksi.setKodeTransaksi(LibraryUtil.SALDO_AWAL);
														detailTransaksi.setPerpustakaan(saldoAwal.getPerpustakaan());
														detailTransaksi.setQty(1.0);
														detailTransaksi.setTanggal(saldoAwal.getTanggalPembuatan());
														detailTransaksi
																.setTanggalDanWaktu(saldoAwal.getTanggalPembuatan());

														session.getTransaction().begin();
														Common.refreshSaveOrUpdate(session, detailTransaksi);
														session.getTransaction().commit();
													}
												}
											}
											j++;
										}
										label.setValue("Populate barcode perpustakaan data \"" + item + "\" ("
												+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
									report.sukses(i, item != null ? item.toString() : "Baris " + i, "populate barcode");
									}
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									report.gagal(i, "Baris " + i, e, "Pastikan kode item/ISBN valid dan terdaftar di perpustakaan.");
								}
								HibernateUtil.closeSession();
							}

						}

					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}

				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/library/ItemAction.java:1702");
				}

				try {
					java.io.File rptFile = report.simpanLaporan();
					downloadPath.setValue(rptFile.getAbsolutePath());
				} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) ItemAction laporan"); }
				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();
	}

	public static DspaceInformation getDspaceItemTahunPT(String cookie, Item item, PerguruanTinggi perguruanTinggi)
			throws Exception {

		String description = "Perpustakaan ";

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", item.getTahun().toString());
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", description);
		jsonPost.put("shortDescription", "Perpustakaan Repository");
		jsonPost.put("sidebarText", description);

		Konfigurasi uuidKonfigurasi = Common.getKonfigurasi("dspace_label_collection_item_tahun_" + item.getTahun(),
				"");
		return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), false, "communities",
				"communities/" + PerguruanTinggiAction.getDspace(cookie, perguruanTinggi, false) + "/communities");

	}

	public static DspaceInformation getDspaceItemTahunJurusan(String cookie, Item item) throws Exception {
		Jurusan jurusan = item.getJurusan();

		String description = "Perpustakaan " + Common.getBahasaConfig("Jurusan") + " " + jurusan.getNama();

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", item.getTahun().toString());
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", description);
		jsonPost.put("shortDescription",
				"Perpustakaan " + Common.getBahasaConfig("Jurusan") + " " + jurusan.getNama() + " Repository");
		jsonPost.put("sidebarText", description);

		Konfigurasi uuidKonfigurasi = Common.getKonfigurasi(
				"dspace_label_collection_jurusan_item_tahun_" + jurusan.getId() + "_" + item.getTahun(), "");
		return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), false, "communities",
				"communities/" + getDspaceItemJurusan(cookie, item) + "/communities");

	}

	public static DspaceInformation getDspaceItemJurusan(String cookie, Item item) throws Exception {
		Jurusan jurusan = item.getJurusan();

		String description = "Perpustakaan " + Common.getBahasaConfig("Jurusan") + " " + jurusan.getNama();

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", "Perpustakaan");
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", description);
		jsonPost.put("shortDescription",
				"Perpustakaan " + Common.getBahasaConfig("Jurusan") + " " + jurusan.getNama() + " Repository");
		jsonPost.put("sidebarText", description);

		Konfigurasi uuidKonfigurasi = Common.getKonfigurasi("dspace_label_collection_jurusan_item_" + jurusan.getId(),
				"");
		return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), false, "communities",
				"communities/" + JurusanAction.getDspace(cookie, jurusan, false) + "/communities");

	}

	public static DspaceInformation getDspaceItem(String cookie, Item item, Perpustakaan perpustakaan,
			DspaceInformation parent) throws Exception {
		Jurusan jurusana = item.getJurusan();

		String description = "Repositori " + item.getTipeItem().getNama() + " di perpustakaan "
				+ (jurusana == null ? "" : (Common.getBahasaConfig("Jurusan") + " " + jurusana.getNama()));

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", "Koleksi " + item.getTipeItem().getNama());
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", description);
		jsonPost.put("shortDescription",
				"Perpustakaan "
						+ (jurusana == null ? "" : (Common.getBahasaConfig("Jurusan") + " " + jurusana.getNama()))
						+ " Repository");
		jsonPost.put("sidebarText", description);

		Konfigurasi uuidKonfigurasi = Common
				.getKonfigurasi("dspace_label_collection_item_data_" + item.getTipeItem().getId()
						+ (perpustakaan == null || perpustakaan.getId() == null ? "" : "perpus_" + perpustakaan.getId())
						+ "_" + (jurusana == null ? "p_" + parent.getId() : jurusana.getId()), "");
		return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), false, "collections",
				"communities/" + parent + "/collections");

	}

	@SuppressWarnings("unchecked")
	public static DspaceInformation getDspace(String cookie, Item item, Perpustakaan perpustakaan, boolean update,
			DspaceInformation parent) throws Exception {

		JSONArray jsonArray = new JSONArray();

		JSONObject jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.date.copyright");
		jsonMetadata.put("value", "");
		jsonArray.put(jsonMetadata);

		Session session = HibernateUtil.currentSession();
		List<String> d = session.createCriteria(ItemPunyaPengarang.class).add(Restrictions.eq("item", item))
				.createAlias("pengarang", "pengarang").setProjection(Projections.groupProperty("pengarang.nama"))
				.list();

		for (String nama : d) {

			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.contributor.author");
			jsonMetadata.put("value", nama);
			jsonArray.put(jsonMetadata);
		}

		d = session.createCriteria(ItemPunyaKategoriItem.class).add(Restrictions.eq("item", item))
				.createAlias("kategoriItem", "kategoriItem")
				.setProjection(Projections.groupProperty("kategoriItem.nama")).list();
		for (String nama : d) {

			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.subject");
			jsonMetadata.put("value", nama);
			jsonArray.put(jsonMetadata);
		}

		d = session.createCriteria(ItemPunyaBarcode.class).add(Restrictions.eq("item", item))
				.setProjection(Projections.groupProperty("barcode")).list();
		for (String nama : d) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.identifier.other");
			jsonMetadata.put("value", nama);
			jsonArray.put(jsonMetadata);
		}

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.description.abstract");
		jsonMetadata.put("value", item.getAbstrak());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.identifier.uri");
		jsonMetadata.put("value", item.getLink());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.title");
		jsonMetadata.put("value", item.getNama());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.identifier.isbn");
		jsonMetadata.put("value", item.getIsbn());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.identifier.isbn");
		jsonMetadata.put("value", item.getIsbn10());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.identifier.issn");
		jsonMetadata.put("value", item.getIssn());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.language");
		jsonMetadata.put("value", item.getBahasa());
		jsonArray.put(jsonMetadata);

		if (item.getTipeItem() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.type");
			jsonMetadata.put("value", item.getTipeItem().getNama());
			jsonArray.put(jsonMetadata);
		}

		if (item.getJenisItem() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.format");
			jsonMetadata.put("value", item.getJenisItem().getNama());
			jsonArray.put(jsonMetadata);
		}

		if (item.getDdcItem() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.subject.ddc");
			jsonMetadata.put("value", item.getDdcItem().getKode());
			jsonArray.put(jsonMetadata);
		}

		if (item.getDewey_decimal_class() != null && !item.getDewey_decimal_class().trim().isEmpty()) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.subject.ddc");
			jsonMetadata.put("value", item.getDewey_decimal_class());
			jsonArray.put(jsonMetadata);
		}

		if (item.getDeweyDecimalClass() != null && !item.getDeweyDecimalClass().trim().isEmpty()) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.subject.ddc");
			jsonMetadata.put("value", item.getDeweyDecimalClass());
			jsonArray.put(jsonMetadata);
		}

		if (item.getPenerbit() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.publisher");
			jsonMetadata.put("value", item.getPenerbit().getNama());
			jsonArray.put(jsonMetadata);
		}

		if (item.getTanggal() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.date.issued");
			jsonMetadata.put("value", Common.databaseDateFormat.get().format(item.getTanggal()));
			jsonArray.put(jsonMetadata);
		}
		LampiranLain lampiranLain = LampiranLain.ambil(item.getId(), LampiranLain.ITEM);
		if (lampiranLain != null) {
			String uri = lampiranLain.createLinkUri(false);
			if (uri != null && !uri.trim().isEmpty()) {
				jsonMetadata = new JSONObject();
				jsonMetadata.put("key", "dc.identifier.uri");
				jsonMetadata.put("value", uri);
				jsonArray.put(jsonMetadata);
			}
		}

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("metadata", jsonArray);

		DspaceInformation dspaceInformation = DspaceInformation.dspaceProcess(cookie, item, perpustakaan,
				jsonPost.toString(), jsonArray.toString(), update, "items", "collections/" + parent + "/items",
				"items/{uuid}/metadata");

		if (lampiranLain != null) {
			DspaceInformation.upload(cookie, dspaceInformation.getUuid(), lampiranLain,
					"File \"" + item.getNama() + "\"");
		}

		Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

		List<FotoItem> fileItems = streamingSession.createCriteria(FotoItem.class).addOrder(Order.asc("id"))
				.add(Restrictions.eq("item", item.getId())).list();
		for (FotoItem fileItem : fileItems) {
			DspaceInformation.upload(cookie, dspaceInformation.getUuid(), fileItem, "File \"" + item.getNama() + "\"");
		}

		List<FotoGambarItem> fotoGambarItems = streamingSession.createCriteria(FotoGambarItem.class)
				.addOrder(Order.asc("id")).add(Restrictions.eq("item", item.getId())).list();
		for (FotoGambarItem fotoGambarItem : fotoGambarItems) {
			DspaceInformation.upload(cookie, dspaceInformation.getUuid(), fotoGambarItem,
					"Gambar \"" + item.getNama() + "\"");
		}

		List<FotoImagePerHalamanItem> fotoImagePerHalamanItems = streamingSession
				.createCriteria(FotoImagePerHalamanItem.class).addOrder(Order.asc("id"))
				.add(Restrictions.eq("item", item.getId())).list();
		for (FotoImagePerHalamanItem fotoImagePerHalamanItem : fotoImagePerHalamanItems) {
			DspaceInformation.upload(cookie, dspaceInformation.getUuid(), fotoImagePerHalamanItem,
					"Halaman " + fotoImagePerHalamanItem.getHalaman());
		}

		StreamingHibernateUtil.getInstance().closeSession();

		return dspaceInformation;
	}

	class ItemRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final ItemPunyaBarcode itemPunyaBarcode = (ItemPunyaBarcode) ((arg1 instanceof ItemPunyaBarcode) ? arg1
					: null);
			final Item item = (Item) ((arg1 instanceof Item) ? arg1 : itemPunyaBarcode.getItem());

//			LibraryUtil.checkRef(item);

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						MyWindow window = new MyWindow("", "none", false);
						window.setHeight("450px");
						window.setWidth("100%");
						window.setParent(detail);
						initDetail(item, window);
						Tbmuser tbmuser = Common.getCurrentUser();
						if (tbmuser.getMahasiswa() != null || tbmuser.ambilDosen() != null
								|| tbmuser.ambilPegawai() != null) {
							Common.freeze(window, true);
						}
					}
				}
			});

			Image image = LibraryUtil.generateImage(item);
			image.setWidth("100%");
			image.setParent(arg0);

			String jur = "";
			for (String j : item.getBy_statement().split(",")) {
				if (Common.isNumber(j)) {
					Jurusan jurusan = (Jurusan) ConstantValues.ambil(Jurusan.class.getName(), Long.parseLong(j));
					if (jurusan != null) {
						jur += jur.isEmpty() ? jurusan.getNama() : ", " + jurusan.getNama();
					}
				}
			}

			new ais.ui.util.MyHtml("<font style=\"font-size: x-small;\">"
					+ (itemPunyaBarcode != null ? "Barcode:" + itemPunyaBarcode.getBarcode() + "<br>" : "")
					+ "ISBN 10 : " + (item.getIsbn10() == null ? "" : item.getIsbn10()) + "<br>ISBN 13 : "
					+ (item.getIsbn() == null ? "" : item.getIsbn()) + "<br>ISSN : "
					+ (item.getIssn() == null ? "" : item.getIssn()) + "<br>DDC : "
					+ (item.getDdcItem() == null ? item.getDeweyDecimalClass() : item.getDdcItem())
					+ ("<br>" + Common.getBahasaConfig("Jurusan") + " : " + (jur))

					+ " </font>").setParent(arg0);

			if (item.getBolehDiDownload()) {
				Vbox vbox = new Vbox();
				vbox.setParent(arg0);
				RevisiHelper.createNewRevisi(Item.class, item, item.getNama()).setParent(vbox);

				Vbox myvbox = new Vbox();
				myvbox.setParent(vbox);

				Hbox hbox = new Hbox();
				hbox.setParent(myvbox);
				LampiranLain.createDownloadUploadFileLain(hbox, item.getId(), LampiranLain.ITEM, "E-Book", true, null,
						null, false, false, false, false, 1024 * 50);
			} else {
				RevisiHelper.createNewRevisi(Item.class, item, item.getNama()).setParent(arg0);
			}

			new Label(item.getJenisItem() == null ? "" : item.getJenisItem().getNama()).setParent(arg0);
			String penerbits = "<font style=\"font-size: x-small;\">";
			penerbits += item.getPenerbit() == null ? "" : item.getPenerbit().getNama() + "<br>";
			penerbits += item.getPenerbit2() == null ? "" : item.getPenerbit2().getNama() + "<br>";
			penerbits += item.getPenerbit3() == null ? "" : item.getPenerbit3().getNama() + "<br>";
			penerbits += item.getPenerbit4() == null ? "" : item.getPenerbit4().getNama() + "<br>";
			penerbits += item.getPenerbit5() == null ? "" : item.getPenerbit5().getNama() + "<br>";
			penerbits += "</font>";

			new ais.ui.util.MyHtml(penerbits).setParent(arg0);
			new ais.ui.util.MyHtml("<font style=\"font-size: x-small;\">" + item.getKategories() + "</font>")
					.setParent(arg0);
			new Label(item.getTipeItem() == null ? "" : item.getTipeItem().getNama()).setParent(arg0);
			new Label(item.getBahasa()).setParent(arg0);
			new Label(item.getPengarangs()).setParent(arg0);
			new Label(item.getPenaklikan()).setParent(arg0);
			new Label(item.getTahun() + "").setParent(arg0);

			Html html = new ais.ui.util.MyHtml("load..");
			html.setParent(arg0);

			html.setContent(LibraryUtil.tersediaDi(item));

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(!Boolean.FALSE.equals(item.getAktif()));
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					item.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(item);
				}
			});

			if (!ConstantValues.googleBookAktif) {
				if (item.getGoogleBookId() != null && !item.getGoogleBookId().isEmpty()) {
					checkbox.setDisabled(true);
				}
			}

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			Hbox toolbar = new Hbox();
			toolbar.setParent(vbox);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Barcode", "/img/Ecommerce-Barcode-icon.png");
			button.setOrient("vertical");
			button.setVisible(
					Common.getCurrentUser().getMahasiswa() == null && Common.getCurrentUser().getDosen() == null);
			button.setTooltiptext("Barcode");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					final MyWindow window = new MyWindow("Barcode", "none", false);
					window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					window.setHeight("95%");
					window.setWidth("800px");

					Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
					borderlayout.setParent(window);
					Center center = new Center();
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);

					center.appendChild(
							new ItemPunyaBarcodeHelper(gridBarcode = new MyGrid()).setAdd(true).initDetail(item));

					South south = new South();
					ais.ui.util.ZkCompat.setFlex(south, true);
					south.setParent(borderlayout);

					Toolbar toolbar = new Toolbar();
					// toolbar.setHeight("25px");
					toolbar.setParent(south);
					MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
					cancel.setTooltiptext("Tutup");
					cancel.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							window.detach();
							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									onSearchDefault(arg0);
								}
							});
						}
					});
					cancel.setParent(toolbar);

					window.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("Cetak", "/img/print.png");
			button.setOrient("vertical");
			button.setTooltiptext("Cetak");
			button.setVisible(
					Common.getCurrentUser().getMahasiswa() == null && Common.getCurrentUser().getDosen() == null);
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public void onEvent(Event event) throws Exception {

					if (item.getDdcItem() != null) {
						final Map parameters = ais.common.HashMapGenerator.getRand();
						parameters.put("id", item.getId());
						parameters.put("perpustakaan", Common.getCurrentPerpustakaan() == null ? ""
								: Common.getCurrentPerpustakaan().getNama());
						Report.generatePDFReport(Report.PDF, new Map[] { parameters, parameters, parameters },
								new String[] { "library/ddc_per_item", "library/ddc_per_item_yg_ada_barcode_nya",
										"library/tampil_item_barcode" },
								new String[] { "Punggung Buku", "Punggung dan Barcode", "Barcode" },
								ais.ui.util.WaktuUtil.getDate());

					} else {
						final Map parameters = ais.common.HashMapGenerator.getRand();
						parameters.put("id", item.getId());
						parameters.put("perpustakaan", Common.getCurrentPerpustakaan() == null ? ""
								: Common.getCurrentPerpustakaan().getNama());

						Report.generatePDFReport(Report.PDF, new Map[] { parameters, parameters, parameters },
								new String[] { "library/ddc_per_item_manual", "library/ddc_per_item_barcode",
										"library/tampil_item_barcode" },
								new String[] { "Punggung Buku", "Punggung dan Barcode", "Barcode" },
								ais.ui.util.WaktuUtil.getDate());
					}
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("Google", "/img/Apps-Google-Play-Books-icon.png");
			button.setOrient("vertical");
			button.setTooltiptext("Baca Buku via Google");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					JSONObject jsonObject = new JSONObject(item.getInfoLain()).getJSONObject("volumeInfo");
					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(jsonObject.getString("previewLink"), "_blank");
					} else {
						Clients.evalJavaScript("popupCenter({url: '" + jsonObject.getString("previewLink")
								+ "', title: 'Book', w: 1200, h: 600});");
					}

				}

			});
			button.setParent(toolbar);
			button.setVisible(item.getGoogleBookId() != null && !item.getGoogleBookId().trim().isEmpty());

			button = new MyToolbarbuttonConfig("Baca", "/img/Book-icon.png");
			button.setOrient("vertical");
			button.setOrient("vertical");
			button.setTooltiptext("Lihat Isi Buku");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					TampilanHasilScanPerHalamanWindow halamanWindow = new TampilanHasilScanPerHalamanWindow("Isi Buku",
							"none", true);

					halamanWindow.init(item);
					try {
						page.getFirstRoot().appendChild(halamanWindow);
						halamanWindow.onModal();
					} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				}

			});
			button.setParent(toolbar);

			Session session = StreamingHibernateUtil.getInstance().currentSession();

			int qty = ((Number) session.createCriteria(FotoImagePerHalamanItem.class)
					.add(Restrictions.eq("item", item.getId())).setProjection(Projections.rowCount()).uniqueResult())
					.intValue();

			StreamingHibernateUtil.getInstance().closeSession();

			button.setVisible(qty > 0);

			toolbar = new Hbox();
			toolbar.setParent(vbox);

			button = new MyToolbarbuttonConfig("Kutipan", "/img/eye-icon.png");
			button.setOrient("vertical");
			button.setTooltiptext("Kutipan Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					ItemAction.tampilkanKutipan(item);
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("Ubah", "/img/svg/edit-box-line.svg");
			button.setOrient("vertical");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(item);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setOrient("vertical");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(HibernateUtil.currentSession(), item);
											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});

			button.setParent(toolbar);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new Item());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public void onAddGoogle(Event event) throws Exception {
		AmbilDataDariGoogleBookBanyak ambilDataDariGoogleBookBanyak = new AmbilDataDariGoogleBookBanyak("");
		page.getFirstRoot().appendChild(ambilDataDariGoogleBookBanyak);
		ambilDataDariGoogleBookBanyak.setHeight("95%");
		ambilDataDariGoogleBookBanyak.setWidth("90%");

		ambilDataDariGoogleBookBanyak.setEventListener(new EventListener() {

			@SuppressWarnings({ "unchecked", "unused" })
			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Object> objects = (List<Object>) arg0.getData();
				for (Object object : objects) {
					Item item = (object instanceof Item) ? (Item) object
							: (object instanceof ais.database.model.library.ItemTemporary)
								? CheckISBN.itemDariItemTemporary((ais.database.model.library.ItemTemporary) object)
								: CheckISBN.simpanVolume((Volume) object, new Item());
				}

				final Timer timer = new Timer(1500);
				page.getFirstRoot().appendChild(timer);
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(arg0);
						timer.detach();

					}
				});
				timer.start();
			}
		});

		ambilDataDariGoogleBookBanyak.onModal();
	}

	public static void onAddExternal(Event event, EventListener eventListener, Item item) throws Exception {
		ItemAction itemAction = new ItemAction();
		itemAction.eventListener = eventListener;
		itemAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(itemAction.addWindow);
		itemAction.addWindow.setHeight("97%");
		itemAction.addWindow.setWidth("90%");

		itemAction.init(item);

		itemAction.addWindow.setVisible(true);
		itemAction.addWindow.onModal();
	}

	protected void initDetail(final Item item, Component component) throws Exception {
		Tabbox tabbox = new Tabbox();
		tabbox.setParent(component);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabKategoriItem = new MyTabConfig("Topik / Kategori");
		tabKategoriItem.setParent(tabs);

		final MyTabConfig tabPengarang = new MyTabConfig("Pengarang");
		tabPengarang.setParent(tabs);

		final MyTabConfig tabDocument = new MyTabConfig("File Lampiran");
		tabDocument.setParent(tabs);
		// tabDocument.setVisible(false);

		final MyTabConfig tabFotoImagePerHalamanItem = new MyTabConfig("Scan Tiap Halaman");
		tabFotoImagePerHalamanItem.setParent(tabs);

		final MyTabConfig tabPemeriksa = new MyTabConfig("Pemeriksa");
		tabPemeriksa.setParent(tabs);
		tabPemeriksa.setVisible(false);

		final MyTabConfig tabGambar = new MyTabConfig("Gambar");
		tabGambar.setParent(tabs);

		final MyTabConfig tabBarcode = new MyTabConfig("Barcode");
		tabBarcode.setParent(tabs);

		final MyTabConfig tabTerbit = new MyTabConfig("Terbit");
		tabTerbit.setParent(tabs);
		tabTerbit.setVisible(false);

		final MyTabConfig tabKomentar = new MyTabConfig("Komentar-Komentar");
		tabKomentar.setParent(tabs);
		tabKomentar.setVisible(false);

		final MyTabConfig tabAbstrak = new MyTabConfig("Abstrak dan Kata Kunci");
		tabAbstrak.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		final Tabpanel tabpanelKategoriItem = new ais.ui.util.MyTabpanel();
		tabpanelKategoriItem.setParent(tabpanels);

		final Tabpanel tabpanelPengarang = new ais.ui.util.MyTabpanel();
		tabpanelPengarang.setParent(tabpanels);

		final Tabpanel tabpanelDocument = new ais.ui.util.MyTabpanel();
		tabpanelDocument.setParent(tabpanels);
		// tabpanelDocument.setVisible(false);

		final Tabpanel tabpanelFotoImagePerHalamanItem = new ais.ui.util.MyTabpanel();
		tabpanelFotoImagePerHalamanItem.setParent(tabpanels);

		final Tabpanel tabpanelPemeriksa = new ais.ui.util.MyTabpanel();
		tabpanelPemeriksa.setParent(tabpanels);
		tabpanelPemeriksa.setVisible(false);

		final Tabpanel tabpanelGambar = new ais.ui.util.MyTabpanel();
		tabpanelGambar.setParent(tabpanels);

		final Tabpanel tabpanelBarcode = new ais.ui.util.MyTabpanel();
		tabpanelBarcode.setParent(tabpanels);

		final Tabpanel tabpanelTerbit = new ais.ui.util.MyTabpanel();
		tabpanelTerbit.setParent(tabpanels);
		tabpanelTerbit.setVisible(false);

		final Tabpanel tabpanelKomentar = new ais.ui.util.MyTabpanel();
		tabpanelKomentar.setParent(tabpanels);
		tabpanelKomentar.setVisible(false);

		final Tabpanel tabpanelAbstrak = new ais.ui.util.MyTabpanel();
		tabpanelAbstrak.setParent(tabpanels);
		tabpanelAbstrak.setWidth("100%");

		tabpanelGambar.appendChild(new ItemPunyaGambarFotoHelper(gridGambar = new MyGrid()).initDetail(item)); 

		// Tombol "Tambah Barcode" diaktifkan eksplisit di tab Barcode dialog Ubah Item: pengguna
		// yang membuka dialog ini sudah berhak mengubah item, sehingga boleh menambah barcode.
		tabpanelBarcode.appendChild(
				new ItemPunyaBarcodeHelper(gridBarcode = new MyGrid()).setAdd(true).initDetail(item));

		tabpanelTerbit.appendChild(new ItemPunyaTerbitHelper(gridTerbit = new MyGrid()).initDetail(item));

		tabpanelAbstrak.appendChild(initAbstrackDanKeyword(item));

		tabpanelKategoriItem
				.appendChild(new ItemPunyaKategoriItemHelper(gridKategoriItem = new MyGrid()).initDetail(item));

		tabpanelPengarang.appendChild(new ItemPunyaPengarangHelper(gridPengarang = new MyGrid()).initDetail(item));

		tabpanelDocument.appendChild(new ItemPunyaFotoHelper(gridDocument = new MyGrid()).initDetail(item));

		tabpanelFotoImagePerHalamanItem.appendChild(
				new FotoImagePerHalamanItemHelper(gridFotoImagePerHalamanItem = new MyGrid()).initDetail(item));

		tabpanelPemeriksa.appendChild(new ItemPunyaPemeriksaHelper(gridPemeriksa = new MyGrid()).initDetail(item));

		tabpanelKomentar.appendChild(new ItemKomentarHelper(gridKomentar = new MyGrid()).initDetail(item));
	}

	protected Component initAbstrackDanKeyword(final Item item) {
		MyDiv myvbox = new MyDiv();
		myvbox.setHeight("100%");
		myvbox.setWidth("100%");

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(myvbox);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabGambar = new MyTabConfig("Indonesia");
		tabGambar.setParent(tabs);

		final MyTabConfig tabBarcode = new MyTabConfig("English");
		tabBarcode.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		final Tabpanel tabpanelGambar = new ais.ui.util.MyTabpanel();
		tabpanelGambar.setParent(tabpanels);

		final Tabpanel tabpanelBarcode = new ais.ui.util.MyTabpanel();
		tabpanelBarcode.setParent(tabpanels);

		tabpanelGambar.appendChild(initAbstrackDanKeywordIndonesia(item));
		tabpanelBarcode.appendChild(initAbstrackDanKeywordEnglish(item));

		return myvbox;
	}

	protected Component initAbstrackDanKeywordIndonesia(final Item item) {

		MyDiv myvbox = new MyDiv();

		myvbox.setHeight("100%");
		myvbox.setWidth("100%");
		myvbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Abstrak")));
		abstrak = new Textbox(item.getAbstrak());
		myvbox.appendChild(abstrak);
		abstrak.setRows(17);
		abstrak.setWidth("97%");
		abstrak.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (item.getId() != null) {
					item.setAbstrak(abstrak.getValue());
					Session session = HibernateUtil.currentSession();
					session.refresh(item);
					session.update(item);
				}
			}
		});

		myvbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Kata_Kunci")));
		kewords = new Textbox(item.getKewords());
		kewords.setRows(2);
		myvbox.appendChild(kewords);
		kewords.setWidth("97%");
		kewords.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (item.getId() != null) {
					item.setKewords(kewords.getValue());
					Session session = HibernateUtil.currentSession();
					session.refresh(item);
					session.update(item);
				}
			}
		});
		return myvbox;
	}

	protected Component initAbstrackDanKeywordEnglish(final Item item) {

		MyDiv myvbox = new MyDiv();

		myvbox.setHeight("100%");
		myvbox.setWidth("100%");
		myvbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Abstract")));
		abstrakEn = new Textbox(item.getAbstrakEn());
		myvbox.appendChild(abstrakEn);
		abstrakEn.setRows(17);
		abstrakEn.setWidth("97%");
		abstrakEn.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (item.getId() != null) {
					item.setAbstrakEn(abstrakEn.getValue());
					Session session = HibernateUtil.currentSession();
					session.refresh(item);
					session.update(item);
				}
			}
		});

		myvbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Keywords")));
		kewordsEn = new Textbox(item.getKewordsEn());
		kewordsEn.setRows(2);
		myvbox.appendChild(kewordsEn);
		kewordsEn.setWidth("97%");
		kewordsEn.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (item.getId() != null) {
					item.setKewordsEn(kewordsEn.getValue());
					Session session = HibernateUtil.currentSession();
					session.refresh(item);
					session.update(item);
				}
			}
		});
		return myvbox;
	}

	@SuppressWarnings("unchecked")
	protected void init(final Item item) throws Exception {
		this.item = item;
		addWindow.setTitle(item.getId() == null ? "Tambah Item" : "Ubah Item");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		if (item.getId() != null && !item.getPengarangs().replaceAll(",", "").trim().isEmpty()) {
			Session session = HibernateUtil.currentSession();
			int jumlahPengarang = ((Number) session.createCriteria(ItemPunyaPengarang.class)
					.add(Restrictions.eq("item", item)).setProjection(Projections.rowCount()).uniqueResult())
					.intValue();
			if (jumlahPengarang == 0) {
				String[] p = item.getPengarangs().split(",");
				for (String s : p) {
					if (!s.trim().isEmpty()) {
						Pengarang pengarang = (Pengarang) session.createCriteria(Pengarang.class)
								.add(Restrictions.ilike("nama", s.trim(), MatchMode.EXACT)).setMaxResults(1)
								.uniqueResult();
						if (pengarang == null) {
							pengarang = new Pengarang();
							pengarang.setNama(s.trim());
							Common.refreshSaveOrUpdate(session, pengarang);
						}

						ItemPunyaPengarang itemPunyaPengarang = (ItemPunyaPengarang) session
								.createCriteria(ItemPunyaPengarang.class).add(Restrictions.eq("item", item))
								.add(Restrictions.eq("pengarang", pengarang)).setMaxResults(1).uniqueResult();
						if (itemPunyaPengarang == null) {
							itemPunyaPengarang = new ItemPunyaPengarang();
							itemPunyaPengarang.setItem(item);
							itemPunyaPengarang.setPengarang(pengarang);
							Common.refreshSaveOrUpdate(session, itemPunyaPengarang);
						}
					}
				}
			}
		}

		if (item.getId() != null && !item.getKategories().replaceAll(",", "").trim().isEmpty()) {
			Session session = HibernateUtil.currentSession();
			int jumlahKategoriItem = ((Number) session.createCriteria(ItemPunyaKategoriItem.class)
					.add(Restrictions.eq("item", item)).setProjection(Projections.rowCount()).uniqueResult())
					.intValue();
			if (jumlahKategoriItem == 0) {
				String[] p = item.getKategories().split(",");
				for (String s : p) {
					if (!s.trim().isEmpty()) {
						KategoriItem kategoriItem = (KategoriItem) session.createCriteria(KategoriItem.class)
								.add(Restrictions.ilike("nama", s.trim(), MatchMode.EXACT)).setMaxResults(1)
								.uniqueResult();
						if (kategoriItem == null) {
							kategoriItem = new KategoriItem();
							kategoriItem.setNama(s.trim());
							Common.refreshSaveOrUpdate(session, kategoriItem);
						}

						ItemPunyaKategoriItem itemPunyaKategoriItem = (ItemPunyaKategoriItem) session
								.createCriteria(ItemPunyaKategoriItem.class).add(Restrictions.eq("item", item))
								.add(Restrictions.eq("kategoriItem", kategoriItem)).setMaxResults(1).uniqueResult();
						if (itemPunyaKategoriItem == null) {
							itemPunyaKategoriItem = new ItemPunyaKategoriItem();
							itemPunyaKategoriItem.setItem(item);
							itemPunyaKategoriItem.setKategoriItem(kategoriItem);
							Common.refreshSaveOrUpdate(session, itemPunyaKategoriItem);
						}
					}
				}
			}
		}

		East east = new East();
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setWidth("50%");

		initDetail(item, east);

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

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("60%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("ISBN 13"));
		row.appendChild(isbn = new Textbox(item.getIsbn()));
		isbn.setWidth("90%");

		isbn.addEventListener("onFocus", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				isbn.select();
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("ISBN 10"));
		row.appendChild(isbn10 = new Textbox(item.getIsbn10()));
		isbn10.setWidth("90%");

		isbn10.addEventListener("onFocus", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				isbn10.select();
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("ISSN"));
		row.appendChild(issn = new Textbox(item.getIssn()));
		issn.setWidth("90%");

		issn.addEventListener("onFocus", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				issn.select();
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul"));
		row.appendChild(nama = new Textbox(item.getNama() == null ? "" : item.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tema / Sub Judul"));
		row.appendChild(tema = new Textbox(item.getTema()));
		tema.setWidth("90%");
		tema.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("E-Book (PDF)"));
		Hbox hbox = new Hbox();
		lainMahasiswa = null;
		LampiranLain.createDownloadUploadFileLain(hbox, item.getId(), LampiranLain.ITEM, "E-Book", true,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();

						if (item.getId() != null) {
							Session session = HibernateUtil.currentSession();
							session.refresh(item);
							item.setLampiranPath(lainMahasiswa.ambilFile().getAbsolutePath());
							Common.refreshUpdate(session, item);
						}
					}
				}, 1024 * 100);
		Vbox vbox = new Vbox();
		vbox.setParent(row);
		hbox.setParent(vbox);
		vbox.appendChild(bolehDiDownload = new MyCheckboxConfig("Tersedia juga buku elektronik"));
		bolehDiDownload.setChecked(item.getBolehDiDownload());

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Label / Klasifikasi"));
		row.appendChild(labelItem = new AmbilDataLabelItemBanbox());
		labelItem.setAttribute("labelItem", item.getLabelItem());
		labelItem.setValue(item.getLabelItem() == null ? "" : item.getLabelItem().getNama());
		labelItem.setWidth("90%");
		labelItem.setReadonly(true);

		Common.initKeterangan(rows, "Jika label tidak diisi, maka bisa langsung memilih klasifikasi");

		rowDdc = new MyFormRow();
		rowDdc.setStyle("border:0px;background: transparent;");
		rowDdc.setParent(rows);
		rowDdc.appendChild(new ais.ui.util.MyLabelConfig("Klasifikasi (DDC)"));
		rowDdc.appendChild(ddcItem = new AmbilDataDdcItemBanbox());
		ddcItem.setAttribute("ddcItem", item.getDdcItem());
		ddcItem.setValue(item.getDdcItem() == null ? "" : item.getDdcItem().toString());
		ddcItem.setWidth("90%");

		Common.initKeterangan(rows, "Atau isi kode klasifikasi secara manual");

		rowKlasifikasi = new MyFormRow();
		rowKlasifikasi.setStyle("border:0px;background: transparent;");
		rowKlasifikasi.setParent(rows);
		rowKlasifikasi.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Klasifikasi Manual (Jika DDC tidak diisi)")));
		rowKlasifikasi.appendChild(deweyDecimalClass = new Textbox(item.getDeweyDecimalClass()));
		deweyDecimalClass.setWidth("90%");

		EventListener eventListener1 = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				rowKlasifikasi.setVisible(ddcItem.getAttribute("ddcItem") == null);
			}
		};

		eventListener1.onEvent(null);
		ddcItem.setEventListener(eventListener1);

		EventListener eventListener2 = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (labelItem.getAttribute("labelItem") != null) {
					rowKlasifikasi.setVisible(false);
					rowDdc.setVisible(false);
				} else {
					rowDdc.setVisible(true);
					rowKlasifikasi.setVisible(ddcItem.getAttribute("ddcItem") == null);
				}

			}
		};

		eventListener2.onEvent(null);
		labelItem.setEventListener(eventListener2);

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("UDC"));
		row.appendChild(udcItem = new AmbilDataUdcItemBanbox());
		udcItem.setAttribute("udcItem", item.getUdcItem());
		udcItem.setValue(item.getUdcItem() == null ? "" : item.getUdcItem().toString());
		udcItem.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bahasa"));
		row.appendChild(bahasa = new Textbox(item.getBahasa()));
		bahasa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Edisi"));
		row.appendChild(edisi = new Textbox(item.getEdisi()));
		edisi.setWidth("90%");

		// Opsi NOMOR PUNGGUNG MANUAL. Default TIDAK terpilih → mode OTOMATIS (nilai diturunkan dari
		// data item saat cetak, pola LaporanBarcodeItem). Bila dicentang, 4 baris entry manual di
		// bawah baru ditampilkan (via listener onCheck).
		MyFormRow rowCekPunggung = new MyFormRow();
		rowCekPunggung.setParent(rows);
		rowCekPunggung.appendChild(new ais.ui.util.MyLabelConfig("Nomor Punggung Manual"));
		cbPunggungManual = new Checkbox();
		cbPunggungManual.setChecked(Boolean.TRUE.equals(item.getPunggungManual()));
		rowCekPunggung.appendChild(cbPunggungManual);

		final MyFormRow rowPunggungKlasifikasi = new MyFormRow();
		rowPunggungKlasifikasi.setParent(rows);
		rowPunggungKlasifikasi.appendChild(new ais.ui.util.MyLabelConfig("No Klasifikasi (kode koleksi di rak)"));
		rowPunggungKlasifikasi.appendChild(punggungKlasifikasi = new Textbox(item.getPunggungKlasifikasi()));
		punggungKlasifikasi.setWidth("90%");

		final MyFormRow rowPunggungPengarang = new MyFormRow();
		rowPunggungPengarang.setParent(rows);
		rowPunggungPengarang.appendChild(new ais.ui.util.MyLabelConfig("Tiga huruf nama belakang pengarang"));
		rowPunggungPengarang.appendChild(punggungPengarang = new Textbox(item.getPunggungPengarang()));
		punggungPengarang.setWidth("90%");

		final MyFormRow rowPunggungJudul = new MyFormRow();
		rowPunggungJudul.setParent(rows);
		rowPunggungJudul.appendChild(new ais.ui.util.MyLabelConfig("Huruf pertama judul buku"));
		rowPunggungJudul.appendChild(punggungJudul = new Textbox(item.getPunggungJudul()));
		punggungJudul.setWidth("90%");

		final MyFormRow rowPunggungEdisi = new MyFormRow();
		rowPunggungEdisi.setParent(rows);
		rowPunggungEdisi.appendChild(new ais.ui.util.MyLabelConfig("Edisi / Copy (mis. C.1)"));
		rowPunggungEdisi.appendChild(punggungEdisi = new Textbox(item.getPunggungEdisi()));
		punggungEdisi.setWidth("90%");

		// Tampilkan baris entry manual hanya bila checkbox dicentang; jika tidak, sembunyikan
		// (kembali ke mode otomatis).
		EventListener togglePunggungManual = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				boolean manual = cbPunggungManual.isChecked();
				rowPunggungKlasifikasi.setVisible(manual);
				rowPunggungPengarang.setVisible(manual);
				rowPunggungJudul.setVisible(manual);
				rowPunggungEdisi.setVisible(manual);
			}
		};
		cbPunggungManual.addEventListener("onCheck", togglePunggungManual);
		togglePunggungManual.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Deskripsi Fisik / Penaklikan"));
		row.appendChild(penaklikan = new Textbox(item.getPenaklikan()));
		penaklikan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jurusan"));

		jurusans = new ArrayList<Checkbox>();
		Map<Serializable, Jurusan> a = ConstantValues.ambilBerdasarClass(Jurusan.class);
		for (Jurusan jurusan : a.values()) {
			if (jurusan.getAktif()) {
				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				Checkbox checkbox = new Checkbox(jurusan.getNama());
				checkbox.setAttribute("jurusan", jurusan);
				row.appendChild(checkbox);
				checkbox.setChecked(item.getBy_statement().contains("," + jurusan.getId() + ","));
				jurusans.add(checkbox);
			}
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis"));
		row.appendChild(jenisItem = new Combobox());
		Common.insertCombo(jenisItem, "nama", JenisItem.class);
		Common.selectComboItem(jenisItem, item.getJenisItem());
		jenisItem.setWidth("90%");
		jenisItem.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tipe Default"));
		row.appendChild(tipeItem = new Combobox());
		Common.insertCombo(tipeItem, "nama", TipeItem.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(tipeItem, item.getTipeItem());
		tipeItem.setWidth("90%");
		if (item.getDefaultSatuanKerja() != null) {
			tipeItem.setDisabled(true);
		}
		tipeItem.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penerbit"));
		row.appendChild(penerbit = new AmbilDataPenerbitBanbox());
		penerbit.setAttribute("penerbit", item.getPenerbit());
		penerbit.setValue(item.getPenerbit() == null ? "" : item.getPenerbit().getNama());
		penerbit.setWidth("90%");
		penerbit.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penerbit Lain 1"));
		row.appendChild(penerbit1 = new AmbilDataPenerbitBanbox());
		penerbit1.setAttribute("penerbit", item.getPenerbit2());
		penerbit1.setValue(item.getPenerbit2() == null ? "" : item.getPenerbit2().getNama());
		penerbit1.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penerbit Lain 2"));
		row.appendChild(penerbit2 = new AmbilDataPenerbitBanbox());
		penerbit2.setAttribute("penerbit", item.getPenerbit3());
		penerbit2.setValue(item.getPenerbit3() == null ? "" : item.getPenerbit3().getNama());
		penerbit2.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penerbit Lain 3"));
		row.appendChild(penerbit3 = new AmbilDataPenerbitBanbox());
		penerbit3.setAttribute("penerbit", item.getPenerbit4());
		penerbit3.setValue(item.getPenerbit4() == null ? "" : item.getPenerbit4().getNama());
		penerbit3.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penerbit Lain 4"));
		row.appendChild(penerbit4 = new AmbilDataPenerbitBanbox());
		penerbit4.setAttribute("penerbit", item.getPenerbit5());
		penerbit4.setValue(item.getPenerbit5() == null ? "" : item.getPenerbit5().getNama());
		penerbit4.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Dimasukkan"));
		row.appendChild(tanggal = new MyDatebox(item.getTanggal()));
		tanggal.setFormat(Common.dateFormat1.get().toPattern());
		tanggal.setWidth("90%");

		row = new MyFormRow();
				row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Terbit"));
		row.appendChild(tanggalTerbit = new MyDatebox(item.getTanggalterbit()));
		tanggalTerbit.setFormat(Common.dateFormat.get().toPattern());
		tanggalTerbit.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Terbit"));
		row.appendChild(tahun = new Intbox(item.getTahun()));
		tahun.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tempat Terbit"));
		row.appendChild(tempatterbit = new Textbox(item.getTempatterbit()));
		tempatterbit.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Halaman"));
		row.appendChild(halaman = new Intbox(item.getHalaman()));
		halaman.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Link"));
		row.appendChild(link = new Textbox(item.getLink()));
		link.setWidth("90%");
		link.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Link Gambar"));
		row.appendChild(imageUrl = new Textbox(item.getImageUrl()));
		imageUrl.setWidth("90%");
		imageUrl.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Catatan"));
		row.appendChild(catatan = new Textbox(item.getCatatan()));
		catatan.setWidth("90%");
		catatan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(item.getKeterangan() == null ? "" : item.getKeterangan()));
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

					if (eventListener != null) {
						eventListener.onEvent(new Event("", addWindow, ItemAction.this.item));
					}
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Item harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (penerbit.getAttribute("penerbit") == null) {
			MyMessageboxConfig.show("Penerbit utama harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		List<Row> rowsPengarang = gridPengarang.getRows().getChildren();
		for (Row row : rowsPengarang) {
			if (!row.isVisible()) {
				continue;
			}
			ItemPunyaPengarang itemPunyaPengarang = (ItemPunyaPengarang) row.getAttribute("itemPunyaPengarang");
			if (itemPunyaPengarang.getPengarang() == null) {
				MyMessageboxConfig.show("Pengarang harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		List<Row> rowsKategoriItem = gridKategoriItem.getRows().getChildren();
		for (Row row : rowsKategoriItem) {
			if (!row.isVisible()) {
				continue;
			}
			ItemPunyaKategoriItem itemPunyaKategoriItem = (ItemPunyaKategoriItem) row
					.getAttribute("itemPunyaKategoriItem");
			if (itemPunyaKategoriItem.getKategoriItem() == null) {
				MyMessageboxConfig.show("Kategori Item harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		List<Row> rowsPemeriksa = gridPemeriksa.getRows().getChildren();
		for (Row row : rowsPemeriksa) {
			if (!row.isVisible()) {
				continue;
			}
			ItemPunyaPemeriksa itemPunyaPemeriksa = (ItemPunyaPemeriksa) row.getAttribute("itemPunyaPemeriksa");
			if (itemPunyaPemeriksa.getPemeriksa() == null) {
				MyMessageboxConfig.show("Pemeriksa harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		List<Row> rowsBarcode = gridBarcode.getRows().getChildren();
		for (Row row : rowsBarcode) {
			if (!row.isVisible()) {
				continue;
			}
			ItemPunyaBarcode itemPunyaBarcode = (ItemPunyaBarcode) row.getAttribute("itemPunyaBarcode");
			if (itemPunyaBarcode != null && itemPunyaBarcode.getBarcode() == null) {
				MyMessageboxConfig.show("Barcode harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		List<Row> rowsTerbit = gridTerbit.getRows().getChildren();
		for (Row row : rowsTerbit) {
			if (!row.isVisible()) {
				continue;
			}
			ItemPunyaTerbit itemPunyaTerbit = (ItemPunyaTerbit) row.getAttribute("itemPunyaTerbit");
			if (itemPunyaTerbit.getMulai() == null) {
				MyMessageboxConfig.show("Mulai terbit harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		List<Row> rowsFotoGambar = gridGambar.getRows().getChildren();
		for (Row row : rowsFotoGambar) {
			if (!row.isVisible()) {
				continue;
			}
			FotoGambarItem fotoGambarItem = (FotoGambarItem) row.getAttribute("fotoGambarItem");
			if (fotoGambarItem.getItem() == null) {
				MyMessageboxConfig.show("Gambar harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		List<Row> rowsDocument = gridDocument.getRows().getChildren();
		for (Row row : rowsDocument) {
			if (!row.isVisible()) {
				continue;
			}
			FotoItem fotoItem = (FotoItem) row.getAttribute("fotoItem");
			if (fotoItem.getItem() == null) {
				MyMessageboxConfig.show("File harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		List<Row> rowsFotoImagePerHalamanItem = gridFotoImagePerHalamanItem.getRows().getChildren();
		for (Row row : rowsFotoImagePerHalamanItem) {
			if (!row.isVisible()) {
				continue;
			}
			FotoImagePerHalamanItem fotoImagePerHalamanItem = (FotoImagePerHalamanItem) row
					.getAttribute("fotoImagePerHalamanItem");
			if (fotoImagePerHalamanItem.getItem() == null) {
				MyMessageboxConfig.show("Scan Tiap Halaman harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		Session session = HibernateUtil.currentSession();
		if (item.getId() != null) {
			item = (Item) session.load(Item.class, item.getId());

		}

		item.setLabelItem((LabelItem) labelItem.getAttribute("labelItem"));
		item.setTanggalterbit(tanggalTerbit.getValue());
		item.setTema(tema.getValue());
		item.setPengarangs(null);
		item.setNama(nama.getValue());
		item.setKeterangan(keterangan.getValue());
		item.setBahasa(bahasa.getValue());
		item.setCatatan(catatan.getValue());
		item.setEdisi(edisi.getValue());
		item.setHalaman(halaman.getValue());
		item.setIsbn(isbn.getValue().trim());
		item.setIsbn10(isbn10.getValue().trim());
		item.setIssn(issn.getValue().trim());
		item.setJenisItem(
				(JenisItem) (jenisItem.getSelectedItem() == null ? null : jenisItem.getSelectedItem().getValue()));
		item.setLink(link.getValue());
		item.setPenaklikan(penaklikan.getValue());
		item.setTanggal(tanggal.getValue());

		item.setPenerbit2((Penerbit) penerbit1.getAttribute("penerbit"));
		item.setPenerbit3((Penerbit) penerbit2.getAttribute("penerbit"));
		item.setPenerbit4((Penerbit) penerbit3.getAttribute("penerbit"));
		item.setPenerbit5((Penerbit) penerbit4.getAttribute("penerbit"));

		item.setPenerbit((Penerbit) penerbit.getAttribute("penerbit"));
		item.setTahun(tahun.getValue());
		item.setTipeItem(
				(TipeItem) (tipeItem.getSelectedItem() == null ? null : tipeItem.getSelectedItem().getValue()));
		item.setAbstrak(abstrak.getValue());
		item.setKewords(kewords.getValue());
		item.setAbstrakEn(abstrakEn.getValue());
		item.setKewordsEn(kewordsEn.getValue());
		item.setImageUrl(imageUrl.getValue().trim());

		item.setDdcItem((DdcItem) ddcItem.getAttribute("ddcItem"));
		item.setUdcItem((UdcItem) udcItem.getAttribute("udcItem"));
		item.setDewey_decimal_class(deweyDecimalClass.getValue());
		item.setDeweyDecimalClass(deweyDecimalClass.getValue());

		// Opsi + komponen label punggung buku (dientri terpisah).
		item.setPunggungManual(cbPunggungManual != null && cbPunggungManual.isChecked());
		item.setPunggungKlasifikasi(punggungKlasifikasi == null ? null : punggungKlasifikasi.getValue());
		item.setPunggungPengarang(punggungPengarang == null ? null : punggungPengarang.getValue());
		item.setPunggungJudul(punggungJudul == null ? null : punggungJudul.getValue());
		item.setPunggungEdisi(punggungEdisi == null ? null : punggungEdisi.getValue());

		String jur = "";
		for (Checkbox c : jurusans) {
			if (c.isChecked()) {
				Jurusan jurusan = (Jurusan) c.getAttribute("jurusan");
				if (jurusan != null) {
					jur += "," + jurusan.getId();
				}
			}
		}
		if (!jur.isEmpty()) {
			jur += ",";
		}

		item.setBy_statement(jur);
		item.setJurusan(null);

		item.setBolehDiDownload(bolehDiDownload.isChecked());
		item.setTempatterbit(tempatterbit.getValue());

		// Cegah duplicate unique key kode__unik_buku (ISBN/judul/edisi/kode menghasilkan kode unik
		// yang sama) → tampilkan pesan ramah, bukan ConstraintViolationException yang menggagalkan
		// transaksi. Pengecekan dibungkus try/catch agar perilaku lama tetap jalan bila gagal.
		try {
			if (item.getNama() != null) {
				String kodeUnik = item.getKodeUnikBuku();
				if (kodeUnik != null) {
					org.hibernate.Criteria cekDup = session.createCriteria(Item.class)
							.add(Restrictions.eq("kodeUnikBuku", kodeUnik)).setMaxResults(1);
					if (item.getId() != null) {
						cekDup.add(Restrictions.ne("id", item.getId()));
					}
					Item dupItem = (Item) cekDup.uniqueResult();
					if (dupItem != null) {
						ais.ui.util.MyMessageboxConfig.show(
								"Buku dengan identitas yang sama sudah terdaftar (ISBN/judul/edisi/kode menghasilkan kode unik yang sama). Mohon periksa daftar buku atau ubah datanya.");
						return false;
					}
				}
			}
		} catch (Exception eCekDup) { ais.common.ErrorAuditUtil.record(eCekDup, "auto-audit(empty-catch) src/ais/action/master/library/ItemAction.java:3314");
			// Pengecekan gagal (mis. nama kosong) → lanjutkan ke proses simpan seperti semula.
		}

		if (item.getAktif() == null) {
			item.setAktif(true);
		}

		if (item.getId() != null) {
			Common.refreshUpdate(session, item);
		} else {
			session.save(item);
		}

		if (ddcItem.getAttribute("ddcItem") != null) {
			DdcItem ddcItem = (DdcItem) this.ddcItem.getAttribute("ddcItem");
			DataDdcItem dataDdcItem = (DataDdcItem) session.createCriteria(DataDdcItem.class)
					.add(Restrictions.eq("ddcItem", ddcItem)).setMaxResults(1).uniqueResult();
			if (dataDdcItem == null) {
				dataDdcItem = new DataDdcItem();
				dataDdcItem.setDdcItem(ddcItem);
				dataDdcItem.setKeterangan("");
				session.save(dataDdcItem);
			}

			DataDdcItemDetail dataDdcItemDetail = (DataDdcItemDetail) session.createCriteria(DataDdcItemDetail.class)
					.add(Restrictions.eq("item", item)).add(Restrictions.eq("dataDdcItem", dataDdcItem))
					.setMaxResults(1).uniqueResult();
			if (dataDdcItemDetail == null) {
				dataDdcItemDetail = new DataDdcItemDetail();
				dataDdcItemDetail.setDataDdcItem(dataDdcItem);
				dataDdcItemDetail.setItem(item);
				dataDdcItemDetail.setKeterangan("");
				session.save(dataDdcItemDetail);
			}
		} else {
			String sql = "delete from library.data_ddc_item_detail where item = " + item.getId();
			session.createSQLQuery(sql).executeUpdate();
		}

		if (udcItem.getAttribute("udcItem") != null) {
			UdcItem udcItem = (UdcItem) this.udcItem.getAttribute("udcItem");
			DataUdcItem dataUdcItem = (DataUdcItem) session.createCriteria(DataUdcItem.class)
					.add(Restrictions.eq("udcItem", udcItem)).setMaxResults(1).uniqueResult();
			if (dataUdcItem == null) {
				dataUdcItem = new DataUdcItem();
				dataUdcItem.setUdcItem(udcItem);
				dataUdcItem.setKeterangan("");
				session.save(dataUdcItem);
			}

			DataUdcItemDetail dataUdcItemDetail = (DataUdcItemDetail) session.createCriteria(DataUdcItemDetail.class)
					.add(Restrictions.eq("item", item)).add(Restrictions.eq("dataUdcItem", dataUdcItem))
					.setMaxResults(1).uniqueResult();
			if (dataUdcItemDetail == null) {
				dataUdcItemDetail = new DataUdcItemDetail();
				dataUdcItemDetail.setDataUdcItem(dataUdcItem);
				dataUdcItemDetail.setItem(item);
				dataUdcItemDetail.setKeterangan("");
				session.save(dataUdcItemDetail);
			}
		} else {
			String sql = "delete from library.data_udc_item_detail where item = " + item.getId();
			session.createSQLQuery(sql).executeUpdate();
		}

		for (Row row : rowsPengarang) {
			if (!row.isVisible()) {
				continue;
			}
			ItemPunyaPengarang itemPunyaPengarang = (ItemPunyaPengarang) row.getAttribute("itemPunyaPengarang");
			itemPunyaPengarang.setItem(item);
			session.saveOrUpdate(itemPunyaPengarang);
		}

		for (Row row : rowsKategoriItem) {
			if (!row.isVisible()) {
				continue;
			}
			ItemPunyaKategoriItem itemPunyaKategoriItem = (ItemPunyaKategoriItem) row
					.getAttribute("itemPunyaKategoriItem");
			itemPunyaKategoriItem.setItem(item);
			session.saveOrUpdate(itemPunyaKategoriItem);
		}

		for (Row row : rowsPemeriksa) {
			if (!row.isVisible()) {
				continue;
			}
			ItemPunyaPemeriksa itemPunyaPemeriksa = (ItemPunyaPemeriksa) row.getAttribute("itemPunyaPemeriksa");
			itemPunyaPemeriksa.setItem(item);
			session.saveOrUpdate(itemPunyaPemeriksa);
		}

		for (Row row : rowsBarcode) {
			if (!row.isVisible()) {
				continue;
			}
			ItemPunyaBarcode itemPunyaBarcode = (ItemPunyaBarcode) row.getAttribute("itemPunyaBarcode");
			if (itemPunyaBarcode != null) {
				itemPunyaBarcode.setItem(item);
				session.saveOrUpdate(itemPunyaBarcode);
			}
		}

		for (Row row : rowsTerbit) {
			if (!row.isVisible()) {
				continue;
			}
			ItemPunyaTerbit itemPunyaTerbit = (ItemPunyaTerbit) row.getAttribute("itemPunyaTerbit");
			itemPunyaTerbit.setItem(item);
			session.saveOrUpdate(itemPunyaTerbit);
		}

		Integer jumlahPemeriksa = ((Number) session.createCriteria(ItemPunyaPemeriksa.class)
				.add(Restrictions.eq("item", item)).setProjection(Projections.rowCount()).uniqueResult()).intValue();
		if (jumlahPemeriksa.equals(0)) {
			List<PenerbitPunyaPemeriksa> penerbitPunyaPemeriksas = session.createCriteria(PenerbitPunyaPemeriksa.class)
					.add(Restrictions.eq("penerbit", item.getPenerbit())).list();
			for (PenerbitPunyaPemeriksa penerbitPunyaPemeriksa : penerbitPunyaPemeriksas) {
				ItemPunyaPemeriksa itemPunyaPemeriksa = new ItemPunyaPemeriksa();
				itemPunyaPemeriksa.setItem(item);
				itemPunyaPemeriksa.setPemeriksa(penerbitPunyaPemeriksa.getPemeriksa());
				session.save(itemPunyaPemeriksa);
			}
		}

		List<String> strings = session.createCriteria(ItemPunyaPengarang.class).createAlias("pengarang", "pengarang")
				.setProjection(Projections.groupProperty("pengarang.nama")).add(Restrictions.eq("item", item)).list();
		String pengarangs = strings.toString().replaceAll("\\[", "").replaceAll("\\]", "");
		item.setPengarangs(pengarangs);

		strings = session.createCriteria(ItemPunyaKategoriItem.class).createAlias("kategoriItem", "kategoriItem")
				.setProjection(Projections.groupProperty("kategoriItem.nama")).add(Restrictions.eq("item", item))
				.list();
		String kategories = "";
		for (String s : strings) {
			kategories += kategories.equals("") ? "[" + s + "]" : ", [" + s + "]";
		}
		item.setKategories(kategories);

		String links = "";
		Session mysession = StreamingHibernateUtil.getInstance().currentSession();
		try {
			mysession.getTransaction().begin();
			for (Row row : rowsFotoGambar) {
				if (!row.isVisible()) {
					continue;
				}
				FotoGambarItem fotoGambarItem = (FotoGambarItem) row.getAttribute("fotoGambarItem");
				fotoGambarItem.setItem(item.getId());
				mysession.saveOrUpdate(fotoGambarItem);
			}

			for (Row row : rowsDocument) {
				if (!row.isVisible()) {
					continue;
				}
				FotoItem fotoItem = (FotoItem) row.getAttribute("fotoItem");
				fotoItem.setItem(item.getId());
				mysession.saveOrUpdate(fotoItem);
			}
			mysession.getTransaction().commit();

			for (Row row : rowsFotoImagePerHalamanItem) {
				if (!row.isVisible()) {
					continue;
				}
				FotoImagePerHalamanItem fotoImagePerHalamanItem = (FotoImagePerHalamanItem) row
						.getAttribute("fotoImagePerHalamanItem");
				fotoImagePerHalamanItem.setItem(item.getId());
				mysession.saveOrUpdate(fotoImagePerHalamanItem);

				String s = CommonMedia.getImageItemPerHalaman(item == null || item.getId() == null ? -1L : item.getId(),
						fotoImagePerHalamanItem.getId(), fotoImagePerHalamanItem.getHalaman(), 300, 250, false);

				links += links.isEmpty() ? s : ";" + fotoImagePerHalamanItem.getHalamanIndex() + "," + s;
			}

		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
		}

		StreamingHibernateUtil.getInstance().closeSession();

		for (Row row : rowsFotoGambar) {
			if (!row.isVisible()) {
				continue;
			}
			FotoGambarItem fotoGambarItem = (FotoGambarItem) row.getAttribute("fotoGambarItem");
			if (fotoGambarItem.getPath() != null && !fotoGambarItem.getPath().trim().isEmpty()
					&& new File(fotoGambarItem.getPath()).exists()) {
				item.setImagePath(fotoGambarItem.getPath());
				break;
			}
		}

		// for (Row row : rowsDocument) {
		// FotoItem fotoGambarItem = (FotoItem) row.getAttribute("fotoItem");
		// if (fotoGambarItem.getPath() != null &&
		// !fotoGambarItem.getPath().trim().isEmpty()
		// && new File(fotoGambarItem.getPath()).exists()) {
		// item.setLampiranPath(fotoGambarItem.getPath());
		// break;
		// }
		// }

		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {

			item.setLampiranPath(lainMahasiswa.ambilFile().getAbsolutePath());

			try {
				Session sessiona = StreamingHibernateUtil.getInstance().currentSession();

				sessiona.refresh(lainMahasiswa);
				lainMahasiswa.setRef(item.getId());

				sessiona.getTransaction().begin();
				sessiona.update(lainMahasiswa);
				sessiona.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
		}

		item.populateScanLinks();
		Common.refreshUpdate(session, (item));

		if (Common.bolehKonfigurasi("setiap_kali_menyimpan_item_check_dengan_google")) {
			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						GoogleBookSynchronized.process(item);
					} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				}
			}).start();
		}

		return true;
	}

	public void checkForTerbit(Item item) {
		Perpustakaan perpustakaan = Common.getCurrentPerpustakaan();
		Session session = HibernateUtil.currentSession();

		if (perpustakaan != null && perpustakaan.getSatuanKerja() != null) {
			Integer jumlah = ((Number) session.createCriteria(ItemPunyaTerbit.class).add(Restrictions.eq("item", item))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			if (jumlah.equals(0)) {
				String content = (item.getAbstrak() != null && !item.getAbstrak().trim().equals("") ? item.getAbstrak()
						: item.getAbstrakEn() != null && !item.getAbstrakEn().trim().equals("") ? item.getAbstrakEn()
								: item.getCatatan());
				ItemPunyaTerbit itemPunyaTerbit = new ItemPunyaTerbit();
				itemPunyaTerbit.setContent(content);
				itemPunyaTerbit.setItem(item);
				itemPunyaTerbit.setMulai(ais.ui.util.WaktuUtil.getDate());
				itemPunyaTerbit.setPerpustakaan(perpustakaan);
				itemPunyaTerbit.setSatuanKerja(perpustakaan.getSatuanKerja());
				session.save(itemPunyaTerbit);
			}
		}
	}

	public Criteria initCriteria(boolean order) {
		return initCriteria(order, false);
	}

	protected Criteria initCriteria(boolean order, boolean asc) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Item.class);
		if (searchbarcode != null && !searchbarcode.getValue().trim().isEmpty() || tampilkan.isChecked()) {
			criteria = session.createCriteria(ItemPunyaBarcode.class)
					.add(searchbarcode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("barcode", searchbarcode.getValue().trim(), MatchMode.ANYWHERE))
					.createCriteria("item");
		}
		if (order)
			criteria.addOrder(asc ? Order.asc("id") : Order.desc("id"));

		String isbn = ItemAction.this.searchisbn.getValue().trim();
		isbn = org.apache.commons.lang3.StringUtils.replace(isbn, "-", "");

		criteria

				.add(bukanAmbilDariGoogle.isChecked() ? Restrictions.isNull("googleBookId")
						: Restrictions.sqlRestriction("true"))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

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
						: Restrictions.or(Restrictions.ilike("isbn", isbn.trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("isbn10", isbn.trim(), MatchMode.ANYWHERE)))

				.add(searchissn.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("issn", searchissn.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchedisi.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("edisi", searchedisi.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchpengarang.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("pengarangs", searchpengarang.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchcatatan.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("catatan", searchcatatan.getValue().trim(), MatchMode.ANYWHERE))
				.add((searchpenerbit == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchpenerbit.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("penerbit.nama", searchpenerbit.getValue().trim(), MatchMode.ANYWHERE)))
				.add(searchjenisItem.getSelectedItem() == null || searchjenisItem.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenisItem", searchjenisItem.getSelectedItem().getValue()))
				.add(searchtipeItem.getSelectedItem() == null || searchtipeItem.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tipeItem", searchtipeItem.getSelectedItem().getValue()))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false),
								Restrictions.ilike("by_statement",
										"," + ((Jurusan) searchjurusan.getSelectedItem().getValue()).getId() + ",",
										MatchMode.ANYWHERE)));

		return criteria;
	}

	public void onSearchDefault(Event event) {
		onSearchDefault(event, false);
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
		grid.setModelCheckMobile(strset);
		if (Common.bolehKonfigurasi("terintegrasi_dengan_google_book_baru", Konfigurasi.TIDAK_AKTIF)) {
			if (!dontLoop) {

				Common.createDefaultTimerNoBusy(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						String isbn = ItemAction.this.searchisbn.getValue().trim();
						isbn = org.apache.commons.lang3.StringUtils.replace(isbn, "-", "");
						String judul = ItemAction.this.searchnama.getValue().trim();
						String tahun = ItemAction.this.searchtahun.getValue() == null ? "_"
								: ItemAction.this.searchtahun.getValue() + "";
						String keyword = "_";
						String catatan = ItemAction.this.searchcatatan.getValue().trim();
						String pengarang = ItemAction.this.searchpengarang.getValue().trim();
						String penerbit = ItemAction.this.searchpenerbit.getValue().trim();
						String kategori = ItemAction.this.searchkategori.getValue().trim();

						LibraryUtil.cariDiGoogleBook(isbn, judul, keyword, catatan, pengarang, penerbit, kategori,
								tahun, ItemAction.this, paging, item, null);
					}
				}, "Mencoba mencari buku ke google book, harap menunggu, setelah beberapa saat, klik tombol cari lagi untuk me-load ulang buku pencarian",
						false, 2500);

			}
		}
	}

	public Boolean checkNamaItem() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(Item.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("isbn", isbn.getValue().trim()))
				.add(this.item.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.item.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
