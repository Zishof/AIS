package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
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
 * Entity <b>anggota keluarga yang dilampirkan pada satu formulir pengajuan beasiswa</b> &mdash;
 * satu baris tabel {@code public.keadaan_keluarga_pengajuan_beasiswa} mewakili satu orang dalam
 * tanggungan/rumah tangga pemohon: siapa namanya, apa hubungannya dengan pemohon (ayah, ibu,
 * adik, kakak, dan seterusnya), apa pekerjaannya, berapa umurnya, plus satu kolom keterangan
 * bebas.
 *
 * <p>Kelas ini adalah <b>anak dari</b> {@link PengajuanBeasiswa}. Formulir induk sudah menyimpan
 * profil sosial-ekonomi keluarga dalam bentuk satu-baris (nama dan pekerjaan bapak/ibu, kelas
 * penghasilan, alamat, kondisi rumah); kelas ini melengkapinya menjadi <b>daftar berulang</b>
 * &mdash; nol sampai banyak orang per pengajuan &mdash; sehingga jumlah tanggungan dan komposisi
 * keluarga bisa direkam sebagai baris terpisah, bukan sebagai teks bebas. Lihat Javadoc
 * {@link PengajuanBeasiswa} untuk gambaran utuh alur beasiswa; jangan diduplikasi di sini.</p>
 *
 * <h2>Sifat data: sensitif secara privasi</h2>
 * <p>Isi tabel ini adalah <b>data pribadi anggota keluarga pihak ketiga</b> &mdash; orang-orang
 * yang bahkan tidak punya akun di sistem ini dan tidak pernah menyetujui apa pun. Digabung dengan
 * kolom-kolom {@link PengajuanBeasiswa} (alamat lengkap, kelas penghasilan orang tua, kondisi
 * rumah tinggal, narasi alasan memohon bantuan), satu baris di sini melengkapi profil kemiskinan
 * sebuah rumah tangga yang dapat diidentifikasi. Perlakukan sebagai data rahasia: <b>jangan</b>
 * menambahkannya ke log, ke pesan galat, ke label komponen yang bisa dilihat pengguna lain, atau
 * ke endpoint baru tanpa penyaringan kepemilikan. Perhatikan juga bahwa {@code @Audited}
 * membuat setiap revisi tersimpan permanen di tabel bayangan Envers, jadi menghapus barisnya
 * <b>tidak</b> menghapus riwayat isinya.</p>
 *
 * <h2>Status pemakaian: yatim di sisi kode</h2>
 * <p>Penelusuran seluruh pohon sumber (Java, ZUL, JSP, XML) menemukan kelas ini <b>hanya</b>
 * disebut di tiga tempat: pendaftaran pemetaan {@code hibernate.cfg.xml}, dan dua rujukan Javadoc
 * di {@link PengajuanBeasiswa} serta {@link Beasiswa}. Artinya:</p>
 * <ul>
 * <li><b>Tidak ada DAO</b>, tidak ada {@code Action}, tidak ada berkas {@code .zul} yang membaca
 * atau menulis entity ini. Layar {@code pengajuan_beasiswa.zul} tidak punya grid anggota
 * keluarga.</li>
 * <li><b>Tidak ada koleksi {@code @OneToMany}</b> di {@link PengajuanBeasiswa} yang menunjuk ke
 * sini. Relasinya searah dari sisi anak, jadi untuk mengambil "semua anggota keluarga pengajuan
 * X" pemanggil harus menulis Criteria/HQL sendiri dengan
 * {@code Restrictions.eq("pengajuanBeasiswa", pengajuan)}.</li>
 * <li>Tabel dan tabel bayangan Envers-nya tetap terbentuk oleh {@code hbm2ddl}, tapi <b>tidak
 * pernah diisi lewat jalur aplikasi biasa</b>. Baris yang ada, kalau ada, berasal dari migrasi
 * data atau SQL manual.</li>
 * </ul>
 * <p>Manifes {@code webapp/WEB-INF/generic-crud/manifests/general_value_object_inventory.csv}
 * mencatat kelas ini sebagai kandidat CRUD generik berstatus {@code ELIGIBLE_METADATA_FIRST}
 * namun <b>masih dinonaktifkan secara default</b>. Bila suatu saat layar generiknya dinyalakan,
 * baca dulu bagian "Jalur akses yang sudah ada" di bawah &mdash; layar itu akan langsung
 * mengekspos data pribadi keluarga tanpa penyaringan kepemilikan bawaan.</p>
 *
 * <h2>Jalur akses yang sudah ada (penting sebelum tabel ini diisi)</h2>
 * <p>Meskipun tidak ada layar khusus, entity ini <b>sudah dapat dijangkau</b> lewat endpoint
 * reflektif generik yang menerima nama kelas dari klien. Ini bukan spekulasi, melainkan hasil
 * pembacaan kode:</p>
 * <ul>
 * <li>{@code /Api} aksi {@code dataRinci}
 * ({@code ais.action.servlet.api.ElearningApiUtil#dataRinci}) memuat <i>kelas apa pun</i>
 * berdasarkan {@code Class.forName(request.getString("class"))} dan {@code id} sembarang, lalu
 * menyalin grafnya sedalam parameter {@code deep} (bawaan 6, ditentukan klien). Satu-satunya
 * syarat adalah token login yang sah &mdash; <b>milik siapa pun</b>, termasuk mahasiswa lain.
 * Karena relasi {@link #getPengajuanBeasiswa()} bersifat <i>eager</i>, membaca satu baris di sini
 * ikut menarik seluruh formulir induk beserta {@code Mahasiswa} dan {@code Beasiswa}-nya.</li>
 * <li>{@code /Data} aksi {@code daftar}/{@code load}
 * ({@code ais.action.servlet.api.DaftarDataService}) menerima nama kelas dari klien dengan
 * satu-satunya penyaring "harus turunan {@link GeneralValueObject}" &mdash; yang dipenuhi kelas
 * ini &mdash; dan membangun {@code Criteria} <b>tanpa penyaringan kepemilikan sama sekali</b>
 * (satu-satunya penyempitan otomatis adalah filter sekolah untuk pengguna ber-{@code Guru}).</li>
 * <li>{@code /Data} aksi {@code simpanDataRinci}/{@code hapusDataRinci} menulis dan menghapus
 * lewat jalur reflektif yang sama; gerbang izin per-kelas di
 * {@code ElearningApiUtil#prosesSimpan} hanya dipasang untuk dua kelas master e-Kantin, kelas
 * lain lolos begitu saja.</li>
 * </ul>
 * <p>Konsekuensinya: <b>begitu tabel ini terisi, isinya langsung terbaca lintas pemohon</b> tanpa
 * perlu ada layar yang menampilkannya. Ini sifat endpoint generiknya, bukan sifat kelas ini,
 * karena itu jangan diperbaiki di berkas ini.</p>
 *
 * <h2>Pemetaan Hibernate</h2>
 * <p>{@code @Entity} + {@code @Table(schema = "public", name = "keadaan_keluarga_pengajuan_beasiswa")},
 * dengan {@code dynamicInsert}/{@code dynamicUpdate} aktif (hanya kolom yang benar-benar berubah
 * ikut dalam {@code INSERT}/{@code UPDATE}) dan {@code @Audited} sehingga setiap perubahan direkam
 * Hibernate Envers ke tabel bayangan {@code keadaan_keluarga_pengajuan_beasiswa_AUD}.</p>
 * <p>Pemetaan memakai <b>property access</b> (anotasi menempel pada getter), sehingga
 * <b>setiap pasangan getter/setter yang tidak dianotasi {@code @Transient} tetap dipetakan</b>.
 * Karena {@code ais.database.hibernate.MyNamingStrategy} adalah turunan
 * {@code DefaultNamingStrategy} (nama kolom = nama properti apa adanya), properti yang tidak
 * diberi {@code @Column} &mdash; {@code hubungan}, {@code pekerjaan}, {@code umur}, {@code oleh},
 * {@code olehId}, {@code tanggal_dirubah} &mdash; jatuh ke kolom bernama persis seperti
 * propertinya. Hanya {@code id}, {@code nama}, {@code keterangan}, dan kolom relasi
 * {@code pengajuan_beasiswa} yang namanya ditentukan eksplisit.</p>
 * <p>Relasi {@link #getPengajuanBeasiswa()} memakai {@code @ManyToOne} dengan
 * {@code cascade = {PERSIST, MERGE}} dan {@code @Fetch(FetchMode.SELECT)}: fetch tetap
 * <i>eager</i> (bawaan {@code @ManyToOne}) tetapi lewat {@code SELECT} terpisah, bukan
 * {@code JOIN}. Menampilkan satu halaman daftar anggota keluarga karena itu menghasilkan satu
 * query tambahan per baris (pola N+1 bawaan generator aslinya).</p>
 *
 * <h2>Hubungan dengan {@link GeneralValueObject}</h2>
 * <p>Kelas induk <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass}, melainkan POJO
 * abstrak biasa; Hibernate <b>tidak memetakan properti milik induk</b>. Karena itu deklarasi
 * ulang {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} di kelas ini
 * <b>bukan duplikasi yang keliru, melainkan keharusan teknis</b> &mdash; tanpa deklarasi ulang,
 * kolom-kolom tersebut tidak akan pernah dipetakan. Pola yang sama muncul di hampir semua entity
 * repo ini.</p>
 * <p>Kelas ini <b>tidak memakai</b> {@link GeneralValueObject#check(Object)} pada getter
 * relasinya; {@link #getPengajuanBeasiswa()} mengembalikan field apa adanya. Akibatnya <b>tidak
 * ada satu pun method di kelas ini yang menyentuh {@code Session} Hibernate</b> &mdash; tidak
 * membuka, tidak menutup, dan tidak me-resolve proxy. Semua akses database terjadi di luar kelas
 * ini.</p>
 *
 * <h2>Pengelompokan method</h2>
 * <ol>
 * <li><b>Jejak audit</b> &mdash; {@link #getOleh()}/{@link #setOleh(String)},
 * {@link #getOlehId()}/{@link #setOlehId(String)},
 * {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan hook {@code @PreUpdate}
 * {@link #onUpdate()}.</li>
 * <li><b>Identitas &amp; deskripsi</b> &mdash; {@link #getId()}/{@link #setId(Long)},
 * {@link #getNama()}/{@link #setNama(String)},
 * {@link #getKeterangan()}/{@link #setKeterangan(String)}, {@link #toString()}.</li>
 * <li><b>Atribut anggota keluarga</b> &mdash; {@link #getHubungan()}/{@link #setHubungan(String)},
 * {@link #getPekerjaan()}/{@link #setPekerjaan(String)}, {@link #getUmur()}/
 * {@link #setUmur(Integer)}.</li>
 * <li><b>Relasi induk</b> &mdash; {@link #getPengajuanBeasiswa()}/
 * {@link #setPengajuanBeasiswa(PengajuanBeasiswa)}.</li>
 * </ol>
 * <p>Tidak ada method statis, tidak ada helper query, tidak ada method bisnis, dan tidak ada
 * validasi apa pun di kelas ini: seluruh isinya adalah aksesor properti ditambah
 * {@link #toString()} dan hook audit. Konstruktor {@link #KeadaanKeluargaPengajuanBeasiswa()}
 * adalah konstruktor tanpa argumen yang dibutuhkan Hibernate.</p>
 *
 * <h2>Verifikasi pola-pola berulang repo ini</h2>
 * <p>Diperiksa langsung atas kode kelas ini, bukan diasumsikan dari entity lain:</p>
 * <ul>
 * <li><b>Getter yang menutup sesi Hibernate:</b> <b>tidak ada</b>. Tidak ada satu pun
 * {@code Session}, {@code HibernateUtil}, atau {@code Common} yang disentuh dari sini.</li>
 * <li><b>Getter destruktif</b> (mengosongkan/memindahkan state saat dibaca): <b>tidak ada</b>.
 * Semua getter hanya membaca field.</li>
 * <li><b>Getter yang menulis balik ke field:</b> <b>tidak ada</b> dalam bentuk kuat. Namun
 * {@link #getNama()} melakukan {@code trim()} pada nilai yang dikembalikan &mdash; lihat bagian
 * Kuirk, karena kombinasi <i>property access</i> + <i>dirty checking</i> membuatnya tetap bisa
 * memicu {@code UPDATE} tanpa ada yang menekan Simpan, meskipun field-nya sendiri tidak
 * diubah.</li>
 * <li><b>Setter yang menolak nilai kosong diam-diam:</b> <b>ada dua</b> &mdash;
 * {@link #setOleh(String)} dan {@link #setOlehId(String)}. Setter properti bisnis lainnya polos
 * (menerima {@code null} apa adanya).</li>
 * <li><b>Flag {@code aktif} satu arah</b> (pola {@code KasKecil}, {@code KasBesar}, dsb.):
 * <b>tidak berlaku</b>. Kelas ini sama sekali tidak punya field {@code aktif} maupun kolomnya,
 * jadi tidak ada penonaktifan lunak &mdash; penghapusan baris hanya bisa fisik.</li>
 * </ul>
 *
 * <h2>Kuirk yang perlu diketahui sebelum menyunting</h2>
 * <ul>
 * <li><b>{@link #getNama()} men-{@code trim()} setiap kali dibaca, tapi tidak menulis balik ke
 * field.</b> Karena pemetaan memakai property access, Hibernate memanggil getter itu saat
 * menyusun {@code INSERT} maupun saat <i>dirty checking</i>. Bila kolom di basis data berisi
 * nilai berspasi di ujung, snapshot hasil pemuatan (nilai mentah) akan berbeda dari hasil getter
 * (nilai ter-{@code trim}), sehingga sekadar <b>memuat baris ke dalam session</b> dapat memicu
 * {@code UPDATE} perapian sekaligus <b>revisi Envers baru</b>. Efeknya idempoten (hanya terjadi
 * sekali per baris kotor), tapi jangan kaget melihat revisi audit yang tak ada penulisnya.</li>
 * <li><b>{@link #getKeterangan()} sengaja tidak di-{@code trim()}</b>, berbeda dari
 * {@link #getNama()}. Ketidakkonsistenan bawaan generator; jangan diseragamkan tanpa
 * permintaan, karena menyeragamkannya akan menambah perilaku menulis-ulang di atas pada satu
 * kolom lagi.</li>
 * <li><b>{@link #toString()} membaca field {@code nama} langsung, bukan lewat
 * {@link #getNama()}</b>, sehingga bebas dari efek {@code trim} di atas. Ini <b>kebalikan</b>
 * dari {@code PengajuanBeasiswa.toString()} yang justru memanggil getter dan karenanya memicu
 * penulisan balik. Perbedaan ini penting: jangan menyalin pola dari kelas induk ke sini.</li>
 * <li><b>{@link #toString()} tetap membocorkan nama orang.</b> Hasilnya
 * <code>{id}-{nama}</code>, yaitu nama anggota keluarga apa adanya. Karena ZK memakai
 * {@code toString()} sebagai label komponen dan banyak jalur log mencetak object mentah, method
 * ini adalah titik kebocoran paling mudah untuk data yang seharusnya rahasia. Nilainya juga
 * {@code null} untuk baris yang belum di-{@code flush} (menjadi teks {@code "null-null"}), jadi
 * jangan diandalkan sebagai identitas.</li>
 * <li><b>{@code nama} dipetakan {@code nullable = false}, tapi tidak ada validasi di lapisan
 * Java.</b> Baris tanpa nama baru gagal di tingkat basis data, dengan pesan galat mentah.</li>
 * <li><b>{@code pengajuan_beasiswa} juga {@code nullable = false}</b> dan tidak divalidasi di
 * sini: anggota keluarga tanpa formulir induk akan ditolak basis data, bukan oleh kode.</li>
 * <li><b>{@code umur} adalah {@link Integer} polos tanpa satuan dan tanpa batas.</b> Tidak ada
 * yang memaksa nilainya masuk akal (negatif atau 999 sama-sama tersimpan), dan tidak ada kode
 * yang menurunkannya dari tanggal lahir &mdash; ia disimpan sebagai angka beku yang menjadi usang
 * seiring waktu. Jangan dipakai untuk perhitungan usia yang akurat.</li>
 * <li><b>{@code hubungan} dan {@code pekerjaan} adalah teks bebas</b>, bukan relasi ke master
 * mana pun (tidak ke {@code Pekerjaan}, tidak ke daftar hubungan keluarga). Tidak ada
 * normalisasi, jadi "Ayah"/"ayah"/"Bapak" akan hidup berdampingan. Panjangnya mengikuti bawaan
 * Hibernate (255 karakter) karena tidak diberi {@code @Column}.</li>
 * <li><b>{@code serialVersionUID} kelas ini sama persis dengan milik
 * {@link PengajuanBeasiswa}</b> karena keduanya lahir dari template generator yang sama.
 * Kebetulan ini tidak berbahaya (nilainya dibandingkan per kelas), tapi jangan dijadikan patokan
 * bahwa kedua kelas sekerabat secara warisan.</li>
 * <li><b>Komentar Javadoc asli generator berbunyi "Bank generated by hbm2java"</b> &mdash; sisa
 * salin-tempel dari entity {@code Bank}, sama sekali tidak berhubungan. Sudah digantikan oleh
 * dokumen ini.</li>
 * <li><b>{@code cascade = {PERSIST, MERGE}} mengarah ke induk, bukan sebaliknya.</b> Menyimpan
 * satu anggota keluarga bisa ikut menyimpan/menggabungkan {@link PengajuanBeasiswa} yang
 * ditempelkan padanya. Menghapus anggota keluarga tidak menyentuh induk (tidak ada
 * {@code REMOVE}), dan menghapus induk <b>tidak</b> menghapus anak-anaknya &mdash; baris yatim
 * dengan foreign key menggantung akan ditolak/tertinggal tergantung batasan basis data.</li>
 * </ul>
 *
 * @see GeneralValueObject
 * @see PengajuanBeasiswa
 * @see Beasiswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true, 
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "keadaan_keluarga_pengajuan_beasiswa")

public class KeadaanKeluargaPengajuanBeasiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya dipatok tetap supaya sesi ZK yang di-<i>passivate</i> lalu
	 * diaktifkan kembali (atau data yang dikirim antar-node) tetap kompatibel walau field kelas
	 * bertambah. Jangan diubah tanpa alasan kuat.
	 *
	 * <p>Nilainya <b>identik</b> dengan milik {@link PengajuanBeasiswa}; lihat catatan di Javadoc
	 * kelas.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama; dipetakan pada {@link #getId()}. */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris ini; dipetakan pada {@link #getOleh()}. */
	private String oleh;

	/** Id pengguna terakhir yang mengubah baris ini; dipetakan pada {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Id pengguna yang terakhir menyunting baris ini (kolom {@code olehId}).
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah disunting lewat jalur yang
	 *         mengisi jejak audit (misalnya baris hasil migrasi/SQL manual &mdash; yang untuk
	 *         entity ini justru merupakan mayoritas, lihat Javadoc kelas).
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna penyunting terakhir. <b>Nilai {@code null} atau yang hanya berisi
	 * spasi diabaikan diam-diam</b> (nilai lama dipertahankan), supaya jejak audit tidak terhapus
	 * oleh pemanggil yang kebetulan menyalin object kosong.
	 *
	 * <p>Normalnya diisi otomatis oleh {@code ais.database.hibernate.AuditTimestampInterceptor}
	 * lewat {@link #onUpdate()}, bukan dipanggil tangan.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong &rarr; tidak ada perubahan.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna penyunting terakhir. Sama seperti {@link #setOlehId(String)},
	 * <b>nilai {@code null} atau kosong diabaikan diam-diam</b>.
	 *
	 * @param oleh nama pengguna; {@code null}/kosong &rarr; tidak ada perubahan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna yang terakhir menyunting baris ini (kolom {@code oleh}).
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah terisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum
	 * {@code UPDATE} baris ini dieksekusi, lalu meneruskan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #setOleh(String)}/{@link #setOlehId(String)} dari pengguna sesi aktif dan
	 * memutakhirkan {@link #setTanggal_dirubah(Date)}.
	 *
	 * <p><b>Jangan panggil manual</b> dan jangan ubah tanda tangannya; hook ini hanya berjalan
	 * pada jalur {@code UPDATE} (bukan {@code INSERT}), karena itu nilai awal
	 * {@code tanggal_dirubah} diinisialisasi langsung pada deklarasi field di baris yang sama.</p>
	 *
	 * <p><b>Catatan gaya:</b> deklarasi field {@code tanggal_dirubah} sengaja dibiarkan menyatu
	 * di baris yang sama seperti aslinya di seluruh repo ini; jangan dipecah saat merapikan
	 * format.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Normalnya diisi otomatis oleh
	 * {@link #onUpdate()}; pemanggilan manual hanya untuk migrasi/perbaikan data.
	 *
	 * @param tanggal_dirubah stempel waktu baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir (kolom {@code tanggal_dirubah}, presisi
	 * {@code TIMESTAMP}). Untuk baris yang belum pernah di-{@code UPDATE}, nilainya adalah waktu
	 * object dibuat di memori &mdash; bukan waktu baris dibuat di basis data.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} pada object yang dibuat
	 *         lewat konstruktor.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas untuk log dan label komponen ZK: <code>{id}-{nama}</code>.
	 *
	 * <p>Berbeda dengan {@code PengajuanBeasiswa.toString()}, method ini membaca <b>field</b>
	 * {@code nama} secara langsung, bukan lewat {@link #getNama()}, sehingga <b>tidak</b> memicu
	 * efek samping {@code trim}/dirty-checking yang dijelaskan di Javadoc kelas.</p>
	 *
	 * <p><b>Awas privasi:</b> keluarannya memuat nama asli anggota keluarga pemohon beasiswa.
	 * Jangan menaruh object ini di log, pesan galat, atau label yang bisa dilihat pengguna
	 * lain.</p>
	 *
	 * @return teks "{id}-{nama}"; untuk object baru yang belum tersimpan hasilnya bisa berbunyi
	 *         {@code "null-null"}.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama anggota keluarga; dipetakan pada {@link #getNama()}, kolom {@code nama}. */
	private String nama;

	/**
	 * Hubungan anggota ini dengan pemohon (teks bebas, mis. "Ayah", "Ibu", "Adik"); dipetakan
	 * pada {@link #getHubungan()}.
	 */
	private String hubungan;

	/**
	 * Pekerjaan anggota keluarga (teks bebas, bukan relasi ke master mana pun); dipetakan pada
	 * {@link #getPekerjaan()}.
	 */
	private String pekerjaan;

	/** Umur anggota keluarga dalam tahun, sebagai angka beku; dipetakan pada {@link #getUmur()}. */
	private Integer umur;

	/** Catatan bebas tambahan; dipetakan pada {@link #getKeterangan()}. */
	private String keterangan;

	/**
	 * Formulir pengajuan beasiswa yang memiliki baris ini (wajib); dipetakan pada
	 * {@link #getPengajuanBeasiswa()}, kolom {@code pengajuan_beasiswa}.
	 */
	private PengajuanBeasiswa pengajuanBeasiswa;

	/**
	 * Konstruktor tanpa argumen yang dibutuhkan Hibernate untuk membuat instance saat memuat
	 * baris. Semua properti dibiarkan {@code null} kecuali {@code tanggal_dirubah} yang
	 * diinisialisasi ke waktu sekarang pada deklarasi field-nya.
	 */
	public KeadaanKeluargaPengajuanBeasiswa() {
	}

	/**
	 * Kunci utama baris ini (kolom {@code id}, {@code IDENTITY}/serial di basis data).
	 *
	 * <p>Dipetakan {@code insertable = false} sehingga nilainya sepenuhnya ditentukan basis data;
	 * menyetel {@link #setId(Long)} sebelum menyimpan tidak akan berpengaruh pada
	 * {@code INSERT}.</p>
	 *
	 * @return id baris, atau {@code null} untuk object yang belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Dipakai Hibernate saat memuat baris; pemanggilan manual praktis
	 * hanya berguna untuk membentuk referensi lepas (detached) ke baris yang sudah ada.
	 *
	 * @param id kunci utama.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama anggota keluarga (kolom {@code nama}, {@code length = 255},
	 * {@code nullable = false}).
	 *
	 * <p><b>Nilai yang dikembalikan sudah di-{@code trim()}</b>, sementara field-nya tidak
	 * diubah. Karena pemetaan memakai <i>property access</i>, hasil getter inilah yang dipakai
	 * Hibernate saat menyusun {@code INSERT} dan saat <i>dirty checking</i> &mdash; sehingga
	 * baris yang di basis data berisi spasi di ujung dapat memicu {@code UPDATE} perapian
	 * (beserta revisi Envers baru) hanya karena dimuat. Lihat bagian Kuirk di Javadoc kelas.</p>
	 *
	 * <p><b>Awas privasi:</b> ini nama orang sungguhan yang bukan pengguna sistem.</p>
	 *
	 * @return nama anggota keluarga tanpa spasi di ujung, atau {@code null} bila field belum
	 *         diisi.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama anggota keluarga. Tidak ada validasi apa pun di sini: {@code null} dan teks
	 * kosong diterima, dan baru ditolak basis data karena kolomnya {@code nullable = false}.
	 *
	 * @param nama nama anggota keluarga.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Catatan bebas tambahan tentang anggota keluarga ini (kolom {@code keterangan},
	 * {@code nullable = true}).
	 *
	 * <p>Berbeda dari {@link #getNama()}, nilainya dikembalikan <b>apa adanya tanpa
	 * {@code trim()}</b>. Kolom warisan template generator; tidak ada kode lain yang membacanya
	 * maupun mengisinya.</p>
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel catatan bebas tambahan.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Hubungan anggota ini dengan mahasiswa pemohon (kolom {@code hubungan}).
	 *
	 * <p>Teks bebas, bukan relasi ke master mana pun dan tanpa normalisasi: "Ayah", "ayah", dan
	 * "Bapak" semuanya nilai yang sah dan berbeda. Jangan dipakai sebagai kunci pengelompokan
	 * tanpa normalisasi di sisi pemanggil.</p>
	 *
	 * @return teks hubungan keluarga, atau {@code null} bila tidak diisi.
	 */
	public String getHubungan() {
		return hubungan;
	}

	/**
	 * Menyetel hubungan anggota ini dengan pemohon.
	 *
	 * @param hubungan teks hubungan keluarga; boleh {@code null}.
	 */
	public void setHubungan(String hubungan) {
		this.hubungan = hubungan;
	}

	/**
	 * Pekerjaan anggota keluarga (kolom {@code pekerjaan}).
	 *
	 * <p>Teks bebas tanpa relasi ke master pekerjaan. Bersama {@code PengajuanBeasiswa}
	 * ({@code pekerjaanBapak}/{@code pekerjaanIbu}/{@code penghasilan}) inilah bagian data yang
	 * paling sensitif secara ekonomi &mdash; perlakukan sebagai rahasia.</p>
	 *
	 * @return teks pekerjaan, atau {@code null} bila tidak diisi.
	 */
	public String getPekerjaan() {
		return pekerjaan;
	}

	/**
	 * Menyetel pekerjaan anggota keluarga.
	 *
	 * @param pekerjaan teks pekerjaan; boleh {@code null}.
	 */
	public void setPekerjaan(String pekerjaan) {
		this.pekerjaan = pekerjaan;
	}

	/**
	 * Umur anggota keluarga dalam tahun (kolom {@code umur}).
	 *
	 * <p><b>Angka beku, bukan turunan tanggal lahir.</b> Tidak ada kode yang menghitung ulang
	 * nilainya, sehingga ia menjadi usang seiring waktu, dan tidak ada validasi rentang &mdash;
	 * nilai negatif atau tak masuk akal tetap tersimpan.</p>
	 *
	 * @return umur dalam tahun, atau {@code null} bila tidak diisi.
	 */
	public Integer getUmur() {
		return umur;
	}

	/**
	 * Menyetel umur anggota keluarga dalam tahun. Tanpa validasi rentang.
	 *
	 * @param umur umur dalam tahun; boleh {@code null}.
	 */
	public void setUmur(Integer umur) {
		this.umur = umur;
	}

	/**
	 * Formulir pengajuan beasiswa yang memiliki baris ini (kolom {@code pengajuan_beasiswa},
	 * {@code nullable = false}).
	 *
	 * <p>Relasi {@code @ManyToOne} dengan {@code cascade = {PERSIST, MERGE}} dan
	 * {@code @Fetch(FetchMode.SELECT)}: fetch tetap <i>eager</i> tetapi lewat {@code SELECT}
	 * terpisah, jadi membaca daftar anggota keluarga menghasilkan satu query tambahan per baris.
	 * Getter ini mengembalikan field apa adanya &mdash; <b>tidak</b> memakai
	 * {@link GeneralValueObject#check(Object)}, tidak membuka dan tidak menutup
	 * {@code Session}.</p>
	 *
	 * <p><b>Awas radius data:</b> object yang dikembalikan membawa serta seluruh profil
	 * sosial-ekonomi pemohon (alamat, kelas penghasilan, kondisi rumah) beserta
	 * {@code Mahasiswa} dan {@code Beasiswa}-nya. Menyerialkan hasil getter ini ke JSON tanpa
	 * pembatasan kedalaman akan membocorkan jauh lebih banyak daripada yang terlihat.</p>
	 *
	 * @return formulir pengajuan induk; secara skema tidak boleh {@code null}, tetapi object yang
	 *         belum diisi lengkap di memori masih bisa mengembalikan {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pengajuan_beasiswa", nullable = false)
	public PengajuanBeasiswa getPengajuanBeasiswa() {
		return pengajuanBeasiswa;
	}

	/**
	 * Menyetel formulir pengajuan beasiswa induk.
	 *
	 * <p>Karena relasi ini ber-{@code cascade = {PERSIST, MERGE}}, menyimpan object ini dapat
	 * ikut menyimpan atau menggabungkan {@link PengajuanBeasiswa} yang ditempelkan &mdash;
	 * termasuk instance baru yang belum pernah disimpan. Pastikan yang dipasang adalah entity
	 * yang benar-benar dimaksud.</p>
	 *
	 * @param pengajuanBeasiswa formulir pengajuan induk; tidak divalidasi di sini, tetapi
	 *                          {@code null} akan ditolak basis data saat {@code INSERT}.
	 */
	public void setPengajuanBeasiswa(PengajuanBeasiswa pengajuanBeasiswa) {
		this.pengajuanBeasiswa = pengajuanBeasiswa;
	}

}
