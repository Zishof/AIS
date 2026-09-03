package ais.database.model.sekolah;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

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

import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.PostingHistory;

/**
 * Entity <b>kepala (header) transaksi belanja seorang siswa di kantin sekolah</b>, dipetakan ke
 * tabel {@code sekolah.pembelian_siswa}. Satu baris mewakili satu struk/nota: siapa yang belanja
 * ({@link #getSiswa()}), di kantin mana ({@link #getKantin()}), kapan ({@link #getTanggal()}), dan
 * berapa total rupiahnya ({@link #getNominal()}). Rincian item per produk TIDAK disimpan di sini
 * melainkan di tabel anak {@code sekolah.pembelian_siswa_detail}
 * ({@code pembelian_siswa_id, produk_id, qty, nominal, total}).
 *
 * <h2>Domain yang terverifikasi dari kode</h2>
 * <p>Kaitan dengan {@link Kantin} bukan dugaan: laporan
 * {@code webapp/report/sekolah/pembayaran/laporan_saldo_rinci.jrxml} menyusun deskripsi mutasi
 * saldo siswa persis dari rangkaian tabel ini —
 * {@code inner join pembelian_siswa b on (a.id = b.siswa_id)}, lalu
 * {@code inner join pembelian_siswa_detail b1 on (b1.pembelian_siswa_id = b.id)}, lalu
 * {@code inner join produk b2 on (b2.id = b1.produk_id)}, dan
 * {@code left join kantin c on (c.id = b.kantin_id)} — menghasilkan kalimat
 * "&lt;nama produk&gt; sebanyak &lt;qty&gt; seharga &lt;nominal&gt; dengan total &lt;total&gt; di
 * &lt;nama kantin&gt;". Jadi entity ini adalah <b>sisi PENGELUARAN tabungan/deposit siswa</b>,
 * sejajar dengan {@code PembayaranSiswa} (pengeluaran untuk biaya sekolah) dan berlawanan dengan
 * {@code DepositSiswa} (pemasukan/top-up).
 *
 * <h2>PENTING: entity ini YATIM di lapis Java</h2>
 * <p>Hasil penelusuran seluruh pohon sumber (3 Sep 2026): <b>tidak ada satu pun</b> baris kode Java
 * di luar berkas ini yang menyebut tipe {@code PembelianSiswa}. Tidak ada {@code new
 * PembelianSiswa()}, tidak ada {@code save}/{@code saveOrUpdate}/{@code persist}/{@code merge},
 * tidak ada {@code createCriteria(PembelianSiswa.class)}, tidak ada HQL {@code from
 * PembelianSiswa}, tidak ada kelas {@code Action}/layar ZUL yang menargetkannya, dan tidak ada
 * migrasi tenant ({@code ais.service.tenant.TenantSchemaMigrations*}) yang membuat tabelnya.
 * Kelasnya tetap terdaftar di {@code hibernate.cfg.xml} (baris 2304), sehingga Hibernate tetap
 * memvalidasi/menurunkan DDL-nya, tetapi tidak ada aliran data yang mengisinya. Hal yang sama
 * berlaku bagi kedua kerabatnya: {@link Kantin} dan {@link UploadTransaksiPembelianSiswa} juga
 * nol referensi di luar berkas modelnya sendiri.
 *
 * <h2>Jebakan penamaan — jangan tertukar dengan {@code inventory.Pembelian}</h2>
 * <p>Fitur "belanja siswa" yang benar-benar HIDUP memakai entity lain, yaitu
 * {@code ais.database.model.inventory.Pembelian} (tabel {@code koperasi.pembelian}, kolom
 * {@code waktu}/{@code harga_jual}), bukan entity ini ({@code tanggal}/{@code nominal}). Deretan
 * nama yang menyesatkan:</p>
 * <ul>
 *   <li>Menu <b>"Belanja Siswa"</b> ({@code MenuInitializer} baris 120-121) membuka
 *       {@code /common/mobile/pembelian.zul}, yang meng-{@code apply}
 *       {@code ais.action.master.inventory.PembelianAction} &rarr; {@code inventory.Pembelian}.</li>
 *   <li>Menu <b>"Laporan Belanja Siswa"</b> (3 varian paket menu) memakai
 *       {@code ais.action.report.format1.sekolah.LaporanPembelianSiswa}; kelima berkas Jasper yang
 *       dirujuknya ({@code pembelian_siswa.jrxml}, {@code pembelian_siswa_detail.jrxml},
 *       {@code ..._per_item_biaya}, {@code ..._per_kasir}, {@code ..._per_kelas}) semuanya
 *       {@code from koperasi.pembelian} — <b>bukan</b> {@code sekolah.pembelian_siswa}, meskipun
 *       namanya sama persis dengan tabel entity ini.</li>
 *   <li>Route API <b>{@code "pembelian_siswa"}</b> ({@code ApiRouteRegistry} baris 213 &rarr;
 *       {@code TabunganSiswa.pembelian_siswa}) juga menyimpan ke {@code inventory.Pembelian}.</li>
 * </ul>
 *
 * <h2>Konsekuensi nyata: dua sumber saldo yang tidak pernah bersesuaian</h2>
 * <p>{@code Siswa.hitungSisaDeposit(Date)} — yang dipakai API tabungan/top-up untuk menampilkan
 * sisa saldo ke pengguna — menghitung pengeluaran belanja dari {@code inventory.Pembelian}
 * ({@code sum(hargaJual)} atas kolom {@code waktu}). Sementara itu {@code laporan_saldo_rinci.jrxml}
 * dan {@code laporan_saldo_rekap_plus_minus_per_asrama.jrxml} menghitungnya dari
 * {@code pembelian_siswa} ({@code sum(nominal)} atas kolom {@code tanggal}). Karena tidak ada kode
 * yang mengisi tabel ini, kedua laporan tersebut selalu memperoleh 0 untuk komponen belanja,
 * sehingga saldo versi laporan akan lebih besar daripada saldo versi aplikasi. Keduanya kebetulan
 * tidak terlihat pengguna: tab "Saldo Siswa Rinci" di
 * {@code ais.action.report.format1.sekolah.LaporanSaldoSiswa} sudah <b>dikomentari mati</b> (baris
 * 258-259 dan 285-288), dan {@code laporan_saldo_rekap_plus_minus_per_asrama} tidak punya pemanggil
 * sama sekali. Perbedaan ini dicatat apa adanya, bukan diperbaiki.
 *
 * <h2>Pengelompokan anggota</h2>
 * <ul>
 *   <li><b>Jejak audit warisan {@link GeneralValueObject}</b> — {@link #getOleh()}/
 *       {@link #setOleh(String)}, {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan callback
 *       {@code @PreUpdate}.</li>
 *   <li><b>Identitas</b> — {@link #getId()}/{@link #setId(Long)}.</li>
 *   <li><b>Relasi transaksi</b> — {@link #getSiswa()} (pembeli), {@link #getKantin()} (tempat
 *       belanja), {@link #getUploadTransaksiPembelianSiswa()} (berkas unggahan asal data bila
 *       transaksi diimpor massal), {@link #getPostingTransaksi()} (cap jurnal akuntansi).</li>
 *   <li><b>Relasi cakupan (didenormalisasi dari siswa)</b> — {@link #getSekolah()} dan
 *       {@link #getYayasan()}; keduanya getter yang MENULIS BALIK, lihat di bawah.</li>
 *   <li><b>Nilai transaksi</b> — {@link #getNominal()}, {@link #getTanggal()},
 *       {@link #getTanggalBayar()}.</li>
 * </ul>
 *
 * <h2>Kuirk dan hal non-obvious</h2>
 * <ol>
 *   <li><b>Rantai getter destruktif dua tingkat.</b> {@link #getSekolah()} menimpa field
 *       {@code sekolah} dari {@code getSiswa().getSekolah()} setiap kali dibaca, dan
 *       {@link #getYayasan()} menimpa field {@code yayasan} dari {@code getSekolah().getYayasan()}
 *       — yang berarti membaca {@code yayasan} memicu ketiga getter berantai. Karena pemetaan
 *       Hibernate di sini berbasis <i>property access</i> (anotasi ada di getter) dan kelas memakai
 *       {@code dynamicUpdate}, nilai hasil timpaan itulah yang ikut tertulis ke kolom
 *       {@code sekolah_id}/{@code yayasan_id} pada flush berikutnya. Efeknya: baris belanja lama
 *       TIDAK dapat mempertahankan sekolah/yayasan historisnya — bila siswa pindah sekolah, seluruh
 *       riwayat belanjanya ikut berpindah cakupan pada pembacaan+penyimpanan berikutnya.</li>
 *   <li><b>{@link #getNominal()} rawan {@code NullPointerException}.</b> Field-nya bertipe
 *       {@code Double} (boleh {@code null}) tetapi getter-nya mengembalikan {@code double}
 *       primitif. Setiap pembacaan pada instance yang {@code nominal}-nya belum pernah diisi —
 *       termasuk object baru dari {@link #PembelianSiswa()} sebelum {@link #setNominal(double)}
 *       dipanggil, dan setiap pembacaan Hibernate saat dirty-checking object semacam itu — akan
 *       melempar NPE saat unboxing.</li>
 *   <li><b>Presisi kolom {@code nominal} tidak masuk akal untuk uang</b>: {@code precision = 17,
 *       scale = 17} berarti seluruh 17 digit berada di belakang koma, sehingga nilai bulat rupiah
 *       tidak muat. Pada basis data yang sudah ada hal ini tidak terasa karena DDL biasanya sudah
 *       terlanjur dibuat sebagai {@code double precision}; ia baru menggigit bila skema diturunkan
 *       ulang dari anotasi.</li>
 *   <li><b>{@link #getPostingTransaksi()} yatim total.</b> Properti bernama {@code postingTransaksi}
 *       hanya ada di berkas ini di seluruh pohon sumber, dan nol pemanggil. Tidak ada mesin posting
 *       untuk belanja kantin siswa: keempat kelas posting kantin yang nyata
 *       ({@code PostingPenjualanKantinAction}, {@code PostingHppKantinAction},
 *       {@code PostingTokoKantinAction}, {@code PostingKantinLanjutanHelper}) semuanya bekerja pada
 *       tabel skema {@code koperasi} lewat SQL native. {@link PostingHistory} pun tidak punya
 *       konstanta {@code JENIS_*} untuk transaksi ini. Kolom {@code posting_transaksi_id}
 *       karenanya selamanya {@code NULL}.</li>
 *   <li><b>Tabel detail tanpa entity.</b> {@code sekolah.pembelian_siswa_detail} dirujuk SQL
 *       laporan tetapi tidak punya kelas Java (bandingkan {@code PembayaranSiswa} yang PUNYA
 *       pasangan {@code PembayaranSiswaDetail}). Akibatnya {@code hbm2ddl} tidak akan pernah
 *       membuat tabel itu, dan laporan yang meng-{@code inner join}-nya akan gagal dengan galat SQL
 *       "relation does not exist" pada basis data yang skemanya murni diturunkan dari anotasi.</li>
 *   <li><b>Ketaksimetrisan tipe tanggal.</b> {@link #getTanggal()} adalah
 *       {@link TemporalType#TIMESTAMP} (sampai detik) sedangkan {@link #getTanggalBayar()} hanya
 *       {@link TemporalType#DATE} (tanpa jam). {@code tanggal_bayar} juga tidak dipakai satu pun
 *       laporan — seluruh penyaringan periode memakai {@code tanggal}.</li>
 *   <li><b>Tiga relasi dimuat EAGER.</b> {@link #getKantin()},
 *       {@link #getUploadTransaksiPembelianSiswa()} dan {@link #getPostingTransaksi()} tidak
 *       menyetel {@code fetch = FetchType.LAZY}, jadi memakai default {@code EAGER} milik
 *       {@code @ManyToOne}, diperkuat {@code @Fetch(FetchMode.SELECT)} — setiap pemuatan satu baris
 *       belanja menembakkan tiga SELECT tambahan. Hanya {@code siswa}/{@code sekolah}/{@code yayasan}
 *       yang LAZY.</li>
 *   <li><b>Setter cakupan menolak object transient.</b> {@link #setSekolah(Sekolah)} dan
 *       {@link #setYayasan(Yayasan)} mengubah argumen ber-{@code id} {@code null} menjadi
 *       {@code null} diam-diam. Untuk {@code sekolah_id} yang dipetakan {@code nullable = false}
 *       ini berarti penyimpanan gagal dengan {@code PropertyValueException} alih-alih pesan
 *       validasi; dalam praktik jarang terjadi karena {@link #getSekolah()} selalu memulihkan
 *       nilainya dari siswa.</li>
 *   <li><b>Tidak ada {@code toString()} dan tidak ada field {@code keterangan}</b> — object ini
 *       memakai {@code toString()} bawaan {@link GeneralValueObject}. Pola berulang
 *       "{@code getKeterangan()} membalik kontrak kelas dasar" yang sering ditemui di keluarga
 *       entity lain <b>TIDAK berlaku</b> di sini karena properti tersebut memang tidak
 *       di-{@code override}. Deskripsi transaksi dibentuk di SQL laporan, bukan di Java.</li>
 *   <li><b>Catatan keamanan.</b> Sebagai entity terdaftar Hibernate, kelas ini terjangkau lewat
 *       servlet generik {@code ais.action.servlet.Data} yang me-{@code Class.forName} nama kelas
 *       dari payload dan masih meloloskan aksi BACA anonim bila klien mengirim
 *       {@code tanpaLogin=true}. Ini bukan temuan baru melainkan penguat masalah yang sudah
 *       tercatat pada audit endpoint {@code /Data}; risiko datanya kebetulan nihil selama tabel ini
 *       tetap kosong, tetapi akan menjadi kebocoran kebiasaan belanja anak di bawah umur bila
 *       modul ini suatu saat dihidupkan kembali.</li>
 * </ol>
 *
 * <h2>Catatan warisan {@link GeneralValueObject}</h2>
 * <p>Kelas dasar {@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass} — ia POJO abstrak biasa, sehingga Hibernate tidak memetakan properti
 * miliknya. Karena itu deklarasi ulang {@code id}, {@code oleh}, {@code olehId} dan
 * {@code tanggal_dirubah} di berkas ini <b>bukan duplikasi keliru</b>, melainkan keharusan teknis
 * agar kolom-kolom tersebut ikut terpetakan. Menghapusnya akan menghilangkan kolom dari tabel.
 * Sebaliknya, utilitas seperti {@link GeneralValueObject#check(Object)} tetap diwarisi dan dipakai
 * di seluruh getter relasi di bawah untuk meresolusi proxy lazy sebelum nilainya dikembalikan.</p>
 *
 * @see GeneralValueObject
 * @see Kantin
 * @see Siswa
 * @see UploadTransaksiPembelianSiswa
 * @see PostingHistory
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "pembelian_siswa", schema = "sekolah")
public class PembelianSiswa extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java, diwarisi dari kontrak {@link java.io.Serializable} milik
	 * {@link GeneralValueObject}.
	 *
	 * <p>Nilainya dibangkitkan sekali oleh perkakas dan harus dipertahankan apa adanya: object
	 * entity ikut terserialisasi ketika ZK menyimpan state desktop/sesi ke disk atau ketika baris
	 * ini masuk cache tingkat kedua. Mengubah angka ini membuat state lama tidak dapat dibaca
	 * kembali ({@code InvalidClassException}).</p>
	 */
	private static final long serialVersionUID = 6891285159811184674L;

	/** Kunci utama baris, dibangkitkan basis data. Lihat {@link #getId()}. */
	private Long id;

	/**
	 * Nama pengguna terakhir yang mengubah baris ini; diisi otomatis oleh
	 * {@code AuditTimestampInterceptor} lewat callback {@code @PreUpdate}. Lihat
	 * {@link #getOleh()}.
	 */
	private String oleh;

	/**
	 * Identitas (user id) pengguna terakhir yang mengubah baris ini, pendamping {@link #oleh}.
	 * Lihat {@link #getOlehId()}.
	 */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna terakhir yang mengubah baris ini.
	 *
	 * @return user id pengubah terakhir, atau {@code null} bila baris belum pernah diperbarui
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan identitas pengguna terakhir yang mengubah baris ini.
	 *
	 * <p><b>Non-obvious:</b> nilai {@code null} atau string kosong (setelah {@code trim})
	 * <b>diabaikan diam-diam</b> — method langsung {@code return} tanpa menyentuh field. Jadi nilai
	 * lama tidak pernah bisa dikosongkan lewat setter ini, dan pemanggil yang mengira berhasil
	 * menghapus jejak audit akan keliru.</p>
	 *
	 * @param olehId user id pengubah; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Perilaku identik {@link #setOlehId(String)}: nilai {@code null}/kosong diabaikan diam-diam
	 * sehingga jejak audit bersifat "hanya tambah/timpa", tidak pernah bisa dikosongkan.</p>
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Dua anggota yang oleh perkakas pembangkit ditulis pada SATU baris fisik; dokumentasi
	 * keduanya digabung di sini agar susunan baris berkas tidak berubah.
	 *
	 * <p><b>1. {@code onUpdate()} — callback JPA {@code @PreUpdate}.</b> Dipanggil kontainer
	 * persistence tepat sebelum pernyataan {@code UPDATE} dikirim ke basis data, lalu
	 * mendelegasikan ke {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang
	 * mengisi {@link #oleh}/{@link #olehId} dari pengguna sesi aktif dan menyegarkan
	 * {@link #tanggal_dirubah}. Tidak ada {@code @PrePersist} berpasangan, sehingga baris BARU
	 * tidak mendapat pengisian otomatis apa pun — ia hanya mengandalkan nilai awal field
	 * {@code tanggal_dirubah} di bawah.</p>
	 *
	 * <p><b>2. Field {@code tanggal_dirubah}.</b> Stempel waktu perubahan terakhir, diinisialisasi
	 * saat object dibuat memakai {@code ais.ui.util.WaktuUtil.getDate()} (jam server aplikasi,
	 * bukan jam basis data). Lihat {@link #getTanggal_dirubah()}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Normalnya tidak perlu dipanggil kode aplikasi: {@code AuditTimestampInterceptor} sudah
	 * memperbaruinya otomatis lewat callback {@code @PreUpdate}. Nilai {@code null} diterima apa
	 * adanya (tidak ada penolakan seperti pada {@link #setOleh(String)}).</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Dipetakan sebagai {@link TemporalType#TIMESTAMP} ke kolom bernama sama
	 * ({@code tanggal_dirubah}) karena tidak ada {@code @Column} yang menimpanya.</p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada object yang baru dibuat
	 *         karena field-nya diinisialisasi ke waktu sekarang
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Kantin sekolah tempat belanja ini terjadi. Wajib terisi ({@code kantin_id nullable = false}).
	 * Lihat {@link #getKantin()}.
	 */
	private Kantin kantin;

	/**
	 * Cap jurnal akuntansi hasil posting transaksi ini. <b>Yatim</b>: tidak ada mesin posting yang
	 * pernah mengisinya. Lihat {@link #getPostingTransaksi()}.
	 */
	private PostingHistory postingTransaksi;

	/**
	 * Sekolah pemilik transaksi, hasil denormalisasi dari {@link #siswa}. Lihat
	 * {@link #getSekolah()} — getter-nya menulis balik field ini.
	 */
	private Sekolah sekolah;

	/** Siswa pembeli. Wajib terisi ({@code siswa_id nullable = false}). Lihat {@link #getSiswa()}. */
	private Siswa siswa;

	/**
	 * Berkas unggahan massal asal baris ini, bila transaksi tidak diinput satu per satu. Boleh
	 * {@code null} untuk transaksi yang direkam langsung. Lihat
	 * {@link #getUploadTransaksiPembelianSiswa()}.
	 */
	private UploadTransaksiPembelianSiswa uploadTransaksiPembelianSiswa;

	/**
	 * Yayasan pemilik transaksi, hasil denormalisasi berantai dari {@link #sekolah}. Lihat
	 * {@link #getYayasan()} — getter-nya menulis balik field ini.
	 */
	private Yayasan yayasan;

	/**
	 * Total rupiah satu struk belanja. Bertipe pembungkus {@code Double} walau getter-nya
	 * mengembalikan primitif — sumber NPE yang dijelaskan di {@link #getNominal()}.
	 */
	private Double nominal;

	/**
	 * Waktu terjadinya transaksi belanja; inilah kolom yang dipakai seluruh penyaringan periode di
	 * laporan saldo. Lihat {@link #getTanggal()}.
	 */
	private Date tanggal;

	/**
	 * Tanggal pelunasan belanja (bila belanja dicatat sebagai utang lebih dulu). Tidak dipakai
	 * pembaca mana pun. Lihat {@link #getTanggalBayar()}.
	 */
	private Date tanggalBayar;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate untuk membuat instance saat memuat baris
	 * dari basis data.
	 *
	 * <p>Tidak ada konstruktor berargumen di kelas ini (berbeda dari {@link Kantin} dan
	 * {@link UploadTransaksiPembelianSiswa} yang punya varian ringkas). Object hasil konstruktor
	 * ini punya {@link #nominal} bernilai {@code null}, sehingga memanggil {@link #getNominal()}
	 * sebelum {@link #setNominal(double)} akan melempar {@code NullPointerException}.</p>
	 */
	public PembelianSiswa() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Dibangkitkan basis data ({@code IDENTITY}) dan dipetakan {@code insertable = false},
	 * artinya nilai yang di-{@link #setId(Long)} secara manual TIDAK akan ikut dikirim pada
	 * {@code INSERT} — urutan/sequence basis data selalu menang.</p>
	 *
	 * @return id baris, atau {@code null} bila object belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris ini.
	 *
	 * <p>Dipakai Hibernate saat memuat baris. Pemanggilan manual pada object baru tidak berpengaruh
	 * pada {@code INSERT} (lihat {@link #getId()}), tetapi tetap menentukan baris mana yang
	 * di-{@code UPDATE} bila object dianggap sudah persisten.</p>
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kantin tempat belanja ini terjadi.
	 *
	 * <p>Getter murni (tidak menulis balik apa pun) dan satu-satunya getter relasi di kelas ini
	 * yang TIDAK memanggil {@link GeneralValueObject#check(Object)} — sehingga pada object
	 * <i>detached</i> nilainya bisa berupa proxy yang belum terinisialisasi dan memicu
	 * {@code LazyInitializationException} saat propertinya diakses. Dalam praktik jarang menggigit
	 * karena relasi ini dipetakan EAGER (tanpa {@code fetch = LAZY}) dengan
	 * {@code @Fetch(FetchMode.SELECT)}, jadi Hibernate langsung menembakkan SELECT terpisah saat
	 * baris belanja dimuat.</p>
	 *
	 * <p>Kolomnya, {@code kantin_id}, adalah kolom yang di-{@code left join} laporan
	 * {@code laporan_saldo_rinci.jrxml} ke tabel {@code kantin} untuk memunculkan nama kantin pada
	 * deskripsi mutasi saldo.</p>
	 *
	 * @return kantin tempat belanja; menurut pemetaan tidak boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kantin_id", nullable = false)
	public Kantin getKantin() {
		return this.kantin;
	}

	/**
	 * Menyetel kantin tempat belanja ini terjadi.
	 *
	 * <p>Berbeda dari {@link #setSekolah(Sekolah)}/{@link #setYayasan(Yayasan)}, setter ini
	 * menerima argumen apa adanya termasuk object transient (ber-{@code id} {@code null}); dengan
	 * {@code cascade = PERSIST, MERGE} kantin baru akan ikut tersimpan bersama baris belanja.</p>
	 *
	 * @param kantin kantin tempat belanja
	 */
	public void setKantin(Kantin kantin) {
		this.kantin = kantin;
	}

	/**
	 * Mengembalikan sekolah pemilik transaksi ini — <b>getter yang MENULIS (destruktif)</b>.
	 *
	 * <p>Alurnya: memanggil {@link #getSiswa()} (menyimpan hasilnya kembali ke field
	 * {@link #siswa}), dan bila siswa ada, <b>menimpa</b> field {@link #sekolah} dengan
	 * {@code siswa.getSekolah()}; nilai yang tersimpan di kolom {@code sekolah_id} diabaikan sama
	 * sekali. Terakhir hasilnya dilewatkan {@link GeneralValueObject#check(Object)} untuk
	 * meresolusi proxy lazy.</p>
	 *
	 * <p><b>Efek samping yang harus disadari:</b> karena pemetaan berbasis <i>property access</i>
	 * dan kelas ini memakai {@code dynamicUpdate}, nilai hasil timpaan itulah yang dibaca
	 * dirty-checking Hibernate dan ikut tertulis ke basis data pada flush berikutnya. Baris belanja
	 * lama karenanya tidak dapat mempertahankan sekolah historisnya: sekali siswa pindah sekolah,
	 * seluruh riwayat belanjanya berpindah cakupan begitu baris-baris itu dibaca lalu disimpan.
	 * Ini juga membuat {@link #setSekolah(Sekolah)} praktis tidak berpengaruh selama {@link #siswa}
	 * terisi.</p>
	 *
	 * @return sekolah pemilik transaksi, diturunkan dari siswa pembeli; {@code null} hanya bila
	 *         siswa belum diisi dan kolomnya memang kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id", nullable = false)
	public Sekolah getSekolah() {
		siswa = getSiswa();
		if (siswa != null) {
			sekolah = siswa.getSekolah();
		}
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik transaksi.
	 *
	 * <p><b>Non-obvious:</b> argumen {@code null} <i>atau</i> object yang {@code getId()}-nya
	 * masih {@code null} (belum tersimpan) sama-sama disimpan sebagai {@code null} — setter ini
	 * menolak object transient diam-diam. Karena kolom {@code sekolah_id} dipetakan
	 * {@code nullable = false}, kondisi itu berujung {@code PropertyValueException} saat flush,
	 * bukan pesan validasi yang ramah.</p>
	 *
	 * <p>Nilainya juga mudah tertimpa: {@link #getSekolah()} akan menghitung ulang dari siswa pada
	 * pembacaan berikutnya.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau object transient disimpan sebagai
	 *                {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan siswa yang berbelanja, dengan proxy lazy sudah diresolusi.
	 *
	 * <p>Memanggil {@link GeneralValueObject#check(Object)} lalu <b>menulis balik hasilnya ke
	 * field</b> {@link #siswa}. Penulisan balik ini bersifat jinak — nilainya tetap entity yang
	 * sama, hanya berganti dari proxy menjadi object terinisialisasi — sehingga tidak mengubah
	 * kolom {@code siswa_id}.</p>
	 *
	 * <p>Relasi ini adalah poros seluruh kelas: {@link #getSekolah()} dan {@link #getYayasan()}
	 * menurunkan nilainya dari sini, dan seluruh laporan menyaring belanja lewat
	 * {@code siswa_id}.</p>
	 *
	 * @return siswa pembeli; menurut pemetaan tidak boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa_id", nullable = false)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return this.siswa;
	}

	/**
	 * Menyetel siswa yang berbelanja.
	 *
	 * <p>Menerima argumen apa adanya, termasuk {@code null} dan object transient (tidak ada
	 * penyaringan seperti pada {@link #setSekolah(Sekolah)}). Menyetel siswa secara efektif juga
	 * menentukan ulang {@link #getSekolah()} dan {@link #getYayasan()}, karena keduanya diturunkan
	 * dari nilai ini.</p>
	 *
	 * @param siswa siswa pembeli
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Mengembalikan berkas unggahan massal yang menjadi asal baris belanja ini.
	 *
	 * <p>Getter murni tanpa {@link GeneralValueObject#check(Object)}, dimuat EAGER lewat SELECT
	 * terpisah. Relasi ini menandai jalur input alternatif: alih-alih direkam satu per satu dari
	 * mesin kasir, sekumpulan transaksi kantin dapat diunggah sebagai berkas dan setiap baris
	 * hasilnya menunjuk ke satu {@link UploadTransaksiPembelianSiswa} yang menyimpan
	 * {@code keterangan}, {@code pathFile} dan {@code waktu} unggah.</p>
	 *
	 * <p><b>Status nyata:</b> jalur unggah itu tidak pernah diimplementasikan — kelas
	 * {@link UploadTransaksiPembelianSiswa} sendiri nol referensi di seluruh pohon sumber, jadi
	 * kolom {@code upload_transaksi_pembelian_siswa_id} selalu {@code NULL}.</p>
	 *
	 * @return berkas unggahan asal, atau {@code null} bila baris tidak berasal dari unggahan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "upload_transaksi_pembelian_siswa_id", nullable = true)
	public UploadTransaksiPembelianSiswa getUploadTransaksiPembelianSiswa() {
		return this.uploadTransaksiPembelianSiswa;
	}

	/**
	 * Menyetel berkas unggahan massal asal baris belanja ini.
	 *
	 * <p>Dengan {@code cascade = PERSIST, MERGE}, object unggahan yang belum tersimpan akan ikut
	 * disimpan bersama baris belanja pertama yang menunjuknya.</p>
	 *
	 * @param uploadTransaksiPembelianSiswa berkas unggahan asal; {@code null} untuk transaksi yang
	 *                                      direkam langsung
	 */
	public void setUploadTransaksiPembelianSiswa(UploadTransaksiPembelianSiswa uploadTransaksiPembelianSiswa) {
		this.uploadTransaksiPembelianSiswa = uploadTransaksiPembelianSiswa;
	}

	/**
	 * Mengembalikan yayasan pemilik transaksi ini — <b>getter yang MENULIS (destruktif), tingkat
	 * kedua</b>.
	 *
	 * <p>Memanggil {@link #getSekolah()} (yang sendirinya sudah menimpa {@link #sekolah} dari
	 * siswa), lalu bila sekolah ada <b>menimpa</b> field {@link #yayasan} dengan
	 * {@code sekolah.getYayasan()}, dan terakhir meresolusi proxy lewat
	 * {@link GeneralValueObject#check(Object)}. Membaca properti ini karenanya menjalankan rantai
	 * tiga getter — {@code yayasan &larr; sekolah &larr; siswa} — dan berpotensi menembakkan dua
	 * pemuatan lazy sekaligus.</p>
	 *
	 * <p><b>Efek samping identik {@link #getSekolah()}:</b> nilai kolom {@code yayasan_id} yang
	 * tersimpan tidak pernah dipakai dan akan ditimpa pada flush berikutnya. Berbeda dari
	 * {@code sekolah_id}, kolom ini dipetakan boleh {@code null}.</p>
	 *
	 * @return yayasan pemilik transaksi, diturunkan dari sekolah; boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan_id")
	public Yayasan getYayasan() {
		sekolah = getSekolah();
		if (sekolah != null) {
			yayasan = sekolah.getYayasan();
		}
		yayasan = check(yayasan);
		return this.yayasan;
	}

	/**
	 * Menyetel yayasan pemilik transaksi.
	 *
	 * <p>Sama seperti {@link #setSekolah(Sekolah)}: {@code null} maupun object transient
	 * (ber-{@code id} {@code null}) sama-sama disimpan sebagai {@code null}. Nilainya akan ditimpa
	 * ulang oleh {@link #getYayasan()} selama rantai siswa &rarr; sekolah &rarr; yayasan dapat
	 * ditelusuri.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau object transient disimpan sebagai
	 *                {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan total rupiah satu struk belanja.
	 *
	 * <p>Inilah angka yang dijumlahkan laporan saldo sebagai komponen PENGELUARAN tabungan siswa
	 * ({@code select siswa_id, sum(nominal) from pembelian_siswa ...} di
	 * {@code laporan_saldo_rinci.jrxml} dan
	 * {@code laporan_saldo_rekap_plus_minus_per_asrama.jrxml}). Secara konsep nilainya adalah
	 * jumlah kolom {@code total} pada seluruh baris {@code pembelian_siswa_detail} milik struk ini,
	 * tetapi tidak ada kode maupun batasan basis data yang menjaga konsistensi kedua angka
	 * tersebut.</p>
	 *
	 * <p><b>Kuirk — {@code NullPointerException}:</b> field-nya bertipe {@code Double} sedangkan
	 * getter ini mengembalikan {@code double} primitif. Bila {@link #setNominal(double)} belum
	 * pernah dipanggil (mis. object baru dari {@link #PembelianSiswa()}, atau baris lama yang
	 * kolomnya {@code NULL} di basis data), unboxing di {@code return} akan melempar NPE — termasuk
	 * ketika yang memanggil adalah dirty-checking Hibernate, sehingga galatnya muncul jauh dari
	 * kode yang menyebabkannya.</p>
	 *
	 * <p><b>Kuirk — presisi:</b> {@code precision = 17, scale = 17} menempatkan seluruh digit di
	 * belakang koma. Anotasi ini hanya berpengaruh saat skema diturunkan ulang dari kode
	 * ({@code hbm2ddl}); basis data yang sudah ada umumnya memakai {@code double precision}.</p>
	 *
	 * @return total rupiah belanja
	 */
	@Column(name = "nominal", nullable = false, precision = 17, scale = 17)
	public double getNominal() {
		return this.nominal;
	}

	/**
	 * Menyetel total rupiah satu struk belanja.
	 *
	 * <p>Menerima primitif {@code double}, jadi setelah setter ini dipanggil sekali
	 * {@link #getNominal()} dijamin aman dari NPE. Tidak ada validasi tanda: nilai negatif diterima
	 * dan akan mengurangi (bukan menambah) total pengeluaran pada laporan saldo.</p>
	 *
	 * @param nominal total rupiah belanja
	 */
	public void setNominal(double nominal) {
		this.nominal = nominal;
	}

	/**
	 * Mengembalikan waktu terjadinya transaksi belanja.
	 *
	 * <p>Dipetakan {@link TemporalType#TIMESTAMP} sehingga menyimpan jam sampai detik. Inilah kolom
	 * yang dipakai SELURUH penyaringan periode di laporan saldo — baik agregat saldo awal
	 * ({@code where date(aa.tanggal) < date($P{mulai})}) maupun rincian mutasi
	 * ({@code where date(b.tanggal) between date($P{mulai}) and date($P{sampai})}). Karena SQL-nya
	 * membungkus kolom dengan {@code date(...)}, komponen jamnya tidak berpengaruh pada
	 * penyaringan, hanya pada pengurutan tampilan.</p>
	 *
	 * @return waktu transaksi; menurut pemetaan tidak boleh {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal", nullable = false, length = 29)
	public Date getTanggal() {
		return this.tanggal;
	}

	/**
	 * Menyetel waktu terjadinya transaksi belanja.
	 *
	 * <p>Tidak ada nilai bawaan: berbeda dari {@link #tanggal_dirubah} yang otomatis terisi waktu
	 * sekarang, {@code tanggal} tetap {@code null} sampai setter ini dipanggil, dan kolomnya
	 * dipetakan {@code nullable = false}.</p>
	 *
	 * @param tanggal waktu transaksi
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengembalikan tanggal pelunasan belanja.
	 *
	 * <p>Berbeda dari {@link #getTanggal()}, properti ini dipetakan {@link TemporalType#DATE} —
	 * hanya tanggal, tanpa jam. Keberadaannya menyiratkan belanja kantin dapat dicatat sebagai
	 * utang lebih dulu lalu dilunasi belakangan (mis. dipotong dari deposit di akhir periode).</p>
	 *
	 * <p><b>Status nyata:</b> tidak ada satu pun pembaca kolom {@code tanggal_bayar} — kedua
	 * laporan yang menyentuh tabel ini menyaring dan menjumlah memakai {@code tanggal}, bukan
	 * {@code tanggal_bayar}. Kolom ini efektif hanya-tulis.</p>
	 *
	 * @return tanggal pelunasan, atau {@code null} bila belum/tidak dilunasi
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_bayar", length = 13)
	public Date getTanggalBayar() {
		return this.tanggalBayar;
	}

	/**
	 * Menyetel tanggal pelunasan belanja.
	 *
	 * @param tanggalBayar tanggal pelunasan; boleh {@code null}
	 */
	public void setTanggalBayar(Date tanggalBayar) {
		this.tanggalBayar = tanggalBayar;
	}

	/**
	 * Mengembalikan cap jurnal akuntansi hasil posting transaksi belanja ini.
	 *
	 * <p>Pola umum di AIS: entity transaksi menyimpan rujukan ke {@link PostingHistory} sebagai
	 * penanda "sudah dijurnal", dan mesin posting memakai kolom itu untuk memilih baris yang belum
	 * diposting sekaligus untuk membatalkan posting (mengosongkannya kembali).</p>
	 *
	 * <p><b>Di sini pola itu tidak pernah terpasang.</b> Nama properti {@code postingTransaksi}
	 * hanya muncul di berkas ini pada seluruh pohon sumber dan tidak punya satu pun pemanggil.
	 * Tidak ada mesin posting untuk belanja kantin siswa — keempat kelas posting kantin yang nyata
	 * ({@code PostingPenjualanKantinAction}, {@code PostingHppKantinAction},
	 * {@code PostingTokoKantinAction}, {@code PostingKantinLanjutanHelper}) semuanya bekerja pada
	 * tabel skema {@code koperasi} lewat SQL native, dan {@link PostingHistory} pun tidak
	 * mendeklarasikan konstanta {@code JENIS_*} untuk transaksi ini. Kolom
	 * {@code posting_transaksi_id} karenanya selamanya {@code NULL}.</p>
	 *
	 * <p>Perhatikan pula getter ini mengembalikan field langsung tanpa
	 * {@link GeneralValueObject#check(Object)}; sama seperti {@link #getKantin()}, keamanannya
	 * bergantung pada pemuatan EAGER.</p>
	 *
	 * @return cap posting jurnal, atau {@code null} bila belum pernah diposting (kondisi normal)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_transaksi_id", nullable = true)
	public PostingHistory getPostingTransaksi() {
		return postingTransaksi;
	}

	/**
	 * Menyetel cap jurnal akuntansi hasil posting transaksi belanja ini.
	 *
	 * <p>Tidak pernah dipanggil kode mana pun (lihat {@link #getPostingTransaksi()}). Bila kelak
	 * mesin posting untuk belanja kantin siswa dibuat, setter inilah titik pasangnya — dan
	 * pembatalan posting dilakukan dengan memanggilnya memakai {@code null}.</p>
	 *
	 * @param postingTransaksi cap posting jurnal; {@code null} untuk membatalkan posting
	 */
	public void setPostingTransaksi(PostingHistory postingTransaksi) {
		this.postingTransaksi = postingTransaksi;
	}

}
