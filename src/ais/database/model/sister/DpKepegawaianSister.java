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

/** Entitas hasil sinkronisasi SISTER untuk endpoint <b>data_pribadi/kepegawaian</b>. Kolom bernama = field SISTER; {@code kode}=id item (kunci upsert); {@code keterangan}=JSON mentah. @Audited (tabel __audit auto hbm2ddl). */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "sister_dp_kepegawaian")
public class DpKepegawaianSister extends GeneralValueObject {
	private static final long serialVersionUID = 1L;
	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
	private String kode;
	private String keterangan;
	private Boolean aktif;
	private String idSdm;
	private String nip;
	private String skCpns;
	private String tanggalSkCpns;
	private String skTmmd;
	private String tmmd;
	private Integer idSumberGaji;
	private String sumberGaji;
	private String nidn;
	private String nuptk;

	public DpKepegawaianSister() {}
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
	@Column(name = "nip", columnDefinition = "text") public String getNip() { return nip; }
	public void setNip(String v) { this.nip = v; }
	@Column(name = "sk_cpns", columnDefinition = "text") public String getSkCpns() { return skCpns; }
	public void setSkCpns(String v) { this.skCpns = v; }
	@Column(name = "tanggal_sk_cpns", columnDefinition = "text") public String getTanggalSkCpns() { return tanggalSkCpns; }
	public void setTanggalSkCpns(String v) { this.tanggalSkCpns = v; }
	@Column(name = "sk_tmmd", columnDefinition = "text") public String getSkTmmd() { return skTmmd; }
	public void setSkTmmd(String v) { this.skTmmd = v; }
	@Column(name = "tmmd", columnDefinition = "text") public String getTmmd() { return tmmd; }
	public void setTmmd(String v) { this.tmmd = v; }
	@Column(name = "id_sumber_gaji") public Integer getIdSumberGaji() { return idSumberGaji; }
	public void setIdSumberGaji(Integer v) { this.idSumberGaji = v; }
	@Column(name = "sumber_gaji", columnDefinition = "text") public String getSumberGaji() { return sumberGaji; }
	public void setSumberGaji(String v) { this.sumberGaji = v; }
	@Column(name = "nidn", columnDefinition = "text") public String getNidn() { return nidn; }
	public void setNidn(String v) { this.nidn = v; }
	@Column(name = "nuptk", columnDefinition = "text") public String getNuptk() { return nuptk; }
	public void setNuptk(String v) { this.nuptk = v; }
	@Override public String toString() { return id + "-" + kode; }
}
