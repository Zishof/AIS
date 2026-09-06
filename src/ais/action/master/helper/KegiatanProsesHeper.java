package ais.action.master.helper;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.report.CommonReportHelper;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.Fakultas;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisPembayaran;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;
import ais.database.model.rab.SatuanKerja;
import ais.delivery.email.sender.MailSender;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Fitur "Proses Tagihan" massal AIS: kumpulan tombol toolbar ZK yang menghitung ulang
 * {@link Kegiatan}/tagihan untuk BANYAK mahasiswa dan calon mahasiswa sekaligus (bukan satu per
 * satu seperti {@link KegiatanHelper#checkKegiatanMahasiswa}/{@code checkKegiatanCalonMahasiswa}
 * yang dipanggilnya di dalam loop), lalu mengekspor hasilnya ke Excel dan/atau surat tagihan PDF
 * ber-email. Nama file "Heper" (bukan "Helper") adalah TYPO HISTORIS pada nama kelas/file asli —
 * dipertahankan apa adanya (mengubahnya berarti mengubah nama class publik dan memutus seluruh
 * pemanggil di codebase), bukan kesalahan penulisan dokumentasi ini.
 *
 * <p><b>Tiga fitur utama:</b></p>
 * <ol>
 * <li>{@link #prosesUlangTagihan(String, String, JenisKegiatan, boolean)} (dan 3 overload
 * lainnya) — popup filter (Fakultas/Prodi/Mahasiswa/Tahun Akademik/Angkatan/Ganjil-Genap/Status
 * Mahasiswa/Jenis Pembayaran/Item Biaya, opsi "Reset Tagihan Kembali ke Billing" dan "Bersihkan
 * Item Tak Sesuai (Massal)" — irreversible, wajib konfirmasi eksplisit), lalu memanggil
 * {@code KegiatanHelper.checkKegiatanMahasiswa}/{@code checkKegiatanCalonMahasiswa} PARALEL
 * (lewat {@code ExecutorService} sekali-pakai berukuran {@link #getSafeThreadPoolSize}) untuk
 * setiap kombinasi mahasiswa×tahun×semester yang cocok filter, sekaligus MEMPERBARUI
 * {@link HistoryStatusMahasiswaUtil status keaktifan mahasiswa secara massal} — bukan hanya saat
 * layar per-mahasiswa dibuka seperti sebelumnya (lihat komentar inline "keluhan UBT"). Hasil
 * ditulis ke satu file Excel ringkasan tagihan per tahun/semester.</li>
 * <li>{@link #prosesSuratTagihan} — varian yang, alih-alih hanya menghitung tagihan, MEMBUAT
 * surat tagihan PDF ({@code Report.generateFileReport}) per mahasiswa yang sisa tagihannya
 * {@code > 0.1} dan (opsional) MENGIRIM EMAIL lampiran PDF tsb lewat {@link MailSender}. Hasil
 * dikemas jadi satu file ZIP (Excel ringkasan + seluruh PDF surat).</li>
 * <li>{@link #singkronkanDataCicilan} — perbaikan data: memetakan ulang seluruh
 * {@link CicilanPembayaran} ke {@link Kegiatan} yang sesuai lewat
 * {@code KegiatanPersistenceHelper}, dengan progress bar modal dan laporan rinci per-kegiatan
 * (berhasil/gagal) via {@code ais.common.LaporanUpload}.</li>
 * </ol>
 *
 * <p><b>Ketahanan concurrency.</b> Ketiga fitur di atas berjalan di {@link #WORKER_EXECUTOR}
 * (pool BERSAMA berukuran kecil, maks 3 thread daemon) — bukan {@code new Thread().start()} bebas
 * seperti pola lama, untuk mencegah ledakan thread saat banyak admin memicu proses berat
 * bersamaan (tiap tugas di dalamnya masih membuka {@code ExecutorService} sendiri hingga 8
 * thread). Antrian kelebihan pekerjaan MENGANTRI (tak terbatas) alih-alih membludak.</p>
 *
 * <p><b>Bukan tanggung jawab kelas ini:</b> logika penghitungan tagihan itu sendiri (didelegasikan
 * penuh ke {@link KegiatanHelper}), maupun aturan status kemahasiswaan (didelegasikan ke
 * {@link HistoryStatusMahasiswaUtil}) — kelas ini murni orkestrasi UI + paralelisasi + ekspor
 * untuk operasi BATCH/massal atas method-method tsb.</p>
 */
public class KegiatanProsesHeper {

	private static final int DEFAULT_THREAD_POOL_SIZE = Math.max(2,
			Math.min(8, Runtime.getRuntime().availableProcessors()));

	/**
	 * Ukuran pool thread yang aman untuk tugas paralel per-permintaan (dipakai
	 * {@link #prosesUlangTagihan(String, String, JenisKegiatan, boolean)}/{@link #prosesSuratTagihan}
	 * saat membuat {@code ExecutorService} sekali-pakai internal): antara 1 dan
	 * {@link #DEFAULT_THREAD_POOL_SIZE} (2-8, mengikuti jumlah prosesor tersedia, dipangkas ke 8),
	 * tidak pernah melebihi {@code totalWork} agar tidak membuat thread menganggur.
	 *
	 * @param totalWork jumlah pekerjaan yang akan diproses; {@code <= 0} memakai default penuh
	 * @return ukuran pool 1..{@link #DEFAULT_THREAD_POOL_SIZE}
	 */
	private static int getSafeThreadPoolSize(int totalWork) {
		if (totalWork <= 0) {
			return DEFAULT_THREAD_POOL_SIZE;
		}
		return Math.max(1, Math.min(DEFAULT_THREAD_POOL_SIZE, totalWork));
	}

	/**
	 * Pool BERSAMA & TERBATAS untuk pekerjaan latar berat (proses ulang tagihan / kegiatan /
	 * surat tagihan). Menggantikan pola lama {@code new Thread(...).start()} per aksi yang TAK
	 * TERBATAS — penyebab ledakan jumlah thread saat banyak admin/aksi berjalan bersamaan. Ukuran
	 * kecil (maks 3) karena tiap tugas masih memakai pool internal lagi (≤8 thread); aksi berlebih
	 * akan MENGANTRI (antrian tak-terbatas) alih-alih membludak jadi ribuan thread. Daemon agar tak
	 * menahan shutdown JVM/Tomcat. Perilaku tiap proses TIDAK berubah, hanya dibatasi berapa yang
	 * berjalan serentak.
	 */
	private static final ExecutorService WORKER_EXECUTOR = Executors.newFixedThreadPool(
			Math.max(2, Math.min(3, Runtime.getRuntime().availableProcessors())),
			new java.util.concurrent.ThreadFactory() {
				private final AtomicInteger nomor = new AtomicInteger(1);

				@Override
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r, "KegiatanProses-Worker-" + nomor.getAndIncrement());
					t.setDaemon(true);
					return t;
				}
			});

	/**
	 * Wadah hasil SATU TASK PARALEL di {@link #prosesSuratTagihan} (satu mahasiswa/calon
	 * mahasiswa, bisa menghasilkan beberapa baris/surat bila diproses lintas beberapa
	 * tahun×semester). Dipakai agar tiap {@link Callable} thread-worker bisa mengumpulkan hasilnya
	 * secara independen (tanpa lock/race) sebelum digabung oleh thread utama setelah SEMUA task
	 * selesai ({@code executor.invokeAll}).
	 */
	private static class SuratResult {
		/** Satu baris per surat yang dibuat, untuk ditulis ke sheet Excel ringkasan (kolom NIM/nama/jenis/fakultas/jurusan/status/angkatan/email/TA/semester/sisa tagihan). */
		public List<Object[]> excelRows = new ArrayList<Object[]>();
		/** File PDF surat tagihan yang dihasilkan (hanya untuk baris dengan sisa tagihan {@code > 0.1}), untuk dikemas ke ZIP hasil akhir. */
		public List<File> files = new ArrayList<File>();
		/** Nama file dalam ZIP untuk tiap entri {@link #files} (berpasangan indeks), berisi info email/NIM/nama/jenis kegiatan/semester. */
		public List<String> fileNames = new ArrayList<String>();
	}

	/**
	 * Sinonim {@link #prosesUlangTagihan(String, String, JenisKegiatan)} yang mengambil jenis
	 * kegiatan default dari combobox yang SUDAH dipilih pengguna di layar pemanggil (mis. filter
	 * jenis pembayaran pada dasbor), bukan {@code null}/"Semua" — memudahkan popup langsung
	 * ter-prefill sesuai konteks layar asal.
	 *
	 * @param buttonLabel   label tombol
	 * @param buttonImage   path ikon tombol
	 * @param jenisKegiatan combobox sumber default jenis kegiatan; item terpilihnya (bila ada) dipakai sebagai default popup
	 * @return tombol toolbar siap dipasang ke UI
	 */
	public static MyToolbarbuttonConfig prosesUlangTagihanCombo(String buttonLabel, String buttonImage,
			Combobox jenisKegiatan) {
		JenisKegiatan jenisPembayaranDefault = jenisKegiatan == null || jenisKegiatan.getSelectedItem() == null ? null
				: (JenisKegiatan) jenisKegiatan.getSelectedItem().getValue();
		return prosesUlangTagihan(buttonLabel, buttonImage, jenisPembayaranDefault);
	}

	/** Sinonim {@code prosesUlangTagihan(buttonLabel, buttonImage, null)} — popup dengan filter Jenis Pembayaran default "Semua". */
	public static MyToolbarbuttonConfig prosesUlangTagihan(String buttonLabel, String buttonImage) {
		JenisKegiatan jenisPembayaranDefault = null;
		return prosesUlangTagihan(buttonLabel, buttonImage, jenisPembayaranDefault);
	}
	
	
	// =========================================================================
		// 1. FITUR SINGKRONISASI DATA CICILAN (ANTI LEAK & NATIVE SQL)
		// =========================================================================

		/**
		 * Tombol perbaikan data: memetakan ulang SELURUH {@link CicilanPembayaran} ke
		 * {@link Kegiatan} yang sesuai (delegasi penuh ke
		 * {@code KegiatanPersistenceHelper.ambilPetaCicilanPerKegiatan}/
		 * {@code sinkronkanCicilanKegiatanLangsung}), dipakai saat data ringkasan pembayaran
		 * kegiatan diduga tidak sinkron dengan cicilan aslinya. Setelah konfirmasi, menampilkan
		 * window progress modal (progressmeter + label status, di-{@code doHighlighted} bukan
		 * {@code doModal} agar tidak menyuspend event-thread ZK) dan memproses tiap kelompok
		 * cicilan-per-kegiatan PARALEL lewat {@code ExecutorService} berukuran
		 * {@code KegiatanPersistenceHelper.hitungThreadPoolAman}, di dalam {@link #WORKER_EXECUTOR}
		 * agar tidak menambah beban thread di luar batas bersama.
		 * <p>
		 * <b>Kuirk server-push</b> (dicatat eksplisit di kode sebagai "OPTIMASI FASE 5"): ZK server
		 * push diaktifkan untuk memperbarui progress bar real-time, tapi HANYA dimatikan lagi di
		 * {@code finally} bila method inilah yang menyalakannya (menghormati bila desktop sudah
		 * punya push aktif dari fitur lain) — sebelumnya push dinyalakan tapi tidak pernah
		 * dimatikan, menyebabkan browser terus polling dan menahan thread Tomcat selama tab
		 * terbuka.
		 * <p>
		 * Hasil akhir: laporan rinci per-kegiatan (berhasil/gagal beserta penyebab teknis lengkap)
		 * lewat {@code ais.common.LaporanUpload}, diunduh otomatis sebagai file setelah window
		 * progress ditutup.
		 *
		 * @param labelBtn label tombol
		 * @param icon     path ikon tombol
		 * @return tombol toolbar siap dipasang ke UI
		 */
		public static MyToolbarbuttonConfig singkronkanDataCicilan(String labelBtn, String icon) {
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(labelBtn, icon);

		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				MyMessageboxConfig.show(
						"Konfirmasi Sinkronisasi:\n\n"
								+ "Sistem akan memetakan ulang cicilan pembayaran ke data kegiatan, "
								+ "membangun ulang ringkasan pembayaran, lalu menyimpan hasil sinkronisasi. Lanjutkan?",
						"Peringatan Sistem", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
						MyMessageboxConfig.QUESTION, new EventListener() {
							@Override
							public void onEvent(Event ev) throws Exception {
								if (Integer.parseInt(ev.getData().toString()) != MyMessageboxConfig.OK) {
									return;
								}

								final Desktop desktop = Executions.getCurrent().getDesktop();
								/* OPTIMASI FASE 5: push dulu dinyalakan tapi tidak pernah dimatikan sehingga browser
								 * terus polling dan menahan thread Tomcat selama tab terbuka. Ditandai di sini lalu
								 * dilepas pada finally tugas latar di bawah (WORKER_EXECUTOR tetap dipakai apa adanya
								 * karena ukurannya sengaja kecil, lihat catatan pada WORKER_EXECUTOR). */
								boolean pushBaruDinyalakan = false;
								if (!desktop.isServerPushEnabled()) {
									desktop.enableServerPush(true);
									pushBaruDinyalakan = true;
								}
								final boolean pushDinyalakanDiSini = pushBaruDinyalakan;

								final Window winProgress = new Window("Sinkronisasi Data Cicilan", "normal", true);
								winProgress.setWidth("520px");
								winProgress.setClosable(false);
								winProgress.setSizable(false);
								winProgress.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

								Vbox vbox = new Vbox();
								vbox.setAlign("center");
								vbox.setWidth("100%");
								vbox.setStyle("padding:18px;background:#f8fafc;box-sizing:border-box;");

								org.zkoss.zul.Html header = new org.zkoss.zul.Html(
										"<div style='width:100%;box-sizing:border-box;background:white;border:1px solid #dbe4f0;"
												+ "border-radius:10px;padding:12px;margin-bottom:12px;'>"
												+ "<div style='font-size:15px;font-weight:700;color:#0f172a;'>Sinkronisasi Cicilan Pembayaran</div>"
												+ "<div style='font-size:11px;color:#64748b;margin-top:4px;'>"
												+ "Proses berjalan bertahap agar server tetap ringan dan data kegiatan tetap konsisten.</div>"
												+ "</div>");
								header.setWidth("100%");
								vbox.appendChild(header);

								final Label lbStatus = new Label(ais.common.Common.getBahasaConfig("Menyiapkan proses sinkronisasi..."));
								lbStatus.setStyle("font-weight:bold;font-size:12px;color:#334155;");
								lbStatus.setWidth("100%");

								final Progressmeter pm = new Progressmeter();
								pm.setWidth("100%");
								pm.setValue(0);

								final Label lbInfo = new Label("0%");
								lbInfo.setStyle("font-size:11px;color:#64748b;");
								lbInfo.setWidth("100%");

								vbox.appendChild(lbStatus);
								vbox.appendChild(pm);
								vbox.appendChild(lbInfo);
								winProgress.appendChild(vbox);
								// doHighlighted: overlay mengambang di layer paling atas tanpa suspend
								// (doModal gagal/embedded saat event-thread ZK dimatikan).
								winProgress.setZindex(100000);
								winProgress.doHighlighted();

								// Laporan rinci per kegiatan (berhasil/gagal+penyebab teknis lengkap+langkah
								// mengatasi) - sebelumnya cuma agregat "Berhasil: X, gagal: Y" tanpa
								// pernah menyebut kegiatan MANA yang gagal atau kenapa.
								final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Data Cicilan Kegiatan");
								final AtomicInteger nomorBarisLaporan = new AtomicInteger(0);

								WORKER_EXECUTOR.execute(new Runnable() {
									@Override
									public void run() {
										try {
										ExecutorService executor = null;
										try {
											updateUI(desktop, lbStatus, pm, 3, "Membaca relasi cicilan dari database...");
											updateInfo(desktop, lbInfo, "Mempersiapkan data sumber.");

											final Map<Long, List<Long>> kegGroups = KegiatanPersistenceHelper
													.ambilPetaCicilanPerKegiatan();
											final int totalKeg = kegGroups == null ? 0 : kegGroups.size();

											if (totalKeg == 0) {
												updateUI(desktop, lbStatus, pm, 100, "Tidak ada data cicilan yang perlu disinkronkan.");
												updateInfo(desktop, lbInfo, "0 data kegiatan diproses.");
												Thread.sleep(1200);
												return;
											}

											int jumlahThread = KegiatanPersistenceHelper.hitungThreadPoolAman(totalKeg);
											executor = Executors.newFixedThreadPool(jumlahThread);

											updateUI(desktop, lbStatus, pm, 8,
													"Memproses " + totalKeg + " kegiatan menggunakan " + jumlahThread
															+ " thread aman...");
											updateInfo(desktop, lbInfo, "0 / " + totalKeg + " kegiatan.");

											final AtomicInteger counter = new AtomicInteger(0);
											final AtomicInteger berhasil = new AtomicInteger(0);
											final AtomicInteger gagal = new AtomicInteger(0);
											List<Future<?>> futures = new ArrayList<Future<?>>();

											for (final Map.Entry<Long, List<Long>> entry : kegGroups.entrySet()) {
												futures.add(executor.submit(new Runnable() {
													@Override
													public void run() {
														int nomorBaris = nomorBarisLaporan.getAndIncrement();
														try {
															boolean ok = KegiatanPersistenceHelper.sinkronkanCicilanKegiatanLangsung(
																	entry.getKey(), entry.getValue());
															if (ok) {
																berhasil.incrementAndGet();
																laporan.catatBerhasil(nomorBaris, "ID:" + entry.getKey(), "Sinkronisasi berhasil");
															} else {
																gagal.incrementAndGet();
																laporan.catatGagal(nomorBaris, "ID:" + entry.getKey(),
																		"sinkronkanCicilanKegiatanLangsung mengembalikan false (lihat KegiatanPersistenceHelper.java utk kondisi penyebab)");
															}
														} catch (Exception e) {
															gagal.incrementAndGet();
															Common.tampilErrorJikaAdmin(e);
															laporan.catatGagalDetail(nomorBaris, "ID:" + entry.getKey(), e);
														} finally {
															int count = counter.incrementAndGet();
															if (count % 10 == 0 || count == totalKeg) {
																int progress = 8 + (int) ((count * 90.0) / totalKeg);
																if (progress > 98) {
																	progress = 98;
																}
																updateUI(desktop, lbStatus, pm, progress,
																		"Memproses data kegiatan: " + count + " / "
																				+ totalKeg);
																updateInfo(desktop, lbInfo,
																		"Berhasil: " + berhasil.get() + ", gagal: "
																				+ gagal.get() + ".");
															}
														}
													}
												}));
											}

											for (Future<?> future : futures) {
												future.get();
											}

											updateUI(desktop, lbStatus, pm, 100, "Sinkronisasi selesai.");
											updateInfo(desktop, lbInfo,
													"Berhasil: " + berhasil.get() + " kegiatan, gagal: " + gagal.get()
															+ " kegiatan.");
											Thread.sleep(1500);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											updateUI(desktop, lbStatus, pm, 100,
													"Sinkronisasi berhenti karena terjadi kendala.");
											updateInfo(desktop, lbInfo,
													"Silakan cek log aplikasi atau pesan error administrator.");
										} finally {
											if (executor != null) {
												try {
													executor.shutdownNow();
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanProsesHeper.java:299");
												}
											}
											Executions.schedule(desktop, new EventListener() {
												@Override
												public void onEvent(Event arg0) {
													try {
														winProgress.detach();
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanProsesHeper.java:307");
													}
													// Laporan rinci per kegiatan (berhasil/gagal+penyebab teknis
													// lengkap) otomatis diunduh sbg berkas teks.
													try {
														laporan.selesaikan(null);
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanProsesHeper.java:laporanSelesai");
													}
												}
											}, null);
										}
										} finally {
											/* OPTIMASI FASE 5: push dulu dinyalakan tapi tidak pernah dimatikan sehingga browser
											 * terus polling dan menahan thread Tomcat selama tab terbuka. Matikan HANYA bila kita
											 * yang menyalakannya, dan hanya bila desktop masih hidup. */
											if (pushDinyalakanDiSini && desktop != null && desktop.isAlive() && desktop.isServerPushEnabled()) {
												try { desktop.enableServerPush(false); } catch (Exception e) {
													ais.common.ErrorAuditUtil.record(e, "Fase5.lepasServerPush");
												}
											}
										}
									}
								});
							}
						});
			}
		});

		return button;
	}

	/** Menjadwalkan (via {@code Executions.schedule}, aman dipanggil dari thread worker non-ZK) pembaruan teks {@code label} dan nilai {@code progressmeter} pada thread event ZK milik {@code desktop} — dipakai {@link #singkronkanDataCicilan} untuk melaporkan progres dari thread paralel. Exception (mis. desktop sudah mati) ditelan diam-diam. */
	private static void updateUI(Desktop desktop, final Label label, final Progressmeter progressmeter, final int value,
			final String message) {
		try {
			Executions.schedule(desktop, new EventListener() {
				@Override
				public void onEvent(Event event) {
					if (label != null) {
						label.setValue(message);
					}
					if (progressmeter != null) {
						progressmeter.setValue(value);
					}
				}
			}, null);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanProsesHeper.java:336");
		}
	}

	/** Padanan {@link #updateUI} tanpa progressmeter — menjadwalkan pembaruan teks {@code label} saja pada thread event ZK milik {@code desktop}. */
	private static void updateInfo(Desktop desktop, final Label label, final String message) {
		try {
			Executions.schedule(desktop, new EventListener() {
				@Override
				public void onEvent(Event event) {
					if (label != null) {
						label.setValue(message);
					}
				}
			}, null);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanProsesHeper.java:350");
		}
	}

	/**
	 * Ubah nilai sel menjadi double secara aman. Kolom total (tagihan/dibayar/tunggakan) umumnya
	 * berisi Number, namun sebagian baris bisa mengembalikan String (mis. "-"/kosong/berformat)
	 * yang dahulu memicu ClassCastException saat di-cast (Double). Number diambil apa adanya;
	 * String di-parse setelah dibersihkan pemisah ribuan; bila gagal/null dianggap 0.
	 */
	private static double nilaiDouble(Object v) {
		if (v == null) {
			return 0d;
		}
		if (v instanceof Number) {
			return ((Number) v).doubleValue();
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return 0d;
		}
		s = s.replace("Rp", "").replace(" ", "");
		boolean adaTitik = s.indexOf('.') >= 0;
		boolean adaKoma = s.indexOf(',') >= 0;
		if (adaTitik && adaKoma) {
			// Format Indonesia: titik = pemisah ribuan, koma = desimal.
			s = s.replace(".", "").replace(",", ".");
		} else if (adaKoma) {
			// Hanya koma → anggap sebagai desimal.
			s = s.replace(",", ".");
		}
		try {
			return Double.parseDouble(s);
		} catch (Exception e) {
			return 0d;
		}
	}


	/** Sinonim {@code prosesUlangTagihan(buttonLabel, buttonImage, jenisPembayaranDefault, false)} — combobox Jenis Pembayaran popup TIDAK dikunci (pengguna bisa mengubahnya meski ada default). */
	public static MyToolbarbuttonConfig prosesUlangTagihan(String buttonLabel, String buttonImage,
			final JenisKegiatan jenisPembayaranDefault) {
		return prosesUlangTagihan(buttonLabel, buttonImage, jenisPembayaranDefault, false);
	}

	/**
	 * Implementasi inti fitur "Proses Tagihan" massal — lihat Javadoc kelas
	 * {@link KegiatanProsesHeper} untuk gambaran umum. Membangun popup filter lengkap
	 * (Fakultas/Prodi/Mahasiswa, Hitung Ulang, rentang Tahun Akademik, Jenis Pembayaran opsional
	 * dikunci lewat {@code lockJenis}, rentang Tahun Angkatan opsional dikunci lewat attribute
	 * {@code "defaultTahunAngkatan"} yang diset overload 5-param, Ganjil/Genap, filter Status
	 * Mahasiswa, "Reset Tagihan Kembali ke Billing", dan "Bersihkan Item Tak Sesuai (Massal)" —
	 * opsi terakhir MENGHAPUS PERMANEN item tagihan belum-dibayar yang tak cocok Setting Biaya
	 * berlaku, non-aktif default, wajib konfirmasi eksplisit SAAT dicentang bukan saat Proses
	 * diklik).
	 * <p>
	 * Saat "Proses Tagihan" diklik: tombol asal di-{@code setDisabled(true)} (dicegah klik ganda),
	 * lalu di {@link #WORKER_EXECUTOR} — untuk setiap {@link JenisKegiatan} yang cocok filter (satu
	 * bila dipilih spesifik, atau SEMUA jenis aktif bila "Semua"), untuk setiap mahasiswa/calon
	 * mahasiswa yang cocok filter, untuk setiap kombinasi tahun×Ganjil/Genap dalam rentang: dibuat
	 * satu {@link Callable} yang memanggil
	 * {@code KegiatanHelper.checkKegiatanMahasiswa}/{@code checkKegiatanCalonMahasiswa}, opsional
	 * {@code KegiatanPersistenceHelper.bersihkanItemAsing} (dihitung ulang lagi setelahnya bila ada
	 * yang dihapus), lalu SELALU memanggil
	 * {@link HistoryStatusMahasiswaUtil#currentStatus(Mahasiswa, String, Integer, boolean)} dengan
	 * {@code refresh=true} — inilah yang membuat status keaktifan ikut diperbarui massal (bukan
	 * hanya tagihan). Seluruh {@link Callable} dieksekusi PARALEL lewat
	 * {@code executor.invokeAll} (ukuran pool dari {@link #getSafeThreadPoolSize}), lalu HASILNYA
	 * dirakit ke satu file Excel (kolom Tag./Byr./Sisa per tahun-semester + grand total + rincian
	 * tagihan per baris) oleh THREAD UTAMA setelah semua task selesai (menghindari race menulis
	 * workbook POI dari banyak thread).
	 * <p>
	 * Progress dan hasil akhir ditampilkan lewat mekanisme timer-polling + preview
	 * {@link Spreadsheet} yang sama dengan {@link KegiatanHelper#doDownloadTagihan}. Bila
	 * "Bersihkan Item Tak Sesuai" dijalankan, popup ringkasan jumlah item yang terhapus ditampilkan
	 * sebelum preview Excel.
	 *
	 * @param buttonLabel             label tombol
	 * @param buttonImage             path ikon tombol
	 * @param jenisPembayaranDefault  jenis kegiatan default terpilih di popup, atau {@code null} untuk "Semua"
	 * @param lockJenis               {@code true} untuk mengunci combobox Jenis Pembayaran (tidak bisa diubah pengguna)
	 * @return tombol toolbar siap dipasang ke UI
	 */
	public static MyToolbarbuttonConfig prosesUlangTagihan(String buttonLabel, String buttonImage,
			final JenisKegiatan jenisPembayaranDefault, final boolean lockJenis) {
		final MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(buttonLabel, buttonImage);

		toolbarbutton.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				final MyWindow window = new MyWindow("Pilih Tahun Akademik, Angkatan, dan Jenis Pembayaran", "none",
						true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("95%");
				window.setWidth("600px");

				final Combobox tahunAkademik = new Combobox();
				Common.generateTahunAjaran(tahunAkademik);

				final Combobox tahunAkademikSampai = new Combobox();
				Common.generateTahunAjaran(tahunAkademikSampai);

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);

				Center center = new Center();
				center.setParent(borderlayout);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);
				MyColumnConfig column = new MyColumnConfig();
				column.setWidth("30%");
				column.setParent(columns);
				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				final Combobox fakultas = new Combobox();
				final Combobox jurusan = new Combobox();
				Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
				row.appendChild(fakultas);
				fakultas.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
				row.appendChild(jurusan);
				jurusan.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa"));
				final MyTextbox mahasiswa = new MyTextbox();
				row.appendChild(mahasiswa);
				mahasiswa.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig hitungUlang = new MyCheckboxConfig("Hitung Ulang Tagihan");
				row.appendChild(hitungUlang);
				hitungUlang.setChecked(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik Mulai *"));
				row.appendChild(tahunAkademik);
				tahunAkademik.setWidth("90%");
				tahunAkademik.setReadonly(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik Sampai *"));
				row.appendChild(tahunAkademikSampai);
				tahunAkademikSampai.setWidth("90%");
				tahunAkademikSampai.setReadonly(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pembayaran"));

				final Combobox jenisPembayaran = new Combobox();
				row.appendChild(jenisPembayaran);
				jenisPembayaran.setWidth("90%");
				jenisPembayaran.setReadonly(true);
				Common.insertComboDanSemua(jenisPembayaran, "namaKegiatan", JenisKegiatan.class,
						Restrictions.eq("aktif", true));

				if (jenisPembayaranDefault != null) {
					Common.selectComboItem(jenisPembayaran, jenisPembayaranDefault, true);
				}
				if (lockJenis) {
					jenisPembayaran.setDisabled(true);
				}

				// Baca default tahun angkatan dari attribute tombol (diset oleh overload 5-param).
				// Jika tidak ada, gunakan range default (tahun_sekarang-5 s/d tahun_sekarang).
				int _defAngkatan = 0;
				try {
					Object _attr = toolbarbutton.getAttribute("defaultTahunAngkatan");
					if (_attr instanceof Integer) _defAngkatan = ((Integer) _attr).intValue();
				} catch (Exception _ex) { ais.common.ErrorAuditUtil.record(_ex, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanProsesHeper.java:504"); /* abaikan */ }
				final int defAngkatan = _defAngkatan;

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan Mulai *"));
				final Intbox angkatan = new Intbox(
						defAngkatan > 0 ? defAngkatan
								: ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 5);
				row.appendChild(angkatan);
				angkatan.setWidth("90%");
				if (defAngkatan > 0) angkatan.setDisabled(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan Sampai *"));
				final Intbox angkatanSampai = new Intbox(
						defAngkatan > 0 ? defAngkatan
								: ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));
				row.appendChild(angkatanSampai);
				angkatanSampai.setWidth("90%");
				if (defAngkatan > 0) angkatanSampai.setDisabled(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig gnj = new MyCheckboxConfig("Ganjil");
				row.appendChild(gnj);
				gnj.setChecked(Common.isNowSemensterGanjil());

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig gnp = new MyCheckboxConfig("Genap");
				row.appendChild(gnp);
				gnp.setChecked(!Common.isNowSemensterGanjil());

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Status Mahasiswa"));
				final Combobox statusMahasiswa = new Combobox();
				final Combobox statusTahunAkademik = new Combobox();
				Hbox hbox = new Hbox();
				hbox.setParent(row);
				hbox.appendChild(statusMahasiswa);
				hbox.appendChild(statusTahunAkademik);
				Common.insertComboDanSemua(statusMahasiswa, "nama", StatusMahasiswa.class);
				Common.generateTahunAjaran(statusTahunAkademik);
				statusMahasiswa.setCols(3);
				statusTahunAkademik.setCols(4);

				final Combobox genapGanjil = new Combobox();
				hbox.appendChild(genapGanjil);
				org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
				comboitem.setLabel(Perkuliahan.GENAP);
				comboitem.setValue(Perkuliahan.GENAP);
				genapGanjil.appendChild(comboitem);
				comboitem = new MyComboitemConfig();
				comboitem.setLabel(Perkuliahan.GANJIL);
				comboitem.setValue(Perkuliahan.GANJIL);
				genapGanjil.appendChild(comboitem);

				Common.selectComboItem(genapGanjil,
						Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
				genapGanjil.setReadonly(true);
				genapGanjil.setCols(3);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig reset = new MyCheckboxConfig("Reset Tagihan Kembali ke Billing");
				row.appendChild(reset);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				// PERMINTAAN: opsi "Bersihkan Item Tak Sesuai" (sebelumnya hanya tersedia per-mahasiswa
				// lewat tombol di InformasiPembayaranMahasiswaAction) kini bisa dijalankan MASSAL utk
				// seluruh mahasiswa yg cocok dgn filter Fakultas/Prodi/Angkatan/dll di atas -- memakai
				// logika deteksi & hapus yg SAMA (KegiatanPersistenceHelper.bersihkanItemAsing), tidak
				// duplikasi. Non-aktif secara default krn sifatnya MENGHAPUS data (irreversible).
				final MyCheckboxConfig bersihkanMassal = new MyCheckboxConfig("Bersihkan Item Tak Sesuai (Massal)");
				bersihkanMassal.setChecked(false);
				bersihkanMassal.setStyle("color:#b91c1c;font-weight:600;");
				row.appendChild(bersihkanMassal);
				// Konfirmasi PERSIS SAAT dicentang (bukan menunggu "Proses Tagihan" diklik) --
				// supaya alur "Proses Tagihan" di bawah tidak perlu diubah sama sekali (aksi ini
				// MENGHAPUS DATA permanen utk seluruh mahasiswa yg cocok filter, jadi wajib
				// dikonfirmasi eksplisit). Batal -> centang otomatis dilepas lagi.
				bersihkanMassal.addEventListener("onCheck", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (!bersihkanMassal.isChecked()) {
							return;
						}
						MyMessageboxConfig.show(
								"PERINGATAN: opsi ini akan MENGHAPUS PERMANEN item tagihan yang terdeteksi tidak sesuai (belum dibayar & tidak cocok Setting Biaya yang berlaku) untuk SELURUH mahasiswa yang cocok dengan filter Fakultas/Prodi/Angkatan/Tahun Akademik/Jenis Pembayaran di atas, bukan hanya satu mahasiswa. Tindakan ini TIDAK DAPAT DIBATALKAN. Item yang SUDAH ADA PEMBAYARAN tidak akan ikut terhapus. Apakah Bapak/Ibu yakin ingin melanjutkan?",
								"Konfirmasi Bersihkan Massal", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
								MyMessageboxConfig.QUESTION, new EventListener() {
									@Override
									public void onEvent(Event ev) throws Exception {
										if (Integer.parseInt(ev.getData().toString()) != MyMessageboxConfig.OK) {
											bersihkanMassal.setChecked(false);
										}
									}
								});
					}
				});

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Reset Item Biaya"));

				final Combobox itemBiaya = new Combobox();
				row.appendChild(itemBiaya);
				itemBiaya.setWidth("90%");
				itemBiaya.setReadonly(true);
				Common.insertComboDanSemua(itemBiaya, "nama", ItemBiaya.class, Restrictions.eq("aktif", true));

				South south = new South();
				ais.ui.util.ZkCompat.setFlex(south, true);
				south.setParent(borderlayout);

				Toolbar toolbar = new Toolbar();
				toolbar.setParent(south);
				MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
				cancel.setTooltiptext("Tutup");
				cancel.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						window.detach();
					}
				});
				cancel.setParent(toolbar);

				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Proses Tagihan", "/img/save.gif");
				save.setTooltiptext("Proses");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						toolbarbutton.setDisabled(true);

						final String ta = (String) tahunAkademik.getSelectedItem().getValue();
						final String taSampai = (String) tahunAkademikSampai.getSelectedItem().getValue();
						final Integer ang = angkatan.getValue() == null ? 0 : angkatan.getValue();
						final Integer angSampai = angkatanSampai.getValue() == null ? 0 : angkatanSampai.getValue();

						// Jika "Semua" dipilih, nilainya null
						final JenisKegiatan jenisKegiatanTerpilih = (JenisKegiatan) (jenisPembayaran
								.getSelectedItem() == null ? null : jenisPembayaran.getSelectedItem().getValue());

						final Fakultas fak = (Fakultas) (fakultas.getSelectedItem() == null ? null
								: fakultas.getSelectedItem().getValue());
						final Jurusan jur = (Jurusan) (jurusan.getSelectedItem() == null ? null
								: jurusan.getSelectedItem().getValue());

						final StatusMahasiswa status = (StatusMahasiswa) (statusMahasiswa.getSelectedItem() == null
								? null
								: statusMahasiswa.getSelectedItem().getValue());
						final String taStatus = (String) statusTahunAkademik.getSelectedItem().getValue();
						final String jenisSmtStatus = (String) genapGanjil.getSelectedItem().getValue();

						final String mhs = mahasiswa.getValue().trim();
						final Boolean ulang = hitungUlang.isChecked();

						final boolean GANJIL = gnj.isChecked();
						final boolean GENAP = gnp.isChecked();
						final boolean rst = reset.isChecked();
						final boolean bersihkanMassalDijalankan = bersihkanMassal.isChecked();
						final java.util.concurrent.atomic.AtomicInteger totalItemAsingDibersihkan = new java.util.concurrent.atomic.AtomicInteger(
								0);

						final ItemBiaya item = (ItemBiaya) (itemBiaya.getSelectedItem() == null ? null
								: itemBiaya.getSelectedItem().getValue());

						window.detach();

						final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
						final Intbox intbox = new Intbox(10);
						final Intbox colS = new Intbox(10);
						Clients.showBusy(label.getValue());

						final String filename = Sessions.getCurrent().getWebApp()
								.getRealPath("/tmp/cetak_data_" + URLEncoder.encode(
										Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
										+ ".xlsx");
						final File file = new File(filename);
						file.createNewFile();

						final Timer timer = new Timer(200);
						timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						timer.setRepeats(true);
						timer.addEventListener("onTimer", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								try {
									Clients.showBusy(label.getValue());
									if (label.getValue().trim().equalsIgnoreCase("-")) {
										Clients.clearBusy();
										timer.detach();
										toolbarbutton.setDisabled(false);
									} else if (label.getValue().isEmpty()) {
										toolbarbutton.setDisabled(false);
										Center center = new Center();
										final MyWindow window = new MyWindow("Cetak Data", "none", true);
										window.setParent(
												ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
										window.setHeight("97%");
										window.setWidth("90%");

										Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
										borderlayout.setParent(window);

										ais.ui.util.ZkCompat.setFlex(center, true);
										center.setParent(borderlayout);

										Common.clear(center);
										Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
										Common.clear(center);
										spreadsheet.setParent(center);
										spreadsheet.setWidth("100%");
										spreadsheet.setHeight("100%");
										spreadsheet.setSrc("../../tmp/" + file.getName());

										spreadsheet.setMaxrows(intbox.getValue() + 3);
										spreadsheet.setMaxcolumns(colS.getValue());
										ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

										South south = new South();
										south.setParent(borderlayout);

										Toolbar toolbar = new Toolbar();
										toolbar.setParent(south);
										MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup",
												"/img/cancel.gif");
										cancel.setTooltiptext("Tutup");
										cancel.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												window.detach();
											}
										});
										cancel.setParent(toolbar);

										MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data",
												"/img/excel.png");
										print.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												try {
													Filedownload.save(new FileInputStream(file),
															"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
															file.getName());
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanProsesHeper.java:718");
												}
											}
										});
										print.setParent(toolbar);

										window.setVisible(true);
										window.onModal();

										Clients.clearBusy();
										timer.detach();
									}
								} catch (Exception e) {
									Clients.clearBusy();
									toolbarbutton.setDisabled(false);
								}
							}
						});
						timer.start();

						try {
							Clients.showBusy(label.getValue());

							WORKER_EXECUTOR.execute(new Runnable() {
								@SuppressWarnings({ })
								@Override
								public void run() {
//									KegiatanHelper.prosestagihan = true;
									try {
										// PERUBAHAN: Menyiapkan Array dari JenisKegiatan untuk looping jika memilih
										// 'Semua'
										List<JenisKegiatan> listJk = new ArrayList<JenisKegiatan>();
										if (jenisKegiatanTerpilih != null) {
											listJk.add(jenisKegiatanTerpilih);
										} else {
											Session sessionJk = null;
											try {
												sessionJk = HibernateUtil.currentNativeSession();
												listJk = ConstantValues
														.simpleList(
																sessionJk.createCriteria(JenisKegiatan.class)
																		.add(Restrictions.eq("aktif", true)),
																JenisKegiatan.class);
											} finally {
												if (sessionJk != null && sessionJk.isOpen()) {
													sessionJk.disconnect();
													sessionJk.close();
												}
												HibernateUtil.closeSession();
											}
										}

										// Konfigurasi Kalkulasi Header Excel
										final int finalThn = Integer.parseInt(ta.split("/")[0]);
										final int finalThnSampai = Integer.parseInt(taSampai.split("/")[0]);
										int maxCols = 7;
										for (int tahun = finalThn; tahun <= finalThnSampai; tahun++) {
											if (GANJIL)
												maxCols += 3;
											if (GENAP)
												maxCols += 3;
										}
										maxCols += 5;
										final int finalMaxCols = maxCols;
										colS.setValue(finalMaxCols);

										// MULTI-THREADING SETUP
										ExecutorService executor = Executors.newFixedThreadPool(getSafeThreadPoolSize(0));
										List<Callable<Object[]>> tasks = new ArrayList<Callable<Object[]>>();
										final AtomicInteger progressCounter = new AtomicInteger(0);
										final long startTime = System.currentTimeMillis();

										// PERUBAHAN: Loop pada setiap JenisKegiatan (Berlaku untuk single & multiple)
										for (final JenisKegiatan j : listJk) {
											List<Mahasiswa> mahasiswas = new ArrayList<Mahasiswa>();
											List<BiodataCalonMahasiswa> biodataCalonMahasiswas = new ArrayList<BiodataCalonMahasiswa>();

											Session session = null;
											try {
												session = HibernateUtil.currentNativeSession();
												if ((ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null && j.getId()
														.equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId()))
														|| (ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
																&& j.getId().equals(
																		ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU
																				.getId()))) {

													Criterion mhsbaru = Restrictions.isNotNull("prodiLulus");
													if (fak != null) {
														mhsbaru = Restrictions.and(mhsbaru,
																Restrictions.eq("prodiLulus.fakultas", fak));
													}
													if (jur != null) {
														mhsbaru = Restrictions.and(mhsbaru,
																Restrictions.eq("prodiLulus", jur));
													}

													Criterion calonmhsbaru = Restrictions.sqlRestriction("true");
													if (fak != null) {
														Criterion calonmhsbaruOr = Restrictions.eq("prodi1.fakultas",
																fak);
														calonmhsbaruOr = Restrictions.or(calonmhsbaruOr,
																Restrictions.eq("prodi2.fakultas", fak));
														calonmhsbaruOr = Restrictions.or(calonmhsbaruOr,
																Restrictions.eq("prodi3.fakultas", fak));
														calonmhsbaruOr = Restrictions.or(calonmhsbaruOr,
																Restrictions.eq("prodi4.fakultas", fak));
														calonmhsbaruOr = Restrictions.or(calonmhsbaruOr,
																Restrictions.eq("prodi5.fakultas", fak));
														calonmhsbaruOr = Restrictions.or(calonmhsbaruOr,
																Restrictions.eq("prodiLulus.fakultas", fak));
														calonmhsbaru = Restrictions.and(calonmhsbaru, calonmhsbaruOr);
													}
													if (jur != null) {
														Criterion calonmhsbaruOr = Restrictions.eq("prodi1", jur);
														calonmhsbaruOr = Restrictions.or(calonmhsbaruOr,
																Restrictions.eq("prodi2", jur));
														calonmhsbaruOr = Restrictions.or(calonmhsbaruOr,
																Restrictions.eq("prodi3", jur));
														calonmhsbaruOr = Restrictions.or(calonmhsbaruOr,
																Restrictions.eq("prodi4", jur));
														calonmhsbaruOr = Restrictions.or(calonmhsbaruOr,
																Restrictions.eq("prodi5", jur));
														calonmhsbaruOr = Restrictions.or(calonmhsbaruOr,
																Restrictions.eq("prodiLulus", jur));
														calonmhsbaru = Restrictions.and(calonmhsbaru, calonmhsbaruOr);
													}

													biodataCalonMahasiswas = ConstantValues.simpleList(
															session.createCriteria(BiodataCalonMahasiswa.class)
																	.add(Restrictions.or(
																			Restrictions.isNull("aktif"),
																			Restrictions.eq("aktif", true)))
																	.add(mhs.trim().isEmpty()
																			? Restrictions.sqlRestriction("true")
																			: Restrictions.or(
																					Restrictions.ilike("nama", mhs,
																							MatchMode.ANYWHERE),
																					Restrictions.or(
																							Restrictions.ilike(
																									"noRegistrasi", mhs,
																									MatchMode.ANYWHERE),
																							Restrictions.ilike(
																									"noUjian", mhs,
																									MatchMode.ANYWHERE))))
																	.createAlias("prodiLulus", "prodiLulus",
																			Criteria.LEFT_JOIN)
																	.createAlias("prodi1", "prodi1", Criteria.LEFT_JOIN)
																	.createAlias("prodi2", "prodi2", Criteria.LEFT_JOIN)
																	.createAlias("prodi3", "prodi3", Criteria.LEFT_JOIN)
																	.createAlias("prodi4", "prodi4", Criteria.LEFT_JOIN)
																	.createAlias("prodi5", "prodi5", Criteria.LEFT_JOIN)
																	.add(Restrictions.between("tahunAkademik", ta,
																			taSampai))
																	.add((ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
																			&& j.getId().equals(
																					ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU
																							.getId())) ? mhsbaru
																									: calonmhsbaru),
															BiodataCalonMahasiswa.class);
												} else {
													Criteria c = session.createCriteria(Mahasiswa.class)
															.add(Restrictions.or(Restrictions.isNull("aktif"),
																	Restrictions.eq("aktif", true)));

													if (status != null) {
														c = session.createCriteria(HistoryStatusMahasiswa.class)
																.add(j != null && j.getUntukBayarSP()
																		? Restrictions.eq("sp",
																				Perkuliahan.SEMESTER_PENDEK)
																		: Restrictions.isNull("sp"))
																		.add(Restrictions.eq("tahunAkademik", taStatus))
																		.add(Restrictions.eq("ganjilGenap", jenisSmtStatus))
																.setProjection(Projections.groupProperty("mahasiswa"))
																.createCriteria("mahasiswa");
													}

													mahasiswas = ConstantValues.simpleList(c.add(mhs.trim().isEmpty()
															? Restrictions.sqlRestriction("true")
															: Restrictions.or(
																	Restrictions.ilike("nim", mhs, MatchMode.ANYWHERE),
																	Restrictions.ilike("nama", mhs,
																			MatchMode.ANYWHERE)))
															.add(Restrictions.between("tahunangkatan", ang, angSampai))
															.add(jur == null ? Restrictions.sqlRestriction("true")
																	: Restrictions.eq("jurusan", jur))
															.createAlias("jurusan", "jurusan")
															.add(fak == null ? Restrictions.sqlRestriction("true")
																	: Restrictions.eq("jurusan.fakultas", fak)),
															Mahasiswa.class);
												}
											} finally {
												if (session != null && session.isOpen()) {
													session.disconnect();
													session.close();
												}
												HibernateUtil.closeSession();
											}

											// TASK UNTUK MAHASISWA
											for (final Mahasiswa mahasiswaData : mahasiswas) {
												tasks.add(new Callable<Object[]>() {
													@Override
													public Object[] call() throws Exception {
														Object[] rowData = new Object[finalMaxCols];
														rowData[0] = mahasiswaData.getNim();
														rowData[1] = mahasiswaData.getNama();
														rowData[2] = j.getNamaKegiatan();
														rowData[3] = mahasiswaData.getJurusan().getFakultas().getNama();
														rowData[4] = mahasiswaData.getJurusan().getNama();
														rowData[5] = mahasiswaData.getStatusAwalMahasiswa() == null ? ""
																: mahasiswaData.getStatusAwalMahasiswa().getNama();
														rowData[6] = mahasiswaData.getTahunangkatan();

														StringBuilder d = new StringBuilder();
														int m = 7;
														Double dibayar = 0.0;
														Double terhutang = 0.0;
														Session s = null;

														try {
															s = HibernateUtil.currentNativeSession();

															for (int tahun = finalThn; tahun <= finalThnSampai; tahun++) {
																if (mahasiswaData.getTahunangkatan() <= tahun) {
																	String Ta = tahun + "/" + (tahun + 1);

																	if (GANJIL) {
																		Integer smt = Common.getSemester(
																				mahasiswaData.getTahunangkatan(), Ta,
																				Perkuliahan.GANJIL,
																				mahasiswaData
																						.getPindahKeKampusIniMasukSemester(),
																				mahasiswaData.getSemesterMulai());
																		if (smt >= j.getMinSmt() && smt <= j.getMaxSmt()) {
																			Kegiatan kegiatan = KegiatanHelper
																					.checkKegiatanMahasiswa(j,
																							mahasiswaData, smt, Ta,
																							ulang, rst, item, s);
																			if (kegiatan != null) {
																				if (bersihkanMassalDijalankan) {
																					int jumlahDihapusIni = ais.action.master.helper.KegiatanPersistenceHelper.bersihkanItemAsing(kegiatan);
																					if (jumlahDihapusIni > 0) {
																						totalItemAsingDibersihkan.addAndGet(jumlahDihapusIni);
																						// Hitung ulang setelah pembersihan agar angka tagihan yg dilaporkan sudah bersih.
																						Kegiatan kegiatanBaru = KegiatanHelper.checkKegiatanMahasiswa(j, mahasiswaData, smt, Ta, ulang, rst, item, s);
																				if (kegiatanBaru != null) { kegiatan = kegiatanBaru; }
																					}
																				}
																				if (d.length() > 0)
																					d.append("\r\n");
																				d.append(kegiatan.getTagihans());
																				rowData[m++] = kegiatan.getAmount()
																						+ kegiatan.getAmountTerhutang();
																				rowData[m++] = kegiatan.getAmount();
																				rowData[m++] = kegiatan
																						.getAmountTerhutang();
																				dibayar += kegiatan.getAmount();
																				terhutang += kegiatan
																						.getAmountTerhutang();
																				// STATUS KEAKTIFAN IKUT DIPERBARUI MASSAL (keluhan UBT): dulu status
																				// Aktif/Nonaktif hanya dihitung ulang saat layar per-mahasiswa dibuka,
																				// sehingga hasil proses tagihan massal tidak mengubah status siapa pun.
																				try {
																					ais.action.master.helper.HistoryStatusMahasiswaUtil
																							.currentStatus(mahasiswaData, Ta, smt, true);
																				} catch (Exception eStatus) {
																					ais.common.ErrorAuditUtil.record(eStatus,
																							"auto-audit KegiatanProsesHeper: gagal perbarui status keaktifan massal");
																				}
																			} else {
																				rowData[m++] = "";
																				rowData[m++] = "";
																				rowData[m++] = "";
																			}
																		} else {
																			rowData[m++] = "";
																			rowData[m++] = "";
																			rowData[m++] = "";
																		}
																	}

																	if (GENAP) {
																		Integer smt = Common.getSemester(
																				mahasiswaData.getTahunangkatan(), Ta,
																				Perkuliahan.GENAP,
																				mahasiswaData
																						.getPindahKeKampusIniMasukSemester(),
																				mahasiswaData.getSemesterMulai());
																		if (smt >= j.getMinSmt() && smt <= j.getMaxSmt()) {
																			Kegiatan kegiatan = KegiatanHelper
																					.checkKegiatanMahasiswa(j,
																							mahasiswaData, smt, Ta,
																							ulang, rst, item, s);
																			if (kegiatan != null) {
																				if (bersihkanMassalDijalankan) {
																					int jumlahDihapusIni = ais.action.master.helper.KegiatanPersistenceHelper.bersihkanItemAsing(kegiatan);
																					if (jumlahDihapusIni > 0) {
																						totalItemAsingDibersihkan.addAndGet(jumlahDihapusIni);
																						// Hitung ulang setelah pembersihan agar angka tagihan yg dilaporkan sudah bersih.
																						Kegiatan kegiatanBaru = KegiatanHelper.checkKegiatanMahasiswa(j, mahasiswaData, smt, Ta, ulang, rst, item, s);
																				if (kegiatanBaru != null) { kegiatan = kegiatanBaru; }
																					}
																				}
																				if (d.length() > 0)
																					d.append("\r\n");
																				d.append(kegiatan.getTagihans());
																				rowData[m++] = kegiatan.getAmount()
																						+ kegiatan.getAmountTerhutang();
																				rowData[m++] = kegiatan.getAmount();
																				rowData[m++] = kegiatan
																						.getAmountTerhutang();
																				dibayar += kegiatan.getAmount();
																				terhutang += kegiatan
																						.getAmountTerhutang();
																				// STATUS KEAKTIFAN IKUT DIPERBARUI MASSAL (keluhan UBT): dulu status
																				// Aktif/Nonaktif hanya dihitung ulang saat layar per-mahasiswa dibuka,
																				// sehingga hasil proses tagihan massal tidak mengubah status siapa pun.
																				try {
																					ais.action.master.helper.HistoryStatusMahasiswaUtil
																							.currentStatus(mahasiswaData, Ta, smt, true);
																				} catch (Exception eStatus) {
																					ais.common.ErrorAuditUtil.record(eStatus,
																							"auto-audit KegiatanProsesHeper: gagal perbarui status keaktifan massal");
																				}
																			} else {
																				rowData[m++] = "";
																				rowData[m++] = "";
																				rowData[m++] = "";
																			}
																		} else {
																			rowData[m++] = "";
																			rowData[m++] = "";
																			rowData[m++] = "";
																		}
																	}
																} else {
																	if (GANJIL) {
																		rowData[m++] = "";
																		rowData[m++] = "";
																		rowData[m++] = "";
																	}
																	if (GENAP) {
																		rowData[m++] = "";
																		rowData[m++] = "";
																		rowData[m++] = "";
																	}
																}
															}
														} catch (Exception e) {
															e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KegiatanProsesHeper.java:1029");
														} finally {
															if (s != null && s.isOpen()) {
																s.disconnect();
																s.close();
															}
															HibernateUtil.closeSession();
														}

														rowData[m + 0] = (terhutang + dibayar);
														rowData[m + 1] = dibayar;
														rowData[m + 2] = terhutang;
														double persentaseLunas = ((dibayar + terhutang) == 0) ? 0.0
																: ((dibayar * 100.0) / (dibayar + terhutang));

														DecimalFormat df = new DecimalFormat("#.##");
														rowData[m + 3] = df.format(persentaseLunas) + "%";
														rowData[m + 4] = d.toString();

														int currProgress = progressCounter.incrementAndGet();
														// Pengamanan untuk UI Thread
														final int totalTasks = colS.getAttribute("tsize") != null
																? (Integer) colS.getAttribute("tsize")
																: 1;
														long elapsed = System.currentTimeMillis() - startTime;
														long etaMs = (currProgress > 0)
																? (elapsed / currProgress) * (totalTasks - currProgress)
																: 0;
														long etaMenit = (etaMs / 1000) / 60;
														long etaDetik = (etaMs / 1000) % 60;

														label.setValue("Proses data " + currProgress + "/" + totalTasks
																+ " (" + df.format(currProgress * 100.0 / totalTasks)
																+ " %) " + "- ETA: " + etaMenit + "m " + etaDetik
																+ "s");

														return rowData;
													}
												});
											}

											// TASK UNTUK CALON MAHASISWA
											for (final BiodataCalonMahasiswa mhsCalon : biodataCalonMahasiswas) {
												tasks.add(new Callable<Object[]>() {
													@Override
													public Object[] call() throws Exception {
														Object[] rowData = new Object[finalMaxCols];
														Jurusan jurusanData = mhsCalon.getProdiLulus() == null
																? mhsCalon.getProdi1()
																: mhsCalon.getProdiLulus();

														rowData[0] = mhsCalon.getNoRegistrasi();
														rowData[1] = mhsCalon.getNama();
														rowData[2] = j.getNamaKegiatan();
														rowData[3] = jurusanData == null ? ""
																: jurusanData.getFakultas().getNama();
														rowData[4] = jurusanData == null ? "" : jurusanData.getNama();
														rowData[5] = mhsCalon.getStatusAwalMahasiswa() == null ? ""
																: mhsCalon.getStatusAwalMahasiswa().getNama();
														rowData[6] = mhsCalon.getTahun();

														StringBuilder d = new StringBuilder();
														int m = 7;
														Double dibayar = 0.0;
														Double terhutang = 0.0;
														Session s = null;

														try {
															s = HibernateUtil.currentNativeSession();

															for (int tahun = finalThn; tahun <= finalThnSampai; tahun++) {
																String Ta = tahun + "/" + (tahun + 1);

																if (mhsCalon.getTahun() != null
																		&& mhsCalon.getTahun().equals(tahun)) {
																	if (GANJIL) {
																		int smt = (ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
																				&& j.getId().equals(
																						ConstantValues.PENDAFTARAN_CALON_MAHASISWA
																								.getId())) ? 0 : 1;
																		Kegiatan kegiatan = KegiatanHelper
																				.checkKegiatanCalonMahasiswa(j,
																						mhsCalon, smt, Ta, ulang, rst,
																						item, s);

																		if (kegiatan != null) {
																			if (bersihkanMassalDijalankan) {
																				int jumlahDihapusIni = ais.action.master.helper.KegiatanPersistenceHelper.bersihkanItemAsing(kegiatan);
																				if (jumlahDihapusIni > 0) {
																					totalItemAsingDibersihkan.addAndGet(jumlahDihapusIni);
																					// Hitung ulang setelah pembersihan agar angka tagihan yg dilaporkan sudah bersih.
																					Kegiatan kegiatanBaru = KegiatanHelper.checkKegiatanCalonMahasiswa(j, mhsCalon, smt, Ta, ulang, rst, item, s);
																			if (kegiatanBaru != null) { kegiatan = kegiatanBaru; }
																				}
																			}
																			if (d.length() > 0)
																				d.append("\r\n");
																			d.append(kegiatan.getTagihans());
																			rowData[m++] = kegiatan.getAmount()
																					+ kegiatan.getAmountTerhutang();
																			rowData[m++] = kegiatan.getAmount();
																			rowData[m++] = kegiatan
																					.getAmountTerhutang();
																			dibayar += kegiatan.getAmount();
																			terhutang += kegiatan.getAmountTerhutang();
																		} else {
																			rowData[m++] = "";
																			rowData[m++] = "";
																			rowData[m++] = "";
																		}
																	}

																	if (GENAP) {
																		int smt = (ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
																				&& j.getId().equals(
																						ConstantValues.PENDAFTARAN_CALON_MAHASISWA
																								.getId())) ? 0 : 2;
																		Kegiatan kegiatan = KegiatanHelper
																				.checkKegiatanCalonMahasiswa(j,
																						mhsCalon, smt, Ta, ulang, rst,
																						item, s);

																		if (kegiatan != null) {
																			if (bersihkanMassalDijalankan) {
																				int jumlahDihapusIni = ais.action.master.helper.KegiatanPersistenceHelper.bersihkanItemAsing(kegiatan);
																				if (jumlahDihapusIni > 0) {
																					totalItemAsingDibersihkan.addAndGet(jumlahDihapusIni);
																					// Hitung ulang setelah pembersihan agar angka tagihan yg dilaporkan sudah bersih.
																					Kegiatan kegiatanBaru = KegiatanHelper.checkKegiatanCalonMahasiswa(j, mhsCalon, smt, Ta, ulang, rst, item, s);
																			if (kegiatanBaru != null) { kegiatan = kegiatanBaru; }
																				}
																			}
																			if (d.length() > 0)
																				d.append("\r\n");
																			d.append(kegiatan.getTagihans());
																			rowData[m++] = kegiatan.getAmount()
																					+ kegiatan.getAmountTerhutang();
																			rowData[m++] = kegiatan.getAmount();
																			rowData[m++] = kegiatan
																					.getAmountTerhutang();
																			dibayar += kegiatan.getAmount();
																			terhutang += kegiatan.getAmountTerhutang();
																		} else {
																			rowData[m++] = "";
																			rowData[m++] = "";
																			rowData[m++] = "";
																		}
																	}
																} else {
																	if (GANJIL) {
																		rowData[m++] = "";
																		rowData[m++] = "";
																		rowData[m++] = "";
																	}
																	if (GENAP) {
																		rowData[m++] = "";
																		rowData[m++] = "";
																		rowData[m++] = "";
																	}
																}
															}
														} catch (Exception e) {
															e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KegiatanProsesHeper.java:1173");
														} finally {
															if (s != null && s.isOpen()) {
																s.disconnect();
																s.close();
															}
															HibernateUtil.closeSession();
														}

														rowData[m + 0] = (terhutang + dibayar);
														rowData[m + 1] = dibayar;
														rowData[m + 2] = terhutang;
														double persentaseLunas = ((dibayar + terhutang) == 0) ? 0.0
																: ((dibayar * 100.0) / (dibayar + terhutang));

														DecimalFormat df = new DecimalFormat("#.##");
														rowData[m + 3] = df.format(persentaseLunas) + "%";
														rowData[m + 4] = d.toString();

														int currProgress = progressCounter.incrementAndGet();
														final int totalTasks = colS.getAttribute("tsize") != null
																? (Integer) colS.getAttribute("tsize")
																: 1;
														long elapsed = System.currentTimeMillis() - startTime;
														long etaMs = (currProgress > 0)
																? (elapsed / currProgress) * (totalTasks - currProgress)
																: 0;
														long etaMenit = (etaMs / 1000) / 60;
														long etaDetik = (etaMs / 1000) % 60;

														label.setValue("Proses data " + currProgress + "/" + totalTasks
																+ " (" + df.format(currProgress * 100.0 / totalTasks)
																+ " %) " + "- ETA: " + etaMenit + "m " + etaDetik
																+ "s");

														return rowData;
													}
												});
											}
										} // Selesai Loop untuk ListJk

										final int totalSize = tasks.size();
										intbox.setValue(totalSize);
										colS.setAttribute("tsize", totalSize);

										// Eksekusi Semua Proses Secara Paralel
										List<Future<Object[]>> futures = executor.invokeAll(tasks);
										executor.shutdown();

										// MERAKIT DAN MENULIS FILE EXCEL (Thread Utama)
										XSSFWorkbook workbook = new XSSFWorkbook();
										XSSFSheet sheet = workbook.createSheet("DATA TAGIHAN");
										sheet.setDefaultColumnWidth(20);
										XSSFCellStyle lockedNumericStyle = workbook.createCellStyle();
										lockedNumericStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
										lockedNumericStyle.setFillForegroundColor(new XSSFColor(Color.RED));
										lockedNumericStyle.setLocked(true);

										XSSFRow rowhead = sheet.createRow((short) 0);
										rowhead.createCell(0).setCellValue("NIM/NO REG");
										rowhead.createCell(1).setCellValue("NAMA");
										rowhead.createCell(2).setCellValue("JENIS PEMBAYARAN");
										rowhead.createCell(3).setCellValue(Common.getBahasaConfig("FAKULTAS"));
										rowhead.createCell(4).setCellValue(Common.getBahasaConfig("JURUSAN"));
										rowhead.createCell(5).setCellValue("STATUS AWAL");
										rowhead.createCell(6).setCellValue("ANGKATAN");

										int m = 7;
										for (int tahun = finalThn; tahun <= finalThnSampai; tahun++) {
											if (GANJIL) {
												rowhead.createCell(m++).setCellValue("Tag. " + tahun + "1");
												rowhead.createCell(m++).setCellValue("Byr. " + tahun + "1");
												rowhead.createCell(m++).setCellValue("Sisa " + tahun + "1");
											}
											if (GENAP) {
												rowhead.createCell(m++).setCellValue("Tag. " + tahun + "2");
												rowhead.createCell(m++).setCellValue("Byr " + tahun + "2");
												rowhead.createCell(m++).setCellValue("Sisa " + tahun + "2");
											}
										}

										rowhead.createCell(m + 0).setCellValue("TAGIHAN");
										rowhead.createCell(m + 1).setCellValue("DIBAYAR");
										rowhead.createCell(m + 2).setCellValue("SISA");
										rowhead.createCell(m + 3).setCellValue("PROSENTASE");
										rowhead.createCell(m + 4).setCellValue("RINCIAN");

										int rowIndex = 1;
										Double totalTagihan = 0.0;
										Double totalDibayar = 0.0;
										Double totalTunggakan = 0.0;

										// Ambil hasil dari tiap thread dan masukan ke Excel
										for (Future<Object[]> future : futures) {
											Object[] rowData = future.get();
											XSSFRow row = sheet.createRow(rowIndex++);

											for (int i = 0; i < finalMaxCols; i++) {
												Object val = rowData[i];
												XSSFCell cell = row.createCell(i);
												if (val == null) {
													cell.setCellValue("");
												} else if (val instanceof String) {
													cell.setCellValue((String) val);
												} else if (val instanceof Double) {
													cell.setCellValue((Double) val);
												} else if (val instanceof Integer) {
													cell.setCellValue((Integer) val);
												} else {
													cell.setCellValue(val.toString());
												}
											}

											// Kalkulasi Grand Total (Index statis dari batas kanan array)
											totalTagihan += nilaiDouble(rowData[finalMaxCols - 5]);
											totalDibayar += nilaiDouble(rowData[finalMaxCols - 4]);
											totalTunggakan += nilaiDouble(rowData[finalMaxCols - 3]);
										}

										XSSFRow rowTotal = sheet.createRow(rowIndex);
										rowTotal.createCell(4).setCellValue("Total Semua");

										DecimalFormat dfTotal = new DecimalFormat("#.##");
										rowTotal.createCell(finalMaxCols - 5)
												.setCellValue(dfTotal.format(totalTagihan));
										rowTotal.createCell(finalMaxCols - 4)
												.setCellValue(dfTotal.format(totalDibayar));
										rowTotal.createCell(finalMaxCols - 3)
												.setCellValue(dfTotal.format(totalTunggakan));

										double sumPersen = (totalTagihan == 0) ? 0
												: (totalDibayar * 100.0) / totalTagihan;
										rowTotal.createCell(finalMaxCols - 2)
												.setCellValue(dfTotal.format(sumPersen) + "%");

										try {
											FileOutputStream fileOut = new FileOutputStream(filename);
											workbook.write(fileOut);
											fileOut.close();
										} catch (IOException e) {
											Common.tampilErrorJikaAdmin(e);
										}

										label.setValue("");

										if (bersihkanMassalDijalankan) {
											MyMessageboxConfig.show(
													"Proses \"Bersihkan Item Tak Sesuai (Massal)\" selesai. Total item tagihan yang berhasil dihapus (belum dibayar & tidak sesuai Setting Biaya yang berlaku saat ini): "
															+ totalItemAsingDibersihkan.get()
															+ " baris, tersebar di seluruh mahasiswa yang cocok dengan filter di atas. Item yang sudah ada pembayarannya TIDAK ikut terhapus. Rincian tagihan per mahasiswa (setelah dibersihkan) dapat dilihat di file Excel yang akan tampil setelah ini.",
													"Bersihkan Massal Selesai", MyMessageboxConfig.OK,
													MyMessageboxConfig.INFORMATION);
										}

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										label.setValue("-");
									} finally {
//										KegiatanHelper.prosestagihan = false;
									}
								}
							});

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}
				});
				save.setParent(toolbar);
				window.onModal();
			}
		});

		return toolbarbutton;
	}

	/**
	 * Varian dengan tahun angkatan tetap — cocok untuk dasbor yang sudah punya filter tahun aktif.
	 * Tahun Angkatan Mulai dan Sampai di-set ke {@code defaultTahunAngkatan} dan di-disabled
	 * sehingga operator tidak perlu (dan tidak bisa) mengubahnya.
	 *
	 * @param defaultTahunAngkatan tahun angkatan yang akan diisi otomatis; jika 0 atau negatif,
	 *        perilaku sama dengan overload 4-param (range default)
	 */
	public static MyToolbarbuttonConfig prosesUlangTagihan(String buttonLabel, String buttonImage,
			final JenisKegiatan jenisPembayaranDefault, final boolean lockJenis,
			final int defaultTahunAngkatan) {
		MyToolbarbuttonConfig btn = prosesUlangTagihan(buttonLabel, buttonImage,
				jenisPembayaranDefault, lockJenis);
		if (defaultTahunAngkatan > 0) {
			btn.setAttribute("defaultTahunAngkatan", Integer.valueOf(defaultTahunAngkatan));
		}
		return btn;
	}

	/**
	 * Fitur "Proses Surat Tagihan": varian dari {@link #prosesUlangTagihan} yang alih-alih hanya
	 * merekap tagihan, MEMBUAT SURAT TAGIHAN PDF per mahasiswa/calon mahasiswa yang tagihannya
	 * belum lunas dan (opsional, checkbox "Kirim Email Surat Tagihan") MENGIRIMKANNYA lewat email.
	 * Popup filter mirip {@link #prosesUlangTagihan} PLUS field khusus surat: Tanggal Surat, Tanggal
	 * Jatuh Tempo (default besok), Nomor Surat, Cara Pembayaran ({@link JenisPembayaran}, di-scope
	 * ke {@link SatuanKerja} pengguna login bila ada), dan Prosentase Denda. Jenis Pembayaran WAJIB
	 * dipilih (tidak boleh "Semua" — divalidasi sebelum proses dimulai).
	 * <p>
	 * Alur di {@link #WORKER_EXECUTOR}: untuk setiap mahasiswa/calon mahasiswa × tahun×semester
	 * yang cocok filter, {@link Callable} paralel memanggil
	 * {@code KegiatanHelper.checkKegiatanMahasiswa}/{@code checkKegiatanCalonMahasiswa} lalu method
	 * lokal {@code kirim(...)} (didefinisikan sebagai method privat di dalam
	 * {@link Runnable}/task ini): menghitung sisa tagihan lewat
	 * {@code CommonReportHelper.populateMapTagihanMahasiswa}, menambah satu baris ke Excel
	 * ringkasan, dan — HANYA bila sisa {@code > 0.1} — membuat PDF surat
	 * ({@code Report.generateFileReport}, template {@code Surat_Tagihan_Mahasiswa}/
	 * {@code Surat_Tagihan}) serta mengirim email berlampiran PDF lewat
	 * {@link MailSender#sendMailLampiranTagihan} bila mahasiswa punya alamat email dan checkbox
	 * kirim dicentang. Hasil tiap task dikumpulkan ke {@link SuratResult} lokal (thread-safe by
	 * design, tanpa shared mutable state antar task), digabung oleh thread utama setelah
	 * {@code executor.invokeAll} selesai menjadi satu Excel ringkasan + kumpulan PDF, dikemas
	 * bersama jadi satu file ZIP ({@code Common.createZip}) untuk diunduh.
	 *
	 * @param buttonLabel label tombol
	 * @param buttonImage path ikon tombol
	 * @return tombol toolbar siap dipasang ke UI
	 */
	public static MyToolbarbuttonConfig prosesSuratTagihan(String buttonLabel, String buttonImage) {

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(buttonLabel, buttonImage);

		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final MyWindow window = new MyWindow("Pilih Tahun Akademik, Angkatan, dan Jenis Pembayaran", "none",
						true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("95%");
				window.setWidth("600px");
				final Combobox tahunAkademik = new Combobox();
				Common.generateTahunAjaran(tahunAkademik);

				final Combobox tahunAkademikSampai = new Combobox();
				Common.generateTahunAjaran(tahunAkademikSampai);

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);

				Center center = new Center();
				center.setParent(borderlayout);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
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

				final Combobox fakultas;
				final Combobox jurusan;
				fakultas = new Combobox();
				jurusan = new Combobox();
				Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
				row.appendChild(fakultas);
				fakultas.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
				row.appendChild(jurusan);
				jurusan.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa"));
				final MyTextbox mahasiswa;
				row.appendChild(mahasiswa = new MyTextbox());
				mahasiswa.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig hitungUlang;
				row.appendChild(hitungUlang = new MyCheckboxConfig("Hitung Ulang Tagihan"));
				hitungUlang.setChecked(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig kirimEmail;
				row.appendChild(kirimEmail = new MyCheckboxConfig("Kirim Email Surat Tagihan"));
				kirimEmail.setChecked(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik Mulai *"));
				row.appendChild(tahunAkademik);
				tahunAkademik.setWidth("90%");
				tahunAkademik.setReadonly(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik Sampai *"));
				row.appendChild(tahunAkademikSampai);
				tahunAkademikSampai.setWidth("90%");
				tahunAkademikSampai.setReadonly(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pembayaran *"));

				final Combobox jenisPembayaran;
				row.appendChild(jenisPembayaran = new Combobox());
				jenisPembayaran.setWidth("90%");
				jenisPembayaran.setReadonly(true);
				Common.insertCombo(jenisPembayaran, "namaKegiatan", JenisKegiatan.class,
						Restrictions.eq("aktif", true));
				Common.selectComboItem(jenisPembayaran, ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan Mulai *"));
				final Intbox angkatan;
				row.appendChild(angkatan = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 5));
				angkatan.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan Sampai *"));
				final Intbox angkatanSampai;
				row.appendChild(angkatanSampai = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)));
				angkatanSampai.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig gnj;
				row.appendChild(gnj = new MyCheckboxConfig("Ganjil"));
				gnj.setChecked(Common.isNowSemensterGanjil());

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig gnp;
				row.appendChild(gnp = new MyCheckboxConfig("Genap"));
				gnp.setChecked(!Common.isNowSemensterGanjil());

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Status Mahasiswa"));
				final Combobox statusMahasiswa;
				final Combobox statusTahunAkademik = new Combobox();
				Hbox hbox = new Hbox();
				hbox.setParent(row);
				hbox.appendChild(statusMahasiswa = new Combobox());
				hbox.appendChild(statusTahunAkademik);
				Common.insertComboDanSemua(statusMahasiswa, "nama", StatusMahasiswa.class);
				Common.generateTahunAjaran(statusTahunAkademik);
				statusMahasiswa.setCols(3);
				statusTahunAkademik.setCols(4);

				final Combobox genapGanjil = new Combobox();
				hbox.appendChild(genapGanjil);
				org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
				comboitem.setLabel(Perkuliahan.GENAP);
				comboitem.setValue(Perkuliahan.GENAP);
				genapGanjil.appendChild(comboitem);
				comboitem = new MyComboitemConfig();
				comboitem.setLabel(Perkuliahan.GANJIL);
				comboitem.setValue(Perkuliahan.GANJIL);
				genapGanjil.appendChild(comboitem);

				Common.selectComboItem(genapGanjil,
						Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
				genapGanjil.setReadonly(true);
				genapGanjil.setCols(3);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Surat"));
				final MyDatebox tanggal;
				row.appendChild(tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
				tanggal.setReadonly(true);

				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Jatuh Tempo"));
				final MyDatebox tanggalJatuhTempo;
				row.appendChild(tanggalJatuhTempo = new MyDatebox(new Date()));
				tanggalJatuhTempo.setReadonly(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Surat"));
				final MyTextbox nomor;
				row.appendChild(nomor = new MyTextbox(""));
				nomor.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Cara Pembayaran"));
				final Combobox caraPembayaran;
				row.appendChild(caraPembayaran = new Combobox());
				SatuanKerja satuanKerja = Common.getSatuanKerja();
				Common.insertComboDanSemua(caraPembayaran, new String[] { "nama", "bank" }, "akun",
						JenisPembayaran.class, "== Tidak menggunakan cara pembayaran ==",
						Restrictions.and(
								satuanKerja == null ? Restrictions.sqlRestriction("true")
										: Restrictions.or(Restrictions.isNull("satuanKerja"),
												Restrictions.eq("satuanKerja", satuanKerja)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Prosentase Denda"));
				final MyDoublebox denda;

				row.appendChild(denda = new MyDoublebox(0.0));
				denda.setWidth("90%");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig reset;
				row.appendChild(reset = new MyCheckboxConfig("Reset Tagihan Kembali ke Billing"));

				South south = new South();
				ais.ui.util.ZkCompat.setFlex(south, true);
				south.setParent(borderlayout);

				Toolbar toolbar = new Toolbar();
				toolbar.setParent(south);
				MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
				cancel.setTooltiptext("Tutup");
				cancel.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						window.detach();
					}
				});
				cancel.setParent(toolbar);

				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Proses Surat Tagihan", "/img/save.gif");
				save.setTooltiptext("Proses");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						final String ta = (String) tahunAkademik.getSelectedItem().getValue();
						final String taSampai = (String) tahunAkademikSampai.getSelectedItem().getValue();
						final Integer ang = angkatan.getValue() == null ? 0 : angkatan.getValue();
						final Integer angSampai = angkatanSampai.getValue() == null ? 0 : angkatanSampai.getValue();
						final JenisKegiatan j = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
								: jenisPembayaran.getSelectedItem().getValue());

						final Fakultas fak = (Fakultas) (fakultas.getSelectedItem() == null ? null
								: fakultas.getSelectedItem().getValue());
						final Jurusan jur = (Jurusan) (jurusan.getSelectedItem() == null ? null
								: jurusan.getSelectedItem().getValue());

						final StatusMahasiswa status = (StatusMahasiswa) (statusMahasiswa.getSelectedItem() == null
								? null
								: statusMahasiswa.getSelectedItem().getValue());
						final String taStatus = (String) statusTahunAkademik.getSelectedItem().getValue();
						final String jenisSmtStatus = (String) genapGanjil.getSelectedItem().getValue();

						final String mhs = mahasiswa.getValue().trim();
						final Boolean ulang = hitungUlang.isChecked();
						final Boolean kirim = kirimEmail.isChecked();

						final boolean GANJIL = gnj.isChecked();
						final boolean GENAP = gnp.isChecked();

						final boolean rst = reset.isChecked();

						if (j == null) {
							MyMessageboxConfig.show("Jenis Pembayaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return;
						}
						window.detach();

						final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
						final Intbox intbox = new Intbox(10);
						Clients.showBusy(label.getValue());

						final String filenameexcel = Sessions.getCurrent().getWebApp()
								.getRealPath("/tmp/pembayaran_mahasiswa_" + URLEncoder.encode(
										Common.dateFormat7.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx");

						final String filename = Sessions.getCurrent().getWebApp()
								.getRealPath("/tmp/cetak_data_" + URLEncoder.encode(
										Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
										+ ".zip");

						final List<File> filenames = new ArrayList<File>();
						final List<String> filesName = new ArrayList<String>();

						final File file = new File(filename);
						file.createNewFile();

						final Timer timer = new Timer(200);
						timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						timer.setRepeats(true);
						timer.addEventListener("onTimer", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								try {
									Clients.showBusy(label.getValue());
									if (label.getValue().trim().equalsIgnoreCase("-")) {
										Clients.clearBusy();
										timer.detach();
									} else if (label.getValue().isEmpty()) {

										filesName.add("hasil_surat_tagihan.xlsx");
										filenames.add(new File(filenameexcel));

										Common.createZip(filesName, filenames, file);

										try {
											Filedownload.save(new FileInputStream(file), "application/zip",
													file.getName());
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanProsesHeper.java:1673");
										}

										Clients.clearBusy();
										timer.detach();

										filesName.clear();
										filenames.clear();
									}
								} catch (Exception e) {
									Clients.clearBusy();
								}
							}
						});
						timer.start();

						// Menyiapkan Base Parameter secara Thread-Safe untuk proses paralel
						final Map<String, Object> baseParameters = new HashMap<String, Object>();
						baseParameters.put("biodata_id", mahasiswa.getId());
						baseParameters.put("tanggal", tanggal.getValue());
						baseParameters.put("nomor", nomor.getValue());
						baseParameters.put("tahunakademik", ta); // Pass string yang benar, bukan combo object
						baseParameters.put("tanggalJatuhTempo", tanggalJatuhTempo.getValue());

						if (caraPembayaran.getSelectedItem() != null
								&& caraPembayaran.getSelectedItem().getValue() != null) {
							JenisPembayaran pilihanJenisPembayaran = (JenisPembayaran) caraPembayaran.getSelectedItem()
									.getValue();
							if (pilihanJenisPembayaran.getBank() != null) {
								baseParameters.put("rekening_pembayaran",
										" melalui Bank " + pilihanJenisPembayaran.getBank().getNama());
							} else {
								baseParameters.put("rekening_pembayaran",
										" melalui  " + pilihanJenisPembayaran.getNama());
							}
						}
						baseParameters.put("prosentaseDenda", denda.getValue());

						try {
							Clients.showBusy(label.getValue());

							WORKER_EXECUTOR.execute(new Runnable() {

								/**
								 * Memproses SATU {@link Kegiatan} (mahasiswa ATAU calon mahasiswa, dibedakan
								 * dari {@code kegiatan.getMahasiswa()}/{@code getCalonMahasiswa()}): menghitung
								 * sisa tagihan &amp; menambah baris ke {@code localResult.excelRows}, dan bila
								 * sisa {@code > 0.1} membuat PDF surat tagihan + (opsional) mengirim email
								 * lampiran, hasilnya ditambahkan ke {@code localResult.files}/{@code fileNames}.
								 * Method instance TANPA state bersama antar panggilan (semua parameter dioper
								 * eksplisit) sehingga aman dipanggil dari banyak thread task paralel sekaligus.
								 *
								 * @param localResult          wadah hasil milik task pemanggil (dimutasi langsung)
								 * @param kegiatan              kegiatan/tagihan yang diproses
								 * @param cicilanPembayarans   riwayat cicilan pembayaran mahasiswa/calon mahasiswa terkait
								 * @param threadParams          parameter report (nama, NIM, kaprodi, dst) milik task ini, dimutasi dengan data spesifik kegiatan
								 */
								private void kirim(SuratResult localResult, Kegiatan kegiatan,
										List<CicilanPembayaran> cicilanPembayarans, Map<String, Object> threadParams)
										throws Exception {
									Mahasiswa mhsData = kegiatan.getMahasiswa();
									BiodataCalonMahasiswa biodataCalon = kegiatan.getCalonMahasiswa();

									if (mhsData != null) {
										threadParams.put("nama", mhsData.getNama());
										threadParams.put("nim", mhsData.getNim());
										threadParams.put("semester", kegiatan.getSemster());
										threadParams.put("tahun_akademik", kegiatan.getTahunAkademik());
										threadParams.put("kaprodi",
												mhsData.getJurusan().getKaprodi() == null ? "(......................)"
														: mhsData.getJurusan().getKaprodi().getNama());
										threadParams.put("jurusan", mhsData.getJurusan().getNama());
										threadParams.put("fakultas", mhsData.getJurusan().getFakultas().getNama());

										List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
										Double sisa = CommonReportHelper.populateMapTagihanMahasiswa(mhsData,
												kegiatan.getSemster(), kegiatan, kegiatan.getJenisKegiatan(),
												cicilanPembayarans, maps);
										Jurusan jurusan = mhsData.getJurusan();

										Object[] rowData = new Object[11];
										rowData[0] = mhsData.getNim();
										rowData[1] = mhsData.getNama();
										rowData[2] = j.getNamaKegiatan();
										rowData[3] = jurusan == null ? "" : jurusan.getFakultas().getNama();
										rowData[4] = jurusan == null ? "" : jurusan.getNama();
										rowData[5] = mhsData.getStatusAwalMahasiswa() == null ? ""
												: mhsData.getStatusAwalMahasiswa().getNama();
										rowData[6] = mhsData.getTahunangkatan();
										rowData[7] = mhsData.getEmail();
										rowData[8] = kegiatan.getTahunAkademik();
										rowData[9] = kegiatan.getSemster();
										rowData[10] = sisa;

										localResult.excelRows.add(rowData);

										if (sisa > 0.1) {
											threadParams.put("maps", maps);
											Toolbar toolbar = null;
											File file = Report.generateFileReport(Report.PDF, threadParams,
													"Surat_Tagihan_Mahasiswa", ais.ui.util.WaktuUtil.getDate(),
													toolbar);

											boolean email = false;
											if (mhsData.getEmail() != null && !mhsData.getEmail().isEmpty()) {
												if (kirim) {
													String subject = "Tagihan \""
															+ kegiatan.getJenisKegiatan().getNamaKegiatan()
															+ "\" semester " + kegiatan.getSemster()
															+ " tahun akademik " + kegiatan.getTahunAkademik()
															+ " untuk mahasiswa atas nama " + mhsData.getNama() + " ("
															+ mhsData.getNim() + ")";
													String body = "Anda mendapatkan informasi tagihan \""
															+ kegiatan.getJenisKegiatan().getNamaKegiatan()
															+ "\" semester " + kegiatan.getSemster()
															+ " tahun akademik " + kegiatan.getTahunAkademik()
															+ " untuk mahasiswa atas nama " + mhsData.getNama() + " ("
															+ mhsData.getNim()
															+ ") <br>Isi informasi tagihan ada di file terlampir.<br><br>Terima Kasih";
													String sender = Common
															.getKonfigurasi("default_email", "info@zishof.com")
															.getNilai();
													JSONArray userIds = new JSONArray();
													userIds.put(mhsData.getNim());
													MailSender.sendMailLampiranTagihan(userIds, subject, body, sender,
															mhsData.getEmail(), null, false, mhsData, file);
												}
												email = true;
											}

											localResult.files.add(file);
											localResult.fileNames.add(URLEncoder
													.encode((email ? mhsData.getEmail() + "_" : "tidak_ada_email")
															+ mhsData.getNim() + "_" + mhsData.getNama() + "_"
															+ kegiatan.getJenisKegiatan().getNama() + "_smt_"
															+ kegiatan.getSemster(), "UTF-8")
													+ "_" + file.getName());
										}

									} else if (biodataCalon != null) {
										threadParams.put("nama", biodataCalon.getNama());
										threadParams.put("nim", biodataCalon.getNoRegistrasi());
										threadParams.put("semester", kegiatan.getSemster());
										threadParams.put("tahun_akademik", kegiatan.getTahunAkademik());

										Jurusan jurusan = biodataCalon.getProdiLulus() == null
												? biodataCalon.getProdi1()
												: biodataCalon.getProdiLulus();
										threadParams.put("kaprodi",
												jurusan == null || jurusan.getKaprodi() == null
														? "(......................)"
														: jurusan.getKaprodi().getNama());
										threadParams.put("jurusan", jurusan == null ? "" : jurusan.getNama());
										threadParams.put("fakultas",
												jurusan == null ? "" : jurusan.getFakultas().getNama());

										List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
										Double sisa = CommonReportHelper.populateMapTagihanMahasiswa(null,
												kegiatan.getSemster(), kegiatan, kegiatan.getJenisKegiatan(),
												cicilanPembayarans, maps);

										Object[] rowData = new Object[11];
										rowData[0] = biodataCalon.getNoRegistrasi();
										rowData[1] = biodataCalon.getNama();
										rowData[2] = j.getNamaKegiatan();
										rowData[3] = jurusan == null ? "" : jurusan.getFakultas().getNama();
										rowData[4] = jurusan == null ? "" : jurusan.getNama();
										rowData[5] = biodataCalon.getStatusAwalMahasiswa() == null ? ""
												: biodataCalon.getStatusAwalMahasiswa().getNama();
										rowData[6] = biodataCalon.getTahun();
										rowData[7] = biodataCalon.getEmail();
										rowData[8] = kegiatan.getTahunAkademik();
										rowData[9] = kegiatan.getSemster();
										rowData[10] = sisa;

										localResult.excelRows.add(rowData);

										if (sisa > 0.1) {
											threadParams.put("maps", maps);
											Toolbar toolbar = null;
											File file = Report.generateFileReport(Report.PDF, threadParams,
													"Surat_Tagihan", ais.ui.util.WaktuUtil.getDate(), toolbar);

											boolean email = false;
											if (biodataCalon.getEmail() != null && !biodataCalon.getEmail().isEmpty()) {
												if (kirim) {
													String subject = "Tagihan \""
															+ kegiatan.getJenisKegiatan().getNamaKegiatan()
															+ "\" semester " + kegiatan.getSemster()
															+ " tahun akademik " + kegiatan.getTahunAkademik()
															+ " untuk mahasiswa atas nama " + biodataCalon.getNama()
															+ " (" + biodataCalon.getNoRegistrasi() + ")";
													String body = "Anda mendapatkan informasi tagihan \""
															+ kegiatan.getJenisKegiatan().getNamaKegiatan()
															+ "\" semester " + kegiatan.getSemster()
															+ " tahun akademik " + kegiatan.getTahunAkademik()
															+ " untuk mahasiswa atas nama " + biodataCalon.getNama()
															+ " (" + biodataCalon.getNoRegistrasi()
															+ ") <br>Isi informasi tagihan ada di file terlampir.<br><br>Terima Kasih";
													String sender = Common
															.getKonfigurasi("default_email", "info@zishof.com")
															.getNilai();
													JSONArray userIds = new JSONArray();
													if (biodataCalon.getMahasiswa() != null) {
														userIds.put(biodataCalon.getMahasiswa().getNim());
													}
													MailSender.sendMailLampiranTagihan(userIds, subject, body, sender,
															biodataCalon.getEmail(), null, false, biodataCalon, file);
												}
												email = true;
											}

											localResult.files.add(file);
											localResult.fileNames.add(URLEncoder
													.encode((email ? biodataCalon.getEmail() + "_" : "tidak_ada_email")
															+ biodataCalon.getNoRegistrasi() + "_"
															+ biodataCalon.getNama() + "_"
															+ kegiatan.getJenisKegiatan().getNama() + "_smt_"
															+ kegiatan.getSemster(), "UTF-8")
													+ "_" + file.getName());
										}
									}
								}

								@SuppressWarnings({ })
								@Override
								public void run() {

									try {
										List<Mahasiswa> mahasiswas = new ArrayList<Mahasiswa>();
										List<BiodataCalonMahasiswa> biodataCalonMahasiswas = new ArrayList<BiodataCalonMahasiswa>();

										Session sessionLoader = null;
										try {
											sessionLoader = HibernateUtil.currentNativeSession();
											if ((ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null && j.getId()
													.equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId()))
													|| (ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
															&& j.getId().equals(
																	ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU
																			.getId()))) {

												Criterion mhsbaru = Restrictions.isNotNull("prodiLulus");
												if (fak != null)
													mhsbaru = Restrictions.and(mhsbaru,
															Restrictions.eq("prodiLulus.fakultas", fak));
												if (jur != null)
													mhsbaru = Restrictions.and(mhsbaru,
															Restrictions.eq("prodiLulus", jur));

												Criterion calonmhsbaru = Restrictions.sqlRestriction("true");
												if (fak != null) {
													Criterion calonmhsbaruOr = Restrictions.eq("prodi1.fakultas", fak);
													calonmhsbaruOr = Restrictions.or(calonmhsbaruOr,
															Restrictions.eq("prodi2.fakultas", fak));
													calonmhsbaruOr = Restrictions.or(calonmhsbaruOr,
															Restrictions.eq("prodi3.fakultas", fak));
													calonmhsbaruOr = Restrictions.or(calonmhsbaruOr,
															Restrictions.eq("prodi4.fakultas", fak));
													calonmhsbaruOr = Restrictions.or(calonmhsbaruOr,
															Restrictions.eq("prodi5.fakultas", fak));
													calonmhsbaruOr = Restrictions.or(calonmhsbaruOr,
															Restrictions.eq("prodiLulus.fakultas", fak));
													calonmhsbaru = Restrictions.and(calonmhsbaru, calonmhsbaruOr);
												}
												if (jur != null) {
													Criterion calonmhsbaruOr = Restrictions.eq("prodi1", jur);
													calonmhsbaruOr = Restrictions.or(calonmhsbaruOr,
															Restrictions.eq("prodi2", jur));
													calonmhsbaruOr = Restrictions.or(calonmhsbaruOr,
															Restrictions.eq("prodi3", jur));
													calonmhsbaruOr = Restrictions.or(calonmhsbaruOr,
															Restrictions.eq("prodi4", jur));
													calonmhsbaruOr = Restrictions.or(calonmhsbaruOr,
															Restrictions.eq("prodi5", jur));
													calonmhsbaruOr = Restrictions.or(calonmhsbaruOr,
															Restrictions.eq("prodiLulus", jur));
													calonmhsbaru = Restrictions.and(calonmhsbaru, calonmhsbaruOr);
												}

												biodataCalonMahasiswas = ConstantValues
														.simpleList(
																sessionLoader
																		.createCriteria(BiodataCalonMahasiswa.class)
																		.add(Restrictions.or(
																				Restrictions.isNull("aktif"),
																				Restrictions.eq("aktif", true)))
																		.add(mhs.trim().isEmpty()
																				? Restrictions.sqlRestriction("true")
																				: Restrictions.or(
																						Restrictions.ilike("nama", mhs,
																								MatchMode.ANYWHERE),
																						Restrictions.or(
																								Restrictions.ilike(
																										"noRegistrasi",
																										mhs,
																										MatchMode.ANYWHERE),
																								Restrictions.ilike(
																										"noUjian", mhs,
																										MatchMode.ANYWHERE))))
																		.createAlias("prodiLulus", "prodiLulus",
																				Criteria.LEFT_JOIN)
																		.createAlias("prodi1", "prodi1",
																				Criteria.LEFT_JOIN)
																		.createAlias("prodi2", "prodi2",
																				Criteria.LEFT_JOIN)
																		.createAlias("prodi3", "prodi3",
																				Criteria.LEFT_JOIN)
																		.createAlias("prodi4", "prodi4",
																				Criteria.LEFT_JOIN)
																		.createAlias("prodi5", "prodi5",
																				Criteria.LEFT_JOIN)
																		.add(Restrictions.between("tahunAkademik", ta,
																				taSampai))
																		.add((ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
																				&& j.getId().equals(
																						ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU
																								.getId())) ? mhsbaru
																										: calonmhsbaru),
																BiodataCalonMahasiswa.class);
											} else {
												Criteria c = sessionLoader.createCriteria(Mahasiswa.class)
														.add(Restrictions.eq("aktif", true));

												if (status != null) {
													c = sessionLoader.createCriteria(HistoryStatusMahasiswa.class)
															.add(j != null && j.getUntukBayarSP()
																	? Restrictions.eq("sp", Perkuliahan.SEMESTER_PENDEK)
																	: Restrictions.isNull("sp"))
																	.add(Restrictions.eq("tahunAkademik", taStatus))
																	.add(Restrictions.eq("ganjilGenap", jenisSmtStatus))
															.setProjection(Projections.groupProperty("mahasiswa"))
															.createCriteria("mahasiswa");
												}

												mahasiswas = ConstantValues.simpleList(c.add(mhs.trim().isEmpty()
														? Restrictions.sqlRestriction("true")
														: Restrictions.or(
																Restrictions.ilike("nim", mhs, MatchMode.ANYWHERE),
																Restrictions.ilike("nama", mhs, MatchMode.ANYWHERE)))
														.add(Restrictions.between("tahunangkatan", ang, angSampai))
														.add(jur == null ? Restrictions.sqlRestriction("true")
																: Restrictions.eq("jurusan", jur))
														.createAlias("jurusan", "jurusan")
														.add(fak == null ? Restrictions.sqlRestriction("true")
																: Restrictions.eq("jurusan.fakultas", fak)),
														Mahasiswa.class);
											}
										} finally {
											if (sessionLoader != null && sessionLoader.isOpen()) {
												sessionLoader.disconnect();
												sessionLoader.close();
											}
											HibernateUtil.closeSession();
										}

										final int totalSize = mahasiswas.size() + biodataCalonMahasiswas.size();
										intbox.setValue(totalSize);

										final int thn = Integer.parseInt(ta.split("/")[0]);
										final int thnSampai = Integer.parseInt(taSampai.split("/")[0]);

										// SETUP MULTI-THREADING (Surat & PDF Generation)
										ExecutorService executor = Executors.newFixedThreadPool(getSafeThreadPoolSize(0));
										List<Callable<SuratResult>> tasks = new ArrayList<Callable<SuratResult>>();
										final AtomicInteger progressCounter = new AtomicInteger(0);
										final long startTime = System.currentTimeMillis();

										// TASK MAHASISWA
										for (final Mahasiswa mahasiswaData : mahasiswas) {
											tasks.add(new Callable<SuratResult>() {
												@Override
												public SuratResult call() throws Exception {
													SuratResult localResult = new SuratResult();
													Map<String, Object> threadParams = new HashMap<String, Object>(
															baseParameters);
													Session s = null;

													try {
														s = HibernateUtil.currentNativeSession();
														List<CicilanPembayaran> cicilanPembayarans = mahasiswaData
																.ambilCicilan();

														for (int tahun = thn; tahun <= thnSampai; tahun++) {
															if (mahasiswaData.getTahunangkatan() <= tahun) {
																String Ta = tahun + "/" + (tahun + 1);

																if (GANJIL) {
																	Integer smt = Common.getSemester(
																			mahasiswaData.getTahunangkatan(), Ta,
																			Perkuliahan.GANJIL,
																			mahasiswaData
																					.getPindahKeKampusIniMasukSemester(),
																			mahasiswaData.getSemesterMulai());
																	if (smt >= j.getMinSmt() && smt <= j.getMaxSmt()) {
																		Kegiatan kegiatan = KegiatanHelper
																				.checkKegiatanMahasiswa(j,
																						mahasiswaData, smt, Ta, ulang,
																						rst, null, s);
																		if (kegiatan != null)
																			kirim(localResult, kegiatan,
																					cicilanPembayarans, threadParams);
																	}
																}

																if (GENAP) {
																	Integer smt = Common.getSemester(
																			mahasiswaData.getTahunangkatan(), Ta,
																			Perkuliahan.GENAP,
																			mahasiswaData
																					.getPindahKeKampusIniMasukSemester(),
																			mahasiswaData.getSemesterMulai());
																	if (smt >= j.getMinSmt() && smt <= j.getMaxSmt()) {
																		Kegiatan kegiatan = KegiatanHelper
																				.checkKegiatanMahasiswa(j,
																						mahasiswaData, smt, Ta, ulang,
																						rst, null, s);
																		if (kegiatan != null)
																			kirim(localResult, kegiatan,
																					cicilanPembayarans, threadParams);
																	}
																}
															}
														}
													} catch (Exception e) {
														e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KegiatanProsesHeper.java:2083");
													} finally {
														if (s != null && s.isOpen()) {
															s.disconnect();
															s.close();
														}
														HibernateUtil.closeSession();
													}

													int currProgress = progressCounter.incrementAndGet();
													long elapsed = System.currentTimeMillis() - startTime;
													long etaMs = (currProgress > 0)
															? (elapsed / currProgress) * (totalSize - currProgress)
															: 0;
													long etaMenit = (etaMs / 1000) / 60;
													long etaDetik = (etaMs / 1000) % 60;

													DecimalFormat df = new DecimalFormat("#.##");
													label.setValue("Buat surat " + currProgress + "/" + totalSize + " ("
															+ df.format(currProgress * 100.0 / totalSize) + " %) "
															+ "- ETA: " + etaMenit + "m " + etaDetik + "s");

													return localResult;
												}
											});
										}

										// TASK CALON MAHASISWA
										for (final BiodataCalonMahasiswa mhsCalon : biodataCalonMahasiswas) {
											tasks.add(new Callable<SuratResult>() {
												@Override
												public SuratResult call() throws Exception {
													SuratResult localResult = new SuratResult();
													Map<String, Object> threadParams = new HashMap<String, Object>(
															baseParameters);
													Session s = null;

													try {
														s = HibernateUtil.currentNativeSession();
														List<CicilanPembayaran> cicilanPembayarans = mhsCalon
																.ambilCicilan();

														for (int tahun = thn; tahun <= thnSampai; tahun++) {
															String Ta = tahun + "/" + (tahun + 1);
															if (mhsCalon.getTahun() != null
																	&& mhsCalon.getTahun().equals(tahun)) {
																if (GANJIL) {
																	int smt = (ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
																			&& j.getId().equals(
																					ConstantValues.PENDAFTARAN_CALON_MAHASISWA
																							.getId())) ? 0 : 1;
																	Kegiatan kegiatan = KegiatanHelper
																			.checkKegiatanCalonMahasiswa(j, mhsCalon,
																					smt, Ta, ulang, rst, null, s);
																	if (kegiatan != null)
																		kirim(localResult, kegiatan, cicilanPembayarans,
																				threadParams);
																}

																if (GENAP) {
																	int smt = (ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
																			&& j.getId().equals(
																					ConstantValues.PENDAFTARAN_CALON_MAHASISWA
																							.getId())) ? 0 : 2;
																	Kegiatan kegiatan = KegiatanHelper
																			.checkKegiatanCalonMahasiswa(j, mhsCalon,
																					smt, Ta, ulang, rst, null, s);
																	if (kegiatan != null)
																		kirim(localResult, kegiatan, cicilanPembayarans,
																				threadParams);
																}
															}
														}
													} catch (Exception e) {
														e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KegiatanProsesHeper.java:2157");
													} finally {
														if (s != null && s.isOpen()) {
															s.disconnect();
															s.close();
														}
														HibernateUtil.closeSession();
													}

													int currProgress = progressCounter.incrementAndGet();
													long elapsed = System.currentTimeMillis() - startTime;
													long etaMs = (currProgress > 0)
															? (elapsed / currProgress) * (totalSize - currProgress)
															: 0;
													long etaMenit = (etaMs / 1000) / 60;
													long etaDetik = (etaMs / 1000) % 60;

													DecimalFormat df = new DecimalFormat("#.##");
													label.setValue("Buat surat " + currProgress + "/" + totalSize + " ("
															+ df.format(currProgress * 100.0 / totalSize) + " %) "
															+ "- ETA: " + etaMenit + "m " + etaDetik + "s");

													return localResult;
												}
											});
										}

										// Eksekusi Surat Paralel
										List<Future<SuratResult>> futures = executor.invokeAll(tasks);
										executor.shutdown();

										// MERAKIT DAN MENULIS FILE EXCEL (Thread Utama)
										XSSFWorkbook workbook = new XSSFWorkbook();
										XSSFSheet sheet = workbook.createSheet("DATA SURAT TAGIHAN");
										sheet.setDefaultColumnWidth(20);
										XSSFCellStyle lockedNumericStyle = workbook.createCellStyle();
										lockedNumericStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
										lockedNumericStyle.setFillForegroundColor(new XSSFColor(Color.RED));
										lockedNumericStyle.setLocked(true);

										XSSFRow rowhead = sheet.createRow((short) 0);
										rowhead.createCell(0).setCellValue("NIM/NO REG");
										rowhead.createCell(1).setCellValue("NAMA");
										rowhead.createCell(2).setCellValue("JENIS PEMBAYARAN");
										rowhead.createCell(3).setCellValue(Common.getBahasaConfig("FAKULTAS"));
										rowhead.createCell(4).setCellValue(Common.getBahasaConfig("JURUSAN"));
										rowhead.createCell(5).setCellValue("STATUS AWAL");
										rowhead.createCell(6).setCellValue("ANGKATAN");
										rowhead.createCell(7).setCellValue("EMAIL");
										rowhead.createCell(8).setCellValue("TA");
										rowhead.createCell(9).setCellValue("SMT");
										rowhead.createCell(10).setCellValue("SISA TAGIHAN");

										int rowIndex = 1;

										// Agregasi Hasil Thread
										for (Future<SuratResult> future : futures) {
											SuratResult result = future.get();

											// Memasukan data row excel hasil thread
											for (Object[] rowData : result.excelRows) {
												XSSFRow row = sheet.createRow(rowIndex++);
												for (int i = 0; i < rowData.length; i++) {
													Object val = rowData[i];
													XSSFCell cell = row.createCell(i);
													if (val == null) {
														cell.setCellValue("");
													} else if (val instanceof String) {
														cell.setCellValue((String) val);
													} else if (val instanceof Double) {
														cell.setCellValue((Double) val);
													} else if (val instanceof Integer) {
														cell.setCellValue((Integer) val);
													} else {
														cell.setCellValue(val.toString());
													}
												}
											}

											// Menambahkan file hasil PDF untuk zip
											filenames.addAll(result.files);
											filesName.addAll(result.fileNames);
										}

										try {
											FileOutputStream fileOut = new FileOutputStream(filenameexcel);
											workbook.write(fileOut);
											fileOut.close();
										} catch (IOException e) {
											Common.tampilErrorJikaAdmin(e);
										}

										mahasiswas.clear();
										biodataCalonMahasiswas.clear();
										label.setValue("");

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										label.setValue("-");
									}
								}
							});

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}

					}
				});
				save.setParent(toolbar);

				window.onModal();

			}
		});

		return toolbarbutton;
	}

}
