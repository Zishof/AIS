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

/** Keanggotaan pelanggan apotik dan preferensi pengingat isi ulang obat. */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "apotik_customer_membership")
public class ApotikCustomerMembership extends GeneralValueObject {
	private static final long serialVersionUID = 1L;
	public static final String AKTIF = "AKTIF";
	public static final String NONAKTIF = "NONAKTIF";
	public static final String DIBLOKIR = "DIBLOKIR";

	private Long id;
	private String kode;
	private Pasien pasien;
	private String nama;
	private String telepon;
	private String tier;
	private Long poinSaldo;
	private String status;
	private Boolean consentNotifikasi;
	private String obatRutin;
	private Integer intervalRefillHari;
	private Date tanggalRefillBerikut;
	private Date tanggalDaftar;
	private String keterangan;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	@Id @GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	@Column(name = "kode", unique = true, nullable = false, length = 60)
	public String getKode() { return kode; }
	public void setKode(String kode) { this.kode = kode; }

	@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "pasien")
	public Pasien getPasien() { pasien = check(pasien); return pasien; }
	public void setPasien(Pasien pasien) { this.pasien = pasien; }

	@Column(name = "nama", nullable = false, length = 180)
	public String getNama() { return nama; }
	public void setNama(String nama) { this.nama = nama; }

	@Column(name = "telepon", length = 60)
	public String getTelepon() { return telepon; }
	public void setTelepon(String telepon) { this.telepon = telepon; }

	@Column(name = "tier", length = 30)
	public String getTier() { return tier == null ? "REGULER" : tier; }
	public void setTier(String tier) { this.tier = tier; }

	@Column(name = "poin_saldo", nullable = false)
	public Long getPoinSaldo() { return poinSaldo == null ? Long.valueOf(0) : poinSaldo; }
	public void setPoinSaldo(Long poinSaldo) { this.poinSaldo = poinSaldo; }

	@Column(name = "status", nullable = false, length = 30)
	public String getStatus() { return status == null ? AKTIF : status; }
	public void setStatus(String status) { this.status = status; }

	@Column(name = "consent_notifikasi", nullable = false)
	public Boolean getConsentNotifikasi() { return consentNotifikasi == null ? Boolean.FALSE : consentNotifikasi; }
	public void setConsentNotifikasi(Boolean consentNotifikasi) { this.consentNotifikasi = consentNotifikasi; }

	@Column(name = "obat_rutin", length = 240)
	public String getObatRutin() { return obatRutin; }
	public void setObatRutin(String obatRutin) { this.obatRutin = obatRutin; }

	@Column(name = "interval_refill_hari")
	public Integer getIntervalRefillHari() { return intervalRefillHari == null ? Integer.valueOf(0) : intervalRefillHari; }
	public void setIntervalRefillHari(Integer intervalRefillHari) { this.intervalRefillHari = intervalRefillHari; }

	@Temporal(TemporalType.DATE) @Column(name = "tanggal_refill_berikut")
	public Date getTanggalRefillBerikut() { return tanggalRefillBerikut; }
	public void setTanggalRefillBerikut(Date tanggalRefillBerikut) { this.tanggalRefillBerikut = tanggalRefillBerikut; }

	@Temporal(TemporalType.TIMESTAMP) @Column(name = "tanggal_daftar", nullable = false)
	public Date getTanggalDaftar() { return tanggalDaftar; }
	public void setTanggalDaftar(Date tanggalDaftar) { this.tanggalDaftar = tanggalDaftar; }

	@Column(name = "keterangan", length = 500)
	public String getKeterangan() { return keterangan; }
	public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

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
