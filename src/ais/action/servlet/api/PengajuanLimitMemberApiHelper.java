package ais.action.servlet.api;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.PembelianAnggotaKoperasi;
import ais.database.model.koperasi.PengajuanLimitTransaksiMember;

/** Alur pengajuan dan keputusan transaksi member yang melampaui limit. */
public final class PengajuanLimitMemberApiHelper {

	private PengajuanLimitMemberApiHelper() { }

	/**
	 * Tipe implementasi bersarang {@link HasilPeriksa} milik {@link PengajuanLimitMemberApiHelper}. Kelas ini
	 * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * PengajuanLimitMemberApiHelper}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code boolean bolehLanjut}, {@code
	 * PengajuanLimitTransaksiMember pengajuan}, {@code String pesan}. Aturan bisnis bersama tetap berada pada
	 * kelas induk atau service yang dipanggilnya.</p>
	 *
	 * @see PengajuanLimitMemberApiHelper
	 */
	public static final class HasilPeriksa {
		public final boolean bolehLanjut;
		public final PengajuanLimitTransaksiMember pengajuan;
		public final String pesan;

		private HasilPeriksa(boolean boleh, PengajuanLimitTransaksiMember p, String pesan) {
			this.bolehLanjut = boleh;
			this.pengajuan = p;
			this.pesan = pesan;
		}
	}

	/** Default false: hanya role yang dicentang eksplisit yang dapat memutuskan. */
	public static boolean bolehVerifikasi(Tbmuser pengguna) {
		if (pengguna == null) return false;
		try {
			for (Tbmrole role : pengguna.ambilRoles()) {
				if (role != null && Boolean.TRUE.equals(role.getBolehVerifikasiMemberMelebihiLimit())) return true;
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "PengajuanLimitMemberApiHelper.bolehVerifikasi");
		}
		return false;
	}

	/**
	 * Membuat pengajuan idempoten berdasarkan kode transaksi. Retry checkout
	 * yang sama tidak membuat antrean ganda. Persetujuan hanya diterima bila
	 * anggota dan nominal persis sama.
	 */
	public static HasilPeriksa periksaAtauAjukan(Session session, Tbmuser kasir,
			AnggotaKoperasi anggota, String kodeTransaksi, double nominal,
			String periode, double limit, double berjalan) throws Exception {
		PengajuanLimitTransaksiMember p = (PengajuanLimitTransaksiMember) session
				.createCriteria(PengajuanLimitTransaksiMember.class)
				.add(Restrictions.eq("kodeTransaksi", kodeTransaksi)).uniqueResult();
		if (p != null) {
			Long anggotaLama = p.getAnggotaKoperasi() == null ? null : p.getAnggotaKoperasi().getId();
			if (anggotaLama == null || !anggotaLama.equals(anggota.getId())
					|| Math.abs(p.getNominalTransaksi().doubleValue() - nominal) >= 0.5d) {
				return new HasilPeriksa(false, p,
						"Kode transaksi sudah terikat pada pengajuan member atau nominal yang berbeda.");
			}
			if (PengajuanLimitTransaksiMember.DISETUJUI.equals(p.getStatus())) {
				return new HasilPeriksa(true, p, "Pengajuan telah disetujui.");
			}
			if (PengajuanLimitTransaksiMember.DIPAKAI.equals(p.getStatus())) {
				return new HasilPeriksa(true, p, "Persetujuan telah dipakai oleh transaksi ini.");
			}
			if (PengajuanLimitTransaksiMember.DITOLAK.equals(p.getStatus())) {
				return new HasilPeriksa(false, p,
						"Pengajuan transaksi melebihi limit telah ditolak. Buat transaksi baru bila ingin mengajukan ulang.");
			}
			return new HasilPeriksa(false, p,
					"Pengajuan transaksi melebihi limit sedang menunggu verifikasi petugas berwenang.");
		}

		p = new PengajuanLimitTransaksiMember();
		p.setAnggotaKoperasi(anggota);
		p.setTipeAnggotaKoperasi(anggota.getTipeAnggotaKoperasi());
		p.setKodeTransaksi(kodeTransaksi);
		p.setNominalTransaksi(Double.valueOf(nominal));
		p.setPeriodeLimit(periode == null ? "" : periode.toUpperCase());
		p.setLimitTransaksi(Double.valueOf(limit));
		p.setPemakaianBerjalan(Double.valueOf(berjalan));
		p.setStatus(PengajuanLimitTransaksiMember.MENUNGGU);
		p.setDiajukanOleh(kasir);
		p.setTanggalPengajuan(new Date());
		Transaction tx = session.beginTransaction();
		try {
			session.save(p);
			tx.commit();
		} catch (Exception e) {
			if (tx != null && tx.isActive()) tx.rollback();
			throw e;
		}
		return new HasilPeriksa(false, p,
				"Transaksi melebihi limit. Pengajuan sudah dibuat dan menunggu verifikasi petugas berwenang.");
	}

