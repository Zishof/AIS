package ais.action.master.dashboard.keuangan;
import ais.ui.util.DashboardGridExportHelper;

/*
 * ENHANCED_DASHBOARD_UIUX_HTML_CSS_2026_06_06
 * Baseline dari file upload terbaru, disusun ulang sesuai package /java/ais/...
 * Catatan: openSession tetap ditutup pada finally; currentSession tidak ditutup manual.
 * Grafik, tren, radar, dan spider web dipertahankan sebagai HTML/CSS agar ringan dan aman di ZK 5.5.
 */


import java.util.Iterator;
import java.util.List;

import org.hibernate.Session;
import org.json.JSONObject;
import org.zkoss.poi.ss.usermodel.BorderStyle;
import org.zkoss.poi.ss.usermodel.Cell;
import org.zkoss.poi.ss.usermodel.CellStyle;
import org.zkoss.poi.ss.usermodel.Font;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.model.impl.BookHelper;
import org.zkoss.zss.ui.Rect;
import org.zkoss.zss.ui.impl.Utils;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Vbox;

import ais.action.maintenance.MainAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MySpreadsheet;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Menyiapkan rincian piutang dalam tampilan spreadsheet agar data lebih mudah dicek dan diekspor.
 * Semua tabel besar diarahkan memakai paging 10 baris agar tampilan ringan dan mudah dibaca.
 */

public class DasboardPiutangRInciExcel extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;
	private Center center;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private Combobox jenisPembayaran = new Combobox();
	private MyTextbox nama;
	private Combobox comboTampilkan;
	private Paging paging;
	private int jumlahDataDalamSatuHalamanElearning;
	private MySpreadsheet spreadsheet;
//	private MyCheckboxConfig sudahPosting;

	public DasboardPiutangRInciExcel() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DasboardPiutangRInciExcel(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {
		DashboardGridExportHelper.pasang(this, "Piutang R Inci Excel");

		jenisPembayaran = Common.createComboJenisPembayaranDanSemua(jenisPembayaran);

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(this);
		borderlayout.setStyle("min-height:25000px");
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getUserId() != null) {
			Integer desktopHeight = MainAction.desktopHeights.get(tbmuser.getUserId());
			if (desktopHeight != null) {
				borderlayout.setStyle("min-height:" + (desktopHeight * 0.9) + "px");
			}
		}

		North north = new North();
		ais.ui.util.ZkCompat.setFlex(north, true);
		north.setParent(borderlayout);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("150x");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("150x");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setWidth("150x");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa"));
		row.appendChild(nama = new MyTextbox());
		nama.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		semesterAbsensi = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig("Semua");
		comboitem.setValue(null);
		semesterAbsensi.appendChild(comboitem);
		semesterAbsensi.setSelectedIndex(2);
		row.appendChild(semesterAbsensi);
		semesterAbsensi.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pembayaran"));
		row.appendChild(jenisPembayaran);
		jenisPembayaran.setWidth("90%");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				reload(true);
			}
		};

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "1,1,4");

		comboTampilkan = new Combobox();
		Integer[] dataCombo = new Integer[] { 10, 30, 50, 100, 300, 500, 1000 };
		for (Integer d : dataCombo) {
			comboitem = new MyComboitemConfig(d + " tampilan");
			comboitem.setValue(d);
			comboTampilkan.appendChild(comboitem);
		}
		comboTampilkan.setReadonly(true);
		Common.selectComboItem(comboTampilkan, 100);
		comboTampilkan.setParent(row);
		comboTampilkan.setCols(7);

