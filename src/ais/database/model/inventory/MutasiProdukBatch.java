package ais.database.model.inventory;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/** Buku besar perubahan saldo satu batch produk. */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "mutasi_produk_batch")
public class MutasiProdukBatch extends GeneralValueObject {
	private static final long serialVersionUID = 1L;
	private Long id;
	private ProdukBatch batch;
	private Date waktu;
	private String jenis;
	private Double masuk;
	private Double keluar;
	private Double saldo;
	private String referensi;
	private String keterangan;
	private String oleh;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	@Id @GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "batch", nullable = false)
	public ProdukBatch getBatch() { batch = check(batch); return batch; }
	public void setBatch(ProdukBatch batch) { this.batch = batch; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(nullable = false)
	public Date getWaktu() { return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu; }
	public void setWaktu(Date waktu) { this.waktu = waktu; }
	public String getJenis() { return jenis; }
	public void setJenis(String jenis) { this.jenis = jenis; }
	public Double getMasuk() { return masuk == null ? 0.0 : masuk; }
	public void setMasuk(Double masuk) { this.masuk = masuk; }
	public Double getKeluar() { return keluar == null ? 0.0 : keluar; }
	public void setKeluar(Double keluar) { this.keluar = keluar; }
	public Double getSaldo() { return saldo == null ? 0.0 : saldo; }
	public void setSaldo(Double saldo) { this.saldo = saldo; }
	public String getReferensi() { return referensi; }
	public void setReferensi(String referensi) { this.referensi = referensi; }
	@Column(columnDefinition = "text")
	public String getKeterangan() { return keterangan; }
	public void setKeterangan(String keterangan) { this.keterangan = keterangan; }
	public String getOleh() { return oleh; }
	public void setOleh(String oleh) { this.oleh = oleh; }
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() { return tanggal_dirubah; }
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }
	@javax.persistence.PreUpdate
	protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }
}
