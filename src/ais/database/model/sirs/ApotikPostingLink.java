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
 * Penanda idempoten satu transaksi Apotik terhadap satu jenis jurnal.
 *
 * <h3>Persoalan yang dipecahkan</h3>
 *
 * <p>Posting jurnal dijalankan per periode: petugas memilih rentang tanggal, layar
 * menampilkan transaksi yang belum berjurnal, dan petugas menekan terapkan. Yang
 * berbahaya pada bentuk kerja itu adalah pengulangan — layar yang sama dibuka
 * kembali besok dengan rentang yang bertumpang tindih, atau tombol yang ditekan
 * dua kali karena halaman terasa lambat. Tanpa penanda, transaksi yang sama akan
 * menghasilkan jurnal kedua, dan pendapatan apotek terbukukan dua kali. Kesalahan
 * semacam itu tidak menampakkan diri: kedua jurnal seimbang, keduanya tampak
 * wajar, dan yang salah hanyalah jumlahnya.</p>
 *
 * <p>Entity ini adalah penandanya. Satu baris berarti: transaksi X sudah pernah
 * dibukukan untuk jenis jurnal Y, inilah jurnalnya. Selama baris itu ada,
 * {@code ApotikPostingHelper} melewati transaksi tersebut.</p>
 *
 * <h3>Mengapa tabel penanda terpisah, bukan kolom pada transaksi</h3>
 *
 * <p>Satu transaksi penjualan apotek melahirkan LEBIH DARI SATU jurnal:
 * {@link #PENJUALAN} mencatat pendapatan dan piutang/kas, {@link #HPP} mencatat
 * harga pokok dan pengurangan persediaan. Keduanya berdiri sendiri — satu bisa
 * sudah terbentuk sementara yang lain gagal karena akunnya belum dipetakan.
 * Sebuah kolom {@code posting_history} tunggal pada {@code TransaksiMedis} tidak
 * dapat menyatakan keadaan setengah-jalan itu; ia hanya dapat mengatakan "sudah"
 * atau "belum", dan salah satu dari keduanya pasti berbohong.</p>
 *
 * <p>Alasan kedua sama pentingnya: {@code TransaksiMedis} adalah entity milik
 * modul rumah sakit yang dipakai bersama dan sudah {@code @Audited}. Menambah
 * kolom di sana menuntut perubahan pada tabel audit lamanya, dan bila tabel audit
 * tidak ikut diubah maka setiap INSERT audit gagal dan seluruh transaksi
 * ter-rollback — gotcha Envers yang sudah berulang di basis kode ini. Tabel baru
 * seperti ini terbentuk utuh berikut tabel auditnya sekaligus.</p>
 *
 * <h3>Penjaga sesungguhnya ada di basis data</h3>
 *
 * <p>Batasan unik atas pasangan kolom {@code (transaksi, jenis)} adalah inti
 * entity ini, bukan hiasan. Pemeriksaan "apakah sudah ada penanda" yang
 * dikerjakan kode aplikasi selalu punya jeda antara membaca dan menulis; dua
 * proses yang memposting bersamaan dapat sama-sama membaca "belum ada" dan
 * sama-sama menulis. Batasan unik di basis data tidak punya jeda itu: yang kedua
 * ditolak, apa pun yang terjadi di atasnya. Pemeriksaan di kode aplikasi tetap
 * berguna untuk memberi pesan yang enak dibaca, tetapi yang benar-benar
 * mencegah jurnal ganda adalah batasan ini.</p>
 *
 * <p><b>Jangan melepasnya.</b> Bila suatu saat batasan itu terasa menghalangi —
 * misalnya karena satu transaksi ingin diposting ulang setelah dibatalkan —
 * jawabannya bukan menghapus batasan, melainkan menghapus baris penandanya
 * secara sadar sebagai bagian dari pembatalan jurnal. Melepas batasan
 * mengembalikan celah balapan yang tidak akan terlihat sampai laporan keuangan
 * tidak cocok.</p>
 *
 * @see ApotikAkunMapping pemetaan peran akun yang menentukan jurnal mana yang dapat terbentuk
 * @see ApotikPbfDokumen dokumen PBF yang memakai kolom posting langsung, bukan tabel penanda
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "apotik_posting_link",
        uniqueConstraints = @UniqueConstraint(columnNames = { "transaksi", "jenis" }))
public class ApotikPostingLink extends GeneralValueObject {

    /** Versi serialisasi; tetap 1 selama bentuk field tidak berubah maknanya. */
    private static final long serialVersionUID = 1L;

    /**
     * Jenis jurnal: pengakuan pendapatan penjualan apotek.
     *
     * <p>Nilainya menjadi separuh dari kunci unik bersama {@code transaksi},
     * sehingga menambah jenis baru berarti membuka satu jurnal tambahan per
     * transaksi. Jangan mengubah teks konstanta yang sudah ada: nilainya
     * tersimpan di kolom {@code jenis} pada baris-baris lama, dan mengubahnya
     * membuat penanda lama tidak lagi cocok — seluruh transaksi yang sudah
     * diposting akan tampak belum diposting dan terjaring untuk diposting
     * ulang.</p>
     */
    public static final String PENJUALAN = "PENJUALAN";

