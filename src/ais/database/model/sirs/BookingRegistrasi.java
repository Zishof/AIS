package ais.database.model.sirs;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.asset.Lokasi;

/**
 * <b>Booking pendaftaran</b> — janji temu pasien yang dibuat <i>sebelum</i> registrasi resmi
 * (tabel {@code sirs.booking_registrasi}) pada modul SIRS AIS.
 *
 * <h2>Posisi dalam alur pelayanan</h2>
 * <p>Verifikasi dari relasi antar entity (bukan dari penamaannya) menunjukkan rantai berikut:</p>
 * <pre>
 * BookingRegistrasi   &larr; kelas ini: janji temu, pasien belum datang
 *        &harr;  relasi DUA ARAH:
 *               booking_registrasi.pendaftaran  dan  pendaftaran.booking_registrasi
 * Pendaftaran         registrasi resmi saat pasien datang (rawat jalan / rawat inap / UGD)
 *        &darr;  diagnosa_penyakit.pendaftaran
 * DiagnosaPenyakit    episode diagnosa
 *        &darr;  kunjungan_dokter.diagnosa_penyakit
 * KunjunganDokter     satu kali visite tenaga medis
 * </pre>
 *
 * <p>Booking bersifat <b>opsional</b>: {@link Pendaftaran} dapat dibuat tanpa booking sama sekali
 * (pasien datang langsung), dan alur UGD bahkan secara eksplisit menyetel
 * {@code pendaftaran.setBookingRegistrasi(null)} karena kegawatdaruratan tidak mengenal janji
 * temu. Sebaliknya, sebuah booking yang belum pernah "ditebus" akan memiliki
 * {@link #getPendaftaran()} bernilai {@code null} — dan justru itulah yang menandainya masih
 * tersedia.</p>
 *
 * <h2>Bagaimana booking berubah menjadi pendaftaran</h2>
 * <p>Penebusan terjadi di layar {@code PendaftaranRawatJalanAction} dan
 * {@code PendaftaranRawatInapAction}. Alurnya:</p>
 * <ol>
 *   <li>Petugas memilih booking lewat {@code AmbilDataBookingRegistrasiBanbox}. Banbox ini
 *   menyaring dengan {@code Restrictions.isNull("pendaftaran")}, sehingga <b>booking yang sudah
 *   ditebus tidak lagi muncul</b> sebagai pilihan.</li>
 *   <li>Data booking disalin ke form pendaftaran: pasien, jadwal dokter, tanggal dilayani,
 *   penanda paket, dan daftar paket ({@code pendaftaran.setPakets(booking.getPakets())}).</li>
 *   <li>Saat simpan, kedua sisi relasi diikat: {@code pendaftaran.setBookingRegistrasi(booking)}
 *   dan — setelah pendaftaran tersimpan — {@code booking.setPendaftaran(pendaftaran)} diikuti
 *   {@code Common.refreshUpdate(session, booking)}.</li>
 * </ol>
 *
 * <p><b>Catatan tentang kekuatan penjaga penebusan ganda.</b> Penjaganya hanya berupa penyaringan
 * di banbox, bukan batasan basis data maupun pemeriksaan ulang saat simpan. Kolom
 * {@code booking_registrasi.pendaftaran} tidak dipetakan {@code unique}, dan alur simpan pada
 * {@code PendaftaranRawatJalanAction} bahkan memanggil {@code session.refresh(bookingRegistrasi)}
 * lalu langsung menimpa {@code setPendaftaran(...)} tanpa memeriksa apakah booking tersebut sudah
 * menunjuk pendaftaran lain. Artinya penjaga ini bersifat "periksa dulu, pakai belakangan"
 * (TOCTOU): dua petugas yang membuka layar pendaftaran bersamaan sama-sama melihat booking itu
 * tersedia, dan keduanya dapat menyimpannya. Pada layar booking sendiri
 * ({@code BookingRegistrasiAction}), tombol ubah dan hapus memang disembunyikan ketika
 * {@code getPendaftaran() != null} — perlindungan yang juga hanya di lapisan tampilan.</p>
 *
 * <h2>Nomor antrian</h2>
 * <p>{@link #getNomorAntrian()} diisi oleh
 * {@code CommonPendaftaranUtil.generateNomorAntrian(bookingRegistrasi, jadwalDokter)} sebagai
 * {@code max(nomorAntrian) + 1} di antara booking pada {@link JadwalDokter} yang sama. Nomor
 * inilah yang kemudian <b>diwarisi</b> oleh pendaftaran hasil penebusan: varian
 * {@code generateNomorAntrian(Pendaftaran, JadwalDokter)} langsung mengembalikan
 * {@code pendaftaran.getBookingRegistrasi().getNomorAntrian()} bila pendaftaran punya booking.
 * Jadi pasien yang sudah booking mempertahankan nomor antriannya saat datang.</p>
 *
 * <h2>Pola arsitektur yang muncul di kelas ini</h2>
 * <ul>
 *   <li><b>Setter yang menjalankan query</b> — {@link #setPasienKomunitas(Pasien)} adalah setter
 *   yang membuka {@link org.hibernate.Session} thread-local dan menjalankan criteria. Baca
 *   Javadoc-nya sebelum memakainya.</li>
 *   <li><b>Getter destruktif</b> — {@link #getTanggalBookingRegistrasi()},
 *   {@link #getKelasPerawatan()}, {@link #getBaru()}, {@link #getMerupakanPaket()},
 *   {@link #getBookingUntukTanggal()}, dan {@link #getDilayaniTanggal()} semuanya menulis balik ke
 *   field. Dua yang terakhir bahkan menulis balik <i>nilai turunan</i> ke kolom yang dipetakan.</li>
 *   <li><b>Field audit bayangan</b> — {@link #oleh}, {@link #olehId}, {@link #tanggal_dirubah}
 *   beserta hook {@link #onUpdate()} adalah keharusan teknis pola AIS, bukan duplikasi.</li>
 * </ul>
 *
 * <p>Dipetakan ke skema {@code sirs} tabel {@code booking_registrasi} dengan {@code dynamicInsert}
 * dan {@code dynamicUpdate}, serta {@link org.hibernate.envers.Audited} sehingga perubahan booking
 * terekam Envers.</p>
 *
 * @see Pendaftaran
 * @see JadwalDokter
 * @see Pasien
 * @see KunjunganDokter
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "booking_registrasi")
public class BookingRegistrasi extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java untuk entity ini; nilainya kebetulan sama dengan
	 * {@link Dokter} dan {@link KunjunganDokter} karena berasal dari cetakan hbm2java yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama teknis (auto-increment kolom {@code id}). Lihat {@link #getId()}. */
	private Long id;

	/**
	 * ID pengguna terakhir yang mengubah booking ini — bagian dari trio field audit bayangan.
	 * Lihat {@link #getOlehId()}.
	 */
	private String olehId;

	/**
	 * @return ID pengguna yang tercatat sebagai pengubah terakhir booking ini, atau {@code null}.
	 * @see #setOlehId(String)
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna pengubah terakhir, <b>mengabaikan nilai kosong</b>.
	 * <p>{@code null} dan string berisi spasi saja diabaikan agar jejak pengubah terakhir tidak
	 * terhapus oleh pemanggil tanpa konteks pengguna login.</p>
	 *
	 * @param olehId ID pengguna pengubah; {@code null}/kosong diabaikan.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/** Nama pengguna terakhir yang mengubah booking ini. Lihat {@link #getOleh()}. */
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
	 * Hibernate menerbitkan UPDATE untuk baris booking ini.
	 *
	 * <p>Hanya {@code @PreUpdate}, jadi pada penyimpanan pertama nilai {@link #tanggal_dirubah}
	 * berasal dari inisialisasi {@code new Date()} pada deklarasi field.</p>
	 *
	 * <p><b>Catatan format:</b> deklarasi method ini dan field {@code tanggal_dirubah} sengaja
	 * berada pada satu baris fisik — bentuk yang dihasilkan alat penyapu lintas ratusan entity.
	 * Jangan memisahkannya tanpa alasan.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = new Date();

	/**
	 * Menyetel waktu perubahan terakhir booking ini; pada alur normal ditimpa otomatis oleh
	 * {@link #onUpdate()} saat flush.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return waktu perubahan terakhir baris booking ({@link TemporalType#TIMESTAMP}). Ini
	 *         metadata audit — jangan dikacaukan dengan
	 *         {@link #getTanggalBookingRegistrasi()} (kapan booking dibuat) maupun
	 *         {@link #getDilayaniTanggal()} (kapan pasien akan dilayani).
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nomor urut booking per lokasi. Lihat {@link #getIndex()}. */
	private Long index;

	/** Kode unik booking (format {@code REG-BOOK-...}). Lihat {@link #getKode()}. */
	private String kode;

	/** Pasien yang membuat janji temu. Lihat {@link #getPasien()}. */
	private Pasien pasien;

	/** Kapan booking ini dibuat, diinisialisasi {@code new Date()}. Lihat {@link #getTanggalBookingRegistrasi()}. */
	private Date tanggalBookingRegistrasi = new Date();

	/**
	 * Tanggal yang diminta pasien — dalam praktiknya selalu tertimpa nilai turunan; lihat
	 * peringatan pada {@link #getBookingUntukTanggal()}.
	 */
	private Date bookingUntukTanggal;

	/** Poliklinik tujuan. Lihat {@link #getPoly()}. */
	private Poly poly;

	/** Sub-poliklinik tujuan. Lihat {@link #getSubpoly()}. */
	private Poly subpoly;

	/** Tenaga medis yang diminta. Lihat {@link #getDokter()}. */
	private Dokter dokter;

	/**
	 * Penanda booking berisi paket tindakan — nilai turunan yang selalu dihitung ulang; lihat
	 * {@link #getMerupakanPaket()}.
	 */
	private Boolean merupakanPaket = false;

	/** Catatan bebas. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Penanda pasien baru. Lihat {@link #getBaru()}. */
	private Boolean baru;

	/** Tanggal pelayanan hasil perhitungan dari hari jadwal. Lihat {@link #getDilayaniTanggal()}. */
	private Date dilayaniTanggal;

	/** Jadwal praktek yang dituju booking ini. Lihat {@link #getJadwalDokter()}. */
	private JadwalDokter jadwalDokter;

	/** Nomor antrian pada jadwal tersebut. Lihat {@link #getNomorAntrian()}. */
	private Integer nomorAntrian;

	/**
	 * Paket tindakan yang dipesan bersama booking ini, lewat tabel penghubung
	 * {@code sirs.booking_registrasi_has_paket}. Lihat {@link #getPakets()}.
	 */
	private Set<Tindakan> pakets = new HashSet<Tindakan>();

	/**
	 * Mengembalikan daftar paket tindakan yang dipesan bersama booking ini.
	 *
	 * <p>Dipetakan {@code @ManyToMany} ke {@link Tindakan} lewat tabel penghubung
	 * {@code sirs.booking_registrasi_has_paket} (kolom {@code booking_registrasi} &rarr;
	 * {@code paket}), diurutkan menaik berdasarkan {@code nama} pada saat pemuatan.</p>
	 *
	 * <p>Isi koleksi ini <b>ikut diwariskan ke pendaftaran</b> saat booking ditebus:
	 * {@code PendaftaranRawatJalanAction} memanggil
	 * {@code pendaftaran.setPakets(myBookingRegistrasi.getPakets())} sehingga paket yang dipesan
	 * saat booking otomatis menjadi paket pendaftaran. Perhatikan bahwa yang disalin adalah
	 * <b>referensi koleksinya</b>, bukan salinan isinya — setelah pemanggilan itu, booking dan
	 * pendaftaran untuk sementara berbagi satu object {@link Set} yang sama, sehingga perubahan
	 * pada salah satu akan terlihat di keduanya sampai salah satunya disetel ulang. Pada alur
	 * simpan {@code PendaftaranRawatJalanAction} hal ini dinetralkan karena pendaftaran menyetel
	 * ulang koleksinya dengan {@code new HashSet<Tindakan>()} sebelum menambahkan isi final.</p>
	 *
	 * <p>Getter ini juga menjadi dasar {@link #getMerupakanPaket()}, meskipun method tersebut
	 * membaca field {@link #pakets} secara langsung — lihat catatan di sana.</p>
	 *
	 * <p>Anotasi {@code @OrderBy("nama asc")} hanya mempengaruhi klausa ORDER BY saat pemuatan;
	 * karena tipenya {@link HashSet}, urutan iterasi di Java tetap tidak terjamin.</p>
	 *
	 * @return koleksi paket tindakan; tidak pernah {@code null}.
	 * @see Tindakan
	 */
	@ManyToMany(targetEntity = Tindakan.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@OrderBy(value = "nama asc")
	@JoinTable(name = "booking_registrasi_has_paket", schema = "sirs", joinColumns = @JoinColumn(name = "booking_registrasi"), inverseJoinColumns = @JoinColumn(name = "paket"))
	public Set<Tindakan> getPakets() {
		return pakets;
	}

	/**
	 * Mengganti seluruh daftar paket tindakan booking ini.
	 * <p>Mengganti referensi koleksi, bukan menggabungkan isinya. Menyetel koleksi kosong lalu
	 * mem-flush akan menghapus seluruh baris {@code sirs.booking_registrasi_has_paket} untuk
	 * booking ini. Karena {@link #getMerupakanPaket()} diturunkan dari isi koleksi ini, memanggil
	 * setter ini juga secara tidak langsung mengubah nilai penanda paket pada pembacaan
	 * berikutnya.</p>
	 *
	 * @param pakets koleksi paket tindakan yang baru.
	 */
	public void setPakets(Set<Tindakan> pakets) {
		this.pakets = pakets;
	}

	/**
	 * Cuplikan keanggotaan komunitas pasien pada saat booking dibuat; diisi oleh
	 * {@link #setPasienKomunitas(Pasien)}. Lihat {@link #getKomunitass()}.
	 */
	private Set<Komunitas> komunitass = new HashSet<Komunitas>();

	/** Penjamin/asuransi yang dipakai. Lihat {@link #getAsuransi()}. */
	private Asuransi asuransi;

	/** Kelas perawatan yang diminta. Lihat {@link #getKelasPerawatan()}. */
	private KelasPerawatan kelasPerawatan;

	/** Unit/cabang tempat booking dibuat. Lihat {@link #getLokasi()}. */
	private Lokasi lokasi;

	/** Shift pelayanan. Lihat {@link #getShift()}. */
	private Shift shift;

	/**
	 * Pendaftaran hasil penebusan booking ini; {@code null} berarti booking belum ditebus.
	 * Lihat {@link #getPendaftaran()}.
	 */
	private Pendaftaran pendaftaran;

	/**
	 * Mengembalikan cuplikan (<i>snapshot</i>) komunitas yang diikuti pasien pada saat booking
	 * dibuat, lewat tabel penghubung {@code sirs.booking_registrasi_has_komunitas}.
	 *
	 * <p><b>Ini data historis, bukan cerminan keanggotaan komunitas pasien saat ini.</b> Isinya
	 * ditetapkan sekali oleh {@link #setPasienKomunitas(Pasien)} berdasarkan keanggotaan yang
	 * berlaku pada {@link #getTanggalBookingRegistrasi()}, lalu dibekukan di tabel penghubung.
	 * Bila pasien kemudian keluar dari sebuah komunitas atau bergabung dengan komunitas baru,
	 * booking lama <b>tetap</b> membawa daftar lamanya — dan memang itu yang diinginkan, karena
	 * komunitas menentukan potongan/tarif yang berlaku pada transaksi tersebut.</p>
	 *
	 * <p>Untuk mengetahui keanggotaan komunitas pasien yang berlaku sekarang, jangan membaca
	 * koleksi ini; kueri {@code KomunitasPunyaPasien} secara langsung.</p>
	 *
	 * <p>Anotasi {@code @OrderBy("nama asc")} hanya mempengaruhi ORDER BY saat pemuatan; urutan
	 * iterasi {@link HashSet} tetap tidak terjamin.</p>
	 *
	 * @return koleksi komunitas pasien pada saat booking; tidak pernah {@code null}.
	 * @see #setPasienKomunitas(Pasien)
	 * @see Komunitas
	 */
	@ManyToMany(targetEntity = Komunitas.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@OrderBy(value = "nama asc")
	@JoinTable(name = "booking_registrasi_has_komunitas", schema = "sirs", joinColumns = @JoinColumn(name = "booking_registrasi"), inverseJoinColumns = @JoinColumn(name = "komunitas"))
	public Set<Komunitas> getKomunitass() {
		return komunitass;
	}

	/**
	 * Mengganti seluruh cuplikan komunitas booking ini.
	 * <p>Umumnya <b>tidak dipanggil langsung</b> oleh kode layar — pengisian yang benar dilakukan
	 * lewat {@link #setPasienKomunitas(Pasien)}, yang memanggil setter ini lebih dulu dengan
	 * {@link HashSet} kosong untuk membersihkan cuplikan lama sebelum mengisinya kembali dari hasil
	 * query. Menyetel koleksi kosong lalu mem-flush akan menghapus seluruh baris
	 * {@code sirs.booking_registrasi_has_komunitas} untuk booking ini, yang berarti kehilangan
	 * data historis potongan/tarif.</p>
	 *
	 * @param komunitass koleksi komunitas yang baru.
	 */
	public void setKomunitass(Set<Komunitas> komunitass) {
		this.komunitass = komunitass;
	}

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate, sekaligus dipakai
	 * {@code BookingRegistrasiAction} saat petugas membuat booking baru.
	 *
	 * <p>Field yang sudah terisi sejak awal: {@link #tanggalBookingRegistrasi} =
	 * {@code new Date()}, {@link #merupakanPaket} = {@code false}, {@link #tanggal_dirubah} =
	 * {@code new Date()}, serta koleksi {@link #pakets} dan {@link #komunitass} berupa
	 * {@link HashSet} kosong. Sisanya {@code null} — termasuk {@link #kode} dan {@link #pasien}
	 * yang kolomnya {@code nullable = false} sehingga wajib diisi sebelum penyimpanan.</p>
	 */
	public BookingRegistrasi() {
	}

	/**
	 * @return kunci utama teknis booking ini, atau {@code null} bila belum tersimpan. Dihasilkan
	 *         basis data ({@link javax.persistence.GenerationType#IDENTITY}) dan dipetakan
	 *         {@code insertable = false} sehingga tidak pernah ikut pada INSERT.
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
	 * Menyetel kode booking.
	 * <p>Kolomnya {@code unique = true, nullable = false}. Pelanggaran keunikan baru terdeteksi
	 * saat flush, bukan saat setter dipanggil.</p>
	 *
	 * @param kode kode unik booking.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * @return kode unik booking (kolom {@code kode}, {@code unique = true},
	 *         {@code nullable = false}).
	 *         <p>{@code BookingRegistrasiAction} membangkitkannya lewat
	 *         {@code Common.generateCode(BookingRegistrasi.class, 10, "REG-BOOK", lokasi)},
	 *         sehingga kode berawalan {@code REG-BOOK} dan <b>tersegmentasi per lokasi</b>. Ada
	 *         pula satu jalur yang memakai {@code Common.generateCode(BookingRegistrasi.class, 8)}
	 *         tanpa awalan dan tanpa lokasi sebagai nilai awal pada form; nilai itu ditimpa oleh
	 *         varian berlokasi pada saat simpan.</p>
	 */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return kode;
	}

	/**
	 * @return catatan bebas mengenai booking ini (kolom {@code keterangan}), atau {@code null}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel catatan bebas mengenai booking ini.
	 *
	 * @param keterangan catatan bebas; boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menyetel tanggal pembuatan booking.
	 * <p><b>Urutan pemanggilan penting.</b> Nilai ini menjadi acuan dua perhitungan turunan:
	 * cuplikan komunitas pada {@link #setPasienKomunitas(Pasien)} dan tanggal pelayanan pada
	 * {@link #getDilayaniTanggal()}. Menyetelnya <i>setelah</i> kedua perhitungan itu berjalan
	 * tidak akan memperbarui hasilnya, karena keduanya menyimpan hasil dan tidak menghitung
	 * ulang.</p>
	 *
	 * @param tanggalBookingRegistrasi tanggal pembuatan booking.
	 */
	public void setTanggalBookingRegistrasi(Date tanggalBookingRegistrasi) {
		this.tanggalBookingRegistrasi = tanggalBookingRegistrasi;
	}

	/**
	 * Mengembalikan tanggal pembuatan booking, dengan <b>menulis balik</b> {@code new Date()} bila
	 * masih kosong.
	 *
	 * <p>Ini getter destruktif ringan: bila field bernilai {@code null}, waktu saat ini ditugaskan
	 * ke field sebelum dikembalikan. Karena Hibernate mengakses entity ini lewat properti,
	 * pembacaan saat flush juga melewati method ini — sehingga baris lama yang kolom tanggalnya
	 * {@code NULL} akan terisi waktu <i>pembacaan</i>, bukan waktu pembuatan sesungguhnya. Nilai
	 * yang dihasilkan dengan cara itu menyesatkan; perlakukan sebagai perkiraan, bukan fakta.</p>
	 *
	 * <p>Perhatikan bahwa getter ini tidak beranotasi {@link Temporal @Temporal}, berbeda dari
	 * {@link #getBookingUntukTanggal()} dan {@link #getDilayaniTanggal()} yang dipetakan
	 * {@link TemporalType#DATE}. Tanpa {@code @Temporal} eksplisit, pemetaannya mengikuti default
	 * penyedia JPA untuk properti {@link Date} — bukan hanya tanggal — sehingga <b>komponen jam
	 * ikut tersimpan</b>. Ini relevan ketika membandingkan nilai ini dengan kolom bertipe DATE.</p>
	 *
	 * @return tanggal (dan jam) pembuatan booking; tidak pernah {@code null}.
	 */
	public Date getTanggalBookingRegistrasi() {
		if (tanggalBookingRegistrasi == null) {
			tanggalBookingRegistrasi = new Date();
		}
		return tanggalBookingRegistrasi;
	}

	/**
	 * Menyetel pasien booking ini <b>sekaligus mengambil cuplikan keanggotaan komunitasnya dari
	 * basis data</b>.
	 *
	 * <h3>Mengapa ada setter yang menjalankan query</h3>
	 * <p>Ini bukan setter biasa. Selain menugaskan {@code pasien} ke field, method ini membuka
	 * {@link org.hibernate.Session} thread-local lewat
	 * {@code HibernateUtil.currentSession()} dan menjalankan sebuah criteria atas
	 * {@code KomunitasPunyaPasien} untuk menentukan komunitas mana saja yang diikuti pasien
	 * tersebut <i>pada saat booking dibuat</i>. Hasilnya dibekukan ke koleksi
	 * {@link #getKomunitass()} sehingga ikut tersimpan ke tabel penghubung
	 * {@code sirs.booking_registrasi_has_komunitas}. Alasan desainnya: komunitas menentukan
	 * potongan atau tarif khusus, dan tarif itu harus mengikuti kondisi saat transaksi terjadi —
	 * bukan berubah surut ketika pasien kelak keluar-masuk komunitas.</p>
	 *
	 * <h3>Kriteria penyaringan komunitas</h3>
	 * <p>Query menggabungkan alias {@code komunitas} lalu menerapkan empat pembatasan sekaligus:</p>
	 * <ul>
	 *   <li>{@code komunitas.mulai <= tanggalBookingRegistrasi} — komunitas sudah mulai berlaku;</li>
	 *   <li>{@code komunitas.sampai IS NULL OR komunitas.sampai >= tanggalBookingRegistrasi} —
	 *   komunitas belum berakhir, dengan {@code null} diperlakukan sebagai "tanpa batas akhir";</li>
	 *   <li>{@code pasien = <pasien>} — hanya keanggotaan milik pasien tersebut;</li>
	 *   <li>{@code komunitas.aktif = true} — komunitas yang dinonaktifkan diabaikan.</li>
	 * </ul>
	 * <p>Proyeksi {@code Projections.groupProperty("komunitas")} membuat hasilnya berupa daftar
	 * {@link Komunitas} yang sudah unik, sehingga pasien yang punya beberapa baris keanggotaan
	 * pada komunitas yang sama tidak menghasilkan duplikat. Perlu dicatat bahwa penyaringan
	 * rentang tanggal dilakukan pada properti <b>komunitas</b> ({@code komunitas.mulai} /
	 * {@code komunitas.sampai} — yaitu masa berlaku komunitasnya), bukan pada masa keanggotaan
	 * pasien di komunitas tersebut. Jadi seorang pasien yang sudah lama keluar dari sebuah
	 * komunitas tetap ikut terjaring selama komunitas itu sendiri masih berlaku dan aktif.</p>
	 *
	 * <h3>Urutan pemanggilan dan efek samping</h3>
	 * <p>Beberapa hal yang wajib diketahui sebelum memanggil method ini.</p>
	 * <ol>
	 *   <li><b>Cuplikan lama selalu dihapus lebih dulu.</b> Baris pertama method ini adalah
	 *   {@code setKomunitass(new HashSet<Komunitas>())}. Artinya memanggilnya dengan
	 *   {@code pasien} bernilai {@code null} — atau ketika query tidak menemukan apa pun —
	 *   <b>mengosongkan</b> daftar komunitas booking ini, dan pengosongan itu akan tersimpan
	 *   sebagai penghapusan baris tabel penghubung pada flush berikutnya.</li>
	 *   <li><b>Acuan tanggalnya dibaca saat itu juga.</b> Method ini memanggil
	 *   {@link #getTanggalBookingRegistrasi()}; bila tanggal booking belum disetel, getter itu
	 *   mengisinya dengan waktu sekarang. Karena itu setel tanggal booking <i>sebelum</i>
	 *   memanggil method ini bila booking dibuat untuk tanggal selain hari ini.</li>
	 *   <li><b>Membutuhkan session Hibernate yang aktif.</b> Karena bergantung pada
	 *   {@code HibernateUtil.currentSession()}, method ini tidak dapat dipanggil dari konteks tanpa
	 *   session (thread latar, proses deserialisasi, atau unit test tanpa fixture basis data) —
	 *   berbeda dari setter lain di kelas ini yang murni menugaskan nilai.</li>
	 *   <li><b>Menembus setter pasangannya.</b> Penugasan terakhir dilakukan langsung ke field
	 *   ({@code this.pasien = pasien}), bukan lewat {@link #setPasien(Pasien)}. Untuk saat ini
	 *   keduanya setara, tetapi bila kelak {@code setPasien} diberi logika tambahan, logika itu
	 *   akan terlewat di jalur ini.</li>
	 *   <li><b>Ada method kembar di {@link Pendaftaran}.</b>
	 *   {@link Pendaftaran#setPasienKomunitas(Pasien)} berisi query yang identik, hanya berbeda
	 *   acuan tanggalnya ({@code tanggalPendaftaran}). Perubahan pada kriteria di sini hampir
	 *   selalu harus dicerminkan di sana agar cuplikan booking dan cuplikan pendaftaran tetap
	 *   konsisten. Perlu diperhatikan bahwa saat booking ditebus menjadi pendaftaran, layar
	 *   pendaftaran memanggil versi {@link Pendaftaran} — sehingga komunitas dihitung ulang pada
	 *   tanggal pendaftaran dan hasilnya <b>dapat berbeda</b> dari cuplikan yang tersimpan di
	 *   booking.</li>
	 * </ol>
	 *
	 * <p>Anotasi {@code @SuppressWarnings("unchecked")} diperlukan karena
	 * {@code Criteria.list()} pada API Hibernate 3 mengembalikan {@link List} mentah.</p>
	 *
	 * @param pasien pasien pemilik booking; {@code null} akan mengosongkan cuplikan komunitas dan
	 *               melewati query.
	 * @see #getKomunitass()
	 * @see Pendaftaran#setPasienKomunitas(Pasien)
	 */
	@SuppressWarnings("unchecked")
	public void setPasienKomunitas(Pasien pasien) {
		setKomunitass(new HashSet<Komunitas>());
		if (pasien != null && getTanggalBookingRegistrasi() != null) {
			List<Komunitas> komunitas = HibernateUtil.currentSession().createCriteria(KomunitasPunyaPasien.class)
					.createAlias("komunitas", "komunitas")
					.add(Restrictions.le("komunitas.mulai", getTanggalBookingRegistrasi()))
					.add(Restrictions.or(Restrictions.isNull("komunitas.sampai"),
							Restrictions.ge("komunitas.sampai", getTanggalBookingRegistrasi())))
					.add(Restrictions.eq("pasien", pasien)).add(Restrictions.eq("komunitas.aktif", true))
					.setProjection(Projections.groupProperty("komunitas")).list();
			getKomunitass().addAll(komunitas);
		}

		this.pasien = pasien;
	}

	/**
	 * Menyetel pasien pemilik booking <b>tanpa</b> memperbarui cuplikan komunitas.
	 * <p>Gunakan {@link #setPasienKomunitas(Pasien)} bila cuplikan komunitas juga perlu diambil
	 * ulang — itulah yang dilakukan {@code BookingRegistrasiAction} pada saat simpan. Setter ini
	 * dipakai untuk penugasan murni, misalnya saat memuat ulang form.</p>
	 *
	 * @param pasien pasien pemilik booking.
	 */
	public void setPasien(Pasien pasien) {
		this.pasien = pasien;
	}

	/**
	 * Mengembalikan pasien pemilik booking, setelah meresolusi proxy lazy-nya lewat
	 * {@code check(...)} dan menulis hasilnya kembali ke field.
	 *
	 * <p>Kolomnya {@code pasien}, {@code nullable = false} — setiap booking wajib menyebut pasien.
	 * Saat booking ditebus, nilai inilah yang disalin ke form pendaftaran melalui
	 * {@code perubahanPasienListener} pada {@code PendaftaranRawatJalanAction}, sehingga pasien
	 * pendaftaran otomatis mengikuti pasien booking.</p>
	 *
	 * @return pasien pemilik booking.
	 * @see Pasien
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pasien", nullable = false)
	public Pasien getPasien() {
		pasien = check(pasien);
		return pasien;
	}

	/**
	 * Menyetel poliklinik tujuan booking.
	 *
	 * @param poly poliklinik tujuan; boleh {@code null}.
	 */
	public void setPoly(Poly poly) {
		this.poly = poly;
	}

	/**
	 * Mengembalikan poliklinik tujuan booking, setelah meresolusi proxy lazy-nya lewat
	 * {@code check(...)}.
	 *
	 * <p>Kolomnya {@code poly}, {@code nullable = true}. Pada alur pemilihan jadwal, nilai ini
	 * diturunkan dari {@link JadwalDokter#getPoly()} milik jadwal yang dipilih — bukan diisi
	 * bebas — sehingga umumnya konsisten dengan jadwalnya. Tidak ada penjaga yang memaksa
	 * konsistensi itu bila poli disetel lewat jalur lain.</p>
	 *
	 * @return poliklinik tujuan, atau {@code null}.
	 * @see Poly
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "poly", nullable = true)
	public Poly getPoly() {
		poly = check(poly);
		return poly;
	}

	/**
	 * Menyetel tenaga medis yang diminta pada booking ini.
	 *
	 * @param dokter tenaga medis yang diminta; boleh {@code null}.
	 */
	public void setDokter(Dokter dokter) {
		this.dokter = dokter;
	}

	/**
	 * Mengembalikan tenaga medis yang diminta pada booking ini, setelah meresolusi proxy lazy-nya
	 * lewat {@code check(...)}.
	 *
	 * <p>Kolomnya {@code dokter}, {@code nullable = true}. Sama seperti {@link #getPoly()}, pada
	 * alur normal nilai ini diturunkan dari {@link JadwalDokter#getDokter()} milik jadwal terpilih.
	 * Karena {@link JadwalDokter} sendiri juga menyimpan dokternya, ada <b>duplikasi data yang
	 * disengaja</b> di sini: field ini menjadi cuplikan yang tetap terbaca meskipun jadwal kelak
	 * diubah atau dihapus. Konsekuensinya, kedua nilai dapat berbeda — bila keduanya perlu
	 * dibandingkan, perlakukan {@code jadwalDokter.getDokter()} sebagai kondisi terkini dan field
	 * ini sebagai kondisi saat booking dibuat.</p>
	 *
	 * @return tenaga medis yang diminta, atau {@code null}.
	 * @see Dokter
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dokter", nullable = true)
	public Dokter getDokter() {
		dokter = check(dokter);
		return dokter;
	}

	/**
	 * Mengembalikan penjamin/asuransi yang akan dipakai pasien, setelah meresolusi proxy lazy-nya
	 * lewat {@code check(...)}.
	 *
	 * <p>Kolomnya {@code asuransi}, {@code nullable = true} — booking tanpa penjamin berarti
	 * pasien umum/bayar sendiri. Nilai ini bersifat rencana; penjamin yang benar-benar dipakai
	 * ditentukan ulang pada {@link Pendaftaran#getAsuransi()} saat registrasi resmi, dan
	 * <b>tidak</b> disalin otomatis dari booking oleh alur penebusan.</p>
	 *
	 * @return penjamin/asuransi, atau {@code null}.
	 * @see Asuransi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "asuransi", nullable = true)
	public Asuransi getAsuransi() {
		asuransi = check(asuransi);
		return asuransi;
	}

	/**
	 * Menyetel penjamin/asuransi booking.
	 *
	 * @param asuransi penjamin; {@code null} berarti pasien umum.
	 */
	public void setAsuransi(Asuransi asuransi) {
		this.asuransi = asuransi;
	}

	/**
	 * Menyetel kelas perawatan yang diminta.
	 * <p>Menyetel {@code null} tidak akan bertahan: pembacaan berikutnya lewat
	 * {@link #getKelasPerawatan()} akan menggantinya dengan {@code ConstantValues.kelasNormal}.</p>
	 *
	 * @param kelasPerawatan kelas perawatan yang diminta.
	 */
	public void setKelasPerawatan(KelasPerawatan kelasPerawatan) {
		this.kelasPerawatan = kelasPerawatan;
	}

	/**
	 * Mengembalikan kelas perawatan yang diminta, dengan <b>mengisi default dan menulis balik</b>
	 * ke field.
	 *
	 * <h3>Dua penulisan balik dalam satu getter</h3>
	 * <p>Method ini melakukan dua penugasan berurutan ke field {@link #kelasPerawatan}:</p>
	 * <ol>
	 *   <li>bila masih {@code null}, diisi {@code ConstantValues.kelasNormal} — kelas perawatan
	 *   default seluruh aplikasi;</li>
	 *   <li>hasilnya dilewatkan {@code check(...)} untuk meresolusi proxy lazy, lalu ditugaskan
	 *   kembali ke field. Baris kedua ini <b>ditambahkan belakangan</b> oleh penyapuan lintas
	 *   berkas (revisi r84369) untuk menyeragamkan pola resolusi proxy di seluruh getter relasi.</li>
	 * </ol>
	 *
	 * <p>Karena Hibernate mengakses entity ini lewat properti, penulisan balik nomor 1 juga terjadi
	 * saat flush: baris booking lama yang kolom {@code kelas_perawatan}-nya {@code NULL} akan
	 * ter-UPDATE menjadi kelas normal pada flush pertama setelah dibaca, tanpa perintah pengguna,
	 * dan menghasilkan satu baris revisi Envers. Ini pola getter destruktif yang berulang di model
	 * AIS — perlakukan pembacaan entity ini sebagai operasi yang dapat mengubah data.</p>
	 *
	 * <p><b>Waspadai {@code ConstantValues.kelasNormal} yang dapat bernilai {@code null}.</b>
	 * Konstanta itu adalah cache yang diisi saat inisialisasi aplikasi; bila belum terisi (mis.
	 * pada konteks yang berjalan sebelum inisialisasi selesai), method ini akan mengembalikan
	 * {@code null} — jadi jangan menganggap nilai kembaliannya dijamin tidak {@code null} hanya
	 * karena ada cabang default. Untuk kebutuhan yang hanya memerlukan ID, {@code ConstantValues}
	 * menyediakan pembungkus aman {@code kelasNormalId()}.</p>
	 *
	 * <p>Perlu dicatat bahwa penulisan balik ini juga membuat booking <b>kehilangan informasi
	 * "kelas perawatan tidak ditentukan"</b>: setelah pembacaan pertama, tidak ada lagi cara
	 * membedakan pasien yang memang memilih kelas normal dari pasien yang belum memilih apa pun.</p>
	 *
	 * @return kelas perawatan yang diminta; biasanya tidak {@code null}, tetapi lihat peringatan
	 *         di atas.
	 * @see KelasPerawatan
	 * @see Pendaftaran#getKelasPerawatan()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelas_perawatan", nullable = true)
	public KelasPerawatan getKelasPerawatan() {
		if (kelasPerawatan == null) {
			kelasPerawatan = ConstantValues.kelasNormal;
		}
		kelasPerawatan = check(kelasPerawatan);
		return kelasPerawatan;
	}

	/**
	 * Menyetel unit/cabang tempat booking dibuat.
	 *
	 * @param lokasi unit/cabang; boleh {@code null} pada tingkat pemetaan.
	 */
	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * Mengembalikan unit/cabang tempat booking dibuat, setelah meresolusi proxy lazy-nya lewat
	 * {@code check(...)}.
	 *
	 * <p>Lokasi berperan sebagai <b>pembatas tenant</b> pada modul SIRS: ia menjadi segmen pada
	 * pembangkitan {@link #getKode()} ({@code REG-BOOK} per lokasi) dan pada penomoran
	 * {@link #getIndex()} ({@code Common.generateMaxByLokasi(BookingRegistrasi.class, lokasi)}).
	 * Kolomnya sendiri dipetakan {@code nullable = true}, sehingga booking tanpa lokasi secara
	 * teknis dapat tersimpan — dan booking seperti itu akan lolos dari penyaringan berbasis lokasi
	 * di layar mana pun yang memakainya. Kode baru yang mengandalkan lokasi untuk pembatasan akses
	 * harus menangani kemungkinan {@code null} secara eksplisit (gagal-tertutup), bukan
	 * mengabaikannya.</p>
	 *
	 * @return unit/cabang tempat booking dibuat, atau {@code null}.
	 * @see ais.database.model.asset.Lokasi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi", nullable = true)
	public Lokasi getLokasi() {
		lokasi = check(lokasi);
		return lokasi;
	}

	/**
	 * Menyetel shift pelayanan booking.
	 *
	 * @param shift shift pelayanan; boleh {@code null}.
	 */
	public void setShift(Shift shift) {
		this.shift = shift;
	}

	/**
	 * Mengembalikan shift pelayanan booking, setelah meresolusi proxy lazy-nya lewat
	 * {@code check(...)}.
	 *
	 * <p>Kolomnya {@code shift}, {@code nullable = true}. Seperti {@link #getDokter()} dan
	 * {@link #getPoly()}, ini cuplikan dari {@link JadwalDokter#getShift()} milik jadwal terpilih
	 * dan dapat berbeda dari nilai jadwal terkini bila jadwalnya kemudian diubah.</p>
	 *
	 * @return shift pelayanan, atau {@code null}.
	 * @see Shift
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "shift", nullable = true)
	public Shift getShift() {
		shift = check(shift);
		return shift;
	}

	/**
	 * Menyetel sub-poliklinik tujuan booking.
	 *
	 * @param subpoly sub-poliklinik; boleh {@code null}.
	 */
	public void setSubpoly(Poly subpoly) {
		this.subpoly = subpoly;
	}

	/**
	 * Mengembalikan sub-poliklinik tujuan booking, setelah meresolusi proxy lazy-nya lewat
	 * {@code check(...)}.
	 *
	 * <p>Perhatikan bahwa sub-poli dipetakan ke entity yang <b>sama</b> dengan {@link #getPoly()},
	 * yaitu {@link Poly}, hanya lewat kolom berbeda ({@code subpoly}). Jadi hierarki poli &rarr;
	 * sub-poli tidak diwakili oleh dua tipe berbeda melainkan oleh dua kolom yang menunjuk tabel
	 * yang sama. Tidak ada penjaga yang memastikan bahwa nilai {@code subpoly} benar-benar
	 * merupakan turunan dari nilai {@code poly} — keduanya dapat menunjuk poli yang tidak
	 * berhubungan, atau bahkan poli yang sama.</p>
	 *
	 * @return sub-poliklinik tujuan, atau {@code null}.
	 * @see Poly
	 * @see #getPoly()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "subpoly", nullable = true)
	public Poly getSubpoly() {
		subpoly = check(subpoly);
		return subpoly;
	}

	/**
	 * Mengembalikan penanda apakah booking ini berisi paket tindakan — <b>selalu dihitung ulang
	 * dari isi koleksi dan ditulis balik</b> ke field.
	 *
	 * <p>Nilainya bukan data mandiri melainkan turunan sederhana: {@code !pakets.isEmpty()}.
	 * Konsekuensinya {@link #setMerupakanPaket(Boolean)} praktis tanpa efek — nilai apa pun yang
	 * disetel akan tertimpa pada pembacaan atau flush berikutnya. Karena property ini tidak diberi
	 * {@link javax.persistence.Transient @Transient}, ia <b>tetap dipetakan ke kolom basis
	 * data</b>, sehingga kolom tersebut berisi data turunan yang di-denormalisasi, bukan pilihan
	 * pengguna.</p>
	 *
	 * <p><b>Bahaya {@code LazyInitializationException}.</b> Method ini membaca field
	 * {@link #pakets} <i>secara langsung</i>, bukan lewat {@link #getPakets()}. Karena relasi
	 * {@code @ManyToMany} secara default dimuat malas, field itu berisi koleksi persisten yang
	 * belum terinisialisasi ketika entity dimuat. Memanggil {@code isEmpty()} atasnya pada
	 * instance yang sudah <i>detached</i> dari {@link org.hibernate.Session} akan melempar
	 * {@code LazyInitializationException} — situasi yang lazim di AIS karena entity sering dibawa
	 * lintas request ZK dan cache in-memory. Pola {@code check(...)} yang melindungi getter relasi
	 * lain tidak berlaku untuk koleksi. Bila entity ini perlu dibaca di luar session, pastikan
	 * koleksi paket sudah diinisialisasi lebih dulu.</p>
	 *
	 * <p>Nilai ini ikut disalin ke pendaftaran saat booking ditebus:
	 * {@code merupakanPaket.setChecked(myBookingRegistrasi.getMerupakanPaket())} pada
	 * {@code PendaftaranRawatJalanAction}.</p>
	 *
	 * @return {@code true} bila ada minimal satu paket tindakan pada booking ini.
	 */
	public Boolean getMerupakanPaket() {
		merupakanPaket = !pakets.isEmpty();
		return merupakanPaket;
	}

	/**
	 * Menyetel penanda paket — <b>praktis tanpa efek</b>, karena {@link #getMerupakanPaket()}
	 * selalu menghitung ulang dari isi {@link #getPakets()}. Untuk mengubah nilainya, ubah isi
	 * koleksi paketnya.
	 *
	 * @param merupakanPaket nilai yang akan segera tertimpa.
	 */
	public void setMerupakanPaket(Boolean merupakanPaket) {
		this.merupakanPaket = merupakanPaket;
	}

	/**
	 * Mengembalikan pendaftaran hasil penebusan booking ini, atau {@code null} bila booking belum
	 * ditebus.
	 *
	 * <h3>Field ini adalah penanda "booking sudah dipakai"</h3>
	 * <p>Nilai {@code null} pada relasi inilah yang menandai sebuah booking masih tersedia:</p>
	 * <ul>
	 *   <li>{@code AmbilDataBookingRegistrasiBanbox} — pemilih booking pada layar pendaftaran —
	 *   menyaring dengan {@code Restrictions.isNull("pendaftaran")}, sehingga booking yang sudah
	 *   ditebus tidak lagi ditawarkan;</li>
	 *   <li>{@code BookingRegistrasiAction} menyembunyikan tombol ubah dan hapus ketika
	 *   {@code bookingRegistrasi.getPendaftaran() != null}, sehingga booking yang sudah ditebus
	 *   tidak dapat diubah atau dihapus lewat layar booking.</li>
	 * </ul>
	 *
	 * <p><b>Kekuatan penjaganya terbatas.</b> Kedua perlindungan di atas hidup di lapisan tampilan
	 * saja. Kolomnya tidak dipetakan {@code unique}, dan alur simpan pendaftaran memanggil
	 * {@code session.refresh(bookingRegistrasi)} lalu langsung
	 * {@code bookingRegistrasi.setPendaftaran(pendaftaran)} tanpa memeriksa ulang apakah booking
	 * itu sudah tertaut ke pendaftaran lain. Pada penyimpanan bersamaan oleh dua petugas, kedua
	 * pendaftaran dapat sama-sama menunjuk booking yang sama sementara booking hanya menyimpan
	 * tautan ke yang terakhir menang. Efek lanjutannya menyentuh antrian: karena
	 * {@code CommonPendaftaranUtil.generateNomorAntrian(Pendaftaran, JadwalDokter)} mengembalikan
	 * langsung {@code pendaftaran.getBookingRegistrasi().getNomorAntrian()}, kedua pendaftaran
	 * tersebut akan memperoleh <b>nomor antrian yang identik</b>.</p>
	 *
	 * <h3>Pemetaan</h3>
	 * <p>Relasi tidak menyatakan {@code fetch = LAZY}, sehingga memakai default {@code @ManyToOne}
	 * yaitu {@link FetchType#EAGER}, diperkuat
	 * {@link Fetch @Fetch}{@code (}{@link FetchMode#SELECT}{@code )}. Itu sebabnya getter ini
	 * tidak memerlukan {@code check(...)} seperti getter relasi lain di kelas ini — tetapi juga
	 * berarti setiap pemuatan booking menyeret SELECT tambahan untuk pendaftarannya.</p>
	 *
	 * <p>Relasi ini merupakan pasangan dua arah dari
	 * {@link Pendaftaran#getBookingRegistrasi()}; keduanya diikat bersamaan oleh layar
	 * pendaftaran, dan tidak ada mekanisme di entity yang menjaga kedua sisi tetap sinkron.</p>
	 *
	 * @return pendaftaran hasil penebusan, atau {@code null} bila booking belum ditebus.
	 * @see Pendaftaran#getBookingRegistrasi()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pendaftaran", nullable = true)
	public Pendaftaran getPendaftaran() {
		return pendaftaran;
	}

	/**
	 * Menautkan booking ini ke pendaftaran hasil penebusannya.
	 * <p>Dipanggil oleh {@code PendaftaranRawatJalanAction} dan
	 * {@code PendaftaranRawatInapAction} <i>setelah</i> pendaftaran tersimpan, lalu diikuti
	 * {@code Common.refreshUpdate(session, bookingRegistrasi)}. Setter ini <b>tidak</b> memeriksa
	 * apakah booking sudah tertaut ke pendaftaran lain, dan <b>tidak</b> menyetel sisi
	 * kebalikannya ({@code pendaftaran.setBookingRegistrasi(this)}) — kedua hal itu menjadi
	 * tanggung jawab pemanggil.</p>
	 *
	 * @param pendaftaran pendaftaran hasil penebusan; {@code null} mengembalikan booking ke status
	 *                    tersedia.
	 */
	public void setPendaftaran(Pendaftaran pendaftaran) {
		this.pendaftaran = pendaftaran;
	}

	/**
	 * Mengembalikan penanda pasien baru, dengan <b>menulis balik</b> {@code false} bila masih
	 * kosong.
	 *
	 * <p>Getter destruktif ringan: baris booking lama yang kolomnya {@code NULL} akan ter-UPDATE
	 * menjadi {@code false} pada flush pertama setelah dibaca. Nilainya diisi dari sebuah checkbox
	 * pada {@code BookingRegistrasiAction} dan menandai apakah pasien tersebut baru pertama kali
	 * berkunjung.</p>
	 *
	 * <p>Perhatikan bahwa {@link Pendaftaran#getBaru()} — padanan field ini di sisi pendaftaran —
	 * <b>tidak</b> memiliki cabang default seperti ini dan dapat mengembalikan {@code null}. Kode
	 * yang menangani keduanya secara seragam harus menyadari perbedaan itu.</p>
	 *
	 * @return {@code true} bila pasien ditandai sebagai pasien baru; tidak pernah {@code null}.
	 */
	public Boolean getBaru() {
		if (baru == null) {
			baru = false;
		}
		return baru;
	}

	/**
	 * Menyetel penanda pasien baru.
	 * <p>Menyetel {@code null} tidak bertahan — pembacaan berikutnya lewat {@link #getBaru()} akan
	 * menggantinya dengan {@code false}.</p>
	 *
	 * @param baru {@code true} bila pasien baru pertama kali berkunjung.
	 */
	public void setBaru(Boolean baru) {
		this.baru = baru;
	}

	/**
	 * Mengembalikan tanggal yang "dipesan" pasien — <b>tetapi nilai yang disimpan selalu
	 * ditimpa</b> oleh {@link #getDilayaniTanggal()}.
	 *
	 * <h3>Field ini bukan masukan pengguna</h3>
	 * <p>Meskipun namanya menyiratkan "pasien ingin dilayani pada tanggal X", method ini tidak
	 * pernah mengembalikan nilai yang disetel lewat {@link #setBookingUntukTanggal(Date)}. Seluruh
	 * badan method-nya hanya dua baris: menugaskan hasil {@link #getDilayaniTanggal()} ke field,
	 * lalu mengembalikannya. Akibatnya:</p>
	 * <ul>
	 *   <li><b>{@link #setBookingUntukTanggal(Date)} praktis tanpa efek</b> — nilainya tertimpa
	 *   pada pembacaan atau flush berikutnya;</li>
	 *   <li>kolom {@code booking_untuk_tanggal} di basis data selalu berisi salinan kolom
	 *   {@code dilayani_tanggal}, sehingga <b>kedua kolom itu redundan</b>;</li>
	 *   <li>tanggal yang benar-benar diinginkan pasien tidak tersimpan di mana pun — yang tersimpan
	 *   adalah tanggal hasil perhitungan dari hari jadwal dokter.</li>
	 * </ul>
	 *
	 * <p>Perlu diperhatikan bahwa kolom ini dipetakan {@code @Column(nullable = false)}, sementara
	 * {@link #getDilayaniTanggal()} <b>dapat mengembalikan {@code null}</b> (yaitu ketika booking
	 * belum menunjuk jadwal dokter). Booking tanpa jadwal karena itu akan gagal disimpan dengan
	 * pelanggaran batasan NOT NULL pada saat flush — bukan dengan pesan validasi yang informatif.</p>
	 *
	 * <p>Dipetakan {@link TemporalType#DATE} (tanpa jam) ke kolom {@code booking_untuk_tanggal}
	 * berdasarkan konvensi penamaan properti, karena {@code @Column} di sini tidak menyebut nama
	 * kolom.</p>
	 *
	 * @return tanggal pelayanan hasil perhitungan; selalu sama dengan
	 *         {@link #getDilayaniTanggal()}.
	 */
	@Temporal(TemporalType.DATE)
	@Column(nullable = false)
	public Date getBookingUntukTanggal() {
		bookingUntukTanggal = getDilayaniTanggal();
		return bookingUntukTanggal;
	}

	/**
	 * Menyetel tanggal yang dipesan — <b>praktis tanpa efek</b>, karena
	 * {@link #getBookingUntukTanggal()} selalu menimpanya dengan {@link #getDilayaniTanggal()}.
	 * Untuk mempengaruhi tanggal pelayanan, setel {@link #setDilayaniTanggal(Date)} atau pilih
	 * jadwal dokter yang sesuai.
	 *
	 * @param bookingUntukTanggal nilai yang akan segera tertimpa.
	 */
	public void setBookingUntukTanggal(Date bookingUntukTanggal) {
		this.bookingUntukTanggal = bookingUntukTanggal;
	}

	/**
	 * @return nomor urut booking di dalam lokasinya, atau {@code null} bila belum tersimpan.
	 *         <p>Diisi sekali pada penyimpanan pertama oleh {@code BookingRegistrasiAction} dengan
	 *         {@code Common.generateMaxByLokasi(BookingRegistrasi.class, lokasi) + 1}, yaitu pola
	 *         "nilai maksimum saat ini ditambah satu" yang dihitung <b>per lokasi</b>. Karena
	 *         pembangkitannya berupa baca-lalu-tulis tanpa penguncian dan tanpa batasan unik pada
	 *         kolomnya, dua booking yang disimpan bersamaan pada lokasi yang sama dapat memperoleh
	 *         nomor urut yang sama. Jangan memakai nilai ini sebagai pengenal unik — gunakan
	 *         {@link #getKode()} atau {@link #getId()}.</p>
	 *         <p>Getter ini tidak beranotasi {@link Column @Column}, jadi dipetakan berdasarkan
	 *         konvensi. Perhatikan bahwa {@code index} adalah kata kunci pada beberapa dialek SQL,
	 *         sehingga nama kolomnya biasanya perlu dikutip oleh dialek Hibernate yang dipakai.</p>
	 */
	public Long getIndex() {
		return index;
	}

	/**
	 * Menyetel nomor urut booking di dalam lokasinya.
	 * <p>Pada alur normal hanya dipanggil sekali saat penyimpanan pertama; mengubahnya kemudian
	 * dapat menimbulkan nomor urut kembar karena pembangkitannya berbasis nilai maksimum.</p>
	 *
	 * @param index nomor urut per lokasi.
	 */
	public void setIndex(Long index) {
		this.index = index;
	}

	/**
	 * Mengembalikan jadwal praktek yang dituju booking ini, setelah meresolusi proxy lazy-nya
	 * lewat {@code check(...)}.
	 *
	 * <p><b>Relasi paling menentukan pada entity ini.</b> Dari jadwal inilah diturunkan tanggal
	 * pelayanan ({@link #getDilayaniTanggal()} membaca {@link JadwalDokter#getHari()}) dan nomor
	 * antrian ({@code CommonPendaftaranUtil.generateNomorAntrian(...)} menghitung
	 * {@code max(nomorAntrian) + 1} di antara booking pada jadwal yang sama). Saat booking
	 * ditebus, jadwal ini juga disalin ke {@link Pendaftaran#getJadwalDokter()}.</p>
	 *
	 * <p>Kolomnya {@code jadwal_dokter}, {@code nullable = true} — booking tanpa jadwal secara
	 * teknis mungkin, tetapi booking seperti itu tidak dapat menghitung tanggal pelayanan maupun
	 * nomor antrian, dan akan gagal disimpan karena
	 * {@link #getBookingUntukTanggal()} memetakan kolom {@code NOT NULL} dari nilai yang
	 * {@code null}.</p>
	 *
	 * <p>Ingat pula karakteristik {@link JadwalDokter} sendiri: jadwal kembar untuk kombinasi
	 * dokter+hari+shift+lokasi yang sama tidak dicegah, dan setiap baris jadwal memiliki deret
	 * antriannya masing-masing. Dua booking untuk praktek yang secara nyata sama dapat memperoleh
	 * nomor antrian yang sama bila keduanya menunjuk baris jadwal yang berbeda.</p>
	 *
	 * @return jadwal praktek yang dituju, atau {@code null}.
	 * @see JadwalDokter
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jadwal_dokter", nullable = true)
	public JadwalDokter getJadwalDokter() {
		jadwalDokter = check(jadwalDokter);
		return jadwalDokter;
	}

	/**
	 * Menyetel jadwal praktek yang dituju booking ini.
	 * <p>Menyetelnya <i>setelah</i> {@link #getDilayaniTanggal()} pernah dipanggil tidak akan
	 * mengubah tanggal pelayanan, karena method tersebut menyimpan hasil perhitungannya dan hanya
	 * menghitung ketika tanggal masih {@code null}. Bila jadwal diganti, setel ulang
	 * {@link #setDilayaniTanggal(Date)} ke {@code null} agar perhitungannya diulang.</p>
	 *
	 * @param jadwalDokter jadwal praktek yang dituju.
	 */
	public void setJadwalDokter(JadwalDokter jadwalDokter) {
		this.jadwalDokter = jadwalDokter;
	}

	/**
	 * Mengembalikan tanggal pasien akan dilayani, <b>menghitungnya sekali</b> dari hari pada
	 * jadwal dokter bila belum pernah dihitung.
	 *
	 * <h3>Cara kerja</h3>
	 * <p>Bila {@link #dilayaniTanggal} masih {@code null} dan booking sudah menunjuk jadwal dokter,
	 * method ini mengambil kalender aplikasi ({@code ais.ui.util.WaktuUtil.getCalendar()}),
	 * menempatkannya pada {@link #getTanggalBookingRegistrasi()}, lalu <b>memajukan kalender satu
	 * hari demi satu hari</b> sampai nama hari kalender cocok (tanpa membedakan huruf besar-kecil)
	 * dengan {@link JadwalDokter#getHari()}. Nama hari kalender diambil dari
	 * {@code ais.common.Common.haris} memakai indeks {@code Calendar.DAY_OF_WEEK - 1}. Hasilnya
	 * disimpan ke field sehingga perhitungan tidak diulang.</p>
	 *
	 * <p>Artinya booking selalu jatuh pada <b>kemunculan pertama hari jadwal tersebut pada atau
	 * setelah tanggal booking dibuat</b> — paling jauh enam hari ke depan. Booking untuk tanggal
	 * yang lebih jauh tidak dapat dinyatakan lewat jalur ini; satu-satunya cara adalah menyetel
	 * {@link #setDilayaniTanggal(Date)} secara eksplisit sebelum getter ini pernah dipanggil.
	 * Perlu diperhatikan juga bahwa bila tanggal booking jatuh tepat pada hari yang sama dengan
	 * hari jadwal, perulangan tidak berjalan sama sekali dan tanggal pelayanan menjadi tanggal
	 * booking itu sendiri.</p>
	 *
	 * <h3>Bahaya: perulangan dapat berjalan tanpa henti</h3>
	 * <p>Kondisi berhenti perulangan adalah kecocokan nama hari. Bila
	 * {@link JadwalDokter#getHari()} berisi nilai yang <b>tidak ada</b> pada
	 * {@code Common.haris} — salah ketik, ejaan berbeda, string kosong, atau {@code "Jumat"}
	 * sementara daftar resminya {@code "Jum'at"} dengan tanda petik satu — kondisi itu tidak akan
	 * pernah tercapai dan perulangan berputar selamanya, menahan thread permintaan dan membebani
	 * satu inti prosesor. Bila nilainya {@code null}, yang terjadi adalah
	 * {@link NullPointerException} pada {@code hari.equalsIgnoreCase(currHari)}. Tidak ada
	 * pembatas jumlah iterasi maupun pemeriksaan nilai hari sebelum perulangan dimulai. Method
	 * kembar {@link Pendaftaran#getDilayaniTanggal()} memiliki bahaya yang persis sama.</p>
	 *
	 * <h3>Catatan pemetaan</h3>
	 * <p>Dipetakan {@link TemporalType#DATE} (tanpa jam) ke kolom {@code dilayani_tanggal}
	 * berdasarkan konvensi, dan {@code @Column(nullable = false)} — padahal method ini
	 * <b>dapat</b> mengembalikan {@code null} ketika booking belum menunjuk jadwal dokter,
	 * sehingga penyimpanan booking tanpa jadwal akan gagal pada tingkat basis data. Nilai ini juga
	 * menjadi acuan penyaringan pada pembangkitan nomor antrian di
	 * {@code CommonPendaftaranUtil}.</p>
	 *
	 * @return tanggal pelayanan hasil perhitungan, atau {@code null} bila booking belum menunjuk
	 *         jadwal dokter dan tanggal belum disetel manual.
	 * @see JadwalDokter#getHari()
	 * @see Pendaftaran#getDilayaniTanggal()
	 */
	@Temporal(TemporalType.DATE)
	@Column(nullable = false)
	public Date getDilayaniTanggal() {
		if (dilayaniTanggal == null) {
			if (getJadwalDokter() != null && getTanggalBookingRegistrasi() != null) {
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(getTanggalBookingRegistrasi());
				dilayaniTanggal = calendar.getTime();
				String hari = getJadwalDokter().getHari();
				String currHari = Common.haris[calendar.get(Calendar.DAY_OF_WEEK) - 1];
				while (!hari.equalsIgnoreCase(currHari)) {
					calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
					currHari = Common.haris[calendar.get(Calendar.DAY_OF_WEEK) - 1];
					dilayaniTanggal = calendar.getTime();
				}
			}
		}
		return dilayaniTanggal;
	}

	/**
	 * Menyetel tanggal pelayanan secara eksplisit.
	 * <p>Menyetel nilai bukan-{@code null} <b>mematikan</b> perhitungan otomatis pada
	 * {@link #getDilayaniTanggal()}, karena perhitungan itu hanya berjalan ketika field masih
	 * {@code null}. Ini satu-satunya cara membuat booking untuk tanggal yang lebih jauh dari
	 * kemunculan pertama hari jadwal. Sebaliknya, menyetelnya kembali ke {@code null} akan memicu
	 * perhitungan ulang pada pembacaan berikutnya — berguna setelah
	 * {@link #setJadwalDokter(JadwalDokter)} mengganti jadwal.</p>
	 *
	 * @param dilayaniTanggal tanggal pelayanan; {@code null} mengaktifkan kembali perhitungan
	 *                        otomatis.
	 */
	public void setDilayaniTanggal(Date dilayaniTanggal) {
		this.dilayaniTanggal = dilayaniTanggal;
	}

	/**
	 * @return nomor antrian pasien pada jadwal yang dituju, atau {@code null} bila belum
	 *         dibangkitkan.
	 *
	 *         <p>Dibangkitkan oleh
	 *         {@code CommonPendaftaranUtil.generateNomorAntrian(BookingRegistrasi, JadwalDokter)}
	 *         sebagai {@code max(nomorAntrian) + 1} di antara booking yang menunjuk
	 *         {@link JadwalDokter} yang sama dengan {@code dilayaniTanggal} pada atau setelah hari
	 *         ini. Nomor mulai dari 1 bila belum ada booking sebelumnya, dan nomor yang sudah
	 *         terisi tidak dihitung ulang pada penyimpanan berikutnya.</p>
	 *
	 *         <p><b>Nomor ini diwarisi oleh pendaftaran.</b> Varian
	 *         {@code generateNomorAntrian(Pendaftaran, JadwalDokter)} langsung mengembalikan nilai
	 *         ini bila pendaftaran memiliki booking, sehingga pasien yang sudah membuat janji temu
	 *         mempertahankan nomor antriannya saat datang — dan tidak ikut mengambil nomor dari
	 *         deret pasien yang datang langsung.</p>
	 *
	 *         <p><b>Nomor kembar mungkin terjadi.</b> Pembangkitannya berupa
	 *         baca-maksimum-lalu-tulis tanpa penguncian baris maupun batasan unik pada kolomnya,
	 *         sehingga dua booking yang disimpan bersamaan pada jadwal yang sama dapat memperoleh
	 *         nomor yang sama. Nomor kembar juga dapat muncul dari arah lain: karena
	 *         {@link JadwalDokter} membolehkan jadwal kembar untuk kombinasi dokter+hari+shift yang
	 *         sama, dua baris jadwal yang secara nyata mewakili praktek yang sama akan memiliki dua
	 *         deret antrian terpisah yang keduanya dimulai dari 1.</p>
	 *
	 *         <p>Getter ini tidak beranotasi {@link Column @Column}, jadi dipetakan berdasarkan
	 *         konvensi penamaan properti.</p>
	 */
	public Integer getNomorAntrian() {
		return nomorAntrian;
	}

	/**
	 * Menyetel nomor antrian booking.
	 * <p>Dipanggil oleh {@code BookingRegistrasiAction} dengan hasil
	 * {@code CommonPendaftaranUtil.generateNomorAntrian(...)}. Menyetelnya manual tidak dicegah,
	 * tetapi dapat menimbulkan nomor kembar karena pembangkitan berikutnya berbasis nilai
	 * maksimum.</p>
	 *
	 * @param nomorAntrian nomor antrian pada jadwal yang dituju.
	 */
	public void setNomorAntrian(Integer nomorAntrian) {
		this.nomorAntrian = nomorAntrian;
	}
}
