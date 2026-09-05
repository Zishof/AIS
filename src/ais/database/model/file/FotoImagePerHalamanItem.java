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
 * Satu halaman hasil pemindaian (scan) digital sebuah {@link ais.database.model.library.Item Item}
 * pustaka -- tabel {@code foto_image_per_halaman_item}. Satu Item bisa memiliki banyak baris,
 * masing-masing mewakili satu halaman lewat kolom {@link #getHalaman()}/{@link
 * #getHalamanIndex()}, sehingga koleksi digitalnya bisa ditampilkan halaman-per-halaman (mis.
 * pembaca buku digital) alih-alih satu berkas utuh seperti {@link FotoItem}.
 *
 * <p><b>Urutan halaman.</b> {@link #getHalamanIndex()} mem-parse {@link #getHalaman()} (label
 * halaman berbentuk teks, mis. bisa memuat angka romawi atau non-numerik pada data lama) menjadi
 * {@code Double} untuk keperluan pengurutan; kegagalan parse (termasuk {@link #halaman} yang masih
 * {@code null}) ditangkap dan menghasilkan {@code 0.0}, BUKAN exception.</p>
 *
 * <p><b>Google Drive sebagai sumber alternatif.</b> {@link #getGdrive()}/{@link
 * #getGdriveUsername()} TIDAK dipetakan sebagai kolom JPA; nilainya disimpan lewat cache berkas
 * per-instance {@link ais.database.model.GeneralValueObject#put(String, String) put}/{@link
 * ais.database.model.GeneralValueObject#retreive(String) retreive} milik {@code
 * GeneralValueObject} induk. Selama {@link #getGdrive()} terisi, {@link #getFoto()} sengaja
 * mengembalikan {@code null} sebagai pertanda berkas asli harus diambil dari Google Drive.</p>
 *
 * <p><b>Baris "copy".</b> {@link #getCopyDari()} adalah asosiasi opsional ke baris
 * {@code FotoImagePerHalamanItem} lain; ketika terisi, {@link #getNama()} dan {@link #getFoto()}
 * membaca nilainya dari baris sumber tersebut -- pola berbagi satu berkas fisik di antara banyak
 * baris tanpa menduplikasi blob, sama seperti subclass {@link FileFoto} lain.</p>
 *
 * @see ais.database.model.library.Item
 * @see FotoItem
 * @see FileFoto
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "foto_image_per_halaman_item")

public class FotoImagePerHalamanItem extends FileFoto {
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
	private static final long serialVersionUID = 2463821577548439808L;
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

	/** @return {@link #nama} apa adanya; bisa {@code null} bila belum diisi. */
	public String toString() {
		return nama;
	}

	private String nama;
	private String keterangan;
	private Long item;
	private String halaman;
	private Double halamanIndex = 0.0;
	private Blob foto;
	private String content;
	private Boolean ditampilkan;
	private FotoImagePerHalamanItem copyDari;

	/** @return selalu {@code null}; kelas ini tidak membedakan "jenis" lampiran. */
	@Override
	public String ambilJenis() {
		return null;
	}

	/** Konstruktor default (dipakai Hibernate). */
	public FotoImagePerHalamanItem() {
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
	 * @return nama berkas halaman (di-trim), atau {@code null} bila belum diisi. Bila
	 *         {@link #copyDari} terisi, nilainya disegarkan lebih dulu dari nama baris sumber.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if (copyDari != null) {
			nama = copyDari.nama;
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama berkas halaman. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan halaman ini, atau {@code null}. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan halaman ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @param foto isi biner (gambar) hasil pemindaian halaman ini. */
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

	/** @param item id {@link ais.database.model.library.Item Item} pemilik halaman ini. */
	public void setItem(Long item) {
		this.item = item;
	}

	/**
	 * @return id {@link ais.database.model.library.Item Item} pemilik halaman ini (FK longgar,
	 *         tanpa {@code @ManyToOne}). Satu Item bisa memiliki banyak baris dengan nilai
	 *         {@link #getItem()} yang sama, satu per halaman.
	 */
	@Column(name = "item", nullable = true)
	public Long getItem() {
		return item;
	}

	/**
	 * @return label halaman (teks bebas, mis. bisa berisi angka romawi pada data lama). Bila kolom
	 *         database {@code null}, getter ini menuliskan balik default {@code "0"} ke field
	 *         in-memory sebelum mengembalikannya -- pola getter yang menyamarkan mutasi state.
	 */
	public String getHalaman() {
		if (halaman == null) {
			halaman = "0";
		}
		return halaman;
	}

	/** @param halaman label halaman (teks bebas). */
	public void setHalaman(String halaman) {
		this.halaman = halaman;
	}

	/** @return isi teks (OCR/catatan) terkait halaman ini (kolom {@code text}), atau {@code null}. */
	@Column(name = "content", nullable = true, columnDefinition = "text")
	public String getContent() {
		return content;
	}

	/** @param content isi teks (OCR/catatan) terkait halaman ini. */
	public void setContent(String content) {
		this.content = content;
	}

	/**
	 * @return penanda apakah halaman ini boleh ditampilkan; {@code null} pada kolom database
	 *         diperlakukan sama dengan {@code true} (default tampil), dan getter ini menuliskan
	 *         balik default tersebut ke field in-memory sebelum mengembalikannya.
	 */
	public Boolean getDitampilkan() {
		if (ditampilkan == null) {
			ditampilkan = true;
		}
		return ditampilkan;
	}

	/** @param ditampilkan penanda apakah halaman ini boleh ditampilkan. */
	public void setDitampilkan(Boolean ditampilkan) {
		this.ditampilkan = ditampilkan;
	}

	/**
	 * @return {@link #halaman} yang di-parse menjadi {@code Double} untuk keperluan pengurutan
	 *         numerik antar halaman. Membaca field {@link #halaman} LANGSUNG (bukan lewat {@link
	 *         #getHalaman()}), sehingga bila {@link #halaman} masih {@code null} pemanggilan
	 *         {@code halaman.trim()} melempar {@code NullPointerException} -- namun exception itu
	 *         (termasuk {@code NumberFormatException} untuk label non-numerik) ditangkap oleh blok
	 *         {@code catch} yang sama dan menghasilkan {@code 0.0}, sehingga secara perilaku tidak
	 *         pernah terlihat sebagai error oleh pemanggil. Getter ini menuliskan hasilnya kembali
	 *         ke field {@link #halamanIndex} sebagai efek samping, meskipun field itu tidak pernah
	 *         dipetakan sebagai kolom persistence.
	 */
	public Double getHalamanIndex() {
		try {
			halamanIndex = Double.parseDouble(halaman.trim());
		} catch (Exception e) {
			halamanIndex = 0.0;
		}
		return halamanIndex;
	}

	/** @param halamanIndex nilai indeks halaman; akan ditimpa pada pemanggilan {@link #getHalamanIndex()} berikutnya. */
	public void setHalamanIndex(Double halamanIndex) {
		this.halamanIndex = halamanIndex;
	}

	/**
	 * @return baris {@code FotoImagePerHalamanItem} sumber bila baris ini adalah "copy" yang
	 *         berbagi berkas fisik dengan baris lain; {@code null} bila baris ini berdiri sendiri.
	 *         {@code NotFoundAction.IGNORE} membuat asosiasi yang menunjuk baris yang sudah terhapus
	 *         diperlakukan sebagai {@code null}, bukan melempar exception.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@NotFound(action = NotFoundAction.IGNORE)
	@JoinColumn(name = "copy_dari", nullable = true)
	public FotoImagePerHalamanItem getCopyDari() {
		return copyDari;
	}

	/** @param copyDari baris sumber untuk berbagi berkas fisik (lihat {@link #getCopyDari()}). */
	public void setCopyDari(FotoImagePerHalamanItem copyDari) {
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
	 * Menetapkan URL Google Drive halaman ini. Nilai tidak kosong ditulis ke cache berkas
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

	/** @return {@link #getItem()}, id {@link ais.database.model.library.Item Item} pemilik halaman ini, dipakai sebagai referensi generik oleh {@link FileFoto}. */
	@Override
	public Long ambilRef() {
		// TODO Auto-generated method stub
		return getItem();
	}
}
