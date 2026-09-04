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
import ais.database.model.koperasi.AnggotaKoperasi;

/**
 * Retur Penjualan -- barang yang dikembalikan PELANGGAN (rusak/salah kirim/tidak sesuai/dll),
 * TERPISAH dari {@link ReturBarang} (barang dikembalikan KE SUPPLIER, bagian alur Kulakan/Pengadaan).
 * Satu baris = satu produk yang diretur dari SATU transaksi penjualan asal ({@link
 * #getPembelianAnggotaKoperasiId()}) -- pola SAMA PERSIS {@link PengadaanProduk} (satu baris per SKU
 * per peristiwa, BUKAN header+item terpisah): retur 3 produk dari satu struk = 3 baris, semua
 * menunjuk {@code pembelianAnggotaKoperasiId} yang sama, tetap bisa dikelompokkan lewat kolom itu
 * utk laporan/tampilan.
 *
 * <p><b>Kenapa {@code pembelianAnggotaKoperasiId} bukan relasi JPA {@code @ManyToOne}</b> -- pola
 * sama {@code PembatalanTransaksiKantin.pembelianAnggotaKoperasiId}: transaksi penjualan LAMA/berdiri
 * sendiri (baris {@code koperasi.pembelian} tanpa header kelompok, lihat
 * {@code PosApi.prosesDetailTransaksi}) tidak selalu punya baris {@code pembelian_anggota_koperasi}
 * yang valid utk di-FK -- disimpan sebagai id polos + {@link #getKodeTransaksiAsal()} (salinan nomor
 * nota, utk tampilan tanpa perlu join) supaya retur tetap bisa dicatat thd transaksi jenis apa pun.</p>
 *
 * <p><b>Stok HANYA bertambah bila {@link #getKembalikanKeStok()} true</b> -- barang kondisi
 * rusak/tak layak jual TIDAK otomatis balik ke stok jual (praktik retail baku: barang rusak masuk
 * kategori write-off, bukan sellable inventory). Lihat {@code StokKantinUtil.recomputeStokProduk}
 * &amp; {@code formulaStokSql} (suku ke-5, ditambahkan bersamaan dgn entity ini) utk formula lengkap.</p>
 *
 * <p><b>KOREKSI per verifikasi 3 Sep 2026 terhadap kalimat paragraf pertama di atas</b> ("{@code
 * ReturBarang} = barang dikembalikan KE SUPPLIER"): itu menggambarkan niat desain awal
 * {@link ReturBarang}, BUKAN implementasi yang benar-benar berjalan. Penelusuran seluruh
 * pemanggil menunjukkan satu-satunya titik penulisan {@code ReturBarang} saat ini adalah
 * {@code PengirimanGudangUtil.terima()} -- mencatat porsi barang berkondisi rusak saat MENERIMA
 * kiriman ANTAR GUDANG/toko internal, bukan retur ke pemasok eksternal sama sekali. Jadi
 * perbedaan yang akurat antara kedua entity ini BUKAN "pelanggan vs supplier", melainkan
 * "retur dari pelanggan lewat POS penjualan" ({@code ReturPenjualan}, dengan alur create eksplisit
 * dan gerbang hak akses -- lihat catatan approval di bawah) vs "kondisi-rusak otomatis saat
 * penerimaan transfer stok antar lokasi" ({@code ReturBarang}, dibuat sistem sebagai efek samping
 * satu fitur lain, tanpa layar CRUD/approval sendiri). Kedua entity tidak tumpang tindih data dan
 * bukan superclass/subclass satu sama lain -- keduanya independen, hanya kebetulan sama-sama
 * "mencatat barang yang tidak jadi terjual sesuai transaksi awal".</p>
 *
 * <p><b>Gerbang pembuatan -- role-gate satu langkah, BUKAN alur persetujuan dua pihak
 * (maker-checker)</b>: baris ini dibuat lewat {@code KantinHelper.returPenjualanSimpan()}, yang
 * dipagari {@code bolehAksiCrud(tbmuser, ..., "returpenjualan", "create")} -- hanya admin/manager
 * global atau {@link Pedagang#getSupervisor() Pedagang supervisor} yang boleh membuat/mengubah
 * baris ini (javadoc method itu menyebutnya "Gated Supervisor", pola sama dengan
 * {@code kulakanSimpan}). Entity ini SENGAJA TIDAK punya kolom {@code status}/{@code disetujui}
 * apa pun: begitu tersimpan, retur langsung berlaku dan stok langsung dihitung ulang (bila
 * {@link #getKembalikanKeStok()} true) -- tidak ada tahap kedua "disetujui pihak lain" setelah
 * baris dibuat. Konsekuensinya seorang supervisor tunggal BISA membuat DAN "menyetujui" (dalam
 * arti langsung berlaku) returnya sendiri tanpa campur tangan pihak kedua -- pola ini konsisten
 * dengan kategori self-approval yang sudah tercatat berulang di modul lain pada inisiatif audit
 * keamanan AIS (bukan temuan baru yang unik pada entity ini, hanya instansiasi lain dari pola yang
 * sama).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "retur_penjualan")
public class ReturPenjualan extends GeneralValueObject {

	private static final long serialVersionUID = 1L;
	/** Primary key baris {@code koperasi.retur_penjualan}. Lihat {@link #getId()}. */
	private Long id;
	/** Produk yang diretur. Lihat {@link #getProduk()}. */
	private Produk produk;
	/** Toko tempat transaksi asal dan retur ini terjadi. Lihat {@link #getToko()}. */
	private Toko toko;

	/** Id header transaksi kelompok asal (BUKAN relasi JPA); lihat javadoc kelas dan {@link #getPembelianAnggotaKoperasiId()}. */
	private Long pembelianAnggotaKoperasiId;
	/** ID baris koperasi.pembelian yang benar-benar dikembalikan. */
	private Long pembelianId;
	/** Salinan nomor nota transaksi asal untuk tampilan tanpa join. Lihat {@link #getKodeTransaksiAsal()}. */
	private String kodeTransaksiAsal;
	/** Anggota koperasi pembeli, bila transaksi asal dilakukan anggota. Lihat {@link #getAnggotaKoperasi()}. */
	private AnggotaKoperasi anggotaKoperasi;
	/** Nama pembeli non-anggota (walk-in), sebagai alternatif {@link #anggotaKoperasi}. Lihat {@link #getNamaPembeli()}. */
	private String namaPembeli;

	/** Jumlah produk yang diretur. Lihat {@link #getQty()}. */
	private Double qty;
	/** Harga satuan produk saat transaksi asal, dasar {@link #getTotalNilai()}. Lihat {@link #getHargaSatuan()}. */
	private Double hargaSatuan;
	/** Total nilai retur; dihitung malas dari qty×hargaSatuan bila kosong. Lihat {@link #getTotalNilai()}. */
	private Double totalNilai;

	/** Alasan retur (rusak/salah kirim/tidak sesuai/dll). Lihat {@link #getAlasan()}. */
	private String alasan;
	/** Kondisi fisik barang saat diretur. Lihat {@link #getKondisiBarang()}. */
	private String kondisiBarang;
	/** Penentu apakah qty ditambahkan kembali ke stok jual. Lihat javadoc lengkap di {@link #getKembalikanKeStok()}. */
	private Boolean kembalikanKeStok;
	/** Metode pengembalian dana/nilai ke pembeli (tunai/kredit poin/dll). Lihat {@link #getMetodePengembalian()}. */
	private String metodePengembalian;
	/** Keterangan bebas. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Waktu retur dicatat. Lihat {@link #getWaktu()}. */
	private Date waktu;
	/**
	 * Nama pengguna pengubah terakhir -- field audit generik <b>shadow</b> milik
	 * {@link GeneralValueObject} (WAJIB dideklarasikan ulang per entity konkret; lihat javadoc
	 * {@link GeneralValueObject#getOleh()}). Di kelas ini field ini SEKALIGUS berfungsi sebagai
	 * satu-satunya jejak "siapa yang mencatat retur" -- tidak ada field terpisah seperti
	 * {@code kasirNama} pada {@link ais.database.model.inventory.SesiKasKasir}, karena pencatat
	 * retur pasti pengguna yang sedang login (lewat gerbang
	 * {@code KantinHelper.bolehAksiCrud(..., "returpenjualan", "create")}), bukan identitas yang
	 * disalin lintas perangkat seperti pada sesi kas offline-first.
	 */
	private String oleh;
	/**
	 * Hook {@code @PreUpdate} Hibernate: menyinkronkan {@link #tanggal_dirubah} lewat
	 * {@code AuditTimestampInterceptor.ubah(this)} setiap kali baris ini diperbarui. Implementasi
	 * kontrak {@link GeneralValueObject#onUpdate()}; isinya tipis karena logika stempel waktu
	 * dipusatkan di interceptor bersama.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Constructor default tanpa argumen, dibutuhkan Hibernate untuk hidrasi entity. */
	public ReturPenjualan() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * @return primary key, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Tanpa validasi.
	 *
	 * @param id nilai primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan produk yang diretur, dengan proxy lazy diresolusi lewat {@link #check(Object)}.
	 * Relasi WAJIB terisi ({@code nullable = false}) -- satu baris {@code ReturPenjualan} selalu
	 * mewakili tepat satu SKU (lihat pola "satu baris per SKU per peristiwa" pada javadoc kelas).
	 *
	 * @return produk yang diretur, tidak boleh {@code null} pada baris tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk", nullable = false)
	public Produk getProduk() {
		produk = check(produk);
		return produk;
	}

	/**
	 * Menyetel produk yang diretur. Tanpa validasi.
	 *
	 * @param produk produk baru
	 */
	public void setProduk(Produk produk) {
		this.produk = produk;
	}

	/**
	 * Mengembalikan toko tempat transaksi asal dan retur ini terjadi, dengan proxy lazy diresolusi
	 * lewat {@link #check(Object)}. Berbeda dari {@link Pedagang#getToko()}, getter ini
	 * <b>tidak</b> punya fallback ke {@code Common.getCurrentToko()} meski kolomnya
	 * {@code nullable = false} di database.
	 *
	 * @return toko terkait, tidak boleh {@code null} pada baris tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko", nullable = false)
	public Toko getToko() {
		toko = check(toko);
		return toko;
	}

	/**
	 * Menyetel toko terkait. Tanpa validasi.
	 *
	 * @param toko toko baru
	 */
	public void setToko(Toko toko) {
		this.toko = toko;
	}

	/**
	 * Mengembalikan id header transaksi kelompok asal ({@code koperasi.pembelian_anggota_koperasi})
	 * -- <b>disimpan sebagai id polos ({@code Long}), BUKAN relasi JPA {@code @ManyToOne}</b>.
	 * Lihat javadoc kelas untuk alasan lengkapnya: transaksi lama/berdiri sendiri (baris
	 * {@code koperasi.pembelian} tanpa header kelompok) tidak selalu punya baris
	 * {@code pembelian_anggota_koperasi} yang valid untuk di-FK, sehingga id polos dipakai supaya
	 * retur tetap bisa dicatat terhadap transaksi jenis apa pun. Karena bukan relasi JPA,
	 * Hibernate TIDAK memvalidasi/menjamin integritas referensial nilai ini -- pemanggil yang
	 * butuh data header harus melakukan query eksplisit sendiri (id yatim/tak valid tidak akan
	 * memicu error di jalur simpan).
	 *
	 * @return id header transaksi kelompok, atau {@code null} bila transaksi asal tidak berkelompok
	 */
	@Column(name = "pembelian_anggota_koperasi_id", nullable = true)
	public Long getPembelianAnggotaKoperasiId() {
		return pembelianAnggotaKoperasiId;
	}

	/**
	 * Menyetel id header transaksi kelompok asal. Tanpa validasi/FK.
	 *
	 * @param pembelianAnggotaKoperasiId id header baru, boleh {@code null}
	 */
	public void setPembelianAnggotaKoperasiId(Long pembelianAnggotaKoperasiId) {
		this.pembelianAnggotaKoperasiId = pembelianAnggotaKoperasiId;
	}

	/**
	 * Mengembalikan id baris {@code koperasi.pembelian} yang secara spesifik dikembalikan --
	 * granularitas lebih halus dari {@link #getPembelianAnggotaKoperasiId()} (yang menunjuk
	 * header/kelompok transaksi, bisa berisi banyak baris produk). Sama seperti field id polos
	 * lainnya di kelas ini, BUKAN relasi JPA -- tanpa validasi FK di sisi Hibernate.
	 *
	 * @return id baris pembelian asal, boleh {@code null}
	 */
	@Column(name = "pembelian_id", nullable = true)
	public Long getPembelianId() {
		return pembelianId;
	}

	/**
	 * Menyetel id baris pembelian asal. Tanpa validasi/FK.
	 *
	 * @param pembelianId id baris pembelian baru, boleh {@code null}
	 */
	public void setPembelianId(Long pembelianId) {
		this.pembelianId = pembelianId;
	}

	/**
	 * Mengembalikan salinan nomor nota transaksi asal -- disimpan sebagai teks agar layar
	 * tampilan/laporan bisa menunjukkan nomor nota TANPA perlu join ke tabel transaksi (yang,
	 * seperti dijelaskan pada {@link #getPembelianAnggotaKoperasiId()}, tidak selalu bisa dijamin
	 * ada lewat FK).
	 *
	 * @return kode/nomor nota transaksi asal, boleh {@code null}
	 */
	public String getKodeTransaksiAsal() {
		return kodeTransaksiAsal;
	}

	/**
	 * Menyetel salinan nomor nota transaksi asal. Tanpa validasi -- tidak disinkronkan otomatis
	 * dengan {@link #getPembelianAnggotaKoperasiId()}/{@link #getPembelianId()}; pemanggil
	 * bertanggung jawab menjaga konsistensi ketiganya.
	 *
	 * @param kodeTransaksiAsal kode transaksi baru, boleh {@code null}
	 */
	public void setKodeTransaksiAsal(String kodeTransaksiAsal) {
		this.kodeTransaksiAsal = kodeTransaksiAsal;
	}

	/**
	 * Mengembalikan anggota koperasi pembeli pada transaksi asal, bila transaksi dilakukan oleh
	 * anggota (proxy lazy diresolusi lewat {@link #check(Object)}). Untuk pembeli non-anggota,
	 * lihat {@link #getNamaPembeli()} sebagai alternatif.
	 *
	 * @return anggota koperasi pembeli, atau {@code null} bila pembeli bukan anggota/tidak tercatat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "anggota_koperasi", nullable = true)
	public AnggotaKoperasi getAnggotaKoperasi() {
		anggotaKoperasi = check(anggotaKoperasi);
		return anggotaKoperasi;
	}

	/**
	 * Menyetel anggota koperasi pembeli. Tanpa validasi.
	 *
	 * @param anggotaKoperasi anggota koperasi baru, boleh {@code null}
	 */
	public void setAnggotaKoperasi(AnggotaKoperasi anggotaKoperasi) {
		this.anggotaKoperasi = anggotaKoperasi;
	}

	/**
	 * Mengembalikan nama pembeli non-anggota (walk-in) pada transaksi asal -- alternatif
	 * {@link #getAnggotaKoperasi()} untuk transaksi yang pembelinya bukan anggota koperasi
	 * terdaftar. Kedua field ini TIDAK saling eksklusif secara skema (tidak ada constraint yang
	 * memaksa hanya satu terisi); pemanggil menentukan mana yang relevan berdasarkan jenis
	 * transaksi asal.
	 *
	 * @return nama pembeli non-anggota, boleh {@code null}
	 */
	public String getNamaPembeli() {
		return namaPembeli;
	}

	/**
	 * Menyetel nama pembeli non-anggota. Tanpa validasi.
	 *
	 * @param namaPembeli nama pembeli baru, boleh {@code null}
	 */
	public void setNamaPembeli(String namaPembeli) {
		this.namaPembeli = namaPembeli;
	}

	/**
	 * Mengembalikan jumlah produk yang diretur -- dikalikan {@link #getHargaSatuan()} sebagai
	 * dasar {@link #getTotalNilai()}.
	 *
	 * @return qty retur, {@code 0.0} bila belum diisi
	 */
	public Double getQty() {
		return qty == null ? 0.0 : qty;
	}

	/**
	 * Menyetel qty retur. Tanpa validasi (termasuk tidak menolak nilai negatif/melebihi qty
	 * transaksi asal).
	 *
	 * @param qty qty baru, boleh {@code null}
	 */
	public void setQty(Double qty) {
		this.qty = qty;
	}

	/**
	 * Mengembalikan harga satuan produk saat transaksi asal.
	 *
	 * @return harga satuan, {@code 0.0} bila belum diisi
	 */
	public Double getHargaSatuan() {
		return hargaSatuan == null ? 0.0 : hargaSatuan;
	}

	/**
	 * Menyetel harga satuan. Tanpa validasi.
	 *
	 * @param hargaSatuan harga satuan baru, boleh {@code null}
	 */
	public void setHargaSatuan(Double hargaSatuan) {
		this.hargaSatuan = hargaSatuan;
	}

	/**
	 * Mengembalikan total nilai retur ({@code qty × hargaSatuan}), dengan <b>pola "getter
	 * destruktif" (compute-and-cache)</b> yang berulang di beberapa entity finansial AIS: bila
	 * {@link #totalNilai} masih {@code null} ATAU kebetulan bernilai {@code 0.0}, method ini
	 * MENGHITUNG ULANG dari {@link #getQty()}{@code × }{@link #getHargaSatuan()} dan
	 * <b>menyimpan hasilnya balik ke field {@link #totalNilai}</b> sebelum mengembalikannya --
	 * getter ini punya efek samping mengubah state object, bukan sekadar membaca.
	 *
	 * <p><b>Konsekuensi/perilaku yang perlu diwaspadai:</b></p>
	 * <ul>
	 *   <li><b>Retur senilai nol yang sah akan selalu dihitung ulang.</b> Kondisi
	 *   {@code totalNilai == 0.0} diperlakukan SAMA seperti {@code totalNilai == null} --
	 *   sehingga retur produk gratis/promosi (nilai memang {@code 0.0} secara sah, mis. sample
	 *   produk yang diretur) tidak pernah "menempel" nilai nolnya: getter akan terus mencoba
	 *   menghitung ulang qty×hargaSatuan setiap kali dipanggil selama hasilnya kebetulan tetap
	 *   nol. Ini tidak menimbulkan bug data (hasilnya tetap benar, nol tetap nol), tetapi berarti
	 *   asumsi "nilai tersimpan tidak pernah dihitung ulang setelah pertama kali diisi" TIDAK
	 *   berlaku untuk kasus nol.</li>
	 *   <li><b>Nilai bisa menjadi stale (usang) bila {@link #qty}/{@link #hargaSatuan} diubah
	 *   SETELAH {@code totalNilai} sempat ter-cache non-nol.</b> Karena kondisi pemicu hitung ulang
	 *   adalah {@code totalNilai == null || totalNilai == 0.0}, sekali field ini terisi angka
	 *   bukan-nol (baik dari perhitungan otomatis maupun {@link #setTotalNilai(Double)} manual),
	 *   perubahan {@link #setQty(Double)}/{@link #setHargaSatuan(Double)} berikutnya TIDAK memicu
	 *   penghitungan ulang -- getter akan terus mengembalikan angka lama sampai
	 *   {@link #setTotalNilai(Double)} dipanggil ulang secara eksplisit (mis. {@code null} atau
	 *   {@code 0.0}) atau baris dimuat ulang dari database (field in-memory di-reset kosong).</li>
	 *   <li>Pola compute-and-cache di getter ini SAMA PERSIS dengan
	 *   {@link ais.database.model.inventory.ReturBarang#getTotal()} -- lihat javadoc di sana untuk
	 *   perbandingan langsung; keduanya adalah instansiasi berulang dari pola arsitektur "getter
	 *   destruktif" yang sudah tercatat di seluruh inisiatif dokumentasi ini, bukan bug baru yang
	 *   unik pada entity ini.</li>
	 * </ul>
	 *
	 * @return total nilai retur, dihitung ulang dari qty×hargaSatuan bila field masih kosong/nol
	 */
	public Double getTotalNilai() {
		if (totalNilai == null || totalNilai == 0.0) {
			totalNilai = getQty() * getHargaSatuan();
		}
		return totalNilai;
	}

	/**
	 * Menyetel total nilai retur secara langsung, melewati perhitungan otomatis
	 * {@link #getTotalNilai()}. Tanpa validasi terhadap konsistensi qty×hargaSatuan.
	 *
	 * @param totalNilai total nilai baru, boleh {@code null}
	 */
	public void setTotalNilai(Double totalNilai) {
		this.totalNilai = totalNilai;
	}

	/**
	 * Mengembalikan alasan retur (rusak/salah kirim/tidak sesuai/dll) -- teks bebas, tidak
	 * dibatasi daftar enum tertentu di sisi entity.
	 *
	 * @return alasan retur, boleh {@code null}
	 */
	public String getAlasan() {
		return alasan;
	}

	/**
	 * Menyetel alasan retur. Tanpa validasi.
	 *
	 * @param alasan alasan baru, boleh {@code null}
	 */
	public void setAlasan(String alasan) {
		this.alasan = alasan;
	}

	/**
	 * Mengembalikan kondisi fisik barang saat diretur -- teks bebas yang secara tidak langsung
	 * menjadi dasar pertimbangan petugas mengisi {@link #getKembalikanKeStok()} (lihat javadoc di
	 * sana), tetapi TIDAK ada validasi otomatis yang mengaitkan isi teks ini dengan nilai flag
	 * tersebut -- keduanya diisi independen oleh petugas.
	 *
	 * @return kondisi barang, boleh {@code null}
	 */
	public String getKondisiBarang() {
		return kondisiBarang;
	}

	/**
	 * Menyetel kondisi barang. Tanpa validasi.
	 *
	 * @param kondisiBarang kondisi baru, boleh {@code null}
	 */
	public void setKondisiBarang(String kondisiBarang) {
		this.kondisiBarang = kondisiBarang;
	}

	/**
	 * Apakah qty retur ini ditambahkan kembali ke stok jual -- default {@code true} (kebanyakan retur
	 * BUKAN karena barang rusak: salah pilih/ukuran/berubah pikiran, barang masih layak jual). Petugas
	 * WAJIB melepas centang ini secara eksplisit utk retur barang rusak/tak layak jual, supaya barang
	 * tsb tidak diam-diam kembali ke stok yang bisa dijual lagi ke pelanggan lain.
	 */
	@Column(name = "kembalikan_ke_stok", nullable = true)
	public Boolean getKembalikanKeStok() {
		return kembalikanKeStok == null ? true : kembalikanKeStok;
	}

	/**
	 * Menyetel flag kembalikan-ke-stok. Tanpa validasi. Lihat {@link #getKembalikanKeStok()} untuk
	 * konsekuensi lengkap nilai {@code null}/{@code false}/{@code true}.
	 *
	 * @param kembalikanKeStok flag baru, boleh {@code null} (diperlakukan sebagai {@code true} saat
	 *                         dibaca)
	 */
	public void setKembalikanKeStok(Boolean kembalikanKeStok) {
		this.kembalikanKeStok = kembalikanKeStok;
	}

	/**
	 * Mengembalikan metode pengembalian dana/nilai ke pembeli (mis. tunai, kredit poin, tukar
	 * barang) -- teks bebas, tidak dibatasi enum di sisi entity.
	 *
	 * @return metode pengembalian, boleh {@code null}
	 */
	public String getMetodePengembalian() {
		return metodePengembalian;
	}

	/**
	 * Menyetel metode pengembalian. Tanpa validasi.
	 *
	 * @param metodePengembalian metode baru, boleh {@code null}
	 */
	public void setMetodePengembalian(String metodePengembalian) {
		this.metodePengembalian = metodePengembalian;
	}

	/**
	 * Mengembalikan keterangan bebas baris retur ini.
	 *
	 * @return keterangan, boleh {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel keterangan bebas. Tanpa validasi.
	 *
	 * @param keterangan keterangan baru, boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan waktu retur ini dicatat, dengan default waktu SEKARANG bila kolom kosong
	 * (dihitung ulang tiap pemanggilan, tidak disimpan balik ke field).
	 *
	 * @return waktu retur, tidak pernah {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
	}

	/**
	 * Menyetel waktu retur. Tanpa validasi.
	 *
	 * @param waktu waktu baru, boleh {@code null} (lihat {@link #getWaktu()} untuk fallback)
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Mengembalikan nama pengguna yang mencatat retur ini (sekaligus metadata audit generik --
	 * lihat javadoc field {@link #oleh} untuk penjelasan kenapa tidak ada field identitas
	 * terpisah seperti pada {@code SesiKasKasir}).
	 *
	 * @return nama pengguna pencatat, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menyetel nama pengguna pencatat retur. Tanpa validasi penolakan nilai kosong di kelas ini
	 * (berbeda dari {@link GeneralValueObject#setOleh(String)}) -- langsung menimpa field apa
	 * adanya.
	 *
	 * @param oleh nama pengguna baru, boleh {@code null}/kosong (langsung menimpa)
	 */
	public void setOleh(String oleh) {
		this.oleh = oleh;
	}


	/**
	 * Penanda jurnal. Jurnal retur jual: debet Pendapatan, kredit Kas/Piutang (+ balik HPP bila masuk stok). Diisi saat baris ini diposting ke buku besar; dipakai
	 * sebagai kunci anti-posting-ganda dan jejak balik dari jurnal ke dokumen sumbernya.
	 * Kolomnya dibuat otomatis oleh Hibernate.
	 */
	private ais.database.model.akunting.PostingHistory postingHistory;

	/**
	 * Mengembalikan jejak posting jurnal akuntansi baris retur ini, tanpa resolusi lazy lewat
	 * {@link #check(Object)} (berbeda dari getter relasi lain di kelas ini -- perhatikan ini bila
	 * relasi diakses setelah session Hibernate tertutup, risiko
	 * {@code LazyInitializationException} lebih tinggi di sini dibanding getter relasi lain pada
	 * entity ini). {@code null} berarti baris ini belum diposting ke buku besar.
	 *
	 * @return riwayat posting jurnal terkait, atau {@code null} bila belum diposting
	 */
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "posting_history", nullable = true)
	public ais.database.model.akunting.PostingHistory getPostingHistory() {
		postingHistory = check(postingHistory);
		return postingHistory;
	}

	/**
	 * Menyetel jejak posting jurnal. Tanpa validasi -- normalnya diisi otomatis oleh proses
	 * posting jurnal, bukan dipanggil manual (menyetel nilai ini secara langsung tanpa benar-benar
	 * membuat baris jurnal akan membuat baris ini tampak "sudah diposting" padahal belum).
	 *
	 * @param postingHistory riwayat posting baru, boleh {@code null}
	 */
	public void setPostingHistory(ais.database.model.akunting.PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

}
