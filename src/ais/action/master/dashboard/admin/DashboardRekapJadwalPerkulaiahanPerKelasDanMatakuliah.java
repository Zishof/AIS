package ais.action.master.dashboard.admin;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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

public class DashboardRekapJadwalPerkulaiahanPerKelasDanMatakuliah extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private Combobox searchsemester = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Combobox angkatanMhs = new Combobox();
	private AmbilDataDosenBanbox searchDosen = new AmbilDataDosenBanbox();

	private MyCheckboxConfig tidaktermasukKonversi, konversiAja;

	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();

	private Combobox searchStatusAwalMahasiswa;

	private Combobox searchstatus;

	public DashboardRekapJadwalPerkulaiahanPerKelasDanMatakuliah() {
		super();
		try {
			init();
			initFakultas();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardRekapJadwalPerkulaiahanPerKelasDanMatakuliah(String title, String border, boolean closable) {
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		semesterAbsensi = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semesterAbsensi.appendChild(comboitem);
		semesterAbsensi.setSelectedIndex(1);
		row.appendChild(semesterAbsensi);
		semesterAbsensi.setWidth("90%");
		semesterAbsensi.setReadonly(true);

		Common.selectComboItem(semesterAbsensi, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester ke"));
		row.appendChild(searchsemester);
		searchsemester.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(angkatanMhs);
		comboitem = new MyComboitemConfig();
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

				if (semesterAbsensi.getSelectedItem() == null) {
					return;
				}
				Boolean genap = semesterAbsensi.getSelectedItem().getValue().equals(Perkuliahan.GENAP);
				org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
				comboitem.setLabel("Semua");
				comboitem.setValue(null);
				searchsemester.appendChild(comboitem);
				if (genap) {
					for (int i : Common.genap) {
						if (i == 0)
							continue;
						comboitem = new MyComboitemConfig();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemester.appendChild(comboitem);
					}
				} else {
					for (int i : Common.ganjil) {
						comboitem = new MyComboitemConfig();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemester.appendChild(comboitem);
					}
				}

				searchsemester.setSelectedIndex(0);
				searchsemester.setReadonly(true);
			}
		};
		semesterAbsensi.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				eventListener.onEvent(arg0);

			}
		});

		eventListener.onEvent(null);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "10");
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

	private void initSpreadsheet() {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(center);
				String tahunAkademik = (String) (DashboardRekapJadwalPerkulaiahanPerKelasDanMatakuliah.this.tahunAkademik
						.getSelectedItem() == null ? null
								: DashboardRekapJadwalPerkulaiahanPerKelasDanMatakuliah.this.tahunAkademik
										.getSelectedItem().getValue());
				String semester = (String) (semesterAbsensi.getSelectedItem() == null
						|| semesterAbsensi.getSelectedItem().getValue() == null ? null
								: semesterAbsensi.getSelectedItem().getValue());

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

				if (tahunAkademik == null) {
					return;
				}

				List<Object[]> jurusans = new ArrayList<Object[]>();

				StatusMahasiswa statusMahasiswa = (StatusMahasiswa) (searchstatus.getSelectedItem() == null
						|| searchstatus.getSelectedItem().getValue() == null ? null
								: searchstatus.getSelectedItem().getValue());
				StatusAwalMahasiswa statusAwalMahasiswa = (StatusAwalMahasiswa) (searchStatusAwalMahasiswa
						.getSelectedItem() == null ? null : searchStatusAwalMahasiswa.getSelectedItem().getValue());

				String sqlStatus = "";
				if (statusMahasiswa != null) {
					sqlStatus = " and a.id in (select mahasiswa from history_status_mahasiswa where status_mahasiswa="
							+ statusMahasiswa.getId() + " and tahunakademik = '" + tahunAkademik + "' and ganjil_genap='"
							+ semester.replace("'", "''") + "') ";
					System.out.println("sqlStatus=>" + sqlStatus);
				}

				String sql = "select max(y.nama) as fakultas, max(x.nama) as jurusan, a.kelas as kelas, "
						+ "max(matkul.nama_matakuliah) as matakuliah, "
						+ "sum(case a.id when b.mahasiswa then 1 else 0 end) as jumlah_sks_disetujui, "
						+ "sum(case a.id when c.mahasiswa then 1 else 0 end) as jumlah_sks_belum_disetujui, "
						+ "sum(case a.id when d.mahasiswa then 1 else 0 end) as belum_ambil_krs, count(a.id) as total "
						+ "from mahasiswa a " + " left join "
						+ " (select bb.matakuliah,aa.mahasiswa,max(m.kode||' '||m.nama) as nama_matakuliah "
						+ " from detailperkuliahan as aa  inner join perkuliahan bb on (aa.perkuliahan=bb.id) "
						+ " inner join matakuliah m on (bb.matakuliah = m.id)  where aa.tahunakademik = '"
						+ tahunAkademik + "' "
						+ (tidaktermasukKonversi.isChecked() ? " and aa.perkuliahan is not null " : "")
						+ (konversiAja.isChecked()
								? " and aa.matakuliah_konversi is not null and aa.perkuliahan is null "
								: "")
						+ (semesterKe == null ? "" : " and aa.semester = " + semesterKe) + "  and aa.semester "
						+ ((semester.equals(Perkuliahan.GENAP) ? " % 2 = 0 " : " % 2 = 1 "))

						+ "  group by bb.matakuliah,aa.mahasiswa) as matkul on (a.id = matkul.mahasiswa) "
						+ "left join  (select bb.matakuliah,aa.mahasiswa  from detailperkuliahan as aa "
						+ " inner join perkuliahan bb on (aa.perkuliahan=bb.id)  where aa.tahunakademik = '"
						+ tahunAkademik + "' "
						+ (tidaktermasukKonversi.isChecked() ? " and aa.perkuliahan is not null " : "")
						+ (konversiAja.isChecked()
								? " and aa.matakuliah_konversi is not null and aa.perkuliahan is null "
								: "")
						+ (semesterKe == null ? "" : " and aa.semester = " + semesterKe) + "  and aa.semester "
						+ ((semester.equals(Perkuliahan.GENAP) ? " % 2 = 0 " : " % 2 = 1 "))

						+ "  group by bb.matakuliah,aa.mahasiswa "
						+ " having min(aa.persetujuan) = 1) as b on (a.id = b.mahasiswa and matkul.matakuliah=b.matakuliah) "
						+ "left join  (select bb.matakuliah,aa.mahasiswa  from detailperkuliahan as aa "
						+ " inner join perkuliahan bb on (aa.perkuliahan=bb.id)  where aa.tahunakademik = '"
						+ tahunAkademik + "' "
						+ (tidaktermasukKonversi.isChecked() ? " and aa.perkuliahan is not null " : "")
						+ (konversiAja.isChecked()
								? " and aa.matakuliah_konversi is not null and aa.perkuliahan is null "
								: "")
						+ (semesterKe == null ? "" : " and aa.semester = " + semesterKe) + "  and aa.semester "
						+ ((semester.equals(Perkuliahan.GENAP) ? " % 2 = 0 " : " % 2 = 1 "))

						+ "  group by bb.matakuliah,aa.mahasiswa "
						+ " having max(aa.persetujuan) = 1 and min(aa.persetujuan) = 0 ) as c on (a.id = c.mahasiswa and matkul.matakuliah=c.matakuliah) "
						+ "left join  (select bb.matakuliah,aa.mahasiswa  from detailperkuliahan as aa "
						+ " inner join perkuliahan bb on (aa.perkuliahan=bb.id)  where aa.tahunakademik = '"
						+ tahunAkademik + "' "
						+ (tidaktermasukKonversi.isChecked() ? " and aa.perkuliahan is not null " : "")
						+ (konversiAja.isChecked()
								? " and aa.matakuliah_konversi is not null and aa.perkuliahan is null "
								: "")
						+ (semesterKe == null ? "" : " and aa.semester = " + semesterKe) + "  and aa.semester "
						+ ((semester.equals(Perkuliahan.GENAP) ? " % 2 = 0 " : " % 2 = 1 "))

						+ "  group by bb.matakuliah,aa.mahasiswa "
						+ " having max(aa.persetujuan) = 0) as d on (a.id = d.mahasiswa and matkul.matakuliah=d.matakuliah) "
						+ "left join jurusan x on (a.jurusan = x.id ) left join fakultas y on (y.id = x.fakultas) "
						+ "where 1=1 " + (dosen == null ? "" : " and a.dosen = " + dosen.getId())
						+ (angkatan == null ? "" : " and a.tahunangkatan = " + angkatan)
						+ (program == null ? "" : " and a.program = '" + program + "'")
						+ (jurusan == null ? "" : " and a.jurusan = " + jurusan.getId())
						+ (fakultas == null ? "" : " and x.fakultas = " + fakultas.getId()) + sqlStatus
						+ (statusAwalMahasiswa != null
								? " and a.status_awal_mahasiswa = " + statusAwalMahasiswa.getId() + " "
								: "")
						+ " and matkul.matakuliah is not null "
						+ "group by x.fakultas,a.jurusan,a.kelas, matkul.matakuliah "
						+ "order by max(y.nama), max(x.nama), a.kelas,max(matkul.nama_matakuliah)";

				System.out.println(sql);
				jurusans = Common.ambilSql(sql);

				spreadsheet = new ais.ui.util.MySpreadsheet();
				Common.clear(center);
				spreadsheet.setParent(center);
				spreadsheet.setWidth("100%");
				spreadsheet.setHeight("100%");
				spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
				spreadsheet.setMaxcolumns(8);
				spreadsheet.setMaxrows(jurusans.size() + 4);

				Worksheet sheet = spreadsheet.getSelectedSheet();
				sheet.setDefaultColumnWidth(40);
				ais.ui.util.EcampusUtil.setBold(sheet,
						new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

				ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0,
						"REKAPITULASI PENGAMBILAN KRS MAHASISWA PER KELAS DAN MATAKULIAH \n "
								+ Common.getBahasaConfig("Fakultas") + " "
								+ (fakultas == null ? "SEMUA" : fakultas.getNama().toUpperCase()) + "\n"
								+ Common.getBahasaConfig("Jurusan") + " "
								+ (jurusan == null ? "SEMUA" : jurusan.getNama().toUpperCase()) + "\n TAHUN AKADEMIK "
								+ tahunAkademik + "\nPROGRAM " + (program == null ? "SEMUA" : program.toUpperCase())
								+ "\n SEMESTER " + semester.toUpperCase() + "\n ANGKATAN "
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
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Kelas");
				Utils.setColumnWidth(sheet, 2, 200);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "Matakuliah");
				Utils.setColumnWidth(sheet, 3, 200);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "Disetujui");
				Utils.setColumnWidth(sheet, 4, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "Sebagian Disetujui");
				Utils.setColumnWidth(sheet, 5, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "Belum Disetujui");
				Utils.setColumnWidth(sheet, 6, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, "Total");
				Utils.setColumnWidth(sheet, 7, 80);

				ais.ui.util.EcampusUtil.setBorder(sheet,
						new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
						BorderStyle.THIN, color);
				ais.ui.util.EcampusUtil.setBold(sheet,
						new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);

				rowIndex = 3;
				colIndex = 0;

				String namaFakultas = "";
				String namaProdi = "";
				Integer totalDisetujui = 0;
				Integer totalBelumDisetujui = 0;
				Integer totalBelumAmbil = 0;
				Integer totalSemua = 0;
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

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2,
							objects[2] == null ? "" : objects[2].toString());
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, objects[3].toString());

					Integer nilai0 = ((Number) (objects[4] == null ? 0 : objects[4])).intValue();
					Integer nilai1 = ((Number) (objects[5] == null ? 0 : objects[5])).intValue();
					Integer nilai2 = ((Number) (objects[6] == null ? 0 : objects[6])).intValue();

					Integer nilai3 = nilai0 + nilai1 + nilai2;

					totalDisetujui += nilai0;
					totalBelumDisetujui += nilai1;
					totalBelumAmbil += nilai2;
					totalSemua += nilai3;

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, nilai0);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, nilai1);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, nilai2);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, nilai3);

					try {
						ais.ui.util.EcampusUtil.setBorder(sheet,
								new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
								BookHelper.BORDER_FULL, BorderStyle.THIN, color);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardRekapJadwalPerkulaiahanPerKelasDanMatakuliah.java:537");
					}

					rowIndex++;
				}

				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "TOTAL");
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, totalDisetujui);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, totalBelumDisetujui);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, totalBelumAmbil);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, totalSemua);

				Common.setStyled(sheet);
				spreadsheet.setMaxrows(rowIndex + 1);
				// Excel mentah -> grid ringan (Book tetap hidup utk tombol Download). Pola B PratinjauXlsxHelper.
				ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

				colIndex = 0;
				try {

					ais.ui.util.EcampusUtil.setBorder(sheet,
							new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
							BookHelper.BORDER_FULL, BorderStyle.THIN, color);
					ais.ui.util.EcampusUtil.setBold(sheet,
							new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);

					ais.ui.util.EcampusUtil.setBold(sheet,
							new Rect(spreadsheet.getMaxcolumns() - 1, 3, spreadsheet.getMaxcolumns() - 1, rowIndex),
							true);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardRekapJadwalPerkulaiahanPerKelasDanMatakuliah.java:566");
				}
			}
		});
	}
}
