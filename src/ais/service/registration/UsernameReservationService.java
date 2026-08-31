package ais.service.registration;

import java.util.Date;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.security.PasswordHashService;
import ais.database.model.Pendaftar;
import ais.database.model.Tbmuser;
import ais.database.model.tenant.PendaftaranTenant;
import ais.database.model.tenant.SchemaNameReservation;
import ais.database.model.tenant.TenantRegistry;

/**
 * <h3>Cek benturan + reservasi atomik username/schema tenant (§3.4 dokumen master).</h3>
 *
 * <p>{@link #tersedia} = pemeriksaan informatif (dipakai {@code check_username} & pra-validasi
 * submit); KEBENARAN FINAL tetap INSERT {@link SchemaNameReservation} (unique
 * {@code normalized_name}) di dalam transaction submit -- race dua submit bersamaan
 * diserialisasi constraint DB, BUKAN oleh cek ini.</p>
 *
 * <p>Sumber benturan yang diperiksa: reserved list (bawaan+konfigurasi), {@code pendaftar.domain},
 * {@code tenant_registry.slug/schema_name}, reservation aktif, {@code tbmuser.userid},
 * {@code koperasi.pedagang.userid} (COUNT>0 -- TIDAK unik dalam praktik, lihat audit §8),
 * dan {@code pg_namespace}.</p>
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
		Number reservasiAktif = (Number) session.createCriteria(SchemaNameReservation.class)
				.add(Restrictions.eq("normalizedName", usernameNormalized))
				.add(Restrictions.in("status",
						new String[] { SchemaNameReservation.STATUS_RESERVED, SchemaNameReservation.STATUS_CONSUMED }))
				.setProjection(Projections.rowCount()).uniqueResult();
		if (reservasiAktif != null && reservasiAktif.longValue() > 0) {
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
	 * Buat reservasi DI DALAM transaction submit yang sedang berjalan (session milik pemanggil,
	 * TIDAK commit di sini). Unique-violation saat flush/commit = kalah race → pemanggil
	 * menerjemahkan jadi USERNAME_NOT_AVAILABLE.
	 *
	 * @return token mentah kepemilikan reservasi (hash-nya yang disimpan) -- saat ini dipakai
	 *         internal service; disimpan supaya release/consume bisa diverifikasi.
	 */
	public static String reservasi(Session session, String usernameNormalized, PendaftaranTenant permohonan,
			int masaBerlakuJam) {
		String tokenMentah = PasswordHashService.tokenAcakHex(32);
		SchemaNameReservation r = new SchemaNameReservation();
		r.setNormalizedName(usernameNormalized);
		r.setPendaftaranTenant(permohonan);
		r.setStatus(SchemaNameReservation.STATUS_RESERVED);
		Date sekarang = new Date();
		r.setReservedAt(sekarang);
		r.setExpiresAt(new Date(sekarang.getTime() + masaBerlakuJam * 3600L * 1000L));
		r.setReservationTokenHash(PasswordHashService.sha256Hex(tokenMentah));
		r.setOleh("pendaftaran");
		r.setOlehId("pendaftaran");
		session.save(r);
		return tokenMentah;
	}
}
