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

/** Pemetaan eksplisit akun Apotik; tidak pernah mewarisi akun contoh Kantin. */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "apotik_akun_mapping",
        uniqueConstraints = @UniqueConstraint(columnNames = { "peran" }))
public class ApotikAkunMapping extends GeneralValueObject {
    private static final long serialVersionUID = 1L;

    public static final String PENDAPATAN = "PENDAPATAN";
    public static final String HPP = "HPP";
    public static final String PERSEDIAAN = "PERSEDIAAN";
    public static final String PIUTANG = "PIUTANG";
    public static final String UTANG_PBF = "UTANG_PBF";

    private Long id;
    private String peran;
    private Akun akun;
    private Boolean aktif = Boolean.TRUE;
    private String keterangan;
    private String oleh;
    private String olehId;
    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @Column(name = "peran", nullable = false, length = 32)
    public String getPeran() { return peran; }
    public void setPeran(String peran) { this.peran = peran; }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "akun", nullable = false)
    public Akun getAkun() { akun = check(akun); return akun; }
    public void setAkun(Akun akun) { this.akun = akun; }

    @Column(name = "aktif", nullable = false)
    public Boolean getAktif() { return aktif == null ? Boolean.TRUE : aktif; }
    public void setAktif(Boolean aktif) { this.aktif = aktif; }

    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }
    public String getOleh() { return oleh; }
    public void setOleh(String oleh) { if (oleh != null && !oleh.trim().isEmpty()) this.oleh = oleh; }
    @Column(name = "oleh_id")
    public String getOlehId() { return olehId; }
    public void setOlehId(String olehId) { if (olehId != null && !olehId.trim().isEmpty()) this.olehId = olehId; }
    @Temporal(TemporalType.TIMESTAMP)
    public Date getTanggal_dirubah() { return tanggal_dirubah; }
    public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }
    @javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }
}
