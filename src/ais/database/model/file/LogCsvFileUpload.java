package ais.database.model.file;

import static javax.persistence.GenerationType.IDENTITY;

import java.sql.Blob;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;




import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import ais.database.model.GeneralValueObject;



/**
 * Model data untuk log csv file upload. Tipe ini membawa state yang dipertukarkan oleh lapisan
 * persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang
 * dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date tanggal_dirubah}, {@code Blob fileContent}, {@code String mimeType}, {@code String
 * fileName}, {@code String keterangan}; pemetaan persistence: tabel {@code public.log_csvfile_upload};
 * inisialisasi/lifecycle ({@code setUploadDate()}); pembacaan/pencarian ({@code getOlehId()}, {@code getOleh()},
 * {@code getTanggal_dirubah()}, {@code getId()}, {@code getFileContent()}, {@code getMimeType()}); mutasi data
 * ({@code setOlehId()}, {@code setOleh()}, {@code onUpdate()}, {@code setTanggal_dirubah()}, {@code setId()},
 * {@code setFileContent()}); operasi domain lain ({@code toString()}). Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "log_csvfile_upload")



public class LogCsvFileUpload extends GeneralValueObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6744917562870234908L;
	private Long id;private String oleh;private String olehId;public String getOlehId() {return olehId;}public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}this.oleh = oleh;}public String getOleh() {return oleh;}@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();public void setTanggal_dirubah(Date tanggal_dirubah) {this.tanggal_dirubah = tanggal_dirubah;}@Temporal(TemporalType.TIMESTAMP)public Date getTanggal_dirubah() {return tanggal_dirubah;}

	public String toString() {
		return mimeType + "_" + fileName;
	}

	private Blob fileContent;
	private String mimeType;
	private String fileName;
	private String keterangan;
	private Date uploadDate = ais.ui.util.WaktuUtil.getDate();

	public LogCsvFileUpload() {

	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "file_content")
	@NotAudited
	public Blob getFileContent() {
		return fileContent;
	}

	public void setFileContent(Blob fileContent) {
		this.fileContent = fileContent;
	}

	@Column(name = "mime_type", length = 255)
	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	@Column(name = "file_name", length = 255)
	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	@Column(name = "keterangan", length = 1000)
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_upload", nullable = false, length = 0)
	public Date getUploadDate() {
		return uploadDate;
	}

	public void setUploadDate(Date uploadDate) {
		this.uploadDate = uploadDate;
	}

}
