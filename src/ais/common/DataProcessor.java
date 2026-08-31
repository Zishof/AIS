package ais.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.LabelBahasa;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.PertemuanFileContent;
import ais.ui.util.UIUtil;

// Pastikan import class entity Anda sesuai (LampiranLain, dll)

/**
 * Kelas utilitas berisi rutin <b>inisialisasi/migrasi data satu-kali saat startup aplikasi</b>
 * ("Encrip Semua" — nama historis, bukan tentang enkripsi kriptografi melainkan tampaknya
 * singkatan/istilah internal untuk "proses semua" data lama yang perlu disiapkan/dimigrasikan).
 * Satu-satunya method publik, {@link #encripSemua()}, dipanggil sekali saat aplikasi start dan
 * menjalankan beberapa pekerjaan latar belakang independen secara paralel di thread terpisah:
 * ekstraksi berkas laporan JRXML dan ZIP SCORM yang belum diekstrak ke disk, pemuatan cache label
 * multi-bahasa ke memori ({@link MemoryDbUtil}), inisialisasi menu aplikasi, dan migrasi data
 * historis kolom {@code tahunAkademik} pada {@link Detailperkuliahan} yang belum terisi.
 *
 * <p>
 * <b>Catatan penamaan:</b> komentar {@code // Ganti nama class sesuai file Anda} dan
 * {@code // Pastikan import class entity Anda sesuai} pada kode asli mengindikasikan kelas ini
 * kemungkinan berasal dari templat/potongan kode yang di-generate atau ditempel dari sumber lain,
 * lalu disesuaikan untuk kebutuhan AIS. Nama kelas dan struktur dipertahankan apa adanya sesuai
 * instruksi pekerjaan dokumentasi ini.
 * </p>
 *
 * <h2>Detail per pekerjaan pada {@link #encripSemua()}</h2>
 * <ul>
 * <li><b>Thread 1 (proses berkas):</b> membuka sesi Hibernate streaming terpisah
 * ({@link StreamingHibernateUtil}) untuk memproses tiga kategori data secara berurutan dalam satu
 * thread: (a) seluruh {@link LampiranLain} berekstensi {@code .jrxml} yang templat fisiknya belum
 * ada di direktori laporan ({@link #processJrxml(LampiranLain)}); (b) seluruh
 * {@link ais.database.model.file.PertemuanFileContent} dengan {@code lokasiFisik} terisi, diekstrak
 * sebagai ZIP SCORM ({@link #prosesUnzip(String, File)}); (c) seluruh {@link LampiranLain} dengan
 * {@code lokasiFisik} terisi, diekstrak dengan cara yang sama. Setiap item diproses dalam blok
 * {@code try/catch} tersendiri agar satu berkas bermasalah (hilang, korup) tidak menghentikan
 * seluruh batch.</li>
 * <li><b>Thread 2 (cache bahasa):</b> memuat seluruh baris {@link LabelBahasa} dari database dan
 * mengisi tiga peta in-memory ({@code MemoryDbUtil.getBahasaIndonesias/Englishs/Arabs()}) agar
 * pencarian terjemahan label UI tidak perlu query database berulang saat runtime.</li>
 * <li><b>Thread utama (menu + migrasi akademik):</b> memanggil {@code UIUtil.initMenus} dan
 * {@code MenuInitializer.initMenus} secara sinkron pada sesi Hibernate terisolasi, lalu menghitung
 * jumlah baris {@link Detailperkuliahan} yang kolom {@code tahunAkademik}-nya masih kosong padahal
 * relasi mahasiswa dan semester (bukan nol) sudah terisi; bila ada, seluruh id baris tersebut
 * diambil lalu migrasi dijalankan pada thread ketiga terpisah lewat
 * {@link #updateDetailPerkuliahanBatch(List)}.</li>
 * </ul>
 *
 * <p>
 * Dijaga idempoten oleh flag statis {@link #hasEncript}: pemanggilan {@link #encripSemua()}
 * kedua dan seterusnya pada JVM yang sama langsung kembali tanpa melakukan apa pun. Flag ini
 * di-set {@code true} SEBELUM blok {@code synchronized} dijalankan, sehingga panggilan bersamaan
 * (concurrent) yang tiba tepat sebelum flag ter-set berpotensi (secara teori, dalam jendela waktu
 * yang sangat sempit) lolos pemeriksaan bersama-sama; blok {@link #LOCK_INIT_ENCRIPT} sendiri
 * hanya mengunci badan method setelah pengecekan flag, bukan pengecekan-dan-set flag secara atomik.
 * </p>
 */
