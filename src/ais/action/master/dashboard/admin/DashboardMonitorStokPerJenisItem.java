package ais.action.master.dashboard.admin;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.zkoss.poi.ss.usermodel.BorderStyle;
import org.zkoss.poi.ss.usermodel.Cell;
import org.zkoss.poi.ss.usermodel.CellStyle;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.model.impl.BookHelper;
import org.zkoss.zss.ui.Rect;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zss.ui.impl.Utils;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Vbox;

import ais.action.master.library.helper.AmbilDataPerpustakaanBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.library.JenisItem;
import ais.database.model.library.Perpustakaan;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DashboardMonitorStokPerJenisItem extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private MyDatebox mulai = new MyDatebox();
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();

	private AmbilDataPerpustakaanBanbox perpustakaan;

	private boolean padaSaatpendataanItemPerpustakaanTampilkanPilihanFakultasDanProgramStudi;

	private Perpustakaan myperpustakaan;

	public DashboardMonitorStokPerJenisItem() {
		super();
		try {
			init();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardMonitorStokPerJenisItem(Perpustakaan perpustakaan) {
		super();
		myperpustakaan = perpustakaan;
		try {
			init();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardMonitorStokPerJenisItem(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() throws Exception {

		padaSaatpendataanItemPerpustakaanTampilkanPilihanFakultasDanProgramStudi = Common.bolehKonfigurasi("saat_pendataan_item_perpustakaan_tampilkan_pilihan_fakultas_dan_prodi", Konfigurasi.TIDAK_AKTIF);

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		// FIX toolbar/tombol tidak tampil: pada ZK5 region North memakai tinggi bawaan
		// (+-100px); dengan flex=true isinya diregangkan ke tinggi tersebut sehingga
		// Toolbar yang diletakkan DI BAWAH grid filter ikut terpotong. Disamakan dengan
		// layar sejenis yang sudah benar (DownloadMahasiswa, DownloadKrs, DownloadNilai):
		// flex dimatikan + tinggi eksplisit. Autoscroll sebagai pengaman bila isi bertambah.
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("160px");
		north.setAutoscroll(true);

		Vbox div = new Vbox();
		div.setParent(north);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(div);
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setStyle("border:0px;background: transparent;");

		Hbox toolbar = new Hbox();
		toolbar.setStyle("border:0px;background: transparent;");
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				spreadsheet.getBook().write(bout);
				bout.close();
				Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Rekap_per_item.xlsx");
			}
		});
		print.setParent(toolbar);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Per Tanggal"));
		mulai.setValue(ais.ui.util.WaktuUtil.getDate());
		mulai.setReadonly(true);
		row.appendChild(mulai);
		mulai.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perpustakaan"));
		row.appendChild(perpustakaan = new AmbilDataPerpustakaanBanbox());
		perpustakaan.setWidth("90%");
		perpustakaan.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		if (myperpustakaan != null) {
			perpustakaan.setAttribute("perpustakaan", myperpustakaan);
			perpustakaan.setValue(myperpustakaan.toString());
			perpustakaan.setDisabled(true);
		}

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		// South south = new South();
		// ais.ui.util.ZkCompat.setFlex(south, true);
		// south.setParent(borderlayout);

		initSpreadsheet();

	}

	@SuppressWarnings({ "unchecked" })
	private void initSpreadsheet() {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(center);

				String dateMulai = mulai.getValue() == null
						? Common.databaseDateFormat.get().format(ais.ui.util.WaktuUtil.getDate())
						: Common.databaseDateFormat.get().format(mulai.getValue());
				Perpustakaan perpustakaan = (Perpustakaan) DashboardMonitorStokPerJenisItem.this.perpustakaan
						.getAttribute("perpustakaan");

				if (dateMulai == null) {
					return;
				}

				Session session = HibernateUtil.currentSession();
				List<JenisItem> jenisItems = session.createCriteria(JenisItem.class).addOrder(Order.asc("nama")).list();

				String subSql = "";
				for (JenisItem jenisItem : jenisItems) {
					String s = "sum(case when c.jenis_item=" + jenisItem.getId()
							+ " then ((a.qty+a.qtybonus)*b.jenis) else 0 end) as stok" + jenisItem.getId() + ",";
					subSql += s;
				}

				spreadsheet = new ais.ui.util.MySpreadsheet();
				Common.clear(center);
				final org.zkoss.zul.Div pembungkusVisual = new org.zkoss.zul.Div();
				pembungkusVisual.setWidth("100%");
				pembungkusVisual.setStyle("height:100%;overflow:auto;box-sizing:border-box;");
				pembungkusVisual.setParent(center);
				spreadsheet.setParent(pembungkusVisual);
				spreadsheet.setWidth("100%");
				spreadsheet.setHeight("100%");
				spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
				spreadsheet.setMaxcolumns(jenisItems.size() + 2);

				Worksheet sheet = spreadsheet.getSelectedSheet();
				sheet.setDefaultColumnWidth(40);
				ais.ui.util.EcampusUtil.setBold(sheet,
						new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

				ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0, "STOK PER JENIS ITEM \n PER TANGGAL " + dateMulai);
				final String color = "#000000";
				int rowIndex = 2;
				int colIndex = 0;
				Utils.setRowHeight(sheet, 1, 150);
				ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
				Cell cell = Utils.getCell(sheet, 1, 0);
				cell.getCellStyle().setWrapText(true);
				cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);

				ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 0, 1, spreadsheet.getMaxcolumns() - 1, false);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "*");
				Utils.setColumnWidth(sheet, 0, 150);
				int col = 1;
				for (JenisItem jenisItem : jenisItems) {
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, col, jenisItem.getNama());
					Utils.setColumnWidth(sheet, col, 100);
					col++;
				}

				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, col, "Total");
				Utils.setColumnWidth(sheet, col, 100);

				ais.ui.util.EcampusUtil.setBorder(sheet,
						new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
						BorderStyle.THIN, color);
				ais.ui.util.EcampusUtil.setBold(sheet,
						new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);

				rowIndex = 3;
				colIndex = 0;

				String sql = "select max(c.isbn) as semua," + subSql
						+ "max(c.isbn) as semuatoa from library.detail_transaksi a "
						+ "inner join library.kode_transaksi b on (a.kode_transaksi = b.id) "
						+ "left join library.item c on (a.item = c.id) where c.jurusan is null  and a.perpustakaan = "
						+ (perpustakaan == null ? "a.perpustakaan" : perpustakaan.getId())
						+ " and date(a.tanggal) <= date('"
						+ (Common.databaseDateFormat.get()
								.format(mulai.getValue() == null ? ais.ui.util.WaktuUtil.getDate() : mulai.getValue()))
						+ "') ";

				System.out.println(sql);
				List<Object[]> data = session.createSQLQuery(sql).list();

				int[] totals = new int[jenisItems.size() + 1];
				col = 0;
				for (@SuppressWarnings("unused")
				JenisItem jenisItem : jenisItems) {
					totals[col] = 0;
					col++;
				}
				totals[col] = 0;
				for (Object[] objects : data) {
					if (objects == null)
						continue;

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "");
					col = 1;
					int total = 0;
					for (@SuppressWarnings("unused")
					JenisItem jenisItem : jenisItems) {
						int count = (int) Double.parseDouble(objects[col] == null ? "0" : objects[col].toString());
						totals[col - 1] += count;
						total += count;
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, col, count);
						col++;
					}
					totals[col - 1] += total;
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, col, total);

					rowIndex++;
				}

				if (padaSaatpendataanItemPerpustakaanTampilkanPilihanFakultasDanProgramStudi) {
					List<Jurusan> jurusans = session.createCriteria(Jurusan.class).addOrder(Order.asc("nama")).list();

					for (Jurusan jurusan : jurusans) {
						sql = "select max(c.isbn) as semua," + subSql
								+ "max(c.isbn) as semuatoa from library.detail_transaksi a "
								+ "inner join library.kode_transaksi b on (a.kode_transaksi = b.id) "
								+ "left join library.item c on (a.item = c.id) where c.jurusan = " + jurusan.getId()
								+ " and a.perpustakaan = "
								+ (perpustakaan == null ? "a.perpustakaan" : perpustakaan.getId())
								+ " and date(a.tanggal) <= date('"
								+ (Common.databaseDateFormat.get().format(
										mulai.getValue() == null ? ais.ui.util.WaktuUtil.getDate() : mulai.getValue()))
								+ "') ";
						System.out.println(sql);
						data = session.createSQLQuery(sql).list();
						for (Object[] objects : data) {
							if (objects == null)
								continue;

							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, jurusan.getNama());
							col = 1;
							int total = 0;
							for (@SuppressWarnings("unused")
							JenisItem jenisItem : jenisItems) {
								int count = (int) Double
										.parseDouble(objects[col] == null ? "0" : objects[col].toString());
								totals[col - 1] += count;
								total += count;
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, col, count);
								col++;
							}
							totals[col - 1] += total;
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, col, total);

							rowIndex++;
						}
					}
				}

				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Jumlah Total");
				col = 1;
				for (int tot : totals) {
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, col, tot);
					col++;
				}

				Common.setStyled(sheet);spreadsheet.setMaxrows(rowIndex + 1);
				// Excel mentah -> grid ringan (Book tetap hidup utk tombol Download). Pola B PratinjauXlsxHelper.
				ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

				try {
					java.util.LinkedHashMap<String, Double> komposisiStok = new java.util.LinkedHashMap<String, Double>();
					for (int ji = 0; ji < jenisItems.size(); ji++) {
						JenisItem jenisItem = jenisItems.get(ji);
						double nilai = ji < totals.length ? totals[ji] : 0;
						komposisiStok.put(jenisItem == null || jenisItem.getNama() == null ? "Tidak ditentukan"
								: jenisItem.getNama(), Double.valueOf(nilai));
					}
					org.zkoss.zul.Html chartStok = new org.zkoss.zul.Html(
							ais.action.master.dashboard.helper.DashboardVisualHelper.komposisi("Stok", "item",
									komposisiStok));
					if (pembungkusVisual.getFirstChild() != null) {
						pembungkusVisual.insertBefore(chartStok, pembungkusVisual.getFirstChild());
					} else {
						pembungkusVisual.appendChild(chartStok);
					}
				} catch (Exception eChartStok) {
					Common.tampilErrorJikaAdmin(eChartStok);
				}
			}
		});

	}
}
