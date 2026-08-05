package ais.action.master.koperasi.helper;

import java.util.Calendar;
import java.util.Date;

import ais.database.model.koperasi.ProdukKoperasi;

/**
 * <h2>BungaSimpananUtil — Mesin Perhitungan Bunga Simpanan (Saldo Terendah / Harian / Rata-rata)</h2>
 *
 * <p>
 * Utilitas statis ini menghitung <b>bunga (jasa) simpanan</b> anggota untuk satu bulan dengan tiga
 * metode yang diakui dalam pengelolaan koperasi simpan pinjam (SOM USPK): <b>saldo terendah</b>,
 * <b>saldo harian</b>, dan <b>saldo rata-rata</b>. Perbedaan ketiganya semata pada dasar saldo yang
 * dikalikan suku bunga, sehingga hasilnya bisa berbeda meski suku bunga sama. Dengan menyediakan
 * ketiganya di satu tempat, koperasi cukup menetapkan metode pada masing-masing produk simpanan,
 * lalu seluruh laporan dan simulasi memakai perhitungan yang konsisten dan mudah ditelusuri.
 * </p>
 *
 * <h3>Rumus tiap metode</h3>
 * <p>
 * Misalkan suku bunga setahun {@code b%} (jadi {@code r = b/100}), jumlah hari dalam bulan {@code N},
 * dan {@code saldo[i]} adalah saldo pada hari ke-<i>i</i> (1..N):
 * </p>
 * <ul>
 * <li><b>Saldo Terendah</b> — {@code min(saldo) × r ÷ 12}. Paling konservatif: hanya menghargai saldo
 * terkecil sepanjang bulan, sehingga penarikan di tengah bulan sangat menekan bunga.</li>
 * <li><b>Saldo Harian</b> — {@code (Σ saldo[i]) × r ÷ 365}. Paling adil: tiap hari saldo mendapat
 * bunga sesuai lamanya mengendap, sehingga setoran/penarikan langsung berpengaruh proporsional.</li>
 * <li><b>Saldo Rata-rata</b> — {@code rata-rata(saldo) × r ÷ 12}. Jalan tengah yang lazim dan mudah
 * dipahami anggota; mendekati metode harian bila mutasi tersebar merata.</li>
 * </ul>
 *
 * <h3>Rekonstruksi saldo harian</h3>
 * <p>
 * Karena saldo simpanan berubah tiap ada setoran atau penarikan, {@link #saldoHarian(double, int,
 * int[], double[])} menyusun deret saldo harian dari <i>saldo awal bulan</i> ditambah mutasi
 * bertanggal (positif = setoran, negatif = penarikan). Mutasi yang terjadi pada suatu hari akan
 * mengubah saldo mulai hari itu hingga akhir bulan. Saldo tidak dibiarkan negatif agar bunga tidak
 * ikut negatif akibat data yang tidak wajar.
 * </p>
 *
 * <h3>Desain &amp; ketahanan</h3>
 * <p>
 * Kelas ini <b>final</b> berkonstruktor privat (murni util), tidak menyentuh basis data, hemat memori
 * (hanya sebuah array {@code double} sepanjang jumlah hari), dan kompatibel Java 1.7. Konstanta metode
 * mengacu langsung ke {@link ProdukKoperasi} agar tidak ada duplikasi nilai. Seluruh masukan
 * di-guard: array null, panjang tak sama, indeks hari di luar rentang, dan bulan kosong ditangani
 * dengan aman sehingga tidak pernah melempar kesalahan.
 * </p>
 *
 * @see ProdukKoperasi#getMetodeBungaSimpanan()
 * @see ProdukKoperasi#getBungaSimpananPersen()
 */
public final class BungaSimpananUtil {

	/** Basis hari setahun untuk metode saldo harian. */
	public static final int HARI_SETAHUN = 365;
	/** Basis bulan setahun untuk metode saldo terendah/rata-rata. */
	public static final int BULAN_SETAHUN = 12;

	private BungaSimpananUtil() {
		// util statis
	}

	/** Jumlah hari kalender pada bulan yang memuat tanggal {@code acuan}. */
	public static int jumlahHariBulan(Date acuan) {
		Calendar c = Calendar.getInstance();
		if (acuan != null) {
			c.setTime(acuan);
		}
		return c.getActualMaximum(Calendar.DAY_OF_MONTH);
	}