public class DataProcessor { // Ganti nama class sesuai file Anda

	private static final Logger log = Logger.getLogger(DataProcessor.class);

	/** Penanda apakah {@link #encripSemua()} sudah pernah dijalankan pada JVM ini (guard idempoten). */
	private static boolean hasEncript = false;
	// Konstanta untuk path panjang agar lebih rapi
	/**
	 * Path dasar (relatif terhadap {@link Common#REAL_PATH}) tempat hasil ekstraksi ZIP SCORM
	 * disimpan. Struktur direktori berlapis yang tidak lazim ({@code /f/s/2/s/s/e/e/w/...} diulang
	 * enam kali) tampaknya sengaja dibuat dalam/tersembunyi (mungkin untuk alasan historis atau
	 * menghindari akses langsung); nilainya dipertahankan apa adanya.
	 */
	private static final String SCORM_BASE_PATH = "/f/s/2/s/s/e/e/w/f/s/2/s/s/e/e/w/f/s/2/s/s/e/e/w/f/s/2/s/s/e/e/w/f/s/2/s/s/e/e/w/f/s/2/s/s/e/e/w/scorm/";

	/** Objek kunci untuk blok {@code synchronized} pada {@link #encripSemua()}. */
	private static final Object LOCK_INIT_ENCRIPT = new Object();

