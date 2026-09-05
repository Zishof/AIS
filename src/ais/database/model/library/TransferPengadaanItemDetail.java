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
 * Entity <b>baris rincian</b> dokumen transfer (mutasi antar perpustakaan) bahan pustaka (tabel
 * {@code library.transfer_pengadaan_item_detail}). Satu baris menyatakan satu eksemplar yang
 * dipindahkan, dan menunjuk balik ke header {@link TransferPengadaanItem}.
 *
 * <p><b>Granularitas per eksemplar, bukan per kuantitas.</b> Ini adalah perbedaan terpenting
 * antara baris transfer dan baris dokumen pengadaan lain. Field kuantitas {@code jumlah} yang
 * ada pada {@link ReturPengadaanItemDetail}, {@link KoreksiItemDetail}, dan
 * {@link SaldoAwalDetail} <b>sengaja dinonaktifkan</b> di kelas ini (deklarasinya masih tersisa
 * sebagai komentar sumber). Sebagai gantinya baris membawa {@link #getItemPunyaBarcode()
 * itemPunyaBarcode}, yaitu eksemplar fisik tertentu. Konsekuensinya:</p>
 * <ul>
 *   <li>memindahkan sepuluh eksemplar berarti sepuluh baris rincian, bukan satu baris dengan
 *       {@code jumlah = 10};</li>
 *   <li>laporan yang menjumlahkan kolom kuantitas tidak dapat dipakai untuk dokumen transfer
 *       &mdash; hitunglah jumlah <em>baris</em>;</li>
 *   <li>keterlacakan per barcode lebih baik daripada pada {@link ReturPengadaanItemDetail} yang
 *       hanya menyimpan judul plus kuantitas.</li>
 * </ul>
 *
 * <p><b>Redundansi {@code item} vs {@code itemPunyaBarcode}.</b> Kedua relasi disimpan
 * berdampingan meskipun {@link ItemPunyaBarcode} sendiri sudah menunjuk judulnya. Penyimpanan
 * ganda ini adalah denormalisasi untuk mempercepat tampilan grid, tetapi model <b>tidak</b>
 * memverifikasi bahwa {@link #getItem() item} benar-benar sama dengan judul milik
 * {@code itemPunyaBarcode}. Baris yang tidak konsisten akan lolos tersimpan dan membuat laporan
 * per judul menyimpang dari kenyataan fisik.</p>
 *
 * <p><b>Tidak ada penjaga perpindahan ganda.</b> Model tidak mencegah satu
 * {@link ItemPunyaBarcode} yang sama muncul pada dua baris dokumen transfer yang berbeda, tidak
 * memeriksa bahwa eksemplar tersebut sedang berada di perpustakaan asal, dan tidak memeriksa
 * bahwa eksemplar tidak sedang dipinjam anggota. Seluruh pemeriksaan tersebut menjadi tanggung
 * jawab lapisan action.</p>
 *
 * <p><b>Jejak audit.</b> Kelas ditandai {@link Audited} sehingga Hibernate Envers merekam setiap
 * revisi. Trio field audit ringan {@code oleh}/{@code olehId}/{@code tanggal_dirubah} beserta
 * callback {@link #onUpdate()} merupakan keharusan teknis mekanisme audit AIS.</p>
 *
 * <p><b>Efek samping.</b> Accessor dan mutator hanya membaca atau mengubah state di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab
 * DAO/service dengan session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see TransferPengadaanItem
 * @see ItemPunyaBarcode
 * @see Item
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "library", name = "transfer_pengadaan_item_detail")



public class TransferPengadaanItemDetail extends GeneralValueObject {

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
	 * sehingga aman terhadap {@code null}. Perhatikan bahwa yang ditampilkan adalah
	 * <em>judul</em>, bukan barcode eksemplar &mdash; dua baris berbeda pada dokumen yang sama
	 * bisa tampil identik meski memindahkan eksemplar yang berlainan.</p>
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

	/** Judul/koleksi yang dipindahkan pada baris ini (denormalisasi dari eksemplarnya). */
	private Item item;
	// private Double jumlah;
	/** Header dokumen transfer pemilik baris ini. */
	private TransferPengadaanItem transferPengadaanItem;
	/** Catatan bebas pada tingkat baris. */
	private String keterangan;

	/** Eksemplar fisik (barcode) yang dipindahkan; inilah granularitas sesungguhnya baris ini. */
	private ItemPunyaBarcode itemPunyaBarcode;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate/ZK untuk instansiasi via refleksi.
	 */
	public TransferPengadaanItemDetail() {
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
	 * Menyetel judul/koleksi yang dipindahkan pada baris ini.
	 *
	 * <p><b>Catatan integritas:</b> setter tidak memverifikasi kesesuaian dengan judul milik
	 * {@link #getItemPunyaBarcode() itemPunyaBarcode}. Pemanggil wajib mengisi keduanya secara
	 * konsisten.</p>
	 *
	 * @param item judul yang dipindahkan.
	 */
	public void setItem(Item item) {
		this.item = item;
	}

	/**
	 * Mengembalikan judul/koleksi yang dipindahkan pada baris ini.
	 *
	 * <p>Getter menjalankan {@code check(...)} milik {@link GeneralValueObject} untuk menukar
	 * proxy Hibernate yang sudah terlepas session dengan instance yang aman dibaca, lalu
	 * <b>menulis hasilnya balik ke field</b>. Karena itu getter ini mengubah state objek
	 * (getter destruktif ringan) dan aman dipanggil dari renderer ZK meski relasinya dipetakan
	 * {@link FetchType#LAZY}.</p>
	 *
	 * @return judul yang dipindahkan, atau {@code null} bila belum ditentukan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = true)
	public Item getItem() {
		item = check(item);
		return item;
	}

	/**
	 * Menyetel header dokumen transfer pemilik baris ini.
	 *
	 * @param transferPengadaanItem header dokumen transfer.
	 */
	public void setTransferPengadaanItem(TransferPengadaanItem transferPengadaanItem) {
		this.transferPengadaanItem = transferPengadaanItem;
	}

	/**
	 * Mengembalikan header dokumen transfer pemilik baris ini.
	 *
	 * <p>Relasi dipetakan {@link FetchMode#SELECT} (eager per baris) sehingga aman dibaca dari
	 * renderer. Kolomnya {@code nullable}, sehingga secara struktur mungkin terdapat baris
	 * rincian yatim tanpa header &mdash; kondisi yang tidak pernah sah secara bisnis dan
	 * sebaiknya dicegah di lapisan action.</p>
	 *
	 * @return header dokumen transfer, atau {@code null} bila baris belum dikaitkan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "transfer_pengadaan_item", nullable = true)
	public TransferPengadaanItem getTransferPengadaanItem() {
		return transferPengadaanItem;
	}

	/**
	 * Mengembalikan eksemplar fisik (barcode) yang dipindahkan pada baris ini.
	 *
	 * <p>Inilah data pokok baris transfer: karena kolom kuantitas tidak dipakai, satu baris
	 * selalu berarti tepat satu eksemplar. Relasi dipetakan {@link FetchMode#SELECT} sehingga
	 * aman dibaca dari renderer ZK.</p>
	 *
	 * <p><b>Catatan integritas:</b> kolomnya {@code nullable}. Baris transfer tanpa eksemplar
	 * praktis tidak bermakna &mdash; ia menyatakan "sesuatu dari judul X pindah" tanpa
	 * menyebut yang mana &mdash; namun model tetap menerimanya.</p>
	 *
	 * @return eksemplar yang dipindahkan, atau {@code null} bila belum ditentukan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "item_punya_barcode", nullable = true)
	public ItemPunyaBarcode getItemPunyaBarcode() {
		return itemPunyaBarcode;
	}

	/**
	 * Menyetel eksemplar fisik (barcode) yang dipindahkan pada baris ini.
	 *
	 * <p><b>Catatan integritas:</b> setter tidak memeriksa apakah eksemplar tersebut sedang
	 * berada di perpustakaan asal, sedang dipinjam anggota, atau sudah tercantum pada dokumen
	 * transfer lain yang belum diterima. Pemeriksaan itu harus dilakukan lapisan action sebelum
	 * baris disimpan.</p>
	 *
	 * @param itemPunyaBarcode eksemplar yang dipindahkan.
	 */
	public void setItemPunyaBarcode(ItemPunyaBarcode itemPunyaBarcode) {
		this.itemPunyaBarcode = itemPunyaBarcode;
	}

}
