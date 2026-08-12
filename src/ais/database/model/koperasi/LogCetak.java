package ais.database.model.koperasi;

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
 * Register riwayat cetak/ekspor (P10) -- syarat lintas-layar matriks paritas: setiap
 * cetak/ekspor tercatat pengguna, waktu, perangkat, jenis dokumen, referensi, dan parameter.
 * Append-only (tidak pernah diubah/dihapus); reprint terlihat sebagai baris baru per jenis+
 * referensi yang sama. TIDAK di-@Audited -- tabel ini sendiri sudah merupakan log.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "koperasi", name = "log_cetak")
public class LogCetak extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private String jenisDokumen;
	private String referensi;
	private String parameterJson;
	private String userId;
	private String perangkat;
	private Date waktu;

	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public LogCetak() {
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

	/** mis. kwitansi_penerimaan, laporan_sesi, laba_rugi, rekap_penjualan, voucher_hutang. */
	@Column(name = "jenis_dokumen", length = 60, nullable = false)
	public String getJenisDokumen() {
		return jenisDokumen;
	}

	public void setJenisDokumen(String jenisDokumen) {
		this.jenisDokumen = jenisDokumen;
	}

	/** Nomor/id dokumen yang dicetak (nomor kwitansi, id sesi, rentang periode, dst). */
	@Column(name = "referensi", length = 160)
	public String getReferensi() {
		return referensi;
	}

	public void setReferensi(String referensi) {
		this.referensi = referensi;
	}

	@Column(name = "parameter_json", columnDefinition = "text")
	public String getParameterJson() {
		return parameterJson;
	}

	public void setParameterJson(String parameterJson) {
		this.parameterJson = parameterJson;
	}

	@Column(name = "user_id", length = 80)
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	@Column(name = "perangkat", length = 120)
	public String getPerangkat() {
		return perangkat;
	}

	public void setPerangkat(String perangkat) {
		this.perangkat = perangkat;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu")
	public Date getWaktu() {
		return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
	}

	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
