package ais.action.servlet.api;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.sirs.ApotikCustomerMembership;
import ais.database.model.sirs.ApotikRewardLedger;
import ais.database.model.sirs.Pasien;

/** API membership, reward ledger, consent notifikasi, dan jadwal refill apotik. */
public final class ApotikMembershipHelper {
	private ApotikMembershipHelper() { }
	private static final SimpleDateFormat TANGGAL = new SimpleDateFormat("yyyy-MM-dd");

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91"); hasil.put("description", pesan);
	}
	private static String str(Object v) { return v == null ? "" : v.toString(); }
	private static Long optLong(JSONObject j, String key) {
		if (j == null || j.isNull(key)) return null;
		try { return Long.valueOf(j.getLong(key)); } catch (Exception e) { return null; }
	}
	private static boolean statusSah(String s) {
		return ApotikCustomerMembership.AKTIF.equals(s)
				|| ApotikCustomerMembership.NONAKTIF.equals(s)
				|| ApotikCustomerMembership.DIBLOKIR.equals(s);
	}
	private static Date tanggal(JSONObject request, String key) {
		try { return TANGGAL.parse(request.optString(key, "")); }
		catch (Exception e) { return null; }
	}
	private static void isi(JSONObject j, ApotikCustomerMembership m) throws Exception {
		Pasien p = m.getPasien();
		j.put("id", m.getId()); j.put("kode", str(m.getKode()));
		j.put("pasienId", p == null ? JSONObject.NULL : p.getId());
		j.put("nomorRm", p == null ? "" : str(p.getKode()));
		j.put("nama", str(m.getNama())); j.put("telepon", str(m.getTelepon()));
		j.put("tier", str(m.getTier())); j.put("poin", m.getPoinSaldo());
		j.put("status", m.getStatus()); j.put("consentNotifikasi", m.getConsentNotifikasi());
		j.put("obatRutin", str(m.getObatRutin())); j.put("intervalRefillHari", m.getIntervalRefillHari());
		j.put("tanggalRefillBerikut", m.getTanggalRefillBerikut() == null ? "" : TANGGAL.format(m.getTanggalRefillBerikut()));
		j.put("tanggalDaftar", m.getTanggalDaftar() == null ? "" : TANGGAL.format(m.getTanggalDaftar()));
		j.put("keterangan", str(m.getKeterangan()));
	}

	public static void daftar(JSONObject request, JSONObject hasil) throws Exception {
		int page = Math.max(1, request == null ? 1 : request.optInt("page", 1));
		int size = request == null ? 100 : request.optInt("page_size", 100);
		if (size < 1) size = 100; if (size > 100) size = 100;
		String status = request == null ? "" : request.optString("status", "").trim().toUpperCase();
		String cari = request == null ? "" : request.optString("keyword", "").trim();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			org.hibernate.Criteria c = session.createCriteria(ApotikCustomerMembership.class).addOrder(Order.desc("id"));
			if (status.length() > 0) c.add(Restrictions.eq("status", status));
			if (cari.length() > 0) c.add(Restrictions.disjunction()
					.add(Restrictions.ilike("kode", "%" + cari + "%"))
					.add(Restrictions.ilike("nama", "%" + cari + "%"))
					.add(Restrictions.ilike("telepon", "%" + cari + "%"))
					.add(Restrictions.ilike("obatRutin", "%" + cari + "%")));
			c.setFirstResult((page - 1) * size).setMaxResults(size);
			@SuppressWarnings("unchecked") List<ApotikCustomerMembership> list = c.list();
			JSONArray data = new JSONArray();
			for (ApotikCustomerMembership m : list) { JSONObject j = new JSONObject(); isi(j, m); data.put(j); }
			hasil.put("status", "00"); hasil.put("data", data); hasil.put("page", page); hasil.put("pageSize", size);
		} finally { HibernateUtil.closeSessionQuietly(session); }
	}

	public static void simpan(Tbmuser user, JSONObject request, JSONObject hasil) throws Exception {
		String nama = request == null ? "" : request.optString("nama", "").trim();
		if (nama.length() == 0) { tolak(hasil, "Nama anggota wajib diisi."); return; }
		String status = request.optString("status", ApotikCustomerMembership.AKTIF).trim().toUpperCase();
		if (!statusSah(status)) { tolak(hasil, "Status membership tidak sah."); return; }
		Session session = HibernateUtil.getSessionFactory().openSession(); Transaction tx = null;
		try {
			tx = session.beginTransaction(); Long id = optLong(request, "id");
			ApotikCustomerMembership m = id == null ? new ApotikCustomerMembership()
					: (ApotikCustomerMembership) session.get(ApotikCustomerMembership.class, id);
			if (m == null) { tolak(hasil, "Membership tidak ditemukan."); tx.rollback(); return; }
			if (id == null) { m.setKode("MEM-APT-" + System.currentTimeMillis()); m.setPoinSaldo(Long.valueOf(0)); m.setTanggalDaftar(new Date()); }
			Long pasienId = optLong(request, "pasien_id");
			if (pasienId != null) m.setPasien((Pasien) session.get(Pasien.class, pasienId));
			m.setNama(nama); m.setTelepon(request.optString("telepon", "").trim());
			m.setTier(request.optString("tier", "REGULER").trim().toUpperCase()); m.setStatus(status);
			m.setConsentNotifikasi(Boolean.valueOf(request.optBoolean("consent_notifikasi", false)));
			m.setObatRutin(request.optString("obat_rutin", "").trim());
			m.setIntervalRefillHari(Integer.valueOf(Math.max(0, request.optInt("interval_refill_hari", 0))));
			m.setTanggalRefillBerikut(tanggal(request, "tanggal_refill_berikut"));
			m.setKeterangan(request.optString("keterangan", "").trim());
			m.setOleh(user.getUserId()); m.setOlehId(user.getUserId()); session.saveOrUpdate(m); tx.commit();
			JSONObject j = new JSONObject(); isi(j, m); hasil.put("status", "00"); hasil.put("data", j);
		} catch (Exception e) { try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { } throw e; }
		finally { HibernateUtil.closeSessionQuietly(session); }
	}

	public static void mutasiPoin(Tbmuser user, JSONObject request, JSONObject hasil) throws Exception {
		Long id = optLong(request, "id"); long poin = request == null ? 0 : request.optLong("poin", 0);
		if (id == null || poin == 0) { tolak(hasil, "ID membership dan jumlah poin wajib diisi."); return; }
		Session session = HibernateUtil.getSessionFactory().openSession(); Transaction tx = null;
		try {
			tx = session.beginTransaction();
			ApotikCustomerMembership m = (ApotikCustomerMembership) session.get(ApotikCustomerMembership.class, id, LockMode.UPGRADE);
			if (m == null) { tolak(hasil, "Membership tidak ditemukan."); tx.rollback(); return; }
			long saldo = m.getPoinSaldo().longValue() + poin;
			if (saldo < 0) { tolak(hasil, "Saldo poin tidak mencukupi."); tx.rollback(); return; }
			m.setPoinSaldo(Long.valueOf(saldo)); m.setOleh(user.getUserId()); m.setOlehId(user.getUserId()); session.update(m);
			ApotikRewardLedger l = new ApotikRewardLedger(); l.setMembership(m);
			l.setJenis(poin > 0 ? "PEROLEHAN" : "PENUKARAN"); l.setPoin(Long.valueOf(poin)); l.setSaldoSetelah(Long.valueOf(saldo));
			l.setReferensi(request.optString("referensi", "").trim()); l.setKeterangan(request.optString("keterangan", "").trim());
			l.setWaktu(new Date()); l.setOleh(user.getUserId()); l.setOlehId(user.getUserId()); session.save(l); tx.commit();
			JSONObject j = new JSONObject(); isi(j, m); hasil.put("status", "00"); hasil.put("data", j);
		} catch (Exception e) { try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { } throw e; }
		finally { HibernateUtil.closeSessionQuietly(session); }
	}

	public static void refill(Tbmuser user, JSONObject request, JSONObject hasil) throws Exception {
		Long id = optLong(request, "id"); if (id == null) { tolak(hasil, "ID membership wajib diisi."); return; }
		Session session = HibernateUtil.getSessionFactory().openSession(); Transaction tx = null;
		try {
			tx = session.beginTransaction(); ApotikCustomerMembership m = (ApotikCustomerMembership) session.get(ApotikCustomerMembership.class, id);
			if (m == null) { tolak(hasil, "Membership tidak ditemukan."); tx.rollback(); return; }
			m.setObatRutin(request.optString("obat_rutin", str(m.getObatRutin())).trim());
			m.setIntervalRefillHari(Integer.valueOf(Math.max(0, request.optInt("interval_refill_hari", m.getIntervalRefillHari().intValue()))));
			Date berikut = tanggal(request, "tanggal_refill_berikut"); if (berikut != null) m.setTanggalRefillBerikut(berikut);
			m.setConsentNotifikasi(Boolean.valueOf(request.optBoolean("consent_notifikasi", m.getConsentNotifikasi().booleanValue())));
			m.setOleh(user.getUserId()); m.setOlehId(user.getUserId()); session.update(m); tx.commit();
			JSONObject j = new JSONObject(); isi(j, m); hasil.put("status", "00"); hasil.put("data", j);
		} catch (Exception e) { try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { } throw e; }
		finally { HibernateUtil.closeSessionQuietly(session); }
	}

	public static void provisionDemo(Tbmuser user, JSONObject request, JSONObject hasil) throws Exception {
		if (!Common.bolehKonfigurasi(Konfigurasi.DATA_SAMPLE_EBISNIS, Konfigurasi.TIDAK_AKTIF)
				|| user == null || !Common.getApakahAdminLain(user)
				|| request == null || !"SEED-DEMO-APOTIK".equals(request.optString("konfirmasi", ""))) {
			tolak(hasil, "Provisioning membership hanya tersedia untuk admin pada server demo."); return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession(); Transaction tx = null;
		try {
			tx = session.beginTransaction();
			@SuppressWarnings("unchecked") List<Pasien> pasien = session.createCriteria(Pasien.class).addOrder(Order.asc("id")).setMaxResults(100).list();
			int dibuat = 0;
			for (int i = 0; i < pasien.size(); i++) {
				String kode = String.format("MEM-APT-UAT-%04d", Integer.valueOf(i + 1));
				Object ada = session.createCriteria(ApotikCustomerMembership.class).add(Restrictions.eq("kode", kode)).setMaxResults(1).uniqueResult();
				if (ada != null) continue;
				Pasien p = pasien.get(i); ApotikCustomerMembership m = new ApotikCustomerMembership();
				m.setKode(kode); m.setPasien(p); m.setNama(str(p.getNama()).length() == 0 ? "Member Demo " + (i + 1) : str(p.getNama()));
				m.setTelepon(str(p.getNoHp()).length() == 0 ? "0813" + String.format("%08d", Integer.valueOf(i + 1)) : str(p.getNoHp()));
				m.setTier(i % 10 == 0 ? "PLATINUM" : i % 4 == 0 ? "GOLD" : "REGULER"); m.setPoinSaldo(Long.valueOf((i + 1) * 25));
				m.setStatus(i % 20 == 0 ? ApotikCustomerMembership.NONAKTIF : ApotikCustomerMembership.AKTIF);
				m.setConsentNotifikasi(Boolean.valueOf(i % 5 != 0)); m.setObatRutin(i % 3 == 0 ? "Amlodipine 5 mg" : i % 3 == 1 ? "Metformin 500 mg" : "Vitamin B Complex");
				m.setIntervalRefillHari(Integer.valueOf(i % 2 == 0 ? 30 : 14)); Calendar cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, (i % 30) + 1); m.setTanggalRefillBerikut(cal.getTime());
				m.setTanggalDaftar(new Date(System.currentTimeMillis() - (long) i * 86400000L)); m.setKeterangan("DATA SAMPLE/UAT — bukan data pasien nyata.");
				m.setOleh(user.getUserId()); m.setOlehId(user.getUserId()); session.save(m); session.flush();
				ApotikRewardLedger l = new ApotikRewardLedger(); l.setMembership(m); l.setJenis("PEROLEHAN"); l.setPoin(m.getPoinSaldo()); l.setSaldoSetelah(m.getPoinSaldo());
				l.setReferensi("SEED-UAT"); l.setKeterangan("Saldo awal DATA SAMPLE/UAT"); l.setWaktu(new Date()); l.setOleh(user.getUserId()); l.setOlehId(user.getUserId()); session.save(l); dibuat++;
				if (i > 0 && i % 25 == 0) session.flush();
			}
			tx.commit(); hasil.put("status", "00"); hasil.put("dibuat", dibuat); hasil.put("total", pasien.size());
		} catch (Exception e) { try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { } throw e; }
		finally { HibernateUtil.closeSessionQuietly(session); }
	}
}
