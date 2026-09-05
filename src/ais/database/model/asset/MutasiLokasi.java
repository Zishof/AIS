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

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}; tidak perlu
	 * diubah kecuali bentuk field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = -6631028994455201991L;

	/** Barang masuk ke lokasi (qty positif). */
	public static final String MASUK = "MASUK";
	/** Barang keluar dari lokasi (qty negatif). */
	public static final String KELUAR = "KELUAR";
	/** Perpindahan antar lokasi (dicatat 2 baris: KELUAR di asal + MASUK di tujuan). */
	public static final String TRANSFER = "TRANSFER";
	/** Koreksi hasil stok opname (qty berisi selisih, boleh +/-). */
	public static final String PENYESUAIAN = "PENYESUAIAN";

	/** Primary key auto-generated (IDENTITY) tabel {@code asset.mutasi_lokasi}. */
	private Long id;
	/** Nama pengguna yang terakhir mengubah baris ini; diisi otomatis, lihat {@link #onUpdate()}. */
	private String oleh;
	/** Id pengguna yang terakhir mengubah baris ini; diisi otomatis, lihat {@link #onUpdate()}. */
	private String olehId;
	/** Waktu perubahan terakhir baris ini; diperbarui otomatis oleh {@link #onUpdate()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	private Lokasi lokasi;
	private Produk produk;
	private Lokasi lokasiPasangan;
	private String jenis;
	private Double qty;
	private Double hargaSatuan;
	/** Tanggal pergerakan stok terjadi (bukan tanggal input); default waktu saat objek dibuat. */
	private Date tanggal = ais.ui.util.WaktuUtil.getDate();
	/** Keterangan bebas untuk baris pergerakan ini, opsional. */
	private String keterangan;
	/** Kode referensi/penghubung ke dokumen sumber (mis. kode transfer bersama), opsional. */
	private String referensi;

	/** Konstruktor default tanpa argumen, dipakai Hibernate untuk instansiasi via refleksi. */
	public MutasiLokasi() {
	}

	/** @return primary key baris ini, atau {@code null} untuk instance baru yang belum disimpan. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/**
	 * Mengisi primary key. Kolom database bersifat {@code insertable = false} (IDENTITY,
	 * auto-generate oleh database), sehingga pengisian manual tidak berpengaruh pada
	 * {@code INSERT}.
	 *
	 * @param id primary key.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return nama pengguna yang terakhir mengubah baris ini (audit), atau {@code null} bila
	 *         belum pernah diubah sejak dimuat.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Mengisi nama pengguna audit. Nilai {@code null}/kosong diabaikan agar jejak lama tidak
	 * tertimpa hampa.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau blank.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * @return id pengguna yang terakhir mengubah baris ini (audit), atau {@code null} bila belum
	 *         pernah diubah sejak dimuat.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna audit. Nilai {@code null}/kosong diabaikan, sama seperti
	 * {@link #setOleh(String)}.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau blank.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Hook siklus hidup JPA yang dipanggil Hibernate tepat sebelum setiap {@code UPDATE}.
	 * Mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}
	 * yang mengisi {@link #tanggal_dirubah}, {@link #oleh}, dan {@link #olehId} dengan waktu serta
	 * identitas pengguna aktif. Dipicu otomatis oleh Hibernate, tidak dipanggil manual.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** @return waktu perubahan terakhir baris ini; tidak pernah {@code null}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dirubah")
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Mengisi waktu perubahan terakhir. Field diinisialisasi ke waktu saat objek dibuat, lalu
	 * ditimpa ulang oleh {@link #onUpdate()} setiap kali baris di-{@code UPDATE}.
	 *
	 * @param tanggal_dirubah waktu perubahan.
	 */
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

	/**
	 * Mengisi lokasi tempat pergerakan terjadi.
	 *
	 * @param lokasi lokasi terkait (wajib diisi sebelum simpan).
	 */
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

	/**
	 * Mengisi produk/barang yang bergerak.
	 *
	 * @param produk produk terkait (wajib diisi sebelum simpan).
	 */
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

	/**
	 * Mengisi lokasi lawan pada transfer.
	 *
	 * @param lokasiPasangan lokasi lawan; boleh {@code null} untuk pergerakan non-transfer.
	 */
	public void setLokasiPasangan(Lokasi lokasiPasangan) {
		this.lokasiPasangan = lokasiPasangan;
	}

	/** @return label jenis pergerakan ({@link #MASUK}/{@link #KELUAR}/{@link #TRANSFER}/{@link #PENYESUAIAN}). */
	@Column(name = "jenis", length = 32)
	public String getJenis() {
		return jenis;
	}

	/**
	 * Mengisi label jenis pergerakan.
	 *
	 * @param jenis salah satu {@link #MASUK}/{@link #KELUAR}/{@link #TRANSFER}/{@link #PENYESUAIAN}.
	 */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/** @return jumlah <b>bertanda</b> (positif=menambah stok, negatif=mengurangi). */
	@Column(name = "qty")
	public Double getQty() {
		return qty == null ? Double.valueOf(0d) : qty;
	}

	/**
	 * Mengisi jumlah bertanda pergerakan. Pemanggil bertanggung jawab menetapkan tanda yang
	 * benar sesuai konvensi kelas ini (lihat javadoc kelas); helper {@code StokLokasiUtil}
	 * melakukan ini secara otomatis.
	 *
	 * @param qty jumlah bertanda (positif=masuk, negatif=keluar).
	 */
	public void setQty(Double qty) {
		this.qty = qty;
	}

	/** @return harga satuan saat pergerakan (untuk penilaian persediaan); boleh {@code null}. */
	@Column(name = "harga_satuan")
	public Double getHargaSatuan() {
		return hargaSatuan == null ? Double.valueOf(0d) : hargaSatuan;
	}

	/**
	 * Mengisi harga satuan saat pergerakan.
	 *
	 * @param hargaSatuan harga satuan, boleh {@code null}.
	 */
	public void setHargaSatuan(Double hargaSatuan) {
		this.hargaSatuan = hargaSatuan;
	}

	/** @return tanggal pergerakan stok terjadi (bukan tanggal input/simpan baris ini). */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal")
	public Date getTanggal() {
		return tanggal;
	}

	/**
	 * Mengisi tanggal pergerakan.
	 *
	 * @param tanggal tanggal pergerakan.
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/** @return keterangan bebas untuk baris pergerakan ini, boleh {@code null}. */
	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Mengisi keterangan bebas.
	 *
	 * @param keterangan teks keterangan, boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return kode referensi/penghubung (mis. kode transfer bersama, no. dokumen sumber). */
	@Column(name = "referensi", length = 128)
	public String getReferensi() {
		return referensi;
	}

	/**
	 * Mengisi kode referensi/penghubung.
	 *
	 * @param referensi kode referensi, boleh {@code null}.
	 */
	public void setReferensi(String referensi) {
		this.referensi = referensi;
	}
}
