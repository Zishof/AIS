package ais.database.model.akunting;

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

import ais.database.model.GeneralValueObject;
import ais.database.model.rab.SatuanKerja;

/**
 * Jenis Reimbursement — menentukan perilaku pengajuan reimbursement pegawai:
 *
 * <ul>
 *   <li><b>menggunakanAnggaran = true</b> (default): pengaju WAJIB memilih
 *       Anggaran (Workspace) pada form pengajuan; akun mengikuti anggaran.</li>
 *   <li><b>menggunakanAnggaran = false</b> ("Tanpa Anggaran"): {@link #akun}
 *       ditentukan DI SINI oleh admin, sehingga pengaju tidak perlu memilih
 *       akun di setiap pengajuan. Admin boleh membuat beberapa jenis tanpa
 *       anggaran dengan akun/satuan kerja berbeda.</li>
 * </ul>
 *
 * Dua baris default dibuat saat bootstrap Tomcat oleh
 * {@code InitIndex.initDefaultJenisReimbursement()}.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "akunting", name = "jenis_reimbursement")
public class JenisReimbursement extends GeneralValueObject {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String nama;
    private String keterangan;
    private Boolean menggunakanAnggaran;
    private Akun akun;
    private SatuanKerja satuanKerja;
    private Boolean aktif;
    private Date tanggalDirubah = ais.ui.util.WaktuUtil.getDate();

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @Column(name = "nama", length = 255)
    public String getNama() { return nama == null ? null : nama.trim(); }
    public void setNama(String nama) { this.nama = nama; }

    @Column(name = "keterangan", columnDefinition = "text")
    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

    /** Default TRUE: pengaju wajib memilih Anggaran (Workspace) saat mengajukan. */
    @Column(name = "menggunakan_anggaran")
    public Boolean getMenggunakanAnggaran() { return menggunakanAnggaran == null ? Boolean.TRUE : menggunakanAnggaran; }
    public void setMenggunakanAnggaran(Boolean menggunakanAnggaran) { this.menggunakanAnggaran = menggunakanAnggaran; }

    /** Akun biaya tetap untuk jenis TANPA anggaran (wajib diisi admin pada jenis tsb). */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "akun", nullable = true)
    public Akun getAkun() { akun = check(akun); return akun; }
    public void setAkun(Akun akun) { this.akun = akun; }

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "satuan_kerja", nullable = true)
    public SatuanKerja getSatuanKerja() { satuanKerja = check(satuanKerja); return satuanKerja; }
    public void setSatuanKerja(SatuanKerja satuanKerja) { this.satuanKerja = satuanKerja; }

    @Column(name = "aktif")
    public Boolean getAktif() { return aktif == null ? Boolean.TRUE : aktif; }
    public void setAktif(Boolean aktif) { this.aktif = aktif; }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "tanggal_dirubah")
    public Date getTanggalDirubah() { return tanggalDirubah; }
    public void setTanggalDirubah(Date tanggalDirubah) { this.tanggalDirubah = tanggalDirubah; }

    @javax.persistence.PreUpdate
    protected void onUpdate() { tanggalDirubah = ais.ui.util.WaktuUtil.getDate(); }

    public String toString() {
        String s = nama == null ? "" : nama.trim();
        try {
            if (satuanKerja != null && satuanKerja.getId() != null && satuanKerja.getNama() != null) {
                s += " (" + satuanKerja.getNama() + ")";
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) JenisReimbursement.toString");
        }
        return s;
    }
}
