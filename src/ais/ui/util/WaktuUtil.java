package ais.ui.util;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

import org.joda.time.LocalDateTime;

import ais.common.Common;

/**
 * Utilitas dasar tanggal/waktu AIS: sumber kebenaran tunggal untuk "sekarang" ({@link #getCalendar()}/
 * {@link #getDate()}) yang memperhitungkan pergeseran zona waktu per instalasi kampus, ditambah
 * sekumpulan method turunan untuk tanggal relatif (kemarin, besok, tahun lalu, dst.) dan
 * pemformatan/parsing tanggal generik.
 *
 * <h2>Mekanisme pergeseran zona waktu ({@link #PENAMBAHAN_WAKTU})</h2>
 * <p>
 * Server aplikasi diasumsikan berjalan pada zona waktu dasar WIB (Asia/Jakarta). Karena Indonesia
 * memiliki tiga zona waktu (WIB/WITA/WIT) dan satu instalasi AIS dapat melayani kampus di zona mana
 * pun, kelas ini menyediakan mekanisme koreksi manual berbasis konfigurasi:
 * {@link #PENAMBAHAN_WAKTU} (dimuat ulang dari konfigurasi {@code "PENAMBAHAN_WAKTU"} lewat
 * {@link #reinit()}) menyimpan jumlah JAM yang ditambahkan ke waktu server saat ini untuk
 * menghasilkan waktu lokal kampus: {@code 0} = WIB/Asia-Jakarta (tanpa koreksi), {@code 1} =
 * WITA/Asia-Makassar (+1 jam), {@code 2} = WIT/Asia-Jayapura (+2 jam) — lihat
 * {@link #gteTimezoneName()} untuk pemetaan nilai ke nama zona waktu IANA. SELURUH method tanggal
 * relatif di kelas ini ({@link #kemarin()}, {@link #besok()}, {@link #tahunLalu()}, dst.) dibangun
 * di atas {@link #getCalendar()}, sehingga otomatis konsisten dengan koreksi zona waktu ini tanpa
 * perlu menghitung ulang secara manual di tiap pemanggil.
 * </p>
 *
 * <p>
 * <b>Catatan</b> — {@link #PENAMBAHAN_WAKTU} adalah bidang statis (state global level-JVM, bukan
 * per-request/per-tenant), sehingga nilainya berlaku untuk seluruh aplikasi dalam satu proses; pada
 * deployment multi-tenant, nilai ini perlu di-{@link #reinit()} ulang sesuai konteks tenant yang
 * sedang aktif bila tiap tenant punya zona waktu berbeda.
 * </p>
 *
 * <h2>Hubungan dengan {@link SmartDateTimeUtil}</h2>
 * <p>
 * Kelas ini menyediakan primitif tanggal/waktu tingkat rendah (nilai {@link Date}/{@link Calendar}
 * mentah, offset zona waktu, format/parse generik). Kelas saudaranya,
 * {@link ais.ui.util.SmartDateTimeUtil}, dibangun DI ATAS kelas ini (memanggil
 * {@link #now()}/{@link #getCalendar()} sebagai titik acuan "sekarang") untuk menghasilkan STRING
 * WAKTU RELATIF berbahasa Indonesia yang ramah-pengguna (mis. "Kemarin", "5 menit lagi", "Setahun
 * yang lalu") — cocok untuk label timestamp di notifikasi/aktivitas, berbeda dari kelas ini yang
 * fokus pada nilai tanggal mentah dan aritmetika kalender.
 * </p>
 */
public class WaktuUtil {

	/**
	 * Jumlah jam koreksi zona waktu yang ditambahkan ke waktu server (WIB) untuk menghasilkan waktu
	 * lokal kampus: {@code 0}=WIB, {@code 1}=WITA, {@code 2}=WIT. Dimuat dari konfigurasi
	 * {@code "PENAMBAHAN_WAKTU"} lewat {@link #reinit()}; nilai default sebelum {@link #reinit()}
	 * dipanggil adalah {@code 0} (WIB, tanpa koreksi).
	 */
	public static int PENAMBAHAN_WAKTU = 0;

