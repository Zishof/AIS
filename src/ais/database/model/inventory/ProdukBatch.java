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
import javax.persistence.UniqueConstraint;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * <h3>Batch/lot produksi -- pelacakan kedaluwarsa dan konsumsi FEFO, TERPISAH dari stok agregat.</h3>
 *
 * <p>Satu baris mewakili satu lot fisik produk pada satu toko/outlet: kombinasi
 * {@link #getProduk()} + {@link #getToko()} + {@link #getNomorBatch()} +
 * {@link #getTanggalExpired()} (ke-4nya {@code UNIQUE} bersama). {@link #getStok()} adalah saldo
 * lot ini SAJA yang masih dapat dipakai -- BUKAN stok agregat produk; {@code Produk.stok}
 * (agregat, dihitung/direkonsiliasi jalur penjualan lain) tetap berjalan sebagai catatan
 * terpisah dan TIDAK otomatis sinkron dua-arah dengan jumlah seluruh batch aktif. Pelacakan batch
 * bersifat OPSIONAL per produk: baris ini hanya tercipta bila payload penerimaan (Kulakan)
 * menyertakan {@code nomor_batch} DAN {@code tanggal_expired} sekaligus (lihat
 * {@code KantinHelper.tambahPenerimaanBatch}) -- produk yang tidak pernah dikirim data lot tidak
 * akan punya baris {@code ProdukBatch} sama sekali, dan penjualannya murni mengandalkan stok
 * agregat seperti sebelum fitur ini ada.</p>
 *
 * <p><b>Konsumsi FEFO (First-Expired-First-Out) saat checkout</b> --
 * {@code KantinHelper.konsumsiBatchFefo} mengambil batch produk bersangkutan dengan filter
 * {@code status = }{@link #STATUS_AKTIF}, {@link #getTanggalExpired()} &ge; awal hari ini (batch
 * yang SUDAH lewat kedaluwarsa otomatis tidak dipertimbangkan), dan {@link #getStok()} {@code > 0},
 * diurutkan ASCENDING oleh {@link #getTanggalExpired()} -- lot yang paling dekat kedaluwarsa
 * dihabiskan lebih dulu. Baris dikunci {@code LockMode.UPGRADE} (SELECT ... FOR UPDATE) sebelum
 * dikurangi untuk mencegah dua checkout konkuren mengambil saldo lot yang sama. Bila total saldo
 * batch aktif tidak mencukupi qty yang diminta, KEKURANGANNYA SENGAJA DIBIARKAN pada stok agregat
 * (bukan dipaksakan negatif pada batch fisik) -- ditolak dengan {@code IllegalStateException}
 * kecuali toko/produk mengizinkan jual minus stok. Setiap perubahan saldo batch (penjualan,
 * kulakan, opname, transfer antar-outlet, pembatalan, karantina QC) dicatat sebagai satu baris
 * {@link MutasiProdukBatch} lewat {@code KantinHelper.catatMutasiBatch} -- kelas ini sendiri tidak
 * pernah menulis mutasi, hanya menyimpan saldo saat ini.</p>
 *
 * <p><b>Karantina QC (Fase E, dok. 48 P6):</b> {@code ProduksiApiHelper.buatQcAlertJikaPerlu}
 * mengubah {@link #getStatus()} batch ber-lot sama menjadi {@link #STATUS_KARANTINA} saat OUTPUT
 * produksi memuat produk ber-flag {@code perlu_qc} -- batch berstatus KARANTINA/{@link
 * #STATUS_DIMUSNAHKAN} otomatis TIDAK LOLOS filter FEFO di atas, sehingga secara efektif terkunci
 * dari penjualan tanpa perlu logika penolakan terpisah di jalur checkout.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "produk_batch", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "produk", "toko", "nomor_batch", "tanggal_expired" }) })
public class ProdukBatch extends GeneralValueObject {
	private static final long serialVersionUID = 1L;

	/** Status normal -- batch ikut dipertimbangkan mesin konsumsi FEFO saat checkout. */
	public static final String STATUS_AKTIF = "AKTIF";
	/**
	 * Batch ditahan (mis. hasil QC gagal, lihat javadoc kelas bagian "Karantina QC") -- TIDAK
	 * lolos filter FEFO, sehingga efektif tidak bisa dijual sampai dikembalikan ke
	 * {@link #STATUS_AKTIF} secara manual/proses lain.
	 */
	public static final String STATUS_KARANTINA = "KARANTINA";
	/** Batch dimusnahkan (mis. kedaluwarsa/rusak, dibuang dari peredaran) -- TIDAK lolos filter FEFO. */
	public static final String STATUS_DIMUSNAHKAN = "DIMUSNAHKAN";

	private Long id;
	private Produk produk;
	private Toko toko;
	private String nomorBatch;
	private Date tanggalProduksi;
	private Date tanggalExpired;
	private Double stok;
	private Double hargaModal;
	private String status;
	private String keterangan;
	private String oleh;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Kunci primer (identity, auto-generated DB). {@code null} sebelum baris batch pertama kali
	 * disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() { return id; }
	/** Setter {@link #getId()} -- normalnya hanya dipanggil Hibernate saat memuat baris dari DB. */
	public void setId(Long id) { this.id = id; }

	/**
	 * Produk pemilik lot ini, wajib. Getter memanggil
	 * {@link ais.database.model.GeneralValueObject#check(Object)} untuk meresolusi proxy lazy yang
	 * mungkin sudah <i>detached</i> dari session asalnya (pola getter relasi standar di seluruh
	 * entity AIS) sebelum mengembalikan field. {@code cascade PERSIST/MERGE}: menyimpan
	 * {@code ProdukBatch} baru ikut menyimpan/merge {@link Produk} terkait bila belum tersimpan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk", nullable = false)
	public Produk getProduk() { produk = check(produk); return produk; }
	/** Setter {@link #getProduk()}. */
	public void setProduk(Produk produk) { this.produk = produk; }

	/**
	 * Toko/outlet pemilik lot ini, wajib -- batch TIDAK dibagi lintas toko; lot yang sama secara
	 * fisik tapi diterima di toko berbeda tercatat sebagai baris {@code ProdukBatch} terpisah.
	 * Getter memanggil {@link ais.database.model.GeneralValueObject#check(Object)}, lihat javadoc
	 * {@link #getProduk()}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko", nullable = false)
	public Toko getToko() { toko = check(toko); return toko; }
	/** Setter {@link #getToko()}. */
	public void setToko(Toko toko) { this.toko = toko; }

	/**
	 * Nomor/kode lot fisik (label pabrik/produksi), wajib -- bersama {@link #getProduk()},
	 * {@link #getToko()}, {@link #getTanggalExpired()} membentuk kunci unik satu baris batch
	 * (lihat {@code @UniqueConstraint} pada kelas). Penerimaan berikutnya dengan kombinasi
	 * ke-4nya identik MENAMBAH saldo baris yang sama, bukan membuat baris baru (lihat
	 * {@code KantinHelper.tambahPenerimaanBatch}).
	 */
	@Column(name = "nomor_batch", nullable = false, length = 100)
	public String getNomorBatch() { return nomorBatch; }
	/** Setter {@link #getNomorBatch()}. */
	public void setNomorBatch(String nomorBatch) { this.nomorBatch = nomorBatch; }

	/** Tanggal produksi lot ini. Opsional, murni informatif -- tidak dipakai logika FEFO/konversi. */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_produksi")
	public Date getTanggalProduksi() { return tanggalProduksi; }
	/** Setter {@link #getTanggalProduksi()}. */
	public void setTanggalProduksi(Date tanggalProduksi) { this.tanggalProduksi = tanggalProduksi; }

	/**
	 * Tanggal kedaluwarsa lot ini, wajib -- kunci urutan konsumsi FEFO (lihat javadoc kelas) DAN
	 * bagian kunci unik baris. Batch dengan tanggal ini sudah lewat hari ini otomatis dikeluarkan
	 * dari perhitungan stok yang boleh dijual oleh {@code KantinHelper.konsumsiBatchFefo}, TANPA
	 * mengubah {@link #getStatus()} baris secara otomatis menjadi {@link #STATUS_DIMUSNAHKAN} --
	 * penandaan status kedaluwarsa (bila diperlukan pelaporan/alur pemusnahan) tetap proses manual
	 * terpisah.
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_expired", nullable = false)
	public Date getTanggalExpired() { return tanggalExpired; }
	/** Setter {@link #getTanggalExpired()}. */
	public void setTanggalExpired(Date tanggalExpired) { this.tanggalExpired = tanggalExpired; }

	/**
	 * Saldo lot ini yang masih dapat dipakai/dijual -- BUKAN stok agregat produk (lihat javadoc
	 * kelas). Getter null-safe: {@code null} dibaca sebagai {@code 0.0} (lot kosong/belum diisi
	 * dianggap tidak tersedia, bukan error). Diubah HANYA lewat {@code KantinHelper} (FEFO
	 * checkout, kulakan, opname, transfer, pembatalan) yang selalu mengiringi perubahan ini dengan
	 * satu baris {@link MutasiProdukBatch} -- mengubah field ini langsung tanpa mencatat mutasi
	 * akan membuat saldo dan buku besar batch tidak sinkron.
	 */
	@Column(nullable = false)
	public Double getStok() { return stok == null ? 0.0 : stok; }
	/** Setter {@link #getStok()} -- lihat javadoc getter untuk kewajiban mencatat mutasi pendamping. */
	public void setStok(Double stok) { this.stok = stok; }

	/**
	 * Harga modal/pokok per unit lot ini saat diterima (mis. dari Kulakan) -- dipakai sebagai
	 * dasar HPP batch-spesifik, terpisah dari {@link Produk#getHargaBeli()} agregat yang bisa
	 * berbeda antar penerimaan. Getter null-safe: {@code null} dibaca sebagai {@code 0.0}.
	 */
	@Column(name = "harga_modal")
	public Double getHargaModal() { return hargaModal == null ? 0.0 : hargaModal; }
	/** Setter {@link #getHargaModal()}. */
	public void setHargaModal(Double hargaModal) { this.hargaModal = hargaModal; }

	/**
	 * Status siklus hidup lot: {@link #STATUS_AKTIF} (default), {@link #STATUS_KARANTINA}, atau
	 * {@link #STATUS_DIMUSNAHKAN} -- lihat javadoc masing-masing konstanta untuk efeknya terhadap
	 * kelayakan jual. Getter null-safe: baris lama/baru tanpa nilai eksplisit dibaca sebagai
	 * {@link #STATUS_AKTIF}.
	 */
	@Column(nullable = false, length = 20)
	public String getStatus() { return status == null ? STATUS_AKTIF : status; }
	/**
	 * Setter {@link #getStatus()} -- TIDAK memvalidasi nilai terhadap ketiga konstanta
	 * {@code STATUS_*}; nilai bebas apa pun akan tersimpan apa adanya dan diperlakukan seperti
	 * status tak dikenal (tidak lolos filter {@code status = STATUS_AKTIF} pada FEFO, sama efeknya
	 * dengan karantina meski labelnya berbeda). Pemanggil bertanggung jawab hanya memakai ketiga
	 * konstanta ini.
	 */
	public void setStatus(String status) { this.status = status; }

	/** Catatan bebas tentang lot ini (mis. alasan karantina/pemusnahan). Opsional. */
	@Column(columnDefinition = "text")
	public String getKeterangan() { return keterangan; }
	/** Setter {@link #getKeterangan()}. */
	public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

	/**
	 * Nama/identitas petugas yang membuat/mengubah baris batch ini -- jejak audit tampilan.
	 * BERBEDA dari pola {@code oleh} pada {@link GrupProduk}/{@link JenisProduk}/
	 * {@link SatuanProduk}: setter di sini TIDAK menjaga terhadap masukan {@code null}/kosong,
	 * jadi nilai kosong/{@code null} langsung menimpa jejak audit sebelumnya.
	 */
	public String getOleh() { return oleh; }
	/** Setter {@link #getOleh()} -- tanpa penjaga null/kosong, lihat javadoc getter. */
	public void setOleh(String oleh) { this.oleh = oleh; }

	/**
	 * Waktu baris ini terakhir diubah. Diinisialisasi ke waktu instansiasi objek pada deklarasi
	 * field, dan diperbarui otomatis oleh {@link #onUpdate()} setiap {@code UPDATE}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() { return tanggal_dirubah; }
	/** Setter {@link #getTanggal_dirubah()} -- normalnya hanya dipanggil {@link #onUpdate()}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }

	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil otomatis Hibernate tepat sebelum tiap {@code UPDATE}
	 * baris ini, mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah}
	 * yang menyetel {@link #tanggal_dirubah} ke waktu saat itu.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }
}
