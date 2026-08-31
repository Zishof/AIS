package ais.database.model.jurnal;
import javax.persistence.*;
/**
 * Entitas Hibernate: satu baris pemetaan hasil impor data dari OJS (Open Journal Systems, sistem
 * jurnal ilmiah eksternal yang datanya diimpor ke modul {@code jurnal} AIS) — dipetakan ke tabel
 * {@code penelitiandanpengabdian.import_mapping_ojs}. Mencatat, untuk satu baris/field sumber OJS
 * ({@link #sourceTable}+{@link #sourcePk}, opsional {@link #sourceField}) yang diproses dalam satu
 * job impor ({@link #jobId}), KE MANA data itu dipetakan di AIS ({@link #targetType}+
 * {@link #targetId}, opsional {@link #targetField}) dan APA keputusan pemetaannya
 * ({@link #decision}, mis. dipetakan otomatis/manual/dilewati/konflik). {@link #rawPayload}
 * menyimpan data mentah sumber sebagai teks (JSON, tidak divalidasi skema di entitas) dan
 * {@link #sourceChecksum} dipakai mendeteksi perubahan data sumber antar-run impor (idempotensi/
 * re-run deteksi). Baris ini sendiri BUKAN hasil impor (bukan data jurnal), melainkan METADATA
 * proses migrasi/importnya — jejak audit &amp; alat bantu rekonsiliasi bila pemetaan perlu ditinjau
 * ulang.
 */
@Entity @Table(schema="penelitiandanpengabdian",name="import_mapping_ojs")
public class ImportMappingOjs extends JurnalEntityBase {
 private static final long serialVersionUID=1L;
 /** Id baris sumber di {@link #sourceTable} OJS (wajib), id job impor yang memproses baris ini (opsional), dan id entitas AIS tujuan pemetaan (opsional, terisi setelah keputusan pemetaan dibuat). */
 private Long sourceId,jobId,targetId;
 /** Nama tabel sumber OJS (wajib); primary key baris sumber sebagai teks (wajib, mendukung PK komposit/non-numerik); nama field sumber spesifik bila pemetaan pada level field, bukan seluruh baris; tipe entitas AIS tujuan pemetaan; nama field tujuan di AIS; keputusan pemetaan (wajib, mis. mapped/manual/skipped/conflict); payload data mentah sumber (teks/JSON); dan checksum data sumber untuk deteksi perubahan antar-run impor. */
 private String sourceTable,sourcePk,sourceField,targetType,targetField,decision,rawPayload,sourceChecksum;
 @Column(name="source_id",nullable=false) public Long getSourceId(){return sourceId;} public void setSourceId(Long v){sourceId=v;}
 @Column(name="job_id") public Long getJobId(){return jobId;} public void setJobId(Long v){jobId=v;}
 @Column(name="source_table",nullable=false,length=160) public String getSourceTable(){return sourceTable;} public void setSourceTable(String v){sourceTable=v;}
 @Column(name="source_pk",nullable=false,length=500) public String getSourcePk(){return sourcePk;} public void setSourcePk(String v){sourcePk=v;}
 @Column(name="source_field",length=160) public String getSourceField(){return sourceField;} public void setSourceField(String v){sourceField=v;}
 @Column(name="target_type",length=255) public String getTargetType(){return targetType;} public void setTargetType(String v){targetType=v;}
 @Column(name="target_id") public Long getTargetId(){return targetId;} public void setTargetId(Long v){targetId=v;}
 @Column(name="target_field",length=160) public String getTargetField(){return targetField;} public void setTargetField(String v){targetField=v;}
 @Column(name="decision",nullable=false,length=40) public String getDecision(){return decision;} public void setDecision(String v){decision=v;}
 @Column(name="raw_payload",columnDefinition="text") public String getRawPayload(){return rawPayload;} public void setRawPayload(String v){rawPayload=v;}
 @Column(name="source_checksum",length=64) public String getSourceChecksum(){return sourceChecksum;} public void setSourceChecksum(String v){sourceChecksum=v;}
}
