package ais.database.model.obe;

import static javax.persistence.GenerationType.IDENTITY;

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
import ais.database.model.PerguruanTinggi;

/**
 * <h3>KategoriCpl — master kategori Capaian Pembelajaran Lulusan (CPL)</h3>
 *
 * <p>Daftar master kategori CPL per perguruan tinggi, sesuai taksonomi Standar Nasional
 * Pendidikan Tinggi (SN-Dikti): umumnya empat kategori baku — Sikap, Pengetahuan,
 * Keterampilan Umum, dan Keterampilan Khusus — meski kelas ini tidak membatasi isinya
 * secara hardcode (nilai kategori sepenuhnya bebas diisi/diedit oleh admin per perguruan
 * tinggi, lihat {@link #getPerguruanTinggi()}).</p>
 *
 * <p><b>Catatan arsitektur penting — relasi ke {@link CapaianLulusan} BUKAN foreign key:</b>
 * kelas ini didaftarkan sebagai entity mandiri di {@code hibernate.cfg.xml} dan
 * {@code ais.common.InitData} (auto-seed data awal), namun {@link CapaianLulusan#getKategori()}
 * menyimpan kategori sebagai <i>teks bebas</i> — salinan {@link #getNama()} pada saat CPL
 * dibuat/diedit — bukan referensi ID ke baris {@link KategoriCpl} ini. Konsekuensinya:
 * mengganti {@code nama} sebuah kategori di sini tidak mengubah label yang sudah tersimpan
 * pada CPL manapun (tidak ada propagasi rename), dan menghapus/menonaktifkan kategori di sini
 * tidak memutus/menghapus data CPL yang pernah memakainya (sengaja, agar data historis tetap
 * bisa dibuka/diedit — lihat komentar UI di {@code CapaianLulusanAction}: "Data lama tetap
 * dapat diedit walaupun kategorinya sudah dinonaktifkan/dihapus"). Lihat juga
 * {@link CapaianLulusan#getKategori()} untuk penjelasan lengkap dari sisi pemakainya.</p>
 *
 * <p>Mengikuti pola entitas OBE lain: extends {@link GeneralValueObject}, ber-audit Envers,
 * tabel {@code public.kategori_cpl}, dan field audit shadow {@code oleh}/{@code olehId}/
 * {@code tanggal_dirubah} yang diisi otomatis oleh
 * {@link ais.database.hibernate.AuditTimestampInterceptor} — kebutuhan teknis Hibernate,
 * bukan duplikasi yang keliru.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "kategori_cpl")
public class KategoriCpl extends GeneralValueObject {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String kode;
    private String nama;
    private String keterangan;
    private Boolean aktif;
    private PerguruanTinggi perguruanTinggi;
    private String oleh;
    private String olehId;
    /** Timestamp terakhir baris ini diubah; default diisi saat objek dibuat, diperbarui via {@link #onUpdate()}. */
    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

    /** @return ID unik baris kategori CPL (primary key, auto-increment via {@code IDENTITY}). */
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() {
        return id;
    }

    /** @param id lihat {@link #getId()}. Normalnya tidak perlu diisi manual — dihasilkan DB saat insert. */
    public void setId(Long id) {
        this.id = id;
    }

    /** @return kode singkat kategori (mis. "S", "P", "KU", "KK"), di-trim; string kosong bila belum diisi. */
    public String getKode() {
        return kode == null ? "" : kode.trim();
    }

    /** @param kode lihat {@link #getKode()}. */
    public void setKode(String kode) {
        this.kode = kode;
    }

    /**
     * @return nama kategori (mis. "Sikap", "Pengetahuan", "Keterampilan Umum",
     *         "Keterampilan Khusus"), di-trim. Nilai inilah yang disalin sebagai teks bebas
     *         ke {@link CapaianLulusan#getKategori()} saat kategori ini dipilih di form CPL —
     *         lihat catatan arsitektur di javadoc kelas.
     */
    @Column(name = "nama", nullable = false, columnDefinition = "text")
    public String getNama() {
        return nama == null ? "" : nama.trim();
    }

    /** @param nama lihat {@link #getNama()}. */
    public void setNama(String nama) {
        this.nama = nama;
    }

    /** @return keterangan/deskripsi tambahan kategori (opsional); tidak di-trim, boleh {@code null}. */
    @Column(name = "keterangan", columnDefinition = "text")
    public String getKeterangan() {
        return keterangan;
    }

    /** @param keterangan lihat {@link #getKeterangan()}. */
    public void setKeterangan(String keterangan) {
        this.keterangan = keterangan;
    }

    /**
     * @return status aktif kategori. Flag satu-arah: {@code null} (baris lama/belum pernah
     *         diisi) dianggap aktif secara default agar data lama tidak tiba-tiba hilang dari
     *         daftar kategori aktif. Kategori yang dinonaktifkan tetap muncul di form edit CPL
     *         lama yang sudah memakainya (lihat {@code CapaianLulusanAction}), hanya
     *         disembunyikan dari daftar pilihan kategori baru.
     */
    public Boolean getAktif() {
        return aktif == null ? true : aktif;
    }

    /** @param aktif lihat {@link #getAktif()}. */
    public void setAktif(Boolean aktif) {
        this.aktif = aktif;
    }

    /**
     * @return perguruan tinggi pemilik daftar kategori ini (kategori CPL bersifat per-PT,
     *         bukan global). Lazy-loaded, di-null-safe-kan via
     *         {@link GeneralValueObject#check(Object)}; bila hasilnya masih {@code null}
     *         (baris lama tanpa kolom ini terisi), jatuh ke PT milik sesi pengguna saat ini
     *         via {@link ais.action.master.helper.util.PerguruanTinggiUtil#getPerguruanTinggi()}.
     *         Exception dari util tsb. ditangkap diam-diam dan direkam ke
     *         {@link ais.common.ErrorAuditUtil} (auto-audit empty-catch).
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "perguruan_tinggi")
    public PerguruanTinggi getPerguruanTinggi() {
        perguruanTinggi = check(perguruanTinggi);
        try {
            if (perguruanTinggi == null) {
                perguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi();
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e,
                    "auto-audit(empty-catch) src/ais/database/model/obe/KategoriCpl.java");
        }
        return perguruanTinggi;
    }

    /** @param perguruanTinggi lihat {@link #getPerguruanTinggi()}. */
    public void setPerguruanTinggi(PerguruanTinggi perguruanTinggi) {
        this.perguruanTinggi = perguruanTinggi;
    }

    /** @return nama pengguna yang terakhir mengubah baris ini (field audit shadow, diisi via {@link #onUpdate()}). */
    public String getOleh() {
        return oleh;
    }

    /**
     * Setter {@link #getOleh()}. Nilai kosong/blank diabaikan (no-op) agar jejak audit lama
     * tidak tertimpa saat proses simpan tidak membawa identitas pengguna — pola baku di semua
     * entitas modul OBE.
     */
    public void setOleh(String oleh) {
        if (oleh != null && !oleh.trim().isEmpty()) this.oleh = oleh;
    }

    /** @return ID pengguna (username) yang terakhir mengubah baris ini. Field audit shadow — lihat {@link #getOleh()}. */
    public String getOlehId() {
        return olehId;
    }

    /** Setter {@link #getOlehId()}. Nilai kosong/blank diabaikan (no-op), sama seperti {@link #setOleh(String)}. */
    public void setOlehId(String olehId) {
        if (olehId != null && !olehId.trim().isEmpty()) this.olehId = olehId;
    }

    /** @return timestamp terakhir baris ini diubah. */
    @Temporal(TemporalType.TIMESTAMP)
    public Date getTanggal_dirubah() {
        return tanggal_dirubah;
    }

    /** @param tanggal_dirubah lihat {@link #getTanggal_dirubah()}. */
    public void setTanggal_dirubah(Date tanggal_dirubah) {
        this.tanggal_dirubah = tanggal_dirubah;
    }

    /**
     * Callback JPA {@code @PreUpdate}, dipanggil otomatis Hibernate sebelum UPDATE untuk
     * mengisi {@link #oleh}/{@link #olehId}/{@link #tanggal_dirubah} dari sesi pengguna aktif
     * via {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
     */
    @javax.persistence.PreUpdate
    protected void onUpdate() {
        ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
    }

    /** @return representasi ringkas {@code "<id>-<nama>"}, dipakai untuk tampilan log/debug. */
    @Override
    public String toString() {
        return id + "-" + nama;
    }
}
