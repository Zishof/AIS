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

/** Entitas hasil sinkronisasi SISTER untuk endpoint <b>trid/bahan_ajar</b>. Kolom bernama = field SISTER; {@code kode}=id item (kunci upsert); {@code keterangan}=JSON mentah. @Audited (tabel __audit auto hbm2ddl). */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "sister_trid_bahan_ajar")
public class TridBahanAjarSister extends GeneralValueObject {
	private static final long serialVersionUID = 1L;
	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
	private String kode;
	private String keterangan;
	private Boolean aktif;
	private String idSdm;
	private String judul;
	private String isbn;
	private String namaJenis;
	private String namaPenerbit;
	private String tanggalTerbit;

	public TridBahanAjarSister() {}
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
	@Column(name = "judul", columnDefinition = "text") public String getJudul() { return judul; }
	public void setJudul(String v) { this.judul = v; }
	@Column(name = "isbn", columnDefinition = "text") public String getIsbn() { return isbn; }
	public void setIsbn(String v) { this.isbn = v; }
	@Column(name = "nama_jenis", columnDefinition = "text") public String getNamaJenis() { return namaJenis; }
	public void setNamaJenis(String v) { this.namaJenis = v; }
	@Column(name = "nama_penerbit", columnDefinition = "text") public String getNamaPenerbit() { return namaPenerbit; }
	public void setNamaPenerbit(String v) { this.namaPenerbit = v; }
	@Column(name = "tanggal_terbit", columnDefinition = "text") public String getTanggalTerbit() { return tanggalTerbit; }
	public void setTanggalTerbit(String v) { this.tanggalTerbit = v; }
	@Override public String toString() { return id + "-" + kode; }
}
