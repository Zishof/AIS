package ais.database.model.library;

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

import ais.common.Common;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;

/**
 * Entity <b>tarif denda keterlambatan</b> pengembalian koleksi perpustakaan (tabel
 * {@code library.denda_keterlambatan_item}). Satu baris menyatakan satu anak tangga tarif yang
 * berlaku bagi sekelompok anggota mulai keterlambatan sekian hari.
 *
 * <h3>Tabel bertingkat, bukan tarif tunggal</h3>
 * <p>Beberapa baris dapat berlaku bagi kelompok anggota yang sama dengan
 * {@link #getJumlahHari() jumlahHari} yang berbeda-beda; nilai itu adalah <b>ambang bawah</b>
 * keterlambatan. {@code LibraryUtil.hitungDendaItem(...)} memilih baris yang
 * {@code jumlah_hari}-nya paling besar namun masih kurang dari atau sama dengan
 * {@link PeminjamanPengadaanItemDetail#getJumlahHariTerlambat() jumlah hari terlambat}, lalu
 * memakai {@link #getDenda() denda}-nya. Dengan begitu perpustakaan dapat menyusun tarif
 * berjenjang &mdash; misalnya baris {@code jumlahHari = 0} untuk tarif dasar dan baris
 * {@code jumlahHari = 30} untuk tarif yang lebih berat setelah sebulan.</p>
 *
 * <h3>Tarif ini benar-benar ditegakkan &mdash; tetapi hanya separuh tabelnya</h3>
 * <p>Isi tabel ini bukan sekadar catatan deskriptif: {@code LibraryUtil.hitungDendaItem(...)}
 * dipanggil dari layar pengembalian ({@code helper/KembaliPengadaanItemDetailAction},
 * {@code helper/KembaliPengadaanItemPunyaItemHelper}), dari {@code KembaliPengadaanItemAction},
 * dan dari {@code PerpustakaanResource}; hasilnya dikalikan kuantitas baris peminjaman lalu
 * <b>disimpan</b> ke {@link KembaliPengadaanItemDetail#getDenda()}.</p>
 * <p><b>Namun hanya baris dengan {@link #getDendaPerItem() dendaPerItem} bernilai benar yang
 * pernah dibaca.</b> Kriteria pencarian pada {@code hitungDendaItem(...)} menyaring
 * {@code dendaPerItem = true} secara mutlak. Cabang sebaliknya &mdash; denda per transaksi,
 * bukan per eksemplar &mdash; dilayani {@code LibraryUtil.hitungDenda(...)} yang menyaring
 * {@code dendaPerItem = false}, tetapi method itu <b>sudah tidak punya pemanggil hidup</b>: dua
 * satu-satunya rujukannya berada di dalam blok komentar pada {@link KembaliPengadaanItem} dan
 * {@link PeminjamanPengadaanItem}. Akibatnya baris yang {@code dendaPerItem}-nya salah adalah
 * data tidur &mdash; tetap dapat dibuat dan disunting lewat
 * {@code DendaKeterlambatanItemAction}, tampil di daftar, ikut terekam Envers, namun tidak
 * pernah memengaruhi denda siapa pun. Petugas yang mengisi tarif tanpa mencentang "denda per
 * item" akan mendapati dendanya nol tanpa penjelasan.</p>
 *
 * <h3>Cara satu baris dipilih</h3>
 * <p>Sama seperti {@link BatasWaktuPeminjamanItem}, pencocokan memakai empat dimensi profil
 * anggota &mdash; {@link #getJenisAnggota()}, {@link #getTipeAnggota()}, {@link #getFakultas()},
 * {@link #getJurusan()} &mdash; yang seluruhnya <b>nullable sebagai wildcard</b>: kriterianya
 * {@code (kolom = nilaiAnggota ATAU kolom IS NULL)}. {@link #getPerpustakaan() perpustakaan}
 * dicocokkan dengan {@code =} sehingga bukan wildcard, dan {@link #getMulaiBerlaku()
 * mulaiBerlaku} disaring {@code <= tanggal transaksi}. Hasil diurutkan menurun pada
 * {@code jumlahHari} lalu {@code mulaiBerlaku}, dan hanya satu baris teratas yang dipakai.</p>
 *
 * <p><b>Tidak ada baris yang cocok berarti tidak ada denda &mdash; dan berpotensi galat.</b>
 * {@code hitungDendaItem(...)} mengembalikan {@code null} bila tak ada yang cocok. Sebagian
 * pemanggil menanganinya ({@code dendaPerItem == null ? 0.0 : dendaPerItem.getDenda()}), tetapi
 * {@code helper/KembaliPengadaanItemDetailAction} kemudian memanggil
 * {@code dendaPerItem.getKeterangan()} tanpa penjagaan pada baris berikutnya, sehingga layar
 * rincian pengembalian melempar {@code NullPointerException} pada instalasi yang belum mengisi
 * tabel ini sama sekali.</p>
 *
 * <p><b>{@link #getDefaultItem() defaultItem} adalah penanda tidur</b>, persis seperti pada
 * {@link BatasWaktuPeminjamanItem}: tidak dibaca kriteria pencarian mana pun dan tidak disediakan
 * kendalinya di layar, meski terinisialisasi {@code true} dan ikut tersimpan.</p>
 *
 * <p><b>Efek samping.</b> Sebagian getter menormalkan {@code null} dan menulis hasilnya balik ke
 * field; lihat catatan pada masing-masing. Persistence, transaksi, otorisasi, dan pemuatan relasi
 * lazy tetap menjadi tanggung jawab DAO/service dengan session aktif; jangan menaruh query
 * duplikat pada model.</p>
 *
 * @see BatasWaktuPeminjamanItem
 * @see KembaliPengadaanItemDetail
 * @see PeminjamanPengadaanItemDetail
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "library", name = "denda_keterlambatan_item")
public class DendaKeterlambatanItem extends GeneralValueObject {

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
	 * Representasi teks tarif untuk combobox, listbox, dan log.
	 *
	 * <p><b>Hanya menampilkan perpustakaan.</b> Seperti pada
	 * {@link BatasWaktuPeminjamanItem#toString()}, seluruh dimensi pembeda &mdash; ambang hari,
	 * besaran denda, jenis/tipe anggota, fakultas, jurusan, tanggal mulai berlaku, dan penanda
	 * denda per item &mdash; tidak ikut tampil, sehingga semua tarif milik satu perpustakaan
	 * terlihat identik di daftar. Membaca lewat field langsung sehingga tidak memicu inisialisasi
	 * proxy.</p>
	 *
	 * @return nama perpustakaan dirangkai dengan {@code ""}; dapat berupa string {@code "null"}.
	 */
	public String toString() {
		return perpustakaan + "";
	}

	/** Tanggal tarif mulai berlaku; disaring {@code <= tanggal transaksi} pada pencarian. */
	private Date mulaiBerlaku = ais.ui.util.WaktuUtil.getDate();
	/** Ambang bawah keterlambatan (hari) agar tarif ini berlaku. */
	private Integer jumlahHari;
	/** Perpustakaan tempat tarif berlaku; dicocokkan dengan {@code =}, bukan wildcard. */
	private Perpustakaan perpustakaan;
	/** Jenis anggota sasaran; {@code null} berarti berlaku untuk semua jenis. */
	private JenisAnggota jenisAnggota;
	/** Tipe anggota sasaran; {@code null} berarti berlaku untuk semua tipe. */
	private TipeAnggota tipeAnggota;
	/** Fakultas sasaran; {@code null} berarti berlaku untuk semua fakultas. */
	private Fakultas fakultas;
	/** Jurusan sasaran; {@code null} berarti berlaku untuk semua jurusan. */
	private Jurusan jurusan;
	/** Besaran denda; dikalikan kuantitas baris peminjaman oleh pemanggil. */
	private Double denda;
	/** Catatan bebas; dipakai layar pengembalian sebagai teks pengganti angka denda. */
	private String keterangan;
	/** Penanda tidur: tersimpan namun tidak pernah dibaca kriteria pencarian mana pun. */
	private Boolean defaultItem = true;
	// private Boolean berulang;
	/** Penanda denda dihitung per eksemplar; hanya baris bernilai benar yang pernah dipakai. */
	private Boolean dendaPerItem;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate/ZK untuk instansiasi via refleksi.
	 * Perhatikan bahwa {@code jumlahHari}, {@code denda}, dan {@code dendaPerItem} tidak
	 * diinisialisasi, sehingga tarif baru yang disimpan tanpa disunting lengkap akan membawa
	 * kolom {@code NULL} yang membuatnya tidak pernah terpilih.
	 */
	public DendaKeterlambatanItem() {
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
	 * Mengembalikan catatan bebas tarif ini, dinormalkan ke string kosong bila belum diisi.
	 *
	 * <p><b>Keterangan di sini bukan sekadar catatan administratif.</b> Layar pengembalian
	 * memakainya sebagai <em>pengganti</em> tampilan angka: bila keterangan tidak kosong, teks
	 * itulah yang ditampilkan pada kolom denda; bila kosong, barulah nominalnya yang tampil.
	 * Selain itu isinya disalin ke {@link KembaliPengadaanItemDetail#getKetDenda()} sebagai
	 * catatan awal riwayat denda. Karena itu mengisi keterangan dengan teks panjang akan
	 * menyembunyikan besaran denda dari petugas di layar.</p>
	 *
	 * <p>Normalisasi tidak ditulis balik ke field, sehingga getter ini tidak mengubah state.</p>
	 *
	 * @return keterangan tarif; tidak pernah {@code null}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan == null ? "" : keterangan;
	}

	/**
	 * Menyetel catatan bebas tarif ini. Lihat catatan pada {@link #getKeterangan()}: teks ini
	 * menggantikan tampilan nominal denda pada layar pengembalian.
	 *
	 * @param keterangan teks keterangan baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan penanda {@code defaultItem}, dinormalkan ke {@code false} bila belum diisi.
	 *
	 * <p><b>Penanda ini tidur.</b> Tidak ada kriteria pencarian tarif di {@code LibraryUtil}
	 * yang menyaring berdasarkan nilai ini, dan {@code DendaKeterlambatanItemAction} tidak
	 * menyediakan kendali untuk mengubahnya. Nilainya terinisialisasi {@code true} pada objek
	 * baru lalu ikut tersimpan, tetapi tidak memengaruhi perilaku apa pun. Pola yang sama
	 * terdapat pada {@link BatasWaktuPeminjamanItem#getDefaultItem()}. Jangan tertukar dengan
	 * {@link #getDendaPerItem() dendaPerItem} yang justru sangat menentukan.</p>
	 *
	 * <p>Normalisasi ditulis balik ke field, sehingga getter ini mengubah state objek.</p>
	 *
	 * @return penanda {@code defaultItem}; tidak pernah {@code null}.
	 */
	public Boolean getDefaultItem() {
		if (defaultItem == null) {
			defaultItem = false;
		}
		return defaultItem;
	}

	/**
	 * Menyetel penanda {@code defaultItem}. Lihat catatan pada {@link #getDefaultItem()}:
	 * nilai ini tersimpan namun tidak memengaruhi perilaku apa pun.
	 *
	 * @param defaultItem penanda baru.
	 */
	public void setDefaultItem(Boolean defaultItem) {
		this.defaultItem = defaultItem;
	}

	/**
	 * Mengembalikan tanggal tarif mulai berlaku.
	 *
	 * <p>Kriteria pencarian menyaring {@code mulaiberlaku <= tanggal transaksi} lalu memakainya
	 * sebagai pengurut kedua (menurun) setelah {@code jumlahHari}. Artinya di antara beberapa
	 * tarif dengan ambang hari yang sama, <b>yang berlaku paling baru menang</b>, dan tarif
	 * bertanggal masa depan dapat dijadwalkan lebih awal tanpa langsung berlaku.</p>
	 *
	 * <p>Dipetakan {@link TemporalType#DATE}. Getter tidak menormalkan {@code null}; baris tanpa
	 * tanggal mulai tidak akan pernah lolos kriteria dan menjadi tarif mati.</p>
	 *
	 * @return tanggal mulai berlaku, atau {@code null} bila belum diisi.
	 */
	@Temporal(TemporalType.DATE)
	public Date getMulaiBerlaku() {
		return mulaiBerlaku;
	}

	/**
	 * Menyetel tanggal tarif mulai berlaku.
	 *
	 * @param mulaiBerlaku tanggal mulai berlaku.
	 */
	public void setMulaiBerlaku(Date mulaiBerlaku) {
		this.mulaiBerlaku = mulaiBerlaku;
	}

	/**
	 * Mengembalikan ambang bawah keterlambatan (dalam hari) agar tarif ini berlaku, dinormalkan
	 * ke {@code 0} bila belum diisi.
	 *
	 * <p>Nilai ini adalah kunci mekanisme tarif berjenjang. Kriteria pencarian memakai batasan
	 * SQL mentah {@code (jumlah_hari) <= <jumlahHariTerlambat>} lalu mengurutkan menurun pada
	 * kolom ini, sehingga di antara semua tarif yang ambangnya sudah terlampaui, yang
	 * <b>ambangnya tertinggi</b>-lah yang dipakai. Baris {@code jumlahHari = 0} berfungsi
	 * sebagai tarif dasar yang berlaku sejak hari keterlambatan pertama.</p>
	 *
	 * <p><b>Normalisasi tidak menyelamatkan baris dengan kolom {@code NULL}.</b> Karena
	 * kriterianya berupa SQL mentah, perbandingan {@code NULL <= n} dalam SQL menghasilkan
	 * {@code NULL} &mdash; bukan benar &mdash; sehingga baris tersebut tidak pernah terpilih
	 * sama sekali. Nilai baku {@code 0} pada getter hanya terlihat setelah entity dimuat penuh
	 * ke formulir, terlambat untuk memengaruhi pemilihan.</p>
	 *
	 * <p>Blok komentar di dalam method adalah sisa mekanisme {@code berulang} yang dihapus;
	 * dulu tarif tak berulang dipaksa berambang nol. Normalisasi ditulis balik ke field,
	 * sehingga getter ini mengubah state objek.</p>
	 *
	 * @return ambang bawah keterlambatan dalam hari; tidak pernah {@code null}.
	 */
	@Column(name = "jumlah_hari", nullable = true)
	public Integer getJumlahHari() {

		// if (!getBerulang()) {
		// jumlahHari = 0;
		// }

		if (jumlahHari == null) {
			jumlahHari = 0;
		}
		return jumlahHari;
	}

	/**
	 * Menyetel ambang bawah keterlambatan (dalam hari) agar tarif ini berlaku.
	 *
	 * @param jumlahHari ambang bawah keterlambatan dalam hari.
	 */
	public void setJumlahHari(Integer jumlahHari) {
		this.jumlahHari = jumlahHari;
	}

	/**
	 * Mengembalikan perpustakaan tempat tarif ini berlaku, dengan <b>pengisian otomatis</b>
	 * dari sesi bila belum diisi.
	 *
	 * <p>Alur getter ini: (1) bila field masih {@code null}, ambil perpustakaan aktif melalui
	 * {@link Common#getCurrentPerpustakaan()}; (2) jalankan {@code check(...)} milik
	 * {@link GeneralValueObject}. Hasilnya ditulis balik ke field, sehingga getter ini mengubah
	 * state objek (getter destruktif ringan).</p>
	 *
	 * <p><b>Perpustakaan bukan wildcard.</b> Kriteria pencarian mencocokkan kolom ini dengan
	 * {@code =}, sehingga tarif yang perpustakaannya kosong tidak akan pernah terpilih dan
	 * setiap perpustakaan wajib memiliki tarifnya sendiri.</p>
	 *
	 * @return perpustakaan tempat tarif berlaku; dapat {@code null} bila sesi juga tidak
	 *         memilikinya.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "perpustakaan", nullable = true)
	public Perpustakaan getPerpustakaan() {
		if (perpustakaan == null) {
			perpustakaan = Common.getCurrentPerpustakaan();
		}
		perpustakaan = check(perpustakaan);
		return perpustakaan;
	}

	/**
	 * Menyetel perpustakaan tempat tarif ini berlaku.
	 *
	 * @param perpustakaan perpustakaan sasaran.
	 */
	public void setPerpustakaan(Perpustakaan perpustakaan) {
		this.perpustakaan = perpustakaan;
	}

	/**
	 * Mengembalikan besaran denda tarif ini.
	 *
	 * <p>Pemanggil mengalikannya dengan {@link PeminjamanPengadaanItemDetail#getJumlah()
	 * kuantitas baris peminjaman}, menambahkan biaya penggantian bila ada, lalu menyimpan
	 * hasilnya ke {@link KembaliPengadaanItemDetail#getDenda()}. Satuannya adalah nominal per
	 * eksemplar untuk satu kejadian keterlambatan &mdash; <b>bukan</b> per hari: tidak ada
	 * pengali hari di jalur perhitungan yang aktif, sehingga kenaikan denda seiring lamanya
	 * keterlambatan harus dinyatakan lewat beberapa baris tarif dengan
	 * {@link #getJumlahHari() ambang} berbeda.</p>
	 *
	 * <p><b>Getter ini tidak menormalkan {@code null}.</b> Berbeda dari hampir seluruh getter
	 * numerik lain di paket ini, kolom yang belum diisi mengembalikan {@code null}. Pemanggil
	 * di {@code LibraryUtil} dan helper pengembalian menjaga <em>baris</em>-nya
	 * ({@code dendaPerItem == null ? 0.0 : dendaPerItem.getDenda()}) tetapi tidak menjaga
	 * <em>nilainya</em>, sehingga tarif yang tersimpan tanpa nominal akan memicu
	 * {@code NullPointerException} saat hasilnya dikalikan kuantitas.</p>
	 *
	 * @return besaran denda per eksemplar, atau {@code null} bila belum diisi.
	 */
	public Double getDenda() {
		return denda;
	}

	/**
	 * Menyetel besaran denda tarif ini.
	 *
	 * <p><b>Catatan integritas:</b> setter tidak menolak nilai negatif maupun {@code null}.
	 * Lihat peringatan {@code null} pada {@link #getDenda()}.</p>
	 *
	 * @param denda besaran denda per eksemplar.
	 */
	public void setDenda(Double denda) {
		this.denda = denda;
	}

	// public Boolean getBerulang() {
	// if (berulang == null) {
	// berulang = false;
	// }
	// return berulang;
	// }
	//
	// public void setBerulang(Boolean berulang) {
	// this.berulang = berulang;
	// }

	/**
	 * Mengembalikan penanda bahwa denda dihitung per eksemplar, dinormalkan ke {@code false}
	 * bila belum diisi.
	 *
	 * <p><b>Inilah saklar yang menentukan apakah sebuah baris tarif hidup atau tidur.</b>
	 * {@code LibraryUtil.hitungDendaItem(...)} &mdash; satu-satunya jalur perhitungan denda yang
	 * masih punya pemanggil hidup &mdash; menyaring {@code dendaPerItem = true} secara mutlak.
	 * Cabang sebaliknya dilayani {@code LibraryUtil.hitungDenda(...)} yang menyaring
	 * {@code dendaPerItem = false}, tetapi method itu sudah tidak dipanggil dari mana pun: dua
	 * satu-satunya rujukannya terkubur di dalam blok komentar pada {@link KembaliPengadaanItem}
	 * dan {@link PeminjamanPengadaanItem}.</p>
	 *
	 * <p>Konsekuensinya, tarif yang disimpan dengan penanda ini bernilai salah &mdash; termasuk
	 * seluruh tarif yang dibuat tanpa mencentang kotaknya di
	 * {@code DendaKeterlambatanItemAction}, karena field ini tidak diinisialisasi &mdash; tidak
	 * akan pernah dipakai. Baris tersebut tetap muncul di daftar, tetap dapat disunting, dan
	 * tetap terekam Envers, namun dendanya tidak pernah dikenakan kepada siapa pun. Perlu
	 * dicatat pula bahwa kriteria memakai {@code Restrictions.eq("dendaPerItem", true)}, sehingga
	 * baris dengan kolom {@code NULL} pun tidak cocok.</p>
	 *
	 * <p>Normalisasi ditulis balik ke field, sehingga getter ini mengubah state objek.</p>
	 *
	 * @return {@code true} bila denda dihitung per eksemplar; tidak pernah {@code null}.
	 */
	public Boolean getDendaPerItem() {
		if (dendaPerItem == null) {
			dendaPerItem = false;
		}
		return dendaPerItem;
	}

	/**
	 * Menyetel penanda bahwa denda dihitung per eksemplar.
	 *
	 * <p>Lihat catatan pada {@link #getDendaPerItem()}: menyetelnya salah membuat baris tarif
	 * ini tidak pernah dipakai oleh jalur perhitungan mana pun yang masih hidup.</p>
	 *
	 * @param dendaPerItem penanda denda per eksemplar.
	 */
	public void setDendaPerItem(Boolean dendaPerItem) {
		this.dendaPerItem = dendaPerItem;
	}

	/**
	 * Mengembalikan jenis anggota sasaran tarif ini.
	 *
	 * <p>Bernilai {@code null} berarti tarif berlaku untuk semua jenis anggota: kriteria
	 * pencarian berbentuk {@code (jenisAnggota = milikAnggota ATAU jenisAnggota IS NULL)}.
	 * Getter menjalankan {@code check(...)} lalu menulis hasilnya balik ke field (getter
	 * destruktif ringan) sehingga aman dibaca meski relasi dipetakan {@link FetchType#LAZY}.</p>
	 *
	 * @return jenis anggota sasaran, atau {@code null} untuk semua jenis.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_anggota", nullable = true)
	public JenisAnggota getJenisAnggota() {
		jenisAnggota = check(jenisAnggota);
		return jenisAnggota;
	}

	/**
	 * Menyetel jenis anggota sasaran tarif ini.
	 *
	 * @param jenisAnggota jenis anggota sasaran; {@code null} berarti semua jenis.
	 */
	public void setJenisAnggota(JenisAnggota jenisAnggota) {
		this.jenisAnggota = jenisAnggota;
	}

	/**
	 * Mengembalikan tipe anggota sasaran tarif ini.
	 *
	 * <p>Bernilai {@code null} berarti tarif berlaku untuk semua tipe anggota, dengan pola
	 * kriteria wildcard yang sama seperti {@link #getJenisAnggota()}. Getter menjalankan
	 * {@code check(...)} dan menulis hasilnya balik ke field.</p>
	 *
	 * @return tipe anggota sasaran, atau {@code null} untuk semua tipe.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tipe_anggota", nullable = true)
	public TipeAnggota getTipeAnggota() {
		tipeAnggota = check(tipeAnggota);
		return tipeAnggota;
	}

	/**
	 * Menyetel tipe anggota sasaran tarif ini.
	 *
	 * @param tipeAnggota tipe anggota sasaran; {@code null} berarti semua tipe.
	 */
	public void setTipeAnggota(TipeAnggota tipeAnggota) {
		this.tipeAnggota = tipeAnggota;
	}

	/**
	 * Mengembalikan fakultas sasaran tarif ini.
	 *
	 * <p>Fakultas anggota tidak disimpan pada {@link Anggota} melainkan diturunkan
	 * {@code LibraryUtil} dari mahasiswa (lewat jurusannya) atau dari dosen yang tertaut.
	 * Bernilai {@code null} berarti tarif berlaku untuk semua fakultas. Getter menjalankan
	 * {@code check(...)} dan menulis hasilnya balik ke field.</p>
	 *
	 * @return fakultas sasaran, atau {@code null} untuk semua fakultas.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * Menyetel fakultas sasaran tarif ini.
	 *
	 * @param fakultas fakultas sasaran; {@code null} berarti semua fakultas.
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Mengembalikan jurusan sasaran tarif ini.
	 *
	 * <p>Seperti {@link #getFakultas()}, jurusan anggota diturunkan {@code LibraryUtil} dari
	 * mahasiswa atau dosen yang tertaut. Bernilai {@code null} berarti tarif berlaku untuk semua
	 * jurusan. Getter menjalankan {@code check(...)} dan menulis hasilnya balik ke field.</p>
	 *
	 * @return jurusan sasaran, atau {@code null} untuk semua jurusan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Menyetel jurusan sasaran tarif ini.
	 *
	 * @param jurusan jurusan sasaran; {@code null} berarti semua jurusan.
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

}
