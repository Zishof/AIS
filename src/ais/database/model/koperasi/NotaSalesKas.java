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
 * Ledger kas sesi sales lapangan -- APPEND-ONLY (ERD &sect;3.10): tidak pernah update/delete,
 * koreksi = baris REVERSAL. Jenis: OPENING_ADVANCE, COLLECTION_CASH, CASH_SALE, EXPENSE_CASH,
 * PURCHASE_PAYMENT, OWNER_DEPOSIT, REFUND, ADJUSTMENT, REVERSAL. Saldo kas fisik seharusnya
 * SELALU dihitung dari ledger ini (bukan agregat tersimpan).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "nota_sales_kas")
public class NotaSalesKas extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	public static final String JENIS_OPENING = "OPENING_ADVANCE";
	public static final String JENIS_COLLECTION_CASH = "COLLECTION_CASH";
	public static final String JENIS_CASH_SALE = "CASH_SALE";
	public static final String JENIS_EXPENSE_CASH = "EXPENSE_CASH";
	public static final String JENIS_PURCHASE_PAYMENT = "PURCHASE_PAYMENT";
	public static final String JENIS_OWNER_DEPOSIT = "OWNER_DEPOSIT";
	public static final String JENIS_REFUND = "REFUND";
	public static final String JENIS_ADJUSTMENT = "ADJUSTMENT";
	public static final String JENIS_REVERSAL = "REVERSAL";

	private Long id;
	private NotaSalesSession sesi;
	private String jenis;
	private BigDecimal nominal;
	private String referensi;
	private String keterangan;
	private Date waktu;

	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public NotaSalesKas() {
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
	@JoinColumn(name = "sesi", nullable = false)
	public NotaSalesSession getSesi() {
		return sesi;
	}

	public void setSesi(NotaSalesSession sesi) {
		this.sesi = sesi;
	}

	@Column(name = "jenis", length = 30, nullable = false)
	public String getJenis() {
		return jenis;
	}

	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/** Bertanda: masuk kas = positif, keluar kas = negatif (EXPENSE/PURCHASE/DEPOSIT ditulis
	 *  negatif oleh helper) -- saldo = SUM(nominal) polos. */
	@Column(name = "nominal", precision = 19, scale = 2)
	public BigDecimal getNominal() {
		return nominal == null ? BigDecimal.ZERO : nominal;
	}

	public void setNominal(BigDecimal nominal) {
		this.nominal = nominal;
	}

	@Column(name = "referensi", length = 120)
	public String getReferensi() {
		return referensi;
	}

	public void setReferensi(String referensi) {
		this.referensi = referensi;
	}

	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
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
