package ais.common;

import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.zkoss.zul.Label;

import ais.database.model.GeneralValueObject;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaDiskusi;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.Tbmuser;
import ais.database.model.Tugas;
import ais.database.model.file.PertemuanFileContent;
import ais.database.model.streaming.AudioPertemuan;
import ais.database.model.streaming.VideoPertemuan;

/**
 * Kumpulan helper statis untuk mengumpulkan (secara paralel-terbatas) dua jenis data ringkasan
 * e-learning lintas banyak {@link Pertemuan} sekaligus: komentar/diskusi (lewat
 * {@link #ambilKomentar}) dan materi/konten (lewat {@link #ambilMateri}) — mis. untuk menyusun
 * tampilan ringkasan aktivitas kelas/pertemuan tanpa harus memuat dan menggabungkan data satu per
 * satu secara sekuensial di thread request.
 *
 * <h2>Pola paralelisasi</h2>
 * <p>
 * Kedua method utama memakai pola yang identik: untuk setiap unit kerja (satu diskusi atau satu
 * pertemuan), sebuah {@link Runnable} disebar ke {@link ExecutorService} berukuran tetap yang
 * dibuat lewat {@link Executors#newFixedThreadPool(int)}, dengan jumlah thread dibatasi oleh
 * {@link DbThreadPool#safe(int)} — bukan sekadar dibatasi jumlah unit kerja, karena pekerjaan ini
 * DB-bound (setiap worker membuka koneksi Hibernate sendiri) dan pool koneksi database harus
 * disisakan untuk request web normal lainnya. Penyelesaian seluruh worker ditunggu lewat
 * {@link CountDownLatch} dengan batas waktu 30 detik; bila timeout terlampaui, executor
 * dimatikan paksa ({@code shutdownNow()}) dan hasil yang sudah terkumpul sejauh itu tetap
 * dikembalikan (partial result), bukan melempar exception. Hasil tiap worker ditulis ke
 * {@link ConcurrentSkipListMap} (aman diakses banyak thread sekaligus, otomatis terurut
 * berdasarkan kunci) sebelum akhirnya disalin ke {@link TreeMap} hasil akhir secara sekuensial
 * di thread pemanggil.
 * </p>
 * <p>
 * Logging debug ke konsol seluruhnya dikendalikan lewat saklar konstan {@link #DEBUG_MODE}
 * (bernilai {@code false} secara default) — bukan dibaca dari konfigurasi runtime, sehingga untuk
 * mengaktifkan log debug perlu mengubah nilai konstanta ini dan build ulang.
 * </p>
 */
public class MateriDanKomentarHelper {

	/** Saklar kompilasi untuk menghidupkan/mematikan log debug progres paralel ke konsol. */
	private static final boolean DEBUG_MODE = false;

	// =========================================================================================
	// 1. METODE AMBIL KOMENTAR / DISKUSI (PARALEL TERBATAS SESUAI POOL DB)
	// =========================================================================================

