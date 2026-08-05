package ais.database.model.asset;

import static javax.persistence.GenerationType.IDENTITY;

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
 * <h2>PengirimanGudangDetail — satu baris produk pada {@link PengirimanGudang}.</h2>
 *
 * <p>
 * {@link #getQtyKirim() qtyKirim} adalah jumlah yang dicatat KELUAR dari lokasi asal saat dokumen
 * dibuat (selalu terisi). {@link #getQtyTerima() qtyTerima} tetap {@code null} selama dokumen
 * berstatus {@link PengirimanGudang#DIKIRIM} (barang masih di lokasi transit, belum dikonfirmasi),
 * lalu diisi oleh {@code PengirimanGudangUtil.terima(...)} — boleh sama dengan {@code qtyKirim}
 * (diterima penuh) atau lebih kecil (diterima sebagian; selisihnya tetap mengendap di lokasi transit
 * milik dokumen ini, bukan hilang begitu saja).
 * </p>
 *
 * @author AIS e-Kantin (modul pergudangan)
 * @see PengirimanGudang
 * @see ais.action.master.inventory.PengirimanGudangUtil
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "pengiriman_gudang_detail")
public class PengirimanGudangDetail extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	private PengirimanGudang pengiriman;
	private Produk produk;
	private Double qtyKirim;
	private Double qtyTerima;
	private Double hargaSatuan;
	private Double qtyRusak;
	private String alasanRusak;

	public PengirimanGudangDetail() {
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dirubah")
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pengiriman", nullable = false)
	public PengirimanGudang getPengiriman() {
		pengiriman = check(pengiriman);
		return pengiriman;
	}

	public void setPengiriman(PengirimanGudang pengiriman) {
		this.pengiriman = pengiriman;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk", nullable = false)
	public Produk getProduk() {
		produk = check(produk);
		return produk;
	}

	public void setProduk(Produk produk) {
		this.produk = produk;
	}

	/** @return jumlah yang dikirim dari lokasi asal (selalu terisi sejak dokumen dibuat). */
	@Column(name = "qty_kirim")
	public Double getQtyKirim() {
		return qtyKirim == null ? Double.valueOf(0d) : qtyKirim;
	}

	public void setQtyKirim(Double qtyKirim) {
		this.qtyKirim = qtyKirim;
	}

	/** @return jumlah yang dikonfirmasi diterima; {@code null} selama masih {@link PengirimanGudang#DIKIRIM}. */
	@Column(name = "qty_terima")
	public Double getQtyTerima() {
		return qtyTerima;
	}

	public void setQtyTerima(Double qtyTerima) {
		this.qtyTerima = qtyTerima;
	}

	@Column(name = "harga_satuan")
	public Double getHargaSatuan() {
		return hargaSatuan == null ? Double.valueOf(0d) : hargaSatuan;
	}

	public void setHargaSatuan(Double hargaSatuan) {
		this.hargaSatuan = hargaSatuan;
	}

	/**
	 * Jumlah dari {@link #qtyTerima} yang staf penerima tandai RUSAK saat konfirmasi terima -- fitur
	 * "Pengiriman Antar Gudang: cek kondisi barang + retur vendor" (gap analisis PDF klien
	 * 2026-07-26). {@code 0}/{@code null} berarti seluruh {@code qtyTerima} kondisi baik. Qty rusak
	 * TETAP dicatat KELUAR dari lokasi transit (baris sudah "selesai diproses", bukan lagi mengendap)
	 * tapi SENGAJA TIDAK ikut ditambahkan ke stok lokasi tujuan ({@code PengirimanGudangUtil.terima})
	 * -- otomatis dicatat sbg {@link ais.database.model.inventory.ReturBarang} sebagai gantinya.
	 */
	@Column(name = "qty_rusak")
	public Double getQtyRusak() {
		return qtyRusak == null ? Double.valueOf(0d) : qtyRusak;
	}

	public void setQtyRusak(Double qtyRusak) {
		this.qtyRusak = qtyRusak;
	}

	@Column(name = "alasan_rusak", columnDefinition = "text")
	public String getAlasanRusak() {
		return alasanRusak;
	}

	public void setAlasanRusak(String alasanRusak) {
		this.alasanRusak = alasanRusak;
	}
}
