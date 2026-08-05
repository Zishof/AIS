package ais.action.master.helper;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.PertemuanPunyaUjian;
import ais.ui.util.MyArrayList;

/**
 * Penjadwal HITUNG-ULANG skor peserta ujian secara <b>asinkron di latar (background thread)</b>,
 * dipakai oleh seluruh jalur "ikut ujian" (JSP web {@code _update_ujian.jsp}, API/mobile
 * {@code ElearningPertemuanApi.simpanJawabanSoal}, dan UI ZK {@code ProsesUjianHelper}).
 *
 * <h3>Untuk apa class ini</h3>
 * Setiap kali peserta memilih/menyimpan jawaban, jawaban tsb LANGSUNG tersimpan ke DB oleh
 * jalur masing-masing. Yang relatif mahal adalah meng-hitung-ulang agregat skor peserta
 * (jumlah benar, jumlah soal, nilai OBE) karena harus membaca seluruh detail jawaban. Bila
 * recompute dijalankan pada SETIAP klik jawaban saat ujian massal (ratusan peserta serentak),
 * beban DB melonjak. Class ini memisahkan "simpan jawaban" (sinkron, tiap klik) dari
 * "hitung-ulang skor" (asinkron + di-throttle), sehingga UI tetap responsif dan skor tetap
 * mutakhir tanpa menunggu tombol "Akhiri Ujian".
 *
 * <h3>Throttle</h3>
 * Recompute hanya dipicu setiap kelipatan N jawaban tersimpan (default 10; dapat diatur lewat
 * konfigurasi {@code interval_hitung_ulang_ujian_per_jawaban}) ATAU secara paksa saat peserta
 * mengakhiri ujian. Dengan begitu, untuk 10 jawaban hanya terjadi 1 recompute, bukan 10.
 *
 * <h3>Keamanan thread &amp; pool koneksi (c3p0)</h3>
 * Berkaca pada insiden "pool koneksi habis" akibat thread-pool besar, class ini SENGAJA memakai
 * SATU worker daemon ({@link Executors#newSingleThreadExecutor}). Semua recompute diserialkan
 * pada satu thread sehingga maksimal hanya satu koneksi DB tambahan yang dipakai pada satu waktu.
 * Selain itu diterapkan <i>coalescing</i> per {@code hasilUjianMahasiswaId}: bila recompute untuk
 * peserta tertentu masih mengantre, permintaan berikutnya untuk peserta yang sama diabaikan —
 * toh recompute selalu membaca SELURUH jawaban terbaru dari DB, jadi cukup satu antrian per peserta.
 *
 * <h3>Konsistensi data</h3>
 * Recompute bersifat autoritatif: ia memuat ulang detail jawaban dengan {@code refresh=true}
 * (menyegarkan cache MapDB sekaligus membaca data terkini dari DB) lalu memakai jalur baku
 * {@link ProsesUjianHelper#generateHasilUjian} untuk menghitung dan menyimpan agregat skor.
 *
 * Kompatibel Java 1.7 / ZKoss 5.5 (tanpa lambda / stream / try-with-resources).
 */
public final class UjianRecomputeUtil {

	/** Utility class: hanya method statis, tak boleh diinstansiasi. */
	private UjianRecomputeUtil() {
	}

	/**
	 * Set penanda peserta yang recompute-nya SEDANG MENGANTRE (sudah dijadwalkan tapi belum
	 * mulai dieksekusi worker). Kunci = {@code hasilUjianMahasiswaId}. Dipakai untuk coalescing:
	 * selama sebuah id ada di sini, permintaan recompute lain untuk id yang sama tidak ditumpuk.
	 * Memakai {@link ConcurrentHashMap} karena diakses dari banyak thread request + thread worker.
	 */
	private static final ConcurrentHashMap<Long, Boolean> antri = new ConcurrentHashMap<Long, Boolean>();

