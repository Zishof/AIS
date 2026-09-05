package ais.database.model.asset;

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.ui.util.WaktuUtil;

/**
 * Baris DETAIL unit barang pada satu dokumen {@link PeminjamanMasterAsset} -- setiap baris
 * menunjuk ke SATU unit fisik ber-barcode ({@link #getAssetDetail()}) dan melacak status
 * kembalinya secara MANDIRI lewat {@link #getDikembalikan()}/{@link #getWaktuPengembalian()}.
 *
 * <h3>Berbasis unit fisik, bukan kuantitas</h3>
 *
 * <p>Berbeda dengan {@link PemakaianMasterAssetDetail} yang menunjuk ke {@link MasterAsset}
 * (katalog) dan mencatat kuantitas, kelas ini menunjuk ke {@code AssetDetail} (unit fisik
 * individual) -- cocok untuk barang tidak habis pakai yang dipinjam per-unit dan harus
 * dikembalikan utuh, bukan barang persediaan yang dikonsumsi.</p>
 *
 * <h3>Pelacakan pengembalian PER-BARIS, terpisah dari header</h3>
 *
 * <p>Flag {@link #getDikembalikan()} dan {@link #getWaktuPengembalian()} di kelas ini berdiri
 * sendiri per unit barang, TERPISAH dari tautan header {@link
 * PeminjamanMasterAsset#getPengembalianMasterAsset()} -- lihat catatan pada javadoc kelas
 * {@link PeminjamanMasterAsset} mengenai risiko kedua mekanisme tidak sinkron. Tidak ada
 * validasi di kelas ini (atau tempat lain di paket entitas ini) yang mencegah unit yang SAMA
 * ({@code assetDetail} yang sama) muncul di baris peminjaman lain selagi baris ini masih
 * {@code dikembalikan = false} -- yaitu tidak ada penjagaan double-booking otomatis di tingkat
 * data untuk mencegah satu unit fisik dipinjamkan ke dua peminjam sekaligus.</p>
 *
 * @see PeminjamanMasterAsset header dokumen peminjaman yang memiliki baris ini
 * @see PemakaianMasterAssetDetail padanan berbasis kuantitas/katalog untuk pemakaian permanen
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "peminjaman_master_asset_detail")
public class PeminjamanMasterAssetDetail extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java, identik di seluruh berkas entitas hbm2java sepaket (lihat
	 * catatan yang sama di {@link PemakaianMasterAsset#serialVersionUID}).
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama baris, kolom {@code id}; di-generate database (strategi IDENTITY). */
	private Long id;

	/** Nama tampil pengguna terakhir yang menyunting baris ini (jejak audit ringan). */
	private String oleh;

	/** Id pengguna terakhir yang menyunting baris ini (pasangan {@link #oleh}). */
	private String olehId;

	/** @return id pengguna terakhir yang menyunting baris ini, atau {@code null} bila belum terisi. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna penyunting terakhir, MENGABAIKAN nilai kosong/blank agar proses batch
	 * tanpa pengguna aktif tidak menimpa jejak audit yang sudah tercatat.
	 *
	 * @param olehId id pengguna penyunting; diabaikan bila {@code null} atau kosong setelah di-trim
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * @return representasi ringkas berupa {@code id-masterAsset}, dipakai untuk log dan tampilan
	 *         debug. Memanggil {@link #getMasterAsset()} (yang pada gilirannya bisa menurunkan
	 *         nilai dari {@link #getAssetDetail()}) alih-alih membaca field {@link #masterAsset}
	 *         langsung.
	 */
	public String toString() {
		masterAsset = getMasterAsset();
		return id + "-" + masterAsset + "";
	}

	/**
	 * Mengisi nama pengguna penyunting terakhir, MENGABAIKAN nilai kosong/blank (lihat
	 * {@link #setOlehId(String)}).
	 *
	 * @param oleh nama pengguna penyunting; diabaikan bila {@code null} atau kosong setelah di-trim
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pengguna terakhir yang menyunting baris ini, atau {@code null} bila belum terisi. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate} yang mencatat waktu perubahan lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Field
	 * {@link #tanggal_dirubah} adalah field AUDIT SHADOW -- inisialisasi
	 * {@code = WaktuUtil.getDate()} saat objek dibuat adalah KEHARUSAN TEKNIS, bukan bug.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah waktu perubahan terakhir; biasanya diisi otomatis lewat {@link #onUpdate()}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Jenis barang (katalog {@link MasterAsset}) unit yang dipinjam; bila {@link #assetDetail}
	 * terisi, getter {@link #getMasterAsset()} MENIMPA field ini dengan katalog milik unit
	 * tersebut -- lihat javadoc getter untuk detail.
	 */
	private MasterAsset masterAsset;

	/** Unit fisik ber-barcode ({@code AssetDetail}) yang dipinjam pada baris ini. */
	private AssetDetail assetDetail;

	/** Harga beli unit pada saat peminjaman dicatat; default {@code 0.0} bila belum diisi, lihat {@link #getHargaBeli()}. */
	private Double hargaBeli;

	/** Header dokumen peminjaman yang memiliki baris detail ini. */
	private PeminjamanMasterAsset peminjamanMasterAsset;

	/**
	 * Penanda status kembali PER-UNIT baris ini; {@code false}/{@code null} berarti unit masih
	 * dalam status dipinjam. Lihat catatan kelas mengenai tidak adanya penjagaan double-booking
	 * berdasarkan flag ini.
	 */
	private Boolean dikembalikan;

	/** Waktu pengembalian unit ini; diisi otomatis saat {@link #getDikembalikan()} bernilai true dan waktu belum tercatat. */
	private Date waktuPengembalian;

	/** Catatan/keterangan bebas untuk baris ini. */
	private String keterangan;

	/** Konstruktor default kosong, dibutuhkan Hibernate untuk instansiasi lewat refleksi. */
	public PeminjamanMasterAssetDetail() {
	}

	/** @return kunci utama baris ini, atau {@code null} bila belum persisten. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id kunci utama; kolom {@code insertable=false} sehingga hanya relevan setelah baris ada. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return catatan/keterangan bebas baris ini, atau {@code null} bila tidak ada. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan catatan/keterangan bebas baris ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @param masterAsset katalog jenis barang unit yang dipinjam; dapat ditimpa getter dari {@link #getAssetDetail()} bila terisi. */
	public void setMasterAsset(MasterAsset masterAsset) {
		this.masterAsset = masterAsset;
	}

	/**
	 * @return katalog {@link MasterAsset} unit yang dipinjam. Nilai kolom database di-REFRESH
	 *         lewat {@code check()}, lalu DITIMPA dengan katalog milik {@link #getAssetDetail()}
	 *         (lewat {@code assetDetail.getAsset().getMasterAsset()}) bila unit fisiknya sudah
	 *         ditetapkan -- memastikan katalog selalu konsisten dengan unit fisik aktual, bukan
	 *         nilai yang mungkin tertinggal di kolom {@code master_asset} baris ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "master_asset", nullable = true)
	public MasterAsset getMasterAsset() {
		masterAsset = check(masterAsset);
		if (getAssetDetail() != null && getAssetDetail().getAsset() != null) {
			masterAsset = getAssetDetail().getAsset().getMasterAsset();
		}
		return masterAsset;
	}

	/** @param peminjamanMasterAsset header dokumen peminjaman pemilik baris detail ini. */
	public void setPeminjamanMasterAsset(PeminjamanMasterAsset peminjamanMasterAsset) {
		this.peminjamanMasterAsset = peminjamanMasterAsset;
	}

	/** @return header dokumen peminjaman pemilik baris ini, atau {@code null} bila belum ditetapkan. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "peminjaman_master_asset", nullable = true)
	public PeminjamanMasterAsset getPeminjamanMasterAsset() {
		return peminjamanMasterAsset;
	}

	/** @return harga beli unit pada saat peminjaman dicatat; {@code 0.0} bila belum diisi. */
	public Double getHargaBeli() {
		return hargaBeli == null ? 0.0 : hargaBeli;
	}

	/** @param hargaBeli harga beli unit pada saat peminjaman dicatat. */
	public void setHargaBeli(Double hargaBeli) {
		this.hargaBeli = hargaBeli;
	}

	/**
	 * @return unit fisik ber-barcode ({@code AssetDetail}) yang dipinjam pada baris ini, atau
	 *         {@code null} bila belum ditetapkan. TIDAK ada query di getter ini yang memeriksa
	 *         apakah unit yang sama sedang dipinjam aktif di baris lain -- lihat catatan
	 *         penjagaan double-booking pada javadoc kelas.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "asset_detail", nullable = true)
	public AssetDetail getAssetDetail() {
		return assetDetail;
	}

	/** @param assetDetail unit fisik ber-barcode yang dipinjam pada baris ini. */
	public void setAssetDetail(AssetDetail assetDetail) {
		this.assetDetail = assetDetail;
	}

	/** @return {@code true} bila unit pada baris ini sudah dikembalikan; {@code false} (default) bila masih dipinjam. */
	public Boolean getDikembalikan() {
		return dikembalikan == null ? false : dikembalikan;
	}

	/** @param dikembalikan status kembali unit pada baris ini. */
	public void setDikembalikan(Boolean dikembalikan) {
		this.dikembalikan = dikembalikan;
	}

	/**
	 * @return waktu pengembalian unit ini; bila {@link #getDikembalikan()} bernilai true tetapi
	 *         waktu belum tercatat, DIISI dan DIKEMBALIKAN waktu SAAT DIPANGGIL ({@link
	 *         WaktuUtil#getDate()}) -- efek samping penulisan pada method get, sehingga waktu
	 *         pengembalian tercatat otomatis pada kunjungan pertama setelah flag diaktifkan.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuPengembalian() {
		if (getDikembalikan() && waktuPengembalian == null) {
			waktuPengembalian = WaktuUtil.getDate();
		}
		return waktuPengembalian;
	}

	/** @param waktuPengembalian waktu pengembalian unit ini. */
	public void setWaktuPengembalian(Date waktuPengembalian) {
		this.waktuPengembalian = waktuPengembalian;
	}

}
