package ais.action.servlet.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.akunting.util.CommonAkunting;
import ais.action.master.koperasi.helper.AkunKantinUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.PostingHistory;

/**
 * Tutup buku: jurnal penutup akun nominal ke <b>Laba Ditahan</b>.
 *
 * <p><b>Celah yang ditutup.</b> Proses {@code Closing} yang ada hanya MENANDAI periode
 * (mengelompokkan jurnal ke satu record closing berdasarkan tanggal batas); ia tidak pernah
 * membuat jurnal penutup. Akibatnya akun Laba Ditahan tak pernah terisi dan laba tahun-tahun
 * sebelumnya bercampur dengan tahun berjalan pada laporan Laba Rugi kumulatif.</p>
 *
 * <p><b>Yang dikerjakan.</b> Untuk periode yang dipilih, seluruh akun berklasifikasi jenis
 * "Rugi Laba" dinolkan: akun bersaldo kredit (pendapatan) didebet, akun bersaldo debet (beban)
 * dikredit, dan selisihnya &mdash; laba atau rugi bersih &mdash; dipindahkan ke akun Laba Ditahan
 * (konfigurasi {@code akun_laba_ditahan}). Setelah itu Neraca tahun berikutnya berangkat dari
 * ekuitas yang benar, bukan dari laba berjalan yang dihitung ulang terus-menerus.</p>
 *
 * <p><b>Aman diulang.</b> Satu periode hanya bisa ditutup sekali: keterangan {@link PostingHistory}
 * memuat penanda periode dan dicek sebelum menulis. Draf ditampilkan lebih dulu (akun apa saja,
 * berapa nilainya, ke mana selisihnya) sehingga bisa diperiksa sebelum dijurnal.</p>
 */
public final class TutupBukuHelper {

	/**
	 * Gerbang aksi granular (grid CRUD {@code TbmroleAction}). Admin global boleh; pengguna tanpa
	 * peran dianggap boleh (kompatibilitas akun lama). Kotak CRUD yang BELUM PERNAH diatur admin
	 * mengikuti visibilitas menunya -- lihat {@code EbisnisMenuKatalog.bolehAksiAkuntansi}.
	 */
	private static boolean bolehAksiMenu(Tbmuser tbmuser, String kunciMenu, String aksi) {
		if (ais.common.Common.getApakahAdminLain(tbmuser)) {
			return true;
		}
		ais.database.model.Tbmrole peran = tbmuser == null ? null : tbmuser.hakAkses();
		if (peran == null) {
			return true;
		}
		return ais.common.EbisnisMenuKatalog.bolehAksiAkuntansi(peran.getEbisnisMenu(), peran.getRoleId(),
				kunciMenu, aksi);
	}

