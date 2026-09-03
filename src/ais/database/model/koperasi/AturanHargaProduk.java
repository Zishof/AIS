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

	/** PK auto-generated (identity). {@code null} sebelum baris aturan disimpan. */
	@Id @GeneratedValue(strategy = IDENTITY) @Column(name = "id", unique = true, nullable = false)
	public Long getId() { return id; } public void setId(Long value) { id = value; }
	/** Id {@code koperasi.produk} (entity {@link ais.database.model.inventory.Produk} —
	 * BUKAN {@link ProdukKoperasi}, katalog produk simpan-pinjam yang berbeda meski satu package;
	 * ini FK mentah {@code Long}, tanpa relasi {@code @ManyToOne}, jadi tidak ada resolusi lazy
	 * seperti pola {@code check()} di entity lain domain koperasi). Wajib — aturan harga selalu
	 * per produk. */
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
	/** Awal jendela waktu berlakunya aturan ini; {@code null} = berlaku sejak kapan pun (tanpa
	 * batas awal). Bersama {@link #getBerlakuSampai()} membentuk jendela opsional yang harus
	 * dievaluasi {@code HargaGrosirApiHelper.terapkanKeItems} terhadap waktu transaksi saat
	 * memilih aturan mana yang cocok — di luar jendela, aturan diperlakukan seolah tidak aktif
	 * meski {@link #getAktif()} bernilai {@code true}. */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "berlaku_mulai") public Date getBerlakuMulai() { return berlakuMulai; } public void setBerlakuMulai(Date value) { berlakuMulai = value; }
	/** Akhir jendela waktu berlakunya aturan ini; {@code null} = tanpa batas akhir. Lihat
	 * {@link #getBerlakuMulai()}. */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "berlaku_sampai") public Date getBerlakuSampai() { return berlakuSampai; } public void setBerlakuSampai(Date value) { berlakuSampai = value; }
	/** Status aktif aturan. Getter null-safe: {@code null} di DB dibaca sebagai {@link Boolean#TRUE}
	 * (default aktif, bukan default nonaktif) — konsisten dengan inisialisasi field
	 * {@code aktif = Boolean.TRUE} pada deklarasi. Menonaktifkan (bukan menghapus baris) adalah
	 * cara "hapus" aturan harga grosir dari rute {@code harga_grosir_list/simpan/hapus} — jejak
	 * komersial (harga yang pernah berlaku) tetap dipertahankan untuk audit. */
	@Column(name = "aktif") public Boolean getAktif() { return aktif == null ? Boolean.TRUE : aktif; } public void setAktif(Boolean value) { aktif = value; }
	/** Catatan bebas tentang aturan harga ini (mis. alasan/sumber kesepakatan harga). Opsional,
	 * tidak dipakai logika evaluasi mesin harga grosir. */
	@Column(name = "keterangan", length = 255) public String getKeterangan() { return keterangan; } public void setKeterangan(String value) { keterangan = value; }
	/** Nama/identitas petugas yang membuat/mengubah baris aturan ini — jejak audit tampilan,
	 * bukan FK ke tabel pengguna. Tidak ada guard null/blank pada setter (berbeda dari pola
	 * {@code setOleh}/{@code setOlehId} pada entity koperasi lain yang mengabaikan nilai
	 * kosong) — di sini nilai kosong/{@code null} langsung menimpa. */
	@Column(name = "oleh", length = 100) public String getOleh() { return oleh; } public void setOleh(String value) { oleh = value; }
	/** Waktu baris aturan ini dibuat/terakhir diubah. Diinisialisasi ke waktu instansiasi objek
	 * ({@code new Date()} pada deklarasi field) — BUKAN dihitung ulang tiap panggilan getter, dan
	 * TIDAK diperbarui otomatis oleh hook {@code @PreUpdate} (kelas ini tidak mendeklarasikan
	 * satu, berbeda dari {@link ProdukKoperasi#onUpdate()}/{@link HargaJualCustomer#onUpdate()})
	 * — pemanggil (helper simpan) yang bertanggung jawab men-set ulang nilai ini saat mengubah
	 * baris bila ingin jejak waktu perubahan akurat. */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "waktu")
	public Date getWaktu() { return waktu; } public void setWaktu(Date value) { waktu = value; }
}
