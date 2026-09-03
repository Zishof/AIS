package ais.database.model.sekolah;

import ais.action.master.helper.KegiatanPersistenceHelper;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.List;
import java.util.Map;

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

import org.hibernate.Session;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.PostingHistory;
import ais.ui.util.WaktuUtil;

/**
 * Baris rincian pada sebuah kuitansi pembayaran siswa — satu baris untuk satu item biaya yang
 * dilunasi.
 *
 * <p>Entity ini adalah <b>simpul tengah rantai billing sekolah</b>. Ia menjawab pertanyaan
 * "uang yang masuk lewat kuitansi ini, dialokasikan ke tagihan yang mana, sebesar berapa":</p>
 *
 * <pre>
 *   PembayaranSiswa          (kepala kuitansi: siapa membayar, kapan, lewat akun apa)
 *          |
 *          |  1..n
 *          v
 *   PembayaranSiswaDetail    &lt;-- KELAS INI (satu baris = satu item biaya yang dilunasi)
 *          |
 *          |  1..1
 *          v
 *   Tagihan                  (kewajiban yang dilunasi baris ini)
 * </pre>
 *
 * <p>Kaitannya ke {@code ais.database.model.sekolah.PembayaranSiswa} dan
 * {@code ais.database.model.sekolah.Tagihan} sudah <b>diverifikasi dari kode</b>, bukan
 * disimpulkan dari penamaan:</p>
 * <ul>
 *   <li>Ke atas, {@link #getPembayaranSiswa()} memetakan kolom {@code pembayaran_siswa_id}
 *   yang berstatus {@code nullable = false} — setiap baris rincian <b>wajib</b> bernaung pada
 *   satu kuitansi. Perhatikan bahwa {@link PembayaranSiswa} sendiri <b>tidak</b> memiliki
 *   koleksi {@code @OneToMany} balik ke sini: setiap kali sebuah kuitansi perlu diuraikan,
 *   baris-barisnya dikumpulkan lewat query eksplisit atas kelas ini (lihat
 *   {@code PembayaranSiswaUtil.dataPembayaran} untuk pencetakan struk,
 *   {@code PembayaranOnline} untuk layar riwayat, dan {@code PostingCicilanSiswaAction} untuk
 *   mesin posting jurnal).</li>
 *   <li>Ke bawah, {@link #getTagihan()} memetakan kolom {@code tagihan} yang boleh
 *   {@code null} — baris rincian bisa berdiri tanpa tagihan tertentu, dan
 *   {@link #updateTagihan(Tagihan, Integer, Double, Session)} akan mencarikan atau
 *   membuatkannya.</li>
 * </ul>
 *
 * <h2>Relasi dua kolom FK (kuirk paling penting)</h2>
 * <p>Pasangan tagihan&harr;pelunasan <b>tidak</b> dipetakan sebagai satu relasi dua arah biasa.
 * Tidak ada {@code mappedBy} di mana pun; yang ada adalah <b>dua kolom FK yang berdiri
 * sendiri dan saling menunjuk</b>:</p>
 * <ul>
 *   <li>{@code sekolah.pembayaran_siswa_detail.tagihan} &rarr; {@code tagihan.id} (dipetakan
 *   di kelas ini)</li>
 *   <li>{@code sekolah.tagihan.pembayaran_siswa_detail_id} &rarr;
 *   {@code pembayaran_siswa_detail.id} (dipetakan di {@link Tagihan}, dengan
 *   {@code unique = true})</li>
 * </ul>
 * <p>Kolom kedua itulah yang menjadi <b>penanda LUNAS</b> di seluruh aplikasi: hampir semua
 * layar dan laporan menentukan status tagihan dengan menguji
 * {@code pembayaranSiswaDetail IS NULL} / {@code IS NOT NULL}, bukan dengan membandingkan
 * jumlah terbayar.</p>
 *
 * <p>Karena keduanya kolom terpisah, keduanya <b>bisa tidak sinkron</b>. Untuk menambalnya,
 * kedua entity dilengkapi <b>getter yang saling memperbaiki</b>: {@link #getTagihan()} di sini
 * memanggil {@code tagihan.setPembayaranSiswaDetail(this)}, sementara
 * {@code Tagihan.getPembayaranSiswaDetail()} memanggil balik
 * {@code pembayaranSiswaDetail.setTagihan(this)}. Konsekuensinya harus dipahami siapa pun yang
 * menyentuh berkas ini:</p>
 * <ul>
 *   <li>Sekadar <b>membaca</b> baris ini dapat menghidupkan kembali status LUNAS pada tagihan
 *   yang sisi baliknya sudah sengaja dikosongkan.</li>
 *   <li>Perbaikan itu tidak simetris dengan penghapusan. Ketika baris rincian dihapus (mis.
 *   pembayaran dibatalkan/direvisi), {@code tagihan.pembayaran_siswa_detail_id} <b>tidak</b>
 *   ikut dibersihkan — meninggalkan FK menggantung yang di produksi muncul sebagai
 *   {@code ConstraintViolationException} pada job sinkronisasi tagihan. Penanganan defensifnya
 *   (verifikasi keberadaan baris lewat SQL native sebelum menulis) ada di
 *   {@code TagihanUtil}.</li>
 *   <li>Contoh perpindahan pelunasan antar tagihan yang <b>benar</b> (mengurus kedua sisi
 *   sekaligus, dalam satu transaksi, plus pemeriksaan pemilik dan cakupan) ada di
 *   {@code NewUiTagihanService.movePayment}.</li>
 * </ul>
 *
 * <h2>Getter yang menulis</h2>
 * <p>Ini <b>bukan</b> POJO pasif. Empat getter mengubah state saat dipanggil, dan karena
 * entity ini dipetakan dengan <b>akses properti</b> (anotasi ada di getter, bukan di field),
 * getter-getter itulah yang dipanggil Hibernate saat menentukan apa yang perlu ditulis ke
 * basis data. Digabung dengan {@code dynamicUpdate}, perubahan yang terjadi "hanya karena
 * dibaca" bisa ikut ter-flush tanpa satu pun baris kode yang secara sengaja menyimpannya:</p>
 * <ul>
 *   <li>{@link #getNominal()} — <b>menimpa rupiah yang tercatat</b> dengan hasil hitung ulang
 *   dari tagihan/tarif hari ini. Yang paling berdampak dari keempatnya.</li>
 *   <li>{@link #getNominalBiaya()} — menimpa tarif acuan dari tagihan.</li>
 *   <li>{@link #getTagihan()} — menulis ke object {@link Tagihan} (menandainya lunas).</li>
 *   <li>{@link #getItemBiayaSekolah()} — penulisan balik jinak, sekadar menyelesaikan proxy
 *   lazy lewat {@code check(...)}.</li>
 * </ul>
 * <p>Untuk membaca angka tanpa memicu efek-efek ini tersedia dua pendamping aman:
 * {@link #ambilNominal()} dan {@link #ambilTagihan()}. Kode yang hanya perlu menjumlahkan
 * sebaiknya memakai keduanya, atau projection SQL/HQL skalar — pendekatan yang dipilih secara
 * eksplisit (lengkap dengan komentar alasannya) di {@code TagihanUtil}.</p>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ol>
 *   <li><b>Jejak audit:</b> {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 *   <li><b>Identitas:</b> {@link #getId()}, {@link #toString()}, {@link #getRef()}.</li>
 *   <li><b>Relasi:</b> {@link #getPembayaranSiswa()}, {@link #getTagihan()},
 *   {@link #getItemBiayaSekolah()}, {@link #getNominalBiaya()},
 *   {@link #getPostingHistory()}.</li>
 *   <li><b>Rupiah:</b> {@link #getNominal()}, {@link #getNominalManual()},
 *   {@link #ambilNominal()}.</li>
 *   <li><b>Logika bisnis:</b> {@link #updateTagihan(Tagihan, Integer, Double, Session)}
 *   (pelunasan tagihan) dan
 *   {@link #buatPembayaran(Sekolah, Tagihan, Siswa, CalonSiswa, Tbmuser, Double, String,
 *   String, AkunPembayaranSiswa, java.util.List, java.util.Map)} (pembuatan kuitansi induk
 *   untuk jalur unggahan Excel — walau berada di kelas ini, ia tidak membuat baris rincian
 *   sama sekali).</li>
 * </ol>
 *
 * <h2>Catatan pemetaan: mengapa field induk dideklarasikan ulang</h2>
 * <p>{@link ais.database.model.GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass} — ia POJO abstrak biasa, sehingga Hibernate tidak memetakan
 * properti apa pun miliknya. Karena itu {@code id}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} <b>harus</b> dideklarasikan ulang di sini agar menjadi kolom. Ini
 * <b>keharusan teknis, bukan duplikasi yang perlu dirapikan</b>; menghapusnya akan menghapus
 * kolom-kolom itu dari pemetaan.</p>
 *
 * <h2>Audit dan jejak perubahan</h2>
 * <p>Kelas ini {@code @Audited} (Envers), sehingga perubahan lewat sesi Hibernate terekam di
 * tabel audit dan dapat ditinjau/dipulihkan lewat {@code RevisiPembayaranSiswaDetailHelper}.
 * Perlu diingat bahwa operasi berbasis SQL native / HQL bulk yang menyentuh tabel ini —
 * termasuk pembatalan posting jurnal dan pembersihan massal kuitansi yatim — <b>melewati</b>
 * Envers sekaligus {@link #onUpdate()}, sehingga riwayat audit finansial buta terhadap
 * perubahan-perubahan tersebut.</p>
 *
 * <h2>Peringatan keamanan</h2>
 * <p>Tabel ini menyimpan uang sungguhan dan, berdasarkan audit yang menyertai pendokumentasian
 * berkas ini, <b>terjangkau langsung dari HTTP tanpa autentikasi</b> — untuk BACA maupun
 * TULIS. Akarnya adalah aturan tangkap-semua
 * {@code <intercept-url pattern="/**" access="IS_AUTHENTICATED_ANONYMOUSLY"/>} pada
 * {@code applicationContext-security.xml}, sehingga satu-satunya gerbang adalah kode di dalam
 * masing-masing servlet. Ringkasnya:</p>
 * <ul>
 *   <li><b>Tulis anonim lewat callback bank.</b> Baris entity ini dibuat dan disimpan di
 *   {@code VirtualAccountBank.bayarSiswa(...)}, yang dipanggil selusin lebih servlet H2H
 *   tanpa tanda tangan/token apa pun ({@code /MncBank}, {@code /Va}, dan kerabatnya). Pada
 *   dua kanal yang <i>punya</i> verifikasi tanda tangan RSA, verifikasi itu hanya dipasang di
 *   cabang penerbitan token — cabang notifikasi pembayarannya tidak diperiksa.</li>
 *   <li><b>Tulis anonim lewat endpoint generik.</b> Pada {@code /Data}, penjagaan
 *   {@code tanpaLogin=true} hanya menutup aksi {@code update_data}/{@code update_file_data};
 *   aksi {@code simpanDataRinci}/{@code hapusDataRinci} lolos dan menerima nama kelas dari
 *   klien, sehingga baris tabel ini dapat disisipkan, diubah, atau dihapus oleh pemanggil
 *   anonim dengan {@code nominal} sepenuhnya dari klien.</li>
 *   <li><b>Pelunasan tagihan siswa arbitrer.</b> {@code VirtualAccountBank.ambilByNisAja}
 *   memperlakukan kode VA yang tidak dikenal sebagai NIS siswa mana pun, mencarikan tagihan
 *   yang belum lunas, lalu membuatkan VA — sehingga tagihan siswa mana pun dapat ditandai
 *   lunas hanya bermodalkan NIS dan besaran cicilan.</li>
 *   <li><b>Baca anonim.</b> {@code /Struk?id=N} membaca kelas ini tanpa cek login maupun
 *   kepemilikan, dengan id sekuensial yang mudah dienumerasi; {@code /Data} dengan
 *   {@code tanpaLogin=true} memberi dump generik dan SQL baca bebas
 *   ({@code SqlSecurityGuard} default non-aktif).</li>
 *   <li><b>Di sisi layar internal</b>, seluruh keluarga helper Revisi/Restore — termasuk
 *   {@code GenericRevisiHelper} yang menjadi dasarnya — <b>tidak memiliki gerbang hak sama
 *   sekali</b> dan tidak memfilter sekolah/yayasan, padahal jalur Restore-nya menulis ulang
 *   baris kuitansi dengan nilai teks bebas. Ditambah lagi {@code checkPrevilages} menilai
 *   <i>menu halaman induk</i> (yang di-cache lengket di sesi HTTP), sehingga hak pada menu
 *   bernilai rendah seperti "Pembayaran Online" atau "Pengaturan Biaya" ikut membuka
 *   penyuntingan baris kuitansi ini.</li>
 * </ul>
 * <p>Jangan menambah jalur baca/tulis baru ke entity ini tanpa memasang gerbang otentikasi,
 * pemeriksaan kepemilikan, dan pembatas tenant secara eksplisit.</p>
 *
 * @see ais.database.model.sekolah.PembayaranSiswa
 * @see ais.database.model.sekolah.Tagihan
 * @see ais.database.model.sekolah.NominalBiaya
 * @see ais.database.model.sekolah.ItemBiayaSekolah
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "pembayaran_siswa_detail", schema = "sekolah")
public class PembayaranSiswaDetail extends GeneralValueObject {

	/**
	 * Versi serialisasi. Instance entity ini ikut diserialisasi ketika ZK menyimpan state
	 * desktop/sesi (baris grid layar pembayaran, wizard, dan jendela revisi menahan referensi
	 * ke object ini), jadi nilai ini sebaiknya tidak diubah tanpa alasan.
	 */
	private static final long serialVersionUID = -4014084859898847843L;

	/** Kunci utama tabel {@code sekolah.pembayaran_siswa_detail}; lihat {@link #getId()}. */
	private Long id;

	/**
	 * Nama pengguna terakhir yang menyentuh baris ini (jejak audit). Dideklarasikan ULANG di
	 * sini walaupun {@code GeneralValueObject} juga punya field bernama sama — lihat catatan
	 * pemetaan pada Javadoc kelas: induknya bukan {@code @MappedSuperclass}, sehingga tanpa
	 * deklarasi ulang kolom ini tidak akan dipetakan Hibernate sama sekali.
	 */
	private String oleh;

	/**
	 * Id/username pengguna terakhir yang menyentuh baris ini (pendamping {@link #oleh}).
	 * Dipetakan ke kolom {@code olehid} — lihat {@link #getOlehId()}.
	 */
	private String olehId;

	/**
	 * @return id pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum pernah
	 *         diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatikan:</b> setter ini <b>menolak diam-diam</b> nilai {@code null} maupun string
	 * kosong/spasi — pemanggilan seperti itu tidak berbuat apa-apa dan nilai LAMA tetap
	 * bertahan. Jadi jejak audit di baris ini tidak pernah bisa dikosongkan, dan proses
	 * otomatis/anonim (mis. callback bank yang tidak punya pengguna login) yang memanggilnya
	 * dengan string kosong akan <b>meninggalkan nama pengguna sebelumnya</b> seolah-olah
	 * dialah yang melakukan transaksi terakhir. Pola penjagaan yang sama dipakai
	 * {@link #setOleh(String)}.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau hanya berisi spasi
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatikan:</b> sama seperti {@link #setOlehId(String)}, setter ini
	 * <b>menolak diam-diam</b> {@code null} dan string kosong/spasi, sehingga nilai lama
	 * bertahan. Konsekuensi audit-nya identik: baris pembayaran yang terakhir disentuh oleh
	 * proses tanpa pengguna akan tetap menampilkan nama pengguna sebelumnya.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau hanya berisi spasi
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum
	 *         pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dijalankan Hibernate tepat sebelum setiap UPDATE baris
	 * ini, dan mendelegasikan pengisian jejak audit ({@link #oleh}, {@link #olehId},
	 * {@link #tanggal_dirubah}) ke {@code AuditTimestampInterceptor}.
	 *
	 * <p>Ini adalah satu-satunya method {@code abstract} yang wajib diimplementasikan setiap
	 * turunan {@link ais.database.model.GeneralValueObject}; implementasi di sini adalah
	 * bentuk standar yang dipakai seluruh entity AIS.</p>
	 *
	 * <p><b>Catatan penting:</b> callback ini hanya terpicu pada UPDATE lewat sesi Hibernate.
	 * Operasi massal berbasis SQL native / HQL bulk yang menyentuh tabel ini (mis. penghapusan
	 * {@code grup_transaksi} + reset posting di layar Posting Cicilan) <b>melewati</b> callback
	 * ini sekaligus melewati Envers, sehingga perubahan tersebut tidak meninggalkan jejak
	 * audit.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir. Diinisialisasi ke waktu server saat object dibuat
	 * (bukan saat disimpan), lalu ditimpa {@code AuditTimestampInterceptor} lewat
	 * {@link #onUpdate()}. Dideklarasikan ulang di sini karena alasan pemetaan yang sama
	 * dengan {@link #oleh}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return stempel waktu perubahan terakhir baris ini
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Jenis biaya yang dibayar pada baris ini (SPP, Uang Gedung, Seragam, …). Kolom
	 * {@code item_biaya_id}, {@code nullable = false} — inilah yang membuat satu kuitansi bisa
	 * memuat beberapa baris berbeda. Lihat {@link #getItemBiayaSekolah()}.
	 */
	private ItemBiayaSekolah itemBiayaSekolah;

	/**
	 * Tarif (baris master nominal) yang menjadi acuan baris ini. Kolom
	 * {@code nominal_biaya_id}. Nilai ini <b>diselaraskan paksa</b> dari {@link #tagihan} tiap
	 * kali {@link #getNominalBiaya()} dipanggil.
	 */
	private NominalBiaya nominalBiaya;

	/**
	 * Kuitansi induk (kepala pembayaran) tempat baris ini bernaung. Kolom
	 * {@code pembayaran_siswa_id}, {@code nullable = false}. Perhatikan bahwa
	 * {@link PembayaranSiswa} <b>tidak</b> memiliki koleksi {@code @OneToMany} balik ke sini:
	 * pengumpulan baris detail selalu dilakukan lewat query eksplisit.
	 */
	private PembayaranSiswa pembayaranSiswa;

	/** Catatan bebas per baris (mis. "Pembayaran SPP Juli (via upload)"). Kolom {@code keterangan}. */
	private String keterangan;

	/**
	 * Nomor referensi eksternal, kolom {@code ref} dengan batasan {@code unique = true} —
	 * jelas dimaksudkan sebagai kunci idempoten transaksi bank. Lihat peringatan pada
	 * {@link #getRef()}: pada rantai pembayaran <i>sekolah</i> kolom ini sesungguhnya
	 * <b>tidak pernah diisi</b> oleh kode mana pun.
	 */
	private String ref;

	/**
	 * Rupiah yang tercatat pada baris ini. Kolom {@code nominal}. <b>Bukan</b> nilai yang
	 * stabil: {@link #getNominal()} dapat menghitung ulang dan <b>menimpa</b> field ini dari
	 * {@link #tagihan}/{@link #nominalBiaya} pada saat sekadar dibaca.
	 */
	private Double nominal;

	/**
	 * Nominal yang diketik/ditetapkan operator dan mengalahkan seluruh hasil hitung otomatis.
	 * Tidak diberi {@code @Column}, sehingga Hibernate memetakannya ke kolom bernama
	 * {@code nominalmanual} (PostgreSQL melipat identifier tanpa kutip menjadi huruf kecil) —
	 * nama ini dikonfirmasi oleh SQL native di {@code ais.common.DataRecoveryHelper}. Pola dan
	 * penamaan yang sama muncul pada {@link PembayaranSiswa}.
	 */
	private Double nominalManual;

	/**
	 * Penanda bahwa baris ini sudah diposting ke jurnal akuntansi. Kolom
	 * {@code posting_history_id}; {@code null} berarti belum diposting. Dipakai sebagai filter
	 * "sudah/belum posting" oleh mesin posting {@code PostingCicilanSiswaAction} dan
	 * {@code PostingDibayarDimukaSiswaAction}.
	 */
	private PostingHistory postingHistory;

	/**
	 * Membuat <b>kuitansi induk</b> ({@link PembayaranSiswa}) untuk sebuah pembayaran hasil
	 * unggahan Excel, menyimpannya, lalu membukukan setoran deposit yang menyertainya.
	 *
	 * <p><b>Nama dan penempatan method ini menyesatkan.</b> Walau berada di kelas
	 * {@code PembayaranSiswaDetail}, method ini <b>tidak pernah membuat satu pun baris
	 * detail</b>. Ia hanya merakit kepala kuitansi. Pemanggilnya-lah
	 * ({@code PembayaranSiswaAction}, jalur "Upload Excel Pembayaran", sekitar baris 817) yang
	 * membuat/menyiapkan {@code PembayaranSiswaDetail} sendiri lalu memasang hasil method ini
	 * lewat {@link #setPembayaranSiswa(PembayaranSiswa)}.</p>
	 *
	 * <p>Parameter {@code tagihan} pun <b>hanya dibaca</b> untuk menyalin periode
	 * ({@code bulan}, {@code tahun}, {@code tahunbulan}) dan jenis biaya; tagihan itu sendiri
	 * <b>tidak</b> ditandai lunas di sini — pelunasan baru terjadi ketika pemanggil menjalankan
	 * {@link #updateTagihan(Tagihan, Integer, Double, Session)}.</p>
	 *
	 * <h4>Alur</h4>
	 * <ol>
	 *   <li>Bila {@code akunPembayaranSiswa} tidak diberikan, dipilih otomatis satu akun
	 *   pembayaran yang aktif (atau yang kolom {@code aktif}-nya masih {@code null}) milik
	 *   sekolah tersebut. <b>Pemilihan ini tidak deterministik</b>: {@code setMaxResults(1)}
	 *   dipakai <b>tanpa</b> {@code addOrder}, jadi baris mana yang terpilih diserahkan pada
	 *   rencana eksekusi basis data. Sekolah dengan lebih dari satu akun pembayaran aktif bisa
	 *   mendapati kuitansi hasil unggahan yang sama jatuh ke akun buku besar berbeda-beda.
	 *   Kuirk yang sama terdokumentasi di {@link AkunPembayaranSiswa}.</li>
	 *   <li>Tanggal ({@code tanggal}) diurai dengan menebak formatnya lewat rangkaian
	 *   {@code if/else} atas panjang potongan string (5 format didukung: {@code dd-MM-yyyy HH:mm},
	 *   jam {@code HH:mm:ss}, {@code yyyy-MM-dd}, format bergaris-miring, dan format default).
	 *   Bila semua tebakan gagal, exception <b>ditelan</b> dan tanggal kuitansi diam-diam
	 *   menjadi <b>waktu server saat impor</b>, bukan tanggal yang tertulis di berkas Excel.</li>
	 *   <li>Kuitansi disimpan, lalu {@code pembayaranSiswa.saveOrUpdateDeposit(session)}
	 *   dipanggil di transaksi terpisah.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping finansial yang perlu diketahui:</b> selain {@code setNominal(nominal)},
	 * method ini juga memanggil {@code setTambahanDeposit(nominal)} dan kemudian
	 * {@code saveOrUpdateDeposit()}. Artinya <b>setiap</b> pembayaran hasil unggahan Excel
	 * dicatat dua kali: sekali sebagai pelunasan tagihan, sekali lagi sebagai <b>setoran ke
	 * dompet elektronik siswa</b>. Ini adalah "setoran deposit hantu" yang sudah dieskalasi
	 * bersama temuan {@link PembayaranSiswa}.</p>
	 *
	 * <p><b>Manajemen transaksi:</b> method ini membuka {@code Session} Hibernate sendiri lewat
	 * {@code openSession()} dan mengelola dua transaksi secara manual. Bila terjadi kegagalan,
	 * transaksi <b>tidak di-rollback</b> secara eksplisit — hanya sesi yang ditutup di blok
	 * {@code finally}. Bila kegagalan terjadi setelah commit pertama, kuitansi sudah terlanjur
	 * tersimpan tanpa deposit yang menyertainya.</p>
	 *
	 * @param sekolah              sekolah pemilik pembayaran; dipakai untuk memilih akun
	 *                             pembayaran dan mengisi {@code yayasan}
	 * @param tagihan              tagihan acuan; <b>hanya dibaca</b> untuk periode dan jenis
	 *                             biaya, tidak dilunasi di sini
	 * @param siswa                siswa pembayar (boleh {@code null} bila yang membayar calon
	 *                             siswa)
	 * @param calonSiswa           calon siswa pembayar (boleh {@code null} bila yang membayar
	 *                             siswa)
	 * @param tbmuser              pengguna yang menjalankan impor; namanya dicatat sebagai
	 *                             {@code validator}. Bila {@code null}, validator diisi string
	 *                             kosong sehingga kuitansi kehilangan jejak "siapa yang
	 *                             menerima uang"
	 * @param nominal              rupiah yang dibayarkan; dipakai sekaligus sebagai
	 *                             {@code nominal} dan {@code tambahanDeposit}
	 * @param tanggal              tanggal pembayaran dalam bentuk string bebas-format; lihat
	 *                             catatan penguraian di atas
	 * @param keterangan           catatan; disimpan dengan imbuhan {@code " (via upload)"}
	 * @param akunPembayaranSiswa  akun buku besar tujuan; bila {@code null} dipilih otomatis
	 *                             (tidak deterministik)
	 * @param warnings             daftar penampung pesan kegagalan untuk ditampilkan ke
	 *                             pengguna di akhir impor; boleh {@code null}
	 * @param datum                baris mentah dari berkas Excel, disertakan apa adanya ke
	 *                             dalam pesan peringatan agar operator bisa menelusuri baris
	 *                             mana yang gagal
	 * @return kuitansi yang baru tersimpan, atau {@code null} bila terjadi kegagalan (pemanggil
	 *         wajib memeriksa {@code null} dan menghentikan pemrosesan baris tersebut)
	 */
	@SuppressWarnings("rawtypes")
	public static PembayaranSiswa buatPembayaran(Sekolah sekolah, Tagihan tagihan, Siswa siswa, CalonSiswa calonSiswa,
			Tbmuser tbmuser, Double nominal, String tanggal, String keterangan, AkunPembayaranSiswa akunPembayaranSiswa,
			List<String> warnings, Map datum) {

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			if (akunPembayaranSiswa == null) {
				akunPembayaranSiswa = (AkunPembayaranSiswa) ConstantValues
						.simpleObject(
								session.createCriteria(AkunPembayaranSiswa.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("sekolah", sekolah)).setMaxResults(1),
								AkunPembayaranSiswa.class);
			}

			PembayaranSiswa pembayaranSiswa = new PembayaranSiswa();
			pembayaranSiswa.setAkunPembayaranSiswa(akunPembayaranSiswa);
			pembayaranSiswa.setBulan(tagihan.getBulan());
			pembayaranSiswa.setTahun(tagihan.getTahun());
			pembayaranSiswa.setCalonSiswa(calonSiswa);
			pembayaranSiswa.setSiswa(siswa);
			pembayaranSiswa.setSekolah(sekolah);
			pembayaranSiswa.setTahunDanBulan(tagihan.getTahunbulan());
			pembayaranSiswa.setValidator(tbmuser == null ? "" : tbmuser.getUserNama());
			pembayaranSiswa.setYayasan(sekolah.getYayasan());
			pembayaranSiswa.setJenisBiayaSekolah(tagihan.getPengaturanBiaya().getJenisBiayaSekolah());
			pembayaranSiswa.setNominal(nominal);
			String content = "";
			Date t = WaktuUtil.getDate();
			try {
				content = tanggal;
				if (content.trim().split("-")[2].split(" ")[0].length() == 4
						&& content.trim().split("-")[0].length() == 2 && content.trim().split(" ").length == 2) {
					t = Common.dateFormat3.get().parse(content.trim());
				} else if (content.trim().split(":").length == 3 && content.trim().length() == 8) {
					t = Common.dateFormat1.get().parse(content.trim());
				} else if (content.trim().split("-")[0].length() == 4) {
					t = Common.databaseDateFormat.get().parse(content.trim());
				} else if (content.trim().contains("/")) {
					t = Common.dateFormat112.get().parse(content.trim());
				} else {
					t = Common.dateFormat1.get().parse(content.trim());
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/PembayaranSiswaDetail.java:149");

			}

			pembayaranSiswa.setTanggal(t);
			pembayaranSiswa.setTambahanDeposit(nominal);
			pembayaranSiswa.setKeterangan(keterangan + " (via upload)");
			session.getTransaction().begin();
			session.save(pembayaranSiswa);
			session.getTransaction().commit();

			session.getTransaction().begin();
			pembayaranSiswa.saveOrUpdateDeposit(session);
			session.getTransaction().commit();
			return pembayaranSiswa;
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/PembayaranSiswaDetail.java:165");
			if (warnings != null) {
				warnings.add("Pembayaran gagal dibuat " + e.getMessage() + ". Data sbb : " + datum);
			}
		} finally {
			KegiatanPersistenceHelper.closeOpenedSession(session);
		}

		return null;
	}

	/**
	 * Representasi teks baris ini, berbentuk {@code "<id>-<tagihan>"}.
	 *
	 * <p>Meng-override {@code toString()} milik {@link ais.database.model.GeneralValueObject}
	 * (yang berformat {@code "kode - nama"}) karena baris detail tidak punya kode/nama sendiri.
	 * Method ini sengaja membaca <b>field</b> {@link #tagihan} secara langsung, bukan lewat
	 * {@link #getTagihan()}, sehingga aman dipakai di dalam log/debug: ia tidak ikut memicu
	 * efek samping penulisan balik yang dijelaskan pada {@link #getTagihan()}. Perhatikan
	 * bahwa {@code tagihan.toString()} sendiri tetap dapat menyentuh relasi lain.</p>
	 *
	 * @return teks ringkas untuk log dan pesan diagnostik
	 */
	public String toString() {
		return id + "-" + (tagihan == null ? "" : tagihan.toString());
	}

	/**
	 * Tagihan yang dilunasi baris ini. Kolom {@code tagihan}, {@code nullable = true}.
	 *
	 * <p>Deklarasi field ini sengaja diletakkan di tengah berkas (setelah {@link #toString()}
	 * yang memakainya) — sah secara Java, tetapi tidak lazim; jangan disalahartikan sebagai
	 * field yang berbeda dari yang dibaca {@link #toString()}.</p>
	 *
	 * <p>Ini adalah <b>salah satu dari dua</b> kolom FK yang menghubungkan pasangan
	 * tagihan&harr;pelunasan; pasangannya adalah {@code tagihan.pembayaran_siswa_detail_id}
	 * di sisi {@link Tagihan}. Lihat bagian "Relasi dua kolom FK" pada Javadoc kelas untuk
	 * konsekuensinya.</p>
	 */
	private Tagihan tagihan;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA untuk membentuk instance saat memuat
	 * baris dari basis data. Jangan dihapus sekalipun kode aplikasi tidak memanggilnya secara
	 * langsung.
	 */
	public PembayaranSiswaDetail() {
	}

	/**
	 * Membuat baris pelunasan baru untuk sebuah tagihan.
	 *
	 * <p>Hanya mengisi field {@link #tagihan}; sisi sebaliknya
	 * ({@code tagihan.pembayaran_siswa_detail_id}) <b>belum</b> terpasang. Penyelarasan sisi
	 * kedua terjadi belakangan, entah secara eksplisit oleh pemanggil
	 * ({@code tagihan.setPembayaranSiswaDetail(detail)}) atau secara implisit lewat efek
	 * samping {@link #getTagihan()}.</p>
	 *
	 * <p>Konstruktor inilah yang dipakai callback bank ({@code Bniresponse},
	 * {@code Bsiresponse}, {@code Briresponse}) untuk mencatat pelunasan hasil pembayaran di
	 * teller/mobile banking, dan juga oleh
	 * {@code PembayaranSiswa.simpanDetailPembayaran(...)}.</p>
	 *
	 * @param tagihan tagihan yang dilunasi baris ini; boleh {@code null}
	 */
	public PembayaranSiswaDetail(Tagihan tagihan) {
		this.tagihan = tagihan;
	}

	/**
	 * Varian ringkas {@link #updateTagihan(Tagihan, Integer, Double, Session)} yang mengambil
	 * nomor cicilan ({@code bayarKe}) langsung dari tagihan yang diberikan.
	 *
	 * <p>Berbeda dari varian 4-argumen, method ini <b>juga menetapkan</b> field
	 * {@link #tagihan} lebih dulu, sehingga relasi terpasang bahkan bila logika di varian
	 * 4-argumen memutuskan berhenti lebih awal.</p>
	 *
	 * <p>Ini adalah pintu masuk yang dipakai
	 * {@code PembayaranSiswa.simpanDetailPembayaran(...)} saat menyimpan kuitansi dari layar
	 * pembayaran.</p>
	 *
	 * @param tagihan tagihan yang dilunasi; boleh {@code null} (tagihan akan dicari/dibuat
	 *                otomatis oleh varian 4-argumen)
	 * @param nominal rupiah yang dibayarkan pada baris ini; pemrosesan hanya berjalan bila
	 *                nilainya &gt; 0,1
	 * @param session sesi Hibernate aktif milik pemanggil — method ini <b>tidak</b> membuka
	 *                atau menutup transaksi sendiri
	 */
	public void updateTagihan(Tagihan tagihan, Double nominal, Session session) {
		this.tagihan = tagihan;
		updateTagihan(tagihan, tagihan == null ? null : tagihan.getBayarKe(), nominal, session);
	}

	/**
	 * Menyelaraskan (atau membuat) {@link Tagihan} pasangan baris ini, lalu menyimpannya —
	 * inilah titik tempat sebuah tagihan <b>benar-benar berubah status menjadi lunas</b>.
	 *
	 * <p>Method ini adalah bagian paling berpengaruh di kelas ini. Ia dijalankan dari alur
	 * penyimpanan kuitansi ({@code PembayaranSiswa.simpanDetailPembayaran}) dan dari jalur
	 * unggahan Excel di {@code PembayaranSiswaAction}.</p>
	 *
	 * <h4>Alur</h4>
	 * <ol>
	 *   <li>Menyegarkan tiga relasi lewat getter-nya masing-masing
	 *   ({@link #getItemBiayaSekolah()}, {@link #getNominalBiaya()},
	 *   {@link #getPembayaranSiswa()}). Karena dua yang pertama <b>menulis balik</b> ke field,
	 *   pemanggilan method ini punya efek samping bahkan sebelum logika utamanya berjalan.</li>
	 *   <li>Seluruh sisanya hanya berjalan bila {@code pembayaranSiswa != null &amp;&amp;
	 *   nominalBiaya != null &amp;&amp; nominal > 0.1}. Bila salah satu tidak terpenuhi method
	 *   <b>diam-diam tidak melakukan apa pun</b> — kuitansi tersimpan, tagihan tetap
	 *   menunggak, tanpa pesan apa pun ke operator.</li>
	 *   <li>Bila {@code tagihan} {@code null}, tagihan dicari lewat kode unik
	 *   {@code Tagihan.genCode(...)} + {@code Tagihan.findByKodeUnik(...)}, dan bila tetap
	 *   tidak ketemu, tagihan <b>baru dibuat</b>. Ada dua cabang: periode
	 *   {@code "Insidentil"} dan periode lainnya (bulanan). Perhatikan bahwa
	 *   {@code genCode} adalah kunci identitas finansial yang punya kelemahan tersendiri
	 *   (kehilangan prefiks identitas siswa pada kombinasi tertentu &rarr; tabrakan kunci
	 *   lintas siswa) — lihat Javadoc {@link Tagihan}.</li>
	 *   <li>Relasi tagihan diisi ulang dari kuitansi ({@code siswa}, {@code calonSiswa},
	 *   {@code itemBiayaSekolah}) dan sisi balik dipasang lewat
	 *   {@code tagihan.setPembayaranSiswaDetail(this)}.</li>
	 *   <li>Penyimpanan: bila tagihan belum punya id, dicari dulu apakah kode uniknya sudah
	 *   ada di basis data; bila ada, id-nya diadopsi dan baris di-{@code refreshUpdate},
	 *   bila tidak, di-{@code save}. Bila sudah punya id, langsung di-{@code update}. Pola
	 *   ini menghindari duplikasi kode unik tapi <b>menimpa</b> tagihan lama yang kebetulan
	 *   berkode unik sama.</li>
	 * </ol>
	 *
	 * <h4>Hal-hal non-obvious yang perlu diwaspadai</h4>
	 * <ul>
	 *   <li><b>Penghentian senyap berbasis tahun masuk.</b> Pada cabang non-insidentil, bila
	 *   {@code tahunbulan} kuitansi lebih kecil atau sama dengan {@code <tahunMasuk>07} milik
	 *   siswa, method langsung {@code return} — tagihan tidak dibuat dan tidak dilunasi,
	 *   padahal uangnya sudah tercatat pada kuitansi. Maksudnya menolak tagihan untuk periode
	 *   sebelum siswa masuk, tetapi tidak ada umpan balik apa pun ke pengguna.</li>
	 *   <li><b>Risiko {@code NullPointerException} untuk pembayaran calon siswa.</b> Baris yang
	 *   menghitung {@code tahunMasukSiswa} memanggil
	 *   {@code pembayaranSiswa.getSiswa().getTahunMasuk()} tanpa penjagaan {@code null},
	 *   padahal kuitansi calon siswa memang punya {@code siswa == null}. Exception-nya
	 *   tertangkap blok {@code catch} di bawah dan hanya diteruskan ke
	 *   {@code Common.tampilErrorJikaAdmin(e)} — artinya <b>hanya pengguna admin</b> yang
	 *   melihat pesan kesalahan; bagi operator biasa pelunasan gagal tanpa gejala apa pun.</li>
	 *   <li><b>Blok besar yang dikomentari</b> (validasi kecocokan tagihan dengan kelas/tahun
	 *   angkatan siswa) sengaja dinonaktifkan. Selama blok itu mati, tidak ada yang mencegah
	 *   sebuah kuitansi dipasangkan ke tagihan yang pengaturan biayanya milik angkatan/kelas
	 *   lain.</li>
	 *   <li>Dua {@code System.out.println} ("cari kode unik …") mencetak kode identitas
	 *   finansial ke log stdout aplikasi.</li>
	 * </ul>
	 *
	 * <p><b>Efek samping:</b> menulis field {@link #itemBiayaSekolah}, {@link #nominalBiaya},
	 * {@link #pembayaranSiswa}, dan {@link #tagihan}; membuat/menyimpan/memperbarui baris
	 * {@link Tagihan}; mengubah relasi pada object {@link Tagihan}. Tidak mengelola transaksi
	 * — pemanggil yang bertanggung jawab commit/rollback.</p>
	 *
	 * @param tagihan tagihan yang dilunasi; bila {@code null} akan dicari lewat kode unik atau
	 *                dibuat baru
	 * @param bayarKe nomor cicilan ke berapa (komponen kode unik tagihan); boleh {@code null}
	 * @param nominal rupiah yang dibayarkan; nilai &le; 0,1 membuat method tidak berbuat apa-apa
	 * @param session sesi Hibernate aktif milik pemanggil, dipakai untuk pencarian dan
	 *                penyimpanan tagihan
	 */
	public void updateTagihan(Tagihan tagihan, Integer bayarKe, Double nominal, Session session) {
		itemBiayaSekolah = getItemBiayaSekolah();
		nominalBiaya = getNominalBiaya();
		pembayaranSiswa = getPembayaranSiswa();

//		KelasSiswa kelasSiswa = tagihan == null ? null : tagihan.getKelasSiswa();

//		if (nominalBiaya != null && nominalBiaya.getPengaturanBiaya() != null
//				&& !nominalBiaya.getPengaturanBiaya().getKhususBuatSiswaTertentu()
//				&& nominalBiaya.getPengaturanBiaya().getTahunAngkatan() != -1 && pembayaranSiswa != null
//				&& pembayaranSiswa.getSiswa() != null && pembayaranSiswa.getSiswa().getTahunMasuk() != null
//
//				&&
//
//				!((nominalBiaya.getPengaturanBiaya().getKelasSiswa() != null && kelasSiswa != null
//						&& nominalBiaya.getPengaturanBiaya().getKelasSiswa().getId().equals(kelasSiswa.getId()))
//						|| (pembayaranSiswa != null && nominalBiaya != null && nominalBiaya.getPengaturanBiaya()
//								.getTahunAngkatan().equals(pembayaranSiswa.getSiswa().getTahunMasuk())))
//
//		) {
//
//			try {
//				MyMessageboxConfig.show("Maaf, terdapat kesalahan data tagihan", "Peringatan", MyMessageboxConfig.OK,
//						MyMessageboxConfig.EXCLAMATION);
//			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/PembayaranSiswaDetail.java:218");
//				// TODO: handle exception
//			}
//			return;
//		}

		if (pembayaranSiswa != null && nominalBiaya != null && nominal > 0.1) {
			try {
				if (tagihan == null) {
					if (pembayaranSiswa.getJenisBiayaSekolah().getPeriode().equals("Insidentil")) {

						String kodeUnik = Tagihan.genCode(itemBiayaSekolah, nominalBiaya.getPengaturanBiaya(),
								pembayaranSiswa.getTahunDanBulan(), pembayaranSiswa.getSiswa(),
								pembayaranSiswa.getCalonSiswa(), bayarKe);
						System.out.println("cari kode unik insidentil => " + kodeUnik);
						tagihan = Tagihan.findByKodeUnik(kodeUnik, session);
						if (tagihan == null) {
							tagihan = new Tagihan(nominalBiaya, pembayaranSiswa.getTahunDanBulan(),
									pembayaranSiswa.getBulan(), pembayaranSiswa.getTahun(), this,
									pembayaranSiswa.getSiswa(), bayarKe);
						}
					} else {
						if (pembayaranSiswa.getTahunDanBulan() != null) {
							int bulanTahunUtama = pembayaranSiswa.getTahunDanBulan();
							int tahunMasukSiswa = Integer
									.parseInt(pembayaranSiswa.getSiswa().getTahunMasuk().toString() + "07");
							System.out.println("siswa " + pembayaranSiswa.getSiswa() + ", bulanTahunUtama => "
									+ bulanTahunUtama + ", tahunMasukSiswa => " + tahunMasukSiswa);
							if (bulanTahunUtama <= tahunMasukSiswa) {
								return;
							}
						}
						String kodeUnik = Tagihan.genCode(itemBiayaSekolah, nominalBiaya.getPengaturanBiaya(),
								pembayaranSiswa.getTahunDanBulan(), pembayaranSiswa.getSiswa(),
								pembayaranSiswa.getCalonSiswa(), bayarKe);
						System.out.println("cari kode unik bulanan => " + kodeUnik);
						tagihan = Tagihan.findByKodeUnik(kodeUnik, session);
						if (tagihan == null) {
							tagihan = new Tagihan(nominalBiaya, pembayaranSiswa.getTahunDanBulan(),
									pembayaranSiswa.getBulan(), pembayaranSiswa.getTahun(), this,
									pembayaranSiswa.getSiswa(), bayarKe);
						}
					}
				}
				tagihan.setSiswa(pembayaranSiswa.getSiswa());
				tagihan.setCalonSiswa(pembayaranSiswa.getCalonSiswa());
				tagihan.setItemBiayaSekolah(itemBiayaSekolah);
				tagihan.setPembayaranSiswaDetail(this);

				if (tagihan.getId() == null) {
					Tagihan temptagihan = Tagihan.findByKodeUnik(tagihan.getKodeUnik(), session);
					if (temptagihan != null) {
						tagihan.setId(temptagihan.getId());
						Common.refreshUpdate(session, tagihan);
					} else {
						session.save(tagihan);
					}
				} else {
					session.update(tagihan);
				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

		}

	}

	/**
	 * Kunci utama baris, dibangkitkan basis data ({@code IDENTITY}/sequence).
	 *
	 * <p>{@code insertable = false} berarti nilai id yang ada di object <b>tidak pernah</b>
	 * ikut dikirim pada INSERT — basis data selalu yang menentukan. Id ini juga menjadi dasar
	 * {@code equals()} yang diwarisi dari {@link ais.database.model.GeneralValueObject};
	 * ingat bahwa {@code hashCode()} di sana <b>tidak</b> di-override, jadi jangan memakai
	 * {@code HashSet}/{@code HashMap} berkunci entity ini untuk deduplikasi.</p>
	 *
	 * <p>Id ini bersifat sekuensial dan tampil apa adanya pada beberapa jalur cetak/struk,
	 * sehingga dapat dienumerasi.</p>
	 *
	 * @return kunci utama baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan tarif acuan baris ini, <b>setelah menyelaraskannya paksa</b> dengan tarif
	 * yang tercantum pada {@link Tagihan} pasangannya.
	 *
	 * <p><b>Getter dengan efek samping.</b> Sebelum mengembalikan nilai, method ini:</p>
	 * <ol>
	 *   <li>memanggil {@link #getTagihan()} dan <b>menimpa</b> field {@link #tagihan} dengan
	 *   hasilnya — sehingga ikut memicu seluruh efek samping getter tersebut;</li>
	 *   <li>bila tagihan itu punya {@code nominalBiaya}, <b>menimpa</b> field
	 *   {@link #nominalBiaya} milik baris ini dengan milik tagihan.</li>
	 * </ol>
	 *
	 * <p>Konsekuensinya: tarif acuan yang tercatat pada kuitansi <b>tidak dijamin abadi</b>.
	 * Bila di kemudian hari tagihan dipindahkan ke tarif lain (mis. lewat
	 * {@code NewUiTagihanService.movePayment}, atau lewat sinkronisasi tarif), maka sekadar
	 * <i>membaca</i> baris kuitansi lama sudah cukup untuk mengubah acuannya. Karena entity
	 * ini dipetakan dengan akses properti dan {@code dynamicUpdate}, perubahan itu bisa
	 * ikut ter-flush ke basis data pada akhir transaksi tanpa ada kode yang secara sengaja
	 * "menyimpan" apa pun.</p>
	 *
	 * <p>Dipanggil dari {@link #updateTagihan(Tagihan, Integer, Double, Session)},
	 * {@link #getNominal()}, layar pembayaran, laporan, dan mesin posting jurnal.</p>
	 *
	 * @return tarif acuan baris ini, atau {@code null} bila tidak ada
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "nominal_biaya_id")
	public NominalBiaya getNominalBiaya() {
		tagihan = getTagihan();
		if (tagihan != null && tagihan.getNominalBiaya() != null) {
			nominalBiaya = tagihan.getNominalBiaya();
		}
		return this.nominalBiaya;
	}

	/**
	 * @param nominalBiaya tarif acuan baris ini
	 */
	public void setNominalBiaya(NominalBiaya nominalBiaya) {
		this.nominalBiaya = nominalBiaya;
	}

	/**
	 * Mengembalikan kuitansi induk baris ini.
	 *
	 * <p><b>PENTING — pemeriksaan integritas di dalam method ini tidak berpengaruh apa pun
	 * (kode mati).</b> Blok {@code try} di sini bermaksud mendeteksi kondisi berbahaya:
	 * baris detail yang tagihannya milik <i>siswa/calon siswa A</i> tetapi menempel pada
	 * kuitansi milik <i>siswa/calon siswa B</i> — persis kasus tercampurnya uang antar siswa.
	 * Bila terdeteksi, kode meng-{@code null}-kan hasilnya. Namun nilai yang di-{@code null}-kan
	 * adalah <b>variabel lokal</b> yang menaungi (<i>shadow</i>) nama field, sementara baris
	 * {@code return} mengembalikan <b>field</b> {@code this.pembayaranSiswa}. Akibatnya
	 * variabel lokal itu tidak pernah dibaca siapa pun dan pemeriksaan silang antar-siswa
	 * <b>tidak pernah benar-benar berlaku</b>: kuitansi milik siswa lain tetap dikembalikan
	 * apa adanya ke seluruh pemanggil (layar pembayaran, cetak struk, mesin posting jurnal,
	 * laporan rekap).</p>
	 *
	 * <p>Perilaku ini termasuk kategori integritas data finansial lintas-siswa yang sudah
	 * dieskalasi bersama temuan {@link Tagihan}/{@link PembayaranSiswa}; jangan "merapikan"
	 * kode ini tanpa perencanaan, karena mengaktifkan pemeriksaannya (mengembalikan variabel
	 * lokal) akan membuat sebagian baris lama tiba-tiba kehilangan kuitansi induknya pada
	 * kolom yang berstatus {@code nullable = false}.</p>
	 *
	 * <p>Exception apa pun di dalam blok pemeriksaan ditelan dan dicatat ke audit error,
	 * sehingga tidak pernah menggagalkan pembacaan.</p>
	 *
	 * @return kuitansi induk baris ini; secara praktis selalu isi field apa adanya
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pembayaran_siswa_id", nullable = false)
	public PembayaranSiswa getPembayaranSiswa() {
		PembayaranSiswa pembayaranSiswa = this.pembayaranSiswa;
		try {
			if (tagihan != null && tagihan.getSiswa() != null && pembayaranSiswa != null
					&& pembayaranSiswa.getSiswa() != null
					&& !tagihan.getSiswa().getId().equals(pembayaranSiswa.getSiswa().getId())) {
				pembayaranSiswa = null;
			} else if (tagihan != null && tagihan.getCalonSiswa() != null && pembayaranSiswa != null
					&& pembayaranSiswa.getCalonSiswa() != null
					&& !tagihan.getCalonSiswa().getId().equals(pembayaranSiswa.getCalonSiswa().getId())) {
				pembayaranSiswa = null;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/PembayaranSiswaDetail.java:328");
			// TODO: handle exception
		}

		return this.pembayaranSiswa;
	}

	/**
	 * Memasang kuitansi induk baris ini.
	 *
	 * <p><b>Setter penjaga.</b> Pemanggilan dengan {@code null}, maupun dengan kuitansi yang
	 * <b>belum tersimpan</b> ({@code id == null}), <b>diabaikan diam-diam</b>: nilai lama
	 * dipertahankan dan tidak ada exception maupun peringatan. Penjagaan ini melindungi kolom
	 * {@code pembayaran_siswa_id} yang berstatus {@code nullable = false} dari
	 * {@code TransientObjectException} Hibernate.</p>
	 *
	 * <p>Konsekuensi praktis yang harus diperhatikan pemanggil:</p>
	 * <ul>
	 *   <li>Kuitansi <b>wajib disimpan lebih dulu</b> sebelum dipasang ke baris detail. Inilah
	 *   sebabnya {@link #buatPembayaran(Sekolah, Tagihan, Siswa, CalonSiswa, Tbmuser, Double,
	 *   String, String, AkunPembayaranSiswa, java.util.List, java.util.Map)} melakukan
	 *   {@code save} + {@code commit} sebelum hasilnya diserahkan ke setter ini.</li>
	 *   <li>Relasi ini <b>tidak dapat dilepas</b> lewat setter ini. Memanggilnya dengan
	 *   {@code null} untuk "melepaskan" baris dari kuitansinya tidak akan berhasil; baris
	 *   tetap menempel pada kuitansi sebelumnya.</li>
	 *   <li>Bila pemanggil salah urutan (memasang kuitansi transient lalu menyimpan detail),
	 *   kegagalan tidak muncul di sini melainkan jauh kemudian sebagai pelanggaran batasan
	 *   {@code NOT NULL} — atau, lebih buruk, baris tersimpan menempel pada kuitansi
	 *   <i>sebelumnya</i>.</li>
	 * </ul>
	 *
	 * @param pembayaranSiswa kuitansi induk yang sudah tersimpan; {@code null} atau kuitansi
	 *                        yang belum punya id akan diabaikan
	 */
	public void setPembayaranSiswa(PembayaranSiswa pembayaranSiswa) {
		if (pembayaranSiswa == null || pembayaranSiswa.getId() == null) {
			return;
		}

		this.pembayaranSiswa = pembayaranSiswa;
	}

	/**
	 * @return catatan bebas untuk baris ini, atau {@code null}
	 */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * @param keterangan catatan bebas untuk baris ini
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengambil rupiah baris ini secara "aman" — <b>tanpa</b> menghitung ulang dari tagihan.
	 *
	 * <p>Ini adalah pendamping ringan bagi {@link #getNominal()}: ia hanya menerapkan
	 * penimpaan oleh {@link #getNominalManual()} lalu mengembalikan nilai yang ada, dan
	 * <b>tidak</b> menyentuh {@link #getTagihan()}/{@link #getNominalBiaya()}. Karena itulah
	 * jalur-jalur yang harus membaca nominal tanpa memicu efek samping — mis.
	 * {@code Tagihan.getPembayaranSiswaDetail()}, {@code Tagihan.getDibayar()}, dan
	 * penjumlahan struk di {@code PembayaranSiswaUtil.dataPembayaran} — memakai method ini,
	 * bukan getter properti.</p>
	 *
	 * <p>Tetap perlu dicatat bahwa method ini <b>bukan</b> bebas efek samping sepenuhnya: bila
	 * ada nominal manual, field {@link #nominal} ikut ditimpa dengan nilai manual tersebut.</p>
	 *
	 * <p>Nilai {@code null} dinormalkan menjadi {@code 0.0} agar pemanggil dapat menjumlahkan
	 * tanpa penjagaan {@code null}.</p>
	 *
	 * @return rupiah baris ini, atau {@code 0.0} bila belum terisi
	 */
	public Double ambilNominal() {
		if (getNominalManual() != null) {
			nominal = getNominalManual();
		}
		return this.nominal == null ? 0.0 : nominal;
	}

	/**
	 * Mengembalikan rupiah baris ini, <b>menghitungnya ulang dan menimpanya</b> dari tarif dan
	 * tagihan bila jenis biayanya tidak boleh diubah saat pembayaran.
	 *
	 * <p><b>Getter paling destruktif di kelas ini.</b> Ia bukan sekadar pembaca: pada cabang
	 * hitung-ulang, field {@link #nominal} — angka rupiah yang tercatat sebagai "yang
	 * benar-benar diterima" pada kuitansi — <b>ditulis ulang</b> dengan nilai tagihan
	 * <i>hari ini</i>. Karena entity ini memakai akses properti, Hibernate memanggil getter
	 * inilah saat menentukan apa yang perlu ditulis ke basis data; kombinasi dengan
	 * {@code dynamicUpdate} membuat perubahan tersebut bisa ter-flush tanpa satu pun baris
	 * kode yang secara sengaja menyimpannya.</p>
	 *
	 * <h4>Urutan penentuan nilai</h4>
	 * <ol>
	 *   <li>Bila {@link #getNominalManual()} terisi, nilai manual itu menang dan langsung
	 *   menimpa {@link #nominal}. Tidak ada perhitungan lain yang dijalankan.</li>
	 *   <li>Selain itu, {@link #getItemBiayaSekolah()}, {@link #getNominalBiaya()}, dan
	 *   {@link #getTagihan()} dipanggil (masing-masing dengan efek sampingnya sendiri), lalu:
	 *     <ul>
	 *       <li>bila item biaya <b>boleh</b> diubah saat pembayaran, atau tagihan {@code null},
	 *       nilai tersimpan dikembalikan apa adanya;</li>
	 *       <li>bila tidak boleh diubah, nominal dihitung ulang: untuk biaya non-bulanan yang
	 *       dibayar sekali, dari {@code nominalBiaya.ambilNominal() - tagihan.getDiskon()};
	 *       selain itu dari {@code tagihan.ambilNominal() - tagihan.getDiskon()}.</li>
	 *     </ul>
	 *   </li>
	 * </ol>
	 *
	 * <h4>Kuirk dan risiko</h4>
	 * <ul>
	 *   <li><b>Kuitansi lama ikut berubah.</b> Bila nominal tagihan berubah setelah pembayaran
	 *   (perubahan tarif, sinkronisasi diskon, atau bug penggelembungan nominal yang
	 *   terdokumentasi pada {@link Tagihan}), maka membuka kembali kuitansi lama sudah cukup
	 *   untuk mengubah angka historisnya. Rupiah yang tercetak di struk hari ini belum tentu
	 *   sama dengan yang tercetak bulan lalu untuk transaksi yang sama.</li>
	 *   <li><b>Pemeriksaan {@code null} terlambat.</b> {@code nominalBiaya.ambilNominal()}
	 *   sudah dipanggil sebelum baris yang memeriksa {@code nominalBiaya != null}. Bila tarif
	 *   acuannya hilang, {@code NullPointerException} terjadi, tertangkap {@code catch} di
	 *   bawah, dicetak ke stdout + audit error, dan method mengembalikan nilai lama — gagal
	 *   secara senyap.</li>
	 *   <li>Karena alasan-alasan di atas, kode yang hanya perlu <i>membaca</i> angka sebaiknya
	 *   memakai {@link #ambilNominal()} atau projection SQL/HQL skalar; itulah yang dilakukan
	 *   {@code TagihanUtil} secara eksplisit.</li>
	 *   <li>Anotasi kolomnya {@code precision = 17, scale = 17}. Bila DDL benar-benar
	 *   dibangkitkan dari anotasi ini, tipe yang terbentuk adalah {@code numeric(17,17)} yang
	 *   sama sekali <b>tidak punya digit di depan koma</b> (hanya bisa menyimpan nilai &lt; 1) —
	 *   jelas bukan yang dimaksud untuk kolom rupiah. Pola salah ketik yang sama muncul pada
	 *   12 kolom uang lain di modul ini (lihat {@link DepositSiswa}, {@link NominalBiaya},
	 *   {@link PembayaranSiswa}, {@link Tagihan}), jadi skema produksi hampir pasti dibuat/
	 *   dimigrasikan di luar generator DDL.</li>
	 * </ul>
	 *
	 * @return rupiah baris ini setelah penimpaan/penghitungan ulang; dapat {@code null} bila
	 *         baris belum pernah diisi nominal
	 */
	@Column(name = "nominal", precision = 17, scale = 17)
	public Double getNominal() {

		if (getNominalManual() != null) {
			nominal = getNominalManual();
		} else {
			try {
				itemBiayaSekolah = getItemBiayaSekolah();
				nominalBiaya = getNominalBiaya();
				tagihan = getTagihan();

				if (itemBiayaSekolah != null && !itemBiayaSekolah.getNilaiBiayaBisaDiubahSaatPembayaran()
						&& tagihan != null) {

					if (nominal == null) {
						this.nominal = nominalBiaya.ambilNominal() - (tagihan == null ? 0.0 : tagihan.getDiskon());
					}

					if (nominalBiaya != null
							&& !nominalBiaya.getPengaturanBiaya().getJenisBiayaSekolah().getPeriode().equals("Bulanan")
							&& nominalBiaya.getDibayarSebayak().equals(1)) {
						this.nominal = nominalBiaya.ambilNominal() - (tagihan == null ? 0.0 : tagihan.getDiskon());
					} else {
						this.nominal = (tagihan == null ? 0.0 : tagihan.ambilNominal())
								- (tagihan == null ? 0.0 : tagihan.getDiskon());
					}
					return this.nominal;
				} else {
					return this.nominal;
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/PembayaranSiswaDetail.java:390");
			}
		}
		return this.nominal;
	}

	/**
	 * Menetapkan rupiah baris ini secara langsung.
	 *
	 * <p>Setter polos tanpa penjagaan. Perlu diingat bahwa nilai yang ditetapkan di sini
	 * <b>tidak permanen</b>: {@link #getNominal()} dapat menghitungnya ulang pada pembacaan
	 * berikutnya. Pemanggil yang ingin angkanya bertahan harus mengisi
	 * {@link #setNominalManual(Double)} juga — itulah pola yang dipakai konsisten oleh
	 * callback bank, jalur unggahan Excel, dan {@code PembayaranSiswa.simpanDetailPembayaran}.</p>
	 *
	 * @param nominal rupiah baris ini
	 */
	public void setNominal(Double nominal) {
		this.nominal = nominal;
	}

	/**
	 * Mengembalikan jenis biaya yang dibayar pada baris ini, setelah proxy lazy-nya
	 * diselesaikan.
	 *
	 * <p>Relasi ini dipetakan {@code FetchType.LAZY}, sehingga nilai yang tersimpan pada field
	 * bisa berupa proxy Hibernate dari sesi yang sudah ditutup. {@code check(...)} yang
	 * diwarisi dari {@link ais.database.model.GeneralValueObject} menyelesaikan proxy tersebut
	 * (memuatnya ulang bila perlu) dan hasilnya <b>ditulis balik</b> ke field — penulisan
	 * balik yang jinak, karena hanya mengganti proxy dengan object yang setara.</p>
	 *
	 * <p>Kolomnya {@code nullable = false}: setiap baris detail wajib menyebut item biaya apa
	 * yang dibayar. Inilah yang membuat satu kuitansi dapat memuat beberapa baris berbeda
	 * (SPP, Uang Kegiatan, Seragam, …), dan yang menjadi dasar pengelompokan di laporan rekap
	 * maupun pemilihan akun jurnal saat posting.</p>
	 *
	 * @return jenis biaya baris ini; secara praktis tidak pernah {@code null} untuk baris yang
	 *         sudah tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item_biaya_id", nullable = false)
	public ItemBiayaSekolah getItemBiayaSekolah() {
		itemBiayaSekolah = check(itemBiayaSekolah);
		return itemBiayaSekolah;
	}

	/**
	 * @param itemBiayaSekolah jenis biaya yang dibayar pada baris ini
	 */
	public void setItemBiayaSekolah(ItemBiayaSekolah itemBiayaSekolah) {
		this.itemBiayaSekolah = itemBiayaSekolah;
	}

	/**
	 * Mengembalikan riwayat posting jurnal baris ini.
	 *
	 * <p>{@code null} berarti baris ini <b>belum</b> diposting ke akuntansi. Mesin posting
	 * ({@code PostingCicilanSiswaAction} untuk pembayaran cicilan/reguler dan
	 * {@code PostingDibayarDimukaSiswaAction} untuk pembayaran di muka) memakai properti ini
	 * lewat {@code PostingJurnalHelper.restriksiPosting("postingHistory", …)} sebagai filter
	 * "sudah/belum tampil", dan mengisinya saat operator menekan tombol Posting.</p>
	 *
	 * <p>Pembatalan posting mengosongkan kembali properti ini sekaligus menghapus baris
	 * {@code akunting.grup_transaksi} terkait lewat SQL native
	 * ({@code delete from akunting.grup_transaksi where pembayaran_siswa_detail=…}) — operasi
	 * yang, karena berbentuk SQL native, <b>melewati Envers</b> sehingga tidak meninggalkan
	 * jejak audit.</p>
	 *
	 * @return riwayat posting, atau {@code null} bila baris ini belum diposting
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history_id")
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * @param postingHistory riwayat posting jurnal; {@code null} untuk menandai baris ini
	 *                       belum/batal diposting
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}
	
	/**
	 * Mengembalikan tagihan pasangan baris ini <b>apa adanya</b>, tanpa efek samping apa pun.
	 *
	 * <p>Ini adalah pendamping aman bagi {@link #getTagihan()}, yang justru menulis balik ke
	 * object {@link Tagihan}. Gunakan method ini di mana pun yang dibutuhkan hanya
	 * "apakah/ke mana relasi ini menunjuk saat ini", terutama di dalam getter entity lain —
	 * pemakaian terpentingnya ada di {@code Tagihan.getPembayaranSiswaDetail()} dan
	 * {@code PengaturanBiaya}, yang memakainya justru untuk <b>mendeteksi</b> relasi yang
	 * belum tersambung tanpa memicu penyambungan otomatis (yang akan menyebabkan rekursi).</p>
	 *
	 * <p>Berhati-hatilah: karena tidak menyentuh {@code check(...)}, nilai yang dikembalikan
	 * bisa berupa proxy Hibernate yang belum terinisialisasi.</p>
	 *
	 * @return tagihan pasangan baris ini, atau {@code null}
	 */
	public Tagihan ambilTagihan() {
		return tagihan;
	}
	

	/**
	 * Mengembalikan tagihan yang dilunasi baris ini, sambil <b>memperbaiki paksa</b> sisi
	 * sebaliknya dari relasi.
	 *
	 * <p><b>Getter dengan efek samping lintas-entity.</b> Bila tagihan yang ditunjuk ternyata
	 * belum menunjuk balik ke baris ini, method ini menjalankan
	 * {@code tagihan.setPembayaranSiswaDetail(this)} — yaitu <b>menulis ke object lain</b>.
	 * Di sisi {@link Tagihan}, properti itulah penanda bahwa tagihan sudah LUNAS.</p>
	 *
	 * <p>Ini adalah separuh dari sepasang "getter perbaikan-mandiri": pasangannya,
	 * {@code Tagihan.getPembayaranSiswaDetail()}, melakukan hal simetris dengan memanggil
	 * {@code pembayaranSiswaDetail.setTagihan(this)}. Keduanya ada karena relasi ini
	 * dipetakan dengan <b>dua kolom FK terpisah</b> yang bisa saling tidak sinkron (lihat
	 * Javadoc kelas).</p>
	 *
	 * <p><b>Konsekuensi yang perlu diwaspadai:</b> sekadar <i>membaca</i> baris detail yatim —
	 * mis. saat merender grid, mencetak struk, atau menyusun laporan — dapat
	 * <b>menghidupkan kembali</b> status LUNAS pada tagihan yang sisi baliknya sudah sengaja
	 * dikosongkan. Perbaikan otomatis ini juga tidak simetris dengan penghapusan: bila baris
	 * detail dihapus, kolom {@code tagihan.pembayaran_siswa_detail_id} <b>tidak</b> ikut
	 * dibersihkan, meninggalkan FK menggantung yang di produksi memunculkan
	 * {@code ConstraintViolationException} pada job sinkronisasi tagihan — kasus yang sudah
	 * ditangani secara defensif di {@code TagihanUtil} dengan verifikasi keberadaan baris
	 * lewat SQL native sebelum menulis.</p>
	 *
	 * <p>Dipanggil dari {@link #getNominalBiaya()}, {@link #getNominal()}, dan seluruh layar
	 * pembayaran/laporan/posting.</p>
	 *
	 * @return tagihan yang dilunasi baris ini, atau {@code null} bila baris ini tidak terikat
	 *         tagihan tertentu
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "tagihan", nullable = true)
	public Tagihan getTagihan() {

		if (tagihan != null && tagihan.ambilPembayaranSiswaDetail() == null) {
			tagihan.setPembayaranSiswaDetail(this);
		}

		return tagihan;
	}

	/**
	 * Menetapkan tagihan yang dilunasi baris ini.
	 *
	 * <p>Setter polos: hanya mengisi kolom {@code tagihan} pada baris <b>ini</b>. Sisi
	 * sebaliknya ({@code tagihan.pembayaran_siswa_detail_id}) <b>tidak</b> ikut diperbarui,
	 * jadi pemanggil yang memindahkan sebuah pelunasan antar tagihan wajib mengurus kedua
	 * sisi sendiri. Contoh yang menanganinya dengan benar adalah
	 * {@code NewUiTagihanService.movePayment}, yang memasang
	 * {@code payment.setTagihan(target)} sekaligus mengosongkan sisi lama dan mengisi sisi
	 * baru dalam satu transaksi.</p>
	 *
	 * <p>Selain dipanggil kode aplikasi, setter ini juga dipanggil oleh
	 * {@code Tagihan.getPembayaranSiswaDetail()} sebagai bagian dari mekanisme
	 * perbaikan-mandiri relasi — artinya nilainya dapat berubah tanpa ada kode aplikasi yang
	 * memanggilnya secara langsung.</p>
	 *
	 * @param tagihan tagihan yang dilunasi baris ini, atau {@code null}
	 */
	public void setTagihan(Tagihan tagihan) {
		this.tagihan = tagihan;
	}

	/**
	 * Nomor referensi eksternal baris ini.
	 *
	 * <p><b>Kolom ini praktis tidak terpakai pada rantai pembayaran sekolah.</b> Batasan
	 * {@code unique = true} menunjukkan maksud awalnya sebagai kunci idempoten transaksi bank
	 * (agar satu notifikasi pembayaran yang dikirim ulang tidak menghasilkan dua baris
	 * pelunasan), tetapi penelusuran seluruh kode menunjukkan <b>tidak ada satu pun</b> jalur
	 * yang memanggil {@link #setRef(String)} pada entity ini — termasuk callback bank, yang
	 * justru menyimpan nomor referensinya pada entity {@code CicilanPembayaran}. Satu-satunya
	 * pemakaian {@code ref} di sini adalah sebagai kolom yang bisa dicari pada jendela revisi
	 * {@code RevisiPembayaranSiswaDetailHelper}.</p>
	 *
	 * <p>Akibatnya perlindungan idempoten yang seharusnya diberikan batasan {@code unique}
	 * tidak pernah aktif: seluruh baris memiliki {@code ref} bernilai {@code null}, dan pada
	 * PostgreSQL nilai {@code NULL} dikecualikan dari batasan unik sehingga sebanyak apa pun
	 * baris duplikat tetap diterima. Ini relevan untuk kanal bank yang mengirim ulang
	 * notifikasi pembayaran.</p>
	 *
	 * @return nomor referensi eksternal; pada praktiknya selalu {@code null}
	 */
	@Column(unique = true)
	public String getRef() {
		return ref;
	}

	/**
	 * @param ref nomor referensi eksternal; lihat catatan pada {@link #getRef()} — tidak ada
	 *            pemanggil untuk entity ini
	 */
	public void setRef(String ref) {
		this.ref = ref;
	}

	/**
	 * Mengembalikan nominal yang ditetapkan operator secara manual, yang mengalahkan seluruh
	 * hasil hitung otomatis.
	 *
	 * <p>Inilah properti yang membuat angka rupiah pada sebuah baris bisa "dibekukan":
	 * selama ia terisi, {@link #getNominal()} dan {@link #ambilNominal()} berhenti menghitung
	 * ulang dari tagihan dan langsung memakai nilai ini. Diisi oleh callback bank, jalur
	 * unggahan Excel, layar pembayaran online, dan helper detail tagihan.</p>
	 *
	 * <p><b>Kuirk penting — nol diperlakukan sebagai "tidak diisi".</b> Getter ini
	 * mengembalikan {@code null} bila nilainya {@code null} <i>atau</i> bila
	 * {@code intValue() == 0}. Karena {@code intValue()} <b>memotong</b> bagian desimal
	 * (bukan membulatkan), efeknya lebih luas dari sekadar nol:</p>
	 * <ul>
	 *   <li>penimpaan manual bernilai Rp 0 tidak dapat direpresentasikan sama sekali —
	 *   pembebasan biaya harus dinyatakan lewat diskon atau penanda "bukan tagihan", bukan
	 *   lewat properti ini;</li>
	 *   <li><b>setiap</b> nominal manual di bawah Rp 1,00 (mis. 0,5) juga diam-diam dianggap
	 *   tidak ada, sehingga baris kembali dihitung otomatis dari tagihan;</li>
	 *   <li>karena entity ini memakai akses properti, getter inilah yang dibaca Hibernate saat
	 *   flush — sehingga nilai 0 yang tersimpan di basis data akan <b>dinormalkan menjadi
	 *   {@code NULL}</b> pada penulisan berikutnya.</li>
	 * </ul>
	 *
	 * <p>Pemanggil yang memang bermaksud mengosongkan penimpaan biasanya mengisi {@code 0.0}
	 * dengan sengaja (lihat {@code PembayaranOnline}), mengandalkan perilaku di atas.</p>
	 *
	 * @return nominal manual, atau {@code null} bila tidak ada penimpaan manual yang berlaku
	 *         (termasuk bila nilainya 0 atau kurang dari 1)
	 */
	public Double getNominalManual() {
		return nominalManual == null || nominalManual.intValue() == 0 ? null : nominalManual;
	}

	/**
	 * Menetapkan nominal manual yang mengalahkan hasil hitung otomatis.
	 *
	 * <p>Setter polos — nilai disimpan apa adanya, termasuk {@code 0.0} dan nilai pecahan di
	 * bawah 1. Penyaringannya baru terjadi pada {@link #getNominalManual()}, jadi apa yang
	 * ditulis di sini belum tentu sama dengan apa yang dibaca kembali.</p>
	 *
	 * @param nominalManual nominal manual, atau {@code 0.0}/{@code null} untuk melepaskan
	 *                      penimpaan
	 */
	public void setNominalManual(Double nominalManual) {
		this.nominalManual = nominalManual;
	}

}
