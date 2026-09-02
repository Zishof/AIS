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
 * <h3>KasBesar — pengajuan pengeluaran dana "kas besar" (tabel {@code akunting.kas_besar})</h3>
 *
 * <p>Entity ini mewakili <b>satu dokumen pengajuan pengeluaran uang bernilai besar</b> di modul
 * akunting AIS: siapa yang mengajukan, untuk keperluan apa, berapa nilainya, rinciannya apa saja,
 * lewat alur persetujuan (SOP) mana, kapan disetujui, kapan uangnya benar-benar ditransfer, dan
 * apakah sudah dipertanggungjawabkan. Dokumen ini adalah dokumen <i>hulu</i>: begitu disetujui, ia
 * memicu pembuatan {@link DaftarPengajuanTransfer} (DPC) → {@code ProsesTransfer} (realisasi
 * pembayaran lewat bank) → {@link PertangungjawabanKasBesar} (SPJ), lalu di-posting ke jurnal lewat
 * {@link PostingHistory}.</p>
 *
 * <h4>Beda dengan {@code KasKecil}</h4>
 * <p>{@link KasKecil} dan {@code KasBesar} adalah <b>dua entity terpisah dengan tabel terpisah</b>
 * ({@code akunting.kas_kecil} vs {@code akunting.kas_besar}), bukan hasil pewarisan satu sama lain.
 * Keduanya lahir dari cetakan hbm2java yang sama sehingga kerangkanya nyaris identik (bahkan
 * {@code serialVersionUID}-nya sama persis), tetapi <b>perannya berbeda secara alur uang</b>:</p>
 * <ul>
 *   <li><b>Tidak ada ambang nominal yang dikodekan di entity ini.</b> Perlu ditegaskan karena mudah
 *   disalahsangka: tidak ada konstanta, tidak ada validasi, dan tidak ada pembacaan
 *   {@code Konfigurasi} di kelas ini yang membandingkan {@link #getNilai()} dengan batas tertentu
 *   untuk memutuskan "ini kas kecil atau kas besar". Pemisahannya murni <b>pemisahan menu dan alur
 *   SOP</b> — pengguna memilih menu Kas Kecil atau menu Kas Besar, dan tiap menu punya alur
 *   persetujuan (SOP) sendiri. Ambang nominal, kalau ada, hidup sebagai konfigurasi alur SOP /
 *   kebijakan organisasi, bukan sebagai kode di sini.</li>
 *   <li><b>Kas kecil = dana yang dipegang unit dan dibelanjakan sendiri.</b> Karena itu
 *   {@code KasKecil} punya {@code saldo}, {@code sisa}, {@code merupakanPenutupanKasKecil}, dan
 *   relasi ke {@code PenggantianKasKecil} (penggantian/replenishment). Konsep "saldo berjalan" ini
 *   <b>tidak ada</b> di {@code KasBesar}.</li>
 *   <li><b>Kas besar = permintaan dana yang dibayarkan lewat bank oleh keuangan pusat.</b> Karena
 *   itu hanya {@code KasBesar} yang punya {@link #getDaftarPengajuanTransfer() daftar pengajuan
 *   transfer} (DPC), {@link #getTanggalTransaksi() tanggal transaksi} yang diturunkan dari proses
 *   transfer/transitori, {@link #getPertangungjawabanKasBesar() pertanggungjawaban}, dan
 *   {@link #getTanggalPersetujuanManual() tanggal persetujuan manual}.</li>
 *   <li><b>Jembatan dua arah:</b> {@link #getAmbilDariKasKecil()} + {@link #getKasKecil()}. Bila
 *   dicentang, dokumen kas besar ini dibuat untuk <b>membayar/mengganti sebuah dokumen kas kecil
 *   yang sudah disetujui</b> — rincian ({@link #getFormula() formula}) disalin dari kas kecil itu,
 *   dan {@code KasKecil.kasBesar} diisi balik menunjuk ke dokumen ini. Di sisi
 *   {@code KasBesarAction} pilihan kas kecil dibatasi ke yang {@code status=Disetujui},
 *   {@code penggantianKasKecil IS NULL}, dan {@code aktif=true}.</li>
 * </ul>
 *
 * <h4>Pengelompokan anggota</h4>
 * <ol>
 *   <li><b>Jejak audit yang dideklarasikan ulang</b> — {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()}, {@link #getId()} dan {@link #onUpdate()}. Lihat catatan
 *   "Kenapa field induk dideklarasikan ulang" di bawah.</li>
 *   <li><b>Identitas dokumen</b> — {@link #getKode()}, {@link #getKodeUnik()}, {@link #getNama()},
 *   {@link #getKeterangan()}, {@link #getNomorSuratAlurKeuangan()}.</li>
 *   <li><b>Isi pengajuan</b> — {@link #getNilai()}, {@link #getFormula()} (rincian JSON),
 *   {@link #getJenisKasBesar()}, {@link #getSatuanKerja()}, {@link #getTanggal()},
 *   {@link #getBulan()}, {@link #getTahun()}.</li>
 *   <li><b>Alur persetujuan</b> — {@link #getDisposisiSop()}, {@link #getStatus()},
 *   {@link #getDibuatOleh()}, {@link #getDisetujuiOleh()}, {@link #getTanggalPembuatan()},
 *   {@link #getTanggalPersetujuan()}, {@link #getTanggalPersetujuanManual()},
 *   {@link #getAktif()}.</li>
 *   <li><b>Hilir (pembayaran, SPJ, jurnal)</b> — {@link #getDaftarPengajuanTransfer()},
 *   {@link #getTanggalTransaksi()}, {@link #getPertangungjawabanKasBesar()},
 *   {@link #getPostingHistory()}.</li>
 *   <li><b>Jembatan kas kecil</b> — {@link #getAmbilDariKasKecil()}, {@link #getKasKecil()}.</li>
 * </ol>
 * <p>Kelas ini <b>tidak punya method utilitas/query statis</b> sama sekali (tidak ada
 * {@code reloadDefault()}, {@code ambilDefault()}, atau {@code hapus()} seperti pada
 * {@link JenisKasBesar} / {@link NomorSuratAlurKeuangan} / {@link DisposisiSop}). Satu-satunya
 * anggota statis adalah tiga konstanta status dan {@link #DEFAULT_FORMULA}.</p>
 *
 * <h4>PENTING — hampir tidak ada getter di kelas ini yang "getter polos"</h4>
 * <p>Ini bukan POJO Hibernate biasa. Karena pemetaan memakai <b>property access</b> ({@code @Id}
 * dipasang di {@link #getId()}), <b>Hibernate membaca state entity lewat getter</b> saat
 * dirty-checking dan flush. Artinya setiap efek samping di dalam getter <b>ikut tersimpan ke
 * database</b> untuk instance yang masih <i>attached</i>, tanpa ada satu baris {@code setX()} pun
 * di kode pemanggil. Dari 32 getter, yang benar-benar polos (hanya {@code return field;}) hanya
 * {@link #getOlehId()}, {@link #getOleh()}, {@link #getTanggal_dirubah()}, {@link #getId()},
 * {@link #getKeterangan()}, {@link #getPostingHistory()}, {@link #getDaftarPengajuanTransfer()},
 * {@link #getPertangungjawabanKasBesar()}, dan {@link #getTanggalPersetujuanManual()}.</p>
 * <p><b>Getter yang menulis balik ke field (dan karenanya berpotensi ke DB):</b>
 * {@link #getAktif()}, {@link #getJenisKasBesar()}, {@link #getFormula()},
 * {@link #getDibuatOleh()}, {@link #getDisetujuiOleh()}, {@link #getTanggalPersetujuan()},
 * {@link #getTanggalPembuatan()}, {@link #getStatus()}, {@link #getSatuanKerja()},
 * {@link #getKodeUnik()}, {@link #getDisposisiSop()}, {@link #getTahun()},
 * {@link #getNomorSuratAlurKeuangan()}, {@link #getBulan()}, {@link #getTanggalTransaksi()}, dan
 * {@link #getKasKecil()} — 16 dari 32. {@link #getKasKecil()} bahkan <b>memutasi entity lain</b>
 * ({@code kasKecil.setKasBesar(this)}).</p>
 * <p><b>Getter yang menutup sesi Hibernate:</b> <b>tidak ada</b> yang melakukannya secara langsung
 * di berkas ini — tidak ada pemanggilan {@code HibernateUtil.closeSession()} maupun
 * {@code session.close()}. Namun hampir semua getter relasi memanggil
 * {@link ais.database.model.GeneralValueObject#check(Object) check(...)}, dan pada tahap terakhir
 * {@code check()} dapat <b>membuka sesi baru sendiri lalu menutupnya kembali</b> untuk memuat ulang
 * object yang sudah detached. Jadi efek "buka-tutup sesi" tetap bisa terjadi, hanya saja
 * terenkapsulasi di kelas induk. Satu jalur tak langsung lain: {@link #getJenisKasBesar()} dapat
 * memanggil {@link JenisKasBesar#ambilDefault(SatuanKerja)} yang membaca cache
 * {@code ConstantValues} (in-memory, tanpa sesi).</p>
 *
 * <h4>VERIFIKASI: pola "flag {@code aktif} satu-arah" — TERKONFIRMASI di kelas ini</h4>
 * <p>Dugaan dari sesi {@code PengajuanMahasiswa} <b>benar untuk {@code KasBesar}</b>. Bukti dari
 * kode berkas ini sendiri:</p>
 * <ol>
 *   <li>{@link #getAktif()} menulis {@code aktif = false} pada dua cabang (disposisi SOP tidak
 *   aktif; atau langkah akhir disposisi berada pada alur bertanda "penolakan ada di sini"), dan
 *   <b>tidak punya satu pun cabang yang menulis {@code aktif = true}</b>. Nilai {@code true} hanya
 *   muncul sebagai <i>nilai balik</i> default saat field masih {@code null}
 *   ({@code return aktif == null ? true : aktif;}) — default itu tidak pernah ditulis ke field.</li>
 *   <li>Properti {@code aktif} <b>dipetakan ke kolom</b> (tidak ada {@code @Transient}; akses
 *   properti), jadi nilai {@code false} yang dipaksakan getter <b>ikut ter-flush permanen</b> ke
 *   {@code akunting.kas_besar.aktif} — bahkan ketika getter hanya dipanggil untuk menggambar
 *   sebuah label di layar.</li>
 *   <li>Efeknya tidak kosmetik: {@code KasBesarAction} menyaring daftar dengan
 *   {@code Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))} dan
 *   pemilihan kas kecil dengan {@code Restrictions.eq("aktif", true)}. Dokumen yang sudah dipaksa
 *   {@code false} <b>hilang dari daftar default</b>.</li>
 *   <li><b>Tidak simetris / tidak reversibel.</b> Pemicunya reversibel — {@code DisposisiSop.aktif}
 *   bisa dihidupkan lagi lewat {@code DisposisiSop.setAktif(true)} — tetapi {@code KasBesar.aktif}
 *   yang sudah {@code false} <b>tidak pernah pulih sendiri</b>; ia hanya bisa dikembalikan oleh
 *   pemanggilan {@link #setAktif(Boolean)} dari luar. Satu-satunya tempat di seluruh kode yang
 *   melakukannya untuk entity ini adalah checkbox "Aktif" di {@code KasBesarAction} (hanya muncul
 *   dalam mode persetujuan dan hanya bila disposisi SOP-nya masih aktif) dan
 *   {@code KasBesarApiHelper} — yang justru <b>tidak akan pernah tereksekusi</b>, lihat catatan
 *   kuirk di {@link #getAktif()}.</li>
 * </ol>
 * <p>{@link #getKasKecil()} memperlihatkan <b>pola satu-arah kedua</b> dengan mekanisme sama: bila
 * {@link #getAmbilDariKasKecil()} bernilai {@code false}, getter menulis {@code kasKecil = null}
 * dan tautannya hilang permanen begitu ter-flush.</p>
 *
 * <h4>Kenapa field induk dideklarasikan ulang di sini (BUKAN bug)</h4>
 * <p>Rantai pewarisannya {@code KasBesar} → {@link DataSop} → {@code GeneralValueObject}, dan
 * <b>tidak satu pun dari kedua induk itu beranotasi {@code @Entity} atau
 * {@code @MappedSuperclass}</b> — keduanya POJO abstrak biasa. Konsekuensinya Hibernate <b>tidak
 * memetakan properti milik induk</b>. Karena itu {@code id}, {@code oleh}, {@code olehId},
 * {@code tanggal_dirubah} beserta getter/setter-nya <b>harus</b> dideklarasikan ulang di setiap
 * entity konkret agar kolom-kolom audit tetap tersimpan. Duplikasi ini keharusan teknis, bukan
 * kelalaian: jangan "dirapikan" dengan menghapusnya.</p>
 *
 * @see KasKecil
 * @see DisposisiSop
 * @see DaftarPengajuanTransfer
 * @see PertangungjawabanKasBesar
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.GeneralValueObject#check(Object)
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "akunting", name = "kas_besar")
public class KasBesar extends DataSop {

	/**
	 * Versi serialisasi Java. Nilainya kebetulan <b>identik</b> dengan milik {@link KasKecil} dan
	 * beberapa entity akunting lain — jejak salin-tempel dari cetakan hbm2java yang sama. Tidak
	 * berbahaya (serialisasi Java memakai nama kelas juga), tapi jangan dijadikan penanda identitas
	 * tipe.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci primer, sekaligus nilai yang dipakai {@link #getKodeUnik()} bila belum ada disposisi. */
	private Long id;
	/** Nama pengguna terakhir yang menyimpan baris ini (jejak audit sederhana, bukan relasi). */
	private String oleh;
	/** ID pengguna terakhir yang menyimpan baris ini (jejak audit sederhana, bukan relasi). */
	private String olehId;

	/**
	 * Mengembalikan ID pengguna terakhir yang menyimpan baris ini. Getter polos.
	 *
	 * @return ID pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi ID pengguna terakhir yang menyimpan baris ini.
	 *
	 * <p><b>Kuirk:</b> nilai {@code null} atau string kosong <b>diabaikan diam-diam</b> (method
	 * langsung {@code return} tanpa menulis apa pun). Jejak audit karenanya hanya bisa ditimpa nilai
	 * baru yang berisi, <b>tidak bisa dikosongkan</b>.</p>
	 *
	 * @param olehId ID pengguna; {@code null}/kosong tidak berefek apa-apa
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna terakhir yang menyimpan baris ini.
	 *
	 * <p><b>Kuirk:</b> sama seperti {@link #setOlehId(String)} — {@code null} atau string kosong
	 * diabaikan diam-diam sehingga jejak audit tidak bisa dihapus lewat setter ini.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong tidak berefek apa-apa
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang menyimpan baris ini. Getter polos.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang dijalankan tepat sebelum baris ini di-{@code UPDATE}.
	 *
	 * <p>Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #setTanggal_dirubah(Date)} beserta {@link #setOleh(String)}/{@link #setOlehId(String)}
	 * dari pengguna sesi yang sedang berjalan. Tidak pernah dipanggil manual — hanya oleh provider
	 * JPA/Hibernate.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir. Diinisialisasi ke waktu server saat object dibuat (bukan
	 * {@code null}), lalu diperbarui {@link #onUpdate()} pada setiap {@code UPDATE}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi waktu perubahan terakhir. Setter polos; normalnya dipanggil oleh
	 * {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}, bukan oleh kode bisnis.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir. Getter polos; tidak pernah {@code null} untuk object
	 * yang baru dibuat di JVM ini karena field-nya sudah diinisialisasi saat deklarasi.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nilai {@link #getStatus()} untuk dokumen yang masih menunggu keputusan. Ini nilai default. */
	public static final String PENGAJUAN = "Pengajuan";
	/**
	 * Nilai {@link #getStatus()} untuk dokumen yang sudah disetujui. Perhatikan ejaannya kurang satu
	 * huruf ({@code "Disetujui"} sebagai nilai, tapi nama konstantanya {@code DISETUJU}) — jangan
	 * dibetulkan namanya tanpa menyapu seluruh pemanggil.
	 */
	public static final String DISETUJU = "Disetujui";
	/**
	 * Nilai {@link #getStatus()} untuk dokumen yang ditolak. Menyetelnya lewat
	 * {@link #setStatus(String)} sekaligus membersihkan penyetuju dan tanggal persetujuan.
	 */
	public static final String DITOLAK = "Ditolak";

	/**
	 * Representasi teks dokumen, dipakai ZK untuk mengisi combobox/label.
	 *
	 * <p><b>Kuirk:</b> membaca <b>field mentah</b> {@code nama}, bukan {@link #getNama()}. Bedanya
	 * dua: tidak ada {@code trim()}, dan nilainya <b>bisa {@code null}</b> — pemanggil yang
	 * merangkai hasilnya ke dalam string akan mendapat teks {@code "null"}, dan pemanggil yang
	 * memanggil method {@code String} di atasnya akan kena {@code NullPointerException}.</p>
	 *
	 * @return nama dokumen apa adanya, mungkin {@code null}
	 */
	public String toString() {
		return nama;
	}

	/** Kode dokumen yang dibangkitkan modul (mis. nomor urut kas besar). Lihat {@link #getKode()}. */
	private String kode;

	/** Judul singkat/keperluan pengajuan yang ditampilkan di daftar dan dropdown. */
	private String nama;
	/** Uraian bebas keperluan pengajuan; kolomnya {@code nullable}. */
	private String keterangan;
	/** Jenis kas besar — menentukan akun sumber/penerima saat posting jurnal. */
	private JenisKasBesar jenisKasBesar;
	/** Nominal total yang diajukan. Lihat {@link #getNilai()} soal default {@code 0.0}. */
	private Double nilai;
	/**
	 * Penanda dokumen masih berlaku. Lihat blok "flag {@code aktif} satu-arah" pada Javadoc kelas
	 * dan {@link #getAktif()}: field ini hanya pernah <b>dipaksa {@code false}</b> oleh getter-nya.
	 */
	private Boolean aktif;
	/** Tanggal pengajuan (kolom {@code tanggal_pengajuan}). */
	private Date tanggal;
	/** Rincian pengajuan dalam bentuk teks JSON array. Lihat {@link #getFormula()}. */
	private String formula;

	/** Unit/satuan kerja pengaju. Bisa diturunkan dari {@link #jenisKasBesar}. */
	private SatuanKerja satuanKerja;
	/** Kepala alur persetujuan (disposisi SOP) dokumen ini. Sumber kebenaran status persetujuan. */
	private DisposisiSop disposisiSop;

	/** Dokumen DPC/pengajuan transfer hilir yang merealisasikan pembayaran dokumen ini. */
	private DaftarPengajuanTransfer daftarPengajuanTransfer;

	/** Pembuat dokumen; biasanya ditimpa oleh pengaju langkah awal disposisi. */
	private Tbmuser dibuatOleh;
	/** Penyetuju dokumen; diturunkan dari langkah "setuju" disposisi. */
	private Tbmuser disetujuiOleh;
	/** Waktu persetujuan; diturunkan dari disposisi/proses transfer/isian manual. */
	private Date tanggalPersetujuan;
	/** Waktu pembuatan; diturunkan dari langkah awal disposisi bila ada. */
	private Date tanggalPembuatan;
	/** Waktu uang benar-benar berpindah. Diturunkan penuh oleh {@link #getTanggalTransaksi()}. */
	private Date tanggalTransaksi;

	/** Status dokumen: {@link #PENGAJUAN}, {@link #DISETUJU}, atau {@link #DITOLAK}. */
	private String status;
	/** Tanggal persetujuan yang diketik petugas untuk dokumen backdate/migrasi. */
	private Date tanggalPersetujuanManual;
	/** Jejak posting jurnal dokumen ini; {@code null} berarti belum pernah di-posting. */
	private PostingHistory postingHistory;
	/** Master penomoran surat alur keuangan; defaultnya {@code NomorSuratAlurKeuangan.KAS_BESAR_DATA}. */
	private NomorSuratAlurKeuangan nomorSuratAlurKeuangan;
	/** Bulan periode dokumen (1-12). Diisi otomatis oleh {@link #getBulan()} bila kosong. */
	private Integer bulan;
	/** Tahun periode dokumen. Diisi otomatis oleh {@link #getTahun()} bila kosong. */
	private Integer tahun;

	/** Penanda dokumen ini dibuat untuk membayar sebuah dokumen {@link KasKecil}. */
	private Boolean ambilDariKasKecil;
	/** Dokumen kas kecil yang dibayar dokumen ini; hanya bermakna bila {@link #ambilDariKasKecil}. */
	private KasKecil kasKecil;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Tidak mengisi apa pun; nilai bawaan
	 * datang dari inisialisasi field ({@link #tanggal_dirubah}) dan dari getter yang mengisi sendiri
	 * ({@link #getBulan()}, {@link #getTahun()}, {@link #getNomorSuratAlurKeuangan()}).
	 */
	public KasBesar() {
	}

	/**
	 * Mengembalikan kunci primer dokumen. Getter polos.
	 *
	 * <p>Kolomnya dipetakan {@code insertable = false} karena nilainya dibangkitkan database
	 * ({@code IDENTITY}/sequence), jadi {@code INSERT} tidak pernah menyertakan kolom {@code id}.
	 * Untuk object yang belum disimpan nilainya {@code null} — hal ini berpengaruh pada
	 * {@link #getKodeUnik()}.</p>
	 *
	 * @return kunci primer, atau {@code null} bila dokumen belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci primer. Setter polos; normalnya hanya dipanggil Hibernate.
	 *
	 * @param id kunci primer
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode dokumen dalam bentuk sudah dirapikan.
	 *
	 * <p>Normalisasi hanya di sisi baca: string kosong/spasi dianggap {@code null}, sisanya
	 * di-{@code trim()}. Field-nya sendiri <b>tidak</b> ditulis ulang, jadi nilai bertele-spasi yang
	 * pernah masuk lewat {@link #setKode(String)} tetap tersimpan apa adanya di database.</p>
	 *
	 * @return kode dokumen yang sudah di-trim, atau {@code null} bila kosong
	 */
	public String getKode() {
		return this.kode == null || kode.trim().isEmpty() ? null : this.kode.trim();
	}

	/**
	 * Mengisi kode dokumen. Setter polos, tanpa normalisasi.
	 *
	 * @param kode kode dokumen
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan judul/keperluan pengajuan, sudah di-{@code trim()} (field tidak ditulis ulang).
	 *
	 * <p>Kolomnya {@code NOT NULL} sepanjang 255 karakter, jadi menyimpan dokumen tanpa nama akan
	 * ditolak database, bukan oleh kode ini.</p>
	 *
	 * @return nama dokumen yang sudah di-trim, atau {@code null} bila field-nya {@code null}
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi judul/keperluan pengajuan. Setter polos, tanpa normalisasi.
	 *
	 * @param nama judul dokumen (wajib terisi sebelum disimpan — kolomnya {@code NOT NULL})
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan uraian bebas keperluan pengajuan. Getter polos (tanpa trim, tanpa default).
	 *
	 * @return keterangan, boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi uraian bebas keperluan pengajuan. Setter polos.
	 *
	 * @param keterangan uraian bebas
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan penanda dokumen masih berlaku — <b>dan memaksanya menjadi {@code false} secara
	 * permanen</b> bila alur persetujuannya sudah mati atau berakhir di langkah penolakan.
	 *
	 * <h4>Alur</h4>
	 * <ol>
	 *   <li>Meresolusi ulang disposisi lewat {@link #getDisposisiSop()} dan <b>menyimpan hasilnya ke
	 *   field</b> {@code disposisiSop} (bisa jadi instance kanonik yang berbeda).</li>
	 *   <li>Bila disposisi ada dan {@code DisposisiSop.getAktif()} bernilai {@code false} →
	 *   {@code aktif = false}.</li>
	 *   <li>Bila langkah akhir disposisi ({@code getDisposisiEnd()}) menunjuk sebuah
	 *   {@code AlurSop} yang bertanda {@code getPenolakanAdaDiSini()} → {@code aktif = false}.
	 *   Artinya dokumen mendarat di langkah tempat penolakan terjadi.</li>
	 *   <li>Mengembalikan {@code aktif}, dengan {@code null} dibaca sebagai {@code true}.</li>
	 * </ol>
	 *
	 * <h4>Sifat satu-arah (terverifikasi)</h4>
	 * <p>Tidak ada cabang yang pernah menulis {@code aktif = true}; nilai {@code true} hanya jadi
	 * nilai balik default saat field masih {@code null} dan default itu tidak disimpan. Karena
	 * properti ini dipetakan ke kolom {@code aktif} dan Hibernate membaca state lewat getter,
	 * <b>pemanggilan getter ini saja sudah cukup untuk mengubur dokumen secara permanen</b> begitu
	 * sesi ter-flush: daftar utama {@code KasBesarAction} menyaring dengan
	 * {@code isNull("aktif") OR aktif = true}. Pemulihan hanya lewat {@link #setAktif(Boolean)} dari
	 * luar — praktis hanya checkbox "Aktif" pada mode persetujuan
	 * {@code KasBesarAction}. Perhatikan juga bahwa pemicunya (disposisi jadi tidak aktif) bisa
	 * dibatalkan, sedangkan akibatnya di sini tidak ikut pulih.</p>
	 *
	 * <h4>Kuirk: penjaga di API tidak pernah jalan</h4>
	 * <p>{@code KasBesarApiHelper} menulis {@code if (kb.getAktif() == null) kb.setAktif(TRUE);}.
	 * Kondisi itu <b>mustahil terpenuhi</b> karena getter ini tidak pernah mengembalikan
	 * {@code null}. Jadi jalur API sama sekali tidak pernah menyalakan kembali flag ini. Dicatat apa
	 * adanya, tidak diperbaiki.</p>
	 *
	 * <p><b>Efek samping:</b> menulis field {@code disposisiSop} dan berpotensi menulis field
	 * {@code aktif} (yang lalu ikut ter-{@code UPDATE}). Dipanggil dari {@code KasBesarAction}
	 * (render kolom "Aktif" dan checkbox), dari filter dasbor, dan secara implisit oleh Hibernate
	 * saat flush.</p>
	 *
	 * @return {@code true} bila dokumen masih berlaku, {@code false} bila sudah dimatikan; tidak
	 *         pernah {@code null}
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
	 * Mengisi penanda dokumen masih berlaku. Setter polos — <b>satu-satunya jalan mengembalikan
	 * nilai {@code true}</b> setelah {@link #getAktif()} memaksanya {@code false}.
	 *
	 * @param aktif {@code true} untuk menghidupkan, {@code false} untuk mematikan, {@code null}
	 *              untuk mengembalikan ke keadaan "belum ditentukan" (dibaca sebagai aktif)
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan nominal total yang diajukan, dengan {@code null} dinormalkan jadi {@code 0.0}.
	 *
	 * <p>Normalisasi hanya di nilai balik — field tetap {@code null}, jadi kolomnya bisa saja
	 * {@code NULL} di database meski pembacaan lewat Java selalu memberi angka. Berarti agregasi SQL
	 * langsung ({@code SUM(nilai)}) dan agregasi lewat Java bisa berbeda perlakuan terhadap baris
	 * kosong.</p>
	 *
	 * @return nominal pengajuan; {@code 0.0} bila belum diisi
	 */
	public Double getNilai() {
		return nilai == null ? 0.0 : nilai;
	}

	/**
	 * Mengisi nominal total yang diajukan. Setter polos.
	 *
	 * @param nilai nominal pengajuan
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Mengembalikan jenis kas besar, <b>mengisinya otomatis dengan jenis default milik satuan
	 * kerja</b> bila masih kosong.
	 *
	 * <p>Langkahnya: resolusi lazy {@code jenisKasBesar} dan {@code satuanKerja} lewat
	 * {@code check()} (hasilnya ditulis balik ke kedua field), lalu bila jenis masih {@code null}
	 * sementara satuan kerja sudah tersimpan, ambil default lewat
	 * {@link JenisKasBesar#ambilDefault(SatuanKerja)} — yang mencari di cache
	 * {@code ConstantValues} entri {@code JenisKasBesar} dengan {@code defaultData = true} untuk
	 * satuan kerja tersebut, dan boleh mengembalikan {@code null} bila belum ada.</p>
	 *
	 * <p><b>Efek samping:</b> menulis field {@code jenisKasBesar} dan {@code satuanKerja}; nilai
	 * default yang terisi otomatis ikut tersimpan ke kolom {@code jenis_kas_besar} pada flush
	 * berikutnya. Relasi lazy, cascade {@code PERSIST}/{@code MERGE}.</p>
	 *
	 * @return jenis kas besar, atau {@code null} bila belum ada dan tidak ada default yang cocok
	 * @see #getSatuanKerja() yang bekerja ke arah sebaliknya (menurunkan satuan kerja dari jenis)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_kas_besar", nullable = true)
	public JenisKasBesar getJenisKasBesar() {

		jenisKasBesar = check(jenisKasBesar);
		satuanKerja = check(satuanKerja);

		if (jenisKasBesar == null && satuanKerja != null && satuanKerja.getId() != null) {
			jenisKasBesar = JenisKasBesar.ambilDefault(satuanKerja);
		}

		return jenisKasBesar;
	}

	/**
	 * Mengisi jenis kas besar. Setter polos.
	 *
	 * @param jenisKasBesar jenis kas besar
	 */
	public void setJenisKasBesar(JenisKasBesar jenisKasBesar) {
		this.jenisKasBesar = jenisKasBesar;
	}

	/**
	 * Mengembalikan tanggal pengajuan (kolom {@code tanggal_pengajuan}), dengan tanggal hari ini
	 * sebagai pengganti bila belum diisi.
	 *
	 * <p><b>Perhatikan:</b> nilai pengganti <b>tidak ditulis balik</b> ke field — berbeda dengan
	 * {@link #getBulan()}/{@link #getTahun()} yang menulis balik. Jadi kolomnya bisa tetap
	 * {@code NULL} di database sementara layar selalu memperlihatkan tanggal hari ini, dan dua
	 * pemanggilan pada hari berbeda memberi hasil berbeda.</p>
	 *
	 * @return tanggal pengajuan, atau tanggal-waktu saat ini bila belum diisi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pengajuan")
	public Date getTanggal() {
		return tanggal == null ? new Date() : tanggal;
	}

	/**
	 * Mengisi tanggal pengajuan. Setter polos.
	 *
	 * @param tanggal tanggal pengajuan
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Nilai bawaan {@link #getFormula()}: teks JSON array kosong {@code "[]"}.
	 *
	 * <p><b>Kuirk:</b> {@code public static} tapi <b>tidak {@code final}</b> — siapa pun bisa
	 * menggantinya dan mengubah default seluruh JVM. Nilainya sendiri {@code String} (immutable),
	 * jadi risikonya sebatas penggantian rujukan yang disengaja.</p>
	 */
	public static String DEFAULT_FORMULA = new JSONArray().toString();

	/**
	 * Mengembalikan rincian pengajuan dalam bentuk teks JSON array, <b>dan menimpanya dengan rincian
	 * milik kas kecil</b> bila dokumen ini dibuat untuk membayar sebuah {@link KasKecil}.
	 *
	 * <p>Isi JSON-nya berupa larik object rincian anggaran (kunci {@code key} = ID mata anggaran,
	 * {@code jumlah} = nominal) — bentuk yang sama dibaca/ditulis {@code KasBesarAction} saat
	 * menyimpan dan saat menghitung pemakaian anggaran.</p>
	 *
	 * <p><b>Efek samping penting:</b> bila {@link #getKasKecil()} mengembalikan dokumen kas kecil,
	 * field {@code formula} milik dokumen <b>ini</b> ditimpa {@code kasKecil.getFormula()} pada
	 * <b>setiap pemanggilan</b>, dan timpaan itu ikut tersimpan ke kolom {@code formula} saat flush.
	 * Rincian kas besar dengan demikian tidak bisa berbeda dari rincian kas kecil sumbernya — apa
	 * pun yang pernah disimpan akan tertimpa lagi begitu getter dibaca. Perhatikan pula bahwa
	 * {@link #getKasKecil()} sendiri punya efek samping (lihat Javadoc-nya).</p>
	 *
	 * @return teks JSON array rincian; {@link #DEFAULT_FORMULA} ({@code "[]"}) bila kosong — tidak
	 *         pernah {@code null}
	 */
	@Column(name = "formula", nullable = true, columnDefinition = "text")
	public String getFormula() {

		if (getKasKecil() != null) {
			formula = getKasKecil().getFormula();
		}

		return formula == null || formula.isEmpty() ? DEFAULT_FORMULA : formula;
	}

	/**
	 * Mengisi rincian pengajuan (teks JSON array). Setter polos, tanpa validasi bentuk JSON.
	 *
	 * <p>Ingat bahwa nilai yang disetel di sini akan <b>tertimpa</b> oleh {@link #getFormula()} bila
	 * dokumen ini tertaut ke sebuah kas kecil.</p>
	 *
	 * @param formula teks JSON array rincian
	 */
	public void setFormula(String formula) {
		this.formula = formula;
	}

	/**
	 * Mengisi pembuat dokumen. Setter polos.
	 *
	 * <p>Nilai yang disetel di sini akan <b>tertimpa</b> {@link #getDibuatOleh()} bila langkah awal
	 * disposisi SOP sudah punya pengaju.</p>
	 *
	 * @param dibuatOleh pengguna pembuat dokumen
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengembalikan pembuat dokumen, dengan <b>pengaju langkah awal disposisi SOP sebagai sumber
	 * kebenaran</b>.
	 *
	 * <p>Alur: resolusi lazy {@code dibuatOleh} lewat {@code check()} (ditulis balik ke field), lalu
	 * bila {@code disposisiSop.getDisposisiStart().getDiajukanOleh()} ada, field diganti dengan
	 * pengaju tersebut. Dengan kata lain kolom {@code dibuat_oleh} berfungsi sebagai cache dari
	 * disposisi, bukan sebagai data mandiri.</p>
	 *
	 * <p><b>Efek samping:</b> menulis field {@code dibuatOleh} → ikut ter-{@code UPDATE} saat flush.
	 * Memicu resolusi lazy berantai lewat {@link #getDisposisiSop()} yang dipanggil tiga kali di
	 * sini (tiap pemanggilan menjalankan {@code check()} lagi).</p>
	 *
	 * @return pengguna pembuat dokumen, atau {@code null} bila belum diketahui
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
	 * Mengisi penyetuju dokumen. Setter polos.
	 *
	 * <p>Dipanggil {@link #setStatus(String)} dengan argumen {@code null} ketika status disetel ke
	 * {@link #DITOLAK}. Nilai yang disetel di sini bisa tertimpa {@link #getDisetujuiOleh()}.</p>
	 *
	 * @param disetujuiOleh pengguna penyetuju, atau {@code null} untuk membatalkan persetujuan
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengembalikan penyetuju dokumen, diturunkan dari disposisi SOP dengan cadangan dari proses
	 * transfer.
	 *
	 * <h4>Urutan keputusan</h4>
	 * <ol>
	 *   <li>Resolusi lazy field lewat {@code check()}.</li>
	 *   <li>Bila {@code disposisiSop.getDisposisiSetuju().getDiajukanOleh()} ada → itulah
	 *   penyetujunya.</li>
	 *   <li>Bila disposisi ada tetapi langkah "setuju"-nya belum ada (atau belum punya pengaju) →
	 *   <b>penyetuju dipaksa {@code null}</b>. Ini yang membuat dokumen kembali berstatus
	 *   {@link #PENGAJUAN} lewat {@link #getStatus()} ketika persetujuan dicabut di alur SOP.</li>
	 *   <li>Bila sampai di sini masih {@code null} dan dokumen sudah punya
	 *   {@link #getDaftarPengajuanTransfer()} dengan {@code prosesTransfer.disetujuiOleh} → pakai
	 *   penyetuju proses transfer sebagai cadangan (kasus dokumen lama/migrasi yang tidak punya
	 *   disposisi).</li>
	 * </ol>
	 *
	 * <p><b>Efek samping:</b> menulis field {@code disetujuiOleh} (termasuk menimpanya dengan
	 * {@code null}) → ikut tersimpan. Perhatikan bahwa {@link #getDisposisiSop()} dipanggil enam
	 * kali dan {@code getDisposisiSetuju()} beberapa kali; {@code DisposisiSop.getDisposisiSetuju()}
	 * sendiri berat dan punya efek samping (menulis field {@code disposisiSetuju} miliknya).</p>
	 *
	 * @return pengguna penyetuju, atau {@code null} bila dokumen belum/tidak jadi disetujui
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujuiOleh = check(disetujuiOleh);

		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
				&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
			disetujuiOleh = getDisposisiSop().getDisposisiSetuju().getDiajukanOleh();
		}

		if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
				|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
			disetujuiOleh = null;
		}

		if (disetujuiOleh == null && getDaftarPengajuanTransfer() != null
				&& getDaftarPengajuanTransfer().getProsesTransfer() != null
				&& getDaftarPengajuanTransfer().getProsesTransfer().getDisetujuiOleh() != null) {
			disetujuiOleh = getDaftarPengajuanTransfer().getProsesTransfer().getDisetujuiOleh();
		}

		return disetujuiOleh;
	}

	/**
	 * Mengisi waktu persetujuan. Setter polos.
	 *
	 * <p>Dipanggil {@link #setStatus(String)} dengan argumen {@code null} ketika status disetel ke
	 * {@link #DITOLAK}.</p>
	 *
	 * @param tanggalPersetujuan waktu persetujuan, atau {@code null} untuk mengosongkan
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengembalikan waktu persetujuan, diturunkan dari disposisi SOP / proses transfer / isian
	 * manual.
	 *
	 * <h4>Urutan keputusan</h4>
	 * <ol>
	 *   <li><b>Di dalam {@code try}</b> (lihat catatan lazy di bawah): bila langkah "setuju"
	 *   disposisi sudah punya pengaju → pakai {@code waktu} langkah tersebut; bila langkah "setuju"
	 *   belum ada → paksa {@code null}; bila masih {@code null} dan ada proses transfer dengan
	 *   tanggal persetujuan → pakai tanggal proses transfer.</li>
	 *   <li><b>Di luar {@code try}</b>: {@code disetujuiOleh} diresolusi ulang, lalu bila
	 *   {@link #getTanggalPersetujuanManual()} terisi <b>dan</b> penyetuju sudah ada, tanggal manual
	 *   <b>menang atas semuanya</b>. Inilah jalur untuk dokumen backdate/migrasi.</li>
	 * </ol>
	 *
	 * <p><b>Catatan {@code try/catch}:</b> blok penangkap ada untuk melindungi dari
	 * {@code LazyInitializationException} — {@link #getDisposisiSop()} dan
	 * {@link #getDaftarPengajuanTransfer()} bisa mengembalikan instance kanonik/berbagi yang
	 * proxy-nya terikat ke {@code Session} lain yang sudah tertutup. Bila meledak, seluruh blok
	 * dilewati (nilai cadangan dipertahankan) dan kejadiannya dicatat
	 * {@code ErrorAuditUtil}.</p>
	 *
	 * <p><b>Kuirk yang perlu diketahui:</b> cabang cadangan proses transfer menguji <b>field mentah</b>
	 * {@code disetujuiOleh}, bukan {@link #getDisetujuiOleh()}, dan {@code check(disetujuiOleh)}
	 * baru dijalankan <i>setelah</i> blok {@code try}. Jadi hasil cabang itu bergantung pada apakah
	 * {@link #getDisetujuiOleh()} kebetulan sudah pernah dipanggil pada instance yang sama —
	 * pembacaan getter ini bisa memberi hasil berbeda tergantung urutan pemanggilan. Bandingkan
	 * dengan {@link #getDisetujuiOleh()} yang menguji hal serupa setelah field-nya dimutakhirkan.</p>
	 *
	 * <p><b>Efek samping:</b> menulis field {@code tanggalPersetujuan} dan {@code disetujuiOleh} →
	 * keduanya ikut tersimpan saat flush.</p>
	 *
	 * @return waktu persetujuan, atau {@code null} bila dokumen belum disetujui
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {

		try {
			// FIX LazyInitializationException: getDisposisiSop()/getDaftarPengajuanTransfer()
			// bisa berupa instance canonical/shared (AuditTimestampInterceptor) yang proxy-nya
			// terikat ke Session lain yang sudah closed -> jangan biarkan getter ini crash,
			// cukup lewati bagian ini (nilai fallback dipertahankan).
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
					&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
				tanggalPersetujuan = getDisposisiSop().getDisposisiSetuju().getWaktu();
			}

			if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
					|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
				tanggalPersetujuan = null;
			}

			if (disetujuiOleh == null && getDaftarPengajuanTransfer() != null
					&& getDaftarPengajuanTransfer().getProsesTransfer() != null
					&& getDaftarPengajuanTransfer().getProsesTransfer().getTanggalPersetujuan() != null) {
				tanggalPersetujuan = getDaftarPengajuanTransfer().getProsesTransfer().getTanggalPersetujuan();
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/akunting/KasBesar.java:getTanggalPersetujuan-lazy");
		}

		disetujuiOleh = check(disetujuiOleh);
		if (getTanggalPersetujuanManual() != null && disetujuiOleh != null) {
			tanggalPersetujuan = getTanggalPersetujuanManual();
		}

		return tanggalPersetujuan;
	}

	/**
	 * Mengisi waktu pembuatan dokumen. Setter polos.
	 *
	 * <p>Nilai ini akan tertimpa {@link #getTanggalPembuatan()} bila langkah awal disposisi SOP
	 * sudah punya pengaju.</p>
	 *
	 * @param tanggalPembuatan waktu pembuatan dokumen
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Mengembalikan waktu pembuatan dokumen, diturunkan dari langkah awal disposisi SOP.
	 *
	 * <p>Bila {@code disposisiSop.getDisposisiStart().getDiajukanOleh()} ada, field ditimpa dengan
	 * {@code waktu} langkah awal tersebut — jadi kolom {@code tanggal_pembuatan} adalah cache dari
	 * "kapan dokumen ini masuk ke alur SOP", bukan "kapan barisnya dibuat di database".</p>
	 *
	 * <p><b>Efek samping:</b> menulis field {@code tanggalPembuatan} → ikut tersimpan. Nilai
	 * pengganti {@code new Date()} pada saat masih kosong <b>tidak</b> ditulis balik (sama seperti
	 * {@link #getTanggal()}), sehingga hasilnya bisa berubah antar pemanggilan.</p>
	 *
	 * @return waktu pembuatan; waktu saat ini bila belum diketahui — tidak pernah {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
				&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
			tanggalPembuatan = getDisposisiSop().getDisposisiStart().getWaktu();
		}

		return tanggalPembuatan == null ? new Date() : tanggalPembuatan;
	}

	/**
	 * Mengembalikan status dokumen, <b>menaikkannya ke {@link #DISETUJU} secara otomatis</b> begitu
	 * ada penyetuju.
	 *
	 * <p>Bila {@link #getDisetujuiOleh()} mengembalikan pengguna, field {@code status} ditulis
	 * {@link #DISETUJU}. Nilai kosong/{@code null} dibaca sebagai {@link #PENGAJUAN}.</p>
	 *
	 * <p><b>Kuirk:</b> status {@link #DITOLAK} <b>tidak pernah dihasilkan otomatis</b> di sini — ia
	 * hanya bisa datang dari {@link #setStatus(String)}. Dan karena cabang di atas hanya menimpa ke
	 * arah "disetujui", dokumen yang sebelumnya {@link #DITOLAK} akan berubah menjadi
	 * {@link #DISETUJU} bila entah bagaimana penyetujunya terisi kembali. Pasangannya
	 * {@link #setStatus(String)} menjaga konsistensi dari sisi sebaliknya (menolak = mengosongkan
	 * penyetuju).</p>
	 *
	 * <p><b>Efek samping:</b> menulis field {@code status}, dan lewat {@link #getDisetujuiOleh()}
	 * juga menulis {@code disetujuiOleh}.</p>
	 *
	 * @return {@link #PENGAJUAN}, {@link #DISETUJU}, atau {@link #DITOLAK} — tidak pernah
	 *         {@code null}
	 */
	public String getStatus() {
		if (getDisetujuiOleh() != null) {
			status = DISETUJU;
		}
		return status == null || status.trim().isEmpty() ? PENGAJUAN : status;
	}

	/**
	 * Mengisi status dokumen, sekaligus <b>membersihkan jejak persetujuan bila statusnya
	 * penolakan</b>.
	 *
	 * <p>Ketika argumennya sama dengan {@link #DITOLAK}, method ini memanggil
	 * {@code setDisetujuiOleh(null)} dan {@code setTanggalPersetujuan(null)} lebih dulu, supaya
	 * {@link #getStatus()} tidak langsung menaikkannya kembali menjadi {@link #DISETUJU}. Untuk
	 * nilai lain tidak ada efek samping.</p>
	 *
	 * @param status status baru; gunakan konstanta {@link #PENGAJUAN}, {@link #DISETUJU}, atau
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
	 * Mengembalikan satuan kerja pengaju, <b>diturunkan dari jenis kas besar bila jenis itu punya
	 * satuan kerja</b>.
	 *
	 * <p>Jenis kas besar diresolusi lebih dulu; bila {@code jenisKasBesar.getSatuanKerja()} ada,
	 * field {@code satuanKerja} <b>ditimpa</b> dengannya — satuan kerja yang dipilih pengguna secara
	 * manual akan kalah. Bila tidak, field hanya diresolusi lazy lewat {@code check()}.</p>
	 *
	 * <p>Perhatikan arah kebergantungan yang <b>berlawanan</b> dengan {@link #getJenisKasBesar()}
	 * (yang menurunkan jenis dari satuan kerja bila jenis kosong). Keduanya saling memanggil field
	 * yang sama tetapi tidak saling memanggil getter, jadi tidak ada rekursi tak berujung — namun
	 * hasil akhirnya bergantung pada getter mana yang dipanggil lebih dulu.</p>
	 *
	 * <p><b>Efek samping:</b> menulis field {@code jenisKasBesar} dan {@code satuanKerja} → ikut
	 * tersimpan.</p>
	 *
	 * @return satuan kerja pengaju, atau {@code null} bila tidak ada
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		jenisKasBesar = check(jenisKasBesar);
		if (jenisKasBesar != null && jenisKasBesar.getSatuanKerja() != null) {
			satuanKerja = jenisKasBesar.getSatuanKerja();
		} else {
			satuanKerja = check(satuanKerja);
		}
		return satuanKerja;
	}

	/**
	 * Mengisi satuan kerja pengaju. Setter polos.
	 *
	 * <p>Ingat bahwa {@link #getSatuanKerja()} akan menimpa nilai ini bila jenis kas besar dokumen
	 * punya satuan kerja sendiri.</p>
	 *
	 * @param satuanKerja satuan kerja pengaju
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/** Kode gabungan yang dijamin unik di tingkat tabel. Lihat {@link #getKodeUnik()}. */
	private String kodeUnik;
	/** Dokumen pertanggungjawaban (SPJ) yang menutup dokumen kas besar ini; {@code null} bila belum. */
	private PertangungjawabanKasBesar pertangungjawabanKasBesar;

	/**
	 * Mengembalikan kode unik dokumen — <b>dihitung ulang dari nol setiap kali dipanggil</b>, bukan
	 * dibaca dari field.
	 *
	 * <p>Rumusnya {@code getKode() + "_" + (disposisiSop == null ? getId() : disposisiSop.getId())}.
	 * Hasilnya ditulis ke field {@code kodeUnik} yang dipetakan ke kolom ber-{@code UNIQUE}
	 * constraint, jadi perhitungan ini ikut tersimpan setiap kali entity ter-flush.</p>
	 *
	 * <p><b>Konsekuensi yang perlu diwaspadai (dicatat apa adanya, tidak diperbaiki):</b></p>
	 * <ul>
	 *   <li>Bagian belakang <b>berpindah dasar</b> begitu disposisi SOP ditautkan: semula
	 *   {@code "<kode>_<idDokumen>"}, sesudahnya {@code "<kode>_<idDisposisi>"}. Nilai kolom unik
	 *   ini berubah di tengah hidup dokumen.</li>
	 *   <li>Tidak ada penjagaan {@code null}: dokumen yang belum punya kode dan belum tersimpan
	 *   menghasilkan literal {@code "null_null"}. Dua dokumen dalam keadaan itu akan
	 *   bertabrakan pada {@code UNIQUE} constraint.</li>
	 *   <li>Memanggil {@link #getDisposisiSop()} yang menjalankan {@code check()} — jadi getter yang
	 *   terlihat seperti perakit string ini sebenarnya bisa menyentuh database.</li>
	 * </ul>
	 *
	 * @return kode unik hasil perhitungan; tidak pernah {@code null} (bisa berisi teks
	 *         {@code "null"})
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		kodeUnik = getKode() + "" + (getDisposisiSop() == null ? "_" + getId() : "_" + getDisposisiSop().getId());
		return kodeUnik;
	}

	/**
	 * Mengisi kode unik. Setter polos — <b>praktis tidak berguna</b>, karena
	 * {@link #getKodeUnik()} menghitung ulang dan menimpa field ini pada pemanggilan berikutnya.
	 * Ada demi kontrak JavaBean yang dibutuhkan Hibernate.
	 *
	 * @param kodeUnik kode unik (akan tertimpa)
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Mengembalikan kepala alur persetujuan (disposisi SOP) dokumen ini.
	 *
	 * <p>Implementasi konkret dari method abstrak {@link DataSop#getDisposisiSop()}. Isinya hanya
	 * resolusi lazy {@code check()} yang ditulis balik ke field — pola getter relasi standar AIS.
	 * Meski begitu ia <b>bukan getter polos</b>: {@code check()} dapat memuat proxy, mengambil
	 * instance kanonik dari {@code EntityIdentityMap}, atau membuka sesi baru untuk memuat ulang
	 * object yang sudah detached.</p>
	 *
	 * <p>Getter ini adalah sumber kebenaran bagi hampir semua getter turunan di kelas ini
	 * ({@link #getAktif()}, {@link #getDibuatOleh()}, {@link #getDisetujuiOleh()},
	 * {@link #getTanggalPersetujuan()}, {@link #getTanggalPembuatan()}, {@link #getKodeUnik()}).</p>
	 *
	 * @return disposisi SOP dokumen, atau {@code null} bila dokumen belum masuk alur SOP
	 * @see DataSop
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menautkan dokumen ini ke sebuah disposisi SOP — <b>hanya bila disposisinya sudah tersimpan</b>.
	 *
	 * <p>Implementasi konkret dari {@link DataSop#setDisposisiSop(DisposisiSop)}. Argumen
	 * {@code null} atau disposisi yang belum punya {@code id} <b>diabaikan diam-diam</b>: method
	 * langsung {@code return}. Akibatnya tautan disposisi <b>tidak pernah bisa dilepas</b> lewat
	 * setter ini, dan disposisi transient tidak akan pernah tersimpan lewat cascade dari sisi ini.
	 * Sifat ini yang membuat riwayat persetujuan sebuah dokumen kas besar tidak bisa "dihapus" dari
	 * layar biasa.</p>
	 *
	 * <p><b>Catatan kode mati:</b> ekspresi ternary di baris terakhir mengecek ulang
	 * {@code disposisiSop == null || disposisiSop.getId() == null} — kondisi yang sudah pasti
	 * {@code false} karena kasus itu sudah dipulangkan oleh penjaga di atas. Jadi cabang
	 * {@code this.disposisiSop} (mempertahankan nilai lama) <b>tidak akan pernah terpilih</b>, dan
	 * method ini selalu menugaskan argumennya. Peninggalan penyuntingan sebelumnya; dicatat, tidak
	 * diubah.</p>
	 *
	 * @param disposisiSop disposisi SOP yang sudah tersimpan; {@code null} atau yang belum punya
	 *                     {@code id} tidak berefek apa-apa
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
	 * Mengembalikan jejak posting jurnal dokumen ini. Getter polos (tanpa {@code check()}).
	 *
	 * <p>{@code null} berarti dokumen belum pernah di-posting ke jurnal. Dipakai
	 * {@code PostingKasBesarAction} untuk menentukan tombol "Posting"/"Batalkan Posting". Relasi ini
	 * memakai {@code FetchMode.SELECT} (default eager per-relasi lewat query terpisah), berbeda dari
	 * relasi lain di kelas ini yang {@code LAZY}.</p>
	 *
	 * @return jejak posting, atau {@code null} bila belum di-posting
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * Mengisi jejak posting jurnal. Setter polos; dipanggil mesin posting saat dokumen di-posting
	 * atau saat posting dibatalkan ({@code null}).
	 *
	 * @param postingHistory jejak posting, atau {@code null} untuk menandai belum di-posting
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	/**
	 * Mengembalikan tahun periode dokumen, <b>mengisinya dengan tahun berjalan bila masih kosong</b>.
	 *
	 * <p><b>Efek samping:</b> berbeda dari {@link #getTanggal()}, nilai default di sini
	 * <b>ditulis balik ke field</b> — sekadar membaca getter ini pada dokumen lama yang kolomnya
	 * {@code NULL} akan mencap dokumen tersebut dengan tahun <i>sekarang</i>, bukan tahun
	 * pengajuannya, dan cap itu ikut tersimpan saat flush. Waktu diambil dari
	 * {@code WaktuUtil.getCalendar()} (waktu server yang bisa digeser konfigurasi), bukan
	 * {@code Calendar.getInstance()} langsung.</p>
	 *
	 * @return tahun periode dokumen; tidak pernah {@code null}
	 */
	public Integer getTahun() {
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * Mengisi tahun periode dokumen. Setter polos.
	 *
	 * @param tahun tahun periode (mis. 2026)
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan master penomoran surat alur keuangan dokumen ini, <b>dengan master "Kas Besar"
	 * sebagai default</b>.
	 *
	 * <p>Bila field masih kosong, diisi dari konstanta statis
	 * {@code NomorSuratAlurKeuangan.KAS_BESAR_DATA} (kode {@code "007"}) — instance <b>berbagi
	 * se-JVM</b> yang disiapkan {@code NomorSuratAlurKeuangan.reloadDefault()} saat aplikasi
	 * dinyalakan. Bila sudah terisi, field cukup diresolusi lazy lewat {@code check()}.</p>
	 *
	 * <p><b>Hal yang perlu diketahui:</b> bila {@code reloadDefault()} belum sempat berjalan,
	 * {@code KAS_BESAR_DATA} masih {@code null} dan getter ini mengembalikan {@code null} (bukan
	 * melempar exception). Selain itu instance statis yang sama akan tertaut ke banyak dokumen kas
	 * besar sekaligus dengan cascade {@code PERSIST}/{@code MERGE} — jangan memutasi object hasil
	 * getter ini, perubahannya akan terasa di seluruh aplikasi.</p>
	 *
	 * <p><b>Efek samping:</b> menulis field {@code nomorSuratAlurKeuangan} → ikut tersimpan.</p>
	 *
	 * @return master penomoran surat, atau {@code null} bila master default belum dimuat
	 * @see NomorSuratAlurKeuangan#KAS_BESAR_DATA
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat_alur_keuangan", nullable = true)
	public NomorSuratAlurKeuangan getNomorSuratAlurKeuangan() {
		if (nomorSuratAlurKeuangan == null) {
			nomorSuratAlurKeuangan = NomorSuratAlurKeuangan.KAS_BESAR_DATA;
		} else {
			nomorSuratAlurKeuangan = check(nomorSuratAlurKeuangan);
		}
		return nomorSuratAlurKeuangan;
	}

	/**
	 * Mengisi master penomoran surat alur keuangan. Setter polos.
	 *
	 * @param nomorSuratAlurKeuangan master penomoran surat
	 */
	public void setNomorSuratAlurKeuangan(NomorSuratAlurKeuangan nomorSuratAlurKeuangan) {
		this.nomorSuratAlurKeuangan = nomorSuratAlurKeuangan;
	}

	/**
	 * Mengembalikan bulan periode dokumen (1-12), <b>mengisinya dengan bulan berjalan bila masih
	 * kosong</b>.
	 *
	 * <p>Perhatikan {@code + 1}: {@code Calendar.MONTH} berbasis nol, sedangkan kolom ini menyimpan
	 * bulan berbasis satu — Januari tersimpan sebagai {@code 1}. Berlaku peringatan yang sama seperti
	 * {@link #getTahun()}: nilai default <b>ditulis balik ke field</b> dan ikut tersimpan, sehingga
	 * dokumen lama berkolom {@code NULL} akan tercap bulan <i>sekarang</i>.</p>
	 *
	 * @return bulan periode dokumen, 1 sampai 12; tidak pernah {@code null}
	 */
	public Integer getBulan() {
		if (bulan == null) {
			bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		}
		return bulan;
	}

	/**
	 * Mengisi bulan periode dokumen. Setter polos, tanpa validasi rentang.
	 *
	 * @param bulan bulan periode berbasis satu (1 = Januari)
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Mengembalikan dokumen DPC/pengajuan transfer yang merealisasikan pembayaran dokumen ini.
	 * Getter polos (tanpa {@code check()}, relasi {@code FetchMode.SELECT}).
	 *
	 * <p>{@code null} berarti dokumen belum masuk antrean pembayaran; {@code KasBesarAction}
	 * memakainya untuk menampilkan tombol "Buat DPC" bagi dokumen berstatus disetujui yang belum
	 * punya DPC, dan {@code MonitorKasBesarDashboard} memakainya untuk menghitung status pembayaran
	 * lewat {@code DpcTransferStatusHelper}.</p>
	 *
	 * @return dokumen pengajuan transfer, atau {@code null} bila belum ada
	 * @see DaftarPengajuanTransfer
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "daftar_pengajuan_transfer", nullable = true)
	public DaftarPengajuanTransfer getDaftarPengajuanTransfer() {
		return daftarPengajuanTransfer;
	}

	/**
	 * Menautkan dokumen ini ke sebuah DPC/pengajuan transfer. Setter polos; dipanggil saat DPC
	 * dibuat dari dokumen kas besar.
	 *
	 * @param daftarPengajuanTransfer dokumen pengajuan transfer
	 */
	public void setDaftarPengajuanTransfer(DaftarPengajuanTransfer daftarPengajuanTransfer) {
		this.daftarPengajuanTransfer = daftarPengajuanTransfer;
	}

	/**
	 * Mengembalikan waktu uang benar-benar berpindah — <b>selalu dihitung ulang</b> dari dokumen
	 * hilir, tidak pernah dibaca apa adanya dari field.
	 *
	 * <h4>Urutan keputusan</h4>
	 * <ol>
	 *   <li><b>Jalur transitori</b> — bila DPC ditandai {@code transitori} dan punya
	 *   {@code transitoriData.prosesTransitori}, pakai {@code tanggalPembuatan} proses transitori
	 *   itu (pembayaran lewat rekening penampungan).</li>
	 *   <li><b>Jalur transfer biasa</b> — bila DPC punya {@code prosesTransfer}, pakai
	 *   {@code tanggalRealisasikan}-nya; bila realisasi belum ada, mundur ke
	 *   {@code tanggalPembuatan} proses transfer.</li>
	 *   <li><b>Belum ada DPC</b> — pakai {@link #getTanggalPembuatan()} dokumen ini sendiri. Karena
	 *   getter itu sendiri jatuh ke {@code new Date()} bila kosong, hasilnya bisa berupa waktu
	 *   sekarang.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping:</b> field {@code tanggalTransaksi} ditimpa di <b>ketiga</b> cabang, jadi
	 * kolom {@code tanggal_transaksi} adalah cache murni yang diperbarui setiap kali getter ini
	 * dibaca. Nilai apa pun yang disetel lewat {@link #setTanggalTransaksi(Date)} akan hilang.
	 * Kolom ini dipakai sebagai dasar tanggal transaksi jurnal saat posting.</p>
	 *
	 * @return waktu transaksi hasil perhitungan; tidak pernah {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_transaksi")
	public Date getTanggalTransaksi() {
		if (getDaftarPengajuanTransfer() != null && daftarPengajuanTransfer.getTransitori()
				&& daftarPengajuanTransfer.getTransitoriData() != null
				&& daftarPengajuanTransfer.getTransitoriData().getProsesTransitori() != null) {
			tanggalTransaksi = daftarPengajuanTransfer.getTransitoriData().getProsesTransitori().getTanggalPembuatan();
		} else if (getDaftarPengajuanTransfer() != null && daftarPengajuanTransfer.getProsesTransfer() != null) {
			tanggalTransaksi = daftarPengajuanTransfer.getProsesTransfer().getTanggalRealisasikan() == null
					? daftarPengajuanTransfer.getProsesTransfer().getTanggalPembuatan()
					: daftarPengajuanTransfer.getProsesTransfer().getTanggalRealisasikan();
		} else {
			tanggalTransaksi = getTanggalPembuatan();
		}
		return tanggalTransaksi;
	}

	/**
	 * Mengisi waktu transaksi. Setter polos — <b>nilainya akan selalu tertimpa</b> oleh
	 * {@link #getTanggalTransaksi()} pada pembacaan berikutnya. Ada demi kontrak JavaBean.
	 *
	 * @param tanggalTransaksi waktu transaksi (akan tertimpa)
	 */
	public void setTanggalTransaksi(Date tanggalTransaksi) {
		this.tanggalTransaksi = tanggalTransaksi;
	}

	/**
	 * Mengembalikan dokumen pertanggungjawaban (SPJ) yang menutup dokumen kas besar ini. Getter
	 * polos.
	 *
	 * <p>{@code null} berarti dana sudah cair tetapi belum dipertanggungjawabkan — inilah yang
	 * dihitung {@code MonitorKasBesarDashboard} sebagai "belum SPJ".</p>
	 *
	 * @return dokumen pertanggungjawaban, atau {@code null} bila belum ada
	 * @see PertangungjawabanKasBesar
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pertangungjawaban_kas_besar", nullable = true)
	public PertangungjawabanKasBesar getPertangungjawabanKasBesar() {
		return pertangungjawabanKasBesar;
	}

	/**
	 * Menautkan dokumen pertanggungjawaban (SPJ) ke dokumen kas besar ini. Setter polos.
	 *
	 * @param pertangungjawabanKasBesar dokumen pertanggungjawaban
	 */
	public void setPertangungjawabanKasBesar(PertangungjawabanKasBesar pertangungjawabanKasBesar) {
		this.pertangungjawabanKasBesar = pertangungjawabanKasBesar;
	}

	/**
	 * Mengembalikan tanggal persetujuan yang diketik petugas secara manual. Getter polos.
	 *
	 * <p>Dipakai untuk dokumen backdate/migrasi yang persetujuannya terjadi di luar sistem. Bila
	 * terisi <b>dan</b> dokumen sudah punya penyetuju, nilai ini <b>mengalahkan</b> semua turunan
	 * lain di {@link #getTanggalPersetujuan()}.</p>
	 *
	 * @return tanggal persetujuan manual, atau {@code null} bila tidak dipakai
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggalPersetujuanManual() {
		return tanggalPersetujuanManual;
	}

	/**
	 * Mengisi tanggal persetujuan manual. Setter polos.
	 *
	 * @param tanggalPersetujuanManual tanggal persetujuan versi manual/backdate
	 */
	public void setTanggalPersetujuanManual(Date tanggalPersetujuanManual) {
		this.tanggalPersetujuanManual = tanggalPersetujuanManual;
	}

	/**
	 * Mengembalikan penanda "dokumen ini dibuat untuk membayar sebuah dokumen kas kecil", dengan
	 * {@code null} dinormalkan jadi {@code false}.
	 *
	 * <p>Nilainya menentukan apakah {@link #getKasKecil()} boleh mempertahankan tautannya —
	 * {@code false} akan <b>menghapus</b> tautan itu. Di layar, penanda ini adalah checkbox "Ambil
	 * dari Kas Kecil" pada {@code KasBesarAction} yang membuka baris pilihan kas kecil.</p>
	 *
	 * @return {@code true} bila dokumen ini membayar sebuah kas kecil; tidak pernah {@code null}
	 */
	public Boolean getAmbilDariKasKecil() {
		return ambilDariKasKecil == null ? false : ambilDariKasKecil;
	}

	/**
	 * Mengisi penanda "ambil dari kas kecil". Setter polos.
	 *
	 * <p>Menyetelnya {@code false} (atau membiarkannya {@code null}) <b>akan melepas</b> tautan
	 * {@link #getKasKecil()} secara permanen pada pembacaan berikutnya.</p>
	 *
	 * @param ambilDariKasKecil {@code true} bila dokumen ini dibuat untuk membayar sebuah kas kecil
	 */
	public void setAmbilDariKasKecil(Boolean ambilDariKasKecil) {
		this.ambilDariKasKecil = ambilDariKasKecil;
	}

	/**
	 * Mengembalikan dokumen {@link KasKecil} yang dibayar dokumen ini — <b>dan memutasi kedua belah
	 * pihak dalam prosesnya</b>.
	 *
	 * <h4>Dua efek samping</h4>
	 * <ol>
	 *   <li><b>Penghapusan tautan satu-arah.</b> Bila {@link #getAmbilDariKasKecil()} bernilai
	 *   {@code false}, field {@code kasKecil} ditulis {@code null}. Sama seperti pola flag
	 *   {@code aktif}, getter ini <b>hanya pernah menulis {@code null}</b> — tidak ada cabang yang
	 *   memulihkan tautan. Begitu ter-flush, kolom {@code kas_kecil} kosong permanen dan hanya bisa
	 *   diisi ulang lewat {@link #setKasKecil(KasKecil)} dari luar.</li>
	 *   <li><b>Menulis balik ke entity LAIN.</b> Bila tautan ada, getter memanggil
	 *   {@code kasKecil.setKasBesar(this)} — memaksa sisi seberang menunjuk balik ke dokumen ini.
	 *   Karena relasi ini bercascade {@code PERSIST}/{@code MERGE}, penunjukan balik itu bisa ikut
	 *   tersimpan. Artinya <b>membaca</b> getter ini dapat mengubah baris {@code akunting.kas_kecil}
	 *   di database, termasuk merebut tautan sebuah kas kecil dari dokumen kas besar lain yang
	 *   sebelumnya memegangnya.</li>
	 * </ol>
	 *
	 * <p>Dipanggil secara tidak langsung oleh {@link #getFormula()} pada <b>setiap</b> pembacaan
	 * rincian, jadi efek samping di atas jauh lebih sering terjadi daripada yang terlihat dari
	 * jumlah pemanggilan eksplisitnya.</p>
	 *
	 * @return dokumen kas kecil yang dibayar, atau {@code null} bila dokumen ini bukan pembayaran
	 *         kas kecil
	 * @see #getAmbilDariKasKecil()
	 * @see #getFormula()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kas_kecil", nullable = true)
	public KasKecil getKasKecil() {
		if (!getAmbilDariKasKecil()) {
			kasKecil = null;
		}

		if (kasKecil != null) {
			kasKecil.setKasBesar(this);
		}

		return kasKecil;
	}

	/**
	 * Menautkan dokumen kas kecil yang dibayar dokumen ini. Setter polos.
	 *
	 * <p>Tautan hanya bertahan bila {@link #getAmbilDariKasKecil()} juga bernilai {@code true};
	 * kalau tidak, {@link #getKasKecil()} akan menghapusnya lagi.</p>
	 *
	 * @param kasKecil dokumen kas kecil sumber, atau {@code null} untuk melepas tautan
	 */
	public void setKasKecil(KasKecil kasKecil) {
		this.kasKecil = kasKecil;
	}
}
