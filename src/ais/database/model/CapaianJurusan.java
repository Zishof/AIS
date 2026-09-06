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
import org.json.JSONArray;

/**
 * Capaian pembelajaran lulusan sebuah jurusan — satu butir rumusan kemampuan yang harus dimiliki
 * lulusan, digolongkan oleh {@link JenisCapaianJurusan} dan dikaitkan ke sekumpulan mata kuliah.
 *
 * <p>Berbeda dari kebanyakan entity master di paket ini, kolom teksnya dipetakan sebagai
 * {@code text} tanpa batas panjang — rumusan capaian pembelajaran memang berupa kalimat panjang,
 * bukan label.</p>
 *
 * <h3>Kaitan ke mata kuliah disimpan sebagai teks JSON, bukan relasi</h3>
 * <p>{@link #getMk()} menyimpan daftar mata kuliah yang mendukung capaian ini sebagai dokumen JSON di
 * dalam satu kolom teks, bukan sebagai tabel penghubung. Konsekuensinya tidak ada integritas
 * referensial: mata kuliah yang dihapus atau diubah tidak tercermin di sini, dan tidak ada kueri
 * basis data yang dapat menjawab "capaian apa saja yang memakai mata kuliah X" tanpa memindai dan
 * mengurai seluruh baris. Perubahan pada daftar itu juga tidak terekam Envers sebagai perubahan
 * relasi, melainkan sekadar sebagai perubahan sebuah kolom teks.</p>
 *
 * <h3>Cakupan jurusan dan tahun kurikulum</h3>
 * <p>{@link #getJurusan()} wajib dan {@link #getTahunLulus()} menandai angkatan/kurikulum mana yang
 * memakai capaian ini. Keduanya tidak dijaga keunikannya, sehingga rumusan yang sama dapat terdaftar
 * berkali-kali untuk jurusan dan tahun yang sama. Penyaringan berdasarkan jurusan harus dilakukan
 * setiap kali daftar capaian ditampilkan; kelas ini tidak melakukannya sendiri.</p>
 *
 * @see JenisCapaianJurusan
 * @see Jurusan
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "capaian_jurusan")

public class CapaianJurusan extends GeneralValueObject {

	/** 
	 * 
	 */
	/** Penanda versi serialisasi Java; dikunci agar objek lama tetap terbaca setelah kelas diubah. */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama, dibangkitkan basis data. Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna yang terakhir mengubah baris ini. Lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna yang terakhir mengubah baris ini. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Id pengguna yang terakhir mengubah baris ini.
	 *
	 * <p>Bagian dari trio jejak audit ringan {@code oleh}/{@code olehId}/{@code tanggal_dirubah} yang
	 * ditempelkan ke hampir seluruh entity paket ini. Jejak ini terpisah dari — dan jauh lebih miskin
	 * daripada — riwayat Envers yang dihasilkan anotasi {@code @Audited} pada kelas ini: Envers
	 * menyimpan setiap revisi, sedangkan trio ini hanya menyimpan pengubah terakhir.</p>
	 *
	 * <p>Getter ini tidak dianotasi {@code @Column}. Karena kelas ini memakai pemetaan berbasis
	 * properti, Hibernate tetap memperlakukannya sebagai properti yang dipersistensi dengan nama
	 * kolom bawaan. Jangan mengganti nama getter tanpa memeriksa nama kolom yang sebenarnya ada di
	 * basis data.</p>
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila baris belum pernah diubah lewat
	 *         jalur yang mengisinya
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir — <b>tetapi menolak nilai kosong secara diam-diam</b>.
	 *
	 * <p>Bila argumennya {@code null} atau hanya berisi spasi, method langsung selesai tanpa mengubah
	 * apa pun dan tanpa melempar. Akibatnya jejak audit ini bersifat <b>satu arah</b>: nilainya dapat
	 * ditimpa oleh id lain, tetapi <b>tidak pernah dapat dikosongkan kembali</b>. Sekali terisi, ia
	 * bertahan selamanya kecuali diganti dengan id yang lain.</p>
	 *
	 * <p>Dua akibat yang perlu diketahui pemanggil. Pertama, kode yang bermaksud membersihkan jejak —
	 * misalnya saat menganonimkan data atau menyalin baris sebagai cetakan baru — akan gagal tanpa
	 * pesan; baris salinan tetap membawa id pengubah dari baris asalnya. Kedua, karena penolakan itu
	 * senyap, pemanggil tidak dapat membedakan "berhasil disetel" dari "diabaikan"; periksa lewat
	 * {@link #getOlehId()} bila hasilnya penting.</p>
	 *
	 * <p>Pola yang sama dipakai {@link #setOleh(String)} dan berulang di hampir seluruh entity paket
	 * ini.</p>
	 *
	 * @param olehId id pengguna pengubah; {@code null} atau kosong diabaikan diam-diam
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir — menolak nilai kosong secara diam-diam.
	 *
	 * <p>Berperilaku persis seperti {@link #setOlehId(String)}: {@code null} atau string kosong
	 * diabaikan tanpa pesan, sehingga jejak ini hanya dapat ditimpa dan tidak pernah dikosongkan.
	 * Lihat uraian lengkapnya di sana.</p>
	 *
	 * @param oleh nama pengguna pengubah; {@code null} atau kosong diabaikan diam-diam
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna yang terakhir mengubah baris ini.
	 *
	 * <p>Pasangan {@link #getOlehId()} yang menyimpan nama, bukan id. Keduanya diisi terpisah dan
	 * <b>tidak ada yang menjamin keduanya menunjuk orang yang sama</b> — bila satu jalur hanya
	 * mengisi salah satunya, yang lain tetap membawa nilai lama. Untuk penelusuran yang andal, id
	 * lebih dapat dipercaya karena nama pengguna dapat berubah.</p>
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang memperbarui stempel waktu perubahan tepat sebelum baris disimpan.
	 *
	 * <p>Dijalankan Hibernate pada peristiwa {@code @PreUpdate} dan mendelegasikan pekerjaannya ke
	 * {@code AuditTimestampInterceptor.ubah(this)}. Karena kaitnya hanya {@code @PreUpdate} dan bukan
	 * {@code @PrePersist}, stempel waktu pada baris yang <b>baru dibuat</b> berasal dari nilai awal
	 * field — yaitu waktu objek Java dibentuk, bukan waktu penyimpanan. Untuk objek yang dibentuk
	 * lalu baru disimpan jauh kemudian, selisihnya nyata.</p>
	 *
	 * <p><b>Catatan bentuk kode:</b> deklarasi field {@code tanggal_dirubah} berbagi baris yang sama
	 * dengan method ini. Ini hasil penyisipan otomatis, bukan kesengajaan gaya. Field itu adalah
	 * stempel waktu perubahan terakhir dan nilai awalnya diambil dari {@code WaktuUtil.getDate()} —
	 * jam aplikasi, yang dapat berbeda dari jam basis data. Bila kedua jam itu tidak selaras, urutan
	 * kejadian yang tersusun dari kolom ini bisa keliru.</p>
	 *
	 * @see #getTanggal_dirubah()
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir secara langsung.
	 *
	 * <p>Berbeda dengan {@link #setOleh(String)} dan {@link #setOlehId(String)}, setter ini menerima
	 * {@code null} tanpa penolakan — jejak waktu <b>dapat</b> dikosongkan, sedangkan jejak pelakunya
	 * tidak. Ketimpangan itu berarti sebuah baris dapat berakhir dengan "siapa" yang terisi dan
	 * "kapan" yang kosong.</p>
	 *
	 * <p>Nilai yang disetel di sini akan ditimpa oleh {@link #onUpdate()} pada penyimpanan berikutnya,
	 * jadi menyetelnya secara manual hanya bermakna untuk impor data historis.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir baris ini, dengan ketelitian sampai detik.
	 *
	 * <p>Diperbarui otomatis oleh {@link #onUpdate()} pada setiap pembaruan. Mengembalikan objek
	 * {@link Date} yang dapat diubah — pemanggil yang memanggil {@code setTime(...)} pada hasilnya
	 * ikut mengubah keadaan entity ini. Salin dulu bila nilainya akan dimanipulasi.</p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada objek yang baru dibentuk karena
	 *         field-nya diberi nilai awal
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks berbentuk {@code "<id>-<nama>"}.
	 *
	 * <p>Dipakai komponen daftar dan kotak pilihan ZK. Karena {@link #getNama()} di kelas ini
	 * dipetakan sebagai {@code text} tanpa batas panjang, hasilnya dapat berupa satu kalimat rumusan
	 * capaian yang panjang — tidak dipotong di sini, sehingga penyaji perlu membatasi lebarnya
	 * sendiri.</p>
	 *
	 * <p>Membaca field {@code nama} secara langsung, bukan lewat {@link #getNama()}, sehingga tidak
	 * ikut memangkas spasi. Pada baris yang belum tersimpan hasilnya diawali {@code "null-"}.</p>
	 *
	 * @return teks gabungan id dan rumusan capaian
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Rumusan capaian pembelajaran. Lihat {@link #getNama()}. */
	private String nama;
	/** Rumusan capaian dalam bahasa Inggris. Lihat {@link #getNamaEn()}. */
	private String namaEn;
	/** Jurusan pemilik capaian ini; wajib. Lihat {@link #getJurusan()}. */
	private Jurusan jurusan;
	/** Tahun kelulusan/kurikulum yang memakai capaian ini. Lihat {@link #getTahunLulus()}. */
	private Integer tahunLulus;
	/** Keterangan bebas mengenai capaian ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Nomor urut tampilan. Lihat {@link #getNomorUrut()}. */
	private Integer nomorUrut;
	/** Penanda apakah capaian ini masih berlaku. Lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Jenis/penggolongan capaian ini; boleh kosong. Lihat {@link #getJenisCapaianJurusan()}. */
	private JenisCapaianJurusan jenisCapaianJurusan;
	/** Daftar mata kuliah pendukung sebagai teks JSON. Lihat {@link #getMk()}. */
	private String mk;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate.
	 *
	 * <p>Tidak mengisi satu pun field. Objek yang dibentuk lewat konstruktor ini belum tentu sah untuk
	 * disimpan: kolom yang dinyatakan {@code nullable = false} pada getter-getter di bawah harus diisi
	 * lebih dulu, dan penolakannya datang dari basis data, bukan dari kelas ini.</p>
	 */
	public CapaianJurusan() {
	}

	/**
	 * Kunci utama baris ini, dibangkitkan basis data dengan strategi {@code IDENTITY}.
	 *
	 * <p>Bernilai {@code null} sampai entity benar-benar tersimpan. Karena strategi {@code IDENTITY}
	 * memerlukan penyisipan nyata untuk memperoleh nomor, Hibernate tidak dapat menunda
	 * {@code save(...)} pada entity ini sebagaimana yang dilakukannya untuk strategi berbasis
	 * urutan.</p>
	 *
	 * <p>Angka ini hanya unik di dalam tabelnya sendiri. Id yang sama muncul kembali di tabel lain
	 * untuk baris yang sama sekali berbeda, jadi jangan pernah membandingkan id lintas entity atau
	 * memakainya sebagai pengenal tunggal pada peta gabungan.</p>
	 *
	 * @return kunci utama, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama secara langsung.
	 *
	 * <p>Disediakan untuk Hibernate dan untuk alur impor data yang memuat objek lepas. <b>Jangan
	 * memanggilnya pada entity yang sedang terikat session</b>: mengubah pengenal objek yang dikelola
	 * membingungkan cache tingkat pertama dan dapat berujung pada pembaruan baris yang salah.</p>
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Rumusan capaian pembelajaran, sudah dipangkas spasi di kedua ujungnya.
	 *
	 * <p>Dipetakan sebagai {@code text} tanpa batas panjang, karena isinya adalah kalimat rumusan —
	 * bukan label pendek seperti kolom {@code nama} pada kebanyakan entity master lain di paket
	 * ini.</p>
	 *
	 * <p>Pemangkasan dilakukan saat membaca, bukan saat menyimpan. Nilai di basis data tetap membawa
	 * spasi apa adanya, sehingga kueri yang mencocokkan kolom {@code nama} secara langsung dapat gagal
	 * menemukan baris yang lewat getter ini terlihat cocok. Untuk teks sepanjang ini, pencocokan
	 * persis memang jarang berguna; pakai pencarian berbasis {@code like} bila diperlukan.</p>
	 *
	 * <p>Kolomnya dinyatakan {@code nullable = false}, tidak ditegakkan di Java.</p>
	 *
	 * @return rumusan capaian tanpa spasi di ujung, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel rumusan capaian pembelajaran.
	 *
	 * <p>Menyimpan nilai apa adanya, tanpa pemangkasan spasi. Tidak ada batas panjang.</p>
	 *
	 * @param nama rumusan capaian; tidak divalidasi
	 * @see #getNama()
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas mengenai capaian ini, dipetakan sebagai {@code text} tanpa batas panjang.
	 *
	 * <p>Dikembalikan apa adanya, tanpa pemangkasan spasi — berbeda dari {@link #getNama()} dan
	 * {@link #getNamaEn()} yang keduanya memangkas.</p>
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan capaian.
	 *
	 * @param keterangan keterangan bebas; boleh {@code null}, tidak divalidasi
	 * @see #getKeterangan()
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Jurusan pemilik capaian pembelajaran ini.
	 *
	 * <p>Relasi wajib — kolom {@code jurusan} dinyatakan {@code nullable = false}. Inilah satu-satunya
	 * hal yang menempatkan capaian ini di dalam struktur akademik, sehingga <b>penyaringan berdasarkan
	 * jurusan harus dilakukan setiap kali daftar capaian ditampilkan</b>; kelas ini tidak melakukannya
	 * sendiri.</p>
	 *
	 * <p>Dimuat secara {@code LAZY} dan disalurkan lewat {@code check(...)} yang berusaha menyelesaikan
	 * proksi dari cache atau session, lalu <b>menugaskan hasilnya kembali ke field</b>. Pada entity
	 * yang terikat session, pertukaran instance itu dapat terbaca sebagai perubahan properti sehingga
	 * {@code UPDATE} diterbitkan dan Envers mencatat revisi yang tidak diminta.</p>
	 *
	 * <p>Riam {@code PERSIST} dan {@code MERGE} berlaku ke arah jurusan.</p>
	 *
	 * @return jurusan pemilik; tidak seharusnya {@code null} pada baris tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = false)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Menyetel jurusan pemilik capaian ini.
	 *
	 * <p>Menerima {@code null} tanpa keberatan meskipun kolomnya wajib. Memindahkan capaian ke jurusan
	 * lain tidak menyentuh daftar mata kuliah pada {@link #getMk()}, sehingga capaian dapat berakhir
	 * menunjuk mata kuliah dari jurusan yang bukan pemiliknya.</p>
	 *
	 * @param jurusan jurusan pemilik
	 * @see #getJurusan()
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Tahun kelulusan — menandai angkatan atau kurikulum mana yang memakai capaian ini.
	 *
	 * <p><b>Tanpa nilai jatuh-tempo</b>: mengembalikan {@code null} apa adanya, berbeda dari
	 * {@link #getNomorUrut()} dan {@link #getAktif()} pada kelas yang sama yang keduanya memberi nilai
	 * jatuh-tempo. Pemanggil wajib menjaga {@code null} sebelum membandingkan atau membuka kotaknya —
	 * membandingkannya langsung dengan sebuah {@code int} akan melempar
	 * {@code NullPointerException}.</p>
	 *
	 * <p>Karena boleh kosong, capaian tanpa tahun tidak akan cocok dengan penyaring tahun mana pun.
	 * Bila daftar capaian disaring per angkatan, baris yang tahunnya belum diisi akan hilang dari
	 * seluruh angkatan sekaligus — bukan muncul di semuanya.</p>
	 *
	 * <p>Disimpan sebagai bilangan bulat tahun, bukan tanggal, dan tidak dibatasi rentang nilainya.
	 * Getter ini tidak dianotasi {@code @Column}; Hibernate memetakannya ke kolom bernama
	 * {@code tahunLulus} secara bawaan.</p>
	 *
	 * @return tahun kelulusan, atau {@code null} bila belum diisi
	 */
	public Integer getTahunLulus() {
		return tahunLulus;
	}

	/**
	 * Menyetel tahun kelulusan yang memakai capaian ini.
	 *
	 * @param tahunLulus tahun sebagai bilangan bulat; boleh {@code null}, tidak divalidasi
	 * @see #getTahunLulus()
	 */
	public void setTahunLulus(Integer tahunLulus) {
		this.tahunLulus = tahunLulus;
	}

	/**
	 * Rumusan capaian pembelajaran dalam bahasa Inggris, sudah dipangkas spasi di kedua ujungnya.
	 *
	 * <p>Dipakai pada dokumen kurikulum dan akreditasi berbahasa Inggris. Bersifat pilihan; kolomnya
	 * tidak menyatakan {@code nullable = false}.</p>
	 *
	 * <p>Berbeda dengan {@code JenisCapaianJurusan.getNamaEn()} yang mengembalikan nilai apa adanya,
	 * getter di sini <b>memangkas spasi</b> sehingga perlakuannya seragam dengan {@link #getNama()} di
	 * kelas yang sama. Perbedaan gaya antara kedua kelas yang saling berpasangan itu perlu diingat
	 * saat menampilkan keduanya berdampingan.</p>
	 *
	 * <p>Nama kolomnya {@code nama_en} — bergaya garis bawah, sedangkan {@code JenisCapaianJurusan}
	 * memakai nama kolom bawaan {@code namaEn} untuk properti yang sama. Kueri SQL asli harus memakai
	 * nama yang tepat untuk masing-masing tabel.</p>
	 *
	 * @return rumusan dalam bahasa Inggris tanpa spasi di ujung, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama_en", columnDefinition = "text")
	public String getNamaEn() {
		return this.namaEn == null ? null : this.namaEn.trim();
	}

	/**
	 * Menyetel rumusan capaian dalam bahasa Inggris.
	 *
	 * @param namaEn rumusan bahasa Inggris; boleh {@code null}, tidak divalidasi
	 * @see #getNamaEn()
	 */
	public void setNamaEn(String namaEn) {
		this.namaEn = namaEn;
	}

	/**
	 * Nomor urut tampilan capaian ini; {@code null} dibaca sebagai {@code 0}.
	 *
	 * <p><b>Pembacaan murni</b> — memakai ternary tanpa menyentuh field, sama seperti
	 * {@link #getAktif()} di kelas ini. Berbeda dari {@code JenisCapaianJurusan.getFeeder()} yang
	 * menulis saat dibaca; kelas ini seluruhnya bebas dari pola itu.</p>
	 *
	 * <p>Tidak ada batasan keunikan pada nomor urut. Karena nilai jatuh-tempo {@code 0} berlaku untuk
	 * semua baris yang belum diberi nomor, baris-baris itu berkerumun di urutan yang sama dan urutan
	 * di antara mereka ditentukan pengurut sekunder — atau, bila tidak ada, oleh urutan baris yang
	 * kebetulan terbaca basis data, yang tidak dijamin tetap antar pemanggilan. Untuk dokumen
	 * kurikulum yang menomori capaian, ketidakstabilan itu berarti nomor butir dapat berpindah antar
	 * cetakan.</p>
	 *
	 * @return nomor urut tampilan; {@code 0} bila belum diisi
	 */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 0 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampilan capaian ini.
	 *
	 * @param nomorUrut nomor urut; boleh {@code null}, dibaca sebagai {@code 0}
	 * @see #getNomorUrut()
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Apakah capaian ini masih berlaku; {@code null} dianggap berlaku.
	 *
	 * <p><b>Pembacaan murni</b> — memakai ternary tanpa menyentuh field. Bandingkan
	 * {@code Asesor.getAktif()} yang menulis {@code true} ke field saat dibaca; bentuk di sinilah yang
	 * benar untuk entity yang dianotasi {@code @Audited}.</p>
	 *
	 * <p><b>Penanda ini dua arah dengan condong ke "aktif".</b> Hanya {@code false} yang benar-benar
	 * menonaktifkan; {@code null} maupun {@code true} sama-sama berarti berlaku. Mengosongkan kolom
	 * bukan cara menonaktifkan sebuah capaian — setelah dikosongkan ia kembali berlaku. Baris lama
	 * yang dibuat sebelum kolom ini ada pun otomatis dianggap berlaku.</p>
	 *
	 * <p>Perhatikan bahwa {@link JenisCapaianJurusan} yang menggolongkan capaian ini <b>tidak</b>
	 * memiliki penanda serupa: menonaktifkan sebuah capaian dapat dilakukan di sini, tetapi
	 * menonaktifkan seluruh jenisnya tidak.</p>
	 *
	 * <p>Getter ini tidak dianotasi {@code @Column}; Hibernate memetakannya ke kolom bernama
	 * {@code aktif} secara bawaan.</p>
	 *
	 * @return {@code true} bila masih berlaku; hanya {@code false} yang menonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel penanda berlaku-tidaknya capaian ini.
	 *
	 * <p>Kirim {@code false} untuk menonaktifkan. Mengirim {@code null} <b>tidak</b> menonaktifkan.</p>
	 *
	 * @param aktif {@code false} untuk menonaktifkan; {@code true} atau {@code null} berarti berlaku
	 * @see #getAktif()
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Jenis/penggolongan capaian ini.
	 *
	 * <p>Bersifat <b>opsional</b> — kolom {@code jenis_capaian_jurusan_id} dinyatakan
	 * {@code nullable = true}. Capaian tanpa jenis akan hilang dari pengelompokan mana pun saat
	 * dokumen kurikulum disusun per jenis, bukan muncul di kelompok "lain-lain". Bila pengelompokan
	 * itu wajib, penegakannya harus dilakukan di lapisan action yang menyimpan.</p>
	 *
	 * <p>Dimuat secara {@code LAZY} dan disalurkan lewat {@code check(...)} yang hasilnya ditugaskan
	 * kembali ke field — perilaku dan akibatnya sama dengan {@link #getJurusan()}.</p>
	 *
	 * <p>Perhatikan nama kolomnya berakhiran {@code _id}, sedangkan {@link #getJurusan()} memetakan ke
	 * kolom bernama {@code jurusan} tanpa akhiran. Kedua gaya penamaan itu hidup berdampingan dalam
	 * satu tabel.</p>
	 *
	 * @return jenis capaian, atau {@code null} bila belum digolongkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_capaian_jurusan_id", nullable = true)
	public JenisCapaianJurusan getJenisCapaianJurusan() {
		jenisCapaianJurusan = check(jenisCapaianJurusan);
		return jenisCapaianJurusan;
	}

	/**
	 * Menyetel jenis/penggolongan capaian ini.
	 *
	 * @param jenisCapaianJurusan jenis capaian; boleh {@code null}
	 * @see #getJenisCapaianJurusan()
	 */
	public void setJenisCapaianJurusan(JenisCapaianJurusan jenisCapaianJurusan) {
		this.jenisCapaianJurusan = jenisCapaianJurusan;
	}

	/**
	 * Larik JSON kosong yang dipakai sebagai nilai jatuh-tempo {@link #getMk()}.
	 *
	 * <p><b>Field ini {@code static} dan tidak {@code final}, serta bertipe objek yang dapat
	 * diubah.</b> Satu instance {@link JSONArray} dibagi seluruh objek {@code CapaianJurusan} di dalam
	 * JVM — dan pada instalasi multi-tenant, seluruh tenant. Selama tidak ada yang menambahkan elemen
	 * ke dalamnya, {@link #getMk()} akan selalu menghasilkan {@code "[]"} dan semuanya berjalan baik.
	 * Namun karena {@link #getMk()} mengembalikan <i>hasil {@code toString()}</i>-nya dan bukan objek
	 * itu sendiri, pemanggil tidak dapat memutasinya lewat jalur itu — yang menjadikan pola ini aman
	 * dalam praktik, meskipun rapuh secara rancangan.</p>
	 *
	 * <p>Jangan menambahkan kode yang menulis ke field ini atau memutasi isinya: perubahan sekecil apa
	 * pun akan mengubah nilai jatuh-tempo bagi setiap capaian yang daftar mata kuliahnya masih kosong.
	 * Konstanta string {@code "[]"} akan menjadi pilihan yang lebih tepat.</p>
	 */
	private static JSONArray jsonArray = new JSONArray();

	/**
	 * Daftar mata kuliah yang mendukung capaian ini, sebagai teks dokumen JSON.
	 *
	 * <p>Mengembalikan larik JSON kosong ({@code "[]"}) bila kolomnya {@code null} atau hanya berisi
	 * spasi, sehingga pemanggil dapat langsung mengurainya tanpa penjagaan {@code null}. Nilai
	 * jatuh-tempo itu berasal dari {@code jsonArray} — lihat catatan di sana. Isi yang tidak kosong
	 * dikembalikan <b>apa adanya, tanpa dipangkas dan tanpa divalidasi</b>: teks yang bukan JSON sah
	 * akan diteruskan begitu saja dan baru meledak di tangan pengurai milik pemanggil.</p>
	 *
	 * <p><b>Ini kaitan tanpa integritas referensial.</b> Karena daftar mata kuliah disimpan sebagai
	 * teks alih-alih tabel penghubung:</p>
	 * <ul>
	 *   <li>Mata kuliah yang dihapus tetap tercantum di sini sebagai id yatim, dan tidak ada yang
	 *       memberi tahu.</li>
	 *   <li>Pertanyaan sebaliknya — "capaian apa saja yang memakai mata kuliah X" — tidak dapat
	 *       dijawab dengan kueri; seluruh baris harus dipindai dan diurai di memori.</li>
	 *   <li>Perubahan daftar terekam Envers sebagai perubahan sebuah kolom teks, bukan sebagai
	 *       penambahan atau penghapusan kaitan, sehingga riwayatnya jauh lebih sulit dibaca.</li>
	 *   <li>Tidak ada yang memastikan mata kuliah yang dicantumkan berasal dari jurusan yang sama
	 *       dengan {@link #getJurusan()}.</li>
	 * </ul>
	 *
	 * <p>Getter ini tidak menyebut nama kolom, sehingga Hibernate memetakannya ke kolom bernama
	 * {@code mk} secara bawaan.</p>
	 *
	 * @return teks JSON daftar mata kuliah; {@code "[]"} bila kosong, tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getMk() {
		return mk == null || mk.trim().isEmpty() ? jsonArray.toString() : mk;
	}

	/**
	 * Menyetel daftar mata kuliah pendukung sebagai teks JSON.
	 *
	 * <p>Menyimpan nilai apa adanya — <b>tidak memeriksa bahwa teksnya JSON yang sah</b>, tidak
	 * memeriksa bahwa id di dalamnya menunjuk mata kuliah yang benar-benar ada, dan tidak memeriksa
	 * bahwa mata kuliah itu berasal dari jurusan pemilik capaian ini. Ketiganya harus ditegakkan di
	 * lapisan action yang menyusun daftar.</p>
	 *
	 * <p>Mengirim {@code null} atau string kosong mengembalikan properti ke nilai jatuh-tempo
	 * {@code "[]"} saat dibaca.</p>
	 *
	 * @param mk teks JSON daftar mata kuliah; tidak divalidasi
	 * @see #getMk()
	 */
	public void setMk(String mk) {
		this.mk = mk;
	}

}
