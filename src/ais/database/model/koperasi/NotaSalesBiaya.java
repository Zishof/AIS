package ais.database.model.koperasi;

import static javax.persistence.GenerationType.IDENTITY;

import java.math.BigDecimal;
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

import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;

/**
 * Biaya sesi sales lapangan (layar legacy 40-41, ERD &sect;3.8) -- kategori configurable
 * ({@link KategoriBiayaSales}), idempoten {@code kodeUnik} (retry offline P7 aman); biaya
 * TUNAI ikut menulis ledger {@link NotaSalesKas} EXPENSE_CASH. Koreksi = reversal, bukan hapus.
 *
 * <p><b>Satu-satunya dari trio dokumen sesi sales yang benar-benar dijurnal.</b> Dok 61 butir
 * E menemukan bahwa dari ketiga dokumen anak {@link NotaSalesSession} ({@link NotaSalesKas}
 * kontrol operasional, {@link NotaSalesPembelian} tautan faktur ber-jalur-posting-sendiri, dan
 * class ini), hanya BIAYA sesi sales yang tidak pernah menyentuh buku besar sama sekali --
 * sehingga beban operasional Laba Rugi lebih kecil daripada yang sesungguhnya terjadi. Celah
 * itu ditutup lewat migrasi r78666 yang menambahkan kolom {@link #postingHistory} pada class
 * ini dan mesin {@code ais.action.master.koperasi.helper.PostingBiayaSalesUtil} yang
 * menjurnal <b>Dr</b> akun kategori biaya ({@link KategoriBiayaSales#getAkun()}) / <b>Cr</b>
 * akun kas sesi sales dari Konfigurasi. Kategori yang belum ber-akun dilewati mesin tetapi
 * dokumennya tetap terhitung draf, sehingga kekurangan setup terlihat di dasbor Draft Jurnal.</p>
 *
 * <p><b>Koreksi via reversal, dijaga sesi CLOSED.</b> {@code SalesInventoryReversalHelper}
 * menolak reversal atas dokumen yang sesinya sudah {@link NotaSalesSession#STATUS_CLOSED}
 * ("snapshot penutupan tidak boleh berubah") -- koreksi atas biaya pada sesi yang sudah
 * ditutup harus lewat dokumen penyesuaian kantor terpisah, bukan lewat mekanisme reversal
 * kelas ini. Reversal juga menulis baris {@link NotaSalesKas#JENIS_REVERSAL} (nominal
 * POSITIF, kas kembali) bila dokumen asal bermetode {@link #METODE_TUNAI}.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "nota_sales_biaya")
public class NotaSalesBiaya extends GeneralValueObject {

	/**
	 * Nomor versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Biaya dibayar tunai -- ikut menulis satu baris {@link NotaSalesKas#JENIS_EXPENSE_CASH}
	 * ke ledger kas sesi (lihat {@link #getMetode()}). Default bila {@link #metode} belum diisi.
	 */
	public static final String METODE_TUNAI = "TUNAI";
	/** Biaya dibayar transfer -- TIDAK menulis baris ledger kas apa pun. */
	public static final String METODE_TRANSFER = "TRANSFER";

	/** Primary key baris {@code nota_sales_biaya}, di-generate DB (identity). */
	private Long id;

	/** Pengait wajib ke {@link NotaSalesSession} pemilik dokumen biaya ini. */
	private NotaSalesSession sesi;

	/**
	 * Pengait wajib ke {@link KategoriBiayaSales} -- sumber akun BEBAN sisi Dr saat dokumen
	 * ini dijurnal lewat {@code PostingBiayaSalesUtil} (lihat javadoc kelas).
	 */
	private KategoriBiayaSales kategori;

	/** Tanggal kejadian biaya (bukan tanggal input); dipakai sebagai tanggal jurnal saat diposting. */
	private Date tanggal;

	/** Uraian bebas biaya ini (mis. "bensin", "tol", "konsumsi"). */
	private String uraian;

	/** Nilai nominal biaya. Sisi Dr jurnal saat diposting. */
	private BigDecimal nilai;

	/** Metode pembayaran: {@link #METODE_TUNAI} (default) atau {@link #METODE_TRANSFER}. */
	private String metode;

	/** Nama penerima pembayaran biaya, bebas teks. */
	private String penerima;

	/** Nomor bukti/kuitansi fisik biaya, opsional. */
	private String nomorBukti;

	/**
	 * Kunci idempoten unik untuk retry aman pada koneksi lapangan yang tidak stabil (pola P7).
	 * Baris REVERSAL memakai konvensi {@code "REV-BIAYA-" + idAsal} sebagai kodeUnik-nya
	 * sendiri, sehingga permintaan reversal ganda atas dokumen yang sama juga idempoten.
	 */
	private String kodeUnik;

	/** Pengguna yang membuat/menginput dokumen biaya ini. */
	private Tbmuser dibuatOleh;
	// P10 reversal: biaya posted dibatalkan lewat baris pembalik bernilai negatif.
	/** Status dokumen: {@link #DOK_AKTIF}, {@link #DOK_DIBATALKAN}, atau {@link #DOK_REVERSAL}. */
	private String statusDok;

	/** Alasan reversal, diisi pada dokumen ASAL (bukan pada baris pembalik) saat direversal. */
	private String alasanReversal;

	/**
	 * Id dokumen asal yang dibalik oleh baris ini, diisi hanya pada baris ber-{@link
	 * #statusDok} {@link #DOK_REVERSAL}. BUKAN {@code @ManyToOne} -- disimpan sebagai id
	 * mentah ({@link Long}), bukan pengait entity langsung ke {@code NotaSalesBiaya} lain.
	 */
	private Long reversalDari;

	/** Dokumen aktif normal, belum pernah direversal. Status default bila {@link #statusDok} belum diisi. */
	public static final String DOK_AKTIF = "AKTIF";
	/** Dokumen ASAL yang sudah dibalik oleh sebuah baris {@link #DOK_REVERSAL}; tidak boleh direversal lagi. */
	public static final String DOK_DIBATALKAN = "DIBATALKAN";
	/** Baris pembalik (nilai negatif) yang dibuat saat sebuah dokumen {@link #DOK_AKTIF} direversal. */
	public static final String DOK_REVERSAL = "REVERSAL";

	/**
	 * Hook Hibernate {@code @PreUpdate}: dipanggil otomatis sebelum setiap UPDATE terhadap
	 * baris ini, mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk mengisi
	 * ulang {@link #tanggal_dirubah}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor default (dibutuhkan Hibernate/JPA), tidak menginisialisasi field apa pun. */
	public NotaSalesBiaya() {
	}

	/**
	 * Mengambil primary key baris {@code NotaSalesBiaya} ini.
	 *
	 * @return {@link #id}, {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi primary key secara manual.
	 *
	 * @param id primary key yang ingin diset pada objek in-memory
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil {@link NotaSalesSession} pemilik dokumen biaya ini.
	 *
	 * @return {@link #sesi}, seharusnya tidak pernah {@code null} pada baris yang sudah
	 *         tersimpan ({@code nullable = false} pada kolom {@code sesi})
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sesi", nullable = false)
	public NotaSalesSession getSesi() {
		return sesi;
	}

	/**
	 * Mengisi pengait sesi pemilik dokumen biaya ini.
	 *
	 * @param sesi sesi baru
	 */
	public void setSesi(NotaSalesSession sesi) {
		this.sesi = sesi;
	}

	/**
	 * Mengambil {@link KategoriBiayaSales} dokumen biaya ini.
	 *
	 * @return {@link #kategori}, seharusnya tidak pernah {@code null} pada baris yang sudah
	 *         tersimpan ({@code nullable = false} pada kolom {@code kategori})
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kategori", nullable = false)
	public KategoriBiayaSales getKategori() {
		return kategori;
	}

	/**
	 * Mengisi pengait kategori biaya. Kategori ini menentukan akun BEBAN sisi Dr saat
	 * dokumen dijurnal (lihat javadoc kelas), sehingga mengubah kategori pada dokumen yang
	 * sudah pernah diposting ({@link #postingHistory} terisi) TIDAK akan menjurnal ulang
	 * baris yang sudah lama diposting -- hanya memengaruhi posting berikutnya bila dokumen
	 * ini sempat dibatalkan postingnya.
	 *
	 * @param kategori kategori baru
	 */
	public void setKategori(KategoriBiayaSales kategori) {
		this.kategori = kategori;
	}

	/**
	 * Mengambil tanggal kejadian biaya, diinisialisasi lazy bila belum pernah diisi.
	 *
	 * @return {@link #tanggal} bila sudah diisi; {@link ais.ui.util.WaktuUtil#getDate()}
	 *         (waktu saat ini) bila belum -- TIDAK PERNAH mengembalikan {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal", nullable = false)
	public Date getTanggal() {
		return tanggal == null ? ais.ui.util.WaktuUtil.getDate() : tanggal;
	}

	/**
	 * Mengisi tanggal kejadian biaya. Nilai ini dipakai {@code PostingBiayaSalesUtil} sebagai
	 * tanggal jurnal saat dokumen diposting -- BUKAN tanggal saat dokumen diinput/disimpan.
	 *
	 * @param tanggal tanggal baru
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengambil uraian bebas biaya ini.
	 *
	 * @return {@link #uraian} apa adanya, bisa {@code null}
	 */
	@Column(name = "uraian", columnDefinition = "text")
	public String getUraian() {
		return uraian;
	}

	/**
	 * Mengisi uraian bebas biaya ini.
	 *
	 * @param uraian teks uraian baru
	 */
	public void setUraian(String uraian) {
		this.uraian = uraian;
	}

	/**
	 * Mengambil nilai nominal biaya, dengan default null-safe.
	 *
	 * @return {@link #nilai}, atau {@link BigDecimal#ZERO} bila belum diisi
	 */
	@Column(name = "nilai", precision = 19, scale = 2)
	public BigDecimal getNilai() {
		return nilai == null ? BigDecimal.ZERO : nilai;
	}

	/**
	 * Mengisi nilai nominal biaya. {@code PostingBiayaSalesUtil} melewati (skip) dokumen
	 * dengan nilai {@code null} atau nol saat posting massal -- dokumen tersebut tetap
	 * berstatus draf tanpa error.
	 *
	 * @param nilai nilai baru
	 */
	public void setNilai(BigDecimal nilai) {
		this.nilai = nilai;
	}

	/**
	 * Mengambil metode pembayaran biaya, dengan default null-safe.
	 *
	 * @return {@link #metode} apa adanya bila sudah diisi dan tidak blank; {@link
	 *         #METODE_TUNAI} bila {@code null} atau kosong/spasi saja
	 */
	@Column(name = "metode", length = 20)
	public String getMetode() {
		return metode == null || metode.trim().isEmpty() ? METODE_TUNAI : metode;
	}

	/**
	 * Mengisi metode pembayaran biaya secara langsung, tanpa validasi terhadap {@link
	 * #METODE_TUNAI}/{@link #METODE_TRANSFER}. Nilai ini menentukan apakah kode pemanggil
	 * ({@code SalesInventoryTripHelper.expenseCreate}) ikut menulis baris ledger
	 * {@link NotaSalesKas#JENIS_EXPENSE_CASH} -- setter ini sendiri TIDAK memicu penulisan
	 * ledger, efek samping itu ada di lapisan pemanggil, bukan di entity ini.
	 *
	 * @param metode nilai baru
	 */
	public void setMetode(String metode) {
		this.metode = metode;
	}

	/**
	 * Mengambil nama penerima pembayaran biaya.
	 *
	 * @return {@link #penerima} apa adanya, bisa {@code null}
	 */
	@Column(name = "penerima", length = 120)
	public String getPenerima() {
		return penerima;
	}

	/**
	 * Mengisi nama penerima pembayaran biaya.
	 *
	 * @param penerima teks nama baru
	 */
	public void setPenerima(String penerima) {
		this.penerima = penerima;
	}

	/**
	 * Mengambil nomor bukti/kuitansi fisik biaya.
	 *
	 * @return {@link #nomorBukti} apa adanya, bisa {@code null}
	 */
	@Column(name = "nomor_bukti", length = 80)
	public String getNomorBukti() {
		return nomorBukti;
	}

	/**
	 * Mengisi nomor bukti/kuitansi fisik biaya.
	 *
	 * @param nomorBukti nomor bukti baru
	 */
	public void setNomorBukti(String nomorBukti) {
		this.nomorBukti = nomorBukti;
	}

	/**
	 * Mengambil kunci idempoten unik dokumen ini.
	 *
	 * @return {@link #kodeUnik} apa adanya, bisa {@code null} pada baris yang dibuat sebelum
	 *         pola idempotensi ini diterapkan
	 */
	@Column(name = "kode_unik", length = 80, unique = true)
	public String getKodeUnik() {
		return kodeUnik;
	}

	/**
	 * Mengisi kunci idempoten unik dokumen ini. Kolom {@code unique = true} pada DB --
	 * penyimpanan dengan {@code kodeUnik} yang sudah dipakai baris lain (termasuk baris
	 * reversal ber-konvensi {@code "REV-BIAYA-" + id}) akan gagal dengan
	 * {@code ConstraintViolationException}, ditangkap kode pemanggil sebagai sinyal replay
	 * idempoten.
	 *
	 * @param kodeUnik kunci idempoten baru
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Mengambil pengguna yang membuat/menginput dokumen biaya ini.
	 *
	 * @return {@link #dibuatOleh}, boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh")
	public Tbmuser getDibuatOleh() {
		return dibuatOleh;
	}

	/**
	 * Mengisi pengguna yang membuat dokumen biaya ini.
	 *
	 * @param dibuatOleh pengguna baru
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengambil status dokumen, dengan default null-safe.
	 *
	 * @return {@link #statusDok} apa adanya bila sudah diisi dan tidak blank; {@link
	 *         #DOK_AKTIF} bila {@code null} atau kosong/spasi saja
	 */
	@Column(name = "status_dok", length = 20)
	public String getStatusDok() {
		return statusDok == null || statusDok.trim().isEmpty() ? DOK_AKTIF : statusDok;
	}

	/**
	 * Mengisi status dokumen secara langsung, tanpa validasi transisi apa pun di level
	 * entity ini -- penjagaan alur reversal (mis. hanya dokumen {@link #DOK_AKTIF} yang
	 * boleh direversal, dokumen sesi {@code CLOSED} ditolak direversal) dilakukan di
	 * {@code SalesInventoryReversalHelper}, bukan di sini.
	 *
	 * @param statusDok nilai status baru, sebaiknya salah satu konstanta {@code DOK_*}
	 */
	public void setStatusDok(String statusDok) {
		this.statusDok = statusDok;
	}

	/**
	 * Mengambil alasan reversal dokumen ini (terisi pada dokumen ASAL saat dibatalkan).
	 *
	 * @return {@link #alasanReversal} apa adanya, bisa {@code null} bila dokumen belum
	 *         pernah direversal
	 */
	@Column(name = "alasan_reversal", columnDefinition = "text")
	public String getAlasanReversal() {
		return alasanReversal;
	}

	/**
	 * Mengisi alasan reversal dokumen ini.
	 *
	 * @param alasanReversal teks alasan baru
	 */
	public void setAlasanReversal(String alasanReversal) {
		this.alasanReversal = alasanReversal;
	}

	/**
	 * Mengambil id dokumen asal yang dibalik oleh baris ini.
	 *
	 * @return {@link #reversalDari}, terisi hanya pada baris ber-{@link #statusDok}
	 *         {@link #DOK_REVERSAL}; {@code null} pada dokumen normal
	 */
	@Column(name = "reversal_dari")
	public Long getReversalDari() {
		return reversalDari;
	}

	/**
	 * Mengisi id dokumen asal yang dibalik baris reversal ini.
	 *
	 * @param reversalDari id dokumen asal baru
	 */
	public void setReversalDari(Long reversalDari) {
		this.reversalDari = reversalDari;
	}

	/**
	 * Mengambil timestamp perubahan terakhir baris ini.
	 *
	 * @return {@link #tanggal_dirubah}, tidak pernah {@code null} setelah objek dikonstruksi
	 *         (inisialisasi eager pada deklarasi field, diperbarui otomatis oleh
	 *         {@link #onUpdate()} pada setiap UPDATE)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Mengisi timestamp perubahan terakhir secara manual. Umumnya tidak perlu dipanggil
	 * langsung karena {@link #onUpdate()} sudah mengisinya otomatis setiap UPDATE.
	 *
	 * @param tanggal_dirubah timestamp baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Pengait ke {@link ais.database.model.akunting.PostingHistory} riwayat posting jurnal
	 * biaya sesi sales ini (dok 61 butir E, ditambahkan lewat migrasi r78666). {@code null}
	 * berarti dokumen ini masih DRAF -- belum pernah dijurnal oleh
	 * {@code PostingBiayaSalesUtil.postingSemua}, entah karena belum diproses ataupun karena
	 * {@link #kategori} dokumen ini belum ber-akun (dilewati mesin, lihat javadoc kelas).
	 */
	private ais.database.model.akunting.PostingHistory postingHistory;

	/**
	 * Riwayat posting jurnal biaya sesi sales (dok 61 butir E): terisi begitu mesin
	 * {@code PostingBiayaSalesUtil} menjurnalkan biaya ini.
	 *
	 * @return {@link #postingHistory}, {@code null} bila dokumen masih draf (belum diposting)
	 */
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "posting_history", nullable = true)
	public ais.database.model.akunting.PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * Mengisi pengait riwayat posting jurnal. Dipanggil {@code PostingBiayaSalesUtil} saat
	 * dokumen berhasil dijurnal (diisi dengan {@link ais.database.model.akunting.PostingHistory}
	 * baru) dan saat posting dibatalkan lewat {@code batalkanPostingSemua} (diisi kembali
	 * dengan {@code null}, mengembalikan dokumen ke status draf).
	 *
	 * @param postingHistory riwayat posting baru, atau {@code null} untuk mengembalikan
	 *        dokumen ke status draf
	 */
	public void setPostingHistory(ais.database.model.akunting.PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

}
