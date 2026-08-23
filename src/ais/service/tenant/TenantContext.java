package ais.service.tenant;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * <h3>Konteks tenant untuk satu request (P1).</h3>
 *
 * <p>Objek nilai <b>immutable</b>: dibentuk sekali oleh {@link TenantContextResolver} di awal
 * request, lalu dibaca saja oleh seluruh lapisan di bawahnya. Tidak ada penyetel; tidak
 * disimpan pada variabel statis maupun {@code ThreadLocal} supaya tidak bocor antar request
 * pada kontainer yang memakai ulang thread.</p>
 *
 * <p><b>{@code schemaName} dan {@code auditSchemaName} tidak boleh sampai ke klien.</b>
 * {@link #toJsonKlien()} sengaja menghilangkan keduanya. Membocorkannya memberi tahu penyerang
 * persis nama schema yang harus dituju bila kelak ada celah injeksi.</p>
 */
public final class TenantContext {

	private final Long tenantId;
	private final String tenantCode;
	private final String tenantName;
	private final String tenantStatus;
	private final String tenantMode;
	private final Long membershipId;
	private final String membershipRole;
	private final Long ownerPendaftarId;
	private final Long activePendaftarId;
	private final String activeTbmuserId;
	private final String schemaName;
	private final String auditSchemaName;
	private final String schemaVersion;
	private final String timezone;
	private final String locale;
	private final Set<String> moduleEntitlements;

	private TenantContext(Builder b) {
		this.tenantId = b.tenantId;
		this.tenantCode = b.tenantCode;
		this.tenantName = b.tenantName;
		this.tenantStatus = b.tenantStatus;
		this.tenantMode = b.tenantMode;
		this.membershipId = b.membershipId;
		this.membershipRole = b.membershipRole;
		this.ownerPendaftarId = b.ownerPendaftarId;
		this.activePendaftarId = b.activePendaftarId;
		this.activeTbmuserId = b.activeTbmuserId;
		this.schemaName = b.schemaName;
		this.auditSchemaName = b.auditSchemaName;
		this.schemaVersion = b.schemaVersion;
		this.timezone = b.timezone;
		this.locale = b.locale;
		Set<String> salinan = new HashSet<String>();
		if (b.moduleEntitlements != null) {
			salinan.addAll(b.moduleEntitlements);
		}
		this.moduleEntitlements = Collections.unmodifiableSet(salinan);
	}

	public Long getTenantId() { return tenantId; }
	public String getTenantCode() { return tenantCode; }
	public String getTenantName() { return tenantName; }
	public String getTenantStatus() { return tenantStatus; }
	public String getTenantMode() { return tenantMode; }
	public Long getMembershipId() { return membershipId; }
	public String getMembershipRole() { return membershipRole; }
	public Long getOwnerPendaftarId() { return ownerPendaftarId; }
	public Long getActivePendaftarId() { return activePendaftarId; }
	public String getActiveTbmuserId() { return activeTbmuserId; }
	public String getSchemaName() { return schemaName; }
	public String getAuditSchemaName() { return auditSchemaName; }
	public String getSchemaVersion() { return schemaVersion; }
	public String getTimezone() { return timezone; }
	public String getLocale() { return locale; }
	public Set<String> getModuleEntitlements() { return moduleEntitlements; }

	/** Benar bila modul tersebut aktif bagi tenant ini. Perbandingan tidak peka huruf besar-kecil. */
	public boolean punyaModul(String moduleCode) {
		if (moduleCode == null) {
			return false;
		}
		return moduleEntitlements.contains(moduleCode.trim().toUpperCase());
	}

	/**
	 * Benar bila request ini harus dilayani dari schema tenant. Dipakai penulis kueri untuk
	 * memilih antara jalur schema tenant dan jalur shared lama.
	 */
	public boolean pakaiSchemaTenant() {
		return schemaName != null && schemaName.length() > 0;
	}

	/**
	 * Bentuk JSON yang <b>boleh</b> dikirim ke klien. Tanpa {@code schemaName},
	 * {@code auditSchemaName}, dan {@code schemaVersion} -- ketiganya urusan server.
	 */
	public JSONObject toJsonKlien() throws JSONException {
		JSONObject o = new JSONObject();
		o.put("tenant_id", tenantId == null ? JSONObject.NULL : tenantId);
		o.put("tenant_code", tenantCode == null ? "" : tenantCode);
		o.put("tenant_name", tenantName == null ? "" : tenantName);
		o.put("tenant_status", tenantStatus == null ? "" : tenantStatus);
		o.put("tenant_mode", tenantMode == null ? "" : tenantMode);
		o.put("membership_role", membershipRole == null ? "" : membershipRole);
		o.put("timezone", timezone == null ? "" : timezone);
		o.put("locale", locale == null ? "" : locale);
		JSONArray modul = new JSONArray();
		for (java.util.Iterator<String> it = moduleEntitlements.iterator(); it.hasNext();) {
			modul.put(it.next());
		}
		o.put("modules", modul);
		return o;
	}

	public String toString() {
		return "TenantContext[id=" + tenantId + ", code=" + tenantCode + ", mode=" + tenantMode
				+ ", aktor=" + activeTbmuserId + "]";
	}

	public static Builder builder() {
		return new Builder();
	}

	/** Pembangun {@link TenantContext}. Enam belas medan terlalu banyak untuk satu konstruktor. */
	public static final class Builder {
		private Long tenantId;
		private String tenantCode;
		private String tenantName;
		private String tenantStatus;
		private String tenantMode;
		private Long membershipId;
		private String membershipRole;
		private Long ownerPendaftarId;
		private Long activePendaftarId;
		private String activeTbmuserId;
		private String schemaName;
		private String auditSchemaName;
		private String schemaVersion;
		private String timezone;
		private String locale;
		private Set<String> moduleEntitlements;

		public Builder tenantId(Long v) { this.tenantId = v; return this; }
		public Builder tenantCode(String v) { this.tenantCode = v; return this; }
		public Builder tenantName(String v) { this.tenantName = v; return this; }
		public Builder tenantStatus(String v) { this.tenantStatus = v; return this; }
		public Builder tenantMode(String v) { this.tenantMode = v; return this; }
		public Builder membershipId(Long v) { this.membershipId = v; return this; }
		public Builder membershipRole(String v) { this.membershipRole = v; return this; }
		public Builder ownerPendaftarId(Long v) { this.ownerPendaftarId = v; return this; }
		public Builder activePendaftarId(Long v) { this.activePendaftarId = v; return this; }
		public Builder activeTbmuserId(String v) { this.activeTbmuserId = v; return this; }
		public Builder schemaName(String v) { this.schemaName = v; return this; }
		public Builder auditSchemaName(String v) { this.auditSchemaName = v; return this; }
		public Builder schemaVersion(String v) { this.schemaVersion = v; return this; }
		public Builder timezone(String v) { this.timezone = v; return this; }
		public Builder locale(String v) { this.locale = v; return this; }
		public Builder moduleEntitlements(Set<String> v) { this.moduleEntitlements = v; return this; }

		public TenantContext build() {
			return new TenantContext(this);
		}
	}
}
