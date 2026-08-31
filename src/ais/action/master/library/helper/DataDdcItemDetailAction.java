package ais.action.master.library.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
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
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.DataDdcItem;
import ais.database.model.library.DataDdcItemDetail;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaBarcode;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk data ddc item detail. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code DataDdcItem dataDdcItem}, {@code MyGrid
 * grid}, {@code boolean edit}, {@code boolean delete}, {@code Textbox barcode}, {@code String contents};
 * inisialisasi/lifecycle ({@code initCriteria()}); pembacaan/pencarian ({@code loadData()}, {@code
 * uploadDataItem()}, {@code loadBarcode()}); operasi domain lain ({@code display()}); konfigurasi constructor:
 * {@code delete}, {@code edit}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut
 * di atas.</p>
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
public class DataDdcItemDetailAction extends MyDetail implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private DataDdcItem dataDdcItem;
	private MyGrid grid;
	private boolean edit = false;
	private boolean delete = false;

	private Textbox barcode;

	public DataDdcItemDetailAction(DataDdcItem dataDdcItem) {
		super();
		this.dataDdcItem = dataDdcItem;
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(DataDdcItemDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	class DataDdcItemDetailRenderer extends ais.ui.util.MyRowRenderer {

		public DataDdcItemDetailRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final DataDdcItemDetail dataDdcItemDetail = (DataDdcItemDetail) data;

			new Label(dataDdcItemDetail.getItem() == null ? ""
					: dataDdcItemDetail.getItem().getIsbn() + " " + dataDdcItemDetail.getItem().getIssn())
							.setParent(row);

			RevisiHelper
					.createNewRevisi(DataDdcItemDetail.class, dataDdcItemDetail,
							dataDdcItemDetail.getItem() == null ? "" : dataDdcItemDetail.getItem().getNama())
					.setParent(row);

			new Label(dataDdcItemDetail.getItem().getPengarangs()).setParent(row);

			final MyTextbox keterangan = new MyTextbox(
					dataDdcItemDetail.getKeterangan() == null ? "" : dataDdcItemDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setParent(row);
			keterangan.setDisabled(!edit);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					dataDdcItemDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (dataDdcItemDetail));
				}
			});

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak ");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public void onEvent(Event event) throws Exception {
					Item item = dataDdcItemDetail.getItem();
					if (item.getDdcItem() != null) {
						final Map parameters = ais.common.HashMapGenerator.getRand();
						parameters.put("id", item.getId());
						parameters.put("perpustakaan", Common.getCurrentPerpustakaan() == null ? ""
								: Common.getCurrentPerpustakaan().getNama());
						Report.generatePDFReport(Report.PDF, new Map[] { parameters, parameters, parameters },
								new String[] { "library/ddc_per_item", "library/ddc_per_item_yg_ada_barcode_nya",
										"library/tampil_item_barcode" },
								new String[] { "Punggung Buku", "Punggung dan Barcode", "Barcode" }, ais.ui.util.WaktuUtil.getDate()
								);

					} else {
						final Map parameters = ais.common.HashMapGenerator.getRand();
						parameters.put("id", item.getId());
						parameters.put("perpustakaan", Common.getCurrentPerpustakaan() == null ? ""
								: Common.getCurrentPerpustakaan().getNama());

						Report.generatePDFReport(Report.PDF, new Map[] { parameters, parameters, parameters },
								new String[] { "library/ddc_per_item_manual", "library/ddc_per_item_barcode",
										"library/tampil_item_barcode" },
								new String[] { "Punggung Buku", "Punggung dan Barcode", "Barcode" }, ais.ui.util.WaktuUtil.getDate()
								);
					}

				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setDisabled(!delete);
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
									Session session = HibernateUtil.currentSession();

									Item item = dataDdcItemDetail.getItem();
									item.setDdcItem(null);
									session.update(item);

									Common.refreshDelete(session, dataDdcItemDetail);

									loadData(null);
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e); 
									MyMessageboxConfig
											.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
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

		List<DataDdcItemDetail> dataDdcItemDetails = initCriteria(true).list();

		ListModel strset = new SimpleListModel(dataDdcItemDetails);
		grid.setRowRenderer(new DataDdcItemDetailRenderer());
		grid.setModelCheckMobile(strset);

	}

	private String[] contents = new String[] { "id", "item" };

	public void display() {
		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("430px");
		panel.setTitle("Daftar Item DDC");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(panel);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Data Item", "/img/add_item.png");
		// button.setDisabled(add);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<Item> items = session.createCriteria(DataDdcItemDetail.class)
						.setProjection(Projections.groupProperty("item"))
						.add(Restrictions.eq("dataDdcItem", dataDdcItem)).list();

				AmbilDataItemBanyak ambilDataItemBanyak = new AmbilDataItemBanyak(items, true, false);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Item> items = (List<Item>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						for (Item item : items) {
							DataDdcItemDetail dataDdcItemDetail = new DataDdcItemDetail();
							dataDdcItemDetail.setItem(item);
							dataDdcItemDetail.setKeterangan("");
							dataDdcItemDetail.setDataDdcItem(dataDdcItem);
							session.save(dataDdcItemDetail);

							item.setDdcItem(dataDdcItem.getDdcItem());
							session.update(item);
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

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload Data"+Common.ukuranLabelFileUpload(), "/img/excel.png");
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
					MyMessageboxConfig
							.show("File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media, "Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});
		toolbar.appendChild(upload);

		new Space().setParent(toolbar);
		new Space().setParent(toolbar);
		new Space().setParent(toolbar);

		new Label("Barcode/ISBN/ISSN").setParent(toolbar);
		new Space().setParent(toolbar);
		barcode = new Textbox();
		barcode.setParent(toolbar);
		barcode.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadBarcode();
			}
		});

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		cari.setParent(toolbar);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadBarcode();
			}
		});

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode/ISBN/ISSN");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama/Judul");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pengarang");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		loadData(null);
	}

	public void uploadDataItem(final File file, final EventListener eventListener, final String[] contents)
			throws Exception {

		final Label peringatan = new Label("");
		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Item DDC");
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
						try { org.zkoss.zul.Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain"); } catch (Exception eD) { ais.common.ErrorAuditUtil.record(eD, "auto-audit(empty-catch) DataDdcItemDetailAction laporan-download"); }
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
							DataDdcItemDetail dataDdcItemDetail = id == null || id.equals(-1L) ? null
									: (DataDdcItemDetail) session.createCriteria(DataDdcItemDetail.class)
											.add(Restrictions.idEq(id)).uniqueResult();
							Item item = (Item) Common.getSheetContentAsObject(sheet, 1, i, Item.class);
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

							if (dataDdcItemDetail == null) {
								dataDdcItemDetail = (DataDdcItemDetail) session.createCriteria(DataDdcItemDetail.class)
										.add(Restrictions.eq("item", item))
										.add(Restrictions.eq("dataDdcItem", dataDdcItem)).addOrder(Order.desc("id"))
										.setMaxResults(1).uniqueResult();
							}

							if (dataDdcItemDetail == null) {
								dataDdcItemDetail = new DataDdcItemDetail();
							}

							dataDdcItemDetail.setDataDdcItem(dataDdcItem);
							dataDdcItemDetail.setItem(item);

							session.getTransaction().begin();
							session.saveOrUpdate(dataDdcItemDetail);
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
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/library/helper/DataDdcItemDetailAction.java:492");
				}

				HibernateUtil.closeSession();

				try {
					java.io.File rptFile = report.simpanLaporan();
					downloadPath.setValue(rptFile.getAbsolutePath());
				} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) DataDdcItemDetailAction laporan"); }
				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();
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
							Restrictions.or(Restrictions.ilike("isbn", barcode, MatchMode.EXACT),
									Restrictions.ilike("issn", barcode, MatchMode.EXACT))))
					.setMaxResults(1).uniqueResult();
		}

		if (item == null) {
			MyMessageboxConfig.show("Barcode " + barcode + " tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		DataDdcItemDetail dataDdcItemDetail = new DataDdcItemDetail();
		dataDdcItemDetail.setItem(item);
		dataDdcItemDetail.setKeterangan("");
		dataDdcItemDetail.setDataDdcItem(dataDdcItem);
		session.save(dataDdcItemDetail);

		item.setDdcItem(dataDdcItem.getDdcItem());
		session.update(item);

		loadData(null);
		this.barcode.focus();
		this.barcode.select();
	}

	@Override
	public Criteria initCriteria(boolean order) {
		// TODO Auto-generated method stub
		return HibernateUtil.currentSession().createCriteria(DataDdcItemDetail.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("dataDdcItem", dataDdcItem));
	}

}
