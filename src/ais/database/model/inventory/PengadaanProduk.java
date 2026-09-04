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
import ais.database.model.asset.PenyediaAsset;

/**
 * Baris REALISASI pengadaan -- SATU baris produk yang sungguh-sungguh masuk gudang/toko lewat
 * kulakan/pembelian, dampak stok &amp; harga beli LANGSUNG diproses saat baris ini disimpan (lihat
 * {@code KantinHelper.kulakanFakturSimpan}: rekonsiliasi saldo awal legacy, penambahan batch
 * penerimaan, recompute stok native, dan opsional pembaruan harga beli MASTER produk) -- BEDA dari
 * {@link PengadaanFaktur} (header administratif faktur, tidak menyentuh stok sendiri) dan dari
 * {@link ais.database.model.inventory.PengajuanPembelianGudang} (murni "antrean kerja" sebelum ada
 * barang masuk sama sekali, TIDAK berelasi FK ke kelas ini -- lihat Javadoc kelas itu).
 *
 * <p><b>Dua jalur input, satu tabel</b>: baris LAMA (sebelum gap-closure 2026-08-11) dibuat satu
 * per satu lewat {@code kulakanSimpan} tanpa header sama sekali ({@link #fakturPengadaan}
 * {@code null} selamanya, {@link #nomorFaktur}/{@link #namaSupplier} diketik ulang manual tiap
 * baris tanpa validasi konsistensi); baris BARU dibuat massal dalam SATU transaksi oleh
 * {@code kulakanFakturSimpan}, terikat ke satu {@link PengadaanFaktur} lewat {@link #fakturPengadaan}
 * -- kedua bentuk data hidup berdampingan selamanya di tabel yang sama, kode pembaca (laporan,
 * hutang) harus toleran terhadap {@link #fakturPengadaan} {@code null}.</p>
 *
 * <p><b>Supplier ganda, sengaja tidak disatukan</b>: field {@link #supplier} di sini menunjuk
 * {@link PenyediaAsset} (dipakai form JSP lama), SEDANGKAN {@link PengadaanFaktur#getSupplier()}
 * pada header baru menunjuk {@link ais.database.model.library.Penyedia} yang BERBEDA -- permintaan
 * eksplisit user saat header dibuat ("ambil dari class Penyedia") sengaja TIDAK memigrasikan/
 * menyatukan kolom lama ini; baris via jalur faktur baru mengisi {@link #namaSupplier} dari nama
 * {@code Penyedia} header (string salinan, bukan FK), sementara {@link #supplier} ({@code PenyediaAsset})
 * tetap kosong pada baris-baris tsb.</p>
 *
 * <p><b>Snapshot UOM pembelian</b> ({@link #satuanInput}, {@link #qtyInput}, {@link #faktorKonversi},
 * {@link #hargaBeliSatuanInput}) dicatat di samping {@link #qty}/{@link #hargaBeliSatuan} yang SELALU
 * dalam satuan DASAR/stok (hasil konversi qtyInput &times; faktorKonversi) -- pilihan desain supaya
 * seluruh rumus stok &amp; HPP lama yang sudah ada tetap bekerja tanpa perubahan, sementara tampilan
 * &amp; input tetap bisa memakai satuan pembelian asli (mis. DUS) tanpa staf menghitung konversi
 * manual. Baris lama (sebelum fitur UOM, sebelum {@code r78484}) punya kolom snapshot ini kosong --
 * getter masing-masing null-safe, fallback ke nilai dasar/1.0 (lihat Javadoc getter terkait).</p>
 *
 * <p><b>Jurnal akunting</b> ({@link #postingHistory}): baris ini dijurnal (debet Persediaan, kredit
 * Utang Supplier/Kas) sebagai dokumen sumber terpisah dari {@link PengadaanFaktur} header -- setiap
 * baris {@link PengadaanProduk} punya jurnalnya sendiri, BUKAN satu jurnal gabungan per faktur,
 * konsisten dgn model akunting AIS yang menjurnal per baris transaksi/dokumen sumber granular.</p>
 *
 * @see GeneralValueObject
 * @see PengadaanFaktur
 * @see ais.database.model.inventory.PengajuanPembelianGudang
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "pengadaan_produk")
public class PengadaanProduk extends GeneralValueObject {

	/** Versi serialisasi tetap; dipertahankan hanya krn kontrak {@link GeneralValueObject}/
	 * {@code Serializable}. */
	private static final long serialVersionUID = 1L;
	/** PK auto-generated (identity). Lihat {@link #getId()}. */
	private Long id;
	/** Produk yang diadakan baris ini. Lihat {@link #getProduk()}. */
	private Produk produk;
	/** Toko pemilik baris pengadaan ini -- penentu kepemilikan/tenant. Lihat {@link #getToko()}. */
	private Toko toko;

	/** Nomor nota dari supplier -- diketik manual per baris pada jalur LAMA (tanpa header, lihat
	 * Javadoc kelas); pada jalur BARU nilainya salinan dari {@link PengadaanFaktur#getNomorFaktur()}
	 * header terkait. Lihat {@link #getNomorFaktur()}. */
	private String nomorFaktur; // Nomor nota dari supplier
	/** Nama supplier sebagai STRING SALINAN (bukan FK) -- lihat Javadoc {@link #getNamaSupplier()}
	 * soal bagaimana nilainya diturunkan dari {@link #supplier}. */
	private String namaSupplier;
	/** Supplier via {@link PenyediaAsset} (jalur LAMA/form JSP) -- lihat Javadoc kelas soal
	 * perbedaannya dari {@link PengadaanFaktur#getSupplier()} ({@code Penyedia}, jalur BARU). Lihat
	 * {@link #getSupplier()}. */
	private PenyediaAsset supplier;
	/** Header Kulakan-per-Faktur (gap-closure 2026-08-11) -- {@code nullable}, data lama tetap
	 * {@code null} selamanya (lihat Javadoc kelas). Lihat {@link #getFakturPengadaan()}. */
	private PengadaanFaktur fakturPengadaan; // Header Kulakan-per-Faktur (gap-closure 2026-08-11) -- nullable, data lama tetap null selamanya
	/** Jumlah barang yang masuk, dalam satuan DASAR/stok (hasil konversi {@link #qtyInput} &times;
	 * {@link #faktorKonversi}) -- lihat Javadoc kelas soal snapshot UOM. Lihat {@link #getQty()}. */
	private Double qty; // Jumlah barang yang masuk
	/** Harga beli per satuan DASAR/stok (hasil konversi {@link #hargaBeliSatuanInput} &divide;
	 * {@link #faktorKonversi}). Lihat {@link #getHargaBeliSatuan()}. */
	private Double hargaBeliSatuan;
	/** Total harga baris ini ({@link #qty} &times; {@link #hargaBeliSatuan}) -- lihat catatan
	 * penting soal getter DESTRUKTIF-nya di {@link #getTotalHarga()}. */
	private Double totalHarga;
	// Snapshot UOM pada saat dokumen disimpan. qty/hargaBeliSatuan tetap dalam
	// satuan stok/dasar agar seluruh rumus stok dan HPP lama tetap kompatibel.
	/** Satuan (UOM) yang dipakai staf saat menginput baris ini (mis. DUS) -- SNAPSHOT, lihat Javadoc
	 * kelas &amp; {@link #getSatuanInput()}. */
	private SatuanProduk satuanInput;
	/** Jumlah barang dalam {@link #satuanInput} (SEBELUM dikonversi ke {@link #qty} dasar) --
	 * snapshot UOM. Lihat {@link #getQtyInput()}. */
	private Double qtyInput;
	/** Faktor konversi {@link #satuanInput} ke satuan dasar SAAT baris ini disimpan -- snapshot UOM,
	 * TIDAK ikut berubah bila faktor konversi produk diubah belakangan. Lihat
	 * {@link #getFaktorKonversi()}. */
	private Double faktorKonversi;
	/** Harga beli per satuan {@link #satuanInput} (SEBELUM dikonversi ke {@link #hargaBeliSatuan}
	 * dasar) -- snapshot UOM. Lihat {@link #getHargaBeliSatuanInput()}. */
	private Double hargaBeliSatuanInput;

	/** Waktu barang ini diadakan/diterima. Lihat {@link #getWaktuPengadaan()}. */
	private Date waktuPengadaan;
	/** Catatan bebas ttg baris pengadaan ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Nama/id petugas yang mencatat baris ini. Lihat {@link #getOleh()}. */
	private String oleh;
	/**
	 * Callback JPA {@code @PreUpdate}: menandai kapan baris ini TERAKHIR diubah, dengan menuliskan
	 * waktu sekarang ke {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Dipanggil otomatis oleh
	 * Hibernate sebelum {@code UPDATE}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor tanpa argumen wajib JPA/Hibernate. */
	public PengadaanProduk() {
	}

	/**
	 * PK identity baris pengadaan ini. {@code null} sebelum entity di-{@code save}/{@code flush} ke
	 * Hibernate (ID baru dibuat DB saat insert, strategi {@link IDENTITY}).
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Setter PK -- dipanggil Hibernate saat memuat entity dari DB. Kode aplikasi normal tidak perlu
	 * memanggil ini; id baru dibuat otomatis oleh DB saat insert.
	 *
	 * @param id id baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Produk yang diadakan baris ini -- {@code nullable = false}. Getter memakai
	 * {@link GeneralValueObject#check(Object)}: menormalkan referensi ke instance kanonik dari cache
	 * identitas JVM-wide (lookup lazy dari cache/DB bila objek belum terinisialisasi penuh) SEBELUM
	 * dikembalikan -- pola dedup/refresh yang dipakai seluruh keluarga {@link GeneralValueObject},
	 * bukan hanya validasi field kosong.
	 *
	 * @return produk baris ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk", nullable = false)
	public Produk getProduk() {
		produk = check(produk);
		return produk;
	}

	/**
	 * Menetapkan produk baris ini.
	 *
	 * @param produk produk baru.
	 */
	public void setProduk(Produk produk) {
		this.produk = produk;
	}

	/**
	 * Toko pemilik baris pengadaan ini -- {@code nullable = false}, penentu kepemilikan/tenant.
	 * Getter memakai {@link GeneralValueObject#check(Object)} (lihat catatan pada Javadoc
	 * {@link #getProduk()}).
	 *
	 * @return toko pemilik baris ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko", nullable = false)
	public Toko getToko() {
		toko = check(toko);
		return toko;
	}

	/**
	 * Menetapkan toko pemilik baris ini.
	 *
	 * @param toko toko baru.
	 */
	public void setToko(Toko toko) {
		this.toko = toko;
	}

	/**
	 * Nomor nota dari supplier. Pada jalur LAMA diketik manual per baris (TANPA validasi konsistensi
	 * dgn baris lain di faktur yang sama secara fisik); pada jalur BARU (lihat Javadoc kelas) nilainya
	 * salinan otomatis dari {@link PengadaanFaktur#getNomorFaktur()} header via
	 * {@link #fakturPengadaan}.
	 *
	 * @return nomor nota/faktur, atau {@code null} bila tidak diisi.
	 */
	public String getNomorFaktur() {
		return nomorFaktur;
	}

	/**
	 * Menetapkan nomor nota/faktur baris ini.
	 *
	 * @param nomorFaktur nomor nota/faktur baru.
	 */
	public void setNomorFaktur(String nomorFaktur) {
		this.nomorFaktur = nomorFaktur;
	}

	/**
	 * Nama supplier sebagai STRING SALINAN (bukan FK/derivasi murni). <b>Getter dengan efek
	 * samping:</b> bila {@link #getSupplier()} (relasi {@link PenyediaAsset}) tidak {@code null},
	 * field {@link #namaSupplier} DITULIS ULANG dari {@code supplier.getNama()} SETIAP KALI getter
	 * ini dipanggil -- pola "getter yang menulis balik field lain" yang sudah berulang kali ditemukan
	 * di model finansial AIS lain. Bila {@link #supplier} {@code null} (mis. baris dari jalur faktur
	 * baru yang memakai {@code Penyedia} header, bukan {@code PenyediaAsset}), field {@link #namaSupplier}
	 * TIDAK disentuh dan nilai yang sudah tersimpan (mis. salinan nama dari header faktur) tetap
	 * dipertahankan apa adanya.
	 *
	 * @return nama supplier -- hasil sinkron dari {@link #supplier} bila relasi itu terisi, atau
	 *         nilai kolom apa adanya bila tidak.
	 */
	public String getNamaSupplier() {
		if (getSupplier() != null) {
			namaSupplier = supplier.getNama();
		}
		return namaSupplier;
	}

	/**
	 * Menetapkan nama supplier secara langsung. Dipakai jalur BARU (via faktur) yang mengisi nama
	 * dari {@code Penyedia} header tanpa mengisi relasi {@link #supplier} ({@code PenyediaAsset})
	 * sama sekali -- lihat Javadoc {@link #getNamaSupplier()} soal kapan nilai ini bisa ditimpa
	 * ulang oleh getter.
	 *
	 * @param namaSupplier nama supplier baru.
	 */
	public void setNamaSupplier(String namaSupplier) {
		this.namaSupplier = namaSupplier;
	}

	/**
	 * Jumlah barang yang masuk, dalam satuan DASAR/stok. Getter null-safe: mengembalikan
	 * {@code 0.0} bila kolom NULL di DB.
	 *
	 * @return qty dasar, tidak pernah {@code null}.
	 */
	public Double getQty() {
		return qty == null ? 0.0 : qty;
	}

	/**
	 * Menetapkan qty dasar baris ini. Tidak ada validasi &gt; 0 di level entity -- validasi tsb
	 * dilakukan helper simpan pemanggil ({@code KantinHelper.kulakanFakturSimpan}) sebelum baris
	 * dibangun.
	 *
	 * @param qty qty dasar baru.
	 */
	public void setQty(Double qty) {
		this.qty = qty;
	}

	/**
	 * Harga beli per satuan DASAR/stok. Getter null-safe: mengembalikan {@code 0.0} bila kolom NULL
	 * di DB.
	 *
	 * @return harga beli per satuan dasar, tidak pernah {@code null}.
	 */
	public Double getHargaBeliSatuan() {
		return hargaBeliSatuan == null ? 0.0 : hargaBeliSatuan;
	}

	/**
	 * Menetapkan harga beli per satuan dasar. Tidak ada validasi &gt; 0 di level entity -- lihat
	 * catatan serupa pada Javadoc {@link #setQty(Double)}.
	 *
	 * @param hargaBeliSatuan harga beli per satuan dasar baru.
	 */
	public void setHargaBeliSatuan(Double hargaBeliSatuan) {
		this.hargaBeliSatuan = hargaBeliSatuan;
	}

	/**
	 * Total harga baris ini ({@link #getQty()} &times; {@link #getHargaBeliSatuan()}).
	 * <b>Getter DESTRUKTIF/lazy-compute:</b> bila field {@link #totalHarga} kosong ({@code null})
	 * ATAU KEBETULAN BERNILAI PERSIS {@code 0.0} (bukan hanya {@code null}), field ini DIHITUNG ULANG
	 * &amp; DITULIS BALIK dari {@code qty * hargaBeliSatuan} SEBELUM dikembalikan -- lalu nilai hasil
	 * hitungan itu MENETAP di field (persisten pada save/flush berikutnya bila entity managed).
	 * Konsekuensi: baris yang qty/harganya sudah diubah lewat {@link #setQty(Double)}/
	 * {@link #setHargaBeliSatuan(Double)} SETELAH {@link #totalHarga} sempat dibaca-hitung sekali
	 * TIDAK otomatis ter-refresh lagi (nilai lama sudah "membeku" begitu bukan {@code null}/{@code 0}
	 * lagi) -- pemanggil yang mengubah qty/harga belakangan harus memanggil
	 * {@link #setTotalHarga(Double)} dgn {@code null} (atau {@code 0.0}) secara eksplisit dulu bila
	 * ingin total dihitung ulang. Pola "getter menulis balik ke field lain" ini konsisten dgn temuan
	 * berulang di model finansial AIS lain -- BUKAN dianggap bug krn tabel ini memang tidak
	 * ber-{@code @Transient} untuk kolom ini (nilai final memang dimaksudkan tersimpan, bukan murni
	 * turunan).
	 *
	 * @return total harga baris ini.
	 */
	public Double getTotalHarga() {
		if (totalHarga == null || totalHarga == 0.0) {
			totalHarga = getQty() * getHargaBeliSatuan();
		}
		return totalHarga;
	}

	/**
	 * Menetapkan total harga baris ini secara langsung. Dipakai helper simpan yang sudah menghitung
	 * total sendiri (mis. {@code qtyInput * hargaBeliSatuanInput}, bukan qty/harga dasar hasil
	 * konversi) -- lihat {@code KantinHelper.kulakanFakturSimpan}; memanggil dgn {@code null} akan
	 * memicu penghitungan ulang otomatis pada panggilan {@link #getTotalHarga()} berikutnya (lihat
	 * Javadoc getter).
	 *
	 * @param totalHarga total harga baru, atau {@code null} utk memicu hitung ulang otomatis.
	 */
	public void setTotalHarga(Double totalHarga) {
		this.totalHarga = totalHarga;
	}

	/**
	 * Satuan (UOM) yang dipakai staf saat menginput baris ini (mis. DUS) -- SNAPSHOT beku saat
	 * simpan, lihat Javadoc kelas. Getter memakai {@link GeneralValueObject#check(Object)} (lihat
	 * catatan pada Javadoc {@link #getProduk()}). {@code nullable}: baris lama (sebelum fitur UOM,
	 * {@code r78484}) tidak punya nilai ini.
	 *
	 * @return satuan input baris ini, atau {@code null} bila belum pernah diisi (data lama/pre-UOM).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_input", nullable = true)
	public SatuanProduk getSatuanInput() {
		satuanInput = check(satuanInput);
		return satuanInput;
	}

	/**
	 * Menetapkan satuan input baris ini.
	 *
	 * @param satuanInput satuan input baru.
	 */
	public void setSatuanInput(SatuanProduk satuanInput) {
		this.satuanInput = satuanInput;
	}

	/**
	 * Jumlah barang dalam {@link #getSatuanInput()} (SEBELUM konversi ke {@link #getQty()} dasar).
	 * Getter null-safe: mengembalikan {@link #getQty()} (qty dasar) bila kolom NULL -- fallback yang
	 * masuk akal utk baris lama pre-UOM di mana satuan input = satuan dasar (faktor konversi 1:1).
	 *
	 * @return qty dalam satuan input, tidak pernah {@code null}.
	 */
	@Column(name = "qty_input")
	public Double getQtyInput() {
		return qtyInput == null ? getQty() : qtyInput;
	}

	/**
	 * Menetapkan qty dalam satuan input.
	 *
	 * @param qtyInput qty satuan input baru.
	 */
	public void setQtyInput(Double qtyInput) {
		this.qtyInput = qtyInput;
	}

	/**
	 * Faktor konversi {@link #getSatuanInput()} ke satuan dasar SAAT baris ini disimpan (snapshot,
	 * tidak ikut berubah bila faktor konversi produk diubah belakangan -- lihat Javadoc kelas).
	 * Getter null-safe DAN clamp bawah: mengembalikan {@code 1.0} bila kolom NULL ATAU &le; 0 --
	 * mencegah pembagian dgn nol/negatif di kode pemanggil yang memakai nilai ini sbg pembagi/pengali
	 * (mis. {@code hargaDasar = hargaBeliSatuan / faktorKonversi} pada
	 * {@code KantinHelper.kulakanFakturSimpan}).
	 *
	 * @return faktor konversi, selalu &gt; 0, tidak pernah {@code null}.
	 */
	@Column(name = "faktor_konversi")
	public Double getFaktorKonversi() {
		return faktorKonversi == null || faktorKonversi.doubleValue() <= 0.0 ? Double.valueOf(1.0) : faktorKonversi;
	}

	/**
	 * Menetapkan faktor konversi. Tidak ada validasi &gt; 0 di level SETTER (clamp hanya ada di
	 * getter, lihat Javadoc {@link #getFaktorKonversi()}) -- menyimpan nilai &le; 0 lewat setter ini
	 * LOLOS ke DB apa adanya, baru dikoreksi saat DIBACA lewat getter.
	 *
	 * @param faktorKonversi faktor konversi baru.
	 */
	public void setFaktorKonversi(Double faktorKonversi) {
		this.faktorKonversi = faktorKonversi;
	}

	/**
	 * Harga beli per satuan {@link #getSatuanInput()} (SEBELUM konversi ke
	 * {@link #getHargaBeliSatuan()} dasar). Getter null-safe: mengembalikan
	 * {@link #getHargaBeliSatuan()} (harga dasar) bila kolom NULL -- fallback yang masuk akal utk
	 * baris lama pre-UOM.
	 *
	 * @return harga beli per satuan input, tidak pernah {@code null}.
	 */
	@Column(name = "harga_beli_satuan_input")
	public Double getHargaBeliSatuanInput() {
		return hargaBeliSatuanInput == null ? getHargaBeliSatuan() : hargaBeliSatuanInput;
	}

	/**
	 * Menetapkan harga beli per satuan input.
	 *
	 * @param hargaBeliSatuanInput harga beli per satuan input baru.
	 */
	public void setHargaBeliSatuanInput(Double hargaBeliSatuanInput) {
		this.hargaBeliSatuanInput = hargaBeliSatuanInput;
	}

	/**
	 * Waktu barang ini diadakan/diterima. Getter null-safe: mengembalikan waktu SEKARANG
	 * ({@link ais.ui.util.WaktuUtil#getDate()}) bila kolom NULL, dihitung ULANG setiap kali getter
	 * dipanggil pada baris yang kolomnya NULL (bukan waktu tetap saat objek dibuat).
	 *
	 * @return waktu pengadaan, atau waktu panggilan getter saat ini bila kolom NULL.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuPengadaan() {
		return waktuPengadaan == null ? ais.ui.util.WaktuUtil.getDate() : waktuPengadaan;
	}

	/**
	 * Menetapkan waktu pengadaan/penerimaan barang.
	 *
	 * @param waktuPengadaan waktu pengadaan baru.
	 */
	public void setWaktuPengadaan(Date waktuPengadaan) {
		this.waktuPengadaan = waktuPengadaan;
	}

	/**
	 * Catatan bebas ttg baris pengadaan ini.
	 *
	 * @return keterangan, atau {@code null}/kosong bila tidak diisi.
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menetapkan keterangan. Tidak ada guard null/blank -- memanggil dgn string kosong akan menimpa
	 * nilai lama.
	 *
	 * @param keterangan catatan bebas baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Nama/id petugas yang mencatat baris ini. Tidak ada guard null/blank pada getter/setter ini
	 * (beda dari pola {@link ais.database.model.koperasi.PayableFakturInfo#getOleh()}).
	 *
	 * @return nama/id petugas pencatat, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menetapkan {@link #oleh}. Tanpa guard -- lihat catatan pada Javadoc {@link #getOleh()}.
	 *
	 * @param oleh nama/id petugas pencatat baru.
	 */
	public void setOleh(String oleh) {
		this.oleh = oleh;
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

	/**
	 * Supplier via {@link PenyediaAsset} (jalur LAMA/form JSP) -- lihat Javadoc kelas soal
	 * perbedaannya dari {@link PengadaanFaktur#getSupplier()} ({@code Penyedia}, jalur BARU). Getter
	 * memakai {@link GeneralValueObject#check(Object)} (lihat catatan pada Javadoc
	 * {@link #getProduk()}). {@code nullable}: baris dari jalur faktur baru umumnya TIDAK mengisi
	 * relasi ini sama sekali (memakai {@link #namaSupplier} string salinan dari header
	 * {@code Penyedia} sebagai gantinya).
	 *
	 * @return supplier ({@code PenyediaAsset}) baris ini, atau {@code null} bila tidak diisi/tidak
	 *         relevan (jalur faktur baru).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "supplier", nullable = true)
	public PenyediaAsset getSupplier() {
		supplier = check(supplier);
		return supplier;
	}

	/**
	 * Menetapkan supplier ({@code PenyediaAsset}) baris ini. Mengubah nilai ini TIDAK otomatis
	 * menyinkronkan {@link #namaSupplier} -- sinkronisasi itu terjadi hanya lewat efek samping
	 * {@link #getNamaSupplier()} saat dipanggil (lihat Javadoc method itu).
	 *
	 * @param supplier supplier baru.
	 */
	public void setSupplier(PenyediaAsset supplier) {
		this.supplier = supplier;
	}

	/**
	 * Header Kulakan-per-Faktur (gap-closure 2026-08-11) -- lihat Javadoc kelas {@link PengadaanFaktur}
	 * &amp; javadoc kelas ini soal dua jalur input yang hidup berdampingan. {@code nullable}: baris
	 * dari jalur LAMA (sebelum gap-closure) punya nilai ini {@code null} SELAMANYA -- kode pembaca
	 * (laporan, hutang, agregasi per faktur) HARUS toleran terhadap {@code null} ini, bukan
	 * menganggapnya data rusak.
	 *
	 * @return header faktur induk baris ini, atau {@code null} bila baris dari jalur lama (tanpa
	 *         header) atau belum ditautkan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "faktur_pengadaan", nullable = true)
	public PengadaanFaktur getFakturPengadaan() {
		fakturPengadaan = check(fakturPengadaan);
		return fakturPengadaan;
	}

	/**
	 * Menetapkan header faktur induk baris ini. Dipakai saat membangun baris baru lewat jalur faktur
	 * (lihat {@code KantinHelper.kulakanFakturSimpan}); tidak ada guard di level entity yang mencegah
	 * baris ini ditautkan ke header dari toko yang berbeda -- konsistensi toko antara baris dan
	 * header (bila diperlukan) adalah tanggung jawab helper pemanggil.
	 *
	 * @param fakturPengadaan header faktur induk baru, atau {@code null} utk melepas tautan.
	 */
	public void setFakturPengadaan(PengadaanFaktur fakturPengadaan) {
		this.fakturPengadaan = fakturPengadaan;
	}


	/**
	 * Penanda jurnal. Jurnal kulakan: debet Persediaan, kredit Utang Supplier/Kas. Diisi saat baris ini diposting ke buku besar; dipakai
	 * sebagai kunci anti-posting-ganda dan jejak balik dari jurnal ke dokumen sumbernya.
	 * Kolomnya dibuat otomatis oleh Hibernate.
	 */
	private ais.database.model.akunting.PostingHistory postingHistory;

	/**
	 * Jejak balik ke jurnal akunting yang memposting baris ini (debet Persediaan, kredit Utang
	 * Supplier/Kas) -- lihat catatan field {@link #postingHistory}. {@code null} berarti baris ini
	 * BELUM diposting ke buku besar; dipakai sbg kunci anti-posting-ganda oleh proses posting
	 * (memeriksa field ini kosong sebelum membuat jurnal baru). Setiap baris {@link PengadaanProduk}
	 * punya jurnalnya sendiri-sendiri, BUKAN satu jurnal gabungan per {@link PengadaanFaktur} header
	 * -- lihat Javadoc kelas.
	 *
	 * @return riwayat posting jurnal baris ini, atau {@code null} bila belum diposting.
	 */
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "posting_pembelian", nullable = true)
	public ais.database.model.akunting.PostingHistory getPostingHistory() {
		postingHistory = check(postingHistory);
		return postingHistory;
	}

	/**
	 * Menetapkan jejak posting jurnal baris ini. Dipanggil proses posting SETELAH jurnal berhasil
	 * dibuat -- lihat catatan pada Javadoc {@link #getPostingHistory()} soal perannya sbg kunci
	 * anti-posting-ganda; setter ini sendiri tidak melakukan pengecekan/pembuatan jurnal apa pun,
	 * murni menyimpan referensinya.
	 *
	 * @param postingHistory riwayat posting jurnal baru.
	 */
	public void setPostingHistory(ais.database.model.akunting.PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

}
