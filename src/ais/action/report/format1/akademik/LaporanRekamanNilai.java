package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.PenilaianMahasiswaHelper;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Skripsi;
import ais.database.model.Staff;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

public class LaporanRekamanNilai extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1550813616089440767L;

	private AmbilDataMahasiswaBanbox bandboxMahasiswa;
	private Center center;
	private MyDatebox tanggal;
	private Combobox semesterAbsensiUjian;
	private Toolbar toolbar;

	private MyCheckboxConfig hitungUlang;

	private Mahasiswa mahasiswa = null;

	public LaporanRekamanNilai() {
		super();
		try {
			initTranskripAkademik();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekaman Nilai", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanRekamanNilai(Mahasiswa mahasiswa) {
		super();
		this.mahasiswa = mahasiswa;
		try {
			initTranskripAkademik();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekaman Nilai", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanRekamanNilai(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initTranskripAkademik();
		init();
	}

	private void initTranskripAkademik() throws Exception {
		tanggal = new MyDatebox();
		tanggal.setValue(ais.ui.util.WaktuUtil.getDate());
	}

	private void init() throws Exception {

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			if (!PenilaianMahasiswaHelper.checkBolehLihatNilai(tbmuser.getMahasiswa())) {
				return;
			}
		}

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onTranskrip(event);

			}
		};

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("350px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("20%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_mahasiswa")));
		row.appendChild(bandboxMahasiswa = new AmbilDataMahasiswaBanbox());
		bandboxMahasiswa.setWidth("90%");

		if (mahasiswa != null || (Common.getCurrentUser() != null && Common.getCurrentUser().getMahasiswa() != null)) {
			Mahasiswa mahasiswa = this.mahasiswa != null ? this.mahasiswa : Common.getCurrentUser().getMahasiswa();
			bandboxMahasiswa.setAttribute("mahasiswa", mahasiswa);
			bandboxMahasiswa.setAttribute("myValue", mahasiswa);
			bandboxMahasiswa.setValue(mahasiswa.getNim() + " - " + mahasiswa.getNama());
			bandboxMahasiswa.setId("mhs");
			bandboxMahasiswa.setDisabled(true);
		}
		bandboxMahasiswa.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				if (bandboxMahasiswa.getAttribute("mahasiswa") == null) {
					return;
				}
				Mahasiswa mahasiswa = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");
				Common.selectComboItem(semesterAbsensiUjian, mahasiswa.currentSemester());

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onTranskrip(arg0);
					}
				});

			}
		});

		semesterAbsensiUjian = new Combobox();
		for (int i = 1; i <= 21; i++) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			semesterAbsensiUjian.appendChild(comboitem);
		}

		if (bandboxMahasiswa.getAttribute("mahasiswa") != null) {
			Mahasiswa mahasiswa = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");
			Common.selectComboItem(semesterAbsensiUjian, mahasiswa.currentSemester());
		} else {
			Common.selectComboItem(semesterAbsensiUjian, 1);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(semesterAbsensiUjian);
		semesterAbsensiUjian.setWidth("90%");
		semesterAbsensiUjian.setReadonly(true);
		semesterAbsensiUjian.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(hitungUlang = new MyCheckboxConfig("Hitung Ulang IP/IPK"));
		hitungUlang.addEventListener("onClick", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal"));
		row.appendChild(tanggal);
		tanggal.setWidth("90%");
		tanggal.addEventListener("onChange", eventListener);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				if (bandboxMahasiswa.getAttribute("mahasiswa") == null) {
					MyMessageboxConfig.show("Pilih Mahasiswa", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}
				Map parameters = generateParameter();
				return parameters;
			}
		}, "Rekaman_Nilai", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onTranskrip(arg0);

			}
		}));

		onTranskrip(null);
	}

	@SuppressWarnings({ "rawtypes" })
	public Map generateParameter() throws Exception {
		// if (tahunAkademikUjianAkhirSemester.getSelectedItem() == null) {
		// // MyMessageboxConfig.show("Pilih salah satu tahun akademik",
		// "Peringatan",
		// // 1,
		// // MyMessageboxConfig.INFORMATION);
		// return null;
		// }
		if (semesterAbsensiUjian.getSelectedItem() == null) {
			// MyMessageboxConfig.show("Pilih ganjil atau genap", "Peringatan",
			// MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION);
			return null;
		}
		if (bandboxMahasiswa.getAttribute("mahasiswa") == null) {
			// MyMessageboxConfig.show("Pilih Mahasiswa", "Peringatan",
			// MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION);
			return null;
		}

		Mahasiswa mahasiswa = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");

		int semester = (Integer) semesterAbsensiUjian.getSelectedItem().getValue();

		if (!PenilaianMahasiswaHelper.checkBolehLihatNilai(mahasiswa, semester)) {
			return null;
		}

		Date tanggalCetak = null;
		try {
			tanggalCetak = tanggal.getValue();
		} catch (org.zkoss.zk.ui.WrongValueException e) {
			try { tanggal.clearErrorMessage(); } catch (Exception ce) { ais.common.ErrorAuditUtil.record(ce, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanRekamanNilai.java:tanggal"); }
			return null;
		}
		return LaporanRekamanNilai.generateParameter(mahasiswa, semester, hitungUlang.isChecked(), tanggalCetak);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map generateParameter(Mahasiswa mahasiswa, int semester, boolean hitungUang, Date tanggal)
			throws Exception {

		Session session = null;
		boolean closeLocalSession = false;
		try {
			session = HibernateUtil.currentSession();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanRekamanNilai.java:286");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekaman Nilai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
		if (session == null || !session.isOpen()) {
			session = HibernateUtil.openSession();
			closeLocalSession = true;
		}
		try {
		if (mahasiswa != null && mahasiswa.getId() != null) {
			Mahasiswa m = (Mahasiswa) session.get(Mahasiswa.class, mahasiswa.getId());
			if (m != null) {
				mahasiswa = m;
			}
		}
		FlushMode oldFlushMode = null;
		try {
			oldFlushMode = session.getFlushMode();
			session.setFlushMode(FlushMode.MANUAL);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanRekamanNilai.java:303");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekaman Nilai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
		Staff staffDekan = (Staff) session.createCriteria(Staff.class).add(Restrictions.eq("staff", "dekan"))
				.setMaxResults(1).uniqueResult();

		Staff staffRektor = (Staff) session.createCriteria(Staff.class).add(Restrictions.eq("staff", "rektor"))
				.setMaxResults(1).uniqueResult();

		Date date = tanggal;

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("tanggal", date);
		parameters.put("rektor", staffRektor == null ? "" : staffRektor.getNama());
		parameters.put("dekan", staffDekan == null ? "" : staffDekan.getNama());
		parameters.put("mahasiswa", mahasiswa == null || mahasiswa.getId() == null ? 1L : mahasiswa.getId());

		System.out.println("Cetak Semester IPK, semester < " + semester + "(" + mahasiswa.getNim() + ")");
		parameters.put("semester", semester);

		if (hitungUang) {
			mahasiswa.reInitDetailperkuliahan(session);
		}
		KrsMahasiswa.parameterData(mahasiswa, semester, hitungUang, parameters);

		parameters.put("judul", mahasiswa.getJudulSkripsi());

		List<Long> detailsperkuliahans = new ArrayList<Long>();
		// Matakuliah yang saling ekivalen cukup tampil SATU kali (nilai tertinggi).
		for (Long detailperkuliahan : ais.action.master.helper.EkivalenNilaiUtil.saringNilaiTertinggi(
				mahasiswa.saringBerdasarNilaiDan0(mahasiswa.ambilDetailperkuliahan(semester)), mahasiswa.getNim())) {
			detailsperkuliahans.add(detailperkuliahan);
		}
		parameters.put("jumlah_mk", detailsperkuliahans.size());
		if (detailsperkuliahans.isEmpty()) {
			detailsperkuliahans.add(-1L);
		}
		parameters.put("detailsperkuliahans", detailsperkuliahans.toArray());

		Common.insertProperty(Mahasiswa.class, mahasiswa, parameters, "");
		if (mahasiswa.getJurusan() != null) {
			Common.insertProperty(Jurusan.class, mahasiswa.getJurusan(), parameters, "jur");
		}
		if (mahasiswa.getJurusan().getFakultas() != null) {
			Common.insertProperty(Fakultas.class, mahasiswa.getJurusan().getFakultas(), parameters, "fak");
		}
		if (mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() != null) {
			Common.insertProperty(PerguruanTinggi.class, mahasiswa.getJurusan().getFakultas().getPerguruanTinggi(),
					parameters, "pt");
		}

		/* Callee di atas bisa menutup session di tengah method -> "Session is
		 * closed!" saat query Skripsi (kasus API /api ipk). Pakai session lain
		 * tanpa mengubah variabel session agar closeLocalSession tetap benar. */
		Session sesiSkripsi = session != null && session.isOpen() ? session
				: ais.database.hibernate.HibernateUtil.currentNativeSession();
		Skripsi skripsi = (Skripsi) sesiSkripsi.createCriteria(Skripsi.class)
				.add(Restrictions.eq("mahasiswa", mahasiswa))
				.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
		if (skripsi != null) {
			Common.insertProperty(Skripsi.class, skripsi, parameters, "skripsi",1,"mahasiswa");
		}

		// JUDUL SKRIPSI (permintaan user): UTAMAKAN dari tabel Skripsi (skripsi.getJudul()); bila record
		// Skripsi TIDAK ADA atau judul-nya KOSONG, JATUH ke kolom judul_skripsi di tabel Mahasiswa. Set ke
		// param "judul" (dipakai $F{judul}) dan "skripsi_judul" agar apa pun binding-nya di jrxml tetap terisi.
		String judulSkripsiResolved = (skripsi != null && skripsi.getJudul() != null
				&& skripsi.getJudul().trim().length() > 0) ? skripsi.getJudul() : mahasiswa.getJudulSkripsi();
		parameters.put("judul", judulSkripsiResolved);
		parameters.put("skripsi_judul", judulSkripsiResolved);

		try {
			if (oldFlushMode != null) {
				/* FIX (SessionException: Session is closed!): "session" bisa saja sudah
				 * ditutup oleh callee di atas (lihat komentar sesiSkripsi). Jangan panggil
				 * setFlushMode pada reference session lama yang sudah closed -> pakai
				 * sesiSkripsi (yang sudah divalidasi open, entah session asli atau
				 * currentNativeSession() thread-bound) sebagai fallback aman. */
				Session sesiFlush = session != null && session.isOpen() ? session : sesiSkripsi;
				if (sesiFlush != null && sesiFlush.isOpen()) {
					sesiFlush.setFlushMode(oldFlushMode);
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanRekamanNilai.java:369");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekaman Nilai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
		return parameters;
		} finally {
			// Tutup session yang DIBUKA lokal (fallback openSession) di FINALLY -> tidak bocor pada
			// jalur exception. Hanya menutup bila kita yang membukanya (closeLocalSession).
			if (closeLocalSession && session != null && session.isOpen()) {
				try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanRekamanNilai.java:376");}
			}
		}
	}

	@SuppressWarnings({})
	public void onTranskrip(Event event) throws Exception {

		try {
			Map parameters = generateParameter();
			// Method ini juga dipanggil saat window baru dibuat. Pada saat itu mahasiswa/
			// semester dapat belum dipilih; null berarti belum siap, bukan parameter report.
			if (parameters == null) return;
			File file = Report.generateFileReportWithProgress(Report.PDF, parameters, "Rekaman_Nilai",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Rekaman Nilai", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
