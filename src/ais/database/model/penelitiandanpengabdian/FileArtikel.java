package ais.database.model.penelitiandanpengabdian;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

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



import ais.database.model.GeneralValueObject;

/**
 * Entitas lampiran berkas artikel (tabel {@code penelitiandanpengabdian.file_artikel}) yang
 * menautkan satu berkas unggahan ke satu {@link Artikel} lewat kolom {@link #artikel}
 * ({@code artikel}) langsung — <b>bukan</b> lewat paket lampiran generik ({@code file}/
 * {@code lampiran}) yang dipakai modul lain di aplikasi ini. Satu artikel dapat memiliki berapa
 * pun baris {@code FileArtikel} (multi-berkas: naskah, bukti submit, sertifikat, dsb.), masing-
 * masing menyimpan lokasi fisik ({@link #path}), nama tampil ({@link #fileName}), tipe MIME
 * ({@link #mimeType}), keterangan, dan waktu unggah ({@link #uploadDate}).
 *
 * <p>Bidang {@link #repoBitstreamId} menyimpan id bitstream (berkas) pada sistem repositori
 * eksternal (mis. saat artikel induknya diekspor/didaftarkan ke repositori tersebut), sejalan
 * dengan {@code repoItemId} pada {@link Artikel} dan {@code repoContributorId} pada
 * {@link AnggotaArtikel}.</p>
 *
 * <h2>Bidang audit bayangan</h2>
 * <p>{@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} beserta {@link #onUpdate()} adalah
 * keharusan teknis agar {@code AuditTimestampInterceptor} dapat bekerja, bukan duplikasi yang bisa
 * dihapus. Setternya sengaja mengabaikan masukan kosong agar jejak audit yang sudah ada tidak
 * tertimpa string kosong dari jalur salin/klon objek.</p>
 *
 * @see Artikel
 * @see AnggotaArtikel
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Table(schema = "penelitiandanpengabdian", name = "file_artikel")



public class FileArtikel extends GeneralValueObject {
	/** Id bitstream (berkas) pada sistem repositori eksternal tempat berkas ini diarsipkan. */
	private Long repoBitstreamId;

	/**
	 * Penanda versi serialisasi Java. Nilai warisan cetakan hbm2java; jangan diubah tanpa alasan.
	 */
	private static final long serialVersionUID = 2463812577548439808L;
	/** Kunci utama basis data, dibangkitkan {@code IDENTITY}; {@code null} selama baris belum tersimpan. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi {@code AuditTimestampInterceptor}, bukan oleh form. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; pasangan teknis dari {@link #oleh}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris lampiran ini.
	 *
	 * @return id pengguna terakhir, atau {@code null} bila belum pernah diubah lewat jalur yang
	 *         memasang interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna yang terakhir mengubah baris lampiran ini.
	 *
	 * <p><b>Setter defensif:</b> masukan {@code null} atau yang hanya berisi spasi diabaikan
	 * diam-diam sehingga nilai lama dipertahankan.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna yang terakhir mengubah baris lampiran ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, masukan {@code null}/kosong diabaikan.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris lampiran ini.
	 *
	 * @return nama pengguna terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate} yang mendelegasikan pencatatan audit ke
	 * {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum baris diperbarui. Interceptor-lah
	 * yang mengisi {@link #oleh}, {@link #olehId}, dan {@link #getTanggal_dirubah()} dari konteks
	 * pengguna aktif. Method sengaja {@code protected} dan tidak boleh dipanggil manual.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Umumnya dipanggil {@code AuditTimestampInterceptor},
	 * bukan oleh form.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris lampiran ini.
	 *
	 * @return stempel waktu perubahan terakhir, dapat {@code null} bila belum pernah diubah lewat
	 *         jalur yang memasang interceptor audit dan field belum diinisialisasi manual
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat gabungan nama berkas dan tipe MIME, dipakai label bawaan komponen
	 * ZK dan penelusuran log.
	 *
	 * @return {@code "<fileName>_<mimeType>"}; bagian yang {@code null} tampil sebagai teks
	 *         {@code "null"} bawaan concatenation Java
	 */
	public String toString() {
		return fileName + "_" + mimeType;
	}

	/** Lokasi fisik berkas di penyimpanan server. */
	private String path;
	/** Tipe MIME berkas (mis. {@code application/pdf}). */
	private String mimeType;
	/** Nama tampil berkas, sesuai nama asal saat diunggah. */
	private String fileName;
	/** Keterangan mengenai isi/peran berkas ini (mis. "naskah final", "bukti submit"). */
	private String keterangan;
	/** Artikel induk yang memiliki lampiran berkas ini. */
	private Artikel artikel;
	/** Waktu unggah berkas; diinisialisasi ke waktu saat ini pada pembuatan objek, dapat ditimpa lewat {@link #setUploadDate(Date)}. */
	private Date uploadDate = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public FileArtikel() {
	}
	/**
	 * Mengembalikan id bitstream (berkas) pada sistem repositori eksternal.
	 *
	 * @return id bitstream repositori, atau {@code null} bila belum diarsipkan
	 */
	@Column(name="repo_bitstream_id") public Long getRepoBitstreamId(){return repoBitstreamId;}
	/**
	 * Menyetel id bitstream (berkas) pada sistem repositori eksternal.
	 *
	 * @param v id bitstream repositori baru
	 */
	public void setRepoBitstreamId(Long v){repoBitstreamId=v;}

	/**
	 * Mengembalikan kunci utama baris lampiran ini.
	 *
	 * @return id baris lampiran, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris lampiran ini. Hanya untuk kebutuhan Hibernate dan penyalinan
	 * objek.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan keterangan mengenai isi/peran berkas ini.
	 *
	 * @return keterangan, dapat {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", length = 1000)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan mengenai isi/peran berkas ini.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menyetel tipe MIME berkas.
	 *
	 * @param mimeType tipe MIME baru (mis. {@code application/pdf})
	 */
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	/**
	 * Mengembalikan tipe MIME berkas.
	 *
	 * @return tipe MIME, dapat {@code null} bila belum diisi
	 */
	@Column(name = "mime_type", length = 255)
	public String getMimeType() {
		return mimeType;
	}

	/**
	 * Menyetel nama tampil berkas.
	 *
	 * @param fileName nama berkas baru
	 */
	public void setNama(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * Mengembalikan nama tampil berkas.
	 *
	 * @return nama berkas, dapat {@code null} bila belum diisi
	 */
	@Column(name = "file_name", length = 255)
	public String getNama() {
		return fileName;
	}

	/**
	 * Menyetel artikel induk yang memiliki lampiran berkas ini.
	 *
	 * @param artikel artikel induk baru
	 */
	public void setArtikel(Artikel artikel) {
		this.artikel = artikel;
	}

	/**
	 * Mengembalikan artikel induk yang memiliki lampiran berkas ini. Dimuat dengan
	 * {@link FetchMode#SELECT}. Kolom relasi ini {@code nullable = false} — setiap baris lampiran
	 * wajib menunjuk satu artikel; inilah yang membuat entitas ini bertaut langsung ke
	 * {@link Artikel} alih-alih lewat paket lampiran generik.
	 *
	 * @return artikel induk
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "artikel", nullable = false)
	public Artikel getArtikel() {
		return artikel;
	}

	/**
	 * Mengembalikan waktu unggah berkas ini.
	 *
	 * @return waktu unggah; tidak pernah {@code null} untuk objek yang baru dibuat di memori
	 *         karena diinisialisasi dari {@code WaktuUtil.getDate()}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_upload", nullable = false, length = 0)
	public Date getUploadDate() {
		return uploadDate;
	}

	/**
	 * Menyetel waktu unggah berkas ini.
	 *
	 * @param uploadDate waktu unggah baru
	 */
	public void setUploadDate(Date uploadDate) {
		this.uploadDate = uploadDate;
	}

	/**
	 * Mengembalikan lokasi fisik berkas di penyimpanan server.
	 *
	 * @return path berkas, dapat {@code null} bila belum diisi
	 */
	@Column(name = "path", length = 1000)
	public String getPath() {
		return path;
	}

	/**
	 * Menyetel lokasi fisik berkas di penyimpanan server.
	 *
	 * @param path path berkas baru
	 */
	public void setPath(String path) {
		this.path = path;
	}

}
