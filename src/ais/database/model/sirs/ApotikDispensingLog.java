package ais.database.model.sirs;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

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

import ais.database.model.GeneralValueObject;

/**
 * Jejak dispensing resep: pemeriksaan kedua (double-check) dan konseling
 * pasien (IR-05 modernisasi UI/UX Apotik).
 *
 * <p><b>Mengapa tabel baru?</b> {@code Resep} adalah entity milik modul SIRS
 * yang dipakai jalur rumah sakit dan sudah {@code @Audited}; menambah kolom di
 * sana menuntut ALTER tabel audit lama (gotcha Envers). Tabel BARU dibuat
 * otomatis oleh {@code hbm2ddl=update} berikut tabel auditnya — tanpa migrasi
 * manual sama sekali.</p>
 *
 * <p><b>Aturan keselamatan yang ditegakkan server</b> (lihat
 * {@code ApotikDispensingHelper}): pemeriksa kedua WAJIB akun yang berbeda
 * dari penyiap; satu resep hanya boleh punya satu catatan per jenis yang
 * aktif. Catatan bersifat append-only — pembatalan dilakukan dengan
 * menonaktifkan baris, bukan menghapusnya.</p>
 *
 * <h3>Mengapa pemeriksaan kedua ada</h3>
 *
 * <p>Pemeriksaan kedua adalah pengaman keselamatan pasien yang paling sederhana
 * dan paling ampuh di apotek: satu orang menyiapkan obat, orang KEDUA
 * memeriksanya sebelum obat diserahkan. Gunanya bukan ketelitian tambahan
 * melainkan mata yang lain — orang yang sudah salah membaca resep cenderung
 * salah membacanya lagi ketika memeriksa pekerjaannya sendiri. Karena itu
 * seluruh nilai pengaman ini bergantung pada satu hal saja: pemeriksanya
 * benar-benar orang lain. Pemeriksaan kedua yang dilakukan penyiapnya sendiri
 * bukan pengaman yang lemah, melainkan bukan pengaman sama sekali — sambil
 * meninggalkan catatan yang menyatakan bahwa pemeriksaan sudah dilakukan.</p>
 *
 * <h3>Seberapa kuat penegakan aturan itu sesungguhnya</h3>
 *
 * <p>{@code ApotikDispensingHelper.catat} memang menolak ketika
 * {@code penyiap_user_id} yang dikirim sama dengan akun yang sedang login, dan
 * penolakan itu berjalan di peladen sehingga klien tidak dapat melewatinya. Yang
 * perlu dipahami adalah apa yang dibandingkan: {@code penyiap_user_id} datang
 * dari PAYLOAD PERMINTAAN, bukan dari catatan peladen tentang siapa yang
 * benar-benar menyiapkan obat. Tidak ada tempat di sistem ini yang merekam
 * penyiap secara berwenang, sehingga tidak ada yang dapat dijadikan pembanding.
 * Akibatnya perbandingan itu berbentuk "akun yang login tidak sama dengan nama
 * yang diketik pengirim" — seorang petugas yang bekerja sendirian dapat
 * mengetikkan id rekan mana pun dan lolos, meninggalkan catatan yang menyatakan
 * pemeriksaan kedua sudah dilakukan.</p>
 *
 * <p>Ini bukan alasan melonggarkan penolakan yang ada — ia tetap menangkap kasus
 * paling lugu, yaitu petugas yang mengisi namanya sendiri. Tetapi Javadoc ini
 * tidak boleh membiarkan pembacanya mengira jejak {@link #getPenyiapUserId()}
 * adalah fakta terverifikasi. Ia adalah PERNYATAAN pengirim. Setiap laporan atau
 * pemeriksaan yang bersandar padanya perlu menyebutkan sifat itu.</p>
 *
 * <h3>Catatan tentang "pembatalan" pada paragraf di atas</h3>
 *
 * <p>Kalimat "pembatalan dilakukan dengan menonaktifkan baris" menyatakan
 * BENTUK yang benar bila pembatalan diperlukan, bukan fasilitas yang sudah
 * tersedia. Pada keadaan sekarang {@code ApotikDispensingHelper.proses} hanya
 * mengenal dua aksi — {@code apotik_dispensing_status} dan
 * {@code apotik_dispensing_catat} — dan tidak ada satu pun jalur yang
 * memanggil {@link #setAktif(Boolean)} dengan {@code FALSE}. Penanda aktif
 * karena itu sekarang berjalan SATU ARAH: dinyalakan saat baris dibuat, tidak
 * pernah dipadamkan. Siapa pun yang kelak menambahkan pembatalan hendaknya
 * mengikuti bentuk yang dinyatakan paragraf itu — menonaktifkan, bukan
 * menghapus — dan menambahkan pemeriksaan hak yang setara.</p>
 *
 * @see AntreanFarmasi papan antrean penyiapan; status SELESAI di sana bukan bukti pemeriksaan
 * @see ApotikNarkotikaLog register wajib untuk golongan obat terkendali
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "apotik_dispensing_log")
public class ApotikDispensingLog extends GeneralValueObject {

	/** Versi serialisasi; tetap 1 selama bentuk field tidak berubah maknanya. */
	private static final long serialVersionUID = 1L;

	/** Pemeriksaan kedua sebelum obat diserahkan. */
	public static final String JENIS_DOUBLE_CHECK = "DOUBLE_CHECK";

	/** Konseling/penjelasan pemakaian obat kepada pasien. */
	public static final String JENIS_KONSELING = "KONSELING";

	/** Kunci baris; dibangkitkan basis data. */
	private Long id;

	/** Resep yang diperiksa/dikonselingkan. Wajib. */
	private Resep resep;

	/** {@link #JENIS_DOUBLE_CHECK} atau {@link #JENIS_KONSELING}. */
	private String jenis;

	/** Akun yang MENYIAPKAN obat (pembanding untuk aturan pemeriksa kedua). */
	private String penyiapUserId;

	/** Akun yang melakukan pemeriksaan/konseling ini. */
	private String pelakuUserId;

	/** Nama tampil pelaku, disalin dari sesi saat pencatatan. */
	private String pelakuNama;

	/** Catatan bebas pemeriksa/konselor. */
	private String catatan;

	/** Penanda aktif; sekarang berjalan satu arah — lihat dokumentasi class. */
	private Boolean aktif;

	/** Waktu pemeriksaan/konseling dicatat. */
	private Date waktu;

	/** Nama tampil pelaku pencatatan (bayangan audit). */
	private String oleh;

	/** Identitas akun pelaku pencatatan (bayangan audit). */
	private String olehId;

	/**
	 * Kait JPA sebelum UPDATE: menyegarkan {@link #getTanggal_dirubah()}.
	 *
	 * <p>Pada keadaan sekarang tidak pernah berjalan, karena tidak ada jalur
	 * yang menyunting baris yang sudah tersimpan. Ia akan mulai berjalan pada
	 * hari pembatalan ditambahkan — dan justru di situ ia berguna, sebab
	 * menonaktifkan sebuah catatan keselamatan adalah perbuatan yang waktunya
	 * perlu diketahui.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Stempel ubah terakhir; disegarkan interseptor audit pada setiap UPDATE. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Representasi teks: kunci baris dan jenis, dipisah tanda hubung.
	 *
	 * <p>Membaca field langsung, bukan lewat getter, sehingga aman dipanggil
	 * pada objek yang sudah lepas dari sesi Hibernate. Bagian kosong diganti
	 * string kosong supaya hasilnya tidak pernah memuat kata "null".</p>
	 *
	 * @return teks ringkas untuk log dan layar
	 */
	public String toString() {
		return (id == null ? "" : id) + "-" + (jenis == null ? "" : jenis);
	}

	/**
	 * Kunci baris, dibangkitkan basis data ({@code IDENTITY}).
	 *
	 * @return kunci baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }

	/**
	 * Menetapkan kunci baris; dipakai Hibernate, bukan kode aplikasi.
	 *
	 * @param id kunci baris
	 */
	public void setId(Long id) { this.id = id; }

	/**
	 * Resep yang diperiksa atau dikonselingkan.
	 *
	 * <p>Getter DESTRUKTIF: hasil {@code check(...)} ditulis balik ke field.
	 * {@code check} menormalkan proksi malas Hibernate yang sudah lepas dari
	 * sesinya menjadi {@code null}, mencegah {@code LazyInitializationException}
	 * ketika objek dibaca di luar sesi. Memanggilnya karena itu dapat mengubah
	 * keadaan objek dan bukan pembacaan murni.</p>
	 *
	 * <p>{@code nullable = false}. Bersama {@link #getJenis()} dan
	 * {@link #getAktif()}, relasi ini menentukan keunikan yang dijaga
	 * pemanggil: satu resep hanya boleh punya satu catatan aktif per jenis.
	 * Penting dicatat bahwa keunikan itu ditegakkan KODE APLIKASI —
	 * {@code cariAktif} mencari lebih dulu, lalu {@code catat} mengembalikan
	 * catatan yang sudah ada alih-alih membuat yang kedua — dan TIDAK ada
	 * batasan unik di basis data yang mendukungnya. Antara pencarian dan
	 * penyimpanan ada jeda, sehingga dua permintaan yang benar-benar bersamaan
	 * dapat sama-sama tidak menemukan apa pun dan sama-sama menyimpan.</p>
	 *
	 * <p>Akibat dari duplikat itu terbatas dan tidak berbahaya: dua catatan
	 * yang keduanya sah, dan {@code cariAktif} akan memilih yang ber-id
	 * terbesar. Tidak ada angka yang menjadi salah. Bandingkan dengan
	 * {@link ApotikPostingLink}, di mana duplikat berarti jurnal ganda dan
	 * karena itu memang dijaga batasan unik basis data.</p>
	 *
	 * @return resep terkait, atau {@code null} bila proksinya lepas
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "resep", nullable = false)
	public Resep getResep() { resep = check(resep); return resep; }

	/**
	 * Menetapkan resep terkait.
	 *
	 * @param resep resep; wajib terisi sebelum disimpan
	 */
	public void setResep(Resep resep) { this.resep = resep; }

	/**
	 * Jenis catatan; {@link #JENIS_DOUBLE_CHECK} atau {@link #JENIS_KONSELING}.
	 *
	 * <p>Keanggotaannya dijaga {@code ApotikDispensingHelper.jenisSah} sebelum
	 * menyentuh entity; kolomnya sendiri sekadar teks 24 karakter tanpa
	 * batasan. Perbedaan antara kedua jenis bukan sekadar label: hanya
	 * DOUBLE_CHECK yang menuntut {@link #getPenyiapUserId()} terisi dan hanya
	 * DOUBLE_CHECK yang tunduk pada aturan pemeriksa-harus-orang-lain.
	 * KONSELING boleh dilakukan siapa saja, termasuk penyiapnya sendiri, karena
	 * gunanya menjelaskan pemakaian obat kepada pasien — bukan memeriksa
	 * pekerjaan orang lain.</p>
	 *
	 * @return jenis catatan
	 */
	@Column(name = "jenis", nullable = false, length = 24)
	public String getJenis() { return jenis; }

	/**
	 * Menetapkan jenis catatan.
	 *
	 * @param jenis salah satu konstanta {@code JENIS_*}
	 */
	public void setJenis(String jenis) { this.jenis = jenis; }

	/**
	 * Akun yang MENYIAPKAN obat — pembanding untuk aturan pemeriksa kedua.
	 *
	 * <p><b>Pernyataan pengirim, bukan fakta terverifikasi.</b> Nilai ini datang
	 * dari payload permintaan ({@code penyiap_user_id}); tidak ada catatan
	 * peladen tentang siapa yang benar-benar menyiapkan obat yang dapat
	 * dijadikan pembanding, dan karena itu tidak ada yang memverifikasinya.
	 * Peladen hanya memastikan nilainya BERBEDA dari akun yang sedang login —
	 * penjagaan yang menangkap petugas yang mengisi namanya sendiri, tetapi
	 * tidak menangkap petugas yang mengetikkan id rekan mana pun.</p>
	 *
	 * <p>Sifat itu perlu disebut setiap kali kolom ini dibaca sebagai bukti.
	 * Sebuah catatan DOUBLE_CHECK menyatakan bahwa seseorang mengaku memeriksa
	 * pekerjaan orang yang ia sebutkan; ia tidak membuktikan bahwa dua orang
	 * benar-benar terlibat. Untuk keperluan pemeriksaan mutu, bacalah kolom ini
	 * bersama {@link #getPelakuUserId()} — yang justru terverifikasi, karena
	 * diambil dari sesi — dan perlakukan pasangannya sebagai klaim.</p>
	 *
	 * <p>Kosong untuk catatan {@link #JENIS_KONSELING}; wajib terisi untuk
	 * {@link #JENIS_DOUBLE_CHECK}, dijaga pemanggil dan bukan oleh kolomnya
	 * sendiri (kolom ini {@code nullable}).</p>
	 *
	 * @return id akun penyiap menurut pengirim, atau {@code null}
	 */
	@Column(name = "penyiap_user_id", length = 60)
	public String getPenyiapUserId() { return penyiapUserId; }

	/**
	 * Menetapkan id akun penyiap.
	 *
	 * @param penyiapUserId id akun penyiap menurut pengirim
	 */
	public void setPenyiapUserId(String penyiapUserId) { this.penyiapUserId = penyiapUserId; }

	/**
	 * Akun yang melakukan pemeriksaan/konseling ini.
	 *
	 * <p>Berbeda dari {@link #getPenyiapUserId()}, nilai ini TERVERIFIKASI:
	 * pemanggil mengisinya dari {@code tbmuser.getUserId()} — identitas sesi
	 * yang sedang berjalan — dan tidak pernah dari payload. Inilah satu-satunya
	 * identitas pada baris ini yang benar-benar dapat dipertanggungjawabkan,
	 * dan karena itu satu-satunya yang layak dipakai ketika sebuah pemeriksaan
	 * dipersoalkan.</p>
	 *
	 * <p>{@code nullable = false} di tingkat kolom, sehingga catatan tanpa
	 * pelaku tidak dapat tersimpan — catatan keselamatan yang tidak menyebut
	 * siapa yang melakukannya tidak bernilai apa pun.</p>
	 *
	 * @return id akun pelaku, diambil dari sesi
	 */
	@Column(name = "pelaku_user_id", nullable = false, length = 60)
	public String getPelakuUserId() { return pelakuUserId; }

	/**
	 * Menetapkan id akun pelaku.
	 *
	 * @param pelakuUserId id akun pelaku; wajib terisi sebelum disimpan
	 */
	public void setPelakuUserId(String pelakuUserId) { this.pelakuUserId = pelakuUserId; }

	/**
	 * Nama tampil pelaku, disalin dari sesi saat pencatatan.
	 *
	 * <p>Snapshot yang disengaja, seperti
	 * {@link ApotikPembayaranTransaksi#getNamaCaraBayar()}: nama pegawai boleh
	 * berubah, akun boleh dinonaktifkan, dan catatan keselamatan yang dibaca
	 * bertahun-tahun kemudian harus tetap menyebut nama yang berlaku saat
	 * pemeriksaan dilakukan. Yang mengikat tetap {@link #getPelakuUserId()};
	 * kolom ini untuk dibaca manusia.</p>
	 *
	 * @return nama pelaku saat pencatatan, atau {@code null}
	 */
	@Column(name = "pelaku_nama", length = 160)
	public String getPelakuNama() { return pelakuNama; }

	/**
	 * Menetapkan nama tampil pelaku.
	 *
	 * @param pelakuNama nama pelaku
	 */
	public void setPelakuNama(String pelakuNama) { this.pelakuNama = pelakuNama; }

	/**
	 * Catatan bebas pemeriksa atau konselor.
	 *
	 * <p>Kolom {@code text} tanpa batas panjang praktis — tempat yang tepat
	 * untuk menuliskan apa yang ditemukan pada pemeriksaan, atau apa yang
	 * dijelaskan kepada pasien. Isinya dapat memuat keterangan klinis; berbeda
	 * dari {@link AntreanFarmasi#getCatatanPublik()}, kolom ini tidak pernah
	 * ditampilkan ke layar publik.</p>
	 *
	 * @return catatan, atau {@code null}
	 */
	@Column(name = "catatan", columnDefinition = "text")
	public String getCatatan() { return catatan; }

	/**
	 * Menetapkan catatan pemeriksa/konselor.
	 *
	 * @param catatan catatan
	 */
	public void setCatatan(String catatan) { this.catatan = catatan; }

	/**
	 * Penanda aktif; menentukan apakah catatan ini masih berlaku.
	 *
	 * <p>Mengembalikan {@code TRUE} bila kolom kosong. Arah bawaan itu perlu
	 * diperhatikan: ia condong MENGANGGAP BERLAKU. Untuk catatan keselamatan,
	 * arah yang lebih hati-hati sebenarnya kebalikannya — menganggap sebuah
	 * pemeriksaan belum dilakukan lebih aman daripada menganggapnya sudah.
	 * Dalam praktik persoalan itu tidak muncul karena pemanggil selalu mengisi
	 * penanda secara eksplisit saat membuat baris, dan karena penyaringan
	 * {@code cariAktif} menyaring dengan {@code Restrictions.eq("aktif",
	 * Boolean.TRUE)} atas KOLOM — sehingga baris berkolom NULL justru TIDAK
	 * terjaring, berperilaku seperti tidak aktif. Kolom dan getter dengan
	 * demikian menjawab berbeda untuk baris yang sama; yang berlaku di jalur
	 * pencarian adalah kolomnya.</p>
	 *
	 * <p><b>Sekarang berjalan satu arah.</b> Tidak ada satu pun aksi yang
	 * memadamkan penanda ini — lihat dokumentasi class. Selama keadaan itu
	 * bertahan, seluruh catatan yang pernah dibuat berlaku selamanya, dan
	 * kesalahan pencatatan tidak dapat dikoreksi lewat aplikasi.</p>
	 *
	 * @return penanda aktif; {@code TRUE} bila kolom kosong
	 */
	@Column(name = "aktif")
	public Boolean getAktif() { return aktif == null ? Boolean.TRUE : aktif; }

	/**
	 * Menetapkan penanda aktif.
	 *
	 * <p>Saat ini hanya dipanggil dengan {@code TRUE}, pada saat baris dibuat.
	 * Bila pembatalan kelak ditambahkan, inilah setter yang dipakai — jangan
	 * menghapus barisnya, sebab jejak bahwa sebuah pemeriksaan pernah dicatat
	 * lalu dibatalkan justru bagian penting dari riwayatnya.</p>
	 *
	 * @param aktif {@code TRUE} bila catatan berlaku
	 */
	public void setAktif(Boolean aktif) { this.aktif = aktif; }

	/**
	 * Waktu pemeriksaan/konseling dicatat.
	 *
	 * <p>Diisi pemanggil dengan waktu peladen pada saat pencatatan, bukan waktu
	 * yang dikirim klien. Untuk catatan keselamatan itu penting: urutan waktu
	 * antara penyiapan, pemeriksaan, dan penyerahan obat adalah bagian dari
	 * yang diperiksa ketika terjadi kesalahan pengobatan.</p>
	 *
	 * @return waktu pencatatan
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu", nullable = false)
	public Date getWaktu() { return waktu; }

	/**
	 * Menetapkan waktu pencatatan.
	 *
	 * @param waktu waktu pencatatan
	 */
	public void setWaktu(Date waktu) { this.waktu = waktu; }

	/**
	 * Nama tampil pelaku pencatatan (bayangan audit).
	 *
	 * <p>Pada entity ini nilainya bertumpang tindih dengan
	 * {@link #getPelakuUserId()} — pemanggil mengisi keduanya dari sesi yang
	 * sama. Tumpang tindih itu disengaja: {@code pelakuUserId} adalah bagian
	 * dari MAKNA catatan (siapa yang memeriksa), sedangkan {@code oleh} adalah
	 * bayangan audit teknis yang ada di setiap entity. Keduanya kebetulan sama
	 * di sini karena yang mencatat adalah yang memeriksa; pada hari sebuah
	 * jalur administratif mengubah baris ini, keduanya akan berbeda dan
	 * perbedaannya justru informasi.</p>
	 *
	 * @return nama pelaku, atau {@code null}
	 */
	public String getOleh() { return oleh; }

	/**
	 * Menetapkan nama pelaku — MENGABAIKAN nilai kosong, tidak menimpanya.
	 *
	 * <p>Menolak {@code null} dan teks berisi spasi saja secara diam. Bentuk ini
	 * seragam di basis kode dan merupakan keharusan teknis: kolom bayangan
	 * audit ini melewati jalur-jalur yang menyalin seluruh properti tanpa
	 * memilah, dan satu penyalinan dengan string kosong sudah cukup untuk
	 * menghapus jejak pelaku yang benar tanpa menyisakan apa pun di baris
	 * itu.</p>
	 *
	 * <p>Untuk catatan keselamatan pasien, kehilangan itu berarti sebuah
	 * pemeriksaan kehilangan penanggung jawabnya — persis hal yang paling
	 * dibutuhkan ketika kesalahan pengobatan ditelusuri. Harganya: nilai tidak
	 * dapat dikosongkan kembali lewat setter.</p>
	 *
	 * @param oleh nama pelaku; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) { if (oleh == null || oleh.trim().isEmpty()) return; this.oleh = oleh; }

	/**
	 * Identitas akun pelaku pencatatan (bayangan audit).
	 *
	 * @return id akun pelaku, atau {@code null}
	 */
	public String getOlehId() { return olehId; }

	/**
	 * Menetapkan id akun pelaku — MENGABAIKAN nilai kosong.
	 *
	 * <p>Berlaku seluruh pertimbangan pada {@link #setOleh(String)}.</p>
	 *
	 * @param olehId id akun pelaku; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) { if (olehId == null || olehId.trim().isEmpty()) return; this.olehId = olehId; }

	/**
	 * Stempel perubahan terakhir.
	 *
	 * <p>Selama tidak ada jalur yang menyunting baris, nilainya selalu sama
	 * dengan waktu pembuatan. Ia akan mulai bergerak pada hari pembatalan
	 * ditambahkan.</p>
	 *
	 * @return waktu ubah terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() { return tanggal_dirubah; }

	/**
	 * Menetapkan stempel perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu ubah
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }
}
