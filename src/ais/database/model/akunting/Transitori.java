package ais.database.model.akunting;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Entity <b>catatan transitori</b> &mdash; satu baris pengajuan pencairan dana yang
 * diselesaikan lewat <i>rekening perantara</i> (transitori), bukan lewat transfer bank
 * langsung ke penerima. Tabel <code>akunting.transitori</code>.
 *
 * <p>Dalam akuntansi, akun transitori adalah akun penampung sementara: dana keluar dari
 * rekening sumber, <b>mampir</b> di rekening perantara, lalu baru diteruskan ke tujuan
 * akhir. Di AIS, keputusan "lewat transitori atau transfer langsung" diambil per baris
 * pengajuan pencairan pada layar Proses Transfer, dan baris yang dipilih lewat transitori
 * melahirkan tepat satu instance kelas ini.</p>
 *
 * <h3>Posisi pada rantai pencairan dana (terverifikasi dari kode)</h3>
 * <p>Rantai lengkapnya adalah: dokumen sumber (uang muka, LPJ, penggantian kas kecil,
 * pembayaran gaji, pembayaran/DP/termin aset, &hellip;) &rarr;
 * {@link DaftarPengajuanTransfer} (baris DPC) &rarr; {@link ProsesTransfer} (batch
 * pencairan: disetujui lalu direalisasikan). Pada tahap realisasi, tiap baris DPC ditandai
 * salah satu dari dua jalur:</p>
 * <ul>
 *   <li><b>Transfer</b> &mdash; dana langsung ke rekening penerima; ditandai pada
 *       {@link DaftarPengajuanTransfer#getTransfer()}; tidak melahirkan baris di sini.</li>
 *   <li><b>Transitori</b> &mdash; dana mampir di rekening perantara; ditandai pada
 *       {@link DaftarPengajuanTransfer#getTransitori()} dan <b>melahirkan satu baris
 *       {@code Transitori}</b>, yang lalu ditaut balik ke pengajuannya lewat
 *       {@link DaftarPengajuanTransfer#getTransitoriData()}.</li>
 * </ul>
 * <p>Kedua penanda itu dijaga saling eksklusif oleh getter di
 * {@link DaftarPengajuanTransfer}, dengan "Transfer" sebagai pemenang.</p>
 *
 * <h3>Hubungan dengan {@link ProsesTransitori} &mdash; diverifikasi, bukan asumsi</h3>
 * <p>Baris ini <b>bukan</b> anak yang dilahirkan oleh {@link ProsesTransitori}. Urutannya
 * justru terbalik: baris <code>Transitori</code> lahir lebih dulu (dari layar Proses
 * Transfer, tanpa induk &mdash; kolom <code>proses_transitori</code> masih
 * <code>NULL</code>), lalu <b>dipungut</b> ke dalam sebuah batch
 * {@link ProsesTransitori} pada layar Proses Transitori. Bukti pada kode:</p>
 * <ul>
 *   <li>kolom FK <code>proses_transitori</code> dideklarasikan <code>nullable = true</code>
 *       ({@link #getProsesTransitori()}), sementara FK ke pengajuan
 *       (<code>daftar_pengajuan_transfer_id</code>) <code>nullable = false</code> dan
 *       <code>unique = true</code> &mdash; jadi induk yang <i>wajib</i> bagi baris ini
 *       adalah pengajuannya, bukan batch-nya;</li>
 *   <li>{@code ProsesTransitoriAction} membangun daftar kandidat dengan filter
 *       <code>prosesTransitori IS NULL</code>, dan pada simpan-nya menugaskan
 *       <code>transitori.setProsesTransitori(prosesTransitori)</code> untuk tiap baris
 *       yang dicentang &mdash; total nilai batch dihitung dari
 *       <code>daftarPengajuanTransfer.nominal</code> milik baris-baris tersebut;</li>
 *   <li>{@code TransitoriAction} memakai <code>prosesTransitori IS NULL</code> persis
 *       sebagai status "Belum" (belum dipungut batch mana pun).</li>
 * </ul>
 * <p>Jadi relasinya banyak-ke-satu yang <b>opsional dan menyusul</b>: N catatan transitori
 * yatim &rarr; 1 batch {@link ProsesTransitori}. Nominalnya sendiri tidak pernah disimpan
 * di kelas ini; semua angka dibaca dari pengajuan pasangannya.</p>
 *
 * <h3>Siklus hidup satu baris</h3>
 * <ol>
 *   <li><b>Lahir</b>: centang "Transitori" pada baris DPC di
 *       {@code ProsesTransferAction} &rarr; <code>new Transitori()</code> dengan
 *       {@link #setDaftarPengajuanTransfer(DaftarPengajuanTransfer)},
 *       {@link #setNama(String)} dan {@link #setKode(String)} disalin dari pengajuannya.
 *       <b>Melepas centang menghapus barisnya secara permanen</b>
 *       (<code>session.delete</code>), bukan menonaktifkannya.</li>
 *   <li><b>Dipantau</b>: layar "Daftar Transitori" ({@code TransitoriAction}) menampilkan
 *       bank/rekening sumber, nominal pengajuan, status, dan keterangan yang bisa disunting
 *       langsung di grid.</li>
 *   <li><b>Dipungut</b>: {@code ProsesTransitoriAction} mengelompokkan beberapa baris ke
 *       satu {@link ProsesTransitori} &mdash; {@link #setProsesTransitori(ProsesTransitori)}
 *       terisi, status berubah menjadi "Diajukan".</li>
 *   <li><b>Disetujui</b>: batch-nya distempel <code>disetujuiOleh</code>. Inilah gerbang
 *       sesungguhnya sebelum penjurnalan.</li>
 *   <li><b>Dijurnal</b>: {@code PostingProsesTransitoriAction} memposting baris yang
 *       batch-nya sudah disetujui, nominal pengajuannya bukan nol/null, dan
 *       <code>transfer = true</code>. Debet = akun pengajuan
 *       ({@code daftarPengajuanTransfer.akun}); kredit = akun transitori milik cara
 *       pembayaran transfer batch-nya
 *       ({@code prosesTransfer.caraPembayaranTransfer.akunTransitori}); posisi debet/kredit
 *       ditukar bila nominal &le; 0,1. Tanggal jurnal = tanggal persetujuan batch. Cap
 *       posting disimpan di {@link #setPostingHistory(PostingHistory)}.</li>
 *   <li><b>Batal posting</b>: jurnal dihapus lewat SQL langsung dengan syarat
 *       <code>closing is null</code>, lalu cap posting dikosongkan.</li>
 * </ol>
 *
 * <h3>Pengelompokan member</h3>
 * <ul>
 *   <li><b>Identitas &amp; label</b>: {@link #getId()}, {@link #getKode()},
 *       {@link #getNama()}, {@link #getKeterangan()}, {@link #toString()}.</li>
 *   <li><b>Jejak audit</b>: {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()}, dan hook {@link #onUpdate()}.</li>
 *   <li><b>Relasi</b>: {@link #getDaftarPengajuanTransfer()} (wajib, satu-ke-satu),
 *       {@link #getProsesTransitori()} (opsional, induk batch),
 *       {@link #getPostingHistory()} (cap posting jurnal).</li>
 *   <li><b>Penanda status</b>: {@link #getTransfer()}, {@link #getAktif()}.</li>
 * </ul>
 *
 * <h3>Hal yang tidak terlihat dari deklarasi</h3>
 * <ul>
 *   <li><b>Field yang dideklarasikan ulang bukan bug.</b> {@link GeneralValueObject} adalah
 *       POJO abstrak biasa &mdash; bukan <code>@Entity</code> maupun
 *       <code>@MappedSuperclass</code> &mdash; sehingga Hibernate tidak memetakan properti
 *       induknya. Menyatakan ulang <code>id</code>, <code>nama</code>,
 *       <code>keterangan</code>, <code>oleh</code>, <code>olehId</code>, dan
 *       <code>tanggal_dirubah</code> di sini adalah <b>keharusan teknis</b> agar kolomnya
 *       ada di tabel ini.</li>
 *   <li><b>{@link #getTransfer()} destruktif.</b> Getter ini menugaskan
 *       <code>transfer = true</code> tanpa syarat (logika aslinya tertinggal sebagai
 *       komentar) dan tidak ada satu pun pemanggil {@link #setTransfer(Boolean)} di seluruh
 *       repositori. Karena akses properti Hibernate membaca lewat getter saat
 *       <i>dirty-check</i>, nilai itu ikut tertulis permanen ke basis data. Lihat catatan
 *       lengkapnya pada {@link #getTransfer()}.</li>
 *   <li><b>{@link #getAktif()} satu arah.</b> Getter memadamkan baris ini bila pengajuannya
 *       sudah tidak aktif, dan tidak pernah menyalakannya kembali &mdash; juga tertulis
 *       permanen lewat mekanisme yang sama.</li>
 *   <li><b>Tidak ada kolom tenant.</b> Entity ini tidak menyimpan satuan kerja, sekolah,
 *       maupun yayasan; satu-satunya konteks unit datang dari
 *       {@code daftarPengajuanTransfer.satuanKerja}. Layar pemantauan tidak menyaring apa
 *       pun berdasarkan konteks itu.</li>
 *   <li><b>Ketidakcocokan <code>@NotFound</code> vs <code>nullable</code>.</b> Relasi
 *       pengajuan ditandai <code>nullable = false</code> (DDL NOT NULL) sekaligus
 *       <code>@NotFound(IGNORE)</code>. Bila baris pengajuan dihapus dari luar, getter
 *       mengembalikan <code>null</code> walau kolomnya seharusnya wajib &mdash; itulah
 *       sebabnya renderer layar punya cabang khusus "Data pengajuan asal tidak
 *       ditemukan".</li>
 *   <li><b>Tanpa <code>kodeUnik</code>.</b> Berbeda dari {@link ProsesTransitori}, kelas ini
 *       tidak punya kolom kunci turunan; kunci alaminya (menurut adapter CRUD generik)
 *       adalah kombinasi <code>daftarPengajuanTransfer</code> + <code>kode</code>, dan
 *       kunci dedup penjurnalan dibentuk {@code GrupTransaksi} sebagai
 *       <code>ais.database.model.akunting.Transitori_&lt;id&gt;</code>.</li>
 *   <li><b>Hanya-baca di CRUD generik.</b> {@code TransitoriWorkflowGenericCrudAdapter}
 *       mematikan seluruh mutasi (create/update/delete/impor) untuk entity ini; satu-satunya
 *       jalur tulis yang sah adalah layar Proses Transfer, layar Daftar Transitori (kolom
 *       keterangan), layar Proses Transitori, dan mesin posting.</li>
 * </ul>
 *
 * <h3>Catatan otorisasi pada pemanggilnya</h3>
 * <ul>
 *   <li>{@code TransitoriAction} hanya memanggil {@code Common.doCheckSecurity()} (gerbang
 *       sesi/login) dan <b>tidak memakai {@code CommonPrivilages.checkPrevilages(...)} sama
 *       sekali</b>, padahal layarnya menulis ke basis data: penyuntingan
 *       {@link #setKeterangan(String)} inline di grid dan penambalan tautan balik
 *       {@code daftarPengajuanTransfer.transitoriData}. Bandingkan dengan
 *       {@code ProsesTransitoriAction} dan {@code PostingProsesTransitoriAction} yang
 *       memeriksa CREATE/UPDATE/DELETE/APPROVE.</li>
 *   <li>{@code ProsesTransitoriApiHelper.bolehAksi()} bersifat <i>fail-open</i>: pengguna
 *       yang <code>hakAkses()</code>-nya <code>null</code> diberi izin PENUH
 *       (create/update/delete/<b>approve</b>/reject) atas batch {@link ProsesTransitori}
 *       &mdash; padahal persetujuan batch itulah gerbang terakhir sebelum jurnal terbentuk.
 *       Pola yang sama sudah terkonfirmasi di banyak helper API modul akunting lain.</li>
 * </ul>
 *
 * <p>Seluruh perubahan direkam Hibernate Envers ({@code @Audited}), dan
 * <code>dynamicInsert</code>/<code>dynamicUpdate</code> membuat pernyataan SQL hanya memuat
 * kolom yang benar-benar berubah.</p>
 *
 * @see ProsesTransitori
 * @see DaftarPengajuanTransfer
 * @see ProsesTransfer
 * @see PostingHistory
 * @see CaraPembayaranTransfer
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "akunting", name = "transitori")
public class Transitori extends GeneralValueObject {

	/**
	 * Versi serialisasi entity ini.
	 *
	 * <p>Nilainya <b>identik</b> dengan milik {@link ProsesTransitori} &mdash; jejak bahwa
	 * kedua kelas lahir dari satu templat generator yang sama pada Apr 2010, bukan tanda
	 * keduanya sekelas. Jangan diubah: entity AIS diserialkan ke cache dan session, sehingga
	 * nilai baru membuat data ter-cache lama tidak terbaca setelah deploy.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama (identity/auto-increment), dipetakan ke kolom <code>id</code>. */
	private Long id;

	/** Nama pengguna yang terakhir mengubah baris ini; diisi oleh {@link #onUpdate()}. */
	private String oleh;

	/** Id pengguna yang terakhir mengubah baris ini; diisi oleh {@link #onUpdate()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna pengubah terakhir.
	 *
	 * @return id pengguna, atau <code>null</code> bila baris belum pernah diubah lewat jalur
	 *         yang mengenali identitas pengguna
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Menolak nilai kosong secara diam-diam:</b> argumen <code>null</code> atau berisi
	 * spasi saja langsung diabaikan (method keluar tanpa error), sehingga jejak audit yang
	 * sudah ada tidak dapat dihapus atau dikosongkan lewat setter ini.</p>
	 *
	 * @param olehId id pengguna pengubah; nilai <code>null</code>/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: argumen <code>null</code> atau berisi spasi
	 * saja diabaikan tanpa error.</p>
	 *
	 * @param oleh nama pengguna pengubah; nilai <code>null</code>/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna pengubah terakhir.
	 *
	 * <p>Kosong pada baris yang baru dibuat: tidak ada hook <code>@PrePersist</code> di kelas
	 * ini, sehingga pembuat baris tidak terstempel &mdash; kolomnya baru terisi pada
	 * perubahan berikutnya.</p>
	 *
	 * @return nama pengguna pengubah terakhir, atau <code>null</code>
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA <code>@PreUpdate</code>: menstempel jejak audit tepat sebelum UPDATE
	 * dieksekusi.
	 *
	 * <p>Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)}, yang mengisi
	 * {@link #setOleh(String)}, {@link #setOlehId(String)}, dan
	 * {@link #setTanggal_dirubah(Date)} dari identitas pengguna sesi berjalan. Dipanggil oleh
	 * <b>provider JPA</b>, bukan oleh kode aplikasi &mdash; jangan memanggilnya langsung.</p>
	 *
	 * <p><b>Hanya berlaku untuk UPDATE.</b> Bila penulisnya bukan request pengguna (misalnya
	 * mesin posting massal dari jalur REST), interceptor bisa tidak menemukan identitas, dan
	 * karena setter menolak nilai kosong, jejak lama bertahan apa adanya.</p>
	 *
	 * <p>Pada baris deklarasi yang sama juga dideklarasikan field
	 * <code>tanggal_dirubah</code> &mdash; diinisialisasi ke waktu sekarang
	 * ({@code WaktuUtil.getDate()}) saat object dibuat, sehingga instance baru selalu punya
	 * timestamp meski belum pernah disimpan. Susunan satu baris ini adalah gaya generator,
	 * bukan sesuatu yang bermakna.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir.
	 *
	 * <p>Umumnya tidak dipanggil kode aplikasi: nilainya diisi {@link #onUpdate()} lewat
	 * interceptor audit. Setter ini menerima <code>null</code> tanpa penjagaan.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir (presisi TIMESTAMP).
	 *
	 * @return waktu perubahan terakhir; tidak pernah <code>null</code> pada instance yang
	 *         dibuat lewat konstruktor, karena field-nya diinisialisasi ke waktu sekarang
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat: <code>id</code>, tanda hubung, lalu <code>nama</code>.
	 *
	 * <p>Membaca <b>field</b> <code>nama</code> secara langsung, bukan lewat
	 * {@link #getNama()}, sehingga tidak ikut melakukan <code>trim()</code>. Nilai
	 * <code>null</code> pada kedua bagian tampil apa adanya sebagai teks
	 * <code>"null"</code> &mdash; jangan dipakai sebagai label di UI.</p>
	 *
	 * @return gabungan <code>id + "-" + nama</code>
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Kode dokumen, disalin dari kode {@link DaftarPengajuanTransfer} saat baris ini lahir.
	 * Bagian dari kunci alami menurut adapter CRUD generik.
	 */
	private String kode;

	/**
	 * Penanda "dana sudah keluar dari rekening transitori". Lihat peringatan pada
	 * {@link #getTransfer()}: kolom ini hanya pernah ditulis oleh getter-nya sendiri.
	 */
	private Boolean transfer;

	/** Nama dokumen, disalin dari nama {@link DaftarPengajuanTransfer} saat baris ini lahir. */
	private String nama;

	/** Catatan bebas; satu-satunya field yang boleh disunting langsung dari layar pemantauan. */
	private String keterangan;

	/** Batch pengelompokan (opsional, menyusul). Lihat {@link #getProsesTransitori()}. */
	private ProsesTransitori prosesTransitori;

	/** Baris pengajuan pencairan yang menjadi asal-usul baris ini (wajib, satu-ke-satu). */
	private DaftarPengajuanTransfer daftarPengajuanTransfer;

	/** Penanda aktif. Lihat {@link #getAktif()} untuk perilaku pewarisan satu arahnya. */
	private Boolean aktif;

	/** Cap posting jurnal; terisi hanya bila jurnalnya benar-benar tersimpan. */
	private PostingHistory postingHistory;

	/**
	 * Konstruktor tanpa argumen yang dibutuhkan Hibernate.
	 *
	 * <p>Instance baru belum punya pengajuan asal; pemanggil <b>wajib</b> mengisi
	 * {@link #setDaftarPengajuanTransfer(DaftarPengajuanTransfer)} sebelum menyimpan, karena
	 * kolomnya NOT NULL.</p>
	 */
	public Transitori() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Dihasilkan basis data (strategi IDENTITY) dan tidak ikut disertakan pada INSERT
	 * (<code>insertable = false</code>). Bernilai <code>null</code> sebelum baris tersimpan.
	 * Id inilah yang dipakai {@code GrupTransaksi} untuk membentuk kunci dedup penjurnalan
	 * dan dipakai mesin posting sebagai proyeksi daftar kerja.</p>
	 *
	 * @return id baris, atau <code>null</code> bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Hanya untuk keperluan Hibernate/penyalinan objek; jangan dipakai
	 * mengubah identitas baris yang sudah tersimpan.
	 *
	 * @param id nilai kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode dokumen apa adanya (tanpa <code>trim()</code>).
	 *
	 * <p>Diisi sekali saat baris lahir, dari kode pengajuan pasangannya, dan tidak pernah
	 * disinkronkan ulang bila kode pengajuan berubah kemudian. Dipakai layar antarmuka baru
	 * sebagai kode cadangan ketika pengajuan asalnya tidak ditemukan.</p>
	 *
	 * @return kode dokumen, atau <code>null</code>
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Menyetel kode dokumen.
	 *
	 * @param kode kode dokumen; boleh <code>null</code>
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama dokumen yang sudah dipangkas spasi tepinya.
	 *
	 * <p>Kolomnya NOT NULL sepanjang 255 karakter, namun getter tetap menjaga
	 * <code>null</code> agar tidak memicu NPE pada instance yang belum tersimpan. Nilai ini
	 * disalin dari nama pengajuan saat baris lahir dan ikut masuk ke keterangan jurnal saat
	 * posting ("Pengajuan transitori &quot;&hellip;&quot; senilai &hellip;").</p>
	 *
	 * @return nama dokumen yang sudah di-<code>trim()</code>, atau <code>null</code>
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama dokumen. Nilai disimpan apa adanya; pemangkasan baru terjadi saat dibaca.
	 *
	 * @param nama nama dokumen
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan catatan bebas baris ini.
	 *
	 * @return keterangan, atau <code>null</code>
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel catatan bebas.
	 *
	 * <p><b>Titik tulis yang aktif dipakai:</b> kolom "Keterangan" pada grid layar Daftar
	 * Transitori menyimpan nilai baru langsung ke basis data pada event
	 * <code>onChange</code> (tanpa dialog konfirmasi dan tanpa pemeriksaan hak akses per
	 * aksi), begitu pula endpoint pembaruan catatan pada layanan antarmuka baru.</p>
	 *
	 * @param keterangan catatan bebas; boleh <code>null</code>
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan batch {@link ProsesTransitori} yang memungut baris ini, bila sudah ada.
	 *
	 * <p><b>Opsional dan menyusul.</b> Bernilai <code>null</code> selama baris masih "yatim"
	 * &mdash; dan justru kondisi <code>null</code> itulah yang membuatnya muncul sebagai
	 * kandidat pada layar Proses Transitori serta berstatus "Belum" pada layar pemantauan.
	 * Nilainya diisi sekali saat batch disimpan; tidak ada jalur UI yang melepasnya
	 * kembali.</p>
	 *
	 * <p><code>@NotFound(IGNORE)</code> membuat referensi yang menunjuk baris batch yang sudah
	 * terhapus dibaca sebagai <code>null</code> alih-alih melempar
	 * <code>ObjectNotFoundException</code> &mdash; efeknya baris tersebut "kembali" menjadi
	 * kandidat batch baru walaupun mungkin sudah pernah dijurnal (cek
	 * {@link #getPostingHistory()} sebelum mengambil kesimpulan).</p>
	 *
	 * <p>Persetujuan batch inilah gerbang penjurnalan: mesin posting menuntut
	 * <code>prosesTransitori.disetujuiOleh</code> tidak <code>null</code>, dan memakai
	 * <code>prosesTransitori.tanggalPersetujuan</code> sebagai tanggal jurnal.</p>
	 *
	 * @return batch pengelompokan, atau <code>null</code> bila baris belum dipungut batch
	 *         mana pun (atau batch-nya sudah terhapus)
	 */
	@NotFound(action = NotFoundAction.IGNORE)
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "proses_transitori", nullable = true)
	public ProsesTransitori getProsesTransitori() {
		return prosesTransitori;
	}

	/**
	 * Menautkan baris ini ke sebuah batch {@link ProsesTransitori}.
	 *
	 * <p>Dipanggil saat batch disimpan, untuk setiap baris yang dicentang operator. Karena
	 * <code>cascade = {PERSIST, MERGE}</code>, batch yang belum tersimpan ikut tersimpan
	 * bersama baris ini.</p>
	 *
	 * @param prosesTransitori batch pengelompokan; boleh <code>null</code> untuk melepas
	 *                         tautan
	 */
	public void setProsesTransitori(ProsesTransitori prosesTransitori) {
		this.prosesTransitori = prosesTransitori;
	}

	/**
	 * Mengembalikan baris pengajuan pencairan yang menjadi asal-usul baris ini.
	 *
	 * <p><b>Relasi wajib dan satu-ke-satu:</b> kolomnya dideklarasikan
	 * <code>nullable = false</code> sekaligus <code>unique = true</code>, jadi satu pengajuan
	 * paling banyak melahirkan satu catatan transitori. Seluruh angka yang ditampilkan dan
	 * dijurnal untuk baris ini &mdash; nominal, akun debet, bank/rekening/atas nama sumber,
	 * waktu pengajuan, disposisi SOP, satuan kerja &mdash; dibaca dari sini, karena entity
	 * ini sendiri tidak menyimpan satu pun di antaranya.</p>
	 *
	 * <p><b>Bisa mengembalikan <code>null</code> meski kolomnya NOT NULL.</b>
	 * <code>@NotFound(IGNORE)</code> menyembunyikan referensi yang menggantung (baris
	 * pengajuan terhapus di luar alur normal) dengan mengembalikan <code>null</code>. Semua
	 * pemanggil harus siap menghadapinya: layar pemantauan punya cabang khusus yang
	 * menampilkan "Data pengajuan asal tidak ditemukan" dengan nominal 0, dan mesin posting
	 * melewati baris seperti itu tanpa menjurnalnya.</p>
	 *
	 * @return pengajuan pencairan pasangannya, atau <code>null</code> bila referensinya
	 *         menggantung
	 */
	@NotFound(action = NotFoundAction.IGNORE)
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "daftar_pengajuan_transfer_id", nullable = false, unique = true)
	public DaftarPengajuanTransfer getDaftarPengajuanTransfer() {
		return daftarPengajuanTransfer;
	}

	/**
	 * Menautkan baris ini ke pengajuan pencairan asalnya.
	 *
	 * <p>Dipanggil satu kali saat baris lahir. Tautan baliknya
	 * ({@code daftarPengajuanTransfer.transitoriData}) diisi terpisah oleh pemanggil, dan
	 * bila terlewat akan ditambal belakangan oleh layar pemantauan atau oleh mesin posting
	 * &mdash; tanpa tautan itu, pembatalan posting dan penelusuran jurnal kehilangan jejak
	 * pasangannya.</p>
	 *
	 * @param daftarPengajuanTransfer pengajuan pencairan asal; wajib terisi sebelum baris
	 *                                disimpan
	 */
	public void setDaftarPengajuanTransfer(DaftarPengajuanTransfer daftarPengajuanTransfer) {
		this.daftarPengajuanTransfer = daftarPengajuanTransfer;
	}

	/**
	 * Mengembalikan penanda "dana sudah keluar dari rekening transitori" &mdash;
	 * <b>selalu {@code true}</b>.
	 *
	 * <p><b>PERINGATAN: getter ini menulis, bukan sekadar membaca.</b> Baris pertamanya
	 * menugaskan <code>transfer = true</code> tanpa syarat apa pun; implementasi aslinya
	 * (<code>return transfer == null ? false : transfer;</code>) masih tertinggal sebagai
	 * komentar di bawahnya. Karena pemetaan Hibernate kelas ini memakai <i>property
	 * access</i> (anotasi berada di getter), nilai itu juga yang dibaca Hibernate saat
	 * <i>dirty-check</i> menjelang <code>flush()</code> &mdash; jadi kolom
	 * <code>transfer</code> di basis data ikut ditulis <code>true</code> secara permanen pada
	 * setiap sesi yang memuat baris ini lalu melakukan flush. Termasuk sesi yang
	 * niatnya hanya membaca: merender grid layar pemantauan sudah cukup.</p>
	 *
	 * <p><b>Akibat yang terverifikasi:</b></p>
	 * <ul>
	 *   <li>{@link #setTransfer(Boolean)} <b>tidak punya satu pun pemanggil</b> di seluruh
	 *       repositori &mdash; tidak ada jalur sah mana pun yang pernah menyalakan atau
	 *       memadamkan penanda ini. Nilai <code>true</code> di basis data seluruhnya berasal
	 *       dari getter ini.</li>
	 *   <li>Kolom status pada layar pemantauan tidak pernah menampilkan "Diajukan": begitu
	 *       {@link #getProsesTransitori()} terisi, statusnya langsung terbaca "Sudah
	 *       Transfer". Filter "sudah diajukan belum transfer"
	 *       (<code>prosesTransitori IS NOT NULL AND transfer = false</code>) karenanya
	 *       permanen kosong.</li>
	 *   <li>Mesin posting jurnal menyaring kandidat dengan <code>transfer = true</code>.
	 *       Saringan yang seharusnya berarti "dana benar-benar sudah keluar dari rekening
	 *       transitori" itu praktis selalu terpenuhi, sehingga <b>bukan gerbang yang
	 *       sesungguhnya</b>; gerbang yang benar-benar bekerja adalah persetujuan batch
	 *       ({@code prosesTransitori.disetujuiOleh}).</li>
	 *   <li>Layanan antarmuka baru sengaja tidak memakai getter ini dan membaca kolom
	 *       <code>transfer</code> lewat SQL mentah untuk mendapatkan status yang jujur
	 *       &mdash; workaround yang sudah tercatat di kodenya.</li>
	 * </ul>
	 *
	 * <p>Jangan "membetulkan" method ini tanpa menelusuri seluruh rantai lebih dulu: data
	 * yang sudah terlanjur ditulis <code>true</code> tidak bisa dibedakan lagi dari yang
	 * memang sudah ditransfer, sehingga mengaktifkan kembali logika aslinya akan
	 * menyembunyikan kandidat posting yang sah maupun yang tidak sah secara serentak.</p>
	 *
	 * @return selalu {@link Boolean#TRUE}; tidak pernah <code>null</code> dan tidak pernah
	 *         <code>false</code>
	 * @see #setTransfer(Boolean)
	 */
	public Boolean getTransfer() {
		transfer = true;
		return transfer;
//		return transfer == null ? false : transfer;
	}

	/**
	 * Menyetel penanda "dana sudah keluar dari rekening transitori".
	 *
	 * <p><b>Tidak ada pemanggilnya</b> di seluruh repositori, dan nilai apa pun yang disetel
	 * di sini akan ditimpa {@code true} oleh {@link #getTransfer()} pada pembacaan
	 * berikutnya &mdash; termasuk pembacaan yang dilakukan Hibernate sendiri saat
	 * <code>flush()</code>. Setter ini efektif mati.</p>
	 *
	 * @param transfer penanda yang diinginkan; tidak berpengaruh dalam praktik
	 * @see #getTransfer()
	 */
	public void setTransfer(Boolean transfer) {
		this.transfer = transfer;
	}

	/**
	 * Mengembalikan status aktif baris ini, dengan pewarisan <b>satu arah</b> dari pengajuan
	 * asalnya.
	 *
	 * <p>Bila {@link #getDaftarPengajuanTransfer()} ada dan pengajuannya sudah tidak aktif,
	 * getter ini <b>menugaskan</b> <code>aktif = false</code> pada field &mdash; bukan sekadar
	 * mengembalikan nilai itu. Sama seperti {@link #getTransfer()}, penugasan di dalam getter
	 * properti Hibernate berarti nilainya ikut tertulis permanen ke basis data pada
	 * <code>flush()</code> berikutnya. Tidak ada cabang yang pernah menyalakannya kembali:
	 * mengaktifkan ulang pengajuannya <b>tidak</b> mengaktifkan ulang baris transitori ini,
	 * dan satu-satunya jalan pulih adalah menyetelnya lewat
	 * {@link #setAktif(Boolean)}.</p>
	 *
	 * <p>Pemeriksaan membaca <b>field</b> <code>daftarPengajuanTransfer</code> secara
	 * langsung, bukan lewat getter-nya; pada instance yang relasinya belum dimuat, cabang ini
	 * terlewat begitu saja dan hasilnya menjadi "aktif".</p>
	 *
	 * <p><code>null</code> diperlakukan sebagai <b>aktif</b>, sehingga baris lama yang
	 * kolomnya belum pernah diisi tetap tampil pada filter "hanya yang aktif" (filter itu
	 * memang ditulis sebagai <code>aktif IS NULL OR aktif = true</code>).</p>
	 *
	 * <p><b>Perhatikan:</b> penanda ini tidak dipakai mesin posting jurnal. Baris yang sudah
	 * dipadamkan tetap ikut diposting selama batch-nya disetujui dan nominal pengajuannya
	 * bukan nol.</p>
	 *
	 * @return {@code true} bila baris dianggap aktif (termasuk saat kolomnya
	 *         <code>null</code>); {@code false} bila dipadamkan sendiri atau diwarisi dari
	 *         pengajuan yang sudah tidak aktif
	 */
	public Boolean getAktif() {
		if (daftarPengajuanTransfer != null && !daftarPengajuanTransfer.getAktif()) {
			aktif = false;
		}
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif baris ini secara manual.
	 *
	 * <p>Ingat bahwa {@link #getAktif()} akan memadamkannya kembali pada pembacaan berikutnya
	 * selama pengajuan asalnya masih berstatus tidak aktif.</p>
	 *
	 * @param aktif status aktif; <code>null</code> berarti "aktif" saat dibaca
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan cap posting jurnal baris ini.
	 *
	 * <p>Berfungsi sebagai penanda idempotensi penjurnalan: mesin posting hanya memproses
	 * baris dengan cap <code>null</code>, dan pembatalan posting hanya memproses baris yang
	 * capnya tidak <code>null</code>. Satu instance {@link PostingHistory} dipakai bersama
	 * oleh seluruh baris dalam satu kali eksekusi posting massal, dengan jenis
	 * {@code PostingHistory.PENGAJUAN_TRANSITORI}.</p>
	 *
	 * <p>Capnya baru dipasang setelah jurnalnya <b>benar-benar</b> tersimpan; bila
	 * penyimpanan jurnal gagal, capnya sengaja dibiarkan kosong agar baris tersebut bisa
	 * dicoba ulang.</p>
	 *
	 * <p>Berbeda dari dua relasi lain di kelas ini, relasi ini <b>tidak</b> memakai
	 * <code>@NotFound(IGNORE)</code>: menghapus baris riwayat posting dari luar akan membuat
	 * pemuatan baris ini melempar <code>ObjectNotFoundException</code>, bukan diam-diam
	 * membuatnya tampak belum pernah diposting.</p>
	 *
	 * @return cap posting, atau <code>null</code> bila baris ini belum pernah dijurnal (atau
	 *         posting-nya sudah dibatalkan)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * Memasang atau melepas cap posting jurnal.
	 *
	 * <p>Dipanggil mesin posting dengan cap batch setelah jurnal tersimpan, dan dipanggil
	 * lagi dengan <code>null</code> oleh pembatalan posting setelah baris
	 * <code>akunting.grup_transaksi</code>/<code>akunting.transaksi</code> pasangannya
	 * dihapus. Pembatalan itu hanya menghapus jurnal yang <b>belum tutup buku</b>
	 * (<code>closing is null</code>), sementara cap di sini dikosongkan tanpa syarat &mdash;
	 * pada baris yang jurnalnya sudah terkunci closing, pengosongan cap membuka peluang
	 * baris yang sama diposting sekali lagi dan menghasilkan jurnal ganda.</p>
	 *
	 * @param postingHistory cap posting, atau <code>null</code> untuk melepasnya
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

}
