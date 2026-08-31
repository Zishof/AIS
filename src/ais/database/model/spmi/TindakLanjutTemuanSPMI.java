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

    private Long            id;
    private HasilTemuanSPMI hasilTemuanSPMI;
    private String          deskripsi;
    private String          picNama;
    private Date            targetDate;
    private Date            tanggalSelesai;
    private int             progressPersen;
    private String          status;
    private String          keterangan;
    private Boolean         aktif;
    private String          oleh;
    private String          olehId;
    private Date            tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

    public TindakLanjutTemuanSPMI() {
    }

    public TindakLanjutTemuanSPMI(HasilTemuanSPMI hasilTemuanSPMI) {
        this.hasilTemuanSPMI = hasilTemuanSPMI;
    }

    @javax.persistence.PreUpdate
    protected void onUpdate() {
        ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "hasil_temuan_spmi", nullable = false)
    public HasilTemuanSPMI getHasilTemuanSPMI() {
        hasilTemuanSPMI = check(hasilTemuanSPMI);
        return hasilTemuanSPMI;
    }

    public void setHasilTemuanSPMI(HasilTemuanSPMI hasilTemuanSPMI) {
        if (hasilTemuanSPMI != null && hasilTemuanSPMI.getId() != null) {
            this.hasilTemuanSPMI = hasilTemuanSPMI;
        }
    }

    @Column(name = "deskripsi", nullable = false, columnDefinition = "text")
    public String getDeskripsi() {
        return deskripsi == null ? "" : deskripsi.trim();
    }

    public void setDeskripsi(String deskripsi) {
        this.deskripsi = deskripsi;
    }

    @Column(name = "pic_nama")
    public String getPicNama() {
        return picNama;
    }

    public void setPicNama(String picNama) {
        this.picNama = picNama;
    }

    @Temporal(TemporalType.DATE)
    @Column(name = "target_date")
    public Date getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(Date targetDate) {
        this.targetDate = targetDate;
    }

    @Temporal(TemporalType.DATE)
    @Column(name = "tanggal_selesai")
    public Date getTanggalSelesai() {
        return tanggalSelesai;
    }

    public void setTanggalSelesai(Date tanggalSelesai) {
        this.tanggalSelesai = tanggalSelesai;
    }

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

    public void setStatus(String status) {
        this.status = status;
    }

    @Column(name = "keterangan", columnDefinition = "text")
    public String getKeterangan() {
        return keterangan;
    }

    public void setKeterangan(String keterangan) {
        this.keterangan = keterangan;
    }

    public Boolean getAktif() {
        return aktif == null ? true : aktif;
    }

    public void setAktif(Boolean aktif) {
        this.aktif = aktif;
    }

    public String getOleh() {
        return oleh;
    }

    public void setOleh(String oleh) {
        if (oleh == null || oleh.trim().isEmpty()) return;
        this.oleh = oleh;
    }

    public String getOlehId() {
        return olehId;
    }

    public void setOlehId(String olehId) {
        if (olehId == null || olehId.trim().isEmpty()) return;
        this.olehId = olehId;
    }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getTanggal_dirubah() {
        return tanggal_dirubah;
    }

    public void setTanggal_dirubah(Date tanggal_dirubah) {
        this.tanggal_dirubah = tanggal_dirubah;
    }

    @Override
    public String toString() {
        return id + "-" + deskripsi;
    }
}
