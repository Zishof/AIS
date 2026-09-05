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

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.WaktuUtil;

/**
 * <h2>StokAwal — stok/kuantitas awal sebuah {@link MasterAsset} saat migrasi/onboarding sistem.</h2>
 *
 * <p>
 * Entity ini merekam berapa banyak unit sebuah aset (mis. "Kursi Kantor Tipe A") sudah dimiliki
 * organisasi <b>sebelum</b> pencatatan berjalan lewat alur pengadaan normal (permintaan → pemesanan
 * → penerimaan) di paket ini. Kebutuhannya sama dengan pola "saldo awal" di modul akunting: sistem
 * baru perlu titik awal yang bukan hasil transaksi tercatat, agar laporan kuantitas/nilai aset tetap
 * benar sejak hari pertama pemakaian aplikasi, tanpa harus merekonstruksi riwayat pengadaan lama yang
 * tidak pernah masuk sistem.
 * </p>
 *
 * <h3>Relasi ke {@link ais.database.model.asset.DetailTransaksiAsset}</h3>
 * <p>
 * Baris {@code StokAwal} dapat dirujuk balik oleh {@link ais.database.model.asset.DetailTransaksiAsset}
 * (lewat {@code DetailTransaksiAsset.getStokAwal()}) sebagai salah satu dari beberapa kemungkinan
 * sumber sebuah baris log transaksi generik lintas-modul aset — sejajar dengan referensi ke
 * {@link SaldoAwalMasterAssetDetail} (penerimaan pengadaan) dan {@code PemakaianMasterAssetDetail}
 * (pemakaian). Dengan demikian, penetapan stok awal ikut tercatat pada buku besar transaksi aset
 * yang sama dengan jenis pergerakan lain, bukan jalur terpisah yang tidak terlihat pada laporan
 * gabungan.
 * </p>
 *
 * <h3>Pemetaan basis data &amp; audit</h3>
 * <p>
 * Dipetakan ke tabel <code>asset.stok_awal</code>. Field jejak {@code oleh}/{@code olehId}/
 * {@code tanggal_dirubah} diisi otomatis lewat hook {@link javax.persistence.PreUpdate}
 * {@link #onUpdate()} ({@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}), dan
 * setiap perubahan direkam ke tabel revisi Envers karena kelas ditandai
 * {@link org.hibernate.envers.Audited @Audited}.
 * </p>
 *
 * @author AIS
 * @see MasterAsset
 * @see ais.database.model.asset.DetailTransaksiAsset
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "stok_awal")
public class StokAwal extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}; tidak perlu
	 * diubah kecuali bentuk field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key auto-generated (IDENTITY) tabel {@code asset.stok_awal}. */
	private Long id;
	/** Nama pengguna yang terakhir mengubah baris ini; diisi otomatis, lihat {@link #onUpdate()}. */
	private String oleh;
	/** Id pengguna yang terakhir mengubah baris ini; diisi otomatis, lihat {@link #onUpdate()}. */
	private String olehId;

	/**
	 * @return id pengguna yang terakhir mengubah baris ini (audit), atau {@code null} bila belum
	 *         pernah diubah sejak dimuat.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna audit. Nilai {@code null}/kosong diabaikan agar jejak lama tidak
	 * tertimpa hampa.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau blank.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna audit. Nilai {@code null}/kosong diabaikan, sama seperti
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau blank.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna yang terakhir mengubah baris ini (audit), atau {@code null} bila
	 *         belum pernah diubah sejak dimuat.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook siklus hidup JPA yang dipanggil Hibernate tepat sebelum setiap {@code UPDATE}.
	 * Mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}
	 * yang mengisi {@link #tanggal_dirubah}, {@link #oleh}, dan {@link #olehId} dengan waktu serta
	 * identitas pengguna aktif. Dipicu otomatis oleh Hibernate, tidak dipanggil manual.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi waktu perubahan terakhir. Field diinisialisasi ke waktu saat objek dibuat, lalu
	 * ditimpa ulang oleh {@link #onUpdate()} setiap kali baris di-{@code UPDATE}.
	 *
	 * @param tanggal_dirubah waktu perubahan.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini; tidak pernah {@code null}. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi ringkas {@code id-nama} (nama diambil dari {@link MasterAsset} terkait) untuk log/combobox. */
	public String toString() {
		return id + "-" + getNama();
	}

	/** Aset induk yang stok awalnya dicatat oleh baris ini; wajib diisi. */
	private MasterAsset masterAsset;
	/** Satuan kerja pemilik/pencatat stok awal ini; opsional. */
	private SatuanKerja satuanKerja;
	/** Jumlah/kuantitas stok awal; default {@code 0.0} bila belum diisi, lihat {@link #getJumlah()}. */
	private Double jumlah;
	/** Tanggal efektif stok awal ini dicatat; default hari ini bila belum diisi. */
	private Date tanggal;
	/** Keterangan bebas, opsional. */
	private String keterangan;

	/** Konstruktor default tanpa argumen, dipakai Hibernate untuk instansiasi via refleksi. */
	public StokAwal() {
	}

	/** @return primary key baris ini, atau {@code null} untuk instance baru yang belum disimpan. */
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
	 * @param id primary key.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return keterangan bebas baris ini, boleh {@code null}. */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas.
	 *
	 * @param keterangan teks keterangan, boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan aset induk yang stok awalnya dicatat, meresolusi proxy lazy Hibernate lewat
	 * {@link GeneralValueObject#check(Object)}.
	 *
	 * @return {@link MasterAsset} terkait (wajib, tidak boleh {@code null} pada baris tersimpan).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "master_asset", nullable = false)
	public MasterAsset getMasterAsset() {
		masterAsset = check(masterAsset);
		return masterAsset;
	}

	/**
	 * Mengisi aset induk.
	 *
	 * @param masterAsset aset terkait (wajib diisi sebelum simpan).
	 */
	public void setMasterAsset(MasterAsset masterAsset) {
		this.masterAsset = masterAsset;
	}

	/** @return jumlah/kuantitas stok awal; tidak pernah {@code null}, default {@code 0.0}. */
	public Double getJumlah() {
		return jumlah == null ? 0.0 : jumlah;
	}

	/**
	 * Mengisi jumlah/kuantitas stok awal.
	 *
	 * @param jumlah jumlah stok awal, boleh {@code null} (diperlakukan sebagai {@code 0.0}).
	 */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/** @return tanggal efektif stok awal ini; default hari ini bila belum diisi. */
	@Temporal(TemporalType.DATE)
	public Date getTanggal() {
		return tanggal == null ? WaktuUtil.getDate() : tanggal;
	}

	/**
	 * Mengisi tanggal efektif stok awal.
	 *
	 * @param tanggal tanggal, boleh {@code null} (diperlakukan sebagai hari ini oleh
	 *                {@link #getTanggal()}).
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengembalikan satuan kerja pemilik/pencatat stok awal ini, meresolusi proxy lazy Hibernate
	 * lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * @return {@link SatuanKerja} terkait, atau {@code null} bila belum ditetapkan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Mengisi satuan kerja pemilik/pencatat.
	 *
	 * @param satuanKerja satuan kerja terkait, boleh {@code null}.
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

}
