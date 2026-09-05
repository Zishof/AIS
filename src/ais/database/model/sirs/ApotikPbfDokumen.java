package ais.database.model.sirs;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

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
import javax.persistence.UniqueConstraint;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.PostingHistory;

/**
 * Dokumen sumber penerimaan obat dari PBF; terpisah penuh dari Kulakan/Kantin.
 *
 * <h3>Apa yang diwakili satu baris</h3>
 *
 * <p>PBF adalah Pedagang Besar Farmasi — distributor resmi yang memasok obat ke
 * apotek. Satu baris di sini adalah satu faktur penerimaan: obat sudah masuk
 * gudang apotek, dan sejak saat itu apotek berutang kepada distributor sebesar
 * {@link #getTotal()}. Utang itu dicicil lewat {@link ApotikPbfPembayaran}, dan
 * sisanya adalah total ini dikurangi jumlah seluruh cicilannya.</p>
 *
 * <p>Baris ini adalah induk dokumen, bukan rinciannya. Barang yang benar-benar
 * masuk dicatat sebagai baris ledger stok dan — bila fakturnya menyertakan
 * tanggal kedaluwarsa — sebagai batch {@link Kadaluarsa}. Yang tersimpan di sini
 * hanyalah ringkasannya: berapa totalnya, berapa banyak barisnya, dari siapa,
 * dan tanggal berapa. Ringkasan itu ditulis sekali oleh
 * {@code ApotikPersediaanHelper} dan TIDAK pernah dihitung ulang dari rinciannya
 * — akibatnya dijelaskan pada {@link #getTotal()} dan
 * {@link #getJumlahBaris()}.</p>
 *
 * <h3>Terpisah penuh dari Kulakan/Kantin — dan mengapa</h3>
 *
 * <p>Modul kantin sudah punya alur kulakan dengan bentuk yang mirip, dan
 * memakainya ulang akan terasa hemat. Pemisahan dipilih karena obat bukan barang
 * dagangan biasa: penerimaannya menyangkut batch dan tanggal kedaluwarsa yang
 * harus ditelusuri per lot, sebagian di antaranya menuntut rantai dingin
 * ({@link ApotikPenerimaanSuhu}), dan utangnya dibukukan ke akun tersendiri
 * ({@link ApotikAkunMapping#UTANG_PBF}) yang berbeda dari utang kantin. Menempel
 * pada alur yang sudah ada akan memaksa salah satu dari dua hal: menambah kolom
 * khusus obat ke entity yang dipakai bersama, atau membiarkan penerimaan obat
 * kehilangan sifat-sifat itu. Keduanya lebih mahal daripada tabel terpisah.</p>
 *
 * <h3>Penanda idempoten posting</h3>
 *
 * <p>Berbeda dari transaksi penjualan yang memakai tabel penanda terpisah
 * ({@link ApotikPostingLink}), dokumen PBF memakai kolom
 * {@link #getPostingHistory()} langsung. Perbedaan itu wajar: satu penjualan
 * melahirkan dua jurnal berdiri sendiri sehingga butuh penanda per jenis,
 * sedangkan satu dokumen PBF hanya melahirkan satu jurnal penerimaan.</p>
 *
 * @see ApotikPbfPembayaran cicilan atas utang dokumen ini, lengkap dengan penjaga anti-lebih-bayar
 * @see ApotikPenerimaanSuhu bukti suhu penerimaan barang rantai dingin pada faktur yang sama
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "apotik_pbf_dokumen",
        uniqueConstraints = @UniqueConstraint(columnNames = { "kode" }))
public class ApotikPbfDokumen extends GeneralValueObject {

    /** Versi serialisasi; tetap 1 selama bentuk field tidak berubah maknanya. */
    private static final long serialVersionUID = 1L;

    /** Kunci baris; dibangkitkan basis data. */
    private Long id;

    /** Kode dokumen internal apotek; unik di seluruh tabel. */
    private String kode;

    /** Nomor faktur menurut distributor; TIDAK dijaga unik. */
    private String noFaktur;

    /** Nama distributor; teks bebas, bukan relasi ke master. */
    private String penyedia;

    /** Tanggal dokumen; penentu penjaringan periode pada layar posting. */
    private Date tanggal;

    /** Nilai utang yang timbul; ringkasan yang tidak pernah dihitung ulang. */
    private Double total;

    /** Banyaknya baris barang pada faktur; ringkasan informatif. */
    private Integer jumlahBaris;

    /** Catatan bebas. */
    private String keterangan;

    /** Jurnal penerimaan yang sudah terbentuk; kosong berarti belum diposting. */
    private PostingHistory postingHistory;

    /** Nama tampil pelaku pencatatan (bayangan audit). */
    private String oleh;

    /** Identitas akun pelaku pencatatan (bayangan audit). */
    private String olehId;

    /** Stempel ubah terakhir; disegarkan interseptor audit pada setiap UPDATE. */
    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

    /**
     * Kunci baris, dibangkitkan basis data ({@code IDENTITY}).
     *
     * <p>{@code insertable = false}: nilai apa pun di objek Java tidak ikut
     * dalam INSERT.</p>
     *
     * @return kunci baris, atau {@code null} bila belum tersimpan
     */
    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return id; }

    /**
     * Menetapkan kunci baris; dipakai Hibernate, bukan kode aplikasi.
     *
     * @param id kunci baris
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Kode dokumen internal apotek — penanda yang diucapkan orang.
     *
     * <p>Dijaga batasan unik di tingkat tabel. Penjagaan itu bukan kerapian
     * belaka: kode dokumen inilah yang muncul sebagai referensi pada layar
     * posting dan pada jurnal yang terbentuk ("Penerimaan PBF Apotik
     * &lt;kode&gt;"), dan dua dokumen berkode sama membuat penelusuran dari
     * jurnal kembali ke fakturnya bercabang tanpa cara memilih. Untuk dokumen
     * utang, ketidakpastian semacam itu berarti dua tagihan yang berbeda dapat
     * dikira satu.</p>
     *
     * <p>Perhatikan bahwa keunikan hanya dijaga basis data; tidak ada
     * pemeriksaan di entity ini maupun sebelum penyimpanan. Duplikat akan
     * ditolak pada saat {@code flush} dalam bentuk pengecualian, bukan pesan
     * yang enak dibaca. Pembuat kode wajib bersiap menanganinya.</p>
     *
     * @return kode dokumen; tidak boleh {@code null} pada baris tersimpan
     */
    @Column(name = "kode", nullable = false, length = 80)
    public String getKode() { return kode; }

    /**
     * Menetapkan kode dokumen internal.
     *
     * @param kode kode dokumen
     */
    public void setKode(String kode) { this.kode = kode; }

    /**
     * Nomor faktur menurut distributor.
     *
     * <p>Berbeda dari {@link #getKode()}, kolom ini TIDAK dijaga unik dan boleh
     * kosong. Kelonggaran itu perlu disadari akibatnya: faktur distributor yang
     * sama dapat dimasukkan dua kali sebagai dua dokumen berkode internal
     * berbeda, dan hasilnya adalah utang ganda kepada distributor yang sama —
     * dua dokumen yang masing-masing tampak sah, masing-masing dapat dicicil
     * sampai lunas, dan masing-masing menghasilkan jurnal utangnya sendiri.
     * Tidak ada satu pun pemeriksaan di lapisan mana pun yang akan
     * mengeluh.</p>
     *
     * <p>Penjaga anti-lebih-bayar yang ada di {@code pbfBayar} tidak menolong di
     * sini: ia menjaga agar cicilan tidak melebihi total SATU dokumen, bukan
     * agar dokumen tidak berjumlah lebih dari yang seharusnya. Kalau kelak
     * dirasa perlu, penjagaan yang tepat berbentuk pemeriksaan kembar atas
     * pasangan penyedia dan nomor faktur pada saat penerimaan dicatat — bukan
     * batasan unik atas kolom ini sendiri, sebab nomor faktur boleh kosong dan
     * distributor yang berbeda dapat memakai penomoran yang bertabrakan.</p>
     *
     * <p>Nomor ini juga yang menghubungkan dokumen dengan bukti suhunya:
     * {@link ApotikPenerimaanSuhu#getNoFaktur()} menyalin nilai yang sama,
     * sebagai teks, tanpa relasi keras.</p>
     *
     * @return nomor faktur distributor, atau {@code null}
     */
    @Column(name = "no_faktur", length = 100)
    public String getNoFaktur() { return noFaktur; }

    /**
     * Menetapkan nomor faktur distributor.
     *
     * @param noFaktur nomor faktur
     */
    public void setNoFaktur(String noFaktur) { this.noFaktur = noFaktur; }

    /**
     * Nama distributor pemasok.
     *
     * <p>Teks bebas, bukan relasi ke master pemasok mana pun. Akibatnya
     * distributor yang sama dapat tercatat dengan beberapa penulisan, dan
     * laporan utang per distributor harus mengelompokkan teks yang tidak
     * dinormalkan. Untuk apotek yang memasok dari banyak PBF, itu persoalan
     * yang tumbuh seiring waktu.</p>
     *
     * @return nama distributor, atau {@code null}
     */
    @Column(name = "penyedia", length = 200)
    public String getPenyedia() { return penyedia; }

    /**
     * Menetapkan nama distributor.
     *
     * @param penyedia nama distributor
     */
    public void setPenyedia(String penyedia) { this.penyedia = penyedia; }

    /**
     * Tanggal dokumen penerimaan.
     *
     * <p>Menentukan dua hal sekaligus. Pertama, penjaringan periode pada layar
     * posting: {@code ApotikPbfPostingHelper} menjaring kandidat dengan
     * {@code date(d.tanggal) BETWEEN date(?) AND date(?)}, sehingga dokumen di
     * luar rentang yang dipilih petugas tidak akan pernah muncul sebagai
     * kandidat — luput tanpa pesan kesalahan, ia hanya tidak tampil. Kedua,
     * tanggal jurnal yang terbentuk: helper menyalin nilai ini ke
     * {@code PostingHistory.setTanggal} dan {@code setTanggalPosting}, sehingga
     * dokumen bertanggal periode lama akan membentuk jurnal di periode lama
     * pula meski dipostingnya hari ini.</p>
     *
     * @return tanggal dokumen
     */
    @Temporal(TemporalType.TIMESTAMP) @Column(name = "tanggal", nullable = false)
    public Date getTanggal() { return tanggal; }

    /**
     * Menetapkan tanggal dokumen.
     *
     * @param tanggal tanggal dokumen
     */
    public void setTanggal(Date tanggal) { this.tanggal = tanggal; }

    /**
     * Nilai utang yang timbul dari penerimaan ini.
     *
     * <p>Mengembalikan {@code 0} bila kosong, sehingga perhitungan sisa utang
     * dan penjumlahan di layar tidak perlu berjaga terhadap {@code null}.</p>
     *
     * <p><b>Ringkasan yang dihitung sekali, tidak pernah dihitung ulang.</b>
     * {@code ApotikPersediaanHelper} menjumlahkan {@code qty * harga_beli}
     * seluruh baris yang dikirim, menyimpan hasilnya di sini, lalu tidak pernah
     * menyentuhnya lagi. Rincian barangnya disimpan sebagai baris ledger stok
     * terpisah, dan tidak ada apa pun yang membandingkan kedua sisi itu
     * belakangan. Bila baris ledger disunting, ditambah, atau dihapus, angka di
     * sini akan tetap seperti semula dan menjadi salah tanpa tanda apa pun.</p>
     *
     * <p>Ini bukan cacat yang perlu segera diperbaiki — total faktur memang
     * merupakan angka yang disepakati dengan distributor dan tidak seharusnya
     * bergeser mengikuti koreksi gudang. Yang perlu diketahui adalah bahwa
     * kolom ini BUKAN turunan dari rinciannya, sehingga siapa pun yang
     * membangun laporan rekonsiliasi tidak boleh mengandaikan keduanya
     * cocok.</p>
     *
     * <p>Angka ini pula yang menjadi dasar dua hal berikutnya: nilai jurnal
     * penerimaan (persediaan didebit, utang PBF dikreditkan), dan batas atas
     * penjaga anti-lebih-bayar di {@code pbfBayar} yang menolak cicilan
     * melebihi total ini dikurangi yang sudah dibayar. Karena itu total yang
     * salah tidak berhenti sebagai catatan yang keliru — ia menaikkan atau
     * menurunkan berapa banyak uang yang boleh keluar untuk dokumen ini.</p>
     *
     * @return nilai dokumen; {@code 0} bila kosong
     */
    @Column(name = "total", nullable = false)
    public Double getTotal() { return total == null ? Double.valueOf(0) : total; }

    /**
     * Menetapkan nilai dokumen.
     *
     * @param total nilai dokumen
     */
    public void setTotal(Double total) { this.total = total; }

    /**
     * Banyaknya baris barang pada faktur.
     *
     * <p>Mengembalikan {@code 0} bila kosong. Ringkasan informatif belaka —
     * berlaku catatan yang sama dengan {@link #getTotal()}: dihitung sekali dari
     * banyaknya baris yang dikirim, tidak pernah dihitung ulang, dan tidak
     * pernah dibandingkan dengan cacah baris ledger yang sebenarnya ada.
     * Berbeda dari total, kolom ini tidak dipakai perhitungan apa pun yang
     * mengikat, sehingga ketidakcocokannya hanya menyesatkan pembaca layar.</p>
     *
     * @return cacah baris faktur; {@code 0} bila kosong
     */
    @Column(name = "jumlah_baris", nullable = false)
    public Integer getJumlahBaris() { return jumlahBaris == null ? Integer.valueOf(0) : jumlahBaris; }

    /**
     * Menetapkan banyaknya baris barang.
     *
     * @param jumlahBaris cacah baris
     */
    public void setJumlahBaris(Integer jumlahBaris) { this.jumlahBaris = jumlahBaris; }

    /**
     * Catatan bebas tentang dokumen.
     *
     * @return keterangan, atau {@code null}
     */
    @Column(name = "keterangan", length = 500)
    public String getKeterangan() { return keterangan; }

    /**
     * Menetapkan catatan bebas.
     *
     * @param keterangan keterangan
     */
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

    /**
     * Jurnal penerimaan yang sudah terbentuk; kosong berarti belum diposting.
     *
     * <p>Getter DESTRUKTIF: hasil {@code check(...)} ditulis balik ke field.
     * {@code check} menormalkan proksi malas Hibernate yang sudah lepas dari
     * sesinya menjadi {@code null}, mencegah {@code LazyInitializationException}
     * saat objek dibaca di luar sesi. Memanggilnya dapat mengubah keadaan
     * objek.</p>
     *
     * <p>Penanda idempoten berlapis dua, sama seperti pada
     * {@link ApotikPbfPembayaran#getPostingHistory()}: daftar kandidat
     * menjaring dengan {@code posting_history IS NULL}, dan {@code postingSatu}
     * memeriksanya sekali lagi di dalam transaksinya sendiri lalu me-rollback
     * bila ternyata sudah terisi. Yang kedua itulah yang menahan dua petugas
     * yang menekan posting bersamaan.</p>
     *
     * <p>Terisinya kolom ini tidak mengunci baris. {@link #setTotal(Double)}
     * tetap dapat dipanggil setelah jurnal terbentuk, dan jurnalnya tidak ikut
     * berubah — untuk saat ini tidak ada jalur aplikasi yang menyunting dokumen
     * setelah tersimpan, sehingga keadaan itu belum dapat terjadi.</p>
     *
     * @return jurnal penerimaan, atau {@code null} bila belum diposting
     */
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "posting_history")
    public PostingHistory getPostingHistory() { postingHistory = check(postingHistory); return postingHistory; }

    /**
     * Menetapkan jurnal penerimaan.
     *
     * <p>Hanya dipanggil {@code ApotikPbfPostingHelper.postingSatu} sesudah
     * jurnalnya benar-benar tersimpan, di dalam transaksi yang sama.
     * Memanggilnya dari tempat lain akan menandai dokumen sebagai
     * sudah-diposting padahal jurnalnya tidak ada, dan dokumen itu tidak akan
     * pernah terjaring lagi — utangnya tidak pernah masuk buku.</p>
     *
     * @param postingHistory jurnal hasil posting
     */
    public void setPostingHistory(PostingHistory postingHistory) { this.postingHistory = postingHistory; }

    /**
     * Nama tampil pelaku pencatatan (bayangan audit).
     *
     * @return nama pelaku, atau {@code null}
     */
    @Column(name = "oleh", length = 60)
    public String getOleh() { return oleh; }

    /**
     * Menetapkan nama pelaku pencatatan.
     *
     * <p>Menetapkan apa adanya, termasuk nilai kosong — berbeda dari entity
     * tetangga seperti {@link ApotikPostingLink#setOleh(String)} yang
     * menolaknya. Untuk dokumen PBF, jejak pelaku yang lebih kuat tetap
     * tersimpan Envers di {@code new_audit.apotik_pbf_dokumen__audit}, dan
     * pemanggil mengisinya dari sesi ({@code auditActor}) saat baris
     * dibuat.</p>
     *
     * @param oleh nama pelaku
     */
    public void setOleh(String oleh) { this.oleh = oleh; }

    /**
     * Identitas akun pelaku pencatatan (bayangan audit).
     *
     * @return id akun pelaku, atau {@code null}
     */
    @Column(name = "oleh_id", length = 60)
    public String getOlehId() { return olehId; }

    /**
     * Menetapkan id akun pelaku pencatatan.
     *
     * <p>Berlaku catatan pada {@link #setOleh(String)}.</p>
     *
     * @param olehId id akun pelaku
     */
    public void setOlehId(String olehId) { this.olehId = olehId; }

    /**
     * Stempel perubahan terakhir.
     *
     * <p>Bergerak sekali dalam daur hidup normal dokumen, yaitu ketika
     * {@link #setPostingHistory(PostingHistory)} menandainya sudah
     * diposting.</p>
     *
     * @return waktu ubah terakhir
     */
    @Temporal(TemporalType.TIMESTAMP) public Date getTanggal_dirubah() { return tanggal_dirubah; }

    /**
     * Menetapkan stempel perubahan terakhir.
     *
     * @param tanggal_dirubah waktu ubah
     */
    public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }

    /**
     * Kait JPA sebelum UPDATE: menyegarkan {@link #getTanggal_dirubah()}.
     *
     * <p>Berjalan lewat {@code AuditTimestampInterceptor.ubah(this)} agar semua
     * entity memakai satu sumber waktu yang sama; tidak berjalan pada INSERT.</p>
     */
    @javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }
}
