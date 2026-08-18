package ais.action.report.helper.keuangan;
import ais.common.PesanFormalHelper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
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
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailKegiatan;
import ais.database.model.Fakultas;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanRekapPerJenisBiayaWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();

	private Center center = new Center();
	private Combobox jenisPembayaran = new Combobox();
	public PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();
	private List<ItemBiaya> itemBiayas;

	public LaporanRekapPerJenisBiayaWindow() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekap Per Jenis Biaya Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanRekapPerJenisBiayaWindow(String title, String border, boolean closable) {
		super(title, border, closable);
		init();
	}

	@SuppressWarnings("deprecation")
	private void init() {

		jenisPembayaran = Common.createComboJenisPembayaran(jenisPembayaran);

		setClosable(true);
		setTitle("Rekap Per Jenis Biaya");
		setWidth("90%");
		setHeight("90%");
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("160px");
		north.setAutoscroll(true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pembayaran"));
		row.appendChild(jenisPembayaran);
		jenisPembayaran.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		tahunAkademik = Common.generateTahunAjaranDanSemua(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");

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
				Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "RekapPerJenisBiaya.xlsx");
			}
		});
		print.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	private void initSpreadsheet() throws Exception {
		Common.clear(center);
		Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null
				|| this.tahunAkademik.getSelectedItem().getValue() == null ? null
						: this.tahunAkademik.getSelectedItem().getValue());
		String semester = (String) (this.semesterAbsensi.getSelectedItem() == null
				|| semesterAbsensi.getSelectedItem().getValue() == null ? null
						: this.semesterAbsensi.getSelectedItem().getValue());
		JenisKegiatan jenisPembayaran = (JenisKegiatan) (this.jenisPembayaran.getSelectedItem() == null ? null
				: this.jenisPembayaran.getSelectedItem().getValue());
		if (jenisPembayaran == null || tahunAkademik == null || semester == null) {
			return;
		}
		JenisKegiatan jenisKegiatan = jenisPembayaran;
		Session session = HibernateUtil.currentSession();
		List<Jurusan> jurusans = session.createCriteria(Jurusan.class)
				.add(fakultas == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("fakultas", fakultas))
				.list();

		List<ItemBiaya> myBiayas = session.createCriteria(ItemBiaya.class).list();
		itemBiayas = new ArrayList<ItemBiaya>();
		for (ItemBiaya itemBiaya : myBiayas) {
			Number value = (Number) session.createCriteria(DetailKegiatan.class).setProjection(Projections.sum("biaya"))
					.createAlias("detailBiaya", "detail_biaya", Criteria.LEFT_JOIN)
					.add(Restrictions.eq("detail_biaya.itemBiaya", itemBiaya))

					.createCriteria("kegiatan").add(Restrictions.eq("jenisKegiatan", jenisKegiatan))
					.add(Restrictions.eq("tahunAkademik", tahunAkademik))
					.add(semester.equals(Perkuliahan.GENAP) ? Restrictions.in("semster", Common.genap)
							: Restrictions.in("semster", Common.ganjil))

					.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
					.createAlias("calonMahasiswa", "calonMahasiswa", Criteria.LEFT_JOIN)

					.add(jurusans.isEmpty() ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(Restrictions.in("mahasiswa.jurusan", jurusans),
									Restrictions.in("calonMahasiswa.prodiLulus", jurusans)))

					.uniqueResult();

			if (value != null && value.intValue() > 0) {
				itemBiayas.add(itemBiaya);
			}
		}

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(itemBiayas.size() + 2);
		spreadsheet.setMaxrows(jurusans.size() + 4);

		Worksheet sheet = spreadsheet.getSelectedSheet();

		sheet.setDefaultColumnWidth(40);
		ais.ui.util.EcampusUtil.setBold(sheet,
				new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

		ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0,
				"REKAPITULASI KEUANGAN " + jenisPembayaran.getNamaKegiatan().toUpperCase() + "\n " + ""
						+ Common.getBahasaConfig("Fakultas") + " " + fakultas.getNama().toUpperCase()
						+ "\n TAHUN AKADEMIK " + tahunAkademik + "\n SEMESTER " + semester.toUpperCase());
		Utils.setRowHeight(sheet, 1, 100);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
		Cell cell = Utils.getCell(sheet, 1, 0);
		cell.getCellStyle().setWrapText(true);
		cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);

		ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 0, 1, spreadsheet.getMaxcolumns() - 1, false);
		final String color = "#000000";
		int rowIndex = 2;
		int colIndex = 0;
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Jurusan");

		int myindex = 0;
		for (ItemBiaya itemBiaya : itemBiayas) {
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++myindex, itemBiaya.getNama().trim());
			Utils.setColumnWidth(sheet, myindex, 100);

		}
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, spreadsheet.getMaxcolumns() - 1, "Jumlah Pemasukan");

		Utils.setRowHeight(sheet, 2, 50);
		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				true);

		rowIndex = 3;
		colIndex = 0;

		Double[] jumlah = new Double[itemBiayas.size() + 1];
		for (Jurusan jurusan : jurusans) {
			colIndex = 0;
			Double total = 0.0;
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, jurusan.getFakultas().getNama() + " - "
					+ jurusan.getNama() + " - " + (jurusan.getJenjang() == null ? "" : jurusan.getJenjang().getNama()));
			for (ItemBiaya itemBiaya : itemBiayas) {
				Number value = (Number) session.createCriteria(DetailKegiatan.class)
						.setProjection(Projections.sum("biaya"))
						.createAlias("detailBiaya", "detail_biaya", Criteria.LEFT_JOIN)
						.add(Restrictions.eq("detail_biaya.itemBiaya", itemBiaya))

						.createCriteria("kegiatan").add(Restrictions.eq("jenisKegiatan", jenisKegiatan))
						.add(Restrictions.eq("tahunAkademik", tahunAkademik))
						.add(semester.equals(Perkuliahan.GENAP) ? Restrictions.in("semster", Common.genap)
								: Restrictions.in("semster", Common.ganjil))
						.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
						.createAlias("calonMahasiswa", "calonMahasiswa", Criteria.LEFT_JOIN)
						.add(Restrictions.or(Restrictions.eq("mahasiswa.jurusan", jurusan),
								Restrictions.eq("calonMahasiswa.prodiLulus", jurusan)))
						.uniqueResult();

				if (jumlah[colIndex] == null) {
					jumlah[colIndex] = 0.0;
				}
				jumlah[colIndex] += value == null ? 0.0 : value.doubleValue();
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex,
						value == null ? 0.0 : value.doubleValue());

				total += value == null ? 0.0 : value.doubleValue();
				try {
					ais.ui.util.EcampusUtil.setBorder(sheet,
							new Rect(0, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
							BorderStyle.THIN, color);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/keuangan/LaporanRekapPerJenisBiayaWindow.java:301");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Per Jenis Biaya Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});

				}

			}

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex, total);
			rowIndex++;

		}

		colIndex = 0;
		Double jumlahTotal = 0.0;
		for (Double d : jumlah) {
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex, d == null ? 0.0 : d);
			jumlahTotal += (d == null ? 0.0 : d);
		}

		System.out.println("jumlahTotal" + jumlahTotal);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, spreadsheet.getMaxcolumns() - 1, jumlahTotal);

		try {
			ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
					true);

			ais.ui.util.EcampusUtil.setBold(sheet,
					new Rect(spreadsheet.getMaxcolumns() - 1, 3, spreadsheet.getMaxcolumns() - 1, rowIndex), true);

			ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(0, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
					BookHelper.BORDER_FULL, BorderStyle.THIN, color);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/keuangan/LaporanRekapPerJenisBiayaWindow.java:331");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Per Jenis Biaya Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});

		}

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Total ");

		try {
			Utils.setAlignment(sheet, new Rect(0, 3, 0, rowIndex), CellStyle.ALIGN_LEFT);
			Utils.setAlignment(sheet, new Rect(1, 3, spreadsheet.getMaxcolumns() - 1, rowIndex), CellStyle.ALIGN_RIGHT);
			ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(0, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
					BookHelper.BORDER_FULL, BorderStyle.THIN, color);

			Utils.setColumnWidth(sheet, 0, 450);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/keuangan/LaporanRekapPerJenisBiayaWindow.java:344");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Per Jenis Biaya Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});

		}

		// try {
		// Utils.setDataFormat(sheet,
		// new Rect(0, rowIndex, spreadsheet.getMaxcolumns() - 1,
		// rowIndex), "mfn_accounting");
		// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/keuangan/LaporanRekapPerJenisBiayaWindow.java:352");
		// Common.tampilErrorJikaAdmin(e);
		// }

		// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

	}
}
