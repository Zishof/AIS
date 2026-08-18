package ais.action.master.feeder.integrator.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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
import org.zkoss.zul.Div;
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

import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DownloadNilaiTransfer extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Center center = new Center();
	private Combobox searchfakultas = new Combobox();

	private Combobox searchjurusan = new Combobox();
	private Intbox searchangkatan = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));
	private Combobox searchprogram = new Combobox();

//	protected Combobox searchTahunAjaran = new Combobox();
//	protected Combobox searchJenisSemester = new Combobox();
//	private Combobox searchsemester = new Combobox();

	private Textbox nimMahasiswa = new Textbox();
	private Textbox namaMahasiswa = new Textbox();

	private File file;

	private Textbox kodeMatakuliah;

	private Combobox searchstatus;

	public DownloadNilaiTransfer() {
		super();
		try {

			Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

//			searchJenisSemester.setReadonly(true);
//			searchTahunAjaran.setReadonly(true);

//			Common.generateTahunAjaranDanSemua(searchTahunAjaran);Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());
//			Common.selectComboItem(searchTahunAjaran, null);

			Common.initPrograms(searchprogram);

//			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
//			comboitem.setLabel(Perkuliahan.GANJIL);
//			comboitem.setValue(Perkuliahan.GANJIL);
//			searchJenisSemester.appendChild(comboitem);
//
//			comboitem = new MyComboitemConfig();
//			comboitem.setLabel(Perkuliahan.GENAP);
//			comboitem.setValue(Perkuliahan.GENAP);
//			searchJenisSemester.appendChild(comboitem);
//
//			comboitem = new MyComboitemConfig();
//			comboitem.setLabel("Semua");
//			comboitem.setValue(null);
//			searchJenisSemester.appendChild(comboitem);
//
//			if (Common.getKonfigurasi("pilihan_semester_di_perkuliahan_dibuat_default_semua_aja", Konfigurasi.AKTIF)
//					.getNilai().equals(Konfigurasi.AKTIF)) {
//				searchJenisSemester.setSelectedItem(comboitem);
//			} else {
//				Common.selectComboItem(searchJenisSemester,
//						Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
//			}

//			final EventListener eventListener = new EventListener() {
//
//				@Override
//				public void onEvent(Event arg0) throws Exception {
//					Common.clear(searchsemester);
//					searchsemester.setSelectedItem(null);
//
//					if (searchJenisSemester.getSelectedItem() == null) {
//						return;
//					}
//
//					if (searchJenisSemester.getSelectedItem().getValue() == null) {
//						org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
//						comboitem.setLabel("Semua");
//						comboitem.setValue(null);
//						searchsemester.appendChild(comboitem);
//						for (int i = 1; i < 30; i++) {
//							comboitem = new MyComboitemConfig();
//							comboitem.setLabel(i + "");
//							comboitem.setValue(i);
//							searchsemester.appendChild(comboitem);
//						}
//					} else {
//
//						Boolean genap = searchJenisSemester.getSelectedItem().getValue().equals(Perkuliahan.GENAP);
//						org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
//						comboitem.setLabel("Semua");
//						comboitem.setValue(null);
//						searchsemester.appendChild(comboitem);
//						if (genap) {
//							for (int i : Common.genap) {
//								if (i == 0)
//									continue;
//								comboitem = new MyComboitemConfig();
//								comboitem.setLabel(i + "");
//								comboitem.setValue(i);
//								searchsemester.appendChild(comboitem);
//							}
//						} else {
//							for (int i : Common.ganjil) {
//								comboitem = new MyComboitemConfig();
//								comboitem.setLabel(i + "");
//								comboitem.setValue(i);
//								searchsemester.appendChild(comboitem);
//							}
//						}
//					}
//
//					searchsemester.setSelectedIndex(0);
//					searchsemester.setReadonly(true);
//				}
//			};
//
//			searchJenisSemester.addEventListener("onChange", eventListener);
//			eventListener.onEvent(null);

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DownloadNilaiTransfer(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
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
		borderlayout.setHeight("2000px");
		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("160px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");

//		row.setParent(rows);
//		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
//		row.appendChild(searchTahunAjaran);
//		searchTahunAjaran.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(searchangkatan);
		searchangkatan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setReadonly(true);
		searchprogram.setWidth("90%");

		row = new MyFormRow();

//		row.setParent(rows);
//		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
//
//		Hbox hbox = new Hbox();
//		row.appendChild(hbox);
//		hbox.appendChild(searchJenisSemester);
//		hbox.appendChild(searchsemester);
//
//		searchJenisSemester.setCols(6);
//		searchsemester.setCols(2);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa"));
		Hbox hbox2 = new Hbox();
		row.appendChild(hbox2);
		hbox2.appendChild(nimMahasiswa);
		nimMahasiswa.setCols(8);
		hbox2.appendChild(namaMahasiswa);
		namaMahasiswa.setCols(8);

		row.appendChild(new ais.ui.util.MyLabelConfig("Status Mahasiswa"));
		row.appendChild(searchstatus = new Combobox());
		Common.insertComboDanSemua(searchstatus, "nama", "kodeEpsbed", StatusMahasiswa.class);
		searchstatus.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Matakuliah"));
		row.appendChild(kodeMatakuliah = new Textbox());
		kodeMatakuliah.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(new ais.ui.util.MyLabelConfig(""));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig search = new MyToolbarbuttonConfig("Tampilkan Data", "/img/svg/search.svg");
		search.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});
		search.setParent(toolbar);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Ambil Data", "/img/excel.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				try {
					Filedownload.save(new FileInputStream(file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "nilai_transfer.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/DownloadNilaiTransfer.java:302");

				}
			}
		});
		print.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
	}

	@SuppressWarnings({ "unchecked" })
	private void initSpreadsheet() throws Exception {

//		final Integer semester = searchsemester.getSelectedItem() == null ? null
//				: (Integer) searchsemester.getSelectedItem().getValue();

		Common.clear(center);

		System.out.println("init spreadsheet running");
		final Jurusan jurusan = searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: (Jurusan) searchjurusan.getSelectedItem().getValue();

		final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/data_nilai_"
				+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx");

		final StatusMahasiswa selectedStatusMahasiswa = (StatusMahasiswa) (searchstatus.getSelectedItem() == null
				|| searchstatus.getSelectedItem().getValue() == null ? null
						: searchstatus.getSelectedItem().getValue());

		(file = new File(filename)).createNewFile();

		final Intbox sizedata = new Intbox(30);
		final Label label = Common.displayLoadBar(this, file, center, sizedata);

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				XSSFWorkbook workbook = new XSSFWorkbook();

				XSSFSheet sheet = workbook.createSheet("NILAI Transfer");
				sheet.setDefaultColumnWidth(18);

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("NIM");
				rowhead.createCell(1).setCellValue("Nama Mahasiswa");
				rowhead.createCell(2).setCellValue("Kode MK Asal");
				rowhead.createCell(3).setCellValue("Nama Mata Kuliah Asal");
				rowhead.createCell(4).setCellValue("SKS Asal");
				rowhead.createCell(5).setCellValue("Nilai Huruf Asal");
				rowhead.createCell(6).setCellValue("Kode Matakuliah Diakui");
				rowhead.createCell(7).setCellValue("Nama Matakuliah Diakui");
				rowhead.createCell(8).setCellValue("Nilai Huruf Diakui");
				rowhead.createCell(9).setCellValue("Nilai Angka Diakui");
				rowhead.createCell(10).setCellValue("Kode Prodi");

				Session session = HibernateUtil.currentNativeSession();

				Criterion criteriaStatus = Restrictions.sqlRestriction("true");
				if (selectedStatusMahasiswa != null) {
					String sql = "this_.mahasiswa in (select mahasiswa from history_status_mahasiswa where status_mahasiswa="
							+ selectedStatusMahasiswa.getId() + " and tahunakademik = '"
							+ Common.getCurrentTahunAkademik() + "' and semester%2="
							+ (Common.isNowSemensterGanjil() ? 1 : 0) + ")";
					System.out.println("sql=>" + sql);
					criteriaStatus = Restrictions.sqlRestriction(sql);
				}

				List<Detailperkuliahan> detailperkuliahans = session.createCriteria(Detailperkuliahan.class)

						.add(criteriaStatus)

						.add(Restrictions.isNotNull("matakuliahKonversi"))

//						.add(searchTahunAjaran.getSelectedItem() == null
//								|| searchTahunAjaran.getSelectedItem().getValue() == null
//										? Restrictions.sqlRestriction("1=1")
//										: Restrictions.eq("tahunAkademik",
//												searchTahunAjaran.getSelectedItem().getValue()))
//
//						.add(searchJenisSemester.getSelectedItem() == null
//								|| searchJenisSemester.getSelectedItem().getValue() == null
//										? Restrictions.sqlRestriction("1=1")
//										: Restrictions.sqlRestriction("this_.semester % 2 = " + (searchJenisSemester
//												.getSelectedItem().getValue().equals(Perkuliahan.GANJIL) ? "1" : "0")))

						.createAlias("matakuliahKonversi", "matakuliahKonversi")

						.add(kodeMatakuliah.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("matakuliahKonversi.kode", kodeMatakuliah.getValue().trim(),
										MatchMode.ANYWHERE))

						.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))

