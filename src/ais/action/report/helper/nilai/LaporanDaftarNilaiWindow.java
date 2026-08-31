package ais.action.report.helper.nilai;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

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
import org.zkoss.zss.model.Range;
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

import ais.common.Common;
import ais.database.dao.DaoFactory;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CommonSorter;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan daftar nilai window. Kelas ini mengubah data domain
 * menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan
 * transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox searchfakultas}, {@code
 * Combobox searchjurusan}, {@code Combobox tahunAkademik}, {@code Combobox semesterAbsensi}, {@code Spreadsheet
 * spreadsheet}, {@code Center center}, {@code List mahasiswas}, {@code TreeMap matakuliahs};
 * inisialisasi/lifecycle ({@code initFakultas()}, {@code init()}, {@code initMahasiswa()}, {@code
 * initSpreadsheet()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanDaftarNilaiWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();

	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();
	private List<CommonSorter> mahasiswas = new ArrayList<CommonSorter>();
	private TreeMap<Long, Matakuliah> matakuliahs = new TreeMap<Long, Matakuliah>();
	private List<Perkuliahan> perkuliahans;

	public LaporanDaftarNilaiWindow() {
		super();
		try {
			initFakultas();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Daftar Nilai Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
		try {
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Daftar Nilai Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanDaftarNilaiWindow(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initFakultas();
		init();
		try {
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Daftar Nilai Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private void initFakultas() throws Exception {

		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));

		/**
		 * Event listener lokal milik {@link LaporanDaftarNilaiWindow}. Kelas ini menangani event untuk komponen induk
		 * dan meneruskan pekerjaan domain ke method/service yang sudah tersedia.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link LaporanDaftarNilaiWindow} dan dapat mengakses
		 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code onEvent}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see LaporanDaftarNilaiWindow
		 */
		class PerkuliahanEventListener implements EventListener {
			@Override
			public void onEvent(Event event) throws Exception {
				// Common.clear(perkuliahan);
				// perkuliahan.setSelectedItem(null);
				if (tahunAkademik.getSelectedItem() == null)
					return;
				if (semesterAbsensi.getSelectedItem() == null)
					return;
				if (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null)
					return;
				if (searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null)
					return;
				perkuliahans = null;
				perkuliahans = DaoFactory.getInstance().getPerkuliahanDao().findByCriteria(Order.desc("id"),
						Restrictions.eq("tahunAjaran", tahunAkademik.getSelectedItem().getValue()),
						Restrictions.eq("semester", semesterAbsensi.getSelectedItem().getValue()),
						CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false), Restrictions.or(
								Restrictions.eq("merupakan_paralel", false), Restrictions.isNull("merupakan_paralel")));
				matakuliahs = null;
				matakuliahs = new TreeMap<Long, Matakuliah>();
				for (Perkuliahan perkuliahan : perkuliahans) {
					if (perkuliahan.getMatakuliah() == null)
						continue;
					matakuliahs.put(perkuliahan.getMatakuliah().getId(), perkuliahan.getMatakuliah());
				}
				initSpreadsheet();
			}
		}

		tahunAkademik = Common.generateTahunAjaranDanSemua(tahunAkademik);
		for (int i = 1; i <= 21; i++) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			semesterAbsensi.appendChild(comboitem);
		}
		Common.selectComboItem(semesterAbsensi, 1);
		/**
		 * Event listener lokal milik {@link LaporanDaftarNilaiWindow}. Kelas ini menangani event untuk komponen induk
		 * dan meneruskan pekerjaan domain ke method/service yang sudah tersedia.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link LaporanDaftarNilaiWindow} dan dapat mengakses
		 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code onEvent}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see LaporanDaftarNilaiWindow
		 */
		class SearchFakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(searchjurusan);
				searchjurusan.setSelectedItem(null);
				if (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
			}

		}

		searchfakultas.addEventListener("onChange", new SearchFakultasEventListener());

		// Apabila user berwenang hanya di fakultas tertentu, maka user hanya
		// boleh mengakses data fakultas atau jurusan tertentu

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser.ambilFakultas() != null) {
			Common.selectComboItem(searchfakultas, tbmuser.ambilFakultas());
			Common.clear(searchjurusan);
			Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.eq("fakultas", tbmuser.ambilFakultas()));
			searchfakultas.setDisabled(true);
		} else {
			searchfakultas.setDisabled(false);
		}

		if (tbmuser.ambilJurusan() != null) {
			Common.selectComboItem(searchjurusan, tbmuser.ambilJurusan());
			searchjurusan.setDisabled(true);
		} else {
			searchjurusan.setDisabled(false);
		}

		PerkuliahanEventListener eventListener = new PerkuliahanEventListener();
		searchfakultas.addEventListener("onChange", eventListener);
		searchjurusan.addEventListener("onChange", eventListener);
		tahunAkademik.addEventListener("onChange", eventListener);
		semesterAbsensi.addEventListener("onChange", eventListener);
		eventListener.onEvent(null);
	}

	@SuppressWarnings("deprecation")
	private void init() {

		setClosable(true);
		// setTitle("Daftar Nilai");
		setWidth("100%");
		setHeight("100%");
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

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("35%");
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("35%");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("pilih_fakultas_harus")));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("pilih_jurusan_harus")));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
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
				Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "daftar_nilai_mahasiswa.xlsx");
			}
		});
		print.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	private void initMahasiswa() {
		mahasiswas = null;
		mahasiswas = new ArrayList<CommonSorter>();
		Integer semester = (Integer) semesterAbsensi.getSelectedItem().getValue();
		if (perkuliahans == null || perkuliahans.size() == 0) {
			return;
		}

		Session session = HibernateUtil.currentSession();

		ProjectionList projectionList = Projections.projectionList();
		projectionList.add(Projections.property("mahasiswa"));
		projectionList.add(Projections.sum("totalNilai"), "mynilai");
		projectionList.add(Projections.groupProperty("mahasiswa"));

		List<Object[]> objs = session.createCriteria(Detailperkuliahan.class)
				.add(Restrictions.isNull("ikutiPerkuliahan")).add(Restrictions.in("perkuliahan", perkuliahans))
				.setProjection(projectionList).addOrder(Order.desc("mynilai")).list();

		for (Object[] o : objs) {
			Mahasiswa mahasiswa = (Mahasiswa) o[0];
			CommonSorter commonSorter = new CommonSorter();
			Double rata = mahasiswa.hitungRataRataSemester(semester, null, null);
			commonSorter.setSerializable(mahasiswa);
			commonSorter.setValue(rata);
			mahasiswas.add(commonSorter);
		}
		Collections.sort(mahasiswas);

	}

	// private void

	private void initSpreadsheet() throws Exception {
		Common.clear(center);
		if (perkuliahans == null) {
			return;
		}
		initMahasiswa();
		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns((matakuliahs.size()) + 5);
		spreadsheet.setMaxrows(mahasiswas.size() + 5);
		final String color = "#000000";

		Worksheet sheet = spreadsheet.getSelectedSheet();
		if (sheet == null) {
			throw new IllegalStateException("Template rowcolumn.xlsx tidak mempunyai worksheet aktif.");
		}

		int rowIndex = 4;
		int colIndex = 3;
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "NIM");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "NAMA");
		for (Matakuliah matakuliah : matakuliahs.values()) {
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, matakuliah.getNama());
			ais.ui.util.EcampusUtil.setCellValue(sheet, 2, colIndex, matakuliah.getId());
			Utils.setColumnWidth(sheet, colIndex, 80);
			colIndex += 1;
		}
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, "JUMLAH NILAI");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 1, "NILAI RATA-RATA");

		try {
			Utils.setColumnWidth(sheet, 2, 450);
			Utils.setColumnWidth(sheet, 1, 100);
			Utils.setColumnWidth(sheet, 0, 0);
			Utils.setRowHeight(sheet, 4, 100);
			Utils.setRowHeight(sheet, 2, 1);
			Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
					|| searchfakultas.getSelectedItem().getValue() == null
					|| searchfakultas.getSelectedItem().getValue() == null ? null
							: searchfakultas.getSelectedItem().getValue());
			Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
					|| searchjurusan.getSelectedItem().getValue() == null
					|| searchjurusan.getSelectedItem().getValue() == null ? null
							: searchjurusan.getSelectedItem().getValue());
			String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null ? null
					: this.tahunAkademik.getSelectedItem().getValue());
			Integer semester = (Integer) (this.semesterAbsensi.getSelectedItem() == null ? null
					: this.semesterAbsensi.getSelectedItem().getValue());
			String namaFakultas = fakultas == null || fakultas.getNama() == null ? "-" : fakultas.getNama().toUpperCase();
			String namaJurusan = jurusan == null || jurusan.getNama() == null ? "-" : jurusan.getNama().toUpperCase();
			ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 1,
					"DAFTAR NILAI MAHASISWA\n " + "" + Common.getBahasaConfig("Fakultas") + " "
							+ namaFakultas + "\n " + Common.getBahasaConfig("Jurusan") + " "
							+ namaJurusan + "\n TAHUN AKADEMIK " + tahunAkademik + "\n SEMESTER "
							+ semester);
			Utils.setRowHeight(sheet, 1, 100);
			ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
			Cell cell = Utils.getCell(sheet, 1, 1);
			cell.getCellStyle().setWrapText(true);
			cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Daftar Nilai Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
		try {
			ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 1, 1, spreadsheet.getMaxcolumns() - 1, false);
			ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(1, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
					BookHelper.BORDER_FULL, BorderStyle.THIN, color);

		} catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/action/report/helper/nilai/LaporanDaftarNilaiWindow.java:389");
		}

		rowIndex = 5;
		colIndex = 1;
		for (CommonSorter commonSorter : mahasiswas) {
			Mahasiswa mahasiswa = (Mahasiswa) commonSorter.getSerializable();
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, mahasiswa.getId());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, mahasiswa.getNim());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 1, mahasiswa.getNama().toUpperCase());

			rowIndex++;
		}

		Session session = HibernateUtil.currentSession();
		for (int i = 5; i < spreadsheet.getMaxrows(); i++) {
			Long mhs = null;
			try {
				mhs = new Double(Utils.getCell(sheet, i, 0).getNumericCellValue()).longValue();
			} catch (Exception e) {
				System.out.println("error = " + e.getMessage());
				continue;
			}

			Double total = 0.0;
			Integer jml = 0;
			for (int y = 3; y < spreadsheet.getMaxcolumns(); y++) {
				Long matkulid = null;
				try {
					Double value = Utils.getCell(sheet, 2, y).getNumericCellValue();
					matkulid = value.longValue();
				} catch (Exception e) {
					// System.out.println("message = " + e.getMessage());
					continue;
				}
				Matakuliah matakuliah = matakuliahs.get(matkulid);
				ProjectionList projectionList = Projections.projectionList();
				projectionList.add(Projections.property("totalNilai"));
				Double detailperkuliahan = (Double) session.createCriteria(Detailperkuliahan.class)
						.add(Restrictions.isNull("ikutiPerkuliahan")).setProjection(projectionList)
						.addOrder(Order.desc("totalNilai")).add(Restrictions.eq("mahasiswa.id", mhs))
						.createCriteria("perkuliahan", Criteria.LEFT_JOIN)
						.add(Restrictions.eq("matakuliah", matakuliah)).setMaxResults(1).uniqueResult();
				if (detailperkuliahan == null) {
					ais.ui.util.EcampusUtil.setCellValue(sheet, i, y, "X");
					continue;
				}
				try {
					ais.ui.util.EcampusUtil.setCellValue(sheet, i, y, detailperkuliahan);
					total += detailperkuliahan;
					jml++;
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/nilai/LaporanDaftarNilaiWindow.java:440");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Daftar Nilai Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}

			try {
				ais.ui.util.EcampusUtil.setCellValue(sheet, i, spreadsheet.getMaxcolumns() - 2, total);
				ais.ui.util.EcampusUtil.setCellValue(sheet, i, spreadsheet.getMaxcolumns() - 1,
						jml.equals(0) ? 0.0 : total / jml.doubleValue());

				Range range = Utils.getRange(sheet, 4, 1, 4, spreadsheet.getMaxcolumns() - 1);
				Cell cell = Utils.getCell(sheet, 4, 2);
				CellStyle cellStyle = cell.getCellStyle();
				cellStyle.setRotation((short) -90);
				cellStyle.setWrapText(true);
				cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
				range.setStyle(cellStyle);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/nilai/LaporanDaftarNilaiWindow.java:456");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Daftar Nilai Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});

			}

		}

		// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

	}
}
