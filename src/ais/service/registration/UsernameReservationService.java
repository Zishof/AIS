package ais.service.registration;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.security.PasswordHashService;
import ais.database.model.Pendaftar;
import ais.database.model.Tbmuser;
import ais.database.model.tenant.PendaftaranAuditEvent;
import ais.database.model.tenant.PendaftaranTenant;
import ais.database.model.tenant.SchemaNameReservation;
import ais.database.model.tenant.TenantRegistry;

/**
 * <h3>Cek benturan + reservasi atomik username/schema tenant (§3.4 dokumen master).</h3>
 *
 * <p>{@link #alasanTidakTersedia} = pemeriksaan informatif (dipakai {@code check_username} &
 * pra-validasi submit); KEBENARAN FINAL tetap INSERT/UPDATE {@link SchemaNameReservation} (unique
 * {@code normalized_name}) di dalam transaction submit -- race dua submit bersamaan diserialisasi
 * constraint DB / optimistic lock, BUKAN oleh cek ini.</p>
 *
 * <p>Sumber benturan yang diperiksa: reserved list (bawaan+konfigurasi), {@code pendaftar.domain},
 * {@code tenant_registry.slug/schema_name}, reservation aktif (lihat {@link #menghalangi}),
 * {@code tbmuser.userid}, {@code koperasi.pedagang.userid} (COUNT&gt;0 -- TIDAK unik dalam praktik,
 * lihat audit §8), dan {@code pg_namespace}.</p>
 *
 * <h4>Masa berlaku reservasi (celah tertutup 2026-09)</h4>
 * <p>
 * {@code normalized_name} unique utk SELURUH baris tabel ini apa pun statusnya -- karena itu hanya
 * PERNAH ada nol atau satu baris {@link SchemaNameReservation} per nama (pelepasan/kedaluwarsa
 * berupa perubahan status pada baris yang sama, bukan baris baru; lihat Javadoc entity).
 * {@link #menghalangi} adalah SATU-SATUNYA definisi "baris ini masih mengunci nama", dipakai baik
 * oleh {@link #alasanTidakTersedia} (ketersediaan) maupun {@link #reservasi} (reuse-atau-insert)
 * supaya keduanya tidak pernah berbeda pendapat.
 * </p>
 * <p>
 * Baris RESERVED yang {@code expiresAt}-nya sudah lewat HANYA berhenti menghalangi bila permohonan
 * pemiliknya masih persis {@code STATUS_EMAIL_VERIFICATION_PENDING}/{@code STATUS_SUBMITTED} --
 * pola pemeriksaan yang sama dipakai {@code PendaftaranTenantService.verifikasiEmail}/
 * {@code resendVerifikasi}/{@code cancel}. Permohonan yang sudah melangkah lebih jauh
 * (VERIFIED/REVIEW_PENDING/PROVISIONING_QUEUED/PROVISIONING/READY/...) TETAP menghalangi tanpa
 * memandang {@code expiresAt}, sehingga penyapu latar ({@code ReservationExpiryScheduler}, yang
 * memanggil {@link #cariKandidatKedaluwarsa}/{@link #kedaluwarsakan}) tidak pernah bisa merebut
 * nama dari permohonan yang masih benar-benar berjalan. Reservasi administratif (tanpa permohonan
 * pemilik) tidak pernah dianggap basi otomatis di sini -- hanya pelepasan admin eksplisit
 * ({@code PendaftaranTenantAdminService.releaseReservation}) yang boleh melepasnya.
 * </p>
 */
public final class UsernameReservationService {

	/** Konstruktor privat -- kelas ini murni kumpulan method statis (utility), tidak dimaksudkan untuk diinstansiasi. */
	private UsernameReservationService() {
	}

