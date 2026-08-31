package ais.action.master.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimerTask;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.SecurityFilter;
import ais.common.TagihanProcessor;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AccessedUsers;
import ais.database.model.GeneralValueObject;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.Konfigurasi;
import ais.database.model.MemoryInfo;
import ais.database.model.OnlineUsers;
import ais.database.model.RestartLog;
import ais.database.model.TanyaJawab;
import ais.database.model.sekolah.PengaturanBiaya;
import ais.ui.util.WaktuUtil;

/**
 * Tipe khusus untuk user online counter. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * TimerTask}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Number count}, {@code Number
 * countOnline}, {@code boolean runningNotifikasiTagihanSekolah}, {@code long index}, {@code Map mapTanyaJawab},
 * {@code double MEGABYTE}, {@code long MEGABYTE_LONG}, {@code List ss}; validasi/perhitungan ({@code check()},
 * {@code restartCheck()}, {@code ujianCheck()}, {@code memoryCheck()}); penghapusan/pembatalan ({@code
 * hapusJsp()}); operasi domain lain ({@code closeNativeSessionQuietly()}, {@code runSafely()}, {@code
 * pesanResourceTertutup()}, {@code doRestart()}, {@code chekWarmingUp()}, {@code run()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see TimerTask
 */
public class UserOnlineCounter extends TimerTask {

	public static Number count = null;
	public static Number countOnline = null;

	private static volatile boolean runningNotifikasiTagihanSekolah = false;

	private static void closeNativeSessionQuietly(Session session) {
		if (session != null) {
			try {
				session.clear();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/UserOnlineCounter.java:50");
			}
			try {
				session.disconnect();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/UserOnlineCounter.java:54");
			}
			try {
				if (session.isOpen()) {
					session.close();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/UserOnlineCounter.java:60");
			}
		}
		try {
			HibernateUtil.closeSession();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/UserOnlineCounter.java:65");
		}
	}

	private static void runSafely(String namaProses, Runnable runnable) {
		// Jangan mulai pekerjaan baru saat aplikasi/webapp sedang berhenti.
		if (Common.aplikasiSedangBerhenti) {
			return;
		}
		try {
			runnable.run();
		} catch (Throwable t) {
			// Saat shutdown/redeploy, resource (MapDB cache & pool koneksi c3p0) sudah
			// ditutup duluan oleh jalur shutdown lain (race antar listener). Error
			// "already closed" / "has been closed" / "Cannot open connection" di sini
			// adalah NOISE shutdown — jangan cetak stack trace yang menakutkan.
			if (Common.aplikasiSedangBerhenti || pesanResourceTertutup(t)) {
				System.out.println("UserOnlineCounter melewati " + namaProses + " (aplikasi sedang berhenti / resource sudah ditutup).");
				return;
			}
			System.out.println("UserOnlineCounter gagal menjalankan " + namaProses + " : " + t.getMessage());
			t.printStackTrace(); ais.common.ErrorAuditUtil.record(t, "auto-audit src/ais/action/master/helper/UserOnlineCounter.java:86");
		}
	}

	/** True bila throwable menandakan resource (MapDB/pool koneksi) sudah ditutup saat shutdown. */
	private static boolean pesanResourceTertutup(Throwable t) {
		for (Throwable c = t; c != null; c = c.getCause()) {
			String m = c.getMessage();
			if (m != null && (m.contains("already closed") || m.contains("has been closed")
					|| m.contains("Cannot open connection") || m.contains("closed()")
					|| m.contains("terminating connection due to")
					|| m.contains("An I/O error occurred while sending to the backend"))) {
				return true;
			}
			if (c instanceof java.lang.IllegalAccessError || c instanceof java.lang.IllegalStateException) {
				if (m != null && m.toLowerCase().contains("closed")) {
					return true;
				}
			}
			if (c == c.getCause()) {
				break;
			}
		}
		return false;
	}

	public static void check() {

		Session session = null;
		try {

			session = HibernateUtil.currentNativeSession();

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) - 23);
			Date waktu = calendar.getTime();
			Date now = ais.ui.util.WaktuUtil.getDate();

			count = (Number) session.createCriteria(AccessedUsers.class).add(Restrictions.between("waktu", waktu, now))
					.setProjection(Projections.rowCount()).uniqueResult();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/UserOnlineCounter.java:127");
		} finally {
			closeNativeSessionQuietly(session);
		}

