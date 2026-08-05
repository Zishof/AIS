package ais.database.hibernate;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.envers.event.AuditEventListener;
import org.hibernate.event.PostDeleteEvent;
import org.hibernate.event.PostInsertEvent;
import org.hibernate.event.PostUpdateEvent;
import org.json.JSONObject;

import ais.action.master.PengumumanAkademisAction;
import ais.action.master.helper.KegiatanHelper;
import ais.action.report.CommonReportHelper;
import ais.action.ws.util.CommonUtil;
import ais.action.ws.util.ConstantUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.FilterLoginAis;
import ais.common.InitDataHelper;
import ais.common.MemoryCacheUtil;
import ais.common.MemoryDbUtil;
import ais.database.model.BankSoalDetail;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.BukuBahanAjar;
import ais.database.model.ChecklistBaruPenilaianDosenOlehMahasiswa;
import ais.database.model.ChecklistHasilPenilaianUmum;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.Detailperkuliahan;
import ais.database.model.DiskusiPengumumanAkademis;
import ais.database.model.Dosen;
import ais.database.model.FormatNilai;
import ais.database.model.FormulirKegiatanPeserta;
import ais.database.model.GeneralValueObject;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.HasilUjianMahasiswaDetail;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.IsiAngketParameterUmum;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.KegiatanKedosenanPunyaDosen;
import ais.database.model.KegiatanKemahasiswaanPunyaMahasiswa;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatKelompokKkn;
import ais.database.model.MahasiswaDapatKelompokPkl;
import ais.database.model.MahasiswaJadiAsisten;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Matakuliah;
import ais.database.model.MatakuliahEkivalen;
import ais.database.model.OrganisasiDosenPunyaDosen;
import ais.database.model.OrganisasiIntraKampusPunyaMahasiswa;
import ais.database.model.PendaftaranWisuda;
import ais.database.model.PengajuanIzinTidakMasukPerkuliahan;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.PenghargaanDosen;
import ais.database.model.PenghargaanMahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaDiskusi;
import ais.database.model.PertemuanPunyaGrupPertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.PrestasiDosen;
import ais.database.model.PrestasiMahasiswa;
import ais.database.model.RolePrivilage;
import ais.database.model.Skripsi;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.TugasKelompok;
import ais.database.model.TugasPertemuan;
import ais.database.model.UjianPunyaSoal;
import ais.database.model.file.FileFoto;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.PertemuanFileContent;
import ais.database.model.file.TugasFileContent;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.penelitiandanpengabdian.Artikel;
import ais.database.model.penelitiandanpengabdian.PengajuanPenelitianDanPengabdian;
import ais.database.model.pkl.KelompokPkl;
import ais.database.model.rab.PenggunaanAnggaran;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Tagihan;
import ais.database.model.sisdes.Penduduk;
import ais.database.model.streaming.AudioPertemuan;
import ais.database.model.streaming.VideoPertemuan;

public class AuditListener extends AuditEventListener {

