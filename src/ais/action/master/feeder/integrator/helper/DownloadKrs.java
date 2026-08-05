package ais.action.master.feeder.integrator.helper;

import java.awt.Color;
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
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
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

import ais.action.master.helper.AmbilDataMasaPerkuliahanBanbox;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.MasaPerkuliahan;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DownloadKrs extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Center center = new Center();
	private Combobox searchfakultas = new Combobox();

	private Combobox searchjurusan = new Combobox();
	private Intbox searchangkatan = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));
	private Combobox searchprogram = new Combobox();
	protected AmbilDataMasaPerkuliahanBanbox searchmasaperkulaiahan;
	protected Combobox searchTahunAjaran = new Combobox();
	protected Combobox searchJenisSemester = new Combobox();
	private Combobox searchsemester = new Combobox();

	private Textbox nimMahasiswa = new Textbox();
	private Textbox namaMahasiswa = new Textbox();
	private Textbox kelas = new Textbox();

	private File file;

	private Textbox kodeMatakuliah;

	private Combobox searchstatus;

	public DownloadKrs() {
		super();
		try {

			Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

			searchJenisSemester.setReadonly(true);
			searchTahunAjaran.setReadonly(true);

			Common.generateTahunAjaranDanSemua(searchTahunAjaran);
			Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());

			Common.initPrograms(searchprogram);

			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(Perkuliahan.GANJIL);
			comboitem.setValue(Perkuliahan.GANJIL);
			searchJenisSemester.appendChild(comboitem);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel(Perkuliahan.GENAP);
			comboitem.setValue(Perkuliahan.GENAP);
			searchJenisSemester.appendChild(comboitem);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Semua");
			comboitem.setValue(null);
			searchJenisSemester.appendChild(comboitem);

			searchJenisSemester.setSelectedItem(comboitem);

			final EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(searchsemester);
					searchsemester.setSelectedItem(null);

					if (searchJenisSemester.getSelectedItem() == null) {
						return;
					}

					if (searchJenisSemester.getSelectedItem().getValue() == null) {
						org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel("Semua");
						comboitem.setValue(null);
						searchsemester.appendChild(comboitem);
						for (int i = 1; i < 30; i++) {
							comboitem = new MyComboitemConfig();
							comboitem.setLabel(i + "");
							comboitem.setValue(i);
							searchsemester.appendChild(comboitem);
						}
					} else {

						Boolean genap = searchJenisSemester.getSelectedItem().getValue().equals(Perkuliahan.GENAP);
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
					}

					searchsemester.setSelectedIndex(0);
					searchsemester.setReadonly(true);
				}
			};

			searchJenisSemester.addEventListener("onChange", eventListener);
			eventListener.onEvent(null);

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DownloadKrs(String title, String border, boolean closable) {
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
		north.setHeight("200px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("TA / Angkatan"));

		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(searchTahunAjaran);
		searchTahunAjaran.setWidth("70px");
		Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());
		searchTahunAjaran.setReadonly(true);

		hbox.appendChild(searchangkatan);
		searchangkatan.setWidth("50px");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Masa"));
		row.appendChild(searchmasaperkulaiahan = new AmbilDataMasaPerkuliahanBanbox());
		searchmasaperkulaiahan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);

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

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));

		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(searchJenisSemester);
		hbox.appendChild(searchsemester);

		searchJenisSemester.setCols(6);
		searchsemester.setCols(2);

		row = new MyFormRow();
		row.setParent(rows);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(kelas);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM/Nama Mhs"));
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

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(div);

		toolbar.appendChild(new ais.ui.util.MyLabelConfig("Matakuliah"));
		toolbar.appendChild(kodeMatakuliah = new Textbox());
		kodeMatakuliah.setCols(5);

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
					Filedownload.save(new FileInputStream(file),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "krs.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/integrator/helper/DownloadKrs.java:314");

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

		final Integer semester = searchsemester.getSelectedItem() == null ? null
				: (Integer) searchsemester.getSelectedItem().getValue();

		final String kel = kelas.getValue().trim();

		Common.clear(center);

		System.out.println("init spreadsheet running");
		final Jurusan jurusan = searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: (Jurusan) searchjurusan.getSelectedItem().getValue();

		final StatusMahasiswa selectedStatusMahasiswa = (StatusMahasiswa) (searchstatus.getSelectedItem() == null
				|| searchstatus.getSelectedItem().getValue() == null ? null
						: searchstatus.getSelectedItem().getValue());

		final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/data_nilai_"
				+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx");

		(file = new File(filename)).createNewFile();

		final Intbox sizedata = new Intbox(30);
		final Label label = Common.displayLoadBar(this, file, center, sizedata);

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				XSSFWorkbook workbook = new XSSFWorkbook();

				XSSFSheet sheet = workbook.createSheet("KRS");
				sheet.setDefaultColumnWidth(18);

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("NIM");
				rowhead.createCell(1).setCellValue("Nama Mahasiswa");
				rowhead.createCell(2).setCellValue("Semester");
				rowhead.createCell(3).setCellValue("Kode MK");
				rowhead.createCell(4).setCellValue("Mata Kuliah");
				rowhead.createCell(5).setCellValue("Kelas");
				rowhead.createCell(6).setCellValue("Kode Prodi");

				Session session = HibernateUtil.currentNativeSession();

				Criterion criteriaStatus = Restrictions.sqlRestriction("true");
				if (selectedStatusMahasiswa != null) {
					String sql = "this_.id in (select mahasiswa from history_status_mahasiswa where status_mahasiswa="
							+ selectedStatusMahasiswa.getId() + " and tahunakademik = '"
							+ searchTahunAjaran.getSelectedItem().getValue() + "' and semester%2="
							+ (searchJenisSemester.getSelectedItem().getValue().equals(Perkuliahan.GANJIL) ? 1 : 0)
							+ ")";
					System.out.println("sql=>" + sql);
					criteriaStatus = Restrictions.sqlRestriction(sql);
				}

				List<Detailperkuliahan> detailperkuliahans = session.createCriteria(Detailperkuliahan.class)

						.add(criteriaStatus)

						.add(searchTahunAjaran.getSelectedItem() == null
								|| searchTahunAjaran.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("tahunAkademik",
												searchTahunAjaran.getSelectedItem().getValue()))

						.add(searchJenisSemester.getSelectedItem() == null
								|| searchJenisSemester.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.sqlRestriction("this_.semester % 2 = " + (searchJenisSemester
												.getSelectedItem().getValue().equals(Perkuliahan.GANJIL) ? "1" : "0")))

						.createAlias("matakuliahKonversi", "matakuliahKonversi", Criteria.LEFT_JOIN)
						.createAlias("perkuliahan", "perkuliahan", Criteria.INNER_JOIN)

						.add(searchmasaperkulaiahan.getAttribute("masaPerkuliahan") == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("perkuliahan.masaPerkuliahan",
										searchmasaperkulaiahan.getAttribute("masaPerkuliahan")))

						.createAlias("perkuliahan.matakuliah", "matakuliah", Criteria.LEFT_JOIN)

						.add(kel.trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("perkuliahan.kelas", kel, MatchMode.ANYWHERE))

						.add(kodeMatakuliah.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.or(
										Restrictions.or(
												Restrictions.ilike("matakuliahKonversi.nama",
														kodeMatakuliah.getValue().trim(), MatchMode.ANYWHERE),
												Restrictions.ilike("matakuliah.nama", kodeMatakuliah.getValue().trim(),
														MatchMode.ANYWHERE)),
										Restrictions.or(
												Restrictions.ilike("matakuliahKonversi.kode",
														kodeMatakuliah.getValue().trim(), MatchMode.ANYWHERE),
												Restrictions.ilike("matakuliah.kode", kodeMatakuliah.getValue().trim(),
														MatchMode.ANYWHERE))))

						.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))

						.add(semester == null || semester.equals(-1) ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("semester", semester))

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
								|| searchprogram.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("mahasiswa.program",
												searchprogram.getSelectedItem().getValue()))

						.add(searchangkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("mahasiswa.tahunangkatan", searchangkatan.getValue()))

						.addOrder(Order.asc("mahasiswa.nim")).addOrder(Order.asc("semester")).list();

				int size = detailperkuliahans.size();

				XSSFCellStyle notLocked = workbook.createCellStyle();
				notLocked.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				notLocked.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));

				int rowIndex = 1;
				for (Detailperkuliahan detailperkuliahanLain : detailperkuliahans) {

					label.setValue("Sedang memproses data " + detailperkuliahanLain.toString() + " ("
							+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

					if (createData(session, sheet, rowIndex, detailperkuliahanLain,
							(MasaPerkuliahan) searchmasaperkulaiahan.getAttribute("masaPerkuliahan"), notLocked)) {
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

				System.out.println("Your excel file has been generated! ");

				HibernateUtil.closeSession();

				detailperkuliahans.clear();
				label.setValue("");
						} catch (Exception e) {
					// FIX "hang selamanya": try tanpa catch sebelumnya membiarkan exception (mis. gagal
					// query/generate Excel) menembus run() tanpa tertangkap, sehingga label progres
					// tidak pernah diset dan popup progres macet selamanya bagi pengguna.
					Common.tampilErrorJikaAdmin(e);
					label.setValue("Error: " + ais.common.PesanFormalHelper.pesanGagalException(
							"pengambilan data KRS Mahasiswa dari database untuk dikirim ke Neo Feeder",
							null, e,
							new String[] {
									"Periksa kembali data KRS dan Detail Perkuliahan Mahasiswa terkait dan coba ulangi.",
									"Pastikan data KRS dan Detail Perkuliahan Mahasiswa terkait sudah lengkap dan tersinkron.",
									"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
							.replace("\n", " "));
				} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}

	public static boolean createData(Session session, XSSFSheet sheet, int rowIndex,
			Detailperkuliahan detailperkuliahan, MasaPerkuliahan masaPerkuliahan, XSSFCellStyle notLocked) {

		Matakuliah matakuliah = detailperkuliahan.getMatakuliahKonversi() == null
				? (detailperkuliahan.getPerkuliahan() == null ? null
						: detailperkuliahan.getPerkuliahan().getMatakuliah())
				: detailperkuliahan.getMatakuliahKonversi();
		Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
		if (matakuliah != null && detailperkuliahan.getTahunAkademik() != null) {

			XSSFRow row = sheet.createRow(rowIndex);

			String id_smt = masaPerkuliahan != null ? masaPerkuliahan.getNama()
					: detailperkuliahan.getTahunAkademik().split("/")[0] + (detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
							&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek()
									.equals(Perkuliahan.SEMESTER_PENDEK) ? "3"
											: (detailperkuliahan.getSemester() % 2 == 0 ? "2" : "1"));

			if (mahasiswa.getSemesterMulai().equals(Perkuliahan.GENAP)) {
				id_smt = masaPerkuliahan != null ? masaPerkuliahan.getNama()
						: detailperkuliahan.getTahunAkademik().split("/")[0]
								+ (detailperkuliahan.getPerkuliahan() != null
										&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
										&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek()
												.equals(Perkuliahan.SEMESTER_PENDEK) ? "3"
														: (detailperkuliahan.getSemester() % 2 == 0 ? "1" : "2"));
			}

			XSSFCell cell = row.createCell(0);
			cell.setCellStyle(notLocked);
			cell.setCellValue(mahasiswa.getNim());

			cell = row.createCell(1);
			cell.setCellValue(mahasiswa.getNama());

			cell = row.createCell(2);
			cell.setCellStyle(notLocked);
			cell.setCellValue(id_smt);

			cell = row.createCell(3);
			cell.setCellStyle(notLocked);
			cell.setCellValue(matakuliah.getKode());

			cell = row.createCell(4);
			cell.setCellValue(matakuliah.getNama());

			cell = row.createCell(5);
			cell.setCellStyle(notLocked);
			cell.setCellValue(
					detailperkuliahan.getPerkuliahan() == null ? "" : detailperkuliahan.getPerkuliahan().getKelas());

			cell = row.createCell(6);
			cell.setCellStyle(notLocked);
			cell.setCellValue(detailperkuliahan.getMatakuliahKonversi() != null
					? detailperkuliahan.getMatakuliahKonversi().getJurusan().getKodeEpsbed()
					: detailperkuliahan.getPerkuliahan().getJurusan().getKodeEpsbed());

			return true;

		} else {
			return false;
		}
	}
}
