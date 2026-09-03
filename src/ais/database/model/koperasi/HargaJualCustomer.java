package ais.database.model.koperasi;

import static javax.persistence.GenerationType.IDENTITY;

import java.math.BigDecimal;
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
import ais.database.model.inventory.Produk;

/**
 * Master Harga Jual per Customer (layar legacy 19) -- kembaran {@link HargaBeliSupplier} di sisi
 * jual: pasangan customer-produk berversi by {@code tanggalEfektif} (overlap ditolak), histori
 * tidak ditimpa, transaksi mengambil versi berlaku lalu menyimpan snapshot sendiri.
 * {@code anggotaKoperasi} NULLABLE: baris tanpa customer = harga jual KHUSUS daftar harga umum
 * bertanggal (dipakai layar 13 "Daftar Harga Jual" versi umum) -- customer spesifik menang atas
 * baris umum saat resolusi.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "harga_jual_customer")
public class HargaJualCustomer extends GeneralValueObject {

	/** Versi serialisasi tetap; dipertahankan hanya krn kontrak {@link GeneralValueObject}/
	 * {@code Serializable}. */
	private static final long serialVersionUID = 1L;

	/** PK auto-generated (identity). Lihat {@link #getId()}. */
	private Long id;
	/** Customer (anggota koperasi) pemilik harga khusus ini; {@code NULL} berarti baris ini adalah
	 * harga jual UMUM (berlaku semua customer) -- lihat catatan resolusi di Javadoc kelas dan
	 * {@link #getAnggotaKoperasi()}. */
	private AnggotaKoperasi anggotaKoperasi;
	/** Produk yang harganya diatur baris ini. Lihat {@link #getProduk()}. */
	private Produk produk;
	/** Harga jual per unit produk, berlaku sejak {@link #tanggalEfektif}. Lihat
	 * {@link #getHarga()}. */
	private BigDecimal harga;
	/** Tanggal mulai berlakunya versi harga ini; bersama {@code produk} (dan {@code anggotaKoperasi}
	 * bila customer spesifik) membentuk kunci keunikan yg dijaga di helper simpan (bukan constraint
	 * DB) -- lihat Javadoc kelas & {@link #getTanggalEfektif()}. */
	private Date tanggalEfektif;
	/** Catatan bebas ttg baris harga ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Status aktif/nonaktif versi harga ini; {@code false} = versi dinonaktifkan (bukan dihapus
	 * fisik) krn salah entri dan belum pernah dipakai transaksi. Lihat {@link #getAktif()}. */
	private Boolean aktif;

	/** Nama petugas yang membuat/mengubah baris harga ini (jejak audit tampilan, bukan FK). Lihat
	 * {@link #getOleh()}. */
	private String oleh;
	/** ID/username petugas yang membuat/mengubah baris harga ini. Lihat {@link #getOlehId()}. */
	private String olehId;
	/** Waktu baris ini dibuat (BUKAN {@link #tanggalEfektif} -- itu tanggal mulai berlaku harga,
	 * ini waktu entry sesungguhnya). Lihat {@link #getWaktu()}. */
	private Date waktu;
	/**
	 * Callback JPA {@code @PreUpdate}: menandai kapan baris ini TERAKHIR diubah, dengan menuliskan
	 * waktu sekarang ke {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Dipanggil otomatis
	 * oleh Hibernate sebelum {@code UPDATE} -- pada praktiknya jarang terpicu krn helper simpan
	 * menolak mengubah {@code harga}/{@code tanggalEfektif}/{@code produk}/{@code anggotaId} pada
	 * baris tersimpan (hanya {@code keterangan}/{@code aktif} yang boleh diedit; perubahan harga
	 * WAJIB berupa baris versi baru, bukan update baris lama -- lihat Javadoc kelas).
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor tanpa argumen wajib JPA/Hibernate; jangan dipakai langsung dari kode aplikasi --
	 * gunakan setter (via {@code SalesInventoryHargaHelper}) sebelum {@code save}, mengikuti aturan
	 * "versi baru, bukan update" pada Javadoc kelas. */
	public HargaJualCustomer() {
	}

	/**
	 * PK identity baris harga ini. {@code null} sebelum entity di-{@code save}/{@code flush} ke
	 * Hibernate (ID baru dibuat DB saat insert, strategi {@link IDENTITY}).
	 *
	 * @return id baris harga, atau {@code null} bila belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Setter PK -- dipanggil Hibernate saat memuat entity dari DB. Kode aplikasi normal tidak
	 * perlu memanggil ini; id baru dibuat otomatis oleh DB saat insert.
	 *
	 * @param id id baris harga.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Customer (anggota koperasi) pemilik harga khusus pada baris ini. {@code nullable = true} --
	 * berbeda dari {@link HargaBeliSupplier#getSupplier()} yang wajib (supplier selalu jelas),
	 * relasi ini SENGAJA nullable: baris dgn {@code anggotaKoperasi == null} adalah harga jual
	 * UMUM (daftar harga standar, layar 13), sedangkan baris dgn anggota terisi adalah harga
	 * KHUSUS pelanggan tsb yang menang atas baris umum saat resolusi harga transaksi (lihat
	 * Javadoc kelas). Relasi {@code LAZY}: mengakses field pada objek di luar sesi Hibernate yang
	 * masih terbuka akan melempar {@code LazyInitializationException} -- termasuk saat memeriksa
	 * {@code == null} pada proxy uninitialized, yang tetap aman krn tidak menyentuh field proxy.
	 *
	 * @return anggota koperasi pemilik harga khusus ini, atau {@code null} bila baris harga umum.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "anggota_koperasi", nullable = true)
	public AnggotaKoperasi getAnggotaKoperasi() {
		return anggotaKoperasi;
	}

	/**
	 * Menetapkan customer pemilik harga khusus. {@code null} secara sengaja berarti "baris harga
	 * umum" (lihat {@link #getAnggotaKoperasi()}) -- BUKAN error, jadi setter ini tidak melakukan
	 * guard null seperti {@link #setOleh}/{@link #setOlehId}.
	 *
	 * @param anggotaKoperasi anggota koperasi pemilik harga khusus, atau {@code null} utk harga
	 *                        umum.
	 */
	public void setAnggotaKoperasi(AnggotaKoperasi anggotaKoperasi) {
		this.anggotaKoperasi = anggotaKoperasi;
	}

	/**
	 * Produk yang harganya diatur baris ini. {@code nullable = false} -- setiap baris harga wajib
	 * terikat satu produk. Relasi {@code LAZY} -- sama catatan lazy-loading dgn
	 * {@link #getAnggotaKoperasi()}.
	 *
	 * @return produk terkait baris harga ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk", nullable = false)
	public Produk getProduk() {
		return produk;
	}

	/**
	 * Menetapkan produk terkait. Helper simpan menolak mengubah field ini pada baris yang sudah
	 * tersimpan (lihat catatan {@link #onUpdate()}) -- hanya dipakai saat membangun baris baru.
	 *
	 * @param produk produk yang harganya diatur.
	 */
	public void setProduk(Produk produk) {
		this.produk = produk;
	}

	/**
	 * Harga jual per unit produk pada baris ini, berlaku sejak {@link #getTanggalEfektif()}.
	 * Getter null-safe: mengembalikan {@link BigDecimal#ZERO} bila kolom NULL di DB.
	 *
	 * <p><b>Catatan integritas:</b> tidak ada validasi nilai non-negatif di level entity atau di
	 * {@code SalesInventoryHargaHelper} -- helper hanya mensyaratkan field tsb terisi (tidak
	 * {@code null}), sehingga secara teori harga nol atau negatif bisa tersimpan bila dikirim
	 * demikian oleh caller. Ini pola yang sama longgarnya dgn field {@code harga} pada
	 * {@link HargaBeliSupplier}, dicatat di sini sbg observasi, bukan diajukan sbg task baru
	 * (dampaknya murni salah-entri data, bukan celah otorisasi -- endpoint tetap memerlukan sesi
	 * user yang berwenang utk memanggilnya).
	 *
	 * @return harga jual, tidak pernah {@code null}.
	 */
	@Column(name = "harga", precision = 19, scale = 2)
	public BigDecimal getHarga() {
		return harga == null ? BigDecimal.ZERO : harga;
	}

	/**
	 * Menetapkan harga jual baris ini. Helper simpan menolak mengubah field ini pada baris yang
	 * sudah tersimpan (perubahan harga = buat versi baru dgn {@code tanggalEfektif} baru, histori
	 * tidak ditimpa) -- lihat Javadoc kelas.
	 *
	 * @param harga harga jual baru.
	 */
	public void setHarga(BigDecimal harga) {
		this.harga = harga;
	}

	/**
	 * Tanggal mulai berlakunya versi harga ini (kolom tipe {@code DATE}, tanpa komponen jam).
	 * {@code nullable = false}. Bersama {@code produk} dan {@code anggotaKoperasi} (termasuk kasus
	 * {@code NULL} utk harga umum), kombinasi ini diperiksa UNIK oleh
	 * {@code SalesInventoryHargaHelper} sebelum insert -- lihat catatan race-condition penting di
	 * Javadoc kelas dan method {@code customerPriceList} pada helper tsb: pengecekan "SELECT
	 * COUNT(*) ... WHERE tanggal_efektif = ?" TIDAK dikawal kunci baris/constraint DB (berbeda dari
	 * tabel {@code harga_beli_supplier}/{@code harga_jual_customer} versi skema TENANT yang punya
	 * {@code CREATE UNIQUE INDEX} parsial), sehingga dua request bersamaan dgn kombinasi
	 * produk+tanggal(+anggota) yang sama bisa lolos keduanya (TOCTOU) dan menghasilkan dua baris
	 * "unik" yang sama-sama {@code aktif=true} -- resolusi harga transaksi lalu diam-diam memilih
	 * baris ber-{@code id} terbesar (paling baru dibuat), bukan menolak/memberi galat.
	 *
	 * @return tanggal efektif versi harga ini.
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_efektif", nullable = false)
	public Date getTanggalEfektif() {
		return tanggalEfektif;
	}

	/**
	 * Menetapkan tanggal efektif. Helper simpan menolak mengubah field ini pada baris yang sudah
	 * tersimpan -- sama seperti {@link #setHarga(BigDecimal)}.
	 *
	 * @param tanggalEfektif tanggal mulai berlaku versi harga ini.
	 */
	public void setTanggalEfektif(Date tanggalEfektif) {
		this.tanggalEfektif = tanggalEfektif;
	}

	/**
	 * Catatan bebas ttg baris harga ini. Salah satu dari sedikit field yang boleh diubah pada
	 * baris tersimpan (bersama {@link #getAktif()}) tanpa perlu membuat versi baru.
	 *
	 * @return keterangan, atau {@code null}/kosong bila tidak diisi.
	 */
	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menetapkan keterangan. Tidak ada guard null/blank -- memanggil dgn string kosong akan
	 * menimpa nilai lama.
	 *
	 * @param keterangan catatan bebas ttg baris harga ini.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Status aktif versi harga ini. Getter null-safe: mengembalikan {@link Boolean#TRUE} bila
	 * kolom NULL di DB (baris lama sebelum kolom ini ada, atau belum pernah di-set eksplisit) --
	 * DEFAULT AKTIF, bukan default nonaktif. Baris {@code aktif = false} dikecualikan dari
	 * resolusi harga transaksi (lihat query {@code COALESCE(h.aktif,true) = true} di
	 * {@code SalesInventoryHargaHelper}) tetapi TIDAK dihapus fisik -- histori tetap tersimpan.
	 *
	 * @return {@code true} bila versi harga ini aktif dipakai, {@code false} bila dinonaktifkan.
	 */
	@Column(name = "aktif")
	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	/**
	 * Menetapkan status aktif. Salah satu dari sedikit field yang boleh diubah pada baris
	 * tersimpan (bersama {@link #getKeterangan()}) -- dipakai utk menonaktifkan versi harga yang
	 * salah entri tanpa menghapusnya (lihat Javadoc kelas).
	 *
	 * @param aktif status aktif baru; {@code null} diperlakukan sbg aktif oleh getter.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Nama petugas yang membuat/mengubah baris harga ini.
	 *
	 * @return nama petugas, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menetapkan {@link #oleh}. Guard null/blank: nilai {@code null}/kosong/spasi DIABAIKAN
	 * (early return) -- field yang sudah terisi tidak ditimpa balik ke kosong.
	 *
	 * @param oleh nama petugas; diabaikan bila {@code null} atau blank.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * ID/username petugas yang membuat/mengubah baris harga ini.
	 *
	 * @return id/username petugas, atau {@code null} bila belum pernah diisi.
	 */
	@Column(name = "oleh_id")
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan {@link #olehId}. Guard null/blank sama seperti {@link #setOleh(String)}.
	 *
	 * @param olehId id/username petugas; diabaikan bila {@code null} atau blank.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Waktu baris harga ini dibuat/dientri. Getter null-safe: mengembalikan waktu SEKARANG
	 * ({@link ais.ui.util.WaktuUtil#getDate()}) bila kolom NULL -- berbeda dari kebanyakan getter
	 * lain di kelas ini yang mengembalikan {@code null}/nol/{@code true} sbg default; di sini
	 * default-nya adalah "sekarang", dihitung ULANG setiap kali getter dipanggil pada baris yang
	 * kolomnya NULL (bukan waktu tetap saat objek dibuat).
	 *
	 * @return waktu entry baris ini, atau waktu panggilan getter saat ini bila kolom NULL.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu")
	public Date getWaktu() {
		return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
	}

	/**
	 * Menetapkan waktu entry baris ini.
	 *
	 * @param waktu waktu baris ini dibuat/dientri.
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Timestamp perubahan terakhir baris ini, diisi otomatis oleh {@link #onUpdate()}.
	 *
	 * @return waktu perubahan terakhir, atau waktu instansiasi objek bila belum pernah di-update.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Setter manual utk {@link #tanggal_dirubah}. Jarang dipakai langsung -- field ini biasanya
	 * diisi otomatis oleh {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah waktu perubahan yang ingin dicatat.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
