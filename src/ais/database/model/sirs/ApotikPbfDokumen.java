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
import ais.database.model.akunting.PostingHistory;

/** Dokumen sumber penerimaan obat dari PBF; terpisah penuh dari Kulakan/Kantin. */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "apotik_pbf_dokumen",
        uniqueConstraints = @UniqueConstraint(columnNames = { "kode" }))
public class ApotikPbfDokumen extends GeneralValueObject {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String kode;
    private String noFaktur;
    private String penyedia;
    private Date tanggal;
    private Double total;
    private Integer jumlahBaris;
    private String keterangan;
    private PostingHistory postingHistory;
    private String oleh;
    private String olehId;
    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @Column(name = "kode", nullable = false, length = 80)
    public String getKode() { return kode; }
    public void setKode(String kode) { this.kode = kode; }

    @Column(name = "no_faktur", length = 100)
    public String getNoFaktur() { return noFaktur; }
    public void setNoFaktur(String noFaktur) { this.noFaktur = noFaktur; }

    @Column(name = "penyedia", length = 200)
    public String getPenyedia() { return penyedia; }
    public void setPenyedia(String penyedia) { this.penyedia = penyedia; }

    @Temporal(TemporalType.TIMESTAMP) @Column(name = "tanggal", nullable = false)
    public Date getTanggal() { return tanggal; }
    public void setTanggal(Date tanggal) { this.tanggal = tanggal; }

    @Column(name = "total", nullable = false)
    public Double getTotal() { return total == null ? Double.valueOf(0) : total; }
    public void setTotal(Double total) { this.total = total; }

    @Column(name = "jumlah_baris", nullable = false)
    public Integer getJumlahBaris() { return jumlahBaris == null ? Integer.valueOf(0) : jumlahBaris; }
    public void setJumlahBaris(Integer jumlahBaris) { this.jumlahBaris = jumlahBaris; }

    @Column(name = "keterangan", length = 500)
    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "posting_history")
    public PostingHistory getPostingHistory() { postingHistory = check(postingHistory); return postingHistory; }
    public void setPostingHistory(PostingHistory postingHistory) { this.postingHistory = postingHistory; }

    @Column(name = "oleh", length = 60)
    public String getOleh() { return oleh; }
    public void setOleh(String oleh) { this.oleh = oleh; }
    @Column(name = "oleh_id", length = 60)
    public String getOlehId() { return olehId; }
    public void setOlehId(String olehId) { this.olehId = olehId; }
    @Temporal(TemporalType.TIMESTAMP) public Date getTanggal_dirubah() { return tanggal_dirubah; }
    public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }
    @javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }
}
