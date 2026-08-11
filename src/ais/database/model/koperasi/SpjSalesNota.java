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
 * Nota/invoice piutang dibawa per SPJ (layar legacy 39-42, ERD &sect;3.4). Satu invoice
 * TIDAK boleh aktif dibawa dua SPJ berbeda (ditegakkan helper saat assign: cek baris lain
 * ber-status belum-final utk piutang_doc yang sama). Status hasil kunjungan mengikuti state
 * machine ERD: CARRIED -> UNPAID | PROMISE_TO_PAY | PARTIAL_COLLECTED | PAID | RETURNED |
 * DISPUTED | LOST -> RECONCILED. {@code nilaiTertagih} diakumulasi dari collection ber-sesi.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "spj_sales_nota")
public class SpjSalesNota extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	public static final String STATUS_ASSIGNED = "ASSIGNED";
	public static final String STATUS_CARRIED = "CARRIED";
	public static final String STATUS_UNPAID = "UNPAID";
	public static final String STATUS_PROMISE = "PROMISE_TO_PAY";
	public static final String STATUS_PARTIAL = "PARTIAL_COLLECTED";
	public static final String STATUS_PAID = "PAID";
	public static final String STATUS_RETURNED = "RETURNED";
	public static final String STATUS_DISPUTED = "DISPUTED";
	public static final String STATUS_LOST = "LOST";
	public static final String STATUS_RECONCILED = "RECONCILED";

	private Long id;
	private SuratPerintahSalesJalan spj;
	private PiutangCustomerDoc piutangDoc;
	private AnggotaKoperasi customer;
	private BigDecimal nilaiAwal;
	private BigDecimal saldoSaatAssign;
	private Date jatuhTempo;
	private String status;
	private String hasilKunjungan;
	private Date janjiBayar;
	private String alasanGagal;
	private BigDecimal nilaiTertagih;

	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public SpjSalesNota() {
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
	@JoinColumn(name = "spj", nullable = false)
	public SuratPerintahSalesJalan getSpj() {
		return spj;
	}

	public void setSpj(SuratPerintahSalesJalan spj) {
		this.spj = spj;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "piutang_doc", nullable = false)
	public PiutangCustomerDoc getPiutangDoc() {
		return piutangDoc;
	}

	public void setPiutangDoc(PiutangCustomerDoc piutangDoc) {
		this.piutangDoc = piutangDoc;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "customer", nullable = false)
	public AnggotaKoperasi getCustomer() {
		return customer;
	}

	public void setCustomer(AnggotaKoperasi customer) {
		this.customer = customer;
	}

	@Column(name = "nilai_awal", precision = 19, scale = 2)
	public BigDecimal getNilaiAwal() {
		return nilaiAwal == null ? BigDecimal.ZERO : nilaiAwal;
	}

	public void setNilaiAwal(BigDecimal nilaiAwal) {
		this.nilaiAwal = nilaiAwal;
	}

	/** Outstanding faktur SAAT di-assign (snapshot -- bukti berapa yang dibawa sales). */
	@Column(name = "saldo_saat_assign", precision = 19, scale = 2)
	public BigDecimal getSaldoSaatAssign() {
		return saldoSaatAssign == null ? BigDecimal.ZERO : saldoSaatAssign;
	}

	public void setSaldoSaatAssign(BigDecimal saldoSaatAssign) {
		this.saldoSaatAssign = saldoSaatAssign;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "jatuh_tempo")
	public Date getJatuhTempo() {
		return jatuhTempo;
	}

	public void setJatuhTempo(Date jatuhTempo) {
		this.jatuhTempo = jatuhTempo;
	}

	@Column(name = "status", length = 30)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_ASSIGNED : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Column(name = "hasil_kunjungan", columnDefinition = "text")
	public String getHasilKunjungan() {
		return hasilKunjungan;
	}

	public void setHasilKunjungan(String hasilKunjungan) {
		this.hasilKunjungan = hasilKunjungan;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "janji_bayar")
	public Date getJanjiBayar() {
		return janjiBayar;
	}

	public void setJanjiBayar(Date janjiBayar) {
		this.janjiBayar = janjiBayar;
	}

	@Column(name = "alasan_gagal", columnDefinition = "text")
	public String getAlasanGagal() {
		return alasanGagal;
	}

	public void setAlasanGagal(String alasanGagal) {
		this.alasanGagal = alasanGagal;
	}

	@Column(name = "nilai_tertagih", precision = 19, scale = 2)
	public BigDecimal getNilaiTertagih() {
		return nilaiTertagih == null ? BigDecimal.ZERO : nilaiTertagih;
	}

	public void setNilaiTertagih(BigDecimal nilaiTertagih) {
		this.nilaiTertagih = nilaiTertagih;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
