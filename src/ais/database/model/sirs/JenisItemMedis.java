package ais.database.model.sirs;

// Generated Apr 12, 2010 1:48:52 AM by Hibernate Tools 3.2.4.CR1

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
 * Entitas lookup jenis item medis pada schema {@code sirs} (tabel
 * {@code jenis_barang_medis}). Ini adalah dimensi klasifikasi WAJIB untuk
 * {@link ItemMedis} (lihat {@link ItemMedis#getJenisItem()}), membedakan
 * apakah suatu item medis tergolong OBAT atau BAHAN_MEDIS (BHP/alkes),
 * lewat baris data yang idnya sudah ditetapkan oleh konstanta
 * {@link #OBAT} dan {@link #BAHAN_MEDIS} di kelas ini (bukan sekadar
 * dua baris lookup generik).
 *
 * <p>
 * Kelas ini adalah tabel lookup DATAR: hanya berisi kode/nama/keterangan
 * tanpa relasi ke {@link KelompokItem}, {@link KelasItem}, maupun
 * {@link GenerikItem} — keempat dimensi klasifikasi item medis
 * (jenis/kelompok/kelas/generik) sepenuhnya independen satu sama lain,
 * diverifikasi langsung dari kode masing-masing kelas.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "jenis_barang_medis")
public class JenisItemMedis extends GeneralValueObject {

	/**
	 * ID baris data tetap (bukan dinamis) yang merepresentasikan jenis
	 * item "Obat". Dipakai kode pemanggil untuk membandingkan
	 * {@link ItemMedis#getJenisItem()} tanpa perlu query berdasarkan nama.
	 */
	public static final Long OBAT = 15L;
	/**
	 * ID baris data tetap yang merepresentasikan jenis item "Bahan
	 * Medis" (BHP/alat kesehatan habis pakai), berlawanan dengan
	 * {@link #OBAT}.
	 */
	public static final Long BAHAN_MEDIS = 16L;

	/**
	 *
	 */
	private static final long serialVersionUID = -3088213612931036389L;
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
	 * Representasi ringkas jenis item medis ini untuk tampilan/log.
	 *
	 * @return nama jenis item medis.
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
	private String kode;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public JenisItemMedis() {
	}

	/**
	 * Konstruktor pintasan untuk membuat referensi ke baris jenis item
	 * medis yang sudah ada berdasarkan ID-nya saja (mis. untuk dipakai
	 * langsung sebagai nilai FK {@link ItemMedis#setJenisItem(JenisItemMedis)}
	 * tanpa perlu memuat seluruh baris dari database), sering dipakai
	 * bersama konstanta {@link #OBAT}/{@link #BAHAN_MEDIS}.
	 *
	 * @param id ID jenis item medis yang sudah ada.
	 */
	public JenisItemMedis(Long id) {
		this.id = id;
	}

	/**
	 * Primary key baris jenis item medis, auto-increment (IDENTITY) dan
	 * diisi database.
	 *
	 * @return ID unik jenis item medis ini.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID jenis item medis.
	 *
	 * @param id ID jenis item medis.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Menetapkan nama jenis item medis.
	 *
	 * @param nama nama jenis item medis (mis. "Obat", "Bahan Medis").
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil nama jenis item medis.
	 *
	 * @return nama jenis item medis.
	 */
	public String getNama() {
		return nama;
	}

	/**
	 * Menetapkan kode jenis item medis.
	 *
	 * @param kode kode unik jenis item medis.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengambil kode jenis item medis. Kolom {@code kode} bersifat
	 * {@code unique} dan {@code nullable = false} di database.
	 *
	 * @return kode jenis item medis.
	 */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return kode;
	}

}
