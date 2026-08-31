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
import org.hibernate.criterion.MatchMode;
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
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataKurikulumBanbox;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Kurikulum;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Komponen dashboard khusus untuk dashboard status krs mahasiswa. Kelas ini memilih variasi data
 * atau tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox searchfakultas}, {@code
 * Combobox searchjurusan}, {@code MyTextbox namaMahasiswa}, {@code MyTextbox searchMatakuliah}, {@code Combobox
 * searchcocok}, {@code Combobox searchprogram}, {@code Combobox angkatanMhsMulai}, {@code Combobox angkatanMhs};
 * inisialisasi/lifecycle ({@code initFakultas()}, {@code init()}, {@code initSpreadsheet()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DashboardStatusKRSMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private MyTextbox namaMahasiswa = new MyTextbox();
	private MyTextbox searchMatakuliah = new MyTextbox();
	private Combobox searchcocok = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Combobox angkatanMhsMulai = new Combobox();
	private Combobox angkatanMhs = new Combobox();
	private AmbilDataDosenBanbox searchDosen = new AmbilDataDosenBanbox();
	private MyCheckboxConfig hitungUlang = new MyCheckboxConfig("Hitung Ulang IPK");
	private Center center = new Center();

	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();

	private File file;
	private Combobox searchStatusAwalMahasiswa;
	private Combobox searchstatus;
	private AmbilDataKurikulumBanbox searchKurikulum;

	public DashboardStatusKRSMahasiswa() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardStatusKRSMahasiswa(String title, String border, boolean closable) {
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
		north.setHeight("292px");
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

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama/NIM"));
		row.appendChild(namaMahasiswa);
		namaMahasiswa.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
		row.appendChild(searchDosen);
		searchDosen.setWidth("90%");
		searchDosen.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Kurikulum *"));
		row.appendChild(searchKurikulum = new AmbilDataKurikulumBanbox());
		searchKurikulum.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Matakuliah"));
		row.appendChild(searchMatakuliah);
		searchMatakuliah.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kecocokan Berdasar"));
		row.appendChild(searchcocok);
		searchcocok.setWidth("90%");
		searchcocok.setReadonly(true);
		MyComboitemConfig comboitemConfig = new MyComboitemConfig("Nama Matakuliah");
		comboitemConfig.setValue("Nama Matakuliah");
		searchcocok.appendChild(comboitemConfig);

		comboitemConfig = new MyComboitemConfig("Kode Matakuliah");
		comboitemConfig.setValue("Kode Matakuliah");
		searchcocok.appendChild(comboitemConfig);

		comboitemConfig = new MyComboitemConfig("Kode dan Nama Matakuliah");
		comboitemConfig.setValue("Kode dan Nama Matakuliah");
		searchcocok.appendChild(comboitemConfig);
		searchcocok.setSelectedIndex(0);

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

		row.appendChild(new ais.ui.util.MyLabelConfig("Status"));

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
		toolbar.appendChild(hitungUlang);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		print = new MyToolbarbuttonConfig("Download", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					Filedownload.save(new FileInputStream(file),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "STATUS_KRS.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardStatusKRSMahasiswa.java:300");

				}
			}
		});
		print.setParent(toolbar);

	}

	@SuppressWarnings({ "unchecked" })
	private void initSpreadsheet() throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(center);
				final String nama = namaMahasiswa.getValue().trim();
				final String mk = searchMatakuliah.getValue().trim();
				final String cocok = searchcocok.getSelectedItem().getValue().toString();

				final String tahunAkademik = (String) (DashboardStatusKRSMahasiswa.this.tahunAkademik
						.getSelectedItem() == null ? null
								: DashboardStatusKRSMahasiswa.this.tahunAkademik.getSelectedItem().getValue());
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

				final String program = (String) (searchprogram.getSelectedItem() == null
						|| searchprogram.getSelectedItem().getValue() == null ? null
								: searchprogram.getSelectedItem().getValue());

				final Integer angkatan = (Integer) (angkatanMhs.getSelectedItem() == null ? null
						: angkatanMhs.getSelectedItem().getValue());

				final Integer angkatanMulai = (Integer) (angkatanMhsMulai.getSelectedItem() == null ? null
						: angkatanMhsMulai.getSelectedItem().getValue());
				final Dosen dosen = (Dosen) searchDosen.getAttribute("dosen");
				final int tahun = Integer.parseInt(StringUtils.split(tahunAkademik, "/")[0]);
				final StatusMahasiswa statusMahasiswa = (StatusMahasiswa) (searchstatus.getSelectedItem() == null
						|| searchstatus.getSelectedItem().getValue() == null ? null
								: searchstatus.getSelectedItem().getValue());
				final StatusAwalMahasiswa statusAwalMahasiswa = (StatusAwalMahasiswa) (searchStatusAwalMahasiswa
						.getSelectedItem() == null ? null : searchStatusAwalMahasiswa.getSelectedItem().getValue());

				final Kurikulum kurikulum = (Kurikulum) searchKurikulum.getAttribute("kurikulum");
				if (kurikulum == null) {
					MyMessageboxConfig.show("Mohon maaf, Kurikulum belum dipilih. Langkah yang dapat dilakukan: (1) buka kembali filter pencarian; (2) pilih Kurikulum dari daftar yang tersedia; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/data_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");

				(file = new File(filename)).createNewFile();

				final Intbox sizedata = new Intbox(30);
				final Label label = Common.displayLoadBar(DashboardStatusKRSMahasiswa.this, file, center, sizedata);

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
						rowhead.createCell(4).setCellValue("PROGRAM");
						rowhead.createCell(5).setCellValue("SEMESTER");
						rowhead.createCell(6).setCellValue("KELAS");
						rowhead.createCell(7).setCellValue("DOSEN PA");
						rowhead.createCell(8).setCellValue("IPK");
						rowhead.createCell(9).setCellValue("SKS");

						Session session = HibernateUtil.currentNativeSession();

						List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs = session
								.createCriteria(KurikulumPunyaMatakuliah.class).createAlias("matakuliah", "matakuliah")
								.add(mk.trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.or(Restrictions.ilike("matakuliah.kode", mk, MatchMode.ANYWHERE),
												Restrictions.ilike("matakuliah.nama", mk, MatchMode.ANYWHERE)))
								.add(Restrictions.eq("kurikulum", kurikulum)).addOrder(Order.asc("semester"))
								.addOrder(Order.asc("matakuliah.nama")).list();
						int index = 10;
						for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {
							if (kurikulumPunyaMatakuliah != null && kurikulumPunyaMatakuliah.getMatakuliah() != null) {
								rowhead.createCell(index)
										.setCellValue(kurikulumPunyaMatakuliah.getMatakuliah().getKode() + "-"
												+ kurikulumPunyaMatakuliah.getMatakuliah().getNama());
							}
							index++;
						}

						rowhead.createCell(index).setCellValue("Total");

						List<Mahasiswa> mahasiswas = ConstantValues.simpleList(session.createCriteria(Mahasiswa.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(nama.trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.or(Restrictions.ilike("nim", nama, MatchMode.ANYWHERE),
												Restrictions.ilike("nama", nama, MatchMode.ANYWHERE)))

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
						for (Mahasiswa mahasiswa : mahasiswas) {

							Integer currentSemester = Common.getSemester(mahasiswa.getTahunangkatan(), semester,
									mahasiswa.getPindahKeKampusIniMasukSemester(), tahun, mahasiswa.getSemesterMulai());
							KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, currentSemester, null,
									null, hitungUlang.isChecked());
							HistoryStatusMahasiswa historyStatusMahasiswa = Common
									.getHistoryStatusMahasiswa(krsMahasiswa);

							if (statusMahasiswa == null || (historyStatusMahasiswa != null
									&& historyStatusMahasiswa.getStatusMahasiswa() != null && historyStatusMahasiswa
											.getStatusMahasiswa().getId().equals(statusMahasiswa.getId()))) {
								label.setValue("Sedang memproses data " + mahasiswa.toString() + " ("
										+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");
								XSSFRow row = sheet.createRow(rowIndex);
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
								cell.setCellValue(mahasiswa.getProgram());

								cell = row.createCell(5);
								cell.setCellValue(currentSemester);

								cell = row.createCell(6);
								cell.setCellValue(krsMahasiswa.getKelas());

								cell = row.createCell(7);
								cell.setCellValue(
										krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNama());

								cell = row.createCell(8);
								cell.setCellValue(krsMahasiswa.getIpk());

								cell = row.createCell(9);
								cell.setCellValue(krsMahasiswa.getSksk());

								String subQuery = "trim(lower(case when d.nama is null then b.nama else d.nama end)) as mk \r\n";
								if (cocok.equals("Kode Matakuliah")) {
									subQuery = "trim(lower(case when d.kode is null then b.kode else d.nama end)) as mk \r\n";
								} else if (cocok.equals("Kode dan Nama Matakuliah")) {
									subQuery = "trim(lower(case when d.kode is null then b.kode||'-'||b.nama else d.nama||'-'||d.nama end)) as mk \r\n";
								}

								List<String> datMk = session
										.createSQLQuery("select \r\n" + subQuery + "from detailperkuliahan a\r\n"
												+ "left join matakuliah b on (a.matakuliah_konversi=b.id)\r\n"
												+ "left join perkuliahan c on (a.perkuliahan=c.id)\r\n"
												+ "left join matakuliah d on (c.matakuliah=d.id)\r\n"
												+ "where a.persetujuan=1 and a.mahasiswa=" + mahasiswa.getId())
										.list();

								int total = 0;
								index = 10;
								for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {

									int count = 0;
									if (kurikulumPunyaMatakuliah != null
											&& kurikulumPunyaMatakuliah.getMatakuliah() != null) {
										if (cocok.equals("Kode Matakuliah")) {
											count = datMk.contains(kurikulumPunyaMatakuliah.getMatakuliah().getKode()
													.toLowerCase().trim()) ? 1 : 0;
										} else if (cocok.equals("Nama Matakuliah")) {
											count = datMk.contains(kurikulumPunyaMatakuliah.getMatakuliah().getNama()
													.toLowerCase().trim()) ? 1 : 0;
										} else if (cocok.equals("Kode dan Nama Matakuliah")) {
											count = datMk.contains(kurikulumPunyaMatakuliah.getMatakuliah().getKode()
													.toLowerCase().trim() + "-"
													+ kurikulumPunyaMatakuliah.getMatakuliah().getNama().toLowerCase()
															.trim()) ? 1 : 0;
										}
									}

									total += count;
									cell = row.createCell(index);
									cell.setCellValue(count);
									index++;
								}

								cell = row.createCell(index);
								cell.setCellValue(total);

								rowIndex++;
							}
						}

						Common.setStyled(sheet);
						sizedata.setValue(rowIndex + 1);

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
