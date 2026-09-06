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

/**
 * Audit minimal pemakaian biometrik: satu baris per satu peristiwa verifikasi/pemakaian
 * kredensial biometrik (mis. tap sidik jari di gerbang, verifikasi wajah saat presensi), mencatat
 * SIAPA melakukan aksi ({@link #getActorUserId()}) terhadap DATA MILIK SIAPA
 * ({@link #getSubjectUserId()} — bisa berbeda dari aktor, mis. petugas gerbang memverifikasi
 * santri), untuk TUJUAN apa ({@link #getPurpose()}), dan HASILNYA (cocok/tidak, skor, kode hasil).
 * Tidak pernah menyimpan template/probe biometrik itu sendiri — hanya metadata hasil verifikasi,
 * konsisten dengan sifatnya sebagai log audit (lihat juga {@link BiometricCredential} tempat
 * template terenkripsi sebenarnya disimpan).
 *
 * <p>Constraint unik {@code uk_biometric_event_mutation} pada
 * ({@code actor_user_id}, {@code purpose}, {@code client_mutation_id}) menjadikan
 * {@link #getClientMutationId()} sebagai kunci idempotensi: percobaan pencatatan event yang sama
 * (mis. retry jaringan dari sisi klien) dengan kombinasi aktor+tujuan+ID mutasi yang identik akan
 * ditolak sebagai duplikat oleh basis data, bukan double-write silent.</p>
 */
@Entity
@Table(schema = "public", name = "biometric_event", uniqueConstraints = @UniqueConstraint(
		name = "uk_biometric_event_mutation",
		columnNames = { "actor_user_id", "purpose", "client_mutation_id" }))
public class BiometricEvent implements Serializable {
	private static final long serialVersionUID = 1L;
	/** ID baris (primary key, auto-increment). */
	private Long id;
	/** ID pengguna yang MELAKUKAN aksi verifikasi ini (mis. petugas/operator perangkat), bisa berbeda dari {@link #subjectUserId}. */
	private String actorUserId;
	/** ID pengguna yang DATANYA diverifikasi (subjek biometrik), lihat {@link BiometricCredential#getSubjectUserId()}. */
	private String subjectUserId;
	/** ID baris {@link BiometricCredential} yang dipakai/diverifikasi pada event ini, bila ada. */
	private Long credentialId;
	/** Modalitas biometrik yang dipakai pada event ini (mis. sidik jari, wajah). */
	private String modality;
	/** Tujuan/konteks bisnis verifikasi ini (mis. "gerbang_pesantren", "presensi"), bagian dari kunci idempotensi bersama {@link #actorUserId}/{@link #clientMutationId}. */
	private String purpose;
	/** ID mutasi buatan klien untuk mencegah pencatatan ganda (idempotency key) — lihat catatan constraint unik pada javadoc kelas. */
	private String clientMutationId;
	/** ID perangkat yang melakukan perekaman/verifikasi (mis. ID scanner/reader biometrik). */
	private String deviceId;
	/** Hasil kecocokan verifikasi; {@code null} diperlakukan sebagai TIDAK cocok (lihat {@link #getMatched()} — fail-closed). */
	private Boolean matched;
	/** Skor kecocokan (confidence) hasil verifikasi biometrik. */
	private Double matchScore;
	/** Skor liveness (deteksi bukan foto/spoofing) hasil verifikasi. */
	private Double livenessScore;
	/** Kode hasil verifikasi (mis. kode sukses/error dari SDK penyedia biometrik). */
	private String resultCode;
	/** Jenis entitas rujukan yang terkait event ini (mis. nama entitas bisnis yang memicu verifikasi), bila ada. */
	private String referenceType;
	/** ID entitas rujukan yang terkait event ini, pasangan dari {@link #referenceType}. */
	private String referenceId;
	/** Respons mentah (kolom {@code TEXT}, format JSON) dari SDK/penyedia biometrik untuk keperluan audit/debug — TIDAK berisi template biometrik (lihat javadoc kelas). */
	private String responseJson;
	/** Waktu perekaman/percobaan verifikasi terjadi di sisi klien/perangkat. */
	private Date capturedAt;
	/** Waktu event ini diterima/dicatat di server; default waktu instansiasi. */
	private Date receivedAt = new Date();

