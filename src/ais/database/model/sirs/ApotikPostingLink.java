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

/** Penanda idempoten satu transaksi Apotik terhadap satu jenis jurnal. */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "apotik_posting_link",
        uniqueConstraints = @UniqueConstraint(columnNames = { "transaksi", "jenis" }))
public class ApotikPostingLink extends GeneralValueObject {
    private static final long serialVersionUID = 1L;
    public static final String PENJUALAN = "PENJUALAN";
    public static final String HPP = "HPP";

    private Long id;
    private TransaksiMedis transaksi;
    private String jenis;
    private PostingHistory postingHistory;
    private Double nilai;
    private Date waktu;
    private String oleh;
    private String olehId;
    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "transaksi", nullable = false)
    public TransaksiMedis getTransaksi() { transaksi = check(transaksi); return transaksi; }
    public void setTransaksi(TransaksiMedis transaksi) { this.transaksi = transaksi; }
    @Column(name = "jenis", nullable = false, length = 24)
    public String getJenis() { return jenis; }
    public void setJenis(String jenis) { this.jenis = jenis; }
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "posting_history", nullable = false)
    public PostingHistory getPostingHistory() { postingHistory = check(postingHistory); return postingHistory; }
    public void setPostingHistory(PostingHistory postingHistory) { this.postingHistory = postingHistory; }
    @Column(name = "nilai", nullable = false)
    public Double getNilai() { return nilai == null ? Double.valueOf(0) : nilai; }
    public void setNilai(Double nilai) { this.nilai = nilai; }
    @Temporal(TemporalType.TIMESTAMP) @Column(name = "waktu", nullable = false)
    public Date getWaktu() { return waktu; }
    public void setWaktu(Date waktu) { this.waktu = waktu; }
    public String getOleh() { return oleh; }
    public void setOleh(String oleh) { if (oleh != null && !oleh.trim().isEmpty()) this.oleh = oleh; }
    @Column(name = "oleh_id") public String getOlehId() { return olehId; }
    public void setOlehId(String olehId) { if (olehId != null && !olehId.trim().isEmpty()) this.olehId = olehId; }
    @Temporal(TemporalType.TIMESTAMP) public Date getTanggal_dirubah() { return tanggal_dirubah; }
    public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }
    @javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }
}
