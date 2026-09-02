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
 * Master <b>kelas paralel akademik</b> pada jalur <b>perguruan tinggi</b>: satu baris menyatakan
 * "ada kelas bernama {@code X} yang berlaku untuk cakupan Fakultas/Jurusan/Angkatan tertentu,
 * dengan Dosen PA bawaan {@code Y}". Contoh isian nyata adalah nama rombongan belajar seperti
 * {@code "A"}, {@code "B"}, {@code "Reguler Pagi"}, {@code "Karyawan"} &mdash; bukan mata kuliah,
 * bukan jadwal, dan bukan ruang.
 *
 * <p><b>Bukan kelas sekolah.</b> Meski namanya generik, entity ini dipakai <b>khusus</b> jalur
 * perguruan tinggi. Buktinya ada pada strukturnya sendiri: relasi {@link #getFakultas()},
 * {@link #getJurusan()} dan {@link #getDosenPaDefault()} (Dosen Pembimbing Akademik) tidak punya
 * padanan di jenjang SD/SMP/SMA. Jalur sekolah memakai entity terpisah
 * {@code ais.database.model.sekolah.KelasSiswa} beserta pasangannya {@code KelasSiswaPunyaSiswa};
 * jalur PMB memakai {@link KelasPmb}. Ketiganya <b>tidak</b> saling menggantikan.</p>
 *
 * <p>Memetakan tabel {@code public.kelas}. Dikelola lewat menu master
 * {@code /pages/master/kelas.zul} &rarr; {@code ais.action.master.KelasAction}; layar yang sama
 * juga disisipkan sebagai {@code MyInclude} di dalam layar Mahasiswa
 * ({@code ais.action.master.MahasiswaAction}), sehingga bisa muncul di dua tempat berbeda dengan
 * hak akses menu yang berbeda pula.</p>
 *
 * <h3>Hal terpenting: penautan ke mahasiswa memakai NAMA, bukan foreign key</h3>
 *
 * <p>Ini adalah kunci untuk memahami hampir semua kuirk kelas ini. Properti
 * {@code Mahasiswa.kelas} bertipe {@code String}, <b>bukan</b> relasi ke entity ini. Keanggotaan
 * mahasiswa pada sebuah kelas ditentukan dengan mencocokkan teks:</p>
 *
 * <pre>
 * Restrictions.ilike("kelas", kelas.getNama(), MatchMode.EXACT)
 * </pre>
 *
 * <p>Konsekuensi yang harus disadari sebelum menyentuh entity ini:</p>
 *
 * <ul>
 *   <li>Kolom jumlah mahasiswa di grid master dihitung ulang dengan query pencocokan teks di atas,
 *   satu query per baris yang tampil.</li>
 *   <li>Mengganti {@link #getNama() nama} berarti <b>memutus</b> seluruh keanggotaan. Karena itu
 *   {@code KelasAction.onSave()} selalu menjalankan SQL native
 *   {@code update mahasiswa set kelas='<baru>' where kelas ilike '<lama>'} setelah menyimpan
 *   &mdash; termasuk ketika nama sebenarnya tidak berubah.</li>
 *   <li>Keunikan {@link #getNama() nama} <b>tidak</b> dijamin database (tidak ada
 *   {@code unique = true} maupun indeks unik); hanya {@code KelasAction.checkNamaKelas()} di UI
 *   yang memeriksanya. Jalur non-UI (mis. endpoint reflektif) bisa membuat dua baris bernama sama,
 *   dan pencocokan teks di atas lalu menjadi ambigu.</li>
 *   <li>Laporan di {@code ais/action/report/format1/akademik/*} (18 berkas) hanya memakai object
 *   ini sebagai nilai pemilih, lalu meneruskan {@code getNama()} sebagai parameter teks (dengan
 *   sentinel {@code "-1"} bila tidak dipilih). Tidak ada satu pun yang menyimpan {@code id}.</li>
 * </ul>
 *
 * <p>Foreign key sungguhan ke tabel ini hanya datang dari lima tempat:
 * {@link KelasPmb#getKelas()}, {@link KelasPunyaMahasiswaTemporary#getKelas()},
 * {@link PenjadwalanMahasiswa#getKelas()}, {@link Perkuliahan#getKelasref()} dan
 * {@link DetailBiaya#getKelas()} (kelas sebagai salah satu dimensi tarif biaya). Perhatikan
 * {@link Perkuliahan} menyimpan <b>keduanya</b>: {@code kelas} bertipe {@code String} (dipakai
 * penyaringan laporan) dan {@code kelasref} sebagai FK ke entity ini &mdash; keduanya bisa
 * berbeda isi karena tidak ada yang menjaga sinkronisasinya.</p>
 *
 * <h3>Semantik cakupan: kosong berarti "semua"</h3>
 *
 * <p>{@link #getFakultas() fakultas}, {@link #getJurusan() jurusan} dan
 * {@link #getTahunAngkatan() tahun angkatan} berfungsi sebagai <b>penyempit cakupan</b>, bukan
 * data wajib. Teks bantuan di layar master menyatakannya eksplisit ("Kosongkan Jurusan jika kelas
 * ini berlaku untuk semua Jurusan"), dan grid menampilkan {@code "Semua"} untuk nilai kosong.
 * Cakupan ini dipakai saat sinkronisasi Dosen PA massal (lihat
 * {@link #getUpdateDosenPaSekarang()}) untuk membatasi mahasiswa mana yang ikut diperbarui.</p>
 *
 * <h3>Pengelompokan anggota kelas ini</h3>
 *
 * <ul>
 *   <li><b>Identitas &amp; audit</b>: {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()}, {@link #onUpdate()}, {@link #toString()}.</li>
 *   <li><b>Atribut master</b>: {@link #getNama()} (kunci alami), {@link #getKeterangan()},
 *   {@link #getAktif()}.</li>
 *   <li><b>Relasi cakupan</b>: {@link #getFakultas()}, {@link #getJurusan()},
 *   {@link #getTahunAngkatan()}.</li>
 *   <li><b>Dosen PA</b>: {@link #getDosenPaDefault()} beserta pemicunya
 *   {@link #getUpdateDosenPaSekarang()}.</li>
 * </ul>
 *
 * <h3>Hal non-obvious yang wajib diketahui sebelum menyentuh kelas ini</h3>
 *
 * <ol>
 *   <li><b>{@link #getNama()} membuang tanda kutip, dan itu bukan kosmetik.</b> Getter memangkas
 *   spasi lalu menghapus setiap {@code "} dan {@code '}. Nilai inilah yang dirangkai mentah ke
 *   dalam string SQL {@code update mahasiswa set kelas='...' where kelas ilike '...'} di
 *   {@code KelasAction.onSave()}; pembersihan kutip di getter inilah yang secara <i>de facto</i>
 *   mencegah SQL injection di sana. Jangan pernah "merapikan" getter ini tanpa lebih dulu
 *   memparameterkan query tersebut. Getter <b>tidak</b> menulis balik ke field, sehingga field
 *   {@code nama} di memori bisa tetap memuat kutip sementara nilai yang tersimpan sudah bersih
 *   (Hibernate memakai <i>property access</i>, lihat butir 3).</li>
 *   <li><b>{@link #getFakultas()} adalah getter yang MENULIS dan MENIMPA.</b> Bila
 *   {@link #getJurusan()} terisi, fakultas dipaksa mengikuti fakultas milik jurusan itu &mdash;
 *   pilihan Fakultas yang dibuat pengguna di dialog diabaikan tanpa pesan apa pun. Bila jurusan
 *   tersebut belum punya fakultas, hasilnya {@code null}. Pola dan konsekuensinya identik dengan
 *   {@link ItemBiayaPunyaAkun#getFakultas()}.</li>
 *   <li><b>Pemetaan memakai <i>property access</i>.</b> Anotasi menempel pada getter, jadi
 *   Hibernate membaca nilai lewat getter &mdash; termasuk saat menyusun snapshot dan saat
 *   INSERT/UPDATE. Setiap normalisasi yang dilakukan getter ({@link #getNama()},
 *   {@link #getAktif()}, {@link #getUpdateDosenPaSekarang()}, {@link #getFakultas()}) karena itu
 *   ikut menentukan isi database, bukan sekadar isi layar.</li>
 *   <li><b>{@link #getUpdateDosenPaSekarang()} BUKAN properti sementara.</b> Tidak ada
 *   {@code @Transient}, jadi centang "Update dosen PA sekarang" benar-benar <b>tersimpan</b> di
 *   database dan dimuat ulang saat dialog dibuka. Akibatnya centang itu bersifat <b>lengket</b>:
 *   sekali dicentang, setiap penyimpanan berikutnya atas baris yang sama akan kembali memicu
 *   penulisan ulang Dosen PA massal, meski pengguna hanya ingin mengubah keterangan.</li>
 *   <li><b>Nama kolom untuk properti tanpa {@code @Column}.</b> {@code tahunAngkatan},
 *   {@code aktif} dan {@code updateDosenPaSekarang} tidak punya {@code @Column}, sehingga nama
 *   kolomnya berasal dari {@code ais.database.hibernate.MyNamingStrategy} yang mewarisi
 *   {@code DefaultNamingStrategy} tanpa mengubah pemetaan properti sama sekali: nama properti
 *   apa adanya, yang oleh PostgreSQL diperlakukan huruf kecil ({@code tahunangkatan},
 *   {@code aktif}, {@code updatedosenpasekarang}).</li>
 *   <li><b>Deklarasi ulang {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} BUKAN
 *   duplikasi yang bisa dihapus.</b> {@link GeneralValueObject} adalah POJO abstrak biasa &mdash;
 *   bukan {@code @Entity} maupun {@code @MappedSuperclass} &mdash; sehingga Hibernate sama sekali
 *   tidak memetakan properti kelas induk. Setiap entity turunan wajib mendeklarasikan sendiri
 *   kolom-kolom itu agar terpetakan.</li>
 *   <li><b>Tidak ada jejak pembuat.</b> Ada {@code @PreUpdate} ({@link #onUpdate()}) tetapi tidak
 *   ada {@code @PrePersist}, jadi {@code oleh}/{@code olehId} baru terisi pada perubahan pertama,
 *   bukan saat baris dibuat. Riwayat lengkapnya bergantung pada {@code @Audited} (Envers), yang
 *   ditampilkan di grid lewat {@code RevisiHelper.createNewRevisi(Kelas.class, ...)}.</li>
 *   <li><b>Entity ini ikut dipanaskan saat aplikasi start.</b> {@code ais.common.InitData}
 *   mendaftarkan {@code Kelas.class} ke {@code InitDataHelper.initData(...)}, sehingga barisnya
 *   masuk cache {@code ConstantValues} dan bisa diambil lewat
 *   {@code ConstantValues.ambil(Kelas.class.getName(), id)} tanpa menyentuh database.</li>
 *   <li><b>Efek samping penyimpanan jauh lebih besar daripada yang terlihat.</b> Satu klik
 *   "Simpan" di layar master dapat memicu: (a) UPDATE massal kolom {@code mahasiswa.kelas}, dan
 *   (b) bila {@link #getUpdateDosenPaSekarang()} bernilai {@code true}, sebuah thread latar yang
 *   menjalankan satu {@code update mahasiswa set dosen=...} per mahasiswa <b>plus</b> penyimpanan
 *   {@code KrsMahasiswa} per mahasiswa. Perubahan sepele pada baris kelas berukuran besar
 *   karenanya bukan operasi ringan.</li>
 *   <li><b>Catatan kontrol akses.</b> Berbeda dari banyak layar lain, {@code KelasAction}
 *   memeriksa hak akses secara eksplisit ({@code CommonPrivilages.checkPrevilages} untuk
 *   READ/CREATE/UPDATE/DELETE) dan bahkan menonaktifkan checkbox "Aktif" di grid bila pengguna
 *   tidak berhak mengubah &mdash; sebuah <b>contoh positif</b>. Perlu dicatat
 *   {@code /pages/master/kelas.zul} <i>tidak</i> termasuk daftar {@code MUST_CHECKED} di
 *   {@code CommonPrivilages}, sehingga {@code Common.doCheckSecurity()} yang dipanggil di awal
 *   layar sebenarnya tidak menegakkan apa pun untuk halaman ini; perlindungan sepenuhnya berasal
 *   dari pemeriksaan eksplisit tadi.</li>
 * </ol>
 *
 * @see GeneralValueObject
 * @see KelasPmb
 * @see KelasPunyaMahasiswaTemporary
 * @see PenjadwalanMahasiswa
 * @see DetailBiaya
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kelas")
public class Kelas extends GeneralValueObject {

	/** 
	 * 
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama tabel {@code public.kelas}; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris; diisi {@link #onUpdate()}. */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris; diisi {@link #onUpdate()}. */
	private String olehId;

	/** @return ID pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum pernah di-update */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna pengubah. <b>Menolak diam-diam</b> nilai {@code null} maupun string
	 * kosong/spasi: nilai lama dipertahankan alih-alih ditimpa, sehingga jejak audit terakhir
	 * tidak hilang saat interceptor dipanggil tanpa konteks pengguna (mis. proses terjadwal atau
	 * thread latar sinkronisasi Dosen PA).
	 *
	 * @param olehId ID pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null}/kosong <b>diabaikan</b> dan nilai lama dipertahankan.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum pernah di-update */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mengisi {@code oleh}/{@code olehId}/{@code tanggal_dirubah}
	 * dari pengguna sesi berjalan lewat
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} tepat sebelum baris
	 * di-UPDATE. Tidak ada padanan {@code @PrePersist}, jadi pembuat baris tidak tercatat di
	 * kolom-kolom ini (lihat javadoc kelas). Pada baris deklarasi yang sama juga dideklarasikan
	 * field {@code tanggal_dirubah}, yang diinisialisasi ke waktu server saat objek dibuat
	 * ({@code ais.ui.util.WaktuUtil.getDate()}) sehingga baris baru tetap punya stempel waktu
	 * meski belum pernah di-update.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah stempel waktu perubahan terakhir baris ini */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return stempel waktu perubahan terakhir (kolom {@code tanggal_dirubah}, presisi TIMESTAMP) */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks object: mengembalikan <b>field</b> {@code nama} apa adanya, <b>bukan</b>
	 * hasil {@link #getNama()}. Perbedaannya nyata: nilai yang dikembalikan di sini belum
	 * di-trim dan masih memuat tanda kutip bila ada, dan bisa bernilai {@code null} untuk object
	 * yang belum diisi (mis. hasil {@code new Kelas()}). Dipakai antara lain oleh komponen
	 * pemilih ZK yang menampilkan object ini sebagai teks.
	 *
	 * @return isi field {@code nama} mentah; dapat {@code null}
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Nama kelas &mdash; kunci alami sekaligus satu-satunya penghubung ke
	 * {@code Mahasiswa.kelas}. Lihat {@link #getNama()}.
	 */
	private String nama;
	/** Catatan bebas; murni informatif, tidak dipakai logika mana pun. */
	private String keterangan;
	/** Penyempit cakupan tingkat fakultas; kosong berarti "semua". Lihat {@link #getFakultas()}. */
	private Fakultas fakultas;
	/** Penyempit cakupan tingkat jurusan/prodi; kosong berarti "semua". */
	private Jurusan jurusan;
	/** Penyempit cakupan tahun angkatan mahasiswa; {@code null} berarti "semua angkatan". */
	private Integer tahunAngkatan;
	/** Penanda baris masih dipakai; {@code null} diperlakukan sebagai aktif. */
	private Boolean aktif;

	/** Dosen Pembimbing Akademik bawaan untuk mahasiswa di kelas ini. */
	private Dosen dosenPaDefault;
	/** Pemicu sinkronisasi Dosen PA massal saat penyimpanan; lihat {@link #getUpdateDosenPaSekarang()}. */
	private Boolean updateDosenPaSekarang;

	/**
	 * Konstruktor tanpa argumen yang dibutuhkan Hibernate dan dipakai layar master saat menekan
	 * tombol "Tambah". Tidak mengisi apa pun; hanya {@code tanggal_dirubah} yang sudah terisi
	 * lewat inisialisasi field.
	 */
	public Kelas() {
	}

	/**
	 * Kunci utama, dibangkitkan database (kolom {@code id}, strategi {@code IDENTITY}, dan
	 * {@code insertable = false} sehingga nilainya tidak pernah dikirim pada INSERT).
	 *
	 * @return ID baris, atau {@code null} untuk object yang belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Praktis hanya dipakai Hibernate; kode aplikasi memuat baris lewat
	 * {@code KelasDao} atau {@code ConstantValues}.
	 *
	 * @param id ID baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama kelas, dinormalkan saat dibaca: di-trim lalu <b>seluruh tanda kutip ganda dan tunggal
	 * dibuang</b> memakai {@code StringUtils.replace}.
	 *
	 * <p>Normalisasi ini punya dua efek yang jauh melampaui kosmetik:</p>
	 * <ul>
	 *   <li><b>Menentukan isi database.</b> Anotasi {@code @Column} menempel pada getter, jadi
	 *   Hibernate menyimpan nilai yang sudah bersih ini &mdash; bukan isi field. Getter sendiri
	 *   <b>tidak</b> menulis balik ke field, sehingga object di memori dan baris di database bisa
	 *   berbeda sampai object dimuat ulang.</li>
	 *   <li><b>Menjadi pertahanan SQL injection satu-satunya</b> pada
	 *   {@code KelasAction.onSave()}, yang merangkai nilai ini langsung ke dalam literal string
	 *   SQL native {@code update mahasiswa set kelas='...' where kelas ilike '...'}. Menghapus
	 *   pembersihan kutip di sini akan langsung membuka celah injeksi di sana.</li>
	 * </ul>
	 *
	 * <p>Kuirk yang menyertainya: pemeriksaan duplikat {@code KelasAction.checkNamaKelas()}
	 * membandingkan nilai <b>mentah</b> dari kotak isian, bukan hasil getter ini, sehingga nama
	 * yang hanya berbeda pada tanda kutip lolos uji duplikat tetapi tersimpan sebagai nama yang
	 * sama persis.</p>
	 *
	 * @return nama kelas sudah di-trim dan tanpa tanda kutip; {@code null} bila field belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null
				: org.apache.commons.lang3.StringUtils
						.replace(org.apache.commons.lang3.StringUtils.replace(this.nama.trim(), "\"", ""), "'", "");
	}

	/**
	 * Menyetel nama kelas apa adanya (tanpa validasi maupun normalisasi &mdash; keduanya terjadi
	 * pada sisi baca, lihat {@link #getNama()}). Kolom ini {@code nullable = false} di database,
	 * jadi menyimpan baris dengan nama {@code null} akan gagal di level SQL; layar master
	 * mencegahnya lebih dulu dengan validasi "Kolom Nama Kelas belum diisi".
	 *
	 * @param nama nama kelas mentah dari isian pengguna
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Catatan bebas untuk baris ini (kolom {@code keterangan}, boleh kosong). Murni informatif:
	 * ditampilkan sebagai satu kolom di grid master dan diisi lewat kotak teks tiga baris di
	 * dialog, tetapi tidak dibaca logika bisnis mana pun.
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel catatan bebas.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Jurusan/program studi yang menjadi cakupan kelas ini; {@code null} berarti kelas berlaku
	 * untuk <b>semua</b> jurusan (grid menampilkannya sebagai {@code "Semua"}).
	 *
	 * <p>Seperti getter relasi lain di AIS, method ini memanggil
	 * {@link GeneralValueObject#check(Object)} untuk meresolusi proxy lazy dan
	 * <b>menetapkan hasilnya kembali ke field</b>. Penulisan balik itu hanya menyentuh object di
	 * memori (bukan database dengan sendirinya) dan tidak pernah melempar exception &mdash;
	 * kegagalan resolusi bersifat senyap dan mengembalikan argumen apa adanya.</p>
	 *
	 * <p>Nilai ini dipakai sebagai penyaring saat sinkronisasi Dosen PA massal: hanya mahasiswa
	 * pada jurusan ini yang ikut diperbarui.</p>
	 *
	 * @return jurusan cakupan, atau {@code null} untuk "semua jurusan"
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Menyetel jurusan cakupan. Perlu diingat konsekuensinya pada {@link #getFakultas()}: begitu
	 * jurusan terisi, fakultas akan dipaksa mengikuti fakultas milik jurusan ini dan pilihan
	 * fakultas yang disetel terpisah menjadi tidak berpengaruh.
	 *
	 * @param jurusan jurusan cakupan; {@code null} berarti "semua jurusan"
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Tahun angkatan mahasiswa yang menjadi cakupan kelas ini; {@code null} berarti "semua
	 * angkatan" (grid menampilkannya sebagai {@code "Semua"}).
	 *
	 * <p>Tidak beranotasi {@code @Column}, sehingga kolomnya dinamai mengikuti nama properti
	 * ({@code tahunangkatan} di PostgreSQL). Dibandingkan langsung dengan
	 * {@code Mahasiswa.tahunangkatan} saat sinkronisasi Dosen PA massal.</p>
	 *
	 * @return tahun angkatan empat digit, atau {@code null} untuk "semua angkatan"
	 */
	public Integer getTahunAngkatan() {
		return tahunAngkatan;
	}

	/**
	 * Menyetel tahun angkatan cakupan. Layar master mengisinya dari sebuah {@code Decimalbox},
	 * jadi nilai desimal apa pun dipangkas menjadi bilangan bulat sebelum sampai ke sini; tidak
	 * ada validasi rentang tahun sama sekali.
	 *
	 * @param tahunAngkatan tahun angkatan; {@code null} berarti "semua angkatan"
	 */
	public void setTahunAngkatan(Integer tahunAngkatan) {
		this.tahunAngkatan = tahunAngkatan;
	}

	/**
	 * Fakultas yang menjadi cakupan kelas ini &mdash; <b>getter yang mengubah state</b>, bukan
	 * pembaca murni.
	 *
	 * <p>Urutan yang dikerjakan method ini:</p>
	 * <ol>
	 *   <li>meresolusi proxy lazy {@code fakultas} lewat {@link GeneralValueObject#check(Object)}
	 *   dan menuliskan hasilnya ke field;</li>
	 *   <li>memanggil {@link #getJurusan()} (yang juga menulis ke field {@code jurusan});</li>
	 *   <li>bila jurusan terisi, <b>menimpa</b> field {@code fakultas} dengan
	 *   {@code jurusan.getFakultas()}.</li>
	 * </ol>
	 *
	 * <p><b>Konsekuensi yang perlu disadari</b> (perilaku ini identik dengan
	 * {@link ItemBiayaPunyaAkun#getFakultas()} dan kerabatnya):</p>
	 * <ul>
	 *   <li>Fakultas yang dipilih pengguna di dialog <b>diabaikan tanpa pesan</b> setiap kali
	 *   Jurusan juga terisi &mdash; nilai yang tersimpan selalu fakultas induk jurusan tersebut.
	 *   Karena pemetaan memakai property access, nilai hasil timpaan inilah yang ikut ter-flush
	 *   ke kolom {@code fakultas}.</li>
	 *   <li>Bila jurusan yang dipilih <b>belum</b> punya fakultas, hasilnya {@code null}: baris
	 *   yang tadinya bercakupan satu fakultas berubah menjadi bercakupan "semua fakultas".</li>
	 *   <li>Method ini tidak pernah melempar exception; kegagalan resolusi bersifat senyap.</li>
	 * </ul>
	 *
	 * @return fakultas cakupan (mengikuti jurusan bila jurusan terisi), atau {@code null} untuk
	 *         "semua fakultas"
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas")
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		jurusan = getJurusan();
		if (jurusan != null) {
			fakultas = jurusan.getFakultas();
		}

		return fakultas;
	}

	/**
	 * Menyetel fakultas cakupan. Nilai ini hanya bertahan selama {@link #getJurusan()} kosong;
	 * bila jurusan terisi, {@link #getFakultas()} akan menimpanya pada pembacaan berikutnya.
	 *
	 * @param fakultas fakultas cakupan; {@code null} berarti "semua fakultas"
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Status aktif baris ini, dengan {@code null} <b>diperlakukan sebagai aktif</b>
	 * ({@code true}). Normalisasi terjadi pada sisi baca saja &mdash; field tidak ditulis balik
	 * &mdash; tetapi karena pemetaan memakai property access, baris baru selalu tersimpan dengan
	 * nilai non-{@code null}.
	 *
	 * <p>Semantik "null = aktif" ini juga dipakai konsisten di sisi query: pencarian di layar
	 * master menyaring dengan {@code (aktif IS NULL OR aktif = true)}, bukan sekadar
	 * {@code aktif = true}, sehingga data lama yang kolomnya masih kosong tetap muncul.</p>
	 *
	 * <p>Checkbox "Aktif" di grid menyimpan perubahannya seketika lewat
	 * {@code Common.refreshSaveOrUpdate}, namun checkbox tersebut dinonaktifkan bila pengguna
	 * tidak berhak mengubah &mdash; berbeda dari beberapa layar master lain yang membiarkan
	 * checkbox grid tanpa gerbang hak akses.</p>
	 *
	 * @return {@code true} bila kelas masih dipakai; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif. Menyetel {@code null} tidak menonaktifkan baris &mdash; lihat
	 * semantik "null = aktif" di {@link #getAktif()}.
	 *
	 * @param aktif status aktif; {@code null} sama artinya dengan aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Dosen Pembimbing Akademik (PA) bawaan untuk mahasiswa di kelas ini.
	 *
	 * <p>Nilai ini <b>tidak</b> berlaku otomatis: ia hanya menjadi sumber data ketika
	 * {@link #getUpdateDosenPaSekarang()} bernilai {@code true} saat penyimpanan, yang lalu
	 * menuliskan {@code mahasiswa.dosen} dan {@code KrsMahasiswa.dosenPa} satu per satu untuk
	 * seluruh mahasiswa dalam cakupan. Mengubah baris ini saja tidak mengubah data mahasiswa
	 * mana pun.</p>
	 *
	 * <p>Seperti getter relasi lain, memanggil {@link GeneralValueObject#check(Object)} untuk
	 * meresolusi proxy lazy dan menulis hasilnya kembali ke field.</p>
	 *
	 * @return dosen PA bawaan, atau {@code null} bila belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen_pa_default")
	public Dosen getDosenPaDefault() {
		dosenPaDefault = check(dosenPaDefault);
		return dosenPaDefault;
	}

	/**
	 * Menyetel Dosen PA bawaan. Menyetel {@code null} sementara
	 * {@link #getUpdateDosenPaSekarang()} bernilai {@code true} berarti seluruh mahasiswa dalam
	 * cakupan akan <b>kehilangan</b> Dosen PA-nya ({@code mahasiswa.dosen} ditulis
	 * {@code null}), bukan sekadar dilewati.
	 *
	 * @param dosenPaDefault dosen PA bawaan; boleh {@code null}
	 */
	public void setDosenPaDefault(Dosen dosenPaDefault) {
		this.dosenPaDefault = dosenPaDefault;
	}

	/**
	 * Penanda "Update dosen PA sekarang": bila {@code true}, penyimpanan baris ini akan sekalian
	 * <b>menimpa Dosen PA seluruh mahasiswa</b> yang masuk cakupan kelas ini.
	 *
	 * <p>Meski terlihat seperti flag layar sekali pakai, properti ini <b>benar-benar tersimpan
	 * di database</b> (tidak ada {@code @Transient}; kolomnya dinamai mengikuti nama properti).
	 * Dialog master mengisi ulang checkbox-nya dari nilai tersimpan setiap kali dibuka, sehingga
	 * flag ini bersifat <b>lengket</b>: sekali dicentang dan disimpan, setiap penyimpanan
	 * berikutnya atas baris yang sama akan kembali menjalankan sinkronisasi massal walaupun
	 * pengguna hanya mengubah keterangan.</p>
	 *
	 * <p>Yang dijalankan saat flag aktif (di {@code KelasAction}, pada thread latar terpisah,
	 * setelah penyimpanan berhasil dan hanya bila {@link #getNama()} tidak kosong):</p>
	 * <ol>
	 *   <li>mengambil mahasiswa aktif yang {@code kelas}-nya sama persis dengan
	 *   {@link #getNama()}, disaring pula oleh {@link #getJurusan()}, {@link #getTahunAngkatan()}
	 *   dan {@link #getFakultas()} bila terisi;</li>
	 *   <li>untuk tiap mahasiswa: menjalankan SQL native {@code update mahasiswa set dosen=<id>}
	 *   lalu menyinkronkan {@code KrsMahasiswa} dan menyetel {@code dosenPa}-nya, masing-masing
	 *   dalam transaksi sendiri.</li>
	 * </ol>
	 *
	 * <p>Karena tiap mahasiswa diproses dalam transaksi terpisah, kegagalan di tengah
	 * meninggalkan hasil <b>separuh jadi</b> tanpa mekanisme pembatalan.</p>
	 *
	 * @return {@code true} bila sinkronisasi Dosen PA massal harus dijalankan saat penyimpanan;
	 *         {@code null} dinormalkan menjadi {@code false} sehingga tidak pernah {@code null}
	 */
	public Boolean getUpdateDosenPaSekarang() {
		return updateDosenPaSekarang == null ? false : updateDosenPaSekarang;
	}

	/**
	 * Menyetel penanda sinkronisasi Dosen PA massal. Nilai ini ikut disimpan permanen &mdash;
	 * lihat catatan "lengket" pada {@link #getUpdateDosenPaSekarang()}.
	 *
	 * @param updateDosenPaSekarang {@code true} untuk memicu sinkronisasi massal pada penyimpanan
	 *        berikutnya; {@code null} diperlakukan sebagai {@code false} saat dibaca
	 */
	public void setUpdateDosenPaSekarang(Boolean updateDosenPaSekarang) {
		this.updateDosenPaSekarang = updateDosenPaSekarang;
	}

}