	/**
	 * Mengumpulkan seluruh komentar/diskusi ({@link PertemuanPunyaDiskusi}) dari kumpulan
	 * {@code pertemuans} yang diberikan, difilter berdasarkan peran penulis komentar (dosen,
	 * mahasiswa, guru, siswa, admin) dan opsional kata kunci pencarian, lalu mengembalikannya
	 * sebagai peta terurut berdasarkan kunci gabungan tanggal-perubahan + id diskusi.
	 *
	 * <p>
	 * Alur kerja: (1) mengambil seluruh id {@link PertemuanPunyaDiskusi} yang menjadi milik
	 * kumpulan {@code pertemuans} lewat {@link Pertemuan#ambilPertemuanPunyaDiskusiTotalSemua};
	 * (2) untuk tiap id, di thread worker terpisah, memuat entity diskusi lewat
	 * {@link GeneralValueObject#ambilData}, membuangnya bila penulisnya tidak termasuk peran
	 * yang diizinkan ({@code dosenBol}/{@code mahasiswaBol}/{@code guruBol}/{@code siswaBol}/
	 * {@code adminBol}), lalu (bila {@code cari} tidak kosong) membuang juga hasil yang isi
	 * komentar maupun identitas penulisnya (NIM/nama/NIDN/kode/username) tidak memuat kata
	 * kunci pencarian (case-insensitive); (3) diskusi yang lolos filter dimasukkan ke peta hasil
	 * dengan kunci {@code "<tanggal_dirubah formatted>_diskusi_<id>"} sehingga hasil akhir
	 * otomatis terurut berdasarkan waktu perubahan terbaru.
	 * </p>
	 *
	 * <p>
	 * Nilai setiap entri hasil berupa {@code Object[]} beranggotakan
	 * {@code {idDiskusi, idPertemuan, tanggalDirubah}} — bukan entity {@link
	 * PertemuanPunyaDiskusi} penuh, agar pemanggil dapat memuat ulang entity sesuai kebutuhan
	 * tampilan tanpa menahan referensi objek besar di memori selama proses paralel berlangsung.
	 * </p>
	 *
	 * @param pertemuans   peta nama pertemuan ke id-nya, sumber data yang akan diproses; bila
	 *                     {@code null}/kosong, method mengembalikan peta kosong tanpa memproses
	 * @param refresh      diteruskan tanpa dipakai langsung di method ini (dipertahankan untuk
	 *                     kesamaan tanda tangan/pola dengan {@link #ambilMateri}); pengambilan
	 *                     total diskusi selalu memakai {@code false} pada pemanggilan
	 *                     {@link Pertemuan#ambilPertemuanPunyaDiskusiTotalSemua}
	 * @param dosenBol     sertakan komentar yang ditulis dosen bila {@code true}
	 * @param mahasiswaBol sertakan komentar yang ditulis mahasiswa bila {@code true}
	 * @param guruBol      sertakan komentar yang ditulis guru bila {@code true}
	 * @param siswaBol     sertakan komentar yang ditulis siswa bila {@code true}
	 * @param adminBol     sertakan komentar yang ditulis admin/{@link Tbmuser} generik (bukan
	 *                     dosen/mahasiswa) bila {@code true}
	 * @param cari         kata kunci pencarian bebas (dicocokkan ke isi komentar dan identitas
	 *                     penulis, case-insensitive); {@code null}/kosong berarti tanpa filter
	 * @param label        komponen ZK {@link Label} yang diteruskan ke
	 *                     {@link Pertemuan#ambilPertemuanPunyaDiskusiTotalSemua} (biasanya untuk
	 *                     keperluan progres/UI di lapisan pemanggil)
	 * @return peta hasil terurut kunci {@code "tanggal_diskusi_id"} ke {@code Object[]}
	 *         {@code {idDiskusi, idPertemuan, tanggalDirubah}}; tidak pernah {@code null}, boleh
	 *         kosong. Bila proses paralel melebihi batas waktu 30 detik, hasil yang dikembalikan
	 *         adalah hasil parsial dari worker yang sempat selesai
	 */
	public static TreeMap<String, Object[]> ambilKomentar(final TreeMap<String, Long> pertemuans, final boolean refresh,
			final boolean dosenBol, final boolean mahasiswaBol, final boolean guruBol, final boolean siswaBol,
			final boolean adminBol, final String cari, final Label label) {

		final TreeMap<String, Object[]> finalResult = new TreeMap<String, Object[]>();

		if (pertemuans == null || pertemuans.isEmpty()) {
			return finalResult;
		}

		TreeSet<Long> pertemuanPunyaDiskusis = Pertemuan.ambilPertemuanPunyaDiskusiTotalSemua(false,
				pertemuans.values(), label);

		if (pertemuanPunyaDiskusis == null || pertemuanPunyaDiskusis.isEmpty()) {
			return finalResult;
		}

		final int size = pertemuanPunyaDiskusis.size();
		// Setiap worker memuat entity melalui Hibernate. Jangan membuat ratusan worker per
		// request karena beberapa pengguna yang membuka ringkasan bersamaan dapat menghabiskan
		// pool koneksi dan membuat jumlah thread melonjak. DbThreadPool menyisakan kapasitas
		// koneksi untuk request web normal (default 16, batas keras 32).
		int maxThreads = DbThreadPool.safe(size);

		if (DEBUG_MODE) {
			System.out.println("DEBUG [Komentar]: Memulai antrean " + size + " tugas diskusi dengan " + maxThreads + " threads paralel.");
		}

		final ConcurrentSkipListMap<String, Object[]> concurrentHasil = new ConcurrentSkipListMap<String, Object[]>();
		final ExecutorService executorService = Executors.newFixedThreadPool(maxThreads);
		final CountDownLatch latch = new CountDownLatch(size);
		final AtomicInteger progressCount = new AtomicInteger(0);
		final AtomicInteger activeThreads = new AtomicInteger(0);

		final String kataKunci = (cari == null || cari.trim().isEmpty()) ? "" : cari.trim().toLowerCase();

		for (final Long pertemuanPunyaDiskusiId : pertemuanPunyaDiskusis) {
			executorService.execute(new Runnable() {
				@Override
				public void run() {

					try {
						if (pertemuanPunyaDiskusiId == null) return;

						PertemuanPunyaDiskusi diskusi = (PertemuanPunyaDiskusi) GeneralValueObject
								.ambilData(PertemuanPunyaDiskusi.class, pertemuanPunyaDiskusiId.toString(), true);

						if (diskusi == null) return;

						if (!dosenBol && diskusi.getDosen() != null) return;
						if (!mahasiswaBol && diskusi.getMahasiswa() != null) return;
						if (!guruBol && diskusi.getGuru() != null) return;
						if (!siswaBol && diskusi.getSiswa() != null) return;
						if (!adminBol && diskusi.getTbmuser() != null && diskusi.getDosen() == null
								&& diskusi.getMahasiswa() == null) return;

						if (!kataKunci.isEmpty()) {
							StringBuilder sbContent = new StringBuilder();
							appendSafe(sbContent, diskusi.getIsi());

							if (diskusi.getMahasiswa() != null) {
								appendSafe(sbContent, diskusi.getMahasiswa().getNim());
								appendSafe(sbContent, diskusi.getMahasiswa().getNama());
							} else if (diskusi.getSiswa() != null) {
								appendSafe(sbContent, diskusi.getSiswa().getNomorIndukNasional());
								appendSafe(sbContent, diskusi.getSiswa().getNomorInduk());
								appendSafe(sbContent, diskusi.getSiswa().getNama());
							} else if (diskusi.getDosen() != null) {
								appendSafe(sbContent, diskusi.getDosen().getNidn());
								appendSafe(sbContent, diskusi.getDosen().getNama());
							} else if (diskusi.getGuru() != null) {
								appendSafe(sbContent, diskusi.getGuru().getKode());
								appendSafe(sbContent, diskusi.getGuru().getNama());
							} else if (diskusi.getTbmuser() != null) {
								appendSafe(sbContent, diskusi.getTbmuser().getUserNama());
								appendSafe(sbContent, diskusi.getTbmuser().getUserId());
							}

							String textToSearch = sbContent.toString().toLowerCase();
							if (!textToSearch.contains(kataKunci)) {
								return;
							}
						}

						Date tglKey = diskusi.getTanggal_dirubah();
						if (tglKey != null && diskusi.getPertemuan() != null
								&& diskusi.getPertemuan().getId() != null) {
							String keyFormat = Common.dateFormat9.get().format(tglKey);
							StringBuilder sbKey = new StringBuilder(keyFormat).append("_diskusi_")
									.append(diskusi.getId());
							concurrentHasil.put(sbKey.toString(),
									new Object[] { diskusi.getId(), diskusi.getPertemuan().getId(), tglKey });
						}

					} catch (Exception e) {
						if (DEBUG_MODE) System.err.println("DEBUG [Komentar ERROR]: " + e.getMessage());
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/MateriDanKomentarHelper.java:128");
					} finally {
						activeThreads.decrementAndGet();

						int done = progressCount.incrementAndGet();
						if (DEBUG_MODE && (done % 10 == 0 || done == size)) {
							System.out.println("DEBUG [Komentar]: Progress " + done + " / " + size + " selesai.");
						}
						
						latch.countDown();
					}
				}
			});
		}

		executorService.shutdown();
		try {
			boolean finished = latch.await(30, TimeUnit.SECONDS);
			if (!finished) {
				executorService.shutdownNow();
				if (DEBUG_MODE) {
					System.err.println("FATAL DEBUG [Komentar]: Timeout 30 Detik Terlampaui! Ada " + latch.getCount() + " proses yang HANG / tertahan.");
				}
			} else if (DEBUG_MODE) {
				System.out.println("DEBUG [Komentar]: Seluruh tugas paralel sukses diselesaikan.");
			}
		} catch (InterruptedException e) {
			executorService.shutdownNow();
			if (DEBUG_MODE) System.err.println("DEBUG [Komentar]: Interrupted Exception terpicu.");
			Thread.currentThread().interrupt();
		}

		if (!concurrentHasil.isEmpty()) {
			for (Map.Entry<String, Object[]> entry : concurrentHasil.entrySet()) {
				finalResult.put(entry.getKey(), entry.getValue());
			}
		}

		return finalResult;
	}

