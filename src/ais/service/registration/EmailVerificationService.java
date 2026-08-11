package ais.service.registration;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.common.security.PasswordHashService;
import ais.database.model.tenant.PendaftaranAuditEvent;
import ais.database.model.tenant.PendaftaranEmailVerification;
import ais.database.model.tenant.PendaftaranTenant;

/**
 * <h3>Tantangan verifikasi email permohonan tenant (token hash, expiry, resend, supersede).</h3>
 *
 * <p>Token 32-byte SecureRandom dikirim sebagai tautan {@code /pendaftaran?mode=verifikasi&token=...};
 * database hanya menyimpan SHA-256 hex-nya. Token baru men-supersede token PENDING lama.
 * Pengiriman lewat jalur produksi {@code ais.delivery.email.sender.MailSender} (HTML, SMTP dari
 * Konfigurasi, gate {@code aktfikan_pengiriman_email}) -- BUKAN {@code MailClient} demo. Gagal
 * kirim TIDAK membatalkan permohonan: token tetap tersimpan, bisa resend / verifikasi manual
 * admin (lihat 05-workflow.md aturan #3).</p>
 */
public final class EmailVerificationService {

	/** Masa berlaku token (jam) -- configurable {@code pendaftaran_verifikasi_jam}, default 48. */
	public static int masaBerlakuJam() {
		try {
			return Integer.parseInt(Common.getKonfigurasi("pendaftaran_verifikasi_jam", "48").getNilai().trim());
		} catch (Exception e) {
			return 48;
		}
	}

	private EmailVerificationService() {
	}

	/**
	 * Buat tantangan baru DI DALAM transaction pemanggil (supersede PENDING lama).
	 * @return token MENTAH (untuk tautan email) -- tidak pernah disimpan/di-log.
	 */
	public static String buatChallenge(Session session, PendaftaranTenant permohonan, String emailTujuanNormalized) {
		List<?> lama = session.createCriteria(PendaftaranEmailVerification.class)
				.add(Restrictions.eq("pendaftaranTenant.id", permohonan.getId()))
				.add(Restrictions.eq("status", PendaftaranEmailVerification.STATUS_PENDING)).list();
		for (Object o : lama) {
			PendaftaranEmailVerification v = (PendaftaranEmailVerification) o;
			v.setStatus(PendaftaranEmailVerification.STATUS_SUPERSEDED);
			session.saveOrUpdate(v);
		}

		String tokenMentah = PasswordHashService.tokenAcakHex(32);
		PendaftaranEmailVerification v = new PendaftaranEmailVerification();
		v.setPendaftaranTenant(permohonan);
		v.setChannel(PendaftaranEmailVerification.CHANNEL_EMAIL);
		v.setDestinationNormalized(emailTujuanNormalized);
		v.setTokenHash(PasswordHashService.sha256Hex(tokenMentah));
		v.setStatus(PendaftaranEmailVerification.STATUS_PENDING);
		v.setAttemptCount(Integer.valueOf(0));
		Date sekarang = new Date();
		v.setCreatedAt(sekarang);
		v.setExpiresAt(new Date(sekarang.getTime() + masaBerlakuJam() * 3600L * 1000L));
		v.setOleh("pendaftaran");
		v.setOlehId("pendaftaran");
		session.save(v);
		return tokenMentah;
	}

	/**
	 * Kirim email verifikasi (best-effort, DI LUAR transaction submit -- gagal kirim tidak
	 * menggagalkan pendaftaran). Menandai sentAt + audit event dalam transaksi kecil sendiri.
	 */
	public static void kirimEmail(Long permohonanId, String emailTujuan, String namaUsaha, String registrationCode,
			String tokenMentah, String baseUrl) {
		String tautan = baseUrl + "/pendaftaran?mode=verifikasi&token=" + tokenMentah;
		boolean terkirim = false;
		try {
			String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
			String subject = "Verifikasi Email Pendaftaran Tenant - " + registrationCode;
			String body = "Halo,<br><br>Terima kasih telah mendaftarkan <b>"
					+ org.apache.commons.lang.StringEscapeUtils.escapeHtml(namaUsaha == null ? "" : namaUsaha)
					+ "</b> (nomor pendaftaran <b>" + registrationCode + "</b>).<br><br>"
					+ "Klik tautan berikut untuk memverifikasi alamat email Anda (berlaku "
					+ masaBerlakuJam() + " jam):<br><a href=\"" + tautan + "\">" + tautan + "</a><br><br>"
					+ "Bila Anda tidak merasa mendaftar, abaikan email ini.<br><br>Salam,<br>Tim eBisnis";
			ais.delivery.email.sender.MailSender.sendMail(new org.json.JSONArray(), subject, body, sender,
					emailTujuan, null);
			terkirim = true;
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit EmailVerificationService.kirimEmail:" + registrationCode);
		}

		Session session = ais.database.hibernate.HibernateUtil.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			PendaftaranEmailVerification terbaru = (PendaftaranEmailVerification) session
					.createCriteria(PendaftaranEmailVerification.class)
					.add(Restrictions.eq("pendaftaranTenant.id", permohonanId))
					.add(Restrictions.eq("status", PendaftaranEmailVerification.STATUS_PENDING))
					.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
			if (terkirim && terbaru != null) {
				terbaru.setSentAt(new Date());
				session.saveOrUpdate(terbaru);
			}
			PendaftaranAuditEvent ev = new PendaftaranAuditEvent();
			ev.setEventCode(PendaftaranAuditEvent.EV_EMAIL_VERIFICATION_SENT);
			ev.setActorType(PendaftaranAuditEvent.ACTOR_SYSTEM);
			ev.setRegistrationId(permohonanId);
			ev.setResult(terkirim ? "SENT" : "SEND_FAILED");
			ev.setWaktu(new Date());
			ev.setOleh("pendaftaran");
			ev.setOlehId("pendaftaran");
			session.save(ev);
			session.getTransaction().commit();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit EmailVerificationService.kirimEmail.audit");
		} finally {
			ais.database.hibernate.HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Cari tantangan PENDING yang cocok dgn token mentah (dibandingkan via hash) DI DALAM
	 * session pemanggil. @return baris cocok yang belum kedaluwarsa, atau null.
	 */
	public static PendaftaranEmailVerification cariByToken(Session session, String tokenMentah) {
		if (tokenMentah == null || tokenMentah.trim().isEmpty() || tokenMentah.length() > 200) {
			return null;
		}
		PendaftaranEmailVerification v = (PendaftaranEmailVerification) session
				.createCriteria(PendaftaranEmailVerification.class)
				.add(Restrictions.eq("tokenHash", PasswordHashService.sha256Hex(tokenMentah.trim())))
				.add(Restrictions.eq("status", PendaftaranEmailVerification.STATUS_PENDING))
				.setMaxResults(1).uniqueResult();
		if (v == null) {
			return null;
		}
		if (v.getExpiresAt() != null && v.getExpiresAt().before(new Date())) {
			v.setStatus(PendaftaranEmailVerification.STATUS_EXPIRED);
			session.saveOrUpdate(v);
			return null;
		}
		return v;
	}
}