	/** @return ID baris (primary key). */
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	/** @param id ID baris (primary key) yang akan diset. */
	public void setId(Long id) { this.id = id; }
	/** @return ID pengguna yang melakukan aksi verifikasi ini. */
	@Column(name = "actor_user_id", nullable = false, length = 255)
	public String getActorUserId() { return actorUserId; }
	/** @param value ID pengguna pelaku aksi yang akan diset. */
	public void setActorUserId(String value) { actorUserId = value; }
	/** @return ID pengguna yang datanya diverifikasi (subjek biometrik). */
	@Column(name = "subject_user_id", nullable = false, length = 255)
	public String getSubjectUserId() { return subjectUserId; }
	/** @param value ID pengguna subjek yang akan diset. */
	public void setSubjectUserId(String value) { subjectUserId = value; }
	/** @return ID kredensial biometrik ({@link BiometricCredential}) yang dipakai pada event ini, bila ada. */
	@Column(name = "credential_id")
	public Long getCredentialId() { return credentialId; }
	/** @param value ID kredensial yang akan diset. */
	public void setCredentialId(Long value) { credentialId = value; }
	/** @return modalitas biometrik yang dipakai pada event ini. */
	@Column(name = "modality", nullable = false, length = 32)
	public String getModality() { return modality; }
	/** @param value modalitas biometrik yang akan diset. */
	public void setModality(String value) { modality = value; }
	/** @return tujuan/konteks bisnis verifikasi ini. */
	@Column(name = "purpose", nullable = false, length = 40)
	public String getPurpose() { return purpose; }
	/** @param value tujuan verifikasi yang akan diset. */
	public void setPurpose(String value) { purpose = value; }
	/** @return ID mutasi buatan klien (kunci idempotensi). */
	@Column(name = "client_mutation_id", nullable = false, length = 150)
	public String getClientMutationId() { return clientMutationId; }
	/** @param value ID mutasi klien yang akan diset. */
	public void setClientMutationId(String value) { clientMutationId = value; }
	/** @return ID perangkat yang melakukan perekaman/verifikasi. */
	@Column(name = "device_id", length = 150)
	public String getDeviceId() { return deviceId; }
	/** @param value ID perangkat yang akan diset. */
	public void setDeviceId(String value) { deviceId = value; }
	/** @return hasil kecocokan verifikasi; {@code null} diperlakukan sebagai TIDAK cocok (fail-closed). */
	@Column(name = "matched", nullable = false)
	public Boolean getMatched() { return matched == null ? Boolean.FALSE : matched; }
	/** @param value hasil kecocokan yang akan diset. */
	public void setMatched(Boolean value) { matched = value; }
	/** @return skor kecocokan (confidence) hasil verifikasi. */
	@Column(name = "match_score")
	public Double getMatchScore() { return matchScore; }
	/** @param value skor kecocokan yang akan diset. */
	public void setMatchScore(Double value) { matchScore = value; }
	/** @return skor liveness hasil verifikasi. */
	@Column(name = "liveness_score")
	public Double getLivenessScore() { return livenessScore; }
	/** @param value skor liveness yang akan diset. */
	public void setLivenessScore(Double value) { livenessScore = value; }
	/** @return kode hasil verifikasi dari SDK/penyedia biometrik. */
	@Column(name = "result_code", nullable = false, length = 40)
	public String getResultCode() { return resultCode; }
	/** @param value kode hasil yang akan diset. */
	public void setResultCode(String value) { resultCode = value; }
	/** @return jenis entitas rujukan yang terkait event ini, bila ada. */
	@Column(name = "reference_type", length = 40)
	public String getReferenceType() { return referenceType; }
	/** @param value jenis entitas rujukan yang akan diset. */
	public void setReferenceType(String value) { referenceType = value; }
	/** @return ID entitas rujukan yang terkait event ini. */
	@Column(name = "reference_id", length = 150)
	public String getReferenceId() { return referenceId; }
	/** @param value ID entitas rujukan yang akan diset. */
	public void setReferenceId(String value) { referenceId = value; }
	/** @return respons mentah (JSON) dari SDK/penyedia biometrik — tidak berisi template biometrik. */
	@Column(name = "response_json", columnDefinition = "TEXT")
	public String getResponseJson() { return responseJson; }
	/** @param value respons mentah yang akan diset. */
	public void setResponseJson(String value) { responseJson = value; }
	/** @return waktu perekaman/percobaan verifikasi terjadi di sisi klien/perangkat. */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "captured_at")
	public Date getCapturedAt() { return capturedAt; }
	/** @param value waktu perekaman klien yang akan diset. */
	public void setCapturedAt(Date value) { capturedAt = value; }
	/** @return waktu event ini diterima/dicatat di server. */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "received_at", nullable = false)
	public Date getReceivedAt() { return receivedAt; }
	/** @param value waktu penerimaan server yang akan diset. */
	public void setReceivedAt(Date value) { receivedAt = value; }
}
