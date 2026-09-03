package ais.action.master.sekolah.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.MemoryDbUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.AsramaSiswaPunyaSiswa;
import ais.database.model.sekolah.DiskonSiswa;
import ais.database.model.sekolah.DiskonSiswaItemBiaya;
import ais.database.model.sekolah.DiskonSiswaPunyaSiswa;
import ais.database.model.sekolah.ItemBiayaSekolah;
import ais.database.model.sekolah.JenisBiayaSekolah;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.NominalBiaya;
import ais.database.model.sekolah.PembayaranSiswa;
import ais.database.model.sekolah.PembayaranSiswaDetail;
import ais.database.model.sekolah.PengaturanBiaya;
import ais.database.model.sekolah.PengaturanBiayaItemBiaya;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Tagihan;

/**
 * Tipe khusus untuk tagihan util. Kelas ini memberi nama dan batas tanggung jawab yang eksplisit
 * pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code int DETIK_MAKS_ANTRI_GENERATE}, {@code
 * int JUMLAH_STRIPE_SISWA}, {@code ReentrantLock KUNCI_GENERATE_SISWA}; pembacaan/pencarian ({@code
 * jalankanTulisTahanDeadlock()}, {@code ambilKeanggotaanSiswaTerbaru()}, {@code getBulanMulai()}, {@code
 * findTagihanByKodeUnik()}, {@code ambilNominalBiaya()}, {@code ambilNominalBiaya()}); validasi/perhitungan
 * ({@code hapusTagihanTidakValid()}); mutasi data ({@code saveTagihanAman()}, {@code
 * resetNominalBiayaDanTagihan()}, {@code resetSemuaTagihanDalamPB()}); penghapusan/pembatalan ({@code
 * hapusTagihanAman()}); operasi domain lain ({@code kunciGenerateUntukSiswa()}, {@code isKonflikKunci()}, {@code
 * tidurBackoffDeadlock()}, {@code isSiswaMemenuhiSyarat()}, {@code sinkronkanKelasTagihan()}, {@code
 * perbaikiJumlahAngsuran()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut
 * di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 */
public class TagihanUtil {

	/**
	 * Batasi tagihan pada pasangan paket + item yang masih tercantum di Setting Biaya.
	 *
	 * <p>Tagihan dan NominalBiaya adalah data materialisasi, sehingga baris lama tetap
	 * ada ketika operator melepas suatu item dari PengaturanBiayaAction. Tanpa korelasi
	 * ini, baris lama tersebut kembali tampil walaupun relasi
	 * PengaturanBiayaItemBiaya-nya sudah dihapus.</p>
	 */
	static Criteria batasiPadaItemYangMasihDiatur(Criteria criteria) {
		return criteria.add(Restrictions.sqlRestriction(
				"exists (select 1 from sekolah.pengaturan_biaya_item_biaya pbi_aktif "
						+ "where pbi_aktif.pengaturan_biaya_id = {alias}.pengaturan_biaya_id "
						+ "and pbi_aktif.item_biaya_sekolah_id = {alias}.item_biaya_sekolah_id)"));
	}

	// === Anti-deadlock generate tagihan (KE-1/2/3): serialisasi per-siswa + retry saat deadlock ===

	/** Lama maksimal sebuah thread menunggu giliran generate untuk siswa yang sama (detik). */
	private static final int DETIK_MAKS_ANTRI_GENERATE = 90;

	/** Jumlah stripe kunci (tetap) agar antrian per-siswa tidak membuat map membengkak. */
	private static final int JUMLAH_STRIPE_SISWA = 128;

	private static final ReentrantLock[] KUNCI_GENERATE_SISWA = new ReentrantLock[JUMLAH_STRIPE_SISWA];
	static {
		for (int i = 0; i < JUMLAH_STRIPE_SISWA; i++) {
			KUNCI_GENERATE_SISWA[i] = new ReentrantLock(true);
		}
	}

	/**
	 * Kunci FIFO (fair) per-siswa untuk generate tagihan. Dua request untuk siswa yang SAMA (app
	 * di-refresh, dobel-tap, banyak tab) diantrikan di aplikasi SEBELUM membuka koneksi DB sehingga
	 * tidak ada dua transaksi yang menulis baris tagihan/pembayaran_siswa_detail siswa itu secara
	 * paralel (penyebab utama deadlock). Lock striping berukuran tetap; siswa berbeda yang kebetulan
	 * sehash hanya sesekali saling menunggu (tanpa error, hanya serialisasi ringan).
	 */
	private static ReentrantLock kunciGenerateUntukSiswa(Siswa siswa) {
		Object id = (siswa == null) ? null : siswa.getId();
		int h = (id == null) ? 0 : id.hashCode();
		int idx = (h & 0x7fffffff) % JUMLAH_STRIPE_SISWA;
		return KUNCI_GENERATE_SISWA[idx];
	}

	/** Unit tulis singkat (begin -> kerjakan -> commit) yang boleh diulang saat deadlock. */
	private interface UnitTulis {
		void kerjakan(Session session) throws Exception;
	}

	/**
	 * True bila exception (di sepanjang rantai penyebab) adalah konflik kunci yang layak diulang:
	 * deadlock (40P01), serialization failure (40001), lock timeout (55P03), statement timeout (57014),
	 * atau transaksi yang sudah aborted (25P02).
	 */
	private static boolean isKonflikKunci(Throwable e) {
		Throwable c = e;
		while (c != null) {
			String state = (c instanceof java.sql.SQLException) ? ((java.sql.SQLException) c).getSQLState() : null;
			if ("40P01".equals(state) || "40001".equals(state) || "55P03".equals(state) || "57014".equals(state)
					|| "25P02".equals(state)) {
				return true;
			}
			String msg = c.getMessage();
			if (msg != null) {
				String m = msg.toLowerCase();
				if (m.indexOf("deadlock detected") >= 0 || m.indexOf("could not serialize") >= 0
						|| m.indexOf("lock timeout") >= 0 || m.indexOf("current transaction is aborted") >= 0) {
					return true;
				}
			}
			c = c.getCause();
		}
		return false;
	}

