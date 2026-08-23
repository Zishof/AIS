package ais.service.tenant;

import org.hibernate.Session;

/**
 * <h3>Penulis audit tenant (P4) — pola tulis-tangan gaya Envers.</h3>
 *
 * <p>Envers bawaan tidak dapat dipakai untuk ini: {@code org.hibernate.envers.default_schema}
 * bersifat <b>statis per SessionFactory</b>, sehingga baris audit seluruh tenant akan
 * berkumpul di satu schema {@code new_audit}. Kelas ini menulis ke
 * {@code <schema-tenant>__audit} dari {@link TenantContext}, sehingga isolasi datanya utuh
 * sampai ke jejak auditnya.</p>
 *
 * <h4>Satu revisi, banyak baris</h4>
 * <p>Satu aksi pengguna menghasilkan <b>satu</b> baris {@code revinfo} yang membawa konteksnya
 * (siapa, peran apa, dari perangkat mana, permintaan yang mana, alasannya apa), lalu
 * <b>beberapa</b> baris {@code audit_baris} — satu per baris data yang tersentuh. Itulah
 * sebabnya konteks tidak diulang pada tiap baris: menyimpan satu faktur berisi lima puluh
 * item tetap satu revisi, bukan lima puluh salinan konteks yang sama.</p>
 *
 * <h4>Berjalan pada transaksi pemanggil</h4>
 * <p>Tidak pernah membuka Session sendiri. Baris audit <b>wajib</b> berada di transaksi yang
 * sama dengan perubahan datanya — audit yang commit terpisah dapat bertahan padahal
 * perubahannya dibatalkan, atau hilang padahal perubahannya jadi.</p>
 *
 * <h4>Jangan menyimpan rahasia</h4>
 * <p>§11.6 melarang audit memuat kata sandi, token, atau secret. Muatan {@code sebelum} dan
 * {@code sesudah} disusun pemanggil, jadi tanggung jawabnya ada di sana — kelas ini tidak
 * dapat menebak medan mana yang rahasia.</p>
 */
public final class TenantAuditWriter {

	/** Baris baru. */
	public static final int REVTYPE_ADD = 0;
	/** Baris diubah. */
	public static final int REVTYPE_MOD = 1;
	/** Baris dihapus/dinonaktifkan. */
	public static final int REVTYPE_DEL = 2;

	private TenantAuditWriter() {
	}

	/**
	 * Konteks satu permintaan. Dipisah dari {@link TenantContext} karena isinya berumur satu
	 * aksi, bukan satu sesi.
	 */
	public static final class Jejak {
		public String actorType;
		public String deviceId;
		public String requestId;
		public String correlationId;
		public String idempotencyKey;
		public String action;
		public String reason;

		public Jejak() {
		}

		public Jejak(String action) {
			this.action = action;
		}
	}

	/**
	 * Terbitkan satu revisi beserta konteksnya, kembalikan nomor {@code rev}.
	 *
	 * @throws TenantAccessException bila tenant berjalan tanpa schema (mode LEGACY) -- audit
	 *         per-tenant tidak berlaku di sana, dan pemanggil harus memeriksanya lebih dulu
	 *         lewat {@link TenantContext#pakaiSchemaTenant()}.
	 */
	public static long mulaiRevisi(Session session, TenantContext ctx, Jejak jejak) {
		String audit = schemaAudit(ctx);
		Jejak j = jejak == null ? new Jejak() : jejak;
		Number rev = (Number) session.createSQLQuery("INSERT INTO \"" + audit + "\".revinfo ("
				+ "revtstmp, tenant_id, tenant_code, membership_id, user_id, role, actor_type,"
				+ " device_id, request_id, correlation_id, idempotency_key, action, reason, waktu)"
				+ " VALUES (:ts, :tid, :tcode, :mid, :uid, :role, :atype, :dev, :req, :corr,"
				+ " :idem, :act, :reason, now()) RETURNING rev")
				.setParameter("ts", Long.valueOf(System.currentTimeMillis()))
				.setParameter("tid", ctx.getTenantId())
				.setParameter("tcode", nol(ctx.getTenantCode()))
				.setParameter("mid", ctx.getMembershipId())
				.setParameter("uid", nol(ctx.getActiveTbmuserId()))
				.setParameter("role", nol(ctx.getMembershipRole()))
				.setParameter("atype", nol(j.actorType))
				.setParameter("dev", nol(j.deviceId))
				.setParameter("req", nol(j.requestId))
				.setParameter("corr", nol(j.correlationId))
				.setParameter("idem", nol(j.idempotencyKey))
				.setParameter("act", nol(j.action))
				.setParameter("reason", nol(j.reason))
				.uniqueResult();
		if (rev == null) {
			throw new IllegalStateException("revinfo tidak mengembalikan rev.");
		}
		return rev.longValue();
	}

