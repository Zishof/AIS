package ais.database.model.spmi;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.LinkedHashMap;
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

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Entitas Hibernate yang memetakan tabel {@code public.tindak_lanjut_temuan_spmi}
 * pada modul SPMI (Sistem Penjaminan Mutu Internal) perguruan tinggi.
 * Merepresentasikan satu rencana/aksi tindak lanjut ({@code deskripsi}) atas
 * satu {@link HasilTemuanSPMI} (temuan audit mutu internal) — siapa
 * penanggung jawabnya ({@code picNama}), target waktu penyelesaian
 * ({@code targetDate}), realisasi tanggal selesai ({@code tanggalSelesai}),
 * progres dalam persen (0-100, dijepit lewat {@link #setProgressPersen(int)}),
 * serta status alur ({@code status} — lihat konstanta {@link #BELUM_DIMULAI},
 * {@link #SEDANG_BERJALAN}, {@link #TERLAMBAT}, {@link #SELESAI} dan peta
 * label {@link #statusLabel}).
 *
 * <p><b>Catatan arsitektur — independen dari mesin SOP generik:</b> berbeda
 * dari {@link HasilSPMI} (yang mewarisi {@code DataSop} dan menautkan
 * persetujuan hasil evaluasinya ke {@code AlurSop}/{@code DisposisiSop}
 * generik), entitas {@code TindakLanjutTemuanSPMI} <b>tidak</b> memakai mesin
 * SOP sama sekali — {@code status} di sini murni kolom string yang diubah
 * langsung lewat CRUD biasa (lihat
 * {@link ais.action.master.spmi.TindakLanjutSPMIAction}), tanpa
 * {@code DisposisiSop}/alur persetujuan berjenjang. Siapa pun yang dapat
 * membuka panel tindak lanjut pada baris temuan (mis. auditor yang sama yang
 * mencatat temuannya) juga dapat menandai tindak lanjut tersebut langsung ke
 * {@link #SELESAI} tanpa verifikasi terpisah oleh pihak lain — tidak ada
 * pemisahan peran pelaksana vs. pemverifikasi pada level entitas maupun UI.
 * Ini konsisten dengan pola "self-approval belum ditambal" yang sudah
 * tercatat berulang pada modul-modul lain di basis kode ini, dan bukan
 * merupakan perluasan dari temuan bypass persetujuan pada mesin
 * {@code AlurSop}/{@code DisposisiSop} generik (yang berlaku untuk
 * {@link HasilSPMI}, bukan untuk kelas ini).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "tindak_lanjut_temuan_spmi")
public class TindakLanjutTemuanSPMI extends GeneralValueObject {

    private static final long serialVersionUID = 7312948576234019801L;

    /** Tindak lanjut belum mulai dikerjakan. */
    public static final String BELUM_DIMULAI   = "Belum Dimulai";
    /** Tindak lanjut sedang dikerjakan, belum melewati target waktu. */
    public static final String SEDANG_BERJALAN = "Sedang Berjalan";
    /** Tindak lanjut melewati {@code targetDate} tapi belum selesai. */
    public static final String TERLAMBAT       = "Terlambat";
    /** Tindak lanjut sudah selesai dikerjakan. */
    public static final String SELESAI         = "Selesai";

    /** Peta label tampilan untuk tiap nilai status (kunci = nilai = label, untuk keperluan dropdown UI). */
    public static final Map<String, String> statusLabel = new LinkedHashMap<String, String>();
    static {
        statusLabel.put(BELUM_DIMULAI,   BELUM_DIMULAI);
        statusLabel.put(SEDANG_BERJALAN, SEDANG_BERJALAN);
        statusLabel.put(TERLAMBAT,       TERLAMBAT);
        statusLabel.put(SELESAI,         SELESAI);
    }

    /** Primary key baris ini. */
    private Long            id;
    /** Temuan audit ({@link HasilTemuanSPMI}) induk yang ditindaklanjuti oleh baris ini. */
    private HasilTemuanSPMI hasilTemuanSPMI;
    /** Uraian rencana/aksi perbaikan yang akan atau sedang dilakukan. */
    private String          deskripsi;
    /** Nama penanggung jawab (PIC) pelaksanaan tindak lanjut ini. */
    private String          picNama;
    /** Target tanggal penyelesaian tindak lanjut. */
    private Date            targetDate;
    /** Tanggal realisasi selesai; {@code null} selama tindak lanjut belum ditandai selesai. */
    private Date            tanggalSelesai;
    /** Progres pelaksanaan dalam persen, dijepit ke rentang 0-100 oleh {@link #setProgressPersen(int)}. */
    private int             progressPersen;
    /** Kode status alur (lihat {@link #BELUM_DIMULAI}, {@link #SEDANG_BERJALAN}, {@link #TERLAMBAT}, {@link #SELESAI}). */
    private String          status;
    /** Keterangan/catatan tambahan bebas terkait tindak lanjut ini. */
    private String          keterangan;
    /** Flag aktif/nonaktif (soft delete); {@code null} diperlakukan sebagai aktif oleh {@link #getAktif()}. */
    private Boolean         aktif;
    /** Kolom audit shadow: nama pengguna yang terakhir menyimpan/mengubah baris ini. */
    private String          oleh;
    /** Kolom audit shadow: identitas (id) pengguna yang terakhir menyimpan/mengubah baris ini. */
    private String          olehId;
    /** Timestamp terakhir kali baris ini diubah; diperbarui otomatis oleh {@link #onUpdate()}. */
    private Date            tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

    /** Konstruktor kosong wajib bagi Hibernate untuk membentuk proxy/instance entitas. */
    public TindakLanjutTemuanSPMI() {
    }

    /**
     * Membuat tindak lanjut baru yang langsung terkait ke temuan audit yang
     * ditindaklanjutinya.
     *
     * @param hasilTemuanSPMI temuan ({@link HasilTemuanSPMI}) induk tindak lanjut ini
     */
    public TindakLanjutTemuanSPMI(HasilTemuanSPMI hasilTemuanSPMI) {
        this.hasilTemuanSPMI = hasilTemuanSPMI;
    }

    /**
     * Hook JPA {@code @PreUpdate}: dipanggil otomatis oleh provider persistence
     * sesaat sebelum baris ini di-{@code UPDATE}, mendelegasikan pencatatan
     * timestamp perubahan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
     * Bukan API publik — tidak dipanggil manual dari kode aplikasi.
     */
    @javax.persistence.PreUpdate
    protected void onUpdate() {
        ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
    }

    /**
     * @return primary key baris ini. Kolom {@code id} bertipe {@code IDENTITY}
     *         (auto-increment oleh database) dan ditandai {@code insertable = false}.
     */
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() {
        return id;
    }

    /** @param id primary key; jarang dipanggil manual karena {@code id} adalah IDENTITY. */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return {@link HasilTemuanSPMI} — temuan audit yang ditindaklanjuti oleh
     *         baris ini. Getter memanggil {@link #check(Object)} warisan dari
     *         {@link GeneralValueObject} untuk menangani kemungkinan proxy
     *         Hibernate yang stale/terputus dari session. Kolom
     *         {@code hasil_temuan_spmi} wajib diisi ({@code nullable = false}).
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "hasil_temuan_spmi", nullable = false)
    public HasilTemuanSPMI getHasilTemuanSPMI() {
        hasilTemuanSPMI = check(hasilTemuanSPMI);
        return hasilTemuanSPMI;
    }

    /**
     * Menyimpan temuan induk. Setter ini menolak nilai yang {@code null} atau
     * belum ter-{@code persist} (belum punya {@code id}) — mencegah relasi
     * menunjuk ke instance transient yang gagal disimpan.
     *
     * @param hasilTemuanSPMI temuan induk; diabaikan bila null atau belum punya id
     */
    public void setHasilTemuanSPMI(HasilTemuanSPMI hasilTemuanSPMI) {
        if (hasilTemuanSPMI != null && hasilTemuanSPMI.getId() != null) {
            this.hasilTemuanSPMI = hasilTemuanSPMI;
        }
    }

    /**
     * @return uraian rencana/aksi tindak lanjut, di-{@code trim()} terlebih
     *         dahulu; dikembalikan sebagai string kosong (bukan {@code null})
     *         bila belum diisi.
     */
    @Column(name = "deskripsi", nullable = false, columnDefinition = "text")
    public String getDeskripsi() {
        return deskripsi == null ? "" : deskripsi.trim();
    }

    /** @param deskripsi uraian rencana/aksi tindak lanjut; wajib diisi (kolom {@code NOT NULL}). */
    public void setDeskripsi(String deskripsi) {
        this.deskripsi = deskripsi;
    }

    /** @return nama penanggung jawab (PIC) pelaksanaan tindak lanjut ini; boleh {@code null}. */
    @Column(name = "pic_nama")
    public String getPicNama() {
        return picNama;
    }

    /** @param picNama nama penanggung jawab (PIC) pelaksanaan; opsional. */
    public void setPicNama(String picNama) {
        this.picNama = picNama;
    }

    /** @return target tanggal penyelesaian tindak lanjut ini; boleh {@code null} bila belum ditetapkan. */
    @Temporal(TemporalType.DATE)
    @Column(name = "target_date")
    public Date getTargetDate() {
        return targetDate;
    }

    /** @param targetDate target tanggal penyelesaian; opsional. */
    public void setTargetDate(Date targetDate) {
        this.targetDate = targetDate;
    }

    /**
     * @return tanggal realisasi selesai tindak lanjut ini; {@code null}
     *         selama tindak lanjut belum ditandai selesai. Catatan: kolom ini
     *         diisi manual lewat form (lihat
     *         {@link ais.action.master.spmi.TindakLanjutSPMIAction}), tidak
     *         otomatis tersinkron dengan {@link #getStatus()} — mengisi
     *         tanggal ini tidak dengan sendirinya mengubah status menjadi
     *         {@link #SELESAI}, dan sebaliknya.
     */
    @Temporal(TemporalType.DATE)
    @Column(name = "tanggal_selesai")
    public Date getTanggalSelesai() {
        return tanggalSelesai;
    }

    /** @param tanggalSelesai tanggal realisasi selesai; lihat {@link #getTanggalSelesai()}. */
    public void setTanggalSelesai(Date tanggalSelesai) {
        this.tanggalSelesai = tanggalSelesai;
    }

    /** @return progres pelaksanaan tindak lanjut dalam persen (0-100). */
    @Column(name = "progress_persen", nullable = false)
    public int getProgressPersen() {
        return progressPersen;
    }

    /** Nilai dijepit ke rentang 0-100 sebelum disimpan. */
    public void setProgressPersen(int progressPersen) {
        this.progressPersen = Math.max(0, Math.min(100, progressPersen));
    }

    /** Status alur tindak lanjut; default {@link #BELUM_DIMULAI} bila belum diisi. */
    @Column(name = "status")
    public String getStatus() {
        return status == null ? BELUM_DIMULAI : status.trim();
    }

    /**
     * Menyimpan status alur tindak lanjut. Tidak ada validasi transisi di
     * sini — nilai apa pun (termasuk kembali ke {@link #BELUM_DIMULAI} dari
     * {@link #SELESAI}) diterima apa adanya oleh setter ini; disiplin urutan
     * status sepenuhnya bergantung pada UI pemanggil (lihat
     * {@link ais.action.master.spmi.TindakLanjutSPMIAction}), bukan pada
     * entitas ini.
     *
     * @param status kode status baru; lihat {@link #getStatus()}
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /** @return keterangan/catatan tambahan bebas terkait tindak lanjut ini; boleh {@code null}. */
    @Column(name = "keterangan", columnDefinition = "text")
    public String getKeterangan() {
        return keterangan;
    }

    /** @param keterangan keterangan/catatan tambahan; opsional. */
    public void setKeterangan(String keterangan) {
        this.keterangan = keterangan;
    }

    /**
     * @return {@code true} bila tindak lanjut ini masih aktif/berlaku,
     *         {@code false} bila dinonaktifkan (soft delete). Default
     *         {@code true} bila kolom belum pernah diisi — pola flag aktif
     *         "default aman" yang konsisten dengan entitas SPMI lain di
     *         paket ini.
     */
    public Boolean getAktif() {
        return aktif == null ? true : aktif;
    }

    /** @param aktif status aktif/nonaktif tindak lanjut ini; lihat {@link #getAktif()}. */
    public void setAktif(Boolean aktif) {
        this.aktif = aktif;
    }

    /**
     * @return nama pengguna yang tercatat pada kolom audit shadow {@code oleh},
     *         atau {@code null} bila belum pernah diisi.
     */
    public String getOleh() {
        return oleh;
    }

    /**
     * Menyimpan nama pengguna (kolom audit shadow {@code oleh}). Setter ini
     * sengaja mengabaikan nilai {@code null} atau kosong — kebutuhan teknis
     * (bukan bug): nilai yang sudah tercatat oleh interceptor audit tidak
     * boleh tertimpa oleh panggilan berikutnya yang membawa nilai kosong/null.
     *
     * @param oleh nama pengguna; diabaikan bila null/kosong
     */
    public void setOleh(String oleh) {
        if (oleh == null || oleh.trim().isEmpty()) return;
        this.oleh = oleh;
    }

    /**
     * @return nilai mentah kolom audit shadow {@code olehId} (identitas
     *         pengguna yang terakhir menyimpan/mengubah baris ini), atau
     *         {@code null} bila belum pernah diisi.
     */
    public String getOlehId() {
        return olehId;
    }

    /**
     * Menyimpan identitas pengguna (kolom audit shadow {@code olehId}), dengan
     * guard yang sama seperti {@link #setOleh(String)}.
     *
     * @param olehId identitas pengguna; diabaikan bila null/kosong
     */
    public void setOlehId(String olehId) {
        if (olehId == null || olehId.trim().isEmpty()) return;
        this.olehId = olehId;
    }

    /**
     * @return timestamp terakhir kali baris ini diubah, diinisialisasi ke
     *         waktu saat objek dibuat dan diperbarui otomatis oleh
     *         {@link #onUpdate()} saat baris diperbarui di database.
     */
    @Temporal(TemporalType.TIMESTAMP)
    public Date getTanggal_dirubah() {
        return tanggal_dirubah;
    }

    /** @param tanggal_dirubah timestamp perubahan terakhir; biasanya diisi otomatis oleh {@link #onUpdate()}. */
    public void setTanggal_dirubah(Date tanggal_dirubah) {
        this.tanggal_dirubah = tanggal_dirubah;
    }

    /**
     * @return representasi ringkas berupa {@code id + "-" + deskripsi},
     *         dipakai untuk log/debug dan tampilan singkat, bukan identitas
     *         bisnis.
     */
    @Override
    public String toString() {
        return id + "-" + deskripsi;
    }
}