	/** Kode alasan tidak tersedia (stabil utk UI/test); null = tersedia. */
	public static String alasanTidakTersedia(Session session, String usernameNormalized) {
		if (!PendaftaranValidationService.usernameValid(usernameNormalized)) {
			return "USERNAME_INVALID";
		}
		if (PendaftaranValidationService.usernameReserved(usernameNormalized)) {
			return "USERNAME_RESERVED";
		}
		if (hitung(session, Pendaftar.class, "domain", usernameNormalized) > 0) {
			return "USERNAME_TAKEN_DOMAIN";
		}
		if (hitung(session, TenantRegistry.class, "slug", usernameNormalized) > 0
				|| hitung(session, TenantRegistry.class, "schemaName", usernameNormalized) > 0) {
			return "USERNAME_TAKEN_TENANT";
		}
		SchemaNameReservation reservasi = cariBaris(session, usernameNormalized);
		if (reservasi != null && menghalangi(reservasi, new Date())) {
			return "USERNAME_TAKEN_RESERVATION";
		}
		if (hitung(session, Tbmuser.class, "userId", usernameNormalized) > 0) {
			return "USERNAME_TAKEN_USER";
		}
		Number diPedagang = (Number) session
				.createSQLQuery("SELECT COUNT(*) FROM koperasi.pedagang WHERE userid = :u")
				.setParameter("u", usernameNormalized).uniqueResult();
		if (diPedagang != null && diPedagang.longValue() > 0) {
			return "USERNAME_TAKEN_USER";
		}
		Number diNamespace = (Number) session
				.createSQLQuery("SELECT COUNT(*) FROM pg_namespace WHERE nspname = :n OR nspname = :na")
				.setParameter("n", usernameNormalized)
				.setParameter("na", usernameNormalized + "__audit").uniqueResult();
		if (diNamespace != null && diNamespace.longValue() > 0) {
			return "USERNAME_TAKEN_SCHEMA";
		}
		return null;
	}

	/**
	 * Helper hitung jumlah baris pada {@code kelas} yang kolom {@code properti}-nya sama persis
	 * ({@code equals}) dengan {@code nilai}. Dipakai berulang oleh {@link #alasanTidakTersedia}
	 * untuk memeriksa benturan username terhadap beberapa sumber sekaligus (mis.
	 * {@code Pendaftar.domain}, {@code TenantRegistry.slug}, {@code TenantRegistry.schemaName},
	 * {@code Tbmuser.userId}) tanpa menulis ulang criteria+projection rowCount di tiap tempat.
	 *
	 * @param session  sesi Hibernate milik pemanggil (dipakai apa adanya, tidak dibuka/ditutup di sini)
	 * @param kelas    kelas entitas yang diperiksa
	 * @param properti nama properti (kolom) yang dibandingkan
	 * @param nilai    nilai yang dicari (perbandingan exact match)
	 * @return jumlah baris yang cocok; 0 bila tidak ada baris cocok atau hasil query {@code null}
	 */
	private static long hitung(Session session, Class<?> kelas, String properti, String nilai) {
		Number n = (Number) session.createCriteria(kelas).add(Restrictions.eq(properti, nilai))
				.setProjection(Projections.rowCount()).uniqueResult();
		return n == null ? 0 : n.longValue();
	}

	/**
	 * Satu-satunya baris {@link SchemaNameReservation} milik {@code normalizedName}, atau
	 * {@code null} bila belum pernah direservasi. Unique {@code normalized_name} menjamin hasil
	 * ini tidak pernah lebih dari satu baris (lihat Javadoc entity & kelas ini).
	 */
	private static SchemaNameReservation cariBaris(Session session, String normalizedName) {
		return (SchemaNameReservation) session.createCriteria(SchemaNameReservation.class)
				.add(Restrictions.eq("normalizedName", normalizedName)).setMaxResults(1).uniqueResult();
	}

