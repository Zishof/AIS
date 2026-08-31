package ais.action.report.helper.keuangan;
import ais.common.PesanFormalHelper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Vbox;

import ais.action.ws.util.ConstantUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailSettingBiaya;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisSeleksi;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan rekap host to host cicilan per item belumbayar window.
 * Kelas ini mengubah data domain menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak
 * tanpa memindahkan aturan transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox searchfakultas}, {@code
 * Combobox searchjurusan}, {@code Combobox tahunAkademik}, {@code Combobox semesterAbsensi}, {@code Combobox
 * jenisPembayaran}, {@code Combobox jenisSeleksi}, {@code Combobox searchprogram}, {@code Combobox jenjang};
 * inisialisasi/lifecycle ({@code init()}, {@code initSpreadsheet()}). Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanRekapHostToHostCicilanPerItemBelumbayarWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private Combobox jenisPembayaran = new Combobox();
	private Combobox jenisSeleksi = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Combobox jenjang = new Combobox();
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private MyDatebox start = new MyDatebox();
	private MyDatebox end = new MyDatebox();
	private List<Checkbox> mapItemBiaya = new ArrayList<Checkbox>();
	private Center center = new Center();

	private Combobox semester;

	private Decimalbox angkatan;

	public LaporanRekapHostToHostCicilanPerItemBelumbayarWindow() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekap Host To Host Cicilan Per Item Belumbayar Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanRekapHostToHostCicilanPerItemBelumbayarWindow(String title, String border, boolean closable) {
		super(title, border, closable);
		init();
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private void init() {

		Common.insertComboDanSemua(jenisPembayaran, "namaKegiatan", JenisKegiatan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertComboDanSemua(jenisSeleksi, "nama", JenisSeleksi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertComboDanSemua(jenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		// setTitle("Rekap Pembayaran Host to Host");
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas/Prodi"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(searchfakultas);
		searchfakultas.setCols(2);
		hbox.appendChild(searchjurusan);
		searchjurusan.setCols(2);
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("TA/Smt"));
		tahunAkademik = Common.generateTahunAjaranDanSemua(tahunAkademik);
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(tahunAkademik);
		tahunAkademik.setCols(5);
		semesterAbsensi = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig("Semua");
		comboitem.setValue(null);
		semesterAbsensi.appendChild(comboitem);
		semesterAbsensi.setSelectedIndex(2);
		hbox.appendChild(semesterAbsensi);
		semesterAbsensi.setCols(5);
		semesterAbsensi.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pembayaran"));
		row.appendChild(jenisPembayaran);
		jenisPembayaran.setWidth("90%");
		jenisPembayaran.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				JenisKegiatan jenisKegiatan = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
						: jenisPembayaran.getSelectedItem().getValue());

				jenisSeleksi.setDisabled(jenisKegiatan == null
						|| !jenisKegiatan.getNamaKegiatan().equals(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA));

				if (jenisSeleksi.isDisabled()) {
					jenisSeleksi.setSelectedItem(null);
				}

			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester/Angkatan"));
		hbox = new Hbox();
		row.appendChild(hbox);
		semester = new Combobox();
		comboitem = new MyComboitemConfig("Semua");
		comboitem.setValue(null);
		semester.appendChild(comboitem);
		for (int i = 1; i < 20; i++) {
			Comboitem itemSmt = new Comboitem();
			itemSmt.setValue(i);
			itemSmt.setLabel(i + "");
			semester.appendChild(itemSmt);
		}
		semester.setReadonly(true);
		semester.setCols(5);
		semester.setSelectedIndex(0);
		hbox.appendChild(semester);

		angkatan = new Decimalbox();
		angkatan.setCols(5);
		hbox.appendChild(angkatan);

		row = new MyFormRow();
		Common.initPrograms(searchprogram);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Seleksi"));
		row.appendChild(jenisSeleksi);
		jenisSeleksi.setDisabled(true);
		jenisSeleksi.setSelectedItem(null);
		jenisSeleksi.setWidth("90%");

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.WEEK_OF_MONTH, calendar.get(Calendar.WEEK_OF_MONTH) - 1);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal"));
		hbox = new Hbox();
		row.appendChild(hbox);
		if (start != null) start.setValue(calendar.getTime());
		hbox.appendChild(start);
		start.setCols(5);
		if (start != null) start.setReadonly(true);

		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);

		if (end != null) end.setValue(calendar.getTime());
		if (end != null) end.setReadonly(true);
		hbox.appendChild(end);
		end.setCols(5);

		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang"));
		row.appendChild(jenjang);
		jenjang.setWidth("90%");

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "8");
		row.setParent(rows);

		final Vbox vbox = new Vbox();
		vbox.setParent(row);

		EventListener listener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(vbox);
				mapItemBiaya.clear();

				JenisKegiatan jenisKegiatan = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
						: jenisPembayaran.getSelectedItem().getValue());

				List<ItemBiaya> itemBiayas = HibernateUtil.currentSession().createCriteria(DetailSettingBiaya.class)
						.createAlias("itemBiaya", "itemBiaya").createAlias("settingBiaya", "settingBiaya")
						.add(jenisKegiatan == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("settingBiaya.jenisKegiatan", jenisKegiatan))
						.add(Restrictions.or(Restrictions.eq("itemBiaya.aktif", true),
								Restrictions.isNull("itemBiaya.aktif")))
						.setProjection(Projections.groupProperty("itemBiaya")).addOrder(Order.asc("itemBiaya")).list();
				Hbox hbox1 = new Hbox();
				vbox.appendChild(hbox1);
				int index = 0;
				for (ItemBiaya itemBiaya : itemBiayas) {
					if (index % 15 == 0) {
						hbox1 = new Hbox();
						vbox.appendChild(hbox1);
					}
					index++;
					Checkbox checkbox = new Checkbox(itemBiaya.getNama());
					checkbox.setAttribute("itemBiaya", itemBiaya);
					mapItemBiaya.add(checkbox);
					checkbox.setChecked(true);
					checkbox.setStyle("font-size:8px");
					checkbox.setParent(hbox1);
				}
				itemBiayas = null;

			}
		};

		jenisPembayaran.addEventListener("onChange", listener);
		try {
			listener.onEvent(null);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/helper/keuangan/LaporanRekapHostToHostCicilanPerItemBelumbayarWindow.java:300");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Host To Host Cicilan Per Item Belumbayar Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "8");
		Hbox toolbar = new Hbox();
		toolbar.setParent(row);
		row.setParent(rows);
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
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
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "REKAP_PEMBAYARAN.xlsx");
			}
		});
		print.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

	}

	// private void

	@SuppressWarnings("unchecked")
	private void initSpreadsheet() throws Exception {

//		int tanggal = Common.getBetweenTwoDates(start.getValue(), end.getValue());
//		if (tanggal > 370) {
//			MyMessageboxConfig.show("Tanggal mulai dan sampai pengambilan data tidak boleh lebih dari 370 hari",
//					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
//			return;
//		}

		final Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());

		final Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());

		final JenisKegiatan jenisPembayaran = (JenisKegiatan) (LaporanRekapHostToHostCicilanPerItemBelumbayarWindow.this.jenisPembayaran
				.getSelectedItem() == null ? null
						: LaporanRekapHostToHostCicilanPerItemBelumbayarWindow.this.jenisPembayaran.getSelectedItem()
								.getValue());
		final JenisSeleksi jenisSeleksi = (JenisSeleksi) (LaporanRekapHostToHostCicilanPerItemBelumbayarWindow.this.jenisSeleksi
				.getSelectedItem() == null ? null
						: LaporanRekapHostToHostCicilanPerItemBelumbayarWindow.this.jenisSeleksi.getSelectedItem()
								.getValue());

		final String tahunAkademik = (String) (LaporanRekapHostToHostCicilanPerItemBelumbayarWindow.this.tahunAkademik
				.getSelectedItem() == null ? null
						: LaporanRekapHostToHostCicilanPerItemBelumbayarWindow.this.tahunAkademik.getSelectedItem()
								.getValue());
		final String semester = (String) (LaporanRekapHostToHostCicilanPerItemBelumbayarWindow.this.semesterAbsensi
				.getSelectedItem() == null ? null
						: LaporanRekapHostToHostCicilanPerItemBelumbayarWindow.this.semesterAbsensi.getSelectedItem()
								.getValue());

		final String program = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());

		final Integer tahunAngkatan = angkatan.getValue() == null ? null : angkatan.getValue().intValue();

		final Jenjang jenjang = (Jenjang) (LaporanRekapHostToHostCicilanPerItemBelumbayarWindow.this.jenjang
				.getSelectedItem() == null ? null
						: LaporanRekapHostToHostCicilanPerItemBelumbayarWindow.this.jenjang.getSelectedItem()
								.getValue());

		final Integer smt = (Integer) (LaporanRekapHostToHostCicilanPerItemBelumbayarWindow.this.semester
				.getValue() == null
				|| LaporanRekapHostToHostCicilanPerItemBelumbayarWindow.this.semester.getSelectedItem() == null ? null
						: LaporanRekapHostToHostCicilanPerItemBelumbayarWindow.this.semester.getSelectedItem()
								.getValue());

		final TreeMap<String, Object[]> jurusans = new TreeMap<String, Object[]>();
		final Set<ItemBiaya> itemBiayas = new TreeSet<ItemBiaya>();

		final Label label = Common.displayLoadBar(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(center);

				spreadsheet = new ais.ui.util.MySpreadsheet();
				Common.clear(center);
				spreadsheet.setParent(center);
				spreadsheet.setWidth("100%");
				spreadsheet.setHeight("100%");
				spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");

				spreadsheet.setMaxcolumns(6 + itemBiayas.size());
				spreadsheet.setMaxrows(jurusans.size() + 25);

				Worksheet sheet = spreadsheet.getSelectedSheet();
				sheet.setDefaultColumnWidth(40);
				try {
					ais.ui.util.EcampusUtil.setBold(sheet,
							new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);
				} catch (Exception e4) { ais.common.ErrorAuditUtil.record(e4, "auto-audit(empty-catch) src/ais/action/report/helper/keuangan/LaporanRekapHostToHostCicilanPerItemBelumbayarWindow.java:417");

				}

				ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0,
						"REKAPITULASI PEMBAYARAN "
								+ (jenisPembayaran == null ? "SEMUA JENIS PEMBAYARAN"
										: jenisPembayaran.getNamaKegiatan().toUpperCase())
								+ "\n  "
								+ (fakultas == null || fakultas.getId().equals(-1L) ? "SEMUA " + "Fakultas"
										: "Fakultas" + " " + fakultas.getNama().toUpperCase())
								+ "\n "
								+ (tahunAkademik == null ? "SEMUA TAHUN AKADEMIK" : "TAHUN AKADEMIK " + tahunAkademik)
								+ "\n  " + (semester == null ? "SEMUA SEMESTER" : "SEMESTER " + semester.toUpperCase())
								+ (jenisSeleksi == null || jenisSeleksi.getId() == null ? ""
										: "\nJENIS SELEKSI " + jenisSeleksi.getNama().toUpperCase()));

				Utils.setRowHeight(sheet, 1, 120);
				ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
				Cell cell = Utils.getCell(sheet, 1, 0);
				cell.getCellStyle().setWrapText(true);
				cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);

				ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 0, 1, spreadsheet.getMaxcolumns() - 1, false);
				final String color = "#000000";
				int rowIndex = 2;
				int colIndex = 0;
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "NIM");
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "Nama");
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Prodi");
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "Program");
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "Angkatan");
				int indexCol = 4;
				for (ItemBiaya itemBiaya : itemBiayas) {
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++indexCol, itemBiaya.getNama());
				}

				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++indexCol, "Total");
				cell = Utils.getCell(sheet, rowIndex, spreadsheet.getMaxcolumns() - 1);
				cell.getCellStyle().setWrapText(true);

				Utils.setRowHeight(sheet, rowIndex, 50);
				ais.ui.util.EcampusUtil.setBorder(sheet,
						new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
						BorderStyle.THIN, color);
				ais.ui.util.EcampusUtil.setBold(sheet,
						new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);

				rowIndex = 3;
				colIndex = 0;

				Double jumlahTotal = 0.0;

				for (Object[] objects : jurusans.values()) {
					if (objects[0] == null) {
						continue;
					}

					GeneralValueObject generalValueObject = (GeneralValueObject) objects[0];
					if (generalValueObject instanceof Mahasiswa) {
						Mahasiswa mahasiswa = (Mahasiswa) generalValueObject;
						String nim = mahasiswa.getNim();
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, nim);

						String nama = mahasiswa.getNama();
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, nama);

						Jurusan jurusan = mahasiswa.getJurusan();
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, jurusan.getNama());

						String program = mahasiswa.getProgram();
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, program);

						String angkatan = mahasiswa.getTahunangkatan().toString();
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, angkatan);
					} else if (generalValueObject instanceof BiodataCalonMahasiswa) {
						BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) generalValueObject;
						String nim = biodataCalonMahasiswa.getNoRegistrasi();
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, nim);

						String nama = biodataCalonMahasiswa.getNama();
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, nama);

						Jurusan jurusan = biodataCalonMahasiswa.getProdiLulus() != null
								? biodataCalonMahasiswa.getProdiLulus()
								: biodataCalonMahasiswa.getProdi1();
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2,
								jurusan == null ? "" : jurusan.getNama());

						String program = biodataCalonMahasiswa.getProgram();
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, program);

						String angkatan = biodataCalonMahasiswa.getTahun().toString();
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, angkatan);
					}

					Map<ItemBiaya, Double> dataTotal = (Map<ItemBiaya, Double>) objects[1];

					indexCol = 4;
					Double total = 0.0;
					for (ItemBiaya itemBiaya : itemBiayas) {
						Double t = dataTotal.get(itemBiaya);
						if (t == null) {
							t = 0.0;
						}
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++indexCol, t);
						total += t;
					}

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++indexCol,
							Common.numberFormat.get().format(total));

					jumlahTotal += total;

					try {
						ais.ui.util.EcampusUtil.setBorder(sheet,
								new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
								BookHelper.BORDER_FULL, BorderStyle.THIN, color);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/keuangan/LaporanRekapHostToHostCicilanPerItemBelumbayarWindow.java:535");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Host To Host Cicilan Per Item Belumbayar Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});

					}

					rowIndex++;
				}

				colIndex = 0;

				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "TOTAL");
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, spreadsheet.getMaxcolumns() - 1,
						Common.numberFormat.get().format(jumlahTotal));
				try {
					ais.ui.util.EcampusUtil.setBorder(sheet,
							new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
							BookHelper.BORDER_FULL, BorderStyle.THIN, color);
					ais.ui.util.EcampusUtil.setBold(sheet,
							new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);

					ais.ui.util.EcampusUtil.setBold(sheet,
							new Rect(spreadsheet.getMaxcolumns() - 2, 3, spreadsheet.getMaxcolumns() - 1, rowIndex),
							true);
				} catch (Exception e3) { ais.common.ErrorAuditUtil.record(e3, "auto-audit(empty-catch) src/ais/action/report/helper/keuangan/LaporanRekapHostToHostCicilanPerItemBelumbayarWindow.java:557");

				}

				Utils.setColumnWidth(sheet, 0, 150);
				Utils.setColumnWidth(sheet, 1, 130);
				Utils.setColumnWidth(sheet, 2, 100);
				Utils.setColumnWidth(sheet, 3, 80);
				Utils.setColumnWidth(sheet, 4, 80);
				indexCol = 4;
				for (@SuppressWarnings("unused")
				ItemBiaya itemBiaya : itemBiayas) {
					Utils.setColumnWidth(sheet, ++indexCol, 100);
				}
				Utils.setColumnWidth(sheet, ++indexCol, 100);

				Common.setStyled(sheet);
				jurusans.clear();

				// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
				ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);
			}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {

				try {

					List<Long> ids = new ArrayList<Long>();
					for (Checkbox checkbox : mapItemBiaya) {
						if (checkbox.isChecked()) {
							ItemBiaya itemBiaya = (ItemBiaya) checkbox.getAttribute("itemBiaya");
							ids.add(itemBiaya.getId());
						}
					}

					List<Long> cicilanPembayarans = new ArrayList<Long>();

					try {
						Session session1 = ais.action.report.Report.openNativeSession();

						cicilanPembayarans = session1.createCriteria(CicilanPembayaran.class)

								.setProjection(Projections.property("id"))

								.add(ids.isEmpty() ? Restrictions.sqlRestriction("false")
										: Restrictions.in("itemBiaya.id", ids))

								.add(Restrictions.sqlRestriction("date(this_.tanggal) between date('"
										+ Common.databaseDateFormat.get().format(start.getValue()) + "') and date('"
										+ Common.databaseDateFormat.get().format(end.getValue()) + "')"))
								.list();
						session1.disconnect();
						session1.close();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/helper/keuangan/LaporanRekapHostToHostCicilanPerItemBelumbayarWindow.java:612");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Host To Host Cicilan Per Item Belumbayar Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
					}

					ais.action.report.Report.closeCurrentSessionQuietly();

					int size = cicilanPembayarans.size();
					int index = 0;

					for (Long cicilanPembayaranId : cicilanPembayarans) {
						try {
							index++;
							Session session1 = ais.action.report.Report.openNativeSession();
							CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session1
									.createCriteria(CicilanPembayaran.class).add(Restrictions.idEq(cicilanPembayaranId))
									.uniqueResult();
							session1.disconnect();
							session1.close();
							ais.action.report.Report.closeCurrentSessionQuietly();
							label.setValue("Ambil data " + cicilanPembayaran.toString() + " ("
									+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");

							if ((cicilanPembayaran.getItemBiaya() != null
									&& ids.contains(cicilanPembayaran.getItemBiaya().getId()))) {

								Kegiatan kegiatan = cicilanPembayaran.getKegiatan();

								if (kegiatan != null) {

									if (smt == null || smt.equals(kegiatan.getSemster())) {

										if (semester == null
												|| (semester.equals(Perkuliahan.GENAP) ? kegiatan.getSemster() % 2 == 0
														: kegiatan.getSemster() % 2 == 1)) {

											if (tahunAkademik == null
													|| tahunAkademik.equals(kegiatan.getTahunAkademik())) {

												if (jenisPembayaran == null || jenisPembayaran.getId()
														.equals(kegiatan.getJenisKegiatan().getId())) {

													Mahasiswa mahasiswa = kegiatan.getMahasiswa();
													BiodataCalonMahasiswa biodataCalonMahasiswa = kegiatan
															.getCalonMahasiswa();

													if (tahunAngkatan == null
															|| (biodataCalonMahasiswa != null
																	&& biodataCalonMahasiswa.getTahun() != null
																	&& tahunAngkatan
																			.equals(biodataCalonMahasiswa.getTahun()))

															|| (mahasiswa != null
																	&& mahasiswa.getTahunangkatan() != null
																	&& tahunAngkatan
																			.equals(mahasiswa.getTahunangkatan()))

													) {

														if (program == null
																|| (biodataCalonMahasiswa != null
																		&& biodataCalonMahasiswa.getProgram() != null
																		&& program.equals(
																				biodataCalonMahasiswa.getProgram()))

																|| (mahasiswa != null && mahasiswa.getProgram() != null
																		&& program.equals(mahasiswa.getProgram()))

														) {

															if (jenisSeleksi == null
																	|| (biodataCalonMahasiswa != null
																			&& biodataCalonMahasiswa
																					.getJenisSeleksi() != null
																			&& jenisSeleksi.getId()
																					.equals(biodataCalonMahasiswa
																							.getJenisSeleksi().getId()))

																	|| (mahasiswa != null
																			&& mahasiswa.getJenisSeleksi() != null
																			&& jenisSeleksi.getId().equals(mahasiswa
																					.getJenisSeleksi().getId()))

															) {

																if (fakultas == null || fakultas.getId().equals(-1L)

																		|| (mahasiswa != null
																				&& mahasiswa.getJurusan() != null
																				&& mahasiswa.getJurusan().getFakultas()
																						.getId()
																						.equals(fakultas.getId()))

																		|| (biodataCalonMahasiswa != null
																				&& biodataCalonMahasiswa
																						.getProdi1() != null
																				&& biodataCalonMahasiswa.getProdi1()
																						.getFakultas().getId()
																						.equals(fakultas.getId()))

																		|| (biodataCalonMahasiswa != null
																				&& biodataCalonMahasiswa
																						.getProdiLulus() != null
																				&& biodataCalonMahasiswa.getProdiLulus()
																						.getFakultas().getId()
																						.equals(fakultas.getId()))) {

																	if (jurusan == null || jurusan.getId().equals(-1L)

																			|| (mahasiswa != null
																					&& mahasiswa.getJurusan() != null
																					&& mahasiswa.getJurusan().getId()
																							.equals(jurusan.getId()))

																			|| (biodataCalonMahasiswa != null
																					&& biodataCalonMahasiswa
																							.getProdi1() != null
																					&& biodataCalonMahasiswa.getProdi1()
																							.getId()
																							.equals(jurusan.getId()))

																			|| (biodataCalonMahasiswa != null
																					&& biodataCalonMahasiswa
																							.getProdiLulus() != null
																					&& biodataCalonMahasiswa
																							.getProdiLulus().getId()
																							.equals(jurusan.getId()))) {

																		if (jenjang == null
																				|| jenjang.getId().equals(-1L)

																				|| (mahasiswa != null
																						&& mahasiswa
																								.getJurusan() != null
																						&& mahasiswa.getJurusan()
																								.getJenjang().getId()
																								.equals(jenjang
																										.getId()))

																				|| (biodataCalonMahasiswa != null
																						&& biodataCalonMahasiswa
																								.getProdi1() != null
																						&& biodataCalonMahasiswa
																								.getProdi1()
																								.getJenjang().getId()
																								.equals(jenjang
																										.getId()))

																				|| (biodataCalonMahasiswa != null
																						&& biodataCalonMahasiswa
																								.getProdiLulus() != null
																						&& biodataCalonMahasiswa
																								.getProdiLulus()
																								.getJenjang().getId()
																								.equals(jenjang
																										.getId()))) {

																			String key = (mahasiswa != null
																					? kegiatan.getMahasiswa().getId()
																							+ "_mhs"
																					: biodataCalonMahasiswa != null
																							? kegiatan
																									.getCalonMahasiswa()
																									.getId()
																									+ "_calon_mhs"
																							: "");

																			Object[] objSbm = jurusans.get(key);
																			Map<ItemBiaya, Double> dataTotal;
																			if (objSbm == null) {
																				dataTotal = new HashMap<ItemBiaya, Double>();
																			} else {
																				dataTotal = (Map<ItemBiaya, Double>) objSbm[1];
																			}

																			Double total = dataTotal.get(
																					cicilanPembayaran.getItemBiaya());
																			if (total == null) {
																				total = 0.0;
																			}
																			total += cicilanPembayaran.getNilai();
																			dataTotal.put(
																					cicilanPembayaran.getItemBiaya(),
																					total);

																			GeneralValueObject generalValueObject = mahasiswa == null
																					? biodataCalonMahasiswa
																					: mahasiswa;

																			Object[] objects = new Object[] {
																					generalValueObject, dataTotal };
																			jurusans.put(key, objects);
																			itemBiayas.add(
																					cicilanPembayaran.getItemBiaya());
																		}
																	}
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/helper/keuangan/LaporanRekapHostToHostCicilanPerItemBelumbayarWindow.java:817");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Host To Host Cicilan Per Item Belumbayar Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
								new String[] {
									"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
									"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
									"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
								});
						}
					}
					cicilanPembayarans = null;

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/helper/keuangan/LaporanRekapHostToHostCicilanPerItemBelumbayarWindow.java:823");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Host To Host Cicilan Per Item Belumbayar Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
				ais.action.report.helper.LoadingReportUtil.selesai(label);
			}
		}).start();

	}
}
