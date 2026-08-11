package ais.database.model.koperasi;

import static javax.persistence.GenerationType.IDENTITY;

import java.math.BigDecimal;
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
 * Alokasi satu {@link PenerimaanPiutangCustomer} ke satu {@link PiutangCustomerDoc} --
 * cermin AP {@link AlokasiPembayaranHutangSupplier}. Invariant ditegakkan helper saat create:
 * &Sigma; alokasi = nominal penerimaan, dan tiap alokasi &le; outstanding fakturnya
 * (FOR UPDATE per faktur, tidak boleh overpayment).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "alokasi_penerimaan_piutang_customer")
public class AlokasiPenerimaanPiutangCustomer extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private PenerimaanPiutangCustomer penerimaan;
	private PiutangCustomerDoc piutangDoc;
	private BigDecimal nominal;

	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public AlokasiPenerimaanPiutangCustomer() {
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
	@JoinColumn(name = "penerimaan", nullable = false)
	public PenerimaanPiutangCustomer getPenerimaan() {
		return penerimaan;
	}

	public void setPenerimaan(PenerimaanPiutangCustomer penerimaan) {
		this.penerimaan = penerimaan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "piutang_doc", nullable = false)
	public PiutangCustomerDoc getPiutangDoc() {
		return piutangDoc;
	}

	public void setPiutangDoc(PiutangCustomerDoc piutangDoc) {
		this.piutangDoc = piutangDoc;
	}

	@Column(name = "nominal", precision = 19, scale = 2)
	public BigDecimal getNominal() {
		return nominal == null ? BigDecimal.ZERO : nominal;
	}

	public void setNominal(BigDecimal nominal) {
		this.nominal = nominal;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