//						.add(semester == null || semester.equals(-1) ? Restrictions.sqlRestriction("true")
//								: Restrictions.eq("semester", semester))

						.createAlias("mahasiswa", "mahasiswa")

						.add(Restrictions.and(
								nimMahasiswa.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.ilike("mahasiswa.nim", nimMahasiswa.getValue().trim(),
												MatchMode.ANYWHERE),

								namaMahasiswa.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.ilike("mahasiswa.nama", namaMahasiswa.getValue().trim(),
												MatchMode.ANYWHERE)))

						.add(searchjurusan.getSelectedItem() == null
								|| searchjurusan.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("mahasiswa.jurusan", jurusan))

						.createAlias("mahasiswa.jurusan", "jurusan", Criteria.LEFT_JOIN)

						.add(searchfakultas.getSelectedItem() == null
								|| searchfakultas.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

						.add(searchprogram.getSelectedItem() == null
								|| searchprogram.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("mahasiswa.program",
												searchprogram.getSelectedItem().getValue()))

						.add(searchangkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("mahasiswa.tahunangkatan", searchangkatan.getValue()))

						.addOrder(Order.asc("mahasiswa.nim")).addOrder(Order.asc("semester")).list();

				int size = detailperkuliahans.size();

				int rowIndex = 1;
				for (Detailperkuliahan detailperkuliahanLain : detailperkuliahans) {

					label.setValue("Sedang memproses data " + detailperkuliahanLain.toString() + " ("
							+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

					createData(session, sheet, rowIndex, detailperkuliahanLain);
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

				System.out.println("Your excel file has been generated! " );

				HibernateUtil.closeSession();

				detailperkuliahans.clear();
				label.setValue("");
							} catch (Exception e) {
								// FIX "hang selamanya": sebelumnya try besar ini TIDAK punya catch di level luar,
								// sehingga exception yang lolos akan menembus keluar Runnable.run() tanpa pernah
								// men-set label.setValue(""), membuat popup progres macet selamanya. Sekarang
								// ditangkap dan ditampilkan sebagai error.
								Common.tampilErrorJikaAdmin(e);
								label.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
										"pengambilan data Nilai Transfer dari Neo Feeder", null, e,
										new String[] {
												"Periksa kembali koneksi ke server Neo Feeder (Pengaturan Koneksi) dan coba ulangi.",
												"Pastikan data Perkuliahan/Nilai Transfer terkait sudah tersinkron dengan benar.",
												"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
										.replace("\n", " "));
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}

	public static void createData(Session session, XSSFSheet sheet, int rowIndex, Detailperkuliahan detailperkuliahan) {

		Matakuliah matakuliah = detailperkuliahan.getMatakuliahKonversi() == null
				? (detailperkuliahan.getPerkuliahan() == null ? null
						: detailperkuliahan.getPerkuliahan().getMatakuliah())
				: detailperkuliahan.getMatakuliahKonversi();
		Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
		if (matakuliah != null) {

			XSSFRow row = sheet.createRow(rowIndex);

			XSSFCell cell = row.createCell(0);
			cell.setCellValue(mahasiswa.getNim());

			cell = row.createCell(1);
			cell.setCellValue(mahasiswa.getNama());

			cell = row.createCell(2);
			cell.setCellValue(detailperkuliahan.getKodeMatakuliahAsal());

			cell = row.createCell(3);
			cell.setCellValue(detailperkuliahan.getNamaMatakuliahAsal());

			cell = row.createCell(4);
			cell.setCellValue(detailperkuliahan.getSksAsal());

			cell = row.createCell(5);
			cell.setCellValue(detailperkuliahan.getNilaiHurufAsal());

			cell = row.createCell(6);
			cell.setCellValue(matakuliah.getKode());

			cell = row.createCell(7);
			cell.setCellValue(matakuliah.getNama());

			cell = row.createCell(8);
			cell.setCellValue(detailperkuliahan.getNilaiHuruf());

			cell = row.createCell(9);
			cell.setCellValue(detailperkuliahan.getTotalIP());

			cell = row.createCell(10);
			cell.setCellValue(detailperkuliahan.getMatakuliahKonversi() != null
					? detailperkuliahan.getMatakuliahKonversi().getJurusan() == null ? ""
							: detailperkuliahan.getMatakuliahKonversi().getJurusan().getKodeEpsbed()
					: detailperkuliahan.getPerkuliahan().getJurusan().getKodeEpsbed());

		}
	}
}
