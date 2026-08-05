package ais.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.action.master.inventory.StokLokasiUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.asset.Lokasi;
import ais.database.model.inventory.AmbangStokGudang;
import ais.database.model.inventory.PengajuanPembelianGudang;
import ais.database.model.sirs.Gudang;

/**
 * <h2>StokThresholdScheduler — notifikasi stok minimum gudang otomatis, 2 tingkat.</h2>
 *
 * <p>Fitur "Purchase" dari gap analisis PDF klien (2026-07-26): tiap {@link AmbangStokGudang} aktif
 * dicek berkala; bila stok {@link ais.database.model.inventory.Produk} di {@link Gudang} tsb sudah
 * di titik ambang atau di bawahnya, dibuatkan {@link PengajuanPembelianGudang} otomatis (idempoten
 * -- tidak dibuat dobel selama masih ada pengajuan lama berstatus BARU/DIPROSES utk pasangan
 * produk+gudang yang sama) lalu staf terkait diberi notifikasi lewat {@link CommonNotifikasi}.</p>
 *
 * <p><b>Arah pengajuan mengikuti hierarki {@link Gudang#getGudangInduk()} secara otomatis</b> --
 * inilah bagian "2 tingkat" yang diminta PDF: gudang CABANG (punya {@code gudangInduk}) diajukan KE
 * gudang induknya (staf menindaklanjuti lewat "Pengiriman Antar Gudang" yang sudah ada); gudang
 * PUSAT (tanpa {@code gudangInduk}, puncak hierarki) diajukan tanpa tujuan gudang -- artinya ke
 * VENDOR EKSTERNAL, staf menindaklanjuti lewat layar Pengadaan/Kulakan yang sudah ada. Tidak perlu
 * konfigurasi arah terpisah -- cukup dari struktur {@code gudangInduk} yang sudah ada.</p>
 *
 * <p>Pola penjadwalan MENIRU {@link DepositoAroScheduler} persis (daemon
 * {@code ScheduledExecutorService}, sesi/transaksi sendiri per siklus, semua {@code Throwable}
 * ditangkap supaya kegagalan otomasi tidak pernah mengganggu aplikasi). Interval lebih rapat
 * (tiap 4 jam, bukan harian) karena stok bahan baku operasional bisa berubah cepat dalam sehari --
 * beda dengan ARO deposito yang murni berbasis tanggal jatuh tempo.</p>
 */
public final class StokThresholdScheduler {

	private static volatile ScheduledExecutorService penjadwal;

	private StokThresholdScheduler() {
	}

