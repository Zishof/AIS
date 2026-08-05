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

/** Entitas hasil sinkronisasi SISTER untuk endpoint <b>data_pribadi/alamat</b>. Kolom bernama = field SISTER; {@code kode}=id item (kunci upsert); {@code keterangan}=JSON mentah. @Audited (tabel __audit auto hbm2ddl). */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "sister_dp_alamat")
public class DpAlamatSister extends GeneralValueObject {
	private static final long serialVersionUID = 1L;
	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
	private String kode;
	private String keterangan;
	private Boolean aktif;
	private String idSdm;
	private String email;
	private String alamat;
	private Integer rt;
	private Integer rw;
	private String dusun;
	private String kelurahan;
	private String kotaKabupaten;
	private String idKotaKabupaten;
	private String kodePos;
	private String teleponRumah;
	private String teleponHp;

	public DpAlamatSister() {}
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
	@Column(name = "email", columnDefinition = "text") public String getEmail() { return email; }
	public void setEmail(String v) { this.email = v; }
	@Column(name = "alamat", columnDefinition = "text") public String getAlamat() { return alamat; }
	public void setAlamat(String v) { this.alamat = v; }
	@Column(name = "rt") public Integer getRt() { return rt; }
	public void setRt(Integer v) { this.rt = v; }
	@Column(name = "rw") public Integer getRw() { return rw; }
	public void setRw(Integer v) { this.rw = v; }
	@Column(name = "dusun", columnDefinition = "text") public String getDusun() { return dusun; }
	public void setDusun(String v) { this.dusun = v; }
	@Column(name = "kelurahan", columnDefinition = "text") public String getKelurahan() { return kelurahan; }
	public void setKelurahan(String v) { this.kelurahan = v; }
	@Column(name = "kota_kabupaten", columnDefinition = "text") public String getKotaKabupaten() { return kotaKabupaten; }
	public void setKotaKabupaten(String v) { this.kotaKabupaten = v; }
	@Column(name = "id_kota_kabupaten", columnDefinition = "text") public String getIdKotaKabupaten() { return idKotaKabupaten; }
	public void setIdKotaKabupaten(String v) { this.idKotaKabupaten = v; }
	@Column(name = "kode_pos", columnDefinition = "text") public String getKodePos() { return kodePos; }
	public void setKodePos(String v) { this.kodePos = v; }
	@Column(name = "telepon_rumah", columnDefinition = "text") public String getTeleponRumah() { return teleponRumah; }
	public void setTeleponRumah(String v) { this.teleponRumah = v; }
	@Column(name = "telepon_hp", columnDefinition = "text") public String getTeleponHp() { return teleponHp; }
	public void setTeleponHp(String v) { this.teleponHp = v; }
	@Override public String toString() { return id + "-" + kode; }
}