	/**
	 * Menambahkan {@code text} (setelah di-trim) ke {@code sb} diikuti satu spasi, hanya bila
	 * {@code text} tidak {@code null}/kosong. Dipakai untuk membangun teks gabungan yang dicari
	 * kata kuncinya di {@link #ambilKomentar}, aman terhadap field yang bernilai {@code null}.
	 *
	 * @param sb   buffer tujuan, dimodifikasi di tempat
	 * @param text teks yang akan ditambahkan; diabaikan bila kosong/{@code null}
	 */
	private static void appendSafe(StringBuilder sb, String text) {
		if (text != null && !text.trim().isEmpty()) {
			sb.append(text.trim()).append(" ");
		}
	}

	// =========================================================================================
	// 2. METODE AMBIL MATERI E-LEARNING (PARALEL TERBATAS SESUAI POOL DB)
	// =========================================================================================

	/**
	 * Varian ringkas {@link #ambilMateri(TreeMap, boolean, Label, boolean, Tbmuser)} dengan
	 * {@code urutBerdasarkanNama=false} — hasil diurutkan murni berdasarkan tanggal/waktu konten
	 * (perilaku default untuk tampilan kronologis).
	 */
	public static TreeMap<String, Object[]> ambilMateri(TreeMap<String, Long> pertemuans, boolean refresh, Label label,
			Tbmuser tbmuser) {
		return ambilMateri(pertemuans, refresh, label, false, tbmuser);
	}

