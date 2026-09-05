package ais.database.model.file;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
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

/**
 * Hasil ekstraksi teks (OCR/full-text) satu halaman dari berkas {@link FileBukuBahanAjar} --
 * tabel {@code file_buku_bahan_ajar_text}. Setiap baris merujuk WAJIB ({@code nullable = false})
 * ke satu {@link FileBukuBahanAjar} lewat {@link #getFileBukuBahanAjar()}, dengan {@link
 * #getHalaman()} sebagai nomor halaman dan {@link #getIsi()} sebagai teks hasil ekstraksinya --
 * dipakai untuk pencarian teks penuh (full-text search) atas isi buku bahan ajar tanpa perlu
 * membaca ulang blob biner {@link FileBukuBahanAjar#getFoto()} setiap kali dicari.
 *
 * <p>Berbeda dari kebanyakan model di paket ini, kelas ini {@code implements Serializable}
 * langsung -- BUKAN subclass {@link FileFoto} atau {@link ais.database.model.GeneralValueObject
 * GeneralValueObject} -- sehingga tidak mewarisi perilaku umum seperti cache berkas
 * {@code put}/{@code retreive} atau resolusi lazy-proxy Hibernate.</p>
 *
 * @see FileBukuBahanAjar
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Table(schema = "public", name = "file_buku_bahan_ajar_text")
public class FileBukuBahanAjarText implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/** @return id pengguna (String) yang mengunggah/mengubah baris ini, atau {@code null}. */
	public String getOlehId() {return olehId;}

	/** Menetapkan id pengunggah; nilai {@code null} atau kosong-setelah-trim diabaikan (field lama tidak ditimpa). */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}

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

	/** @return {@link #nama} apa adanya; bisa {@code null} bila belum diisi. */
	public String toString() {
		return nama;
	}

	private String nama;
	private String keterangan;
	private FileBukuBahanAjar fileBukuBahanAjar;
	private Integer halaman;
	private String isi;

	/** Konstruktor default (dipakai Hibernate). */
	public FileBukuBahanAjarText() {
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

	/** @return nama (di-trim), atau {@code null} bila belum diisi. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama baris ini. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan baris ini, atau {@code null}. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan baris ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return berkas {@link FileBukuBahanAjar} induk yang teksnya diekstrak pada baris ini. Kolom
	 *         {@code file_buku_bahan_ajar} WAJIB diisi ({@code nullable = false}) -- setiap baris
	 *         teks harus terkait ke satu berkas.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "file_buku_bahan_ajar", nullable = false)
	public FileBukuBahanAjar getFileBukuBahanAjar() {
		return fileBukuBahanAjar;
	}

	/** @param fileBukuBahanAjar berkas {@link FileBukuBahanAjar} induk baris ini. */
	public void setFileBukuBahanAjar(FileBukuBahanAjar fileBukuBahanAjar) {
		this.fileBukuBahanAjar = fileBukuBahanAjar;
	}

	/** @return nomor halaman dari berkas induk yang teksnya diekstrak pada baris ini. */
	public Integer getHalaman() {
		return halaman;
	}

	/** @param halaman nomor halaman dari berkas induk yang diekstrak. */
	public void setHalaman(Integer halaman) {
		this.halaman = halaman;
	}

	/** @return teks hasil ekstraksi/OCR halaman ini (kolom {@code text}), atau {@code null}. */
	@Column(name = "isi", columnDefinition = "text", nullable = true)
	public String getIsi() {
		return isi;
	}

	/** @param isi teks hasil ekstraksi/OCR halaman ini. */
	public void setIsi(String isi) {
		this.isi = isi;
	}

}
