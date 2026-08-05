package ais.action.master.helper.util;

import org.zkoss.zk.ui.HtmlBasedComponent;

import ais.common.ConstantValues;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Mahasiswa;

/**
 * Utilitas PEWARNAAN status "belum lulus" pada tampilan LAYAR (ZK). Dipakai agar mata kuliah yang
 * BELUM LULUS ditandai MERAH secara SERAGAM di seluruh tampilan (transkrip layar, KHS layar, Rencana
 * Studi/KRS, dasbor, rekap, dsb.) sehingga pengguna mudah melihat mana mata kuliah yang belum tuntas.
 *
 * <p><b>Sumber kebenaran status lulus.</b> Penentuan lulus/tidak MENGIKUTI ATURAN SISTEM yang sudah
 * ada &mdash; {@link ConstantValues#lulusDariNilaiHuruf(String, Mahasiswa)} &mdash; yang menimbang
 * konfigurasi Nilai Huruf per Jurusan &rarr; Fakultas &rarr; global. Pada entitas
 * {@link Detailperkuliahan}, nilai ini sudah ter-<i>cache</i> di kolom {@code lulus} dan
 * <i>self-heal</i> lewat {@link Detailperkuliahan#getLulus()} (huruf "A" yang keliru tersimpan
 * "belum lulus" otomatis dikoreksi saat dibaca). Jadi util ini cukup membaca {@code getLulus()} &mdash;
 * tidak menduplikasi logika bisnis dan pasti konsisten dengan perhitungan IP/kelulusan.</p>
 *
 * <p><b>Kapan diberi warna merah?</b> HANYA bila mata kuliah sudah punya <b>nilai huruf final</b> dan
 * nilai itu <b>tidak lulus</b>. Mata kuliah yang <i>belum</i> ada nilainya (huruf kosong) TIDAK
 * diwarnai &mdash; sebab "belum dinilai" berbeda dengan "tidak lulus"; menandai yang kosong sebagai
 * merah justru menyesatkan. (Sesuai pilihan pengguna: ikuti aturan sistem, bukan tandai yang kosong.)</p>
 *
 * <p><b>Cara pakai.</b> Panggil {@link #warnai(HtmlBasedComponent, Detailperkuliahan)} pada komponen
 * teks (Label/baris) tempat mata kuliah / nilai ditampilkan, sesudah komponen dibuat maupun sesudah
 * nilainya berubah (idempoten: gaya merah hanya ditambahkan bila memang belum lulus, dan aman
 * dipanggil berulang). Untuk laporan PDF (JasperReports) gunakan mekanisme terpisah: sertakan kolom
 * {@code detailperkuliahan.lulus} pada query lalu tambahkan {@code conditionalStyle} berwarna merah.</p>
 *
 * <p>Kelas ini murni membaca data (tak mengubah DB) dan tak menyimpan state, sehingga hemat memori dan
 * aman dipakai lintas thread render ZK. Semua method statik; konstruktor disembunyikan.</p>
 */
public final class WarnaStatusLulusUtil {

	/** Gaya CSS penanda "belum lulus": teks merah tegas + tebal agar kontras di tabel/daftar. */
	public static final String STYLE_BELUM_LULUS = "color:#d32f2f;font-weight:bold;";

	private WarnaStatusLulusUtil() {
		// utilitas statik; tidak untuk di-instansiasi.
	}

	/**
	 * Apakah {@code detailperkuliahan} berstatus BELUM LULUS (ada nilai huruf final &amp; tidak lulus
	 * menurut aturan sistem). Mengembalikan {@code false} bila argumen null, nilai huruf masih kosong,
	 * atau status lulus tak dapat ditentukan.
	 */
	public static boolean belumLulus(Detailperkuliahan detailperkuliahan) {
		if (detailperkuliahan == null) {
			return false;
		}
		try {
			String nilaiHuruf = detailperkuliahan.getNilaiHuruf();
			if (nilaiHuruf == null || nilaiHuruf.trim().isEmpty()) {
				return false; // belum dinilai -> bukan "tidak lulus", jangan diwarnai.
			}
			Boolean lulus = detailperkuliahan.getLulus();
			return lulus != null && !lulus.booleanValue();
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Versi berbasis (nilai huruf, mahasiswa) untuk tampilan yang tak memegang objek Detailperkuliahan.
	 * Memakai aturan sistem {@link ConstantValues#lulusDariNilaiHuruf(String, Mahasiswa)}.
	 */
	public static boolean belumLulus(String nilaiHuruf, Mahasiswa mahasiswa) {
		if (nilaiHuruf == null || nilaiHuruf.trim().isEmpty()) {
			return false;
		}
		try {
			// Dipanggil via refleksi: method ConstantValues.lulusDariNilaiHuruf ada di RUNTIME (dipakai
			// getLulus di banyak tempat) namun bisa belum ter-compile di artefak yang jadi classpath saat
			// build terisolasi -> refleksi menghindari error kompilasi tanpa mengubah kelas pusat itu.
			java.lang.reflect.Method m = ConstantValues.class.getMethod("lulusDariNilaiHuruf", String.class,
					Mahasiswa.class);
			Object r = m.invoke(null, nilaiHuruf, mahasiswa);
			return (r instanceof Boolean) && !((Boolean) r).booleanValue();
		} catch (Throwable e) {
			return false;
		}
	}

	/**
	 * Memberi gaya MERAH pada {@code komponen} bila {@code detailperkuliahan} belum lulus. Gaya lama
	 * dipertahankan (di-append), jadi aman dipakai pada komponen yang sudah punya style. No-op bila
	 * komponen null atau mata kuliah lulus/belum dinilai.
	 */
	public static void warnai(HtmlBasedComponent komponen, Detailperkuliahan detailperkuliahan) {
		if (komponen == null || !belumLulus(detailperkuliahan)) {
			return;
		}
		terapkanMerah(komponen);
	}

	/** Sama seperti {@link #warnai(HtmlBasedComponent, Detailperkuliahan)} tapi dari (nilai huruf, mahasiswa). */
	public static void warnai(HtmlBasedComponent komponen, String nilaiHuruf, Mahasiswa mahasiswa) {
		if (komponen == null || !belumLulus(nilaiHuruf, mahasiswa)) {
			return;
		}
		terapkanMerah(komponen);
	}

	private static void terapkanMerah(HtmlBasedComponent komponen) {
		String lama = komponen.getStyle();
		if (lama != null && lama.contains(STYLE_BELUM_LULUS)) {
			return; // sudah diwarnai; idempoten.
		}
		komponen.setStyle((lama == null || lama.trim().isEmpty() ? "" : lama.trim() + ";") + STYLE_BELUM_LULUS);
	}
}
