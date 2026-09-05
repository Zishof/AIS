package ais.database.model.sirs;

// Generated Dec 12, 2009 7:42:38 PM by Hibernate Tools 3.2.4.CR1

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

import ais.database.model.GeneralValueObject;
import ais.database.model.asset.Lokasi;

/**
 * <b>Jadwal praktek mingguan</b> tenaga medis (tabel {@code sirs.jadwal_dokter}) pada modul SIRS
 * AIS.
 *
 * <h2>Apa yang sebenarnya disimpan</h2>
 * <p>Satu baris {@code JadwalDokter} adalah sebuah <b>pola berulang mingguan</b>, bukan satu slot
 * waktu konkret. Kombinasi yang membentuknya:</p>
 * <ul>
 *   <li>{@link #getDokter() dokter} — tenaga medis yang praktek ({@code nullable = false});</li>
 *   <li>{@link #getHari() hari} — <b>nama hari</b> dalam bentuk teks ("Senin", "Selasa", ...),
 *   bukan tanggal;</li>
 *   <li>{@link #getShift() shift} — rentang jam praktek pada hari itu ({@code nullable = false});</li>
 *   <li>{@link #getLokasi() lokasi} — unit/cabang tempat praktek ({@code nullable = false});</li>
 *   <li>{@link #getPoly() poli} — poliklinik tujuan (boleh {@code null});</li>
 *   <li>{@link #getJadwalDokterDimulai() rentang berlaku} sampai
 *   {@link #getJadwalDokterSampai()} — masa berlaku pola tersebut;</li>
 *   <li>{@link #getWarna() warna} — pasangan warna untuk tampilan komponen kalender ZK.</li>
 * </ul>
 * <p>Tanggal pelayanan yang konkret <b>tidak</b> disimpan di sini. Tanggal itu dihitung di sisi
 * dokumen yang memakai jadwal ini, yaitu {@link Pendaftaran#getDilayaniTanggal()} dan
 * {@link BookingRegistrasi#getDilayaniTanggal()}, yang memajukan kalender hari demi hari dari
 * tanggal pendaftaran/booking sampai menemukan hari yang namanya cocok dengan {@link #getHari()}.
 * Baca peringatan pada {@link #getHari()} sebelum menulis kode yang mengisi field tersebut.</p>
 *
 * <h2>Pemakai jadwal ini</h2>
 * <p>Entity ini bukan entity tidur — ia dipakai secara luas:</p>
 * <ul>
 *   <li>{@code ais.action.master.sirs.JadwalDokterAction} — layar CRUD jadwal (termasuk tombol
 *   salin jadwal);</li>
 *   <li>paket {@code ais.action.master.sirs.jadwal_dokter} — sekumpulan composer kalender ZK
 *   ({@code CalendarJadwalDokterComposer}, {@code CalendarJadwalPolyComposer},
 *   {@code CalendarJadwalLokasiComposer}, {@code CalendarJadwalUmumComposer},
 *   {@code CalendarLihatJadwalComposer}, {@code CalendarLihatJadwalBulananComposer}) serta
 *   {@code AmbilJadwalHarian}/{@code AmbilJadwalBulanan} yang menjadi pemilih jadwal saat
 *   pendaftaran;</li>
 *   <li>{@link Pendaftaran} dan {@link BookingRegistrasi} — menyimpan jadwal terpilih dan
 *   menurunkan tanggal pelayanan serta nomor antrian darinya;</li>
 *   <li>{@code ais.action.master.sirs.util.CommonPendaftaranUtil} — pembangkit nomor antrian
 *   per jadwal;</li>
 *   <li>{@code ais.common.CommonSirs}, {@code ais.common.InitSirs}, dan
 *   {@code ais.common.newui.sirs.NewUiJadwalDokterController}.</li>
 * </ul>
 *
 * <h2>PENJAGA BENTROK JADWAL — tidak ada</h2>
 * <p>Ini karakteristik penting yang harus diketahui sebelum mengubah apa pun di sekitar entity ini.
 * <b>Tidak ada satu pun penjaga bentrok jadwal, baik di entity ini maupun di layar
 * penyimpannya.</b> Verifikasi dilakukan pada {@code JadwalDokterAction.onSave(...)}: seluruh isi
 * method itu hanya memeriksa <i>kelengkapan</i> field wajib (shift, tenaga medis, hari, poli,
 * lokasi harus terisi), lalu langsung memanggil {@code Common.refreshSaveOrUpdate(session, entity)}.
 * Tidak ada query pencarian jadwal yang sudah ada, tidak ada pembandingan rentang berlaku, dan
 * tidak ada batasan unik pada tingkat pemetaan. Akibat yang dapat terjadi:</p>
 * <ul>
 *   <li><b>Satu tenaga medis dapat dijadwalkan di dua lokasi sekaligus</b> pada hari dan shift yang
 *   sama — misalnya "dr. A, Senin, Shift Pagi, Lokasi Pusat" dan "dr. A, Senin, Shift Pagi, Lokasi
 *   Cabang" dapat hidup berdampingan tanpa peringatan apa pun.</li>
 *   <li><b>Jadwal kembar persis</b> (seluruh kombinasi dokter+hari+shift+lokasi+poli identik) dapat
 *   tersimpan berkali-kali. Ini bukan sekadar berantakan di layar: nomor antrian dihitung per
 *   {@code jadwalDokter} oleh {@code CommonPendaftaranUtil.generateNomorAntrian(...)}, sehingga
 *   dua jadwal kembar berarti <b>dua deret antrian terpisah</b> untuk dokter, hari, dan shift yang
 *   sebenarnya sama — pasien dapat memperoleh nomor antrian yang sama dari dua jadwal berbeda.
 *   Risiko ini diperbesar oleh tombol "Copy Jadwal" pada {@code JadwalDokterAction} yang memang
 *   dirancang untuk menduplikasi jadwal dengan cepat.</li>
 *   <li><b>Rentang berlaku yang tumpang tindih</b> untuk kombinasi yang sama tidak dicegah, dan
 *   urutan {@code jadwalDokterDimulai} &le; {@code jadwalDokterSampai} juga tidak divalidasi —
 *   rentang terbalik dapat tersimpan.</li>
 * </ul>
 * <p>Perlu ditegaskan juga bahwa entity ini <b>tidak menyimpan kuota</b>: tidak ada field kapasitas
 * atau jumlah pasien maksimum per slot. Karena itu pertanyaan "apakah satu slot jadwal bisa
 * di-booking dua pasien" terjawab: ya, dan itu memang disengaja — banyak {@link BookingRegistrasi}
 * dan {@link Pendaftaran} boleh menunjuk satu {@code JadwalDokter} yang sama, dan yang membedakan
 * mereka adalah nomor antrian, bukan pembatasan slot.</p>
 *
 * <h2>Rentang berlaku tidak menyaring apa pun</h2>
 * <p>Sama seperti penanda {@code aktif} pada {@link Dokter}, pasangan
 * {@link #getJadwalDokterDimulai()}/{@link #getJadwalDokterSampai()} <b>hanya ditampilkan, tidak
 * pernah dipakai sebagai filter query</b>. Penelusuran seluruh basis kode menunjukkan kedua
 * properti itu hanya muncul sebagai isian {@code Datebox} pada {@code JadwalDokterAction} dan
 * {@code CalendarJadwalDokterComposer}, serta sebagai teks pada renderer grid — tidak ada satu pun
 * {@code Restrictions} yang membatasi jadwal berdasarkan rentang berlakunya. Akibatnya
 * <b>jadwal yang sudah kedaluwarsa tetap muncul dan tetap dapat dipilih</b> saat pendaftaran
 * pasien baru maupun saat booking.</p>
 *
 * <h2>Pemetaan &amp; audit</h2>
 * <p>Dipetakan ke skema {@code sirs} tabel {@code jadwal_dokter} dengan {@code dynamicInsert} dan
 * {@code dynamicUpdate} aktif, serta {@link org.hibernate.envers.Audited} sehingga perubahan jadwal
 * terekam Envers — {@code JadwalDokterAction} memang menampilkan riwayat revisi shift lewat
 * {@code RevisiHelper}. Trio field audit bayangan ({@link #oleh}, {@link #olehId},
 * {@link #tanggal_dirubah}) beserta hook {@link #onUpdate()} adalah keharusan teknis pola AIS.</p>
 *
 * <p>Seluruh relasi dipetakan {@link FetchType#LAZY} dan getter-nya meresolusi proxy lewat
 * {@code check(...)} yang diwarisi dari {@link ais.database.model.GeneralValueObject}, sehingga
 * aman dipanggil pada instance yang sudah <i>detached</i>.</p>
 *
 * @see Dokter
 * @see Shift
 * @see Poly
 * @see Pendaftaran
 * @see BookingRegistrasi
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "jadwal_dokter")
public class JadwalDokter extends GeneralValueObject {

	/** Penanda versi serialisasi Java untuk entity ini. */
	private static final long serialVersionUID = -6970840500825359503L;

	/** Kunci utama teknis (auto-increment kolom {@code id}). Lihat {@link #getId()}. */
	private Long id;

	/**
	 * ID pengguna terakhir yang mengubah jadwal ini — bagian dari trio field audit bayangan.
	 * Lihat {@link #getOlehId()}.
	 */
	private String olehId;

	/**
	 * @return ID pengguna yang tercatat sebagai pengubah terakhir jadwal ini, atau {@code null}.
	 * @see #setOlehId(String)
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna pengubah terakhir, <b>mengabaikan nilai kosong</b>.
	 * <p>Nilai {@code null} atau string berisi spasi saja diabaikan agar jejak pengubah terakhir
	 * tidak terhapus oleh pemanggil tanpa konteks pengguna login.</p>
	 *
	 * @param olehId ID pengguna pengubah; {@code null}/kosong diabaikan.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/** Nama pengguna terakhir yang mengubah jadwal ini. Lihat {@link #getOleh()}. */
	private String oleh;

	/**
	 * Menyetel nama pengguna pengubah terakhir, <b>mengabaikan nilai kosong</b> dengan alasan yang
	 * sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/kosong diabaikan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna yang tercatat sebagai pengubah terakhir, atau {@code null}.
	 * @see #setOleh(String)
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@link javax.persistence.PreUpdate} yang mendelegasikan pengisian jejak audit ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} tepat sebelum
	 * Hibernate menerbitkan UPDATE untuk baris jadwal ini.
	 *
	 * <p>Hanya {@code @PreUpdate} (tidak ada {@code @PrePersist}), jadi pada penyimpanan pertama
	 * nilai {@link #tanggal_dirubah} berasal dari inisialisasi {@code new Date()} pada deklarasi
	 * field.</p>
	 *
	 * <p><b>Catatan format:</b> deklarasi method ini dan field {@code tanggal_dirubah} sengaja
	 * berada pada satu baris fisik — bentuk yang dihasilkan alat penyapu lintas ratusan entity.
	 * Jangan memisahkannya tanpa alasan.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = new Date();

	/**
	 * Menyetel waktu perubahan terakhir jadwal ini.
	 * <p>Pada alur normal ditimpa otomatis oleh {@link #onUpdate()} saat flush.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return waktu perubahan terakhir baris jadwal ini ({@link TemporalType#TIMESTAMP}). Ini
	 *         metadata audit, bukan bagian dari masa berlaku jadwal — untuk itu lihat
	 *         {@link #getJadwalDokterDimulai()} dan {@link #getJadwalDokterSampai()}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks jadwal dalam format
	 * {@code "<dokter> - <poli> - <hari> - <shift>"}.
	 *
	 * <h3>Mengapa tiga getter dipanggil tanpa memakai hasilnya</h3>
	 * <p>Tiga baris pertama ({@code getDokter()}, {@code getPoly()}, {@code getShift()}) sekilas
	 * tampak sia-sia karena nilai kembaliannya dibuang. Sebenarnya ketiganya dipanggil
	 * <b>demi efek sampingnya</b>: masing-masing getter menjalankan {@code check(...)} yang
	 * meresolusi proxy Hibernate yang mungkin masih lazy dan <i>menulis hasilnya kembali ke
	 * field</i>. Setelah tiga panggilan itu, ekspresi {@code return} di bawahnya boleh membaca
	 * field {@link #dokter}, {@link #poly}, dan {@link #shift} secara langsung dengan aman —
	 * termasuk pada instance yang sudah <i>detached</i>, yang tanpa resolusi tersebut akan
	 * melempar {@code LazyInitializationException}. Jadi urutan tiga panggilan itu <b>tidak boleh
	 * dihapus</b> oleh pembersihan kode otomatis, meskipun alat analisis statis menandainya sebagai
	 * pemanggilan tanpa efek.</p>
	 *
	 * <h3>Penanganan null yang tidak seragam</h3>
	 * <p>Hanya {@link #poly} yang dijaga eksplisit ({@code poly == null ? "" : poly.toString()}).
	 * {@link #dokter}, {@link #hari}, dan {@link #shift} dirangkai lewat operator {@code +},
	 * sehingga nilai {@code null} akan tampil sebagai literal {@code "null"} pada teks jadwal
	 * alih-alih menjadi kosong. Ini tidak menimbulkan exception, tetapi menghasilkan tampilan
	 * seperti {@code "null -  - Senin - null"} bila jadwal belum lengkap. Pada data yang dibuat
	 * lewat {@code JadwalDokterAction} hal ini tidak terjadi karena dokter dan shift wajib
	 * terisi.</p>
	 *
	 * <p>Teks yang dihasilkan method ini muncul di banyak tempat: label shift dokter pada form
	 * pendaftaran ({@code CommonPendaftaranUtil}), judul acara pada kalender jadwal, dan — yang
	 * paling perlu diperhatikan — <b>disalin ke kolom basis data</b> lewat
	 * {@link #getDeskripsi()}.</p>
	 *
	 * @return teks jadwal untuk ditampilkan di UI; tidak pernah {@code null}.
	 */
	public String toString() {
		getDokter();
		getPoly();
		getShift();
		return dokter + " - " + (poly == null ? "" : poly.toString()) + " - " + hari + " - " + shift;
	}

	/** Shift (rentang jam) praktek pada hari tersebut. Lihat {@link #getShift()}. */
	private Shift shift;

	/** Tenaga medis yang menjalani jadwal ini. Lihat {@link #getDokter()}. */
	private Dokter dokter;

	/** Unit/cabang tempat jadwal ini berlaku. Lihat {@link #getLokasi()}. */
	private Lokasi lokasi;

	/** Poliklinik tujuan jadwal ini. Lihat {@link #getPoly()}. */
	private Poly poly;

	/**
	 * Nama hari berulangnya jadwal ini, <b>disimpan sebagai teks</b> dan berisi default
	 * {@code "Senin"}. Lihat peringatan penting pada {@link #getHari()}.
	 */
	private String hari = "Senin";

	/**
	 * Pasangan warna tampilan kalender, default {@code "#A32929,#D96666"}.
	 * Lihat {@link #getWarna()}.
	 */
	private String warna = "#A32929,#D96666";

	/**
	 * Tanggal awal masa berlaku jadwal, diinisialisasi ke {@code new Date()}.
	 * Lihat {@link #getJadwalDokterDimulai()}.
	 */
	private Date jadwalDokterDimulai = new Date();

	/**
	 * Tanggal akhir masa berlaku jadwal; {@code null} berarti tanpa batas akhir.
	 * Lihat {@link #getJadwalDokterSampai()}.
	 */
	private Date jadwalDokterSampai;

	/** Catatan bebas mengenai jadwal ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/**
	 * Salinan teks jadwal yang ditulis ulang pada setiap pembacaan — lihat peringatan pada
	 * {@link #getDeskripsi()}.
	 */
	private String deskripsi;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate, sekaligus dipakai
	 * {@code JadwalDokterAction} dan composer kalender saat petugas menambah jadwal baru.
	 *
	 * <p>Tiga field sudah terisi sejak awal: {@link #hari} = {@code "Senin"}, {@link #warna} =
	 * {@code "#A32929,#D96666"}, dan {@link #jadwalDokterDimulai} = {@code new Date()} (ditambah
	 * {@link #tanggal_dirubah}). Sisanya {@code null}, termasuk relasi wajib
	 * {@link #dokter}, {@link #shift}, dan {@link #lokasi} yang harus diisi sebelum penyimpanan.</p>
	 */
	public JadwalDokter() {

	}

	/**
	 * Konstruktor pembungkus ringan yang hanya mengisi kunci utama.
	 *
	 * <p>Berguna untuk membuat "referensi kurus" ke sebuah jadwal — misalnya sebagai nilai
	 * pembanding pada {@code Restrictions.eq("jadwalDokter", new JadwalDokter(id))} — tanpa perlu
	 * memuat barisnya dari basis data. Ingat bahwa
	 * {@link ais.database.model.GeneralValueObject#equals(Object)} dibandingkan berdasarkan
	 * {@code id}, sehingga instance hasil konstruktor ini {@code equals()} dengan instance penuh
	 * yang ber-ID sama.</p>
	 *
	 * <p><b>Jangan menyimpan instance hasil konstruktor ini ke basis data.</b> Seluruh field lain
	 * bernilai {@code null} kecuali default {@link #hari}, {@link #warna}, dan
	 * {@link #jadwalDokterDimulai}; melakukan {@code merge}/{@code update} atasnya akan menimpa
	 * baris asli dengan nilai kosong, termasuk mengosongkan relasi wajib.</p>
	 *
	 * @param id kunci utama jadwal yang hendak direferensikan.
	 */
	public JadwalDokter(Long id) {
		this.id = id;
	}

	/**
	 * @return kunci utama teknis jadwal ini, atau {@code null} bila belum tersimpan. Nilai inilah
	 *         yang dipakai komponen kalender ZK sebagai judul acara dan kemudian dibaca kembali
	 *         oleh {@code AmbilJadwalHarian} lewat
	 *         {@code Restrictions.idEq(Long.parseLong(ce.getTitle()))} untuk memuat ulang jadwal
	 *         yang diklik petugas.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama teknis.
	 * <p>Diisi Hibernate setelah INSERT; jangan mengubahnya pada instance terkelola karena
	 * identitas object ({@code equals}) bergantung padanya.</p>
	 *
	 * @param id kunci utama teknis.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan shift (rentang jam praktek) jadwal ini, setelah meresolusi proxy lazy-nya
	 * lewat {@code check(...)} dan menulis hasilnya kembali ke field.
	 *
	 * <p>Kolomnya {@code shift} dan dipetakan {@code nullable = false}, sehingga setiap jadwal
	 * wajib menyebut shift. {@code JadwalDokterAction} menegakkan ini di sisi UI dengan menolak
	 * penyimpanan bila combo shift belum dipilih, dan daftar shift-nya sendiri bersifat
	 * <i>cascading</i> — baru terisi setelah lokasi dipilih, karena shift didefinisikan per
	 * lokasi.</p>
	 *
	 * <p>Karena {@code nullable = false} sekaligus tanpa penjaga {@code null} di
	 * {@link #toString()}, kode lain (mis. renderer grid pada {@code JadwalDokterAction} yang
	 * memanggil {@code jadwalDokter.getShift().toString()}) memang mengandalkan shift selalu
	 * terisi. Data yang masuk lewat jalur non-UI tetap dapat melanggar asumsi itu bila batasan
	 * kolom tidak ditegakkan basis data.</p>
	 *
	 * @return shift jadwal ini.
	 * @see Shift
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "shift", nullable = false)
	public Shift getShift() {
		shift = check(shift);
		return this.shift;
	}

	/**
	 * Menyetel shift jadwal ini.
	 * <p>Wajib diisi (kolom {@code nullable = false}). Perlu disadari bahwa mengganti shift pada
	 * jadwal yang sudah dipakai <b>tidak</b> mengubah {@link Pendaftaran} maupun
	 * {@link BookingRegistrasi} yang terlanjur menunjuk jadwal ini: dokumen-dokumen itu menyimpan
	 * referensi ke jadwal, bukan salinan shift-nya, sehingga perubahan shift berlaku surut pada
	 * seluruh riwayat yang menunjuknya.</p>
	 *
	 * @param shift shift praktek.
	 */
	public void setShift(Shift shift) {
		this.shift = shift;
	}

	/**
	 * Mengembalikan tenaga medis pemilik jadwal ini, setelah meresolusi proxy lazy-nya lewat
	 * {@code check(...)}.
	 *
	 * <p>Kolomnya {@code dokter}, {@code nullable = false}. Ingat bahwa {@link Dokter} adalah
	 * master <i>tenaga medis</i> secara umum — bisa dokter, bidan, perawat, atau siswa praktek —
	 * sehingga jadwal ini juga dipakai untuk menjadwalkan shift perawat, bukan hanya praktek
	 * dokter.</p>
	 *
	 * <p><b>Tidak ada penjaga bentrok.</b> Tenaga medis yang sama dapat memiliki beberapa baris
	 * jadwal untuk hari dan shift yang sama di lokasi berbeda; lihat pembahasan lengkap pada
	 * Javadoc kelas ini.</p>
	 *
	 * @return tenaga medis pemilik jadwal.
	 * @see Dokter
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dokter", nullable = false)
	public Dokter getDokter() {
		dokter = check(dokter);
		return this.dokter;
	}

	/**
	 * Menyetel tenaga medis pemilik jadwal ini.
	 * <p>Wajib diisi. Tidak ada pemeriksaan apakah tenaga medis tersebut sudah punya jadwal lain
	 * pada hari+shift yang sama, dan tidak ada pemeriksaan apakah tenaga medis tersebut masih
	 * aktif ({@link Dokter#getAktif()} tidak dipakai sebagai filter di mana pun).</p>
	 *
	 * @param dokter tenaga medis pemilik jadwal.
	 */
	public void setDokter(Dokter dokter) {
		this.dokter = dokter;
	}

	/**
	 * Mengembalikan lokasi (unit/cabang) tempat jadwal ini berlaku, setelah meresolusi proxy
	 * lazy-nya lewat {@code check(...)}.
	 *
	 * <p>Kolomnya {@code lokasi}, {@code nullable = false}. Lokasi berperan ganda pada entity ini:
	 * selain menandai tempat praktek, ia juga menjadi <b>penentu daftar shift yang tersedia</b> —
	 * {@code JadwalDokterAction} memuat ulang combo shift setiap kali lokasi berubah. Lokasi juga
	 * menjadi filter utama pada pencarian jadwal ({@code initCriteria}) dan pada pemilih jadwal
	 * {@code AmbilJadwalHarian}.</p>
	 *
	 * <p>Karena {@link Pendaftaran} dan {@link BookingRegistrasi} juga menyimpan lokasinya
	 * sendiri, perhatikan bahwa <b>tidak ada penjaga yang memastikan lokasi dokumen sama dengan
	 * lokasi jadwal yang dipilih</b>: sebuah pendaftaran di lokasi A secara teknis dapat menunjuk
	 * jadwal milik lokasi B bila jadwal itu terpilih lewat jalur yang tidak menyaring lokasi.</p>
	 *
	 * @return lokasi tempat jadwal ini berlaku.
	 * @see ais.database.model.asset.Lokasi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi", nullable = false)
	public Lokasi getLokasi() {
		lokasi = check(lokasi);
		return this.lokasi;
	}

	/**
	 * Menyetel lokasi tempat jadwal ini berlaku.
	 * <p>Wajib diisi. Mengubah lokasi setelah jadwal dipakai berpotensi membuat jadwal tidak
	 * konsisten dengan shift yang terlanjur dipilih, karena shift didefinisikan per lokasi dan
	 * relasi shift tidak ikut disesuaikan oleh setter ini.</p>
	 *
	 * @param lokasi unit/cabang tempat praktek.
	 */
	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * Menyetel nama hari berulangnya jadwal ini.
	 *
	 * <p><b>Nilai yang disetel harus persis salah satu anggota {@code ais.common.Common.haris}</b>,
	 * yaitu {@code "Minggu"}, {@code "Senin"}, {@code "Selasa"}, {@code "Rabu"}, {@code "Kamis"},
	 * {@code "Jum'at"} (perhatikan tanda petik satu di dalamnya), atau {@code "Sabtu"}. Alasannya
	 * dijelaskan lengkap pada {@link #getHari()}: nilai di luar daftar itu membuat perhitungan
	 * tanggal pelayanan <b>berputar tanpa henti</b>.</p>
	 *
	 * <p>Setter ini <b>tidak melakukan validasi apa pun</b> — ia menerima string sembarang,
	 * termasuk {@code null} dan string kosong. Perlindungan satu-satunya berada di lapisan UI,
	 * yaitu combo hari pada {@code JadwalDokterAction} yang isinya memang dibangun dengan
	 * mengiterasi {@code Common.haris}. Data yang masuk lewat impor, skrip SQL, atau integrasi lain
	 * tidak terlindungi.</p>
	 *
	 * @param hari nama hari; harus salah satu anggota {@code Common.haris}.
	 */
	public void setHari(String hari) {
		this.hari = hari;
	}

	/**
	 * Mengembalikan nama hari berulangnya jadwal ini (kolom {@code hari}, maksimal 20 karakter),
	 * default {@code "Senin"}.
	 *
	 * <h3>Mengapa nilai field ini kritis</h3>
	 * <p>Hari disimpan sebagai <b>teks bahasa Indonesia</b>, bukan sebagai angka atau enum. Nilai
	 * itu kemudian dicocokkan dengan nama hari kalender oleh dua method yang menghitung tanggal
	 * pelayanan, yaitu {@link Pendaftaran#getDilayaniTanggal()} dan
	 * {@link BookingRegistrasi#getDilayaniTanggal()}. Keduanya memakai pola yang sama:</p>
	 * <pre>{@code
	 * String hari = getJadwalDokter().getHari();
	 * String currHari = Common.haris[calendar.get(Calendar.DAY_OF_WEEK) - 1];
	 * while (!hari.equalsIgnoreCase(currHari)) {
	 *     calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
	 *     currHari = Common.haris[calendar.get(Calendar.DAY_OF_WEEK) - 1];
	 *     dilayaniTanggal = calendar.getTime();
	 * }
	 * }</pre>
	 *
	 * <h3>Dua bahaya nyata</h3>
	 * <ol>
	 *   <li><b>Perulangan tanpa henti.</b> Perulangan di atas hanya berhenti ketika nama hari
	 *   jadwal cocok dengan salah satu dari tujuh nilai {@code Common.haris}. Bila field ini berisi
	 *   nilai di luar daftar itu — salah ketik, nama hari berbahasa lain, string kosong, atau
	 *   {@code "Jumat"} tanpa tanda petik satu sementara daftar resminya {@code "Jum'at"} —
	 *   kondisi berhenti tidak akan pernah tercapai dan perulangan berjalan selamanya, menahan
	 *   thread permintaan sampai server dihentikan. Ini bukan sekadar kesalahan tampilan melainkan
	 *   gangguan ketersediaan. Perhatikan khusus nilai {@code "Jum'at"}: tanda petik satu di
	 *   dalamnya rawan berubah akibat perbedaan pengodean karakter, penyuntingan manual, atau
	 *   proses impor.</li>
	 *   <li><b>{@link NullPointerException}.</b> Bila field ini {@code null}, baris
	 *   {@code hari.equalsIgnoreCase(currHari)} langsung melempar NPE. Kolomnya sendiri tidak
	 *   dinyatakan {@code nullable = false}, dan
	 *   {@code JadwalDokterAction.onSave(...)} secara eksplisit menyetel {@code null} ketika combo
	 *   hari kosong — meskipun jalur itu praktis tidak tercapai karena validasi sebelumnya sudah
	 *   menolak combo hari yang kosong.</li>
	 * </ol>
	 *
	 * <p>Karena pencocokannya memakai {@code equalsIgnoreCase}, perbedaan huruf besar-kecil aman;
	 * yang tidak aman adalah perbedaan ejaan dan spasi.</p>
	 *
	 * @return nama hari berulangnya jadwal; secara nominal salah satu anggota
	 *         {@code Common.haris}, tetapi tidak dijamin oleh entity ini.
	 */
	@Column(name = "hari", length = 20)
	public String getHari() {
		return hari;
	}

	/**
	 * Menyetel pasangan warna tampilan kalender.
	 * <p>Kolomnya dibatasi 20 karakter, sementara format yang diharapkan adalah dua kode warna
	 * heksadesimal dipisah koma (15 karakter untuk format {@code #RRGGBB,#RRGGBB}). Format lain
	 * yang lebih panjang — misalnya menambahkan warna ketiga — akan melebihi batas kolom dan gagal
	 * saat flush.</p>
	 *
	 * @param warna pasangan kode warna, mis. {@code "#A32929,#D96666"}.
	 */
	public void setWarna(String warna) {
		this.warna = warna;
	}

	/**
	 * @return pasangan kode warna untuk menggambar jadwal ini pada komponen kalender ZK, default
	 *         {@code "#A32929,#D96666"} (kolom {@code warna}, boleh {@code null}, maksimal 20
	 *         karakter).
	 *         <p>Nilainya berupa dua kode warna heksadesimal dipisah koma: bagian pertama untuk
	 *         warna utama/latar acara, bagian kedua untuk warna pendampingnya. Field ini murni
	 *         kosmetik — tidak ada logika bisnis yang bergantung padanya — sehingga aman diubah
	 *         massal untuk menyeragamkan tampilan kalender.</p>
	 */
	@Column(name = "warna", nullable = true, length = 20)
	public String getWarna() {
		return warna;
	}

	/**
	 * @return tanggal awal masa berlaku jadwal ini, dipetakan {@link TemporalType#DATE} sehingga
	 *         hanya menyimpan tanggal tanpa jam. Diinisialisasi ke {@code new Date()} pada
	 *         konstruksi.
	 *         <p><b>Peringatan: nilai ini tidak menyaring apa pun.</b> Sebagaimana dijelaskan pada
	 *         Javadoc kelas, tidak ada satu pun query di basis kode yang membatasi jadwal
	 *         berdasarkan rentang berlakunya — nilai ini hanya ditampilkan pada grid dan form.
	 *         Jadwal yang secara nominal belum mulai berlaku pun tetap dapat dipilih untuk
	 *         pendaftaran maupun booking.</p>
	 *         <p>Getter ini tidak beranotasi {@link Column @Column}, jadi dipetakan ke kolom
	 *         {@code jadwal_dokter_dimulai} berdasarkan konvensi penamaan properti.</p>
	 */
	@Temporal(TemporalType.DATE)
	public Date getJadwalDokterDimulai() {
		return jadwalDokterDimulai;
	}

	/**
	 * Menyetel tanggal awal masa berlaku jadwal.
	 * <p>Tidak ada validasi bahwa nilainya lebih awal daripada
	 * {@link #setJadwalDokterSampai(Date)}; rentang terbalik dapat tersimpan tanpa peringatan.</p>
	 *
	 * @param jadwalDokterDimulai tanggal awal berlaku; boleh {@code null}.
	 */
	public void setJadwalDokterDimulai(Date jadwalDokterDimulai) {
		this.jadwalDokterDimulai = jadwalDokterDimulai;
	}

	/**
	 * @return tanggal akhir masa berlaku jadwal ({@link TemporalType#DATE}), atau {@code null} yang
	 *         secara konvensi berarti "tanpa batas akhir". Nilai bawaannya memang {@code null}.
	 *         <p><b>Peringatan yang sama seperti {@link #getJadwalDokterDimulai()}:</b> tanggal
	 *         akhir ini tidak pernah dipakai sebagai filter, sehingga <b>jadwal yang sudah
	 *         kedaluwarsa tetap muncul dan tetap dapat dipilih</b> pada layar pendaftaran pasien
	 *         dan booking. Jangan mengandalkan field ini untuk menonaktifkan jadwal; satu-satunya
	 *         cara efektif menghentikan sebuah jadwal saat ini adalah menghapus barisnya.</p>
	 *         <p>Dipetakan ke kolom {@code jadwal_dokter_sampai} berdasarkan konvensi.</p>
	 */
	@Temporal(TemporalType.DATE)
	public Date getJadwalDokterSampai() {
		return jadwalDokterSampai;
	}

	/**
	 * Menyetel tanggal akhir masa berlaku jadwal.
	 * <p>{@code null} berarti tanpa batas akhir. Ingat bahwa menyetel tanggal akhir <b>tidak</b>
	 * menghentikan jadwal ini dari daftar pilihan — lihat peringatan pada
	 * {@link #getJadwalDokterSampai()}.</p>
	 *
	 * @param jadwalDokterSampai tanggal akhir berlaku; boleh {@code null}.
	 */
	public void setJadwalDokterSampai(Date jadwalDokterSampai) {
		this.jadwalDokterSampai = jadwalDokterSampai;
	}

	/**
	 * @return catatan bebas mengenai jadwal ini, atau {@code null}. Ditampilkan sebagai kolom
	 *         tersendiri pada grid {@code JadwalDokterAction}.
	 *         <p>Tidak beranotasi {@link Column @Column}, jadi dipetakan ke kolom
	 *         {@code keterangan} berdasarkan konvensi. Jangan mengacaukannya dengan
	 *         {@link #getDeskripsi()} yang <b>bukan</b> catatan pengguna melainkan salinan
	 *         otomatis dari {@link #toString()}.</p>
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel catatan bebas mengenai jadwal ini. Ini satu-satunya field teks pada entity ini yang
	 * benar-benar mempertahankan nilai yang disetel pengguna — bandingkan dengan
	 * {@link #setDeskripsi(String)}.
	 *
	 * @param keterangan catatan bebas; boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan poliklinik tujuan jadwal ini, setelah meresolusi proxy lazy-nya lewat
	 * {@code check(...)}.
	 *
	 * <p>Berbeda dari {@link #getShift()}, {@link #getDokter()}, dan {@link #getLokasi()}, kolom
	 * {@code poly} <b>tidak</b> dinyatakan {@code nullable = false} pada pemetaan — anotasi
	 * {@link JoinColumn @JoinColumn} di sini hanya menyebut nama kolom. Jadi pada tingkat basis
	 * data, jadwal tanpa poli diperbolehkan, dan {@link #toString()} memang menyiapkan penjaga
	 * {@code null} khusus untuk relasi ini.</p>
	 *
	 * <p>Namun di sisi lain {@code JadwalDokterAction.onSave(...)} <b>menolak</b> penyimpanan bila
	 * combo poli belum dipilih. Jadi ada ketidakselarasan yang perlu diketahui: aturan "poli
	 * wajib" hanya hidup di lapisan UI layar tersebut, sementara pemetaan dan jalur non-UI (impor,
	 * skrip, composer kalender lain) tetap membolehkan poli kosong. Kode pembaca karena itu harus
	 * tetap memeriksa {@code null}, sebagaimana dilakukan renderer grid pada
	 * {@code JadwalDokterAction}.</p>
	 *
	 * @return poliklinik tujuan jadwal ini, atau {@code null}.
	 * @see Poly
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "poly")
	public Poly getPoly() {
		poly = check(poly);
		return poly;
	}

	/**
	 * Menyetel poliklinik tujuan jadwal ini.
	 *
	 * @param poly poliklinik tujuan; boleh {@code null} pada tingkat pemetaan, meskipun layar
	 *            {@code JadwalDokterAction} mewajibkannya.
	 */
	public void setPoly(Poly poly) {
		this.poly = poly;
	}

	/**
	 * Mengembalikan deskripsi jadwal — dengan <b>selalu menghitung ulang dan menimpa</b> nilai
	 * yang tersimpan.
	 *
	 * <h3>Getter destruktif: nilai yang disetel tidak pernah bertahan</h3>
	 * <p>Method ini tidak mengembalikan isi field {@link #deskripsi}, melainkan memanggil
	 * {@link #toString()} lalu <b>menugaskan hasilnya ke field</b> sebelum mengembalikannya. Ini
	 * instance dari pola "getter destruktif" yang berulang di model AIS, dan di sini efeknya
	 * paling terasa karena property {@code deskripsi} <b>ikut dipetakan ke kolom basis data</b> —
	 * getter ini tidak diberi {@link javax.persistence.Transient @Transient}, dan tanpa
	 * {@link Column @Column} eksplisit Hibernate memetakannya ke kolom {@code deskripsi}
	 * berdasarkan konvensi.</p>
	 *
	 * <p>Karena Hibernate mengakses entity ini lewat properti (anotasi ada di getter), method ini
	 * juga dipanggil saat flush. Konsekuensinya:</p>
	 * <ul>
	 *   <li><b>{@link #setDeskripsi(String)} praktis tidak berfungsi.</b> Nilai apa pun yang
	 *   disetel akan tertimpa oleh {@code toString()} pada pembacaan atau flush berikutnya.</li>
	 *   <li>Kolom {@code deskripsi} di basis data adalah <b>data turunan yang di-denormalisasi</b>,
	 *   bukan masukan pengguna. Isinya selalu berupa {@code "<dokter> - <poli> - <hari> -
	 *   <shift>"} pada saat baris terakhir disimpan.</li>
	 *   <li>Nilai tersimpan bisa menjadi <b>basi</b>: mengganti nama tenaga medis pada master
	 *   {@link Dokter} tidak memperbarui kolom {@code deskripsi} pada baris jadwal yang sudah ada
	 *   sampai baris itu tersentuh flush lagi. Jangan memakai kolom ini sebagai kriteria pencarian
	 *   atau sebagai sumber kebenaran pada laporan; bacalah relasinya langsung.</li>
	 *   <li>Karena {@code toString()} memanggil {@code getDokter()}/{@code getPoly()}/
	 *   {@code getShift()}, getter ini <b>memicu resolusi tiga proxy lazy</b> — artinya membaca
	 *   "sekadar deskripsi" dapat menerbitkan beberapa query tambahan per baris.</li>
	 * </ul>
	 *
	 * @return teks {@code "<dokter> - <poli> - <hari> - <shift>"} hasil {@link #toString()},
	 *         sekaligus menimpa field {@link #deskripsi} dengan nilai tersebut.
	 */
	public String getDeskripsi() {
		deskripsi = toString();
		return deskripsi;
	}

	/**
	 * Menyetel deskripsi jadwal — <b>praktis tanpa efek</b>.
	 *
	 * <p>Nilai yang disetel di sini akan tertimpa pada pembacaan berikutnya lewat
	 * {@link #getDeskripsi()}, yang selalu menghitung ulang dari {@link #toString()}. Karena
	 * Hibernate juga membaca lewat getter tersebut saat flush, nilai kustom tidak akan pernah
	 * sampai ke basis data. Setter ini pada praktiknya hanya ada agar pemetaan properti JavaBean
	 * lengkap; jangan memakainya untuk menyimpan catatan — pakai
	 * {@link #setKeterangan(String)}.</p>
	 *
	 * @param deskripsi nilai yang akan segera tertimpa.
	 */
	public void setDeskripsi(String deskripsi) {
		this.deskripsi = deskripsi;
	}

}