//		sudahPosting = new MyCheckboxConfig("Sudah Posting");
//		sudahPosting.setParent(row);

		MyToolbarbuttonConfig tampilkan;
		row.appendChild(tampilkan = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png"));
		tampilkan.addEventListener("onClick", eventListener);

		row.appendChild(new ais.ui.util.MyLabelConfig(""));

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "6");

		jumlahDataDalamSatuHalamanElearning = 10;

		paging = new Paging();
		paging.setMold("os");
		paging.setParent(row);
		Common.initPagingCustom(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload(false);
			}
		}, jumlahDataDalamSatuHalamanElearning);

		center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		Common.createDefaultTimer(eventListener);

	}

	@SuppressWarnings({ "unchecked" })
	private void reload(boolean hitungUlangPaging) {
		Common.clear(center);
		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(10);
		Vbox mainVbox = new Vbox();
		mainVbox.setParent(center);
		mainVbox.setWidth("100%");
		mainVbox.setSpacing("12px");
		mainVbox.setStyle("padding:12px; background:#f8fafc; box-sizing:border-box; overflow:auto;");
		ais.ui.util.DashboardJurnalPembayaranUtil.renderJurnalMahasiswaPanel(mainVbox, "Ringkasan Jurnal Pembayaran Mahasiswa",
				"Menampilkan ringkasan akun jurnal sebelum pengguna membuka format spreadsheet. Data utama tetap tersedia di tabel/spreadsheet dan file Excel baru dibuat ketika tombol download dijalankan.");
		spreadsheet.setParent(mainVbox);

		String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null
				|| this.tahunAkademik.getSelectedItem().getValue() == null ? null
						: this.tahunAkademik.getSelectedItem().getValue());
		String semester = (String) (this.semesterAbsensi.getSelectedItem() == null
				|| semesterAbsensi.getSelectedItem().getValue() == null ? null
						: this.semesterAbsensi.getSelectedItem().getValue());

		Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());

		JenisKegiatan jenisPembayaran = (JenisKegiatan) (this.jenisPembayaran.getSelectedItem() == null ? null
				: this.jenisPembayaran.getSelectedItem().getValue());

		jumlahDataDalamSatuHalamanElearning = (Integer) comboTampilkan.getSelectedItem().getValue();
		int mulai = jumlahDataDalamSatuHalamanElearning * (paging == null ? 0 : paging.getActivePage());

		Session session = HibernateUtil.currentSession();
		if (hitungUlangPaging) {
			Number size = 10000000;

			paging.setPageSize(jumlahDataDalamSatuHalamanElearning);
			paging.setMold("os");
			paging.setTotalSize(size == null ? 0 : size.intValue());
			paging.setDetailed(false);
			paging.getParent().setVisible((size == null ? 0 : size.intValue()) > jumlahDataDalamSatuHalamanElearning);
		}

		String sql = "select (case when c.nim is not null then c.nim else b.no_registrasi end) as kode_transaksi, "
				+ "(case when c.nama is not null then c.nama else b.nama end) as nama, d.nama_kegiatan as nama_jenis_kegiatan, a.dibayar, a.tagihan, a.tagihans, a.bulans, a.tahun_akademik, a.semster "
				+ " from kegiatan a  left join biodata_calon_mahasiswa b on (a.calon_mahasiswa = b.id) "
				+ "left join mahasiswa c on (a.mahasiswa = c.id) "

				+ " inner join jenis_kegiatan d on (a.jenis_kegiatan=d.id)  "
				+ " left join jurusan x on (a.jurusan = x.id   ) "
				+ "where a.aktif and (c.nama ilike :buktilike or b.nama ilike :buktilike or c.nim ilike :buktilike or b.no_registrasi ilike :buktilike) "
				+ " and (a.dibayar > 0.1 or a.tagihan > 0.1) ";

		sql += (jurusan == null ? "" : " and a.jurusan = " + jurusan.getId());
		sql += (fakultas == null ? "" : " and x.fakultas = " + fakultas.getId());
		sql += (jenisPembayaran == null ? "" : " and a.jenis_kegiatan = " + jenisPembayaran.getId());

		if (tahunAkademik != null) {
			sql += " and a.tahun_akademik = :tahunAkademik ";
		}

		if (semester != null) {
			sql += ((semester.equals(Perkuliahan.GENAP) ? " and a.semster % 2 = 0 " : " and a.semster % 2 = 1 "));
		}

		sql += " order by d.nama_kegiatan, (case when c.nama is not null then c.nama else b.nama end)  " + "  limit "
				+ jumlahDataDalamSatuHalamanElearning + "  offset " + mulai;

		List<Object[]> objects = session.createSQLQuery(sql).setString("buktilike", "%" + nama.getValue().trim() + "%")
				.setString("tahunAkademik", tahunAkademik == null ? "-1" : tahunAkademik)

				.list();

		spreadsheet.setMaxrows(objects.size() + 4);
		Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(40);

		ais.ui.util.EcampusUtil.setBold(sheet,
				new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

		Font hlink_font = sheet.getWorkbook().createFont();
		hlink_font.setUnderline(XSSFFont.U_SINGLE);

		CellStyle hlink_style = sheet.getWorkbook().createCellStyle();
		hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		hlink_style.setFont(hlink_font);

		ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0, "REKAPITULASI KARTU PIUTANG");

		final String color = "#000000";
		int rowIndex = 2;
		int colIndex = 0;
		Utils.setRowHeight(sheet, 1, 150);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
		Cell cell = Utils.getCell(sheet, 1, 0);
		cell.getCellStyle().setWrapText(true);
		cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);

		ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 0, 1, spreadsheet.getMaxcolumns() - 1, false);

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "NIM/No.Reg");
		Utils.setColumnWidth(sheet, 0, 120);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "Nama");
		Utils.setColumnWidth(sheet, 1, 200);

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Item Biaya");
		Utils.setColumnWidth(sheet, 2, 150);

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "Bulan");
		Utils.setColumnWidth(sheet, 3, 150);

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "TA");
		Utils.setColumnWidth(sheet, 4, 150);

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "Smt");
		Utils.setColumnWidth(sheet, 5, 50);

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "Dibayar");
		Utils.setColumnWidth(sheet, 6, 150);

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, "Tagihan");
		Utils.setColumnWidth(sheet, 7, 150);

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, "Sisa");
		Utils.setColumnWidth(sheet, 8, 150);

		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				true);

		rowIndex = 2;

		String kodeAkun = "";
		Double totalDibayar = 0.0;
		Double totalTagihan = 0.0;
		Double totalSisa = 0.0;
		for (Object[] o : objects) {

			try {
				String kode_transaksi = o[0] == null ? "" : o[0].toString();
				String nama = o[1] == null ? "" : o[1].toString();
				String nama_jenis_kegiatan = o[2] == null ? "" : o[2].toString();

				Number dibayar = o[3] == null ? 0.0 : (Number) o[3];
				Number tagihan = o[4] == null ? 0.0 : (Number) o[4];
				Number sisa = tagihan.doubleValue() - dibayar.doubleValue();

				String tagihansD = o[5] == null ? "" : o[5].toString();
				String bulansD = o[6] == null ? "" : o[6].toString();

				String tahun_akademik = o[7] == null ? "" : o[7].toString();
				Number semster = o[8] == null ? 0.0 : (Number) o[8];

				JSONObject dibayars = null;
				JSONObject tagihans = null;

				dibayars = new JSONObject(bulansD);
				tagihans = new JSONObject(tagihansD);

				if (!kodeAkun.equalsIgnoreCase(nama_jenis_kegiatan)) {

					if (!kodeAkun.isEmpty()) {

						rowIndex++;
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Total");
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, nama_jenis_kegiatan);
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "");
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "");
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "");
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "");

						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6,
								totalDibayar.doubleValue() >= 0.0
										? Common.numberFormat.get().format(totalDibayar.doubleValue())
										: "(" + Common.numberFormat.get().format(Math.abs(totalDibayar.doubleValue())) + ")");

						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7,
								totalTagihan.doubleValue() >= 0.0
										? Common.numberFormat.get().format(totalTagihan.doubleValue())
										: "(" + Common.numberFormat.get().format(Math.abs(totalTagihan.doubleValue())) + ")");

						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8,
								totalSisa.doubleValue() >= 0.0 ? Common.numberFormat.get().format(totalSisa.doubleValue())
										: "(" + Common.numberFormat.get().format(Math.abs(totalSisa.doubleValue())) + ")");

						totalDibayar = 0.0;
						totalTagihan = 0.0;
						totalSisa = 0.0;

					}

					rowIndex++;
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "");
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, nama_jenis_kegiatan);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "");
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "");
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "");
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "");
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "");
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, "");
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, "");

					kodeAkun = nama_jenis_kegiatan;
				}

				totalDibayar += dibayar.doubleValue();
				totalTagihan += tagihan.doubleValue();
				totalSisa += sisa.doubleValue();

				Iterator<String> iterator = tagihans.keys();
				while (iterator.hasNext()) {
					try {
						String v = iterator.next();
						Object val = tagihans.get(v) + "";
						String[] vv = v.split("_");
						Long idItem = Long.parseLong(vv[0].trim());

						Integer bulanBayar = vv.length < 2 ? null : Integer.parseInt(vv[1].trim());
						Double tag = Double.parseDouble(val + "");
						Double dib = 0.0;
						if (tag.intValue() != 0) {

							ItemBiaya itemBiaya = (ItemBiaya) ConstantValues.ambil(ItemBiaya.class.getName(), idItem);

							Iterator<String> iteratorDibayar = dibayars.keys();
							while (iteratorDibayar.hasNext()) {
								try {
									String vvc = iteratorDibayar.next();

									Object valv = dibayars.get(vvc) + "";
									String[] vvv = vvc.split("_");

									Long idItemV = Long.parseLong(vvv[0].trim());
									Integer bulanBayarV = vvv.length < 2 ? null : Integer.parseInt(vvv[1]);

									if (bulanBayarV.intValue() == 0) {
										bulanBayarV = null;
									}

									if (idItemV.equals(idItem)
											&& ((bulanBayar == null && bulanBayarV == null) || (bulanBayar != null
													&& bulanBayarV != null && bulanBayar.equals(bulanBayarV)))) {
										Double nn = Double.parseDouble(valv + "");
										dib = nn;
										break;
									}
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/keuangan/DasboardPiutangRInciExcel.java:518");
									// TODO: handle exception
								}
							}

							Number sisaV = tag.doubleValue() - dib.doubleValue();

							rowIndex++;
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, kode_transaksi);
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, nama);
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2,
									itemBiaya == null ? "" : itemBiaya.getNama());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3,
									bulanBayar == null ? "" : Common.numberFormat.get().format(bulanBayar));
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, tahun_akademik);
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5,
									Common.numberFormat.get().format(semster.intValue()));
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6,
									dib.doubleValue() >= 0.0 ? Common.numberFormat.get().format(dib.doubleValue())
											: "(" + Common.numberFormat.get().format(Math.abs(dib.doubleValue())) + ")");
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7,
									tag.doubleValue() >= 0.0 ? Common.numberFormat.get().format(tag.doubleValue())
											: "(" + Common.numberFormat.get().format(Math.abs(tag.doubleValue())) + ")");
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8,
									sisaV.doubleValue() >= 0.0 ? Common.numberFormat.get().format(sisaV.doubleValue())
											: "(" + Common.numberFormat.get().format(Math.abs(sisaV.doubleValue())) + ")");

						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/keuangan/DasboardPiutangRInciExcel.java:547");
					}
				}

			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/keuangan/DasboardPiutangRInciExcel.java:551");
				// TODO: handle exception
			}

		}

		if (!kodeAkun.isEmpty()) {

			rowIndex++;
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Total");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, kodeAkun);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "");

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6,
					totalDibayar.doubleValue() >= 0.0 ? Common.numberFormat.get().format(totalDibayar.doubleValue())
							: "(" + Common.numberFormat.get().format(Math.abs(totalDibayar.doubleValue())) + ")");

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7,
					totalTagihan.doubleValue() >= 0.0 ? Common.numberFormat.get().format(totalTagihan.doubleValue())
							: "(" + Common.numberFormat.get().format(Math.abs(totalTagihan.doubleValue())) + ")");

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8,
					totalSisa.doubleValue() >= 0.0 ? Common.numberFormat.get().format(totalSisa.doubleValue())
							: "(" + Common.numberFormat.get().format(Math.abs(totalSisa.doubleValue())) + ")");

		}

		// Excel mentah -> grid ringan (Book tetap hidup untuk tombol Ekspor Excel/Download). Pola B PratinjauXlsxHelper.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

	}
}
