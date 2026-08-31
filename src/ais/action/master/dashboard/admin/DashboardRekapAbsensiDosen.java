package ais.action.master.dashboard.admin;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.zkoss.poi.ss.usermodel.BorderStyle;
import org.zkoss.poi.ss.usermodel.Cell;
import org.zkoss.poi.ss.usermodel.CellStyle;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.model.impl.BookHelper;
import org.zkoss.zss.ui.Rect;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zss.ui.impl.Utils;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.North;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Komponen dashboard khusus untuk dashboard rekap absensi dosen. Kelas ini memilih variasi data
 * atau tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Spreadsheet spreadsheet}, {@code Center
 * center}, {@code Perkuliahan selectedperkuliahan}; inisialisasi/lifecycle ({@code init()}, {@code
 * initSpreadsheet()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DashboardRekapAbsensiDosen extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();

	private Perkuliahan selectedperkuliahan = null;

	public DashboardRekapAbsensiDosen(Perkuliahan perkuliahan) {
		super();
		this.selectedperkuliahan = perkuliahan;
		try {
			init();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardRekapAbsensiDosen(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() throws Exception {

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(north);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				spreadsheet.getBook().write(bout);
				bout.close();
				Filedownload.save(bout.toByteArray(),
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Rekap_Data.xlsx");
			}
		});
		print.setParent(toolbar);

	}

	private void initSpreadsheet() {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(center);

				List<Object[]> jurusans = new ArrayList<Object[]>();

				String sql = "select \n"
						+ "'Dosen Utama' as jenis, a.nama as mahasiswa,sum(case when absensi ilike '%'||a.id||',1%' then 1 else 0 end) as hadir, \n"
						+ "sum(case when absensi ilike '%'||a.id||',2%' then 1 else 0 end) as alpa, \n"
						+ "sum(case when absensi ilike '%'||a.id||',3%' then 1 else 0 end) as sakit, \n"
						+ "sum(case when absensi ilike '%'||a.id||',4%' then 1 else 0 end) as izin, \n"
						+ "(a.id) as kode_perkuliahan   \n" +

						"from dosen a  \ninner join perkuliahan d on (d.dosen1=a.id) \n"
						+ "inner join pertemuan c on (c.perkuliahan=d.id) \n" +

						"where d.perkuliahan_paralel=" + selectedperkuliahan.getId() + " or d.id="
						+ selectedperkuliahan.getId() + "  \ngroup by a.id \n" +

						"union all \n" +

						"select \n"
						+ "'Dosen II' as jenis, a.nama as mahasiswa,sum(case when absensi ilike '%'||a.id||',1%' then 1 else 0 end) as hadir, \n"
						+ "sum(case when absensi ilike '%'||a.id||',2%' then 1 else 0 end) as alpa, \n"
						+ "sum(case when absensi ilike '%'||a.id||',3%' then 1 else 0 end) as sakit, \n"
						+ "sum(case when absensi ilike '%'||a.id||',4%' then 1 else 0 end) as izin, \n"
						+ "(a.id) as kode_perkuliahan   \n" +

						"from dosen a  \ninner join perkuliahan d on (d.dosen2=a.id) \n"
						+ "inner join pertemuan c on (c.perkuliahan=d.id) \n" +

						"where d.perkuliahan_paralel=" + selectedperkuliahan.getId() + " or d.id="
						+ selectedperkuliahan.getId() + "  \ngroup by a.id \n" +

						"union all \n" +

						"select \n"
						+ "'Dosen III' as jenis, a.nama as mahasiswa,sum(case when absensi ilike '%'||a.id||',1%' then 1 else 0 end) as hadir, \n"
						+ "sum(case when absensi ilike '%'||a.id||',2%' then 1 else 0 end) as alpa, \n"
						+ "sum(case when absensi ilike '%'||a.id||',3%' then 1 else 0 end) as sakit, \n"
						+ "sum(case when absensi ilike '%'||a.id||',4%' then 1 else 0 end) as izin, \n"
						+ "(a.id) as kode_perkuliahan   \n" +

						"from dosen a  \ninner join perkuliahan d on (d.dosen3=a.id) \n"
						+ "inner join pertemuan c on (c.perkuliahan=d.id) \n" +

						"where d.perkuliahan_paralel=" + selectedperkuliahan.getId() + " or d.id="
						+ selectedperkuliahan.getId() + "  \ngroup by a.id \n" +

						"union all \n" +

						"select \n"
						+ "'Dosen IV' as jenis, a.nama as mahasiswa,sum(case when absensi ilike '%'||a.id||',1%' then 1 else 0 end) as hadir, \n"
						+ "sum(case when absensi ilike '%'||a.id||',2%' then 1 else 0 end) as alpa, \n"
						+ "sum(case when absensi ilike '%'||a.id||',3%' then 1 else 0 end) as sakit, \n"
						+ "sum(case when absensi ilike '%'||a.id||',4%' then 1 else 0 end) as izin, \n"
						+ "(a.id) as kode_perkuliahan   \n" +

						"from dosen a  \ninner join perkuliahan d on (d.dosen4=a.id) \n"
						+ "inner join pertemuan c on (c.perkuliahan=d.id) \n" +

						"where d.perkuliahan_paralel=" + selectedperkuliahan.getId() + " or d.id="
						+ selectedperkuliahan.getId() + "  \ngroup by a.id \n" +

						"union all \n" +

						"select \n"
						+ "'Dosen V' as jenis, a.nama as mahasiswa,sum(case when absensi ilike '%'||a.id||',1%' then 1 else 0 end) as hadir, \n"
						+ "sum(case when absensi ilike '%'||a.id||',2%' then 1 else 0 end) as alpa, \n"
						+ "sum(case when absensi ilike '%'||a.id||',3%' then 1 else 0 end) as sakit, \n"
						+ "sum(case when absensi ilike '%'||a.id||',4%' then 1 else 0 end) as izin, \n"
						+ "(a.id) as kode_perkuliahan   \n" +

						"from dosen a  \ninner join perkuliahan d on (d.dosen5=a.id) \n"
						+ "inner join pertemuan c on (c.perkuliahan=d.id) \n" +

						"where d.perkuliahan_paralel=" + selectedperkuliahan.getId() + " or d.id="
						+ selectedperkuliahan.getId() + "  \ngroup by a.id \n" +

						"union all \n" +

						"select \n"
						+ "'Dosen VI' as jenis, a.nama as mahasiswa,sum(case when absensi ilike '%'||a.id||',1%' then 1 else 0 end) as hadir, \n"
						+ "sum(case when absensi ilike '%'||a.id||',2%' then 1 else 0 end) as alpa, \n"
						+ "sum(case when absensi ilike '%'||a.id||',3%' then 1 else 0 end) as sakit, \n"
						+ "sum(case when absensi ilike '%'||a.id||',4%' then 1 else 0 end) as izin, \n"
						+ "(a.id) as kode_perkuliahan   \n" +

						"from dosen a  \ninner join perkuliahan d on (d.dosen6=a.id) \n"
						+ "inner join pertemuan c on (c.perkuliahan=d.id) \n" +

						"where d.perkuliahan_paralel=" + selectedperkuliahan.getId() + " or d.id="
						+ selectedperkuliahan.getId() + "  \ngroup by a.id \n" +

						"union all \n" +

						"select \n"
						+ "'Dosen VII' as jenis, a.nama as mahasiswa,sum(case when absensi ilike '%'||a.id||',1%' then 1 else 0 end) as hadir, \n"
						+ "sum(case when absensi ilike '%'||a.id||',2%' then 1 else 0 end) as alpa, \n"
						+ "sum(case when absensi ilike '%'||a.id||',3%' then 1 else 0 end) as sakit, \n"
						+ "sum(case when absensi ilike '%'||a.id||',4%' then 1 else 0 end) as izin, \n"
						+ "(a.id) as kode_perkuliahan   \n" +

						"from dosen a  \ninner join perkuliahan d on (d.dosen7=a.id) \n"
						+ "inner join pertemuan c on (c.perkuliahan=d.id) \n" +

						"where d.perkuliahan_paralel=" + selectedperkuliahan.getId() + " or d.id="
						+ selectedperkuliahan.getId() + "  \ngroup by a.id \n" +

						"union all \n" +

						"select \n"
						+ "'Dosen VIII' as jenis, a.nama as mahasiswa,sum(case when absensi ilike '%'||a.id||',1%' then 1 else 0 end) as hadir, \n"
						+ "sum(case when absensi ilike '%'||a.id||',2%' then 1 else 0 end) as alpa, \n"
						+ "sum(case when absensi ilike '%'||a.id||',3%' then 1 else 0 end) as sakit, \n"
						+ "sum(case when absensi ilike '%'||a.id||',4%' then 1 else 0 end) as izin, \n"
						+ "(a.id) as kode_perkuliahan   \n" +

						"from dosen a  \ninner join perkuliahan d on (d.dosen8=a.id) \n"
						+ "inner join pertemuan c on (c.perkuliahan=d.id) \n" +

						"where d.perkuliahan_paralel=" + selectedperkuliahan.getId() + " or d.id="
						+ selectedperkuliahan.getId() + "  \ngroup by a.id \n" +

						"union all \n" +

						"select \n"
						+ "'Dosen IX' as jenis, a.nama as mahasiswa,sum(case when absensi ilike '%'||a.id||',1%' then 1 else 0 end) as hadir, \n"
						+ "sum(case when absensi ilike '%'||a.id||',2%' then 1 else 0 end) as alpa, \n"
						+ "sum(case when absensi ilike '%'||a.id||',3%' then 1 else 0 end) as sakit, \n"
						+ "sum(case when absensi ilike '%'||a.id||',4%' then 1 else 0 end) as izin, \n"
						+ "(a.id) as kode_perkuliahan   \n" +

						"from dosen a  \ninner join perkuliahan d on (d.dosen9=a.id) \n"
						+ "inner join pertemuan c on (c.perkuliahan=d.id) \n" +

						"where d.perkuliahan_paralel=" + selectedperkuliahan.getId() + " or d.id="
						+ selectedperkuliahan.getId() + "  \ngroup by a.id \n" +

						"union all \n" +

						"select \n"
						+ "'Dosen X' as jenis, a.nama as mahasiswa,sum(case when absensi ilike '%'||a.id||',1%' then 1 else 0 end) as hadir, \n"
						+ "sum(case when absensi ilike '%'||a.id||',2%' then 1 else 0 end) as alpa, \n"
						+ "sum(case when absensi ilike '%'||a.id||',3%' then 1 else 0 end) as sakit, \n"
						+ "sum(case when absensi ilike '%'||a.id||',4%' then 1 else 0 end) as izin, \n"
						+ "(a.id) as kode_perkuliahan   \n" +

						"from dosen a  \ninner join perkuliahan d on (d.dosen10=a.id) \n"
						+ "inner join pertemuan c on (c.perkuliahan=d.id) \n" +

						"where d.perkuliahan_paralel=" + selectedperkuliahan.getId() + " or d.id="
						+ selectedperkuliahan.getId() + "  \ngroup by a.id";

				System.out.println(sql);
				jurusans = Common.ambilSql(sql);

				spreadsheet = new ais.ui.util.MySpreadsheet();
				Common.clear(center);
				org.zkoss.zul.Div pembungkusVisual = new org.zkoss.zul.Div();
				pembungkusVisual.setWidth("100%");
				pembungkusVisual.setStyle("height:100%;overflow:auto;box-sizing:border-box;");
				pembungkusVisual.setParent(center);
				new org.zkoss.zul.Html(
						ais.action.master.dashboard.helper.DashboardVisualHelper.kehadiran(jurusans, 1, 2, 3, 4, 5))
						.setParent(pembungkusVisual);
				spreadsheet.setParent(pembungkusVisual);
				spreadsheet.setWidth("100%");
				spreadsheet.setHeight("100%");
				spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
				spreadsheet.setMaxcolumns(7);
				spreadsheet.setMaxrows(jurusans.size() + 4);

				Worksheet sheet = spreadsheet.getSelectedSheet();
				sheet.setDefaultColumnWidth(40);
				ais.ui.util.EcampusUtil.setBold(sheet,
						new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

				ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0,
						"REKAPITULASI ABSENSI DOSEN\n" + selectedperkuliahan.info().toUpperCase());
				final String color = "#000000";
				int rowIndex = 2;
				int colIndex = 0;
				Utils.setRowHeight(sheet, 1, 150);
				ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
				Cell cell = Utils.getCell(sheet, 1, 0);
				cell.getCellStyle().setWrapText(true);
				cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);

				ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 0, 1, spreadsheet.getMaxcolumns() - 1, false);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Jenis");
				Utils.setColumnWidth(sheet, 0, 100);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "Dosen");
				Utils.setColumnWidth(sheet, 1, 200);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, Common.getBahasaConfig("Hadir"));
				Utils.setColumnWidth(sheet, 2, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, Common.getBahasaConfig("Alpa"));
				Utils.setColumnWidth(sheet, 3, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, Common.getBahasaConfig("Sakit"));
				Utils.setColumnWidth(sheet, 4, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, Common.getBahasaConfig("Izin"));
				Utils.setColumnWidth(sheet, 5, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "Total tidak hadir");
				Utils.setColumnWidth(sheet, 6, 80);

				ais.ui.util.EcampusUtil.setBorder(sheet,
						new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
						BorderStyle.THIN, color);
				ais.ui.util.EcampusUtil.setBold(sheet,
						new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);

				rowIndex = 3;
				colIndex = 0;

				Integer nilai0Total = 0;
				Integer nilai1Total = 0;
				Integer nilai2Total = 0;
				Integer nilai3Total = 0;
				Integer totalSemua = 0;
				for (Object[] objects : jurusans) {
					if (objects[0] == null)
						continue;
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, objects[0].toString());

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, objects[1].toString());

					Integer nilai0 = ((Number) (objects[2] == null ? 0 : objects[2])).intValue();
					Integer nilai1 = ((Number) (objects[3] == null ? 0 : objects[3])).intValue();
					Integer nilai2 = ((Number) (objects[4] == null ? 0 : objects[4])).intValue();
					Integer nilai3 = ((Number) (objects[5] == null ? 0 : objects[5])).intValue();

					Integer total = nilai1 + nilai2 + nilai3;

					nilai0Total += nilai0;
					nilai1Total += nilai1;
					nilai2Total += nilai2;
					nilai3Total += nilai3;
					totalSemua += total;

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, nilai0);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, nilai1);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, nilai2);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, nilai3);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, total);

					try {
						ais.ui.util.EcampusUtil.setBorder(sheet,
								new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
								BookHelper.BORDER_FULL, BorderStyle.THIN, color);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardRekapAbsensiDosen.java:350");
					}

					rowIndex++;
				}

				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "TOTAL");
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, nilai0Total);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, nilai1Total);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, nilai2Total);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, nilai3Total);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, totalSemua);

				Common.setStyled(sheet);
				spreadsheet.setMaxrows(rowIndex + 1);
				// Excel mentah -> grid ringan (Book tetap hidup utk tombol Download). Pola B PratinjauXlsxHelper.
				ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

				colIndex = 0;
				try {
					ais.ui.util.EcampusUtil.setBold(sheet,
							new Rect(spreadsheet.getMaxcolumns() - 1, 3, spreadsheet.getMaxcolumns() - 1, rowIndex),
							true);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardRekapAbsensiDosen.java:373");
				}
			}
		});

	}
}
