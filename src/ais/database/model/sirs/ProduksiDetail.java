package ais.database.model.sirs;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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

/**
 * Entitas baris (detail) <b>Produksi</b> item medis pada schema {@code sirs}
 * (tabel {@code produksi_detail}). Setiap baris mencatat satu
 * {@link ItemMedis} yang dikonsumsi sebagai BAHAN BAKU pada satu dokumen
 * {@link Produksi}.
 *
 * <h2>Baris ini adalah sisi KONSUMSI dokumen produksi</h2>
 * <p>
 * Berbeda dari pasangan header-detail lain di klaster ini yang barisnya searah
 * dengan headernya, baris ini bergerak BERLAWANAN arah dengan headernya:
 * {@link Produksi#getQty()} di header MENAMBAH stok item hasil, sedangkan
 * {@link #getJumlah()} di baris ini MENGURANGI stok bahan baku. Keduanya
 * terjadi di gudang yang sama, {@link Produksi#getLokasi()}.
 * </p>
 * <p>
 * Karena arah mutasi ditentukan oleh TEMPAT data berada dan bukan oleh tanda
 * angkanya, kode yang memproses dokumen produksi wajib menerapkan arah yang
 * benar untuk masing-masing sisi. Tertukarnya arah — bahan baku ikut menambah
 * stok, misalnya — akan menghasilkan penggandaan persediaan yang konsisten
 * (selalu bertambah, tidak pernah berkurang) dan tidak akan terdeteksi oleh
 * satu pun mekanisme di level model, karena semua angkanya sendiri tetap
 * positif dan wajar.
 * </p>
 *
 * <h2>Catatan integritas</h2>
 * <p>
 * Tidak ada penjaga kecukupan stok: skema tidak memeriksa apakah stok bahan
 * baku {@link #getItem()} di gudang produksi mencukupi
 * {@link #getJumlah()} sebelum dokumen disetujui. Tidak ada pula pemeriksaan
 * terhadap resep {@link BahanBakuItem} — baris ini diisi bebas, sehingga bahan
 * yang tercatat tidak dijamin merupakan bahan yang sah bagi item hasil di
 * {@link Produksi#getItem()}, maupun dalam takaran yang sesuai.
 * </p>
 * <p>
 * Perlu diperhatikan bahwa entitas ini TIDAK memiliki kolom
 * {@code satuanItem}, berbeda dari hampir seluruh baris detail lain di klaster
 * pengadaan ini ({@link PermintaanPembelianDetail},
 * {@link PesananPembelianDetail}, {@link PenerimaanOrderDetail},
 * {@link PenerimaanOrderKembaliDetail}, {@link SaldoAwalMedisDetail} semuanya
 * punya). Akibatnya {@link #getJumlah()} tidak menyatakan satuannya sendiri
 * dan hanya bermakna bila disepakati mengacu pada satuan dasar item yang
 * bersangkutan. Perbandingan takaran terhadap resep di {@link BahanBakuItem}
 * karena itu hanya sah bila kedua sisi memakai satuan dasar yang sama.
 * </p>
 * <p>
 * Seluruh relasi baris ini OPSIONAL ({@code nullable = true}), termasuk
 * {@link #getProduksi()}, sehingga baris detail yatim sah secara skema.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "produksi_detail")
public class ProduksiDetail extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java untuk entitas ini, dipatok agar object
	 * yang sudah terserialisasi tetap kompatibel meski struktur kelas berubah.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String olehId;

	/**
	 * Mengambil ID pengguna yang terakhir mengubah baris ini. Field audit
	 * shadow yang diisi lewat {@link #setOlehId(String)}.
	 *
	 * @return ID pengguna terakhir yang mengubah, atau {@code null} jika belum
	 *         pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna yang mengubah baris ini. Nilai kosong/blank
	 * SENGAJA diabaikan (early return) agar field audit ini tidak pernah
	 * ditimpa jadi kosong — pola field audit shadow yang merupakan KEHARUSAN
	 * TEKNIS berulang di paket model AIS, bukan bug.
	 *
	 * @param olehId ID pengguna; nilai {@code null} atau string
	 *               kosong/whitespace-saja tidak akan mengubah nilai tersimpan.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	private String oleh;

	/**
	 * Representasi ringkas baris bahan baku ini untuk tampilan/log, berupa
	 * {@link #getItem()} yang dirangkai jadi teks. Konkatenasi dengan
	 * {@code ""} dipakai agar item yang belum diisi menghasilkan teks
	 * {@code "null"} alih-alih melempar {@link NullPointerException}.
	 *
	 * @return teks representasi bahan baku pada baris ini.
	 */
	public String toString() {
		return item + "";
	}

	/**
	 * Menetapkan nama pengguna yang mengubah baris ini. Nilai kosong/blank
	 * diabaikan agar field audit ini tidak pernah ditimpa kosong.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau kosong/whitespace-saja
	 *             tidak akan mengubah nilai tersimpan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna terakhir yang mengubah, atau {@code null} jika
	 *         belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate} yang memperbarui {@link #tanggal_dirubah}
	 * otomatis sebelum baris ini di-UPDATE, lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = new Date();

	/**
	 * Menetapkan timestamp perubahan terakhir baris ini secara manual.
	 *
	 * @param tanggal_dirubah timestamp perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil timestamp perubahan terakhir baris ini, diperbarui otomatis
	 * oleh {@link #onUpdate()} setiap kali baris di-UPDATE.
	 *
	 * @return timestamp perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	private ItemMedis item;
	private Double jumlah;
	private Produksi produksi;
	private String keterangan;
	private Double hargaBeli;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public ProduksiDetail() {
	}

	/**
	 * Primary key baris bahan baku ini, auto-increment (IDENTITY) dan diisi
	 * database.
	 *
	 * @return ID unik baris ini, atau {@code null} untuk baris yang belum
	 *         pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID baris bahan baku ini.
	 *
	 * @param id ID baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil keterangan bebas baris bahan baku ini.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas baris bahan baku ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan item medis yang dikonsumsi sebagai bahan baku pada baris ini.
	 *
	 * @param item item medis bahan baku.
	 */
	public void setItem(ItemMedis item) {
		this.item = item;
	}

	/**
	 * Mengambil item medis yang dikonsumsi sebagai BAHAN BAKU pada baris ini —
	 * relasi OPSIONAL ke {@link ItemMedis}. Inilah item yang stoknya BERKURANG
	 * saat dokumen produksi disetujui, kebalikan dari
	 * {@link Produksi#getItem()} yang stoknya bertambah.
	 *
	 * <p>
	 * Skema tidak memeriksa apakah item ini terdaftar sebagai bahan baku yang
	 * sah bagi item hasil di {@link Produksi#getItem()} menurut
	 * {@link BahanBakuItem}. Konsekuensinya bahan apa pun bisa dicatat sebagai
	 * dikonsumsi oleh produksi apa pun, termasuk item yang tidak ada
	 * hubungannya sama sekali dengan hasilnya — jalur untuk mengurangi stok
	 * item bernilai tinggi dengan berkedok peracikan. Untuk bahan medis,
	 * ketidaksesuaian ini juga bermakna bahwa komposisi obat yang benar-benar
	 * diracik tidak dapat diandalkan dari data ini.
	 * </p>
	 * <p>
	 * Skema juga tidak mencegah item hasil produksi dicatat sebagai bahan
	 * bakunya sendiri: baris dengan item yang sama dengan
	 * {@link Produksi#getItem()} akan menghasilkan pengurangan dan penambahan
	 * pada item yang sama dalam satu dokumen.
	 * </p>
	 * <p>
	 * Getter ini memanggil {@code check(...)} milik
	 * {@link ais.database.model.GeneralValueObject} untuk meresolusi proxy lazy
	 * dan menugaskan hasilnya kembali ke field — sehingga bukan getter murni:
	 * ia bisa mengubah state object dan membuka koneksi database sendiri saat
	 * sesi Hibernate asalnya sudah tertutup.
	 * </p>
	 *
	 * @return item medis bahan baku, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = true)
	public ItemMedis getItem() {
		item = check(item);
		return item;
	}

	/**
	 * Menetapkan jumlah bahan baku yang dikonsumsi pada baris ini. Tidak ada
	 * penolakan nilai negatif maupun nol, dan tidak ada pemeriksaan kecukupan
	 * stok, di level entitas.
	 *
	 * @param jumlah jumlah bahan baku yang dikonsumsi.
	 */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * Mengambil jumlah bahan baku yang dikonsumsi pada baris ini — angka yang
	 * MENGURANGI stok {@link #getItem()} di gudang
	 * {@link Produksi#getLokasi()} saat dokumen produksi disetujui.
	 *
	 * <p>
	 * Skema tidak memasang satu pun batas pada angka ini:
	 * </p>
	 * <ul>
	 *   <li>tidak ada pemeriksaan bahwa stok bahan baku di gudang produksi
	 *       mencukupi, sehingga produksi bisa mengonsumsi bahan yang tidak ada
	 *       dan menghasilkan stok negatif;</li>
	 *   <li>tidak ada pemeriksaan terhadap takaran resep di
	 *       {@link BahanBakuItem} dikalikan {@link Produksi#getQty()},
	 *       sehingga konsumsi bahan tidak dijamin sebanding dengan hasil yang
	 *       diklaim;</li>
	 *   <li>tidak ada penolakan nilai negatif — dan nilai negatif di sini
	 *       berbahaya secara khusus, karena ia akan menjadi pengurangan atas
	 *       angka negatif, yaitu PENAMBAHAN stok bahan baku lewat dokumen yang
	 *       bentuknya konsumsi produksi: penciptaan persediaan tanpa jejak
	 *       dokumen penerimaan.</li>
	 * </ul>
	 * <p>
	 * Karena entitas ini tidak punya kolom satuan, angka ini hanya bermakna
	 * bila disepakati mengacu pada satuan dasar item yang bersangkutan.
	 * Nilainya bisa {@code null} karena tidak di-default; pemanggil yang
	 * memakainya untuk mutasi stok WAJIB menangani {@code null} agar tidak
	 * melempar {@link NullPointerException} saat auto-unboxing.
	 * </p>
	 *
	 * @return jumlah bahan baku yang dikonsumsi, atau {@code null} bila belum
	 *         diisi.
	 */
	public Double getJumlah() {
		return jumlah;
	}

	/**
	 * Menetapkan dokumen produksi induk baris ini.
	 *
	 * @param produksi dokumen produksi induk.
	 */
	public void setProduksi(Produksi produksi) {
		this.produksi = produksi;
	}

	/**
	 * Mengambil dokumen {@link Produksi} yang menjadi induk struktural baris
	 * ini. Lewat induk inilah baris ini memperoleh konteks yang tidak
	 * disimpannya sendiri: gudang tempat stok bahan berkurang, status
	 * persetujuan yang menentukan apakah konsumsi sudah terjadi, dan item hasil
	 * yang menjadi tujuan konsumsi ini.
	 *
	 * <p>
	 * Relasi OPSIONAL ({@code nullable = true}) walaupun secara bisnis wajib,
	 * sehingga baris detail yatim tetap sah secara skema dan tidak akan pernah
	 * ikut bergerak saat dokumen mana pun disetujui. Getter ini memanggil
	 * {@code check(...)} sehingga bukan getter murni (lihat {@link #getItem()}).
	 * </p>
	 *
	 * @return dokumen produksi induk, atau {@code null} bila baris ini yatim.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produksi", nullable = true)
	public Produksi getProduksi() {
		produksi = check(produksi);
		return produksi;
	}

	/**
	 * Menetapkan harga beli satuan bahan baku pada baris ini.
	 *
	 * @param hargaBeli harga beli per satuan bahan baku.
	 */
	public void setHargaBeli(Double hargaBeli) {
		this.hargaBeli = hargaBeli;
	}

	/**
	 * Mengambil harga beli satuan bahan baku pada baris ini — dasar penilaian
	 * bahan yang dikonsumsi, dan lewat penjumlahan seluruh barisnya, dasar
	 * biaya produksi yang tersimpan di {@link Produksi#getBiaya()}. Karena
	 * angka di header itu TERSIMPAN dan tidak diturunkan ulang saat dibaca,
	 * keduanya bisa menyimpang bila baris bahan berubah setelah biaya header
	 * terisi.
	 *
	 * <p>
	 * Skema tidak menautkan angka ini ke harga beli historis item yang
	 * bersangkutan (mis. {@link HargaBeliItem}) maupun ke baris penerimaan
	 * yang dulu memasukkan bahan tersebut, sehingga penilaian bahan yang
	 * dikonsumsi tidak mengikuti metode persediaan tertentu — ia sekadar angka
	 * yang diisikan pada saat dokumen dibuat.
	 * </p>
	 *
	 * @return harga beli per satuan bahan baku, atau {@code null} bila belum
	 *         diisi.
	 */
	@Column(name = "harga_beli", nullable = true)
	public Double getHargaBeli() {
		return hargaBeli;
	}

}
