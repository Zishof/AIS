package ais.action.master.library;


import ais.common.CommonSearchFilterHelper;
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
import org.hibernate.metadata.ClassMetadata;
import org.json.JSONObject;
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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.helper.AmbilDataPenerbitBanbox;
import ais.action.master.library.helper.AmbilDataPerpustakaanBanbox;
import ais.action.master.library.helper.DetailTransaksiHelper;
import ais.action.master.library.helper.TampilanHasilScanPerHalamanWindow;
import ais.action.master.library.util.LibraryUtil;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.library.DetailTransaksi;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaBarcode;
import ais.database.model.library.JenisItem;
import ais.database.model.library.PenerimaanPengadaanItem;
import ais.database.model.library.Perpustakaan;
import ais.database.model.library.SaldoAwal;
import ais.database.model.library.TerimaPengadaanItem;
import ais.database.model.library.TipeAnggota;
import ais.database.model.library.TipeItem;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class BarcodeItemAction extends GenericAutowireComposer implements DataCriteria {

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
	protected Textbox searchtema;
	protected Textbox searchedisi;
	protected Textbox searchpengarang;
	protected Textbox searchcatatan;
	protected Textbox searchkategori;
	protected Intbox searchtahun;
	protected AmbilDataPenerbitBanbox searchpenerbit;
	protected AmbilDataPerpustakaanBanbox searchperpustakaan;
	protected Combobox searchjenisItem;
	protected Combobox searchtipeItem;
	protected Textbox searchbarcode;
	private Combobox searchfakultas;
	private Combobox searchjurusan;

	protected Textbox barcode;
	protected Combobox tipeItem;

	protected boolean edit = false;
	protected boolean delete = false;

	protected ItemPunyaBarcode itemPunyaBarcode;

	private MyToolbarbuttonConfig find;

	private String[] contents = new String[] { "id", "perpustakaan", "item", "tipeItem", "batchItemPunyaBarcode",
			"barcode" };
	private boolean padaSaatpendataanItemPerpustakaanTampilkanPilihanFakultasDanProgramStudi;

	private SaldoAwal saldoAwal;
	private PenerimaanPengadaanItem penerimaanPengadaanItem;
	private TerimaPengadaanItem terimaPengadaanItem;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		searchperpustakaan.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		if (execution.getParameter("perpustakaan") != null) {
			Session session = HibernateUtil.currentSession();
			Perpustakaan perpustakaan = (Perpustakaan) session.createCriteria(Perpustakaan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("perpustakaan").trim())))
					.uniqueResult();
			if (perpustakaan != null) {
				searchperpustakaan.setValue(perpustakaan.getNama());
				searchperpustakaan.setAttribute("perpustakaan", perpustakaan);
				searchperpustakaan.setDisabled(true);
			}
		}

		if (execution.getParameter("saldoAwal") != null) {
			Session session = HibernateUtil.currentSession();
			SaldoAwal saldoAwal = (SaldoAwal) session.createCriteria(SaldoAwal.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("saldoAwal").trim()))).uniqueResult();
			this.saldoAwal = saldoAwal;
		}

		System.out.println("saldoAwal => " + saldoAwal);

		if (execution.getParameter("penerimaanPengadaanItem") != null) {
			Session session = HibernateUtil.currentSession();
			PenerimaanPengadaanItem penerimaanPengadaanItem = (PenerimaanPengadaanItem) session
					.createCriteria(PenerimaanPengadaanItem.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("penerimaanPengadaanItem").trim())))
					.uniqueResult();
			this.penerimaanPengadaanItem = penerimaanPengadaanItem;
		}

		System.out.println("penerimaanPengadaanItem => " + penerimaanPengadaanItem);

		if (execution.getParameter("terimaPengadaanItem") != null) {
			Session session = HibernateUtil.currentSession();
			TerimaPengadaanItem terimaPengadaanItem = (TerimaPengadaanItem) session
					.createCriteria(PenerimaanPengadaanItem.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("terimaPengadaanItem").trim())))
					.uniqueResult();
			this.terimaPengadaanItem = terimaPengadaanItem;
		}

		System.out.println("terimaPengadaanItem => " + terimaPengadaanItem);

		padaSaatpendataanItemPerpustakaanTampilkanPilihanFakultasDanProgramStudi = Common.bolehKonfigurasi("saat_pendataan_item_perpustakaan_tampilkan_pilihan_fakultas_dan_prodi", Konfigurasi.TIDAK_AKTIF);

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		if (searchfakultas != null) { searchfakultas.setVisible(padaSaatpendataanItemPerpustakaanTampilkanPilihanFakultasDanProgramStudi); }
		if (searchjurusan != null) { searchjurusan.setVisible(padaSaatpendataanItemPerpustakaanTampilkanPilihanFakultasDanProgramStudi); }

		Tbmuser tbmuser = Common.getCurrentUser();

		@SuppressWarnings("unused")
		TipeAnggota tipeAnggota = LibraryUtil.UMUM;
		Common.insertCombo(searchtipeItem, "nama", TipeItem.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.insertCombo(searchjenisItem, "nama", JenisItem.class);

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		if (tbmuser.getMahasiswa() != null || tbmuser.ambilDosen() != null) {

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

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(ItemPunyaBarcode.class, this,
				"Download Barcode Item", "/img/print.png", contents);
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload Data Barcode" + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		if (upload != null) { upload.setUpload(Common.ukuranFileUpload()); }
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
														onSearchDefault(arg0);
													}
												});
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

										ClassMetadata classMetadata = HibernateUtil
												.getClassMetadata(ItemPunyaBarcode.class);
										Session session = HibernateUtil.currentNativeSession();

										int rowCount = (sheet.getLastRowNum() + 1);
										for (int i = 1; i < rowCount; i++) {
											try {

												Long id = Common.getSheetContentAsLong(sheet, 0, i);
												ItemPunyaBarcode itemPunyaBarcode = id == null || id.equals(-1L) ? null
														: (ItemPunyaBarcode) session
																.createCriteria(ItemPunyaBarcode.class)
																.add(Restrictions.idEq(id)).uniqueResult();

												if (itemPunyaBarcode == null) {
													itemPunyaBarcode = new ItemPunyaBarcode();
												}

												Common.setObjectValues(classMetadata, itemPunyaBarcode, contents, 1,
														sheet, i);

												session.getTransaction().begin();
												session.saveOrUpdate(itemPunyaBarcode);
												session.getTransaction().commit();

												label.setValue("Upload data \"" + itemPunyaBarcode.getBarcode() + " - "
														+ itemPunyaBarcode.getItem() + "\" ("
														+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
											}

										}
									} catch (Exception e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/library/BarcodeItemAction.java:350");
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
		Common.appendKeToolbar(upload, find, comp);

	        FilterLanjutHelper.setup(comp);
}

	class ItemRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub

			final ItemPunyaBarcode itemPunyaBarcode = (arg1 instanceof DetailTransaksi)
					? ((DetailTransaksi) arg1).getItemPunyaBarcode()
					: (ItemPunyaBarcode) arg1;
			if (itemPunyaBarcode != null) {
				final Item item = itemPunyaBarcode.getItem();

				final DetailTransaksiHelper detail = new DetailTransaksiHelper(itemPunyaBarcode, item,
						itemPunyaBarcode.getPerpustakaan());
				detail.setParent(arg0);

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

				new ais.ui.util.MyHtml(
						"<font style=\"font-size: x-small;\"><b>Barcode : " + (itemPunyaBarcode.getBarcode())
								+ "</b><br>ISBN 10 : " + (item.getIsbn10() == null ? "" : item.getIsbn10())
								+ "<br>ISBN 13 : " + (item.getIsbn() == null ? "" : item.getIsbn()) + "<br>ISSN : "
								+ (item.getIssn() == null ? "" : item.getIssn()) + "<br>DDC : "
								+ (item.getDdcItem() == null ? item.getDeweyDecimalClass() : item.getDdcItem())
								+ ("<br>" + Common.getBahasaConfig("Jurusan") + " : " + (jur))

								+ " </font>")
						.setParent(arg0);
				RevisiHelper.createNewRevisi(Item.class, item, item.getNama()).setParent(arg0);
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

				new Label(item.getPengarangs()).setParent(arg0);

				Session session = HibernateUtil.currentSession();

				DetailTransaksi detailTransaksi = (DetailTransaksi) session.createCriteria(DetailTransaksi.class)
						.add(Restrictions.eq("itemPunyaBarcode", itemPunyaBarcode))
						.addOrder(Order.desc("tanggalDanWaktu")).setMaxResults(1).uniqueResult();
				new Label(detailTransaksi == null ? ""
						: (Common.dateFormat3.get().format(detailTransaksi.getTanggalDanWaktu()) + " "
								+ (detailTransaksi.getKodeTransaksi() == null ? ""
										: (detailTransaksi.getKodeTransaksi().getKode() + " - "
												+ detailTransaksi.getKodeTransaksi().getNama()))
								+ " - " + DetailTransaksiHelper.dapatkanInfo(detailTransaksi)))
						.setParent(arg0);

				final java.util.List<org.zkoss.zk.ui.Component> aksiButtons = new java.util.ArrayList<org.zkoss.zk.ui.Component>();

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cetak", "/img/print.png");
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
				aksiButtons.add(button);

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
				aksiButtons.add(button);
				button.setVisible(item.getGoogleBookId() != null && !item.getGoogleBookId().trim().isEmpty());

				button = new MyToolbarbuttonConfig("Baca", "/img/Book-icon.png");
				button.setOrient("vertical");
				button.setOrient("vertical");
				button.setTooltiptext("Lihat Isi Buku");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						TampilanHasilScanPerHalamanWindow halamanWindow = new TampilanHasilScanPerHalamanWindow(
								"Isi Buku", "none", true);

						halamanWindow.init(item);
						try {
							page.getFirstRoot().appendChild(halamanWindow);
							halamanWindow.onModal();
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					}

				});
				aksiButtons.add(button);

				button = new MyToolbarbuttonConfig("Ubah", "/img/svg/edit-box-line.svg");
				button.setOrient("vertical");
				button.setTooltiptext("Ubah Data");
				button.setVisible(edit);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						init(itemPunyaBarcode);
						addWindow.setVisible(true);
						addWindow.onModal();
					}

				});
				aksiButtons.add(button);

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
												Common.refreshDelete(itemPunyaBarcode);
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

				aksiButtons.add(button);

				ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
			}
		}

	}

	protected void init(final ItemPunyaBarcode item) throws Exception {
		this.itemPunyaBarcode = item;
		addWindow.setTitle(itemPunyaBarcode.getId() == null ? "Tambah Barcode Item" : "Ubah Barcode Item");
		Common.clear(addWindow);
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

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("60%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Barcode"));
		row.appendChild(barcode = new Textbox(item.getBarcode()));
		barcode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tipe Item"));
		row.appendChild(tipeItem = new Combobox());
		tipeItem.setWidth("90%");
		tipeItem.setReadonly(true);
		Common.insertCombo(tipeItem, "nama", TipeItem.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(tipeItem, item.getTipeItem());

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

				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (barcode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Barcode Item harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (tipeItem.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tipe Item harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = check();
		if (i) {
			MyMessageboxConfig.show("Barcode sudah digunakan, coba ganti dengan barcode yang lain", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (itemPunyaBarcode.getId() != null) {
			itemPunyaBarcode = (ItemPunyaBarcode) session.load(ItemPunyaBarcode.class, itemPunyaBarcode.getId());

		}

		itemPunyaBarcode.setTipeItem((TipeItem) tipeItem.getSelectedItem().getValue());
		itemPunyaBarcode.setBarcode(barcode.getValue().trim());
		Common.refreshSaveOrUpdate(session, itemPunyaBarcode);

		return true;
	}

	public Boolean check() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(ItemPunyaBarcode.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("barcode", barcode.getValue().trim()))
				.add(this.itemPunyaBarcode.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.itemPunyaBarcode.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	public Criteria initCriteria(boolean order) {
		return initCriteria(order, false);
	}

	protected Criteria initCriteria(boolean order, boolean asc) {

		Session session = HibernateUtil.currentSession();
		Perpustakaan perpustakaan = (Perpustakaan) (searchperpustakaan.getAttribute("perpustakaan"));

		Criteria criteria = session.createCriteria(ItemPunyaBarcode.class);

		if (saldoAwal != null) {
			criteria.createAlias("batchItemPunyaBarcode", "batchItemPunyaBarcode")
					.add(Restrictions.eq("batchItemPunyaBarcode.saldoAwal", saldoAwal));
		}

		System.out.println("initCriteria saldoAwal => " + saldoAwal);

		if (penerimaanPengadaanItem != null) {
			criteria.createAlias("batchItemPunyaBarcode", "batchItemPunyaBarcode")
					.add(Restrictions.eq("batchItemPunyaBarcode.penerimaanPengadaanItem", penerimaanPengadaanItem));
		}

		System.out.println("initCriteria penerimaanPengadaanItem => " + penerimaanPengadaanItem);

		if (terimaPengadaanItem != null) {
			criteria.createAlias("batchItemPunyaBarcode", "batchItemPunyaBarcode")
					.add(Restrictions.eq("batchItemPunyaBarcode.terimaPengadaanItem", terimaPengadaanItem));
		}

		System.out.println("initCriteria terimaPengadaanItem => " + terimaPengadaanItem);

		criteria.add(perpustakaan == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("perpustakaan", perpustakaan))
				.add(searchbarcode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("barcode", searchbarcode.getValue().trim(), MatchMode.ANYWHERE));

		if (order)
			criteria.addOrder(asc ? Order.asc("barcode") : Order.desc("barcode"));

		String isbn = BarcodeItemAction.this.searchisbn.getValue().trim();
		isbn = org.apache.commons.lang3.StringUtils.replace(isbn, "-", "");

		criteria.createCriteria("item").createAlias("penerbit", "penerbit", Criteria.LEFT_JOIN)
				.add(Restrictions.isNull("defaultSatuanKerja"))
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
				.add(searchjenisItem.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenisItem", searchjenisItem.getSelectedItem().getValue()))
				.add(searchtipeItem.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tipeItem", searchtipeItem.getSelectedItem().getValue()))

				.add(!padaSaatpendataanItemPerpustakaanTampilkanPilihanFakultasDanProgramStudi
						? Restrictions.sqlRestriction("1=1") : CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(!padaSaatpendataanItemPerpustakaanTampilkanPilihanFakultasDanProgramStudi
						? Restrictions.sqlRestriction("1=1") : CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));

		return criteria;
	}

	public void onSearchDefault(Event event) {
		onSearchDefault(event, false);
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event, final Boolean dontLoop) {

		if (searchnama == null) {
			return;
		}

		Common.initPaging(initCriteria(false), paging);

		final List<Item> item = initCriteria(true, dontLoop).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(item);
		grid.setRowRenderer(new ItemRenderer());
		grid.setModelCheckMobile(strset);

	}

}