	/**
	 * Penghitung jumlah jawaban tersimpan per peserta SEJAK recompute terakhir, untuk keperluan
	 * throttle. Kunci = {@code hasilUjianMahasiswaId}, nilai = {@link AtomicInteger} (increment
	 * aman lintas-thread). Saat mencapai ambang batas, dinolkan kembali dan recompute dipicu.
	 */
	private static final ConcurrentHashMap<Long, AtomicInteger> penghitung = new ConcurrentHashMap<Long, AtomicInteger>();

	/**
	 * Satu-satunya worker: {@link Executors#newSingleThreadExecutor} dengan thread daemon bernama
	 * {@code "ujian-recompute-worker"}. Single-thread = recompute berjalan serial (aman untuk pool
	 * c3p0); daemon = tidak menghalangi shutdown JVM/Tomcat.
	 */
	private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "ujian-recompute-worker");
			t.setDaemon(true);
			return t;
		}
	});

	/**
	 * Menandai bahwa SATU jawaban peserta baru saja tersimpan, dan — bila ambang throttle tercapai
	 * — menjadwalkan hitung-ulang skor di latar.
	 *
	 * <p><b>Tujuan.</b> Dipanggil dari setiap jalur simpan jawaban (JSP/API/ZK) tepat setelah commit.
	 * Inilah "pintu masuk" throttle: ia memisahkan frekuensi penyimpanan jawaban (tiap klik) dari
	 * frekuensi recompute (tiap N klik), sehingga skor ter-update berkala tanpa membebani DB.</p>
	 *
	 * <p><b>Cara kerja.</b> (1) Bila id null, langsung keluar (defensif). (2) Ambil/inisialisasi
	 * {@link AtomicInteger} penghitung untuk peserta tsb dari {@link #penghitung}; pembuatan entry
	 * memakai {@code putIfAbsent} agar aman bila dua thread menambah peserta yang sama bersamaan.
	 * (3) {@code incrementAndGet()} menaikkan hitungan; bila hasilnya mencapai/melebihi
	 * {@link #ambangBatas()} (default 10), penghitung dinolkan lalu {@link #hitungUlangSekarang(Long)}
	 * dipanggil untuk menjadwalkan recompute. Jika belum mencapai ambang, tidak terjadi apa-apa
	 * selain menaikkan angka — sangat murah.</p>
	 *
	 * <p><b>Parameter.</b> {@code hasilUjianMahasiswaId} = id baris {@code HasilUjianMahasiswa}
	 * peserta yang jawabannya baru disimpan.</p>
	 *
	 * <p><b>Sifat &amp; thread-safety.</b> Non-blocking dan sangat ringan (operasi map + atomic int).
	 * Aman dipanggil dari banyak thread request. Recompute aktual dijalankan di thread worker, bukan
	 * di thread pemanggil, sehingga respons ke pengguna tidak tertunda.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Mengubah default ambang cukup lewat konfigurasi
	 * {@code interval_hitung_ulang_ujian_per_jawaban} tanpa rebuild. Penghitung disimpan di memori
	 * (per JVM) dan bersifat best-effort: bila aplikasi restart, hitungan tereset — tidak masalah,
	 * karena "Akhiri Ujian" tetap menghitung penuh. Bersihkan state peserta lewat
	 * {@link #bersihkan(Long)} saat sesi ujian peserta selesai untuk mencegah penumpukan entry.</p>
	 *
	 * @param hasilUjianMahasiswaId id HasilUjianMahasiswa peserta (boleh null → diabaikan)
	 */
	public static void tandaiJawabanTersimpan(final Long hasilUjianMahasiswaId) {
		if (hasilUjianMahasiswaId == null) {
			return;
		}
		int ambang = ambangBatas();
		AtomicInteger c = penghitung.get(hasilUjianMahasiswaId);
		if (c == null) {
			c = new AtomicInteger(0);
			AtomicInteger prev = penghitung.putIfAbsent(hasilUjianMahasiswaId, c);
			if (prev != null) {
				c = prev;
			}
		}
		if (c.incrementAndGet() >= ambang) {
			c.set(0);
			hitungUlangSekarang(hasilUjianMahasiswaId);
		}
	}

	/**
	 * Memaksa penjadwalan hitung-ulang skor peserta SEKARANG (non-blocking, dengan coalescing),
	 * tanpa menunggu ambang throttle.
	 *
	 * <p><b>Tujuan.</b> Dipakai ketika skor wajib segera dimutakhirkan: saat "Akhiri Ujian" atau
	 * aksi admin (mis. Hitung Ulang). Berbeda dengan {@link #tandaiJawabanTersimpan(Long)} yang
	 * dibatasi ambang, method ini selalu menjadwalkan recompute (sekali, ter-coalesce).</p>
	 *
	 * <p><b>Cara kerja &amp; coalescing.</b> (1) Id null → keluar. (2) {@code antri.putIfAbsent(id, TRUE)}:
	 * bila peserta SUDAH punya recompute yang mengantre, method langsung berhenti (tidak menumpuk
	 * antrian ganda) — ini kunci agar worker tidak kebanjiran tugas duplikat. (3) Bila belum,
	 * sebuah {@link Runnable} dikirim ke {@link #WORKER}. Di dalam runnable, penanda antri DILEPAS
	 * di AWAL eksekusi; konsekuensinya, jawaban yang masuk SAAT/SESUDAH recompute mulai berjalan
	 * akan boleh menjadwalkan recompute baru — sehingga hasil terakhir selalu tercakup (tidak ada
	 * jawaban "tertinggal" dari snapshot lama). (4) Bila {@code submit} ditolak (mis. executor sedang
	 * shutdown), penanda antri dibersihkan kembali agar tidak macet.</p>
	 *
	 * <p><b>Parameter.</b> {@code hasilUjianMahasiswaId} = id HasilUjianMahasiswa yang akan dihitung
	 * ulang.</p>
	 *
	 * <p><b>Sifat.</b> Non-blocking: hanya mengantrekan tugas; pekerjaan berat ({@link #hitungUlang(Long)})
	 * berjalan di thread worker tunggal. Aman dipanggil berulang/serentak berkat coalescing.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Jangan mengganti single-thread executor menjadi pool besar tanpa
	 * mempertimbangkan batas koneksi c3p0 — itulah alasan desain serial ini. Bila butuh paralel,
	 * gunakan plafon kecil dan pastikan tiap recompute menutup session-nya (generateHasilUjian sudah
	 * mengelola session-nya sendiri).</p>
	 *
	 * @param hasilUjianMahasiswaId id HasilUjianMahasiswa peserta (boleh null → diabaikan)
	 */
	public static void hitungUlangSekarang(final Long hasilUjianMahasiswaId) {
		if (hasilUjianMahasiswaId == null) {
			return;
		}
		// Sudah ada yang mengantre untuk peserta ini → cukup; jangan tumpuk.
		if (antri.putIfAbsent(hasilUjianMahasiswaId, Boolean.TRUE) != null) {
			return;
		}
		try {
			WORKER.submit(new Runnable() {
				@Override
				public void run() {
					// Lepas penanda di awal: jawaban yang datang SAAT/SESUDAH ini berjalan
					// akan menjadwalkan recompute baru (hasil terbaru terjamin).
					antri.remove(hasilUjianMahasiswaId);
					hitungUlang(hasilUjianMahasiswaId);
				}
			});
		} catch (Exception e) {
			antri.remove(hasilUjianMahasiswaId);
		}
	}

	/**
	 * Menghapus state throttle (penghitung jawaban) milik seorang peserta.
	 *
	 * <p><b>Tujuan.</b> Dipanggil saat sesi ujian peserta dianggap selesai — terutama dari jalur
	 * "Akhiri Ujian" ({@code ProsesUjianHelper.onSelesai} setelah {@code generateHasilUjian}) — untuk
	 * (a) mencegah penumpukan entry {@link #penghitung} bagi peserta yang sudah tidak aktif, dan
	 * (b) mengembalikan hitungan ke kondisi bersih bila peserta mengulang ujian.</p>
	 *
	 * <p><b>Cara kerja.</b> Bila id null, keluar. Selain itu menghapus entry peserta dari
	 * {@link #penghitung}. Tidak menyentuh {@link #antri} maupun membatalkan recompute yang sedang
	 * berjalan — recompute yang terlanjur dijadwalkan tetap berjalan dan tetap valid (membaca data
	 * terbaru), jadi tidak ada risiko data salah; method ini murni housekeeping memori.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code hasilUjianMahasiswaId} = id peserta; void.</p>
	 *
	 * <p><b>Sifat.</b> Sangat ringan (satu operasi remove pada {@link ConcurrentHashMap}), aman
	 * lintas-thread, idempoten (memanggilnya berulang tidak berefek samping).</p>
	 *
	 * <p><b>Pemeliharaan.</b> Karena penghitung hanya menyimpan angka kecil per peserta, tidak
	 * memanggil {@code bersihkan} pun tidak menyebabkan kebocoran berbahaya — namun memanggilnya saat
	 * selesai adalah praktik baik agar map tidak tumbuh seiring banyaknya peserta selama uptime server.
	 * Bila kelak ditambah cache lain per peserta, hapus juga di sini agar konsisten.</p>
	 *
	 * @param hasilUjianMahasiswaId id HasilUjianMahasiswa peserta (boleh null → diabaikan)
	 */
	public static void bersihkan(Long hasilUjianMahasiswaId) {
		if (hasilUjianMahasiswaId == null) {
			return;
		}
		penghitung.remove(hasilUjianMahasiswaId);
	}

	/**
	 * Membaca ambang batas throttle (berapa jawaban tersimpan per satu recompute) dari konfigurasi.
	 *
	 * <p><b>Tujuan.</b> Memusatkan pembacaan parameter {@code interval_hitung_ulang_ujian_per_jawaban}
	 * sehingga frekuensi recompute dapat disetel oleh admin tanpa mengubah/rebuild kode. Dipakai
	 * {@link #tandaiJawabanTersimpan(Long)} untuk memutuskan kapan memicu recompute.</p>
	 *
	 * <p><b>Cara kerja.</b> Default 10. Membaca {@code Common.getKonfigurasi("interval_hitung_ulang_ujian_per_jawaban", "10")}
	 * lalu mem-parsing nilainya menjadi integer. Bila konfigurasi tidak ada/format salah, blok
	 * {@code try/catch} membiarkan nilai tetap pada default 10 (fail-safe). Terakhir, nilai dijaga
	 * minimal 1 ({@code n < 1 ? 1 : n}) untuk mencegah ambang 0/negatif yang akan memicu recompute
	 * pada setiap jawaban (atau perilaku tak terdefinisi).</p>
	 *
	 * <p><b>Return.</b> Bilangan bulat &ge; 1: jumlah jawaban tersimpan yang memicu satu recompute.</p>
	 *
	 * <p><b>Sifat &amp; pertimbangan.</b> Method dibaca cukup sering (tiap jawaban), namun
	 * {@code getKonfigurasi} pada umumnya murah karena konfigurasi di-cache di lapisan Common.
	 * Bila kelak terbukti menjadi hotspot, nilai ini bisa di-cache lokal dengan TTL; saat ini
	 * kesederhanaan lebih diutamakan agar perubahan konfigurasi langsung berefek.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Bila ingin menonaktifkan throttle (recompute tiap jawaban), set
	 * konfigurasi ke 1. Untuk mengurangi beban lebih jauh, naikkan nilainya (mis. 20). Jangan
	 * menambah logika berat di sini karena dipanggil pada jalur panas.</p>
	 *
	 * @return ambang jumlah jawaban per recompute (minimal 1; default 10)
	 */
	private static int ambangBatas() {
		int n = 10;
		try {
			n = Integer.parseInt(
					Common.getKonfigurasi("interval_hitung_ulang_ujian_per_jawaban", "10").getNilai().trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/UjianRecomputeUtil.java:252");
		}
		return n < 1 ? 1 : n;
	}

	/**
	 * Mengeksekusi hitung-ulang skor yang sesungguhnya untuk satu peserta — dijalankan DI THREAD
	 * WORKER (bukan thread request).
	 *
	 * <p><b>Tujuan.</b> Inilah "pekerja berat" di balik {@link #hitungUlangSekarang(Long)}. Tugasnya
	 * memuat ulang seluruh detail jawaban peserta dari sumber terkini lalu menghitung & menyimpan
	 * agregat skor (jumlah benar, jumlah soal, nilai OBE) memakai jalur perhitungan baku sistem,
	 * sehingga hasil di profil/laporan/hasil-ujian konsisten dengan saat submit.</p>
	 *
	 * <p><b>Cara kerja.</b> (1) Mengambil {@code HasilUjianMahasiswa} via cache objek
	 * {@code GeneralValueObject.ambilData}. Bila null atau {@code PertemuanPunyaUjian}-nya null,
	 * keluar (tidak ada yang bisa dihitung). (2) Menentukan {@code jumlahDiujikan} dari
	 * {@code getJmlDitampilkan()} (null-safe → 0). (3) Memuat daftar soal dan detail jawaban dengan
	 * parameter {@code refresh=true}: ini PENTING karena selain membaca data terkini, ia juga
	 * MENYEGARKAN cache MapDB detail jawaban — tanpa ini, jawaban yang baru disimpan bisa terbaca
	 * basi sehingga skor salah (lihat insiden koherensi cache). (4) Memanggil
	 * {@link ProsesUjianHelper#generateHasilUjian} yang menetapkan jumlah soal, menghitung OBE &
	 * pilihan ganda, lalu menyimpan {@code HasilUjianMahasiswa} (mengelola session-nya sendiri).</p>
	 *
	 * <p><b>Parameter.</b> {@code hasilUjianMahasiswaId} = id peserta yang dihitung ulang.</p>
	 *
	 * <p><b>Penanganan error.</b> Seluruh badan dibungkus {@code try/catch}; kegagalan dilaporkan via
	 * {@code Common.tampilErrorJikaAdmin} tanpa melempar keluar — karena berjalan di worker daemon,
	 * exception yang lolos akan hilang ke uncaught handler dan menyulitkan diagnosa. Aman bila peserta
	 * sudah dihapus di tengah jalan (ambilData mengembalikan null → keluar rapi).</p>
	 *
	 * <p><b>Pemeliharaan.</b> Method bersifat {@code private} dan hanya dipanggil dari worker; jangan
	 * memanggilnya langsung dari thread request (akan blocking + tanpa coalescing). Bila aturan
	 * perhitungan skor berubah, ubah di {@code ProsesUjianHelper.generateHasilUjian} (sumber tunggal),
	 * bukan di sini, agar konsisten dengan jalur submit.</p>
	 *
	 * @param hasilUjianMahasiswaId id HasilUjianMahasiswa peserta yang akan dihitung ulang
	 */
	private static void hitungUlang(Long hasilUjianMahasiswaId) {
		try {
			HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) GeneralValueObject
					.ambilData(HasilUjianMahasiswa.class, hasilUjianMahasiswaId.toString());
			if (hasilUjianMahasiswa == null || hasilUjianMahasiswa.getPertemuanPunyaUjian() == null) {
				return;
			}
			PertemuanPunyaUjian pertemuanPunyaUjian = hasilUjianMahasiswa.getPertemuanPunyaUjian();
			int jumlahDiujikan = pertemuanPunyaUjian.getJmlDitampilkan() == null ? 0
					: pertemuanPunyaUjian.getJmlDitampilkan().intValue();

			// refresh=true → segarkan cache MapDB & ambil jawaban terbaru dari DB.
			MyArrayList<Long> ujianPunyaSoals = hasilUjianMahasiswa.ambilUjianPunyaSoals(jumlahDiujikan, null, true);
			Map<Long, Set<Long>> hasilUjianMahasiswaDetails = hasilUjianMahasiswa
					.ambilHasilUjianMahasiswaDetail(jumlahDiujikan, ujianPunyaSoals, true);

			ProsesUjianHelper.generateHasilUjian(ujianPunyaSoals, hasilUjianMahasiswa, pertemuanPunyaUjian,
					hasilUjianMahasiswaDetails);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}
}
