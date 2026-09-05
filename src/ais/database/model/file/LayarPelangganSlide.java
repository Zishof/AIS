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

/**
 * Satu gambar slideshow screensaver Layar Pelanggan (menu Konfigurasi tab baru).
 *
 * <p><b>Kenapa di database streaming, bukan utama.</b> Kolom {@code gambar} adalah
 * {@code Blob} -- SEMUA tabel file/blob di codebase ini WAJIB dipetakan lewat
 * {@code StreamingHibernateUtil}/{@code hibernate.streaming.cfg.xml} (database TERPISAH
 * dari {@code hibernate.cfg.xml} utama), pola yang SAMA persis dgn
 * {@link FotoGambarProduk}/{@link GaleriFotoImage}. Query yg menyentuh entity ini
 * TIDAK BOLEH lewat {@code HibernateUtil} biasa (akan gagal {@code SQLGrammarException
 * 42P01 relation does not exist} karena tabel ini memang tak ada di DB utama), dan
 * TIDAK BISA di-join lintas-DB dgn tabel {@code koperasi.toko} -- {@code tokoId} di
 * bawah SENGAJA {@code Long} lepas (bukan {@code @ManyToOne Toko}), bukan kelupaan.</p>
 *
 * <p><b>Lingkup tampil (dedup mesin).</b> {@code idMesin} null = tampil di SEMUA mesin
 * POS toko ybs; diisi (UUID dari {@code IdentitasMesin.instance.idMesin} sisi Flutter)
 * = HANYA tampil di mesin itu. Pola "null = lingkup lebih luas" SAMA dgn konvensi
 * {@code tokoId} nullable di {@code AturanDiskon} (lihat KantinHelper.diskonSimpan).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "layar_pelanggan_slide")
public class LayarPelangganSlide {

	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	private Blob gambar;
	private String namaFile;
	private Long tokoId;
	private String idMesin;
	private Integer urutan;
	private Boolean aktif;
	private Date tanggalUpload;

	/** @return primary key baris ini; kolom identity, tidak pernah di-{@code INSERT} manual. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/** @param id primary key baris ini. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return nama pengguna yang mengunggah/mengubah baris ini, atau {@code null}. */
	@Column(name = "oleh", nullable = true)
	public String getOleh() {
		return oleh;
	}

	/** Menetapkan nama pengunggah; nilai {@code null} atau kosong-setelah-trim diabaikan (field lama tidak ditimpa). */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/** @return id pengguna (String) yang mengunggah/mengubah baris ini, atau {@code null}. */
	@Column(name = "oleh_id", nullable = true)
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

	/**
	 * Callback JPA {@code @PreUpdate}: menandai timestamp perubahan lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} setiap baris ini
	 * di-{@code UPDATE}.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** @return waktu perubahan terakhir baris ini, diinisialisasi ke waktu sekarang saat objek dibuat. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dirubah", nullable = true)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @param tanggal_dirubah waktu perubahan terakhir baris ini; lihat {@link #onUpdate()}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @param gambar isi biner gambar slide. */
	public void setGambar(Blob gambar) {
		this.gambar = gambar;
	}

	/**
	 * @return isi biner gambar slide, atau {@code null} bila belum diisi. Tidak diaudit
	 *         ({@code @NotAudited}) karena isi biner tidak perlu dilacak riwayatnya oleh Envers.
	 *         Dipetakan lewat {@code StreamingHibernateUtil}/database streaming terpisah, BUKAN
	 *         {@code HibernateUtil} utama -- lihat Javadoc kelas.
	 */
	@NotAudited
	@Column(name = "gambar", nullable = true)
	public Blob getGambar() {
		return gambar;
	}

	/** @return nama berkas asli gambar slide, atau {@code null}. */
	@Column(name = "nama_file", nullable = true, length = 255)
	public String getNamaFile() {
		return namaFile;
	}

	/** @param namaFile nama berkas asli gambar slide. */
	public void setNamaFile(String namaFile) {
		this.namaFile = namaFile;
	}

	/**
	 * @return id toko ({@code koperasi.toko}) pemilik slide ini, atau {@code null}. Sengaja
	 *         {@code Long} lepas (bukan {@code @ManyToOne Toko}) karena entity ini berada di
	 *         database streaming terpisah dan tidak bisa di-join lintas-database dengan tabel
	 *         {@code toko} -- lihat Javadoc kelas.
	 */
	@Column(name = "toko_id", nullable = true)
	public Long getTokoId() {
		return tokoId;
	}

	/** @param tokoId id toko ({@code koperasi.toko}) pemilik slide ini. */
	public void setTokoId(Long tokoId) {
		this.tokoId = tokoId;
	}

	/**
	 * @return UUID mesin POS (sisi Flutter, {@code IdentitasMesin.instance.idMesin}) yang
	 *         menjadi lingkup tampil slide ini, atau {@code null} bila slide tampil di SEMUA
	 *         mesin milik {@link #getTokoId() toko} ybs -- lihat Javadoc kelas untuk konvensi
	 *         "null = lingkup lebih luas".
	 */
	@Column(name = "id_mesin", nullable = true, length = 100)
	public String getIdMesin() {
		return idMesin;
	}

	/** @param idMesin UUID mesin POS yang menjadi lingkup tampil slide ini; {@code null} untuk semua mesin. */
	public void setIdMesin(String idMesin) {
		this.idMesin = idMesin;
	}

	/** @return urutan tampil slide ini dalam slideshow; {@code 0} bila kolom database {@code null}. */
	@Column(name = "urutan", nullable = true)
	public Integer getUrutan() {
		return urutan == null ? 0 : urutan;
	}

	/** @param urutan urutan tampil slide ini dalam slideshow. */
	public void setUrutan(Integer urutan) {
		this.urutan = urutan;
	}

	/** @return penanda apakah slide ini aktif ditampilkan; {@code true} bila kolom database {@code null} (default aktif). */
	@Column(name = "aktif", nullable = true)
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif penanda apakah slide ini aktif ditampilkan. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** @return waktu unggah gambar slide ini, atau {@code null}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_upload", nullable = true)
	public Date getTanggalUpload() {
		return tanggalUpload;
	}

	/** @param tanggalUpload waktu unggah gambar slide ini. */
	public void setTanggalUpload(Date tanggalUpload) {
		this.tanggalUpload = tanggalUpload;
	}
}
