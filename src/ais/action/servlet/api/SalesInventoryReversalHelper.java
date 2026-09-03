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
		if (SalesInventoryReversalTenant.aktif(ctx)) {
			payablePaymentReverseTenant(ctx, tbmuser, id, alasan, hasil);
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
		if (SalesInventoryReversalTenant.aktif(ctx)) {
			collectionReverseTenant(ctx, tbmuser, id, alasan, hasil);
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
		if (SalesInventoryReversalTenant.aktif(ctx)) {
			expenseReverseTenant(ctx, tbmuser, id, alasan, hasil);
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
		if (SalesInventoryReversalTenant.aktif(ctx)) {
			bgStatusTenant(ctx, tbmuser, id, statusBaru, alasan, hasil, sisiHutang);
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
		if (SalesInventoryReversalTenant.aktif(ctx)) {
			printLogCreateTenant(ctx, tbmuser, request, jenis, hasil);
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
		if (SalesInventoryReversalTenant.aktif(ctx)) {
			printLogListTenant(ctx, jenis, referensi, hasil);
			return;
		}
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

	// =============================================================================================
	// Jalur schema tenant
	// =============================================================================================

	/**
	 * Pembalikan pembayaran hutang pada schema tenant.
	 *
	 * <p>Urutannya sengaja sama dengan jalur legacy: periksa kunci idempotensi, ambil dokumen
	 * asal, tolak bila statusnya bukan AKTIF, lalu dalam SATU transaksi terbitkan dokumen cermin
	 * bernilai negatif, cerminkan alokasinya, tandai dokumen asal DIBATALKAN, dan catat
	 * alasannya pada {@code reversal_log}.</p>
	 *
	 * <p>Keempatnya harus satu transaksi. Dokumen pembalik tanpa alokasi pembalik akan
	 * mengembalikan uangnya tetapi TIDAK mengembalikan sisa hutangnya, dan itu justru keadaan
	 * yang paling menyesatkan -- lihat blok 2 pada {@code uji-kesetaraan-reversal.sql}.</p>
	 *
	 * <p>Penjaga pengulangan ada dua lapis: pemeriksaan kunci di muka menangani percobaan
	 * berurutan (klik ganda, klien mengulang setelah waktu habis), dan indeks unik parsial dari
	 * migrasi v11 menangani dua permintaan yang benar-benar bersamaan. Lapis kedua itulah yang
	 * sebelum v11 tidak ada sama sekali.</p>
	 */
	private static void payablePaymentReverseTenant(EbisnisActorContextResolver.ActorContext ctx,
			Tbmuser tbmuser, Long id, String alasan, JSONObject hasil) throws Exception {
		String skema = SalesInventoryReversalTenant.skema(ctx);
		String kodeRev = "REV-PHS-" + id;
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			java.sql.PreparedStatement psCek = session.connection().prepareStatement(
					SalesInventoryReversalTenant.cariPembalik(skema));
			psCek.setString(1, kodeRev);
			java.sql.ResultSet rsCek = psCek.executeQuery();
			boolean sudahAda = rsCek.next();
			long idSudah = sudahAda ? rsCek.getLong(1) : 0;
			rsCek.close();
			psCek.close();
			if (sudahAda) {
				hasil.put("status", "00");
				hasil.put("id", idSudah);
				hasil.put("idempotentReplay", true);
				return;
			}

			java.sql.PreparedStatement psAsal = session.connection().prepareStatement(
					SalesInventoryReversalTenant.asalPembayaran(skema));
			psAsal.setLong(1, id.longValue());
			java.sql.ResultSet rsAsal = psAsal.executeQuery();
			if (!rsAsal.next()) {
				rsAsal.close();
				psAsal.close();
				tolak(hasil, "Pembayaran tidak ditemukan.");
				return;
			}
			String nomorAsal = str(rsAsal.getString(1));
			long supplierId = rsAsal.getLong(2);
			BigDecimal nilai = rsAsal.getBigDecimal(3);
			String caraBayar = rsAsal.getString(4);
			String nomorBg = rsAsal.getString(5);
			String namaBank = rsAsal.getString(6);
			String statusAsal = str(rsAsal.getString(7));
			rsAsal.close();
			psAsal.close();
			if (!PembayaranHutangSupplier.DOK_AKTIF.equals(statusAsal)) {
				tolak(hasil, "Dokumen berstatus " + statusAsal + " tidak bisa direversal.");
				return;
			}

			tx = session.beginTransaction();
			java.sql.PreparedStatement psRev = session.connection().prepareStatement(
					SalesInventoryReversalTenant.sisipPembalikPembayaran(skema),
					java.sql.Statement.RETURN_GENERATED_KEYS);
			// nomor_dokumen wajib pada model tenant; jalur legacy tidak memberi nomor pada
			// dokumen pembalik AP, jadi nomornya diturunkan dari nomor asalnya.
			psRev.setString(1, "REV-" + nomorAsal);
			psRev.setLong(2, supplierId);
			psRev.setString(3, caraBayar);
			psRev.setString(4, nomorBg);
			psRev.setString(5, namaBank);
			psRev.setBigDecimal(6, nilai.negate());
			psRev.setString(7, "REVERSAL pembayaran #" + id + ": " + alasan);
			psRev.setString(8, kodeRev);
			psRev.setLong(9, id.longValue());
			psRev.setString(10, tbmuser.getUserId());
			psRev.executeUpdate();
			long idRev = 0;
			java.sql.ResultSet gk = psRev.getGeneratedKeys();
			if (gk.next()) {
				idRev = gk.getLong(1);
			}
			gk.close();
			psRev.close();
			if (idRev <= 0) {
				tx.rollback();
				tolak(hasil, "Dokumen pembalik gagal disimpan.");
				return;
			}

			java.sql.PreparedStatement psAlok = session.connection().prepareStatement(
					SalesInventoryReversalTenant.cerminkanAlokasi(skema));
			psAlok.setLong(1, idRev);
			psAlok.setString(2, tbmuser.getUserId());
			psAlok.setLong(3, id.longValue());
			psAlok.executeUpdate();
			psAlok.close();

			java.sql.PreparedStatement psBatal = session.connection().prepareStatement(
					SalesInventoryReversalTenant.batalkanAsal(skema));
			psBatal.setString(1, alasan);
			psBatal.setLong(2, id.longValue());
			psBatal.executeUpdate();
			psBatal.close();

			java.sql.PreparedStatement psLog = session.connection().prepareStatement(
					SalesInventoryReversalTenant.catatReversal(skema));
			psLog.setLong(1, id.longValue());
			psLog.setString(2, alasan);
			psLog.setString(3, tbmuser.getUserId());
			psLog.executeUpdate();
			psLog.close();

			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", idRev);
		} catch (java.sql.SQLException dup) {
			// Indeks unik v11 pada idempotency_key: dua permintaan kembar yang bersamaan.
			try {
				if (tx != null && tx.isActive()) {
					tx.rollback();
				}
			} catch (Exception ignore) {
			}
			if (dup.getSQLState() != null && dup.getSQLState().startsWith("23")) {
				hasil.put("status", "00");
				hasil.put("idempotentReplay", true);
			} else {
				throw dup;
			}
		} catch (Exception e) {
			try {
				if (tx != null && tx.isActive()) {
					tx.rollback();
				}
			} catch (Exception ignore) {
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Pencatatan riwayat cetak pada schema tenant.
	 *
	 * <p>{@code parameter} legacy disimpan pada kolom {@code alasan} -- satu-satunya kolom teks
	 * bebas yang tersedia. Namanya tidak cocok, tetapi membuang isian yang pada jalur legacy
	 * tersimpan jelas lebih buruk.</p>
	 */
	private static void printLogCreateTenant(EbisnisActorContextResolver.ActorContext ctx,
			Tbmuser tbmuser, JSONObject request, String jenis, JSONObject hasil) throws Exception {
		String skema = SalesInventoryReversalTenant.skema(ctx);
		String referensi = request.optString("referensi", "").trim();
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			int cetakanKe = 1;
			java.sql.PreparedStatement psNo = session.connection().prepareStatement(
					SalesInventoryReversalTenant.cetakanBerikut(skema));
			psNo.setString(1, jenis);
			psNo.setString(2, referensi);
			java.sql.ResultSet rsNo = psNo.executeQuery();
			if (rsNo.next()) {
				cetakanKe = rsNo.getInt(1);
			}
			rsNo.close();
			psNo.close();

			java.sql.PreparedStatement ins = session.connection().prepareStatement(
					SalesInventoryReversalTenant.sisipLogCetak(skema),
					java.sql.Statement.RETURN_GENERATED_KEYS);
			ins.setString(1, jenis);
			ins.setString(2, referensi);
			ins.setInt(3, cetakanKe);
			ins.setString(4, tbmuser.getUserId());
			ins.setString(5, request.optString("perangkat", "").trim());
			ins.setString(6, request.optString("parameter", "").trim());
			ins.executeUpdate();
			long idBaru = 0;
			java.sql.ResultSet gk = ins.getGeneratedKeys();
			if (gk.next()) {
				idBaru = gk.getLong(1);
			}
			gk.close();
			ins.close();
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", idBaru);
		} catch (Exception e) {
			try {
				if (tx != null && tx.isActive()) {
					tx.rollback();
				}
			} catch (Exception ignore) {
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Riwayat cetak pada schema tenant: ENAM medan yang sama dengan jalur legacy. */
	private static void printLogListTenant(EbisnisActorContextResolver.ActorContext ctx,
			String jenis, String referensi, JSONObject hasil) throws Exception {
		String skema = SalesInventoryReversalTenant.skema(ctx);
		StringBuilder where = new StringBuilder(" WHERE 1=1");
		if (!jenis.isEmpty()) {
			where.append(" AND l.dokumen_tipe = ?");
		}
		if (!referensi.isEmpty()) {
			where.append(" AND l.nomor_dokumen = ?");
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					SalesInventoryReversalTenant.selectLogCetak(skema, where.toString()));
			int ix = 1;
			if (!jenis.isEmpty()) {
				ps.setString(ix++, jenis);
			}
			if (!referensi.isEmpty()) {
				ps.setString(ix++, referensi);
			}
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject r = new JSONObject();
				r.put("id", rs.getLong(1));
				r.put("jenisDokumen", str(rs.getString(2)));
				r.put("referensi", str(rs.getString(3)));
				r.put("userId", str(rs.getString(4)));
				r.put("perangkat", str(rs.getString(5)));
				r.put("waktu", str(rs.getTimestamp(6)));
				arr.put(r);
			}
			rs.close();
			ps.close();
			hasil.put("status", "00");
			hasil.put("rows", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}
	/**
	 * Pembalikan biaya trip pada schema tenant.
	 *
	 * <p>Bentuknya sama dengan jalur legacy: dokumen cermin bernilai negatif, berstatus REVERSAL,
	 * menunjuk asalnya, dan asalnya ditandai DIBATALKAN. Bila biayanya dibayar tunai, kas yang
	 * dipegang sales <b>kembali naik</b> lewat satu baris buku kas bertanda positif — itu sebabnya
	 * pembalikan biaya menunggu bundel v12, bukan sekadar kolom penunjuk.</p>
	 *
	 * <p>Sesi yang sudah ditutup tetap ditolak. Snapshot penutupan tidak boleh berubah diam-diam;
	 * koreksinya lewat dokumen penyesuaian kantor, sama seperti jalur legacy.</p>
	 *
	 * <p>Dua medan legacy tidak punya kolom di sini. {@code penerima} memang tidak ada pada
	 * {@code sales_trip_biaya}; {@code alasanReversal} ditampung {@code reversal_log}, yang
	 * justru menyimpan lebih banyak daripada legacy — pelaku dan waktunya ikut tercatat.</p>
	 */
	private static void expenseReverseTenant(EbisnisActorContextResolver.ActorContext ctx,
			Tbmuser tbmuser, Long id, String alasan, JSONObject hasil) throws Exception {
		String skema = SalesInventoryReversalTenant.skema(ctx);
		String kodeRev = "REV-BIAYA-" + id;
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			java.sql.PreparedStatement psCek = session.connection().prepareStatement(
					SalesInventoryTripTenant.cariBiayaPembalik(skema));
			psCek.setString(1, kodeRev);
			java.sql.ResultSet rsCek = psCek.executeQuery();
			boolean sudah = rsCek.next();
			long idSudah = sudah ? rsCek.getLong(1) : 0;
			rsCek.close();
			psCek.close();
			if (sudah) {
				hasil.put("status", "00");
				hasil.put("id", idSudah);
				hasil.put("idempotentReplay", true);
				return;
			}

			java.sql.PreparedStatement psAsal = session.connection().prepareStatement(
					SalesInventoryTripTenant.biayaUntukBalik(skema));
			psAsal.setLong(1, id.longValue());
			java.sql.ResultSet rsAsal = psAsal.executeQuery();
			if (!rsAsal.next()) {
				rsAsal.close();
				psAsal.close();
				tolak(hasil, "Biaya tidak ditemukan.");
				return;
			}
			long tripId = rsAsal.getLong(1);
			String kategori = str(rsAsal.getString(2));
			BigDecimal nilai = rsAsal.getBigDecimal(4);
			String caraBayar = str(rsAsal.getString(5));
			String statusAsal = str(rsAsal.getString(6));
			String statusTrip = str(rsAsal.getString(7));
			// Penunjuk kategori (v15) ikut dibawa; tanpa ini baris pembalik kehilangan
			// kategorinya dan akun bebannya tidak lagi dapat ditelusuri.
			Long kategoriId = rsAsal.getObject(8) == null ? null
					: Long.valueOf(rsAsal.getLong(8));
			rsAsal.close();
			psAsal.close();
			if (!"AKTIF".equals(statusAsal)) {
				tolak(hasil, "Dokumen berstatus " + statusAsal + " tidak bisa direversal.");
				return;
			}
			if (NotaSalesSession.STATUS_CLOSED.equals(statusTrip)) {
				tolak(hasil, "Biaya bagian sesi yang SUDAH DITUTUP -- snapshot penutupan tidak"
						+ " boleh berubah. Koreksi lewat dokumen penyesuaian kantor.");
				return;
			}

			tx = session.beginTransaction();
			java.sql.PreparedStatement ins = session.connection().prepareStatement(
					SalesInventoryTripTenant.sisipBiayaPembalik(skema),
					java.sql.Statement.RETURN_GENERATED_KEYS);
			ins.setLong(1, tripId);
			ins.setString(2, kategori);
			if (kategoriId == null) {
				ins.setNull(3, java.sql.Types.BIGINT);
			} else {
				ins.setLong(3, kategoriId.longValue());
			}
			ins.setString(4, "REVERSAL biaya #" + id + ": " + alasan);
			ins.setBigDecimal(5, nilai.negate());
			ins.setString(6, caraBayar);
			ins.setString(7, kodeRev);
			ins.setLong(8, id.longValue());
			ins.setString(9, tbmuser.getUserId());
			ins.executeUpdate();
			long idRev = 0;
			java.sql.ResultSet gk = ins.getGeneratedKeys();
			if (gk.next()) {
				idRev = gk.getLong(1);
			}
			gk.close();
			ins.close();
			if (idRev <= 0) {
				tx.rollback();
				tolak(hasil, "Biaya pembalik gagal disimpan.");
				return;
			}

			if ("TUNAI".equals(caraBayar)) {
				// Kas kembali: bertanda POSITIF, sebagaimana jalur legacy menyisipkan baris
				// REVERSAL bernilai asal.getNilai() yang memang positif.
				java.sql.PreparedStatement kas = session.connection().prepareStatement(
						SalesInventoryTripTenant.sisipKas(skema));
				try {
					kas.setLong(1, tripId);
					kas.setString(2, ais.service.tenant.TenantKasTrip.REVERSAL);
					kas.setBigDecimal(3, nilai);
					kas.setString(4, kodeRev);
					kas.setString(5, "Reversal biaya tunai: " + alasan);
					kas.setString(6, kodeRev);
					kas.setString(7, tbmuser.getUserId());
					kas.executeUpdate();
				} finally {
					kas.close();
				}
			}

			java.sql.PreparedStatement psBatal = session.connection().prepareStatement(
					SalesInventoryTripTenant.batalkanBiaya(skema));
			psBatal.setLong(1, id.longValue());
			psBatal.executeUpdate();
			psBatal.close();

			java.sql.PreparedStatement psLog = session.connection().prepareStatement(
					SalesInventoryTripTenant.catatReversalBiaya(skema));
			psLog.setLong(1, id.longValue());
			psLog.setString(2, alasan);
			psLog.setString(3, tbmuser.getUserId());
			psLog.executeUpdate();
			psLog.close();

			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", idRev);
		} catch (java.sql.SQLException dup) {
			try {
				if (tx != null && tx.isActive()) {
					tx.rollback();
				}
			} catch (Exception ignore) {
			}
			if (dup.getSQLState() != null && dup.getSQLState().startsWith("23")) {
				hasil.put("status", "00");
				hasil.put("idempotentReplay", true);
			} else {
				throw dup;
			}
		} catch (Exception e) {
			try {
				if (tx != null && tx.isActive()) {
					tx.rollback();
				}
			} catch (Exception ignore) {
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}
	/**
	 * Pembalikan penerimaan piutang pada schema tenant.
	 *
	 * <p>Lima hal yang dikerjakan jalur legacy dikerjakan juga di sini: dokumen cermin bernilai
	 * negatif, alokasi negatif, kas trip turun bila penagihannya tunai, status nota bawaan
	 * mundur, dan status sales order mundur dari LUNAS. Dokumen asal ditandai DIBATALKAN dan
	 * tetap terbaca di riwayat.</p>
	 *
	 * <p><b>Satu langkah legacy lenyap, dan itu perbaikan.</b> Legacy menurunkan
	 * {@code SpjSalesNota.nilaiTertagih} lalu menjepitnya ke nol supaya tidak negatif. Model
	 * tenant menurunkan angka itu dari alokasi, dan alokasi pembalik memang negatif — jumlahnya
	 * turun sendiri. Tidak ada pengurang yang bisa terlupa, dan tidak ada penjepit yang
	 * menyembunyikan kelupaan itu.</p>
	 *
	 * <p>Sesi yang sudah ditutup tetap ditolak: snapshot penutupan tidak boleh berubah
	 * diam-diam.</p>
	 */
	private static void collectionReverseTenant(EbisnisActorContextResolver.ActorContext ctx,
			Tbmuser tbmuser, Long id, String alasan, JSONObject hasil) throws Exception {
		String skema = SalesInventoryReversalTenant.skema(ctx);
		String oleh = tbmuser == null || tbmuser.getUserId() == null ? "" : tbmuser.getUserId();
		String kodeRev = "REV-KWT-" + id;
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			java.sql.PreparedStatement psCek = session.connection().prepareStatement(
					SalesInventoryReversalTenant.cariPembalikPenerimaan(skema));
			psCek.setString(1, kodeRev);
			java.sql.ResultSet rsCek = psCek.executeQuery();
			boolean sudah = rsCek.next();
			long idSudah = sudah ? rsCek.getLong(1) : 0;
			rsCek.close();
			psCek.close();
			if (sudah) {
				hasil.put("status", "00");
				hasil.put("id", idSudah);
				hasil.put("idempotentReplay", true);
				return;
			}

			java.sql.PreparedStatement psAsal = session.connection().prepareStatement(
					SalesInventoryReversalTenant.asalPenerimaan(skema));
			psAsal.setLong(1, id.longValue());
			java.sql.ResultSet rsAsal = psAsal.executeQuery();
			if (!rsAsal.next()) {
				rsAsal.close();
				psAsal.close();
				tolak(hasil, "Penerimaan tidak ditemukan.");
				return;
			}
			String nomorAsal = str(rsAsal.getString(1));
			long customerId = rsAsal.getLong(2);
			Long salesId = rsAsal.getObject(3) == null ? null : Long.valueOf(rsAsal.getLong(3));
			BigDecimal nilai = rsAsal.getBigDecimal(4);
			String caraBayar = str(rsAsal.getString(5));
			String nomorBg = rsAsal.getString(6);
			String namaBank = rsAsal.getString(7);
			String statusAsal = str(rsAsal.getString(8));
			Long tripId = rsAsal.getObject(9) == null ? null : Long.valueOf(rsAsal.getLong(9));
			String statusTrip = str(rsAsal.getString(10));
			Long spjId = rsAsal.getObject(11) == null ? null : Long.valueOf(rsAsal.getLong(11));
			rsAsal.close();
			psAsal.close();
			if (!PenerimaanPiutangCustomer.DOK_AKTIF.equals(statusAsal)) {
				tolak(hasil, "Dokumen berstatus " + statusAsal + " tidak bisa direversal.");
				return;
			}
			if (tripId != null && NotaSalesSession.STATUS_CLOSED.equals(statusTrip)) {
				tolak(hasil, "Penerimaan ini bagian sesi yang SUDAH DITUTUP -- snapshot penutupan"
						+ " tidak boleh berubah. Koreksi lewat dokumen penyesuaian kantor.");
				return;
			}

			tx = session.beginTransaction();
			// Dicuplik sebelum apa pun berubah pada dokumen asalnya.
			String sebelumAsal = SalesInventoryAudit.cuplikan(session, skema,
					"penerimaan_piutang", id);
			java.sql.PreparedStatement psRev = session.connection().prepareStatement(
					SalesInventoryReversalTenant.sisipPembalikPenerimaan(skema),
					java.sql.Statement.RETURN_GENERATED_KEYS);
			psRev.setString(1, "REV-" + nomorAsal);
			psRev.setLong(2, customerId);
			if (salesId == null) {
				psRev.setNull(3, java.sql.Types.BIGINT);
			} else {
				psRev.setLong(3, salesId.longValue());
			}
			psRev.setString(4, caraBayar);
			psRev.setString(5, nomorBg);
			psRev.setString(6, namaBank);
			psRev.setBigDecimal(7, nilai.negate());
			psRev.setString(8, "REVERSAL kwitansi " + nomorAsal + ": " + alasan);
			psRev.setString(9, kodeRev);
			psRev.setLong(10, id.longValue());
			if (tripId == null) {
				psRev.setNull(11, java.sql.Types.BIGINT);
			} else {
				psRev.setLong(11, tripId.longValue());
			}
			psRev.setString(12, oleh);
			psRev.executeUpdate();
			long idRev = 0;
			java.sql.ResultSet gk = psRev.getGeneratedKeys();
			if (gk.next()) {
				idRev = gk.getLong(1);
			}
			gk.close();
			psRev.close();
			if (idRev <= 0) {
				tx.rollback();
				tolak(hasil, "Dokumen pembalik gagal disimpan.");
				return;
			}

			// Daftar alokasi asal dibaca SEBELUM dicerminkan, supaya turunannya sesudah
			// pencerminan dapat dibandingkan terhadap dokumen yang tepat.
			java.util.List<Long> piutangIds = new java.util.ArrayList<Long>();
			java.sql.PreparedStatement psAl = session.connection().prepareStatement(
					SalesInventoryReversalTenant.alokasiPenerimaan(skema));
			psAl.setLong(1, id.longValue());
			java.sql.ResultSet rsAl = psAl.executeQuery();
			while (rsAl.next()) {
				piutangIds.add(Long.valueOf(rsAl.getLong(1)));
			}
			rsAl.close();
			psAl.close();

			java.sql.PreparedStatement psMirror = session.connection().prepareStatement(
					SalesInventoryReversalTenant.cerminkanAlokasiPiutang(skema));
			psMirror.setLong(1, idRev);
			psMirror.setString(2, oleh);
			psMirror.setLong(3, id.longValue());
			psMirror.executeUpdate();
			psMirror.close();

			if (tripId != null
					&& PenerimaanPiutangCustomer.METODE_TUNAI.equals(caraBayar)) {
				// Kas yang dipegang sales turun kembali. Bertanda NEGATIF.
				java.sql.PreparedStatement kas = session.connection().prepareStatement(
						SalesInventoryTripTenant.sisipKas(skema));
				try {
					kas.setLong(1, tripId.longValue());
					kas.setString(2, ais.service.tenant.TenantKasTrip.REVERSAL);
					kas.setBigDecimal(3, nilai.negate());
					kas.setString(4, kodeRev);
					kas.setString(5, "Reversal penagihan tunai: " + alasan);
					kas.setString(6, kodeRev);
					kas.setString(7, oleh);
					kas.executeUpdate();
				} finally {
					kas.close();
				}
			}

			for (int i = 0; i < piutangIds.size(); i++) {
				long did = piutangIds.get(i).longValue();
				double sisa = sisaPiutangReversal(session, skema, did);
				if (spjId != null) {
					// HANYA statusnya; nilai tertagihnya sudah turun sendiri lewat alokasi
					// pembalik yang bernilai negatif.
					java.sql.PreparedStatement psS = session.connection().prepareStatement(
							SalesInventoryReceivableTenant.ubahStatusNotaBawaan(skema));
					try {
						psS.setString(1, sisa > 0.009 ? SpjSalesNota.STATUS_PARTIAL
								: SpjSalesNota.STATUS_PAID);
						psS.setLong(2, spjId.longValue());
						psS.setLong(3, did);
						psS.executeUpdate();
					} finally {
						psS.close();
					}
				}
				if (sisa > 0.009) {
					// Outstanding hidup lagi: order yang sempat LUNAS mundur ke SIAP_TAGIH.
					java.sql.PreparedStatement psO = session.connection().prepareStatement(
							SalesInventoryReceivableTenant.ubahStatusOrderSiapTagih(skema));
					try {
						psO.setString(1, oleh);
						psO.setLong(2, orderDariPiutangReversal(session, skema, did));
						psO.executeUpdate();
					} finally {
						psO.close();
					}
				}
			}

			java.sql.PreparedStatement psBatal = session.connection().prepareStatement(
					SalesInventoryReversalTenant.batalkanPenerimaan(skema));
			psBatal.setString(1, alasan);
			psBatal.setLong(2, id.longValue());
			psBatal.executeUpdate();
			psBatal.close();

			java.sql.PreparedStatement psLog = session.connection().prepareStatement(
					SalesInventoryReversalTenant.catatReversalPenerimaan(skema));
			psLog.setLong(1, id.longValue());
			psLog.setString(2, alasan);
			psLog.setString(3, oleh);
			psLog.executeUpdate();
			psLog.close();

			// DUA baris, satu untuk tiap dokumen yang tersentuh. Pembaliknya adalah dokumen
			// BARU (ADD), sedangkan yang dibalik dicatat DEL: barisnya tetap ada, tetapi bagi
			// pemakai data itulah pembatalannya. Mencatat hanya salah satunya membuat riwayat
			// salah satu dokumen bungkam tentang peristiwa yang justru paling penting baginya.
			SalesInventoryAudit.catatBaru(session, ctx, "penagihan_balik", skema,
					"penerimaan_piutang", Long.valueOf(idRev));
			SalesInventoryAudit.catat(session, ctx, "penagihan_balik", "penerimaan_piutang", id,
					ais.service.tenant.TenantAuditWriter.REVTYPE_DEL, sebelumAsal,
					SalesInventoryAudit.cuplikan(session, skema, "penerimaan_piutang", id));
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", idRev);
			hasil.put("nomor", "REV-" + nomorAsal);
		} catch (java.sql.SQLException dup) {
			try {
				if (tx != null && tx.isActive()) {
					tx.rollback();
				}
			} catch (Exception ignore) {
			}
			if (dup.getSQLState() != null && dup.getSQLState().startsWith("23")) {
				hasil.put("status", "00");
				hasil.put("idempotentReplay", true);
			} else {
				throw dup;
			}
		} catch (Exception e) {
			try {
				if (tx != null && tx.isActive()) {
					tx.rollback();
				}
			} catch (Exception ignore) {
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Sisa satu dokumen piutang pada schema tenant; dihitung dari alokasinya. */
	private static double sisaPiutangReversal(Session session, String skema, long piutangId)
			throws Exception {
		java.sql.PreparedStatement ps = session.connection().prepareStatement(
				SalesInventoryReceivableTenant.sisaSatuPiutang(skema));
		try {
			ps.setLong(1, piutangId);
			java.sql.ResultSet rs = ps.executeQuery();
			double sisa = rs.next() ? rs.getDouble(1) : -1;
			rs.close();
			return sisa;
		} finally {
			ps.close();
		}
	}

	/** Sales order asal satu piutang; 0 bila tidak berasal dari order. */
	private static long orderDariPiutangReversal(Session session, String skema, long piutangId)
			throws Exception {
		java.sql.PreparedStatement ps = session.connection().prepareStatement(
				SalesInventoryReceivableTenant.orderDariPiutang(skema));
		try {
			ps.setLong(1, piutangId);
			java.sql.ResultSet rs = ps.executeQuery();
			long id = 0;
			if (rs.next() && rs.getObject(1) != null) {
				id = rs.getLong(1);
			}
			rs.close();
			return id;
		} finally {
			ps.close();
		}
	}
	/**
	 * Siklus status giro pada schema tenant.
	 *
	 * <p>Bentuknya sama dengan jalur legacy: hanya dokumen bermetode GIRO, hanya dari keadaan
	 * DITERIMA (yang pada basis data berarti {@code NULL}), dan hanya sekali — sesudah CAIR atau
	 * TOLAK statusnya final.</p>
	 *
	 * <p><b>TOLAK menerbitkan pembalikan.</b> Giro yang ditolak berarti uangnya tidak pernah
	 * benar-benar berpindah, sehingga hutang atau piutangnya harus hidup kembali. Pembalikannya
	 * dipanggil lewat aksi publiknya sendiri — {@code payablePaymentReverse} atau
	 * {@code collectionReverse} — yang keduanya sudah mengenali jalur tenant dan sudah idempoten
	 * lewat kunci {@code REV-PHS-<id>} / {@code REV-KWT-<id>}. Memanggilnya kembali, bukan
	 * menyalin isinya, membuat kedua jalan itu tidak mungkin berselisih.</p>
	 *
	 * <p>Pembalikannya sengaja dijalankan <b>sesudah</b> status giro tersimpan, sama seperti
	 * legacy: bila pembalikannya gagal, statusnya sudah tercatat dan pembalikannya dapat diulang
	 * tanpa menggandakan — kebalikannya akan menyembunyikan penolakan gironya sama sekali.</p>
	 */
	private static void bgStatusTenant(EbisnisActorContextResolver.ActorContext ctx,
			Tbmuser tbmuser, Long id, String statusBaru, String alasan, JSONObject hasil,
			boolean sisiHutang) throws Exception {
		String skema = SalesInventoryReversalTenant.skema(ctx);
		String tabel = sisiHutang ? "pembayaran_hutang" : "penerimaan_piutang";
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					SalesInventoryReversalTenant.giroUntukStatus(skema, tabel));
			ps.setLong(1, id.longValue());
			java.sql.ResultSet rs = ps.executeQuery();
			if (!rs.next()) {
				rs.close();
				ps.close();
				tolak(hasil, sisiHutang ? "Pembayaran tidak ditemukan."
						: "Penerimaan tidak ditemukan.");
				return;
			}
			String metode = str(rs.getString(1));
			String statusLama = rs.getString(2);
			rs.close();
			ps.close();
			if (!"GIRO".equals(metode)) {
				tolak(hasil, "Status BG hanya utk dokumen metode GIRO.");
				return;
			}
			// NULL berarti DITERIMA -- baru diterima, belum ada kabarnya.
			if (statusLama != null && !"DITERIMA".equals(statusLama)) {
				tolak(hasil, "Status BG sudah final (" + statusLama + ").");
				return;
			}
			tx = session.beginTransaction();
			java.sql.PreparedStatement upd = session.connection().prepareStatement(
					SalesInventoryReversalTenant.ubahStatusGiro(skema, tabel));
			try {
				upd.setString(1, statusBaru);
				upd.setDate(2, new java.sql.Date(ais.ui.util.WaktuUtil.getDate().getTime()));
				upd.setLong(3, id.longValue());
				upd.executeUpdate();
			} finally {
				upd.close();
			}
			tx.commit();
			hasil.put("status", "00");
			hasil.put("statusBg", statusBaru);
		} catch (Exception e) {
			try {
				if (tx != null && tx.isActive()) {
					tx.rollback();
				}
			} catch (Exception ignore) {
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		// Di luar sesi di atas, supaya pembalikannya membuka sesinya sendiri -- sama seperti
		// jalur legacy yang memanggil aksinya kembali, bukan menyalin isinya.
		if ("TOLAK".equals(statusBaru)) {
			JSONObject reqRev = new JSONObject();
			String alasanRev = "BG ditolak bank" + (alasan.isEmpty() ? "" : (": " + alasan));
			JSONObject hasilRev = new JSONObject();
			reqRev.put("alasan", alasanRev);
			if (sisiHutang) {
				reqRev.put("pembayaran_id", id);
				payablePaymentReverse(ctx, tbmuser, reqRev, hasilRev);
			} else {
				reqRev.put("penerimaan_id", id);
				collectionReverse(ctx, tbmuser, reqRev, hasilRev);
			}
			hasil.put("reversal", hasilRev);
		}
	}
}
