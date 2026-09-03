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
import ais.database.model.akunting.Akun;

/**
 * Entity master <b>Cara Pengadaan Aset</b> (tabel {@code asset.cara_pengadaan_asset}) —
 * katalog {@code nama/keterangan} yang memasangkan sebuah metode pengadaan (mis. "Pembelian
 * Langsung", "Hibah", "Tender") dengan dua akun buku besar opsional: {@link #getAkun()} (akun
 * utama) dan {@link #getAkunTransaksi()} (akun transaksi, boleh kosong dan dimaksudkan
 * fallback ke akun utama saat penjurnalan — lihat komentar pemeliharaan di
 * {@code ais.action.master.asset.CaraPengadaanAssetAction#onSave}).
 *
 * <h2>Status pemakaian: master hidup, hilir tidak ditemukan</h2>
 * <p>Layar CRUD-nya, {@code ais.action.master.asset.CaraPengadaanAssetAction}, sepenuhnya
 * fungsional — tambah/ubah/hapus/cari/cetak. Namun penelusuran seluruh paket
 * {@code ais.database.model.asset} tidak menemukan satu pun entity (termasuk {@link MasterAsset}
 * dan {@code PemesananPengadaanMasterAsset}) yang punya field bertipe {@code CaraPengadaanAsset}
 * atau yang menunjuknya lewat foreign key. Referensi kelas ini di luar dirinya sendiri hanya
 * ada di {@code hibernate.cfg.xml} (registrasi mapping) dan {@code InitData} (daftar
 * pemanasan cache entity, bukan pemakaian data). Komentar di controller CRUD menyebut niat
 * desain "penjurnalan otomatis pengadaan aset sesuai metode pengadaannya", tapi mesin posting
 * mana pun yang membaca baris {@code CaraPengadaanAsset} untuk benar-benar menjurnal belum
 * ditemukan di kode saat ini — pola yang sama seperti master "hidup dengan hilir mati"
 * ({@code AkunPajak}). Perubahan pada {@link #getAkun()}/{@link #getAkunTransaksi()} karenanya
 * tidak menggerakkan jurnal apa pun sampai ada konsumen yang membacanya.</p>
 *
 * <h2>Field audit bayangan {@code oleh}/{@code olehId}/{@code tanggal_dirubah}</h2>
 * <p>Tidak diisi langsung oleh kode aplikasi. Hook {@link javax.persistence.PreUpdate}
 * {@link #onUpdate()} mendelegasikan ke
 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang menuliskan
 * pengguna aktif dan waktu perubahan pada setiap {@code UPDATE}. Ini keharusan teknis siklus
 * hidup Hibernate, bukan kode mati.</p>
 *
 * @see MasterAsset
 * @see ais.database.model.akunting.Akun
 * @see ais.action.master.asset.CaraPengadaanAssetAction
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "cara_pengadaan_asset")



public class CaraPengadaanAsset extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}; tidak pernah
	 * perlu diubah kecuali bentuk field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key auto-generated (IDENTITY) tabel {@code asset.cara_pengadaan_asset}. */
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
	 * Representasi ringkas untuk log/debug dan tampilan combobox generik: hanya {@link #nama}
	 * tanpa awalan {@code id}.
	 *
	 * @return {@link #nama}, bisa {@code null} bila belum diisi
	 */
	public String toString() {
		return nama;
	}

	/** Nama metode pengadaan (mis. "Pembelian Langsung", "Hibah", "Tender"). */
	private String nama;
	/** Keterangan bebas, opsional. */
	private String keterangan;
	/** Akun buku besar utama yang dipasangkan dengan metode pengadaan ini. */
	private Akun akun;
	/** Akun buku besar transaksi opsional; dimaksudkan fallback ke {@link #akun} bila kosong. */
	private Akun akunTransaksi;

	/**
	 * Konstruktor default tanpa argumen, dipakai Hibernate untuk instansiasi entity via
	 * refleksi dan oleh kode aplikasi saat membuat baris baru sebelum diisi setter.
	 */
	public CaraPengadaanAsset() {
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
	 * Mengembalikan nama metode pengadaan, di-trim untuk menghindari whitespace tak sengaja
	 * dari input.
	 *
	 * @return nama ter-trim, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama metode pengadaan. Tidak melakukan trim di sisi setter — trimming terjadi
	 * hanya saat dibaca lewat {@link #getNama()}.
	 *
	 * @param nama nama metode pengadaan
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
	 * @param akun akun utama; wajib diisi sebelum simpan menurut validasi di
	 *             {@code CaraPengadaanAssetAction#onSave}, walau kolom database sendiri
	 *             {@code nullable}
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Mengembalikan akun buku besar transaksi opsional, meresolusi proxy lazy Hibernate lewat
	 * {@link GeneralValueObject#check(Object)} sebelum dikembalikan.
	 *
	 * @return akun transaksi yang sudah teresolusi, atau {@code null} bila sengaja dikosongkan
	 *         (dimaksudkan fallback ke {@link #getAkun()} pada saat penjurnalan)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_transaksi", nullable = true)
	public Akun getAkunTransaksi() {
		akunTransaksi = check(akunTransaksi);
		return akunTransaksi;
	}

	/**
	 * Mengisi akun buku besar transaksi opsional.
	 *
	 * @param akunTransaksi akun transaksi, boleh {@code null}
	 */
	public void setAkunTransaksi(Akun akunTransaksi) {
		this.akunTransaksi = akunTransaksi;
	}

}
