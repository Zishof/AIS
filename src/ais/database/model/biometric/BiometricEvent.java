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
import javax.persistence.UniqueConstraint;

/** Audit minimal pemakaian biometrik. Tidak pernah menyimpan template/probe. */
@Entity
@Table(schema = "public", name = "biometric_event", uniqueConstraints = @UniqueConstraint(
		name = "uk_biometric_event_mutation",
		columnNames = { "actor_user_id", "purpose", "client_mutation_id" }))
public class BiometricEvent implements Serializable {
	private static final long serialVersionUID = 1L;
	private Long id;
	private String actorUserId;
	private String subjectUserId;
	private Long credentialId;
	private String modality;
	private String purpose;
	private String clientMutationId;
	private String deviceId;
	private Boolean matched;
	private Double matchScore;
	private Double livenessScore;
	private String resultCode;
	private String referenceType;
	private String referenceId;
	private String responseJson;
	private Date capturedAt;
	private Date receivedAt = new Date();

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	@Column(name = "actor_user_id", nullable = false, length = 255)
	public String getActorUserId() { return actorUserId; }
	public void setActorUserId(String value) { actorUserId = value; }
	@Column(name = "subject_user_id", nullable = false, length = 255)
	public String getSubjectUserId() { return subjectUserId; }
	public void setSubjectUserId(String value) { subjectUserId = value; }
	@Column(name = "credential_id")
	public Long getCredentialId() { return credentialId; }
	public void setCredentialId(Long value) { credentialId = value; }
	@Column(name = "modality", nullable = false, length = 32)
	public String getModality() { return modality; }
	public void setModality(String value) { modality = value; }
	@Column(name = "purpose", nullable = false, length = 40)
	public String getPurpose() { return purpose; }
	public void setPurpose(String value) { purpose = value; }
	@Column(name = "client_mutation_id", nullable = false, length = 150)
	public String getClientMutationId() { return clientMutationId; }
	public void setClientMutationId(String value) { clientMutationId = value; }
	@Column(name = "device_id", length = 150)
	public String getDeviceId() { return deviceId; }
	public void setDeviceId(String value) { deviceId = value; }
	@Column(name = "matched", nullable = false)
	public Boolean getMatched() { return matched == null ? Boolean.FALSE : matched; }
	public void setMatched(Boolean value) { matched = value; }
	@Column(name = "match_score")
	public Double getMatchScore() { return matchScore; }
	public void setMatchScore(Double value) { matchScore = value; }
	@Column(name = "liveness_score")
	public Double getLivenessScore() { return livenessScore; }
	public void setLivenessScore(Double value) { livenessScore = value; }
	@Column(name = "result_code", nullable = false, length = 40)
	public String getResultCode() { return resultCode; }
	public void setResultCode(String value) { resultCode = value; }
	@Column(name = "reference_type", length = 40)
	public String getReferenceType() { return referenceType; }
	public void setReferenceType(String value) { referenceType = value; }
	@Column(name = "reference_id", length = 150)
	public String getReferenceId() { return referenceId; }
	public void setReferenceId(String value) { referenceId = value; }
	@Column(name = "response_json", columnDefinition = "TEXT")
	public String getResponseJson() { return responseJson; }
	public void setResponseJson(String value) { responseJson = value; }
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "captured_at")
	public Date getCapturedAt() { return capturedAt; }
	public void setCapturedAt(Date value) { capturedAt = value; }
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "received_at", nullable = false)
	public Date getReceivedAt() { return receivedAt; }
	public void setReceivedAt(Date value) { receivedAt = value; }
}
