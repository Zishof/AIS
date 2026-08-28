package ais.action.master.koperasi;

import ais.common.Common;
import ais.database.model.Konfigurasi;

/**
 * Satu sumber kebijakan koreksi transaksi POS.
 *
 * <p>Global aktif selalu menang. Bila global tidak aktif, izin ditentukan oleh
 * konfigurasi toko yang bersangkutan. Nilai disimpan pada tabel konfigurasi
 * yang sudah ada agar fitur ini tidak membutuhkan perubahan skema tabel toko.</p>
 */
public final class KoreksiTransaksiUtil {

	public static final String KUNCI_GLOBAL = "pos_izinkan_edit_transaksi_global";
	private static final String AWAL_KUNCI_TOKO = "pos_izinkan_edit_transaksi_toko_";

	private KoreksiTransaksiUtil() {
	}

	public static boolean globalAktif() {
		return aktif(KUNCI_GLOBAL);
	}

	public static String kunciToko(Long tokoId) {
		return AWAL_KUNCI_TOKO + (tokoId == null ? "0" : tokoId.toString());
	}

	public static boolean tokoAktif(Long tokoId) {
		return tokoId != null && aktif(kunciToko(tokoId));
	}

	public static boolean efektif(Long tokoId) {
		return globalAktif() || tokoAktif(tokoId);
	}

	private static boolean aktif(String kunci) {
		try {
			return Konfigurasi.AKTIF.equalsIgnoreCase(
					Common.getKonfigurasi(kunci, Konfigurasi.TIDAK_AKTIF).getNilai());
		} catch (Exception e) {
			return false;
		}
	}
}
