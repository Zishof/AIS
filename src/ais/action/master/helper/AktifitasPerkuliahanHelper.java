package ais.action.master.helper;

import java.io.File;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timebox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.EksporFromFeederAction;
import ais.action.master.LogLoginAction;
import ais.action.master.PerkuliahanAction;
import ais.action.master.TampilanELearningAction;
import ais.action.master.dashboard.admin.DashboardDataNilaiMahasiswa;
import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.master.dashboard.admin.RekapHasilTugasPerTugasDanUjianObe;
import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.FeederExporter;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.report.CommonReportHelper;
import ais.action.report.Report;
import ais.action.report.format1.akademik.LaporanJurnalMengajar;
import ais.action.report.format1.akademik.LaporanKontrakPerkuliahan;
import ais.action.report.format1.akademik.LaporanMonitorPerkuliahan;
import ais.action.report.format1.akademik.LaporanMonitorPerkuliahanKbm;
import ais.action.report.format1.akademik.LaporanMonitorPerkuliahanParalel;
import ais.action.report.format1.akademik.LaporanRekapitulasiTugasMandiri;
import ais.common.AIGenerator;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.common.calendar.CalendarUtil;
import ais.common.classroom.ClassRoomUtil;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.GeneralValueObject;
import ais.database.model.InterviewCalonMahasiswa;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Mahasiswa;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.TugasKelompok;
import ais.database.model.TugasPertemuan;
import ais.database.model.VOPembelajaran;
import ais.database.model.file.FileFoto;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelAgakKecilBold;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelBoldConfig;
import ais.ui.util.MyLabelBoldMerah;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Helper UI ZK yang membangun panel "Aktifitas Perkuliahan" — tampilan tab utama satu
 * {@link Perkuliahan} (kelas matakuliah suatu semester) yang dipakai bersama oleh dosen, mahasiswa,
 * dan admin (dibedakan lewat {@link Tbmuser} yang sedang login serta parameter {@code mahasiswa}/
 * {@code biodataCalonMahasiswa} bila dipanggil dari sisi mahasiswa/calon mahasiswa). Instance
 * dibuat sekali per pembukaan window dan menyimpan state UI lokal (tab mana yang terakhir aktif,
 * berapa pertemuan yang ditampilkan sekaligus) — bukan singleton, tidak boleh dibagikan antar
 * request/desktop.
 *
 * <p><b>Struktur tab (dibangun oleh {@link #initDetail(Perkuliahan, DataLoader, Component, int, int)}):</b>
 * Home (deskripsi/pendahuluan/capaian matakuliah — bisa digenerasi otomatis via {@link AIGenerator}),
 * Agenda (daftar {@link Pertemuan}/pertemuan kuliah berpaging, dirender oleh
 * {@link #tampilRinci(Perkuliahan, DataLoader, Tabpanel, Component, int, int, boolean)}), Info
 * (pengumuman), Ref. (referensi buku/bahan ajar/artikel), Ujian, Tgs (tugas individu), Tgs.Kel.
 * (tugas kelompok), Nilai, dan Lap. (sub-tab laporan: Rencana Perkuliahan, Jurnal Mengajar, Kontrak
 * Perkuliahan, Rencana Paralel, Laporan KBM, Tugas Individu, Kehadiran, Nilai, Kehadiran &amp; Nilai,
 * Ketidakhadiran — masing-masing men-generate PDF via {@link Report#generatePDFReport}). Setiap
 * pertemuan pada tab Agenda menampilkan tombol aksi cepat (Dasbor/Catatan/Ujian/Diskusi/dst) yang
 * dibangun oleh {@link #createKeteranganData}.</p>
 *
 * <p><b>Kuirk penting ZK 5:</b> memilih tab (klik) TIDAK otomatis membuat {@code Tabpanel}-nya
 * visible di sisi client — hanya CSS class tab yang berubah. Karena itu hampir setiap listener
 * {@code onClick} tab di kelas ini secara eksplisit memanggil {@code tab.setSelected(true)} DAN
 * {@code tabpanel.setVisible(true)} sebelum mengisi konten (idempoten, dicek via
 * {@code getChildren().isEmpty()}), dan beberapa tempat mem-redispatch {@code onClick} lewat
 * {@code Events.sendEvent}/listener {@code onSelect} agar tab pertama yang dipilih otomatis oleh ZK
 * tetap terisi kontennya.</p>
 *
 * <p><b>Efek samping:</b> tombol "Kirim ke Feeder" di {@link #initAgendaPerkuliahan} mengirim data
 * perkuliahan ke server PDDikti Neo Feeder secara asinkron (thread terpisah) via
 * {@link ais.action.master.feeder.util.FeederExporter}; tombol "History" memuat ulang seluruh
 * {@link Pertemuan} aktif milik perkuliahan dari database. Method baca (tampilRinci, displayHeader,
 * createKeterangan*) tidak melakukan mutasi tersembunyi kecuali disebutkan (mis. edit Pendahuluan/
 * Deskripsi/Capaian langsung melakukan {@code Common.refreshUpdate} saat tombol Simpan diklik).</p>
 */
public class AktifitasPerkuliahanHelper {

	/** Helper penjadwalan dipakai oleh tombol "Tambah/Ubah Agenda" dan "Buat Pertemuan" di toolbar Agenda. */
	protected PenjadwalanHelper penjadwalanHelper = new PenjadwalanHelper();
	/** Mahasiswa pemilik konteks bila panel dibuka dari sisi mahasiswa (mis. lewat Aktifitas Kuliah Mahasiswa); null bila dibuka dari sisi dosen/admin. */
	private Mahasiswa mahasiswa;
	/** Calon mahasiswa pemilik konteks bila panel dibuka dari alur pendaftaran/kuliah tamu; null di jalur dosen/admin biasa. */
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	/** Indeks pertemuan awal (0-based) yang ditampilkan pada halaman Agenda aktif. */
	private Integer mulai;
	/** Jumlah pertemuan yang ditampilkan sekaligus per halaman Agenda (dipilih via {@link #jumlahDitampilkan}). */
	private Integer banyak;
	/** Component induk tempat seluruh tabbox ditempelkan; diisi ulang tiap {@link #initDetail}. */
	private Component groupbox;
	/** Menandai tab Agenda sebagai tab aktif terakhir agar tetap terpilih setelah re-render ({@link #initDetail}). */
	private boolean tampikanTab = false;
	/** Bila true, seluruh panel ringkasan (KBM/Keaktifan/Rekap) di tab Home langsung dimuat tanpa menunggu klik tombol "Tampilkan". */
	private boolean tampilLangsungRinci = false;
	/** Combobox jumlah pertemuan per halaman (1-16) di toolbar Agenda. */
	private Combobox jumlahDitampilkan;
	/** Tabpanel Agenda yang diisi ulang oleh {@link #tampilRinci}. */
	private Tabpanel tabpanelAgenda;

	/** User yang sedang login (hasil {@code Common.getCurrentUser()}), dipakai untuk kontrol visibilitas tombol berbasis peran. */
	private Tbmuser tbmuser;
	/** Hak edit konten (Pendahuluan/Deskripsi/Capaian, penilaian, dsb.); diteruskan dari pemanggil, default true. */
	private boolean edit = true;

	/**
	 * Daftar jenis "Lampiran Lain" opsional (di luar RPS/SAP/Absen Manual/Soal UTS/UAS baku) yang
	 * ditampilkan di {@link #tampilkanLampiran} bila konfigurasi {@code tampilkan_&lt;nama&gt;} aktif.
	 */
	public static String[] lampiranLain = new String[] { "Laporan Kuliah Umum / Seminar Praktisi I",
			"Laporan Kuliah Umum / Seminar Praktisi II", "Laporan Integrasi penelitian dalam pembelajaran",
			"Laporan integrasi pengabdian kepada masyarakat dalam pemebelajaran" };

	/**
	 * Constructor untuk konteks mahasiswa/calon mahasiswa (atau dosen/admin bila keduanya null).
	 *
	 * @param mahasiswa               mahasiswa pemilik konteks, atau null bila bukan sisi mahasiswa.
	 * @param biodataCalonMahasiswa   calon mahasiswa pemilik konteks, atau null.
	 * @param edit                    true bila panel boleh menampilkan kontrol edit (Simpan/Ubah).
	 */
	public AktifitasPerkuliahanHelper(Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa, boolean edit) {
		this.edit = edit;
		this.mahasiswa = mahasiswa;
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
		tbmuser = Common.getCurrentUser();
	}

	/**
	 * Sama seperti {@link #AktifitasPerkuliahanHelper(Mahasiswa, BiodataCalonMahasiswa, boolean)},
	 * ditambah kendali {@code tampilLangsungRinci} untuk memaksa panel ringkasan tab Home (KBM,
	 * Keaktifan Peserta, Rekapitulasi Pembelajaran) langsung dimuat tanpa klik tombol "Tampilkan"
	 * — dipakai pada tampilan ringkas/dasbor yang ingin langsung menampilkan semua data.
	 *
	 * @param mahasiswa               mahasiswa pemilik konteks, atau null.
	 * @param biodataCalonMahasiswa   calon mahasiswa pemilik konteks, atau null.
	 * @param tampilLangsungRinci     true untuk memuat langsung panel ringkasan tab Home.
	 * @param edit                    true bila panel boleh menampilkan kontrol edit.
	 */
	public AktifitasPerkuliahanHelper(Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa,
			boolean tampilLangsungRinci, boolean edit) {
		this.edit = edit;
		this.mahasiswa = mahasiswa;
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
		this.tampilLangsungRinci = tampilLangsungRinci;
		tbmuser = Common.getCurrentUser();
	}

	/**
	 * Menentukan apakah panel sedang dipakai oleh peserta didik. Pemeriksaan tidak boleh hanya
	 * mengandalkan {@link Tbmuser#getMahasiswa()}, karena getter tersebut sengaja mengembalikan
	 * {@code null} ketika akun mempunyai asosiasi pegawai/dosen atau role aktifnya berubah. Konteks
	 * eksplisit dari constructor adalah sumber utama, sedangkan role aktif menjadi pertahanan kedua.
	 */
	private boolean penggunaAdalahPesertaDidik() {
		if (mahasiswa != null || biodataCalonMahasiswa != null) {
			return true;
		}
		if (tbmuser == null) {
			return false;
		}
		try {
			Tbmrole roleAktif = tbmuser.hakAkses();
			String roleId = roleAktif == null ? null : roleAktif.getRoleId();
			if (roleId != null && (roleId.equalsIgnoreCase(Tbmrole.MAHASISWA)
					|| roleId.equalsIgnoreCase(Tbmrole.MAHASISWAPASCASARJANA)
					|| roleId.equalsIgnoreCase(Tbmrole.SISWA)
					|| roleId.equalsIgnoreCase(Tbmrole.PESERTA_KURSUS))) {
				return true;
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"audit otorisasi laporan kelas AktifitasPerkuliahanHelper");
		}
		return false;
	}

	private boolean bolehMelihatLaporanKelas() {
		return !penggunaAdalahPesertaDidik();
	}

	private void tampilkanPenolakanLaporanKelas() throws InterruptedException {
		MyMessageboxConfig.show(
				"Laporan rekap kelas hanya dapat dibuka oleh dosen atau petugas yang berwenang. "
						+ "Peserta didik hanya dapat melihat nilai miliknya sendiri melalui menu nilai/KHS.",
				"Akses dibatasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
	}

	/**
	 * Membangun toolbar aksi di atas tab Agenda: "Tambah/Ubah Agenda" (buka {@link #penjadwalanHelper}),
	 * "Buat Pertemuan" (bila {@code perkuliahan} sudah ada, via {@code PenjadwalanHelper.buatSatuPertemuan}),
	 * tombol cetak "Absensi"/"UTS"/"UAS" (via {@code CommonReportHelper.onLaporanAbsensi}), tombol
	 * Kalender ({@link #tampilCalender}), tombol export ruang kelas ({@code ClassRoomUtil.createButton}),
	 * "Refresh", export DSpace, combobox {@link #jumlahDitampilkan} (memicu {@link #tampilRinci} saat
	 * berubah), tombol "Kirim ke Feeder" (hanya bila admin diizinkan akses Feeder dan konfigurasi
	 * {@code aktifkan_terhubung_langsung_ke_feeder} aktif — mengirim data perkuliahan ke server PDDikti
	 * Neo Feeder secara asinkron di thread terpisah, dengan log error yang bisa di-download bila gagal),
	 * tombol pemulihan pertemuan ({@code RecoveryPertemuanHelper}), dan tombol "History" (menampilkan
	 * riwayat revisi lalu me-reload seluruh {@link Pertemuan} aktif perkuliahan dari database).
	 *
	 * @param perkuliahan konteks perkuliahan; boleh null untuk sebagian tombol (mis. saat agenda belum
	 *                    ada), tombol lain menyesuaikan visibilitasnya.
	 * @param dataLoader  callback yang dipanggil untuk memuat ulang tampilan setelah aksi toolbar
	 *                    (mis. setelah agenda diubah atau kalender diproses).
	 * @return toolbar siap ditempel ke parent.
	 */
	public Toolbar initAgendaPerkuliahan(final Perkuliahan perkuliahan, final DataLoader dataLoader) {

		Toolbar hbox = new Toolbar();

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah/Ubah Agenda", "/img/jadwal.png");
		button.setTooltiptext("Ubah Agenda Perkuliahan");

		button.setVisible(tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
				&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				penjadwalanHelper.display(perkuliahan, new DataLoader() {

					@Override
					public void loadData(Object value) {
						perkuliahan.belum();
						try {
							tampikanTab = true;
							initDetail(perkuliahan, groupbox, mulai, banyak);
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AktifitasPerkuliahanHelper.java:178");
						}
					}
				});
			}

		});

		button.setParent(hbox);

		if (perkuliahan != null) {
			button = PenjadwalanHelper.buatSatuPertemuan(perkuliahan, tbmuser, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					perkuliahan.belum();
					try {
						tampikanTab = true;
						initDetail(perkuliahan, groupbox, mulai, banyak);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AktifitasPerkuliahanHelper.java:198");
					}
				}
			});
			button.setParent(hbox);
		}

		button = new MyToolbarbuttonConfig("Absensi", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.onLaporanAbsensi(perkuliahan, true);
			}

		});
		button.setParent(hbox);

		button = new MyToolbarbuttonConfig("UTS", "/img/print.png");

		button.setVisible(tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
				&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.onLaporanAbsensi(perkuliahan, "UTS");

			}

		});
		button.setParent(hbox);

		button = new MyToolbarbuttonConfig("UAS", "/img/print.png");

		button.setVisible(tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
				&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.onLaporanAbsensi(perkuliahan, "UAS");
			}

		});
		button.setParent(hbox);

		AktifitasPerkuliahanHelper.tampilCalender(hbox, dataLoader, perkuliahan);

		ClassRoomUtil.createButton(perkuliahan, dataLoader).setParent(hbox);

		button = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						perkuliahan.belum();
						tampikanTab = true;
						initDetail(perkuliahan, groupbox, mulai, banyak);
					}
				});
			}

		});
		button.setParent(hbox);

		DspaceHelper.tampilkanButtonExportDiPertemuan(hbox, perkuliahan, null, null, null, null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				tampikanTab = true;
				initDetail(perkuliahan, groupbox, mulai, banyak);
				LogLoginAction.tampilDpsaceLog();
			}
		});

		jumlahDitampilkan = new Combobox();
		for (int i = 1; i <= 16; i++) {
			Comboitem comboitem = new Comboitem(i + " " + Common.getBahasaConfig("Pertemuan"));
			comboitem.setValue(i);
			jumlahDitampilkan.appendChild(comboitem);
		}
		jumlahDitampilkan.setCols(5);
		hbox.appendChild(jumlahDitampilkan);
		jumlahDitampilkan.setReadonly(true);
		Common.selectComboItem(jumlahDitampilkan, banyak);
		jumlahDitampilkan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					// Guard: getSelectedItem() bisa null (mis. combobox belum/tidak punya item
					// terpilih saat event onChange terpicu) -> skip aksi, jangan lempar NPE.
					if (jumlahDitampilkan.getSelectedItem() == null) {
						return;
					}
					banyak = (Integer) jumlahDitampilkan.getSelectedItem().getValue();
					tampilRinci(perkuliahan, dataLoader, tabpanelAgenda, groupbox, mulai, banyak, true);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AktifitasPerkuliahanHelper.java:298");
				}
			}
		});

		if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
				&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {

			MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Kirim ke Feeder",
					"/img/Finance-Invoice-icon.png");
			buttonTagihan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin mengirim ke feeder ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										String[] kon = EksporFromFeederAction.koneksi();
										final String ip = kon[0];
										final String port = kon[1];
										final String username = kon[2];
										final String password = kon[3];
										final String url = kon[4];

										if (!EksporFromFeederAction.exists(url)) {

											MyMessageboxConfig.show(
													ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalKoneksi(ip, port, Common.bolehKonfigurasi("aktifkan_https_ke_feeder", ais.database.model.Konfigurasi.TIDAK_AKTIF), "Pemeriksaan ketersediaan alamat " + url + " gagal (server Neo Feeder tidak merespons)."),
													"Peringatan", MyMessageboxConfig.OK,
													MyMessageboxConfig.EXCLAMATION);
											return;
										}

										final List<String> errorLog = new ArrayList<String>();
										final Label myLabelProsesDetail = Common.displayLoadBar(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												if (arg0 != null && !arg0.getName().isEmpty()) {
													EksporFromFeederAction.display();
													MyMessageboxConfig.show(arg0.getName(), "Info",
															MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
												}

												if (!errorLog.isEmpty()) {
													String err = "";
													for (String s : errorLog) {
														err += err.isEmpty() ? s
																: "\n----------------------------------------------------------------------------------------------------------\n"
																		+ s;
													}

													MyMessageboxConfig.show(
															"Error Terjadi, catatan error akan otomatis ter-download",
															"Error Terjadi", MyMessageboxConfig.OK,
															MyMessageboxConfig.EXCLAMATION);

													File file = new File(
															"/opt/ecampus/error_" + Common.randLong() + ".txt");
													if (!file.getParentFile().exists()) {
														file.getParentFile().mkdirs();
													}
													FileUtils.writeStringToFile(file, err);
													Filedownload.save(file, "text/plain");
												}

												Common.createDefaultTimer(new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														perkuliahan.belum();
														tampikanTab = true;
														initDetail(perkuliahan, groupbox, mulai, banyak);
													}
												});
											}
										});

										new Thread(new Runnable() {

											@Override
											public void run() {
												try {
													FeederConnector feederConnector = new FeederConnector(ip,
															Integer.parseInt(port), null);

													String token = feederConnector.getToken(username, password);
													System.out.println("TOKEN => " + token);

													if (token == null || token.trim().isEmpty()
															|| token.trim().toLowerCase().startsWith("error")) {
														myLabelProsesDetail
																.setValue("Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
														return;
													}

													FeederExporter feederImporter = new FeederExporter(feederConnector,
															token, null, null, myLabelProsesDetail);

													int size = 1;
													int index = 1;
													myLabelProsesDetail.setValue("Memproses " + perkuliahan.info()
															+ " (" + Common.numberFormat.get().format((index * 100.0) / size)
															+ "%");
													index++;
													ais.action.master.PertemuanAction.kirimKeFeeder(feederImporter,
															perkuliahan, feederConnector, token, errorLog);

													// FIX "gagal diam-diam": sebelumnya exception di sini hanya
													// dicatat via tampilErrorJikaAdmin (tak terlihat pengguna) lalu
													// myLabelProsesDetail di-set "" (=SUKSES palsu) di luar try,
													// menutupi kegagalan. Tanda sukses kini menjadi statement
													// terakhir DI DALAM try.
													myLabelProsesDetail.setValue("");
												} catch (Exception e) {
													ais.common.Common.tampilErrorJikaAdmin(e);
													myLabelProsesDetail.setValue(
															"Error: " + ais.common.PesanFormalHelper.pesanGagalException(
																	"pengiriman data Aktifitas Perkuliahan \""
																			+ perkuliahan.info() + "\" ke Neo Feeder",
																	null, e,
																	new String[] {
																			"Periksa kembali koneksi ke server Neo Feeder (Pengaturan Koneksi) dan coba ulangi.",
																			"Pastikan Username/Password Feeder pada Pengaturan Koneksi masih benar.",
																			"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
																	.replace("\n", " "));
												}
											}
										}).start();

									}

								}
							});

				}
			});
			hbox.appendChild(buttonTagihan);

		}

		RecoveryPertemuanHelper.button(perkuliahan, new EventListener() {
			
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						perkuliahan.belum();
						tampikanTab = true;
						initDetail(perkuliahan, groupbox, mulai, banyak);
					}
				});
			}
		}).setParent(hbox);
		button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		button.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiPertemuanHelper revisiHelper = new RevisiPertemuanHelper(perkuliahan, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						perkuliahan.belum();
						tampikanTab = true;
						Session session = HibernateUtil.currentNativeSession();
						List<Pertemuan> pertemuansTemp = session.createCriteria(Pertemuan.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.addOrder(!perkuliahan.getUrutkanotomatis() ? Order.asc("pertemuanKe")
										: Order.asc("tanggal"))
								.add(Restrictions.isNotNull("tanggal")).addOrder(Order.asc("id"))
								.add(Restrictions.eq("perkuliahan", perkuliahan)).list();
						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
						HibernateUtil.closeSession();

						session = HibernateUtil.currentSession();
						perkuliahan.reInitPertemuan(pertemuansTemp, session);
						pertemuansTemp.clear();
						pertemuansTemp = null;

						perkuliahan.belum();
						dataLoader.loadData(null);

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								perkuliahan.belum();
								tampikanTab = true;
								initDetail(perkuliahan, groupbox, mulai, banyak);
							}
						});
					}
				});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();

			}

		});
		button.setParent(hbox);

		return hbox;
	}

	/**
	 * Menambahkan tombol "Kalender" (ikon Google Calendar) ke {@code hbox}, tersembunyi untuk
	 * mahasiswa/siswa. Saat diklik, menentukan {@link PerguruanTinggi} konteks dari dosen/mahasiswa/
	 * fakultas milik {@link Tbmuser} yang login, lalu memproses sinkronisasi Google Calendar untuk
	 * seluruh pertemuan {@code voPembelajaran} via {@link CalendarUtil#proses} dan menunggu hasilnya
	 * dengan {@code CalendarUtil.cretaeTimerWaiting} sebelum memanggil {@code dataLoader.loadData}
	 * untuk me-refresh tampilan.
	 *
	 * @param hbox            parent tempat tombol ditempel.
	 * @param dataLoader      callback reload setelah sinkronisasi kalender selesai.
	 * @param voPembelajaran  objek pembelajaran (mis. {@link Perkuliahan}) sumber daftar pertemuan yang disinkronkan.
	 */
	public static void tampilCalender(Component hbox, final DataLoader dataLoader,
			final VOPembelajaran voPembelajaran) {
		final Tbmuser tbmuser = Common.getCurrentUser();
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Kalender", FileFoto.icon("calendar.google"));
		toolbarbutton.setParent(hbox);
		toolbarbutton.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				CalendarUtil calendarUtil = new CalendarUtil(tbmuser);

				PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

				if (tbmuser != null && tbmuser.ambilDosen() != null
						&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
						&& tbmuser.ambilDosen().getPerguruanTinggi() != null) {
					selectedPerguruanTinggi = tbmuser.ambilDosen().getPerguruanTinggi();
				} else if (tbmuser != null && tbmuser.getMahasiswa() != null
						&& tbmuser.getMahasiswa().getJurusan() != null
						&& tbmuser.getMahasiswa().getJurusan().getFakultas() != null
						&& tbmuser.getMahasiswa().getJurusan().getFakultas().getPerguruanTinggi() != null) {
					selectedPerguruanTinggi = tbmuser.getMahasiswa().getJurusan().getFakultas().getPerguruanTinggi();
				} else if (tbmuser != null && tbmuser.ambilFakultas() != null
						&& tbmuser.ambilFakultas().getPerguruanTinggi() != null) {
					selectedPerguruanTinggi = tbmuser.ambilFakultas().getPerguruanTinggi();
				}
				final List<com.google.api.services.calendar.model.Event> events = new ArrayList<com.google.api.services.calendar.model.Event>();
				calendarUtil.proses(voPembelajaran.ambilPertemuan(), selectedPerguruanTinggi, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						List<com.google.api.services.calendar.model.Event> eventsa = (List<com.google.api.services.calendar.model.Event>) arg0
								.getData();
						events.addAll(eventsa);
					}
				});

				CalendarUtil.cretaeTimerWaiting(events, WaktuUtil.getDate(), new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						dataLoader.loadData(null);
					}
				});
			}
		});
	}

	/**
	 * Varian singkat {@link #initDetail(Perkuliahan, DataLoader, Component, int, int)} dengan
	 * {@code dataLoader} default (dibangkitkan otomatis di dalam method utama, memanggil
	 * {@link #tampilRinci} ulang).
	 *
	 * @param perkuliahan konteks perkuliahan yang panelnya dibangun.
	 * @param groupbox    parent tempat seluruh tabbox ditempel.
	 * @param mulai       indeks pertemuan awal untuk tab Agenda.
	 * @param banyak      jumlah pertemuan per halaman tab Agenda.
	 * @throws Exception diteruskan dari operasi ZK/Hibernate di dalamnya.
	 */
	public void initDetail(Perkuliahan perkuliahan, Component groupbox, int mulai, int banyak) throws Exception {
		initDetail(perkuliahan, null, groupbox, mulai, banyak);
	}

	/** Teks null-safe untuk kolom deskriptif perkuliahan yang bisa kosong. */
	private static String teksAman(String s) {
		return s == null ? "" : s;
	}

	/** Nama {@link ais.database.model.Matakuliah} dari {@code p}, null-safe (string kosong bila perkuliahan/matakuliah/nama null). */
	private static String namaMatakuliah(Perkuliahan p) {
		return p == null || p.getMatakuliah() == null || p.getMatakuliah().getNama() == null ? ""
				: p.getMatakuliah().getNama();
	}

	/** {@link #teksAman(String)} ditambah escaping HTML dasar (&amp;, &lt;, &gt;) untuk teks yang disisipkan ke markup mentah (mis. judul kartu hero). */
	private static String escapeHtmlAman(String s) {
		return teksAman(s).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	/**
	 * Pembungkus aman untuk {@link #displayHeaderInternal(Perkuliahan, Component)}: menangkap
	 * exception (mis. NPE pada kolom Pendahuluan/Capaian/matakuliah yang kosong/rusak) agar tab Home
	 * tidak blank total, dan menampilkan pesan "Sebagian informasi Home gagal dimuat" sebagai
	 * pengganti bila render internal gagal.
	 *
	 * @param perkuliahan konteks perkuliahan.
	 * @param header      tabpanel Home tempat konten ditempel.
	 */
	private void displayHeader(final Perkuliahan perkuliahan, Component header) {
		try {
			displayHeaderInternal(perkuliahan, header);
		} catch (Exception e) {
			/* Jangan biarkan tab Home blank total: NPE pada kolom kosong
			 * (deskripsi/capaian/matakuliah) dulu menggugurkan seluruh render. */
			ais.common.Common.tampilErrorJikaAdmin(e);
			MyFormRow rowError = new MyFormRow();
			Rows rowsError = null;
			try {
				Grid gridError = new Grid();
				gridError.setWidth("100%");
				gridError.setSclass("fgrid");
				gridError.setParent(header);
				rowsError = new Rows();
				rowsError.setParent(gridError);
				rowError.setParent(rowsError);
				rowError.appendChild(new MyLabelBoldMerah(
						"Sebagian informasi Home gagal dimuat. Tutup jendela ini lalu buka kembali."));
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/AktifitasPerkuliahanHelper.java:592");
			}
		}
	}

	/**
	 * Membangun isi tab "Home": kartu hero judul matakuliah, lalu (bila bukan konteks mahasiswa/calon
	 * mahasiswa/peserta kursus/siswa) blok Pendahuluan, Deskripsi Pembelajaran, dan Capaian/Kompetensi
	 * — masing-masing dengan mode tampil (label HTML read-only) dan mode edit (textbox/CKEditor +
	 * tombol Simpan yang langsung memanggil {@code Common.refreshUpdate} pada {@link Perkuliahan}),
	 * plus tombol "Generate ..." yang memakai {@link AIGenerator} untuk mengisi teks tersebut otomatis
	 * dari LLM. Selanjutnya membangun empat kartu ringkasan (Aktifitas Perkuliahan, Kegiatan Belajar
	 * Mengajar, Keaktifan Peserta Perkuliahan, Rekapitulasi Pembelajaran) yang masing-masing dimuat
	 * on-demand lewat tombol "Tampilkan" kecuali {@link #tampilLangsungRinci} true, dan terakhir
	 * lampiran pendukung tingkat matakuliah via {@link #tampilkanLampiran}.
	 *
	 * @param perkuliahan konteks perkuliahan.
	 * @param header      tabpanel Home tempat seluruh grid/kartu ditempel.
	 */
	private void displayHeaderInternal(final Perkuliahan perkuliahan, Component header) {

		/* Borderlayout/Center lama dihapus: tanpa tinggi eksplisit Borderlayout
		 * ZK 5 bisa kolaps menjadi 0px sehingga seluruh isi Home tidak terlihat.
		 * Grid kini langsung menjadi anak tabpanel dan tingginya mengikuti isi. */
		Grid grid = new Grid();
		grid.setWidth("100%");
		grid.setParent(header);
		grid.setStyle("min-height: 400px;");
		grid.setSclass("fgrid ais-aktifitas-home");

		Columns columns = new Columns();
		columns.setParent(grid);
		Column column = new Column();
		column.setParent(columns);
		column.setWidth("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		/* Kartu hero: identitas perkuliahan di paling atas (pola dasbor). */
		String namaMk = namaMatakuliah(perkuliahan);
		MyFormRow rowHero = new MyFormRow();
		rowHero.setValign("top");
		rowHero.setParent(rows);
		StringBuilder hero = new StringBuilder();
		hero.append("<div class=\"ais-aktifitas-hero\">");
		hero.append("<div class=\"ais-aktifitas-hero-judul\">")
				.append(escapeHtmlAman(namaMk.trim().isEmpty() ? "Perkuliahan" : namaMk)).append("</div>");
		hero.append("<div class=\"ais-aktifitas-hero-sub\">Pendahuluan, deskripsi pembelajaran, capaian, dan aktivitas kelas dalam satu halaman.</div>");
		hero.append("</div>");
		rowHero.appendChild(new Html(hero.toString()));

		if (perkuliahan != null && mahasiswa == null && biodataCalonMahasiswa == null
				&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null) {

			final MyFormRow row = new MyFormRow();
			row.setValign("top");
			final MyFormRow rowEdit = new MyFormRow();
			final MyCkEditor pendahuluan = new MyCkEditor();
			final Html labelPendahuluan = new ais.ui.util.MyHtml(teksAman(perkuliahan.getPendahuluan()));

			if (mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
					&& tbmuser.getSiswa() == null) {
				MyToolbarbutton toolbarbutton = new MyToolbarbutton("fa-cog", "Generate Pendahuluan");

				toolbarbutton.setParent(pendahuluan.hbox);

				String tanya = "Apakah yang dimaksud matakuliah \"" + namaMatakuliah(perkuliahan)
						+ "\" secara lengkap dan detail";

				String tanyaMengajar = " matakuliah " + namaMatakuliah(perkuliahan);

				String tanyaAkhiran = "";
				toolbarbutton.addEventListener("onClick", AIGenerator.generateApa("Generate Pendahuluan",
						"Pendahuluan tentang apa ?", tanya, true, tanyaAkhiran,
						Common.getKonfigurasi("llama_system_pengajar", "Kamu adalah Pengajar atau Dosen atau Guru ")
								.getNilai().trim(),
						null, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								HibernateUtil.currentSession().refresh(perkuliahan);
								pendahuluan.setValue(ais.action.servlet.Wa.ubahKeBold((arg0.getData() + ""))
										.replaceAll("\n", "<br>"));
								perkuliahan.setPendahuluan(pendahuluan.getValue());
								Common.refreshUpdate(perkuliahan);
								labelPendahuluan.setContent(perkuliahan.getPendahuluan());

							}
						}, tanyaMengajar, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								pendahuluan.setValue(ais.action.servlet.Wa.ubahKeBold((arg0.getData() + ""))
										.replaceAll("\n", "<br>"));
							}
						}));

			}

			rowEdit.setParent(rows);
			rowEdit.setVisible(false);

			MyGroupboxStyled vbox1 = new MyGroupboxStyled();
			rowEdit.appendChild(vbox1);
			Hbox hbox = new Hbox();
			vbox1.appendChild(new MyCaptionStyled("Pendahuluan"));
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
			button.setTooltiptext("Simpan Data");
			button.setVisible(mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
					&& tbmuser.getSiswa() == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					row.setVisible(true);
					rowEdit.setVisible(false);
					HibernateUtil.currentSession().refresh(perkuliahan);
					perkuliahan.setPendahuluan(pendahuluan.getValue());
					Common.refreshUpdate(perkuliahan);
					labelPendahuluan.setContent(perkuliahan.getPendahuluan());
				}

			});
			button.setParent(hbox);
			hbox.setParent(vbox1);

			vbox1.appendChild(pendahuluan);
			pendahuluan.setValue(teksAman(perkuliahan.getPendahuluan()));
			pendahuluan.setHeight("200px");
			pendahuluan.setWidth("100%");

			row.setParent(rows);

			vbox1 = new MyGroupboxStyled();
			row.appendChild(vbox1);
			hbox = new Hbox();
			vbox1.appendChild(new MyCaptionStyled("Pendahuluan"));
			hbox.appendChild(new Space());
			hbox.appendChild(new Space());
			button = new MyToolbarbuttonConfig("Ubah", "/img/edit-icon.png");
			button.setTooltiptext("Ubah Data");
			button.setVisible(mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
					&& tbmuser.getSiswa() == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					row.setVisible(false);
					rowEdit.setVisible(true);
				}

			});
			button.setParent(hbox);
			hbox.setParent(vbox1);

			vbox1.appendChild(labelPendahuluan);
		}

		final Textbox pembelajaran = new Textbox();
		final Html labelPembelajaran = new ais.ui.util.MyHtml(
				teksAman(perkuliahan.getDeskripsiPembelajaran()).replaceAll("\n", "<br>"));

		if (perkuliahan != null && mahasiswa == null && biodataCalonMahasiswa == null
				&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setValign("top");
			row.setParent(rows);
			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Generate Deskripsi Pembelajaran",
					"/img/svg/gear.svg");
			row.appendChild(toolbarbutton);

			String tanya = "Apakah deskripsi dan tata cara pembelajaran matakuliah \""
					+ namaMatakuliah(perkuliahan) + "\"";
			String tanyaMengajar = " matakuliah " + namaMatakuliah(perkuliahan);
			String tanyaAkhiran = "";
			toolbarbutton.addEventListener("onClick",
					AIGenerator.generateApa("Generate Deskripsi", "Deskripsi tentang apa ?", tanya, true, tanyaAkhiran,
							Common.getKonfigurasi("llama_system_pengajar", "Kamu adalah Pengajar atau Dosen atau Guru ")
									.getNilai().trim(),
							null, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									pembelajaran.setValue(ais.action.servlet.Wa.ubahKeBold((arg0.getData() + ""))
											.replaceAll("\n", "<br>"));
									HibernateUtil.currentSession().refresh(perkuliahan);
									perkuliahan.setDeskripsiPembelajaran(pembelajaran.getValue());
									Common.refreshUpdate(perkuliahan);
									labelPembelajaran.setContent(perkuliahan.getDeskripsiPembelajaran());

								}
							}, tanyaMengajar, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									labelPembelajaran.setContent(ais.action.servlet.Wa.ubahKeBold((arg0.getData() + ""))
											.replaceAll("\n", "<br>"));
								}
							}));
		}

		if (perkuliahan != null) {

			final MyFormRow row = new MyFormRow();
			row.setValign("top");
			final MyFormRow rowEdit = new MyFormRow();

			rowEdit.setParent(rows);
			rowEdit.setVisible(false);

			MyGroupboxStyled vbox1 = new MyGroupboxStyled();
			rowEdit.appendChild(vbox1);
			Hbox hbox = new Hbox();
			vbox1.appendChild(new MyCaptionStyled("Deskripsi Pembelajaran"));
			hbox.appendChild(new Space());
			hbox.appendChild(new Space());
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
			button.setTooltiptext("Simpan Data");
			button.setVisible(mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
					&& tbmuser.getSiswa() == null);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {

					row.setVisible(true);
					rowEdit.setVisible(false);
					HibernateUtil.currentSession().refresh(perkuliahan);
					perkuliahan.setDeskripsiPembelajaran(pembelajaran.getValue());
					Common.refreshUpdate(perkuliahan);
					labelPembelajaran.setContent(teksAman(perkuliahan.getDeskripsiPembelajaran()).replaceAll("\n", "<br>"));
				}

			});
			button.setParent(hbox);
			hbox.setParent(vbox1);

			vbox1.appendChild(pembelajaran);
			pembelajaran.setValue(teksAman(perkuliahan.getDeskripsiPembelajaran()));
			pembelajaran.setRows(5);
			pembelajaran.setWidth("100%");

			row.setParent(rows);

			vbox1 = new MyGroupboxStyled();
			row.appendChild(vbox1);
			hbox = new Hbox();
			vbox1.appendChild(new MyCaptionStyled("Deskripsi Pembelajaran"));
			hbox.appendChild(new Space());
			hbox.appendChild(new Space());
			button = new MyToolbarbuttonConfig("Ubah", "/img/edit-icon.png");
			button.setTooltiptext("Ubah Data");
			button.setVisible(mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
					&& tbmuser.getSiswa() == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					row.setVisible(false);
					rowEdit.setVisible(true);
				}

			});
			button.setParent(hbox);
			hbox.setParent(vbox1);

			vbox1.appendChild(labelPembelajaran);

		}

		final Textbox capaian = new Textbox();
		final Html labelcapaian = new ais.ui.util.MyHtml(
				teksAman(perkuliahan.getCapaianPembelajaranProdi()).replaceAll("\n", "<br>"));

		if (perkuliahan != null && mahasiswa == null && biodataCalonMahasiswa == null
				&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setValign("top");
			row.setParent(rows);
			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Generate Capaian / kompetensi",
					"/img/svg/gear.svg");
			row.appendChild(toolbarbutton);

			String tanya = "Apakah capaian atau kompetensi yang ingin dicapai setelah belajar matakuliah \""
					+ namaMatakuliah(perkuliahan) + "\"";

			String tanyaAkhiran = "";
			String tanyaMengajar = " matakuliah " + namaMatakuliah(perkuliahan);
			toolbarbutton.addEventListener("onClick",
					AIGenerator.generateApa("Generate Capaian / kompetensi", "Capaian / kompetensi tentang apa ?",
							tanya, true, tanyaAkhiran,
							Common.getKonfigurasi("llama_system_pengajar", "Kamu adalah Pengajar atau Dosen atau Guru ")
									.getNilai().trim(),
							null, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									HibernateUtil.currentSession().refresh(perkuliahan);
									capaian.setValue(ais.action.servlet.Wa.ubahKeBold((arg0.getData() + ""))
											.replaceAll("\n", "<br>"));
									perkuliahan.setCapaianPembelajaranProdi(capaian.getValue());
									Common.refreshUpdate(perkuliahan);
									labelcapaian.setContent(perkuliahan.getCapaianPembelajaranProdi());

								}
							}, tanyaMengajar, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									labelcapaian.setContent(ais.action.servlet.Wa.ubahKeBold((arg0.getData() + ""))
											.replaceAll("\n", "<br>"));
								}
							}));

		}

		if (perkuliahan != null)

		{

			final MyFormRow row = new MyFormRow();
			row.setValign("top");
			final MyFormRow rowEdit = new MyFormRow();

			rowEdit.setParent(rows);
			rowEdit.setVisible(false);

			MyGroupboxStyled vbox1 = new MyGroupboxStyled();
			rowEdit.appendChild(vbox1);
			Hbox hbox = new Hbox();
			vbox1.appendChild(new MyCaptionStyled("Capaian / Kompetensi"));
			hbox.appendChild(new Space());
			hbox.appendChild(new Space());
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
			button.setTooltiptext("Simpan Data");
			button.setVisible(mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
					&& tbmuser.getSiswa() == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					row.setVisible(true);
					rowEdit.setVisible(false);
					HibernateUtil.currentSession().refresh(perkuliahan);
					perkuliahan.setCapaianPembelajaranProdi(capaian.getValue());
					Common.refreshUpdate(perkuliahan);
					labelcapaian.setContent(teksAman(perkuliahan.getCapaianPembelajaranProdi()).replaceAll("\n", "<br>"));
				}

			});
			button.setParent(hbox);
			hbox.setParent(vbox1);

			vbox1.appendChild(capaian);
			capaian.setValue(teksAman(perkuliahan.getCapaianPembelajaranProdi()));
			capaian.setRows(5);
			capaian.setWidth("100%");

			row.setParent(rows);

			vbox1 = new MyGroupboxStyled();
			row.appendChild(vbox1);
			hbox = new Hbox();
			vbox1.appendChild(new MyCaptionStyled("Capaian / Kompetensi"));
			hbox.appendChild(new Space());
			hbox.appendChild(new Space());
			button = new MyToolbarbuttonConfig("Ubah", "/img/edit-icon.png");
			button.setTooltiptext("Ubah Data");
			button.setVisible(mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
					&& tbmuser.getSiswa() == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					row.setVisible(false);
					rowEdit.setVisible(true);
				}

			});
			button.setParent(hbox);
			hbox.setParent(vbox1);

			vbox1.appendChild(labelcapaian);

		}

		if (perkuliahan != null) {

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setValign("top");
			row.setParent(rows);

			final MyGroupboxStyled vbox1 = new MyGroupboxStyled();
			row.appendChild(vbox1);
			Hbox hbox = new Hbox();
			vbox1.appendChild(new MyCaptionStyled("Aktifitas Perkuliahan"));
			hbox.appendChild(new Space());
			hbox.appendChild(new Space());
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tampilkan", "/img/eye-icon.png");
			button.setTooltiptext("Tampilkan Data");
			button.setVisible(!tampilLangsungRinci);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					event.getTarget().setVisible(false);
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							vbox1.appendChild(Common.getDeskripsiPerkuliahanHbox(perkuliahan));
						}
					});
				}

			});
			button.setParent(hbox);
			hbox.setParent(vbox1);

			row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);

			final Vbox finalRowMahasiswa = new Vbox();
			final Vbox finalRowDosen = new Vbox();

			MyGroupboxStyled vbox2 = new MyGroupboxStyled();
			row.appendChild(vbox2);
			final Hbox hboxa = new Hbox();
			vbox2.appendChild(new MyCaptionStyled("Kegiatan Belajar Mengajar"));
			hboxa.appendChild(new Space());
			hboxa.appendChild(new Space());
			button = new MyToolbarbuttonConfig("Tampilkan", "/img/eye-icon.png");
			button.setVisible(!tampilLangsungRinci);
			button.setTooltiptext("Tampilkan Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					event.getTarget().setVisible(false);
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							TampilanELearningAction.tampilkanStatistikAktifitasMahasiswa(perkuliahan, finalRowMahasiswa,
									finalRowDosen, hboxa);
						}
					});
				}

			});
			button.setParent(hboxa);
			hboxa.setParent(vbox2);

			finalRowMahasiswa.setParent(vbox2);
			finalRowDosen.setParent(vbox2);

			row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);

			final Vbox finalRowMahasiswaA = new Vbox();
			final Vbox finalRowDosenA = new Vbox();

			MyGroupboxStyled vbox2A = new MyGroupboxStyled();
			row.appendChild(vbox2A);
			final Hbox hboxaA = new Hbox();
			vbox2A.appendChild(new MyCaptionStyled("Keaktifan Peserta Perkuliahan"));
			hboxaA.appendChild(new Space());
			hboxaA.appendChild(new Space());
			button = new MyToolbarbuttonConfig("Tampilkan", "/img/eye-icon.png");
			button.setVisible(!tampilLangsungRinci);
			button.setTooltiptext("Tampilkan Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					event.getTarget().setVisible(false);
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							TampilanELearningAction.tampilkanStatistikKeaktifanPeserta(perkuliahan,
									perkuliahan.getJurusan(), finalRowMahasiswaA, finalRowDosenA, hboxaA);
						}
					});
				}

			});
			button.setParent(hboxaA);
			hboxaA.setParent(vbox2A);

			finalRowMahasiswaA.setParent(vbox2A);
			finalRowDosenA.setParent(vbox2A);

			row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);

			final Vbox finalRowPertemuan = new Vbox();

			vbox2 = new MyGroupboxStyled();
			row.appendChild(vbox2);
			final Hbox hboxaa = new Hbox();
			vbox2.appendChild(new MyCaptionStyled("Rekapitulasi Pembelajaran"));
			hboxaa.appendChild(new Space());
			hboxaa.appendChild(new Space());
			button = new MyToolbarbuttonConfig("Tampilkan", "/img/eye-icon.png");
			button.setVisible(!tampilLangsungRinci);
			button.setTooltiptext("Tampilkan Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					event.getTarget().setVisible(false);
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							TampilanELearningAction.tampilkanRekapPerkuliahan(perkuliahan, finalRowPertemuan, hboxaa);
						}
					});
				}

			});
			button.setParent(hboxaa);
			hboxaa.setParent(vbox2);

			finalRowPertemuan.setParent(vbox2);

			if (tampilLangsungRinci) {
				try {
					vbox1.appendChild(Common.getDeskripsiPerkuliahanHbox(perkuliahan));
					TampilanELearningAction.tampilkanStatistikAktifitasMahasiswa(perkuliahan, finalRowMahasiswa,
							finalRowDosen, hboxa);
					TampilanELearningAction.tampilkanStatistikKeaktifanPeserta(perkuliahan, perkuliahan.getJurusan(),
							finalRowMahasiswaA, finalRowDosenA, hboxaA);
					TampilanELearningAction.tampilkanRekapPerkuliahan(perkuliahan, finalRowPertemuan, hboxaa);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AktifitasPerkuliahanHelper.java:1119");
				}
			}

			if (perkuliahan.getMatakuliah() != null) {
				AktifitasPerkuliahanHelper.tampilkanLampiran(rows, perkuliahan.getId(),
						perkuliahan.getMatakuliah().getId(), "", "_matakuliah",
						mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
								&& tbmuser.getSiswa() == null,
						"");
			}
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		MyLabelAgakKecilBold label = new MyLabelAgakKecilBold(
				"*)  Untuk melihat semua Agenda Pertemuan, klik tab \"Agenda\" di atas.");
		label.setSclass("ais-aktifitas-catatan");
		row.appendChild(label);
	}

	/**
	 * Menyalin (clone) satu {@link LampiranLain} dari referensi lama ({@code refAmbilDari} + jenis
	 * bertanda {@code tambahanAmbilDari}) menjadi lampiran baru milik {@code ref} bila lampiran tujuan
	 * ({@code ref} + {@code jenis}) belum ada — dipakai saat perkuliahan paralel/salinan agenda ingin
	 * mewarisi lampiran (RPS/SAP/dst.) dari matakuliah sumbernya. Hanya berjalan bila
	 * {@code tambahanAmbilDari} diisi dan {@code tambahan} kosong (menandai konteks "salinan", bukan
	 * upload manual baru). Clone menyimpan jejak asal via {@code setCopyDari}. Transaksi Hibernate
	 * dibuka/di-commit manual; kegagalan di-rollback dan ditampilkan hanya untuk admin
	 * ({@code Common.tampilErrorJikaAdmin}) tanpa melempar exception ke pemanggil.
	 *
	 * @param ref               id entity tujuan (mis. id {@link Perkuliahan} atau matakuliah) yang lampirannya diperiksa/diisi.
	 * @param refAmbilDari      id entity sumber tempat lampiran asli disalin.
	 * @param tambahan          suffix jenis pada sisi tujuan; harus kosong agar penyalinan dijalankan.
	 * @param tambahanAmbilDari suffix jenis pada sisi sumber; harus diisi agar penyalinan dijalankan.
	 * @param jenis             kode jenis lampiran (mis. {@link LampiranLain#SILABUS}, {@link LampiranLain#SAP}).
	 */
	public static void chekSimpan(Long ref, Long refAmbilDari, String tambahan, String tambahanAmbilDari,
			String jenis) {
		if (tambahanAmbilDari != null && !tambahanAmbilDari.trim().isEmpty() && refAmbilDari != null) {
			if (tambahan == null || tambahan.trim().isEmpty()) {
				LampiranLain lampiranLain = LampiranLain.ambil(ref, jenis);
				if (lampiranLain == null || lampiranLain.getId() == null) {
					LampiranLain lampiranLainData = LampiranLain.ambil(refAmbilDari, jenis + tambahanAmbilDari);
					if (lampiranLainData != null && lampiranLainData.getId() != null) {
						try {
							LampiranLain lampiran = (LampiranLain) lampiranLainData.clone();
							lampiran.setId(null);
							lampiran.setCopyDari(lampiranLainData);
							lampiran.setRef(ref);
							lampiran.setJenis(jenis);

							Session session = StreamingHibernateUtil.getInstance().currentSession();
							session.getTransaction().begin();
							session.save(lampiran);
							session.getTransaction().commit();

							StreamingHibernateUtil.getInstance().closeSession();
						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}
					}
				}
			}
		}
	}

	/**
	 * Menampilkan baris "Lampiran Pendukung" berisi widget upload/download file untuk RPS, SAP,
	 * Absen Manual, Soal UTS, Soal UAS (masing-masing hanya bila konfigurasi {@code tampilkan_*}
	 * terkait aktif), seluruh entri statis {@link #lampiranLain} yang diaktifkan via konfigurasi
	 * {@code tampilkan_&lt;nama&gt;}, serta jenis tambahan dinamis dari konfigurasi
	 * {@code tampilkan_lampiran_lain_di_agenda} (daftar dipisah koma). Untuk tiap jenis, memanggil
	 * {@link #chekSimpan} lebih dulu (menyalin dari sumber bila ini konteks salinan/paralel), lalu
	 * membangun widget {@code LampiranLain.createDownloadUploadFileLain} yang saat file diunggah
	 * langsung melakukan update Hibernate ({@code session.update}) untuk mematri {@code ref} pada
	 * lampiran tersebut.
	 *
	 * @param rows              Rows ZK tempat baris-baris lampiran ditambahkan.
	 * @param ref               id entity pemilik lampiran (mis. id {@link Perkuliahan} atau matakuliah).
	 * @param refAmbilDari      id entity sumber untuk penyalinan otomatis via {@link #chekSimpan}.
	 * @param tambahan          suffix jenis pada sisi tujuan (mis. kosong untuk konteks utama).
	 * @param tambahanAmbilDari suffix jenis pada sisi sumber.
	 * @param bolehUpload       true bila pengguna saat ini boleh mengunggah/mengganti file.
	 * @param span              nilai colspan ZK opsional untuk merentangkan baris (kosong/null = default).
	 */
	@SuppressWarnings("deprecation")
	public static void tampilkanLampiran(Rows rows, final Long ref, final Long refAmbilDari, final String tambahan,
			final String tambahanAmbilDari, final boolean bolehUpload, final String span) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		if (span != null && !span.isEmpty()) {
			ais.ui.util.ZkCompat.setSpans(row, span);
		}
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new MyLabelBold("Lampiran Pendukung"));

		if (Common.bolehKonfigurasi("tampilkan_rps")) {
			row = new MyFormRow();
			if (span != null && !span.isEmpty()) {
				ais.ui.util.ZkCompat.setSpans(row, span);
			}
			row.setValign("top");
			row.setParent(rows);

			AktifitasPerkuliahanHelper.chekSimpan(ref, refAmbilDari, tambahan, tambahanAmbilDari, LampiranLain.SILABUS);

			MyFormRow rowPreview = new MyFormRow();
			if (span != null && !span.isEmpty()) {
				ais.ui.util.ZkCompat.setSpans(rowPreview, span);
			}
			rowPreview.setValign("top");
			rowPreview.setParent(rows);

			Hbox hbox1 = new Hbox();
			hbox1.setParent(row);
			LampiranLain.createDownloadUploadFileLain(hbox1, ref, LampiranLain.SILABUS + tambahan, "RPS", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LampiranLain ttd = (LampiranLain) arg0.getData();
							try {
								Session session = StreamingHibernateUtil.getInstance().currentSession();

								session.refresh(ttd);
								ttd.setRef(ref);

								session.getTransaction().begin();
								session.update(ttd);
								session.getTransaction().commit();

								StreamingHibernateUtil.getInstance().closeSession();
							} catch (Exception e) {
								StreamingHibernateUtil.getInstance().rollbackTransaction();
								Common.tampilErrorJikaAdmin(e);
							}

						}
					}, null, false, false, false, bolehUpload, null, false, false, rowPreview);
		}

		Row rowPreview;
		Hbox hbox1;
		if (Common.bolehKonfigurasi("tampilkan_sap")) {
			row = new MyFormRow();
			if (span != null && !span.isEmpty()) {
				ais.ui.util.ZkCompat.setSpans(row, span);
			}
			row.setValign("top");
			row.setParent(rows);

			AktifitasPerkuliahanHelper.chekSimpan(ref, refAmbilDari, tambahan, tambahanAmbilDari, LampiranLain.SAP);

			rowPreview = new MyFormRow();
			if (span != null && !span.isEmpty()) {
				ais.ui.util.ZkCompat.setSpans(rowPreview, span);
			}
			rowPreview.setValign("top");
			rowPreview.setParent(rows);

			hbox1 = new Hbox();
			hbox1.setParent(row);
			LampiranLain.createDownloadUploadFileLain(hbox1, ref, LampiranLain.SAP + tambahan, "SAP", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LampiranLain ttd = (LampiranLain) arg0.getData();
							try {
								Session session = StreamingHibernateUtil.getInstance().currentSession();

								session.refresh(ttd);
								ttd.setRef(ref);

								session.getTransaction().begin();
								session.update(ttd);
								session.getTransaction().commit();

								StreamingHibernateUtil.getInstance().closeSession();
							} catch (Exception e) {
								StreamingHibernateUtil.getInstance().rollbackTransaction();
								Common.tampilErrorJikaAdmin(e);
							}
						}
					}, null, false, false, false, bolehUpload, null, false, false, rowPreview);

		}

		if (Common.bolehKonfigurasi("tampilkan_absen_manual")) {
			row = new MyFormRow();
			if (span != null && !span.isEmpty()) {
				ais.ui.util.ZkCompat.setSpans(row, span);
			}
			row.setValign("top");
			row.setParent(rows);

			AktifitasPerkuliahanHelper.chekSimpan(ref, refAmbilDari, tambahan, tambahanAmbilDari, "Absen Manual");

			rowPreview = new MyFormRow();
			if (span != null && !span.isEmpty()) {
				ais.ui.util.ZkCompat.setSpans(rowPreview, span);
			}
			rowPreview.setValign("top");
			rowPreview.setParent(rows);

			hbox1 = new Hbox();
			hbox1.setParent(row);
			LampiranLain.createDownloadUploadFileLain(hbox1, ref, "Absen Manual" + tambahan, "Absen Manual", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LampiranLain ttd = (LampiranLain) arg0.getData();
							try {
								Session session = StreamingHibernateUtil.getInstance().currentSession();

								session.refresh(ttd);
								ttd.setRef(ref);

								session.getTransaction().begin();
								session.update(ttd);
								session.getTransaction().commit();

								StreamingHibernateUtil.getInstance().closeSession();
							} catch (Exception e) {
								StreamingHibernateUtil.getInstance().rollbackTransaction();
								Common.tampilErrorJikaAdmin(e);
							}
						}
					}, null, false, false, false, bolehUpload, null, false, false, rowPreview);
		}

		if (Common.bolehKonfigurasi("tampilkan_soal_uts"))

		{
			row = new MyFormRow();
			if (span != null && !span.isEmpty()) {
				ais.ui.util.ZkCompat.setSpans(row, span);
			}
			row.setValign("top");
			row.setParent(rows);

			AktifitasPerkuliahanHelper.chekSimpan(ref, refAmbilDari, tambahan, tambahanAmbilDari, "Soal UTS");

			rowPreview = new MyFormRow();
			if (span != null && !span.isEmpty()) {
				ais.ui.util.ZkCompat.setSpans(rowPreview, span);
			}
			rowPreview.setValign("top");
			rowPreview.setParent(rows);

			hbox1 = new Hbox();
			hbox1.setParent(row);
			LampiranLain.createDownloadUploadFileLain(hbox1, ref, "Soal UTS" + tambahan, "Soal UTS", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LampiranLain ttd = (LampiranLain) arg0.getData();
							try {
								Session session = StreamingHibernateUtil.getInstance().currentSession();

								session.refresh(ttd);
								ttd.setRef(ref);

								session.getTransaction().begin();
								session.update(ttd);
								session.getTransaction().commit();

								StreamingHibernateUtil.getInstance().closeSession();
							} catch (Exception e) {
								StreamingHibernateUtil.getInstance().rollbackTransaction();
								Common.tampilErrorJikaAdmin(e);
							}
						}
					}, null, false, false, false, bolehUpload, null, false, false, rowPreview);
		}

		if (Common.bolehKonfigurasi("tampilkan_soal_uas")) {
			row = new MyFormRow();
			if (span != null && !span.isEmpty()) {
				ais.ui.util.ZkCompat.setSpans(row, span);
			}
			row.setValign("top");
			row.setParent(rows);

			AktifitasPerkuliahanHelper.chekSimpan(ref, refAmbilDari, tambahan, tambahanAmbilDari, "Soal UAS");

			rowPreview = new MyFormRow();
			if (span != null && !span.isEmpty()) {
				ais.ui.util.ZkCompat.setSpans(rowPreview, span);
			}
			rowPreview.setValign("top");
			rowPreview.setParent(rows);

			hbox1 = new Hbox();
			hbox1.setParent(row);
			LampiranLain.createDownloadUploadFileLain(hbox1, ref, "Soal UAS" + tambahan, "Soal UAS", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LampiranLain ttd = (LampiranLain) arg0.getData();
							try {
								Session session = StreamingHibernateUtil.getInstance().currentSession();

								session.refresh(ttd);
								ttd.setRef(ref);

								session.getTransaction().begin();
								session.update(ttd);
								session.getTransaction().commit();

								StreamingHibernateUtil.getInstance().closeSession();
							} catch (Exception e) {
								StreamingHibernateUtil.getInstance().rollbackTransaction();
								Common.tampilErrorJikaAdmin(e);
							}
						}
					}, null, false, false, false, bolehUpload, null, false, false, rowPreview);

		}

		for (String t : lampiranLain) {

			if (Common.bolehKonfigurasi("tampilkan_" + t, Konfigurasi.TIDAK_AKTIF)) {
				row = new MyFormRow();
				if (span != null && !span.isEmpty()) {
					ais.ui.util.ZkCompat.setSpans(row, span);
				}
				row.setValign("top");
				row.setParent(rows);

				AktifitasPerkuliahanHelper.chekSimpan(ref, refAmbilDari, tambahan, tambahanAmbilDari, t);

				rowPreview = new MyFormRow();
				if (span != null && !span.isEmpty()) {
					ais.ui.util.ZkCompat.setSpans(rowPreview, span);
				}
				rowPreview.setValign("top");
				rowPreview.setParent(rows);

				hbox1 = new Hbox();
				hbox1.setParent(row);
				LampiranLain.createDownloadUploadFileLain(hbox1, ref, t + tambahan, t, false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						LampiranLain ttd = (LampiranLain) arg0.getData();
						try {
							Session session = StreamingHibernateUtil.getInstance().currentSession();

							session.refresh(ttd);
							ttd.setRef(ref);

							session.getTransaction().begin();
							session.update(ttd);
							session.getTransaction().commit();

							StreamingHibernateUtil.getInstance().closeSession();
						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}
					}
				}, null, false, false, false, bolehUpload, null, false, false, rowPreview);

			}
		}

		String tampilkan_lampiran_lain_di_agenda = Common.getKonfigurasi("tampilkan_lampiran_lain_di_agenda", "")
				.getNilai();
		if (tampilkan_lampiran_lain_di_agenda != null && !tampilkan_lampiran_lain_di_agenda.trim().isEmpty()) {
			for (String s : tampilkan_lampiran_lain_di_agenda.split(",")) {

				AktifitasPerkuliahanHelper.chekSimpan(ref, refAmbilDari, tambahan, tambahanAmbilDari, s);

				row = new MyFormRow();
				if (span != null && !span.isEmpty()) {
					ais.ui.util.ZkCompat.setSpans(row, span);
				}
				row.setValign("top");
				row.setParent(rows);

				rowPreview = new MyFormRow();
				if (span != null && !span.isEmpty()) {
					ais.ui.util.ZkCompat.setSpans(rowPreview, span);
				}
				rowPreview.setValign("top");
				rowPreview.setParent(rows);

				hbox1 = new Hbox();
				hbox1.setParent(row);
				LampiranLain.createDownloadUploadFileLain(hbox1, ref, s + tambahan, s, false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						LampiranLain ttd = (LampiranLain) arg0.getData();
						try {
							Session session = StreamingHibernateUtil.getInstance().currentSession();

							session.refresh(ttd);
							ttd.setRef(ref);

							session.getTransaction().begin();
							session.update(ttd);
							session.getTransaction().commit();

							StreamingHibernateUtil.getInstance().closeSession();
						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}
					}
				}, null, false, false, false, bolehUpload, null, false, false, rowPreview);
			}
		}
	}

	/**
	 * Method utama yang membangun seluruh tabbox "Aktifitas Perkuliahan" (lihat daftar tab pada
	 * Javadoc kelas) dan menempelkannya ke {@code groupbox}. Membersihkan {@code groupbox} lebih
	 * dulu ({@code Common.clear}), lalu membuat tab Home (eager, langsung terisi via
	 * {@link #displayHeader}) dan tab-tab lain yang kontennya baru dimuat saat pertama diklik (pola
	 * lazy-load dengan guard {@code getChildren().isEmpty()} agar tidak dimuat ulang). Menyimpan
	 * {@code mulai}/{@code banyak}/{@code groupbox} ke field instance untuk dipakai ulang oleh
	 * listener toolbar (Refresh, Tambah Agenda, History, dll.) yang memanggil {@code initDetail} lagi
	 * setelah aksinya selesai. Bila {@link #tampikanTab} true (tab Agenda sebelumnya aktif), konten
	 * Agenda di-render lewat {@code Common.createDefaultTimer} dengan jeda 1 detik agar panel sudah
	 * benar-benar tampil di client sebelum diisi (menghindari race condition ukuran 0 pada renderer
	 * PDF/iframe).
	 *
	 * @param perkuliahan konteks perkuliahan yang panelnya dibangun; tidak boleh null.
	 * @param mydataLoader callback reload custom; bila null, dibuatkan default yang memanggil
	 *                     {@link #tampilRinci} untuk tab Agenda.
	 * @param groupbox    parent tempat tabbox ditempel; dibersihkan sebelum diisi ulang.
	 * @param mulai       indeks pertemuan awal untuk tab Agenda.
	 * @param banyak      jumlah pertemuan per halaman tab Agenda.
	 * @throws Exception diteruskan dari operasi ZK/Hibernate di dalamnya.
	 */
	@SuppressWarnings({})
	public void initDetail(final Perkuliahan perkuliahan, final DataLoader mydataLoader, final Component groupbox,
			final int mulai, final int banyak) throws Exception {
		this.mulai = mulai;
		this.banyak = banyak;
		this.groupbox = groupbox;
		tabpanelAgenda = new ais.ui.util.MyTabpanel();
		final DataLoader dataLoader = mydataLoader == null ? new DataLoader() {

			@Override
			public void loadData(Object value) {
				try {
					tampilRinci(perkuliahan, this, tabpanelAgenda, groupbox, mulai, banyak, false);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AktifitasPerkuliahanHelper.java:1521");
				}
			}
		} : mydataLoader;

		Common.clear(groupbox);

		final Tabbox tabbox = new Tabbox();
		tabbox.setSclass("ais-aktifitas-tabbox");
		tabbox.setParent(groupbox);
		tabbox.setWidth("100%");
		tabbox.setHeight("100%");
		/* Styling modern + responsif: css_utama.css blok "AKTIFITAS TABBOX MODERN" */
		tabbox.setSclass("ais-aktifitas-tabbox");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tabPendahuluan = new MyTabConfig("Home", "/img/home-icon.png");
		tabPendahuluan.setParent(tabs);
		tabPendahuluan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				tampikanTab = false;
			}
		});

		final MyTabConfig tab = new MyTabConfig("Agenda", "/img/jadwal.png");
		tab.setParent(tabs);

		final MyTabConfig tabPengumuman = new MyTabConfig("Info", "/img/Status-dialog-information-icon.png");
		tabPengumuman.setParent(tabs);

		final MyTabConfig tabReferensi = new MyTabConfig("Ref.", "/img/Blue-Books-icon.png");
		tabReferensi.setParent(tabs);
		
		
		final MyTabConfig tabUjian = new MyTabConfig("Ujian", "/img/svg/user-edit.svg");
		tabUjian.setParent(tabs);

		final MyTabConfig tabTgs = new MyTabConfig("Tgs", "/img/svg/task-line.svg");
		tabTgs.setParent(tabs);

		final MyTabConfig tabTugasKelompok = new MyTabConfig("Tgs.Kel.", "/img/Document-scheduled-tasks-icon.png");
		tabTugasKelompok.setParent(tabs);
		
		

		final MyTabConfig tabPenilaian = new MyTabConfig("Nilai", "/img/svg/check2.svg");
		tabPenilaian.setParent(tabs);

		final MyTabConfig tabLaporan = new MyTabConfig("Lap.", "/img/print.png");
		tabLaporan.setParent(tabs);

		/*
		 * PERINGATAN ZK 5 — JANGAN HAPUS tabpanel.setVisible(true) DI TIAP onClick
		 * ─────────────────────────────────────────────────────────────────────────
		 * Masalah: di ZK 5, tab.setSelected(true) yang dipanggil dari handler onClick
		 * pada komponen Tab TIDAK otomatis membuat Tabpanel-nya visible di sisi client.
		 * ZK hanya mengubah CSS class tab ("z-tab-seld") dan menyembunyikan panel lain,
		 * tetapi TIDAK mengirim perintah display:block ke panel milik tab yang diklik.
		 * Akibatnya konten panel sudah ter-load dan ada di DOM, tapi tetap display:none
		 * → seluruh area tab terlihat blank/putih.
		 *
		 * Fix wajib di setiap onClick:
		 *   tab.setSelected(true);      // sinkronisasi state server; sembunyikan panel lain
		 *   tabpanel.setVisible(true);  // perintah eksplisit ke client: tampilkan panel ini
		 *
		 * KEDUA baris ini harus ada bersama. Jika salah satu dihapus:
		 *   - tanpa setSelected  → panel lain tidak tersembunyi (tumpang tindih)
		 *   - tanpa setVisible   → panel tetap display:none meski konten sudah ada (blank)
		 * ─────────────────────────────────────────────────────────────────────────
		 */
		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelPendahuluan = new ais.ui.util.MyTabpanel();
		tabpanelPendahuluan.setParent(tabpanels);
		/* Tinggi Home mengikuti isi; tinggi raksasa lama (banyak x 12000px)
		 * hanya menyisakan area kosong panjang di bawah konten. */
		tabpanelPendahuluan.setStyle("height:auto;min-height:560px;overflow:visible;");
		displayHeader(perkuliahan, tabpanelPendahuluan);

		tabpanelAgenda.setParent(tabpanels);
		tabpanelAgenda.setStyle("height: " + (banyak * 12000) + "px;");
		tab.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				tampikanTab = true;
				tab.setSelected(true);           // sinkronisasi state; sembunyikan panel lain
				tabpanelAgenda.setVisible(true); // WAJIB: ZK5 tidak auto-show panel dari onClick
				if (tabpanelAgenda.getChildren().size() == 0) {
					tampilRinci(perkuliahan, dataLoader, tabpanelAgenda, groupbox, mulai, banyak, true);
				}
			}
		});

		if (tampikanTab) {
			if (tabpanelAgenda.getChildren().size() == 0) {
				// Konten Agenda ("Rencana dan Realisasi") KADANG tidak langsung keluar saat panel
				// disusun (mis. dibuka via jendela detail / e-Learning). DEFER render ke
				// Common.createDefaultTimer 1 detik agar konten dimuat SETELAH panel benar-benar
				// tampil di klien (mirip perbaikan panel "Perkuliahan & Kelas"). Tab tetap dipilih
				// & panel di-visible dulu; render di-guard idempoten (cek masih kosong).
				tab.setSelected(true);
				tabpanelAgenda.setVisible(true);
				Common.createDefaultTimer(new EventListener() {
					@Override
					public void onEvent(Event ev) throws Exception {
						try {
							if (tabpanelAgenda.getChildren().size() == 0) {
								tampilRinci(perkuliahan, dataLoader, tabpanelAgenda, groupbox, mulai, banyak);
							}
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}
				}, "", false, 1000);
			}
		}

		final Tabpanel tabpanelPengumuman = new ais.ui.util.MyTabpanel();

		tabpanelPengumuman.setParent(tabpanels);
		tabpanelPengumuman.setHeight("1250px");
		tabPengumuman.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				tabPengumuman.setSelected(true);           // sinkronisasi state; sembunyikan panel lain
				tabpanelPengumuman.setVisible(true);       // WAJIB: ZK5 tidak auto-show panel dari onClick
				if (tabpanelPengumuman.getChildren().size() == 0) {
					Common.clear(tabpanelPengumuman);
					MyInclude iframe = new MyInclude(
							"/pages/master/tampilan_pengumuman_perkuliahan.zul?perkuliahan=" + perkuliahan.getId());
					iframe.setParent(tabpanelPengumuman);
				}
			}
		});

		final Tabpanel tabpanelReferensi = new ais.ui.util.MyTabpanel();
		tabpanelReferensi.setHeight("1250px");
		tabpanelReferensi.setParent(tabpanels);
		tabReferensi.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				tabReferensi.setSelected(true);           // sinkronisasi state; sembunyikan panel lain
				tabpanelReferensi.setVisible(true);       // WAJIB: ZK5 tidak auto-show panel dari onClick
				if (tabpanelReferensi.getChildren().size() == 0) {

					final Tabbox tabbox = new Tabbox();
					tabbox.setSclass("ais-aktifitas-tabbox");
					tabbox.setParent(tabpanelReferensi);
					tabbox.setWidth("100%");
					tabbox.setHeight("100%");

					Tabs tabs = new Tabs();
					tabs.setParent(tabbox);

					final MyTabConfig tabReferensi = new MyTabConfig("Buku");
					tabReferensi.setParent(tabs);

					final MyTabConfig tabBukuAjar = new MyTabConfig("Bahan Ajar");
					tabBukuAjar.setParent(tabs);

					final MyTabConfig tabArtikel = new MyTabConfig("Artikel");
					tabArtikel.setParent(tabs);

					Tabpanels tabpanels = new Tabpanels();
					tabpanels.setParent(tabbox);

					Tabpanel tabpanelReferensi = new ais.ui.util.MyTabpanel();
					tabpanelReferensi.setHeight("1250px");
					tabpanelReferensi.setParent(tabpanels);
					PerkuliahanPunyaItemHelper perkuliahanPunyaItemHelper = new PerkuliahanPunyaItemHelper();
					perkuliahanPunyaItemHelper.display(perkuliahan, tabpanelReferensi);

					final Tabpanel tabpanelBukuAjar = new ais.ui.util.MyTabpanel();
					tabBukuAjar.setLabel("Buku Diktat / Ajar ");

					tabpanelBukuAjar.setParent(tabpanels);
					tabBukuAjar.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (tabpanelBukuAjar.getChildren().size() == 0) {
								tabpanelBukuAjar.setHeight("1250px");
								BukuBahanAjarHelper bukuBahanAjarHelper = new BukuBahanAjarHelper();
								bukuBahanAjarHelper.display(perkuliahan.getMatakuliah(), tabpanelBukuAjar, perkuliahan);
							}
						}
					});

					final Tabpanel tabpanelArtikel = new ais.ui.util.MyTabpanel();
					tabArtikel.setLabel("Artikel Ilmiah");

					tabpanelArtikel.setParent(tabpanels);
					tabpanelArtikel.setHeight("1250px");
					tabArtikel.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (tabpanelArtikel.getChildren().size() == 0) {

								DataPunyaArtikelHelper dataPunyaArtikelHelper = new DataPunyaArtikelHelper();
								dataPunyaArtikelHelper.display(null, null, null, null, null, perkuliahan, null,
										tabpanelArtikel);
							}
						}
					});

				}
			}
		});

		final Tabpanel tabpanelUjian = new ais.ui.util.MyTabpanel();
		tabpanelUjian.setParent(tabpanels);
		tabpanelUjian.setHeight(Common.isMobile() ? "" + (banyak * 12000) + "px" : "22050px");
		tabUjian.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				tabUjian.setSelected(true);           // sinkronisasi state; sembunyikan panel lain
				tabpanelUjian.setVisible(true);       // WAJIB: ZK5 tidak auto-show panel dari onClick
				if (tabpanelUjian.getChildren().isEmpty()) {
					RekapitulasiUjianHelper.display(tabpanelUjian, tbmuser, perkuliahan);
				}
			}
		});

		final Tabpanel tabpanelTugas = new ais.ui.util.MyTabpanel();
		tabpanelTugas.setParent(tabpanels);
		tabpanelTugas.setHeight(Common.isMobile() ? "" + (banyak * 12000) + "px" : "22050px");
		tabTgs.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				tabTgs.setSelected(true);           // sinkronisasi state; sembunyikan panel lain
				tabpanelTugas.setVisible(true);     // WAJIB: ZK5 tidak auto-show panel dari onClick
				if (tabpanelTugas.getChildren().isEmpty()) {
					RekapitulasiTugasHelper.display(tabpanelTugas, tbmuser, perkuliahan);
				}
			}
		});

		final Tabpanel tabpanelTugasKelompok = new ais.ui.util.MyTabpanel();
		tabpanelTugasKelompok.setParent(tabpanels);
		tabpanelTugasKelompok.setHeight(Common.isMobile() ? "" + (banyak * 12000) + "px" : "22050px");
		tabTugasKelompok.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				tabTugasKelompok.setSelected(true);           // sinkronisasi state; sembunyikan panel lain
				tabpanelTugasKelompok.setVisible(true);       // WAJIB: ZK5 tidak auto-show panel dari onClick
				if (tabpanelTugasKelompok.getChildren().size() == 0) {

					TugasKelompokHelper tugasKelompokHelper = new TugasKelompokHelper(mahasiswa, biodataCalonMahasiswa);
					tugasKelompokHelper.display(perkuliahan, null, null, tabpanelTugasKelompok);
				}
			}
		});

		final Tabpanel tabpanelPenilaian = new ais.ui.util.MyTabpanel();
		tabpanelPenilaian.setParent(tabpanels);
		tabpanelPenilaian.setHeight("18650px");
		tabPenilaian.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				tabPenilaian.setSelected(true);           // sinkronisasi state; sembunyikan panel lain
				tabpanelPenilaian.setVisible(true);       // WAJIB: ZK5 tidak auto-show panel dari onClick
				if (tabpanelPenilaian.getChildren().size() == 0) {

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null) {
								DetailperkuliahanForPenilaianHelper detailperkuliahanHelper = new DetailperkuliahanForPenilaianHelper(
										edit);

								Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();

								Boolean aktifPenilaian = Common.checkApakahDosenBolehMenilai(dosen, tbmuser,
										perkuliahan.getTahunAjaran(),
										perkuliahan.getStatusSemesterPendek() != null ? Perkuliahan.SP
												: perkuliahan.getGanjilGenap());
								
								if (Common.bolehKonfigurasi("hanya_dosen_yg_boleh_entry_nilai", Konfigurasi.TIDAK_AKTIF)) {
									if (tbmuser != null && tbmuser.ambilDosen() == null) {
										aktifPenilaian = false;
									}
								}


								System.out.println("aktifPenilaian = " + aktifPenilaian);

								detailperkuliahanHelper.display(perkuliahan, tabpanelPenilaian, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										// TODO Auto-generated method stub

									}
								}, null, aktifPenilaian);
							} else if (tbmuser != null && tbmuser.getMahasiswa() != null) {

								Long detailperkuliahanid = perkuliahan.ambilDetailperkuliahan(tbmuser.getMahasiswa());
								if (detailperkuliahanid != null) {
									Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
											.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());

									if (detailperkuliahan != null) {

										PenilaianMahasiswaHelper.tampilNilai(detailperkuliahan, tabpanelPenilaian);
									}
								}
							}
						}
					});
				}
			}
		});

		final Tabpanel tabpanelLaporan = new ais.ui.util.MyTabpanel();
		// Diperbesar 580px → 2000px agar konten sub-tab Laporan (Rencana/Jurnal/Kontrak/dst)
		// tidak terpotong/hilang (sebelumnya panel luar lebih pendek dari konten 650px).
		tabpanelLaporan.setHeight("2000px");
		tabpanelLaporan.setParent(tabpanels);
		tabLaporan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				tabLaporan.setSelected(true);           // sinkronisasi state; sembunyikan panel lain
				tabpanelLaporan.setVisible(true);       // WAJIB: ZK5 tidak auto-show panel dari onClick
				if (tabpanelLaporan.getChildren().size() == 0) {

					final Tabbox tabbox = new Tabbox();
					tabbox.setSclass("ais-aktifitas-tabbox");
					tabbox.setParent(tabpanelLaporan);
					tabbox.setWidth("100%");
					tabbox.setHeight("100%");

					Tabs tabs = new Tabs();
					tabs.setParent(tabbox);

					final MyTabConfig tabMonitor = new MyTabConfig("Rencana Perkuliahan");
					tabMonitor.setParent(tabs);

					final MyTabConfig tabJurnal = new MyTabConfig("Jurnal Mengajar");
					tabJurnal.setParent(tabs);

					final MyTabConfig tabKontrak = new MyTabConfig("Kontrak Perkuliahan");
					tabKontrak.setParent(tabs);

					List<Long> parales = perkuliahan.ambilParalel();

					final MyTabConfig tabMonitorParalel = new MyTabConfig("Rencana Paralel");
					tabMonitorParalel.setParent(tabs);
					tabMonitorParalel.setVisible(!parales.isEmpty());

					final MyTabConfig tabMonitorKbm = new MyTabConfig("Laporan KBM");
					tabMonitorKbm.setParent(tabs);

					final MyTabConfig tabRekapitulasTugasMandiri = new MyTabConfig("Tugas Individu");
					tabRekapitulasTugasMandiri.setParent(tabs);

					final MyTabConfig tabRekapitulasKehadiran = new MyTabConfig("Kehadiran");
					tabRekapitulasKehadiran.setParent(tabs);
					tabRekapitulasKehadiran.setVisible(bolehMelihatLaporanKelas());

					final MyTabConfig tabRekapitulasNilai = new MyTabConfig("Nilai");
					tabRekapitulasNilai.setParent(tabs);
					tabRekapitulasNilai.setVisible(bolehMelihatLaporanKelas());

					final MyTabConfig tabRekapitulasKehadiranNilai = new MyTabConfig("Kehadiran & Nilai");
					tabRekapitulasKehadiranNilai.setParent(tabs);
					tabRekapitulasKehadiranNilai.setVisible(bolehMelihatLaporanKelas());

					final MyTabConfig tabRekapitulasKetidakhadiran = new MyTabConfig("Ketidakhadiran");
					tabRekapitulasKetidakhadiran.setParent(tabs);
					tabRekapitulasKetidakhadiran.setVisible(bolehMelihatLaporanKelas());

					// Pola sama seperti tab luar: setiap onClick wajib setSelected(true)+setVisible(true)
					// agar panel tidak tetap display:none di ZK 5. Tab pertama ditangani di onClick-nya sendiri.

					Tabpanels tabpanels = new Tabpanels();
					tabpanels.setParent(tabbox);

					final Tabpanel tabpanelReferensi = new ais.ui.util.MyTabpanel();
					tabpanelReferensi.setHeight("2000px");
					tabpanelReferensi.setParent(tabpanels);

					// Muat laporan setelah tab benar-benar dipilih. Renderer PDF memakai timer dan
					// sebelumnya dijalankan ketika panel masih tersembunyi, sehingga ukuran area
					// pratinjau dapat terbaca 0 dan hasilnya tampak kosong.
					tabMonitor.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							if (tabMonitor.getParent() != null) {
								tabbox.setSelectedTab(tabMonitor);
							}
							tabpanelReferensi.setVisible(true);
							tabpanelReferensi.setWidth("100%");
							tabpanelReferensi.setHeight("2000px");
							if (tabpanelReferensi.getChildren().isEmpty()) {
								LaporanMonitorPerkuliahan laporanMonitorPerkuliahan = new LaporanMonitorPerkuliahan(
										perkuliahan);
								laporanMonitorPerkuliahan.setBorder("none");
								laporanMonitorPerkuliahan.setHeight("100%");
								laporanMonitorPerkuliahan.setWidth("100%");
								tabpanelReferensi.appendChild(laporanMonitorPerkuliahan);
							}
						}
					});
					final Tabpanel tabpanelJurnalParalel = new ais.ui.util.MyTabpanel();
					tabpanelJurnalParalel.setParent(tabpanels);
					tabJurnal.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (tabJurnal.getParent() != null) { tabJurnal.setSelected(true); }
							tabpanelJurnalParalel.setVisible(true);
							if (tabpanelJurnalParalel.getChildren().isEmpty()) {

								LaporanJurnalMengajar laporanJurnalMengajar = new LaporanJurnalMengajar(perkuliahan);
								laporanJurnalMengajar.setBorder("none");
								laporanJurnalMengajar.setHeight("2000px");
								laporanJurnalMengajar.setWidth("100%");
								tabpanelJurnalParalel.appendChild(laporanJurnalMengajar);
								tabpanelJurnalParalel.setHeight("2000px");

							}

						}
					});

					final Tabpanel tabpanelKontrak = new ais.ui.util.MyTabpanel();
					tabpanelKontrak.setParent(tabpanels);
					tabKontrak.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							tabKontrak.setSelected(true);
							tabpanelKontrak.setVisible(true);
							if (tabpanelKontrak.getChildren().isEmpty()) {

								LaporanKontrakPerkuliahan laporanKontrakPerkuliahan = new LaporanKontrakPerkuliahan(
										perkuliahan);
								laporanKontrakPerkuliahan.setBorder("none");
								laporanKontrakPerkuliahan.setHeight("2000px");
								laporanKontrakPerkuliahan.setWidth("100%");
								tabpanelKontrak.appendChild(laporanKontrakPerkuliahan);
								tabpanelKontrak.setHeight("2000px");

							}

						}
					});

					final Tabpanel tabpanelMonitorParalel = new ais.ui.util.MyTabpanel();
					tabpanelMonitorParalel.setParent(tabpanels);
					tabpanelMonitorParalel.setVisible(tabMonitorParalel.isVisible());
					tabMonitorParalel.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							tabMonitorParalel.setSelected(true);
							tabpanelMonitorParalel.setVisible(true);
							if (tabpanelMonitorParalel.getChildren().isEmpty()) {
								LaporanMonitorPerkuliahanParalel laporanJurnalPerkuliahan = new LaporanMonitorPerkuliahanParalel(
										perkuliahan);
								laporanJurnalPerkuliahan.setBorder("none");
								laporanJurnalPerkuliahan.setHeight("2000px");
								laporanJurnalPerkuliahan.setWidth("100%");
								tabpanelMonitorParalel.appendChild(laporanJurnalPerkuliahan);
								tabpanelMonitorParalel.setHeight("2000px");
							}

						}
					});

					final Tabpanel tabpanelMonitorKbm = new ais.ui.util.MyTabpanel();
					tabpanelMonitorKbm.setParent(tabpanels);
					tabMonitorKbm.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							tabbox.setSelectedTab(tabMonitorKbm);
							tabpanelMonitorKbm.setVisible(true);
							tabpanelMonitorKbm.setWidth("100%");
							tabpanelMonitorKbm.setHeight("2000px");
							if (tabpanelMonitorKbm.getChildren().isEmpty()) {
								LaporanMonitorPerkuliahanKbm laporanMonitorPerkuliahan = new LaporanMonitorPerkuliahanKbm(
										perkuliahan);
								laporanMonitorPerkuliahan.setBorder("none");
								laporanMonitorPerkuliahan.setHeight("100%");
								laporanMonitorPerkuliahan.setWidth("100%");
								tabpanelMonitorKbm.appendChild(laporanMonitorPerkuliahan);
								tabpanelMonitorKbm.setHeight("2000px");
							}

						}
					});

					final Tabpanel tabpanelTugasMandiri = new ais.ui.util.MyTabpanel();
					tabpanelTugasMandiri.setParent(tabpanels);
					tabRekapitulasTugasMandiri.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							tabRekapitulasTugasMandiri.setSelected(true);
							tabpanelTugasMandiri.setVisible(true);
							if (tabpanelTugasMandiri.getChildren().isEmpty()) {
								LaporanRekapitulasiTugasMandiri laporanRekapitulasiTugasMandiri = new LaporanRekapitulasiTugasMandiri(
										perkuliahan);
								laporanRekapitulasiTugasMandiri.setBorder("none");
								laporanRekapitulasiTugasMandiri.setHeight("2000px");
								laporanRekapitulasiTugasMandiri.setWidth("100%");
								tabpanelTugasMandiri.appendChild(laporanRekapitulasiTugasMandiri);
								tabpanelTugasMandiri.setHeight("2000px");
							}

						}
					});

					final Tabpanel tabpanelRekapitulasKehadiran = new ais.ui.util.MyTabpanel();
					tabpanelRekapitulasKehadiran.setParent(tabpanels);
					tabRekapitulasKehadiran.setVisible(bolehMelihatLaporanKelas());
					tabRekapitulasKehadiran.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (!bolehMelihatLaporanKelas()) {
								tampilkanPenolakanLaporanKelas();
								return;
							}
							tabbox.setSelectedTab(tabRekapitulasKehadiran);
							tabpanelRekapitulasKehadiran.setVisible(true);
							tabpanelRekapitulasKehadiran.setWidth("100%");
							tabpanelRekapitulasKehadiran.setHeight("2000px");
							if (tabpanelRekapitulasKehadiran.getChildren().isEmpty()) {

								Dosen kaprodi = null;
								Map<String, Object> parameters = ais.common.HashMapGenerator.getRandStringObject();

								kaprodi = perkuliahan == null || perkuliahan.getJurusan() == null ? null
										: perkuliahan.getJurusan().getKaprodi();

								parameters.put("fakultas_id", perkuliahan.getJurusan() == null ? -1L
										: perkuliahan.getJurusan().getFakultas().getId());
								parameters.put("jurusan_id",
										perkuliahan.getJurusan() == null ? -1L : perkuliahan.getJurusan().getId());

								parameters.put("perkuliahan", perkuliahan.getId());
								parameters.put("tampil_nilai", "1");
								parameters.put("kaprodi",
										kaprodi == null ? "(                                          )"
												: kaprodi.getNama());
								parameters.put("nip", kaprodi == null ? "" : kaprodi.getCode());
								parameters.put("tanggal", Common.dateFormat2.get().format(ais.ui.util.WaktuUtil.getDate()));

								parameters.put("nama_kaprodi",
										kaprodi == null ? "(                                          )"
												: kaprodi.getNama());
								parameters.put("nip_kaprodi",
										kaprodi == null || kaprodi.getCode() == null ? "" : kaprodi.getCode().trim());

								parameters.put("nidn_kaprodi",
										kaprodi == null || kaprodi.getNidn() == null ? "" : kaprodi.getNidn());

								parameters.put("id_kaprodi",
										kaprodi == null || kaprodi.getId() == null ? -1L : kaprodi.getId());

								System.out.println("parameters " + parameters);

								List<Map<String, Serializable>> maps = CommonReportHelper
										.generateParameterMapAbsensi(perkuliahan);
								parameters.put("maps", maps);

								Map<String, Long> parametersCover = new HashMap<String, Long>();
								parametersCover.put("perkuliahan", perkuliahan == null || perkuliahan.getId() == null ? -1 : perkuliahan.getId());

								if (perkuliahan != null) {
									if (perkuliahan.getJurusan() != null) {
										Common.insertProperty(Jurusan.class, perkuliahan.getJurusan(), parameters,
												"jur");
									}
									if (perkuliahan.getJurusan().getFakultas() != null) {
										Common.insertProperty(Fakultas.class, perkuliahan.getJurusan().getFakultas(),
												parameters, "fak");
									}
									if (perkuliahan.getJurusan().getFakultas().getPerguruanTinggi() != null) {
										Common.insertProperty(PerguruanTinggi.class,
												perkuliahan.getJurusan().getFakultas().getPerguruanTinggi(), parameters,
												"pt");
									}
								}

								String ttd = null;
								if (kaprodi != null) {
									LampiranLain lam = LampiranLain.ambil(kaprodi.getId(), LampiranLain.TTD_DOSEN);
									String nama = lam == null ? null : lam.getNama();

									if (nama != null) {
										if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
												|| nama.toLowerCase().endsWith(".jpeg")
												|| nama.toLowerCase().endsWith(".gif")
												|| nama.toLowerCase().endsWith(".tif")
												|| nama.toLowerCase().endsWith(".bmp")) {
											ttd = lam.ambilFile().getAbsolutePath();

											parameters.put("ttd_kaprodi", ttd);
										}
									}
								}
								System.out.println("ttd_kaprodi => " + ttd);

								if (perkuliahan != null) {
									int d = 1;
									for (Dosen dosena : perkuliahan.populateDosenBuNama()) {
										LampiranLain lam = LampiranLain.ambil(dosena.getId(), LampiranLain.TTD_DOSEN);
										String nama = lam == null ? null : lam.getNama();

										if (nama != null) {
											if (nama.toLowerCase().endsWith(".jpg")
													|| nama.toLowerCase().endsWith(".png")
													|| nama.toLowerCase().endsWith(".jpeg")
													|| nama.toLowerCase().endsWith(".gif")
													|| nama.toLowerCase().endsWith(".tif")
													|| nama.toLowerCase().endsWith(".bmp")) {
												ttd = lam.ambilFile().getAbsolutePath();
												parameters.put("ttd_dosen_" + d, ttd);
												System.out.println("ttd_dosen_" + d + " => " + ttd);
											}
										}
										d++;
									}

									if (kaprodi != null) {
										LampiranLain lam = LampiranLain.ambil(kaprodi.getId(), LampiranLain.TTD_DOSEN);
										String nama = lam == null ? null : lam.getNama();

										if (nama != null) {
											if (nama.toLowerCase().endsWith(".jpg")
													|| nama.toLowerCase().endsWith(".png")
													|| nama.toLowerCase().endsWith(".jpeg")
													|| nama.toLowerCase().endsWith(".gif")
													|| nama.toLowerCase().endsWith(".tif")
													|| nama.toLowerCase().endsWith(".bmp")) {
												ttd = lam.ambilFile().getAbsolutePath();

												parameters.put("ttd_dosen_" + d, ttd);
											}
										}
									}
								}

								Report.generatePDFReport(Report.PDF, parameters, "LaporanAbsensiLanscapeTotal",
										ais.ui.util.WaktuUtil.getDate(), maps, Common.locale,
										tabpanelRekapitulasKehadiran);

								tabpanelRekapitulasKehadiran.setHeight("2000px");
							}

						}
					});

					final Tabpanel tabpanelRekapitulasNilai = new ais.ui.util.MyTabpanel();
					tabpanelRekapitulasNilai.setParent(tabpanels);
					tabRekapitulasNilai.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (!bolehMelihatLaporanKelas()) {
								tampilkanPenolakanLaporanKelas();
								return;
							}
							tabbox.setSelectedTab(tabRekapitulasNilai);
							tabpanelRekapitulasNilai.setVisible(true);
							tabpanelRekapitulasNilai.setWidth("100%");
							tabpanelRekapitulasNilai.setHeight("2000px");
							if (tabpanelRekapitulasNilai.getChildren().isEmpty()) {

								DetailperkuliahanForPenilaianHelper.onLaporan(perkuliahan, tabpanelRekapitulasNilai);

								tabpanelRekapitulasNilai.setHeight("2000px");
							}

						}
					});

					final Tabpanel tabpanelRekapitulasKehadiranNilai = new ais.ui.util.MyTabpanel();
					tabpanelRekapitulasKehadiranNilai.setParent(tabpanels);
					tabRekapitulasKehadiranNilai.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (!bolehMelihatLaporanKelas()) {
								tampilkanPenolakanLaporanKelas();
								return;
							}
							tabRekapitulasKehadiranNilai.setSelected(true);
							tabpanelRekapitulasKehadiranNilai.setVisible(true);
							if (tabpanelRekapitulasKehadiranNilai.getChildren().isEmpty()) {
								DashboardDataNilaiMahasiswa laporan = new DashboardDataNilaiMahasiswa(perkuliahan);
								laporan.setHeight("100%");
								laporan.setWidth("100%");
								laporan.setParent(tabpanelRekapitulasKehadiranNilai);
								tabpanelRekapitulasKehadiranNilai.setHeight("2000px");
							}

						}
					});

					final Tabpanel tabpanelRekapitulasKetidakhadiran = new ais.ui.util.MyTabpanel();
					tabpanelRekapitulasKetidakhadiran.setParent(tabpanels);
					tabRekapitulasKetidakhadiran.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (!bolehMelihatLaporanKelas()) {
								tampilkanPenolakanLaporanKelas();
								return;
							}
							tabRekapitulasKetidakhadiran.setSelected(true);
							tabpanelRekapitulasKetidakhadiran.setVisible(true);
							if (tabpanelRekapitulasKetidakhadiran.getChildren().isEmpty()) {

								Dosen kaprodi = null;
								final Map<String, Object> parameters = ais.common.HashMapGenerator
										.getRandStringObject();

								kaprodi = perkuliahan == null || perkuliahan.getJurusan() == null ? null
										: perkuliahan.getJurusan().getKaprodi();

								parameters.put("fakultas_id", perkuliahan.getJurusan() == null ? -1L
										: perkuliahan.getJurusan().getFakultas().getId());
								parameters.put("jurusan_id",
										perkuliahan.getJurusan() == null ? -1L : perkuliahan.getJurusan().getId());

								parameters.put("perkuliahan", perkuliahan.getId());
								parameters.put("tampil_nilai", "1");
								parameters.put("kaprodi",
										kaprodi == null ? "(                                          )"
												: kaprodi.getNama());
								parameters.put("nip", kaprodi == null ? "" : kaprodi.getCode());
								parameters.put("tanggal", Common.dateFormat2.get().format(ais.ui.util.WaktuUtil.getDate()));

								parameters.put("nama_kaprodi",
										kaprodi == null ? "(                                          )"
												: kaprodi.getNama());
								parameters.put("nip_kaprodi",
										kaprodi == null || kaprodi.getCode() == null ? "" : kaprodi.getCode().trim());

								parameters.put("nidn_kaprodi",
										kaprodi == null || kaprodi.getNidn() == null ? "" : kaprodi.getNidn());

								parameters.put("id_kaprodi",
										kaprodi == null || kaprodi.getId() == null ? -1L : kaprodi.getId());

								System.out.println("parameters " + parameters);

								List<Map<String, Serializable>> maps = CommonReportHelper
										.generateParameterMapAbsensi(perkuliahan);
								parameters.put("maps", maps);

								final Map<String, Long> parametersCover = new HashMap<String, Long>();
								parametersCover.put("perkuliahan", perkuliahan == null || perkuliahan.getId() == null ? -1 : perkuliahan.getId());

								String ttd = null;
								if (kaprodi != null) {
									LampiranLain lam = LampiranLain.ambil(kaprodi.getId(), LampiranLain.TTD_DOSEN);
									String nama = lam == null ? null : lam.getNama();

									if (nama != null) {
										if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
												|| nama.toLowerCase().endsWith(".jpeg")
												|| nama.toLowerCase().endsWith(".gif")
												|| nama.toLowerCase().endsWith(".tif")
												|| nama.toLowerCase().endsWith(".bmp")) {
											ttd = lam.ambilFile().getAbsolutePath();

											parameters.put("ttd_kaprodi", ttd);
										}
									}
								}
								System.out.println("ttd_kaprodi => " + ttd);

								if (perkuliahan != null) {
									int d = 1;
									for (Dosen dosena : perkuliahan.populateDosenBuNama()) {
										LampiranLain lam = LampiranLain.ambil(dosena.getId(), LampiranLain.TTD_DOSEN);
										String nama = lam == null ? null : lam.getNama();

										if (nama != null) {
											if (nama.toLowerCase().endsWith(".jpg")
													|| nama.toLowerCase().endsWith(".png")
													|| nama.toLowerCase().endsWith(".jpeg")
													|| nama.toLowerCase().endsWith(".gif")
													|| nama.toLowerCase().endsWith(".tif")
													|| nama.toLowerCase().endsWith(".bmp")) {
												ttd = lam.ambilFile().getAbsolutePath();
												parameters.put("ttd_dosen_" + d, ttd);
												System.out.println("ttd_dosen_" + d + " => " + ttd);
											}
										}
										d++;
									}

									if (kaprodi != null) {
										LampiranLain lam = LampiranLain.ambil(kaprodi.getId(), LampiranLain.TTD_DOSEN);
										String nama = lam == null ? null : lam.getNama();

										if (nama != null) {
											if (nama.toLowerCase().endsWith(".jpg")
													|| nama.toLowerCase().endsWith(".png")
													|| nama.toLowerCase().endsWith(".jpeg")
													|| nama.toLowerCase().endsWith(".gif")
													|| nama.toLowerCase().endsWith(".tif")
													|| nama.toLowerCase().endsWith(".bmp")) {
												ttd = lam.ambilFile().getAbsolutePath();

												parameters.put("ttd_dosen_" + d, ttd);
											}
										}
									}
								}

								Report.generatePDFReport(Report.PDF, parameters, "LaporanRekapKetidakhadiran",
										ais.ui.util.WaktuUtil.getDate(), maps, Common.locale,
										tabpanelRekapitulasKetidakhadiran);

								tabpanelRekapitulasKetidakhadiran.setHeight("2000px");

							}

						}
					});

					// Saat menu "Lap." baru dibuka, ZK menandai tab pertama secara visual tetapi tidak
					// mengirim onClick. Akibatnya panel Rencana Perkuliahan aktif namun kosong. Jalankan
					// listener yang sama setelah seluruh pasangan Tab/Tabpanel selesai dibuat agar konten
					// pertama langsung terpasang dalam keadaan panel sudah terlihat.
					org.zkoss.zk.ui.event.Events.sendEvent(new Event("onClick", tabMonitor));

				}
			}
		});

	}

	/** Varian singkat {@link #tampilRinci(Perkuliahan, DataLoader, Tabpanel, Component, int, int, boolean)} tanpa flag {@code tampilHal} (default false). */
	@SuppressWarnings({})
	private void tampilRinci(final Perkuliahan perkuliahan, final DataLoader dataLoader, final Tabpanel tabpanel,
			final Component groupbox, final int mulai, final int banyak) throws Exception {
		tampilRinci(perkuliahan, dataLoader, tabpanel, groupbox, mulai, banyak, false);
	}

	/**
	 * Mengisi ulang tab "Agenda" (sub-tabbox: Rencana dan Realisasi, Kehadiran, Tugas/Ujian/Materi,
	 * RPS/Form Rencana Pembelajaran, dan — bila kurikulum OBE aktif — Nilai OBE &amp; Rekap Nilai OBE)
	 * untuk satu {@link Perkuliahan}. Mengambil daftar id {@link Pertemuan} aktif via
	 * {@code perkuliahan.ambilPertemuan(m, banyak, tampilHal)} (memuat ulang dari DB terlebih dahulu
	 * bila {@code perkuliahan.udah()} bernilai false, termasuk reinit diskusi tiap pertemuan). Untuk
	 * tab "Rencana dan Realisasi", tiap {@link Pertemuan} dirender sebagai kartu berisi info tanggal
	 * rencana/realisasi, riwayat revisi, topik, dosen tamu, catatan, tombol video conference/absen/
	 * toolbar aksi ({@link #createKeterangan}), status kehadiran, dan (bila konfigurasi mengizinkan)
	 * komentar/diskusi. Bila belum ada pertemuan sama sekali, menampilkan pesan "belum dibuat" beserta
	 * tombol "Buat Pertemuan"/"Ambil" (copy dari perkuliahan lain). Paging tab Agenda di-clamp agar
	 * halaman aktif tidak pernah melebihi total halaman (mencegah {@code WrongValueException} saat
	 * jumlah pertemuan berkurang).
	 *
	 * @param perkuliahan konteks perkuliahan.
	 * @param dataLoader  callback reload dipanggil ulang oleh listener tombol di dalam kartu pertemuan.
	 * @param tabpanel    tabpanel Agenda yang dibersihkan lalu diisi ulang.
	 * @param groupbox    parent luar (dipakai untuk memanggil {@link #initDetail} ulang dari beberapa listener).
	 * @param m           indeks pertemuan awal (0-based) untuk halaman ini.
	 * @param banyak      jumlah pertemuan per halaman.
	 * @param tampilHal   diteruskan ke {@code perkuliahan.ambilPertemuan} untuk menentukan strategi pengambilan halaman.
	 * @throws Exception diteruskan dari operasi ZK/Hibernate di dalamnya.
	 */
	@SuppressWarnings({ "unchecked" })
	private void tampilRinci(final Perkuliahan perkuliahan, final DataLoader dataLoader, final Tabpanel tabpanel,
			final Component groupbox, final int m, final int banyak, boolean tampilHal) throws Exception {

		final List<Long> pertemuans;
		Integer jumlahParentNull = perkuliahan.getJumlahMaksimalPertemuan();

		if (perkuliahan.udah()) {
			Object[] a = perkuliahan.ambilPertemuan(m, banyak, tampilHal);
			pertemuans = (List<Long>) a[0];
			jumlahParentNull = (Integer) a[1];
			mulai = (Integer) a[2];
		} else {
			Session session = HibernateUtil.currentSession();
			List<Pertemuan> pertemuansTemp = session.createCriteria(Pertemuan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.addOrder(!perkuliahan.getUrutkanotomatis() ? Order.asc("pertemuanKe") : Order.asc("tanggal"))
					.add(Restrictions.isNotNull("tanggal")).addOrder(Order.asc("id"))
					.add(Restrictions.eq("perkuliahan", perkuliahan)).list();
			perkuliahan.reInitPertemuan(pertemuansTemp, session);
			try {
				for (Pertemuan pertemuan : pertemuansTemp) {
					pertemuan.reInitPertemuanPunyaDiskusi(session);
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AktifitasPerkuliahanHelper.java:2366");
			}

			pertemuansTemp.clear();
			pertemuansTemp = null;

			Object[] a = perkuliahan.ambilPertemuan(m, banyak, tampilHal);
			pertemuans = (List<Long>) a[0];
			jumlahParentNull = (Integer) a[1];
			mulai = (Integer) a[2];
		}

		Common.clear(tabpanel);

		final Tabbox tabbox = new Tabbox();
		tabbox.setSclass("ais-aktifitas-tabbox");
		tabbox.setParent(tabpanel);
		tabbox.setWidth("100%");
		tabbox.setHeight("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab = new MyTabConfig("Rencana dan Realisasi");
		tab.setParent(tabs);

		MyTabConfig tab1 = new MyTabConfig("Kehadiran");
		tab1.setParent(tabs);

		MyTabConfig tabTgs = new MyTabConfig("Tugas, Ujian, Materi");
		tabTgs.setParent(tabs);

		MyTabConfig tabSilabus = new MyTabConfig((perkuliahan != null && perkuliahan.getKurikulum() != null
				&& perkuliahan.getKurikulum().apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap()))
						? "RPS OBE"
						: "Form Rencana Pembelajaran");
		tabSilabus.setParent(tabs);

		MyTabConfig tabNilaiObe = new MyTabConfig("Nilai OBE");
		if (perkuliahan != null && perkuliahan.getKurikulum() != null
				&& perkuliahan.getKurikulum().apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap())) {
			tabNilaiObe.setParent(tabs);
		}

		MyTabConfig tabRekapNilai = new MyTabConfig("Rekap Nilai OBE");
		if (perkuliahan != null && perkuliahan.getKurikulum() != null
				&& perkuliahan.getKurikulum().apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap())
				&& tbmuser != null && tbmuser.getMahasiswa() == null
				&& perkuliahan.getKurikulumPunyaMatakuliah() != null) {
			tabRekapNilai.setParent(tabs);
		}

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		// FIX konten sub-tab AGENDA KOSONG (Kehadiran / Tugas,Ujian,Materi / RPS OBE / Nilai OBE /
		// Rekap Nilai OBE): di ZK 5.5 klik tab hanya memicu onSelect Tabbox, sedangkan mount konten
		// dipasang pada onClick MASING-MASING tab -> kadang tak ter-trigger sehingga panel kosong.
		// Pasang onSelect di Tabbox yang me-RE-DISPATCH onClick ke tab terpilih (idempoten krn tiap
		// handler cek getChildren().isEmpty()). Tab pertama (Rencana dan Realisasi) DIKECUALIKAN
		// karena panelnya dibangun eager tanpa onClick. Pola sama dgn tab "Lap." (~1908).
		final org.zkoss.zul.Tab tabPertamaAgenda = tab;
		tabbox.addEventListener(org.zkoss.zk.ui.event.Events.ON_SELECT, new EventListener() {
			@Override
			public void onEvent(Event evSel) throws Exception {
				org.zkoss.zul.Tab terpilih = tabbox.getSelectedTab();
				if (terpilih != null && terpilih != tabPertamaAgenda) {
					org.zkoss.zk.ui.event.Events.sendEvent(new Event("onClick", terpilih));
				}
			}
		});

		Tabpanel tabpanelPendahuluan = new ais.ui.util.MyTabpanel();
		tabpanelPendahuluan.setParent(tabpanels);

		final Tabpanel tabpanelRekapitulasKehadiranNilai = new ais.ui.util.MyTabpanel();
		tabpanelRekapitulasKehadiranNilai.setParent(tabpanels);
		tab1.addEventListener("onClick", new EventListener() {

			protected DetailpertemuanHelper detailpertemuanHelper = new DetailpertemuanHelper();

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelRekapitulasKehadiranNilai.getChildren().isEmpty()) {
					detailpertemuanHelper.displayDetailPertemuan(perkuliahan, tabpanelRekapitulasKehadiranNilai);
					tabpanelRekapitulasKehadiranNilai.setHeight("3050px");
				}

			}
		});

		final Tabpanel tabpanelTgs = new ais.ui.util.MyTabpanel();
		tabpanelTgs.setParent(tabpanels);
		tabpanelTgs.setHeight("13050px");
		tabTgs.addEventListener("onClick", new EventListener() {

			private boolean materiBol = true;
			private boolean ujianBol = true;
			private boolean tugasBol = true;

			void reload() throws Exception {

				try {
					Common.clear(tabpanelTgs);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AktifitasPerkuliahanHelper.java:2470");
					// TODO: handle exception
				}
				onEvent(null);
			}

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelTgs.getChildren().size() == 0) {

					final MyCheckboxConfig materiPil = new MyCheckboxConfig("Materi");
					final MyCheckboxConfig ujianPil = new MyCheckboxConfig("Ujian");
					final MyCheckboxConfig tugasPil = new MyCheckboxConfig("Tugas");
					final Textbox cari = new Textbox();
					EventListener eventListenerReload = new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							materiBol = materiPil.isChecked();
							ujianBol = ujianPil.isChecked();
							tugasBol = tugasPil.isChecked();
							reload();
						}
					};

					org.zkoss.zul.Vbox outerVbox = new org.zkoss.zul.Vbox();
					outerVbox.setWidth("100%");
					outerVbox.setParent(tabpanelTgs);

					boolean mobile = Common.isMobile();

					Box vbox = mobile ? new Vbox() : new Hbox();
					vbox.setParent(outerVbox);

					Hbox hbox = new Hbox();
					hbox.setParent(vbox);

					hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Cari :")));
					hbox.appendChild(cari);
					cari.setCols(15);

					final MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Refresh",
							"/img/Button-Refresh-icon.png");
					hbox.appendChild(button);

					hbox = new Hbox();
					hbox.setParent(vbox);
					hbox.appendChild(materiPil);
					hbox.appendChild(ujianPil);
					hbox.appendChild(tugasPil);

					org.zkoss.zul.Div center = new org.zkoss.zul.Div();
					center.setWidth("100%");
					center.setParent(outerVbox);

					materiPil.addEventListener("onClick", eventListenerReload);
					ujianPil.addEventListener("onClick", eventListenerReload);
					tugasPil.addEventListener("onClick", eventListenerReload);

					materiPil.setChecked(materiBol);
					ujianPil.setChecked(ujianBol);
					tugasPil.setChecked(tugasBol);

					final Rows rows = new Rows();
					final Paging paging = new Paging();

					Grid grid = new Grid();
					grid.setSclass("dgrid");
					grid.setParent(center);
					grid.appendChild(rows);

					Columns columns = new Columns();
					columns.setParent(grid);

					MyColumnConfig column = new MyColumnConfig();
					column.setParent(columns);

					column = new MyColumnConfig();
					column.setParent(columns);
					column.setWidth(Common.isMobile() ? "40%" : "20%");

					EventListener eventListener = new EventListener() {

						@SuppressWarnings("deprecation")
						@Override
						public void onEvent(Event arg0) throws Exception {

							Common.clear(rows);
							MyFormRow row = new MyFormRow();
							row.setValign("top");
							row.setStyle("border:0px;background: transparent;font-size: x-small;");
							row.setParent(rows);
							ais.ui.util.ZkCompat.setSpans(row, "2");

							Vbox vbox = new Vbox();
							vbox.setParent(row);
							vbox.setWidth("100%");
							final Label label;
							final boolean refresh = arg0 != null && arg0.getTarget() == button;
							vbox.appendChild(label = new Label(ais.common.Common.getBahasaConfig("Ambil data ...")));
							Image img;
							vbox.appendChild(img = new Image("/loading_icon.gif"));
							img.setWidth("90%");

							Common.createDefaultTimerNoBusy(new EventListener() {

								@Override
								public void onEvent(Event a) throws Exception {
									Tbmuser tbmuser = Common.getCurrentUser();
									Common.clear(rows);
									TampilanELearningAction.loadDataMateri(cari, rows, paging, refresh, materiPil,
											ujianPil, tugasPil, label, tbmuser, perkuliahan.ambilPertemuan(refresh),
											true, false);
								}
							});

						}
					};

					Common.createDefaultTimer(eventListener);
					button.addEventListener("onClick", eventListener);
					cari.addEventListener("onOK", eventListener);

				}
			}
		});

		final Tabpanel tabpanelSilabus = new ais.ui.util.MyTabpanel();
		tabpanelSilabus.setParent(tabpanels);
		// JANGAN bungkus MyWindow(height:100%)+MyInclude(height:100%) — di dalam tabbox,
		// rantai height:100% TAK resolve → konten kolaps 0 → tab tampak KOSONG. Pakai pola
		// yang TERBUKTI jalan (dasbor OBE): MyInclude LANGSUNG ke tabpanel + tinggi EKSPLISIT
		// (12000px) + tabpanel overflow:auto agar bisa di-scroll.
		tabpanelSilabus.setStyle("overflow:auto;");
		tabSilabus.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelSilabus.getChildren().isEmpty()) {

					if ((perkuliahan != null && perkuliahan.getKurikulum().apakahObe(perkuliahan.getTahunAjaran(),
							perkuliahan.getGanjilGenap()))) {

						KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = perkuliahan.ambilKurikulumPunyaMatakuliah();

						if (kurikulumPunyaMatakuliah != null) {
							MyInclude iframe = new MyInclude("/pages/master/rps_obe.zul?kur="
									+ kurikulumPunyaMatakuliah.getId() + "&perkuliahan=" + perkuliahan.getId());
							iframe.setHeight("12000px");
							iframe.setParent(tabpanelSilabus);
						} else {
							new MyLabelBoldMerah("Kurikulum belum diisi secara benar").setParent(tabpanelSilabus);
						}

					} else {
						tabpanelSilabus.appendChild(
								new ais.ui.util.MyHtml(PerkuliahanAction.generateiIntroductoryText(perkuliahan)));
					}

				}
			}
		});

		if (perkuliahan != null && perkuliahan.getKurikulum() != null && perkuliahan.getKurikulum().apakahObe(
				perkuliahan.getTahunAjaran(),
				perkuliahan.getGanjilGenap())) {
			final Tabpanel tabpanelNilaiObe = new ais.ui.util.MyTabpanel();
			tabpanelNilaiObe.setParent(tabpanels);
			tabpanelNilaiObe.setStyle("overflow:auto;");
			tabNilaiObe.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanelNilaiObe.getChildren().isEmpty()) {

						// MyInclude langsung + tinggi eksplisit (hindari height:100% yang kolaps).
						MyInclude iframe = new MyInclude(
								"/pages/master/nilai_obe.zul?perkuliahan=" + perkuliahan.getId());
						iframe.setHeight("12000px");
						iframe.setParent(tabpanelNilaiObe);

					}
				}
			});

		}

		if (perkuliahan != null && perkuliahan.getKurikulum() != null && perkuliahan.getKurikulum().apakahObe(
				perkuliahan.getTahunAjaran(),
				perkuliahan.getGanjilGenap())
				&& tbmuser != null && tbmuser.getMahasiswa() == null
				&& perkuliahan.getKurikulumPunyaMatakuliah() != null) {
			final Tabpanel tabpanelRekapNilai = new ais.ui.util.MyTabpanel();
			tabpanelRekapNilai.setParent(tabpanels);
			tabpanelRekapNilai.setStyle("overflow:auto;");
			tabRekapNilai.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanelRekapNilai.getChildren().isEmpty()) {

						RekapHasilTugasPerTugasDanUjianObe addWindow = new RekapHasilTugasPerTugasDanUjianObe(true,
								perkuliahan);
						addWindow.setClosable(false);
						addWindow.setWidth("100%");
						// Tinggi EKSPLISIT (bukan "100%;" yang kolaps + malformed) — pola dasbor OBE.
						addWindow.setHeight("13050px");
						tabpanelRekapNilai.appendChild(addWindow);

					}
				}
			});
		}

		final ais.ui.util.MyDiv myGroupbox = new ais.ui.util.MyDiv();
		myGroupbox.setStyle("height: " + (banyak * 12000) + "px;");
		myGroupbox.setParent(tabpanelPendahuluan);
		myGroupbox.appendChild(initAgendaPerkuliahan(perkuliahan, new DataLoader() {

			@Override
			public void loadData(Object value) {
				try {
					tampilRinci(perkuliahan, dataLoader, tabpanel, myGroupbox, mulai, banyak);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

			}
		}));

		if (jumlahParentNull > banyak) {
			final Paging paging = new Paging();
			paging.setDetailed(!Common.isMobile());
			try {
				paging.setPageSize(banyak);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AktifitasPerkuliahanHelper.java:2706");
			}
			paging.setMold("os");
			paging.setTotalSize(jumlahParentNull);
			/* Clamp: bila data berkurang (pertemuan dihapus/filter berubah),
			 * offset lama bisa menunjuk halaman melebihi total ->
			 * WrongValueException "Unable to set active page to N since only M". */
			int halamanAktif = banyak <= 0 ? 0 : mulai / banyak;
			int totalHalaman = banyak <= 0 ? 1 : (int) Math.ceil(jumlahParentNull / (double) banyak);
			if (totalHalaman < 1) {
				totalHalaman = 1;
			}
			if (halamanAktif >= totalHalaman) {
				halamanAktif = totalHalaman - 1;
			}
			paging.setActivePage(halamanAktif < 0 ? 0 : halamanAktif);
			paging.addEventListener("onPaging", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					tampilRinci(perkuliahan, dataLoader, tabpanel, myGroupbox, banyak * paging.getActivePage(), banyak,
							false);

				}
			});

			if (banyak == 1) {
				myGroupbox.appendChild(new MyLabelBoldConfig("Pilih pertemuan ke : "));
			}
			paging.setParent(myGroupbox);
		}

		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
				&& tbmuser.getBiodataCalonMahasiswa() == null && pertemuans.isEmpty() && perkuliahan != null
				&& perkuliahan.getKurikulum() != null && !perkuliahan.getKurikulum().apakahObe(
						perkuliahan.getTahunAjaran(),
						perkuliahan.getGanjilGenap())) {
			Html html = new ais.ui.util.MyHtml(
					"<strong><font style='color:red'>Agenda perkuliahan belum dibuat</font></strong><br><br><br>");
			html.setHeight("150px");
			html.setWidth("100%");
			html.setParent(myGroupbox);

			if (tbmuser.getMahasiswa() == null && (tbmuser.ambilDosen() == null
					|| (perkuliahan != null && perkuliahan.getDosenBisaMerubahTanggalPerkuliahan()))) {

				myGroupbox.appendChild(new Space());
				myGroupbox.appendChild(new Space());
				myGroupbox.appendChild(new Space());
				myGroupbox.appendChild(new Space());
				myGroupbox.appendChild(new Space());

				Hbox hbox = new Hbox();
				hbox.setParent(myGroupbox);

				PenjadwalanHelper.tampilTombolBuatPertemuan(hbox, perkuliahan, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								perkuliahan.belum();
								tampikanTab = true;
								initDetail(perkuliahan, groupbox, mulai, banyak);

							}
						});
					}
				});

				PenjadwalanHelper.tampilTombolAmbil(hbox, perkuliahan, null, null, null, null, null, null,
						new DataLoader() {

							@Override
							public void loadData(Object value) {
								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										perkuliahan.belum();
										tampikanTab = true;
										initDetail(perkuliahan, groupbox, mulai, banyak);

									}
								});
							}
						});
			}

		} else if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
				&& pertemuans.isEmpty() && perkuliahan != null && perkuliahan.getKurikulum() != null
				&& perkuliahan.getKurikulum().apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap())) {

			// Kurikulum OBE & agenda/rincian belum dibuat: tampilkan pesan + tombol Ambil (copy) dan
			// Tambah Rincian OBE (sebelumnya tombol copy TIDAK muncul untuk OBE).
			Html html = new ais.ui.util.MyHtml(
					"<strong><font style='color:red'>Rincian OBE / agenda perkuliahan belum dibuat</font></strong><br><br><br>");
			html.setHeight("150px");
			html.setWidth("100%");
			html.setParent(myGroupbox);

			if (tbmuser.getMahasiswa() == null && (tbmuser.ambilDosen() == null
					|| (perkuliahan != null && perkuliahan.getDosenBisaMerubahTanggalPerkuliahan()))) {

				myGroupbox.appendChild(new Space());
				myGroupbox.appendChild(new Space());
				myGroupbox.appendChild(new Space());
				myGroupbox.appendChild(new Space());
				myGroupbox.appendChild(new Space());

				Hbox hbox = new Hbox();
				hbox.setParent(myGroupbox);

				hbox.appendChild(PenjadwalanHelper.buatSatuPertemuan(perkuliahan, tbmuser, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								perkuliahan.belum();
								tampikanTab = true;
								initDetail(perkuliahan, groupbox, mulai, banyak);
							}
						});
					}
				}));

				PenjadwalanHelper.tampilTombolAmbil(hbox, perkuliahan, null, null, null, null, null, null,
						new DataLoader() {

							@Override
							public void loadData(Object value) {
								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										perkuliahan.belum();
										tampikanTab = true;
										initDetail(perkuliahan, groupbox, mulai, banyak);
									}
								});
							}
						});
			}

		} else {
			Grid grid = new Grid();
			grid.setSclass("dgrid");
			grid.setSclass("fgrid");
			grid.setWidth("100%");
			grid.setParent(myGroupbox);
			grid.setWidth("100%");

			Rows rows = new Rows();
			rows.setParent(grid);

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
			Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
			calendar1.set(Calendar.DATE, calendar1.get(Calendar.DATE) + 6);

			boolean urut = false;
			try {
				String pil = tbmuser.retreive("urutkan_diskusi_berdasarkan_terlama");
				urut = (pil == null || pil.trim().isEmpty() ? false : Boolean.parseBoolean(pil));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AktifitasPerkuliahanHelper.java:2818");
				// TODO: handle exception
			}
			boolean mobile = Common.isMobile();
			for (Long pertemuanid : pertemuans) {
				if (pertemuanid == null) {
					continue;
				}
				Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
				if (pertemuan != null) {

					if (banyak == 1 && perkuliahan.getKurikulum() != null && perkuliahan.getKurikulum().apakahObe(
							perkuliahan.getTahunAjaran(),
							perkuliahan.getGanjilGenap())
							&& !pertemuan.getPertemuanKe().equals(m + 1)) {
						Session session = HibernateUtil.currentSession();
						try {
							session.refresh(pertemuan);
						} catch (org.hibernate.UnresolvableObjectException missing) {
							// Pertemuan telah dihapus oleh proses lain setelah daftar dibaca.
							// Lewati baris stale agar panel lain tetap dapat ditampilkan.
							continue;
						}
						pertemuan.setPertemuanKe(m + 1);
						pertemuan.setPertemuanManual(m + 1);
						Common.refreshUpdate(session, pertemuan);
					}

					final MyFormRow rowUtama = new MyFormRow();
					rowUtama.setParent(rows);
					rowUtama.setValign("top");

					String tgl = pertemuan.getPerkuliahan() == null || pertemuan.getTanggal() == null ? "-"
							: ("Rencana : " + Common.dateFormat4.get().format(pertemuan.getTanggal()) + " "
									+ (pertemuan.getWaktuMulai() == null && pertemuan.getWaktuSelesai() == null ? ""
											: pertemuan.getWaktuMulai() + "-" + pertemuan.getWaktuSelesai()));

					Groupbox pertemuanBox = new ais.ui.util.MyGroupboxStyled();
					pertemuanBox.setWidth(mobile ? "93%" : "95%");
					rowUtama.appendChild(pertemuanBox);
					MyCaptionStyled c;
					pertemuanBox.appendChild(
							c = new MyCaptionStyled("Pertemuan ke-" + pertemuan.getPertemuanKe() + ", " + tgl));
					c.setStyle("font-size:12px;font-weight: bolder;text-decoration: none;color:"
							+ pertemuan.warna().split(",")[0] + ";border: 1px solid " + pertemuan.warna().split(",")[0]
							+ ";\r\n" + "  padding: 5px;" + "  background-color: rgba(169,169,169,0.4);"
							+ "  border-radius: 5px 15px;");

					Vbox a = RevisiHelper.createNewRevisi(Pertemuan.class, pertemuan,
							pertemuan.getStatusPertemuan() == null ? "" : pertemuan.getStatusPertemuan().getNama());

					a.appendChild(new Label(pertemuan.getTanggalRealisasi() == null ? ""
							: "Realisasi : " + Common.dateFormat4.get().format(pertemuan.getTanggalRealisasi())));

					final Vbox vbox = new Vbox();
					vbox.setParent(pertemuanBox);

					a.setParent(vbox);

					new MyLabelAgakKecilBold(pertemuan.getTopik()).setParent(vbox);
//					new MyLabelAgakKecilBold(pertemuan.getMetodePembelajaran()).setParent(vbox);
//					new MyLabelAgakKecilBold(pertemuan.getBukuRujukan1()).setParent(vbox);
//					new MyLabelAgakKecilBold(pertemuan.getBukuRujukan2()).setParent(vbox);
					new MyLabelAgakKecilBold(pertemuan.getDosenTamu()).setParent(vbox);
					new MyLabelAgakKecilBold(pertemuan.getDosenTamu2()).setParent(vbox);

					DashboardTimelinePertemuan.displayCatatan(vbox, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							try {
								tampilRinci(perkuliahan, dataLoader, tabpanel, groupbox, mulai, banyak, false);
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AktifitasPerkuliahanHelper.java:2882");
							}
						}
					}, pertemuan, tbmuser, mobile);

					Component aa = DashboardTimelinePertemuan.createVideoConrefrence(pertemuan, null, false,
							new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									try {
										tampilRinci(perkuliahan, dataLoader, tabpanel, groupbox, mulai, banyak, false);
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AktifitasPerkuliahanHelper.java:2895");
									}
								}
							});

					Component bb = AbsensiHelper.createTombolAbsen(pertemuan, true, new DataLoader() {

						@Override
						public void loadData(Object value) {
							try {
								tampilRinci(perkuliahan, this, tabpanel, groupbox, mulai, banyak, false);
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AktifitasPerkuliahanHelper.java:2907");
							}
						}
					});

					AktifitasPerkuliahanHelper.createKeterangan(pertemuan, new DataLoader() {

						@Override
						public void loadData(Object value) {
							try {
								tampilRinci(perkuliahan, this, tabpanel, groupbox, mulai, banyak, false);
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AktifitasPerkuliahanHelper.java:2919");
							}
						}
					}, aa, bb, DashboardTimelinePertemuan.createScanFoto(tbmuser, pertemuan)).setParent(pertemuanBox);

					AbsensiHelper.createStatusKehadiran(perkuliahan.populateDosen().values(), pertemuan)
							.setParent(pertemuanBox);

					DashboardTimelinePertemuan.tampilOnline(pertemuan, pertemuanBox, tbmuser, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							tampilRinci(perkuliahan, dataLoader, tabpanel, groupbox, mulai, banyak, false);
						}
					});

					if (Common.bolehKonfigurasi("tampilkan_komentar_di_aktifitas_perkuliahan")) {
						if (!pertemuan.udah()) {
							Session session = HibernateUtil.currentSession();
							pertemuan.reInitPertemuanPunyaDiskusi(session);
						}

						Vbox vbox2 = new Vbox();
						vbox2.setParent(pertemuanBox);

						TreeSet<Long> pertemuanPunyaDiskusisa = pertemuan.ambilPertemuanPunyaDiskusiTotal(urut);
						DashboardTimelinePertemuan.loadKomentarDetail(null, "42px", pertemuanPunyaDiskusisa, pertemuan,
								vbox2, "background-color: rgba(255,255,255,0.5);", 0, 50, false, null);
					}

				}
			}
		}

		tab.focus();
	}

	/**
	 * Varian singkat {@link #createKeteranganData} yang mengambil {@link Tbmuser} dan
	 * {@link Mahasiswa} dari user yang sedang login ({@code Common.getCurrentUser()}).
	 *
	 * @param pertemuan  pertemuan yang toolbar aksinya dibangun.
	 * @param dataLoader callback reload dipanggil dari tombol-tombol navigasi tab.
	 * @param buttons    komponen tombol tambahan yang disisipkan ke akhir toolbar.
	 * @return Vbox berisi baris tombol aksi (Dasbor/Catatan/Ujian/dll.), siap ditempel ke parent.
	 */
	public static Vbox createKeterangan(final Pertemuan pertemuan, final DataLoader dataLoader, Component... buttons) {
		Tbmuser tbmuser = Common.getCurrentUser();
		return createKeteranganData(pertemuan, tbmuser, tbmuser == null ? null : tbmuser.getMahasiswa(), null,
				dataLoader, buttons);

	}

	/**
	 * Varian {@link #createKeteranganData} untuk konteks mahasiswa/calon mahasiswa eksplisit, dengan
	 * {@link Tbmuser} diambil dari user yang sedang login.
	 *
	 * @param pertemuan              pertemuan yang toolbar aksinya dibangun.
	 * @param mahasiswa              konteks mahasiswa eksplisit (menentukan tombol mana yang tampil).
	 * @param biodataCalonMahasiswa  konteks calon mahasiswa eksplisit.
	 * @param dataLoader             callback reload.
	 * @param buttons                tombol tambahan.
	 * @return Vbox toolbar aksi pertemuan.
	 */
	public static Vbox createKeterangan(Pertemuan pertemuan, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, DataLoader dataLoader, Component... buttons) {
		Tbmuser tbmuser = Common.getCurrentUser();
		return createKeteranganData(pertemuan, tbmuser, mahasiswa, biodataCalonMahasiswa, dataLoader, buttons);
	}

	/** Varian {@link #createKeteranganData} dengan {@code vertical=false}, {@code simple=false}, tanpa {@code btnTabNav}. */
	public static Vbox createKeteranganData(Pertemuan pertemuan, Tbmuser tbmuser, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, DataLoader dataLoader, Component... buttons) {
		return createKeteranganData(pertemuan, tbmuser, mahasiswa, biodataCalonMahasiswa, dataLoader, false, false,
				null, buttons);
	}

	/** Varian {@link #createKeteranganData} tanpa {@code btnTabNav} eksplisit (navigasi tab jatuh ke {@code PertemuanHelper.display}). */
	public static Vbox createKeteranganData(final Pertemuan pertemuan, final Tbmuser tbmuser, final Mahasiswa mahasiswa,
			final BiodataCalonMahasiswa biodataCalonMahasiswa, final DataLoader dataLoader, final boolean vertical,
			final boolean simple, final Component... buttons) {
		return createKeteranganData(pertemuan, tbmuser, mahasiswa, biodataCalonMahasiswa, dataLoader, vertical, simple,
				null, buttons);
	}

	/**
	 * Membangun toolbar tombol aksi cepat untuk satu {@link Pertemuan}: Dasbor (ringkasan), Catatan
	 * (disorot merah bila {@code pertemuan.getCatatan()} terisi), Ujian (badge jumlah ujian &amp;
	 * total peserta dari {@code ambilPertemuanPunyaUjianTotal}), Diskusi, File, Tugas (individu &amp;
	 * kelompok, memicu peringatan bila judul tugas belum diisi tapi sudah ada data tugas), dan
	 * Evaluasi/kuesioner — masing-masing badge dihitung dari query/agregasi milik {@link Pertemuan}
	 * dan disembunyikan sesuai peran (mahasiswa/siswa/dosen/admin) serta status {@code getWisuda()}.
	 * Setiap tombol yang punya {@code dataLoader} akan membuka tab terkait: lewat
	 * {@code btnTabNav.pilih(index)} bila tersedia (navigasi tab tanpa reload window), atau
	 * fallback membuka {@code new PertemuanHelper(...).display(pertemuan, dataLoader, index)}.
	 * Tombol disusun otomatis menjadi baris-baris "pill" horizontal ({@code tampilPerRow} tombol per
	 * baris: 1000/baris bila {@code vertical}, 4 di mobile, 8 di desktop) agar rapat dan center;
	 * memanggil {@code pertemuan.masukkanData("akses")} sebagai pencatatan akses.
	 *
	 * @param pertemuan              pertemuan yang toolbar aksinya dibangun; tidak boleh null.
	 * @param tbmuser                user yang sedang login, menentukan tombol mana yang tampil/badge apa yang dihitung.
	 * @param mahasiswa              konteks mahasiswa (untuk delegasi ke {@code PertemuanHelper}).
	 * @param biodataCalonMahasiswa  konteks calon mahasiswa (untuk delegasi ke {@code PertemuanHelper}).
	 * @param dataLoader             callback reload; bila null, tombol navigasi tab tidak dipasangi listener (murni tampilan).
	 * @param vertical               true untuk menyusun semua tombol dalam satu kolom (satu tombol per baris).
	 * @param simple                 true untuk menyembunyikan tombol Catatan dan Diskusi (mode ringkas).
	 * @param btnTabNav              navigasi tab existing untuk dipakai langsung (hindari buka window baru); boleh null.
	 * @param buttons                tombol tambahan yang disisipkan ke akhir daftar sebelum disusun jadi baris.
	 * @return Vbox berisi baris-baris tombol aksi, siap ditempel ke parent (mis. kartu pertemuan).
	 */
	public static Vbox createKeteranganData(final Pertemuan pertemuan, final Tbmuser tbmuser, final Mahasiswa mahasiswa,
			final BiodataCalonMahasiswa biodataCalonMahasiswa, final DataLoader dataLoader, final boolean vertical,
			final boolean simple, final ais.ui.util.MyButtonTabbox btnTabNav, final Component... buttons) {

		if (pertemuan != null) {
			pertemuan.masukkanData("akses");
		}
		List<Component> components = new ArrayList<Component>();
		try {

			// Tombol Dasbor — ringkasan semua tab pertemuan
			if (dataLoader != null) {
				MyToolbarbutton aDasbor = new MyToolbarbutton("fa-line-chart", "Dasbor");
				aDasbor.setStyle("color:black;");
				aDasbor.setHref("");
				components.add(aDasbor);
				aDasbor.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (btnTabNav != null) {
							btnTabNav.pilih(9);
						} else {
							new PertemuanHelper(mahasiswa, biodataCalonMahasiswa).display(pertemuan, dataLoader, 9);
						}
					}
				});
			}

			if (!simple) {
				MyToolbarbutton aCatatat = new MyToolbarbutton("fa-file-text-o", "Catatan");
				aCatatat.setStyle(
						(pertemuan.getCatatan() != null && !pertemuan.getCatatan().trim().isEmpty() ? "color:red"
								: "color:black"));
				aCatatat.setHref("");
				components.add(aCatatat);
				if (dataLoader != null) {
					aCatatat.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (btnTabNav != null) {
								btnTabNav.pilih(1);
							} else {
								new PertemuanHelper(mahasiswa, biodataCalonMahasiswa).display(pertemuan, dataLoader, 1);
							}
						}
					});
				}
			}

			TreeMap<Long, PertemuanPunyaUjian> ujiandata = pertemuan.ambilPertemuanPunyaUjianTotal(tbmuser);
			int ujian = ujiandata.size();
			Number diskusi = pertemuan.ambilJumlahPertemuanPunyaDiskusi();

			if (pertemuan.getWisuda() == null) {
				Vbox vboxUjian = new Vbox();
				MyToolbarbutton a = new MyToolbarbutton("fa-check-square-o", "Ujian");
				if (ujian == 0 && tbmuser != null && (tbmuser.getMahasiswa() != null || tbmuser.getSiswa() != null
						|| tbmuser.getBiodataCalonMahasiswa() != null)) {
					vboxUjian.setVisible(false);
				}

				a.setStyle((ujian > 0 ? "color:red" : "color:black"));
				a.setHref("");
				vboxUjian.appendChild(a);

				if (ujian > 0) {

					int totalPeserta = 0;
					for (PertemuanPunyaUjian pertemuanPunyaUjian : ujiandata.values()) {
						int tg = pertemuanPunyaUjian.ambilJumlahHasilUjianMahasiswaTelahIkut(false);
						totalPeserta += tg;
					}

					MyLabelKecil labelKecil = new MyLabelKecil(Common.numberFormat.get().format(ujian) + " ujian");
					labelKecil.setStyle("font-size:8px;color:blue;");
					vboxUjian.appendChild(labelKecil);

					labelKecil = new MyLabelKecil(Common.numberFormat.get().format(totalPeserta) + " peserta");
					labelKecil.setStyle("font-size:8px;color:blue;");
					vboxUjian.appendChild(labelKecil);
				}
				ujiandata.clear();
				ujiandata = null;

				components.add(vboxUjian);
				if (dataLoader != null) {
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (btnTabNav != null) {
								btnTabNav.pilih(6);
							} else {
								new PertemuanHelper(mahasiswa, biodataCalonMahasiswa).display(pertemuan, dataLoader, 6);
							}
						}
					});
				}

			}

			MyToolbarbutton a;
			Vbox vb;
			if (!simple) {
				a = new MyToolbarbutton("fa-comments", "Diskusi");
				a.setStyle((diskusi.intValue() > 0 ? "color:red;" : "color:black;"));
				a.setHref("");

				vb = new Vbox();
				vb.appendChild(a);
				if (diskusi.intValue() > 0) {
					MyLabelKecil labelKecil = new MyLabelKecil(
							Common.numberFormat.get().format(diskusi.intValue()) + " diskusi");
					labelKecil.setStyle("font-size:8px;color:blue;");
					vb.appendChild(labelKecil);
				}

				components.add(vb);

				if (dataLoader != null) {
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (btnTabNav != null) {
								btnTabNav.pilih(7);
							} else {
								new PertemuanHelper(mahasiswa, biodataCalonMahasiswa).display(pertemuan, dataLoader, 7);
							}
						}
					});
				}
			}

			Number file = pertemuan.ambilJumlahPertemuanFileContent();
			Number tugas = pertemuan.ambilJumlahTugasFileContent();

			if (!simple) {
				a = new MyToolbarbutton("fa-files-o", "Materi");
				a.setStyle((file.intValue() > 0 ? "color:red;" : "color:black"));
				a.setHref("");

				vb = new Vbox();
				vb.appendChild(a);
				if (file.intValue() > 0) {
					MyLabelKecil labelKecil = new MyLabelKecil(Common.numberFormat.get().format(file.intValue()) + " materi");
					labelKecil.setStyle("font-size:8px;color:blue;");
					vb.appendChild(labelKecil);
				}
				components.add(vb);

				if (file.intValue() == 0 && tbmuser != null && (tbmuser.getMahasiswa() != null
						|| tbmuser.getSiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null)) {
					vb.setVisible(false);
				}

				if (dataLoader != null) {
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (btnTabNav != null) {
								btnTabNav.pilih(2);
							} else {
								new PertemuanHelper(mahasiswa, biodataCalonMahasiswa).display(pertemuan, dataLoader, 2);
							}
						}
					});
				}
			}

			if (pertemuan.getWisuda() == null) {
				TreeMap<Long, TugasPertemuan> tugases = pertemuan.ambilTugasPertemuanTotal();
				TreeMap<Long, TugasKelompok> tugasesKelompok = pertemuan.ambilTugasKelompokTotal();

				if ((!tugases.isEmpty() || !tugasesKelompok.isEmpty()) && pertemuan.getJudultugas().trim().isEmpty()) {
//				System.out.println("Tugas utama tidak ditampilkan karena sudah ada sub tugas");
				} else {
					Vbox vboxTugas = new Vbox();

					a = new MyToolbarbutton("fa-tasks", "Tugas");

					a.setStyle((pertemuan.getJudultugas() != null && !pertemuan.getJudultugas().trim().equals("")
							? "color:red;"
							: "color:black"));
					a.setHref("");

					vboxTugas.appendChild(a);

					if (pertemuan.getJudultugas() != null && !pertemuan.getJudultugas().trim().equals("")) {

						MyLabelKecil labelKecil = new MyLabelKecil(pertemuan.getJudultugas().length() > 30
								? pertemuan.getJudultugas().substring(0, 30) + "..."
								: pertemuan.getJudultugas());
						labelKecil.setStyle("font-size:8px;color:blue;");
						vboxTugas.appendChild(labelKecil);

						labelKecil = new MyLabelKecil(Common.numberFormat.get().format(tugas.intValue()) + " peserta");
						labelKecil.setStyle("font-size:8px;color:blue;");
						vboxTugas.appendChild(labelKecil);
					}

					if (pertemuan.getJudultugas().trim().isEmpty() && tbmuser != null && (tbmuser.getMahasiswa() != null
							|| tbmuser.getSiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null)) {
						vboxTugas.setVisible(false);
					}

					components.add(vboxTugas);
					if (dataLoader != null) {
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (btnTabNav != null) {
									btnTabNav.pilih(3);
								} else {
									new PertemuanHelper(mahasiswa, biodataCalonMahasiswa).display(pertemuan, dataLoader, 3);
								}
							}
						});
					}
				}

				for (final TugasPertemuan tugasPertemuan : tugases.values()) {
					tugas = tugasPertemuan.ambilJumlahTugasFileContent();
					Vbox vboxTugas = new Vbox();
					a = new MyToolbarbutton("fa-tasks", "Tugas");

					a.setStyle(
							(tugasPertemuan.getJudultugas() != null && !tugasPertemuan.getJudultugas().trim().equals("")
									? "color:red"
									: "color:black"));
					a.setHref("");
					vboxTugas.appendChild(a);

					if (tugasPertemuan.getJudultugas() != null && !tugasPertemuan.getJudultugas().trim().equals("")) {
						MyLabelKecil labelKecil = new MyLabelKecil(tugasPertemuan.getJudultugas().length() > 30
								? tugasPertemuan.getJudultugas().substring(0, 30) + "..."
								: tugasPertemuan.getJudultugas());
						labelKecil.setStyle("font-size:8px;color:blue;");
						vboxTugas.appendChild(labelKecil);

						labelKecil = new MyLabelKecil(Common.numberFormat.get().format(tugas.intValue()) + " peserta");
						labelKecil.setStyle("font-size:8px;color:blue;");
						vboxTugas.appendChild(labelKecil);
					}

					if (tugasPertemuan.getJudultugas().trim().isEmpty() && tbmuser != null
							&& (tbmuser.getMahasiswa() != null || tbmuser.getSiswa() != null
									|| tbmuser.getBiodataCalonMahasiswa() != null)) {
						vboxTugas.setVisible(false);
					}

					components.add(vboxTugas);
					if (dataLoader != null) {
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (btnTabNav != null) {
									btnTabNav.pilih(3);
								} else {
									new PertemuanHelper(mahasiswa, biodataCalonMahasiswa).display(pertemuan, dataLoader, 3,
											tugasPertemuan, null, null, null, null);
								}
							}
						});
					}
				}
				tugases = null;

				for (final TugasKelompok tugasKelompok : tugasesKelompok.values()) {

					Vbox vboxTugas = new Vbox();
					a = new MyToolbarbutton("fa-users", "Tugas");

					a.setStyle(
							(tugasKelompok.getJudultugas() != null && !tugasKelompok.getJudultugas().trim().equals("")
									? "color:red"
									: "color:black"));
					a.setHref("");
					vboxTugas.appendChild(a);

					if (tugasKelompok.getJudultugas() != null && !tugasKelompok.getJudultugas().trim().equals("")) {
						MyLabelKecil labelKecil = new MyLabelKecil(tugasKelompok.getJudultugas().length() > 30
								? tugasKelompok.getJudultugas().substring(0, 30) + "..."
								: tugasKelompok.getJudultugas());
						labelKecil.setStyle("font-size:8px;color:blue;");
						vboxTugas.appendChild(labelKecil);
					}
					if (tugasKelompok.getJudultugas().trim().isEmpty() && tbmuser != null
							&& (tbmuser.getMahasiswa() != null || tbmuser.getSiswa() != null
									|| tbmuser.getBiodataCalonMahasiswa() != null)) {
						vboxTugas.setVisible(false);
					}
					components.add(vboxTugas);
					if (dataLoader != null) {
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (btnTabNav != null) {
									btnTabNav.pilih(3);
								} else {
									new PertemuanHelper(mahasiswa, biodataCalonMahasiswa).display(pertemuan, dataLoader, 3,
											null, tugasKelompok, null, null, null);
								}
							}
						});
					}
				}
				tugasesKelompok = null;
			}

			if (!simple) {
				Number audio = pertemuan.ambilJumlahAudioPertemuan();
				Number video = pertemuan.ambilJumlahVideoPertemuan();

				a = new MyToolbarbutton("fa-file-audio-o", "Audio");

				a.setStyle((audio.intValue() > 0 ? "color:red" : "color:black"));
				a.setHref("");

				vb = new Vbox();
				vb.appendChild(a);
				if (audio.intValue() > 0) {
					MyLabelKecil labelKecil = new MyLabelKecil(
							Common.numberFormat.get().format(audio.intValue()) + " file audio");
					labelKecil.setStyle("font-size:8px;color:blue;");
					vb.appendChild(labelKecil);
				}
				components.add(vb);
				if (audio.intValue() == 0 && tbmuser != null && (tbmuser.getMahasiswa() != null
						|| tbmuser.getSiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null)) {
					vb.setVisible(false);
				}

				if (dataLoader != null) {
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (btnTabNav != null) {
								btnTabNav.pilih(4);
							} else {
								new PertemuanHelper(mahasiswa, biodataCalonMahasiswa).display(pertemuan, dataLoader, 4);
							}
						}
					});
				}

				a = new MyToolbarbutton("fa-file-video-o", "Video");

				a.setStyle((video.intValue() > 0 ? "color:red" : "color:black"));
				a.setHref("");
				vb = new Vbox();
				vb.appendChild(a);
				if (video.intValue() > 0) {
					MyLabelKecil labelKecil = new MyLabelKecil(
							Common.numberFormat.get().format(video.intValue()) + " file video");
					labelKecil.setStyle("font-size:8px;color:blue;");
					vb.appendChild(labelKecil);
				}
				components.add(vb);

				if (video.intValue() == 0 && tbmuser != null && (tbmuser.getMahasiswa() != null
						|| tbmuser.getSiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null)) {
					vb.setVisible(false);
				}

				if (dataLoader != null) {
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (btnTabNav != null) {
								btnTabNav.pilih(5);
							} else {
								new PertemuanHelper(mahasiswa, biodataCalonMahasiswa).display(pertemuan, dataLoader, 5);
							}
						}
					});
				}

				components.add(TampilanELearningAction.dilihat(pertemuan, "akses", "Akses"));

				a = createCalendarButton(pertemuan, tbmuser, DashboardTimelinePertemuan.tampilkan_kalendar_di_elearning,
						dataLoader);
				components.add(a);

				a = createClasroomButton(pertemuan);
				components.add(a);

				if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
						&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getSiswa() == null
						&& tbmuser.getCalonSiswa() == null) {

					a = new MyToolbarbutton("fa-qrcode", "QRCode");

					components.add(a);
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event a) throws Exception {
							File myfilebarcode1 = new File(
									Common.ambilREAL_PATH_REPORT() + "/crcode_" + pertemuan.getId() + ".png");
							BarcodeCommon.generateCRCode(pertemuan.getId().toString(), myfilebarcode1);
							String url = Common.getRequestHostWithProtocol() + "/report/"
									+ URLEncoder.encode(myfilebarcode1.getName(), "UTF-8");

							final MyWindow window = new MyWindow();
							window.setClosable(false);
							ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

							Borderlayout borderlayout = new Borderlayout();
							borderlayout.setParent(window);

							Center center = new Center();
							center.setBorder("none");
							center.setParent(borderlayout);
							ais.ui.util.ZkCompat.setFlex(center, true);
							Image img = new Image(url);
							final Row row = Common.tampilanScroll(center);
							row.appendChild(DashboardTimelinePertemuan.displayInfoPertemuan(pertemuan));

							final MyDatebox tanggal = new MyDatebox(pertemuan.getTanggal());
							tanggal.setReadonly(false);
							final Timebox waktuMulai = new ais.ui.util.MyTimebox();
							waktuMulai.setFormat(Common.timeFormat.get().toPattern());
							final Timebox waktuSelesai = new ais.ui.util.MyTimebox();
							waktuSelesai.setFormat(Common.timeFormat.get().toPattern());

							final MyCheckboxConfig dosenBolehAbsenMenggunakanFoto = new MyCheckboxConfig(
									"Dosen Diizinkan / Boleh Absen Online");
							final MyCheckboxConfig mahasiswaBolehAbsenMenggunakanFoto = new MyCheckboxConfig(
									"Mahasiswa Diizinkan / Boleh Absen Online");
							final MyCheckboxConfig perkulaiahnOnlineHarusSesuaiJadwal = new MyCheckboxConfig(
									"Pertemuan online dan absensi online harus sesuai dengan jadwal yang telah ditentukan");

							EventListener updateLocal = new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									HibernateUtil.currentSession().refresh(pertemuan);
									pertemuan.setTanggal(tanggal.getValue());
									pertemuan.setTanggalEdit(tanggal.getValue());
									pertemuan.setWaktuMulai(waktuMulai.getValue() == null ? null
											: Common.timeFormat2.get().format(waktuMulai.getValue()));
									pertemuan.setWaktuSelesai(waktuSelesai.getValue() == null ? null
											: Common.timeFormat2.get().format(waktuSelesai.getValue()));

									pertemuan.setMahasiswaBolehAbsenMenggunakanFoto(
											mahasiswaBolehAbsenMenggunakanFoto.isChecked());
									pertemuan.setDosenBolehAbsenMenggunakanFoto(
											dosenBolehAbsenMenggunakanFoto.isChecked());
									pertemuan.setPerkulaiahnOnlineHarusSesuaiJadwal(
											perkulaiahnOnlineHarusSesuaiJadwal.isChecked());

									Common.refreshUpdate(pertemuan);

									Common.clear(row);
									row.appendChild(DashboardTimelinePertemuan.displayInfoPertemuan(pertemuan));
								}
							};

							tanggal.addEventListener("onChange", updateLocal);
							waktuMulai.addEventListener("onChange", updateLocal);
							waktuSelesai.addEventListener("onChange", updateLocal);

							try {
								waktuMulai.setValue(
										pertemuan.getWaktuMulai() == null || pertemuan.getWaktuMulai().trim().isEmpty()
												? null
												: Common.timeFormat2.get().parse(pertemuan.getWaktuMulai()));
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AktifitasPerkuliahanHelper.java:3399");

							}
							try {
								waktuSelesai.setValue(pertemuan.getWaktuSelesai() == null
										|| pertemuan.getWaktuSelesai().trim().isEmpty() ? null
												: Common.timeFormat2.get().parse(pertemuan.getWaktuSelesai()));
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AktifitasPerkuliahanHelper.java:3406");

							}

							MyFormRow rowbaru = new MyFormRow();
							rowbaru.setParent(row.getParent());
							Hbox hbox = new Hbox();
							rowbaru.appendChild(hbox);
							hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Ubah waktu : ")));
							hbox.appendChild(tanggal);
							tanggal.setCols(6);
							hbox.appendChild(waktuMulai);
							waktuMulai.setCols(2);
							hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
							hbox.appendChild(waktuSelesai);
							waktuSelesai.setCols(2);

							if (tbmuser != null && tbmuser.ambilDosen() != null
									&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
									&& (pertemuan.getPerkuliahan() != null
											&& !pertemuan.getPerkuliahan().getDosenBisaMerubahTanggalPerkuliahan())) {
								tanggal.setDisabled(true);
								waktuMulai.setDisabled(true);
								waktuSelesai.setDisabled(true);
							}

							rowbaru = new MyFormRow();
							rowbaru.setParent(row.getParent());
							rowbaru.appendChild(dosenBolehAbsenMenggunakanFoto);
							dosenBolehAbsenMenggunakanFoto.setChecked(pertemuan.getDosenBolehAbsenMenggunakanFoto());

							if (pertemuan.getPerkuliahan() != null
									&& !pertemuan.getPerkuliahan().getDosenBolehAbsenMenggunakanFoto()) {
								dosenBolehAbsenMenggunakanFoto.setDisabled(true);
							}

							rowbaru = new MyFormRow();
							rowbaru.setParent(row.getParent());
							rowbaru.appendChild(mahasiswaBolehAbsenMenggunakanFoto);
							mahasiswaBolehAbsenMenggunakanFoto
									.setChecked(pertemuan.getMahasiswaBolehAbsenMenggunakanFoto());

							if (pertemuan.getPerkuliahan() != null
									&& !pertemuan.getPerkuliahan().getMahasiswaBolehAbsenMenggunakanFoto()) {
								mahasiswaBolehAbsenMenggunakanFoto.setDisabled(true);
							}

							dosenBolehAbsenMenggunakanFoto.addEventListener("onClick", updateLocal);
							mahasiswaBolehAbsenMenggunakanFoto.addEventListener("onClick", updateLocal);

							rowbaru = new MyFormRow();
							rowbaru.setParent(row.getParent());
							rowbaru.appendChild(perkulaiahnOnlineHarusSesuaiJadwal);
							perkulaiahnOnlineHarusSesuaiJadwal
									.setChecked(pertemuan.getPerkulaiahnOnlineHarusSesuaiJadwal());
							perkulaiahnOnlineHarusSesuaiJadwal.addEventListener("onClick", updateLocal);

							if (pertemuan.getPerkuliahan() != null
									&& pertemuan.getPerkuliahan().getWaktuPerkuliahanOnlineBebas()) {
								perkulaiahnOnlineHarusSesuaiJadwal.setDisabled(true);
							}

							if (tbmuser != null && tbmuser.getDosen() != null
									&& Common.bolehKonfigurasi("absen_tanpa_batas_waktu", Konfigurasi.TIDAK_AKTIF)) {
								rowbaru.setVisible(false);
							}

							rowbaru = new MyFormRow();
							rowbaru.setParent(row.getParent());
							rowbaru.appendChild(
									new MyLabelBoldConfig("Scan / baca QR-Code berikut untuk melakukan absen :"));

							rowbaru = new MyFormRow();
							rowbaru.setParent(row.getParent());
							rowbaru.appendChild(img);
							img.setWidth("100%");

							South south = new South();
							ais.ui.util.ZkCompat.setFlex(south, true);
							south.setParent(borderlayout);

							Toolbar toolbar = new Toolbar();
							toolbar.setParent(south);
							MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
							cancel.setTooltiptext("Tutup");
							cancel.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									window.detach();
									Common.createDefaultTimer(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											if (dataLoader != null) {
												dataLoader.loadData(true);
											}
										}
									});
								}
							});
							cancel.setParent(toolbar);

							cancel = new MyToolbarbuttonConfig("Scan QR-Code KTM", "/img/QR-Code-icon_.png");
							cancel.setTooltiptext("Tutup");
							cancel.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {

									String host = URLEncoder.encode(
											Common.getRequestHostWithProtocol() + "/Absen?id=" + pertemuan.getId(),
											"UTF-8");

									String src = Common.getRequestHostWithProtocol() + "/read_qr_code_kartu.jsp?q="
											+ host;

									final MyWindow window = new MyWindow("Absen via QR-Code KTM", "none", false);
									ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

									Borderlayout borderlayout = new Borderlayout();
									borderlayout.setParent(window);

									Center center = new Center();
									center.setBorder("none");
									center.setParent(borderlayout);
									ais.ui.util.ZkCompat.setFlex(center, true);

									Html html = new ais.ui.util.MyHtml("<iframe src=\"" + src
											+ "\" style=\"width:100%;height:1500px;border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
									html.setHeight("1500px");
									Common.tampilanScroll(center).appendChild(html);

									South south = new South();
									ais.ui.util.ZkCompat.setFlex(south, true);
									south.setParent(borderlayout);

									Toolbar toolbar = new Toolbar();
									toolbar.setParent(south);
									MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai",
											"/img/cancel.gif");
									cancel.setTooltiptext("Tutup");
									cancel.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											window.detach();
										}
									});
									cancel.setParent(toolbar);
									boolean mobile = Common.isMobile();
									window.setVisible(true);
									window.setHeight("97%");
									window.setWidth(mobile ? "97%" : "750px");
									window.onModal();

								}
							});
							cancel.setParent(toolbar);

							cancel = new MyToolbarbuttonConfig("Scan RFID", "/img/QR-Code-icon_.png");
							cancel.setTooltiptext("Tutup");
							cancel.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {

									String host = URLEncoder.encode(
											Common.getRequestHostWithProtocol() + "/Absen?id=" + pertemuan.getId(),
											"UTF-8");

									String src = Common.getRequestHostWithProtocol() + "/read_rfid_kartu.jsp?q=" + host;

									final MyWindow window = new MyWindow("Absen via RFID", "none", false);
									ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

									Borderlayout borderlayout = new Borderlayout();
									borderlayout.setParent(window);

									Center center = new Center();
									center.setBorder("none");
									center.setParent(borderlayout);
									ais.ui.util.ZkCompat.setFlex(center, true);

									Html html = new ais.ui.util.MyHtml("<iframe src=\"" + src
											+ "\" style=\"width:100%;height:1500px;border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
									html.setHeight("1500px");
									Common.tampilanScroll(center).appendChild(html);

									South south = new South();
									ais.ui.util.ZkCompat.setFlex(south, true);
									south.setParent(borderlayout);

									Toolbar toolbar = new Toolbar();
									toolbar.setParent(south);
									MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai",
											"/img/cancel.gif");
									cancel.setTooltiptext("Tutup");
									cancel.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											window.detach();
										}
									});
									cancel.setParent(toolbar);
									boolean mobile = Common.isMobile();
									window.setVisible(true);
									window.setHeight("97%");
									window.setWidth(mobile ? "97%" : "750px");
									window.onModal();

								}
							});
							cancel.setParent(toolbar);

							boolean mobile = Common.isMobile();
							window.setVisible(true);
							window.setHeight("98%");
							window.setWidth(mobile ? "98%" : "550px");
							window.onModal();
						}
					});
				}

				int jumlahHasil = pertemuan.ambilJumlahKelompokParameterTambahanPertemuan();
				if (jumlahHasil > 0 || (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
						&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
						&& tbmuser.getCalonSiswa() == null)) {
					a = new MyToolbarbutton("fa-check-circle", "Evaluasi");
					a.setStyle((jumlahHasil > 0 ? "color:red;" : "color:black"));
					a.setHref("");

					vb = new Vbox();
					vb.appendChild(a);
					if (jumlahHasil > 0) {
						MyLabelKecil labelKecil = new MyLabelKecil(
								Common.numberFormat.get().format(jumlahHasil) + " pertanyaan");
						labelKecil.setStyle("font-size:8px;color:blue;");
						vb.appendChild(labelKecil);
					}
					components.add(vb);

					if (dataLoader != null) {
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (btnTabNav != null) {
									btnTabNav.pilih(8);
								} else {
									new PertemuanHelper(mahasiswa, biodataCalonMahasiswa).display(pertemuan, dataLoader, 8);
								}
							}
						});
					}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		if (buttons != null) {
			for (Component button : buttons) {
				components.add(button);
			}
		}

		/*
		 * HORIZONTAL + TERPUSAT & RAPAT (gaya Bootstrap btn-toolbar/btn-group):
		 * - Pakai ZK Hbox (render baris <table>) → tombol PASTI mendatar tanpa CSS flex.
		 * - Hbox TIDAK di-set width 100% → lebarnya SEBESAR ISI (tombol rapat), lalu di-tengah-kan
		 *   memakai margin:0 auto + vbox align center. (Set width 100% membuat tombol memenuhi
		 *   lebar & renggang seperti sebelumnya.)
		 * - setSpacing kecil = jarak antar tombol rapat.
		 * - Tiap baris dibungkus "pill" ber-border membulat → tampak seperti satu button group.
		 * - Setiap 'tampilPerRow' tombol (8 desktop / 4 mobile / 1000 = satu baris untuk mode
		 *   vertical) dibuat baris baru. Hbox dibuat HANYA saat ada tombol visible → tidak ada
		 *   pill kosong.
		 */
		int tampilPerRow = vertical ? 1000 : (Common.isMobile() ? 4 : 8);
		/*
		 * KUNCI RAPAT: pill Hbox dibuat "display:inline-table;width:auto" → lebar tabel hanya
		 * SEBESAR ISI (tombol saling rapat), TIDAK memenuhi 100% lebar (yang menyebabkan sel
		 * tersebar renggang). Pemusatan dilakukan lewat text-align:center pada Vbox (td ZK Vbox
		 * mewarisi text-align → pill inline-table otomatis di tengah), BUKAN align Vbox yang
		 * malah menyusutkan sel.
		 */
		final String STYLE_BARIS = "display:inline-table;width:auto;vertical-align:top;margin:4px auto;"
				+ "background:#f8fafc;border:1px solid #e2e8f0;border-radius:10px;padding:4px 8px;"
				+ "box-shadow:0 1px 2px rgba(0,0,0,0.05);";

		Vbox vbox = new Vbox();
		vbox.setWidth("100%");
		vbox.setStyle("text-align:center;");

		Hbox hboxBaru = null;
		int size = 0;
		for (Component component : components) {
			/* Ikon di atas, label di bawah (baik tombol langsung maupun yang dibungkus Vbox). */
			if (component instanceof Button) {
				((Button) component).setOrient("vertical");
			} else {
				Component child = component.getFirstChild();
				while (child != null) {
					if (child instanceof Button) {
						((Button) child).setOrient("vertical");
					}
					child = child.getNextSibling();
				}
			}

			if (component.isVisible()) {
				if (hboxBaru == null || size % tampilPerRow == 0) {
					hboxBaru = new Hbox();
					hboxBaru.setSpacing("6px");
					hboxBaru.setStyle(STYLE_BARIS);
					hboxBaru.setParent(vbox);
				}
				size++;
				component.setParent(hboxBaru);
			}
		}
		components = null;
		return vbox;
	}

	/**
	 * Membangun tombol "Kalender" untuk satu {@link Pertemuan}. Bila event Google Calendar untuk
	 * pertemuan ini sudah ada ({@code CalendarUtil.chekSudahAda}), klik membuka tanggal pertemuan
	 * langsung di Google Calendar (popup/tab baru sesuai {@code Common.isMobile()}); bila belum ada,
	 * klik memicu pembuatan event via {@link CalendarUtil#proses} untuk konteks
	 * {@link PerguruanTinggi} yang relevan (diturunkan dari dosen/mahasiswa/fakultas milik
	 * {@code tbmuser}), lalu memanggil {@code dataLoader.loadData} setelah selesai.
	 *
	 * @param pertemuan  pertemuan yang tombolnya dibangun.
	 * @param tbmuser    user yang sedang login (menentukan konteks perguruan tinggi &amp; visibilitas tombol).
	 * @param tampil     bila false, tombol langsung disembunyikan tanpa pengecekan lain.
	 * @param dataLoader callback reload dipanggil setelah event kalender selesai diproses.
	 * @return tombol siap ditempel ke parent.
	 * @throws Exception diteruskan dari {@link CalendarUtil}.
	 */
	public static MyToolbarbutton createCalendarButton(final Pertemuan pertemuan, final Tbmuser tbmuser, boolean tampil,
			final DataLoader dataLoader) throws Exception {
		final boolean ada = CalendarUtil.chekSudahAda(pertemuan, tbmuser);
//		MyToolbarbuttonConfig a = new MyToolbarbuttonConfig("Kalender", FileFoto.icon("calendar.google"));

		MyToolbarbutton a = new MyToolbarbutton("fa-calendar", "Kalender");

		if (!tampil) {
			a.setVisible(false);
		} else {
			a.setVisible(ada || (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
					&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null));
		}

		a.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (ada) {
					java.util.Calendar calendar = WaktuUtil.getCalendar();
					calendar.setTime(pertemuan.getTanggal());
					String url = "https://calendar.google.com/calendar/r/day/" + calendar.get(java.util.Calendar.YEAR)
							+ "/" + (calendar.get(java.util.Calendar.MONTH) + 1) + "/"
							+ calendar.get(java.util.Calendar.DATE);
					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(url, "_blank");
					} else {

						Clients.evalJavaScript(
								"popupCenter({url: '" + url + "', title: 'Kalender', w: 1200, h: 600});");

					}
				} else {

					CalendarUtil calendarUtil = new CalendarUtil(tbmuser);

					PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

					if (tbmuser != null && tbmuser.ambilDosen() != null
							&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
							&& tbmuser.ambilDosen().getPerguruanTinggi() != null) {
						selectedPerguruanTinggi = tbmuser.ambilDosen().getPerguruanTinggi();
					} else if (tbmuser != null && tbmuser.getMahasiswa() != null
							&& tbmuser.getMahasiswa().getJurusan() != null
							&& tbmuser.getMahasiswa().getJurusan().getFakultas() != null
							&& tbmuser.getMahasiswa().getJurusan().getFakultas().getPerguruanTinggi() != null) {
						selectedPerguruanTinggi = tbmuser.getMahasiswa().getJurusan().getFakultas()
								.getPerguruanTinggi();
					} else if (tbmuser != null && tbmuser.ambilFakultas() != null
							&& tbmuser.ambilFakultas().getPerguruanTinggi() != null) {
						selectedPerguruanTinggi = tbmuser.ambilFakultas().getPerguruanTinggi();
					}
					final List<com.google.api.services.calendar.model.Event> events = new ArrayList<com.google.api.services.calendar.model.Event>();
					TreeMap<String, Long> pertemuans = new TreeMap<String, Long>();
					pertemuans.put(Common.dateFormat8.get().format(pertemuan.getTanggal()) + "_" + pertemuan.getId(),
							pertemuan.getId());
					calendarUtil.proses(pertemuans, selectedPerguruanTinggi, new EventListener() {

						@SuppressWarnings("unchecked")
						@Override
						public void onEvent(Event arg0) throws Exception {
							List<com.google.api.services.calendar.model.Event> eventsa = (List<com.google.api.services.calendar.model.Event>) arg0
									.getData();
							events.addAll(eventsa);
						}
					});

					CalendarUtil.cretaeTimerWaiting(events, pertemuan.getTanggal(), new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							dataLoader.loadData(null);
						}
					});
				}
			}
		});
		return a;
	}

	/**
	 * Varian tombol Kalender untuk {@link GelombangPendaftaran} (gelombang PMB): klik memproses
	 * sinkronisasi Google Calendar via {@link CalendarUtil#proses} lalu memanggil
	 * {@code dataLoader.loadData} setelah selesai. Tersembunyi untuk mahasiswa/siswa/calon.
	 *
	 * @param gelombangPendaftaran gelombang pendaftaran yang jadwalnya disinkronkan ke kalender.
	 * @param tbmuser              user yang sedang login.
	 * @param tampil               bila false, tombol disembunyikan.
	 * @param dataLoader           callback reload setelah sinkronisasi selesai.
	 * @return tombol siap ditempel ke parent.
	 * @throws Exception diteruskan dari {@link CalendarUtil}.
	 */
	public static Button createCalendarButton(final GelombangPendaftaran gelombangPendaftaran, final Tbmuser tbmuser,
			boolean tampil, final DataLoader dataLoader) throws Exception {
		MyToolbarbuttonConfig a = new MyToolbarbuttonConfig("Kalender", FileFoto.icon("calendar.google"));

		if (!tampil) {
			a.setVisible(false);
		} else {
			a.setVisible((tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
					&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null));
		}
		a.setStyle("font-size:9px");
		a.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				CalendarUtil calendarUtil = new CalendarUtil(tbmuser);

				final List<com.google.api.services.calendar.model.Event> events = new ArrayList<com.google.api.services.calendar.model.Event>();

				calendarUtil.proses(gelombangPendaftaran, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						List<com.google.api.services.calendar.model.Event> eventsa = (List<com.google.api.services.calendar.model.Event>) arg0
								.getData();
						events.addAll(eventsa);
					}
				});

				CalendarUtil.cretaeTimerWaiting(events, gelombangPendaftaran.getMulai(), new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						dataLoader.loadData(null);
					}
				});

			}
		});
		return a;
	}

	/**
	 * Varian tombol Kalender untuk {@link InterviewCalonMahasiswa} (jadwal wawancara PMB): klik
	 * memproses sinkronisasi Google Calendar via {@link CalendarUtil#proses} lalu memanggil
	 * {@code dataLoader.loadData} setelah selesai. Tersembunyi untuk mahasiswa/siswa/calon.
	 *
	 * @param interviewCalonMahasiswa jadwal wawancara yang disinkronkan ke kalender.
	 * @param tbmuser                 user yang sedang login.
	 * @param tampil                  bila false, tombol disembunyikan.
	 * @param dataLoader              callback reload setelah sinkronisasi selesai.
	 * @return tombol siap ditempel ke parent.
	 * @throws Exception diteruskan dari {@link CalendarUtil}.
	 */
	public static Button createCalendarButton(final InterviewCalonMahasiswa interviewCalonMahasiswa,
			final Tbmuser tbmuser, boolean tampil, final DataLoader dataLoader) throws Exception {
		MyToolbarbuttonConfig a = new MyToolbarbuttonConfig("Kalender", FileFoto.icon("calendar.google"));

		if (!tampil) {
			a.setVisible(false);
		} else {
			a.setVisible((tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
					&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null));
		}
		a.setStyle("font-size:9px");
		a.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				CalendarUtil calendarUtil = new CalendarUtil(tbmuser);

				final List<com.google.api.services.calendar.model.Event> events = new ArrayList<com.google.api.services.calendar.model.Event>();

				calendarUtil.proses(interviewCalonMahasiswa, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						List<com.google.api.services.calendar.model.Event> eventsa = (List<com.google.api.services.calendar.model.Event>) arg0
								.getData();
						events.addAll(eventsa);
					}
				});

				CalendarUtil.cretaeTimerWaiting(events, interviewCalonMahasiswa.getMulai(), new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						dataLoader.loadData(null);
					}
				});

			}
		});
		return a;
	}

	/**
	 * Membangun tombol "Classroom", terlihat hanya bila {@link Pertemuan} memiliki
	 * {@code VOPembelajaran} dengan properti "ClasroomAlternateLink" terisi (link Google Classroom
	 * hasil integrasi). Klik membuka link tersebut di tab baru (mobile) atau popup terpusat (desktop).
	 *
	 * @param pertemuan pertemuan yang link Classroom-nya diperiksa.
	 * @return tombol siap ditempel ke parent (tersembunyi otomatis bila link tidak ada).
	 * @throws Exception diteruskan dari {@code pertemuan.ambilVOPembelajaran()}.
	 */
	public static MyToolbarbutton createClasroomButton(final Pertemuan pertemuan) throws Exception {
		VOPembelajaran voPembelajaran = pertemuan.ambilVOPembelajaran();
		final String link = voPembelajaran == null ? null : voPembelajaran.retreive("ClasroomAlternateLink");
//		A a = new A("Classroom", FileFoto.icon("classroom.google"));

		MyToolbarbutton a = new MyToolbarbutton("fa-calendar-check-o", "Classroom");

		a.setVisible(link != null && !link.trim().isEmpty());
		a.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (Common.isMobile()) {
					ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
				} else {
					Clients.evalJavaScript("popupCenter({url: '" + Common.jsEscape(link) + "', title: 'Classroom', w: 1200, h: 600});");

				}

			}
		});
		return a;
	}

}