	/** Mengunci persetujuan ke transaksi final setelah checkout berhasil. */
	public static void tandaiDipakai(Session session, PengajuanLimitTransaksiMember p,
			PembelianAnggotaKoperasi pembelian) throws Exception {
		if (p == null || PengajuanLimitTransaksiMember.DIPAKAI.equals(p.getStatus())) return;
		Transaction tx = session.beginTransaction();
		try {
			p.setStatus(PengajuanLimitTransaksiMember.DIPAKAI);
			p.setTanggalDipakai(new Date());
			p.setPembelianAnggotaKoperasi(pembelian);
			session.update(p);
			tx.commit();
		} catch (Exception e) {
			if (tx != null && tx.isActive()) tx.rollback();
			throw e;
		}
	}

	public static void proses(String action, Tbmuser pengguna, JSONObject request,
			JSONObject hasil) throws Exception {
		if ("pengajuan_limit_member_list".equals(action)) daftar(pengguna, request, hasil);
		else if ("pengajuan_limit_member_putuskan".equals(action)) putuskan(pengguna, request, hasil);
		else {
			hasil.put("status", "91");
			hasil.put("description", "Aksi pengajuan limit member tidak dikenal.");
		}
	}

	@SuppressWarnings("unchecked")
	private static void daftar(Tbmuser pengguna, JSONObject request, JSONObject hasil) throws Exception {
		boolean boleh = bolehVerifikasi(pengguna);
		hasil.put("bolehVerifikasi", boleh);
		if (!boleh) {
			hasil.put("status", "00");
			hasil.put("data", new JSONArray());
			hasil.put("total", 0);
			return;
		}
		int halaman = Math.max(1, request.optInt("page", 1));
		int ukuran = Math.max(10, Math.min(100, request.optInt("page_size", 25)));
		String status = request.optString("status", PengajuanLimitTransaksiMember.MENUNGGU).trim();
		String keyword = request.optString("keyword", "").trim();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Criteria hitung = kriteria(session, status, keyword);
			Number total = (Number) hitung.setProjection(Projections.rowCount()).uniqueResult();
			List<PengajuanLimitTransaksiMember> daftar = kriteria(session, status, keyword)
					.addOrder(Order.desc("tanggalPengajuan"))
					.setFirstResult((halaman - 1) * ukuran).setMaxResults(ukuran).list();
			JSONArray data = new JSONArray();
			for (PengajuanLimitTransaksiMember p : daftar) data.put(json(p));
			hasil.put("status", "00");
			hasil.put("data", data);
			hasil.put("total", total == null ? 0 : total.intValue());
		} finally { tutup(session); }
	}

	private static Criteria kriteria(Session session, String status, String keyword) {
		Criteria c = session.createCriteria(PengajuanLimitTransaksiMember.class, "p")
				.createAlias("anggotaKoperasi", "anggota");
		if (status.length() > 0 && !"SEMUA".equalsIgnoreCase(status))
			c.add(Restrictions.eq("p.status", status.toUpperCase()));
		if (keyword.length() > 0) c.add(Restrictions.or(
				Restrictions.ilike("anggota.nama", keyword, MatchMode.ANYWHERE),
				Restrictions.ilike("p.kodeTransaksi", keyword, MatchMode.ANYWHERE)));
		return c;
	}

	private static void putuskan(Tbmuser pengguna, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehVerifikasi(pengguna)) {
			hasil.put("status", "96");
			hasil.put("description", "Role pengguna tidak diizinkan memverifikasi transaksi member melebihi limit.");
			return;
		}
		long id = request.optLong("id", 0L);
		String keputusan = request.optString("keputusan", "").trim().toUpperCase();
		if (id <= 0 || !("SETUJUI".equals(keputusan) || "TOLAK".equals(keputusan))) {
			hasil.put("status", "91");
			hasil.put("description", "ID dan keputusan SETUJUI/TOLAK wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			PengajuanLimitTransaksiMember p = (PengajuanLimitTransaksiMember) session.get(
					PengajuanLimitTransaksiMember.class, Long.valueOf(id));
			if (p == null) throw new IllegalStateException("Pengajuan tidak ditemukan.");
			if (!PengajuanLimitTransaksiMember.MENUNGGU.equals(p.getStatus()))
				throw new IllegalStateException("Pengajuan sudah diputuskan oleh pengguna lain.");
			p.setStatus("SETUJUI".equals(keputusan)
					? PengajuanLimitTransaksiMember.DISETUJUI : PengajuanLimitTransaksiMember.DITOLAK);
			p.setDiputuskanOleh(pengguna);
			p.setTanggalKeputusan(new Date());
			p.setCatatan(request.optString("catatan", "").trim());
			session.update(p);
			tx.commit();
			hasil.put("status", "00");
			hasil.put("description", "SETUJUI".equals(keputusan)
					? "Pengajuan disetujui. Kasir dapat mengirim ulang transaksi yang sama."
					: "Pengajuan ditolak.");
			hasil.put("data", json(p));
		} catch (Exception e) {
			if (tx != null && tx.isActive()) tx.rollback();
			hasil.put("status", "91");
			hasil.put("description", e.getMessage() == null ? "Keputusan belum dapat disimpan." : e.getMessage());
		} finally { tutup(session); }
	}

	private static JSONObject json(PengajuanLimitTransaksiMember p) throws Exception {
		JSONObject o = new JSONObject();
		o.put("id", p.getId());
		o.put("kodeTransaksi", p.getKodeTransaksi());
		o.put("nominal", p.getNominalTransaksi());
		o.put("periode", p.getPeriodeLimit());
		o.put("limit", p.getLimitTransaksi());
		o.put("pemakaianBerjalan", p.getPemakaianBerjalan());
		o.put("status", p.getStatus());
		o.put("catatan", p.getCatatan() == null ? "" : p.getCatatan());
		AnggotaKoperasi a = p.getAnggotaKoperasi();
		o.put("anggotaId", a == null ? JSONObject.NULL : a.getId());
		o.put("kodeMember", a == null || a.getKode() == null ? "" : a.getKode());
		o.put("namaMember", a == null || a.getNama() == null ? "" : a.getNama());
		o.put("tipeMember", p.getTipeAnggotaKoperasi() == null
				|| p.getTipeAnggotaKoperasi().getNama() == null ? "" : p.getTipeAnggotaKoperasi().getNama());
		o.put("diajukanOleh", p.getDiajukanOleh() == null ? "" : p.getDiajukanOleh().getUserId());
		o.put("diputuskanOleh", p.getDiputuskanOleh() == null ? "" : p.getDiputuskanOleh().getUserId());
		SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		o.put("tanggalPengajuan", p.getTanggalPengajuan() == null ? "" : f.format(p.getTanggalPengajuan()));
		o.put("tanggalKeputusan", p.getTanggalKeputusan() == null ? "" : f.format(p.getTanggalKeputusan()));
		o.put("pembelianId", p.getPembelianAnggotaKoperasi() == null
				? JSONObject.NULL : p.getPembelianAnggotaKoperasi().getId());
		return o;
	}

	private static void tutup(Session session) {
		if (session == null) return;
		try { session.clear(); } catch (Exception ignore) { }
		try { session.disconnect(); } catch (Exception ignore) { }
		try { session.close(); } catch (Exception ignore) { }
	}
}
