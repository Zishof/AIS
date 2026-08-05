package ais.action.master.dashboard.admin;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
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
import org.zkoss.zul.Hbox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DashboardRekapPengambilanKRSPerSmt extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox searchsemester = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Combobox angkatanMhs = new Combobox();
	private AmbilDataDosenBanbox searchDosen = new AmbilDataDosenBanbox();

	private MyCheckboxConfig tidaktermasukKonversi, konversiAja;

	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();

	private Combobox searchStatusAwalMahasiswa;

	private Combobox searchstatus;

	public DashboardRekapPengambilanKRSPerSmt() {
		super();
		try {
			init();
			initFakultas();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardRekapPengambilanKRSPerSmt(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			initFakultas();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
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

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("200px");
		north.setAutoscroll(true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
		row.appendChild(searchDosen);
		searchDosen.setWidth("90%");
		searchDosen.setWidth("90%");

		Hbox konversi = new Hbox();
		row.appendChild(konversi);
		konversi.appendChild(tidaktermasukKonversi = new MyCheckboxConfig("Bukan konversi"));
		konversi.appendChild(konversiAja = new MyCheckboxConfig("Hanya konversi"));
		konversiAja.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				tidaktermasukKonversi.setChecked(!konversiAja.isChecked());
				tidaktermasukKonversi.setVisible(!konversiAja.isChecked());

			}
		});
		tidaktermasukKonversi.setChecked(true);

		row = new MyFormRow();
		row.setParent(rows);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester ke"));
		row.appendChild(searchsemester);
		searchsemester.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(angkatanMhs);
		MyComboitemConfig comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		angkatanMhs.appendChild(comboitem);
		for (int i = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 10; i <= ais.ui.util.WaktuUtil
				.getCalendar().get(Calendar.YEAR) + 10; i++) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			angkatanMhs.appendChild(comboitem);
		}
		angkatanMhs.setSelectedIndex(0);
		angkatanMhs.setWidth("90%");
		angkatanMhs.setReadonly(true);

		Common.initPrograms(searchprogram);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");

		Hbox statusMhs = new Hbox();
		row.appendChild(statusMhs);
		statusMhs.appendChild(searchStatusAwalMahasiswa = new Combobox());
		Common.insertComboDanSemua(searchStatusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(searchStatusAwalMahasiswa, null);
		searchStatusAwalMahasiswa.setCols(4);
		searchStatusAwalMahasiswa.setReadonly(true);

		statusMhs.appendChild(searchstatus = new Combobox());
		searchstatus.setCols(4);
		searchstatus.setReadonly(true);
		Common.insertComboDanSemua(searchstatus, new String[] { "nama", "kodeEpsbed" }, StatusMahasiswa.class);
		Common.selectComboItem(searchstatus, null);

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(searchsemester);
				searchsemester.setSelectedItem(null);

				org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
				comboitem.setLabel("Semua");
				comboitem.setValue(null);
				searchsemester.appendChild(comboitem);
				for (int i = 1; i < 20; i++) {
					if (i == 0)
						continue;
					comboitem = new MyComboitemConfig();
					comboitem.setLabel(i + "");
					comboitem.setValue(i);
					searchsemester.appendChild(comboitem);
				}
				searchsemester.setSelectedIndex(0);
				searchsemester.setReadonly(true);
			}
		};

		eventListener.onEvent(null);

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
				initSpreadsheet();
			}
		});
		print.setParent(toolbar);

		print = new MyToolbarbuttonConfig("Download", "/img/print.png");
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

	@SuppressWarnings("unchecked")
	private void initSpreadsheet() {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(center);

				Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
						|| searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? null
								: searchfakultas.getSelectedItem().getValue());
				Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
						|| searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? null
								: searchjurusan.getSelectedItem().getValue());

				Integer semesterKe = (Integer) (searchsemester.getSelectedItem() == null ? null
						: searchsemester.getSelectedItem().getValue());
				String program = (String) (searchprogram.getSelectedItem() == null
						|| searchprogram.getSelectedItem().getValue() == null ? null
								: searchprogram.getSelectedItem().getValue());

				Integer angkatan = (Integer) (angkatanMhs.getSelectedItem() == null ? null
						: angkatanMhs.getSelectedItem().getValue());

				Dosen dosen = (Dosen) searchDosen.getAttribute("dosen");

				Session session = HibernateUtil.currentSession();

				StatusMahasiswa statusMahasiswa = (StatusMahasiswa) (searchstatus.getSelectedItem() == null
						|| searchstatus.getSelectedItem().getValue() == null ? null
								: searchstatus.getSelectedItem().getValue());
				StatusAwalMahasiswa statusAwalMahasiswa = (StatusAwalMahasiswa) (searchStatusAwalMahasiswa
						.getSelectedItem() == null ? null : searchStatusAwalMahasiswa.getSelectedItem().getValue());

				String sqlStatus = "";
				if (statusMahasiswa != null) {
					sqlStatus = " and a.id in (select mahasiswa from history_status_mahasiswa where status_mahasiswa="
							+ statusMahasiswa.getId() + " and tahunakademik = '" + Common.getCurrentTahunAkademik()
							+ "' and semester%2=" + (Common.isNowSemensterGanjil() ? 1 : 0) + ") ";
					System.out.println("sqlStatus=>" + sqlStatus);
				}

				String[] jenisSemester = new String[] { Perkuliahan.GANJIL, Perkuliahan.GENAP };
				List<String> tahunAkademiks = session.createCriteria(Perkuliahan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.setProjection(Projections.groupProperty("tahunAjaran"))
						.add(Restrictions.sqlRestriction("to_number(split_part(tahun_ajaran,'/',1),'9999')<="
								+ ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)))
						.add(Restrictions.sqlRestriction("to_number(split_part(tahun_ajaran,'/',1),'9999')>="
								+ (ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 7)))
						.add(Restrictions.isNotNull("tahunAjaran")).addOrder(Order.asc("tahunAjaran")).list();
				List<Object[]> jurusans = new ArrayList<Object[]>();
				int index = 0;
				String sql = "select  max(y.nama) as fakultas, max(x.nama) as jurusan,";

				for (@SuppressWarnings("unused")
				String tahunAkademik : tahunAkademiks) {
					for (@SuppressWarnings("unused")
					String semester : jenisSemester) {
						sql += "sum(case a.id when b" + index + ".mahasiswa then 1 else 0 end) as jumlah_sks_disetujui"
								+ index + ",  " + "sum(case a.id when c" + index
								+ ".mahasiswa then 1 else 0 end) as jumlah_sks_belum_disetujui" + index + ",  "
								+ "sum(case a.id when d" + index + ".mahasiswa then 1 else 0 end) as belum_ambil_krs"
								+ index + ", 1 as total" + index + ",  ";
						index++;
					}
				}
				sql += "1 as total  from mahasiswa a  ";
				index = 0;
				for (String tahunAkademik : tahunAkademiks) {
					for (String semester : jenisSemester) {
						sql += " left join (select aa.mahasiswa from detailperkuliahan as aa where aa.tahunakademik = '"
								+ tahunAkademik + "' "
								+ (tidaktermasukKonversi.isChecked() ? " and aa.perkuliahan is not null " : "")
								+ (konversiAja.isChecked()
										? " and aa.matakuliah_konversi is not null and aa.perkuliahan is null "
										: "")
								+ (semesterKe == null ? "" : "  and aa.semester = " + semesterKe) + " and aa.semester "
								+ ((semester.equals(Perkuliahan.GENAP) ? " % 2 = 0 " : " % 2 = 1 "))
								+ " group by aa.mahasiswa having min(aa.persetujuan) = 1) as b" + index
								+ " on (a.id = b" + index + ".mahasiswa)  "
								+ "left join (select aa.mahasiswa from detailperkuliahan as aa where aa.tahunakademik = '"
								+ tahunAkademik + "' "
								+ (tidaktermasukKonversi.isChecked() ? " and aa.perkuliahan is not null " : "")
								+ (konversiAja.isChecked()
										? " and aa.matakuliah_konversi is not null and aa.perkuliahan is null "
										: "")
								+ (semesterKe == null ? "" : "  and aa.semester = " + semesterKe) + " and aa.semester  "
								+ ((semester.equals(Perkuliahan.GENAP) ? " % 2 = 0 " : " % 2 = 1 "))
								+ " group by aa.mahasiswa  having max(aa.persetujuan) = 1 and min(aa.persetujuan) = 0 ) as c"
								+ index + " on (a.id = c" + index + ".mahasiswa)  "
								+ "left join (select aa.mahasiswa from detailperkuliahan as aa where aa.tahunakademik = '"
								+ tahunAkademik + "' "
								+ (tidaktermasukKonversi.isChecked() ? " and aa.perkuliahan is not null " : "")
								+ (konversiAja.isChecked()
										? " and aa.matakuliah_konversi is not null and aa.perkuliahan is null "
										: "")
								+ (semesterKe == null ? "" : "  and aa.semester = " + semesterKe) + "   "
								+ ((semester.equals(Perkuliahan.GENAP) ? " and aa.semester % 2 = 0 "
										: " and aa.semester % 2 = 1 "))
								+ " group by aa.mahasiswa  having max(aa.persetujuan) = 0) as d" + index
								+ " on (a.id = d" + index + ".mahasiswa)  ";

						index++;
					}
				}
				sql += " left join jurusan x on (a.jurusan = x.id  )  "
						+ "left join fakultas y on (y.id = x.fakultas)  where 1=1 "
						+ (dosen == null ? "" : " and a.dosen = " + dosen.getId())
						+ (angkatan == null ? "" : " and  a.tahunangkatan = " + angkatan)
						+ (program == null ? "" : " and a.program = '" + program + "'")
						+ (jurusan == null ? "" : " and a.jurusan = " + jurusan.getId())
						+ (fakultas == null ? "" : " and x.fakultas = " + fakultas.getId()) + sqlStatus
						+ (statusAwalMahasiswa != null
								? " and a.status_awal_mahasiswa = " + statusAwalMahasiswa.getId() + " "
								: "")
						+ " group by x.fakultas,a.jurusan  order by max(y.nama), max(x.nama) ";

				System.out.println(sql);
				jurusans = Common.ambilSql(sql);

				Common.clear(center);
				// Center 1 anak -> Vlayout: panel CHART (status KRS per periode) di atas,
				// Spreadsheet (tabel + sumber Download Excel) tetap di bawah.
				org.zkoss.zul.Vlayout centerBox = new org.zkoss.zul.Vlayout();
				centerBox.setWidth("100%"); centerBox.setHeight("100%"); centerBox.setStyle("overflow:auto;");
				centerBox.setParent(center);
				final org.zkoss.zul.Html chartPanel = new org.zkoss.zul.Html();
				chartPanel.setParent(centerBox);
				spreadsheet = new ais.ui.util.MySpreadsheet();
				spreadsheet.setParent(centerBox);
				spreadsheet.setWidth("100%");
				spreadsheet.setVflex("1");
				spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
				spreadsheet.setMaxcolumns((tahunAkademiks.size() * 4 * 2) + 2);
				spreadsheet.setMaxrows(jurusans.size() + 4);

				Worksheet sheet = spreadsheet.getSelectedSheet();
				sheet.setDefaultColumnWidth(40);
				ais.ui.util.EcampusUtil.setBold(sheet,
						new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

				ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0,
						"REKAPITULASI PENGAMBILAN KRS MAHASISWA \n " + Common.getBahasaConfig("Fakultas") + " "
								+ (fakultas == null ? "SEMUA" : fakultas.getNama().toUpperCase()) + "\n"
								+ Common.getBahasaConfig("Jurusan") + " "
								+ (jurusan == null ? "SEMUA" : jurusan.getNama().toUpperCase()) + "\nPROGRAM "
								+ (program == null ? "SEMUA" : program.toUpperCase()) + "\nANGKATAN "
								+ (angkatan == null ? "SEMUA" : angkatan) + "\n DOSEN "
								+ (dosen == null ? "SEMUA" : dosen.getNama()));
				final String color = "#000000";
				int rowIndex = 2;
				int colIndex = 0;
				Utils.setRowHeight(sheet, 1, 150);
				ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
				Cell cell = Utils.getCell(sheet, 1, 0);
				cell.getCellStyle().setWrapText(true);
				cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);

				ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 0, 1, spreadsheet.getMaxcolumns() - 1, false);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Fakultas");
				Utils.setColumnWidth(sheet, 0, 200);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "Jurusan");
				Utils.setColumnWidth(sheet, 1, 200);

				colIndex = 2;
				for (String tahunAkademik : tahunAkademiks) {
					for (String semester : jenisSemester) {
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, tahunAkademik + "/" + semester);
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 1,
								tahunAkademik + "/" + semester);
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 2,
								tahunAkademik + "/" + semester);

						ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex, colIndex, rowIndex, colIndex + 3, false);

						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex, "Disetujui");
						Utils.setColumnWidth(sheet, colIndex, 70);
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + 1, "Sebagian Disetujui");
						Utils.setColumnWidth(sheet, colIndex + 1, 70);
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + 2, "Belum Disetujui");
						Utils.setColumnWidth(sheet, colIndex + 2, 70);
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + 3, "Total");
						Utils.setColumnWidth(sheet, colIndex + 3, 70);

						colIndex += 4;
					}
				}

				ais.ui.util.EcampusUtil.setBorder(sheet,
						new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex + 1),
						BookHelper.BORDER_FULL, BorderStyle.THIN, color);
				ais.ui.util.EcampusUtil.setBold(sheet,
						new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex + 1), true);

				rowIndex = 4;
				colIndex = 0;

				String namaFakultas = "";
				String namaProdi = "";
				Integer[] totalDisetujui = new Integer[tahunAkademiks.size() * 2];
				Integer[] totalBelumDisetujui = new Integer[tahunAkademiks.size() * 2];
				Integer[] totalBelumAmbil = new Integer[tahunAkademiks.size() * 2];
				Integer[] totalSemua = new Integer[tahunAkademiks.size() * 2];
				for (Object[] objects : jurusans) {
					if (objects[0] == null)
						continue;
					if (!namaFakultas.equals(objects[0].toString())) {
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, objects[0].toString());
						namaFakultas = objects[0].toString();
					} else {
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "");
					}

					if (!namaProdi.equals(objects[1].toString())) {
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, objects[1].toString());
						namaProdi = objects[1].toString();
					} else {
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "");
					}
					colIndex = 2;
					int cindex = 0;
					for (@SuppressWarnings("unused")
					String tahunAkademik : tahunAkademiks) {
						for (@SuppressWarnings("unused")
						String semester : jenisSemester) {
							Integer nilai0 = ((Number) (objects[colIndex] == null ? 0 : objects[colIndex])).intValue();
							Integer nilai1 = ((Number) (objects[colIndex + 1] == null ? 0 : objects[colIndex + 1]))
									.intValue();
							Integer nilai2 = ((Number) (objects[colIndex + 2] == null ? 0 : objects[colIndex + 2]))
									.intValue();

							Integer nilai3 = nilai0 + nilai1 + nilai2;

							if (totalDisetujui[cindex] == null) {
								totalDisetujui[cindex] = 0;
							}
							if (totalBelumDisetujui[cindex] == null) {
								totalBelumDisetujui[cindex] = 0;
							}
							if (totalBelumAmbil[cindex] == null) {
								totalBelumAmbil[cindex] = 0;
							}
							if (totalSemua[cindex] == null) {
								totalSemua[cindex] = 0;
							}

							totalDisetujui[cindex] += nilai0;
							totalBelumDisetujui[cindex] += nilai1;
							totalBelumAmbil[cindex] += nilai2;
							totalSemua[cindex] += nilai3;

							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, nilai0);
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 1, nilai1);
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 2, nilai2);
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 3, nilai3);

							colIndex += 4;
							cindex++;

						}
					}

					try {
						ais.ui.util.EcampusUtil.setBorder(sheet,
								new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
								BookHelper.BORDER_FULL, BorderStyle.THIN, color);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardRekapPengambilanKRSPerSmt.java:536");
					}

					rowIndex++;
				}

				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "TOTAL");

				int cindex = 0;
				colIndex = 2;
				for (@SuppressWarnings("unused")
				String tahunAkademik : tahunAkademiks) {
					for (@SuppressWarnings("unused")
					String semester : jenisSemester) {
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, totalDisetujui[cindex]);
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 1,
								totalBelumDisetujui[cindex]);
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 2, totalBelumAmbil[cindex]);
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 3, totalSemua[cindex]);

						if (totalSemua[cindex] == 0) {
							Utils.setColumnWidth(sheet, colIndex, 0);
							Utils.setColumnWidth(sheet, colIndex + 1, 0);
							Utils.setColumnWidth(sheet, colIndex + 2, 0);
							Utils.setColumnWidth(sheet, colIndex + 3, 0);
						}

						cindex++;
						colIndex += 4;

					}
				}

				Common.setStyled(sheet);
				spreadsheet.setMaxrows(rowIndex + 1);
				// Excel mentah -> grid ringan (Book tetap hidup utk tombol Download). Pola B PratinjauXlsxHelper.
				ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);
				// -- Panel visual: STACKED BAR status pengambilan KRS per periode (HTML/CSS, BUKAN JFreeChart) --
				try {
					java.util.List<String> periodeLabel = new java.util.ArrayList<String>();
					for (String ta : tahunAkademiks) { for (String sm : jenisSemester) { periodeLabel.add(ta + "/" + sm); } }
					int nP = totalDisetujui.length;
					double[][] vals = new double[nP][3];
					for (int i = 0; i < nP; i++) {
						vals[i][0] = totalDisetujui[i] == null ? 0 : totalDisetujui[i];
						vals[i][1] = totalBelumDisetujui[i] == null ? 0 : totalBelumDisetujui[i];
						vals[i][2] = totalBelumAmbil[i] == null ? 0 : totalBelumAmbil[i];
					}
					chartPanel.setContent("<div style='padding:10px 12px 2px;'>"
							+ ais.ui.util.HtmlChartHelper.stackedBar(
									"Status Pengambilan KRS per Periode",
									"Melihat berapa KRS yang sudah disetujui, sebagian disetujui, dan belum disetujui pada tiap tahun/semester.",
									periodeLabel.toArray(new String[0]),
									new String[] { "Disetujui", "Sebagian Disetujui", "Belum Disetujui" },
									vals,
									new String[] { "#22c55e", "#f59e0b", "#ef4444" })
							+ "</div>");
				} catch (Exception eChart) { ais.common.ErrorAuditUtil.record(eChart, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardRekapPengambilanKRSPerSmt.java:593");
					// chart hanya pelengkap; kegagalannya tidak boleh mengganggu tabel/Excel.
				}

			}
		});

	}
}
