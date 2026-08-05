package ais.action.master;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;

/**
 * Utilitas BERSAMA untuk menjamin keunikan "kode" (nomor surat / nomor agenda) yang
 * dihasilkan banyak modul transaksional — khususnya seluruh Action di paket
 * {@code ais.action.master.asset.*} dan {@code ais.action.master.akunting.*} — pada saat
 * penyimpanan data BARU.
 *
 * <h3>Latar belakang masalah</h3>
 * Sebelum util ini ada, setiap Action membentuk kode lewat pola lama
 * {@code generateCode()} yang nilainya bersumber dari {@code getindex() = rowCount(baris aktif) + 1}.
 * Pola {@code rowCount + 1} TIDAK monotonik: ketika sebuah baris dihapus atau di-nonaktifkan
 * (kolom {@code aktif = false}), jumlah baris turun sehingga index berikutnya "mundur" ke
 * angka yang SUDAH pernah dipakai. Akibatnya kode yang diformat bisa BENTROK dengan kode
 * lama → muncul DUPLIKAT. Karena banyak tabel kode tidak punya UNIQUE constraint, duplikat
 * ini sering lolos diam-diam dan baru ketahuan belakangan (laporan ganda, relasi ambigu, dst.).
 *
 * <h3>Strategi penyelesaian</h3>
 * Util ini memusatkan satu aturan sederhana: <b>sebuah kode hanya boleh dipakai bila belum
 * ada di tabel</b>. Pemeriksaan dilakukan dengan {@code SELECT count(*) WHERE kode = ?} pada
 * kelas entity yang bersangkutan. Bila kode kandidat ternyata sudah ada, util menambahkan
 * sufiks angka berurut ("-2", "-3", ...) sampai memperoleh kode yang benar-benar bebas.
 * Pendekatan sufiks dipilih karena UNIFORM lintas semua modul: ia hanya butuh tahu kelas
 * entity-nya (yang punya kolom {@code kode}) tanpa perlu memahami format/penomoran spesifik
 * masing-masing NomorSurat. (Beberapa modul, mis. PermintaanPengadaanMasterAsset, memilih
 * menaikkan index lalu memformat ulang demi penomoran lebih rapi; util ini adalah jaring
 * pengaman generik yang dipakai mayoritas modul.)
 *
 * <h3>Batas & catatan konkurensi</h3>
 * Pemeriksaan membaca data yang sudah ter-commit pada {@link HibernateUtil#currentSession()
 * currentSession}. Untuk skenario dua penyimpanan yang terjadi BERSAMAAN persis (race),
 * keduanya bisa membaca "kode bebas" yang sama sebelum salah satunya commit; agar 100% kebal
 * tambahkan UNIQUE INDEX pada kolom {@code kode} di basis data — penanganan
 * {@code ConstraintViolationException} yang sudah ada di tiap Action akan menangani sisa
 * kasus langka tsb. Semua method bersifat defensif: bila pemeriksaan gagal (mis. tabel belum
 * ada, koneksi bermasalah) util TIDAK memblokir penyimpanan, melainkan jatuh ke perilaku lama.
 *
 * <h3>Pemeliharaan</h3>
 * Class bersifat {@code final} dan stateless (tanpa field instance), aman dipanggil dari thread
 * mana pun selama {@code currentSession} tersedia di thread tsb. Kompatibel Java 1.6/1.7
 * (tanpa lambda, stream, maupun diamond operator) mengikuti standar codebase ZK 5.5 ini.
 */
public final class KodeUnikUtil {

	/** Constructor privat: class ini murni kumpulan method statis (utility), tak boleh diinstansiasi. */
	private KodeUnikUtil() {
	}

