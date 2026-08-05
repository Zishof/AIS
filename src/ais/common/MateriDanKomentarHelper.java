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

public class MateriDanKomentarHelper {

	// Switch untuk menghidupkan/mematikan log debug di console
	private static final boolean DEBUG_MODE = false;

	// =========================================================================================
	// 1. METODE AMBIL KOMENTAR / DISKUSI (PARALEL MAX 30 THREADS)
	// =========================================================================================

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
		// Dioptimalkan ke 230 threads agar Database Pool & Memory Tomcat stabil
		int maxThreads = Math.min(230, size);

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

		try {
			boolean finished = latch.await(30, TimeUnit.SECONDS);
			if (!finished && DEBUG_MODE) {
				System.err.println("FATAL DEBUG [Komentar]: Timeout 30 Detik Terlampaui! Ada " + latch.getCount() + " proses yang HANG / tertahan.");
			} else if (DEBUG_MODE) {
				System.out.println("DEBUG [Komentar]: Seluruh tugas paralel sukses diselesaikan.");
			}
			executorService.shutdown();
		} catch (InterruptedException e) {
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

	private static void appendSafe(StringBuilder sb, String text) {
		if (text != null && !text.trim().isEmpty()) {
			sb.append(text.trim()).append(" ");
		}
	}

	// =========================================================================================
	// 2. METODE AMBIL MATERI E-LEARNING (PARALEL MAX 30 THREADS)
	// =========================================================================================

	public static TreeMap<String, Object[]> ambilMateri(TreeMap<String, Long> pertemuans, boolean refresh, Label label,
			Tbmuser tbmuser) {
		return ambilMateri(pertemuans, refresh, label, false, tbmuser);
	}

	public static TreeMap<String, Object[]> ambilMateri(final TreeMap<String, Long> pertemuans, final boolean refresh,
			final Label label, final boolean urutBerdasarkanNama, final Tbmuser tbmuser) {

		final TreeMap<String, Object[]> finalResult = new TreeMap<String, Object[]>();
		if (pertemuans == null || pertemuans.isEmpty()) {
			return finalResult;
		}

		final int size = pertemuans.size();
		// Dioptimalkan ke 230 threads agar Database Pool & Memory Tomcat stabil
		int maxThreads = Math.min(230, size);

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

		try {
			boolean finished = latch.await(30, TimeUnit.SECONDS);
			if (!finished && DEBUG_MODE) {
				System.err.println("FATAL DEBUG [Materi]: Timeout 30 Detik Terlampaui! Ada " + latch.getCount() + " proses yang HANG / tertahan.");
			} else if (DEBUG_MODE) {
				System.out.println("DEBUG [Materi]: Seluruh tugas paralel sukses diselesaikan.");
			}
			executorService.shutdown();
		} catch (InterruptedException e) {
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