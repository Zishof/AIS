package ais.action.master.library.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.json.JSONObject;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.master.LogLoginAction;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.ItemAction;
import ais.action.master.library.util.LibraryUtil;
import ais.action.report.format1.library.LaporanBarcodeSaldoAwal;
import ais.action.report.format1.library.LaporanNoPunggungDanBarcodeSaldoAwal;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.SaldoAwalDetailDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DspaceInformation;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.library.BatchItemPunyaBarcode;
import ais.database.model.library.DetailTransaksi;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaBarcode;
import ais.database.model.library.JenisItem;
import ais.database.model.library.LabelItem;
import ais.database.model.library.Penerbit;
import ais.database.model.library.Perpustakaan;
import ais.database.model.library.SaldoAwal;
import ais.database.model.library.SaldoAwalDetail;
import ais.database.model.library.TipeItem;
import ais.ui.dspace.DspaceCommon;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIframe;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk saldo awal detail. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code SaldoAwal saldoAwal}, {@code MyGrid
 * grid}, {@code boolean edit}, {@code boolean delete}, {@code Paging paging}, {@code Textbox barcode}, {@code
 * String contents}, {@code Textbox cari}; inisialisasi/lifecycle ({@code initCriteria()}); pembacaan/pencarian
 * ({@code loadData()}, {@code getDspacePerpustakaan()}, {@code loadBarcode()}, {@code uploadDataItem()});
 * operasi domain lain ({@code display()}, {@code singkronkanBarcode()}, {@code generateBarcode()}, {@code
 * rollbackSaldoAwalBarcodeTransaction()}); konfigurasi constructor: {@code delete}, {@code edit}, {@code
 * paging}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see MyDetail
 */
