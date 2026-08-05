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

/** Entitas hasil sinkronisasi SISTER untuk endpoint <b>trid/sertifikasi_profesi</b>. Kolom bernama = field SISTER; {@code kode}=id item (kunci upsert); {@code keterangan}=JSON mentah. @Audited (tabel __audit auto hbm2ddl). */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "sister_trid_sertifikasi_profesi")
public class TridSertifikasiProfesiSister extends GeneralValueObject {
	private static final long serialVersionUID = 1L;
	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
	private String kode;
	private String keterangan;
	private Boolean aktif;
	private String idSdm;
	private String jenisSertifikasi;
	private String bidangStudi;
	private String skSertifikasi;
	private Integer idLembagaSertifikasi;
	private String namaLembagaSertifikasi;
	private String terhitungMulaiTanggal;
	private String terhitungSampaiTanggal;
	private String nomorRegistrasi;
	private Integer tahunSertifikasi;

	public TridSertifikasiProfesiSister() {}
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
	@Column(name = "jenis_sertifikasi", columnDefinition = "text") public String getJenisSertifikasi() { return jenisSertifikasi; }
	public void setJenisSertifikasi(String v) { this.jenisSertifikasi = v; }
	@Column(name = "bidang_studi", columnDefinition = "text") public String getBidangStudi() { return bidangStudi; }
	public void setBidangStudi(String v) { this.bidangStudi = v; }
	@Column(name = "sk_sertifikasi", columnDefinition = "text") public String getSkSertifikasi() { return skSertifikasi; }
	public void setSkSertifikasi(String v) { this.skSertifikasi = v; }
	@Column(name = "id_lembaga_sertifikasi") public Integer getIdLembagaSertifikasi() { return idLembagaSertifikasi; }
	public void setIdLembagaSertifikasi(Integer v) { this.idLembagaSertifikasi = v; }
	@Column(name = "nama_lembaga_sertifikasi", columnDefinition = "text") public String getNamaLembagaSertifikasi() { return namaLembagaSertifikasi; }
	public void setNamaLembagaSertifikasi(String v) { this.namaLembagaSertifikasi = v; }
	@Column(name = "terhitung_mulai_tanggal", columnDefinition = "text") public String getTerhitungMulaiTanggal() { return terhitungMulaiTanggal; }
	public void setTerhitungMulaiTanggal(String v) { this.terhitungMulaiTanggal = v; }
	@Column(name = "terhitung_sampai_tanggal", columnDefinition = "text") public String getTerhitungSampaiTanggal() { return terhitungSampaiTanggal; }
	public void setTerhitungSampaiTanggal(String v) { this.terhitungSampaiTanggal = v; }
	@Column(name = "nomor_registrasi", columnDefinition = "text") public String getNomorRegistrasi() { return nomorRegistrasi; }
	public void setNomorRegistrasi(String v) { this.nomorRegistrasi = v; }
	@Column(name = "tahun_sertifikasi") public Integer getTahunSertifikasi() { return tahunSertifikasi; }
	public void setTahunSertifikasi(Integer v) { this.tahunSertifikasi = v; }
	@Override public String toString() { return id + "-" + kode; }
}
