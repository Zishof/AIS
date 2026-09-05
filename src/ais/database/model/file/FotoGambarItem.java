package ais.database.model.file;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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
 * Foto sampul (cover) satu {@link ais.database.model.library.Item Item} pustaka -- tabel
 * {@code foto_gambar_item}, satu baris per item lewat kolom {@link #getItem()}. Disajikan ke
 * klien lewat endpoint generik {@code /AmbilMedia?...&clazz=ais.database.model.file.FotoGambarItem
 * &property=item} (lihat {@code Item.getImageUrlBesar()}), sehingga item pustaka tetap punya
 * sampul yang bisa ditampilkan walau tidak bergantung host Google Books eksternal.
 *
 * <p><b>Google Drive sebagai sumber alternatif.</b> {@link #getGdrive()}/{@link
 * #getGdriveUsername()} TIDAK dipetakan sebagai kolom JPA; nilainya disimpan lewat cache berkas
 * per-instance {@link ais.database.model.GeneralValueObject#put(String, String) put}/{@link
 * ais.database.model.GeneralValueObject#retreive(String) retreive} milik {@code
 * GeneralValueObject} induk. Selama {@link #getGdrive()} terisi, {@link #getFoto()} sengaja
 * mengembalikan {@code null} -- pertanda bagi pemanggil bahwa berkas asli harus diambil dari URL
 * Google Drive tersebut, bukan dari blob kolom {@code foto}.</p>
 *
 * <p><b>Baris "copy".</b> {@link #getCopyDari()} adalah asosiasi opsional ke baris
 * {@code FotoGambarItem} lain; ketika terisi, {@link #getNama()}, {@link #getKeterangan()}, {@link
 * #getFoto()}, dan {@link #getGdrive()} membaca nilainya dari baris sumber tersebut, bukan dari
 * field baris ini sendiri -- pola berbagi satu berkas fisik/URL di antara banyak baris tanpa
 * menduplikasi blob, sama seperti subclass {@link FileFoto} lain (lihat penjelasan rantai
 * {@code copyDari} pada {@link FileFoto#berkasMilikBarisIni(java.io.File)}).</p>
 *
 * @see ais.database.model.library.Item
 * @see FileFoto
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "foto_gambar_item")

public class FotoGambarItem extends FileFoto {
	/**
	 * Path/lokasi penyimpanan lokal baris ini. Field ini MENIMPA (shadow) field privat sejenis di
	 * {@link FileFoto}: kolom ini tidak diberi anotasi JPA ({@code @Column}) sehingga bukan kolom
	 * ter-mapping -- getter/setter di sini hanya menyediakan state in-memory milik baris, terpisah
	 * dari implementasi induk yang juga privat dan tidak bisa diwarisi langsung.
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
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/** @return id pengguna (String) yang mengunggah/mengubah baris ini, atau {@code null}. */
	public String getOlehId() {
		return olehId;
	}

	/** Menetapkan id pengunggah; nilai {@code null} atau kosong-setelah-trim diabaikan (field lama tidak ditimpa). */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/** Menetapkan nama pengunggah; nilai {@code null} atau kosong-setelah-trim diabaikan (field lama tidak ditimpa). */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
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
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah waktu perubahan terakhir baris ini; lihat {@link #onUpdate()}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini, diinisialisasi ke waktu sekarang saat objek dibuat. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return {@link #getNama()}, atau string kosong bila nama {@code null}. */
	public String toString() {
		return getNama() == null ? "" : getNama();
	}

	/**
	 * Callback JPA {@code @PrePersist}: mengisi default sebelum baris pertama kali disimpan --
	 * {@link #nama} menjadi {@code "Gambar Item"} bila kosong, {@link #keterangan} menjadi string
	 * kosong bila {@code null}.
	 */
	@javax.persistence.PrePersist
	protected void onCreate() {
		if (nama == null || nama.trim().isEmpty()) {
			nama = "Gambar Item";
		}
		if (keterangan == null) {
			keterangan = "";
		}
	}

	private String nama;
	private String keterangan;
	private Long item;
	private Blob foto;
	private String path;
	private FotoGambarItem copyDari;

	/** @return selalu {@code null}; kelas ini tidak membedakan "jenis" lampiran. */
	@Override
	public String ambilJenis() {
		return null;
	}

	/** Konstruktor default (dipakai Hibernate). */
	public FotoGambarItem() {
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
	 * @return nama berkas sampul. Bila {@link #copyDari} terisi, nilainya disegarkan lebih dulu dari
	 *         nama baris sumber; kosong (setelah trim) selalu digantikan default {@code "Gambar
	 *         Item"} -- konsisten dengan default yang diisi {@link #onCreate()}.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if (copyDari != null) {
			nama = copyDari.nama;
		}
		return this.nama == null || this.nama.trim().isEmpty() ? "Gambar Item" : this.nama.trim();
	}

	/** @param nama nama berkas sampul. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return keterangan sampul. Bila {@link #copyDari} terisi, nilainya disegarkan lebih dulu dari
	 *         keterangan baris sumber.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		if (copyDari != null) {
			keterangan = copyDari.keterangan;
		}
		return this.keterangan;
	}

	/** @param keterangan keterangan sampul. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @param foto isi biner berkas sampul. */
	public void setFoto(Blob foto) {
		this.foto = foto;
	}

	/**
	 * @return {@code null} bila {@link #getGdrive()} terisi (berkas asli ada di Google Drive, bukan
	 *         di kolom ini); jika tidak, blob milik {@link #copyDari} bila terisi, atau blob baris
	 *         ini sendiri. Tidak diaudit ({@code @NotAudited}) karena isi biner tidak perlu dilacak
	 *         riwayatnya oleh Envers.
	 */
	@NotAudited
	public Blob getFoto() {
		return gdrive != null && !gdrive.trim().isEmpty() ? null : (copyDari == null ? foto : copyDari.foto);
	}

	/** @param item id {@link ais.database.model.library.Item Item} pemilik sampul ini. */
	public void setItem(Long item) {
		this.item = item;
	}

	/** @return id {@link ais.database.model.library.Item Item} pemilik sampul ini (FK longgar, tanpa {@code @ManyToOne}). */
	@Column(name = "item", nullable = true)
	public Long getItem() {
		return item;
	}

	/** @return {@link #path}, path cache tambahan (bukan kolom database). */
	public String getPath() {
		return path;
	}

	/** @param path path cache tambahan untuk baris ini. */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * @return baris {@code FotoGambarItem} sumber bila baris ini adalah "copy" yang berbagi berkas
	 *         fisik/URL dengan baris lain; {@code null} bila baris ini berdiri sendiri.
	 *         {@code NotFoundAction.IGNORE} membuat asosiasi yang menunjuk baris yang sudah terhapus
	 *         diperlakukan sebagai {@code null}, bukan melempar exception.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@NotFound(action = NotFoundAction.IGNORE)
	@JoinColumn(name = "copy_dari", nullable = true)
	public FotoGambarItem getCopyDari() {
		return copyDari;
	}

	/** @param copyDari baris sumber untuk berbagi berkas fisik/URL (lihat {@link #getCopyDari()}). */
	public void setCopyDari(FotoGambarItem copyDari) {
		this.copyDari = copyDari;
	}

	private String gdrive;
	private String gdriveUsername;

	/**
	 * @return URL Google Drive tempat berkas sampul sesungguhnya disimpan, atau {@code null} bila
	 *         berkas disimpan sebagai blob biasa. Bukan kolom JPA -- nilainya dibaca dari cache
	 *         berkas per-instance {@link ais.database.model.GeneralValueObject#retreive(String)
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
	 * Menetapkan URL Google Drive sampul. Nilai tidak kosong ditulis ke cache berkas per-instance
	 * lewat {@link ais.database.model.GeneralValueObject#put(String, String) put(gdrive,
	 * "gdrive")} milik induk -- BUKAN ke kolom database.
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

	/** @return {@link #getItem()}, id {@link ais.database.model.library.Item Item} pemilik sampul ini, dipakai sebagai referensi generik oleh {@link FileFoto}. */
	@Override
	public Long ambilRef() {
		// TODO Auto-generated method stub
		return getItem();
	}
}
