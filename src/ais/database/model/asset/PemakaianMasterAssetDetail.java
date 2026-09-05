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

/**
 * Baris DETAIL barang pada satu dokumen {@link PemakaianMasterAsset} -- mencatat jenis barang
 * ({@link MasterAsset}, tingkat KATALOG, bukan unit fisik ber-barcode) beserta jumlahnya yang
 * ditugaskan/diserahkan pada dokumen pemakaian tersebut.
 *
 * <h3>Berbasis kuantitas, bukan unit fisik individual</h3>
 *
 * <p>Berbeda dengan {@link PeminjamanMasterAssetDetail} yang menunjuk ke {@code AssetDetail}
 * (satu unit fisik ber-barcode) dan melacak status kembali per-unit, kelas ini menunjuk ke
 * {@link MasterAsset} (katalog jenis barang) dan mencatat {@link #getJumlah()} sebagai kuantitas
 * -- cocok untuk barang habis pakai/persediaan yang diserahkan dalam jumlah tertentu, bukan unit
 * bernomor seri tunggal. Konsekuensinya: TIDAK ada mekanisme di tingkat baris ini yang mencegah
 * jumlah yang diserahkan melebihi stok/saldo yang tersedia -- penjagaan itu (bila ada) berada di
 * lapisan action/helper pemanggil, bukan di entitas ini.</p>
 *
 * <p>Extends {@link ais.database.model.GeneralValueObject} (bukan {@code DataSop}) karena baris
 * detail tidak punya alur disposisi sendiri -- alur persetujuan mengikuti header
 * {@link #getPemakaianMasterAsset()}.</p>
 *
 * @see PemakaianMasterAsset header dokumen pemakaian yang memiliki baris ini
 * @see PeminjamanMasterAssetDetail padanan berbasis unit fisik untuk peminjaman sementara
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "pemakaian_master_asset_detail")
public class PemakaianMasterAssetDetail extends GeneralValueObject {

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
	 *         debug. Memanggil {@link #getMasterAsset()} (bukan membaca field langsung) sehingga
	 *         ikut memicu refresh proxy Hibernate lewat {@code check()}.
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

	/** Jenis barang (katalog {@link MasterAsset}) yang diserahkan pada baris ini. */
	private MasterAsset masterAsset;

	/** Kuantitas barang yang diserahkan; default {@code 1.0} bila belum diisi, lihat {@link #getJumlah()}. */
	private Double jumlah;

	/** Header dokumen pemakaian yang memiliki baris detail ini. */
	private PemakaianMasterAsset pemakaianMasterAsset;

	/** Catatan/keterangan bebas untuk baris ini. */
	private String keterangan;

	/**
	 * Penanda mode agregasi tampilan: {@code true} bila baris ini mewakili ringkasan PER
	 * {@link MasterAsset} (mis. digabung dari beberapa transaksi), {@code false} untuk baris
	 * transaksi tunggal biasa.
	 */
	private Boolean dataPerMasterAsset = false;

	/** Konstruktor default kosong, dibutuhkan Hibernate untuk instansiasi lewat refleksi. */
	public PemakaianMasterAssetDetail() {
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

	/** @param masterAsset jenis barang (katalog) yang diserahkan pada baris ini. */
	public void setMasterAsset(MasterAsset masterAsset) {
		this.masterAsset = masterAsset;
	}

	/**
	 * @return jenis barang (katalog {@link MasterAsset}) baris ini, dilewatkan lewat
	 *         {@code check()} untuk memastikan proxy Hibernate segar (pola umum paket ini).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "master_asset", nullable = true)
	public MasterAsset getMasterAsset() {
		masterAsset = check(masterAsset);
		return masterAsset;
	}

	/** @param jumlah kuantitas barang yang diserahkan pada baris ini. */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * @return kuantitas barang yang diserahkan; bila belum pernah diisi, DIISI dan DIKEMBALIKAN
	 *         nilai default {@code 1.0} (efek samping penulisan pada method get -- pola berulang
	 *         di paket ini). Tidak ada validasi terhadap saldo/stok tersedia di tingkat entitas ini.
	 */
	public Double getJumlah() {
		if (jumlah == null) {
			jumlah = 1.0;
		}
		return jumlah;
	}

	/** @param pemakaianMasterAsset header dokumen pemakaian pemilik baris detail ini. */
	public void setPemakaianMasterAsset(PemakaianMasterAsset pemakaianMasterAsset) {
		this.pemakaianMasterAsset = pemakaianMasterAsset;
	}

	/** @return header dokumen pemakaian pemilik baris ini, atau {@code null} bila belum ditetapkan. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pemakaian_master_asset", nullable = true)
	public PemakaianMasterAsset getPemakaianMasterAsset() {
		return pemakaianMasterAsset;
	}

	/** @return {@code true} bila baris ini mewakili ringkasan per {@link MasterAsset}, {@code false} bila transaksi tunggal. */
	@Column(name = "data_per_master_asset", nullable = true)
	public Boolean getDataPerMasterAsset() {
		return dataPerMasterAsset;
	}

	/** @param dataPerMasterAsset penanda mode agregasi tampilan baris ini. */
	public void setDataPerMasterAsset(Boolean dataPerMasterAsset) {
		this.dataPerMasterAsset = dataPerMasterAsset;
	}

}