	/**
	 * Titik masuk tunggal kelas ini: menjalankan seluruh pekerjaan inisialisasi/migrasi data
	 * satu-kali (ekstraksi berkas JRXML/ZIP, pemuatan cache bahasa, inisialisasi menu, migrasi
	 * {@code tahunAkademik}) secara paralel di beberapa thread latar belakang. Aman dipanggil
	 * berkali-kali — hanya pemanggilan pertama pada JVM yang benar-benar bekerja (lihat
	 * {@link #hasEncript}). Lihat Javadoc kelas untuk rincian lengkap tiap pekerjaan.
	 */
	@SuppressWarnings("unchecked")
	public static void encripSemua() {
		// Cek state
		if (hasEncript) {
			return;
		}
		hasEncript = true;

		synchronized (LOCK_INIT_ENCRIPT) {

			// --- THREAD 1: Pemrosesan File (JRXML & ZIP) ---
			new Thread(new Runnable() {
				@Override
				public void run() {
					Session streamingSession = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
					try {
						// 1. Proses JRXML
						List<LampiranLain> jrxmlList = streamingSession.createCriteria(LampiranLain.class)
								.add(Restrictions.ilike("nama", ".jrxml", MatchMode.END)).list();

						for (LampiranLain lampiran : jrxmlList) {
							// PENJAGA: satu berkas jrxml bermasalah (mis. fisik hilang) jangan
							// menghentikan seluruh batch thread ini -> log lalu lanjut item berikutnya.
							try {
								processJrxml(lampiran);
							} catch (Exception eItem) {
								ais.common.ErrorAuditUtil.record(eItem,
										"dataprocessor-jrxml-item-gagal-lanjut src/ais/common/DataProcessor.java:57");
							}
						}

						// 2. Proses ZIP (PertemuanFileContent)
						List<PertemuanFileContent> pertemuanList = streamingSession
								.createCriteria(PertemuanFileContent.class).add(Restrictions.isNotNull("lokasiFisik"))
								.list();

						for (PertemuanFileContent content : pertemuanList) {
							// PENJAGA: content.ambilFile() bisa mencatat FileNotFoundException (mis.
							// PertemuanFileContent/<id>/ezyzip.zip hilang) via FileFoto.ambilFile lalu
							// mengembalikan placeholder; prosesUnzip sendiri sudah aman krn skip bila
							// !exists(). Tetap bungkus per-item agar RuntimeException lain (mis. unzip
							// korup) tak menghentikan item2 berikutnya di batch ini.
							try {
								// Menggunakan helper method untuk menghindari duplikasi kode
								prosesUnzip(content.getLokasiFisik(), content.ambilFile());
							} catch (Exception eItem) {
								ais.common.ErrorAuditUtil.record(eItem,
										"dataprocessor-pertemuanfilecontent-item-gagal-lanjut src/ais/common/DataProcessor.java:67");
							}
						}

						// 3. Proses ZIP (LampiranLain)
						List<LampiranLain> lampiranZipList = streamingSession.createCriteria(LampiranLain.class)
								.add(Restrictions.isNotNull("lokasiFisik")).list();

						for (LampiranLain lampiran : lampiranZipList) {
							try {
								prosesUnzip(lampiran.getLokasiFisik(), lampiran.ambilFile());
							} catch (Exception eItem) {
								ais.common.ErrorAuditUtil.record(eItem,
										"dataprocessor-lampiranlain-zip-item-gagal-lanjut src/ais/common/DataProcessor.java:75");
							}
						}

					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DataProcessor.java:79");
					} finally {
						// Pastikan session selalu ditutup
						if (streamingSession != null && streamingSession.isOpen()) {
							streamingSession.disconnect();
							streamingSession.close();
						}
					}
				}
			}).start();

			// --- THREAD 2: Cache Bahasa ---
			new Thread(new Runnable() {
				@Override
				public void run() {
					Session session = null;
					try {
						session = HibernateUtil.currentNativeSession();
						List<LabelBahasa> labelBahasas = session.createCriteria(LabelBahasa.class).list();
						for (LabelBahasa label : labelBahasas) {
							MemoryDbUtil.getBahasaIndonesias().put(label.getNama(), label.getIndonesia());
							MemoryDbUtil.getBahasaEnglishs().put(label.getNama(), label.getEnglish());
							MemoryDbUtil.getBahasaArabs().put(label.getNama(), label.getArab());
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DataProcessor.java:104");
					} finally {
						if (session != null && session.isOpen()) {
							// session.disconnect();
							if (session.isOpen()) {
								session.disconnect();
								session.close();
							}
						}
						HibernateUtil.closeSession();
					}
				}
			}).start();

			// 1. Buka Session Baru (Isolated Session)
			// Menggunakan openSession() agar terpisah dari session HTTP request
			Session session = HibernateUtil.getSessionFactory().openSession();

			try {
				UIUtil.initMenus(session);
				MenuInitializer.initMenus(session);

				// Cek jumlah data yang perlu diupdate
				Number countNum = (Number) session.createCriteria(Detailperkuliahan.class)
						.add(Restrictions.isNull("tahunAkademik")).add(Restrictions.isNotNull("mahasiswa"))
						.add(Restrictions.isNotNull("semester")).add(Restrictions.ne("semester", 0))
						.setProjection(Projections.rowCount()).uniqueResult();

				int count = (countNum != null) ? countNum.intValue() : 0;

				if (count > 0) {
					// Ambil semua ID sekaligus
					final List<Long> ids = session.createCriteria(Detailperkuliahan.class)
							.add(Restrictions.isNull("tahunAkademik")).add(Restrictions.isNotNull("mahasiswa"))
							.add(Restrictions.isNotNull("semester")).add(Restrictions.ne("semester", 0))
							.setProjection(Projections.property("id")).list();

					// Jalankan Thread Update
					new Thread(new Runnable() {
						@Override
						public void run() {
							updateDetailPerkuliahanBatch(ids);
						}
					}).start();
				}

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DataProcessor.java:151");
			} finally {
				// 2. WAJIB Tutup Session
				if (session != null && session.isOpen()) {
					// session.disconnect();
					if (session.isOpen()) {
						session.disconnect();
						session.close();
					}
				}
				HibernateUtil.closeSession();
			}

		}
	}

	// --- HELPER METHODS (Private) ---

	/**
	 * Menyalin templat laporan JRXML dari lokasi penyimpanan lampiran ({@code lampiran.ambilFile()})
	 * ke direktori laporan aplikasi ({@code Common.ambilREAL_PATH_REPORT()}), hanya bila berkas
	 * tujuan belum ada di sana. Kegagalan (mis. berkas sumber hilang) dicatat ke
	 * {@link ais.common.ErrorAuditUtil} dan diabaikan (tidak dilempar ulang) agar item lain dalam
	 * batch tetap diproses.
	 *
	 * @param lampiran entitas {@link LampiranLain} yang mewakili satu berkas {@code .jrxml}
	 */
	private static void processJrxml(LampiranLain lampiran) {
		try {
			File fileJrxml = new File(Common.ambilREAL_PATH_REPORT() + "/" + lampiran.getNama());
			if (!fileJrxml.exists()) {
				File fileSource = lampiran.ambilFile();
				if (fileSource != null && fileSource.exists()) {
					// Try-with-resources (Java 7+) otomatis menutup stream
					try {
						FileInputStream fis = new FileInputStream(fileSource);
						FileOutputStream fos = new FileOutputStream(fileJrxml);
						IOUtils.copyLarge(fis, fos);
						fos.close();
						fis.close();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DataProcessor.java:182");
//						e.printStackTrace();
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DataProcessor.java:187");
//			e.printStackTrace();
		}
	}

	/**
	 * Mengekstrak satu berkas ZIP (paket SCORM) ke direktori tujuan di bawah
	 * {@link #SCORM_BASE_PATH}, dengan sejumlah pemeriksaan untuk data lama yang berpotensi rusak:
	 * berkas 0-byte dilewati lebih awal (dianggap upload terputus, bukan ZIP valid), validitas
	 * struktur ZIP diperiksa lebih dulu ({@code new ZipFile(...)}) SEBELUM direktori hasil
	 * ekstraksi lama dihapus (agar hasil ekstraksi lama tetap dipertahankan bila sumber ternyata
	 * rusak), dan {@link ZipException} akibat ZIP korup dicatat sebagai peringatan ringkas (tanpa
	 * stack trace penuh/audit DB) agar log tidak dibanjiri pada migrasi data lama berskala besar.
	 *
	 * @param lokasiFisik        path berkas ZIP utama yang dicoba lebih dulu
	 * @param fileSourceFallback berkas alternatif (mis. hasil {@code ambilFile()} pada entitas)
	 *                           yang dipakai bila {@code lokasiFisik} tidak ditemukan di disk
	 */
	private static void prosesUnzip(String lokasiFisik, File fileSourceFallback) {
		try {
			File fileZip = new File(lokasiFisik);

			if (!fileZip.exists()) {
				fileZip = fileSourceFallback;
			}

			if (fileZip != null && fileZip.exists() && fileZip.length() == 0L) {
				// Berkas ada tapi kosong (0 byte) -- placeholder/upload terputus, bukan zip
				// valid. new ZipFile() dijamin gagal (ZipException), jadi lewati lebih awal
				// tanpa membanjiri log dengan stack trace yang sudah pasti terjadi.
				return;
			}

			if (fileZip != null && fileZip.exists()) {
				/* Validasi central directory SEBELUM direktori hasil lama dihapus. Ini
				 * membedakan ZIP valid dari upload terputus/berkas lain berekstensi zip,
				 * serta mempertahankan hasil ekstraksi lama bila sumber rusak. */
				ZipFile zipValidasi = null;
				try {
					zipValidasi = new ZipFile(fileZip);
				} finally {
					if (zipValidasi != null) {
						try { zipValidasi.close(); } catch (Exception ignored) { }
					}
				}
				String folderName = fileZip.getName().replace(".zip", "").replaceAll(" ", "_");
				String fullPath = Common.REAL_PATH + SCORM_BASE_PATH + folderName;

				File dir = new File(fullPath);

				// Hapus direktori lama jika ada
				if (dir.exists()) {
					try {
						FileUtils.deleteDirectory(dir);
					} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DataProcessor.java:210");
					}
				}

				dir.mkdirs();

				Path source = Paths.get(fileZip.getAbsolutePath());
				Path target = Paths.get(dir.getAbsolutePath());

				Common.unzipFolder(source, target);
			}
		} catch (ZipException zipRusak) {
			// Berkas non-kosong tapi bukan struktur zip valid (data legacy korup) --
			// kondisi yang sudah diketahui dan tak bisa diperbaiki di sini. Log ringkas
			// tanpa stack trace/audit-DB penuh agar batch besar tak membanjiri log.
			log.warn("Lewati unzip, bukan berkas zip valid: " + lokasiFisik + " (" + zipRusak.getMessage() + ")");
		} catch (Exception e) {
			// Log error tapi jangan stop loop
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DataProcessor.java:223");
		}
	}

	/**
	 * Migrasi data historis: mengisi kolom {@code tahunAkademik} yang masih kosong pada
	 * sekumpulan baris {@link Detailperkuliahan} (id-nya sudah dihitung sebelumnya oleh
	 * {@link #encripSemua()}), dihitung dari tahun angkatan dan semester mulai mahasiswa terkait
	 * lewat {@link Common#getTahunAkademik(Integer, Integer, String)}. Diproses dalam satu
	 * transaksi Hibernate dengan flush+clear session setiap 20 baris (batching) untuk menjaga
	 * penggunaan memori tetap terkendali pada migrasi data berskala besar. Bila terjadi galat,
	 * seluruh transaksi di-rollback dan galat ditampilkan/dicatat lewat
	 * {@link Common#tampilErrorJikaAdmin(Exception)}.
	 *
	 * @param ids daftar id {@link Detailperkuliahan} yang akan dimigrasikan
	 */
	private static void updateDetailPerkuliahanBatch(List<Long> ids) {
		Session mySession = null;
		Transaction tx = null;
		try {
			mySession = HibernateUtil.currentNativeSession();
			tx = mySession.beginTransaction();

			int batchSize = 20; // Hibernate batch size recommend
			int i = 0;

			for (Long id : ids) {
				Detailperkuliahan dp = (Detailperkuliahan) mySession.get(Detailperkuliahan.class, id);

				if (dp != null && dp.getMahasiswa() != null) {
					Integer tahunAngkatan = dp.getMahasiswa().getTahunangkatan();
					String semesterMulai = dp.getMahasiswa().getSemesterMulai();

					if (tahunAngkatan != null && semesterMulai != null) {
						Integer tahunMulai = Common.getTahunAkademik(dp.getSemester(), tahunAngkatan, semesterMulai);
						String tahunAkademik = tahunMulai + "/" + (tahunMulai + 1);

						dp.setTahunAkademik(tahunAkademik);
						mySession.update(dp); // Cukup update, commit nanti di luar loop
					}
				}

				// Batching: Flush dan clear session tiap 20 data agar memori tidak penuh
				if (++i % batchSize == 0) {
					mySession.flush();
					mySession.clear();
				}
			}

			tx.commit();
		} catch (Exception e) {
			if (tx != null)
				tx.rollback();
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (mySession != null && mySession.isOpen()) {
				// mySession.disconnect();
				if (mySession.isOpen()) {
					mySession.disconnect();
					mySession.close();
				}
			}
			HibernateUtil.closeSession();
		}
	}
}
