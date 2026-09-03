package ais.database.model.asset;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Entity master <b>Kategori Aset</b> (tabel {@code asset.kategori_asset}) — katalog
 * {@code kode/nama/keterangan/aktif} untuk pengelompokan administratif {@link MasterAsset},
 * dikelola lewat layar CRUD {@code ais.action.master.asset.KategoriAssetAction}.
 *
 * <h2>Bukan sinonim {@link JenisAsset}/{@link StatusAsset}</h2>
 * <p>Ketiga katalog di paket ini punya nama mirip tapi sumbu klasifikasi berbeda dan
 * <b>tidak saling menunjuk</b>: {@link JenisAsset} adalah jenis barang/jasa (dengan label
 * siklus pakai {@code tipe}), {@link StatusAsset} adalah kondisi unit aset saat ini
 * ("Baik"/"Rusak"/dst), sedangkan {@code KategoriAsset} adalah pengelompokan administratif
 * dengan {@code kode} sendiri dan flag {@link #getAktif()} — satu-satunya dari ketiganya yang
 * bisa dinonaktifkan tanpa dihapus.</p>
 *
 * <h2>Flag {@link #getAktif()}: default "aktif" satu arah</h2>
 * <p>Kolom database mengizinkan {@code NULL}, dan getter menafsirkan {@code null} sebagai
 * {@code true} ("aktif" secara default). Baris yang belum pernah eksplisit di-set
 * {@code aktif=false} akan selalu tampil sebagai aktif; sebaliknya baris yang sudah pernah
 * di-set {@code false} tetap nonaktif walau field lain berubah. Default hanya berlaku pada arah
 * baca ({@code getAktif()}), tidak menulis balik nilai eksplisit {@code true} ke database.</p>
 *
 * <h2>Field audit bayangan {@code oleh}/{@code olehId}/{@code tanggal_dirubah}</h2>
 * <p>Tidak diisi langsung oleh kode aplikasi. Hook {@link javax.persistence.PreUpdate}
 * {@link #onUpdate()} mendelegasikan ke
 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang menuliskan
 * pengguna aktif dan waktu perubahan pada setiap {@code UPDATE}. Ini keharusan teknis siklus
 * hidup Hibernate, bukan kode mati.</p>
 *
 * @see MasterAsset
 * @see JenisAsset
 * @see StatusAsset
 * @see ais.action.master.asset.KategoriAssetAction
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "kategori_asset")
public class KategoriAsset extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}; tidak pernah
	 * perlu diubah kecuali bentuk field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key auto-generated (IDENTITY) tabel {@code asset.kategori_asset}. */
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

	/**
	 * Representasi ringkas untuk log/debug dan tampilan combobox generik: {@code id} diikuti
	 * {@link #nama}.
	 *
	 * @return string berformat {@code "<id>-<nama>"}
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode singkat kategori, dipakai sebagai identitas tampil selain {@link #id}. */
	private String kode;
	/** Nama kategori aset. */
	private String nama;
	/** Keterangan bebas, opsional. */
	private String keterangan;
	/** Flag aktif; {@code null} ditafsirkan sebagai aktif oleh {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Konstruktor default tanpa argumen, dipakai Hibernate untuk instansiasi entity via
	 * refleksi dan oleh kode aplikasi saat membuat baris baru sebelum diisi setter.
	 */
	public KategoriAsset() {
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
	 * Mengembalikan nama kategori, di-trim untuk menghindari whitespace tak sengaja dari input.
	 *
	 * @return nama ter-trim, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama kategori. Tidak melakukan trim di sisi setter — trimming terjadi hanya saat
	 * dibaca lewat {@link #getNama()}.
	 *
	 * @param nama nama kategori
	 */
	public void setNama(String nama) {
		this.nama = nama;
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
	 * Mengembalikan status aktif kategori. Lihat catatan kelas: {@code null} database
	 * ditafsirkan sebagai aktif, sehingga baris lama yang belum pernah disentuh field ini akan
	 * selalu tampil aktif.
	 *
	 * @return {@code true} bila aktif atau belum pernah di-set; {@code false} hanya bila
	 *         eksplisit pernah di-set nonaktif
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengisi status aktif. Tidak ada normalisasi di setter — nilai {@code null} yang di-set di
	 * sini tetap tersimpan {@code null} dan akan dibaca sebagai aktif oleh {@link #getAktif()}.
	 *
	 * @param aktif status aktif baru; {@code null} diperbolehkan dan berarti "aktif" saat dibaca
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan kode singkat kategori. Berbeda dari {@link #getNama()}, tidak ada trimming
	 * di sini — nilai dikembalikan apa adanya.
	 *
	 * @return kode kategori, boleh {@code null} bila belum diisi
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Mengisi kode singkat kategori. Tidak dianotasi {@code @Column} — kolom dipetakan lewat
	 * konvensi nama Hibernate default, bukan pemetaan eksplisit, dan tidak ada validasi
	 * keunikan di level entity.
	 *
	 * @param kode kode kategori
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

}
