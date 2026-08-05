package ais.database.model.sister;

import static javax.persistence.GenerationType.IDENTITY;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.hibernate.envers.Audited;
import ais.database.model.GeneralValueObject;

/** Entitas hasil sinkronisasi SISTER untuk endpoint <b>bkd/tunjang</b>. Kolom bernama = field SISTER; {@code kode}=id item (kunci upsert); {@code keterangan}=JSON mentah. @Audited (tabel __audit auto hbm2ddl). */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "sister_bkd_tunjang")
public class BkdTunjangSister extends GeneralValueObject {
	private static final long serialVersionUID = 1L;
	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
	private String kode;
	private String keterangan;
	private Boolean aktif;
	private String idSdm;
	private String idSmt;
	private String nmSdm;
	private String nidn;
	private String unsur;
	private String judulKeg;
	private Integer idKatgiat;
	private String nmKat;
	private Double bebanSks;
	private Double nilai;

	public BkdTunjangSister() {}
	@Id @GeneratedValue(strategy = IDENTITY) @Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return this.id; }
	public void setId(Long id) { this.id = id; }
	public String getOlehId() { return olehId; }
	public void setOlehId(String olehId) { if (olehId==null||olehId.trim().isEmpty()) return; this.olehId = olehId; }
	public String getOleh() { return oleh; }
	public void setOleh(String oleh) { if (oleh==null||oleh.trim().isEmpty()) return; this.oleh = oleh; }
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }
	@Temporal(TemporalType.TIMESTAMP) public Date getTanggal_dirubah() { return tanggal_dirubah; }
	public void setTanggal_dirubah(Date t) { this.tanggal_dirubah = t; }
	@Column(name = "kode") public String getKode() { return kode==null||kode.isEmpty()?null:kode.trim(); }
	public void setKode(String kode) { this.kode = kode; }
	@Column(name = "keterangan", columnDefinition = "text") public String getKeterangan() { return keterangan; }
	public void setKeterangan(String k) { this.keterangan = k; }
	@Column(name = "aktif") public Boolean getAktif() { return aktif==null?true:aktif; }
	public void setAktif(Boolean a) { this.aktif = a; }
	@Column(name = "id_sdm", columnDefinition = "text") public String getIdSdm() { return idSdm; }
	public void setIdSdm(String v) { this.idSdm = v; }
	@Column(name = "id_smt", columnDefinition = "text") public String getIdSmt() { return idSmt; }
	public void setIdSmt(String v) { this.idSmt = v; }
	@Column(name = "nm_sdm", columnDefinition = "text") public String getNmSdm() { return nmSdm; }
	public void setNmSdm(String v) { this.nmSdm = v; }
	@Column(name = "nidn", columnDefinition = "text") public String getNidn() { return nidn; }
	public void setNidn(String v) { this.nidn = v; }
	@Column(name = "unsur", columnDefinition = "text") public String getUnsur() { return unsur; }
	public void setUnsur(String v) { this.unsur = v; }
	@Column(name = "judul_keg", columnDefinition = "text") public String getJudulKeg() { return judulKeg; }
	public void setJudulKeg(String v) { this.judulKeg = v; }
	@Column(name = "id_katgiat") public Integer getIdKatgiat() { return idKatgiat; }
	public void setIdKatgiat(Integer v) { this.idKatgiat = v; }
	@Column(name = "nm_kat", columnDefinition = "text") public String getNmKat() { return nmKat; }
	public void setNmKat(String v) { this.nmKat = v; }
	@Column(name = "beban_sks") public Double getBebanSks() { return bebanSks; }
	public void setBebanSks(Double v) { this.bebanSks = v; }
	@Column(name = "nilai") public Double getNilai() { return nilai; }
	public void setNilai(Double v) { this.nilai = v; }
	@Override public String toString() { return id + "-" + kode; }
}
