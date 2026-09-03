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
 * Entity master <b>Jenis Aset</b> (tabel {@code asset.jenis_asset}) — katalog datar
 * {@code kode/nama/keterangan} yang mengklasifikasikan {@link MasterAsset} berdasarkan
 * jenis barang/jasa (mis. "Kendaraan", "Peralatan Kantor", "Jasa Konsultasi"). Dipilih lewat
 * combobox di layar CRUD {@code ais.action.master.asset.JenisAssetAction} dan diakses lewat
 * {@code ais.database.dao.asset.JenisAssetDao}.
 *
 * <h2>Bukan satu-satunya sumbu klasifikasi</h2>
 * <p>Paket ini punya tiga entity katalog yang namanya mirip tapi maknanya berbeda dan
 * <b>tidak saling menunjuk</b> — masing-masing kolom terpisah di {@code MasterAsset}:</p>
 * <ul>
 *   <li>{@code JenisAsset} (kelas ini) — jenis barang/jasa apa itu, ditambah label bebas
 *       {@link #getTipe()} yang mengelompokkan menurut siklus pakai (habis pakai/tidak habis
 *       pakai/jasa).</li>
 *   <li>{@link KategoriAsset} — pengelompokan administratif terpisah, punya {@code kode} dan
 *       flag {@code aktif} sendiri; tidak mewarisi atau merujuk {@code JenisAsset}.</li>
 *   <li>{@link StatusAsset} — status siklus hidup unit aset (mis. "Baik", "Rusak", "Hilang"),
 *       sumbu yang sama sekali berbeda dari jenis maupun kategori.</li>
 * </ul>
 *
 * <h2>Field {@link #getTipe()}: label bebas, bukan enum type-safe</h2>
 * <p>Kolom {@code tipe} adalah {@code String} lepas (tanpa {@code @Column}, tanpa validasi di
 * entity) yang di layar {@code JenisAssetAction} diisi lewat combobox <i>readonly</i> berisi
 * konstanta {@code MasterAsset.TIPE_TIDAK_HABIS_PAKAI}, {@code MasterAsset.TIPE_HABIS_PAKAI},
 * {@code MasterAsset.TIPE_JASA}, {@code MasterAsset.TIPE_TIDAK_HABIS_PAKAI_NON_ASET}, atau
 * {@code null} ("Tidak Ditentukan"). Karena tidak ada constraint database maupun enum Java yang
 * memaksanya, baris yang dibuat lewat jalur lain (impor, skrip, API) bisa saja mengisi nilai di
 * luar keempat konstanta tersebut tanpa ditolak.</p>
 *
 * <h2>Field audit bayangan {@code oleh}/{@code olehId}/{@code tanggal_dirubah}</h2>
 * <p>Ketiga field ini tidak pernah diisi oleh kode aplikasi secara langsung. Keberadaannya
 * adalah <b>keharusan teknis</b>: hook {@link javax.persistence.PreUpdate} {@link #onUpdate()}
 * memanggil {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} pada setiap
 * {@code UPDATE}, yang menuliskan pengguna aktif dan waktu perubahan lewat setter-nya sendiri.
 * Jangan menganggap field tanpa pembaca eksplisit ini sebagai kode mati.</p>
 *
 * @see MasterAsset
 * @see KategoriAsset
 * @see StatusAsset
 * @see ais.action.master.asset.JenisAssetAction
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "jenis_asset")
public class JenisAsset extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}; tidak pernah
	 * perlu diubah kecuali bentuk field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key auto-generated (IDENTITY) tabel {@code asset.jenis_asset}. */
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
	 * Mengisi id pengguna audit. Guard di awal method membuat setter ini <b>diam-diam mengabaikan
	 * nilai kosong</b> — pemanggilan dengan {@code null} atau string kosong/spasi tidak
	 * menghapus nilai lama, berbeda dari setter lain di kelas ini yang selalu menimpa.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau blank
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna audit. Sama seperti {@link #setOlehId(String)}, guard di awal
	 * method membuat nilai {@code null}/blank <b>diabaikan</b>, bukan menghapus nilai lama.
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
	 * serta identitas pengguna aktif saat ini. Tidak dipanggil manual di mana pun secara sengaja
	 * — Hibernate yang memicunya lewat anotasi {@link javax.persistence.PreUpdate}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi waktu perubahan terakhir. Nilai default konstruksi objek adalah waktu saat ini
	 * (lihat inisialisasi field), lalu ditimpa ulang oleh {@link #onUpdate()} setiap kali baris
	 * benar-benar di-{@code UPDATE}.
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

	/** Nama jenis aset/barang/jasa yang tampil di combo dan laporan. */
	private String nama;
	/** Keterangan bebas, opsional. */
	private String keterangan;
	/** Label klasifikasi siklus pakai; lihat catatan kelas untuk daftar nilai yang dipakai UI. */
	private String tipe;

	/**
	 * Konstruktor default tanpa argumen, dipakai Hibernate untuk instansiasi entity via
	 * refleksi dan oleh kode aplikasi saat membuat baris baru sebelum diisi setter.
	 */
	public JenisAsset() {
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
	 * Mengisi primary key. Karena kolom database bersifat {@code insertable = false}
	 * (IDENTITY, auto-generate oleh database), pengisian manual di sisi Java tidak berpengaruh
	 * pada {@code INSERT}.
	 *
	 * @param id primary key
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama jenis aset, di-trim untuk menghindari whitespace tak sengaja dari input.
	 *
	 * @return nama ter-trim, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama jenis aset. Tidak melakukan trim di sisi setter — trimming terjadi hanya
	 * saat dibaca lewat {@link #getNama()}.
	 *
	 * @param nama nama jenis aset
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
	 * Mengembalikan label klasifikasi siklus pakai. Tidak dianotasi {@code @Column} — kolom
	 * dipetakan lewat konvensi nama Hibernate default, bukan pemetaan eksplisit. String kosong
	 * atau blank dinormalisasi menjadi {@code null} agar konsisten dengan pilihan "Tidak
	 * Ditentukan" di combobox {@code JenisAssetAction}.
	 *
	 * @return salah satu konstanta {@code MasterAsset.TIPE_*}, atau {@code null} bila tidak
	 *         ditentukan atau berisi blank
	 */
	public String getTipe() {
		return tipe == null || tipe.trim().isEmpty() ? null : tipe;
	}

	/**
	 * Mengisi label klasifikasi siklus pakai. Tidak memvalidasi terhadap daftar konstanta
	 * {@code MasterAsset.TIPE_*} — pemanggil di luar {@code JenisAssetAction} bisa mengisi nilai
	 * bebas apa pun tanpa ditolak entity ini.
	 *
	 * @param tipe label klasifikasi; sebaiknya salah satu konstanta {@code MasterAsset.TIPE_*}
	 */
	public void setTipe(String tipe) {
		this.tipe = tipe;
	}
}
