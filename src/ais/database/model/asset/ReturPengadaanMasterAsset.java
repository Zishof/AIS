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
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.ui.util.WaktuUtil;

/**
 * Entity dokumen <b>Retur Pengadaan Aset</b> (tabel {@code asset.retur_pengadaan_master_asset})
 * — pencatatan pengembalian barang ke {@link PenyediaAsset vendor} setelah barang tersebut
 * sebelumnya diterima/dicatat lewat {@link PenerimaanPengadaanMasterAsset} (BAST), biasanya karena
 * cacat/tidak sesuai spesifikasi. Rincian barang yang diretur disimpan sebagai baris
 * {@link ReturPengadaanMasterAssetDetail}.
 *
 * <h2>Tidak ada layar Action/CRUD khusus, tidak ada koreksi nilai buku otomatis</h2>
 * <p>Berbeda dari {@link PenghapusanMasterAsset} dan {@link PengembalianMasterAsset} yang
 * masing-masing punya {@code ais.action.master.asset.PenghapusanMasterAssetAction} dan
 * {@code ais.action.master.asset.PengembalianMasterAssetAction} sebagai layar input/persetujuan
 * khusus, penelusuran repo <b>tidak menemukan kelas Action/CRUD berdedikasi</b> untuk entity ini —
 * satu-satunya pemakai yang ditemukan adalah
 * {@code ais.action.master.asset.helper.DasboardAnalisisVendor} (dasbor analisis vendor), yang
 * hanya MEMBACA (query {@code Criteria}, tanpa insert/update/delete) baris retur untuk dihitung
 * sebagai metrik {@code returCount} per vendor. Tidak ada kode di repo yang, saat sebuah retur
 * dibuat, ikut mengoreksi nilai buku {@code MasterAsset}/{@code AssetDetail} yang diterima
 * sebelumnya, membalik jurnal penerimaan, atau menandai baris {@code PenerimaanPengadaanMasterAssetDetail}
 * terkait sebagai "sudah diretur" — retur pengadaan aset di modul ini murni <b>catatan
 * administratif terpisah</b>, bukan koreksi otomatis atas transaksi penerimaan/nilai buku yang
 * sudah tercatat.</p>
 *
 * <h2>{@link #getPenerimaanPengadaanMasterAsset()}: relasi opsional, longgar</h2>
 * <p>Field ini menghubungkan retur ke BAST asal (opsional, {@code nullable = true}) tapi tidak ada
 * penjaga yang memvalidasi bahwa barang yang diretur ({@link ReturPengadaanMasterAssetDetail})
 * benar-benar berasal dari BAST tersebut, atau bahwa kuantitas retur tidak melebihi kuantitas
 * diterima — validasi semacam itu, bila diperlukan, harus dilakukan di lapisan pemanggil (layar
 * generik CRUD atau proses batch yang membuat entity ini), bukan di sini.</p>
 *
 * @see ReturPengadaanMasterAssetDetail
 * @see PenerimaanPengadaanMasterAsset
 * @see ais.action.master.asset.helper.DasboardAnalisisVendor
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "retur_pengadaan_master_asset")

public class ReturPengadaanMasterAsset extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}; tidak pernah
	 * perlu diubah kecuali bentuk field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key auto-generated (IDENTITY) tabel {@code asset.retur_pengadaan_master_asset}. */
	private Long id;
	/** Nomor urut tampilan (mis. urutan baris pada grid/laporan); tidak dipakai untuk logika bisnis. */
	private Long index;
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
	 * Representasi ringkas untuk log/debug dan tampilan combobox generik.
	 *
	 * @return {@link #kode} dokumen retur, boleh {@code null} bila belum diisi
	 */
	public String toString() {
		return kode;
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

	/** Kode dokumen retur, unik. */
	private String kode;
	/** Keterangan/alasan retur, bebas teks. */
	private String keterangan;
	/** Vendor/penyedia tujuan retur. */
	private PenyediaAsset penyedia;
	/** Pemilik aset (mis. satuan kerja/entitas pemilik) terkait dokumen retur ini. */
	private PemilikAsset pemilikAsset;
	/** Lokasi asal barang yang diretur. */
	private Lokasi lokasi;
	/** Ruang spesifik asal barang yang diretur, meresolusi proxy lazy lewat {@link #getRuang()}. */
	private Ruang ruang;
	/** BAST/penerimaan asal barang yang diretur; opsional, tidak divalidasi ketat — lihat catatan kelas. */
	private PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset;
	/** Tanggal dokumen retur dibuat; lihat {@link #getTanggalPembuatan()} untuk fallback default. */
	private Date tanggalPembuatan;
	/** Tanggal dokumen retur disetujui. */
	private Date tanggalPersetujuan;
	/** Pengguna pembuat dokumen. */
	private Tbmuser dibuatOleh;
	/** Pengguna penyetuju dokumen. */
	private Tbmuser disetujuiOleh;

	/**
	 * Konstruktor default tanpa argumen, dipakai Hibernate untuk instansiasi entity via
	 * refleksi dan oleh kode aplikasi saat membuat baris baru sebelum diisi setter.
	 */
	public ReturPengadaanMasterAsset() {
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
	 * Mengembalikan kode dokumen. Berbeda dari beberapa entity sejenis di paket ini (mis.
	 * {@link PenghapusanMasterAsset#getKode()}), getter ini <b>tidak melakukan trim</b> —
	 * mengembalikan nilai kolom apa adanya.
	 *
	 * @return kode dokumen, boleh {@code null}
	 */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return this.kode;
	}

	/**
	 * Mengisi kode dokumen.
	 *
	 * @param kode kode dokumen, harus unik pada tabel
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan keterangan/alasan retur bebas teks.
	 *
	 * @return keterangan, boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan/alasan retur.
	 *
	 * @param keterangan teks keterangan, boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengisi pengguna pembuat dokumen.
	 *
	 * @param dibuatOleh pengguna pembuat
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengembalikan pengguna pembuat dokumen. Berbeda dari
	 * {@link PenghapusanMasterAsset}/{@link PengembalianMasterAsset} yang mewarisi
	 * {@code DataSop} dan mensinkronkan nilai ini dari disposisi SOP, kelas ini mewarisi
	 * {@link GeneralValueObject} langsung — getter murni mengembalikan field tersimpan tanpa
	 * derivasi tambahan.
	 *
	 * @return pengguna pembuat, boleh {@code null} untuk baris yang belum lengkap
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "dibuat_oleh", nullable = false)
	public Tbmuser getDibuatOleh() {
		return dibuatOleh;
	}

	/**
	 * Mengisi pengguna penyetuju dokumen.
	 *
	 * @param disetujuiOleh pengguna penyetuju
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengembalikan pengguna penyetuju dokumen, murni field tersimpan (tidak ada sinkronisasi
	 * disposisi SOP seperti pada {@link PenghapusanMasterAsset}).
	 *
	 * @return pengguna penyetuju, atau {@code null} bila belum disetujui
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		return disetujuiOleh;
	}

	/**
	 * Mengisi tanggal pembuatan dokumen.
	 *
	 * @param tanggalPembuatan tanggal pembuatan
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Mengembalikan tanggal pembuatan dokumen. Bila belum pernah diisi, dikembalikan
	 * {@link WaktuUtil#getDate()} (waktu saat ini) sebagai fallback, tanpa derivasi dari
	 * disposisi SOP (berbeda dari {@code DataSop}).
	 *
	 * @return tanggal pembuatan; tidak pernah {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		return tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan;
	}

	/**
	 * Mengisi tanggal persetujuan dokumen.
	 *
	 * @param tanggalPersetujuan tanggal persetujuan
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengembalikan tanggal persetujuan dokumen, murni field tersimpan tanpa fallback maupun
	 * derivasi dari disposisi SOP.
	 *
	 * @return tanggal persetujuan, atau {@code null} bila belum disetujui
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {
		return tanggalPersetujuan;
	}

	/**
	 * Mengisi nomor urut tampilan.
	 *
	 * @param index nomor urut, tidak dipakai untuk logika bisnis
	 */
	public void setIndex(Long index) {
		this.index = index;
	}

	/**
	 * Mengembalikan nomor urut tampilan.
	 *
	 * @return nomor urut, boleh {@code null}
	 */
	public Long getIndex() {
		return index;
	}

	/**
	 * Mengembalikan vendor/penyedia tujuan retur. Dibaca langsung oleh
	 * {@code DasboardAnalisisVendor} untuk menghitung metrik {@code returCount} per vendor — lihat
	 * catatan kelas.
	 *
	 * @return vendor tujuan retur, boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penyedia", nullable = true)
	public PenyediaAsset getPenyedia() {
		return penyedia;
	}

	/**
	 * Mengisi vendor/penyedia tujuan retur.
	 *
	 * @param penyedia vendor tujuan retur
	 */
	public void setPenyedia(PenyediaAsset penyedia) {
		this.penyedia = penyedia;
	}

	/**
	 * Mengembalikan BAST/penerimaan asal barang yang diretur. Relasi opsional dan tidak
	 * divalidasi ketat — lihat catatan kelas.
	 *
	 * @return penerimaan asal, boleh {@code null} bila retur tidak ditautkan ke BAST spesifik
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penerimaan_pengadaan_master_asset", nullable = true)
	public PenerimaanPengadaanMasterAsset getPenerimaanPengadaanMasterAsset() {
		return penerimaanPengadaanMasterAsset;
	}

	/**
	 * Mengisi BAST/penerimaan asal barang yang diretur.
	 *
	 * @param penerimaanPengadaanMasterAsset penerimaan asal
	 */
	public void setPenerimaanPengadaanMasterAsset(PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset) {
		this.penerimaanPengadaanMasterAsset = penerimaanPengadaanMasterAsset;
	}

	/**
	 * Mengembalikan pemilik aset terkait dokumen retur ini.
	 *
	 * @return pemilik aset, boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pemilik_asset", nullable = true)
	public PemilikAsset getPemilikAsset() {
		return pemilikAsset;
	}

	/**
	 * Mengisi pemilik aset terkait dokumen retur ini.
	 *
	 * @param pemilikAsset pemilik aset
	 */
	public void setPemilikAsset(PemilikAsset pemilikAsset) {
		this.pemilikAsset = pemilikAsset;
	}

	/**
	 * Mengembalikan lokasi asal barang yang diretur.
	 *
	 * @return lokasi, boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "lokasi", nullable = true)
	public Lokasi getLokasi() {
		return lokasi;
	}

	/**
	 * Mengisi lokasi asal barang yang diretur.
	 *
	 * @param lokasi lokasi asal
	 */
	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * Mengembalikan ruang spesifik asal barang yang diretur, meresolusi proxy lazy lewat
	 * {@link #check(Object)}.
	 *
	 * @return ruang asal, boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ruang", nullable = true)
	public Ruang getRuang() {
		ruang = check(ruang);
		return this.ruang;
	}

	/**
	 * Mengisi ruang spesifik asal barang yang diretur.
	 *
	 * @param ruang ruang asal
	 */
	public void setRuang(Ruang ruang) {
		this.ruang = ruang;
	}
}
