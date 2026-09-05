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
import ais.database.model.akunting.Akun;

/**
 * Pemetaan eksplisit akun Apotik; tidak pernah mewarisi akun contoh Kantin.
 *
 * <h3>Peran dan mengapa pemetaan harus eksplisit</h3>
 *
 * <p>Setiap jurnal apotek perlu tahu akun mana yang didebit dan mana yang
 * dikreditkan. Pertanyaan "akun mana" tidak punya jawaban yang berlaku umum:
 * bagan akun tiap rumah sakit berbeda, dan nomor akun persediaan di satu tempat
 * bisa berarti hal lain di tempat lain. Entity ini menyimpan jawabannya dalam
 * bentuk pemetaan peran &rarr; akun: kata {@link #PERSEDIAAN} adalah peran yang
 * dikenal kode, dan baris di tabel ini menyatakan akun nyata mana yang memainkan
 * peran itu di instalasi bersangkutan.</p>
 *
 * <p>Kalimat "tidak pernah mewarisi akun contoh Kantin" pada judul menyebut
 * keputusan yang penting. Modul kantin sudah punya pemetaan akunnya sendiri, dan
 * memakainya ulang untuk apotek akan terasa hemat. Ia justru berbahaya: obat dan
 * makanan kantin masuk ke akun persediaan yang berbeda, dan pendapatan farmasi
 * dilaporkan terpisah dari pendapatan kantin. Warisan diam-diam akan
 * menghasilkan jurnal yang seimbang dan tampak wajar, tetapi menempatkan angka
 * di akun yang salah — kesalahan yang baru ketahuan saat laporan keuangan
 * disusun dan sudah menyangkut banyak periode.</p>
 *
 * <p>Konsekuensi dari sikap itu terlihat di
 * {@code ApotikPostingHelper}/{@code ApotikPbfPostingHelper}: bila peran yang
 * dibutuhkan belum dipetakan, keduanya TIDAK menebak dan TIDAK memakai akun
 * cadangan. Draf jurnalnya ditandai belum siap dengan alasan yang menyebut peran
 * mana yang kurang ("Akun Utang PBF Apotik belum dipetakan"), dan tidak ada
 * jurnal yang terbentuk. Menolak lebih baik daripada menebak: jurnal yang tidak
 * terbentuk akan terlihat sebagai pekerjaan yang tertunda, sedangkan jurnal yang
 * salah akun terlihat sebagai pekerjaan yang selesai.</p>
 *
 * <h3>Satu peran, satu baris</h3>
 *
 * <p>Batasan unik atas kolom {@code peran} menegakkan bahwa tiap peran hanya
 * punya satu pemetaan. Tanpa batasan itu, dua baris untuk peran yang sama akan
 * membuat akun yang terpilih bergantung pada urutan baris yang dikembalikan
 * basis data — tidak dapat ditebak, dan dapat berubah sendiri setelah
 * pemeliharaan tabel. Pemanggil memang sudah membatasi hasilnya dengan
 * {@code setMaxResults(1)}, tetapi itu memilih SATU dari beberapa, bukan
 * memastikan hanya ada satu.</p>
 *
 * @see ApotikPostingLink penanda transaksi mana yang sudah berjurnal
 * @see ApotikPbfDokumen dokumen utang PBF yang memakai peran {@link #UTANG_PBF} dan {@link #PERSEDIAAN}
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "apotik_akun_mapping",
        uniqueConstraints = @UniqueConstraint(columnNames = { "peran" }))
public class ApotikAkunMapping extends GeneralValueObject {

    /** Versi serialisasi; tetap 1 selama bentuk field tidak berubah maknanya. */
    private static final long serialVersionUID = 1L;

    /**
     * Peran: akun pendapatan penjualan apotek (dikreditkan saat penjualan).
     *
     * <p>Kelima konstanta peran adalah kosakata tetap yang dikenal kode posting.
     * Nilainya tersimpan sebagai teks di kolom {@code peran}, sehingga mengubah
     * teks konstanta akan memutuskan pemetaan yang sudah ada: pencarian
     * {@code Restrictions.eq("peran", ...)} tidak lagi menemukan barisnya, dan
     * seluruh jurnal yang bergantung padanya berhenti terbentuk dengan alasan
     * "belum dipetakan". Tambahkan peran baru bila perlu; jangan mengganti nama
     * yang lama.</p>
     */
    public static final String PENDAPATAN = "PENDAPATAN";

    /** Peran: akun harga pokok penjualan (didebit saat penjualan). */
    public static final String HPP = "HPP";

    /** Peran: akun persediaan obat (didebit saat penerimaan PBF, dikreditkan saat HPP). */
    public static final String PERSEDIAAN = "PERSEDIAAN";

    /** Peran: akun piutang atas penjualan yang belum dibayar tunai. */
    public static final String PIUTANG = "PIUTANG";

    /** Peran: akun utang kepada distributor obat (dikreditkan saat penerimaan PBF). */
    public static final String UTANG_PBF = "UTANG_PBF";

    /** Kunci baris; dibangkitkan basis data. */
    private Long id;

    /** Salah satu dari kelima konstanta peran; unik di seluruh tabel. */
    private String peran;

    /** Akun nyata yang memainkan peran tersebut. Wajib. */
    private Akun akun;

    /** Penanda aktif; hanya baris aktif yang diambil jalur posting. */
    private Boolean aktif = Boolean.TRUE;

    /** Catatan bebas tentang alasan pemetaan. */
    private String keterangan;

    /** Nama tampil pelaku perubahan terakhir (bayangan audit). */
    private String oleh;

    /** Identitas akun pelaku perubahan terakhir (bayangan audit). */
    private String olehId;

    /** Stempel ubah terakhir; disegarkan interseptor audit pada setiap UPDATE. */
    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

    /**
     * Kunci baris, dibangkitkan basis data ({@code IDENTITY}).
     *
     * @return kunci baris, atau {@code null} bila belum tersimpan
     */
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return id; }

    /**
     * Menetapkan kunci baris; dipakai Hibernate, bukan kode aplikasi.
     *
     * @param id kunci baris
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Peran yang dipetakan; salah satu dari kelima konstanta.
     *
     * <p>Dijaga batasan unik di tingkat tabel, sehingga satu peran hanya punya
     * satu pemetaan. Entity tidak memvalidasi bahwa teksnya termasuk kosakata
     * yang dikenal — kolomnya sekadar teks 32 karakter. Peran yang salah tulis
     * akan tersimpan dengan tenang lalu tidak pernah ditemukan pencarian
     * pemanggil, sehingga jurnal yang bersangkutan berhenti terbentuk seolah
     * pemetaannya belum dibuat sama sekali. Selalu pakai konstanta.</p>
     *
     * @return nama peran
     */
    @Column(name = "peran", nullable = false, length = 32)
    public String getPeran() { return peran; }

    /**
     * Menetapkan peran yang dipetakan.
     *
     * @param peran salah satu konstanta peran
     */
    public void setPeran(String peran) { this.peran = peran; }

    /**
     * Akun nyata yang memainkan peran tersebut.
     *
     * <p>Getter DESTRUKTIF: hasil {@code check(...)} ditulis balik ke field.
     * {@code check} menormalkan proksi malas Hibernate yang sudah lepas dari
     * sesinya menjadi {@code null}, mencegah
     * {@code LazyInitializationException} saat objek dibaca di luar sesi.
     * Karena itu memanggilnya dapat mengubah keadaan objek.</p>
     *
     * <p>{@code nullable = false} di tingkat kolom. Perlu diperhatikan bahwa
     * jalur posting tetap berjaga sendiri terhadap {@code null} — helper
     * memeriksa {@code m == null ? null : m.getAkun()} dan menandai draf belum
     * siap bila hasilnya kosong. Kehati-hatian ganda itu tepat: getter destruktif
     * di atas dapat mengembalikan {@code null} untuk proksi yang lepas meskipun
     * kolomnya di basis data terisi, sehingga "kolom wajib terisi" tidak sama
     * dengan "getter tidak pernah kosong".</p>
     *
     * <p>{@code Akun} milik modul akuntansi dan dipakai bersama seluruh sistem.
     * Menghapus atau menonaktifkan akun di sana akan berpengaruh langsung ke
     * jurnal apotek tanpa ada peringatan dari sisi ini.</p>
     *
     * @return akun yang dipetakan, atau {@code null} bila proksinya lepas
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "akun", nullable = false)
    public Akun getAkun() { akun = check(akun); return akun; }

    /**
     * Menetapkan akun yang memainkan peran ini.
     *
     * @param akun akun akuntansi; wajib terisi sebelum disimpan
     */
    public void setAkun(Akun akun) { this.akun = akun; }

    /**
     * Penanda aktif; hanya baris aktif yang diambil jalur posting.
     *
     * <p>Mengembalikan {@code TRUE} bila kolom kosong. Bawaan itu perlu dipahami
     * arahnya: ia condong MENGAKTIFKAN, bukan menonaktifkan. Untuk pemetaan akun
     * arah itu tepat — baris pemetaan yang entah bagaimana tersimpan tanpa
     * penanda tetap dipakai, sehingga jurnal tetap terbentuk alih-alih diam-diam
     * berhenti dengan alasan "belum dipetakan" yang membingungkan. Bandingkan
     * dengan penanda berbawaan {@code FALSE} seperti
     * {@link ApotikPenerimaanSuhu#getAdaColdChain()}, di mana yang aman justru
     * menganggap sesuatu tidak berlaku sampai dinyatakan berlaku.</p>
     *
     * <p>Pencarian pemanggil menyaring dengan {@code Restrictions.eq("aktif",
     * Boolean.TRUE)} atas KOLOM, bukan lewat getter ini. Perbedaan itu nyata:
     * baris yang kolomnya benar-benar NULL di basis data TIDAK akan terjaring
     * penyaringan tersebut, meskipun getter ini akan menyebutnya aktif bila
     * barisnya dimuat dengan cara lain. Kolom yang kosong karena itu berperilaku
     * seperti nonaktif di jalur posting — kebalikan dari apa yang disarankan
     * getter ini. Isilah penanda secara eksplisit ketika membuat baris.</p>
     *
     * <p>Penonaktifan sendiri bersifat dua arah dan tidak merusak apa pun:
     * baris dapat dinyalakan kembali, dan jurnal yang sudah terbentuk dengan
     * pemetaan lama tidak berubah. Yang berhenti hanyalah pembentukan jurnal
     * BARU yang membutuhkan peran itu.</p>
     *
     * @return penanda aktif; {@code TRUE} bila kolom kosong
     */
    @Column(name = "aktif", nullable = false)
    public Boolean getAktif() { return aktif == null ? Boolean.TRUE : aktif; }

    /**
     * Menetapkan penanda aktif.
     *
     * @param aktif {@code TRUE} untuk mengaktifkan pemetaan
     */
    public void setAktif(Boolean aktif) { this.aktif = aktif; }

    /**
     * Catatan bebas tentang alasan pemetaan.
     *
     * <p>Tempat yang tepat untuk menuliskan mengapa akun tertentu dipilih —
     * keterangan yang biasanya hanya ada di kepala orang yang menyiapkan bagan
     * akun, dan yang paling dibutuhkan justru ketika orang itu sudah tidak ada.</p>
     *
     * @return keterangan, atau {@code null}
     */
    public String getKeterangan() { return keterangan; }

    /**
     * Menetapkan catatan bebas.
     *
     * @param keterangan keterangan
     */
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

    /**
     * Nama tampil pelaku perubahan terakhir (bayangan audit).
     *
     * @return nama pelaku, atau {@code null}
     */
    public String getOleh() { return oleh; }

    /**
     * Menetapkan nama pelaku — MENGABAIKAN nilai kosong, tidak menimpanya.
     *
     * <p>Menolak {@code null} dan teks berisi spasi saja secara diam. Bentuk ini
     * seragam di basis kode dan merupakan keharusan teknis, bukan kelalaian:
     * kolom bayangan audit ini melewati jalur-jalur yang menyalin seluruh
     * properti tanpa memilah, dan satu penyalinan dengan string kosong sudah
     * cukup untuk menghapus nama pelaku yang benar tanpa menyisakan jejak.</p>
     *
     * <p>Untuk pemetaan akun pertaruhannya nyata. Mengubah akun mana yang
     * memainkan peran {@link #PENDAPATAN} akan mengubah ke mana seluruh
     * pendapatan apotek dibukukan sejak saat itu, dan pertanyaan "siapa
     * mengubah pemetaan ini" adalah hal pertama yang ditanyakan ketika laporan
     * keuangan tiba-tiba berpindah bentuk. Harganya: nilai tidak dapat
     * dikosongkan kembali lewat setter.</p>
     *
     * @param oleh nama pelaku; diabaikan bila {@code null} atau kosong
     */
    public void setOleh(String oleh) { if (oleh != null && !oleh.trim().isEmpty()) this.oleh = oleh; }

    /**
     * Identitas akun pelaku perubahan terakhir (bayangan audit).
     *
     * @return id akun pelaku, atau {@code null}
     */
    @Column(name = "oleh_id")
    public String getOlehId() { return olehId; }

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
     * <p>Untuk entity konfigurasi seperti ini nilainya bergerak setiap kali
     * pemetaan disunting, dan itu justru informasi yang berguna: ia menjawab
     * "sejak kapan jurnal mulai memakai akun yang sekarang". Revisi lengkapnya
     * tersimpan Envers di {@code new_audit.apotik_akun_mapping__audit}.</p>
     *
     * @return waktu ubah terakhir
     */
    @Temporal(TemporalType.TIMESTAMP)
    public Date getTanggal_dirubah() { return tanggal_dirubah; }

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
     * entity memakai satu sumber waktu yang sama. Tidak berjalan pada INSERT.</p>
     */
    @javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }
}
