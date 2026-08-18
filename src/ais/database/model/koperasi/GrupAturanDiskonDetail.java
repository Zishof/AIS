package ais.database.model.koperasi;

import static javax.persistence.GenerationType.IDENTITY;
import java.util.Date;
import javax.persistence.*;
import ais.database.model.GeneralValueObject;

/** Produk yang menjadi anggota satu grup aturan diskon. */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert=true, dynamicUpdate=true)
@Table(schema="koperasi", name="grup_aturan_diskon_detail")
public class GrupAturanDiskonDetail extends GeneralValueObject {
    private static final long serialVersionUID=1L;
    private Long id, grupAturanDiskon, produk;
    private Boolean aktif;
    private String oleh, olehId;
    private Date tanggal_dirubah=ais.ui.util.WaktuUtil.getDate();
    @PreUpdate protected void onUpdate(){ ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }
    @Id @GeneratedValue(strategy=IDENTITY) @Column(name="id", insertable=false, unique=true, nullable=false)
    public Long getId(){ return id; } public void setId(Long v){ id=v; }
    @Column(name="grup_aturan_diskon", nullable=false) public Long getGrupAturanDiskon(){ return grupAturanDiskon; }
    public void setGrupAturanDiskon(Long v){ grupAturanDiskon=v; }
    @Column(name="produk", nullable=false) public Long getProduk(){ return produk; } public void setProduk(Long v){ produk=v; }
    @Column(name="aktif") public Boolean getAktif(){ return aktif == null ? Boolean.TRUE : aktif; } public void setAktif(Boolean v){ aktif=v; }
    public String getOleh(){ return oleh; } public void setOleh(String v){ oleh=v; }
    public String getOlehId(){ return olehId; } public void setOlehId(String v){ olehId=v; }
    @Temporal(TemporalType.TIMESTAMP) public Date getTanggal_dirubah(){ return tanggal_dirubah; } public void setTanggal_dirubah(Date v){ tanggal_dirubah=v; }
}
