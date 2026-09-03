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

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.envers.Audited;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.Akun;
import ais.database.model.rab.SatuanKerja;

/**
 * Entity master <b>Jenis Penerimaan Barang</b> (tabel {@code asset.jenis_penerimaan_barang}) —
 * katalog {@code kode/nama/keterangan/aktif} berisi jenis penerimaan barang/jasa dalam alur
 * pengadaan aset, masing-masing dipasangkan dengan akun buku besar dan opsional
 * {@link SatuanKerja}. Dipakai live oleh {@code PenerimaanPengadaanMasterAsset} (dipilih lewat
 * combobox di {@code ais.action.master.asset.PenerimaanPengadaanMasterAssetAction}) dan dibaca
 * luas oleh mesin posting pengadaan/pembayaran/transfer untuk menentukan akun hutang penyedia.
 *
 * <h2>{@link #getAkunHutangPenyedia()}: akun hutang yang dibaca banyak mesin posting</h2>
 * <p>Field ini bukan sekadar metadata tampilan — dibaca berulang kali oleh
 * {@code PostingPengadaanAction}, {@code PostingPembayaranAction}, dan
 * {@code ais.database.model.akunting.DaftarPengajuanTransfer} untuk menentukan akun hutang
 * (payable) kepada penyedia/vendor saat menjurnal penerimaan barang. Mengubah akun ini pada
 * baris yang sudah dipakai transaksi lama mengubah akun yang dipakai baris jurnal berikutnya
 * yang membaca ulang relasi ini, tapi tidak menulis ulang jurnal yang sudah terlanjur diposting.</p>
 *
 * <h2>Cache statis {@link #DEFAULT_PENERIMAAN_BARANG_JASA}: dipakai sebagai fallback lazy-default</h2>
 * <p>Sama seperti {@code JenisPajakPpn.PPN}, field statis ini aktif dibaca — oleh
 * {@code PenerimaanPengadaanMasterAsset} sebagai nilai fallback saat baris lama belum punya
 * jenis penerimaan eksplisit ("dokumen lama yang..." — lihat komentar getter lazy di entity
 * itu). Diisi ulang sekali saat startup oleh {@link #reloadDefault()} lewat
 * {@code InitData#reloadDefaults()}, dan diisi ulang lagi setelah setiap simpan berhasil di
 * {@code JenisPenerimaanBarangAction} agar cache tidak kedaluwarsa terhadap perubahan CRUD.</p>
 *
 * <h2>Field audit bayangan {@code oleh}/{@code olehId}/{@code tanggal_dirubah}</h2>
 * <p>Tidak diisi langsung oleh kode aplikasi. Hook {@link javax.persistence.PreUpdate}
 * {@link #onUpdate()} mendelegasikan ke
 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang menuliskan
 * pengguna aktif dan waktu perubahan pada setiap {@code UPDATE}. Ini keharusan teknis siklus
 * hidup Hibernate, bukan kode mati.</p>
 *
 * @see ais.database.model.asset.PenerimaanPengadaanMasterAsset
 * @see ais.action.master.asset.JenisPenerimaanBarangAction
 * @see ais.action.master.asset.PostingPengadaanAction
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "jenis_penerimaan_barang")
public class JenisPenerimaanBarang extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}; tidak pernah
	 * perlu diubah kecuali bentuk field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key auto-generated (IDENTITY) tabel {@code asset.jenis_penerimaan_barang}. */
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

	/**
	 * Cache statis in-memory berisi baris jenis penerimaan pertama (urut {@code id} asc), dipakai
	 * sebagai fallback lazy-default oleh {@code PenerimaanPengadaanMasterAsset} untuk dokumen
	 * lama yang belum punya jenis penerimaan eksplisit. Diisi ulang saat startup dan setelah
	 * setiap simpan CRUD berhasil — lihat catatan kelas.
	 */
	public static JenisPenerimaanBarang DEFAULT_PENERIMAAN_BARANG_JASA = null;

	/**
	 * Memastikan tabel {@code asset.jenis_penerimaan_barang} punya minimal satu baris seed
	 * ("Penerimaan Barang/Jasa", kode "001", aktif) dan mengisi
	 * {@link #DEFAULT_PENERIMAAN_BARANG_JASA} dengan baris pertama (urut {@code id} asc) yang
	 * ditemukan atau baris seed yang baru dibuat.
	 *
	 * <p><b>Cara kerja:</b> membuka sesi Hibernate native lewat
	 * {@link ais.database.hibernate.HibernateUtil#currentNativeSession()}, mengambil satu baris
	 * pertama; bila tabel kosong, membuat dan menyimpan baris default dalam transaksi eksplisit
	 * ({@code begin()}/{@code commit()} manual), lalu menutup sesi.</p>
	 *
	 * <p><b>Kapan dipanggil:</b> sekali secara asinkron saat startup aplikasi dari
	 * {@code ais.common.InitData#reloadDefaults()}, dan lagi setelah setiap simpan berhasil di
	 * {@code ais.action.master.asset.JenisPenerimaanBarangAction} agar cache
	 * {@link #DEFAULT_PENERIMAAN_BARANG_JASA} tetap sinkron dengan perubahan CRUD terbaru —
	 * berbeda dari beberapa saudara paket ini yang hanya di-reload sekali di startup.</p>
	 *
	 * <p><b>Tidak ada penanganan exception</b> — kegagalan Hibernate menyebar sebagai unchecked
	 * exception ke pemanggil.</p>
	 */
	public static void reloadDefault() {
		Session session = HibernateUtil.currentNativeSession();
		DEFAULT_PENERIMAAN_BARANG_JASA = (JenisPenerimaanBarang) session.createCriteria(JenisPenerimaanBarang.class)
				.setMaxResults(1).addOrder(Order.asc("id")).uniqueResult();
		if (DEFAULT_PENERIMAAN_BARANG_JASA == null) {
			DEFAULT_PENERIMAAN_BARANG_JASA = new JenisPenerimaanBarang();
			DEFAULT_PENERIMAAN_BARANG_JASA.setKode("001");
			DEFAULT_PENERIMAAN_BARANG_JASA.setAktif(true);
			DEFAULT_PENERIMAAN_BARANG_JASA.setNama("Penerimaan Barang/Jasa");
			DEFAULT_PENERIMAAN_BARANG_JASA.setKeterangan("Penerimaan Barang/Jasa");
			session.getTransaction().begin();
			session.save(DEFAULT_PENERIMAAN_BARANG_JASA);
			session.getTransaction().commit();
		}
		HibernateUtil.closeSession();
	}

	/** Kode singkat jenis penerimaan. */
	private String kode;
	/** Akun buku besar utama yang dipasangkan dengan jenis penerimaan ini. */
	private Akun akun;
	/** Akun buku besar hutang penyedia/vendor; dibaca luas oleh mesin posting, lihat catatan kelas. */
	private Akun akunHutangPenyedia;
	/** Nama jenis penerimaan barang/jasa. */
	private String nama;
	/** Keterangan bebas, opsional. */
	private String keterangan;
	/** Satuan kerja terkait, opsional. */
	private SatuanKerja satuanKerja;
	/** Flag aktif; {@code null} ditafsirkan sebagai aktif oleh {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Konstruktor default tanpa argumen, dipakai Hibernate untuk instansiasi entity via
	 * refleksi dan oleh kode aplikasi saat membuat baris baru sebelum diisi setter.
	 */
	public JenisPenerimaanBarang() {
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
	 * Mengembalikan kode singkat jenis penerimaan. {@code null} dinormalisasi menjadi string
	 * kosong agar aman dipakai langsung di tampilan tanpa null-check tambahan.
	 *
	 * @return kode ter-trim, tidak pernah {@code null} (string kosong bila belum diisi)
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Mengisi kode singkat jenis penerimaan. Tidak dianotasi {@code @Column} — kolom dipetakan
	 * lewat konvensi nama Hibernate default.
	 *
	 * @param kode kode jenis penerimaan
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama jenis penerimaan, di-trim untuk menghindari whitespace tak sengaja
	 * dari input.
	 *
	 * @return nama ter-trim, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama jenis penerimaan. Tidak melakukan trim di sisi setter — trimming terjadi
	 * hanya saat dibaca lewat {@link #getNama()}.
	 *
	 * @param nama nama jenis penerimaan
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
	 * Mengembalikan status aktif jenis penerimaan. {@code null} database ditafsirkan sebagai
	 * aktif, sehingga baris lama yang belum pernah disentuh field ini akan selalu tampil aktif.
	 * Juga dipakai sebagai kriteria filter di combobox pencarian
	 * {@code PenerimaanPengadaanMasterAssetAction}.
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
	 * Mengembalikan akun buku besar utama, meresolusi proxy lazy Hibernate lewat
	 * {@link GeneralValueObject#check(Object)} sebelum dikembalikan agar pemanggil tidak
	 * menerima proxy yang bisa meledak di luar sesi Hibernate.
	 *
	 * @return akun utama yang sudah teresolusi, atau {@code null} bila belum dipasangkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/**
	 * Mengisi akun buku besar utama.
	 *
	 * @param akun akun utama, boleh {@code null}
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Mengembalikan satuan kerja terkait, meresolusi proxy lazy Hibernate lewat
	 * {@link GeneralValueObject#check(Object)} sebelum dikembalikan.
	 *
	 * @return satuan kerja yang sudah teresolusi, atau {@code null} bila tidak dibatasi satuan
	 *         kerja tertentu
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Mengisi satuan kerja terkait.
	 *
	 * @param satuanKerja satuan kerja, boleh {@code null}
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Mengembalikan akun buku besar hutang penyedia/vendor, meresolusi proxy lazy Hibernate
	 * lewat {@link GeneralValueObject#check(Object)} sebelum dikembalikan. Dibaca berulang kali
	 * oleh mesin posting pengadaan, pembayaran, dan pengajuan transfer — lihat catatan kelas.
	 *
	 * @return akun hutang penyedia yang sudah teresolusi, atau {@code null} bila belum
	 *         dipasangkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_hutang_penyedia", nullable = true)
	public Akun getAkunHutangPenyedia() {
		akunHutangPenyedia = check(akunHutangPenyedia);
		return akunHutangPenyedia;
	}

	/**
	 * Mengisi akun buku besar hutang penyedia/vendor.
	 *
	 * @param akunHutangPenyedia akun hutang penyedia, boleh {@code null}
	 */
	public void setAkunHutangPenyedia(Akun akunHutangPenyedia) {
		this.akunHutangPenyedia = akunHutangPenyedia;
	}

}
