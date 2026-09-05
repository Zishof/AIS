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

/**
 * Entitas bahan baku item pada schema {@code sirs} (tabel
 * {@code bahan_baku_item}). Merepresentasikan hubungan komposisi/resep
 * (bill of materials) ANTAR dua {@link ItemMedis}: {@link #getItem()}
 * adalah bahan baku (komponen), {@link #getItemInduk()} adalah item induk
 * (komposit/hasil racikan) yang memakainya, dan {@link #getQty()} adalah
 * jumlah bahan baku tersebut yang dibutuhkan untuk membentuk satu unit
 * item induk. Kedua sisi relasi sama-sama menunjuk ke {@link ItemMedis},
 * sehingga struktur ini pada dasarnya self-referencing lewat tabel
 * penghubung ini (mis. dipakai untuk racikan obat: item induk = obat
 * racikan, item = bahan baku penyusunnya beserta takarannya).
 *
 * <p>
 * Baik {@link #getItem()} maupun {@link #getItemInduk()} adalah relasi
 * OPSIONAL ({@code nullable = true}) — kolom database tidak memaksakan
 * keduanya wajib terisi bersamaan, sehingga validasi bahwa satu baris
 * bahan baku harus punya kedua sisi terisi (bila memang itu aturan
 * bisnisnya) perlu dijaga di kode pemanggil, bukan di level entitas ini.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "bahan_baku_item")
public class BahanBakuItem extends GeneralValueObject {

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
	 * Representasi ringkas baris bahan baku ini untuk tampilan/log.
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
	private Double qty;
	private ItemMedis item;
	private ItemMedis itemInduk;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public BahanBakuItem() {
	}

	/**
	 * Primary key baris bahan baku item, auto-increment (IDENTITY) dan
	 * diisi database.
	 *
	 * @return ID unik baris bahan baku item ini.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID baris bahan baku item.
	 *
	 * @param id ID baris bahan baku item.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil keterangan bebas baris bahan baku ini.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas baris bahan baku ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan item medis yang berperan sebagai bahan baku (komponen)
	 * pada baris ini.
	 *
	 * @param item item medis bahan baku.
	 */
	public void setItem(ItemMedis item) {
		this.item = item;
	}

	/**
	 * Mengambil item medis yang berperan sebagai bahan baku (komponen)
	 * pada baris ini — relasi OPSIONAL ke {@link ItemMedis}, sisi
	 * "komponen" dari hubungan komposisi. Lihat {@link #getItemInduk()}
	 * untuk sisi "induk"-nya.
	 *
	 * @return item medis bahan baku, atau {@code null} jika belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = true)
	public ItemMedis getItem() {
		item = check(item);
		return item;
	}

	/**
	 * Menetapkan item medis induk (komposit/hasil racikan) yang memakai
	 * bahan baku pada baris ini.
	 *
	 * @param itemInduk item medis induk.
	 */
	public void setItemInduk(ItemMedis itemInduk) {
		this.itemInduk = itemInduk;
	}

	/**
	 * Mengambil item medis induk (komposit/hasil racikan) pada baris ini
	 * — relasi OPSIONAL ke {@link ItemMedis}, sisi "induk" dari hubungan
	 * komposisi, BERBEDA dari {@link #getItem()} (sisi "komponen") meski
	 * tipe relasinya sama-sama {@link ItemMedis}.
	 *
	 * @return item medis induk, atau {@code null} jika belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item_induk", nullable = true)
	public ItemMedis getItemInduk() {
		itemInduk = check(itemInduk);
		return itemInduk;
	}

	/**
	 * Menetapkan jumlah (qty) bahan baku {@link #getItem()} yang
	 * dibutuhkan untuk membentuk satu unit {@link #getItemInduk()}.
	 *
	 * @param qty jumlah kebutuhan bahan baku.
	 */
	public void setQty(Double qty) {
		this.qty = qty;
	}

	/**
	 * Mengambil jumlah (qty) bahan baku yang dibutuhkan untuk membentuk
	 * satu unit item induk, apa adanya tanpa fallback bila {@code null}.
	 *
	 * @return jumlah kebutuhan bahan baku, atau {@code null} jika belum
	 *         diisi.
	 */
	public Double getQty() {
		return qty;
	}

}
