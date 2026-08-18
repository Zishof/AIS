package ais.database.model.koperasi;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import ais.database.model.GeneralValueObject;

/** Header promo massal. JSON kriteria menyimpan pilihan multi jenis/tipe member. */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "koperasi", name = "grup_aturan_diskon")
public class GrupAturanDiskon extends GeneralValueObject {
    private static final long serialVersionUID = 1L;
    private Long id, toko, jenisAnggota, tipeAnggota;
    private String namaGrup, keterangan, hariAktif, detailJson, jenisMemberJson, tipeMemberJson, oleh, olehId,
            dasarPerhitungan, grupEksklusif;
    private Boolean berlakuSemuaMember, khususMember, potonganLangsung, aktif, dapatDigabung;
    private Integer prioritas;
    private Double persentase, maksimalPotongan, nominal, cashback;
    private Date tanggalMulai, tanggalSelesai, tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

    @javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }
    @Id @GeneratedValue(strategy = IDENTITY) @Column(name="id", insertable=false, unique=true, nullable=false)
    public Long getId(){ return id; } public void setId(Long v){ id=v; }
    @Column(name="nama_grup", nullable=false, length=255) public String getNamaGrup(){ return namaGrup; }
    public void setNamaGrup(String v){ namaGrup=v; }
    @Column(name="keterangan", columnDefinition="text") public String getKeterangan(){ return keterangan; }
    public void setKeterangan(String v){ keterangan=v; }
    @Column(name="toko") public Long getToko(){ return toko; } public void setToko(Long v){ toko=v; }
    @Column(name="jenis_anggota") public Long getJenisAnggota(){ return jenisAnggota; } public void setJenisAnggota(Long v){ jenisAnggota=v; }
    @Column(name="tipe_anggota") public Long getTipeAnggota(){ return tipeAnggota; } public void setTipeAnggota(Long v){ tipeAnggota=v; }
    @Column(name="berlaku_semua_member") public Boolean getBerlakuSemuaMember(){ return berlakuSemuaMember == null ? Boolean.TRUE : berlakuSemuaMember; }
    public void setBerlakuSemuaMember(Boolean v){ berlakuSemuaMember=v; }
    @Column(name="khusus_member") public Boolean getKhususMember(){ return khususMember == null ? Boolean.FALSE : khususMember; }
    public void setKhususMember(Boolean v){ khususMember=v; }
    @Column(name="jenis_member_json", columnDefinition="text") public String getJenisMemberJson(){ return jenisMemberJson; }
    public void setJenisMemberJson(String v){ jenisMemberJson=v; }
    @Column(name="tipe_member_json", columnDefinition="text") public String getTipeMemberJson(){ return tipeMemberJson; }
    public void setTipeMemberJson(String v){ tipeMemberJson=v; }
    @Column(name="persentase") public Double getPersentase(){ return persentase == null ? 0D : persentase; }
    public void setPersentase(Double v){ persentase=v; }
    @Column(name="maksimal_potongan") public Double getMaksimalPotongan(){ return maksimalPotongan == null ? 0D : maksimalPotongan; }
    public void setMaksimalPotongan(Double v){ maksimalPotongan=v; }
    @Column(name="nominal") public Double getNominal(){ return nominal == null ? 0D : nominal; }
    public void setNominal(Double v){ nominal=v; }
    @Column(name="cashback") public Double getCashback(){ return cashback == null ? 0D : cashback; }
    public void setCashback(Double v){ cashback=v; }
    @Column(name="prioritas", nullable=false) public Integer getPrioritas(){ return prioritas == null ? 100 : prioritas; }
    public void setPrioritas(Integer v){ prioritas=v; }
    @Column(name="dapat_digabung", nullable=false) public Boolean getDapatDigabung(){ return dapatDigabung == null ? Boolean.FALSE : dapatDigabung; }
    public void setDapatDigabung(Boolean v){ dapatDigabung=v; }
    @Column(name="dasar_perhitungan", nullable=false, length=30) public String getDasarPerhitungan(){ return dasarPerhitungan == null || dasarPerhitungan.trim().isEmpty() ? "SETELAH_DISKON" : dasarPerhitungan; }
    public void setDasarPerhitungan(String v){ dasarPerhitungan=v; }
    @Column(name="grup_eksklusif", length=100) public String getGrupEksklusif(){ return grupEksklusif; }
    public void setGrupEksklusif(String v){ grupEksklusif=v; }
    @Column(name="potongan_langsung") public Boolean getPotonganLangsung(){ return potonganLangsung == null ? Boolean.TRUE : potonganLangsung; }
    public void setPotonganLangsung(Boolean v){ potonganLangsung=v; }
    @Temporal(TemporalType.TIMESTAMP) @Column(name="tanggal_mulai") public Date getTanggalMulai(){ return tanggalMulai; }
    public void setTanggalMulai(Date v){ tanggalMulai=v; }
    @Temporal(TemporalType.TIMESTAMP) @Column(name="tanggal_selesai") public Date getTanggalSelesai(){ return tanggalSelesai; }
    public void setTanggalSelesai(Date v){ tanggalSelesai=v; }
    @Column(name="hari_aktif", length=20) public String getHariAktif(){ return hariAktif; } public void setHariAktif(String v){ hariAktif=v; }
    @Column(name="aktif") public Boolean getAktif(){ return aktif == null ? Boolean.TRUE : aktif; } public void setAktif(Boolean v){ aktif=v; }
    @Column(name="detail_json", columnDefinition="text") public String getDetailJson(){ return detailJson; } public void setDetailJson(String v){ detailJson=v; }
    public String getOleh(){ return oleh; } public void setOleh(String v){ oleh=v; }
    public String getOlehId(){ return olehId; } public void setOlehId(String v){ olehId=v; }
    @Temporal(TemporalType.TIMESTAMP) public Date getTanggal_dirubah(){ return tanggal_dirubah; } public void setTanggal_dirubah(Date v){ tanggal_dirubah=v; }
}
