package ais.action.master.helper.keuangan;

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
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.AmbilDataItemBiayaBanbox;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DownloadCicilanCalonMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Center center = new Center();
	private Combobox searchfakultas = new Combobox();

	private Combobox searchjurusan = new Combobox();
	private Intbox searchangkatan = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 3);
	private Intbox searchangkatansd = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));
	private Combobox searchprogram = new Combobox();

	private Combobox searchsemester = new Combobox();

	private AmbilDataItemBiayaBanbox itemBiaya = new AmbilDataItemBiayaBanbox();
	private Textbox keterangan = new Textbox();

	private Textbox nimMahasiswa = new Textbox();

	private MyCheckboxConfig tampilkanHanyaYangBulanNull = new MyCheckboxConfig();

	private File file;

	private Combobox tahunAkademik;

	private Paging paging;

	private MyDatebox start;

	private MyDatebox end;

	private Intbox ambil;

	public DownloadCicilanCalonMahasiswa() {
		super();
		try {

			Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

			Common.initPrograms(searchprogram);

			MyComboitemConfig comboitemSemua = new MyComboitemConfig();
			comboitemSemua.setLabel("Semua Semester");
			comboitemSemua.setValue(-1);
			searchsemester.appendChild(comboitemSemua);
			for (int i = 1; i < 30; i++) {
				org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
				comboitem.setLabel(i + "");
				comboitem.setValue(i);
				searchsemester.appendChild(comboitem);
			}

			searchsemester.setSelectedIndex(0);

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"Membuka jendela Unduh Cicilan Calon Mahasiswa",
					e,
					new String[] {
							"Muat ulang (refresh) halaman ini, lalu buka kembali jendela Unduh Cicilan Calon Mahasiswa.",
							"Periksa apakah data Fakultas, Jurusan, dan Program Studi pada master data sudah terisi dengan benar.",
							"Jika jendela tetap gagal terbuka setelah beberapa kali percobaan, hubungi Administrator." });
		}
	}

	public DownloadCicilanCalonMahasiswa(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"Membuka jendela Unduh Cicilan Calon Mahasiswa",
					e,
					new String[] {
							"Muat ulang (refresh) halaman ini, lalu buka kembali jendela Unduh Cicilan Calon Mahasiswa.",
							"Periksa apakah data Fakultas, Jurusan, dan Program Studi pada master data sudah terisi dengan benar.",
							"Jika jendela tetap gagal terbuka setelah beberapa kali percobaan, hubungi Administrator." });
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));

		Hbox hbox = new Hbox();
		row.appendChild(hbox);

		hbox.appendChild(searchangkatan);
		searchangkatan.setCols(2);
		hbox.appendChild(searchangkatansd);
		searchangkatansd.setCols(2);

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
		searchprogram.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Item Biaya"));
		row.appendChild(itemBiaya);
		itemBiaya.setReadonly(true);
		itemBiaya.setWidth("90%");

		itemBiaya.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tgl.Mulai"));
		row.appendChild(start = new MyDatebox());
		start.setWidth("90%");

		row = new MyFormRow();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(searchsemester);
		searchsemester.setReadonly(true);
		searchsemester.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Calon Mahasiswa"));

		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(nimMahasiswa);
		nimMahasiswa.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik = new Combobox());
		Common.generateTahunAjaranDanSemua(tahunAkademik);
		tahunAkademik.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hanya yang tanpa bulan"));
		row.appendChild(tampilkanHanyaYangBulanNull);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan);
		keterangan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tgl.Sampai"));
		row.appendChild(end = new MyDatebox());
		end.setWidth("90%");

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.WEEK_OF_MONTH, calendar.get(Calendar.WEEK_OF_MONTH) - 3);
		if (start != null) start.setValue(calendar.getTime());
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Tampilkan")));
		toolbar.appendChild(ambil = new Intbox(500));
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("data")));
		ambil.setCols(3);
		ambil.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

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
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
							"PEMBAYARAN_MAHASISWA.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/keuangan/DownloadCicilanCalonMahasiswa.java:290");

				}
			}
		});
		print.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		paging = new Paging();
		paging.setParent(div);
		paging.setMold("os");
		paging.setPageSize(1);
		paging.setTotalSize(1000);
		paging.addEventListener("onPaging", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});
	}

	@SuppressWarnings({ "unchecked" })
	private void initSpreadsheet() throws Exception {

		final Integer semester = searchsemester.getSelectedItem() == null ? null
				: (Integer) searchsemester.getSelectedItem().getValue();

		Common.clear(center);

		System.out.println("init spreadsheet running");
		final Jurusan jurusan = searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: (Jurusan) searchjurusan.getSelectedItem().getValue();

		final String filename = Sessions.getCurrent().getWebApp()
				.getRealPath("/tmp/data_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");

		(file = new File(filename)).createNewFile();

		final Intbox sizedata = new Intbox(30);
		final Label label = Common.displayLoadBar(this, file, center, sizedata);

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				XSSFWorkbook workbook = new XSSFWorkbook();

				XSSFCellStyle notLocked = workbook.createCellStyle();
				notLocked.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				notLocked.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
				// notLocked.setLocked(false);

				XSSFSheet sheet = workbook.createSheet("PEMBAYARAN");
				// sheet.protectSheet("passwordrahasia");
				sheet.setDefaultColumnWidth(18);

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("ID");
				rowhead.createCell(1).setCellValue("JENIS PEMBAYARAN");
				rowhead.createCell(2).setCellValue("CALON MAHASISWA");
				rowhead.createCell(3).setCellValue("SEMESTER");
				rowhead.createCell(4).setCellValue("ITEM BIAYA");
				rowhead.createCell(5).setCellValue("BULAN");
				rowhead.createCell(6).setCellValue("WAKTU");
				rowhead.createCell(7).setCellValue("JUMLAH BAYAR");
				rowhead.createCell(8).setCellValue("KETERANGAN");
				rowhead.createCell(9).setCellValue("SMT");
				rowhead.createCell(10).setCellValue("TA");
				rowhead.createCell(11).setCellValue("PRODI");
				// tahunAkademik

				Session session = HibernateUtil.currentNativeSession();

				int sekaliAmbil = ambil.getValue() == null ? 500 : ambil.getValue();
				int angkatanMulai = searchangkatan.getValue() == null ? 2000 : searchangkatan.getValue();
				int angkatanSampai = searchangkatansd.getValue() == null ? 2500 : searchangkatansd.getValue();

				List<CicilanPembayaran> cicilanPembayarans = session.createCriteria(CicilanPembayaran.class)

						.add(Restrictions.sqlRestriction("date(this_.tanggal) between date('"
								+ Common.databaseDateFormat.get().format(start.getValue()) + "') and date('"
								+ Common.databaseDateFormat.get().format(end.getValue()) + "')"))

						.setMaxResults(sekaliAmbil)
						.setFirstResult(sekaliAmbil * (paging == null ? 0 : paging.getActivePage()))

						.add(itemBiaya.getAttribute("itemBiaya") == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("itemBiaya", itemBiaya.getAttribute("itemBiaya")))

						.add(keterangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("keterangan", keterangan.getValue().trim(), MatchMode.ANYWHERE))

						.add(tampilkanHanyaYangBulanNull.isChecked()
								? Restrictions.isNull("pengaturanPembayaranBulanan")
								: Restrictions.sqlRestriction("true"))

						.add(Restrictions.gt("nilai", 1.0)).createAlias("kegiatan", "kegiatan")

						.add(tahunAkademik.getSelectedItem() == null
								|| tahunAkademik.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("kegiatan.tahunAkademik",
												tahunAkademik.getSelectedItem().getValue()))

						.add(semester.equals(-1) ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("kegiatan.semster", semester))

						.createAlias("kegiatan.calonMahasiswa", "calonMahasiswa")

						.add(nimMahasiswa.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")

								: Restrictions.or(
										Restrictions.ilike("calonMahasiswa.noUjian", nimMahasiswa.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.or(
												Restrictions.ilike("calonMahasiswa.noRegistrasi",
														nimMahasiswa.getValue().trim(), MatchMode.ANYWHERE),
												Restrictions.ilike("calonMahasiswa.nama",
														nimMahasiswa.getValue().trim(), MatchMode.ANYWHERE))))

						.add(searchjurusan.getSelectedItem() == null
								|| searchjurusan.getSelectedItem().getValue() == null
								|| searchjurusan.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("calonMahasiswa.prodiLulus", jurusan))

						.createAlias("calonMahasiswa.prodiLulus", "jurusan", Criteria.LEFT_JOIN)

						.add(searchfakultas.getSelectedItem() == null
								|| searchfakultas.getSelectedItem().getValue() == null
								|| searchfakultas.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

						.add(searchprogram.getSelectedItem() == null
								|| searchprogram.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("calonMahasiswa.program",
												searchprogram.getSelectedItem().getValue()))

						.add(Restrictions.between("calonMahasiswa.tahun", angkatanMulai, angkatanSampai))

						.addOrder(Order.asc("calonMahasiswa.noRegistrasi")).addOrder(Order.asc("ke")).list();

				int size = cicilanPembayarans.size();

				System.out.println("size = " + size);
				int rowIndex = 1;
				int rowIndexAsli = 1;
				for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {

					if (cicilanPembayaran != null && cicilanPembayaran.getId() != null) {
						label.setValue("Sedang memproses data " + cicilanPembayaran.toString() + " ("
								+ Common.numberFormat.get().format(rowIndexAsli * 100.0 / size) + " %)");

						createData(session, cicilanPembayaran.getKegiatan().getSemster(), sheet, rowIndex, notLocked,
								notLocked, cicilanPembayaran);
						rowIndex++;
					}
					rowIndexAsli++;
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
					PesanFormalHelper.tampilkanGagalException(
							"Menyimpan berkas Excel hasil unduhan Cicilan Calon Mahasiswa",
							e,
							new String[] {
									"Pastikan berkas dengan nama yang sama tidak sedang dibuka oleh aplikasi lain (misalnya Microsoft Excel).",
									"Periksa ketersediaan ruang penyimpanan (disk space) pada server.",
									"Ulangi proses unduh data. Jika kegagalan berulang, hubungi Administrator/Developer disertai tangkapan layar (screenshot) pesan ini." });
				}

				System.out.println("Your excel file has been generated! ");

				HibernateUtil.closeSession();

				cicilanPembayarans.clear();
				cicilanPembayarans = null;
				label.setValue("");
				} catch (Exception e) {
					// FIX "gagal diam-diam"/hang selamanya: sebelumnya try ini TIDAK punya catch,
					// jadi exception apa pun (mis. query Hibernate gagal) akan lolos tak
					// tertangani keluar dari run() thread ini; label.setValue("") di atas tidak
					// pernah tereksekusi, sehingga popup progres bar tidak akan pernah tertutup
					// dan pengguna tidak pernah tahu prosesnya gagal.
					Common.tampilErrorJikaAdmin(e);
					label.setValue("Error: " + PesanFormalHelper.pesanGagalException(
							"pengunduhan (Excel) data Cicilan Calon Mahasiswa", null, e,
							new String[] {
									"Periksa kembali filter (Angkatan, Fakultas/Prodi, Semester, Tanggal) yang dipilih lalu ulangi.",
									"Pastikan koneksi ke database server dalam kondisi baik.",
									"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
							.replace("\n", " "));
				} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}

	private void createData(Session session, int mySemester, XSSFSheet sheet, int rowIndex,
			XSSFCellStyle lockedNumericStyle, XSSFCellStyle notLocked, CicilanPembayaran cicilanPembayaran) {

		XSSFRow row = sheet.createRow(rowIndex);

		XSSFCell cell = row.createCell(0);
		cell.setCellStyle(lockedNumericStyle);
		cell.setCellValue(cicilanPembayaran.getId());

		cell = row.createCell(1);
		cell.setCellStyle(lockedNumericStyle);
		cell.setCellValue(cicilanPembayaran.getKegiatan().getJenisKegiatan() == null ? ""
				: cicilanPembayaran.getKegiatan().getJenisKegiatan().toString());

		cell = row.createCell(2);
		cell.setCellStyle(lockedNumericStyle);
		cell.setCellValue(cicilanPembayaran.getKegiatan().getCalonMahasiswa().toString());

		cell = row.createCell(3);
		cell.setCellStyle(lockedNumericStyle);
		cell.setCellValue(cicilanPembayaran.getKegiatan().getSemster());

		cell = row.createCell(4);
		cell.setCellStyle(lockedNumericStyle);
		cell.setCellValue(cicilanPembayaran.getItemBiaya() == null ? "" : cicilanPembayaran.getItemBiaya().toString());

		cell = row.createCell(5);
		cell.setCellStyle(lockedNumericStyle);
		cell.setCellValue(cicilanPembayaran.getPengaturanPembayaranBulanan() == null
				|| cicilanPembayaran.getPengaturanPembayaranBulanan().getRealBulan() == null ? ""
						: cicilanPembayaran.getPengaturanPembayaranBulanan().getRealBulan().toString());

		cell = row.createCell(6);
		cell.setCellStyle(lockedNumericStyle);
		cell.setCellValue(cicilanPembayaran.getTanggal() == null ? ""
				: Common.dateFormat3.get().format(cicilanPembayaran.getTanggal()));

		cell = row.createCell(7);
		cell.setCellStyle(lockedNumericStyle);
		cell.setCellValue(cicilanPembayaran.getNilai());

		cell = row.createCell(8);
		cell.setCellStyle(notLocked);
		cell.setCellValue(cicilanPembayaran.getKeterangan());

		Kegiatan kegiatan = cicilanPembayaran.getKegiatan();
		cell = row.createCell(9);
		cell.setCellStyle(lockedNumericStyle);
		cell.setCellValue(kegiatan.getSemster() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL);

		cell = row.createCell(10);
		cell.setCellStyle(lockedNumericStyle);
		cell.setCellValue(kegiatan.getTahunAkademik());

		cell = row.createCell(11);
		cell.setCellStyle(lockedNumericStyle);
		cell.setCellValue(kegiatan.getJurusan() == null ? "" : kegiatan.getJurusan().getNama());

	}
}