	/**
	 * Memeriksa apakah sebuah nilai {@code kode} SUDAH dipakai oleh baris mana pun pada tabel
	 * yang dipetakan {@code entityClass}.
	 *
	 * <p><b>Tujuan.</b> Menjadi satu-satunya sumber kebenaran "apakah kode ini sudah ada?" yang
	 * dipakai {@link #pastikanUnik(Class, String)} maupun pemanggil lain yang ingin memvalidasi
	 * kode sebelum menyimpan. Dengan memusatkan logika ini, semua modul memakai kriteria yang
	 * sama persis sehingga perilaku konsisten dan mudah diaudit.</p>
	 *
	 * <p><b>Cara kerja.</b> Membuka {@link HibernateUtil#currentSession()} (session milik request
	 * berjalan — TIDAK ditutup manual, sesuai aturan currentSession), lalu menjalankan kriteria
	 * {@code createCriteria(entityClass).add(Restrictions.eq("kode", kode)).setProjection(rowCount())}.
	 * Hasil {@code count} dibaca sebagai {@link Number}; method mengembalikan {@code true} bila
	 * count &gt; 0. Pemeriksaan mencakup SEMUA baris (termasuk yang non-aktif/{@code aktif=false})
	 * supaya kode yang pernah dipakai tidak digunakan ulang — ini penting karena tujuan keunikan
	 * adalah lintas seluruh tabel, bukan hanya baris aktif.</p>
	 *
	 * <p><b>Parameter.</b> {@code entityClass} adalah kelas entity Hibernate yang memiliki properti
	 * {@code kode} (mis. {@code KasBesar.class}, {@code PemesananPengadaanMasterAsset.class}).
	 * {@code kode} adalah nilai yang hendak diuji. Bila {@code entityClass} null, atau {@code kode}
	 * null/kosong, method langsung mengembalikan {@code false} (dianggap "tidak menabrak apa pun")
	 * agar pemanggil tidak terjebak loop atau NPE.</p>
	 *
	 * <p><b>Return.</b> {@code true} bila minimal satu baris dengan {@code kode} tsb ditemukan;
	 * {@code false} bila tidak ada, ATAU bila terjadi kegagalan pemeriksaan.</p>
	 *
	 * <p><b>Penanganan error &amp; alasan desain.</b> Seluruh query dibungkus {@code try/catch};
	 * bila gagal (tabel belum terbentuk, transaksi bermasalah, dll.) method mengembalikan
	 * {@code false} — sengaja "fail-open" agar kegagalan pemeriksaan keunikan TIDAK pernah
	 * menggagalkan proses simpan utama. Risiko duplikat akibat fail-open ditutup oleh UNIQUE INDEX
	 * di DB (disarankan). Method ini ringan (satu COUNT ber-index bila kolom kode terindeks) dan
	 * aman dipanggil berulang dalam loop oleh {@link #pastikanUnik(Class, String)}.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Jika nama properti kode pada suatu entity berbeda (bukan "kode"),
	 * util ini tidak cocok untuk entity tsb; pertimbangkan menambah overload yang menerima nama
	 * properti. Jangan menutup {@code currentSession} di sini.</p>
	 *
	 * @param entityClass kelas entity Hibernate ber-properti {@code kode}
	 * @param kode        nilai kode yang diperiksa
	 * @return {@code true} bila kode sudah dipakai pada tabel tsb; selain itu {@code false}
	 */
	public static boolean kodeSudahDipakai(Class<?> entityClass, String kode) {
		if (entityClass == null || kode == null || kode.trim().isEmpty()) {
			return false;
		}
		try {
			Session session = HibernateUtil.currentSession();
			Number n = (Number) session.createCriteria(entityClass).add(Restrictions.eq("kode", kode))
					.setProjection(Projections.rowCount()).uniqueResult();
			return n != null && n.longValue() > 0;
		} catch (Exception e) {
			// Bila pemeriksaan gagal, jangan blokir penyimpanan (fallback ke perilaku lama).
			return false;
		}
	}

