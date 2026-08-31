package ais.ui.util;

import java.util.Calendar;
import java.util.Date;

import org.joda.time.LocalDateTime;
import org.joda.time.Period;

/**
 * Pemformat waktu relatif ("time ago"/"time from now") berbahasa Indonesia, dibangun DI ATAS
 * {@link ais.ui.util.WaktuUtil} — berbeda dari kelas tersebut yang menyediakan primitif tanggal
 * mentah (nilai {@link Date}/{@link java.util.Calendar}, offset zona waktu, aritmetika kalender,
 * format/parse generik), kelas ini fokus SEMATA pada mengubah selisih dua titik waktu menjadi TEKS
 * yang ramah dibaca manusia (mis. {@code "Kemarin"}, {@code "5 menit lagi"}, {@code "Setahun yang
 * lalu"}), cocok untuk label timestamp pada notifikasi, linimasa aktivitas, atau daftar
 * pengumuman/jadwal.
 *
 * <h2>Cara kerja</h2>
 * <p>
 * Kedua method publik ({@link #getDayString} dan {@link #getDayStringJamMenit}) menghitung selisih
 * ({@link org.joda.time.Period}, dari pustaka Joda-Time) antara titik acuan {@code dariWaktu}
 * (biasanya "sekarang", diambil lewat {@link ais.ui.util.WaktuUtil#now()} agar konsisten dengan
 * koreksi zona waktu {@link ais.ui.util.WaktuUtil#PENAMBAHAN_WAKTU}) dan tanggal target
 * {@code date}, lalu memetakan selisih tersebut ke satu string Indonesia lewat rangkaian
 * percabangan {@code if-else} berjenjang: tahun → bulan → minggu → hari → jam/menit, dengan
 * penanganan khusus untuk nilai {@code -1}/{@code 0}/{@code 1}/{@code 2} pada tiap satuan (mis.
 * "Setahun" alih-alih "1 tahun", "Kemarin Lusa" untuk H-2, "Baru saja" untuk selisih nol). Tanda
 * (lampau vs mendatang) ditentukan dari tanda numerik hasil {@link org.joda.time.Period} itu
 * sendiri (negatif = {@code dariWaktu} setelah {@code date}, yaitu {@code date} di masa lampau).
 * </p>
 *
 * <h2>Parameter {@code waktu} opsional</h2>
 * <p>
 * Kedua method menerima parameter {@code waktu} berformat {@code "JAM.MENIT"} (mis.
 * {@code "14.30"}) yang, bila diisi, MENIMPA komponen jam/menit pada {@code date} sebelum
 * perhitungan selisih dijalankan (detik dipaksa ke {@code 1}) — berguna ketika tanggal dan jam suatu
 * peristiwa (mis. jadwal ujian) disimpan terpisah di dua kolom database. Kegagalan parsing
 * {@code waktu} (format tidak sesuai) ditangkap dan diabaikan; {@code date} asli (tanpa jam
 * ditimpa) dipakai sebagai fallback.
 * </p>
 */
public class SmartDateTimeUtil {

	/**
	 * Varian ringkas {@link #getDayString(LocalDateTime, Date, String)} dengan titik acuan
	 * "sekarang" diambil otomatis lewat {@link ais.ui.util.WaktuUtil#now()}.
	 *
	 * @param date  tanggal target yang akan dibandingkan terhadap "sekarang"
	 * @param waktu jam.menit opsional (format {@code "JAM.MENIT"}) untuk menimpa komponen waktu
	 *              {@code date}; boleh {@code null}/kosong
	 * @return string waktu relatif berbahasa Indonesia, diakhiri {@code ", "} (lihat javadoc kelas
	 *         untuk daftar lengkap variasi teksnya)
	 */
	public static String getDayString(Date date, String waktu) {
		return getDayString(ais.ui.util.WaktuUtil.now(), date, waktu);
	}

