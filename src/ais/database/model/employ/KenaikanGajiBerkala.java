package ais.database.model.employ;

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

import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;



/**
 * Model data untuk kenaikan gaji berkala. Tipe ini membawa state yang dipertukarkan oleh lapisan
 * persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang
 * dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code String keterangan}, {@code Date tanggal_dirubah}, {@code Pegawai pegawai}, {@code
 * String noSK}, {@code Date tanggalSk}; pemetaan persistence: tabel {@code employ.kenaikan_gaji_berkala};
 * pembacaan/pencarian ({@code getOlehId()}, {@code getId()}, {@code getOleh()}, {@code getTanggal_dirubah()},
 * {@code getKeterangan()}, {@code getPegawai()}); mutasi data ({@code setOlehId()}, {@code onUpdate()}, {@code
 * setId()}, {@code setOleh()}, {@code setTanggal_dirubah()}, {@code setKeterangan()}); operasi domain lain
 * ({@code toString()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * <h2>Isi berkas</h2>
 * <p>Kenaikan gaji berkala (KGB) adalah penyesuaian gaji pokok yang diberikan secara periodik karena
 * bertambahnya masa kerja, tanpa disertai kenaikan pangkat. Satu baris di sini mencatat satu
 * peristiwa KGB: gaji pokok lama ({@link #getGaji()}) dan gaji pokok baru
 * ({@link #getGajiPokokBaru()}), masa kerja pada saat itu dalam tahun dan bulan, nomor dan tanggal
 * SK, tanggal mulai berlaku ({@link #getTmt()}), perkiraan tanggal kenaikan berikutnya
 * ({@link #getNaikBerikutnya()}), serta status pemrosesannya.</p>
 *
 * <p>Berbeda dengan {@link KenaikanPangkat} yang mengubah golongan, jabatan, dan hak akses pengguna,
 * entity ini murni mengenai nilai gaji pokok. Keduanya tetap bersinggungan: berkas
 * {@link KenaikanPangkat} memiliki penanda "terdapat kenaikan gaji berkala" beserta jarak bulannya,
 * yang dipakai {@code Pegawai} saat menurunkan gaji pokok yang berlaku. Kedua jalur itu berdiri
 * sendiri dan tidak saling memutakhirkan.</p>
 *
 * <h2>Masa kerja yang dicatat adalah salinan</h2>
 * <p>{@link #getMasaKerjaTahun()} dan {@link #getMasaKerjaBulan()} adalah <b>angka rekaman</b>, bukan
 * relasi ke {@link MasaKerja} maupun hasil perhitungan ulang. Keduanya menyimpan masa kerja pegawai
 * sebagaimana dinilai saat KGB diproses, sehingga berkas tetap dapat dipertanggungjawabkan meski
 * aturan perhitungan masa kerja berubah kemudian.</p>
 *
 * <h2>Gerbang persetujuan yang lemah</h2>
 * <p>{@link #getStatus()} menyimpan status pemrosesan berkas dan memang benar-benar dipakai — berbeda
 * dengan field serupa pada {@link Pensiun} dan {@link MutasiPindah} yang tidak pernah ditulis.
 * Nilainya dipilih operator lewat sepasang radio pada layar pengelola. Yang perlu diwaspadai: pilihan
 * itu <b>tidak dijaga hak akses persetujuan apa pun</b> — layar tersebut hanya memeriksa hak baca,
 * tambah, ubah, dan hapus, sehingga siapa pun yang boleh menyunting berkas dapat sekaligus menandainya
 * disetujui, termasuk berkas yang ia buat sendiri. Tidak ada pemisahan pengusul dan penyetuju, tidak
 * ada pemeriksaan kepemilikan, dan tidak ada keterlibatan alur SOP. Perlakukan status di sini sebagai
 * catatan administratif, bukan sebagai bukti persetujuan yang berwenang.</p>
 *
 * @see GeneralValueObject
 * @see KenaikanPangkat
 * @see GajiPokok
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "employ", name = "kenaikan_gaji_berkala")



public class KenaikanGajiBerkala extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilai ini dipakai bersama oleh banyak entity paket
	 * {@code employ} karena berkas-berkasnya disalin dari template yang sama; angkanya tidak memiliki
	 * makna bisnis dan tidak boleh diubah tanpa alasan, sebab perubahan akan mematahkan deserialisasi
	 * state ZK/HTTP session dari versi aplikasi sebelumnya.
	 */
	private static final long serialVersionUID = 1129196121609467759L;

	/** Kunci utama baris {@code employ.kenaikan_gaji_berkala}; diisi database (IDENTITY). */
	private Long id;
	/**
	 * Nama/identitas petugas terakhir yang menyimpan berkas ini, beserta id penggunanya. Keduanya
	 * beserta accessor-nya dideklarasikan dalam satu baris mengikuti gaya berkas aslinya; setter
	 * masing-masing bersifat satu arah -- masukan {@code null}/kosong-setelah-trim diabaikan agar
	 * jejak audit tidak pernah tertimpa kosong, dan nilai yang sudah terisi tidak dapat dikosongkan
	 * kembali lewat setter.
	 */
	private String oleh;private String olehId;public String getOlehId() {return olehId;}public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}
	/** Keterangan bebas untuk berkas ini; juga menjadi label {@link #toString()}. */
	private String keterangan;
	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil otomatis Hibernate tepat sebelum tiap {@code UPDATE}
	 * baris ini, mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah}
	 * yang menyetel {@link #tanggal_dirubah} ke waktu saat itu. Deklarasi one-liner mengikuti gaya
	 * berkas hbm2java asli (tidak dirapikan agar diff minimal terhadap riwayat SVN).
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Kunci utama baris {@code employ.kenaikan_gaji_berkala}.
	 *
	 * <p>Dihasilkan database dengan strategi {@code IDENTITY} dan dipetakan
	 * {@code insertable = false}, sehingga nilai yang diisi manual pada objek baru <b>diabaikan</b>
	 * saat {@code INSERT}. Objek yang belum tersimpan mengembalikan {@code null}.</p>
	 *
	 * @return id baris, atau {@code null} bila entity belum pernah tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Setter {@link #getId()}. Umumnya hanya dipanggil Hibernate saat memuat/menyimpan baris, atau
	 * oleh kode yang sengaja membentuk referensi ringan ke baris yang sudah ada.
	 *
	 * @param id nilai kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Setter {@link #getOleh()} -- pola pengaman sama dengan {@code setOlehId(String)}: masukan
	 * {@code null}/kosong-setelah-trim diabaikan sehingga nama penyimpan lama tidak tertimpa kosong.
	 *
	 * @param oleh nama penyimpan; {@code null}/kosong diabaikan diam-diam
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama/identitas petugas yang terakhir menyimpan berkas KGB ini -- jejak audit untuk ditampilkan
	 * di layar. Karena berkas ini menetapkan nilai gaji pokok baru dan status persetujuannya tidak
	 * dijaga hak khusus, jejak siapa yang terakhir menyimpannya bernilai penting saat penelusuran.
	 *
	 * @return nama penyimpan terakhir, atau {@code null} bila tidak tercatat
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Setter {@link #getTanggal_dirubah()}. Normalnya <b>tidak perlu dipanggil manual</b> karena
	 * {@link #onUpdate()} sudah menyetelnya otomatis sebelum tiap {@code UPDATE}. Pemanggilan manual
	 * hanya masuk akal pada migrasi/impor data yang ingin mempertahankan cap waktu asli.
	 *
	 * @param tanggal_dirubah cap waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Cap waktu perubahan terakhir baris ini. Diinisialisasi ke waktu pembuatan objek melalui
	 * {@code WaktuUtil.getDate()} (bukan {@code new Date()}, agar mengikuti sumber waktu tunggal
	 * aplikasi) dan diperbarui otomatis oleh {@link #onUpdate()}. Jangan tertukar dengan
	 * {@link #getTmt()} yang merupakan tanggal berlakunya kenaikan gaji.
	 *
	 * @return cap waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang dibentuk di JVM
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Label teks entity ini, yaitu isi {@link #getKeterangan() keterangan}. Karena kolom tersebut
	 * boleh {@code null}, komponen ZK yang menampilkan objek ini apa adanya dapat memperlihatkan
	 * baris kosong.
	 *
	 * @return keterangan berkas ini, atau {@code null} bila belum diisi
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Keterangan bebas mengenai berkas KGB ini. Boleh {@code null}. Perhatikan bahwa nilai ini juga
	 * dipakai sebagai hasil {@link #toString()}.
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Setter {@link #getKeterangan()}. Menerima {@code null} apa adanya sehingga keterangan dapat
	 * dikosongkan kembali.
	 *
	 * @param keterangan keterangan bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** Pegawai penerima kenaikan gaji berkala ini; relasi wajib. */
	private Pegawai pegawai;
	/** Nomor SK kenaikan gaji berkala. */
	private String noSK;
	/** Tanggal SK kenaikan gaji berkala. */
	private Date tanggalSk;
	/** Salinan masa kerja (tahun) pada saat KGB diproses; wajib terisi. */
	private Integer masaKerjaTahun;
	/** Salinan masa kerja (bulan) pada saat KGB diproses; wajib terisi. */
	private Integer masaKerjaBulan;
	/** Gaji pokok baru hasil kenaikan; wajib terisi. */
	private GajiPokok gajiPokokBaru;
	/** Terhitung mulai tanggal berlakunya kenaikan; wajib terisi. */
	private Date tmt;
	/** Perkiraan tanggal kenaikan berkala berikutnya. */
	private Date naikBerikutnya;
	/** Status pemrosesan berkas; wajib terisi -- lihat {@link #getStatus()}. */
	private String status;
	/** Gaji pokok lama sebelum kenaikan; wajib terisi. */
	private GajiPokok gaji;

	/**
	 * Pegawai penerima kenaikan gaji berkala ini. Kolomnya {@code nullable = false} sehingga setiap
	 * berkas wajib menyebut pemiliknya.
	 *
	 * <p>Perlu dicatat bahwa getter ini adalah <b>pembaca murni</b>: ia mengembalikan referensi apa
	 * adanya, tanpa resolusi proxy lazy dan tanpa substitusi pengguna aktif. Ini berbeda dari
	 * {@link KenaikanPangkat#getPegawai()}, {@link Pensiun#getPegawai()},
	 * {@link MutasiPindah#getPegawai()}, dan {@link RiwayatStatusKepegawaian#getPegawai()} yang
	 * semuanya mengganti referensi kosong dengan pegawai pengguna yang sedang login. Di sini
	 * referensi yang kosong tetap kosong, sehingga penyimpanan berkas tanpa pegawai akan ditolak
	 * database alih-alih diam-diam berpindah kepemilikan — perilaku yang justru lebih aman.</p>
	 *
	 * <p>Konsekuensinya, relasi ini <b>tidak kebal terhadap kegagalan pemuatan lazy</b>: membacanya
	 * di luar session yang memuat berkas dapat gagal. Muat berkas beserta pegawainya dalam satu
	 * session, atau lakukan resolusi sendiri di lapisan pemanggil.</p>
	 *
	 * <p>Relasi memakai {@code FetchMode.SELECT} sehingga pegawai diambil lewat query terpisah, dan
	 * cascade {@code PERSIST}/{@code MERGE} membuat penyimpanan berkas ini ikut menyimpan perubahan
	 * pada objek pegawai yang tertaut.</p>
	 *
	 * @return pegawai penerima KGB; {@code null} hanya pada objek yang belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pegawai", nullable = false)
	public Pegawai getPegawai() {
		return pegawai;
	}

	/**
	 * Setter {@link #getPegawai()}. Nilai wajib diisi sebelum penyimpanan karena kolomnya
	 * {@code nullable = false}; berbeda dengan entity berkas lain di paket ini, {@code null} di sini
	 * benar-benar berarti kosong dan tidak akan disubstitusi getter.
	 *
	 * @param pegawai pegawai penerima KGB
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Nomor SK kenaikan gaji berkala. Teks bebas tanpa format yang ditegakkan; kolomnya tidak
	 * dinyatakan wajib sehingga boleh {@code null}.
	 *
	 * @return nomor SK, atau {@code null} bila belum diisi
	 */
	@Column(name = "no_sk")
	public String getNoSK() {
		return noSK;
	}

	/**
	 * Setter {@link #getNoSK()}.
	 *
	 * @param noSK nomor SK; boleh {@code null}
	 */
	public void setNoSK(String noSK) {
		this.noSK = noSK;
	}

	/**
	 * Tanggal SK kenaikan gaji berkala, yaitu kapan surat keputusan diterbitkan. Dibedakan dari
	 * {@link #getTmt()} yang mencatat kapan kenaikan mulai berlaku; SK lazim terbit setelah TMT-nya.
	 *
	 * <p>Perhatikan ketidakseragaman penamaan: getter dan setter memakai ejaan {@code SK} berhuruf
	 * besar sementara field penyimpannya bernama {@code tanggalSk}. Pemetaan Hibernate mengikuti nama
	 * properti dari getter, jadi selisih ejaan ini tidak berpengaruh pada penyimpanan.</p>
	 *
	 * @return tanggal SK, atau {@code null} bila belum diisi
	 */
	@Column(name = "tanggal_sk")
	public Date getTanggalSK() {
		return tanggalSk;
	}

	/**
	 * Setter {@link #getTanggalSK()}.
	 *
	 * @param tanggalSK tanggal terbit SK; boleh {@code null}
	 */
	public void setTanggalSK(Date tanggalSK) {
		this.tanggalSk = tanggalSK;
	}

	/**
	 * Masa kerja pegawai dalam <b>tahun</b> pada saat kenaikan gaji berkala ini diproses, disimpan
	 * sebagai angka rekaman. Kolomnya {@code nullable = false} sehingga wajib terisi.
	 *
	 * <p>Ini adalah salinan, bukan nilai hidup: tidak ada relasi ke entity {@link MasaKerja} dan
	 * tidak ada perhitungan ulang saat dibaca. Dengan begitu berkas tetap mencerminkan dasar
	 * penetapan yang berlaku saat itu meski aturan perhitungan masa kerja diubah kemudian. Angka ini
	 * juga tidak diselaraskan dengan masa kerja yang dipakai {@link GajiPokok} untuk memilih baris
	 * tabel gaji; keduanya dapat berbeda bila diisi lewat jalur yang berlainan.</p>
	 *
	 * @return masa kerja dalam tahun; {@code null} hanya pada objek yang belum diisi
	 */
	@Column(name = "masa_kerja_tahun", nullable = false)
	public Integer getMasaKerjaTahun() {
		return masaKerjaTahun;
	}

	/**
	 * Setter {@link #getMasaKerjaTahun()}. Nilai wajib diisi sebelum penyimpanan karena kolomnya
	 * {@code nullable = false}. Tidak ada pemeriksaan bahwa nilainya tidak negatif.
	 *
	 * @param masaKerjaTahun masa kerja dalam tahun
	 */
	public void setMasaKerjaTahun(Integer masaKerjaTahun) {
		this.masaKerjaTahun = masaKerjaTahun;
	}

	/**
	 * Sisa masa kerja dalam <b>bulan</b> yang melengkapi {@link #getMasaKerjaTahun()}, disimpan
	 * sebagai angka rekaman. Kolomnya {@code nullable = false} sehingga wajib terisi.
	 *
	 * <p>Tidak ada pemeriksaan di tingkat model bahwa nilainya berada dalam rentang 0 sampai 11;
	 * penormalan bulan menjadi tahun sepenuhnya tanggung jawab layar pengisi.</p>
	 *
	 * @return sisa masa kerja dalam bulan; {@code null} hanya pada objek yang belum diisi
	 */
	@Column(name = "masa_kerja_bulan", nullable = false)
	public Integer getMasaKerjaBulan() {
		return masaKerjaBulan;
	}

	/**
	 * Setter {@link #getMasaKerjaBulan()}. Nilai wajib diisi sebelum penyimpanan karena kolomnya
	 * {@code nullable = false}.
	 *
	 * @param masaKerjaBulan sisa masa kerja dalam bulan
	 */
	public void setMasaKerjaBulan(Integer masaKerjaBulan) {
		this.masaKerjaBulan = masaKerjaBulan;
	}

	/**
	 * Status pemrosesan berkas kenaikan gaji berkala, disimpan sebagai teks bebas dengan kolom
	 * {@code nullable = false}.
	 *
	 * <p>Berbeda dengan field bernama sama pada {@link Pensiun} dan {@link MutasiPindah} yang tidak
	 * pernah ditulis kode mana pun, status di sini <b>benar-benar dipakai</b>. Nilainya dipilih
	 * operator dari sepasang radio berlabel "DISETUJUI" dan "BELUM DIPROSES" pada layar pengelola
	 * KGB; berkas yang statusnya {@code null} diperlakukan layar tersebut sama dengan "BELUM
	 * DIPROSES". Perlu dicatat bahwa kedua teks itu dideklarasikan sebagai konstanta privat di dalam
	 * kelas Action-nya, bukan sebagai konstanta pada entity ini — kebalikan dari {@link Pensiun} dan
	 * {@link MutasiPindah} yang mendeklarasikan konstantanya di entity namun tak pernah memakainya.
	 * Kode baru yang perlu membandingkan status karena itu harus menuliskan sendiri teks
	 * pembandingnya, dengan risiko salah eja yang menyertainya.</p>
	 *
	 * <p><b>Peringatan mengenai kewenangan.</b> Pilihan status pada layar pengelola tidak dijaga hak
	 * akses persetujuan apa pun: layar tersebut hanya memeriksa hak baca, tambah, ubah, dan hapus.
	 * Siapa pun yang berwenang menyunting berkas KGB dengan sendirinya dapat menandainya "DISETUJUI",
	 * termasuk berkas yang ia buat sendiri, tanpa pemisahan pengusul dan penyetuju, tanpa pemeriksaan
	 * kepemilikan, dan tanpa keterlibatan alur SOP. Status di sini karenanya merupakan catatan
	 * administratif, bukan bukti persetujuan yang berwenang.</p>
	 *
	 * @return status pemrosesan; {@code null} pada baris lama diperlakukan sebagai belum diproses
	 */
	@Column(name = "status", nullable = false)
	public String getStatus() {
		return status;
	}

	/**
	 * Setter {@link #getStatus()}. Menerima teks apa adanya tanpa memeriksa bahwa nilainya termasuk
	 * salah satu status yang dikenal, dan tanpa pemeriksaan hak akses apa pun.
	 *
	 * @param status status pemrosesan berkas
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Baris tabel gaji pokok yang berlaku <b>setelah</b> kenaikan berkala ini, yaitu hasil yang
	 * hendak dicapai berkas ini. Kolomnya {@code nullable = false} sehingga wajib terisi.
	 *
	 * <p>Berpasangan dengan {@link #getGaji()} yang menyimpan gaji pokok lama; keduanya bersama-sama
	 * membuat berkas ini dapat dibaca sebagai perpindahan dari satu baris tabel gaji ke baris lain.
	 * Tidak ada pemeriksaan di tingkat model bahwa nilai baru lebih tinggi dari yang lama, maupun
	 * bahwa keduanya berasal dari golongan yang sama.</p>
	 *
	 * <p>Relasi memakai {@code FetchMode.SELECT} sehingga diambil lewat query terpisah; getter ini
	 * pembaca murni tanpa resolusi lazy.</p>
	 *
	 * @return gaji pokok baru; {@code null} hanya pada objek yang belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "gaji_pokok_baru", nullable = false)
	public GajiPokok getGajiPokokBaru() {
		return gajiPokokBaru;
	}

	/**
	 * Setter {@link #getGajiPokokBaru()}. Nilai wajib diisi sebelum penyimpanan karena kolomnya
	 * {@code nullable = false}.
	 *
	 * @param gajiPokokBaru gaji pokok setelah kenaikan
	 */
	public void setGajiPokokBaru(GajiPokok gajiPokokBaru) {
		this.gajiPokokBaru = gajiPokokBaru;
	}

	/**
	 * Terhitung mulai tanggal (TMT), yaitu saat kenaikan gaji berkala ini <b>mulai berlaku</b>.
	 * Kolomnya {@code nullable = false}.
	 *
	 * <p>Perlu ditegaskan bahwa tanggal ini tidak menggerakkan apa pun secara otomatis: gaji pokok
	 * yang berlaku bagi seorang pegawai diturunkan {@code Pegawai} dari berkas {@link KenaikanPangkat}
	 * beserta penanda kenaikan berkala di sana, bukan dibaca dari kumpulan berkas KGB ini. Menyimpan
	 * baris di sini karena itu belum tentu mengubah gaji yang benar-benar dihitung; keselarasan
	 * antara kedua jalur menjadi tanggung jawab operator.</p>
	 *
	 * @return tanggal mulai berlaku kenaikan; {@code null} hanya pada objek yang belum diisi
	 */
	@Column(name = "tmt", nullable = false)
	public Date getTmt() {
		return tmt;
	}

	/**
	 * Setter {@link #getTmt()}. Nilai wajib diisi sebelum penyimpanan karena kolomnya
	 * {@code nullable = false}.
	 *
	 * @param tmt tanggal mulai berlaku kenaikan
	 */
	public void setTmt(Date tmt) {
		this.tmt = tmt;
	}

	/**
	 * Perkiraan tanggal kenaikan gaji berkala <b>berikutnya</b>, dipakai layar penjadwalan untuk
	 * menyorot pegawai yang sudah waktunya diproses. Boleh {@code null}.
	 *
	 * <p>Nilai ini <b>tidak dihitung otomatis</b> dari {@link #getTmt()} maupun dari jarak bulan mana
	 * pun: layar pengelola hanya mengisi kolom tanggalnya dengan tanggal hari ini sebagai nilai awal,
	 * dan sisanya diserahkan kepada operator. Tidak ada proses terjadwal yang membaca tanggal ini
	 * lalu membentuk berkas KGB berikutnya secara otomatis — perannya sebatas pengingat.</p>
	 *
	 * @return perkiraan tanggal kenaikan berikutnya, atau {@code null} bila tidak diisi
	 */
	@Column(name = "naik_berikutnya")
	public Date getNaikBerikutnya() {
		return naikBerikutnya;
	}

	/**
	 * Setter {@link #getNaikBerikutnya()}.
	 *
	 * @param naikBerikutnya perkiraan tanggal kenaikan berikutnya; boleh {@code null}
	 */
	public void setNaikBerikutnya(Date naikBerikutnya) {
		this.naikBerikutnya = naikBerikutnya;
	}

	/**
	 * Baris tabel gaji pokok yang berlaku <b>sebelum</b> kenaikan berkala ini, yaitu titik tolak
	 * berkas ini. Kolomnya {@code nullable = false} sehingga wajib terisi.
	 *
	 * <p>Penamaannya patut diperhatikan: field ini bernama {@code gaji} saja sementara pasangannya
	 * bernama {@link #getGajiPokokBaru() gajiPokokBaru}, sehingga sekilas tampak seperti dua hal yang
	 * berlainan jenis padahal keduanya sama-sama menunjuk baris {@link GajiPokok}. Bacalah pasangan
	 * ini sebagai "gaji pokok lama" dan "gaji pokok baru".</p>
	 *
	 * <p>Disimpan sebagai salinan referensi pada berkas sehingga riwayat kenaikan tetap terbaca meski
	 * data gaji pegawai berubah lagi kemudian. Relasi memakai {@code FetchMode.SELECT}; getter ini
	 * pembaca murni tanpa resolusi lazy.</p>
	 *
	 * @return gaji pokok sebelum kenaikan; {@code null} hanya pada objek yang belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "gaji", nullable = false)
	public GajiPokok getGaji() {
		return gaji;
	}

	/**
	 * Setter {@link #getGaji()}. Nilai wajib diisi sebelum penyimpanan karena kolomnya
	 * {@code nullable = false}.
	 *
	 * @param gaji gaji pokok sebelum kenaikan
	 */
	public void setGaji(GajiPokok gaji) {
		this.gaji = gaji;
	}

}