	/** Balasan seragam saat aksi ditolak gerbang peran. */
	private static void tolakHak(JSONObject hasil, String pekerjaan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", "Anda tidak memiliki hak " + pekerjaan
				+ ". Hubungi admin untuk mengaktifkannya pada Grup Pengguna.");
	}


	public static final String JENIS = "Tutup Buku";
	public static final String CFG_LABA_DITAHAN = "akun_laba_ditahan";

	private TutupBukuHelper() {
	}

	/**
	 * Hak menu Tutup Buku, dikirim bersama PRATINJAU (draft) -- di layar itulah tombol
	 * Posting berada, jadi di situ pula tempat paling tepat memberi tahu bahwa
	 * tombolnya akan ditolak. Menutup buku hanya mengenal satu wewenang: memposting.
	 *
	 * <p>Bukan gerbang: gerbang sebenarnya tetap pemeriksaan pada cabang
	 * {@code tutup_buku_posting} di bawah.</p>
	 */
	private static JSONObject hakAksesJson(Tbmuser tbmuser) throws Exception {
		JSONObject j = new JSONObject();
		j.put("create", bolehAksiMenu(tbmuser, "tutup_buku", "create"));
		return j;
	}

	public static void proses(String action, Tbmuser tbmuser, JSONObject payload, JSONObject hasil)
			throws Exception {
		if ("tutup_buku_draft".equals(action)) {
			jalankan(tbmuser, payload, hasil, false);
			hasil.put("hak", hakAksesJson(tbmuser));
		} else if ("tutup_buku_posting".equals(action)) {
			if (!bolehAksiMenu(tbmuser, "tutup_buku", "create")) {
				tolakHak(hasil, "memposting tutup buku");
				return;
			}
			jalankan(tbmuser, payload, hasil, true);
		} else {
			hasil.put("status", "99");
			hasil.put("message", "Aksi tutup buku tidak dikenal: " + action);
		}
	}

	private static void jalankan(Tbmuser tbmuser, JSONObject payload, JSONObject hasil, boolean terapkan)
			throws Exception {
		String mulai = payload == null ? "" : payload.optString("mulai", "").trim();
		String sampai = payload == null ? "" : payload.optString("sampai", "").trim();
		if (mulai.isEmpty() || sampai.isEmpty()) {
			hasil.put("status", "99");
			hasil.put("message", "Tanggal mulai dan sampai wajib diisi.");
			return;
		}
		String penanda = "[TUTUPBUKU " + mulai + ".." + sampai + "]";

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			long satker = satkerId(session);
			// Akun nominal (jenis laporan Rugi Laba) beserta saldonya pada periode ini.
			String sql = "select d.id, d.kode, d.nama, "
					+ " coalesce(sum(a.debet),0) - coalesce(sum(a.kredit),0) as saldo_debet, "
					+ " lower(coalesce(c.keterangan,'') || ' ' || coalesce(m.keterangan,'') || ' '"
					+ "   || coalesce(f.keterangan,'')) as tag "
					+ " from akunting.transaksi a "
					+ " join akunting.grup_transaksi a1 on a1.id = a.grup_transaksi "
					+ " join akunting.akun d on d.id = a.akun "
					+ " join akunting.kelompok_laporan_punya_akun b on b.akun = d.id "
					+ " join akunting.kelompok_laporan c on c.id = b.kelompok_laporan "
					+ " join akunting.jenis_laporan f on f.id = c.jenis_laporan "
					+ " left join akunting.master_grup_laporan m on m.id = c.master_grup_laporan "
					+ " where a1.posting_history is not null "
					+ "   and ( :satker = -1 or a1.satuan_kerja = :satker ) "
					+ "   and cast(a.tanggal_transaksi as date) between cast(:mulai as date) and cast(:sampai as date) "
					+ "   and (c.aktif is null or c.aktif) "
					+ "   and ( lower(coalesce(f.keterangan,'')) like '%laba%' "
					+ "         or lower(coalesce(f.keterangan,'')) like '%rugi%' ) "
					+ " group by d.id, d.kode, d.nama, c.keterangan, m.keterangan, f.keterangan "
					+ " having abs(coalesce(sum(a.debet),0) - coalesce(sum(a.kredit),0)) >= 0.005 "
					+ " order by d.kode ";
			SQLQuery q = session.createSQLQuery(sql);
			q.setParameter("satker", Long.valueOf(satker));
			q.setParameter("mulai", mulai);
			q.setParameter("sampai", sampai);
			List<?> rows = q.list();

			List<Akun> akunDebet = new ArrayList<Akun>();
			List<Double> nilaiDebet = new ArrayList<Double>();
			List<Akun> akunKredit = new ArrayList<Akun>();
			List<Double> nilaiKredit = new ArrayList<Double>();
			JSONArray rincian = new JSONArray();
			double totalPendapatan = 0, totalBeban = 0;

			for (int i = 0; i < rows.size(); i++) {
				Object[] r = (Object[]) rows.get(i);
				Long idAkun = Long.valueOf(((Number) r[0]).longValue());
				String kode = r[1] == null ? "" : r[1].toString();
				String nama = r[2] == null ? "" : r[2].toString();
				double saldoDebet = (r[3] instanceof Number) ? ((Number) r[3]).doubleValue() : 0;
				Akun akun = (Akun) session.get(Akun.class, idAkun);
				if (akun == null) {
					continue;
				}
				JSONObject j = new JSONObject();
				j.put("kodeAkun", kode);
				j.put("namaAkun", nama);
				if (saldoDebet < 0) {
					// Saldo alami KREDIT (pendapatan) -> ditutup dengan mendebet sebesar saldonya.
					double nilai = -saldoDebet;
					akunDebet.add(akun);
					nilaiDebet.add(Double.valueOf(nilai));
					totalPendapatan += nilai;
					j.put("sisi", "Debet (menutup pendapatan)");
					j.put("nilai", nilai);
				} else {
					double nilai = saldoDebet;
					akunKredit.add(akun);
					nilaiKredit.add(Double.valueOf(nilai));
					totalBeban += nilai;
					j.put("sisi", "Kredit (menutup beban)");
					j.put("nilai", nilai);
				}
				rincian.put(j);
			}

			double labaBersih = totalPendapatan - totalBeban;
			// Akun tujuan diambil dari master TOKO (kolom Akun Laba Ditahan); konfigurasi lama
			// hanya cadangan bagi pemasangan yang belum mengisi masternya.
			Long tokoId = payload != null && payload.optLong("tokoId", 0) > 0
					? Long.valueOf(payload.optLong("tokoId")) : null;
			Akun akunLabaDitahan = AkunKantinUtil.akunLabaDitahan(session, tokoId);
			String alasan = "";
			if (rincian.length() == 0) {
				alasan = "Tidak ada saldo akun Laba Rugi pada periode ini yang perlu ditutup.";
			} else if (akunLabaDitahan == null) {
				alasan = "Akun Laba Ditahan belum diatur. Isi kolom Akun Laba Ditahan pada master Toko.";
			} else if (sudahDitutup(session, penanda)) {
				alasan = "Periode ini sudah pernah ditutup. Batalkan jurnal penutupnya lebih dulu bila ingin mengulang.";
			}
			if (akunLabaDitahan != null && Math.abs(labaBersih) >= 0.005) {
				if (labaBersih > 0) {
					akunKredit.add(akunLabaDitahan);
					nilaiKredit.add(Double.valueOf(labaBersih));
				} else {
					akunDebet.add(akunLabaDitahan);
					nilaiDebet.add(Double.valueOf(-labaBersih));
				}
			}

			hasil.put("status", "00");
			hasil.put("rincian", rincian);
			hasil.put("totalPendapatan", totalPendapatan);
			hasil.put("totalBeban", totalBeban);
			hasil.put("labaBersih", labaBersih);
			hasil.put("akunLabaDitahan", akunLabaDitahan == null ? "" : AkunKantinUtil.label(akunLabaDitahan));
			hasil.put("siap", alasan.isEmpty());
			hasil.put("alasan", alasan);

			if (!terapkan) {
				hasil.put("message", alasan.isEmpty()
						? (rincian.length() + " akun akan ditutup; laba bersih "
								+ Common.numberFormat.get().format(labaBersih) + " dipindahkan ke Laba Ditahan.")
						: alasan);
				return;
			}
			if (!alasan.isEmpty()) {
				hasil.put("status", "99");
				hasil.put("message", alasan);
				return;
			}
			Tbmuser pengguna = tbmuser == null ? Common.getCurrentUser() : tbmuser;
			if (pengguna == null) {
				hasil.put("status", "01");
				hasil.put("message", "Sesi pengguna tidak ditemukan. Silakan masuk kembali sebelum memposting.");
				return;
			}

			Date tglTutup = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(sampai);
			String ket = "Jurnal penutup periode " + mulai + " s.d " + sampai + " " + penanda;
			PostingHistory ph = new PostingHistory(JENIS);
			ph.setTanggal(tglTutup);
			ph.setTbmuser(pengguna);
			ph.setKeterangan(ket);

			session.beginTransaction();
			try {
				session.save(ph);
				boolean ok = CommonAkunting.saveTransaksi(akunDebet.toArray(new Akun[] {}),
						akunKredit.toArray(new Akun[] {}), null, null, ph, true, ket, tglTutup,
						nilaiDebet.toArray(new Double[] {}), nilaiKredit.toArray(new Double[] {}),
						Double.valueOf(0.0), null, AkunKantinUtil.satkerKantin(), session);
				if (!ok) {
					session.getTransaction().rollback();
					hasil.put("status", "99");
					hasil.put("message", "Jurnal penutup ditolak (periode mungkin sudah ditutup di modul Closing).");
					return;
				}
				session.getTransaction().commit();
			} catch (Exception e) {
				batalkanDiam(session);
				throw e;
			}
			hasil.put("diposting", rincian.length());
			hasil.put("message", "Jurnal penutup terbentuk untuk " + rincian.length() + " akun; laba bersih "
					+ Common.numberFormat.get().format(labaBersih) + " masuk ke Laba Ditahan.");
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Penanda periode disimpan pada keterangan PostingHistory; dipakai sebagai kunci anti-ganda. */
	private static boolean sudahDitutup(Session session, String penanda) {
		try {
			Object v = session.createSQLQuery("select count(*) from akunting.posting_history"
					+ " where jenis = :jenis and coalesce(keterangan,'') like :penanda")
					.setParameter("jenis", JENIS).setParameter("penanda", "%" + penanda + "%").uniqueResult();
			return v instanceof Number && ((Number) v).longValue() > 0;
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit TutupBukuHelper.sudahDitutup");
			return false;
		}
	}

	private static long satkerId(Session session) {
		try {
			ais.database.model.rab.SatuanKerja s = AkunKantinUtil.satkerKantin();
			return s == null || s.getId() == null ? -1 : s.getId().longValue();
		} catch (Exception e) {
			return -1;
		}
	}

	private static void batalkanDiam(Session session) {
		try {
			if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) TutupBukuHelper.batalkanDiam");
		}
	}
}
