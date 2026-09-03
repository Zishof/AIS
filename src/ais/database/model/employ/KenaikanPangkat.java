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

import org.hibernate.envers.Audited;
import org.json.JSONObject;

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jabatan;
import ais.database.model.Pegawai;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * Model data untuk kenaikan pangkat. Tipe ini membawa state yang dipertukarkan oleh lapisan
 * persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang
 * dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * DataSop}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code String keterangan}, {@code Date tanggal_dirubah}, {@code Pegawai pegawai}, {@code
 * JenisKenaikanPangkat jenisKenaikanPangkat}, {@code String noSuratUsul}; pemetaan persistence: tabel {@code
 * employ.kenaikan_pangkat}; pembacaan/pencarian ({@code getOlehId()}, {@code getId()}, {@code getOleh()}, {@code
 * getTanggal_dirubah()}, {@code getKeterangan()}, {@code getPegawai()}); mutasi data ({@code setOlehId()},
 * {@code onUpdate()}, {@code setId()}, {@code setOleh()}, {@code setTanggal_dirubah()}, {@code
 * setKeterangan()}); operasi domain lain ({@code compareTo()}, {@code toString()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> accessor, mutator, dan pembanding hanya membaca atau mengubah state entity di memori.
 * Persistence, transaksi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan session
 * aktif.</p>
 *
 * <h2>Berkas pusat perjalanan karir pegawai</h2>
 * <p>Entity ini adalah berkas terpenting dalam klaster karir kepegawaian. Satu baris mencatat satu SK
 * perubahan kedudukan pegawai — kenaikan golongan, perubahan jabatan, penyesuaian gaji, atau
 * pengunduran diri — beserta seluruh akibat yang menyertainya. Yang membuatnya istimewa: nilai-nilai
 * yang <b>benar-benar berlaku</b> bagi seorang pegawai (golongan, jabatan struktural, jabatan
 * fungsional, gaji pokok, insentif, tunjangan makan dan transport) tidak disimpan sebagai kolom pada
 * {@code Pegawai}, melainkan <b>diturunkan setiap kali dibaca</b> dari kumpulan berkas ini yang
 * sedang berlaku. Karena itu menambah, menyunting, atau menghapus satu baris di sini dapat mengubah
 * gaji dan jabatan seorang pegawai seketika, tanpa perlu ada proses "penerapan" tersendiri.</p>
 *
 * <h2>Gerbang persetujuan dan celahnya</h2>
 * <p>Berkas ini berstatus {@link DataSop}, artinya ia dapat ditautkan ke satu alur SOP lewat
 * {@link #getDisposisiSop()}. Ketika tautan itu ada, {@link #getStatus()} menyimpulkan persetujuan
 * dari keadaan alurnya. Ketika tautan itu <b>tidak</b> ada, status dibaca apa adanya dari kolom yang
 * disetel operator lewat sebuah kotak centang di layar pengelola. Pembacaan dokumentasi
 * {@link #getStatus()} sangat dianjurkan sebelum menyentuh apa pun yang berhubungan dengan
 * persetujuan; di sana dicatat pula dua kelemahan nyata: penjagaan hak yang hanya berlaku di sisi
 * tampilan, dan sebuah cabang penyaring di {@code Pegawai} yang menerapkan berkas <b>tanpa</b>
 * memeriksa status sama sekali.</p>
 *
 * <h2>Banyak getter di sini bukan pembaca murni</h2>
 * <p>Kelas ini memuat dua pola yang harus disadari sebelum memakainya:</p>
 * <ul>
 * <li><b>Getter destruktif.</b> Sejumlah getter <i>menulis</i> ke field yang dibacanya —
 * {@link #getGolongan()}, {@link #getJabatan()}, {@link #getJabatanFungsional()},
 * {@link #getJabatanStruktural()}, {@link #getGajiPokok()}, {@link #getInsentif()},
 * {@link #getKenaikanBerkalaBulan()}, dan {@link #getPegawai()} termasuk di dalamnya. Pemanggilan
 * membaca dapat mengubah isi objek, dan karena entity ini memakai <b>akses properti</b> (anotasi
 * menempel pada getter), nilai hasil perubahan itulah yang ikut tersimpan saat Hibernate memeriksa
 * perubahan sebelum menulis. Membaca berkas ini bukan operasi yang bebas akibat.</li>
 * <li><b>Properti turunan yang tetap tersimpan.</b> {@link #getKenaikanJabatan()},
 * {@link #getJenis()}, {@link #getMenjabat()}, {@link #getStatus()},
 * {@link #getKenaikanPangkatGolongan()}, dan {@link #getKenaikanPangkatFungsional()} menghitung
 * nilainya dari field lain setiap kali dipanggil, namun tidak satu pun ditandai
 * {@code @Transient}. Semuanya tetap punya kolom di database dan nilainya ditimpa hasil perhitungan
 * setiap kali baris disimpan. Kolom-kolom itu karena itu tidak boleh diperlakukan sebagai masukan;
 * menulisinya lewat SQL langsung akan hilang pada penyimpanan berikutnya.</li>
 * </ul>
 *
 * <p>Sebagian besar perilaku bercabang di atas bergantung pada {@link #getJenisPerubahan()}, yang
 * menentukan aspek mana dari berkas ini yang bermakna. Nilai {@link #UBAH_PENGUNDURAN_DIRI} secara
 * khusus mengosongkan hampir seluruh relasi kedudukan dan memaksa dua penanda penonaktifan menjadi
 * menyala.</p>
 *
 * @see DataSop
 * @see DisposisiSop
 * @see KenaikanGajiBerkala
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "employ", name = "kenaikan_pangkat")
public class KenaikanPangkat extends DataSop {

	/**
	 * Penanda versi serialisasi Java. Nilai ini dipakai bersama oleh banyak entity paket
	 * {@code employ} karena berkas-berkasnya disalin dari template yang sama; angkanya tidak memiliki
	 * makna bisnis dan tidak boleh diubah tanpa alasan, sebab perubahan akan mematahkan deserialisasi
	 * state ZK/HTTP session dari versi aplikasi sebelumnya.
	 */
	private static final long serialVersionUID = 1129196121609467759L;

	/** Kunci utama baris {@code employ.kenaikan_pangkat}; diisi database (IDENTITY). */
	private Long id;
	/** Nama/identitas petugas terakhir yang menyimpan berkas ini -- jejak audit tampilan. */
	private String oleh;
	/** Id pengguna terakhir yang menyimpan berkas ini -- jejak audit yang dapat ditelusuri balik. */
	private String olehId;

	/**
	 * Id pengguna yang terakhir menyimpan berkas ini, terpisah dari {@link #getOleh()} yang menyimpan
	 * nama tampilan. Mengingat berkas ini menentukan golongan dan gaji pegawai, jejak ini merupakan
	 * salah satu dari sedikit petunjuk untuk menelusuri siapa yang mengubah kedudukan seseorang.
	 * Riwayat perubahan yang lengkap tersedia di tabel audit Envers karena entity ini
	 * {@code @Audited}.
	 *
	 * <p>Dapat {@code null} untuk baris warisan maupun baris yang disimpan proses batch tanpa konteks
	 * pengguna.</p>
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
	 * Urutan pembanding untuk pengurutan daftar berkas kenaikan pangkat.
	 *
	 * <p>Menerapkan pola berjenjang yang lazim di aplikasi ini: pembandingan dicoba berturut-turut
	 * pada pasangan field pertama yang <b>keduanya</b> (objek ini dan pembandingnya) tidak
	 * {@code null} -- dimulai dari {@link #getMulai() tanggal mulai berlaku}, lalu {@code nomorUrut},
	 * {@code nim}, {@code nama}, dan terakhir {@code keterangan}. Begitu satu pasangan memenuhi
	 * syarat, hasilnya langsung dikembalikan dan jenjang berikutnya tidak diperiksa.</p>
	 *
	 * <p>Jenjang pertama itulah yang membuat daftar berkas terurut secara kronologis dalam pemakaian
	 * normal. Perlu diketahui bahwa {@link #getMulai()} tidak pernah mengembalikan {@code null} — ia
	 * menggantikan field kosong dengan tanggal hari ini — sehingga syarat "kedua sisi tidak null"
	 * praktis selalu terpenuhi selama pembandingnya juga bertipe {@code KenaikanPangkat}. Akibatnya
	 * jenjang-jenjang berikutnya nyaris tidak pernah tercapai, dan berkas yang belum mengisi tanggal
	 * mulai akan berbaris di posisi "hari ini" alih-alih di ujung daftar.</p>
	 *
	 * <p>Jenjang pertama menyempitkan tipe argumen menjadi {@code KenaikanPangkat}. Bila daftar yang
	 * diurutkan bercampur dengan objek berjenis lain, penyempitan itu gagal — namun kegagalannya
	 * terperangkap {@code try/catch} yang mencatat ke audit error dan mengembalikan {@code 0}
	 * ("dianggap setara"), sehingga pengurutan tidak batal. Nilai {@code 0} juga dikembalikan bila
	 * tidak ada satu pun pasangan field yang dapat dibandingkan, termasuk saat argumen
	 * {@code null}.</p>
	 *
	 * <p>Seperti pembanding berjenjang lain, relasi urutan yang dihasilkan tidak dijamin transitif
	 * dan tidak konsisten dengan {@code equals}; untuk pengurutan yang harus stabil, tentukan
	 * {@code Comparator} eksplisit di lapisan pemanggil.</p>
	 *
	 * @param arg0 objek pembanding; diharapkan berjenis {@code KenaikanPangkat}
	 * @return bilangan negatif/nol/positif sesuai kontrak {@link Comparable}, atau {@code 0} bila
	 *         tidak ada field yang dapat dibandingkan maupun bila terjadi kesalahan
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		try {

			if (getMulai() != null && ((KenaikanPangkat) arg0).getMulai() != null) {
				return getMulai().compareTo(((KenaikanPangkat) arg0).getMulai());
			} else if (getNomorUrut() != null && arg0.getNomorUrut() != null) {
				return getNomorUrut().compareTo(arg0.getNomorUrut());
			} else if (getNim() != null && arg0.getNim() != null) {
				return getNim().compareTo(arg0.getNim());
			} else if (getNama() != null && arg0.getNama() != null) {
				return getNama().compareTo(arg0.getNama());
			} else if (getKeterangan() != null && arg0.getKeterangan() != null) {
				return getKeterangan().compareTo(arg0.getKeterangan());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/employ/KenaikanPangkat.java:74");

		}

		return 0;
	}

	/**
	 * Kunci utama baris {@code employ.kenaikan_pangkat}.
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
	 * Nama/identitas petugas yang terakhir menyimpan berkas ini -- jejak audit untuk ditampilkan di
	 * layar. Untuk penelusuran teknis gunakan {@link #getOlehId()}.
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
	 * {@link #getMulai()}, {@link #getTmt()}, maupun {@link #getTanggalSuratkeputusan()} yang
	 * semuanya bermakna bisnis; yang ini murni cap waktu teknis penyuntingan.
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
	 * Keterangan bebas mengenai berkas kenaikan pangkat ini. Boleh {@code null}. Perhatikan bahwa
	 * nilai ini juga dipakai sebagai hasil {@link #toString()} dan sebagai jenjang terakhir
	 * {@link #compareTo(GeneralValueObject)}.
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

	/** Pegawai pemilik berkas ini; relasi wajib -- lihat {@link #getPegawai()}. */
	private Pegawai pegawai;
	/** Jenis/pengelompokan usulan kenaikan pangkat; label pelaporan, tidak mempercabang aturan. */
	private JenisKenaikanPangkat jenisKenaikanPangkat;
	/** Nomor surat usul kenaikan pangkat. */
	private String noSuratUsul;
	/** Tanggal surat usul kenaikan pangkat. */
	private Date tanggalSuratUsul;
	/** Golongan baru; dapat ditimpa dari {@link #getGajiPokok()} -- lihat {@link #getGolongan()}. */
	private Golongan golongan;
	/** Baris tabel gaji pokok yang ditetapkan berkas ini. */
	private GajiPokok gajiPokok;
	/** Baris tabel insentif yang ditetapkan berkas ini. */
	private Insentif insentif;
	/** Baris tabel tunjangan makan yang ditetapkan berkas ini. */
	private Makan makan;
	/** Baris tabel tunjangan transport yang ditetapkan berkas ini. */
	private Transport transport;
	/** Nama pejabat penanda tangan SK. */
	private String namaPejabat;
	/** Nomor SK kenaikan pangkat. */
	private String nomorSuratkeputusan;
	/** Jarak bulan antar kenaikan gaji berkala -- lihat {@link #getKenaikanBerkalaBulan()}. */
	private Integer kenaikanBerkalaBulan;
	/** Tanggal SK kenaikan pangkat. */
	private Date tanggalSuratkeputusan;
	/** Awal masa berlaku berkas; tidak pernah kosong saat dibaca -- lihat {@link #getMulai()}. */
	private Date mulai;
	/** Akhir masa berlaku berkas; kosong berarti belum berakhir. */
	private Date sampai;
	/** Peraturan yang menjadi dasar hukum berkas ini. */
	private Peraturan peraturan;
	/** Terhitung mulai tanggal menurut SK -- lihat {@link #getTmt()}. */
	private Date tmt;
	/** Tautan ke alur SOP yang menyetujui berkas ini; kosong berarti tanpa alur SOP. */
	private DisposisiSop disposisiSop;
	/** Cadangan peran pengguna dalam bentuk JSON -- lihat {@link #getJsonDataPengguna()}. */
	private String jsonDataPengguna;
	/** Turunan: apakah berkas ini mengubah jabatan -- lihat {@link #getKenaikanJabatan()}. */
	private Boolean kenaikanJabatan = false;
	/** Turunan: jenis jabatan (struktural/fungsional) -- lihat {@link #getJenis()}. */
	private String jenis;
	/** Jabatan fungsional baru; dikosongkan bila jenis perubahan pengunduran diri. */
	private JabatanFungsional jabatanFungsional;
	/** Jabatan struktural baru; dikosongkan bila jenis perubahan pengunduran diri. */
	private JabatanStruktural jabatanStruktural;
	/** Jabatan umum baru; dikosongkan bila jenis perubahan pengunduran diri. */
	private Jabatan jabatan;
	/** Turunan: apakah berkas ini sedang berlaku hari ini -- lihat {@link #getMenjabat()}. */
	private Boolean menjabat = false;
	/** Turunan: status persetujuan -- lihat {@link #getStatus()}. */
	private Boolean status = false;
	/** Penanda agar berkas sebelumnya ditutup saat berkas ini disetujui. */
	private Boolean nonAktifkanJabatanSebelumnya = false;
	/** Penanda bahwa berkas ini disertai kenaikan gaji berkala berjangka. */
	private Boolean terdapatKenaikanGajiBerkala;
	/** Penanda bahwa nilai gaji ditulis langsung di berkas ini, bukan lewat tabel master. */
	private Boolean gajiLangsungDitentukanDisini;
	/** Penanda penggajian otomatis berdasar masa kerja -- lihat getternya. */
	private Boolean gajiPokokOtomatisMasaKerja;
	/** Penanda agar akun pengguna pegawai dinonaktifkan saat berkas ini disetujui. */
	private Boolean nonAktifkanPengguna = false;
	/** Penanda agar akun pengguna pegawai diaktifkan kembali saat berkas ini disetujui. */
	private Boolean aktifkanPengguna = false;
	/** Turunan: apakah ini kenaikan pangkat jalur struktural. */
	private Boolean kenaikanPangkatGolongan;
	/** Turunan: apakah ini kenaikan pangkat jalur fungsional. */
	private Boolean kenaikanPangkatFungsional;

	/** Nilai gaji yang ditulis langsung di berkas ini; dipakai bila gaji tidak dari tabel master. */
	private Double nilaiGaji = 0.0;
	/** Nilai insentif yang ditulis langsung di berkas ini. */
	private Double nilaiInsentif = 0.0;

	/**
	 * Nilai {@link #getJenisPerubahan()} untuk berkas yang mengubah <b>jabatan sekaligus golongan</b>
	 * -- bentuk kenaikan pangkat yang paling lengkap, dan sekaligus nilai default yang dipakai
	 * {@link #getJenisPerubahan()} bila kolomnya kosong atau tidak dikenali.
	 */
	public static final String UBAH_JABATAN_DAN_GOLONGAN = "Jabatan dan Golongan";
	/** Nilai {@link #getJenisPerubahan()} untuk berkas yang hanya mengubah <b>jabatan</b>. */
	public static final String UBAH_JABATAN = "Jabatan";
	/** Nilai {@link #getJenisPerubahan()} untuk berkas yang hanya mengubah <b>golongan</b>. */
	public static final String UBAH_GOLONGAN = "Golongan";
	/**
	 * Nilai {@link #getJenisPerubahan()} untuk berkas <b>pengunduran diri</b>.
	 *
	 * <p>Nilai ini bukan sekadar label: ia mengubah perilaku banyak getter sekaligus. Bila terpasang,
	 * {@link #getGolongan()}, {@link #getJabatan()}, {@link #getJabatanFungsional()},
	 * {@link #getJabatanStruktural()}, {@link #getGajiPokok()}, dan {@link #getInsentif()} semuanya
	 * mengembalikan {@code null} — dan karena getter-getter itu menulis balik ke field-nya, relasi
	 * yang bersangkutan benar-benar terputus saat berkas disimpan. Bersamaan dengan itu
	 * {@link #getNonAktifkanJabatanSebelumnya()} dan {@link #getNonAktifkanPengguna()} dipaksa
	 * menyala. Gabungan keduanya membuat berkas pengunduran diri menutup jabatan yang sedang berjalan
	 * sekaligus menonaktifkan akun pengguna pegawai bersangkutan.</p>
	 */
	public static final String UBAH_PENGUNDURAN_DIRI = "Pengunduran Diri";

	/** Jenis perubahan yang dibawa berkas ini -- lihat {@link #getJenisPerubahan()}. */
	private String jenisPerubahan;

	// private String status;

	/**
	 * Pegawai pemilik berkas kenaikan pangkat ini. <b>Getter ini bukan pembaca murni</b> — ia
	 * mengubah state objek, dan perilakunya perlu dipahami sebelum dipakai.
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
	 * properti, Hibernate memanggil getter yang sama ketika memeriksa perubahan sebelum menulis.
	 * Berkas yang kehilangan referensi pegawainya karena itu dapat <b>berpindah kepemilikan ke
	 * pembacanya</b>. Pada entity inilah akibatnya paling berat di seluruh klaster karir: berkas yang
	 * berpindah adalah berkas yang menentukan golongan, jabatan, dan gaji, sehingga perpindahan
	 * kepemilikan berarti perpindahan kedudukan dan penghasilan. Kolomnya {@code nullable = false},
	 * sehingga substitusi ini sekaligus menyamarkan data yang seharusnya ditolak database.</p>
	 *
	 * <p>Kegagalan mengambil pengguna aktif — misalnya karena getter dipanggil dari utas latar tanpa
	 * konteks sesi, termasuk oleh pekerjaan tertunda yang dijadwalkan layar pengelola — ditangkap dan
	 * dicatat ke audit error, lalu field dibiarkan {@code null}. Perilaku getter ini dengan demikian
	 * berbeda antara konteks web dan konteks batch.</p>
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
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/employ/KenaikanPangkat.java:178");

		}

		return pegawai;
	}

	/**
	 * Setter {@link #getPegawai()}. Menyimpan referensi apa adanya, termasuk {@code null} -- namun
	 * perlu diingat bahwa menyetel {@code null} tidak benar-benar mengosongkan relasi, karena
	 * getter-nya akan menggantinya dengan pegawai pengguna aktif pada pembacaan berikutnya.
	 *
	 * @param pegawai pegawai pemilik berkas
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Nomor surat usul kenaikan pangkat, yaitu nomor surat yang mengajukan perubahan kedudukan ini.
	 * Teks bebas tanpa format yang ditegakkan; boleh {@code null}. Dibedakan dari
	 * {@link #getNomorSuratkeputusan()} yang mencatat nomor SK penetapannya.
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
	 * Tanggal surat usul kenaikan pangkat. Tanpa anotasi {@code @Temporal}, sehingga dipetakan
	 * mengikuti default penyedia persistence untuk {@link Date} (cap waktu lengkap) -- berbeda dengan
	 * {@link #getTanggalSuratkeputusan()} yang menyatakan {@code TemporalType.DATE} secara eksplisit.
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
	 * Golongan/pangkat yang ditetapkan berkas ini. <b>Getter destruktif</b> dengan dua lapis
	 * penimpaan, sehingga nilai yang dikembalikan sering kali bukan nilai yang disetel pemanggil.
	 *
	 * <p>Lapis pertama adalah resolusi proxy lazy lewat {@code check(..)}, yang menulis balik hasil
	 * resolusinya ke field seperti dijelaskan pada {@link #getPegawai()}.</p>
	 *
	 * <p>Lapis kedua adalah <b>penurunan dari gaji pokok</b>: bila {@link #getGajiPokok()} tidak
	 * kosong, golongan diambil dari golongan yang melekat pada baris gaji pokok tersebut, menimpa apa
	 * pun yang ada di field. Aturan ini menjadikan tabel gaji pokok sebagai sumber kebenaran tunggal
	 * bagi pasangan golongan-gaji, sehingga keduanya tidak dapat menyimpang satu sama lain. Efek
	 * sampingnya, menyetel golongan lewat {@link #setGolongan(Golongan)} tidak berpengaruh selama
	 * gaji pokok terisi — dan karena entity memakai akses properti, golongan hasil turunan itulah
	 * yang tersimpan ke database.</p>
	 *
	 * <p>Terakhir, bila {@link #getJenisPerubahan()} bernilai {@link #UBAH_PENGUNDURAN_DIRI},
	 * golongan dikosongkan sepenuhnya. Berkas pengunduran diri memang tidak menetapkan pangkat baru;
	 * pengosongan ini pun ikut tersimpan, memutus relasi golongan pada baris tersebut.</p>
	 *
	 * <p>Perlu dicatat bahwa {@link #getGajiPokok()} yang dipanggil di sini juga getter destruktif
	 * yang mengosongkan dirinya sendiri pada kondisi tertentu; urutan pemanggilan antara keduanya
	 * karena itu memengaruhi hasil. Kolomnya {@code nullable = true} sehingga berkas tanpa golongan
	 * tetap dapat disimpan.</p>
	 *
	 * @return golongan yang berlaku menurut berkas ini, atau {@code null} untuk berkas pengunduran
	 *         diri maupun berkas yang memang tidak menetapkan golongan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "golongan", nullable = true)
	public Golongan getGolongan() {
		golongan = check(golongan);
		if (getGajiPokok() != null) {
			golongan = getGajiPokok().getGolongan();
		}

		if (getJenisPerubahan().equalsIgnoreCase(UBAH_PENGUNDURAN_DIRI)) {
			golongan = null;
		}

		return golongan;
	}

	/**
	 * Setter {@link #getGolongan()}. Perhatikan bahwa nilai yang disetel di sini <b>dapat diabaikan</b>
	 * oleh getter-nya: bila gaji pokok terisi, golongan diturunkan dari sana; bila jenis perubahannya
	 * pengunduran diri, golongan dikosongkan.
	 *
	 * @param golongan golongan yang ditetapkan berkas ini
	 */
	public void setGolongan(Golongan golongan) {
		this.golongan = golongan;
	}

	/**
	 * Nama pejabat yang menandatangani SK kenaikan pangkat. Disimpan sebagai teks, bukan relasi ke
	 * data pegawai, sehingga tetap terbaca meski pejabat bersangkutan tidak lagi tercatat di sistem.
	 * Boleh {@code null}. Tidak ada anotasi kolom, jadi nama kolomnya mengikuti nama properti.
	 *
	 * @return nama pejabat penanda tangan, atau {@code null} bila belum diisi
	 */
	public String getNamaPejabat() {
		return namaPejabat;
	}

	/**
	 * Setter {@link #getNamaPejabat()}.
	 *
	 * @param namaPejabat nama pejabat penanda tangan; boleh {@code null}
	 */
	public void setNamaPejabat(String namaPejabat) {
		this.namaPejabat = namaPejabat;
	}

	/**
	 * Nomor SK kenaikan pangkat, yaitu nomor surat keputusan yang menetapkan perubahan kedudukan ini
	 * -- dibedakan dari {@link #getNoSuratUsul()} yang mencatat nomor surat pengusulnya. Teks bebas;
	 * boleh {@code null}.
	 *
	 * @return nomor SK, atau {@code null} bila belum diisi
	 */
	public String getNomorSuratkeputusan() {
		return nomorSuratkeputusan;
	}

	/**
	 * Setter {@link #getNomorSuratkeputusan()}.
	 *
	 * @param nomorSuratkeputusan nomor SK; boleh {@code null}
	 */
	public void setNomorSuratkeputusan(String nomorSuratkeputusan) {
		this.nomorSuratkeputusan = nomorSuratkeputusan;
	}

	/**
	 * Tanggal terbit SK kenaikan pangkat, dipetakan {@code TemporalType.DATE} sehingga hanya bagian
	 * tanggalnya yang disimpan. Dibedakan dari {@link #getTmt()} (kapan SK mulai berlaku) dan dari
	 * {@link #getMulai()} (kapan berkas ini mulai diperhitungkan aplikasi); ketiganya lazim berbeda.
	 *
	 * @return tanggal SK, atau {@code null} bila belum diisi
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalSuratkeputusan() {
		return tanggalSuratkeputusan;
	}

	/**
	 * Setter {@link #getTanggalSuratkeputusan()}.
	 *
	 * @param tanggalSuratkeputusan tanggal terbit SK; boleh {@code null}
	 */
	public void setTanggalSuratkeputusan(Date tanggalSuratkeputusan) {
		this.tanggalSuratkeputusan = tanggalSuratkeputusan;
	}

	/**
	 * Peraturan yang menjadi dasar hukum berkas ini, merujuk baris master {@link Peraturan}. Getter
	 * ini meresolusi proxy lazy lewat {@code check(..)} dan menulis balik hasilnya ke field, namun
	 * tidak melakukan substitusi maupun pengosongan apa pun. Kolomnya {@code nullable = true}.
	 *
	 * @return peraturan dasar, atau {@code null} bila tidak diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "peraturan", nullable = true)
	public Peraturan getPeraturan() {
		peraturan = check(peraturan);
		return peraturan;
	}

	/**
	 * Setter {@link #getPeraturan()}.
	 *
	 * @param peraturan peraturan dasar hukum; boleh {@code null}
	 */
	public void setPeraturan(Peraturan peraturan) {
		this.peraturan = peraturan;
	}

	/**
	 * <b>Properti turunan:</b> apakah berkas ini membawa perubahan jabatan. Dihitung ulang setiap
	 * kali dipanggil sebagai "ada salah satu jabatan yang terisi" -- {@link #getJabatan()},
	 * {@link #getJabatanFungsional()}, atau {@link #getJabatanStruktural()}. Nilai apa pun yang
	 * disetel lewat {@link #setKenaikanJabatan(Boolean)} akan ditimpa pada pembacaan berikutnya.
	 *
	 * <p>Perhatikan bahwa properti ini <b>tidak</b> ditandai {@code @Transient}: ia tetap memiliki
	 * kolom di database, dan karena entity memakai akses properti, hasil perhitungan itulah yang
	 * tersimpan setiap kali baris disimpan. Kolom tersebut karena itu merupakan cerminan, bukan
	 * masukan; menulisinya lewat SQL langsung tidak akan bertahan.</p>
	 *
	 * <p>Karena ketiga getter jabatan yang dipanggil di sini bersifat destruktif dan mengosongkan
	 * dirinya pada berkas pengunduran diri, properti ini otomatis bernilai {@code false} untuk berkas
	 * semacam itu.</p>
	 *
	 * @return {@code true} bila berkas ini menetapkan jabatan apa pun; {@code false} bila tidak
	 */
	public Boolean getKenaikanJabatan() {
		kenaikanJabatan = getJabatan() != null;

		if (getJabatanFungsional() != null) {
			kenaikanJabatan = true;
		} else if (getJabatanStruktural() != null) {
			kenaikanJabatan = true;
		}

		return kenaikanJabatan;
	}

	/**
	 * Setter {@link #getKenaikanJabatan()}. Nilai yang disetel hanya bertahan sampai pembacaan
	 * berikutnya, karena getter-nya selalu menghitung ulang. Disediakan agar kontrak JavaBean lengkap
	 * dan agar Hibernate dapat memuat nilai kolomnya.
	 *
	 * @param kenaikanJabatan diabaikan secara efektif
	 */
	public void setKenaikanJabatan(Boolean kenaikanJabatan) {
		this.kenaikanJabatan = kenaikanJabatan;
	}

	/**
	 * <b>Properti turunan:</b> jenis jabatan yang dibawa berkas ini, bernilai salah satu dari
	 * konstanta jenis pada {@code Pegawai} -- fungsional bila {@link #getJabatanFungsional()} terisi,
	 * struktural bila {@link #getJabatanStruktural()} terisi.
	 *
	 * <p>Bila keduanya kosong, nilainya <b>jatuh ke struktural</b> alih-alih dibiarkan kosong.
	 * Default ini perlu disadari: berkas yang sama sekali tidak menetapkan jabatan tetap melaporkan
	 * dirinya berjenis struktural, sehingga penyaring atau laporan yang mengelompokkan berdasarkan
	 * properti ini akan menghitungnya di kelompok struktural. Perhatikan pula bahwa pemeriksaan
	 * fungsional didahulukan, jadi bila kedua jabatan terisi sekaligus — keadaan yang tidak dicegah
	 * di mana pun — hasilnya fungsional.</p>
	 *
	 * <p>Seperti properti turunan lain di kelas ini, nilainya tetap tersimpan ke kolom database dan
	 * ditimpa hasil perhitungan pada setiap penyimpanan.</p>
	 *
	 * @return jenis jabatan; tidak pernah {@code null} setelah pemanggilan pertama
	 */
	public String getJenis() {

		if (getJabatanFungsional() != null) {
			jenis = Pegawai.JENIS_FUNGSIONAL;
		} else if (getJabatanStruktural() != null) {
			jenis = Pegawai.JENIS_STRUKTURAL;
		}

		if (jenis == null) {
			jenis = Pegawai.JENIS_STRUKTURAL;
		}
		return jenis;
	}

	/**
	 * Setter {@link #getJenis()}. Nilai yang disetel dapat ditimpa pembacaan berikutnya bila salah
	 * satu jabatan terisi; bila keduanya kosong, nilai yang tidak {@code null} akan dipertahankan.
	 *
	 * @param jenis jenis jabatan
	 */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/**
	 * Jabatan fungsional yang ditetapkan berkas ini. <b>Getter destruktif:</b> meresolusi proxy lazy
	 * lewat {@code check(..)} dan menulis balik hasilnya, lalu <b>mengosongkan field</b> bila
	 * {@link #getJenisPerubahan()} bernilai {@link #UBAH_PENGUNDURAN_DIRI}.
	 *
	 * <p>Pengosongan itu ikut tersimpan karena entity memakai akses properti, sehingga mengubah jenis
	 * perubahan sebuah berkas menjadi pengunduran diri akan benar-benar memutus relasi jabatan
	 * fungsionalnya pada penyimpanan berikutnya — bukan sekadar menyembunyikannya dari tampilan.
	 * Kolomnya {@code nullable = true}.</p>
	 *
	 * @return jabatan fungsional yang ditetapkan, atau {@code null} untuk berkas pengunduran diri
	 *         maupun berkas yang tidak menetapkannya
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jabatan_fungsional", nullable = true)
	public JabatanFungsional getJabatanFungsional() {
		jabatanFungsional = check(jabatanFungsional);
		if (getJenisPerubahan().equalsIgnoreCase(UBAH_PENGUNDURAN_DIRI)) {
			jabatanFungsional = null;
		}
		return jabatanFungsional;
	}

	/**
	 * Setter {@link #getJabatanFungsional()}. Nilai yang disetel akan dikosongkan getter-nya bila
	 * jenis perubahan berkas ini pengunduran diri.
	 *
	 * @param jabatanFungsional jabatan fungsional yang ditetapkan; boleh {@code null}
	 */
	public void setJabatanFungsional(JabatanFungsional jabatanFungsional) {
		this.jabatanFungsional = jabatanFungsional;
	}

	/**
	 * Jabatan struktural yang ditetapkan berkas ini. <b>Getter destruktif</b> dengan perilaku sama
	 * persis dengan {@link #getJabatanFungsional()}: resolusi proxy lazy yang ditulis balik, disusul
	 * pengosongan field bila jenis perubahannya {@link #UBAH_PENGUNDURAN_DIRI}.
	 *
	 * <p>Properti ini juga menjadi dasar {@link #getKenaikanPangkatGolongan()}, sehingga berkas
	 * pengunduran diri otomatis tidak tergolong kenaikan pangkat jalur golongan.</p>
	 *
	 * @return jabatan struktural yang ditetapkan, atau {@code null} untuk berkas pengunduran diri
	 *         maupun berkas yang tidak menetapkannya
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jabatan_struktural", nullable = true)
	public JabatanStruktural getJabatanStruktural() {
		jabatanStruktural = check(jabatanStruktural);

		if (getJenisPerubahan().equalsIgnoreCase(UBAH_PENGUNDURAN_DIRI)) {
			jabatanStruktural = null;
		}
		return jabatanStruktural;
	}

	/**
	 * Setter {@link #getJabatanStruktural()}. Nilai yang disetel akan dikosongkan getter-nya bila
	 * jenis perubahan berkas ini pengunduran diri.
	 *
	 * @param jabatanStruktural jabatan struktural yang ditetapkan; boleh {@code null}
	 */
	public void setJabatanStruktural(JabatanStruktural jabatanStruktural) {
		this.jabatanStruktural = jabatanStruktural;
	}

	/**
	 * Jabatan umum yang ditetapkan berkas ini, merujuk entity {@link Jabatan} di paket model induk --
	 * berbeda dari pasangan jabatan struktural/fungsional yang khusus kepegawaian. <b>Getter
	 * destruktif</b> dengan pola sama: resolusi proxy lazy yang ditulis balik, lalu pengosongan field
	 * bila jenis perubahannya {@link #UBAH_PENGUNDURAN_DIRI}.
	 *
	 * @return jabatan yang ditetapkan, atau {@code null} untuk berkas pengunduran diri maupun berkas
	 *         yang tidak menetapkannya
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jabatan", nullable = true)
	public Jabatan getJabatan() {
		jabatan = check(jabatan);
		if (getJenisPerubahan().equalsIgnoreCase(UBAH_PENGUNDURAN_DIRI)) {
			jabatan = null;
		}
		return jabatan;
	}

	/**
	 * Setter {@link #getJabatan()}. Nilai yang disetel akan dikosongkan getter-nya bila jenis
	 * perubahan berkas ini pengunduran diri.
	 *
	 * @param jabatan jabatan yang ditetapkan; boleh {@code null}
	 */
	public void setJabatan(Jabatan jabatan) {
		this.jabatan = jabatan;
	}

	/**
	 * <b>Properti turunan:</b> apakah berkas ini <b>sedang berlaku hari ini</b> dilihat dari rentang
	 * {@link #getMulai()} sampai {@link #getSampai()}.
	 *
	 * <p>Perhitungannya berjenjang. Field yang masih {@code null} lebih dulu dianggap {@code true}.
	 * Kemudian, bila tanggal berakhir terisi dan sudah terlewat, hasilnya {@code false}; bila tanggal
	 * mulai terisi dan belum tiba, hasilnya juga {@code false}; selebihnya, sepanjang tanggal mulai
	 * terisi, hasilnya {@code true}. Perlu dicatat bahwa {@link #getMulai()} tidak pernah
	 * mengembalikan {@code null}, sehingga jenjang terakhir itu praktis selalu tercapai.</p>
	 *
	 * <p>Pembandingan tanggalnya memakai dua langkah yang berpasangan: kesamaan hari diperiksa dengan
	 * <b>membandingkan teks</b> hasil pemformatan tanggal, baru kemudian urutannya diperiksa dengan
	 * pembandingan {@link Date} biasa. Susunan semacam ini dipakai untuk menetralkan komponen jam
	 * yang mungkin ikut tersimpan — tanpa itu, berkas yang mulai berlaku hari ini pukul sekian akan
	 * dianggap "belum tiba" sepanjang sisa hari tersebut. Konsekuensinya, hari batas — baik tanggal
	 * mulai maupun tanggal berakhir — selalu diperlakukan sebagai <b>masih berlaku</b>.</p>
	 *
	 * <p>Karena "hari ini" diambil dari sumber waktu aplikasi pada saat pemanggilan, nilai properti
	 * ini <b>berubah dengan sendirinya seiring berjalannya waktu</b> tanpa ada yang menyunting
	 * berkas. Seperti properti turunan lain di kelas ini, nilainya tidak {@code @Transient}: ia tetap
	 * tersimpan ke kolom database dan ditimpa hasil perhitungan pada setiap penyimpanan, sehingga
	 * kolom tersebut hanya mencerminkan keadaan pada saat baris terakhir disimpan dan tidak boleh
	 * dipakai sebagai penyaring di query SQL langsung.</p>
	 *
	 * <p>Bersama {@link #getStatus()}, properti inilah yang menentukan apakah sebuah berkas ikut
	 * diperhitungkan {@code Pegawai} saat menurunkan golongan, jabatan, dan gaji yang berlaku.</p>
	 *
	 * @return {@code true} bila berkas ini sedang berlaku hari ini; {@code false} bila sudah lewat
	 *         atau belum tiba
	 */
	public Boolean getMenjabat() {
		if (menjabat == null) {
			menjabat = true;
		}

		if (getSampai() != null
				&& !Common.dateFormat83.get().format(getSampai()).equals(Common.dateFormat83.get().format(WaktuUtil.getDate()))
				&& getSampai().before(WaktuUtil.getDate())) {
			menjabat = false;
		} else if (getMulai() != null
				&& !Common.dateFormat83.get().format(getMulai()).equals(Common.dateFormat83.get().format(WaktuUtil.getDate()))
				&& getMulai().after(WaktuUtil.getDate())) {
			menjabat = false;
		} else if (getMulai() != null) {
			menjabat = true;
		}

		return menjabat;
	}

	/**
	 * Setter {@link #getMenjabat()}. Nilai yang disetel akan ditimpa pembacaan berikutnya karena
	 * getter-nya selalu menghitung ulang dari rentang tanggal.
	 *
	 * @param menjabat diabaikan secara efektif
	 */
	public void setMenjabat(Boolean menjabat) {
		this.menjabat = menjabat;
	}

	/**
	 * <b>Gerbang persetujuan berkas ini.</b> Menyatakan apakah kenaikan pangkat sudah disetujui
	 * sehingga boleh diperhitungkan. Ini properti terpenting di kelas ini sekaligus yang paling
	 * perlu kehati-hatian, karena perilakunya bercabang tergantung ada tidaknya alur SOP.
	 *
	 * <h3>Bila berkas tertaut alur SOP</h3>
	 * <p>Ketika {@link #getDisposisiSop()} tidak kosong, status <b>dihitung ulang</b> dari keadaan
	 * alur tersebut, dengan tiga kemungkinan yang diperiksa berurutan. Pertama, bila disposisinya
	 * sudah tidak aktif, status {@code false}. Kedua, bila langkah akhir alur berada pada titik yang
	 * ditandai sebagai titik penolakan, status juga {@code false}. Ketiga — barulah — status bernilai
	 * {@code true} apabila alur tersebut memiliki disposisi persetujuan yang sudah tersimpan (ditandai
	 * id-nya tidak kosong). Susunan ini bersifat <i>fail-closed</i> pada dua cabang pertama: keadaan
	 * yang meragukan berujung "belum disetujui", bukan sebaliknya.</p>
	 *
	 * <h3>Bila berkas tidak tertaut alur SOP</h3>
	 * <p>Ketika {@link #getDisposisiSop()} kosong, tidak satu pun cabang di atas berlaku dan yang
	 * dikembalikan adalah <b>nilai kolom {@code status} apa adanya</b> — nilai yang disetel operator
	 * lewat sebuah kotak centang pada layar pengelola. Inilah jalur persetujuan yang sesungguhnya
	 * dipakai untuk berkas tanpa SOP, dan di sinilah dua kelemahan berikut berada.</p>
	 *
	 * <h3>Kelemahan yang tercatat</h3>
	 * <ul>
	 * <li><b>Penjagaan hak hanya di sisi tampilan.</b> Layar pengelola memang memeriksa hak
	 * persetujuan, tetapi pemeriksaan itu hanya dipakai untuk menyembunyikan dan menonaktifkan kotak
	 * centangnya. Nilai kotak centang tetap dibaca dan disimpan di sisi peladen pada setiap
	 * penyimpanan, tanpa pemeriksaan ulang. Perlindungan yang hanya hidup di lapisan tampilan tidak
	 * berlaku bagi permintaan yang dibentuk di luar layar tersebut. Pola yang sama terdapat pada dua
	 * jalur lain yang juga menulis status berkas ini.</li>
	 * <li><b>Tidak ada pemisahan pengusul dan penyetuju.</b> Baik di jalur non-SOP maupun di alur SOP,
	 * tidak ada satu pun pemeriksaan yang membandingkan siapa yang mengajukan berkas dengan siapa
	 * yang menyetujuinya. Pemegang hak persetujuan pada menu ini dapat menyetujui berkas buatannya
	 * sendiri.</li>
	 * </ul>
	 *
	 * <h3>Celah pada pemakaian di sisi pembaca</h3>
	 * <p>Yang paling perlu diketahui: gerbang ini <b>tidak selalu diperiksa</b>. Penyaring di
	 * {@code Pegawai} yang memilih berkas mana yang berlaku memiliki dua cabang. Cabang pertama
	 * mensyaratkan {@link #getMenjabat()} <i>dan</i> status ini bernilai {@code true}. Cabang kedua,
	 * yang berlaku ketika tanggal mulai <b>dan</b> tanggal berakhir sama-sama terisi dan hari ini
	 * berada di dalam rentangnya, menerima berkas <b>tanpa memeriksa status sama sekali</b>. Karena
	 * penyaring itulah sumber tunggal bagi penurunan golongan, jabatan struktural, jabatan
	 * fungsional, gaji pokok, insentif, serta tunjangan makan dan transport, berkas yang belum
	 * disetujui tetap dapat menggerakkan seluruh nilai tersebut — cukup dengan mengisi tanggal
	 * berakhir. Jangan menganggap gerbang ini memadai sebagai satu-satunya pengaman.</p>
	 *
	 * <p>Efek samping berat yang memang benar digerbangi status ini — penutupan berkas sebelumnya,
	 * penulisan ulang peran pengguna, serta penonaktifan dan pengaktifan akun pegawai — dijalankan
	 * layar pengelola secara tertunda di luar transaksi penyimpanan.</p>
	 *
	 * <p>Sebagaimana properti turunan lain di kelas ini, hasil perhitungan ditulis balik ke field dan
	 * ikut tersimpan ke kolom {@code status}; kolom itu karenanya bukan masukan tepercaya untuk
	 * berkas ber-SOP. Field yang masih {@code null} diperlakukan sebagai {@code false} (belum
	 * disetujui) — pilihan default yang aman.</p>
	 *
	 * @return {@code true} bila berkas ini terhitung disetujui; {@code false} bila belum, ditolak,
	 *         atau disposisinya sudah tidak aktif
	 */
	public Boolean getStatus() {
		if (status == null) {
			status = false;
		}

		disposisiSop = getDisposisiSop();
		if (disposisiSop != null && !disposisiSop.getAktif()) {
			status = false;
		} else if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
			status = false;
		} else if (disposisiSop != null) {
			status = disposisiSop.getDisposisiSetuju() != null && disposisiSop.getDisposisiSetuju().getId() != null;
		}

		return status;
	}

	/**
	 * Setter {@link #getStatus()}, yaitu <b>penyetel gerbang persetujuan</b>. Menyimpan nilai apa
	 * adanya tanpa pemeriksaan hak akses apa pun — seluruh penjagaan kewenangan berada di lapisan
	 * Action, dan sebagaimana dicatat pada getter-nya, penjagaan itu hanya hidup di sisi tampilan.
	 *
	 * <p>Nilai yang disetel di sini hanya bertahan untuk berkas <b>tanpa</b> alur SOP; pada berkas
	 * yang tertaut disposisi SOP, getter-nya akan menghitung ulang dan menimpanya.</p>
	 *
	 * @param status status persetujuan
	 */
	public void setStatus(Boolean status) {
		this.status = status;
	}

	/**
	 * Awal masa berlaku berkas ini menurut aplikasi. <b>Tidak pernah mengembalikan {@code null}:</b>
	 * bila field kosong, yang dikembalikan adalah tanggal hari ini dari sumber waktu aplikasi.
	 *
	 * <p>Nilai pengganti itu tidak ditulis ke field, tetapi tetap berakibat pada data. Karena entity
	 * memakai akses properti, Hibernate membaca getter ini ketika memeriksa perubahan sebelum
	 * menulis — sehingga baris yang kolom {@code mulai}-nya kosong akan <b>tersimpan dengan tanggal
	 * hari ini</b> pada penyimpanan berikutnya, dan tanggal itu berbeda-beda tergantung kapan barisnya
	 * kebetulan tersentuh. Berkas yang sengaja dibiarkan tanpa tanggal mulai karena itu tidak dapat
	 * dipertahankan dalam keadaan demikian.</p>
	 *
	 * <p>Sifat "tidak pernah kosong" ini juga menjelaskan perilaku dua properti lain:
	 * {@link #compareTo(GeneralValueObject)} nyaris selalu berhenti di jenjang pertamanya, dan
	 * {@link #getMenjabat()} nyaris selalu mencapai jenjang terakhirnya. Dipetakan
	 * {@code TemporalType.DATE} sehingga hanya bagian tanggal yang disimpan.</p>
	 *
	 * <p>Bedakan dari {@link #getTmt()} yang mencatat tanggal mulai berlaku menurut SK: yang ini
	 * adalah tanggal yang benar-benar dipakai aplikasi untuk memutuskan berkas mana yang berlaku,
	 * sedangkan TMT bersifat administratif.</p>
	 *
	 * @return tanggal mulai berlaku; tidak pernah {@code null} -- tanggal hari ini bila belum diisi
	 */
	@Temporal(TemporalType.DATE)
	public Date getMulai() {
		return mulai == null ? WaktuUtil.getDate() : mulai;
	}

	/**
	 * Setter {@link #getMulai()}. Menyetel {@code null} tidak benar-benar mengosongkan tanggal mulai,
	 * karena getter-nya menggantinya dengan tanggal hari ini pada pembacaan berikutnya.
	 *
	 * @param mulai tanggal mulai berlaku berkas
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Akhir masa berlaku berkas ini. Nilai {@code null} berarti berkas <b>belum berakhir</b> dan
	 * karenanya masih berlaku sepanjang tanggal mulainya sudah tiba. Dipetakan
	 * {@code TemporalType.DATE}.
	 *
	 * <p>Berbeda dengan {@link #getMulai()}, getter ini pembaca murni dan benar-benar dapat
	 * mengembalikan {@code null}. Field inilah yang disetel layar pengelola ketika sebuah berkas
	 * ditutup karena digantikan berkas yang lebih baru — penutupan yang dijalankan hanya bila berkas
	 * penggantinya sudah berstatus disetujui.</p>
	 *
	 * <p><b>Perhatian:</b> mengisi tanggal ini punya akibat yang tidak terduga pada penyaringan
	 * berkas. Sebagaimana dijelaskan pada {@link #getStatus()}, cabang kedua penyaring di
	 * {@code Pegawai} menerima berkas yang tanggal mulai dan tanggal berakhirnya sama-sama terisi
	 * tanpa memeriksa status, sehingga mengisi tanggal berakhir pada berkas yang belum disetujui
	 * justru membuatnya ikut diperhitungkan.</p>
	 *
	 * @return tanggal berakhir berlaku, atau {@code null} bila berkas belum berakhir
	 */
	@Temporal(TemporalType.DATE)
	public Date getSampai() {
		return sampai;
	}

	/**
	 * Setter {@link #getSampai()}. Menerima {@code null} untuk menyatakan berkas belum berakhir.
	 *
	 * @param sampai tanggal berakhir berlaku; boleh {@code null}
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * <b>Properti turunan:</b> apakah berkas ini merupakan kenaikan pangkat jalur <b>struktural</b>,
	 * dihitung semata dari terisinya {@link #getJabatanStruktural()}. Nilai yang disetel lewat
	 * setter-nya selalu ditimpa pada pembacaan.
	 *
	 * <p>Penamaannya membingungkan: properti bernama "kenaikan pangkat golongan" ini sebenarnya
	 * menandai jalur struktural, dan berpasangan dengan {@link #getKenaikanPangkatFungsional()} yang
	 * menandai jalur fungsional. Keduanya dapat bernilai {@code true} bersamaan bila kedua jabatan
	 * terisi sekaligus, dan keduanya {@code false} untuk berkas yang tidak menetapkan jabatan apa pun
	 * — termasuk berkas pengunduran diri, karena getter jabatan struktural mengosongkan dirinya pada
	 * berkas semacam itu.</p>
	 *
	 * <p>Meski memiliki kolom sendiri di database, kolom itu hanya cerminan hasil perhitungan dan
	 * tidak boleh diperlakukan sebagai masukan.</p>
	 *
	 * @return {@code true} bila berkas ini menetapkan jabatan struktural
	 */
	@Column(name = "kenaikan_pangkat_golongan")
	public Boolean getKenaikanPangkatGolongan() {
		kenaikanPangkatGolongan = getJabatanStruktural() != null;
		return kenaikanPangkatGolongan;
	}

	/**
	 * Setter {@link #getKenaikanPangkatGolongan()}. Nilai yang disetel selalu ditimpa pembacaan
	 * berikutnya.
	 *
	 * @param kenaikanPangkatGolongan diabaikan secara efektif
	 */
	public void setKenaikanPangkatGolongan(Boolean kenaikanPangkatGolongan) {
		this.kenaikanPangkatGolongan = kenaikanPangkatGolongan;
	}

	/**
	 * <b>Properti turunan:</b> apakah berkas ini merupakan kenaikan pangkat jalur <b>fungsional</b>,
	 * dihitung semata dari terisinya {@link #getJabatanFungsional()}. Berpasangan dengan
	 * {@link #getKenaikanPangkatGolongan()}; berlaku catatan yang sama mengenai penimpaan nilai dan
	 * kolom database yang sekadar cerminan.
	 *
	 * @return {@code true} bila berkas ini menetapkan jabatan fungsional
	 */
	@Column(name = "kenaikan_pangkat_fungsional")
	public Boolean getKenaikanPangkatFungsional() {
		kenaikanPangkatFungsional = getJabatanFungsional() != null;
		return kenaikanPangkatFungsional;
	}

	/**
	 * Setter {@link #getKenaikanPangkatFungsional()}. Nilai yang disetel selalu ditimpa pembacaan
	 * berikutnya.
	 *
	 * @param kenaikanPangkatFungsional diabaikan secara efektif
	 */
	public void setKenaikanPangkatFungsional(Boolean kenaikanPangkatFungsional) {
		this.kenaikanPangkatFungsional = kenaikanPangkatFungsional;
	}

	/**
	 * Terhitung mulai tanggal (TMT) menurut SK, yaitu tanggal berlakunya kenaikan pangkat secara
	 * administratif. Boleh {@code null}.
	 *
	 * <p>Perlu ditegaskan bahwa <b>bukan field ini</b> yang dipakai aplikasi untuk memutuskan berkas
	 * mana yang sedang berlaku — peran itu dipegang pasangan {@link #getMulai()} dan
	 * {@link #getSampai()}. TMT di sini murni catatan administratif untuk keperluan cetak dan
	 * pelaporan, dan tidak ada mekanisme yang menjaga agar ia selaras dengan tanggal mulai. Keduanya
	 * dapat berbeda tanpa peringatan apa pun.</p>
	 *
	 * <p>Tidak diberi anotasi {@code @Temporal}, sehingga dipetakan mengikuti default penyedia
	 * persistence untuk {@link Date} — berbeda dengan {@link #getMulai()} dan {@link #getSampai()}
	 * yang menyatakan {@code TemporalType.DATE} secara eksplisit.</p>
	 *
	 * @return TMT menurut SK, atau {@code null} bila belum diisi
	 */
	@Column(name = "tmt")
	public Date getTmt() {
		return tmt;
	}

	/**
	 * Setter {@link #getTmt()}.
	 *
	 * @param tmt TMT menurut SK; boleh {@code null}
	 */
	public void setTmt(Date tmt) {
		this.tmt = tmt;
	}

	/**
	 * Jenis/pengelompokan usulan kenaikan pangkat, merujuk baris master
	 * {@link JenisKenaikanPangkat}. Getter ini meresolusi proxy lazy lewat {@code check(..)} dan
	 * menulis balik hasilnya, namun tidak melakukan substitusi maupun pengosongan.
	 *
	 * <p>Jenis di sini murni label pengelompokan dan pelaporan: tidak ada kode yang mencocokkan nama
	 * jenis tertentu untuk mempercabang aturan. Yang benar-benar menentukan perilaku berkas adalah
	 * {@link #getJenisPerubahan()}, yang merupakan properti berbeda dan bertipe teks. Kedua nama itu
	 * mudah tertukar — perhatikan baik-baik mana yang sedang dipakai.</p>
	 *
	 * <p>Kolomnya {@code nullable = true} sehingga berkas boleh tanpa jenis.</p>
	 *
	 * @return jenis kenaikan pangkat, atau {@code null} bila tidak diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_kenaikan_pangkat", nullable = true)
	public JenisKenaikanPangkat getJenisKenaikanPangkat() {
		jenisKenaikanPangkat = check(jenisKenaikanPangkat);
		return jenisKenaikanPangkat;
	}

	/**
	 * Setter {@link #getJenisKenaikanPangkat()}.
	 *
	 * @param jenisKenaikanPangkat jenis kenaikan pangkat; boleh {@code null}
	 */
	public void setJenisKenaikanPangkat(JenisKenaikanPangkat jenisKenaikanPangkat) {
		this.jenisKenaikanPangkat = jenisKenaikanPangkat;
	}

	/**
	 * Baris tabel gaji pokok yang ditetapkan berkas ini. <b>Getter destruktif:</b> setelah resolusi
	 * proxy lazy yang ditulis balik, field <b>dikosongkan</b> pada dua keadaan — bila
	 * {@link #getJenisPerubahan()} bernilai {@link #UBAH_PENGUNDURAN_DIRI}, atau bila
	 * {@link #getGajiLangsungDitentukanDisini()} menyala.
	 *
	 * <p>Keadaan kedua itulah kunci pemakaiannya: berkas dapat menetapkan gaji dengan dua cara yang
	 * saling meniadakan. Cara pertama menunjuk baris tabel gaji pokok lewat relasi ini; cara kedua
	 * menuliskan angkanya langsung di berkas lewat {@link #getNilaiGaji()}. Ketika cara kedua
	 * dipilih, relasi ini diputus supaya tidak ada dua sumber angka gaji yang saling bertentangan
	 * pada satu berkas. Pemutusan itu ikut tersimpan karena entity memakai akses properti —
	 * menyalakan penanda tersebut pada berkas yang sudah menunjuk baris gaji pokok akan benar-benar
	 * menghapus tautannya.</p>
	 *
	 * <p>Relasi ini juga menjadi sumber {@link #getGolongan()}: selama masih terisi, golongan berkas
	 * diturunkan dari golongan yang melekat pada baris gaji pokok ini, sehingga pasangan
	 * golongan-gaji dijamin selaras.</p>
	 *
	 * @return baris gaji pokok yang ditetapkan, atau {@code null} untuk berkas pengunduran diri
	 *         maupun berkas yang menuliskan nilai gajinya secara langsung
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gaji_pokok", nullable = true)
	public GajiPokok getGajiPokok() {
		gajiPokok = check(gajiPokok);

		if (getJenisPerubahan().equalsIgnoreCase(UBAH_PENGUNDURAN_DIRI) || getGajiLangsungDitentukanDisini()) {
			gajiPokok = null;
		}

		return gajiPokok;
	}

	/**
	 * Setter {@link #getGajiPokok()}. Nilai yang disetel akan dikosongkan getter-nya bila jenis
	 * perubahannya pengunduran diri atau bila nilai gaji ditentukan langsung di berkas ini.
	 *
	 * @param gajiPokok baris tabel gaji pokok; boleh {@code null}
	 */
	public void setGajiPokok(GajiPokok gajiPokok) {
		this.gajiPokok = gajiPokok;
	}

	/**
	 * Baris tabel insentif yang ditetapkan berkas ini. <b>Getter destruktif</b> dengan aturan
	 * pengosongan yang sama persis dengan {@link #getGajiPokok()}: dikosongkan pada berkas
	 * pengunduran diri, dan dikosongkan pula ketika {@link #getGajiLangsungDitentukanDisini()}
	 * menyala — dalam hal itu angkanya diambil dari {@link #getNilaiInsentif()}.
	 *
	 * @return baris insentif yang ditetapkan, atau {@code null} pada kedua keadaan di atas
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "insentif", nullable = true)
	public Insentif getInsentif() {
		insentif = check(insentif);
		if (getJenisPerubahan().equalsIgnoreCase(UBAH_PENGUNDURAN_DIRI) || getGajiLangsungDitentukanDisini()) {
			insentif = null;
		}
		return insentif;
	}

	/**
	 * Setter {@link #getInsentif()}. Nilai yang disetel akan dikosongkan getter-nya pada berkas
	 * pengunduran diri maupun bila nilai ditentukan langsung di berkas ini.
	 *
	 * @param insentif baris tabel insentif; boleh {@code null}
	 */
	public void setInsentif(Insentif insentif) {
		this.insentif = insentif;
	}

	/**
	 * Baris tabel tunjangan makan yang ditetapkan berkas ini. Getter ini meresolusi proxy lazy lewat
	 * {@code check(..)} dan menulis balik hasilnya, tetapi <b>tidak</b> ikut dikosongkan pada berkas
	 * pengunduran diri maupun saat nilai gaji ditentukan langsung -- berbeda dari
	 * {@link #getGajiPokok()} dan {@link #getInsentif()}.
	 *
	 * <p>Ketidakseragaman ini patut diketahui: berkas pengunduran diri tetap dapat membawa tautan
	 * tunjangan makan, sehingga pembaca yang mengandalkan "berkas pengunduran diri pasti kosong dari
	 * komponen penghasilan" akan keliru. Kolomnya {@code nullable = true}.</p>
	 *
	 * @return baris tunjangan makan yang ditetapkan, atau {@code null} bila tidak diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "makan", nullable = true)
	public Makan getMakan() {
		makan = check(makan);
		return makan;
	}

	/**
	 * Setter {@link #getMakan()}.
	 *
	 * @param makan baris tabel tunjangan makan; boleh {@code null}
	 */
	public void setMakan(Makan makan) {
		this.makan = makan;
	}

	/**
	 * Baris tabel tunjangan transport yang ditetapkan berkas ini. Berperilaku sama dengan
	 * {@link #getMakan()}: meresolusi proxy lazy dan menulis balik hasilnya, tanpa pengosongan
	 * bersyarat apa pun. Berlaku catatan ketidakseragaman yang sama.
	 *
	 * @return baris tunjangan transport yang ditetapkan, atau {@code null} bila tidak diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "transport", nullable = true)
	public Transport getTransport() {
		transport = check(transport);
		return transport;
	}

	/**
	 * Setter {@link #getTransport()}.
	 *
	 * @param transport baris tabel tunjangan transport; boleh {@code null}
	 */
	public void setTransport(Transport transport) {
		this.transport = transport;
	}

	/**
	 * Tautan ke alur SOP yang memproses persetujuan berkas ini -- properti yang dikontrakkan kelas
	 * induk {@link DataSop}. Getter meresolusi proxy lazy lewat {@code check(..)} dan menulis balik
	 * hasilnya.
	 *
	 * <p>Nilai {@code null} berarti berkas ini <b>tidak</b> melewati alur SOP, dan persetujuannya
	 * ditentukan langsung lewat kolom status. Perbedaan dua keadaan itu dijelaskan lengkap pada
	 * {@link #getStatus()}. Kolomnya {@code nullable = true}.</p>
	 *
	 * @return disposisi SOP yang menaungi berkas ini, atau {@code null} bila tanpa alur SOP
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Setter {@link #getDisposisiSop()} dengan <b>pengaman satu arah</b>: masukan yang {@code null}
	 * atau yang belum tersimpan (id-nya masih kosong) ditolak seluruhnya, sehingga tautan alur SOP
	 * yang sudah terpasang tidak dapat dilepas maupun ditimpa disposisi yang belum tersimpan.
	 *
	 * <p>Perlindungan ini penting karena tautan SOP-lah yang memindahkan penentuan persetujuan dari
	 * kotak centang manual ke alur resmi; membiarkannya dapat dilepas berarti membiarkan berkas yang
	 * sudah masuk alur dikembalikan ke jalur persetujuan manual. Sebagai akibatnya, pelepasan tautan
	 * hanya mungkin lewat UPDATE SQL langsung.</p>
	 *
	 * <p>Ekspresi kondisional di dalamnya menyisakan cabang yang tidak dapat tercapai: seluruh
	 * masukan yang {@code null} atau berid kosong sudah tersaring oleh penjagaan di baris pertama,
	 * sehingga sisa pemeriksaannya selalu menghasilkan penugasan masukan itu sendiri. Sisa ini tidak
	 * berbahaya, hanya berlebih; dibiarkan apa adanya agar diff terhadap riwayat tetap minimal.</p>
	 *
	 * @param disposisiSop disposisi SOP yang sudah tersimpan; {@code null} atau yang belum tersimpan
	 *                     diabaikan diam-diam
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
	}

	/**
	 * Penanda bahwa berkas kenaikan pangkat <b>sebelumnya</b> harus ditutup ketika berkas ini
	 * disetujui -- penutupan dilakukan layar pengelola dengan mematikan penanda menjabat berkas lama
	 * dan mengisi tanggal berakhirnya dengan kemarin.
	 *
	 * <p>Getter ini <b>memaksa nilainya menyala</b> bila {@link #getJenisPerubahan()} bernilai
	 * {@link #UBAH_PENGUNDURAN_DIRI}: berkas pengunduran diri dengan sendirinya mengakhiri jabatan
	 * yang sedang berjalan, jadi penanda ini tidak dapat dimatikan untuk berkas semacam itu.
	 * Pemaksaan itu ditulis ke field dan ikut tersimpan.</p>
	 *
	 * <p>Field yang masih {@code null} dibaca sebagai {@code false} -- default yang aman, karena
	 * menutup berkas sebelumnya adalah tindakan yang harus disengaja. Perlu diketahui bahwa
	 * penutupan tersebut hanya dijalankan bila berkas ini sudah berstatus disetujui, dan berjalan
	 * secara tertunda di luar transaksi penyimpanan.</p>
	 *
	 * @return {@code true} bila berkas sebelumnya harus ditutup; selalu {@code true} untuk berkas
	 *         pengunduran diri
	 */
	public Boolean getNonAktifkanJabatanSebelumnya() {
		if (getJenisPerubahan().equalsIgnoreCase(UBAH_PENGUNDURAN_DIRI)) {
			nonAktifkanJabatanSebelumnya = true;
		}
		return nonAktifkanJabatanSebelumnya == null ? false : nonAktifkanJabatanSebelumnya;
	}

	/**
	 * Setter {@link #getNonAktifkanJabatanSebelumnya()}. Nilai {@code false} yang disetel akan
	 * ditimpa menjadi {@code true} oleh getter bila jenis perubahan berkas ini pengunduran diri.
	 *
	 * @param nonAktifkanJabatanSebelumnya penanda penutupan berkas sebelumnya; boleh {@code null}
	 */
	public void setNonAktifkanJabatanSebelumnya(Boolean nonAktifkanJabatanSebelumnya) {
		this.nonAktifkanJabatanSebelumnya = nonAktifkanJabatanSebelumnya;
	}

	/**
	 * Jenis perubahan yang dibawa berkas ini, bernilai salah satu dari {@link #UBAH_JABATAN_DAN_GOLONGAN},
	 * {@link #UBAH_JABATAN}, {@link #UBAH_GOLONGAN}, atau {@link #UBAH_PENGUNDURAN_DIRI}.
	 *
	 * <p>Properti ini adalah <b>saklar perilaku utama</b> kelas ini: nilainya diperiksa oleh tujuh
	 * getter lain untuk memutuskan relasi mana yang dikosongkan dan penanda mana yang dipaksa
	 * menyala. Jangan tertukar dengan {@link #getJenisKenaikanPangkat()} yang merupakan relasi ke
	 * tabel master dan tidak mempengaruhi perilaku apa pun.</p>
	 *
	 * <p>Getter ini <b>tidak pernah mengembalikan {@code null}</b>. Tiga keadaan dijatuhkan ke nilai
	 * default {@link #UBAH_JABATAN_DAN_GOLONGAN}: field yang kosong, field yang hanya berisi spasi,
	 * dan — yang perlu dicatat khusus — field yang berisi teks {@code "Ubah Jabatan dan Golangan"},
	 * yaitu <b>ejaan keliru</b> yang pernah tersimpan ke database pada versi terdahulu. Penanganan
	 * ejaan keliru itu berfungsi sebagai migrasi data yang berjalan saat pembacaan, sehingga baris
	 * lama tidak perlu diperbaiki lebih dahulu. Jaminan "tidak pernah null" inilah yang membuat
	 * seluruh pemanggilan {@code equalsIgnoreCase} pada getter-getter lain aman dari kegagalan
	 * penunjuk kosong.</p>
	 *
	 * <p>Perbandingan di seluruh kelas ini memakai {@code equalsIgnoreCase}, jadi perbedaan huruf
	 * besar/kecil tidak menjadi masalah — namun spasi berlebih di tengah teks tetap membuat nilai
	 * tidak dikenali dan diperlakukan sebagai jenis default. Tidak ada anotasi kolom, sehingga nama
	 * kolomnya mengikuti nama properti; tidak ada pula batasan nilai di tingkat database, sehingga
	 * teks di luar keempat konstanta dapat tersimpan lewat impor atau SQL langsung dan akan
	 * diperlakukan sebagai jenis default.</p>
	 *
	 * @return jenis perubahan; tidak pernah {@code null}
	 */
	public String getJenisPerubahan() {
		return jenisPerubahan == null || jenisPerubahan.trim().isEmpty()
				|| jenisPerubahan.equalsIgnoreCase("Ubah Jabatan dan Golangan") ? UBAH_JABATAN_DAN_GOLONGAN
						: jenisPerubahan;
	}

	/**
	 * Setter {@link #getJenisPerubahan()}. Menerima teks apa adanya tanpa memeriksa bahwa nilainya
	 * termasuk salah satu dari empat konstanta yang dikenal.
	 *
	 * <p>Menyetel nilai ini bukan tindakan ringan: mengubahnya menjadi
	 * {@link #UBAH_PENGUNDURAN_DIRI} akan membuat getter-getter relasi mengosongkan golongan,
	 * jabatan, gaji pokok, dan insentif pada penyimpanan berikutnya, sekaligus memaksa dua penanda
	 * penonaktifan menyala. Perubahan itu tidak dapat dibatalkan hanya dengan mengembalikan jenis
	 * perubahannya, karena relasi yang telanjur terputus tidak dipulihkan.</p>
	 *
	 * @param jenisPerubahan jenis perubahan; {@code null}/kosong berarti memakai jenis default
	 */
	public void setJenisPerubahan(String jenisPerubahan) {
		this.jenisPerubahan = jenisPerubahan;
	}

	/**
	 * Cadangan data pengguna dalam bentuk teks JSON, dipakai untuk menyimpan peran-peran akun pegawai
	 * agar dapat dipulihkan atau ditetapkan ulang ketika berkas ini disetujui. Kolomnya bertipe
	 * {@code text} sehingga panjangnya praktis tidak dibatasi.
	 *
	 * <p>Getter ini <b>tidak pernah mengembalikan {@code null} maupun teks kosong</b>: field yang
	 * kosong dikembalikan sebagai objek JSON kosong ({@code "{}"}), sehingga pemanggil dapat langsung
	 * menguraikannya tanpa penjagaan tambahan. Yang tidak dijamin adalah kesahihan isinya — teks yang
	 * bukan JSON baru gagal saat diurai pemanggil. Perhatikan pula bahwa pemeriksaan hanya menyaring
	 * teks kosong setelah pemangkasan; nilai berisi spasi saja memang tertangani, tetapi teks tak
	 * sah lainnya diteruskan apa adanya.</p>
	 *
	 * <p>Isi cadangan ini dituliskan kembali ke akun pengguna pegawai oleh layar pengelola, dan hanya
	 * bila berkas sudah berstatus disetujui sekaligus sedang berlaku. Karena penulisan itu berjalan
	 * tertunda di luar transaksi penyimpanan, perubahan peran tidak langsung terlihat begitu berkas
	 * disimpan.</p>
	 *
	 * @return teks JSON data pengguna; tidak pernah {@code null} -- minimal {@code "{}"}
	 */
	@Column(columnDefinition = "text")
	public String getJsonDataPengguna() {
		return jsonDataPengguna == null || jsonDataPengguna.trim().isEmpty() ? new JSONObject().toString()
				: jsonDataPengguna;
	}

	/**
	 * Setter {@link #getJsonDataPengguna()}. Menyimpan teks apa adanya tanpa memeriksa bahwa isinya
	 * JSON yang sah.
	 *
	 * @param jsonDataPengguna teks JSON data pengguna; {@code null}/kosong berarti objek kosong saat
	 *                         dibaca
	 */
	public void setJsonDataPengguna(String jsonDataPengguna) {
		this.jsonDataPengguna = jsonDataPengguna;
	}

	/**
	 * Penanda bahwa <b>akun pengguna</b> pegawai harus dinonaktifkan ketika berkas ini disetujui.
	 * Penonaktifan yang dijalankan layar pengelola mencakup pegawainya sendiri beserta seluruh akun
	 * pengguna yang tertaut kepadanya.
	 *
	 * <p>Seperti {@link #getNonAktifkanJabatanSebelumnya()}, getter ini <b>memaksa nilainya menyala</b>
	 * bila {@link #getJenisPerubahan()} bernilai {@link #UBAH_PENGUNDURAN_DIRI} — pegawai yang
	 * mengundurkan diri dengan sendirinya kehilangan akses. Pemaksaan itu ditulis ke field dan ikut
	 * tersimpan, sehingga penanda ini tidak dapat dimatikan untuk berkas pengunduran diri.</p>
	 *
	 * <p>Field yang masih {@code null} dibaca sebagai {@code false}. Penanda ini berpasangan dengan
	 * {@link #getAktifkanPengguna()}; keduanya tidak saling meniadakan di tingkat model, sehingga
	 * secara teknis dapat menyala bersamaan — keadaan yang tidak bermakna dan sebaiknya dicegah
	 * lapisan pengisi.</p>
	 *
	 * @return {@code true} bila akun pengguna harus dinonaktifkan; selalu {@code true} untuk berkas
	 *         pengunduran diri
	 */
	public Boolean getNonAktifkanPengguna() {
		if (getJenisPerubahan().equalsIgnoreCase(UBAH_PENGUNDURAN_DIRI)) {
			nonAktifkanPengguna = true;
		}
		return nonAktifkanPengguna == null ? false : nonAktifkanPengguna;
	}

	/**
	 * Setter {@link #getNonAktifkanPengguna()}. Nilai {@code false} yang disetel akan ditimpa menjadi
	 * {@code true} oleh getter bila jenis perubahan berkas ini pengunduran diri.
	 *
	 * @param nonAktifkanPengguna penanda penonaktifan akun; boleh {@code null}
	 */
	public void setNonAktifkanPengguna(Boolean nonAktifkanPengguna) {
		this.nonAktifkanPengguna = nonAktifkanPengguna;
	}

	/**
	 * Penanda bahwa <b>akun pengguna</b> pegawai harus diaktifkan kembali ketika berkas ini
	 * disetujui, mencakup pegawainya sendiri beserta seluruh akun pengguna yang tertaut kepadanya --
	 * kebalikan dari {@link #getNonAktifkanPengguna()}.
	 *
	 * <p>Berbeda dengan pasangannya, getter ini <b>tidak</b> dipaksa oleh jenis perubahan apa pun:
	 * ia sekadar membaca field, dengan {@code null} dibaca sebagai {@code false}. Asimetri itu masuk
	 * akal — pengunduran diri hanya perlu memaksa penonaktifan, tidak pernah pengaktifan.</p>
	 *
	 * <p>Sebagaimana penanda saudaranya, tindakan yang dipicunya hanya berjalan bila berkas sudah
	 * berstatus disetujui, dan dijalankan secara tertunda di luar transaksi penyimpanan.</p>
	 *
	 * @return {@code true} bila akun pengguna harus diaktifkan kembali; {@code false} bila tidak
	 *         ditentukan
	 */
	public Boolean getAktifkanPengguna() {
		return aktifkanPengguna == null ? false : aktifkanPengguna;
	}

	/**
	 * Setter {@link #getAktifkanPengguna()}. Menerima {@code null}, yang dibaca kembali sebagai
	 * {@code false}.
	 *
	 * @param aktifkanPengguna penanda pengaktifan akun; boleh {@code null}
	 */
	public void setAktifkanPengguna(Boolean aktifkanPengguna) {
		this.aktifkanPengguna = aktifkanPengguna;
	}

	/**
	 * Penanda bahwa berkas ini disertai <b>kenaikan gaji berkala</b> yang berulang menurut jangka
	 * bulan tertentu. Bila menyala, {@link #getKenaikanBerkalaBulan()} menentukan jarak antar
	 * kenaikan, dan perhitungan gaji pokok pegawai akan menaikkan gaji secara berkala berdasarkan
	 * jangka itu terhitung sejak berkas mulai berlaku.
	 *
	 * <p>Jangan tertukar dengan entity {@link KenaikanGajiBerkala} yang mencatat peristiwa kenaikan
	 * berkala satu per satu sebagai berkas tersendiri. Penanda di sini adalah pengaturan otomatis
	 * yang menempel pada berkas kenaikan pangkat; keduanya jalur yang berbeda dan tidak saling
	 * memutakhirkan.</p>
	 *
	 * <p>Field yang masih {@code null} dibaca sebagai {@code false} -- default yang aman, karena
	 * kenaikan otomatis harus dinyalakan dengan sengaja.</p>
	 *
	 * @return {@code true} bila berkas ini menyertakan kenaikan gaji berkala otomatis
	 */
	public Boolean getTerdapatKenaikanGajiBerkala() {
		return terdapatKenaikanGajiBerkala == null ? false : terdapatKenaikanGajiBerkala;
	}

	/**
	 * Setter {@link #getTerdapatKenaikanGajiBerkala()}. Menerima {@code null}, yang dibaca kembali
	 * sebagai {@code false}. Mematikan penanda ini juga membuat {@link #getKenaikanBerkalaBulan()}
	 * mengosongkan dirinya.
	 *
	 * @param terdapatKenaikanGajiBerkala penanda kenaikan berkala; boleh {@code null}
	 */
	public void setTerdapatKenaikanGajiBerkala(Boolean terdapatKenaikanGajiBerkala) {
		this.terdapatKenaikanGajiBerkala = terdapatKenaikanGajiBerkala;
	}

	/**
	 * Jarak <b>bulan</b> antar kenaikan gaji berkala yang menyertai berkas ini. <b>Getter
	 * destruktif:</b> field dikosongkan bila {@link #getTerdapatKenaikanGajiBerkala()} padam atau
	 * bila {@link #getGajiLangsungDitentukanDisini()} menyala.
	 *
	 * <p>Kedua syarat itu menjaga agar angka jangka ini tidak tertinggal sebagai data yatim.
	 * Kenaikan berkala hanya bermakna bila penandanya menyala, dan tidak dapat diterapkan bila nilai
	 * gaji ditulis langsung di berkas alih-alih diambil dari tabel gaji pokok — sebab kenaikan
	 * berkala bekerja dengan berpindah baris pada tabel tersebut. Pengosongan ikut tersimpan karena
	 * entity memakai akses properti, sehingga mematikan penanda kenaikan berkala benar-benar
	 * menghapus angka jangkanya dari database pada penyimpanan berikutnya.</p>
	 *
	 * <p>Tidak ada pemeriksaan bahwa nilainya positif atau masuk akal; angka nol atau negatif dapat
	 * tersimpan dan baru menimbulkan masalah saat perhitungan gaji dijalankan.</p>
	 *
	 * @return jarak bulan antar kenaikan berkala, atau {@code null} bila kenaikan berkala tidak
	 *         berlaku bagi berkas ini
	 */
	public Integer getKenaikanBerkalaBulan() {
		if (!getTerdapatKenaikanGajiBerkala() || getGajiLangsungDitentukanDisini()) {
			kenaikanBerkalaBulan = null;
		}
		return kenaikanBerkalaBulan;
	}

	/**
	 * Setter {@link #getKenaikanBerkalaBulan()}. Nilai yang disetel akan dikosongkan getter-nya bila
	 * penanda kenaikan berkala padam atau nilai gaji ditentukan langsung di berkas ini.
	 *
	 * @param kenaikanBerkalaBulan jarak bulan antar kenaikan berkala; boleh {@code null}
	 */
	public void setKenaikanBerkalaBulan(Integer kenaikanBerkalaBulan) {
		this.kenaikanBerkalaBulan = kenaikanBerkalaBulan;
	}

	/**
	 * Penanda bahwa nilai gaji dan insentif <b>ditulis langsung di berkas ini</b> lewat
	 * {@link #getNilaiGaji()} dan {@link #getNilaiInsentif()}, alih-alih diambil dari baris tabel
	 * master.
	 *
	 * <p>Penanda ini mengendalikan tiga getter sekaligus: {@link #getGajiPokok()} dan
	 * {@link #getInsentif()} mengosongkan tautan masternya, dan {@link #getKenaikanBerkalaBulan()}
	 * mengosongkan angka jangkanya. Dengan begitu satu berkas hanya punya satu sumber angka gaji, dan
	 * dua mekanisme penetapan gaji tidak dapat aktif bersamaan.</p>
	 *
	 * <p>Perlu dicatat bahwa penanda ini <b>berbeda</b> dari {@link #getGajiPokokOtomatisMasaKerja()}:
	 * yang ini menuliskan angka secara manual, sedangkan yang itu justru menyerahkan pemilihan baris
	 * gaji pokok kepada perhitungan masa kerja. Field yang masih {@code null} dibaca sebagai
	 * {@code false}, yaitu perilaku baku mengambil gaji dari tabel master.</p>
	 *
	 * @return {@code true} bila nilai gaji ditulis langsung di berkas ini
	 */
	public Boolean getGajiLangsungDitentukanDisini() {
		return gajiLangsungDitentukanDisini == null ? false : gajiLangsungDitentukanDisini;
	}

	/**
	 * Setter {@link #getGajiLangsungDitentukanDisini()}. Menyalakan penanda ini akan memutus tautan
	 * gaji pokok dan insentif serta mengosongkan jangka kenaikan berkala pada pembacaan berikutnya,
	 * dan pemutusan itu ikut tersimpan.
	 *
	 * @param gajiLangsungDitentukanDisini penanda penetapan gaji langsung; boleh {@code null}
	 */
	public void setGajiLangsungDitentukanDisini(Boolean gajiLangsungDitentukanDisini) {
		this.gajiLangsungDitentukanDisini = gajiLangsungDitentukanDisini;
	}

	/**
	 * Penanda "Penggajian Otomatis Berdasarkan Masa Kerja". Bila {@code true}, Gaji Pokok dihitung
	 * otomatis dari golongan/penggajian-berdasarkan + masa kerja pegawai (tabel master Gaji Pokok),
	 * bukan dipilih manual. Default {@code false} (mengikuti proses penggajian yang berjalan saat ini).
	 *
	 * <p>{@code @NotAudited}: kolom ditambahkan otomatis ke tabel utama oleh hbm2ddl=update, dan
	 * sengaja tidak diaudit Envers agar tidak menuntut kolom baru pada tabel audit ({@code _AUD}).</p>
	 *
	 * <p>Perlu dibedakan dari {@link #getGajiLangsungDitentukanDisini()}: penanda itu menuliskan
	 * angka gaji secara manual di berkas ini, sedangkan penanda ini tetap memakai tabel master namun
	 * menyerahkan pemilihan barisnya kepada perhitungan masa kerja. Perhatikan pula bahwa masa kerja
	 * yang dipakai di jalur tersebut dihitung dari selisih tanggal masuk dan keluar pegawai, bukan
	 * dari rumus pada entity {@link MasaKerja} -- dua pengertian masa kerja yang berjalan paralel di
	 * aplikasi ini.</p>
	 *
	 * <p>Berbeda dari kedua penanda lain, getter ini tidak mengosongkan relasi apa pun; ia sekadar
	 * membaca field dengan {@code null} dibaca sebagai {@code false}.</p>
	 *
	 * @return {@code true} bila gaji pokok dipilih otomatis berdasarkan masa kerja
	 */
	@org.hibernate.envers.NotAudited
	public Boolean getGajiPokokOtomatisMasaKerja() {
		return gajiPokokOtomatisMasaKerja == null ? false : gajiPokokOtomatisMasaKerja;
	}

	/**
	 * Setter {@link #getGajiPokokOtomatisMasaKerja()}. Menerima {@code null}, yang dibaca kembali
	 * sebagai {@code false}. Perubahan pada penanda ini tidak terekam tabel audit Envers karena
	 * propertinya {@code @NotAudited}.
	 *
	 * @param gajiPokokOtomatisMasaKerja penanda penggajian otomatis; boleh {@code null}
	 */
	public void setGajiPokokOtomatisMasaKerja(Boolean gajiPokokOtomatisMasaKerja) {
		this.gajiPokokOtomatisMasaKerja = gajiPokokOtomatisMasaKerja;
	}

	/**
	 * Nilai gaji yang ditulis <b>langsung</b> di berkas ini, dipakai ketika
	 * {@link #getGajiLangsungDitentukanDisini()} menyala dan tautan ke tabel gaji pokok karenanya
	 * diputus.
	 *
	 * <p>Field diinisialisasi {@code 0.0} dan getter mengembalikan {@code 0.0} pula bila field
	 * kosong, sehingga nilainya <b>tidak pernah {@code null}</b> dan aman dipakai dalam perhitungan
	 * tanpa penjagaan. Perlu disadari bahwa nol di sini <b>tidak dapat dibedakan</b> dari "belum
	 * diisi": berkas yang menyalakan penetapan gaji langsung namun lupa mengisi angkanya akan
	 * menetapkan gaji nol, bukan menimbulkan kesalahan.</p>
	 *
	 * <p>Tidak ada anotasi kolom, sehingga nama kolomnya mengikuti nama properti. Nilai ini diabaikan
	 * sepenuhnya selama penetapan gaji langsung tidak menyala — tetapi tetap tersimpan, sehingga
	 * angka sisa dari percobaan sebelumnya bisa tertinggal di baris dan mendadak berlaku bila
	 * penandanya dinyalakan kembali.</p>
	 *
	 * @return nilai gaji yang ditetapkan langsung; tidak pernah {@code null}
	 */
	public Double getNilaiGaji() {
		return nilaiGaji == null ? 0.0 : nilaiGaji;
	}

	/**
	 * Setter {@link #getNilaiGaji()}. Menerima {@code null}, yang dibaca kembali sebagai {@code 0.0}.
	 * Tidak ada pemeriksaan bahwa nilainya tidak negatif.
	 *
	 * @param nilaiGaji nilai gaji yang ditetapkan langsung; boleh {@code null}
	 */
	public void setNilaiGaji(Double nilaiGaji) {
		this.nilaiGaji = nilaiGaji;
	}

	/**
	 * Nilai insentif yang ditulis <b>langsung</b> di berkas ini, pasangan {@link #getNilaiGaji()} dan
	 * dipakai pada keadaan yang sama, yaitu ketika {@link #getGajiLangsungDitentukanDisini()} menyala
	 * dan tautan ke tabel insentif diputus.
	 *
	 * <p>Berlaku catatan yang sama: nilainya tidak pernah {@code null} karena {@code null} dibaca
	 * sebagai {@code 0.0}, nol tidak dapat dibedakan dari "belum diisi", dan angka sisa tetap
	 * tersimpan meski penandanya sedang padam.</p>
	 *
	 * @return nilai insentif yang ditetapkan langsung; tidak pernah {@code null}
	 */
	public Double getNilaiInsentif() {
		return nilaiInsentif == null ? 0.0 : nilaiInsentif;
	}

	/**
	 * Setter {@link #getNilaiInsentif()}. Menerima {@code null}, yang dibaca kembali sebagai
	 * {@code 0.0}. Tidak ada pemeriksaan bahwa nilainya tidak negatif.
	 *
	 * @param nilaiInsentif nilai insentif yang ditetapkan langsung; boleh {@code null}
	 */
	public void setNilaiInsentif(Double nilaiInsentif) {
		this.nilaiInsentif = nilaiInsentif;
	}

}
