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

/** Entitas hasil sinkronisasi SISTER untuk endpoint <b>bkd/laporan_akhir_bkd</b>. Kolom bernama = field SISTER; {@code kode}=id item (kunci upsert); {@code keterangan}=JSON mentah. @Audited (tabel __audit auto hbm2ddl). */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "sister_bkd_laporan_akhir_bkd")
public class BkdLaporanAkhirBkdSister extends GeneralValueObject {
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
	private String idRegPtk;
	private String sksKinerjaAjar;
	private String sksLebihAjar;
	private String sksKinerjaDidik;
	private String sksLebihDidik;
	private String sksKinerjaLit;
	private String sksLebihLit;
	private String sksKinerjaPengmas;
	private String sksLebihPengmas;
	private String sksKinerjaPenunjang;
	private String sksLebihTunjang;
	private String sksKinerja;
	private String sksLebih;
	private String statKewajiban;
	private String statTugas;
	private String statBelajar;
	private String idJabfung;
	private String simpulanAsesor;

	public BkdLaporanAkhirBkdSister() {}
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
	@Column(name = "id_reg_ptk", columnDefinition = "text") public String getIdRegPtk() { return idRegPtk; }
	public void setIdRegPtk(String v) { this.idRegPtk = v; }
	@Column(name = "sks_kinerja_ajar", columnDefinition = "text") public String getSksKinerjaAjar() { return sksKinerjaAjar; }
	public void setSksKinerjaAjar(String v) { this.sksKinerjaAjar = v; }
	@Column(name = "sks_lebih_ajar", columnDefinition = "text") public String getSksLebihAjar() { return sksLebihAjar; }
	public void setSksLebihAjar(String v) { this.sksLebihAjar = v; }
	@Column(name = "sks_kinerja_didik", columnDefinition = "text") public String getSksKinerjaDidik() { return sksKinerjaDidik; }
	public void setSksKinerjaDidik(String v) { this.sksKinerjaDidik = v; }
	@Column(name = "sks_lebih_didik", columnDefinition = "text") public String getSksLebihDidik() { return sksLebihDidik; }
	public void setSksLebihDidik(String v) { this.sksLebihDidik = v; }
	@Column(name = "sks_kinerja_lit", columnDefinition = "text") public String getSksKinerjaLit() { return sksKinerjaLit; }
	public void setSksKinerjaLit(String v) { this.sksKinerjaLit = v; }
	@Column(name = "sks_lebih_lit", columnDefinition = "text") public String getSksLebihLit() { return sksLebihLit; }
	public void setSksLebihLit(String v) { this.sksLebihLit = v; }
	@Column(name = "sks_kinerja_pengmas", columnDefinition = "text") public String getSksKinerjaPengmas() { return sksKinerjaPengmas; }
	public void setSksKinerjaPengmas(String v) { this.sksKinerjaPengmas = v; }
	@Column(name = "sks_lebih_pengmas", columnDefinition = "text") public String getSksLebihPengmas() { return sksLebihPengmas; }
	public void setSksLebihPengmas(String v) { this.sksLebihPengmas = v; }
	@Column(name = "sks_kinerja_penunjang", columnDefinition = "text") public String getSksKinerjaPenunjang() { return sksKinerjaPenunjang; }
	public void setSksKinerjaPenunjang(String v) { this.sksKinerjaPenunjang = v; }
	@Column(name = "sks_lebih_tunjang", columnDefinition = "text") public String getSksLebihTunjang() { return sksLebihTunjang; }
	public void setSksLebihTunjang(String v) { this.sksLebihTunjang = v; }
	@Column(name = "sks_kinerja", columnDefinition = "text") public String getSksKinerja() { return sksKinerja; }
	public void setSksKinerja(String v) { this.sksKinerja = v; }
	@Column(name = "sks_lebih", columnDefinition = "text") public String getSksLebih() { return sksLebih; }
	public void setSksLebih(String v) { this.sksLebih = v; }
	@Column(name = "stat_kewajiban", columnDefinition = "text") public String getStatKewajiban() { return statKewajiban; }
	public void setStatKewajiban(String v) { this.statKewajiban = v; }
	@Column(name = "stat_tugas", columnDefinition = "text") public String getStatTugas() { return statTugas; }
	public void setStatTugas(String v) { this.statTugas = v; }
	@Column(name = "stat_belajar", columnDefinition = "text") public String getStatBelajar() { return statBelajar; }
	public void setStatBelajar(String v) { this.statBelajar = v; }
	@Column(name = "id_jabfung", columnDefinition = "text") public String getIdJabfung() { return idJabfung; }
	public void setIdJabfung(String v) { this.idJabfung = v; }
	@Column(name = "simpulan_asesor", columnDefinition = "text") public String getSimpulanAsesor() { return simpulanAsesor; }
	public void setSimpulanAsesor(String v) { this.simpulanAsesor = v; }
	@Override public String toString() { return id + "-" + kode; }
}
