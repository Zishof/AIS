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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Entity <b>baris rincian</b> dokumen koreksi stok item perpustakaan (tabel
 * {@code library.koreksi_item_detail}). Satu baris menyatakan penyesuaian stok untuk satu
 * judul/koleksi {@link Item} dan menunjuk balik ke header {@link KoreksiItem}. Tipe ini membawa
 * state yang dipertukarkan oleh lapisan persistence, service, dan UI; makna bisnis utamanya
 * ditentukan oleh field serta relasi yang dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap
 * dimiliki {@link GeneralValueObject}. Kelas ini hanya memuat perbedaan yang benar-benar
 * spesifik untuk variasi ini; perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di
 * kelas induk agar fungsi tidak bercabang atau tumpang tindih.</p>
 *
 * <h3>Tiga angka stok dan hubungannya</h3>
 * <p>Baris koreksi menyimpan tiga besaran yang harus dibaca sebagai satu kesatuan, ditambah satu
 * kode transaksi yang menentukan arahnya:</p>
 * <ul>
 *   <li>{@link #getStok() stok} &mdash; <b>foto stok sebelum koreksi</b>. Diisi layar ZK
 *       ({@code helper/KoreksiItemDetailAction}) dari hasil query stok saat item dipilih atau
 *       barcode dipindai.</li>
 *   <li>{@link #getKodeTransaksi() kodeTransaksi} &mdash; jenis penyesuaian, praktis selalu salah
 *       satu dari {@code LibraryUtil.adjustmentPenambahan} atau
 *       {@code LibraryUtil.adjustmentPengurangan}. Yang dipakai darinya adalah
 *       {@link KodeTransaksi#getJenis()}, bernilai {@link KodeTransaksi#PENAMBAHAN} ({@code +1})
 *       atau {@link KodeTransaksi#PENGURANGAN} ({@code -1}).</li>
 *   <li>{@link #getJumlah() jumlah} &mdash; <b>delta bertanda</b>. Layar menghitungnya sebagai
 *       {@code Math.abs(masukanPengguna) * kodeTransaksi.getJenis()}, sehingga pengguna selalu
 *       mengetikkan angka positif dan tandanya ditentukan oleh pilihan kode transaksi. Nilai
 *       inilah yang diteruskan {@code KoreksiItemAction} sebagai {@code qty} ke mutasi stok.</li>
 *   <li>{@link #getStokmenjadi() stokmenjadi} &mdash; <b>foto stok sesudah koreksi</b>, dihitung
 *       layar sebagai {@code stok + jumlah}.</li>
 * </ul>
 *
 * <p><b>Yang benar-benar memengaruhi persediaan hanyalah {@code jumlah}.</b> Kolom {@code stok}
 * dan {@code stokmenjadi} adalah dokumentasi/pembanding: keduanya hanya diisi ketika baris
 * disunting di layar dan <b>tidak pernah dihitung ulang saat dokumen diposting</b>. Bila dokumen
 * koreksi disimpan sebagai draf lalu stok bergerak karena peminjaman, pengembalian, transfer,
 * atau koreksi lain sebelum dokumen ini disetujui, maka {@code stok} dan {@code stokmenjadi} yang
 * tersimpan sudah tidak menggambarkan keadaan sebenarnya &mdash; sementara {@code jumlah} tetap
 * diposting apa adanya. Jejak audit dokumen karenanya bisa menampilkan "dari 10 menjadi 12"
 * padahal stok saat posting adalah 7 dan berubah menjadi 9. Ini bukan kesalahan aritmetika
 * melainkan konsekuensi desain <i>snapshot</i>; laporan rekonsiliasi sebaiknya menghitung ulang
 * dari mutasi, bukan mempercayai kedua kolom foto tersebut.</p>
 *
 * <p><b>Tidak ada penjaga keseimbangan.</b> Model tidak memvalidasi apa pun di antara keempat
 * besaran itu: tidak memeriksa bahwa {@code stokmenjadi == stok + jumlah}, tidak memeriksa bahwa
 * tanda {@code jumlah} sesuai dengan {@code kodeTransaksi.getJenis()}, tidak menolak
 * {@code stokmenjadi} negatif (stok minus), dan tidak menolak {@code kodeTransaksi} yang
 * {@code null}. Konsistensi keempatnya sepenuhnya bergantung pada urutan pemanggilan setter di
 * layar ZK; jalur mana pun yang melewati layar itu &mdash; importer massal, API, atau perubahan
 * langsung lewat DAO &mdash; dapat menuliskan kombinasi yang tidak masuk akal tanpa peringatan.
 * Perlu dicatat pula bahwa {@link KodeTransaksi#getJenis()} <b>mengembalikan {@code +1} bila
 * kolomnya {@code NULL}</b>, sehingga kode transaksi yang datanya belum lengkap akan diperlakukan
 * sebagai penambahan, bukan ditolak.</p>
 *
 * <p><b>Granularitas.</b> Seperti {@link ReturPengadaanItemDetail} dan berbeda dari
 * {@link TransferPengadaanItemDetail}, baris koreksi bekerja pada tingkat judul plus kuantitas
 * dan tidak menyimpan {@link ItemPunyaBarcode}. Eksemplar mana persisnya yang hilang atau
 * ditemukan tidak terekam di sini, meskipun layar mengizinkan pemindaian barcode untuk membantu
 * memilih judul.</p>
 *
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String
 * oleh}, {@code String olehId}, {@code Date tanggal_dirubah}, {@code Item item}, {@code Double
 * jumlah}, {@code KoreksiItem koreksiItem}, {@code String keterangan}, {@code KodeTransaksi
 * kodeTransaksi}, {@code Double stok}, {@code Double stokmenjadi}; pemetaan persistence: tabel
 * {@code library.koreksi_item_detail}; pembacaan/pencarian ({@code getOlehId()}, {@code
 * getOleh()}, {@code getTanggal_dirubah()}, {@code getId()}, {@code getKeterangan()}, {@code
 * getItem()}, {@code getJumlah()}, {@code getStok()}, {@code getStokmenjadi()}, {@code
 * getKodeTransaksi()}); mutasi data ({@code setOlehId()}, {@code setOleh()}, {@code onUpdate()},
 * {@code setTanggal_dirubah()}, {@code setId()}, {@code setKeterangan()}); operasi domain lain
 * ({@code toString()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 *
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value
 * object di memori. Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi
 * tanggung jawab DAO/service dengan session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see KoreksiItem
 * @see KodeTransaksi
 * @see Item
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "library", name = "koreksi_item_detail")

public class KoreksiItemDetail extends GeneralValueObject {

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
	 * Representasi teks baris untuk grid, combobox, dan log.
	 *
	 * <p>Membaca field {@link #item} secara langsung dan merangkainya dengan {@code ""}
	 * sehingga aman terhadap {@code null}. Perhatikan bahwa arah dan besar koreksi
	 * <em>tidak</em> ikut tampil; dua baris penambahan dan pengurangan atas judul yang sama
	 * terlihat identik lewat {@code toString()}.</p>
	 *
	 * @return representasi teks item pada baris ini.
	 */
	public String toString() {
		return item + "";
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

	/** Judul/koleksi yang stoknya dikoreksi pada baris ini. */
	private Item item;
	/** Delta bertanda koreksi ({@code +} menambah, {@code -} mengurangi); inilah qty mutasi stok. */
	private Double jumlah;
	/** Header dokumen koreksi pemilik baris ini. */
	private KoreksiItem koreksiItem;
	/** Catatan bebas pada tingkat baris, umumnya alasan selisih untuk judul ini. */
	private String keterangan;
	/** Jenis penyesuaian; {@link KodeTransaksi#getJenis()}-nya menentukan tanda {@link #jumlah}. */
	private KodeTransaksi kodeTransaksi;
	/** Foto stok sebelum koreksi, direkam saat baris disunting di layar. */
	private Double stok;
	/** Foto stok sesudah koreksi ({@code stok + jumlah}), direkam saat baris disunting di layar. */
	private Double stokmenjadi;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate/ZK untuk instansiasi via refleksi.
	 * Perhatikan bahwa {@code jumlah}, {@code stok}, dan {@code stokmenjadi} tidak
	 * diinisialisasi, sehingga baris baru membawa {@code null} sampai layar mengisinya.
	 */
	public KoreksiItemDetail() {
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
	 * Mengembalikan keterangan baris.
	 *
	 * @return keterangan bebas, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan baris.
	 *
	 * @param keterangan teks keterangan baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menyetel judul/koleksi yang stoknya dikoreksi.
	 *
	 * @param item judul yang dikoreksi.
	 */
	public void setItem(Item item) {
		this.item = item;
	}

	/**
	 * Mengembalikan judul/koleksi yang stoknya dikoreksi.
	 *
	 * <p>Getter menjalankan {@code check(...)} milik {@link GeneralValueObject} untuk menukar
	 * proxy Hibernate yang sudah terlepas session dengan instance yang aman dibaca, lalu
	 * <b>menulis hasilnya balik ke field</b>. Karena itu getter ini mengubah state objek
	 * (getter destruktif ringan) dan aman dipanggil dari renderer ZK meski relasinya dipetakan
	 * {@link FetchType#LAZY}.</p>
	 *
	 * @return judul yang dikoreksi, atau {@code null} bila belum ditentukan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = true)
	public Item getItem() {
		item = check(item);
		return item;
	}

	/**
	 * Menyetel delta bertanda koreksi stok.
	 *
	 * <p><b>Kontrak nilai.</b> Nilai yang diharapkan adalah selisih <em>bertanda</em>, bukan
	 * besaran mutlak. Layar ZK menghitungnya sebagai
	 * {@code Math.abs(masukanPengguna) * kodeTransaksi.getJenis()} sehingga pengguna cukup
	 * mengetik angka positif dan arahnya ditentukan oleh pilihan kode transaksi
	 * (penambahan {@code +1} atau pengurangan {@code -1}). Pemanggil non-UI yang menyetel
	 * nilai ini secara langsung <b>wajib</b> memberi tanda sendiri; nilai positif akan
	 * menambah stok apa pun kode transaksi yang tersimpan pada baris.</p>
	 *
	 * <p><b>Catatan integritas.</b> Setter tidak memeriksa konsistensi apa pun. Ia tidak
	 * membandingkan tanda nilai baru dengan {@link #getKodeTransaksi() kodeTransaksi} yang
	 * sedang terpasang, tidak memperbarui {@link #getStokmenjadi() stokmenjadi} agar tetap
	 * sama dengan {@code stok + jumlah}, dan tidak menolak nilai yang membuat stok menjadi
	 * negatif. Menyetel {@code jumlah} tanpa memperbarui {@code stokmenjadi} menghasilkan baris
	 * yang secara aritmetika bertentangan dengan dirinya sendiri, dan karena tidak ada
	 * pengecekan di sisi model maupun basis data, baris seperti itu akan tersimpan serta
	 * terposting seolah-olah sah. Urutan pemanggilan yang benar adalah: setel
	 * {@code kodeTransaksi}, hitung dan setel {@code jumlah}, lalu setel {@code stokmenjadi}
	 * dari {@code stok + jumlah} &mdash; persis seperti yang dilakukan
	 * {@code helper/KoreksiItemDetailAction}.</p>
	 *
	 * @param jumlah delta bertanda; positif menambah stok, negatif mengurangi.
	 */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * Mengembalikan delta bertanda koreksi stok untuk baris ini.
	 *
	 * <p>Inilah satu-satunya angka pada baris koreksi yang benar-benar memengaruhi persediaan:
	 * {@code KoreksiItemAction} meneruskannya langsung sebagai {@code qty} mutasi stok, apa
	 * adanya, tanpa membaca ulang {@link #getStok() stok} maupun {@link #getStokmenjadi()
	 * stokmenjadi}. Nilai positif berarti eksemplar ditambahkan ke catatan (ditemukan saat
	 * opname, sumbangan yang belum tercatat), nilai negatif berarti eksemplar dikeluarkan
	 * (hilang, rusak berat, dimusnahkan).</p>
	 *
	 * <p>Getter <b>tidak</b> menormalkan {@code null} menjadi {@code 0.0}. Baris yang dibuat
	 * lewat konstruktor dan belum disunting di layar, atau baris lama dengan kolom
	 * {@code NULL}, akan mengembalikan {@code null}; layar ZK karenanya memeriksa {@code null}
	 * secara eksplisit sebelum berhitung, dan pemanggil lain harus melakukan hal yang sama
	 * sebelum melakukan operasi aritmetika atau {@code doubleValue()}.</p>
	 *
	 * @return delta bertanda koreksi; dapat {@code null} bila baris belum diisi.
	 */
	public Double getJumlah() {
		return jumlah;
	}

	/**
	 * Menyetel header dokumen koreksi pemilik baris ini.
	 *
	 * @param koreksiItem header dokumen koreksi.
	 */
	public void setKoreksiItem(KoreksiItem koreksiItem) {
		this.koreksiItem = koreksiItem;
	}

	/**
	 * Mengembalikan header dokumen koreksi pemilik baris ini.
	 *
	 * <p>Relasi dipetakan {@link FetchMode#SELECT} (eager per baris) sehingga aman dibaca dari
	 * renderer. Kolomnya {@code nullable}, sehingga secara struktur mungkin terdapat baris
	 * rincian yatim tanpa header &mdash; kondisi yang tidak pernah sah secara bisnis dan
	 * sebaiknya dicegah di lapisan action.</p>
	 *
	 * @return header dokumen koreksi, atau {@code null} bila baris belum dikaitkan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "koreksi_item", nullable = true)
	public KoreksiItem getKoreksiItem() {
		return koreksiItem;
	}

	/**
	 * Menyetel jenis penyesuaian (kode transaksi) baris ini.
	 *
	 * <p>Nilai yang dipakai layar praktis hanya dua: {@code LibraryUtil.adjustmentPenambahan}
	 * dan {@code LibraryUtil.adjustmentPengurangan}. Setter tidak membatasi pilihan tersebut
	 * dan tidak menyesuaikan tanda {@link #getJumlah() jumlah} yang sudah tersimpan, sehingga
	 * mengganti kode transaksi tanpa menghitung ulang {@code jumlah} akan membuat arah yang
	 * ditampilkan berbeda dari arah yang benar-benar diposting.</p>
	 *
	 * @param kodeTransaksi jenis penyesuaian.
	 */
	public void setKodeTransaksi(KodeTransaksi kodeTransaksi) {
		this.kodeTransaksi = kodeTransaksi;
	}

	/**
	 * Mengembalikan jenis penyesuaian (kode transaksi) baris ini.
	 *
	 * <p>Getter menjalankan {@code check(...)} lalu menulis hasilnya balik ke field (getter
	 * destruktif ringan) sehingga aman dibaca dari renderer meski relasi dipetakan
	 * {@link FetchType#LAZY}. Kolomnya {@code nullable}: baris tanpa kode transaksi tetap
	 * tersimpan, dan karena tanda sudah melekat pada {@code jumlah}, mutasi stoknya tetap
	 * terjadi &mdash; hanya labelnya yang hilang dari laporan.</p>
	 *
	 * @return jenis penyesuaian, atau {@code null} bila belum ditentukan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kode_transaksi", nullable = true)
	public KodeTransaksi getKodeTransaksi() {
		kodeTransaksi = check(kodeTransaksi);
		return kodeTransaksi;
	}

	/**
	 * Menyetel foto stok sebelum koreksi.
	 *
	 * @param stok stok sebelum koreksi.
	 */
	public void setStok(Double stok) {
		this.stok = stok;
	}

	/**
	 * Mengembalikan foto stok sebelum koreksi.
	 *
	 * <p>Nilai ini <b>tidak</b> ikut memengaruhi mutasi persediaan; ia hanya merekam berapa
	 * stok tercatat pada saat baris disunting di layar, agar dokumen koreksi dapat dibaca
	 * sebagai "dari sekian menjadi sekian". Karena tidak pernah dihitung ulang saat posting,
	 * angka ini bisa kedaluwarsa untuk dokumen yang lama berstatus draf.</p>
	 *
	 * <p>Getter tidak menormalkan {@code null}; layar ZK memakai pola
	 * {@code stok == null ? 0.0 : stok} setiap kali menampilkannya.</p>
	 *
	 * @return stok sebelum koreksi, atau {@code null} bila belum direkam.
	 */
	public Double getStok() {
		return stok;
	}

	/**
	 * Menyetel foto stok sesudah koreksi.
	 *
	 * <p>Pemanggil bertanggung jawab menjaga {@code stokmenjadi == stok + jumlah}; model tidak
	 * menghitungnya sendiri dan tidak memvalidasinya.</p>
	 *
	 * @param stokmenjadi stok sesudah koreksi.
	 */
	public void setStokmenjadi(Double stokmenjadi) {
		this.stokmenjadi = stokmenjadi;
	}

	/**
	 * Mengembalikan foto stok sesudah koreksi.
	 *
	 * <p>Sama seperti {@link #getStok()}, nilai ini bersifat dokumentatif dan tidak memengaruhi
	 * mutasi persediaan. Ia dihitung layar sebagai {@code stok + jumlah} pada saat penyuntingan
	 * dan dibekukan di sana. Bila stok bergerak antara penyuntingan dan persetujuan dokumen,
	 * angka yang tersimpan tidak lagi sesuai dengan hasil posting yang sebenarnya; rekonsiliasi
	 * sebaiknya dihitung ulang dari mutasi, bukan dibaca dari kolom ini.</p>
	 *
	 * <p>Getter tidak menormalkan {@code null}, dan tidak menolak nilai negatif &mdash; stok
	 * minus tercatat apa adanya.</p>
	 *
	 * @return stok sesudah koreksi, atau {@code null} bila belum direkam.
	 */
	public Double getStokmenjadi() {
		return stokmenjadi;
	}

}