	// Scheduler TUNGGAL (daemon) untuk tugas tertunda audit (mis. checkGenNim +5 dtk). DULU dipanggil
	// `Executors.newSingleThreadScheduledExecutor().schedule(...)` PER-event → tiap panggilan melahirkan
	// pool `pool-N-thread-1` yang TAK pernah di-shutdown & parkir selamanya di DelayedWorkQueue.take
	// (snapshot: puluhan pool-N idle, pool-856 = pool ke-856, puncak 1204 thread, 626rb total dimulai).
	// Satu scheduler dipakai-ulang → tak bocor. Ukuran 2 = terbatas + tahan bila satu tugas lambat.
	private static final java.util.concurrent.ScheduledExecutorService AUDIT_SCHEDULER = java.util.concurrent.Executors
			.newScheduledThreadPool(2, new java.util.concurrent.ThreadFactory() {
				private final java.util.concurrent.atomic.AtomicInteger seq = new java.util.concurrent.atomic.AtomicInteger();

				@Override
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r, "audit-scheduler-" + seq.incrementAndGet());
					t.setDaemon(true);
					return t;
				}
			});

	// Pembatas konkurensi tugas audit asinkron (webhook/curl saat data berubah). DULU tiap event audit
	// = `new Thread(...).start()` (tak terbatas) → menyumbang churn thread besar (snapshot: 626rb total
	// dimulai). Sekarang lewat pool TETAP + backpressure (CallerRunsPolicy). Ukuran via -Daudit.pool.size
	// (default 4). Wrapper menutup Session Hibernate thread-local tiap tugas (thread pool dipakai-ulang).
	private static final int AUDIT_POOL_SIZE;
	static {
		int n = 4;
		try {
			n = Integer.parseInt(System.getProperty("audit.pool.size", "4").trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/hibernate/AuditListener.java:129");
		}
		AUDIT_POOL_SIZE = n < 1 ? 1 : n;
	}

	private static final java.util.concurrent.ExecutorService AUDIT_POOL = new java.util.concurrent.ThreadPoolExecutor(
			AUDIT_POOL_SIZE, AUDIT_POOL_SIZE, 60L, java.util.concurrent.TimeUnit.SECONDS,
			new java.util.concurrent.ArrayBlockingQueue<Runnable>(1000), new java.util.concurrent.ThreadFactory() {
				private final java.util.concurrent.atomic.AtomicInteger seq = new java.util.concurrent.atomic.AtomicInteger();

				@Override
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r, "audit-worker-" + seq.incrementAndGet());
					t.setDaemon(true);
					return t;
				}
			}, new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

	/**
	 * Menjalankan tugas audit asinkron pada pool TETAP (pengganti {@code new Thread(...).start()}).
	 * Menutup Session Hibernate thread-local setelah tiap tugas (thread pool dipakai-ulang). Bila
	 * antrean penuh, dijalankan di thread pemanggil (backpressure), bukan melahirkan thread baru.
	 */
	private static void jalankanAudit(final Runnable r) {
		Runnable wrapped = new Runnable() {
			@Override
			public void run() {
				try {
					r.run();
				} finally {
					try {
						HibernateUtil.closeSession();
					} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/database/hibernate/AuditListener.java:161");
					}
				}
			}
		};
		try {
			AUDIT_POOL.execute(wrapped);
		} catch (Throwable t) {
			wrapped.run();
		}
	}

	/**
	 *
	 */
	private static final long serialVersionUID = 7612168173112689006L;

	/*
	 * Guard re-entransi proses().
	 *
	 * Akar masalah (lihat error statement timeout & StackOverflowError):
	 * onPostUpdate -> proses() -> HistoryStatusMahasiswaUtil.currentStatus ->
	 * Common.singkronkanKrsMahasiswa -> CommonHibernateHelper.refreshSaveOrUpdate ->
	 * safeFlush -> session.flush() -> memicu onPostUpdate lagi pada entity yang sama ->
	 * proses() lagi -> ... (tak berujung). Pada UI menumpuk hingga StackOverflowError,
	 * pada batch update menahan transaksi sampai "canceling statement due to statement
	 * timeout".
	 *
	 * Dengan ThreadLocal ini, hanya pemanggilan proses() TERLUAR per thread yang
	 * dijalankan penuh. flush() bersarang yang memicu onPost* berikutnya akan melewati
	 * proses() (logika sinkronisasi/audit), namun super.onPost*() tetap dipanggil
	 * sehingga seluruh perilaku lama tetap berjalan. Kompatibel Java 1.6/1.7
	 * (tanpa lambda/diamond).
	 */
	private static final ThreadLocal<Boolean> PROSES_AKTIF = new ThreadLocal<Boolean>();

	private static boolean masukProses() {
		Boolean aktif = PROSES_AKTIF.get();
		if (aktif != null && aktif.booleanValue()) {
			return false;
		}
		PROSES_AKTIF.set(Boolean.TRUE);
		return true;
	}

	private static void keluarProses() {
		try {
			PROSES_AKTIF.remove();
		} catch (Exception e) {
			PROSES_AKTIF.set(null);
		}
	}

	public static void prosesUntukElearning(Serializable serializable, String cla, Serializable id) {
		prosesUntukElearning(serializable, cla, id, null);
	}

	public static void prosesUntukElearning(Serializable serializable, String cla, Serializable id,
			Map<Long, GeneralValueObject> voMahasiswaDosens) {

		// 1. Validasi awal & Inisialisasi
		if (serializable == null)
			return;

		// Mencegah NullPointerException jika cla null
		String action = (cla != null) ? cla.trim() : "";
		boolean isPostDelete = action.contains("onPostDelete");

		String data = "";
		String className = serializable.getClass().getName();
		Mahasiswa mahasiswa = null;
		// Inisialisasi list kosong agar tidak perlu cek null di bawah
		List<Dosen> dosens = new ArrayList<Dosen>();

		try {
			// 2. Mapping Object berdasarkan Tipe
			if (serializable instanceof Skripsi) {
				Skripsi s = (Skripsi) serializable;
				mahasiswa = s.getMahasiswa();
				data = safeString(s.getId());
				dosens = safeList(s.populateDosenBuNama());

			} else if (serializable instanceof MahasiswaRequestTugasAkhir) {
				MahasiswaRequestTugasAkhir m = (MahasiswaRequestTugasAkhir) serializable;
				mahasiswa = m.getMahasiswa();
				data = safeString(m.getId());
				dosens = safeList(m.populateDosenBuNama());

			} else if (serializable instanceof KrsMahasiswa) {
				KrsMahasiswa k = (KrsMahasiswa) serializable;
				mahasiswa = k.getMahasiswa();
				data = safeString(k.getId());
				dosens = safeList(k.populateDosenBuNama());

			} else if (serializable instanceof PertemuanPunyaGrupPertemuan) {
				PertemuanPunyaGrupPertemuan p = (PertemuanPunyaGrupPertemuan) serializable;
				mahasiswa = p.getMahasiswa();
				data = safeString(p.getId());
				dosens = safeList(p.populateDosenBuNama());

			} else if (serializable instanceof PendaftaranWisuda) {
				PendaftaranWisuda p = (PendaftaranWisuda) serializable;
				mahasiswa = p.getMahasiswa();
				data = safeString(p.getId());

			} else if (serializable instanceof FormulirKegiatanPeserta) {
				FormulirKegiatanPeserta f = (FormulirKegiatanPeserta) serializable;
				// Logika asli: prioritaskan Mahasiswa, jika null baru cek Dosen
				if (f.getMahasiswa() != null) {
					mahasiswa = f.getMahasiswa();
					data = safeString(f.getId());
				} else if (f.getDosen() != null) {
					dosens.add(f.getDosen());
					data = safeString(f.getId());
				}

			}

			else if (serializable instanceof KelompokKkn) {
				KelompokKkn p = (KelompokKkn) serializable;
				if (isPostDelete) {
					p.removeMahasiswaDapatKelompokKkn(id);
				} else {
					dosens = safeList(p.populateDosenBuNama());
				}
			}

			else if (serializable instanceof KelompokPkl) {
				KelompokPkl p = (KelompokPkl) serializable;
				if (isPostDelete) {
					p.removeMahasiswaDapatKelompokPkl(id);
				} else {
					dosens = safeList(p.populateDosenBuNama());
				}
			}

			else if (serializable instanceof MahasiswaDapatKelompokKkn) {
				// Logika KKN digabung di sini agar tidak cast dua kali
				MahasiswaDapatKelompokKkn p = (MahasiswaDapatKelompokKkn) serializable;
				mahasiswa = p.getMahasiswa();

				// Logika Khusus Kelompok (KKN)
				if (isPostDelete) {
					data = ""; // Reset data sesuai logika asli
					if (p.getKelompokKkn() != null) {
						p.getKelompokKkn().removeMahasiswaDapatKelompokKkn(id);
						// Update referensi ID lokal (jika diperlukan untuk logika selanjutnya)
						// id = p.getKelompokKkn().getId();
					}
				} else {
					data = safeString(p.getId());
					if (p.getKelompokKkn() != null) {
						dosens = safeList(p.getKelompokKkn().populateDosenBuNama());
						p.getKelompokKkn().populateMahasiswaDapatKelompokKkn(p);
						// id = p.getKelompokKkn().getId();
					}
				}

			} else if (serializable instanceof MahasiswaDapatKelompokPkl) {
				// Logika PKL digabung di sini
				MahasiswaDapatKelompokPkl p = (MahasiswaDapatKelompokPkl) serializable;
				mahasiswa = p.getMahasiswa();

				// Logika Khusus Kelompok (PKL)
				if (isPostDelete) {
					data = "";
					if (p.getKelompokPkl() != null) {
						p.getKelompokPkl().removeMahasiswaDapatKelompokPkl(id);
					}
				} else {
					data = safeString(p.getId());
					if (p.getKelompokPkl() != null) {
						dosens = safeList(p.getKelompokPkl().populateDosenBuNama());
						p.getKelompokPkl().populateMahasiswaDapatKelompokPkl(p);
					}
				}
			}

			// 3. Eksekusi Penyimpanan (Mahasiswa)
			if (mahasiswa != null) {
				mahasiswa.putBaru(data, className);
				if (voMahasiswaDosens == null) {
					mahasiswa.tulisPutBaru(className);
				} else {
					voMahasiswaDosens.put(mahasiswa.getId(), mahasiswa);
				}
			}

			// 4. Eksekusi Penyimpanan (Dosen)
			// Tidak perlu cek null karena dosens diinisialisasi ArrayList kosong
			for (Dosen dosen : dosens) {
				if (dosen == null)
					continue; // Safety check
				try {
					dosen.putBaru(data, className);
					if (voMahasiswaDosens == null) {
						dosen.tulisPutBaru(className);
					} else {
						voMahasiswaDosens.put(dosen.getId(), dosen);
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/hibernate/AuditListener.java:362"); // Sebaiknya dilog, jangan dikosongkan
				}
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/hibernate/AuditListener.java:367");
		}
	}

// --- Helper Methods (Private) untuk kebersihan kode ---

// Mencegah NullPointerException saat convert ID ke String
	private static String safeString(Object obj) {
		return (obj != null) ? obj.toString() : "";
	}

// Mencegah NullPointerException jika method populate mengembalikan null
	private static <T> List<T> safeList(List<T> list) {
		return (list != null) ? list : new ArrayList<T>();
	}

	private void proses(Serializable serializable, String cla, Serializable id) {

		PenggunaanAnggaran.simpan(serializable);

		if (ConstantValues.ketikaUbahDataPenggunaKirimke) {

			if (serializable instanceof Mahasiswa) {
				final Mahasiswa mahasiswa = (Mahasiswa) serializable;
				jalankanAudit(new Runnable() {

					@SuppressWarnings({ "rawtypes", "unchecked" })
					@Override
					public void run() {

						if (!ConstantValues.ketikaUbahDataPenggunaKirimkeLink.isEmpty()) {
							String hasil = "";
							try {

								Map parameters = new HashMap();
								Common.insertProperty(Mahasiswa.class, mahasiswa, parameters, "");
								parameters.put("username", mahasiswa.getNim());

								try {
									parameters.put("password", Common.desEncrypter.get().decrypt(mahasiswa.getPass()));
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/hibernate/AuditListener.java:408");
								}

								JSONObject postData = new JSONObject(parameters);

								System.out.println("linkPost -> " + ConstantValues.ketikaUbahDataPenggunaKirimkeLink);
//								System.out.println("postData -> " + postData);

								String[] command = { "curl", "-k", "-H", "Accept: application/json", "-X", "POST",
										ConstantValues.ketikaUbahDataPenggunaKirimkeLink, "--data",
										postData.toString() };

								ProcessBuilder process = new ProcessBuilder(command);
								Process p;
								p = process.start();
								BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
								StringBuilder builder = new StringBuilder();
								String line = null;
								while ((line = reader.readLine()) != null) {
									builder.append(line);
									builder.append(System.getProperty("line.separator"));
								}
								hasil = builder.toString();

							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/hibernate/AuditListener.java:433");
							}

							System.out.println("hasil -> " + hasil);
						}

					}
				});
			}

			else if (serializable instanceof Siswa) {
				final Siswa siswa = (Siswa) serializable;
				jalankanAudit(new Runnable() {

					@SuppressWarnings({ "rawtypes", "unchecked" })
					@Override
					public void run() {

						if (!ConstantValues.ketikaUbahDataPenggunaKirimkeLink.isEmpty()) {
							String hasil = "";
							try {

								Map parameters = new HashMap();
								Common.insertProperty(Siswa.class, siswa, parameters, "");
								parameters.put("username",
										siswa.getNomorIndukNasional().isEmpty() ? siswa.getNomorInduk()
												: siswa.getNomorIndukNasional());

								try {
									parameters.put("password", Common.desEncrypter.get().decrypt(siswa.getPass()));
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/hibernate/AuditListener.java:464");
								}

								JSONObject postData = new JSONObject(parameters);

								System.out.println("linkPost -> " + ConstantValues.ketikaUbahDataPenggunaKirimkeLink);
//								System.out.println("postData -> " + postData);

								String[] command = { "curl", "-k", "-H", "Accept: application/json", "-X", "POST",
										ConstantValues.ketikaUbahDataPenggunaKirimkeLink, "--data",
										postData.toString() };

								ProcessBuilder process = new ProcessBuilder(command);
								Process p;
								p = process.start();
								BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
								StringBuilder builder = new StringBuilder();
								String line = null;
								while ((line = reader.readLine()) != null) {
									builder.append(line);
									builder.append(System.getProperty("line.separator"));
								}
								hasil = builder.toString();

							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/hibernate/AuditListener.java:489");
							}

							System.out.println("hasil -> " + hasil);
						}

					}
				});
			} else if (serializable instanceof Tbmuser) {
				final Tbmuser tbmuser = (Tbmuser) serializable;
				jalankanAudit(new Runnable() {

					@SuppressWarnings({ "rawtypes", "unchecked" })
					@Override
					public void run() {

						if (!ConstantValues.ketikaUbahDataPenggunaKirimkeLink.isEmpty()) {
							String hasil = "";
							try {

								Map parameters = new HashMap();
								Common.insertProperty(Tbmuser.class, tbmuser, parameters, "");
								parameters.put("username", tbmuser.getUserId());

								try {
									parameters.put("password",
											Common.desEncrypter.get().decrypt(tbmuser.getUserPassword()));
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/hibernate/AuditListener.java:517");
								}

								JSONObject postData = new JSONObject(parameters);

								System.out.println("linkPost -> " + ConstantValues.ketikaUbahDataPenggunaKirimkeLink);
//								System.out.println("postData -> " + postData);

								String[] command = { "curl", "-k", "-H", "Accept: application/json", "-X", "POST",
										ConstantValues.ketikaUbahDataPenggunaKirimkeLink, "--data",
										postData.toString() };

								ProcessBuilder process = new ProcessBuilder(command);
								Process p;
								p = process.start();
								BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
								StringBuilder builder = new StringBuilder();
								String line = null;
								while ((line = reader.readLine()) != null) {
									builder.append(line);
									builder.append(System.getProperty("line.separator"));
								}
								hasil = builder.toString();

							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/hibernate/AuditListener.java:542");
							}

							System.out.println("hasil -> " + hasil);
						}

					}
				});
			}
		}

		// FIX ClassCastException "HashMap cannot be cast to GeneralValueObject": Envers memetakan entity
		// audit (tabel _AUD) sebagai DYNAMIC-MAP (java.util.HashMap), dan flush Envers
		// (AuditProcess.doBeforeTransactionCompletion) ikut memicu listener onPostInsert ini. Blok di
		// bawah hanya relevan untuk entity APLIKASI, jadi saring dengan instanceof (konsisten dgn cabang
		// lain di method ini) agar HashMap milik Envers tidak di-cast paksa -> CCE di SETIAP commit
		// yang menghasilkan revisi audit.
		if (ConstantValues.ketikaUbahSemuaDataKirimke && (serializable instanceof GeneralValueObject)) {
			try {
				final GeneralValueObject generalValueObject = (GeneralValueObject) serializable;
				jalankanAudit(new Runnable() {

					@SuppressWarnings({ "rawtypes", "unchecked" })
					@Override
					public void run() {

						if (!ConstantValues.ketikaUbahSemuaDataKirimkeLink.isEmpty()) {
							String hasil = "";
							try {

								Map parameters = new HashMap();
								Common.insertProperty(generalValueObject.getClass(), generalValueObject, parameters,
										"");

								JSONObject postData = new JSONObject(parameters);

								System.out.println("linkPost -> " + ConstantValues.ketikaUbahSemuaDataKirimkeLink);
//								System.out.println("postData -> " + postData);

								String[] command = { "curl", "-k", "-H", "Accept: application/json", "-X", "POST",
										ConstantValues.ketikaUbahSemuaDataKirimkeLink, "--data", postData.toString() };

								ProcessBuilder process = new ProcessBuilder(command);
								Process p;
								p = process.start();
								BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
								StringBuilder builder = new StringBuilder();
								String line = null;
								while ((line = reader.readLine()) != null) {
									builder.append(line);
									builder.append(System.getProperty("line.separator"));
								}
								hasil = builder.toString();

							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/hibernate/AuditListener.java:591");
							}

							System.out.println("hasil -> " + hasil);
						}

					}
				});
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/hibernate/AuditListener.java:599");
				// TODO: handle exception
			}
		}

		if (serializable instanceof BiodataMahasiswa) {
			BiodataMahasiswa biodataMahasiswa = (BiodataMahasiswa) serializable;
			if (biodataMahasiswa != null && biodataMahasiswa.getMahasiswa() != null) {
				biodataMahasiswa.getMahasiswa().biodataMahasiswa = biodataMahasiswa;
			}
		} else if (serializable instanceof Konfigurasi) {
			Konfigurasi konfigurasi = (Konfigurasi) serializable;
			if (konfigurasi.getNama() != null
					&& konfigurasi.getNama().equalsIgnoreCase("ISTILAH_PENDAFTARAN_CALON_MAHASISWA")) {
				ConstantUtil.PENDAFTARAN_CALON_MAHASISWA = konfigurasi.getNilai();
			} else if (konfigurasi.getNama() != null
					&& konfigurasi.getNama().equalsIgnoreCase("ISTILAH_PENDAFTARAN_MAHASISWA_LAMA")) {
				ConstantUtil.PENDAFTARAN_MAHASISWA_LAMA = konfigurasi.getNilai();
			} else if (konfigurasi.getNama() != null
					&& konfigurasi.getNama().equalsIgnoreCase("ISTILAH_PENDAFTARAN_WISUDA")) {
				ConstantUtil.PENDAFTARAN_WISUDA = konfigurasi.getNilai();
			} else if (konfigurasi.getNama() != null
					&& konfigurasi.getNama().equalsIgnoreCase("PENDAFTARAN_ULANG_MAHASISWA_BARU")) {
				ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU = konfigurasi.getNilai();
			} else if (konfigurasi.getNama() != null
					&& konfigurasi.getNama().equalsIgnoreCase("ISTILAH_PEMBAYARAN_SP")) {
				ConstantUtil.PEMBAYARAN_SP = konfigurasi.getNilai();
			}
		} else if (serializable instanceof Pertemuan) {
			Pertemuan pertemuan = (Pertemuan) serializable;
			if (pertemuan.getTanggal() != null) {
				String sekarang = Common.dateFormat8.get().format(pertemuan.getTanggal());
				Map<Long, Pertemuan> pertemuans = PengumumanAkademisAction.pertemuansHarian.get(sekarang);
				if (pertemuans == null) {
					pertemuans = new HashMap<Long, Pertemuan>();
					pertemuans.put(pertemuan.getId(), pertemuan);
					PengumumanAkademisAction.pertemuansHarian.put(sekarang, pertemuans);
				} else {
					pertemuans.put(pertemuan.getId(), pertemuan);
				}
			}
		}

		prosesUntukElearning(serializable, cla, id);

		if (serializable instanceof Detailperkuliahan) {
			Detailperkuliahan p = (Detailperkuliahan) serializable;
			if (p.getPerkuliahan() != null) {
				String key = "rubahStatusPenilaian_" + p.getPerkuliahan().getId();
				CommonUtil.reset(key);
			}
		}

		if (cla.trim().contains("onPostInsert") || cla.trim().contains("onPostDelete")
				|| cla.trim().contains("onPostUpdate")) {
			if (serializable instanceof LampiranLain) {
				LampiranLain p = (LampiranLain) serializable;

				if (Common.folder != null && Common.folder.exists()) {

					try {

						String[] ex = StringUtils.split(p.getNama(), ".");
						String extension = ex[ex.length - 1];
						File fileBaru = new File(Common.folder.getAbsolutePath() + "/" + p.getJenis() + "_" + p.getRef()
								+ "." + extension);
						if (fileBaru.exists()) {
							// System.out.println("hapus -> " + fileBaru);
							fileBaru.delete();
						}

						File file = null;
						if (p.getId() != null && p.getNama() != null && p.getNama().trim().length() > 0
								&& !FileFoto.isBlobExtractionRunning()) {
							/*
							 * AuditListener berjalan di tengah proses flush/commit Hibernate. Jangan
							 * memanggil ambilFile() dari sini karena ambilFile() dapat membuka session
							 * baru, membaca Blob, lalu memicu audit lagi. Cukup pakai file yang sudah
							 * ada di disk/cache. Jika belum ada, file akan dibuat saat diakses normal.
							 */
							file = p.ambilFileDariCacheAtauDisk();
						}
						if (file != null && file.exists() && file.length() > 0L) {
							if (fileBaru != null
									&& !file.getAbsolutePath().equalsIgnoreCase(fileBaru.getAbsolutePath())) {
								// System.out.println("simpan ke -> " + fileBaru);
								FileUtils.copyFile(file, fileBaru);

							}
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/hibernate/AuditListener.java:690");
					}

				}

				LampiranLain.resetLokasi(false, p.getRef(), p.getJenis());
				LampiranLain.resetLokasi(true, p.getRef(), p.getJenis());

			} else if (serializable instanceof RolePrivilage) {
				RolePrivilage rolePrivilage = (RolePrivilage) serializable;
				Map<String, List<RolePrivilage>> rolePrivilagesUtamaBaru = new HashMap<String, List<RolePrivilage>>();
				for (String key : CommonPrivilages.rolePrivilagesUtama.keySet()) {
					if (!key.startsWith(rolePrivilage.getRole().getRoleId())) {
						List<RolePrivilage> h = CommonPrivilages.rolePrivilagesUtama.get(key);
						rolePrivilagesUtamaBaru.put(key, h);
//						System.out.println("Hapus key " + key + ", yg ditambah lagi " + h);
					}
				}
				CommonPrivilages.rolePrivilagesUtama.clear();
				CommonPrivilages.rolePrivilagesUtama.putAll(rolePrivilagesUtamaBaru);
			}

		}

		if (serializable instanceof Tagihan
				&& (cla.trim().contains("onPostInsert") || cla.trim().contains("onPostUpdate"))) {
			try {
				Tagihan tagihan = (Tagihan) serializable;
				String kodeUnik = Tagihan.genCode(tagihan.getItemBiayaSekolah(), tagihan.getPengaturanBiaya(),
						tagihan.getTahunbulan(), tagihan.getSiswa(), tagihan.getCalonSiswa(), tagihan.getBayarKe());
				MemoryDbUtil.getAllTagihan().put(kodeUnik, tagihan);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/hibernate/AuditListener.java:722");
			}
		}

		else if (serializable instanceof Kegiatan
				&& (cla.trim().contains("onPostInsert") || cla.trim().contains("onPostUpdate"))) {
			Kegiatan kegiatan = (Kegiatan) serializable;
			try {

				if (kegiatan != null && kegiatan.getId() != null && kegiatan.getJenisKegiatan() != null
						&& kegiatan.getCalonMahasiswa() != null && kegiatan.getCalonMahasiswa().getMahasiswa() == null
						&& kegiatan.getDibayar() > 100000) {
					JenisKegiatan jenisKegiatan = kegiatan.getJenisKegiatan();
					if (ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
							&& jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId())) {
						final Kegiatan kegiatanFinal = kegiatan;
						AUDIT_SCHEDULER.schedule(new Runnable() {
							@Override
							public void run() {
								try {
									CommonReportHelper.checkGenNim(kegiatanFinal);
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/hibernate/AuditListener.java:744");
								}
							}
						}, 5, java.util.concurrent.TimeUnit.SECONDS);

					}
				}

				if (kegiatan != null && kegiatan.getId() != null && kegiatan.getJenisKegiatan() != null
						&& kegiatan.getMahasiswa() != null
						&& kegiatan.getJenisKegiatan().getDigunakanSyaratKeaktifan()) {
					boolean terlambarLangsungTidakAktif = Common
							.getKonfigurasi("mhs_all_lambat_bayar_langsung_tidak_aktif", "", kegiatan.getSemster(),
									kegiatan.getMahasiswa().getTahunangkatan(), kegiatan.getMahasiswa().getJurusan(),
									kegiatan.getMahasiswa().getProgram(),
									kegiatan.getMahasiswa().getStatusAwalMahasiswa())
							.getNilai().equals(Konfigurasi.AKTIF);
					if (terlambarLangsungTidakAktif) {
						double harusLunas = 0.1;
						boolean checkStatusPembayaranMahasiswa = (kegiatan != null
								&& kegiatan.getPersentaseLunas() >= harusLunas);

						if (kegiatan.getMahasiswa() != null) {
							KegiatanHelper.updateBatasStudiMahasiswa(kegiatan.getMahasiswa(), null,
									kegiatan.getSemster(), checkStatusPembayaranMahasiswa, false);
						}

						HistoryStatusMahasiswa historyStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil
								.currentStatus(kegiatan.getMahasiswa(), kegiatan.getTahunAkademik(),
										kegiatan.getSemster());
						historyStatusMahasiswa.put(checkStatusPembayaranMahasiswa + "",
								"checkStatusPembayaranMahasiswa");
						historyStatusMahasiswa.setStatusMahasiswa(
								checkStatusPembayaranMahasiswa ? ConstantValues.AKTIF : ConstantValues.TIDAK_AKTIF);
						System.out.println("mahasiswa " + kegiatan.getMahasiswa() + ", checkStatusPembayaranMahasiswa "
								+ checkStatusPembayaranMahasiswa);
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/hibernate/AuditListener.java:783");
			}
		} else if (serializable instanceof Kegiatan && cla.trim().contains("onPostDelete")) {
			Kegiatan kegiatan = (Kegiatan) serializable;
			try {
				if (kegiatan != null && kegiatan.getId() != null && kegiatan.getJenisKegiatan() != null
						&& kegiatan.getMahasiswa() != null
						&& kegiatan.getJenisKegiatan().getDigunakanSyaratKeaktifan()) {
					boolean terlambarLangsungTidakAktif = Common
							.getKonfigurasi("mhs_all_lambat_bayar_langsung_tidak_aktif", "", kegiatan.getSemster(),
									kegiatan.getMahasiswa().getTahunangkatan(), kegiatan.getMahasiswa().getJurusan(),
									kegiatan.getMahasiswa().getProgram(),
									kegiatan.getMahasiswa().getStatusAwalMahasiswa())
							.getNilai().equals(Konfigurasi.AKTIF);
					if (terlambarLangsungTidakAktif) {
						boolean checkStatusPembayaranMahasiswa = false;
						if (kegiatan.getMahasiswa() != null) {
							KegiatanHelper.updateBatasStudiMahasiswa(kegiatan.getMahasiswa(), null,
									kegiatan.getSemster(), checkStatusPembayaranMahasiswa, false);
						}

						HistoryStatusMahasiswa historyStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil
								.currentStatus(kegiatan.getMahasiswa(), kegiatan.getTahunAkademik(),
										kegiatan.getSemster());
						historyStatusMahasiswa.put(checkStatusPembayaranMahasiswa + "",
								"checkStatusPembayaranMahasiswa");
						historyStatusMahasiswa.setStatusMahasiswa(ConstantValues.TIDAK_AKTIF);
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/hibernate/AuditListener.java:813");
			}
		}

		if (cla.trim().contains("onPostInsert") || cla.trim().contains("onPostDelete")) {
			if (serializable instanceof Detailperkuliahan) {
				Detailperkuliahan p = (Detailperkuliahan) serializable;
				if (p.getPerkuliahan() != null) {

					String key = "ambilJumlahDetailperkuliahan_" + p.getPerkuliahan().getId().toString();
					CommonUtil.reset(key);
					key = "ambilJumlahDetailperkuliahan_" + p.getPerkuliahan().getId().toString() + "_"
							+ p.getMahasiswa().getId().toString();
					CommonUtil.reset(key);
					try {
						key = "ambilMkYngTelahDiambil_" + p.getMahasiswa().getId().toString() + "_" + p.getSemester()
								+ "_" + p.getPerkuliahan().getStatusSemesterPendek() + "_"
								+ URLEncoder.encode(p.getPerkuliahan().getTahunAjaran(), "UTF-8");
						CommonUtil.reset(key);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/hibernate/AuditListener.java:833");
					}

					try {
						key = "hitungSksYangTelahDiambil_" + p.getMahasiswa().getId() + "_" + p.getTahap() + "_"
								+ p.getSemester() + "_" + p.getPerkuliahan().getStatusSemesterPendek();
						CommonUtil.reset(key);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/hibernate/AuditListener.java:841");
					}
					try {
						key = "hitungSksYangTelahDiambil_" + p.getMahasiswa().getId() + "_0_" + p.getSemester() + "_"
								+ p.getPerkuliahan().getStatusSemesterPendek();
						CommonUtil.reset(key);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/hibernate/AuditListener.java:848");
					}

				}
			}
		}

		if (cla.trim().contains("onPostDelete")) {
			if (serializable instanceof DiskusiPengumumanAkademis) {
				DiskusiPengumumanAkademis p = (DiskusiPengumumanAkademis) serializable;
				if (p.getPengumumanAkademis() != null) {
					p.getPengumumanAkademis().removeDiskusiPengumumanAkademis(id);
				}
			} else if (serializable instanceof KegiatanKemahasiswaanPunyaMahasiswa) {
				KegiatanKemahasiswaanPunyaMahasiswa p = (KegiatanKemahasiswaanPunyaMahasiswa) serializable;
				if (p.getMahasiswa() != null) {
					p.getMahasiswa().removeKegiatanKemahasiswaanPunyaMahasiswa(id);
				}
			} else if (serializable instanceof OrganisasiIntraKampusPunyaMahasiswa) {
				OrganisasiIntraKampusPunyaMahasiswa p = (OrganisasiIntraKampusPunyaMahasiswa) serializable;
				if (p.getMahasiswa() != null) {
					p.getMahasiswa().removeOrganisasiIntraKampusPunyaMahasiswa(id);
				}
			} else if (serializable instanceof PrestasiMahasiswa) {
				PrestasiMahasiswa p = (PrestasiMahasiswa) serializable;
				if (p.getMahasiswa() != null) {
					p.getMahasiswa().removePrestasiMahasiswa(id);
				}
			}

			else if (serializable instanceof PenghargaanMahasiswa) {
				PenghargaanMahasiswa p = (PenghargaanMahasiswa) serializable;
				if (p.getMahasiswa() != null) {
					p.getMahasiswa().removePenghargaanMahasiswa(id);
				}
			}

			else if (serializable instanceof KegiatanKedosenanPunyaDosen) {
				KegiatanKedosenanPunyaDosen p = (KegiatanKedosenanPunyaDosen) serializable;
				if (p.getDosen() != null) {
					p.getDosen().removeKegiatanKedosenanPunyaDosen(id);
				}
			} else if (serializable instanceof OrganisasiDosenPunyaDosen) {
				OrganisasiDosenPunyaDosen p = (OrganisasiDosenPunyaDosen) serializable;
				if (p.getDosen() != null) {
					p.getDosen().removeOrganisasiDosenPunyaDosen(id);
				}
			} else if (serializable instanceof PrestasiDosen) {
				PrestasiDosen p = (PrestasiDosen) serializable;
				if (p.getDosen() != null) {
					p.getDosen().removePrestasiDosen(id);
				}
			}

			else if (serializable instanceof PenghargaanDosen) {
				PenghargaanDosen p = (PenghargaanDosen) serializable;
				if (p.getDosen() != null) {
					p.getDosen().removePenghargaanDosen(id);
				}
			}

			else if (serializable instanceof PengajuanPenelitianDanPengabdian) {
				PengajuanPenelitianDanPengabdian p = (PengajuanPenelitianDanPengabdian) serializable;
				if (p.getTbmuser() != null && p.getTbmuser().getDosen() != null) {
					p.getTbmuser().getDosen().removePengajuanPenelitianDanPengabdian(id);
				}
			}

			else if (serializable instanceof Artikel) {
				Artikel p = (Artikel) serializable;
				if (p.getTbmuser() != null && p.getTbmuser().getDosen() != null) {
					p.getTbmuser().getDosen().removeArtikel(id);
				}
			}

			else if (serializable instanceof BukuBahanAjar) {
				BukuBahanAjar p = (BukuBahanAjar) serializable;
				if (p.getDosenPengarang1() != null) {
					p.getDosenPengarang1().removeBukuBahanAjar(id);
				}
				if (p.getDosenPengarang2() != null) {
					p.getDosenPengarang2().removeBukuBahanAjar(id);
				}
				if (p.getDosenPengarang3() != null) {
					p.getDosenPengarang3().removeBukuBahanAjar(id);
				}
				if (p.getDosenPengarang4() != null) {
					p.getDosenPengarang4().removeBukuBahanAjar(id);
				}
				if (p.getDosenPengarang5() != null) {
					p.getDosenPengarang5().removeBukuBahanAjar(id);
				}
			}

			else if (serializable instanceof IsiAngketParameterUmum) {
				IsiAngketParameterUmum p = (IsiAngketParameterUmum) serializable;
				p.delete();
			} else if (serializable instanceof MahasiswaJadiAsisten) {
				MahasiswaJadiAsisten p = (MahasiswaJadiAsisten) serializable;
				if (p.getPerkuliahan() != null) {
					p.getPerkuliahan().removeMahasiswaJadiAsisten(id);
				}
			} else if (serializable instanceof PengajuanIzinTidakMasukPerkuliahan) {
				PengajuanIzinTidakMasukPerkuliahan p = (PengajuanIzinTidakMasukPerkuliahan) serializable;
				if (p.getPertemuan() != null) {
					p.getPertemuan().removePengajuanIzinTidakMasukPerkuliahan(id);
				}
			} else if (serializable instanceof Tbmuser) {
				Tbmuser p = (Tbmuser) serializable;
				p.delete();
			} else if (serializable instanceof Tbmrole) {
				Tbmrole p = (Tbmrole) serializable;
				p.delete();
			} else if (serializable instanceof ChecklistHasilPenilaianUmum) {
				ChecklistHasilPenilaianUmum p = (ChecklistHasilPenilaianUmum) serializable;
				p.delete();
			} else if (serializable instanceof ChecklistBaruPenilaianDosenOlehMahasiswa) {
				ChecklistBaruPenilaianDosenOlehMahasiswa p = (ChecklistBaruPenilaianDosenOlehMahasiswa) serializable;
				p.delete();
			} else if (serializable instanceof Perkuliahan) {
				Perkuliahan p = (Perkuliahan) serializable;
				p.hapusPerkuliahanDosen();
				p.belum("paralel");
				if (p.getPerkuliahan_paralel() != null) {
					p.getPerkuliahan_paralel().belum("paralel");
				}
				p.delete();
			} else if (serializable instanceof Detailperkuliahan) {
				Detailperkuliahan p = (Detailperkuliahan) serializable;
				if (p.getPerkuliahan() != null) {
					p.getPerkuliahan().belum("detailperkulaiahan");
				}
				p.delete();
				String key = KrsMahasiswa.class.getSimpleName() + "_" + p.getMahasiswa().getId() + "-" + p.getSemester()
						+ "-" + p.getTahap() + "-"
						+ (p.getPerkuliahan() == null ? null : p.getPerkuliahan().getStatusSemesterPendek());
				CommonUtil.reset(key);
			} else if (serializable instanceof PengaturanPembayaranBulanan) {
				PengaturanPembayaranBulanan p = (PengaturanPembayaranBulanan) serializable;
				p.delete();
			} else if (serializable instanceof PengaturanPembayaranBulanan) {
				DetailBiaya p = (DetailBiaya) serializable;
				p.delete();
			} else if (serializable instanceof Kegiatan) {
				Kegiatan p = (Kegiatan) serializable;
				p.delete();
			} else if (serializable instanceof CicilanPembayaran) {
				CicilanPembayaran p = (CicilanPembayaran) serializable;
				p.delete();

				CicilanPembayaran.populateHapusPembayaran(p, p.getKegiatan());

			} else if (serializable instanceof KrsMahasiswa) {
				KrsMahasiswa p = (KrsMahasiswa) serializable;
				p.delete();
			} else if (serializable instanceof HistoryStatusMahasiswa) {
				HistoryStatusMahasiswa p = (HistoryStatusMahasiswa) serializable;
				p.delete();
			} else if (serializable instanceof Matakuliah) {
				Matakuliah p = (Matakuliah) serializable;
				p.delete();
			} else if (serializable instanceof DetailKegiatan) {
				DetailKegiatan p = (DetailKegiatan) serializable;
				p.delete();

				DetailKegiatan.populateHapusPembayaran(p, p.getKegiatan());

			} else if (serializable instanceof Pertemuan) {
				Pertemuan p = (Pertemuan) serializable;
				p.hapusPertemuan();
				p.delete();
			} else if (serializable instanceof PertemuanPunyaDiskusi) {
				PertemuanPunyaDiskusi p = (PertemuanPunyaDiskusi) serializable;
				p.delete();
			} else if (serializable instanceof TugasPertemuan) {
				TugasPertemuan p = (TugasPertemuan) serializable;
				p.delete();
			} else if (serializable instanceof PertemuanPunyaUjian) {
				PertemuanPunyaUjian p = (PertemuanPunyaUjian) serializable;
				p.delete();
			} else if (serializable instanceof PertemuanFileContent) {
				PertemuanFileContent p = (PertemuanFileContent) serializable;
				p.delete();
			} else if (serializable instanceof TugasFileContent) {
				TugasFileContent p = (TugasFileContent) serializable;
				p.delete();
			} else if (serializable instanceof VideoPertemuan) {
				VideoPertemuan p = (VideoPertemuan) serializable;
				p.delete();
			} else if (serializable instanceof AudioPertemuan) {
				AudioPertemuan p = (AudioPertemuan) serializable;
				p.delete();
			} else if (serializable instanceof BankSoalDetail) {
				BankSoalDetail p = (BankSoalDetail) serializable;
				p.delete();
			} else if (serializable instanceof FormatNilai) {
				FormatNilai p = (FormatNilai) serializable;
				p.delete();
			} else if (serializable instanceof HasilUjianMahasiswa) {
				HasilUjianMahasiswa p = (HasilUjianMahasiswa) serializable;
				p.delete();
			} else if (serializable instanceof UjianPunyaSoal) {
				UjianPunyaSoal p = (UjianPunyaSoal) serializable;
				p.delete();
			} else if (serializable instanceof HasilUjianMahasiswaDetail) {
				HasilUjianMahasiswaDetail p = (HasilUjianMahasiswaDetail) serializable;
				p.delete();
			} else if (serializable instanceof MatakuliahEkivalen) {
				MatakuliahEkivalen p = (MatakuliahEkivalen) serializable;
				p.delete();
			} else if (serializable instanceof GeneralValueObject) {
				GeneralValueObject p = (GeneralValueObject) serializable;
				p.delete();
			}

		} else if (cla.trim().contains("onPostUpdate")) {

			if (serializable instanceof DiskusiPengumumanAkademis) {
				DiskusiPengumumanAkademis p = (DiskusiPengumumanAkademis) serializable;
				if (p.getPengumumanAkademis() != null) {
					p.getPengumumanAkademis().populateDiskusiPengumumanAkademis(p.getId(), true);
				}
			} else if (serializable instanceof KegiatanKemahasiswaanPunyaMahasiswa) {
				KegiatanKemahasiswaanPunyaMahasiswa p = (KegiatanKemahasiswaanPunyaMahasiswa) serializable;
				if (p.getMahasiswa() != null) {
					p.getMahasiswa().populateKegiatanKemahasiswaanPunyaMahasiswa(p);
				}
			} else if (serializable instanceof OrganisasiIntraKampusPunyaMahasiswa) {
				OrganisasiIntraKampusPunyaMahasiswa p = (OrganisasiIntraKampusPunyaMahasiswa) serializable;
				if (p.getMahasiswa() != null) {
					p.getMahasiswa().populateOrganisasiIntraKampusPunyaMahasiswa(p);
				}
			}

			else if (serializable instanceof PrestasiMahasiswa) {
				PrestasiMahasiswa p = (PrestasiMahasiswa) serializable;
				if (p.getMahasiswa() != null) {
					p.getMahasiswa().populatePrestasiMahasiswa(p);
				}
			}

			else if (serializable instanceof PenghargaanMahasiswa) {
				PenghargaanMahasiswa p = (PenghargaanMahasiswa) serializable;
				if (p.getMahasiswa() != null) {
					p.getMahasiswa().populatePenghargaanMahasiswa(p);
				}
			}

			else if (serializable instanceof KegiatanKedosenanPunyaDosen) {
				KegiatanKedosenanPunyaDosen p = (KegiatanKedosenanPunyaDosen) serializable;
				if (p.getDosen() != null) {
					p.getDosen().populateKegiatanKedosenanPunyaDosen(p);
				}
			} else if (serializable instanceof OrganisasiDosenPunyaDosen) {
				OrganisasiDosenPunyaDosen p = (OrganisasiDosenPunyaDosen) serializable;
				if (p.getDosen() != null) {
					p.getDosen().populateOrganisasiDosenPunyaDosen(p);
				}
			}

			else if (serializable instanceof PrestasiDosen) {
				PrestasiDosen p = (PrestasiDosen) serializable;
				if (p.getDosen() != null) {
					p.getDosen().populatePrestasiDosen(p);
				}
			}

			else if (serializable instanceof PenghargaanDosen) {
				PenghargaanDosen p = (PenghargaanDosen) serializable;
				if (p.getDosen() != null) {
					p.getDosen().populatePenghargaanDosen(p);
				}
			}

			else if (serializable instanceof PengajuanPenelitianDanPengabdian) {
				PengajuanPenelitianDanPengabdian p = (PengajuanPenelitianDanPengabdian) serializable;
				if (p.getTbmuser() != null && p.getTbmuser().getDosen() != null) {
					p.getTbmuser().getDosen().populatePengajuanPenelitianDanPengabdian(p);
				}
			}

			else if (serializable instanceof Artikel) {
				Artikel p = (Artikel) serializable;
				if (p.getTbmuser() != null && p.getTbmuser().getDosen() != null) {
					p.getTbmuser().getDosen().populateArtikel(p);
				}
			}

			else if (serializable instanceof BukuBahanAjar) {
				BukuBahanAjar p = (BukuBahanAjar) serializable;
				if (p.getDosenPengarang1() != null) {
					p.getDosenPengarang1().populateBukuBahanAjar(p);
				}
				if (p.getDosenPengarang2() != null) {
					p.getDosenPengarang2().populateBukuBahanAjar(p);
				}
				if (p.getDosenPengarang3() != null) {
					p.getDosenPengarang3().populateBukuBahanAjar(p);
				}
				if (p.getDosenPengarang4() != null) {
					p.getDosenPengarang4().populateBukuBahanAjar(p);
				}
				if (p.getDosenPengarang5() != null) {
					p.getDosenPengarang5().populateBukuBahanAjar(p);
				}
			}

			else if (serializable instanceof IsiAngketParameterUmum) {
				IsiAngketParameterUmum p = (IsiAngketParameterUmum) serializable;
				p.write();
			} else if (serializable instanceof MahasiswaJadiAsisten) {
				MahasiswaJadiAsisten p = (MahasiswaJadiAsisten) serializable;
				if (p.getPerkuliahan() != null) {
					p.getPerkuliahan().populateMahasiswaJadiAsisten(p.getId());
				}
			} else if (serializable instanceof PengajuanIzinTidakMasukPerkuliahan) {
				PengajuanIzinTidakMasukPerkuliahan p = (PengajuanIzinTidakMasukPerkuliahan) serializable;
				if (p.getPertemuan() != null) {
					p.getPertemuan().populatePengajuanIzinTidakMasukPerkuliahan(p.getId());
				}
			} else if (serializable instanceof Tbmuser) {
				Tbmuser p = (Tbmuser) serializable;
				p.write();
			} else if (serializable instanceof Tbmrole) {
				Tbmrole p = (Tbmrole) serializable;
				p.write();
			} else if (serializable instanceof ChecklistHasilPenilaianUmum) {
				ChecklistHasilPenilaianUmum p = (ChecklistHasilPenilaianUmum) serializable;
				p.write();
			} else if (serializable instanceof ChecklistBaruPenilaianDosenOlehMahasiswa) {
				ChecklistBaruPenilaianDosenOlehMahasiswa p = (ChecklistBaruPenilaianDosenOlehMahasiswa) serializable;
				p.write();
			} else if (serializable instanceof Perkuliahan) {
				Perkuliahan p = (Perkuliahan) serializable;
				p.belum("paralel");
				if (p.getPerkuliahan_paralel() != null) {
					p.getPerkuliahan_paralel().belum("paralel");
				}
				p.hapusPerkuliahanDosen();
				p.reInitPerkuliahanDosen();
				p.write();
			}

			else if (serializable instanceof Detailperkuliahan) {
				Detailperkuliahan p = (Detailperkuliahan) serializable;
				if (p.getPerkuliahan() != null) {
					p.getPerkuliahan().belum("detailperkulaiahan");
				}
				p.write();
			}

			else if (serializable instanceof PengaturanPembayaranBulanan) {
				PengaturanPembayaranBulanan p = (PengaturanPembayaranBulanan) serializable;
				p.write();
			} else if (serializable instanceof DetailBiaya) {
				DetailBiaya p = (DetailBiaya) serializable;
				p.write();
			} else if (serializable instanceof Kegiatan) {
				Kegiatan p = (Kegiatan) serializable;
				p.write();

				if (p.getCalonMahasiswa() != null && p.getJenisKegiatan() != null
						&& ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null && p.getJenisKegiatan().getId()
								.equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId())) {
					BiodataCalonMahasiswa biodataCalonMahasiswa = p.getCalonMahasiswa();
					biodataCalonMahasiswa.setPembayaranDaftarUlang(p);
				}
				if (p.getCalonMahasiswa() != null && p.getJenisKegiatan() != null
						&& ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
						&& p.getJenisKegiatan().getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId())) {
					BiodataCalonMahasiswa biodataCalonMahasiswa = p.getCalonMahasiswa();
					biodataCalonMahasiswa.setPembayaranRegistrasi(p);
				}

			} else if (serializable instanceof CicilanPembayaran) {
				CicilanPembayaran p = (CicilanPembayaran) serializable;

				if (p.getKegiatan() != null) {
					p.getKegiatan().appendCicilan(p);
				}
				CicilanPembayaran.populatePembayaran(p, p.getKegiatan());

			}

			else if (serializable instanceof HistoryStatusMahasiswa) {
				HistoryStatusMahasiswa p = (HistoryStatusMahasiswa) serializable;
				p.write();
			} else if (serializable instanceof KrsMahasiswa) {
				KrsMahasiswa p = (KrsMahasiswa) serializable;
				p.write();
			} else if (serializable instanceof Matakuliah) {
				Matakuliah p = (Matakuliah) serializable;
				p.write();
			} else if (serializable instanceof DetailKegiatan) {
				DetailKegiatan p = (DetailKegiatan) serializable;
				p.write();

			} else if (serializable instanceof Pertemuan) {
				Pertemuan p = (Pertemuan) serializable;
				p.hapusPertemuan();
				p.tambahPertemuan();
				p.write();
			} else if (serializable instanceof PertemuanPunyaDiskusi) {
				PertemuanPunyaDiskusi p = (PertemuanPunyaDiskusi) serializable;
				p.write();
			} else if (serializable instanceof TugasPertemuan) {
				TugasPertemuan p = (TugasPertemuan) serializable;
				p.write();
			} else if (serializable instanceof PertemuanPunyaUjian) {
				PertemuanPunyaUjian p = (PertemuanPunyaUjian) serializable;
				p.write();
			} else if (serializable instanceof PertemuanFileContent) {
				PertemuanFileContent p = (PertemuanFileContent) serializable;
				p.write();
			} else if (serializable instanceof TugasFileContent) {
				TugasFileContent p = (TugasFileContent) serializable;
				p.write();
			} else if (serializable instanceof VideoPertemuan) {
				VideoPertemuan p = (VideoPertemuan) serializable;
				p.write();
			} else if (serializable instanceof AudioPertemuan) {
				AudioPertemuan p = (AudioPertemuan) serializable;
				p.write();
			} else if (serializable instanceof BankSoalDetail) {
				BankSoalDetail p = (BankSoalDetail) serializable;
				p.write();
			} else if (serializable instanceof FormatNilai) {
				FormatNilai p = (FormatNilai) serializable;
				p.write();
			} else if (serializable instanceof HasilUjianMahasiswa) {
				HasilUjianMahasiswa p = (HasilUjianMahasiswa) serializable;
				p.write();
			} else if (serializable instanceof UjianPunyaSoal) {
				UjianPunyaSoal p = (UjianPunyaSoal) serializable;
				p.write();
			} else if (serializable instanceof HasilUjianMahasiswaDetail) {
				HasilUjianMahasiswaDetail p = (HasilUjianMahasiswaDetail) serializable;
				p.write();
			} else if (serializable instanceof MatakuliahEkivalen) {
				MatakuliahEkivalen p = (MatakuliahEkivalen) serializable;
				p.write();
			} else if (serializable instanceof Mahasiswa) {
				Mahasiswa p = (Mahasiswa) serializable;
				if (p != null && p.getId() != null && p.getJurusan() != null && p.getJurusan().getFakultas() != null
						&& p.getJurusan().getFakultas().getPerguruanTinggi() != null) {
					p.getJurusan().getFakultas().getPerguruanTinggi().put(p.getId().toString(),
							p.getNim() + "_id_mahasiswa");
				}
			} else if (serializable instanceof Siswa) {
				Siswa p = (Siswa) serializable;
				if (p != null && p.getId() != null && FilterLoginAis.perguruanTinggi != null) {
					FilterLoginAis.perguruanTinggi.put(p.getId().toString(), p.getNim() + "_id_siswa");
				}
			} else if (serializable instanceof Penduduk) {
				Penduduk p = (Penduduk) serializable;
				if (p != null && p.getId() != null && FilterLoginAis.perguruanTinggi != null) {
					FilterLoginAis.perguruanTinggi.put(p.getId().toString(), p.getKode() + "_id_penduduk");
				}
			}
		} else if (cla.trim().contains("onPostInsert")) {
			if (serializable instanceof DiskusiPengumumanAkademis) {
				DiskusiPengumumanAkademis p = (DiskusiPengumumanAkademis) serializable;
				if (p.getPengumumanAkademis() != null) {
					p.getPengumumanAkademis().populateDiskusiPengumumanAkademis(p.getId(), true);
				}
			} else if (serializable instanceof KegiatanKemahasiswaanPunyaMahasiswa) {
				KegiatanKemahasiswaanPunyaMahasiswa p = (KegiatanKemahasiswaanPunyaMahasiswa) serializable;
				if (p.getMahasiswa() != null) {
					p.getMahasiswa().populateKegiatanKemahasiswaanPunyaMahasiswa(p);
				}
			} else if (serializable instanceof OrganisasiIntraKampusPunyaMahasiswa) {
				OrganisasiIntraKampusPunyaMahasiswa p = (OrganisasiIntraKampusPunyaMahasiswa) serializable;
				if (p.getMahasiswa() != null) {
					p.getMahasiswa().populateOrganisasiIntraKampusPunyaMahasiswa(p);
				}
			} else if (serializable instanceof PrestasiMahasiswa) {
				PrestasiMahasiswa p = (PrestasiMahasiswa) serializable;
				if (p.getMahasiswa() != null) {
					p.getMahasiswa().populatePrestasiMahasiswa(p);
				}
			}

			else if (serializable instanceof PenghargaanMahasiswa) {
				PenghargaanMahasiswa p = (PenghargaanMahasiswa) serializable;
				if (p.getMahasiswa() != null) {
					p.getMahasiswa().populatePenghargaanMahasiswa(p);
				}
			}

			else if (serializable instanceof KegiatanKedosenanPunyaDosen) {
				KegiatanKedosenanPunyaDosen p = (KegiatanKedosenanPunyaDosen) serializable;
				if (p.getDosen() != null) {
					p.getDosen().populateKegiatanKedosenanPunyaDosen(p);
				}
			} else if (serializable instanceof OrganisasiDosenPunyaDosen) {
				OrganisasiDosenPunyaDosen p = (OrganisasiDosenPunyaDosen) serializable;
				if (p.getDosen() != null) {
					p.getDosen().populateOrganisasiDosenPunyaDosen(p);
				}
			}

			else if (serializable instanceof PrestasiDosen) {
				PrestasiDosen p = (PrestasiDosen) serializable;
				if (p.getDosen() != null) {
					p.getDosen().populatePrestasiDosen(p);
				}
			}

			else if (serializable instanceof PenghargaanDosen) {
				PenghargaanDosen p = (PenghargaanDosen) serializable;
				if (p.getDosen() != null) {
					p.getDosen().populatePenghargaanDosen(p);
				}
			}

			else if (serializable instanceof PengajuanPenelitianDanPengabdian) {
				PengajuanPenelitianDanPengabdian p = (PengajuanPenelitianDanPengabdian) serializable;
				if (p.getTbmuser() != null && p.getTbmuser().getDosen() != null) {
					p.getTbmuser().getDosen().populatePengajuanPenelitianDanPengabdian(p);
				}
			}

			else if (serializable instanceof Artikel) {
				Artikel p = (Artikel) serializable;
				if (p.getTbmuser() != null && p.getTbmuser().getDosen() != null) {
					p.getTbmuser().getDosen().populateArtikel(p);
				}
			}

			else if (serializable instanceof BukuBahanAjar) {
				BukuBahanAjar p = (BukuBahanAjar) serializable;
				if (p.getDosenPengarang1() != null) {
					p.getDosenPengarang1().populateBukuBahanAjar(p);
				}
				if (p.getDosenPengarang2() != null) {
					p.getDosenPengarang2().populateBukuBahanAjar(p);
				}
				if (p.getDosenPengarang3() != null) {
					p.getDosenPengarang3().populateBukuBahanAjar(p);
				}
				if (p.getDosenPengarang4() != null) {
					p.getDosenPengarang4().populateBukuBahanAjar(p);
				}
				if (p.getDosenPengarang5() != null) {
					p.getDosenPengarang5().populateBukuBahanAjar(p);
				}
			}

			else if (serializable instanceof Perkuliahan) {
				Perkuliahan p = (Perkuliahan) serializable;
				p.belum("paralel");
				if (p.getPerkuliahan_paralel() != null) {
					p.getPerkuliahan_paralel().belum("paralel");
				}
				p.reInitPerkuliahanDosen();
			} else if (serializable instanceof MahasiswaJadiAsisten) {
				MahasiswaJadiAsisten p = (MahasiswaJadiAsisten) serializable;
				if (p.getPerkuliahan() != null) {
					p.getPerkuliahan().populateMahasiswaJadiAsisten(p.getId());
				}
			} else if (serializable instanceof PengajuanIzinTidakMasukPerkuliahan) {
				PengajuanIzinTidakMasukPerkuliahan p = (PengajuanIzinTidakMasukPerkuliahan) serializable;
				if (p.getPertemuan() != null) {
					p.getPertemuan().populatePengajuanIzinTidakMasukPerkuliahan(p.getId());
				}
			} else if (serializable instanceof Tbmuser) {
				Tbmuser p = (Tbmuser) serializable;
				p.write();
			} else if (serializable instanceof Tbmrole) {
				Tbmrole p = (Tbmrole) serializable;
				p.write();
			} else if (serializable instanceof KrsMahasiswa) {
				KrsMahasiswa p = (KrsMahasiswa) serializable;
				p.write();
			} else if (serializable instanceof Pertemuan) {
				Pertemuan p = (Pertemuan) serializable;
				p.tambahPertemuan();
			} else if (serializable instanceof Mahasiswa) {
				Mahasiswa p = (Mahasiswa) serializable;
				if (p != null && p.getId() != null && p.getJurusan() != null && p.getJurusan().getFakultas() != null
						&& p.getJurusan().getFakultas().getPerguruanTinggi() != null) {
					p.getJurusan().getFakultas().getPerguruanTinggi().put(p.getId().toString(),
							p.getNim() + "_id_mahasiswa");
				}
			}
		}

		if (serializable instanceof GeneralValueObject) {
			GeneralValueObject generalValueObject = (GeneralValueObject) serializable;
			if (ConstantValues.classExist(serializable.getClass())) {
				if (cla.trim().contains("onPostInsert")) {
					InitDataHelper.reInitDataBaru(generalValueObject);

					try {
						String clazz = serializable.getClass().getName();
						clazz = StringUtils.split(clazz, "_")[0];
						// System.out.println("tambah clazz -> " + clazz + ", id " + id);
						MemoryCacheUtil.broadcastObjectSync("PUT", clazz, String.valueOf(id));
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/hibernate/AuditListener.java:1447");
					}

				} else if (cla.trim().contains("onPostUpdate")) {
					InitDataHelper.reInitDataUpdate(generalValueObject);

					try {
						String clazz = serializable.getClass().getName();
						clazz = StringUtils.split(clazz, "_")[0];
						// System.out.println("ubah clazz -> " + clazz + ", id " + id);
						MemoryCacheUtil.broadcastObjectSync("PUT", clazz, String.valueOf(id));
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/hibernate/AuditListener.java:1459");
					}

				} else if (cla.trim().contains("onPostDelete")) {
					ConstantValues.hapus(serializable.getClass().getName(), id);

					try {
						String clazz = serializable.getClass().getName();
						clazz = StringUtils.split(clazz, "_")[0];
						// System.out.println("hapus clazz -> " + clazz + ", id " + id);
						MemoryCacheUtil.broadcastObjectSync("PUT", clazz, String.valueOf(id));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/hibernate/AuditListener.java:1470");
						// TODO: handle exception
					}
				}
			}

			if (cla.trim().contains("onPostDelete")) {
				GeneralValueObject.hapus(serializable.getClass(), id.toString());
			}

			GeneralValueObject.masukkanData(serializable.getClass(), generalValueObject);

			if (generalValueObject instanceof HasilUjianMahasiswa) {
				HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) generalValueObject;
				String keyhasil = HasilUjianMahasiswa.genKey(hasilUjianMahasiswa.getPertemuanPunyaUjian(),
						hasilUjianMahasiswa.getMahasiswa(), hasilUjianMahasiswa.getBiodataCalonMahasiswa(),
						hasilUjianMahasiswa.getSiswa(), hasilUjianMahasiswa.getCalonSiswa());
				GeneralValueObject.masukkanDataLangsung(HasilUjianMahasiswa.class, generalValueObject, keyhasil);
			}
		}

		if (cla.trim().contains("onPostInsert") || cla.trim().contains("onPostUpdate")) {

			if (serializable instanceof IsiAngketParameterUmum) {
				IsiAngketParameterUmum p = (IsiAngketParameterUmum) serializable;
				if (p.getMahasiswa() != null) {
					p.getMahasiswa().populateIsiAngketParameterUmum(p);
				} else if (p.getDosen() != null) {
					p.getDosen().populateIsiAngketParameterUmum(p);
				} else if (p.getTbmuser() != null) {
					p.getTbmuser().populateIsiAngketParameterUmum(p);
				}
			} else if (serializable instanceof ChecklistHasilPenilaianUmum) {
				ChecklistHasilPenilaianUmum p = (ChecklistHasilPenilaianUmum) serializable;
				if (p.getMahasiswa() != null) {
					p.getMahasiswa().populateChecklistHasilPenilaianUmum(p, p.getPertemuanId(),
							p.getTbmuserDinilai() == null ? null : p.getTbmuserDinilai().getUserId());
				} else if (p.getDosen() != null) {
					p.getDosen().populateChecklistHasilPenilaianUmum(p, p.getPertemuanId(),
							p.getTbmuserDinilai() == null ? null : p.getTbmuserDinilai().getUserId());
				} else if (p.getTbmuser() != null) {
					p.getTbmuser().populateChecklistHasilPenilaianUmum(p, p.getPertemuanId(),
							p.getTbmuserDinilai() == null ? null : p.getTbmuserDinilai().getUserId());
				}
			} else if (serializable instanceof ChecklistBaruPenilaianDosenOlehMahasiswa) {
				ChecklistBaruPenilaianDosenOlehMahasiswa p = (ChecklistBaruPenilaianDosenOlehMahasiswa) serializable;
				if (p.getMahasiswa() != null) {
					p.getMahasiswa().populateChecklistBaruPenilaianDosenOlehMahasiswa(p.getId());
				}
			} else if (serializable instanceof CicilanPembayaran) {
				CicilanPembayaran p = (CicilanPembayaran) serializable;

				CicilanPembayaran.populatePembayaran(p, p.getKegiatan());

			} else if (serializable instanceof Kegiatan) {
				Kegiatan p = (Kegiatan) serializable;
				if (p.getMahasiswa() != null) {
					p.getMahasiswa().populateKegiatan(p.getId());
				}
				if (p.getCalonMahasiswa() != null) {
					p.getCalonMahasiswa().populateKegiatan(p.getId());
				}
			} else if (serializable instanceof Detailperkuliahan) {
				Detailperkuliahan p = (Detailperkuliahan) serializable;
				if (p.getPerkuliahan() != null) {
					p.getPerkuliahan().belum("detailperkulaiahan");
				}
				if (p.getMahasiswa() != null) {
					Mahasiswa mahasiswa = p.getMahasiswa();
					mahasiswa.populateDetailperkuliahan(p, true);
				}
				if (p.getPerkuliahan() != null) {
					Perkuliahan perkuliahan = p.getPerkuliahan();
					perkuliahan.populateDetailperkuliahan(p);
				}
			}

			else if (serializable instanceof Pertemuan) {
				Pertemuan p = (Pertemuan) serializable;
				if (p.getPerkuliahan() != null) {
					p.getPerkuliahan().populatePertemuan(p);
				}
				if (p.getKelompokKkn() != null) {
					p.getKelompokKkn().populatePertemuan(p);
				}
				if (p.getKelompokPkl() != null) {
					p.getKelompokPkl().populatePertemuan(p);
				}
				if (p.getKrsMahasiswa() != null) {
					p.getKrsMahasiswa().populatePertemuan(p);
				}
				if (p.getMahasiswaRequestTugasAkhir() != null) {
					p.getMahasiswaRequestTugasAkhir().populatePertemuan(p);
				}
				if (p.getSkripsi() != null) {
					p.getSkripsi().populatePertemuan(p);
				}
				if (p.getJadwalPelajaran() != null) {
					p.getJadwalPelajaran().populatePertemuan(p);
				}
				if (p.getJadwalUjianPMB() != null) {
					p.getJadwalUjianPMB().populatePertemuan(p);
				}
				if (p.getJadwalUjianPSB() != null) {
					p.getJadwalUjianPSB().populatePertemuan(p);
				}
				if (p.getFormulirKegiatan() != null) {
					p.getFormulirKegiatan().populatePertemuan(p);
				}
			}

			else if (serializable instanceof PertemuanPunyaDiskusi) {
				PertemuanPunyaDiskusi p = (PertemuanPunyaDiskusi) serializable;
				if (p.getPertemuan() != null) {
					p.getPertemuan().populatePertemuanPunyaDiskusi(p.getId(), true);
				}

			}

			else if (serializable instanceof TugasPertemuan) {
				TugasPertemuan p = (TugasPertemuan) serializable;
				if (p.getPertemuan() != null) {
					Pertemuan pertemuan = p.ambilPertemuan();
					if (pertemuan != null) {
						pertemuan.populateTugasPertemuan(p.getId(), true);
					}
				}

			}

			else if (serializable instanceof TugasKelompok) {
				TugasKelompok p = (TugasKelompok) serializable;
				if (p.getPertemuan() != null) {
					Pertemuan pertemuan = p.ambilPertemuan();
					if (pertemuan != null) {
						pertemuan.populateTugasKelompok(p.getId(), true);
					}
				}
			}

			else if (serializable instanceof PertemuanPunyaUjian) {
				PertemuanPunyaUjian p = (PertemuanPunyaUjian) serializable;
				if (p.getPertemuan() != null) {
					p.getPertemuan().populatePertemuanPunyaUjian(p.getId(), true);
				}

			}

			else if (serializable instanceof PertemuanFileContent) {
				PertemuanFileContent p = (PertemuanFileContent) serializable;
				if (p.getPertemuan() != null) {
					Pertemuan.populatePertemuanFileContent(p, true);
				}

			}

			else if (serializable instanceof TugasFileContent) {
				TugasFileContent p = (TugasFileContent) serializable;
				if (p.getPertemuan() != null) {
					try {
						Pertemuan.populateTugasFileContent(p, Class.forName(p.getClassFrom()), true);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/hibernate/AuditListener.java:1632");
					}
				}

			}

			else if (serializable instanceof VideoPertemuan) {
				VideoPertemuan p = (VideoPertemuan) serializable;
				if (p.getPertemuan() != null) {
					Pertemuan.populateVideoPertemuan(p, true);
				}

			}

			else if (serializable instanceof AudioPertemuan) {
				AudioPertemuan p = (AudioPertemuan) serializable;
				if (p.getPertemuan() != null) {
					Pertemuan.populateAudioPertemuan(p, true);
				}

			}

			else if (serializable instanceof BankSoalDetail) {
				BankSoalDetail p = (BankSoalDetail) serializable;
				if (p.getBankSoal() != null) {
					p.getBankSoal().populateBankSoalDetail(p, true);
				}

			}

			else if (serializable instanceof FormatNilai) {
				FormatNilai p = (FormatNilai) serializable;
				if (p.getPerkuliahan() != null) {
					p.getPerkuliahan().populateFormatNilai(p, true);
				}
			}

			// else if (serializable instanceof HasilUjianMahasiswaDetail) {
			// HasilUjianMahasiswaDetail p = (HasilUjianMahasiswaDetail)
			// serializable;
			// if (p.getHasilUjianMahasiswa() != null) {
			// p.getHasilUjianMahasiswa().populateHasilUjianMahasiswaDetail(p,
			// true);
			// }
			//
			// }

			else if (serializable instanceof HasilUjianMahasiswa) {
				HasilUjianMahasiswa p = (HasilUjianMahasiswa) serializable;
				if (p.getPertemuanPunyaUjian() != null) {
					p.getPertemuanPunyaUjian().populateHasilUjianMahasiswa(p, true);
				}

			}

			else if (serializable instanceof UjianPunyaSoal) {
				UjianPunyaSoal p = (UjianPunyaSoal) serializable;
				if (p.getUjian() != null) {
					p.getUjian().populateUjianPunyaSoal(p, true);
				}

			}

			else if (serializable instanceof MatakuliahEkivalen) {
				MatakuliahEkivalen p = (MatakuliahEkivalen) serializable;
				if (p.getMatakuliah() != null && p.getMatakuliahEkivalen() != null) {
					p.getMatakuliah().populateEkivalen(p);
					p.getMatakuliahEkivalen().populateEkivalen(p);
				}
			}
		} else if (cla.trim().contains("onPostDelete")) {

			if (serializable instanceof IsiAngketParameterUmum) {
				IsiAngketParameterUmum p = (IsiAngketParameterUmum) serializable;
				if (p.getMahasiswa() != null) {
					p.getMahasiswa().removeIsiAngketParameterUmum(id);
				} else if (p.getDosen() != null) {
					p.getDosen().removeIsiAngketParameterUmum(id);
				} else if (p.getTbmuser() != null) {
					p.getTbmuser().removeIsiAngketParameterUmum(id);
				}
			} else if (serializable instanceof ChecklistHasilPenilaianUmum) {
				ChecklistHasilPenilaianUmum p = (ChecklistHasilPenilaianUmum) serializable;
				if (p.getMahasiswa() != null) {
					p.getMahasiswa().removeChecklistHasilPenilaianUmum(id, p.getPertemuanId(),
							p.getTbmuserDinilai() == null ? null : p.getTbmuserDinilai().getUserId());
				} else if (p.getDosen() != null) {
					p.getDosen().removeChecklistHasilPenilaianUmum(id, p.getPertemuanId(),
							p.getTbmuserDinilai() == null ? null : p.getTbmuserDinilai().getUserId());
				} else if (p.getTbmuser() != null) {
					p.getTbmuser().removeChecklistHasilPenilaianUmum(id, p.getPertemuanId(),
							p.getTbmuserDinilai() == null ? null : p.getTbmuserDinilai().getUserId());
				}
			} else if (serializable instanceof ChecklistBaruPenilaianDosenOlehMahasiswa) {
				ChecklistBaruPenilaianDosenOlehMahasiswa p = (ChecklistBaruPenilaianDosenOlehMahasiswa) serializable;
				if (p.getMahasiswa() != null) {
					p.getMahasiswa().removeChecklistBaruPenilaianDosenOlehMahasiswa(id);
				}
			} else if (serializable instanceof CicilanPembayaran) {
				CicilanPembayaran p = (CicilanPembayaran) serializable;

				CicilanPembayaran.populateHapusPembayaran(p, p.getKegiatan());

			} else if (serializable instanceof Kegiatan) {
				Kegiatan p = (Kegiatan) serializable;
				if (p.getMahasiswa() != null) {
					p.getMahasiswa().removeKegiatan(id);
				}
				if (p.getCalonMahasiswa() != null) {
					p.getCalonMahasiswa().removeKegiatan(id);
				}
			} else if (serializable instanceof Detailperkuliahan) {
				Detailperkuliahan p = (Detailperkuliahan) serializable;
				if (p.getMahasiswa() != null) {
					Mahasiswa mahasiswa = p.getMahasiswa();
					mahasiswa.removeDetailperkuliahan(id);
				}
				if (p.getPerkuliahan() != null) {
					Perkuliahan perkuliahan = p.getPerkuliahan();
					perkuliahan.removeDetailperkuliahan(p);
				}
			} else if (serializable instanceof Pertemuan) {
				Pertemuan p = (Pertemuan) serializable;
				if (p.getPerkuliahan() != null) {
					p.getPerkuliahan().removePertemuan(id);
				}
				if (p.getKelompokKkn() != null) {
					p.getKelompokKkn().removePertemuan(p);
				}
				if (p.getKelompokPkl() != null) {
					p.getKelompokPkl().removePertemuan(p);
				}
				if (p.getKrsMahasiswa() != null) {
					p.getKrsMahasiswa().removePertemuan(p);
				}
				if (p.getMahasiswaRequestTugasAkhir() != null) {
					p.getMahasiswaRequestTugasAkhir().removePertemuan(p);
				}
				if (p.getSkripsi() != null) {
					p.getSkripsi().removePertemuan(p);
				}
				if (p.getJadwalPelajaran() != null) {
					p.getJadwalPelajaran().removePertemuan(p);
				}
				if (p.getJadwalUjianPMB() != null) {
					p.getJadwalUjianPMB().removePertemuan(p);
				}
				if (p.getJadwalUjianPSB() != null) {
					p.getJadwalUjianPSB().removePertemuan(p);
				}
				if (p.getFormulirKegiatan() != null) {
					p.getFormulirKegiatan().removePertemuan(p);
				}
			} else if (serializable instanceof PertemuanPunyaDiskusi) {
				PertemuanPunyaDiskusi p = (PertemuanPunyaDiskusi) serializable;
				if (p.getPertemuan() != null) {
					p.getPertemuan().removePertemuanPunyaDiskusi(id);
				}
			} else if (serializable instanceof TugasPertemuan) {
				TugasPertemuan p = (TugasPertemuan) serializable;
				if (p.getPertemuan() != null) {
					Pertemuan pertemuan = p.ambilPertemuan();
					if (pertemuan != null) {
						pertemuan.removeTugasPertemuan(id);
					}
				}
			} else if (serializable instanceof TugasKelompok) {
				TugasKelompok p = (TugasKelompok) serializable;
				if (p.getPertemuan() != null) {
					Pertemuan pertemuan = p.ambilPertemuan();
					if (pertemuan != null) {
						pertemuan.removeTugasKelompok(id);
					}
				}
			} else if (serializable instanceof PertemuanPunyaUjian) {
				PertemuanPunyaUjian p = (PertemuanPunyaUjian) serializable;
				if (p.getPertemuan() != null) {
					p.getPertemuan().removePertemuanPunyaUjian(id);
				}
			} else if (serializable instanceof PertemuanFileContent) {
				PertemuanFileContent p = (PertemuanFileContent) serializable;
				if (p.getPertemuan() != null) {
					Pertemuan.removePertemuanFileContent(p);
				}
			} else if (serializable instanceof TugasFileContent) {
				TugasFileContent p = (TugasFileContent) serializable;
				if (p.getPertemuan() != null) {
					try {

						Long idmy = p.getMahasiswa() != null && p.getMahasiswa() > 0L ? p.getMahasiswa()
								: p.getBiodataCalonMahasiswa();

						if (p.getSiswa() != null && p.getSiswa() > 0L) {
							idmy = p.getSiswa();
						}
						if (p.getCalonSiswa() != null && p.getCalonSiswa() > 0L) {
							idmy = p.getCalonSiswa();
						}

						Pertemuan.removeTugasFileContent(idmy, Class.forName(p.getClassFrom()));
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/hibernate/AuditListener.java:1834");
					}
				}
			} else if (serializable instanceof VideoPertemuan) {
				VideoPertemuan p = (VideoPertemuan) serializable;
				if (p.getPertemuan() != null) {
					Pertemuan.removeVideoPertemuan(p);
				}
			} else if (serializable instanceof AudioPertemuan) {
				AudioPertemuan p = (AudioPertemuan) serializable;
				if (p.getPertemuan() != null) {
					Pertemuan.removeAudioPertemuan(p);
				}
			}

			else if (serializable instanceof BankSoalDetail) {
				BankSoalDetail p = (BankSoalDetail) serializable;
				if (p.getBankSoal() != null) {
					p.getBankSoal().removeBankSoalDetail(id);
				}
			}

			else if (serializable instanceof FormatNilai) {
				FormatNilai p = (FormatNilai) serializable;
				if (p.getPerkuliahan() != null) {
					p.getPerkuliahan().removeFormatNilai(id);
				}
			}

			// else if (serializable instanceof HasilUjianMahasiswaDetail) {
			// HasilUjianMahasiswaDetail p = (HasilUjianMahasiswaDetail)
			// serializable;
			// if (p.getHasilUjianMahasiswa() != null) {
			// p.getHasilUjianMahasiswa().removeHasilUjianMahasiswaDetail(id);
			// }
			// }

			else if (serializable instanceof HasilUjianMahasiswa) {
				HasilUjianMahasiswa p = (HasilUjianMahasiswa) serializable;
				if (p.getPertemuanPunyaUjian() != null) {
					p.getPertemuanPunyaUjian().removeHasilUjianMahasiswa(id);
				}
			}

			else if (serializable instanceof UjianPunyaSoal) {
				UjianPunyaSoal p = (UjianPunyaSoal) serializable;
				if (p.getUjian() != null) {
					p.getUjian().removeUjianPunyaSoal(id);
				}
			}

			else if (serializable instanceof MatakuliahEkivalen) {
				MatakuliahEkivalen p = (MatakuliahEkivalen) serializable;
				if (p.getMatakuliah() != null && p.getMatakuliahEkivalen() != null) {
					p.getMatakuliah().removeEkivalen(id);
					p.getMatakuliahEkivalen().removeEkivalen(id);
				}
			}
		}

	}

	@Override
	public void onPostDelete(PostDeleteEvent arg0) {
		// TODO Auto-generated method stub
		Serializable serializable = (Serializable) arg0.getEntity();

		if (!AuditTrailHelper.isAuditable(serializable)) {
			AuditTrailHelper.debug("AuditListener.onPostDelete skip non-auditable "
					+ AuditTrailHelper.describeEntity(serializable, arg0.getId()));
			return;
		}
		AuditTrailHelper.debug("AuditListener.onPostDelete proses "
				+ AuditTrailHelper.describeEntity(serializable, arg0.getId()));

		String cla = this.getClass().getName() + " onPostDelete ";

		if (masukProses()) {
			try {
				proses(serializable, cla, arg0.getId());
			} finally {
				keluarProses();
			}
		}

		super.onPostDelete(arg0);
	}

	@Override
	public void onPostInsert(PostInsertEvent arg0) {
		// TODO Auto-generated method stub

		Serializable serializable = (Serializable) arg0.getEntity();
		if (!AuditTrailHelper.isAuditable(serializable)) {
			AuditTrailHelper.debug("AuditListener.onPostInsert skip non-auditable "
					+ AuditTrailHelper.describeEntity(serializable, arg0.getId()));
			return;
		}
		AuditTrailHelper.debug("AuditListener.onPostInsert proses "
				+ AuditTrailHelper.describeEntity(serializable, arg0.getId()));
		String cla = this.getClass().getName() + " onPostInsert ";

		if (masukProses()) {
			try {
				proses(serializable, cla, arg0.getId());
			} finally {
				keluarProses();
			}
		}

		if (serializable != null && (serializable instanceof CicilanPembayaran)) {
			CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) serializable;
			if (cicilanPembayaran != null && cicilanPembayaran.getPengaturanPembayaranBulanan() != null) {
				cicilanPembayaran.getPengaturanPembayaranBulanan().checkDendaCicilan(cicilanPembayaran, null);
			}
			Kegiatan kegiatan = cicilanPembayaran.getKegiatan();
			if (cicilanPembayaran != null && kegiatan != null) {
				if (kegiatan != null) {
					kegiatan.appendCicilan(cicilanPembayaran);
				}
			}
		} else if (serializable != null && (serializable instanceof DetailKegiatan)) {
			DetailKegiatan detailKegiatan = (DetailKegiatan) serializable;

			Kegiatan kegiatan = detailKegiatan.getKegiatan();
			if (detailKegiatan != null && kegiatan != null) {
				if (kegiatan != null) {
					kegiatan.appendDetailKegiatan(detailKegiatan);
				}
			}
		}

		super.onPostInsert(arg0);
	}

	@Override
	public void onPostUpdate(final PostUpdateEvent arg0) {
		Serializable serializable = (Serializable) arg0.getEntity();

		if (!AuditTrailHelper.isAuditable(serializable)) {
			AuditTrailHelper.debug("AuditListener.onPostUpdate skip non-auditable "
					+ AuditTrailHelper.describeEntity(serializable, arg0.getId()));
			return;
		}

		Boolean updateDecision = AuditTrailHelper.consumeUpdateDecision(serializable, arg0.getId());
		boolean hasBusinessChange = updateDecision == null
				? AuditTrailHelper.hasBusinessChange(arg0.getState(), arg0.getOldState(),
						arg0.getPersister() == null ? null : arg0.getPersister().getPropertyNames())
				: updateDecision.booleanValue();

		AuditTrailHelper.debug("AuditListener.onPostUpdate entity=" + AuditTrailHelper.describeEntity(serializable, arg0.getId())
				+ ", hasBusinessChange=" + hasBusinessChange
				+ ", decisionSource=" + (updateDecision == null ? "PostUpdateEvent.oldState" : "UpdateEventListener"));

		if (!hasBusinessChange) {
			AuditTrailHelper.debug("AuditListener.onPostUpdate skip super/proses karena hanya metadata audit atau tidak ada perubahan.");
			return;
		}

		String changes = AuditTrailHelper.buildBusinessChangeText(arg0.getState(), arg0.getOldState(),
				arg0.getPersister() == null ? null : arg0.getPersister().getPropertyNames());
		if (changes != null && changes.trim().length() > 0) {
			AuditTrailHelper.debug("AuditListener.onPostUpdate changes="
					+ AuditTrailHelper.abbreviate(changes.replace('\n', ';'), 800));
		}

		String cla = this.getClass().getName() + " onPostUpdate ";

		if (masukProses()) {
			try {
				proses(serializable, cla, arg0.getId());
			} finally {
				keluarProses();
			}
		}

		super.onPostUpdate(arg0);
	}

}