    /** Jenis jurnal: harga pokok penjualan dan pengurangan persediaan. */
    public static final String HPP = "HPP";

    /** Kunci baris; dibangkitkan basis data. */
    private Long id;

    /** Transaksi apotek yang ditandai sudah berjurnal. Wajib. */
    private TransaksiMedis transaksi;

    /** {@link #PENJUALAN} atau {@link #HPP}; separuh kunci unik. */
    private String jenis;

    /** Jurnal yang terbentuk. Wajib — penanda tanpa jurnal tidak punya arti. */
    private PostingHistory postingHistory;

    /** Nilai yang dibukukan; salinan untuk rekonsiliasi. */
    private Double nilai;

    /** Waktu penandaan dibuat. */
    private Date waktu;

    /** Nama tampil pelaku posting (bayangan audit). */
    private String oleh;

    /** Identitas akun pelaku posting (bayangan audit). */
    private String olehId;

    /** Stempel ubah terakhir; disegarkan interseptor audit pada setiap UPDATE. */
    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

    /**
     * Kunci baris, dibangkitkan basis data ({@code IDENTITY}).
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
     * Transaksi apotek yang ditandai sudah berjurnal.
     *
     * <p>Getter DESTRUKTIF: hasil {@code check(...)} ditulis balik ke field.
     * {@code check} menormalkan proksi malas Hibernate yang sudah lepas dari
     * sesinya menjadi {@code null} alih-alih membiarkan
     * {@code LazyInitializationException} meledak. Memanggilnya dapat mengubah
     * keadaan objek.</p>
     *
     * <p>Bersama {@link #getJenis()} membentuk kunci unik tabel ini. Karena
     * relasi ini {@code nullable = false} dan {@code jenis} juga, tidak ada
     * baris penanda yang mengambang tanpa menunjuk apa pun — dan itu penting,
     * sebab satu baris tanpa transaksi akan menempati satu slot kunci unik
     * tanpa melindungi apa pun.</p>
     *
     * @return transaksi yang ditandai, atau {@code null} bila proksinya lepas
     */
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "transaksi", nullable = false)
    public TransaksiMedis getTransaksi() { transaksi = check(transaksi); return transaksi; }

    /**
     * Menetapkan transaksi yang ditandai.
     *
     * @param transaksi transaksi apotek; wajib terisi sebelum disimpan
     */
    public void setTransaksi(TransaksiMedis transaksi) { this.transaksi = transaksi; }

    /**
     * Jenis jurnal yang ditandai; {@link #PENJUALAN} atau {@link #HPP}.
     *
     * <p>Separuh kunci unik. Nilai di luar kedua konstanta tidak ditolak entity
     * maupun basis data — kolomnya sekadar teks sepanjang 24 karakter. Nilai
     * salah tulis akan membentuk penanda yang tidak pernah cocok dengan
     * pemeriksaan pemanggil, sehingga transaksinya tampak belum diposting dan
     * dapat berjurnal ganda. Selalu pakai konstanta, jangan menuliskan teksnya
     * ulang.</p>
     *
     * @return jenis jurnal
     */
    @Column(name = "jenis", nullable = false, length = 24)
    public String getJenis() { return jenis; }

    /**
     * Menetapkan jenis jurnal.
     *
     * @param jenis {@link #PENJUALAN} atau {@link #HPP}
     */
    public void setJenis(String jenis) { this.jenis = jenis; }

    /**
     * Jurnal yang terbentuk dari penandaan ini.
     *
     * <p>Berlaku catatan getter destruktif yang sama seperti
     * {@link #getTransaksi()}.</p>
     *
     * <p>{@code nullable = false} — dan itu keputusan yang tepat. Penanda tanpa
     * jurnal akan menyatakan "transaksi ini sudah dibukukan" sambil tidak dapat
     * menunjukkan ke mana; transaksinya berhenti terjaring untuk diposting,
     * pendapatannya tidak pernah masuk buku, dan tidak ada yang tampak salah
     * karena daftar "belum diposting" memang kosong. Kolom yang wajib terisi
     * membuat keadaan itu mustahil tersimpan.</p>
     *
     * @return jurnal hasil posting, atau {@code null} bila proksinya lepas
     */
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "posting_history", nullable = false)
    public PostingHistory getPostingHistory() { postingHistory = check(postingHistory); return postingHistory; }

    /**
     * Menetapkan jurnal hasil posting.
     *
     * @param postingHistory jurnal; wajib terisi sebelum disimpan
     */
    public void setPostingHistory(PostingHistory postingHistory) { this.postingHistory = postingHistory; }

    /**
     * Nilai yang dibukukan pada jurnal ini.
     *
     * <p>Mengembalikan {@code 0} bila kosong sehingga penjumlahan pada layar
     * ringkasan tidak perlu berjaga terhadap {@code null}.</p>
     *
     * <p>Salinan, bukan sumber kebenaran: angka yang mengikat tetap ada di
     * jurnalnya sendiri. Gunanya rekonsiliasi — bila nilai di sini tidak lagi
     * cocok dengan jumlah jurnalnya, itu tanda ada yang menyunting salah satu
     * dari keduanya belakangan. Perlu ditekankan bahwa perbandingan itu TIDAK
     * dijalankan otomatis di mana pun; salinan ini hanya menyediakan bahan
     * untuk memeriksanya bila suatu saat dicurigai.</p>
     *
     * @return nilai yang dibukukan; {@code 0} bila kosong
     */
    @Column(name = "nilai", nullable = false)
    public Double getNilai() { return nilai == null ? Double.valueOf(0) : nilai; }

    /**
     * Menetapkan nilai yang dibukukan.
     *
     * @param nilai nilai jurnal
     */
    public void setNilai(Double nilai) { this.nilai = nilai; }

    /**
     * Waktu penandaan dibuat — yaitu waktu posting dijalankan.
     *
     * <p>Berbeda dari tanggal jurnalnya, yang mengikuti tanggal transaksi.
     * Keduanya memang boleh berjauhan: transaksi bulan lalu yang baru diposting
     * hari ini menghasilkan jurnal bertanggal bulan lalu dengan penanda
     * bertanggal hari ini. Perbedaan itu justru yang membuat kolom ini berguna
     * — ia menjawab "kapan pembukuannya dikerjakan", pertanyaan yang tidak
     * dapat dijawab tanggal jurnal.</p>
     *
     * @return waktu penandaan
     */
    @Temporal(TemporalType.TIMESTAMP) @Column(name = "waktu", nullable = false)
    public Date getWaktu() { return waktu; }

    /**
     * Menetapkan waktu penandaan.
     *
     * @param waktu waktu posting dijalankan
     */
    public void setWaktu(Date waktu) { this.waktu = waktu; }

    /**
     * Nama tampil pelaku posting (bayangan audit).
     *
     * @return nama pelaku, atau {@code null}
     */
    public String getOleh() { return oleh; }

    /**
     * Menetapkan nama pelaku — MENGABAIKAN nilai kosong, tidak menimpanya.
     *
     * <p>Menolak {@code null} dan teks berisi spasi saja secara diam. Bentuk ini
     * seragam di basis kode dan merupakan keharusan teknis: kolom
     * {@code oleh}/{@code oleh_id} adalah bayangan audit yang menempel pada
     * barisnya, dan entity di sini melewati jalur-jalur yang menyalin properti
     * tanpa tahu mana yang bermakna. Satu penyalinan lugu dengan string kosong
     * sudah cukup untuk menghapus nama pelaku yang benar, dan baris itu tidak
     * menyimpan nilai sebelumnya di mana pun.</p>
     *
     * <p>Untuk penanda posting, jawaban atas "siapa yang membukukan ini" adalah
     * hal yang justru paling sering ditanyakan ketika angka laporan
     * dipersoalkan. Menolak penulisan kosong berarti jejak itu tidak dapat
     * dikosongkan lagi lewat setter — harga yang benar untuk kolom yang hanya
     * boleh bertambah jelas.</p>
     *
     * @param oleh nama pelaku; diabaikan bila {@code null} atau kosong
     */
    public void setOleh(String oleh) { if (oleh != null && !oleh.trim().isEmpty()) this.oleh = oleh; }

    /**
     * Identitas akun pelaku posting (bayangan audit).
     *
     * @return id akun pelaku, atau {@code null}
     */
    @Column(name = "oleh_id") public String getOlehId() { return olehId; }

    /**
     * Menetapkan id akun pelaku — MENGABAIKAN nilai kosong.
     *
     * <p>Berlaku seluruh pertimbangan pada {@link #setOleh(String)}.</p>
     *
     * @param olehId id akun pelaku; diabaikan bila {@code null} atau kosong
     */
    public void setOlehId(String olehId) { if (olehId != null && !olehId.trim().isEmpty()) this.olehId = olehId; }

    /**
     * Stempel perubahan terakhir.
     *
     * <p>Praktis selalu sama dengan {@link #getWaktu()}: baris penanda ditulis
     * sekali dan tidak pernah disunting.</p>
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
     * <p>Tidak berjalan pada pemakaian normal karena baris penanda tidak pernah
     * di-UPDATE; dipertahankan demi keseragaman, supaya perubahan yang
     * seharusnya tidak terjadi tetap meninggalkan jejak waktu bila toh
     * terjadi.</p>
     */
    @javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }
}
