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
import ais.database.model.inventory.Produk;
import ais.database.model.library.Penyedia;

/**
 * Master Harga Beli per Supplier (layar legacy 18, {@code masterbl.DBF}: KODESUPPL, KODEBRG,
 * TANGGAL, HARGABELI) -- harga BERVERSI by {@code tanggalEfektif}: pasangan supplier-produk pada
 * rentang efektif yang sama harus unik (overlap ditolak di helper simpan); histori TIDAK ditimpa
 * (perubahan harga = baris versi baru); transaksi memilih versi dgn {@code tanggalEfektif}
 * terbaru &le; tanggal transaksi lalu MENYIMPAN SNAPSHOT sendiri (faktur historis tidak
 * dihitung ulang). Nonaktifkan versi ({@code aktif=false}) utk data salah yang belum dipakai --
 * bukan delete fisik.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "harga_beli_supplier")
public class HargaBeliSupplier extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private Penyedia supplier;
	private Produk produk;
	private BigDecimal harga;
	private Date tanggalEfektif;
	private String keterangan;
	private Boolean aktif;

	private String oleh;
	private String olehId;
	private Date waktu;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public HargaBeliSupplier() {
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
	@JoinColumn(name = "supplier", nullable = false)
	public Penyedia getSupplier() {
		return supplier;
	}

	public void setSupplier(Penyedia supplier) {
		this.supplier = supplier;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk", nullable = false)
	public Produk getProduk() {
		return produk;
	}

	public void setProduk(Produk produk) {
		this.produk = produk;
	}

	@Column(name = "harga", precision = 19, scale = 2)
	public BigDecimal getHarga() {
		return harga == null ? BigDecimal.ZERO : harga;
	}

	public void setHarga(BigDecimal harga) {
		this.harga = harga;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_efektif", nullable = false)
	public Date getTanggalEfektif() {
		return tanggalEfektif;
	}

	public void setTanggalEfektif(Date tanggalEfektif) {
		this.tanggalEfektif = tanggalEfektif;
	}

	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@Column(name = "aktif")
	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
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
