package ais.database.model.sirs;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Master <b>tenaga medis</b> (tabel {@code sirs.dokter}) pada modul SIRS (Sistem Informasi Rumah
 * Sakit) AIS.
 *
 * <h2>Peran dalam domain</h2>
 * <p>Meskipun namanya {@code Dokter}, entity ini <b>bukan</b> hanya dokter. Daftar kategori yang
 * disediakan ({@link #SPESIALIS}, {@link #UMUM}, {@link #BIDAN}, {@link #PERAWAT},
 * {@link #SEKOLAH}, {@link #LAIN}) memperlihatkan bahwa tabel ini adalah master <i>tenaga medis /
 * pemberi layanan</i> secara umum: dokter spesialis, dokter umum, bidan, perawat, siswa praktek,
 * sampai kategori "lain-lain". Layar pengelolanya
 * ({@code ais.action.master.sirs.DokterAction}) memang memberi label "Tenaga Medis" pada kolom
 * yang menunjuk entity ini, jadi jangan menafsirkan kelas ini sempit sebagai "dokter" saja ketika
 * membaca laporan atau menulis query baru.</p>
 *
 * <p>Entity ini adalah <b>daun referensi</b> di klaster pendaftaran &amp; pelayanan SIRS. Ia
 * dirujuk (sebagai sisi "satu" dari {@code @ManyToOne}) oleh setidaknya:</p>
 * <ul>
 *   <li>{@link JadwalDokter} — jadwal praktek mingguan (kolom {@code jadwal_dokter.dokter},
 *   {@code nullable = false});</li>
 *   <li>{@link Pendaftaran} — dokter penanggung jawab pada registrasi kunjungan
 *   ({@code pendaftaran.dokter}, {@code nullable = false});</li>
 *   <li>{@link BookingRegistrasi} — dokter yang dipilih saat pasien membuat janji temu
 *   ({@code booking_registrasi.dokter}, {@code nullable = true});</li>
 *   <li>{@link KunjunganDokter} — catatan satu kali pemeriksaan/visite
 *   ({@code kunjungan_dokter.dokter}, {@code nullable = true}).</li>
 * </ul>
 * <p>Karena posisinya sebagai daun, entity ini tidak menyimpan logika alur. Semua aturan
 * penjadwalan, antrian, dan penagihan hidup di entity/action yang merujuknya.</p>
 *
 * <h2>Pemetaan &amp; audit</h2>
 * <p>Dipetakan ke skema {@code sirs}, tabel {@code dokter}, dengan {@code dynamicInsert} dan
 * {@code dynamicUpdate} aktif sehingga Hibernate hanya menyertakan kolom yang benar-benar berubah
 * pada perintah INSERT/UPDATE. Anotasi {@link org.hibernate.envers.Audited} membuat setiap
 * perubahan direkam ke tabel revisi Envers — artinya penggantian nama, kode, atau kategori tenaga
 * medis dapat dilacak historisnya, dan {@code RevisiHelper} di layar-layar SIRS memanfaatkan itu.</p>
 *
 * <p>Perlu diperhatikan: karena {@code @Audited}, <b>setiap</b> pemanggilan setter yang benar-benar
 * mengubah nilai akan menghasilkan satu baris revisi tambahan pada flush. Jangan memanggil setter
 * di dalam getter untuk field yang dipetakan kecuali memang disengaja (lihat catatan pada
 * {@link #getAktif()}).</p>
 *
 * <h2>Pola arsitektur AIS yang muncul di kelas ini</h2>
 * <ul>
 *   <li><b>Field audit bayangan</b> — {@link #getOleh()}/{@link #getOlehId()} dan
 *   {@link #getTanggal_dirubah()} beserta hook {@link #onUpdate()} adalah <i>keharusan teknis</i>
 *   pola AIS (bukan duplikasi yang perlu dibersihkan): kelas induk
 *   {@link ais.database.model.GeneralValueObject} juga memiliki {@code oleh}/{@code olehId},
 *   tetapi setiap entity yang di-{@code @Audited} wajib mendeklarasikan salinannya sendiri agar
 *   Envers dan {@code AuditTimestampInterceptor} dapat menuliskan jejak pengubah ke tabel
 *   revisinya sendiri.</li>
 *   <li><b>Setter yang menolak nilai kosong</b> — {@link #setOleh(String)} dan
 *   {@link #setOlehId(String)} sengaja mengabaikan {@code null}/string kosong supaya jejak
 *   pengubah terakhir tidak terhapus oleh pemanggil yang tidak menyediakan konteks pengguna.</li>
 *   <li><b>Getter destruktif</b> — {@link #getAktif()} menulis balik nilai default ke field.</li>
 *   <li><b>Field tidur</b> — {@link #getTarif()} dan {@link #getJenisBiayas()} tidak pernah dibaca
 *   oleh kode aplikasi (lihat Javadoc masing-masing).</li>
 * </ul>
 *
 * @see JadwalDokter
 * @see KunjunganDokter
 * @see Pendaftaran
 * @see BookingRegistrasi
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "dokter")
public class Dokter extends GeneralValueObject {

	/**
	 * Kategori tenaga medis "Spesialis" — dokter dengan kualifikasi spesialisasi tertentu.
	 * <p>Nilai konstanta ini disimpan apa adanya (sebagai teks) pada kolom {@code kategori},
	 * bukan sebagai kode numerik atau FK. Karena itu perubahan pada literal string ini akan
	 * memutus pencocokan dengan data lama yang sudah tersimpan di basis data.</p>
	 *
	 * @see #getKategori()
	 * @see #KATEGOSRIES
	 */
	public static final String SPESIALIS = "Spesialis";

	/**
	 * Kategori tenaga medis "Umum" — dokter umum / dokter jaga tanpa spesialisasi khusus.
	 * <p>Disimpan sebagai teks pada kolom {@code kategori}; lihat catatan pada {@link #SPESIALIS}
	 * mengenai bahaya mengubah literalnya.</p>
	 *
	 * @see #getKategori()
	 */
	public static final String UMUM = "Umum";

	/**
	 * Kategori tenaga medis "Bidan".
	 * <p>Keberadaan kategori ini menegaskan bahwa tabel {@code sirs.dokter} berfungsi sebagai
	 * master tenaga medis umum, bukan khusus dokter. Disimpan sebagai teks pada kolom
	 * {@code kategori}.</p>
	 *
	 * @see #getKategori()
	 */
	public static final String BIDAN = "Bidan";

	/**
	 * Kategori tenaga medis "Perawat".
	 * <p>Disimpan sebagai teks pada kolom {@code kategori}. Perawat yang terdaftar di sini dapat
	 * dijadwalkan lewat {@link JadwalDokter} sama seperti dokter, sehingga jadwal shift perawat
	 * dan jadwal praktek dokter berbagi satu tabel jadwal.</p>
	 *
	 * @see #getKategori()
	 */
	public static final String PERAWAT = "Perawat";

	/**
	 * Kategori tenaga medis "Siswa Praktek" — peserta didik/koas yang sedang menjalani praktek.
	 * <p>Perhatikan bahwa <b>nama konstantanya</b> ({@code SEKOLAH}) berbeda dari <b>nilainya</b>
	 * ({@code "Siswa Praktek"}). Saat menulis query native atau membandingkan nilai kolom
	 * {@code kategori}, yang harus dipakai adalah nilainya, bukan nama konstantanya.</p>
	 *
	 * @see #getKategori()
	 */
	public static final String SEKOLAH = "Siswa Praktek";

	/**
	 * Kategori penampung "Lain-lain" untuk tenaga medis yang tidak masuk kategori lain.
	 * <p>Disimpan sebagai teks pada kolom {@code kategori}.</p>
	 *
	 * @see #getKategori()
	 */
	public static final String LAIN = "Lain-lain";

	/**
	 * Himpunan seluruh kategori tenaga medis yang sah, dipakai untuk mengisi combobox kategori
	 * pada layar {@code ais.action.master.sirs.DokterAction} (baik combobox filter pencarian
	 * maupun combobox pada form tambah/ubah).
	 *
	 * <p><b>Perhatikan tiga hal berikut sebelum memakai konstanta ini.</b></p>
	 *
	 * <p><b>1. Ejaan namanya salah dan sengaja dibiarkan.</b> Nama yang benar seharusnya
	 * {@code KATEGORIS}/{@code KATEGORIES}; huruf {@code S} tertukar posisinya menjadi
	 * {@code KATEGOSRIES}. Ejaan ini <b>tidak boleh diperbaiki sepihak</b> karena merupakan API
	 * publik yang sudah dirujuk dari luar paket (lihat {@code DokterAction}); memperbaikinya harus
	 * dilakukan bersama seluruh pemanggilnya dalam satu perubahan.</p>
	 *
	 * <p><b>2. Koleksi ini {@code public static final} tetapi isinya bisa diubah.</b> Modifier
	 * {@code final} hanya mengunci <i>referensi</i>-nya, bukan isinya: kode mana pun di JVM yang
	 * sama dapat memanggil {@code Dokter.KATEGOSRIES.add(...)} atau {@code .clear()} dan
	 * mempengaruhi seluruh aplikasi, termasuk semua sesi pengguna lain. Tidak ada pembungkus
	 * {@code Collections.unmodifiableSet(...)}. Perlakukan koleksi ini sebagai <b>hanya-baca
	 * berdasarkan konvensi</b>, dan jangan pernah memutasinya dari kode layar.</p>
	 *
	 * <p><b>3. Urutannya tidak deterministik.</b> Implementasinya {@link HashSet}, bukan
	 * {@link java.util.LinkedHashSet} atau {@link java.util.TreeSet}, sehingga urutan iterasi
	 * ditentukan oleh nilai hash string dan bukan urutan penambahan pada blok
	 * {@code static}. Akibat praktisnya: urutan pilihan pada combobox kategori di layar tenaga
	 * medis tidak mengikuti urutan penulisan di kelas ini. Bila suatu saat urutan tampil perlu
	 * dijamin (misalnya "Spesialis" harus muncul pertama), perbaikannya adalah mengganti tipe
	 * koleksi ini atau mengurutkan di sisi pemanggil — bukan menyusun ulang baris
	 * {@code add(...)} di blok {@code static}, yang tidak akan berpengaruh sama sekali.</p>
	 *
	 * <p>Nilai kolom {@code kategori} sendiri <b>tidak divalidasi</b> terhadap himpunan ini oleh
	 * entity: {@link #setKategori(String)} menerima string apa pun. Pembatasan hanya terjadi di
	 * lapisan UI karena combobox-nya hanya menawarkan pilihan dari himpunan ini. Data yang masuk
	 * lewat jalur lain (impor, skrip SQL, API) dapat berisi kategori di luar daftar ini.</p>
	 *
	 * @see #getKategori()
	 * @see #SPESIALIS
	 * @see #LAIN
	 */
	public static final Set<String> KATEGOSRIES = new HashSet<String>();
	static {
		KATEGOSRIES.add(SPESIALIS);
		KATEGOSRIES.add(UMUM);
		KATEGOSRIES.add(BIDAN);
		KATEGOSRIES.add(PERAWAT);
		KATEGOSRIES.add(SEKOLAH);
		KATEGOSRIES.add(LAIN);
	}

	/**
	 * Penanda versi serialisasi Java untuk entity ini.
	 * <p>Nilainya sengaja dibuat sama dengan beberapa entity SIRS lain (mis.
	 * {@link KunjunganDokter} dan {@link BookingRegistrasi}) karena kelas-kelas tersebut lahir
	 * dari cetakan hbm2java yang sama. Kesamaan nilai ini tidak berbahaya — {@code serialVersionUID}
	 * hanya dibandingkan antar versi kelas yang sama, bukan antar kelas berbeda.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama teknis (auto-increment kolom {@code id}). Lihat {@link #getId()}. */
	private Long id;

	/**
	 * Identitas (username/ID) pengguna terakhir yang mengubah baris ini — bagian dari trio field
	 * audit bayangan bersama {@link #oleh} dan {@link #tanggal_dirubah}. Lihat {@link #getOlehId()}.
	 */
	private String olehId;

	/**
	 * @return ID pengguna yang tercatat sebagai pengubah terakhir baris ini, atau {@code null}
	 *         bila belum pernah diisi.
	 * @see #setOlehId(String)
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna pengubah terakhir, <b>mengabaikan nilai kosong</b>.
	 * <p>Bila {@code olehId} bernilai {@code null} atau hanya berisi spasi, method langsung
	 * kembali tanpa mengubah apa pun. Ini disengaja: jejak pengubah terakhir tidak boleh terhapus
	 * hanya karena sebuah pemanggil (mis. proses batch, impor, atau job terjadwal) tidak memiliki
	 * konteks pengguna login. Nilai lama dipertahankan sampai ada pengubah nyata berikutnya.</p>
	 *
	 * @param olehId ID pengguna pengubah; {@code null} atau string kosong diabaikan.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Nama tampilan pengguna terakhir yang mengubah baris ini (pendamping {@link #olehId}).
	 * Lihat {@link #getOleh()}.
	 */
	private String oleh;

	/**
	 * Representasi teks tenaga medis dalam format {@code "kode - nama"}.
	 * <p>Method ini <b>meng-override</b> {@link ais.database.model.GeneralValueObject#toString()}
	 * dan dipakai luas di lapisan UI: label pada grid jadwal
	 * ({@code JadwalDokterAction.JadwalDokterRenderer} memanggil
	 * {@code jadwalDokter.getDokter().toString()}), isi banbox pemilih tenaga medis, serta hasil
	 * {@link JadwalDokter#toString()} yang menyusun teks jadwal dari {@code dokter} + poli + hari
	 * + shift.</p>
	 *
	 * <p>Perhatikan bahwa method ini memanggil {@link #getKode()} dan {@link #getNama()}, bukan
	 * membaca field langsung. Untuk kelas ini kedua getter tersebut sepele (tidak melakukan
	 * resolusi proxy maupun tulis-balik), sehingga {@code toString()} aman dipanggil pada instance
	 * yang sudah <i>detached</i> selama field skalar {@code kode} dan {@code nama} sudah terisi.
	 * Bila salah satunya {@code null}, hasilnya akan memuat literal {@code "null"} — bukan
	 * melempar exception.</p>
	 *
	 * @return string {@code "kode - nama"} untuk ditampilkan di UI dan laporan.
	 */
	public String toString() {
		return getKode() + " - " + getNama();
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, <b>mengabaikan nilai kosong</b> dengan alasan yang
	 * sama seperti {@link #setOlehId(String)}: mencegah jejak audit terhapus oleh pemanggil tanpa
	 * konteks pengguna.
	 *
	 * @param oleh nama pengguna pengubah; {@code null} atau string kosong diabaikan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna yang tercatat sebagai pengubah terakhir, atau {@code null} bila belum
	 *         pernah diisi.
	 * @see #setOleh(String)
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@link javax.persistence.PreUpdate} yang mendelegasikan pengisian jejak audit ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} tepat sebelum
	 * Hibernate menerbitkan perintah UPDATE untuk baris ini.
	 *
	 * <p>Interceptor tersebut mengisi {@link #oleh}, {@link #olehId}, dan
	 * {@link #tanggal_dirubah} dari konteks pengguna yang sedang aktif. Karena hook ini hanya
	 * {@code @PreUpdate} (bukan {@code @PrePersist}), pengisian otomatis terjadi pada
	 * <b>perubahan</b>, sedangkan pada penyimpanan pertama nilai audit bergantung pada pemanggil
	 * atau pada nilai awal {@code new Date()} pada {@link #tanggal_dirubah}.</p>
	 *
	 * <p><b>Catatan format:</b> deklarasi method ini dan deklarasi field
	 * {@code tanggal_dirubah} sengaja berada pada satu baris fisik yang sama. Bentuk itu dihasilkan
	 * oleh alat penyapu yang menyisipkan pasangan hook+field ke ratusan entity sekaligus; jangan
	 * memisahkannya tanpa alasan karena akan menghasilkan diff besar di banyak berkas dan
	 * menyulitkan pelacakan penyapuan berikutnya.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = new Date();

	/**
	 * Menyetel waktu perubahan terakhir baris ini.
	 * <p>Berbeda dari {@link #setOleh(String)}/{@link #setOlehId(String)}, setter ini
	 * <b>tidak</b> menolak {@code null}; menyetelnya ke {@code null} akan benar-benar mengosongkan
	 * kolom {@code tanggal_dirubah}. Pada alur normal nilai ini ditimpa otomatis oleh
	 * {@link #onUpdate()} saat flush, sehingga pemanggilan manual jarang diperlukan.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return waktu perubahan terakhir baris ini; diinisialisasi ke {@code new Date()} saat object
	 *         dibuat dan diperbarui otomatis oleh {@link #onUpdate()} pada setiap UPDATE.
	 *         Dipetakan sebagai {@link TemporalType#TIMESTAMP} sehingga menyimpan tanggal
	 *         <i>dan</i> jam.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nama tenaga medis, kolom {@code nama}. Lihat {@link #getNama()}. */
	private String nama;

	/** Kode unik tenaga medis, kolom {@code kode}. Lihat {@link #getKode()}. */
	private String kode;

	/** Catatan bebas, kolom {@code keterangan}. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Tarif jasa tenaga medis — field tidur, lihat peringatan pada {@link #getTarif()}. */
	private Double tarif;

	/** Kategori tenaga medis, salah satu nilai pada {@link #KATEGOSRIES}. Lihat {@link #getKategori()}. */
	private String kategori;

	/** Alamat tenaga medis, kolom {@code alamat}. Lihat {@link #getAlamat()}. */
	private String alamat;

	/** Penanda aktif/nonaktif — lihat peringatan pemakaian pada {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Daftar jenis biaya yang boleh ditagihkan oleh tenaga medis ini, dipetakan lewat tabel
	 * penghubung {@code sirs.dokter_has_jenis_biaya}. Lihat peringatan pemakaian pada
	 * {@link #getJenisBiayas()}.
	 */
	private Set<JenisBiaya> jenisBiayas = new HashSet<JenisBiaya>();

	/**
	 * Relasi many-to-many ke {@link JenisBiaya} lewat tabel penghubung
	 * {@code sirs.dokter_has_jenis_biaya} ({@code dokter} &rarr; {@code jenis_biaya}), diurutkan
	 * menaik berdasarkan {@code nama}.
	 *
	 * <p><b>Peringatan pemakaian — koleksi ini praktis tidak pernah dibaca lewat accessor.</b>
	 * Layar pengelolanya, {@code ais.action.master.sirs.DokterAction}, secara eksplisit membaca
	 * dan menulis tabel penghubung {@code sirs.dokter_has_jenis_biaya} memakai SQL native pada
	 * session Hibernate yang aktif, dan komentar di kelas tersebut menyatakan hal itu dilakukan
	 * <i>"BUKAN via dokter.getJenisBiayas()"</i>. Di luar {@code DokterAction} dan kelas ini
	 * sendiri, tidak ada pemanggil {@code getJenisBiayas()} maupun
	 * {@link #setJenisBiayas(Set)} di basis kode.</p>
	 *
	 * <p>Konsekuensi yang perlu disadari:</p>
	 * <ul>
	 *   <li>Isi koleksi ini <b>tidak</b> menjadi sumber kebenaran untuk logika penagihan; sumber
	 *   kebenarannya adalah isi tabel penghubung sebagaimana dibaca langsung oleh
	 *   {@code DokterAction}.</li>
	 *   <li>Karena tetap dipetakan sebagai {@code @ManyToMany} dengan
	 *   {@code cascade = { MERGE, PERSIST }}, koleksi ini <b>tetap ikut di-flush</b> oleh
	 *   Hibernate. Jika suatu saat ada kode yang memuat {@code Dokter} lalu mem-flush-nya dalam
	 *   keadaan koleksi ini kosong (mis. instance baru yang tidak pernah dimuat dari basis data),
	 *   ada risiko baris tabel penghubung terhapus. Karena itu jangan menyimpan/merge instance
	 *   {@code Dokter} hasil konstruksi manual ke basis data.</li>
	 *   <li>Anotasi {@code @OrderBy("nama asc")} pada {@link Set} hanya mempengaruhi klausa ORDER
	 *   BY saat pemuatan; karena tipe koleksinya {@link HashSet} (bukan
	 *   {@link java.util.LinkedHashSet}), urutan iterasi di Java tetap tidak terjamin.</li>
	 * </ul>
	 *
	 * @return koleksi {@link JenisBiaya} yang terhubung ke tenaga medis ini; tidak pernah
	 *         {@code null} (diinisialisasi ke {@link HashSet} kosong).
	 */
	@ManyToMany(targetEntity = JenisBiaya.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@OrderBy(value = "nama asc")
	@JoinTable(name = "dokter_has_jenis_biaya", schema = "sirs", joinColumns = @JoinColumn(name = "dokter"), inverseJoinColumns = @JoinColumn(name = "jenis_biaya"))
	public Set<JenisBiaya> getJenisBiayas() {
		return jenisBiayas;
	}

	/**
	 * Mengganti seluruh isi koleksi {@link JenisBiaya} tenaga medis ini.
	 * <p>Mengganti <i>referensi</i> koleksi, bukan menggabungkan isinya. Menyetel koleksi kosong
	 * lalu mem-flush akan menghapus seluruh baris tabel penghubung untuk tenaga medis ini. Lihat
	 * peringatan pemakaian pada {@link #getJenisBiayas()}.</p>
	 *
	 * @param jenisBiayas koleksi jenis biaya yang baru.
	 */
	public void setJenisBiayas(Set<JenisBiaya> jenisBiayas) {
		this.jenisBiayas = jenisBiayas;
	}

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate untuk instansiasi lewat refleksi.
	 * <p>Semua field skalar dibiarkan {@code null} kecuali {@link #tanggal_dirubah} (diisi
	 * {@code new Date()}) dan {@link #jenisBiayas} (diisi {@link HashSet} kosong). Instance hasil
	 * konstruktor ini belum memiliki {@link #getKode()}, sehingga {@link #toString()} akan
	 * menghasilkan {@code "null - null"} sampai field-nya diisi.</p>
	 */
	public Dokter() {
	}

	/**
	 * @return kunci utama teknis baris ini, atau {@code null} bila entity belum pernah disimpan.
	 *         Dihasilkan oleh basis data ({@link javax.persistence.GenerationType#IDENTITY}) dan
	 *         dipetakan dengan {@code insertable = false} sehingga kolom {@code id} tidak pernah
	 *         disertakan pada perintah INSERT.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama teknis.
	 * <p>Pada alur normal nilai ini diisi oleh Hibernate setelah INSERT dan <b>tidak boleh</b>
	 * diubah manual: {@link ais.database.model.GeneralValueObject#equals(Object)} dibangun di atas
	 * {@code id}, sehingga mengubahnya pada instance yang sudah terkelola akan merusak identitas
	 * object di dalam session maupun di {@code EntityIdentityMap}.</p>
	 *
	 * @param id kunci utama teknis.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return nama tenaga medis (kolom {@code nama}, maksimal 255 karakter, boleh {@code null}).
	 *         Dipakai bersama {@link #getKode()} oleh {@link #toString()}.
	 */
	@Column(name = "nama", nullable = true, length = 255)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menyetel nama tenaga medis. Tidak ada validasi panjang di sisi Java; batas 255 karakter
	 * hanya ditegakkan oleh definisi kolom basis data.
	 *
	 * @param nama nama tenaga medis.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return catatan bebas mengenai tenaga medis ini (kolom {@code keterangan}, boleh
	 *         {@code null}). Ditampilkan sebagai kolom informasi pada grid layar tenaga medis.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel catatan bebas mengenai tenaga medis ini.
	 *
	 * @param keterangan catatan bebas; boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menyetel kode unik tenaga medis.
	 * <p>Kolomnya dipetakan {@code unique = true, nullable = false}, sehingga menyetel nilai
	 * duplikat atau {@code null} baru akan gagal saat flush — bukan saat pemanggilan setter ini.
	 * Validasi keunikan di sisi aplikasi (bila ada) menjadi tanggung jawab layar pemanggil.</p>
	 *
	 * @param kode kode unik tenaga medis.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * @return kode unik tenaga medis (kolom {@code kode}, {@code unique = true},
	 *         {@code nullable = false}, maksimal 255 karakter). Kode inilah yang muncul di bagian
	 *         depan {@link #toString()} dan dipakai petugas untuk mencari tenaga medis pada
	 *         banbox pemilih.
	 */
	@Column(name = "kode", nullable = false, unique = true, length = 255)
	public String getKode() {
		return kode;
	}

	/**
	 * Menyetel kategori tenaga medis.
	 * <p><b>Tidak ada validasi terhadap {@link #KATEGOSRIES}.</b> Setter ini menerima string apa
	 * pun, termasuk nilai di luar daftar kategori resmi dan nilai dengan ejaan/kapitalisasi
	 * berbeda. Pembatasan pilihan hanya terjadi di lapisan UI (combobox pada
	 * {@code DokterAction} yang isinya diambil dari {@link #KATEGOSRIES}). Data yang masuk lewat
	 * impor, skrip SQL, atau integrasi lain dapat memuat kategori sembarang, jadi kode yang
	 * mengelompokkan tenaga medis berdasarkan kategori sebaiknya menyiapkan cabang
	 * "tidak dikenal".</p>
	 *
	 * @param kategori kategori tenaga medis; idealnya salah satu nilai pada {@link #KATEGOSRIES}.
	 */
	public void setKategori(String kategori) {
		this.kategori = kategori;
	}

	/**
	 * @return kategori tenaga medis sebagai teks bebas — pada data yang dibuat lewat layar
	 *         {@code DokterAction} nilainya akan berupa salah satu anggota {@link #KATEGOSRIES},
	 *         tetapi lihat catatan pada {@link #setKategori(String)}: nilai di luar daftar tetap
	 *         mungkin ada.
	 *         <p>Perhatikan bahwa getter ini <b>tidak</b> memiliki anotasi
	 *         {@link Column @Column} eksplisit, sehingga Hibernate memetakannya ke kolom bernama
	 *         {@code kategori} berdasarkan konvensi penamaan properti.</p>
	 */
	public String getKategori() {
		return kategori;
	}

	/**
	 * Menyetel tarif jasa tenaga medis. Lihat peringatan pada {@link #getTarif()}: field ini
	 * tidur dan tidak dibaca oleh kode aplikasi mana pun.
	 *
	 * @param tarif nominal tarif; boleh {@code null}.
	 */
	public void setTarif(Double tarif) {
		this.tarif = tarif;
	}

	/**
	 * @return nominal tarif jasa tenaga medis, atau {@code null}.
	 *
	 * <p><b>Peringatan: field tidur (dormant).</b> Penelusuran seluruh basis kode tidak menemukan
	 * satu pun pemanggil {@code getTarif()} maupun {@link #setTarif(Double)} di luar kelas ini —
	 * termasuk di layar pengelolanya sendiri, {@code ais.action.master.sirs.DokterAction}, yang
	 * tidak menyediakan input untuk tarif. Kolomnya tetap dipetakan dan tetap ikut dibaca/ditulis
	 * oleh Hibernate, tetapi nilainya tidak pernah dipakai untuk perhitungan biaya apa pun.</p>
	 *
	 * <p>Perhitungan jasa tenaga medis yang sesungguhnya berjalan lewat jalur lain: relasi
	 * {@code sirs.dokter_has_jenis_biaya} (lihat {@link #getJenisBiayas()}) dan field
	 * {@code biaya} pada {@link KunjunganDokter}. Jangan menghidupkan kembali field ini sebagai
	 * sumber tarif tanpa lebih dulu memastikan konsistensinya dengan kedua jalur tersebut —
	 * data historis pada kolom ini kemungkinan besar kosong atau usang.</p>
	 */
	public Double getTarif() {
		return tarif;
	}

	/**
	 * Menyetel alamat tenaga medis.
	 *
	 * @param alamat alamat; boleh {@code null}.
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * @return alamat tenaga medis, atau {@code null}. Diisi dan ditampilkan lewat layar
	 *         {@code DokterAction} (kolom alamat pada grid dan input pada form ubah).
	 *         <p>Tidak beranotasi {@link Column @Column}, jadi dipetakan ke kolom {@code alamat}
	 *         berdasarkan konvensi.</p>
	 */
	public String getAlamat() {
		return alamat;
	}

	/**
	 * Mengembalikan penanda aktif tenaga medis, dengan <b>default {@code true} yang ditulis balik
	 * ke field</b>.
	 *
	 * <h3>Perilaku: getter destruktif</h3>
	 * <p>Ini instance dari pola "getter destruktif" yang berulang di seluruh model AIS: bila
	 * {@link #aktif} masih {@code null}, getter tidak sekadar mengembalikan {@code true} sebagai
	 * nilai pengganti, melainkan <b>menugaskan</b> {@code true} ke field terlebih dahulu. Karena
	 * Hibernate mengakses entity ini lewat properti (anotasi ditempatkan pada getter), pembacaan
	 * saat flush juga melewati method ini — sehingga baris yang kolom {@code aktif}-nya
	 * {@code NULL} di basis data akan ikut ter-UPDATE menjadi {@code true} pada flush pertama
	 * setelah entity dibaca, tanpa ada perintah pengguna. Karena entity ini
	 * {@link org.hibernate.envers.Audited}, UPDATE tersebut juga menghasilkan satu baris revisi
	 * Envers. Jangan menganggap "membaca saja tidak mengubah data" untuk entity ini.</p>
	 *
	 * <h3>Peringatan: penanda ini tidak menyaring apa pun</h3>
	 * <p>Nilai {@code aktif} <b>diisi</b> oleh layar {@code DokterAction} (dari sebuah checkbox
	 * pada form ubah), tetapi penelusuran seluruh basis kode tidak menemukan satu pun query yang
	 * menyaring {@code Dokter} berdasarkan penanda ini: dari sekian puluh titik yang membangun
	 * criteria atas {@code Dokter.class}, tidak ada yang menambahkan pembatasan pada properti
	 * {@code aktif}. Akibat praktisnya:</p>
	 * <ul>
	 *   <li>Tenaga medis yang sudah dinonaktifkan <b>tetap muncul</b> di banbox pemilih tenaga
	 *   medis, di daftar penjadwalan {@link JadwalDokter}, dan di form pendaftaran pasien —
	 *   sehingga masih dapat dipilih untuk jadwal dan registrasi baru.</li>
	 *   <li>Penanda ini praktis hanya berfungsi sebagai <b>informasi tampilan</b>, bukan sebagai
	 *   penjaga integritas.</li>
	 * </ul>
	 * <p>Bila suatu saat penyaringan hendak diaktifkan, perbaikannya harus dilakukan serentak di
	 * seluruh titik pemilih tenaga medis, dan harus memperhitungkan bahwa data lama dapat memiliki
	 * {@code aktif = NULL} — nilai yang oleh getter ini diperlakukan sebagai aktif.</p>
	 *
	 * @return {@code true} bila tenaga medis dianggap aktif; tidak pernah {@code null} karena
	 *         {@code null} otomatis diganti (dan ditulis balik) menjadi {@code true}.
	 * @see #setAktif(Boolean)
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menyetel penanda aktif tenaga medis.
	 * <p>Menyetel {@code null} tidak akan bertahan lama: pembacaan berikutnya lewat
	 * {@link #getAktif()} akan mengubahnya kembali menjadi {@code true}. Untuk menonaktifkan,
	 * setel secara eksplisit ke {@link Boolean#FALSE}. Ingat pula bahwa menonaktifkan tenaga medis
	 * <b>tidak</b> menyembunyikannya dari daftar pilihan mana pun — lihat peringatan pada
	 * {@link #getAktif()}.</p>
	 *
	 * @param aktif {@code true} untuk aktif, {@code false} untuk nonaktif.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
