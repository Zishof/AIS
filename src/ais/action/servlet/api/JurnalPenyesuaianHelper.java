package ais.action.servlet.api;

import java.util.Date;
import java.util.List;

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
import ais.database.model.akunting.TemplateJurnalPenyesuaian;

/**
 * Jurnal penyesuaian berkala dari template: amortisasi biaya dibayar di muka, akrual beban,
 * penyisihan piutang tak tertagih, dan sejenisnya.
 *
 * <p>Template didefinisikan sekali (akun debet, akun kredit, nilai, frekuensi), lalu tiap periode
 * cukup dilihat drafnya dan diposting. Satu template hanya bisa diposting SEKALI untuk periode
 * yang sama &mdash; penanda {@code [PENYESUAIAN <id> <periode>]} disimpan pada keterangan jurnal
 * dan dicek sebelum menulis &mdash; sehingga menjalankan ulang tidak menggandakan beban.</p>
 *
 * <p>Aksi: {@code penyesuaian_template_daftar}, {@code penyesuaian_template_simpan},
 * {@code penyesuaian_template_hapus}, {@code penyesuaian_draft}, {@code penyesuaian_posting}.</p>
 */
public final class JurnalPenyesuaianHelper {

	public static final String JENIS = "Jurnal Penyesuaian Berkala";

	private JurnalPenyesuaianHelper() {
	}

	public static void proses(String action, Tbmuser tbmuser, JSONObject payload, JSONObject hasil)
			throws Exception {
		if ("penyesuaian_template_daftar".equals(action)) {
			daftar(hasil);
		} else if ("penyesuaian_template_simpan".equals(action)) {
			simpan(tbmuser, payload, hasil);
		} else if ("penyesuaian_template_hapus".equals(action)) {
			hapus(payload, hasil);
		} else if ("penyesuaian_draft".equals(action)) {
			jalankan(tbmuser, payload, hasil, false);
		} else if ("penyesuaian_posting".equals(action)) {
			jalankan(tbmuser, payload, hasil, true);
		} else {
			hasil.put("status", "99");
			hasil.put("message", "Aksi jurnal penyesuaian tidak dikenal: " + action);
		}
	}

	@SuppressWarnings("unchecked")
	private static List<TemplateJurnalPenyesuaian> semuaTemplate(Session session, boolean hanyaAktif) {
		org.hibernate.Criteria c = session.createCriteria(TemplateJurnalPenyesuaian.class)
				.addOrder(org.hibernate.criterion.Order.asc("nama"));
		if (hanyaAktif) {
			c.add(org.hibernate.criterion.Restrictions.eq("aktif", Boolean.TRUE));
		}
		return c.list();
	}

