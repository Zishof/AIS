package ais.database.model.inventory;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Reservasi komponen sebuah Work Order produksi (Fase D dok. 48 P4 / dok. 49).
 *
 * <p>Satu baris = satu komponen BOM yang DIKUNCI oleh satu WO: ditulis saat WO
 * di-RELEASED (kebutuhan = qty baris BOM x rasio planned WO), {@code qtySisa}
 * berkurang saat dokumen ISSUE ber-referensi WO itu POSTED (dan pulih saat
 * ISSUE di-REVERSED), lalu baris ditutup ({@code SELESAI}/{@code BATAL}) saat
 * WO COMPLETED/CANCELLED. Baris tidak pernah dihapus — jejak komersial.</p>
 *
 * <p><b>INFORMASI SAJA untuk kasir</b>: keputusan pemilik dok. 48 §6 no. 4
 * (apakah reserved menolak penjualan kasir) masih terbuka — sampai dikunci,
 * ledger ini TIDAK mengurangi stok yang boleh dijual dan tidak disentuh alur
 * kasir mana pun. Pembaca hari ini: dasbor/laporan dan pengecekan kekurangan
 * komponen saat rilis WO.</p>
 *
 * <p>SENGAJA tabel sendiri (bukan kolom di stok): stok AIS dihitung dari
 * ledger-ledger (rumus {@code StokKantinUtil}), tidak ada tabel stok-per-baris
 * yang bisa ditumpangi tanpa mengubah makna — pola yang sama dengan
 * {@link MutasiStokProduksi}.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "inventory_production", name = "production_reservation")
public class ReservasiStokProduksi implements Serializable {
	private static final long serialVersionUID = 1L;

	public static final String STATUS_AKTIF = "AKTIF";
	public static final String STATUS_SELESAI = "SELESAI";
	public static final String STATUS_BATAL = "BATAL";

	private Long id; private Long woId; private Long tokoId; private Long produkId;
	private BigDecimal qty = BigDecimal.ZERO; private BigDecimal qtySisa = BigDecimal.ZERO;
	private String status = STATUS_AKTIF; private String keterangan;
	private String oleh; private Date dibuat = new Date(); private Date diubah = new Date();

	@Id @GeneratedValue(strategy = IDENTITY) @Column(name = "id", unique = true, nullable = false)
	public Long getId() { return id; } public void setId(Long value) { id = value; }
	/** Id {@code ProduksiDokumen} bertipe WO pemilik reservasi. */
	@Column(name = "wo_id", nullable = false) public Long getWoId() { return woId; } public void setWoId(Long value) { woId = value; }
	@Column(name = "toko_id", nullable = false) public Long getTokoId() { return tokoId; } public void setTokoId(Long value) { tokoId = value; }
	/** Id {@code koperasi.produk} komponen yang dikunci. */
	@Column(name = "produk_id", nullable = false) public Long getProdukId() { return produkId; } public void setProdukId(Long value) { produkId = value; }
	/** Kebutuhan awal saat rilis (satuan dasar produk). */
	@Column(name = "qty", nullable = false, precision = 19, scale = 4) public BigDecimal getQty() { return qty; } public void setQty(BigDecimal value) { qty = value; }
	/** Sisa yang masih terkunci — berkurang oleh ISSUE ber-referensi WO. */
	@Column(name = "qty_sisa", nullable = false, precision = 19, scale = 4) public BigDecimal getQtySisa() { return qtySisa; } public void setQtySisa(BigDecimal value) { qtySisa = value; }
	@Column(name = "status", nullable = false, length = 20) public String getStatus() { return status; } public void setStatus(String value) { status = value; }
	@Column(name = "keterangan", length = 255) public String getKeterangan() { return keterangan; } public void setKeterangan(String value) { keterangan = value; }
	@Column(name = "oleh", length = 100) public String getOleh() { return oleh; } public void setOleh(String value) { oleh = value; }
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "dibuat", nullable = false)
	public Date getDibuat() { return dibuat; } public void setDibuat(Date value) { dibuat = value; }
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "diubah", nullable = false)
	public Date getDiubah() { return diubah; } public void setDiubah(Date value) { diubah = value; }
}
