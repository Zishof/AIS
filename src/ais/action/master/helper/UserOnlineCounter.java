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
 * {@link TimerTask} inti "housekeeping" AIS: dijadwalkan berulang (lihat {@link #run()}, tidur
 * ~30 detik peka-shutdown antar eksekusi) oleh Spring timer factory ("timerFactory") sejak webapp
 * start, dan menjalankan berbagai pekerjaan latar belakang aplikasi &mdash; TIDAK khusus untuk
 * satu domain, namanya ("penghitung user online") hanya mencerminkan tugas pertamanya
 * ({@link #check()}); tugas lain yang ditambahkan kemudian (notifikasi tagihan sekolah,
 * warming-up WhatsApp/Watzap, cek kuota ujian, auto-restart, cek memori/GC, proses tagihan,
 * cek kelas les) numpang di siklus {@code run()} yang sama.
 *
 * <p><b>Setiap eksekusi {@link #run()}</b> (dibungkus {@link #runSafely(String, Runnable)} per
 * pekerjaan agar satu pekerjaan gagal tidak menghentikan yang lain): notifikasi tagihan sekolah
 * ({@code PengaturanBiaya.chekNotifikasi()}, hanya tiap {@code index} genap, dijaga flag
 * {@link #runningNotifikasiTagihanSekolah} agar tak tumpang tindih bila eksekusi sebelumnya
 * belum selesai), warming-up nomor WhatsApp Watzap (tiap 25 siklus, lihat {@link #chekWarmingUp()}),
 * {@link #ujianCheck()}, {@link #check()} (hitung user online), {@link #restartCheck()},
 * {@link #memoryCheck()} (turut memicu {@link #hapusJsp(File)} dan {@link #runGc(double)}),
 * {@code TagihanProcessor.proses()}, dan {@code PengaturanBiaya.checkKelasLes()}. Di akhir,
 * ThreadLocal berat milik thread ini dibersihkan ({@code Common.bersihkanThreadLocalThreadIni()})
 * agar tidak menyandera classloader webapp saat redeploy.</p>
 *
 * <p><b>Lifecycle/shutdown:</b> {@link #run()} dan {@link #hapusJsp(File)} memeriksa
 * {@code Common.aplikasiSedangBerhenti} berulang kali (termasuk di tengah tidur 30 detik, per
 * detik) agar task ini membatalkan diri ({@code cancel()}) dan keluar CEPAT (&le;1 detik) saat
 * Tomcat/webapp berhenti &mdash; mencegah warning "failed to stop" thread Spring timerFactory dan
 * kebocoran classloader. {@link #runSafely} juga meredam noise error "already closed"/"has been
 * closed"/dsb. yang wajar terjadi saat shutdown karena resource (pool koneksi c3p0, cache MapDB)
 * sudah ditutup lebih dulu oleh jalur shutdown lain (race antar listener) &mdash; lihat
 * {@link #pesanResourceTertutup(Throwable)}.</p>
 *
 * <p><b>State statis:</b> {@link #count} (total {@code AccessedUsers} 23 jam terakhir) dan
 * {@link #countOnline} (jumlah nama distinct pada {@code SecurityFilter.dataOnline}, session HTTP
 * aktif saat ini) dibaca oleh dasbor admin untuk menampilkan jumlah pengguna online. Field
 * {@link #mapTanyaJawab} adalah cache LRU BER-BATAS (2000 entri, bukan {@code HashMap} polos)
 * nomor WhatsApp masuk &rarr; {@link TanyaJawab} terakhir, dipakai fitur bot tanya-jawab; nomor
 * yang tersingkir dari cache memulai alur dari awal saat masuk lagi.</p>
 *
 * <p><b>Kuirk:</b> {@link #ss} (daftar nama berkas JSP yang dikecualikan dari
 * {@link #hapusJsp(File)}) seluruhnya berisi baris ter-<i>comment-out</i> &mdash; efektif KOSONG
 * saat ini, sehingga hanya nama berkas berawalan {@code capture}/{@code read_}/{@code jml_} yang
 * dikecualikan dari penghapusan otomatis berkas {@code .jsp/.jspx/.zul/.html/.htm} di
 * {@code Common.REAL_PATH} (dipicu {@link #memoryCheck()}). Method ini bukan class publik yang
 * dimaksudkan namanya ("User Online Counter") &mdash; dokumentasikan apa adanya karena mengubah
 * cakupan tugasnya berisiko memecah alur housekeeping lain yang sudah bergantung padanya.</p>
 *
 * @see TimerTask
 */
public class UserOnlineCounter extends TimerTask {

	/** Total {@link AccessedUsers} tercatat dalam 23 jam terakhir, dihitung ulang tiap siklus {@link #run()} lewat {@link #check()}. Dibaca dasbor admin. */
	public static Number count = null;
	/** Jumlah nama distinct pada {@code SecurityFilter.dataOnline} (session HTTP aktif saat ini), dihitung ulang tiap siklus {@link #run()} lewat {@link #check()}. Dibaca dasbor admin. */
	public static Number countOnline = null;

	/** Penjaga agar {@code PengaturanBiaya.chekNotifikasi()} tidak dijalankan tumpang tindih bila eksekusi sebelumnya (tiap {@code index} genap) belum selesai saat siklus timer berikutnya tiba. */
	private static volatile boolean runningNotifikasiTagihanSekolah = false;

	/**
	 * Tutup session Hibernate native (dibuka via {@code HibernateUtil.currentNativeSession()})
	 * secara menyeluruh &mdash; {@code clear()}, {@code disconnect()}, {@code close()} bila masih
	 * terbuka, lalu {@code HibernateUtil.closeSession()} &mdash; dengan setiap langkah diredam bila
	 * gagal (dicatat via {@code ErrorAuditUtil}, tak dilempar). Dipakai di {@code finally} beberapa
	 * method statis kelas ini yang membuka session sendiri (mis. {@link #check()}, {@link #doRestart}).
	 * No-op bila {@code session == null}.
	 */
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

	/**
	 * Bungkus satu pekerjaan housekeeping ({@code runnable}) dalam {@code try/catch} yang meredam
	 * dan mencatat kegagalannya tanpa mengganggu pekerjaan lain di {@link #run()}. Tidak menjalankan
	 * {@code runnable} sama sekali bila {@code Common.aplikasiSedangBerhenti} sudah {@code true}
	 * (webapp sedang shutdown). Bila terjadi {@link Throwable} dan aplikasi memang sedang berhenti,
	 * ATAU error itu dikenali sebagai "resource sudah ditutup saat shutdown" oleh
	 * {@link #pesanResourceTertutup(Throwable)}, hanya dicatat log ringkas (tanpa stack trace
	 * menakutkan); selain itu dicatat lengkap dengan stack trace ke {@code ErrorAuditUtil}.
	 *
	 * @param namaProses label pekerjaan untuk pesan log (mis. "hitung user online").
	 * @param runnable   pekerjaan yang dijalankan; exception di dalamnya tidak menjalar ke pemanggil.
	 */
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

	/**
	 * Hitung ulang {@link #count} dan {@link #countOnline} dari dua sumber berbeda. {@link #count}:
	 * jumlah baris {@link AccessedUsers} dengan {@code waktu} dalam 23 jam terakhir (session native
	 * dedikasi, ditutup di {@code finally} via {@link #closeNativeSessionQuietly}). {@link #countOnline}:
	 * jumlah NAMA distinct (bukan jumlah session) dari {@code SecurityFilter.dataOnline}
	 * (peta session HTTP aktif in-memory) &mdash; jadi satu pengguna login di beberapa
	 * tab/perangkat sekaligus tetap dihitung satu. Kedua penghitungan diredam terpisah bila gagal
	 * (dicatat via {@code ErrorAuditUtil}, nilai lama dipertahankan untuk bagian yang gagal).
	 */
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

	/**
	 * Jalankan perintah shell OS ({@code bash -l -c perintah_restart}, direktori kerja {@code /opt/})
	 * untuk MERESTART server aplikasi (dipanggil {@link #restartCheck()} pada jam terjadwal, atau
	 * {@link #memoryCheck()} saat memori bebas jatuh di bawah ambang konfigurasi). Output proses
	 * dibaca penuh sebelum proses dianggap selesai, dicetak ke {@code System.out}, dan dicatat
	 * sebagai satu baris {@link RestartLog} baru (session native dedikasi, transaksi
	 * begin/commit/rollback manual, ditutup di {@code finally}). Kegagalan menjalankan perintah OS
	 * tetap menghasilkan {@link RestartLog} (keterangan berisi pesan error), bukan dilempar ke
	 * pemanggil ({@code Common.tampilErrorJikaAdmin} dipakai untuk notifikasi admin).
	 *
	 * @param perintah_restart perintah shell yang dijalankan untuk memicu restart (dari konfigurasi {@code perintah_restart}).
	 * @return teks keterangan hasil eksekusi (perintah + output ATAU pesan error), juga yang disimpan ke {@link RestartLog}.
	 */
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

	/**
	 * Bila konfigurasi {@code aktifkan_auto_restart} aktif dan jam terjadwal ({@code auto_restart},
	 * format sama dengan {@code Common.timeFormat2}) sama persis dengan waktu sekarang, panggil
	 * {@link #doRestart(String)} dengan perintah dari konfigurasi {@code perintah_restart}.
	 * Dipanggil tiap siklus {@link #run()}; karena dicocokkan per-detik/menit (bukan rentang),
	 * restart hanya terpicu pada satu siklus timer yang kebetulan jatuh tepat di menit tersebut.
	 */
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

	/**
	 * Bersihkan entri kadaluarsa dari {@code ProsesUjianHelper.kuotaUjian} (tanda ujian daring
	 * sedang berlangsung, dipakai penegakan kuota): untuk tiap kunci di sana, ambil
	 * {@link HasilUjianMahasiswa} terkait dan hitung durasi sejak {@code mulaiPada} &mdash; bila
	 * sudah lebih dari 3 jam, kunci dianggap basi (mahasiswa dianggap sudah tidak lagi
	 * benar-benar mengerjakan) dan dihapus dari {@code kuotaUjian} agar slot kuota terbebas untuk
	 * percobaan lain. Kegagalan seluruh pengecekan diredam &amp; dicatat, tidak dilempar.
	 */
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

	/** Penghitung siklus {@link #run()} sejak instance ini dibuat; dipakai untuk menjarangkan pekerjaan berat (mis. warming-up WhatsApp tiap 25 siklus, notifikasi tagihan tiap 2 siklus) tanpa timer terpisah. Bukan {@code static} — direset ke 0 tiap kali task ini diinstansiasi ulang (mis. redeploy). */
	private long index = 0L;

	/**
	 * LRU BER-BATAS (bukan HashMap polos): key = nomor WhatsApp masuk dan entri tidak
	 * pernah dihapus — sebelumnya tumbuh monoton per nomor distinct seumur JVM
	 * (optimasi RAM Fase 1). 2000 percakapan aktif terakhir dipertahankan; nomor lama
	 * yang tersingkir memulai alur tanya-jawab dari awal.
	 */
	public static Map<String, TanyaJawab> mapTanyaJawab = java.util.Collections
			.synchronizedMap(new java.util.LinkedHashMap<String, TanyaJawab>(16, 0.75f, true) {
				private static final long serialVersionUID = 1L;

				@Override
				protected boolean removeEldestEntry(java.util.Map.Entry<String, TanyaJawab> eldest) {
					return size() > 2000;
				}
			});

	/**
	 * "Pemanasan" nomor WhatsApp terdaftar di konfigurasi {@code warming_up_code_watzap} (format
	 * {@code nomorPengirim,apiKey,numberKey,nomorAsli} dipisah {@code ;}), dipanggil tiap 25 siklus
	 * {@link #run()}. Untuk tiap entri, membuka THREAD BARU yang: tidur acak 1-120 detik (sebar
	 * beban), lalu mengambil satu {@link TanyaJawab} acak dari database (session native dedikasi,
	 * ditutup segera setelah query id) sebagai isi pesan pemanasan (fallback teks generik bila
	 * tabel kosong atau data tak bernama), lalu mengirim pesan via API Watzap
	 * ({@code https://api.watzap.id/v1/send_message}) memakai proses {@code curl} eksternal
	 * (bukan HTTP client Java) dan mencatat hasilnya ke {@link #mapTanyaJawab} untuk nomor asli
	 * terkait. Tujuannya menjaga koneksi WhatsApp Business tetap "hangat"/aktif. Setiap tingkat
	 * kegagalan (parsing config, query DB, kirim pesan) diredam &amp; dicatat terpisah agar satu
	 * nomor gagal tidak menggagalkan nomor lain; setiap thread selalu menutup session Hibernate-nya
	 * di {@code finally}.
	 */
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

	/**
	 * Satu siklus housekeeping terjadwal. Lihat ringkasan lengkap urutan pekerjaan di Javadoc
	 * class-level {@link UserOnlineCounter}. Sebelum bekerja: keluar &amp; {@code cancel()} segera
	 * bila {@code Common.aplikasiSedangBerhenti}; lalu tidur ~30 detik SAMBIL memeriksa flag itu
	 * tiap detik (peka shutdown, keluar &le;1 detik bila berhenti di tengah tidur) sebagai jeda
	 * antar-eksekusi task berulang ini. Setiap pekerjaan dibungkus {@link #runSafely(String, Runnable)}
	 * agar satu pekerjaan gagal tidak menggagalkan yang lain. Menaikkan {@link #index} di akhir dan
	 * membersihkan ThreadLocal berat thread ini.
	 */
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

	/** Konstanta konversi byte ke megabyte (basis 1024), versi {@code double}. */
	private static final double MEGABYTE = 1024.0 * 1024.0;

	/** Konversi byte ke megabyte (basis 1024, hasil pecahan). */
	public static double bytesToMegabytes(double bytes) {
		return bytes / MEGABYTE;
	}

	/** Konstanta konversi byte ke megabyte (basis 1024), versi {@code long}. */
	private static final long MEGABYTE_LONG = 1024L * 1024L;

	/** Konversi byte ke megabyte (basis 1024, hasil dibulatkan ke bawah/integer division). */
	public static long bytesToMegabytesLong(long bytes) {
		return bytes / MEGABYTE_LONG;
	}

	/**
	 * Daftar nama berkas yang DIKECUALIKAN dari penghapusan otomatis oleh {@link #hapusJsp(File)}.
	 * Kuirk: seluruh baris pengisi daftar ini ada dalam bentuk komentar ({@code //}) &mdash; daftar
	 * ini EFEKTIF KOSONG saat ini. Dibiarkan apa adanya (bukan bug baru) sebagai riwayat konfigurasi
	 * sebelumnya; pengecualian penghapusan yang benar-benar aktif saat ini hanya berdasar AWALAN
	 * nama berkas ({@code capture}/{@code read_}/{@code jml_}), lihat {@link #hapusJsp(File)}.
	 */
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

	/**
	 * Hapus REKURSIF berkas {@code .jsp/.jspx/.zul/.html/.htm} di bawah {@code dir} (kecuali yang
	 * berawalan {@code capture}/{@code read_}/{@code jml_}, atau ada di daftar {@link #ss} yang
	 * saat ini efektif kosong) &mdash; pembersihan berkas sementara/temporer yang tertinggal di
	 * webapp (mis. hasil ekspor/print sementara). Turun ke subdirektori kecuali {@code WEB-INF}
	 * dan {@code f}. Dipanggil dari {@link #memoryCheck()} terhadap {@code Common.REAL_PATH}.
	 * Berhenti (tidak menelusuri) segera bila {@code Common.aplikasiSedangBerhenti} agar shutdown
	 * tidak tertahan penelusuran filesystem yang bisa lama. No-op bila {@code dir} null/tidak ada.
	 *
	 * @param dir direktori yang ditelusuri &amp; dibersihkan; boleh {@code null}.
	 */
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

	/**
	 * Ambil snapshot memori JVM ({@code Runtime.maxMemory/totalMemory/freeMemory}), simpan sebagai
	 * satu baris {@link MemoryInfo} baru (session native dedikasi, transaksi begin/commit/rollback
	 * manual, ditutup di {@code finally}) untuk riwayat monitoring, lalu memicu
	 * {@link #hapusJsp(File)} pada {@code Common.REAL_PATH} bila terkonfigurasi. Menghitung persentase
	 * memori bebas total terhadap memori teralokasi, memanggil {@link #runGc(double)} dengannya, dan
	 * &mdash; bila persentase itu di bawah ambang konfigurasi {@code persen_auto_restart} DAN
	 * konfigurasi restart terisi &mdash; memicu {@link #doRestart(String)} sebagai upaya pemulihan
	 * paksa atas tekanan memori. Kegagalan simpan {@link MemoryInfo} maupun parsing ambang restart
	 * diredam &amp; dicatat, tidak dilempar.
	 */
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

	/** Penjaga re-entrancy sederhana (bukan {@code volatile}/atomik) agar {@link #runGc(double)} tidak memicu GC bertumpuk saat dipanggil beruntun. */
	private static boolean gc = false;

	/**
	 * Bila persentase memori bebas ({@code persen}) di bawah ambang konfigurasi
	 * {@code persen_auto_run_gc_baru} (default 10.0), paksa jalankan
	 * {@code Runtime.getRuntime().gc()} dan {@code runFinalization()} sebagai upaya melepas memori
	 * sebelum kondisi memori makin kritis. Dijaga flag {@link #gc} agar tidak tumpang tindih;
	 * kegagalan parsing ambang konfigurasi dicatat &amp; diredam.
	 *
	 * @param persen persentase memori bebas saat ini terhadap memori teralokasi (dari {@link #memoryCheck()}).
	 */
	public static void runGc(double persen) {
		if (!gc) {
			ais.database.model.Konfigurasi konfigurasiGc = Common.getKonfigurasi("persen_auto_run_gc_baru", "10.0");
			String restartRunGc = konfigurasiGc == null || konfigurasiGc.getNilai() == null
					? "10.0" : konfigurasiGc.getNilai();
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
