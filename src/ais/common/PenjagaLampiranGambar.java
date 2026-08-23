package ais.common;

/**
 * <h3>Penegakan batas lampiran gambar di sisi server.</h3>
 *
 * <p>Klien POS Desktop/Android sudah mengecilkan gambar ke bawah 500 KB sebelum mengirim
 * ({@code services/kompresi_gambar.dart}). Kelas ini memastikan batas itu menjadi
 * <b>jaminan</b>, bukan sekadar kesepakatan: klien yang dimodifikasi, versi lama yang belum
 * punya gerbangnya, atau pemanggil API langsung tetap tertahan di sini.</p>
 *
 * <h4>Menolak, bukan mengompresi</h4>
 * <p>Server sengaja tidak mengecilkan sendiri. Mengompresi di server berarti setiap unggahan
 * membebani CPU yang dipakai bersama seluruh toko, dan menyembunyikan dari pengguna bahwa
 * gambarnya diubah. Klien yang mengecilkan tahu persis apa yang dikirimnya.</p>
 *
 * <h4>Panjang base64 diperiksa SEBELUM didekode</h4>
 * <p>Mendekode dulu baru memeriksa ukuran berarti muatan 100 MB sudah terlanjur dialokasikan
 * di memori sebelum ditolak — beberapa permintaan seperti itu cukup untuk menjatuhkan
 * kontainer. {@link #periksaPanjangBase64} menolaknya sambil masih berupa teks.</p>
 *
 * <p>Seluruh metode mengembalikan {@code null} bila lolos, atau pesan galat siap tampil bila
 * ditolak — mengikuti gaya {@code tolak(hasil, pesan)} pada helper POS, bukan melempar.</p>
 */
public final class PenjagaLampiranGambar {

	/** Ambang yang sama dengan klien: {@code maksLampiranGambarBytes}. */
	public static final int MAKS_GAMBAR_BYTES = 500 * 1024;

	/**
	 * Batas longgar untuk lampiran non-gambar (faktur PDF, dokumen pindaian). Bukan aturan
	 * bisnis, melainkan pagar agar satu permintaan tidak menghabiskan memori kontainer.
	 */
	public static final int MAKS_LAMPIRAN_BYTES = 10 * 1024 * 1024;

	private PenjagaLampiranGambar() {
	}

	/**
	 * Benar bila byte awalnya adalah tanda pengenal format gambar yang dikenal.
	 *
	 * <p>Diperiksa dari <b>isinya</b>, bukan dari ekstensi nama berkas. Nama berkas datang
	 * dari klien dan dapat berisi apa saja; isi berkas tidak berbohong semudah itu.</p>
	 */
	public static boolean tampaknyaGambar(byte[] b) {
		if (b == null || b.length < 12) {
			return false;
		}
		// JPEG: FF D8 FF
		if (u(b, 0) == 0xFF && u(b, 1) == 0xD8 && u(b, 2) == 0xFF) {
			return true;
		}
		// PNG: 89 50 4E 47 0D 0A 1A 0A
		if (u(b, 0) == 0x89 && u(b, 1) == 0x50 && u(b, 2) == 0x4E && u(b, 3) == 0x47
				&& u(b, 4) == 0x0D && u(b, 5) == 0x0A && u(b, 6) == 0x1A && u(b, 7) == 0x0A) {
			return true;
		}
		// GIF: "GIF8"
		if (u(b, 0) == 0x47 && u(b, 1) == 0x49 && u(b, 2) == 0x46 && u(b, 3) == 0x38) {
			return true;
		}
		// BMP: "BM"
		if (u(b, 0) == 0x42 && u(b, 1) == 0x4D) {
			return true;
		}
		// WEBP: "RIFF" .... "WEBP"
		if (u(b, 0) == 0x52 && u(b, 1) == 0x49 && u(b, 2) == 0x46 && u(b, 3) == 0x46
				&& u(b, 8) == 0x57 && u(b, 9) == 0x45 && u(b, 10) == 0x42 && u(b, 11) == 0x50) {
			return true;
		}
		return false;
	}

	private static int u(byte[] b, int i) {
		return b[i] & 0xFF;
	}

	/**
	 * Periksa panjang teks base64 <b>sebelum</b> didekode.
	 *
	 * @param maksBytes batas ukuran hasil dekode yang dituju.
	 * @return {@code null} bila masih wajar, atau pesan penolakan.
	 */
	public static String periksaPanjangBase64(String base64, int maksBytes) {
		if (base64 == null) {
			return null;
		}
		// Base64 memuat 4 karakter untuk setiap 3 byte. Diberi kelonggaran 10% untuk spasi,
		// baris baru, dan awalan data-URI yang kadang ikut terkirim.
		long maksKarakter = (long) (((maksBytes + 2L) / 3L) * 4L * 1.1);
		if (base64.length() > maksKarakter) {
			return "Berkas terlalu besar. Maksimum " + ringkasUkuran(maksBytes) + ".";
		}
		return null;
	}

	/**
	 * Lampiran yang <b>wajib</b> berupa gambar: foto produk, slide layar pelanggan.
	 *
	 * @return {@code null} bila lolos, atau pesan penolakan.
	 */
	public static String periksaGambarWajib(byte[] isi) {
		if (isi == null || isi.length == 0) {
			return "Berkas gambar kosong.";
		}
		if (!tampaknyaGambar(isi)) {
			return "Lampiran harus berupa gambar (JPG, PNG, GIF, BMP, atau WEBP).";
		}
		if (isi.length > MAKS_GAMBAR_BYTES) {
			return "Ukuran gambar " + ringkasUkuran(isi.length) + " melebihi batas "
					+ ringkasUkuran(MAKS_GAMBAR_BYTES)
					+ ". Perbarui aplikasi POS Anda -- versi terbaru mengecilkannya otomatis.";
		}
		return null;
	}

	/**
	 * Lampiran serba-guna: gambar dibatasi 500 KB, yang bukan gambar dibatasi
	 * {@link #MAKS_LAMPIRAN_BYTES}.
	 *
	 * @return {@code null} bila lolos, atau pesan penolakan.
	 */
	public static String periksaBilaGambar(byte[] isi) {
		if (isi == null || isi.length == 0) {
			return "Berkas lampiran kosong.";
		}
		if (tampaknyaGambar(isi)) {
			if (isi.length > MAKS_GAMBAR_BYTES) {
				return "Ukuran gambar " + ringkasUkuran(isi.length) + " melebihi batas "
						+ ringkasUkuran(MAKS_GAMBAR_BYTES)
						+ ". Perbarui aplikasi POS Anda -- versi terbaru mengecilkannya otomatis.";
			}
			return null;
		}
		if (isi.length > MAKS_LAMPIRAN_BYTES) {
			return "Ukuran berkas " + ringkasUkuran(isi.length) + " melebihi batas "
					+ ringkasUkuran(MAKS_LAMPIRAN_BYTES) + ".";
		}
		return null;
	}

	/** "512 KB", "1,3 MB" -- angka mentah tidak berarti apa-apa bagi pengguna. */
	public static String ringkasUkuran(long bytes) {
		if (bytes < 1024L) {
			return bytes + " byte";
		}
		if (bytes < 1024L * 1024L) {
			return (bytes / 1024L) + " KB";
		}
		long sepersepuluh = (bytes * 10L) / (1024L * 1024L);
		return (sepersepuluh / 10L) + "," + (sepersepuluh % 10L) + " MB";
	}
}
