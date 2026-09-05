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
import ais.database.model.library.Penyedia;

/**
 * Retur Pembelian (gap-closure roadmap Fase 3, permintaan user 2026-08-11) -- barang yang
 * dikembalikan KE SUPPLIER (rusak/salah kirim/tidak sesuai), kebalikan {@link ReturPenjualan}
 * (barang kembali DARI pelanggan). Menggantikan peran {@link ReturBarang} yang sebelumnya JSP-only,
 * tanpa header/supplier, dan SENGAJA tidak terintegrasi ke rumus stok (lihat JavaDoc di sana) --
 * fitur ini FULL terintegrasi: reachable dari JSP/Electron/Flutter, tertaut opsional ke
 * {@link PengadaanFaktur} asal, DAN mengurangi stok otomatis (lihat suku baru di
 * {@code StokKantinUtil.formulaStokSql}).
 *
 * <p>Pola SAMA PERSIS {@link ReturPenjualan}/{@link PengadaanProduk} -- satu baris per SKU per
 * peristiwa retur, dikelompokkan lewat {@link #getFakturPengadaan()} bila petugas menautkannya ke
 * faktur asal (opsional -- barang bisa saja diretur tanpa jejak faktur pengadaan yang jelas,
 * mis. data lama/barang titipan).</p>
 *
 * <p><b>TIDAK ADA flag "kembalikan ke stok"</b> (beda dari {@link ReturPenjualan}) -- retur
 * pembelian secara fisik SELALU berarti barang meninggalkan toko kembali ke supplier, jadi qty
 * SELALU mengurangi stok tanpa syarat, tidak ada skenario "barang tetap di toko tapi dicatat retur".</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "retur_pembelian")
public class ReturPembelian extends GeneralValueObject {

	/** Versi serialisasi Java. Nilai {@code 1L} bawaan karena kelas ini ditulis tangan, bukan hasil hbm2java. */
	private static final long serialVersionUID = 1L;
	/** Kunci utama {@code koperasi.retur_pembelian.id}, auto-increment. Lihat {@link #getId()}. */
	private Long id;
	/** SKU yang dikembalikan ke supplier — wajib. Lihat {@link #getProduk()}. */
	private Produk produk;
	/** Outlet asal barang; pembatas tenant — wajib. Lihat {@link #getToko()}. */
	private Toko toko;

	/** Faktur pengadaan asal barang, opsional. Lihat {@link #getFakturPengadaan()}. */
	private PengadaanFaktur fakturPengadaan;
	/** Nomor faktur asal sebagai teks bebas, untuk kasus tanpa taut faktur. Lihat {@link #getKodeFakturAsal()}. */
	private String kodeFakturAsal;
	/** Supplier tujuan pengembalian, opsional. Lihat {@link #getSupplier()}. */
	private Penyedia supplier;

	/**
	 * Jumlah unit yang dikembalikan ke supplier. SELALU mengurangi stok tanpa syarat — lihat
	 * peringatan ketiadaan penjaga keseimbangan di {@link #getQty()}.
	 */
	private Double qty;
	/** Harga beli per unit sebagai dasar nilai retur. Lihat {@link #getHargaSatuan()}. */
	private Double hargaSatuan;
	/** Nilai rupiah retur; dihitung malas dari qty × harga. Lihat {@link #getTotalNilai()}. */
	private Double totalNilai;

	/** Sebab pengembalian (rusak/salah kirim/tidak sesuai), teks bebas. Lihat {@link #getAlasan()}. */
	private String alasan;
	/** Catatan tambahan panjang. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Waktu peristiwa retur. Lihat {@link #getWaktu()}. */
	private Date waktu;
	/** Petugas yang mencatat retur (audit shadow). Lihat {@link #getOleh()}. */
	private String oleh;
	/**
	 * Callback JPA {@code @PreUpdate} yang menyegarkan stempel waktu audit sebelum baris ini
	 * di-UPDATE, mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} agar aturan
	 * penulisan stempel tinggal di satu tempat untuk seluruh entity. Hanya berjalan pada UPDATE,
	 * bukan INSERT — nilai awal berasal dari inisialisasi field {@code tanggal_dirubah} yang
	 * ditulis di baris yang sama. Method ini {@code protected} dan milik runtime JPA; memanggilnya
	 * manual justru merusak jejak audit.
	 *
	 * <p>Perhatikan method dan deklarasi field {@code tanggal_dirubah} sengaja dipadatkan dalam
	 * satu baris fisik. Itu gaya penulisan blok audit standar yang disisipkan seragam ke banyak
	 * entity; jangan dipecah tanpa alasan agar tetap mudah dicocokkan antar berkas. Berbeda dari
	 * {@link Pembelian}, di sini tidak ada getter/setter untuk {@code tanggal_dirubah} sehingga
	 * kolomnya tidak dipetakan Hibernate dan hanya hidup di memori — stempelnya nyata hanya lewat
	 * Envers.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Konstruktor kosong wajib JPA/Hibernate. Menghasilkan baris tanpa produk dan tanpa toko;
	 * keduanya dipetakan {@code nullable = false} sehingga wajib diisi pemanggil sebelum
	 * {@code session.save()}.
	 */
	public ReturPembelian() {
	}

	/**
	 * Kunci utama {@code koperasi.retur_pembelian.id}, dibangkitkan database dengan strategi
	 * {@code IDENTITY}.
	 *
	 * <p>Berbeda dari {@link Pembelian} dan {@link DraftPembelian} yang memetakan kolom id dengan
	 * {@code insertable = false}, di sini kolom id ikut disertakan dalam INSERT. Dalam praktik itu
	 * tidak menimbulkan masalah karena kelas ini tidak punya konstruktor yang memasang id
	 * (bandingkan {@code Pembelian(Long)}), sehingga nilainya selalu {@code null} saat baris baru
	 * disimpan dan database tetap yang menentukan.</p>
	 *
	 * <p>Id ini dipakai sebagai kunci sumber saat baris diposting ke buku besar — lihat
	 * {@code kunciSumber = "koperasi.retur_pembelian:" + id} di alur draf posting.</p>
	 *
	 * @return id baris retur, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** Memasang kunci utama; normalnya hanya dipanggil Hibernate saat memuat baris. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * SKU yang dikembalikan ke supplier — kolom <b>wajib</b> ({@code nullable = false}) dan kunci
	 * yang dipakai rumus stok untuk mengurangi persediaan.
	 *
	 * <p>Setiap baris retur pembelian menyumbang suku {@code &minus;Σretur_pembelian.qty} pada
	 * rumus stok baku {@code StokKantinUtil.formulaStokSql}, dikelompokkan berdasarkan produk ini.
	 * Karena itu mengubah taut produk sebuah baris retur yang sudah tersimpan akan menggeser
	 * pengurangan stok dari satu SKU ke SKU lain — operasi yang harus selalu disertai perhitungan
	 * ulang stok pada KEDUA produk, bukan hanya yang baru.</p>
	 *
	 * <p>Getter menormalkan proxy lewat {@code check(...)} milik {@link GeneralValueObject},
	 * sehingga aman dipanggil dari lapisan tampilan di luar Session — dengan konsekuensi
	 * {@code null} menjadi ambigu antara "tidak ada produk" (yang mustahil di sini karena kolomnya
	 * wajib) dan "produk tidak dapat dimuat sekarang".</p>
	 *
	 * @return produk yang diretur; secara skema tidak pernah {@code null} untuk baris tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk", nullable = false)
	public Produk getProduk() {
		produk = check(produk);
		return produk;
	}

	/**
	 * Memasang produk yang diretur. Wajib diisi sebelum penyimpanan. Mengubahnya pada baris yang
	 * sudah tersimpan mengharuskan perhitungan ulang stok pada produk lama maupun baru.
	 */
	public void setProduk(Produk produk) {
		this.produk = produk;
	}

	/**
	 * Outlet asal barang yang diretur — kolom <b>wajib</b> dan <b>pembatas tenant</b> yang
	 * menentukan siapa boleh melihat dan menghapus baris ini.
	 *
	 * <p>Lapisan API mengisi kolom ini bukan dari masukan pengguna melainkan dari
	 * {@code produk.getToko()}, dan menolak permintaan bila toko produk tidak sama dengan toko
	 * pedagang pemanggil. Pemeriksaan setara juga berlaku saat penghapusan, yang membandingkan
	 * toko baris retur dengan toko pemanggil. Dengan begitu tenant tidak dapat dipalsukan lewat
	 * payload — pola yang benar dan layak dipertahankan pada alur baru mana pun yang menulis
	 * entity ini.</p>
	 *
	 * <p>Getter menormalkan proxy lewat {@code check(...)}. Perhatikan tidak ada lapis penimpaan
	 * apa pun di sini, berbeda dari {@code Pembelian.getToko()} yang dapat ditimpa kode pembayaran
	 * online.</p>
	 *
	 * @return outlet asal barang; secara skema tidak pernah {@code null} untuk baris tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko", nullable = false)
	public Toko getToko() {
		toko = check(toko);
		return toko;
	}

	/**
	 * Memasang outlet asal barang. Harus diturunkan dari {@code produk.getToko()}, bukan dari
	 * masukan pengguna — lihat catatan penegakan tenant di {@link #getToko()}.
	 */
	public void setToko(Toko toko) {
		this.toko = toko;
	}

	/**
	 * Faktur pengadaan asal barang yang diretur — <b>opsional dan sengaja demikian</b>.
	 *
	 * <p>Ketiadaan taut ini bukan cacat data: barang dapat diretur tanpa jejak faktur pengadaan
	 * yang jelas, misalnya stok lama hasil migrasi, barang titipan, atau kiriman yang fakturnya
	 * belum sempat dicatat. Untuk kasus seperti itu nomor fakturnya masih dapat dicatat sebagai
	 * teks lewat {@link #getKodeFakturAsal()}. Bila taut ini ada, ia dipakai untuk mengelompokkan
	 * beberapa baris retur yang berasal dari satu faktur yang sama.</p>
	 *
	 * <p><b>Taut ini TIDAK berfungsi sebagai penjaga keseimbangan.</b> Menautkan sebuah faktur
	 * tidak membuat sistem memeriksa bahwa barang yang diretur benar-benar ada di faktur itu,
	 * apalagi bahwa jumlahnya masih tersisa — lihat penjelasan lengkapnya di {@link #getQty()}.
	 * Perlakukan taut ini murni sebagai rujukan dokumen, bukan sebagai validasi.</p>
	 *
	 * <p>Getter murni yang mengembalikan field apa adanya; tidak menormalkan lewat
	 * {@code check(...)} seperti {@link #getProduk()} dan {@link #getToko()}, sehingga relasi lazy
	 * yang belum termuat dapat melempar {@code LazyInitializationException} bila disentuh di luar
	 * Session.</p>
	 *
	 * @return faktur pengadaan asal, atau {@code null} bila retur tidak ditautkan ke faktur
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "faktur_pengadaan", nullable = true)
	public PengadaanFaktur getFakturPengadaan() {
		fakturPengadaan = check(fakturPengadaan);
		return fakturPengadaan;
	}

	/**
	 * Memasang faktur pengadaan asal. Bila diisi dan {@link #getKodeFakturAsal()} masih kosong,
	 * lapisan API mengisi kode faktur dari nomor faktur ini secara otomatis.
	 */
	public void setFakturPengadaan(PengadaanFaktur fakturPengadaan) {
		this.fakturPengadaan = fakturPengadaan;
	}

	/**
	 * Nomor faktur asal sebagai <b>teks bebas</b> — pendamping {@link #getFakturPengadaan()} untuk
	 * kasus di mana faktur fisik diketahui tetapi dokumennya tidak ada di sistem.
	 *
	 * <p>Kolom ini menyalin nomor faktur alih-alih bergantung pada taut objek justru agar
	 * informasi tetap terbaca ketika faktur asal tidak ada atau kelak dihapus. Bila
	 * {@link #getFakturPengadaan()} diisi sedangkan kolom ini dibiarkan kosong, lapisan API
	 * mengisinya otomatis dari {@code faktur.getNomorFaktur()} sehingga kedua kolom saling
	 * melengkapi, bukan bersaing.</p>
	 *
	 * <p>Karena berupa teks bebas, isinya tidak divalidasi terhadap nomor faktur mana pun yang
	 * benar-benar ada. Jangan menjadikannya kunci join dalam laporan; pakai
	 * {@link #getFakturPengadaan()} untuk itu, dan perlakukan kolom ini sebagai keterangan
	 * tampilan.</p>
	 *
	 * @return nomor faktur asal, atau {@code null}/kosong bila tidak dicatat
	 */
	@Column(name = "kode_faktur_asal")
	public String getKodeFakturAsal() {
		return kodeFakturAsal;
	}

	/** Memasang nomor faktur asal sebagai teks. Diisi otomatis dari faktur bila taut faktur ada dan kolom ini kosong. */
	public void setKodeFakturAsal(String kodeFakturAsal) {
		this.kodeFakturAsal = kodeFakturAsal;
	}

	/**
	 * Supplier tujuan pengembalian barang — pihak yang menerima barang kembali dan yang utangnya
	 * berkurang akibat retur ini.
	 *
	 * <p>Opsional, dengan pengisian otomatis: bila {@link #getFakturPengadaan()} diisi sedangkan
	 * kolom ini dibiarkan kosong, lapisan API menurunkannya dari {@code faktur.getSupplier()}.
	 * Dengan begitu petugas cukup memilih faktur, dan supplier ikut terisi konsisten dengan
	 * dokumen asalnya.</p>
	 *
	 * <p>Kolom inilah yang membedakan retur pembelian dari retur penjualan secara akuntansi:
	 * jurnalnya mendebet utang kepada supplier ini (atau kas bila pengembaliannya tunai) dan
	 * mengkredit persediaan. Karena kolomnya opsional, baris tanpa supplier tetap dapat diposting
	 * — konsekuensinya jurnal kehilangan lawan transaksi yang spesifik, sehingga pengisian
	 * supplier sangat dianjurkan untuk setiap retur yang berdampak ke utang.</p>
	 *
	 * <p>Getter murni tanpa {@code check(...)}; berlaku catatan proxy lazy yang sama seperti pada
	 * {@link #getFakturPengadaan()}.</p>
	 *
	 * @return supplier tujuan retur, atau {@code null} bila tidak dicatat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "supplier", nullable = true)
	public Penyedia getSupplier() {
		supplier = check(supplier);
		return supplier;
	}

	/** Memasang supplier tujuan retur. Diturunkan otomatis dari faktur bila taut faktur ada dan kolom ini kosong. */
	public void setSupplier(Penyedia supplier) {
		this.supplier = supplier;
	}

	/**
	 * Jumlah unit yang dikembalikan ke supplier — angka yang <b>SELALU mengurangi stok tanpa
	 * syarat</b>.
	 *
	 * <p>Berbeda dari {@code ReturPenjualan} yang punya flag "kembalikan ke stok", retur pembelian
	 * tidak mengenal skenario "barang tetap di toko tetapi dicatat retur": secara fisik barang
	 * memang meninggalkan toko kembali ke supplier. Karena itu seluruh qty di sini masuk sebagai
	 * suku pengurang {@code &minus;Σretur_pembelian.qty} pada rumus stok baku, tanpa syarat apa
	 * pun.</p>
	 *
	 * <p><b>Tidak ada penjaga keseimbangan terhadap barang yang pernah masuk.</b> Ini sifat yang
	 * paling perlu disadari dari entity ini dan konsisten di seluruh lapisan:</p>
	 * <ul>
	 *   <li>Entity ini tidak memvalidasi apa pun — bahkan {@link #setQty(Double)} menerima nilai
	 *       negatif, yang akan berperilaku sebagai <i>penambah</i> stok karena sukunya
	 *       dikurangkan.</li>
	 *   <li>Lapisan API hanya memeriksa {@code qty > 0}. Tidak ada pemeriksaan bahwa qty retur
	 *       &le; qty yang pernah diterima dari faktur asal, tidak ada pemeriksaan terhadap
	 *       akumulasi retur sebelumnya atas faktur yang sama, dan tidak ada pemeriksaan terhadap
	 *       stok yang tersedia saat itu.</li>
	 *   <li>Akibatnya sebuah faktur berisi 10 unit dapat diretur 100 unit, atau diretur 10 unit
	 *       sebanyak tiga kali, dan stok produk dapat terdorong menjadi <b>negatif</b> tanpa
	 *       peringatan.</li>
	 * </ul>
	 * <p>Kelonggaran itu punya alasan yang masuk akal — retur sering dilakukan atas barang yang
	 * fakturnya tidak tertaut, sehingga penjaga berbasis faktur tidak dapat diterapkan seragam —
	 * tetapi konsekuensinya kebenaran angka retur sepenuhnya bersandar pada disiplin petugas dan
	 * pada pembatasan peran (hanya admin/manajer atau supervisor toko yang boleh mencatat retur).
	 * Setiap penambahan penjaga di kemudian hari harus menangani baris tanpa faktur sebagai kasus
	 * yang sah, bukan menolaknya.</p>
	 *
	 * <p>Getter murni dengan bawaan {@code 0.0} — berbeda dari {@code Pembelian.getQty()} yang
	 * berbawaan {@code 1.0}. Bawaan nol di sini lebih aman: baris retur yang qty-nya belum diisi
	 * tidak mengurangi stok sama sekali, alih-alih diam-diam mengurangi satu unit.</p>
	 *
	 * @return jumlah unit yang diretur; tidak pernah {@code null}
	 */
	public Double getQty() {
		return qty == null ? 0.0 : qty;
	}

	/**
	 * Memasang jumlah unit yang diretur. <b>Tidak divalidasi</b> terhadap jumlah yang pernah
	 * diterima maupun terhadap stok tersedia, dan nilai negatif tidak dicegah — lihat
	 * {@link #getQty()}. Setiap perubahan nilai ini pada baris tersimpan harus diikuti
	 * perhitungan ulang stok produknya.
	 */
	public void setQty(Double qty) {
		this.qty = qty;
	}

	/**
	 * Harga <b>beli</b> per unit yang dipakai sebagai dasar nilai retur.
	 *
	 * <p>Perhatikan ini harga beli/pengadaan, bukan harga jual — retur pembelian mengembalikan
	 * barang ke supplier, sehingga nilai yang relevan adalah yang dulu dibayarkan kepada supplier.
	 * Ini salah satu pembeda paling jelas dari {@link Pembelian} yang seluruh kolom harganya
	 * adalah harga jual.</p>
	 *
	 * <p>Getter murni dengan bawaan {@code 0.0}, tanpa pembangkitan malas dari katalog maupun dari
	 * faktur asal — berbeda dari {@code Pembelian.getHargaSatuan()} yang memungut harga dari
	 * produk bila kosong. Ketiadaan pembangkitan itu tepat di sini: harga beli yang benar adalah
	 * yang tercantum di faktur pengadaan, bukan angka mana pun yang tersimpan di katalog produk,
	 * dan menebaknya dari katalog justru akan menghasilkan nilai retur yang keliru. Konsekuensinya
	 * pemanggil <b>wajib</b> mengisi harga ini; bila tidak, {@link #getTotalNilai()} akan bernilai
	 * nol dan retur tercatat tanpa dampak nilai.</p>
	 *
	 * @return harga beli per unit; tidak pernah {@code null}
	 */
	public Double getHargaSatuan() {
		return hargaSatuan == null ? 0.0 : hargaSatuan;
	}

	/** Memasang harga beli per unit. Wajib diisi pemanggil — tidak ada pembangkitan otomatis dari katalog. */
	public void setHargaSatuan(Double hargaSatuan) {
		this.hargaSatuan = hargaSatuan;
	}

	/**
	 * Nilai rupiah retur ({@code qty × hargaSatuan}), dengan <b>perhitungan malas</b> bila kolom
	 * masih kosong atau nol.
	 *
	 * <p>Getter destruktif ringan: bila {@code totalNilai} {@code null} <b>atau tepat
	 * {@code 0.0}</b>, nilainya dihitung ulang dan ditulis balik ke field. Perilakunya berada di
	 * antara dua pola yang dipakai di kelas serumpun — tidak seketat
	 * {@code Pembelian.getTotal()} yang menghitung ulang tanpa syarat, tetapi juga tidak sekonservatif
	 * getter yang hanya memeriksa {@code null}.</p>
	 *
	 * <p>Dua akibat praktis dari syarat yang mencakup {@code 0.0}:</p>
	 * <ul>
	 *   <li><b>Nilai nol tidak dapat dipertahankan</b> selama qty dan harga satuan sama-sama
	 *       positif. Retur barang yang sengaja dinilai nol (mis. barang rusak yang tidak
	 *       dikreditkan supplier) akan dihitung ulang menjadi nilai positif setiap kali dibaca.
	 *       Untuk kasus itu, nol harus dinyatakan lewat {@link #setHargaSatuan(Double)} bernilai
	 *       nol, bukan lewat setter ini.</li>
	 *   <li>Sebaliknya, nilai yang <b>berbeda</b> dari {@code qty × harga} akan bertahan. Lapisan
	 *       API memang memasang nilai eksplisit saat menyimpan, sehingga baris yang sudah
	 *       tersimpan umumnya konsisten; namun bila kelak qty atau harga diubah tanpa memperbarui
	 *       kolom ini, nilai lama akan <b>tetap dipertahankan</b> dan menjadi basi tanpa
	 *       peringatan. Setiap alur yang mengubah qty/harga wajib ikut mengosongkan atau
	 *       memperbarui kolom ini.</li>
	 * </ul>
	 * <p>Perhatikan perbandingan {@code totalNilai == 0.0} membandingkan {@code Double} dengan
	 * primitif sehingga terjadi auto-unboxing — aman di sini karena cabang {@code null} sudah
	 * diperiksa lebih dulu lewat hubung-singkat {@code ||}.</p>
	 *
	 * @return nilai rupiah retur; tidak pernah {@code null}
	 */
	public Double getTotalNilai() {
		if (totalNilai == null || totalNilai == 0.0) {
			totalNilai = getQty() * getHargaSatuan();
		}
		return totalNilai;
	}

	/**
	 * Memasang nilai rupiah retur secara eksplisit. Nilai {@code 0.0} <b>tidak bertahan</b> bila
	 * qty dan harga satuan positif — lihat {@link #getTotalNilai()}.
	 */
	public void setTotalNilai(Double totalNilai) {
		this.totalNilai = totalNilai;
	}

	/**
	 * Sebab barang dikembalikan (rusak, salah kirim, tidak sesuai pesanan, kedaluwarsa), berupa
	 * <b>teks bebas</b>.
	 *
	 * <p>Sengaja tidak dibatasi enum atau tabel master, sehingga petugas dapat menuliskan sebab
	 * apa pun yang sesuai keadaan. Harganya, kolom ini <b>tidak dapat diandalkan untuk
	 * pengelompokan</b>: "rusak", "Rusak", dan "barang rusak" akan terhitung sebagai tiga kategori
	 * berbeda. Laporan yang ingin menganalisis pola penyebab retur harus melakukan normalisasi
	 * sendiri, atau kolom ini perlu diganti rujukan ke master alasan.</p>
	 *
	 * <p>Tidak dipetakan {@code columnDefinition = "text"} seperti {@link #getKeterangan()},
	 * sehingga panjangnya mengikuti bawaan {@code varchar(255)} Hibernate — alasan yang terlalu
	 * panjang akan ditolak database, bukan dipotong diam-diam. Untuk catatan panjang, pakai
	 * {@link #getKeterangan()}.</p>
	 *
	 * <p>Getter murni tanpa normalisasi; dapat mengembalikan {@code null} maupun string kosong,
	 * dan lapisan API memang menyimpan string kosong bila petugas tidak mengisinya.</p>
	 *
	 * @return sebab retur, atau {@code null}/kosong bila tidak diisi
	 */
	public String getAlasan() {
		return alasan;
	}

	/** Memasang sebab retur sebagai teks bebas. Tidak dinormalkan — lihat catatan pengelompokan di {@link #getAlasan()}. */
	public void setAlasan(String alasan) {
		this.alasan = alasan;
	}

	/**
	 * Catatan tambahan bebas tentang retur ini — dipetakan {@code text} sehingga <b>tanpa batas
	 * panjang</b>.
	 *
	 * <p>Berbeda dari {@code Pembelian.getKeterangan()} yang destruktif (membangkitkan kalimat
	 * otomatis lalu memotongnya permanen di 253 karakter), getter di sini <b>murni</b>: ia
	 * mengembalikan apa yang disimpan, tanpa pembangkitan dan tanpa pemotongan. Nilai {@code null}
	 * juga tidak dinormalkan menjadi string kosong, sehingga pemanggil harus siap menerimanya.</p>
	 *
	 * <p>Tempat yang tepat untuk keterangan panjang seperti nomor berita acara, nama petugas
	 * supplier yang menerima barang, atau rincian kondisi barang — sedangkan sebab singkatnya
	 * ditulis di {@link #getAlasan()}.</p>
	 *
	 * @return catatan tambahan, atau {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/** Memasang catatan tambahan. Disimpan apa adanya tanpa pemotongan panjang. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Waktu peristiwa retur, dengan bawaan waktu sekarang bila kolomnya kosong.
	 *
	 * <p>Inilah tanggal yang menentukan sebuah baris retur masuk periode posting yang mana —
	 * alur draf posting menyaring {@code koperasi.retur_pembelian} berdasarkan rentang tanggal
	 * atas kolom ini. Karena itu ketepatannya berdampak langsung pada periode akuntansi mana yang
	 * menanggung kredit persediaan.</p>
	 *
	 * <p>Waspadai bawaannya: bila {@code waktu} {@code null}, yang dikembalikan adalah
	 * {@code WaktuUtil.getDate()} — waktu SAAT DIBACA — dan nilai itu sengaja <b>tidak</b> ditulis
	 * balik ke field, sehingga setiap pembacaan menghasilkan waktu berbeda. Baris tanpa waktu
	 * karenanya selalu tampak "baru saja terjadi" di tampilan, sementara nilai yang benar-benar
	 * tersimpan di database tetap {@code NULL} dan justru <b>tidak akan terjaring</b> oleh
	 * penyaringan rentang tanggal pada alur posting. Baris seperti itu dapat luput dari posting
	 * tanpa terlihat luput. Lapisan API mencegahnya dengan selalu memasang
	 * {@code rb.setWaktu(new Date())} saat menyimpan; alur baru mana pun harus melakukan hal
	 * serupa.</p>
	 *
	 * @return waktu retur; tidak pernah {@code null}, tetapi bisa tidak stabil (lihat di atas)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
	}

	/** Memasang waktu peristiwa retur. Wajib diisi eksplisit saat menyimpan — lihat risiko luput-posting di {@link #getWaktu()}. */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Petugas yang mencatat retur ini (field audit shadow).
	 *
	 * <p>Diisi lapisan API dari {@code tbmuser.getUserId()}, atau string tetap
	 * {@code "retur_pembelian"} bila konteks pengguna tidak tersedia. Menyimpan jejak pelaku di
	 * baris itu sendiri — di samping rekaman Envers ({@code @Audited}) — agar daftar retur dapat
	 * menampilkan "dicatat oleh siapa" dengan satu SELECT biasa tanpa join ke infrastruktur
	 * Envers. Ini <b>keharusan teknis, bukan duplikasi ceroboh</b>.</p>
	 *
	 * <p>Berbeda dari {@link Pembelian} dan {@link DraftPembelian}, kelas ini hanya punya satu
	 * kolom pelaku ({@code oleh}) tanpa pendamping {@code olehId}, dan setternya <b>tidak</b>
	 * memasang penjaga anti-kosong. Artinya {@link #setOleh(String)} di sini <i>dapat</i>
	 * mengosongkan jejak audit, sedangkan padanannya di kedua kelas itu menolak nilai kosong.
	 * Perbedaan ini perlu diketahui sebelum menyalin pola dari satu kelas ke kelas lain.</p>
	 *
	 * <p>Perhatikan pula jejak ini hanya mencatat pembuat; tidak ada kolom yang mencatat siapa
	 * yang terakhir mengubah baris. Untuk itu satu-satunya sumber adalah Envers.</p>
	 *
	 * @return identitas petugas pencatat, atau {@code null} pada data lama
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Memasang identitas petugas pencatat. Berbeda dari padanannya di {@link Pembelian}, setter
	 * ini <b>tidak</b> menolak nilai kosong sehingga dapat menghapus jejak audit.
	 */
	public void setOleh(String oleh) {
		this.oleh = oleh;
	}


	/**
	 * Penanda jurnal. Jurnal retur beli: debet Utang Supplier/Kas, kredit Persediaan. Diisi saat baris ini diposting ke buku besar; dipakai
	 * sebagai kunci anti-posting-ganda dan jejak balik dari jurnal ke dokumen sumbernya.
	 * Kolomnya dibuat otomatis oleh Hibernate.
	 */
	private ais.database.model.akunting.PostingHistory postingHistory;

	/**
	 * Penanda baris ini sudah diposting ke buku besar — <b>kunci anti-posting-ganda</b> sekaligus
	 * jejak balik dari jurnal ke dokumen sumbernya.
	 *
	 * <p>Jurnal retur beli mendebet Utang Supplier (atau Kas bila pengembaliannya tunai) dan
	 * mengkredit Persediaan, dengan nilai dari {@link #getTotalNilai()}. Selama taut ini terisi,
	 * baris dikecualikan dari pencarian draf posting berikutnya sehingga mustahil terhitung dua
	 * kali. Jejak baliknya memakai kunci sumber berpola
	 * {@code "koperasi.retur_pembelian:" + id}.</p>
	 *
	 * <p><b>Penanda ini diperiksa oleh alur penghapusan.</b> {@code KantinHelper.returPembelianHapus}
	 * menolak permintaan hapus ({@code "Retur sudah diposting ke jurnal, tidak bisa dihapus dari
	 * menu ini."}) begitu field ini terisi, sama seperti pembatalan transaksi POS di berkas yang
	 * sama menolak begitu penanda posting ditemukan. Tanpa penjaga itu, penghapusan akan
	 * meninggalkan entri jurnal di buku besar tanpa dokumen sumber yang dapat ditelusuri, dan
	 * perhitungan ulang stok yang mengikuti penghapusan akan mengembalikan barang ke persediaan
	 * sehingga stok fisik tidak lagi sejalan dengan nilai persediaan yang sudah dikredit di
	 * jurnal.</p>
	 *
	 * <p>Baris ber-{@code postingHistory} tetap <b>tidak boleh dihapus</b>: koreksi atas retur
	 * yang sudah diposting semestinya ditempuh lewat jurnal koreksi, bukan lewat penghapusan
	 * dokumen sumber.</p>
	 *
	 * <p>Kolomnya ({@code posting_history}) dibuat otomatis oleh Hibernate saat boot sesuai
	 * kebijakan repositori yang menyerahkan ALTER TABLE kepada hbm2ddl. Dimuat {@code LAZY};
	 * pemeriksaan status posting dari lapisan tampilan harus dilakukan di dalam Session yang masih
	 * terbuka.</p>
	 *
	 * @return riwayat posting yang menyerap baris ini, atau {@code null} bila belum diposting
	 */
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "posting_history", nullable = true)
	public ais.database.model.akunting.PostingHistory getPostingHistory() {
		postingHistory = check(postingHistory);
		return postingHistory;
	}

	/**
	 * Menandai baris ini sudah diposting ke buku besar. Harus dipanggil dalam transaksi database
	 * yang sama dengan penyimpanan entri jurnalnya agar tidak ada keadaan antara berupa jurnal
	 * yang sudah terbentuk sementara barisnya belum tertandai — keadaan seperti itu membuat baris
	 * dapat terposting dua kali.
	 */
	public void setPostingHistory(ais.database.model.akunting.PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

}
