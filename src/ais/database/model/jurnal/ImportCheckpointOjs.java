package ais.database.model.jurnal;
import javax.persistence.*;
/**
 * Entitas Hibernate untuk tabel {@code penelitiandanpengabdian.import_checkpoint_ojs} —
 * titik pemulihan (checkpoint) proses impor data dari sistem OJS (Open Journal Systems) ke
 * AIS, dicatat per batch per tabel sumber di bawah satu pekerjaan impor ({@link #getJobId()},
 * mengacu ke {@link ImportJobOjs}).
 *
 * <p>
 * Checkpoint menyimpan posisi terakhir yang sudah diproses ({@link #getCursorValue()}) beserta
 * penghitung hasil ({@link #getProcessedCount()}/{@link #getAcceptedCount()}/
 * {@link #getFailedCount()}) untuk satu {@link #getSourceTable()} OJS, sehingga proses impor
 * yang terhenti (mis. karena error atau restart) dapat dilanjutkan dari titik terakhir tanpa
 * mengulang dari awal. Tidak ada relasi Hibernate terpetakan ke {@code ImportJobOjs}; tautan
 * dilakukan lewat id mentah {@link #getJobId()}.
 * </p>
 *
 * <p>
 * Satu {@link ImportJobOjs} dapat memiliki BANYAK baris checkpoint (satu per kombinasi tabel
 * sumber + nomor batch), membentuk jejak progres inkremental yang dibaca oleh
 * {@code OjsImportExecutionService.resume(...)}/{@code start(...)} untuk menentukan dari mana
 * batch berikutnya harus melanjutkan pembacaan {@link #getSourceTable()} — lihat juga uji
 * {@code OjsImportCancelResumeSelfTest} yang memverifikasi perilaku lanjut/batal ini.
 * {@link #getProcessedCount()} = {@link #getAcceptedCount()} + {@link #getFailedCount()} secara
 * konseptual (jumlah baris yang berhasil ditambah yang gagal pada batch tersebut).
 * </p>
 */
@Entity @Table(schema="penelitiandanpengabdian",name="import_checkpoint_ojs")
public class ImportCheckpointOjs extends JurnalEntityBase {
 private static final long serialVersionUID=1L; private Long jobId,processedCount,acceptedCount,failedCount; private Integer batchNumber; private String sourceTable,cursorValue,status;
 /** Id pekerjaan impor ({@link ImportJobOjs}) pemilik checkpoint ini — kolom FK mentah, bukan relasi Hibernate terpetakan. */
 @Column(name="job_id",nullable=false) public Long getJobId(){return jobId;} public void setJobId(Long v){jobId=v;}
 /** Nama tabel sumber di basis data OJS yang sedang diimpor pada checkpoint ini (mis. "submissions"); satu job dapat punya rangkaian checkpoint berbeda per tabel. */
 @Column(name="source_table",nullable=false,length=160) public String getSourceTable(){return sourceTable;} public void setSourceTable(String v){sourceTable=v;}
 /** Nilai kursor/posisi terakhir yang sudah diproses pada tabel sumber (mis. id baris terakhir atau nilai kolom urut), dipakai sebagai titik lanjut ({@code WHERE id > cursorValue}, dsb.) saat resume impor. */
 @Column(name="cursor_value",length=500) public String getCursorValue(){return cursorValue;} public void setCursorValue(String v){cursorValue=v;}
 /** Nomor urut batch impor ini dalam pekerjaan yang sama (bertambah setiap kali satu potongan/chunk data selesai diproses). */
 @Column(name="batch_number",nullable=false) public Integer getBatchNumber(){return batchNumber;} public void setBatchNumber(Integer v){batchNumber=v;}
 /** Jumlah baris sumber yang sudah diproses pada batch ini — secara konseptual sama dengan {@link #getAcceptedCount()} + {@link #getFailedCount()}. */
 @Column(name="processed_count",nullable=false) public Long getProcessedCount(){return processedCount;} public void setProcessedCount(Long v){processedCount=v;}
 /** Jumlah baris yang berhasil diterima/dimasukkan ke AIS pada batch ini (menghasilkan baris {@link ImportMappingOjs} dengan keputusan berhasil). */
 @Column(name="accepted_count",nullable=false) public Long getAcceptedCount(){return acceptedCount;} public void setAcceptedCount(Long v){acceptedCount=v;}
 /** Jumlah baris yang gagal diimpor pada batch ini (mis. gagal validasi/transformasi); rincian kegagalan biasanya dirujuk lewat ringkasan error pada {@link ImportJobOjs#getErrorSummary()}. */
 @Column(name="failed_count",nullable=false) public Long getFailedCount(){return failedCount;} public void setFailedCount(Long v){failedCount=v;}
 /** Status checkpoint batch ini (mis. "BERJALAN", "SELESAI", "GAGAL") — dipakai menentukan apakah batch ini valid sebagai titik lanjut ({@code resume}) atau perlu diulang. */
 @Column(name="status",nullable=false,length=30) public String getStatus(){return status;} public void setStatus(String v){status=v;}
}
