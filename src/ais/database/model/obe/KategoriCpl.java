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

/** Master kategori yang digunakan oleh Capaian Lulusan (CPL). */
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
    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKode() {
        return kode == null ? "" : kode.trim();
    }

    public void setKode(String kode) {
        this.kode = kode;
    }

    @Column(name = "nama", nullable = false, columnDefinition = "text")
    public String getNama() {
        return nama == null ? "" : nama.trim();
    }

    public void setNama(String nama) {
        this.nama = nama;
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

    public void setPerguruanTinggi(PerguruanTinggi perguruanTinggi) {
        this.perguruanTinggi = perguruanTinggi;
    }

    public String getOleh() {
        return oleh;
    }

    public void setOleh(String oleh) {
        if (oleh != null && !oleh.trim().isEmpty()) this.oleh = oleh;
    }

    public String getOlehId() {
        return olehId;
    }

    public void setOlehId(String olehId) {
        if (olehId != null && !olehId.trim().isEmpty()) this.olehId = olehId;
    }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getTanggal_dirubah() {
        return tanggal_dirubah;
    }

    public void setTanggal_dirubah(Date tanggal_dirubah) {
        this.tanggal_dirubah = tanggal_dirubah;
    }

    @javax.persistence.PreUpdate
    protected void onUpdate() {
        ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
    }

    @Override
    public String toString() {
        return id + "-" + nama;
    }
}
