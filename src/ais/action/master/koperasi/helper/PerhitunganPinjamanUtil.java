package ais.action.master.koperasi.helper;

import ais.database.model.koperasi.ProdukKoperasi;

/**
 * <h2>PerhitunganPinjamanUtil — Penghitung Jadwal Angsuran Pinjaman (Flat / Menurun / Anuitas)</h2>
 *
 * <p>
 * Utilitas statis ini menghasilkan <b>jadwal angsuran</b> sebuah pinjaman: berapa bagian pokok dan
 * berapa bagian bunga pada tiap periode cicilan. Ia dipakai ulang oleh proses pembentukan
 * {@code TransaksiKoperasiDetail} saat pinjaman dibuat, sehingga logika perhitungan bunga terpusat
 * di satu tempat dan mudah dipelihara. Metode perhitungan mengikuti pilihan pada produk pinjaman
 * ({@link ProdukKoperasi#getMetodeBunga()}), sesuai ragam yang diatur SOM USPK: bunga <i>flat</i>
 * (tetap), bunga <i>menurun</i> (efektif), dan <i>anuitas</i>.
 * </p>
 *
 * <h3>Kompatibilitas mundur (perilaku lama tetap sama)</h3>
 * <p>
 * Metode <b>FLAT adalah default</b> dan hasilnya <b>identik</b> dengan perhitungan historis modul:
 * pokok per periode = total pokok ÷ jumlah angsuran, dan bunga per periode = total margin flat ÷
 * jumlah angsuran (di mana total margin flat = {@code nilai × bunga% × jangka waktu}, sebagaimana
 * dihitung {@code TransaksiKoperasi.getMargin()}). Dengan demikian, produk lama yang tidak mengisi
 * metode bunga tetap terhitung persis seperti sebelumnya — opsi menurun/anuitas hanya berlaku bila
 * produk secara eksplisit memilihnya.
 * </p>
 *
 * <h3>Rumus tiap metode</h3>
 * <ul>
 * <li><b>Flat</b>: pokok<sub>i</sub> = P ÷ n; bunga<sub>i</sub> = M<sub>flat</sub> ÷ n. Total bunga
 * tetap, cicilan rata.</li>
 * <li><b>Menurun (efektif)</b>: pokok<sub>i</sub> = P ÷ n; bunga<sub>i</sub> = sisa pokok awal
 * periode × r, dengan r = tingkat bunga per periode. Bunga mengecil tiap periode karena dihitung
 * dari sisa pokok.</li>
 * <li><b>Anuitas</b>: total cicilan tetap, dihitung dari rumus anuitas
 * A = P·r·(1+r)<sup>n</sup> ÷ ((1+r)<sup>n</sup>−1); bunga<sub>i</sub> = sisa pokok × r; pokok<sub>i</sub>
 * = A − bunga<sub>i</sub>. Komposisi pokok-bunga berubah, total cicilan konstan.</li>
 * </ul>
 *
 * <h3>Catatan penafsiran tingkat bunga</h3>
 * <p>
 * Nilai {@code bungaPerPeriode} adalah tingkat bunga per periode angsuran dalam persen — konsisten
 * dengan cara {@code TransaksiKoperasi.getMargin()} memakai {@code bunga} (per bulan) dikalikan
 * jumlah bulan. Untuk pinjaman bulanan (kasus paling umum) hal ini tepat; untuk durasi lain,
 * menurun/anuitas merupakan pendekatan yang wajar sementara metode FLAT tetap akurat seperti semula.
 * Fungsi bersifat tahan-banting: jumlah angsuran ≤ 0 menghasilkan jadwal kosong, dan r ≤ 0 pada
 * anuitas ditangani sebagai pembagian rata.
 * </p>
 *
 * <p>
 * Kelas final dengan konstruktor privat (tidak untuk diinstansiasi). Kompatibel Java 1.7.
 * </p>
 *
 * @see ProdukKoperasi#getMetodeBunga()
 */
public final class PerhitunganPinjamanUtil {

	private PerhitunganPinjamanUtil() {
		// util statis: tidak untuk diinstansiasi
	}

	/**
	 * Hitung jadwal angsuran.
	 *
	 * @param pokokTotal      total pokok pinjaman (P)
	 * @param marginTotalFlat total bunga versi flat (M<sub>flat</sub>), dipakai hanya untuk metode FLAT
	 *                        agar hasilnya identik dengan perhitungan lama
	 * @param bungaPerPeriode tingkat bunga per periode (persen), dipakai untuk MENURUN &amp; ANUITAS
	 * @param jumlahAngsur    jumlah periode cicilan (n)
	 * @param metode          {@link ProdukKoperasi#METODE_FLAT}, {@link ProdukKoperasi#METODE_MENURUN},
	 *                        atau {@link ProdukKoperasi#METODE_ANUITAS}
	 * @return larik {@code [n][2]}, elemen ke-i berisi {@code {pokok, bunga}} periode ke-(i+1);
	 *         larik kosong bila {@code jumlahAngsur <= 0}
	 */
	public static double[][] hitungJadwal(double pokokTotal, double marginTotalFlat, double bungaPerPeriode,
			int jumlahAngsur, String metode) {
		if (jumlahAngsur <= 0) {
			return new double[0][2];
		}
		double[][] out = new double[jumlahAngsur][2];
		String m = metode == null || metode.trim().isEmpty() ? ProdukKoperasi.METODE_FLAT : metode;
		double r = bungaPerPeriode / 100.0;
		double pokokPer = pokokTotal / jumlahAngsur;

		if (ProdukKoperasi.METODE_MENURUN.equals(m)) {
			double sisa = pokokTotal;
			for (int i = 0; i < jumlahAngsur; i++) {
				double bunga = sisa * r;
				out[i][0] = pokokPer;
				out[i][1] = bunga < 0 ? 0.0 : bunga;
				sisa -= pokokPer;
			}
		} else if (ProdukKoperasi.METODE_ANUITAS.equals(m)) {
			double sisa = pokokTotal;
			double angsuran;
			if (r <= 0) {
				angsuran = pokokPer;
			} else {
				double f = Math.pow(1.0 + r, jumlahAngsur);
				angsuran = pokokTotal * r * f / (f - 1.0);
			}
			for (int i = 0; i < jumlahAngsur; i++) {
				double bunga = sisa * r;
				double pokok = angsuran - bunga;
				if (pokok > sisa) {
					pokok = sisa;
				}
				if (pokok < 0) {
					pokok = 0.0;
				}
				out[i][0] = pokok;
				out[i][1] = bunga < 0 ? 0.0 : bunga;
				sisa -= pokok;
			}
		} else {
			// FLAT — identik dengan perhitungan historis: pokok rata + bunga flat rata.
			double marginPer = marginTotalFlat / jumlahAngsur;
			for (int i = 0; i < jumlahAngsur; i++) {
				out[i][0] = pokokPer;
				out[i][1] = marginPer;
			}
		}
		return out;
	}

	/**
	 * Total bunga sepanjang tenor untuk sebuah metode — berguna untuk ringkasan/estimasi. Untuk FLAT
	 * sama dengan {@code marginTotalFlat}; untuk menurun/anuitas menjumlahkan bunga tiap periode.
	 */
	public static double totalBunga(double pokokTotal, double marginTotalFlat, double bungaPerPeriode,
			int jumlahAngsur, String metode) {
		double[][] jadwal = hitungJadwal(pokokTotal, marginTotalFlat, bungaPerPeriode, jumlahAngsur, metode);
		double total = 0.0;
		for (int i = 0; i < jadwal.length; i++) {
			total += jadwal[i][1];
		}
		return total;
	}
}
