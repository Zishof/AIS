package ais.service.tenant;

import org.hibernate.Session;
import org.hibernate.Transaction;

import ais.database.hibernate.HibernateUtil;

/**
 * <h3>Satu Session dan satu transaksi untuk satu request tenant (P1 &sect;9.4).</h3>
 *
 * <p>Urutannya tetap: buka Session &rarr; mulai transaksi &rarr; bentuk {@link TenantContext}
 * &rarr; jalankan pekerjaan &rarr; commit. Galat apa pun membatalkan transaksi, dan Session
 * selalu ditutup di {@code finally}.</p>
 *
 * <p><b>Konteks dibentuk di dalam transaksi yang sama</b> dengan pekerjaannya. Bila
 * pembentukan konteks memakai Session tersendiri, keanggotaan dapat dicabut tepat di antara
 * pemeriksaan dan pemakaian -- pekerjaan tetap berjalan atas kewenangan yang sudah tidak ada.</p>
 *
 * <p>Java 7: pekerjaan diserahkan lewat antarmuka {@link Tugas}, bukan lambda.</p>
 */
public final class TenantRequestExecutor {

	private TenantRequestExecutor() {
	}

	/** Pekerjaan yang berjalan di dalam satu transaksi tenant. */
	public interface Tugas {
		/**
		 * @param session Session milik executor -- <b>jangan</b> ditutup di sini.
		 * @param ctx     konteks tenant yang sudah tervalidasi.
		 */
		Object jalankan(Session session, TenantContext ctx) throws Exception;
	}

	/**
	 * Jalankan {@code tugas} pada konteks tenant yang dibentuk dari parameter aktor.
	 *
	 * @throws TenantAccessException bila tenant/aktor tidak sah -- transaksi sudah dibatalkan.
	 * @throws Exception             apa pun yang dilempar {@code tugas}.
	 */
	public static Object jalankan(Long tenantId, String tbmuserId, Long pendaftarId, Tugas tugas)
			throws Exception {
		if (tugas == null) {
			throw new IllegalArgumentException("Tugas kosong.");
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			TenantContext ctx = TenantContextResolver.resolve(session, tenantId, tbmuserId, pendaftarId);
			// Satu kueri ke pg_namespace; memastikan schema benar-benar ada sebelum kueri
			// pertama menabraknya dengan galat SQL mentah yang membocorkan namanya.
			TenantSchemaLocator.pastikanSiap(session, muatRegistry(session, ctx));
			Object hasil = tugas.jalankan(session, ctx);
			tx.commit();
			tx = null;
			return hasil;
		} finally {
			batalkanBilaPerlu(tx);
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Varian tanpa nilai kembali, untuk pemanggil yang hanya menulis. */
	public static void jalankanTanpaHasil(Long tenantId, String tbmuserId, Long pendaftarId,
			Tugas tugas) throws Exception {
		jalankan(tenantId, tbmuserId, pendaftarId, tugas);
	}

	/** Muat baris {@link ais.database.model.tenant.TenantRegistry} milik tenant pada {@code ctx}, untuk dicek {@link TenantSchemaLocator#pastikanSiap}. */
	private static ais.database.model.tenant.TenantRegistry muatRegistry(Session session,
			TenantContext ctx) {
		return (ais.database.model.tenant.TenantRegistry) session
				.get(ais.database.model.tenant.TenantRegistry.class, ctx.getTenantId());
	}

	/**
	 * Batalkan transaksi yang belum selesai. Kegagalan rollback sengaja ditelan: ia hanya terjadi
	 * ketika koneksinya memang sudah putus, dan melemparnya dari {@code finally} akan menutupi
	 * galat asli yang sedang naik.
	 */
	private static void batalkanBilaPerlu(Transaction tx) {
		if (tx == null) {
			return;
		}
		try {
			tx.rollback();
		} catch (RuntimeException abaikan) {
			// koneksi sudah putus; galat aslinya lebih penting untuk sampai ke pemanggil
		}
	}
}