	/**
	 * true bila baris {@code r} masih mengunci namanya pada waktu {@code sekarang} -- dipakai baik
	 * oleh {@link #alasanTidakTersedia} maupun {@link #reservasi} (harus selalu sepakat, lihat
	 * Javadoc kelas).
	 *
	 * <ul>
	 * <li>CONSUMED selalu menghalangi (nama sudah benar-benar dipakai sebagai slug/schema jadi).</li>
	 * <li>RELEASED/EXPIRED tidak pernah menghalangi.</li>
	 * <li>RESERVED menghalangi kecuali kedaluwarsa ({@code expiresAt} tidak null dan sudah lewat
	 * {@code sekarang}) DAN permohonan pemiliknya ADA serta masih persis
	 * {@code STATUS_EMAIL_VERIFICATION_PENDING}/{@code STATUS_SUBMITTED} (pola sama dgn
	 * {@code PendaftaranTenantService.verifikasiEmail}/{@code resendVerifikasi}/{@code cancel}).
	 * Reservasi tanpa permohonan pemilik (dibuat administratif) TIDAK PERNAH dianggap basi otomatis
	 * di sini.</li>
	 * </ul>
	 */
	private static boolean menghalangi(SchemaNameReservation r, Date sekarang) {
		String status = r.getStatus();
		if (SchemaNameReservation.STATUS_CONSUMED.equals(status)) {
			return true;
		}
		if (!SchemaNameReservation.STATUS_RESERVED.equals(status)) {
			return false;
		}
		if (r.getExpiresAt() == null || !r.getExpiresAt().before(sekarang)) {
			return true;
		}
		PendaftaranTenant permohonan = r.getPendaftaranTenant();
		if (permohonan == null) {
			return true;
		}
		String statusPermohonan = permohonan.getStatus();
		return !PendaftaranTenant.STATUS_EMAIL_VERIFICATION_PENDING.equals(statusPermohonan)
				&& !PendaftaranTenant.STATUS_SUBMITTED.equals(statusPermohonan);
	}

	/**
	 * Buat/reuse reservasi DI DALAM transaction submit yang sedang berjalan (session milik
	 * pemanggil, TIDAK commit di sini). Bila baris lama utk nama ini sudah tidak menghalangi lagi
	 * ({@link #menghalangi} false -- RELEASED/EXPIRED), baris yang SAMA di-UPDATE (seluruh field
	 * direset) alih-alih INSERT baru: unique {@code normalized_name} berlaku utk SELURUH baris
	 * termasuk yang sudah dilepas/kedaluwarsa, jadi INSERT kedua akan selalu bentrok. Bila baris
	 * lama masih menghalangi (kalah race terhadap {@link #alasanTidakTersedia} milik pemanggil),
	 * method ini melempar {@link IllegalStateException} berpesan yang memuat
	 * {@code schema_name_reservation} supaya diterjemahkan sama seperti unique-violation biasa
	 * oleh {@code PendaftaranTenantService.terjemahkanException}.
	 *
	 * @return token mentah kepemilikan reservasi (hash-nya yang disimpan) -- saat ini dipakai
	 *         internal service; disimpan supaya release/consume bisa diverifikasi.
	 */
	public static String reservasi(Session session, String usernameNormalized, PendaftaranTenant permohonan,
			int masaBerlakuJam) {
		Date sekarang = new Date();
		SchemaNameReservation r = cariBaris(session, usernameNormalized);
		if (r != null && menghalangi(r, sekarang)) {
			throw new IllegalStateException("schema_name_reservation: normalized_name='" + usernameNormalized
					+ "' masih menghalangi (status " + r.getStatus() + ").");
		}
		if (r == null) {
			r = new SchemaNameReservation();
			r.setNormalizedName(usernameNormalized);
		}
		String tokenMentah = PasswordHashService.tokenAcakHex(32);
		r.setPendaftaranTenant(permohonan);
		r.setStatus(SchemaNameReservation.STATUS_RESERVED);
		r.setReservedAt(sekarang);
		r.setExpiresAt(new Date(sekarang.getTime() + masaBerlakuJam * 3600L * 1000L));
		r.setConsumedAt(null);
		r.setReleasedAt(null);
		r.setReservationTokenHash(PasswordHashService.sha256Hex(tokenMentah));
		r.setOleh("pendaftaran");
		r.setOlehId("pendaftaran");
		session.saveOrUpdate(r);
		return tokenMentah;
	}