	/**
	 * Catat satu baris data yang tersentuh pada revisi tersebut.
	 *
	 * @param entity   nama tabel tanpa schema, mis. {@code "supplier"}.
	 * @param entityId id barisnya; disimpan sebagai teks supaya kunci non-numerik pun tertampung.
	 * @param sebelum  keadaan lama, atau {@code null} untuk penambahan.
	 * @param sesudah  keadaan baru, atau {@code null} untuk penghapusan.
	 */
	public static void catat(Session session, TenantContext ctx, long rev, String entity,
			Object entityId, int revtype, String sebelum, String sesudah) {
		if (entity == null || entity.trim().length() == 0) {
			throw new IllegalArgumentException("entity audit kosong.");
		}
		String audit = schemaAudit(ctx);
		session.createSQLQuery("INSERT INTO \"" + audit + "\".audit_baris ("
				+ "rev, revtype, entity, entity_id, sebelum, sesudah, waktu)"
				+ " VALUES (:rev, :rt, :ent, :eid, :seb, :ses, now())")
				.setParameter("rev", Long.valueOf(rev))
				.setParameter("rt", Integer.valueOf(revtype))
				.setParameter("ent", entity.trim())
				.setParameter("eid", entityId == null ? "" : String.valueOf(entityId))
				.setParameter("seb", sebelum)
				.setParameter("ses", sesudah)
				.executeUpdate();
	}

	/**
	 * Jalan pintas untuk aksi yang hanya menyentuh satu baris: terbitkan revisi lalu catat
	 * barisnya sekaligus.
	 */
	public static long catatTunggal(Session session, TenantContext ctx, Jejak jejak, String entity,
			Object entityId, int revtype, String sebelum, String sesudah) {
		long rev = mulaiRevisi(session, ctx, jejak);
		catat(session, ctx, rev, entity, entityId, revtype, sebelum, sesudah);
		return rev;
	}

	private static String schemaAudit(TenantContext ctx) {
		if (ctx == null || !ctx.pakaiSchemaTenant()) {
			throw new TenantAccessException(TenantAccessException.TENANT_SCHEMA_INVALID,
					"Audit per-tenant tidak berlaku pada mode ini.");
		}
		String a = ctx.getAuditSchemaName();
		if (a == null || a.trim().length() == 0) {
			throw new TenantAccessException(TenantAccessException.TENANT_SCHEMA_INVALID,
					"Schema audit tenant tidak tersedia.");
		}
		try {
			// Divalidasi ulang di sini juga: nama schema tidak boleh pernah masuk SQL tanpa
			// lolos pola, sekalipun sudah divalidasi saat konteks dibentuk. Memakai validator
			// AUDIT -- pastikanAman biasa menolak nama turunan yang panjang. Lihat
			// TenantSchemaLocator.pastikanAmanAudit.
			return TenantSchemaLocator.pastikanAmanAudit(a.trim());
		} catch (IllegalArgumentException e) {
			throw new TenantAccessException(TenantAccessException.TENANT_SCHEMA_INVALID,
					"Konfigurasi schema audit tidak sah.", e);
		}
	}

	private static String nol(String v) {
		return v == null || v.trim().length() == 0 ? null : v.trim();
	}
}
