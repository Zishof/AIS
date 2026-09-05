package ais.database.model.sirs;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.library.Penyedia;

/**
 * Entitas harga beli item pada schema {@code sirs} (tabel
 * {@code harga_beli_item}). Mencatat harga beli {@link #getHargaBeli()}
 * untuk satu {@link ItemMedis} (relasi WAJIB, {@link #getItem()}) dari
 * satu {@link Penyedia}/vendor (relasi WAJIB, {@link #getPenyedia()}).
 *
 * <p>
 * TIDAK ADA HISTORI HARGA BERBASIS RENTANG TANGGAL: sudah diverifikasi
 * dari kode, kelas ini TIDAK punya field periode/tanggal berlaku (mis.
 * "berlaku mulai"/"berlaku sampai"). Setiap baris hanyalah SATU harga
 * "saat ini" untuk kombinasi item+vendor tertentu; riwayat perubahan
 * harga dari waktu ke waktu hanya bisa ditelusuri lewat tabel audit
 * Envers (anotasi {@code @Audited} di kelas ini), BUKAN lewat query
 * langsung terhadap {@code harga_beli_item} dengan filter tanggal. Bila
 * aplikasi butuh "harga yang berlaku pada tanggal X", itu harus
 * direkonstruksi dari tabel revisi Envers, bukan dari struktur kolom
 * kelas ini.
 * </p>
 *
 * <p>
 * CATATAN PENAMAAN: field internalnya bernama {@link #vendor}, namun
 * accessor publiknya disebut {@link #getPenyedia()}/
 * {@link #setPenyedia(Penyedia)} (bukan {@code getVendor}/
 * {@code setVendor}) — konsisten dengan nama kelas {@link Penyedia} yang
 * direferensikan, tapi tidak simetris dengan nama field/kolom
 * {@code vendor} itu sendiri.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "harga_beli_item")
public class HargaBeliItem extends GeneralValueObject {

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
	 * Representasi ringkas baris harga beli ini untuk tampilan/log.
	 *
	 * @return teks keterangan baris ini.
	 */
	public String toString() {
		return keterangan;
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

	private String keterangan;
	private ItemMedis item;
	private Double hargaBeli;
	private Penyedia vendor;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public HargaBeliItem() {
	}

	/**
	 * Primary key baris harga beli item, auto-increment (IDENTITY) dan
	 * diisi database.
	 *
	 * @return ID unik baris harga beli item ini.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID baris harga beli item.
	 *
	 * @param id ID baris harga beli item.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil keterangan bebas baris harga beli ini.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas baris harga beli ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengambil item medis yang harga belinya dicatat baris ini —
	 * relasi WAJIB ({@code nullable = false}).
	 *
	 * @return item medis terkait.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = false)
	public ItemMedis getItem() {
		item = check(item);
		return item;
	}

	/**
	 * Menetapkan item medis yang harga belinya dicatat baris ini.
	 *
	 * @param item item medis terkait.
	 */
	public void setItem(ItemMedis item) {
		this.item = item;
	}

	/**
	 * Mengambil harga beli yang dicatat baris ini apa adanya (tanpa
	 * fallback bila {@code null}).
	 *
	 * @return harga beli, atau {@code null} jika belum diisi.
	 */
	@Column(name = "harga_beli", nullable = true)
	public Double getHargaBeli() {
		return hargaBeli;
	}

	/**
	 * Menetapkan harga beli yang dicatat baris ini.
	 *
	 * @param hargaBeli nilai harga beli.
	 */
	public void setHargaBeli(Double hargaBeli) {
		this.hargaBeli = hargaBeli;
	}

	/**
	 * Menetapkan vendor (penyedia) sumber harga beli ini.
	 *
	 * @param vendor vendor/penyedia; parameter dan field diberi nama
	 *               {@code vendor}, meski nama method {@code setPenyedia}
	 *               — lihat catatan penamaan di javadoc kelas.
	 */
	public void setPenyedia(Penyedia vendor) {
		this.vendor = vendor;
	}

	/**
	 * Mengambil vendor (penyedia) sumber harga beli ini — relasi WAJIB
	 * ({@code nullable = false}). Setiap baris harga beli terikat pada
	 * SATU vendor tertentu; item medis yang sama bisa punya beberapa
	 * baris {@code HargaBeliItem} untuk vendor berbeda-beda.
	 *
	 * @return vendor/penyedia sumber harga beli ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "vendor", nullable = false)
	public Penyedia getPenyedia() {
		vendor = check(vendor);
		return vendor;
	}

}
