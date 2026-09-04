package ais.database.model.sirs;

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

/**
 * Konsumsi batch obat oleh penjualan apotik (FASE A) -- {@code sisa batch =
 * Kadaluarsa.qty - SUM(konsumsi.qty)}.
 *
 * <p>TABEL BARU (bukan mengurangi {@link Kadaluarsa#getQty()} langsung) DISENGAJA:
 * {@code sirs.kadaluarsa} existing bermakna "qty per batch SAAT DITERIMA" dan dipakai
 * MonitorKadaluarsaItemAction/dashboard -- mengubah maknanya menjadi "sisa" merusak data &amp;
 * laporan lama. Ledger konsumsi append-only ini membuat sisa selalu bisa dihitung ulang
 * dan tiap pengurangan tertaut ke baris penjualan yang menyebabkannya (auditable).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "apotik_batch_konsumsi")
public class ApotikBatchKonsumsi extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private Kadaluarsa kadaluarsa;
	private TransaksiMedisDetail transaksiDetail;
	private Double qty;
	private Date waktu;

	private String oleh;
	private String olehId;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public ApotikBatchKonsumsi() {
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kadaluarsa", nullable = false)
	public Kadaluarsa getKadaluarsa() {
		kadaluarsa = check(kadaluarsa);
		return kadaluarsa;
	}

	public void setKadaluarsa(Kadaluarsa kadaluarsa) {
		this.kadaluarsa = kadaluarsa;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "transaksi_detail", nullable = false)
	public TransaksiMedisDetail getTransaksiDetail() {
		transaksiDetail = check(transaksiDetail);
		return transaksiDetail;
	}

	public void setTransaksiDetail(TransaksiMedisDetail transaksiDetail) {
		this.transaksiDetail = transaksiDetail;
	}

	@Column(name = "qty")
	public Double getQty() {
		return qty == null ? Double.valueOf(0) : qty;
	}

	public void setQty(Double qty) {
		this.qty = qty;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu")
	public Date getWaktu() {
		return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
	}

	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	@Column(name = "oleh_id")
	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