public class SaldoAwalDetailAction extends MyDetail implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private SaldoAwal saldoAwal;
	private MyGrid grid;
	private boolean edit = false;
	private boolean delete = false;

	private Paging paging;

	private Textbox barcode;

	private String[] contents = new String[] { "id", "item", "jumlah" };

	private Textbox cari;

	public SaldoAwalDetailAction(SaldoAwal saldoAwal) {
		super();
		this.saldoAwal = saldoAwal;
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(SaldoAwalDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link SaldoAwalDetailAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link SaldoAwalDetailAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see SaldoAwalDetailAction
	 */
	class SaldoAwalDetailRenderer extends ais.ui.util.MyRowRenderer {

		public SaldoAwalDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final SaldoAwalDetail saldoAwalDetail = (SaldoAwalDetail) data;
			final Item item = saldoAwalDetail.getItem();

			LibraryUtil.checkRef(item);

			Image image = LibraryUtil.generateImage(saldoAwalDetail.getItem());
			image.setWidth("100%");
			image.setParent(row);

			final MyDoublebox jumlah = new MyDoublebox(
					saldoAwalDetail.getJumlah() == null ? 0.0 : saldoAwalDetail.getJumlah());

			RevisiHelper
					.createNewRevisi(Item.class, item, saldoAwalDetail.getSaldoAwal().getPerpustakaan().getId(),
							saldoAwalDetail.getItem() == null ? ""
									: saldoAwalDetail.getItem().getIsbn() + " " + saldoAwalDetail.getItem().getIssn())
					.setParent(row);

			RevisiHelper
					.createNewRevisi(SaldoAwalDetail.class, saldoAwalDetail,
							saldoAwalDetail.getSaldoAwal().getPerpustakaan().getId(),
							saldoAwalDetail.getItem() == null ? "" : saldoAwalDetail.getItem().getNama())
					.setParent(row);

			(jumlah).setParent(row);
			jumlah.setDisabled(saldoAwalDetail.getSaldoAwal().getDisetujuiOleh() != null
					|| (saldoAwalDetail.getDataPerItem() != null && saldoAwalDetail.getDataPerItem()) || !edit);
			jumlah.setStyle("text-align:right");
			jumlah.setWidth("90%");
			jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					Double saldo = Math.abs(jumlah.getValue() == null ? 0.0 : jumlah.getValue());
					jumlah.setValue(saldo);
					Session session = HibernateUtil.currentSession();
					saldoAwalDetail.setJumlah(saldo);
					Common.refreshUpdate(session, (saldoAwalDetail));
				}
			});

			final MyTextbox keterangan = new MyTextbox(
					saldoAwalDetail.getKeterangan() == null ? "" : saldoAwalDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(row);
			keterangan.setDisabled(saldoAwalDetail.getSaldoAwal().getDisetujuiOleh() != null || !edit);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					saldoAwalDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (saldoAwalDetail));
				}
			});

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("", "/img/print.png");
			cetak.setParent(toolbar);
			cetak.setVisible(item.getId() != null);
			cetak.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					LaporanNoPunggungDanBarcodeSaldoAwal laporanBarcodeItem = new LaporanNoPunggungDanBarcodeSaldoAwal(
							saldoAwal, item);
					laporanBarcodeItem.setTitle("Cetak Barcode");
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporanBarcodeItem);
					laporanBarcodeItem.setHeight("95%");
					laporanBarcodeItem.setWidth("90%");
					laporanBarcodeItem.setClosable(true);
					laporanBarcodeItem.onModal();
				}
			});

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setDisabled(saldoAwalDetail.getSaldoAwal().getDisetujuiOleh() != null || !delete);
			button.setTooltiptext("Hapus Data");
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
											Common.refreshDelete(HibernateUtil.currentSession(), saldoAwalDetail);

											loadData(null);

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
			toolbar.setParent(row);

		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Common.initPaging(initCriteria(false), paging);
		List<SaldoAwalDetail> saldoAwalDetails = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(saldoAwalDetails);
		grid.setRowRenderer(new SaldoAwalDetailRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display() {
		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("760px");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(panel);

		new Label(ais.common.Common.getBahasaConfig("Cari")).setParent(toolbar);
		cari = new Textbox();
		cari.setParent(toolbar);
		cari.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		MyToolbarbuttonConfig pencari = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		pencari.setParent(toolbar);
		pencari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Data Item", "/img/add_item.png");
		button.setDisabled(saldoAwal.getDisetujuiOleh() != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<Item> items = session.createCriteria(SaldoAwalDetail.class)
						.setProjection(Projections.groupProperty("item")).add(Restrictions.eq("saldoAwal", saldoAwal))
						.list();

				AmbilDataItemBanyak ambilDataItemBanyak = new AmbilDataItemBanyak(items);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Item> items = (List<Item>) arg0.getData();
						SaldoAwalDetailDao saldoAwalDetailDao = DaoFactory.getInstance().getSaldoAwalDetailDao();
						for (Item item : items) {
							SaldoAwalDetail saldoAwalDetail = new SaldoAwalDetail();
							saldoAwalDetail.setItem(item);
							saldoAwalDetail.setJumlah(0.0);
							saldoAwalDetail.setKeterangan("");
							saldoAwalDetail.setSaldoAwal(saldoAwal);
							saldoAwalDetailDao.save(saldoAwalDetail);
						}

						loadData(null);
					}
				});
				ambilDataItemBanyak.setWidth("97%");
				ambilDataItemBanyak.setHeight("97%");
				ambilDataItemBanyak.setVisible(true);
				ambilDataItemBanyak.onModal();
			}

		});
		button.setParent(toolbar);

		new Space().setParent(toolbar);
		new Space().setParent(toolbar);
		new Space().setParent(toolbar);

		new Label("Tambah ISBN/ISSN").setParent(toolbar);
		new Space().setParent(toolbar);
		barcode = new Textbox();
		barcode.setDisabled(saldoAwal.getDisetujuiOleh() != null);
		barcode.setParent(toolbar);
		barcode.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadBarcode();
			}
		});

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Cari ISBN/ISSN", "/img/svg/search.svg");
		cari.setParent(toolbar);
		cari.setDisabled(saldoAwal.getDisetujuiOleh() != null);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadBarcode();
			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload Data" + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		upload.setUpload(Common.ukuranFileUpload());
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
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
									loadData(null);
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
		toolbar.appendChild(upload);

		MyToolbarbuttonConfig barcode = new MyToolbarbuttonConfig("Singkronkan", "/img/Ecommerce-Barcode-icon.png");
		barcode.setParent(toolbar);
		barcode.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				singkronkanBarcode(saldoAwal);

			}
		});

		barcode = new MyToolbarbuttonConfig("Barcode", "/img/album.png");
		barcode.setParent(toolbar);
		barcode.setDisabled(saldoAwal.getDisetujuiOleh() == null);
		barcode.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				generateBarcode(saldoAwal, false);

			}
		});

		barcode = new MyToolbarbuttonConfig("Punggung", "/img/album.png");
		barcode.setParent(toolbar);
		barcode.setDisabled(saldoAwal.getDisetujuiOleh() == null);
		barcode.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				generateBarcode(saldoAwal, true);

			}
		});

		final String[] contentsTambahan = new String[] { "id-item", "isbn", "isbn10", "issn", "nama", "tema", "bahasa",
				"edisi", "penaklikan", "catatan", "jenisItem", "tipeItem", "labelItem", "penerbit", "tahun", "halaman",
				"pengarangs", "kategories", "deweyDecimalClass", "fakultas", "jurusan", "tempatterbit", "abstrak",
				"abstrakEn", "kewords", "kewordsEn", "imageUrl", "imagePath", "lampiranPath", "kode" };

		final String[] contentsBarcode = new String[] { "id", "barcode", "item", "tipeItem" };

		List<String> columnHeadersAdding = new ArrayList<String>();
		for (String kode : contentsTambahan) {
			columnHeadersAdding.add(kode);
		}

		final ClassMetadata classMetadata = HibernateUtil.getClassMetadata(Item.class);

		EventListener dataAdding = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (arg0 == null || !(arg0.getData() instanceof Object[])) {
					return;
				}
				Object[] objects = (Object[]) arg0.getData();
				if (objects.length < 3 || !(objects[0] instanceof ItemPunyaBarcode)
						|| !(objects[2] instanceof XSSFRow)) {
					return;
				}
				ItemPunyaBarcode itemPunyaBarcode = (ItemPunyaBarcode) objects[0];
				Item item = itemPunyaBarcode.getItem();
				if (item == null) {
					return;
				}
				XSSFRow row = (XSSFRow) objects[2];
				// XSSFWorkbook workbook = (XSSFWorkbook) objects[3];

				row.createCell(contentsBarcode.length).setCellValue(item.getId());
				for (int i = 1; i < contentsTambahan.length; i++) {
					Object d = null;
					try {
						if (classMetadata == null) {
							continue;
						}
						d = classMetadata.getPropertyValue(item, contentsTambahan[i], EntityMode.POJO);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/helper/SaldoAwalDetailAction.java:500");

					}
					row.createCell(contentsBarcode.length + i).setCellValue(d == null ? "" : d.toString());
				}

			}
		};

		MyToolbarbuttonConfig cetakToolbarbutton1 = Common.cetakDataCustomButton(ItemPunyaBarcode.class,
				new DataCriteria() {

					@Override
					public Criteria initCriteria(boolean order) {
						return HibernateUtil.currentSession().createCriteria(ItemPunyaBarcode.class)
								.createAlias("batchItemPunyaBarcode", "batchItemPunyaBarcode")
								.add(Restrictions.eq("batchItemPunyaBarcode.saldoAwal", saldoAwal))
								.addOrder(Order.asc("barcode"));
					}
				}, "Download Barcode", "/img/print.png", columnHeadersAdding, dataAdding, false, null, "",
				contentsBarcode);
		toolbar.appendChild(cetakToolbarbutton1);

		MyToolbarbuttonConfig uploadBarcode = new MyToolbarbuttonConfig("Upload Barcode", "/img/excel.png");
		uploadBarcode.setUpload(Common.ukuranFileUpload());
		uploadBarcode.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
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
							final Tbmuser dibuatOleh = Common.getCurrentUser();
							final Label peringatan = new Label("");

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
										MyMessageboxConfig.show("Upload data berhasil dilakukan."
												+ (peringatan.getValue().isEmpty() ? "" : "\n" + peringatan.getValue()),
												"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
												new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														loadData(null);
													}
												});
										Clients.clearBusy();
										timer.detach();
									}

								}
							});
							timer.start();

							new Thread(new Runnable() {

								@SuppressWarnings("rawtypes")
								@Override
								public void run() {
									try {

									try {

										XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
										XSSFSheet sheet = workbook.getSheetAt(0);

										Session session = HibernateUtil.currentNativeSession();
										Long tempId = -1L;
										int rowCount = (sheet.getLastRowNum() + 1);
										int indexke = 0;
										for (int i = 1; i < rowCount; i++) {
											try {

												Item item = (Item) Common.getSheetContentAsObject(sheet,
														contentsBarcode.length, i, Item.class);

												if (item == null) {
													item = (Item) Common.getSheetContentAsObject(sheet, 2, i,
															Item.class);
												}

												if (item == null) {
													String isbn = Common.getSheetContentAsString(sheet, 2, i);
													if (isbn != null && !isbn.trim().isEmpty()) {
														item = (Item) session.createCriteria(Item.class)
																.add(Restrictions.or(
																		Restrictions.and(Restrictions.ne("kode", ""),
																				Restrictions.and(
																						Restrictions.isNotNull("kode"),
																						Restrictions.eq("kode",
																								isbn.trim()))),
																		Restrictions.or(
																				Restrictions.eq("isbn", isbn.trim()),
																				Restrictions.eq("isbn10",
																						isbn.trim()))))
																.addOrder(Order.desc("id")).setMaxResults(1)
																.uniqueResult();
													}
												}

												if (item == null) {
													String judul = Common.getSheetContentAsString(sheet,
															contentsBarcode.length + 4, i);
													System.out.println("judul => " + judul);
													if (judul != null && !judul.trim().isEmpty()) {
														item = (Item) session.createCriteria(Item.class)
																.add(Restrictions.ilike("nama", judul.trim(),
																		MatchMode.EXACT))
																.addOrder(Order.desc("id")).setMaxResults(1)
																.uniqueResult();
													}
												}

												if (item == null) {
													item = new Item();
												}

												Map datum = Common.setObjectValues(classMetadata, item,
														contentsTambahan, 1, sheet, i);
												if (item.getPenerbit() == null && datum.get("penerbit") != null
														&& !datum.get("penerbit").toString().trim().isEmpty()) {
													Penerbit penerbit = (Penerbit) session
															.createCriteria(Penerbit.class)
															.add(Restrictions.ilike("nama",
																	datum.get("penerbit").toString().trim()))
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

												if (LibraryUtil.TEXTBOOK != null
														&& LibraryUtil.TEXTBOOK.getNama() != null
														&& item.getTipeItem().getId()
																.equals(LibraryUtil.TEXTBOOK.getId())
														&& datum.get("tipeItem") != null
														&& !datum.get("tipeItem").toString().trim().isEmpty()
														&& !datum.get("tipeItem").toString().trim().equalsIgnoreCase(
																LibraryUtil.TEXTBOOK.getNama().trim())) {
													TipeItem tipeItem = (TipeItem) session
															.createCriteria(TipeItem.class)
															.add(Restrictions.ilike("nama",
																	datum.get("tipeItem").toString().trim()))
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
														&& !datum.get("jenisItem").toString().trim().isEmpty()
														&& !datum.get("jenisItem").toString().trim()
																.equalsIgnoreCase(LibraryUtil.TEXT.getNama().trim())) {
													JenisItem jenisItem = (JenisItem) session
															.createCriteria(JenisItem.class)
															.add(Restrictions.ilike("nama",
																	datum.get("jenisItem").toString().trim()))
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

												if ((item.getLabelItem() == null || (datum.get("labelItem") != null
														&& !item.getLabelItem().getNama().equalsIgnoreCase(
																datum.get("labelItem").toString().trim())))
														&& datum.get("labelItem") != null
														&& !datum.get("labelItem").toString().trim().isEmpty()) {
													LabelItem labelItem = (LabelItem) session
															.createCriteria(LabelItem.class)
															.add(Restrictions.ilike("nama",
																	datum.get("labelItem").toString().trim()))
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

												if (item.getNama() == null || item.getNama().trim().isEmpty()) {
													continue;
												}

												session.getTransaction().begin();
												session.saveOrUpdate(item);
												session.getTransaction().commit();

												String barcode = Common.getSheetContentAsString(sheet, 1, i);
												if (item != null && barcode != null && !barcode.trim().isEmpty()) {
													if (!tempId.equals(item.getId())) {
														tempId = item.getId();
														indexke = 0;
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
														batchItemPunyaBarcode.setDibuatOleh(dibuatOleh);
														batchItemPunyaBarcode.setItem(item);
														batchItemPunyaBarcode.setSaldoAwal(saldoAwal);
														batchItemPunyaBarcode
																.setTanggal(saldoAwal.getTanggalPersetujuan());

														session.getTransaction().begin();
														Common.refreshSaveOrUpdate(session, batchItemPunyaBarcode);
														session.getTransaction().commit();
													}

													Long id = Common.getSheetContentAsLong(sheet, 0, i);
													TipeItem tipeItem = (TipeItem) Common.getSheetContentAsObject(sheet,
															3, i, TipeItem.class);
													ItemPunyaBarcode itemPunyaBarcode = id == null || id.equals(-1L)
															? null
															: (ItemPunyaBarcode) session
																	.createCriteria(ItemPunyaBarcode.class)
																	.add(Restrictions.idEq(id)).uniqueResult();

													if (itemPunyaBarcode == null) {
														itemPunyaBarcode = (ItemPunyaBarcode) session
																.createCriteria(ItemPunyaBarcode.class).setMaxResults(1)
																.add(Restrictions.eq("barcode", barcode.trim()))
																.uniqueResult();
													}

													if (itemPunyaBarcode == null) {
														itemPunyaBarcode = new ItemPunyaBarcode();
													}

													itemPunyaBarcode.setBarcode(barcode.trim());
													itemPunyaBarcode.setBatchItemPunyaBarcode(batchItemPunyaBarcode);
													itemPunyaBarcode.setIndexke(indexke);
													itemPunyaBarcode.setItem(item);
													itemPunyaBarcode.setPerpustakaan(saldoAwal.getPerpustakaan());
													itemPunyaBarcode.setTipeItem(tipeItem);

													session.getTransaction().begin();
													session.saveOrUpdate(itemPunyaBarcode);
													session.getTransaction().commit();
													indexke++;

													label.setValue("Upload data \"" + itemPunyaBarcode.getBarcode()
															+ " - " + itemPunyaBarcode.getItem() + "\" ("
															+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
												}
											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
											}

										}

										label.setValue("Sedang memproses stok ...");
										session.createSQLQuery(
												"delete from library.detail_transaksi where saldo_awal_detail in (select id from library.saldo_awal_detail where saldo_awal="
														+ saldoAwal.getId() + ");")
												.executeUpdate();
										session.createSQLQuery("delete from library.saldo_awal_detail where saldo_awal="
												+ saldoAwal.getId()).executeUpdate();

										@SuppressWarnings("unchecked")
										List<Object[]> datas = session
												.createSQLQuery("SELECT batch_item_punya_barcode, count(*) jumlah "
														+ "FROM library.item_punya_barcode a  "
														+ "inner join library.batch_item_punya_barcode b on (a.batch_item_punya_barcode=b.id) "
														+ "where b.saldo_awal=" + saldoAwal.getId() + " "
														+ "group by batch_item_punya_barcode ; ")
												.list();
										for (Object[] o : datas) {
											BatchItemPunyaBarcode batchItemPunyaBarcode = (BatchItemPunyaBarcode) session
													.createCriteria(BatchItemPunyaBarcode.class)
													.add(Restrictions.idEq(Long.parseLong(o[0].toString())))
													.uniqueResult();
											SaldoAwalDetail saldoAwalDetail = new SaldoAwalDetail();
											saldoAwalDetail.setBatchItemPunyaBarcode(batchItemPunyaBarcode);
											saldoAwalDetail.setDataPerItem(true);
											saldoAwalDetail.setItem(batchItemPunyaBarcode.getItem());
											saldoAwalDetail.setJumlah(Double.parseDouble(o[1].toString()));
											saldoAwalDetail.setSaldoAwal(saldoAwal);

											session.getTransaction().begin();
											session.saveOrUpdate(saldoAwalDetail);
											session.getTransaction().commit();

											DetailTransaksi detailTransaksi = new DetailTransaksi();
											detailTransaksi.setSaldoAwalDetail(saldoAwalDetail);
											detailTransaksi.setQtyBonus(0.0);

											detailTransaksi.setItem(saldoAwalDetail.getItem());
											detailTransaksi.setKeterangan("Transaksi Saldo Awal");
											detailTransaksi.setKodeTransaksi(LibraryUtil.SALDO_AWAL);
											detailTransaksi.setPerpustakaan(saldoAwal.getPerpustakaan());
											detailTransaksi.setQty(saldoAwalDetail.getJumlah());
											detailTransaksi.setTanggal(saldoAwal.getTanggalPembuatan());

											session.getTransaction().begin();
											session.saveOrUpdate(detailTransaksi);
											session.getTransaction().commit();
										}

									} catch (Exception e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/library/helper/SaldoAwalDetailAction.java:855");
									}

									HibernateUtil.closeSession();

									label.setValue("");
																	} finally {
										ais.database.hibernate.HibernateUtil.closeSession();
									}
								}
							}).start();

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
		toolbar.appendChild(uploadBarcode);

		MyToolbarbuttonConfig exportKeOjs = new MyToolbarbuttonConfig("Ekspor", "/img/corner.gif");
		toolbar.appendChild(exportKeOjs);
		exportKeOjs.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("saldo_awal_pustaka_terhubung_ke_dspace"));
		exportKeOjs.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						loadData(null);
						LogLoginAction.tampilDpsaceLog();
					}
				});

				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							String cookie = DspaceCommon.login();
							@SuppressWarnings("unchecked")
							List<SaldoAwalDetail> saldoAwalDetails = initCriteria(true).list();

							int rowIndex = 1;
							for (SaldoAwalDetail saldoAwalDetail : saldoAwalDetails) {
								label.setValue("Sedang memproses data " + saldoAwalDetail.toString() + " ("
										+ Common.numberFormat.get().format((rowIndex++) * 100.0 / saldoAwalDetails.size())
										+ " %)");

								DspaceInformation f = getDspacePerpustakaan(cookie, saldoAwal.getPerpustakaan());

								DspaceInformation s = ItemAction.getDspaceItem(cookie, saldoAwalDetail.getItem(),
										saldoAwal.getPerpustakaan(), f);

								ItemAction.getDspace(cookie, saldoAwalDetail.getItem(), saldoAwal.getPerpustakaan(),
										true, s);
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
		toolbar.appendChild(batalExport);
		batalExport.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("saldo_awal_pustaka_terhubung_ke_dspace"));
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
											loadData(null);
											LogLoginAction.tampilDpsaceLog();
										}
									});

									new Thread(new Runnable() {

										@SuppressWarnings("unchecked")
										@Override
										public void run() {
											try {
											try {
												String cookie = DspaceCommon.login();
												List<SaldoAwalDetail> saldoAwalDetails = initCriteria(true).list();

												int rowIndex = 1;
												for (SaldoAwalDetail saldoAwalDetail : saldoAwalDetails) {
													label.setValue("Sedang memproses data " + saldoAwalDetail.toString()
															+ " ("
															+ Common.numberFormat.get().format(
																	(rowIndex++) * 100.0 / saldoAwalDetails.size())
															+ " %)");
													DspaceInformation dspaceInformation = DspaceInformation
															.getDspaceInformation(Item.class.getName(),
																	-saldoAwalDetail.getItem().getId());
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

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(center);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tabJawaban = new MyTabConfig("Item");
		tabJawaban.setParent(tabs);

		final MyTabConfig tabSoal = new MyTabConfig("Barcode");
		tabSoal.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
		tabpanelUtama.setParent(tabpanels);

		borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanelUtama);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode/ISBN/ISSN");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama/Judul");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jumlah");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("15%");

		loadData(null);

		final Tabpanel tabpanelKedua = new ais.ui.util.MyTabpanel();
		tabpanelKedua.setParent(tabpanels);

		tabSoal.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelKedua.getChildren().isEmpty()) {
					Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
					borderlayout.setParent(tabpanelKedua);

					Center center = new Center();
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);

					MyIframe include = new MyIframe("/pages/master/library/barcode_item.zul?saldoAwal="
							+ (saldoAwal == null || saldoAwal.getId() == null ? -1L : saldoAwal.getId()));
					include.setParent(center);
				}
			}
		});

		South mySouth = new South();
		mySouth.setParent(borderlayout);

		paging.setParent(mySouth);

	}

	public static DspaceInformation getDspacePerpustakaan(String cookie, Perpustakaan perpustakaan) throws Exception {

		String description = "Repositori " + perpustakaan.getNama();

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", "Koleksi " + perpustakaan.getNama());
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", description);
		jsonPost.put("shortDescription", perpustakaan.getNama() + " Repository");
		jsonPost.put("sidebarText", description);

		return DspaceInformation.dspaceProcess(cookie, perpustakaan, jsonPost.toString(), false, "communities",
				"communities");
	}

	protected void singkronkanBarcode(final SaldoAwal saldoAwal) {
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Barcode Saldo Awal");

				Tbmuser dibuatOleh = Common.getCurrentUser();
				Session session = HibernateUtil.currentNativeSession();

				session.createSQLQuery(
						"delete from library.detail_transaksi where saldo_awal_detail in (select id from library.saldo_awal_detail where saldo_awal = "
								+ saldoAwal.getId() + " and (data_per_item is null or data_per_item = false));")
						.executeUpdate();

				@SuppressWarnings("unchecked")
				List<SaldoAwalDetail> saldoAwalDetails = session.createCriteria(SaldoAwalDetail.class)
						.addOrder(Order.desc("id")).add(Restrictions.eq("saldoAwal", saldoAwal)).list();
				int baris = 1;
				for (SaldoAwalDetail saldoAwalDetail : saldoAwalDetails) {
					String kunci = String.valueOf(saldoAwalDetail.getItem());
					try {
					System.out.println("saldoAwalDetail => " + saldoAwalDetail);
					BatchItemPunyaBarcode batchItemPunyaBarcode = (BatchItemPunyaBarcode) session
							.createCriteria(BatchItemPunyaBarcode.class).add(Restrictions.eq("saldoAwal", saldoAwal))
							.add(Restrictions.eq("item", saldoAwalDetail.getItem())).setMaxResults(1).uniqueResult();
					if (batchItemPunyaBarcode == null) {
						batchItemPunyaBarcode = new BatchItemPunyaBarcode();
						batchItemPunyaBarcode.setBerasalDari(BatchItemPunyaBarcode.SALDO_AWAL);
						batchItemPunyaBarcode.setDibuatOleh(dibuatOleh);
						batchItemPunyaBarcode.setItem(saldoAwalDetail.getItem());
						batchItemPunyaBarcode.setSaldoAwal(saldoAwal);
						batchItemPunyaBarcode.setTanggal(saldoAwal.getTanggalPersetujuan());

						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, batchItemPunyaBarcode);
						session.getTransaction().commit();
					}

					saldoAwalDetail.setBatchItemPunyaBarcode(batchItemPunyaBarcode);
					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, saldoAwalDetail);
					session.getTransaction().commit();

					Item item = saldoAwalDetail.getItem();

					int jumlahItem = saldoAwalDetail.getJumlah().intValue();
					if (jumlahItem < 100) {
						for (int i = 0; i < jumlahItem; i++) {
							ItemPunyaBarcode itemPunyaBarcode = (ItemPunyaBarcode) (session
									.createCriteria(ItemPunyaBarcode.class)
									.add(Restrictions.eq("batchItemPunyaBarcode", batchItemPunyaBarcode))
									.add(Restrictions.eq("item", item)).add(Restrictions.eq("indexke", i + 1))
									.setMaxResults(1).uniqueResult());
							if (itemPunyaBarcode == null) {
								itemPunyaBarcode = new ItemPunyaBarcode();
								itemPunyaBarcode.setIndexke(i + 1);
								itemPunyaBarcode.setItem(item);
								itemPunyaBarcode.setBatchItemPunyaBarcode(batchItemPunyaBarcode);
								itemPunyaBarcode.setBarcode(BarcodeCommon.generateCode(batchItemPunyaBarcode));
								itemPunyaBarcode.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
								itemPunyaBarcode.setPerpustakaan(saldoAwal.getPerpustakaan());

								session.getTransaction().begin();
								Common.refreshSaveOrUpdate(session, itemPunyaBarcode);
								session.getTransaction().commit();
							} else if (itemPunyaBarcode.getIndexke() == null) {
								itemPunyaBarcode.setIndexke(i + 1);
								session.getTransaction().begin();
								Common.refreshUpdate(session, itemPunyaBarcode);
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
							detailTransaksi.setTanggalDanWaktu(saldoAwal.getTanggalPembuatan());

							session.getTransaction().begin();
							Common.refreshSaveOrUpdate(session, detailTransaksi);
							session.getTransaction().commit();
						}

					}
					laporan.catatBerhasil(baris - 1, kunci, "Sinkronisasi barcode berhasil");
					} catch (Exception ePerItem) {
						Common.tampilErrorJikaAdmin(ePerItem);
						laporan.catatGagalDetail(baris - 1, kunci, ePerItem);
					}
					baris++;
				}
				HibernateUtil.closeSession();

				laporan.selesaikan(null);

			}
		}, "Harap tunggu, sedang melakukan singkronisasi barcode ...", false, 500);
	}

	protected void generateBarcode(final SaldoAwal saldoAwal, final boolean punggng) {
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (Common.bolehKonfigurasi("generate_barcode_saldo_awal_otomatis")) {
					Tbmuser dibuatOleh = Common.getCurrentUser();
					Session session = null;
					try {
						session = HibernateUtil.openSession();

						int size = ((Number) session.createCriteria(SaldoAwalDetail.class)
								.setProjection(Projections.rowCount()).add(Restrictions.eq("saldoAwal", saldoAwal))
								.uniqueResult()).intValue();
						if (size < 30) {
							session.createSQLQuery(
									"delete from library.detail_transaksi where saldo_awal_detail in (select id from library.saldo_awal_detail where saldo_awal = "
											+ saldoAwal.getId() + " and (data_per_item is null or data_per_item = false));")
									.executeUpdate();

							@SuppressWarnings("unchecked")
							List<SaldoAwalDetail> saldoAwalDetails = session.createCriteria(SaldoAwalDetail.class)
									.addOrder(Order.desc("id")).add(Restrictions.eq("saldoAwal", saldoAwal)).list();
							for (SaldoAwalDetail saldoAwalDetail : saldoAwalDetails) {
								System.out.println("saldoAwalDetail => " + saldoAwalDetail);
								BatchItemPunyaBarcode batchItemPunyaBarcode = (BatchItemPunyaBarcode) session
										.createCriteria(BatchItemPunyaBarcode.class)
										.add(Restrictions.eq("saldoAwal", saldoAwal))
										.add(Restrictions.eq("item", saldoAwalDetail.getItem())).setMaxResults(1)
										.uniqueResult();
								if (batchItemPunyaBarcode == null) {
									batchItemPunyaBarcode = new BatchItemPunyaBarcode();
									batchItemPunyaBarcode.setBerasalDari(BatchItemPunyaBarcode.SALDO_AWAL);
									batchItemPunyaBarcode.setDibuatOleh(dibuatOleh);
									batchItemPunyaBarcode.setItem(saldoAwalDetail.getItem());
									batchItemPunyaBarcode.setSaldoAwal(saldoAwal);
									batchItemPunyaBarcode.setTanggal(saldoAwal.getTanggalPersetujuan());

									session.beginTransaction();
									Common.refreshSaveOrUpdate(session, batchItemPunyaBarcode);
									session.getTransaction().commit();
								}

								saldoAwalDetail.setBatchItemPunyaBarcode(batchItemPunyaBarcode);
								session.beginTransaction();
								Common.refreshSaveOrUpdate(session, saldoAwalDetail);
								session.getTransaction().commit();

								Item item = saldoAwalDetail.getItem();

								int jumlahItem = saldoAwalDetail.getJumlah().intValue();

								for (int i = 0; i < jumlahItem; i++) {
									ItemPunyaBarcode itemPunyaBarcode = (ItemPunyaBarcode) (session
											.createCriteria(ItemPunyaBarcode.class)
											.add(Restrictions.eq("batchItemPunyaBarcode", batchItemPunyaBarcode))
											.add(Restrictions.eq("item", item)).add(Restrictions.eq("indexke", i + 1))
											.setMaxResults(1).uniqueResult());
									if (itemPunyaBarcode == null) {
										itemPunyaBarcode = new ItemPunyaBarcode();
										itemPunyaBarcode.setIndexke(i + 1);
										itemPunyaBarcode.setItem(item);
										itemPunyaBarcode.setBatchItemPunyaBarcode(batchItemPunyaBarcode);
										itemPunyaBarcode.setBarcode(BarcodeCommon.generateCode(batchItemPunyaBarcode));
										itemPunyaBarcode.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
										itemPunyaBarcode.setPerpustakaan(saldoAwal.getPerpustakaan());

										session.beginTransaction();
										Common.refreshSaveOrUpdate(session, itemPunyaBarcode);
										session.getTransaction().commit();
									} else if (itemPunyaBarcode.getIndexke() == null) {
										itemPunyaBarcode.setIndexke(i + 1);
										session.beginTransaction();
										Common.refreshUpdate(session, itemPunyaBarcode);
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
									detailTransaksi.setTanggalDanWaktu(saldoAwal.getTanggalPembuatan());

									session.beginTransaction();
									Common.refreshSaveOrUpdate(session, detailTransaksi);
									session.getTransaction().commit();
								}

							}
						}
					} finally {
						rollbackSaldoAwalBarcodeTransaction(session);
						HibernateUtil.closeSessionQuietly(session);
					}

				}

				if (punggng) {
					LaporanNoPunggungDanBarcodeSaldoAwal laporanBarcodeItem = new LaporanNoPunggungDanBarcodeSaldoAwal(
							saldoAwal);
					laporanBarcodeItem.setTitle("Cetak Barcode");
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporanBarcodeItem);
					laporanBarcodeItem.setHeight("95%");
					laporanBarcodeItem.setWidth("90%");
					laporanBarcodeItem.setClosable(true);
					laporanBarcodeItem.onModal();
				} else {
					LaporanBarcodeSaldoAwal laporanBarcodeItem = new LaporanBarcodeSaldoAwal(saldoAwal);
					laporanBarcodeItem.setTitle("Cetak Barcode");
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporanBarcodeItem);
					laporanBarcodeItem.setHeight("95%");
					laporanBarcodeItem.setWidth("90%");
					laporanBarcodeItem.setClosable(true);
					laporanBarcodeItem.onModal();
				}
			}
		});

	}

	public void loadBarcode() throws Exception {
		String barcode = this.barcode.getText().trim();
		if (barcode.trim().equals("")) {
			MyMessageboxConfig.show("Barcode/ISBN/ISSN harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Session session = HibernateUtil.currentSession();
		ItemPunyaBarcode itemPunyaBarcode = barcode == null || barcode.trim().equals("")
				|| barcode.trim().equals("null")
						? null
						: (ItemPunyaBarcode) session.createCriteria(ItemPunyaBarcode.class)
								.add(Restrictions.ilike("barcode", barcode, MatchMode.EXACT)).setMaxResults(1)
								.uniqueResult();

		Item item = null;
		if (itemPunyaBarcode != null) {
			item = itemPunyaBarcode.getItem();
		} else {
			item = (Item) session.createCriteria(Item.class)
					.add(Restrictions.or(Restrictions.ilike("isbn10", barcode, MatchMode.EXACT),
							Restrictions.or(Restrictions.ilike("isbn10", barcode, MatchMode.EXACT),
									Restrictions.or(Restrictions.ilike("isbn", barcode, MatchMode.EXACT),
											Restrictions.ilike("issn", barcode, MatchMode.EXACT)))))
					.setMaxResults(1).uniqueResult();
		}

		if (item == null) {
			MyMessageboxConfig.show("Barcode " + barcode + " tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		SaldoAwalDetail saldoAwalDetail = new SaldoAwalDetail();
		saldoAwalDetail.setItem(item);
		saldoAwalDetail.setJumlah(1.0);
		saldoAwalDetail.setKeterangan("");
		saldoAwalDetail.setSaldoAwal(saldoAwal);
		session.save(saldoAwalDetail);
		loadData(null);
		this.barcode.focus();
		this.barcode.select();
	}

	public void uploadDataItem(final File file, final EventListener eventListener, final String[] contents)
			throws Exception {

		final Label peringatan = new Label("");
		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Item Saldo Awal");
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
						try { org.zkoss.zul.Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain"); } catch (Exception eD) { ais.common.ErrorAuditUtil.record(eD, "auto-audit(empty-catch) SaldoAwalDetailAction laporan-download"); }
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

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				try {

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);

					Session session = HibernateUtil.currentNativeSession();

					int rowCount = (sheet.getLastRowNum() + 1);
					for (int i = 1; i < rowCount; i++) {
						try {

							Long id = Common.getSheetContentAsLong(sheet, 0, i);
							SaldoAwalDetail saldoAwalDetail = id == null || id.equals(-1L) ? null
									: (SaldoAwalDetail) session.createCriteria(SaldoAwalDetail.class)
											.add(Restrictions.idEq(id)).uniqueResult();
							Item item = (Item) Common.getSheetContentAsObject(sheet, 1, i, Item.class);
							Double jumlah = Common.getSheetContentAsDouble(sheet, 2, i);
							if (item == null) {
								String isbn = Common.getSheetContentAsString(sheet, 1, i);
								if (isbn != null && !isbn.trim().isEmpty()) {
									item = (Item) session.createCriteria(Item.class)
											.add(Restrictions.or(Restrictions.eq("isbn", isbn.trim()),
													Restrictions.eq("isbn10", isbn.trim())))
											.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
								}
							}
							if (item == null) {
								report.gagal(i, "Baris " + i, "Item tidak ditemukan (ISBN/kode tidak cocok).", "Pastikan kode item/ISBN valid dan terdaftar di perpustakaan.");
								continue;
							}

							if (saldoAwalDetail == null) {
								saldoAwalDetail = (SaldoAwalDetail) session.createCriteria(SaldoAwalDetail.class)
										.add(Restrictions.eq("item", item)).add(Restrictions.eq("saldoAwal", saldoAwal))
										.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
							}

							if (saldoAwalDetail == null) {
								saldoAwalDetail = new SaldoAwalDetail();
							}

							saldoAwalDetail.setJumlah(jumlah);
							saldoAwalDetail.setSaldoAwal(saldoAwal);
							saldoAwalDetail.setItem(item);

							session.getTransaction().begin();
							session.saveOrUpdate(saldoAwalDetail);
							session.getTransaction().commit();

							label.setValue("Upload data \"" + item.toString() + "\" ("
									+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
							report.sukses(i, item.toString(), "");

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
							report.gagal(i, "Baris " + i, e, "Pastikan kode item/ISBN valid dan terdaftar di perpustakaan.");
						}

					}
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/library/helper/SaldoAwalDetailAction.java:1497");
				}

				HibernateUtil.closeSession();

				try {
					java.io.File rptFile = report.simpanLaporan();
					downloadPath.setValue(rptFile.getAbsolutePath());
				} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) SaldoAwalDetailAction laporan"); }
				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();
	}

	private void rollbackSaldoAwalBarcodeTransaction(Session session) {
		try {
			if (session != null && session.isOpen() && session.getTransaction() != null
					&& session.getTransaction().isActive()
					&& !session.getTransaction().wasCommitted() && !session.getTransaction().wasRolledBack()) {
				session.getTransaction().rollback();
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@Override
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(SaldoAwalDetail.class).createAlias("item", "item")

				.add(cari == null || cari.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("item.isbn10", cari.getValue().trim()),
								Restrictions.or(Restrictions.ilike("item.isbn", cari.getValue().trim()),
										Restrictions.ilike("item.nama", cari.getValue().trim(), MatchMode.ANYWHERE))))

				.add(Restrictions.eq("saldoAwal", saldoAwal));
		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

}
