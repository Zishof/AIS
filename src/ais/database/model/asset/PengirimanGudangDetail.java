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

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}; tidak perlu
	 * diubah kecuali bentuk field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 1L;

	/** Primary key auto-generated (IDENTITY) tabel {@code asset.pengiriman_gudang_detail}. */
	private Long id;
	/** Nama pengguna yang terakhir mengubah baris ini; diisi otomatis, lihat {@link #onUpdate()}. */
	private String oleh;
	/** Id pengguna yang terakhir mengubah baris ini; diisi otomatis, lihat {@link #onUpdate()}. */
	private String olehId;
	/** Waktu perubahan terakhir baris ini; diperbarui otomatis oleh {@link #onUpdate()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Dokumen pengiriman induk (header) tempat baris detail ini berada; wajib diisi. */
	private PengirimanGudang pengiriman;
	/** Produk/barang yang dikirim pada baris ini; wajib diisi. */
	private Produk produk;
	private Double qtyKirim;
	private Double qtyTerima;
	/** Harga satuan produk saat pengiriman (untuk penilaian persediaan); boleh {@code null}. */
	private Double hargaSatuan;
	private Double qtyRusak;
	/** Alasan bebas mengapa {@link #qtyRusak} ditandai rusak; opsional. */
	private String alasanRusak;

	/** Konstruktor default tanpa argumen, dipakai Hibernate untuk instansiasi via refleksi. */
	public PengirimanGudangDetail() {
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

	/**
	 * Mengembalikan dokumen pengiriman induk, meresolusi proxy lazy Hibernate lewat
	 * {@link GeneralValueObject#check(Object)}.
	 *
	 * @return {@link PengirimanGudang} induk (wajib, tidak boleh {@code null} pada baris tersimpan).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pengiriman", nullable = false)
	public PengirimanGudang getPengiriman() {
		pengiriman = check(pengiriman);
		return pengiriman;
	}

	/**
	 * Mengisi dokumen pengiriman induk.
	 *
	 * @param pengiriman dokumen induk (wajib diisi sebelum simpan).
	 */
	public void setPengiriman(PengirimanGudang pengiriman) {
		this.pengiriman = pengiriman;
	}

	/**
	 * Mengembalikan produk/barang yang dikirim, meresolusi proxy lazy Hibernate lewat
	 * {@link GeneralValueObject#check(Object)}.
	 *
	 * @return {@link Produk} terkait (wajib, tidak boleh {@code null} pada baris tersimpan).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk", nullable = false)
	public Produk getProduk() {
		produk = check(produk);
		return produk;
	}

	/**
	 * Mengisi produk/barang.
	 *
	 * @param produk produk terkait (wajib diisi sebelum simpan).
	 */
	public void setProduk(Produk produk) {
		this.produk = produk;
	}

	/** @return jumlah yang dikirim dari lokasi asal (selalu terisi sejak dokumen dibuat). */
	@Column(name = "qty_kirim")
	public Double getQtyKirim() {
		return qtyKirim == null ? Double.valueOf(0d) : qtyKirim;
	}

	/**
	 * Mengisi jumlah yang dikirim.
	 *
	 * @param qtyKirim jumlah kirim, boleh {@code null} (diperlakukan sebagai {@code 0}).
	 */
	public void setQtyKirim(Double qtyKirim) {
		this.qtyKirim = qtyKirim;
	}

	/** @return jumlah yang dikonfirmasi diterima; {@code null} selama masih {@link PengirimanGudang#DIKIRIM}. */
	@Column(name = "qty_terima")
	public Double getQtyTerima() {
		return qtyTerima;
	}

	/**
	 * Mengisi jumlah yang dikonfirmasi diterima. Diisi oleh {@code PengirimanGudangUtil.terima(...)}
	 * saat penerimaan dikonfirmasi; boleh sama dengan {@link #getQtyKirim()} (diterima penuh) atau
	 * lebih kecil (diterima sebagian).
	 *
	 * @param qtyTerima jumlah diterima, boleh {@code null} selama belum dikonfirmasi.
	 */
	public void setQtyTerima(Double qtyTerima) {
		this.qtyTerima = qtyTerima;
	}

	/** @return harga satuan produk saat pengiriman; tidak pernah {@code null}, default {@code 0}. */
	@Column(name = "harga_satuan")
	public Double getHargaSatuan() {
		return hargaSatuan == null ? Double.valueOf(0d) : hargaSatuan;
	}

	/**
	 * Mengisi harga satuan.
	 *
	 * @param hargaSatuan harga satuan, boleh {@code null} (diperlakukan sebagai {@code 0}).
	 */
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

	/**
	 * Mengisi jumlah yang ditandai rusak saat konfirmasi terima.
	 *
	 * @param qtyRusak jumlah rusak, boleh {@code null} (diperlakukan sebagai {@code 0}, berarti
	 *                 seluruh {@link #getQtyTerima()} kondisi baik).
	 */
	public void setQtyRusak(Double qtyRusak) {
		this.qtyRusak = qtyRusak;
	}

	/** @return alasan bebas mengapa {@link #getQtyRusak()} ditandai rusak, boleh {@code null}. */
	@Column(name = "alasan_rusak", columnDefinition = "text")
	public String getAlasanRusak() {
		return alasanRusak;
	}

	/**
	 * Mengisi alasan rusak.
	 *
	 * @param alasanRusak teks alasan, boleh {@code null}.
	 */
	public void setAlasanRusak(String alasanRusak) {
		this.alasanRusak = alasanRusak;
	}
}
