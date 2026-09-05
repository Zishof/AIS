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
 * Baris rincian satu item {@link AssetDetail} yang tercakup dalam dokumen
 * {@link PenghapusanMasterAsset} (tabel {@code asset.penghapusan_master_asset_detail}). Dibangun
 * dan dirender oleh {@code ais.action.master.asset.helper.PenghapusanMasterAssetHelper} dan
 * {@code ais.action.master.asset.helper.PenghapusanMasterAssetDetailAction}.
 *
 * <h2>Referensi, bukan penghapusan: {@link #getAssetDetail()}/{@link #getMasterAsset()} tidak pernah di-{@code DELETE}</h2>
 * <p>Baris ini hanya MEREFERENSIKAN {@link AssetDetail} dan {@link MasterAsset} lewat foreign key
 * — proses "penghapusan aset" tidak menghapus baris {@code AssetDetail}/{@code MasterAsset} induk
 * dari database (lihat catatan kelas {@link PenghapusanMasterAsset}). Yang benar-benar bisa dihapus
 * lewat UI adalah baris {@link PenghapusanMasterAssetDetail} itu sendiri — baik lewat tombol hapus
 * baris individual maupun ikut terhapus saat dokumen induknya dihapus — dan keduanya hanya
 * ditampilkan/diizinkan selama dokumen induk belum disetujui
 * ({@code penghapusanMasterAsset.getDisetujuiOleh() == null}).</p>
 *
 * <h2>{@link #getPenyusutanAsset()}: mengunci nilai buku pada bulan penghapusan</h2>
 * <p>Diisi oleh {@code PenghapusanMasterAssetHelper.masukkanPenyusutan(...)} saat baris ini
 * dibuat: method tersebut memastikan rangkaian baris {@link PenyusutanAsset} bulanan sejak tanggal
 * beli {@link AssetDetail} sampai tanggal pembuatan dokumen penghapusan sudah ada (membuat yang
 * belum ada), lalu mengikat baris penyusutan pada bulan/tahun penghapusan ke sini — sehingga
 * {@link PenyusutanAsset#getNilaiBuku()} yang ditampilkan di grid selalu konsisten dengan histori
 * penyusutan aset sampai titik penghapusan, bukan nilai buku "hari ini".</p>
 *
 * <h2>{@link #getHargaBeli()}: harga jual/nilai realisasi, ikut menyinkronkan {@code MasterAsset}</h2>
 * <p>Meski namanya "hargaBeli", field ini dipakai UI sebagai <b>harga jual</b> aset yang dihapus
 * (kolom "Harga Jual" pada grid) dan turut dijumlahkan menjadi {@link PenghapusanMasterAsset#getNilai()}
 * dokumen induk. Saat diedit di grid, listener perubahan pada helper JUGA menulis nilai yang sama
 * ke {@code masterAsset.setHargaBeliDefault(...)} — efek samping lintas-entity yang membuat harga
 * jual satu baris penghapusan bisa mengubah harga beli default {@link MasterAsset} yang mungkin
 * masih dipakai transaksi lain (mis. saldo awal, retur).</p>
 *
 * @see PenghapusanMasterAsset
 * @see PenyusutanAsset
 * @see ais.action.master.asset.helper.PenghapusanMasterAssetHelper
 * @see ais.action.master.asset.helper.PenghapusanMasterAssetDetailAction
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "penghapusan_master_asset_detail")
public class PenghapusanMasterAssetDetail extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}; tidak pernah
	 * perlu diubah kecuali bentuk field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key auto-generated (IDENTITY) tabel {@code asset.penghapusan_master_asset_detail}. */
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
	 * efek samping, memanggil {@code toString()} memaksa resolusi {@link #masterAsset} (termasuk
	 * jalur penurunan dari {@link #getAssetDetail()} bila ada) — bukan operasi baca murni tanpa
	 * efek.
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

	/** Cache field {@code MasterAsset}; lihat {@link #getMasterAsset()} — bisa diturunkan dari {@link #assetDetail}. */
	private MasterAsset masterAsset;
	/** Item {@code AssetDetail} spesifik yang dihapus/ditulis-off oleh baris ini. */
	private AssetDetail assetDetail;
	/** Nilai realisasi/harga jual baris ini; lihat catatan kelas — nama field menyesatkan (bukan harga beli asli). */
	private Double hargaBeli;
	/** Dokumen induk {@link PenghapusanMasterAsset} yang memuat baris ini. */
	private PenghapusanMasterAsset penghapusanMasterAsset;
	/** Baris {@link PenyusutanAsset} yang menjadi acuan nilai buku pada bulan penghapusan; lihat catatan kelas. */
	private PenyusutanAsset penyusutanAsset;
	/** Keterangan bebas per baris (mis. kondisi barang, alasan spesifik). */
	private String keterangan;

	/**
	 * Konstruktor default tanpa argumen, dipakai Hibernate untuk instansiasi entity via
	 * refleksi dan oleh kode aplikasi saat membuat baris baru sebelum diisi setter.
	 */
	public PenghapusanMasterAssetDetail() {
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
	 * Mengisi field mentah {@code MasterAsset}. Nilai ini bisa ditimpa oleh
	 * {@link #getMasterAsset()} bila {@link #assetDetail} terisi dan punya rantai relasi ke
	 * {@code Asset}/{@code MasterAsset} — lihat javadoc getter tersebut.
	 *
	 * @param masterAsset master asset terkait
	 */
	public void setMasterAsset(MasterAsset masterAsset) {
		this.masterAsset = masterAsset;
	}

	/**
	 * Mengembalikan {@code MasterAsset} (kategori/model barang) yang terkait baris ini. Bila
	 * {@link #getAssetDetail()} terisi dan rantai relasinya sampai ke {@code Asset.getMasterAsset()}
	 * berhasil, nilai tersebut MENIMPA field mentah {@link #masterAsset} — memastikan
	 * {@code MasterAsset} yang ditampilkan selalu konsisten dengan item spesifik yang dihapus,
	 * bukan sekadar nilai yang kebetulan tersimpan langsung ke kolom {@code masterasset}. Hanya
	 * jatuh ke field mentah (diresolusi lewat {@link #check(Object)}) bila {@code assetDetail}
	 * kosong atau rantai relasinya putus.
	 *
	 * @return master asset efektif, atau {@code null} bila tidak bisa ditentukan dari kedua
	 *         sumber
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "masterasset", nullable = true)
	public MasterAsset getMasterAsset() {
		if (getAssetDetail() != null && getAssetDetail().getAsset() != null) {
			masterAsset = getAssetDetail().getAsset().getMasterAsset();
		} else {
			masterAsset = check(masterAsset);
		}
		return masterAsset;
	}

	/**
	 * Mengisi dokumen induk {@link PenghapusanMasterAsset}.
	 *
	 * @param penghapusanMasterAsset dokumen induk
	 */
	public void setPenghapusanMasterAsset(PenghapusanMasterAsset penghapusanMasterAsset) {
		this.penghapusanMasterAsset = penghapusanMasterAsset;
	}

	/**
	 * Mengembalikan dokumen induk {@link PenghapusanMasterAsset} baris ini. Dipakai secara luas
	 * oleh helper UI untuk memeriksa {@code getDisetujuiOleh() == null} sebelum mengizinkan
	 * baris ini diubah/dihapus — lihat catatan kelas.
	 *
	 * @return dokumen induk, boleh {@code null} bila baris belum sempat dipasangkan (jarang
	 *         terjadi dalam alur normal)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penghapusan_master_asset", nullable = true)
	public PenghapusanMasterAsset getPenghapusanMasterAsset() {
		return penghapusanMasterAsset;
	}

	/**
	 * Mengembalikan nilai realisasi/harga jual baris ini, dengan {@code null} dinormalisasi
	 * menjadi {@code 0.0}. Meski nama field/method-nya "hargaBeli", nilai ini dipakai UI sebagai
	 * <b>harga jual</b> hasil penghapusan dan ikut dijumlahkan ke
	 * {@link PenghapusanMasterAsset#getNilai()} — lihat catatan kelas.
	 *
	 * @return nilai realisasi baris ini, tidak pernah {@code null}
	 */
	public Double getHargaBeli() {
		return hargaBeli == null ? 0.0 : hargaBeli;
	}

	/**
	 * Mengisi nilai realisasi/harga jual baris ini. Dipanggil oleh helper grid setiap kali
	 * pengguna mengedit kolom "Harga Jual"; pemanggil JUGA menulis nilai yang sama ke
	 * {@code masterAsset.setHargaBeliDefault(...)} sebagai efek samping terpisah — lihat catatan
	 * kelas.
	 *
	 * @param hargaBeli nilai realisasi baru, boleh {@code null} (akan dibaca sebagai 0.0)
	 */
	public void setHargaBeli(Double hargaBeli) {
		this.hargaBeli = hargaBeli;
	}

	/**
	 * Mengembalikan item {@link AssetDetail} spesifik yang dihapus/ditulis-off oleh baris ini.
	 *
	 * @return asset detail terkait, boleh {@code null} untuk baris lama yang dibuat tanpa
	 *         memilih item spesifik (mis. lewat {@code loadBarcode()} pada
	 *         {@code PenghapusanMasterAssetDetailAction})
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "asset_detail", nullable = true)
	public AssetDetail getAssetDetail() {
		return assetDetail;
	}

	/**
	 * Mengisi item {@code AssetDetail} spesifik. Mempengaruhi hasil {@link #getMasterAsset()}
	 * pada pembacaan berikutnya — lihat javadoc getter tersebut.
	 *
	 * @param assetDetail asset detail yang dihapus
	 */
	public void setAssetDetail(AssetDetail assetDetail) {
		this.assetDetail = assetDetail;
	}

	/**
	 * Mengembalikan baris {@link PenyusutanAsset} yang menjadi acuan nilai buku pada bulan
	 * penghapusan — lihat catatan kelas untuk cara pengisiannya oleh
	 * {@code PenghapusanMasterAssetHelper.masukkanPenyusutan(...)}.
	 *
	 * @return baris penyusutan acuan, boleh {@code null} bila belum pernah dihitungkan (mis.
	 *         baris dibuat lewat jalur yang tidak memanggil {@code masukkanPenyusutan})
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penyusutan_asset", nullable = true)
	public PenyusutanAsset getPenyusutanAsset() {
		return penyusutanAsset;
	}

	/**
	 * Mengisi baris penyusutan acuan.
	 *
	 * @param penyusutanAsset baris penyusutan yang jadi acuan nilai buku
	 */
	public void setPenyusutanAsset(PenyusutanAsset penyusutanAsset) {
		this.penyusutanAsset = penyusutanAsset;
	}

}