	/**
	 * Menerjemahkan {@link #PENAMBAHAN_WAKTU} menjadi nama zona waktu IANA yang sesuai.
	 *
	 * @return {@code "Asia/Jakarta"} (WIB) untuk nilai {@code 0} atau nilai lain di luar 1-2,
	 *         {@code "Asia/Makassar"} (WITA) untuk nilai {@code 1}, {@code "Asia/Jayapura"} (WIT)
	 *         untuk nilai {@code 2}
	 */
	public static String gteTimezoneName() {
		String timeZone = "Asia/Jakarta";
		if (PENAMBAHAN_WAKTU == 1) {
			timeZone = "Asia/Makassar";
		} else if (PENAMBAHAN_WAKTU == 2) {
			timeZone = "Asia/Jayapura";
		}
		return timeZone;
	}

	/**
	 * Memuat ulang {@link #PENAMBAHAN_WAKTU} dari konfigurasi runtime {@code "PENAMBAHAN_WAKTU"}
	 * (dengan nilai saat ini sebagai default bila kunci belum ada di konfigurasi). Kegagalan parsing
	 * ditangkap dan dicatat ke {@code ErrorAuditUtil}, tidak dilempar ke pemanggil — nilai
	 * {@link #PENAMBAHAN_WAKTU} sebelumnya dipertahankan bila gagal.
	 */
	public static void reinit() {
		try {
			PENAMBAHAN_WAKTU = Integer
					.parseInt(Common.getKonfigurasi("PENAMBAHAN_WAKTU", PENAMBAHAN_WAKTU + "").getNilai());
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/util/WaktuUtil.java:32");
		}
	}

