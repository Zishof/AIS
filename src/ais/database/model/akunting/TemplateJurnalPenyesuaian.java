package ais.database.model.akunting;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * Template jurnal penyesuaian berkala (amortisasi, akrual, penyisihan) &mdash; <b>definisi</b>
 * sebuah jurnal berulang, bukan dokumennya.
 *
 * <h2>1. Kedudukan: fitur "Template Jurnal" yang benar-benar hidup</h2>
 * <p>Di paket ini ada dua rancangan "template jurnal" yang namanya mirip tetapi nasibnya
 * berlawanan. {@code TemplateTransaksi}/{@code TemplateGrupTransaksi} adalah rancangan 2010 yang
 * <b>TIDUR/YATIM total</b> &mdash; nol pemanggil di seluruh pohon sumber (lihat Javadoc
 * {@code TemplateTransaksi}). Kelas <b>ini</b>-lah yang dipakai sungguhan: ia punya layar,
 * punya API, dan jurnalnya benar-benar masuk buku besar. Verifikasi pemanggil (3 Sep 2026):</p>
 * <ul>
 *   <li>{@code ais.action.servlet.api.JurnalPenyesuaianHelper} &mdash; satu-satunya kelas yang
 *   membaca/menulis entity ini (CRUD template, penyusunan draf, dan posting);</li>
 *   <li>{@code ais.action.servlet.PosApi} &mdash; jalur REST POS Desktop/Android, mengoper semua
 *   aksi berawalan {@code penyesuaian_} ke helper di atas;</li>
 *   <li>{@code ais.action.master.koperasi.SiklusAkuntansiKantinAction} &mdash; layar ZK
 *   "Siklus Akuntansi" halaman {@code penyesuaian}, memanggil helper yang <i>sama</i> sehingga
 *   angka ZK dan POS mustahil berbeda;</li>
 *   <li>{@code ais.action.master.akunting.PostingJurnalAction} &mdash; mendaftarkan layar itu
 *   sebagai TAB "Jurnal Penyesuaian" di dalam menu <b>Posting Jurnal</b> (lihat butir 7);</li>
 *   <li>{@code hibernate.cfg.xml} &mdash; pendaftaran mapping; tabelnya dibuat otomatis oleh
 *   {@code hbm2ddl}, tidak ada skrip DDL manual.</li>
 * </ul>
 *
 * <h2>2. Celah yang ditutup</h2>
 * <p>Penyusutan aset sudah punya prosesnya sendiri ({@code PenyusutanAsset}), tetapi penyesuaian
 * berkala lain &mdash; amortisasi biaya dibayar di muka, akrual beban yang belum ditagih, dan
 * penyisihan piutang tak tertagih &mdash; sebelumnya tidak punya proses sama sekali; satu-satunya
 * jalan adalah mengetik Jurnal Umum manual tiap bulan, yang mudah terlewat dan mudah salah akun.
 * Entity ini menyimpan definisinya sekali supaya tiap periode cukup "lihat draf lalu posting".</p>
 *
 * <h2>3. Anatomi satu baris</h2>
 * <p>Satu baris = satu jurnal berulang <b>dua kaki sederhana</b>: tepat satu akun debet
 * ({@link #getAkunDebet()}), tepat satu akun kredit ({@link #getAkunKredit()}), dan satu nominal
 * tetap ({@link #getNilai()}) yang dipakai untuk KEDUA kaki sekaligus &mdash; jadi jurnalnya
 * selalu seimbang menurut konstruksi. Bentuk ini <b>tidak</b> mendukung jurnal majemuk
 * (banyak debet/banyak kredit), tidak mendukung nominal yang menurun tiap periode (mis. bunga
 * efektif), dan tidak menyimpan jadwal/tanggal mulai-berakhir: template berlaku selamanya sampai
 * {@link #getAktif()} dimatikan atau barisnya dihapus. <b>Tidak ada tabel jadwal amortisasi</b>
 * dan tidak ada perhitungan sisa nilai buku di mana pun &mdash; "jadwal"-nya hanyalah
 * {@link #getFrekuensi()} (BULANAN/TAHUNAN) plus periode yang diketik operator saat memposting.</p>
 *
 * <h2>4. Cara jurnal terbentuk &mdash; kaitan ke {@code GrupTransaksi}</h2>
 * <p>Entity ini <b>bukan</b> dokumen: ia tidak punya kolom cap posting, tidak punya tanggal
 * dokumen, dan tidak pernah menjadi {@code reference} sebuah jurnal. Saat operator menekan
 * "Posting", {@code JurnalPenyesuaianHelper.jalankan(...)} untuk tiap template yang siap:</p>
 * <ol>
 *   <li>membuat {@link PostingHistory} baru berjenis {@code "Jurnal Penyesuaian Berkala"}
 *   (konstanta {@code JurnalPenyesuaianHelper.JENIS}), diisi tanggal, pengguna, dan
 *   keterangan bertanda periode;</li>
 *   <li>memanggil {@code CommonAkunting.saveTransaksi(new Akun[]{akunDebet},
 *   new Akun[]{akunKredit}, null, null, postingHistory, true, keterangan, tanggal,
 *   new Double[]{nilai}, new Double[]{nilai}, 0.0, null, satuanKerja, session)} yang menerbitkan
 *   satu kepala jurnal {@link ais.database.model.akunting.GrupTransaksi} beserta dua baris
 *   {@link Transaksi} (debet dan kredit).</li>
 * </ol>
 * <p><b>Konsekuensi TERVERIFIKASI dan penting.</b> Pemanggilan di atas mengoper
 * {@code reference = null} <i>dan</i> tidak mengoper {@code ref} sama sekali. Akibatnya
 * {@code GrupTransaksi.ambilUnik()} tidak menemukan satu pun kolom referensi terisi dan
 * mengembalikan string kosong, lalu {@code GrupTransaksi.getKodeUnik()} menerjemahkan string
 * kosong menjadi {@code null}. Pencarian idempotensi bawaan mesin posting
 * ({@code Restrictions.eq("kodeUnik", null)} &rarr; SQL {@code kode_unik = NULL}) karena itu
 * <b>tidak pernah cocok</b>. Dengan kata lain: <b>penjaga anti-duplikat milik
 * {@code CommonAkunting} MATI TOTAL untuk fitur ini</b> &mdash; setiap panggilan posting selalu
 * menulis kepala jurnal + baris jurnal baru. Satu-satunya yang mencegah beban ganda adalah
 * penjaga buatan tangan di helper (butir 5). Sisi baiknya: fitur ini <b>tidak</b> terkena kelas
 * bug "kaki jurnal hilang"/"kunci global {@code ref} telanjang" yang dijelaskan panjang lebar di
 * Javadoc {@link ais.database.model.akunting.GrupTransaksi} dan pada {@code task_e68c78f1},
 * karena ia tidak pernah berbagi kunci dengan dokumen lain.</p>
 * <p>Jurnal yang sudah terbentuk <b>tidak</b> ikut terhapus ketika templatenya dihapus
 * ({@code JurnalPenyesuaianHelper.hapus}) &mdash; itu memang disengaja: riwayat akuntansi tidak
 * boleh hilang hanya karena definisinya dibuang. Lihat butir 5 untuk efek sampingnya pada
 * penjaga anti-posting-ganda.</p>
 *
 * <h2>5. Penjaga anti-posting-ganda per periode &mdash; hasil verifikasi</h2>
 * <p>Mekanismenya <b>bukan</b> kolom pada entity ini (tidak ada kolom "periode terakhir diposting"
 * di sini), melainkan <b>penanda teks pada keterangan cap posting</b>. Bentuknya
 * {@code [PENYESUAIAN <id template> <periode>]} yang ditempelkan di ujung keterangan, lalu
 * sebelum memposting helper menjalankan:</p>
 * <pre>{@code
 * select count(*) from akunting.posting_history
 *  where jenis = 'Jurnal Penyesuaian Berkala'
 *    and coalesce(keterangan,'') like '%[PENYESUAIAN <id> <periode>]%'
 * }</pre>
 * <p>Untuk pemakaian normal lewat layar ZK penjaga ini <b>bekerja</b>: layar selalu memformat
 * periode sebagai {@code yyyy-MM}, sehingga menekan "Posting Semua yang Siap" dua kali pada bulan
 * yang sama menghasilkan alasan "Sudah diposting untuk periode &hellip;" dan tidak ada beban
 * kedua. Kurungan siku aman pada PostgreSQL (dialek yang dipakai instalasi ini) karena
 * {@code [} bukan metakarakter {@code LIKE} di sana. Namun penjaga ini <b>bukan kunci basis
 * data</b>, dan verifikasi menemukan beberapa celah nyata:</p>
 * <ol>
 *   <li><b>Fail-open saat query gagal.</b> {@code sudahDiposting(...)} membungkus seluruh query
 *   dalam {@code try/catch} dan mengembalikan {@code false} pada exception apa pun &mdash; yaitu
 *   "belum pernah diposting". Gangguan sesaat pada basis data membuat penjaga <b>menghilang
 *   secara senyap</b>, bukan menahan posting.</li>
 *   <li><b>Periode tidak divalidasi formatnya.</b> Satu-satunya syarat adalah "tidak kosong".
 *   Lewat REST, {@code periode="2026-1"} dan {@code periode="2026-01"} menghasilkan DUA penanda
 *   berbeda, sementara {@code tanggalPeriode(...)} mem-parse keduanya (lenient) ke bulan yang
 *   SAMA. Beban yang sama karenanya dapat diposting berkali-kali hanya dengan mengubah ejaan
 *   periode.</li>
 *   <li><b>Tanggal jurnal bebas dari periode.</b> Payload boleh membawa {@code tanggal} sendiri
 *   yang sama sekali tidak diverifikasi berada di dalam {@code periode}; penanda hanya mengunci
 *   pasangan (template, teks periode), bukan tanggal buku.</li>
 *   <li><b>TOCTOU / balapan.</b> Seluruh daftar kesiapan (termasuk pemeriksaan penanda) dihitung
 *   SEBELUM perulangan posting, dan tiap posting memakai transaksi sendiri. Dua permintaan
 *   {@code penyesuaian_posting} yang berjalan bersamaan sama-sama lolos pemeriksaan lalu
 *   sama-sama menulis jurnal. Tidak ada indeks unik pada {@code posting_history.keterangan}
 *   yang menahannya.</li>
 *   <li><b>Penanda memakai id template.</b> Menghapus lalu membuat ulang template yang sama
 *   menghasilkan id baru tanpa penanda, sehingga periode yang sudah diposting bisa diposting
 *   ulang &mdash; sementara jurnal lama tetap tinggal (butir 4).</li>
 *   <li><b>Penanda bisa hilang karena panjang kolom.</b> Keterangan disusun sebagai
 *   {@code "Penyesuaian " + nama + " periode " + periode + " " + penanda} sedangkan
 *   {@link #getNama()} boleh 255 karakter dan {@code PostingHistory.keterangan} dipetakan tanpa
 *   {@code length} (bawaan {@code varchar(255)}). Nama template yang panjang membuat penanda
 *   melewati batas kolom; pada PostgreSQL posting akan gagal keras, tetapi pada basis data yang
 *   memangkas diam-diam penanda ikut terpotong dan penjaganya lumpuh permanen.</li>
 *   <li><b>Ruang lingkup penjaga GLOBAL.</b> Query di atas tidak memfilter tenant apa pun, dan
 *   memang tidak bisa: {@link PostingHistory} tidak punya kolom sekolah/yayasan, dan entity ini
 *   pun tidak (butir 6).</li>
 * </ol>
 * <p>Kesimpulan: penjaga ini adalah <b>pencegah kekeliruan operator</b> yang efektif pada jalur
 * layar, <b>bukan</b> jaminan integritas terhadap klien REST yang menyusun payload sendiri atau
 * terhadap permintaan serentak.</p>
 *
 * <h2>6. Cakupan tenant: tidak ada</h2>
 * <p>Entity ini <b>tidak memiliki kolom {@code sekolah} maupun {@code yayasan}</b>. Satu-satunya
 * pembeda organisasi adalah {@link #getSatuanKerja()}, yang saat penyimpanan diisi paksa dari
 * konfigurasi global {@code satuan_kerja_kantin} lewat {@code AkunKantinUtil.satkerKantin()}
 * &mdash; nilai yang SAMA untuk seluruh instalasi, bukan turunan dari tenant pengguna. Ditambah
 * {@code JurnalPenyesuaianHelper.semuaTemplate(...)} yang memuat seluruh tabel tanpa satu pun
 * pembatas, artinya: setiap pengguna yang boleh membuka menu ini melihat, mengubah, dan
 * menghapus template <b>milik semua tenant</b>, dan posting menulis jurnal untuk seluruh
 * instalasi sekaligus. Ini bukan pola "fail-open" bersyarat seperti {@code SekolahUtil}, melainkan
 * ketiadaan penyaring sama sekali &mdash; sekelas temuan {@code task_f1283f4a}.</p>
 *
 * <h2>7. Siapa yang boleh membuat, mengubah, dan memposting</h2>
 * <p>Gerbangnya ada di {@code JurnalPenyesuaianHelper.bolehAksiMenu(...)} dengan kunci menu
 * {@code "jurnal_penyesuaian"} (terdaftar di {@code EbisnisMenuKatalog.DAFTAR},
 * {@code KUNCI_AKUNTANSI}, dan {@code KUNCI_CRUD}). Tiga hal yang perlu diketahui:</p>
 * <ul>
 *   <li><b>Fail-open peran null.</b> {@code bolehAksiMenu} mengembalikan {@code true} ketika
 *   {@code tbmuser.hakAkses()} bernilai {@code null} ("kompatibilitas akun lama"). Ini persis pola
 *   {@code task_66986071} yang sudah terkonfirmasi di lima helper API lain &mdash; hanya saja di
 *   sini yang dipagari adalah pembuatan/pengubahan/penghapusan template <i>dan</i> aksi
 *   {@code penyesuaian_posting} yang menulis jurnal finansial. Pengguna terautentikasi yang
 *   perannya tidak terbaca dapat membuat template beban fiktif lalu mempostingnya berulang tiap
 *   periode.</li>
 *   <li><b>Aksi baca tidak dipagari.</b> {@code penyesuaian_template_daftar} dan
 *   {@code penyesuaian_draft} tidak melewati gerbang apa pun; setiap pemegang token POS yang sah
 *   dapat menarik seluruh daftar template lengkap dengan kode akun debet/kredit dan nominalnya.</li>
 *   <li><b>Pewarisan hak lewat menu induk.</b> Layar ZK-nya bukan menu tersendiri melainkan tab
 *   {@code jurnal_penyesuaian} di dalam {@code PostingJurnalAction.TABS} (menu "Posting Jurnal").
 *   {@code SiklusAkuntansiKantinAction.doBeforeCompose} hanya memanggil
 *   {@code Common.doCheckSecurity()}, yang membaca atribut sesi {@code currentMenu} &mdash; untuk
 *   halaman ter-<i>include</i> atribut itu tetap menunjuk menu INDUK. Ini instans lanjutan dari
 *   pola pewarisan hak menu induk yang sudah didokumentasikan sejak batch 61/73. Untungnya
 *   gerbang helper tetap berlaku pada jalur ZK maupun REST, jadi warisan menu hanya membuat
 *   layarnya terlihat, bukan otomatis memberi hak posting &mdash; kecuali bila perannya
 *   {@code null} (butir pertama).</li>
 * </ul>
 *
 * <h2>8. Pengelompokan method</h2>
 * <ul>
 *   <li><b>Konstanta frekuensi:</b> {@link #BULANAN}, {@link #TAHUNAN}.</li>
 *   <li><b>Identitas &amp; siklus hidup:</b> {@link #TemplateJurnalPenyesuaian()},
 *   {@link #getId()}/{@link #setId(Long)}, {@link #onUpdate()}.</li>
 *   <li><b>Isi template:</b> {@link #getNama()}, {@link #getAkunDebet()}, {@link #getAkunKredit()},
 *   {@link #getNilai()}, {@link #getFrekuensi()}, {@link #getAktif()}, {@link #getKeterangan()}
 *   beserta setter-nya.</li>
 *   <li><b>Konteks organisasi:</b> {@link #getSatuanKerja()}/{@link #setSatuanKerja(ais.database.model.rab.SatuanKerja)}.</li>
 *   <li><b>Jejak audit:</b> {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()} beserta setter-nya.</li>
 * </ul>
 * <p>Tidak ada method bisnis di kelas ini. Seluruh perhitungan, penyusunan draf, penentuan
 * tanggal, dan penulisan jurnal berada di {@code JurnalPenyesuaianHelper} &mdash; entity ini murni
 * pembawa data.</p>
 *
 * <h2>9. Hal-hal non-obvious</h2>
 * <ul>
 *   <li><b>Tiga getter melakukan substitusi nilai (bukan tulis-balik).</b> {@link #getNilai()},
 *   {@link #getFrekuensi()}, dan {@link #getAktif()} mengganti {@code null} dengan nilai bawaan
 *   pada saat dibaca, TANPA menugaskannya kembali ke field. Berbeda dari getter destruktif
 *   {@code Transaksi.getAkun()} atau {@code GrupTransaksi.getKodeUnik()}, membaca entity ini
 *   tidak pernah mengubah data yang tersimpan &mdash; kolom {@code NULL} tetap {@code NULL} di
 *   basis data.</li>
 *   <li><b>Jebakan terpenting akibat substitusi itu:</b> {@link #getAktif()} melaporkan
 *   {@code TRUE} untuk baris ber-{@code aktif = NULL}, sehingga grid daftar template menampilkan
 *   baris tersebut sebagai <b>aktif</b>; tetapi penyaring draf/posting bekerja di tingkat SQL
 *   ({@code Restrictions.eq("aktif", Boolean.TRUE)}) yang <b>tidak</b> melihat substitusi getter,
 *   sehingga baris itu <b>diam-diam tidak pernah ikut diposting</b>. Layar dan mesin berbeda
 *   pendapat, tanpa pesan apa pun. Jalur simpan lewat helper selalu mengisi nilai eksplisit, jadi
 *   kasus ini muncul pada baris hasil impor/sunting manual di basis data.</li>
 *   <li><b>{@link #getFrekuensi()} menormalkan ke huruf besar dan memangkas spasi.</b>
 *   Perbandingan di helper memakai {@code TemplateJurnalPenyesuaian.TAHUNAN.equals(...)} atas
 *   hasil getter, jadi masukan {@code " tahunan "} tetap dikenali. Nilai selain BULANAN/TAHUNAN
 *   tidak ditolak di mana pun; ia hanya diperlakukan sebagai "bukan TAHUNAN", yang efeknya sama
 *   dengan BULANAN.</li>
 *   <li><b>Template TAHUNAN hanya boleh diposting pada periode berakhiran {@code -12}</b>
 *   (Desember). Aturan ini ada di helper, bukan di entity, dan tidak dapat dikonfigurasi per
 *   template.</li>
 *   <li><b>{@link #getNilai()} tidak pernah mengembalikan {@code null}</b>, sehingga cabang
 *   penjaga {@code t.getNilai() == null ? 0 : ...} di helper adalah kode mati. Yang benar-benar
 *   menyaring adalah syarat {@code nilai <= 0}. Nilai negatif karena itu <b>ditolak</b>
 *   (bukan dibalik menjadi jurnal terbalik).</li>
 *   <li><b>{@link #getOleh()} dan {@link #getOlehId()} diisi dengan nilai yang SAMA</b> oleh
 *   {@code JurnalPenyesuaianHelper.simpan(...)} ({@code tbmuser.getUserId()} untuk keduanya),
 *   menyimpang dari konvensi repo di mana {@code oleh} memuat nama dan {@code olehId} memuat id.
 *   Jangan andalkan {@code oleh} sebagai nama tampil untuk entity ini.</li>
 *   <li><b>Entity di-{@code @Audited} (Envers).</b> Setiap versi template digandakan ke tabel
 *   {@code template_jurnal_penyesuaian_aud}; menghapus template tidak menghapus jejak versinya.
 *   Ini membantu telusur "siapa mengubah nominal amortisasi", tetapi berarti pula data lama tetap
 *   dapat dibaca lewat layar revisi.</li>
 *   <li><b>{@code dynamicInsert}/{@code dynamicUpdate} aktif</b>, jadi hanya kolom yang benar-benar
 *   berubah yang ditulis &mdash; kolom {@code NULL} tidak akan "terisi sendiri" oleh substitusi
 *   getter di atas.</li>
 * </ul>
 *
 * <h2>10. Catatan pewarisan &mdash; deklarasi ulang field BUKAN bug</h2>
 * <p>Kelas ini {@code extends} {@link ais.database.model.GeneralValueObject}. Kelas dasar itu
 * <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} &mdash; ia POJO abstrak biasa,
 * sehingga Hibernate <b>tidak memetakan</b> properti yang dideklarasikan di sana. Field
 * {@code id}, {@code nama}, {@code keterangan}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} karena itu <b>WAJIB dideklarasikan ulang</b> di kelas ini lengkap dengan
 * anotasi kolomnya; pengulangan tersebut adalah keharusan teknis, bukan duplikasi yang perlu
 * "dibersihkan". Menghapusnya akan membuat kolom-kolom itu hilang dari tabel.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.akunting.GrupTransaksi
 * @see ais.database.model.akunting.Transaksi
 * @see ais.database.model.akunting.PostingHistory
 * @see ais.database.model.akunting.Akun
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "akunting", name = "template_jurnal_penyesuaian")
public class TemplateJurnalPenyesuaian extends GeneralValueObject {

	/** Versi serialisasi. Nilai {@code 1L} bawaan; entity ini tidak pernah dikirim lintas JVM. */
	private static final long serialVersionUID = 1L;

	/**
	 * Sebulan sekali (default).
	 *
	 * <p>Nilai yang dikembalikan {@link #getFrekuensi()} bila kolom kosong. Template berfrekuensi
	 * ini siap diposting pada periode {@code yyyy-MM} mana pun.</p>
	 */
	public static final String BULANAN = "BULANAN";
	/**
	 * Setahun sekali.
	 *
	 * <p>{@code JurnalPenyesuaianHelper} hanya menganggap template TAHUNAN siap diposting bila
	 * periode yang diminta berakhiran {@code -12} (Desember); di luar itu alasannya
	 * "Template tahunan hanya diposting pada periode Desember."</p>
	 */
	public static final String TAHUNAN = "TAHUNAN";

	/** Kunci utama, dibangkitkan basis data. Dipakai sebagai komponen penanda anti-posting-ganda. */
	private Long id;
	/** Nama template, tampil di grid dan disalin ke keterangan jurnal yang terbentuk. */
	private String nama;
	/** Akun yang didebet tiap periode (mis. Beban Amortisasi). */
	private Akun akunDebet;
	/** Akun yang dikredit tiap periode (mis. Biaya Dibayar Di Muka). */
	private Akun akunKredit;
	/** Nominal tetap per periode; dipakai untuk kaki debet dan kredit sekaligus. */
	private Double nilai;
	/** {@link #BULANAN} atau {@link #TAHUNAN}; kosong dianggap {@link #BULANAN}. */
	private String frekuensi;
	/** Bendera aktif; hanya baris {@code TRUE} (di tingkat SQL) yang ikut draf/posting. */
	private Boolean aktif;
	/** Catatan bebas operator; tidak ikut ke keterangan jurnal. */
	private String keterangan;
	/** Satuan kerja RAB; diisi paksa dari konfigurasi global {@code satuan_kerja_kantin}. */
	private ais.database.model.rab.SatuanKerja satuanKerja;
	/** Jejak audit: pengguna terakhir yang menyimpan (diisi dengan {@code userId}, bukan nama). */
	private String oleh;
	/** Jejak audit: id pengguna terakhir yang menyimpan. */
	private String olehId;

	/**
	 * Kait JPA yang dijalankan tepat sebelum setiap {@code UPDATE} atas entity ini.
	 *
	 * <p>Mendelegasikan ke {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}
	 * yang memperbarui {@link #getTanggal_dirubah()} (dan jejak pengguna bila tersedia) agar
	 * kolom audit tidak bergantung pada kedisiplinan tiap pemanggil.</p>
	 *
	 * <p>Implementasi ini juga memenuhi satu-satunya kontrak {@code abstract} milik
	 * {@link ais.database.model.GeneralValueObject}. Method tidak boleh melempar exception:
	 * kegagalan di sini akan membatalkan seluruh transaksi penyimpanan template.</p>
	 *
	 * <p>Tidak dipanggil pada {@code INSERT} &mdash; nilai awal {@link #getTanggal_dirubah()}
	 * berasal dari inisialisasi field.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir. Diinisialisasi ke waktu server saat objek dibuat dan diperbarui
	 * oleh {@link #onUpdate()} pada tiap {@code UPDATE}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Konstruktor kosong wajib Hibernate.
	 *
	 * <p>Semua field bernilai {@code null} kecuali {@link #getTanggal_dirubah()} yang sudah terisi
	 * waktu server. Perhatikan bahwa objek baru yang belum diisi tetap terbaca "aktif" dan
	 * "BULANAN" lewat getter-nya karena substitusi nilai bawaan &mdash; lihat butir 9 pada Javadoc
	 * kelas.</p>
	 */
	public TemplateJurnalPenyesuaian() {
	}

	/**
	 * Mengembalikan kunci utama template.
	 *
	 * <p>Selain sebagai identitas baris, nilai ini menjadi komponen penanda anti-posting-ganda
	 * {@code [PENYESUAIAN <id> <periode>]} yang ditulis ke keterangan {@link PostingHistory}.
	 * Karena itu id yang berubah (mis. template dihapus lalu dibuat ulang) membuat penanda lama
	 * tidak lagi cocok &mdash; lihat butir 5 pada Javadoc kelas.</p>
	 *
	 * @return id baris; {@code null} bila belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama.
	 *
	 * <p>Dipakai Hibernate saat memuat/menyimpan. Kolom dipetakan {@code insertable = false},
	 * jadi nilai yang diisi manual tidak akan ikut pada {@code INSERT}.</p>
	 *
	 * @param id kunci utama baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama template apa adanya.
	 *
	 * <p>Nama ini bukan sekadar label: {@code JurnalPenyesuaianHelper} menyalinnya ke keterangan
	 * jurnal ({@code "Penyesuaian " + nama + " periode " + periode + " " + penanda}). Nama yang
	 * sangat panjang dapat mendorong penanda anti-posting-ganda melewati batas kolom keterangan
	 * cap posting &mdash; lihat butir 5 pada Javadoc kelas.</p>
	 *
	 * @return nama template; dapat {@code null} untuk baris yang belum diisi.
	 */
	@Column(name = "nama", nullable = true, length = 255)
	public String getNama() {
		return nama;
	}

	/**
	 * Menyetel nama template.
	 *
	 * <p>Jalur simpan lewat helper menolak nama kosong, tetapi setter ini tidak memvalidasi
	 * apa pun.</p>
	 *
	 * @param nama nama template; boleh {@code null} di level entity.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan akun yang akan DIDEBET tiap periode.
	 *
	 * <p>Relasi {@code LAZY}: pemanggil di luar sesi Hibernate aktif dapat menerima proxy yang
	 * belum terinisialisasi. Bila akun ini {@code null}, template dianggap belum lengkap dan
	 * dilewati saat penyusunan draf dengan alasan "Akun debet/kredit template belum lengkap."</p>
	 *
	 * @return akun debet; {@code null} bila belum dipilih.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_debet", nullable = true)
	public Akun getAkunDebet() {
		akunDebet = check(akunDebet);
		return akunDebet;
	}

	/**
	 * Menyetel akun yang akan didebet.
	 *
	 * <p>Helper mengisi akun ini dari <b>kode akun</b> yang dikirim klien
	 * ({@code akunDebetKode}) dan menolak bila kodenya tidak ditemukan atau sama dengan akun
	 * kredit. Setter ini sendiri tidak memeriksa apa pun.</p>
	 *
	 * @param akunDebet akun debet; boleh {@code null}.
	 */
	public void setAkunDebet(Akun akunDebet) {
		this.akunDebet = akunDebet;
	}

	/**
	 * Mengembalikan akun yang akan DIKREDIT tiap periode.
	 *
	 * <p>Relasi {@code LAZY}, dengan perlakuan kelengkapan yang sama seperti
	 * {@link #getAkunDebet()}.</p>
	 *
	 * @return akun kredit; {@code null} bila belum dipilih.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_kredit", nullable = true)
	public Akun getAkunKredit() {
		akunKredit = check(akunKredit);
		return akunKredit;
	}

	/**
	 * Menyetel akun yang akan dikredit.
	 *
	 * @param akunKredit akun kredit; boleh {@code null}.
	 */
	public void setAkunKredit(Akun akunKredit) {
		this.akunKredit = akunKredit;
	}

	/**
	 * Mengembalikan nominal per periode, dengan {@code null} disubstitusi menjadi {@code 0}.
	 *
	 * <p><b>Bukan getter destruktif</b>: substitusi hanya berlaku pada nilai kembalian, field dan
	 * kolom basis data tetap {@code NULL}. Karena getter ini tidak pernah mengembalikan
	 * {@code null}, cabang penjaga null di {@code JurnalPenyesuaianHelper.jalankan(...)} adalah
	 * kode mati; yang benar-benar menyaring adalah syarat {@code nilai <= 0} yang menandai
	 * template dengan alasan "Nilai template nol." dan melewatinya.</p>
	 *
	 * <p>Nilai yang sama dipakai untuk kaki debet <i>dan</i> kaki kredit, sehingga jurnal yang
	 * terbentuk selalu seimbang.</p>
	 *
	 * @return nominal per periode; {@code 0} bila kolom kosong.
	 */
	@Column(name = "nilai", nullable = true)
	public Double getNilai() {
		return nilai == null ? Double.valueOf(0) : nilai;
	}

	/**
	 * Menyetel nominal per periode.
	 *
	 * <p>Mengubah nilai ini <b>tidak</b> menyentuh jurnal periode yang sudah terlanjur diposting
	 * (berbeda dari {@code Pajak.getNilai()} yang menghitung ulang dari tarif terkini) &mdash;
	 * nominal yang sudah masuk buku besar tersimpan pada baris {@link Transaksi} sendiri.
	 * Perubahan hanya berlaku untuk posting periode berikutnya.</p>
	 *
	 * @param nilai nominal per periode; boleh {@code null} (dibaca sebagai {@code 0}).
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Mengembalikan frekuensi yang sudah dinormalkan: dipangkas spasi dan dijadikan huruf besar,
	 * dengan kosong/{@code null} disubstitusi menjadi {@link #BULANAN}.
	 *
	 * <p>Normalisasi ini yang membuat perbandingan {@code TAHUNAN.equals(getFrekuensi())} di
	 * helper tahan terhadap masukan {@code " tahunan "}. Nilai di luar {@link #BULANAN}/
	 * {@link #TAHUNAN} tidak ditolak di mana pun &mdash; ia hanya "bukan TAHUNAN", yang efeknya
	 * identik dengan BULANAN.</p>
	 *
	 * <p>Seperti {@link #getNilai()}, substitusi tidak ditulis balik ke field.</p>
	 *
	 * @return {@link #BULANAN}, {@link #TAHUNAN}, atau teks lain dalam huruf besar.
	 */
	@Column(name = "frekuensi", nullable = true, length = 20)
	public String getFrekuensi() {
		return frekuensi == null || frekuensi.trim().isEmpty() ? BULANAN : frekuensi.trim().toUpperCase();
	}

	/**
	 * Menyetel frekuensi mentah.
	 *
	 * <p>Nilai disimpan apa adanya; normalisasi terjadi saat dibaca. Kolom hanya menampung 20
	 * karakter.</p>
	 *
	 * @param frekuensi {@link #BULANAN} atau {@link #TAHUNAN}; boleh {@code null}.
	 */
	public void setFrekuensi(String frekuensi) {
		this.frekuensi = frekuensi;
	}

	/**
	 * Mengembalikan bendera aktif, dengan {@code null} disubstitusi menjadi {@link Boolean#TRUE}.
	 *
	 * <p><b>PERHATIAN &mdash; layar dan mesin bisa berbeda pendapat.</b> Substitusi ini hanya
	 * berlaku di Java. Penyaring draf/posting bekerja di tingkat SQL
	 * ({@code Restrictions.eq("aktif", Boolean.TRUE)}) dan tidak pernah melihat substitusi ini,
	 * sehingga baris ber-{@code aktif = NULL} <b>tampil aktif di grid tetapi tidak pernah ikut
	 * diposting</b>, tanpa pesan apa pun. Jalur simpan lewat helper selalu menulis nilai
	 * eksplisit, jadi kondisi ini muncul pada baris hasil impor atau suntingan langsung di basis
	 * data.</p>
	 *
	 * @return {@code TRUE} bila aktif atau kolom kosong; {@code FALSE} bila dinonaktifkan.
	 */
	@Column(name = "aktif", nullable = true)
	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	/**
	 * Menyetel bendera aktif.
	 *
	 * <p>Menonaktifkan template <b>tidak</b> menghapus jurnal yang sudah terbentuk; ia hanya
	 * mengeluarkan template dari daftar draf periode berikutnya. Ini cara yang dianjurkan untuk
	 * menghentikan amortisasi yang sudah habis, karena menghapus template membuat penanda
	 * anti-posting-ganda kehilangan acuan id-nya (butir 5 pada Javadoc kelas).</p>
	 *
	 * @param aktif {@code TRUE} untuk mengikutkan template pada draf/posting; {@code null}
	 *              dibaca sebagai {@code TRUE} oleh getter tetapi TIDAK oleh penyaring SQL.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan catatan bebas operator atas template ini.
	 *
	 * <p>Hanya keterangan template; <b>tidak</b> ikut disalin ke keterangan jurnal yang terbentuk
	 * (yang dipakai di sana adalah {@link #getNama()} dan penanda periode). Nilai dikembalikan apa
	 * adanya, termasuk {@code null}.</p>
	 *
	 * @return catatan bebas; dapat {@code null}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel catatan bebas operator.
	 *
	 * @param keterangan catatan bebas; boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan satuan kerja RAB yang melekat pada template.
	 *
	 * <p>Nilai ini diteruskan apa adanya ke {@code CommonAkunting.saveTransaksi(...)} sebagai
	 * satuan kerja jurnal. <b>Bukan pembatas tenant</b>: jalur simpan mengisinya paksa dari
	 * konfigurasi global {@code satuan_kerja_kantin} lewat {@code AkunKantinUtil.satkerKantin()},
	 * yang bernilai sama untuk seluruh instalasi dan mengembalikan {@code null} bila konfigurasi
	 * kosong/tidak valid. Relasi {@code LAZY}.</p>
	 *
	 * @return satuan kerja; {@code null} bila konfigurasi belum diisi.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public ais.database.model.rab.SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Menyetel satuan kerja RAB.
	 *
	 * <p>Nilai yang diisi pemanggil lain akan tertimpa pada penyimpanan berikutnya lewat
	 * {@code JurnalPenyesuaianHelper.simpan(...)}, yang selalu menyetel ulang dari konfigurasi
	 * global.</p>
	 *
	 * @param satuanKerja satuan kerja; boleh {@code null}.
	 */
	public void setSatuanKerja(ais.database.model.rab.SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Mengembalikan jejak pengguna terakhir yang menyimpan template.
	 *
	 * <p><b>Kuirk:</b> jalur simpan mengisi kolom ini dengan {@code tbmuser.getUserId()} &mdash;
	 * nilai yang SAMA dengan {@link #getOlehId()} &mdash; menyimpang dari konvensi repo di mana
	 * {@code oleh} memuat nama pengguna. Jangan pakai sebagai nama tampil.</p>
	 *
	 * @return id pengguna penyimpan terakhir; {@code null} bila disimpan tanpa sesi pengguna.
	 */
	@Column(name = "oleh", nullable = true)
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menyetel jejak pengguna penyimpan.
	 *
	 * @param oleh identitas pengguna; boleh {@code null}.
	 */
	public void setOleh(String oleh) {
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan id pengguna terakhir yang menyimpan template.
	 *
	 * <p>Dipetakan ke kolom {@code olehid} (huruf kecil semua, tanpa garis bawah).</p>
	 *
	 * @return id pengguna penyimpan terakhir; {@code null} bila disimpan tanpa sesi pengguna.
	 */
	@Column(name = "olehid", nullable = true)
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna penyimpan.
	 *
	 * @param olehId id pengguna; boleh {@code null}.
	 */
	public void setOlehId(String olehId) {
		this.olehId = olehId;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir.
	 *
	 * <p>Terisi otomatis: nilai awal dari inisialisasi field saat objek dibuat, lalu diperbarui
	 * {@link #onUpdate()} pada setiap {@code UPDATE}. Karena entity ini {@code @Audited},
	 * riwayat perubahannya juga tersimpan di tabel {@code _aud} milik Envers &mdash; kolom ini
	 * hanya menampilkan versi terakhir.</p>
	 *
	 * @return waktu perubahan terakhir; praktis tidak pernah {@code null}.
	 */
	@Column(name = "tanggal_dirubah", nullable = true)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Menyetel waktu perubahan terakhir.
	 *
	 * <p>Umumnya tidak perlu dipanggil manual &mdash; {@link #onUpdate()} sudah menanganinya.
	 * Disediakan untuk Hibernate dan untuk skenario impor yang ingin mempertahankan stempel
	 * waktu asal.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
