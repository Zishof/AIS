package ais.database.model.sekolah;

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
import ais.ui.util.WaktuUtil;

/**
 * Penugasan seorang calon siswa ke satu sesi wawancara PSB.
 *
 * <p>Setiap baris menghubungkan satu {@link CalonSiswa} dengan satu
 * {@link InterviewCalonSiswa}. Kolom {@code mulai} dan {@code sampai}
 * bersifat opsional: jika null, waktu diambil dari sesi induk
 * ({@link InterviewCalonSiswa#getMulai()} / {@link InterviewCalonSiswa#getSampai()}).</p>
 *
 * <p>Kolom {@code siap} diisi oleh calon siswa melalui portal PPDB ketika
 * calon menyatakan kesiapan mengikuti wawancara. Kolom {@code keterangan}
 * menyimpan catatan opsional dari calon (mis. tautan konferensi sisi peserta
 * atau pesan ke pewawancara).</p>
 *
 * <p>Tabel: {@code sekolah.interview_punya_calon_siswa}. @Audited aktif.</p>
 *
 * @author  Tim Pengembang AIS
 * @version 2026-07-16
 * @see     InterviewCalonSiswa
 * @see     CalonSiswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "interview_punya_calon_siswa")
public class InterviewPunyaCalonSiswa extends GeneralValueObject {

    private static final long serialVersionUID = -6204712983011765432L;

    // ── Audit fields ─────────────────────────────────────────────────────
    private Long id;
    private String oleh;
    private String olehId;

    public String getOlehId() { return olehId; }
    public void setOlehId(String olehId) {
        if (olehId == null || olehId.trim().isEmpty()) { return; }
        this.olehId = olehId;
    }
    public void setOleh(String oleh) {
        if (oleh == null || oleh.trim().isEmpty()) { return; }
        this.oleh = oleh;
    }
    public String getOleh() { return oleh; }

    @javax.persistence.PreUpdate
    protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }

    private Date tanggal_dirubah = WaktuUtil.getDate();
    public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }
    @Temporal(TemporalType.TIMESTAMP)
    public Date getTanggal_dirubah() { return tanggal_dirubah; }

    public String toString() {
        return calonSiswa != null ? calonSiswa.getNama() : String.valueOf(id);
    }

    // ── Bidang data ───────────────────────────────────────────────────────
    private InterviewCalonSiswa interviewCalonSiswa;
    private CalonSiswa          calonSiswa;
    private Date    mulai;
    private Date    sampai;
    private Boolean siap;
    private String  keterangan;

    public InterviewPunyaCalonSiswa() {}

    // ── Kunci utama ───────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }

    // ── Relasi ────────────────────────────────────────────────────────────

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_calon_siswa_id")
    public InterviewCalonSiswa getInterviewCalonSiswa() {
        interviewCalonSiswa = check(interviewCalonSiswa);
        return interviewCalonSiswa;
    }
    public void setInterviewCalonSiswa(InterviewCalonSiswa interviewCalonSiswa) {
        this.interviewCalonSiswa = interviewCalonSiswa;
    }

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "calon_siswa_id")
    public CalonSiswa getCalonSiswa() {
        calonSiswa = check(calonSiswa);
        return calonSiswa;
    }
    public void setCalonSiswa(CalonSiswa calonSiswa) {
        this.calonSiswa = calonSiswa;
    }

    // ── Jadwal per-peserta (opsional, fallback ke sesi induk) ─────────────

    @Temporal(TemporalType.TIMESTAMP)
    public Date getMulai() {
        return mulai != null ? mulai
                : (getInterviewCalonSiswa() == null ? null : getInterviewCalonSiswa().getMulai());
    }
    public void setMulai(Date mulai) { this.mulai = mulai; }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getSampai() {
        return sampai != null ? sampai
                : (getInterviewCalonSiswa() == null ? null : getInterviewCalonSiswa().getSampai());
    }
    public void setSampai(Date sampai) { this.sampai = sampai; }

    // ── Status kesiapan ───────────────────────────────────────────────────

    public Boolean getSiap() { return siap == null ? false : siap; }
    public void setSiap(Boolean siap) { this.siap = siap; }

    @Column(columnDefinition = "text")
    public String getKeterangan() { return keterangan == null ? "" : keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }
}