	/** Mulai penjadwal (idempoten; aman dipanggil sekali saat start). */
	public static synchronized void mulai() {
		try {
			if (penjadwal != null) {
				return;
			}
			ScheduledExecutorService s = Executors.newScheduledThreadPool(1, daemonFactory("stok-ambang"));
			s.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					try {
						jalankanSekali();
					} catch (Throwable t) {
						ErrorAuditUtil.record(t, "auto-audit src/ais/common/StokThresholdScheduler.java:run");
					}
				}
			}, 5, 4 * 60, TimeUnit.MINUTES);
			penjadwal = s;
			System.out.println("[StokAmbang] Penjadwal ambang stok gudang aktif (tiap 4 jam).");
		} catch (Throwable t) {
			System.err.println("[StokAmbang] Gagal memulai penjadwal: " + t.getMessage());
		}
	}

	/** Hentikan penjadwal. Dipanggil saat aplikasi berhenti. */
	public static synchronized void hentikan() {
		ScheduledExecutorService s = penjadwal;
		penjadwal = null;
		if (s != null) {
			try {
				s.shutdownNow();
			} catch (Throwable ignored) {
				ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/StokThresholdScheduler.java:hentikan");
			}
		}
	}

	/**
	 * Jalankan satu siklus pengecekan ambang sekarang (membuka &amp; menutup sesi/transaksi sendiri).
	 *
	 * @return jumlah {@link PengajuanPembelianGudang} baru yang diterbitkan siklus ini.
	 */
	@SuppressWarnings("unchecked")
	public static int jalankanSekali() {
		Session session = null;
		Transaction tx = null;
		int diterbitkan = 0;
		try {
			session = HibernateUtil.openSession();
			tx = session.beginTransaction();

			List<AmbangStokGudang> semuaAmbang = session.createCriteria(AmbangStokGudang.class)
					.add(Restrictions.eq("aktif", true)).list();

			for (AmbangStokGudang ambang : semuaAmbang) {
				try {
					if (prosesSatuAmbang(session, ambang)) {
						diterbitkan++;
					}
				} catch (Exception exSatu) {
					ErrorAuditUtil.record(exSatu,
							"auto-audit src/ais/common/StokThresholdScheduler.java:prosesSatuAmbang");
				}
			}

			tx.commit();
			tx = null;
			System.out.println("[StokAmbang] Siklus selesai, pengajuan baru diterbitkan=" + diterbitkan);
		} catch (Throwable t) {
			if (tx != null) {
				try {
					tx.rollback();
				} catch (Throwable ignored) {
					ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/StokThresholdScheduler.java:rollback");
				}
			}
			System.err.println("[StokAmbang] Gagal menjalankan siklus: " + t.getMessage());
		} finally {
			if (session != null) {
				try {
					session.close();
				} catch (Throwable ignored) {
					ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/StokThresholdScheduler.java:close");
				}
			}
		}
		return diterbitkan;
	}

	@SuppressWarnings("unchecked")
	private static boolean prosesSatuAmbang(Session session, AmbangStokGudang ambang) {
		Gudang gudang = ambang.getGudang();
		Long produkId = ambang.getProduk().getId();
		if (gudang == null || produkId == null) {
			return false;
		}

		List<Long> lokasiIds = session.createCriteria(Lokasi.class).add(Restrictions.eq("gudang", gudang))
				.setProjection(Projections.property("id")).list();
		double totalStok = 0;
		for (Long lokasiId : lokasiIds) {
			totalStok += StokLokasiUtil.qtyStok(session, lokasiId, produkId);
		}

		if (totalStok > ambang.getAmbangMinimum()) {
			return false; // stok masih di atas ambang, tidak perlu diajukan
		}

		// Idempoten: jangan terbitkan dobel selama masih ada pengajuan OTOMATIS lama yang belum tuntas
		// utk pasangan produk+gudang yang sama.
		Number sudahAda = (Number) session.createCriteria(PengajuanPembelianGudang.class)
				.setProjection(Projections.rowCount()).add(Restrictions.eq("produk", ambang.getProduk()))
				.add(Restrictions.eq("gudangAsal", gudang))
				.add(Restrictions.in("status", new String[] { PengajuanPembelianGudang.STATUS_BARU,
						PengajuanPembelianGudang.STATUS_DIPROSES }))
				.uniqueResult();
		if (sudahAda != null && sudahAda.longValue() > 0) {
			return false;
		}

		Gudang gudangTujuan = gudang.getGudangInduk(); // null = puncak hierarki -> ke vendor eksternal
		boolean keVendor = gudangTujuan == null;

		PengajuanPembelianGudang pengajuan = new PengajuanPembelianGudang();
		pengajuan.setProduk(ambang.getProduk());
		pengajuan.setGudangAsal(gudang);
		pengajuan.setGudangTujuan(gudangTujuan);
		pengajuan.setStokSaatDiajukan(Double.valueOf(totalStok));
		// Saran qty: kembalikan ke 2x ambang (buffer sederhana) dikurangi stok saat ini -- staf boleh
		// ubah bebas saat memproses, ini murni titik awal supaya form tidak kosong.
		double saranQty = Math.max(0, (ambang.getAmbangMinimum() * 2) - totalStok);
		pengajuan.setQtyDiminta(Double.valueOf(saranQty));
		pengajuan.setStatus(PengajuanPembelianGudang.STATUS_BARU);
		pengajuan.setOtomatis(Boolean.TRUE);
		pengajuan.setWaktuDibuat(ais.ui.util.WaktuUtil.getDate());
		pengajuan.setKeterangan("Dibuat otomatis: stok " + ambang.getProduk().getNama() + " di gudang "
				+ gudang.getNama() + " = " + totalStok + " (ambang " + ambang.getAmbangMinimum() + "). Tujuan: "
				+ (keVendor ? "Vendor eksternal (gudang ini puncak hierarki)" : gudangTujuan.getNama()));
		session.save(pengajuan);

		kirimNotifikasi(session, ambang, gudang, totalStok, keVendor, gudangTujuan);
		return true;
	}

	@SuppressWarnings("unchecked")
	private static void kirimNotifikasi(Session session, AmbangStokGudang ambang, Gudang gudang, double totalStok,
			boolean keVendor, Gudang gudangTujuan) {
		try {
			List<Long> userIds = session.createSQLQuery(
					"select id from public.tbmuser where "
							+ "userrole = :kantin or user_role2 = :kantin or user_role3 = :kantin "
							+ "or user_role4 = :kantin or user_role5 = :kantin "
							+ "or userrole = :admin or user_role2 = :admin or user_role3 = :admin "
							+ "or user_role4 = :admin or user_role5 = :admin")
					.setParameter("kantin", Tbmrole.KANTIN).setParameter("admin", Tbmrole.ADMINISTRATOR).list();
			List<Tbmuser> penerima = new ArrayList<Tbmuser>();
			for (Long uid : userIds) {
				Tbmuser u = (Tbmuser) session.get(Tbmuser.class, uid);
				if (u != null) {
					penerima.add(u);
				}
			}
			if (penerima.isEmpty()) {
				return;
			}

			LinkedHashMap<String, String> rincian = new LinkedHashMap<String, String>();
			rincian.put("Produk", ambang.getProduk().getNama());
			rincian.put("Gudang", gudang.getNama());
			rincian.put("Stok saat ini", String.valueOf(totalStok));
			rincian.put("Ambang minimum", String.valueOf(ambang.getAmbangMinimum()));
			rincian.put("Tujuan pengajuan", keVendor ? "Vendor eksternal" : gudangTujuan.getNama());

			CommonNotifikasi.terbitkanKeBanyak(penerima, "Stok Menipis: " + ambang.getProduk().getNama(),
					"Stok " + ambang.getProduk().getNama() + " di gudang " + gudang.getNama()
							+ " sudah mencapai ambang minimum. Pengajuan pembelian otomatis telah dibuat, mohon ditindaklanjuti.",
					rincian, null, ambang.getProduk(), null, null, CommonNotifikasi.STATUS_WARNING);
		} catch (Exception e) {
			ErrorAuditUtil.record(e, "auto-audit src/ais/common/StokThresholdScheduler.java:kirimNotifikasi");
		}
	}

	private static ThreadFactory daemonFactory(final String nama) {
		return new ThreadFactory() {
			private final AtomicInteger n = new AtomicInteger(1);

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, nama + "-" + n.getAndIncrement());
				t.setDaemon(true);
				return t;
			}
		};
	}
}
