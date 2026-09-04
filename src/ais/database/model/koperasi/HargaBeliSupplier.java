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
import ais.database.model.library.Penyedia;

/**
 * Master Harga Beli per Supplier (layar legacy 18, {@code masterbl.DBF}: KODESUPPL, KODEBRG,
 * TANGGAL, HARGABELI) -- harga BERVERSI by {@code tanggalEfektif}: pasangan supplier-produk pada
 * rentang efektif yang sama harus unik (overlap ditolak di helper simpan); histori TIDAK ditimpa
 * (perubahan harga = baris versi baru); transaksi memilih versi dgn {@code tanggalEfektif}
 * terbaru &le; tanggal transaksi lalu MENYIMPAN SNAPSHOT sendiri (faktur historis tidak
 * dihitung ulang). Nonaktifkan versi ({@code aktif=false}) utk data salah yang belum dipakai --
 * bukan delete fisik.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "harga_beli_supplier")
public class HargaBeliSupplier extends GeneralValueObject {

	/** Versi serialisasi tetap; dipertahankan hanya krn kontrak {@link GeneralValueObject}/
	 * {@code Serializable}. */
	private static final long serialVersionUID = 1L;

	/** PK auto-generated (identity). Lihat {@link #getId()}. */
	private Long id;
	/** Supplier (pemasok, entity {@link Penyedia} di paket {@code library}) pemilik harga ini.
	 * Lihat {@link #getSupplier()}. */
	private Penyedia supplier;
	/** Produk yang harganya diatur baris ini. Lihat {@link #getProduk()}. */
	private Produk produk;
	/** Harga beli per unit produk dari supplier ini, berlaku sejak {@link #tanggalEfektif}. Lihat
	 * {@link #getHarga()}. */
	private BigDecimal harga;
	/** Tanggal mulai berlakunya versi harga ini; bersama {@code supplier} dan {@code produk}
	 * membentuk kunci keunikan yg dijaga di helper simpan (bukan constraint DB) -- lihat Javadoc
	 * kelas & {@link #getTanggalEfektif()}. */
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
	 * menolak mengubah {@code harga}/{@code tanggalEfektif}/{@code produk}/{@code supplierId}
	 * pada baris tersimpan (hanya {@code keterangan}/{@code aktif} yang boleh diedit; perubahan
	 * harga WAJIB berupa baris versi baru, bukan update baris lama -- lihat Javadoc kelas).
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor tanpa argumen wajib JPA/Hibernate; jangan dipakai langsung dari kode aplikasi --
	 * gunakan setter (via {@code SalesInventoryHargaHelper}) sebelum {@code save}, mengikuti aturan
	 * "versi baru, bukan update" pada Javadoc kelas. */
	public HargaBeliSupplier() {
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
	 * Supplier pemilik harga pada baris ini. {@code nullable = false} -- berbeda dari
	 * {@link HargaJualCustomer#getAnggotaKoperasi()} yang sengaja nullable (utk kasus "harga
	 * umum"), di sisi beli TIDAK ADA konsep "harga umum tanpa supplier" -- setiap baris harga beli
	 * wajib terikat satu supplier tertentu. Relasi {@code LAZY}: mengakses field pada objek di
	 * luar sesi Hibernate yang masih terbuka akan melempar {@code LazyInitializationException}.
	 *
	 * @return supplier pemilik harga ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "supplier", nullable = false)
	public Penyedia getSupplier() {
		supplier = check(supplier);
		return supplier;
	}

	/**
	 * Menetapkan supplier pemilik harga. Helper simpan menolak mengubah field ini pada baris yang
	 * sudah tersimpan (lihat catatan {@link #onUpdate()}) -- hanya dipakai saat membangun baris
	 * baru.
	 *
	 * @param supplier supplier pemilik harga baris ini.
	 */
	public void setSupplier(Penyedia supplier) {
		this.supplier = supplier;
	}

	/**
	 * Produk yang harganya diatur baris ini. {@code nullable = false}. Relasi {@code LAZY} -- sama
	 * catatan lazy-loading dgn {@link #getSupplier()}.
	 *
	 * @return produk terkait baris harga ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk", nullable = false)
	public Produk getProduk() {
		produk = check(produk);
		return produk;
	}

	/**
	 * Menetapkan produk terkait. Helper simpan menolak mengubah field ini pada baris yang sudah
	 * tersimpan -- sama seperti {@link #setSupplier(Penyedia)}.
	 *
	 * @param produk produk yang harganya diatur.
	 */
	public void setProduk(Produk produk) {
		this.produk = produk;
	}

	/**
	 * Harga beli per unit produk dari supplier ini, berlaku sejak {@link #getTanggalEfektif()}.
	 * Getter null-safe: mengembalikan {@link BigDecimal#ZERO} bila kolom NULL di DB.
	 *
	 * <p><b>Catatan integritas:</b> tidak ada validasi nilai non-negatif di level entity atau di
	 * {@code SalesInventoryHargaHelper} -- helper hanya mensyaratkan field tsb terisi (tidak
	 * {@code null}), sehingga secara teori harga nol atau negatif bisa tersimpan bila dikirim
	 * demikian oleh caller. Dicatat sbg observasi (sama pola dgn {@link
	 * HargaJualCustomer#getHarga()}), bukan diajukan sbg task baru krn dampaknya salah-entri data
	 * dari user berwenang, bukan celah otorisasi.
	 *
	 * @return harga beli, tidak pernah {@code null}.
	 */
	@Column(name = "harga", precision = 19, scale = 2)
	public BigDecimal getHarga() {
		return harga == null ? BigDecimal.ZERO : harga;
	}

	/**
	 * Menetapkan harga beli baris ini. Helper simpan menolak mengubah field ini pada baris yang
	 * sudah tersimpan (perubahan harga = buat versi baru dgn {@code tanggalEfektif} baru, histori
	 * tidak ditimpa) -- lihat Javadoc kelas.
	 *
	 * @param harga harga beli baru.
	 */
	public void setHarga(BigDecimal harga) {
		this.harga = harga;
	}

	/**
	 * Tanggal mulai berlakunya versi harga ini (kolom tipe {@code DATE}, tanpa komponen jam).
	 * {@code nullable = false}. Bersama {@code supplier} dan {@code produk}, kombinasi ini
	 * diperiksa UNIK oleh {@code SalesInventoryHargaHelper} sebelum insert.
	 *
	 * <p><b>Catatan integritas (race condition):</b> pengecekan duplikat ("SELECT COUNT(*) FROM
	 * koperasi.harga_beli_supplier WHERE supplier = ? AND produk = ? AND tanggal_efektif = ?") di
	 * {@code SalesInventoryHargaHelper} adalah check-then-insert BIASA -- tidak dikawal
	 * {@code FOR UPDATE}, advisory lock, atau constraint UNIK di level DB pada tabel
	 * {@code koperasi.harga_beli_supplier} (berbeda dari tabel versi skema TENANT yang punya
	 * {@code CREATE UNIQUE INDEX} parsial utk {@code harga_jual_customer}; tabel tenant
	 * {@code harga_beli_supplier} bahkan tidak punya unique index setara). Dua request simpan
	 * bersamaan dgn kombinasi supplier+produk+tanggal yang sama karena itu BISA lolos keduanya dan
	 * menghasilkan dua baris "unik" yang sama-sama {@code aktif=true} -- resolusi harga transaksi
	 * lalu diam-diam memilih baris ber-{@code id} terbesar (paling baru dibuat, lihat
	 * {@code ORDER BY tanggal_efektif DESC, id DESC LIMIT 1}), TANPA galat yang memberi tahu user
	 * bahwa duplikat sudah tercipta. Bila baris pemenang (id terbesar) kelak dinonaktifkan
	 * (mengira hanya ada satu versi pada tanggal tsb), baris "duplikat" yang lebih lama akan
	 * diam-diam menjadi harga aktif berikutnya dgn nilai yang mungkin berbeda -- pola race yang
	 * sama dgn yang sudah ditemukan &amp; ditambal (lewat UNIQUE index) pada penjaga Jurnal
	 * Penyesuaian. Dilaporkan terpisah via {@code spawn_task} sbg kandidat perbaikan (constraint
	 * unik parsial di skema {@code koperasi}), bukan ditambal langsung di sini krn di luar cakupan
	 * (perubahan skema/servlet, bukan Javadoc model).
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
	 * kolom NULL di DB -- DEFAULT AKTIF, bukan default nonaktif. Baris {@code aktif = false}
	 * dikecualikan dari resolusi harga transaksi tetapi TIDAK dihapus fisik -- histori tetap
	 * tersimpan (nonaktivasi dipakai utk data salah yang belum dipakai, bukan penghapusan).
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
	 * salah entri tanpa menghapusnya.
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
	 * ({@link ais.ui.util.WaktuUtil#getDate()}) bila kolom NULL, dihitung ULANG setiap kali getter
	 * dipanggil pada baris yang kolomnya NULL (bukan waktu tetap saat objek dibuat).
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
