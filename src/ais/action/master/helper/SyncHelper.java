package ais.action.master.helper; // Sesuaikan dengan package Anda

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Timer;

import ais.common.Common;
import ais.common.LaporanUpload;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

// Sesuaikan import di bawah ini dengan lokasi class komponen ZK kustom Anda
// import ais.common.MyToolbarbuttonConfig;
// import ais.common.MyMessageboxConfig;

/**
 * Helper sentral untuk memproses sinkronisasi data secara paralel menggunakan Thread Pool.
 * Terisolasi, bebas memory-leak, dan kompatibel dengan Java 1.6/1.7.
 */
public class SyncHelper {

	// --- Antarmuka untuk mengambil data dari Class Pemanggil (Controller) ---
	public interface SyncDataProvider {
		List<Perkuliahan> getData() throws Exception;
	}

	// --- Antarmuka untuk Aksi Dinamis (Callback) Eksekusi per Item ---
	public interface SyncAction {
		void execute(Perkuliahan perkuliahan, Session session) throws Exception;
	}

	/** "kunci" (penanda baris) yang dipakai laporan hasil sinkronisasi: kode+nama matkul bila ada, jika tidak ID. */
	private static String kunciPerkuliahan(Perkuliahan perkuliahan) {
		try {
			if (perkuliahan == null) {
				return "-";
			}
			if (perkuliahan.getMatakuliah() != null && perkuliahan.getMatakuliah().getNama() != null
					&& !perkuliahan.getMatakuliah().getNama().trim().isEmpty()) {
				String kode = perkuliahan.getMatakuliah().getKode() == null ? ""
						: perkuliahan.getMatakuliah().getKode().trim() + " - ";
				return kode + perkuliahan.getMatakuliah().getNama().trim();
			}
			return perkuliahan.getId() == null ? "-" : "ID:" + perkuliahan.getId();
		} catch (Exception e) {
			return perkuliahan != null && perkuliahan.getId() != null ? "ID:" + perkuliahan.getId() : "-";
		}
	}

	/**
	 * Method bantuan sentral untuk membuat tombol sinkronisasi dengan proses Background paralel.
	 *
	 * <p>Hasil TIAP baris (berhasil/gagal beserta penyebab teknis lengkap — kelas, pesan, titik
	 * berhenti di kode, langkah mengatasi) dicatat via {@link LaporanUpload} dan otomatis
	 * diunduh sebagai berkas teks begitu proses selesai, MENGGANTIKAN popup generik lama
	 * "... telah selesai 100%." yang menyembunyikan kegagalan per baris.</p>
	 */
	public static void buatTombolSinkronisasi(Component parent, String labelTombol, String icon,
			boolean isVisible, final String namaProses, final SyncDataProvider dataProvider, final SyncAction action) {

		MyToolbarbuttonConfig btn = new MyToolbarbuttonConfig(labelTombol, icon);
		btn.setVisible(isVisible);
		parent.appendChild(btn);

		btn.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						// Gunakan AtomicReference agar text status aman dibaca lintas Thread
						final AtomicReference<String> statusMessage = new AtomicReference<String>("Menyiapkan " + namaProses + "...");

						// Laporan rinci per baris (berhasil/gagal+penyebab teknis lengkap+langkah
						// mengatasi) - dibuat BARU tiap klik, otomatis diunduh sbg berkas teks
						// sesudah proses selesai.
						final LaporanUpload laporan = new LaporanUpload(namaProses);
						final AtomicInteger nomorBarisLaporan = new AtomicInteger(0);

						// Thread Pengontrol Utama (Background)
						new Thread(new Runnable() {
							@Override
							public void run() {
								List<Perkuliahan> perkuliahans = null;
								try {
									// Ambil data melalui jembatan antarmuka
									perkuliahans = dataProvider.getData();

									if (perkuliahans == null || perkuliahans.isEmpty()) {
										statusMessage.set(""); // Sinyal selesai jika data kosong
										return;
									}

									final int totalData = perkuliahans.size();
									final AtomicInteger dataTerproses = new AtomicInteger(0);

									// Thread Pool (Maksimal 100 proses paralel bersamaan)
									ExecutorService executor = Executors.newFixedThreadPool(ais.common.DbThreadPool.safe(100));

									for (final Perkuliahan perkuliahan : perkuliahans) {
										executor.submit(new Runnable() {
											@Override
											public void run() {
												Session session = null;
												int nomorBaris = nomorBarisLaporan.getAndIncrement();
												String kunci = kunciPerkuliahan(perkuliahan);
												try {
													// Buka session terisolasi untuk tiap Thread
													session = HibernateUtil.getSessionFactory().openSession();

													// Eksekusi proses dinamis
													action.execute(perkuliahan, session);

													laporan.catatBerhasil(nomorBaris, kunci, "Sinkronisasi berhasil");

												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/SyncHelper.java:97");
													// e.printStackTrace();
													laporan.catatGagalDetail(nomorBaris, kunci, e);
												} finally {
													// Manajemen Session Anti Bocor
													if (session != null) {
														try { if (session.isOpen()) session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/SyncHelper.java:102");}
														try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/SyncHelper.java:103");}
														try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/SyncHelper.java:104");}
													}
												}

												// Update Indikator Progress Bar & Angka
												int current = dataTerproses.incrementAndGet();
												double persentase = (current * 100.0) / totalData;

												String textProgress = namaProses + " | "
														+ current + " dari " + totalData + " data "
														+ "(" + Common.numberFormat.get().format(persentase) + "%) diproses...";

												statusMessage.set(textProgress);
											}
										});
									}

									// Tunggu semua 100 antrean selesai
									executor.shutdown();
									try {
										executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
									} catch (InterruptedException e) {
										Thread.currentThread().interrupt();
									}

								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/SyncHelper.java:129");
									// e.printStackTrace();
								} finally {
									// Optimasi Memori: Bebaskan list perkuliahan secepatnya
									if (perkuliahans != null) {
										perkuliahans.clear();
										perkuliahans = null;
									}
									// Kirim sinyal selesai ke UI
									statusMessage.set("");
								}
							}
						}).start();

						// ZK UI TIMER: Membaca statusMessage dan Update Layout
						final Timer timer = new Timer(500);
						timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						timer.setRepeats(true);
						timer.addEventListener("onTimer", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								String currentStatus = statusMessage.get();

								if (currentStatus == null || currentStatus.isEmpty()) {
									Clients.clearBusy();
									timer.detach();
									// Laporan rinci per baris (berhasil/gagal+penyebab teknis lengkap)
									// otomatis diunduh sbg berkas teks; menggantikan popup generik lama.
									laporan.selesaikan(null);
								} else {
									Clients.showBusy(currentStatus);
								}
							}
						});
						timer.start();

					}
				});
			}
		});
	}
}