	/**
	 * Titik acuan tunggal "sekarang" untuk seluruh kelas ini: mengambil waktu sistem
	 * ({@link Calendar#getInstance()}) lalu menambahkan koreksi jam {@link #PENAMBAHAN_WAKTU} bila
	 * bukan nol, menghasilkan waktu lokal kampus (lihat javadoc kelas untuk penjelasan mekanisme
	 * zona waktu). Seluruh method tanggal relatif di kelas ini dibangun di atas method ini.
	 *
	 * @return kalender pada waktu "sekarang" versi kampus (waktu server + koreksi zona waktu)
	 */
	public static Calendar getCalendar() {
		Calendar calendar = Calendar.getInstance();
		if (PENAMBAHAN_WAKTU != 0) {
			calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) + PENAMBAHAN_WAKTU);
		}
		return calendar;
	}


	/** @return tanggal dua tahun sebelum {@link #getCalendar()} "sekarang". */
	public static Date duaTahunLalu() {
		Calendar calendar = WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 2);
		return calendar.getTime();
	}

	/** @return tanggal setahun sebelum {@link #getCalendar()} "sekarang". */
	public static Date tahunLalu() {
		Calendar calendar = WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);
		return calendar.getTime();
	}

	/**
	 * Memformat {@code tanggal} sebagai teks lengkap (mis. "Senin, 1 Januari 2026") lewat pola
	 * {@code Common.dateFormat51}.
	 *
	 * @param tanggal tanggal yang diformat
	 * @return representasi teks lengkap dari {@code tanggal}
	 */
	public static String formatTanggalLengkap(Date tanggal) {
		return Common.dateFormat51.get().format(tanggal);
	}

	/** @return tanggal setahun setelah {@link #getCalendar()} "sekarang". */
	public static Date tahunAkandatang() {
		Calendar calendar = WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1);
		return calendar.getTime();
	}

	/** @return tanggal sehari sebelum {@link #getCalendar()} "sekarang" (kemarin). */
	public static Date kemarin() {
		Calendar calendar = WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
		return calendar.getTime();
	}

	/** @return tanggal sehari setelah {@link #getCalendar()} "sekarang" (besok). */
	public static Date besok() {
		Calendar calendar = WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		return calendar.getTime();
	}

	/** @return tanggal seminggu setelah {@link #getCalendar()} "sekarang". */
	public static Date minggudepan() {
		Calendar calendar = WaktuUtil.getCalendar();
		calendar.set(Calendar.WEEK_OF_MONTH, calendar.get(Calendar.WEEK_OF_MONTH) + 1);
		return calendar.getTime();
	}

	/** @return tanggal sebulan setelah {@link #getCalendar()} "sekarang". */
	public static Date bulandepan() {
		Calendar calendar = WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
		return calendar.getTime();
	}

	/** @return tanggal dua hari setelah {@link #getCalendar()} "sekarang" (besok lusa). */
	public static Date besoklusa() {
		Calendar calendar = WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 2);
		return calendar.getTime();
	}

	/**
	 * Seperti {@link #besok()}, tetapi dihitung relatif terhadap {@code date} yang diberikan
	 * (bukan terhadap "sekarang"); jam/menit/detik tetap mengikuti waktu saat ini karena kalender
	 * awal diisi dari {@link #getCalendar()} sebelum {@code setTime(date)} menimpa tanggalnya.
	 *
	 * @param date tanggal acuan
	 * @return tanggal sehari setelah {@code date}
	 */
	public static Date besok(Date date) {
		Calendar calendar = WaktuUtil.getCalendar();
		calendar.setTime(date);
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		return calendar.getTime();
	}

	/** @return waktu "sekarang" versi kampus, lihat {@link #getCalendar()}. */
	public static Date getDate() {
		return WaktuUtil.getCalendar().getTime();
	}

	/** @return waktu "sekarang" versi kampus sebagai {@link org.joda.time.LocalDateTime} (Joda-Time). */
	public static LocalDateTime sekarang() {
		return new LocalDateTime(WaktuUtil.getDate());
	}

	/** Alias berbahasa Inggris dari {@link #sekarang()}; identik secara fungsional. */
	public static LocalDateTime now() {
		return new LocalDateTime(WaktuUtil.getDate());
	}

	/**
	 * Memformat {@code tanggal} sesuai pola {@code format} (sintaks {@link DateTimeFormatter},
	 * mis. {@code "dd-MM-yyyy HH:mm"}) memakai zona waktu default JVM ({@code ZoneId.systemDefault()}
	 * — CATATAN: ini BUKAN zona waktu terkoreksi {@link #PENAMBAHAN_WAKTU}, karena {@code tanggal}
	 * di sini adalah instan absolut yang diformat ulang, bukan dihitung dari {@link #getCalendar()}).
	 *
	 * @param tanggal tanggal yang diformat; hasil kosong bila {@code null}
	 * @param format  pola format {@link DateTimeFormatter}; hasil kosong bila {@code null}/kosong
	 * @return teks tanggal terformat, atau string kosong bila {@code tanggal}/{@code format} tidak
	 *         valid
	 */
	public static String formatDate(Date tanggal, String format) {
		if (tanggal == null || format == null || format.isEmpty())
			return "";

		// DateTimeFormatter thread-safe, aman jika ingin dijadikan static final di
		// level class
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

		return tanggal.toInstant().atZone(ZoneId.systemDefault()).format(formatter);
	}
	
	
	/**
	 * Mengurai teks {@code tanggal} menjadi {@link Date} sesuai pola {@code format} (sintaks
	 * {@link SimpleDateFormat}, mis. {@code "dd-MM-yyyy"}) — CATATAN: memakai
	 * {@link SimpleDateFormat}, berbeda dari {@link #formatDate(Date, String)} yang memakai
	 * {@link DateTimeFormatter}; keduanya menerima sintaks pola yang serupa tetapi bukan API yang
	 * sama.
	 *
	 * @param tanggal teks tanggal yang diurai
	 * @param format  pola {@link SimpleDateFormat}
	 * @return hasil parsing, atau {@code null} bila {@code tanggal}/{@code format} tidak valid atau
	 *         parsing gagal (galat dicetak ke {@code System.err}, tidak dilempar ke pemanggil)
	 */
	public static Date parseDate(String tanggal, String format) {
	    if (tanggal == null || format == null || format.isEmpty()) {
	        return null;
	    }

	    try {
	        // Membuat formatter
	        SimpleDateFormat sdf = new SimpleDateFormat(format);
	        // Mengubah teks menjadi Date
	        return sdf.parse(tanggal); 
	    } catch (Exception e) {
	        // Jika format tanggal salah/tidak sesuai, kembalikan null atau lempar exception
	        System.err.println("Gagal parsing tanggal: " + e.getMessage());
	        return null; 
	    }
	}
}
