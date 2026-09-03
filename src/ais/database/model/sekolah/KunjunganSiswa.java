package ais.database.model.sekolah;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
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
import ais.ui.util.WaktuUtil;

/**
 * Satu baris catatan <b>kedatangan (absensi masuk) siswa di anjungan mandiri sekolah</b>, tabel
 * {@code sekolah.kunjungan_siswa}.
 *
 * <h3>Domain sebenarnya — diverifikasi dari kode, bukan dari nama kelas</h3>
 * <p>Nama "KunjunganSiswa" menyesatkan: entity ini <b>bukan</b> catatan kunjungan ke ruang BK/
 * konseling, <b>bukan</b> kunjungan perpustakaan (itu keluarga {@code KunjunganAnggota}), dan
 * <b>bukan</b> buku tamu orang tua/wali. Domainnya dipastikan dari tiga sumber independen:</p>
 * <ol>
 *   <li>Halaman anjungan {@code /WEB-INF/z/x/y/welsis.zul} (dilayani servlet {@code /welsis},
 *       {@code ais.action.servlet.Welsis}) menampilkan gambar pemindai dan label
 *       <i>"Masukkan kode siswa atau scan kartu siswa Anda disini"</i> di atas kotak isian
 *       {@code kodeSiswa}.</li>
 *   <li>Tautan menu di {@code WEB-INF/baru/erp.jsp} menandai modul ini
 *       {@code data-module-id="absen_siswa"} dan {@code home.jsp} menamainya
 *       {@code link_modul_absen_siswa}.</li>
 *   <li>Layanan JSON {@code WEB-INF/baru/modul/welsis/_welsis_service.jsp} memberi judul blok
 *       kodenya sendiri <i>"LOGIKA 1: ABSENSI SCAN SISWA"</i> dan membalas
 *       <i>"Absensi Berhasil, selamat belajar &lt;nama&gt;"</i>.</li>
 * </ol>
 * <p>Jadi: siswa menempelkan kartu / mengetik NIS, NISN, atau ID sidik jari di anjungan; sistem
 * mencatat satu baris di sini <b>dan sekaligus</b> menandai kehadirannya. Mode kedua —
 * "Pengunjung Bukan Siswa" (buku tamu dengan nama + alamat manual) — memang ada di kode tetapi
 * tombol pemicunya dimatikan di kedua lapis, lihat {@link #getNama()}.</p>
 *
 * <h3>Efek samping lintas entity: baris ini memicu absensi</h3>
 * <p>Kedua kanal penulis (lihat di bawah) tidak berhenti pada penyimpanan baris ini. Setelah
 * baris tersimpan mereka juga:</p>
 * <ol>
 *   <li>mencari/membuat {@link AbsenPiket} untuk pasangan (kelas siswa, sekolah, tanggal);</li>
 *   <li>mengambil {@link AbsenPiketDetail} lewat
 *       {@code AbsenPiketDetail.ambil(null, siswa, absenPiket, kelas.getAbsensi(), session)};</li>
 *   <li>meng-{@code populate}-nya dengan status {@code ConstantValues.MASUK}, jam datang diambil
 *       dari baris {@code KunjunganSiswa} <b>pertama</b> hari itu (bukan dari baris yang baru
 *       dibuat), lalu meng-{@code update} detail absensi tersebut.</li>
 * </ol>
 * <p>Konsekuensi praktis: <b>menghapus baris di sini tidak membatalkan absensinya</b> —
 * {@link AbsenPiketDetail} yang sudah berstatus MASUK tetap tinggal.</p>
 *
 * <h3>Granularitas: satu baris per (siswa, sekolah, tanggal, JAM)</h3>
 * <p>Kedua kanal penulis melakukan dedup dengan kunci empat bagian
 * {@code siswa} + {@code sekolah} + {@link #getTgl() tgl} + {@link #getJam() jam}. Pemindaian
 * kedua dalam jam yang sama tidak membuat baris baru (anjungan hanya membalas "absen Anda sudah
 * tercatat pada jam ini"), tetapi pemindaian di jam berikutnya <b>membuat baris baru</b>. Jadi
 * tabel ini bukan satu baris per hari melainkan hingga 24 baris per siswa per hari.</p>
 *
 * <h3>Siapa menulis, siapa membaca</h3>
 * <ul>
 *   <li><b>Tulis (kanal baru, JSP):</b> {@code /welsis?hanya_tampil_jsp=true&amp;p=welsis&amp;
 *       s=_welsis_service&amp;action=scan&amp;kode=...} — pencarian siswa TANPA batasan sekolah.</li>
 *   <li><b>Tulis (kanal lama, ZK):</b> {@link ais.action.master.sekolah.KunjunganSiswaAction}
 *       {@code onKodeSiswa()} pada {@code /welsis?versilama=true} — pencarian siswa dibatasi
 *       sekolah yang dipilih di combobox; plus {@code onSave()} dari dialog buku tamu.</li>
 *   <li><b>Baca:</b> grid ZK ({@code KunjunganSiswaRenderer}), layar master
 *       {@code /pages/master/sekolah/kunjuangan_siswa.zul} (perhatikan salah eja nama berkas),
 *       layanan JSON {@code action=list}, unduhan Excel lewat
 *       {@code Common.appendDownloadButton(...)} dengan kolom {@code id, siswa.nis, siswa.nama,
 *       sekolah.nama, tanggal, tgl, jam, kode, nama, alamat, keterangan}, serta endpoint
 *       reflektif {@code /Data} (aksi {@code daftar}/{@code load}) karena kelas ini turunan
 *       {@link GeneralValueObject} sehingga lolos saringan
 *       {@code DaftarDataService.resolveKelasEntitas}.</li>
 * </ul>
 *
 * <h3>PERINGATAN KEAMANAN — data pribadi siswa terbuka tanpa login</h3>
 * <p>Seluruh kanal di atas berjalan <b>tanpa otentikasi</b>. Ringkasnya:
 * {@code applicationContext-security.xml} memberi {@code /**} akses
 * {@code IS_AUTHENTICATED_ANONYMOUSLY} dan {@code /welsis} tidak masuk satu pun pola
 * {@code IS_AUTHENTICATED_REMEMBERED}; {@code Welsis.process()} tidak memeriksa pengguna;
 * {@code Common.doCheckSecurity()} pada {@code KunjunganSiswaAction.doBeforeCompose} adalah
 * <i>no-op</i> untuk halaman ini karena {@code CommonPrivilages.MUST_CHECKED} hanya memuat 12
 * halaman modul perguruan tinggi; dan {@code KunjunganSiswaAction.initCriteria()} bersifat
 * <b>fail-open</b> (tanpa pengguna login, seluruh baris lintas yayasan/sekolah ditampilkan).
 * Rincian dan dampaknya dicatat pada {@link #getSiswa()}, {@link #getNama()}, dan
 * {@link #getAlamat()}. Isi baris ini mencakup nama, NIS/NISN, kelas, jam datang, dan —
 * lewat {@link #getAlamat()} — alamat rumah anak di bawah umur.</p>
 *
 * <h3>Kolom denormalisasi yang menulis balik saat dibaca</h3>
 * <p>Empat getter menyalin nilai dari {@link #getSiswa()} ke field miliknya sendiri
 * <i>setiap kali dipanggil</i>: {@link #getKode()}, {@link #getNama()}, {@link #getAlamat()},
 * dan {@link #getSekolah()}. Karena Hibernate memakai akses properti, salinan itu ikut terbaca
 * saat pemeriksaan <i>dirty</i>, sehingga membuka layar daftar saja dapat memicu {@code UPDATE}
 * beserta revisi Envers palsu meski pengguna tidak mengubah apa pun. Pola yang sama sudah
 * didokumentasikan pada keluarga {@code ItemBiayaPunya*} dan {@code KelompokParameterTambahan*}.</p>
 *
 * <h3>Hubungan dengan {@link GeneralValueObject}</h3>
 * <p>{@link GeneralValueObject} adalah POJO abstrak biasa — <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass} — sehingga Hibernate tidak memetakan satu pun properti induknya.
 * Karena itu {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} <b>wajib</b>
 * dideklarasikan ulang di sini; pengulangan tersebut bukan bug melainkan keharusan teknis.
 * Konsekuensi lain: properti induk yang tidak diulang selalu bernilai transien, lihat catatan
 * pada {@link #getKode()}.</p>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 *   <li><b>Jejak audit:</b> {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 *   <li><b>Identitas:</b> {@link #getId()}, {@link #toString()}.</li>
 *   <li><b>Relasi:</b> {@link #getSiswa()}, {@link #getSekolah()}.</li>
 *   <li><b>Salinan identitas pengunjung:</b> {@link #getKode()}, {@link #getNama()},
 *       {@link #getAlamat()}.</li>
 *   <li><b>Waktu:</b> {@link #getTanggal()}, {@link #getTgl()}, {@link #getJam()}.</li>
 *   <li><b>Bebas:</b> {@link #getKeterangan()}.</li>
 * </ul>
 *
 * @see ais.action.master.sekolah.KunjunganSiswaAction
 * @see AbsenPiket
 * @see AbsenPiketDetail
 * @see Siswa
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "kunjungan_siswa")
public class KunjunganSiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya tetap agar baris yang sudah tersimpan di sesi/kluster tetap
	 * dapat dideserialisasi setelah kelas ini diubah; jangan diubah tanpa alasan kuat.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama tabel. Lihat {@link #getId()}. */
	private Long id;

	/** Nama pengguna terakhir yang menyimpan baris ini. Lihat {@link #getOleh()}. */
	private String oleh;

	/** ID pengguna terakhir yang menyimpan baris ini. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan ID pengguna terakhir yang menyimpan baris ini.
	 *
	 * <p>Untuk baris yang lahir dari pemindaian di anjungan, nilainya umumnya {@code null}: tidak
	 * ada pengguna yang login di anjungan dan {@link #onUpdate()} hanya berjalan pada
	 * {@code UPDATE}, bukan pada {@code INSERT}.</p>
	 *
	 * @return ID pengguna, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna terakhir yang menyimpan baris ini.
	 *
	 * <p>Nilai {@code null} atau kosong (setelah {@code trim}) <b>diabaikan diam-diam</b> sehingga
	 * jejak audit yang sudah ada tidak pernah terhapus oleh pemanggil yang tidak mengenal pengguna
	 * aktif.</p>
	 *
	 * @param olehId ID pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna terakhir yang menyimpan baris ini.
	 *
	 * <p>Aturannya identik {@link #setOlehId(String)}: {@code null}/kosong diabaikan diam-diam.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang menyimpan baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dijalankan kontainer persistence TEPAT SEBELUM pernyataan
	 * {@code UPDATE} dikirim ke basis data.
	 *
	 * <p><b>Efek samping:</b> mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@code oleh}/{@code olehId} dari pengguna sesi aktif dan menyegarkan
	 * {@link #getTanggal_dirubah()}. Tidak ada {@code @PrePersist}, sehingga baris hasil
	 * pemindaian (yang hanya di-{@code INSERT}) tidak pernah melewati callback ini dan
	 * mengandalkan nilai awal field {@code tanggal_dirubah}.</p>
	 *
	 * <p>Perlu diingat bahwa getter denormalisasi kelas ini ({@link #getKode()},
	 * {@link #getNama()}, {@link #getAlamat()}, {@link #getSekolah()}) dapat memicu
	 * {@code UPDATE} — dan karenanya callback ini — hanya karena baris dibaca di layar.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir. Diinisialisasi ke waktu server saat instance dibuat sehingga baris
	 * yang baru di-{@code INSERT} pun sudah bercap waktu meski {@link #onUpdate()} belum berjalan.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir.
	 *
	 * <p>Berbeda dari {@link #setOleh(String)}, setter ini menerima {@code null} apa adanya.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (kolom {@code tanggal_dirubah}, presisi
	 * {@code TIMESTAMP}).
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} pada instance hasil
	 *         konstruktor, tetapi bisa {@code null} bila kolom di basis data kosong
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini: mengembalikan {@link #getKeterangan()} apa adanya.
	 *
	 * <p><b>Membalik kontrak kelas induk.</b> {@link GeneralValueObject#toString()} menyusun
	 * {@code "kode - nama"}; di sini yang dikembalikan justru keterangan bebas. Karena baris hasil
	 * pemindaian tidak pernah mengisi keterangan, {@code toString()} praktis selalu
	 * mengembalikan {@code null} — dan pemanggil yang merangkainya ke {@code String} akan
	 * menghasilkan teks {@code "null"}, bukan string kosong. Pola pembalikan kontrak yang sama
	 * sudah tercatat pada banyak entity lain di basis kode ini.</p>
	 *
	 * @return isi {@link #getKeterangan()}, termasuk {@code null} bila belum diisi
	 */
	public String toString() {
		return keterangan;
	}

	/** Siswa yang memindai kartunya; {@code null} untuk baris buku tamu. Lihat {@link #getSiswa()}. */
	private Siswa siswa;

	/** Salinan nama pengunjung. Lihat {@link #getNama()}. */
	private String nama;

	/** Salinan kode pengunjung — praktis selalu kosong. Lihat {@link #getKode()}. */
	private String kode;

	/**
	 * Mengembalikan kode pengunjung, dengan menyalin ulang dari {@link #getSiswa()} bila ada.
	 *
	 * <p><b>Efek samping:</b> bila {@code siswa} tidak {@code null}, field {@code kode} ditimpa
	 * dengan {@code siswa.getKode()} pada setiap pemanggilan; nilai {@code null} kemudian
	 * dinormalkan menjadi {@code ""}. Karena Hibernate memakai akses properti, penulisan balik ini
	 * dapat memicu {@code UPDATE} dan revisi Envers palsu hanya karena baris dibaca.</p>
	 *
	 * <p><b>Kuirk yang perlu diketahui:</b> {@link Siswa} tidak menimpa {@code getKode()} sehingga
	 * yang terpanggil adalah {@link GeneralValueObject#getKode()} — properti kelas induk yang
	 * <b>tidak dipetakan Hibernate</b> (induk bukan {@code @MappedSuperclass}) dan tidak pernah
	 * disetel dari mana pun untuk {@code Siswa}. Nilainya karena itu selalu {@code null}, sehingga
	 * kolom {@code kode} pada baris siswa <b>selalu berisi string kosong</b>. Itu pula sebabnya
	 * kolom "Siswa" di grid — yang dirender sebagai {@code getKode() + " " + getNama()} — selalu
	 * diawali satu spasi menganggur.</p>
	 *
	 * @return kode pengunjung; tidak pernah {@code null}, praktis selalu {@code ""} untuk siswa
	 */
	public String getKode() {
		if (siswa != null) {
			kode = siswa.getKode();
		}
		if (kode == null) {
			kode = "";
		}
		return kode;
	}

	/**
	 * Menyetel kode pengunjung. Nilai apa pun diterima, termasuk {@code null}.
	 *
	 * <p>Praktis tidak berguna untuk baris siswa karena {@link #getKode()} akan menimpanya lagi
	 * pada pembacaan berikutnya.</p>
	 *
	 * @param kode kode baru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama pengunjung, dengan menyalin ulang dari {@link #getSiswa()} bila ada.
	 *
	 * <p><b>Efek samping:</b> sama seperti {@link #getKode()} — bila {@code siswa} tidak
	 * {@code null}, field {@code nama} ditimpa {@code siswa.getNama()} (yang pada {@link Siswa}
	 * adalah alias {@code getNamaSiswa()} yang sudah di-{@code trim}); {@code null} dinormalkan
	 * menjadi {@code ""}. Penulisan balik ini dapat memicu {@code UPDATE} saat baris dibaca.</p>
	 *
	 * <p><b>Mode buku tamu.</b> Bila {@code siswa} {@code null}, kolom ini menampung nama tamu yang
	 * diketik manual — bersama {@link #getAlamat()} — lewat dialog
	 * {@code KunjunganSiswaAction.init()} yang menampilkan label "Nama Pengunjung"/"Alamat
	 * Pengunjung" saat centang "Bukan Siswa Sekolah" aktif. Jalur itu praktis mati: tombol
	 * pemicunya ({@code id="add"}, label "Pengunjung Bukan Siswa") sudah {@code visible="false"}
	 * di {@code welsis.zul} <i>dan</i> dipaksa {@code add.setVisible(false)} sekali lagi di
	 * {@code doAfterCompose}, sementara halaman master {@code kunjuangan_siswa.zul} tidak
	 * mendeklarasikan tombol itu sama sekali.</p>
	 *
	 * @return nama pengunjung; tidak pernah {@code null}
	 */
	public String getNama() {
		if (siswa != null) {
			nama = siswa.getNama();
		}
		if (nama == null) {
			nama = "";
		}
		return nama;
	}

	/**
	 * Menyetel nama pengunjung. Menerima {@code null}.
	 *
	 * <p>Hanya bertahan untuk baris buku tamu; pada baris siswa nilainya akan ditimpa
	 * {@link #getNama()}.</p>
	 *
	 * @param nama nama pengunjung
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** Salinan alamat pengunjung. Lihat {@link #getAlamat()}. */
	private String alamat;

	/** Unit sekolah pemilik baris. Nilainya derivatif. Lihat {@link #getSekolah()}. */
	private Sekolah sekolah;

	/** Cap waktu kedatangan lengkap (tanggal + jam:menit:detik). Lihat {@link #getTanggal()}. */
	private Date tanggal = ais.ui.util.WaktuUtil.getDate();

	/** Tanggal kedatangan tanpa komponen waktu, dipakai untuk dedup. Lihat {@link #getTgl()}. */
	private Date tgl = ais.ui.util.WaktuUtil.getDate();

	/** Jam kedatangan (0-23), dipakai untuk dedup. Lihat {@link #getJam()}. */
	private Integer jam;

	/** Catatan bebas; praktis tidak pernah terisi dari anjungan. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate.
	 *
	 * <p>Inisialisasi field membuat {@link #getTanggal()} dan {@link #getTgl()} langsung berisi
	 * waktu server saat instance dibuat, sehingga baris baru selalu punya cap waktu meski
	 * pemanggil lupa menyetelnya. {@link #getJam()} baru terisi pada pembacaan pertama.</p>
	 */
	public KunjunganSiswa() {
	}

	/**
	 * Mengembalikan kunci utama baris ini (kolom {@code id}).
	 *
	 * <p>Dihasilkan basis data ({@code IDENTITY}) dan karena itu {@code insertable = false} —
	 * nilainya baru terisi setelah {@code INSERT} berhasil.</p>
	 *
	 * @return ID baris, atau {@code null} bila belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Diisi Hibernate; jangan dipakai kode aplikasi.
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan catatan bebas atas kedatangan ini (kolom {@code keterangan}, boleh
	 * {@code null}).
	 *
	 * <p>Berbeda dari {@link GeneralValueObject#getKeterangan()} yang menormalkan {@code null}
	 * menjadi {@code ""}, versi ini mengembalikan nilai mentah — termasuk {@code null}. Kolom ini
	 * hanya bisa diisi lewat dialog buku tamu {@code KunjunganSiswaAction.init()} (label
	 * "Keterangan"), sehingga baris hasil pemindaian selalu meninggalkannya kosong. Konsumen
	 * terpenting adalah {@link #toString()} dan renderer grid, yang keduanya memakainya tanpa
	 * pemeriksaan {@code null}.</p>
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel catatan bebas. Menerima {@code null}.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan siswa yang memindai kartunya (kolom FK {@code siswa}).
	 *
	 * <p>Relasi {@code @ManyToOne} lazy dengan cascade {@code PERSIST}+{@code MERGE}. Pemanggilan
	 * melewatkan nilainya ke {@link GeneralValueObject#check(Object)} lebih dulu agar proxy lazy
	 * yang sudah lepas dari sesi diresolusi menjadi instance nyata; {@code check} tidak pernah
	 * melempar exception dan mengembalikan argumennya apa adanya bila resolusi gagal.</p>
	 *
	 * <p>Kolom FK dideklarasikan {@code nullable = false} pada anotasi, tetapi mode buku tamu
	 * memang menyimpan baris dengan {@code siswa == null}; batasan sesungguhnya bergantung pada
	 * skema basis data yang terpasang, bukan pada anotasi ini.</p>
	 *
	 * <p><b>Catatan akses/privasi.</b> Relasi inilah yang membuat baris ini menjadi data pribadi
	 * anak di bawah umur. Pembacaannya tidak dijaga kepemilikan:
	 * {@code KunjunganSiswaAction.initCriteria()} hanya menambahkan
	 * {@code Restrictions.eq("siswa", siswa)} bila pengguna yang login <b>adalah</b> seorang siswa;
	 * untuk peran lain — termasuk orang tua/wali, guru, dan pengunjung anonim — tidak ada
	 * penyaringan sama sekali (kondisinya menjadi {@code sqlRestriction("true")}), sehingga seluruh
	 * baris lintas sekolah dan lintas yayasan ikut tampil. Pola fail-open yang sama sudah tercatat
	 * pada data pelanggaran/hukuman siswa.</p>
	 *
	 * @return siswa pemilik baris, atau {@code null} pada baris buku tamu
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = false)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	/**
	 * Menyetel siswa pemilik baris. Menerima {@code null} (mode buku tamu).
	 *
	 * @param siswa siswa yang memindai kartunya
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Mengembalikan cap waktu kedatangan lengkap (kolom {@code tanggal}, presisi
	 * {@code TIMESTAMP}).
	 *
	 * <p>Inilah nilai yang ditampilkan sebagai "Waktu" di grid dan yang diformat menjadi jam datang
	 * pada {@link AbsenPiketDetail}. Bila kosong, getter mengembalikan waktu server saat ini
	 * <b>tanpa</b> menyimpannya ke field — jadi tidak ada penulisan balik di sini, tetapi dua
	 * pemanggilan beruntun pada baris bercap {@code null} bisa mengembalikan nilai berbeda.</p>
	 *
	 * @return cap waktu kedatangan; tidak pernah {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal() {
		return tanggal == null ? WaktuUtil.getDate() : tanggal;
	}

	/**
	 * Menyetel cap waktu kedatangan.
	 *
	 * @param tanggal cap waktu baru; boleh {@code null} (dibaca kembali sebagai waktu sekarang)
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengembalikan unit sekolah pemilik baris (kolom FK {@code sekolah}), <b>diturunkan ulang</b>
	 * dari {@link #getSiswa()} bila siswa terisi.
	 *
	 * <p><b>Efek samping:</b> getter ini menulis ke dua field sekaligus. Ia memanggil
	 * {@link #getSiswa()} (yang sendiri menulis balik field {@code siswa} hasil {@code check}),
	 * lalu — bila siswa ada — <b>menimpa</b> field {@code sekolah} dengan
	 * {@code siswa.getSekolah()}, dan akhirnya melewatkan hasilnya ke
	 * {@link GeneralValueObject#check(Object)}. Nilai sekolah yang disimpan penulis sebelumnya
	 * karena itu tidak pernah menang atas sekolah siswa; bila siswa pindah sekolah, seluruh
	 * riwayat kedatangannya ikut "pindah" pada pembacaan berikutnya. Seperti getter denormalisasi
	 * lain di kelas ini, penulisan balik tersebut dapat memicu {@code UPDATE} dan revisi Envers
	 * palsu hanya karena baris dibaca.</p>
	 *
	 * @return unit sekolah pemilik baris, atau {@code null} bila tidak dapat ditentukan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = false)
	public Sekolah getSekolah() {
		siswa = getSiswa();
		if (siswa != null) {
			sekolah = siswa.getSekolah();
		}
		sekolah = check(sekolah);
		return sekolah;
	}

	/**
	 * Menyetel unit sekolah pemilik baris.
	 *
	 * <p><b>Penjaga khusus:</b> objek yang {@code null} <i>atau</i> yang belum punya
	 * {@link Sekolah#getId()} (mis. instance kosong hasil combobox yang belum dipilih) disimpan
	 * sebagai {@code null}, bukan sebagai entity transien. Ini mencegah Hibernate mencoba
	 * meng-{@code cascade PERSIST} sekolah baru yang tidak diinginkan.</p>
	 *
	 * @param sekolah unit sekolah; {@code null} atau tanpa ID akan disimpan sebagai {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan tanggal kedatangan tanpa komponen waktu (kolom {@code tgl}, presisi
	 * {@code DATE}).
	 *
	 * <p>Bersama {@link #getJam()}, kolom inilah kunci dedup "satu baris per siswa per jam" yang
	 * dipakai kedua kanal penulis. Nilainya <b>tidak pernah disetel secara eksplisit</b> oleh kode
	 * mana pun: satu-satunya sumbernya adalah inisialisasi field ke waktu server saat instance
	 * dibuat. Karena kolom dipetakan {@code TemporalType.DATE}, Hibernate memotong komponen jam
	 * baik saat menyimpan maupun saat mengikat parameter {@code Restrictions.eq("tgl", ...)},
	 * sehingga perbandingannya tetap benar meski argumennya bercap waktu penuh.</p>
	 *
	 * @return tanggal kedatangan; tidak pernah {@code null} pada instance hasil konstruktor
	 */
	@Temporal(TemporalType.DATE)
	public Date getTgl() {
		return tgl;
	}

	/**
	 * Menyetel tanggal kedatangan. Tidak dipanggil dari mana pun di basis kode saat ini.
	 *
	 * @param tgl tanggal baru; boleh {@code null}
	 */
	public void setTgl(Date tgl) {
		this.tgl = tgl;
	}

	/**
	 * Mengembalikan alamat pengunjung, dengan menyalin ulang dari {@link #getSiswa()} bila ada.
	 *
	 * <p><b>Efek samping:</b> bila {@code siswa} tidak {@code null}, field {@code alamat} ditimpa
	 * dengan {@code siswa.getAlamatSiswa()} pada setiap pemanggilan — penulisan balik yang dapat
	 * memicu {@code UPDATE} dan revisi Envers palsu saat baris sekadar dibaca. Berbeda dari
	 * {@link #getKode()}/{@link #getNama()}, di sini {@code null} <b>tidak</b> dinormalkan menjadi
	 * {@code ""}, sehingga baris buku tamu tanpa alamat mengembalikan {@code null} apa adanya.</p>
	 *
	 * <p><b>Catatan privasi.</b> {@code Siswa.getAlamatSiswa()} adalah alamat rumah siswa. Getter
	 * ini menyalinnya ke tabel kedatangan, dan salinan itu ikut terbawa ke berkas unduhan Excel
	 * (kolom {@code alamat} terdaftar eksplisit di
	 * {@code KunjunganSiswaAction.doAfterCompose}). Layar yang menghasilkan unduhan tersebut tidak
	 * menerapkan penyaringan kepemilikan — lihat catatan pada {@link #getSiswa()}.</p>
	 *
	 * @return alamat pengunjung, atau {@code null} bila tidak ada
	 */
	public String getAlamat() {
		if (siswa != null) {
			alamat = siswa.getAlamatSiswa();
		}
		return alamat;
	}

	/**
	 * Menyetel alamat pengunjung. Menerima {@code null}.
	 *
	 * <p>Hanya bertahan untuk baris buku tamu; pada baris siswa nilainya akan ditimpa
	 * {@link #getAlamat()}.</p>
	 *
	 * @param alamat alamat pengunjung
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Mengembalikan jam kedatangan dalam format 24 jam (kolom {@code jam}, nilai 0-23).
	 *
	 * <p><b>Efek samping:</b> bila field masih {@code null}, getter mengisinya dengan
	 * {@code Calendar.HOUR_OF_DAY} <b>saat pemanggilan</b> dan menyimpan hasilnya ke field. Karena
	 * Hibernate memakai akses properti, inilah satu-satunya mekanisme pengisian kolom {@code jam}:
	 * tidak ada penulis yang memanggil {@link #setJam(Integer)}, sehingga nilai yang tersimpan
	 * adalah jam pada saat Hibernate membaca properti ini menjelang {@code INSERT}.</p>
	 *
	 * <p><b>Kegunaan:</b> bersama {@link #getTgl()} menjadi kunci dedup pemindaian ("cegah spam
	 * absen di jam yang sama") pada {@code KunjunganSiswaAction.onKodeSiswa()},
	 * {@code KunjunganSiswaAction.onSave()}, dan layanan JSON {@code action=scan}. Pengisian lazy
	 * ini juga berarti baris lama yang kolom {@code jam}-nya kosong di basis data akan "berubah"
	 * menjadi jam sekarang begitu dibaca dalam sesi aktif.</p>
	 *
	 * @return jam kedatangan 0-23; tidak pernah {@code null}
	 */
	public Integer getJam() {
		if (jam == null) {
			jam = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		}
		return jam;
	}

	/**
	 * Menyetel jam kedatangan. Tidak dipanggil dari mana pun di basis kode saat ini.
	 *
	 * @param jam jam 0-23; {@code null} akan diisi ulang otomatis oleh {@link #getJam()}
	 */
	public void setJam(Integer jam) {
		this.jam = jam;
	}

}
