package ais.action.report.helper.absen;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

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
import ais.database.model.Detailperkuliahan;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan daftar hadir window. Kelas ini mengubah data domain
 * menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan
 * transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox searchfakultas}, {@code
 * Combobox searchjurusan}, {@code Combobox tahunAkademik}, {@code Combobox semesterAbsensi}, {@code Combobox
 * perkuliahan}, {@code Spreadsheet spreadsheet}, {@code Center center}, {@code List mahasiswas};
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
public class LaporanDaftarHadirWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368331375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private Combobox perkuliahan = new Combobox();

	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();
	private List<Mahasiswa> mahasiswas = new ArrayList<Mahasiswa>();

	public LaporanDaftarHadirWindow() {
		super();
		try {
			initFakultas();
			init();
			perkuliahan.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					initSpreadsheet();
				}
			});
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Daftar Hadir Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
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
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Daftar Hadir Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanDaftarHadirWindow(String title, String border, boolean closable) {
		super(title, border, closable);
		initFakultas();
		init();
		perkuliahan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});
		try {
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Daftar Hadir Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private void initFakultas() {

		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));

		/**
		 * Event listener lokal milik {@link LaporanDaftarHadirWindow}. Kelas ini menangani event untuk komponen induk
		 * dan meneruskan pekerjaan domain ke method/service yang sudah tersedia.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link LaporanDaftarHadirWindow} dan dapat mengakses
		 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code onEvent}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see LaporanDaftarHadirWindow
		 */
		class PerkuliahanEventListener implements EventListener {
			@Override
			public void onEvent(Event event) throws Exception {
				Common.clear(perkuliahan);
				perkuliahan.setSelectedItem(null);
				if (tahunAkademik.getSelectedItem() == null)
					return;
				if (semesterAbsensi.getSelectedItem() == null)
					return;
				if (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null)
					return;
				if (searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null)
					return;
				List<Perkuliahan> perkuliahans = null;
				perkuliahans = DaoFactory.getInstance().getPerkuliahanDao().findByCriteria(Order.desc("id"),
						Restrictions.eq("tahunAjaran", tahunAkademik.getSelectedItem().getValue()),
						Restrictions.eq("semester", semesterAbsensi.getSelectedItem().getValue()),
						CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false));
				for (Perkuliahan o : perkuliahans) {
					org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
					comboitem.setLabel((o.getDosen1() == null ? "" : o.getDosen1().getNama()) + " - "
							+ o.getMatakuliah().getNama() + " (" + o.getId() + ")");
					comboitem.setValue(o);

					String deskripsi = "Smt: "
							+ (o.getSemester()
									+ (o.getKelas() == null || o.getKelas().equals("") ? "" : " " + o.getKelas()))
							+ ", Ruang: " + (o.getRuang() == null ? "" : o.getRuang().getKodeRuangan()) + ", Hari: "
							+ o.getHari() + ", Waktu: " + o.getWaktuMulai() + "-" + o.getWaktuSelesai();

					comboitem.setDescription(deskripsi);

					perkuliahan.appendChild(comboitem);
				}

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
		 * Event listener lokal milik {@link LaporanDaftarHadirWindow}. Kelas ini menangani event untuk komponen induk
		 * dan meneruskan pekerjaan domain ke method/service yang sudah tersedia.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link LaporanDaftarHadirWindow} dan dapat mengakses
		 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code onEvent}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see LaporanDaftarHadirWindow
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
		try {
			eventListener.onEvent(null);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Daftar Hadir Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	@SuppressWarnings("deprecation")
	private void init() {

		setClosable(true);
		setTitle("Daftar Hadir");
		setWidth("90%");
		setHeight("90%");
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("320px");
		north.setAutoscroll(true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
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
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(semesterAbsensi);
		semesterAbsensi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perkuliahan"));
		row.appendChild(perkuliahan);
		perkuliahan.setWidth("90%");

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
				Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "daftar_hadir_mahasiswa.xlsx");
			}
		});
		print.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	private void initMahasiswa() throws Exception {
		mahasiswas = null;
		mahasiswas = new ArrayList<Mahasiswa>();
		if (perkuliahan.getSelectedItem() == null) {
			// MyMessageboxConfig.show("Data perkuliahan harus dipilih", "Peringatan",
			// 1,
			// MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Session session = HibernateUtil.currentSession();

		ProjectionList projectionList = Projections.projectionList();
		projectionList.add(Projections.property("mahasiswa"));

		List<Mahasiswa> objs = session.createCriteria(Detailperkuliahan.class)
				.add(Restrictions.isNull("ikutiPerkuliahan"))
				.add(Restrictions.eq("perkuliahan", perkuliahan.getSelectedItem().getValue()))
				.setProjection(projectionList).createCriteria("mahasiswa").addOrder(Order.desc("tahunangkatan"))
				.addOrder(Order.asc("nim")).list();

		List<Long> mks = new ArrayList<Long>();
		for (Mahasiswa mahasiswa : objs) {
			if (mks.contains(mahasiswa.getId()))
				continue;
			mahasiswas.add(mahasiswa);
			mks.add(mahasiswa.getId());
		}

	}

	// private void

	private void initSpreadsheet() throws Exception {
		Common.clear(center);
		if (perkuliahan.getSelectedItem() == null) {
			// MyMessageboxConfig.show("Data perkuliahan harus dipilih", "Peringatan",
			// 1,
			// MyMessageboxConfig.EXCLAMATION);
			return;
		}
		initMahasiswa();
		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(22);
		spreadsheet.setMaxrows(mahasiswas.size() + 12);
		final String color = "#000000";

		Perkuliahan perkuliahan = (Perkuliahan) this.perkuliahan.getSelectedItem().getValue();
		Worksheet sheet = spreadsheet.getSelectedSheet();

		ais.ui.util.EcampusUtil.setCellValue(sheet, 3, 1, "MATA KULIAH");
		ais.ui.util.EcampusUtil.setCellValue(sheet, 4, 1, "NAMA DOSEN 1");
		ais.ui.util.EcampusUtil.setCellValue(sheet, 5, 1, "NAMA DOSEN 2");

		ais.ui.util.EcampusUtil.mergeCells(sheet, 3, 1, 3, 2, false);
		ais.ui.util.EcampusUtil.mergeCells(sheet, 4, 1, 4, 2, false);
		ais.ui.util.EcampusUtil.mergeCells(sheet, 5, 1, 5, 2, false);

		ais.ui.util.EcampusUtil.setCellValue(sheet, 3, 3, ": " + perkuliahan.getMatakuliah().getNama().toUpperCase());
		ais.ui.util.EcampusUtil.setCellValue(sheet, 4, 3,
				": " + (perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNama().toUpperCase()));
		ais.ui.util.EcampusUtil.setCellValue(sheet, 5, 3,
				": " + (perkuliahan.getDosen2() == null ? "" : perkuliahan.getDosen2().getNama().toUpperCase()));
		ais.ui.util.EcampusUtil.mergeCells(sheet, 3, 3, 3, spreadsheet.getMaxcolumns() - 1, false);
		ais.ui.util.EcampusUtil.mergeCells(sheet, 4, 3, 4, spreadsheet.getMaxcolumns() - 1, false);
		ais.ui.util.EcampusUtil.mergeCells(sheet, 5, 3, 5, spreadsheet.getMaxcolumns() - 1, false);

		int rowIndex = 8;
		int colIndex = 3;
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "No.");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "NIM");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "Nama");

		for (int i = 0; i < 16; i++) {
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4 + i, "Tanggal Pertemuan");
			Utils.setColumnWidth(sheet, 4 + i, 35);
		}
		ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex, 4, rowIndex, spreadsheet.getMaxcolumns() - 1, false);
		ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex, 1, rowIndex + 1, 1, false);
		ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex, 2, rowIndex + 1, 2, false);
		ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex, 3, rowIndex + 1, 3, false);

		Utils.setColumnWidth(sheet, 3, 450);
		Utils.setColumnWidth(sheet, 2, 100);
		Utils.setColumnWidth(sheet, 1, 30);
		Utils.setColumnWidth(sheet, 0, 0);
		Utils.setRowHeight(sheet, 8, 50);
		Utils.setRowHeight(sheet, 2, 1);
		try {
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
			ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 1,
					"DAFTAR HADIR MAHASISWA\n " + "" + "Fakultas" + " " + fakultas.getNama().toUpperCase() + "\n "
							+ "Jurusan" + " " + jurusan.getNama().toUpperCase() + "\n TAHUN AKADEMIK " + tahunAkademik
							+ "\n SEMESTER " + semester);
			Utils.setRowHeight(sheet, 1, 100);
			ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
			Cell cell = Utils.getCell(sheet, 1, 1);
			cell.getCellStyle().setWrapText(true);
			cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Daftar Hadir Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
		ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 1, 1, spreadsheet.getMaxcolumns() - 1, false);
		ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(1, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				BookHelper.BORDER_FULL, BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(1, rowIndex + 1, spreadsheet.getMaxcolumns() - 1, rowIndex + 1), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);

		rowIndex = 10;
		colIndex = 2;
		int index = 1;
		for (Mahasiswa mahasiswa : mahasiswas) {
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, mahasiswa.getId());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, index);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, mahasiswa.getNim());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 1, mahasiswa.getNama().toUpperCase());
			ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(1, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
					BookHelper.BORDER_FULL, BorderStyle.THIN, color);
			index++;
			rowIndex++;
		}

		Range range = Utils.getRange(sheet, 8, 1, 8, spreadsheet.getMaxcolumns() - 1);
		Cell cell = Utils.getCell(sheet, 8, 2);
		CellStyle cellStyle = cell.getCellStyle();
		// cellStyle.setRotation((short) -90);
		cellStyle.setWrapText(true);
		cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
		range.setStyle(cellStyle);
		Utils.setAlignment(sheet, new Rect(1, 9, 3, spreadsheet.getMaxrows() - 1), CellStyle.ALIGN_LEFT);
		Utils.setAlignment(sheet, new Rect(4, 9, spreadsheet.getMaxrows() - 1, spreadsheet.getMaxrows() - 1),
				CellStyle.ALIGN_RIGHT);

		// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

	}
}
