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
 * <h2>MutasiLokasi — buku besar (ledger) pergerakan stok barang <b>per lokasi/gudang</b>.</h2>
 *
 * <p>
 * Entity ini adalah <b>sumber kebenaran tunggal</b> untuk stok barang di setiap {@link Lokasi}
 * (gudang, outlet, kasir, dsb) pada modul pergudangan e-Kantin. Alih-alih menyimpan angka stok
 * sebagai satu kolom yang gampang tidak sinkron, setiap perubahan stok dicatat sebagai <b>satu baris
 * pergerakan</b> di sini. Stok berjalan sebuah (lokasi, produk) kemudian dihitung sebagai
 * <b>penjumlahan</b> seluruh baris terkait — pendekatan buku besar yang akurat, dapat diaudit, dan
 * mudah ditelusuri ("kenapa stok segini?" cukup lihat riwayat baris).
 * </p>
 *
 * <h3>Konvensi tanda {@link #getQty() qty} (penting)</h3>
 * <p>
 * Kolom {@code qty} disimpan <b>bertanda (signed)</b> sehingga stok = {@code SUM(qty)} — tanpa
 * logika kondisional pada saat agregasi (efisien &amp; anti-salah):
 * </p>
 * <ul>
 *   <li><b>MASUK</b> (barang datang): {@code qty} bernilai <b>positif</b>.</li>
 *   <li><b>KELUAR</b> (barang keluar/terjual/rusak): {@code qty} bernilai <b>negatif</b>.</li>
 *   <li><b>TRANSFER</b> antar lokasi: dicatat sebagai <b>dua baris</b> — satu KELUAR (negatif) di
 *       lokasi asal dan satu MASUK (positif) di lokasi tujuan — keduanya berbagi
 *       {@link #getReferensi() referensi} yang sama dan saling menunjuk lewat
 *       {@link #getLokasiPasangan() lokasiPasangan}. Dengan begitu stok tiap lokasi tetap sekadar
 *       {@code SUM(qty)} baris miliknya sendiri.</li>
 *   <li><b>PENYESUAIAN/OPNAME</b> (koreksi hasil hitung fisik): {@code qty} berisi <b>selisih</b>
 *       (boleh positif atau negatif) agar stok tercatat menyamai stok fisik.</li>
 * </ul>
 * <p>
 * Field {@link #getJenis() jenis} hanyalah <b>label</b> untuk tampilan/penyaringan; nilai stok
 * sepenuhnya ditentukan oleh tanda {@code qty}. Helper penulis ({@code StokLokasiUtil}) yang
 * menetapkan tanda dengan benar sehingga pemanggil cukup menyebut jenis + jumlah positif.
 * </p>
 *
 * <h3>Relasi &amp; skema</h3>
 * <p>
 * Dipetakan ke <code>asset.mutasi_lokasi</code>. Menautkan {@link Lokasi} (tempat pergerakan terjadi)
 * dan {@link Produk} (<code>koperasi.produk</code>) — relasi lintas-skema yang lazim di aplikasi ini.
 * {@link #getLokasiPasangan() lokasiPasangan} opsional, dipakai hanya untuk transfer (menunjuk lokasi
 * lawan). {@link #getHargaSatuan() hargaSatuan} menyimpan harga saat pergerakan sehingga penilaian
 * persediaan (nilai barang) dapat dihitung historis bila diperlukan. Kolom {@link #getReferensi()
 * referensi} menautkan baris ke dokumen sumber (mis. kode transfer, no. faktur) untuk penelusuran.
 * </p>
 *
 * <h3>Audit, kinerja &amp; efisiensi memori</h3>
 * <p>
 * Ditandai {@link org.hibernate.annotations.Entity dynamicInsert/dynamicUpdate} agar hanya kolom
 * terisi yang ditulis (menghemat I/O), dan {@link Audited @Audited} untuk jejak revisi. Karena stok
 * dihitung via agregasi SQL ({@code SUM(qty)} dengan filter lokasi/produk/tanggal) pada kolom-kolom
 * yang secara logis terindeks, laporan tetap gesit meski baris bertambah banyak; tidak ada tabel
 * cache stok yang berisiko melenceng. Entity ini ramping (kolom skalar + tiga relasi lazy) sehingga
 * hemat memori saat dimuat massal. Semua operasi tunduk pada aturan sesi framework: bila memakai
 * {@code currentSession()} jangan ditutup manual; bila {@code openSession()}/{@code currentNativeSession()}
 * tutup di {@code finally}. Kompatibel Java 1.7.
 * </p>
 *
 * @author AIS e-Kantin (modul pergudangan)
 * @see Lokasi
 * @see ais.action.master.inventory.StokLokasiUtil
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "mutasi_lokasi")
public class MutasiLokasi extends GeneralValueObject {

	private static final long serialVersionUID = -6631028994455201991L;

	/** Barang masuk ke lokasi (qty positif). */
	public static final String MASUK = "MASUK";
	/** Barang keluar dari lokasi (qty negatif). */
	public static final String KELUAR = "KELUAR";
	/** Perpindahan antar lokasi (dicatat 2 baris: KELUAR di asal + MASUK di tujuan). */
	public static final String TRANSFER = "TRANSFER";
	/** Koreksi hasil stok opname (qty berisi selisih, boleh +/-). */
	public static final String PENYESUAIAN = "PENYESUAIAN";

	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	private Lokasi lokasi;
	private Produk produk;
	private Lokasi lokasiPasangan;
	private String jenis;
	private Double qty;
	private Double hargaSatuan;
	private Date tanggal = ais.ui.util.WaktuUtil.getDate();
	private String keterangan;
	private String referensi;

	public MutasiLokasi() {
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

	/** @return lokasi tempat pergerakan ini terjadi (wajib). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi", nullable = false)
	public Lokasi getLokasi() {
		lokasi = check(lokasi);
		return lokasi;
	}

	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	/** @return produk/barang yang bergerak (wajib). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk", nullable = false)
	public Produk getProduk() {
		produk = check(produk);
		return produk;
	}

	public void setProduk(Produk produk) {
		this.produk = produk;
	}

	/** @return lokasi lawan pada transfer (asal/tujuan); {@code null} untuk non-transfer. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi_pasangan", nullable = true)
	public Lokasi getLokasiPasangan() {
		lokasiPasangan = check(lokasiPasangan);
		return lokasiPasangan;
	}

	public void setLokasiPasangan(Lokasi lokasiPasangan) {
		this.lokasiPasangan = lokasiPasangan;
	}

	/** @return label jenis pergerakan ({@link #MASUK}/{@link #KELUAR}/{@link #TRANSFER}/{@link #PENYESUAIAN}). */
	@Column(name = "jenis", length = 32)
	public String getJenis() {
		return jenis;
	}

	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/** @return jumlah <b>bertanda</b> (positif=menambah stok, negatif=mengurangi). */
	@Column(name = "qty")
	public Double getQty() {
		return qty == null ? Double.valueOf(0d) : qty;
	}

	public void setQty(Double qty) {
		this.qty = qty;
	}

	/** @return harga satuan saat pergerakan (untuk penilaian persediaan); boleh {@code null}. */
	@Column(name = "harga_satuan")
	public Double getHargaSatuan() {
		return hargaSatuan == null ? Double.valueOf(0d) : hargaSatuan;
	}

	public void setHargaSatuan(Double hargaSatuan) {
		this.hargaSatuan = hargaSatuan;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal")
	public Date getTanggal() {
		return tanggal;
	}

	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return kode referensi/penghubung (mis. kode transfer bersama, no. dokumen sumber). */
	@Column(name = "referensi", length = 128)
	public String getReferensi() {
		return referensi;
	}

	public void setReferensi(String referensi) {
		this.referensi = referensi;
	}
}
