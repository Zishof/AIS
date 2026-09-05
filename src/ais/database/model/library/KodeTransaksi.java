package ais.database.model.library;

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
 * Entity <b>kode jenis transaksi persediaan</b> perpustakaan (tabel
 * {@code library.kode_transaksi}). Satu baris memberi nama dan <b>arah</b> pada satu jenis
 * pergerakan stok koleksi &mdash; misalnya saldo awal, pembelian masuk, hibah masuk, retur beli,
 * pinjam keluar, pengembalian masuk, hilang, transfer, atau penyesuaian.
 *
 * <h3>Arah mutasi ditentukan oleh {@link #getJenis() jenis}</h3>
 * <p>Dua konstanta pada kelas ini menyatakan arah pergerakan: {@link #PENAMBAHAN} bernilai
 * {@code +1} dan {@link #PENGURANGAN} bernilai {@code -1}. Nilainya bukan sekadar penanda
 * kategori melainkan <b>faktor pengali</b>: {@code helper/KoreksiItemDetailAction} menghitung
 * delta koreksi sebagai {@code Math.abs(masukanPengguna) &times; kodeTransaksi.getJenis()},
 * sehingga pengguna selalu mengetik angka positif dan tandanya diambil dari baris kode transaksi
 * yang dipilih. Hasil bertanda itulah yang disimpan ke {@link KoreksiItemDetail#getJumlah()} dan
 * diteruskan sebagai {@code qty} mutasi stok.</p>
 *
 * <p><b>Barisnya di-<i>seed</i> otomatis, bukan diisi pengguna.</b>
 * {@code ais.action.master.library.util.LibraryUtil} menyimpan sekumpulan konstanta statis
 * ({@code SALDO_AWAL}, {@code BELI_MASUK}, {@code HIBAH_MASUK}, {@code MASUK_LAIN},
 * {@code RETUR_BELI}, {@code PINJAM_KELUAR}, {@code PENGEMBALIAN_MASUK}, {@code HILANG},
 * {@code KELUAR_LAIN}, {@code PEMAKAIAN}, {@code TRANSFER}, {@code TERIMA},
 * {@code adjustmentPenambahan}, {@code adjustmentPengurangan}) yang dibuat dan disetel jenisnya
 * saat inisialisasi. Karena itu tabel ini pada praktiknya adalah daftar tertutup; menambah baris
 * secara manual tidak akan membuatnya muncul di layar mana pun, sebab layar koreksi hanya
 * menawarkan dua konstanta penyesuaian tersebut.</p>
 *
 * <p><b>Nilai baku {@code jenis} berarah menambah &mdash; perhatikan ini.</b>
 * {@link #getJenis()} mengembalikan {@code 1} ketika kolomnya {@code NULL}. Kode transaksi yang
 * datanya belum lengkap karenanya diperlakukan sebagai <em>penambahan</em> stok, bukan ditolak
 * atau dianggap netral. Untuk sebuah faktor pengali arah mutasi, ini kegagalan yang membuka ke
 * sisi yang salah: baris rusak menambah persediaan alih-alih menghentikan proses. Lihat catatan
 * lengkapnya pada {@link #getJenis()}.</p>
 *
 * <p><b>Jejak audit.</b> Kelas ditandai {@link Audited}. Trio field audit ringan
 * {@code oleh}/{@code olehId}/{@code tanggal_dirubah} beserta callback {@link #onUpdate()}
 * merupakan keharusan teknis mekanisme audit AIS. Perhatikan bahwa entity ini tidak memiliki
 * relasi ke {@link Perpustakaan}: kode transaksi berlaku lintas tenant dalam satu instalasi.</p>
 *
 * <p><b>Efek samping.</b> Accessor dan mutator hanya membaca atau mengubah state di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab
 * DAO/service dengan session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see KoreksiItemDetail
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "library", name = "kode_transaksi")
public class KodeTransaksi extends GeneralValueObject {

	/**
	 * Arah mutasi <b>menambah</b> stok, bernilai {@code +1}. Dipakai sebagai faktor pengali
	 * sehingga kuantitas yang dimasukkan pengguna tetap positif setelah dikalikan.
	 */
	public static final Integer PENAMBAHAN = 1;
	/**
	 * Arah mutasi <b>mengurangi</b> stok, bernilai {@code -1}. Dipakai sebagai faktor pengali
	 * sehingga kuantitas positif yang dimasukkan pengguna berubah menjadi delta negatif.
	 */
	public static final Integer PENGURANGAN = -1;

	/**
	 * Versi serialisasi Java. Nilainya sengaja disamakan di seluruh entity modul
	 * {@code library} karena kelas-kelas ini dibangkitkan dari template yang sama; jangan
	 * diubah agar sesi ZK/HTTP yang sudah terserialisasi tetap dapat dibaca.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama (surrogate key) baris ini, dibangkitkan oleh database. */
	private Long id;
	/** Nama pengguna aplikasi yang terakhir mengubah baris ini (jejak audit ringan). */
	private String oleh;
	/** ID pengguna aplikasi yang terakhir mengubah baris ini (jejak audit ringan). */
	private String olehId;

	/**
	 * Mengembalikan ID pengguna aplikasi yang terakhir mengubah baris ini.
	 *
	 * @return ID pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna pengubah terakhir. Bersifat <b>no-op bila nilai baru kosong atau
	 * hanya berisi spasi</b> agar jejak audit lama tidak tertimpa oleh pemanggil tanpa konteks
	 * pengguna.
	 *
	 * @param olehId ID pengguna baru; diabaikan bila {@code null}/blank.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir; no-op bila nilai baru kosong/blank.
	 *
	 * @param oleh nama pengguna baru; diabaikan bila {@code null}/blank.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna aplikasi yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}. Dipanggil Hibernate tepat sebelum {@code UPDATE},
	 * lalu mendelegasikan pengisian trio field audit kepada
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}
	/**
	 * Cap waktu perubahan terakhir. Diinisialisasi ke waktu server saat objek dibuat dan
	 * diperbarui oleh {@link #onUpdate()} pada setiap {@code UPDATE}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel cap waktu perubahan terakhir secara manual.
	 *
	 * @param tanggal_dirubah cap waktu baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir baris ini.
	 *
	 * @return cap waktu perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks kode transaksi untuk combobox, listbox, dan log.
	 *
	 * <p>Mengembalikan {@link #nama} apa adanya lewat field langsung &mdash; <b>bukan</b> lewat
	 * {@link #getNama()}, sehingga spasi di ujung tidak dipangkas dan hasilnya dapat berupa
	 * {@code null} murni (bukan string {@code "null"}) untuk baris yang namanya belum diisi.
	 * Komponen ZK yang menampilkan daftar harus siap menerima {@code null}.</p>
	 *
	 * <p>Arah mutasi tidak ikut tampil, sehingga dua kode dengan nama mirip namun berlawanan
	 * arah hanya dapat dibedakan dari teks namanya sendiri.</p>
	 *
	 * @return nama kode transaksi, atau {@code null} bila belum diisi.
	 */
	public String toString() {
		return nama;
	}

	/** Kode singkat transaksi; tidak dipetakan eksplisit dan tidak dijamin unik. */
	private String kode;
	/** Nama kode transaksi yang tampil di layar; wajib terisi. */
	private String nama;
	/** Catatan bebas mengenai kode transaksi ini. */
	private String keterangan;
	/** Arah mutasi: {@link #PENAMBAHAN} ({@code +1}) atau {@link #PENGURANGAN} ({@code -1}). */
	private Integer jenis;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate/ZK untuk instansiasi via refleksi.
	 * Seluruh field bernilai {@code null}; {@code LibraryUtil} mengisi
	 * {@link #setNama(String)} dan {@link #setJenis(Integer)} saat men-<i>seed</i> konstanta
	 * bawaannya.
	 */
	public KodeTransaksi() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * @return ID baris, atau {@code null} bila objek belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris ini. Umumnya hanya dipanggil Hibernate setelah {@code INSERT}.
	 *
	 * @param id ID baris baru.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama kode transaksi, dengan spasi di ujung <b>dipangkas</b>.
	 *
	 * <p>Pemangkasan dilakukan setiap kali getter dipanggil dan tidak ditulis balik ke field,
	 * sehingga nilai yang tersimpan di basis data tetap membawa spasi aslinya. Kolom bersifat
	 * {@code NOT NULL} dengan panjang {@code 255}; getter tetap mengembalikan {@code null} bila
	 * field belum diisi pada objek yang belum tersimpan.</p>
	 *
	 * <p>Nama inilah yang dipakai {@code KoreksiItemAction} untuk menyusun keterangan mutasi
	 * ({@code "Transaksi " + kodeTransaksi.getNama()}), sehingga teks yang dipilih di sini
	 * muncul pada riwayat persediaan.</p>
	 *
	 * @return nama kode transaksi tanpa spasi di ujung, atau {@code null} bila belum diisi.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama kode transaksi.
	 *
	 * <p>Setter tidak memangkas spasi; pemangkasan hanya terjadi pada {@link #getNama()}.
	 * Akibatnya dua baris yang namanya hanya berbeda spasi ujung akan terlihat sama di layar
	 * namun berbeda di basis data.</p>
	 *
	 * @param nama nama kode transaksi.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan catatan bebas mengenai kode transaksi ini.
	 *
	 * @return keterangan bebas, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel catatan bebas mengenai kode transaksi ini.
	 *
	 * @param keterangan teks keterangan baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan arah mutasi kode transaksi ini, dengan <b>nilai baku {@link #PENAMBAHAN}</b>
	 * bila kolomnya belum diisi.
	 *
	 * <p>Nilai kembalian dipakai sebagai faktor pengali, bukan sebagai label: layar koreksi
	 * menghitung {@code Math.abs(masukanPengguna) &times; getJenis()} untuk memperoleh delta
	 * bertanda pada {@link KoreksiItemDetail#getJumlah()}. Nilai yang sah hanyalah
	 * {@link #PENAMBAHAN} ({@code +1}) dan {@link #PENGURANGAN} ({@code -1}); nilai lain akan
	 * ikut mengalikan besaran kuantitas, bukan sekadar membalik tandanya.</p>
	 *
	 * <p><b>Nilai baku ini gagal ke arah yang salah.</b> Ketika kolomnya {@code NULL} &mdash;
	 * misalnya baris yang disisipkan langsung ke basis data, atau kode transaksi baru yang
	 * jenisnya belum sempat disetel &mdash; getter mengembalikan {@code 1}, sehingga transaksi
	 * diperlakukan sebagai <em>penambahan</em> stok. Untuk sebuah faktor arah mutasi, pilihan
	 * yang aman semestinya menolak baris tersebut atau memperlakukannya sebagai netral
	 * ({@code 0}); memilih {@code +1} berarti data yang rusak menaikkan persediaan secara diam.
	 * Normalisasi juga tidak ditulis balik ke field, sehingga kolomnya tetap {@code NULL} di
	 * basis data dan perilaku ini terus berulang setiap kali baris dibaca.</p>
	 *
	 * <p>Perlu dicatat bahwa {@code LibraryUtil} selalu memanggil {@link #setJenis(Integer)}
	 * secara eksplisit saat men-<i>seed</i> keempat belas konstanta bawaannya, sehingga pada
	 * instalasi yang sehat cabang baku ini tidak pernah terpakai.</p>
	 *
	 * @return arah mutasi; {@code +1} bila belum diisi.
	 */
	public Integer getJenis() {
		return jenis == null ? 1 : jenis;
	}

	/**
	 * Menyetel arah mutasi kode transaksi ini.
	 *
	 * <p><b>Catatan integritas:</b> setter tidak membatasi nilai pada {@link #PENAMBAHAN} atau
	 * {@link #PENGURANGAN}. Menyetel angka lain &mdash; misalnya {@code 2} &mdash; akan
	 * menggandakan kuantitas yang dimasukkan pengguna pada perhitungan delta koreksi, bukan
	 * sekadar menentukan arahnya.</p>
	 *
	 * @param jenis arah mutasi; gunakan konstanta {@link #PENAMBAHAN} atau {@link #PENGURANGAN}.
	 */
	public void setJenis(Integer jenis) {
		this.jenis = jenis;
	}

	/**
	 * Mengembalikan kode singkat transaksi.
	 *
	 * <p>Tidak dianotasi {@link Column} sehingga dipetakan ke kolom bernama {@code kode} dengan
	 * pengaturan bawaan: <i>nullable</i> dan <b>tanpa jaminan keunikan</b>. Berbeda dari kode
	 * dokumen pada entity transaksi seperti {@link KoreksiItem#getKode()} yang wajib unik, nilai
	 * di sini murni label opsional &mdash; {@code LibraryUtil} bahkan tidak mengisinya saat
	 * men-<i>seed</i> konstanta bawaan, dan tidak ada satu pun jalur perhitungan yang membacanya.
	 * Gunakan {@link #getNama()} sebagai identitas yang tampil kepada pengguna.</p>
	 *
	 * @return kode singkat transaksi, atau {@code null} bila belum diisi.
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Menyetel kode singkat transaksi.
	 *
	 * @param kode kode singkat transaksi.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

}
