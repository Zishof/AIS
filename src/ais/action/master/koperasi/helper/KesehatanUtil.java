package ais.action.master.koperasi.helper;

/**
 * <h2>KesehatanUtil — Penilaian Kesehatan Koperasi (Skor Berbobot &amp; Predikat)</h2>
 *
 * <p>
 * Utilitas statis ini mengubah lima aspek kesehatan usaha simpan pinjam yang masing-masing sudah
 * dinilai 0–100 (permodalan, kualitas aktiva produktif/pinjaman, likuiditas, solvabilitas, dan
 * rentabilitas) menjadi <b>satu skor kesehatan menyeluruh</b> beserta <b>predikat</b>-nya (Sehat,
 * Cukup Sehat, Dalam Pengawasan, Dalam Pengawasan Khusus). Penilaian mengikuti kerangka penilaian
 * kesehatan koperasi (SOM USPK / Permenkop): tiap aspek diberi bobot, lalu skor akhir adalah
 * penjumlahan skor tiap aspek dikalikan bobotnya. Dengan begitu koperasi memperoleh gambaran cepat
 * dan objektif atas kondisinya, sekaligus tahu aspek mana yang perlu diperbaiki.
 * </p>
 *
 * <h3>Bobot tiap aspek</h3>
 * <p>
 * Bobot berikut berjumlah 100% dan mengutamakan aspek yang paling menentukan kesehatan usaha simpan
 * pinjam (kualitas pinjaman dan permodalan). Nilai dapat disesuaikan dengan ketetapan pengawas/RAT
 * cukup dengan mengubah konstanta di satu tempat ini (mudah dipelihara):
 * </p>
 * <ul>
 * <li>{@link #BOBOT_PERMODALAN} — kecukupan modal sendiri.</li>
 * <li>{@link #BOBOT_KAP} — kualitas aktiva produktif (pinjaman lancar).</li>
 * <li>{@link #BOBOT_LIKUIDITAS} — ketersediaan kas untuk kewajiban.</li>
 * <li>{@link #BOBOT_SOLVABILITAS} — kemampuan menutup kewajiban dengan modal.</li>
 * <li>{@link #BOBOT_RENTABILITAS} — kemampuan menghasilkan keuntungan.</li>
 * </ul>
 *
 * <h3>Predikat</h3>
 * <ul>
 * <li>Skor ≥ 80 → <b>Sehat</b></li>
 * <li>60 ≤ skor &lt; 80 → <b>Cukup Sehat</b></li>
 * <li>40 ≤ skor &lt; 60 → <b>Dalam Pengawasan</b></li>
 * <li>skor &lt; 40 → <b>Dalam Pengawasan Khusus</b></li>
 * </ul>
 *
 * <h3>Catatan kejujuran &amp; desain</h3>
 * <p>
 * Penilaian ini bersifat <b>operasional</b> yang dihitung dari data simpan pinjam; aspek kualitatif
 * seperti manajemen dan jatidiri koperasi pada penilaian resmi tidak tercakup di sini karena
 * memerlukan input penilai. Kelas final berkonstruktor privat, tanpa akses basis data, ringan, dan
 * kompatibel Java 1.7.
 * </p>
 */
public final class KesehatanUtil {

	public static final int BOBOT_PERMODALAN = 25;
	public static final int BOBOT_KAP = 30;
	public static final int BOBOT_LIKUIDITAS = 20;
	public static final int BOBOT_SOLVABILITAS = 10;
	public static final int BOBOT_RENTABILITAS = 15;

	private KesehatanUtil() {
		// util statis
	}

	/**
	 * Hitung skor kesehatan menyeluruh (0–100) dari lima aspek yang masing-masing 0–100.
	 *
	 * @param permodalan   skor permodalan (0–100)
	 * @param kap          skor kualitas pinjaman (0–100)
	 * @param likuiditas   skor likuiditas (0–100)
	 * @param solvabilitas skor solvabilitas (0–100)
	 * @param rentabilitas skor rentabilitas (0–100)
	 * @return skor tertimbang 0–100
	 */
	public static double skor(int permodalan, int kap, int likuiditas, int solvabilitas, int rentabilitas) {
		double total = permodalan * BOBOT_PERMODALAN + kap * BOBOT_KAP + likuiditas * BOBOT_LIKUIDITAS
				+ solvabilitas * BOBOT_SOLVABILITAS + rentabilitas * BOBOT_RENTABILITAS;
		return total / 100.0;
	}

	/** Predikat kesehatan dari skor menyeluruh. */
	public static String predikat(double skor) {
		if (skor >= 80) {
			return "Sehat";
		}
		if (skor >= 60) {
			return "Cukup Sehat";
		}
		if (skor >= 40) {
			return "Dalam Pengawasan";
		}
		return "Dalam Pengawasan Khusus";
	}

	/** Warna (hex) yang mewakili predikat, untuk penanda visual pada dasbor. */
	public static String warna(double skor) {
		if (skor >= 80) {
			return "#16a34a"; // hijau — sehat
		}
		if (skor >= 60) {
			return "#2563eb"; // biru — cukup sehat
		}
		if (skor >= 40) {
			return "#f97316"; // oranye — dalam pengawasan
		}
		return "#dc2626"; // merah — pengawasan khusus
	}
}