	/**
	 * Implementasi kanonik: mengumpulkan SELURUH jenis konten pembelajaran dari kumpulan
	 * {@code pertemuans} yang diberikan — materi berkas ({@link PertemuanFileContent}), audio
	 * ({@link AudioPertemuan}), video ({@link VideoPertemuan}), tugas langsung pada pertemuan
	 * (field {@code judultugas}), tugas relasi ({@link Tugas}), dan ujian
	 * ({@link PertemuanPunyaUjian}) — ke dalam satu peta hasil gabungan terurut, memakai pola
	 * paralelisasi yang sama seperti dijelaskan pada javadoc kelas ini (satu worker per
	 * {@link Pertemuan}, dibatasi {@link DbThreadPool#safe(int)} thread, timeout 30 detik).
	 *
	 * <p>
	 * Bila {@code refresh} bernilai {@code true}, cache anak-data milik {@link Pertemuan}
	 * (kunci {@code pertemuan_file_content}, {@code audio_pertemuan}, {@code video_pertemuan},
	 * {@code pertemuan_tugas}, {@code kelompok_tugas}, {@code pertemuan_punya_Ujian}) dibatalkan
	 * lebih dulu lewat {@code pertemuan.belum(...)} SEBELUM data diambil. Ini sengaja dilakukan
	 * karena tidak semua alur simpan konten (mis. simpan TUGAS) menginvalidasi cache terkait
	 * secara otomatis — tanpa langkah ini, konten yang baru saja ditambahkan pengguna bisa jadi
	 * tidak langsung muncul di ringkasan sampai cache kedaluwarsa dengan sendirinya. Kegagalan
	 * saat membatalkan cache diabaikan (dicatat ke {@link ErrorAuditUtil}, tidak menghentikan
	 * pengambilan data) agar satu invalidasi cache yang gagal tidak menggagalkan seluruh proses.
	 * </p>
	 *
	 * <p>
	 * Setiap jenis konten mendapat kunci unik dengan pola
	 * {@code "<baseKey>_<jenis>_<id>"} (jenis: {@code materi}, {@code audio}, {@code video},
	 * {@code tugas_pertemuan}, {@code tugas}, {@code ujian}), dengan {@code baseKey} berupa
	 * gabungan (opsional) nomor urut/judul pertemuan dan tanggal yang diformat — bila
	 * {@code urutBerdasarkanNama=true}, urutan tampilan mengutamakan nama/urutan pertemuan
	 * sebelum tanggal; bila {@code false}, urutan murni berdasarkan tanggal. Nilai setiap entri
	 * berupa {@code Object[]} beranggotakan {@code {entitasKonten, pertemuanInduk, tanggalKunci}}.
	 * </p>
	 *
	 * @param pertemuans          peta nama pertemuan ke id-nya, sumber data yang akan diproses;
	 *                            bila {@code null}/kosong, mengembalikan peta kosong
	 * @param refresh             bila {@code true}, batalkan cache anak-data pertemuan sebelum
	 *                            mengambil ulang konten dari database (lihat penjelasan di atas)
	 * @param label               komponen ZK {@link Label} diteruskan apa adanya tanpa dipakai
	 *                            langsung di method ini (dipertahankan untuk kesamaan tanda
	 *                            tangan dengan {@link #ambilKomentar})
	 * @param urutBerdasarkanNama bila {@code true}, kunci pengurutan diawali nomor urut/judul
	 *                            pertemuan sebelum tanggal; bila {@code false}, murni berdasarkan
	 *                            tanggal
	 * @param tbmuser             pengguna aktif, diteruskan ke
	 *                            {@link Pertemuan#ambilPertemuanPunyaUjianTotal(Tbmuser)} untuk
	 *                            menentukan cakupan ujian yang boleh dilihat pengguna tersebut
	 * @return peta hasil terurut kunci gabungan ke {@code Object[]}
	 *         {@code {entitasKonten, pertemuanInduk, tanggalKunci}}; tidak pernah {@code null},
	 *         boleh kosong. Sama seperti {@link #ambilKomentar}, hasil dapat berupa hasil
	 *         parsial bila batas waktu 30 detik terlampaui
	 */
	public static TreeMap<String, Object[]> ambilMateri(final TreeMap<String, Long> pertemuans, final boolean refresh,
			final Label label, final boolean urutBerdasarkanNama, final Tbmuser tbmuser) {

		final TreeMap<String, Object[]> finalResult = new TreeMap<String, Object[]>();
		if (pertemuans == null || pertemuans.isEmpty()) {
			return finalResult;
		}

		final int size = pertemuans.size();
		// Sama seperti diskusi: pekerjaan ini DB-bound. Pool per-request yang terlalu besar
		// mempercepat habisnya koneksi, bukan mempercepat halaman.
		int maxThreads = DbThreadPool.safe(size);

		if (DEBUG_MODE) {
			System.out.println("DEBUG [Materi]: Memulai antrean " + size + " tugas materi dengan " + maxThreads + " threads paralel.");
		}

		final ConcurrentSkipListMap<String, Object[]> concurrentMateris = new ConcurrentSkipListMap<String, Object[]>();
		final ExecutorService executorService = Executors.newFixedThreadPool(maxThreads);
		final CountDownLatch latch = new CountDownLatch(size);
		final AtomicInteger progressCount = new AtomicInteger(0);
		final AtomicInteger activeThreads = new AtomicInteger(0);

		for (final Long pertemuanid : pertemuans.values()) {
			executorService.execute(new Runnable() {
				@Override
				public void run() {

					try {
						if (pertemuanid == null) return;

						Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class,
								pertemuanid.toString(), true);
						if (pertemuan == null) return;

						// FRESHNESS (permintaan: konten yg BARU ditambah PASTI muncul): saat REFRESH,
						// batalkan cache anak-data pertemuan (belum) supaya materi/audio/video/tugas/ujian yang
						// baru disimpan PASTI di-query ulang dari DB & tampil — TIDAK bergantung apakah alur-simpan
						// tiap tipe memanggil belum() (mis. simpan TUGAS tak invalidate "pertemuan_tugas"/
						// "kelompok_tugas"). Hanya saat refresh (tombol Refresh / setelah tambah konten) agar load
						// normal tetap cepat memakai cache.
						if (refresh) {
							try {
								pertemuan.belum("pertemuan_file_content");
								pertemuan.belum("audio_pertemuan");
								pertemuan.belum("video_pertemuan");
								pertemuan.belum("pertemuan_tugas");
								pertemuan.belum("kelompok_tugas");
								pertemuan.belum("pertemuan_punya_Ujian");
							} catch (Exception abaikanBelum) { ais.common.ErrorAuditUtil.record(abaikanBelum, "auto-audit(empty-catch) src/ais/common/MateriDanKomentarHelper.java:228");
							}
						}

						Date tglKey = pertemuan.getTanggal();
						if (tglKey == null) return;

						String baseKey = (urutBerdasarkanNama && pertemuan.getPertemuanKe() != null
								? pertemuan.getPertemuanKe().toString()
								: "") + Common.dateFormat8.get().format(tglKey);

						// 1. MATERI (PertemuanFileContent)
						TreeMap<Long, PertemuanFileContent> pertemuanFileContents = pertemuan
								.ambilPertemuanFileContentTotal();
						if (pertemuanFileContents != null) {
							for (PertemuanFileContent pertemuanFileContent : pertemuanFileContents.values()) {
								if (pertemuanFileContent == null || pertemuanFileContent.getId() == null)
									continue;
								StringBuilder sbKey = new StringBuilder(baseKey).append("_materi_")
										.append(pertemuanFileContent.getId());
								concurrentMateris.put(sbKey.toString(),
										new Object[] { pertemuanFileContent, pertemuan, tglKey });
							}
						}

						// 2. AUDIO (AudioPertemuan)
						TreeMap<Long, AudioPertemuan> audioPertemuansa = pertemuan.ambilAudioPertemuanTotal();
						if (audioPertemuansa != null) {
							for (AudioPertemuan audioPertemuan : audioPertemuansa.values()) {
								if (audioPertemuan == null || audioPertemuan.getId() == null)
									continue;
								StringBuilder sbKey = new StringBuilder(baseKey).append("_audio_")
										.append(audioPertemuan.getId());
								concurrentMateris.put(sbKey.toString(),
										new Object[] { audioPertemuan, pertemuan, tglKey });
							}
						}

						// 3. VIDEO (VideoPertemuan)
						TreeMap<Long, VideoPertemuan> videoPertemuansa = pertemuan.ambilVideoPertemuanTotal();
						if (videoPertemuansa != null) {
							for (VideoPertemuan videoPertemuan : videoPertemuansa.values()) {
								if (videoPertemuan == null || videoPertemuan.getId() == null)
									continue;
								StringBuilder sbKey = new StringBuilder(baseKey).append("_video_")
										.append(videoPertemuan.getId());
								concurrentMateris.put(sbKey.toString(),
										new Object[] { videoPertemuan, pertemuan, tglKey });
							}
						}

						// 4. TUGAS (Langsung dari Pertemuan)
						if (pertemuan.getJudultugas() != null && !pertemuan.getJudultugas().trim().isEmpty()) {
							Date tglKeyTugas = pertemuan.getMulai() != null ? pertemuan.getMulai()
									: pertemuan.getTanggal();
							String keyTugas = (urutBerdasarkanNama ? pertemuan.getJudultugas().trim() : "")
									+ Common.dateFormat8.get().format(tglKeyTugas);

							StringBuilder sbKey = new StringBuilder(keyTugas).append("_tugas_pertemuan_")
									.append(pertemuan.getId());
							concurrentMateris.put(sbKey.toString(), new Object[] { pertemuan, pertemuan, tglKeyTugas });
						}

						// 5. TUGAS (Relasi List Tugas)
						TreeMap<Long, Tugas> tugases = pertemuan.ambilTugasTotalSemua();
						if (tugases != null) {
							for (Tugas tugas : tugases.values()) {
								if (tugas == null || tugas.getId() == null)
									continue;

								Date tglKeyTugas = tugas.getMulai() != null ? tugas.getMulai() : pertemuan.getTanggal();
								String keyTugas = (urutBerdasarkanNama && tugas.getJudultugas() != null
										? tugas.getJudultugas().trim()
										: "") + Common.dateFormat8.get().format(tglKeyTugas);

								StringBuilder sbKey = new StringBuilder(keyTugas).append("_tugas_")
										.append(tugas.getId());
								concurrentMateris.put(sbKey.toString(), new Object[] { tugas, pertemuan, tglKeyTugas });
							}
						}

						// 6. UJIAN (PertemuanPunyaUjian)
						TreeMap<Long, PertemuanPunyaUjian> pertemuanPunyaUjians = pertemuan
								.ambilPertemuanPunyaUjianTotal(tbmuser);
						if (pertemuanPunyaUjians != null) {
							for (PertemuanPunyaUjian pertemuanPunyaUjian : pertemuanPunyaUjians.values()) {
								if (pertemuanPunyaUjian == null || pertemuanPunyaUjian.getId() == null)
									continue;

								Date tglKeyUjian = pertemuanPunyaUjian.getMulaiUjian() != null
										? pertemuanPunyaUjian.getMulaiUjian()
										: pertemuan.getTanggal();
								String keyUjian = (urutBerdasarkanNama && pertemuanPunyaUjian.getNama() != null
										? pertemuanPunyaUjian.getNama().trim()
										: "") + Common.dateFormat8.get().format(tglKeyUjian);

								StringBuilder sbKey = new StringBuilder(keyUjian).append("_ujian_")
										.append(pertemuanPunyaUjian.getId());
								concurrentMateris.put(sbKey.toString(),
										new Object[] { pertemuanPunyaUjian, pertemuan, tglKeyUjian });
							}
						}

					} catch (Exception e) {
						if (DEBUG_MODE) System.err.println("DEBUG [Materi ERROR]: Pada pertemuan_id " + pertemuanid + " -> " + e.getMessage());
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/MateriDanKomentarHelper.java:333");
					} finally {
						activeThreads.decrementAndGet();

						int done = progressCount.incrementAndGet();
						if (DEBUG_MODE && (done % 10 == 0 || done == size)) {
							System.out.println("DEBUG [Materi]: Progress " + done + " / " + size + " selesai.");
						}
						
						latch.countDown();
					}
				}
			});
		}

		executorService.shutdown();
		try {
			boolean finished = latch.await(30, TimeUnit.SECONDS);
			if (!finished) {
				executorService.shutdownNow();
				if (DEBUG_MODE) {
					System.err.println("FATAL DEBUG [Materi]: Timeout 30 Detik Terlampaui! Ada " + latch.getCount() + " proses yang HANG / tertahan.");
				}
			} else if (DEBUG_MODE) {
				System.out.println("DEBUG [Materi]: Seluruh tugas paralel sukses diselesaikan.");
			}
		} catch (InterruptedException e) {
			executorService.shutdownNow();
			if (DEBUG_MODE) System.err.println("DEBUG [Materi]: Interrupted Exception terpicu.");
			Thread.currentThread().interrupt();
		}

		if (!concurrentMateris.isEmpty()) {
			for (Map.Entry<String, Object[]> entry : concurrentMateris.entrySet()) {
				finalResult.put(entry.getKey(), entry.getValue());
			}
		}

		return finalResult;
	}
}
