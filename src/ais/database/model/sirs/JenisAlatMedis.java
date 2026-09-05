package ais.database.model.sirs;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Entitas lookup jenis alat medis pada schema {@code sirs} (tabel
 * {@code jenis_alat_medis}). Dipakai lewat relasi opsional
 * {@link AlatMedis#getJenisAlatMedis()} untuk mengklasifikasikan basis
 * tarif alat medis — konstanta {@link #JENIS_TARIF_KAMAR_DAN_RUANGAN} dan
 * {@link #JENIS_TARIF_BED} menunjukkan nilai {@code nama} yang lazim
 * dipakai untuk baris data ini (tarif dihitung dari kamar/ruangan vs
 * tarif dihitung dari tempat tidur).
 *
 * <p>
 * PENTING: field String {@link AlatMedis#getJenis()} milik kelas
 * {@link AlatMedis} sendiri (dengan konstanta
 * {@link AlatMedis#JENIS_TEMPAT_TIDUR}/{@link AlatMedis#JENIS_UMUM})
 * adalah konsep TERPISAH dari entitas lookup ini — keduanya kebetulan
 * sama-sama disebut "jenis" dan sama-sama berbicara soal tempat
 * tidur/umum vs kamar-ruangan/bed, tetapi disimpan di kolom database yang
 * berbeda ({@code jenis} sebagai field String langsung di
 * {@code alat_medis}, vs {@code jenis_alat_medis} sebagai foreign key ke
 * tabel ini) dan tidak saling mereferensikan. Sudah diverifikasi dari
 * kode, bukan diasumsikan dari kemiripan nama.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "jenis_alat_medis")
public class JenisAlatMedis extends GeneralValueObject {

	/**
	 * Nilai {@code nama} lazim untuk baris jenis alat medis yang
	 * tarifnya dihitung berdasarkan kamar dan ruangan (bukan per unit
	 * tempat tidur).
	 */
	public static final String JENIS_TARIF_KAMAR_DAN_RUANGAN = "Tarif Kamar dan Ruangan";
	/**
	 * Nilai {@code nama} lazim untuk baris jenis alat medis yang
	 * tarifnya dihitung per tempat tidur (BED), berlawanan dengan
	 * {@link #JENIS_TARIF_KAMAR_DAN_RUANGAN}.
	 */
	public static final String JENIS_TARIF_BED = "Tarif Tempat Tidur (BED)";

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String olehId;

	/**
	 * Mengambil ID pengguna yang terakhir mengubah baris ini. Field
	 * audit shadow yang diisi lewat {@link #setOlehId(String)}.
	 *
	 * @return ID pengguna terakhir yang mengubah, atau {@code null} jika
	 *         belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna yang mengubah baris ini. Nilai kosong/blank
	 * SENGAJA diabaikan (early return) agar field audit ini tidak pernah
	 * ditimpa jadi kosong — pola field audit shadow yang merupakan
	 * KEHARUSAN TEKNIS berulang di paket model AIS, bukan bug.
	 *
	 * @param olehId ID pengguna; nilai {@code null} atau string
	 *               kosong/whitespace-saja tidak akan mengubah nilai
	 *               tersimpan.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	private String oleh;

	/**
	 * Representasi ringkas jenis alat medis ini untuk tampilan/log.
	 *
	 * @return nama jenis alat medis.
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Menetapkan nama pengguna yang mengubah baris ini. Nilai kosong/blank
	 * diabaikan agar field audit ini tidak pernah ditimpa kosong.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau
	 *             kosong/whitespace-saja tidak akan mengubah nilai
	 *             tersimpan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna terakhir yang mengubah, atau {@code null} jika
	 *         belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate} yang memperbarui {@link #tanggal_dirubah}
	 * otomatis sebelum baris ini di-UPDATE, lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = new Date();

	/**
	 * Menetapkan timestamp perubahan terakhir baris ini secara manual.
	 *
	 * @param tanggal_dirubah timestamp perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil timestamp perubahan terakhir baris ini, diperbarui
	 * otomatis oleh {@link #onUpdate()} setiap kali baris di-UPDATE.
	 *
	 * @return timestamp perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	private String nama;
	private String keterangan;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public JenisAlatMedis() {
	}

	/**
	 * Konstruktor pintasan untuk membuat referensi ke baris jenis alat
	 * medis yang sudah ada berdasarkan ID-nya saja, tanpa perlu memuat
	 * seluruh baris dari database.
	 *
	 * @param id ID jenis alat medis yang sudah ada.
	 */
	public JenisAlatMedis(Long id) {
		this.id = id;
	}

	/**
	 * Primary key baris jenis alat medis, auto-increment (IDENTITY) dan
	 * diisi database.
	 *
	 * @return ID unik jenis alat medis ini.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID jenis alat medis.
	 *
	 * @param id ID jenis alat medis.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil nama jenis alat medis. Kolom {@code nama} bersifat
	 * {@code nullable = true} — berbeda dari kebanyakan lookup lain di
	 * paket ini, nama boleh kosong.
	 *
	 * @return nama jenis alat medis, atau {@code null} jika belum diisi.
	 */
	@Column(name = "nama", nullable = true, length = 250)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menetapkan nama jenis alat medis.
	 *
	 * @param nama nama jenis alat medis, mis. {@link #JENIS_TARIF_KAMAR_DAN_RUANGAN}
	 *             atau {@link #JENIS_TARIF_BED}.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil keterangan bebas jenis alat medis ini.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas jenis alat medis ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

}
