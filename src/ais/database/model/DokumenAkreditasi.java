package ais.database.model;

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

import ais.database.model.rab.SatuanKerja;
import ais.ui.util.WaktuUtil;

@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "dokumen_akreditasi")
public class DokumenAkreditasi extends GeneralValueObject {

    private static final long serialVersionUID = 2463821577548439808L;

    private Long id;
    private String oleh;
    private String olehId;
    private Date tanggal_dirubah = WaktuUtil.getDate();

    private String kode;
    private String nama;
    private String keterangan;
    private Date tanggalDokumen;
    private Akreditasi akreditasi;
    private Integer nomorUrut;
    private Boolean aktif;
    private DokumenAkreditasi induk;
    private SatuanKerja satuanKerja;

    public DokumenAkreditasi() {
    }

    public DokumenAkreditasi(Akreditasi akreditasi, DokumenAkreditasi induk) {
        this.akreditasi = akreditasi;
        this.induk = induk;
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

    public String getOlehId() {
        return olehId;
    }

    public void setOlehId(String olehId) {
        if (olehId == null || olehId.trim().isEmpty()) {
            return;
        }
        this.olehId = olehId;
    }

    public String getOleh() {
        return oleh;
    }

    public void setOleh(String oleh) {
        if (oleh == null || oleh.trim().isEmpty()) {
            return;
        }
        this.oleh = oleh;
    }

    @javax.persistence.PreUpdate
    protected void onUpdate() {
        ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
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
        return (id == null ? "" : id.toString()) + "-" + getNama();
    }

    public String getKode() {
        return kode == null ? "" : kode.trim();
    }

    public void setKode(String kode) {
        this.kode = kode;
    }

    @Column(name = "nama", nullable = false, length = 255)
    public String getNama() {
        return nama == null ? "" : nama.trim();
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    @Column(name = "keterangan", columnDefinition = "text", nullable = true)
    public String getKeterangan() {
        return keterangan == null ? "" : keterangan;
    }

    public void setKeterangan(String keterangan) {
        this.keterangan = keterangan;
    }

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "akreditasi", nullable = false)
    public Akreditasi getAkreditasi() {
        akreditasi = check(akreditasi);
        return akreditasi;
    }

    public void setAkreditasi(Akreditasi akreditasi) {
        this.akreditasi = akreditasi;
    }

    @Temporal(TemporalType.DATE)
    public Date getTanggalDokumen() {
        return tanggalDokumen == null ? WaktuUtil.getDate() : tanggalDokumen;
    }

    public void setTanggalDokumen(Date tanggalDokumen) {
        this.tanggalDokumen = tanggalDokumen;
    }

    public Integer getNomorUrut() {
        return nomorUrut == null || nomorUrut.intValue() <= 0 ? Integer.valueOf(1) : nomorUrut;
    }

    public void setNomorUrut(Integer nomorUrut) {
        this.nomorUrut = nomorUrut;
    }

    public Boolean getAktif() {
        return aktif == null ? Boolean.TRUE : aktif;
    }

    public void setAktif(Boolean aktif) {
        this.aktif = aktif;
    }

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "induk", nullable = true)
    public DokumenAkreditasi getInduk() {
        induk = check(induk);
        return induk;
    }

    public void setInduk(DokumenAkreditasi induk) {
        this.induk = induk;
    }

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "satuan_kerja", nullable = true)
    public SatuanKerja getSatuanKerja() {
        satuanKerja = check(satuanKerja);
        return satuanKerja;
    }

    public void setSatuanKerja(SatuanKerja satuanKerja) {
        this.satuanKerja = satuanKerja;
    }
}
