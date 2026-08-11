package ais.common;

import java.util.Calendar;
import java.util.Date;

/**
 * Gap-closure "Promo Pilih Hari" (menu Aturan Diskon/Promo) -- SATU sumber kebenaran dipakai
 * BERSAMA oleh dua mesin pencocokan promo yang wajib tetap sinkron:
 * <ul>
 * <li>{@code ais.action.master.koperasi.PosKantinAction.evaluasiDiskon} (ZK/JSP, checkout Kasir
 * langsung).</li>
 * <li>{@code ais.action.servlet.api.KantinHelper.evaluasiDiskonItems} (API, dipakai Electron/
 * Flutter lewat aksi {@code diskon_evaluasi}).</li>
 * </ul>
 * Pola sama persis dengan {@link ProdukKunciUnikUtil} -- satu static helper dipanggil dari banyak
 * tempat berbeda, supaya logic tidak bisa drift di antara keduanya.
 */
public final class HariAktifUtil {

	private HariAktifUtil() {
	}

	/**
	 * @param hariAktifCsv nilai {@code AturanDiskon.hariAktif} -- CSV angka ISO-8601 weekday
	 *                     ({@code 1}=Senin .. {@code 7}=Minggu, mis. {@code "1,2,3,4,5"}).
	 *                     {@code null}/kosong berarti TIDAK ada batasan hari (berlaku semua hari),
	 *                     konsisten dgn konvensi {@code tanggalMulai}/{@code tanggalSelesai} null =
	 *                     tanpa batas.
	 * @param pada         tanggal/waktu transaksi yang dicek.
	 * @return true bila promo aktif pada hari tsb (atau tidak ada batasan hari sama sekali).
	 */
	public static boolean aktifPadaHari(String hariAktifCsv, Date pada) {
		if (hariAktifCsv == null || hariAktifCsv.trim().isEmpty()) {
			return true;
		}
		if (pada == null) {
			pada = new Date();
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(pada);
		int dow = cal.get(Calendar.DAY_OF_WEEK); // java.util.Calendar: Minggu=1 .. Sabtu=7
		int iso = (dow == Calendar.SUNDAY) ? 7 : dow - 1; // konversi -> Senin=1 .. Minggu=7
		for (String tok : hariAktifCsv.split(",")) {
			if (String.valueOf(iso).equals(tok.trim())) {
				return true;
			}
		}
		return false;
	}
}
