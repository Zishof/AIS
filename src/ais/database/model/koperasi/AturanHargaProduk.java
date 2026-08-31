package ais.database.model.koperasi;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Aturan harga ber-ambang kuantitas (harga grosir) — Fase A dok. 48/49.
 *
 * <p>SATU bentuk aturan melayani kedua metode PDF referensi: "harga kemasan"
 * dinyatakan sebagai ambang {@code minQtyDasar} = isi kemasan. Ambang TERBESAR
 * yang &le; qty menang; aturan ber-{@code toko} mengalahkan aturan global
 * ({@code toko} null = semua toko). Tanpa aturan yang cocok, harga katalog
 * ({@code Produk.hargaJual}) berlaku.</p>
 *
 * <p><b>Urutan terhadap AturanDiskon (keputusan pemilik sistem 29-08-2026):</b>
 * aturan ini menentukan HARGA SATUAN lebih dulu; {@code AturanDiskon} memotong
 * SESUDAHNYA di atas harga grosir. Penerapannya satu mesin
 * ({@code HargaGrosirApiHelper.terapkanKeItems}) yang dipanggil baik pratinjau
 * keranjang ({@code diskon_evaluasi}) maupun checkout ({@code bayar}) — dua
 * salinan akan menyimpang diam-diam.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "koperasi", name = "aturan_harga_produk")
public class AturanHargaProduk implements Serializable {
	private static final long serialVersionUID = 1L;

	private Long id; private Long produk; private Long toko;
	private Double minQtyDasar; private Double harga;
	private Double hargaPaket; private Boolean kelipatanWajib;
	private Date berlakuMulai; private Date berlakuSampai;
	private Boolean aktif = Boolean.TRUE; private String keterangan;
	private String oleh; private Date waktu = new Date();

	@Id @GeneratedValue(strategy = IDENTITY) @Column(name = "id", unique = true, nullable = false)
	public Long getId() { return id; } public void setId(Long value) { id = value; }
	/** Id {@code koperasi.produk}. Wajib — aturan harga selalu per produk. */
	@Column(name = "produk", nullable = false) public Long getProduk() { return produk; } public void setProduk(Long value) { produk = value; }
	/** Null = berlaku semua toko; terisi = hanya toko itu (dan menang atas aturan global). */
	@Column(name = "toko") public Long getToko() { return toko; } public void setToko(Long value) { toko = value; }
	/** Ambang kuantitas dalam SATUAN DASAR produk (qty keranjang setelah konversi kemasan). */
	@Column(name = "min_qty_dasar", nullable = false) public Double getMinQtyDasar() { return minQtyDasar; } public void setMinQtyDasar(Double value) { minQtyDasar = value; }
	/** Harga per SATUAN DASAR saat ambang terpenuhi — bukan harga per kemasan. */
	@Column(name = "harga", nullable = false) public Double getHarga() { return harga; } public void setHarga(Double value) { harga = value; }
	/** Metode 2 dok. 48 §6 no.1: harga TETAP per paket/kemasan (mis. Rp 4.500.000/karung yang
	 * tidak persis isi x harga satuan). Bila terisi, mesin memakai {@code hargaPaket/minQtyDasar}
	 * sebagai harga satuan efektif — total kelipatan paket selalu = harga paket x jumlah paket.
	 * {@code null} = aturan ambang biasa (Metode 1). */
	@Column(name = "harga_paket") public Double getHargaPaket() { return hargaPaket; } public void setHargaPaket(Double value) { hargaPaket = value; }
	/** §6 no.2: {@code true} = pembeli grosir WAJIB kelipatan {@code minQtyDasar} — bayar menolak
	 * qty nanggung ("53 kg") dengan pesan terbaca. {@code null}/false = bebas (perilaku lama). */
	@Column(name = "kelipatan_wajib") public Boolean getKelipatanWajib() { return kelipatanWajib; } public void setKelipatanWajib(Boolean value) { kelipatanWajib = value; }
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "berlaku_mulai") public Date getBerlakuMulai() { return berlakuMulai; } public void setBerlakuMulai(Date value) { berlakuMulai = value; }
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "berlaku_sampai") public Date getBerlakuSampai() { return berlakuSampai; } public void setBerlakuSampai(Date value) { berlakuSampai = value; }
	@Column(name = "aktif") public Boolean getAktif() { return aktif == null ? Boolean.TRUE : aktif; } public void setAktif(Boolean value) { aktif = value; }
	@Column(name = "keterangan", length = 255) public String getKeterangan() { return keterangan; } public void setKeterangan(String value) { keterangan = value; }
	@Column(name = "oleh", length = 100) public String getOleh() { return oleh; } public void setOleh(String value) { oleh = value; }
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "waktu")
	public Date getWaktu() { return waktu; } public void setWaktu(Date value) { waktu = value; }
}
