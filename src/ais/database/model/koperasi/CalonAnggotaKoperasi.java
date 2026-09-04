package ais.database.model.koperasi;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

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

import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.library.Perpustakaan;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Siswa;

/**
 * <h2>CalonAnggotaKoperasi — Formulir Pendaftaran Calon Anggota Koperasi</h2>
 *
 * <p>
 * Entity ini adalah <b>formulir pendaftaran</b> seseorang yang mengajukan diri menjadi anggota
 * sebuah {@link Koperasi}, yaitu tahap sebelum ia menjadi {@link AnggotaKoperasi} resmi. Satu baris
 * di tabel {@code koperasi.calon_anggota_koperasi} memuat berkas identitas calon secara lengkap:
 * sumber identitasnya di modul lain, nomor identitas, nama, alamat, kontak, jenis dan tipe
 * keanggotaan yang diminta, serta usulan kredensial ({@link #getUserid()} dan {@link #getPass()}).
 * </p>
 *
 * <h3>Rancangan: salinan field, bukan pemindahan kunci asing</h3>
 * <p>
 * Kelas ini secara struktural adalah <b>cermin</b> dari {@link AnggotaKoperasi} — himpunan fieldnya
 * hampir sama persis, getter turunannya memakai aturan yang sama persis, bahkan
 * {@code serialVersionUID}-nya sama. Rancangan itu disengaja: begitu pendaftaran disetujui, isi
 * formulir ini <b>disalin field demi field</b> ke baris {@link AnggotaKoperasi} yang baru, bukan
 * dipindahkan lewat penggantian kunci asing. Baris formulirnya sendiri tetap tinggal sebagai arsip
 * pendaftaran.
 * </p>
 * <p>
 * Yang menghubungkan keduanya adalah sepasang kunci asing yang saling menunjuk, masing-masing
 * {@code nullable}:
 * </p>
 * <ul>
 * <li>{@link #getAnggotaKoperasi()} pada kelas ini — menunjuk <i>maju</i> ke baris anggota yang
 * lahir dari formulir ini; kekosongannya berarti pendaftaran belum menghasilkan anggota;</li>
 * <li>{@code AnggotaKoperasi.getCalonAnggotaKoperasi()} — menunjuk <i>mundur</i> ke formulir asalnya,
 * sehingga anggota yang berasal dari pendaftaran dapat dibedakan dari anggota yang dibuat langsung
 * oleh admin.</li>
 * </ul>
 * <p>
 * Karena datanya disalin dan bukan dipindahkan, <b>perubahan pada formulir setelah persetujuan tidak
 * merambat ke baris anggota</b>, dan sebaliknya. Keduanya menjadi dua kebenaran yang berdiri
 * sendiri-sendiri; bila keduanya perlu diselaraskan, penyelarasan itu harus dilakukan secara
 * eksplisit.
 * </p>
 *
 * <h3>Status alur persetujuan pada basis kode saat ini</h3>
 * <p>
 * <b>Perlu diketahui pembaca kode:</b> penelusuran seluruh sumber Java menunjukkan bahwa
 * <b>tidak ada satu pun jalur kode yang benar-benar menjalankan persetujuan itu</b>. Tidak ada
 * pemanggil {@code AnggotaKoperasi.setCalonAnggotaKoperasi(...)} maupun
 * {@link #setAnggotaKoperasi(AnggotaKoperasi)} di luar entity-nya sendiri, dan tidak ada
 * {@code Action}/helper yang membaca atau menulis baris tabel ini. Yang ada hanyalah tiga
 * pendaftaran pasif:
 * </p>
 * <ul>
 * <li>{@code ais.common.InitData} — kelas ini ikut disebut pada {@code initClasses(...)} sehingga
 * tabelnya dibuat dan disemai bersama entity master lain;</li>
 * <li>{@code ais.action.servlet.api.RevisiApiHelper} — terdaftar sebagai entitas {@code calon_anggota}
 * pada menu Riwayat Perubahan Data. Aksesnya sengaja digerbangi kunci menu {@code "anggota"} karena
 * cuplikan revisinya membawa data pribadi (kode identitas, nama, alamat, telepon, HP, surel) yang
 * tidak ditutupi penyaring properti sensitif — penyaring itu bertugas menutup kredensial, bukan data
 * pribadi;</li>
 * <li>manifes CRUD generik ({@code general_value_object_inventory}) — berstatus
 * {@code ELIGIBLE_METADATA_FIRST} namun <b>tetap dinonaktifkan secara bawaan</b> sampai pemetaan
 * Hibernate, menu, dan lingkupnya diverifikasi.</li>
 * </ul>
 * <p>
 * Dengan kata lain entity ini <b>tidur</b>: skema dan jejak auditnya siap, alur pemakaiannya belum
 * ada. Konsekuensi yang penting bagi audit keamanan — pertanyaan "siapa yang boleh menyetujui
 * pendaftaran" saat ini <b>tidak dapat dijawab dari kode</b>, karena gerbang persetujuannya memang
 * belum ditulis. Bila alur itu kelak dibuat, gerbang tersebut harus ditambahkan secara sadar; tidak
 * ada apa pun pada entity ini yang menyediakannya, dan tidak ada pula yang mencegah pembuat formulir
 * menyetujui formulirnya sendiri.
 * </p>
 *
 * <h3>Getter turunan yang menimpa nilainya sendiri</h3>
 * <p>
 * Sebagian besar getter di kelas ini <b>bukan pengakses biasa</b>: ia menghitung ulang nilainya dari
 * relasi sumber identitas dan <b>menimpa field</b> sebelum mengembalikannya, sehingga nilai hasil
 * {@code setXxx(...)} dapat hilang diam-diam. Ini pola getter destruktif yang berulang di domain
 * ini. Yang terdampak: {@link #getNama()}, {@link #getKode()}, {@link #getKodeIdentitas()},
 * {@link #getJenisAnggotaKoperasi()}, {@link #getJenisIdentitasAnggotaKoperasi()},
 * {@link #getTipeAnggotaKoperasi()}, {@link #getSatuanKerja()}, {@link #getKoperasi()}, dan
 * {@link #getAktif()}. Karena Hibernate memakai getter yang sama saat menulis baris, nilai turunan
 * itu <b>ikut tersimpan</b> ke kolomnya.
 * </p>
 *
 * <h3>Catatan tenancy</h3>
 * <p>
 * {@link #getKoperasi()} tidak sekadar membaca kolomnya: bila kolom itu kosong, ia mengisi diri
 * dengan {@code Common.getCurrentKoperasi()} — koperasi milik <b>pembaca</b>, bukan milik data.
 * Artinya baris tanpa pemilik akan tampak menjadi milik siapa pun yang kebetulan membukanya, dan
 * nilai itu ikut tersimpan pada penulisan berikutnya. Setiap query yang mengambil data kelas ini
 * karena itu wajib menyaring koperasinya sendiri di tingkat query, bukan bersandar pada getter ini.
 * </p>
 *
 * @see AnggotaKoperasi baris anggota resmi yang lahir dari formulir ini
 * @see Koperasi unit usaha yang menaungi pendaftaran
 * @see JenisAnggotaKoperasi penentu aturan saldo/cashback keanggotaan yang diminta
 * @see TipeAnggotaKoperasi penggolongan calon (mahasiswa, dosen, siswa, guru, pegawai)
 * @see JenisIdentitasAnggotaKoperasi jenis nomor identitas yang dipakai
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "calon_anggota_koperasi")
public class CalonAnggotaKoperasi extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilainya sengaja sama dengan {@link AnggotaKoperasi} dan
	 * beberapa entity koperasi lain hasil pembangkitan Hibernate Tools; jangan diubah.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama (identity/auto-increment). Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna pengubah terakhir (field bayangan audit). Lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna pengubah terakhir (field bayangan audit). Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Id pengguna yang tercatat terakhir kali mengubah baris ini.
	 *
	 * <p>
	 * <b>Field bayangan audit</b> yang diisi otomatis oleh
	 * {@code ais.database.hibernate.AuditTimestampInterceptor}, bukan data bisnis dan bukan identitas
	 * pendaftar. Untuk mengetahui siapa yang membuat formulir ini, gunakan {@link #getDibuatOleh()};
	 * untuk mengetahui siapa calonnya, gunakan relasi sumber identitas ({@link #getMahasiswa()} dan
	 * sejenisnya) atau {@link #getNama()}.
	 * </p>
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah terakhir.
	 *
	 * <p>
	 * Nilai {@code null} maupun string kosong <b>diabaikan diam-diam</b> sehingga nilai lama
	 * dipertahankan. Ini disengaja: jejak audit yang sudah ada tidak boleh terhapus oleh proses yang
	 * kebetulan tidak mengetahui pelakunya. Nilai tak-kosong tetap boleh menimpa nilai tak-kosong
	 * sebelumnya, karena yang dicatat adalah pengubah <i>terakhir</i>.
	 * </p>
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir; nilai {@code null}/kosong diabaikan dengan alasan yang
	 * sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna yang tercatat terakhir kali mengubah baris ini (field bayangan audit).
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 * @see #getDibuatOleh() pembuat formulir, yang berbeda maknanya
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang berjalan tepat sebelum baris ini di-{@code UPDATE}.
	 *
	 * <p>
	 * Mendelegasikan pengisian {@link #getOleh()}, {@link #getOlehId()}, dan
	 * {@link #getTanggal_dirubah()} ke {@code AuditTimestampInterceptor.ubah(Object)}. Hanya dipicu
	 * pada pembaruan, bukan penyisipan baris baru — karena itu pembuat baris dicatat terpisah lewat
	 * {@link #getDibuatOleh()}.
	 * </p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Cap waktu perubahan terakhir (field bayangan audit). Lihat {@link #getTanggal_dirubah()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi cap waktu perubahan terakhir. Umumnya tidak dipanggil langsung; {@link #onUpdate()}
	 * yang mengisinya lewat interceptor audit.
	 *
	 * @param tanggal_dirubah cap waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Cap waktu baris ini terakhir diubah (field bayangan audit).
	 *
	 * <p>
	 * Bukan tanggal pendaftaran — untuk itu gunakan {@link #getTanggal()}. Nilai awalnya diambil dari
	 * {@code WaktuUtil.getDate()} (waktu aplikasi, yang dapat digeser konfigurasi) alih-alih
	 * {@code new Date()}, supaya konsisten dengan seluruh cap waktu lain di aplikasi.
	 * </p>
	 *
	 * @return cap waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks formulir ini dalam bentuk {@code "kode - nama"}.
	 *
	 * <p>
	 * Dipakai komponen ZK sebagai label baris. Perhatikan bahwa method ini memanggil
	 * {@link #getNama()}, yang <b>menyentuh enam relasi malas</b> (mahasiswa, dosen, pegawai,
	 * tbmuser, guru, siswa) untuk menurunkan namanya. Memanggil {@code toString()} atas banyak baris
	 * sekaligus — misalnya untuk mengisi combobox — karena itu memicu sejumlah besar query tambahan.
	 * Field {@code kode} dibaca langsung tanpa lewat {@link #getKode()}, sehingga bila kode belum
	 * pernah diturunkan hasilnya berawalan {@code "null - "}.
	 * </p>
	 *
	 * @return label {@code "kode - nama"}
	 */
	public String toString() {
		String nama = getNama();
		return kode + " - " + nama;
	}

	/** Nomor identitas calon, diturunkan dari sumber identitasnya. Lihat {@link #getKodeIdentitas()}. */
	private String kodeIdentitas;
	/** Keterangan jenis identitas dalam bentuk teks bebas. Lihat {@link #getJenisIdentitas()}. */
	private String jenisIdentitas;
	/** Kode keanggotaan yang diusulkan, unik. Lihat {@link #getKode()}. */
	private String kode;
	/** Nama calon, diturunkan dari sumber identitasnya. Lihat {@link #getNama()}. */
	private String nama;
	/** Alamat calon. Lihat {@link #getAlamat()}. */
	private String alamat;
	/** Koperasi tujuan pendaftaran; penentu batas tenancy. Lihat {@link #getKoperasi()}. */
	private Koperasi koperasi;

	/** Usulan nama pengguna untuk akun anggota. Lihat {@link #getUserid()}. */
	private String userid;
	/** Usulan kata sandi untuk akun anggota. Lihat {@link #getPass()}. */
	private String pass;

	/** Sumber identitas: mahasiswa. Lihat {@link #getMahasiswa()}. */
	private Mahasiswa mahasiswa;
	/** Sumber identitas: dosen. Lihat {@link #getDosen()}. */
	private Dosen dosen;
	/** Sumber identitas: guru. Lihat {@link #getGuru()}. */
	private Guru guru;
	/** Sumber identitas: siswa. Lihat {@link #getSiswa()}. */
	private Siswa siswa;
	/** Sumber identitas: pegawai. Lihat {@link #getPegawai()}. */
	private Pegawai pegawai;
	/** Sumber identitas: akun pengguna aplikasi. Lihat {@link #getTbmuser()}. */
	private Tbmuser tbmuser;
	/** Satuan kerja calon, diturunkan berlapis. Lihat {@link #getSatuanKerja()}. */
	private SatuanKerja satuanKerja;
	/** Jenis keanggotaan yang diminta. Lihat {@link #getJenisAnggotaKoperasi()}. */
	private JenisAnggotaKoperasi jenisAnggotaKoperasi;
	/** Penggolongan calon dalam bentuk teks bebas. Lihat {@link #getTipe()}. */
	private String tipe;
	/** Catatan bebas atas pendaftaran ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Nomor telepon tetap calon. Lihat {@link #getTelp()}. */
	private String telp;
	/** Nomor telepon seluler calon. Lihat {@link #getHp()}. */
	private String hp;
	/** Alamat surel calon. Lihat {@link #getEmail()}. */
	private String email;
	/** Jenis nomor identitas yang dipakai. Lihat {@link #getJenisIdentitasAnggotaKoperasi()}. */
	private JenisIdentitasAnggotaKoperasi jenisIdentitasAnggotaKoperasi;
	/** Tipe keanggotaan terstruktur. Lihat {@link #getTipeAnggotaKoperasi()}. */
	private TipeAnggotaKoperasi tipeAnggotaKoperasi;
	/** Penanda formulir masih berlaku; bawaan {@code true}. Lihat {@link #getAktif()}. */
	private Boolean aktif = true;

	/** Tanggal pendaftaran diajukan. Lihat {@link #getTanggal()}. */
	private Date tanggal = ais.ui.util.WaktuUtil.getDate();
	/** Pengguna yang membuat formulir ini. Lihat {@link #getDibuatOleh()}. */
	private Tbmuser dibuatOleh;

	/** Anggota resmi hasil persetujuan formulir ini. Lihat {@link #getAnggotaKoperasi()}. */
	private AnggotaKoperasi anggotaKoperasi;

	/**
	 * Konstruktor kosong yang diwajibkan JPA/Hibernate.
	 *
	 * <p>
	 * Dua field sudah bernilai awal saat objek dibuat: {@link #getAktif()} bernilai {@code true} dan
	 * {@link #getTanggal()} bernilai waktu aplikasi saat ini, sehingga formulir baru langsung
	 * berstatus aktif dan bertanggal hari ini tanpa perlu diisi pemanggil.
	 * </p>
	 */
	public CalonAnggotaKoperasi() {
	}

	/**
	 * Kunci utama baris ini, dibangkitkan basis data (strategi {@code IDENTITY}).
	 *
	 * <p>
	 * Bernilai {@code null} selama formulir belum disimpan. Pemeriksaan {@code id == null} juga
	 * dipakai sebagai penanda "baris baru" di dalam {@link #getSatuanKerja()}, yang hanya menebak
	 * satuan kerja dari pengguna aktif pada baris yang belum tersimpan.
	 * </p>
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama. Dipakai Hibernate saat memuat baris.
	 *
	 * @param id id baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Catatan bebas atas pendaftaran ini — misalnya alasan pengajuan, catatan verifikator, atau
	 * keterangan berkas yang menyertainya.
	 *
	 * <p>
	 * Berbeda dari {@code keterangan} pada beberapa entity lain, kolom ini <b>tidak</b> bertipe
	 * {@code text} melainkan {@code varchar} bawaan, sehingga catatan yang sangat panjang berisiko
	 * ditolak basis data.
	 * </p>
	 *
	 * @return catatan bebas, atau {@code null} bila tidak ada
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi catatan bebas atas pendaftaran ini.
	 *
	 * @param keterangan catatan bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Sumber identitas calon bila ia seorang <b>mahasiswa</b>.
	 *
	 * <p>
	 * Ini satu dari enam relasi sumber identitas ({@link #getMahasiswa()}, {@link #getDosen()},
	 * {@link #getGuru()}, {@link #getSiswa()}, {@link #getPegawai()}, {@link #getTbmuser()}) yang
	 * seluruhnya {@code nullable}. Pada praktiknya hanya satu yang diisi, dan pilihan itu menentukan
	 * hasil hampir semua getter turunan di kelas ini: {@link #getNama()}, {@link #getKode()},
	 * {@link #getKodeIdentitas()}, {@link #getJenisIdentitasAnggotaKoperasi()},
	 * {@link #getTipeAnggotaKoperasi()}, dan {@link #getSatuanKerja()}.
	 * </p>
	 *
	 * <p>
	 * Mahasiswa menempati <b>urutan pertama</b> pada seluruh rantai pemeriksaan tersebut, sehingga
	 * bila lebih dari satu sumber identitas terisi, mahasiswalah yang menang. Nilai dilewatkan
	 * {@link GeneralValueObject#check(Object)} untuk menyelesaikan proksi malas.
	 * </p>
	 *
	 * @return mahasiswa sumber identitas, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * Menetapkan mahasiswa sebagai sumber identitas calon.
	 *
	 * @param mahasiswa mahasiswa sumber identitas; boleh {@code null}
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Sumber identitas calon bila ia seorang <b>dosen</b>. Menempati urutan kedua pada rantai
	 * pemeriksaan getter turunan, setelah {@link #getMahasiswa()}.
	 *
	 * @return dosen sumber identitas, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen", nullable = true)
	public Dosen getDosen() {
		dosen = check(dosen);
		return dosen;
	}

	/**
	 * Menetapkan dosen sebagai sumber identitas calon.
	 *
	 * @param dosen dosen sumber identitas; boleh {@code null}
	 */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/**
	 * Sumber identitas calon bila ia seorang <b>pegawai</b>.
	 *
	 * <p>
	 * Menempati urutan terakhir pada rantai penurunan nama dan kode identitas, tetapi urutan
	 * <b>pertama</b> pada {@link #getSatuanKerja()} — satuan kerja pegawai dianggap paling
	 * meyakinkan karena melekat langsung pada barisnya sendiri, bukan diturunkan lewat jurusan,
	 * fakultas, atau sekolah.
	 * </p>
	 *
	 * @return pegawai sumber identitas, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = true)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/**
	 * Menetapkan pegawai sebagai sumber identitas calon.
	 *
	 * @param pegawai pegawai sumber identitas; boleh {@code null}
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Sumber identitas calon berupa <b>akun pengguna aplikasi</b>.
	 *
	 * <p>
	 * Ini sumber identitas cadangan bagi calon yang tidak tercatat sebagai mahasiswa, dosen, guru,
	 * siswa, maupun pegawai — misalnya pengguna umum atau mitra luar. Karena letaknya paling akhir
	 * pada rantai {@link #getNama()}, namanya hanya terpakai bila tidak ada sumber lain, dan ia
	 * <b>tidak</b> ikut menentukan {@link #getKodeIdentitas()} — akun aplikasi memang tidak membawa
	 * nomor identitas resmi.
	 * </p>
	 *
	 * @return akun pengguna sumber identitas, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		tbmuser = check(tbmuser);
		return tbmuser;
	}

	/**
	 * Menetapkan akun pengguna aplikasi sebagai sumber identitas calon.
	 *
	 * @param tbmuser akun pengguna; boleh {@code null}
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * Jenis keanggotaan yang diminta calon — penentu aturan saldo/dompet dan cashback yang akan
	 * berlaku baginya setelah menjadi anggota.
	 *
	 * <p>
	 * <b>Getter destruktif dengan nilai bawaan.</b> Bila kolomnya kosong, field diisi
	 * {@code ConstantValues.ANGGOTA_KOPERASI_REGULER} dan nilai itu ikut tersimpan pada penulisan
	 * berikutnya — jadi "tidak memilih jenis" pada praktiknya berarti "memilih reguler", bukan
	 * "belum ditentukan". Perhatikan cabang {@code if}/{@code else}-nya: {@code check(...)} hanya
	 * dipanggil pada cabang <i>kolom terisi</i>, sedangkan nilai bawaan dari {@code ConstantValues}
	 * dikembalikan apa adanya karena memang sudah berupa objek konstanta, bukan proksi.
	 * </p>
	 *
	 * @return jenis keanggotaan, tidak pernah {@code null} setelah dipanggil
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_anggota_koperasi", nullable = true)
	public JenisAnggotaKoperasi getJenisAnggotaKoperasi() {
		if (jenisAnggotaKoperasi == null) {
			jenisAnggotaKoperasi = ConstantValues.ANGGOTA_KOPERASI_REGULER;
		} else {
			jenisAnggotaKoperasi = check(jenisAnggotaKoperasi);
		}
		return jenisAnggotaKoperasi;
	}

	/**
	 * Mengisi jenis keanggotaan yang diminta.
	 *
	 * @param jenisAnggotaKoperasi jenis keanggotaan; {@code null} akan berubah menjadi reguler saat
	 *            dibaca
	 */
	public void setJenisAnggotaKoperasi(JenisAnggotaKoperasi jenisAnggotaKoperasi) {
		this.jenisAnggotaKoperasi = jenisAnggotaKoperasi;
	}

	/**
	 * Kode keanggotaan yang diusulkan untuk calon ini — <b>diturunkan sekali lalu dibekukan</b>.
	 *
	 * <p>
	 * Kolomnya {@code unique}, sehingga kode inilah pengenal fungsional formulir dan menjadi
	 * awalan label {@link #toString()}. Penurunannya hanya berjalan ketika field masih kosong; sekali
	 * terisi, nilainya tidak pernah dihitung ulang. Urutan sumbernya:
	 * </p>
	 * <ol>
	 * <li>NIM {@link #getMahasiswa()} — diambil tanpa memeriksa apakah NIM-nya kosong, sehingga
	 * mahasiswa tanpa NIM menghasilkan kode {@code null} dan penurunan berhenti di situ;</li>
	 * <li>NIDN {@link #getDosen()}, bila tidak kosong;</li>
	 * <li>nomor induk {@link #getSiswa()}, bila tidak kosong;</li>
	 * <li>NUPTK {@link #getGuru()}, bila tidak kosong;</li>
	 * <li>bila semuanya gagal, kode acak dari {@code BarcodeCommon.generateCode()} — inilah yang
	 * dipakai calon dari kalangan pegawai atau pengguna umum.</li>
	 * </ol>
	 *
	 * <p>
	 * <b>Ketidaksesuaian yang perlu diketahui pada cabang guru.</b> Syarat cabang keempat memeriksa
	 * {@code getGuru().getNuptk()}, tetapi nilai yang <i>ditugaskan</i> diambil dari
	 * {@code getDosen().getNuptk()} — sumber yang berbeda dari yang diperiksa. Akibatnya, untuk calon
	 * yang hanya berelasi guru (dosen kosong) cabang ini melempar {@link NullPointerException}
	 * alih-alih menghasilkan kode, dan untuk calon yang berelasi guru sekaligus dosen kode yang
	 * tersimpan adalah NUPTK milik dosennya. Perilaku ini <b>tidak diubah</b> di sini; ia
	 * didokumentasikan apa adanya agar pemanggil tidak menganggap cabang guru dapat diandalkan.
	 * Kelas cerminnya, {@link AnggotaKoperasi}, memakai rantai penurunan yang setara — periksa
	 * keduanya bersama-sama bila cabang ini hendak diperbaiki.
	 * </p>
	 *
	 * <p>
	 * Perhatikan pula pemeriksaan terakhir {@code else if (kode == null)}: karena seluruh rantai ini
	 * hanya dimasuki ketika kode memang kosong, syarat itu selalu benar pada cabang tersebut.
	 * </p>
	 *
	 * @return kode keanggotaan yang diusulkan; dapat {@code null} bila sumber identitasnya ada tetapi
	 *         nomornya kosong
	 */
	@Column(unique = true)
	public String getKode() {

		if (kode == null || kode.trim().isEmpty()) {
			if (getMahasiswa() != null) {
				kode = getMahasiswa().getNim();
			} else if (getDosen() != null && getDosen().getNidn() != null && !getDosen().getNidn().trim().isEmpty()) {
				kode = getDosen().getNidn();
			} else if (getSiswa() != null && getSiswa().getNomorInduk() != null
					&& !getSiswa().getNomorInduk().trim().isEmpty()) {
				kode = getSiswa().getNomorInduk();
			} else if (getGuru() != null && getGuru().getNuptk() != null && !getGuru().getNuptk().trim().isEmpty()) {
				kode = getDosen().getNuptk();
			} else if (kode == null) {
				kode = BarcodeCommon.generateCode();
			}
		}

		return kode;
	}

	/**
	 * Mengisi kode keanggotaan secara eksplisit.
	 *
	 * <p>
	 * Nilai yang diisi di sini <b>bertahan</b>: {@link #getKode()} hanya menurunkan kode ketika field
	 * masih kosong, sehingga setter ini adalah cara memaksa kode tertentu (mis. saat memindahkan data
	 * lama). Kolomnya {@code unique}, jadi nilai kembar akan ditolak basis data saat disimpan.
	 * </p>
	 *
	 * @param kode kode keanggotaan yang diusulkan
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Nama calon anggota — <b>selalu diturunkan ulang</b> dari sumber identitasnya.
	 *
	 * <p>
	 * Enam baris pertama method ini memanggil keenam getter sumber identitas semata-mata untuk
	 * <b>efek sampingnya</b>: masing-masing menjalankan {@link GeneralValueObject#check(Object)}
	 * sehingga proksi malas terselesaikan lebih dulu dan rantai {@code if}/{@code else} di bawahnya
	 * dapat memeriksa field secara langsung tanpa memicu pemuatan di tengah jalan. Konsekuensi
	 * praktisnya: satu pemanggilan {@code getNama()} dapat menyentuh <b>enam relasi sekaligus</b>,
	 * bahkan ketika sumber identitas yang terisi hanya satu — mahal bila dipanggil per baris pada
	 * daftar yang panjang.
	 * </p>
	 *
	 * <p>
	 * Urutan penentuannya: mahasiswa, dosen, guru, siswa, pegawai, lalu nama akun
	 * {@link #getTbmuser()} sebagai pilihan terakhir. Perhatikan urutan ini <b>berbeda</b> dari
	 * urutan pada {@link #getKode()} dan {@link #getKodeIdentitas()}, yang menempatkan siswa sebelum
	 * guru.
	 * </p>
	 *
	 * <p>
	 * <b>Getter destruktif.</b> Field {@code nama} ditimpa hasil penurunan setiap kali sumber
	 * identitas apa pun terisi, sehingga {@link #setNama(String)} hanya bertahan pada formulir yang
	 * <b>sama sekali tidak</b> memiliki relasi sumber identitas. Karena Hibernate memakai getter ini
	 * saat menulis, nama hasil penurunan ikut tersimpan ke kolomnya — itulah yang membuat kolom
	 * {@code nama} tetap terisi dan dapat dicari lewat SQL meski nilainya turunan.
	 * </p>
	 *
	 * @return nama calon, atau {@code null} bila tidak ada sumber identitas dan nama tidak diisi
	 *         manual
	 */
	public String getNama() {
		getMahasiswa();
		getDosen();
		getPegawai();
		getTbmuser();
		getGuru();
		getSiswa();

		if (mahasiswa != null) {
			nama = mahasiswa.getNama();
		} else if (dosen != null) {
			nama = dosen.getNama();
		} else if (guru != null) {
			nama = guru.getNama();
		} else if (siswa != null) {
			nama = siswa.getNama();
		} else if (pegawai != null) {
			nama = pegawai.getNama();
		} else if (tbmuser != null) {
			nama = tbmuser.getUserNama();
		}
		return nama;
	}

	/**
	 * Membangkitkan usulan alamat surel untuk calon ini ketika ia tidak punya surel sendiri.
	 *
	 * <p>
	 * Bentuknya: {@link #getNama()} yang dihuruf-kecilkan, dibuang seluruh karakter selain huruf,
	 * angka, dan spasi, lalu spasinya dihapus; disambung angka acak tiga digit (100–998); disambung
	 * domain bawaan dari konfigurasi {@code alamat_email_default} (nilai bawaannya
	 * {@code "@eschool.id"}). Angka acak itulah yang membuat dua calon bernama sama tidak langsung
	 * bertabrakan — tetapi <b>tidak ada jaminan keunikan</b>: tidak ada pemeriksaan ke basis data,
	 * dan ruang acaknya hanya 899 kemungkinan per nama. Pemanggil yang membutuhkan surel benar-benar
	 * unik harus memeriksanya sendiri dan mengulang bila perlu.
	 * </p>
	 *
	 * <p>
	 * <b>Peringatan pemakaian:</b> method ini murni membangkitkan nilai dan <b>tidak</b> menyimpannya
	 * ke {@link #setEmail(String)}; pemanggil harus melakukannya sendiri. Ia juga akan melempar
	 * {@link NullPointerException} bila {@link #getNama()} bernilai {@code null}, yaitu pada formulir
	 * tanpa sumber identitas yang namanya belum diisi. Perhatikan pula bahwa
	 * {@code Common.getKonfigurasi(...)} <b>menulis nilai bawaannya ke basis data</b> bila kunci itu
	 * belum ada, sehingga pemanggilan pertama method ini punya efek samping menyemai konfigurasi.
	 * </p>
	 *
	 * @return usulan alamat surel
	 */
	public String generateEmail() {
		return getNama().toLowerCase().replaceAll("[^\\sa-zA-Z0-9]", "").replaceAll(" ", "")
				+ ThreadLocalRandom.current().nextLong(100, 999)
				+ Common.getKonfigurasi("alamat_email_default", "@eschool.id").getNilai().trim();
	}

	/**
	 * Mengisi nama calon secara manual.
	 *
	 * <p>
	 * Hanya bertahan pada formulir tanpa relasi sumber identitas sama sekali — lihat
	 * {@link #getNama()}.
	 * </p>
	 *
	 * @param nama nama calon
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Alamat tempat tinggal calon.
	 *
	 * <p>
	 * Bertipe {@code text} sehingga panjangnya tidak dibatasi. Bersama nama, nomor identitas,
	 * telepon, HP, dan surel, kolom ini termasuk <b>data pribadi</b> yang ikut terekam pada cuplikan
	 * revisi Envers — itulah alasan riwayat entitas {@code calon_anggota} digerbangi kunci menu
	 * {@code "anggota"} di {@code RevisiApiHelper} alih-alih terbuka bagi setiap pengguna yang login.
	 * </p>
	 *
	 * @return alamat calon, atau {@code null}
	 */
	@Column(name = "alamat", nullable = true, columnDefinition = "text")
	public String getAlamat() {
		return alamat;
	}

	/**
	 * Mengisi alamat calon.
	 *
	 * @param alamat alamat tempat tinggal; boleh {@code null}
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Koperasi tujuan pendaftaran — <b>penentu batas tenancy</b> formulir ini.
	 *
	 * <p>
	 * <b>Getter destruktif dengan cadangan dari konteks pembaca.</b> Bila kolomnya kosong, field
	 * diisi {@code Common.getCurrentKoperasi()}, yaitu koperasi yang sedang aktif pada sesi
	 * <b>pengguna yang membaca</b> — bukan sesuatu yang berasal dari data itu sendiri. Ada dua akibat
	 * yang perlu diperhitungkan:
	 * </p>
	 * <ul>
	 * <li>satu baris tanpa pemilik akan tampak menjadi milik koperasi yang berbeda bagi pembaca yang
	 * berbeda, sehingga hasil pembacaan tidak stabil;</li>
	 * <li>karena Hibernate memakai getter ini saat menulis, koperasi pembaca itu <b>ikut tersimpan
	 * secara permanen</b> pada penulisan berikutnya — kepemilikan data ditentukan oleh siapa yang
	 * kebetulan membukanya lebih dulu.</li>
	 * </ul>
	 * <p>
	 * Karena itu jangan menjadikan getter ini penyaring tenancy. Query yang mengambil data kelas ini
	 * harus menyaring kolom {@code koperasi} di tingkat query, dan tetap memperlakukan baris ber-nilai
	 * {@code null} sebagai baris yang belum jelas pemiliknya.
	 * </p>
	 *
	 * @return koperasi tujuan pendaftaran; dapat berasal dari konteks pembaca bila kolomnya kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "koperasi", nullable = true)
	public Koperasi getKoperasi() {
		koperasi = check(koperasi);
		if (koperasi == null) {
			koperasi = Common.getCurrentKoperasi();
		}
		return koperasi;
	}

	/**
	 * Mengisi koperasi tujuan pendaftaran secara eksplisit.
	 *
	 * <p>
	 * Mengisi nilai di sini adalah cara yang benar untuk menetapkan kepemilikan formulir, karena ia
	 * mencegah cadangan berbasis konteks pembaca pada {@link #getKoperasi()} ikut berjalan.
	 * </p>
	 *
	 * @param koperasi koperasi tujuan pendaftaran
	 */
	public void setKoperasi(Koperasi koperasi) {
		this.koperasi = koperasi;
	}

	/**
	 * Nomor identitas resmi calon — <b>selalu diturunkan ulang</b> dari sumber identitasnya.
	 *
	 * <p>
	 * Seperti {@link #getNama()}, method ini lebih dulu memanggil keenam getter sumber identitas
	 * untuk menyelesaikan proksi malas, lalu memilih nomor pertama yang <i>tidak kosong</i> menurut
	 * urutan: NIM mahasiswa, NIDN dosen, NUPTK guru, nomor induk siswa, nomor induk nasional siswa,
	 * lalu kode pegawai. Berbeda dari {@link #getKode()}, setiap cabang di sini memeriksa
	 * kekosongan nilainya lebih dulu, sehingga sumber yang nomornya kosong dilewati alih-alih
	 * menghentikan penurunan.
	 * </p>
	 *
	 * <p>
	 * Jenis nomor yang terpilih dicerminkan oleh {@link #getJenisIdentitasAnggotaKoperasi()}, yang
	 * memakai urutan pemeriksaan sedikit berbeda — keduanya sebaiknya dibaca berpasangan agar label
	 * jenis dan nomornya benar-benar sepadan.
	 * </p>
	 *
	 * <p>
	 * <b>Getter destruktif:</b> field ditimpa hasil penurunan, sehingga
	 * {@link #setKodeIdentitas(String)} hanya bertahan bila tidak ada sumber identitas yang membawa
	 * nomor. Nilainya termasuk data pribadi dan ikut terekam pada cuplikan revisi.
	 * </p>
	 *
	 * @return nomor identitas calon, atau {@code null} bila tidak ada sumber yang membawanya
	 */
	@Column(name = "kode_identitas", nullable = true)
	public String getKodeIdentitas() {
		getMahasiswa();
		getDosen();
		getPegawai();
		getTbmuser();
		getGuru();
		getSiswa();
		if (mahasiswa != null && mahasiswa.getNim() != null && !mahasiswa.getNim().trim().isEmpty()) {
			kodeIdentitas = mahasiswa.getNim();
		} else if (dosen != null && dosen.getNidn() != null && !dosen.getNidn().trim().isEmpty()) {
			kodeIdentitas = dosen.getNidn();
		} else if (guru != null && guru.getNuptk() != null && !guru.getNuptk().trim().isEmpty()) {
			kodeIdentitas = guru.getNuptk();
		} else if (siswa != null && siswa.getNomorInduk() != null && !siswa.getNomorInduk().trim().isEmpty()) {
			kodeIdentitas = siswa.getNomorInduk();
		} else if (siswa != null && siswa.getNomorIndukNasional() != null
				&& !siswa.getNomorIndukNasional().trim().isEmpty()) {
			kodeIdentitas = siswa.getNomorIndukNasional();
		} else if (pegawai != null && pegawai.getMycode() != null && !pegawai.getMycode().trim().isEmpty()) {
			kodeIdentitas = pegawai.getMycode();
		}

		return kodeIdentitas;
	}

	/**
	 * Mengisi nomor identitas calon secara manual.
	 *
	 * <p>
	 * Hanya bertahan bila tidak ada sumber identitas yang membawa nomor — lihat
	 * {@link #getKodeIdentitas()}.
	 * </p>
	 *
	 * @param kodeIdentitas nomor identitas calon
	 */
	public void setKodeIdentitas(String kodeIdentitas) {
		this.kodeIdentitas = kodeIdentitas;
	}

	/**
	 * Penggolongan calon dalam bentuk <b>teks bebas</b>.
	 *
	 * <p>
	 * Berbeda dari {@link #getTipeAnggotaKoperasi()} yang terstruktur dan diturunkan otomatis, kolom
	 * ini tidak divalidasi dan tidak pernah diisi sendiri oleh kelas ini. Untuk logika yang
	 * bergantung pada golongan calon, pakai relasi terstrukturnya; kolom ini hanya cocok sebagai
	 * catatan tambahan.
	 * </p>
	 *
	 * @return teks penggolongan, atau {@code null}
	 */
	public String getTipe() {
		return tipe;
	}

	/**
	 * Mengisi penggolongan calon dalam bentuk teks bebas.
	 *
	 * @param tipe teks penggolongan; boleh {@code null}
	 */
	public void setTipe(String tipe) {
		this.tipe = tipe;
	}

	/**
	 * Keterangan jenis identitas dalam bentuk <b>teks bebas</b>, pendamping tidak terstruktur bagi
	 * {@link #getJenisIdentitasAnggotaKoperasi()}. Tidak pernah diisi otomatis oleh kelas ini.
	 *
	 * @return teks jenis identitas, atau {@code null}
	 */
	public String getJenisIdentitas() {
		return jenisIdentitas;
	}

	/**
	 * Mengisi keterangan jenis identitas dalam bentuk teks bebas.
	 *
	 * @param jenisIdentitas teks jenis identitas; boleh {@code null}
	 */
	public void setJenisIdentitas(String jenisIdentitas) {
		this.jenisIdentitas = jenisIdentitas;
	}

	/**
	 * Nomor telepon tetap calon. Termasuk data pribadi yang ikut terekam pada cuplikan revisi Envers.
	 *
	 * @return nomor telepon tetap, atau {@code null}
	 */
	public String getTelp() {
		return telp;
	}

	/**
	 * Mengisi nomor telepon tetap calon.
	 *
	 * @param telp nomor telepon tetap; boleh {@code null}
	 */
	public void setTelp(String telp) {
		this.telp = telp;
	}

	/**
	 * Nomor telepon seluler calon — biasanya kanal kontak yang benar-benar dipakai. Termasuk data
	 * pribadi yang ikut terekam pada cuplikan revisi Envers.
	 *
	 * @return nomor telepon seluler, atau {@code null}
	 */
	public String getHp() {
		return hp;
	}

	/**
	 * Mengisi nomor telepon seluler calon.
	 *
	 * @param hp nomor telepon seluler; boleh {@code null}
	 */
	public void setHp(String hp) {
		this.hp = hp;
	}

	/**
	 * Alamat surel calon.
	 *
	 * <p>
	 * Nama kolomnya {@code email_nasabah} — sisa penamaan dari modul yang menyebut anggotanya
	 * "nasabah"; jangan tertukar dengan properti {@code email} pada entity lain. Bila calon tidak
	 * punya surel, {@link #generateEmail()} dapat membangkitkan usulan, tetapi hasilnya harus disetel
	 * lewat {@link #setEmail(String)} secara eksplisit karena method itu tidak menyimpan apa pun.
	 * </p>
	 *
	 * @return alamat surel, atau {@code null}
	 */
	@Column(name = "email_nasabah")
	public String getEmail() {
		return email;
	}

	/**
	 * Mengisi alamat surel calon.
	 *
	 * @param email alamat surel; boleh {@code null}
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Jenis nomor identitas yang dipakai calon — <b>selalu diturunkan ulang</b> dari sumber
	 * identitasnya.
	 *
	 * <p>
	 * Urutannya: mahasiswa menghasilkan {@code NIM}, dosen {@code NIDN}, siswa {@code NIS}, guru
	 * {@code NUPTK}; bila tidak satu pun sumber itu ada <i>dan</i> field masih kosong, nilainya
	 * jatuh ke {@code KTP} sebagai bawaan. Perhatikan urutan ini menempatkan <b>siswa sebelum
	 * guru</b>, kebalikan dari {@link #getKodeIdentitas()} yang memeriksa guru lebih dulu —
	 * akibatnya, pada formulir yang (tidak lazim) berelasi guru sekaligus siswa, label jenisnya bisa
	 * menyebut {@code NIS} sementara nomor yang tersimpan adalah NUPTK.
	 * </p>
	 *
	 * <p>
	 * <b>Getter destruktif</b>, dan berbeda dari {@link #getTipeAnggotaKoperasi()}, di sini
	 * {@code check(...)} dipanggil <b>sekali di akhir</b> untuk semua cabang, sehingga nilai bawaan
	 * maupun nilai kolom sama-sama terselesaikan proksinya. Nilai hasil
	 * {@link #setJenisIdentitasAnggotaKoperasi(JenisIdentitasAnggotaKoperasi)} hanya bertahan bila
	 * tidak ada sumber identitas berjenis akademik yang terisi.
	 * </p>
	 *
	 * @return jenis nomor identitas, tidak pernah {@code null} setelah dipanggil
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_identitas_anggota_koperasi", nullable = true)
	public JenisIdentitasAnggotaKoperasi getJenisIdentitasAnggotaKoperasi() {

		if (getMahasiswa() != null) {
			jenisIdentitasAnggotaKoperasi = ConstantValues.NIM;
		} else if (getDosen() != null) {
			jenisIdentitasAnggotaKoperasi = ConstantValues.NIDN;
		} else if (getSiswa() != null) {
			jenisIdentitasAnggotaKoperasi = ConstantValues.NIS;
		} else if (getGuru() != null) {
			jenisIdentitasAnggotaKoperasi = ConstantValues.NUPTK;
		} else if (jenisIdentitasAnggotaKoperasi == null) {
			jenisIdentitasAnggotaKoperasi = ConstantValues.KTP;
		}

		jenisIdentitasAnggotaKoperasi = check(jenisIdentitasAnggotaKoperasi);
		return jenisIdentitasAnggotaKoperasi;
	}

	/**
	 * Mengisi jenis nomor identitas secara manual.
	 *
	 * <p>
	 * Hanya bertahan bila tidak ada sumber identitas akademik (mahasiswa/dosen/siswa/guru) yang
	 * terisi — lihat {@link #getJenisIdentitasAnggotaKoperasi()}.
	 * </p>
	 *
	 * @param jenisIdentitasAnggotaKoperasi jenis nomor identitas
	 */
	public void setJenisIdentitasAnggotaKoperasi(JenisIdentitasAnggotaKoperasi jenisIdentitasAnggotaKoperasi) {
		this.jenisIdentitasAnggotaKoperasi = jenisIdentitasAnggotaKoperasi;
	}

	/**
	 * Tipe/golongan keanggotaan calon secara terstruktur — <b>selalu diturunkan ulang</b> dari sumber
	 * identitasnya.
	 *
	 * <p>
	 * Urutannya: mahasiswa menghasilkan {@code MAHASISWA}, dosen {@code DOSEN}, siswa {@code SISWA},
	 * guru {@code GURU}; bila tidak satu pun sumber itu ada <i>dan</i> field masih kosong, nilainya
	 * jatuh ke {@code PEGAWAI} sebagai bawaan. Nilai ini dipakai bersama
	 * {@link #getJenisAnggotaKoperasi()} untuk menentukan aturan yang berlaku bagi calon setelah ia
	 * menjadi anggota.
	 * </p>
	 *
	 * <p>
	 * <b>Perhatikan letak {@code check(...)}-nya.</b> Berbeda dari
	 * {@link #getJenisIdentitasAnggotaKoperasi()} yang memanggil {@code check(...)} sekali di akhir
	 * untuk semua cabang, di sini {@code check(...)} berada di cabang {@code else} terakhir saja —
	 * yaitu cabang "tidak ada sumber identitas <i>dan</i> field sudah terisi". Akibatnya, nilai yang
	 * disetel manual lewat {@link #setTipeAnggotaKoperasi(TipeAnggotaKoperasi)} pada formulir tanpa
	 * sumber identitas <b>tidak dilewatkan</b> penyelesaian proksi, sehingga dapat dikembalikan dalam
	 * keadaan proksi yang belum terinisialisasi. Perbedaan struktur antara dua getter yang tampak
	 * sepasang ini disengaja disebut di sini agar tidak dianggap seragam.
	 * </p>
	 *
	 * @return tipe keanggotaan, tidak pernah {@code null} setelah dipanggil
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tipe_anggota_koperasi", nullable = true)
	public TipeAnggotaKoperasi getTipeAnggotaKoperasi() {
		if (getMahasiswa() != null) {
			tipeAnggotaKoperasi = ConstantValues.MAHASISWA;
		} else if (getDosen() != null) {
			tipeAnggotaKoperasi = ConstantValues.DOSEN;
		} else if (getSiswa() != null) {
			tipeAnggotaKoperasi = ConstantValues.SISWA;
		} else if (getGuru() != null) {
			tipeAnggotaKoperasi = ConstantValues.GURU;
		} else if (tipeAnggotaKoperasi == null) {
			tipeAnggotaKoperasi = ConstantValues.PEGAWAI;
		} else {
			tipeAnggotaKoperasi = check(tipeAnggotaKoperasi);
		}
		return tipeAnggotaKoperasi;
	}

	/**
	 * Mengisi tipe keanggotaan secara manual.
	 *
	 * <p>
	 * Hanya bertahan bila tidak ada sumber identitas akademik yang terisi — lihat
	 * {@link #getTipeAnggotaKoperasi()}.
	 * </p>
	 *
	 * @param tipeAnggotaKoperasi tipe keanggotaan
	 */
	public void setTipeAnggotaKoperasi(TipeAnggotaKoperasi tipeAnggotaKoperasi) {
		this.tipeAnggotaKoperasi = tipeAnggotaKoperasi;
	}

	/**
	 * Penanda apakah formulir pendaftaran ini masih berlaku.
	 *
	 * <p>
	 * <b>Bendera satu arah.</b> Getter ini hanya menormalkan {@code null} menjadi {@code true} — dan
	 * menulis normalisasi itu balik ke field — sehingga baris lama yang kolomnya kosong ikut menjadi
	 * aktif begitu dibaca. Tidak ada satu pun aturan di kelas ini yang dapat membuat nilainya menjadi
	 * {@code false} dengan sendirinya; penonaktifan hanya terjadi lewat
	 * {@link #setAktif(Boolean)} yang dipanggil secara sadar.
	 * </p>
	 *
	 * <p>
	 * Ini <b>berbeda</b> dari {@code AnggotaKoperasi.getAktif()}, yang memaksa status menjadi tidak
	 * aktif ketika tanggal kedaluwarsa keanggotaan terlewati. Formulir pendaftaran di sini tidak
	 * mengenal kedaluwarsa: sebuah pengajuan yang tidak pernah ditindaklanjuti akan selamanya
	 * terbaca sebagai aktif. Bila daftar pendaftaran perlu dibatasi umurnya, pembatasan itu harus
	 * dilakukan pemanggil lewat {@link #getTanggal()}, bukan lewat properti ini.
	 * </p>
	 *
	 * @return {@code true} bila formulir masih berlaku; praktis tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menetapkan status berlaku formulir pendaftaran.
	 *
	 * <p>
	 * Satu-satunya cara menonaktifkan sebuah pendaftaran. Perlu diingat, mengisi {@code null} di sini
	 * <b>tidak</b> berarti "tidak diketahui": {@link #getAktif()} akan mengubahnya kembali menjadi
	 * {@code true} pada pembacaan berikutnya.
	 * </p>
	 *
	 * @param aktif {@code false} untuk menonaktifkan; {@code null} akan kembali menjadi {@code true}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Tanggal pendaftaran diajukan.
	 *
	 * <p>
	 * Disimpan sebagai {@code DATE} (tanpa komponen jam), karena yang bermakna hanyalah harinya.
	 * Nilai awalnya diisi waktu aplikasi saat objek dibuat, dan getter ini mengisinya lagi bila
	 * kebetulan kosong — sehingga formulir selalu bertanggal. Perlu dicatat bahwa "tanggal" yang
	 * terisi otomatis pada baris lama adalah tanggal <b>saat baris itu dibaca</b>, bukan tanggal
	 * pengajuan sebenarnya.
	 * </p>
	 *
	 * <p>
	 * Jangan tertukar dengan {@link #getTanggal_dirubah()}, yang mencatat kapan barisnya terakhir
	 * disunting.
	 * </p>
	 *
	 * @return tanggal pendaftaran, tidak pernah {@code null}
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggal() {
		if (tanggal == null) {
			tanggal = ais.ui.util.WaktuUtil.getDate();
		}
		return tanggal;
	}

	/**
	 * Mengisi tanggal pendaftaran.
	 *
	 * @param tanggal tanggal pengajuan; {@code null} akan diisi ulang saat dibaca
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Pengguna yang <b>membuat</b> formulir pendaftaran ini.
	 *
	 * <p>
	 * Berbeda dari field bayangan audit {@link #getOleh()}/{@link #getOlehId()} yang mencatat
	 * pengubah <i>terakhir</i> dan diisi otomatis interceptor, properti ini adalah data bisnis:
	 * ia menyimpan pembuatnya dan tidak berubah oleh penyuntingan berikutnya. Pada alur pendaftaran
	 * yang dijalankan petugas, nilainya adalah petugas — bukan calon anggotanya.
	 * </p>
	 *
	 * <p>
	 * Karena tidak ada kode yang mengisinya (lihat catatan alur persetujuan pada dokumentasi kelas),
	 * pengisian properti ini menjadi tanggung jawab alur yang kelak dibuat. Bila gerbang persetujuan
	 * ditambahkan, properti inilah yang dapat dipakai untuk memastikan penyetuju bukan pembuat
	 * formulir yang sama.
	 * </p>
	 *
	 * @return pengguna pembuat formulir, atau {@code null} bila tidak dicatat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = true)
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);
		return dibuatOleh;
	}

	/**
	 * Mencatat pengguna pembuat formulir pendaftaran ini.
	 *
	 * @param dibuatOleh pengguna pembuat; boleh {@code null}
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Sumber identitas calon bila ia seorang <b>guru</b>.
	 *
	 * <p>
	 * Menempati urutan ketiga pada {@link #getNama()} tetapi keempat pada {@link #getKode()} dan
	 * {@link #getKodeIdentitas()}. Perlu diperhatikan bahwa cabang guru di dalam {@link #getKode()}
	 * mengambil nilainya dari relasi dosen, bukan dari relasi ini — lihat peringatan pada dokumentasi
	 * method tersebut sebelum mengandalkan penurunan kode bagi calon berlatar guru.
	 * </p>
	 *
	 * @return guru sumber identitas, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru", nullable = true)
	public Guru getGuru() {
		guru = check(guru);
		return guru;
	}

	/**
	 * Menetapkan guru sebagai sumber identitas calon.
	 *
	 * @param guru guru sumber identitas; boleh {@code null}
	 */
	public void setGuru(Guru guru) {
		this.guru = guru;
	}

	/**
	 * Sumber identitas calon bila ia seorang <b>siswa</b>.
	 *
	 * <p>
	 * Satu-satunya sumber identitas yang menyediakan <b>dua</b> nomor pada
	 * {@link #getKodeIdentitas()}: nomor induk sekolah lebih dulu, dan nomor induk nasional sebagai
	 * cadangan bila yang pertama kosong.
	 * </p>
	 *
	 * @return siswa sumber identitas, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = true)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	/**
	 * Menetapkan siswa sebagai sumber identitas calon.
	 *
	 * @param siswa siswa sumber identitas; boleh {@code null}
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Satuan kerja yang menaungi calon — <b>diturunkan berlapis</b> dari sumber identitasnya, dengan
	 * rantai pemeriksaan terpanjang di kelas ini.
	 *
	 * <h3>Urutan penentuan</h3>
	 * <ol>
	 * <li>satuan kerja {@link #getPegawai()}, bila ada — paling meyakinkan karena melekat langsung
	 * pada barisnya;</li>
	 * <li>satuan kerja <b>jurusan</b> {@link #getDosen()}, hanya bila jurusan itu menyalakan penanda
	 * {@code dosenHarusPakaiSatuanKerja};</li>
	 * <li>satuan kerja <b>fakultas</b> dosen, dengan syarat penanda yang sama pada fakultas;</li>
	 * <li>satuan kerja <b>perguruan tinggi</b> dosen, dengan syarat penanda yang sama;</li>
	 * <li>satuan kerja <b>sekolah</b> {@link #getGuru()}, bila sekolah menyalakan
	 * {@code guruHarusPakaiSatuanKerja};</li>
	 * <li>satuan kerja pada akun {@link #getTbmuser()};</li>
	 * <li>bila semuanya gagal <i>dan</i> field masih kosong, barulah blok cadangan terakhir
	 * dijalankan.</li>
	 * </ol>
	 *
	 * <h3>Blok cadangan terakhir</h3>
	 * <p>
	 * Blok ini mengulang penurunan dari sekolah guru dan perguruan tinggi dosen, kali ini
	 * <b>tanpa</b> memeriksa penanda "harus pakai satuan kerja" — jaring pengaman untuk unit yang
	 * belum menyalakan penanda itu. Bila itu pun gagal dan barisnya <b>belum tersimpan</b>
	 * ({@code id == null}), satuan kerja ditebak dari <b>pengguna yang sedang aktif</b>
	 * ({@code Common.getCurrentUser().ambilSatuanKerja()}), dengan cadangan berikutnya berupa satuan
	 * kerja perpustakaan aktif. Pembatasan pada baris baru itu penting: baris yang sudah tersimpan
	 * tidak akan pernah mewarisi satuan kerja pembacanya, sehingga data lama tidak berubah-ubah
	 * mengikuti siapa yang membukanya — pembatasan yang tidak dimiliki {@link #getKoperasi()}.
	 * </p>
	 * <p>
	 * Ketiga percobaan di blok cadangan dibungkus {@code try}/{@code catch} yang mencatat kegagalan
	 * ke {@code ErrorAuditUtil} lalu melanjutkan. Ini <b>disengaja</b>: satuan kerja bersifat
	 * pelengkap, dan kegagalan menurunkannya — misalnya karena tidak ada pengguna aktif pada konteks
	 * latar belakang — tidak boleh menggagalkan pembacaan formulir.
	 * </p>
	 *
	 * <h3>Catatan perilaku</h3>
	 * <p>
	 * <b>Getter destruktif:</b> setiap cabang yang cocok menimpa field, sehingga
	 * {@link #setSatuanKerja(SatuanKerja)} hanya bertahan bila tidak satu pun cabang cocok. Berbeda
	 * dari getter turunan lain di kelas ini, hasilnya <b>tidak</b> dilewatkan
	 * {@link GeneralValueObject#check(Object)} — nilainya sudah berasal dari getter entity lain yang
	 * melakukannya masing-masing. Perlu disadari pula bahwa rantai ini menyentuh relasi berlapis
	 * (dosen &rarr; jurusan &rarr; satuan kerja, dan seterusnya), sehingga satu pemanggilannya dapat
	 * memicu beberapa query berantai.
	 * </p>
	 *
	 * @return satuan kerja calon, atau {@code null} bila tidak dapat diturunkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		if (getPegawai() != null && getPegawai().getSatuanKerja() != null) {
			satuanKerja = getPegawai().getSatuanKerja();
		} else if (getDosen() != null && getDosen().getJurusan() != null
				&& getDosen().getJurusan().getSatuanKerja() != null
				&& getDosen().getJurusan().getDosenHarusPakaiSatuanKerja()) {
			satuanKerja = getDosen().getJurusan().getSatuanKerja();
		}

		else if (getDosen() != null && getDosen().getFakultas() != null
				&& getDosen().getFakultas().getSatuanKerja() != null
				&& getDosen().getFakultas().getDosenHarusPakaiSatuanKerja()) {
			satuanKerja = getDosen().getFakultas().getSatuanKerja();
		}

		else if (getDosen() != null && getDosen().getPerguruanTinggi() != null
				&& getDosen().getPerguruanTinggi().getSatuanKerja() != null
				&& getDosen().getPerguruanTinggi().getDosenHarusPakaiSatuanKerja()) {
			satuanKerja = getDosen().getPerguruanTinggi().getSatuanKerja();
		}

		else if (getGuru() != null && getGuru().getSekolah() != null && getGuru().getSekolah().getSatuanKerja() != null
				&& getGuru().getSekolah().getGuruHarusPakaiSatuanKerja()) {
			satuanKerja = getGuru().getSekolah().getSatuanKerja();
		} else if (getTbmuser() != null && getTbmuser().getSatuanKerja() != null) {
			satuanKerja = getTbmuser().getSatuanKerja();
		} else if (satuanKerja == null) {
			guru = getGuru();
			dosen = getDosen();
			if (this.guru != null && guru.getSekolah() != null && guru.getSekolah().getSatuanKerja() != null) {
				try {
					this.satuanKerja = guru.getSekolah().getSatuanKerja();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/koperasi/CalonAnggotaKoperasi.java:501");
				}
			} else if (this.dosen != null && dosen.getPerguruanTinggi() != null
					&& dosen.getPerguruanTinggi().getSatuanKerja() != null) {
				try {
					this.satuanKerja = dosen.getPerguruanTinggi().getSatuanKerja();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/koperasi/CalonAnggotaKoperasi.java:507");
				}
			} else if (this.satuanKerja == null && this.id == null) {
				try {
					SatuanKerja satuanKerja = Common.getCurrentUser().ambilSatuanKerja();
					Perpustakaan currentPerpustakaan = Common.getCurrentPerpustakaan();
					if (satuanKerja == null && currentPerpustakaan != null) {
						satuanKerja = currentPerpustakaan.getSatuanKerja();
					}
					this.satuanKerja = satuanKerja;
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/koperasi/CalonAnggotaKoperasi.java:517");
				}
			}
		}
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Mengisi satuan kerja calon secara manual.
	 *
	 * <p>
	 * Hanya bertahan bila tidak satu pun cabang penurunan pada {@link #getSatuanKerja()} cocok.
	 * </p>
	 *
	 * @param satuanKerja satuan kerja; boleh {@code null}
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Usulan nama pengguna untuk akun yang akan dibuat bagi calon ini.
	 *
	 * <p>
	 * Getter menormalkan nilainya: string kosong atau yang hanya berisi spasi dikembalikan sebagai
	 * {@code null}, dan nilai tak-kosong dikembalikan dalam keadaan sudah dipangkas spasi tepinya —
	 * sehingga pemanggil tidak perlu membedakan "kosong" dari "belum diisi". Normalisasi ini
	 * <b>tidak</b> ditulis balik ke field, jadi kolom basis datanya tetap menyimpan nilai aslinya
	 * berikut spasi tepinya. Perbedaan itu berarti pencarian lewat SQL langsung atas kolom
	 * {@code userid} bisa memberi hasil berbeda dari pembacaan lewat getter ini.
	 * </p>
	 *
	 * <p>
	 * Ini <b>usulan</b>, bukan kredensial yang aktif: selama pendaftaran belum disetujui, tidak ada
	 * akun yang dapat dipakai untuk masuk dengan nilai ini.
	 * </p>
	 *
	 * @return usulan nama pengguna yang sudah dipangkas, atau {@code null} bila kosong
	 */
	public String getUserid() {
		return userid == null || userid.trim().isEmpty() ? null : userid.trim();
	}

	/**
	 * Mengisi usulan nama pengguna.
	 *
	 * @param userid usulan nama pengguna; boleh {@code null} atau kosong
	 */
	public void setUserid(String userid) {
		this.userid = userid;
	}

	/**
	 * Usulan kata sandi untuk akun yang akan dibuat bagi calon ini.
	 *
	 * <p>
	 * Dinormalkan dengan cara yang sama seperti {@link #getUserid()}: kosong menjadi {@code null},
	 * selebihnya dipangkas spasi tepinya tanpa ditulis balik ke field.
	 * </p>
	 *
	 * <p>
	 * <b>Catatan penting soal penyimpanan.</b> Kolom ini menyimpan nilainya <b>apa adanya</b> —
	 * tidak ada penyandian, penggaraman, maupun pencacahan yang dilakukan entity ini, dan tidak ada
	 * pula {@code @NotAudited} padanya, sehingga setiap nilai yang pernah tersimpan <b>ikut terekam
	 * ke tabel revisi Envers</b> dan tetap ada di sana meski kolom aslinya kemudian dikosongkan.
	 * Kelas cerminnya, {@link AnggotaKoperasi}, memperlakukan properti {@code pass}-nya dengan cara
	 * yang persis sama, sementara PIN transaksinya justru dicacah lewat
	 * {@code ais.common.security.PasswordHashService} — jadi mekanisme pencacahannya sudah tersedia
	 * di basis kode ini, hanya belum diterapkan pada properti sandi.
	 * </p>
	 * <p>
	 * Selama entity ini belum dipakai jalur kode mana pun (lihat catatan alur persetujuan pada
	 * dokumentasi kelas), kolom ini pada praktiknya tidak pernah terisi. Namun bila alur pendaftaran
	 * kelak dibuat, sandi usulan sebaiknya <b>tidak</b> disimpan di sini; lebih aman membangkitkannya
	 * pada saat persetujuan dan langsung menyerahkannya ke mekanisme kredensial akun, sehingga tidak
	 * ada sandi yang mengendap di tabel pendaftaran maupun di riwayat revisinya.
	 * </p>
	 *
	 * @return usulan kata sandi yang sudah dipangkas, atau {@code null} bila kosong
	 */
	public String getPass() {
		return pass == null || pass.trim().isEmpty() ? null : pass.trim();
	}

	/**
	 * Mengisi usulan kata sandi.
	 *
	 * <p>
	 * Nilai disimpan apa adanya tanpa pencacahan — lihat peringatan pada {@link #getPass()}.
	 * </p>
	 *
	 * @param pass usulan kata sandi; boleh {@code null} atau kosong
	 */
	public void setPass(String pass) {
		this.pass = pass;
	}

	/**
	 * Anggota koperasi resmi yang <b>lahir dari</b> formulir pendaftaran ini.
	 *
	 * <p>
	 * Inilah penanda hasil persetujuan: selama bernilai {@code null}, pendaftaran belum menghasilkan
	 * anggota; begitu terisi, formulir ini sudah dikonversi. Relasinya berpasangan dengan
	 * {@code AnggotaKoperasi.getCalonAnggotaKoperasi()} yang menunjuk balik ke sini, dan keduanya
	 * bersama-sama membentuk jejak asal-usul keanggotaan.
	 * </p>
	 *
	 * <p>
	 * Ingat bahwa hubungan ini <b>hanya jejak</b>, bukan pemilikan data: isi formulir disalin field
	 * demi field ke baris anggota, sehingga kedua baris berdiri sendiri dan tidak saling menyelaraskan
	 * diri setelah konversi. Membaca nama atau alamat lewat relasi ini karena itu bisa memberi hasil
	 * berbeda dari membacanya pada formulir.
	 * </p>
	 *
	 * <p>
	 * <b>Tidak ada gerbang di sini.</b> Relasi ini {@code nullable} dan tidak dijaga apa pun — tidak
	 * ada pemeriksaan bahwa anggota yang ditunjuk memang berasal dari formulir ini, tidak ada
	 * pemeriksaan koperasinya sama, dan tidak ada yang mencegah satu baris anggota ditunjuk oleh
	 * lebih dari satu formulir. Pemeriksaan semacam itu harus disediakan alur persetujuan yang
	 * memakainya.
	 * </p>
	 *
	 * @return anggota resmi hasil persetujuan, atau {@code null} bila pendaftaran belum dikonversi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "anggota_koperasi", nullable = true)
	public AnggotaKoperasi getAnggotaKoperasi() {
		anggotaKoperasi = check(anggotaKoperasi);
		return anggotaKoperasi;
	}

	/**
	 * Menautkan formulir ini ke baris anggota resmi hasil persetujuannya.
	 *
	 * <p>
	 * Dipanggil pada langkah terakhir konversi, setelah baris {@link AnggotaKoperasi} tersimpan dan
	 * memiliki id. Untuk melengkapi jejak dua arah, {@code AnggotaKoperasi.setCalonAnggotaKoperasi(...)}
	 * perlu diisi pula.
	 * </p>
	 *
	 * @param anggotaKoperasi anggota resmi hasil persetujuan; {@code null} berarti belum dikonversi
	 */
	public void setAnggotaKoperasi(AnggotaKoperasi anggotaKoperasi) {
		this.anggotaKoperasi = anggotaKoperasi;
	}
}