	/**
	 * Susun saldo tiap hari (indeks 0 = hari ke-1) dari saldo awal bulan ditambah mutasi bertanggal.
	 *
	 * @param saldoAwal  saldo pada awal bulan (sebelum hari ke-1)
	 * @param jumlahHari jumlah hari dalam bulan (minimal 1)
	 * @param hariMutasi hari-ke (1..jumlahHari) tiap mutasi; boleh {@code null}
	 * @param nominal    nominal bertanda tiap mutasi (+setor/-tarik), sejajar {@code hariMutasi}
	 * @return array saldo harian sepanjang {@code jumlahHari}
	 */
	public static double[] saldoHarian(double saldoAwal, int jumlahHari, int[] hariMutasi, double[] nominal) {
		int n = jumlahHari < 1 ? 1 : jumlahHari;
		double[] saldo = new double[n];
		for (int i = 0; i < n; i++) {
			saldo[i] = saldoAwal;
		}
		if (hariMutasi != null && nominal != null) {
			int m = Math.min(hariMutasi.length, nominal.length);
			for (int k = 0; k < m; k++) {
				int hari = hariMutasi[k];
				if (hari < 1) {
					hari = 1;
				}
				if (hari > n) {
					hari = n;
				}
				for (int i = hari - 1; i < n; i++) {
					saldo[i] += nominal[k];
				}
			}
		}
		for (int i = 0; i < n; i++) {
			if (saldo[i] < 0) {
				saldo[i] = 0;
			}
		}
		return saldo;
	}

	/** Saldo terendah sepanjang bulan. */
	public static double saldoTerendah(double[] saldoHarian) {
		if (saldoHarian == null || saldoHarian.length == 0) {
			return 0.0;
		}
		double min = saldoHarian[0];
		for (int i = 1; i < saldoHarian.length; i++) {
			if (saldoHarian[i] < min) {
				min = saldoHarian[i];
			}
		}
		return min < 0 ? 0.0 : min;
	}

	/** Saldo rata-rata sepanjang bulan. */
	public static double saldoRataRata(double[] saldoHarian) {
		if (saldoHarian == null || saldoHarian.length == 0) {
			return 0.0;
		}
		double total = 0.0;
		for (int i = 0; i < saldoHarian.length; i++) {
			total += saldoHarian[i];
		}
		return total / saldoHarian.length;
	}

	/** Jumlah seluruh saldo harian (dasar metode saldo harian). */
	public static double totalSaldoHarian(double[] saldoHarian) {
		if (saldoHarian == null) {
			return 0.0;
		}
		double total = 0.0;
		for (int i = 0; i < saldoHarian.length; i++) {
			total += saldoHarian[i];
		}
		return total;
	}

	/**
	 * Hitung bunga simpanan satu bulan sesuai metode.
	 *
	 * @param metode              salah satu konstanta {@code ProdukKoperasi.BUNGA_SIMPANAN_*}
	 * @param saldoHarian         deret saldo harian dari {@link #saldoHarian(double, int, int[], double[])}
	 * @param bungaTahunanPersen  suku bunga setahun dalam persen
	 * @return nominal bunga bulan tersebut (tak pernah negatif)
	 */
	public static double hitungBunga(String metode, double[] saldoHarian, double bungaTahunanPersen) {
		double r = bungaTahunanPersen / 100.0;
		if (r <= 0) {
			return 0.0;
		}
		double bunga;
		if (ProdukKoperasi.BUNGA_SIMPANAN_SALDO_TERENDAH.equals(metode)) {
			bunga = saldoTerendah(saldoHarian) * r / BULAN_SETAHUN;
		} else if (ProdukKoperasi.BUNGA_SIMPANAN_SALDO_HARIAN.equals(metode)) {
			bunga = totalSaldoHarian(saldoHarian) * r / HARI_SETAHUN;
		} else {
			bunga = saldoRataRata(saldoHarian) * r / BULAN_SETAHUN;
		}
		return bunga < 0 ? 0.0 : bunga;
	}

	/** Label ramah-baca untuk sebuah metode bunga simpanan. */
	public static String label(String metode) {
		if (ProdukKoperasi.BUNGA_SIMPANAN_SALDO_TERENDAH.equals(metode)) {
			return "Saldo Terendah";
		}
		if (ProdukKoperasi.BUNGA_SIMPANAN_SALDO_HARIAN.equals(metode)) {
			return "Saldo Harian";
		}
		return "Saldo Rata-rata";
	}
}
