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
 * Entity Hibernate untuk tabel {@code public.kelompok_kegiatan_kedosenan} &mdash; <b>puncak rantai
 * master</b> modul "Kegiatan Dosen" (aktivitas Tridharma: pendidikan, penelitian, pengabdian, dan
 * penunjang). Satu baris di sini adalah satu <i>aspek</i>/kelompok kegiatan, mis. "Seminar ilmiah",
 * "Pelatihan", atau "Membina kegiatan mahasiswa".
 *
 * <h2>Posisi dalam rantai master</h2>
 *
 * <p>Rantai master modul ini berjenjang empat tingkat:</p>
 *
 * <pre>
 *   {@link JenisKelompokKegiatanKedosenan}   (payung terluar, mis. "Kelompok Utama")
 *        &darr;
 *   KelompokKegiatanKedosenan                (kelas ini &mdash; aspek kegiatan)
 *        &darr;
 *   DetailKelompokKegiatanKedosenan          (butir rincian di bawah aspek)
 *        &darr;
 *   {@link JabatanKegiatanKedosenan} / {@link SkalaKegiatanKedosenan}
 * </pre>
 *
 * <p>Perhatikan bahwa relasi berjenjang itu <b>tidak dinyatakan dari sisi kelas ini</b>: kelas ini
 * hanya memegang satu {@code @ManyToOne} ke atas ({@link #getJenisKelompokKegiatanKedosenan()}).
 * Sisi anak ({@code DetailKelompokKegiatanKedosenan}) yang memegang FK balik, dan tidak ada
 * koleksi {@code @OneToMany} di sini &mdash; daftar detail selalu diambil lewat query terpisah
 * ({@code DetailKelompokKegiatanKedosenanHelper}), bukan lewat navigasi objek.</p>
 *
 * <p>Transaksi sesungguhnya (kegiatan yang benar-benar dilakukan dosen) hidup di
 * {@link KegiatanKedosenan} &rarr; {@link KegiatanKedosenanPunyaDosen}.
 * {@link KegiatanKedosenan} menyimpan FK <b>ganda</b> yang terdenormalisasi: ke kelas ini
 * ({@code kelompok_kegiatan_kedosenan}, {@code nullable = false}) <i>dan</i> ke
 * {@code DetailKelompokKegiatanKedosenan} sekaligus &mdash; kedua kolom itu tidak saling dijaga
 * konsistensinya oleh basis data, jadi secara teori baris transaksi bisa menunjuk detail yang
 * bukan milik kelompok yang ditunjuknya.</p>
 *
 * <p><b>Catatan penamaan (penting).</b> Di repo ini banyak kelas berawalan {@code Kegiatan*} yang
 * <b>tidak berkerabat</b> meskipun namanya mirip &mdash; {@link KegiatanKedosenan} sama sekali
 * tidak berelasi dengan {@code Kegiatan}/{@code DetailKegiatan} pada modul penagihan. Nama kelas
 * ini sendiri <b>tidak menyesatkan</b>: layar masternya berjudul "Aspek Kegiatan" dan berada di
 * bawah modul "Kegiatan Dosen".</p>
 *
 * <h2>Pengelompokan anggota</h2>
 *
 * <ol>
 *   <li><b>Konstanta bidang Tridharma</b>: {@link #BIDANG_PENDIDIKAN}, {@link #BIDANG_PENELITIAN},
 *   {@link #BIDANG_PENGABDIAN}, {@link #BIDANG_PENUNJANG}.</li>
 *   <li><b>Jejak audit</b> (dideklarasikan ulang, lihat catatan warisan di bawah):
 *   {@link #getOleh()}, {@link #getOlehId()}, {@link #getTanggal_dirubah()},
 *   {@link #onUpdate()}.</li>
 *   <li><b>Identitas</b>: {@link #getId()}, {@link #getNama()}, {@link #toString()}.</li>
 *   <li><b>Relasi ke atas</b>: {@link #getJenisKelompokKegiatanKedosenan()}.</li>
 *   <li><b>Atribut &amp; bendera kelayakan</b>: {@link #getKeterangan()}, {@link #getNomorUrut()},
 *   {@link #getAktif()}, {@link #getBisaDipilihDosen()}.</li>
 *   <li><b>Klasifikasi bidang</b>: {@link #getJenis()} &mdash; satu-satunya method dengan logika
 *   bisnis nyata di kelas ini.</li>
 * </ol>
 *
 * <p><b>Tidak ada method query statis maupun method utilitas basis data</b> di kelas ini. Seluruh
 * pencarian, penyimpanan, dan penghapusan hidup di {@code ais.action.master.KelompokKegiatanKedosenanAction}
 * serta helper-helper modul ({@code DetailKelompokKegiatanKedosenanHelper},
 * {@code AmbilDataKegiatanForKegiatanKedosenanHelper}, {@code BkdKegiatanDosenHelper}).</p>
 *
 * <h2>Kuirk yang wajib diketahui pemanggil</h2>
 *
 * <ul>
 *   <li><b>{@link #getJenis()} bukan getter murni.</b> Ia <i>menurunkan</i> nilai bidang Tridharma
 *   dari {@link #getNama()} lewat rantai perbandingan string harfiah, lalu <b>menulis balik</b>
 *   hasilnya ke field {@code jenis}. Karena {@code jenis} adalah properti terpetakan (tidak ada
 *   {@code @Transient}) dan Hibernate memakai <i>property access</i>, sekadar <i>membaca</i>
 *   kelompok pada sesi yang masih terbuka dapat memicu {@code UPDATE} kolom {@code jenis}. Rincian
 *   dan jalur pemicunya ada pada dokumentasi method itu.</li>
 *   <li><b>Nama kolom FK menyesatkan.</b> {@link #getJenisKelompokKegiatanKedosenan()} dipetakan ke
 *   kolom {@code skala_kegiatan_kedosenan} &mdash; sisa salin-tempel dari pemetaan
 *   {@link SkalaKegiatanKedosenan}. Kolom itu <b>tidak</b> menunjuk skala; ia menunjuk
 *   {@code jenis_kelompok_kegiatan_kedosenan}. Query/laporan yang ditulis langsung di SQL mudah
 *   salah tafsir di sini.</li>
 *   <li><b>{@link #getNama()} memangkas, {@link #setNama(String)} tidak.</b> Nilai yang tersimpan
 *   di basis data bisa mengandung spasi tepi, sehingga pemeriksaan duplikat di layar master
 *   ({@code Restrictions.eq("nama", ...trim())}) bisa meleset untuk baris lama.</li>
 *   <li><b>{@link #toString()} memakai field mentah</b>, bukan {@link #getNama()}, sehingga bisa
 *   mengembalikan {@code null} dan tidak dipangkas.</li>
 * </ul>
 *
 * <h2>Catatan warisan {@link GeneralValueObject}</h2>
 *
 * <p>Kelas induk {@link ais.database.model.GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass} &mdash; ia POJO abstrak biasa, sehingga Hibernate tidak memetakan satu
 * pun propertinya. Karena itu {@code id}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} <b>harus</b> dideklarasikan ulang di sini; itu keharusan teknis, bukan
 * duplikasi yang keliru.</p>
 *
 * <p><b>Pengecualian kontrak base class:</b> {@link GeneralValueObject#getKeterangan()} menjamin
 * hasil non-{@code null} (mengembalikan {@code ""}), sedangkan {@link #getKeterangan()} di sini
 * mengembalikan field apa adanya sehingga <b>bisa {@code null}</b>. Hal yang sama berlaku pada
 * kelas kembarannya {@link JenisKelompokKegiatanKedosenan}.</p>
 *
 * <p>Kelas ini {@link Audited @Audited}: setiap versi baris disalin ke tabel riwayat Envers,
 * termasuk versi yang kolom {@code jenis}-nya baru terisi akibat efek samping
 * {@link #getJenis()}.</p>
 *
 * @see JenisKelompokKegiatanKedosenan
 * @see KegiatanKedosenan
 * @see KegiatanKedosenanPunyaDosen
 * @see JabatanKegiatanKedosenan
 * @see SkalaKegiatanKedosenan
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kelompok_kegiatan_kedosenan")

public class KelompokKegiatanKedosenan extends GeneralValueObject {

	/**
	 * Bidang Tridharma "Pendidikan". Salah satu dari empat nilai sah kolom {@code jenis}.
	 *
	 * <p>Berbeda dengan tiga konstanta lainnya, nilai ini <b>tidak pernah dihasilkan</b> oleh
	 * penurunan otomatis {@link #getJenis()} &mdash; hanya bisa masuk lewat pilihan eksplisit
	 * pengguna pada combobox "Bidang Kegiatan Dosen" di layar master.</p>
	 */
	public static final String BIDANG_PENDIDIKAN = "Pendidikan";
	/** Bidang Tridharma "Penelitian". Salah satu dari empat nilai sah kolom {@code jenis}. */
	public static final String BIDANG_PENELITIAN = "Penelitian";
	/** Bidang Tridharma "Pengabdian" (kepada masyarakat). Salah satu dari empat nilai sah kolom {@code jenis}. */
	public static final String BIDANG_PENGABDIAN = "Pengabdian";
	/**
	 * Bidang "Penunjang". Salah satu dari empat nilai sah kolom {@code jenis}, sekaligus
	 * <b>nilai balik terakhir</b> {@link #getJenis()} bila tidak ada aturan yang cocok.
	 */
	public static final String BIDANG_PENUNJANG = "Penunjang";
	/**
	 * Versi serialisasi. Nilainya identik dengan puluhan entity lain di paket ini (antara lain
	 * {@link JenisKelompokKegiatanKedosenan} dan {@link KegiatanKedosenanPunyaDosen}) karena
	 * semuanya hasil salin-tempel dari berkas yang sama &mdash; jangan dijadikan penanda identitas
	 * kelas.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama baris, {@code IDENTITY} basis data. Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris. Lihat {@link #getOleh()}. */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan ID pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Dideklarasikan ulang di kelas ini (bukan diwarisi secara terpetakan) karena
	 * {@link GeneralValueObject} bukan {@code @MappedSuperclass}; lihat catatan warisan pada
	 * dokumentasi kelas.</p>
	 *
	 * @return ID pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna pengubah terakhir.
	 *
	 * <p><b>Kuirk:</b> nilai {@code null}, kosong, atau hanya berisi spasi <b>diabaikan
	 * diam-diam</b> (method langsung {@code return} tanpa mengubah apa pun), sehingga jejak audit
	 * yang sudah ada tidak dapat dikosongkan lewat setter ini. Biasanya diisi otomatis oleh
	 * {@code ais.database.hibernate.AuditTimestampInterceptor} lewat {@link #onUpdate()}.</p>
	 *
	 * @param olehId ID pengguna pengubah; {@code null}/kosong/spasi diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p><b>Kuirk:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong/spasi
	 * diabaikan diam-diam sehingga nilai lama bertahan.</p>
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/kosong/spasi diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Dideklarasikan ulang di kelas ini karena {@link GeneralValueObject} bukan
	 * {@code @MappedSuperclass}; lihat catatan warisan pada dokumentasi kelas.</p>
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
	 * {@link #setOleh(String)}, {@link #setOlehId(String)}, dan {@link #setTanggal_dirubah(Date)}
	 * dari konteks pengguna yang sedang login.
	 *
	 * <p><b>Efek samping:</b> memodifikasi state objek di tengah siklus flush. Jangan dipanggil
	 * manual dari kode aplikasi &mdash; Hibernate yang memicunya.</p>
	 *
	 * <p>Hanya {@code @PreUpdate} yang dipasang; baris BARU ({@code INSERT}) tidak melewati
	 * callback ini, sehingga baris hasil auto-seed {@code InitDataHelper} maupun hasil unggah Excel
	 * masuk tanpa jejak {@code oleh}/{@code olehId}.</p>
	 *
	 * <p>Karena {@link #getJenis()} bisa mengotori field {@code jenis} saat baris sekadar dibaca,
	 * callback ini juga ikut terpicu pada {@code UPDATE} yang <b>tidak diminta pengguna mana
	 * pun</b> &mdash; jejak audit lalu mencatat pengguna yang kebetulan sedang membuka layar.</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja berada pada baris fisik yang sama dalam
	 * kode aslinya; nilainya diinisialisasi memakai jam aplikasi
	 * ({@code ais.ui.util.WaktuUtil.getDate()}), bukan {@code new Date()}, agar konsisten dengan
	 * zona waktu/penyetelan waktu server yang dipakai seluruh modul.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir baris ini.
	 *
	 * <p>Tanpa validasi; nilai {@code null} diterima apa adanya. Umumnya tidak dipanggil manual
	 * &mdash; {@link #onUpdate()} yang mengisinya.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir, boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (kolom {@code tanggal_dirubah},
	 * {@code TIMESTAMP}).
	 *
	 * <p>Field-nya sudah terisi sejak objek dibuat (jam aplikasi saat konstruksi), jadi nilai ini
	 * <b>tidak pernah {@code null}</b> untuk objek yang baru dibuat di memori; untuk baris hasil
	 * pembacaan dari basis data nilainya mengikuti isi kolom dan bisa {@code null}.</p>
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks entity: <b>field {@code nama} mentah</b>.
	 *
	 * <p><b>Kuirk:</b> memakai field langsung, bukan {@link #getNama()}, sehingga hasilnya
	 * <b>tidak dipangkas</b> dan <b>bisa {@code null}</b> (melanggar konvensi umum
	 * {@code toString()}). Bandingkan dengan {@link JenisKelompokKegiatanKedosenan#toString()} yang
	 * mengembalikan {@code id + "-" + nama}.</p>
	 *
	 * <p>Dipakai antara lain sebagai label {@code Comboitem} pada combobox pemilihan aspek kegiatan
	 * (lewat {@code Common.insertCombo}) dan pada berbagai keterangan yang dirangkai
	 * {@code BkdKegiatanDosenHelper}.</p>
	 *
	 * @return nama aspek kegiatan apa adanya, bisa {@code null}
	 */
	public String toString() {
		return nama;
	}

	/** Nama aspek/kelompok kegiatan. Wajib diisi. Lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas. Boleh {@code null}. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/**
	 * Payung jenis kelompok di atas aspek ini. Lihat {@link #getJenisKelompokKegiatanKedosenan()}
	 * &mdash; perhatikan nama kolom FK-nya menyesatkan.
	 */
	private JenisKelompokKegiatanKedosenan jenisKelompokKegiatanKedosenan;
	/** Bendera aktif. {@code null} dibaca sebagai {@code true}. Lihat {@link #getAktif()}. */
	private Boolean aktif;
	/**
	 * Bendera boleh-dipilih-dosen. {@code null} dibaca sebagai {@code true}.
	 * Lihat {@link #getBisaDipilihDosen()}.
	 */
	private Boolean bisaDipilihDosen;
	/** Urutan tampil. {@code null} dibaca sebagai {@code 1}. Lihat {@link #getNomorUrut()}. */
	private Integer nomorUrut;

	/**
	 * Bidang Tridharma aspek ini (salah satu konstanta {@code BIDANG_*}).
	 *
	 * <p>Properti <b>terpetakan</b> (kolom {@code jenis}) meski tidak beranotasi {@code @Column} &mdash;
	 * tidak ada {@code @Transient}, jadi Hibernate memetakannya dengan nama bawaan. Field ini bisa
	 * terisi tanpa campur tangan pengguna; lihat {@link #getJenis()}.</p>
	 */
	private String jenis;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Semua field dibiarkan kosong kecuali {@code tanggal_dirubah} yang langsung diisi jam
	 * aplikasi pada inisialisasi field. Dipakai layar master saat pengguna menekan "Tambah"
	 * ({@code KelompokKegiatanKedosenanAction.onAdd}).</p>
	 */
	public KelompokKegiatanKedosenan() {
	}

	/**
	 * Mengembalikan kunci utama baris (kolom {@code id}).
	 *
	 * <p>Dibangkitkan basis data ({@code IDENTITY}, {@code insertable = false}), jadi
	 * {@code null} berarti objek belum pernah disimpan &mdash; layar master memakai kondisi itu
	 * untuk memilih judul dialog "Tambah" versus "Ubah".</p>
	 *
	 * @return ID baris, atau {@code null} untuk objek yang belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris. Tanpa validasi.
	 *
	 * <p>Praktis hanya dipanggil Hibernate saat memuat/menyimpan baris; kode aplikasi tidak
	 * sepatutnya mengubah ID entity yang sudah ada.</p>
	 *
	 * @param id ID baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama aspek/kelompok kegiatan, <b>sudah dipangkas</b> spasi tepinya.
	 *
	 * <p><b>Asimetri yang perlu diwaspadai:</b> {@link #setNama(String)} menyimpan nilai apa adanya
	 * (tanpa {@code trim()}), sehingga kolom {@code nama} di basis data bisa berisi spasi tepi
	 * sementara pembacaan lewat getter ini sudah bersih. Akibat nyatanya: pemeriksaan duplikat pada
	 * layar master memakai {@code Restrictions.eq("nama", <masukan yang sudah di-trim>)} yang
	 * membandingkan langsung dengan <i>kolom</i>, bukan dengan hasil getter ini &mdash; baris lama
	 * yang berspasi tepi tidak akan terdeteksi sebagai duplikat.</p>
	 *
	 * <p>Nilai inilah yang dipakai {@link #getJenis()} sebagai kunci penurunan bidang Tridharma;
	 * perhatikan bahwa method itu membandingkan <b>field mentah</b>, bukan hasil getter ini.</p>
	 *
	 * @return nama aspek kegiatan yang sudah dipangkas, atau {@code null} bila field kosong
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama aspek/kelompok kegiatan. Tanpa validasi dan <b>tanpa {@code trim()}</b>
	 * &mdash; lihat catatan asimetri pada {@link #getNama()}.
	 *
	 * <p>Kolom {@code nama} berstatus {@code nullable = false}; validasi wajib-isi ditegakkan di
	 * lapisan layar ({@code KelompokKegiatanKedosenanAction.onSave}), bukan di sini.</p>
	 *
	 * @param nama nama aspek kegiatan
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas aspek ini (kolom {@code keterangan}).
	 *
	 * <p><b>Pengecualian kontrak base class:</b> {@link GeneralValueObject#getKeterangan()}
	 * menjanjikan hasil non-{@code null} (mengembalikan {@code ""} untuk field kosong). Override di
	 * kelas ini <b>membalik jaminan itu</b>: field dikembalikan apa adanya sehingga bisa
	 * {@code null}. Pemanggil wajib memeriksa sendiri.</p>
	 *
	 * <p>Konsekuensi praktis: cabang {@code keterangan} pada
	 * {@link GeneralValueObject#compareTo(GeneralValueObject)} tidak lagi otomatis terpenuhi, dan
	 * pemanggil yang merangkai string tanpa penjagaan bisa menghasilkan teks "null".</p>
	 *
	 * @return keterangan aspek kegiatan, bisa {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas aspek ini. Tanpa validasi; {@code null} diterima.
	 *
	 * @param keterangan keterangan baru, boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif aspek ini, dengan <b>default {@code true}</b> untuk field yang
	 * masih {@code null}.
	 *
	 * <p>Getter ini <b>murni membaca</b> &mdash; default {@code true} hanya dipakai sebagai nilai
	 * balik dan <b>tidak</b> ditulis balik ke field, jadi tidak ada risiko {@code UPDATE} siluman
	 * di sini.</p>
	 *
	 * <p><b>Ditegakkan sungguhan</b> (bukan bendera write-only): saat dosen memilih kegiatan yang
	 * boleh diikuti, {@code AmbilDataKegiatanForKegiatanKedosenanHelper} menyaring dengan
	 * {@code isNull("kelompokKegiatanKedosenan.aktif") OR eq(..., true)} &mdash; bentuk saringan
	 * yang sengaja dibuat konsisten dengan default {@code true} di getter ini.</p>
	 *
	 * @return {@code true} bila aspek aktif atau bendera belum pernah diisi; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif aspek ini.
	 *
	 * <p>Dipanggil dari checkbox "Aktif" pada baris grid layar master, yang langsung menyusulnya
	 * dengan {@code Common.refreshSaveOrUpdate(...)} &mdash; jadi satu klik checkbox langsung
	 * menjadi satu {@code UPDATE} tanpa tombol simpan.</p>
	 *
	 * @param aktif status aktif; {@code null} akan dibaca kembali sebagai {@code true}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan payung {@link JenisKelompokKegiatanKedosenan} di atas aspek ini &mdash; tingkat
	 * teratas rantai master modul (mis. "Kelompok Utama", "Penunjang").
	 *
	 * <p><b>Kuirk pemetaan yang mudah menyesatkan:</b> relasi ini dipetakan ke kolom
	 * {@code skala_kegiatan_kedosenan}, bukan ke kolom bernama {@code jenis_...}. Nama kolom itu
	 * sisa salin-tempel dari pemetaan {@link SkalaKegiatanKedosenan} (bandingkan tabel penghubung
	 * {@code detail_kelompok_has_skala_kegiatan_kedosenan} yang memakai nama kolom sama untuk hal
	 * yang benar-benar berbeda). Siapa pun yang menulis SQL/jrxml langsung di atas tabel
	 * {@code kelompok_kegiatan_kedosenan} akan mengira kolom ini menunjuk skala kegiatan &mdash;
	 * padahal ia menunjuk {@code jenis_kelompok_kegiatan_kedosenan}.</p>
	 *
	 * <p>Dimuat {@code FetchMode.SELECT} (query terpisah, bukan {@code JOIN}), dengan cascade
	 * {@code PERSIST}/{@code MERGE} sehingga menyimpan aspek ikut menyimpan payung baru yang
	 * belum tersimpan. Kolom bersifat {@code nullable = false}; wajib-isinya ditegakkan di layar
	 * ({@code onSave} menolak simpan bila combobox "Kelompok Kegiatan" kosong), <b>tetapi</b> jalur
	 * unggah Excel ({@code Common.uploadData}) tidak melewati {@code onSave} sehingga bergantung
	 * sepenuhnya pada constraint basis data.</p>
	 *
	 * @return payung jenis kelompok, atau {@code null} untuk objek baru yang belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "skala_kegiatan_kedosenan", nullable = false)
	public JenisKelompokKegiatanKedosenan getJenisKelompokKegiatanKedosenan() {
		return jenisKelompokKegiatanKedosenan;
	}

	/**
	 * Menyetel payung {@link JenisKelompokKegiatanKedosenan} aspek ini. Tanpa validasi.
	 *
	 * <p>Dipanggil dari {@code onSave} layar master (nilai combobox "Kelompok Kegiatan") dan dari
	 * auto-seed {@code InitDataHelper} saat tabel masih kosong.</p>
	 *
	 * @param jenisKelompokKegiatanKedosenan payung jenis kelompok
	 */
	public void setJenisKelompokKegiatanKedosenan(JenisKelompokKegiatanKedosenan jenisKelompokKegiatanKedosenan) {
		this.jenisKelompokKegiatanKedosenan = jenisKelompokKegiatanKedosenan;
	}

	/**
	 * Mengembalikan nomor urut tampil aspek ini, dengan <b>default {@code 1}</b> untuk field yang
	 * masih {@code null}.
	 *
	 * <p>Getter ini <b>murni membaca</b>: default {@code 1} tidak ditulis balik ke field.</p>
	 *
	 * <p>Dipakai sebagai kunci pengurutan utama daftar aspek
	 * ({@code addOrder(asc("nomorUrut")).addOrder(asc("nama"))}). Karena default hanya berlaku di
	 * memori dan <b>tidak</b> di basis data, pengurutan SQL menempatkan baris ber-{@code nomorUrut}
	 * {@code null} sesuai aturan {@code NULLS} basis data (PostgreSQL: paling akhir pada urutan
	 * menaik) &mdash; bukan di posisi "1" seperti yang disiratkan getter ini.</p>
	 *
	 * @return nomor urut tampil; {@code 1} bila belum pernah diisi. Tidak pernah {@code null}
	 */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampil aspek ini.
	 *
	 * <p>Dipanggil dari {@code Intbox} nomor urut pada baris grid layar master, yang langsung
	 * menyusulnya dengan {@code Common.refreshUpdate(...)}. Perhatikan: {@code UPDATE} itu ikut
	 * membawa serta field {@code jenis} yang mungkin baru saja terisi diam-diam oleh
	 * {@link #getJenis()} ketika baris tersebut dirender.</p>
	 *
	 * @param nomorUrut nomor urut baru; {@code null} akan dibaca kembali sebagai {@code 1}
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Mengembalikan apakah aspek ini boleh dipilih sendiri oleh dosen, dengan <b>default
	 * {@code true}</b> untuk field yang masih {@code null}.
	 *
	 * <p>Getter ini <b>murni membaca</b>: default {@code true} tidak ditulis balik ke field.</p>
	 *
	 * <p><b>Berlaku menurun ke anak:</b> {@code DetailKelompokKegiatanKedosenan.getBisaDipilihDosen()}
	 * memaksa hasilnya {@code false} bila kelompok induknya (objek ini) mengembalikan {@code false}
	 * &mdash; jadi mematikan bendera di sini otomatis mengunci seluruh detail di bawahnya. Selain
	 * itu bendera ini disaring langsung di query pemilihan kegiatan oleh
	 * {@code AmbilDataKegiatanForKegiatanKedosenanHelper} dan {@code KegiatanKedosenanAction}.</p>
	 *
	 * @return {@code true} bila dosen boleh memilih aspek ini atau bendera belum pernah diisi;
	 *         tidak pernah {@code null}
	 */
	public Boolean getBisaDipilihDosen() {
		return bisaDipilihDosen == null ? true : bisaDipilihDosen;
	}

	/**
	 * Menyetel apakah aspek ini boleh dipilih sendiri oleh dosen.
	 *
	 * <p>Dipanggil dari checkbox "Bisa Dipilih Dosen" pada baris grid layar master, diikuti
	 * langsung oleh {@code Common.refreshSaveOrUpdate(...)}.</p>
	 *
	 * @param bisaDipilihDosen bendera baru; {@code null} akan dibaca kembali sebagai {@code true}
	 */
	public void setBisaDipilihDosen(Boolean bisaDipilihDosen) {
		this.bisaDipilihDosen = bisaDipilihDosen;
	}

	/**
	 * Mengembalikan bidang Tridharma aspek ini (salah satu konstanta {@code BIDANG_*}), dengan
	 * <b>penurunan otomatis dari nama aspek</b> bila kolom {@code jenis} masih kosong.
	 *
	 * <h3>Cara kerja</h3>
	 *
	 * <p>Bila field {@code jenis} masih {@code null} <i>dan</i> {@code nama} tidak {@code null},
	 * method mencocokkan nama aspek (tanpa memedulikan besar-kecil huruf) dengan <b>daftar harfiah
	 * 12 nama</b> yang ditanam langsung di kode &mdash; mis. "Penalaran dan Ilmiah" &rarr;
	 * {@link #BIDANG_PENELITIAN}, "Menduduki Jabatan Pimpinan" &rarr; {@link #BIDANG_PENGABDIAN},
	 * "Pelatihan" &rarr; {@link #BIDANG_PENUNJANG}. Hasil pencocokan <b>disimpan ke field
	 * {@code jenis}</b>. Bila tidak ada yang cocok, method mengembalikan
	 * {@link #BIDANG_PENUNJANG} sebagai nilai balik terakhir <i>tanpa</i> menulisnya ke field.</p>
	 *
	 * <h3>Efek samping: getter ini menulis, dan tulisannya bisa sampai ke basis data</h3>
	 *
	 * <p>{@code jenis} adalah <b>properti terpetakan</b> (tidak ada {@code @Transient}) dan
	 * Hibernate memakai <i>property access</i> pada kelas ini. Jadi begitu penurunan di atas
	 * berhasil pada objek yang masih <i>managed</i> di sesi terbuka, pemeriksaan kotor
	 * (<i>dirty checking</i>) pada flush berikutnya akan menerbitkan {@code UPDATE} kolom
	 * {@code jenis} &mdash; padahal pengguna hanya membaca. Jalur nyata yang terpantau:</p>
	 * <ul>
	 *   <li>Renderer grid layar master memanggil method ini untuk setiap baris halaman yang sedang
	 *   ditampilkan, pada sesi yang sama dengan query pemuatnya. Menyusul itu, satu klik checkbox
	 *   "Aktif"/"Bisa Dipilih Dosen" atau satu perubahan nomor urut memicu flush yang menyapu
	 *   <b>seluruh</b> baris kotor di halaman itu, bukan hanya baris yang diklik.</li>
	 *   <li>{@code BkdKegiatanDosenHelper} memanggilnya lewat
	 *   {@code detail.getKelompokKegiatanKedosenan().getJenis()} untuk mengisi
	 *   {@code AsesemenPenilaian.setBidang(...)}, tepat sebelum {@code commit()} &mdash; di sini
	 *   penulisan balik hampir pasti ikut ter-<i>commit</i>.</li>
	 *   <li>{@code KelompokKegiatanKedosenanAction.init(...)} memakainya untuk memilih item
	 *   combobox "Bidang Kegiatan Dosen"; nilai turunan itu kemudian ditulis kembali secara
	 *   <b>eksplisit</b> oleh {@code onSave} lewat {@link #setJenis(String)}. Artinya, mengubah
	 *   apa pun pada satu aspek lama akan sekaligus <b>membekukan tebakan {@link #BIDANG_PENUNJANG}</b>
	 *   menjadi data sungguhan bagi aspek yang namanya tidak ada di daftar harfiah.</li>
	 * </ul>
	 *
	 * <p>Karena kelas ini {@code @Audited}, setiap {@code UPDATE} siluman tersebut juga menambah
	 * satu revisi Envers atas nama pengguna yang kebetulan sedang membuka layar (lihat
	 * {@link #onUpdate()}).</p>
	 *
	 * <h3>Kerapuhan lain</h3>
	 *
	 * <ul>
	 *   <li>Pencocokan memakai <b>field {@code nama} mentah</b>, bukan {@link #getNama()} yang
	 *   sudah dipangkas &mdash; satu spasi tepi saja membuat seluruh rantai perbandingan meleset dan
	 *   aspek jatuh ke {@link #BIDANG_PENUNJANG}.</li>
	 *   <li>Daftar 12 nama itu ditulis harfiah lengkap dengan tanda baca dan spasi (mis. "Panitia /
	 *   Badan pada Lembaga Pemerintah"); mengganti nama aspek lewat layar master akan mematahkan
	 *   penurunan tanpa peringatan apa pun.</li>
	 *   <li>{@link #BIDANG_PENDIDIKAN} tidak pernah muncul dari penurunan ini &mdash; hanya bisa
	 *   masuk lewat pilihan eksplisit pengguna.</li>
	 *   <li>Kolom {@code jenis} tidak termasuk daftar kolom cetak/unggah layar master
	 *   ({@code id}, {@code nama}, {@code jenisKelompokKegiatanKedosenan}, {@code nomorUrut},
	 *   {@code bisaDipilihDosen}, {@code aktif}, {@code keterangan}), jadi nilainya tidak bisa
	 *   diisi massal lewat Excel maupun ikut tercetak.</li>
	 * </ul>
	 *
	 * @return bidang Tridharma aspek ini; salah satu konstanta {@code BIDANG_*}, tidak pernah
	 *         {@code null} &mdash; {@link #BIDANG_PENUNJANG} bila tidak diketahui
	 */
	public String getJenis() {
		if (jenis == null && nama != null) {
			if (nama.equalsIgnoreCase("Penalaran dan Ilmiah")) {
				jenis = BIDANG_PENELITIAN;
			} else if (nama.equalsIgnoreCase("Pelatihan")) {
				jenis = BIDANG_PENUNJANG;
			} else if (nama.equalsIgnoreCase("Bakat dan minat")) {
				jenis = BIDANG_PENUNJANG;
			} else if (nama.equalsIgnoreCase("Panitia / Badan pada Lembaga Pemerintah")) {
				jenis = BIDANG_PENUNJANG;
			} else if (nama.equalsIgnoreCase(
					"Menulis artikel, kritik, opini dan sebagainya pada media massa (koran/majalah populer/umum)")) {
				jenis = BIDANG_PENUNJANG;
			} else if (nama.equalsIgnoreCase(
					"Melakukan penelitian atau hasil pemikiran yang tidak dipublikasikan (tersimpan di perpustakaan perguruan tinggi)")) {
				jenis = BIDANG_PENELITIAN;
			} else if (nama.equalsIgnoreCase("Memberi latihan / penyuluhan / penataran / ceramah pada masyarakat")) {
				jenis = BIDANG_PENGABDIAN;
			} else if (nama.equalsIgnoreCase("Menduduki Jabatan Pimpinan")) {
				jenis = BIDANG_PENGABDIAN;
			} else if (nama.equalsIgnoreCase(
					"Memberikan jasa konsultan yang relevan dengan kepakarannya dan disetujui oleh pimpinan")) {
				jenis = BIDANG_PENGABDIAN;
			} else if (nama.equalsIgnoreCase("Membina kegiatan mahasiswa")) {
				jenis = BIDANG_PENUNJANG;
			} else if (nama.equalsIgnoreCase("Dosen Mendapatkan Tugas Tambahan")) {
				jenis = BIDANG_PENUNJANG;
			} else if (nama.equalsIgnoreCase("Membimbing Akademik Dosen")) {
				jenis = BIDANG_PENUNJANG;
			}
		}
		return jenis == null ? KelompokKegiatanKedosenan.BIDANG_PENUNJANG : jenis;
	}

	/**
	 * Menyetel bidang Tridharma aspek ini. Tanpa validasi &mdash; nilai di luar keempat konstanta
	 * {@code BIDANG_*} diterima apa adanya, dan {@code null} mengembalikan aspek ke mode penurunan
	 * otomatis {@link #getJenis()}.
	 *
	 * <p>Dipanggil dari {@code KelompokKegiatanKedosenanAction.onSave} dengan nilai combobox
	 * "Bidang Kegiatan Dosen". Karena combobox itu diisi awal dari {@link #getJenis()}, menyimpan
	 * ulang aspek yang kolom {@code jenis}-nya masih kosong akan <b>mempermanenkan nilai
	 * turunan/fallback</b> &mdash; termasuk tebakan {@link #BIDANG_PENUNJANG} untuk aspek yang
	 * namanya tidak dikenali. Bila combobox tidak punya item terpilih, {@code onSave} menulis
	 * {@code null} sehingga penurunan otomatis kembali aktif pada pembacaan berikutnya.</p>
	 *
	 * @param jenis bidang Tridharma; sebaiknya salah satu konstanta {@code BIDANG_*}, boleh
	 *              {@code null}
	 */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}
}
