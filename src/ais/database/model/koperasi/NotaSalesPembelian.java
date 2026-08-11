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
import ais.database.model.library.Penyedia;

/**
 * Pembelian/kulakan dalam sesi sales lapangan (ERD &sect;3.9) -- LINK ke faktur Kulakan
 * existing ({@link PengadaanFaktur}), bukan duplikasi. Hanya {@code dibayarSesi} (kas/DP
 * aktual saat sesi) yang mengurangi hasil bersih sesi; bagian kredit dilaporkan terpisah
 * sebagai hutang baru (sisaHutang) -- rumus ERD &sect;4.1.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "nota_sales_pembelian")
public class NotaSalesPembelian extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	public static final String TUJUAN_MOBIL_SALES = "MOBIL_SALES";
	public static final String TUJUAN_GUDANG = "GUDANG";

	private Long id;
	private NotaSalesSession sesi;
	private PengadaanFaktur pengadaanFaktur;
	private Penyedia supplier;
	private BigDecimal totalFaktur;
	private BigDecimal dibayarSesi;
	private BigDecimal sisaHutang;
	private String tujuanStok;
	private String keterangan;
	private String kodeUnik;

	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public NotaSalesPembelian() {
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

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pengadaan_faktur")
	public PengadaanFaktur getPengadaanFaktur() {
		return pengadaanFaktur;
	}

	public void setPengadaanFaktur(PengadaanFaktur pengadaanFaktur) {
		this.pengadaanFaktur = pengadaanFaktur;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "supplier")
	public Penyedia getSupplier() {
		return supplier;
	}

	public void setSupplier(Penyedia supplier) {
		this.supplier = supplier;
	}

	@Column(name = "total_faktur", precision = 19, scale = 2)
	public BigDecimal getTotalFaktur() {
		return totalFaktur == null ? BigDecimal.ZERO : totalFaktur;
	}

	public void setTotalFaktur(BigDecimal totalFaktur) {
		this.totalFaktur = totalFaktur;
	}

	/** Kas/transfer/DP yang BENAR-BENAR dibayar pada sesi (satu-satunya pengurang hasil bersih). */
	@Column(name = "dibayar_sesi", precision = 19, scale = 2)
	public BigDecimal getDibayarSesi() {
		return dibayarSesi == null ? BigDecimal.ZERO : dibayarSesi;
	}

	public void setDibayarSesi(BigDecimal dibayarSesi) {
		this.dibayarSesi = dibayarSesi;
	}

	@Column(name = "sisa_hutang", precision = 19, scale = 2)
	public BigDecimal getSisaHutang() {
		return sisaHutang == null ? BigDecimal.ZERO : sisaHutang;
	}

	public void setSisaHutang(BigDecimal sisaHutang) {
		this.sisaHutang = sisaHutang;
	}

	@Column(name = "tujuan_stok", length = 30)
	public String getTujuanStok() {
		return tujuanStok == null || tujuanStok.trim().isEmpty() ? TUJUAN_MOBIL_SALES : tujuanStok;
	}

	public void setTujuanStok(String tujuanStok) {
		this.tujuanStok = tujuanStok;
	}

	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@Column(name = "kode_unik", length = 80, unique = true)
	public String getKodeUnik() {
		return kodeUnik;
	}

	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
