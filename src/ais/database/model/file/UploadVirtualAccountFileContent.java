package ais.database.model.file;

// Generated May 15, 2010 10:07:50 AM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.sql.Blob;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * Berkas asal (mis. rekonsiliasi/rekap bank) yang diunggah untuk membentuk satu baris
 * {@code UploadVirtualAccount} -- tabel {@code upload_virtual_account_file_content}. Dipakai
 * oleh {@code UploadVirtualAccountBRIAction}/{@code UploadVirtualAccountBankNTTAction}: setelah
 * berkas (mis. CSV/PDF rekening koran) diproses menjadi baris-baris {@code UploadVirtualAccount},
 * berkas asalnya sendiri disimpan di sini lewat {@link #getRef()} sebagai FK ke id
 * {@code UploadVirtualAccount} tersebut, sehingga berkas sumber bisa diunduh ulang untuk audit
 * (lihat pemanggil yang mencari lewat {@code Restrictions.eq("ref", uploadVirtualAccount.getId())}).
 *
 * <p>Modul virtual account terkait entity ini ({@code VirtualAccountBank}) sebelumnya punya
 * temuan command injection tersendiri (dilacak terpisah); kelas ini sendiri hanya menyimpan blob
 * berkas dan tidak menjalankan proses eksternal apa pun.</p>
 *
 * <p><b>Google Drive sebagai sumber alternatif.</b> {@link #getGdrive()}/{@link
 * #getGdriveUsername()} TIDAK dipetakan sebagai kolom JPA; nilainya disimpan lewat cache berkas
 * per-instance {@link ais.database.model.GeneralValueObject#put(String, String) put}/{@link
 * ais.database.model.GeneralValueObject#retreive(String) retreive} milik {@code
 * GeneralValueObject} induk. Selama {@link #getGdrive()} terisi, {@link #getFoto()} sengaja
 * mengembalikan {@code null} sebagai pertanda berkas asli harus diambil dari Google Drive.</p>
 *
 * <p><b>Baris "copy".</b> {@link #getCopyDari()} adalah asosiasi opsional ke baris
 * {@code UploadVirtualAccountFileContent} lain; ketika terisi, {@link #getNama()} dan {@link
 * #getFoto()} membaca nilainya dari baris sumber tersebut -- pola berbagi satu berkas fisik di
 * antara banyak baris tanpa menduplikasi blob, sama seperti subclass {@link FileFoto} lain.</p>
 *
 * @see FileFoto
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "upload_virtual_account_file_content")

public class UploadVirtualAccountFileContent extends FileFoto {
	/**
	 * Path/lokasi penyimpanan lokal baris ini. Field ini MENIMPA (shadow) field privat sejenis di
	 * {@link FileFoto}: tidak diberi anotasi JPA ({@code @Column}), jadi bukan kolom ter-mapping --
	 * getter/setter di sini hanya menyediakan state in-memory milik baris.
	 */
	private String lokasiSimpan;

	/** @return {@link #lokasiSimpan}, path penyimpanan lokal baris ini (bukan kolom database). */
	public String getLokasiSimpan() {
		return lokasiSimpan;
	}

	/** @param lokasiSimpan path penyimpanan lokal baru untuk baris ini. */
	public void setLokasiSimpan(String lokasiSimpan) {
		this.lokasiSimpan = lokasiSimpan;
	}

	/**
	 *
	 */
	private static final long serialVersionUID = 8396956558947881938L;
	private Long id;
	private String oleh;
	private String olehId;

	/** @return id pengguna (String) yang mengunggah/mengubah baris ini, atau {@code null}. */
	public String getOlehId() {
		return olehId;
	}

	/** Menetapkan id pengunggah; nilai {@code null} atau kosong-setelah-trim diabaikan (field lama tidak ditimpa). */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/** Menetapkan nama pengunggah; nilai {@code null} atau kosong-setelah-trim diabaikan (field lama tidak ditimpa). */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pengguna yang mengunggah/mengubah baris ini, atau {@code null}. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: menandai timestamp perubahan lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} setiap baris ini
	 * di-{@code UPDATE}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah waktu perubahan terakhir baris ini; lihat {@link #onUpdate()}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini, diinisialisasi ke waktu sekarang saat objek dibuat. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return gabungan {@link #nama} dan {@link #fileMimeType} dipisah garis bawah; bisa memuat literal {@code "null"} bila salah satu belum diisi. */
	public String toString() {
		return nama + "_" + fileMimeType;
	}

	private Long ref;
	private Blob foto;
	private String nama;
	private String keterangan;
	private String fileMimeType;
	private Date uploadDate = ais.ui.util.WaktuUtil.getDate();
	private UploadVirtualAccountFileContent copyDari;

	/** @return selalu {@code null}; kelas ini tidak membedakan "jenis" lampiran. */
	@Override
	public String ambilJenis() {
		return null;
	}

	/** Konstruktor default (dipakai Hibernate). */
	public UploadVirtualAccountFileContent() {
	}

	/** @return primary key baris ini; kolom identity, tidak pernah di-{@code INSERT} manual. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id primary key baris ini. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return id {@code UploadVirtualAccount} yang dibentuk dari berkas ini (FK longgar, tanpa
	 *         {@code @ManyToOne}). Dipakai pemanggil untuk mencari kembali berkas asal lewat
	 *         {@code Restrictions.eq("ref", uploadVirtualAccount.getId())} (lihat Javadoc kelas).
	 */
	@Column(name = "ref")
	public Long getRef() {
		return this.ref;
	}

	/** @param ref id {@code UploadVirtualAccount} yang dibentuk dari berkas ini. */
	public void setRef(Long ref) {
		this.ref = ref;
	}

	/** @param fileMimeType mime-type berkas ini. */
	public void setFileMimeType(String fileMimeType) {
		this.fileMimeType = fileMimeType;
	}

	/**
	 * @return mime-type berkas ini, atau {@code null} bila belum diisi. Bila {@link #copyDari}
	 *         terisi, nilainya disegarkan lebih dulu dari mime-type baris sumber.
	 */
	@Column(name = "file_mime_tipe", length = 255)
	public String getFileMimeType() {
		if (copyDari != null) {
			fileMimeType = copyDari.fileMimeType;
		}
		return fileMimeType;
	}

	/** @param uploadDate waktu unggah berkas ini. */
	public void setUploadDate(Date uploadDate) {
		this.uploadDate = uploadDate;
	}

	/** @return waktu unggah berkas ini, diinisialisasi ke waktu sekarang saat objek dibuat. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_upload", nullable = false, length = 0)
	public Date getUploadDate() {
		return uploadDate;
	}

	/** @param realFile nama berkas asli (disimpan ke kolom {@code real_file}). */
	public void setNama(String realFile) {
		this.nama = realFile;
	}

	/**
	 * @return nama berkas ini, dipetakan ke kolom {@code real_file}. Bila {@link #copyDari}
	 *         terisi, nilainya disegarkan lebih dulu dari nama baris sumber. Berbeda dari
	 *         kebanyakan subclass {@link FileFoto} lain, method ini TIDAK men-trim atau
	 *         memberi default nilai apa pun saat kosong.
	 */
	@Column(name = "real_file", length = 255)
	public String getNama() {
		if (copyDari != null) {
			nama = copyDari.nama;
		}
		return nama;
	}

	/** @param foto isi biner berkas ini. */
	public void setFoto(Blob foto) {
		this.foto = foto;
	}

	/**
	 * @return {@code null} bila {@link #getGdrive()} terisi (berkas asli ada di Google Drive, bukan
	 *         di kolom ini); jika tidak, blob milik {@link #copyDari} bila terisi, atau blob baris
	 *         ini sendiri. Dipetakan ke kolom {@code filecontent} dan tidak diaudit
	 *         ({@code @NotAudited}) karena isi biner tidak perlu dilacak riwayatnya oleh Envers.
	 */
	@NotAudited
	@Column(name = "filecontent")
	public Blob getFoto() {

		return gdrive != null && !gdrive.trim().isEmpty() ? null : (copyDari == null ? foto : copyDari.foto);
	}

	/**
	 * @return baris {@code UploadVirtualAccountFileContent} sumber bila baris ini adalah "copy"
	 *         yang berbagi berkas fisik dengan baris lain; {@code null} bila baris ini berdiri
	 *         sendiri. {@code NotFoundAction.IGNORE} membuat asosiasi yang menunjuk baris yang
	 *         sudah terhapus diperlakukan sebagai {@code null}, bukan melempar exception.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@NotFound(action = NotFoundAction.IGNORE)
	@JoinColumn(name = "copy_dari", nullable = true)
	public UploadVirtualAccountFileContent getCopyDari() {
		return copyDari;
	}

	/** @param copyDari baris sumber untuk berbagi berkas fisik (lihat {@link #getCopyDari()}). */
	public void setCopyDari(UploadVirtualAccountFileContent copyDari) {
		this.copyDari = copyDari;
	}

	private String gdrive;
	private String gdriveUsername;

	/**
	 * @return URL Google Drive tempat berkas sesungguhnya disimpan, atau {@code null} bila berkas
	 *         disimpan sebagai blob biasa. Bukan kolom JPA -- nilainya dibaca dari cache berkas
	 *         per-instance {@link ais.database.model.GeneralValueObject#retreive(String)
	 *         retreive("gdrive")} milik induk, dengan penyegaran lebih dulu dari {@link #copyDari}
	 *         bila terisi.
	 */
	public String getGdrive() {
		if (copyDari != null) {
			gdrive = copyDari.gdrive;
		}
		String s = gdrive == null || gdrive.trim().isEmpty() ? retreive("gdrive") : gdrive;
		return s != null && !s.trim().isEmpty() ? s : gdrive;
	}

	/**
	 * Menetapkan URL Google Drive berkas ini. Nilai tidak kosong ditulis ke cache berkas
	 * per-instance lewat {@link ais.database.model.GeneralValueObject#put(String, String)
	 * put(gdrive, "gdrive")} milik induk -- BUKAN ke kolom database.
	 *
	 * @param gdrive URL Google Drive baru; {@code null}/kosong tidak ditulis ke cache (hanya
	 *               mengubah field in-memory).
	 */
	public void setGdrive(String gdrive) {
		if (gdrive != null && !gdrive.trim().isEmpty()) {
			put(gdrive, "gdrive");
		}
		this.gdrive = gdrive;
	}

	/**
	 * @return nama pengguna akun Google Drive terkait, disegarkan lebih dulu dari {@link
	 *         #copyDari} bila terisi. Bukan kolom JPA -- murni field in-memory.
	 */
	public String getGdriveUsername() {
		if (copyDari != null) {
			gdriveUsername = copyDari.gdriveUsername;
		}
		return gdriveUsername;
	}

	/** @param gdriveUsername nama pengguna akun Google Drive terkait. */
	public void setGdriveUsername(String gdriveUsername) {
		this.gdriveUsername = gdriveUsername;
	}

	/** @return selalu {@code null}; kelas ini tidak menyediakan tautan eksternal langsung. */
	@Override
	public String ambilLink() {
		return null;
	}

	/** @return {@link #getRef()}, id {@code UploadVirtualAccount} yang dibentuk dari berkas ini, dipakai sebagai referensi generik oleh {@link FileFoto}. */
	@Override
	public Long ambilRef() {
		// TODO Auto-generated method stub
		return ref;
	}

	/** @return keterangan baris ini, atau string kosong (bukan {@code null}) bila belum diisi. */
	public String getKeterangan() {
		return keterangan == null ? "" : keterangan;
	}

	/** @param keterangan keterangan baris ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}
}
