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
 * Baris rincian satu jenis barang ({@link MasterAsset}) yang tercakup dalam dokumen
 * {@link ReturPengadaanMasterAsset} (tabel {@code asset.retur_pengadaan_master_asset_detail}).
 *
 * <h2>Level {@code MasterAsset}, bukan {@code AssetDetail} per unit</h2>
 * <p>Berbeda dari {@link PenghapusanMasterAssetDetail} yang menunjuk item {@code AssetDetail}
 * spesifik per unit, baris retur ini bekerja pada level {@link MasterAsset} (kategori/model
 * barang) dengan {@link #getJumlah() kuantitas} — konsisten dengan sifat retur pengadaan yang
 * biasanya berupa barang belum berupa unit individual bernomor (mis. sebelum diberi barcode aset
 * tetap), atau barang habis pakai/consumable.</p>
 *
 * <h2>{@link #getDikembalikan()}: field pelacak progres yang TIDAK diisi otomatis oleh kode manapun</h2>
 * <p>Nama field ini menyiratkan pelacakan berapa banyak dari {@link #getJumlah()} yang sudah
 * benar-benar dikembalikan/diproses vendor, namun penelusuran repo tidak menemukan kode di jalur
 * mana pun (dashboard, action, helper) yang menulis nilai ke field ini secara otomatis —
 * konsisten dengan catatan pada {@link ReturPengadaanMasterAsset} bahwa entity ini tidak dikelola
 * lewat layar Action berdedikasi. Nilai field ini (bila terisi) kemungkinan hanya hasil input
 * manual lewat layar CRUD generik, bukan hasil rekonsiliasi otomatis terhadap status retur nyata
 * di sisi vendor.</p>
 *
 * <h2>{@link #getHargaBeli()}: default lazy dari {@code MasterAsset}, hanya sekali lalu ter-cache permanen</h2>
 * <p>Getter mengisi field {@link #hargaBeli} dari {@code masterAsset.getHargaBeliDefault()} hanya
 * ketika field masih {@code null} dan field mentah {@link #masterAsset} sudah terisi. Setelah
 * berhasil terisi (baik dari default ini maupun dari {@link #setHargaBeli(Double)} eksplisit),
 * nilainya tidak pernah dihitung ulang oleh getter ini lagi — bila {@link #setMasterAsset(MasterAsset)}
 * dipanggil BELAKANGAN dengan {@code MasterAsset} yang berbeda (mis. pengguna mengganti pilihan
 * barang pada baris yang sama), {@link #hargaBeli} yang sudah ter-cache TIDAK ikut diperbarui
 * mengikuti harga default barang yang baru — berpotensi baris retur menampilkan harga beli barang
 * lama meski {@code masterAsset}-nya sudah berganti.</p>
 *
 * @see ReturPengadaanMasterAsset
 * @see MasterAsset
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "retur_pengadaan_master_asset_detail")
public class ReturPengadaanMasterAssetDetail extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}; tidak pernah
	 * perlu diubah kecuali bentuk field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key auto-generated (IDENTITY) tabel {@code asset.retur_pengadaan_master_asset_detail}. */
	private Long id;
	/** Nama pengguna yang terakhir mengubah baris ini; diisi otomatis, lihat catatan kelas. */
	private String oleh;
	/** Id pengguna yang terakhir mengubah baris ini; diisi otomatis, lihat catatan kelas. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna audit, atau {@code null} bila belum pernah diubah sejak dimuat
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna audit. Guard di awal method membuat setter ini diam-diam mengabaikan
	 * nilai {@code null}/blank — tidak menghapus nilai lama, berbeda dari setter lain di kelas
	 * ini yang selalu menimpa.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau blank
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Representasi ringkas untuk log/debug: id baris disambung {@link #getMasterAsset()}. Sebagai
	 * efek samping, memanggil {@code toString()} memaksa resolusi {@link #masterAsset} lewat
	 * {@link #check(Object)} — bukan operasi baca murni tanpa efek.
	 *
	 * @return string berformat {@code "<id>-<masterAsset>"}
	 */
	public String toString() {
		masterAsset = getMasterAsset();
		return id + "-" + masterAsset + "";
	}

	/**
	 * Mengisi nama pengguna audit. Guard yang sama seperti {@link #setOlehId(String)} membuat
	 * nilai {@code null}/blank diabaikan, bukan menghapus nilai lama.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau blank
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna audit, atau {@code null} bila belum pernah diubah sejak dimuat
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook siklus hidup JPA yang dipanggil Hibernate tepat sebelum setiap {@code UPDATE}.
	 * Mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}
	 * yang mengisi {@link #tanggal_dirubah}, {@link #oleh}, dan {@link #olehId} dengan waktu
	 * serta identitas pengguna aktif. Dipicu otomatis oleh Hibernate lewat
	 * {@link javax.persistence.PreUpdate}, tidak dipanggil manual di tempat lain.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi waktu perubahan terakhir. Field diinisialisasi ke waktu saat ini pada konstruksi
	 * objek, lalu ditimpa ulang oleh {@link #onUpdate()} setiap kali baris di-{@code UPDATE}.
	 *
	 * @param tanggal_dirubah waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini.
	 *
	 * @return timestamp perubahan terakhir; tidak pernah {@code null} karena field diinisialisasi
	 *         saat objek dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Jenis/kategori barang ({@link MasterAsset}) yang diretur pada baris ini. */
	private MasterAsset masterAsset;
	/** Kuantitas barang yang diretur; default {@code 0.0} bila belum diisi. */
	private Double jumlah = 0.0;
	/** Kuantitas yang sudah "dikembalikan"/diproses; lihat catatan kelas — tidak diisi otomatis oleh kode manapun. */
	private Double dikembalikan = 0.0;
	/** Dokumen induk {@link ReturPengadaanMasterAsset} yang memuat baris ini. */
	private ReturPengadaanMasterAsset returPengadaanMasterAsset;
	/** Keterangan bebas per baris (mis. alasan retur spesifik barang ini). */
	private String keterangan;
	/** Harga beli per unit; lihat {@link #getHargaBeli()} untuk perilaku default lazy dari {@code MasterAsset}. */
	private Double hargaBeli;

	/**
	 * Konstruktor default tanpa argumen, dipakai Hibernate untuk instansiasi entity via
	 * refleksi dan oleh kode aplikasi saat membuat baris baru sebelum diisi setter.
	 */
	public ReturPengadaanMasterAssetDetail() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * @return id, atau {@code null} untuk instance baru yang belum disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi primary key. Kolom database bersifat {@code insertable = false} (IDENTITY,
	 * auto-generate oleh database), sehingga pengisian manual tidak berpengaruh pada
	 * {@code INSERT}.
	 *
	 * @param id primary key
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan keterangan bebas baris ini.
	 *
	 * @return keterangan, boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas.
	 *
	 * @param keterangan teks keterangan, boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengisi jenis/kategori barang yang diretur. Lihat catatan kelas: bila dipanggil setelah
	 * {@link #getHargaBeli()} sudah pernah men-cache nilai non-null, harga beli lama TIDAK ikut
	 * diperbarui mengikuti {@code MasterAsset} yang baru.
	 *
	 * @param masterAsset jenis/kategori barang
	 */
	public void setMasterAsset(MasterAsset masterAsset) {
		this.masterAsset = masterAsset;
	}

	/**
	 * Mengembalikan jenis/kategori barang ({@link MasterAsset}) yang diretur pada baris ini,
	 * meresolusi proxy lazy lewat {@link #check(Object)}.
	 *
	 * @return master asset terkait, boleh {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "masterAsset", nullable = true)
	public MasterAsset getMasterAsset() {
		masterAsset = check(masterAsset);
		return masterAsset;
	}

	/**
	 * Mengisi kuantitas barang yang diretur.
	 *
	 * @param jumlah kuantitas retur
	 */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * Mengembalikan kuantitas barang yang diretur pada baris ini. Berbeda dari beberapa entity
	 * sejenis, field ini diinisialisasi {@code 0.0} pada deklarasi (bukan dinormalisasi di
	 * getter), sehingga nilainya hanya bisa {@code null} bila secara eksplisit di-{@code set(null)}.
	 *
	 * @return kuantitas retur, biasanya tidak {@code null} kecuali di-set eksplisit
	 */
	public Double getJumlah() {
		return jumlah;
	}

	/**
	 * Mengisi dokumen induk {@link ReturPengadaanMasterAsset}.
	 *
	 * @param returPengadaanMasterAsset dokumen induk
	 */
	public void setReturPengadaanMasterAsset(ReturPengadaanMasterAsset returPengadaanMasterAsset) {
		this.returPengadaanMasterAsset = returPengadaanMasterAsset;
	}

	/**
	 * Mengembalikan dokumen induk {@link ReturPengadaanMasterAsset} baris ini.
	 *
	 * @return dokumen induk, boleh {@code null} bila baris belum sempat dipasangkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "retur_pengadaan_master_asset", nullable = true)
	public ReturPengadaanMasterAsset getReturPengadaanMasterAsset() {
		return returPengadaanMasterAsset;
	}

	/**
	 * Mengembalikan kuantitas yang sudah "dikembalikan"/diproses. Lihat catatan kelas: tidak ada
	 * kode di repo yang menulis nilai ini secara otomatis berdasarkan status retur nyata di sisi
	 * vendor — nilai yang terbaca murni hasil input manual (bila ada).
	 *
	 * @return kuantitas dikembalikan, default {@code 0.0} pada deklarasi field
	 */
	public Double getDikembalikan() {
		return dikembalikan;
	}

	/**
	 * Mengisi kuantitas yang sudah "dikembalikan"/diproses.
	 *
	 * @param dikembalikan kuantitas dikembalikan
	 */
	public void setDikembalikan(Double dikembalikan) {
		this.dikembalikan = dikembalikan;
	}

	/**
	 * Mengembalikan harga beli per unit baris ini. Bila field masih {@code null} dan
	 * {@link #masterAsset} (field mentah, bukan lewat {@link #getMasterAsset()}) sudah terisi,
	 * nilai di-default-lazy dari {@code masterAsset.getHargaBeliDefault()} dan DI-CACHE ke field
	 * — panggilan berikutnya tidak menghitung ulang. Lihat catatan kelas untuk implikasi bila
	 * {@code masterAsset} diganti setelah cache ini terisi.
	 *
	 * @return harga beli per unit; bisa tetap {@code null} bila {@link #masterAsset} juga
	 *         {@code null} saat pertama kali dipanggil
	 */
	public Double getHargaBeli() {
		if (hargaBeli == null && masterAsset != null) {
			hargaBeli = masterAsset.getHargaBeliDefault();
		}
		return hargaBeli;
	}

	/**
	 * Mengisi harga beli per unit secara eksplisit, mem-bypass default lazy
	 * {@link #getHargaBeli()}.
	 *
	 * @param hargaBeli harga beli per unit
	 */
	public void setHargaBeli(Double hargaBeli) {
		this.hargaBeli = hargaBeli;
	}

}
