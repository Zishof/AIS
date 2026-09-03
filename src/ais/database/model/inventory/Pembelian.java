package ais.database.model.inventory;

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.GeneralValueObject;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.UploadLogInfo;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.AturanDiskon;
import ais.database.model.koperasi.CaraPembayaranKoperasi;
import ais.database.model.koperasi.KodePembayaranOnline;
import ais.database.model.koperasi.PembelianAnggotaKoperasi;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Siswa;

/**
 * <h2>Baris transaksi PENJUALAN kasir/POS koperasi — satu baris per SKU per struk.</h2>
 *
 * <p><b>PERINGATAN NAMA MENYESATKAN — baca ini sebelum apa pun.</b> Meski bernama
 * {@code Pembelian} dan berada di paket {@code inventory}, entity ini <b>BUKAN</b> pembelian
 * barang dari supplier ke toko. Ia adalah <b>baris penjualan</b>: barang yang KELUAR dari toko
 * ke pembeli. Namanya diambil dari sudut pandang <i>pembeli</i> ("pembelian yang dilakukan
 * member"), bukan sudut pandang koperasi. Bukti tak terbantahkan ada di tiga tempat:</p>
 * <ul>
 *   <li>Rumus stok baku {@code StokKantinUtil.formulaStokSql} menempatkan
 *       {@code Σpembelian.qty} sebagai suku <b>PENGURANG</b> stok, sejajar dengan
 *       {@code Σpemakaian_bahan_baku.qty} dan {@code Σretur_pembelian.qty}, dengan komentar
 *       eksplisit "total unit yang benar-benar TERJUAL ke pembeli". Barang masuk dari supplier
 *       dicatat suku lain sama sekali: {@code Σpengadaan.qty}.</li>
 *   <li>Seluruh kolom harga di sini adalah harga JUAL — {@link #getHargaJual()} dan
 *       {@link #getHargaSatuan()} sama-sama jatuh balik ke {@code Produk.getHargaJual()},
 *       tidak ada satu pun kolom harga beli/HPP masukan.</li>
 *   <li>Field {@link #getPostingHpp()} menandai baris ini "sudah ikut diposting sebagai HPP
 *       (beban pokok)". HPP hanya lahir dari peristiwa PENJUALAN; sebuah pembelian tidak
 *       pernah menghasilkan beban pokok.</li>
 * </ul>
 * <p>Yang benar-benar mencatat pembelian toko ke supplier adalah {@code PengadaanFaktur} +
 * {@code PengadaanProduk}, dan pengembaliannya {@link ReturPembelian}. Pengembalian barang
 * atas baris {@code Pembelian} ini justru dicatat oleh {@code ReturPenjualan}. Jangan pernah
 * memasangkan {@code Pembelian} dengan {@link ReturPembelian} hanya karena namanya berima —
 * keduanya berada di arah arus barang yang BERLAWANAN.</p>
 *
 * <h3>Posisi dalam rantai dokumen</h3>
 * <p>Entity ini adalah <b>anak (detail)</b>; induknya (header/struk) adalah
 * {@link PembelianAnggotaKoperasi} lewat {@link #getPembelianAnggotaKoperasi()}. Satu struk
 * berisi banyak baris {@code Pembelian}. Tabelnya {@code koperasi.pembelian} — perhatikan
 * skema {@code koperasi} walau kelasnya di paket {@code inventory}; pemisahan paket/skema di
 * sini murni historis dan tidak bisa dijadikan petunjuk domain.</p>
 * <pre>
 *   DraftPembelianAnggotaKoperasi (header draft) --1:N--&gt; DraftPembelian (baris draft)
 *              |  finalisasi (simpanRinci)                      |  setLunas(pembelian)
 *              v                                                v
 *   PembelianAnggotaKoperasi      (header final) --1:N--&gt; Pembelian (baris final)  &lt;-- ANDA DI SINI
 * </pre>
 * <p>Ada DUA jalan sebuah baris {@code Pembelian} lahir, dan keduanya diputuskan di
 * {@code PembelianAnggotaKoperasi.simpanRinci}:</p>
 * <ol>
 *   <li><b>Jalur langsung</b> — kasir memindai barang lalu langsung dibayar. Baris dibentuk
 *       dari payload JSON transaksi, termasuk {@code satuan_jual_id} / {@code qty_input} /
 *       {@code faktor_ke_dasar}.</li>
 *   <li><b>Jalur draft</b> — pesanan ditahan dulu (mis. pesanan meja kantin yang dibayar
 *       belakangan) sebagai {@link DraftPembelian}, lalu "dipromosikan" saat lunas. Di jalur
 *       ini baris {@code Pembelian} dibentuk dengan menyalin kolom demi kolom dari draft.</li>
 * </ol>
 *
 * <h3>Sifat yang wajib diketahui sebelum menyentuh kelas ini</h3>
 * <p><b>Mayoritas getter di kelas ini DESTRUKTIF</b> — mereka bukan pembaca pasif, melainkan
 * menulis balik ke field-nya sendiri sebelum mengembalikan nilai. {@link #getNama()},
 * {@link #getKeterangan()}, {@link #getMember()}, {@link #getKios()}, {@link #getJenisMember()},
 * {@link #getCaraBayar()}, {@link #getTotal()}, {@link #getHargaJual()},
 * {@link #getHargaSatuan()}, {@link #getSiswa()}, {@link #getMahasiswa()},
 * {@link #getCalonSiswa()}, {@link #getBiodataCalonMahasiswa()}, {@link #getTbmuser()},
 * {@link #getToko()}, {@link #getWaktu()}, {@link #getAnggotaKoperasi()}, dan
 * {@link #getCaraPembayaranKoperasi()} semuanya memodifikasi state. Konsekuensinya nyata dan
 * sering mengejutkan: memanggil getter saja pada entity yang ter-attach ke Session Hibernate
 * dapat membuat entity itu <b>dirty</b> dan tersimpan diam-diam saat flush, meskipun kode
 * pemanggil merasa hanya "membaca untuk ditampilkan". Ini keputusan desain lama yang dipakai
 * luas (denormalisasi snapshot demi laporan/struk yang tetap terbaca walau master berubah),
 * jadi <b>jangan</b> "membetulkannya" menjadi getter murni tanpa menelusuri seluruh
 * pemanggil.</p>
 *
 * <p><b>Field audit shadow</b> — {@link #getOleh()}, {@link #getOlehId()}, dan
 * {@link #getTanggal_dirubah()} bukan duplikasi ceroboh dari mekanisme audit Envers
 * ({@code @Audited}). Envers merekam SIAPA lewat tabel revisi terpisah yang hanya terbaca
 * lewat query khusus; ketiga kolom ini menyimpan jejak yang sama <i>di baris itu sendiri</i>
 * agar laporan dan grid ZK dapat menampilkan "diubah oleh X pada Y" dengan satu SELECT biasa
 * tanpa join ke infrastruktur Envers. Ini <b>keharusan teknis, bukan bug</b>.</p>
 *
 * <p><b>Snapshot identitas pembeli tersebar di enam kolom.</b> Satu baris hanya mewakili satu
 * pembeli, tetapi pembeli itu bisa berupa {@link Siswa}, {@link CalonSiswa}, {@link Mahasiswa},
 * {@link BiodataCalonMahasiswa}, {@link Tbmuser} (pegawai/guru/dosen), atau
 * {@link AnggotaKoperasi} — dan sebagian besar kosong. Bentuk "satu kolom FK per jenis
 * pembeli" ini dipakai konsisten oleh {@link #getMember()} dan {@link #getJenisMember()} yang
 * menelusuri kolom-kolom itu dalam urutan prioritas tetap. Menambah jenis pembeli baru berarti
 * menambah kolom DAN menyisipkan cabang di kedua getter tersebut.</p>
 *
 * @see PembelianAnggotaKoperasi header/struk yang memuat baris ini
 * @see DraftPembelian kembaran baris ini pada tahap draft (sebelum lunas)
 * @see ais.action.master.inventory.StokKantinUtil rumus stok yang mengurangi Σpembelian.qty
 * @see ReturPembelian arah SEBALIKNYA — retur ke supplier, bukan pasangan kelas ini
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "pembelian")
public class Pembelian extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya SENGAJA identik dengan
	 * {@link DraftPembelian#serialVersionUID} karena kedua kelas lahir dari template hbm2java
	 * yang sama; jangan disamakan artinya dengan "kedua kelas kompatibel biner" — keduanya
	 * kelas berbeda dan tidak pernah saling di-deserialisasi.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama {@code koperasi.pembelian.id}, auto-increment. Lihat {@link #getId()}. */
	private Long id;
	/** Nama/identitas petugas terakhir yang mengubah baris (audit shadow). Lihat {@link #getOleh()}. */
	private String oleh;
	/** Id petugas terakhir yang mengubah baris (audit shadow). Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Id petugas terakhir yang menyentuh baris ini — pasangan {@link #getOleh()} yang menyimpan
	 * id teknis, bukan nama tampilan. Getter murni tanpa efek samping dan tanpa normalisasi;
	 * dapat mengembalikan {@code null} pada baris lama yang ditulis sebelum kolom ini ada.
	 *
	 * @return id petugas, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id petugas, <b>menolak diam-diam</b> nilai kosong.
	 *
	 * <p>Perhatikan penjaga di baris pertama: {@code null} maupun string yang hanya berisi spasi
	 * menyebabkan method langsung {@code return} tanpa mengubah apa pun. Artinya nilai lama
	 * <b>dipertahankan</b>, bukan ditimpa dengan kosong. Ini disengaja: jejak audit "siapa yang
	 * terakhir menyentuh" tidak boleh terhapus hanya karena satu alur pemanggil kebetulan
	 * meneruskan string kosong (mis. binding form ZK yang field-nya belum terisi, atau payload
	 * JSON yang tidak menyertakan kunci ini). Sisi buruknya, setter ini <b>tidak bisa dipakai
	 * untuk mengosongkan</b> kolom; bila benar-benar perlu dikosongkan, kolomnya harus di-update
	 * lewat query langsung.</p>
	 *
	 * @param olehId id petugas; {@code null}/kosong diabaikan tanpa error
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama petugas, dengan penjaga anti-kosong yang sama persis seperti
	 * {@link #setOlehId(String)} — nilai {@code null} atau blank diabaikan dan nilai lama
	 * dipertahankan. Lihat penjelasan lengkap alasannya di setter tersebut.
	 *
	 * @param oleh nama/identitas petugas; {@code null}/kosong diabaikan tanpa error
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama petugas terakhir yang mengubah baris ini (field audit shadow — lihat JavaDoc kelas).
	 * Getter murni; dapat {@code null} pada data lama.
	 *
	 * @return nama petugas, atau {@code null}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate} yang menyegarkan stempel waktu audit tepat sebelum baris
	 * ini di-UPDATE ke database.
	 *
	 * <p>Seluruh pekerjaannya didelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} agar aturan penulisan
	 * stempel waktu tinggal di satu tempat untuk seluruh entity, bukan disalin ke ratusan kelas
	 * model. Perhatikan bahwa callback ini <b>hanya berjalan pada UPDATE</b>, bukan INSERT —
	 * nilai awal saat baris baru dibuat berasal dari inisialisasi field
	 * {@code tanggal_dirubah = ais.ui.util.WaktuUtil.getDate()} di deklarasinya. Konsekuensi
	 * praktisnya: baris yang tidak pernah diubah tetap memiliki stempel waktu yang wajar
	 * (yaitu waktu pembuatannya), sehingga laporan tidak perlu menangani {@code null}.</p>
	 *
	 * <p>Method ini {@code protected} dan tidak boleh dipanggil dari kode aplikasi — ia milik
	 * runtime JPA. Memanggilnya manual akan mengubah stempel tanpa disertai UPDATE yang
	 * sesungguhnya, sehingga justru merusak jejak audit.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir (audit shadow). Diinisialisasi ke waktu pembuatan objek
	 * lewat {@code WaktuUtil.getDate()} — bukan {@code new Date()} — agar seluruh aplikasi
	 * memakai satu sumber waktu yang dapat digeser saat pengujian/simulasi. Diperbarui pada
	 * setiap UPDATE oleh {@link #onUpdate()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir. Umumnya TIDAK dipanggil kode aplikasi karena
	 * {@link #onUpdate()} sudah mengurusnya otomatis; setter ini ada terutama agar Hibernate
	 * dapat memuat nilai dari database dan agar alur impor data lama dapat memasang stempel
	 * historis yang sebenarnya.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir baris ini, dipetakan sebagai {@code TIMESTAMP}. Nama
	 * method sengaja mempertahankan gaya {@code snake_case} peninggalan hbm2java karena nama
	 * properti ini sudah terlanjur dipakai sebagai string di binding ZK, {@code Common
	 * .insertProperty}, dan kueri Criteria di banyak modul — menormalkannya jadi
	 * {@code getTanggalDirubah()} akan memutus semua rujukan berbasis nama tersebut.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang
	 *         dibuat lewat konstruktor kelas ini
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas {@code "<id>-<keterangan>"} untuk log dan komponen ZK yang
	 * menampilkan objek apa adanya.
	 *
	 * <p><b>Membaca field {@code keterangan} secara LANGSUNG, bukan lewat
	 * {@link #getKeterangan()}.</b> Perbedaannya penting: getter tersebut destruktif — ia
	 * membangkitkan kalimat deskripsi dan memotongnya di 254 karakter bila masih kosong.
	 * Dengan membaca field mentah, {@code toString()} tidak memicu efek samping apa pun,
	 * sehingga aman dipanggil dari logger atau debugger tanpa mengubah state entity. Imbalannya,
	 * hasilnya bisa berupa {@code "123-null"} untuk baris yang keterangannya belum pernah
	 * dibangkitkan — itu perilaku yang diterima untuk keperluan diagnostik.</p>
	 *
	 * @return gabungan id dan keterangan mentah
	 */
	public String toString() {
		return id + "-" + keterangan;
	}

	/** Kode/nomor baris; bila kosong dibangkitkan on the fly. Lihat {@link #getKode()}. */
	private String kode;
	/** Snapshot "kode + nama produk" untuk struk/laporan. Lihat {@link #getNama()}. */
	private String nama;

	/** Snapshot nama {@link Toko} (kios) tempat transaksi terjadi. Lihat {@link #getKios()}. */
	private String kios;
	/** Snapshot identitas pembeli dalam satu string tampilan. Lihat {@link #getMember()}. */
	private String member;
	/** SKU yang dijual pada baris ini — satu-satunya penghubung ke katalog. Lihat {@link #getProduk()}. */
	private Produk produk;
	/** Pembeli bila ia siswa. Salah satu dari enam kolom identitas; lihat JavaDoc kelas. */
	private Siswa siswa;
	/** Pembeli bila ia calon siswa (belum diterima). Lihat {@link #getCalonSiswa()}. */
	private CalonSiswa calonSiswa;
	/** Pembeli bila ia mahasiswa. Lihat {@link #getMahasiswa()}. */
	private Mahasiswa mahasiswa;
	/** Pembeli bila ia calon mahasiswa (pendaftar PMB). Lihat {@link #getBiodataCalonMahasiswa()}. */
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	/** Header/struk induk baris ini. Lihat {@link #getPembelianAnggotaKoperasi()}. */
	private PembelianAnggotaKoperasi pembelianAnggotaKoperasi;
	/** Cara pembayaran; didelegasikan ke header bila header ada. Lihat {@link #getCaraPembayaranKoperasi()}. */
	private CaraPembayaranKoperasi caraPembayaranKoperasi;
	/** Pembeli bila ia anggota koperasi. Lihat {@link #getAnggotaKoperasi()}. */
	private AnggotaKoperasi anggotaKoperasi;
	/** Snapshot label jenis pembeli ("Siswa", "Dosen", ...). Lihat {@link #getJenisMember()}. */
	private String jenisMember;
	/** Snapshot label cara bayar ("Tunai", "Online (Topup)", ...). Lihat {@link #getCaraBayar()}. */
	private String caraBayar;
	/** Pembeli bila ia pengguna internal (guru/dosen/pegawai). Lihat {@link #getTbmuser()}. */
	private Tbmuser tbmuser;
	/** Kalimat deskripsi baris, dibangkitkan otomatis bila kosong. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Harga jual SATU unit (bukan total). Lihat {@link #getHargaSatuan()}. */
	private Double hargaSatuan;
	/**
	 * Nilai jual baris. Meski namanya seolah harga per unit, isinya adalah hasil
	 * {@code qty × harga produk} saat dibangkitkan otomatis — lihat peringatan lengkapnya di
	 * {@link #getHargaJual()}.
	 */
	private Double hargaJual;
	/** Potongan harga baris dalam rupiah (bukan persen). Lihat {@link #getDiskon()}. */
	private Double diskon;
	/** Nilai bersih baris; SELALU dihitung ulang oleh getter. Lihat {@link #getTotal()}. */
	private Double total;
	/**
	 * Jumlah unit terjual dalam <b>satuan dasar</b> produk — inilah angka yang dipakai rumus
	 * stok dan HPP. Jangan tertukar dengan {@link #getQtyInput()} yang menyimpan angka mentah
	 * ketikan kasir dalam satuan jual.
	 */
	private Double qty;
	// Fase B (dok. 48/49): snapshot satuan JUAL per baris. qty TETAP satuan
	// dasar -- seluruh rumus stok/HPP/laporan tidak berubah; tiga kolom ini
	// murni tampilan + audit (baris "2 Karung" tetap terbaca sebagai 2 Karung
	// walau preset satuan berubah di masa depan).
	private Long satuanJual;
	private Double qtyInput;
	private Double faktorKeDasar;
	/** Waktu transaksi; didelegasikan ke tanggal bayar header bila ada. Lihat {@link #getWaktu()}. */
	private Date waktu;
	/** Flag aktif satu arah (default {@code true} bila {@code null}). Lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Outlet tempat baris ini terjadi — pembatas tenant utama. Lihat {@link #getToko()}. */
	private Toko toko;
	/** Jejak berkas impor bila baris ini lahir dari unggahan massal. Lihat {@link #getUploadLog()}. */
	private UploadLogInfo uploadLog;
	/** Kode pembayaran online bila transaksi lewat topup; menimpa banyak kolom lain. Lihat {@link #getKodePembayaranOnline()}. */
	private KodePembayaranOnline kodePembayaranOnline;

	/** Cashback rupiah dari {@link #getAturanDiskon()}. Lihat {@link #getCashback()}. */
	private Double cashback;
	/** Aturan diskon yang berlaku pada baris ini. Lihat {@link #getAturanDiskon()}. */
	private AturanDiskon aturanDiskon;
	/** Penanda barang sudah diserahkan ke pembeli. Lihat {@link #getTerlayani()}. */
	private Boolean terlayani;
	/** Id baris {@code Pembelian} induk untuk fitur "Produk Ekstra". Lihat {@link #getIndukId()}. */
	private Long indukId;
	/**
	 * Penanda baris penjualan ini SUDAH ikut diposting sebagai HPP (beban pokok).
	 * Ditambahkan 2026-08-19 agar Posting HPP dapat dilakukan PER BARANG seperti
	 * Posting Cicilan Mahasiswa: tanpa penanda ini, memposting sebagian barang
	 * berisiko terhitung DUA KALI ketika periode yang sama diposting ulang.
	 * Kolomnya dibuat otomatis oleh Hibernate saat boot (hbm2ddl).
	 */
	private ais.database.model.akunting.PostingHistory postingHpp;

	/**
	 * Konstruktor kosong wajib JPA/Hibernate. Menghasilkan baris tanpa produk, tanpa toko, dan
	 * tanpa header; seluruh kolom wajib harus diisi pemanggil sebelum {@code session.save()},
	 * khususnya {@link #setToko(Toko)} yang dipetakan {@code nullable = false}.
	 */
	public Pembelian() {
	}

	/**
	 * Konstruktor pintas yang hanya memasang kunci utama. Dipakai untuk membentuk referensi
	 * ringan ke baris yang sudah ada (mis. sebagai parameter kueri atau nilai FK) tanpa harus
	 * memuat seluruh baris dari database.
	 *
	 * <p><b>Objek hasil konstruktor ini bukan entity yang termuat.</b> Seluruh field lain
	 * bernilai {@code null}, sehingga memanggil getter destruktif seperti {@link #getTotal()}
	 * atau {@link #getMember()} pada objek ini akan menghasilkan nilai bawaan yang menyesatkan
	 * (mis. total {@code 0.0}), bukan nilai sebenarnya di database. Jangan pernah menyimpan
	 * objek dari konstruktor ini kembali ke database — itu akan menimpa baris asli dengan
	 * kolom-kolom kosong.</p>
	 *
	 * @param id kunci utama baris yang dirujuk
	 */
	public Pembelian(Long id) {
		this.id = id;
	}

	/**
	 * Kunci utama {@code koperasi.pembelian.id}, dibangkitkan database dengan strategi
	 * {@code IDENTITY}. Perhatikan {@code insertable = false}: kolom ini sengaja dikeluarkan
	 * dari pernyataan INSERT agar nilai yang mungkin tertinggal di objek (mis. sisa dari
	 * konstruktor {@link #Pembelian(Long)}) tidak ikut terkirim dan bertabrakan dengan sekuens
	 * database.
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** Memasang kunci utama; normalnya hanya dipanggil Hibernate saat memuat baris. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Kode/nomor baris transaksi, dengan <b>pembangkitan darurat bila kolomnya kosong</b>.
	 *
	 * <p>Bila {@code kode} masih {@code null}, method mengembalikan string berpola
	 * {@code "INV-" + epochMillis + counter} dengan {@code counter} berasal dari
	 * {@code ais.common.Common.increments} yang di-increment di tempat. Perilaku ini punya dua
	 * sifat yang harus disadari:</p>
	 * <ul>
	 *   <li><b>Nilainya TIDAK stabil.</b> Karena hasilnya tidak pernah ditulis balik ke field
	 *       {@code kode}, setiap pemanggilan pada objek yang sama menghasilkan kode BERBEDA
	 *       (epoch bergerak dan counter naik). Jangan menjadikan hasil getter ini sebagai kunci
	 *       pencocokan, kunci cache, atau nilai yang dibandingkan antar dua pemanggilan.
	 *       Untuk membuat kode permanen, pemanggil harus memanggil {@link #setKode(String)}
	 *       secara eksplisit — dan itulah yang dilakukan
	 *       {@code PembelianAnggotaKoperasi.simpanRinci} saat menyalin kode dari draft.</li>
	 *   <li><b>{@code Common.increments} adalah counter statis proses tunggal</b>, bukan sekuens
	 *       database. Pada penerapan multi-node ia dapat menghasilkan angka yang sama di dua
	 *       node pada milidetik yang sama. Keunikan kode di sini karenanya bersifat "cukup baik
	 *       untuk label tampilan", bukan jaminan keunikan tingkat basis data — tidak ada
	 *       {@code unique} constraint pada kolom ini.</li>
	 * </ul>
	 * <p>Bila {@code kode} terisi, nilainya dikembalikan sudah ter-{@code trim()} sehingga spasi
	 * pinggir hasil impor data lama tidak bocor ke struk.</p>
	 *
	 * @return kode tersimpan yang sudah dipangkas, atau kode {@code "INV-..."} sekali pakai
	 */
	public String getKode() {
		return kode == null ? "INV-" + (ais.ui.util.WaktuUtil.getDate().getTime()) + (++Common.increments)
				: kode.trim();
	}

	/** Memasang kode baris. Satu-satunya cara membuat {@link #getKode()} stabil antar pemanggilan. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Snapshot nama produk untuk struk dan laporan, berbentuk {@code "<kode produk> <nama
	 * produk>"}.
	 *
	 * <p><b>Getter destruktif.</b> Setiap pemanggilan menimpa field {@code nama} dengan nilai
	 * SEGAR dari {@link Produk} yang tertaut, bila taut itu ada. Ini membalik makna kolom
	 * {@code nama} dari yang mungkin diharapkan: alih-alih menjadi snapshot beku pada saat
	 * penjualan, kolom ini justru <b>ikut berubah</b> setiap kali baris dibaca sesudah nama
	 * produk di katalog diubah. Snapshot yang benar-benar beku hanya tersisa pada baris yang
	 * produknya sudah dihapus/terputus ({@code getProduk()} mengembalikan {@code null}), karena
	 * pada kasus itu blok penimpaan dilewati dan nilai lama bertahan. Konsekuensi praktisnya:
	 * mencetak ulang struk lama dapat menampilkan nama produk versi hari ini, bukan versi saat
	 * transaksi terjadi.</p>
	 *
	 * <p>Method ini juga menugaskan hasil {@link #getProduk()} ke field {@code produk},
	 * sehingga proxy Hibernate yang belum ter-inisialisasi tergantikan objek yang sudah
	 * diperiksa {@code check(...)}. Karena penulisan field terjadi di dalam getter, membaca
	 * properti ini pada entity yang ter-attach dapat menandainya dirty (lihat JavaDoc kelas).</p>
	 *
	 * <p>Dipetakan {@code columnDefinition = "text"} sehingga tidak ada batas panjang di sisi
	 * database — berbeda dari {@link #getKeterangan()} yang dipotong di 254 karakter.</p>
	 *
	 * @return label produk gabungan, atau nilai tersimpan bila produk tidak tertaut
	 */
	@Column(columnDefinition = "text")
	public String getNama() {
		produk = getProduk();
		if (produk != null) {
			nama = produk.getKode() + " " + produk.getNama();
		}
		return nama;
	}

	/**
	 * Memasang snapshot nama produk. Perlu disadari bahwa nilai yang dipasang di sini akan
	 * <b>ditimpa</b> pada pemanggilan {@link #getNama()} berikutnya selama {@link #getProduk()}
	 * tidak {@code null}.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Kalimat deskripsi baris untuk struk/laporan, <b>dibangkitkan otomatis bila kosong</b> dan
	 * <b>dipotong keras di 254 karakter</b>.
	 *
	 * <p>Getter destruktif dengan tiga tahap yang semuanya menulis balik ke field:</p>
	 * <ol>
	 *   <li><b>Normalisasi null.</b> {@code null} diubah menjadi string kosong, sehingga
	 *       pemanggil (dan tahap berikutnya) tidak perlu memeriksa {@code null}. Efek sampingnya,
	 *       sekadar membaca properti ini mengubah baris yang kolomnya {@code NULL} di database
	 *       menjadi string kosong saat flush berikutnya.</li>
	 *   <li><b>Pembangkitan kalimat.</b> Bila masih kosong DAN produk tertaut, dirakit kalimat
	 *       {@code "<nama produk> sebanyak <qty> seharga <hargaJual>"}. Angka diformat lewat
	 *       {@code Common.numberFormat} yang berupa {@code ThreadLocal} — pilihan sengaja,
	 *       karena {@code NumberFormat} tidak thread-safe dan getter ini dipanggil dari thread
	 *       permintaan web mana pun. Perhatikan bahwa angka yang dicetak adalah
	 *       {@link #getHargaJual()}, yang pada baris hasil pembangkitan otomatis bernilai
	 *       <i>total</i> baris, bukan harga per unit — lihat peringatan di getter tersebut.</li>
	 *   <li><b>Pemotongan.</b> Bila panjang melebihi 254, dipotong ke 253 karakter
	 *       ({@code substring(0, 253)}) dan hasil potongan <b>ditulis balik permanen</b>.
	 *       Pemotongan ini destruktif dan tidak dapat dibatalkan: keterangan panjang yang
	 *       diketik petugas akan kehilangan ekornya begitu properti ini dibaca sekali saja pada
	 *       entity yang ter-attach. Angka 254/253 mencerminkan kolom {@code varchar(255)} di
	 *       database dan menyisakan satu karakter sebagai margin.</li>
	 * </ol>
	 * <p>Kontras dengan {@link #getNama()} yang dipetakan {@code text} tanpa batas — hanya
	 * keterangan yang dibatasi panjang.</p>
	 *
	 * @return keterangan baris; tidak pernah {@code null} setelah pemanggilan pertama
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		if (keterangan == null) {
			keterangan = "";
		}

		if (keterangan.isEmpty() && getProduk() != null) {
			keterangan = getProduk().getNama() + " sebanyak " + Common.numberFormat.get().format(getQty()) + " seharga "
					+ Common.numberFormat.get().format(getHargaJual());
		}

		if (keterangan.length() > 254) {
			keterangan = keterangan.substring(0, 253);
		}
		return this.keterangan;
	}

	/**
	 * Memasang keterangan baris apa adanya, tanpa pemotongan. Pemotongan baru terjadi saat
	 * {@link #getKeterangan()} dipanggil — jadi nilai panjang yang dipasang di sini masih utuh
	 * sampai dibaca sekali.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * <b>PERINGATAN: meski namanya "harga jual", isinya adalah NILAI BARIS (qty × harga), bukan
	 * harga per unit</b> — setidaknya pada setiap baris yang nilainya dibangkitkan di sini.
	 *
	 * <p>Getter destruktif dengan pembangkitan malas. Bila {@code hargaJual} masih {@code null}
	 * <b>atau bernilai tepat {@code 0.0}</b>, method menghitung {@code qty × Produk.hargaJual}
	 * dan menuliskannya ke field. Harga per unit yang sesungguhnya disimpan terpisah di
	 * {@link #getHargaSatuan()}. Ketidakselarasan penamaan ini nyata dan sudah menyebar ke
	 * konsumen: {@link #getKeterangan()} mencetak nilai ini di belakang kata "seharga",
	 * sehingga kalimat "sebanyak 3 seharga 30.000" sebetulnya berarti total 30.000 untuk 3
	 * unit, bukan 30.000 per unit.</p>
	 *
	 * <p>Perhatikan syarat pembangkitan ulang mencakup {@code 0.0}, bukan hanya {@code null}.
	 * Artinya baris yang <b>sengaja</b> bernilai nol — barang gratis, hadiah, sampel, atau
	 * produk ekstra yang harganya sudah ikut di baris induk — akan <b>terus-menerus</b> dicoba
	 * dihitung ulang setiap kali properti dibaca. Selama produk masih punya harga jual di
	 * katalog, nilai nol yang disengaja itu akan berubah sendiri menjadi angka positif dan
	 * tersimpan saat flush. Untuk mempertahankan nilai nol, baris tersebut harus diputus dari
	 * produk atau produknya berharga nol — tidak ada penanda "nol yang disengaja" di entity
	 * ini.</p>
	 *
	 * <p>Perhitungannya sendiri defensif: {@code null} pada qty maupun harga produk
	 * diperlakukan {@code 0.0}, sehingga tidak pernah melempar {@code NullPointerException}
	 * walau produk belum punya harga. Pemakaian {@code new Double(...)} adalah gaya lama
	 * pra-autoboxing yang dipertahankan agar konsisten dengan sisa berkas; secara fungsional
	 * setara dengan {@code Double.valueOf(...)}.</p>
	 *
	 * @return nilai jual baris; tidak pernah {@code null}
	 */
	public Double getHargaJual() {
		if (hargaJual == null || hargaJual.doubleValue() == 0.0) {
			Produk produkAktif = getProduk();
			Double qtyAktif = getQty();
			double qtyAman = qtyAktif == null ? 0.0 : qtyAktif.doubleValue();
			double hargaProduk = produkAktif == null || produkAktif.getHargaJual() == null ? 0.0
					: produkAktif.getHargaJual().doubleValue();
			hargaJual = new Double(qtyAman * hargaProduk);
		}
		return hargaJual == null ? 0.0 : hargaJual;
	}

	/**
	 * Memasang nilai jual baris. Memasang {@code null} atau {@code 0.0} <b>tidak bertahan</b>:
	 * pemanggilan {@link #getHargaJual()} berikutnya akan menghitung ulang dan menimpanya.
	 */
	public void setHargaJual(Double hargaJual) {
		this.hargaJual = hargaJual;
	}

	/**
	 * Flag aktif baris, dengan bawaan {@code true} untuk nilai {@code null}.
	 *
	 * <p>Bawaan "null berarti aktif" ini adalah pola <b>flag aktif satu arah</b> yang dipakai
	 * luas di model AIS: seluruh baris lama yang ditulis sebelum kolom ini ada otomatis
	 * terbaca aktif, sehingga penambahan kolom tidak menyembunyikan data historis dari laporan.
	 * Harganya, membedakan "belum pernah diputuskan" dari "sengaja diaktifkan" mustahil lewat
	 * getter ini — keduanya sama-sama {@code true}. Kode yang perlu tahu bedanya harus membaca
	 * kolom mentah lewat SQL.</p>
	 *
	 * @return {@code true} bila baris aktif atau belum pernah ditentukan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** Memasang flag aktif. Memasang {@code null} setara dengan mengembalikannya ke keadaan "aktif". */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * SKU yang dijual pada baris ini — taut satu-satunya ke katalog produk, dan kunci yang
	 * dipakai rumus stok untuk mengurangi persediaan.
	 *
	 * <p>Getter melewatkan field ke {@code check(...)} milik {@link GeneralValueObject}, helper
	 * bersama yang menormalkan proxy Hibernate: bila relasi lazy ini belum ter-inisialisasi dan
	 * Session-nya sudah tertutup, {@code check} mengembalikan {@code null} alih-alih membiarkan
	 * {@code LazyInitializationException} meledak di lapisan tampilan. Ini membuat seluruh
	 * getter relasi di kelas ini aman dipanggil dari JSP/ZK di luar transaksi, dengan
	 * konsekuensi penting: <b>{@code null} di sini ambigu</b> — bisa berarti "baris memang tidak
	 * punya produk" atau "produknya ada tetapi tidak dapat dimuat sekarang". Kode yang
	 * mengambil keputusan finansial berdasar ada/tidaknya produk harus memastikan dirinya
	 * berjalan di dalam Session yang masih terbuka.</p>
	 *
	 * <p>Dipetakan {@code nullable = true} sehingga baris tanpa produk sah secara skema —
	 * dipakai antara lain oleh baris jasa/biaya bebas yang diketik manual kasir.</p>
	 *
	 * @return produk yang dijual, atau {@code null} (lihat catatan ambiguitas di atas)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk", nullable = true)
	public Produk getProduk() {
		produk = check(produk);
		return produk;
	}

	/**
	 * Memasang produk yang dijual. Karena {@code cascade = PERSIST, MERGE}, memasang objek
	 * {@link Produk} yang belum tersimpan akan ikut menyimpannya saat baris ini disimpan —
	 * pastikan yang dipasang adalah produk katalog yang sudah ada, bukan objek baru.
	 */
	public void setProduk(Produk produk) {
		this.produk = produk;
	}

	/**
	 * Pembeli bila ia berstatus {@link Siswa}, dengan <b>tiga lapis sumber yang saling
	 * menimpa</b> menurut urutan prioritas tetap.
	 *
	 * <p>Getter destruktif. Urutan penimpaannya, dari yang paling lemah ke yang paling kuat:</p>
	 * <ol>
	 *   <li><b>Kolom sendiri</b> ({@code siswa}), dinormalkan lewat {@code check(...)}.</li>
	 *   <li><b>Turunan header</b> — bila baris ini punya {@link #getPembelianAnggotaKoperasi()}
	 *       yang anggotanya ternyata seorang siswa, nilai itu MENGGANTIKAN kolom sendiri. Ini
	 *       menjaga konsistensi struk: satu struk hanya punya satu pembeli, dan pembeli yang
	 *       sah adalah yang tercatat di header, bukan yang mungkin tertinggal di baris.</li>
	 *   <li><b>Turunan pembayaran online</b> — bila {@code kodePembayaranOnline} terisi,
	 *       siswanya menang atas keduanya. Perhatikan lapis ini membaca field {@code
	 *       kodePembayaranOnline} <b>langsung</b>, bukan lewat getter, sehingga ia tidak akan
	 *       aktif bila relasi tersebut masih berupa proxy yang belum termuat — perbedaan halus
	 *       yang membuat hasil getter ini dapat berbeda tergantung apakah
	 *       {@link #getKodePembayaranOnline()} sudah pernah dipanggil sebelumnya dalam Session
	 *       yang sama.</li>
	 * </ol>
	 * <p>Lapis ketiga bersifat menimpa <b>tanpa syarat</b>: bila kode pembayaran online ada
	 * tetapi siswanya {@code null} (mis. topup oleh pegawai), field {@code siswa} di baris ini
	 * ikut <b>dikosongkan</b>. Itu memang yang dikehendaki — identitas pembeli harus mengikuti
	 * satu sumber kebenaran — tetapi berarti nilai kolom {@code siswa} di database dapat
	 * terhapus hanya karena baris dibaca.</p>
	 *
	 * @return siswa pembeli, atau {@code null} bila pembelinya bukan siswa
	 * @see #getMember() perakit label identitas yang menelusuri keenam kolom pembeli
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = true)
	public Siswa getSiswa() {
		siswa = check(siswa);

		if (getPembelianAnggotaKoperasi() != null && getPembelianAnggotaKoperasi().getAnggotaKoperasi() != null
				&& getPembelianAnggotaKoperasi().getAnggotaKoperasi().getSiswa() != null) {
			siswa = getPembelianAnggotaKoperasi().getAnggotaKoperasi().getSiswa();
		}
		if (kodePembayaranOnline != null) {
			siswa = kodePembayaranOnline.getSiswa();
		}
		return siswa;
	}

	/**
	 * Memasang siswa pembeli. Nilai ini <b>dapat ditimpa</b> pada pembacaan berikutnya oleh
	 * header atau kode pembayaran online — lihat urutan prioritas di {@link #getSiswa()}.
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Jumlah unit terjual dalam <b>satuan dasar</b>, dengan bawaan {@code 1.0} untuk
	 * {@code null}.
	 *
	 * <p>Inilah angka yang masuk ke rumus stok ({@code Σpembelian.qty} sebagai pengurang) dan
	 * ke perhitungan {@link #getTotal()} serta HPP. Bawaannya {@code 1.0}, bukan {@code 0.0} —
	 * pilihan yang masuk akal untuk transaksi ritel (baris tanpa qty hampir pasti berarti satu
	 * unit) tetapi perlu diwaspadai karena artinya <b>tidak ada cara mengungkapkan "qty belum
	 * diisi"</b> lewat getter ini: baris yang kolomnya {@code NULL} akan terhitung mengurangi
	 * stok sebanyak satu unit dan menagih pembeli satu unit.</p>
	 *
	 * <p>Bertipe {@code Double}, bukan bilangan bulat, karena produk dapat dijual dalam satuan
	 * pecahan (mis. 0,5 kg). Konsekuensi umum aritmetika titik-mengambang berlaku: jangan
	 * membandingkan qty dengan {@code ==} pada nilai hasil perhitungan.</p>
	 *
	 * <p>Bila kasir mengetik dalam satuan lain (mis. "2 Karung"), angka mentah ketikan itu
	 * disimpan di {@link #getQtyInput()} dan faktor konversinya di {@link #getFaktorKeDasar()};
	 * {@code qty} di sini SELALU sudah dikonversi ke satuan dasar sehingga seluruh rumus
	 * stok/HPP tidak perlu tahu soal satuan jual sama sekali.</p>
	 *
	 * @return jumlah unit dalam satuan dasar; tidak pernah {@code null}
	 */
	public Double getQty() {
		return qty == null ? 1.0 : qty;
	}

	/** Memasang jumlah unit dalam satuan dasar. Memasang {@code null} membuat getter mengembalikan {@code 1.0}. */
	public void setQty(Double qty) {
		this.qty = qty;
	}

	/**
	 * Id {@code koperasi.satuan_produk} yang dipilih kasir; null = satuan dasar.
	 *
	 * <p>Disimpan sebagai {@code Long} polos, <b>bukan</b> relasi {@code @ManyToOne}. Ini
	 * disengaja dan konsisten dengan dua saudaranya di bawah: ketiganya adalah <i>snapshot</i>
	 * tampilan/audit, sehingga justru TIDAK boleh mengikuti perubahan master satuan di masa
	 * depan. Relasi objek akan memaksa nilai selalu segar dan merusak tujuan snapshot itu
	 * sendiri; menyimpan id mentah membuat baris tetap terbaca "2 Karung" walau preset satuan
	 * "Karung" kelak diubah atau dihapus.</p>
	 *
	 * <p>Getter murni tanpa efek samping — pengecualian di kelas yang mayoritas getternya
	 * destruktif.</p>
	 *
	 * @return id satuan jual, atau {@code null} bila transaksi memakai satuan dasar
	 */
	@Column(name = "satuan_jual", nullable = true)
	public Long getSatuanJual() { return satuanJual; }
	/** Memasang id satuan jual snapshot. Diisi jalur POS langsung ({@code PembelianAnggotaKoperasi.simpanRinci}) dari payload {@code satuan_jual_id}. */
	public void setSatuanJual(Long satuanJual) { this.satuanJual = satuanJual; }

	/**
	 * Qty yang DIKETIK kasir dalam satuan jual (mis. 2 utk "2 Karung").
	 *
	 * <p>Angka ini <b>tidak pernah</b> dipakai rumus stok, HPP, maupun total tagihan — seluruh
	 * perhitungan memakai {@link #getQty()} yang sudah dalam satuan dasar. Fungsinya murni agar
	 * struk dan laporan dapat menampilkan kembali apa yang benar-benar diketik petugas.
	 * Karenanya nilai {@code null} di sini bukan kelainan: ia normal untuk seluruh baris yang
	 * dijual dalam satuan dasar, dan untuk seluruh baris yang dibuat sebelum fitur ini ada.</p>
	 *
	 * @return qty dalam satuan jual, atau {@code null}
	 */
	@Column(name = "qty_input", nullable = true)
	public Double getQtyInput() { return qtyInput; }
	/** Memasang qty ketikan kasir dalam satuan jual. Murni snapshot tampilan; tidak memengaruhi {@link #getQty()}. */
	public void setQtyInput(Double qtyInput) { this.qtyInput = qtyInput; }

	/**
	 * Faktor konversi saat transaksi (snapshot): qty = qtyInput * faktor.
	 *
	 * <p>Dibekukan pada saat transaksi justru supaya perubahan faktor konversi di master satuan
	 * kelak tidak menulis ulang sejarah. Bila faktor "1 Karung = 25 kg" suatu hari direvisi jadi
	 * 20 kg, baris lama tetap dapat dijelaskan sebagai "2 Karung × 25 = 50 kg" karena angka 25
	 * tersimpan di kolom ini, bukan dibaca ulang dari master.</p>
	 *
	 * <p>Perhatikan tidak ada penjaga konsistensi yang memaksa
	 * {@code qty == qtyInput × faktorKeDasar}: ketiga kolom diisi bersama oleh pemanggil di
	 * {@code PembelianAnggotaKoperasi.simpanRinci}, dan entity ini tidak memverifikasinya. Baris
	 * yang ketiganya terisi tetapi tidak konsisten hanya mungkin lahir dari penulisan langsung
	 * ke database atau dari pemanggil baru yang lalai.</p>
	 *
	 * @return faktor konversi ke satuan dasar, atau {@code null} bila memakai satuan dasar
	 */
	@Column(name = "faktor_ke_dasar", nullable = true)
	public Double getFaktorKeDasar() { return faktorKeDasar; }
	/** Memasang faktor konversi snapshot. Harus dipasang bersama {@link #setSatuanJual(Long)} dan {@link #setQtyInput(Double)} agar triplet-nya bermakna. */
	public void setFaktorKeDasar(Double faktorKeDasar) { this.faktorKeDasar = faktorKeDasar; }

	/**
	 * Snapshot nama outlet ("kios") tempat baris ini terjadi, untuk ditampilkan di struk dan
	 * laporan tanpa perlu join ke tabel {@link Toko}.
	 *
	 * <p>Getter destruktif ganda: ia menormalkan field {@code toko} lewat {@code check(...)}
	 * DAN menimpa field {@code kios} dengan nama toko yang berlaku sekarang. Sama seperti
	 * {@link #getNama()}, ini membuat kolom {@code kios} bukan snapshot beku melainkan cermin
	 * yang ikut berubah ketika nama toko diganti — struk lama akan tercetak dengan nama outlet
	 * versi terkini. Nama lama hanya bertahan pada baris yang tautan tokonya sudah putus.</p>
	 *
	 * <p>Perhatikan getter ini memanggil {@code check(toko)} langsung, <b>bukan</b>
	 * {@link #getToko()}. Bedanya bermakna: {@link #getToko()} punya lapis tambahan yang
	 * menimpa toko dari {@code kodePembayaranOnline}. Jadi pada transaksi topup online,
	 * {@code getKios()} dapat melaporkan nama outlet yang <b>berbeda</b> dari
	 * {@code getToko().getNama()} selama {@link #getToko()} belum pernah dipanggil lebih dulu
	 * dalam pemuatan yang sama.</p>
	 *
	 * @return nama outlet, atau nilai tersimpan bila toko tidak tertaut
	 */
	public String getKios() {
		toko = check(toko);
		if (toko != null) {
			kios = toko.getNama();
		}
		return kios;
	}

	/** Memasang snapshot nama outlet. Akan ditimpa {@link #getKios()} selama {@code toko} tertaut. */
	public void setKios(String kios) {
		this.kios = kios;
	}

	/**
	 * Waktu transaksi baris ini, dengan <b>pengambilalihan oleh header</b> dan <b>bawaan waktu
	 * sekarang</b>.
	 *
	 * <p>Getter destruktif dengan dua perilaku yang perlu dipisahkan:</p>
	 * <ul>
	 *   <li><b>Pengambilalihan header.</b> Bila {@link #getPembelianAnggotaKoperasi()} ada,
	 *       field {@code waktu} DITIMPA dengan {@code header.getTanggalPembayaran()}. Ini
	 *       menegakkan aturan akuntansi yang benar: seluruh baris dalam satu struk harus jatuh
	 *       pada saat yang sama, yaitu saat pembayaran — bukan saat masing-masing barang
	 *       dipindai. Aturan ini penting untuk pesanan yang ditahan lama: sebuah pesanan meja
	 *       yang dibuka pukul 10 pagi dan dibayar pukul 2 siang harus masuk periode pukul 2,
	 *       karena pukul 2 itulah pendapatan diakui. Sisi tajamnya, penimpaan terjadi
	 *       <b>tanpa syarat</b>, jadi bila tanggal bayar header {@code null}, field {@code waktu}
	 *       baris ikut dikosongkan dan jatuh ke bawaan di bawah.</li>
	 *   <li><b>Bawaan waktu sekarang.</b> Bila {@code waktu} akhirnya {@code null}, yang
	 *       dikembalikan adalah {@code WaktuUtil.getDate()} — waktu SAAT DIBACA. Nilai bawaan
	 *       ini sengaja TIDAK ditulis balik ke field, sehingga pembacaan berikutnya menghasilkan
	 *       waktu yang berbeda lagi. Untuk laporan berbasis rentang tanggal, baris tanpa
	 *       {@code waktu} karenanya selalu tampak "baru saja terjadi" dan dapat muncul di
	 *       periode yang salah. Pemanggil yang menyimpan baris wajib memasang waktu eksplisit
	 *       lewat {@link #setWaktu(Date)} — itulah yang dilakukan
	 *       {@code PembelianAnggotaKoperasi.simpanRinci}.</li>
	 * </ul>
	 *
	 * @return waktu transaksi; tidak pernah {@code null}, tetapi bisa tidak stabil (lihat di atas)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		if (getPembelianAnggotaKoperasi() != null) {
			waktu = getPembelianAnggotaKoperasi().getTanggalPembayaran();
		}
		return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
	}

	/**
	 * Memasang waktu transaksi. Nilai ini akan <b>ditimpa</b> tanggal bayar header pada
	 * pembacaan berikutnya bila baris ini punya header — lihat {@link #getWaktu()}.
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Merakit satu string identitas pembeli untuk ditampilkan di struk, grid, dan laporan —
	 * menelusuri <b>enam kolom identitas</b> dalam urutan prioritas tetap dan memakai yang
	 * pertama terisi.
	 *
	 * <p>Getter destruktif (menulis balik ke field {@code member}). Urutan penelusurannya,
	 * beserta format yang dihasilkan:</p>
	 * <ol>
	 *   <li>{@link #getAnggotaKoperasi()} &rarr; {@code "<kode> <nama>"}</li>
	 *   <li>{@link #getSiswa()} &rarr; {@code "<NISN> <nama>"}</li>
	 *   <li>{@link #getMahasiswa()} &rarr; {@code "<NIM> <nama>"}</li>
	 *   <li>{@link #getBiodataCalonMahasiswa()} &rarr; {@code "<no registrasi> <nama>"}</li>
	 *   <li>{@link #getCalonSiswa()} &rarr; {@code "<no registrasi> <nama>"}</li>
	 *   <li>anggota koperasi dari header &rarr; {@code "<kode identitas> <nama>"}</li>
	 *   <li>{@link #getTbmuser()} &rarr; nama pengguna saja, tanpa nomor identitas</li>
	 * </ol>
	 * <p>Dua hal yang mudah terlewat. <b>Pertama</b>, cabang pertama dan cabang keenam
	 * sama-sama mengambil anggota koperasi, tetapi memakai properti nomor yang BERBEDA:
	 * {@code getKode()} pada cabang pertama versus {@code getKodeIdentitas()} pada cabang
	 * keenam. Karena {@link #getAnggotaKoperasi()} sendiri sudah mengambil alih nilainya dari
	 * header bila header ada, cabang keenam praktis hampir tak pernah tercapai — ia peninggalan
	 * dari masa sebelum pengambilalihan itu ada, dan tetap dipertahankan sebagai jaring
	 * pengaman untuk data lama.</p>
	 * <p><b>Kedua</b>, method ini memanggil getter-getter destruktif lain sebagai bagian dari
	 * penelusurannya. Sekadar menampilkan nama pembeli di sebuah grid karenanya dapat memicu
	 * penimpaan kolom {@code siswa}, {@code mahasiswa}, {@code tbmuser}, dan lainnya sekaligus.
	 * Ini penyebab paling umum entity {@code Pembelian} menjadi dirty tanpa ada kode yang
	 * terlihat "mengubah" apa pun.</p>
	 * <p>Bila seluruh cabang gagal, nilai lama field {@code member} dikembalikan apa adanya —
	 * dapat {@code null} untuk transaksi tanpa identitas (pembeli umum tunai).</p>
	 *
	 * @return label identitas pembeli, atau {@code null} bila tidak ada identitas sama sekali
	 * @see #getJenisMember() pendamping yang menghasilkan LABEL KATEGORI dari kolom yang sama
	 */
	public String getMember() {
		if (getAnggotaKoperasi() != null) {
			member = getAnggotaKoperasi().getKode() + " " + getAnggotaKoperasi().getNama();
		} else if (getSiswa() != null) {
			member = getSiswa().getNomorIndukNasional() + " " + getSiswa().getNama();
		} else if (getMahasiswa() != null) {
			member = getMahasiswa().getNim() + " " + getMahasiswa().getNama();
		} else if (getBiodataCalonMahasiswa() != null) {
			member = getBiodataCalonMahasiswa().getNoRegistrasi() + " " + getBiodataCalonMahasiswa().getNama();
		} else if (getCalonSiswa() != null) {
			member = getCalonSiswa().getNoRegistrasi() + " " + getCalonSiswa().getNama();
		} else if (getPembelianAnggotaKoperasi() != null
				&& getPembelianAnggotaKoperasi().getAnggotaKoperasi() != null) {
			member = getPembelianAnggotaKoperasi().getAnggotaKoperasi().getKodeIdentitas() + " "
					+ getPembelianAnggotaKoperasi().getAnggotaKoperasi().getNama();
		} else if (getTbmuser() != null) {
			member = getTbmuser().getUserNama();
		}
		return member;
	}

	/** Memasang label identitas pembeli. Hampir selalu ditimpa {@link #getMember()} pada pembacaan berikutnya. */
	public void setMember(String member) {
		this.member = member;
	}

	/**
	 * Outlet tempat baris ini terjadi — <b>pembatas tenant utama</b> untuk seluruh modul kantin.
	 *
	 * <p>Hampir setiap pemeriksaan kepemilikan di lapisan API membandingkan
	 * {@code pedagang.getToko().getId()} dengan toko baris/produk yang sedang diakses, sehingga
	 * kolom ini adalah pemisah data antar-outlet yang sebenarnya. Dipetakan
	 * {@code nullable = false}: setiap baris penjualan WAJIB punya outlet, dan
	 * {@link #Pembelian()} yang tidak diisi toko akan gagal saat disimpan.</p>
	 *
	 * <p>Getter destruktif dengan satu lapis penimpaan: bila {@code kodePembayaranOnline}
	 * terisi, toko diambil dari sana dan menggantikan kolom sendiri. Alasannya, pada transaksi
	 * topup online outlet yang sah adalah outlet yang terdaftar di kode pembayaran, bukan yang
	 * kebetulan tersimpan di baris. Namun penimpaan ini <b>tanpa penjaga {@code null}</b>: bila
	 * kode pembayaran online ada tetapi tokonya {@code null}, field {@code toko} baris ini ikut
	 * dikosongkan. Karena kolomnya {@code nullable = false}, baris yang ter-attach dan dibaca
	 * dalam kondisi itu dapat menyebabkan kegagalan constraint saat flush — bukan pada baris
	 * kode yang terlihat mengubah apa pun, melainkan pada pembacaan biasa.</p>
	 *
	 * <p>Seperti pada {@link #getSiswa()}, lapis penimpaan membaca field
	 * {@code kodePembayaranOnline} secara langsung, bukan lewat getter-nya.</p>
	 *
	 * @return outlet baris ini
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko", nullable = false)
	public Toko getToko() {
		toko = check(toko);
		if (kodePembayaranOnline != null) {
			toko = kodePembayaranOnline.getToko();
		}
		return toko;
	}

	/** Memasang outlet. Wajib diisi sebelum penyimpanan karena kolomnya {@code nullable = false}. */
	public void setToko(Toko toko) {
		this.toko = toko;
	}

	/**
	 * Pembeli bila ia {@link CalonSiswa} — pendaftar yang belum resmi diterima, mis. yang
	 * membeli formulir atau seragam saat pendaftaran.
	 *
	 * <p>Getter destruktif dengan dua lapis: kolom sendiri (dinormalkan {@code check(...)}),
	 * lalu penimpaan tanpa syarat dari {@code kodePembayaranOnline} bila taut itu ada.
	 * Berbeda dari {@link #getSiswa()}, di sini <b>tidak ada</b> lapis turunan dari header —
	 * {@link AnggotaKoperasi} memang tidak punya taut ke calon siswa, sehingga identitas calon
	 * siswa hanya dapat berasal dari kolom baris atau dari kode pembayaran online.</p>
	 *
	 * @return calon siswa pembeli, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "calon_siswa", nullable = true)
	public CalonSiswa getCalonSiswa() {
		calonSiswa = check(calonSiswa);

		if (kodePembayaranOnline != null) {
			calonSiswa = kodePembayaranOnline.getCalonSiswa();
		}

		return calonSiswa;
	}

	/** Memasang calon siswa pembeli. Dapat ditimpa kode pembayaran online — lihat {@link #getCalonSiswa()}. */
	public void setCalonSiswa(CalonSiswa calonSiswa) {
		this.calonSiswa = calonSiswa;
	}

	/**
	 * Harga jual <b>satu unit</b> — inilah harga per unit yang sesungguhnya, berbeda dari
	 * {@link #getHargaJual()} yang justru menyimpan nilai baris. Nilai inilah yang dipakai
	 * {@link #getTotal()}.
	 *
	 * <p>Getter destruktif dengan pembangkitan malas dari katalog, tetapi dengan <b>dua
	 * perbedaan penting</b> dibanding {@link #getHargaJual()}:</p>
	 * <ul>
	 *   <li>Syaratnya hanya {@code hargaSatuan == null}, <b>tidak</b> termasuk {@code 0.0}.
	 *       Artinya harga nol yang disengaja (barang gratis, hadiah) BERTAHAN di sini dan tidak
	 *       akan dihitung ulang — kebalikan dari perilaku {@link #getHargaJual()}. Bila sebuah
	 *       baris harus benar-benar bernilai nol, {@code hargaSatuan} adalah kolom yang bisa
	 *       diandalkan untuk itu, dan {@link #getTotal()} yang membacanya akan ikut nol.</li>
	 *   <li>Membaca field {@code produk} secara <b>langsung</b>, bukan lewat
	 *       {@link #getProduk()}. Akibatnya, bila relasi produk masih berupa proxy lazy yang
	 *       belum pernah disentuh, kondisi {@code produk != null} tetap benar dan
	 *       {@code produk.getHargaJual()} akan memicu inisialisasi proxy — yang di luar Session
	 *       melempar {@code LazyInitializationException} alih-alih dilindungi
	 *       {@code check(...)} seperti getter relasi lainnya. Pemanggil di lapisan tampilan
	 *       sebaiknya memanggil {@link #getProduk()} lebih dulu bila baris berpotensi
	 *       ter-detach.</li>
	 * </ul>
	 * <p>Harga diambil dari {@code Produk.getHargaJual()} <b>saat dibaca</b>, sehingga baris
	 * lama yang kolom harganya kosong akan memakai harga katalog hari ini, bukan harga saat
	 * transaksi. Itu sebabnya jalur POS normal selalu memasang harga eksplisit lewat
	 * {@link #setHargaSatuan(Double)} — pembangkitan di sini adalah jaring pengaman untuk data
	 * impor, bukan alur utama.</p>
	 *
	 * @return harga jual per unit; tidak pernah {@code null}
	 */
	public Double getHargaSatuan() {
		if (hargaSatuan == null && produk != null) {
			hargaSatuan = produk.getHargaJual();
		}
		return hargaSatuan == null ? 0.0 : hargaSatuan;
	}

	/**
	 * Memasang harga jual per unit — snapshot harga saat transaksi. Berbeda dari
	 * {@link #setHargaJual(Double)}, nilai {@code 0.0} yang dipasang di sini BERTAHAN.
	 */
	public void setHargaSatuan(Double hargaSatuan) {
		this.hargaSatuan = hargaSatuan;
	}

	/**
	 * Jejak berkas unggahan bila baris ini lahir dari impor massal, bukan dari kasir.
	 *
	 * <p>Berguna untuk membatalkan satu batch impor secara menyeluruh: seluruh baris dengan
	 * {@code upload_log} yang sama dapat ditemukan dan dibersihkan sekaligus. Bernilai
	 * {@code null} — dan itu keadaan normal — untuk setiap baris yang dibuat lewat POS.</p>
	 *
	 * <p>Getter murni tanpa efek samping. Perhatikan relasi ini TIDAK diberi
	 * {@code fetch = LAZY} melainkan dibiarkan pada bawaan {@code EAGER} milik
	 * {@code @ManyToOne}, dengan {@code @Fetch(FetchMode.SELECT)} yang memaksa pemuatannya
	 * lewat SELECT terpisah alih-alih ikut dalam JOIN kueri utama. Kombinasi ini menghindari
	 * pembengkakan kueri daftar penjualan yang sudah banyak join, dengan konsekuensi satu
	 * SELECT tambahan per baris ketika daftar dimuat.</p>
	 *
	 * @return info unggahan asal, atau {@code null} untuk baris hasil transaksi normal
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "upload_log", nullable = true)
	public UploadLogInfo getUploadLog() {
		return uploadLog;
	}

	/** Memasang jejak berkas unggahan asal baris ini. Diisi hanya oleh alur impor massal. */
	public void setUploadLog(UploadLogInfo uploadLog) {
		this.uploadLog = uploadLog;
	}

	/**
	 * Pembeli bila ia {@link Mahasiswa}. Tiga lapis sumber, persis sepola {@link #getSiswa()}:
	 * kolom sendiri, lalu mahasiswa milik anggota koperasi di header, lalu mahasiswa milik kode
	 * pembayaran online sebagai lapis terkuat.
	 *
	 * <p>Getter destruktif — setiap lapis menulis balik ke field. Sama seperti pada
	 * {@link #getSiswa()}, lapis header hanya menimpa bila hasilnya bukan {@code null}
	 * (dijaga oleh rantai pemeriksaan bertingkat), sedangkan lapis kode pembayaran online
	 * menimpa <b>tanpa syarat</b> dan dapat mengosongkan kolom.</p>
	 *
	 * @return mahasiswa pembeli, atau {@code null} bila pembelinya bukan mahasiswa
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);

		if (getPembelianAnggotaKoperasi() != null && getPembelianAnggotaKoperasi().getAnggotaKoperasi() != null
				&& getPembelianAnggotaKoperasi().getAnggotaKoperasi().getMahasiswa() != null) {
			mahasiswa = getPembelianAnggotaKoperasi().getAnggotaKoperasi().getMahasiswa();
		}

		if (kodePembayaranOnline != null) {
			mahasiswa = kodePembayaranOnline.getMahasiswa();
		}

		return mahasiswa;
	}

	/** Memasang mahasiswa pembeli. Dapat ditimpa header/kode pembayaran online — lihat {@link #getMahasiswa()}. */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Pembeli bila ia {@link BiodataCalonMahasiswa} — pendaftar PMB yang belum menjadi
	 * mahasiswa, mis. yang membeli formulir pendaftaran di koperasi kampus.
	 *
	 * <p>Getter destruktif dua lapis: kolom sendiri lalu penimpaan tanpa syarat dari
	 * {@code kodePembayaranOnline}. Seperti {@link #getCalonSiswa()}, tidak ada lapis turunan
	 * dari header karena {@link AnggotaKoperasi} tidak menaut calon mahasiswa.</p>
	 *
	 * @return calon mahasiswa pembeli, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "biodata_calon_mahasiswa", nullable = true)
	public BiodataCalonMahasiswa getBiodataCalonMahasiswa() {
		biodataCalonMahasiswa = check(biodataCalonMahasiswa);
		if (kodePembayaranOnline != null) {
			biodataCalonMahasiswa = kodePembayaranOnline.getBiodataCalonMahasiswa();
		}
		return biodataCalonMahasiswa;
	}

	/** Memasang calon mahasiswa pembeli. Dapat ditimpa kode pembayaran online. */
	public void setBiodataCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	/**
	 * Pembeli bila ia pengguna internal ({@link Tbmuser}: guru, dosen, pegawai, atau pemegang
	 * peran lain) — kolom identitas dengan <b>prioritas paling rendah</b>, dan satu-satunya yang
	 * dapat DIKOSONGKAN secara aktif oleh getter-nya sendiri.
	 *
	 * <p>Getter destruktif tiga lapis, dengan lapis ketiga yang berperilaku unik:</p>
	 * <ol>
	 *   <li>Kolom sendiri, dinormalkan {@code check(...)}.</li>
	 *   <li>Penimpaan tanpa syarat dari {@code kodePembayaranOnline}, sepola getter identitas
	 *       lainnya.</li>
	 *   <li><b>Pengosongan paksa.</b> Bila SALAH SATU dari {@link #getSiswa()},
	 *       {@link #getCalonSiswa()}, {@link #getBiodataCalonMahasiswa()}, atau
	 *       {@link #getMahasiswa()} terisi, field {@code tbmuser} dipaksa menjadi {@code null}.
	 *       Ini menegakkan aturan bahwa identitas akademik selalu mengalahkan identitas akun
	 *       sistem: seorang mahasiswa yang juga punya akun {@code Tbmuser} harus tercatat
	 *       sebagai mahasiswa, bukan sebagai "pengguna". Tanpa aturan ini,
	 *       {@link #getJenisMember()} dapat melabelinya "Karyawan" hanya karena akunnya tertaut
	 *       ke data pegawai.</li>
	 * </ol>
	 * <p>Konsekuensi yang harus disadari: pengosongan itu bukan sekadar memengaruhi nilai
	 * kembalian, melainkan <b>menulis {@code null} ke field</b>. Pada entity yang ter-attach,
	 * membaca properti ini akan menghapus taut {@code tbmuser} di database saat flush. Bila
	 * suatu saat diperlukan jejak "akun mana yang dipakai bertransaksi" secara terpisah dari
	 * "siapa pembelinya", kolom ini bukan tempat yang aman untuk menyimpannya — pakai
	 * {@link #getOlehId()} yang memang diperuntukkan bagi jejak petugas.</p>
	 *
	 * <p>Perhatikan pula lapis ketiga memanggil empat getter destruktif sekaligus, sehingga
	 * membaca properti ini berpotensi memicu penimpaan pada empat kolom identitas lain.</p>
	 *
	 * @return pengguna internal pembeli, atau {@code null} bila ada identitas akademik yang
	 *         mengalahkannya
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		tbmuser = check(tbmuser);

		if (kodePembayaranOnline != null) {
			tbmuser = kodePembayaranOnline.getTbmuser();
		}

		if (getSiswa() != null || getCalonSiswa() != null || getBiodataCalonMahasiswa() != null
				|| getMahasiswa() != null) {
			tbmuser = null;
		}

		return tbmuser;
	}

	/**
	 * Memasang pengguna internal sebagai pembeli. Nilai ini akan <b>dibuang</b> pada pembacaan
	 * berikutnya bila baris juga punya identitas akademik — lihat {@link #getTbmuser()}.
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * Header/struk induk yang memuat baris ini — taut anak&rarr;induk dari rantai dokumen
	 * penjualan.
	 *
	 * <p>Relasi ini adalah sumber kebenaran bagi banyak getter lain di kelas ini:
	 * {@link #getWaktu()} mengambil tanggal bayarnya, {@link #getAnggotaKoperasi()} dan
	 * {@link #getCaraPembayaranKoperasi()} mengambil alih nilainya dari sini, dan
	 * {@link #getSiswa()}/{@link #getMahasiswa()} memakainya sebagai lapis kedua. Karena itu,
	 * baris dengan header {@code null} berperilaku cukup berbeda dari baris yang punya
	 * header — bukan sekadar "kehilangan satu taut".</p>
	 *
	 * <p>Dipetakan {@code nullable = true}, dan itu bukan kelalaian: baris penjualan dapat sah
	 * tanpa header pada transaksi kasir sederhana yang tidak membentuk dokumen struk koperasi.
	 * Getter murni-normalisasi ({@code check(...)}) tanpa lapis penimpaan.</p>
	 *
	 * @return header struk, atau {@code null} untuk baris tanpa dokumen induk
	 * @see PembelianAnggotaKoperasi#simpanRinci pembuat baris-baris ini
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pembelian_anggota_koperasi", nullable = true)
	public PembelianAnggotaKoperasi getPembelianAnggotaKoperasi() {
		pembelianAnggotaKoperasi = check(pembelianAnggotaKoperasi);
		return pembelianAnggotaKoperasi;
	}

	/** Memasang header struk induk. Dipanggil {@code PembelianAnggotaKoperasi.simpanRinci} saat merakit baris. */
	public void setPembelianAnggotaKoperasi(PembelianAnggotaKoperasi pembelianAnggotaKoperasi) {
		this.pembelianAnggotaKoperasi = pembelianAnggotaKoperasi;
	}

	/**
	 * Potongan harga baris dalam <b>rupiah</b> (bukan persen), dengan bawaan {@code 0.0}.
	 *
	 * <p>Nilainya dikurangkan langsung dari {@code hargaSatuan × qty} di {@link #getTotal()}.
	 * Karena diskon disimpan sebagai nominal jadi — bukan persentase — perubahan pada
	 * {@link #getAturanDiskon()} di masa depan tidak menulis ulang potongan baris lama; kolom
	 * ini adalah snapshot hasil perhitungan, sedangkan aturan asalnya hanya dicatat sebagai
	 * rujukan.</p>
	 *
	 * <p><b>Tidak ada penjaga batas atas.</b> Entity ini tidak memeriksa bahwa diskon
	 * &le; {@code hargaSatuan × qty}, sehingga diskon yang melebihi nilai barang akan
	 * menghasilkan {@link #getTotal()} NEGATIF tanpa peringatan apa pun — baris yang justru
	 * mengurangi tagihan struk. Validasi kewajaran diskon sepenuhnya menjadi tanggung jawab
	 * lapisan pemanggil (aturan diskon dan kasir), bukan model ini. Nilai negatif pada kolom
	 * diskon juga tidak dicegah dan akan berperilaku sebagai biaya tambahan.</p>
	 *
	 * @return potongan dalam rupiah; tidak pernah {@code null}
	 */
	public Double getDiskon() {
		return diskon == null ? 0.0 : diskon;
	}

	/** Memasang potongan harga baris dalam rupiah. Tidak divalidasi terhadap nilai barang — lihat {@link #getDiskon()}. */
	public void setDiskon(Double diskon) {
		this.diskon = diskon;
	}

	/**
	 * Nilai bersih baris: {@code (hargaSatuan × qty) − diskon}. <b>Selalu dihitung ulang, dan
	 * nilai yang tersimpan di kolom SELALU dibuang.</b>
	 *
	 * <p>Ini bentuk getter destruktif yang paling tegas di kelas ini: berbeda dari
	 * {@link #getHargaJual()} yang hanya menghitung saat kosong, method ini menghitung
	 * <b>tanpa syarat</b> pada setiap pemanggilan lalu menimpa field {@code total}. Karena itu
	 * {@link #setTotal(Double)} praktis tidak berguna sebagai cara memaksakan nilai — apa pun
	 * yang dipasang akan hilang pada pembacaan pertama. Kolom {@code total} di database
	 * efektifnya adalah <i>cache</i> hasil perhitungan, bukan data yang berdiri sendiri.</p>
	 *
	 * <p>Desain ini punya sisi baik dan sisi buruk yang keduanya nyata. Sisi baiknya, total
	 * tidak akan pernah "basi" terhadap qty/harga/diskon di baris yang sama — mustahil ada
	 * baris yang totalnya tidak cocok dengan komponennya. Sisi buruknya, total juga
	 * <b>ikut berubah surut</b>: karena {@link #getHargaSatuan()} dapat memungut harga katalog
	 * hari ini untuk baris lama yang kolom harganya kosong, mencetak ulang struk lama dapat
	 * menghasilkan total yang berbeda dari yang dulu benar-benar dibayar pembeli. Untuk
	 * kebutuhan rekonsiliasi yang harus setia pada nilai historis, angka yang bisa dipercaya
	 * adalah yang tercatat di header pembayaran, bukan hasil getter ini.</p>
	 *
	 * <p>Perhatikan juga {@link #getCashback()} <b>tidak</b> ikut diperhitungkan di sini —
	 * cashback bukan pengurang tagihan melainkan imbalan terpisah.</p>
	 *
	 * @return nilai bersih baris; tidak pernah {@code null}, dapat negatif bila diskon berlebih
	 */
	public Double getTotal() {
		total = (getHargaSatuan() * getQty()) - getDiskon();
		return total;
	}

	/**
	 * Memasang nilai total baris. <b>Nyaris tanpa efek</b>: {@link #getTotal()} menghitung ulang
	 * tanpa syarat dan menimpanya. Setter ini praktis hanya dipakai Hibernate saat memuat baris.
	 */
	public void setTotal(Double total) {
		this.total = total;
	}

	/**
	 * Kode pembayaran online (topup) yang membiayai baris ini — <b>sumber identitas dengan
	 * prioritas tertinggi</b> di seluruh kelas ini.
	 *
	 * <p>Bila terisi, taut ini mengambil alih {@link #getSiswa()}, {@link #getMahasiswa()},
	 * {@link #getCalonSiswa()}, {@link #getBiodataCalonMahasiswa()}, {@link #getTbmuser()},
	 * dan bahkan {@link #getToko()} — masing-masing menimpa nilai kolom baris tanpa syarat.
	 * Alasannya, pada transaksi topup pembeli sudah terverifikasi di sisi pembayaran, sehingga
	 * data pembayaranlah yang otoritatif, bukan apa pun yang tersimpan di baris.</p>
	 *
	 * <p>Ada satu kehalusan yang berulang di seluruh berkas: getter-getter yang menimpa itu
	 * membaca <b>field</b> {@code kodePembayaranOnline} secara langsung dan tidak pernah
	 * memanggil getter ini. Karena relasi ini dimuat lewat {@code @Fetch(FetchMode.SELECT)}
	 * pada {@code @ManyToOne} bawaan (EAGER), dalam praktik ia sudah terisi begitu baris dimuat,
	 * sehingga perbedaan itu jarang terlihat. Namun pada objek yang dirakit manual di memori,
	 * urutan pemanggilan dapat mengubah hasil.</p>
	 *
	 * <p>Getter murni tanpa efek samping — ia tidak menormalkan lewat {@code check(...)} seperti
	 * relasi lain, sehingga nilai yang dikembalikan adalah isi field apa adanya.</p>
	 *
	 * @return kode pembayaran online, atau {@code null} untuk transaksi tunai/nontopup
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kode_pembayaran_online", nullable = true)
	public KodePembayaranOnline getKodePembayaranOnline() {
		return kodePembayaranOnline;
	}

	/**
	 * Memasang kode pembayaran online. Sadari bahwa memasang nilai di sini akan
	 * <b>menimpa enam kolom lain</b> pada pembacaan berikutnya — lihat
	 * {@link #getKodePembayaranOnline()}.
	 */
	public void setKodePembayaranOnline(KodePembayaranOnline kodePembayaranOnline) {
		this.kodePembayaranOnline = kodePembayaranOnline;
	}

	/**
	 * Merakit label KATEGORI pembeli ("Siswa", "Dosen", "Masyarakat Umum", ...) — pendamping
	 * {@link #getMember()} yang merakit label IDENTITAS dari kolom-kolom yang sama.
	 *
	 * <p>Getter destruktif (menulis balik ke field {@code jenisMember}). Urutan penelusurannya
	 * <b>berbeda</b> dari {@link #getMember()} dan perbedaan itu disengaja: di sini identitas
	 * akademik didahulukan, sedangkan di sana keanggotaan koperasi yang didahulukan.</p>
	 * <ol>
	 *   <li>{@link #getMahasiswa()} &rarr; "Mahasiswa"</li>
	 *   <li>{@link #getSiswa()} &rarr; "Siswa"</li>
	 *   <li>{@link #getCalonSiswa()} &rarr; "Calon Siswa"</li>
	 *   <li>{@link #getBiodataCalonMahasiswa()} &rarr; "Calon Mahasiswa"</li>
	 *   <li>{@link #getTbmuser()} yang punya data dosen &rarr; "Dosen"</li>
	 *   <li>{@link #getTbmuser()} yang punya data guru &rarr; "Guru"</li>
	 *   <li>{@link #getTbmuser()} yang punya data pegawai &rarr; "Karyawan"</li>
	 *   <li>{@link #getTbmuser()} dengan hak akses &rarr; nama peran apa adanya — satu-satunya
	 *       cabang yang menghasilkan label DINAMIS, sehingga daftar nilai kolom ini tidak
	 *       terbatas pada himpunan tetap dan tidak boleh diperlakukan sebagai enum</li>
	 *   <li>anggota koperasi di header &rarr; nama jenis anggota koperasi (juga dinamis)</li>
	 *   <li>selain itu &rarr; "Masyarakat Umum"</li>
	 * </ol>
	 * <p>Perhatikan cabang 5&ndash;8 memanggil {@link #getTbmuser()} berulang kali, dan getter
	 * itu sendiri destruktif serta memicu empat getter identitas lainnya. Satu pemanggilan
	 * {@code getJenisMember()} karenanya dapat menyentuh dan menulis balik hampir seluruh kolom
	 * identitas baris ini. Karena {@link #getTbmuser()} memaksa {@code null} ketika ada
	 * identitas akademik, cabang 5&ndash;8 tidak akan pernah tercapai untuk pembeli yang juga
	 * siswa/mahasiswa — konsisten dengan urutan di atas, bukan bertentangan dengannya.</p>
	 * <p>Cabang terakhir memastikan method ini <b>tidak pernah</b> mengembalikan {@code null},
	 * berbeda dari {@link #getMember()} yang bisa. Laporan yang mengelompokkan berdasarkan
	 * jenis pembeli karenanya tidak perlu menangani kategori kosong.</p>
	 *
	 * @return label kategori pembeli; tidak pernah {@code null}
	 */
	public String getJenisMember() {
		if (getMahasiswa() != null) {
			jenisMember = "Mahasiswa";
		} else if (getSiswa() != null) {
			jenisMember = "Siswa";
		} else if (getCalonSiswa() != null) {
			jenisMember = "Calon Siswa";
		} else if (getBiodataCalonMahasiswa() != null) {
			jenisMember = "Calon Mahasiswa";
		} else if (getTbmuser() != null && getTbmuser().getDosen() != null) {
			jenisMember = "Dosen";
		} else if (getTbmuser() != null && getTbmuser().getGuru() != null) {
			jenisMember = "Guru";
		} else if (getTbmuser() != null && getTbmuser().getPegawai() != null) {
			jenisMember = "Karyawan";
		} else if (getTbmuser() != null && getTbmuser().hakAkses() != null) {
			jenisMember = getTbmuser().hakAkses().getRoleName();
		} else if (getPembelianAnggotaKoperasi() != null && getPembelianAnggotaKoperasi().getAnggotaKoperasi() != null
				&& getPembelianAnggotaKoperasi().getAnggotaKoperasi().getJenisAnggotaKoperasi() != null) {
			jenisMember = getPembelianAnggotaKoperasi().getAnggotaKoperasi().getJenisAnggotaKoperasi().getNama();
		} else {
			jenisMember = "Masyarakat Umum";
		}
		return jenisMember;
	}

	/** Memasang label kategori pembeli. Selalu ditimpa {@link #getJenisMember()} pada pembacaan berikutnya. */
	public void setJenisMember(String jenisMember) {
		this.jenisMember = jenisMember;
	}

	/**
	 * Merakit label cara pembayaran untuk struk dan laporan, dari tiga kemungkinan.
	 *
	 * <p>Getter destruktif (menulis balik ke field {@code caraBayar}) dengan urutan:</p>
	 * <ol>
	 *   <li>Bila {@code kodePembayaranOnline} terisi &rarr; {@code "Online (Topup)"} — label
	 *       harfiah, bukan nama kanal pembayaran yang sebenarnya. Informasi kanal (bank/dompet
	 *       digital mana) ada di objek {@link KodePembayaranOnline} dan tidak muncul di sini,
	 *       jadi laporan yang perlu memisahkan kanal tidak dapat mengandalkan kolom ini.</li>
	 *   <li>Bila header punya {@link CaraPembayaranKoperasi} &rarr; namanya. Ini satu-satunya
	 *       cabang yang menghasilkan label DINAMIS dari master, sehingga nilai kolom ini bukan
	 *       himpunan tetap.</li>
	 *   <li>Selain itu &rarr; {@code "Tunai"}. Perhatikan ini <b>anggapan</b>, bukan fakta yang
	 *       tercatat: baris tanpa header dan tanpa kode pembayaran online akan dilabeli tunai
	 *       walaupun sebenarnya tidak ada satu pun bukti cara bayarnya. Untuk baris hasil impor
	 *       data lama, label "Tunai" karenanya harus dibaca sebagai "tidak diketahui".</li>
	 * </ol>
	 * <p>Cabang 2 membaca cara pembayaran <b>dari header</b>, bukan dari
	 * {@link #getCaraPembayaranKoperasi()} milik baris ini sendiri. Keduanya biasanya sama
	 * karena getter tersebut juga mengambil alih dari header, tetapi perbedaannya muncul pada
	 * baris tanpa header yang kolom {@code cara_pembayaran_koperasi}-nya terisi: baris seperti
	 * itu tetap dilabeli "Tunai" di sini meski {@link #getCaraPembayaranKoperasi()}
	 * mengembalikan cara bayar lain.</p>
	 *
	 * @return label cara pembayaran; tidak pernah {@code null}
	 */
	public String getCaraBayar() {
		if (kodePembayaranOnline != null) {
			caraBayar = "Online (Topup)";
		} else if (getPembelianAnggotaKoperasi() != null
				&& getPembelianAnggotaKoperasi().getCaraPembayaranKoperasi() != null) {
			caraBayar = getPembelianAnggotaKoperasi().getCaraPembayaranKoperasi().getNama();
		} else {
			caraBayar = "Tunai";
		}
		return caraBayar;
	}

	/** Memasang label cara pembayaran. Selalu ditimpa {@link #getCaraBayar()} pada pembacaan berikutnya. */
	public void setCaraBayar(String caraBayar) {
		this.caraBayar = caraBayar;
	}

	/**
	 * Memasang anggota koperasi pembeli. Nilai ini <b>diabaikan</b> pada pembacaan berikutnya
	 * bila baris punya header — lihat pengambilalihan di {@link #getAnggotaKoperasi()}.
	 */
	public void setAnggotaKoperasi(AnggotaKoperasi anggotaKoperasi) {
		this.anggotaKoperasi = anggotaKoperasi;
	}

	/**
	 * Anggota koperasi pembeli, dengan <b>pengambilalihan mutlak oleh header</b> bila header
	 * ada.
	 *
	 * <p>Getter destruktif berbentuk if/else, bukan berlapis: bila
	 * {@code pembelianAnggotaKoperasi} terisi, kolom {@code anggota_koperasi} milik baris ini
	 * <b>sama sekali tidak dibaca</b> — nilainya langsung diganti dengan anggota koperasi milik
	 * header, termasuk bila header itu sendiri tidak punya anggota ({@code null}). Baru ketika
	 * header tidak ada, kolom sendiri dipakai dan dinormalkan lewat {@code check(...)}.</p>
	 *
	 * <p>Inilah bentuk paling jelas dari pola "begitu induk terpasang, anak berhenti membaca
	 * kolomnya sendiri" di kelas ini. Konsekuensinya: pada baris yang punya header, kolom
	 * {@code anggota_koperasi} di database adalah data mati — ia tidak pernah dibaca lagi dan
	 * akan ditimpa nilai header pada flush berikutnya. Setiap kueri SQL langsung yang menyaring
	 * berdasarkan kolom itu dapat memberi hasil yang berbeda dari yang dilihat aplikasi;
	 * penyaringan yang benar harus menempuh header.</p>
	 *
	 * <p>Perhatikan cabang pertama membaca <b>field</b> {@code pembelianAnggotaKoperasi}
	 * langsung, bukan {@link #getPembelianAnggotaKoperasi()}, sehingga tidak melewati
	 * {@code check(...)}. Bila header masih berupa proxy lazy pada entity yang sudah
	 * ter-detach, {@code pembelianAnggotaKoperasi.getAnggotaKoperasi()} dapat melempar
	 * {@code LazyInitializationException} alih-alih jatuh ke cabang {@code else}.</p>
	 *
	 * @return anggota koperasi pembeli, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "anggota_koperasi", nullable = true)
	public AnggotaKoperasi getAnggotaKoperasi() {
		if (pembelianAnggotaKoperasi != null) {
			anggotaKoperasi = pembelianAnggotaKoperasi.getAnggotaKoperasi();
		} else {
			anggotaKoperasi = check(anggotaKoperasi);
		}
		return anggotaKoperasi;
	}

	// ==========================================================
	// MODIFIKASI UNTUK FITUR ATURAN DISKON & CASHBACK
	// ==========================================================

	/**
	 * Cashback dalam rupiah yang diperoleh pembeli dari baris ini, hasil penerapan
	 * {@link #getAturanDiskon()}.
	 *
	 * <p><b>Bukan pengurang tagihan.</b> Berbeda dari {@link #getDiskon()},
	 * {@link #getTotal()} sama sekali tidak memperhitungkan cashback — pembeli tetap membayar
	 * penuh, dan cashback adalah imbalan terpisah yang dikreditkan ke saldo/dompetnya. Karena
	 * itu menjumlahkan cashback ke dalam nilai penjualan adalah kesalahan yang mudah terjadi:
	 * secara akuntansi ia beban promosi, bukan potongan pendapatan.</p>
	 *
	 * <p>Seperti diskon, disimpan sebagai nominal jadi (snapshot), sehingga perubahan aturan
	 * diskon di kemudian hari tidak menulis ulang cashback baris lama. Getter murni dengan
	 * bawaan {@code 0.0}; tidak ada penjaga yang mencegah nilai negatif.</p>
	 *
	 * @return cashback dalam rupiah; tidak pernah {@code null}
	 */
	@Column(name = "cashback")
	public Double getCashback() {
		return cashback == null ? 0.0 : cashback;
	}

	/** Memasang nominal cashback baris. Tidak memengaruhi {@link #getTotal()} — lihat {@link #getCashback()}. */
	public void setCashback(Double cashback) {
		this.cashback = cashback;
	}

	/**
	 * Aturan diskon yang menghasilkan {@link #getDiskon()} dan {@link #getCashback()} pada
	 * baris ini — dicatat sebagai <b>rujukan/jejak</b>, bukan sebagai sumber perhitungan.
	 *
	 * <p>Nilai potongan dan cashback sudah dibekukan sebagai nominal di kolomnya masing-masing,
	 * jadi mengubah atau menghapus aturan di master tidak mengubah angka baris lama. Taut ini
	 * ada untuk menjawab "kenapa baris ini dapat potongan?" saat audit, dan untuk laporan
	 * efektivitas promosi yang mengelompokkan penjualan per aturan.</p>
	 *
	 * <p>Getter murni-normalisasi ({@code check(...)}) tanpa lapis penimpaan. Bernilai
	 * {@code null} untuk mayoritas baris — transaksi tanpa promosi.</p>
	 *
	 * @return aturan diskon yang dipakai, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "aturan_diskon", nullable = true)
	public AturanDiskon getAturanDiskon() {
		aturanDiskon = check(aturanDiskon);
		return aturanDiskon;
	}

	/** Memasang aturan diskon asal. Murni jejak; tidak memicu perhitungan ulang diskon/cashback. */
	public void setAturanDiskon(AturanDiskon aturanDiskon) {
		this.aturanDiskon = aturanDiskon;
	}

	/**
	 * Penanda barang pada baris ini sudah <b>diserahkan secara fisik</b> kepada pembeli.
	 *
	 * <p>Memisahkan dua peristiwa yang di ritel biasa terjadi bersamaan tetapi di kantin/kafe
	 * tidak: pembayaran (dicatat header) dan penyerahan barang (dicatat di sini). Sebuah
	 * pesanan dapat lunas tetapi belum terlayani karena makanannya masih dimasak; layar dapur
	 * memakai flag ini untuk mengetahui apa yang masih harus dikeluarkan.</p>
	 *
	 * <p>Bawaannya {@code false} untuk {@code null} — <b>kebalikan</b> dari
	 * {@link #getAktif()} yang berbawaan {@code true}. Perbedaan arah ini disengaja dan aman:
	 * pilihan konservatif untuk "sudah diserahkan?" adalah menganggap belum, sehingga baris
	 * lama tidak keliru dianggap sudah dilayani. Sama seperti flag satu arah lainnya, tidak ada
	 * cara membedakan "belum ditentukan" dari "tegas belum" lewat getter ini, dan entity tidak
	 * mencatat kapan maupun oleh siapa penyerahan terjadi — hanya ya/tidak.</p>
	 *
	 * @return {@code true} bila barang sudah diserahkan; {@code false} bila belum atau belum
	 *         ditentukan
	 */
	public Boolean getTerlayani() {
		return terlayani == null ? false : terlayani;
	}

	/** Menandai barang baris ini sudah/belum diserahkan ke pembeli. */
	public void setTerlayani(Boolean terlayani) {
		this.terlayani = terlayani;
	}

	/**
	 * Gap-closure "Produk Ekstra" -- {@code null} (default, SEMUA baris lama) berarti baris biasa,
	 * persis perilaku hari ini. Terisi berarti baris ini adalah Ekstra yang dipilih pembeli, dan
	 * nilainya adalah {@code id} baris {@link Pembelian} LAIN (induknya) di bill yang sama --
	 * SENGAJA field polos (bukan relasi {@code @ManyToOne} self-reference) karena tidak ada
	 * kebutuhan navigasi object graph, hanya filter/grouping SQL di struk &amp; laporan (lihat
	 * {@code PosApi.prosesDetailTransaksi}, {@code ORDER BY COALESCE(induk_id,id),id}).
	 */
	@Column(name = "induk_id")
	public Long getIndukId() {
		return indukId;
	}

	/**
	 * Memasang id baris induk untuk baris "Produk Ekstra". Nilainya harus berupa id
	 * {@link Pembelian} lain di struk yang SAMA — tidak ada foreign key yang menegakkan
	 * aturan itu (lihat {@link #getIndukId()}).
	 */
	public void setIndukId(Long indukId) {
		this.indukId = indukId;
	}

	/**
	 * Cara pembayaran koperasi untuk baris ini, dengan <b>pengambilalihan mutlak oleh header</b>
	 * — pola if/else yang sama persis dengan {@link #getAnggotaKoperasi()}.
	 *
	 * <p>Bila {@code pembelianAnggotaKoperasi} terisi, kolom
	 * {@code cara_pembayaran_koperasi} milik baris ini tidak dibaca sama sekali dan diganti
	 * dengan cara pembayaran header, termasuk bila header bernilai {@code null}. Ini benar
	 * secara domain: cara membayar adalah sifat satu struk secara keseluruhan, bukan sifat
	 * tiap barang di dalamnya — mustahil satu barang dibayar tunai sementara barang lain di
	 * struk yang sama dibayar potong gaji. Kolom di tingkat baris hanya bermakna untuk baris
	 * lepas tanpa header.</p>
	 *
	 * <p>Berlaku pula peringatan yang sama seperti pada {@link #getAnggotaKoperasi()}: cabang
	 * pertama membaca field header secara langsung tanpa {@code check(...)}, sehingga proxy
	 * lazy yang ter-detach dapat melempar {@code LazyInitializationException}; dan pada baris
	 * yang punya header, kolom baris menjadi data mati yang menyesatkan bagi kueri SQL
	 * langsung.</p>
	 *
	 * <p>Jangan tertukar dengan {@link #getCaraBayar()} yang menghasilkan <i>label teks</i> dan
	 * menempuh jalur berbeda (mendahulukan pembayaran online, lalu jatuh ke "Tunai").</p>
	 *
	 * @return cara pembayaran koperasi, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "cara_pembayaran_koperasi", nullable = true)
	public CaraPembayaranKoperasi getCaraPembayaranKoperasi() {
		if (pembelianAnggotaKoperasi != null) {
			caraPembayaranKoperasi = pembelianAnggotaKoperasi.getCaraPembayaranKoperasi();
		} else {
			caraPembayaranKoperasi = check(caraPembayaranKoperasi);
		}

		return caraPembayaranKoperasi;
	}

	/**
	 * Memasang cara pembayaran koperasi di tingkat baris. Hanya bermakna untuk baris tanpa
	 * header — pada baris berheader nilainya diabaikan dan ditimpa.
	 */
	public void setCaraPembayaranKoperasi(CaraPembayaranKoperasi caraPembayaranKoperasi) {
		this.caraPembayaranKoperasi = caraPembayaranKoperasi;
	}

	/**
	 * Penanda bahwa baris penjualan ini SUDAH ikut diposting sebagai HPP (beban pokok) —
	 * <b>kunci anti-posting-ganda</b> untuk posting HPP per barang.
	 *
	 * <p>Bila terisi, baris ini dikecualikan dari pratinjau maupun eksekusi Posting HPP
	 * berikutnya, sehingga barang yang sudah diposting tidak dapat terhitung dua kali walaupun
	 * periode yang sama diposting ulang. Tanpa penanda ini, memposting sebagian barang saja
	 * mustahil dilakukan dengan aman. Selain sebagai gerbang, taut ini juga berfungsi sebagai
	 * jejak balik dari baris penjualan ke entri jurnal yang menyerapnya.</p>
	 *
	 * <p>Penulisan penanda dilakukan <b>dalam transaksi database yang sama</b> dengan
	 * pembuatan entri jurnalnya (lihat {@code postingPerBarang}), sehingga tidak mungkin ada
	 * keadaan antara berupa jurnal yang sudah terbentuk tetapi barisnya belum tertandai.
	 * Kolomnya ({@code posting_hpp}) dibuat otomatis oleh Hibernate saat boot sesuai kebijakan
	 * repositori ini yang menyerahkan ALTER TABLE kepada hbm2ddl.</p>
	 *
	 * <p>Perhatikan penanda ini spesifik untuk HPP dan berdiri sendiri dari penanda posting
	 * PENDAPATAN — sebuah baris dapat sudah diposting sebagai pendapatan tetapi belum sebagai
	 * beban pokok, atau sebaliknya. Keduanya jangan diperlakukan sebagai satu status.</p>
	 *
	 * @return riwayat posting HPP yang menyerap baris ini, atau {@code null} bila belum pernah
	 *         diposting sebagai HPP
	 */
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "posting_hpp", nullable = true)
	public ais.database.model.akunting.PostingHistory getPostingHpp() {
		return postingHpp;
	}

	/**
	 * Menandai baris ini sudah ikut diposting sebagai HPP. Harus dipanggil dalam transaksi yang
	 * sama dengan penyimpanan entri jurnalnya agar gerbang anti-posting-ganda tetap utuh.
	 */
	public void setPostingHpp(ais.database.model.akunting.PostingHistory postingHpp) {
		this.postingHpp = postingHpp;
	}
}
