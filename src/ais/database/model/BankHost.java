package ais.database.model;

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

import org.hibernate.envers.Audited;

/**
 * Entity <b>daftar host bank / payment gateway mitra host-to-host (H2H)</b> — tabel
 * {@code public.bank_host}, {@code @Audited} (Hibernate Envers),
 * {@code dynamicInsert}/{@code dynamicUpdate}. Turunan langsung
 * {@link ais.database.model.GeneralValueObject}.
 *
 * <h3>Peran dalam alur pembayaran host-to-host</h3>
 * <p>Satu baris entity ini mewakili <b>satu mitra bank/agregator pembayaran yang boleh
 * memanggil AIS</b>. Isinya sangat sedikit — nama, alamat IP, keterangan, dan satu
 * {@link JenisPembayaran} baku — tetapi perannya besar: baris inilah yang menjawab dua
 * pertanyaan pada setiap transaksi H2H yang masuk:</p>
 * <ol>
 *   <li><b>"Siapa yang memanggil?"</b> — {@link #getIp()} adalah <i>satu-satunya</i> penanda
 *       identitas pemanggil. Seluruh servlet bank ({@code ais.action.servlet.BCA},
 *       {@code Briva}, {@code Bniresponse}, {@code Bsiresponse}, {@code Mandiri}, {@code Bjb},
 *       {@code Jaring}, {@code JatelindoCallback}, {@code Va}, dan puluhan lainnya) serta
 *       seluruh logika web service ({@code ais.action.ws.logic.InqueryLogic},
 *       {@code PaymentLogic}, {@code ReversalLogic}) mencari baris ini lewat
 *       {@code ais.action.ws.util.PembayaranUtil#getBankHost(String, String)} dengan kunci
 *       alamat IP pemanggil. Lihat bagian <i>Catatan keamanan</i> di bawah — mekanisme ini
 *       jauh lebih longgar daripada kesan "daftar putih IP".</li>
 *   <li><b>"Uang ini masuk sebagai jenis pembayaran apa?"</b> —
 *       {@link #getJenisPembayaran()} menjadi jenis pembayaran baku yang dipakai saat
 *       transaksi dari mitra tersebut dibukukan, kecuali pemanggil menentukan jenis lain
 *       secara eksplisit (pola {@code bankHost == null || bankHost.getJenisPembayaran() ==
 *       null ? <jenis lain> : bankHost.getJenisPembayaran()} yang berulang di hampir semua
 *       servlet bank).</li>
 * </ol>
 *
 * <h3>Hubungan dengan entity lain</h3>
 * <ul>
 *   <li>{@link LogHostToHost} — <b>relasi terpenting</b>. Setiap permintaan H2H (inquiry,
 *       payment, reversal) menghasilkan satu baris log yang menyimpan {@code @ManyToOne} ke
 *       baris ini beserta salinan {@link #getIp()}. Perlu diketahui:
 *       {@code LogHostToHost#getBankHost()} punya <i>fallback</i> sendiri — bila kolom FK-nya
 *       kosong, log akan menelusuri seluruh {@code BankHost} dan mencocokkan
 *       {@code LogHostToHost.getIp()} dengan {@link #getIp()} secara
 *       {@code equalsIgnoreCase}, lalu memasang hasilnya. Artinya tautan log&rarr;bank host
 *       bisa berubah belakangan bila IP sebuah baris {@code BankHost} disunting.</li>
 *   <li>{@link ais.database.model.RekonsiliasiHostToHost} — <b>tahap berikutnya</b> dalam alur
 *       yang sama. Entity ini menentukan siapa yang boleh menembak endpoint dan log apa yang
 *       tercatat; {@code RekonsiliasiHostToHost} kemudian mengurai berkas rekonsiliasi dari
 *       bank, mencocokkannya dengan {@link LogHostToHost} tersebut, dan memindahkan baris
 *       {@link CicilanPembayaran} ke/dari {@link CicilanPembayaranGagal}. Dengan kata lain
 *       {@code BankHost} adalah <i>gerbang masuk</i>, {@code LogHostToHost} adalah
 *       <i>jejaknya</i>, dan {@code RekonsiliasiHostToHost} adalah <i>pencocokan akhirnya</i>.
 *       Karena rekonsiliasi bertumpu pada log, baris log yang tercatat atas nama
 *       {@code BankHost} yang salah akan ikut menyesatkan rekonsiliasi.</li>
 *   <li>{@link VirtualAccountBank} — menyimpan {@code @ManyToOne} ke baris ini untuk menandai
 *       bank penerbit nomor virtual account. {@code ais.action.master.VirtualAccountBankAction}
 *       juga membandingkan {@link #getIp()} dengan nilai konfigurasi untuk memilih
 *       "bank host default" saat menerbitkan VA.</li>
 *   <li>{@link JenisPembayaran} — lihat {@link #getJenisPembayaran()}.</li>
 * </ul>
 *
 * <h3>Layar pengelola</h3>
 * <p>{@code /pages/master/bank_host.zul} dengan controller
 * {@code ais.action.master.BankHostAction}. Layar tersebut termasuk <b>contoh positif</b> dari
 * sisi otorisasi: {@code doAfterCompose()} memanggil {@code Common.doCheckSecurity()} dan
 * menutup layar bila {@code CommonPrivilages.checkPrevilages(CommonPrivilages.READ)} gagal,
 * serta menyembunyikan tombol Tambah/Ubah/Hapus sesuai hak {@code CREATE}/{@code UPDATE}/
 * {@code DELETE}. (Halaman ini sendiri tidak tercantum pada daftar putih {@code MUST_CHECKED}
 * di {@code ais.common.CommonPrivilages}, sehingga proteksinya sepenuhnya bergantung pada
 * pemeriksaan eksplisit di controller — pola yang sudah berulang kali dicatat di seluruh
 * basis kode ini.)</p>
 * <p>Yang tampil di layar hanya {@link #getNama()}, {@link #getIp()},
 * {@link #getKeterangan()}, dan {@link #getJenisPembayaran()}. Kolom
 * {@link #getUsername()}/{@link #getPassword()} <b>sengaja dikomentari</b> di seluruh
 * renderer, form, dan method simpan — lihat {@link #getPassword()}.</p>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 *   <li><b>Jejak audit</b> (deklarasi ulang properti {@code GeneralValueObject}):
 *       {@link #getOleh()}, {@link #getOlehId()}, {@link #getTanggal_dirubah()},
 *       {@link #onUpdate()}.</li>
 *   <li><b>Identitas baris</b>: {@link #getId()}, {@link #getNama()},
 *       {@link #getKeterangan()}, {@link #toString()}.</li>
 *   <li><b>Identitas jaringan / kredensial</b>: {@link #getIp()}, {@link #getUsername()},
 *       {@link #getPassword()}.</li>
 *   <li><b>Relasi</b>: {@link #getJenisPembayaran()}.</li>
 * </ul>
 * <p>Entity ini <b>tidak punya method bisnis maupun query statis sama sekali</b> — seluruh
 * logika pencarian dan penerapannya berada di {@code PembayaranUtil} serta servlet bank
 * masing-masing.</p>
 *
 * <h3>Catatan {@code GeneralValueObject}</h3>
 * <p>{@link ais.database.model.GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass}, melainkan POJO abstrak biasa; Hibernate <b>tidak</b> memetakan
 * properti induknya. Karena itu deklarasi ulang {@code id}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} di kelas ini <b>bukan duplikasi yang keliru, melainkan keharusan
 * teknis</b> agar keempat kolom tersebut benar-benar tersimpan. Konsekuensi lain: properti
 * warisan yang <i>tidak</i> dideklarasikan ulang (mis. {@code kode}, {@code nomorUrut},
 * {@code aktif}) hanya hidup di memori dan tidak pernah masuk basis data.</p>
 *
 * <h3><b>Catatan keamanan — penting</b></h3>
 * <p>Tiga hal berikut membuat "daftar IP mitra" pada entity ini jauh lebih longgar daripada
 * yang terlihat. Semuanya berada di {@code PembayaranUtil}, bukan di kelas ini, tetapi
 * seluruhnya bekerja lewat {@link #getIp()} sehingga wajib diketahui siapa pun yang menyunting
 * baris {@code BankHost}:</p>
 * <ol>
 *   <li><b>IP pemanggil diambil dari header yang dikirim klien.</b>
 *       {@code PembayaranUtil#getBankHost(HttpServletRequest)} lebih mendahulukan header
 *       {@code Cf-Connecting-Ip}, {@code CF-Connecting-IP}, {@code X-Forwarded-For}, lalu
 *       {@code X-Real-IP} daripada {@code request.getRemoteAddr()}, <b>tanpa</b> memeriksa
 *       apakah permintaan benar-benar datang lewat proxy tepercaya. Siapa pun yang bisa
 *       menjangkau endpoint H2H dapat menyisipkan header berisi IP mitra dan langsung dikenali
 *       sebagai mitra tersebut.</li>
 *   <li><b>Baris {@code BankHost} dibuat otomatis untuk IP yang tidak dikenal.</b> Bila
 *       pencarian gagal dan konfigurasi
 *       {@code apabila_bank_host_tidak_ditemukan_buat_data_bank_otomatis} aktif,
 *       {@code PembayaranUtil} <b>menyimpan baris baru</b> bernama {@code "Default Bank"} untuk
 *       IP tersebut lalu melanjutkan proses. Konfigurasi itu <b>bawaannya aktif</b>
 *       ({@code createRowActiveDefault(..., Konfigurasi.AKTIF)} di
 *       {@code KonfigurasiNewAction}, dan {@code Common.bolehKonfigurasi(String)} memakai
 *       {@code AKTIF} sebagai nilai bawaan). Akibatnya daftar ini mengisi dirinya sendiri:
 *       secara bawaan tidak ada IP yang benar-benar ditolak, dan isi tabel {@code bank_host}
 *       lama-kelamaan menjadi catatan "siapa saja yang pernah menembak endpoint", bukan daftar
 *       mitra resmi.</li>
 *   <li><b>Baris ber-IP {@code "0.0.0.0"} berfungsi sebagai wildcard.</b> Bila pencarian tetap
 *       gagal, {@code PembayaranUtil} mencari baris dengan {@code ip = "0.0.0.0"} dan
 *       memakainya sebagai penampung terakhir. Satu baris semacam itu cukup untuk membuat
 *       setiap pemanggil, dari IP mana pun, dianggap mitra yang sah.</li>
 * </ol>
 * <p>Selain itu {@link #getUsername()}/{@link #getPassword()} adalah properti Hibernate
 * terpetakan (akses properti, tanpa {@code @Transient}) berisi <b>teks polos</b> dan ikut
 * disalin ke tabel audit {@code bank_host_AUD} oleh {@code @Audited} — meskipun tidak ada satu
 * baris kode aktif pun yang membacanya. Rinciannya di {@link #getPassword()}.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see LogHostToHost
 * @see ais.database.model.RekonsiliasiHostToHost
 * @see JenisPembayaran
 * @see VirtualAccountBank
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true, 
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "bank_host")
public class BankHost extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya dibangkitkan sekali saat kelas dibuat dan tidak boleh
	 * diubah tanpa alasan, karena {@link GeneralValueObject} {@code Serializable} dan instance
	 * entity ikut tersimpan pada session ZK maupun cache berkas milik kelas induk.
	 */
	private static final long serialVersionUID = 2463821577543439808L;
	/** Kunci primer baris, kolom {@code id}. Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna pengubah terakhir, kolom {@code oleh}. Lihat {@link #getOleh()}. */
	private String oleh;
	/** Identitas (user id) pengubah terakhir, kolom {@code olehid}. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan identitas (user id) pengguna yang terakhir mengubah baris ini.
	 *
	 * @return user id pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel identitas (user id) pengguna pengubah terakhir.
	 *
	 * <p><b>Bukan setter biasa:</b> nilai {@code null} atau yang hanya berisi spasi
	 * <b>diabaikan diam-diam</b> — field lama dipertahankan. Perilaku ini disengaja agar jejak
	 * audit tidak bisa dikosongkan oleh alur yang kebetulan menyetel nilai kosong (mis.
	 * pemrosesan H2H yang berjalan tanpa pengguna login). Konsekuensinya jejak audit
	 * <b>tidak dapat dihapus lewat setter ini</b>.</p>
	 *
	 * @param olehId user id pengubah; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: nilai {@code null}/kosong
	 * <b>diabaikan diam-diam</b> sehingga jejak audit tidak bisa dikosongkan.</p>
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * <p>Untuk baris yang dibuat otomatis oleh {@code PembayaranUtil} saat menerima transaksi
	 * dari IP tak dikenal, nilai ini biasanya kosong — tidak ada pengguna yang terlibat.</p>
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: menyegarkan jejak audit tepat sebelum baris di-{@code UPDATE}.
	 *
	 * <p>Mendelegasikan seluruh pekerjaan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}, yang mengisi
	 * {@link #setOleh(String)}/{@link #setOlehId(String)} dari pengguna yang sedang login dan
	 * memperbarui {@link #setTanggal_dirubah(Date)}. Dipanggil oleh Hibernate, bukan oleh kode
	 * aplikasi.</p>
	 *
	 * <p><b>Perhatikan format baris ini:</b> deklarasi method dan deklarasi field
	 * {@code tanggal_dirubah} ditulis pada <b>satu baris fisik yang sama</b> (pola yang sama di
	 * seluruh entity AIS). Field {@code tanggal_dirubah} diinisialisasi ke waktu sekarang lewat
	 * {@code ais.ui.util.WaktuUtil#getDate()}, sehingga baris yang belum pernah disimpan pun
	 * sudah punya stempel waktu.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir baris ini.
	 *
	 * <p>Tidak ada validasi: berbeda dengan {@link #setOleh(String)}, nilai {@code null}
	 * diterima apa adanya dan akan menghapus stempel waktu.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (kolom {@code tanggal_dirubah},
	 * {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk instance baru karena
	 *         field-nya diinisialisasi ke waktu sekarang saat objek dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini: <b>hanya {@code nama}</b>.
	 *
	 * <p>Sengaja meng-{@code override} {@link GeneralValueObject#toString()} yang berformat
	 * {@code "kode - nama"}, karena entity ini tidak memakai properti {@code kode}. Nilai yang
	 * dikembalikan adalah field {@code nama} secara langsung (bukan lewat {@link #getNama()}),
	 * sehingga <b>tidak</b> ikut di-{@code trim} dan dapat berupa {@code null} — dalam hal itu
	 * ZK maupun perangkaian String akan menampilkan teks {@code "null"}.</p>
	 *
	 * @return nama bank host apa adanya, mungkin {@code null}
	 */
	public String toString() {
		return nama;
	}

	/** Nama mitra bank/gateway, kolom {@code nama} ({@code NOT NULL}). Lihat {@link #getNama()}. */
	private String nama;
	/** Alamat IP mitra — penanda identitas H2H, kolom {@code ip}. Lihat {@link #getIp()}. */
	private String ip;
	/** Nama pengguna mitra (tidak terpakai), kolom {@code username}. Lihat {@link #getUsername()}. */
	private String username;
	/** Kata sandi mitra dalam teks polos (tidak terpakai), kolom {@code password}. Lihat {@link #getPassword()}. */
	private String password;
	/** Keterangan bebas, kolom {@code keterangan}. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Jenis pembayaran baku untuk transaksi dari mitra ini. Lihat {@link #getJenisPembayaran()}. */
	private JenisPembayaran jenisPembayaran;

	/**
	 * Konstruktor tanpa argumen — wajib ada agar Hibernate dapat meng-instansiasi entity.
	 *
	 * <p>Seluruh properti dibiarkan {@code null} kecuali {@code tanggal_dirubah} yang sudah
	 * berisi waktu sekarang lewat inisialisasi field.</p>
	 */
	public BankHost() {
	}

	/**
	 * Mengembalikan kunci primer baris ini.
	 *
	 * <p>Kolom {@code id}, {@code IDENTITY} (di-generate basis data) dan
	 * {@code insertable = false} — nilainya baru terisi setelah baris benar-benar tersimpan.
	 * Selama masih {@code null}, {@link GeneralValueObject#equals(Object)} jatuh ke pembandingan
	 * identitas objek, sehingga dua instance baru yang belum disimpan tidak akan dianggap
	 * sama meski isinya identik.</p>
	 *
	 * @return kunci primer, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci primer baris ini.
	 *
	 * <p>Umumnya hanya dipanggil Hibernate. Menyetelnya secara manual pada entity yang sudah
	 * ter-<i>attach</i> dapat membuat Hibernate menganggapnya baris lain.</p>
	 *
	 * @param id kunci primer
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama mitra bank/gateway, sudah di-{@code trim}.
	 *
	 * <p>Meng-{@code override} {@link GeneralValueObject#getNama()} sehingga nilai yang dipakai
	 * adalah field milik kelas ini (yang benar-benar terpetakan), bukan field {@code nama}
	 * milik kelas induk yang tidak pernah tersimpan. Kolom {@code nama} berstatus
	 * {@code nullable = false}, tetapi entity yang dibuat otomatis oleh {@code PembayaranUtil}
	 * selalu mengisinya dengan literal {@code "Default Bank"} atau {@code "Bank Host"} —
	 * nama-nama itu berasal dari kode, bukan dari bank yang bersangkutan, jadi jangan
	 * diandalkan untuk membedakan mitra.</p>
	 *
	 * <p>Nilai ini juga menjadi kunci urut ketiga {@link GeneralValueObject#compareTo} dan
	 * kolom urut baku pada layar {@code BankHostAction}.</p>
	 *
	 * @return nama mitra tanpa spasi di ujung, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama mitra bank/gateway.
	 *
	 * <p>Nilai disimpan apa adanya — pemangkasan spasi baru dilakukan saat dibaca lewat
	 * {@link #getNama()}, sehingga kolom di basis data dapat berisi spasi di ujung.</p>
	 *
	 * @param nama nama mitra
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas baris ini (kolom {@code keterangan}, boleh {@code null}).
	 *
	 * <p><b>Berbeda dari kelas induk:</b> {@link GeneralValueObject#getKeterangan()} tidak
	 * pernah mengembalikan {@code null} (mengganti dengan {@code ""}), sedangkan versi ini
	 * mengembalikan isi field apa adanya. Karena {@link GeneralValueObject#compareTo} memakai
	 * {@code getKeterangan()} sebagai kunci urut terakhir, entity ini <b>bisa</b> jatuh ke
	 * hasil "setara" ({@code 0}) — kondisi yang pada entity lain tidak pernah terjadi.</p>
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas baris ini.
	 *
	 * @param keterangan keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menyetel alamat IP mitra.
	 *
	 * <p>Tidak ada validasi format maupun pemangkasan spasi, dan <b>tidak ada
	 * {@code unique constraint}</b> pada kolomnya — beberapa baris boleh memiliki IP yang sama.
	 * Bila itu terjadi, {@code PembayaranUtil#getBankHost(String, String)} memakai
	 * {@code setMaxResults(1)} sehingga baris mana yang terpilih tidak deterministik.</p>
	 *
	 * <p><b>Efek samping tidak langsung yang besar:</b> mengubah IP sebuah baris berarti
	 * mengubah identitas mitra untuk seluruh transaksi H2H berikutnya, dan juga dapat mengubah
	 * hasil {@code LogHostToHost#getBankHost()} untuk log <i>lama</i> yang FK-nya kosong
	 * (pencocokan susulan berdasarkan IP).</p>
	 *
	 * @param ip alamat IP mitra; nilai khusus {@code "0.0.0.0"} menjadikan baris ini penampung
	 *           terakhir bagi <b>semua</b> IP yang tidak cocok baris lain
	 */
	public void setIp(String ip) {
		this.ip = ip;
	}

	/**
	 * Mengembalikan alamat IP mitra, sudah di-{@code trim}, dengan {@code null} dipetakan ke
	 * String kosong.
	 *
	 * <p>Inilah <b>satu-satunya penanda identitas</b> mitra H2H di seluruh alur pembayaran.
	 * Dibaca antara lain oleh:</p>
	 * <ul>
	 *   <li>seluruh {@code ais.action.ws.logic.*Logic} dan {@code ais.action.ws.util.DisplayUtil}
	 *       untuk mengisi {@code LogHostToHost#setIp(String)};</li>
	 *   <li>servlet respons bank ({@code Bniresponse}, {@code Briresponse}, {@code Bsiresponse},
	 *       {@code FasPayResponse}, {@code Jaring}, {@code JatelindoCallback}) sebagai
	 *       <i>fallback</i> ketika {@code request.getRemoteAddr()} tidak tersedia — mis. saat
	 *       transaksi diproses ulang di luar konteks HTTP;</li>
	 *   <li>{@code ais.action.master.VirtualAccountBankAction} untuk mencocokkan bank host
	 *       dengan nilai konfigurasi saat menerbitkan virtual account.</li>
	 * </ul>
	 *
	 * <p><b>Kehalusan yang penting:</b> pencarian baris di
	 * {@code PembayaranUtil#getBankHost(String, String)} memakai
	 * {@code Restrictions.eq("ip", ipAdd)} yang menyasar <b>field</b>, bukan getter ini.
	 * Jadi IP yang tersimpan dengan spasi di ujung <b>tidak akan pernah cocok</b>, walaupun
	 * getter ini menampilkannya seolah bersih.</p>
	 *
	 * @return alamat IP mitra tanpa spasi di ujung, atau String kosong bila belum diisi
	 *         (tidak pernah {@code null})
	 */
	public String getIp() {
		return ip == null ? "" : ip.trim();
	}

	/**
	 * Menyetel nama pengguna mitra.
	 *
	 * <p><b>Tidak ada satu pun pemanggil aktif</b> di basis kode — satu-satunya pemanggil
	 * ({@code BankHostAction#onSave}) dikomentari. Lihat {@link #getPassword()}.</p>
	 *
	 * @param username nama pengguna mitra
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Mengembalikan nama pengguna mitra (kolom {@code username}).
	 *
	 * <p>Properti Hibernate terpetakan penuh — entity ini memakai akses properti (anotasi
	 * {@code @Id} berada di getter) dan getter ini tidak diberi {@code @Transient}, sehingga
	 * kolomnya ada, tersimpan, dan ikut disalin ke tabel audit {@code bank_host_AUD}. Namun
	 * <b>tidak dibaca oleh kode aktif mana pun</b>; lihat {@link #getPassword()} untuk
	 * penjelasan lengkap dan implikasi keamanannya.</p>
	 *
	 * @return nama pengguna mitra apa adanya (tanpa {@code trim}), atau {@code null}
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Menyetel kata sandi mitra.
	 *
	 * <p>Nilai disimpan <b>apa adanya</b> — tidak ada hashing, enkripsi, maupun pengaburan.
	 * Satu-satunya pemanggil ({@code BankHostAction#onSave}) dikomentari, jadi tidak ada alur
	 * aktif yang mengisi kolom ini. Lihat {@link #getPassword()}.</p>
	 *
	 * @param password kata sandi mitra dalam teks polos
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Mengembalikan kata sandi mitra (kolom {@code password}) — <b>teks polos, dan properti
	 * yang sudah tidak terpakai</b>.
	 *
	 * <h4>Status pemakaian</h4>
	 * <p>Penelusuran seluruh basis kode menemukan <b>nol</b> pemanggil aktif untuk
	 * {@link #getUsername()}, {@link #getPassword()}, {@link #setUsername(String)}, dan
	 * {@link #setPassword(String)}. Semua kemunculannya di {@code BankHostAction}
	 * (label pada renderer grid, {@code Textbox} pada form, dan baris penyimpanan
	 * {@code bankHost.setPassword(password.getValue())}) berbentuk <b>komentar</b>. Otentikasi
	 * mitra yang benar-benar berjalan sepenuhnya berbasis alamat IP
	 * ({@link #getIp()}), sedangkan kredensial <i>keluar</i> menuju bank diambil dari entity
	 * {@link Konfigurasi} — mis. {@code BSIMajaUtil} yang mengambil {@code Bearer} token lewat
	 * {@code sendRequestToken()} dan {@code maja_BILLING_HOST} dari konfigurasi, bukan dari
	 * baris {@code BankHost}. Dengan kata lain kedua properti ini adalah <b>peninggalan
	 * rancangan lama</b> ("bank login ke AIS memakai username/password") yang digantikan
	 * daftar IP, tetapi kolom dan setter/getternya tidak pernah dibuang.</p>
	 *
	 * <h4>Kenapa tetap perlu diperhatikan</h4>
	 * <p>Tidak terpakai <b>tidak berarti kosong dan tidak berarti aman</b>:</p>
	 * <ul>
	 *   <li>Kolomnya tetap ada dan berisi apa pun yang pernah diisi sebelum kode UI-nya
	 *       dikomentari, atau yang diisi langsung lewat SQL.</li>
	 *   <li>Kelas ini {@code @Audited}, sehingga setiap versi nilai teks polos itu juga
	 *       tersalin ke tabel revisi {@code bank_host_AUD} dan bertahan meski baris aslinya
	 *       diubah atau dihapus.</li>
	 *   <li>Karena keduanya properti Hibernate <b>terpetakan</b>, keduanya terjangkau oleh
	 *       endpoint reflektif generik yang membaca entity apa pun berdasarkan nama kelas dari
	 *       klien — pola yang sudah didokumentasikan di tempat lain pada basis kode ini.
	 *       Layar {@code BankHostAction} boleh saja menyembunyikannya; endpoint reflektif tidak
	 *       peduli kolom mana yang ditampilkan UI.</li>
	 * </ul>
	 * <p>Bila kolom ini memang sudah tidak dipakai, menghapusnya (beserta isi tabel audit)
	 * lebih aman daripada membiarkannya — tetapi keputusan itu di luar cakupan dokumentasi ini
	 * dan tidak boleh dilakukan tanpa memeriksa integrasi luar yang mungkin masih menulis ke
	 * kolom tersebut.</p>
	 *
	 * @return kata sandi mitra dalam teks polos apa adanya, atau {@code null}
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Mengembalikan jenis pembayaran baku untuk transaksi yang masuk dari mitra ini.
	 *
	 * <p>Relasi {@code @ManyToOne} {@code LAZY} ke kolom {@code jenis_pembayaran} (boleh
	 * {@code null}), dengan {@code cascade = PERSIST, MERGE}.</p>
	 *
	 * <p><b>Getter ini menulis balik ke field</b>: {@code jenisPembayaran = check(jenisPembayaran)}.
	 * {@link GeneralValueObject#check(Object)} membuka lapisan proxy Hibernate dan mengganti
	 * referensinya dengan instance <i>canonical</i> dari {@code EntityIdentityMap}/cache kelas
	 * induk, agar akses ke relasi ini tidak meledak dengan
	 * {@code LazyInitializationException} setelah session ditutup. Penulisan balik ini
	 * <b>hanya menyentuh memori</b> — tidak ada {@code UPDATE} ke basis data, tidak ada session
	 * Hibernate yang ditutup, dan nilai lama tidak dihapus. Jadi ini <i>bukan</i> pola
	 * "getter destruktif" maupun "getter yang menulis ke DB" yang ditemukan pada sebagian
	 * entity lain; satu-satunya efek yang bisa teramati adalah identitas objek yang
	 * dikembalikan bisa berbeda dari yang tadinya dipasang lewat
	 * {@link #setJenisPembayaran(JenisPembayaran)}.</p>
	 *
	 * <p><b>Pemakaian:</b> hampir seluruh servlet bank memakai pola
	 * {@code bankHost == null || bankHost.getJenisPembayaran() == null ? <jenis lain> :
	 * bankHost.getJenisPembayaran()} saat membukukan pembayaran, sehingga nilai di sini
	 * menentukan pos jenis pembayaran transaksi H2H. Perlu diketahui:
	 * {@code BankHostAction#init} <b>mengisi properti ini secara otomatis</b> dengan
	 * {@link JenisPembayaran} yang bertanda {@code defaultPembayaran = true} setiap kali form
	 * dibuka untuk baris yang jenis pembayarannya masih kosong — perubahan itu ikut tersimpan
	 * begitu pengguna menekan Simpan, walaupun ia tidak menyentuh kolom tersebut. Baris yang
	 * dibuat otomatis oleh {@code PembayaranUtil} sendiri selalu lahir tanpa jenis pembayaran.</p>
	 *
	 * @return jenis pembayaran baku mitra ini sesudah di-<i>resolve</i> dari proxy, atau
	 *         {@code null} bila belum ditentukan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pembayaran", nullable = true)
	public JenisPembayaran getJenisPembayaran() {
		jenisPembayaran = check(jenisPembayaran);
		return jenisPembayaran;
	}

	/**
	 * Menyetel jenis pembayaran baku untuk mitra ini.
	 *
	 * <p>Karena relasinya {@code cascade = PERSIST, MERGE}, menyimpan {@code BankHost} akan
	 * ikut mem-{@code persist}/{@code merge} objek {@link JenisPembayaran} yang dipasang di
	 * sini bila objek tersebut belum tersimpan.</p>
	 *
	 * @param jenisPembayaran jenis pembayaran baku; boleh {@code null}
	 */
	public void setJenisPembayaran(JenisPembayaran jenisPembayaran) {
		this.jenisPembayaran = jenisPembayaran;
	}

}
