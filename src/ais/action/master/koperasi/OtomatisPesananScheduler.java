package ais.action.master.koperasi;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.Toko;

/**
 * Penjadwal "proses otomatis setelah lewat jam 24" untuk pesanan kantin.
 *
 * <p>Sebelum ini prosesnya hanya berjalan saat ada orang membuka halaman
 * Pesanan. Kalau seharian tidak ada yang membukanya, tidak ada yang diproses --
 * padahal justru hari-hari sibuk yang paling mungkin terlewat. Penjadwal ini
 * menjalankannya sendiri.</p>
 *
 * <h3>Kapan berjalan</h3>
 * <p>Setiap hari pukul {@value #JAM_JALAN}:{@value #MENIT_JALAN} waktu server,
 * plus satu kali beberapa menit setelah aplikasi hidup. Yang kedua adalah
 * jaring pengaman: server yang mati semalaman melewatkan jadwalnya, dan tanpa
 * itu pesanan kemarin akan menganggur sampai malam berikutnya.</p>
 *
 * <h3>Yang diproses</h3>
 * <p>Hanya toko yang pengaturan EFEKTIF-nya menyala (per toko mengalahkan
 * global -- lihat {@link OtomatisPesananUtil}), dan hanya baris yang tanggalnya
 * sudah lewat hari: {@code DATE(...) < CURRENT_DATE}. Jendela itu sama persis
 * dgn yang dipakai halaman Pesanan, supaya penjadwal dan layar tidak menyapu
 * rentang yang berbeda.</p>
 *
 * <h3>Batas yang disengaja</h3>
 * <p>Bayar otomatis memakai jalur {@code KantinHelper.bayar} yang sama dgn
 * kasir -- BUKAN UPDATE langsung -- supaya transaksi, stok, diskon, dan jejak
 * auditnya terbentuk persis seperti pembayaran biasa. Konsekuensinya jalur itu
 * menuntut identitas pengguna; draft yang tidak menyimpan penggunanya
 * DILEWATI dan dihitung sebagai "dilewati", bukan dipaksakan atas nama akun
 * lain.</p>
 */
public final class OtomatisPesananScheduler {

	/** Jam server saat siklus harian dijalankan. */
	public static final int JAM_JALAN = 0;
	public static final int MENIT_JALAN = 30;

	/** Jeda sebelum siklus penyusul dijalankan setelah aplikasi hidup. */
	private static final int JEDA_MENIT_SETELAH_START = 5;

	private static volatile ScheduledExecutorService penjadwal;

	private OtomatisPesananScheduler() {
	}

