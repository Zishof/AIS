package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONException;
import org.json.JSONObject;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.GeneralValueObject;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.VOMahasiswa;
import ais.ui.util.WaktuUtil;

/**
 * Helper persistence Kegiatan.
 *
 * Fokus class ini:
 * - Mengambil dan menyinkronkan daftar DetailKegiatan/CicilanPembayaran.
 * - Membangun ulang field denormalisasi Kegiatan: bulans, tagihans, tagihan,
 *   dibayar, persentase, cicilans, dan detailKegiatans.
 * - Menjaga agar update tidak membebani UI melalui antrean async yang lebih
 *   kecil dan aman.
 *
 * Catatan session:
 * - Session yang dibuat dengan openSession() selalu ditutup di finally.
 * - currentSession() tidak ditutup oleh helper ini.
 */
public class KegiatanPersistenceHelper {

	private static final String HQL_UPDATE_KEGIATAN = "UPDATE Kegiatan SET bulans = :nilaiBaru, "
			+ "tagihans = :nilaiTagihanBaru, tagihan = :tagihanBaru, dibayar = :dibayarBaru, "
			+ "persentase = :persentaseBaru, cicilans = :cicilansBaru, "
			+ "detailKegiatans = :detailKegiatansBaru WHERE id = :idKegiatan";

	private static final int MAX_IN_CLAUSE_SIZE = 800;
	private static final int ASYNC_DELAY_SECONDS = 15;
	private static final int MAX_RETRY = 5;

	private static final ScheduledExecutorService asyncExecutor = Executors.newScheduledThreadPool(
			hitungThreadPoolAman(Runtime.getRuntime().availableProcessors()), new ThreadFactory() {
				@Override
				public Thread newThread(Runnable runnable) {
					Thread thread = new Thread(runnable);
					thread.setDaemon(true);
					thread.setName("AsyncUpdateKegiatan-Thread-" + thread.getId());
					return thread;
				}
			});

	private static final ConcurrentHashMap<Long, PendingKegiatanData> pendingTasks = new ConcurrentHashMap<Long, PendingKegiatanData>();
	private static final Object[] kegiatanLocks = buatKegiatanLocks();

	private static class PendingKegiatanData {
		private ScheduledFuture<?> future;
		private String cicilans;
		private String detailKegiatans;
		private Kegiatan kegiatan;

		private PendingKegiatanData(String cicilans, String detailKegiatans, Kegiatan kegiatan) {
			this.cicilans = cicilans;
			this.detailKegiatans = detailKegiatans;
			this.kegiatan = kegiatan;
		}
	}

	private static class RekapPembayaran {
		private String bulans = "{}";
		private Double dibayar = Double.valueOf(0.0);
	}

	public static int hitungThreadPoolAman(int totalData) {
		int processor = Runtime.getRuntime().availableProcessors();
		int batasProcessor = processor <= 0 ? 2 : processor;
		int jumlah = Math.min(8, Math.max(1, batasProcessor));
		if (totalData > 0) {
			jumlah = Math.min(jumlah, totalData);
		}
		return Math.max(1, jumlah);
	}

