package ais.action.master.koperasi;

import ais.common.Common;
import ais.database.model.Konfigurasi;
import ais.database.model.inventory.Toko;

/**
 * Aturan "proses otomatis setelah lewat jam 24" untuk pesanan kantin.
 *
 * <p>Dua perlakuan yang diatur terpisah:</p>
 * <ul>
 *   <li><b>Bayar otomatis</b> -- draft pesanan yang belum lunas dan tanggalnya
 *       sudah lewat hari ditandai terbayar;</li>
 *   <li><b>Layani otomatis</b> -- transaksi yang belum dilayani dan tanggalnya
 *       sudah lewat hari ditandai terlayani.</li>
 * </ul>
 *
 * <p>Keduanya <b>MATI secara bawaan</b>. Menyalakannya berarti sistem
 * menganggap uang sudah diterima / barang sudah diserahkan tanpa ada orang yang
 * mengonfirmasi, jadi itu harus keputusan sadar pengelola, bukan sesuatu yang
 * menyala sendiri.</p>
 *
 * <p><b>Pengaturan per toko mengalahkan global.</b> Nilai per toko bersifat
 * tri-state: {@code null} berarti ikut global, {@code TRUE}/{@code FALSE}
 * berarti toko itu menentukan sendiri.</p>
 */
public class OtomatisPesananUtil {

	private OtomatisPesananUtil() {
	}

	/** Kunci konfigurasi global. Sudah dipakai versi JSP sejak awal. */
	public static final String KUNCI_BAYAR = "otomatis_verifikasi_bayar_setelah_jam_24";

	/** Kunci konfigurasi global untuk layani otomatis. */
	public static final String KUNCI_LAYANI = "otomatis_layani_setelah_jam_24";

	public static boolean globalBayar() {
		return aktif(KUNCI_BAYAR);
	}

	public static boolean globalLayani() {
		return aktif(KUNCI_LAYANI);
	}

	/** Nilai efektif untuk satu toko: per toko bila diisi, selebihnya global. */
	public static boolean bayarOtomatis(Toko toko) {
		if (toko != null && toko.getOtomatisBayarSetelahJam24() != null) {
			return toko.getOtomatisBayarSetelahJam24().booleanValue();
		}
		return globalBayar();
	}

	public static boolean layaniOtomatis(Toko toko) {
		if (toko != null && toko.getOtomatisLayaniSetelahJam24() != null) {
			return toko.getOtomatisLayaniSetelahJam24().booleanValue();
		}
		return globalLayani();
	}

	private static boolean aktif(String kunci) {
		try {
			return Konfigurasi.AKTIF.equalsIgnoreCase(
					Common.getKonfigurasi(kunci, Konfigurasi.TIDAK_AKTIF).getNilai());
		} catch (Exception e) {
			// Konfigurasi tak terbaca -> perlakukan sbg MATI. Gagal ke arah
			// "tidak memproses apa-apa" jauh lebih aman daripada menandai
			// transaksi terbayar/terlayani atas dasar kondisi yang tak diketahui.
			return false;
		}
	}
}
