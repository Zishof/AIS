package ais.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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

public class DataProcessor { // Ganti nama class sesuai file Anda

	private static boolean hasEncript = false;
	// Konstanta untuk path panjang agar lebih rapi
	private static final String SCORM_BASE_PATH = "/f/s/2/s/s/e/e/w/f/s/2/s/s/e/e/w/f/s/2/s/s/e/e/w/f/s/2/s/s/e/e/w/f/s/2/s/s/e/e/w/f/s/2/s/s/e/e/w/scorm/";

	private static final Object LOCK_INIT_ENCRIPT = new Object();

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
		} catch (Exception e) {
			// Log error tapi jangan stop loop
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DataProcessor.java:223");
		}
	}

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