	private static void daftar(JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			List<TemplateJurnalPenyesuaian> daftar = semuaTemplate(session, false);
			JSONArray arr = new JSONArray();
			for (int i = 0; i < daftar.size(); i++) {
				TemplateJurnalPenyesuaian t = daftar.get(i);
				JSONObject j = new JSONObject();
				j.put("id", t.getId());
				j.put("nama", t.getNama() == null ? "" : t.getNama());
				j.put("akunDebet", AkunKantinUtil.label(t.getAkunDebet()));
				j.put("akunKredit", AkunKantinUtil.label(t.getAkunKredit()));
				j.put("akunDebetKode", t.getAkunDebet() == null ? "" : t.getAkunDebet().getKode());
				j.put("akunKreditKode", t.getAkunKredit() == null ? "" : t.getAkunKredit().getKode());
				j.put("nilai", t.getNilai());
				j.put("frekuensi", t.getFrekuensi());
				j.put("aktif", t.getAktif());
				j.put("keterangan", t.getKeterangan() == null ? "" : t.getKeterangan());
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static Akun akunByKode(Session session, String kode) {
		if (kode == null || kode.trim().isEmpty()) {
			return null;
		}
		return (Akun) session.createCriteria(Akun.class)
				.add(org.hibernate.criterion.Restrictions.eq("kode", kode.trim())).setMaxResults(1).uniqueResult();
	}

	private static void simpan(Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			String nama = payload.optString("nama", "").trim();
			if (nama.isEmpty()) {
				hasil.put("status", "99");
				hasil.put("message", "Nama template wajib diisi.");
				return;
			}
			Akun debet = akunByKode(session, payload.optString("akunDebetKode", ""));
			Akun kredit = akunByKode(session, payload.optString("akunKreditKode", ""));
			if (debet == null || kredit == null) {
				hasil.put("status", "99");
				hasil.put("message", "Akun debet dan akun kredit wajib diisi dengan kode akun yang ada.");
				return;
			}
			if (debet.getId() != null && debet.getId().equals(kredit.getId())) {
				hasil.put("status", "99");
				hasil.put("message", "Akun debet dan kredit tidak boleh sama.");
				return;
			}
			long id = payload.optLong("id", 0);
			TemplateJurnalPenyesuaian t = id > 0
					? (TemplateJurnalPenyesuaian) session.get(TemplateJurnalPenyesuaian.class, Long.valueOf(id))
					: new TemplateJurnalPenyesuaian();
			if (t == null) {
				hasil.put("status", "99");
				hasil.put("message", "Template tidak ditemukan.");
				return;
			}
			t.setNama(nama);
			t.setAkunDebet(debet);
			t.setAkunKredit(kredit);
			t.setNilai(Double.valueOf(payload.optDouble("nilai", 0)));
			t.setFrekuensi(payload.optString("frekuensi", TemplateJurnalPenyesuaian.BULANAN));
			t.setAktif(Boolean.valueOf(payload.optBoolean("aktif", true)));
			t.setKeterangan(payload.optString("keterangan", ""));
			t.setSatuanKerja(AkunKantinUtil.satkerKantin());
			if (tbmuser != null) {
				t.setOleh(tbmuser.getUserId());
				t.setOlehId(tbmuser.getUserId());
			}
			session.beginTransaction();
			session.saveOrUpdate(t);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", t.getId());
			hasil.put("message", "Template '" + nama + "' tersimpan.");
		} catch (Exception e) {
			batalkanDiam(session);
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static void hapus(JSONObject payload, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			TemplateJurnalPenyesuaian t = (TemplateJurnalPenyesuaian) session
					.get(TemplateJurnalPenyesuaian.class, Long.valueOf(payload.optLong("id", 0)));
			if (t == null) {
				hasil.put("status", "99");
				hasil.put("message", "Template tidak ditemukan.");
				return;
			}
			// Jurnal yang sudah terbentuk TIDAK ikut terhapus -- riwayat akuntansi tidak boleh
			// hilang hanya karena templatenya dibuang.
			session.beginTransaction();
			session.delete(t);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("message", "Template dihapus. Jurnal yang terlanjur terbentuk tetap tersimpan.");
		} catch (Exception e) {
			batalkanDiam(session);
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Penanda periode per template; dipakai sebagai kunci anti-posting-ganda. */
	private static String penanda(long idTemplate, String periode) {
		return "[PENYESUAIAN " + idTemplate + " " + periode + "]";
	}

	private static boolean sudahDiposting(Session session, String penanda) {
		try {
			Object v = session.createSQLQuery("select count(*) from akunting.posting_history"
					+ " where jenis = :jenis and coalesce(keterangan,'') like :penanda")
					.setParameter("jenis", JENIS).setParameter("penanda", "%" + penanda + "%").uniqueResult();
			return v instanceof Number && ((Number) v).longValue() > 0;
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit JurnalPenyesuaianHelper.sudahDiposting");
			return false;
		}
	}

	private static void jalankan(Tbmuser tbmuser, JSONObject payload, JSONObject hasil, boolean terapkan)
			throws Exception {
		String periode = payload == null ? "" : payload.optString("periode", "").trim();   // yyyy-MM
		String tanggalTeks = payload == null ? "" : payload.optString("tanggal", "").trim(); // yyyy-MM-dd
		if (periode.isEmpty()) {
			hasil.put("status", "99");
			hasil.put("message", "Periode (format yyyy-MM) wajib diisi.");
			return;
		}
		JSONArray idsArr = payload == null ? null : payload.optJSONArray("posting_ids");

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			List<TemplateJurnalPenyesuaian> daftar = semuaTemplate(session, true);
			JSONArray rincian = new JSONArray();
			int siap = 0;
			for (int i = 0; i < daftar.size(); i++) {
				TemplateJurnalPenyesuaian t = daftar.get(i);
				String alasan = "";
				double nilai = t.getNilai() == null ? 0 : t.getNilai().doubleValue();
				if (t.getAkunDebet() == null || t.getAkunKredit() == null) {
					alasan = "Akun debet/kredit template belum lengkap.";
				} else if (nilai <= 0) {
					alasan = "Nilai template nol.";
				} else if (TemplateJurnalPenyesuaian.TAHUNAN.equals(t.getFrekuensi())
						&& !periode.endsWith("-12")) {
					alasan = "Template tahunan hanya diposting pada periode Desember.";
				} else if (sudahDiposting(session, penanda(t.getId().longValue(), periode))) {
					alasan = "Sudah diposting untuk periode " + periode + ".";
				}
				JSONObject j = new JSONObject();
				j.put("id", t.getId());
				j.put("nama", t.getNama());
				j.put("debet", AkunKantinUtil.label(t.getAkunDebet()));
				j.put("kredit", AkunKantinUtil.label(t.getAkunKredit()));
				j.put("nilai", nilai);
				j.put("frekuensi", t.getFrekuensi());
				j.put("siap", alasan.isEmpty());
				j.put("alasan", alasan);
				rincian.put(j);
				if (alasan.isEmpty()) {
					siap++;
				}
			}
			hasil.put("status", "00");
			hasil.put("rincian", rincian);
			hasil.put("jumlahSiap", siap);
			if (!terapkan) {
				hasil.put("message", daftar.isEmpty()
						? "Belum ada template penyesuaian yang aktif."
						: siap + " dari " + daftar.size() + " template siap diposting untuk periode " + periode + ".");
				return;
			}

			Tbmuser pengguna = tbmuser == null ? Common.getCurrentUser() : tbmuser;
			if (pengguna == null) {
				hasil.put("status", "01");
				hasil.put("message", "Sesi pengguna tidak ditemukan. Silakan masuk kembali sebelum memposting.");
				return;
			}
			Date tanggal = tanggalPeriode(periode, tanggalTeks);
			int berhasil = 0;
			JSONArray masalah = new JSONArray();
			for (int i = 0; i < rincian.length(); i++) {
				JSONObject b = rincian.getJSONObject(i);
				if (!b.optBoolean("siap", false)) {
					continue;
				}
				long idT = b.optLong("id");
				if (idsArr != null && idsArr.length() > 0 && !mengandung(idsArr, idT)) {
					continue;
				}
				TemplateJurnalPenyesuaian t = (TemplateJurnalPenyesuaian) session
						.get(TemplateJurnalPenyesuaian.class, Long.valueOf(idT));
				if (t == null) {
					continue;
				}
				String ket = "Penyesuaian " + t.getNama() + " periode " + periode + " "
						+ penanda(idT, periode);
				try {
					PostingHistory ph = new PostingHistory(JENIS);
					ph.setTanggal(tanggal);
					ph.setTbmuser(pengguna);
					ph.setKeterangan(ket);
					session.beginTransaction();
					session.save(ph);
					boolean ok = CommonAkunting.saveTransaksi(new Akun[] { t.getAkunDebet() },
							new Akun[] { t.getAkunKredit() }, null, null, ph, true, ket, tanggal,
							new Double[] { t.getNilai() }, new Double[] { t.getNilai() },
							Double.valueOf(0.0), null, AkunKantinUtil.satkerKantin(), session);
					if (!ok) {
						session.getTransaction().rollback();
						masalah.put(t.getNama() + ": jurnal ditolak (periode mungkin sudah ditutup).");
						continue;
					}
					session.getTransaction().commit();
					berhasil++;
				} catch (Exception ex) {
					batalkanDiam(session);
					ais.common.ErrorAuditUtil.record(ex, "auto-audit JurnalPenyesuaianHelper.posting " + idT);
					masalah.put(t.getNama() + ": " + ex.getMessage());
				}
			}
			hasil.put("diposting", berhasil);
			hasil.put("masalah", masalah);
			hasil.put("message", berhasil + " jurnal penyesuaian terbentuk"
					+ (masalah.length() > 0 ? ", " + masalah.length() + " gagal." : "."));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static boolean mengandung(JSONArray arr, long id) {
		for (int i = 0; i < arr.length(); i++) {
			if (arr.optLong(i) == id) {
				return true;
			}
		}
		return false;
	}

	/** Tanggal jurnal: yang diminta pemakai, atau hari terakhir periode bila tidak diisi. */
	private static Date tanggalPeriode(String periode, String tanggalTeks) {
		try {
			if (tanggalTeks != null && tanggalTeks.length() >= 10) {
				return new java.text.SimpleDateFormat("yyyy-MM-dd").parse(tanggalTeks);
			}
			java.util.Calendar c = java.util.Calendar.getInstance();
			c.setTime(new java.text.SimpleDateFormat("yyyy-MM").parse(periode));
			c.set(java.util.Calendar.DAY_OF_MONTH, c.getActualMaximum(java.util.Calendar.DAY_OF_MONTH));
			return c.getTime();
		} catch (Exception e) {
			return ais.ui.util.WaktuUtil.getDate();
		}
	}

	private static void batalkanDiam(Session session) {
		try {
			if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) JurnalPenyesuaianHelper.batalkanDiam");
		}
	}
}
