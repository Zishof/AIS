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

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.koperasi.CaraPembayaranKoperasi;

/** Pembayaran utang satu dokumen PBF, mendukung pelunasan bertahap. */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "apotik_pbf_pembayaran")
public class ApotikPbfPembayaran extends GeneralValueObject {
    private static final long serialVersionUID = 1L;
    private Long id;
    private ApotikPbfDokumen dokumen;
    private CaraPembayaranKoperasi caraBayar;
    private Double nominal;
    private Date tanggal;
    private String keterangan;
    private PostingHistory postingHistory;
    private String oleh;
    private String olehId;
    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "dokumen", nullable = false)
    public ApotikPbfDokumen getDokumen() { dokumen = check(dokumen); return dokumen; }
    public void setDokumen(ApotikPbfDokumen dokumen) { this.dokumen = dokumen; }
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cara_bayar", nullable = false)
    public CaraPembayaranKoperasi getCaraBayar() { caraBayar = check(caraBayar); return caraBayar; }
    public void setCaraBayar(CaraPembayaranKoperasi caraBayar) { this.caraBayar = caraBayar; }
    @Column(name = "nominal", nullable = false)
    public Double getNominal() { return nominal == null ? Double.valueOf(0) : nominal; }
    public void setNominal(Double nominal) { this.nominal = nominal; }
    @Temporal(TemporalType.TIMESTAMP) @Column(name = "tanggal", nullable = false)
    public Date getTanggal() { return tanggal; }
    public void setTanggal(Date tanggal) { this.tanggal = tanggal; }
    @Column(name = "keterangan", length = 500)
    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "posting_history")
    public PostingHistory getPostingHistory() { postingHistory = check(postingHistory); return postingHistory; }
    public void setPostingHistory(PostingHistory postingHistory) { this.postingHistory = postingHistory; }
    @Column(name = "oleh", length = 60) public String getOleh() { return oleh; }
    public void setOleh(String oleh) { this.oleh = oleh; }
    @Column(name = "oleh_id", length = 60) public String getOlehId() { return olehId; }
    public void setOlehId(String olehId) { this.olehId = olehId; }
    @Temporal(TemporalType.TIMESTAMP) public Date getTanggal_dirubah() { return tanggal_dirubah; }
    public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }
    @javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }
}
