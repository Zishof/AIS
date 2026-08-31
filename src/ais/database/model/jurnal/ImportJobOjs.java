package ais.database.model.jurnal;
import java.util.Date; import javax.persistence.*;
/**
 * Entitas Hibernate untuk tabel {@code penelitiandanpengabdian.import_job_ojs} — satu
 * pekerjaan (job) impor data jurnal dari sumber OJS ({@link ImportSumberOjs}, via
 * {@link #getSourceId()}) ke AIS. Setiap job dapat memiliki banyak
 * {@link ImportCheckpointOjs} (tautan lewat id mentah, bukan relasi Hibernate terpetakan)
 * yang merekam progres per tabel/batch.
 *
 * <p>
 * Job bersifat idempoten: {@link #getIdempotencyKey()} dipakai untuk mencegah pekerjaan yang
 * sama dijalankan dobel. {@link #getDryRun()} menandai apakah job hanya simulasi (tidak
 * menulis data final). Hasil akhir job dirangkum dalam {@link #getReportJson()} (ringkasan
 * sukses) dan/atau {@link #getErrorSummary()} (ringkasan kegagalan), dengan
 * {@link #getStartedAt()}/{@link #getFinishedAt()} menandai rentang waktu eksekusi.
 * </p>
 */
@Entity @Table(schema="penelitiandanpengabdian",name="import_job_ojs")
public class ImportJobOjs extends JurnalEntityBase {
 private static final long serialVersionUID=1L; private Long sourceId; private Boolean dryRun; private String status,idempotencyKey,reportJson,errorSummary; private Date startedAt,finishedAt;
 /** Id sumber impor OJS ({@link ImportSumberOjs}) asal data job ini. */
 @Column(name="source_id",nullable=false) public Long getSourceId(){return sourceId;} public void setSourceId(Long v){sourceId=v;}
 /** Menandai apakah job ini hanya simulasi (tidak menulis data final ke tabel produksi). */
 @Column(name="dry_run",nullable=false) public Boolean getDryRun(){return dryRun;} public void setDryRun(Boolean v){dryRun=v;}
 /** Status pekerjaan impor (mis. "ANTRE", "BERJALAN", "SELESAI", "GAGAL"). */
 @Column(name="status",nullable=false,length=30) public String getStatus(){return status;} public void setStatus(String v){status=v;}
 /** Kunci idempotensi untuk mencegah job yang sama dijalankan/dicatat lebih dari sekali. */
 @Column(name="idempotency_key",nullable=false,length=160) public String getIdempotencyKey(){return idempotencyKey;} public void setIdempotencyKey(String v){idempotencyKey=v;}
 /** Ringkasan hasil impor dalam format JSON (jumlah baris, tabel yang diproses, dsb). */
 @Column(name="report_json",columnDefinition="text") public String getReportJson(){return reportJson;} public void setReportJson(String v){reportJson=v;}
 /** Ringkasan kesalahan bila job gagal atau selesai dengan sebagian error. */
 @Column(name="error_summary",columnDefinition="text") public String getErrorSummary(){return errorSummary;} public void setErrorSummary(String v){errorSummary=v;}
 /** Waktu mulai eksekusi job. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="started_at") public Date getStartedAt(){return startedAt;} public void setStartedAt(Date v){startedAt=v;}
 /** Waktu job selesai (berhasil maupun gagal). */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="finished_at") public Date getFinishedAt(){return finishedAt;} public void setFinishedAt(Date v){finishedAt=v;}
}
