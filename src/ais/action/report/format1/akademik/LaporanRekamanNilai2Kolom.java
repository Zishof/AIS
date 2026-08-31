package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Skripsi;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan rekaman nilai2 kolom. Kelas ini mengubah data domain
 * menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan
 * transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code AmbilDataMahasiswaBanbox
 * bandboxMahasiswa}, {@code Center center}, {@code MyDatebox tanggal}, {@code Toolbar toolbar}, {@code
 * MyCheckboxConfig hitungUlang}, {@code MyDatebox tanggalDicetak}, {@code Mahasiswa mahasiswa}, {@code Combobox
 * semesterAbsensiUjian}; inisialisasi/lifecycle ({@code initTranskripAkademik()}, {@code init()}); operasi
 * domain lain ({@code generateParameter()}, {@code onTranskrip()}). Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanRekamanNilai2Kolom extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1550813616089440767L;

	private AmbilDataMahasiswaBanbox bandboxMahasiswa;
	private Center center;
	private MyDatebox tanggal;

	private Toolbar toolbar;

	private MyCheckboxConfig hitungUlang;

	private MyDatebox tanggalDicetak;

	private Mahasiswa mahasiswa = null;

	private Combobox semesterAbsensiUjian;

	public LaporanRekamanNilai2Kolom() {
		super();
		try {
			initTranskripAkademik();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekaman Nilai2 Kolom", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanRekamanNilai2Kolom(Mahasiswa mahasiswa) {
		super();
		this.mahasiswa = mahasiswa;
		try {
			initTranskripAkademik();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekaman Nilai2 Kolom", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanRekamanNilai2Kolom(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initTranskripAkademik();
		init();
	}

	private void initTranskripAkademik() throws Exception {
		tanggal = new MyDatebox();
		tanggal.setValue(ais.ui.util.WaktuUtil.getDate());
	}

	private void init() throws Exception {

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

		if (this.mahasiswa != null
				|| (Common.getCurrentUser() != null && Common.getCurrentUser().getMahasiswa() != null)) {
			Mahasiswa mahasiswa = this.mahasiswa != null ? this.mahasiswa : Common.getCurrentUser().getMahasiswa();
			bandboxMahasiswa.setAttribute("mahasiswa", mahasiswa);
			bandboxMahasiswa.setAttribute("myValue", mahasiswa);
			bandboxMahasiswa.setValue(mahasiswa.getNim() + " - " + mahasiswa.getNama());
			bandboxMahasiswa.setId("mhs_" + mahasiswa.getId());
			bandboxMahasiswa.setDisabled(true);
		}
//		bandboxMahasiswa.setEventListener(eventListener);

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

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(hitungUlang = new MyCheckboxConfig("Hitung Ulang IP/IPK"));
		hitungUlang.addEventListener("onClick", eventListener);

		semesterAbsensiUjian = new Combobox();
		for (int i = 1; i <= 21; i++) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			semesterAbsensiUjian.appendChild(comboitem);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(semesterAbsensiUjian);
		semesterAbsensiUjian.setWidth("90%");
		semesterAbsensiUjian.setReadonly(true);
		semesterAbsensiUjian.addEventListener("onChange", eventListener);

		if (bandboxMahasiswa.getAttribute("mahasiswa") != null) {
			Mahasiswa mahasiswa = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");
			Common.selectComboItem(semesterAbsensiUjian, mahasiswa.currentSemester());
		} else {
			Common.selectComboItem(semesterAbsensiUjian, 1);
		}

		Tbmuser tbmuser = Common.getCurrentUser();
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Ditetapkan"));
		tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
		row.appendChild(tanggal);
		tanggal.setWidth("90%");
		tanggal.addEventListener("onChange", eventListener);
		tanggal.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
		tanggal.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Cetak"));
		tanggalDicetak = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
		row.appendChild(tanggalDicetak);
		tanggalDicetak.setWidth("90%");
		tanggalDicetak.addEventListener("onChange", eventListener);
		tanggalDicetak.setDisabled(tbmuser == null || tbmuser.getMahasiswa() != null);
		tanggalDicetak.setReadonly(true);

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
		}, "Rekaman_Nilai_2_Kolom", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onTranskrip(arg0);

			}
		}));

		if (bandboxMahasiswa.getAttribute("mahasiswa") != null) {
			onTranskrip(null);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		if (bandboxMahasiswa.getAttribute("mahasiswa") == null) {
			// MyMessageboxConfig.show("Pilih Mahasiswa", "Peringatan",
			// MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION);
			return null;
		}
		if (tanggal.getValue() == null) {
			// MyMessageboxConfig.show("Tanggal atau genap", "Peringatan",
			// MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION);
			return null;
		}

		Mahasiswa mahasiswa = bandboxMahasiswa == null ? null : (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");
		if (mahasiswa == null) {
			MyMessageboxConfig.show("Mahasiswa belum dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return null;
		}
		Session session = HibernateUtil.currentSession();

		Date date = tanggal.getValue();

		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("dua_kolom", "ya");
		parameters.put("tanggal", date);
		if (hitungUlang.isChecked()) {
			mahasiswa.reInitDetailperkuliahan(session);
		}
		Integer semester = semesterAbsensiUjian == null || semesterAbsensiUjian.getSelectedItem() == null ? null
				: (Integer) semesterAbsensiUjian.getSelectedItem().getValue();
		if (semester == null) {
			MyMessageboxConfig.show("Semester belum dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return null;
		}
		KrsMahasiswa.parameterData(mahasiswa, semester, hitungUlang.isChecked(), parameters);

		parameters.put("tanggal_dicetak",
				tanggalDicetak.getValue() == null ? ais.ui.util.WaktuUtil.getDate() : tanggalDicetak.getValue());
		List<Long> semuaDetailperkuliahan = mahasiswa.ambilDetailperkuliahan(semester);
		List<Long> detailsperkuliahans = new ArrayList<Long>();
		for (Long detailperkuliahan : mahasiswa.saringBerdasarNilaiDan0(semuaDetailperkuliahan)) {
			detailsperkuliahans.add(detailperkuliahan);
		}
		// MK SKS=0 yang SUDAH bernilai tetap TAMPIL di transkrip (nilai huruf "L") namun TIDAK ikut
		// menghitung IPK (IPK dihitung terpisah di KrsMahasiswa.parameterData). Opsional per konfigurasi.
		if (Common.bolehKonfigurasi("tampilkan_matakuliah_sks_0_bernilai_di_transkrip", Konfigurasi.TIDAK_AKTIF)) {
			for (Long idSks0 : mahasiswa.saringTampilkanSks0Bernilai(semuaDetailperkuliahan)) {
				if (!detailsperkuliahans.contains(idSks0)) {
					detailsperkuliahans.add(idSks0);
				}
			}
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

		mahasiswa.putPhotoLulus(parameters);

		String namaFile = "Rekaman_Nilai_2_Kolom";
		if (Common.bolehKonfigurasi("Rekaman_Nilai_2_Kolom_harus_berdasarkan_jenjang", Konfigurasi.TIDAK_AKTIF)) {
			parameters.put("nama_laporan", namaFile + "_" + mahasiswa.getJenjang().getId());
		}
		Skripsi skripsi = (Skripsi) session.createCriteria(Skripsi.class).add(Restrictions.eq("mahasiswa", mahasiswa))
				.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
		if (skripsi != null) {
			Common.insertProperty(Skripsi.class, skripsi, parameters, "skripsi",1,"mahasiswa");
		}
		// JUDUL SKRIPSI: UTAMAKAN dari tabel Skripsi; bila tak ada / kosong, jatuh ke judul_skripsi Mahasiswa.
		String judulSkripsiResolved = (skripsi != null && skripsi.getJudul() != null
				&& skripsi.getJudul().trim().length() > 0) ? skripsi.getJudul() : mahasiswa.getJudulSkripsi();
		parameters.put("judul", judulSkripsiResolved);
		parameters.put("skripsi_judul", judulSkripsiResolved);
		return parameters;
	}

	@SuppressWarnings({})
	public void onTranskrip(Event event) throws Exception {

		try {
			Mahasiswa mahasiswa = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");
			String namaFile = "Rekaman_Nilai_2_Kolom";
			if (mahasiswa != null
					&& Common.bolehKonfigurasi("Rekaman_Nilai_2_Kolom_harus_berdasarkan_jenjang", Konfigurasi.TIDAK_AKTIF)) {
				namaFile = namaFile + "_" + mahasiswa.getJenjang().getId();
			}

			Map parameters = generateParameter();
			if (parameters == null) {
				return;
			}
			File file = Report.generateFileReportWithProgress(Report.PDF, parameters, namaFile,
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Rekaman Nilai2 Kolom", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
