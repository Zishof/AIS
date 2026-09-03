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
import ais.database.model.koperasi.DraftPembelianAnggotaKoperasi;
import ais.database.model.koperasi.KodePembayaranOnline;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Siswa;

/**
 * <h2>Baris pesanan PENJUALAN yang masih DITAHAN (belum dibayar) — kembaran tahap draft dari
 * {@link Pembelian}.</h2>
 *
 * <p><b>Peringatan nama yang sama seperti {@link Pembelian}:</b> meski bernama "Pembelian" dan
 * berada di paket {@code inventory}, ini adalah baris <b>penjualan</b> — barang yang akan KELUAR
 * dari toko ke pembeli. Namanya diambil dari sudut pandang pembeli. Ia tidak ada hubungannya
 * dengan pembelian toko ke supplier ({@code PengadaanFaktur}/{@code PengadaanProduk}) maupun
 * dengan {@link ReturPembelian}.</p>
 *
 * <h3>Kedudukan: ANAK dari draft, bukan versi lain dari draft</h3>
 * <p>Ini kekeliruan yang paling mudah terjadi karena kemiripan nama, jadi perlu ditegaskan:
 * <b>{@code DraftPembelian} BUKAN padanan sejajar dari
 * {@link DraftPembelianAnggotaKoperasi}</b>. Keduanya berada pada TINGKAT yang berbeda dalam
 * dokumen yang sama — {@code DraftPembelianAnggotaKoperasi} adalah <i>header</i> (satu pesanan
 * ditahan), sedangkan kelas ini adalah <i>baris</i> di dalam header itu (satu SKU). Hubungan
 * keduanya induk&ndash;anak, bukan dua penamaan untuk gagasan yang sama.</p>
 * <pre>
 *   DraftPembelianAnggotaKoperasi (header draft) --1:N--&gt; DraftPembelian (baris draft) &lt;-- ANDA DI SINI
 *              |  lunas -&gt; PembelianAnggotaKoperasi         |  lunas -&gt; Pembelian
 *              v                                            v
 *   PembelianAnggotaKoperasi      (header final) --1:N--&gt; Pembelian (baris final)
 * </pre>
 * <p>Pemetaan tingkat demi tingkat itu berlaku rapi, termasuk pada penamaan {@code lunas}:
 * {@link #getLunas()} di sini menunjuk baris {@link Pembelian} hasil finalisasi, sedangkan
 * {@code DraftPembelianAnggotaKoperasi.getLunas()} menunjuk <i>header</i>
 * {@code PembelianAnggotaKoperasi}. Jangan tertukar — keduanya bertipe berbeda.</p>
 *
 * <h3>Mekanisme finalisasi: SALIN-BEKU, bukan delegasi hidup</h3>
 * <p>Perbedaan penting dari pola yang dipakai di tingkat header. Di tingkat header,
 * {@code PembelianAnggotaKoperasi} <i>mendelegasikan</i> sejumlah getter-nya kepada draft
 * asalnya — begitu draft terpasang, header berhenti membaca kolomnya sendiri dan menyalin nilai
 * dari draft <b>pada setiap pembacaan</b>. Di tingkat baris, <b>hal itu TIDAK terjadi</b>:
 * {@link Pembelian} sama sekali tidak punya taut balik ke {@code DraftPembelian} dan tidak
 * pernah membaca apa pun darinya.</p>
 * <p>Alih-alih, finalisasi berupa <b>penyalinan satu kali</b> di
 * {@code PembelianAnggotaKoperasi.simpanRinci}: baris {@link Pembelian} BARU dibuat, kolomnya
 * diisi eksplisit satu per satu dari draft ({@code kode}, {@code nama}, {@code qty},
 * {@code diskon}, {@code cashback}, {@code hargaSatuan}, {@code produk}, {@code aturanDiskon}),
 * lalu draft ditandai lewat {@link #setLunas(Pembelian)}. Sesudah itu keduanya <b>hidup
 * terpisah</b>: mengubah draft tidak lagi memengaruhi baris final, dan sebaliknya. Arah tautnya
 * pun satu arah — dari draft ke final, bukan sebaliknya.</p>
 * <p>Konsekuensi yang harus disadari: karena penyalinan bersifat eksplisit kolom demi kolom,
 * setiap kolom yang kelak ditambahkan ke {@link Pembelian} <b>tidak otomatis</b> ikut tersalin.
 * Itu sudah terjadi pada triplet satuan jual ({@code satuanJual}/{@code qtyInput}/
 * {@code faktorKeDasar}): kelas ini tidak memilikinya sama sekali, dan alur finalisasi tidak
 * mengisinya, sehingga transaksi yang menempuh jalur draft kehilangan snapshot satuan jual yang
 * dimiliki transaksi jalur langsung. Dampaknya terbatas pada tampilan/audit — seluruh rumus
 * stok, HPP, dan tagihan memakai {@link #getQty()} dalam satuan dasar yang tersalin dengan
 * benar — tetapi struk hasil jalur draft tidak dapat menampilkan kembali "2 Karung" seperti
 * struk jalur langsung.</p>
 *
 * <h3>Beda lain dari {@link Pembelian}</h3>
 * <ul>
 *   <li><b>Tidak ada {@code postingHpp}.</b> Wajar: draft belum menjadi penjualan, jadi belum
 *       ada beban pokok yang boleh diposting. Baris draft tidak pernah menyentuh buku besar.</li>
 *   <li><b>Tidak ada relasi {@code caraPembayaranKoperasi} di tingkat baris</b> — cara bayar
 *       hanya ada di header draft. {@link #getCaraBayar()} tetap merakit labelnya dari sana.</li>
 *   <li><b>{@link #getWaktu()} tidak mengambil alih dari header.</b> Berbeda dari
 *       {@code Pembelian.getWaktu()} yang menimpa dirinya dengan tanggal bayar header. Itu tepat:
 *       draft justru BELUM dibayar, sehingga belum ada tanggal bayar yang dapat dipakai.</li>
 *   <li><b>Tidak masuk rumus stok.</b> {@code StokKantinUtil} menjumlahkan
 *       {@code koperasi.pembelian}, bukan {@code koperasi.draft_pembelian} — barang yang masih
 *       tertahan sebagai draft belum mengurangi persediaan. Stok baru berkurang saat draft
 *       difinalisasi menjadi {@link Pembelian}.</li>
 * </ul>
 *
 * <h3>Sifat lain yang diwarisi utuh dari {@link Pembelian}</h3>
 * <p>Selebihnya kelas ini adalah salinan struktural {@link Pembelian}, termasuk seluruh sifat
 * yang perlu diwaspadai: <b>mayoritas getter destruktif</b> (menulis balik ke field saat dibaca,
 * sehingga entity ter-attach dapat menjadi dirty hanya karena ditampilkan), <b>field audit
 * shadow</b> {@code oleh}/{@code olehId}/{@code tanggal_dirubah} sebagai pendamping Envers yang
 * merupakan keharusan teknis, dan <b>identitas pembeli yang tersebar di enam kolom</b> yang
 * ditelusuri {@link #getMember()} serta {@link #getJenisMember()}. Penjelasan lengkap tiap sifat
 * ada di JavaDoc masing-masing method, dan padanannya di {@link Pembelian}.</p>
 *
 * @see DraftPembelianAnggotaKoperasi header draft yang memuat baris ini
 * @see Pembelian baris final hasil finalisasi baris ini
 * @see PembelianAnggotaKoperasi#simpanRinci alur penyalinan draft menjadi baris final
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "draft_pembelian")
public class DraftPembelian extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya kebetulan identik dengan
	 * {@code Pembelian.serialVersionUID} karena kedua kelas lahir dari template hbm2java yang
	 * sama; itu <b>tidak</b> berarti keduanya kompatibel biner — keduanya kelas berbeda dan
	 * tidak pernah saling di-deserialisasi.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama {@code koperasi.draft_pembelian.id}, auto-increment. Lihat {@link #getId()}. */
	private Long id;
	/** Nama petugas terakhir yang mengubah baris (audit shadow). Lihat {@link #getOleh()}. */
	private String oleh;
	/** Id petugas terakhir yang mengubah baris (audit shadow). Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Id petugas terakhir yang menyentuh baris draft ini — pasangan {@link #getOleh()} yang
	 * menyimpan id teknis, bukan nama tampilan. Getter murni; dapat {@code null} pada baris lama.
	 *
	 * @return id petugas, atau {@code null}
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id petugas, <b>menolak diam-diam</b> nilai {@code null} maupun blank.
	 *
	 * <p>Penjaga di baris pertama membuat method langsung {@code return} tanpa mengubah apa pun
	 * bila nilai kosong, sehingga jejak audit lama <b>dipertahankan</b> alih-alih ditimpa. Ini
	 * disengaja: jejak "siapa yang terakhir menyentuh" tidak boleh hilang hanya karena satu alur
	 * pemanggil kebetulan meneruskan string kosong. Harganya, setter ini <b>tidak dapat dipakai
	 * untuk mengosongkan</b> kolom — pengosongan hanya mungkin lewat UPDATE langsung.</p>
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
	 * {@link #setOlehId(String)} — lihat alasan lengkapnya di sana.
	 *
	 * @param oleh nama petugas; {@code null}/kosong diabaikan tanpa error
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama petugas terakhir yang mengubah baris draft ini (field audit shadow — lihat JavaDoc
	 * kelas). Getter murni; dapat {@code null} pada data lama.
	 *
	 * @return nama petugas, atau {@code null}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate} yang menyegarkan stempel waktu audit tepat sebelum baris
	 * draft ini di-UPDATE.
	 *
	 * <p>Pada entity draft, callback ini lebih sering terpicu daripada padanannya di
	 * {@link Pembelian}: sebuah pesanan yang ditahan wajar mengalami banyak perubahan
	 * (tambah/kurang qty, ganti produk) sebelum dibayar, dan setiap perubahan itu memperbarui
	 * stempel di sini. Karena finalisasi juga memanggil {@code session.update(draftPembelian)}
	 * untuk memasang {@link #setLunas(Pembelian)}, stempel terakhir sebuah baris draft yang sudah
	 * lunas praktis menandai <b>saat pelunasan</b>, bukan saat terakhir isinya diubah.</p>
	 *
	 * <p>Pekerjaannya didelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} agar aturan
	 * penulisan stempel tinggal di satu tempat untuk seluruh entity. Hanya berjalan pada UPDATE,
	 * bukan INSERT — nilai awal berasal dari inisialisasi field. Method ini {@code protected}
	 * dan milik runtime JPA; memanggilnya manual justru merusak jejak audit.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir (audit shadow). Diinisialisasi ke waktu pembuatan objek
	 * lewat {@code WaktuUtil.getDate()} — bukan {@code new Date()} — agar seluruh aplikasi
	 * memakai satu sumber waktu yang dapat digeser saat pengujian. Diperbarui setiap UPDATE oleh
	 * {@link #onUpdate()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir. Umumnya tidak dipanggil kode aplikasi karena
	 * {@link #onUpdate()} sudah mengurusnya; setter ini terutama untuk Hibernate dan alur impor
	 * data historis.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir baris draft ini. Nama method mempertahankan gaya
	 * {@code snake_case} peninggalan hbm2java karena nama properti ini sudah dipakai sebagai
	 * string di binding ZK, {@code Common.insertProperty}, dan kueri Criteria — menormalkannya
	 * akan memutus rujukan berbasis nama tersebut.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas {@code "<id>-<keterangan>"} untuk log dan komponen ZK.
	 *
	 * <p>Membaca field {@code keterangan} <b>langsung</b>, bukan lewat {@link #getKeterangan()}
	 * yang destruktif. Dengan begitu method ini aman dipanggil dari logger atau debugger tanpa
	 * memicu pembangkitan kalimat maupun pemotongan 253 karakter. Imbalannya, hasilnya dapat
	 * berupa {@code "123-null"} untuk baris yang keterangannya belum pernah dibangkitkan —
	 * perilaku yang diterima untuk keperluan diagnostik.</p>
	 *
	 * @return gabungan id dan keterangan mentah
	 */
	public String toString() {
		return id + "-" + keterangan;
	}

	/** Kode/nomor baris; bila kosong dibangkitkan on the fly. Lihat {@link #getKode()}. */
	private String kode;
	/** Snapshot "kode + nama produk" untuk tampilan pesanan. Lihat {@link #getNama()}. */
	private String nama;

	/** Snapshot nama {@link Toko} (kios) tempat pesanan dibuat. Lihat {@link #getKios()}. */
	private String kios;
	/** Snapshot identitas pemesan dalam satu string tampilan. Lihat {@link #getMember()}. */
	private String member;
	/** SKU yang dipesan pada baris ini. Lihat {@link #getProduk()}. */
	private Produk produk;
	/** Pemesan bila ia siswa. Salah satu dari enam kolom identitas; lihat JavaDoc kelas. */
	private Siswa siswa;
	/** Pemesan bila ia calon siswa. Lihat {@link #getCalonSiswa()}. */
	private CalonSiswa calonSiswa;
	/** Pemesan bila ia mahasiswa. Lihat {@link #getMahasiswa()}. */
	private Mahasiswa mahasiswa;
	/** Pemesan bila ia calon mahasiswa (pendaftar PMB). Lihat {@link #getBiodataCalonMahasiswa()}. */
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	/** Header draft induk baris ini. Lihat {@link #getDraftPembelianAnggotaKoperasi()}. */
	private DraftPembelianAnggotaKoperasi draftPembelianAnggotaKoperasi;
	/** Pemesan bila ia anggota koperasi. Lihat {@link #getAnggotaKoperasi()}. */
	private AnggotaKoperasi anggotaKoperasi;
	/** Snapshot label jenis pemesan ("Siswa", "Dosen", ...). Lihat {@link #getJenisMember()}. */
	private String jenisMember;
	/** Snapshot label cara bayar rencana. Lihat {@link #getCaraBayar()}. */
	private String caraBayar;
	/** Pemesan bila ia pengguna internal (guru/dosen/pegawai). Lihat {@link #getTbmuser()}. */
	private Tbmuser tbmuser;
	/** Kalimat deskripsi baris, dibangkitkan otomatis bila kosong. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Harga jual SATU unit (bukan total) — kolom inilah yang tersalin ke baris final. Lihat {@link #getHargaSatuan()}. */
	private Double hargaSatuan;
	/**
	 * Nilai baris. Meski namanya seolah harga per unit, isinya {@code qty × harga produk} saat
	 * dibangkitkan otomatis — lihat {@link #getHargaJual()}. <b>TIDAK tersalin</b> saat
	 * finalisasi; baris final membangkitkannya sendiri.
	 */
	private Double hargaJual;
	/** Potongan harga baris dalam rupiah (bukan persen). Lihat {@link #getDiskon()}. */
	private Double diskon;
	/** Nilai bersih baris; SELALU dihitung ulang oleh getter. Lihat {@link #getTotal()}. */
	private Double total;
	/**
	 * Jumlah unit dipesan dalam <b>satuan dasar</b> produk. Inilah satu-satunya kolom kuantitas
	 * di kelas ini — tidak ada triplet satuan jual seperti pada {@link Pembelian}, sehingga
	 * angka ketikan asli kasir tidak terekam pada jalur draft (lihat JavaDoc kelas).
	 */
	private Double qty;
	/** Waktu pesanan dibuat. Berbeda dari {@link Pembelian}, TIDAK diambil alih header. Lihat {@link #getWaktu()}. */
	private Date waktu;
	/** Flag aktif satu arah (default {@code true} bila {@code null}). Lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Outlet tempat pesanan dibuat — pembatas tenant utama. Lihat {@link #getToko()}. */
	private Toko toko;
	/** Jejak berkas impor bila baris lahir dari unggahan massal. Lihat {@link #getUploadLog()}. */
	private UploadLogInfo uploadLog;
	/** Kode pembayaran online bila pesanan dikaitkan topup; menimpa kolom identitas lain. Lihat {@link #getKodePembayaranOnline()}. */
	private KodePembayaranOnline kodePembayaranOnline;

	/** Cashback rupiah dari {@link #getAturanDiskon()}. Lihat {@link #getCashback()}. */
	private Double cashback;
	/** Aturan diskon yang berlaku pada baris ini. Lihat {@link #getAturanDiskon()}. */
	private AturanDiskon aturanDiskon;
	/** Penanda barang sudah diserahkan ke pemesan. Lihat {@link #getTerlayani()}. */
	private Boolean terlayani;
	/** Id baris {@code DraftPembelian} induk untuk "Produk Ekstra". Lihat {@link #getIndukId()}. */
	private Long indukId;

	/**
	 * Baris {@link Pembelian} hasil finalisasi baris draft ini; {@code null} = belum dibayar.
	 * Sekaligus gerbang anti-finalisasi-ganda. Lihat {@link #getLunas()}.
	 */
	private Pembelian lunas;

	/**
	 * Konstruktor kosong wajib JPA/Hibernate. Menghasilkan baris draft tanpa produk, toko, atau
	 * header; kolom wajib harus diisi pemanggil sebelum {@code session.save()}, khususnya
	 * {@link #setToko(Toko)} yang dipetakan {@code nullable = false}.
	 */
	public DraftPembelian() {
	}

	/**
	 * Konstruktor pintas yang hanya memasang kunci utama, untuk membentuk referensi ringan ke
	 * baris draft yang sudah ada tanpa memuatnya dari database.
	 *
	 * <p><b>Objek hasil konstruktor ini bukan entity yang termuat.</b> Field lain {@code null},
	 * sehingga getter destruktif seperti {@link #getTotal()} akan mengembalikan nilai bawaan
	 * yang menyesatkan. Jangan pernah menyimpannya kembali ke database — itu akan menimpa baris
	 * asli dengan kolom kosong, termasuk menghapus taut {@link #getLunas()} sehingga sebuah
	 * draft yang sudah lunas dapat difinalisasi ulang.</p>
	 *
	 * @param id kunci utama baris draft yang dirujuk
	 */
	public DraftPembelian(Long id) {
		this.id = id;
	}

	/**
	 * Kunci utama {@code koperasi.draft_pembelian.id}, dibangkitkan database dengan strategi
	 * {@code IDENTITY}.
	 *
	 * <p>Nilai id inilah yang menjadi sasaran {@link #getIndukId()} pada baris "Produk Ekstra",
	 * dan yang dipakai sebagai kunci peta {@code petaDraftKePembelian} saat finalisasi memetakan
	 * id draft ke id baris final. {@code insertable = false} mengeluarkan kolom ini dari INSERT
	 * agar nilai sisa di objek tidak bertabrakan dengan sekuens database.</p>
	 *
	 * @return id baris draft, atau {@code null} bila belum tersimpan
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
	 * Kode/nomor baris, dengan <b>pembangkitan darurat bila kolomnya kosong</b> — perilaku
	 * identik {@code Pembelian.getKode()}.
	 *
	 * <p>Bila {@code kode} masih {@code null}, dikembalikan string berpola
	 * {@code "INV-" + epochMillis + counter}. Hasilnya <b>tidak stabil</b>: karena tidak pernah
	 * ditulis balik ke field, setiap pemanggilan menghasilkan kode berbeda. Itu berbahaya secara
	 * khusus di kelas ini, sebab finalisasi menyalin kode draft ke baris final lewat
	 * {@code pembelian.setKode(draftPembelian.getKode())} — bila kolom draft kosong, kode yang
	 * "dibekukan" ke baris final hanyalah hasil pembangkitan sesaat yang tidak pernah cocok
	 * dengan apa pun yang tersimpan di baris draft. Alur pembuatan draft karenanya harus
	 * memasang kode eksplisit lewat {@link #setKode(String)}.</p>
	 *
	 * <p>{@code Common.increments} adalah counter statis proses tunggal, bukan sekuens database,
	 * sehingga keunikannya "cukup baik untuk label" dan bukan jaminan tingkat basis data — tidak
	 * ada constraint {@code unique} pada kolom ini. Bila kode terisi, nilainya dikembalikan sudah
	 * ter-{@code trim()}.</p>
	 *
	 * @return kode tersimpan yang sudah dipangkas, atau kode {@code "INV-..."} sekali pakai
	 */
	public String getKode() {
		return kode == null ? "INV-" + (ais.ui.util.WaktuUtil.getDate().getTime()) + (++Common.increments)
				: kode.trim();
	}

	/** Memasang kode baris. Satu-satunya cara membuat {@link #getKode()} stabil dan tersalin utuh saat finalisasi. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Snapshot nama produk berbentuk {@code "<kode produk> <nama produk>"}.
	 *
	 * <p><b>Getter destruktif.</b> Setiap pemanggilan menimpa field {@code nama} dengan nilai
	 * segar dari {@link Produk} yang tertaut, sehingga kolom ini bukan snapshot beku melainkan
	 * cermin yang ikut berubah bila nama produk di katalog diubah. Nilai lama hanya bertahan
	 * pada baris yang tautan produknya sudah putus.</p>
	 *
	 * <p>Di kelas ini efeknya merambat lebih jauh daripada di {@link Pembelian}: finalisasi
	 * menyalin hasil {@code draftPembelian.getNama()} ke baris final, jadi nama yang membeku di
	 * baris final adalah nama produk <b>pada saat pelunasan</b>, bukan pada saat pesanan dibuat.
	 * Untuk pesanan yang ditahan lama dan produknya sempat diganti nama, keduanya bisa
	 * berbeda.</p>
	 *
	 * <p>Dipetakan {@code columnDefinition = "text"} sehingga tanpa batas panjang — berbeda dari
	 * {@link #getKeterangan()} yang dipotong di 254 karakter.</p>
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

	/** Memasang snapshot nama produk. Akan ditimpa {@link #getNama()} selama produk masih tertaut. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Kalimat deskripsi baris, <b>dibangkitkan otomatis bila kosong</b> dan <b>dipotong keras di
	 * 254 karakter</b>. Perilakunya identik {@code Pembelian.getKeterangan()}.
	 *
	 * <p>Getter destruktif tiga tahap, semuanya menulis balik ke field:</p>
	 * <ol>
	 *   <li><b>Normalisasi null</b> menjadi string kosong, sehingga tahap berikutnya tidak perlu
	 *       memeriksa {@code null}. Efek sampingnya, membaca properti ini mengubah baris yang
	 *       kolomnya {@code NULL} menjadi string kosong saat flush.</li>
	 *   <li><b>Pembangkitan kalimat</b> {@code "<nama produk> sebanyak <qty> seharga
	 *       <hargaJual>"} bila masih kosong dan produk tertaut. Angka diformat lewat
	 *       {@code Common.numberFormat} yang berupa {@code ThreadLocal} — sengaja, karena
	 *       {@code NumberFormat} tidak thread-safe sedangkan getter ini dapat dipanggil dari
	 *       thread permintaan web mana pun. Perhatikan angka setelah kata "seharga" adalah
	 *       {@link #getHargaJual()} yang pada baris hasil pembangkitan otomatis berisi
	 *       <i>nilai baris</i>, bukan harga per unit.</li>
	 *   <li><b>Pemotongan</b> ke 253 karakter bila melebihi 254, dan hasil potongan
	 *       <b>ditulis balik permanen</b>. Keterangan panjang yang diketik petugas kehilangan
	 *       ekornya begitu properti dibaca sekali pada entity ter-attach. Angka 254/253
	 *       mencerminkan kolom {@code varchar(255)} dengan margin satu karakter.</li>
	 * </ol>
	 * <p>Berbeda dari {@link #getNama()} dan {@link #getKode()}, keterangan <b>tidak</b> disalin
	 * ke baris final saat finalisasi — baris {@link Pembelian} hasil finalisasi membangkitkan
	 * keterangannya sendiri dari produk dan qty-nya.</p>
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
	 * {@link #getKeterangan()} dipanggil.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * <b>PERINGATAN: meski namanya "harga jual", isinya adalah NILAI BARIS (qty × harga), bukan
	 * harga per unit</b> pada setiap baris yang nilainya dibangkitkan di sini. Harga per unit
	 * yang sesungguhnya ada di {@link #getHargaSatuan()}.
	 *
	 * <p>Getter destruktif dengan pembangkitan malas: bila {@code hargaJual} masih {@code null}
	 * <b>atau bernilai tepat {@code 0.0}</b>, dihitung {@code qty × Produk.hargaJual} lalu
	 * dituliskan ke field. Karena syaratnya mencakup {@code 0.0}, baris yang <b>sengaja</b>
	 * bernilai nol (barang gratis, hadiah, atau produk ekstra yang harganya sudah ikut di baris
	 * induk) akan terus-menerus dihitung ulang dan berubah sendiri menjadi angka positif selama
	 * produknya punya harga di katalog. Tidak ada penanda "nol yang disengaja" di entity ini.</p>
	 *
	 * <p>Kabar baiknya untuk integritas finansial: kolom ini <b>TIDAK ikut disalin</b> saat
	 * finalisasi. {@code PembelianAnggotaKoperasi.simpanRinci} hanya menyalin
	 * {@link #getHargaSatuan()}, {@link #getQty()}, {@link #getDiskon()}, dan
	 * {@link #getCashback()}, sehingga ketidakstabilan nilai di sini tidak merembes ke baris
	 * penjualan definitif — baris final membangkitkan nilainya sendiri dari harga satuan yang
	 * sudah beku.</p>
	 *
	 * <p>Perhitungannya defensif: {@code null} pada qty maupun harga produk diperlakukan
	 * {@code 0.0} sehingga tidak pernah melempar {@code NullPointerException}. Pemakaian
	 * {@code new Double(...)} adalah gaya lama pra-autoboxing yang setara
	 * {@code Double.valueOf(...)}.</p>
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
	 * {@link #getHargaJual()} akan menghitung ulang dan menimpanya.
	 */
	public void setHargaJual(Double hargaJual) {
		this.hargaJual = hargaJual;
	}

	/**
	 * Flag aktif baris draft, dengan bawaan {@code true} untuk {@code null}.
	 *
	 * <p>Pola <b>flag aktif satu arah</b> yang lazim di model AIS: seluruh baris lama yang
	 * ditulis sebelum kolom ini ada otomatis terbaca aktif, sehingga penambahan kolom tidak
	 * menyembunyikan data historis. Harganya, membedakan "belum pernah diputuskan" dari "sengaja
	 * diaktifkan" mustahil lewat getter ini.</p>
	 *
	 * <p>Perlu ditegaskan flag ini <b>bukan</b> penanda draft sudah dibayar — itu tugas
	 * {@link #getLunas()}. Sebuah baris draft dapat {@code aktif = true} sekaligus sudah lunas,
	 * dan keduanya tidak saling memengaruhi.</p>
	 *
	 * @return {@code true} bila baris aktif atau belum ditentukan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** Memasang flag aktif. Memasang {@code null} setara mengembalikannya ke keadaan "aktif". */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * SKU yang dipesan pada baris draft ini — taut satu-satunya ke katalog produk, dan salah
	 * satu kolom yang disalin apa adanya ke baris final saat finalisasi.
	 *
	 * <p>Getter melewatkan field ke {@code check(...)} milik {@link GeneralValueObject}, helper
	 * bersama yang menormalkan proxy Hibernate: bila relasi lazy ini belum ter-inisialisasi dan
	 * Session-nya sudah tertutup, {@code check} mengembalikan {@code null} alih-alih membiarkan
	 * {@code LazyInitializationException} meledak di lapisan tampilan. Konsekuensinya,
	 * <b>{@code null} di sini ambigu</b> — dapat berarti "baris memang tanpa produk" atau
	 * "produknya ada tetapi tak dapat dimuat sekarang". Perbedaan itu penting justru pada saat
	 * finalisasi: bila draft difinalisasi di luar Session yang memuat produknya, baris final
	 * berisiko lahir tanpa produk dan karenanya tidak akan mengurangi stok apa pun.</p>
	 *
	 * <p>Dipetakan {@code nullable = true} sehingga baris tanpa produk sah secara skema.</p>
	 *
	 * @return produk yang dipesan, atau {@code null} (lihat catatan ambiguitas di atas)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk", nullable = true)
	public Produk getProduk() {
		produk = check(produk);
		return produk;
	}

	/**
	 * Memasang produk yang dipesan. Karena {@code cascade = PERSIST, MERGE}, memasang
	 * {@link Produk} yang belum tersimpan akan ikut menyimpannya — pastikan yang dipasang produk
	 * katalog yang sudah ada.
	 */
	public void setProduk(Produk produk) {
		this.produk = produk;
	}

	/**
	 * Pemesan bila ia berstatus {@link Siswa}, dengan <b>tiga lapis sumber yang saling
	 * menimpa</b> menurut urutan prioritas tetap.
	 *
	 * <p>Getter destruktif. Urutannya dari yang paling lemah ke yang paling kuat:</p>
	 * <ol>
	 *   <li><b>Kolom sendiri</b> ({@code siswa}), dinormalkan lewat {@code check(...)}.</li>
	 *   <li><b>Turunan header draft</b> — bila {@link #getDraftPembelianAnggotaKoperasi()} punya
	 *       anggota koperasi yang ternyata seorang siswa, nilai itu MENGGANTIKAN kolom sendiri.
	 *       Ini menjaga konsistensi pesanan: satu draft hanya punya satu pemesan, dan pemesan
	 *       yang sah adalah yang tercatat di header.</li>
	 *   <li><b>Turunan pembayaran online</b> — bila {@code kodePembayaranOnline} terisi, siswanya
	 *       menang atas keduanya. Lapis ini membaca <b>field</b> {@code kodePembayaranOnline}
	 *       langsung, bukan getter-nya, sehingga tidak aktif bila relasi itu masih proxy yang
	 *       belum termuat.</li>
	 * </ol>
	 * <p>Lapis ketiga menimpa <b>tanpa syarat</b>: bila kode pembayaran online ada tetapi
	 * siswanya {@code null}, kolom {@code siswa} baris ini ikut <b>dikosongkan</b> saat flush.</p>
	 *
	 * @return siswa pemesan, atau {@code null} bila pemesannya bukan siswa
	 * @see #getMember() perakit label identitas yang menelusuri keenam kolom pemesan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = true)
	public Siswa getSiswa() {
		siswa = check(siswa);

		if (getDraftPembelianAnggotaKoperasi() != null && getDraftPembelianAnggotaKoperasi().getAnggotaKoperasi() != null
				&& getDraftPembelianAnggotaKoperasi().getAnggotaKoperasi().getSiswa() != null) {
			siswa = getDraftPembelianAnggotaKoperasi().getAnggotaKoperasi().getSiswa();
		}
		if (kodePembayaranOnline != null) {
			siswa = kodePembayaranOnline.getSiswa();
		}
		return siswa;
	}

	/**
	 * Memasang siswa pemesan. Nilai ini <b>dapat ditimpa</b> pada pembacaan berikutnya oleh
	 * header draft atau kode pembayaran online — lihat {@link #getSiswa()}.
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Jumlah unit dipesan dalam <b>satuan dasar</b>, dengan bawaan {@code 1.0} untuk
	 * {@code null}.
	 *
	 * <p>Inilah kolom kuantitas yang disalin ke baris final saat finalisasi, dan karenanya angka
	 * yang pada akhirnya mengurangi stok serta menagih pembeli. Bawaannya {@code 1.0}, bukan
	 * {@code 0.0} — masuk akal untuk ritel, tetapi berarti <b>tidak ada cara mengungkapkan "qty
	 * belum diisi"</b>: baris draft yang kolomnya {@code NULL} akan difinalisasi sebagai satu
	 * unit tanpa peringatan.</p>
	 *
	 * <p>Bertipe {@code Double} karena produk dapat dijual dalam satuan pecahan (mis. 0,5 kg);
	 * berlaku kewaspadaan umum aritmetika titik-mengambang, jangan membandingkan dengan
	 * {@code ==}.</p>
	 *
	 * <p><b>Tidak ada pendamping {@code qtyInput}/{@code faktorKeDasar} di kelas ini.</b> Bila
	 * kasir memesan "2 Karung", yang tersimpan hanyalah hasil konversinya ke satuan dasar; angka
	 * ketikan asli dan faktor konversinya tidak terekam di jalur draft. Lihat JavaDoc kelas
	 * untuk cakupan dampaknya (terbatas pada tampilan/audit, bukan nilai transaksi).</p>
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
	 * Snapshot nama outlet ("kios") tempat pesanan dibuat, untuk ditampilkan tanpa join ke
	 * tabel {@link Toko}.
	 *
	 * <p>Getter destruktif ganda: menormalkan field {@code toko} lewat {@code check(...)} DAN
	 * menimpa field {@code kios} dengan nama toko yang berlaku sekarang. Seperti
	 * {@link #getNama()}, kolom ini bukan snapshot beku melainkan cermin yang ikut berubah bila
	 * nama outlet diganti.</p>
	 *
	 * <p>Perhatikan getter ini memanggil {@code check(toko)} langsung, <b>bukan</b>
	 * {@link #getToko()} yang punya lapis tambahan dari {@code kodePembayaranOnline}. Pada
	 * pesanan bertaut topup, {@code getKios()} dapat melaporkan outlet yang <b>berbeda</b> dari
	 * {@code getToko().getNama()} selama {@link #getToko()} belum pernah dipanggil lebih dulu.</p>
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
	 * Waktu pesanan draft ini dibuat, dengan bawaan waktu sekarang bila kolomnya kosong.
	 *
	 * <p><b>Perbedaan penting dari {@code Pembelian.getWaktu()}:</b> di sini <b>tidak ada</b>
	 * pengambilalihan oleh header. Padanannya di baris final menimpa dirinya dengan
	 * {@code header.getTanggalPembayaran()} agar seluruh baris satu struk jatuh pada saat
	 * pengakuan pendapatan yang sama. Aturan itu tidak dapat diterapkan di sini karena draft
	 * justru <b>belum dibayar</b> — tanggal bayar belum ada. Ketiadaan pengambilalihan ini
	 * karenanya bukan kelalaian, melainkan konsekuensi wajar dari tahap dokumennya.</p>
	 *
	 * <p>Waspadai bawaannya: bila {@code waktu} {@code null}, yang dikembalikan adalah
	 * {@code WaktuUtil.getDate()} — waktu SAAT DIBACA — dan nilai itu sengaja <b>tidak</b>
	 * ditulis balik ke field, sehingga pembacaan berikutnya menghasilkan waktu berbeda lagi.
	 * Daftar pesanan tertahan yang diurutkan berdasarkan waktu dapat berperilaku tak terduga
	 * untuk baris yang kolomnya kosong, karena setiap baris seperti itu selalu tampak "baru
	 * saja dibuat".</p>
	 *
	 * <p>Perlu dicatat pula bahwa {@code waktu} draft <b>tidak disalin</b> ke baris final:
	 * finalisasi memasang waktu pelunasan yang baru. Jadi kapan pesanan dibuat dan kapan ia
	 * dibayar tersimpan di dua tempat berbeda, dan hanya yang kedua yang masuk ke laporan
	 * penjualan.</p>
	 *
	 * @return waktu pesanan; tidak pernah {@code null}, tetapi bisa tidak stabil (lihat di atas)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
	}

	/** Memasang waktu pesanan dibuat. Berbeda dari {@link Pembelian}, nilai ini tidak akan ditimpa header. */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Merakit satu string identitas pemesan untuk tampilan daftar pesanan tertahan —
	 * menelusuri <b>enam kolom identitas</b> dalam urutan prioritas tetap dan memakai yang
	 * pertama terisi.
	 *
	 * <p>Getter destruktif (menulis balik ke field {@code member}). Urutan penelusuran beserta
	 * formatnya:</p>
	 * <ol>
	 *   <li>{@link #getAnggotaKoperasi()} &rarr; {@code "<kode> <nama>"}</li>
	 *   <li>{@link #getSiswa()} &rarr; {@code "<NISN> <nama>"}</li>
	 *   <li>{@link #getMahasiswa()} &rarr; {@code "<NIM> <nama>"}</li>
	 *   <li>{@link #getBiodataCalonMahasiswa()} &rarr; {@code "<no registrasi> <nama>"}</li>
	 *   <li>{@link #getCalonSiswa()} &rarr; {@code "<no registrasi> <nama>"}</li>
	 *   <li>anggota koperasi dari header draft &rarr; {@code "<kode identitas> <nama>"}</li>
	 *   <li>{@link #getTbmuser()} &rarr; nama pengguna saja, tanpa nomor identitas</li>
	 * </ol>
	 * <p>Perhatikan cabang pertama dan keenam sama-sama mengambil anggota koperasi tetapi memakai
	 * properti nomor yang BERBEDA — {@code getKode()} versus {@code getKodeIdentitas()}. Karena
	 * {@link #getAnggotaKoperasi()} sendiri sudah mengambil alih nilainya dari header, cabang
	 * keenam praktis hampir tak pernah tercapai; ia peninggalan dari masa sebelum
	 * pengambilalihan itu ada dan dipertahankan sebagai jaring pengaman data lama.</p>
	 * <p>Method ini memanggil banyak getter destruktif lain, sehingga sekadar menampilkan nama
	 * pemesan di sebuah grid dapat memicu penimpaan beberapa kolom identitas sekaligus — penyebab
	 * paling umum entity ini menjadi dirty tanpa ada kode yang tampak mengubah apa pun.</p>
	 * <p>Bila seluruh cabang gagal, nilai lama field dikembalikan apa adanya — dapat {@code null}
	 * untuk pesanan tanpa identitas.</p>
	 *
	 * @return label identitas pemesan, atau {@code null} bila tidak ada identitas sama sekali
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
		} else if (getDraftPembelianAnggotaKoperasi() != null
				&& getDraftPembelianAnggotaKoperasi().getAnggotaKoperasi() != null) {
			member = getDraftPembelianAnggotaKoperasi().getAnggotaKoperasi().getKodeIdentitas() + " "
					+ getDraftPembelianAnggotaKoperasi().getAnggotaKoperasi().getNama();
		} else if (getTbmuser() != null) {
			member = getTbmuser().getUserNama();
		}
		return member;
	}

	/** Memasang label identitas pemesan. Hampir selalu ditimpa {@link #getMember()} pada pembacaan berikutnya. */
	public void setMember(String member) {
		this.member = member;
	}

	/**
	 * Outlet tempat pesanan ini dibuat — <b>pembatas tenant utama</b> untuk modul kantin.
	 *
	 * <p>Hampir setiap pemeriksaan kepemilikan di lapisan API membandingkan toko pedagang
	 * pemanggil dengan toko baris/produk yang diakses, sehingga kolom ini adalah pemisah data
	 * antar-outlet yang sebenarnya. Dipetakan {@code nullable = false}: setiap baris draft WAJIB
	 * punya outlet.</p>
	 *
	 * <p>Getter destruktif dengan satu lapis penimpaan dari {@code kodePembayaranOnline}, karena
	 * pada pesanan bertaut topup outlet yang sah adalah yang terdaftar di kode pembayaran.
	 * Penimpaan ini <b>tanpa penjaga {@code null}</b>: bila kode pembayaran ada tetapi tokonya
	 * {@code null}, field {@code toko} ikut dikosongkan — dan karena kolomnya
	 * {@code nullable = false}, baris ter-attach yang dibaca dalam kondisi itu dapat memicu
	 * kegagalan constraint saat flush, pada pembacaan biasa yang tidak tampak mengubah apa pun.</p>
	 *
	 * <p>Perhatikan {@code toko} draft <b>tidak</b> disalin dari draft ke baris final:
	 * finalisasi memasang toko dari konteks header. Keduanya normalnya sama, tetapi bukan karena
	 * ada mekanisme yang menjaminnya.</p>
	 *
	 * @return outlet baris draft ini
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
	 * Pemesan bila ia {@link CalonSiswa} — pendaftar yang belum resmi diterima.
	 *
	 * <p>Getter destruktif dua lapis: kolom sendiri (dinormalkan {@code check(...)}), lalu
	 * penimpaan tanpa syarat dari {@code kodePembayaranOnline}. Berbeda dari {@link #getSiswa()},
	 * di sini <b>tidak ada</b> lapis turunan dari header draft — {@link AnggotaKoperasi} memang
	 * tidak menaut calon siswa, sehingga identitas ini hanya dapat berasal dari kolom baris atau
	 * dari kode pembayaran online.</p>
	 *
	 * @return calon siswa pemesan, atau {@code null}
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

	/** Memasang calon siswa pemesan. Dapat ditimpa kode pembayaran online — lihat {@link #getCalonSiswa()}. */
	public void setCalonSiswa(CalonSiswa calonSiswa) {
		this.calonSiswa = calonSiswa;
	}

	/**
	 * Harga jual <b>satu unit</b> — harga per unit yang sesungguhnya, berbeda dari
	 * {@link #getHargaJual()} yang menyimpan nilai baris. <b>Inilah kolom harga yang disalin ke
	 * baris final</b> saat finalisasi, sehingga ia yang pada akhirnya menentukan tagihan.
	 *
	 * <p>Karena perannya itu, dua sifat berikut layak diperhatikan:</p>
	 * <ul>
	 *   <li><b>Syarat pembangkitannya hanya {@code null}</b>, tidak termasuk {@code 0.0} —
	 *       kebalikan dari {@link #getHargaJual()}. Artinya harga nol yang disengaja (barang
	 *       gratis, hadiah) BERTAHAN di sini dan tersalin apa adanya ke baris final. Ini kolom
	 *       yang benar untuk menyatakan "memang gratis".</li>
	 *   <li><b>Harga diambil dari katalog SAAT DIBACA</b>, bukan saat pesanan dibuat. Untuk
	 *       pesanan yang ditahan lama dan harganya sempat berubah di katalog, baris draft yang
	 *       kolom harganya kosong akan difinalisasi memakai harga <b>hari pelunasan</b>, bukan
	 *       harga saat pemesanan. Bila kebijakan yang dikehendaki adalah mengunci harga pada saat
	 *       memesan, alur pembuatan draft wajib memasang harga eksplisit lewat
	 *       {@link #setHargaSatuan(Double)} — entity ini tidak menguncinya sendiri.</li>
	 * </ul>
	 * <p>Getter membaca field {@code produk} secara <b>langsung</b>, bukan lewat
	 * {@link #getProduk()}. Akibatnya, bila relasi produk masih proxy lazy yang belum disentuh,
	 * {@code produk.getHargaJual()} akan memicu inisialisasi proxy dan di luar Session melempar
	 * {@code LazyInitializationException} alih-alih dilindungi {@code check(...)}.</p>
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
	 * Memasang harga jual per unit. Satu-satunya cara mengunci harga pada saat pemesanan;
	 * berbeda dari {@link #setHargaJual(Double)}, nilai {@code 0.0} di sini BERTAHAN.
	 */
	public void setHargaSatuan(Double hargaSatuan) {
		this.hargaSatuan = hargaSatuan;
	}

	/**
	 * Jejak berkas unggahan bila baris draft ini lahir dari impor massal, bukan dari kasir.
	 * Bernilai {@code null} — dan itu normal — untuk pesanan yang dibuat lewat POS.
	 *
	 * <p>Getter murni tanpa efek samping. Relasi ini tidak diberi {@code fetch = LAZY} melainkan
	 * dibiarkan pada bawaan {@code EAGER} milik {@code @ManyToOne}, dengan
	 * {@code @Fetch(FetchMode.SELECT)} yang memaksa pemuatannya lewat SELECT terpisah alih-alih
	 * ikut JOIN kueri utama — menghindari pembengkakan kueri daftar pesanan, dengan konsekuensi
	 * satu SELECT tambahan per baris.</p>
	 *
	 * @return info unggahan asal, atau {@code null} untuk pesanan normal
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "upload_log", nullable = true)
	public UploadLogInfo getUploadLog() {
		return uploadLog;
	}

	/** Memasang jejak berkas unggahan asal. Diisi hanya oleh alur impor massal. */
	public void setUploadLog(UploadLogInfo uploadLog) {
		this.uploadLog = uploadLog;
	}

	/**
	 * Pemesan bila ia {@link Mahasiswa}. Tiga lapis sumber, sepola {@link #getSiswa()}: kolom
	 * sendiri, lalu mahasiswa milik anggota koperasi di header draft, lalu mahasiswa milik kode
	 * pembayaran online sebagai lapis terkuat.
	 *
	 * <p>Getter destruktif — setiap lapis menulis balik ke field. Lapis header hanya menimpa bila
	 * hasilnya bukan {@code null} (dijaga rantai pemeriksaan bertingkat), sedangkan lapis kode
	 * pembayaran online menimpa <b>tanpa syarat</b> dan dapat mengosongkan kolom.</p>
	 *
	 * @return mahasiswa pemesan, atau {@code null} bila pemesannya bukan mahasiswa
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);

		if (getDraftPembelianAnggotaKoperasi() != null && getDraftPembelianAnggotaKoperasi().getAnggotaKoperasi() != null
				&& getDraftPembelianAnggotaKoperasi().getAnggotaKoperasi().getMahasiswa() != null) {
			mahasiswa = getDraftPembelianAnggotaKoperasi().getAnggotaKoperasi().getMahasiswa();
		}

		if (kodePembayaranOnline != null) {
			mahasiswa = kodePembayaranOnline.getMahasiswa();
		}

		return mahasiswa;
	}

	/** Memasang mahasiswa pemesan. Dapat ditimpa header draft/kode pembayaran online — lihat {@link #getMahasiswa()}. */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Pemesan bila ia {@link BiodataCalonMahasiswa} — pendaftar PMB yang belum menjadi mahasiswa.
	 *
	 * <p>Getter destruktif dua lapis: kolom sendiri lalu penimpaan tanpa syarat dari
	 * {@code kodePembayaranOnline}. Seperti {@link #getCalonSiswa()}, tidak ada lapis turunan
	 * dari header draft karena {@link AnggotaKoperasi} tidak menaut calon mahasiswa.</p>
	 *
	 * @return calon mahasiswa pemesan, atau {@code null}
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

	/** Memasang calon mahasiswa pemesan. Dapat ditimpa kode pembayaran online. */
	public void setBiodataCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	/**
	 * Pemesan bila ia pengguna internal ({@link Tbmuser}: guru, dosen, pegawai, atau pemegang
	 * peran lain) — kolom identitas dengan <b>prioritas paling rendah</b>, dan satu-satunya yang
	 * dapat DIKOSONGKAN secara aktif oleh getter-nya sendiri.
	 *
	 * <p>Getter destruktif tiga lapis, dengan lapis ketiga yang berperilaku unik:</p>
	 * <ol>
	 *   <li>Kolom sendiri, dinormalkan {@code check(...)}.</li>
	 *   <li>Penimpaan tanpa syarat dari {@code kodePembayaranOnline}.</li>
	 *   <li><b>Pengosongan paksa.</b> Bila SALAH SATU dari {@link #getSiswa()},
	 *       {@link #getCalonSiswa()}, {@link #getBiodataCalonMahasiswa()}, atau
	 *       {@link #getMahasiswa()} terisi, field {@code tbmuser} dipaksa {@code null}. Ini
	 *       menegakkan aturan bahwa identitas akademik selalu mengalahkan identitas akun sistem:
	 *       seorang mahasiswa yang juga punya akun harus tercatat sebagai mahasiswa, bukan
	 *       sebagai "pengguna". Tanpa aturan ini {@link #getJenisMember()} dapat melabelinya
	 *       "Karyawan" hanya karena akunnya tertaut ke data pegawai.</li>
	 * </ol>
	 * <p>Pengosongan itu bukan sekadar memengaruhi nilai kembalian melainkan <b>menulis
	 * {@code null} ke field</b>, sehingga pada entity ter-attach membaca properti ini menghapus
	 * taut {@code tbmuser} di database saat flush. Untuk jejak "akun mana yang membuat pesanan"
	 * yang terpisah dari "siapa pemesannya", pakai {@link #getOlehId()}, bukan kolom ini.</p>
	 *
	 * <p>Lapis ketiga memanggil empat getter destruktif sekaligus, sehingga membaca properti ini
	 * berpotensi menulis balik empat kolom identitas lain.</p>
	 *
	 * @return pengguna internal pemesan, atau {@code null} bila ada identitas akademik yang
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
	 * Memasang pengguna internal sebagai pemesan. Nilai ini akan <b>dibuang</b> pada pembacaan
	 * berikutnya bila baris juga punya identitas akademik — lihat {@link #getTbmuser()}.
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}



	/**
	 * Potongan harga baris dalam <b>rupiah</b> (bukan persen), dengan bawaan {@code 0.0}.
	 * Termasuk kolom yang <b>disalin ke baris final</b> saat finalisasi.
	 *
	 * <p>Disimpan sebagai nominal jadi — bukan persentase — sehingga perubahan pada
	 * {@link #getAturanDiskon()} di kemudian hari tidak menulis ulang potongan baris lama. Kolom
	 * ini snapshot hasil perhitungan; aturan asalnya hanya dicatat sebagai rujukan.</p>
	 *
	 * <p><b>Tidak ada penjaga batas atas.</b> Entity tidak memeriksa bahwa diskon
	 * &le; {@code hargaSatuan × qty}, sehingga diskon berlebih menghasilkan {@link #getTotal()}
	 * NEGATIF tanpa peringatan — dan karena kolom ini tersalin apa adanya ke baris final,
	 * nilai janggal yang lolos di tahap draft akan ikut terbawa ke penjualan definitif. Validasi
	 * kewajaran diskon sepenuhnya tanggung jawab lapisan pemanggil. Nilai negatif juga tidak
	 * dicegah dan akan berperilaku sebagai biaya tambahan.</p>
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
	 * Nilai bersih baris draft: {@code (hargaSatuan × qty) − diskon}. <b>Selalu dihitung ulang,
	 * dan nilai yang tersimpan di kolom SELALU dibuang.</b>
	 *
	 * <p>Bentuk getter destruktif paling tegas di kelas ini: berbeda dari
	 * {@link #getHargaJual()} yang hanya menghitung saat kosong, method ini menghitung
	 * <b>tanpa syarat</b> pada setiap pemanggilan lalu menimpa field {@code total}. Karena itu
	 * {@link #setTotal(Double)} praktis tidak berguna untuk memaksakan nilai — apa pun yang
	 * dipasang hilang pada pembacaan pertama. Kolom {@code total} di database efektifnya
	 * <i>cache</i> hasil perhitungan, bukan data yang berdiri sendiri.</p>
	 *
	 * <p>Untuk entity draft ini perilaku tersebut justru menguntungkan: pesanan tertahan memang
	 * sering diubah (tambah qty, ganti produk), dan perhitungan ulang tanpa syarat menjamin
	 * totalnya tidak pernah basi terhadap komponennya. Sisi buruknya sama seperti di
	 * {@link Pembelian}: karena {@link #getHargaSatuan()} dapat memungut harga katalog hari ini
	 * untuk baris yang kolom harganya kosong, total sebuah pesanan lama dapat <b>berubah
	 * sendiri</b> antara saat dipesan dan saat dibayar.</p>
	 *
	 * <p>Perhatikan {@link #getCashback()} <b>tidak</b> ikut diperhitungkan — cashback bukan
	 * pengurang tagihan melainkan imbalan terpisah. Perhatikan pula {@code total} <b>tidak</b>
	 * termasuk kolom yang disalin ke baris final; baris final menghitung totalnya sendiri.</p>
	 *
	 * @return nilai bersih baris; tidak pernah {@code null}, dapat negatif bila diskon berlebih
	 */
	public Double getTotal() {
		total = (getHargaSatuan() * getQty()) - getDiskon();
		return total;
	}

	/**
	 * Memasang nilai total baris. <b>Nyaris tanpa efek</b>: {@link #getTotal()} menghitung ulang
	 * tanpa syarat dan menimpanya. Praktis hanya dipakai Hibernate saat memuat baris.
	 */
	public void setTotal(Double total) {
		this.total = total;
	}

	/**
	 * Kode pembayaran online (topup) yang dikaitkan ke baris draft ini — <b>sumber identitas
	 * dengan prioritas tertinggi</b> di seluruh kelas ini.
	 *
	 * <p>Bila terisi, taut ini mengambil alih {@link #getSiswa()}, {@link #getMahasiswa()},
	 * {@link #getCalonSiswa()}, {@link #getBiodataCalonMahasiswa()}, {@link #getTbmuser()}, dan
	 * bahkan {@link #getToko()} — masing-masing menimpa kolom baris tanpa syarat, karena pada
	 * transaksi topup data pembayaranlah yang otoritatif.</p>
	 *
	 * <p>Kehalusan yang berulang di seluruh berkas: getter-getter yang menimpa itu membaca
	 * <b>field</b> {@code kodePembayaranOnline} secara langsung dan tidak pernah memanggil getter
	 * ini. Karena relasi dimuat {@code EAGER} lewat {@code @Fetch(FetchMode.SELECT)}, dalam
	 * praktik ia sudah terisi begitu baris dimuat sehingga perbedaan itu jarang terlihat — tetapi
	 * pada objek yang dirakit manual di memori, urutan pemanggilan dapat mengubah hasil.</p>
	 *
	 * <p>Getter murni; tidak menormalkan lewat {@code check(...)} seperti relasi lain.</p>
	 *
	 * @return kode pembayaran online, atau {@code null} untuk pesanan tunai/nontopup
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kode_pembayaran_online", nullable = true)
	public KodePembayaranOnline getKodePembayaranOnline() {
		return kodePembayaranOnline;
	}

	/**
	 * Memasang kode pembayaran online. Sadari bahwa memasang nilai di sini akan <b>menimpa enam
	 * kolom lain</b> pada pembacaan berikutnya — lihat {@link #getKodePembayaranOnline()}.
	 */
	public void setKodePembayaranOnline(KodePembayaranOnline kodePembayaranOnline) {
		this.kodePembayaranOnline = kodePembayaranOnline;
	}

	/**
	 * Merakit label KATEGORI pemesan ("Siswa", "Dosen", "Masyarakat Umum", ...) — pendamping
	 * {@link #getMember()} yang merakit label IDENTITAS dari kolom yang sama.
	 *
	 * <p>Getter destruktif. Urutan penelusurannya <b>berbeda</b> dari {@link #getMember()} dan
	 * perbedaan itu disengaja: di sini identitas akademik didahulukan, di sana keanggotaan
	 * koperasi.</p>
	 * <ol>
	 *   <li>{@link #getMahasiswa()} &rarr; "Mahasiswa"</li>
	 *   <li>{@link #getSiswa()} &rarr; "Siswa"</li>
	 *   <li>{@link #getCalonSiswa()} &rarr; "Calon Siswa"</li>
	 *   <li>{@link #getBiodataCalonMahasiswa()} &rarr; "Calon Mahasiswa"</li>
	 *   <li>{@link #getTbmuser()} berdata dosen &rarr; "Dosen"</li>
	 *   <li>{@link #getTbmuser()} berdata guru &rarr; "Guru"</li>
	 *   <li>{@link #getTbmuser()} berdata pegawai &rarr; "Karyawan"</li>
	 *   <li>{@link #getTbmuser()} dengan hak akses &rarr; nama peran apa adanya — cabang DINAMIS,
	 *       sehingga nilai kolom ini tidak terbatas pada himpunan tetap dan tidak boleh
	 *       diperlakukan sebagai enum</li>
	 *   <li>anggota koperasi di header draft &rarr; nama jenis anggota koperasi (juga dinamis)</li>
	 *   <li>selain itu &rarr; "Masyarakat Umum"</li>
	 * </ol>
	 * <p>Cabang 5&ndash;8 memanggil {@link #getTbmuser()} berulang kali, dan getter itu sendiri
	 * destruktif serta memicu empat getter identitas lainnya — satu pemanggilan method ini dapat
	 * menyentuh dan menulis balik hampir seluruh kolom identitas baris. Karena
	 * {@link #getTbmuser()} memaksa {@code null} ketika ada identitas akademik, cabang
	 * 5&ndash;8 tidak pernah tercapai untuk pemesan yang juga siswa/mahasiswa — konsisten dengan
	 * urutan di atas, bukan bertentangan dengannya.</p>
	 * <p>Cabang terakhir memastikan method ini <b>tidak pernah</b> mengembalikan {@code null},
	 * berbeda dari {@link #getMember()} yang bisa.</p>
	 *
	 * @return label kategori pemesan; tidak pernah {@code null}
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
		} else if (getDraftPembelianAnggotaKoperasi() != null && getDraftPembelianAnggotaKoperasi().getAnggotaKoperasi() != null
				&& getDraftPembelianAnggotaKoperasi().getAnggotaKoperasi().getJenisAnggotaKoperasi() != null) {
			jenisMember = getDraftPembelianAnggotaKoperasi().getAnggotaKoperasi().getJenisAnggotaKoperasi().getNama();
		} else {
			jenisMember = "Masyarakat Umum";
		}
		return jenisMember;
	}

	/** Memasang label kategori pemesan. Selalu ditimpa {@link #getJenisMember()} pada pembacaan berikutnya. */
	public void setJenisMember(String jenisMember) {
		this.jenisMember = jenisMember;
	}

	/**
	 * Merakit label cara pembayaran yang <b>direncanakan</b> untuk pesanan ini — perlu ditekankan
	 * bahwa pada tahap draft ini rencana, bukan fakta: draft menurut definisinya belum dibayar.
	 *
	 * <p>Getter destruktif dengan urutan:</p>
	 * <ol>
	 *   <li>Bila {@code kodePembayaranOnline} terisi &rarr; {@code "Online (Topup)"} — label
	 *       harfiah, bukan nama kanal pembayaran sebenarnya. Informasi kanal ada di objek
	 *       {@link KodePembayaranOnline} dan tidak muncul di sini.</li>
	 *   <li>Bila header draft punya {@code CaraPembayaranKoperasi} &rarr; namanya. Satu-satunya
	 *       cabang yang menghasilkan label DINAMIS dari master.</li>
	 *   <li>Selain itu &rarr; {@code "Tunai"}. Ini <b>anggapan</b>, bukan fakta tercatat: pesanan
	 *       tanpa header dan tanpa kode pembayaran online akan dilabeli tunai walau tidak ada
	 *       bukti apa pun. Untuk baris hasil impor data lama, "Tunai" harus dibaca sebagai
	 *       "tidak diketahui".</li>
	 * </ol>
	 * <p>Berbeda dari {@link Pembelian}, kelas ini <b>tidak punya</b> relasi
	 * {@code caraPembayaranKoperasi} di tingkat baris sama sekali — cara bayar hanya ada di
	 * header draft, dan cabang 2 membacanya dari sana. Ketiadaan kolom itu di tingkat baris
	 * konsisten dengan kenyataan bahwa cara membayar adalah sifat satu pesanan, bukan sifat tiap
	 * barang di dalamnya.</p>
	 *
	 * @return label cara pembayaran yang direncanakan; tidak pernah {@code null}
	 */
	public String getCaraBayar() {
		if (kodePembayaranOnline != null) {
			caraBayar = "Online (Topup)";
		} else if (getDraftPembelianAnggotaKoperasi() != null
				&& getDraftPembelianAnggotaKoperasi().getCaraPembayaranKoperasi() != null) {
			caraBayar = getDraftPembelianAnggotaKoperasi().getCaraPembayaranKoperasi().getNama();
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
	 * Memasang anggota koperasi pemesan. Nilai ini <b>diabaikan</b> pada pembacaan berikutnya
	 * bila baris punya header draft — lihat pengambilalihan di {@link #getAnggotaKoperasi()}.
	 */
	public void setAnggotaKoperasi(AnggotaKoperasi anggotaKoperasi) {
		this.anggotaKoperasi = anggotaKoperasi;
	}

	/**
	 * Anggota koperasi pemesan, dengan <b>pengambilalihan mutlak oleh header draft</b> bila
	 * header ada.
	 *
	 * <p>Getter destruktif berbentuk if/else, bukan berlapis: bila
	 * {@link #getDraftPembelianAnggotaKoperasi()} terisi, kolom {@code anggota_koperasi} milik
	 * baris ini <b>sama sekali tidak dibaca</b> — nilainya langsung diganti dengan anggota
	 * koperasi milik header, termasuk bila header itu sendiri tidak punya anggota
	 * ({@code null}). Baru ketika header tidak ada, kolom sendiri dipakai dan dinormalkan lewat
	 * {@code check(...)}.</p>
	 *
	 * <p>Inilah bentuk paling jelas dari pola "begitu induk terpasang, anak berhenti membaca
	 * kolomnya sendiri" di kelas ini. Pada baris yang punya header, kolom
	 * {@code anggota_koperasi} di database menjadi data mati: tidak pernah dibaca lagi dan akan
	 * ditimpa nilai header pada flush berikutnya. Kueri SQL langsung yang menyaring berdasarkan
	 * kolom itu dapat memberi hasil berbeda dari yang dilihat aplikasi; penyaringan yang benar
	 * harus menempuh header.</p>
	 *
	 * <p>Satu beda kecil namun bermakna dari padanannya di {@link Pembelian}: cabang pertama di
	 * sini memanggil <b>getter</b> {@link #getDraftPembelianAnggotaKoperasi()}, sedangkan
	 * {@code Pembelian.getAnggotaKoperasi()} membaca field header secara langsung. Karena getter
	 * header di kelas ini tidak memakai {@code check(...)} melainkan mengembalikan field apa
	 * adanya, hasil akhirnya setara — tetapi bentuk kode yang berbeda ini kerap menimbulkan
	 * kesan keliru bahwa keduanya berperilaku lain.</p>
	 *
	 * @return anggota koperasi pemesan, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "anggota_koperasi", nullable = true)
	public AnggotaKoperasi getAnggotaKoperasi() {
		if (getDraftPembelianAnggotaKoperasi() != null) {
			anggotaKoperasi = getDraftPembelianAnggotaKoperasi().getAnggotaKoperasi();
		} else {
			anggotaKoperasi = check(anggotaKoperasi);
		}
		return anggotaKoperasi;
	}

	// ==========================================================
	// MODIFIKASI UNTUK FITUR ATURAN DISKON & CASHBACK
	// ==========================================================

	/**
	 * Cashback dalam rupiah yang akan diperoleh pemesan dari baris ini, hasil penerapan
	 * {@link #getAturanDiskon()}. Termasuk kolom yang <b>disalin ke baris final</b>.
	 *
	 * <p><b>Bukan pengurang tagihan.</b> Berbeda dari {@link #getDiskon()}, {@link #getTotal()}
	 * sama sekali tidak memperhitungkan cashback — pemesan tetap membayar penuh, dan cashback
	 * adalah imbalan terpisah yang dikreditkan ke saldonya. Menjumlahkan cashback ke dalam nilai
	 * penjualan adalah kesalahan yang mudah terjadi: secara akuntansi ia beban promosi, bukan
	 * potongan pendapatan.</p>
	 *
	 * <p>Seperti diskon, disimpan sebagai nominal jadi (snapshot) sehingga perubahan aturan
	 * diskon kelak tidak menulis ulang cashback baris lama. Getter murni dengan bawaan
	 * {@code 0.0}; tidak ada penjaga yang mencegah nilai negatif.</p>
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
	 * Aturan diskon yang menghasilkan {@link #getDiskon()} dan {@link #getCashback()} pada baris
	 * ini — dicatat sebagai <b>rujukan/jejak</b>, bukan sumber perhitungan. Termasuk kolom yang
	 * disalin ke baris final.
	 *
	 * <p>Nilai potongan dan cashback sudah dibekukan sebagai nominal di kolomnya masing-masing,
	 * jadi mengubah atau menghapus aturan di master tidak mengubah angka baris lama. Taut ini ada
	 * untuk menjawab "kenapa baris ini dapat potongan?" saat audit, dan untuk laporan efektivitas
	 * promosi.</p>
	 *
	 * <p>Getter murni-normalisasi ({@code check(...)}) tanpa lapis penimpaan. Bernilai
	 * {@code null} untuk mayoritas baris — pesanan tanpa promosi.</p>
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
	 * Penanda barang pada baris draft ini sudah <b>diserahkan secara fisik</b> kepada pemesan.
	 *
	 * <p>Di tahap draft, flag ini menjadi bermakna khusus: ia memungkinkan barang diserahkan
	 * <b>sebelum</b> dibayar — persis skenario kantin/kafe di mana makanan diantar ke meja lebih
	 * dulu dan tagihannya diselesaikan belakangan. Jadi {@code terlayani = true} pada baris yang
	 * {@link #getLunas()}-nya masih {@code null} adalah keadaan yang sah dan lazim, bukan
	 * kelainan data. Layar dapur memakai flag ini untuk mengetahui apa yang masih harus
	 * dikeluarkan.</p>
	 *
	 * <p>Bawaannya {@code false} untuk {@code null} — <b>kebalikan</b> dari {@link #getAktif()}
	 * yang berbawaan {@code true}. Perbedaan arah ini disengaja dan aman: pilihan konservatif
	 * untuk "sudah diserahkan?" adalah menganggap belum, sehingga baris lama tidak keliru
	 * dianggap sudah dilayani. Seperti flag satu arah lainnya, "belum ditentukan" tidak dapat
	 * dibedakan dari "tegas belum", dan entity tidak mencatat kapan maupun oleh siapa penyerahan
	 * terjadi.</p>
	 *
	 * <p>Perhatikan flag ini <b>tidak</b> termasuk kolom yang disalin ke baris final — status
	 * penyerahan berhenti di dokumen draft dan tidak terbawa ke penjualan definitif.</p>
	 *
	 * @return {@code true} bila barang sudah diserahkan; {@code false} bila belum atau belum
	 *         ditentukan
	 */
	public Boolean getTerlayani() {
		return terlayani == null ? false : terlayani;
	}

	/** Menandai barang baris draft ini sudah/belum diserahkan. Berdiri sendiri dari status lunas. */
	public void setTerlayani(Boolean terlayani) {
		this.terlayani = terlayani;
	}

	/**
	 * Gap-closure "Produk Ekstra" -- sama makna dgn {@link Pembelian#getIndukId()}, TAPI di sini
	 * nilainya menunjuk ke {@code id} baris {@link DraftPembelian} LAIN (bukan {@code Pembelian}),
	 * karena baris ini masih berupa draft/tertahan. Saat draft difinalisasi
	 * ({@code PembelianAnggotaKoperasi.simpanRinci}, cabang draft-finalization), nilai ini di-remap
	 * 2-pass jadi {@code Pembelian.indukId} yang menunjuk ke {@code Pembelian} baru hasil finalisasi
	 * induknya -- lihat JavaDoc di sana.
	 */
	@Column(name = "induk_id")
	public Long getIndukId() {
		return indukId;
	}

	/**
	 * Memasang id baris draft induk untuk baris "Produk Ekstra". Nilainya harus berupa id
	 * {@link DraftPembelian} lain di draft yang SAMA — tidak ada foreign key yang menegakkan
	 * aturan itu (lihat {@link #getIndukId()}).
	 */
	public void setIndukId(Long indukId) {
		this.indukId = indukId;
	}

	/**
	 * Baris {@link Pembelian} hasil finalisasi baris draft ini — <b>penanda "sudah dibayar"
	 * sekaligus gerbang anti-finalisasi-ganda</b>. {@code null} berarti baris masih tertahan.
	 *
	 * <p>Perhatikan tipenya: {@link Pembelian}, yaitu <b>baris final</b>, bukan header. Ini
	 * sejajar dengan tingkat dokumennya sendiri, dan berbeda dari
	 * {@code DraftPembelianAnggotaKoperasi.getLunas()} yang bertipe
	 * {@code PembelianAnggotaKoperasi} (header final). Kedua properti bernama sama, berada di
	 * dua kelas yang namanya mirip, dan bertipe berbeda — sumber kekeliruan yang mudah terjadi.</p>
	 *
	 * <p>Taut ini dipasang di akhir setiap iterasi penyalinan pada
	 * {@code PembelianAnggotaKoperasi.simpanRinci}, tepat setelah baris {@link Pembelian}
	 * padanannya tersimpan dan mendapat id. Perannya ganda:</p>
	 * <ul>
	 *   <li><b>Jejak asal-usul.</b> Menjawab "baris penjualan ini dulunya pesanan yang mana?"
	 *       tanpa perlu mencocokkan kode atau waktu. Perlu dicatat arah tautnya <b>satu arah</b>:
	 *       {@link Pembelian} tidak punya taut balik ke draft, sehingga penelusuran dari baris
	 *       final ke draft asalnya hanya mungkin lewat kueri terbalik pada kolom ini.</li>
	 *   <li><b>Gerbang anti-dobel.</b> Selama taut ini terisi, baris draft tidak boleh
	 *       difinalisasi lagi — tanpa gerbang tersebut, memproses ulang sebuah draft akan
	 *       melahirkan baris penjualan kedua untuk barang yang sama, menggandakan pendapatan
	 *       sekaligus mengurangi stok dua kali.</li>
	 * </ul>
	 * <p><b>Penegakan gerbang itu sepenuhnya ada di pemanggil, bukan di entity ini.</b> Kelas ini
	 * tidak menolak {@link #setLunas(Pembelian)} dipanggil dua kali, tidak memeriksa apakah nilai
	 * lama sudah terisi, dan tidak punya constraint basis data yang mencegahnya — persis seperti
	 * yang berlaku pada padanannya di tingkat header. Pemeriksaan yang sesungguhnya dilakukan di
	 * tingkat header ({@code draft.getLunas() != null} diperiksa lebih dulu, dan alur langsung
	 * berhenti bila sudah terisi), sehingga perlindungan baris-baris di dalamnya bersifat
	 * <i>tidak langsung</i> — ia bergantung pada keutuhan pemeriksaan di tingkat atasnya. Setiap
	 * alur baru yang memfinalisasi baris draft tanpa melewati gerbang header itu akan melewati
	 * perlindungan ini tanpa peringatan apa pun.</p>
	 *
	 * <p>Dimuat {@code EAGER} lewat {@code @Fetch(FetchMode.SELECT)} sehingga status lunas sebuah
	 * baris draft selalu diketahui begitu baris dimuat, tanpa risiko
	 * {@code LazyInitializationException} saat diperiksa dari lapisan tampilan.</p>
	 *
	 * @return baris penjualan hasil finalisasi, atau {@code null} bila draft belum dibayar
	 * @see DraftPembelianAnggotaKoperasi#getLunas() padanan di tingkat header (bertipe berbeda)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "lunas", nullable = true)
	public Pembelian getLunas() {
		return lunas;
	}

	/**
	 * Menandai baris draft ini sudah difinalisasi dengan menautkan baris {@link Pembelian} hasil
	 * penyalinannya.
	 *
	 * <p>Harus dipanggil dalam alur penyimpanan yang sama dengan pembuatan baris final agar tidak
	 * ada keadaan antara berupa baris penjualan yang sudah lahir sementara draftnya belum
	 * tertandai — keadaan seperti itu membuat draft dapat difinalisasi ulang dan menggandakan
	 * penjualan. Setter ini <b>tidak</b> memeriksa apakah taut lama sudah terisi; lihat catatan
	 * penegakan gerbang di {@link #getLunas()}.</p>
	 *
	 * @param lunas baris penjualan hasil finalisasi
	 */
	public void setLunas(Pembelian lunas) {
		this.lunas = lunas;
	}

	/**
	 * Header draft induk yang memuat baris ini — taut anak&rarr;induk pada tahap draft, padanan
	 * {@code Pembelian.getPembelianAnggotaKoperasi()}.
	 *
	 * <p>Relasi ini adalah sumber kebenaran bagi beberapa getter lain di kelas ini:
	 * {@link #getAnggotaKoperasi()} mengambil alih nilainya dari sini, {@link #getCaraBayar()}
	 * membaca cara pembayaran darinya, dan {@link #getSiswa()}/{@link #getMahasiswa()}
	 * memakainya sebagai lapis kedua. Ia juga kunci yang dipakai finalisasi untuk mengumpulkan
	 * seluruh baris milik satu draft ({@code Restrictions.eq("draftPembelianAnggotaKoperasi",
	 * ...)}), sehingga baris yang kolom ini {@code null} <b>tidak akan pernah ikut
	 * difinalisasi</b> — ia menjadi baris yatim yang tertinggal di tabel draft selamanya.</p>
	 *
	 * <p>Dipetakan {@code nullable = true} dan dimuat {@code EAGER} lewat
	 * {@code @Fetch(FetchMode.SELECT)}. Getter murni yang mengembalikan field apa adanya —
	 * berbeda dari kebanyakan getter relasi lain di kelas ini, ia tidak menormalkan lewat
	 * {@code check(...)}.</p>
	 *
	 * @return header draft induk, atau {@code null} untuk baris draft tanpa dokumen induk
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "draft_pembelian_anggota_koperasi", nullable = true)
	public DraftPembelianAnggotaKoperasi getDraftPembelianAnggotaKoperasi() {
		return draftPembelianAnggotaKoperasi;
	}

	/**
	 * Memasang header draft induk. Wajib diisi agar baris ikut terjaring saat draft
	 * difinalisasi — lihat {@link #getDraftPembelianAnggotaKoperasi()}.
	 */
	public void setDraftPembelianAnggotaKoperasi(DraftPembelianAnggotaKoperasi draftPembelianAnggotaKoperasi) {
		this.draftPembelianAnggotaKoperasi = draftPembelianAnggotaKoperasi;
	}
}
