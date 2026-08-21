package ais.action.master.dashboard.admin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
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
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DashboardMaksimakKrsMahasiswa extends MyWindow {

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
	private Combobox angkatanMhsMulai = new Combobox();private Combobox angkatanMhs = new Combobox();
	private AmbilDataDosenBanbox searchDosen = new AmbilDataDosenBanbox();
	private MyCheckboxConfig tidaktermasukKonversi, konversiAja;
	private Center center = new Center();

	private File file;

	public DashboardMaksimakKrsMahasiswa() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardMaksimakKrsMahasiswa(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
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
		// searchfakultas.addEventListener("onChange", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// initSpreadsheet();
		// }
		// });

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");
		// searchjurusan.addEventListener("onChange", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// initSpreadsheet();
		// }
		// });

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);
		// tahunAkademik.addEventListener("onChange", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// initSpreadsheet();
		// }
		// });

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
		row.appendChild(searchDosen);
		searchDosen.setWidth("90%");
		searchDosen.setWidth("90%");
		// searchDosen.setEventListener(new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// initSpreadsheet();
		// }
		// });

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

		Hbox konversi = new Hbox();
		row.appendChild(konversi);
		konversi.appendChild(tidaktermasukKonversi = new MyCheckboxConfig("Bukan konversi"));
		konversi.appendChild(konversiAja = new MyCheckboxConfig("Hanya konversi"));
		konversiAja.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				tidaktermasukKonversi.setChecked(!konversiAja.isChecked());
				tidaktermasukKonversi.setVisible(!konversiAja.isChecked());
				initSpreadsheet();
			}
		});
		tidaktermasukKonversi.setChecked(true);

		semesterAbsensi.addEventListener("onChange", eventListener);
		eventListener.onEvent(null);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "10");
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

		print = new MyToolbarbuttonConfig("Hapus kelebihan SKS matakuliah yang diambil mahasiswa", "/img/svg/trash.svg");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				MyMessageboxConfig.show(
						"Apakah yakin ingin menghapus kelebihan SKS matakuliah yang telah diambil mahasiswa ?",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									Common.createDefaultTimer(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {

											hapusSpreadsheet();

										}
									});
								}

							}
						});

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
					Filedownload.save(new FileInputStream(file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "KRS_MAKSIMAL_SKS.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardMaksimakKrsMahasiswa.java:346");

				}
			}
		});
		print.setParent(toolbar);

		// Common.createDefaultTimer(new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// initSpreadsheet();
		// }
		// });

	}

	@SuppressWarnings({ "unchecked" })
	private void hapusSpreadsheet() throws Exception {
		final String tahunAkademik = (String) (DashboardMaksimakKrsMahasiswa.this.tahunAkademik
				.getSelectedItem() == null ? null
						: DashboardMaksimakKrsMahasiswa.this.tahunAkademik.getSelectedItem().getValue());
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
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());

		final Integer angkatan = (Integer) (angkatanMhs.getSelectedItem() == null ? null
				: angkatanMhs.getSelectedItem().getValue());
		final Dosen dosen = (Dosen) searchDosen.getAttribute("dosen");

		if (tahunAkademik == null) {
			return;
		}

		Session session = HibernateUtil.currentNativeSession();

		String sql = " (select a.mahasiswa from (select aa.mahasiswa,min(aa.persetujuan) as persetujuan "
				+ " from detailperkuliahan as aa where aa.tahunakademik = '" + tahunAkademik + "'  "
				+ (tidaktermasukKonversi.isChecked() ? " and aa.perkuliahan is not null " : "")
				+ (konversiAja.isChecked() ? " and aa.matakuliah_konversi is not null and aa.perkuliahan is null " : "")
				+ (semesterKe == null ? "" : " and aa.semester = " + semesterKe) + " and aa.semester "
				+ ((semester.equals(Perkuliahan.GENAP) ? " % 2 = 0 " : " % 2 = 1 "))

				+ " group by aa.mahasiswa) a where a.persetujuan = 1) ";

		System.out.println("sql = " + sql);

		List<Mahasiswa> mahasiswas = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.sqlRestriction("this_.id in " + sql))

				.add(dosen != null ? Restrictions.eq("dosen", dosen.getId()) : Restrictions.sqlRestriction("1=1"))

				.createAlias("jurusan", "jurusan").addOrder(Order.asc("jurusan")).addOrder(Order.desc("tahunangkatan"))
				.addOrder(Order.asc("nim"))

				.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jurusan.fakultas", fakultas))

				.add(jurusan == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("jurusan", jurusan))

				.add(program == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("program", program))

				.add(angkatan == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("tahunangkatan", angkatan))

				.list();

		final List<KrsMahasiswa> krsMahasiswas = mahasiswas.isEmpty() ? new ArrayList<KrsMahasiswa>()
				: session.createCriteria(KrsMahasiswa.class).add(Restrictions.in("mahasiswa", mahasiswas))
						.add(Restrictions.lt("selisih", 0)).list();

		HibernateUtil.closeSession();

		final Label label = Common.displayLoadBar(this, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				MyMessageboxConfig.show(
						"Penghapusan kelebihan SKS matakuliah yang telah diambil mahasiswa berhasil dilakukan. Click kembali tombol proses untuk melihat maksimal SKS yang diambil oleh mahasiswa saat ini.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {
				int rowIndex = 1;
				for (KrsMahasiswa krsMahasiswa : krsMahasiswas) {

					label.setValue("Sedang memproses data " + krsMahasiswa.toString() + " ("
							+ Common.numberFormat.get().format(rowIndex * 100.0 / krsMahasiswas.size()) + " %)");

					Common.hapusMatakuliahYangMelebihiKetentuan(krsMahasiswa.getMahasiswa(), krsMahasiswa.getSemester(),
							krsMahasiswa.getTahapan(), krsMahasiswa.getSemesterPendek(),
							krsMahasiswa.getSksYangDiambil());
					rowIndex++;
				}

				label.setValue("");
			}
		}).start();

	}

	@SuppressWarnings({ "unchecked" })
	private void initSpreadsheet() throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(center);
				final String tahunAkademik = (String) (DashboardMaksimakKrsMahasiswa.this.tahunAkademik
						.getSelectedItem() == null ? null
								: DashboardMaksimakKrsMahasiswa.this.tahunAkademik.getSelectedItem().getValue());
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
						|| searchprogram.getSelectedItem().getValue() == null ? null
								: searchprogram.getSelectedItem().getValue());

								final Integer angkatan = (Integer) (angkatanMhs.getSelectedItem() == null ? null
						: angkatanMhs.getSelectedItem().getValue());

				final Integer angkatanMulai = (Integer) (angkatanMhsMulai.getSelectedItem() == null ? null
						: angkatanMhsMulai.getSelectedItem().getValue());
				final Dosen dosen = (Dosen) searchDosen.getAttribute("dosen");

				if (tahunAkademik == null) {
					return;
				}

				final int tahun = Integer.parseInt(StringUtils.split(tahunAkademik, "/")[0]);

				System.out.println("init spreadsheet running => tahun = " + tahun);

				final String filename = Sessions.getCurrent().getWebApp()
						.getRealPath("/tmp/data_"
								+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
								+ ".xlsx");

				(file = new File(filename)).createNewFile();

				final Intbox sizedata = new Intbox(30);
				final Label label = Common.displayLoadBar(DashboardMaksimakKrsMahasiswa.this, file, center, sizedata);

				new Thread(new Runnable() {

					@Override
					public void run() {
						try {

						XSSFWorkbook workbook = new XSSFWorkbook();

						XSSFSheet sheet = workbook.createSheet("MAX_KRS_MAHASISWA");
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
						rowhead.createCell(8).setCellValue("MAKS. SKS");
						rowhead.createCell(9).setCellValue("JML. SKS DIAMBIL");
						rowhead.createCell(10).setCellValue("SELISIH");
						rowhead.createCell(11).setCellValue("IP TERAKHIR");

						Session session = HibernateUtil.currentNativeSession();

						String sql = " (select a.mahasiswa from "
								+ "(select aa.mahasiswa,min(aa.persetujuan) as persetujuan "
								+ " from detailperkuliahan as aa where aa.tahunakademik = '" + tahunAkademik + "' "
								+ (tidaktermasukKonversi.isChecked() ? " and aa.perkuliahan is not null " : "")
								+ (konversiAja.isChecked()
										? " and aa.matakuliah_konversi is not null and aa.perkuliahan is null "
										: "")
								+ (semesterKe == null ? "" : " and aa.semester = " + semesterKe) + "  and aa.semester "
								+ ((semester.equals(Perkuliahan.GENAP) ? " % 2 = 0 " : " % 2 = 1 "))

								+ " group by aa.mahasiswa) a where a.persetujuan = 1) ";

						System.out.println("sql = " + sql);

						List<Mahasiswa> mahasiswas = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.sqlRestriction("this_.id in " + sql))

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

								.list();

						int size = mahasiswas.size();

						int rowIndex = 1;
						for (Mahasiswa mahasiswa : mahasiswas) {
							label.setValue("Sedang memproses data " + mahasiswa.toString() + " ("
									+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

							XSSFRow row = sheet.createRow(rowIndex);
							XSSFCell cell = row.createCell(0);
							cell.setCellValue(mahasiswa.getNim());

							cell = row.createCell(1);
							cell.setCellValue(mahasiswa.getNama());

							cell = row.createCell(2);
							cell.setCellValue(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama());

							cell = row.createCell(3);
							cell.setCellValue(mahasiswa.getTahunangkatan());

							cell = row.createCell(4);
							cell.setCellValue(tahunAkademik);

							Integer currentSemester = Common.getSemester(mahasiswa.getTahunangkatan(), semester,
									mahasiswa.getPindahKeKampusIniMasukSemester(), tahun, mahasiswa.getSemesterMulai());

							cell = row.createCell(5);
							cell.setCellValue(currentSemester);

							KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, currentSemester, null,
									null);

							Double[] batas = Common.getMinDanMaxIPK(mahasiswa, currentSemester,
									krsMahasiswa.getSemesterPendek());
							Integer maxsks = batas[0].intValue();
							// Double minip = batas[1];
							Double iplast = batas[2];
							int selisih = maxsks - krsMahasiswa.getSksYangDiambil();

							cell = row.createCell(6);
							cell.setCellValue(krsMahasiswa.getKelas());

							cell = row.createCell(7);
							cell.setCellValue(
									krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNama());

							cell = row.createCell(8);
							cell.setCellValue(maxsks);

							cell = row.createCell(9);
							cell.setCellValue(krsMahasiswa.getSksYangDiambil());

							cell = row.createCell(10);
							cell.setCellValue(selisih);

							cell = row.createCell(11);
							cell.setCellValue(iplast.equals(0.5) ? 0.0 : iplast);

							rowIndex++;
						}

						Common.setStyled(sheet);sizedata.setValue(rowIndex + 1);

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
