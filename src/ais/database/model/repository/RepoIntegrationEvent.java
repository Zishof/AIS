package ais.database.model.repository;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

/**
 * Entitas Hibernate yang memetakan tabel {@code public.repo_integration_event}
 * — audit append-only (immutable, {@code dynamicUpdate = false}) untuk setiap
 * panggilan ke layanan integrasi eksternal pada modul repositori
 * institusional: registrasi DOI, verifikasi ORCID, resolusi ROR (Research
 * Organization Registry), notifikasi COAR Notify antar-repositori, pemindaian
 * antivirus berkas unggahan, dan layanan berbasis AI (mis. deteksi
 * kemiripan/klasifikasi topik). Polanya sejalan dengan audit log integrasi
 * API eksternal lain di aplikasi ini (bandingkan {@code DataSisterApi}):
 * satu baris = satu percobaan pemanggilan, menyimpan payload
 * permintaan/respons apa adanya untuk keperluan diagnosis kegagalan.
 *
 * <p>
 * Baris ini bersifat multi-tenant ({@link #getTenantKey()}) dan boleh tidak
 * terkait item tertentu ({@link #getItemId()} nullable — mis. verifikasi
 * ORCID penulis independen dari item manapun).
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=false)
@Table(schema="public",name="repo_integration_event")
public class RepoIntegrationEvent implements Serializable {
    private static final long serialVersionUID=1L;private Long id,itemId;private String tenantKey,serviceName,actionName,status,actorId,requestId,errorMessage;private String requestPayload,responsePayload;private Date createdAt;
    /** Id baris log integrasi ini (identity, auto-generated). */
    @Id @GeneratedValue(strategy=IDENTITY) @Column(name="id",insertable=false,nullable=false)public Long getId(){return id;}public void setId(Long v){id=v;}
    /** Id {@link RepoItem} terkait, bila panggilan integrasi ini menyangkut satu item spesifik (mis. registrasi DOI item); {@code null} bila tidak terkait item manapun. */
    @Column(name="item_id")public Long getItemId(){return itemId;}public void setItemId(Long v){itemId=v;}
    /** Kunci penyewa (tenant) pemilik baris log ini — dasar isolasi data antar-institusi pada instalasi multi-tenant. */
    @Column(name="tenant_key",nullable=false,length=120)public String getTenantKey(){return tenantKey;}public void setTenantKey(String v){tenantKey=v;}
    /** Nama layanan eksternal yang dipanggil, mis. {@code "DOI"}, {@code "ORCID"}, {@code "ROR"}, {@code "COAR_NOTIFY"}, {@code "ANTIVIRUS"}, {@code "AI"}. */
    @Column(name="service_name",nullable=false,length=60)public String getServiceName(){return serviceName;}public void setServiceName(String v){serviceName=v;}
    /** Nama operasi/aksi spesifik dalam layanan tersebut, mis. {@code "REGISTER"}, {@code "LOOKUP"}, {@code "SCAN"}. */
    @Column(name="action_name",nullable=false,length=80)public String getActionName(){return actionName;}public void setActionName(String v){actionName=v;}
    /** Status hasil pemanggilan, mis. {@code "SUCCESS"}, {@code "FAILED"}, {@code "PENDING"}. */
    @Column(name="status",nullable=false,length=30)public String getStatus(){return status;}public void setStatus(String v){status=v;}
    /** Id pengguna yang memicu pemanggilan integrasi ini (bila dipicu aksi pengguna, bukan proses batch/terjadwal otomatis). */
    @Column(name="actor_id",length=255)public String getActorId(){return actorId;}public void setActorId(String v){actorId=v;}
    /** Id korelasi permintaan untuk menelusuri satu pemanggilan yang sama lintas log/audit lain. */
    @Column(name="request_id",length=120)public String getRequestId(){return requestId;}public void setRequestId(String v){requestId=v;}
    /** Payload permintaan yang dikirim ke layanan eksternal, disimpan apa adanya untuk audit/diagnosis. */
    @Column(name="request_payload",columnDefinition="TEXT")public String getRequestPayload(){return requestPayload;}public void setRequestPayload(String v){requestPayload=v;}
    /** Payload respons yang diterima dari layanan eksternal, disimpan apa adanya untuk audit/diagnosis. */
    @Column(name="response_payload",columnDefinition="TEXT")public String getResponsePayload(){return responsePayload;}public void setResponsePayload(String v){responsePayload=v;}
    /** Pesan kesalahan bila pemanggilan gagal ({@link #getStatus()} = {@code "FAILED"}); {@code null} bila berhasil. */
    @Column(name="error_message",columnDefinition="TEXT")public String getErrorMessage(){return errorMessage;}public void setErrorMessage(String v){errorMessage=v;}
    /** Waktu persis pemanggilan integrasi ini tercatat. */
    @Temporal(TemporalType.TIMESTAMP)@Column(name="created_at",nullable=false)public Date getCreatedAt(){return createdAt;}public void setCreatedAt(Date v){createdAt=v;}
}
