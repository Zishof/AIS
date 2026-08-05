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

/** Entitas hasil sinkronisasi SISTER untuk endpoint <b>referensi/sdm</b>. Kolom bernama = field SISTER; {@code kode}=id item (kunci upsert); {@code keterangan}=JSON mentah. @Audited (tabel __audit auto hbm2ddl). */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "sister_ref_sdm")
public class RefSdmSister extends GeneralValueObject {
	private static final long serialVersionUID = 1L;
	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
	private String kode;
	private String keterangan;
	private Boolean aktif;
	private String namaSdm;
	private String nidn;
	private String nip;
	private String nuptk;
	private String namaStatusAktif;
	private String namaStatusPegawai;
	private String jenisSdm;

	public RefSdmSister() {}
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
	@Column(name = "nama_sdm", columnDefinition = "text") public String getNamaSdm() { return namaSdm; }
	public void setNamaSdm(String v) { this.namaSdm = v; }
	@Column(name = "nidn", columnDefinition = "text") public String getNidn() { return nidn; }
	public void setNidn(String v) { this.nidn = v; }
	@Column(name = "nip", columnDefinition = "text") public String getNip() { return nip; }
	public void setNip(String v) { this.nip = v; }
	@Column(name = "nuptk", columnDefinition = "text") public String getNuptk() { return nuptk; }
	public void setNuptk(String v) { this.nuptk = v; }
	@Column(name = "nama_status_aktif", columnDefinition = "text") public String getNamaStatusAktif() { return namaStatusAktif; }
	public void setNamaStatusAktif(String v) { this.namaStatusAktif = v; }
	@Column(name = "nama_status_pegawai", columnDefinition = "text") public String getNamaStatusPegawai() { return namaStatusPegawai; }
	public void setNamaStatusPegawai(String v) { this.namaStatusPegawai = v; }
	@Column(name = "jenis_sdm", columnDefinition = "text") public String getJenisSdm() { return jenisSdm; }
	public void setJenisSdm(String v) { this.jenisSdm = v; }
	@Override public String toString() { return id + "-" + kode; }
}
