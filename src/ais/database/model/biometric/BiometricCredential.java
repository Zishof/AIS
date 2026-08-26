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
 */
@Entity
@Table(schema = "public", name = "biometric_credential")
public class BiometricCredential implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long id;
	private String subjectUserId;
	private String modality;
	private String positionCode;
	private String templateFormat;
	private String templateCiphertext;
	private String templateHash;
	private String keyVersion;
	private String provider;
	private Integer qualityScore;
	private Boolean active = Boolean.TRUE;
	private Date consentAt;
	private String consentBy;
	private Date createdAt = new Date();
	private Date updatedAt = new Date();

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	@Column(name = "subject_user_id", nullable = false, length = 255)
	public String getSubjectUserId() { return subjectUserId; }
	public void setSubjectUserId(String subjectUserId) { this.subjectUserId = subjectUserId; }

	@Column(name = "modality", nullable = false, length = 32)
	public String getModality() { return modality; }
	public void setModality(String modality) { this.modality = modality; }

	@Column(name = "position_code", length = 40)
	public String getPositionCode() { return positionCode; }
	public void setPositionCode(String positionCode) { this.positionCode = positionCode; }

	@Column(name = "template_format", nullable = false, length = 80)
	public String getTemplateFormat() { return templateFormat; }
	public void setTemplateFormat(String templateFormat) { this.templateFormat = templateFormat; }

	@Column(name = "template_ciphertext", nullable = false, columnDefinition = "TEXT")
	public String getTemplateCiphertext() { return templateCiphertext; }
	public void setTemplateCiphertext(String templateCiphertext) { this.templateCiphertext = templateCiphertext; }

	@Column(name = "template_hash", nullable = false, length = 64)
	public String getTemplateHash() { return templateHash; }
	public void setTemplateHash(String templateHash) { this.templateHash = templateHash; }

	@Column(name = "key_version", nullable = false, length = 32)
	public String getKeyVersion() { return keyVersion; }
	public void setKeyVersion(String keyVersion) { this.keyVersion = keyVersion; }

	@Column(name = "provider", length = 120)
	public String getProvider() { return provider; }
	public void setProvider(String provider) { this.provider = provider; }

	@Column(name = "quality_score")
	public Integer getQualityScore() { return qualityScore; }
	public void setQualityScore(Integer qualityScore) { this.qualityScore = qualityScore; }

	@Column(name = "active", nullable = false)
	public Boolean getActive() { return active == null ? Boolean.FALSE : active; }
	public void setActive(Boolean active) { this.active = active; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "consent_at", nullable = false)
	public Date getConsentAt() { return consentAt; }
	public void setConsentAt(Date consentAt) { this.consentAt = consentAt; }

	@Column(name = "consent_by", nullable = false, length = 255)
	public String getConsentBy() { return consentBy; }
	public void setConsentBy(String consentBy) { this.consentBy = consentBy; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created_at", nullable = false)
	public Date getCreatedAt() { return createdAt; }
	public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "updated_at", nullable = false)
	public Date getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