	/**
	 * Mengembalikan sebuah kode yang DIJAMIN belum dipakai pada {@code entityClass}, dengan
	 * berangkat dari {@code kodeAwal} dan menambahkan sufiks angka bila perlu.
	 *
	 * <p><b>Tujuan.</b> Inilah method yang dipanggil tiap Action tepat sebelum menyimpan data baru
	 * (umumnya membungkus hasil {@code generateCode()}/format NomorSurat). Tujuannya mengubah kode
	 * yang "mungkin bentrok" menjadi kode yang pasti bebas, sehingga proses simpan tidak lagi
	 * menghasilkan duplikat akibat index {@code rowCount + 1} yang tidak monotonik.</p>
	 *
	 * <p><b>Cara kerja.</b> (1) Bila {@code kodeAwal} null/kosong, dikembalikan apa adanya — tidak ada
	 * yang bisa diunikkan, dan ini menghindari pembuatan kode "-2" dari nilai kosong. (2) Bila
	 * {@code kodeAwal} BELUM dipakai (cek via {@link #kodeSudahDipakai(Class, String)}), langsung
	 * dikembalikan tanpa perubahan — kasus normal/mayoritas, tanpa overhead tambahan. (3) Bila sudah
	 * dipakai, method mencoba {@code kodeAwal + "-2"}, lalu {@code "-3"}, dan seterusnya, memeriksa
	 * tiap kandidat hingga menemukan yang bebas. Untuk mencegah loop tak berujung pada kondisi
	 * abnormal (mis. format yang selalu menghasilkan nilai sama, atau pemeriksaan selalu mengembalikan
	 * true), terdapat batas pengaman {@code n < 100000}; bila batas tercapai, kandidat terakhir tetap
	 * dikembalikan (sangat tidak mungkin terjadi pada data nyata).</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code entityClass} = entity ber-properti {@code kode}.
	 * {@code kodeAwal} = kode dasar hasil generator. Mengembalikan {@code kodeAwal} itu sendiri bila
	 * sudah unik/null/kosong, atau varian bersufiks ("-N") yang pertama kali bebas.</p>
	 *
	 * <p><b>Konsekuensi &amp; pertimbangan.</b> Sufiks "-N" mengubah tampilan kode hanya pada kasus
	 * tabrakan (jarang, biasanya akibat penghapusan data). Ini disengaja sebagai kompromi: solusi
	 * generik yang berlaku seragam untuk puluhan modul tanpa perlu mengetahui aturan format tiap
	 * NomorSurat. Bila suatu modul menuntut penomoran rapi tanpa sufiks, modul tsb dapat menaikkan
	 * index lalu memformat ulang sendiri (lihat PermintaanPengadaanMasterAssetAction sebagai contoh).</p>
	 *
	 * <p><b>Konkurensi.</b> Karena bertumpu pada data ter-commit, dua transaksi paralel masih dapat
	 * memilih sufiks bebas yang sama; UNIQUE INDEX pada kolom {@code kode} + penanganan
	 * {@code ConstraintViolationException} di pemanggil adalah lapis pengaman terakhir. Method ini
	 * sendiri tidak membuka/menutup transaksi — ia hanya membaca lewat {@code currentSession}.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Jangan mengganti pemisah sufiks tanpa memeriksa apakah ada validasi
	 * panjang/format kode di DB; "-N" menambah panjang string sehingga pastikan kolom {@code kode}
	 * cukup lebar. Batas 100000 sengaja besar agar tak pernah jadi penghalang pada praktiknya.</p>
	 *
	 * @param entityClass kelas entity Hibernate ber-properti {@code kode}
	 * @param kodeAwal    kode dasar (boleh sudah dipakai); null/kosong dikembalikan apa adanya
	 * @return kode yang dijamin belum dipakai pada tabel entity tsb
	 */
	public static String pastikanUnik(Class<?> entityClass, String kodeAwal) {
		if (kodeAwal == null || kodeAwal.trim().isEmpty()) {
			return kodeAwal;
		}
		if (!kodeSudahDipakai(entityClass, kodeAwal)) {
			return kodeAwal;
		}
		int n = 2;
		String kandidat = kodeAwal + "-" + n;
		while (kodeSudahDipakai(entityClass, kandidat) && n < 100000) {
			n++;
			kandidat = kodeAwal + "-" + n;
		}
		return kandidat;
	}
}
