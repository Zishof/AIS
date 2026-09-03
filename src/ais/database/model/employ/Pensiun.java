package ais.database.model.employ;

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

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;

/**
 * Model data untuk pensiun. Tipe ini membawa state yang dipertukarkan oleh lapisan persistence,
 * service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code String keterangan}, {@code Date tanggal_dirubah}, {@code String DISETUJUI}, {@code
 * String BELUM_DIPROSES}, {@code Pegawai pegawai}; pemetaan persistence: tabel {@code employ.pensiun};
 * pembacaan/pencarian ({@code getOlehId()}, {@code getId()}, {@code getOleh()}, {@code getTanggal_dirubah()},
 * {@code getKeterangan()}, {@code getPegawai()}); mutasi data ({@code setOlehId()}, {@code onUpdate()}, {@code
 * setId()}, {@code setOleh()}, {@code setTanggal_dirubah()}, {@code setKeterangan()}); operasi domain lain
 * ({@code toString()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * <h2>Status pemakaian: entity tidur</h2>
 * <p>Berdasarkan penelusuran seluruh kode sumber, entity ini <b>tidak dipakai alur berjalan mana pun</b>.
 * Satu-satunya yang menyebut namanya adalah pasangan DAO-nya sendiri
 * ({@code ais.database.dao.employ.PensiunDao} dan implementasinya), sementara
 * {@code getPensiunDao()} tidak pernah dipanggil dari mana pun. Yang lebih menyesatkan:
 * {@code ais.action.master.employ.PensiunAction} — beserta halaman ZUL yang menunjuk kepadanya —
 * merupakan turunan {@code PegawaiAction} dan <b>sama sekali tidak mengimpor entity ini</b>, sehingga
 * kesamaan nama tidak berarti keduanya terhubung.</p>
 *
 * <p>Proses pemensiunan yang benar-benar berjalan menempuh jalur lain: layar usulan dan pemrosesan
 * pegawai pensiun mengubah langsung field status pegawai pada {@code Pegawai}/{@code Dosen}/
 * {@code Guru} memakai nilai konstanta pensiun dari {@code ConstantValues}, tanpa membentuk baris di
 * {@code employ.pensiun} dan tanpa melewati alur SOP. Akibatnya <b>tidak ada berkas pensiun yang
 * tersimpan</b> sebagai jejak: nomor surat usul, tanggal SK, golongan terakhir, dan jenis pensiun
 * yang disediakan entity ini tidak terekam di mana pun untuk pegawai yang dipensiunkan lewat jalur
 * tersebut.</p>
 *
 * <p>Konsekuensi praktis: tabel {@code employ.pensiun} kemungkinan besar kosong atau hanya berisi
 * data warisan. Kelas ini tetap dipertahankan karena pemetaannya masih terdaftar pada konfigurasi
 * Hibernate — menghapusnya berarti menyentuh skema — tetapi jangan menganggapnya sumber kebenaran
 * status pensiun pegawai. Bila suatu saat berkas pensiun hendak benar-benar dicatat, entity inilah
 * tempatnya, dan alur pemensiunan yang berjalan perlu diarahkan ke sini lebih dahulu.</p>
 *
 * @see GeneralValueObject
 * @see JenisPensiun
 * @see MutasiPindah
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "employ", name = "pensiun")
public class Pensiun extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilai ini dipakai bersama oleh banyak entity paket
	 * {@code employ} karena berkas-berkasnya disalin dari template yang sama; angkanya tidak memiliki
	 * makna bisnis dan tidak boleh diubah tanpa alasan, sebab perubahan akan mematahkan deserialisasi
	 * state ZK/HTTP session dari versi aplikasi sebelumnya.
	 */
	private static final long serialVersionUID = 1129196121609467759L;

	/** Kunci utama baris {@code employ.pensiun}; diisi database (IDENTITY). */
	private Long id;
	/** Nama/identitas petugas terakhir yang menyimpan berkas ini -- jejak audit tampilan. */
	private String oleh;
	/** Id pengguna terakhir yang menyimpan berkas ini -- jejak audit yang dapat ditelusuri balik. */
	private String olehId;

	/**
	 * Id pengguna yang terakhir menyimpan berkas pensiun ini, terpisah dari {@link #getOleh()} yang
	 * menyimpan nama tampilan. Dapat {@code null} untuk baris warisan maupun baris yang disimpan
	 * proses batch tanpa konteks pengguna.
	 *
	 * @return id pengguna penyimpan terakhir, atau {@code null} bila tidak tercatat
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Setter {@link #getOlehId()}. Mengabaikan (tidak menimpa nilai lama) bila masukan
	 * {@code null}/kosong-setelah-trim -- pola pengaman umum di entity domain kepegawaian agar jejak
	 * audit "olehId" tidak pernah ditimpa kosong. Setter ini karena itu bersifat satu arah: nilai
	 * yang sudah terisi tidak dapat dikosongkan kembali lewat jalur ini.
	 *
	 * @param olehId id pengguna penyimpan; {@code null}/kosong diabaikan diam-diam
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/** Keterangan bebas untuk berkas pensiun ini; juga menjadi label {@link #toString()}. */
	private String keterangan;
	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil otomatis Hibernate tepat sebelum tiap {@code UPDATE}
	 * baris ini, mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah}
	 * yang menyetel {@link #tanggal_dirubah} ke waktu saat itu. Deklarasi one-liner mengikuti gaya
	 * berkas hbm2java asli (tidak dirapikan agar diff minimal terhadap riwayat SVN).
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Kunci utama baris {@code employ.pensiun}.
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
	 * Setter {@link #getOleh()} -- pola pengaman sama dengan {@link #setOlehId(String)}: masukan
	 * {@code null}/kosong-setelah-trim diabaikan sehingga nama penyimpan lama tidak tertimpa kosong.
	 *
	 * @param oleh nama penyimpan; {@code null}/kosong diabaikan diam-diam
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama/identitas petugas yang terakhir menyimpan berkas pensiun ini -- jejak audit untuk
	 * ditampilkan di layar. Untuk penelusuran teknis gunakan {@link #getOlehId()}.
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
	 * aplikasi) dan diperbarui otomatis oleh {@link #onUpdate()}.
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
	 * baris kosong. Pola yang sama dipakai {@link MutasiPindah} dan entity berkas lain di paket ini.
	 *
	 * @return keterangan berkas ini, atau {@code null} bila belum diisi
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Keterangan bebas mengenai berkas pensiun ini. Boleh {@code null}. Perhatikan bahwa nilai ini
	 * juga dipakai sebagai hasil {@link #toString()}.
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

	/**
	 * Nilai {@link #getStatus()} yang menandakan usulan pensiun sudah disetujui.
	 *
	 * <p><b>Konstanta ini tidak pernah dipakai.</b> Penelusuran seluruh kode sumber tidak menemukan
	 * satu pun pembacaan maupun penulisan {@code Pensiun.DISETUJUI}, dan tidak ada kode yang memanggil
	 * {@code setStatus} pada entity ini. Tidak ada titik persetujuan berkas pensiun — karena itu
	 * juga tidak ada pemeriksaan hak atau kepemilikan yang bisa diperiksa di sini. Konstanta
	 * dipertahankan sebagai dokumentasi rancangan yang belum terwujud; siapa pun yang kelak
	 * menghidupkan alur pensiun harus menambahkan gerbang persetujuannya sendiri, termasuk pemisahan
	 * antara pengusul dan penyetuju.</p>
	 */
	public static final String DISETUJUI = "DISETUJUI";
	/**
	 * Nilai {@link #getStatus()} yang menandakan usulan pensiun belum diproses. Sama seperti
	 * {@link #DISETUJUI}, konstanta ini tidak pernah dirujuk kode mana pun.
	 */
	public static final String BELUM_DIPROSES = "BELUM DIPROSES";

	/** Pegawai pemilik berkas pensiun ini; relasi wajib -- lihat {@link #getPegawai()}. */
	private Pegawai pegawai;
	/** Nomor surat usul pensiun. */
	private String noSuratUsul;
	/** Tanggal surat usul pensiun. */
	private Date tanggalSuratUsul;
	/** Jenis/dasar pensiun; relasi wajib ke master {@link JenisPensiun}. */
	private JenisPensiun jenisPensiun;
	/** Golongan terakhir pegawai saat pensiun; relasi wajib. */
	private Golongan golonganTerakhir;
	/** Tanggal pensiun menurut berkas (perhatikan salah ketik nama kolom -- lihat getter). */
	private Date tanggalPensiun;
	/** Terhitung mulai tanggal berlakunya pensiun. */
	private Date tmtPensiun;
	/** Status berkas dalam bentuk teks bebas; tidak pernah ditulis kode mana pun. */
	private String status;

	/**
	 * Pegawai pemilik berkas pensiun ini. <b>Getter ini bukan pembaca murni</b> — ia mengubah state
	 * objek, dan perilakunya perlu dipahami sebelum dipakai.
	 *
	 * <p>Dua hal terjadi sebelum nilai dikembalikan. Pertama, referensi dilewatkan
	 * {@code GeneralValueObject.check(..)} yang meresolusi proxy lazy: bila objek yang dipegang masih
	 * berupa proxy yang belum ter-inisialisasi dan session pembuatnya sudah tertutup, helper tersebut
	 * berusaha menggantinya dengan objek nyata — dari peta identitas entity, dari cache, atau dengan
	 * membuka session baru dan memuat ulang berdasarkan id. Hasil resolusi <b>ditulis balik</b> ke
	 * field, sehingga pemanggilan berikutnya sudah menerima objek yang sama. Tujuannya menghindari
	 * kegagalan pemuatan lazy pada objek yang sudah lepas dari session; harganya adalah getter yang
	 * memiliki efek samping dan karenanya tidak aman dipanggil dari banyak thread sekaligus atas satu
	 * instance yang sama.</p>
	 *
	 * <p>Kedua — dan ini yang jauh lebih berbahaya — bila setelah resolusi nilainya <b>tetap
	 * {@code null}</b>, getter mengisi field dengan pegawai milik <b>pengguna yang sedang login</b>.
	 * Pengganti ini bukan sekadar nilai kembalian sementara: karena entity dipetakan lewat akses
	 * properti (anotasi menempel pada getter), Hibernate memanggil getter yang sama ketika memeriksa
	 * perubahan sebelum menulis. Berkas pensiun yang kehilangan referensi pegawainya karena itu dapat
	 * <b>tersimpan atas nama pembacanya</b>, bukan atas nama pemilik aslinya. Kolomnya
	 * {@code nullable = false}, sehingga substitusi ini juga menyamarkan data yang seharusnya ditolak
	 * database. Pemanggil yang ingin memeriksa apakah sebuah berkas benar-benar punya pemilik tidak
	 * boleh bersandar pada getter ini; periksa langsung ke database atau ke nilai kolomnya.</p>
	 *
	 * <p>Kegagalan saat mengambil pengguna aktif — misalnya karena getter dipanggil dari utas latar
	 * yang tidak punya konteks sesi — ditangkap dan dicatat ke audit error, lalu field dibiarkan
	 * {@code null}. Artinya perilaku getter ini berbeda tergantung ada tidaknya sesi pengguna, dan
	 * hasil yang sama tidak dapat diandalkan di konteks web maupun batch.</p>
	 *
	 * <p>Relasinya {@code @ManyToOne} dengan pemuatan lazy dan cascade {@code PERSIST}/{@code MERGE},
	 * jadi menyimpan berkas ini ikut menyimpan perubahan pada objek pegawai yang tertaut.</p>
	 *
	 * @return pegawai pemilik berkas, hasil substitusi pengguna aktif bila referensi aslinya kosong,
	 *         atau {@code null} bila substitusi pun tidak memungkinkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = false)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);

		try {
			if (pegawai == null) {
				pegawai = Common.getCurrentUser().getPegawai();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/employ/Pensiun.java:118");

		}

		return pegawai;
	}

	/**
	 * Setter {@link #getPegawai()}. Menyimpan referensi apa adanya, termasuk {@code null} -- namun
	 * perlu diingat bahwa menyetel {@code null} tidak benar-benar mengosongkan relasi, karena
	 * getter-nya akan menggantinya dengan pegawai pengguna aktif pada pembacaan berikutnya.
	 *
	 * @param pegawai pegawai pemilik berkas pensiun
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Nomor surat usul pensiun, yaitu nomor surat yang mengajukan pemensiunan pegawai ini. Teks bebas
	 * tanpa format yang ditegakkan; boleh {@code null}.
	 *
	 * @return nomor surat usul, atau {@code null} bila belum diisi
	 */
	@Column(name = "no_surat_usul")
	public String getNoSuratUsul() {
		return noSuratUsul;
	}

	/**
	 * Setter {@link #getNoSuratUsul()}.
	 *
	 * @param noSuratUsul nomor surat usul; boleh {@code null}
	 */
	public void setNoSuratUsul(String noSuratUsul) {
		this.noSuratUsul = noSuratUsul;
	}

	/**
	 * Tanggal surat usul pensiun. Tanpa anotasi {@code @Temporal}, sehingga dipetakan mengikuti
	 * default penyedia persistence untuk {@link Date} (cap waktu lengkap, bukan tanggal saja) --
	 * berbeda dengan beberapa field tanggal lain di paket ini yang menyatakannya eksplisit.
	 *
	 * @return tanggal surat usul, atau {@code null} bila belum diisi
	 */
	@Column(name = "tanggal_surat_usul")
	public Date getTanggalSuratUsul() {
		return tanggalSuratUsul;
	}

	/**
	 * Setter {@link #getTanggalSuratUsul()}.
	 *
	 * @param tanggalSuratUsul tanggal surat usul; boleh {@code null}
	 */
	public void setTanggalSuratUsul(Date tanggalSuratUsul) {
		this.tanggalSuratUsul = tanggalSuratUsul;
	}

	/**
	 * Jenis/dasar pensiun, merujuk baris master {@link JenisPensiun} -- misalnya batas usia pensiun,
	 * atas permintaan sendiri, atau meninggal dunia.
	 *
	 * <p>Kolomnya {@code nullable = false}, jadi setiap berkas pensiun wajib menyebut jenisnya.
	 * Relasi memakai {@code FetchMode.SELECT} sehingga baris master diambil lewat query terpisah,
	 * bukan digabung ke query utama. Berbeda dengan {@link #getPegawai()}, getter ini pembaca murni:
	 * tidak ada resolusi lazy maupun substitusi nilai default.</p>
	 *
	 * @return jenis pensiun, atau {@code null} pada objek yang belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jenisPensiun", nullable = false)
	public JenisPensiun getJenisPensiun() {
		return jenisPensiun;
	}

	/**
	 * Setter {@link #getJenisPensiun()}. Nilai wajib diisi sebelum penyimpanan karena kolomnya
	 * {@code nullable = false}.
	 *
	 * @param jenisPensiun jenis/dasar pensiun
	 */
	public void setJenisPensiun(JenisPensiun jenisPensiun) {
		this.jenisPensiun = jenisPensiun;
	}

	/**
	 * Golongan terakhir yang dipangku pegawai saat pensiun -- disimpan sebagai <b>salinan referensi
	 * pada berkas</b>, bukan dihitung ulang dari riwayat kepangkatan. Dengan begitu berkas pensiun
	 * tetap menunjukkan golongan yang berlaku saat itu meski data kepangkatan berubah kemudian.
	 *
	 * <p>Kolomnya {@code nullable = false} sehingga wajib diisi. Getter ini pembaca murni tanpa
	 * resolusi lazy.</p>
	 *
	 * @return golongan terakhir, atau {@code null} pada objek yang belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "golongan_terakhir", nullable = false)
	public Golongan getGolonganTerakhir() {
		return golonganTerakhir;
	}

	/**
	 * Setter {@link #getGolonganTerakhir()}. Nilai wajib diisi sebelum penyimpanan karena kolomnya
	 * {@code nullable = false}.
	 *
	 * @param golonganTerakhir golongan terakhir pegawai
	 */
	public void setGolonganTerakhir(Golongan golonganTerakhir) {
		this.golonganTerakhir = golonganTerakhir;
	}

	/**
	 * Tanggal pensiun menurut berkas ini.
	 *
	 * <p><b>Perhatikan nama kolomnya:</b> {@code tanggal_pesiun}, dengan huruf "n" yang hilang. Salah
	 * ketik ini sudah terlanjur menjadi nama kolom di database dan sengaja tidak diperbaiki -- setiap
	 * query SQL langsung, laporan, atau skrip migrasi yang menyentuh kolom ini harus memakai ejaan
	 * yang keliru tersebut, bukan ejaan yang benar. {@link MutasiPindah#getTanggalPensiun()} memakai
	 * nama kolom yang sama persis.</p>
	 *
	 * @return tanggal pensiun, atau {@code null} bila belum diisi
	 */
	@Column(name = "tanggal_pesiun")
	public Date getTanggalPensiun() {
		return tanggalPensiun;
	}

	/**
	 * Setter {@link #getTanggalPensiun()}.
	 *
	 * @param tanggalPensiun tanggal pensiun; boleh {@code null}
	 */
	public void setTanggalPensiun(Date tanggalPensiun) {
		this.tanggalPensiun = tanggalPensiun;
	}

	/**
	 * Terhitung mulai tanggal (TMT) berlakunya pensiun, yaitu saat pegawai resmi berstatus pensiun.
	 * Dibedakan dari {@link #getTanggalPensiun()} yang mencatat tanggal pensiun menurut berkas; dalam
	 * praktik administrasi kepegawaian keduanya dapat berbeda, misalnya ketika SK terbit setelah
	 * tanggal berlakunya.
	 *
	 * @return tanggal mulai berlaku pensiun, atau {@code null} bila belum diisi
	 */
	@Column(name = "tmt_pensiun")
	public Date getTmtPensiun() {
		return tmtPensiun;
	}

	/**
	 * Setter {@link #getTmtPensiun()}.
	 *
	 * @param tmtPensiun tanggal mulai berlaku pensiun; boleh {@code null}
	 */
	public void setTmtPensiun(Date tmtPensiun) {
		this.tmtPensiun = tmtPensiun;
	}

	/**
	 * Status pemrosesan berkas pensiun, dirancang untuk diisi salah satu dari {@link #DISETUJUI} atau
	 * {@link #BELUM_DIPROSES}.
	 *
	 * <p><b>Tidak ada kode yang menulis field ini</b>, sebagaimana dijelaskan pada {@link #DISETUJUI}.
	 * Kolomnya bertipe teks bebas tanpa batasan nilai di tingkat database, sehingga bila kelak diisi,
	 * pemanggil sendiri yang harus menjaga agar isinya terbatas pada dua konstanta di atas —
	 * perbandingan berbasis teks seperti ini mudah rusak oleh perbedaan huruf besar/kecil atau spasi
	 * berlebih.</p>
	 *
	 * @return status berkas, atau {@code null} -- yang merupakan keadaan normal saat ini
	 */
	@Column(name = "status")
	public String getStatus() {
		return status;
	}

	/**
	 * Setter {@link #getStatus()}. Menerima teks apa adanya tanpa memeriksa bahwa nilainya termasuk
	 * salah satu konstanta yang dikenal, dan tanpa pemeriksaan hak akses apa pun -- gerbang
	 * persetujuan, bila kelak dibutuhkan, harus dibangun di lapisan Action/service.
	 *
	 * @param status status berkas; boleh {@code null}
	 */
	public void setStatus(String status) {
		this.status = status;
	}

}