	public static void closeOpenedSession(Session session) {
		if (session == null) {
			return;
		}
		try {
			if (session.isOpen()) {
				try {
					session.clear();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:115");
				}
				try {
					session.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:119");
				}
				session.close();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:125");
		}
	}

	public static void closeNativeSession(Session session) {
		closeOpenedSession(session);
		try {
			HibernateUtil.closeSession();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:133");
		}
	}

	private static void rollbackQuietly(Transaction tx) {
		if (tx != null) {
			try {
				if (tx.isActive()) {
					tx.rollback();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:143");
			}
		}
	}

	private static boolean isEmpty(String value) {
		return value == null || value.trim().length() == 0;
	}

	private static Double safeDouble(Double value) {
		return value == null ? Double.valueOf(0.0) : value;
	}

	private static long safeLong(Double value) {
		return value == null ? 0L : value.longValue();
	}

	private static Long getId(Object object) {
		if (object == null) {
			return null;
		}
		if (object instanceof CicilanPembayaran) {
			return ((CicilanPembayaran) object).getId();
		}
		if (object instanceof DetailKegiatan) {
			return ((DetailKegiatan) object).getId();
		}
		try {
			Object value = object.getClass().getMethod("getId", new Class[0]).invoke(object, new Object[0]);
			if (value instanceof Number) {
				return Long.valueOf(((Number) value).longValue());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:175");
		}
		return null;
	}

	// ========================================================================
	// 1. PENGAMBILAN DETAIL KEGIATAN DAN CICILAN
	// ========================================================================

	@SuppressWarnings("unchecked")
	public static List<DetailKegiatan> ambilDetailKegiatanSaja(Kegiatan kegiatan, boolean refresh) {
		if (kegiatan == null || kegiatan.getId() == null) {
			return new ArrayList<DetailKegiatan>();
		}

		TreeSet<Long> keyData = new TreeSet<Long>();

		if (!refresh) {
			List<Long> aktifIds = kegiatan.ambilDetailKegiatansAktifIds();
			if (aktifIds != null && !aktifIds.isEmpty()) {
				keyData.addAll(aktifIds);
			}
		}

		Session session = null;
		try {
			if (refresh || (keyData.isEmpty() && !kegiatan.udah("ambilDetailKegiatanSaja"))) {
				Exception last = null;
				for (int attempt = 0; attempt < 2; attempt++) {
					try {
						session = HibernateUtil.getSessionFactory().openSession();
						Criteria criteria = session.createCriteria(DetailKegiatan.class);
						criteria.setProjection(Projections.property("id"));
						criteria.add(Restrictions.eq("kegiatan", kegiatan));
						criteria.addOrder(Order.asc("id"));
						criteria.setTimeout(600);
						List<Long> dbKeys = criteria.list();
						if (dbKeys != null && !dbKeys.isEmpty()) keyData.addAll(dbKeys);
						last = null;
						break;
					} catch (Exception queryError) {
						last = queryError;
						if (!isConnectionFailure(queryError) || attempt > 0) throw queryError;
					} finally {
						closeOpenedSession(session);
						session = null;
					}
				}
				if (last != null) throw last;
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeOpenedSession(session);
		}

		List<DetailKegiatan> hasilDetailKegiatan = new ArrayList<DetailKegiatan>();
		if (!keyData.isEmpty()) {
			hasilDetailKegiatan = GeneralValueObject.ambilDataBanyak(DetailKegiatan.class, new ArrayList<Long>(keyData),
					refresh);
		}

		updateDetailKegiatan(hasilDetailKegiatan, kegiatan, refresh);
		return hasilDetailKegiatan;
	}

	private static boolean isConnectionFailure(Throwable error) {
		Throwable current = error;
		while (current != null) {
			String name = current.getClass().getName();
			String message = current.getMessage() == null ? "" : current.getMessage().toLowerCase();
			if (name.indexOf("JDBCConnectionException") >= 0 || name.indexOf("EOFException") >= 0
					|| name.indexOf("SocketException") >= 0 || message.indexOf("connection has been closed") >= 0
					|| message.indexOf("i/o error") >= 0) return true;
			current = current.getCause();
		}
		return false;
	}

	// ========================================================================
	// HITUNG TAGIHAN "SEGAR & KONSISTEN" (READ-ONLY)
	// ========================================================================

	/**
	 * Hitung ulang total tagihan Kegiatan langsung dari template biaya AKTIF/terkini
	 * (bukan dari field denormalisasi Kegiatan.tagihan/tagihans yang bisa basi/tertukar
	 * generasi), dengan sumber nilai yang KONSISTEN dengan tampilan "Daftar Rincian
	 * Tagihan" (DetailKegiatan ter-baru per item, dikurangi diskon tersimpan terbesar).
	 * Murni READ-ONLY -- tidak menulis apa pun ke database, aman dipanggil berulang
	 * (mis. tiap render baris grid) tanpa efek samping/nilai bolak-balik.
	 *
	 * Dipakai bersama oleh InformasiPembayaranMahasiswaAction (dashboard/rincian
	 * mahasiswa) dan DetailSettingBiayaAction (grid mahasiswa yang cocok dengan satu
	 * SettingBiaya) agar keduanya SELALU menampilkan angka yang sama persis untuk
	 * Kegiatan yang sama -- sebelumnya logika ini terduplikasi lokal di
	 * InformasiPembayaranMahasiswaAction saja.
	 *
	 * @return total tagihan, atau {@code null} bila gerbang konfigurasi
	 *         {@code dashboard_tagihan_segar_konsisten} nonaktif atau data kegiatan
	 *         belum lengkap (pemanggil sebaiknya fallback ke {@link Kegiatan#hitungTagihan()}).
	 */
	@SuppressWarnings("rawtypes")
	public static Double hitungTagihanSegarKonsisten(Kegiatan k) {
		try {
			if (k == null || k.getJenisKegiatan() == null || k.getSemster() == null
					|| (k.getMahasiswa() == null && k.getCalonMahasiswa() == null)) {
				return null;
			}
			if (!Konfigurasi.AKTIF.equals(
					Common.getKonfigurasi("dashboard_tagihan_segar_konsisten", Konfigurasi.AKTIF).getNilai())) {
				return null;
			}
			Mahasiswa m = k.getMahasiswa();
			Integer smt = k.getSemster();
			Collection detailBiayas;
			if (k.getCalonMahasiswa() != null) {
				// Kegiatan milik CALON mahasiswa (pendaftaran/daftar ulang): WAJIB memakai
				// template jalur-CALON, persis seperti layar pembayarannya
				// (DaftarUlangMahasiswaBaruAction). Template jalur-MAHASISWA untuk kegiatan
				// jenis ini bisa BASI: cohort search-nya kosong sehingga cache lama tidak
				// pernah tergantikan dan menunjuk generasi DetailBiaya lama dengan nominal
				// lama -> header kelebihan (log kasus: item 99 via jalur-mahasiswa 18.95jt
				// [detailBiaya 31194] vs jalur-calon 15.95jt [detailBiaya 31006]).
				BiodataCalonMahasiswa cm = k.getCalonMahasiswa();
				Jurusan jurusanCalon = cm.getProdiLulus();
				if (jurusanCalon == null || jurusanCalon.getId() == null) {
					jurusanCalon = cm.getProdi1() == null ? cm.getProdi2() : cm.getProdi1();
				}
				detailBiayas = PembayaranUtilHelper.getDetailBiayaCalonMahasiswa(cm, k.getJenisKegiatan(),
						jurusanCalon, smt, false);
			} else {
				detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(m, smt, k.getJenisKegiatan(), false);
			}
			boolean adaBulanan = false;
			if (detailBiayas != null) {
				for (Object o : detailBiayas) {
					if (o instanceof PengaturanPembayaranBulanan) {
						adaBulanan = true;
						break;
					}
				}
			}
			if (adaBulanan && k.getCalonMahasiswa() == null) {
				detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(m, smt, k.getJenisKegiatan(), "-1", false);
			}
			Collection detailKegiatans = (k.getId() == null) ? null : k.ambilDetailKegiatan();

			// === Resolusi diskon DETERMINISTIK & READ-ONLY (anti BOLAK-BALIK 1.8jt/5.165jt) ===
			// "Hitung Ulang"/recompute MENUMPUK DetailKegiatan DUPLIKAT per item: sebagian sudah
			// meng-cache diskon BENAR (kadang kini NON-AKTIF), sebagian fresh diskon=0. Memilih SATU
			// DK (ambilSatuDetailKegiatan) hasilnya TIDAK stabil -> tagihan dasbor bolak-balik. Solusi:
			// pindai SEMUA DetailKegiatan kegiatan (aktif & non-aktif, via ambilDetailKegiatanSaja
			// refresh=true yang query by kegiatan TANPA filter aktif), simpan diskon TERBESAR per item
			// + status bukanTagihan dari DK TERBARU (id tertinggi). Diskon benar selalu ketemu -> hasil
			// KONSISTEN tiap render, tanpa menulis apa pun.
			//
			// FIX 2026-08-06 (kasus MARDI MARSON: item 500rb ganda ke-nol-kan, "Biaya Semester"
			// nilaiBisaDiubah sudah diedit langsung jadi 500rb NET tapi header tampil 0): diskon
			// TERBESAR di atas dulu SELALU dipasangkan ke `biaya` DK TERBARU walau keduanya berasal
			// dari GENERASI DK BERBEDA -> kalau baris terbaru sudah net (biaya diedit langsung jadi
			// lebih kecil, diskon di baris itu sendiri 0), diskon historis dari baris LAMA tetap
			// tersubtraksi lagi (double-count) -> hasil 0/negatif. Sekarang diskon historis HANYA
			// dipakai ulang bila baris tempat diskon terbesar itu ditemukan py `biaya` (gross) yang
			// SAMA dengan `biaya` baris terbaru -- artinya base belum berubah, cuma metadata diskon
			// hilang saat regenerasi (kasus asli yang dulu dilindungi fix ini). Bila base baris
			// terbaru SUDAH beda (diedit langsung / generasi baru beda nominal), pakai diskon MILIK
			// baris terbaru itu sendiri (bisa 0 -- base-nya memang sudah net), TIDAK subtraksi ulang.
			Map<String, double[]> diskonItemMap = new HashMap<String, double[]>();
			if (k.getId() != null) {
				try {
					List<DetailKegiatan> semuaDk = ambilDetailKegiatanSaja(k, true);
					if (semuaDk != null) {
						for (DetailKegiatan dkc : semuaDk) {
							if (dkc == null || dkc.getItemBiaya() == null || dkc.getItemBiaya().getId() == null) {
								continue;
							}
							// Samakan identitas dengan ambilSatuDetailKegiatan() yang dipakai panel rincian:
							// item + bayarKe + kegiatan. Memetakan hanya per item membuat baris historis
							// bayarKe lain dapat menimpa DPP/komponen aktif yang sedang ditampilkan.
							String detailKey = DetailKegiatan.kodeUnik(null, dkc.getItemBiaya(),
									dkc.getDetailBiaya() == null ? null : dkc.getDetailBiaya().getBayarKe(), k, null);
							if (detailKey == null) {
								continue;
							}
							// [maxDiskon, bukanTagihanTerbaru?1:0, idTerbaru, biayaDkTerbaru, adaBiayaDk?1:0,
							//  biayaPadaBarisMaxDiskon, diskonMilikBarisTerbaruSendiri]
							double[] info = diskonItemMap.get(detailKey);
							if (info == null) {
								info = new double[] { 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0 };
								diskonItemMap.put(detailKey, info);
							}
							double diskonBarisIni = dkc.getDiskon() == null ? 0.0 : dkc.getDiskon().doubleValue();
							double biayaBarisIni = dkc.getBiaya() == null ? 0.0 : dkc.getBiaya().doubleValue();
							if (diskonBarisIni > info[0]) {
								info[0] = diskonBarisIni;
								info[5] = biayaBarisIni;
							}
							double idDk = dkc.getId() == null ? -1.0 : dkc.getId().doubleValue();
							// Kondisi/nominal terkini harus berasal dari baris AKTIF. Baris nonaktif tetap
							// dipindai di atas hanya untuk fallback histori diskon, bukan untuk meniadakan
							// tagihan aktif (kasus DPP hilang dari total panel kiri).
							if (Boolean.TRUE.equals(dkc.getAktif()) && idDk >= info[2]) {
								info[2] = idDk;
								info[1] = (dkc.getBukanTagihan() != null && dkc.getBukanTagihan()) ? 1.0 : 0.0;
								info[3] = biayaBarisIni;
								info[4] = 1.0;
								info[6] = diskonBarisIni;
							}
						}
					}
				} catch (Exception eDk) {
					ais.common.ErrorAuditUtil.record(eDk,
							"auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:hitungTagihanSegarKonsisten");
				}
			}

			double total = 0.0;
			// ANTI "NOMINAL KELEBIHAN": template biaya bisa berisi item DOBEL (mahasiswa cocok
			// ke >1 baris Setting Biaya dgn item sama). Rincian tagihan di layar ini sudah
			// men-dedup per item, tapi penjumlahan header dulunya TIDAK -> header lebih besar
			// dari jumlah rincian (mis. selisih persis 1x nilai item yang dobel). Dedup di sini
			// per kunci itemBiaya+bayarKe (non-bulanan) dan per-PPB (bulanan), READ-ONLY.
			java.util.Set<String> kunciSudahDihitung = new java.util.HashSet<String>();
			if (detailBiayas != null) {
				for (Object o : detailBiayas) {
					if (o instanceof DetailBiaya) {
						DetailBiaya db = (DetailBiaya) o;
						String kunciDb = "item:"
								+ (db.getItemBiaya() == null || db.getItemBiaya().getId() == null ? "?"
										: db.getItemBiaya().getId().toString())
								+ "|bayarKe:" + (db.getBayarKe() == null ? "1" : db.getBayarKe().toString());
						if (!kunciSudahDihitung.add(kunciDb)) {
							continue;
						}
						// Nilai dasar item. Overload 2-arg untuk item HARGA-TETAP (nilaiBisaDiubah=false)
						// TIDAK mengurangi diskon (DetailKegiatan tak di-resolve) -> nilai BRUTO.
						Double j = Kegiatan.ambilJumlahTagihan(k, db);
						// PENTING (idempoten): JANGAN memanggil overload ber-DetailKegiatan di sini karena
						// itu memicu hitungDiskon yang MENULIS ke DB (setDiskon/refreshUpdate) + menutup
						// session. Recompute display ini harus READ-ONLY; bila tidak, tiap klik "Hitung
						// Ulang" mengubah state diskon -> hasil bolak-balik BENAR(1.8jt)/SALAH(5.165jt).
						// Solusi: untuk item harga-tetap, kurangi diskon yang SUDAH TERSIMPAN (read-only).
						if (j != null && db.getItemBiaya() != null && !db.getItemBiaya().getNilaiBisaDiubah()
								&& db.getItemBiaya().getId() != null) {
							String detailKey = DetailKegiatan.kodeUnik(null, db.getItemBiaya(), db.getBayarKe(),
									k, null);
							double[] info = diskonItemMap.get(detailKey);
							if (info != null) {
								if (info[1] == 1.0) {
									j = Double.valueOf(0.0);
								} else {
									double diskonDipakai = diskonEfektif(info);
									if (diskonDipakai > 0.0) {
										j = Double.valueOf(j.doubleValue() - diskonDipakai);
										if (j.doubleValue() < 0.0) {
											j = Double.valueOf(0.0);
										}
									}
								}
							}
						}
						// KONSISTEN DGN RINCIAN (fix header 28.25jt vs rincian 25.25jt): untuk item
						// nilaiBisaDiubah, ambilJumlahTagihan memilih SATU DetailKegiatan secara TIDAK
						// STABIL sehingga bisa mengambil baris DUPLIKAT LAMA yang nilainya basi (log
						// kasus: item 99 terhitung 18.95jt padahal DK terbaru 15.95jt). Tampilan
						// "Daftar Rincian Tagihan" memakai DK ter-BARU (id terbesar) -- header wajib
						// memakai sumber yang sama: neto = biaya DK terbaru - diskon (bukanTagihan=0).
						// Item khusus (parameterTambahan/skor, hitung tunggakan) tetap ke hasil lama.
						try {
							if (db.getItemBiaya() != null && db.getItemBiaya().getId() != null
									&& db.getItemBiaya().getNilaiBisaDiubah()
									&& db.getItemBiaya().getParameterTambahan() == null
									&& (db.getItemBiaya().getPenghitungan() == null || !db.getItemBiaya()
											.getPenghitungan().equals(ItemBiaya.HITUNG_TUNGGAKAN_SMT_LALU))) {
								String detailKey = DetailKegiatan.kodeUnik(null, db.getItemBiaya(), db.getBayarKe(),
										k, null);
								double[] info = diskonItemMap.get(detailKey);
								if (info != null && info.length >= 5 && info[4] == 1.0) {
									double neto = info[1] == 1.0 ? 0.0 : info[3] - diskonEfektif(info);
									if (neto < 0.0) {
										neto = 0.0;
									}
									j = Double.valueOf(neto);
								}
							}
						} catch (Exception eKoreksi) {
							ais.common.ErrorAuditUtil.record(eKoreksi,
									"auto-audit hitungTagihanSegarKonsisten koreksi DK-terbaru");
						}
						if (j != null) {
							total += j;
						}
					} else if (o instanceof PengaturanPembayaranBulanan) {
						PengaturanPembayaranBulanan ppb = (PengaturanPembayaranBulanan) o;
						if (ppb.getId() != null && !kunciSudahDihitung.add("ppb:" + ppb.getId())) {
							continue;
						}
						Double j = Kegiatan.ambilJumlahTagihan(k, detailKegiatans, m, smt, ppb);
						if (j != null) {
							total += j;
						}
					}
				}
			}
			return Double.valueOf(total);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Tentukan diskon yang BOLEH dipakai utk mengurangi {@code biaya} DK TERBARU (dipakai
	 * {@link #hitungTagihanSegarKonsisten(Kegiatan)}), menghindari DOUBLE-COUNT saat base sudah
	 * net.
	 *
	 * <p>{@code info} = {@code [maxDiskon, bukanTagihanTerbaru, idTerbaru, biayaDkTerbaru,
	 * adaBiayaDk, biayaPadaBarisMaxDiskon, diskonMilikBarisTerbaruSendiri]} (lihat pemindaian di
	 * {@link #hitungTagihanSegarKonsisten(Kegiatan)}).</p>
	 *
	 * <p><b>Aturan.</b> (1) Bila baris TERBARU sendiri sudah punya diskon tercatat (&gt;0),
	 * pakai itu -- paling akurat, sudah dipasangkan dgn biaya di baris yg sama. (2) Bila baris
	 * terbaru diskonnya 0/kosong TAPI biaya-nya SAMA dgn biaya baris tempat diskon terbesar
	 * historis ditemukan, berarti base belum berubah sejak generasi yang py diskon benar --
	 * metadata diskonnya saja yang hilang saat regenerasi -> pakai diskon historis itu (baru
	 * "reapply"). (3) Selain itu (base baris terbaru SUDAH beda dari base baris diskon historis
	 * -- mis. nilai diedit langsung jadi net) -> jangan subtraksi ulang, kembalikan 0.</p>
	 */
	private static double diskonEfektif(double[] info) {
		if (info == null || info.length < 7) {
			return 0.0;
		}
		if (info[6] > 0.0) {
			return info[6];
		}
		if (info[0] > 0.0 && info[3] == info[5]) {
			return info[0];
		}
		return 0.0;
	}

	@SuppressWarnings("unchecked")
	public static List<DetailKegiatan> ambilDetailKegiatanSaja(VOMahasiswa student, String jsonLokasiDetailKegiatan,
			Collection<Kegiatan> kegiatansCache, boolean refresh) {

		TreeSet<Long> keysData = new TreeSet<Long>();
		Map<Long, Kegiatan> mapKegiatanUtama = mapKegiatan(kegiatansCache);

		if (!refresh) {
			keysData.addAll(ekstrakIdDariJson(jsonLokasiDetailKegiatan));

			for (Kegiatan kegiatan : mapKegiatanUtama.values()) {
				List<Long> aktifIds = kegiatan.ambilDetailKegiatansAktifIds();
				if (aktifIds != null && !aktifIds.isEmpty()) {
					keysData.addAll(aktifIds);
				}
			}
		}

		Session session = null;
		try {
			if (refresh) {
				session = HibernateUtil.getSessionFactory().openSession();
				Criteria criteria = session.createCriteria(DetailKegiatan.class);
				criteria.setProjection(Projections.property("id"));

				Criteria kegCrit = criteria.createCriteria("kegiatan");
				if (student instanceof Mahasiswa) {
					kegCrit.add(Restrictions.eq("mahasiswa", student));
				} else {
					kegCrit.add(Restrictions.eq("calonMahasiswa", student));
				}

				kegCrit.addOrder(Order.asc("semster"));
				kegCrit.addOrder(Order.asc("jenisKegiatan"));
				kegCrit.addOrder(Order.asc("id"));

				criteria.setTimeout(600);
				List<Long> dbKeys = criteria.list();
				if (dbKeys != null && !dbKeys.isEmpty()) {
					keysData.addAll(dbKeys);
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeOpenedSession(session);
		}

		List<DetailKegiatan> detailKegiatans = new ArrayList<DetailKegiatan>();
		if (!keysData.isEmpty()) {
			detailKegiatans = GeneralValueObject.ambilDataBanyak(DetailKegiatan.class, new ArrayList<Long>(keysData),
					refresh);
		}

		sinkronkanDetailPerKegiatan(detailKegiatans, mapKegiatanUtama, refresh);
		return detailKegiatans;
	}

	@SuppressWarnings("unchecked")
	public static List<CicilanPembayaran> ambilCicilan(Kegiatan kegiatan, boolean refresh) {
		if (kegiatan == null || kegiatan.getId() == null) {
			return new ArrayList<CicilanPembayaran>();
		}

		TreeSet<Long> keyData = new TreeSet<Long>();

		if (!refresh) {
			List<Long> aktifIds = kegiatan.ambilCicilansAktifIds();
			if (aktifIds != null && !aktifIds.isEmpty()) {
				keyData.addAll(aktifIds);
			}
		}

		Session session = null;
		try {
			if (refresh || (keyData.isEmpty() && !kegiatan.udah("ambilCicilan"))) {
				session = HibernateUtil.getSessionFactory().openSession();
				Criteria criteria = session.createCriteria(CicilanPembayaran.class);
				criteria.setProjection(Projections.property("id"));
				criteria.add(Restrictions.eq("kegiatan", kegiatan));
				criteria.addOrder(Order.asc("id"));
				criteria.setTimeout(600);

				List<Long> dbKeys = criteria.list();
				if (dbKeys != null && !dbKeys.isEmpty()) {
					keyData.addAll(dbKeys);
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeOpenedSession(session);
		}

		List<CicilanPembayaran> hasilCicilan = new ArrayList<CicilanPembayaran>();
		if (!keyData.isEmpty()) {
			hasilCicilan = GeneralValueObject.ambilDataBanyak(CicilanPembayaran.class, new ArrayList<Long>(keyData),
					refresh);
		}

		if (refresh) {
			kegiatan.setTanggal_dirubah(WaktuUtil.getDate());
		}
		updatePembayaran(hasilCicilan, kegiatan, refresh);

		return hasilCicilan;
	}

	@SuppressWarnings("unchecked")
	public static List<CicilanPembayaran> ambilCicilan(Object student, String jsonLokasiCicilan,
			Collection<Kegiatan> kegiatansCache, JenisKegiatan jenisKegiatanData, boolean refresh) {

		if (student instanceof Mahasiswa && Boolean.TRUE.equals(((Mahasiswa) student).getTidakAdaTagihan())) {
			return new ArrayList<CicilanPembayaran>();
		}

		TreeSet<Long> keyData = new TreeSet<Long>();
		Map<Long, Kegiatan> mapKegiatanUtama = mapKegiatan(kegiatansCache);

		if (!refresh) {
			keyData.addAll(ekstrakIdDariJson(jsonLokasiCicilan));

			for (Kegiatan kegiatan : mapKegiatanUtama.values()) {
				List<Long> aktifIds = kegiatan.ambilCicilansAktifIds();
				if (aktifIds != null && !aktifIds.isEmpty()) {
					keyData.addAll(aktifIds);
				}
			}
		}

		Session session = null;
		try {
			if (refresh) {
				session = HibernateUtil.getSessionFactory().openSession();
				Criteria criteria = session.createCriteria(CicilanPembayaran.class);
				criteria.setProjection(Projections.property("id"));

				Criteria kegCrit = criteria.createCriteria("kegiatan");
				if (student instanceof Mahasiswa) {
					kegCrit.add(Restrictions.eq("mahasiswa", student));
				} else {
					kegCrit.add(Restrictions.eq("calonMahasiswa", student));
				}

				if (jenisKegiatanData != null && jenisKegiatanData.getId() != null) {
					kegCrit.add(Restrictions.eq("jenisKegiatan", jenisKegiatanData));
				}

				kegCrit.addOrder(Order.asc("semster"));
				kegCrit.addOrder(Order.asc("jenisKegiatan"));
				kegCrit.addOrder(Order.asc("id"));

				criteria.setTimeout(600);
				List<Long> dbKeys = criteria.list();
				if (dbKeys != null && !dbKeys.isEmpty()) {
					keyData.addAll(dbKeys);
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeOpenedSession(session);
		}

		List<CicilanPembayaran> hasilCicilan = new ArrayList<CicilanPembayaran>();
		if (!keyData.isEmpty()) {
			hasilCicilan = GeneralValueObject.ambilDataBanyak(CicilanPembayaran.class, new ArrayList<Long>(keyData),
					refresh);
		}

		sinkronkanCicilanPerKegiatan(hasilCicilan, mapKegiatanUtama, refresh);
		return hasilCicilan;
	}

	public static Collection<DetailKegiatan> ambilDetailKegiatan(Collection<DetailKegiatan> temp, Kegiatan kegiatan,
			boolean refresh) {
		if (kegiatan == null || kegiatan.getId() == null || temp == null) {
			return temp != null ? temp : new ArrayList<DetailKegiatan>();
		}

		List<DetailKegiatan> list = new ArrayList<DetailKegiatan>();
		for (DetailKegiatan detailKegiatan : temp) {
			if (detailKegiatan != null && detailKegiatan.getKegiatan() != null
					&& kegiatan.getId().equals(detailKegiatan.getKegiatan().getId())) {
				list.add(detailKegiatan);
			}
		}

		updateDetailKegiatan(list, kegiatan, refresh);
		return list;
	}

	private static Map<Long, Kegiatan> mapKegiatan(Collection<Kegiatan> kegiatansCache) {
		Map<Long, Kegiatan> result = new HashMap<Long, Kegiatan>();
		if (kegiatansCache == null) {
			return result;
		}
		for (Kegiatan kegiatan : kegiatansCache) {
			if (kegiatan != null && kegiatan.getId() != null) {
				result.put(kegiatan.getId(), kegiatan);
			}
		}
		return result;
	}

	private static List<Long> ekstrakIdDariJson(String jsonData) {
		List<Long> result = new ArrayList<Long>();
		if (isEmpty(jsonData)) {
			return result;
		}
		try {
			JSONObject jsonObject = new JSONObject(jsonData);
			Iterator<?> keys = jsonObject.keys();
			while (keys.hasNext()) {
				String key = (String) keys.next();
				String value = jsonObject.optString(key, "");
				if (!isEmpty(value)) {
					try {
						result.add(Long.valueOf(key.trim()));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:449");
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:453");
		}
		return result;
	}

	private static void sinkronkanDetailPerKegiatan(List<DetailKegiatan> detailKegiatans,
			Map<Long, Kegiatan> mapKegiatanUtama, boolean refresh) {
		if (detailKegiatans == null || detailKegiatans.isEmpty()) {
			if (refresh) {
				for (Kegiatan kegiatan : mapKegiatanUtama.values()) {
					kegiatan.setDetailKegiatans("");
					simpanPerubahanAsync(kegiatan, true, true);
				}
			}
			return;
		}

		Map<Long, List<DetailKegiatan>> grouped = new HashMap<Long, List<DetailKegiatan>>();
		Map<Long, Kegiatan> mapKegiatanTemp = new HashMap<Long, Kegiatan>();

		for (DetailKegiatan detailKegiatan : detailKegiatans) {
			if (detailKegiatan != null && detailKegiatan.getKegiatan() != null && detailKegiatan.getId() != null) {
				Long kId = detailKegiatan.getKegiatan().getId();
				if (!grouped.containsKey(kId)) {
					grouped.put(kId, new ArrayList<DetailKegiatan>());
					mapKegiatanTemp.put(kId, detailKegiatan.getKegiatan());
				}
				grouped.get(kId).add(detailKegiatan);
			}
		}

		for (Map.Entry<Long, List<DetailKegiatan>> entry : grouped.entrySet()) {
			Long kId = entry.getKey();
			Kegiatan target = mapKegiatanUtama.containsKey(kId) ? mapKegiatanUtama.get(kId) : mapKegiatanTemp.get(kId);
			updateDetailKegiatan(entry.getValue(), target, refresh);
			if (refresh) {
				mapKegiatanUtama.remove(kId);
			}
		}

		if (refresh) {
			for (Kegiatan kegiatan : mapKegiatanUtama.values()) {
				kegiatan.setDetailKegiatans("");
				simpanPerubahanAsync(kegiatan, true, true);
			}
		}
	}

	private static void sinkronkanCicilanPerKegiatan(List<CicilanPembayaran> cicilanPembayarans,
			Map<Long, Kegiatan> mapKegiatanUtama, boolean refresh) {
		if (cicilanPembayarans == null || cicilanPembayarans.isEmpty()) {
			if (refresh) {
				for (Kegiatan kegiatan : mapKegiatanUtama.values()) {
					updatePembayaran(new ArrayList<CicilanPembayaran>(), kegiatan, true);
				}
			}
			return;
		}

		Map<Long, List<CicilanPembayaran>> grouped = new HashMap<Long, List<CicilanPembayaran>>();
		Map<Long, Kegiatan> mapKegiatanTemp = new HashMap<Long, Kegiatan>();

		for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
			if (cicilanPembayaran != null && cicilanPembayaran.getKegiatan() != null) {
				Long kId = cicilanPembayaran.getKegiatan().getId();
				if (!grouped.containsKey(kId)) {
					grouped.put(kId, new ArrayList<CicilanPembayaran>());
					mapKegiatanTemp.put(kId, cicilanPembayaran.getKegiatan());
				}
				grouped.get(kId).add(cicilanPembayaran);
			}
		}

		for (Map.Entry<Long, List<CicilanPembayaran>> entry : grouped.entrySet()) {
			Long kId = entry.getKey();
			Kegiatan target = mapKegiatanUtama.containsKey(kId) ? mapKegiatanUtama.get(kId) : mapKegiatanTemp.get(kId);
			if (refresh && target != null) {
				target.setTanggal_dirubah(WaktuUtil.getDate());
			}
			updatePembayaran(entry.getValue(), target, refresh);
			if (refresh) {
				mapKegiatanUtama.remove(kId);
			}
		}

		if (refresh) {
			for (Kegiatan kegiatan : mapKegiatanUtama.values()) {
				updatePembayaran(new ArrayList<CicilanPembayaran>(), kegiatan, true);
			}
		}
	}

	// ========================================================================
	// 2. UPDATE STATE KEGIATAN
	// ========================================================================

	public static void updatePembayaran(List<CicilanPembayaran> listCp, Kegiatan kegiatan, boolean refresh) {
		if (kegiatan == null || kegiatan.getId() == null) {
			return;
		}
		try {
			kegiatan.setCicilans(bangunStringAktif(listCp));
			simpanPerubahanAsync(kegiatan, refresh, refresh);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public static void updatePembayaran(DetailBiaya detailBiaya, PengaturanPembayaranBulanan pengaturanBulanan,
			Kegiatan kegiatan, Double nilai) {
		if (kegiatan == null || nilai == null) {
			return;
		}
		try {
			String key = buatKeyTagihan(detailBiaya, pengaturanBulanan);
			if (key == null) {
				return;
			}

			String oldTagihans = kegiatan.getTagihans() == null ? "{}" : kegiatan.getTagihans();
			JSONObject jsonObject = new JSONObject(oldTagihans);
			jsonObject.put(key, String.valueOf(nilai.intValue()));

			String newTagihans = murnikan(kegiatan.getJenisKegiatan(), jsonObject.toString());
			if (!oldTagihans.equals(newTagihans)) {
				kegiatan.setTagihans(newTagihans);
				simpanPerubahanAsync(kegiatan, false, false);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public static void updateDetailKegiatan(List<DetailKegiatan> listDk, Kegiatan kegiatan, boolean refresh) {
		if (kegiatan == null || kegiatan.getId() == null) {
			return;
		}
		try {
			kegiatan.setDetailKegiatans(bangunStringAktif(listDk));
			simpanPerubahanAsync(kegiatan, refresh, refresh);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public static void hapusCicilan(CicilanPembayaran cicilanPembayaran, Kegiatan kegiatan) {
		if (kegiatan == null || cicilanPembayaran == null || cicilanPembayaran.getId() == null) {
			return;
		}
		kegiatan.setCicilans("," + cicilanPembayaran.getId() + ":false,");
		simpanPerubahanAsync(kegiatan, true, true);
	}

	public static void hapusDetailKegiatan(DetailKegiatan detailKegiatan, Kegiatan kegiatan) {
		if (kegiatan == null || detailKegiatan == null || detailKegiatan.getId() == null) {
			return;
		}
		kegiatan.setDetailKegiatans("," + detailKegiatan.getId() + ":false,");
		simpanPerubahanAsync(kegiatan, true, true);
	}

	@SuppressWarnings("rawtypes")
	private static String bangunStringAktif(List list) {
		StringBuilder builder = new StringBuilder(",");
		if (list != null) {
			for (Object object : list) {
				Long id = getId(object);
				if (id != null) {
					builder.append(id).append(":true,");
				}
			}
		}
		return builder.toString();
	}

	private static String buatKeyTagihan(DetailBiaya detailBiaya, PengaturanPembayaranBulanan pengaturanBulanan) {
		if (pengaturanBulanan != null && pengaturanBulanan.getDetailBiaya() != null
				&& pengaturanBulanan.getDetailBiaya().getItemBiaya() != null
				&& pengaturanBulanan.getDetailBiaya().getItemBiaya().getId() != null) {
			String key = pengaturanBulanan.getDetailBiaya().getItemBiaya().getId().toString();
			if (pengaturanBulanan.getRealBulan() != null) {
				key += "_" + pengaturanBulanan.getRealBulan();
			}
			return key;
		}
		if (detailBiaya != null && detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getId() != null) {
			return detailBiaya.getItemBiaya().getId().toString();
		}
		return null;
	}

	private static String buatKeyTagihan(DetailKegiatan detail) {
		if (detail == null || detail.getItemBiaya() == null || detail.getItemBiaya().getId() == null) {
			return null;
		}
		String key = detail.getItemBiaya().getId().toString();
		if (detail.getPengaturanPembayaranBulanan() != null
				&& detail.getPengaturanPembayaranBulanan().getRealBulan() != null) {
			key += "_" + detail.getPengaturanPembayaranBulanan().getRealBulan();
		}
		return key;
	}

	private static long nominalTagihan(Double nilai) {
		if (nilai == null) {
			return 0L;
		}
		return Math.round(nilai.doubleValue());
	}

	private static double ambilDiskonTerbesar(Map<String, Double> diskonTerbesarPerKey, String key) {
		if (diskonTerbesarPerKey == null || key == null) {
			return 0.0;
		}
		Double diskon = diskonTerbesarPerKey.get(key);
		return diskon == null ? 0.0 : diskon.doubleValue();
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Double> kumpulkanDiskonTerbesarPerKey(Kegiatan kegiatan) {
		Map<String, Double> hasil = new HashMap<String, Double>();
		if (kegiatan == null || kegiatan.getId() == null) {
			return hasil;
		}

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			List<DetailKegiatan> semuaDetail = session.createCriteria(DetailKegiatan.class)
					.add(Restrictions.eq("kegiatan", kegiatan)).list();
			if (semuaDetail == null) {
				return hasil;
			}

			for (DetailKegiatan detail : semuaDetail) {
				String key = buatKeyTagihan(detail);
				if (key == null) {
					continue;
				}
				double diskon = detail.getDiskon() == null ? 0.0 : detail.getDiskon().doubleValue();
				Double tersimpan = hasil.get(key);
				if (tersimpan == null || diskon > tersimpan.doubleValue()) {
					hasil.put(key, Double.valueOf(diskon));
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeOpenedSession(session);
		}
		return hasil;
	}

	private static Double normalisasiNilaiDiskon(String key, DetailKegiatan detail, Double nilai,
			Map<String, Double> diskonTerbesarPerKey) {
		if (nilai == null) {
			return Double.valueOf(0.0);
		}
		double hasil = nilai.doubleValue();
		double diskonTerbesar = ambilDiskonTerbesar(diskonTerbesarPerKey, key);
		if (diskonTerbesar > 0.0) {
			double diskonDetail = detail == null || detail.getDiskon() == null ? 0.0 : detail.getDiskon().doubleValue();
			if (diskonTerbesar > diskonDetail) {
				hasil -= (diskonTerbesar - diskonDetail);
				if (hasil < 0.0) {
					hasil = 0.0;
				}
			}
		}
		return Double.valueOf(hasil);
	}

	private static void putTagihanStabil(JSONObject jsonTagihan, String key, Double nilai,
			Map<String, Double> diskonTerbesarPerKey) throws JSONException {
		long nilaiBaru = nominalTagihan(nilai);
		if (!jsonTagihan.has(key)) {
			jsonTagihan.put(key, String.valueOf(nilaiBaru));
			return;
		}

		long nilaiLama = 0L;
		try {
			nilaiLama = Long.parseLong(jsonTagihan.optString(key, "0"));
		} catch (Exception e) {
			nilaiLama = 0L;
		}

		if (ambilDiskonTerbesar(diskonTerbesarPerKey, key) > 0.0 && nilaiLama >= 0L && nilaiBaru >= 0L) {
			jsonTagihan.put(key, String.valueOf(Math.min(nilaiLama, nilaiBaru)));
		} else {
			jsonTagihan.put(key, String.valueOf(nilaiLama + nilaiBaru));
		}
	}

	// ========================================================================
	// 3. PERSISTENCE ASYNC
	// ========================================================================

	private static void simpanPerubahanAsync(final Kegiatan kegiatan, boolean refreshOrDelete, boolean immediateUpdate) {
		if (kegiatan == null || kegiatan.getId() == null) {
			return;
		}

		final Long id = kegiatan.getId();

		if (immediateUpdate) {
			synchronized (pendingTasks) {
				PendingKegiatanData pending = pendingTasks.remove(id);
				if (pending != null && pending.future != null) {
					pending.future.cancel(false);
				}
			}
			eksekusiUpdateDenganRetry(kegiatan, kegiatan.getCicilans(), kegiatan.getDetailKegiatans());
			return;
		}

		synchronized (pendingTasks) {
			PendingKegiatanData pending = pendingTasks.get(id);
			if (pending != null && pending.future != null && !pending.future.isDone()) {
				pending.cicilans = kegiatan.getCicilans();
				pending.detailKegiatans = kegiatan.getDetailKegiatans();
				pending.kegiatan = kegiatan;
				return;
			}

			final PendingKegiatanData data = new PendingKegiatanData(kegiatan.getCicilans(),
					kegiatan.getDetailKegiatans(), kegiatan);
			data.future = asyncExecutor.schedule(new Runnable() {
				@Override
				public void run() {
					PendingKegiatanData latest = pendingTasks.remove(id);
					if (latest != null) {
						eksekusiUpdateDenganRetry(latest.kegiatan, latest.cicilans, latest.detailKegiatans);
					}
				}
			}, ASYNC_DELAY_SECONDS, TimeUnit.SECONDS);

			pendingTasks.put(id, data);
		}
	}

	private static void eksekusiUpdateDenganRetry(Kegiatan kegiatan, String cicilansBaru, String detailKegiatansBaru) {
		if (kegiatan == null || kegiatan.getId() == null) {
			return;
		}

		Long idKegiatan = kegiatan.getId();
		Object lock = getKegiatanLock(idKegiatan);
		synchronized (lock) {
			eksekusiUpdateDenganRetryTerkunci(kegiatan, cicilansBaru, detailKegiatansBaru);
		}
	}

	private static Object[] buatKegiatanLocks() {
		// Bulk sinkronisasi pembayaran dapat memproses ribuan kegiatan bersamaan. Dengan 64
		// stripe, kegiatan yang berbeda tetapi memiliki id modulo sama ikut terserialisasi dan
		// membentuk lock convoy panjang. 1024 stripe tetap sangat kecil di memori, tetapi jauh
		// menurunkan tabrakan palsu; kegiatan dengan id yang sama tetap aman/serial.
		Object[] locks = new Object[1024];
		for (int i = 0; i < locks.length; i++) {
			locks[i] = new Object();
		}
		return locks;
	}

	private static Object getKegiatanLock(Long idKegiatan) {
		long value = idKegiatan == null ? 0L : idKegiatan.longValue();
		if (value < 0L) {
			value = 0L - value;
		}
		return kegiatanLocks[(int) (value % kegiatanLocks.length)];
	}

	private static void eksekusiUpdateDenganRetryTerkunci(Kegiatan kegiatan, String cicilansBaru,
			String detailKegiatansBaru) {
		Long idKegiatan = kegiatan.getId();
		int attempt = 0;
		boolean success = false;

		while (attempt < MAX_RETRY && !success) {
			attempt++;
			Session session = null;
			Transaction tx = null;
			try {
				session = HibernateUtil.getSessionFactory().openSession();
				session.setFlushMode(FlushMode.MANUAL);

				Kegiatan kegiatanDb = (Kegiatan) session.get(Kegiatan.class, idKegiatan);
				if (kegiatanDb == null) {
					return;
				}

				List<Long> aktifCicilan = ekstrakIdAktif(cicilansBaru);
				List<Long> aktifDetail = ekstrakIdAktif(detailKegiatansBaru);

				List<CicilanPembayaran> listCicilan = loadCicilanByIds(session, aktifCicilan);
				List<DetailKegiatan> listDetail = loadDetailKegiatanByIds(session, aktifDetail);

				RekapPembayaran rekapPembayaran = bangunRekapPembayaran(listCicilan);
					// Worker persistence hanya merangkum snapshot DetailKegiatan yang sudah ada.
					// Jangan dari sini memanggil getDetailBiayaMahasiswa untuk validasi item asing:
					// helper tersebut dapat menyinkronkan KRS/Mahasiswa lagi dan menimbulkan rantai
					// rekursif (FK KRS, NIM ganda, serta koneksi tertutup) di thread async.
					String tagihansTerbaru = bangunRekapTagihan(kegiatanDb, listDetail, false, false);

				kegiatanDb.setBulans(rekapPembayaran.bulans);
				kegiatanDb.setTagihans(tagihansTerbaru);
				kegiatanDb.setCicilans(cicilansBaru);
				kegiatanDb.setDetailKegiatans(detailKegiatansBaru);

				Double tagihan = kegiatanDb.hitungTagihan();
				Double dibayar = kegiatanDb.hitungDibayar();
				Double persentase = (tagihan != null && tagihan.doubleValue() > 0.0 && dibayar != null)
						? Double.valueOf((dibayar.doubleValue() * 100.0) / tagihan.doubleValue())
						: Double.valueOf(0.0);

				kegiatanDb.setTagihan(tagihan);
				kegiatanDb.setDibayar(dibayar);
				kegiatanDb.setPersentase(persentase);

				kegiatan.setBulans(kegiatanDb.getBulans());
				kegiatan.setTagihans(kegiatanDb.getTagihans());
				kegiatan.setTagihan(kegiatanDb.getTagihan());
				kegiatan.setDibayar(kegiatanDb.getDibayar());
				kegiatan.setPersentase(kegiatanDb.getPersentase());
				kegiatan.setCicilans(cicilansBaru);
				kegiatan.setDetailKegiatans(detailKegiatansBaru);

				if (databaseSudahSama(session, idKegiatan, kegiatanDb, cicilansBaru, detailKegiatansBaru)) {
					success = true;
					break;
				}

					tx = session.beginTransaction();
					// Query ini sering menunggu lock saat sinkronisasi pembayaran massal.
				// Query.setTimeout(45) memakai Statement.cancel(), yang oleh PostgreSQL
				// dilaporkan sebagai "canceling statement due to user request". Gunakan
				// timeout transaksi server yang lebih longgar; retry/backoff di method ini
				// tetap menjadi pengaman bila kontensi benar-benar berkepanjangan.
					session.createSQLQuery("SET LOCAL statement_timeout = '300s'").executeUpdate();
					// Banyak instalasi menetapkan lock_timeout global sangat pendek. Override hanya
					// untuk transaksi worker ini, lalu serialkan per kegiatan juga lintas node JVM.
					// Advisory lock dilepas otomatis saat commit/rollback.
					session.createSQLQuery("SET LOCAL lock_timeout = '120s'").executeUpdate();
					// pg_advisory_xact_lock mengembalikan pseudo-type PostgreSQL void
					// (JDBC Types.OTHER/1111). Hibernate 3 gagal melakukan auto-discovery
					// terhadap tipe tersebut. Bungkus pemanggilan lock dalam CTE dan
					// kembalikan scalar INTEGER yang tipenya ditentukan secara eksplisit.
					session.createSQLQuery("WITH lock_guard AS (SELECT pg_advisory_xact_lock(:lockKey)) "
							+ "SELECT 1 AS lock_acquired FROM lock_guard")
							.addScalar("lock_acquired", org.hibernate.Hibernate.INTEGER)
							.setParameter("lockKey", Long.valueOf(4200000000000L + idKegiatan.longValue()))
							.uniqueResult();
				Query query = session.createQuery(HQL_UPDATE_KEGIATAN);
				query.setParameter("nilaiBaru", kegiatanDb.getBulans());
				query.setParameter("nilaiTagihanBaru", kegiatanDb.getTagihans());
				query.setParameter("tagihanBaru", kegiatanDb.getTagihan());
				query.setParameter("dibayarBaru", kegiatanDb.getDibayar());
				query.setParameter("persentaseBaru", kegiatanDb.getPersentase());
				query.setParameter("cicilansBaru", cicilansBaru);
				query.setParameter("detailKegiatansBaru", detailKegiatansBaru);
				query.setParameter("idKegiatan", idKegiatan);
				query.executeUpdate();

				tx.commit();
				success = true;
			} catch (Exception e) {
				rollbackQuietly(tx);
				if (attempt >= MAX_RETRY) {
					Common.tampilErrorJikaAdmin(e);
				} else {
					try {
						// Backoff diperpanjang: beri waktu lebih agar lock (kegiatan/detail_biaya)
						// terlepas sebelum percobaan berikut (kontensi saat singkronkanDenganPembayaran).
						Thread.sleep(Math.min(750L * attempt, 4000L) + (long) (Math.random() * 500L));
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:903");
					}
				}
			} finally {
				closeOpenedSession(session);
			}
		}
	}

	private static boolean databaseSudahSama(Session session, Long idKegiatan, Kegiatan kegiatan, String cicilansBaru,
			String detailKegiatansBaru) {
		try {
			String hqlCek = "SELECT bulans, tagihans, tagihan, dibayar, persentase, cicilans, detailKegiatans "
					+ "FROM Kegiatan WHERE id = :id";
			Object[] current = (Object[]) session.createQuery(hqlCek).setParameter("id", idKegiatan).uniqueResult();
			if (current == null) {
				return false;
			}
			return isSama(current[0], kegiatan.getBulans()) && isSama(current[1], kegiatan.getTagihans())
					&& isSama(current[2], kegiatan.getTagihan()) && isSama(current[3], kegiatan.getDibayar())
					&& isSama(current[4], kegiatan.getPersentase()) && isSama(current[5], cicilansBaru)
					&& isSama(current[6], detailKegiatansBaru);
		} catch (Exception e) {
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	private static List<CicilanPembayaran> loadCicilanByIds(Session session, List<Long> ids) {
		List<CicilanPembayaran> result = new ArrayList<CicilanPembayaran>();
		if (session == null || ids == null || ids.isEmpty()) {
			return result;
		}
		for (int start = 0; start < ids.size(); start += MAX_IN_CLAUSE_SIZE) {
			int end = Math.min(start + MAX_IN_CLAUSE_SIZE, ids.size());
			List<Long> chunk = ids.subList(start, end);
			result.addAll(session.createCriteria(CicilanPembayaran.class).add(Restrictions.in("id", chunk)).list());
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private static List<DetailKegiatan> loadDetailKegiatanByIds(Session session, List<Long> ids) {
		List<DetailKegiatan> result = new ArrayList<DetailKegiatan>();
		if (session == null || ids == null || ids.isEmpty()) {
			return result;
		}
		for (int start = 0; start < ids.size(); start += MAX_IN_CLAUSE_SIZE) {
			int end = Math.min(start + MAX_IN_CLAUSE_SIZE, ids.size());
			List<Long> chunk = ids.subList(start, end);
			result.addAll(session.createCriteria(DetailKegiatan.class).add(Restrictions.in("id", chunk)).list());
		}
		return result;
	}

	private static RekapPembayaran bangunRekapPembayaran(List<CicilanPembayaran> listCicilan) throws JSONException { 
		RekapPembayaran rekap = new RekapPembayaran();
		JSONObject jsonBulans = new JSONObject();
		double totalDibayar = 0.0;

		if (listCicilan != null) {
			for (CicilanPembayaran cicilan : listCicilan) {
				if (cicilan == null || cicilan.getItemBiaya() == null || cicilan.getTanggal() == null
						|| cicilan.getId() == null) {
					continue;
				}

				String realBulan = "0";
				if (cicilan.getPengaturanPembayaranBulanan() != null
						&& cicilan.getPengaturanPembayaranBulanan().getRealBulan() != null) {
					realBulan = String.valueOf(cicilan.getPengaturanPembayaranBulanan().getRealBulan());
				}

				String key = cicilan.getItemBiaya().getId() + "_" + realBulan + "_"
						+ Common.dateFormat84.get().format(cicilan.getTanggal()) + "-" + cicilan.getId();

				Double nilai = safeDouble(cicilan.getNilai());
				if (ItemBiaya.DIKALI_NILAI_MINUS.equals(cicilan.getItemBiaya().getPenghitungan())) {
					nilai = Double.valueOf(-Math.abs(nilai.doubleValue()));
				}

				jsonBulans.put(key, String.valueOf(safeLong(nilai)));
				totalDibayar += nilai.doubleValue();
			}
		}

		rekap.bulans = jsonBulans.toString();
		rekap.dibayar = Double.valueOf(totalDibayar);
		return rekap;
	}
 
	// ============================================================
	// SELF-HEALING TAGIHAN: cegah item ASING (mis. item prodi lain yang nyangkut, atau item
	// yang sudah tidak berlaku) ikut menggelembungkan tagihan. Sebuah item DIKECUALIKAN dari
	// perhitungan HANYA bila: (1) tidak ada di daftar biaya yang berlaku untuk mahasiswa ini
	// (getDetailBiayaMahasiswa) DAN (2) belum ada pembayaran sama sekali. Item yang sudah
	// dibayar TIDAK PERNAH disentuh. Dapat dimatikan via konfigurasi 'tagihan_buang_item_asing'.
	// ============================================================
	private static boolean buangItemAsingAktif() {
		try {
			return ais.database.model.Konfigurasi.AKTIF.equals(Common
				.getKonfigurasi("tagihan_buang_item_asing", ais.database.model.Konfigurasi.AKTIF).getNilai());
		} catch (Exception e) {
			return false;
		}
	}

	/** Id ItemBiaya yang BERLAKU utk mahasiswa kegiatan ini (incl. bulanan). null bila tak bisa ditentukan/kosong. */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static java.util.Set<Long> kumpulkanItemBiayaBerlaku(Kegiatan kegiatan) {
		try {
			if (kegiatan == null || kegiatan.getMahasiswa() == null || kegiatan.getMahasiswa().getId() == null
				|| kegiatan.getJenisKegiatan() == null || kegiatan.getSemster() == null) {
				return null;
			}
			java.util.Collection biayas = ais.action.master.helper.PembayaranUtilHelper.getDetailBiayaMahasiswa(
				kegiatan.getMahasiswa(), kegiatan.getSemster(), kegiatan.getJenisKegiatan(), false);
			boolean adaBulanan = false;
			if (biayas != null) {
				for (Object o : biayas) {
					if (o instanceof PengaturanPembayaranBulanan) {
						adaBulanan = true;
						break;
					}
				}
			}
			if (adaBulanan) {
				biayas = ais.action.master.helper.PembayaranUtilHelper.getDetailBiayaMahasiswa(
					kegiatan.getMahasiswa(), kegiatan.getSemster(), kegiatan.getJenisKegiatan(), "-1", false);
			}
			java.util.Set<Long> out = new java.util.HashSet<Long>();
			if (biayas != null) {
				for (Object o : biayas) {
					ItemBiaya ib = null;
					if (o instanceof DetailBiaya) {
						ib = ((DetailBiaya) o).getItemBiaya();
					} else if (o instanceof PengaturanPembayaranBulanan) {
						DetailBiaya db = ((PengaturanPembayaranBulanan) o).getDetailBiaya();
						ib = db == null ? null : db.getItemBiaya();
					}
					if (ib != null && ib.getId() != null) {
						out.add(ib.getId());
					}
				}
			}
			return out.isEmpty() ? null : out;
		} catch (Exception e) {
			return null;
		}
	}

	/** Id ItemBiaya yang SUDAH ada pembayaran (cicilan nilai>0) pada kegiatan ini. */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static java.util.Set<Long> kumpulkanItemBiayaAdaPembayaran(Kegiatan kegiatan) {
		java.util.Set<Long> out = new java.util.HashSet<Long>();
		if (kegiatan == null || kegiatan.getId() == null) {
			return out;
		}
		Session s = null;
		try {
			s = HibernateUtil.getSessionFactory().openSession();
			java.util.List rows = s.createQuery(
				"select distinct c.itemBiaya.id from CicilanPembayaran c where c.kegiatan.id = :kid "
					+ "and c.nilai is not null and c.nilai > 0 and c.itemBiaya is not null")
				.setParameter("kid", kegiatan.getId()).list();
			if (rows != null) {
				for (Object o : rows) {
					if (o instanceof Number) {
						out.add(((Number) o).longValue());
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:1075");
		} finally {
			try {
				if (s != null && s.isOpen()) {
					s.close();
				}
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:1081");
			}
		}
		return out;
	}

	/** true bila DetailKegiatan ASING: item tak ada di biaya berlaku DAN belum ada pembayaran. */
	public static boolean detailKegiatanAsingTakDihitung(DetailKegiatan detail, java.util.Set<Long> itemValid,
			java.util.Set<Long> itemAdaBayar) {
		try {
			if (detail == null || detail.getItemBiaya() == null || detail.getItemBiaya().getId() == null) {
				return false;
			}
			if (itemValid == null) {
				return false;
			}
			Long ib = detail.getItemBiaya().getId();
			if (itemValid.contains(ib)) {
				return false;
			}
			if (itemAdaBayar != null && itemAdaBayar.contains(ib)) {
				return false;
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/** Daftar DetailKegiatan ASING pada kegiatan (utk pembersihan manual / tombol Bersihkan). */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static java.util.List<DetailKegiatan> cariDetailKegiatanAsing(Kegiatan kegiatan) {
		java.util.List<DetailKegiatan> out = new java.util.ArrayList<DetailKegiatan>();
		try {
			if (kegiatan == null || kegiatan.getId() == null) {
				return out;
			}
			java.util.Set<Long> valid = kumpulkanItemBiayaBerlaku(kegiatan);
			if (valid == null) {
				return out;
			}
			java.util.Set<Long> bayar = kumpulkanItemBiayaAdaPembayaran(kegiatan);
			java.util.Collection dks = kegiatan.ambilDetailKegiatan();
			if (dks != null) {
				for (Object o : dks) {
					if (o instanceof DetailKegiatan && detailKegiatanAsingTakDihitung((DetailKegiatan) o, valid, bayar)) {
						out.add((DetailKegiatan) o);
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:1131");
		}
		return out;
	}

	/**
	 * Hapus DetailKegiatan ASING (belum dibayar & tidak sesuai Setting Biaya yang berlaku) pada SATU
	 * kegiatan. Dipakai bersama oleh tombol "Bersihkan Item Tak Sesuai" per-mahasiswa
	 * (InformasiPembayaranMahasiswaAction) MAUPUN proses massal (KegiatanProsesHeper.prosesUlangTagihan,
	 * opsi "Bersihkan Item Tak Sesuai (Massal)") -- satu logika pembersihan yang sama, tidak diduplikasi.
	 * Session/transaksi dibuka & ditutup sendiri di sini (aman dipanggil dari thread worker paralel).
	 * @return jumlah baris DetailKegiatan yang benar-benar terhapus (0 bila tak ada/gagal).
	 */
	public static int bersihkanItemAsing(Kegiatan kegiatan) {
		if (kegiatan == null || kegiatan.getId() == null) {
			return 0;
		}
		java.util.List<DetailKegiatan> asing = cariDetailKegiatanAsing(kegiatan);
		if (asing.isEmpty()) {
			return 0;
		}
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			int jumlah = 0;
			for (DetailKegiatan dk : asing) {
				if (dk == null || dk.getId() == null) {
					continue;
				}
				DetailKegiatan dkdb = (DetailKegiatan) session.get(DetailKegiatan.class, dk.getId());
				if (dkdb != null) {
					session.delete(dkdb);
					jumlah++;
				}
			}
			tx.commit();
			return jumlah;
		} catch (Exception e) {
			try {
				if (tx != null && tx.isActive()) {
					tx.rollback();
				}
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:bersihkanItemAsing:rollback"); }
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(bersihkan-massal-gagal) src/ais/action/master/helper/KegiatanPersistenceHelper.java:bersihkanItemAsing kegiatanId="
							+ kegiatan.getId());
			return 0;
		} finally {
			try {
				if (session != null) {
					session.close();
				}
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:bersihkanItemAsing:close"); }
		}
	}

	private static String bangunRekapTagihan(Kegiatan kegiatan, List<DetailKegiatan> listDetail) throws JSONException {
		return bangunRekapTagihan(kegiatan, listDetail, false);
	}

	/**
	 * @param live bila {@code true}, item biaya ber-rumus KRS (UTS/UAS/SKS/Matakuliah)
	 *             yang nilainya tidak diinput manual dihitung ulang mengikuti KRS terkini
	 *             (sama dengan yang tampil di grid layar). Dipakai saat mencetak bukti agar
	 *             tagihan tidak basi. Jalur async tetap memakai {@code false} (perilaku lama).
	 */
	private static String bangunRekapTagihan(Kegiatan kegiatan, List<DetailKegiatan> listDetail, boolean live)
			throws JSONException {
		return bangunRekapTagihan(kegiatan, listDetail, live, true);
	}

	private static String bangunRekapTagihan(Kegiatan kegiatan, List<DetailKegiatan> listDetail, boolean live,
			boolean validasiItemAsing) throws JSONException {
		JSONObject jsonTagihan = new JSONObject();
		Map<String, Double> diskonTerbesarPerKey = kumpulkanDiskonTerbesarPerKey(kegiatan);
		java.util.Set<Long> itemValidBerlaku = null;
		java.util.Set<Long> itemSudahAdaBayar = null;
		if (validasiItemAsing && buangItemAsingAktif()) {
			itemValidBerlaku = kumpulkanItemBiayaBerlaku(kegiatan);
			if (itemValidBerlaku != null) {
				itemSudahAdaBayar = kumpulkanItemBiayaAdaPembayaran(kegiatan);
			}
		}
		boolean kegiIsAngsuran = kegiatan != null && kegiatan.getJenisKegiatan() != null
				&& Boolean.TRUE.equals(kegiatan.getJenisKegiatan().getHanyaBerupaAngsuran());
		if (listDetail != null) {
			for (DetailKegiatan detail : listDetail) {
				if (detail == null || detail.getItemBiaya() == null) {
					continue;
				}

				if (detailKegiatanAsingTakDihitung(detail, itemValidBerlaku, itemSudahAdaBayar)) {
					try {
						System.out.println("[bangunRekapTagihan] Item ASING dilewati (tak berlaku & belum dibayar) kegiatan="
							+ (kegiatan == null ? "?" : String.valueOf(kegiatan.getId())) + " item="
							+ detail.getItemBiaya().getId() + " " + detail.getItemBiaya().getNama());
					} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:1171");
					}
					continue;
				}

				// Mode angsuran: lewati DK tanpa PPB agar tidak menambah key non-"_" ke tagihans
				if (kegiIsAngsuran && detail.getPengaturanPembayaranBulanan() == null) {
					continue;
				}

				String key = buatKeyTagihan(detail);
				if (key == null) {
					continue;
				}

				Double nilai = hitungJumlahTagihan(kegiatan, detail, live);
				nilai = normalisasiNilaiDiskon(key, detail, nilai, diskonTerbesarPerKey);
				if (ItemBiaya.DIKALI_NILAI_MINUS.equals(detail.getItemBiaya().getPenghitungan())) {
					nilai = Double.valueOf(-Math.abs(nilai.doubleValue()));
				}

				putTagihanStabil(jsonTagihan, key, nilai, diskonTerbesarPerKey);
			}
		}
		String hasilTagihan = murnikan(kegiatan == null ? null : kegiatan.getJenisKegiatan(), jsonTagihan.toString());
		hasilTagihan = lindungiNilaiDiskonTersimpan(kegiatan, hasilTagihan, diskonTerbesarPerKey);

		// LINDUNGI dari hasil KOSONG yang TRANSIEN. Akar masalah "tagihan dasbor BOLAK-BALIK
		// BENAR/SALAH": saat "Hitung Ulang"/recompute, engine menonaktifkan DetailKegiatan lama
		// (DetailKegiatan.getKodeUnik() mengembalikan null saat aktif=false) lalu membuat/meng-
		// aktifkan DK baru. Pada jendela transisi itu, daftar DK AKTIF (sumber listDetail) bisa
		// SEMENTARA KOSONG -> map tagihan terbangun {} -> menimpa nilai tersimpan yang BENAR
		// (mis. {"199":"1800000",...}) -> dasbor menampilkan BRUTO. Klik berikutnya benar lagi.
		// Bila rebuild menghasilkan KOSONG padahal nilai 'tagihans' TERSIMPAN tidak kosong,
		// JANGAN timpa dengan kosong -> pertahankan nilai tersimpan yang valid (idempoten/stabil).
		if (hasilTagihan == null || hasilTagihan.trim().isEmpty() || hasilTagihan.trim().equals("{}")) {
			String tersimpan = kegiatan == null ? null : kegiatan.getTagihans();
			if (tersimpan != null && !tersimpan.trim().isEmpty() && !tersimpan.trim().equals("{}")) {
				return tersimpan;
			}
		}
		return hasilTagihan;
	}

	private static String lindungiNilaiDiskonTersimpan(Kegiatan kegiatan, String hasilTagihan,
			Map<String, Double> diskonTerbesarPerKey) {
		if (kegiatan == null || diskonTerbesarPerKey == null || diskonTerbesarPerKey.isEmpty()
				|| hasilTagihan == null || hasilTagihan.trim().length() == 0 || "{}".equals(hasilTagihan.trim())) {
			return hasilTagihan;
		}

		String tersimpan = kegiatan.getTagihans();
		if (tersimpan == null || tersimpan.trim().length() == 0 || "{}".equals(tersimpan.trim())) {
			return hasilTagihan;
		}

		try {
			JSONObject jsonBaru = new JSONObject(hasilTagihan);
			JSONObject jsonLama = new JSONObject(tersimpan);
			Iterator<?> iterator = jsonLama.keys();
			boolean berubah = false;
			while (iterator.hasNext()) {
				String key = (String) iterator.next();
				if (ambilDiskonTerbesar(diskonTerbesarPerKey, key) <= 0.0 || !jsonBaru.has(key)) {
					continue;
				}
				long nilaiLama = Long.parseLong(jsonLama.optString(key, "0"));
				long nilaiBaru = Long.parseLong(jsonBaru.optString(key, "0"));
				if (nilaiLama >= 0L && nilaiBaru > nilaiLama) {
					jsonBaru.put(key, String.valueOf(nilaiLama));
					berubah = true;
				}
			}
			return berubah ? jsonBaru.toString() : hasilTagihan;
		} catch (Exception e) {
			return hasilTagihan;
		}
	}

	private static Double hitungJumlahTagihan(Kegiatan kegiatan, DetailKegiatan detail) {
		return hitungJumlahTagihan(kegiatan, detail, false);
	}

	private static Double hitungJumlahTagihan(Kegiatan kegiatan, DetailKegiatan detail, boolean live) {
		if (detail == null) {
			return Double.valueOf(0.0);
		}
		try {
			if (detail.getPengaturanPembayaranBulanan() == null) {
				DetailBiaya detailBiaya = detail.getDetailBiaya();
				if (live && bolehHitungUlangLive(detailBiaya)) {
					// Sama dengan tampilan grid layar: segarkan nilaiBiayaBaru lalu pakai
					// resolusi tanpa detailKegiatan (memakai hitungan rumus, bukan biaya beku).
					segarkanNilaiBiayaBaru(kegiatan, detailBiaya);
					return Kegiatan.ambilJumlahTagihan((DetailKegiatan) null, kegiatan, detailBiaya, false);
				}
				return Kegiatan.ambilJumlahTagihan(detail, kegiatan, detailBiaya, false);
			}
			return Kegiatan.ambilJumlahTagihan(detail, detail.getDetailBiaya(), kegiatan,
					kegiatan == null ? null : kegiatan.getMahasiswa(),
					kegiatan == null ? null : kegiatan.getSemster(), detail.getPengaturanPembayaranBulanan());
		} catch (Exception e) {
			return Double.valueOf(0.0);
		}
	}

	/**
	 * Item biaya yang boleh dihitung ulang LIVE mengikuti KRS terkini: item ber-rumus
	 * (UTS/UAS/SKS/Matakuliah) yang nilainya TIDAK diinput manual ({@code nilaiBisaDiubah=false}).
	 * Item tanpa penghitungan (flat), tunggakan semester lalu, dan item yang nilainya
	 * bisa diubah manual TIDAK ikut dihitung ulang agar nilai manual/diskon tidak hilang.
	 */
	private static boolean bolehHitungUlangLive(DetailBiaya detailBiaya) {
		try {
			if (detailBiaya == null || detailBiaya.getItemBiaya() == null) {
				return false;
			}
			ItemBiaya itemBiaya = detailBiaya.getItemBiaya();
			if (Boolean.TRUE.equals(itemBiaya.getNilaiBisaDiubah())) {
				return false;
			}
			if (PembayaranNominalModifikasiHelper.isTanpaPenghitungan(itemBiaya)) {
				return false;
			}
			String penghitungan = itemBiaya.getPenghitungan();
			if (ItemBiaya.HITUNG_TUNGGAKAN_SMT_LALU.equals(penghitungan)) {
				return false;
			}
			if (itemBiaya.getParameterTambahan() != null) {
				return false;
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/** Hitung ulang nilaiBiayaBaru detailBiaya mengikuti KRS terkini (idempoten, aman dipanggil ulang). */
	private static void segarkanNilaiBiayaBaru(Kegiatan kegiatan, DetailBiaya detailBiaya) {
		try {
			if (kegiatan == null || detailBiaya == null) {
				return;
			}
			Mahasiswa mahasiswa = kegiatan.getMahasiswa();
			Integer semester = kegiatan.getSemster();
			if (mahasiswa == null || mahasiswa.getId() == null || semester == null) {
				return;
			}
			detailBiaya.updateKeterangan(mahasiswa, semester);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Hitung ulang tagihan kegiatan secara LIVE (mengikuti KRS terkini, sama dengan grid
	 * layar) lalu simpan ke kolom denormalisasi yang dibaca laporan bukti pembayaran:
	 * {@code tagihans, tagihan, dibayar, persentase, amount, amountTerhutang}. Dipakai
	 * sebelum mencetak agar JUMLAH TAGIHAN pada PDF sama dengan layar.
	 *
	 * <p>Hanya item ber-rumus KRS non-manual yang ikut dihitung ulang (lihat
	 * {@link #bolehHitungUlangLive}); item flat/manual/tunggakan tidak berubah. Aman
	 * dipanggil dari thread web (session terkelola). Mengembalikan {@code true} bila berhasil.</p>
	 */
	public static boolean segarkanTagihanLive(Long kegiatanId) {
		if (kegiatanId == null) {
			return false;
		}
		Object lock = getKegiatanLock(kegiatanId);
		synchronized (lock) {
			Session session = null;
			Transaction tx = null;
			try {
				session = HibernateUtil.getSessionFactory().openSession();
				session.setFlushMode(FlushMode.MANUAL);

				Kegiatan kegiatan = (Kegiatan) session.get(Kegiatan.class, kegiatanId);
				if (kegiatan == null) {
					return false;
				}

				List<Long> aktifDetail = ekstrakIdAktif(kegiatan.getDetailKegiatans());
				List<DetailKegiatan> listDetail = loadDetailKegiatanByIds(session, aktifDetail);

				String tagihansTerbaru = bangunRekapTagihan(kegiatan, listDetail, true);
				kegiatan.setTagihans(tagihansTerbaru);

				Double tagihan = kegiatan.hitungTagihan();
				if (tagihan == null) {
					tagihan = Double.valueOf(0.0);
				}
				Double dibayar = kegiatan.getDibayar();
				if (dibayar == null) {
					dibayar = Double.valueOf(0.0);
				}
				Double sisa = Double.valueOf(tagihan.doubleValue() - dibayar.doubleValue());
				if (sisa.doubleValue() < 0.0) {
					sisa = Double.valueOf(0.0);
				}
				Double persentase = (tagihan.doubleValue() > 0.0)
						? Double.valueOf((dibayar.doubleValue() * 100.0) / tagihan.doubleValue())
						: Double.valueOf(0.0);

				tx = session.beginTransaction();
				Query query = session.createQuery("UPDATE Kegiatan SET tagihans = :tagihans, tagihan = :tagihan, "
						+ "persentase = :persentase, amount = :amount, amountTerhutang = :amountTerhutang "
						+ "WHERE id = :idKegiatan");
				query.setParameter("tagihans", tagihansTerbaru);
				query.setParameter("tagihan", tagihan);
				query.setParameter("persentase", persentase);
				query.setParameter("amount", dibayar);
				query.setParameter("amountTerhutang", sisa);
				query.setParameter("idKegiatan", kegiatanId);
				query.executeUpdate();
				tx.commit();
				return true;
			} catch (Exception e) {
				rollbackQuietly(tx);
				Common.tampilErrorJikaAdmin(e);
				return false;
			} finally {
				closeOpenedSession(session);
			}
		}
	}

	// ========================================================================
	// 4. REUSE UNTUK KegiatanProsesHeper.singkronkanDataCicilan
	// ========================================================================

	@SuppressWarnings("unchecked")
	public static Map<Long, List<Long>> ambilPetaCicilanPerKegiatan() {
		Map<Long, List<Long>> result = new HashMap<Long, List<Long>>();
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Query query = session.createSQLQuery(
					"SELECT id, kegiatan FROM cicilan_pembayaran WHERE kegiatan IS NOT NULL ORDER BY kegiatan DESC");
			query.setFetchSize(1000);

			List<Object[]> rows = query.list();
			if (rows == null) {
				return result;
			}

			for (Object[] row : rows) {
				if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
					continue;
				}
				Long cicilanId = Long.valueOf(((Number) row[0]).longValue());
				Long kegiatanId = Long.valueOf(((Number) row[1]).longValue());
				List<Long> ids = result.get(kegiatanId);
				if (ids == null) {
					ids = new ArrayList<Long>();
					result.put(kegiatanId, ids);
				}
				ids.add(cicilanId);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeOpenedSession(session);
		}
		return result;
	}

	public static boolean sinkronkanCicilanKegiatanLangsung(Long kegiatanId, List<Long> cicilanIds) {
		if (kegiatanId == null) {
			return false;
		}

		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			session.setFlushMode(FlushMode.MANUAL);

			Kegiatan kegiatan = (Kegiatan) session.get(Kegiatan.class, kegiatanId);
			if (kegiatan == null) {
				return false;
			}

			String cicilansBaru = bangunStringAktifDariIds(cicilanIds);
			List<CicilanPembayaran> listCicilan = loadCicilanByIds(session,
					cicilanIds == null ? new ArrayList<Long>() : cicilanIds);
			RekapPembayaran rekap = bangunRekapPembayaran(listCicilan);

			kegiatan.setCicilans(cicilansBaru);
			kegiatan.setBulans(rekap.bulans);
			Double dibayar = rekap.dibayar;
			try {
				dibayar = kegiatan.hitungDibayar();
			} catch (Exception e) {
				dibayar = rekap.dibayar;
			}
			kegiatan.setDibayar(dibayar);

			tx = session.beginTransaction();
			session.createSQLQuery("UPDATE kegiatan SET bulans = :bulans, cicilans = :cicilans, dibayar = :dibayar "
					+ "WHERE id = :id")
					.setParameter("bulans", kegiatan.getBulans() == null ? "{}" : kegiatan.getBulans())
					.setParameter("cicilans", cicilansBaru)
					.setParameter("dibayar", kegiatan.getDibayar() == null ? Double.valueOf(0.0) : kegiatan.getDibayar())
					.setParameter("id", kegiatanId).executeUpdate();
			tx.commit();
			return true;
		} catch (Exception e) {
			rollbackQuietly(tx);
			Common.tampilErrorJikaAdmin(e);
			return false;
		} finally {
			closeOpenedSession(session);
		}
	}

	private static String bangunStringAktifDariIds(List<Long> ids) {
		StringBuilder builder = new StringBuilder(",");
		if (ids != null) {
			for (Long id : ids) {
				if (id != null) {
					builder.append(id).append(":true,");
				}
			}
		}
		return builder.toString();
	}

	// ========================================================================
	// 5. UTILITY PUBLIK
	// ========================================================================

	public static List<Long> ekstrakIdAktif(String data) {
		List<Long> list = new ArrayList<Long>();
		if (!isEmpty(data)) {
			String[] partsData = data.split(",");
			for (int i = 0; i < partsData.length; i++) {
				String part = partsData[i];
				if (isEmpty(part)) {
					continue;
				}
				String[] parts = part.split(":");
				try {
					Long id = Long.valueOf(parts[0].trim());
					boolean aktif = parts.length > 1 ? Boolean.parseBoolean(parts[1].trim()) : true;
					if (aktif) {
						list.add(id);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:1518");
				}
			}
		}
		return list;
	}

	public static String murnikan(JenisKegiatan jenisKegiatan, String tagihans) {
		if (jenisKegiatan == null || isEmpty(tagihans)) {
			return tagihans;
		}

		boolean hanyaAngsuran = Boolean.TRUE.equals(jenisKegiatan.getHanyaBerupaAngsuran());
		boolean hanyaBukanAngsuran = Boolean.TRUE.equals(jenisKegiatan.getHanyaBerupaBukanAngsuran());

		if (!hanyaAngsuran && !hanyaBukanAngsuran) {
			return tagihans;
		}

		try {
			JSONObject oldJson = new JSONObject(tagihans);
			JSONObject newJson = new JSONObject();
			Iterator<?> iterator = oldJson.keys();
			while (iterator.hasNext()) {
				String key = (String) iterator.next();
				if (((hanyaAngsuran && key.contains("_")) || (hanyaBukanAngsuran && !key.contains("_")))
						&& !oldJson.isNull(key)) {
					newJson.put(key, oldJson.get(key));
				}
			}
			return newJson.toString();
		} catch (Exception e) {
			return tagihans;
		}
	}

	private static boolean isSama(Object o1, Object o2) {
		if (o1 == null && o2 == null) {
			return true;
		}
		if (o1 == null || o2 == null) {
			return false;
		}
		if (o1 instanceof Number && o2 instanceof Number) {
			double d1 = ((Number) o1).doubleValue();
			double d2 = ((Number) o2).doubleValue();
			return Math.abs(d1 - d2) < 0.0001;
		}
		return o1.equals(o2);
	}
}
