package ais.database.model.akunting;
/* ENHANCED_PENGGUNAAN_ANGGARAN_MEMORY_SAFE_2026_06_03 - Java 1.6/1.7 compatible. */

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;
import org.json.JSONArray;

import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;

/**
 * Entity <b>pengajuan kas kecil</b> (<i>petty cash</i>) — satu baris tabel
 * {@code akunting.kas_kecil}, mewakili satu permintaan penggunaan dana tunai kecil yang
 * diajukan sebuah satuan kerja, ditelusuri melalui alur SOP, lalu (bila disetujui) diposting
 * ke jurnal umum dan/atau diganti dananya lewat {@link PenggantianKasKecil}.
 *
 * <h2>Posisi dalam alur akunting</h2>
 * <ol>
 *   <li><b>Master saldo</b> — {@link JenisKasKecil} memegang "dompet" kas kecil: akun kas
 *       kecil, akun penutup, saldo awal, dan satuan kerja pemiliknya.</li>
 *   <li><b>Pengajuan</b> — baris {@code KasKecil} dibuat lewat {@code KasKecilAction}
 *       (kode di-generate otomatis, {@link #getNilai() nilai} = jumlah yang diminta,
 *       {@link #getFormula() formula} = rincian akun debet dalam JSON).</li>
 *   <li><b>Disposisi / persetujuan</b> — pengajuan ditempelkan pada satu
 *       {@link DisposisiSop} (lihat {@link DataSop}). Semua penanda persetujuan yang
 *       terlihat di UI ({@link #getDibuatOleh()}, {@link #getDisetujuiOleh()},
 *       {@link #getTanggalPersetujuan()}, {@link #getStatus()}, {@link #getAktif()})
 *       <b>diturunkan ulang dari disposisi itu setiap kali getter-nya dibaca</b> —
 *       kolom di tabel hanya menjadi cache. Jalur manual (tanpa SOP) tetap didukung:
 *       {@code KasKecilAction} boleh mengisi {@code disetujuiOleh}/{@code tanggalPersetujuan}
 *       langsung dari dropdown status.</li>
 *   <li><b>Pencairan / penggantian</b> — {@link PenggantianKasKecil} adalah dokumen
 *       pengisian ulang dompet; bila pengajuan ini sudah masuk ke sebuah penggantian, maka
 *       disposisi, status, dan tanggal transaksinya <b>diambil alih</b> oleh dokumen
 *       penggantian tersebut (lihat {@link #getDisposisiSop()}, {@link #getStatus()},
 *       {@link #getTanggalTransaksi()}). Relasi {@link #getKasBesar()} dipakai untuk
 *       pengajuan yang pendanaannya berasal dari kas besar.</li>
 *   <li><b>Posting jurnal</b> — {@code PostingKasKecilAction} membaca daftar pengajuan yang
 *       sudah disetujui dan bernilai bukan nol, mengurai {@link #getFormula()} menjadi
 *       beberapa baris debet, dan mengkredit {@code jenisKasKecil.akun}. Bila
 *       {@link #getMerupakanPenutupanKasKecil()} bernilai {@code true}, ditambahkan satu
 *       baris debet ke {@code jenisKasKecil.akunPenutupKasKecil} sebesar
 *       {@link #getSisa()}. Jejak posting disimpan di {@link #getPostingHistory()}.</li>
 * </ol>
 *
 * <h2>Relasi utama</h2>
 * <ul>
 *   <li>{@link JenisKasKecil} — dompet/master saldo, sekaligus sumber akun jurnal dan
 *       (secara tidak langsung) sumber {@link #getSatuanKerja() satuan kerja}.</li>
 *   <li>{@link DisposisiSop} — instance alur persetujuan; sumber kebenaran status.</li>
 *   <li>{@link PenggantianKasKecil} — dokumen pengisian ulang; relasi dua arah
 *       ({@code PenggantianKasKecil.kasKecil} adalah sisi sebaliknya).</li>
 *   <li>{@link KasBesar} — sumber dana bila pengajuan didanai dari kas besar.</li>
 *   <li>{@link PostingHistory} — penanda bahwa baris ini sudah masuk buku besar.</li>
 *   <li>{@link NomorSuratAlurKeuangan} — konfigurasi penomoran surat; default statis
 *       {@link NomorSuratAlurKeuangan#KAS_KECIL_DATA}.</li>
 *   <li>{@link SatuanKerja}, {@link Tbmuser} — unit pengaju dan pengguna.</li>
 * </ul>
 *
 * <h2>Pengelompokan method</h2>
 * <ul>
 *   <li><b>Jejak audit yang dideklarasikan ulang</b> — {@link #getId()}, {@link #getOleh()},
 *       {@link #getOlehId()}, {@link #getTanggal_dirubah()} beserta setter-nya, plus
 *       {@link #onUpdate()}. Lihat catatan tentang {@code GeneralValueObject} di bawah.</li>
 *   <li><b>Getter/setter polos</b> — {@code kode}, {@code nama}, {@code keterangan},
 *       {@code penggantianKasKecil}, {@code postingHistory}, {@code kasBesar},
 *       {@code tampilkanAnggaran}, {@code merupakanPenutupanKasKecil}, dan seluruh setter.</li>
 *   <li><b>Getter berlogika (menulis balik ke field)</b> — {@link #getAktif()},
 *       {@link #getJenisKasKecil()}, {@link #getDibuatOleh()}, {@link #getDisetujuiOleh()},
 *       {@link #getTanggalPersetujuan()}, {@link #getStatus()}, {@link #getSatuanKerja()},
 *       {@link #getKodeUnik()}, {@link #getDisposisiSop()}, {@link #getTahun()},
 *       {@link #getBulan()}, {@link #getNomorSuratAlurKeuangan()},
 *       {@link #getTanggalTransaksi()}, {@link #getSisa()}.</li>
 *   <li><b>Getter berlogika tanpa tulis balik</b> — {@link #getNilai()}, {@link #getSaldo()},
 *       {@link #getTanggal()}, {@link #getTanggalPembuatan()}, {@link #getFormula()},
 *       {@link #getKode()}, {@link #getNama()}: hanya menormalkan nilai kembali.</li>
 *   <li><b>Setter berlogika</b> — {@link #setOleh(String)}, {@link #setOlehId(String)}
 *       (menolak nilai kosong), {@link #setStatus(String)} (membersihkan penanda
 *       persetujuan saat "Ditolak"), {@link #setDisposisiSop(DisposisiSop)} (menolak
 *       disposisi tanpa id).</li>
 *   <li><b>Konstanta</b> — {@link #PENGAJUAN}, {@link #DISETUJU}, {@link #DITOLAK},
 *       {@link #DEFAULT_FORMULA}.</li>
 * </ul>
 * <p>Tidak ada method utilitas/query statis di class ini; pencarian dan kalkulasi saldo
 * hidup di lapisan action ({@code JenisKasKecilAction.hitungSaldo(...)},
 * {@code KasKecilAction}, {@code MonitorKasKecilDashboard}).</p>
 *
 * <h2>Verifikasi pola "flag {@code aktif} satu arah" — <b>IDENTIK</b></h2>
 * <p>Dugaan dari sesi {@code PengajuanMahasiswa} <b>terbukti benar dan identik baris demi
 * baris</b> di sini. {@link #getAktif()} memaksa field {@code aktif} menjadi {@code false}
 * bila (a) disposisinya sendiri sudah nonaktif, atau (b) alur berhenti di simpul yang
 * ditandai sebagai titik penolakan; ia <b>tidak pernah</b> mengembalikan nilai ke
 * {@code true}. Karena {@code aktif} adalah properti yang dipetakan (bukan
 * {@code @Transient}), nilai {@code false} itu ikut ter-<i>flush</i> ke database begitu
 * entity berada dalam session yang dirty-check — jadi sekadar <b>membaca</b> daftar
 * pengajuan bisa mengubah data. Satu-satunya jalan kembali adalah pemanggilan eksplisit
 * {@link #setAktif(Boolean)} (mis. checkbox "Aktif" di grid {@code KasKecilAction}).</p>
 * <p>Dampaknya di sini lebih tajam daripada di {@code PengajuanMahasiswa}, karena
 * {@code JenisKasKecilAction.hitungSaldo(...)} menjumlahkan {@code nilai} <b>hanya</b> dari
 * baris dengan {@code aktif IS NULL OR aktif = true}. Sekali sebuah pengajuan dipaksa
 * nonaktif, nilainya berhenti mengurangi saldo dompet kas kecil — dan tetap begitu meski
 * penolakannya kemudian dicabut.</p>
 *
 * <h2>Verifikasi pola berulang lain</h2>
 * <ul>
 *   <li><b>Getter yang menulis balik ke field/DB</b> — <b>ada, dan banyak</b>: 14 getter
 *       (daftar di atas). Tiga di antaranya menulis <i>tanpa syarat</i> setiap kali dibaca:
 *       {@link #getKodeUnik()}, {@link #getTanggalTransaksi()}, dan {@link #getSisa()}.</li>
 *   <li><b>Getter yang menutup session Hibernate</b> — <b>tidak ada secara langsung</b> di
 *       file ini. Namun getter relasi memanggil {@code check(...)} milik
 *       {@link ais.database.model.GeneralValueObject}, yang pada tahap penyelamat terakhir
 *       membuka session baru sendiri dan menutupnya di {@code finally}. Jadi biaya "buka +
 *       tutup session" tetap bisa muncul dari jalur getter di sini, hanya saja tidak
 *       ditulis di class ini.</li>
 *   <li><b>Penelan {@code LazyInitializationException}</b> — {@link #getTanggalPersetujuan()}
 *       membungkus seluruh logikanya dengan {@code try/catch} bertanda
 *       {@code auto-audit(empty-catch)}, sehingga kegagalan proxy detached tidak
 *       menggagalkan render halaman.</li>
 * </ul>
 *
 * <h2>Catatan teknis: field warisan yang dideklarasikan ulang</h2>
 * <p>Class ini turunan {@link DataSop}, yang turunan
 * {@link ais.database.model.GeneralValueObject}. Induk-induk itu <b>bukan</b> {@code @Entity}
 * maupun {@code @MappedSuperclass} — keduanya POJO abstrak biasa, sehingga Hibernate tidak
 * memetakan properti apa pun dari sana. Karena itu {@code id}, {@code oleh}, {@code olehId},
 * dan {@code tanggal_dirubah} <b>wajib</b> dideklarasikan ulang di setiap entity konkret;
 * ini keharusan teknis, bukan duplikasi yang perlu "dibersihkan".</p>
 *
 * <h2>Kuirk yang perlu diketahui</h2>
 * <ul>
 *   <li>{@link #getKodeUnik()} dipetakan {@code @Column(unique = true)} padahal nilainya
 *       <i>dihitung ulang</i> dari {@code kode} + id disposisi setiap kali dibaca — dan
 *       {@code kode} boleh {@code null}, sehingga hasilnya bisa berupa string
 *       {@code "null_123"}.</li>
 *   <li>{@link #getDisetujuiOleh()} dan {@link #getTanggalPersetujuan()} diakhiri blok yang
 *       <b>menghapus</b> nilai bila disposisi ada tetapi belum punya langkah "setuju" —
 *       blok ini menimpa hasil pencarian {@code kasBesar}/{@code penggantianKasKecil} yang
 *       baru saja dihitung di atasnya.</li>
 *   <li>{@link #setDisposisiSop(DisposisiSop)} mengandung ternary yang kondisinya tidak
 *       pernah benar setelah guard di barisnya sendiri; efektifnya penugasan biasa.</li>
 *   <li>{@link #toString()} membaca field {@code nama} mentah (bukan {@link #getNama()}),
 *       sehingga bisa mengembalikan {@code null}.</li>
 * </ul>
 *
 * @see JenisKasKecil
 * @see PenggantianKasKecil
 * @see KasBesar
 * @see DisposisiSop
 * @see DataSop
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "akunting", name = "kas_kecil")
public class KasKecil extends DataSop {

	/**
	 * Nomor versi serialisasi Java. Nilainya sengaja dipertahankan sejak file di-generate
	 * Hibernate Tools; angka yang sama juga muncul di beberapa entity akunting lain
	 * (mis. {@link JenisKasKecil}) karena hasil salin-tempel generator, bukan karena
	 * ada hubungan tipe di antara mereka.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama baris ({@code akunting.kas_kecil.id}), IDENTITY dari database. */
	private Long id;

	/** Nama pengguna terakhir yang menyimpan baris ini (jejak audit sederhana). */
	private String oleh;

	/** Identitas (username/NIP) pengguna terakhir yang menyimpan baris ini. */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna terakhir yang menyimpan baris ini.
	 *
	 * @return identitas pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan identitas pengguna terakhir yang menyimpan baris ini.
	 *
	 * <p><b>Perhatian:</b> nilai {@code null} atau string kosong <b>diabaikan diam-diam</b>
	 * (method langsung {@code return}), sehingga jejak audit lama tidak bisa dikosongkan
	 * lewat setter ini. Perilaku ini disengaja: pengisi otomatis
	 * {@code AuditTimestampInterceptor} kadang dipanggil dari konteks tanpa sesi pengguna,
	 * dan nilai kosong dari sana tidak boleh menghapus jejak yang sudah ada.</p>
	 *
	 * @param olehId identitas pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna terakhir yang menyimpan baris ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong diabaikan
	 * diam-diam sehingga jejak lama tidak terhapus.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang menyimpan baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum
	 * {@code UPDATE} dijalankan, dan mendelegasikan pemutakhiran stempel waktu ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 *
	 * <p>Jangan dipanggil manual dari kode aplikasi. Perhatikan bahwa deklarasi field
	 * {@code tanggal_dirubah} sengaja ditempel pada baris yang sama oleh perkakas audit
	 * otomatis repo ini — nilai awalnya {@code ais.ui.util.WaktuUtil.getDate()} sehingga
	 * baris baru pun sudah punya stempel waktu sebelum sempat di-{@code update}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya diisi otomatis oleh {@link #onUpdate()}; pengisian manual hanya dilakukan
	 * saat migrasi/impor data.</p>
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk objek baru
	 *         karena field-nya diinisialisasi saat konstruksi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Status awal/netral sebuah pengajuan: belum disetujui dan belum ditolak.
	 *
	 * <p>Dipakai sebagai nilai balik cadangan {@link #getStatus()} bila kolom status masih
	 * kosong.</p>
	 */
	public static final String PENGAJUAN = "Pengajuan";

	/**
	 * Status pengajuan yang sudah disetujui — prasyarat agar baris muncul di layar posting
	 * jurnal ({@code PostingKasKecilAction}).
	 *
	 * <p>Perhatikan ejaannya yang tidak lengkap ({@code DISETUJU}, bukan {@code DISETUJUI});
	 * nilai string-nya sendiri {@code "Disetujui"}. Nama konstanta ini dipakai lintas class
	 * akunting sehingga tidak diubah.</p>
	 */
	public static final String DISETUJU = "Disetujui";

	/** Status pengajuan yang ditolak dalam alur SOP. */
	public static final String DITOLAK = "Ditolak";

	/**
	 * Representasi teks entity, dipakai komponen ZK (combobox/label) yang menampilkan objek
	 * apa adanya.
	 *
	 * <p><b>Kuirk:</b> membaca field {@code nama} secara langsung, bukan lewat
	 * {@link #getNama()}, sehingga nilai yang belum di-{@code trim} dan nilai {@code null}
	 * ikut dikembalikan apa adanya.</p>
	 *
	 * @return nama pengajuan, bisa {@code null}
	 */
	public String toString() {
		return nama;
	}

	/** Nomor/kode agenda pengajuan; di-generate {@code KasKecilAction.generateCode(...)}. */
	private String kode;

	/** Judul singkat pengajuan (kolom {@code nama}, wajib isi). */
	private String nama;

	/** Uraian bebas keperluan penggunaan dana. */
	private String keterangan;

	/** Dompet/master kas kecil yang dibebani pengajuan ini. */
	private JenisKasKecil jenisKasKecil;

	/** Nominal dana yang diminta (rupiah). */
	private Double nilai;

	/** Penanda baris masih berlaku; dipaksa {@code false} satu arah oleh {@link #getAktif()}. */
	private Boolean aktif;

	/** Tanggal pengajuan (kolom {@code tanggal_pengajuan}), acuan perhitungan saldo. */
	private Date tanggal;

	/** Saldo dompet kas kecil pada saat pengajuan, hasil {@code JenisKasKecilAction.hitungSaldo}. */
	private Double saldo;

	/** Sisa dana setelah pengajuan ini, dihitung ulang oleh {@link #getSisa()}. */
	private Double sisa;

	/** Rincian akun debet dalam bentuk JSON array; lihat {@link #getFormula()}. */
	private String formula;

	/** Satuan kerja pengaju; biasanya diturunkan dari {@link #jenisKasKecil}. */
	private SatuanKerja satuanKerja;

	/** Instance alur SOP yang menaungi pengajuan ini — sumber kebenaran status. */
	private DisposisiSop disposisiSop;

	/** Pengguna pembuat pengajuan; bisa ditimpa oleh pengaju langkah awal disposisi. */
	private Tbmuser dibuatOleh;

	/** Pengguna penyetuju; bisa ditimpa/ dikosongkan berdasar disposisi. */
	private Tbmuser disetujuiOleh;

	/** Waktu persetujuan; bisa ditimpa/dikosongkan berdasar disposisi. */
	private Date tanggalPersetujuan;

	/** Waktu pembuatan baris. */
	private Date tanggalPembuatan;

	/** Penanda UI: tampilkan blok informasi anggaran pada formulir/laporan. */
	private Boolean tampilkanAnggaran;

	/** Status pengajuan; salah satu dari {@link #PENGAJUAN}/{@link #DISETUJU}/{@link #DITOLAK}. */
	private String status;

	/** Dokumen penggantian (pengisian ulang dompet) yang memuat pengajuan ini. */
	private PenggantianKasKecil penggantianKasKecil;

	/** Kas besar sumber dana, bila pengajuan didanai dari kas besar. */
	private KasBesar kasBesar;

	/** Jejak posting ke buku besar; {@code null} berarti belum diposting. */
	private PostingHistory postingHistory;

	/** Konfigurasi penomoran surat alur keuangan untuk dokumen ini. */
	private NomorSuratAlurKeuangan nomorSuratAlurKeuangan;

	/** Bulan acuan penomoran (1-12); diisi otomatis oleh {@link #getBulan()}. */
	private Integer bulan;

	/** Tahun acuan penomoran; diisi otomatis oleh {@link #getTahun()}. */
	private Integer tahun;

	/** Tanggal transaksi kas; dihitung ulang oleh {@link #getTanggalTransaksi()}. */
	private Date tanggalTransaksi;

	/**
	 * Penanda bahwa pengajuan ini adalah penutupan dompet kas kecil, sehingga posting
	 * jurnalnya menambahkan baris debet ke akun penutup sebesar {@link #getSisa()}.
	 */
	private Boolean merupakanPenutupanKasKecil;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JavaBeans.
	 *
	 * <p>Semua field dibiarkan {@code null} kecuali {@code tanggal_dirubah} yang sudah
	 * berisi waktu sekarang. Pengisian nilai bisnis dilakukan lapisan action.</p>
	 */
	public KasKecil() {
	}

	/**
	 * Mengembalikan kunci utama baris.
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama baris. Normalnya hanya dipanggil Hibernate.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nomor/kode agenda pengajuan yang sudah dinormalkan.
	 *
	 * <p>String kosong diperlakukan sama dengan {@code null} sehingga pemanggil cukup
	 * memeriksa {@code null} saja. Nilai {@code null} inilah yang memicu
	 * {@code KasKecilAction} membangkitkan kode baru saat formulir dibuka.</p>
	 *
	 * @return kode agenda tanpa spasi tepi, atau {@code null} bila belum diisi
	 */
	public String getKode() {
		return this.kode == null || kode.trim().isEmpty() ? null : this.kode.trim();
	}

	/**
	 * Menetapkan nomor/kode agenda pengajuan.
	 *
	 * @param kode kode agenda
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan judul singkat pengajuan tanpa spasi tepi.
	 *
	 * @return nama pengajuan, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan judul singkat pengajuan.
	 *
	 * @param nama judul pengajuan
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan uraian keperluan penggunaan dana.
	 *
	 * @return keterangan bebas, atau {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan uraian keperluan penggunaan dana.
	 *
	 * @param keterangan keterangan bebas
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif pengajuan, dengan <b>koreksi otomatis berdasarkan alur
	 * SOP</b>.
	 *
	 * <p><b>Cara kerja.</b> Field {@link #aktif} dipaksa {@code false} bila salah satu dari
	 * dua kondisi terpenuhi:</p>
	 * <ol>
	 *   <li>{@link #getDisposisiSop()} ada tetapi disposisinya sendiri sudah tidak aktif;</li>
	 *   <li>alur berhenti di simpul akhir yang ditandai sebagai titik penolakan
	 *       ({@code getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()}) — dengan kata
	 *       lain pengajuan <b>ditolak</b>.</li>
	 * </ol>
	 * <p>Bila kolomnya masih {@code null}, hasilnya dianggap {@code true}.</p>
	 *
	 * <p><b>Efek samping &amp; sifat satu arah.</b> Ini adalah pola "flag {@code aktif} satu
	 * arah" yang sama persis dengan {@code ais.database.model.PengajuanMahasiswa#getAktif()}
	 * dan entity ber-SOP lain ({@link KasBesar}, {@link DanaTalangan},
	 * {@link PenggantianKasKecil}, {@link DaftarPengajuanTransfer}). Penimpaan menulis ke
	 * properti yang dipetakan, sehingga sekadar <b>membaca</b> daftar pengajuan bisa
	 * menyimpan {@code aktif = false} ke database. Method ini <b>tidak pernah</b>
	 * mengembalikan nilai ke {@code true}: bila disposisi kemudian diaktifkan lagi atau
	 * penolakan dicabut, kolom {@code aktif} tetap {@code false} sampai ada yang memanggil
	 * {@link #setAktif(Boolean)} secara eksplisit.</p>
	 *
	 * <p><b>Konsekuensi khusus kas kecil.</b> {@code JenisKasKecilAction.hitungSaldo(...)}
	 * hanya menjumlahkan {@code nilai} dari baris dengan {@code aktif IS NULL OR aktif =
	 * true}. Sekali pengajuan dipaksa nonaktif, nilainya berhenti mengurangi saldo dompet
	 * kas kecil secara permanen.</p>
	 *
	 * <p>Baris pertama ({@code disposisiSop = getDisposisiSop();}) hanyalah penugasan ulang
	 * hasil resolusi proxy ke field yang sama — tidak mengubah perilaku.</p>
	 *
	 * @return {@code true} bila pengajuan masih berlaku; {@code false} bila disposisinya
	 *         nonaktif atau pengajuan ditolak. Tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		disposisiSop = getDisposisiSop();
		if (disposisiSop != null && !disposisiSop.getAktif()) {
			aktif = false;
		}

		if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
			aktif = false;
		}

		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan status aktif pengajuan.
	 *
	 * <p>Satu-satunya cara mengembalikan status menjadi {@code true} setelah
	 * {@link #getAktif()} memaksanya {@code false}. Dipakai antara lain oleh checkbox
	 * "Aktif" pada grid {@code KasKecilAction}.</p>
	 *
	 * @param aktif status aktif; {@code null} berarti "belum ditentukan" dan akan dibaca
	 *              sebagai {@code true}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan nominal dana yang diminta.
	 *
	 * <p>Menormalkan {@code null} menjadi {@code 0.0} supaya pemanggil (perhitungan saldo,
	 * format angka di grid, penjumlahan jurnal) bisa langsung memakai tipe primitif tanpa
	 * risiko {@code NullPointerException}. Field aslinya tidak diubah.</p>
	 *
	 * @return nominal pengajuan; {@code 0.0} bila belum diisi
	 */
	public Double getNilai() {
		return nilai == null ? 0.0 : nilai;
	}

	/**
	 * Menetapkan nominal dana yang diminta.
	 *
	 * @param nilai nominal pengajuan dalam rupiah
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Mengembalikan dompet/master kas kecil yang dibebani pengajuan ini.
	 *
	 * <p>Relasi lazy, sehingga nilainya dilewatkan {@code check(...)} milik
	 * {@link ais.database.model.GeneralValueObject} untuk menyelesaikan proxy yang mungkin
	 * sudah lepas dari session-nya; hasilnya ditugaskan kembali ke field (pola standar
	 * repo ini). Objek inilah sumber akun kas kecil dan akun penutup saat posting jurnal.</p>
	 *
	 * @return jenis/dompet kas kecil, atau {@code null} bila belum dipilih
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_kas_kecil", nullable = true)
	public JenisKasKecil getJenisKasKecil() {
		jenisKasKecil = check(jenisKasKecil);
		return jenisKasKecil;
	}

	/**
	 * Menetapkan dompet/master kas kecil yang dibebani pengajuan ini.
	 *
	 * @param jenisKasKecil jenis kas kecil terpilih
	 */
	public void setJenisKasKecil(JenisKasKecil jenisKasKecil) {
		this.jenisKasKecil = jenisKasKecil;
	}

	/**
	 * Mengembalikan tanggal pengajuan (kolom {@code tanggal_pengajuan}).
	 *
	 * <p>Bila kolomnya masih kosong, dikembalikan waktu sekarang sebagai nilai tampil —
	 * <b>tanpa</b> menulis balik ke field, jadi baris yang belum pernah diisi tetap
	 * tersimpan {@code null} di database. Tanggal ini adalah batas atas kriteria pada
	 * {@code JenisKasKecilAction.hitungSaldo(...)}: hanya pengajuan sampai tanggal ini yang
	 * dihitung memakai saldo dompet.</p>
	 *
	 * @return tanggal pengajuan, atau waktu sekarang bila belum diisi; tidak pernah
	 *         {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pengajuan")
	public Date getTanggal() {
		return tanggal == null ? new Date() : tanggal;
	}

	/**
	 * Menetapkan tanggal pengajuan.
	 *
	 * @param tanggal tanggal pengajuan
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Nilai bawaan {@link #getFormula()}: representasi teks dari {@link JSONArray} kosong
	 * ({@code "[]"}), sehingga pemanggil selalu bisa langsung mem-parse hasil getter tanpa
	 * memeriksa {@code null}.
	 *
	 * <p><b>Perhatian:</b> field ini {@code public static} dan <b>tidak</b> {@code final},
	 * jadi secara teknis bisa diubah dari mana saja pada saat runtime.</p>
	 */
	public static String DEFAULT_FORMULA = new JSONArray().toString();

	/**
	 * Mengembalikan rincian akun debet pengajuan dalam bentuk teks JSON array.
	 *
	 * <p>Berbeda dari {@link KasBesar}/{@link UangMuka} yang memakai satu akun debet,
	 * kas kecil mendukung banyak baris debet. Format elemen:
	 * <code>[{"akun": &lt;id akun&gt;, "jumlah": &lt;nominal&gt;}, ...]</code>. Parsing
	 * dilakukan di {@code KasKecilAction} (formulir) dan {@code PostingKasKecilAction}
	 * (pembentukan jurnal) — keduanya harus tetap sinkron bila strukturnya berubah.</p>
	 *
	 * @return teks JSON array rincian debet; {@link #DEFAULT_FORMULA} ({@code "[]"}) bila
	 *         kolomnya kosong. Tidak pernah {@code null}
	 */
	@Column(name = "formula", nullable = true, columnDefinition = "text")
	public String getFormula() {
		return formula == null || formula.isEmpty() ? DEFAULT_FORMULA : formula;
	}

	/**
	 * Menetapkan rincian akun debet dalam bentuk teks JSON array.
	 *
	 * @param formula teks JSON array; lihat {@link #getFormula()} untuk formatnya
	 */
	public void setFormula(String formula) {
		this.formula = formula;
	}

	/**
	 * Mengembalikan saldo dompet kas kecil yang tercatat untuk pengajuan ini.
	 *
	 * <p>Nilai ini adalah <b>snapshot</b> yang dihitung lapisan action lewat
	 * {@code JenisKasKecilAction.hitungSaldo(id, jenisKasKecil, tanggal)} = saldo awal
	 * dompet dikurangi total pengajuan aktif lain sampai tanggal pengajuan yang belum
	 * diganti. Getter ini hanya menormalkan {@code null} menjadi {@code 0.0}.</p>
	 *
	 * @return saldo dompet saat pengajuan; {@code 0.0} bila belum dihitung
	 */
	public Double getSaldo() {
		return saldo == null ? 0.0 : saldo;
	}

	/**
	 * Menetapkan snapshot saldo dompet kas kecil.
	 *
	 * @param saldo saldo hasil perhitungan {@code JenisKasKecilAction.hitungSaldo(...)}
	 */
	public void setSaldo(Double saldo) {
		this.saldo = saldo;
	}

	/**
	 * Menetapkan pengguna pembuat pengajuan.
	 *
	 * <p>Nilai ini bisa ditimpa lagi saat {@link #getDibuatOleh()} dibaca, bila pengajuan
	 * sudah punya disposisi SOP.</p>
	 *
	 * @param dibuatOleh pengguna pembuat
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengembalikan pengguna pembuat pengajuan, dengan <b>SOP sebagai sumber kebenaran</b>.
	 *
	 * <p>Alurnya: resolusi proxy lewat {@code check(...)}, lalu — bila ada disposisi SOP
	 * dengan langkah awal yang punya pengaju — field {@link #dibuatOleh} <b>ditimpa</b> oleh
	 * pengaju langkah awal tersebut. Penimpaan menulis ke properti yang dipetakan, sehingga
	 * membaca getter ini dapat memutakhirkan kolom {@code dibuat_oleh} di database.</p>
	 *
	 * @return pengguna pembuat pengajuan, atau {@code null} bila tidak diketahui
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = true)
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);
		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
				&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
			dibuatOleh = getDisposisiSop().getDisposisiStart().getDiajukanOleh();
		}
		return dibuatOleh;
	}

	/**
	 * Menetapkan pengguna penyetuju secara manual.
	 *
	 * <p>Dipakai jalur persetujuan non-SOP di {@code KasKecilAction} (dropdown status
	 * "Disetujui"), dan dikosongkan kembali oleh {@link #setStatus(String)} bila status
	 * diubah menjadi {@link #DITOLAK}.</p>
	 *
	 * @param disetujuiOleh pengguna penyetuju, boleh {@code null}
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengembalikan pengguna penyetuju, hasil penelusuran berjenjang ke dokumen induk dan
	 * alur SOP.
	 *
	 * <p><b>Urutan penentuan:</b></p>
	 * <ol>
	 *   <li>resolusi proxy field sendiri lewat {@code check(...)};</li>
	 *   <li>bila ada {@link #getKasBesar()} yang sudah punya penyetuju &rarr; pakai itu;</li>
	 *   <li>selain itu, bila ada {@link #getPenggantianKasKecil()} yang sudah punya
	 *       penyetuju &rarr; pakai itu;</li>
	 *   <li>selain itu, bila disposisi SOP sudah punya langkah "setuju" dengan pengaju
	 *       &rarr; pakai pengaju langkah tersebut;</li>
	 *   <li><b>terakhir</b>: bila disposisi SOP ada tetapi <i>belum</i> punya langkah
	 *       "setuju" (atau langkah itu tanpa pengaju), hasilnya <b>dipaksa {@code null}</b>.</li>
	 * </ol>
	 *
	 * <p><b>Kuirk penting:</b> langkah 5 dievaluasi setelah langkah 2-4, sehingga begitu
	 * sebuah pengajuan punya disposisi SOP yang belum disetujui, penyetuju dari kas besar
	 * maupun dari dokumen penggantian yang baru saja ditemukan akan <b>dibuang</b>. Efeknya
	 * disengaja (SOP adalah otoritas persetujuan), tetapi membuat cabang 2 dan 3 hanya
	 * berlaku untuk pengajuan tanpa disposisi.</p>
	 *
	 * <p><b>Efek samping:</b> hasilnya ditugaskan ke field yang dipetakan, jadi pembacaan
	 * getter ini dapat mengubah (termasuk mengosongkan) kolom {@code disetujui_oleh} di
	 * database. Getter ini juga menjadi penentu utama {@link #getStatus()}.</p>
	 *
	 * @return pengguna penyetuju, atau {@code null} bila pengajuan belum disetujui
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujuiOleh = check(disetujuiOleh);

		if (getKasBesar() != null && getKasBesar().getDisetujuiOleh() != null) {
			disetujuiOleh = getKasBesar().getDisetujuiOleh();
		} else if (getPenggantianKasKecil() != null && getPenggantianKasKecil().getDisetujuiOleh() != null) {
			disetujuiOleh = getPenggantianKasKecil().getDisetujuiOleh();
		} else if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
				&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
			disetujuiOleh = getDisposisiSop().getDisposisiSetuju().getDiajukanOleh();
		}

		if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
				|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
			disetujuiOleh = null;
		}
		return disetujuiOleh;
	}

	/**
	 * Menetapkan waktu persetujuan secara manual.
	 *
	 * <p>Dipakai jalur persetujuan non-SOP, dan dikosongkan kembali oleh
	 * {@link #setStatus(String)} bila status diubah menjadi {@link #DITOLAK}.</p>
	 *
	 * @param tanggalPersetujuan waktu persetujuan, boleh {@code null}
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengembalikan waktu persetujuan, mengikuti urutan penelusuran yang sama dengan
	 * {@link #getDisetujuiOleh()} (kas besar &rarr; dokumen penggantian &rarr; waktu langkah
	 * "setuju" pada disposisi), lalu <b>dikosongkan</b> bila disposisi ada tetapi belum
	 * punya langkah "setuju".
	 *
	 * <p><b>Pembungkus {@code try/catch}:</b> seluruh penelusuran dibungkus penangkap
	 * {@code Exception} bertanda {@code auto-audit(empty-catch)}. Alasannya tercatat di
	 * kode: {@link #getKasBesar()}/{@link #getPenggantianKasKecil()}/{@link #getDisposisiSop()}
	 * bisa mengembalikan instance canonical/shared milik {@code AuditTimestampInterceptor}
	 * yang proxy-nya terikat ke session lain yang sudah tertutup. Bila itu terjadi, getter
	 * tidak boleh menggagalkan render halaman — penelusuran cukup dilewati dan nilai kolom
	 * yang tersimpan dipertahankan sebagai cadangan.</p>
	 *
	 * <p><b>Efek samping:</b> menulis ke properti yang dipetakan, jadi pembacaan getter ini
	 * dapat mengubah kolom {@code tanggal_persetujuan}.</p>
	 *
	 * @return waktu persetujuan, atau {@code null} bila pengajuan belum disetujui
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {

		try {
			// FIX LazyInitializationException: getKasBesar()/getPenggantianKasKecil()/getDisposisiSop()
			// bisa berupa instance canonical/shared (AuditTimestampInterceptor) yang proxy-nya
			// terikat ke Session lain yang sudah closed -> jangan biarkan getter ini crash,
			// cukup lewati bagian ini (nilai fallback dipertahankan).
			if (getKasBesar() != null && getKasBesar().getTanggalPersetujuan() != null) {
				tanggalPersetujuan = getKasBesar().getTanggalPersetujuan();
			} else if (getPenggantianKasKecil() != null && getPenggantianKasKecil().getTanggalPersetujuan() != null) {
				tanggalPersetujuan = getPenggantianKasKecil().getTanggalPersetujuan();
			} else if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
					&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
				tanggalPersetujuan = getDisposisiSop().getDisposisiSetuju().getWaktu();
			}

			if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
					|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
				tanggalPersetujuan = null;
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/akunting/KasKecil.java:getTanggalPersetujuan-lazy");
		}
		return tanggalPersetujuan;
	}

	/**
	 * Menetapkan waktu pembuatan baris.
	 *
	 * @param tanggalPembuatan waktu pembuatan
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Mengembalikan waktu pembuatan baris.
	 *
	 * <p>Bila kolomnya kosong, dikembalikan waktu sekarang <b>tanpa</b> menulis balik ke
	 * field. Nilai ini juga menjadi cadangan {@link #getTanggalTransaksi()}.</p>
	 *
	 * @return waktu pembuatan, atau waktu sekarang bila belum diisi; tidak pernah
	 *         {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		return tanggalPembuatan == null ? new Date() : tanggalPembuatan;
	}

	/**
	 * Mengembalikan status pengajuan, dihitung ulang dari kondisi persetujuan dan alur SOP.
	 *
	 * <p><b>Urutan penentuan:</b></p>
	 * <ol>
	 *   <li>bila {@link #getDisetujuiOleh()} terisi &rarr; {@link #DISETUJU};</li>
	 *   <li>bila tidak, tetapi kolom status masih berbunyi {@link #DISETUJU} &rarr;
	 *       diturunkan kembali menjadi {@link #PENGAJUAN} (koreksi otomatis bila penyetuju
	 *       dicabut);</li>
	 *   <li>bila pengajuan sudah masuk dokumen {@link PenggantianKasKecil}, statusnya
	 *       <b>diambil alih</b> oleh {@code penggantianKasKecil.getStatus()};</li>
	 *   <li>bila alur SOP berhenti di simpul yang ditandai sebagai titik penolakan &rarr;
	 *       {@link #DITOLAK} (menimpa semua hasil sebelumnya);</li>
	 *   <li>bila hasil akhirnya kosong &rarr; {@link #PENGAJUAN}.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping:</b> menulis ke properti {@code status} yang dipetakan, jadi
	 * pembacaan getter ini dapat memutakhirkan kolom status di database. Kombinasi dengan
	 * langkah 4 membuat pola penolakan bersifat satu arah seperti pada {@link #getAktif()}:
	 * status yang sudah menjadi {@link #DITOLAK} hanya bisa berubah bila alur SOP-nya sendiri
	 * berubah, atau lewat {@link #setStatus(String)}.</p>
	 *
	 * <p>Status {@link #DISETUJU} inilah yang menjadi syarat sebuah pengajuan muncul di
	 * layar posting jurnal {@code PostingKasKecilAction}.</p>
	 *
	 * @return salah satu dari {@link #PENGAJUAN}, {@link #DISETUJU}, {@link #DITOLAK}, atau
	 *         status yang diwarisi dokumen penggantian; tidak pernah {@code null}/kosong
	 */
	public String getStatus() {
		if (getDisetujuiOleh() != null) {
			status = DISETUJU;
		} else if (status != null && status.equals(DISETUJU)) {
			status = PENGAJUAN;
		}

		if (getPenggantianKasKecil() != null) {
			status = penggantianKasKecil.getStatus();
		}

		disposisiSop = getDisposisiSop();
		if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
			status = DITOLAK;
		}

		return status == null || status.trim().isEmpty() ? PENGAJUAN : status;
	}

	/**
	 * Menetapkan status pengajuan.
	 *
	 * <p><b>Efek samping:</b> bila status yang diberikan adalah {@link #DITOLAK}, penanda
	 * persetujuan ikut dibersihkan — {@link #setDisetujuiOleh(Tbmuser)} dan
	 * {@link #setTanggalPersetujuan(Date)} dipanggil dengan {@code null} — supaya
	 * {@link #getStatus()} tidak langsung menaikkannya kembali menjadi {@link #DISETUJU}
	 * pada pembacaan berikutnya.</p>
	 *
	 * @param status status baru; gunakan konstanta {@link #PENGAJUAN}/{@link #DISETUJU}/
	 *               {@link #DITOLAK}
	 */
	public void setStatus(String status) {
		if (status != null && status.equals(DITOLAK)) {
			setDisetujuiOleh(null);
			setTanggalPersetujuan(null);
		}
		this.status = status;
	}

	/**
	 * Mengembalikan satuan kerja pengaju.
	 *
	 * <p>Bila {@link #getJenisKasKecil()} sudah menunjuk satuan kerja, nilai itulah yang
	 * dipakai dan <b>menimpa</b> field lokal — dompet kas kecil dianggap otoritas
	 * kepemilikan unit. Bila tidak, field lokal diselesaikan proxy-nya lewat
	 * {@code check(...)}.</p>
	 *
	 * <p><b>Efek samping:</b> menulis ke properti yang dipetakan, sehingga pembacaan getter
	 * ini dapat memutakhirkan kolom {@code satuan_kerja}.</p>
	 *
	 * @return satuan kerja pengaju, atau {@code null} bila tidak diketahui
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		if (getJenisKasKecil() != null && getJenisKasKecil().getSatuanKerja() != null) {
			satuanKerja = getJenisKasKecil().getSatuanKerja();
		} else {
			satuanKerja = check(satuanKerja);
		}
		return satuanKerja;
	}

	/**
	 * Menetapkan satuan kerja pengaju.
	 *
	 * <p>Perhatikan bahwa nilai ini akan diabaikan {@link #getSatuanKerja()} selama
	 * {@link #getJenisKasKecil()} punya satuan kerja sendiri.</p>
	 *
	 * @param satuanKerja satuan kerja pengaju
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/** Kode unik turunan (kode agenda + id disposisi/id baris); lihat {@link #getKodeUnik()}. */
	private String kodeUnik;

	/**
	 * Mengembalikan kode unik dokumen, <b>dihitung ulang setiap kali dipanggil</b>.
	 *
	 * <p>Bentuknya {@code <kode> + "_" + <id disposisi>} bila pengajuan sudah punya
	 * {@link DisposisiSop}, atau {@code <kode> + "_" + <id baris>} bila belum. Tujuannya
	 * memberi identitas stabil per dokumen alur, bukan sekadar per baris.</p>
	 *
	 * <p><b>Kuirk:</b> kolomnya dipetakan {@code @Column(unique = true)} padahal nilainya
	 * derivatif dan ditulis ulang tiap pembacaan; bila {@link #getKode()} masih {@code null}
	 * hasilnya berupa string harfiah {@code "null_<id>"}, dan dua baris tanpa kode maupun
	 * disposisi (id masih {@code null}) akan menghasilkan {@code "null_null"} yang sama —
	 * berpotensi menabrak batasan unik.</p>
	 *
	 * @return kode unik dokumen; tidak pernah {@code null} (namun bisa mengandung teks
	 *         {@code "null"})
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		kodeUnik = getKode() + "" + (getDisposisiSop() == null ? "_" + getId() : "_" + getDisposisiSop().getId());
		return kodeUnik;
	}

	/**
	 * Menetapkan kode unik dokumen.
	 *
	 * <p>Praktis tidak berpengaruh: {@link #getKodeUnik()} selalu menghitung ulang nilainya
	 * sebelum mengembalikan.</p>
	 *
	 * @param kodeUnik kode unik dokumen
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Mengembalikan instance alur SOP yang menaungi pengajuan ini.
	 *
	 * <p>Implementasi kontrak {@link DataSop}. Bila pengajuan sudah masuk sebuah dokumen
	 * {@link PenggantianKasKecil} yang punya disposisi sendiri, disposisi <b>dokumen
	 * penggantian itulah</b> yang dipakai — pengajuan mengikuti alur persetujuan
	 * penggantiannya. Bila tidak, field lokal diselesaikan proxy-nya lewat
	 * {@code check(...)}.</p>
	 *
	 * <p>Getter ini adalah pusat gravitasi class: {@link #getAktif()}, {@link #getStatus()},
	 * {@link #getDibuatOleh()}, {@link #getDisetujuiOleh()}, {@link #getTanggalPersetujuan()},
	 * dan {@link #getKodeUnik()} semuanya bergantung padanya.</p>
	 *
	 * <p><b>Efek samping:</b> menulis ke properti yang dipetakan ({@code disposisi_sop}).</p>
	 *
	 * @return disposisi SOP yang berlaku, atau {@code null} bila pengajuan tidak melalui SOP
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {

		if (getPenggantianKasKecil() != null && getPenggantianKasKecil().getDisposisiSop() != null) {
			disposisiSop = getPenggantianKasKecil().getDisposisiSop();
		} else {
			disposisiSop = check(disposisiSop);
		}

		return disposisiSop;
	}

	/**
	 * Menetapkan disposisi SOP pengajuan.
	 *
	 * <p>Implementasi kontrak {@link DataSop}. Argumen {@code null} atau disposisi yang
	 * belum tersimpan (id {@code null}) <b>ditolak diam-diam</b> supaya taut alur yang sudah
	 * ada tidak terhapus oleh objek setengah jadi — pola pengaman yang sama dipakai
	 * {@link #setOleh(String)}/{@link #setOlehId(String)}.</p>
	 *
	 * <p><b>Kuirk:</b> ternary di baris penugasan sebenarnya kode mati — setelah guard di
	 * atasnya, kondisi {@code (disposisiSop == null || disposisiSop.getId() == null)} tidak
	 * mungkin benar, sehingga cabang yang mempertahankan nilai lama tidak pernah tercapai
	 * dan efeknya sama dengan penugasan biasa. Dibiarkan apa adanya (dokumentasi saja,
	 * tanpa perubahan logika).</p>
	 *
	 * @param disposisiSop disposisi SOP; diabaikan bila {@code null} atau belum punya id
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
	 * Mengembalikan dokumen penggantian (pengisian ulang dompet) yang memuat pengajuan ini.
	 *
	 * <p>Getter polos — tidak memanggil {@code check(...)}; relasinya memakai
	 * {@code FetchMode.SELECT} sehingga Hibernate memuatnya lewat query terpisah saat
	 * dibutuhkan. Sisi sebaliknya adalah {@code PenggantianKasKecil.getKasKecil()}.</p>
	 *
	 * @return dokumen penggantian, atau {@code null} bila pengajuan belum masuk penggantian
	 *         mana pun
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penggantian_kas_kecil", nullable = true)
	public PenggantianKasKecil getPenggantianKasKecil() {
		return penggantianKasKecil;
	}

	/**
	 * Menetapkan dokumen penggantian yang memuat pengajuan ini.
	 *
	 * <p>Perhatikan dampaknya luas: sekali diisi, {@link #getDisposisiSop()},
	 * {@link #getStatus()}, dan {@link #getTanggalTransaksi()} akan mengikuti dokumen
	 * penggantian.</p>
	 *
	 * @param penggantianKasKecil dokumen penggantian
	 */
	public void setPenggantianKasKecil(PenggantianKasKecil penggantianKasKecil) {
		this.penggantianKasKecil = penggantianKasKecil;
	}

	/**
	 * Mengembalikan jejak posting pengajuan ini ke buku besar.
	 *
	 * @return riwayat posting, atau {@code null} bila pengajuan belum diposting
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * Menetapkan jejak posting pengajuan ini.
	 *
	 * <p>Diisi {@code PostingKasKecilAction} setelah jurnal terbentuk, dan dikosongkan
	 * kembali saat posting dibatalkan.</p>
	 *
	 * @param postingHistory riwayat posting, atau {@code null} untuk menandai belum posting
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	/**
	 * Mengembalikan tahun acuan penomoran surat.
	 *
	 * <p>Bila kolomnya masih kosong, diisi tahun berjalan dari
	 * {@code ais.ui.util.WaktuUtil.getCalendar()} dan <b>ditulis balik ke field</b>,
	 * sehingga pembacaan pertama pada baris lama dapat memutakhirkan kolom {@code tahun}
	 * di database.</p>
	 *
	 * @return tahun acuan penomoran; tidak pernah {@code null}
	 */
	public Integer getTahun() {
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * Menetapkan tahun acuan penomoran surat.
	 *
	 * @param tahun tahun acuan
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan konfigurasi penomoran surat alur keuangan untuk dokumen ini.
	 *
	 * <p>Bila kolomnya kosong, dipakai konfigurasi bawaan statis
	 * {@link NomorSuratAlurKeuangan#KAS_KECIL_DATA} dan nilai itu <b>ditulis balik ke
	 * field</b>. Bila kolomnya terisi, proxy-nya diselesaikan lewat {@code check(...)}.</p>
	 *
	 * <p><b>Perhatian:</b> {@code KAS_KECIL_DATA} adalah field statis yang diisi saat
	 * inisialisasi data master; sebelum inisialisasi itu berjalan, getter ini bisa
	 * mengembalikan {@code null} sekaligus menulis {@code null} ke field.</p>
	 *
	 * @return konfigurasi penomoran surat, atau {@code null} bila data master belum dimuat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat_alur_keuangan", nullable = true)
	public NomorSuratAlurKeuangan getNomorSuratAlurKeuangan() {
		if (nomorSuratAlurKeuangan == null) {
			nomorSuratAlurKeuangan = NomorSuratAlurKeuangan.KAS_KECIL_DATA;
		} else {
			nomorSuratAlurKeuangan = check(nomorSuratAlurKeuangan);
		}
		return nomorSuratAlurKeuangan;
	}

	/**
	 * Menetapkan konfigurasi penomoran surat alur keuangan.
	 *
	 * @param nomorSuratAlurKeuangan konfigurasi penomoran
	 */
	public void setNomorSuratAlurKeuangan(NomorSuratAlurKeuangan nomorSuratAlurKeuangan) {
		this.nomorSuratAlurKeuangan = nomorSuratAlurKeuangan;
	}

	/**
	 * Mengembalikan bulan acuan penomoran surat (1-12).
	 *
	 * <p>Bila kolomnya kosong, diisi bulan berjalan ({@code Calendar.MONTH + 1}, karena
	 * {@link Calendar} menomori bulan mulai 0) dan <b>ditulis balik ke field</b>.</p>
	 *
	 * @return bulan acuan penomoran, 1-12; tidak pernah {@code null}
	 */
	public Integer getBulan() {
		if (bulan == null) {
			bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		}
		return bulan;
	}

	/**
	 * Menetapkan bulan acuan penomoran surat.
	 *
	 * @param bulan bulan acuan, 1-12
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Mengembalikan tanggal transaksi kas, <b>dihitung ulang tanpa syarat setiap kali
	 * dipanggil</b>.
	 *
	 * <p>Bila pengajuan sudah masuk dokumen {@link PenggantianKasKecil} yang punya
	 * {@code DaftarPengajuanTransfer} dengan {@code ProsesTransfer}, dipakai tanggal
	 * pembuatan proses transfer tersebut — yaitu saat uang benar-benar berpindah. Bila
	 * belum, dipakai {@link #getTanggalPembuatan()} sebagai cadangan.</p>
	 *
	 * <p><b>Efek samping:</b> hasilnya selalu ditulis ke properti yang dipetakan
	 * ({@code tanggal_transaksi}), jadi membaca kolom ini di grid/laporan dapat
	 * memutakhirkan basis data. Nilai yang pernah di-{@link #setTanggalTransaksi(Date)}
	 * secara manual selalu tertimpa.</p>
	 *
	 * @return tanggal transaksi kas; tidak pernah {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_transaksi")
	public Date getTanggalTransaksi() {
		if (getPenggantianKasKecil() != null && getPenggantianKasKecil().getDaftarPengajuanTransfer() != null
				&& getPenggantianKasKecil().getDaftarPengajuanTransfer().getProsesTransfer() != null) {
			tanggalTransaksi = getPenggantianKasKecil().getDaftarPengajuanTransfer().getProsesTransfer()
					.getTanggalPembuatan();
		} else {
			tanggalTransaksi = getTanggalPembuatan();
		}
		return tanggalTransaksi;
	}

	/**
	 * Menetapkan tanggal transaksi kas.
	 *
	 * <p>Praktis hanya berumur pendek: {@link #getTanggalTransaksi()} selalu menghitung
	 * ulang nilainya.</p>
	 *
	 * @param tanggalTransaksi tanggal transaksi kas
	 */
	public void setTanggalTransaksi(Date tanggalTransaksi) {
		this.tanggalTransaksi = tanggalTransaksi;
	}

	/**
	 * Mengembalikan dokumen kas besar sumber dana pengajuan ini.
	 *
	 * <p>Getter polos ({@code FetchMode.SELECT}, tanpa {@code check(...)}). Relasi ini
	 * dipakai {@link #getDisetujuiOleh()} dan {@link #getTanggalPersetujuan()} sebagai
	 * sumber penanda persetujuan bagi pengajuan tanpa disposisi SOP.</p>
	 *
	 * @return dokumen kas besar, atau {@code null} bila dana tidak berasal dari kas besar
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kas_besar", nullable = true)
	public KasBesar getKasBesar() {
		return kasBesar;
	}

	/**
	 * Menetapkan dokumen kas besar sumber dana pengajuan ini.
	 *
	 * @param kasBesar dokumen kas besar
	 */
	public void setKasBesar(KasBesar kasBesar) {
		this.kasBesar = kasBesar;
	}

	/**
	 * Mengembalikan penanda tampilkan blok informasi anggaran pada formulir/laporan.
	 *
	 * <p>Menormalkan {@code null} menjadi {@code false} (bawaan: tidak ditampilkan).</p>
	 *
	 * @return {@code true} bila blok anggaran ditampilkan; tidak pernah {@code null}
	 */
	public Boolean getTampilkanAnggaran() {
		return tampilkanAnggaran == null ? false : tampilkanAnggaran;
	}

	/**
	 * Menetapkan penanda tampilkan blok informasi anggaran.
	 *
	 * @param tampilkanAnggaran {@code true} untuk menampilkan blok anggaran
	 */
	public void setTampilkanAnggaran(Boolean tampilkanAnggaran) {
		this.tampilkanAnggaran = tampilkanAnggaran;
	}

	/**
	 * Mengembalikan penanda bahwa pengajuan ini merupakan <b>penutupan</b> dompet kas kecil.
	 *
	 * <p>Bila {@code true}, {@code PostingKasKecilAction} menambahkan satu baris debet ke
	 * {@code jenisKasKecil.akunPenutupKasKecil} sebesar {@link #getSisa()} dan menaikkan
	 * nilai kredit menjadi {@code nilai + sisa} — mencerminkan pengembalian sisa dana di
	 * akhir periode. Menormalkan {@code null} menjadi {@code false}.</p>
	 *
	 * @return {@code true} bila pengajuan adalah penutupan kas kecil; tidak pernah
	 *         {@code null}
	 */
	public Boolean getMerupakanPenutupanKasKecil() {
		return merupakanPenutupanKasKecil == null ? false : merupakanPenutupanKasKecil;
	}

	/**
	 * Menetapkan penanda penutupan dompet kas kecil.
	 *
	 * @param merupakanPenutupanKasKecil {@code true} bila pengajuan ini menutup dompet
	 */
	public void setMerupakanPenutupanKasKecil(Boolean merupakanPenutupanKasKecil) {
		this.merupakanPenutupanKasKecil = merupakanPenutupanKasKecil;
	}

	/**
	 * Mengembalikan sisa dana dompet setelah pengajuan ini, <b>dihitung ulang tanpa syarat
	 * setiap kali dipanggil</b>: {@link #getSaldo()} dikurangi {@link #getNilai()}.
	 *
	 * <p>Karena kedua operand sudah dinormalkan ke {@code 0.0}, method ini tidak pernah
	 * melempar {@code NullPointerException}. Nilainya dipakai layar pengajuan (label
	 * "nilai harus dikembalikan"), dasbor monitor kas kecil, dan — yang paling penting —
	 * sebagai nominal baris debet akun penutup saat {@link #getMerupakanPenutupanKasKecil()}
	 * bernilai {@code true}.</p>
	 *
	 * <p><b>Efek samping:</b> hasilnya ditulis ke properti {@code sisa} yang dipetakan, jadi
	 * pembacaan getter ini dapat memutakhirkan kolomnya di database; nilai yang pernah
	 * di-{@link #setSisa(Double)} selalu tertimpa. Perlu diingat bahwa {@link #getSaldo()}
	 * sendiri hanya snapshot yang dihitung lapisan action saat penyimpanan, sehingga
	 * {@code sisa} ikut basi bila saldo dompet berubah setelahnya.</p>
	 *
	 * @return sisa dana = saldo snapshot dikurangi nominal pengajuan; tidak pernah
	 *         {@code null}
	 */
	public Double getSisa() {
		sisa = getSaldo() - getNilai();
		return sisa;
	}

	/**
	 * Menetapkan sisa dana dompet.
	 *
	 * <p>Praktis tidak berpengaruh: {@link #getSisa()} selalu menghitung ulang nilainya.</p>
	 *
	 * @param sisa sisa dana
	 */
	public void setSisa(Double sisa) {
		this.sisa = sisa;
	}

}
