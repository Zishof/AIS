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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

/**
 * Entity <b>MASTER organisasi profesi/keilmuan dosen</b> &mdash; tabel
 * {@code public.organisasi_dosen}.
 *
 * <p>Satu baris di sini mewakili satu organisasi tempat dosen dapat tercatat sebagai anggota:
 * asosiasi profesi, himpunan keilmuan, forum/konsorsium prodi, senat, dan sejenisnya. Baris ini
 * hanya menyimpan <i>identitas organisasinya</i> (kode, nama Indonesia, nama Inggris, tingkat,
 * cakupan fakultas/jurusan, keterangan). <b>Siapa</b> yang menjadi anggota, dengan jabatan apa,
 * pada rentang tahun berapa, dan apakah pengajuannya sudah disetujui &mdash; semuanya disimpan
 * pada entity penghubung {@link OrganisasiDosenPunyaDosen}, bukan di sini.</p>
 *
 * <h2>Posisi dalam keluarga entity</h2>
 * <ul>
 *   <li>{@link OrganisasiDosenPunyaDosen} &mdash; keanggotaan seorang {@link Dosen} pada satu
 *       organisasi; satu-satunya entity yang menunjuk balik ke sini lewat properti
 *       {@code organisasiDosen}.</li>
 *   <li>{@link LevelOrganisasiDosen} &mdash; master <b>tingkat/cakupan organisasi</b>
 *       (Internasional / Nasional / Lokal), dirujuk oleh {@link #getLevelOrganisasiDosen()}.
 *       Ini adalah properti <b>milik organisasi</b>, bukan milik keanggotaan.</li>
 *   <li>{@link JabatanOrganisasiDosen} &mdash; master <b>jabatan/peran orang di dalam
 *       organisasi</b> (Ketua / Pengurus / Anggota). Dirujuk oleh
 *       {@link OrganisasiDosenPunyaDosen}, <b>bukan</b> oleh entity ini. Perbedaan level vs
 *       jabatan ini penting; lihat catatan pada {@link #getLevelOrganisasiDosen()}.</li>
 *   <li>{@link Fakultas} / {@link Jurusan} &mdash; cakupan organisasi. Keduanya boleh
 *       {@code null}, yang berarti "berlaku untuk semua"; lihat {@link #getFakultas()}.</li>
 * </ul>
 *
 * <h2>Dari mana baris ini dibuat/diubah</h2>
 * <ol>
 *   <li><b>Layar master</b> &mdash; {@code ais.action.master.OrganisasiDosenAction}
 *       ({@code /pages/master/organisasi_dosen.zul}). Menyediakan CRUD penuh (Tambah/Ubah/Hapus),
 *       pencarian per nama/kode/fakultas/jurusan serta per nama+NIDN dosen anggotanya, panel
 *       detail keanggotaan ({@code OrganisasiDosenPunyaDosenHelper}), dan tab bawaan untuk
 *       master {@link JabatanOrganisasiDosen} dan {@link LevelOrganisasiDosen}.</li>
 *   <li><b>Impor Excel per-organisasi</b> &mdash; {@code OrganisasiDosenAction#onUploadData}.
 *       Setiap <i>sheet</i> pada berkas {@code .xlsx} dicocokkan ke satu organisasi
 *       <b>berdasarkan {@link #getKode() kode}</b> (nama sheet = kode). Bila tidak ketemu,
 *       organisasi baru dibuat otomatis dengan {@code nama} dan {@code keterangan} = nama sheet
 *       (lihat kuirk pada {@link #getKode()}). Isi sheet kemudian dipakai membuat/memperbarui
 *       baris {@link OrganisasiDosenPunyaDosen}.</li>
 *   <li><b>Impor/ekspor generik</b> &mdash; {@code Common.uploadData}/{@code Common.cetakData}
 *       dengan daftar kolom {@code id, nama, namaEn, fakultas, jurusan, keterangan}. Perhatikan
 *       {@code levelOrganisasiDosen} <b>tidak</b> termasuk kolom yang diekspor/diimpor.</li>
 * </ol>
 *
 * <h2>Siapa yang membaca baris ini</h2>
 * <ul>
 *   <li>{@code ais.action.master.helper.AmbilDataOrganisasiForOrganisasiDosenHelper} &mdash;
 *       pemilih organisasi saat dosen/staf menambah keanggotaan. Filter fakultas/jurusannya
 *       memakai {@code OR isNull(...)} sehingga organisasi ber-cakupan "Semua" selalu ikut
 *       tampil.</li>
 *   <li>{@code ais.action.master.helper.DosenPunyaOrganisasiDosenHelper} dan
 *       {@code ais.action.master.helper.profile.ProfileDosen} &mdash; menampilkan organisasi
 *       yang diikuti seorang dosen pada halaman profil.</li>
 *   <li>{@code ais.action.master.dashboard.admin.DashboardOrganisasiDosenUmum} &mdash;
 *       agregasi jumlah dosen per organisasi per jabatan per tahun.</li>
 *   <li>{@code ais.action.report.format1.akademik.LaporanOrganisasiDosen} dan
 *       {@code LaporanPerOrganisasiDosen} &mdash; cetakan daftar organisasi dosen.</li>
 *   <li>{@code ais.action.master.sapto.LaporanProfileDosen_A_4_5_5} &mdash; borang akreditasi
 *       BAN-PT butir A-4.5.5, satu-satunya konsumen yang <i>seharusnya</i> memakai
 *       {@link #getLevelOrganisasiDosen()}; lihat catatan bug di getter tersebut.</li>
 *   <li>{@code ais.common.InitData} &mdash; kelas ini terdaftar sebagai entity yang di-preload
 *       ke cache in-memory, sehingga {@code GeneralValueObject.check(...)} umumnya bisa
 *       menyelesaikan proxy-nya tanpa menyentuh database.</li>
 * </ul>
 *
 * <h2>Pengelompokan anggota kelas</h2>
 * <ul>
 *   <li><b>Jejak audit</b> (dideklarasikan ulang dari base class, lihat catatan di bawah):
 *       {@link #getOleh()}, {@link #getOlehId()}, {@link #getTanggal_dirubah()} beserta
 *       setter-nya, dan callback {@link #onUpdate()}.</li>
 *   <li><b>Identitas</b>: {@link #getId()}, {@link #getKode()}, {@link #getNama()},
 *       {@link #getNamaEn()}, {@link #toString()}.</li>
 *   <li><b>Klasifikasi</b>: {@link #getLevelOrganisasiDosen()}, {@link #getFakultas()},
 *       {@link #getJurusan()}.</li>
 *   <li><b>Deskriptif</b>: {@link #getKeterangan()}.</li>
 * </ul>
 * <p>Tidak ada method utilitas, query statis, maupun logika bisnis lain di kelas ini &mdash;
 * seluruh perilaku non-trivial terkonsentrasi pada {@link #getKode()} (lihat di bawah).</p>
 *
 * <h2>Verifikasi pola berulang keluarga entity ini</h2>
 * <p>Diperiksa langsung dari kode kelas ini, bukan diasumsikan dari entity lain:</p>
 * <ul>
 *   <li><b>Getter yang menulis balik ke field/DB</b>: <b>ADA satu</b> &mdash;
 *       {@link #getKode()} membangkitkan kode dari {@link #getId() id} dan menyimpannya ke field
 *       {@code kode}. Karena {@code kode} adalah properti terpetakan Hibernate, sekadar
 *       me-render daftar organisasi (renderer memanggil {@code getKode()} per baris) sudah cukup
 *       untuk memicu {@code UPDATE} saat flush.</li>
 *   <li><b>Getter yang menutup session Hibernate</b>: <b>TIDAK ADA</b>. Tidak ada satu pun
 *       pemanggilan {@code HibernateUtil.closeSession()} di kelas ini.</li>
 *   <li><b>Getter destruktif</b> (getter yang menghapus/mengosongkan data seperti
 *       {@code OrganisasiDosenPunyaDosen#getTbmuser()} atau {@code Komentar#getTbmuser()}):
 *       <b>TIDAK ADA</b>. Semua relasi di kelas ini murni baca.</li>
 *   <li><b>{@code getNama()} yang membangkitkan ulang label</b> (pola {@code Kota}/
 *       {@code Penghasilan}): <b>TIDAK ADA</b> &mdash; {@link #getNama()} di sini hanya
 *       me-{@code trim()} tanpa menulis balik.</li>
 *   <li><b>Asimetri {@code check()}</b>: {@link #getJurusan()} dan {@link #getFakultas()}
 *       memanggil {@code check(...)} untuk meresolusi proxy lazy, tetapi
 *       {@link #getLevelOrganisasiDosen()} <b>tidak</b> &mdash; relasi itu memakai fetch EAGER
 *       bawaan {@code @ManyToOne} plus {@code @Fetch(FetchMode.SELECT)} sebagai gantinya.</li>
 * </ul>
 *
 * <h2>Catatan {@code GeneralValueObject}</h2>
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass} &mdash; ia POJO abstrak biasa, sehingga Hibernate <b>tidak</b>
 * memetakan properti apa pun yang dideklarasikan di sana. Karena itu field {@link #id},
 * {@link #oleh}, {@link #olehId}, dan {@code tanggal_dirubah} <b>wajib</b> dideklarasikan ulang
 * di kelas ini agar ikut tersimpan; pengulangan tersebut <b>keharusan teknis, bukan bug</b>.
 * Konsekuensi lain: properti warisan yang <i>tidak</i> dideklarasikan ulang (misalnya
 * {@code diubahDari}) tidak tersimpan ke database dan akan kembali {@code null} setelah baris
 * dimuat ulang.</p>
 *
 * <h2>Catatan Envers</h2>
 * <p>Kelas ditandai {@link Audited}, sehingga setiap perubahan baris tersalin ke tabel revisi
 * dan dapat ditelusuri lewat {@code RevisiHelper} pada layar masternya. Kombinasi
 * {@code dynamicInsert}/{@code dynamicUpdate} membuat Hibernate hanya menuliskan kolom yang
 * benar-benar berubah.</p>
 *
 * <h2>Catatan keamanan (hasil audit, tidak diperbaiki di sini)</h2>
 * <ul>
 *   <li>Pada {@code OrganisasiDosenAction}, pemeriksaan hak akses UPDATE/DELETE
 *       ({@code CommonPrivilages.checkPrevilages(...)}) <b>dikomentari</b>, begitu pula
 *       {@code setVisible(edit)}/{@code setVisible(delete)} pada tombol Ubah dan Hapus &mdash;
 *       instance lain dari pola inversi hak akses yang berulang di modul master AIS.</li>
 *   <li>{@code OrganisasiDosenAction#initCriteria} menyisipkan nilai kotak pencarian nama dosen
 *       dan NIDN <b>mentah</b> ke dalam {@code Restrictions.sqlRestriction(...)} (subquery
 *       {@code organisasi_dosen_punya_dosen}) &mdash; SQL injection.</li>
 * </ul>
 *
 * @see GeneralValueObject
 * @see OrganisasiDosenPunyaDosen
 * @see LevelOrganisasiDosen
 * @see JabatanOrganisasiDosen
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "organisasi_dosen")

public class OrganisasiDosen extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya sengaja dibiarkan sama dengan sejumlah entity master lain
	 * hasil generator {@code hbm2java} (mis. {@link LevelOrganisasiDosen},
	 * {@link JabatanOrganisasiDosen}); duplikasi ini tidak menimbulkan masalah karena
	 * {@code serialVersionUID} hanya dibandingkan antar versi kelas yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris, kolom {@code id}. Dideklarasikan ulang dari base class (lihat Javadoc kelas). */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini. Dideklarasikan ulang dari base class. */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris ini. Dideklarasikan ulang dari base class. */
	private String olehId;

	/**
	 * ID pengguna terakhir yang mengubah baris ini. Diisi otomatis oleh
	 * {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}.
	 *
	 * <p>Properti ini dideklarasikan ulang di sini (tidak sekadar diwarisi) karena
	 * {@link GeneralValueObject} bukan {@code @MappedSuperclass}; tanpa deklarasi ulang,
	 * kolomnya tidak akan terpetakan Hibernate.</p>
	 *
	 * @return ID pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna pengubah terakhir.
	 *
	 * <p><b>Kuirk:</b> nilai {@code null} atau string kosong <b>diabaikan diam-diam</b> (method
	 * langsung {@code return}), sehingga jejak audit lama tidak pernah bisa dihapus lewat
	 * setter ini.</p>
	 *
	 * @param olehId ID pengguna pengubah; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna pengubah terakhir. Umumnya dipanggil
	 * {@code AuditTimestampInterceptor}, bukan kode aplikasi.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong diabaikan.</p>
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna terakhir yang mengubah baris ini. Dideklarasikan ulang di kelas ini karena
	 * alasan yang sama dengan {@link #getOlehId()}.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil Hibernate TEPAT SEBELUM setiap {@code UPDATE}
	 * baris ini, lalu meneruskan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #setOleh(String)}, {@link #setOlehId(String)}, dan
	 * {@link #setTanggal_dirubah(Date)} dari konteks pengguna yang sedang login.
	 *
	 * <p><b>Efek samping:</b> memodifikasi state objek di tengah siklus flush. Jangan dipanggil
	 * manual dari kode aplikasi &mdash; Hibernate yang memicunya.</p>
	 *
	 * <p>Hanya {@code @PreUpdate} yang dipasang; baris BARU ({@code INSERT}) tidak melewati
	 * callback ini, sehingga organisasi yang dibuat otomatis oleh impor Excel tidak punya jejak
	 * audit sampai ada penyuntingan berikutnya.</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja diletakkan pada baris fisik yang sama
	 * dalam kode aslinya; nilainya diinisialisasi memakai jam aplikasi
	 * ({@code ais.ui.util.WaktuUtil.getDate()}), bukan {@code new Date()}, agar konsisten dengan
	 * zona waktu/penyetelan waktu server yang dipakai seluruh modul.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan waktu perubahan terakhir. Berbeda dari {@link #setOleh(String)}, setter ini
	 * <b>tidak</b> menyaring {@code null}.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Waktu perubahan terakhir baris ini (kolom {@code tanggal_dirubah}, presisi timestamp).
	 * Terisi otomatis saat objek dibuat dan diperbarui pada setiap {@code UPDATE} lewat
	 * {@link #onUpdate()}.
	 *
	 * @return waktu perubahan terakhir; praktis tidak pernah {@code null} untuk objek baru
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas berbentuk {@code "<id>-<nama>"}.
	 *
	 * <p><b>Perhatian:</b> method ini membaca <b>field</b> {@code nama} langsung, bukan lewat
	 * {@link #getNama()}, sehingga hasilnya <b>tidak</b> di-{@code trim()} dan bisa berisi spasi
	 * awal/akhir. Dipakai antara lain untuk label combobox, pesan progres impor/ekspor, dan
	 * keperluan debug.</p>
	 *
	 * @return string {@code "<id>-<nama>"}; bagian id berbunyi {@code "null"} untuk baris yang
	 *         belum disimpan
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama organisasi dalam bahasa Indonesia (kolom {@code nama}, wajib isi, unik). */
	private String nama;
	/** Keterangan bebas tentang organisasi (kolom {@code keterangan}, opsional). */
	private String keterangan;
	/** Kode organisasi (kolom {@code kode}); dibangkitkan otomatis dari id bila kosong &mdash; lihat {@link #getKode()}. */
	private String kode;
	/** Cakupan program studi organisasi; {@code null} berarti berlaku untuk semua prodi. */
	private Jurusan jurusan;
	/** Cakupan fakultas organisasi; {@code null} berarti berlaku untuk semua fakultas. */
	private Fakultas fakultas;
	/** Tingkat organisasi (Internasional/Nasional/Lokal) &mdash; lihat {@link #getLevelOrganisasiDosen()}. */
	private LevelOrganisasiDosen levelOrganisasiDosen;
	/** Nama organisasi dalam bahasa Inggris (kolom {@code namaen}, opsional). */
	private String namaEn;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Semua properti dibiarkan
	 * {@code null} kecuali {@code tanggal_dirubah} yang langsung terisi jam aplikasi. Dipakai
	 * juga oleh layar master ({@code onAdd}) dan alur impor Excel untuk membuat baris baru.
	 */
	public OrganisasiDosen() {
	}

	/**
	 * Primary key baris (kolom {@code id}, {@code IDENTITY}/serial PostgreSQL).
	 *
	 * <p>{@code insertable = false} berarti nilai kolom sepenuhnya ditentukan sequence database;
	 * objek baru punya {@code id} {@code null} sampai di-{@code flush}. Nilai ini juga menjadi
	 * bahan pembentuk {@link #getKode() kode} otomatis.</p>
	 *
	 * @return id baris, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan primary key. Praktis hanya dipakai Hibernate saat memuat/menyimpan baris;
	 * kode aplikasi tidak boleh mengubah id baris yang sudah tersimpan.
	 *
	 * @param id id baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama organisasi dalam bahasa Indonesia (kolom {@code nama}, wajib isi, <b>unik</b> di
	 * tingkat database).
	 *
	 * <p>Ini adalah label utama organisasi di seluruh aplikasi: kolom grid layar master, isi
	 * combobox pemilih organisasi, judul panel detail keanggotaan, kolom "Nama Organisasi" pada
	 * borang akreditasi A-4.5.5, dan label pada profil dosen.</p>
	 *
	 * <p>Getter hanya melakukan {@code trim()} pada nilai yang dikembalikan dan <b>tidak</b>
	 * menulis balik ke field &mdash; berbeda dari pola {@code Kota#getNama()}/
	 * {@code Penghasilan#getNama()} yang memicu {@code UPDATE} saat sekadar dibaca.</p>
	 *
	 * <p>Keunikan ditegakkan dua kali: oleh constraint kolom, dan oleh
	 * {@code OrganisasiDosenAction#checkNamaOrganisasiDosen()} yang menolak simpan bila sudah
	 * ada baris lain (id berbeda) dengan nama persis sama.</p>
	 *
	 * @return nama organisasi tanpa spasi awal/akhir, atau {@code null} bila field belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255, unique = true)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama organisasi. Nilai disimpan apa adanya (tanpa {@code trim()}); pemangkasan
	 * baru terjadi saat dibaca lewat {@link #getNama()}.
	 *
	 * @param nama nama organisasi; wajib non-kosong agar lolos validasi layar master dan
	 *             constraint {@code NOT NULL} kolomnya
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas tentang organisasi (kolom {@code keterangan}, opsional).
	 *
	 * <p>Ditampilkan sebagai kolom tersendiri pada grid layar master dan pada grid pemilih
	 * organisasi. Pada organisasi yang dibuat otomatis oleh impor Excel, isinya disamakan
	 * dengan nama sheet.</p>
	 *
	 * <p>Berbeda dari klaim umum {@link GeneralValueObject} yang menjanjikan keterangan non-null,
	 * getter ini mengembalikan field apa adanya sehingga <b>boleh {@code null}</b> &mdash;
	 * pemanggil (mis. {@code new Label(...)} pada renderer) harus siap menerimanya.</p>
	 *
	 * @return keterangan organisasi, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan organisasi.
	 *
	 * @param keterangan keterangan bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Kode organisasi (kolom {@code kode}) &mdash; <b>satu-satunya method dengan logika nyata di
	 * kelas ini</b>.
	 *
	 * <p><b>Tujuan:</b> menyediakan identitas pendek dan stabil untuk organisasi, dipakai
	 * sebagai <i>nama sheet</i> pada ekspor Excel keanggotaan dan sebagai kunci pencocokan
	 * kembali saat berkas itu diimpor ulang.</p>
	 *
	 * <p><b>Perilaku:</b> bila baris sudah punya {@link #getId() id} tetapi {@code kode} masih
	 * {@code null}/kosong, kode dibangkitkan sebagai id yang <b>dipadkan nol menjadi 5 digit</b>
	 * ({@code "0000000000" + id}, lalu diambil 5 karakter terakhir), kemudian <b>disimpan ke
	 * field {@code kode}</b>. Bila kode sudah terisi, nilainya dikembalikan apa adanya.</p>
	 *
	 * <p><b>Efek samping (pola "getter yang menulis"):</b> karena {@code kode} adalah properti
	 * terpetakan Hibernate (tidak beranotasi {@code @Column}, jadi memakai nama kolom bawaan
	 * {@code kode}), penugasan di dalam getter membuat objek menjadi <i>dirty</i>. Sekadar
	 * me-render daftar organisasi sudah memicu {@code UPDATE} &mdash; renderer layar master
	 * memanggil {@code getKode()} untuk setiap baris, begitu pula proses ekspor Excel. Jadi
	 * kolom {@code kode} terisi sendiri seiring waktu tanpa ada yang pernah mengetiknya.</p>
	 *
	 * <p><b>Kuirk 1 &mdash; tidak ada jalur input manual:</b> layar master
	 * {@code OrganisasiDosenAction} menyediakan kotak <i>pencarian</i> kode
	 * ({@code searchkode}) tetapi <b>tidak</b> menyediakan kolom isian kode pada form
	 * Tambah/Ubah. Praktis nilai kode selalu hasil pembangkitan otomatis di atas.</p>
	 *
	 * <p><b>Kuirk 2 &mdash; kode meluap tanpa peringatan:</b> pemadan hanya 5 digit. Begitu
	 * id melewati 99.999, kode yang dihasilkan adalah 5 digit <i>terakhir</i> dari id sehingga
	 * dua organisasi berbeda bisa memperoleh kode identik; tidak ada constraint unik pada kolom
	 * ini yang mencegahnya, dan pencocokan sheet impor akan menjadi ambigu.</p>
	 *
	 * <p><b>Kuirk 3 &mdash; impor sheet asing membuat organisasi "hantu":</b> pada
	 * {@code onUploadData}, sheet yang namanya tidak cocok dengan kode mana pun akan membuat
	 * {@code OrganisasiDosen} baru dengan {@code nama} = nama sheet (biasanya berupa angka
	 * seperti {@code "00012"}), tanpa level maupun cakupan fakultas/jurusan. Karena baris baru
	 * itu langsung memperoleh kode dari <b>id barunya sendiri</b> (bukan dari nama sheet),
	 * pengunggahan berkas yang sama untuk kedua kali tetap tidak menemukan kecocokan dan mencoba
	 * membuat baris kembar &mdash; yang lalu ditolak constraint unik pada {@code nama} dan
	 * gagal diam-diam (pengecualiannya hanya dicatat, sheet tersebut dilewati).</p>
	 *
	 * @return kode organisasi (5 digit berpadding nol), atau {@code null} bila baris belum
	 *         punya id dan kode belum pernah diisi
	 */
	public String getKode() {
		if (id != null && (kode == null || kode.trim().isEmpty())) {
			String k = "0000000000" + id;
			kode = k.substring(k.length() - 5);
		}
		return kode;
	}

	/**
	 * Menetapkan kode organisasi secara eksplisit. Praktis hanya dipakai Hibernate saat memuat
	 * baris; tidak ada layar yang memanggilnya (lihat kuirk pada {@link #getKode()}).
	 *
	 * <p>Mengisi nilai non-kosong di sini mematikan pembangkitan otomatis pada
	 * {@link #getKode()}.</p>
	 *
	 * @param kode kode organisasi; boleh {@code null}
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Program studi yang menjadi cakupan organisasi ini (kolom FK {@code jurusan}, opsional).
	 *
	 * <p>{@code null} berarti <b>"Semua"</b> prodi &mdash; itulah teks yang dirender layar
	 * master dan grid pemilih organisasi untuk nilai kosong. Pada pemilih organisasi
	 * ({@code AmbilDataOrganisasiForOrganisasiDosenHelper}) filter prodi disusun sebagai
	 * {@code OR isNull("jurusan")}, sehingga organisasi lintas-prodi selalu ikut muncul berapa
	 * pun filter yang dipilih pengguna.</p>
	 *
	 * <p><b>Efek samping:</b> getter memanggil {@code check(...)} milik
	 * {@link GeneralValueObject} untuk meresolusi proxy lazy (cache in-memory &rarr; session
	 * aktif &rarr; session baru), lalu <b>menugaskan kembali hasilnya ke field</b>. Penugasan
	 * ini menukar proxy dengan instance yang sudah terinisialisasi &mdash; tidak mengubah
	 * identitas baris, jadi tidak membuat objek dirty terhadap kolom FK-nya.</p>
	 *
	 * <p>Nilai awal pada form Tambah diisi otomatis dari prodi pengguna yang sedang login bila
	 * pengguna tersebut terikat pada satu prodi; dalam hal itu combobox prodi juga
	 * di-{@code setDisabled(true)} sehingga staf prodi tidak bisa membuat organisasi atas nama
	 * prodi lain lewat UI.</p>
	 *
	 * @return prodi cakupan organisasi, atau {@code null} bila berlaku untuk semua prodi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Menetapkan prodi cakupan organisasi. Dipanggil {@code OrganisasiDosenAction#onSave} dari
	 * pilihan combobox Prodi; nilai {@code null} berarti organisasi berlaku lintas prodi.
	 *
	 * @param jurusan prodi cakupan; boleh {@code null}
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Fakultas yang menjadi cakupan organisasi ini (kolom FK {@code fakultas}, opsional).
	 *
	 * <p>Semantik dan efek samping {@code check(...)}-nya identik dengan {@link #getJurusan()}:
	 * {@code null} dirender sebagai <b>"Semua"</b>, dan filter fakultas pada pemilih organisasi
	 * memakai {@code OR isNull("fakultas")}.</p>
	 *
	 * <p><b>Kuirk:</b> berbeda dari {@code ItemBiayaPunyaAkun#getFakultas()}, getter ini
	 * <b>tidak</b> menurunkan fakultas dari {@link #getJurusan()} secara otomatis. Akibatnya
	 * kedua kolom bisa saling bertentangan &mdash; misalnya {@code jurusan} diisi prodi milik
	 * Fakultas A sementara {@code fakultas} diisi Fakultas B &mdash; dan tidak ada validasi
	 * konsistensi di {@code onSave}. Pada form Tambah, {@code fakultas} juga terisi otomatis
	 * dari fakultas pengguna yang sedang login bila masih kosong.</p>
	 *
	 * @return fakultas cakupan organisasi, atau {@code null} bila berlaku untuk semua fakultas
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * Menetapkan fakultas cakupan organisasi. Dipanggil {@code OrganisasiDosenAction#onSave}
	 * dari pilihan combobox Fakultas, dan juga oleh {@code init()} yang mengisi default dari
	 * fakultas pengguna yang sedang login.
	 *
	 * @param fakultas fakultas cakupan; boleh {@code null}
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * <b>Tingkat/cakupan organisasi</b> &mdash; Internasional, Nasional, atau Lokal (kolom FK
	 * {@code level_organisasi_dosen}, opsional di database tetapi <b>wajib diisi lewat UI</b>).
	 *
	 * <p>Tiga baris {@link LevelOrganisasiDosen} tersebut di-<i>seed</i> otomatis oleh
	 * {@code OrganisasiDosenAction#doAfterCompose()} pada saat layar master pertama kali dibuka
	 * jika tabelnya masih kosong &mdash; jadi daftar pilihannya bukan enum di kode, melainkan
	 * baris database yang bisa diubah/ditambah pengguna lewat tab "Level Organisasi Dosen".</p>
	 *
	 * <p><b>Cara pakai yang benar:</b> tingkat organisasi adalah properti <b>organisasinya</b>
	 * (IEEE = Internasional, sebuah forum prodi = Lokal), bukan properti keanggotaan. Kode yang
	 * ingin mengetahui tingkat keikutsertaan seorang dosen harus menempuh jalur
	 * {@code OrganisasiDosenPunyaDosen.getOrganisasiDosen().getLevelOrganisasiDosen().getNama()}.
	 * Jangan dikacaukan dengan {@link JabatanOrganisasiDosen} yang menyimpan <b>peran orang di
	 * dalam</b> organisasi (Ketua/Pengurus/Anggota, juga di-seed di tempat yang sama) dan
	 * digantung pada {@link OrganisasiDosenPunyaDosen}, bukan di sini.</p>
	 *
	 * <p><b>BUG NYATA yang sudah dikonfirmasi</b> (dicatat, tidak diperbaiki di sini):
	 * {@code ais.action.master.sapto.LaporanProfileDosen_A_4_5_5} &mdash; borang akreditasi
	 * BAN-PT butir A-4.5.5 yang justru memerlukan tingkat organisasi &mdash; mengisi kolom
	 * Internasional/Nasional/Lokal dengan membandingkan
	 * {@code OrganisasiDosenPunyaDosen#getJabatanOrganisasiDosen().getNama()} terhadap string
	 * {@code "Internasional"} dan {@code "Nasional"}. Yang dibaca adalah master <b>jabatan</b>,
	 * yang isinya Ketua/Pengurus/Anggota, sehingga kedua perbandingan itu tidak akan pernah
	 * cocok dan <b>seluruh baris borang jatuh ke cabang {@code else} dan ditandai "Lokal"</b>.
	 * Sumber yang benar adalah properti ini. Efeknya langsung ke dokumen akreditasi:
	 * keanggotaan organisasi internasional/nasional tidak pernah terhitung.</p>
	 *
	 * <p><b>Konsekuensi kedua &mdash; field ini praktis "write-only":</b> penelusuran seluruh
	 * kode menunjukkan {@code getLevelOrganisasiDosen()} hanya dibaca di dua tempat, keduanya di
	 * dalam {@code OrganisasiDosenAction} sendiri (kolom grid daftar dan pengisian awal combobox
	 * pada form Ubah). Tidak ada laporan, dasbor, ekspor, maupun borang yang memakainya
	 * &mdash; termasuk daftar kolom ekspor/impor generik layar master, yang hanya memuat
	 * {@code id, nama, namaEn, fakultas, jurusan, keterangan}. Jadi data yang wajib diisi
	 * pengguna ini tidak pernah dimanfaatkan hilir, sementara satu-satunya konsumen yang
	 * membutuhkannya membaca kolom yang salah.</p>
	 *
	 * <p><b>Catatan teknis:</b> berbeda dari {@link #getJurusan()}/{@link #getFakultas()},
	 * getter ini <b>tidak</b> memanggil {@code check(...)}. Relasi ini memakai fetch EAGER
	 * bawaan {@code @ManyToOne} (tidak ada {@code FetchType.LAZY}) ditambah
	 * {@code @Fetch(FetchMode.SELECT)}, sehingga Hibernate memuatnya lewat query {@code SELECT}
	 * terpisah dan getter dapat mengembalikan field apa adanya. Konsekuensinya: bila objek
	 * organisasi sudah <i>detached</i> dan relasinya belum termuat, tidak ada mekanisme
	 * pemulihan seperti pada dua relasi lain.</p>
	 *
	 * @return tingkat organisasi, atau {@code null} untuk baris lama/hasil impor yang belum
	 *         pernah disunting lewat layar master
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "level_organisasi_dosen", nullable = true)
	public LevelOrganisasiDosen getLevelOrganisasiDosen() {
		return levelOrganisasiDosen;
	}

	/**
	 * Menetapkan tingkat organisasi.
	 *
	 * <p>Satu-satunya pemanggil adalah {@code OrganisasiDosenAction#onSave}, yang lebih dulu
	 * menolak penyimpanan bila combobox "Tingkat / Level Organisasi Dosen" belum dipilih
	 * &mdash; sehingga lewat UI nilainya tidak pernah dikosongkan kembali. Baris yang dibuat di
	 * luar layar master (impor Excel {@code onUploadData}, atau endpoint reflektif) tidak
	 * melewati validasi itu dan bisa tetap {@code null}.</p>
	 *
	 * @param levelOrganisasiDosen tingkat organisasi; boleh {@code null} pada tingkat model
	 */
	public void setLevelOrganisasiDosen(LevelOrganisasiDosen levelOrganisasiDosen) {
		this.levelOrganisasiDosen = levelOrganisasiDosen;
	}

	/**
	 * Nama organisasi dalam bahasa Inggris (kolom {@code namaen}, opsional).
	 *
	 * <p>Perhatikan nama kolom fisiknya seluruhnya huruf kecil tanpa pemisah
	 * ({@code namaen}), berbeda dari nama properti Java {@code namaEn}; anotasi
	 * {@code @Column} eksplisit inilah yang menjembatani keduanya.</p>
	 *
	 * <p>Dipakai untuk dokumen berbahasa Inggris (transkrip/borang internasional). Pada grid
	 * layar master nilainya dirender sebagai baris kedua di bawah nama Indonesia, dan ikut
	 * dalam daftar kolom ekspor/impor generik. Getter mengembalikan field apa adanya tanpa
	 * {@code trim()} &mdash; berbeda dari {@link #getNama()}.</p>
	 *
	 * @return nama organisasi dalam bahasa Inggris, atau {@code null} bila belum diisi
	 */
	@Column(name = "namaen")
	public String getNamaEn() {
		return namaEn;
	}

	/**
	 * Menetapkan nama organisasi dalam bahasa Inggris. Dipanggil
	 * {@code OrganisasiDosenAction#onSave} dari isian "Nama Organisasi (dalam bhs inggris)".
	 *
	 * <p>Tidak ada validasi wajib-isi maupun keunikan untuk kolom ini.</p>
	 *
	 * @param namaEn nama organisasi dalam bahasa Inggris; boleh {@code null}/kosong
	 */
	public void setNamaEn(String namaEn) {
		this.namaEn = namaEn;
	}

}
