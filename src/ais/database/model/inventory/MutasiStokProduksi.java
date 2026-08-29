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
import javax.persistence.UniqueConstraint;

/**
 * Ledger pergerakan stok dari dokumen PRODUKSI (Fase 0 dok. 49) — suku ke-9
 * rumus stok {@code StokKantinUtil}.
 *
 * <p>Satu baris = efek stok SATU baris dokumen produksi pada SATU arah. Baris
 * {@code FORWARD} ditulis saat dokumen ISSUE/RETURN/OUTPUT/WASTE mencapai
 * POSTED; baris {@code REVERSE} ditulis saat dokumen di-REVERSED — ledger tidak
 * pernah dihapus (ADR kontrak data terpadu: koreksi lewat movement lawan, bukan
 * menghapus historis; pola yang sama dengan {@link DistribusiPostingStok}).</p>
 *
 * <p>Idempoten dua lapis: constraint unik {@code (dokumen_id, baris_id, arah)}
 * menjaga di tingkat baris, dan {@code kunci_idempoten} berformat fondasi
 * Fase 9 ({@code PRODUCTION:<dokumen>:<jenis>:<baris>:<arah>}) menjaga lintas
 * retry. Penulisnya memeriksa-lalu-melewati sebelum menulis, jadi memproses
 * ulang transisi status tidak menggandakan pergerakan.</p>
 *
 * <p>SENGAJA tabel sendiri, bukan menumpang {@code pemakaian_bahan_baku}
 * (konsumsi resep saat JUAL — dipakai laporan HPP) maupun
 * {@code mutasi_stok_toko} (transfer ANTAR toko — produksi masuk/keluar satu
 * toko). Menumpang salah satunya mencemari laporan pembacanya; lihat dok. 49
 * §3 Fase 0 dan JavaDoc {@code BahanBakuUtil}.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "koperasi", name = "mutasi_stok_produksi", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "dokumen_id", "baris_id", "arah" }),
		@UniqueConstraint(columnNames = { "kunci_idempoten" }) })
public class MutasiStokProduksi implements Serializable {
	private static final long serialVersionUID = 1L;

	public static final String ARAH_FORWARD = "FORWARD";
	public static final String ARAH_REVERSE = "REVERSE";

	private Long id; private Long dokumenId; private Long barisId; private String arah;
	private String jenis; private Long toko; private Long produk;
	private BigDecimal qtyMasuk = BigDecimal.ZERO; private BigDecimal qtyKeluar = BigDecimal.ZERO;
	private String kunciIdempoten; private String keterangan;
	private String oleh; private Date waktu = new Date();

	@Id @GeneratedValue(strategy = IDENTITY) @Column(name = "id", unique = true, nullable = false)
	public Long getId() { return id; } public void setId(Long value) { id = value; }
	@Column(name = "dokumen_id", nullable = false) public Long getDokumenId() { return dokumenId; } public void setDokumenId(Long value) { dokumenId = value; }
	@Column(name = "baris_id", nullable = false) public Long getBarisId() { return barisId; } public void setBarisId(Long value) { barisId = value; }
	@Column(name = "arah", nullable = false, length = 10) public String getArah() { return arah; } public void setArah(String value) { arah = value; }
	@Column(name = "jenis", nullable = false, length = 20) public String getJenis() { return jenis; } public void setJenis(String value) { jenis = value; }
	@Column(name = "toko", nullable = false) public Long getToko() { return toko; } public void setToko(Long value) { toko = value; }
	/** Id {@code koperasi.produk} yang stoknya bergerak (= {@code ProduksiDokumenBaris.itemId}). */
	@Column(name = "produk", nullable = false) public Long getProduk() { return produk; } public void setProduk(Long value) { produk = value; }
	@Column(name = "qty_masuk", nullable = false, precision = 19, scale = 4) public BigDecimal getQtyMasuk() { return qtyMasuk; } public void setQtyMasuk(BigDecimal value) { qtyMasuk = value; }
	@Column(name = "qty_keluar", nullable = false, precision = 19, scale = 4) public BigDecimal getQtyKeluar() { return qtyKeluar; } public void setQtyKeluar(BigDecimal value) { qtyKeluar = value; }
	@Column(name = "kunci_idempoten", nullable = false, length = 120) public String getKunciIdempoten() { return kunciIdempoten; } public void setKunciIdempoten(String value) { kunciIdempoten = value; }
	@Column(name = "keterangan", length = 255) public String getKeterangan() { return keterangan; } public void setKeterangan(String value) { keterangan = value; }
	@Column(name = "oleh", length = 100) public String getOleh() { return oleh; } public void setOleh(String value) { oleh = value; }
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "waktu", nullable = false)
	public Date getWaktu() { return waktu; } public void setWaktu(Date value) { waktu = value; }
}
