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
 * Entity <b>baris rincian</b> dokumen retur pengadaan item perpustakaan (tabel
 * {@code library.retur_pengadaan_item_detail}). Satu baris menyatakan satu judul/koleksi
 * {@link Item} yang dikembalikan perpustakaan kepada penyedia beserta kuantitasnya, dan menunjuk
 * balik ke header {@link ReturPengadaanItem}.
 *
 * <p><b>Dua kolom kuantitas dan bedanya.</b> Entity ini menyimpan dua angka yang mudah
 * tertukar:</p>
 * <ul>
 *   <li>{@link #getJumlah() jumlah} &mdash; kuantitas <em>yang layak/diajukan</em> untuk diretur,
 *       biasanya disalin dari baris penerimaan sebagai batas atas;</li>
 *   <li>{@link #getDikembalikan() dikembalikan} &mdash; kuantitas <em>yang benar-benar
 *       dikembalikan</em> pada dokumen ini. Nilai inilah yang dipakai lapisan action
 *       ({@code ReturPengadaanItemAction}) sebagai {@code qty} mutasi stok.</li>
 * </ul>
 * <p>Layar ZK {@code helper/ReturPengadaanItemDetailAction} menampilkan sisa sebagai
 * {@code jumlah - dikembalikan}. Perlu ditegaskan bahwa perhitungan sisa itu hanya kosmetik di
 * layar: <b>tidak ada penjaga keseimbangan pada model ini</b>. Tidak ada validasi bahwa
 * {@code dikembalikan <= jumlah}, tidak ada validasi bahwa {@code jumlah} tidak melebihi
 * kuantitas pada {@link PenerimaanPengadaanItemDetail} yang bersangkutan, dan tidak ada
 * pemeriksaan bahwa satu baris penerimaan tidak diretur berulang kali lewat beberapa dokumen
 * retur. Importer massal, jalur API, atau perubahan langsung lewat DAO dapat menghasilkan retur
 * melebihi penerimaan tanpa peringatan apa pun.</p>
 *
 * <p><b>Granularitas.</b> Berbeda dengan {@link TransferPengadaanItemDetail} dan
 * {@link KembaliPengadaanItemDetail} yang menyimpan referensi {@link ItemPunyaBarcode}
 * (eksemplar fisik tertentu), baris retur ini hanya bekerja pada tingkat judul
 * ({@link Item}) plus kuantitas. Konsekuensinya, eksemplar mana persisnya yang kembali ke
 * penyedia tidak terekam di sini; penelusuran per barcode harus dilakukan lewat dokumen
 * penerimaan asal.</p>
 *
 * <p><b>Jejak audit.</b> Kelas ditandai {@link Audited} sehingga Hibernate Envers merekam setiap
 * revisi. Trio field audit ringan {@link #getOleh() oleh}, {@link #getOlehId() olehId}, dan
 * {@link #getTanggal_dirubah() tanggal_dirubah} beserta callback {@link #onUpdate()} merupakan
 * keharusan teknis mekanisme audit AIS, bukan duplikasi yang perlu dibersihkan.</p>
 *
 * <p><b>Efek samping.</b> Accessor dan mutator hanya membaca atau mengubah state di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab
 * DAO/service dengan session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see ReturPengadaanItem
 * @see PenerimaanPengadaanItemDetail
 * @see Item
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "library", name = "retur_pengadaan_item_detail")



public class ReturPengadaanItemDetail extends GeneralValueObject {

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
	public String getOlehId() {return olehId;}

	/**
	 * Menyetel ID pengguna pengubah terakhir. Bersifat <b>no-op bila nilai baru kosong atau
	 * hanya berisi spasi</b> agar jejak audit lama tidak tertimpa oleh pemanggil tanpa konteks
	 * pengguna.
	 *
	 * @param olehId ID pengguna baru; diabaikan bila {@code null}/blank.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}

	/**
	 * Representasi teks baris untuk grid, combobox, dan log.
	 *
	 * <p>Sengaja membaca field {@link #item} secara langsung (bukan lewat {@link #getItem()})
	 * dan merangkainya dengan {@code ""} sehingga aman terhadap {@code null}. Hasilnya adalah
	 * {@code toString()} milik {@link Item}, atau string {@code "null"} bila item belum
	 * diisi.</p>
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

	/** Judul/koleksi yang diretur pada baris ini. */
	private Item item;
	/** Kuantitas yang layak/diajukan untuk diretur; berperan sebagai batas atas di layar. */
	private Double jumlah = 0.0;
	/** Kuantitas yang benar-benar dikembalikan; nilai inilah yang menjadi qty mutasi stok. */
	private Double dikembalikan = 0.0;
	/** Header dokumen retur pemilik baris ini. */
	private ReturPengadaanItem returPengadaanItem;
	/** Catatan bebas pada tingkat baris, umumnya alasan retur untuk judul ini. */
	private String keterangan;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate/ZK untuk instansiasi via refleksi.
	 * Kedua kuantitas ({@code jumlah} dan {@code dikembalikan}) sudah terinisialisasi
	 * {@code 0.0} sehingga baris baru tidak pernah membawa {@code null}.
	 */
	public ReturPengadaanItemDetail() {
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
	 * Menyetel judul/koleksi yang diretur pada baris ini.
	 *
	 * @param item item yang diretur.
	 */
	public void setItem(Item item) {
		this.item = item;
	}

	/**
	 * Mengembalikan judul/koleksi yang diretur pada baris ini.
	 *
	 * <p>Getter menjalankan {@code check(...)} milik {@link GeneralValueObject} lebih dahulu
	 * untuk menukar proxy Hibernate yang sudah terlepas dari session dengan instance yang aman
	 * dibaca, lalu <b>menulis hasilnya balik ke field</b>. Karena itu getter ini mengubah state
	 * objek (getter destruktif ringan) dan aman dipanggil dari renderer ZK meski relasinya
	 * dipetakan {@link FetchType#LAZY}.</p>
	 *
	 * @return item yang diretur, atau {@code null} bila belum ditentukan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = true)
	public Item getItem() {
		item = check(item);
		return item;
	}

	/**
	 * Menyetel kuantitas yang layak/diajukan untuk diretur.
	 *
	 * <p><b>Catatan integritas:</b> setter tidak menolak nilai negatif dan tidak
	 * membandingkannya dengan kuantitas pada dokumen penerimaan asal. Batas atas retur
	 * sepenuhnya menjadi tanggung jawab pemanggil.</p>
	 *
	 * @param jumlah kuantitas baru.
	 */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * Mengembalikan kuantitas yang layak/diajukan untuk diretur.
	 *
	 * <p>Berbeda dengan banyak getter kuantitas lain di modul ini, getter ini <b>tidak</b>
	 * melakukan normalisasi {@code null} &rarr; {@code 0.0}. Field memang diinisialisasi
	 * {@code 0.0} pada konstruktor, tetapi baris yang dimuat dari database dengan kolom
	 * {@code NULL} akan mengembalikan {@code null} dan berpotensi memicu
	 * {@code NullPointerException} pada aritmetika pemanggil (mis. perhitungan sisa
	 * {@code jumlah - dikembalikan}). Layar ZK karenanya memeriksa {@code null} secara
	 * eksplisit sebelum berhitung.</p>
	 *
	 * @return kuantitas yang diajukan untuk diretur; dapat {@code null} untuk data lama.
	 */
	public Double getJumlah() {
		return jumlah;
	}

	/**
	 * Menyetel header dokumen retur pemilik baris ini.
	 *
	 * @param returPengadaanItem header dokumen retur.
	 */
	public void setReturPengadaanItem(ReturPengadaanItem returPengadaanItem) {
		this.returPengadaanItem = returPengadaanItem;
	}

	/**
	 * Mengembalikan header dokumen retur pemilik baris ini.
	 *
	 * <p>Relasi dipetakan {@link FetchMode#SELECT} (eager per baris) sehingga aman dibaca dari
	 * renderer. Kolomnya {@code nullable}, sehingga secara struktur mungkin terdapat baris
	 * rincian yatim tanpa header &mdash; kondisi yang tidak pernah sah secara bisnis dan
	 * sebaiknya dicegah di lapisan action.</p>
	 *
	 * @return header dokumen retur, atau {@code null} bila baris belum dikaitkan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "retur_pengadaan_item", nullable = true)
	public ReturPengadaanItem getReturPengadaanItem() {
		return returPengadaanItem;
	}

	/**
	 * Mengembalikan kuantitas yang benar-benar dikembalikan kepada penyedia pada baris ini.
	 *
	 * <p>Nilai inilah yang dibaca {@code ReturPengadaanItemAction} untuk mengisi {@code qty}
	 * mutasi stok, jadi angka ini &mdash; bukan {@link #getJumlah() jumlah} &mdash; yang
	 * menentukan dampak dokumen retur terhadap persediaan. Seperti {@link #getJumlah()},
	 * getter ini tidak menormalkan {@code null} menjadi {@code 0.0}.</p>
	 *
	 * @return kuantitas yang dikembalikan; dapat {@code null} untuk data lama.
	 */
	public Double getDikembalikan() {
		return dikembalikan;
	}

	/**
	 * Menyetel kuantitas yang benar-benar dikembalikan kepada penyedia.
	 *
	 * <p><b>Catatan integritas:</b> setter tidak memaksakan {@code dikembalikan <= jumlah}.
	 * Layar ZK memang menghitung dan menampilkan sisa, tetapi perhitungan itu hanya informatif
	 * dan tidak memblokir penyimpanan. Akibatnya nilai yang melebihi kuantitas penerimaan asal
	 * akan lolos ke database dan langsung mengurangi stok lebih banyak dari yang pernah
	 * masuk.</p>
	 *
	 * @param dikembalikan kuantitas yang dikembalikan.
	 */
	public void setDikembalikan(Double dikembalikan) {
		this.dikembalikan = dikembalikan;
	}

}
