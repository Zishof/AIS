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

/** Entitas hasil sinkronisasi SISTER untuk endpoint <b>referensi/profil_pt</b>. Kolom bernama = field SISTER; {@code kode}=id item (kunci upsert); {@code keterangan}=JSON mentah. @Audited (tabel __audit auto hbm2ddl). */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "sister_ref_profil_pt")
public class RefProfilPtSister extends GeneralValueObject {
	private static final long serialVersionUID = 1L;
	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
	private String kode;
	private String keterangan;
	private Boolean aktif;
	private String kodePerguruanTinggi;
	private String namaPerguruanTinggi;
	private String telepon;
	private String faximile;
	private String email;
	private String website;
	private String jalan;
	private String dusun;
	private Integer rt;
	private Integer rw;
	private String kelurahan;
	private String kodePos;
	private String idWilayah;
	private String namaWilayah;
	private Double lintang;
	private Double bujur;
	private String skPendirian;
	private String tanggalSkPendirian;
	private String idStatusMilik;
	private String namaStatusMilik;
	private String statusPerguruanTinggi;
	private String skIzinOperasional;
	private String tanggalIzinOperasional;

	public RefProfilPtSister() {}
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
	@Column(name = "kode_perguruan_tinggi", columnDefinition = "text") public String getKodePerguruanTinggi() { return kodePerguruanTinggi; }
	public void setKodePerguruanTinggi(String v) { this.kodePerguruanTinggi = v; }
	@Column(name = "nama_perguruan_tinggi", columnDefinition = "text") public String getNamaPerguruanTinggi() { return namaPerguruanTinggi; }
	public void setNamaPerguruanTinggi(String v) { this.namaPerguruanTinggi = v; }
	@Column(name = "telepon", columnDefinition = "text") public String getTelepon() { return telepon; }
	public void setTelepon(String v) { this.telepon = v; }
	@Column(name = "faximile", columnDefinition = "text") public String getFaximile() { return faximile; }
	public void setFaximile(String v) { this.faximile = v; }
	@Column(name = "email", columnDefinition = "text") public String getEmail() { return email; }
	public void setEmail(String v) { this.email = v; }
	@Column(name = "website", columnDefinition = "text") public String getWebsite() { return website; }
	public void setWebsite(String v) { this.website = v; }
	@Column(name = "jalan", columnDefinition = "text") public String getJalan() { return jalan; }
	public void setJalan(String v) { this.jalan = v; }
	@Column(name = "dusun", columnDefinition = "text") public String getDusun() { return dusun; }
	public void setDusun(String v) { this.dusun = v; }
	@Column(name = "rt") public Integer getRt() { return rt; }
	public void setRt(Integer v) { this.rt = v; }
	@Column(name = "rw") public Integer getRw() { return rw; }
	public void setRw(Integer v) { this.rw = v; }
	@Column(name = "kelurahan", columnDefinition = "text") public String getKelurahan() { return kelurahan; }
	public void setKelurahan(String v) { this.kelurahan = v; }
	@Column(name = "kode_pos", columnDefinition = "text") public String getKodePos() { return kodePos; }
	public void setKodePos(String v) { this.kodePos = v; }
	@Column(name = "id_wilayah", columnDefinition = "text") public String getIdWilayah() { return idWilayah; }
	public void setIdWilayah(String v) { this.idWilayah = v; }
	@Column(name = "nama_wilayah", columnDefinition = "text") public String getNamaWilayah() { return namaWilayah; }
	public void setNamaWilayah(String v) { this.namaWilayah = v; }
	@Column(name = "lintang") public Double getLintang() { return lintang; }
	public void setLintang(Double v) { this.lintang = v; }
	@Column(name = "bujur") public Double getBujur() { return bujur; }
	public void setBujur(Double v) { this.bujur = v; }
	@Column(name = "sk_pendirian", columnDefinition = "text") public String getSkPendirian() { return skPendirian; }
	public void setSkPendirian(String v) { this.skPendirian = v; }
	@Column(name = "tanggal_sk_pendirian", columnDefinition = "text") public String getTanggalSkPendirian() { return tanggalSkPendirian; }
	public void setTanggalSkPendirian(String v) { this.tanggalSkPendirian = v; }
	@Column(name = "id_status_milik", columnDefinition = "text") public String getIdStatusMilik() { return idStatusMilik; }
	public void setIdStatusMilik(String v) { this.idStatusMilik = v; }
	@Column(name = "nama_status_milik", columnDefinition = "text") public String getNamaStatusMilik() { return namaStatusMilik; }
	public void setNamaStatusMilik(String v) { this.namaStatusMilik = v; }
	@Column(name = "status_perguruan_tinggi", columnDefinition = "text") public String getStatusPerguruanTinggi() { return statusPerguruanTinggi; }
	public void setStatusPerguruanTinggi(String v) { this.statusPerguruanTinggi = v; }
	@Column(name = "sk_izin_operasional", columnDefinition = "text") public String getSkIzinOperasional() { return skIzinOperasional; }
	public void setSkIzinOperasional(String v) { this.skIzinOperasional = v; }
	@Column(name = "tanggal_izin_operasional", columnDefinition = "text") public String getTanggalIzinOperasional() { return tanggalIzinOperasional; }
	public void setTanggalIzinOperasional(String v) { this.tanggalIzinOperasional = v; }
	@Override public String toString() { return id + "-" + kode; }
}
