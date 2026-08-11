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
import ais.database.model.inventory.PengadaanFaktur;

/**
 * Alokasi satu {@link PembayaranHutangSupplier} ke satu {@link PengadaanFaktur} (layar legacy
 * 24: satu pembayaran boleh melunasi banyak faktur; satu faktur boleh dibayar bertahap).
 * Invariant (ditegakkan atomik di helper): SUM(alokasi per pembayaran) = nominal pembayaran;
 * tiap alokasi &le; outstanding faktur saat itu.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "alokasi_pembayaran_hutang_supplier")
public class AlokasiPembayaranHutangSupplier extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private PembayaranHutangSupplier pembayaran;
	private PengadaanFaktur pengadaanFaktur;
	private BigDecimal nominal;

	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public AlokasiPembayaranHutangSupplier() {
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
	@JoinColumn(name = "pembayaran", nullable = false)
	public PembayaranHutangSupplier getPembayaran() {
		return pembayaran;
	}

	public void setPembayaran(PembayaranHutangSupplier pembayaran) {
		this.pembayaran = pembayaran;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pengadaan_faktur", nullable = false)
	public PengadaanFaktur getPengadaanFaktur() {
		return pengadaanFaktur;
	}

	public void setPengadaanFaktur(PengadaanFaktur pengadaanFaktur) {
		this.pengadaanFaktur = pengadaanFaktur;
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
