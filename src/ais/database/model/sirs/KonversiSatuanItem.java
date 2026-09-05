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
 * Entitas konversi satuan item pada schema {@code sirs} (tabel
 * {@code konversi_satuan_item}). Mendefinisikan faktor konversi
 * ({@link #getNilaiPersamaan()}) antara dua {@link SatuanItem} penuh —
 * {@link #getSatuanDari()} dan {@link #getSatuanMenjadi()} — secara
 * OPSIONAL diikat ke satu {@link ItemMedis} tertentu lewat
 * {@link #getItem()} (relasi {@code nullable = true}, jadi baris konversi
 * bisa dibuat generik tanpa terikat item spesifik).
 *
 * <p>
 * MEKANISME KONVERSI KEDUA: kelas ini adalah mekanisme konversi satuan
 * yang TERPISAH dari pasangan {@code namaAwal}/{@code nama}/{@code jumlah}
 * yang sudah melekat pada setiap baris {@link SatuanItem} itu sendiri —
 * lihat javadoc kelas {@link SatuanItem} untuk penjelasan tumpang tindih
 * struktural antara kedua mekanisme ini (belum diverifikasi apakah
 * keduanya disinkronkan atau dipakai untuk kasus yang benar-benar
 * berbeda; perlu penelusuran lebih lanjut di kode pemanggil bila
 * diperlukan kepastian).
 * </p>
 *
 * <p>
 * SUDAH DIVERIFIKASI, TIDAK ADA BUG "reset rasio tak valid jadi 1.0
 * sebelum validasi": berbeda dari pola yang pernah ditemukan di
 * {@code koperasi.SatuanProduk.getRasio()} (yang menormalkan nilai
 * tidak valid menjadi {@code 1.0} DI DALAM getter, sebelum validasi
 * sempat berjalan), getter {@link #getNilaiPersamaan()} di kelas ini
 * HANYA mengembalikan field apa adanya tanpa mutasi/normalisasi apapun.
 * Nilai {@code 1.0} di sini SEKADAR nilai inisialisasi default field
 * saat objek baru dibuat (dan dipakai form tambah/renderer sebagai
 * fallback tampilan bila {@code null} — lihat
 * {@code ItemMedisAction}/{@code KonversiSatuanItemAction}), bukan
 * perilaku getter yang menimpa nilai tersimpan.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "konversi_satuan_item")
public class KonversiSatuanItem extends GeneralValueObject {

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
	 * Representasi ringkas baris konversi satuan ini untuk tampilan/log.
	 *
	 * @return teks keterangan baris konversi ini.
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
	private SatuanItem satuanDari;
	private SatuanItem satuanMenjadi;
	private Double nilaiPersamaan = 1.0;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate). Field
	 * {@link #nilaiPersamaan} diinisialisasi ke {@code 1.0} saat
	 * deklarasi, sekadar sebagai nilai awal form tambah data baru, BUKAN
	 * perilaku getter yang menimpa nilai tersimpan — lihat javadoc kelas.
	 */
	public KonversiSatuanItem() {
	}

	/**
	 * Primary key baris konversi satuan item, auto-increment (IDENTITY)
	 * dan diisi database.
	 *
	 * @return ID unik baris konversi satuan item ini.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID baris konversi satuan item.
	 *
	 * @param id ID baris konversi satuan item.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil keterangan bebas baris konversi satuan ini.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas baris konversi satuan ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengambil satuan asal ("dari") dari konversi ini — relasi OPSIONAL
	 * ke {@link SatuanItem}.
	 *
	 * @return satuan asal, atau {@code null} jika belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_dari", nullable = true)
	public SatuanItem getSatuanDari() {
		satuanDari = check(satuanDari);
		return satuanDari;
	}

	/**
	 * Menetapkan satuan asal ("dari") dari konversi ini.
	 *
	 * @param satuanDari satuan asal konversi.
	 */
	public void setSatuanDari(SatuanItem satuanDari) {
		this.satuanDari = satuanDari;
	}

	/**
	 * Mengambil satuan tujuan ("menjadi") dari konversi ini — relasi
	 * OPSIONAL ke {@link SatuanItem}.
	 *
	 * @return satuan tujuan, atau {@code null} jika belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_menjadi", nullable = true)
	public SatuanItem getSatuanMenjadi() {
		satuanMenjadi = check(satuanMenjadi);
		return satuanMenjadi;
	}

	/**
	 * Menetapkan satuan tujuan ("menjadi") dari konversi ini.
	 *
	 * @param satuanMenjadi satuan tujuan konversi.
	 */
	public void setSatuanMenjadi(SatuanItem satuanMenjadi) {
		this.satuanMenjadi = satuanMenjadi;
	}

	/**
	 * Mengambil faktor konversi (nilai persamaan) dari
	 * {@link #getSatuanDari()} ke {@link #getSatuanMenjadi()} APA
	 * ADANYA — getter ini TIDAK melakukan normalisasi maupun reset ke
	 * {@code 1.0} untuk nilai tidak valid; lihat javadoc kelas untuk
	 * perbandingan eksplisit dengan pola bug serupa yang pernah
	 * ditemukan di modul lain.
	 *
	 * @return faktor konversi, atau {@code null} bila baris dimuat dari
	 *         database tanpa nilai (field hanya berisi {@code 1.0} bila
	 *         objek baru dibuat lewat konstruktor dan belum pernah
	 *         di-load ulang).
	 */
	@Column(name = "nilai_persamaan", nullable = true)
	public Double getNilaiPersamaan() {
		return nilaiPersamaan;
	}

	/**
	 * Menetapkan faktor konversi dari {@link #getSatuanDari()} ke
	 * {@link #getSatuanMenjadi()}.
	 *
	 * @param nilaiPersamaan faktor konversi; sebaiknya bukan nol untuk
	 *                       menghindari pembagian dengan nol di kode
	 *                       pemanggil yang menghitung konversi terbalik
	 *                       (validasi ini tidak dijaga di level entitas
	 *                       ini).
	 */
	public void setNilaiPersamaan(Double nilaiPersamaan) {
		this.nilaiPersamaan = nilaiPersamaan;
	}

	/**
	 * Menetapkan item medis yang menjadi konteks konversi ini.
	 *
	 * @param item item medis terkait; {@code null} berarti konversi ini
	 *             berlaku generik/tidak terikat item tertentu.
	 */
	public void setItem(ItemMedis item) {
		this.item = item;
	}

	/**
	 * Mengambil item medis yang menjadi konteks konversi ini — relasi
	 * OPSIONAL ({@code nullable = true}), sehingga baris konversi BISA
	 * dibuat tanpa terikat ke satu item tertentu.
	 *
	 * @return item medis terkait, atau {@code null} jika konversi ini
	 *         generik/tidak terikat item tertentu.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = true)
	public ItemMedis getItem() {
		item = check(item);
		return item;
	}

}
