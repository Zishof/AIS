package ais.action.master.dashboard.admin;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.LogUserActifity;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Html;
public class DashboardLogAktifitasPengguna extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox searchjenisaktifitas = new Combobox();
	private Textbox searchnamapengguna = new Textbox();
	private Textbox searchdata = new Textbox();
	private Textbox searchaktifitas = new Textbox();
	private MyDatebox mulai = new MyDatebox();
	private MyDatebox sampai = new MyDatebox();
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();

	public DashboardLogAktifitasPengguna() {
		super();
		try {
			init();
			initFakultas();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardLogAktifitasPengguna(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			initFakultas();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private String buildDashboardIntroHtml() {
		StringBuilder html = new StringBuilder(12000);
		html.append("<div style='font-family:Arial,Helvetica,sans-serif;color:#0f172a;background:#f8fafc;padding:14px;box-sizing:border-box;'>");
		html.append("<div style='background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%);border-radius:20px;padding:20px;color:white;box-shadow:0 18px 45px rgba(15,23,42,.22);'>");
		html.append("<div style='font-size:24px;font-weight:900;margin-bottom:8px;'>Dasbor Aktivitas Pengguna</div>");
		html.append("<div style='font-size:13px;line-height:1.6;color:#dbeafe;max-width:980px;'>Aktivitas pengguna diringkas agar perubahan, pembacaan, dan pengelolaan data lebih mudah dipantau. Tab Data Excel tetap menyimpan tampilan laporan lama dan tombol download.</div>");
		html.append("</div>");
		html.append("<div style='display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:12px;margin-top:14px;'>");
		appendDashboardCard(html, "Filter", "Atur rentang tanggal dan pengguna pada tab Data Excel.");
		appendDashboardCard(html, "Tabel Ringkas", "Data utama disajikan dalam laporan yang bisa dibaca langsung.");
		appendDashboardCard(html, "Download", "File Excel asli tetap tersedia dari toolbar laporan.");
		appendDashboardCard(html, "Pemantauan", "Gunakan ringkasan ini untuk melihat perubahan besar tanpa membuka semua baris.");
		html.append("</div>");
		html.append("<div style='display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-top:14px;'>");
		html.append("<div style='background:white;border:1px solid #e2e8f0;border-radius:18px;padding:16px;box-shadow:0 12px 28px rgba(15,23,42,.06);'>");
		html.append("<div style='font-size:15px;font-weight:900;margin-bottom:8px;'>Alur Penggunaan</div>");
		html.append("<div style='font-size:12px;color:#64748b;line-height:1.7;'>1. Pilih filter pada tab Data Excel. 2. Klik Proses atau Refresh. 3. Periksa tabel ringkas. 4. Download Excel jika membutuhkan file laporan lengkap.</div>");
		html.append("</div>");
		html.append("<div style='background:white;border:1px solid #e2e8f0;border-radius:18px;padding:16px;box-shadow:0 12px 28px rgba(15,23,42,.06);'>");
		html.append("<div style='font-size:15px;font-weight:900;margin-bottom:8px;'>Catatan</div>");
		html.append("<div style='font-size:12px;color:#64748b;line-height:1.7;'>Tampilan lama tidak dihapus. Spreadsheet tetap dipertahankan agar format unduhan dan perhitungan lama tetap sama.</div>");
		html.append("</div>");
		html.append("</div>");
		html.append("</div>");
		return html.toString();
	}

	private void appendDashboardCard(StringBuilder html, String title, String detail) {
		html.append("<div style='background:white;border:1px solid #e2e8f0;border-radius:18px;padding:16px;box-shadow:0 12px 28px rgba(15,23,42,.06);'>");
		html.append("<div style='font-size:13px;font-weight:900;color:#1d4ed8;margin-bottom:8px;'>").append(title).append("</div>");
		html.append("<div style='font-size:12px;color:#64748b;line-height:1.5;'>").append(detail).append("</div>");
		html.append("</div>");
	}


	private void initFakultas() {

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Tabbox dashboardTabbox = new Tabbox();
		dashboardTabbox.setParent(Common.tampilanScroll(this));
		dashboardTabbox.setWidth("100%");
		dashboardTabbox.setHeight("100%");

		Tabs dashboardTabs = new Tabs();
		dashboardTabs.setParent(dashboardTabbox);

		Tab dashboardTab = new Tab("Dasbor");
		dashboardTab.setParent(dashboardTabs);
		dashboardTab.setSelected(true);

		Tab dataTab = new Tab("Data Excel");
		dataTab.setParent(dashboardTabs);

		Tabpanels dashboardTabpanels = new Tabpanels();
		dashboardTabpanels.setParent(dashboardTabbox);

		Tabpanel dashboardPanel = new ais.ui.util.MyTabpanel();
		dashboardPanel.setStyle("overflow:auto;background:#f8fafc;");
		dashboardPanel.setParent(dashboardTabpanels);

		Html dashboardInfo = new Html(buildDashboardIntroHtml());
		dashboardInfo.setParent(dashboardPanel);

		Tabpanel dataPanel = new ais.ui.util.MyTabpanel();
		dataPanel.setStyle("overflow:auto;background:#ffffff;");
		dataPanel.setParent(dashboardTabpanels);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(dataPanel);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		/* FIX 21-08-2026: tinggi panel filter kurang 52px sehingga baris toolbar
		 * (Proses/Download) terpotong di bagian bawah. Ditambah satu tinggi baris
		 * toolbar ZK. Autoscroll tetap aktif sebagai pengaman bila isi filter
		 * bertambah di kemudian hari. */
		north.setHeight("252px");
		north.setAutoscroll(true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setStyle("border:0px;background: transparent;");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");
		searchfakultas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai"));
		Calendar dateMulai = ais.ui.util.WaktuUtil.getCalendar();
		dateMulai.set(Calendar.MONTH, dateMulai.get(Calendar.MONTH) - 1);
		mulai.setValue(dateMulai.getTime());
		row.appendChild(mulai);
		mulai.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});
		mulai.setReadonly(true);

		MyComboitemConfig comboitem = new MyComboitemConfig("Tambah data baru");
		comboitem.setValue(CommonPrivilages.CREATE);
		searchjenisaktifitas.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Ubah data");
		comboitem.setValue(CommonPrivilages.UPDATE);
		searchjenisaktifitas.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Hapus data");
		comboitem.setValue(CommonPrivilages.DELETE);
		searchjenisaktifitas.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Baca data");
		comboitem.setValue(CommonPrivilages.READ);
		searchjenisaktifitas.appendChild(comboitem);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Aktifitas"));
		row.appendChild(searchjenisaktifitas);
		searchjenisaktifitas.setWidth("90%");
		searchjenisaktifitas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aktifitas"));
		row.appendChild(searchaktifitas);
		searchaktifitas.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");
		searchjurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Sampai"));
		sampai.setValue(ais.ui.util.WaktuUtil.getDate());
		row.appendChild(sampai);
		sampai.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});
		sampai.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Pengguna"));
		row.appendChild(searchnamapengguna);
		searchnamapengguna.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Data"));
		row.appendChild(searchdata);
		searchdata.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "8");
		row.setParent(rows);
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(row);
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Proses", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						initSpreadsheet();
					}
				});
			}
		});
		print.setParent(toolbar);

		ais.action.master.helper.LaporanKunjunganPenggunaHelper.pasangTombolLaporan(
				toolbar, this, mulai, sampai, searchfakultas, searchjurusan, null, null);

		print = new MyToolbarbuttonConfig("Download", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				spreadsheet.getBook().write(bout);
				bout.close();
				Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Rekap_status_mahasiswa.xlsx");
			}
		});
		print.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	private void initSpreadsheet() {

		Common.clear(center);

		Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());

		String dateMulai = Common.databaseDateFormat.get().format(mulai.getValue());

		String dateSelesai = Common.databaseDateFormat.get().format(sampai.getValue());

		Integer activityType = (Integer) (searchjenisaktifitas.getSelectedItem() == null ? null
				: searchjenisaktifitas.getSelectedItem().getValue());

		String namapengguna = searchnamapengguna.getValue().trim();

		Session session = HibernateUtil.currentSession();

		List<LogUserActifity> logUserActifities = session.createCriteria(LogUserActifity.class)
				.add(Restrictions.isNotNull("classCalled")).add(Restrictions.isNotNull("activityType"))
				.add(activityType == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("activityType", activityType))

				.addOrder(Order.desc("id")).createAlias("detailLogLogin", "detailLogLogin")
				.createAlias("detailLogLogin.logLogin", "logLogin")
				.createAlias("logLogin.mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("logLogin.dosen", "dosen", Criteria.LEFT_JOIN)
				.createAlias("logLogin.tbmuser", "tbmuser", Criteria.LEFT_JOIN)

				.add(searchdata.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("keterangan1", searchdata.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchaktifitas.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("keterangan", searchaktifitas.getValue().trim(), MatchMode.ANYWHERE))

				.add(namapengguna.equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.or(Restrictions.ilike("mahasiswa.nama", namapengguna, MatchMode.ANYWHERE),
										Restrictions.ilike("dosen.nama", namapengguna, MatchMode.ANYWHERE)),
								Restrictions.ilike("tbmuser.userNama", namapengguna, MatchMode.ANYWHERE)))

				.add(jurusan == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("logLogin.jurusan", jurusan))
				.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("logLogin.fakultas", fakultas))

				.add(Restrictions.sqlRestriction(
						"date(login) between date('" + dateMulai + "') and date('" + dateSelesai + "')"))

				.setMaxResults(50).list();

		if (logUserActifities.size() == 0) {
			return;
		}

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(5);
		spreadsheet.setMaxrows(logUserActifities.size() + 4);

		Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(40);
		ais.ui.util.EcampusUtil.setBold(sheet,
				new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

		ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0,
				"CATATAN AKTIFITAS PENGGUNA \n " + "" + Common.getBahasaConfig("Fakultas") + " "
						+ (fakultas == null ? "SEMUA" : fakultas.getNama().toUpperCase()) + "\n"
						+ Common.getBahasaConfig("Jurusan") + " "
						+ (jurusan == null ? "SEMUA" : jurusan.getNama().toUpperCase()) + "\n DARI TANGGAL " + dateMulai
						+ " S.D " + dateSelesai);
		final String color = "#000000";
		int rowIndex = 2;
		int colIndex = 0;
		Utils.setRowHeight(sheet, 1, 150);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
		Cell cell = Utils.getCell(sheet, 1, 0);
		cell.getCellStyle().setWrapText(true);
		cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);

		ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 0, 1, spreadsheet.getMaxcolumns() - 1, false);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Pengguna");
		Utils.setColumnWidth(sheet, 0, 200);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "Jenis Pengguna");
		Utils.setColumnWidth(sheet, 1, 120);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Aktifitas");
		Utils.setColumnWidth(sheet, 2, 400);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "Data");
		Utils.setColumnWidth(sheet, 3, 150);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "Waktu");
		Utils.setColumnWidth(sheet, 4, 140);

		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				true);

		rowIndex = 3;
		colIndex = 0;

		for (LogUserActifity objects : logUserActifities) {
			if (objects == null || objects.getDetailLogLogin() == null
					|| objects.getDetailLogLogin().getLogLogin() == null)
				continue;

			Mahasiswa mahasiswa = objects.getDetailLogLogin().getLogLogin().getMahasiswa();
			Dosen dosen = objects.getDetailLogLogin().getLogLogin().getDosen();
			Tbmuser tbmuser = objects.getDetailLogLogin().getLogLogin().getTbmuser();

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, mahasiswa != null ? mahasiswa.getNama()
					: dosen != null ? dosen.getNama() : tbmuser != null ? tbmuser.getUserNama() : "");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1,
					mahasiswa != null ? Common.getBahasa("label_mahasiswa")
							: dosen != null ? Common.getBahasa("label_dosen")
									: tbmuser != null ? tbmuser.hakAkses().getRoleName() : "");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, objects.getKeterangan());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, objects.getKeterangan1());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, Common.dateFormat3.get().format(objects.getWaktu()));

			rowIndex++;
		}

		Common.setStyled(sheet);spreadsheet.setMaxrows(rowIndex + 1);
		// Excel mentah -> grid ringan (Book tetap hidup utk tombol Download). Pola B PratinjauXlsxHelper.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

	}
}