	/**
	 * Baris RESERVED yang {@code expiresAt}-nya sudah lewat {@code sekarang} DAN permohonan
	 * pemiliknya masih persis {@code STATUS_EMAIL_VERIFICATION_PENDING}/{@code STATUS_SUBMITTED} --
	 * satu-satunya kombinasi yang berarti "ditinggalkan sebelum verifikasi" (lihat
	 * {@link #menghalangi}). Dipakai {@code ReservationExpiryScheduler}; inner join ke
	 * {@code pendaftaranTenant} otomatis mengecualikan reservasi administratif tanpa permohonan
	 * (tidak pernah disapu di sini) dan permohonan yang sudah melangkah lebih jauh (masih
	 * menghalangi terlepas dari {@code expiresAt}).
	 *
	 * @param maksimal batas jumlah baris (jaga tick penyapu tetap pendek)
	 */
	public static List<SchemaNameReservation> cariKandidatKedaluwarsa(Session session, Date sekarang, int maksimal) {
		List<?> baris = session.createCriteria(SchemaNameReservation.class)
				.add(Restrictions.eq("status", SchemaNameReservation.STATUS_RESERVED))
				.add(Restrictions.isNotNull("expiresAt"))
				.add(Restrictions.lt("expiresAt", sekarang))
				.createAlias("pendaftaranTenant", "p")
				.add(Restrictions.or(Restrictions.eq("p.status", PendaftaranTenant.STATUS_EMAIL_VERIFICATION_PENDING),
						Restrictions.eq("p.status", PendaftaranTenant.STATUS_SUBMITTED)))
				.addOrder(Order.asc("id"))
				.setMaxResults(maksimal)
				.list();
		List<SchemaNameReservation> hasil = new java.util.ArrayList<SchemaNameReservation>(baris.size());
		for (Object o : baris) {
			hasil.add((SchemaNameReservation) o);
		}
		return hasil;
	}

	/**
	 * Tandai satu reservasi basi {@link SchemaNameReservation#STATUS_EXPIRED} sekaligus permohonan
	 * pemiliknya {@code PendaftaranTenant.STATUS_EXPIRED} (pola sama dgn
	 * {@code PendaftaranTenantService.cancel}, hanya dipicu waktu bukan aksi pendaftar). Dipanggil
	 * DI DALAM transaksi kecil milik pemanggil ({@code ReservationExpiryScheduler}) -- tidak
	 * commit/rollback di sini. Menulis {@link PendaftaranAuditEvent#EV_REGISTRATION_EXPIRED}.
	 *
	 * <p>
	 * Pemanggil WAJIB sudah memverifikasi ulang {@code r.getStatus()}/{@code r.getExpiresAt()} pada
	 * saat transaksi ini dibuka (baris bisa saja berubah antara query kandidat dan klaim, mis.
	 * pendaftar mengklik tautan verifikasi tepat di antara keduanya) -- method ini sendiri tidak
	 * mengulang pemeriksaan itu, murni eksekusi transisi.
	 * </p>
	 */
	public static void kedaluwarsakan(Session session, SchemaNameReservation r, Date sekarang) {
		r.setStatus(SchemaNameReservation.STATUS_EXPIRED);
		session.saveOrUpdate(r);
		PendaftaranTenant permohonan = r.getPendaftaranTenant();
		if (permohonan == null) {
			return;
		}
		String statusPermohonan = permohonan.getStatus();
		if (!PendaftaranTenant.STATUS_EMAIL_VERIFICATION_PENDING.equals(statusPermohonan)
				&& !PendaftaranTenant.STATUS_SUBMITTED.equals(statusPermohonan)) {
			return;
		}
		permohonan.setStatus(PendaftaranTenant.STATUS_EXPIRED);
		permohonan.setCurrentStage("EXPIRED");
		session.saveOrUpdate(permohonan);
		try {
			PendaftaranAuditEvent ev = new PendaftaranAuditEvent();
			ev.setEventCode(PendaftaranAuditEvent.EV_REGISTRATION_EXPIRED);
			ev.setActorType(PendaftaranAuditEvent.ACTOR_SYSTEM);
			ev.setPendaftarId(permohonan.getPendaftar().getId());
			ev.setRegistrationId(permohonan.getId());
			ev.setResult("OK");
			ev.setWaktu(sekarang);
			ev.setOleh("reservation-expiry-sweep");
			ev.setOlehId("reservation-expiry-sweep");
			session.save(ev);
		} catch (Exception e) {
			// Audit tidak boleh menggagalkan alur; exception dicatat ke error_log saja.
			ais.common.ErrorAuditUtil.record(e, "auto-audit UsernameReservationService.kedaluwarsakan.audit");
		}
	}
}
