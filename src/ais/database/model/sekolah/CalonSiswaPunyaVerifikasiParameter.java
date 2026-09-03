package ais.database.model.sekolah;

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

import ais.database.model.GeneralValueObject;

/**
 * Entity Hibernate yang memetakan tabel {@code public.calon_siswa_punya_verifikasi_parameter} pada
 * modul <b>PSB/PPDB</b> (penerimaan siswa baru) — padanan versi sekolah dari
 * {@link ais.database.model.BiodataCalonMahasiswaPunyaVerifikasiParameter} di modul PMB.
 *
 * <h2>Domain (terverifikasi dari kode, bukan dari nama kelas)</h2>
 * <p>Ini adalah <b>entity transaksi</b>, bukan master. Satu baris = <b>satu entri yang didaftarkan
 * seorang calon siswa</b> ({@link CalonSiswa}) di bawah <b>satu kategori parameter verifikasi</b>
 * yang berlaku pada gelombang pendaftarannya, beserta <b>status verifikasinya</b>
 * ({@code verified}) dan catatan petugas ({@code keterangan}).</p>
 *
 * <p>Perhatikan bahwa relasinya <b>bukan</b> "satu baris per parameter" seperti yang biasa terjadi
 * pada tabel penghubung verifikasi berkas. Kategori (mis. "Sertifikat Prestasi", "Piagam
 * Lomba") didefinisikan sebagai master per gelombang oleh entity terpisah
 * {@code GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa} (tabel
 * {@code sekolah.gelombang_punya_parameter_verifikasi_calon_siswa}); untuk <b>satu</b> kategori itu,
 * seorang calon siswa boleh menambahkan <b>berapa pun</b> baris entity ini — masing-masing dengan
 * {@code nama} berupa teks bebas yang diketik sendiri (mis. judul sertifikatnya). Jadi kardinalitas
 * praktisnya adalah <i>N entri per (calon siswa, kategori)</i>.</p>
 *
 * <p>Kolom {@code parameter_verifikasi_calon_siswa} — meski namanya terdengar seperti "parameter
 * yang diverifikasi" — pada layar dirender di bawah judul kolom <b>"Tingkat"</b>. Isinya dipilih
 * dari himpunan {@code ParameterVerifikasiCalonSiswa} yang <i>ditautkan ke kategori tersebut</i>
 * (relasi {@code @ManyToMany} pada entity gelombang), sehingga secara semantik entity itu berperan
 * sebagai <b>daftar nilai tingkat/gradasi</b> (mis. Sekolah / Kabupaten / Provinsi / Nasional) yang
 * boleh dipilih untuk kategori bersangkutan. Nama kelasnya menyesatkan; jangan membacanya sebagai
 * "butir syarat berkas".</p>
 *
 * <h2>Berkas bukti</h2>
 * <p>Berkas bukti tidak disimpan di entity ini. {@code VerifikasiParameterPSBHelper} memasang
 * widget unggah/unduh {@code LampiranLain.createDownloadUploadFileLain(...)} dengan
 * {@code ref} = {@link #getId()} baris ini, {@code jenis} = <b>nama kelas berkualifikasi penuh</b>
 * ({@code CalonSiswaPunyaVerifikasiParameter.class.getName()}), dan label {@code "Bukti"}.
 * Konsekuensi non-obvious: nama kelas ini adalah <b>nilai data</b> di kolom
 * {@code lampiran_lain.jenis}, sehingga <b>mengganti nama/paket kelas ini akan memutus tautan
 * seluruh berkas bukti yang sudah terunggah</b>. String yang sama juga muncul hard-coded pada
 * daftar {@code daftarJenisDihapus} di {@code KonfigurasiNewAction} — pekerjaan pemindahan lampiran
 * ke Google Drive yang menghapus blob lokal setelah diunggah.</p>
 *
 * <h2>Siapa yang menulis baris ini</h2>
 * <p>Satu-satunya penulis maupun pembaca di seluruh basis kode adalah
 * {@code ais.action.master.sekolah.psb.VerifikasiParameterPSBHelper}, yang dipanggil dari dua
 * keluarga layar:</p>
 * <ol>
 * <li>{@code CalonSiswaAction} — menu petugas "Calon Siswa"; panel verifikasi hanya dibangun untuk
 * calon siswa yang <b>sudah tersimpan</b> ({@code calonSiswa.getId() != null}).</li>
 * <li>Keluarga formulir pendaftaran mandiri {@code PPDB1}, {@code PPDB2}, {@code PPDB3},
 * {@code PPDB_Alumni}, {@code PPDB_Simple}…{@code PPDB_Simple8} — formulir publik yang dipakai
 * calon siswa/orang tua.</li>
 * </ol>
 * <p>Helper membedakan dua mode berdasarkan {@code Common.getCurrentUser()}: bila ada user login,
 * baris dirender sebagai checkbox {@code verified} + textbox {@code keterangan} yang dapat diedit;
 * bila tidak ada (mode publik/anonim), ketiganya dirender sebagai {@code Label} read-only sehingga
 * pendaftar anonim tidak bisa memverifikasi dirinya sendiri.</p>
 *
 * <p><b>Penyimpanan langsung (bukan tertunda).</b> Centang dan keterangan disimpan <i>seketika</i>
 * lewat {@code Common.refreshSaveOrUpdate(...)} di dalam listener {@code onClick}/{@code onChange},
 * tanpa menunggu tombol "Simpan" formulir induk; demikian pula tombol hapus yang langsung memanggil
 * {@code Common.refreshDelete(...)}. Method {@code simpanVerifikasi()} yang dipanggil saat formulir
 * disimpan hanya berfungsi sebagai jaring pengaman (menulis ulang nilai yang sama).</p>
 *
 * <h2>Broken access control — TERVERIFIKASI dari kode pemanggil</h2>
 * <p>{@code VerifikasiParameterPSBHelper} <b>tidak memuat satu pun panggilan
 * {@code CommonPrivilages.checkPrevilages(...)}</b>. Satu-satunya gerbang adalah "ada user login
 * atau tidak". Akibatnya, siapa pun yang bisa membuka layar detail calon siswa — termasuk pengguna
 * yang <b>hanya berhak READ</b> pada menu Calon Siswa — dapat:</p>
 * <ul>
 * <li>menambah entri baru lewat tombol "Tambah &lt;kategori&gt;",</li>
 * <li><b>mencentang / membatalkan centang {@code verified}</b>,</li>
 * <li>mengubah {@code keterangan},</li>
 * <li><b>menghapus</b> baris beserta ketertautan berkas buktinya,</li>
 * <li>mengunggah/mengganti berkas bukti selama baris belum berstatus terverifikasi,</li>
 * </ul>
 * <p>dan semuanya <b>langsung tersimpan</b>. Kontrasnya tajam di file yang sama:
 * {@code CalonSiswaAction} <i>sudah</i> menghitung {@code edit =
 * CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE)} dan memakainya untuk menonaktifkan
 * tombol status kasar tingkat calon siswa ({@code terverifikasi.setDisabled(!edit)},
 * {@code diterima}, {@code ditolak}, …) — tetapi flag itu <b>tidak pernah diteruskan</b> ke
 * {@code VerifikasiParameterPSBHelper.tampilkanVerifikasi(...)}. Gerbang yang benar dipasang pada
 * saklar kasar, lalu dilewati sepenuhnya pada saklar halus di bawahnya.</p>
 *
 * <p><b>Cakupan tenant fail-open.</b> {@code CalonSiswaAction.initCriteria()} menyaring sekolah dan
 * yayasan hanya bila combo pencarian benar-benar terpilih; bila "Semua" (nilai {@code null})
 * terpilih, kedua klausanya menjadi literal {@code Restrictions.sqlRestriction("1=1")}. Ini persis
 * bentuk <b>fail-open</b> yang sudah berulang kali tercatat di modul {@code sekolah/}. Digabungkan
 * dengan paragraf sebelumnya, konsekuensinya: hak <i>baca</i> pada satu sekolah cukup untuk
 * mendaftar calon siswa <b>seluruh sekolah/yayasan</b> lalu <b>mengubah status verifikasi berkas
 * penerimaan mereka</b> — sebuah keputusan yang menentukan lolos/tidaknya seorang anak pada seleksi
 * PSB. Perubahan tetap terekam Envers ({@code @Audited}) dan beratribusi benar lewat
 * {@link #onUpdate()}, sehingga ini adalah cacat <i>otorisasi</i>, bukan cacat auditabilitas.</p>
 *
 * <p>Sisi baiknya, dua pola berbahaya lain <b>tidak</b> ditemukan di jalur ini: mode publik/anonim
 * tidak pernah merender checkbox verifikasi (jadi pendaftar tidak dapat memverifikasi dirinya
 * sendiri), dan tidak ada tombol mutasi massal — setiap perubahan berlaku satu baris.</p>
 *
 * <h2>Pola arsitektur repo — hasil pemeriksaan pada file ini</h2>
 * <ul>
 * <li><b>Getter penulis-balik/destruktif ({@code check()}):</b> <b>ADA, satu tempat</b> —
 * {@link #getCalonSiswa()} menugaskan ulang hasil {@code check(...)} ke fieldnya. Dua relasi lain
 * tidak memakainya karena dipetakan EAGER (lihat di bawah).</li>
 * <li><b>Getter yang "membalik kontrak" penyimpanan:</b> <b>ADA, dua tempat</b> —
 * {@link #getKeterangan()} mengubah {@code null} menjadi {@code ""} sekaligus mem-{@code trim},
 * dan {@link #getVerified()} mengubah {@code null} menjadi {@code false}. Karena entity ini
 * memakai <i>property access</i> (anotasi {@code @Id} ada di getter), nilai <b>hasil getter</b>
 * itulah yang dibaca Hibernate saat INSERT/dirty-check, bukan isi field. Baris warisan yang
 * bernilai {@code NULL} di database karena itu akan "sembuh sendiri" menjadi {@code ''}/{@code
 * false} pada flush berikutnya, disertai satu <b>revisi Envers palsu</b> yang tidak berasal dari
 * tindakan pengguna mana pun.</li>
 * <li><b>Getter destruktif yang mengosongkan data (pola {@code KelasSiswaPSB.getNama()}):</b>
 * <b>TIDAK ADA</b> di sini — {@link #getNama()} murni mengembalikan field apa adanya.</li>
 * <li><b>Filter tenant di dalam entity:</b> <b>TIDAK ADA</b>, dan memang tidak diharapkan ada;
 * penyaringan sepenuhnya tanggung jawab pemanggil (lihat catatan fail-open di atas).</li>
 * <li><b>Tombol mutasi massal tanpa gerbang:</b> <b>TIDAK ADA</b> pada jalur ini.</li>
 * </ul>
 *
 * <h2>Kuirk dan bug laten lain</h2>
 * <ul>
 * <li><b>NPE laten pada panel verifikasi.</b> Kolom join {@code parameter_verifikasi_calon_siswa}
 * dideklarasikan {@code nullable = true}, tetapi {@code VerifikasiParameterPSBHelper.reloadData()}
 * memanggil {@code getParameterVerifikasiCalonSiswa().getNama()} <b>tanpa penjaga null</b>. Satu
 * saja baris ber-FK {@code null} (data impor/SQL langsung, atau baris warisan dari sebelum dialog
 * "Tambah" mewajibkan "Tingkat") akan melempar {@code NullPointerException} dan merusak render
 * <b>seluruh</b> panel verifikasi calon siswa tersebut, bukan hanya baris itu. Jalur UI normal aman
 * karena dialog "Tambah" memvalidasi Tingkat wajib dipilih.</li>
 * <li><b>N+1 query.</b> {@link #getParameterVerifikasiCalonSiswa()} dan
 * {@link #getGelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa()} dipetakan
 * {@code @ManyToOne} <i>tanpa</i> {@code fetch}, yaitu EAGER menurut baku JPA, dan ditambah
 * {@code @Fetch(FetchMode.SELECT)} yang secara eksplisit melarang penggabungan JOIN. Setiap baris
 * yang dimuat karena itu memicu dua {@code SELECT} tambahan.</li>
 * <li><b>Skema tabel berbeda dari kerabatnya.</b> Entity ini dipetakan ke {@code schema = "public"},
 * sedangkan {@code ParameterVerifikasiCalonSiswa} dan
 * {@code GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa} ada di {@code schema =
 * "sekolah"}. Ini tampaknya warisan salin-tempel dari padanan PT-nya (yang memang berada di
 * {@code public}); hanya sedikit entity di paket {@code sekolah/} yang memakai {@code public}.</li>
 * <li><b>Divergensi dari padanan PT.</b>
 * {@link ais.database.model.BiodataCalonMahasiswaPunyaVerifikasiParameter} sudah dilengkapi hook
 * {@code @PrePersist} + {@code normalize()}, fallback nama otomatis
 * ({@code ambilNamaParameterDefault()}), {@code toString()}, dan anotasi
 * {@code @Column(columnDefinition = "text")} pada {@code keterangan}. Versi sekolah ini
 * <b>tidak punya satu pun</b> di antaranya: tanpa {@code toString()} entity ini akan tampil sebagai
 * {@code Object.toString()} default bila pernah dirender lewat jalur generik, dan tanpa
 * {@code columnDefinition} kolom {@code keterangan} memakai panjang baku pemetaan (255) alih-alih
 * {@code text} seperti di sisi PT.</li>
 * <li><b>Cascade ke calon siswa.</b> Ketiga relasi memakai {@code CascadeType.PERSIST} dan
 * {@code MERGE}. Menyimpan satu baris verifikasi karena itu ikut mem-{@code merge} objek
 * {@link CalonSiswa} yang tertaut — perilaku yang perlu diingat karena penyimpanan di sini terjadi
 * pada setiap klik checkbox.</li>
 * </ul>
 *
 * <h2>Catatan pewarisan</h2>
 * <p>Kelas ini {@code extends} {@link ais.database.model.GeneralValueObject}, yang <b>bukan</b>
 * {@code @Entity} maupun {@code @MappedSuperclass} melainkan POJO abstrak biasa. Hibernate karena
 * itu <b>tidak</b> memetakan properti milik induk. Deklarasi ulang {@code id}, {@code oleh},
 * {@code olehId}, dan {@code tanggal_dirubah} di kelas ini <b>bukan duplikasi keliru</b>, melainkan
 * keharusan teknis agar keempat kolom itu benar-benar terpetakan. Yang tetap diwarisi dan dipakai
 * adalah utilitas runtime seperti {@code check(...)} pada {@link #getCalonSiswa()}.</p>
 *
 * <h2>Pengelompokan method</h2>
 * <ol>
 * <li><b>Jejak audit</b> — {@link #getOleh()}/{@link #setOleh(String)},
 * {@link #getOlehId()}/{@link #setOlehId(String)},
 * {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan hook {@link #onUpdate()}.</li>
 * <li><b>Identitas</b> — {@link #CalonSiswaPunyaVerifikasiParameter()}, {@link #getId()}/
 * {@link #setId(Long)}.</li>
 * <li><b>Relasi</b> — {@link #getCalonSiswa()}/{@link #setCalonSiswa(CalonSiswa)},
 * {@link #getParameterVerifikasiCalonSiswa()} beserta setternya, dan
 * {@link #getGelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa()} beserta setternya.</li>
 * <li><b>Isi entri &amp; status</b> — {@link #getNama()}/{@link #setNama(String)},
 * {@link #getKeterangan()}/{@link #setKeterangan(String)},
 * {@link #getVerified()}/{@link #setVerified(Boolean)}.</li>
 * </ol>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.BiodataCalonMahasiswaPunyaVerifikasiParameter
 * @see CalonSiswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "calon_siswa_punya_verifikasi_parameter")

public class CalonSiswaPunyaVerifikasiParameter extends GeneralValueObject {

	/**
	 * Versi serialisasi Java.
	 *
	 * <p>Nilainya <b>sengaja sama persis</b> dengan milik
	 * {@link ais.database.model.BiodataCalonMahasiswaPunyaVerifikasiParameter} dan
	 * {@code ParameterVerifikasiCalonSiswa} — sisa salin-tempel saat modul sekolah diturunkan dari
	 * modul PT. Tidak berdampak fungsional karena kesetaraan {@code serialVersionUID} hanya
	 * diperiksa per kelas.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama {@code IDENTITY}; lihat {@link #getId()}. */
	private Long id;

	/** Nama pengguna pengubah terakhir (jejak audit); lihat {@link #getOleh()}. */
	private String oleh;

	/** Id pengguna pengubah terakhir (jejak audit); lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir; {@code null} bila baris belum pernah di-{@code UPDATE}
	 * @see #getOleh()
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah terakhir.
	 *
	 * <p><b>Nilai {@code null} atau kosong diabaikan diam-diam</b> (method langsung
	 * {@code return}), sehingga jejak audit lama tidak pernah terhapus oleh pemanggil yang lalai.
	 * Umumnya dipanggil {@code AuditTimestampInterceptor} dari {@link #onUpdate()}, bukan oleh kode
	 * layar.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau hanya berisi spasi
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong diabaikan diam-diam.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau hanya berisi spasi
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir; {@code null} bila baris belum pernah di-{@code UPDATE}
	 * @see #getOlehId()
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum pernyataan {@code UPDATE}
	 * baris ini dieksekusi.
	 *
	 * <p>Mendelegasikan ke {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang
	 * mengisi {@link #setOleh(String)}, {@link #setOlehId(String)}, dan
	 * {@link #setTanggal_dirubah(Date)} dari konteks pengguna aktif. Ketiga kolom jejak audit karena
	 * itu <b>tidak perlu</b> diisi manual oleh layar/Action.</p>
	 *
	 * <p><b>Hanya berlaku untuk UPDATE.</b> Tidak ada pasangan {@code @PrePersist} (berbeda dari
	 * padanan PT-nya), sehingga pada INSERT pertama {@code oleh}/{@code olehId} tetap {@code null}
	 * dan {@code tanggal_dirubah} mengandalkan nilai awal field di bawah. Karena baris entity ini
	 * dibuat lewat tombol "Tambah" pada panel verifikasi lalu langsung disimpan, praktisnya baris
	 * baru <b>tidak beratribusi pembuat</b> sampai ada perubahan berikutnya.</p>
	 *
	 * <p>Perhatikan juga interaksinya dengan {@link #getKeterangan()} dan {@link #getVerified()}:
	 * kedua getter itu menormalkan {@code null}, sehingga baris warisan ber-{@code NULL} bisa
	 * dianggap "kotor" dan memicu {@code UPDATE} — dan karenanya hook ini — oleh operasi yang secara
	 * semantik hanya membaca.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi waktu perubahan terakhir.
	 *
	 * <p>Umumnya dipanggil {@code AuditTimestampInterceptor} dari {@link #onUpdate()}, bukan oleh
	 * kode layar. Tidak ada validasi: {@code null} diterima apa adanya dan akan menulis {@code NULL}
	 * ke kolom.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini.
	 *
	 * <p>Field-nya diinisialisasi saat object dibuat dengan {@code ais.ui.util.WaktuUtil.getDate()}
	 * — waktu server yang sudah dinormalkan aplikasi, bukan {@code new Date()} mentah. Artinya
	 * baris yang belum pernah di-{@code UPDATE} pun tetap punya stempel waktu, yakni waktu
	 * instance-nya <i>dibuat di JVM</i> (bukan waktu commit).</p>
	 *
	 * @return waktu perubahan terakhir; praktis tidak pernah {@code null} untuk object yang dibuat
	 *         lewat konstruktor
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Calon siswa pemilik entri ini; lihat {@link #getCalonSiswa()}. */
	private CalonSiswa calonSiswa;

	/**
	 * "Tingkat" entri ini — nilai dari master {@code ParameterVerifikasiCalonSiswa}; lihat
	 * {@link #getParameterVerifikasiCalonSiswa()}.
	 */
	private ParameterVerifikasiCalonSiswa parameterVerifikasiCalonSiswa;

	/**
	 * Kategori parameter verifikasi (per gelombang PSB) tempat entri ini bernaung; lihat
	 * {@link #getGelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa()}.
	 */
	private GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa;

	/** Teks bebas judul entri yang diketik pengguna; lihat {@link #getNama()}. */
	private String nama;

	/** Catatan petugas verifikasi; lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Status terverifikasi; lihat {@link #getVerified()}. */
	private Boolean verified;

	/**
	 * Konstruktor tanpa argumen — wajib ada untuk Hibernate, dan dipakai
	 * {@code VerifikasiParameterPSBHelper} saat tombol "Simpan" pada dialog "Tambah
	 * &lt;kategori&gt;" ditekan.
	 *
	 * <p>Seluruh field dibiarkan {@code null} kecuali {@code tanggal_dirubah} yang terisi waktu
	 * sekarang lewat inisialisasi field. Perhatikan bahwa untuk instance baru ini
	 * {@link #getVerified()} sudah mengembalikan {@code false} dan {@link #getKeterangan()}
	 * mengembalikan {@code ""} meskipun kedua fieldnya masih {@code null} — dan karena entity
	 * memakai property access, nilai hasil getter itulah yang ditulis pada INSERT, sehingga baris
	 * yang lahir dari jalur UI tidak pernah menyimpan {@code NULL} pada kedua kolom tersebut.</p>
	 */
	public CalonSiswaPunyaVerifikasiParameter() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Dipetakan {@code IDENTITY} dengan {@code insertable = false}, jadi nilainya dibangkitkan
	 * database dan baru terisi setelah {@code INSERT} ter-flush. Nilai ini juga dipakai sebagai
	 * {@code ref} berkas bukti pada {@code lampiran_lain} (lihat dokumentasi kelas), sehingga
	 * <b>tidak boleh</b> diubah setelah baris punya lampiran.</p>
	 *
	 * <p>Karena berurutan dan mudah ditebak, id ini juga memudahkan enumerasi bila suatu saat ada
	 * jalur (REST/servlet) yang menerima id baris verifikasi mentah dari klien.</p>
	 *
	 * @return kunci utama; {@code null} untuk object yang belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama. Umumnya hanya dipanggil Hibernate; kode aplikasi tidak boleh memakainya.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan calon siswa pemilik entri verifikasi ini.
	 *
	 * <p><b>Getter penulis-balik (write-back).</b> Relasi ini dipetakan {@code FetchType.LAZY},
	 * sehingga isinya bisa berupa proxy Hibernate yang tidak lagi terhubung ke session. Baris
	 * {@code calonSiswa = check(calonSiswa)} memanggil utilitas
	 * {@link ais.database.model.GeneralValueObject#check(Object)} untuk meresolusi proxy tersebut
	 * (lewat cache, session aktif, atau — sebagai penyelamat terakhir — session baru) <b>dan
	 * menugaskan hasilnya kembali ke field</b>. Efek sampingnya: memanggil getter ini dapat
	 * mengubah state object, dan dapat menyebabkan Hibernate menganggap entity "kotor" pada flush
	 * berikutnya meskipun pemanggil hanya bermaksud membaca.</p>
	 *
	 * <p>Penugasan ulang itu <b>bukan bug</b> dan tidak boleh "dirapikan" menjadi
	 * {@code return check(calonSiswa);} — pola yang sama dipakai ribuan getter relasi di seluruh
	 * aplikasi dan justru menjadi cache tingkat instance untuk panggilan berikutnya.</p>
	 *
	 * @return calon siswa pemilik entri; {@code null} bila kolom {@code calon_siswa} kosong
	 *         (kolomnya {@code nullable = true})
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "calon_siswa", nullable = true)
	public CalonSiswa getCalonSiswa() {
		calonSiswa = check(calonSiswa);
		return calonSiswa;
	}

	/**
	 * Menautkan entri ini ke seorang calon siswa.
	 *
	 * <p>Dipanggil {@code VerifikasiParameterPSBHelper} saat entri baru dibuat lewat dialog
	 * "Tambah". Karena relasinya ber-{@code cascade} {@code PERSIST}/{@code MERGE}, menyimpan entri
	 * ini ikut mem-{@code merge} objek calon siswa yang ditautkan.</p>
	 *
	 * @param calonSiswa calon siswa pemilik entri; boleh {@code null}
	 */
	public void setCalonSiswa(CalonSiswa calonSiswa) {
		this.calonSiswa = calonSiswa;
	}

	/**
	 * Mengembalikan catatan petugas untuk entri ini.
	 *
	 * <p><b>Getter yang membalik kontrak penyimpanan.</b> Method ini tidak pernah mengembalikan
	 * {@code null}: {@code null} dipetakan menjadi {@code ""} dan nilai yang ada di-{@code trim}.
	 * Karena entity ini memakai <i>property access</i> (anotasi {@code @Id} berada di getter),
	 * nilai hasil getter inilah yang dibaca Hibernate saat INSERT dan saat dirty-check — bukan isi
	 * field. Konsekuensinya:</p>
	 * <ul>
	 * <li>kolom {@code keterangan} tidak pernah berisi {@code NULL} untuk baris yang lahir dari
	 * jalur aplikasi;</li>
	 * <li>baris warisan ber-{@code NULL} (impor/SQL langsung) akan "sembuh sendiri" menjadi
	 * {@code ''} pada flush pertama setelah dimuat, disertai satu <b>revisi Envers palsu</b> yang
	 * tidak berasal dari tindakan pengguna;</li>
	 * <li>spasi di awal/akhir yang sengaja diketik petugas hilang senyap.</li>
	 * </ul>
	 *
	 * <p>Berbeda dari padanan PT-nya, getter ini <b>tidak</b> beranotasi
	 * {@code @Column(columnDefinition = "text")}, sehingga metadata pemetaan memakai panjang baku
	 * (255) untuk kolom {@code keterangan}.</p>
	 *
	 * @return catatan petugas yang sudah di-{@code trim}; {@code ""} bila belum diisi — tidak
	 *         pernah {@code null}
	 */
	public String getKeterangan() {
		return keterangan == null ? "" : keterangan.trim();
	}

	/**
	 * Mengisi catatan petugas untuk entri ini.
	 *
	 * <p>Dipanggil {@code VerifikasiParameterPSBHelper} dari listener {@code onChange} textbox
	 * keterangan, lalu langsung diikuti {@code Common.refreshSaveOrUpdate(...)}; juga dari
	 * {@code simpanVerifikasi()} sebagai jaring pengaman. Nilai disimpan apa adanya —
	 * normalisasinya terjadi di {@link #getKeterangan()}.</p>
	 *
	 * <p>Pada layar, textbox ini <b>dinonaktifkan</b> begitu {@code verified} tercentang, sehingga
	 * keterangan praktis hanya bisa diubah selama entri belum terverifikasi.</p>
	 *
	 * @param keterangan catatan petugas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan judul entri — teks bebas yang diketik pengguna pada dialog "Tambah
	 * &lt;kategori&gt;" (mis. nama sertifikat atau prestasi yang diajukan).
	 *
	 * <p>Dipetakan sebagai kolom {@code text} sehingga panjangnya tidak dibatasi. Berbeda dari
	 * {@link #getKeterangan()} dan {@link #getVerified()}, getter ini <b>tidak</b> menormalkan apa
	 * pun dan <b>tidak</b> destruktif — nilai {@code null} dikembalikan sebagai {@code null}. Juga
	 * tidak ada fallback ke nama parameter seperti pada padanan PT-nya, sehingga baris ber-{@code
	 * nama} kosong akan tampil sebagai checkbox tanpa label pada panel verifikasi. Jalur UI normal
	 * mencegah hal itu karena dialog "Tambah" menolak nama kosong.</p>
	 *
	 * @return judul entri; boleh {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getNama() {
		return nama;
	}

	/**
	 * Mengisi judul entri.
	 *
	 * <p>Dipanggil sekali saja pada jalur normal, yaitu dari dialog "Tambah &lt;kategori&gt;" di
	 * {@code VerifikasiParameterPSBHelper} dengan nilai yang sudah di-{@code trim}. Panel
	 * verifikasi tidak menyediakan cara mengubah judul entri setelah tersimpan.</p>
	 *
	 * @param nama judul entri
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan "Tingkat" entri ini.
	 *
	 * <p>Meski tipenya bernama {@code ParameterVerifikasiCalonSiswa}, pada layar nilai ini dirender
	 * di bawah judul kolom <b>"Tingkat"</b> dan dipilih dari himpunan yang ditautkan ke kategori
	 * ({@code getParameterVerifikasiCalonSiswas()} milik entity gelombang). Perlakukan sebagai
	 * gradasi/tingkatan entri, bukan sebagai butir syarat berkas.</p>
	 *
	 * <p>Dipetakan {@code @ManyToOne} <b>tanpa</b> atribut {@code fetch} — artinya EAGER menurut
	 * baku JPA — dan ditambah {@code @Fetch(FetchMode.SELECT)} yang melarang penggabungan JOIN,
	 * sehingga setiap baris yang dimuat memicu satu {@code SELECT} tambahan. Karena sudah
	 * terinisialisasi saat pemuatan, getter ini <b>tidak</b> memerlukan
	 * {@link ais.database.model.GeneralValueObject#check(Object)} seperti
	 * {@link #getCalonSiswa()}.</p>
	 *
	 * <p><b>Peringatan bug laten:</b> kolom joinnya {@code nullable = true}, tetapi
	 * {@code VerifikasiParameterPSBHelper.reloadData()} langsung memanggil {@code .getNama()} atas
	 * hasil getter ini tanpa penjaga null. Satu baris ber-FK {@code null} akan melempar
	 * {@code NullPointerException} dan merusak render seluruh panel verifikasi calon siswa
	 * bersangkutan.</p>
	 *
	 * @return tingkat entri; {@code null} bila kolomnya kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "parameter_verifikasi_calon_siswa", nullable = true)
	public ParameterVerifikasiCalonSiswa getParameterVerifikasiCalonSiswa() {
		return parameterVerifikasiCalonSiswa;
	}

	/**
	 * Menetapkan "Tingkat" entri ini.
	 *
	 * <p>Dipanggil dari dialog "Tambah &lt;kategori&gt;" dengan nilai combobox "Tingkat" yang
	 * validasinya mewajibkan pilihan terisi. Tidak ada jalur UI untuk mengubahnya setelah entri
	 * tersimpan.</p>
	 *
	 * @param parameterVerifikasiCalonSiswa tingkat entri; boleh {@code null} secara teknis, tetapi
	 *                                      lihat peringatan NPE pada
	 *                                      {@link #getParameterVerifikasiCalonSiswa()}
	 */
	public void setParameterVerifikasiCalonSiswa(ParameterVerifikasiCalonSiswa parameterVerifikasiCalonSiswa) {
		this.parameterVerifikasiCalonSiswa = parameterVerifikasiCalonSiswa;
	}

	/**
	 * Mengembalikan status terverifikasi entri ini.
	 *
	 * <p><b>Getter yang membalik kontrak penyimpanan</b>, sama seperti {@link #getKeterangan()}:
	 * {@code null} dipetakan menjadi {@code false}. Karena entity memakai property access, nilai
	 * inilah yang ditulis Hibernate, sehingga kolom {@code verified} tidak pernah berisi
	 * {@code NULL} untuk baris yang lahir dari jalur aplikasi, dan baris warisan ber-{@code NULL}
	 * akan "sembuh sendiri" menjadi {@code false} pada flush berikutnya beserta satu revisi Envers
	 * palsu. Konsekuensi lain: tidak ada cara membedakan "belum diperiksa" dari "diperiksa dan
	 * dinyatakan tidak memenuhi" — keduanya {@code false}.</p>
	 *
	 * <p>Nilai ini mengendalikan tiga hal pada panel verifikasi: checkbox tercentang, textbox
	 * keterangan dinonaktifkan, dan tombol hapus serta widget unggah berkas bukti disembunyikan.
	 * Dengan kata lain, mencentang entri <b>mengunci</b> entri tersebut dari perubahan lebih
	 * lanjut — kecuali centangnya dibuka lagi, yang juga tidak dilindungi privilese apa pun (lihat
	 * dokumentasi kelas).</p>
	 *
	 * @return {@code true} bila entri sudah dinyatakan terverifikasi; {@code false} bila belum atau
	 *         kolomnya {@code NULL} — tidak pernah {@code null}
	 */
	public Boolean getVerified() {
		return verified == null ? false : verified;
	}

	/**
	 * Menetapkan status terverifikasi entri ini.
	 *
	 * <p>Satu-satunya penulis nyata adalah {@code VerifikasiParameterPSBHelper}: listener
	 * {@code onClick} checkbox / {@code onChange} textbox keterangan (langsung diikuti
	 * {@code Common.refreshSaveOrUpdate(...)}, jadi <b>tersimpan seketika</b>) dan
	 * {@code simpanVerifikasi()} saat formulir induk disimpan.</p>
	 *
	 * <p><b>Catatan keamanan:</b> jalur pemanggil sama sekali tidak memeriksa
	 * {@code CommonPrivilages.checkPrevilages(...)}; syaratnya hanya "ada user login". Rinciannya
	 * ada pada dokumentasi kelas.</p>
	 *
	 * @param verified status terverifikasi; {@code null} akan dibaca sebagai {@code false} oleh
	 *                 {@link #getVerified()}
	 */
	public void setVerified(Boolean verified) {
		this.verified = verified;
	}

	/**
	 * Mengembalikan kategori parameter verifikasi (per gelombang PSB) tempat entri ini bernaung.
	 *
	 * <p>Menunjuk ke entity {@code GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa}
	 * (tabel {@code sekolah.gelombang_punya_parameter_verifikasi_calon_siswa}) — master yang
	 * menyatakan kategori apa saja yang berlaku pada satu gelombang pendaftaran, lengkap dengan
	 * {@code judul}/{@code nama} untuk label layar dan himpunan tingkat yang boleh dipilih.
	 * Pasangan (calon siswa, kategori) inilah kunci pencarian yang dipakai
	 * {@code VerifikasiParameterPSBHelper.reloadData()} untuk membangun sub-grid entri.</p>
	 *
	 * <p>Gelombang pendaftaran <b>tidak</b> disimpan langsung di entity ini; ia diturunkan lewat
	 * kategori. Perhatikan bahwa relasi ini tidak pernah diperbarui setelah entri dibuat, sehingga
	 * entri tetap menempel pada kategori gelombang aslinya bahkan bila calon siswa kemudian
	 * dipindahkan ke gelombang lain — akibatnya entri tersebut tidak lagi muncul di panel
	 * verifikasinya (yang menyaring kategori berdasarkan gelombang aktif) meski barisnya masih ada
	 * di database.</p>
	 *
	 * <p>Seperti {@link #getParameterVerifikasiCalonSiswa()}, relasi ini EAGER (tanpa atribut
	 * {@code fetch}) dengan {@code @Fetch(FetchMode.SELECT)} — satu {@code SELECT} tambahan per
	 * baris — sehingga tidak memerlukan
	 * {@link ais.database.model.GeneralValueObject#check(Object)}.</p>
	 *
	 * @return kategori parameter verifikasi milik gelombang; {@code null} bila kolomnya kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "gelombang_punya_parameter_verifikasi_calon_siswa", nullable = true)
	public GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa getGelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa() {
		return gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa;
	}

	/**
	 * Menautkan entri ini ke sebuah kategori parameter verifikasi milik satu gelombang PSB.
	 *
	 * <p>Dipanggil sekali dari dialog "Tambah &lt;kategori&gt;" di
	 * {@code VerifikasiParameterPSBHelper}, dengan kategori yang sedang dirender. Menyetel nilai
	 * yang keliru akan membuat entri "hilang" dari panel verifikasi karena pencariannya menyaring
	 * tepat pada pasangan (calon siswa, kategori).</p>
	 *
	 * @param gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa kategori parameter
	 *                                                                  verifikasi; boleh {@code null}
	 */
	public void setGelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa(
			GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa) {
		this.gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa = gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa;
	}

}
