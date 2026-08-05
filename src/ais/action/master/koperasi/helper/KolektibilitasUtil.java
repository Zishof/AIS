package ais.action.master.koperasi.helper;

import java.util.Date;
import java.util.List;

import ais.database.model.koperasi.TransaksiKoperasi;
import ais.database.model.koperasi.TransaksiKoperasiDetail;

/**
 * <h2>KolektibilitasUtil — Penentu Otomatis Kualitas Pinjaman (Kolektibilitas)</h2>
 *
 * <p>
 * Utilitas statis ini menentukan <b>kualitas/kolektibilitas sebuah pinjaman secara otomatis</b>
 * berdasarkan berapa lama angsuran tertua menunggak (belum dibayar melewati tanggal jatuh tempo).
 * Sesuai SOM USPK BAB III tentang penanganan pinjaman bermasalah, kualitas pinjaman digolongkan
 * menjadi <i>Lancar</i>, <i>Kurang Lancar</i>, <i>Ragu-ragu</i>, dan <i>Macet</i>. Sebelumnya nilai
 * kolektibilitas diisi manual pada {@link TransaksiKoperasi#getKolektibilitas()}; util ini
 * memungkinkan pengurus memperbaruinya secara massal dan konsisten hanya dari data tunggakan yang
 * sudah ada, sehingga tidak ada penilaian yang terlewat atau tidak seragam.
 * </p>
 *
 * <h3>Ambang batas hari tunggakan</h3>
 * <p>
 * Ambang berikut mengikuti praktik umum koperasi dan dapat disesuaikan dengan ketetapan Rapat
 * Anggota bila perlu (cukup ubah konstanta di sini — satu tempat, mudah dipelihara):
 * </p>
 * <ul>
 * <li><b>Lancar</b> — tidak ada tunggakan (hari tunggakan tertua ≤ 0).</li>
 * <li><b>Kurang Lancar</b> — menunggak 1–{@value #MAKS_KURANG_LANCAR} hari.</li>
 * <li><b>Ragu-ragu</b> — menunggak {@code (}{@value #MAKS_KURANG_LANCAR}{@code +1)}–{@value #MAKS_RAGU} hari.</li>
 * <li><b>Macet</b> — menunggak lebih dari {@value #MAKS_RAGU} hari.</li>
 * </ul>
 *
 * <h3>Cara pemakaian</h3>
 * <p>
 * Pemanggil mengambil daftar angsuran ({@link TransaksiKoperasiDetail}) milik sebuah pinjaman yang
 * <b>belum dibayar</b> (yaitu {@code pembayaranAnggotaKoperasiDetail} masih kosong), lalu memanggil
 * {@link #hariTunggakanTertua(List, Date)} untuk mendapatkan lama tunggakan terparah, dan
 * {@link #dariHariTunggakan(long)} untuk mengubahnya menjadi kode kolektibilitas. Nilai tersebut lalu
 * disimpan ke {@code TransaksiKoperasi.setKolektibilitas(...)}. Fungsi bersifat tahan-banting: daftar
 * kosong atau tanggal null diperlakukan sebagai tidak menunggak (Lancar).
 * </p>
 *
 * <h3>Catatan desain</h3>
 * <p>
 * Kelas final dengan konstruktor privat (murni util, tidak untuk diinstansiasi). Tanpa akses basis
 * data — penghitungan murni di memori dari data yang sudah diambil pemanggil, sehingga ringan dan
 * mudah diuji. Kompatibel Java 1.7.
 * </p>
 *
 * @see TransaksiKoperasi#getKolektibilitas()
 */
public final class KolektibilitasUtil {

	/** Batas atas (hari) untuk golongan Kurang Lancar. */
	public static final int MAKS_KURANG_LANCAR = 90;
	/** Batas atas (hari) untuk golongan Ragu-ragu; di atas ini digolongkan Macet. */
	public static final int MAKS_RAGU = 180;

	/** Satu hari dalam milidetik. */
	private static final long SATU_HARI_MS = 1000L * 60 * 60 * 24;

	private KolektibilitasUtil() {
		// util statis: tidak untuk diinstansiasi
	}

	/**
	 * Ubah lama hari tunggakan tertua menjadi kode kolektibilitas
	 * ({@link TransaksiKoperasi#KOL_LANCAR} .. {@link TransaksiKoperasi#KOL_MACET}).
	 *
	 * @param hariTunggakan jumlah hari tunggakan angsuran tertua (≤ 0 berarti tidak menunggak)
	 * @return kode kolektibilitas
	 */
	public static String dariHariTunggakan(long hariTunggakan) {
		if (hariTunggakan <= 0) {
			return TransaksiKoperasi.KOL_LANCAR;
		}
		if (hariTunggakan <= MAKS_KURANG_LANCAR) {
			return TransaksiKoperasi.KOL_KURANG_LANCAR;
		}
		if (hariTunggakan <= MAKS_RAGU) {
			return TransaksiKoperasi.KOL_RAGU;
		}
		return TransaksiKoperasi.KOL_MACET;
	}

	/**
	 * Hitung lama tunggakan tertua (dalam hari) dari sekumpulan angsuran yang belum dibayar: yaitu
	 * selisih hari terbesar antara tanggal jatuh tempo yang sudah lewat dengan tanggal acuan
	 * ({@code now}). Angsuran tanpa tanggal jatuh tempo atau yang belum jatuh tempo diabaikan.
	 *
	 * @param angsuranBelum daftar angsuran yang belum dibayar
	 * @param now           tanggal acuan (biasanya hari ini)
	 * @return hari tunggakan tertua; 0 bila tidak ada yang menunggak
	 */
	public static long hariTunggakanTertua(List<TransaksiKoperasiDetail> angsuranBelum, Date now) {
		if (angsuranBelum == null || now == null) {
			return 0L;
		}
		long maksHari = 0L;
		for (TransaksiKoperasiDetail d : angsuranBelum) {
			try {
				if (d == null || d.getTanggal() == null) {
					continue;
				}
				Date jatuhTempo = d.getTanggal();
				if (jatuhTempo.before(now)) {
					long hari = (now.getTime() - jatuhTempo.getTime()) / SATU_HARI_MS;
					if (hari > maksHari) {
						maksHari = hari;
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/helper/KolektibilitasUtil.java:113");
				// abaikan baris bermasalah, lanjut ke berikutnya
			}
		}
		return maksHari;
	}

	/**
	 * Tentukan langsung kolektibilitas sebuah pinjaman dari daftar angsurannya yang belum dibayar.
	 *
	 * @param angsuranBelum angsuran belum dibayar milik satu pinjaman
	 * @param now           tanggal acuan
	 * @return kode kolektibilitas
	 */
	public static String hitung(List<TransaksiKoperasiDetail> angsuranBelum, Date now) {
		return dariHariTunggakan(hariTunggakanTertua(angsuranBelum, now));
	}

	/** Label ramah pengguna untuk sebuah kode kolektibilitas. */
	public static String label(String kode) {
		if (TransaksiKoperasi.KOL_KURANG_LANCAR.equals(kode)) {
			return "Kurang Lancar";
		} else if (TransaksiKoperasi.KOL_RAGU.equals(kode)) {
			return "Ragu-ragu";
		} else if (TransaksiKoperasi.KOL_MACET.equals(kode)) {
			return "Macet";
		}
		return "Lancar";
	}
}
