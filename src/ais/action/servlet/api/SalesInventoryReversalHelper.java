package ais.action.servlet.api;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.koperasi.AlokasiPembayaranHutangSupplier;
import ais.database.model.koperasi.AlokasiPenerimaanPiutangCustomer;
import ais.database.model.koperasi.LogCetak;
import ais.database.model.koperasi.NotaSalesBiaya;
import ais.database.model.koperasi.NotaSalesKas;
import ais.database.model.koperasi.NotaSalesSession;
import ais.database.model.koperasi.PembayaranHutangSupplier;
import ais.database.model.koperasi.PenerimaanPiutangCustomer;
import ais.database.model.koperasi.PiutangCustomerDoc;
import ais.database.model.koperasi.SalesOrderLapangan;
import ais.database.model.koperasi.SpjSalesNota;

/**
 * <h3>P10: Reversal dokumen posted + siklus BG + register riwayat cetak.</h3>
 *
 * <p>Prinsip matriks paritas: event posted TIDAK PERNAH dihapus -- koreksi = DOKUMEN PEMBALIK
 * bernilai negatif yang mengembalikan outstanding, ber-{@code kodeUnik} idempoten
 * ({@code REV-<jenis>-<id asal>}), hanya Pemilik/Admin, wajib beralasan. Dokumen asal ditandai
 * {@code DIBATALKAN} (tetap terlihat di riwayat). Siklus giro: {@code DITERIMA -> CAIR | TOLAK};
 * TOLAK otomatis menerbitkan reversal.</p>
 *
 * <p>Batas jujur: reversal penerimaan/biaya yang terkait SESI YANG SUDAH CLOSED ditolak --
 * snapshot penutupan tidak boleh berubah diam-diam; koreksinya lewat penyesuaian kantor
 * (dokumen baru), bukan mengutak-atik sesi yang sudah disetujui.</p>
 */
public final class SalesInventoryReversalHelper {

	private SalesInventoryReversalHelper() {
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static Long optLong(JSONObject r, String kunci) {
		if (r == null || r.isNull(kunci)) {
			return null;
		}
		try {
			return Long.valueOf((r.get(kunci) + "").trim());
		} catch (Exception e) {
			return null;
		}
	}

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", pesan);
	}

	private static boolean pemilikAtauAdmin(EbisnisActorContextResolver.ActorContext ctx) {
		return ctx.admin || EbisnisActorContextResolver.ACTOR_PEMILIK.equals(ctx.actorType);
	}

	private static void isiOleh(Object entity, Tbmuser tbmuser) {
		try {
			entity.getClass().getMethod("setOleh", String.class).invoke(entity, tbmuser.getUserId());
			entity.getClass().getMethod("setOlehId", String.class).invoke(entity, tbmuser.getUserId());
		} catch (Exception ignore) {
		}
	}

	// =============================================================================================
	// Reversal pembayaran hutang supplier (AP)
	// =============================================================================================

