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

/**
 * Master Harga Jual per Customer (layar legacy 19) -- kembaran {@link HargaBeliSupplier} di sisi
 * jual: pasangan customer-produk berversi by {@code tanggalEfektif} (overlap ditolak), histori
 * tidak ditimpa, transaksi mengambil versi berlaku lalu menyimpan snapshot sendiri.
 * {@code anggotaKoperasi} NULLABLE: baris tanpa customer = harga jual KHUSUS daftar harga umum
 * bertanggal (dipakai layar 13 "Daftar Harga Jual" versi umum) -- customer spesifik menang atas
 * baris umum saat resolusi.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "harga_jual_customer")
public class HargaJualCustomer extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private AnggotaKoperasi anggotaKoperasi;
	private Produk produk;
	private BigDecimal harga;
	private Date tanggalEfektif;
	private String keterangan;
	private Boolean aktif;

	private String oleh;
	private String olehId;
	private Date waktu;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public HargaJualCustomer() {
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
	@JoinColumn(name = "anggota_koperasi", nullable = true)
	public AnggotaKoperasi getAnggotaKoperasi() {
		return anggotaKoperasi;
	}

	public void setAnggotaKoperasi(AnggotaKoperasi anggotaKoperasi) {
		this.anggotaKoperasi = anggotaKoperasi;
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
