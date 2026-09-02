package ais.action.master.dashboard.admin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Html;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Window;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.FormatNilai;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.NilaiHuruf;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.TugasKelompok;
import ais.database.model.TugasPertemuan;
import ais.database.model.obe.CapaianLulusan;
import ais.database.model.obe.CapaianPembelajaranLulusan;
import ais.database.model.obe.ProfilLulusan;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Tipe khusus untuk rekap hasil tugas per tugas dan ujian obe. Kelas ini memberi nama dan batas
 * tanggung jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang
 * diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Center center}, {@code File file},
 * {@code boolean simple}, {@code Perkuliahan perkuliahan}, {@code int MAX_OBE_PARALLEL_THREADS}, {@code Map
 * excelPreviewHtmlBySheet}, {@code Map nilaiAkhirObePerMhs}; inisialisasi/lifecycle ({@code init()}, {@code
 * initSpreadsheet()}); pembacaan/pencarian ({@code findNearestObeHeader()}, {@code getHeaderCellStringValue()},
 * {@code getJoinedHeaderText()}, {@code getBestChildHeaderText()}, {@code getLastHeaderText()}, {@code
 * getParentHeaderText()}); validasi/perhitungan ({@code hitungNilaiObeMahasiswaParallel()}, {@code
 * calculateStudentObeResult()}, {@code hitungHurufObeSafe()}, {@code hitungPersenPerKomponen()}, {@code
 * recalculateTotalPerFormat()}, {@code hitungHurufObe()}); mutasi data ({@code prosesMasukkanKeNilaiAkhir()},
 * {@code updateProgressLabelSafe()}, {@code setAllBorders()}, {@code parseTugasJSON()}, {@code
 * parseIdsToSet()}); operasi domain lain ({@code masukkanKeNilaiAkhir()}, {@code addTabFromMemorySheet()},
 * {@code showExcelPreviewPopup()}, {@code convertSheetToModernPreviewTable()}, {@code
 * buildSheetDashboardSummaryHtml()}, {@code appendOverviewCard()}). Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class RekapHasilTugasPerTugasDanUjianObe extends MyWindow {

	private static final long serialVersionUID = 790038368339375113L;

	private Center center = new Center();
	private File file;
	private boolean simple;
	private Perkuliahan perkuliahan;

	/*
	 * Batas maksimal worker untuk proses hitung nilai OBE.
	 * Jumlah thread aktual tetap disesuaikan dengan jumlah mahasiswa agar tidak
	 * membuat thread berlebih ketika data sedikit.
	 */
	private static final int MAX_OBE_PARALLEL_THREADS = 150;

	/**
	 * Tipe implementasi bersarang {@link ObeCellValue} milik {@link RekapHasilTugasPerTugasDanUjianObe}. Kelas ini
	 * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * RekapHasilTugasPerTugasDanUjianObe}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API
	 * kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Object value}, {@code boolean
	 * summary}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 *
	 * @see RekapHasilTugasPerTugasDanUjianObe
	 */
	private static class ObeCellValue {
		Object value;
		boolean summary;

		ObeCellValue(Object value, boolean summary) {
			this.value = value;
			this.summary = summary;
		}
	}

	/**
	 * Tipe implementasi bersarang {@link ObeAssessmentColumn} milik {@link RekapHasilTugasPerTugasDanUjianObe}.
	 * Kelas ini memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok
	 * anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * RekapHasilTugasPerTugasDanUjianObe}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API
	 * kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long formatId}, {@code String key}.
	 * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 *
	 * @see RekapHasilTugasPerTugasDanUjianObe
	 */
	private static class ObeAssessmentColumn {
		Long formatId;
		String key;

		ObeAssessmentColumn(Long formatId, String key) {
			this.formatId = formatId;
			this.key = key;
		}
	}

	/**
	 * Tipe implementasi bersarang {@link ObeFormatColumn} milik {@link RekapHasilTugasPerTugasDanUjianObe}. Kelas
	 * ini memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * RekapHasilTugasPerTugasDanUjianObe}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API
	 * kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code FormatNilai formatNilai}, {@code List
	 * assessmentColumns}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 *
	 * @see RekapHasilTugasPerTugasDanUjianObe
	 */
	private static class ObeFormatColumn {
		FormatNilai formatNilai;
		List<ObeAssessmentColumn> assessmentColumns = new ArrayList<ObeAssessmentColumn>();

		ObeFormatColumn(FormatNilai formatNilai) {
			this.formatNilai = formatNilai;
		}
	}

	/**
	 * Tipe implementasi bersarang {@link StudentObeResult} milik {@link RekapHasilTugasPerTugasDanUjianObe}. Kelas
	 * ini memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * RekapHasilTugasPerTugasDanUjianObe}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API
	 * kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int nomor}, {@code Mahasiswa
	 * mahasiswa}, {@code Double hasilKonversiTotal}, {@code List cells}, {@code Map subCpmk}, {@code Map cpmk},
	 * {@code Map cpl}, {@code Map pl}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
	 * dipanggilnya.</p>
	 *
	 * @see RekapHasilTugasPerTugasDanUjianObe
	 */
	private static class StudentObeResult {
		int nomor;
		Mahasiswa mahasiswa;
		Double hasilKonversiTotal = Double.valueOf(0.0);
		List<ObeCellValue> cells = new ArrayList<ObeCellValue>();
		Map<Long, Double> subCpmk = new HashMap<Long, Double>(); // ketercapaian per Sub-CPMK (FormatNilai)
		Map<Long, Double> cpmk = new HashMap<Long, Double>();
		Map<Long, Double> cpl = new HashMap<Long, Double>();
		Map<Long, Double> pl = new HashMap<Long, Double>();
	}

	/*
	 * Cache HTML preview Excel penuh. Tab utama menampilkan tabel ringan,
	 * sedangkan popup "Lihat / Download Excel" memakai cache ini agar tampilan
	 * Excel lama tetap tersedia tanpa menghitung ulang.
	 */
	private final Map<String, String> excelPreviewHtmlBySheet = new LinkedHashMap<String, String>();

	/**
	 * Nilai akhir OBE per mahasiswa (mahasiswa.id → hasilKonversiTotal 0-100), diisi saat rekap
	 * selesai dihitung. Dipakai tombol "Masukkan ke nilai akhir" untuk mendorong nilai ke
	 * {@code Detailperkuliahan.totalNilai} (NILAI AKHIR resmi mata kuliah).
	 */
	private volatile Map<Long, Double> nilaiAkhirObePerMhs;

	public RekapHasilTugasPerTugasDanUjianObe(boolean simple, Perkuliahan perkuliahan) {
		super();
		this.simple = simple;
		this.perkuliahan = perkuliahan;
		try {
			init();
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

		North south = new North();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);

		if (!simple) {
			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig(Common.getBahasaConfig("Tutup"),
					"/img/cancel.gif");
			cancel.setTooltiptext(Common.getBahasaConfig("Tutup"));
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					detach();
				}
			});
			cancel.setParent(toolbar);
		}

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig(Common.getBahasaConfig("Lihat / Download Excel"),
				"/img/excel.png");
		print.setTooltiptext(Common.getBahasaConfig("Lihat preview Excel lengkap dan download file Excel"));
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				showExcelPreviewPopup();
			}
		});
		print.setParent(toolbar);

		print = new MyToolbarbuttonConfig(Common.getBahasaConfig("Refresh"), "/img/excel.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				initSpreadsheet();
			}
		});
		print.setParent(toolbar);

		// Dorong nilai akhir OBE (hasil konversi total per mahasiswa) ke NILAI AKHIR resmi mata
		// kuliah (Detailperkuliahan.totalNilai + nilai huruf). Hanya untuk pengelola (bukan peserta).
		ais.database.model.Tbmuser userAkhir = Common.getCurrentUser();
		boolean pesertaDidik = userAkhir != null
				&& (userAkhir.getMahasiswa() != null || userAkhir.getSiswa() != null);
		if (!pesertaDidik) {
			MyToolbarbuttonConfig masukAkhir = new MyToolbarbuttonConfig(
					Common.getBahasaConfig("Masukkan ke nilai akhir"), "/img/svg/check2.svg");
			masukAkhir.setStyle("font-weight:bold;");
			masukAkhir.setTooltiptext(Common.getBahasaConfig(
					"Masukkan nilai akhir OBE (kolom Nilai Konversi) menjadi NILAI AKHIR resmi mata kuliah untuk semua mahasiswa"));
			masukAkhir.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					masukkanKeNilaiAkhir();
				}
			});
			masukAkhir.setParent(toolbar);

			MyToolbarbuttonConfig cetakNilai = new MyToolbarbuttonConfig("Cetak Nilai", "/img/print.png");
			cetakNilai.setTooltiptext("Cetak daftar nilai akhir perkuliahan ini (Lap. → Nilai)");
			cetakNilai.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					ais.action.master.helper.DetailperkuliahanForPenilaianHelper.onLaporan(perkuliahan);
				}
			});
			cetakNilai.setParent(toolbar);
		}

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		initSpreadsheet();
	}

	/** Konfirmasi lalu dorong nilai akhir OBE ke NILAI AKHIR resmi (Detailperkuliahan.totalNilai). */
	private void masukkanKeNilaiAkhir() {
		try {
			final Map<Long, Double> data = nilaiAkhirObePerMhs;
			if (data == null || data.isEmpty()) {
				ais.ui.util.MyMessageboxConfig.show(
						"Data rekap OBE belum siap. Silakan tunggu tabel selesai dimuat, atau klik Refresh dahulu.",
						"Informasi", ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION);
				return;
			}
			if (perkuliahan != null && perkuliahan.getDikunci() != null) {
				ais.ui.util.MyMessageboxConfig.show("Nilai perkuliahan ini sudah DIKUNCI, tidak dapat diubah.",
						"Informasi", ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION);
				return;
			}
			ais.ui.util.MyMessageboxConfig.show(
					"Masukkan nilai akhir OBE menjadi NILAI AKHIR resmi untuk " + data.size()
							+ " mahasiswa? Nilai akhir & nilai huruf yang ada akan DITIMPA dengan hasil konversi OBE.",
					"Konfirmasi", ais.ui.util.MyMessageboxConfig.OK | ais.ui.util.MyMessageboxConfig.CANCEL,
					ais.ui.util.MyMessageboxConfig.QUESTION, new EventListener() {
						@Override
						public void onEvent(Event ev) throws Exception {
							if (Integer.parseInt(ev.getData().toString()) != ais.ui.util.MyMessageboxConfig.OK) {
								return;
							}
							Common.createDefaultTimer(new EventListener() {
								@Override
								public void onEvent(Event t) throws Exception {
									prosesMasukkanKeNilaiAkhir(data);
								}
							});
						}
					});
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Tulis nilai akhir OBE ke {@code Detailperkuliahan.totalNilai} + nilai huruf/lulus/IP (dan versi
	 * sementara) untuk tiap mahasiswa. Pola commit meniru {@code GradingHelper}.
	 */
	private void prosesMasukkanKeNilaiAkhir(Map<Long, Double> data) {
		org.hibernate.Session session = null;
		org.hibernate.Transaction tx = null;
		int ok = 0;
		int gagal = 0;
		try {
			ais.database.model.Matakuliah matakuliah = perkuliahan == null ? null : perkuliahan.getMatakuliah();
			session = ais.database.hibernate.HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			int count = 0;
			for (Map.Entry<Long, Double> e : data.entrySet()) {
				Long mhsId = e.getKey();
				Double nilai = e.getValue();
				try {
					ais.database.model.Mahasiswa mhs = (ais.database.model.Mahasiswa) session
							.get(ais.database.model.Mahasiswa.class, mhsId);
					if (mhs == null) {
						continue;
					}
					Long dpId = perkuliahan.ambilDetailperkuliahan(mhs);
					if (dpId == null) {
						continue;
					}
					ais.database.model.Detailperkuliahan dp = (ais.database.model.Detailperkuliahan) session
							.get(ais.database.model.Detailperkuliahan.class, dpId);
					if (dp == null) {
						continue;
					}
					double total = nilai == null ? 0.0 : nilai.doubleValue();
					ais.database.model.NilaiHuruf nilaiHuruf = null;
					try {
						nilaiHuruf = Common.getNilaiHuruf(total, mhs.getTahunangkatan(), mhs.getJurusan(),
								mhs.getJurusan() == null ? null : mhs.getJurusan().getFakultas(), dp.getTahunAkademik(),
								dp.getSemester() % 2 == 0 ? ais.database.model.Perkuliahan.GENAP
										: ais.database.model.Perkuliahan.GANJIL,
								matakuliah == null ? "" : matakuliah.getKode(),
								matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());
					} catch (Exception exH) {
						ais.common.ErrorAuditUtil.record(exH, "masukkan-nilai-akhir-obe getNilaiHuruf mhs=" + mhsId);
					}
					dp.setTotalNilai(total);
					dp.setNilaiHuruf(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
					dp.setLulus(nilaiHuruf == null ? null : nilaiHuruf.getLulus());
					dp.setTotalIP(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());
					dp.setTotalNilaiSementara(total);
					dp.setNilaiHurufSementara(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
					dp.setTotalIPSementara(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());
					session.update(dp);
					ok++;
					if (++count % 50 == 0) {
						session.flush();
						session.clear();
					}
				} catch (Exception exM) {
					gagal++;
					ais.common.ErrorAuditUtil.record(exM, "masukkan-nilai-akhir-obe mhs=" + mhsId);
				}
			}
			tx.commit();
		} catch (Exception e) {
			if (tx != null) {
				try {
					tx.rollback();
				} catch (Exception ignore) {
					ais.common.ErrorAuditUtil.record(ignore, "masukkan-nilai-akhir-obe rollback");
				}
			}
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null) {
				try {
					if (session.isOpen()) {
						session.close();
					}
				} catch (Exception ignore) {
					ais.common.ErrorAuditUtil.record(ignore, "masukkan-nilai-akhir-obe close");
				}
			}
		}
		final int okF = ok;
		final int gagalF = gagal;
		try {
			ais.ui.util.MyMessageboxConfig.show(
					"Selesai. " + okF + " nilai akhir diperbarui" + (gagalF > 0 ? ", " + gagalF + " gagal" : "")
							+ ".\n\nCek tab Nilai untuk memverifikasi.",
					"Selesai", ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION);
		} catch (Exception ignore) {
			ais.common.ErrorAuditUtil.record(ignore, "masukkan-nilai-akhir-obe ringkasan");
		}
	}

	private void initSpreadsheet() throws Exception {
		final TreeMap<String, Long> pertemuans = new TreeMap<String, Long>();
		Perkuliahan kul = (Perkuliahan) perkuliahan;
		Perkuliahan kuliyah = kul.getMerupakan_paralel() && kul.getPerkuliahan_paralel() != null
				? kul.getPerkuliahan_paralel()
				: kul;
		List<Perkuliahan> perkuliahans = kuliyah.ambilParalelPerkuliahan();
		if (!perkuliahans.contains(kuliyah)) {
			perkuliahans.add(kuliyah);
		}
		for (Perkuliahan perkuliahanParalel : perkuliahans) {
			pertemuans.putAll(perkuliahanParalel.ambilPertemuan());
		}
		if (pertemuans.isEmpty()) {
			return;
		}

		final List<Mahasiswa> hasilUjianMahasiswas = perkuliahan.ambilMahasiswa();
		if (hasilUjianMahasiswas.isEmpty()) {
			return;
		}

		// Siapkan UI Tabbox di main thread
		center.getChildren().clear();

		// 0. Buat SATU kontainer induk sebagai anak tunggal dari Center
		org.zkoss.zul.Div masterContainer = new org.zkoss.zul.Div();
		masterContainer.setHeight("100%");
		masterContainer.setWidth("100%");
		masterContainer.setParent(center);

		// 1. Buat kontainer khusus untuk Loading Bar (Harus FINAL agar bisa diakses Thread)
		final org.zkoss.zul.Div loadingContainer = new org.zkoss.zul.Div();
		loadingContainer.setParent(masterContainer); // Masukkan ke masterContainer

		// 2. Buat kontainer khusus untuk Tabbox hasil OBE
		org.zkoss.zul.Div tabContainer = new org.zkoss.zul.Div();
		tabContainer.setHeight("100%");
		tabContainer.setWidth("100%");
		tabContainer.setParent(masterContainer); // Masukkan ke masterContainer

		final ais.ui.util.MyButtonTabbox tabbox = ais.ui.util.MyButtonTabbox.buat(
				tabContainer, "100%", new int[] { 1 });

		final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/data_obe_"
				+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
				+ ".xlsx");

		file = new File(filename);
		synchronized (excelPreviewHtmlBySheet) {
			excelPreviewHtmlBySheet.clear();
		}

		final Intbox sizedata = new Intbox(30);
		final Intbox sizedataCol = new Intbox(30);

		// 3. Parsing loadingContainer ke displayLoadBar
		final Label label = Common.displayLoadBar(this, file, loadingContainer, sizedata, sizedataCol);

		// Tangkap Desktop untuk ZK 5.5
		final Desktop desktop = Executions.getCurrent().getDesktop();

		/* OPTIMASI FASE 5: server push dulu dinyalakan di sini tetapi TIDAK PERNAH dimatikan,
		 * sehingga browser terus polling (menahan thread Tomcat) selama tab terbuka walau proses
		 * sudah selesai. Tugas juga dijalankan pada thread MENTAH tanpa batas.
		 * jalankanDenganPush() menyalakan push ber-reference-count, menjalankan tugas pada pool
		 * daemon berbatas milik AsyncTaskManager, lalu MELEPAS push di finally. */
		ais.common.AsyncTaskManager.jalankanDenganPush(desktop, new Runnable() {
			@SuppressWarnings({ "unchecked" })
			@Override
			public void run() {
				Session sessionLocal = null;
				try {
					sessionLocal = HibernateUtil.getSessionFactory().openSession();

					XSSFWorkbook workbook = new XSSFWorkbook();

					// =========================================================================
					// SHEET 1: "DATA" (SHEET ASLI, TIDAK DIKURANGI)
					// =========================================================================
					XSSFSheet sheet = workbook.createSheet("DATA");
					sheet.setDefaultColumnWidth(20);
					int rowIndex = 0;

					XSSFFont hlink_font = workbook.createFont();
					hlink_font.setColor(IndexedColors.BLACK.getIndex());
					hlink_font.setBold(true);

					XSSFCellStyle hlink_style = workbook.createCellStyle();
					hlink_style.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
					hlink_style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
					hlink_style.setFont(hlink_font);
					setAllBorders(hlink_style);

					XSSFCellStyle hlink_styleFooter = workbook.createCellStyle();
					hlink_styleFooter.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
					hlink_styleFooter.setFillPattern(FillPatternType.SOLID_FOREGROUND);
					hlink_styleFooter.setFont(hlink_font);
					setAllBorders(hlink_styleFooter);

					XSSFCellStyle hlink_style1 = workbook.createCellStyle();
					hlink_style1.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
					hlink_style1.setFillPattern(FillPatternType.SOLID_FOREGROUND);
					hlink_style1.setFont(hlink_font);
					setAllBorders(hlink_style1);

					XSSFCellStyle dataStyle = workbook.createCellStyle();
					dataStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
					setAllBorders(dataStyle);

					XSSFCellStyle dataStyle1 = workbook.createCellStyle();
					dataStyle1.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
					setAllBorders(dataStyle1);

					XSSFCell cell = sheet.createRow(rowIndex).createCell(0);
					cell.setCellValue(
							Common.getBahasaConfig("Rekap Penilian OBE") + " \"" + perkuliahan.infoSimple() + "\"");
					cell.setCellStyle(hlink_styleFooter);

					rowIndex++;
					XSSFRow rowhead = sheet.createRow(rowIndex);
					XSSFRow rowhead1 = sheet.createRow(rowIndex + 1);
					XSSFRow rowhead2 = sheet.createRow(rowIndex + 2);

					String[] headers = { "No.", "NIM/No. Reg", "Nama", "Prodi", "Program" };
					for (int i = 0; i < headers.length; i++) {
						createHeaderCell(rowhead, i, Common.getBahasaConfig(headers[i]), hlink_style);
						createHeaderCell(rowhead1, i, Common.getBahasaConfig(headers[i]), hlink_style);
						createHeaderCell(rowhead2, i, Common.getBahasaConfig(headers[i]), hlink_style);
					}

					try {
						for (int i = 0; i < 5; i++) {
							sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex + 2, i, i));
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:350");
					}

					rowIndex += 3;
					int index = 5;

					KurikulumPunyaMatakuliah kpm = perkuliahan.getKurikulumPunyaMatakuliah();
					Matakuliah mk = kpm.getMatakuliah();
					boolean isCpmkBase = kpm.getNilaiMenggunakanCpmk() != null ? kpm.getNilaiMenggunakanCpmk() : false;
					double targetMinimal = kpm.getMinimalKetercapaian() != null ? kpm.getMinimalKetercapaian() : 60.0;

					List<FormatNilai> formatNilais = Common.getFormatNilais(perkuliahan);

					// PREPARE REFERENSI OBE (CPMK, CPL, PL)
					List<CapaianPembelajaranLulusan> listCpmkUnique = new ArrayList<CapaianPembelajaranLulusan>();
					Map<Long, List<FormatNilai>> mapCpmkIdToFns = new HashMap<Long, List<FormatNilai>>();

					if (mk.getCapaianPembelajaranLulusan() != null
							&& !mk.getCapaianPembelajaranLulusan().trim().isEmpty()) {
						Set<Long> cpmkIds = RekapHasilTugasPerTugasDanUjianObe.this
								.parseIdsToSet(mk.getCapaianPembelajaranLulusan());
						if (!cpmkIds.isEmpty()) {
							listCpmkUnique = ConstantValues.simpleList(
									sessionLocal.createCriteria(CapaianPembelajaranLulusan.class)
											.add(Restrictions.in("id", cpmkIds)).addOrder(Order.asc("kode")),
									CapaianPembelajaranLulusan.class);
						}
					}

					for (FormatNilai fn : formatNilais) {
						CapaianPembelajaranLulusan cpmkObj = fn.getCapaianPembelajaranLulusan();
						if (cpmkObj != null) {
							List<FormatNilai> fns = mapCpmkIdToFns.get(cpmkObj.getId());
							if (fns == null) {
								fns = new ArrayList<FormatNilai>();
								mapCpmkIdToFns.put(cpmkObj.getId(), fns);
							}
							fns.add(fn);
						}
					}

					List<CapaianLulusan> listCpl = new ArrayList<CapaianLulusan>();
					if (mk.getCapaianLulusan() != null && !mk.getCapaianLulusan().trim().isEmpty()) {
						Set<Long> cplIds = RekapHasilTugasPerTugasDanUjianObe.this
								.parseIdsToSet(mk.getCapaianLulusan());
						if (!cplIds.isEmpty()) {
							listCpl = ConstantValues
									.simpleList(
											sessionLocal.createCriteria(CapaianLulusan.class)
													.add(Restrictions.in("id", cplIds)).addOrder(Order.asc("kode")),
											CapaianLulusan.class);
						}
					}

					List<ProfilLulusan> listPL = new ArrayList<ProfilLulusan>();
					Set<Long> plIds = new HashSet<Long>();
					for (CapaianLulusan cpl : listCpl) {
						if (cpl.getProfil() != null)
							plIds.addAll(RekapHasilTugasPerTugasDanUjianObe.this.parseIdsToSet(cpl.getProfil()));
					}
					if (!plIds.isEmpty()) {
						listPL = ConstantValues.simpleList(sessionLocal.createCriteria(ProfilLulusan.class)
								.add(Restrictions.in("id", plIds)).addOrder(Order.asc("kode")), ProfilLulusan.class);
					}

					// TARIK DATA TUGAS & UJIAN
					List<PertemuanPunyaUjian> pertemuanPunyaUjians = sessionLocal
							.createCriteria(PertemuanPunyaUjian.class).createAlias("pertemuan", "p")
							.add(Restrictions.eq("p.perkuliahan", perkuliahan)).list();
					List<Pertemuan> pertemuansTugas = sessionLocal.createCriteria(Pertemuan.class)
							.add(Restrictions.eq("perkuliahan", perkuliahan)).add(Restrictions.isNotNull("judultugas"))
							.list();
					Collection<Long> pertemuansList = perkuliahan.ambilPertemuan().values();
					List<TugasPertemuan> pertemuansTugasLanjut = pertemuansList.isEmpty()
							? new ArrayList<TugasPertemuan>()
							: sessionLocal.createCriteria(TugasPertemuan.class)
									.add(Restrictions.in("pertemuan", pertemuansList)).list();
					List<TugasKelompok> pertemuansTugasKelompoks = sessionLocal.createCriteria(TugasKelompok.class)
							.add(Restrictions.eq("perkuliahan", perkuliahan)).list();

					Map<Long, Map<Long, Map<String, Double>>> dataNiliasUtama = new HashMap<Long, Map<Long, Map<String, Double>>>();
					Map<Long, Map<String, Double>> dataBobot = new HashMap<Long, Map<String, Double>>();
					Map<Long, Map<Long, PertemuanPunyaUjian>> mapPertemuanPunyaUjian = new HashMap<Long, Map<Long, PertemuanPunyaUjian>>();
					Map<Long, Map<Long, Pertemuan>> mapTugas = new HashMap<Long, Map<Long, Pertemuan>>();
					Map<Long, Map<Long, TugasPertemuan>> mapTugasLanjut = new HashMap<Long, Map<Long, TugasPertemuan>>();
					Map<Long, Map<Long, TugasKelompok>> mapTugasKelompok = new HashMap<Long, Map<Long, TugasKelompok>>();
					Map<String, String> mapHasilObe = new HashMap<String, String>();

					if (!pertemuanPunyaUjians.isEmpty() && !hasilUjianMahasiswas.isEmpty()) {
						/*
						 * Jangan menyaring hanya dengan keyhasil. Sebagian baris warisan/import memiliki
						 * keyhasil NULL tetapi nilai_obe sudah berisi JSON yang sah. Rekap hanya memakai
						 * nilaiObe, sehingga baris seperti itu tetap merupakan sumber nilai yang valid.
						 * Syarat OR mempertahankan baris ujian normal sekaligus memulihkan data OBE lama.
						 */
						List<HasilUjianMahasiswa> listHasil = sessionLocal.createCriteria(HasilUjianMahasiswa.class)
								.add(Restrictions.or(Restrictions.isNotNull("keyhasil"),
										Restrictions.isNotNull("nilaiObe")))
								.add(Restrictions.in("pertemuanPunyaUjian", pertemuanPunyaUjians))
								.add(Restrictions.in("mahasiswa", hasilUjianMahasiswas)).list();
						for (HasilUjianMahasiswa hum : listHasil) {
							if (hum.getNilaiObe() != null)
								mapHasilObe.put(hum.getPertemuanPunyaUjian().getId() + "_" + hum.getMahasiswa().getId(),
										hum.getNilaiObe());
						}
					}

					parseTugasJSON(pertemuansTugas, formatNilais, hasilUjianMahasiswas, dataNiliasUtama, mapTugas,
							Pertemuan.class.getName());
					parseTugasJSON(pertemuansTugasLanjut, formatNilais, hasilUjianMahasiswas, dataNiliasUtama,
							mapTugasLanjut, TugasPertemuan.class.getName());
					parseTugasJSON(pertemuansTugasKelompoks, formatNilais, hasilUjianMahasiswas, dataNiliasUtama,
							mapTugasKelompok, TugasKelompok.class.getName());

					for (PertemuanPunyaUjian ppu : pertemuanPunyaUjians) {
						for (Mahasiswa m : hasilUjianMahasiswas) {
							String val = mapHasilObe.get(ppu.getId() + "_" + m.getId());
							if (val != null && !val.isEmpty()) {
								try {
									JSONObject j = new JSONObject(val);
									for (FormatNilai fn : formatNilais) {
										if (!j.isNull(fn.getId().toString())) {
											addMapType(mapPertemuanPunyaUjian, fn.getId(), ppu.getId(), ppu);
											Double skor = j.getDouble(fn.getId().toString());
											Double max = j.isNull(fn.getId() + "_max") ? 0.0
													: j.getDouble(fn.getId() + "_max");
											Double didapat = max.equals(0.0) ? 0.0 : (skor * 100.0) / max;
											addNilaiUtama(dataNiliasUtama, m.getId(), fn.getId(),
													PertemuanPunyaUjian.class.getName() + "_" + ppu.getId(), didapat);
										}
									}
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:474");
								}
							}
						}
					}

					for (FormatNilai fn : formatNilais) {
						Map<Long, PertemuanPunyaUjian> md = mapPertemuanPunyaUjian.get(fn.getId());
						Map<Long, Pertemuan> mt = mapTugas.get(fn.getId());
						Map<Long, TugasPertemuan> mtl = mapTugasLanjut.get(fn.getId());
						Map<Long, TugasKelompok> mtk = mapTugasKelompok.get(fn.getId());

						if (md != null)
							for (PertemuanPunyaUjian p : md.values())
								extractBobot(p.getFormatNilais(), fn.getId(),
										PertemuanPunyaUjian.class.getName() + "_" + p.getId(), dataBobot);
						if (mt != null)
							for (Pertemuan p : mt.values())
								extractBobot(p.getFormatNilais(), fn.getId(),
										Pertemuan.class.getName() + "_" + p.getId(), dataBobot);
						if (mtl != null)
							for (TugasPertemuan p : mtl.values())
								extractBobot(p.getFormatNilais(), fn.getId(),
										TugasPertemuan.class.getName() + "_" + p.getId(), dataBobot);
						if (mtk != null)
							for (TugasKelompok p : mtk.values())
								extractBobot(p.getFormatNilais(), fn.getId(),
										TugasKelompok.class.getName() + "_" + p.getId(), dataBobot);
					}

					Map<String, Double> persensData = new HashMap<String, Double>();
					for (FormatNilai fn : formatNilais) {
						Map<Long, PertemuanPunyaUjian> md = mapPertemuanPunyaUjian.get(fn.getId());
						Map<Long, Pertemuan> mt = mapTugas.get(fn.getId());
						Map<Long, TugasPertemuan> mtl = mapTugasLanjut.get(fn.getId());
						Map<Long, TugasKelompok> mtk = mapTugasKelompok.get(fn.getId());

						if (md != null || mt != null || mtl != null || mtk != null) {
							if (md != null)
								for (PertemuanPunyaUjian p : md.values())
									hitungPersenPerKomponen(PertemuanPunyaUjian.class.getName(), p.getId(), fn,
											dataBobot, persensData);
							if (mt != null)
								for (Pertemuan p : mt.values())
									hitungPersenPerKomponen(Pertemuan.class.getName(), p.getId(), fn, dataBobot,
											persensData);
							if (mtl != null)
								for (TugasPertemuan p : mtl.values())
									hitungPersenPerKomponen(TugasPertemuan.class.getName(), p.getId(), fn, dataBobot,
											persensData);
							if (mtk != null)
								for (TugasKelompok p : mtk.values())
									hitungPersenPerKomponen(TugasKelompok.class.getName(), p.getId(), fn, dataBobot,
											persensData);
						}
					}

					final List<ObeFormatColumn> obeFormatColumns = new ArrayList<ObeFormatColumn>();
					for (FormatNilai formatNilai : formatNilais) {
						Map<Long, PertemuanPunyaUjian> mapd = mapPertemuanPunyaUjian.get(formatNilai.getId());
						Map<Long, Pertemuan> mapdTgs = mapTugas.get(formatNilai.getId());
						Map<Long, TugasPertemuan> mapdTgsLanjut = mapTugasLanjut.get(formatNilai.getId());
						Map<Long, TugasKelompok> mapdTgsKelompok = mapTugasKelompok.get(formatNilai.getId());

						if (mapd != null || mapdTgs != null || mapdTgsLanjut != null || mapdTgsKelompok != null) {
							ObeFormatColumn formatColumn = new ObeFormatColumn(formatNilai);
							cell = rowhead.createCell(index);
							cell.setCellValue(formatNilai.getNama() + " ("
									+ Common.numberFormat.get().format(formatNilai.getPersen()) + " %)");
							cell.setCellStyle(hlink_style);
							int colMulai = index;

							if (mapd != null) {
								for (PertemuanPunyaUjian ppu : mapd.values()) {
									String key = PertemuanPunyaUjian.class.getName() + "_" + ppu.getId();
									createSubHeader(rowhead1, rowhead2, index++, ppu.getUjian().getNama(), hlink_style,
											formatNilai, key, dataBobot, persensData);
									formatColumn.assessmentColumns.add(new ObeAssessmentColumn(formatNilai.getId(), key));
								}
							}
							if (mapdTgs != null) {
								for (Pertemuan pt : mapdTgs.values()) {
									String key = Pertemuan.class.getName() + "_" + pt.getId();
									createSubHeader(rowhead1, rowhead2, index++, pt.getJudultugas(), hlink_style,
											formatNilai, key, dataBobot, persensData);
									formatColumn.assessmentColumns.add(new ObeAssessmentColumn(formatNilai.getId(), key));
								}
							}
							if (mapdTgsLanjut != null) {
								for (TugasPertemuan tp : mapdTgsLanjut.values()) {
									String key = TugasPertemuan.class.getName() + "_" + tp.getId();
									createSubHeader(rowhead1, rowhead2, index++, tp.getJudultugas(), hlink_style,
											formatNilai, key, dataBobot, persensData);
									formatColumn.assessmentColumns.add(new ObeAssessmentColumn(formatNilai.getId(), key));
								}
							}
							if (mapdTgsKelompok != null) {
								for (TugasKelompok tk : mapdTgsKelompok.values()) {
									String key = TugasKelompok.class.getName() + "_" + tk.getId();
									createSubHeader(rowhead1, rowhead2, index++, tk.getJudultugas(), hlink_style,
											formatNilai, key, dataBobot, persensData);
									formatColumn.assessmentColumns.add(new ObeAssessmentColumn(formatNilai.getId(), key));
								}
							}

							createHeaderColumn(rowhead1, rowhead2, index++,
									Common.getBahasaConfig("Total") + " " + formatNilai.getNama(),
									Common.numberFormat.get().format(formatNilai.getPersen()) + "%", hlink_style1);
							createHeaderColumn(rowhead1, rowhead2, index++, Common.getBahasaConfig("Nilai Konversi"),
									"100%", hlink_style1);
							createHeaderColumn(rowhead1, rowhead2, index++, Common.getBahasaConfig("Kategori Capaian"),
									"(nilai huruf)", hlink_style1);
							createHeaderColumn(rowhead1, rowhead2, index++, Common.getBahasaConfig("Status Kelulusan"),
									"(L/TL)", hlink_style1);

							obeFormatColumns.add(formatColumn);

							try {
								sheet.addMergedRegion(new CellRangeAddress(1, 1, colMulai, index - 1));
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:593");
							}
						}
					}

					createMainHeaderMerged(sheet, rowhead, rowhead1, rowhead2, index++, "Total", "Total Konversi",
							hlink_style1);
					createMainHeaderMerged(sheet, rowhead, rowhead1, rowhead2, index++, "Nilai OBE", "Nilai Huruf",
							hlink_style1);
					createMainHeaderMerged(sheet, rowhead, rowhead1, rowhead2, index++, "Status", "(L/TL)",
							hlink_style1);

					// MAP PENAMPUNG SKOR AKHIR UNTUK EXPORT OBE BARU
					Map<Long, Double> mapMhsTotalAkhir = new HashMap<Long, Double>();
					Map<Long, Map<Long, Double>> mapMhsSubCpmk = new HashMap<Long, Map<Long, Double>>();
					Map<Long, Map<Long, Double>> mapMhsCpmk = new HashMap<Long, Map<Long, Double>>();
					Map<Long, Map<Long, Double>> mapMhsCpl = new HashMap<Long, Map<Long, Double>>();
					Map<Long, Map<Long, Double>> mapMhsPl = new HashMap<Long, Map<Long, Double>>();

					List<StudentObeResult> hasilParallel = hitungNilaiObeMahasiswaParallel(hasilUjianMahasiswas,
							obeFormatColumns, dataNiliasUtama, persensData, listCpmkUnique, listCpl, listPL,
							mapCpmkIdToFns, targetMinimal, desktop, label);

					int indexLocal = 1;
					int maxIndexData = index;
					for (StudentObeResult hasilMahasiswa : hasilParallel) {
						Mahasiswa mahasiswa = hasilMahasiswa.mahasiswa;
						try {
							Executions.activate(desktop);
							try {
								label.setValue(Common.getBahasaConfig("Menulis hasil nilai OBE") + " "
										+ (mahasiswa == null ? "" : mahasiswa.toString()) + " (" + Common.numberFormat.get()
												.format(indexLocal * 100.0 / hasilParallel.size())
										+ " %)");
							} finally {
								Executions.deactivate(desktop);
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:630");
						}

						XSSFRow row = sheet.createRow(rowIndex);
						writeExcelCell(row, 0, Integer.valueOf(hasilMahasiswa.nomor), dataStyle);
						writeExcelCell(row, 1, mahasiswa == null ? "" : mahasiswa.getNim(), dataStyle);
						writeExcelCell(row, 2, mahasiswa == null ? "" : mahasiswa.getNama(), dataStyle);
						writeExcelCell(row, 3,
								mahasiswa == null || mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama(),
								dataStyle);
						writeExcelCell(row, 4, mahasiswa == null ? "" : mahasiswa.getProgram(), dataStyle);

						int writeIndex = 5;
						for (ObeCellValue cellValue : hasilMahasiswa.cells) {
							writeExcelCell(row, writeIndex++, cellValue.value, cellValue.summary ? dataStyle1 : dataStyle);
						}
						if (writeIndex > maxIndexData) {
							maxIndexData = writeIndex;
						}

						if (mahasiswa != null && mahasiswa.getId() != null) {
							mapMhsTotalAkhir.put(mahasiswa.getId(), hasilMahasiswa.hasilKonversiTotal);
							mapMhsSubCpmk.put(mahasiswa.getId(), hasilMahasiswa.subCpmk);
							mapMhsCpmk.put(mahasiswa.getId(), hasilMahasiswa.cpmk);
							mapMhsCpl.put(mahasiswa.getId(), hasilMahasiswa.cpl);
							mapMhsPl.put(mahasiswa.getId(), hasilMahasiswa.pl);
						}
						indexLocal++;
						rowIndex++;
					}

					// Simpan nilai akhir OBE per mahasiswa untuk tombol "Masukkan ke nilai akhir".
					nilaiAkhirObePerMhs = mapMhsTotalAkhir;

					// ── Baris Rata-rata Kelas ─────────────────────────────────────────────────
					// Hitung rata-rata numerik per kolom dari seluruh mahasiswa yang ada datanya,
					// lalu tulis satu baris footer ber-highlight di bawah data mahasiswa.
					if (!hasilParallel.isEmpty()) {
						int numCols = 0;
						for (StudentObeResult sor : hasilParallel) {
							if (sor.cells.size() > numCols) numCols = sor.cells.size();
						}
						double[] sums   = new double[numCols];
						int[]    counts = new int[numCols];
						for (StudentObeResult sor : hasilParallel) {
							for (int ci = 0; ci < sor.cells.size() && ci < numCols; ci++) {
								Object v = sor.cells.get(ci).value;
								if (v instanceof Number) {
									sums[ci]   += ((Number) v).doubleValue();
									counts[ci] += 1;
								}
							}
						}
						XSSFRow avgRow = sheet.createRow(rowIndex);
						writeExcelCell(avgRow, 0, "RATA-RATA KELAS", hlink_styleFooter);
						writeExcelCell(avgRow, 1, "",                hlink_styleFooter);
						writeExcelCell(avgRow, 2, "",                hlink_styleFooter);
						writeExcelCell(avgRow, 3, "",                hlink_styleFooter);
						writeExcelCell(avgRow, 4, "",                hlink_styleFooter);
						for (int ci = 0; ci < numCols; ci++) {
							if (counts[ci] > 0) {
								double avg = Math.round(sums[ci] / counts[ci] * 100.0) / 100.0;
								writeExcelCell(avgRow, 5 + ci, Double.valueOf(avg), hlink_styleFooter);
							} else {
								writeExcelCell(avgRow, 5 + ci, "", hlink_styleFooter);
							}
						}
						rowIndex++;
					}

					index = maxIndexData;

					for (int i = 5; i < index; i++) {
						try {
							sheet.autoSizeColumn(i);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:702");
						}
					}

					// -> TAB 1: RENDER LANGSUNG DARI SHEET "DATA" DI MEMORI
					addTabFromMemorySheet(desktop, tabbox, sheet);

					// -> TAB SELANJUTNYA DIBUAT BERSAMAAN DENGAN PEMBUATAN EXCEL SHEET DI MEMORI

					// Rangkuman Sub-CPMK (rata-rata kelas per Sub-CPMK) — melengkapi CPMK/CPL/PL agar SEMUA
					// level OBE punya tabel "Rata-Rata Nilai" per item. REUSE generateSheetRangkuman.
					List<SubCpmkItem> listSubCpmk = new ArrayList<SubCpmkItem>();
					for (FormatNilai fn : formatNilais) {
						if (fn != null && fn.getId() != null) {
							listSubCpmk.add(new SubCpmkItem(fn));
						}
					}
					generateSheetRangkuman(workbook, "Rangkuman Sub-CPMK", "RANGKUMAN KETERCAPAIAN SUB-CPMK",
							listSubCpmk, mapMhsSubCpmk, targetMinimal, hlink_style, dataStyle, desktop, tabbox);

					generateSheetPenilaian(workbook, "Penilaian CPMK", "EVALUASI KETERCAPAIAN CPMK",
							hasilUjianMahasiswas, listCpmkUnique, mapMhsCpmk, mapMhsTotalAkhir, targetMinimal,
							hlink_style, dataStyle, desktop, tabbox);
					generateSheetHasil(workbook, "Hasil CPMK", "Rekap Penilaian OBE", listCpmkUnique, mapMhsCpmk,
							targetMinimal, hlink_style, dataStyle, desktop, tabbox);
					generateSheetRangkuman(workbook, "Rangkuman CPMK", "RANGKUMAN KETERCAPAIAN CPMK", listCpmkUnique,
							mapMhsCpmk, targetMinimal, hlink_style, dataStyle, desktop, tabbox);
					generateSheetPenilaian(workbook, "Penilaian CPL", "EVALUASI KETERCAPAIAN CPL", hasilUjianMahasiswas,
							listCpl, mapMhsCpl, mapMhsTotalAkhir, targetMinimal, hlink_style, dataStyle, desktop, tabbox);
					generateSheetHasil(workbook, "Hasil CPL", "Rekap Outcome OBE", listCpl, mapMhsCpl, targetMinimal,
							hlink_style, dataStyle, desktop, tabbox);
					generateSheetRangkuman(workbook, "Rangkuman CPL", "RANGKUMAN KETERCAPAIAN CPL", listCpl, mapMhsCpl,
							targetMinimal, hlink_style, dataStyle, desktop, tabbox);
					generateSheetPenilaian(workbook, "Penilaian PL", "EVALUASI KETERCAPAIAN PROFIL LULUSAN (PL)",
							hasilUjianMahasiswas, listPL, mapMhsPl, mapMhsTotalAkhir, targetMinimal, hlink_style,
							dataStyle, desktop, tabbox);
					generateSheetHasil(workbook, "Hasil PL", "Rekap Outcome OBE PL", listPL, mapMhsPl, targetMinimal,
							hlink_style, dataStyle, desktop, tabbox);
					generateSheetRangkuman(workbook, "Rangkuman PL", "RANGKUMAN KETERCAPAIAN PROFIL LULUSAN", listPL,
							mapMhsPl, targetMinimal, hlink_style, dataStyle, desktop, tabbox);

					// Sheet CQI: ringkasan analisis dosen per CPMK (jika ada data)
					generateSheetCqiSummary(workbook, perkuliahan, hlink_style, hlink_styleFooter,
							dataStyle, desktop, tabbox);

					sizedataCol.setValue(100);
					sizedata.setValue(100);

					// SETELAH SEMUA TAB DAN SHEET SELESAI, BARU KITA SAVE FILE EXCEL FISIKNYA
					try {
						File tempFile = new File(filename + ".tmp");
						FileOutputStream fileOut = new FileOutputStream(tempFile);
						workbook.write(fileOut);
						fileOut.close();

						if (tempFile.exists()) {
							if (file.exists())
								file.delete();
							tempFile.renameTo(file); // <--- SAAT INI TIMER BAWAAN MENDETEKSI FILE!
						}
					} catch (IOException e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:765");
					}

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:769");
				} finally {
					if (sessionLocal != null && sessionLocal.isOpen()) {
						try {
							sessionLocal.close();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:774");
						}
					}
					
					// PEMBERSIHAN FINAL DAN MENYEMBUNYIKAN AUTO-RENDER SPREADSHEET
					try {
						Executions.activate(desktop);
						try {
							label.setValue("");
							// KUNCI PERBAIKAN: Sembunyikan default komponen spreadsheet bawaan Common!
							loadingContainer.setVisible(false); 
						} finally {
							Executions.deactivate(desktop);
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:788");
					}
				}
			}
		});
	}

	// =========================================================================
	// HELPER UI: RENDER TABS LANGSUNG DARI OBJEK SHEET DI MEMORI
	// =========================================================================

	private void addTabFromMemorySheet(Desktop desktop, ais.ui.util.MyButtonTabbox tabbox, XSSFSheet sheet) {
		final String sheetName = sheet == null ? "Sheet" : sheet.getSheetName();
		final String tablePreview = convertSheetToModernPreviewTable(sheet);
		final String excelPreview = convertSheetToHtmlTable(sheet);

		synchronized (excelPreviewHtmlBySheet) {
			excelPreviewHtmlBySheet.put(sheetName, excelPreview);
		}

		try {
			Executions.activate(desktop);
			try {
				org.zkoss.zul.Div tabpanel = tabbox.tambahTab(sheetName, null);
				tabpanel.setStyle("overflow:auto; padding:12px; background:#f8fafc; box-sizing:border-box;");

				Html htmlObj = new Html(tablePreview);
				htmlObj.setParent(tabpanel);
			} finally {
				Executions.deactivate(desktop);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:824");
		}
	}

	private void showExcelPreviewPopup() {
		try {
			final Window win = new Window(Common.getBahasaConfig("Lihat / Download Excel Rekap OBE"), "normal", true);
			win.setWidth("95%");
			win.setHeight("88%");
			win.setPosition("center");
			win.setSizable(true);
			win.setClosable(true);
			win.setStyle("background:#ffffff; border-radius:12px; overflow:hidden;");

			Toolbar toolbar = new Toolbar();
			toolbar.setStyle("background:#f8fafc; border-bottom:1px solid #e5e7eb; padding:8px; min-height:38px;");
			toolbar.setParent(win);

			MyToolbarbuttonConfig download = new MyToolbarbuttonConfig(Common.getBahasaConfig("Download File Excel"),
					"/img/excel.png");
			download.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					try {
						if (file != null && file.exists()) {
							Filedownload.save(file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:852");
					}
				}
			});
			download.setParent(toolbar);

			MyToolbarbuttonConfig close = new MyToolbarbuttonConfig(Common.getBahasaConfig("Tutup"), "/img/cancel.gif");
			close.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					win.detach();
				}
			});
			close.setParent(toolbar);

			// GANTI TAB -> BUTTON GROUP (kelas reusable ais.ui.util.MyButtonTabbox): tab per
			// sheet Excel ini data-driven (jumlahnya tergantung berapa sheet yang sudah
			// selesai diproses), sama seperti pola "Ke-1".."Ke-N" di SetingBiayaAction yang
			// sebelumnya bermasalah blank/scroll pakai Tab/Tabpanel bawaan ZK. Borderlayout
			// bikinan MyButtonTabbox sendiri sudah cukup -- tak perlu lagi
			// Common.tampilanScrollTabbox(win) yang dulu dipakai khusus utk Tabbox bawaan ZK.
			ais.ui.util.MyButtonTabbox tabboxPreview = ais.ui.util.MyButtonTabbox.buat(win, "100%", null);

			Map<String, String> previews = new LinkedHashMap<String, String>();
			synchronized (excelPreviewHtmlBySheet) {
				previews.putAll(excelPreviewHtmlBySheet);
			}

			if (previews.isEmpty()) {
				org.zkoss.zul.Div panel = tabboxPreview.tambahTab(1, Common.getBahasaConfig("Informasi"));
				panel.setStyle("padding:16px; overflow:auto;");
				Html info = new Html("<div style='padding:14px; border:1px solid #fde68a; background:#fffbeb; "
						+ "border-radius:10px; color:#92400e; font-family:Arial,sans-serif; font-size:12px;'>"
						+ "Preview Excel belum tersedia. Silakan klik Refresh sampai proses selesai. "
						+ "Jika file sudah selesai dibuat, tombol Download File Excel tetap dapat digunakan." + "</div>");
				info.setParent(panel);
				tabboxPreview.pilih(1);
			} else {
				int indexSheet = 1;
				for (Map.Entry<String, String> entry : previews.entrySet()) {
					org.zkoss.zul.Div panel = tabboxPreview.tambahTab(indexSheet, entry.getKey());
					panel.setStyle("overflow:auto; padding:10px; background:#f8fafc;");
					Html html = new Html("<div>" + entry.getValue() + "</div>");
					html.setParent(panel);
					indexSheet++;
				}
				tabboxPreview.pilih(1);
			}

			win.setParent(this);
			win.doModal();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:909");
			try {
				if (file != null && file.exists()) {
					Filedownload.save(file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
				}
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:914");
			}
		}
	}

	private String convertSheetToModernPreviewTable(XSSFSheet sheet) {
		if (sheet == null) {
			return "<div style='padding:12px; color:#64748b;'>Data belum tersedia.</div>";
		}

		String title = getCellStringValue(sheet.getRow(0) == null ? null : sheet.getRow(0).getCell(0));
		if (title == null || title.trim().isEmpty()) {
			title = sheet.getSheetName();
		}

		StringBuilder sb = new StringBuilder();
		sb.append("<div style='font-family:Arial,sans-serif; color:#0f172a;'>");
		sb.append("<div style='background:#ffffff; border:1px solid #e5e7eb; border-radius:14px; "
				+ "box-shadow:0 8px 22px rgba(15,23,42,.06); overflow:hidden;'>");
		sb.append("<div style='padding:14px 16px; background:linear-gradient(135deg,#eff6ff,#ffffff); "
				+ "border-bottom:1px solid #e5e7eb;'>");
		sb.append("<div style='font-size:15px; font-weight:800; color:#1e3a8a;'>")
				.append(escapeHtml(title)).append("</div>");
		sb.append("<div style='margin-top:4px; font-size:11px; color:#64748b;'>Tampilan tabel ringan. "
				+ "Header yang sama otomatis digabung agar tabel lebih ringkas. Gunakan tombol <b>Lihat / Download Excel</b> untuk preview format Excel lengkap.</div>");
		sb.append("</div>");
		sb.append(buildSheetDashboardSummaryHtml(sheet));
		sb.append(buildSheetTableHtml(sheet, true));
		sb.append("</div></div>");
		return sb.toString();
	}


	private String buildSheetDashboardSummaryHtml(XSSFSheet sheet) {
		SheetDashboardInfo info = collectSheetDashboardInfo(sheet);
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='padding:12px 14px; background:#ffffff;'>");
		sb.append("<div style='display:flex; flex-wrap:wrap; gap:10px; margin-bottom:12px;'>");
		appendOverviewCard(sb, "Baris Data", Common.numberFormat.get().format(info.dataRows), "Jumlah baris yang terbaca pada tab ini");
		appendOverviewCard(sb, "Kolom", Common.numberFormat.get().format(info.columnCount), "Jumlah kolom aktif pada tabel");
		appendOverviewCard(sb, "Rata-rata", Common.numberFormat.get().format(info.averageScore), "Rata-rata capaian komponen OBE");
		appendOverviewCard(sb, "Tertinggi", Common.numberFormat.get().format(info.maxScore), "Capaian komponen tertinggi");
		appendOverviewCard(sb, "Tercapai", Common.numberFormat.get().format(info.passedCount), "Jumlah nilai >= target " + Common.numberFormat.get().format(info.target));
		sb.append("</div>");
		String sheetName = sheet.getSheetName() == null ? "" : sheet.getSheetName().toLowerCase();
		if (("data".equals(sheetName) || sheetName.indexOf("penilaian") >= 0)
				&& info.dataRows > 0 && info.columnCount <= 8) {
			sb.append("<div style='margin-bottom:12px;padding:12px 14px;border:1px solid #f59e0b;"
					+ "border-left:4px solid #d97706;border-radius:8px;background:#fffbeb;color:#78350f;"
					+ "font-size:12px;line-height:1.6;'>")
				.append("<div style='font-weight:800;margin-bottom:4px;'>Analisis kelengkapan data OBE</div>")
				.append("Peserta kelas sudah ditemukan, tetapi belum ada kolom asesmen OBE yang dapat dihitung. ")
				.append("Angka 0, nilai huruf T, dan status TL pada kondisi ini bukan bukti bahwa seluruh mahasiswa ")
				.append("memperoleh nilai nol; artinya sistem belum menemukan pasangan lengkap antara Sub-CPMK, ")
				.append("komponen tugas/ujian, bobot komponen, dan nilai OBE mahasiswa. Periksa RPS OBE, tautkan ")
				.append("setiap tugas/ujian ke Sub-CPMK beserta bobotnya, pastikan nilai sudah tersimpan, lalu jalankan ")
				.append("Hitung Ulang/Refresh Rekap Nilai OBE.")
				.append("</div>");
		}
		sb.append("<div style='display:flex; flex-wrap:wrap; gap:12px; align-items:stretch;'>");
		sb.append("<div style='flex:1 1 320px; min-width:280px; border:1px solid #e5e7eb; border-radius:14px; padding:12px; background:#f8fafc;'>");
		sb.append("<div style='font-weight:800; color:#0f172a; margin-bottom:8px;'>Trend Komponen OBE</div>");
		sb.append(buildTrendBarsHtml(info));
		sb.append("</div>");
		sb.append("<div style='flex:1 1 260px; min-width:260px; border:1px solid #e5e7eb; border-radius:14px; padding:12px; background:#f8fafc;'>");
		sb.append("<div style='font-weight:800; color:#0f172a; margin-bottom:8px;'>Distribusi Nilai</div>");
		sb.append(buildDistributionChartHtml(info));
		sb.append("</div>");
		sb.append("<div style='flex:1 1 280px; min-width:260px; border:1px solid #e5e7eb; border-radius:14px; padding:12px; background:#f8fafc;'>");
		sb.append("<div style='font-weight:800; color:#0f172a; margin-bottom:8px;'>Spider Web Komponen OBE</div>");
		sb.append(buildSpiderWebChartHtml(info));
		sb.append("</div>");
		sb.append("</div>");
		sb.append("</div>");
		return sb.toString();
	}

	private void appendOverviewCard(StringBuilder sb, String title, String value, String subtitle) {
		sb.append("<div style='flex:1 1 150px; min-width:145px; background:linear-gradient(135deg,#ffffff,#f8fafc); border:1px solid #e5e7eb; border-radius:14px; padding:11px 12px; box-shadow:0 6px 18px rgba(15,23,42,.05);'>");
		sb.append("<div style='font-size:11px; color:#64748b; font-weight:700;'>").append(escapeHtml(title)).append("</div>");
		sb.append("<div style='font-size:22px; line-height:28px; color:#0f172a; font-weight:900;'>").append(escapeHtml(value)).append("</div>");
		sb.append("<div style='font-size:10.5px; color:#94a3b8;'>").append(escapeHtml(subtitle)).append("</div>");
		sb.append("</div>");
	}

		private SheetDashboardInfo collectSheetDashboardInfo(XSSFSheet sheet) {
		SheetDashboardInfo info = new SheetDashboardInfo();
		if (sheet == null) {
			return info;
		}
		int maxRow = sheet.getLastRowNum();
		int maxCol = resolveMaxColumn(sheet, maxRow);
		info.columnCount = maxCol;
		info.target = detectTargetValue(sheet, maxRow, maxCol);
		info.dataRows = countMeaningfulDataRows(sheet, maxRow, maxCol);

		String sheetName = sheet.getSheetName() == null ? "" : sheet.getSheetName().toLowerCase();
		if (sheetName.indexOf("rangkuman") >= 0) {
			collectRangkumanComponentScores(sheet, info, maxRow, maxCol);
		} else if (sheetName.indexOf("hasil") >= 0) {
			collectHasilComponentScores(sheet, info, maxRow, maxCol);
		} else {
			collectPenilaianComponentScores(sheet, info, maxRow, maxCol);
		}

		if (info.scoreCount == 0) {
			collectFallbackComponentScores(sheet, info, maxRow, maxCol);
		}
		info.averageScore = info.scoreCount == 0 ? 0.0 : info.totalScore / info.scoreCount;
		return info;
	}

	private int countMeaningfulDataRows(XSSFSheet sheet, int maxRow, int maxCol) {
		int count = 0;
		int start = detectFirstDataRow(sheet, maxRow, maxCol);
		for (int r = start; r <= maxRow; r++) {
			XSSFRow row = sheet.getRow(r);
			if (row != null && !isRowBlank(row, maxCol)) {
				count++;
			}
		}
		return count;
	}

	private int detectFirstDataRow(XSSFSheet sheet, int maxRow, int maxCol) {
		for (int r = 0; r <= Math.min(maxRow, 12); r++) {
			XSSFRow row = sheet.getRow(r);
			if (row == null) {
				continue;
			}
			for (int c = 0; c < maxCol; c++) {
				String text = normalizeHeaderText(getHeaderCellStringValue(sheet, r, c));
				if ("nim/no. reg".equals(text) || "nim".equals(text) || "nama mahasiswa".equals(text)) {
					return r + 1;
				}
			}
		}
		return 4;
	}

	private void collectPenilaianComponentScores(XSSFSheet sheet, SheetDashboardInfo info, int maxRow, int maxCol) {
		int dataStart = detectFirstDataRow(sheet, maxRow, maxCol);
		int headerLimit = Math.max(0, dataStart - 1);
		for (int c = 0; c < maxCol; c++) {
			String label = resolveObeComponentLabel(sheet, c, headerLimit);
			if (label == null || label.trim().length() == 0) {
				continue;
			}
			if (!isObeScoreColumn(sheet, c, headerLimit)) {
				continue;
			}
			for (int r = dataStart; r <= maxRow; r++) {
				XSSFRow row = sheet.getRow(r);
				if (row == null) {
					continue;
				}
				Double value = parseDashboardNumber(getCellStringValue(row.getCell(c)));
				addScoreToDashboardInfo(info, label, value);
			}
		}
	}

	private void collectRangkumanComponentScores(XSSFSheet sheet, SheetDashboardInfo info, int maxRow, int maxCol) {
		for (int r = 0; r <= maxRow; r++) {
			XSSFRow row = sheet.getRow(r);
			if (row == null) {
				continue;
			}
			String kode = getCellStringValue(row.getCell(1));
			String header = normalizeHeaderText(kode);
			if (kode == null || kode.trim().length() == 0 || "kode".equals(header)) {
				continue;
			}
			Double value = parseDashboardNumber(getCellStringValue(row.getCell(3)));
			addScoreToDashboardInfo(info, kode.trim(), value);
		}
	}

	private void collectHasilComponentScores(XSSFSheet sheet, SheetDashboardInfo info, int maxRow, int maxCol) {
		String currentLabel = null;
		int lulus = 0;
		int tidakLulus = 0;
		for (int r = 0; r <= maxRow + 1; r++) {
			XSSFRow row = r <= maxRow ? sheet.getRow(r) : null;
			String c1 = row == null ? "" : getCellStringValue(row.getCell(1));
			String c2 = row == null ? "" : getCellStringValue(row.getCell(2));
			boolean newBlock = c1 != null && c1.trim().length() > 0 && c1.toLowerCase().indexOf("rekap") >= 0;
			if (newBlock || r == maxRow + 1) {
				if (currentLabel != null) {
					int total = lulus + tidakLulus;
					if (total > 0) {
						addScoreToDashboardInfo(info, currentLabel, Double.valueOf(lulus * 100.0 / total));
					}
				}
				currentLabel = newBlock ? extractComponentCodeFromTitle(c1) : null;
				lulus = 0;
				tidakLulus = 0;
				continue;
			}
			if (currentLabel == null || c1 == null) {
				continue;
			}
			String lower = c1.trim().toLowerCase();
			int jumlah = parseIntegerSafe(c2);
			if (lower.indexOf("tidak") >= 0 && lower.indexOf("lulus") >= 0) {
				tidakLulus += jumlah;
			} else if (lower.indexOf("lulus") >= 0) {
				lulus += jumlah;
			}
		}
	}

	private void collectFallbackComponentScores(XSSFSheet sheet, SheetDashboardInfo info, int maxRow, int maxCol) {
		int dataStart = detectFirstDataRow(sheet, maxRow, maxCol);
		int headerLimit = Math.max(0, dataStart - 1);
		for (int c = 0; c < maxCol; c++) {
			String label = resolveColumnLabel(sheet, c, headerLimit);
			String lower = label == null ? "" : label.toLowerCase();
			if (lower.indexOf("cpmk") < 0 && lower.indexOf("cpl") < 0 && lower.indexOf("pl") < 0
					&& lower.indexOf("nilai akhir") < 0 && lower.indexOf("konversi") < 0
					&& lower.indexOf("ujian") < 0 && lower.indexOf("tugas") < 0 && lower.indexOf("uts") < 0
					&& lower.indexOf("uas") < 0) {
				continue;
			}
			for (int r = dataStart; r <= maxRow; r++) {
				XSSFRow row = sheet.getRow(r);
				if (row == null) {
					continue;
				}
				addScoreToDashboardInfo(info, label, parseDashboardNumber(getCellStringValue(row.getCell(c))));
			}
		}
	}

	private boolean isObeScoreColumn(XSSFSheet sheet, int col, int maxHeaderRow) {
		String allHeaders = getJoinedHeaderText(sheet, col, maxHeaderRow);
		String h = allHeaders == null ? "" : allHeaders.toLowerCase();
		if (col <= 4) {
			return false;
		}
		if (h.indexOf("skala") >= 0 || h.indexOf("status") >= 0 || h.indexOf("kategori") >= 0
				|| h.indexOf("l/tl") >= 0 || h.indexOf("huruf") >= 0) {
			return false;
		}

		String parent = normalizeHeaderText(getParentHeaderText(sheet, col, maxHeaderRow));
		String child = normalizeHeaderText(getBestChildHeaderText(sheet, col, maxHeaderRow));
		boolean hasObeParent = parent.indexOf("sub-cpmk") >= 0 || parent.indexOf("cpmk") >= 0
				|| parent.indexOf("cpl") >= 0 || parent.indexOf("pl") >= 0;

		if (h.indexOf("nilai konversi") >= 0 || h.indexOf("total sub-cpmk") >= 0
				|| h.indexOf("total cpmk") >= 0 || h.indexOf("total cpl") >= 0 || h.indexOf("total pl") >= 0) {
			return true;
		}

		/*
		 * Untuk tab DATA, kolom asesmen/tugas berada di bawah header Sub-CPMK
		 * yang di-merge. Kolom ini valid untuk chart, walaupun header paling bawah
		 * berisi persentase seperti 1,11% atau 3,33%. Sebelumnya kondisi ini tidak
		 * terbaca sehingga fallback mengambil label dari baris nilai mahasiswa dan
		 * label chart berubah menjadi angka 22.22, 33.33, 0, dan sejenisnya.
		 */
		if (hasObeParent && child.length() > 0) {
			return true;
		}

		String lastHeader = normalizeHeaderText(getLastHeaderText(sheet, col, maxHeaderRow));
		return "nilai".equals(lastHeader) && hasObeParent;
	}

	private String resolveObeComponentLabel(XSSFSheet sheet, int col, int maxHeaderRow) {
		String parent = getParentHeaderText(sheet, col, maxHeaderRow);
		String child = getBestChildHeaderText(sheet, col, maxHeaderRow);
		String joined = getJoinedHeaderText(sheet, col, maxHeaderRow);
		String parentLower = parent == null ? "" : parent.toLowerCase();
		String childLower = child == null ? "" : child.toLowerCase();
		String joinedLower = joined == null ? "" : joined.toLowerCase();

		String base = "";
		if (parentLower.indexOf("sub-cpmk") >= 0 || parentLower.indexOf("cpmk") >= 0
				|| parentLower.indexOf("cpl") >= 0 || parentLower.indexOf("pl") >= 0) {
			base = parent;
		} else if (joinedLower.indexOf("sub-cpmk") >= 0 || joinedLower.indexOf("cpmk") >= 0
				|| joinedLower.indexOf("cpl") >= 0 || joinedLower.indexOf("pl") >= 0) {
			base = findNearestObeHeader(sheet, col, maxHeaderRow);
		}

		if (base == null) {
			base = "";
		}
		base = base.trim();

		if (child == null) {
			child = "";
		}
		child = child.trim();

		if (childLower.indexOf("nilai konversi") >= 0 || childLower.indexOf("total sub-cpmk") >= 0
				|| childLower.indexOf("total cpmk") >= 0 || childLower.indexOf("total cpl") >= 0
				|| childLower.indexOf("total pl") >= 0) {
			return cleanupComponentLabel(base.length() == 0 ? child : base + " - " + child);
		}

		if (base.length() > 0 && child.length() > 0 && !base.equalsIgnoreCase(child)) {
			return cleanupComponentLabel(base + " - " + child);
		}

		if (base.length() > 0) {
			return cleanupComponentLabel(base);
		}

		if (child.length() > 0) {
			return cleanupComponentLabel(child);
		}

		return cleanupComponentLabel(getLastHeaderText(sheet, col, maxHeaderRow));
	}

	private String findNearestObeHeader(XSSFSheet sheet, int col, int maxRow) {
		for (int r = Math.min(maxRow, 8); r >= 0; r--) {
			String text = getHeaderCellStringValue(sheet, r, col);
			if (text != null) {
				String lower = text.toLowerCase();
				if (lower.indexOf("sub-cpmk") >= 0 || lower.indexOf("cpmk") >= 0 || lower.indexOf("cpl") >= 0
						|| lower.indexOf("pl") >= 0) {
					return text.trim();
				}
			}
		}
		return "";
	}

	private String getHeaderCellStringValue(XSSFSheet sheet, int rowIndex, int colIndex) {
		if (sheet == null || rowIndex < 0 || colIndex < 0) {
			return "";
		}
		String direct = getCellStringValue(sheet.getRow(rowIndex) == null ? null : sheet.getRow(rowIndex).getCell(colIndex));
		if (direct != null && direct.trim().length() > 0) {
			return direct.trim();
		}
		try {
			for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
				CellRangeAddress region = sheet.getMergedRegion(i);
				if (region != null && rowIndex >= region.getFirstRow() && rowIndex <= region.getLastRow()
						&& colIndex >= region.getFirstColumn() && colIndex <= region.getLastColumn()) {
					String merged = getCellStringValue(sheet.getRow(region.getFirstRow()) == null ? null
							: sheet.getRow(region.getFirstRow()).getCell(region.getFirstColumn()));
					return merged == null ? "" : merged.trim();
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:1249");
		}
		return "";
	}

	private String getJoinedHeaderText(XSSFSheet sheet, int col, int maxHeaderRow) {
		StringBuilder sb = new StringBuilder();
		for (int r = 0; r <= maxHeaderRow; r++) {
			String text = getHeaderCellStringValue(sheet, r, col);
			if (text != null && text.trim().length() > 0) {
				if (sb.length() > 0) sb.append(' ');
				sb.append(text.trim());
			}
		}
		return sb.toString();
	}

	private String getBestChildHeaderText(XSSFSheet sheet, int col, int maxHeaderRow) {
		String candidate = "";
		for (int r = maxHeaderRow; r >= 0; r--) {
			String text = getHeaderCellStringValue(sheet, r, col);
			if (text == null || text.trim().length() == 0) {
				continue;
			}
			String normalized = normalizeHeaderText(text);
			if (isIgnoredChartHeaderText(normalized)) {
				continue;
			}
			if (isNumericLikeHeader(normalized)) {
				continue;
			}
			candidate = text.trim();
			break;
		}
		return candidate;
	}

	private boolean isIgnoredChartHeaderText(String normalized) {
		if (normalized == null) {
			return true;
		}
		return "no.".equals(normalized) || "no".equals(normalized) || "nim".equals(normalized)
				|| "nim/no. reg".equals(normalized) || "nama".equals(normalized)
				|| "nama mahasiswa".equals(normalized) || "prodi".equals(normalized)
				|| "program".equals(normalized) || normalized.indexOf("rekap penilian obe") >= 0
				|| normalized.indexOf("rumus:") >= 0 || normalized.indexOf("evaluasi ketercapaian") >= 0
				|| normalized.indexOf("rangkuman ketercapaian") >= 0;
	}

	private boolean isNumericLikeHeader(String normalized) {
		if (normalized == null || normalized.length() == 0) {
			return true;
		}
		String s = normalized.replace("%", "").replace(",", ".").trim();
		boolean hasDigit = false;
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			if (ch >= '0' && ch <= '9') {
				hasDigit = true;
			} else if (ch == '.' || ch == '-' || ch == ' ') {
				continue;
			} else {
				return false;
			}
		}
		return hasDigit;
	}

	private String getLastHeaderText(XSSFSheet sheet, int col, int maxHeaderRow) {
		for (int r = maxHeaderRow; r >= 0; r--) {
			String text = getHeaderCellStringValue(sheet, r, col);
			if (text != null && text.trim().length() > 0) {
				return text.trim();
			}
		}
		return "";
	}

	private String getParentHeaderText(XSSFSheet sheet, int col, int maxHeaderRow) {
		String last = "";
		for (int r = 0; r <= maxHeaderRow; r++) {
			String text = getHeaderCellStringValue(sheet, r, col);
			if (text != null && text.trim().length() > 0) {
				String lower = text.toLowerCase();
				if (lower.indexOf("sub-cpmk") >= 0 || lower.indexOf("cpmk") >= 0 || lower.indexOf("cpl") >= 0
						|| lower.indexOf("pl") >= 0 || lower.indexOf("nilai akhir") >= 0) {
					last = text.trim();
				}
			}
		}
		return last;
	}

	private String cleanupComponentLabel(String label) {
		if (label == null) {
			return "";
		}
		String v = label.trim();
		if (v.length() == 0 || isNumericLikeHeader(normalizeHeaderText(v))) {
			return "";
		}
		if (v.length() > 60) {
			v = v.substring(0, 57) + "...";
		}
		return v;
	}

	private String extractComponentCodeFromTitle(String title) {
		if (title == null) {
			return "Komponen";
		}
		String[] parts = title.trim().split("\\s+");
		for (int i = parts.length - 1; i >= 0; i--) {
			String p = parts[i].replaceAll("[^A-Za-z0-9\\-]", "");
			String lower = p.toLowerCase();
			if (lower.indexOf("cpmk") >= 0 || lower.indexOf("cpl") >= 0 || lower.equals("pl") || lower.startsWith("pl")) {
				return p;
			}
		}
		return cleanupComponentLabel(title);
	}

	private String normalizeHeaderText(String text) {
		return text == null ? "" : text.trim().toLowerCase();
	}

	private int parseIntegerSafe(String text) {
		Double d = parseDashboardNumber(text);
		return d == null ? 0 : d.intValue();
	}

	private void addScoreToDashboardInfo(SheetDashboardInfo info, String label, Double value) {
		if (info == null || value == null || value.isNaN() || value.isInfinite()) {
			return;
		}
		double v = value.doubleValue();
		if (v < 0.0 || v > 100.0) {
			return;
		}
		String safeLabel = label == null || label.trim().length() == 0 ? "Komponen" : label.trim();
		info.totalScore += v;
		info.scoreCount++;
		if (v > info.maxScore) {
			info.maxScore = v;
		}
		if (v >= info.target) {
			info.passedCount++;
		} else {
			info.failedCount++;
		}
		if (v >= 80.0) {
			info.bucketA++;
		} else if (v >= 70.0) {
			info.bucketB++;
		} else if (v >= 60.0) {
			info.bucketC++;
		} else {
			info.bucketD++;
		}
		ColumnScoreSummary col = null;
		for (ColumnScoreSummary existing : info.columnScores.values()) {
			if (existing != null && safeLabel.equals(existing.label)) {
				col = existing;
				break;
			}
		}
		if (col == null) {
			col = new ColumnScoreSummary();
			col.label = safeLabel;
			info.columnScores.put(Integer.valueOf(info.columnScores.size()), col);
		}
		col.total += v;
		col.count++;
	}

	private double detectTargetValue(XSSFSheet sheet, int maxRow, int maxCol) {
		double fallback = 60.0;
		if (sheet == null) {
			return fallback;
		}
		for (int r = 0; r <= Math.min(maxRow, 8); r++) {
			XSSFRow row = sheet.getRow(r);
			if (row == null) {
				continue;
			}
			for (int c = 0; c < maxCol; c++) {
				String text = getHeaderCellStringValue(sheet, r, c);
				if (text == null) {
					continue;
				}
				String lower = text.toLowerCase();
				if (lower.indexOf("target") >= 0 || lower.indexOf("minimal") >= 0) {
					Double value = parseDashboardNumber(text);
					if (value != null && value.doubleValue() > 0.0 && value.doubleValue() <= 100.0) {
						return value.doubleValue();
					}
				}
			}
		}
		return fallback;
	}

	private String resolveColumnLabel(XSSFSheet sheet, int col, int maxRow) {
		String best = "Kolom " + (col + 1);
		for (int r = Math.min(maxRow, 6); r >= 0; r--) {
			String text = getHeaderCellStringValue(sheet, r, col);
			if (text != null && text.trim().length() > 0) {
				text = text.trim();
				if (text.length() > 38) {
					text = text.substring(0, 35) + "...";
				}
				best = text;
				break;
			}
		}
		return best;
	}

	private Double parseDashboardNumber(String text) {
		if (text == null) {
			return null;
		}
		String s = text.trim();
		if (s.length() == 0) {
			return null;
		}
		int idxParen = s.indexOf('(');
		if (idxParen > 0) {
			s = s.substring(0, idxParen).trim();
		}
		if (s.endsWith("%")) {
			s = s.substring(0, s.length() - 1).trim();
		}
		if (s.indexOf(',') >= 0) {
			s = s.replace(".", "").replace(',', '.');
		}
		StringBuilder clean = new StringBuilder();
		boolean dotSeen = false;
		boolean digitSeen = false;
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			if ((ch >= '0' && ch <= '9') || (ch == '-' && clean.length() == 0)) {
				clean.append(ch);
				if (ch >= '0' && ch <= '9') digitSeen = true;
			} else if (ch == '.' && !dotSeen) {
				clean.append(ch);
				dotSeen = true;
			} else if (digitSeen) {
				break;
			}
		}
		if (!digitSeen) {
			return null;
		}
		try {
			return Double.valueOf(clean.toString());
		} catch (Exception e) {
			return null;
		}
	}

	private String buildTrendBarsHtml(SheetDashboardInfo info) {
		StringBuilder sb = new StringBuilder();
		List<ColumnScoreSummary> summaries = new ArrayList<ColumnScoreSummary>(info.columnScores.values());
		for (int i = summaries.size() - 1; i >= 0; i--) {
			if (summaries.get(i).count <= 0) summaries.remove(i);
		}
		for (int i = 0; i < summaries.size() - 1; i++) {
			for (int j = i + 1; j < summaries.size(); j++) {
				if (summaries.get(j).average() > summaries.get(i).average()) {
					ColumnScoreSummary tmp = summaries.get(i);
					summaries.set(i, summaries.get(j));
					summaries.set(j, tmp);
				}
			}
		}
		int limit = Math.min(8, summaries.size());
		if (limit == 0) {
			return "<div style='font-size:11px; color:#94a3b8;'>Belum ada data numerik untuk chart.</div>";
		}
		for (int i = 0; i < limit; i++) {
			ColumnScoreSummary c = summaries.get(i);
			double avg = c.average();
			int width = (int) Math.max(2, Math.min(100, avg));
			sb.append("<div style='margin-bottom:8px;'>");
			sb.append("<div style='display:flex; justify-content:space-between; gap:8px; font-size:10.5px; color:#334155;'>");
			sb.append("<span style='overflow:hidden; text-overflow:ellipsis;'>").append(escapeHtml(c.label)).append("</span>");
			sb.append("<b>").append(Common.numberFormat.get().format(avg)).append("</b>");
			sb.append("</div>");
			sb.append("<div style='height:9px; background:#e2e8f0; border-radius:999px; overflow:hidden; margin-top:3px;'>");
			sb.append("<div style='height:9px; width:").append(width).append("%; background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4)); border-radius:999px;'></div>");
			sb.append("</div></div>");
		}
		return sb.toString();
	}

	private String buildDistributionChartHtml(SheetDashboardInfo info) {
		int total = info.bucketA + info.bucketB + info.bucketC + info.bucketD;
		if (total <= 0) {
			return "<div style='font-size:11px; color:#94a3b8;'>Belum ada distribusi nilai.</div>";
		}
		StringBuilder sb = new StringBuilder();
		appendDistributionBar(sb, "80-100", info.bucketA, total, "#16a34a");
		appendDistributionBar(sb, "70-79", info.bucketB, total, "#2563eb");
		appendDistributionBar(sb, "60-69", info.bucketC, total, "#f59e0b");
		appendDistributionBar(sb, "<60", info.bucketD, total, "#ef4444");
		return sb.toString();
	}

	private void appendDistributionBar(StringBuilder sb, String label, int value, int total, String color) {
		int width = total <= 0 ? 0 : (int) Math.max(2, Math.round(value * 100.0 / total));
		sb.append("<div style='display:flex; align-items:center; gap:8px; margin-bottom:9px;'>");
		sb.append("<div style='width:56px; font-size:10.5px; color:#334155; font-weight:700;'>").append(label).append("</div>");
		sb.append("<div style='flex:1; height:20px; background:#e2e8f0; border-radius:999px; overflow:hidden;'>");
		sb.append("<div style='height:20px; width:").append(width).append("%; background:").append(color).append(";'></div>");
		sb.append("</div>");
		sb.append("<div style='width:38px; text-align:right; font-size:11px; color:#0f172a; font-weight:800;'>").append(value).append("</div>");
		sb.append("</div>");
	}

		private String buildSpiderWebChartHtml(SheetDashboardInfo info) {
		List<ColumnScoreSummary> summaries = new ArrayList<ColumnScoreSummary>(info.columnScores.values());
		for (int i = summaries.size() - 1; i >= 0; i--) {
			if (summaries.get(i).count <= 0) summaries.remove(i);
		}
		int limit = Math.min(8, summaries.size());
		if (limit == 0) {
			return "<div style='font-size:11px; color:#94a3b8;'>Belum ada data komponen OBE untuk spider web chart.</div>";
		}
		StringBuilder points = new StringBuilder();
		StringBuilder targetPoints = new StringBuilder();
		StringBuilder labels = new StringBuilder();
		double center = 130.0;
		double radius = 70.0;
		for (int i = 0; i < limit; i++) {
			ColumnScoreSummary c = summaries.get(i);
			double angle = (-90.0 + (360.0 * i / limit)) * Math.PI / 180.0;
			double avg = Math.max(0.0, Math.min(100.0, c.average()));
			double valRadius = radius * avg / 100.0;
			double x = center + Math.cos(angle) * valRadius;
			double y = center + Math.sin(angle) * valRadius;
			if (points.length() > 0) points.append(' ');
			points.append(formatSvgNumber(x)).append(',').append(formatSvgNumber(y));
			double targetRadius = radius * Math.max(0.0, Math.min(100.0, info.target)) / 100.0;
			double tx = center + Math.cos(angle) * targetRadius;
			double ty = center + Math.sin(angle) * targetRadius;
			if (targetPoints.length() > 0) targetPoints.append(' ');
			targetPoints.append(formatSvgNumber(tx)).append(',').append(formatSvgNumber(ty));
			double lx = center + Math.cos(angle) * (radius + 28.0);
			double ly = center + Math.sin(angle) * (radius + 28.0);
			labels.append("<text x='").append(formatSvgNumber(lx)).append("' y='").append(formatSvgNumber(ly))
					.append("' text-anchor='middle' dominant-baseline='middle' font-size='9' font-weight='700' fill='#475569'>")
					.append(escapeHtml(shortLabel(c.label, 12))).append("</text>");
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='display:flex; align-items:center; justify-content:center; overflow:visible;'>");
		sb.append("<svg viewBox='0 0 260 230' width='100%' height='230' style='max-width:320px; overflow:visible; background:#ffffff; border:1px solid #e2e8f0; border-radius:14px;'>");
		sb.append("<circle cx='130' cy='115' r='70' fill='#f8fafc' stroke='#e2e8f0'/><circle cx='130' cy='115' r='47' fill='none' stroke='#e2e8f0'/><circle cx='130' cy='115' r='24' fill='none' stroke='#e2e8f0'/>");
		// Karena center grafik di y=115, geser polygon dari perhitungan center y=130 ke y=115.
		sb.append("<g transform='translate(0,-15)'>");
		sb.append("<polygon points='").append(targetPoints).append("' fill='rgba(245,158,11,.10)' stroke='#f59e0b' stroke-width='1.5' stroke-dasharray='4 3'/>");
		sb.append("<polygon points='").append(points).append("' fill='rgba(37,99,235,.20)' stroke='#2563eb' stroke-width='2'/>");
		sb.append("</g>");
		sb.append("<g transform='translate(0,-15)'>").append(labels).append("</g>");
		sb.append("</svg></div>");
		sb.append("<div style='font-size:10px;color:#64748b;text-align:center;margin-top:4px;'>Garis oranye = target, area biru = rata-rata capaian komponen.</div>");
		return sb.toString();
	}

	private String formatSvgNumber(double value) {
		return Common.numberFormat.get().format(value).replace(',', '.');
	}

	private String shortLabel(String value, int max) {
		if (value == null) return "";
		String v = value.trim();
		return v.length() <= max ? v : v.substring(0, max) + "...";
	}

	/**
	 * Tipe implementasi bersarang {@link SheetDashboardInfo} milik {@link RekapHasilTugasPerTugasDanUjianObe}.
	 * Kelas ini memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok
	 * anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * RekapHasilTugasPerTugasDanUjianObe}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API
	 * kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int dataRows}, {@code int
	 * columnCount}, {@code int scoreCount}, {@code int passedCount}, {@code int failedCount}, {@code int bucketA},
	 * {@code int bucketB}, {@code int bucketC}. Aturan bisnis bersama tetap berada pada kelas induk atau service
	 * yang dipanggilnya.</p>
	 *
	 * @see RekapHasilTugasPerTugasDanUjianObe
	 */
	private static class SheetDashboardInfo {
		int dataRows;
		int columnCount;
		int scoreCount;
		int passedCount;
		int failedCount;
		int bucketA;
		int bucketB;
		int bucketC;
		int bucketD;
		double target = 60.0;
		double totalScore;
		double averageScore;
		double maxScore;
		Map<Integer, ColumnScoreSummary> columnScores = new LinkedHashMap<Integer, ColumnScoreSummary>();
	}

	/**
	 * Pembawa data/helper lokal milik {@link RekapHasilTugasPerTugasDanUjianObe} untuk column score summary. Tipe
	 * ini mengelompokkan nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang
	 * jelas.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * RekapHasilTugasPerTugasDanUjianObe}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API
	 * kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String label}, {@code double total},
	 * {@code int count}; operasi lokal: {@code average}(). Aturan bisnis bersama tetap berada pada kelas induk
	 * atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see RekapHasilTugasPerTugasDanUjianObe
	 */
	private static class ColumnScoreSummary {
		String label;
		double total;
		int count;
		double average() {
			return count <= 0 ? 0.0 : total / count;
		}
	}

	private int resolveMaxColumn(XSSFSheet sheet, int maxRow) {
		int maxCol = 0;
		if (sheet == null) {
			return 0;
		}
		for (int r = 0; r <= maxRow; r++) {
			XSSFRow row = sheet.getRow(r);
			if (row != null && row.getLastCellNum() > maxCol) {
				maxCol = row.getLastCellNum();
			}
		}
		return maxCol;
	}

	private boolean isRowBlank(XSSFRow row, int maxCol) {
		if (row == null) {
			return true;
		}
		for (int c = 0; c < maxCol; c++) {
			String value = getCellStringValue(row.getCell(c));
			if (value != null && value.trim().length() > 0) {
				return false;
			}
		}
		return true;
	}

	private int countNonBlankCells(XSSFRow row, int maxCol) {
		if (row == null) {
			return 0;
		}
		int count = 0;
		for (int c = 0; c < maxCol; c++) {
			String value = getCellStringValue(row.getCell(c));
			if (value != null && value.trim().length() > 0) {
				count++;
			}
		}
		return count;
	}

	private int firstNonBlankColumn(XSSFRow row, int maxCol) {
		if (row == null) {
			return -1;
		}
		for (int c = 0; c < maxCol; c++) {
			String value = getCellStringValue(row.getCell(c));
			if (value != null && value.trim().length() > 0) {
				return c;
			}
		}
		return -1;
	}

	private String getCellStringValue(XSSFCell cell) {
		if (cell == null) {
			return "";
		}
		try {
			String value = cell.toString();
			if (value == null) {
				return "";
			}
			if (value.endsWith(".0")) {
				value = value.substring(0, value.length() - 2);
			}
			return value;
		} catch (Exception e) {
			return "";
		}
	}

	private String escapeHtml(String value) {
		if (value == null) {
			return "";
		}
		String text = value;
		text = text.replace("&", "&amp;");
		text = text.replace("<", "&lt;");
		text = text.replace(">", "&gt;");
		text = text.replace("\"", "&quot;");
		text = text.replace("'", "&#39;");
		return text;
	}

	private String convertSheetToHtmlTable(XSSFSheet sheet) {
		return buildSheetTableHtml(sheet, false);
	}

	private String buildSheetTableHtml(XSSFSheet sheet, boolean previewRingan) {
		if (sheet == null) {
			return "<div style='padding:12px; color:#64748b;'>Data belum tersedia.</div>";
		}

		StringBuilder sb = new StringBuilder();
		int maxRow = sheet.getLastRowNum();
		int maxCol = resolveMaxColumn(sheet, maxRow);
		if (maxCol <= 0) {
			return "<div style='padding:12px; color:#64748b;'>Data kosong.</div>";
		}

		String wrapperStyle = previewRingan
				? "width:100%; overflow:auto; max-height:620px; background:#ffffff;"
				: "width:100%; overflow:auto; background:#f8fafc; padding:10px; box-sizing:border-box;";
		String tableStyle = previewRingan
				? "border-collapse:separate; border-spacing:0; min-width:100%; font-family:Arial,sans-serif; font-size:11.5px; background:#ffffff; white-space:nowrap;"
				: "border-collapse:separate; border-spacing:0; font-family:Arial,sans-serif; font-size:12px; min-width:100%; white-space:nowrap; background:#ffffff; border:1px solid #e2e8f0; border-radius:12px; overflow:hidden; box-shadow:0 8px 22px rgba(15,23,42,.07);";

		sb.append("<div style=\"").append(wrapperStyle).append("\">");
		sb.append("<table style=\"").append(tableStyle).append("\">");

		boolean[][] mergedCells = new boolean[maxRow + 1][maxCol + 1];
		Map<String, int[]> mergeInfo = new HashMap<String, int[]>();
		for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
			CellRangeAddress region = sheet.getMergedRegion(i);
			int firstRow = Math.max(0, region.getFirstRow());
			int lastRow = Math.min(maxRow, region.getLastRow());
			int firstCol = Math.max(0, region.getFirstColumn());
			int lastCol = Math.min(maxCol - 1, region.getLastColumn());
			if (firstRow > lastRow || firstCol > lastCol) {
				continue;
			}
			mergeInfo.put(firstRow + "_" + firstCol, new int[] { lastRow - firstRow + 1, lastCol - firstCol + 1 });
			for (int r = firstRow; r <= lastRow; r++) {
				for (int c = firstCol; c <= lastCol; c++) {
					if (r == firstRow && c == firstCol) {
						continue;
					}
					mergedCells[r][c] = true;
				}
			}
		}

		for (int r = 0; r <= maxRow; r++) {
			XSSFRow row = sheet.getRow(r);
			if (row == null || isRowBlank(row, maxCol)) {
				continue;
			}

			int nonBlank = countNonBlankCells(row, maxCol);
			int firstNonBlank = firstNonBlankColumn(row, maxCol);
			if (r <= 2 && nonBlank == 1 && firstNonBlank >= 0) {
				String titleValue = getCellStringValue(row.getCell(firstNonBlank));
				sb.append("<tr><td colspan=\"").append(maxCol).append("\" style=\"")
						.append(resolveHeaderCellStyle(r, firstNonBlank, true, previewRingan))
						.append(" text-align:left; font-size:").append(r == 0 ? "13px" : "11.5px")
						.append(";\">").append(escapeHtml(titleValue).replace("\n", "<br/>"))
						.append("</td></tr>");
				continue;
			}

			sb.append("<tr>");
			for (int c = 0; c < maxCol; c++) {
				if (r < mergedCells.length && c < mergedCells[r].length && mergedCells[r][c]) {
					continue;
				}
				if (isAutoMergedHeaderSkip(sheet, r, c, maxRow)) {
					continue;
				}

				XSSFCell cell = row.getCell(c);
				String cellValue = getCellStringValue(cell);
				int rowspan = 1;
				int colspan = 1;
				int[] merge = mergeInfo.get(r + "_" + c);
				if (merge != null) {
					rowspan = merge[0];
					colspan = merge[1];
				} else {
					rowspan = resolveAutoHeaderRowspan(sheet, r, c, maxRow);
				}

				String style = resolveCellStyle(cell, r, c, previewRingan);
				sb.append("<td");
				if (rowspan > 1) {
					sb.append(" rowspan=\"").append(rowspan).append("\"");
				}
				if (colspan > 1) {
					sb.append(" colspan=\"").append(colspan).append("\"");
				}
				sb.append(" style=\"").append(style).append("\">");
				sb.append(escapeHtml(cellValue).replace("\n", "<br/>"));
				sb.append("</td>");
			}
			sb.append("</tr>");
		}
		sb.append("</table></div>");
		return sb.toString();
	}

	private boolean isAutoMergedHeaderSkip(XSSFSheet sheet, int rowIndex, int columnIndex, int maxRow) {
		if (sheet == null || rowIndex <= 0 || rowIndex > 6 || columnIndex > 6) {
			return false;
		}
		String current = getCellStringValue(sheet.getRow(rowIndex) == null ? null : sheet.getRow(rowIndex).getCell(columnIndex));
		if (current == null || current.trim().length() == 0) {
			return false;
		}
		String previous = getCellStringValue(sheet.getRow(rowIndex - 1) == null ? null : sheet.getRow(rowIndex - 1).getCell(columnIndex));
		return current.equalsIgnoreCase(previous);
	}

	private int resolveAutoHeaderRowspan(XSSFSheet sheet, int rowIndex, int columnIndex, int maxRow) {
		if (sheet == null || rowIndex > 6 || columnIndex > 6) {
			return 1;
		}
		String value = getCellStringValue(sheet.getRow(rowIndex) == null ? null : sheet.getRow(rowIndex).getCell(columnIndex));
		if (value == null || value.trim().length() == 0) {
			return 1;
		}
		int span = 1;
		for (int r = rowIndex + 1; r <= Math.min(maxRow, 6); r++) {
			String next = getCellStringValue(sheet.getRow(r) == null ? null : sheet.getRow(r).getCell(columnIndex));
			if (value.equalsIgnoreCase(next)) {
				span++;
			} else {
				break;
			}
		}
		return span;
	}

	private String resolveHeaderCellStyle(int rowIndex, int columnIndex, boolean fullWidth, boolean previewRingan) {
		String style = "padding:9px 11px; border:1px solid #bfdbfe; background:#1d4ed8; color:#ffffff; font-weight:800;";
		if (rowIndex == 0) {
			style += "background:#1e3a8a;";
		}
		return style;
	}

	private String resolveCellStyle(XSSFCell cell, int rowIndex, int columnIndex, boolean previewRingan) {
		String style = "padding:7px 9px; border:1px solid #e2e8f0; color:#0f172a; background:#ffffff; vertical-align:middle;";
		if (rowIndex <= 4) {
			style += "font-weight:800; text-align:center;";
		}
		if (!previewRingan && rowIndex <= 3) {
			style += "position:sticky; top:0; z-index:2;";
		}
		if (!previewRingan && columnIndex <= 2) {
			style += "position:sticky; left:0; z-index:3;";
		}
		if (cell != null && cell.getCellStyle() != null) {
			short colorIdx = cell.getCellStyle().getFillForegroundColor();
			if (colorIdx == IndexedColors.GREY_50_PERCENT.getIndex()
					|| colorIdx == IndexedColors.GREY_40_PERCENT.getIndex()) {
				style += "background:#1d4ed8; color:#ffffff;";
			} else if (colorIdx == IndexedColors.ORANGE.getIndex()) {
				style += "background:#fef3c7; color:#92400e;";
			} else if (colorIdx == IndexedColors.GREY_25_PERCENT.getIndex()) {
				style += "background:#f8fafc;";
			}
		}
		if (rowIndex <= 4 && (cell == null || getCellStringValue(cell).trim().length() == 0)) {
			style += "background:#1d4ed8; color:#ffffff;";
		}
		if (rowIndex > 4 && columnIndex <= 2) {
			style += "font-weight:700; background:#f8fafc;";
		}
		return style;
	}

	// =========================================================================
	// HELPER POI BUIDLERS UNTUK SHEET OBE
	// =========================================================================

	private List<StudentObeResult> hitungNilaiObeMahasiswaParallel(final List<Mahasiswa> mahasiswaList,
			final List<ObeFormatColumn> formatColumns,
			final Map<Long, Map<Long, Map<String, Double>>> dataNiliasUtama,
			final Map<String, Double> persensData, final List<CapaianPembelajaranLulusan> listCpmkUnique,
			final List<CapaianLulusan> listCpl, final List<ProfilLulusan> listPL,
			final Map<Long, List<FormatNilai>> mapCpmkIdToFns, final double targetMinimal, final Desktop desktop,
			final Label label) {
		List<StudentObeResult> results = new ArrayList<StudentObeResult>();
		if (mahasiswaList == null || mahasiswaList.isEmpty()) {
			return results;
		}

		int workerCount = Math.min(MAX_OBE_PARALLEL_THREADS, mahasiswaList.size());
		if (workerCount < 1) {
			workerCount = 1;
		}
		updateProgressLabelSafe(desktop, label,
				Common.getBahasaConfig("Menghitung nilai OBE secara paralel") + " (" + workerCount + " thread)...");

		ExecutorService executor = Executors.newFixedThreadPool(workerCount);
		List<Future<StudentObeResult>> futures = new ArrayList<Future<StudentObeResult>>();
		try {
			for (int i = 0; i < mahasiswaList.size(); i++) {
				final Mahasiswa mahasiswa = mahasiswaList.get(i);
				final int nomor = i + 1;
				futures.add(executor.submit(new Callable<StudentObeResult>() {
					@Override
					public StudentObeResult call() throws Exception {
						try {
							return calculateStudentObeResult(nomor, mahasiswa, formatColumns, dataNiliasUtama, persensData,
									listCpmkUnique, listCpl, listPL, mapCpmkIdToFns, targetMinimal);
						} catch (Exception e) {
							StudentObeResult fallback = new StudentObeResult();
							fallback.nomor = nomor;
							fallback.mahasiswa = mahasiswa;
							return fallback;
						}
					}
				}));
			}

			int progressStep = Math.max(1, mahasiswaList.size() / 20);
			for (int i = 0; i < futures.size(); i++) {
				try {
					StudentObeResult result = futures.get(i).get();
					results.add(result);
				} catch (Exception e) {
					StudentObeResult fallback = new StudentObeResult();
					fallback.nomor = i + 1;
					fallback.mahasiswa = mahasiswaList.get(i);
					results.add(fallback);
				}
				if ((i + 1) == futures.size() || (i + 1) % progressStep == 0) {
					updateProgressLabelSafe(desktop, label,
							Common.getBahasaConfig("Menggabungkan hasil hitung nilai OBE") + " " + (i + 1) + " / "
									+ futures.size() + " (" + Common.numberFormat.get()
											.format((i + 1) * 100.0 / futures.size())
									+ " %)");
				}
			}
		} finally {
			try {
				executor.shutdown();
				executor.awaitTermination(60, TimeUnit.SECONDS);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:1983");
			}
			try {
				executor.shutdownNow();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:1987");
			}
		}
		return results;
	}

	private StudentObeResult calculateStudentObeResult(int nomor, Mahasiswa mahasiswa,
			List<ObeFormatColumn> formatColumns, Map<Long, Map<Long, Map<String, Double>>> dataNiliasUtama,
			Map<String, Double> persensData, List<CapaianPembelajaranLulusan> listCpmkUnique,
			List<CapaianLulusan> listCpl, List<ProfilLulusan> listPL,
			Map<Long, List<FormatNilai>> mapCpmkIdToFns, double targetMinimal) {
		StudentObeResult result = new StudentObeResult();
		result.nomor = nomor;
		result.mahasiswa = mahasiswa;
		Long mahasiswaId = mahasiswa == null ? null : mahasiswa.getId();

		Double totalNilaiKonversi = Double.valueOf(0.0);
		Double jumlah = Double.valueOf(0.0);
		Double totalNilaiBobotAkhir = Double.valueOf(0.0);
		Double totalBobotFormatAkhir = Double.valueOf(0.0);
		Map<Long, Double> skorFormatTotalMap = new HashMap<Long, Double>();
		Map<Long, Double> skorFormatBobotMap = new HashMap<Long, Double>();

		for (ObeFormatColumn formatColumn : formatColumns) {
			if (formatColumn == null || formatColumn.formatNilai == null || formatColumn.assessmentColumns == null
					|| formatColumn.assessmentColumns.isEmpty()) {
				continue;
			}
			FormatNilai formatNilai = formatColumn.formatNilai;
			Double totalNilaiPerformatNilai = Double.valueOf(0.0);

			for (ObeAssessmentColumn assessmentColumn : formatColumn.assessmentColumns) {
				Double nilai = getNilaiMahasiswa(dataNiliasUtama, mahasiswaId, formatNilai.getId(), assessmentColumn.key);
				Double persenData = getPersenNilai(persensData, assessmentColumn.key, formatNilai.getId(), nilai);
				totalNilaiPerformatNilai += persenData;
				result.cells.add(new ObeCellValue(Common.numberFormat.get().format(nilai) + " ("
						+ Common.numberFormat.get().format(persenData) + ")", false));
			}

			double bobotFormatFraction = normalizePercentFraction(formatNilai.getPersen());
			totalNilaiBobotAkhir += totalNilaiPerformatNilai;
			totalBobotFormatAkhir += bobotFormatFraction;
			skorFormatTotalMap.put(formatNilai.getId(), totalNilaiPerformatNilai);
			skorFormatBobotMap.put(formatNilai.getId(), bobotFormatFraction);
			result.cells.add(new ObeCellValue(totalNilaiPerformatNilai, true));

			Double hasilKonversi = hitungKonversi(formatNilai, totalNilaiPerformatNilai);
			totalNilaiKonversi += hasilKonversi;
			jumlah++;
			result.cells.add(new ObeCellValue(hasilKonversi, true));
			result.cells.add(new ObeCellValue(hitungHurufObeSafe(hasilKonversi, mahasiswa), true));

			Double minimal = Double.valueOf(0.0);
			try {
				minimal = formatNilai.ambilMinimal();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:2042");
			}
			result.cells.add(new ObeCellValue(hasilKonversi >= safeDouble(minimal) ? "L" : "TL", true));
		}

		Double hasilKonversiTotal = totalBobotFormatAkhir > 0.0 ? totalNilaiBobotAkhir / totalBobotFormatAkhir
				: totalNilaiKonversi / (jumlah == 0 ? 1 : jumlah);
		result.hasilKonversiTotal = hasilKonversiTotal;
		result.cells.add(new ObeCellValue(hasilKonversiTotal, true));
		result.cells.add(new ObeCellValue(hitungHurufObeSafe(hasilKonversiTotal, mahasiswa), true));
		result.cells.add(new ObeCellValue(hasilKonversiTotal >= targetMinimal ? "L" : "TL", true));

		// Ketercapaian per Sub-CPMK (FormatNilai) = total/bobot (basis SAMA dengan agregasi CPMK di bawah).
		// Dipakai untuk sheet "Rangkuman Sub-CPMK" (rata-rata kelas per Sub-CPMK).
		for (Long fnId : skorFormatTotalMap.keySet()) {
			Double t = skorFormatTotalMap.get(fnId);
			Double b = skorFormatBobotMap.get(fnId);
			result.subCpmk.put(fnId, (b != null && b > 0.0) ? (t == null ? 0.0 : t) / b : 0.0);
		}

		Map<Long, Double> sCpmkTotal = new HashMap<Long, Double>();
		Map<Long, Double> sCpmkTarget = new HashMap<Long, Double>();
		for (CapaianPembelajaranLulusan cpmk : listCpmkUnique) {
			Double totalCpmk = Double.valueOf(0.0);
			Double targetCpmk = Double.valueOf(0.0);
			List<FormatNilai> fns = mapCpmkIdToFns.get(cpmk.getId());
			if (fns != null) {
				for (FormatNilai fn : fns) {
					Double t = skorFormatTotalMap.get(fn.getId());
					Double b = skorFormatBobotMap.get(fn.getId());
					totalCpmk += t == null ? 0.0 : t;
					targetCpmk += b == null ? 0.0 : b;
				}
			}
			Double valCpmk = targetCpmk > 0.0 ? (totalCpmk / targetCpmk) : 0.0;
			result.cpmk.put(cpmk.getId(), valCpmk);
			sCpmkTotal.put(cpmk.getId(), totalCpmk);
			sCpmkTarget.put(cpmk.getId(), targetCpmk);
		}

		Map<Long, Double> sCplTotal = new HashMap<Long, Double>();
		Map<Long, Double> sCplTarget = new HashMap<Long, Double>();
		for (CapaianLulusan cpl : listCpl) {
			Double totalCpl = Double.valueOf(0.0);
			Double targetCpl = Double.valueOf(0.0);
			String mappedCpmk = cpl.getCapaianPembelajaranLulusan() != null ? cpl.getCapaianPembelajaranLulusan() : "";
			for (CapaianPembelajaranLulusan cpmk : listCpmkUnique) {
				if (containsIdInCsv(mappedCpmk, cpmk.getId())) {
					double relWeight = normalizeRelationWeight(cpmk.getBobot());
					Double totalCpmk = sCpmkTotal.get(cpmk.getId());
					Double targetCpmk = sCpmkTarget.get(cpmk.getId());
					totalCpl += (totalCpmk == null ? 0.0 : totalCpmk) * relWeight;
					targetCpl += (targetCpmk == null ? 0.0 : targetCpmk) * relWeight;
				}
			}
			Double valCpl = targetCpl > 0.0 ? (totalCpl / targetCpl) : 0.0;
			result.cpl.put(cpl.getId(), valCpl);
			sCplTotal.put(cpl.getId(), totalCpl);
			sCplTarget.put(cpl.getId(), targetCpl);
		}

		for (ProfilLulusan pl : listPL) {
			Double totalPl = Double.valueOf(0.0);
			Double targetPl = Double.valueOf(0.0);
			for (CapaianLulusan cpl : listCpl) {
				String mappedPl = cpl.getProfil() != null ? cpl.getProfil() : "";
				if (containsIdInCsv(mappedPl, pl.getId())) {
					Double totalCpl = sCplTotal.get(cpl.getId());
					Double targetCpl = sCplTarget.get(cpl.getId());
					totalPl += totalCpl == null ? 0.0 : totalCpl;
					targetPl += targetCpl == null ? 0.0 : targetCpl;
				}
			}
			result.pl.put(pl.getId(), targetPl > 0.0 ? (totalPl / targetPl) : 0.0);
		}
		return result;
	}

	private Double getNilaiMahasiswa(Map<Long, Map<Long, Map<String, Double>>> data, Long mahasiswaId, Long formatId,
			String key) {
		try {
			Double value = data.get(mahasiswaId).get(formatId).get(key);
			return value == null ? Double.valueOf(0.0) : value;
		} catch (Exception e) {
			return Double.valueOf(0.0);
		}
	}

	private Double getPersenNilai(Map<String, Double> persensData, String key, Long formatId, Double nilai) {
		try {
			Double persen = persensData.get(key + "_" + formatId);
			return Double.valueOf((persen == null ? 0.0 : persen.doubleValue()) * safeDouble(nilai));
		} catch (Exception e) {
			return Double.valueOf(0.0);
		}
	}

	private String hitungHurufObeSafe(Double nilai, Mahasiswa mahasiswa) {
		try {
			return hitungHurufObe(nilai, mahasiswa, perkuliahan);
		} catch (Exception e) {
			return "-";
		}
	}

	private void writeExcelCell(XSSFRow row, int column, Object value, XSSFCellStyle style) {
		XSSFCell cell = row.createCell(column);
		cell.setCellStyle(style);
		if (value == null) {
			cell.setCellValue("");
		} else if (value instanceof Number) {
			cell.setCellValue(((Number) value).doubleValue());
		} else {
			cell.setCellValue(String.valueOf(value));
		}
	}

	private void updateProgressLabelSafe(Desktop desktop, Label label, String message) {
		// FIX DesktopUnavailableException: desktop dari thread parallel bisa sudah
		// "Stopped" (user menutup/navigasi keluar sebelum thread hitung nilai OBE
		// selesai) -- cek isAlive() dulu sebelum activate(), supaya tidak melempar
		// exception yang cuma akan tertangkap & jadi noise di audit log.
		if (desktop == null || label == null || !desktop.isAlive()) {
			return;
		}
		try {
			Executions.activate(desktop);
			try {
				label.setValue(message == null ? "" : message);
			} finally {
				Executions.deactivate(desktop);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:2170");
		}
	}

	private void generateSheetPenilaian(XSSFWorkbook wb, String sheetName, String titleStr, List<Mahasiswa> listMhs,
			List<? extends Object> items, Map<Long, Map<Long, Double>> scores, Map<Long, Double> finalScores,
			double target, XSSFCellStyle hStyle, XSSFCellStyle dStyle, Desktop desktop,
			ais.ui.util.MyButtonTabbox tabbox) {
		XSSFSheet sheet = wb.createSheet(sheetName);
		int rIdx = 0;

		XSSFRow rTitle = sheet.createRow(rIdx++);
		createHeaderCell(rTitle, 1, titleStr, hStyle);
		XSSFRow rInfo = sheet.createRow(rIdx++);
		createHeaderCell(rInfo, 1, "Rumus: Total = SUM(Nilai x Bobot); Konversi = Total / Bobot Target; Status L jika Nilai Konversi >= Target.", hStyle);
		rIdx++;

		XSSFRow rH1 = sheet.createRow(rIdx++);
		XSSFRow rH2 = sheet.createRow(rIdx++);

		createHeaderCell(rH1, 0, "No.", hStyle);
		createHeaderCell(rH2, 0, "No.", hStyle);
		createHeaderCell(rH1, 1, "NIM", hStyle);
		createHeaderCell(rH2, 1, "NIM", hStyle);
		createHeaderCell(rH1, 2, "Nama Mahasiswa", hStyle);
		createHeaderCell(rH2, 2, "Nama Mahasiswa", hStyle);
		createHeaderCell(rH1, 3, "Nilai Akhir", hStyle);
		createHeaderCell(rH2, 3, "Nilai Akhir", hStyle);
		createHeaderCell(rH1, 4, "Skala 4", hStyle);
		createHeaderCell(rH2, 4, "Skala 4", hStyle);
		createHeaderCell(rH1, 5, "Status", hStyle);
		createHeaderCell(rH2, 5, "Status", hStyle);

		try {
			for (int i = 0; i <= 5; i++)
				sheet.addMergedRegion(new CellRangeAddress(rIdx - 2, rIdx - 1, i, i));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:2206");
		}

		int cIdx = 6;
		for (Object item : items) {
			try {
				String kode = (String) item.getClass().getMethod("getKode").invoke(item);
				createHeaderCell(rH1, cIdx, kode, hStyle);
				createHeaderCell(rH1, cIdx + 1, kode, hStyle);
				createHeaderCell(rH1, cIdx + 2, kode, hStyle);

				createHeaderCell(rH2, cIdx, "Nilai", hStyle);
				createHeaderCell(rH2, cIdx + 1, "Skala 4", hStyle);
				createHeaderCell(rH2, cIdx + 2, "L/TL", hStyle);

				try {
					sheet.addMergedRegion(new CellRangeAddress(rIdx - 2, rIdx - 2, cIdx, cIdx + 2));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:2223");
				}
				cIdx += 3;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:2226");
			}
		}

		int no = 1;
		for (Mahasiswa m : listMhs) {
			XSSFRow rData = sheet.createRow(rIdx++);
			rData.createCell(0).setCellValue(no++);
			rData.getCell(0).setCellStyle(dStyle);
			rData.createCell(1).setCellValue(m.getNim());
			rData.getCell(1).setCellStyle(dStyle);
			rData.createCell(2).setCellValue(m.getNama());
			rData.getCell(2).setCellStyle(dStyle);

			Double fScore = finalScores.get(m.getId()) == null ? 0.0 : finalScores.get(m.getId());
			rData.createCell(3).setCellValue(Common.numberFormat.get().format(fScore));
			rData.getCell(3).setCellStyle(dStyle);
			rData.createCell(4).setCellValue(getScale4(fScore));
			rData.getCell(4).setCellStyle(dStyle);
			rData.createCell(5).setCellValue(fScore >= target ? "L" : "TL");
			rData.getCell(5).setCellStyle(dStyle);

			cIdx = 6;
			Map<Long, Double> mScores = scores.get(m.getId());
			for (Object item : items) {
				try {
					Long itemId = (Long) item.getClass().getMethod("getId").invoke(item);
					Double iScore = mScores == null || mScores.get(itemId) == null ? 0.0 : mScores.get(itemId);

					rData.createCell(cIdx).setCellValue(Common.numberFormat.get().format(iScore));
					rData.getCell(cIdx).setCellStyle(dStyle);
					rData.createCell(cIdx + 1).setCellValue(getScale4(iScore));
					rData.getCell(cIdx + 1).setCellStyle(dStyle);
					rData.createCell(cIdx + 2).setCellValue(iScore >= target ? "L" : "TL");
					rData.getCell(cIdx + 2).setCellStyle(dStyle);
					cIdx += 3;
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:2262");
				}
			}
		}

		for (int i = 0; i < cIdx; i++) {
			try {
				sheet.autoSizeColumn(i);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:2270");
			}
		}

		// LANGSUNG RENDER TAB DARI OBJEK SHEET DI MEMORI INI
		addTabFromMemorySheet(desktop, tabbox, sheet);
	}

	private void generateSheetHasil(XSSFWorkbook wb, String sheetName, String titleStr, List<? extends Object> items,
			Map<Long, Map<Long, Double>> scores, double target, XSSFCellStyle hStyle, XSSFCellStyle dStyle,
			Desktop desktop, ais.ui.util.MyButtonTabbox tabbox) {
		XSSFSheet sheet = wb.createSheet(sheetName);
		int rIdx = 0;

		for (Object item : items) {
			try {
				Long itemId = (Long) item.getClass().getMethod("getId").invoke(item);
				String kode = (String) item.getClass().getMethod("getKode").invoke(item);

				XSSFRow rTitle = sheet.createRow(rIdx++);
				createHeaderCell(rTitle, 1, titleStr + " " + kode, hStyle);

				XSSFRow rH = sheet.createRow(rIdx++);
				createHeaderCell(rH, 1, "Skala Nilai", hStyle);
				createHeaderCell(rH, 2, "Jumlah Mahasiswa", hStyle);

				int c4 = 0, c3 = 0, c2 = 0, c1 = 0, cL = 0, cTL = 0;

				for (Map<Long, Double> mScores : scores.values()) {
					Double sc = mScores.get(itemId) == null ? 0.0 : mScores.get(itemId);
					int scale = getScale4(sc);
					if (scale == 4)
						c4++;
					else if (scale == 3)
						c3++;
					else if (scale == 2)
						c2++;
					else
						c1++;

					if (sc >= target)
						cL++;
					else
						cTL++;
				}

				String[] lbls = { "4", "3", "2", "1", "Lulus", "Tidak Lulus" };
				int[] vals = { c4, c3, c2, c1, cL, cTL };

				for (int i = 0; i < lbls.length; i++) {
					XSSFRow rD = sheet.createRow(rIdx++);
					rD.createCell(1).setCellValue(lbls[i]);
					rD.getCell(1).setCellStyle(dStyle);
					rD.createCell(2).setCellValue(vals[i]);
					rD.getCell(2).setCellStyle(dStyle);
				}
				rIdx += 2;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:2327");
			}
		}

		try {
			sheet.autoSizeColumn(1);
			sheet.autoSizeColumn(2);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:2334");
		}

		// LANGSUNG RENDER TAB DARI OBJEK SHEET DI MEMORI INI
		addTabFromMemorySheet(desktop, tabbox, sheet);
	}

	/**
	 * Adapter agar {@link FormatNilai} (Sub-CPMK) bisa dipakai oleh {@link #generateSheetRangkuman} yang
	 * membaca item via refleksi {@code getId()}/{@code getKode()}. FormatNilai tak punya {@code getKode()}
	 * (hanya {@code getKodeSubCpmk()}/{@code getNama()}), jadi dibungkus di sini.
	 */
	private static class SubCpmkItem {
		private final Long id;
		private final String kode;
		private final String nama;

		SubCpmkItem(FormatNilai fn) {
			this.id = fn.getId();
			String k = null;
			try {
				k = fn.getKodeSubCpmk();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:2356");
			}
			String nm = null;
			try {
				nm = fn.getNama();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:2361");
			}
			this.nama = nm == null ? "" : nm;
			this.kode = (k == null || k.trim().isEmpty()) ? this.nama : k;
		}

		public Long getId() {
			return id;
		}

		public String getKode() {
			return kode;
		}

		public String getNama() {
			return nama;
		}
	}

	private void generateSheetRangkuman(XSSFWorkbook wb, String sheetName, String titleStr,
			List<? extends Object> items, Map<Long, Map<Long, Double>> scores, double target, XSSFCellStyle hStyle,
			XSSFCellStyle dStyle, Desktop desktop, ais.ui.util.MyButtonTabbox tabbox) {
		XSSFSheet sheet = wb.createSheet(sheetName);
		int rIdx = 0;

		XSSFRow rTitle = sheet.createRow(rIdx++);
		createHeaderCell(rTitle, 1, titleStr, hStyle);
		rIdx++;

		XSSFRow rH = sheet.createRow(rIdx++);
		String[] headers = { "Kode", "Target", "Rata-Rata Nilai", "Keterangan", "Jml Mhs Memenuhi",
				"Jml Mhs Belum Memenuhi", "Analisis Pelaksanaan", "Rencana Perbaikan" };
		for (int i = 0; i < headers.length; i++) {
			createHeaderCell(rH, i + 1, headers[i], hStyle);
		}

		for (Object item : items) {
			try {
				Long itemId = (Long) item.getClass().getMethod("getId").invoke(item);
				String kode = (String) item.getClass().getMethod("getKode").invoke(item);

				Double totalSum = 0.0;
				int cL = 0, cTL = 0, count = 0;

				for (Map<Long, Double> mScores : scores.values()) {
					Double sc = mScores.get(itemId) == null ? 0.0 : mScores.get(itemId);
					totalSum += sc;
					if (sc >= target)
						cL++;
					else
						cTL++;
					count++;
				}

				Double avg = count == 0 ? 0.0 : (totalSum / count);

				XSSFRow rD = sheet.createRow(rIdx++);
				rD.createCell(1).setCellValue(kode);
				rD.getCell(1).setCellStyle(dStyle);
				rD.createCell(2).setCellValue(target);
				rD.getCell(2).setCellStyle(dStyle);
				rD.createCell(3).setCellValue(Common.numberFormat.get().format(avg));
				rD.getCell(3).setCellStyle(dStyle);
				rD.createCell(4).setCellValue(avg >= target ? "Tercapai" : "Tidak Tercapai");
				rD.getCell(4).setCellStyle(dStyle);
				rD.createCell(5).setCellValue(cL);
				rD.getCell(5).setCellStyle(dStyle);
				rD.createCell(6).setCellValue(cTL);
				rD.getCell(6).setCellStyle(dStyle);
				rD.createCell(7).setCellValue("");
				rD.getCell(7).setCellStyle(dStyle);
				rD.createCell(8).setCellValue(cTL > 0 ? "Perlu Remedial" : "");
				rD.getCell(8).setCellStyle(dStyle);

			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:2435");
			}
		}

		for (int i = 1; i <= 8; i++) {
			try {
				sheet.autoSizeColumn(i);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:2442");
			}
		}

		// LANGSUNG RENDER TAB DARI OBJEK SHEET DI MEMORI INI
		addTabFromMemorySheet(desktop, tabbox, sheet);
	}

	// =========================================================================
	// HELPER DATA TYPE DAN FORMATTING
	// =========================================================================

	/**
	 * Sheet "Analisis CQI" — ringkasan CQI (analisis dosen dan evaluasi admin)
	 * untuk perkuliahan ini. Dibuat hanya jika ada data CQI; jika kosong tab tidak ditambahkan.
	 */
	private void generateSheetCqiSummary(XSSFWorkbook wb, Perkuliahan perk,
			XSSFCellStyle hStyle, XSSFCellStyle hFooterStyle,
			XSSFCellStyle dStyle, Desktop desktop, ais.ui.util.MyButtonTabbox tabbox) {
		try {
			String cqiRaw = perk == null ? null : perk.getCqiData();
			if (cqiRaw == null || cqiRaw.trim().isEmpty()) return;
			org.json.JSONArray cqiArr = null;
			try { cqiArr = new org.json.JSONArray(cqiRaw); } catch (Exception ex) { return; }
			if (cqiArr.length() == 0) return;

			XSSFSheet sheet = wb.createSheet("Analisis CQI");
			int rIdx = 0;

			XSSFRow rTitle = sheet.createRow(rIdx++);
			createHeaderCell(rTitle, 0, "ANALISIS CQI — " + (perk.infoSimple() != null ? perk.infoSimple() : ""), hStyle);
			rIdx++;

			XSSFRow rH = sheet.createRow(rIdx++);
			String[] headers = {"No", "CPMK", "Masalah / Gap Ketercapaian",
					"Analisis Penyebab", "Rencana Tindak Lanjut", "PJ", "Target Waktu", "Status", "Evaluasi Admin"};
			for (int i = 0; i < headers.length; i++) {
				createHeaderCell(rH, i, headers[i], hStyle);
			}

			for (int i = 0; i < cqiArr.length(); i++) {
				org.json.JSONObject e = cqiArr.optJSONObject(i);
				if (e == null) continue;
				XSSFRow rD = sheet.createRow(rIdx++);
				writeExcelCell(rD, 0, Integer.valueOf(i + 1), dStyle);
				writeExcelCell(rD, 1, e.optString("cpmk",""),        dStyle);
				writeExcelCell(rD, 2, e.optString("masalah",""),     dStyle);
				writeExcelCell(rD, 3, e.optString("analisis",""),    dStyle);
				writeExcelCell(rD, 4, e.optString("rencana",""),     dStyle);
				writeExcelCell(rD, 5, e.optString("pj",""),          dStyle);
				writeExcelCell(rD, 6, e.optString("targetWaktu",""), dStyle);
				writeExcelCell(rD, 7, e.optString("status",""),      dStyle);
				writeExcelCell(rD, 8, e.optString("adminKomentar",""), dStyle);
			}

			for (int i = 0; i < headers.length; i++) {
				try { sheet.autoSizeColumn(i); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:2498");}
			}

			addTabFromMemorySheet(desktop, tabbox, sheet);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:2503");
		}
	}

	private int getScale4(Double score) {
		double nilai = safeDouble(score);
		if (nilai >= 80.0)
			return 4;
		if (nilai >= 70.0)
			return 3;
		if (nilai >= 60.0)
			return 2;
		return 1;
	}

	private double safeDouble(Double value) {
		return value == null || value.isNaN() || value.isInfinite() ? 0.0 : value.doubleValue();
	}

	private double normalizePercentFraction(Double value) {
		double d = safeDouble(value);
		if (d < 0.0)
			d = 0.0;
		return d > 1.0 ? d / 100.0 : d;
	}

	private double normalizePercentDisplay(Double value) {
		return normalizePercentFraction(value) * 100.0;
	}

	private double normalizeRelationWeight(Double value) {
		double d = safeDouble(value);
		if (d <= 0.0)
			return 1.0;
		return d > 1.0 ? d / 100.0 : d;
	}

	private boolean containsIdInCsv(String csv, Long id) {
		if (csv == null || id == null)
			return false;
		String target = String.valueOf(id.longValue());
		String[] parts = csv.split(",");
		for (String part : parts) {
			if (part != null && target.equals(part.trim()))
				return true;
		}
		return false;
	}

	private void setAllBorders(XSSFCellStyle style) {
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		style.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
		style.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
	}

	private void createHeaderCell(XSSFRow row, int index, String text, XSSFCellStyle style) {
		XSSFCell cell = row.createCell(index);
		cell.setCellValue(text);
		cell.setCellStyle(style);
	}

	private void createSubHeader(XSSFRow r1, XSSFRow r2, int index, String txt1, XSSFCellStyle style, FormatNilai fn,
			String key, Map<Long, Map<String, Double>> db, Map<String, Double> pData) {
		createHeaderCell(r1, index, txt1, style);
		Map<String, Double> mapB = db.get(fn.getId());
		Double persenFraction = 0.0;
		if (mapB != null) {
			Double total = 0.0, nilai = 0.0;
			for (String keydata : mapB.keySet()) {
				Double d = mapB.get(keydata);
				if (d == null)
					d = 0.0;
				if (keydata.equalsIgnoreCase(key))
					nilai += d;
				total += d;
			}
			if (total > 0.0) {
				persenFraction = (nilai / total) * normalizePercentFraction(fn.getPersen());
			}
		}
		pData.put(key + "_" + fn.getId(), persenFraction);
		createHeaderCell(r2, index, Common.numberFormat.get().format(persenFraction * 100.0) + "%", style);
	}

	private void createHeaderColumn(XSSFRow r1, XSSFRow r2, int index, String txt1, String txt2, XSSFCellStyle style) {
		createHeaderCell(r1, index, txt1, style);
		createHeaderCell(r2, index, txt2, style);
	}

	private void createMainHeaderMerged(XSSFSheet sheet, XSSFRow r0, XSSFRow r1, XSSFRow r2, int index, String txt0,
			String txt2, XSSFCellStyle style) {
		createHeaderCell(r0, index, txt0, style);
		createHeaderCell(r1, index, txt0, style);
		createHeaderCell(r2, index, txt2, style);
		try {
			sheet.addMergedRegion(new CellRangeAddress(1, 2, index, index));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:2602");
		}
	}

	private <T> void parseTugasJSON(List<T> listTugas, List<FormatNilai> fns, List<Mahasiswa> mhsList,
			Map<Long, Map<Long, Map<String, Double>>> dataMain, Map<Long, Map<Long, T>> mapOut, String prefixClass)
			throws Exception {
		for (T o : listTugas) {
			String jsonStr = (String) o.getClass().getMethod("getKeteranganNilai").invoke(o);
			Long objId = (Long) o.getClass().getMethod("getId").invoke(o);
			if (jsonStr == null || jsonStr.trim().isEmpty())
				continue;

			JSONObject j = new JSONObject(jsonStr);
			for (FormatNilai fn : fns) {
				boolean has = false;
				for (Mahasiswa m : mhsList) {
					String k = m.getId() + "_mhs_nilai_" + fn.getId();
					if (!j.isNull(k)) {
						has = true;
						addNilaiUtama(dataMain, m.getId(), fn.getId(), prefixClass + "_" + objId, j.getDouble(k));
					}
				}
				if (has) {
					Map<Long, T> inner = mapOut.get(fn.getId());
					if (inner == null) {
						inner = new HashMap<Long, T>();
						mapOut.put(fn.getId(), inner);
					}
					inner.put(objId, o);
				}
			}
		}
	}

	private void addNilaiUtama(Map<Long, Map<Long, Map<String, Double>>> data, Long mhsId, Long formatId, String key,
			Double val) {
		Map<Long, Map<String, Double>> mhsMap = data.get(mhsId);
		if (mhsMap == null) {
			mhsMap = new HashMap<Long, Map<String, Double>>();
			data.put(mhsId, mhsMap);
		}
		Map<String, Double> fmtMap = mhsMap.get(formatId);
		if (fmtMap == null) {
			fmtMap = new HashMap<String, Double>();
			mhsMap.put(formatId, fmtMap);
		}
		Double current = fmtMap.get(key) == null ? 0.0 : fmtMap.get(key);
		fmtMap.put(key, current + val);
	}

	private <T> void addMapType(Map<Long, Map<Long, T>> map, Long formatId, Long itemId, T item) {
		Map<Long, T> inner = map.get(formatId);
		if (inner == null) {
			inner = new HashMap<Long, T>();
			map.put(formatId, inner);
		}
		inner.put(itemId, item);
	}

	private void extractBobot(String jsonStr, Long formatId, String key, Map<Long, Map<String, Double>> dataBobot) {
		if (jsonStr == null || jsonStr.trim().isEmpty())
			return;
		try {
			JSONObject jObj = new JSONObject(jsonStr);
			if (!jObj.isNull(formatId.toString())) {
				Double bobot = jObj.isNull(formatId.toString() + "_bobot") ? 100.0
						: jObj.getDouble(formatId.toString() + "_bobot");
				Map<String, Double> mapB = dataBobot.get(formatId);
				if (mapB == null) {
					mapB = new HashMap<String, Double>();
					dataBobot.put(formatId, mapB);
				}
				mapB.put(key, bobot);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:2677");
		}
	}

	private void hitungPersenPerKomponen(String keyPrefix, Long itemId, FormatNilai fn,
			Map<Long, Map<String, Double>> dataBobot, Map<String, Double> persensData) {
		Double persenFraction = 0.0;
		Map<String, Double> mapB = dataBobot.get(fn.getId());
		if (mapB != null) {
			Double totalB = 0.0, nilaiB = 0.0;
			for (String keyd : mapB.keySet()) {
				Double d = mapB.get(keyd);
				if (d == null)
					d = 0.0;
				if (keyd.equalsIgnoreCase(keyPrefix + "_" + itemId))
					nilaiB += d;
				totalB += d;
			}
			if (totalB > 0.0) {
				persenFraction = (nilaiB / totalB) * normalizePercentFraction(fn.getPersen());
			}
		}
		persensData.put(keyPrefix + "_" + itemId + "_" + fn.getId(), persenFraction);
	}

	private <T> int writeNilaiCells(XSSFRow row, int index, Collection<T> items,
			Map<Long, Map<Long, Map<String, Double>>> data, Map<String, Double> pData, Mahasiswa mhs, FormatNilai fn,
			String prefix, XSSFCellStyle style) {
		for (T item : items) {
			try {
				Long itemId = (Long) item.getClass().getMethod("getId").invoke(item);
				String key = prefix + "_" + itemId;
				Double nilia = 0.0;
				try {
					nilia = data.get(mhs.getId()).get(fn.getId()).get(key);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:2712");
				}
				if (nilia == null)
					nilia = 0.0;

				Double persenData = 0.0;
				try {
					persenData = pData.get(key + "_" + fn.getId()) * nilia;
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:2720");
				}
				XSSFCell cell = row.createCell(index++);
				cell.setCellStyle(style);
				cell.setCellValue(Common.numberFormat.get().format(nilia) + " ("
						+ Common.numberFormat.get().format(persenData) + ")");
			} catch (Exception e) {
				XSSFCell cell = row.createCell(index++);
				cell.setCellStyle(style);
				cell.setCellValue(0.0);
			}
		}
		return index;
	}

	private Double recalculateTotalPerFormat(Map<Long, Map<Long, Map<String, Double>>> data, Map<String, Double> pData,
			Long mhsId, Long formatId, Map<Long, PertemuanPunyaUjian> md, Map<Long, Pertemuan> mt,
			Map<Long, TugasPertemuan> mtl, Map<Long, TugasKelompok> mtk) {
		Double total = 0.0;
		if (md != null)
			total += sumNilai(md.values(), data, pData, mhsId, formatId, PertemuanPunyaUjian.class.getName());
		if (mt != null)
			total += sumNilai(mt.values(), data, pData, mhsId, formatId, Pertemuan.class.getName());
		if (mtl != null)
			total += sumNilai(mtl.values(), data, pData, mhsId, formatId, TugasPertemuan.class.getName());
		if (mtk != null)
			total += sumNilai(mtk.values(), data, pData, mhsId, formatId, TugasKelompok.class.getName());
		return total;
	}

	private <T> Double sumNilai(Collection<T> items, Map<Long, Map<Long, Map<String, Double>>> data,
			Map<String, Double> pData, Long mhsId, Long formatId, String prefix) {
		Double sum = 0.0;
		for (T item : items) {
			try {
				Long itemId = (Long) item.getClass().getMethod("getId").invoke(item);
				String key = prefix + "_" + itemId;
				Double nilia = 0.0;
				try {
					nilia = data.get(mhsId).get(formatId).get(key);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:2760");
				}
				if (nilia == null)
					nilia = 0.0;
				Double persenData = 0.0;
				try {
					persenData = pData.get(key + "_" + formatId) * nilia;
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:2767");
				}
				if (persenData != null)
					sum += persenData;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/RekapHasilTugasPerTugasDanUjianObe.java:2771");
			}
		}
		return sum;
	}

	private String hitungHurufObe(Double hasilKonversi, Mahasiswa mahasiswa, Perkuliahan perkuliahan) {
		boolean coba = true;
		NilaiHuruf huruf = Common.getNilaiHuruf(hasilKonversi, mahasiswa == null ? 0 : mahasiswa.getTahunangkatan(),
				mahasiswa == null ? perkuliahan.getJurusan() : mahasiswa.getJurusan(),
				mahasiswa == null ? perkuliahan.getJurusan().getFakultas() : mahasiswa.getJurusan().getFakultas(),
				perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap(), perkuliahan.getMatakuliah().getKode(), coba,
				true, perkuliahan.getMatakuliah().getJenisNilaiHuruf());
		return huruf == null ? "-" : huruf.getNilaiHuruf();
	}

	private Double hitungKonversi(FormatNilai formatNilai, Double totalNilaiPerformatNilai) {
		double persenFraction = normalizePercentFraction(formatNilai == null ? null : formatNilai.getPersen());
		BigDecimal divisor = new BigDecimal(persenFraction <= 0.0 ? 1.0 : persenFraction);
		BigDecimal nilai = new BigDecimal(safeDouble(totalNilaiPerformatNilai));
		return nilai.divide(divisor, 2, RoundingMode.HALF_UP).doubleValue();
	}

	private Set<Long> parseIdsToSet(String ids) {
		Set<Long> longs = new HashSet<Long>();
		if (ids != null && !ids.trim().isEmpty()) {
			// Beberapa data lama menyimpan pasangan id sebagai "formatId_detailId"
			// (contoh 43_29462). Keduanya adalah id sah; terima pemisah lama tanpa
			// NumberFormatException dan tanpa menghilangkan data rekap.
			for (String s : ids.split("[,_;\\s]+")) {
				if (!s.trim().isEmpty()) {
					try {
						longs.add(Long.parseLong(s.trim()));
					} catch (NumberFormatException e) {
						// Token nonnumerik bukan id dan aman dilewati.
					}
				}
			}
		}
		return longs;
	}
}
