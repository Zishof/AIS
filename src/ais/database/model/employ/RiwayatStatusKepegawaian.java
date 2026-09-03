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
 * Model data untuk riwayat status kepegawaian. Tipe ini membawa state yang dipertukarkan oleh
 * lapisan persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi
 * yang dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code String keterangan}, {@code String CPNS}, {@code String PNS}, {@code Date
 * tanggal_dirubah}, {@code Pegawai pegawai}; pemetaan persistence: tabel {@code
 * employ.riwayat_status_kepegawaian}; pembacaan/pencarian ({@code getOlehId()}, {@code getId()}, {@code
 * getOleh()}, {@code getTanggal_dirubah()}, {@code getKeterangan()}, {@code getPegawai()}); mutasi data ({@code
 * setOlehId()}, {@code setId()}, {@code setOleh()}, {@code onUpdate()}, {@code setTanggal_dirubah()}, {@code
 * setKeterangan()}); operasi domain lain ({@code toString()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * <h2>Apa yang dimaksud "status kepegawaian" di sini</h2>
 * <p>Perlu ditegaskan lebih dahulu karena istilahnya mudah disalahartikan: status yang dicatat entity
 * ini <b>bukan</b> keadaan aktif/cuti/nonaktif/pensiun seorang pegawai. Yang dicatat adalah
 * <b>kedudukan pengangkatan</b> dalam pengertian kepegawaian negeri, yaitu {@link #CPNS} (calon
 * pegawai negeri sipil) atau {@link #PNS} (pegawai negeri sipil penuh) — dua nilai itulah yang
 * ditawarkan layar pengelolanya sebagai pilihan radio. Keadaan aktif atau tidaknya seorang pegawai
 * disimpan terpisah pada {@code ais.database.model.Pegawai} dan tidak ada hubungannya dengan tabel
 * ini.</p>
 *
 * <h2>Snapshot, bukan nilai turunan</h2>
 * <p>Seluruh field di sini adalah <b>rekaman apa adanya</b> (snapshot) dari satu peristiwa
 * pengangkatan: tidak satu pun dihitung ulang saat dibaca, dan tidak ada getter yang menurunkan
 * nilainya dari data lain. Satu baris mewakili satu SK pengangkatan, lengkap dengan nomor dan
 * tanggal SK, tanggal SK dari BKN, pejabat penanda tangan beserta NIP-nya, TMT, TMT masa percobaan,
 * golongan serta jabatan yang menyertainya, tahun anggaran, nomor dan tanggal surat keterangan uji
 * kesehatan, nomor dan tanggal STTPL (surat tanda tamat pendidikan dan pelatihan), serta uraian
 * tugas. Riwayat lengkap seorang pegawai terbentuk dari kumpulan baris semacam ini, dan urutan
 * kronologisnya harus disusun pemanggil dari {@link #getTmt()} atau {@link #getTanggalSK()} — entity
 * ini tidak menyediakan penanda "baris terkini" maupun urutan bawaan.</p>
 *
 * <p>Golongan dan jabatan yang disalin ke sini <b>tidak</b> menjadi golongan/jabatan pegawai yang
 * berlaku. Nilai yang berlaku diturunkan {@code Pegawai} dari berkas {@link KenaikanPangkat};
 * riwayat status kepegawaian hanya dibaca sebagai <b>cadangan</b> untuk menentukan pangkat awal
 * ketika seorang pegawai belum memiliki berkas kenaikan pangkat sama sekali — perilaku yang dipakai
 * layar riwayat kenaikan pangkat/golongan. Dua sumber data ini dapat berbeda isi tanpa ada mekanisme
 * yang menyelaraskannya.</p>
 *
 * <p><b>Catatan pemetaan:</b> {@link #getStatusKepegawaian()} dan {@link #getJenisPegawai()}
 * disimpan sebagai teks bebas, bukan relasi ke tabel master — termasuk ke entity master
 * {@code ais.database.model.StatusKepegawaian} yang berdiri sendiri dan sama sekali tidak tertaut ke
 * sini. Konsistensi nilainya sepenuhnya bergantung pada layar pengisi.</p>
 *
 * @see GeneralValueObject
 * @see KenaikanPangkat
 * @see Golongan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "employ", name = "riwayat_status_kepegawaian")

public class RiwayatStatusKepegawaian extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilai ini dipakai bersama oleh banyak entity paket
	 * {@code employ} karena berkas-berkasnya disalin dari template yang sama; angkanya tidak memiliki
	 * makna bisnis dan tidak boleh diubah tanpa alasan, sebab perubahan akan mematahkan deserialisasi
	 * state ZK/HTTP session dari versi aplikasi sebelumnya.
	 */
	private static final long serialVersionUID = 1129196121609467759L;

	/** Kunci utama baris {@code employ.riwayat_status_kepegawaian}; diisi database (IDENTITY). */
	private Long id;
	/** Nama/identitas petugas terakhir yang menyimpan baris ini -- jejak audit tampilan. */
	private String oleh;
	/** Id pengguna terakhir yang menyimpan baris ini -- jejak audit yang dapat ditelusuri balik. */
	private String olehId;

	/**
	 * Id pengguna yang terakhir menyimpan baris riwayat ini, terpisah dari {@link #getOleh()} yang
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

	/** Keterangan bebas untuk baris riwayat ini; juga menjadi label {@link #toString()}. */
	private String keterangan;
	/**
	 * Nilai {@link #getStatusKepegawaian()} untuk <b>calon pegawai negeri sipil</b>, yaitu kedudukan
	 * pengangkatan tahap awal sebelum diangkat penuh.
	 *
	 * <p>Dipakai layar pengelola riwayat status kepegawaian sebagai nilai salah satu dari dua pilihan
	 * radio, dan dibandingkan kembali dengan {@code equals} saat memuat baris untuk menentukan radio
	 * mana yang tersorot. Karena kolomnya teks bebas, perbandingan itu peka terhadap perbedaan huruf
	 * besar/kecil maupun spasi berlebih; nilai yang masuk lewat jalur lain (impor, SQL langsung) dan
	 * tidak persis sama akan diperlakukan seolah bukan CPNS.</p>
	 */
	public static final String CPNS = "CPNS";
	/**
	 * Nilai {@link #getStatusKepegawaian()} untuk <b>pegawai negeri sipil</b> penuh. Pasangan
	 * {@link #CPNS}; berlaku catatan perbandingan teks yang sama.
	 */
	public static final String PNS = "PNS";

	/**
	 * Kunci utama baris {@code employ.riwayat_status_kepegawaian}.
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
	 * Nama/identitas petugas yang terakhir menyimpan baris riwayat ini -- jejak audit untuk
	 * ditampilkan di layar. Untuk penelusuran teknis gunakan {@link #getOlehId()}.
	 *
	 * @return nama penyimpan terakhir, atau {@code null} bila tidak tercatat
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil otomatis Hibernate tepat sebelum tiap {@code UPDATE}
	 * baris ini, mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah}
	 * yang menyetel {@link #tanggal_dirubah} ke waktu saat itu. Deklarasi one-liner mengikuti gaya
	 * berkas hbm2java asli (tidak dirapikan agar diff minimal terhadap riwayat SVN).
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

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
	 * {@link #getTmt()} yang merupakan tanggal berlakunya pengangkatan; yang ini murni cap waktu
	 * teknis kapan barisnya disunting.
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
	 * @return keterangan baris ini, atau {@code null} bila belum diisi
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Keterangan bebas mengenai baris riwayat ini. Boleh {@code null}. Perhatikan bahwa nilai ini
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

	/** Pegawai pemilik riwayat ini; relasi wajib -- lihat {@link #getPegawai()}. */
	private Pegawai pegawai;
	/** Kedudukan pengangkatan: {@link #CPNS} atau {@link #PNS}; teks bebas, wajib terisi. */
	private String statusKepegawaian;
	/** Jenis pegawai sebagai teks bebas; bukan relasi ke {@link TipePegawai}. */
	private String jenisPegawai;
	/** Nomor SK pengangkatan; wajib terisi. */
	private String noSK;
	/** Tanggal SK pengangkatan; wajib terisi. */
	private Date tanggalSK;
	/** Terhitung mulai tanggal berlakunya pengangkatan; wajib terisi. */
	private Date tmt;
	/** Golongan yang menyertai pengangkatan ini; wajib terisi. */
	private Golongan golongan;
	/** Jabatan struktural yang menyertai pengangkatan ini; boleh kosong. */
	private JabatanStruktural jabatanStruktural;
	/** Jabatan fungsional yang menyertai pengangkatan ini; boleh kosong. */
	private JabatanFungsional jabatanFungsional;
	/** Tahun anggaran pengangkatan, disimpan sebagai teks. */
	private String tahunAnggaran;

	/** Tanggal SK/persetujuan teknis dari BKN. */
	private Date tanggalSKBKN;
	/** Nama pejabat penanda tangan SK. */
	private String sKPejabat;
	/** NIP pejabat penanda tangan SK. */
	private String sKPejabatNIP;
	/** Terhitung mulai tanggal masa percobaan. */
	private Date tmtCoba;
	/** Nomor surat keterangan uji kesehatan. */
	private String noUjiSehat;
	/** Tanggal surat keterangan uji kesehatan. */
	private Date tanggalUjiSehat;
	/** Nomor STTPL (surat tanda tamat pendidikan dan pelatihan). */
	private String noSTTPL;
	/** Tanggal STTPL. */
	private Date tanggalSTTPL;
	/** Uraian tugas yang menyertai pengangkatan ini. */
	private String tugas;

	/**
	 * Pegawai pemilik baris riwayat ini. <b>Getter ini bukan pembaca murni</b> — ia mengubah state
	 * objek, dan perilakunya perlu dipahami sebelum dipakai.
	 *
	 * <p>Dua hal terjadi sebelum nilai dikembalikan. Pertama, referensi dilewatkan
	 * {@code GeneralValueObject.check(..)} yang meresolusi proxy lazy: bila objek yang dipegang masih
	 * berupa proxy yang belum ter-inisialisasi dan session pembuatnya sudah tertutup, helper tersebut
	 * berusaha menggantinya dengan objek nyata — dari peta identitas entity, dari cache, atau dengan
	 * membuka session baru dan memuat ulang berdasarkan id. Hasil resolusi <b>ditulis balik</b> ke
	 * field. Tujuannya menghindari kegagalan pemuatan lazy pada objek yang sudah lepas dari session;
	 * harganya adalah getter dengan efek samping, yang karenanya tidak aman dipanggil dari banyak
	 * thread sekaligus atas satu instance.</p>
	 *
	 * <p>Kedua — dan ini yang jauh lebih berbahaya — bila setelah resolusi nilainya <b>tetap
	 * {@code null}</b>, getter mengisi field dengan pegawai milik <b>pengguna yang sedang login</b>.
	 * Pengganti ini bukan sekadar nilai kembalian sementara: karena entity dipetakan lewat akses
	 * properti (anotasi menempel pada getter), Hibernate memanggil getter yang sama ketika memeriksa
	 * perubahan sebelum menulis. Baris riwayat yang kehilangan referensi pegawainya karena itu dapat
	 * <b>berpindah kepemilikan ke pembacanya</b> — pada entity ini akibatnya serius, sebab yang
	 * berpindah adalah rekaman SK pengangkatan seseorang. Kolomnya {@code nullable = false}, sehingga
	 * substitusi ini sekaligus menyamarkan data yang seharusnya ditolak database.</p>
	 *
	 * <p>Kegagalan mengambil pengguna aktif — misalnya karena getter dipanggil dari utas latar tanpa
	 * konteks sesi — ditangkap dan dicatat ke audit error, lalu field dibiarkan {@code null}.
	 * Perilaku getter ini dengan demikian berbeda antara konteks web dan konteks batch.</p>
	 *
	 * <p>Relasinya {@code @ManyToOne} dengan pemuatan lazy dan cascade {@code PERSIST}/{@code MERGE},
	 * jadi menyimpan baris ini ikut menyimpan perubahan pada objek pegawai yang tertaut.</p>
	 *
	 * @return pegawai pemilik riwayat, hasil substitusi pengguna aktif bila referensi aslinya kosong,
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
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/employ/RiwayatStatusKepegawaian.java:131");

		}

		return pegawai;
	}

	/**
	 * Setter {@link #getPegawai()}. Menyimpan referensi apa adanya, termasuk {@code null} -- namun
	 * perlu diingat bahwa menyetel {@code null} tidak benar-benar mengosongkan relasi, karena
	 * getter-nya akan menggantinya dengan pegawai pengguna aktif pada pembacaan berikutnya.
	 *
	 * @param pegawai pegawai pemilik riwayat
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Kedudukan pengangkatan yang dicatat baris ini: {@link #CPNS} atau {@link #PNS}. Sekali lagi
	 * perlu ditegaskan bahwa ini <b>bukan</b> keadaan aktif/nonaktif pegawai.
	 *
	 * <p>Disimpan sebagai teks bebas dengan kolom {@code nullable = false}, tanpa batasan nilai di
	 * tingkat database dan tanpa relasi ke tabel master mana pun -- termasuk ke entity
	 * {@code ais.database.model.StatusKepegawaian} yang berdiri sendiri dan tidak tertaut ke sini.
	 * Yang menjaga agar isinya terbatas pada dua konstanta itu hanyalah layar pengelola, yang
	 * menyajikannya sebagai sepasang radio. Nilai yang masuk lewat impor atau SQL langsung tidak
	 * tersaring, dan karena pembacaannya kembali memakai perbandingan {@code equals} yang peka huruf
	 * besar/kecil, nilai seperti {@code "Pns"} akan diperlakukan sebagai bukan PNS maupun CPNS.</p>
	 *
	 * @return kedudukan pengangkatan; normalnya {@link #CPNS} atau {@link #PNS}
	 */
	@Column(name = "status_kepegawaian", nullable = false)
	public String getStatusKepegawaian() {
		return statusKepegawaian;
	}

	/**
	 * Setter {@link #getStatusKepegawaian()}. Menerima teks apa adanya tanpa memeriksa bahwa nilainya
	 * salah satu dari {@link #CPNS} atau {@link #PNS}.
	 *
	 * @param statusKepegawaian kedudukan pengangkatan; wajib terisi sebelum penyimpanan
	 */
	public void setStatusKepegawaian(String statusKepegawaian) {
		this.statusKepegawaian = statusKepegawaian;
	}

	/**
	 * Jenis pegawai yang menyertai pengangkatan ini, disimpan sebagai teks bebas dan boleh
	 * {@code null}.
	 *
	 * <p>Perhatikan bahwa ini <b>bukan</b> relasi ke master {@link TipePegawai}: nilainya berupa
	 * salinan teks, sehingga penggantian nama pada tabel master tidak terbawa ke sini dan pencocokan
	 * antara keduanya harus dilakukan berdasarkan teks. Untuk mengetahui tipe pegawai yang berlaku
	 * saat ini, baca relasi pada {@code Pegawai}, bukan kolom ini.</p>
	 *
	 * @return jenis pegawai sebagai teks, atau {@code null} bila tidak diisi
	 */
	@Column(name = "jenis_pegawai", nullable = true)
	public String getJenisPegawai() {
		return jenisPegawai;
	}

	/**
	 * Setter {@link #getJenisPegawai()}.
	 *
	 * @param jenisPegawai jenis pegawai sebagai teks; boleh {@code null}
	 */
	public void setJenisPegawai(String jenisPegawai) {
		this.jenisPegawai = jenisPegawai;
	}

	/**
	 * Nomor SK pengangkatan yang mendasari baris riwayat ini. Kolomnya {@code nullable = false}
	 * sehingga wajib terisi -- setiap perubahan kedudukan kepegawaian harus punya dasar surat
	 * keputusan. Perhatikan penamaan kolomnya, {@code no_SK}, yang memakai huruf besar dan berbeda
	 * dari kebiasaan huruf kecil di kolom lain.
	 *
	 * @return nomor SK; {@code null} hanya pada objek yang belum diisi
	 */
	@Column(name = "no_SK", nullable = false)
	public String getNoSK() {
		return noSK;
	}

	/**
	 * Setter {@link #getNoSK()}. Nilai wajib diisi sebelum penyimpanan karena kolomnya
	 * {@code nullable = false}.
	 *
	 * @param noSK nomor SK pengangkatan
	 */
	public void setNoSK(String noSK) {
		this.noSK = noSK;
	}

	/**
	 * Tanggal SK pengangkatan, yaitu kapan surat keputusan diterbitkan. Kolomnya
	 * {@code nullable = false}. Dibedakan dari {@link #getTmt()} yang mencatat kapan pengangkatan
	 * mulai berlaku; SK lazim terbit setelah TMT-nya.
	 *
	 * @return tanggal SK; {@code null} hanya pada objek yang belum diisi
	 */
	@Column(name = "tgl_SK", nullable = false)
	public Date getTanggalSK() {
		return tanggalSK;
	}

	/**
	 * Setter {@link #getTanggalSK()}. Nilai wajib diisi sebelum penyimpanan karena kolomnya
	 * {@code nullable = false}.
	 *
	 * @param tanggalSK tanggal terbit SK pengangkatan
	 */
	public void setTanggalSK(Date tanggalSK) {
		this.tanggalSK = tanggalSK;
	}

	/**
	 * Terhitung mulai tanggal (TMT), yaitu saat kedudukan kepegawaian pada baris ini <b>mulai
	 * berlaku</b>. Kolomnya {@code nullable = false}.
	 *
	 * <p>Field inilah yang menjadi sumbu waktu riwayat: untuk mengetahui kedudukan seorang pegawai
	 * pada suatu saat, pemanggil perlu mengurutkan baris-baris riwayatnya menurut TMT dan mengambil
	 * yang terakhir tidak melewati saat tersebut. Entity ini tidak menyediakan bantuan apa pun untuk
	 * itu -- tidak ada penanda "terkini", tidak ada tanggal berakhir, dan tidak ada pemeriksaan
	 * bahwa TMT antarbaris tidak saling tumpang tindih atau berurutan mundur.</p>
	 *
	 * @return tanggal mulai berlaku pengangkatan; {@code null} hanya pada objek yang belum diisi
	 */
	@Column(name = "tmt", nullable = false)
	public Date getTmt() {
		return tmt;
	}

	/**
	 * Setter {@link #getTmt()}. Nilai wajib diisi sebelum penyimpanan karena kolomnya
	 * {@code nullable = false}.
	 *
	 * @param tmt tanggal mulai berlaku pengangkatan
	 */
	public void setTmt(Date tmt) {
		this.tmt = tmt;
	}

	/**
	 * Golongan/pangkat yang menyertai pengangkatan pada baris ini, disimpan sebagai salinan referensi
	 * agar riwayat tetap terbaca meski golongan pegawai berubah kemudian. Kolomnya
	 * {@code nullable = false}.
	 *
	 * <p>Nilai ini <b>bukan</b> golongan pegawai yang berlaku: golongan berjalan diturunkan
	 * {@code Pegawai} dari berkas {@link KenaikanPangkat}. Riwayat status kepegawaian hanya dibaca
	 * sebagai cadangan penentu pangkat awal ketika pegawai belum punya berkas kenaikan pangkat sama
	 * sekali. Karena tidak ada mekanisme penyelaras, kedua sumber dapat berbeda isi.</p>
	 *
	 * <p>Relasi memakai {@code FetchMode.SELECT} sehingga diambil lewat query terpisah; getter ini
	 * pembaca murni tanpa resolusi lazy maupun substitusi.</p>
	 *
	 * @return golongan pada saat pengangkatan; {@code null} hanya pada objek yang belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "golongan", nullable = false)
	public Golongan getGolongan() {
		return golongan;
	}

	/**
	 * Setter {@link #getGolongan()}. Nilai wajib diisi sebelum penyimpanan karena kolomnya
	 * {@code nullable = false}.
	 *
	 * @param golongan golongan pada saat pengangkatan
	 */
	public void setGolongan(Golongan golongan) {
		this.golongan = golongan;
	}

	/**
	 * Jabatan struktural yang menyertai pengangkatan pada baris ini, disimpan sebagai salinan
	 * referensi. Boleh {@code null} -- pengangkatan tidak selalu disertai jabatan struktural, dan
	 * seorang pegawai lazimnya hanya memangku salah satu dari jabatan struktural atau
	 * {@link #getJabatanFungsional() fungsional}. Tidak ada pemeriksaan di tingkat model yang
	 * mencegah keduanya terisi sekaligus.
	 *
	 * @return jabatan struktural saat pengangkatan, atau {@code null} bila tidak ada
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jabatan_struktural")
	public JabatanStruktural getJabatanStruktural() {
		return jabatanStruktural;
	}

	/**
	 * Setter {@link #getJabatanStruktural()}.
	 *
	 * @param jabatanStruktural jabatan struktural saat pengangkatan; boleh {@code null}
	 */
	public void setJabatanStruktural(JabatanStruktural jabatanStruktural) {
		this.jabatanStruktural = jabatanStruktural;
	}

	/**
	 * Jabatan fungsional yang menyertai pengangkatan pada baris ini, disimpan sebagai salinan
	 * referensi. Boleh {@code null}. Berpasangan dengan {@link #getJabatanStruktural()}; lihat
	 * catatan di sana mengenai ketiadaan pemeriksaan bahwa hanya satu di antaranya terisi.
	 *
	 * @return jabatan fungsional saat pengangkatan, atau {@code null} bila tidak ada
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jabatan_fungsional")
	public JabatanFungsional getJabatanFungsional() {
		return jabatanFungsional;
	}

	/**
	 * Setter {@link #getJabatanFungsional()}.
	 *
	 * @param jabatanFungsional jabatan fungsional saat pengangkatan; boleh {@code null}
	 */
	public void setJabatanFungsional(JabatanFungsional jabatanFungsional) {
		this.jabatanFungsional = jabatanFungsional;
	}

	/**
	 * Tahun anggaran yang membiayai pengangkatan ini, disimpan sebagai <b>teks</b> dan bukan angka --
	 * sehingga pengurutan maupun perbandingannya bersifat leksikografis, dan nilai seperti
	 * {@code "2010"} tidak dapat dibandingkan secara numerik tanpa konversi. Boleh {@code null}.
	 *
	 * @return tahun anggaran sebagai teks, atau {@code null} bila tidak diisi
	 */
	@Column(name = "tahun_anggaran")
	public String getTahunAnggaran() {
		return tahunAnggaran;
	}

	/**
	 * Setter {@link #getTahunAnggaran()}.
	 *
	 * @param tahunAnggaran tahun anggaran sebagai teks; boleh {@code null}
	 */
	public void setTahunAnggaran(String tahunAnggaran) {
		this.tahunAnggaran = tahunAnggaran;
	}

	/**
	 * Tanggal surat persetujuan teknis atau penetapan NIP dari BKN (Badan Kepegawaian Negara) yang
	 * mendahului penerbitan SK pengangkatan. Boleh {@code null}, misalnya untuk pengangkatan yang
	 * tidak melalui jalur tersebut atau untuk data warisan yang tidak lengkap.
	 *
	 * @return tanggal SK BKN, atau {@code null} bila tidak diisi
	 */
	@Column(name = "tanggal_sk_bkn")
	public Date getTanggalSKBKN() {
		return tanggalSKBKN;
	}

	/**
	 * Setter {@link #getTanggalSKBKN()}.
	 *
	 * @param tanggalSKBKN tanggal SK BKN; boleh {@code null}
	 */
	public void setTanggalSKBKN(Date tanggalSKBKN) {
		this.tanggalSKBKN = tanggalSKBKN;
	}

	/**
	 * Nama pejabat yang menandatangani SK pengangkatan. Disimpan sebagai teks, bukan relasi ke data
	 * pegawai, sehingga tetap terbaca meski pejabat bersangkutan sudah tidak tercatat di sistem.
	 * Boleh {@code null}.
	 *
	 * <p>Perhatikan penamaan getter/setter yang diawali huruf kecil ({@code getsKPejabat}), akibat
	 * nama field yang dimulai dengan huruf kecil disusul huruf besar. Penamaan ini menyalahi
	 * konvensi JavaBean pada umumnya namun sudah terlanjur dipakai di halaman ZUL dan pemetaan
	 * Hibernate, jadi jangan diubah.</p>
	 *
	 * @return nama pejabat penanda tangan SK, atau {@code null} bila tidak diisi
	 */
	@Column(name = "sk_pejabat")
	public String getsKPejabat() {
		return sKPejabat;
	}

	/**
	 * Setter {@link #getsKPejabat()}.
	 *
	 * @param sKPejabat nama pejabat penanda tangan SK; boleh {@code null}
	 */
	public void setsKPejabat(String sKPejabat) {
		this.sKPejabat = sKPejabat;
	}

	/**
	 * NIP pejabat yang menandatangani SK pengangkatan, melengkapi {@link #getsKPejabat()}. Disimpan
	 * sebagai teks agar nol di depan tidak hilang. Boleh {@code null}.
	 *
	 * @return NIP pejabat penanda tangan SK, atau {@code null} bila tidak diisi
	 */
	@Column(name = "sk_pejabat_nip")
	public String getsKPejabatNIP() {
		return sKPejabatNIP;
	}

	/**
	 * Setter {@link #getsKPejabatNIP()}.
	 *
	 * @param sKPejabatNIP NIP pejabat penanda tangan SK; boleh {@code null}
	 */
	public void setsKPejabatNIP(String sKPejabatNIP) {
		this.sKPejabatNIP = sKPejabatNIP;
	}

	/**
	 * Terhitung mulai tanggal masa percobaan (masa prajabatan) bagi pengangkatan ini -- relevan
	 * terutama pada baris berstatus {@link #CPNS}, yang lazim menjalani masa percobaan sebelum
	 * diangkat menjadi {@link #PNS}. Boleh {@code null}.
	 *
	 * <p>Tidak ada perhitungan otomatis yang memakai tanggal ini: berakhirnya masa percobaan maupun
	 * pengangkatan menjadi PNS tetap harus dicatat operator sebagai baris riwayat baru.</p>
	 *
	 * @return tanggal mulai masa percobaan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "tmt_coba")
	public Date getTmtCoba() {
		return tmtCoba;
	}

	/**
	 * Setter {@link #getTmtCoba()}.
	 *
	 * @param tmtCoba tanggal mulai masa percobaan; boleh {@code null}
	 */
	public void setTmtCoba(Date tmtCoba) {
		this.tmtCoba = tmtCoba;
	}

	/**
	 * Nomor surat keterangan uji kesehatan yang menjadi salah satu syarat pengangkatan. Boleh
	 * {@code null}.
	 *
	 * @return nomor surat uji kesehatan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "no_uji_sehat")
	public String getNoUjiSehat() {
		return noUjiSehat;
	}

	/**
	 * Setter {@link #getNoUjiSehat()}.
	 *
	 * @param noUjiSehat nomor surat uji kesehatan; boleh {@code null}
	 */
	public void setNoUjiSehat(String noUjiSehat) {
		this.noUjiSehat = noUjiSehat;
	}

	/**
	 * Tanggal surat keterangan uji kesehatan, melengkapi {@link #getNoUjiSehat()}. Boleh
	 * {@code null}; tidak ada pemeriksaan bahwa nomor dan tanggalnya terisi bersamaan.
	 *
	 * @return tanggal surat uji kesehatan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "tanggal_uji_sehat")
	public Date getTanggalUjiSehat() {
		return tanggalUjiSehat;
	}

	/**
	 * Setter {@link #getTanggalUjiSehat()}.
	 *
	 * @param tanggalUjiSehat tanggal surat uji kesehatan; boleh {@code null}
	 */
	public void setTanggalUjiSehat(Date tanggalUjiSehat) {
		this.tanggalUjiSehat = tanggalUjiSehat;
	}

	/**
	 * Nomor STTPL (Surat Tanda Tamat Pendidikan dan Pelatihan), yaitu bukti kelulusan diklat
	 * prajabatan yang menjadi syarat pengangkatan menjadi {@link #PNS}. Boleh {@code null},
	 * khususnya pada baris berstatus {@link #CPNS} yang diklatnya belum ditempuh.
	 *
	 * @return nomor STTPL, atau {@code null} bila tidak diisi
	 */
	@Column(name = "no_sttpl")
	public String getNoSTTPL() {
		return noSTTPL;
	}

	/**
	 * Setter {@link #getNoSTTPL()}.
	 *
	 * @param noSTTPL nomor STTPL; boleh {@code null}
	 */
	public void setNoSTTPL(String noSTTPL) {
		this.noSTTPL = noSTTPL;
	}

	/**
	 * Tanggal STTPL, melengkapi {@link #getNoSTTPL()}. Boleh {@code null}; tidak ada pemeriksaan
	 * bahwa nomor dan tanggalnya terisi bersamaan.
	 *
	 * @return tanggal STTPL, atau {@code null} bila tidak diisi
	 */
	@Column(name = "tanggal_sttpl")
	public Date getTanggalSTTPL() {
		return tanggalSTTPL;
	}

	/**
	 * Setter {@link #getTanggalSTTPL()}.
	 *
	 * @param tanggalSTTPL tanggal STTPL; boleh {@code null}
	 */
	public void setTanggalSTTPL(Date tanggalSTTPL) {
		this.tanggalSTTPL = tanggalSTTPL;
	}

	/**
	 * Uraian tugas yang dibebankan lewat SK pengangkatan ini, ditulis sebagai teks bebas. Boleh
	 * {@code null}. Bersifat catatan administratif semata -- tidak ada modul yang membaca isinya
	 * untuk menurunkan beban kerja, jadwal, maupun komponen gaji.
	 *
	 * @return uraian tugas, atau {@code null} bila tidak diisi
	 */
	@Column(name = "tugas")
	public String getTugas() {
		return tugas;
	}

	/**
	 * Setter {@link #getTugas()}.
	 *
	 * @param tugas uraian tugas; boleh {@code null}
	 */
	public void setTugas(String tugas) {
		this.tugas = tugas;
	}

}