	public static void payablePaymentReverse(EbisnisActorContextResolver.ActorContext ctx,
			Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!pemilikAtauAdmin(ctx)) {
			tolak(hasil, "Reversal pembayaran hanya oleh Pemilik/Admin.");
			return;
		}
		Long id = optLong(request, "pembayaran_id");
		String alasan = request.optString("alasan", "").trim();
		if (id == null || alasan.isEmpty()) {
			tolak(hasil, "pembayaran_id dan alasan wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			String kodeRev = "REV-PHS-" + id;
			PembayaranHutangSupplier sudah = (PembayaranHutangSupplier) session
					.createCriteria(PembayaranHutangSupplier.class)
					.add(Restrictions.eq("kodeUnik", kodeRev)).setMaxResults(1).uniqueResult();
			if (sudah != null) {
				hasil.put("status", "00");
				hasil.put("id", sudah.getId());
				hasil.put("idempotentReplay", true);
				return;
			}
			PembayaranHutangSupplier asal = (PembayaranHutangSupplier) session
					.get(PembayaranHutangSupplier.class, id);
			if (asal == null) {
				tolak(hasil, "Pembayaran tidak ditemukan.");
				return;
			}
			if (!PembayaranHutangSupplier.DOK_AKTIF.equals(asal.getStatusDok())) {
				tolak(hasil, "Dokumen berstatus " + asal.getStatusDok() + " tidak bisa direversal.");
				return;
			}
			tx = session.beginTransaction();
			PembayaranHutangSupplier rev = new PembayaranHutangSupplier();
			rev.setSupplier(asal.getSupplier());
			rev.setNominal(asal.getNominal().negate());
			rev.setMetode(asal.getMetode());
			rev.setNoBg(asal.getNoBg());
			rev.setNamaBank(asal.getNamaBank());
			rev.setKeterangan("REVERSAL pembayaran #" + id + ": " + alasan);
			rev.setKodeUnik(kodeRev);
			rev.setDibuatOleh(tbmuser);
			rev.setStatusDok(PembayaranHutangSupplier.DOK_REVERSAL);
			rev.setReversalDari(id);
			isiOleh(rev, tbmuser);
			session.save(rev);
			List aloks = session.createCriteria(AlokasiPembayaranHutangSupplier.class)
					.add(Restrictions.eq("pembayaran", asal)).addOrder(Order.asc("id")).list();
			for (int i = 0; i < aloks.size(); i++) {
				AlokasiPembayaranHutangSupplier a = (AlokasiPembayaranHutangSupplier) aloks.get(i);
				AlokasiPembayaranHutangSupplier balik = new AlokasiPembayaranHutangSupplier();
				balik.setPembayaran(rev);
				balik.setPengadaanFaktur(a.getPengadaanFaktur());
				balik.setNominal(a.getNominal().negate());
				session.save(balik);
			}
			asal.setStatusDok(PembayaranHutangSupplier.DOK_DIBATALKAN);
			asal.setAlasanReversal(alasan);
			session.saveOrUpdate(asal);
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", rev.getId());
		} catch (org.hibernate.exception.ConstraintViolationException dup) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			hasil.put("status", "00");
			hasil.put("idempotentReplay", true);
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// Reversal penerimaan piutang customer (AR) -- derivasi order/nota/kas sesi ikut dipulihkan
	// =============================================================================================

	public static void collectionReverse(EbisnisActorContextResolver.ActorContext ctx,
			Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!pemilikAtauAdmin(ctx)) {
			tolak(hasil, "Reversal penerimaan hanya oleh Pemilik/Admin.");
			return;
		}
		Long id = optLong(request, "penerimaan_id");
		String alasan = request.optString("alasan", "").trim();
		if (id == null || alasan.isEmpty()) {
			tolak(hasil, "penerimaan_id dan alasan wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			String kodeRev = "REV-KWT-" + id;
			PenerimaanPiutangCustomer sudah = (PenerimaanPiutangCustomer) session
					.createCriteria(PenerimaanPiutangCustomer.class)
					.add(Restrictions.eq("kodeUnik", kodeRev)).setMaxResults(1).uniqueResult();
			if (sudah != null) {
				hasil.put("status", "00");
				hasil.put("id", sudah.getId());
				hasil.put("idempotentReplay", true);
				return;
			}
			PenerimaanPiutangCustomer asal = (PenerimaanPiutangCustomer) session
					.get(PenerimaanPiutangCustomer.class, id);
			if (asal == null) {
				tolak(hasil, "Penerimaan tidak ditemukan.");
				return;
			}
			if (!PenerimaanPiutangCustomer.DOK_AKTIF.equals(asal.getStatusDok())) {
				tolak(hasil, "Dokumen berstatus " + asal.getStatusDok() + " tidak bisa direversal.");
				return;
			}
			NotaSalesSession sesi = asal.getSesi();
			if (sesi != null && NotaSalesSession.STATUS_CLOSED.equals(sesi.getStatus())) {
				tolak(hasil, "Penerimaan ini bagian sesi " + str(sesi.getNomor())
						+ " yang SUDAH DITUTUP -- snapshot penutupan tidak boleh berubah."
						+ " Koreksi lewat dokumen penyesuaian kantor.");
				return;
			}
			tx = session.beginTransaction();
			PenerimaanPiutangCustomer rev = new PenerimaanPiutangCustomer();
			rev.setCustomer(asal.getCustomer());
			rev.setNominal(asal.getNominal().negate());
			rev.setMetode(asal.getMetode());
			rev.setNoBg(asal.getNoBg());
			rev.setNamaBank(asal.getNamaBank());
			rev.setKeterangan("REVERSAL kwitansi " + str(asal.getNomor()) + ": " + alasan);
			rev.setKodeUnik(kodeRev);
			rev.setDibuatOleh(tbmuser);
			rev.setSales(asal.getSales());
			rev.setSesi(sesi);
			rev.setStatusDok(PenerimaanPiutangCustomer.DOK_REVERSAL);
			rev.setReversalDari(id);
			isiOleh(rev, tbmuser);
			session.save(rev);
			session.flush();
			rev.setNomor("REV-" + str(asal.getNomor()));
			session.saveOrUpdate(rev);

			List aloks = session.createCriteria(AlokasiPenerimaanPiutangCustomer.class)
					.add(Restrictions.eq("penerimaan", asal)).addOrder(Order.asc("id")).list();
			for (int i = 0; i < aloks.size(); i++) {
				AlokasiPenerimaanPiutangCustomer a = (AlokasiPenerimaanPiutangCustomer) aloks.get(i);
				AlokasiPenerimaanPiutangCustomer balik = new AlokasiPenerimaanPiutangCustomer();
				balik.setPenerimaan(rev);
				balik.setPiutangDoc(a.getPiutangDoc());
				balik.setNominal(a.getNominal().negate());
				session.save(balik);
				// Derivasi order: outstanding hidup lagi -> LUNAS mundur ke SIAP_TAGIH.
				PiutangCustomerDoc doc = a.getPiutangDoc();
				if (doc.getSalesOrder() != null
						&& SalesOrderLapangan.STATUS_LUNAS.equals(doc.getSalesOrder().getStatus())) {
					doc.getSalesOrder().setStatus(SalesOrderLapangan.STATUS_SIAP_TAGIH);
					session.saveOrUpdate(doc.getSalesOrder());
				}
				// Derivasi nota dibawa: nilai tertagih dikurangi kembali.
				if (sesi != null) {
					SpjSalesNota notaBawa = (SpjSalesNota) session.createCriteria(SpjSalesNota.class)
							.add(Restrictions.eq("spj", sesi.getSpj()))
							.add(Restrictions.eq("piutangDoc", doc)).setMaxResults(1).uniqueResult();
					if (notaBawa != null) {
						BigDecimal sisaTagih = notaBawa.getNilaiTertagih().subtract(a.getNominal());
						notaBawa.setNilaiTertagih(sisaTagih.signum() < 0 ? BigDecimal.ZERO : sisaTagih);
						notaBawa.setStatus(notaBawa.getNilaiTertagih().signum() > 0
								? SpjSalesNota.STATUS_PARTIAL : SpjSalesNota.STATUS_CARRIED);
						session.saveOrUpdate(notaBawa);
					}
				}
			}
			// Kas sesi: penerimaan tunai ber-sesi dibalik dgn baris REVERSAL negatif.
			if (sesi != null && PenerimaanPiutangCustomer.METODE_TUNAI.equals(asal.getMetode())) {
				NotaSalesKas kas = new NotaSalesKas();
				kas.setSesi(sesi);
				kas.setJenis(NotaSalesKas.JENIS_REVERSAL);
				kas.setNominal(asal.getNominal().negate());
				kas.setReferensi("REV-KWT-" + id);
				kas.setKeterangan("Reversal penagihan tunai: " + alasan);
				session.save(kas);
			}
			asal.setStatusDok(PenerimaanPiutangCustomer.DOK_DIBATALKAN);
			asal.setAlasanReversal(alasan);
			session.saveOrUpdate(asal);
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", rev.getId());
			hasil.put("nomor", str(rev.getNomor()));
		} catch (org.hibernate.exception.ConstraintViolationException dup) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			hasil.put("status", "00");
			hasil.put("idempotentReplay", true);
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// Reversal biaya sesi
	// =============================================================================================

	public static void expenseReverse(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		if (!pemilikAtauAdmin(ctx)) {
			tolak(hasil, "Reversal biaya hanya oleh Pemilik/Admin.");
			return;
		}
		Long id = optLong(request, "biaya_id");
		String alasan = request.optString("alasan", "").trim();
		if (id == null || alasan.isEmpty()) {
			tolak(hasil, "biaya_id dan alasan wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			String kodeRev = "REV-BIAYA-" + id;
			NotaSalesBiaya sudah = (NotaSalesBiaya) session.createCriteria(NotaSalesBiaya.class)
					.add(Restrictions.eq("kodeUnik", kodeRev)).setMaxResults(1).uniqueResult();
			if (sudah != null) {
				hasil.put("status", "00");
				hasil.put("id", sudah.getId());
				hasil.put("idempotentReplay", true);
				return;
			}
			NotaSalesBiaya asal = (NotaSalesBiaya) session.get(NotaSalesBiaya.class, id);
			if (asal == null) {
				tolak(hasil, "Biaya tidak ditemukan.");
				return;
			}
			if (!NotaSalesBiaya.DOK_AKTIF.equals(asal.getStatusDok())) {
				tolak(hasil, "Dokumen berstatus " + asal.getStatusDok() + " tidak bisa direversal.");
				return;
			}
			if (NotaSalesSession.STATUS_CLOSED.equals(asal.getSesi().getStatus())) {
				tolak(hasil, "Biaya bagian sesi yang SUDAH DITUTUP -- snapshot penutupan tidak boleh"
						+ " berubah. Koreksi lewat dokumen penyesuaian kantor.");
				return;
			}
			tx = session.beginTransaction();
			NotaSalesBiaya rev = new NotaSalesBiaya();
			rev.setSesi(asal.getSesi());
			rev.setKategori(asal.getKategori());
			rev.setUraian("REVERSAL biaya #" + id + ": " + alasan);
			rev.setNilai(asal.getNilai().negate());
			rev.setMetode(asal.getMetode());
			rev.setPenerima(asal.getPenerima());
			rev.setKodeUnik(kodeRev);
			rev.setDibuatOleh(tbmuser);
			rev.setStatusDok(NotaSalesBiaya.DOK_REVERSAL);
			rev.setReversalDari(id);
			session.save(rev);
			if (NotaSalesBiaya.METODE_TUNAI.equals(asal.getMetode())) {
				NotaSalesKas kas = new NotaSalesKas();
				kas.setSesi(asal.getSesi());
				kas.setJenis(NotaSalesKas.JENIS_REVERSAL);
				kas.setNominal(asal.getNilai()); // positif: kas kembali.
				kas.setReferensi(kodeRev);
				kas.setKeterangan("Reversal biaya tunai: " + alasan);
				session.save(kas);
			}
			asal.setStatusDok(NotaSalesBiaya.DOK_DIBATALKAN);
			asal.setAlasanReversal(alasan);
			session.saveOrUpdate(asal);
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", rev.getId());
		} catch (org.hibernate.exception.ConstraintViolationException dup) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			hasil.put("status", "00");
			hasil.put("idempotentReplay", true);
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// Siklus BG: DITERIMA -> CAIR | TOLAK (TOLAK => reversal otomatis)
	// =============================================================================================

	public static void payableBgStatus(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		bgStatus(ctx, tbmuser, request, hasil, true);
	}

	public static void collectionBgStatus(EbisnisActorContextResolver.ActorContext ctx,
			Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		bgStatus(ctx, tbmuser, request, hasil, false);
	}

	private static void bgStatus(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil, boolean sisiHutang) throws Exception {
		if (!pemilikAtauAdmin(ctx)) {
			tolak(hasil, "Status BG hanya diubah Pemilik/Admin.");
			return;
		}
		Long id = optLong(request, "id");
		String statusBaru = request.optString("status_bg", "").trim().toUpperCase();
		String alasan = request.optString("alasan", "").trim();
		if (id == null || (!"CAIR".equals(statusBaru) && !"TOLAK".equals(statusBaru))) {
			tolak(hasil, "id dan status_bg (CAIR|TOLAK) wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			String metode;
			String statusLama;
			Object dok;
			if (sisiHutang) {
				PembayaranHutangSupplier p = (PembayaranHutangSupplier) session
						.get(PembayaranHutangSupplier.class, id);
				if (p == null) {
					tolak(hasil, "Pembayaran tidak ditemukan.");
					return;
				}
				metode = p.getMetode();
				statusLama = p.getStatusBg();
				dok = p;
			} else {
				PenerimaanPiutangCustomer p = (PenerimaanPiutangCustomer) session
						.get(PenerimaanPiutangCustomer.class, id);
				if (p == null) {
					tolak(hasil, "Penerimaan tidak ditemukan.");
					return;
				}
				metode = p.getMetode();
				statusLama = p.getStatusBg();
				dok = p;
			}
			if (!"GIRO".equals(metode)) {
				tolak(hasil, "Status BG hanya utk dokumen metode GIRO.");
				return;
			}
			if (statusLama != null && !"DITERIMA".equals(statusLama)) {
				tolak(hasil, "Status BG sudah final (" + statusLama + ").");
				return;
			}
			tx = session.beginTransaction();
			dok.getClass().getMethod("setStatusBg", String.class).invoke(dok, statusBaru);
			dok.getClass().getMethod("setTanggalStatusBg", java.util.Date.class)
					.invoke(dok, ais.ui.util.WaktuUtil.getDate());
			session.saveOrUpdate(dok);
			tx.commit();
			hasil.put("status", "00");
			hasil.put("statusBg", statusBaru);
			// TOLAK: dana tidak pernah masuk/keluar -> terbitkan reversal otomatis (idempoten).
			if ("TOLAK".equals(statusBaru)) {
				JSONObject reqRev = new JSONObject();
				String alasanRev = "BG ditolak bank" + (alasan.isEmpty() ? "" : (": " + alasan));
				JSONObject hasilRev = new JSONObject();
				if (sisiHutang) {
					reqRev.put("pembayaran_id", id);
					reqRev.put("alasan", alasanRev);
					payablePaymentReverse(ctx, tbmuser, reqRev, hasilRev);
				} else {
					reqRev.put("penerimaan_id", id);
					reqRev.put("alasan", alasanRev);
					collectionReverse(ctx, tbmuser, reqRev, hasilRev);
				}
				hasil.put("reversal", hasilRev);
			}
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// Register riwayat cetak (append-only)
	// =============================================================================================

	public static void printLogCreate(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		String jenis = request.optString("jenis_dokumen", "").trim();
		if (jenis.isEmpty()) {
			tolak(hasil, "jenis_dokumen wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			LogCetak log = new LogCetak();
			log.setJenisDokumen(jenis);
			log.setReferensi(request.optString("referensi", "").trim());
			log.setParameterJson(request.optString("parameter", "").trim());
			log.setUserId(tbmuser.getUserId());
			log.setPerangkat(request.optString("perangkat", "").trim());
			session.save(log);
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", log.getId());
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void printLogList(EbisnisActorContextResolver.ActorContext ctx, JSONObject request,
			JSONObject hasil) throws Exception {
		if (!pemilikAtauAdmin(ctx)) {
			tolak(hasil, "Riwayat cetak hanya utk Pemilik/Admin.");
			return;
		}
		String jenis = request.optString("jenis_dokumen", "").trim();
		String referensi = request.optString("referensi", "").trim();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			org.hibernate.Criteria c = session.createCriteria(LogCetak.class);
			if (!jenis.isEmpty()) c.add(Restrictions.eq("jenisDokumen", jenis));
			if (!referensi.isEmpty()) c.add(Restrictions.eq("referensi", referensi));
			List rows = c.addOrder(Order.desc("id")).setMaxResults(200).list();
			JSONArray arr = new JSONArray();
			for (int i = 0; i < rows.size(); i++) {
				LogCetak l = (LogCetak) rows.get(i);
				JSONObject r = new JSONObject();
				r.put("id", l.getId());
				r.put("jenisDokumen", str(l.getJenisDokumen()));
				r.put("referensi", str(l.getReferensi()));
				r.put("userId", str(l.getUserId()));
				r.put("perangkat", str(l.getPerangkat()));
				r.put("waktu", str(l.getWaktu()));
				arr.put(r);
			}
			hasil.put("status", "00");
			hasil.put("rows", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}
}
