package ais.action.report.helper.keuangan;
import ais.common.PesanFormalHelper;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.Fakultas;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanRekapPerProdiDenganPenguranganPerValidatorWindow extends
		MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Combobox jenisPembayaran = new Combobox();
	private Center center = new Center();
	public PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	public LaporanRekapPerProdiDenganPenguranganPerValidatorWindow() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekap Per Prodi Dengan Pengurangan Per Validator Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanRekapPerProdiDenganPenguranganPerValidatorWindow(
			String title, String border, boolean closable) {
		super(title, border, closable);
		init();
	}

	@SuppressWarnings("deprecation")
	private void init() {

		jenisPembayaran = Common.createComboJenisPembayaran(jenisPembayaran);

		setClosable(true);
		setTitle("Rekap Per Validator");
		setWidth("90%");
		setHeight("90%");
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("280px");
		north.setAutoscroll(true);

		MyGrid grid = new MyGrid();grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("30%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("70%");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" },
				Fakultas.class);
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");



		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pembayaran"));
		row.appendChild(jenisPembayaran);
		jenisPembayaran.setWidth("90%");



		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		tahunAkademik = Common.generateTahunAjaranDanSemua(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");



		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
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
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "RekapPerValidator.xlsx");
			}
		});
		print.setParent(toolbar);

		try {
			initSpreadsheet();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/keuangan/LaporanRekapPerProdiDenganPenguranganPerValidatorWindow.java:195");
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Rekap Per Prodi Dengan Pengurangan Per Validator Window", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
				new String[] {
					"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
					"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});

		}
	}

	@SuppressWarnings("unchecked")
	private Set<String> generateValidator() {
		Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null || searchfakultas.getSelectedItem().getValue()==null ? null
				: searchfakultas.getSelectedItem().getValue());
		String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null || this.tahunAkademik.getSelectedItem().getValue() == null ? null
				: this.tahunAkademik.getSelectedItem().getValue());
		String semester = (String) (this.semesterAbsensi.getSelectedItem() == null || semesterAbsensi.getSelectedItem().getValue() == null ? null
				: this.semesterAbsensi.getSelectedItem().getValue());
		if (tahunAkademik == null || semester == null) {
			return new HashSet<String>();
		}

		ProjectionList projectionList = Projections.projectionList();
		projectionList.add(Projections.groupProperty("validator"));

		Session session = HibernateUtil.currentSession();

		List<String> validatorMahasiswa = session
				.createCriteria(Kegiatan.class).add(Restrictions.eq("aktif", true)).add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
				.setProjection(projectionList)
				.add(Restrictions.eq("tahunAkademik", tahunAkademik))
				.add(semester.equals(Perkuliahan.GENAP) ? Restrictions.in(
						"semster", Common.genap) : Restrictions.in("semster",
						Common.ganjil))
				.createCriteria("mahasiswa")
				.createCriteria("jurusan", Criteria.LEFT_JOIN)
				.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("fakultas", fakultas)).list();

		List<String> validatorCalonMahasiswa = session
				.createCriteria(Kegiatan.class).add(Restrictions.eq("aktif", true)).add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
				.setProjection(projectionList)
				.add(Restrictions.eq("tahunAkademik", tahunAkademik))
				.add(semester.equals(Perkuliahan.GENAP) ? Restrictions.in(
						"semster", Common.genap) : Restrictions.in("semster",
						Common.ganjil))
				.createCriteria("calonMahasiswa")
				.createCriteria("prodiLulus")
				.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("fakultas", fakultas)).list();

		Set<String> validator = new HashSet<String>();
		validator.addAll(validatorMahasiswa);
		validator.addAll(validatorCalonMahasiswa);
		return validator;
	}

	@SuppressWarnings("unchecked")
	private void initSpreadsheet() throws Exception {
		Common.clear(center);
		Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null || searchfakultas.getSelectedItem().getValue()==null ? null
				: searchfakultas.getSelectedItem().getValue());
		String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null || this.tahunAkademik.getSelectedItem().getValue() == null ? null
				: this.tahunAkademik.getSelectedItem().getValue());
		String semester = (String) (this.semesterAbsensi.getSelectedItem() == null || semesterAbsensi.getSelectedItem().getValue() == null ? null
				: this.semesterAbsensi.getSelectedItem().getValue());
		JenisKegiatan jenisPembayaran = (JenisKegiatan) (this.jenisPembayaran
				.getSelectedItem() == null ? null : this.jenisPembayaran
				.getSelectedItem().getValue());
		if (jenisPembayaran == null || tahunAkademik == null
				|| semester == null) {
			return;
		}

		JenisKegiatan jenisKegiatan = jenisPembayaran;

		Set<String> validator = generateValidator();

		Session session = HibernateUtil.currentSession();

		List<Jurusan> jurusans = session
				.createCriteria(Jurusan.class)
				.addOrder(Order.asc("nama"))
				.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("fakultas", fakultas)).list();

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(1 + (validator.size() * 3));
		spreadsheet.setMaxrows(jurusans.size() + 25);

		Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(40);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 0,
				spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1),
				false);

		ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0, "REKAPITULASI KEUANGAN  "
				+ jenisPembayaran.getNamaKegiatan().toUpperCase()
				+ "\n "
				+ ""
				+ "Fakultas"
				+ " "
				+ (fakultas == null ? "SEMUA" : fakultas.getNama()
						.toUpperCase()) + "\n TAHUN AKADEMIK " + tahunAkademik
				+ "\n SEMESTER " + semester);
		Utils.setRowHeight(sheet, 1, 100);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1,
				spreadsheet.getMaxcolumns() - 1, 1), true);
		Cell cell = Utils.getCell(sheet, 1, 0);
		cell.getCellStyle().setWrapText(true);
		cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);

		ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 0, 1, spreadsheet.getMaxcolumns() - 1, false);
		final String color = "#000000";

		ais.ui.util.EcampusUtil.setCellValue(sheet, 2, 0, "Jurusan");
		int rowIndex = 3;
		int mycol = 1;
		for (String validate : validator) {

			rowIndex = 3;
			int colIndex = 0;
			ais.ui.util.EcampusUtil.setCellValue(sheet, 2, (mycol),
					validate == null ? "Tidak ada validator" : validate);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, mycol, "Pemasukan");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, mycol + 1, "Pengurangan");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, mycol + 2, "Selisih");
			Utils.setRowHeight(sheet, 2, 50);
			try {
				ais.ui.util.EcampusUtil.setBorder(sheet,
						new Rect(0, 2, spreadsheet.getMaxcolumns() - 1, 2),
						BookHelper.BORDER_FULL, BorderStyle.THIN, color);
				ais.ui.util.EcampusUtil.setBorder(sheet,
						new Rect(0, 3, spreadsheet.getMaxcolumns() - 1, 3),
						BookHelper.BORDER_FULL, BorderStyle.THIN, color);
				ais.ui.util.EcampusUtil.setBold(sheet,
						new Rect(0, 2, spreadsheet.getMaxcolumns() - 1, 3),
						true);
				ais.ui.util.EcampusUtil.mergeCells(sheet, 2, mycol, 2, mycol + 2, false);
			} catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/action/report/helper/keuangan/LaporanRekapPerProdiDenganPenguranganPerValidatorWindow.java:333");
			}

			rowIndex = 3;
			colIndex = 0;
			Double total = 0.0;
			Double totalPengurangan = 0.0;

			for (Jurusan jurusan : jurusans) {
				ProjectionList projectionList = Projections.projectionList();
				projectionList.add(Projections.sum("nilai"));
				projectionList.add(Projections.sum("kegiatan.pengurangan"));
				Object[] myvalue = (Object[]) session
						.createCriteria(CicilanPembayaran.class)
						.createAlias("kegiatan", "kegiatan")
						.setProjection(projectionList)
						.add(Restrictions.eq("kegiatan.jenisKegiatan",
								jenisKegiatan))
						.add(validate == null || validate.trim().equals("") ? Restrictions
								.or(Restrictions.isNull("kegiatan.validator"),
										Restrictions.eq("kegiatan.validator",
												"")) : Restrictions.eq(
								"kegiatan.validator", validate))
						.add(Restrictions.eq("kegiatan.tahunAkademik",
								tahunAkademik))
						.add(semester.equals(Perkuliahan.GENAP) ? Restrictions
								.in("kegiatan.semster", Common.genap)
								: Restrictions.in("kegiatan.semster",
										Common.ganjil))

						.createAlias("kegiatan.mahasiswa", "mahasiswa",
								Criteria.LEFT_JOIN)
						.createAlias("kegiatan.calonMahasiswa",
								"calonMahasiswa", Criteria.LEFT_JOIN)
						.add(Restrictions.or(Restrictions.eq(
								"mahasiswa.jurusan", jurusan), Restrictions.eq(
								"calonMahasiswa.prodiLulus", jurusan)))
						.uniqueResult();

				Double amount = myvalue == null || myvalue[0] == null ? 0.0
						: new Double(myvalue[0].toString());

				Double pengurangan = myvalue == null || myvalue[1] == null ? 0.0
						: new Double(myvalue[1].toString());

				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex,
						jurusan.getNama());
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, mycol, amount);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, mycol + 1, pengurangan);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, mycol + 2, amount
						- pengurangan);
				total += amount;
				totalPengurangan += pengurangan;

				try {
					ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(colIndex, rowIndex + 1,
							spreadsheet.getMaxcolumns() - 1, rowIndex + 1),
							BookHelper.BORDER_FULL, BorderStyle.THIN, color);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/keuangan/LaporanRekapPerProdiDenganPenguranganPerValidatorWindow.java:391");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Per Prodi Dengan Pengurangan Per Validator Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}

				rowIndex++;
			}

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, mycol, total);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, mycol + 1, totalPengurangan);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, mycol + 2, total
					- totalPengurangan);
			Utils.setColumnWidth(sheet, mycol, 100);
			Utils.setColumnWidth(sheet, mycol + 1, 100);
			Utils.setColumnWidth(sheet, mycol + 2, 100);
			mycol += 3;

		}

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, 0, "Sub Total Fakultas "
				+ (fakultas == null ? "Semua" : fakultas.getNama()));

		ais.ui.util.EcampusUtil.setBold(sheet,
				new Rect(0, rowIndex + 1, spreadsheet.getMaxcolumns() - 1,
						rowIndex), true);
		Utils.setAlignment(sheet, new Rect(0, 4, 0, rowIndex + 1),
				CellStyle.ALIGN_LEFT);
		Utils.setAlignment(sheet, new Rect(1, 4,
				spreadsheet.getMaxcolumns() - 1, rowIndex + 1),
				CellStyle.ALIGN_RIGHT);
		try {
			ais.ui.util.EcampusUtil.setBorder(sheet,
					new Rect(0, rowIndex + 1, spreadsheet.getMaxcolumns() - 1,
							rowIndex + 1), BookHelper.BORDER_FULL,
					BorderStyle.THIN, color);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/keuangan/LaporanRekapPerProdiDenganPenguranganPerValidatorWindow.java:424");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Per Prodi Dengan Pengurangan Per Validator Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
		Utils.setColumnWidth(sheet, 0, 450);

		// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

	}
}
