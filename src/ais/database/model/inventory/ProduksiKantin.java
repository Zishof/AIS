package ais.database.model.inventory;

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
 * <h2>ProduksiKantin — catatan Produksi Harian menu kantin.</h2>
 *
 * <p>
 * Entity BARU untuk mencatat <b>hasil produksi</b> tiap menu kantin per hari sehingga tersedia laporan
 * katalog §3.5: <i>Rencana Produksi</i>, <i>Realisasi Produksi</i>, dan <i>Sisa Makanan/Waste</i>.
 * Sebelumnya sistem hanya punya penjualan (porsi terjual) dan {@link PemakaianBahanBaku} (pemakaian
 * bahan) namun belum ada konsep <i>porsi dibuat/sisa/dibuang</i>. Dengan pendaftaran di
 * {@code hibernate.cfg.xml}, tabel {@code koperasi.produksi_kantin} otomatis dibuat (hbm2ddl=update).
 * </p>
 *
 * <h3>Kolom & perhitungan</h3>
 * <ul>
 *   <li>{@code porsiRencana} — target porsi yang direncanakan.</li>
 *   <li>{@code porsiDibuat} — porsi yang benar-benar diproduksi.</li>
 *   <li>{@code porsiTerjual} — porsi terjual (boleh diisi manual atau dicocokkan dengan penjualan).</li>
 *   <li>{@code porsiSisa} — sisa yang tidak terjual namun masih layak.</li>
 *   <li>{@code porsiWaste} — porsi terbuang/rusak/basi (kerugian).</li>
 * </ul>
 *
 * <p>
 * Penamaan kolom mengikuti aturan proyek (field tanpa @Column ter-fold ke huruf kecil tanpa underscore,
 * mis. {@code porsiRencana}→{@code porsirencana}). Kompatibel Java 1.7 / Hibernate 3.
 * </p>
 *
 * @author AIS e-Kantin (modul produksi)
 * @see PemakaianBahanBaku
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "produksi_kantin")
public class ProduksiKantin extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private Produk produk;
	private Toko toko;
	private Date tanggal;
	private Double porsiRencana;
	private Double porsiDibuat;
	private Double porsiTerjual;
	private Double porsiSisa;
	private Double porsiWaste;
	private String keterangan;
	private String oleh;
	private String olehId;

	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil otomatis Hibernate tepat sebelum tiap {@code UPDATE}
	 * baris ini, mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah}
	 * yang menyetel {@link #tanggal_dirubah} ke waktu saat itu.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor kosong wajib Hibernate. Field diisi lewat setter/reflection sebelum atau saat pemuatan dari DB. */
	public ProduksiKantin() {
	}

	/**
	 * Primary key baris produksi harian ini. Digenerasi database via strategi {@code IDENTITY};
	 * {@code null} pada objek yang belum pernah di-{@code save}.
	 * @return id baris, atau {@code null} jika belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** Setter {@link #getId()} -- normalnya hanya dipanggil Hibernate saat memuat baris dari DB. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Produk (menu kantin) yang produksinya dicatat baris ini. Getter memanggil
	 * {@link ais.database.model.GeneralValueObject#check(Object)} untuk meresolusi proxy lazy yang
	 * mungkin sudah <i>detached</i> dari session asalnya (pola getter relasi standar di seluruh
	 * entity AIS) sebelum mengembalikan field.
	 * @return produk/menu yang diproduksi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk")
	public Produk getProduk() {
		produk = check(produk);
		return produk;
	}

	/** Setter {@link #getProduk()}. */
	public void setProduk(Produk produk) {
		this.produk = produk;
	}

	/**
	 * Toko/outlet kantin tempat produksi ini dicatat. Getter memanggil
	 * {@link ais.database.model.GeneralValueObject#check(Object)}, lihat javadoc {@link #getProduk()}.
	 * @return toko pemilik baris produksi ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko")
	public Toko getToko() {
		toko = check(toko);
		return toko;
	}

	/** Setter {@link #getToko()}. */
	public void setToko(Toko toko) {
		this.toko = toko;
	}

	/**
	 * Tanggal produksi (satu baris per produk per toko per hari, secara konvensi -- kelas ini
	 * TIDAK memiliki {@code @UniqueConstraint} yang menegakkan itu, jadi duplikasi baris untuk
	 * kombinasi produk/toko/tanggal yang sama secara teknis dimungkinkan bila pemanggil tidak
	 * berhati-hati). Getter null-safe: {@code null} dibaca sebagai waktu saat getter dipanggil
	 * ({@code WaktuUtil.getDate()}), BUKAN dibekukan.
	 * @return tanggal produksi, tidak pernah {@code null}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal() {
		return tanggal == null ? ais.ui.util.WaktuUtil.getDate() : tanggal;
	}

	/** Setter {@link #getTanggal()}. */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Target porsi yang direncanakan diproduksi hari ini. Getter null-safe: {@code null} dibaca
	 * sebagai {@code 0.0}.
	 * @return target porsi rencana, tidak pernah {@code null}.
	 */
	public Double getPorsiRencana() {
		return porsiRencana == null ? 0.0 : porsiRencana;
	}

	/** Setter {@link #getPorsiRencana()}. */
	public void setPorsiRencana(Double porsiRencana) {
		this.porsiRencana = porsiRencana;
	}

	/**
	 * Porsi yang BENAR-BENAR diproduksi (realisasi terhadap {@link #getPorsiRencana()}). Getter
	 * null-safe: {@code null} dibaca sebagai {@code 0.0}.
	 *
	 * <p><b>Penjaga keseimbangan produksi-hasil (TIDAK ditegakkan model/database).</b> Secara
	 * konsep, porsi yang diproduksi lalu mengalir ke salah satu dari tiga nasib: terjual
	 * ({@link #getPorsiTerjual()}), tersisa layak jual ({@link #getPorsiSisa()}), atau
	 * terbuang/rusak ({@link #getPorsiWaste()}) -- sehingga secara akuntansi porsi wajar bila
	 * {@code porsiDibuat = porsiTerjual + porsiSisa + porsiWaste}. Kelas ini TIDAK memiliki
	 * constraint {@code CHECK}, trigger, maupun validasi setter yang menegakkan persamaan
	 * tersebut -- keempat field ({@code porsiDibuat}, {@code porsiTerjual}, {@code porsiSisa},
	 * {@code porsiWaste}) adalah kolom {@code Double} independen yang masing-masing bisa diisi
	 * bebas tanpa saling memeriksa. Penelusuran kode BELUM menemukan satu pun jalur penulis khusus
	 * (servlet/action) yang mengisi baris kelas ini secara terprogram -- satu-satunya jalur tulis
	 * yang teridentifikasi adalah editor generik {@code RevisiApiHelper} (entri {@code "produksi"}
	 * pada peta entitasnya) yang mengizinkan create/update/delete refleksi tanpa logika bisnis
	 * spesifik kelas ini. Entitas ini terdaftar "hidup" pada metadata modul dasbor
	 * ({@code NewUiModuleDashboardService}, entri "Produksi" dasbor Kantin/Toko) namun belum
	 * ditemukan kueri agregat/dasbor nyata yang membaca kolom-kolom ini
	 * ({@code DashboardKantinAction} tidak merujuknya) -- konsisten dengan javadoc kelas yang
	 * menyebutnya "Entity BARU" untuk laporan §3.5 yang disiapkan lebih dulu skemanya. Karena
	 * belum ada jalur tulis bisnis yang mengisi field ini secara otomatis dari transaksi penjualan
	 * riil, penjaga keseimbangan di atas SEPENUHNYA bergantung pada kedisiplinan operator yang
	 * mengisi form (bila/ketika UI-nya dibuat) atau editor generik -- dicatat di sini sebagai
	 * referensi audit sesuai pola serupa yang sudah tercatat berulang di paket ini (lihat javadoc
	 * {@link MutasiStokProduksi#getQtyMasuk()} untuk domain produksi WO, dan
	 * {@link ProduksiGenealogiLot#getAllocatedQty()} untuk penjaga genealogi lot), bukan sebagai
	 * temuan baru yang genuinely berbeda.</p>
	 *
	 * @return porsi yang diproduksi, tidak pernah {@code null}.
	 */
	public Double getPorsiDibuat() {
		return porsiDibuat == null ? 0.0 : porsiDibuat;
	}

	/** Setter {@link #getPorsiDibuat()} -- lihat javadoc getter soal ketiadaan penjaga keseimbangan produksi-hasil. */
	public void setPorsiDibuat(Double porsiDibuat) {
		this.porsiDibuat = porsiDibuat;
	}

	/**
	 * Porsi yang terjual pada tanggal ini -- boleh diisi manual atau dicocokkan dengan data
	 * penjualan riil (lihat javadoc kelas). Getter null-safe: {@code null} dibaca sebagai
	 * {@code 0.0}. Lihat javadoc {@link #getPorsiDibuat()} soal penjaga keseimbangan yang tidak
	 * ditegakkan terhadap tiga field nasib porsi ini/{@link #getPorsiSisa()}/{@link #getPorsiWaste()}.
	 * @return porsi terjual, tidak pernah {@code null}.
	 */
	public Double getPorsiTerjual() {
		return porsiTerjual == null ? 0.0 : porsiTerjual;
	}

	/** Setter {@link #getPorsiTerjual()}. */
	public void setPorsiTerjual(Double porsiTerjual) {
		this.porsiTerjual = porsiTerjual;
	}

	/**
	 * Porsi sisa yang tidak terjual namun masih layak (mis. bisa disimpan/dijual esok hari,
	 * berbeda dari {@link #getPorsiWaste()} yang sudah tidak layak). Getter null-safe: {@code null}
	 * dibaca sebagai {@code 0.0}.
	 * @return porsi sisa layak, tidak pernah {@code null}.
	 */
	public Double getPorsiSisa() {
		return porsiSisa == null ? 0.0 : porsiSisa;
	}

	/** Setter {@link #getPorsiSisa()}. */
	public void setPorsiSisa(Double porsiSisa) {
		this.porsiSisa = porsiSisa;
	}

	/**
	 * Porsi terbuang/rusak/basi -- kerugian murni, berbeda dari {@link #getPorsiSisa()} yang masih
	 * layak. Getter null-safe: {@code null} dibaca sebagai {@code 0.0}.
	 * @return porsi waste, tidak pernah {@code null}.
	 */
	public Double getPorsiWaste() {
		return porsiWaste == null ? 0.0 : porsiWaste;
	}

	/** Setter {@link #getPorsiWaste()}. */
	public void setPorsiWaste(Double porsiWaste) {
		this.porsiWaste = porsiWaste;
	}

	/**
	 * Catatan bebas untuk baris produksi ini (mis. alasan waste tinggi/rencana meleset), opsional.
	 * @return catatan, atau {@code null} bila tidak diisi.
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/** Setter {@link #getKeterangan()}. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Nama petugas yang mencatat/mengubah baris ini -- jejak audit tampilan, bebas teks.
	 * @return nama petugas, atau {@code null} bila tidak diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/** Setter {@link #getOleh()}. */
	public void setOleh(String oleh) {
		this.oleh = oleh;
	}

	/**
	 * Id/userid petugas yang mencatat/mengubah baris ini -- pasangan identitas mesin untuk
	 * {@link #getOleh()} (nama tampilan), dipakai bila pencarian/pemfilteran berbasis id
	 * diperlukan alih-alih nama teks bebas.
	 * @return id petugas, atau {@code null} bila tidak diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/** Setter {@link #getOlehId()}. */
	public void setOlehId(String olehId) {
		this.olehId = olehId;
	}

	/**
	 * Waktu baris ini terakhir diubah. Diinisialisasi ke waktu instansiasi objek pada deklarasi
	 * field, dan diperbarui otomatis oleh {@link #onUpdate()} setiap {@code UPDATE}.
	 * @return waktu baris ini terakhir diubah.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Setter {@link #getTanggal_dirubah()} -- normalnya hanya dipanggil {@link #onUpdate()}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
