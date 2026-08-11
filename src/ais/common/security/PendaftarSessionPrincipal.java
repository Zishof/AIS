package ais.common.security;

import java.io.Serializable;
import java.util.Date;

import ais.database.model.Pendaftar;

/**
 * <h3>Principal sesi RINGAN pendaftar ebisnis.id (§12.1 dokumen master).</h3>
 *
 * <p>Jalur BARU tidak boleh bergantung pada entity Hibernate detached yang basi sebagai
 * satu-satunya kebenaran -- principal ini hanya membawa identitas minimum; SETIAP request
 * sensitif WAJIB re-fetch status akun/tenant/membership/entitlement dari DB berdasarkan
 * {@link #pendaftarId}. Atribut sesi lama {@code SESSION_PENDAFTAR} (entity penuh) tetap
 * diisi utk kompatibilitas dashboard existing.</p>
 */
public class PendaftarSessionPrincipal implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String SESSION_KEY = "pendaftarPrincipal";

	public final Long pendaftarId;
	public final String email;
	public final String accountStatus;
	public final Date issuedAt;
	public final String csrfNonce;
	/** Tenant aktif terpilih (tenant switcher) -- boleh berubah, maka bukan final. */
	public Long activeTenantId;

	public PendaftarSessionPrincipal(Long pendaftarId, String email, String accountStatus) {
		this.pendaftarId = pendaftarId;
		this.email = email;
		this.accountStatus = accountStatus;
		this.issuedAt = new Date();
		this.csrfNonce = PasswordHashService.tokenAcakHex(16);
	}

	public static PendaftarSessionPrincipal dari(Pendaftar pendaftar) {
		return new PendaftarSessionPrincipal(pendaftar.getId(), pendaftar.getEmail(), "ACTIVE");
	}

	/** Ambil principal dari sesi; null bila belum login jalur pendaftar. */
	public static PendaftarSessionPrincipal dariSesi(javax.servlet.http.HttpSession session) {
		if (session == null) {
			return null;
		}
		Object o = session.getAttribute(SESSION_KEY);
		return o instanceof PendaftarSessionPrincipal ? (PendaftarSessionPrincipal) o : null;
	}
}
