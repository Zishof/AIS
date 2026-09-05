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
 *
 * <p>
 * Siklus hidup job dikelola oleh {@code ais.action.master.jurnal.importer.OjsImportExecutionService}
 * ({@code start(...)}/{@code resume(...)}), yang membaca koneksi sumber lewat
 * {@code ImportSumberOjs.getConnectionReference()} + {@code OjsConnectionRegistry} (lihat
 * catatan keamanan pada {@link ImportSumberOjs}), menulis batch {@link ImportMappingOjs} dan
 * checkpoint {@link ImportCheckpointOjs}, lalu ditransformasi ke entitas domain AIS lewat
 * {@code OjsDomainTransformService}. Job dengan {@link #getDryRun()} bernilai {@code true}
 * hanya menginspeksi/mensimulasikan hasil (mis. untuk pratinjau sebelum eksekusi nyata) tanpa
 * menulis entitas AIS final — pola yang sama dipakai action produksi lain di AIS untuk memberi
 * operator kesempatan meninjau dampak sebelum commit sesungguhnya.
 * </p>
 */
@Entity @Table(schema="penelitiandanpengabdian",name="import_job_ojs")
public class ImportJobOjs extends JurnalEntityBase {
 private static final long serialVersionUID=1L; private Long sourceId; private Boolean dryRun; private String status,idempotencyKey,reportJson,errorSummary; private Date startedAt,finishedAt;
 /** Id sumber impor OJS ({@link ImportSumberOjs}) asal data job ini — kolom FK mentah, bukan relasi Hibernate terpetakan. */
 @Column(name="source_id",nullable=false) public Long getSourceId(){return sourceId;} public void setSourceId(Long v){sourceId=v;}
 /** Menandai apakah job ini hanya simulasi (tidak menulis data final ke tabel produksi) — dipakai operator meninjau hasil pemetaan/transformasi sebelum menjalankan impor sesungguhnya. */
 @Column(name="dry_run",nullable=false) public Boolean getDryRun(){return dryRun;} public void setDryRun(Boolean v){dryRun=v;}
 /** Status pekerjaan impor (mis. "ANTRE", "BERJALAN", "SELESAI", "GAGAL") — dipakai menentukan apakah job masih dapat dilanjutkan ({@code resume}) atau sudah final. */
 @Column(name="status",nullable=false,length=30) public String getStatus(){return status;} public void setStatus(String v){status=v;}
 /** Kunci idempotensi (disediakan pemanggil saat memulai job) untuk mencegah job yang sama dijalankan/dicatat lebih dari sekali, mis. akibat klik ganda atau retry jaringan pada permintaan {@code startImport}. */
 @Column(name="idempotency_key",nullable=false,length=160) public String getIdempotencyKey(){return idempotencyKey;} public void setIdempotencyKey(String v){idempotencyKey=v;}
 /** Ringkasan hasil impor dalam format JSON (jumlah baris, tabel yang diproses, dsb.), diisi saat job berjalan/selesai untuk ditampilkan ke operator tanpa perlu menghitung ulang dari {@link ImportCheckpointOjs}. */
 @Column(name="report_json",columnDefinition="text") public String getReportJson(){return reportJson;} public void setReportJson(String v){reportJson=v;}
 /** Ringkasan kesalahan bila job gagal atau selesai dengan sebagian error, dipakai operator mendiagnosis kegagalan tanpa membuka log server. */
 @Column(name="error_summary",columnDefinition="text") public String getErrorSummary(){return errorSummary;} public void setErrorSummary(String v){errorSummary=v;}
 /** Waktu mulai eksekusi job (diisi saat job pertama kali dijalankan, bukan saat baris job dibuat/diantrekan). */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="started_at") public Date getStartedAt(){return startedAt;} public void setStartedAt(Date v){startedAt=v;}
 /** Waktu job selesai (berhasil maupun gagal) — kosong selama job masih berjalan/diantrekan. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="finished_at") public Date getFinishedAt(){return finishedAt;} public void setFinishedAt(Date v){finishedAt=v;}
}
