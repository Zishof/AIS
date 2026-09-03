package ais.database.model.sekolah;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GelombangPendaftaran;
import ais.database.model.GeneralValueObject;

/**
 * Master <b>Kelompok Gelombang</b> — pengelompok (bucket) satu tingkat di atas gelombang
 * pendaftaran, dipakai untuk menyatukan beberapa gelombang menjadi satu "kartu penerimaan"
 * pada halaman pengumuman/pendaftaran publik.
 *
 * <h2>Domain (TERVERIFIKASI dari kode, bukan dugaan)</h2>
 * <p>Meski berada di paket {@code ais.database.model.sekolah}, entity ini <b>dipakai bersama
 * oleh DUA modul sekaligus</b> — inilah fakta paling non-obvious tentang kelas ini:</p>
 * <ul>
 *   <li><b>Modul sekolah (PPDB/PSB)</b> — {@code GelombangPendaftaranPsb.getKelompokGelombang()}
 *   memetakan {@code @JoinColumn(name = "kelompok_gelombang")} ke tabel ini.</li>
 *   <li><b>Modul perguruan tinggi (PMB)</b> — {@code ais.database.model.GelombangPendaftaran}
 *   (gelombang penerimaan mahasiswa baru) memetakan FK ke kolom yang sama persis.</li>
 * </ul>
 * <p>Jadi satu baris {@code sekolah.kelompok_gelombang} dapat menaungi campuran gelombang
 * siswa <i>dan</i> gelombang mahasiswa. Konsekuensinya dibahas pada bagian
 * "Hak akses" di bawah.</p>
 *
 * <p>Relasinya bersifat <b>satu arah dari sisi gelombang</b>: tidak ada
 * {@code @OneToMany}/{@code mappedBy} di kelas ini. Dua koleksi yang tampak seperti relasi
 * ({@link #gelombangPendaftaranPsbs} dan {@link #gelombangPendaftarans}) sebenarnya
 * <b>bukan koleksi terpetakan</b> — lihat penjelasan rincinya di bawah, karena inilah sumber
 * beberapa perilaku aneh yang tercatat pada Javadoc kedua field tersebut.</p>
 *
 * <h2>Apa yang ditampilkan ke calon pendaftar</h2>
 * <p>Konsumen utama entity ini adalah {@code ais.action.master.TampilanPengumumanAkademisAction}
 * (halaman pengumuman/pendaftaran). Untuk setiap kelompok yang punya gelombang aktif dan sedang
 * dalam rentang tanggal, halaman itu merender satu {@code Groupbox} berisi:</p>
 * <ul>
 *   <li>{@link #getNama()} sebagai judul kartu;</li>
 *   <li>rentang tanggal pendaftaran = <b>tanggal mulai paling awal</b> dan <b>tanggal selesai
 *   paling akhir</b> dari seluruh gelombang anggota kelompok (dihitung ulang di action, bukan
 *   disimpan di sini);</li>
 *   <li>{@link #getInfo()} sebagai teks informasi (dirender sebagai HTML mentah);</li>
 *   <li>tautan berkas informasi — lampiran ber-{@code ref} = {@link #getId()} dan kategori
 *   {@code "INFO_KELOMPOK_PPDB"} pada {@code ais.database.model.file.LampiranLain}. Berkas
 *   fisiknya <b>tidak</b> disimpan di entity ini, hanya dikaitkan lewat id;</li>
 *   <li>{@link #getKeterangan()} sebagai baris tambahan bila tidak kosong;</li>
 *   <li>tombol "Daftar Sekarang" yang membuka pemilih gelombang
 *   ({@code TampilanPengumumanAkademisAction.sebagaiKelompok(...)} untuk sisi sekolah,
 *   {@code sebagaiKelompokPmb(...)} untuk sisi perguruan tinggi).</li>
 * </ul>
 * <p>Gelombang yang <b>tidak</b> punya kelompok tetap dirender sebagai kartu tersendiri; jadi
 * kelompok murni bersifat opsional dan hanya mengubah cara pengelompokan tampilan, bukan aturan
 * bisnis pendaftaran.</p>
 *
 * <h2>Pengelompokan anggota kelas ini</h2>
 * <ol>
 *   <li><b>Identitas &amp; audit</b> — {@link #serialVersionUID}, {@link #getId()}/{@link #setId(Long)},
 *   {@link #getOleh()}/{@link #setOleh(String)}, {@link #getOlehId()}/{@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 *   <li><b>Atribut master</b> — {@link #getKode()}, {@link #getNama()}, {@link #getInfo()},
 *   {@link #getKeterangan()}, {@link #getAktif()} beserta setter-nya.</li>
 *   <li><b>Bucket in-memory (BUKAN kolom)</b> — {@link #gelombangPendaftaranPsbs},
 *   {@link #gelombangPendaftarans}.</li>
 *   <li><b>Utilitas</b> — {@link #toString()} (di-override, formatnya berbeda dari induk),
 *   constructor default {@link #KelompokGelombang()}.</li>
 * </ol>
 * <p>Kelas ini <b>tidak memiliki method bisnis maupun query statis</b>; seluruh logika
 * pencarian, validasi, dan penyusunan kartu pengumuman berada di action pemanggil.</p>
 *
 * <h2>Catatan tentang {@link GeneralValueObject}</h2>
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} —
 * ia POJO abstrak biasa, sehingga Hibernate <b>tidak</b> memetakan properti milik induk.
 * Karena itu deklarasi ULANG {@code id}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} di kelas ini <b>bukan bug dan bukan duplikasi ceroboh</b>, melainkan
 * <b>keharusan teknis</b> agar keempat kolom tersebut benar-benar dipetakan ke
 * {@code sekolah.kelompok_gelombang}. Hal yang sama berlaku untuk {@code kode}, {@code nama},
 * dan {@code keterangan}.</p>
 * <p>Karena {@code equals()}, {@code compareTo()}, dan {@code toString()} pada induk membaca
 * lewat <b>getter</b> (bukan field), <i>shadowing</i> field di atas tidak merusak identitas
 * maupun pengurutan: {@code equals()} tetap membandingkan {@link #getId()}, dan
 * {@code compareTo()} jatuh ke {@link #getNama()} karena {@code nomorUrut}/{@code nim} tidak
 * pernah diisi untuk entity ini. {@code hashCode()} tetap <b>tidak</b> di-override di seluruh
 * hierarki, jadi jangan pakai {@code HashSet}/{@code HashMap} berkunci entity untuk deduplikasi
 * kelompok — pemanggil yang ada memakai {@code List.contains(...)} (berbasis {@code equals})
 * justru karena alasan ini.</p>
 *
 * <h2>Layar pengelola &amp; hak akses (hasil audit)</h2>
 * <p>Layar CRUD-nya adalah {@code ais.action.master.sekolah.KelompokGelombangAction} di atas
 * {@code /pages/master/sekolah/kelompok_gelombang.zul}.</p>
 * <p><b>Kabar baik:</b> berbeda dari empat instance <i>broken access control</i> keluarga PSB
 * yang sudah terkonfirmasi sebelumnya ({@code RuangPSB},
 * {@code CalonSiswaPunyaVerifikasiParameter},
 * {@code GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa}/{@code InterviewPunyaCalonSiswa},
 * {@code RuangGelombangPendaftaranPsbPSB}), layar ini <b>bukan</b> kasus "nol
 * {@code checkPrevilages}". Ia memanggil {@code Common.doCheckSecurity()} pada
 * {@code doBeforeCompose} (wajib login) dan menggerbangi tombol Tambah dengan
 * {@code CommonPrivilages.CREATE}, kolom Ubah/Hapus dengan {@code UPDATE}/{@code DELETE},
 * checkbox Aktif dengan {@code setDisabled(!edit)}, serta tombol unggah massal dengan
 * kombinasi ketiganya. Tidak ditemukan pula pola <i>getter write-back</i>/destruktif di berkas
 * ini: seluruh getter di sini hanya membaca dan menormalkan nilai, tidak ada satu pun yang
 * menulis balik ke field selain normalisasi lokal.</p>
 * <p><b>Kabar buruk — "pewarisan hak lewat menu induk":</b> {@code kelompok_gelombang.zul}
 * <b>tidak terdaftar sebagai menu mana pun</b> ({@code MenuSnapshotData}/{@code MenuInitializer}
 * nol entri). Layar ini hanya bisa dibuka sebagai <i>tab</i>/{@code MyInclude} dari dua layar
 * lain: {@code GelombangPendaftaranPsbAction.onKelompok(...)} (menu 5702 "Gelombang Pendaftaran"
 * modul sekolah) dan {@code GelombangPendaftaranAction.onKelompok(...)} (menu 22017x "Gelombang
 * Pendaftaran" modul PMB). Karena {@code CommonPrivilages.checkPrevilages(...)} menguji
 * {@code Common.getCurrentMenu()} — yaitu menu <b>induk</b>, bukan layar yang sesungguhnya
 * dirender — hak CREATE/UPDATE/DELETE atas master ini sebenarnya adalah hak menu Gelombang
 * Pendaftaran. Ini instance ke-10 pola yang sama (lihat {@code PaketPsb},
 * {@code KategoriItemPenilaianSiswa}, {@code SubMatapelajaran}, keluarga b52, dan
 * {@code GrupChecklistPenilaianGuru}).</p>
 * <p><b>Varian baru pada instance ini: kebocoran hak LINTAS MODUL.</b> Karena tabelnya global
 * (tidak ada kolom {@code sekolah}/{@code yayasan} sama sekali — lihat di bawah) dan
 * pemberi haknya ada DUA di modul berbeda, operator yang hanya diberi wewenang atas gelombang
 * <b>PMB perguruan tinggi</b> otomatis memperoleh CRUD penuh atas master yang menggerakkan
 * kartu pengumuman <b>PPDB sekolah</b> milik seluruh instalasi — dan sebaliknya. Tidak ada
 * pemisahan tenant maupun modul di jalur mana pun.</p>
 * <p><b>Cakupan tenant:</b> ini <i>bukan</i> kasus <i>fail-open</i> — memang <b>tidak ada
 * filter tenant sama sekali</b>, karena entity ini tidak punya kolom pemilik.
 * {@code KelompokGelombangAction.initCriteria(...)} hanya menyaring {@code nama} dan
 * {@code aktif}. Klasifikasinya sama dengan {@code RuangPSB} (b48): katalog global yang
 * di-CRUD lintas sekolah/yayasan. Sensitivitas datanya rendah (label + teks informasi publik,
 * tanpa PII), sehingga dampak utamanya adalah integritas tampilan PPDB/PMB, bukan kebocoran
 * data pribadi. Tombol "Download" ({@code Common.cetakData}) memang tidak digerbangi
 * privilese, tetapi hanya mengekspor enam kolom label ({@code id}, {@code kode}, {@code nama},
 * {@code info}, {@code keterangan}, {@code aktif}) sehingga tidak menaikkan severity.</p>
 *
 * <h2>Kuirk yang perlu diketahui sebelum menyunting</h2>
 * <ul>
 *   <li><b>Kelompok baru tidak pernah muncul di combobox.</b>
 *   {@code KelompokGelombangAction.onSave(...)} menulis {@code kode}, {@code nama},
 *   {@code info}, dan {@code keterangan} — tetapi <b>tidak pernah</b> menulis {@code aktif},
 *   sehingga baris baru tersimpan dengan {@code aktif = NULL}. Layar master sendiri toleran
 *   NULL ({@code Restrictions.or(isNull("aktif"), eq("aktif", true))}) dan {@link #getAktif()}
 *   mengembalikan {@code true} untuk NULL, jadi barisnya tampak normal dan tercentang. Namun
 *   kedua layar konsumen memakai {@code Restrictions.eq("aktif", true)} yang <b>ketat</b>,
 *   sehingga kelompok yang baru dibuat tidak pernah bisa dipilih dari gelombang mana pun
 *   sampai admin menekan checkbox Aktif dua kali (mati lalu hidup, menulis {@code false} lalu
 *   {@code true}). Divergensi getter-toleran vs SQL-ketat ini adalah instance ke-5 pola
 *   "kolom aktif tak pernah ditulis layar master".</li>
 *   <li><b>Nama wajib unik GLOBAL.</b> {@code checkNamaKelompokGelombang()} menghitung baris
 *   ber-{@code nama} sama tanpa batasan tenant apa pun, jadi instalasi multi-sekolah tidak bisa
 *   punya dua kelompok bernama "Reguler". Kembar persis temuan pada {@code PaketPsb}.</li>
 *   <li><b>{@link #toString()} sengaja berbeda dari induk</b> ({@code id + "-" + nama}, bukan
 *   {@code "kode - nama"}). Jangan disamakan tanpa menelusuri pemakaian label/debug.</li>
 *   <li><b>Instance kelas ini di-cache global.</b> {@code ais.common.InitData} mendaftarkan
 *   {@code KelompokGelombang.class} ke pemuat cache in-memory, sehingga satu object Java
 *   dipakai bersama seluruh sesi JVM. Segala state yang ditulis ke field publik entity ini
 *   otomatis menjadi state bersama lintas pengguna — dasar dari peringatan pada
 *   {@link #gelombangPendaftaranPsbs}.</li>
 * </ul>
 *
 * @see GeneralValueObject
 * @see GelombangPendaftaranPsb
 * @see GelombangPendaftaran
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "kelompok_gelombang")
public class KelompokGelombang extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Entity AIS kerap diserialkan (cache in-memory, replikasi
	 * sesi ZK), jadi nilai ini harus tetap stabil selama bentuk field tidak berubah tak
	 * kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key. Dideklarasikan ulang karena induk tidak dipetakan Hibernate; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini. Lihat {@link #setOleh(String)}. */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris ini. Lihat {@link #setOlehId(String)}. */
	private String olehId;

	/**
	 * Mengembalikan ID pengguna yang terakhir mengubah baris ini.
	 *
	 * @return ID pengguna terakhir, atau {@code null} bila jejak audit belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna yang terakhir mengubah baris ini.
	 *
	 * <p><b>Perilaku non-obvious:</b> setter ini <b>menolak diam-diam</b> nilai {@code null}
	 * maupun string kosong/spasi — nilai lama dipertahankan, tidak ada exception dan tidak ada
	 * log. Ini disengaja agar jejak audit yang sudah terisi tidak terhapus oleh jalur simpan
	 * yang kebetulan tidak membawa konteks pengguna (mis. tugas terjadwal atau job impor).
	 * Akibatnya: jejak audit <b>tidak dapat dikosongkan kembali</b> lewat setter ini.</p>
	 *
	 * @param olehId ID pengguna; diabaikan bila {@code null} atau hanya berisi spasi
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna yang terakhir mengubah baris ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: nilai {@code null}/kosong <b>diabaikan
	 * diam-diam</b> sehingga jejak audit lama tetap utuh.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau hanya berisi spasi
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna terakhir, atau {@code null} bila jejak audit belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mencatat stempel waktu/pelaku perubahan tepat sebelum
	 * Hibernate menjalankan {@code UPDATE} atas baris ini.
	 *
	 * <p>Mendelegasikan ke {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)},
	 * yang mengisi {@link #setTanggal_dirubah(Date)} beserta {@link #setOleh(String)}/
	 * {@link #setOlehId(String)} dari konteks pengguna aktif. Karena ini {@code @PreUpdate}
	 * (bukan {@code @PrePersist}), callback <b>tidak</b> berjalan pada INSERT pertama — nilai
	 * awal {@code tanggal_dirubah} justru datang dari inisialisasi field di bawah.</p>
	 *
	 * <p><b>Perhatikan gaya baris:</b> baris kode di bawah memadatkan DUA deklarasi sekaligus —
	 * method {@code onUpdate()} dan field {@code tanggal_dirubah} (diinisialisasi
	 * {@code ais.ui.util.WaktuUtil.getDate()}, yaitu waktu server yang sudah dinormalkan zona
	 * waktu instalasi, bukan {@code new Date()} langsung). Jangan memecah baris ini tanpa
	 * kebutuhan; dokumentasi field-nya ada di {@link #getTanggal_dirubah()}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Umumnya dipanggil otomatis oleh {@link #onUpdate()}; pemanggilan manual hanya masuk akal
	 * pada jalur migrasi/impor data historis.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru (boleh {@code null}, tidak divalidasi)
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini, dipetakan sebagai kolom
	 * {@code TIMESTAMP}.
	 *
	 * <p>Tidak pernah {@code null} untuk object yang baru dibuat di Java, karena field-nya
	 * diinisialisasi {@code WaktuUtil.getDate()} pada deklarasi (lihat {@link #onUpdate()}).
	 * Untuk baris lama hasil migrasi nilainya tetap bisa {@code null}.</p>
	 *
	 * @return stempel waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks entity, <b>di-override dan sengaja berbeda</b> dari
	 * {@link GeneralValueObject#toString()}.
	 *
	 * <p>Induk menghasilkan {@code "kode - nama"}; kelas ini menghasilkan {@code id + "-" + nama}
	 * (tanpa spasi, memakai id numerik). Untuk baris yang belum tersimpan hasilnya berbentuk
	 * {@code "null-<nama>"}. Bentuk ini dipakai untuk keperluan debug/log; label yang tampil ke
	 * pengguna pada combobox dan kartu pengumuman diambil dari {@link #getNama()}, bukan dari
	 * method ini.</p>
	 *
	 * @return gabungan {@code id} dan {@code nama} dipisah tanda hubung
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode ringkas kelompok. Lihat {@link #getKode()}. */
	private String kode;
	/** Teks informasi yang ditampilkan ke calon pendaftar. Lihat {@link #getInfo()}. */
	private String info;
	/** Nama kelompok; wajib diisi dan unik global. Lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Saklar aktif; lihat peringatan penting pada {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Bucket <b>in-memory</b> berisi gelombang PPDB sekolah anggota kelompok ini.
	 *
	 * <p><b>BUKAN kolom dan BUKAN relasi Hibernate.</b> Karena {@code @Id} berada pada getter,
	 * Hibernate memakai <i>property access</i>; field publik tanpa getter seperti ini tidak
	 * pernah dipindai, dipetakan, dimuat, maupun disimpan. Tidak ada {@code @OneToMany},
	 * {@code @Transient}, maupun {@code mappedBy} di mana pun — arah relasi yang sesungguhnya
	 * ada di {@code GelombangPendaftaranPsb.getKelompokGelombang()}. Isi list ini
	 * <b>selalu kosong</b> untuk object yang baru dimuat dari database.</p>
	 *
	 * <p><b>Siapa yang mengisinya.</b> Hanya
	 * {@code ais.action.master.TampilanPengumumanAkademisAction} saat merender halaman
	 * pengumuman/pendaftaran: setiap gelombang PSB yang lolos saringan (aktif, tanggal berjalan,
	 * cocok yayasan/sekolah pengguna, cocok aturan "hanya untuk anak pegawai") ditambahkan ke
	 * sini bila belum ada ({@code contains}, berbasis {@code equals} atas {@code id}). Nilainya
	 * lalu dipakai untuk (a) menghitung rentang tanggal kartu kelompok, dan (b) mengisi combobox
	 * "Gelombang" pada dialog {@code sebagaiKelompok(...)} setelah tombol "Daftar Sekarang"
	 * ditekan.</p>
	 *
	 * <p><b>PERINGATAN — state bersama lintas pengguna.</b> Object {@code KelompokGelombang}
	 * yang dipakai action tersebut berasal dari cache in-memory global (lihat {@code InitData}),
	 * jadi list ini hidup di object yang sama untuk <b>semua</b> sesi dan <b>tidak pernah
	 * dikosongkan</b>. Tiga konsekuensi nyata:</p>
	 * <ol>
	 *   <li><b>Akumulasi lintas tenant.</b> Saringan tenant dijalankan saat menambah, tetapi
	 *   hasilnya menumpuk pada object bersama. Setelah pengunjung sekolah A merender halaman,
	 *   gelombang sekolah A tetap berada di list; pengunjung sekolah B yang menekan "Daftar
	 *   Sekarang" pada kelompok yang sama akan melihat gelombang sekolah A ikut terdaftar di
	 *   combobox, karena dialog itu membaca list apa adanya <b>tanpa menyaring ulang</b>.</li>
	 *   <li><b>Saringan "hanya untuk anak pegawai" bisa bocor</b> dengan mekanisme yang sama:
	 *   begitu satu pengguna ber-{@code Pegawai} membuka halaman, gelombang khusus anak pegawai
	 *   masuk ke list bersama dan ikut tampil bagi pendaftar umum.</li>
	 *   <li><b>Tidak aman-thread.</b> {@code ArrayList} ini ditambah dan di-{@code Collections.sort}
	 *   dari event thread ZK mana pun tanpa sinkronisasi.</li>
	 * </ol>
	 * <p>Karena datanya hanya nama gelombang (bukan PII) dampaknya adalah kesalahan tampilan dan
	 * pelemahan pembatasan pendaftaran, bukan kebocoran data pribadi. Perbaikan yang benar
	 * adalah memakai koleksi lokal per-render di action, bukan field pada entity ber-cache.</p>
	 */
	public List<GelombangPendaftaranPsb> gelombangPendaftaranPsbs = new ArrayList<GelombangPendaftaranPsb>();
	/**
	 * Bucket <b>in-memory</b> berisi gelombang PMB perguruan tinggi anggota kelompok ini.
	 *
	 * <p>Pasangan sisi-mahasiswa dari {@link #gelombangPendaftaranPsbs}: sama-sama <b>bukan
	 * kolom, bukan relasi terpetakan</b>, sama-sama hanya diisi
	 * {@code TampilanPengumumanAkademisAction} (cabang {@code sebagaiKelompokPmb(...)}), dan
	 * sama-sama menumpuk pada object yang di-cache global. Seluruh peringatan pada
	 * {@link #gelombangPendaftaranPsbs} berlaku identik di sini.</p>
	 *
	 * <p>Keberadaan dua bucket terpisah pada satu entity adalah bukti paling langsung bahwa
	 * master ini memang dipakai bersama oleh modul sekolah dan modul perguruan tinggi.</p>
	 */
	public List<GelombangPendaftaran> gelombangPendaftarans = new ArrayList<GelombangPendaftaran>();

	/**
	 * Constructor default tanpa argumen.
	 *
	 * <p>Wajib ada untuk Hibernate (hidrasi entity dari hasil query) dan dipakai layar master
	 * saat menekan tombol "Tambah". Seluruh atribut mulai {@code null} kecuali
	 * {@code tanggal_dirubah} dan kedua bucket in-memory yang sudah diinisialisasi pada
	 * deklarasi field.</p>
	 */
	public KelompokGelombang() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * <p>Dipetakan ke kolom {@code id} dengan {@code IDENTITY} (nilai dibangkitkan database,
	 * karena itu {@code insertable = false}). Selain sebagai kunci, nilai ini juga dipakai
	 * sebagai {@code ref} lampiran berkas informasi berkategori {@code "INFO_KELOMPOK_PPDB"}
	 * pada {@code LampiranLain} — konsekuensinya lampiran baru hanya bisa dikaitkan setelah
	 * baris tersimpan dan mendapat id.</p>
	 *
	 * @return primary key, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key baris ini. Tanpa validasi.
	 *
	 * @param id nilai primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode ringkas kelompok, sudah dinormalkan.
	 *
	 * <p><b>Tidak pernah mengembalikan {@code null}</b>: nilai {@code null} maupun string kosong
	 * dikembalikan sebagai {@code ""}, selain itu hasilnya di-{@code trim()}. Kode bersifat
	 * opsional (tidak divalidasi layar master) dan dipakai sebagai keterangan sekunder pada
	 * combobox pemilih kelompok di layar gelombang.</p>
	 *
	 * <p>Perhatikan: tanpa anotasi {@code @Column}, Hibernate memetakannya ke kolom {@code kode}
	 * default. Karena getter menormalkan {@code null} menjadi {@code ""}, menyimpan ulang baris
	 * lama yang kodenya {@code NULL} akan menuliskan string kosong ke database.</p>
	 *
	 * @return kode kelompok yang sudah di-trim, atau {@code ""} bila belum diisi
	 */
	public String getKode() {
		return kode == null || kode.isEmpty() ? "" : kode.trim();
	}

	/**
	 * Menyetel kode ringkas kelompok. Tanpa validasi maupun normalisasi.
	 *
	 * @param kode kode baru (boleh {@code null})
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama kelompok — label utama yang dilihat calon pendaftar.
	 *
	 * <p>Dipetakan {@code nullable = false} sepanjang 255 karakter, dan divalidasi wajib-isi
	 * serta <b>unik secara global</b> oleh layar master (lihat catatan kuirk pada Javadoc
	 * kelas). Berbeda dari {@link #getKode()}/{@link #getKeterangan()}/{@link #getInfo()},
	 * getter ini <b>meneruskan {@code null} apa adanya</b> dan hanya melakukan {@code trim()}
	 * bila nilainya ada — perbedaan kecil yang penting bagi
	 * {@code GeneralValueObject.compareTo(...)}, yang memakai {@code nama} sebagai kunci urut
	 * efektif untuk entity ini.</p>
	 *
	 * @return nama kelompok yang sudah di-trim, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama kelompok. Tanpa validasi; keunikan dan kewajiban isi ditegakkan di layar
	 * master, bukan di sini.
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas kelompok, sudah dinormalkan.
	 *
	 * <p><b>Tidak pernah {@code null}</b> — {@code null} dikembalikan sebagai {@code ""}.
	 * Halaman pengumuman menambahkan satu baris tabel "Keterangan" pada kartu kelompok hanya
	 * bila hasil method ini tidak kosong.</p>
	 *
	 * <p>Efek samping tak langsung: karena {@code GeneralValueObject.compareTo(...)} memakai
	 * {@code keterangan} sebagai kunci urut terakhir dan getter ini tidak pernah {@code null},
	 * cabang tersebut selalu memenuhi syarat. Untuk entity ini hal itu tidak berpengaruh karena
	 * pengurutan sudah berhenti lebih dulu di {@link #getNama()}.</p>
	 *
	 * @return keterangan yang sudah di-trim, atau {@code ""} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan == null ? "" : keterangan.trim();
	}

	/**
	 * Menyetel keterangan bebas kelompok. Tanpa validasi.
	 *
	 * @param keterangan keterangan baru (boleh {@code null})
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif kelompok, dengan <b>NULL diperlakukan sebagai aktif</b>.
	 *
	 * <p><b>Sumber kebingungan operasional yang nyata — baca sebelum mengubah apa pun.</b>
	 * Method ini toleran NULL ({@code null} → {@code true}) dan pencarian layar master juga
	 * toleran NULL ({@code isNull(aktif) OR aktif = true}), sehingga baris ber-{@code aktif}
	 * NULL tampil normal dan checkbox-nya tercentang. Namun kedua layar konsumen
	 * ({@code GelombangPendaftaranPsbAction} dan {@code GelombangPendaftaranAction}) mengisi
	 * combobox "Kelompok Gelombang" dengan {@code Restrictions.eq("aktif", true)} yang
	 * <b>ketat</b> dan tidak cocok dengan NULL.</p>
	 * <p>Karena formulir simpan layar master tidak pernah menuliskan kolom {@code aktif}, setiap
	 * kelompok yang baru dibuat berakhir dengan {@code aktif = NULL} dan <b>tidak pernah muncul
	 * di combobox mana pun</b> — sehingga tidak ada gelombang yang bisa dikaitkan kepadanya —
	 * sampai admin menekan checkbox Aktif dua kali (mati lalu hidup) yang menulis {@code false}
	 * lalu {@code true}. Jangan "memperbaiki" method ini menjadi mengembalikan {@code false}
	 * untuk NULL tanpa lebih dulu melakukan backfill kolom di seluruh instalasi: itu akan
	 * mematikan kelompok-kelompok lama yang selama ini berfungsi.</p>
	 *
	 * @return {@code true} bila kolom bernilai {@code true} atau {@code NULL}; {@code false}
	 *         hanya bila kolom benar-benar berisi {@code false}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif kelompok.
	 *
	 * <p>Satu-satunya pemanggil di UI adalah checkbox "Aktif" pada baris grid layar master
	 * (digerbangi {@code CommonPrivilages.UPDATE}), yang langsung menyimpan perubahan lewat
	 * {@code Common.refreshSaveOrUpdate(...)} pada event {@code onCheck}. Menyetel
	 * {@code false} menyembunyikan kelompok dari combobox pemilih kelompok, tetapi
	 * <b>tidak</b> memutus gelombang yang sudah terlanjur menunjuk kelompok ini — FK-nya tetap
	 * ada dan kartu pengumumannya tetap dirender.</p>
	 *
	 * @param aktif status baru; {@code null} berarti "belum ditentukan" dan dibaca sebagai aktif
	 *              oleh {@link #getAktif()}, tetapi tidak cocok dengan filter combobox konsumen
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan teks informasi kelompok yang ditampilkan ke calon pendaftar, sudah
	 * dinormalkan.
	 *
	 * <p><b>Tidak pernah {@code null}</b> — {@code null} dikembalikan sebagai {@code ""}.
	 * Dipetakan sebagai kolom {@code text} (tanpa batas panjang) karena isinya paragraf, bukan
	 * label.</p>
	 *
	 * <p><b>Catatan keamanan tampilan:</b> nilai ini dirender halaman pengumuman ke dalam
	 * komponen {@code Html} <b>tanpa escaping</b>, digabung langsung ke dalam string tabel HTML.
	 * Jadi markup yang diketik admin akan aktif di halaman yang dilihat calon pendaftar. Karena
	 * pengisiannya terbatas pada pengguna yang lolos gerbang privilese (walau lewat menu induk —
	 * lihat Javadoc kelas), ini bukan jalur injeksi anonim, tetapi tetap perlu diperhatikan bila
	 * suatu saat kolom ini bisa diisi dari sumber yang kurang tepercaya.</p>
	 *
	 * @return teks informasi yang sudah di-trim, atau {@code ""} bila belum diisi
	 */
	@Column(name = "info", columnDefinition = "text", nullable = true)
	public String getInfo() {
		return info == null ? "" : info.trim();
	}

	/**
	 * Menyetel teks informasi kelompok. Tanpa validasi maupun sanitasi HTML.
	 *
	 * @param info teks informasi baru (boleh {@code null})
	 */
	public void setInfo(String info) {
		this.info = info;
	}

}
