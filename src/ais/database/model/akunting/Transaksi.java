package ais.database.model.akunting;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;

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

import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;

/**
 * Entity <b>BARIS JURNAL</b> (satu sisi debet <i>atau</i> kredit) pada mesin akuntansi
 * <i>double-entry</i> AIS — tabel <code>akunting.transaksi</code>.
 *
 * <h3>Kedudukan dalam mesin jurnal</h3>
 * <p>
 * AIS memisahkan jurnal menjadi dua tingkat:
 * </p>
 * <ul>
 *   <li><b>Induk / header jurnal</b> — {@link ais.database.model.akunting.GrupTransaksi}
 *       (tabel <code>akunting.grup_transaksi</code>): memuat kode bukti, tanggal transaksi,
 *       keterangan, <code>totalDebet</code>/<code>totalKredit</code>, satuan kerja/workspace
 *       (pembatas tenant yang sesungguhnya), serta puluhan FK ke dokumen sumber
 *       (tagihan siswa, pembayaran gaji, kas kecil, aset, koperasi, dan seterusnya).</li>
 *   <li><b>Anak / baris jurnal</b> — kelas ini. Setiap baris menunjuk satu
 *       {@link ais.database.model.akunting.Akun} (perkiraan/COA) dan membawa nominal pada
 *       kolom {@link #getDebet() debet} <b>dan</b> {@link #getKredit() kredit} yang
 *       <b>terpisah</b> (bukan satu kolom bertanda). Satu induk lazimnya memiliki dua baris
 *       atau lebih sehingga jumlah sisi debet sama dengan sisi kredit.</li>
 * </ul>
 *
 * <h3>Relasi yang TERVERIFIKASI dari kode kelas ini</h3>
 * <ul>
 *   <li>{@link #getGrupTransaksi()} — <code>&#64;ManyToOne</code> ke
 *       {@link ais.database.model.akunting.GrupTransaksi} lewat kolom
 *       <code>grup_transaksi</code>. Perhatikan <b><code>nullable = true</code></b>: secara
 *       skema, baris jurnal <i>boleh</i> yatim tanpa induk. Lihat catatan cakupan tenant
 *       di bawah.</li>
 *   <li>{@link #getAkun()} — <code>&#64;ManyToOne</code> ke
 *       {@link ais.database.model.akunting.Akun} lewat kolom <code>akun</code>, juga
 *       <code>nullable = true</code>. Inilah perkiraan yang didebet/dikredit.</li>
 *   <li>{@link #getAkunOver()} — kolom <code>akun_over</code>, akun "pengganti" hasil
 *       koreksi manual. <b>Bukan sekadar pelengkap</b>: lihat peringatan pada
 *       {@link #getAkun()}.</li>
 *   <li>{@link #getPostingHistory()} — batch posting yang mengunci baris ini
 *       (<code>posting_history</code>).</li>
 *   <li>{@link #getJenisTransaksi()}, {@link #getDevisi()}, {@link #getPegawai()},
 *       {@link #getMatauang()} — relasi opsional pelengkap.</li>
 * </ul>
 *
 * <h3>Status pola Dr/Cr — hasil verifikasi</h3>
 * <p>
 * <b>Tidak ada satu pun logika penentuan/pembalikan debet-kredit di dalam entity ini.</b>
 * Kelas ini murni menampung apa yang disetel pemanggil:
 * </p>
 * <ul>
 *   <li>Idiom lama <code>nilai &gt; 0.1</code> <b>masih hidup</b>, tetapi berada di lapisan
 *       UI/helper, bukan di sini — <code>transaksi.setMerupakanDebet(transaksi.getDebet()
 *       &gt; 0.1)</code> pada <code>GrupTransaksiAction</code>,
 *       <code>TransaksiJurnalUmumHelper</code>, <code>TransaksiJurnalPenerimaanAction</code>
 *       dan <code>TransaksiJurnalPengeluaranAction</code>. Akibatnya nominal debet
 *       0,1 rupiah ke bawah akan diklasifikasikan sebagai kredit.</li>
 *   <li>Pembalikan sisi berdasarkan arah arus kas dilakukan
 *       <code>CommonAkunting.saveTransaksi(...)</code> lewat cabang
 *       <code>apakahUangMasuk</code>: variabel bernama <code>transaksiAkunDebet</code> justru
 *       diisi pada kolom kredit ketika uang keluar. <b>Namun</b>
 *       {@link #setMerupakanDebet(Boolean)} di sana dipanggil <b>tanpa syarat</b>
 *       (<code>true</code> untuk slot debet, <code>false</code> untuk slot kredit). Jadi
 *       {@link #getMerupakanDebet()} menyatakan <i>peran/slot</i> baris di dalam pasangan
 *       jurnal, <b>bukan</b> kolom nominal mana yang benar-benar terisi — kedua nilai itu
 *       bisa saling bertentangan. Jangan pakai flag ini untuk menentukan sisi buku besar;
 *       pakailah nilai {@link #getDebet()}/{@link #getKredit()}.</li>
 * </ul>
 *
 * <h3>Validasi nominal — hasil verifikasi</h3>
 * <p>
 * <b>Tidak ada validasi apa pun di level entity.</b> {@link #setDebet(Double)} dan
 * {@link #setKredit(Double)} adalah setter telanjang: nominal negatif diterima, nominal
 * pada kedua kolom sekaligus diterima, nominal nol diterima. Satu-satunya "pembelaan"
 * adalah getter yang mengganti <code>null</code> menjadi <code>0.0</code>. Sejalan dengan
 * itu, di tingkat induk pun tidak ada penjaga keseimbangan Dr = Cr saat penyimpanan
 * (<code>CommonAkunting.saveTransaksi</code>); keseimbangan hanya ditampilkan sebagai
 * selisih di layar dan diperiksa belakangan pada proses <i>closing</i>.
 * </p>
 *
 * <h3>Cakupan tenant (fail-open struktural)</h3>
 * <p>
 * Entity ini <b>tidak memiliki kolom <code>sekolah</code> maupun <code>yayasan</code></b>,
 * dan juga tidak memiliki <code>satuanKerja</code>/<code>workspace</code>. Satu-satunya
 * pembatas organisasi adalah lewat induk ({@code grupTransaksi.satuanKerja} /
 * {@code grupTransaksi.workspace}). Konsekuensinya, setiap kueri terhadap
 * <code>akunting.transaksi</code> yang <b>tidak</b> mem-<i>join</i> induknya akan menjangkau
 * seluruh instalasi, dan baris yatim (<code>grup_transaksi IS NULL</code>) tidak akan
 * terjangkau filter tenant mana pun. Kolom {@link #getDevisi() devisi} bukan pengganti:
 * setternya tidak pernah dipanggil pada jalur akunting mana pun.
 * </p>
 *
 * <h3>Baris DRAFT hidup di tabel resmi</h3>
 * <p>
 * Kolom {@link #getSimpan() simpan} adalah penanda draft. Selama operator masih mengetik di
 * layar jurnal, baris disimpan ke tabel <code>akunting.transaksi</code> yang sama dengan
 * baris resmi, dengan <code>simpan = false</code>, lalu disapu oleh SQL mentah
 * <code>delete from akunting.transaksi where simpan = false and parent_code = '…'</code>
 * saat tombol Simpan/Tutup ditekan. Pola pembatalan yang sama juga membuang <b>anak lebih
 * dahulu, baru induk</b>: bila setelah penyapuan tidak tersisa baris <code>simpan = true</code>,
 * <code>grupTransaksi</code> ikut dihapus. Bila sesi/tab ditutup tanpa menekan tombol,
 * baris draft tertinggal permanen — dan laporan buku besar/neraca saldo/jurnal harian
 * <b>tidak</b> memfilter kolom ini. Lihat {@link #getSimpan()}.
 * </p>
 *
 * <h3>Getter destruktif — perlakukan pembacaan sebagai penulisan</h3>
 * <p>
 * Kelas dianotasi <code>dynamicUpdate = true</code> dan sejumlah getter <b>menulis balik</b>
 * ke field terpetakan, sehingga sekadar membaca entity di dalam sesi Hibernate yang hidup
 * dapat menerbitkan <code>UPDATE</code> pada baris jurnal resmi. Yang menyentuh uang/akun:
 * {@link #getAkun()} (memindahkan perkiraan secara permanen),
 * {@link #getStatusPosting()} dan {@link #getTanggalPosting()} (menimpa status/tanggal
 * posting), serta {@link #getBulan()}/{@link #getTahun()} (menstempel periode akuntansi
 * dengan bulan/tahun <i>hari ini</i>). {@link #getRandomValue()} bahkan menerbitkan nilai
 * acak baru pada pembacaan pertama.
 * </p>
 *
 * <h3>Jejak audit</h3>
 * <p>
 * Kelas dianotasi {@link org.hibernate.envers.Audited}, sehingga setiap versi baris jurnal
 * digandakan ke tabel revisi <code>akunting.transaksi_aud</code>. Kolom {@link #getOleh()
 * oleh}/{@link #getOlehId() olehId} dan {@link #getTanggal_dirubah() tanggal_dirubah}
 * diisi oleh <code>AuditTimestampInterceptor</code>.
 * </p>
 *
 * <h3>Pengelompokan method</h3>
 * <ol>
 *   <li><b>Identitas &amp; audit</b> — {@link #getId()}, {@link #getOleh()},
 *       {@link #getOlehId()}, {@link #getTanggal_dirubah()}, {@link #onUpdate()},
 *       {@link #toString()}.</li>
 *   <li><b>Penanda jurnal</b> — {@link #getKode()}, {@link #getParentCode()},
 *       {@link #getJenisJurnal()}, {@link #getGrupTransaksi()},
 *       {@link #getJenisTransaksi()}, {@link #getKeterangan()}.</li>
 *   <li><b>Waktu &amp; periode</b> — {@link #getTanggalTransaksi()},
 *       {@link #getTanggalDimasukkan()}, {@link #getBulan()}, {@link #getTahun()},
 *       {@link #getJatuhTempo()}, {@link #getTanggalBayar()}.</li>
 *   <li><b>Akun &amp; nominal</b> — {@link #getAkun()}, {@link #getAkunOver()},
 *       {@link #getSubAkun()}, {@link #getDebet()}, {@link #getKredit()},
 *       {@link #getMerupakanDebet()}, {@link #getJumlahTransaksi()}.</li>
 *   <li><b>Mata uang</b> — {@link #getMatauang()}, {@link #getCurrencyCurs()},
 *       {@link #getNilaiRupiah()} (praktis tidak terpakai).</li>
 *   <li><b>Posting &amp; pemeriksaan</b> — {@link #getPostingHistory()},
 *       {@link #getStatusPosting()}, {@link #getTanggalPosting()},
 *       {@link #getStatusPemeriksaan()}.</li>
 *   <li><b>Siklus hidup draft</b> — {@link #getSimpan()}, {@link #getRandomValue()}.</li>
 *   <li><b>Pelengkap organisasi</b> — {@link #getDevisi()}, {@link #getPegawai()}.</li>
 * </ol>
 *
 * <p>
 * Kelas kembaran struktural (salin-tempel, kolom hampir identik) adalah
 * {@link ais.database.model.akunting.TemplateTransaksi} — cetakan baris jurnal berulang.
 * </p>
 *
 * @see ais.database.model.akunting.GrupTransaksi
 * @see ais.database.model.akunting.Akun
 * @see ais.database.model.akunting.PostingHistory
 * @see ais.database.model.akunting.TemplateTransaksi
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "akunting", name = "transaksi")
public class Transaksi extends GeneralValueObject {

	/**
	 * Nilai {@link #getStatusPosting()} untuk baris jurnal yang <b>belum</b> diposting,
	 * yaitu baris yang belum terikat ke satu {@link PostingHistory}.
	 */
	public static final Integer STATUS_POSTING_BELUM = 0;
	/**
	 * Nilai {@link #getStatusPosting()} untuk baris jurnal yang <b>sudah</b> diposting.
	 *
	 * <p>Dipakai juga sebagai nilai pembanding pada kueri Criteria
	 * (<code>Restrictions.eq("statusPosting", STATUS_POSTING_SELESAI)</code>) di helper
	 * jurnal penerimaan/pengeluaran; kueri tersebut membaca kolom tersimpan, bukan hasil
	 * perhitungan ulang getter.</p>
	 */
	public static final Integer STATUS_POSTING_SELESAI = 1;

	/**
	 * Nilai {@link #getStatusPemeriksaan()} untuk baris yang belum diperiksa (nilai
	 * bawaan). Kolom pemeriksaan tidak pernah diubah oleh kode mana pun — lihat
	 * {@link #getStatusPemeriksaan()}.
	 */
	public static final Integer STATUS_PEMERIKSAAN_BELUM = 0;
	/**
	 * Nilai {@link #getStatusPemeriksaan()} untuk baris yang sudah diperiksa. Tidak ada
	 * pemanggil {@link #setStatusPemeriksaan(Integer)} di seluruh repo, sehingga konstanta
	 * ini praktis tidak pernah tersimpan.
	 */
	public static final Integer STATUS_PEMERIKSAAN_SELESAI = 1;

	/** Nilai {@link #getJenisJurnal()} untuk jurnal penerimaan kas. */
	public static final String JURNAL_KAS_MASUK = "Kas Masuk";
	/** Nilai {@link #getJenisJurnal()} untuk jurnal pengeluaran kas. */
	public static final String JURNAL_KAS_KELUAR = "Kas Keluar";
	/** Nilai {@link #getJenisJurnal()} untuk jurnal umum (termasuk jurnal penyesuaian/penutup). */
	public static final String JURNAL_UMUM = "Umum";
	/**
	 * Nilai {@link #getJenisJurnal()} untuk baris yang dibangkitkan otomatis oleh mesin
	 * posting <code>CommonAkunting.saveTransaksi(...)</code> dari dokumen sumber
	 * (pembayaran siswa, gaji, aset, koperasi, dan seterusnya) — bukan dari layar jurnal
	 * manual.
	 */
	public static final String JURNAL_TRANSAKSI = "Transaksi";

	/**
	 * Versi serialisasi Java.
	 *
	 * <p><b>Catatan:</b> nilai ini identik dengan sejumlah entity lain di paket
	 * <code>akunting</code> (mis. {@link GrupTransaksi}, {@link PostingHistory}) karena
	 * berkas-berkas tersebut lahir dari satu cetakan salin-tempel.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci primer baris jurnal (kolom <code>id</code>, IDENTITY). */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi interceptor audit. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; diisi interceptor audit. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris jurnal ini.
	 *
	 * @return id pengguna pengubah, atau <code>null</code> bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah baris jurnal ini.
	 *
	 * <p><b>Non-obvious:</b> nilai <code>null</code>, string kosong, atau string yang hanya
	 * berisi spasi <b>diabaikan diam-diam</b> (method langsung <code>return</code>) sehingga
	 * nilai lama dipertahankan. Artinya jejak audit tidak dapat dikosongkan sekali sudah
	 * terisi, dan pemanggil tidak mendapat umpan balik apa pun bahwa nilainya ditolak.</p>
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila kosong/null
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks baris jurnal, yaitu isi {@link #getKeterangan() keterangan} apa
	 * adanya.
	 *
	 * <p><b>Non-obvious:</b> method ini membaca field <code>keterangan</code> secara
	 * langsung (bukan lewat getter) dan tidak menjaga <code>null</code>. Karena
	 * <code>keterangan</code> dapat disetel <code>null</code> lewat
	 * {@link #setKeterangan(String)}, method ini dapat mengembalikan <code>null</code> —
	 * berisiko bagi komponen ZK yang memanggil <code>toString()</code> untuk label.</p>
	 *
	 * @return keterangan baris jurnal, mungkin <code>null</code>
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Menyetel nama pengguna pengubah baris jurnal ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: nilai kosong/null <b>diabaikan
	 * diam-diam</b> dan nilai lama dipertahankan.</p>
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila kosong/null
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris jurnal ini.
	 *
	 * @return nama pengguna pengubah, atau <code>null</code> bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait <code>&#64;PreUpdate</code> JPA yang menyerahkan pencatatan jejak audit
	 * (<code>oleh</code>, <code>olehId</code>, <code>tanggal_dirubah</code>) kepada
	 * <code>ais.database.hibernate.AuditTimestampInterceptor</code> sesaat sebelum
	 * Hibernate menerbitkan <code>UPDATE</code>.
	 *
	 * <p>Dipanggil oleh Hibernate, bukan oleh kode aplikasi. Karena kait ini bereaksi pada
	 * <i>setiap</i> UPDATE, getter destruktif yang disebut pada Javadoc kelas juga akan
	 * memicu pembaruan stempel audit — riwayat Envers bisa memuat revisi yang tidak pernah
	 * berasal dari tindakan pengguna.</p>
	 *
	 * <p><b>Catatan gaya:</b> baris di bawah ini sengaja memuat deklarasi method dan
	 * deklarasi field <code>tanggal_dirubah</code> sekaligus — bentuk salin-tempel yang
	 * seragam di seluruh entity AIS. Jangan dipecah agar mudah dibandingkan antar berkas.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir baris jurnal ini.
	 *
	 * <p>Lazimnya dipanggil oleh <code>AuditTimestampInterceptor</code> lewat
	 * {@link #onUpdate()}, bukan oleh kode layar.</p>
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris jurnal ini (kolom
	 * <code>tanggal_dirubah</code>, presisi TIMESTAMP).
	 *
	 * @return stempel waktu perubahan terakhir; secara bawaan berisi waktu pembuatan objek
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Kode bukti jurnal; disalin dari {@code grupTransaksi.kode} atau dari {@link #parentCode}. */
	private String kode;
	/** Jenis buku jurnal; salah satu dari konstanta <code>JURNAL_*</code>. */
	private String jenisJurnal;
	/** Tanggal efektif transaksi; disegarkan dari induk oleh {@link #getTanggalTransaksi()}. */
	private Date tanggalTransaksi = ais.ui.util.WaktuUtil.getDate();
	/** Tanggal baris ini dimasukkan ke sistem. */
	private Date tanggalDimasukkan = ais.ui.util.WaktuUtil.getDate();
	/** Bulan periode akuntansi (1&ndash;12); dihitung lazy oleh {@link #getBulan()}. */
	private Integer bulan;
	/** Tahun periode akuntansi; dihitung lazy oleh {@link #getTahun()}. */
	private Integer tahun;
	/** Divisi organisasi; kolom warisan modul SIRS, tidak dipakai jalur akunting. */
	private Devisi devisi;
	/** Pegawai yang terkait baris jurnal ini (opsional). */
	private Pegawai pegawai;
	/** Uraian baris jurnal; juga menjadi nilai {@link #toString()}. */
	private String keterangan = "";
	/** Perkiraan/COA yang didebet atau dikredit oleh baris ini. */
	private Akun akun;
	/** Perkiraan pengganti hasil koreksi manual; menimpa {@link #akun} pada setiap pembacaan. */
	private Akun akunOver;
	/** Mata uang baris; praktis tidak pernah diisi. */
	private Matauang matauang;
	/** Sub-perkiraan bebas berupa teks; tidak ada pemanggil setternya di seluruh repo. */
	private String subAkun;
	/** Penanda peran/slot baris di dalam pasangan jurnal — lihat {@link #getMerupakanDebet()}. */
	private Boolean merupakanDebet;
	/** Nominal sisi debet (rupiah); nol bila baris ini bersisi kredit. */
	private Double debet = 0.0;
	/** Nominal sisi kredit (rupiah); nol bila baris ini bersisi debet. */
	private Double kredit = 0.0;
	/** Tanggal jatuh tempo (opsional, untuk baris berkarakter utang/piutang). */
	private Date jatuhTempo;
	/** Tanggal pembayaran (opsional). */
	private Date tanggalBayar;
	/** Tanggal posting; dinolkan ulang oleh {@link #getTanggalPosting()} bila tanpa batch posting. */
	private Date tanggalPosting;
	/** Status posting tersimpan; dihitung ulang oleh {@link #getStatusPosting()} pada tiap pembacaan. */
	private Integer statusPosting = STATUS_POSTING_BELUM;
	/** Status pemeriksaan; tidak pernah diubah oleh kode mana pun. */
	private Integer statusPemeriksaan = STATUS_PEMERIKSAAN_BELUM;

	/**
	 * Penanda baris resmi (<code>true</code>) versus baris draft (<code>false</code>).
	 * Lihat {@link #getSimpan()} untuk mekanisme penyapuan draft.
	 */
	private Boolean simpan = false;

	/** Nominal ringkas hasil turunan debet/kredit; tidak ada pembacanya — lihat {@link #getJumlahTransaksi()}. */
	private Double jumlahTransaksi = 0.0;
	/** Kurs mata uang; tidak ada pemanggil setternya. */
	private Double currencyCurs = 0.0;
	/** Nilai ekuivalen rupiah; tidak ada pemanggil setternya. */
	private Double nilaiRupiah = 0.0;

	/** Kode korelasi satu sesi penyusunan jurnal; kunci penyapuan draft. */
	private String parentCode;
	/** Induk/header jurnal tempat baris ini bernaung; FK <code>grup_transaksi</code>, nullable. */
	private GrupTransaksi grupTransaksi;
	// private Workspace defaultWorkspace;
	// private AkunPajak akunPajak;

	/** Klasifikasi transaksi; diwarisi dari induk bila kosong. */
	private JenisTransaksi jenisTransaksi;

	/** Nilai acak pembeda baris; dibangkitkan saat pembacaan pertama. */
	private Long randomValue;

	/** Batch posting yang mengunci baris ini; keberadaannya menentukan {@link #getStatusPosting()}. */
	private PostingHistory postingHistory;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Objek yang dihasilkan sudah memiliki nilai bawaan yang aman untuk nominal
	 * ({@link #debet} dan {@link #kredit} bernilai <code>0.0</code>), status posting
	 * {@link #STATUS_POSTING_BELUM}, status pemeriksaan {@link #STATUS_PEMERIKSAAN_BELUM},
	 * penanda draft {@link #simpan} bernilai <code>false</code>, serta tanggal transaksi
	 * dan tanggal dimasukkan berisi waktu saat ini.</p>
	 */
	public Transaksi() {
	}

	/**
	 * Mengembalikan kunci primer baris jurnal.
	 *
	 * @return id baris, atau <code>null</code> bila objek belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci primer baris jurnal.
	 *
	 * @param id kunci primer baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan uraian baris jurnal (kolom <code>keterangan</code>).
	 *
	 * @return uraian baris jurnal; bawaan string kosong, dapat menjadi <code>null</code>
	 *         bila disetel demikian
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel uraian baris jurnal.
	 *
	 * <p>Tidak ada penyaringan/pembersihan masukan di sini; teks disimpan apa adanya dan
	 * kelak ditayangkan lewat {@link #toString()}.</p>
	 *
	 * @param keterangan uraian baris jurnal
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menyetel tanggal efektif transaksi.
	 *
	 * <p><b>Perhatian:</b> nilai yang disetel di sini akan <b>ditimpa kembali</b> oleh
	 * {@link #getTanggalTransaksi()} bila baris sudah tertaut ke induk — induklah yang
	 * memegang tanggal resmi.</p>
	 *
	 * @param tanggalTransaksi tanggal efektif transaksi
	 */
	public void setTanggalTransaksi(Date tanggalTransaksi) {
		this.tanggalTransaksi = tanggalTransaksi;
	}

	/**
	 * Mengembalikan tanggal efektif transaksi baris jurnal ini.
	 *
	 * <p><b>Efek samping (getter penulis):</b> bila {@link #getGrupTransaksi() induk} tidak
	 * <code>null</code>, field <code>tanggalTransaksi</code> <b>ditimpa</b> dengan tanggal
	 * induk. Karena kelas ini <code>dynamicUpdate = true</code>, sekadar membaca baris di
	 * dalam sesi Hibernate yang hidup dapat menerbitkan <code>UPDATE</code> pada kolom
	 * <code>tanggal_transaksi</code>. Perilaku ini disengaja agar tanggal anak selalu
	 * mengikuti header, tetapi berarti perubahan tanggal induk merambat ke baris lama secara
	 * senyap.</p>
	 *
	 * <p>Bila setelah penyegaran nilainya masih <code>null</code>, method jatuh ke
	 * {@link #getTanggalDimasukkan()} sebagai pengganti (nilai yang dikembalikan saja —
	 * field tidak diisi).</p>
	 *
	 * @return tanggal transaksi induk bila ada, jika tidak tanggal transaksi baris, dan
	 *         sebagai pilihan terakhir tanggal dimasukkan; tidak pernah <code>null</code>
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_transaksi", nullable = true)
	public Date getTanggalTransaksi() {
		if (grupTransaksi != null) {
			tanggalTransaksi = grupTransaksi.getTanggalTransaksi();
		}
		return tanggalTransaksi == null ? getTanggalDimasukkan() : tanggalTransaksi;
	}

	/**
	 * Menyetel tanggal baris jurnal dimasukkan ke sistem.
	 *
	 * @param tanggalDimasukkan tanggal pemasukan data
	 */
	public void setTanggalDimasukkan(Date tanggalDimasukkan) {
		this.tanggalDimasukkan = tanggalDimasukkan;
	}

	/**
	 * Mengembalikan tanggal baris jurnal dimasukkan ke sistem.
	 *
	 * <p>Bila field bernilai <code>null</code>, method mengembalikan waktu saat ini
	 * <b>tanpa</b> menuliskannya ke field (berbeda dari {@link #getBulan()}/{@link #getTahun()}
	 * yang menulis balik).</p>
	 *
	 * @return tanggal pemasukan data; tidak pernah <code>null</code>
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dimasukkan", nullable = true)
	public Date getTanggalDimasukkan() {
		return tanggalDimasukkan == null ? ais.ui.util.WaktuUtil.getDate() : tanggalDimasukkan;
	}

	/**
	 * Menyetel bulan periode akuntansi baris jurnal ini.
	 *
	 * <p>Nilai yang diharapkan adalah 1&ndash;12 (bukan indeks {@link Calendar} yang dimulai
	 * dari 0). Mesin posting mengisinya dengan
	 * <code>calendar.get(Calendar.MONTH) + 1</code>. Tidak ada validasi rentang.</p>
	 *
	 * @param bulan bulan periode akuntansi, 1&ndash;12
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Mengembalikan bulan periode akuntansi baris jurnal ini.
	 *
	 * <p><b>Efek samping (getter penulis) — berdampak pada periode akuntansi:</b> bila field
	 * masih <code>null</code>, method <b>menuliskan bulan berjalan saat ini</b> ke field.
	 * Baris jurnal lama yang kolom <code>bulan</code>-nya kosong karena itu akan
	 * "dipindahkan" ke periode berjalan hanya karena dibaca, dan perubahan tersebut ikut
	 * ter-<code>flush</code> ke basis data beserta salinan revisi Envers-nya. Bulan yang
	 * benar seharusnya diturunkan dari {@link #getTanggalTransaksi()}, bukan dari jam
	 * dinding.</p>
	 *
	 * @return bulan periode akuntansi 1&ndash;12; tidak pernah <code>null</code>
	 */
	public Integer getBulan() {
		if (bulan == null) {
			bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		}
		return bulan;
	}

	/**
	 * Menyetel tahun periode akuntansi baris jurnal ini.
	 *
	 * @param tahun tahun periode akuntansi (empat digit)
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan tahun periode akuntansi baris jurnal ini.
	 *
	 * <p><b>Efek samping (getter penulis):</b> sama seperti {@link #getBulan()}, bila field
	 * masih <code>null</code> method menuliskan <b>tahun berjalan saat ini</b> ke field,
	 * sehingga membaca baris jurnal lintas tahun buku dapat menstempel ulang periodenya.</p>
	 *
	 * @return tahun periode akuntansi; tidak pernah <code>null</code>
	 */
	public Integer getTahun() {
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * Menyetel divisi organisasi baris jurnal ini.
	 *
	 * <p><b>Catatan verifikasi:</b> tidak ada satu pun jalur akunting yang memanggil setter
	 * ini (satu-satunya pemanggil <code>setDevisi</code> di repo adalah entity lain pada
	 * modul SIRS). Kolom <code>devisi</code> pada tabel <code>akunting.transaksi</code>
	 * karena itu selalu kosong dan <b>tidak dapat dipakai sebagai pembatas
	 * organisasi/tenant</b>.</p>
	 *
	 * @param devisi divisi organisasi
	 */
	public void setDevisi(Devisi devisi) {
		this.devisi = devisi;
	}

	/**
	 * Mengembalikan divisi organisasi baris jurnal ini.
	 *
	 * <p>Nilai dilewatkan {@link ais.database.model.GeneralValueObject#check(Object)}
	 * terlebih dahulu untuk menyelesaikan proxy lazy Hibernate dan memetakannya ke instance
	 * kanonik. Hasil <code>check</code> ditulis balik ke field — ini penulisan referensi
	 * objek, bukan perubahan nilai kolom, sehingga tidak menerbitkan UPDATE.</p>
	 *
	 * @return divisi organisasi, atau <code>null</code> (dalam praktiknya selalu
	 *         <code>null</code>; lihat {@link #setDevisi(Devisi)})
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "devisi", nullable = true)
	public Devisi getDevisi() {
		devisi = check(devisi);
		return devisi;
	}

	/**
	 * Menyetel pegawai yang terkait dengan baris jurnal ini.
	 *
	 * @param pegawai pegawai terkait, boleh <code>null</code>
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Mengembalikan pegawai yang terkait dengan baris jurnal ini.
	 *
	 * <p>Nilai dilewatkan {@link ais.database.model.GeneralValueObject#check(Object)} untuk
	 * de-proxy dan kanonikalisasi instance.</p>
	 *
	 * @return pegawai terkait, atau <code>null</code>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = true)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/**
	 * Menyetel perkiraan (COA) yang didebet/dikredit oleh baris jurnal ini.
	 *
	 * <p><b>Perhatian:</b> penyetelan ini <b>tidak permanen</b> selama
	 * {@link #getAkunOver()} berisi nilai — {@link #getAkun()} akan menimpanya kembali pada
	 * pembacaan berikutnya. Untuk benar-benar memindahkan baris ke perkiraan lain,
	 * {@link #setAkunOver(Akun)} harus dikosongkan lebih dahulu.</p>
	 *
	 * @param akun perkiraan tujuan, boleh <code>null</code>
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Mengembalikan perkiraan (COA) yang didebet/dikredit oleh baris jurnal ini.
	 *
	 * <p><b>PENTING — getter destruktif atas atribusi akun baris jurnal resmi.</b> Bila
	 * {@link #getAkunOver()} berisi nilai, field <code>akun</code> <b>ditimpa</b> dengan
	 * <code>akunOver</code> lalu dikembalikan. Konsekuensinya:</p>
	 * <ul>
	 *   <li>Karena kelas ini <code>dynamicUpdate = true</code> dan Hibernate memanggil getter
	 *       ini saat pemeriksaan <i>dirty</i>, membaca baris di dalam sesi hidup akan
	 *       menerbitkan <code>UPDATE akunting.transaksi SET akun = akun_over</code>. Nilai
	 *       akun asli baris jurnal <b>hilang dari tabel utama</b> dan hanya tersisa di tabel
	 *       revisi Envers <code>akunting.transaksi_aud</code>.</li>
	 *   <li>Penimpaan ini bersifat <b>satu arah dan permanen</b>: selama
	 *       <code>akun_over</code> tidak dikosongkan, upaya mengembalikan akun asli lewat
	 *       {@link #setAkun(Akun)} akan dibatalkan senyap pada pembacaan berikutnya.</li>
	 *   <li>Satu-satunya penulis <code>akunOver</code> adalah dialog koreksi akun pada
	 *       <code>TransaksiJurnalUmumHelper</code>, yang memang bermaksud memindahkan baris.
	 *       Namun karena penimpaan terjadi di dalam getter, efeknya juga berlaku bagi jalur
	 *       baca-saja mana pun (laporan, dasbor, API) yang menyentuh baris tersebut.</li>
	 * </ul>
	 *
	 * <p>Bila <code>akunOver</code> kosong, nilai <code>akun</code> hanya dilewatkan
	 * {@link ais.database.model.GeneralValueObject#check(Object)} untuk de-proxy.</p>
	 *
	 * @return perkiraan efektif baris jurnal ini (akun pengganti bila ada), atau
	 *         <code>null</code>
	 * @see #getAkunOver()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		if (getAkunOver() != null) {
			akun = getAkunOver();
		} else {
			akun = check(akun);
		}
		return akun;
	}

	/**
	 * Menyetel sub-perkiraan bebas berupa teks.
	 *
	 * <p><b>Catatan verifikasi:</b> tidak ada pemanggil setter ini di seluruh repo — kolom
	 * <code>sub_akun</code> merupakan kolom warisan yang tidak pernah terisi.</p>
	 *
	 * @param subAkun kode/keterangan sub-perkiraan
	 */
	public void setSubAkun(String subAkun) {
		this.subAkun = subAkun;
	}

	/**
	 * Mengembalikan sub-perkiraan bebas berupa teks (kolom <code>sub_akun</code>).
	 *
	 * @return sub-perkiraan, dalam praktiknya selalu <code>null</code>
	 */
	@Column(name = "sub_akun", nullable = true)
	public String getSubAkun() {
		return subAkun;
	}

	/**
	 * Menyetel nominal sisi <b>debet</b> baris jurnal (rupiah).
	 *
	 * <p><b>Tidak ada validasi apa pun.</b> Nilai negatif, nilai nol, maupun pengisian
	 * bersamaan dengan {@link #setKredit(Double)} (baris terisi di kedua sisi sekaligus)
	 * semuanya diterima tanpa peringatan. Penjagaan keseimbangan Dr = Cr juga tidak
	 * dilakukan di tingkat entity maupun pada
	 * <code>CommonAkunting.saveTransaksi(...)</code>.</p>
	 *
	 * <p>Mesin posting selalu memanggil setter ini berpasangan dengan
	 * <code>setKredit(0.0)</code> dan menggunakan <code>Math.abs(nilai)</code>, sehingga
	 * nominal negatif hanya bisa muncul lewat jalur masukan manual.</p>
	 *
	 * @param debet nominal sisi debet
	 */
	public void setDebet(Double debet) {
		this.debet = debet;
	}

	/**
	 * Mengembalikan nominal sisi <b>debet</b> baris jurnal (rupiah).
	 *
	 * <p>Bila field bernilai <code>null</code>, field diisi <code>0.0</code> lalu
	 * dikembalikan — penulisan balik yang sengaja dilakukan agar penjumlahan di laporan
	 * tidak melempar <code>NullPointerException</code> saat <i>unboxing</i>.</p>
	 *
	 * @return nominal sisi debet; tidak pernah <code>null</code>
	 */
	public Double getDebet() {
		if (debet == null) {
			debet = 0.0;
		}
		return debet;
	}

	/**
	 * Menyetel nominal sisi <b>kredit</b> baris jurnal (rupiah).
	 *
	 * <p>Sama seperti {@link #setDebet(Double)}: tidak ada validasi tanda, rentang, maupun
	 * saling-eksklusif terhadap sisi debet.</p>
	 *
	 * @param kredit nominal sisi kredit
	 */
	public void setKredit(Double kredit) {
		this.kredit = kredit;
	}

	/**
	 * Mengembalikan nominal sisi <b>kredit</b> baris jurnal (rupiah).
	 *
	 * <p>Bila field bernilai <code>null</code>, field diisi <code>0.0</code> lalu
	 * dikembalikan.</p>
	 *
	 * @return nominal sisi kredit; tidak pernah <code>null</code>
	 */
	public Double getKredit() {
		if (kredit == null) {
			kredit = 0.0;
		}
		return kredit;
	}

	/**
	 * Menyetel tanggal jatuh tempo baris jurnal (untuk baris berkarakter utang/piutang).
	 *
	 * @param jatuhTempo tanggal jatuh tempo, boleh <code>null</code>
	 */
	public void setJatuhTempo(Date jatuhTempo) {
		this.jatuhTempo = jatuhTempo;
	}

	/**
	 * Mengembalikan tanggal jatuh tempo baris jurnal (kolom <code>jatuh_tempo</code>,
	 * presisi DATE).
	 *
	 * @return tanggal jatuh tempo, atau <code>null</code> bila tidak relevan
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "jatuh_tempo", nullable = true)
	public Date getJatuhTempo() {
		return jatuhTempo;
	}

	/**
	 * Menyetel tanggal pembayaran yang terkait baris jurnal ini.
	 *
	 * @param tanggalBayar tanggal pembayaran, boleh <code>null</code>
	 */
	public void setTanggalBayar(Date tanggalBayar) {
		this.tanggalBayar = tanggalBayar;
	}

	/**
	 * Mengembalikan tanggal pembayaran yang terkait baris jurnal ini (kolom
	 * <code>tanggal_bayar</code>, presisi DATE).
	 *
	 * @return tanggal pembayaran, atau <code>null</code>
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_bayar", nullable = true)
	public Date getTanggalBayar() {
		return tanggalBayar;
	}

	/**
	 * Menyetel tanggal posting baris jurnal ini.
	 *
	 * <p><b>Perhatian:</b> nilai ini hanya bertahan selama {@link #getPostingHistory()}
	 * tidak <code>null</code>; {@link #getTanggalPosting()} akan menolkannya kembali bila
	 * batch posting tidak ada.</p>
	 *
	 * @param tanggalPosting tanggal posting
	 */
	public void setTanggalPosting(Date tanggalPosting) {
		this.tanggalPosting = tanggalPosting;
	}

	/**
	 * Mengembalikan tanggal posting baris jurnal ini (kolom <code>tanggal_posting</code>,
	 * presisi DATE).
	 *
	 * <p><b>Efek samping (getter destruktif):</b> bila {@link #getPostingHistory()} bernilai
	 * <code>null</code>, field <code>tanggalPosting</code> <b>dinolkan</b> lalu
	 * dikembalikan. Method ini hanya bisa <i>menghapus</i> tanggal, tidak pernah
	 * mengisinya dari batch posting — pengisian tetap menjadi tanggung jawab pemanggil
	 * {@link #setTanggalPosting(Date)}. Karena <code>dynamicUpdate = true</code>, pembacaan
	 * di dalam sesi hidup atas baris yang batch postingnya sudah dilepas akan
	 * mengosongkan kolom di basis data.</p>
	 *
	 * @return tanggal posting bila baris terikat batch posting, jika tidak <code>null</code>
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_posting", nullable = true)
	public Date getTanggalPosting() {
		tanggalPosting = getPostingHistory() == null ? null : tanggalPosting;
		return tanggalPosting;
	}

	/**
	 * Menyetel status posting baris jurnal ini.
	 *
	 * <p><b>Non-obvious:</b> nilai yang disetel di sini <b>tidak pernah terbaca kembali</b>
	 * lewat {@link #getStatusPosting()}, karena getter tersebut selalu menghitung ulang dari
	 * ada-tidaknya {@link #getPostingHistory()}. Setter ini tetap bermakna karena kolom
	 * <code>status_posting</code> yang tersimpan dipakai langsung oleh kueri Criteria
	 * (<code>Restrictions.eq("statusPosting", …)</code>) dan pengurutan pada layar posting
	 * harian.</p>
	 *
	 * @param statusPosting {@link #STATUS_POSTING_BELUM} atau {@link #STATUS_POSTING_SELESAI}
	 */
	public void setStatusPosting(Integer statusPosting) {
		this.statusPosting = statusPosting;
	}

	/**
	 * Mengembalikan status posting baris jurnal ini (kolom <code>status_posting</code>).
	 *
	 * <p><b>Efek samping (getter destruktif):</b> field <b>selalu ditimpa</b> dengan hasil
	 * perhitungan <code>postingHistory == null ? BELUM : SELESAI</code>, apa pun nilai yang
	 * tersimpan. Nilai yang pernah disetel {@link #setStatusPosting(Integer)} karena itu
	 * dapat berubah sendiri saat baris dibaca, dan perubahan tersebut ikut ter-<i>flush</i>
	 * ke basis data.</p>
	 *
	 * <p><b>Non-obvious — status ini tidak menghormati pembatalan posting.</b> Yang diperiksa
	 * hanyalah <i>keberadaan</i> {@link PostingHistory}, bukan bendera
	 * <code>postingHistory.posting</code>. Baris yang batch postingnya sudah dibatalkan
	 * (<code>posting = false</code>) tetap dilaporkan {@link #STATUS_POSTING_SELESAI} oleh
	 * method ini. Bagian lain sistem (layar posting, laporan buku besar) memeriksa
	 * <code>postingHistory.posting</code> secara eksplisit, sehingga tampilan status baris
	 * dapat berbeda dari kenyataan yang dipakai laporan.</p>
	 *
	 * @return {@link #STATUS_POSTING_SELESAI} bila baris terikat batch posting mana pun,
	 *         jika tidak {@link #STATUS_POSTING_BELUM}
	 */
	@Column(name = "status_posting", nullable = true)
	public Integer getStatusPosting() {
		statusPosting = getPostingHistory() == null ? STATUS_POSTING_BELUM : STATUS_POSTING_SELESAI;
		return statusPosting;
	}

	/**
	 * Menyetel kode bukti jurnal baris ini.
	 *
	 * <p>Mesin posting mengisinya dengan <code>grupTransaksi.getKode()</code>, sedangkan
	 * layar jurnal manual mengisinya dengan {@link #getParentCode() parentCode} — jadi
	 * kolom ini merupakan denormalisasi kode induk, bukan identitas mandiri.</p>
	 *
	 * @param kode kode bukti jurnal
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan kode bukti jurnal baris ini.
	 *
	 * @return kode bukti jurnal, atau <code>null</code> bila belum diisi
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Menyetel status pemeriksaan baris jurnal ini.
	 *
	 * <p><b>Catatan verifikasi:</b> tidak ada pemanggil setter ini di seluruh repo. Alur
	 * pemeriksaan/verifikasi jurnal yang direncanakan konstanta
	 * <code>STATUS_PEMERIKSAAN_*</code> tidak pernah diimplementasikan — kolom
	 * <code>status_pemeriksaan</code> selamanya bernilai bawaan
	 * {@link #STATUS_PEMERIKSAAN_BELUM}.</p>
	 *
	 * @param statusPemeriksaan {@link #STATUS_PEMERIKSAAN_BELUM} atau
	 *        {@link #STATUS_PEMERIKSAAN_SELESAI}
	 */
	public void setStatusPemeriksaan(Integer statusPemeriksaan) {
		this.statusPemeriksaan = statusPemeriksaan;
	}

	/**
	 * Mengembalikan status pemeriksaan baris jurnal ini (kolom
	 * <code>status_pemeriksaan</code>).
	 *
	 * <p>Berbeda dari {@link #getStatusPosting()}, method ini adalah getter murni tanpa
	 * perhitungan ulang maupun efek samping.</p>
	 *
	 * @return status pemeriksaan; dalam praktiknya selalu {@link #STATUS_PEMERIKSAAN_BELUM}
	 */
	@Column(name = "status_pemeriksaan", nullable = true)
	public Integer getStatusPemeriksaan() {
		return statusPemeriksaan;
	}

	/**
	 * Menyetel penanda peran baris di dalam pasangan jurnal.
	 *
	 * <p><b>Non-obvious — flag ini dapat bertentangan dengan nominal.</b> Ada dua gaya
	 * pengisian yang hidup berdampingan di repo:</p>
	 * <ul>
	 *   <li><b>Turunan dari nominal</b> — idiom lama <code>setMerupakanDebet(getDebet()
	 *       &gt; 0.1)</code> pada <code>GrupTransaksiAction</code>,
	 *       <code>TransaksiJurnalUmumHelper</code>, <code>TransaksiJurnalPenerimaanAction</code>
	 *       dan <code>TransaksiJurnalPengeluaranAction</code>. Ambang <code>0.1</code>
	 *       dimaksudkan sebagai pengganti perbandingan "tidak nol" pada bilangan pecahan,
	 *       tetapi berarti nominal debet 0,1 rupiah ke bawah salah diklasifikasikan sebagai
	 *       kredit.</li>
	 *   <li><b>Konstan menurut slot</b> — <code>CommonAkunting.saveTransaksi(...)</code>
	 *       memanggil <code>setMerupakanDebet(true)</code> untuk baris slot debet dan
	 *       <code>false</code> untuk slot kredit <b>tanpa syarat</b>, padahal pada cabang
	 *       "uang keluar" nominal justru ditulis ke kolom yang berlawanan. Untuk baris hasil
	 *       mesin posting, flag ini karena itu menandai <i>peran dalam pasangan</i>, bukan
	 *       kolom nominal mana yang terisi.</li>
	 * </ul>
	 * <p>Karena kedua gaya itu bercampur di satu tabel, flag ini <b>tidak layak dipakai</b>
	 * untuk menentukan sisi buku besar; gunakan {@link #getDebet()}/{@link #getKredit()}.</p>
	 *
	 * @param merupakanDebet <code>true</code> bila baris berperan sebagai sisi debet
	 */
	public void setMerupakanDebet(Boolean merupakanDebet) {
		this.merupakanDebet = merupakanDebet;
	}

	/**
	 * Mengembalikan penanda peran baris di dalam pasangan jurnal (kolom
	 * <code>merupakan_debet</code>).
	 *
	 * <p>Lihat {@link #setMerupakanDebet(Boolean)} untuk peringatan bahwa nilai ini dapat
	 * bertentangan dengan kolom nominal.</p>
	 *
	 * @return <code>true</code> bila baris ditandai sebagai sisi debet, <code>false</code>
	 *         bila sisi kredit, atau <code>null</code> bila belum pernah ditandai
	 */
	@Column(name = "merupakan_debet", nullable = true)
	public Boolean getMerupakanDebet() {
		return merupakanDebet;
	}

	/**
	 * Menyetel mata uang baris jurnal ini.
	 *
	 * <p><b>Catatan verifikasi:</b> tidak ada pemanggil setter ini di seluruh repo. Dukungan
	 * multi-mata-uang pada tabel <code>akunting.transaksi</code>
	 * ({@link #getMatauang() matauang}, {@link #getCurrencyCurs() currencyCurs},
	 * {@link #getNilaiRupiah() nilaiRupiah}) tidak pernah diaktifkan; seluruh nominal
	 * diperlakukan sebagai rupiah.</p>
	 *
	 * @param matauang mata uang baris jurnal
	 */
	public void setMatauang(Matauang matauang) {
		this.matauang = matauang;
	}

	/**
	 * Mengembalikan mata uang baris jurnal ini.
	 *
	 * <p>Diambil dengan <code>FetchMode.SELECT</code> (kueri terpisah, bukan join) — pola
	 * yang sama dipakai untuk {@link #getGrupTransaksi()} dan {@link #getPostingHistory()}
	 * agar tidak menyeret kolom induk pada setiap baris laporan.</p>
	 *
	 * @return mata uang, dalam praktiknya selalu <code>null</code>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "matauang", nullable = true)
	public Matauang getMatauang() {
		return matauang;
	}

	/**
	 * Menyetel nominal ringkas baris jurnal (kolom <code>jumlah_transaksi</code>).
	 *
	 * <p><b>Non-obvious:</b> satu-satunya pengisi kolom ini adalah
	 * <code>CommonAkunting.saveTransaksi(...)</code>, yang memakai idiom
	 * <code>getKredit() &lt; 1.0 ? getKredit() : getDebet()</code> — artinya "ambil sisi
	 * yang tidak nol", dengan angka 1,0 rupiah sebagai ambang pengganti perbandingan
	 * terhadap nol. Pada salah satu varian <code>saveTransaksi</code> (jalur transaksi
	 * pegawai) kedua cabang idiom tersebut sama-sama mengembalikan <code>getKredit()</code>,
	 * sehingga baris sisi kredit pada transaksi "uang keluar" tercatat
	 * <code>jumlahTransaksi = 0.0</code>.</p>
	 *
	 * <p>Dampak praktisnya nihil: <b>tidak ada satu pun pembaca</b>
	 * {@link #getJumlahTransaksi()} di seluruh repo — kolom ini denormalisasi mati.</p>
	 *
	 * @param jumlahTransaksi nominal ringkas baris jurnal
	 */
	public void setJumlahTransaksi(Double jumlahTransaksi) {
		this.jumlahTransaksi = jumlahTransaksi;
	}

	/**
	 * Mengembalikan nominal ringkas baris jurnal (kolom <code>jumlah_transaksi</code>).
	 *
	 * <p>Tidak ada pemanggil method ini di luar kelas ini; nilai resmi baris jurnal selalu
	 * dibaca dari {@link #getDebet()}/{@link #getKredit()}.</p>
	 *
	 * @return nominal ringkas; bawaan <code>0.0</code>
	 */
	@Column(name = "jumlah_transaksi", nullable = true)
	public Double getJumlahTransaksi() {
		return jumlahTransaksi;
	}

	/**
	 * Menyetel kurs mata uang baris jurnal ini.
	 *
	 * <p><b>Catatan verifikasi:</b> tidak ada pemanggil setter ini di seluruh repo (lihat
	 * {@link #setMatauang(Matauang)}).</p>
	 *
	 * @param currencyCurs kurs terhadap rupiah
	 */
	public void setCurrencyCurs(Double currencyCurs) {
		this.currencyCurs = currencyCurs;
	}

	/**
	 * Mengembalikan kurs mata uang baris jurnal ini (kolom <code>currency_curs</code>).
	 *
	 * @return kurs terhadap rupiah; dalam praktiknya selalu <code>0.0</code>
	 */
	@Column(name = "currency_curs", nullable = true)
	public Double getCurrencyCurs() {
		return currencyCurs;
	}

	/**
	 * Menyetel nilai ekuivalen rupiah baris jurnal ini.
	 *
	 * <p><b>Catatan verifikasi:</b> tidak ada pemanggil setter ini di seluruh repo (lihat
	 * {@link #setMatauang(Matauang)}).</p>
	 *
	 * @param nilaiRupiah nilai ekuivalen dalam rupiah
	 */
	public void setNilaiRupiah(Double nilaiRupiah) {
		this.nilaiRupiah = nilaiRupiah;
	}

	/**
	 * Mengembalikan nilai ekuivalen rupiah baris jurnal ini (kolom
	 * <code>nilai_rupiah</code>).
	 *
	 * @return nilai ekuivalen rupiah; dalam praktiknya selalu <code>0.0</code>
	 */
	@Column(name = "nilai_rupiah", nullable = true)
	public Double getNilaiRupiah() {
		return nilaiRupiah;
	}

	/**
	 * Menyetel kode korelasi sesi penyusunan jurnal.
	 *
	 * <p>Nilai dibangkitkan helper jurnal dengan pola
	 * <code>"PARENT-" + Long.toHexString(waktuMilidetik).toUpperCase()</code>, lalu dipasang
	 * pada induk maupun seluruh baris anaknya. Kode inilah kunci penyapuan draft
	 * ({@code delete from akunting.transaksi where simpan = false and parent_code = '…'})
	 * dan kunci penarikan kembali baris resmi
	 * ({@code Restrictions.eq("simpan", true).add(Restrictions.eq("parentCode", …))}).</p>
	 *
	 * <p>Karena kode ini dirangkai langsung ke dalam SQL mentah oleh helper jurnal, nilainya
	 * <b>harus tetap dibangkitkan server</b>; jangan pernah mengisinya dari masukan
	 * pengguna.</p>
	 *
	 * @param parentCode kode korelasi sesi penyusunan jurnal
	 */
	public void setParentCode(String parentCode) {
		this.parentCode = parentCode;
	}

	/**
	 * Mengembalikan kode korelasi sesi penyusunan jurnal (kolom <code>parent_code</code>).
	 *
	 * @return kode korelasi, atau <code>null</code> untuk baris yang tidak berasal dari
	 *         layar jurnal
	 */
	@Column(name = "parent_code", nullable = true)
	public String getParentCode() {
		return parentCode;
	}

	/**
	 * Menyetel jenis buku jurnal baris ini.
	 *
	 * @param jenisJurnal salah satu dari {@link #JURNAL_KAS_MASUK},
	 *        {@link #JURNAL_KAS_KELUAR}, {@link #JURNAL_UMUM}, atau
	 *        {@link #JURNAL_TRANSAKSI}
	 */
	public void setJenisJurnal(String jenisJurnal) {
		this.jenisJurnal = jenisJurnal;
	}

	/**
	 * Mengembalikan jenis buku jurnal baris ini (kolom <code>jenis_jurnal</code>).
	 *
	 * <p>Nilai berupa teks bebas, bukan enum — kesalahan ketik pada pemanggil tidak
	 * terdeteksi kompiler dan hanya berakibat baris hilang dari filter
	 * <code>Restrictions.in("jenisJurnal", …)</code> di layar posting harian.</p>
	 *
	 * @return jenis buku jurnal, atau <code>null</code>
	 */
	@Column(name = "jenis_jurnal", nullable = true)
	public String getJenisJurnal() {
		return jenisJurnal;
	}

	/**
	 * Menautkan baris jurnal ini ke induk/header jurnalnya.
	 *
	 * <p>Setelah tautan ini terpasang, {@link #getTanggalTransaksi()} dan
	 * {@link #getJenisTransaksi()} akan menyegarkan diri dari induk pada setiap
	 * pembacaan.</p>
	 *
	 * @param grupTransaksi induk/header jurnal, boleh <code>null</code>
	 */
	public void setGrupTransaksi(GrupTransaksi grupTransaksi) {
		this.grupTransaksi = grupTransaksi;
	}

	/**
	 * Mengembalikan induk/header jurnal tempat baris ini bernaung (FK
	 * <code>grup_transaksi</code>).
	 *
	 * <p><b>Penting untuk cakupan tenant:</b> induk inilah satu-satunya pembawa
	 * <code>satuanKerja</code>/<code>workspace</code>. Entity baris tidak memiliki kolom
	 * organisasi apa pun, dan FK ini <code>nullable</code> — kueri terhadap
	 * <code>akunting.transaksi</code> yang tidak mem-<i>join</i> induk akan menjangkau
	 * seluruh instalasi, dan baris yatim tidak akan terjangkau filter mana pun.</p>
	 *
	 * <p>Diambil dengan <code>FetchMode.SELECT</code>. Berbeda dari relasi lain di kelas
	 * ini, nilai <b>tidak</b> dilewatkan
	 * {@link ais.database.model.GeneralValueObject#check(Object)}, sehingga pemanggil dapat
	 * menerima proxy Hibernate yang belum ter-inisialisasi bila sesi sudah tertutup.</p>
	 *
	 * @return induk/header jurnal, atau <code>null</code> untuk baris yatim
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "grup_transaksi", nullable = true)
	public GrupTransaksi getGrupTransaksi() {
		return grupTransaksi;
	}

	/**
	 * Menyetel penanda baris resmi versus baris draft.
	 *
	 * <p>Seluruh jalur penyimpanan resmi (layar jurnal umum/penerimaan/pengeluaran, unggah
	 * Excel, dan <code>CommonAkunting.saveTransaksi(...)</code>) memanggilnya dengan
	 * <code>true</code>. Nilai <code>false</code> adalah keadaan bawaan baris yang masih
	 * dalam penyusunan.</p>
	 *
	 * @param simpan <code>true</code> untuk baris resmi, <code>false</code> untuk draft
	 */
	public void setSimpan(Boolean simpan) {
		this.simpan = simpan;
	}

	/**
	 * Mengembalikan penanda baris resmi (<code>true</code>) versus baris draft
	 * (<code>false</code>).
	 *
	 * <p><b>PENTING — baris draft berbagi tabel dengan baris jurnal resmi.</b> Selama
	 * operator menyusun jurnal, baris disimpan ke <code>akunting.transaksi</code> dengan
	 * <code>simpan = false</code>. Penyapuannya baru terjadi ketika tombol
	 * "Simpan"/"Tutup" ditekan, lewat SQL mentah
	 * <code>delete from akunting.transaksi where simpan = false and parent_code = '…'</code>
	 * (pola pembatalan yang sama membuang anak lebih dahulu, lalu menghapus
	 * {@link #getGrupTransaksi() induk} bila tidak ada baris <code>simpan = true</code>
	 * yang tersisa). Bila sesi/peramban ditutup tanpa menekan tombol tersebut, baris draft
	 * <b>tertinggal permanen</b>.</p>
	 *
	 * <p>Yang membuat hal ini berdampak: hanya ketiga helper jurnal itu yang memfilter
	 * <code>simpan</code>. Kueri buku besar, neraca saldo, jurnal harian, riwayat
	 * transaksi, dan dasbor akunting <b>tidak</b> memfilter kolom ini sama sekali, sehingga
	 * baris setengah jadi ikut terhitung sebagai mutasi resmi.</p>
	 *
	 * @return <code>true</code> bila baris sudah dinyatakan resmi; bawaan <code>false</code>
	 */
	public Boolean getSimpan() {
		return simpan;
	}

	/**
	 * Mengembalikan nilai acak pembeda baris (kolom <code>random_value</code>).
	 *
	 * <p><b>Efek samping (getter penulis):</b> bila field masih <code>null</code>, method
	 * membangkitkan bilangan acak baru dengan {@link Random} tanpa benih tetap,
	 * menuliskannya ke field, dan <b>mencetaknya ke <code>System.out</code></b>. Jadi
	 * pembacaan pertama atas baris jurnal lama akan menerbitkan <code>UPDATE</code> sekaligus
	 * satu baris log per baris jurnal yang dirender — beban yang terasa pada laporan buku
	 * besar berukuran besar.</p>
	 *
	 * <p><b>Kuirk pemetaan:</b> properti bertipe {@link Long} ini dianotasi
	 * <code>&#64;JoinColumn</code> alih-alih <code>&#64;Column</code>. Anotasi relasi pada
	 * properti skalar bukan pemetaan yang sah, sehingga nama kolom yang diminta
	 * (<code>random_value</code>) belum tentu dipakai Hibernate. Pola yang persis sama ada
	 * di {@link ais.database.model.akunting.TemplateTransaksi#getRandomValue()}.</p>
	 *
	 * <p>Tidak ada pembaca nilai ini di luar kelas ini dan kembarannya.</p>
	 *
	 * @return nilai acak pembeda baris; tidak pernah <code>null</code> setelah pemanggilan
	 *         pertama
	 */
	@JoinColumn(name = "random_value", nullable = true)
	public Long getRandomValue() {
		if (randomValue == null) {
			Random randomGenerator = new Random();
			long fraction = (long) (Long.MAX_VALUE * randomGenerator.nextDouble());
			randomValue = fraction;
			System.out.println("randomValue = " + randomValue);
		}
		return randomValue;
	}

	/**
	 * Menyetel nilai acak pembeda baris.
	 *
	 * <p>Dipakai Hibernate saat memuat baris dari basis data; menyetelnya secara eksplisit
	 * dari kode aplikasi mencegah pembangkitan otomatis pada
	 * {@link #getRandomValue()}.</p>
	 *
	 * @param randomValue nilai acak pembeda baris
	 */
	public void setRandomValue(Long randomValue) {
		this.randomValue = randomValue;
	}

	/**
	 * Mengembalikan batch posting yang mengunci baris jurnal ini (FK
	 * <code>posting_history</code>).
	 *
	 * <p>Keberadaan objek ini menjadi satu-satunya penentu {@link #getStatusPosting()} dan
	 * {@link #getTanggalPosting()}. Perlu diingat bahwa {@link PostingHistory} sendiri
	 * memiliki bendera <code>posting</code> yang dapat bernilai <code>false</code> setelah
	 * posting dibatalkan — bendera itu <b>tidak</b> diperiksa oleh kedua getter tersebut.</p>
	 *
	 * <p>Diambil dengan <code>FetchMode.SELECT</code> dan tidak dilewatkan
	 * {@link ais.database.model.GeneralValueObject#check(Object)}.</p>
	 *
	 * @return batch posting, atau <code>null</code> bila baris belum diposting
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * Menautkan baris jurnal ini ke satu batch posting.
	 *
	 * <p>Pemanggilan setter ini <b>secara langsung mengubah</b> hasil
	 * {@link #getStatusPosting()} dan {@link #getTanggalPosting()} pada pembacaan
	 * berikutnya, termasuk berpotensi mengosongkan <code>tanggal_posting</code> bila
	 * disetel <code>null</code>.</p>
	 *
	 * @param postingHistory batch posting, atau <code>null</code> untuk melepas posting
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	/**
	 * Mengembalikan klasifikasi transaksi baris jurnal ini (FK
	 * <code>jenis_transaksi</code>).
	 *
	 * <p><b>Efek samping (getter penulis):</b> bila field masih <code>null</code> sedangkan
	 * {@link #getGrupTransaksi() induk} memiliki jenis transaksi, nilai induk
	 * <b>dituliskan</b> ke field baris ini — pewarisan senyap dari header ke anak yang
	 * dapat menerbitkan <code>UPDATE</code> saat baris dibaca di dalam sesi hidup. Hasil
	 * akhirnya lalu dilewatkan
	 * {@link ais.database.model.GeneralValueObject#check(Object)} untuk de-proxy.</p>
	 *
	 * <p>Perhatikan bahwa induk diakses lewat field <code>grupTransaksi</code> secara
	 * langsung, bukan lewat {@link #getGrupTransaksi()}, sehingga proxy induk yang belum
	 * ter-inisialisasi tetap dianggap "ada" dan akan dipaksa memuat diri.</p>
	 *
	 * @return klasifikasi transaksi baris ini (diwarisi dari induk bila kosong), atau
	 *         <code>null</code>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_transaksi", nullable = true)
	public JenisTransaksi getJenisTransaksi() {
		if (jenisTransaksi == null && grupTransaksi != null && grupTransaksi.getJenisTransaksi() != null) {
			jenisTransaksi = grupTransaksi.getJenisTransaksi();
		}
		jenisTransaksi = check(jenisTransaksi);
		return jenisTransaksi;
	}

	/**
	 * Menyetel klasifikasi transaksi baris jurnal ini.
	 *
	 * <p>Menyetel nilai bukan-<code>null</code> di sini mematikan pewarisan otomatis dari
	 * induk yang dijelaskan pada {@link #getJenisTransaksi()}.</p>
	 *
	 * @param jenisTransaksi klasifikasi transaksi, boleh <code>null</code>
	 */
	public void setJenisTransaksi(JenisTransaksi jenisTransaksi) {
		this.jenisTransaksi = jenisTransaksi;
	}

	/**
	 * Mengembalikan perkiraan pengganti hasil koreksi manual (FK <code>akun_over</code>).
	 *
	 * <p><b>PENTING:</b> selama nilainya bukan <code>null</code>, {@link #getAkun()} akan
	 * <b>menimpa</b> kolom <code>akun</code> dengan nilai ini pada setiap pembacaan —
	 * lihat peringatan lengkap di sana. Kolom ini karena itu bukan sekadar catatan
	 * pendamping, melainkan penentu efektif perkiraan baris jurnal.</p>
	 *
	 * <p>Nilai dilewatkan {@link ais.database.model.GeneralValueObject#check(Object)} untuk
	 * de-proxy.</p>
	 *
	 * @return perkiraan pengganti, atau <code>null</code> bila baris tidak pernah dikoreksi
	 * @see #getAkun()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_over", nullable = true)
	public Akun getAkunOver() {
		akunOver = check(akunOver);
		return akunOver;
	}

	/**
	 * Menyetel perkiraan pengganti hasil koreksi manual.
	 *
	 * <p>Satu-satunya pemanggil adalah dialog koreksi akun pada
	 * <code>TransaksiJurnalUmumHelper</code>, yang menyetel nilai ini lalu langsung
	 * melakukan <code>refreshUpdate</code> dan <code>flush()</code>. Pada <i>flush</i>
	 * itulah {@link #getAkun()} dipanggil Hibernate dan menimpa kolom <code>akun</code>,
	 * sehingga satu operasi menulis kedua kolom sekaligus dan menghapus jejak akun asli
	 * dari tabel utama.</p>
	 *
	 * <p>Menyetel <code>null</code> di sini adalah satu-satunya cara mengembalikan kendali
	 * kolom <code>akun</code> kepada {@link #setAkun(Akun)}.</p>
	 *
	 * @param akunOver perkiraan pengganti, atau <code>null</code> untuk mencabut koreksi
	 */
	public void setAkunOver(Akun akunOver) {
		this.akunOver = akunOver;
	}

}