	/**
	 * Implementasi kanonik: menghitung selisih antara {@code dariWaktu} dan {@code date} (setelah
	 * {@code waktu} opsional diterapkan ke {@code date}), lalu mengembalikan string waktu relatif
	 * berbahasa Indonesia yang paling sesuai (lihat javadoc kelas untuk daftar variasi teks dan
	 * urutan percabangan tahun→bulan→minggu→hari→jam/menit).
	 *
	 * @param dariWaktu titik acuan waktu (biasanya "sekarang")
	 * @param date      tanggal target yang dibandingkan terhadap {@code dariWaktu}
	 * @param waktu     jam.menit opsional (format {@code "JAM.MENIT"}) untuk menimpa komponen waktu
	 *                  {@code date}; boleh {@code null}/kosong; kegagalan parsing diabaikan diam-diam
	 * @return string waktu relatif berbahasa Indonesia, selalu diakhiri {@code ", "}; string kosong
	 *         (sebelum akhiran) bila selisih tidak cocok dengan satu pun kondisi yang ditangani
	 */
	public static String getDayString(LocalDateTime dariWaktu, Date date, String waktu) {

		if (waktu == null || waktu.trim().isEmpty()) {
			waktu = null;
		}

		try {
			if (waktu != null) {
				Integer jamMulai = Integer.parseInt(waktu.split("\\.")[0]);
				Integer menitMulai = Integer.parseInt(waktu.split("\\.")[1]);

				Calendar calendar = WaktuUtil.getCalendar();
				calendar.setTime(date);
				calendar.set(Calendar.HOUR_OF_DAY, jamMulai);
				calendar.set(Calendar.MINUTE, menitMulai);
				calendar.set(Calendar.SECOND, 1);
				date = calendar.getTime();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/util/SmartDateTimeUtil.java:34");
		}

		LocalDateTime birthDate = new LocalDateTime(date);

		Period period = new Period(dariWaktu, birthDate);

		String s;
		if (period.getYears() < -1)
			s = Math.abs(period.getYears()) + " tahun yang lalu";
		else if (period.getYears() == -1)
			s = "Setahun yang lalu";
		else if (period.getMonths() < -1 && period.getYears() == 0)
			s = Math.abs(period.getMonths()) + " bulan yang lalu";
		else if (period.getMonths() == -1 && period.getYears() == 0)
			s = "Sebulan yang lalu";
		else if (period.getWeeks() < -1 && period.getMonths() == 0 && period.getYears() == 0)
			s = Math.abs(period.getWeeks()) + " minggu yang lalu";
		else if (period.getWeeks() == -1 && period.getMonths() == 0 && period.getYears() == 0)
			s = "Seminggu yang lalu";
		else if (period.getDays() < -2 && period.getWeeks() == 0 && period.getMonths() == 0 && period.getYears() == 0)
			s = Math.abs(period.getDays()) + " hari yang lalu";
		else if (period.getDays() == -2 && period.getWeeks() == 0 && period.getMonths() == 0 && period.getYears() == 0)
			s = "Kemarin Lusa";
		else if (period.getDays() == -1 && period.getWeeks() == 0 && period.getMonths() == 0 && period.getYears() == 0)
			s = "Kemarin";
		else if (period.getMinutes() == 0 && period.getHours() == 0 && period.getDays() == 0 && period.getWeeks() == 0
				&& period.getMonths() == 0 && period.getYears() == 0)
			s = "Baru saja";
		else if (period.getMinutes() > 0 && period.getHours() == 0 && period.getDays() == 0 && period.getWeeks() == 0
				&& period.getMonths() == 0 && period.getYears() == 0)
			s = Math.abs(period.getMinutes()) + " menit lagi";
		else if (period.getHours() > 0 && period.getDays() == 0 && period.getWeeks() == 0 && period.getMonths() == 0
				&& period.getYears() == 0)
			s = Math.abs(period.getHours()) + " jam lagi";
		else if (period.getMinutes() < 0 && period.getHours() == 0 && period.getDays() == 0 && period.getWeeks() == 0
				&& period.getMonths() == 0 && period.getYears() == 0)
			s = Math.abs(period.getMinutes()) + " menit yang lalu";
		else if (period.getHours() < 0 && period.getDays() == 0 && period.getWeeks() == 0 && period.getMonths() == 0
				&& period.getYears() == 0)
			s = Math.abs(period.getHours()) + " jam yang lalu";
		else if (period.getDays() == 0 && period.getWeeks() == 0 && period.getMonths() == 0 && period.getYears() == 0)
			s = "Hari ini";
		else if (period.getDays() == 1 && period.getWeeks() == 0 && period.getMonths() == 0 && period.getYears() == 0)
			s = "Besok";
		else if (period.getDays() == 2 && period.getWeeks() == 0 && period.getMonths() == 0 && period.getYears() == 0)
			s = "Besok Lusa";
		else if (period.getDays() > 2 && period.getWeeks() == 0 && period.getMonths() == 0 && period.getYears() == 0)
			s = Math.abs(period.getDays()) + " hari lagi";
		else if (period.getWeeks() == 1 && period.getMonths() == 0 && period.getYears() == 0)
			s = "Seminggu lagi";
		else if (period.getWeeks() > 1 && period.getMonths() == 0 && period.getYears() == 0)
			s = Math.abs(period.getWeeks()) + " minggu lagi";
		else if (period.getMonths() == 1 && period.getYears() == 0)
			s = "Sebulan lagi";
		else if (period.getMonths() > 1 && period.getYears() == 0)
			s = Math.abs(period.getMonths()) + " bulan lagi";
		else if (period.getYears() == 1)
			s = "Setahun lagi";
		else if (period.getYears() > 1)
			s = Math.abs(period.getYears()) + " tahun lagi";
		else
			s = "";
		return s + ", ";
	}

	/**
	 * Varian ringkas dari {@link #getDayString}: hanya menangani skala jam/menit (tanpa
	 * hari/minggu/bulan/tahun), menghasilkan format seperti {@code "Baru saja"}, {@code "5 menit"},
	 * atau {@code "2 jam 15 menit"} — cocok untuk konteks di mana selisih waktu dipastikan tidak
	 * melebihi satu hari (mis. hitung mundur/hitung maju sesi ujian). TIDAK diakhiri {@code ", "}
	 * seperti {@link #getDayString}, dan TIDAK membedakan arah lampau/mendatang dalam teksnya
	 * (selalu memakai nilai absolut jam/menit).
	 *
	 * @param dariWaktu titik acuan waktu
	 * @param date      tanggal/jam target
	 * @param waktu     jam.menit opsional (format {@code "JAM.MENIT"}) untuk menimpa komponen waktu
	 *                  {@code date}; boleh {@code null}/kosong; kegagalan parsing diabaikan diam-diam
	 * @return {@code "Baru saja"} bila selisih nol menit/jam; {@code "<n> menit"} bila selisih
	 *         kurang dari satu jam; selain itu {@code "<jam> jam <menit> menit"}
	 */
	public static String getDayStringJamMenit(LocalDateTime dariWaktu, Date date, String waktu) {

		if (waktu == null || waktu.trim().isEmpty()) {
			waktu = null;
		}

		try {
			if (waktu != null) {
				Integer jamMulai = Integer.parseInt(waktu.split("\\.")[0]);
				Integer menitMulai = Integer.parseInt(waktu.split("\\.")[1]);

				Calendar calendar = WaktuUtil.getCalendar();
				calendar.setTime(date);
				calendar.set(Calendar.HOUR_OF_DAY, jamMulai);
				calendar.set(Calendar.MINUTE, menitMulai);
				calendar.set(Calendar.SECOND, 1);
				date = calendar.getTime();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/util/SmartDateTimeUtil.java:119");
		}

		LocalDateTime birthDate = new LocalDateTime(date);

		Period period = new Period(dariWaktu, birthDate);

		String s;
		if (period.getMinutes() == 0 && period.getHours() == 0 && period.getDays() == 0 && period.getWeeks() == 0
				&& period.getMonths() == 0 && period.getYears() == 0)
			s = "Baru saja";
		else if (period.getMinutes() > 0 && period.getHours() == 0 && period.getDays() == 0 && period.getWeeks() == 0
				&& period.getMonths() == 0 && period.getYears() == 0)
			s = Math.abs(period.getMinutes()) + " menit";
		else
			s = Math.abs(period.getHours()) + " jam " + Math.abs(period.getMinutes()) + " menit";

		return s;
	}

}
