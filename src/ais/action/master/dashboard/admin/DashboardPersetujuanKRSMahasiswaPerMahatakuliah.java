package ais.action.master.dashboard.admin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Komponen dashboard khusus untuk dashboard persetujuan krs mahasiswa per mahatakuliah. Kelas ini
 * memilih variasi data atau tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan
 * dari kelas induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox searchfakultas}, {@code
 * Combobox searchjurusan}, {@code Combobox tahunAkademik}, {@code Combobox semesterAbsensi}, {@code Combobox
 * searchsemester}, {@code Combobox searchprogram}, {@code Combobox angkatanMhsMulai}, {@code Combobox
 * angkatanMhs}; inisialisasi/lifecycle ({@code initFakultas()}, {@code init()}, {@code initSpreadsheet()}).
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DashboardPersetujuanKRSMahasiswaPerMahatakuliah extends MyWindow {

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
	private Combobox angkatanMhsMulai = new Combobox();
	private Combobox angkatanMhs = new Combobox();
	private AmbilDataDosenBanbox searchDosen = new AmbilDataDosenBanbox();
	private MyCheckboxConfig tidaktermasukKonversi, konversiAja;
	private Center center = new Center();

	private File file;
	private Combobox searchStatusAwalMahasiswa;
	private Combobox searchstatus;
	private Textbox searchmk;

	public DashboardPersetujuanKRSMahasiswaPerMahatakuliah() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardPersetujuanKRSMahasiswaPerMahatakuliah(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void initFakultas() {

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {

		initFakultas();

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));

		Hbox hbox = new Hbox();
		hbox.setParent(row);
		semesterAbsensi = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semesterAbsensi.appendChild(comboitem);
		semesterAbsensi.setSelectedIndex(1);
		hbox.appendChild(semesterAbsensi);
		semesterAbsensi.setCols(3);
		semesterAbsensi.setReadonly(true);

		Common.selectComboItem(semesterAbsensi, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		hbox.appendChild(searchsemester);
		searchsemester.setCols(2);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Matakuliah"));
		row.appendChild(searchmk = new Textbox());
		searchmk.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));

		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(angkatanMhsMulai);
		hbox.appendChild(angkatanMhs);
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

			comboitem = new MyComboitemConfig();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			angkatanMhsMulai.appendChild(comboitem);
		}
		Common.selectComboItem(angkatanMhsMulai, ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 5);
		Common.selectComboItem(angkatanMhs, ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));
		hbox.setWidth("90%");
		angkatanMhs.setReadonly(true);
		angkatanMhsMulai.setCols(5);
		angkatanMhs.setCols(5);

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

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "9");
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

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		print = new MyToolbarbuttonConfig("Download", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					Filedownload.save(new FileInputStream(file),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
							"KRS_SUDAH_DISETUJUI_SEMUA.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardPersetujuanKRSMahasiswaPerMahatakuliah.java:327");

				}
			}
		});
		print.setParent(toolbar);

	}

	@SuppressWarnings({ })
	private void initSpreadsheet() throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(center);
				final String tahunAkademik = (String) (DashboardPersetujuanKRSMahasiswaPerMahatakuliah.this.tahunAkademik
						.getSelectedItem() == null ? null
								: DashboardPersetujuanKRSMahasiswaPerMahatakuliah.this.tahunAkademik.getSelectedItem()
										.getValue());
				final String semester = (String) (semesterAbsensi.getSelectedItem() == null
						|| semesterAbsensi.getSelectedItem().getValue() == null ? null
								: semesterAbsensi.getSelectedItem().getValue());

				final Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
						|| searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? null
								: searchfakultas.getSelectedItem().getValue());
				final Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
						|| searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? null
								: searchjurusan.getSelectedItem().getValue());

				final Integer semesterKe = (Integer) (searchsemester.getSelectedItem() == null ? null
						: searchsemester.getSelectedItem().getValue());
				final String program = (String) (searchprogram.getSelectedItem() == null
						|| searchprogram.getSelectedItem().getValue() == null
						|| searchprogram.getSelectedItem().getValue() == null ? null
								: searchprogram.getSelectedItem().getValue());

				final Integer angkatan = (Integer) (angkatanMhs.getSelectedItem() == null ? null
						: angkatanMhs.getSelectedItem().getValue());

				final Integer angkatanMulai = (Integer) (angkatanMhsMulai.getSelectedItem() == null ? null
						: angkatanMhsMulai.getSelectedItem().getValue());
				final Dosen dosen = (Dosen) searchDosen.getAttribute("dosen");

				final String mk = searchmk.getValue().trim();

				if (tahunAkademik == null) {
					return;
				}

				final StatusMahasiswa statusMahasiswa = (StatusMahasiswa) (searchstatus.getSelectedItem() == null
						|| searchstatus.getSelectedItem().getValue() == null ? null
								: searchstatus.getSelectedItem().getValue());
				final StatusAwalMahasiswa statusAwalMahasiswa = (StatusAwalMahasiswa) (searchStatusAwalMahasiswa
						.getSelectedItem() == null ? null : searchStatusAwalMahasiswa.getSelectedItem().getValue());

				final int tahun = Integer.parseInt(StringUtils.split(tahunAkademik, "/")[0]);

				System.out.println("init spreadsheet running => tahun = " + tahun);

				final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/data_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");

				(file = new File(filename)).createNewFile();

				final Intbox sizedata = new Intbox(30);
				final Label label = Common.displayLoadBar(DashboardPersetujuanKRSMahasiswaPerMahatakuliah.this, file,
						center, sizedata);

				new Thread(new Runnable() {

					@Override
					public void run() {
						try {

						XSSFWorkbook workbook = new XSSFWorkbook();

						XSSFSheet sheet = workbook.createSheet("KRS_MAHASISWA");
						sheet.setDefaultColumnWidth(25);

						XSSFRow rowhead = sheet.createRow((short) 0);

						rowhead.createCell(0).setCellValue("NIM MAHASISWA");
						rowhead.createCell(1).setCellValue("NAMA MAHASISWA");
						rowhead.createCell(2).setCellValue("PROGRAM STUDI");
						rowhead.createCell(3).setCellValue("ANGKATAN");
						rowhead.createCell(4).setCellValue("TAHUN AKADEMIK");
						rowhead.createCell(5).setCellValue("SEMESTER");
						rowhead.createCell(6).setCellValue("KELAS");
						rowhead.createCell(7).setCellValue("DOSEN PA");

						rowhead.createCell(8).setCellValue("KODE MK");
						rowhead.createCell(9).setCellValue("NAMA MK");
						rowhead.createCell(10).setCellValue("JUMLAH SKS");
						rowhead.createCell(11).setCellValue("STATUS PERSETUJUAN");
						rowhead.createCell(12).setCellValue("DOSEN PENGAJAR");

						Session session = HibernateUtil.currentNativeSession();

						List<Mahasiswa> mahasiswas = ConstantValues.simpleList(session.createCriteria(Mahasiswa.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

								.add(statusAwalMahasiswa == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("statusAwalMahasiswa", statusAwalMahasiswa))

								.add(dosen != null ? Restrictions.eq("dosen", dosen.getId())
										: Restrictions.sqlRestriction("1=1"))

								.createAlias("jurusan", "jurusan").addOrder(Order.asc("jurusan"))
								.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"))

								.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("jurusan.fakultas", fakultas))

								.add(jurusan == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("jurusan", jurusan))

								.add(program == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("program", program))

								.add(angkatan == null && angkatanMulai == null ? Restrictions.sqlRestriction("1=1") :

										angkatan == null && angkatanMulai != null
												? Restrictions.ge("tahunangkatan", angkatanMulai)
												:

												angkatan != null && angkatanMulai == null
														? Restrictions.le("tahunangkatan", angkatan)

														: Restrictions.between("tahunangkatan", angkatanMulai,
																angkatan))

								, Mahasiswa.class);

						int size = mahasiswas.size();

						int rowIndex = 1;
						int rowIndexMk = 1;
						for (Mahasiswa mahasiswa : mahasiswas) {

							Integer currentSemester = Common.getSemester(mahasiswa.getTahunangkatan(), semester,
									mahasiswa.getPindahKeKampusIniMasukSemester(), tahun, mahasiswa.getSemesterMulai());

							if (semesterKe != null && !currentSemester.equals(semesterKe)) {
								continue;
							}
							KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, currentSemester, null,
									null);
							HistoryStatusMahasiswa historyStatusMahasiswa = Common
									.getHistoryStatusMahasiswa(krsMahasiswa);

							if (statusMahasiswa == null || (historyStatusMahasiswa != null
									&& historyStatusMahasiswa.getStatusMahasiswa() != null && historyStatusMahasiswa
											.getStatusMahasiswa().getId().equals(statusMahasiswa.getId()))) {

								label.setValue("Sedang memproses data " + mahasiswa.toString() + " ("
										+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

								List<Object[]> mksBelum = Common.dataMkKrs(mahasiswa, semesterKe, semester, null,
										mk.trim(), false, konversiAja.isChecked(), tidaktermasukKonversi.isChecked(),
										true);
								for (Object[] o : mksBelum) {
									String kode = org.apache.commons.lang3.StringUtils
											.replace(o[0] == null ? "" : o[0].toString(), "|", "");
									String nama = org.apache.commons.lang3.StringUtils
											.replace(o[1] == null ? "" : o[1].toString(), "|", "");

									Integer sks = o[6] == null ? 0 : Integer.parseInt(o[6].toString());

									XSSFRow row = sheet.createRow(rowIndexMk);
									XSSFCell cell = row.createCell(0);
									cell.setCellValue(mahasiswa.getNim());

									cell = row.createCell(1);
									cell.setCellValue(mahasiswa.getNama());

									cell = row.createCell(2);
									cell.setCellValue(
											mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama());

									cell = row.createCell(3);
									cell.setCellValue(mahasiswa.getTahunangkatan());

									cell = row.createCell(4);
									cell.setCellValue(tahunAkademik);

									cell = row.createCell(5);
									cell.setCellValue(currentSemester);

									cell = row.createCell(6);
									cell.setCellValue(krsMahasiswa.getKelas());

									cell = row.createCell(7);
									cell.setCellValue(krsMahasiswa.getDosenPa() == null ? ""
											: krsMahasiswa.getDosenPa().getNama());

									cell = row.createCell(8);
									cell.setCellValue(kode);

									cell = row.createCell(9);
									cell.setCellValue(nama);

									cell = row.createCell(10);
									cell.setCellValue(sks);

									cell = row.createCell(11);
									cell.setCellValue("BELUM DISETUJUI");

									try {
										Long perkuliahan = o[7] == null ? 0 : Long.parseLong(o[7].toString());
										if (perkuliahan != null) {
											Perkuliahan perkuliahan2 = (Perkuliahan) session
													.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
													.add(Restrictions.idEq(perkuliahan)).uniqueResult();
											if (perkuliahan2 != null) {
												cell = row.createCell(12);
												String dsn = "";
												for (Dosen d : perkuliahan2.populateDosenBuNama()) {
													dsn += dsn.isEmpty() ? d.getNama() : ", " + d.getNama();
												}
												cell.setCellValue(dsn);
												perkuliahan2 = null;
											}
										}
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardPersetujuanKRSMahasiswaPerMahatakuliah.java:557");
										// TODO: handle exception
									}

									rowIndexMk++;
								}

								List<Object[]> mksTelah = Common.dataMkKrs(mahasiswa, semesterKe, semester, null,
										mk.trim(), true, konversiAja.isChecked(), tidaktermasukKonversi.isChecked(),
										true);
								for (Object[] o : mksTelah) {
									String kode = org.apache.commons.lang3.StringUtils
											.replace(o[0] == null ? "" : o[0].toString(), "|", "");
									String nama = org.apache.commons.lang3.StringUtils
											.replace(o[1] == null ? "" : o[1].toString(), "|", "");

									Integer sks = o[6] == null ? 0 : Integer.parseInt(o[6].toString());

									XSSFRow row = sheet.createRow(rowIndexMk);
									XSSFCell cell = row.createCell(0);
									cell.setCellValue(mahasiswa.getNim());

									cell = row.createCell(1);
									cell.setCellValue(mahasiswa.getNama());

									cell = row.createCell(2);
									cell.setCellValue(
											mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama());

									cell = row.createCell(3);
									cell.setCellValue(mahasiswa.getTahunangkatan());

									cell = row.createCell(4);
									cell.setCellValue(tahunAkademik);

									cell = row.createCell(5);
									cell.setCellValue(currentSemester);

									cell = row.createCell(6);
									cell.setCellValue(krsMahasiswa.getKelas());

									cell = row.createCell(7);
									cell.setCellValue(krsMahasiswa.getDosenPa() == null ? ""
											: krsMahasiswa.getDosenPa().getNama());

									cell = row.createCell(8);
									cell.setCellValue(kode);

									cell = row.createCell(9);
									cell.setCellValue(nama);

									cell = row.createCell(10);
									cell.setCellValue(sks);

									cell = row.createCell(11);
									cell.setCellValue("DISETUJUI");

									try {
										Long perkuliahan = o[7] == null ? 0 : Long.parseLong(o[7].toString());
										if (perkuliahan != null) {
											Perkuliahan perkuliahan2 = (Perkuliahan) session
													.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
													.add(Restrictions.idEq(perkuliahan)).uniqueResult();
											if (perkuliahan2 != null) {
												cell = row.createCell(12);
												String dsn = "";
												for (Dosen d : perkuliahan2.populateDosenBuNama()) {
													dsn += dsn.isEmpty() ? d.getNama() : ", " + d.getNama();
												}
												cell.setCellValue(dsn);
												perkuliahan2 = null;
											}
										}
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardPersetujuanKRSMahasiswaPerMahatakuliah.java:630");
										// TODO: handle exception
									}

									rowIndexMk++;
								}

								if (mksBelum.isEmpty() && mksTelah.isEmpty()) {
									XSSFRow row = sheet.createRow(rowIndexMk);
									XSSFCell cell = row.createCell(0);
									cell.setCellValue(mahasiswa.getNim());

									cell = row.createCell(1);
									cell.setCellValue(mahasiswa.getNama());

									cell = row.createCell(2);
									cell.setCellValue(
											mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama());

									cell = row.createCell(3);
									cell.setCellValue(mahasiswa.getTahunangkatan());

									cell = row.createCell(4);
									cell.setCellValue(tahunAkademik);

									cell = row.createCell(5);
									cell.setCellValue(currentSemester);

									cell = row.createCell(6);
									cell.setCellValue(krsMahasiswa.getKelas());

									cell = row.createCell(7);
									cell.setCellValue(krsMahasiswa.getDosenPa() == null ? ""
											: krsMahasiswa.getDosenPa().getNama());

									cell = row.createCell(8);
									cell.setCellValue("");

									cell = row.createCell(9);
									cell.setCellValue("");

									cell = row.createCell(10);
									cell.setCellValue("");

									cell = row.createCell(11);
									cell.setCellValue("BELUM AMBIL KRS");

									cell = row.createCell(12);
									cell.setCellValue("");

									rowIndexMk++;
								}

								rowIndex++;
							}

						}

						sizedata.setValue(rowIndexMk + 1);

						try {
							FileOutputStream fileOut = new FileOutputStream(filename);
							workbook.write(fileOut);
							fileOut.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							Common.tampilErrorJikaAdmin(e);
						}

						HibernateUtil.closeSession();

						mahasiswas.clear();
						label.setValue("");
											} finally {
							ais.database.hibernate.HibernateUtil.closeSession();
						}
					}
				}).start();
			}
		});
	}

}