	/** Mulai penjadwal (idempoten; aman dipanggil sekali saat start). */
	public static synchronized void mulai() {
		try {
			if (penjadwal != null) {
				return;
			}
			ScheduledExecutorService s =
					Executors.newScheduledThreadPool(1, daemonFactory("otomatis-pesanan"));

			// Siklus harian, dipatok ke jam dinding -- bukan "24 jam sejak
			// aplikasi hidup", yang akan bergeser tiap kali server di-restart.
			s.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					jalankanAman("harian");
				}
			}, menitKeJadwalBerikutnya(), 24 * 60, TimeUnit.MINUTES);

			// Jaring pengaman untuk server yang semalam mati.
			s.schedule(new Runnable() {
				@Override
				public void run() {
					jalankanAman("penyusul-start");
				}
			}, JEDA_MENIT_SETELAH_START, TimeUnit.MINUTES);

			penjadwal = s;
			System.out.println("[OtomatisPesanan] Penjadwal aktif; siklus berikutnya "
					+ menitKeJadwalBerikutnya() + " menit lagi.");
		} catch (Throwable t) {
			System.err.println("[OtomatisPesanan] Gagal memulai penjadwal: " + t.getMessage());
		}
	}

	/** Hentikan penjadwal. Dipanggil saat aplikasi berhenti. */
	public static synchronized void hentikan() {
		ScheduledExecutorService s = penjadwal;
		penjadwal = null;
		if (s != null) {
			try {
				s.shutdownNow();
			} catch (Throwable t) {
				ais.common.ErrorAuditUtil.record(t, "OtomatisPesananScheduler.hentikan");
			}
		}
	}

	private static void jalankanAman(String pemicu) {
		try {
			Ringkasan r = jalankanSekali();
			System.out.println("[OtomatisPesanan] (" + pemicu + ") toko=" + r.tokoDiproses
					+ " dibayar=" + r.dibayar + " dilewati=" + r.dilewatiTanpaPengguna
					+ " gagalBayar=" + r.gagalBayar + " dilayani=" + r.dilayani);
		} catch (Throwable t) {
			// Kegagalan satu siklus TIDAK boleh menghentikan penjadwal.
			ais.common.ErrorAuditUtil.record(t, "OtomatisPesananScheduler siklus " + pemicu);
		}
	}

	/** Menit dari sekarang sampai jadwal harian berikutnya. */
	private static long menitKeJadwalBerikutnya() {
		Calendar sekarang = Calendar.getInstance();
		Calendar target = Calendar.getInstance();
		target.set(Calendar.HOUR_OF_DAY, JAM_JALAN);
		target.set(Calendar.MINUTE, MENIT_JALAN);
		target.set(Calendar.SECOND, 0);
		target.set(Calendar.MILLISECOND, 0);
		if (!target.after(sekarang)) {
			target.add(Calendar.DAY_OF_MONTH, 1);
		}
		long selisih = target.getTimeInMillis() - sekarang.getTimeInMillis();
		return Math.max(1L, selisih / (60L * 1000L));
	}

	private static ThreadFactory daemonFactory(final String nama) {
		return new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, nama);
				// Daemon: penjadwal tidak boleh menahan proses saat aplikasi
				// diminta berhenti.
				t.setDaemon(true);
				return t;
			}
		};
	}

	/** Ringkasan satu siklus. */
	public static final class Ringkasan {
		public int tokoDiproses;
		public int dibayar;
		public int gagalBayar;
		public int dilewatiTanpaPengguna;
		public int dilayani;
	}

	/**
	 * Jalankan satu siklus sekarang. Membuka dan menutup sesinya sendiri.
	 *
	 * <p>Dipisahkan dari penjadwal supaya dapat dipanggil manual (mis. dari
	 * aksi API) tanpa menunggu jadwal.</p>
	 */
	public static Ringkasan jalankanSekali() {
		Ringkasan ringkasan = new Ringkasan();
		Session session = HibernateUtil.getSessionFactory().openSession();
		List<Long> tokoIds = new ArrayList<Long>();
		try {
			@SuppressWarnings("unchecked")
			List<Toko> daftar = session.createCriteria(Toko.class)
					.add(org.hibernate.criterion.Restrictions.eq("aktif", Boolean.TRUE)).list();
			for (Toko t : daftar) {
				if (t == null || t.getId() == null) {
					continue;
				}
				boolean bayar = OtomatisPesananUtil.bayarOtomatis(t);
				boolean layani = OtomatisPesananUtil.layaniOtomatis(t);
				if (!bayar && !layani) {
					continue;
				}
				tokoIds.add(t.getId());
				ringkasan.tokoDiproses++;
				if (layani) {
					ringkasan.dilayani += tandaiTerlayani(session, t.getId());
				}
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}

		// Pembayaran diproses SETELAH sesi pemindaian ditutup: KantinHelper.bayar
		// membuka sesinya sendiri, dan menahan dua sesi sekaligus di satu utas
		// pernah menjadi sumber sesi menggantung di modul ini.
		for (int i = 0; i < tokoIds.size(); i++) {
			Long tokoId = tokoIds.get(i);
			Session cek = HibernateUtil.getSessionFactory().openSession();
			boolean perluBayar;
			try {
				Toko t = (Toko) cek.get(Toko.class, tokoId);
				perluBayar = OtomatisPesananUtil.bayarOtomatis(t);
			} finally {
				HibernateUtil.closeSessionQuietly(cek);
			}
			if (perluBayar) {
				bayarDraftTertunggak(tokoId, ringkasan);
			}
		}
		return ringkasan;
	}

	/** Tandai terlayani transaksi yang tanggalnya sudah lewat hari. */
	private static int tandaiTerlayani(Session session, Long tokoId) {
		try {
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					"UPDATE koperasi.pembelian SET terlayani = true"
							+ " WHERE COALESCE(terlayani, false) = false"
							+ " AND DATE(waktu) < CURRENT_DATE AND toko = ?");
			ps.setLong(1, tokoId.longValue());
			session.getTransaction().begin();
			int n = ps.executeUpdate();
			session.getTransaction().commit();
			ps.close();
			return n;
		} catch (Throwable t) {
			ais.common.ErrorAuditUtil.record(t,
					"OtomatisPesananScheduler.tandaiTerlayani toko=" + tokoId);
			return 0;
		}
	}

	/**
	 * Bayarkan draft tertunggak lewat jalur kasir yang sama.
	 *
	 * <p>Setiap draft diproses TERPISAH: satu draft yang ditolak (mis. stok
	 * habis) tidak boleh menghentikan sisanya.</p>
	 */
	private static void bayarDraftTertunggak(Long tokoId, Ringkasan ringkasan) {
		List<Object[]> antrean = new ArrayList<Object[]>();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					"SELECT a.id, a.cara_pembayaran_koperasi, a.tbmuser"
							+ " FROM koperasi.draft_pembelian_anggota_koperasi a"
							+ " WHERE a.lunas IS NULL AND DATE(a.tanggal_pembayaran) < CURRENT_DATE"
							+ " AND a.toko = ? ORDER BY a.id ASC");
			ps.setLong(1, tokoId.longValue());
			java.sql.ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				antrean.add(new Object[] { Long.valueOf(rs.getLong(1)),
						Long.valueOf(rs.getLong(2)), rs.getString(3) });
			}
			rs.close();
			ps.close();
		} catch (Throwable t) {
			ais.common.ErrorAuditUtil.record(t,
					"OtomatisPesananScheduler.antreanDraft toko=" + tokoId);
			return;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}

		for (int i = 0; i < antrean.size(); i++) {
			Object[] baris = antrean.get(i);
			Long draftId = (Long) baris[0];
			Long caraBayar = (Long) baris[1];
			String userId = (String) baris[2];
			try {
				if (userId == null || userId.trim().length() == 0) {
					// Tanpa identitas pengguna, pembayaran tidak dapat dicatat
					// atas nama siapa pun secara jujur -- dilewati, bukan
					// dipaksakan.
					ringkasan.dilewatiTanpaPengguna++;
					continue;
				}
				if (!bayarSatuDraft(tokoId, draftId, caraBayar, userId)) {
					ringkasan.gagalBayar++;
				} else {
					ringkasan.dibayar++;
				}
			} catch (Throwable t) {
				ringkasan.gagalBayar++;
				ais.common.ErrorAuditUtil.record(t,
						"OtomatisPesananScheduler.bayarDraft draft=" + draftId);
			}
		}
	}

	private static boolean bayarSatuDraft(Long tokoId, Long draftId, Long caraBayar,
			String userId) throws Exception {
		JSONArray transaksi = new JSONArray();
		Tbmuser pengguna = null;
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			pengguna = (Tbmuser) session.createCriteria(Tbmuser.class)
					.add(org.hibernate.criterion.Restrictions.eq("userId", userId.trim()))
					.setMaxResults(1).uniqueResult();
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					"SELECT p.id, p.kode, d.nama, d.hargasatuan, d.qty, d.diskon,"
							+ " d.aturan_diskon, d.cashback"
							+ " FROM koperasi.draft_pembelian d"
							+ " LEFT JOIN koperasi.produk p ON d.produk = p.id"
							+ " WHERE d.draft_pembelian_anggota_koperasi = ?");
			ps.setLong(1, draftId.longValue());
			java.sql.ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				JSONObject item = new JSONObject();
				item.put("id", rs.getLong(1));
				item.put("kode", rs.getString(2) == null ? "" : rs.getString(2));
				item.put("nama", rs.getString(3) == null ? "" : rs.getString(3));
				item.put("harga", rs.getDouble(4));
				item.put("jumlah", rs.getDouble(5));
				item.put("diskon", rs.getDouble(6));
				long aturan = rs.getLong(7);
				if (!rs.wasNull()) {
					item.put("aturanDiskon", aturan);
				}
				item.put("cashback", rs.getDouble(8));
				transaksi.put(item);
			}
			rs.close();
			ps.close();
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}

		if (pengguna == null || transaksi.length() == 0) {
			return false;
		}

		JSONObject payload = new JSONObject();
		payload.put("kodeUnik", "AUTO-" + System.currentTimeMillis() + "-" + draftId);
		payload.put("idToko", tokoId);
		// Kirim sbg TEKS berformat baku, bukan objek Date: JSONObject menyimpan
		// objeknya apa adanya sehingga pembaca menerima Date.toString() yang tidak
		// dikenali parser. Penerima kini toleran, tetapi sumbernya tetap dibetulkan
		// supaya kontrak payload sama dgn pemanggil dari klien POS.
		payload.put("waktu",
				ais.common.Common.dateFormat3.get().format(ais.ui.util.WaktuUtil.getDate()));
		payload.put("kanalCheckout", "otomatis_jadwal");
		if (caraBayar != null && caraBayar.longValue() > 0) {
			payload.put("caraBayar", caraBayar);
		}
		payload.put("draftPembelianAnggotaKoperasi", draftId);
		payload.put("transaksi", transaksi);

		JSONObject hasil = new JSONObject();
		ais.action.servlet.api.KantinHelper.bayar(pengguna, payload, hasil);
		String status = hasil.optString("status", "");
		return "00".equals(status) || "success".equals(status);
	}

	/** Tanggal acuan siklus -- disediakan agar mudah diuji. */
	public static Date sekarang() {
		return new Date();
	}
}
