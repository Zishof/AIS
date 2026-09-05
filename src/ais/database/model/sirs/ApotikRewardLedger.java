package ais.database.model.sirs;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

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

/** Ledger poin append-only; saldo pada membership adalah proyeksi cepatnya. */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "apotik_reward_ledger")
public class ApotikRewardLedger extends GeneralValueObject {
	private static final long serialVersionUID = 1L;
	private Long id;
	private ApotikCustomerMembership membership;
	private String jenis;
	private Long poin;
	private Long saldoSetelah;
	private String referensi;
	private String keterangan;
	private Date waktu;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	@Id @GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "membership", nullable = false)
	public ApotikCustomerMembership getMembership() { membership = check(membership); return membership; }
	public void setMembership(ApotikCustomerMembership membership) { this.membership = membership; }

	@Column(name = "jenis", nullable = false, length = 30)
	public String getJenis() { return jenis; }
	public void setJenis(String jenis) { this.jenis = jenis; }

	@Column(name = "poin", nullable = false)
	public Long getPoin() { return poin; }
	public void setPoin(Long poin) { this.poin = poin; }

	@Column(name = "saldo_setelah", nullable = false)
	public Long getSaldoSetelah() { return saldoSetelah; }
	public void setSaldoSetelah(Long saldoSetelah) { this.saldoSetelah = saldoSetelah; }

	@Column(name = "referensi", length = 100)
	public String getReferensi() { return referensi; }
	public void setReferensi(String referensi) { this.referensi = referensi; }

	@Column(name = "keterangan", length = 500)
	public String getKeterangan() { return keterangan; }
	public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

	@Temporal(TemporalType.TIMESTAMP) @Column(name = "waktu", nullable = false)
	public Date getWaktu() { return waktu; }
	public void setWaktu(Date waktu) { this.waktu = waktu; }

	@Column(name = "oleh", length = 200)
	public String getOleh() { return oleh; }
	public void setOleh(String oleh) { this.oleh = oleh; }

	@Column(name = "olehid", length = 200)
	public String getOlehId() { return olehId; }
	public void setOlehId(String olehId) { this.olehId = olehId; }

	@Temporal(TemporalType.TIMESTAMP) @Column(name = "tanggal_dirubah")
	public Date getTanggal_dirubah() { return tanggal_dirubah; }
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}
}