	/** Jeda backoff eksponensial + jitter (di LUAR transaksi DB) sebelum mengulang unit tulis. */
	private static void tidurBackoffDeadlock(int percobaan) {
		long dasar = 200L;
		int pangkat = percobaan - 1;
		if (pangkat < 0) pangkat = 0;
		if (pangkat > 8) pangkat = 8;
		long jeda = Math.min(dasar << pangkat, 2000L) + (long) (Math.random() * 150);
		try {
			Thread.sleep(jeda);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Jalankan satu unit tulis dengan TAHAN-DEADLOCK. Saat PostgreSQL melaporkan deadlock/serialization
	 * failure/lock timeout, transaksi di-rollback lalu session di-clear (menghapus status "current
	 * transaction is aborted" agar query berikutnya pada session yang sama tidak ikut gagal -- inilah
	 * akar KE-1), tunggu backoff singkat, lalu unit diulang. Bila semua percobaan gagal, kegagalan
	 * dilaporkan ke admin dan proses TETAP lanjut: penulisan penyelarasan nominal bersifat best-effort,
	 * tagihan tetap tampil memakai nilai di memori. Mengembalikan true bila commit berhasil.
	 */
	private static boolean jalankanTulisTahanDeadlock(Session session, UnitTulis unit) {
		if (session == null || !session.isOpen() || unit == null) {
			return false;
		}
		int maksimal = 4;
		for (int percobaan = 1; percobaan <= maksimal; percobaan++) {
			Transaction tx = null;
			try {
				tx = session.beginTransaction();
				unit.kerjakan(session);
				tx.commit();
				return true;
			} catch (Exception e) {
				try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtil.java:136");}
				try { if (session.isOpen()) session.clear(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtil.java:137");}
				if (isKonflikKunci(e) && percobaan < maksimal) {
					tidurBackoffDeadlock(percobaan);
					continue;
				}
				Common.tampilErrorJikaAdmin(e);
				return false;
			}
		}
		return false;
	}

	// --- HELPER METHODS UNTUK PENYEDERHANAAN ---

	private static boolean isSiswaMemenuhiSyarat(Siswa siswa, PengaturanBiaya pb, List<Long> kelases,
			List<Long> asramas, String kelasData) {
		if (pb == null || siswa == null || siswa.getTahunMasuk() == null)
			return false;

		boolean syaratAsrama = (!pb.getTanpaAsrama() && pb.getAsramaSiswa() == null) || pb.getTanpaAsrama()
				|| (pb.getAsramaSiswa() != null && asramas.contains(pb.getAsramaSiswa().getId()));

		boolean syaratKelas = (pb.getKelasSiswa() == null && (Integer.valueOf(0).equals(pb.getTahunAngkatan())
				|| pb.getTahunAngkatan().equals(siswa.getTahunMasuk()))) || pb.getKhususBuatSiswaTertentu()
				|| (pb.getKelasSiswa() != null && kelases.contains(pb.getKelasSiswa().getId()));

		boolean syaratKelasBanyak = pb.getKelasBanyak() == null || pb.getKelasBanyak().trim().isEmpty()
				|| (kelasData != null && pb.getKelasBanyak().trim().contains(kelasData));

		return syaratAsrama && syaratKelas && syaratKelasBanyak;
	}

	/**
	 * Tipe implementasi bersarang {@link DataKeanggotaanSiswa} milik {@link TagihanUtil}. Kelas ini memberi nama
	 * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link TagihanUtil}.
	 * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
	 * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code List kelasIds}, {@code List
	 * asramaIds}, {@code String namaKelas}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
	 * dipanggilnya.</p>
	 *
	 * @see TagihanUtil
	 */
	private static class DataKeanggotaanSiswa {
		private final List<Long> kelasIds = new ArrayList<Long>();
		private final List<Long> asramaIds = new ArrayList<Long>();
		private String namaKelas;
	}

	/**
	 * Ambil keanggotaan kelas/asrama langsung dari DB. Siswa.ambilkelas(), ambilasrama(),
	 * dan ambilKelas() memakai ConstantValues sehingga dapat tertinggal setelah siswa
	 * dipindah kelas. Sinkronisasi tagihan harus memakai relasi terbaru.
	 */
	@SuppressWarnings("unchecked")
	private static DataKeanggotaanSiswa ambilKeanggotaanSiswaTerbaru(Siswa siswa, String tahunAjaran) {
		DataKeanggotaanSiswa data = new DataKeanggotaanSiswa();
		if (siswa == null || siswa.getId() == null) {
			return data;
		}
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			List<KelasSiswaPunyaSiswa> daftarKelas = session.createCriteria(KelasSiswaPunyaSiswa.class)
					.createAlias("kelasSiswa", "kelasTerbaru")
					.add(Restrictions.eq("siswa", siswa))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.or(Restrictions.isNull("kelasTerbaru.aktif"),
							Restrictions.eq("kelasTerbaru.aktif", true))).list();
			for (KelasSiswaPunyaSiswa relasi : daftarKelas) {
				KelasSiswa kelas = relasi == null ? null : relasi.getKelasSiswa();
				if (kelas == null || kelas.getId() == null) {
					continue;
				}
				data.kelasIds.add(kelas.getId());
				if (tahunAjaran != null && tahunAjaran.equals(kelas.getTahunAjaran())) {
					data.namaKelas = kelas.getNama();
				}
			}

			List<AsramaSiswaPunyaSiswa> daftarAsrama = session.createCriteria(AsramaSiswaPunyaSiswa.class)
					.add(Restrictions.eq("siswa", siswa))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
			for (AsramaSiswaPunyaSiswa relasi : daftarAsrama) {
				if (relasi != null && relasi.getAsramaSiswa() != null && relasi.getAsramaSiswa().getId() != null) {
					data.asramaIds.add(relasi.getAsramaSiswa().getId());
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"TagihanUtil.ambilKeanggotaanSiswaTerbaru siswa=" + siswa.getId());
			// Fallback menjaga proses lama tetap berjalan saat query relasi gagal.
			data.kelasIds.clear();
			data.kelasIds.addAll(siswa.ambilkelas());
			data.asramaIds.clear();
			data.asramaIds.addAll(siswa.ambilasrama());
			KelasSiswa kelas = Siswa.ambilKelas(siswa, tahunAjaran);
			data.namaKelas = kelas == null ? null : kelas.getNama();
		} finally {
			closeSessionAndDisconnect(session);
		}
		return data;
	}

	/** Samakan snapshot kelas pada seluruh tagihan PB dengan kelas aktif siswa pada tahun ajaran PB. */
	private static void sinkronkanKelasTagihan(Session session, Siswa siswa, PengaturanBiaya pengaturanBiaya) {
		if (session == null || !session.isOpen() || siswa == null || pengaturanBiaya == null) {
			return;
		}
		try {
			Criteria kelasCriteria = session.createCriteria(KelasSiswaPunyaSiswa.class)
					.createAlias("kelasSiswa", "kelasAktif")
					.add(Restrictions.eq("siswa", siswa))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.or(Restrictions.isNull("kelasAktif.aktif"),
							Restrictions.eq("kelasAktif.aktif", true)))
					.add(pengaturanBiaya.getTahunAjaran() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("kelasAktif.tahunAjaran", pengaturanBiaya.getTahunAjaran()))
					.addOrder(Order.desc("id")).setMaxResults(1);
			KelasSiswa kelasTerbaru = null;
			KelasSiswaPunyaSiswa relasi = (KelasSiswaPunyaSiswa) kelasCriteria.uniqueResult();
			if (relasi != null) {
				kelasTerbaru = relasi.getKelasSiswa();
			}

			Transaction tx = session.beginTransaction();
			session.createQuery("update Tagihan set kelasSiswa = :kelas where siswa = :siswa "
					+ "and pengaturanBiaya = :pengaturanBiaya")
					.setParameter("kelas", kelasTerbaru)
					.setParameter("siswa", siswa)
					.setParameter("pengaturanBiaya", pengaturanBiaya)
					.executeUpdate();
			tx.commit();
			session.clear();
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception rollbackError) {
				ais.common.ErrorAuditUtil.record(rollbackError,
						"TagihanUtil.sinkronkanKelasTagihan rollback");
			}
			ais.common.ErrorAuditUtil.record(e, "TagihanUtil.sinkronkanKelasTagihan siswa=" + siswa.getId());
		}
	}

	public static int getBulanMulai(JenisBiayaSekolah jenisBiaya) {
		int mulai = 8;
		try {
			mulai = Integer.parseInt(Common.getKonfigurasi("bulan_mulai_tagihan", "8").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtil.java:173");
			// abaikan jika tidak ada konfigurasi, gunakan default 8
		}
		if (jenisBiaya != null && jenisBiaya.getMulaiDitagihDiBulan() != null) {
			mulai = jenisBiaya.getMulaiDitagihDiBulan();
		}
		return mulai;
	}

	private static void perbaikiJumlahAngsuran(Session session, NominalBiaya nominalBiayaSiswa) {
		if (!nominalBiayaSiswa.getItemBiayaSekolah().getAngsuranSeragam()
				&& !nominalBiayaSiswa.getPengaturanBiaya().getJenisBiayaSekolah().getPeriode().equals("Bulanan")) {

			Number maks = (Number) session.createCriteria(Tagihan.class)
					.add(Restrictions.eq("nominalBiaya", nominalBiayaSiswa)).setProjection(Projections.rowCount())
					.add(Restrictions.gt("nominal", 0.1)).uniqueResult();

			int intMaks = (maks == null) ? 1 : maks.intValue();
			if (nominalBiayaSiswa.getDibayarSebayak().intValue() != intMaks) {
				nominalBiayaSiswa.setDibayarSebayak(intMaks);
				Transaction tx = session.beginTransaction();
				try {
					Common.refreshUpdate(session, nominalBiayaSiswa);
					tx.commit();
				} catch (Exception eUpdate) {
					try {
						if (tx != null && tx.isActive()) {
							tx.rollback();
						}
					} catch (Exception eRollback) { ais.common.ErrorAuditUtil.record(eRollback, "auto-audit(rollback-gagal) src/ais/action/master/sekolah/helper/TagihanUtil.java:perbaikiJumlahAngsuran"); }
					try { session.clear(); } catch (Exception eClear) { ais.common.ErrorAuditUtil.record(eClear, "auto-audit(clear-gagal) src/ais/action/master/sekolah/helper/TagihanUtil.java:perbaikiJumlahAngsuran"); }
					ais.common.ErrorAuditUtil.record(eUpdate,
							"TagihanUtil.perbaikiJumlahAngsuran: gagal update Dibayar Sebayak untuk nominalBiaya id="
									+ (nominalBiayaSiswa.getId() == null ? "null" : nominalBiayaSiswa.getId())
									+ " (kemungkinan referensi pembayaran_siswa_detail sudah terhapus/yatim)");
				}
			}
		}
	}

	private static boolean tagihanBelumDibayar(Tagihan tagihan) {
		if (tagihan == null) return false;
		if (tagihan.getPembayaranSiswaDetail() != null) return false;
		if (tagihan.getDibayar() != null && tagihan.getDibayar() > 0.1) return false;
		if (tagihan.getDibayarManual() != null && tagihan.getDibayarManual() > 0.1) return false;
		return true;
	}

	private static void hapusTagihanAman(Session session, Tagihan tagihan) {
		if (session == null || tagihan == null || !tagihanBelumDibayar(tagihan)) return;
		try {
			String kodeUnik = tagihan.getKodeUnik();
			if (kodeUnik != null) {
				try { MemoryDbUtil.getAllTagihan().remove(kodeUnik); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtil.java:hapusTagihanAman-cache"); }
			}
			Transaction txDel = session.beginTransaction();
			session.delete(tagihan);
			txDel.commit();
		} catch (Exception eDel) {
			try { session.clear(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtil.java:hapusTagihanAman-clear");}
		}
	}

	@SuppressWarnings("unchecked")
	private static void bersihkanTagihanNolDanNormalisasiAngsuran(Session session, Siswa siswa,
			CalonSiswa calonSiswa, PengaturanBiaya pengaturanBiaya, ItemBiayaSekolah itemBiayaSekolah) {
		if (session == null || !session.isOpen() || pengaturanBiaya == null || (siswa == null && calonSiswa == null)) return;
		try {
			Criteria critNb = session.createCriteria(NominalBiaya.class)
					.add(Restrictions.eq("pengaturanBiaya", pengaturanBiaya));
			if (itemBiayaSekolah != null) {
				critNb.add(Restrictions.eq("itemBiayaSekolah", itemBiayaSekolah));
			}
			if (siswa != null) {
				critNb.add(Restrictions.eq("siswa", siswa));
			} else {
				critNb.add(Restrictions.eq("calonSiswa", calonSiswa));
			}

			List<NominalBiaya> nominalBiayas = critNb.list();
			for (NominalBiaya nb : nominalBiayas) {
				if (nb == null) continue;
				List<Tagihan> tagihans = session.createCriteria(Tagihan.class)
						.add(Restrictions.eq("nominalBiaya", nb))
						.addOrder(Order.asc("bayarKe"))
						.addOrder(Order.asc("id"))
						.list();

				boolean semuaBelumDibayar = true;
				int jumlahValid = 0;
				int bayarKeBerbayarTerakhir = 0;
				int nomorBaru = 1;
				for (Tagihan t : tagihans) {
					boolean belumDibayar = tagihanBelumDibayar(t);
					if (!belumDibayar) {
						semuaBelumDibayar = false;
						jumlahValid++;
						if (t.getBayarKe() != null && t.getBayarKe() > bayarKeBerbayarTerakhir) {
							bayarKeBerbayarTerakhir = t.getBayarKe();
						}
						continue;
					}

					Double nominalTagihan = t.getNominal();
					boolean tagihanNol = nominalTagihan == null || nominalTagihan <= 0.1;
					boolean bukanTagihan = Boolean.TRUE.equals(nb.getBukanTagihan()) || Boolean.TRUE.equals(t.ambilBukanTagihanData())
							|| Boolean.TRUE.equals(t.ambilBukanTagihan());
					if (tagihanNol || bukanTagihan) {
						hapusTagihanAman(session, t);
					} else {
						jumlahValid++;
						if (semuaBelumDibayar && t.getBayarKe() != null && t.getBayarKe().intValue() != nomorBaru) {
							t.setBayarKe(nomorBaru);
							t.setKodeUnik(Tagihan.genCode(t.getItemBiayaSekolah(), t.getPengaturanBiaya(),
									t.getTahunbulan(), t.getSiswa(), t.getCalonSiswa(), nomorBaru));
							Transaction txU = session.beginTransaction();
							Common.refreshUpdate(session, t);
							txU.commit();
						}
						nomorBaru++;
					}
				}

				if (Boolean.TRUE.equals(nb.getBukanTagihan()) || nb.getNominal() == null || nb.getNominal() <= 0.1) {
					if (semuaBelumDibayar) {
						try {
							Transaction txNbDel = session.beginTransaction();
							session.delete(nb);
							txNbDel.commit();
						} catch (Exception eDelNb) {
							try { session.clear(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtil.java:bersihkanTagihanNol-nb-clear");}
						}
					}
					continue;
				}

				int jumlahNormal = jumlahValid;
				if (bayarKeBerbayarTerakhir > jumlahNormal) {
					jumlahNormal = bayarKeBerbayarTerakhir;
				}
				if (jumlahNormal < 1) {
					jumlahNormal = 1;
				}
				if (nb.getDibayarSebayak() == null || nb.getDibayarSebayak().intValue() != jumlahNormal
						|| nb.getDibayarSebayakManual() == null
						|| nb.getDibayarSebayakManual().intValue() != jumlahNormal) {
					nb.setDibayarSebayakManual(jumlahNormal);
					nb.setDibayarSebayak(jumlahNormal);
					Transaction txNb = session.beginTransaction();
					Common.refreshUpdate(session, nb);
					txNb.commit();
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private static boolean isDuplicateKodeUnikException(Throwable e) {
		Throwable t = e;
		while (t != null) {
			String msg = t.getMessage();
			if (msg != null && msg.toLowerCase().indexOf("tagihan_kode_unik_key") >= 0) {
				return true;
			}
			t = t.getCause();
		}
		return false;
	}

	private static Tagihan findTagihanByKodeUnik(Session session, String kodeUnik) {
		if (kodeUnik == null || kodeUnik.trim().length() == 0) {
			return null;
		}
		return (Tagihan) session.createCriteria(Tagihan.class).add(Restrictions.eq("kodeUnik", kodeUnik))
				.addOrder(Order.asc("id")).setMaxResults(1).uniqueResult();
	}

	private static Tagihan saveTagihanAman(Session session, Tagihan tagihan) throws Exception {
		if (session == null || tagihan == null) {
			return tagihan;
		}
		String kodeUnik = tagihan.getKodeUnik();
		Tagihan existing = findTagihanByKodeUnik(session, kodeUnik);
		if (existing != null) {
			return existing;
		}
		Transaction tx = null;
		boolean createdTx = false;
		try {
			tx = session.getTransaction();
			if (tx == null || !tx.isActive()) {
				tx = session.beginTransaction();
				createdTx = true;
			}
			session.save(tagihan);
			if (createdTx && tx != null && tx.isActive()) {
				tx.commit();
			}
			return tagihan;
		} catch (Exception e) {
			try { if (createdTx && tx != null && tx.isActive()) tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtil.java:243");}
			try { session.clear(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtil.java:244");}
			if (isDuplicateKodeUnikException(e)) {
				existing = findTagihanByKodeUnik(session, kodeUnik);
				if (existing != null) {
					return existing;
				}
			}
			throw e;
		}
	}

	private static Tagihan pastikanNominalBulananSamaDenganNominalBiaya(Session session, Tagihan tagihan,
			NominalBiaya nominalBiaya) throws Exception {
		if (session == null || tagihan == null || nominalBiaya == null || nominalBiaya.getPengaturanBiaya() == null
				|| nominalBiaya.getPengaturanBiaya().getJenisBiayaSekolah() == null
				|| !"Bulanan".equalsIgnoreCase(nominalBiaya.getPengaturanBiaya().getJenisBiayaSekolah().getPeriode())) {
			return tagihan;
		}

		// Jangan sentuh tagihan yang sudah dibayar
		if (tagihan.getId() != null && tagihan.getPembayaranSiswaDetail() != null) {
			return tagihan;
		}

		Double nominalAcuan = Boolean.TRUE.equals(nominalBiaya.getBukanTagihan()) ? 0.0 : nominalBiaya.getNominal();
		if (nominalAcuan == null) {
			nominalAcuan = 0.0;
		}
		tagihan.setNominal(nominalAcuan);

		// nominalBiaya (FK) tidak boleh diubah jika sudah punya referensi sendiri;
		// hanya boleh di-set saat pertama kali (null) atau saat tagihan baru (id=null)
		if (tagihan.getNominalBiaya() == null) {
			tagihan.setNominalBiaya(nominalBiaya);
		}

		tagihan.setBayarKe(1);

		// KE-4: FK tagihan.pembayaran_siswa_detail_id dapat menunjuk baris pembayaran_siswa_detail yang
		// SUDAH DIHAPUS (mis. pembayaran dibatalkan/direvisi oleh proses lain) sementara referensi pada
		// objek Tagihan yang sedang diproses job sinkron ini masih membawa id lama -> UPDATE gagal
		// ConstraintViolationException "fka3b5b0265a776744" (FK target tak ada di pembayaran_siswa_detail).
		// Verifikasi LANGSUNG ke tabel (SQL native, BUKAN session.get()/proxy yang bisa mengembalikan data
		// basi dari cache) sebelum menulis; null-kan bila target sudah tak ada agar tagihan TETAP tersimpan
		// (bukan gagal sinkron seutuhnya). setFlushMode(MANUAL) pada query cek ini mencegah Hibernate
		// auto-flush perubahan tagihan yang BELUM diverifikasi (yang justru akan memicu error yang sama
		// lebih dini, sebelum sempat di-null-kan).
		if (tagihan.getPembayaranSiswaDetail() != null && tagihan.getPembayaranSiswaDetail().getId() != null) {
			Long idPsd = tagihan.getPembayaranSiswaDetail().getId();
			boolean psdAda = true;
			try {
				Object ada = session.createSQLQuery("SELECT id FROM pembayaran_siswa_detail WHERE id = :id")
						.setFlushMode(org.hibernate.FlushMode.MANUAL).setParameter("id", idPsd).setMaxResults(1)
						.uniqueResult();
				psdAda = (ada != null);
			} catch (Exception eCekPsd) {
				psdAda = true; // gagal cek -> jangan asumsikan hilang, biarkan alur normal berjalan
			}
			if (!psdAda) {
				tagihan.setPembayaranSiswaDetail(null);
			}
		}

		if (tagihan.getId() == null) {
			return saveTagihanAman(session, tagihan);
		}
		// Tulis penyelarasan nominal TAHAN-DEADLOCK. flush=false: biarkan commit yang men-flush agar
		// deadlock/serialization failure muncul sebagai kegagalan commit yang ditangkap + diulang helper
		// (bukan tertelan diam-diam di safeFlush yang meninggalkan session dalam status transaksi aborted
		// -> query berikutnya gagal "current transaction is aborted", akar KE-1).
		final Tagihan tagihanUtkSimpan = tagihan;
		jalankanTulisTahanDeadlock(session, new UnitTulis() {
			@Override
			public void kerjakan(Session s) throws Exception {
				Common.refreshUpdate(s, tagihanUtkSimpan, false);
			}
		});
		return tagihan;
	}

	// --- END OF HELPER METHODS ---

	public static List<Tagihan> doGenerateTagihanBulanan(Siswa siswa, PengaturanBiaya pengaturanBiaya,
			Integer tahunSampai, Integer bulanSampai, int mulai) {

		DataKeanggotaanSiswa keanggotaan = ambilKeanggotaanSiswaTerbaru(siswa, pengaturanBiaya.getTahunAjaran());
		List<Long> kelases = keanggotaan.kelasIds;
		List<Long> asramas = keanggotaan.asramaIds;
		String kelasData = keanggotaan.namaKelas;

		List<Tagihan> tagihans = new ArrayList<Tagihan>();

		if (isSiswaMemenuhiSyarat(siswa, pengaturanBiaya, kelases, asramas, kelasData)) {
			int tahunMasukSiswa = Integer
					.parseInt(siswa.getTahunMasuk().toString() + (mulai > 9 ? mulai : "0" + mulai));
			int bulanTahunUtama = PembayaranSiswa.convert(tahunSampai, bulanSampai);

			Session session = null;
			try {
				// Pakai openSession DEDIKASI (bukan currentNativeSession). Alur generate tagihan ini
				// memanggil banyak helper (ambilNominalBiaya, Tagihan.ambilAtauBuat /
				// saveTagihanDenganKodeUnikAman, perbaikiJumlahAngsuran) yang di dalamnya bisa memanggil
				// HibernateUtil.closeSession() → menutup session ThreadLocal yang sedang dipakai di
				// tengah loop, memicu "Session is closed!" saat createCriteria/commit berikutnya. Session
				// dedikasi TIDAK tersimpan di ThreadLocal sehingga kebal terhadap penutupan itu. Ditutup
				// di finally (closeSessionAndDisconnect).
				session = HibernateUtil.openSession();
				@SuppressWarnings("unchecked")
				List<PengaturanBiayaItemBiaya> daftarBiayas = ConstantValues
						.simpleList(
								session.createCriteria(PengaturanBiayaItemBiaya.class)
										.add(Restrictions.eq("pengaturanBiaya", pengaturanBiaya)),
								PengaturanBiayaItemBiaya.class);

				for (PengaturanBiayaItemBiaya pbItemBiaya : daftarBiayas) {
					try {
						if (Tagihan.getBoleh(pengaturanBiaya, siswa, null, pbItemBiaya.getItemBiayaSekolah())) {

							Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
							cal.set(Calendar.DATE, 1);
							cal.set(Calendar.MONTH, mulai - 1);
							cal.set(Calendar.YEAR, siswa.getTahunMasuk());

							Integer pembayaranTerakhir = 0;
							while (bulanTahunUtama > pembayaranTerakhir) {
								int tahunCurrent = cal.get(Calendar.YEAR);
								int bulanCurrent = cal.get(Calendar.MONTH);
								int bulanCurrentPlus = bulanCurrent + 1;
								pembayaranTerakhir = PembayaranSiswa.convert(tahunCurrent, bulanCurrentPlus);

								if (pengaturanBiaya.getBulanMulai() != null
										&& pembayaranTerakhir < pengaturanBiaya.getBulanMulai()) {
									cal.add(Calendar.MONTH, 1);
									continue;
								}
								if (pengaturanBiaya.getBulanSampai() != null
										&& pembayaranTerakhir > pengaturanBiaya.getBulanSampai()) {
									break;
								}

								NominalBiaya nominalBiayaSiswa = ambilNominalBiaya(pbItemBiaya, siswa,
										pembayaranTerakhir, session);
								if (nominalBiayaSiswa == null || nominalBiayaSiswa.getNominal() == null
										|| nominalBiayaSiswa.getNominal() <= 0.1
										|| Boolean.TRUE.equals(nominalBiayaSiswa.getBukanTagihan())) {
									cal.add(Calendar.MONTH, 1);
									continue;
								}

								List<Long> notPembayaran = new ArrayList<Long>();
								int batasBayar = nominalBiayaSiswa.getDibayarSebayak() == null ? 1
										: nominalBiayaSiswa.getDibayarSebayak();
								for (int bayarKe = 1; bayarKe <= batasBayar; bayarKe++) {
									String kodeUnik = Tagihan.genCode(nominalBiayaSiswa.getItemBiayaSekolah(),
											nominalBiayaSiswa.getPengaturanBiaya(), pembayaranTerakhir, siswa,
											nominalBiayaSiswa.getCalonSiswa(), bayarKe);

									Tagihan tagihan = (Tagihan) session.createCriteria(Tagihan.class)
											.add(Restrictions.eq("kodeUnik", kodeUnik)).setMaxResults(1).uniqueResult();

									if (tagihan == null) {
										try {
											if (pembayaranTerakhir < tahunMasukSiswa) {
												cal.add(Calendar.MONTH, 1);
												continue;
											}

											Criteria critDetail = session.createCriteria(PembayaranSiswaDetail.class)
													.createAlias("tagihan", "tagihan")
													.add(Restrictions.eq("tagihan.bayarKe", bayarKe))
													.add(Restrictions.eq("nominalBiaya", nominalBiayaSiswa));

											if (!notPembayaran.isEmpty()) {
												critDetail.add(Restrictions.not(Restrictions.in("id", notPembayaran)));
											}

											PembayaranSiswaDetail pembayaranSiswaDetail = (PembayaranSiswaDetail) critDetail
													.add(Restrictions.eq("itemBiayaSekolah",
															pbItemBiaya.getItemBiayaSekolah()))
													.createCriteria("pembayaranSiswa")
													.add(Restrictions.eq("siswa", siswa))
													.add(Restrictions.eq("jenisBiayaSekolah",
															pengaturanBiaya.getJenisBiayaSekolah()))
													.add(Restrictions.eq("tahunDanBulan", pembayaranTerakhir))
													.setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();

											if (pembayaranSiswaDetail != null
													&& pembayaranSiswaDetail.getId() != null) {
												notPembayaran.add(pembayaranSiswaDetail.getId());
											}

											if (pembayaranSiswaDetail == null
													|| pembayaranSiswaDetail.getTagihan() == null) {
												Double n = null;
												if (bayarKe > 1) {
													Number nm = (Number) session.createCriteria(Tagihan.class)
															.add(Restrictions.eq("nominalBiaya", nominalBiayaSiswa))
															.add(Restrictions.lt("bayarKe", bayarKe))
															.setProjection(Projections.sum("nominal")).uniqueResult();

													if (nm != null
															&& nm.doubleValue() < nominalBiayaSiswa.getNominal()) {
														n = nominalBiayaSiswa.getNominal() - nm.doubleValue();
													}
												}

												tagihan = new Tagihan();
												tagihan.setNominalBiaya(nominalBiayaSiswa);
												tagihan.setTahunbulan(pembayaranTerakhir);
												tagihan.setBulan(bulanCurrentPlus);
												tagihan.setTahun(tahunCurrent);
												tagihan.setPembayaranSiswaDetail(pembayaranSiswaDetail);
												tagihan.setSiswa(siswa);
												tagihan.setItemBiayaSekolah(pbItemBiaya.getItemBiayaSekolah());
												tagihan.setBayarKe(bayarKe);
												// FIX BULANAN: bayarKe==1 & pembayaran TUNGGAL (dibayarSebayak<=1) -> nominal = NOMINAL
												// PENUH dari NominalBiaya. Sebelumnya 'n' tetap null utk bayarKe==1 -> setNominal(null)
												// -> tagihan bulan baru tampil 0 (getNominal() utk item boleh-angsur, nominal null = 0).
												if (n == null && (nominalBiayaSiswa.getDibayarSebayak() == null
														|| nominalBiayaSiswa.getDibayarSebayak().intValue() <= 1)) {
													n = nominalBiayaSiswa.getNominal();
												}
												tagihan.setNominal(n);

												tagihan = pastikanNominalBulananSamaDenganNominalBiaya(session, tagihan,
														nominalBiayaSiswa);

												if (pembayaranSiswaDetail != null
														&& pembayaranSiswaDetail.getId() != null) {
													pembayaranSiswaDetail.setTagihan(tagihan);
													final PembayaranSiswaDetail psdUtkSimpan = pembayaranSiswaDetail;
													jalankanTulisTahanDeadlock(session, new UnitTulis() {
														@Override
														public void kerjakan(Session s) throws Exception {
															s.update(psdUtkSimpan);
														}
													});

													if (TagihanDiskonSiswaHelper.diskonTidakMemotongTagihan(tagihan)) {
														DaftarPengajuanTransfer.simpanDiskonPembayaran(tagihan);
													}
												}
												tagihans.add(tagihan);
											}
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										}
									} else {
										// FIX BULANAN: tagihan LAMA yg nominalnya 0/null (mis. dibuat sync sebelumnya yg
										// bermasalah) dikoreksi agar SAMA dgn NominalBiaya — HANYA bila pembayaran tunggal,
										// belum dibayar, bukan override manual, & bukan 'bukan tagihan'.
										try {
											tagihan = pastikanNominalBulananSamaDenganNominalBiaya(session, tagihan,
													nominalBiayaSiswa);
										} catch (Exception eFixNom) { ais.common.ErrorAuditUtil.record(eFixNom, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtil.java:499");
										}
										tagihans.add(tagihan);
									}
								}
								cal.add(Calendar.MONTH, 1);
							}
						}

					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/TagihanUtil.java:514");
			} finally {
				closeSessionAndDisconnect(session);
			}
		}
		return TagihanUtilCalonSiswa.saring(tagihans);
	}

	public static List<Tagihan> doGenerateTagihanTahunan(Siswa siswa, PengaturanBiaya pengaturanBiaya,
			Integer tahunSampai) {
		DataKeanggotaanSiswa keanggotaan = ambilKeanggotaanSiswaTerbaru(siswa, pengaturanBiaya.getTahunAjaran());
		List<Long> kelases = keanggotaan.kelasIds;
		List<Long> asramas = keanggotaan.asramaIds;
		String kelasData = keanggotaan.namaKelas;
		List<Tagihan> tagihans = new ArrayList<Tagihan>();

		if (isSiswaMemenuhiSyarat(siswa, pengaturanBiaya, kelases, asramas, kelasData)) {
			Session session = null;
			try {
				// Pakai openSession DEDIKASI (bukan currentNativeSession). Alur generate tagihan ini
				// memanggil banyak helper (ambilNominalBiaya, Tagihan.ambilAtauBuat /
				// saveTagihanDenganKodeUnikAman, perbaikiJumlahAngsuran) yang di dalamnya bisa memanggil
				// HibernateUtil.closeSession() → menutup session ThreadLocal yang sedang dipakai di
				// tengah loop, memicu "Session is closed!" saat createCriteria/commit berikutnya. Session
				// dedikasi TIDAK tersimpan di ThreadLocal sehingga kebal terhadap penutupan itu. Ditutup
				// di finally (closeSessionAndDisconnect).
				session = HibernateUtil.openSession();
				@SuppressWarnings("unchecked")
				List<PengaturanBiayaItemBiaya> daftarBiayas = ConstantValues
						.simpleList(
								session.createCriteria(PengaturanBiayaItemBiaya.class)
										.add(Restrictions.eq("pengaturanBiaya", pengaturanBiaya)),
								PengaturanBiayaItemBiaya.class);
				List<Long> notPembayaran = new ArrayList<Long>();

				for (PengaturanBiayaItemBiaya pbItemBiaya : daftarBiayas) {
					try {
						NominalBiaya nominalBiayaSiswa = ambilNominalBiaya(pbItemBiaya, siswa, session);

						if (Tagihan.getBoleh(pengaturanBiaya, siswa, null, pbItemBiaya.getItemBiayaSekolah())
								&& nominalBiayaSiswa != null
								&& !Boolean.TRUE.equals(nominalBiayaSiswa.getBukanTagihan())
								&& nominalBiayaSiswa.getNominal() > 0.0) {

							for (int bayarKe = 1; bayarKe <= nominalBiayaSiswa.getDibayarSebayak(); bayarKe++) {
								String kodeUnik = Tagihan.genCode(nominalBiayaSiswa.getItemBiayaSekolah(),
										nominalBiayaSiswa.getPengaturanBiaya(), tahunSampai, siswa,
										nominalBiayaSiswa.getCalonSiswa(), bayarKe);

								Tagihan tagihan = (Tagihan) session.createCriteria(Tagihan.class)
										.add(Restrictions.eq("kodeUnik", kodeUnik)).setMaxResults(1).uniqueResult();

								if (tagihan == null) {
									try {
										Criteria critDetail = session.createCriteria(PembayaranSiswaDetail.class)
												.createAlias("tagihan", "tagihan")
												.add(Restrictions.eq("tagihan.bayarKe", bayarKe))
												.add(Restrictions.eq("nominalBiaya", nominalBiayaSiswa));

										if (!notPembayaran.isEmpty()) {
											critDetail.add(Restrictions.not(Restrictions.in("id", notPembayaran)));
										}

										PembayaranSiswaDetail pembayaranSiswaDetail = (PembayaranSiswaDetail) critDetail
												.add(Restrictions.eq("itemBiayaSekolah",
														pbItemBiaya.getItemBiayaSekolah()))
												.createCriteria("pembayaranSiswa").add(Restrictions.eq("siswa", siswa))
												.add(Restrictions.eq("jenisBiayaSekolah",
														pengaturanBiaya.getJenisBiayaSekolah()))
												.add(Restrictions.isNull("bulan"))
												.add(Restrictions.eq("tahun", tahunSampai)).setMaxResults(1)
												.addOrder(Order.desc("id")).uniqueResult();

										if (pembayaranSiswaDetail != null && pembayaranSiswaDetail.getId() != null) {
											notPembayaran.add(pembayaranSiswaDetail.getId());
										}

										if (pembayaranSiswaDetail == null
												|| pembayaranSiswaDetail.getTagihan() == null) {
											Double n = null;
											if (bayarKe > 1) {
												Number nm = (Number) session.createCriteria(Tagihan.class)
														.add(Restrictions.eq("nominalBiaya", nominalBiayaSiswa))
														.add(Restrictions.lt("bayarKe", bayarKe))
														.setProjection(Projections.sum("nominal")).uniqueResult();

												if (nm != null && nm.doubleValue() < nominalBiayaSiswa.getNominal()) {
													n = nominalBiayaSiswa.getNominal() - nm.doubleValue();
												}
											}

											tagihan = new Tagihan();
											tagihan.setNominalBiaya(nominalBiayaSiswa);
											tagihan.setTahunbulan(tahunSampai);
											tagihan.setBulan(null);
											tagihan.setTahun(tahunSampai);
											tagihan.setPembayaranSiswaDetail(pembayaranSiswaDetail);
											tagihan.setSiswa(siswa);
											tagihan.setItemBiayaSekolah(pbItemBiaya.getItemBiayaSekolah());
											tagihan.setBayarKe(bayarKe);
											tagihan.setNominal(n);

											tagihan = saveTagihanAman(session, tagihan);

											if (pembayaranSiswaDetail != null
													&& pembayaranSiswaDetail.getId() != null) {
												pembayaranSiswaDetail.setTagihan(tagihan);
												final PembayaranSiswaDetail psdUtkSimpan = pembayaranSiswaDetail;
												jalankanTulisTahanDeadlock(session, new UnitTulis() {
													@Override
													public void kerjakan(Session s) throws Exception {
														s.update(psdUtkSimpan);
													}
												});

												if (TagihanDiskonSiswaHelper.diskonTidakMemotongTagihan(tagihan)) {
													DaftarPengajuanTransfer.simpanDiskonPembayaran(tagihan);
												}
											}
											tagihans.add(tagihan);
										}
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}
								} else {
									tagihans.add(tagihan);
								}
							}
						}

						perbaikiJumlahAngsuran(session, nominalBiayaSiswa);

					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/TagihanUtil.java:652");
			} finally {
				closeSessionAndDisconnect(session);
			}
		}

		return TagihanUtilCalonSiswa.saring(tagihans);
	}

	public static List<Tagihan> doGenerateTagihanInsendentil(Siswa siswa, PengaturanBiaya pengaturanBiaya,
			boolean refresh) {
		DataKeanggotaanSiswa keanggotaan = ambilKeanggotaanSiswaTerbaru(siswa, pengaturanBiaya.getTahunAjaran());
		List<Long> kelases = keanggotaan.kelasIds;
		List<Long> asramas = keanggotaan.asramaIds;
		String kelasData = keanggotaan.namaKelas;

		List<Tagihan> tagihans = new ArrayList<Tagihan>();

		if (isSiswaMemenuhiSyarat(siswa, pengaturanBiaya, kelases, asramas, kelasData)) {
			Session session = null;
			try {
				// Pakai openSession DEDIKASI (bukan currentNativeSession). Alur generate tagihan ini
				// memanggil banyak helper (ambilNominalBiaya, Tagihan.ambilAtauBuat /
				// saveTagihanDenganKodeUnikAman, perbaikiJumlahAngsuran) yang di dalamnya bisa memanggil
				// HibernateUtil.closeSession() → menutup session ThreadLocal yang sedang dipakai di
				// tengah loop, memicu "Session is closed!" saat createCriteria/commit berikutnya. Session
				// dedikasi TIDAK tersimpan di ThreadLocal sehingga kebal terhadap penutupan itu. Ditutup
				// di finally (closeSessionAndDisconnect).
				session = HibernateUtil.openSession();
				@SuppressWarnings("unchecked")
				List<PengaturanBiayaItemBiaya> daftarBiayas = ConstantValues
						.simpleList(
								session.createCriteria(PengaturanBiayaItemBiaya.class)
										.add(Restrictions.eq("pengaturanBiaya", pengaturanBiaya)),
								PengaturanBiayaItemBiaya.class);

				for (PengaturanBiayaItemBiaya pbItemBiaya : daftarBiayas) {
					try {
						NominalBiaya nominalBiayaSiswa = ambilNominalBiaya(pbItemBiaya, siswa, session);

						if (Tagihan.getBoleh(pengaturanBiaya, siswa, null, pbItemBiaya.getItemBiayaSekolah())
								&& nominalBiayaSiswa != null
								&& !Boolean.TRUE.equals(nominalBiayaSiswa.getBukanTagihan())
								&& (nominalBiayaSiswa.getNominal() > 0 || nominalBiayaSiswa
										.getItemBiayaSekolah().getNilaiBiayaBisaDiubahSaatPembayaran())) {

							JenisBiayaSekolah jbs = nominalBiayaSiswa.getPengaturanBiaya().getJenisBiayaSekolah();
							Integer tahunbulan = nominalBiayaSiswa.getTahunbulan() != null
									? nominalBiayaSiswa.getTahunbulan()
									: PembayaranSiswa.convert(jbs.getUntukTahun(), jbs.getUntukBulan());

							for (int bayarKe = 1; bayarKe <= nominalBiayaSiswa.getDibayarSebayak(); bayarKe++) {
								Tagihan tagihan = Tagihan.ambilAtauBuat(session,
										nominalBiayaSiswa.getItemBiayaSekolah(), nominalBiayaSiswa.getPengaturanBiaya(),
										nominalBiayaSiswa.getSiswa(), nominalBiayaSiswa.getCalonSiswa(), bayarKe,
										nominalBiayaSiswa, tahunbulan, refresh);
								if (tagihan != null && tagihan.getId() != null) {
									tagihans.add(tagihan);
								}
							}
						}

						perbaikiJumlahAngsuran(session, nominalBiayaSiswa);

					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/TagihanUtil.java:722");
			} finally {
				closeSessionAndDisconnect(session);
			}
		}

		return TagihanUtilCalonSiswa.saring(tagihans);
	}

	@SuppressWarnings("unchecked")
	public static void doSinkronkanTagihanSiswa(final PengaturanBiaya pengaturanBiaya,
			PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya, Integer pembayaranTerakhir, Label label, Textbox nama,
			boolean refresh) {
		String nilaiNama = nama == null ? "" : nama.getValue();
		doSinkronkanTagihanSiswa(pengaturanBiaya, pengaturanBiayaItemBiaya, pembayaranTerakhir, label,
				nilaiNama, refresh);
	}

	@SuppressWarnings("unchecked")
	public static void doSinkronkanTagihanSiswa(final PengaturanBiaya pengaturanBiaya,
			PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya, Integer pembayaranTerakhir, Label label, String nama,
			boolean refresh) {
		label.setValue("Singkronisasi data tagihan");
		// Buang object Tagihan lama sebelum generate. Tanpa invalidasi ini, ambilAtauBuat()
		// dapat menemukan entity yang sudah berubah/terhapus dari cache dan melewati query DB.
		PengaturanBiaya.invalidasiCacheTagihan(pengaturanBiaya);
		Session session = null;
		List<Siswa> siswaInstanceList = new ArrayList<Siswa>();

		try {
			session = HibernateUtil.currentNativeSession();
			Criteria criteria = DetailTagihanSiswaHelper.initCriteriaDenganNama(session, pengaturanBiaya, null, nama,
					pengaturanBiayaItemBiaya, false, true);
			siswaInstanceList = ConstantValues.simpleList(criteria, Siswa.class);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/TagihanUtil.java:745");
		} finally {
			closeSessionAndDisconnect(session);
		}

		int size = siswaInstanceList.size();
		int indexKe = 0;
		String periode = pengaturanBiaya.getJenisBiayaSekolah().getPeriode();

		if (periode.equals("Bulanan")) {
			int mulai = getBulanMulai(pengaturanBiaya.getJenisBiayaSekolah());
			// Gunakan bulanSampai dari PengaturanBiaya (misal 202706) agar sinkronisasi
			// mencakup SELURUH periode, bukan hanya hari-ini + mulai bulan.
			// Format bulanSampai: YYYYMM (contoh 202706 = Juni 2027).
			// Tambah 1 bulan karena kondisi loop "while bulanTahunUtama > pembayaranTerakhir"
			// (strict >) agar bulan terakhir ikut diproses.
			int tahunSampai;
			int bulanSampai;
			if (pengaturanBiaya.getBulanSampai() != null && pengaturanBiaya.getBulanSampai() > 0) {
				int bs = pengaturanBiaya.getBulanSampai();
				int bsMonth = bs % 100;
				int bsYear = bs / 100;
				if (bsMonth < 12) {
					tahunSampai = bsYear;
					bulanSampai = bsMonth + 1;
				} else {
					tahunSampai = bsYear + 1;
					bulanSampai = 1;
				}
			} else {
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + mulai);
				tahunSampai = calendar.get(Calendar.YEAR);
				bulanSampai = calendar.get(Calendar.MONTH);
			}

			for (Siswa siswa : siswaInstanceList) {
				label.setValue("Memproses data tagihan " + siswa.getNomorInduk() + " " + siswa.getNama() + " ("
						+ Common.numberFormat.get().format((++indexKe) * 100.0 / size) + "%)");
				List<Tagihan> tags = doGenerateTagihanBulanan(siswa, pengaturanBiaya, tahunSampai, bulanSampai, mulai);
				Session innerSession = null;
				try {
					innerSession = HibernateUtil.currentNativeSession();
					for (Tagihan tagihan : tags) {
						innerSession.refresh(tagihan);
						// refresh memuat ulang nilai DB — koreksi bulan/tahun yang tak selaras
						// dengan tahunbulan SEBELUM update agar ikut tersimpan (self-heal).
						selaraskanBulanTahun(tagihan);
						Transaction tx = innerSession.beginTransaction();
						innerSession.update(tagihan);
						tx.commit();
					}
					hapusTagihanTidakValid(innerSession, siswa, pengaturanBiaya, tags);
					bersihkanTagihanNolDanNormalisasiAngsuran(innerSession, siswa, null, pengaturanBiaya, null);
					sinkronkanKelasTagihan(innerSession, siswa, pengaturanBiaya);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/TagihanUtil.java:799");
				} finally {
					closeSessionAndDisconnect(innerSession);
				}
			}
		} else if (periode.equals("Tahunan")) {
			int tahunSampai = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
			for (Siswa siswa : siswaInstanceList) {
				label.setValue("Memproses data tagihan " + siswa.getNomorInduk() + " " + siswa.getNama() + " ("
						+ Common.numberFormat.get().format((++indexKe) * 100.0 / size) + "%)");
				List<Tagihan> tags = doGenerateTagihanTahunan(siswa, pengaturanBiaya, tahunSampai);
				Session innerSession = null;
				try {
					innerSession = HibernateUtil.currentNativeSession();
					for (Tagihan tagihan : tags) {
						innerSession.refresh(tagihan);
						selaraskanBulanTahun(tagihan); // no-op utk format tahunan (tahunbulan=YYYY)
						Transaction tx = innerSession.beginTransaction();
						innerSession.update(tagihan);
						tx.commit();
					}
					hapusTagihanTidakValid(innerSession, siswa, pengaturanBiaya, tags);
					bersihkanTagihanNolDanNormalisasiAngsuran(innerSession, siswa, null, pengaturanBiaya, null);
					sinkronkanKelasTagihan(innerSession, siswa, pengaturanBiaya);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/TagihanUtil.java:822");
				} finally {
					closeSessionAndDisconnect(innerSession);
				}
			}
		} else { // Insidentil
			for (Siswa siswa : siswaInstanceList) {
				label.setValue("Memproses data tagihan " + siswa.getNomorInduk() + " " + siswa.getNama() + " ("
						+ Common.numberFormat.get().format((++indexKe) * 100.0 / size) + "%)");
				List<Tagihan> tags = doGenerateTagihanInsendentil(siswa, pengaturanBiaya, refresh);
				Session innerSession = null;
				try {
					innerSession = HibernateUtil.currentNativeSession();
					for (Tagihan tagihan : tags) {
						try {
							innerSession.refresh(tagihan);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtil.java:838");
						}
						Transaction tx = innerSession.beginTransaction();
						innerSession.update(tagihan);
						tx.commit();
					}

					if (pengaturanBiayaItemBiaya != null) {
						NominalBiaya nominalBiaya = TagihanUtil.ambilNominalBiaya(pengaturanBiayaItemBiaya, siswa,
								pembayaranTerakhir, innerSession);
						JenisBiayaSekolah jbs = nominalBiaya.getPengaturanBiaya().getJenisBiayaSekolah();
						Integer tahunbulan = nominalBiaya.getTahunbulan() != null ? nominalBiaya.getTahunbulan()
								: PembayaranSiswa.convert(jbs.getUntukTahun(), jbs.getUntukBulan());
						int index = nominalBiaya.getDibayarSebayak();
						Double total = 0.0;

						for (int bayarKe = 1; bayarKe <= index; bayarKe++) {
							Tagihan tagihan = Tagihan.ambilAtauBuat(nominalBiaya.getItemBiayaSekolah(),
									nominalBiaya.getPengaturanBiaya(), nominalBiaya.getSiswa(),
									nominalBiaya.getCalonSiswa(), bayarKe, nominalBiaya, tahunbulan,
									pengaturanBiayaItemBiaya);

							Double d = tagihan == null || !((tagihan.getAktif() && !tagihan.ambilBukanTagihanData())
									&& !tagihan.getNominalBiaya().getBukanTagihan()) ? 0.0 : tagihan.getNominal();
							total += d;
						}

						boolean adaPerubahan = true;
						if (Math.abs(nominalBiaya.getNominal() - total) > 0.1) {
							for (int bayarKe = 1; bayarKe <= index; bayarKe++) {
								Tagihan tagihan = Tagihan.ambilAtauBuat(nominalBiaya.getItemBiayaSekolah(),
										nominalBiaya.getPengaturanBiaya(), nominalBiaya.getSiswa(),
										nominalBiaya.getCalonSiswa(), bayarKe, nominalBiaya, tahunbulan,
										pengaturanBiayaItemBiaya);

								if (tagihan != null && tagihan.getNominal().intValue() == 0
										&& !Boolean.TRUE.equals(tagihan.getNonaktifManual())) {
									tagihan.setAktif(true);
									tagihan.setAktifkanmanual(true);
									tagihan.setNominal(nominalBiaya.getNominal() - total);
									tagihan.setNominalManual(nominalBiaya.getNominal() - total);
									Transaction tx2 = innerSession.beginTransaction();
									Common.refreshSaveOrUpdate(innerSession, tagihan);
									tx2.commit();
									adaPerubahan = false;
								}
							}

							if (adaPerubahan && (nominalBiaya.getNominal() - total) > 0.1) {
								index = index + 1;
								Tagihan tagihan = Tagihan.ambilAtauBuat(nominalBiaya.getItemBiayaSekolah(),
										nominalBiaya.getPengaturanBiaya(), nominalBiaya.getSiswa(),
										nominalBiaya.getCalonSiswa(), index, nominalBiaya, tahunbulan,
										pengaturanBiayaItemBiaya);

								tagihan.setAktif(true);
								tagihan.setAktifkanmanual(true);
								tagihan.setNominal(nominalBiaya.getNominal() - total);
								tagihan.setNominalManual(nominalBiaya.getNominal() - total);
								Transaction tx3 = innerSession.beginTransaction();
								Common.refreshSaveOrUpdate(innerSession, tagihan);
								tx3.commit();
							}
						}

						if (nominalBiaya != null) {
							try {
								innerSession.refresh(nominalBiaya);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtil.java:906");
							}
							nominalBiaya.setDibayarSebayakManual(index);
							nominalBiaya.setDibayarSebayak(index);
							Transaction tx4 = innerSession.beginTransaction();
							Common.refreshUpdate(innerSession, nominalBiaya);
							tx4.commit();
						}
					}
					hapusTagihanTidakValid(innerSession, siswa, pengaturanBiaya, tags);
					bersihkanTagihanNolDanNormalisasiAngsuran(innerSession, siswa, null, pengaturanBiaya, null);
					sinkronkanKelasTagihan(innerSession, siswa, pengaturanBiaya);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/TagihanUtil.java:917");
				} finally {
					closeSessionAndDisconnect(innerSession);
				}
			}
		}
		// Bangun ulang cache dari kondisi DB final agar grid dan proses pembayaran membaca
		// object yang sama dengan hasil sinkronisasi, termasuk penghapusan tagihan tidak valid.
		PengaturanBiaya.reloadTagihan(pengaturanBiaya, true);
		label.setValue("");
	}

	/**
	 * Sinkronkan tagihan untuk SATU siswa saja (dipakai setelah Reset per-baris).
	 * Setara dengan satu iterasi doSinkronkanTagihanSiswa tanpa Label progress.
	 */
	public static void sinkronkanSatuSiswa(Siswa siswa, PengaturanBiaya pengaturanBiaya) {
		if (siswa == null || pengaturanBiaya == null || pengaturanBiaya.getJenisBiayaSekolah() == null) return;
		String periode = pengaturanBiaya.getJenisBiayaSekolah().getPeriode();
		List<Tagihan> tags = new ArrayList<Tagihan>();
		if ("Bulanan".equals(periode)) {
			int mulai = getBulanMulai(pengaturanBiaya.getJenisBiayaSekolah());
			int tahunSampai, bulanSampai;
			if (pengaturanBiaya.getBulanSampai() != null && pengaturanBiaya.getBulanSampai() > 0) {
				int bs = pengaturanBiaya.getBulanSampai();
				int bsMonth = bs % 100;
				int bsYear = bs / 100;
				if (bsMonth < 12) { tahunSampai = bsYear; bulanSampai = bsMonth + 1; }
				else { tahunSampai = bsYear + 1; bulanSampai = 1; }
			} else {
				Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
				cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) + mulai);
				tahunSampai = cal.get(Calendar.YEAR);
				bulanSampai = cal.get(Calendar.MONTH);
			}
			tags = doGenerateTagihanBulanan(siswa, pengaturanBiaya, tahunSampai, bulanSampai, mulai);
		} else if ("Tahunan".equals(periode)) {
			int tahunSampai = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
			tags = doGenerateTagihanTahunan(siswa, pengaturanBiaya, tahunSampai);
		} else {
			tags = doGenerateTagihanInsendentil(siswa, pengaturanBiaya, true);
		}
		Session innerSession = null;
		try {
			innerSession = HibernateUtil.currentNativeSession();
			for (Tagihan tagihan : tags) {
				try { innerSession.refresh(tagihan); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtil.java:960");}
				Transaction tx = innerSession.beginTransaction();
				innerSession.update(tagihan);
				tx.commit();
			}
			hapusTagihanTidakValid(innerSession, siswa, pengaturanBiaya, tags);
			bersihkanTagihanNolDanNormalisasiAngsuran(innerSession, siswa, null, pengaturanBiaya, null);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/TagihanUtil.java:967");
		} finally {
			closeSessionAndDisconnect(innerSession);
		}
	}

	/**
	 * Hapus NominalBiaya + Tagihan yang belum dibayar untuk siswa/calonSiswa,
	 * lalu singkronkan ulang (jika siswa != null). Reusable dari DetailTagihanSiswaHelper dan PembayaranOnline.
	 */
	public static void resetNominalBiayaDanTagihan(NominalBiaya nb, Siswa siswa, CalonSiswa calonSiswa) {
		if (nb == null || nb.getPengaturanBiaya() == null || nb.getItemBiayaSekolah() == null) return;
		Session sessionReset = null;
		try {
			sessionReset = HibernateUtil.getSessionFactory().openSession();

			// 1. Hapus Tagihan yang belum ada pembayaran
			Criteria critTagihan = sessionReset
				.createCriteria(Tagihan.class)
				.createAlias("nominalBiaya", "nbDel")
				.add(Restrictions.eq("nbDel.pengaturanBiaya", nb.getPengaturanBiaya()))
				.add(Restrictions.eq("nbDel.itemBiayaSekolah", nb.getItemBiayaSekolah()))
				.add(Restrictions.isNull("pembayaranSiswaDetail"))
				.add(Restrictions.or(Restrictions.isNull("dibayar"), Restrictions.le("dibayar", 0.0)))
				.add(Restrictions.or(Restrictions.isNull("dibayarManual"), Restrictions.le("dibayarManual", 0.0)));
			if (siswa != null) {
				critTagihan.add(Restrictions.eq("siswa", siswa));
			} else if (calonSiswa != null) {
				critTagihan.add(Restrictions.eq("calonSiswa", calonSiswa));
			}
			@SuppressWarnings("unchecked")
			List<Tagihan> tagihanDel = critTagihan.list();
			for (Tagihan t : tagihanDel) {
				if (t.getPembayaranSiswaDetail() != null) continue;
				if (t.getDibayar() != null && t.getDibayar() > 0.1) continue;
				if (t.getDibayarManual() != null && t.getDibayarManual() > 0.1) continue;
				hapusTagihanAman(sessionReset, t);
			}

			sessionReset.clear();

			// 2. Hapus NominalBiaya yang tidak punya tagihan berbayar
			Criteria critNb = sessionReset
				.createCriteria(NominalBiaya.class)
				.add(Restrictions.eq("pengaturanBiaya", nb.getPengaturanBiaya()))
				.add(Restrictions.eq("itemBiayaSekolah", nb.getItemBiayaSekolah()));
			if (siswa != null) {
				critNb.add(Restrictions.eq("siswa", siswa));
			} else if (calonSiswa != null) {
				critNb.add(Restrictions.eq("calonSiswa", calonSiswa));
			}
			@SuppressWarnings("unchecked")
			List<NominalBiaya> nbListDel = critNb.list();
			for (NominalBiaya nbRow : nbListDel) {
				Number paidCnt = (Number) sessionReset
					.createCriteria(Tagihan.class)
					.add(Restrictions.eq("nominalBiaya", nbRow))
					.add(Restrictions.disjunction()
						.add(Restrictions.isNotNull("pembayaranSiswaDetail"))
						.add(Restrictions.gt("dibayar", 0.0))
						.add(Restrictions.gt("dibayarManual", 0.0)))
					.setProjection(Projections.rowCount())
					.uniqueResult();
				if (paidCnt != null && paidCnt.longValue() > 0) continue;
				try {
					Transaction txNb = sessionReset.beginTransaction();
					sessionReset.delete(nbRow);
					txNb.commit();
				} catch (Exception eDel) {
					try { sessionReset.clear(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtil.java:1042");}
				}
			}
		} catch (Exception eRes) {
			eRes.printStackTrace(); ais.common.ErrorAuditUtil.record(eRes, "auto-audit src/ais/action/master/sekolah/helper/TagihanUtil.java:1046");
		} finally {
			if (sessionReset != null && sessionReset.isOpen()) {
				try { sessionReset.clear(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtil.java:1049");}
				try { sessionReset.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtil.java:1050");}
			}
		}
		if (siswa != null) {
			sinkronkanSatuSiswa(siswa, nb.getPengaturanBiaya());
			Session sessionBersih = null;
			try {
				sessionBersih = HibernateUtil.getSessionFactory().openSession();
				bersihkanTagihanNolDanNormalisasiAngsuran(sessionBersih, siswa, null, nb.getPengaturanBiaya(),
						nb.getItemBiayaSekolah());
			} finally {
				closeSessionAndDisconnect(sessionBersih);
			}
		}
	}

	/**
	 * Hapus SEMUA NominalBiaya + Tagihan belum dibayar dalam satu PengaturanBiaya untuk siswa/calonSiswa,
	 * lalu singkronkan ulang. Digunakan untuk Reset per-Grup di PembayaranOnline.
	 */
	public static void resetSemuaTagihanDalamPB(PengaturanBiaya pb, Siswa siswa, CalonSiswa calonSiswa) {
		if (pb == null || (siswa == null && calonSiswa == null)) return;
		Session sessionReset = null;
		try {
			sessionReset = HibernateUtil.getSessionFactory().openSession();

			// 1. Hapus semua Tagihan belum dibayar untuk PB ini
			Criteria critTagihan = sessionReset
				.createCriteria(Tagihan.class)
				.createAlias("nominalBiaya", "nbDel")
				.add(Restrictions.eq("nbDel.pengaturanBiaya", pb))
				.add(Restrictions.isNull("pembayaranSiswaDetail"))
				.add(Restrictions.or(Restrictions.isNull("dibayar"), Restrictions.le("dibayar", 0.0)))
				.add(Restrictions.or(Restrictions.isNull("dibayarManual"), Restrictions.le("dibayarManual", 0.0)));
			if (siswa != null) {
				critTagihan.add(Restrictions.eq("siswa", siswa));
			} else if (calonSiswa != null) {
				critTagihan.add(Restrictions.eq("calonSiswa", calonSiswa));
			}
			@SuppressWarnings("unchecked")
			List<Tagihan> tagihanDel = critTagihan.list();
			for (Tagihan t : tagihanDel) {
				if (t.getPembayaranSiswaDetail() != null) continue;
				if (t.getDibayar() != null && t.getDibayar() > 0.1) continue;
				if (t.getDibayarManual() != null && t.getDibayarManual() > 0.1) continue;
				hapusTagihanAman(sessionReset, t);
			}

			sessionReset.clear();

			// 2. Hapus semua NominalBiaya tanpa tagihan berbayar
			Criteria critNb = sessionReset
				.createCriteria(NominalBiaya.class)
				.add(Restrictions.eq("pengaturanBiaya", pb));
			if (siswa != null) {
				critNb.add(Restrictions.eq("siswa", siswa));
			} else if (calonSiswa != null) {
				critNb.add(Restrictions.eq("calonSiswa", calonSiswa));
			}
			@SuppressWarnings("unchecked")
			List<NominalBiaya> nbListDel = critNb.list();
			for (NominalBiaya nbRow : nbListDel) {
				Number paidCnt = (Number) sessionReset
					.createCriteria(Tagihan.class)
					.add(Restrictions.eq("nominalBiaya", nbRow))
					.add(Restrictions.disjunction()
						.add(Restrictions.isNotNull("pembayaranSiswaDetail"))
						.add(Restrictions.gt("dibayar", 0.0))
						.add(Restrictions.gt("dibayarManual", 0.0)))
					.setProjection(Projections.rowCount())
					.uniqueResult();
				if (paidCnt != null && paidCnt.longValue() > 0) continue;
				try {
					Transaction txNb = sessionReset.beginTransaction();
					sessionReset.delete(nbRow);
					txNb.commit();
				} catch (Exception eDel) {
					try { sessionReset.clear(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtil.java:1125");}
				}
			}
		} catch (Exception eRes) {
			eRes.printStackTrace(); ais.common.ErrorAuditUtil.record(eRes, "auto-audit src/ais/action/master/sekolah/helper/TagihanUtil.java:1129");
		} finally {
			if (sessionReset != null && sessionReset.isOpen()) {
				try { sessionReset.clear(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtil.java:1132");}
				try { sessionReset.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtil.java:1133");}
			}
		}
		if (siswa != null) {
			sinkronkanSatuSiswa(siswa, pb);
			Session sessionBersih = null;
			try {
				sessionBersih = HibernateUtil.getSessionFactory().openSession();
				bersihkanTagihanNolDanNormalisasiAngsuran(sessionBersih, siswa, null, pb, null);
			} finally {
				closeSessionAndDisconnect(sessionBersih);
			}
		}
	}

	/**
	 * Hapus tagihan yang tidak valid (tidak masuk daftar generate terbaru) untuk
	 * satu siswa dalam satu PengaturanBiaya. Tagihan yang sudah dibayar TIDAK pernah dihapus.
	 *
	 * Tagihan "tidak valid" = tagihan yang ada di DB untuk kombinasi siswa+PengaturanBiaya,
	 * namun TIDAK ada di daftar {@code tagihanValid} yang dihasilkan generate terbaru.
	 * Ini mencakup: bukanTagihan=true, nominal=0, di luar range bulan, dan duplikat dari PB lain.
	 */
	@SuppressWarnings("unchecked")
	private static void hapusTagihanTidakValid(Session session, Siswa siswa,
			PengaturanBiaya pengaturanBiaya, List<Tagihan> tagihanValid) {
		if (session == null || !session.isOpen() || siswa == null || pengaturanBiaya == null) return;
		try {
			// Kumpulkan kodeUnik dari tagihan yang VALID (tidak boleh dihapus)
			Set<String> validKodeUnik = new HashSet<String>();
			for (Tagihan t : tagihanValid) {
				if (t != null && t.getKodeUnik() != null) {
					validKodeUnik.add(t.getKodeUnik());
				}
			}

			// Ambil semua tagihan untuk siswa+PB ini yang belum dibayar (kriteria DB)
			List<Tagihan> kandidat = session.createCriteria(Tagihan.class)
					.createAlias("nominalBiaya", "nb")
					.add(Restrictions.eq("nb.pengaturanBiaya", pengaturanBiaya))
					.add(Restrictions.eq("siswa", siswa))
					.add(Restrictions.isNull("pembayaranSiswaDetail"))
					.add(Restrictions.or(
							Restrictions.isNull("dibayar"),
							Restrictions.le("dibayar", 0.0)))
					.add(Restrictions.or(
							Restrictions.isNull("dibayarManual"),
							Restrictions.le("dibayarManual", 0.0)))
					.list();

			for (Tagihan t : kandidat) {
				// Perlindungan ganda: jangan hapus jika ada tanda pembayaran apapun
				if (t.getPembayaranSiswaDetail() != null) continue;
				if (t.getDibayar() != null && t.getDibayar() > 0.1) continue;
				if (t.getDibayarManual() != null && t.getDibayarManual() > 0.1) continue;

				// Jika kodeUnik ada di daftar valid → lewati
				String ku = t.getKodeUnik();
				if (ku != null && validKodeUnik.contains(ku)) continue;

				// Tagihan tidak valid + belum dibayar → hapus
				try {
					Transaction txDel = session.beginTransaction();
					session.delete(t);
					txDel.commit();
				} catch (Exception eDel) {
					try { session.clear(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtil.java:1192");}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public static NominalBiaya ambilNominalBiaya(PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya, Siswa siswa,
			Session session) {
		Integer tahunbulan = null;
		return ambilNominalBiaya(pengaturanBiayaItemBiaya, siswa, tahunbulan, session);
	}

	public static NominalBiaya ambilNominalBiaya(PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya, Siswa siswa,
			Integer tahunbulan, Session session) {
		// Guard: jika session null/closed (misal thread latar session ThreadLocal ditutup
		// helper lain), buka session baru agar createCriteria tidak throw "Session is closed!".
		boolean ownSession = (session == null || !session.isOpen());
		if (ownSession) {
			session = HibernateUtil.openSession();
		}
		try {
			return doAmbilNominalBiaya(pengaturanBiayaItemBiaya, siswa, tahunbulan, session);
		} finally {
			if (ownSession && session != null && session.isOpen()) {
				try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtil.java:1218");}
			}
		}
	}

	@SuppressWarnings("deprecation")
	private static NominalBiaya doAmbilNominalBiaya(PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya, Siswa siswa,
			Integer tahunbulan, Session session) {
		String kodeUnik = NominalBiaya.genCode(pengaturanBiayaItemBiaya.getItemBiayaSekolah(),
				pengaturanBiayaItemBiaya.getPengaturanBiaya(), siswa, null, tahunbulan);

		NominalBiaya nominalBiayaSiswa = (NominalBiaya) session.createCriteria(NominalBiaya.class)
				.add(Restrictions.eq("kodeUnik", kodeUnik)).addOrder(Order.asc("id")).setMaxResults(1).uniqueResult();

		if (nominalBiayaSiswa == null) {
			Criteria crit = session.createCriteria(NominalBiaya.class)
					.add(Restrictions.eq("pengaturanBiaya", pengaturanBiayaItemBiaya.getPengaturanBiaya()))
					.add(Restrictions.eq("itemBiayaSekolah", pengaturanBiayaItemBiaya.getItemBiayaSekolah()));

			if (tahunbulan == null || tahunbulan < 2100) {
				crit.add(Restrictions.sqlRestriction("true"));
			} else {
				crit.add(Restrictions.eq("tahunbulan", tahunbulan));
			}

			nominalBiayaSiswa = (NominalBiaya) crit.add(Restrictions.eq("siswa", siswa)).setMaxResults(1)
					.uniqueResult();
		}

		// Step 3 (last resort): cari NominalBiaya APAPUN untuk kombinasi
		// (pengaturanBiaya, itemBiayaSekolah, siswa) tanpa filter tahunbulan/status.
		// Satu kombinasi PB+item+siswa = satu NominalBiaya; reuse record lama
		// meskipun bukanTagihan=true, aktif=false, atau tahunbulan berbeda.
		if (nominalBiayaSiswa == null) {
			nominalBiayaSiswa = (NominalBiaya) session.createCriteria(NominalBiaya.class)
					.add(Restrictions.eq("pengaturanBiaya", pengaturanBiayaItemBiaya.getPengaturanBiaya()))
					.add(Restrictions.eq("itemBiayaSekolah", pengaturanBiayaItemBiaya.getItemBiayaSekolah()))
					.add(Restrictions.eq("siswa", siswa))
					.addOrder(Order.asc("id"))
					.setMaxResults(1).uniqueResult();
			if (nominalBiayaSiswa != null) {
				System.out.println("[TagihanUtil][doAmbilNominalBiaya] step3-reuse NominalBiaya id="
						+ nominalBiayaSiswa.getId() + " bukanTagihan=" + nominalBiayaSiswa.getBukanTagihan()
						+ " tahunbulan=" + nominalBiayaSiswa.getTahunbulan()
						+ " (search-tahunbulan=" + tahunbulan + " kodeUnik=" + kodeUnik + ")");
			}
		}

		if (nominalBiayaSiswa == null) {
			System.out.println("[TagihanUtil][doAmbilNominalBiaya] creating NEW NominalBiaya"
					+ " kodeUnik=" + kodeUnik + " tahunbulan=" + tahunbulan
					+ " siswa=" + (siswa != null ? siswa.getId() : null)
					+ " pb=" + (pengaturanBiayaItemBiaya.getPengaturanBiaya() != null
							? pengaturanBiayaItemBiaya.getPengaturanBiaya().getId() : null));
			nominalBiayaSiswa = new NominalBiaya();
			nominalBiayaSiswa.setTahunbulan(tahunbulan);
			nominalBiayaSiswa.setNominal(pengaturanBiayaItemBiaya.getDefaultBiaya());
			nominalBiayaSiswa.setItemBiayaSekolah(pengaturanBiayaItemBiaya.getItemBiayaSekolah());
			nominalBiayaSiswa.setPengaturanBiaya(pengaturanBiayaItemBiaya.getPengaturanBiaya());
			nominalBiayaSiswa.setSiswa(siswa);
			nominalBiayaSiswa.setPengaturanBiayaItemBiaya(pengaturanBiayaItemBiaya);
			Transaction tx = session.beginTransaction();
			session.save(nominalBiayaSiswa);
			tx.commit();
		}

		if (nominalBiayaSiswa.getPengaturanBiayaItemBiaya() == null) {
			nominalBiayaSiswa.setPengaturanBiayaItemBiaya(pengaturanBiayaItemBiaya);
			Transaction tx = session.beginTransaction();
			session.update(nominalBiayaSiswa);
			tx.commit();
		}

		// Daftarkan sebagai canonical: semua pemegang NominalBiaya dengan ID ini
		// otomatis melihat state terbaru (termasuk bukanTagihan) tanpa re-query DB.
		return ais.common.EntityIdentityMap.canonical(nominalBiayaSiswa);
	}

	public static List<Tagihan> getTagihan(JenisBiayaSekolah jenisBiaya, PengaturanBiaya pengaturanData, Siswa s,
			Integer bulan, Integer tahun, boolean refresh) {
		return getTagihan(jenisBiaya, pengaturanData, s, bulan, tahun, null, false, refresh);
	}

	@SuppressWarnings("unchecked")
	public static List<Tagihan> getTagihan(JenisBiayaSekolah jenisBiaya, PengaturanBiaya pengaturanData, Siswa s,
			Integer bulan, Integer tahun, String ta, boolean tampilSemua, boolean refresh) {

		if (!Tagihan.getBoleh(pengaturanData, s, null)) {
			return new ArrayList<Tagihan>();
		}

		int mulai = getBulanMulai(jenisBiaya);
		Integer tahunbulan = PembayaranSiswa.convert(tahun, bulan);
		List<Tagihan> tagihans = new ArrayList<Tagihan>();
		Session session = null;

		try {
			session = HibernateUtil.openSession();
			Criteria criteria = batasiPadaItemYangMasihDiatur(session.createCriteria(Tagihan.class)
					.createAlias("itemBiayaSekolah", "itemBiayaSekolah")
					.createAlias("nominalBiaya", "nb_filter")
					.add(Restrictions.eq("itemBiayaSekolah.aktif", true))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.or(Restrictions.isNull("nb_filter.bukanTagihan"),
							Restrictions.eq("nb_filter.bukanTagihan", false)))
					.add(Restrictions.eq("siswa", s)));

			if (pengaturanData != null) {
				criteria.add(Restrictions.eq("pengaturanBiaya", pengaturanData));
			} else if (jenisBiaya != null) {
				criteria.createAlias("pengaturanBiaya", "pengaturanBiaya").addOrder(Order.desc("pengaturanBiaya.id"))
						.add(Restrictions.eq("pengaturanBiaya.jenisBiayaSekolah", jenisBiaya))
						.add(Restrictions.eq("pengaturanBiaya.sekolah", s.getSekolah()))
						.add(Restrictions.eq("pengaturanBiaya.statusAwalSiswa", s.getStatusAwalSiswa()))
						.add(s.getPenjurusanSekolah() == null ? Restrictions.isNull("pengaturanBiaya.penjurusanSekolah")
								: Restrictions.or(Restrictions.isNull("pengaturanBiaya.penjurusanSekolah"),
										Restrictions.eq("pengaturanBiaya.penjurusanSekolah", s.getPenjurusanSekolah())))
						.add(ta == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("pengaturanBiaya.tahunAjaran", ta));
			} else {
				criteria.add(Restrictions.sqlRestriction("false"));
			}

			criteria.add(Restrictions.or(Restrictions.gt("nominal", 0.1),
					Restrictions.eq("itemBiayaSekolah.nilaiBiayaBisaDiubahSaatPembayaran", true)))
					.addOrder(Order.asc("itemBiayaSekolah.nama")).addOrder(Order.asc("tahunbulan"))
					.addOrder(Order.asc("bayarKe"));

			if (jenisBiaya != null) {
				if (jenisBiaya.getPeriode().equals("Bulanan")) {
					criteria.add(Restrictions.le("tahunbulan", tahunbulan));
				} else if (jenisBiaya.getPeriode().equals("Tahunan")) {
					criteria.add(Restrictions.le("tahun", tahun));
				}
			}

			if (ta == null || tampilSemua) {
				if (tampilSemua || (jenisBiaya != null && (jenisBiaya.getPeriode().equalsIgnoreCase("Bulanan")
						|| jenisBiaya.getPeriode().equalsIgnoreCase("Tahunan")))) {
					criteria.add(
							Restrictions.or(
									Restrictions.and(Restrictions.isNotNull("pembayaranBerakhirPada"),
											Restrictions.sqlRestriction(
													"this_.pembayaran_berakhir_pada < CURRENT_TIMESTAMP")),
									Restrictions.isNull("pembayaranSiswaDetail")));
				}
			}

			tagihans = criteria.list();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/TagihanUtil.java:1368");
		} finally {
			closeSessionAndDisconnect(session);
		}

		// Filter object-level: buang tagihan yang nominalBiaya-nya bukanTagihan=true
		List<Tagihan> tagihansFiltered = new ArrayList<Tagihan>();
		for (Tagihan t : tagihans) {
			NominalBiaya nbCheck = t.getNominalBiaya();
			if (nbCheck != null && Boolean.TRUE.equals(nbCheck.getBukanTagihan())) continue;
			tagihansFiltered.add(t);
		}
		tagihans = tagihansFiltered;

		List<NominalBiaya> nominalsTagihan = new ArrayList<NominalBiaya>();
		for (Tagihan tagihan : tagihans) {
			NominalBiaya nb = tagihan.getNominalBiaya();
			if (nb != null && tagihan.getBayarKe() <= nb.getDibayarSebayak()) {
				boolean ubahPembayaran = nb.getItemBiayaSekolah().getNilaiBiayaBisaDiubahSaatPembayaran()
						|| tagihan.getNominal() > 0.1;

				if (jenisBiaya != null && jenisBiaya.getPeriode().equalsIgnoreCase("Bulanan")) {
					// bulanMulai digunakan hanya untuk PEMBANGKITAN tagihan (doGenerateTagihanBulanan),
					// bukan untuk menyembunyikan tagihan yang sudah ada di DB.
					Integer bulanSampai = nb.getPengaturanBiaya().getBulanSampai();

					if (bulanSampai != null && tagihan.getTahunbulan() > bulanSampai)
						break;

					if (tahunbulan >= tagihan.getTahunbulan() && ubahPembayaran) {
						nominalsTagihan.add(nb);
					}
				} else if (jenisBiaya != null && jenisBiaya.getPeriode().equalsIgnoreCase("Tahunan")) {
					if (tahun >= tagihan.getTahun() && ubahPembayaran) {
						nominalsTagihan.add(nb);
					}
				} else {
					if (ubahPembayaran) {
						nominalsTagihan.add(nb);
					}
				}
			}
		}

		if (nominalsTagihan.isEmpty()) {
			// Jangan generate tagihan baru untuk siswa yang sudah keluar di bulan setelah lulus
			if (s != null && s.getStatusKeluar() != null) {
				try {
					Integer lulusYYYYMM = null;
					if (s.getTanggalLulus() != null) {
						Calendar calLulus = Calendar.getInstance();
						calLulus.setTime(s.getTanggalLulus());
						lulusYYYYMM = calLulus.get(Calendar.YEAR) * 100
								+ (calLulus.get(Calendar.MONTH) + 1);
					} else if (s.getTahunLulus() != null) {
						lulusYYYYMM = s.getTahunLulus() * 100 + 12;
					}
					Integer reqYYYYMM = PembayaranSiswa.convert(tahun, bulan);
					if (lulusYYYYMM != null && reqYYYYMM != null && reqYYYYMM > lulusYYYYMM) {
						return tagihans; // jangan generate tagihan post-lulus
					}
				} catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtil.java:1429");
				}
			}
			Session innerSession = null;
			try {
				innerSession = HibernateUtil.openSession();
				PengaturanBiaya pengaturanBiaya = pengaturanData != null ? pengaturanData
						: (PengaturanBiaya) ConstantValues.simpleObject(
								innerSession.createCriteria(PengaturanBiaya.class)
										.add(Restrictions.or(Restrictions.eq("aktif", true),
												Restrictions.isNull("aktif")))
										.add(s.getPenjurusanSekolah() == null ? Restrictions.sqlRestriction("true")
												: Restrictions.or(Restrictions.isNull("penjurusanSekolah"),
														Restrictions.eq("penjurusanSekolah", s.getPenjurusanSekolah())))
										.add(Restrictions.or(Restrictions.isNull("statusAwalSiswa"),
												Restrictions.eq("statusAwalSiswa", s.getStatusAwalSiswa())))

										.add(Restrictions.eq("jenisBiayaSekolah", jenisBiaya))
										.add(Restrictions.eq("sekolah", s.getSekolah()))
										.add(ta == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("tahunAjaran", ta))
										.add(Restrictions.or(Restrictions.eq("tahunAngkatan", 0),
												Restrictions.eq("tahunAngkatan", s.getTahunMasuk())))
										.setMaxResults(1),
								PengaturanBiaya.class);

				if (pengaturanBiaya != null) {
					// Serialisasi generate PER-SISWA (anti-deadlock KE-1/2/3): request untuk siswa yang sama
					// diantrikan FIFO sehingga tidak membangkitkan + menulis tagihan secara paralel.
					ReentrantLock kunciSiswaGenerate = kunciGenerateUntukSiswa(s);
					boolean terkunciGenerate = false;
					try {
						try {
							terkunciGenerate = kunciSiswaGenerate.tryLock(DETIK_MAKS_ANTRI_GENERATE, TimeUnit.SECONDS);
						} catch (InterruptedException ieGen) {
							Thread.currentThread().interrupt();
						}
						if (jenisBiaya != null && jenisBiaya.getPeriode().equalsIgnoreCase("Bulanan")) {
							tagihans = doGenerateTagihanBulanan(s, pengaturanBiaya, tahun, bulan, mulai);
						} else if (jenisBiaya != null && jenisBiaya.getPeriode().equalsIgnoreCase("Tahunan")) {
							tagihans = doGenerateTagihanTahunan(s, pengaturanBiaya, tahun);
						} else {
							tagihans = doGenerateTagihanInsendentil(s, pengaturanBiaya, refresh);
						}
					} finally {
						if (terkunciGenerate) {
							try { kunciSiswaGenerate.unlock(); } catch (Exception exUnlockGen) { ais.common.ErrorAuditUtil.record(exUnlockGen, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtil.java:1475");}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/TagihanUtil.java:1480");
			} finally {
				closeSessionAndDisconnect(innerSession);
			}
		}

		boolean tagihanDIbuatOtomatisMenghitungSisa = Common.bolehKonfigurasi("tagihan_dibuat_otomatis_menghitung_sisa", Konfigurasi.TIDAK_AKTIF);

		if (tagihanDIbuatOtomatisMenghitungSisa && tagihans != null && !tagihans.isEmpty()) {
			// ===================================================================================
			// FITUR BARU: AUTO-BALANCING MENGGUNAKAN OOP (nb.ambilTagihans)
			// Hanya menambahkan Tagihan baru (Shortfall) ke dalam List eksisting.
			// ===================================================================================
			java.util.Set<Long> processedNbIds = new java.util.HashSet<Long>();
			java.util.Set<Long> existingTagihanIds = new java.util.HashSet<Long>();
			List<Tagihan> tagihanBaruDitambahkan = new ArrayList<Tagihan>();

			// 1. Kumpulkan semua ID Tagihan yang sudah eksisting di list awal
			for (Tagihan t : tagihans) {
				if (t != null && t.getId() != null) {
					existingTagihanIds.add(t.getId());
				}
			}

			// Gunakan perulangan index-based atau foreach yang aman
			for (int i = 0; i < tagihans.size(); i++) {
				Tagihan t = tagihans.get(i);
				if (t == null) continue;

				NominalBiaya nb = null;
				try {
					nb = t.getNominalBiaya();
				} catch (Exception e) {
					nb = null; // Failsafe untuk LazyInitialization / Session putus
				}

				if (nb != null && nb.getId() != null) {
					// Cegah eksekusi berulang untuk NominalBiaya yang sama
					if (!processedNbIds.contains(nb.getId())) {
						processedNbIds.add(nb.getId());

						// Panggil method ajaib OOP dari NominalBiaya
						List<Tagihan> tgs = nb.ambilTagihans();
						if (tgs != null && !tgs.isEmpty()) {
							// Filter: Hanya ambil Tagihan yang belum ada di list awal
							for (Tagihan tg : tgs) {
								if (tg != null) {
									// Cek apakah ini Tagihan baru (ID belum terdaftar, atau ID masih null karena baru dicreate)
									if (tg.getId() == null || !existingTagihanIds.contains(tg.getId())) {
										if (!tagihanBaruDitambahkan.contains(tg)) {
											tagihanBaruDitambahkan.add(tg);
											if (tg.getId() != null) {
												existingTagihanIds.add(tg.getId()); // Tandai agar tidak double
											}
										}
									}
								}
							}
						}
					}
				}
			}

			// 2. Jika terdapat penambahan Tagihan baru, masukkan ke list aslinya
			if (!tagihanBaruDitambahkan.isEmpty()) {
				tagihans.addAll(tagihanBaruDitambahkan);
			}
			// ===================================================================================
		}

		// === CEGAH PEMBAYARAN DOBEL ATAS TAGIHAN BULAN/ITEM YANG SAMA ===
		// Bila siswa SUDAH MELUNASI suatu tagihan (item + bulan), tagihan DUPLIKAT (baris berbeda
		// untuk item+bulan yang sama yang belum bertaut pembayaran) tidak boleh ikut ditampilkan/
		// diproses lagi. Kumpulkan "slot" yang sudah lunas lalu disaring di bawah.
		boolean cegahPembayaranDobel = Common.bolehKonfigurasi("tagihan_cegah_pembayaran_dobel_sudah_lunas");
		java.util.Set<String> slotSudahLunas = cegahPembayaranDobel ? kumpulkanSlotSudahLunas(s)
				: new java.util.HashSet<String>();

		List<Tagihan> tagihanbaru = new ArrayList<Tagihan>();
		for (Tagihan tagihan : tagihans) {
			NominalBiaya nb = tagihan.getNominalBiaya();
			if (nb != null && tagihan.getBayarKe() <= nb.getDibayarSebayak()) {
				try {
					if (jenisBiaya != null && jenisBiaya.getPeriode().equalsIgnoreCase("Bulanan")) {
						if (tagihan.getTahunbulan() == null) {
							continue;
						}
						// bulanMulai hanya mengontrol PEMBANGKITAN tagihan; tagihan yang sudah ada di
						// DB (termasuk yang dibuat admin) tetap ditampilkan agar bisa dibayar.
						Integer bSampai = nb.getPengaturanBiaya().getBulanSampai();
						if (bSampai != null && tagihan.getTahunbulan() > bSampai)
							break;
					}
				}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtil.java:1573");
					// TODO: handle exception
				}

				boolean bisaUbah = nb.getItemBiayaSekolah().getNilaiBiayaBisaDiubahSaatPembayaran();
				if (tagihan.getPembayaranSiswaDetail() == null) {
					// Lewati bila slot (item+bulan) ini sudah LUNAS dibayar di pembayaran lain →
					// mencegah tagihan duplikat ikut diproses & pembayaran dobel siswa.
					String slot = slotKeyTagihan(tagihan);
					if (cegahPembayaranDobel && slot != null && slotSudahLunas.contains(slot)) {
						continue;
					}
					if (bisaUbah || tagihan.getNominal() > 0.1) {
						tagihanbaru.add(tagihan);
					}
				} else {
					if (bisaUbah && tagihan.getNominal().intValue() == 0) {
						tagihanbaru.add(tagihan);
					}
				}
			}
		}

		// Sinkronisasi diskon tagihan dipindahkan ke helper reusable.
		// Helper ini memakai session terisolasi dan tidak memakai ConstantValues.simpleList(),
		// sehingga DiskonSiswa tidak menjadi proxy detach saat dibaca dari background thread.
		TagihanDiskonSiswaHelper.sinkronkanDiskon(tagihanbaru);

		// SELF-HEAL: selaraskan kolom bulan/tahun dengan tahunbulan (sumber kebenaran) —
		// memperbaiki data lama yang tersimpan tidak konsisten (mis. tahun=2026 padahal
		// tahunbulan=202706/Juni 2027) setiap kali tagihan dimuat untuk ditampilkan.
		selaraskanBulanTahunDenganTahunbulan(tagihanbaru);

		return TagihanUtilCalonSiswa.saring(tagihanbaru);
	}

	/**
	 * Menyelaraskan kolom {@code bulan} dan {@code tahun} sebuah {@link Tagihan} dengan
	 * {@code tahunbulan}-nya. Untuk tagihan berkala bulanan, {@code tahunbulan} berformat
	 * YYYYMM (contoh {@code 202706} = Juni 2027) dan merupakan <b>sumber kebenaran</b> —
	 * dipakai kodeUnik, pencarian pembayaran, dan seluruh proses sinkronisasi. Kolom
	 * {@code bulan}/{@code tahun} hanyalah turunan untuk tampilan/laporan, tetapi data
	 * lama bisa tersimpan tidak konsisten (mis. {@code tahun} diisi tahun ajaran mulai
	 * [2026] padahal {@code tahunbulan}=202706 berarti kalender 2027 — kasus SPP semester
	 * genap TA 2026/2027). Nilai {@code tahunbulan} ≤ 2100 berarti format tahunan (YYYY)
	 * dan tidak disentuh.
	 *
	 * @return true bila ada kolom yang dikoreksi (objek berubah, belum tersimpan)
	 */
	static boolean selaraskanBulanTahun(Tagihan tagihan) {
		try {
			if (tagihan == null || tagihan.getTahunbulan() == null) return false;
			int tb = tagihan.getTahunbulan();
			if (tb <= 2100) return false; // format tahunan (YYYY), bulan memang null
			int th = tb / 100;
			int bl = tb % 100;
			if (bl < 1 || bl > 12) return false;
			boolean berubah = false;
			if (tagihan.getTahun() == null || tagihan.getTahun().intValue() != th) {
				tagihan.setTahun(th);
				berubah = true;
			}
			if (tagihan.getBulan() == null || tagihan.getBulan().intValue() != bl) {
				tagihan.setBulan(bl);
				berubah = true;
			}
			return berubah;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Varian daftar dari {@link #selaraskanBulanTahun(Tagihan)}: koreksi yang ditemukan
	 * langsung dipersistenkan pada session dedikasi (satu transaksi), sehingga data lama
	 * yang tidak konsisten tersembuhkan permanen begitu tagihan dimuat.
	 */
	private static void selaraskanBulanTahunDenganTahunbulan(List<Tagihan> tagihans) {
		if (tagihans == null || tagihans.isEmpty()) return;
		List<Tagihan> perluDisimpan = new ArrayList<Tagihan>();
		for (Tagihan t : tagihans) {
			try {
				if (t != null && t.getId() != null && selaraskanBulanTahun(t)) {
					perluDisimpan.add(t);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtil.java:1658"); /* lewati baris bermasalah */ }
		}
		if (perluDisimpan.isEmpty()) return;
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			Transaction tx = session.beginTransaction();
			try {
				for (Tagihan t : perluDisimpan) {
					session.update(t);
				}
				tx.commit();
			} catch (Exception e) {
				try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtil.java:1671"); /* abaikan */ }
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtil.java:1673");
			/* self-heal bersifat pelengkap — kegagalan tidak boleh mengganggu tampilan */
		} finally {
			closeSessionAndDisconnect(session);
		}
	}

	/**
	 * Kunci "slot tagihan": identitas satu tagihan menurut item + pengaturan biaya + bulan/tahun +
	 * urutan bayar. Dipakai untuk menyamakan tagihan duplikat (baris berbeda di DB yang sebenarnya
	 * tagihan yang sama). Bila dua tagihan punya slot sama, keduanya adalah tagihan yang sama.
	 */
	private static String slotKeyTagihan(Tagihan t) {
		try {
			if (t == null || t.getItemBiayaSekolah() == null || t.getItemBiayaSekolah().getId() == null) {
				return null;
			}
			Long item = t.getItemBiayaSekolah().getId();
			Long pb = (t.getPengaturanBiaya() == null) ? null : t.getPengaturanBiaya().getId();
			Integer tb = t.getTahunbulan();
			Integer bk = t.getBayarKe();
			return item + "_" + pb + "_" + tb + "_" + bk;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Mengumpulkan "slot" tagihan ({@link #slotKeyTagihan}) yang SUDAH LUNAS dibayar oleh
	 * {@code siswa}: tagihan yang bertaut PembayaranSiswaDetail + PembayaranSiswa nyata, dengan
	 * nominal terbayar menutup nominal tagihan. Hasilnya dipakai untuk MENYARING tagihan duplikat
	 * pada item+bulan yang sama agar tidak ditampilkan/diproses ulang (cegah pembayaran dobel).
	 *
	 * <p>Memakai openSession() yang ditutup di finally; query hanya-baca.</p>
	 */
	@SuppressWarnings("unchecked")
	private static java.util.Set<String> kumpulkanSlotSudahLunas(Siswa siswa) {
		java.util.Set<String> keys = new java.util.HashSet<String>();
		if (siswa == null || siswa.getId() == null) {
			return keys;
		}
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			/* Gunakan projection nilai scalar. Jangan memanggil Tagihan.getDibayar() di
			 * sini: getter tersebut merambat ke PembayaranSiswaDetail dan
			 * ItemBiayaSekolah; identity-map legacy dapat mengganti relasi hasil query
			 * dengan proxy dari request lama yang sesinya sudah ditutup. */
			List<Object[]> lunas = session.createQuery(
					"select ib.id, pb.id, t.tahunbulan, t.bayarKe, t.nominal, "
					+ "psd.nominalManual, psd.nominal "
					+ "from Tagihan t join t.itemBiayaSekolah ib "
					+ "left join t.pengaturanBiaya pb "
					+ "join t.pembayaranSiswaDetail psd join psd.pembayaranSiswa ps "
					+ "where t.siswa.id = :siswaId")
					.setParameter("siswaId", siswa.getId()).list();
			for (Object[] row : lunas) {
				try {
					if (row == null || row.length < 7 || row[0] == null) {
						continue;
					}
					Number nilaiBayar = row[5] instanceof Number ? (Number) row[5]
							: (row[6] instanceof Number ? (Number) row[6] : null);
					double bayar = nilaiBayar == null ? 0.0 : nilaiBayar.doubleValue();
					double nominal = row[4] instanceof Number ? ((Number) row[4]).doubleValue() : 0.0;
					// LUNAS: tagihan bernominal tetap (> 0) yang nominal terbayarnya menutup tagihan
					// (toleransi pembulatan kecil). Pembayaran sebagian (cicilan slot yang sama) TIDAK
					// dianggap lunas → tetap boleh dibayar. Item bernominal 0 yang nilainya bisa diubah
					// saat pembayaran (mis. top-up) sengaja TIDAK diblok agar tetap bisa dibayar lagi.
					if (nominal > 0.1 && bayar + 0.5 >= nominal) {
						keys.add(String.valueOf(row[0]) + "_" + String.valueOf(row[1]) + "_"
								+ String.valueOf(row[2]) + "_" + String.valueOf(row[3]));
					}
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtil.java:1738");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/helper/TagihanUtil.java:1742");
		} finally {
			closeSessionAndDisconnect(session);
		}
		return keys;
	}

	private static void closeSessionAndDisconnect(Session session) {
		if (session != null) {
			try {
				session.clear();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtil.java:1753");
			}
			try {
				session.disconnect();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtil.java:1757");
			}
			try {
				session.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanUtil.java:1761");
			}
		}
	}
}
