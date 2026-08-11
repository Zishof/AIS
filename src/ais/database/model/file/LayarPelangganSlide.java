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

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "oleh", nullable = true)
	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	@Column(name = "oleh_id", nullable = true)
	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dirubah", nullable = true)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	public void setGambar(Blob gambar) {
		this.gambar = gambar;
	}

	@NotAudited
	@Column(name = "gambar", nullable = true)
	public Blob getGambar() {
		return gambar;
	}

	@Column(name = "nama_file", nullable = true, length = 255)
	public String getNamaFile() {
		return namaFile;
	}

	public void setNamaFile(String namaFile) {
		this.namaFile = namaFile;
	}

	@Column(name = "toko_id", nullable = true)
	public Long getTokoId() {
		return tokoId;
	}

	public void setTokoId(Long tokoId) {
		this.tokoId = tokoId;
	}

	@Column(name = "id_mesin", nullable = true, length = 100)
	public String getIdMesin() {
		return idMesin;
	}

	public void setIdMesin(String idMesin) {
		this.idMesin = idMesin;
	}

	@Column(name = "urutan", nullable = true)
	public Integer getUrutan() {
		return urutan == null ? 0 : urutan;
	}

	public void setUrutan(Integer urutan) {
		this.urutan = urutan;
	}

	@Column(name = "aktif", nullable = true)
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_upload", nullable = true)
	public Date getTanggalUpload() {
		return tanggalUpload;
	}

	public void setTanggalUpload(Date tanggalUpload) {
		this.tanggalUpload = tanggalUpload;
	}
}
