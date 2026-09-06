package ais.database.model.biometric;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Template biometrik yang sudah dienkripsi di sisi server.
 *
 * <p>Entitas ini sengaja tidak memakai Envers: menyalin ciphertext biometrik ke
 * tabel audit akan memperbanyak material sensitif. Perubahan dan pemakaian
 * dicatat tanpa template di {@link BiometricEvent}.</p>
 *
 * <p>
 * <b>VERIFIKASI KEAMANAN — template TERENKRIPSI, bukan mentah/plaintext.</b> Diperiksa lewat
 * {@code ais.action.servlet.api.BiometricApi} (pemakai/penulis utama entitas ini): saat menyimpan
 * kredensial baru, nilai template mentah ({@code clear}) dilewatkan ke
 * {@code BiometricCrypto.encrypt(clear, aad(...))} sebelum ditulis ke
 * {@link #getTemplateCiphertext()} — TIDAK ADA jalur yang menulis template mentah langsung ke
 * kolom ini. Saat verifikasi (pencocokan), ciphertext didekripsi kembali lewat
 * {@code BiometricCrypto.decrypt(c.getTemplateCiphertext(), aad(...))}, dengan AAD
 * (Additional Authenticated Data — data konteks yang diikat ke ciphertext tanpa ikut
 * dienkripsi, mis. identitas subjek/modalitas/format) yang mengikat ciphertext ke subjek,
 * modalitas, dan format tertentu (mencegah ciphertext dipakai ulang di konteks lain meski
 * berhasil didekripsi). Ini SESUAI praktik yang benar untuk data biometrik ("special category
 * data" dalam banyak yurisdiksi privasi): template tidak pernah tersimpan sebagai plaintext, dan
 * kunci enkripsinya diberi versi ({@link #getKeyVersion()}) untuk mendukung rotasi kunci di masa
 * depan. {@link #getTemplateHash()} adalah nilai TERPISAH dari ciphertext (kemungkinan untuk
 * deteksi duplikat/lookup cepat tanpa dekripsi penuh, bukan representasi utama template) —
 * bukan pengganti enkripsi, hash saja tidak dipakai untuk pencocokan biometrik di jalur yang
 * diperiksa.
 * </p>
 */
@Entity
@Table(schema = "public", name = "biometric_credential")
public class BiometricCredential implements Serializable {

	private static final long serialVersionUID = 1L;

	/** ID baris (primary key, auto-increment). */
	private Long id;
	/** ID pengguna pemilik template biometrik ini (subjek yang datanya direkam — bukan berarti subjek adalah pemohon/aktor yang melakukan aksi, lihat {@link BiometricEvent}). */
	private String subjectUserId;
	/** Modalitas biometrik (mis. sidik jari, wajah) yang direkam template ini. */
	private String modality;
	/** Kode posisi/slot (mis. jari tangan yang mana) bila modalitas memerlukan pembedaan posisi. */
	private String positionCode;
	/** Format/versi struktur data template (mis. nama algoritma/SDK penyedia biometrik), dipakai untuk memvalidasi kompatibilitas saat dekripsi/pencocokan. */
	private String templateFormat;
	/** Ciphertext template biometrik — HASIL ENKRIPSI (lihat catatan keamanan pada javadoc kelas), bukan template mentah/plaintext. */
	private String templateCiphertext;
	/** Hash template, terpisah dari ciphertext (kemungkinan untuk deteksi duplikat/lookup cepat) — bukan representasi utama template dan bukan pengganti enkripsi. */
	private String templateHash;
	/** Versi kunci enkripsi yang dipakai untuk {@link #templateCiphertext}, mendukung rotasi kunci di masa depan. */
	private String keyVersion;
	/** Penyedia/vendor SDK biometrik yang menghasilkan template ini. */
	private String provider;
	/** Skor kualitas template saat perekaman (dipakai untuk menolak template berkualitas rendah). */
	private Integer qualityScore;
	/** Status aktif kredensial ini; {@code null} diperlakukan sebagai TIDAK aktif (lihat {@link #getActive()} — kebalikan dari konvensi umum AIS yang biasanya default aktif, perhatikan bila membaca data lama). */
	private Boolean active = Boolean.TRUE;
	/** Waktu subjek memberikan persetujuan (consent) perekaman data biometrik ini. */
	private Date consentAt;
	/** Identitas pihak yang mencatatkan persetujuan (petugas/operator, atau subjek sendiri bila self-service). */
	private String consentBy;
	/** Waktu baris ini dibuat; default waktu instansiasi. */
	private Date createdAt = new Date();
	/** Waktu baris ini terakhir diperbarui; default waktu instansiasi (perlu diset ulang manual saat update, tidak ada callback otomatis seperti entitas AIS lain yang memakai {@code onUpdate()}). */
	private Date updatedAt = new Date();

	/** @return ID baris (primary key). */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	/** @param id ID baris (primary key) yang akan diset. */
	public void setId(Long id) { this.id = id; }

	/** @return ID pengguna pemilik/subjek template biometrik ini. */
	@Column(name = "subject_user_id", nullable = false, length = 255)
	public String getSubjectUserId() { return subjectUserId; }
	/** @param subjectUserId ID pengguna subjek yang akan diset. */
	public void setSubjectUserId(String subjectUserId) { this.subjectUserId = subjectUserId; }

	/** @return modalitas biometrik (mis. sidik jari, wajah) template ini. */
	@Column(name = "modality", nullable = false, length = 32)
	public String getModality() { return modality; }
	/** @param modality modalitas biometrik yang akan diset. */
	public void setModality(String modality) { this.modality = modality; }

	/** @return kode posisi/slot template ini, bila berlaku. */
	@Column(name = "position_code", length = 40)
	public String getPositionCode() { return positionCode; }
	/** @param positionCode kode posisi/slot yang akan diset. */
	public void setPositionCode(String positionCode) { this.positionCode = positionCode; }

	/** @return format/versi struktur data template. */
	@Column(name = "template_format", nullable = false, length = 80)
	public String getTemplateFormat() { return templateFormat; }
	/** @param templateFormat format template yang akan diset. */
	public void setTemplateFormat(String templateFormat) { this.templateFormat = templateFormat; }

	/**
	 * @return ciphertext template biometrik (hasil {@code BiometricCrypto.encrypt(...)}) — TIDAK
	 *         PERNAH berupa template mentah/plaintext, lihat catatan keamanan pada javadoc kelas.
	 */
	@Column(name = "template_ciphertext", nullable = false, columnDefinition = "TEXT")
	public String getTemplateCiphertext() { return templateCiphertext; }
	/** @param templateCiphertext ciphertext template yang akan diset — HARUS sudah terenkripsi oleh pemanggil, bukan template mentah. */
	public void setTemplateCiphertext(String templateCiphertext) { this.templateCiphertext = templateCiphertext; }

	/** @return hash template (terpisah dari ciphertext, lihat catatan pada javadoc kelas). */
	@Column(name = "template_hash", nullable = false, length = 64)
	public String getTemplateHash() { return templateHash; }
	/** @param templateHash hash template yang akan diset. */
	public void setTemplateHash(String templateHash) { this.templateHash = templateHash; }

	/** @return versi kunci enkripsi yang dipakai untuk ciphertext ini. */
	@Column(name = "key_version", nullable = false, length = 32)
	public String getKeyVersion() { return keyVersion; }
	/** @param keyVersion versi kunci enkripsi yang akan diset. */
	public void setKeyVersion(String keyVersion) { this.keyVersion = keyVersion; }

	/** @return penyedia/vendor SDK biometrik penghasil template ini. */
	@Column(name = "provider", length = 120)
	public String getProvider() { return provider; }
	/** @param provider penyedia/vendor SDK yang akan diset. */
	public void setProvider(String provider) { this.provider = provider; }

	/** @return skor kualitas template saat perekaman. */
	@Column(name = "quality_score")
	public Integer getQualityScore() { return qualityScore; }
	/** @param qualityScore skor kualitas yang akan diset. */
	public void setQualityScore(Integer qualityScore) { this.qualityScore = qualityScore; }

	/**
	 * @return status aktif kredensial; {@code null} diperlakukan sebagai TIDAK aktif
	 *         ({@code Boolean.FALSE}) — kebalikan dari konvensi default-aktif yang umum dipakai
	 *         entitas lain di {@code ais.database.model}, sengaja fail-closed untuk data sensitif.
	 */
	@Column(name = "active", nullable = false)
	public Boolean getActive() { return active == null ? Boolean.FALSE : active; }
	/** @param active status aktif yang akan diset. */
	public void setActive(Boolean active) { this.active = active; }

	/** @return waktu subjek memberikan persetujuan perekaman data biometrik ini. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "consent_at", nullable = false)
	public Date getConsentAt() { return consentAt; }
	/** @param consentAt waktu persetujuan yang akan diset. */
	public void setConsentAt(Date consentAt) { this.consentAt = consentAt; }

	/** @return identitas pihak yang mencatatkan persetujuan. */
	@Column(name = "consent_by", nullable = false, length = 255)
	public String getConsentBy() { return consentBy; }
	/** @param consentBy identitas pencatat persetujuan yang akan diset. */
	public void setConsentBy(String consentBy) { this.consentBy = consentBy; }

	/** @return waktu baris ini dibuat. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created_at", nullable = false)
	public Date getCreatedAt() { return createdAt; }
	/** @param createdAt waktu pembuatan yang akan diset. */
	public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

	/** @return waktu baris ini terakhir diperbarui. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "updated_at", nullable = false)
	public Date getUpdatedAt() { return updatedAt; }
	/** @param updatedAt waktu pembaruan yang akan diset (harus diset manual oleh pemanggil, tidak ada callback {@code @PreUpdate} otomatis). */
	public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