		try {
			countOnline = 0;
			Set<String> set = new HashSet<String>();
			for (OnlineUsers onlineUsers : SecurityFilter.dataOnline.values()) {
				set.add(onlineUsers.getNama());
			}
			countOnline = set.size();
			set.clear();
			set = null;
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/UserOnlineCounter.java:142");
		}

	}

	public static String doRestart(String perintah_restart) {
		String keterangan = "Perintah restart \"" + perintah_restart + "\"";

		try {
			Process p = Runtime.getRuntime().exec(new String[] { "bash", "-l", "-c", perintah_restart }, null,
					new File("/opt/"));
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty("line.separator"));
			}
			String hasil = builder.toString();

			System.out.println(hasil);
			keterangan += ", hasil \"" + hasil + "\"";
		} catch (Exception e) {
			keterangan += ", hasil error \"" + e.getMessage() + "\"";
			Common.tampilErrorJikaAdmin(e);
		}

		RestartLog restartLog = new RestartLog();
		restartLog.setKeterangan(keterangan);

		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			session.save(restartLog);
			session.getTransaction().commit();
		} catch (Exception e) {
			try {
				if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/UserOnlineCounter.java:183");
			}
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeNativeSessionQuietly(session);
		}
		return keterangan;
	}

	public static void restartCheck() {
		if (Common.bolehKonfigurasi("aktifkan_auto_restart", Konfigurasi.TIDAK_AKTIF)) {
			String restartTime = Common.getKonfigurasi("auto_restart", "").getNilai();
			String perintah_restart = Common.getKonfigurasi("perintah_restart", "").getNilai();
			if (!restartTime.isEmpty() && !perintah_restart.isEmpty()) {
				String timeSekarang = Common.timeFormat2.get().format(WaktuUtil.getDate());
//				System.out.println("aktifkan_auto_restart -> restartTime " + restartTime + ", timeSekarang "
//						+ timeSekarang + ", perintah_restart " + perintah_restart);
				if (timeSekarang.equals(restartTime)) {
					doRestart(perintah_restart);
				}
			}
		}
	}

	public static void ujianCheck() {
		try {
			Set<String> removedData = new HashSet<String>();
			for (String key : ProsesUjianHelper.kuotaUjian) {
				HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) GeneralValueObject
						.ambilDataLangsung(HasilUjianMahasiswa.class, key);
				if (hasilUjianMahasiswa.getMulaiPada() != null) {
					long durationInMillis = WaktuUtil.getDate().getTime()
							- hasilUjianMahasiswa.getMulaiPada().getTime();

					long hour = (durationInMillis / (1000 * 60 * 60)) % 24;
					if (hour > 3L) {
						removedData.add(key);
					}
				}
			}

			if (!removedData.isEmpty()) {
				System.out.println("removedData -> " + removedData);
				ProsesUjianHelper.kuotaUjian.removeAll(removedData);
			}
			removedData = null;
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/UserOnlineCounter.java:230");
		}
	}

	private long index = 0L;

	// LRU BER-BATAS (bukan HashMap polos): key = nomor WhatsApp masuk dan entri tidak
	// pernah dihapus — sebelumnya tumbuh monoton per nomor distinct seumur JVM
	// (optimasi RAM Fase 1). 2000 percakapan aktif terakhir dipertahankan; nomor lama
	// yang tersingkir memulai alur tanya-jawab dari awal.
	public static Map<String, TanyaJawab> mapTanyaJawab = java.util.Collections
			.synchronizedMap(new java.util.LinkedHashMap<String, TanyaJawab>(16, 0.75f, true) {
				private static final long serialVersionUID = 1L;

				@Override
				protected boolean removeEldestEntry(java.util.Map.Entry<String, TanyaJawab> eldest) {
					return size() > 2000;
				}
			});

	private void chekWarmingUp() {
		try {
			for (final String s : Common.getKonfigurasi("warming_up_code_watzap", "").getNilai().trim().split(";")) {

				try {
					if (!s.trim().isEmpty()) {
						new Thread(new Runnable() {

							@SuppressWarnings("unchecked")
							@Override
							public void run() {
								try {
								try {
									String[] ss = s.split(",");
									String phone_no = ss[0];
									String watzap_api_key = ss[1];
									String watzap_number_key = ss[2];
									String phone_no_asli = ss[3];

									try {
										Random r = new Random();
										int low = 1;
										int high = 120;
										int result = r.nextInt(high - low) + low;
										Thread.sleep(1000 * result);
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/UserOnlineCounter.java:263");
										// TODO: handle exception
									}

									TanyaJawab tanyaJawab = null;
									Session session = HibernateUtil.currentNativeSession();
									List<Long> keyList = session.createCriteria(TanyaJawab.class)
											.add(Restrictions.isNotNull("nama"))
											.setProjection(Projections.property("id")).list();
									// session.disconnect();
									if (session.isOpen()) {session.disconnect();session.close();}
									HibernateUtil.closeSession();

									if (!keyList.isEmpty()) {
										try {

											int size = keyList.size();
											int randIdx = new Random().nextInt(size);

											Long randomKey = keyList.get(randIdx);
											tanyaJawab = (TanyaJawab) ConstantValues.ambil(TanyaJawab.class.getName(),
													randomKey);

											System.out.println("************ Random Value " + randIdx + " " + size
													+ " ************ " + randomKey + " ************ " + tanyaJawab);

											if (tanyaJawab != null && tanyaJawab.getNama() != null) {
												mapTanyaJawab.put(phone_no_asli.trim(), tanyaJawab);
											}
										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/UserOnlineCounter.java:293");
										}
									}

									if (keyList.isEmpty() || (tanyaJawab != null && tanyaJawab.getNama() != null)) {

										JSONObject jsonObject = new JSONObject();
										jsonObject.put("api_key", watzap_api_key.trim());
										jsonObject.put("number_key", watzap_number_key.trim());
										jsonObject.put("phone_no", phone_no.trim());
										jsonObject.put("message",
												tanyaJawab != null && tanyaJawab.getNama() != null
														? tanyaJawab.getNama()
														: "kirimkan data pemanasan ke-" + index);
										jsonObject.put("wait_until_send", "1");

										String linkPost = "https://api.watzap.id/v1/send_message";
										String[] command = { "curl", "--request", "POST", linkPost, "--header",
												"Content-Type: application/json", "--data", jsonObject.toString() };
										System.out.println(
												"kirim pemanasan linkPost -> " + linkPost + ", to " + phone_no);
										ProcessBuilder process = new ProcessBuilder(command);
										Process p;
										p = process.start();
										BufferedReader reader = new BufferedReader(
												new InputStreamReader(p.getInputStream()));
										StringBuilder builder = new StringBuilder();
										String line;
										while ((line = reader.readLine()) != null) {
											builder.append(line);
											builder.append(System.getProperty("line.separator"));
										}
										String hasil = builder.toString();
										System.out.println("hasil pemanasan " + hasil);
									}
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/UserOnlineCounter.java:329");
								}
															} finally {
									ais.database.hibernate.HibernateUtil.closeSession();
								}
							}
						}).start();
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/UserOnlineCounter.java:338");
				}

			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/UserOnlineCounter.java:343");
		}
	}

	@Override
	public void run() {

		// Webapp/Tomcat sedang berhenti: hentikan task ini (MapDB cache & pool koneksi
		// sudah/akan ditutup → hanya menghasilkan "already closed" dan menahan thread).
		if (Common.aplikasiSedangBerhenti) {
			try { cancel(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/UserOnlineCounter.java:353"); }
			return;
		}

		// Tidur ~30 detik TAPI peka shutdown: keluar dalam <=1 detik bila aplikasi
		// berhenti, supaya thread Spring "timerFactory" tidak tertahan lama saat Tomcat
		// stop (cegah warning "failed to stop it" / kebocoran classloader).
		for (int detik = 0; detik < 30; detik++) {
			if (Common.aplikasiSedangBerhenti) {
				try { cancel(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/UserOnlineCounter.java:362"); }
				return;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}

		if (index % 2 == 0) {
			runSafely("notifikasi tagihan sekolah", new Runnable() {
				@Override
				public void run() {
					if (runningNotifikasiTagihanSekolah) {
						return;
					}
					runningNotifikasiTagihanSekolah = true;
					try {
						PengaturanBiaya.chekNotifikasi();
					} finally {
						runningNotifikasiTagihanSekolah = false;
					}
				}
			});
		}

		if (index % 25 == 0) {
			runSafely("warming up WhatsApp", new Runnable() {
				@Override
				public void run() {
					chekWarmingUp();
				}
			});
		}

		runSafely("cek kuota ujian", new Runnable() {
			@Override
			public void run() {
				ujianCheck();
			}
		});
		runSafely("hitung user online", new Runnable() {
			@Override
			public void run() {
				check();
			}
		});
		runSafely("cek restart otomatis", new Runnable() {
			@Override
			public void run() {
				restartCheck();
			}
		});
		runSafely("cek memori", new Runnable() {
			@Override
			public void run() {
				memoryCheck();
			}
		});
		runSafely("proses tagihan", new Runnable() {
			@Override
			public void run() {
				TagihanProcessor.proses();
			}
		});
		runSafely("cek kelas les", new Runnable() {
			@Override
			public void run() {
				PengaturanBiaya.checkKelasLes();
			}
		});

		index++;

		// Bersihkan ThreadLocal "berat" (DesEncrypter = class webapp) milik thread
		// timerFactory ini agar tidak menyandera classloader webapp saat redeploy.
		try {
			Common.bersihkanThreadLocalThreadIni();
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/master/helper/UserOnlineCounter.java:442");
		}
	}

	private static final double MEGABYTE = 1024.0 * 1024.0;

	public static double bytesToMegabytes(double bytes) {
		return bytes / MEGABYTE;
	}

	private static final long MEGABYTE_LONG = 1024L * 1024L;

	public static long bytesToMegabytesLong(long bytes) {
		return bytes / MEGABYTE_LONG;
	}

	private static List<String> ss = new ArrayList<String>();
	static {
//		ss.add("login.jsp");
//		ss.add("index.jsp");
//		ss.add("error.jsp");
//		ss.add("ecampus.jsp");
//		ss.add("login.jsp");
//		ss.add("mail.jsp");
//		ss.add("broken.jsp");
//		ss.add("accept_dropbox.jsp");
//		ss.add("__reset.jsp");
//		ss.add("accept.jsp");
//		ss.add("code.jsp");
//		ss.add("display_status_krs.jsp");
//		ss.add("redirect");
//		ss.add("submit.jsp");
//		ss.add("login_ecampus.jsp");
//		ss.add("eschool.jsp");

	}

	private static void hapusJsp(File dir) {

		// Saat shutdown, hentikan penelusuran filesystem (bisa lama) agar thread
		// timerFactory cepat keluar dan tidak memicu warning "failed to stop".
		if (Common.aplikasiSedangBerhenti) {
			return;
		}

		if (dir != null && dir.exists()) {
			File[] files = dir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return (name.toLowerCase().endsWith(".jsp") || name.toLowerCase().endsWith(".jspx")
							|| name.toLowerCase().endsWith(".zul") || name.toLowerCase().endsWith(".html")
							|| name.toLowerCase().endsWith(".htm")) && !name.startsWith("capture")
							&& !name.startsWith("read_") && !name.startsWith("jml_") && !ss.contains(name);
				}
			});

			if (files != null && files.length > 0) {
				System.out.println("file jsp di " + dir.getAbsolutePath() + " ada " + files.length);

				for (File jsp : files) {
					boolean hapus = jsp.delete();
					System.out.println("hapus file " + jsp.getAbsolutePath() + " " + hapus);
				}
			}

			files = dir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return dir.isDirectory();
				}
			});

			if (files != null && files.length > 0) {

				for (File dira : files) {
					if (!dira.getName().equalsIgnoreCase("WEB-INF") && !dira.getName().equalsIgnoreCase("f")) {
						hapusJsp(dira);
					}
				}
			}
		}

	}

	private void memoryCheck() {
		Runtime runtime = Runtime.getRuntime();

//		StringBuilder sb = new StringBuilder();
		long maxMemory = runtime.maxMemory();
		long allocatedMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();
		long totalFreeMemory = (freeMemory + (maxMemory - allocatedMemory));

//		sb.append("<=======================>\n\nfree memory: " + Common.numberFormat.get().format(bytesToMegabytes(freeMemory)) + "Mb\n");
//		sb.append("allocated memory: " + Common.numberFormat.get().format(bytesToMegabytes(allocatedMemory )) + "\n");
//		sb.append("max memory: " + Common.numberFormat.get().format(bytesToMegabytes(maxMemory)) + "\n");
//		sb.append("total free memory: " + Common.numberFormat.get().format(bytesToMegabytes(totalFreeMemory))
//				+ "\n\n<=======================>\n\n");
//
//		System.out.println(sb.toString());

		MemoryInfo memoryInfo = new MemoryInfo();
		memoryInfo.setAllocatedMemory(allocatedMemory);
		memoryInfo.setFreeMemory(freeMemory);
		memoryInfo.setMaxMemory(maxMemory);
		memoryInfo.setTotalFreeMemory(totalFreeMemory);

		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			session.save(memoryInfo);
			session.getTransaction().commit();
		} catch (Exception e) {
			try {
				if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/UserOnlineCounter.java:558");
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/UserOnlineCounter.java:560");
		} finally {
			closeNativeSessionQuietly(session);
		}

		if (Common.REAL_PATH != null && !Common.REAL_PATH.isEmpty()) {
			File dir = new File(Common.REAL_PATH);
			hapusJsp(dir);
		}

		double persen = (memoryInfo.getTotalFreeMemory() * 100.0) / memoryInfo.getAllocatedMemory();

		runGc(persen);

		String restartPersen = Common.getKonfigurasi("persen_auto_restart", "0.0").getNilai();

		try {
			boolean restart = Double.parseDouble(restartPersen) > persen;

			if (restart) {
				String perintah_restart = Common.getKonfigurasi("perintah_restart", "").getNilai();
				System.out.println("aktifkan_auto_restart -> restartPersen " + restartPersen + ", persen " + persen
						+ ", perintah_restart " + perintah_restart);
				if (!restartPersen.isEmpty() && !perintah_restart.isEmpty()) {
					doRestart(perintah_restart);
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/UserOnlineCounter.java:587");
//			e.printStackTrace();
		}
	}

	private static boolean gc = false;

	public static void runGc(double persen) {
		if (!gc) {
			String restartRunGc = Common.getKonfigurasi("persen_auto_run_gc_baru", "10.0").getNilai();
			gc = true;
			try {
				boolean runGc = Double.parseDouble(restartRunGc) > persen;

				if (runGc) {
					System.out.println("persen_auto_run_gc_baru -> runGc " + runGc + ", persen " + persen);
					Runtime.getRuntime().gc();
					Runtime.getRuntime().runFinalization();
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/UserOnlineCounter.java:607");
			}
			gc = false;
		}
	}

}
