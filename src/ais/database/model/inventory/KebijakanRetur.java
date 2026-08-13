package ais.database.model.inventory;

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

/**
 * Master kebijakan retur yang dapat dipilih pada setiap produk.
 *
 * <p>Baris baku bernama {@value #TANPA_KEBIJAKAN} dibuat oleh migrasi startup
 * dan dipakai untuk produk lama maupun produk baru yang belum memilih kebijakan
 * khusus.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "koperasi", name = "kebijakan_retur")
public class KebijakanRetur extends GeneralValueObject {

	private static final long serialVersionUID = 1L;
	public static final String TANPA_KEBIJAKAN = "Tanpa Kebijakan Retur";

	private Long id;
	private String nama;
	private String keterangan;
	private Boolean aktif;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	@javax.persistence.PreUpdate
	protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() { return nama == null ? "" : nama.trim(); }
	public void setNama(String nama) { this.nama = nama; }

	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() { return keterangan; }
	public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

	@Column(name = "aktif")
	public Boolean getAktif() { return aktif == null ? Boolean.TRUE : aktif; }
	public void setAktif(Boolean aktif) { this.aktif = aktif; }

	public String getOleh() { return oleh; }
	public void setOleh(String oleh) { this.oleh = oleh; }

	public String getOlehId() { return olehId; }
	public void setOlehId(String olehId) { this.olehId = olehId; }

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() { return tanggal_dirubah; }
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }

	@Override
	public String toString() { return getNama(); }
}
