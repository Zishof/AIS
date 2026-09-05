package ais.database.model.library;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
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
 * Entity <b>baris rincian tingkat ketiga</b> dokumen saldo awal koleksi perpustakaan (tabel
 * {@code library.saldo_awal_detail_detail}). Satu baris menyatakan satu <em>eksemplar</em> yang
 * dicatat sebagai stok pembuka, lengkap dengan nomor barcode dan kondisi fisiknya, dan menunjuk
 * balik ke {@link SaldoAwalDetail}.
 *
 * <p>Tingkat ini hanya terisi bila baris induknya memakai mode per eksemplar, yaitu ketika
 * {@link SaldoAwalDetail#getDataPerItem()} bernilai benar. Dalam mode itu, kuantitas yang sahih
 * bukan lagi kolom {@code jumlah} pada induk melainkan <b>cacah baris pada tingkat ini</b>.</p>
 *
 * <p><b>Barcode disimpan sebagai teks, bukan relasi.</b> Perbedaan penting dengan dokumen lain
 * yang bekerja per eksemplar &mdash; {@link TransferPengadaanItemDetail},
 * {@link KembaliPengadaanItemDetail}, {@link PeminjamanPengadaanItemDetail} &mdash; adalah bahwa
 * ketiganya menunjuk {@link ItemPunyaBarcode} lewat kunci asing, sedangkan kelas ini hanya
 * menyimpan {@link #getBarcode() barcode} sebagai {@code String} biasa. Konsekuensinya:</p>
 * <ul>
 *   <li>tidak ada integritas referensial &mdash; barcode yang salah ketik, duplikat, atau tidak
 *       pernah ada sebagai {@link ItemPunyaBarcode} tetap tersimpan tanpa keluhan;</li>
 *   <li>tidak ada penjaga yang mencegah nomor barcode yang sama dicatat pada dua baris saldo
 *       awal yang berbeda, atau dicatat padahal eksemplarnya sudah ada dari alur pengadaan;</li>
 *   <li>penelusuran dari eksemplar ke dokumen saldo awal asalnya hanya bisa dilakukan lewat
 *       pencocokan teks, bukan lewat {@code join}.</li>
 * </ul>
 * <p>Pilihan desain ini masuk akal untuk pekerjaan pendataan awal &mdash; barcode diketik atau
 * dipindai lebih dulu, entri {@link ItemPunyaBarcode}-nya dibuat kemudian &mdash; tetapi berarti
 * seluruh validasi barcode berada di lapisan action, bukan di basis data.</p>
 *
 * <p><b>Kondisi fisik.</b> {@link #getStatusItem() statusItem} merekam keadaan eksemplar pada
 * saat pendataan (baik, rusak, hilang, dan seterusnya) sehingga koleksi lama tidak semuanya
 * masuk sebagai "baik". Kolomnya {@code nullable}; baris tanpa status tetap tersimpan.</p>
 *
 * <p><b>Jejak audit.</b> Kelas ditandai {@link Audited}. Trio field audit ringan
 * {@code oleh}/{@code olehId}/{@code tanggal_dirubah} beserta callback {@link #onUpdate()}
 * merupakan keharusan teknis mekanisme audit AIS.</p>
 *
 * <p><b>Efek samping.</b> Accessor dan mutator hanya membaca atau mengubah state di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab
 * DAO/service dengan session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see SaldoAwalDetail
 * @see SaldoAwal
 * @see StatusItem
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "library", name = "saldo_awal_detail_detail")



public class SaldoAwalDetailDetail extends GeneralValueObject {

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
	 * <p><b>Perhatikan:</b> yang ditampilkan adalah {@link #keterangan}, bukan
	 * {@link #barcode}. Ini berbeda dari saudara-saudaranya di modul {@code library} yang
	 * menampilkan item atau kode dokumen. Karena keterangan biasanya kosong pada baris hasil
	 * pemindaian massal, {@code toString()} sering menghasilkan {@code "null"} &mdash; barcode
	 * yang justru menjadi identitas baris ini harus dibaca lewat {@link #getBarcode()}.</p>
	 *
	 * @return keterangan baris dirangkai dengan {@code ""}; dapat berupa string {@code "null"}.
	 */
	public String toString() {
		return keterangan + "";
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

	/** Nomor barcode eksemplar, disimpan sebagai teks bebas tanpa relasi ke {@link ItemPunyaBarcode}. */
	private String barcode;
	/** Baris saldo awal tingkat kedua (per judul) pemilik baris eksemplar ini. */
	private SaldoAwalDetail saldoAwalDetail;
	/** Catatan bebas per eksemplar; juga menjadi isi {@link #toString()}. */
	private String keterangan;
	/** Kondisi fisik eksemplar pada saat pendataan awal. */
	private StatusItem statusItem;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate/ZK untuk instansiasi via refleksi.
	 */
	public SaldoAwalDetailDetail() {
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
	 * Mengembalikan catatan bebas per eksemplar.
	 *
	 * @return keterangan bebas, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel catatan bebas per eksemplar. Perhatikan bahwa nilai ini juga menjadi hasil
	 * {@link #toString()}, sehingga mengisinya memperbaiki keterbacaan baris di layar dan log.
	 *
	 * @param keterangan teks keterangan baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan baris saldo awal tingkat kedua (per judul) pemilik baris eksemplar ini.
	 *
	 * <p>Relasi dipetakan {@link FetchMode#SELECT} (eager per baris) sehingga aman dibaca dari
	 * renderer. Kolomnya {@code nullable}, sehingga secara struktur mungkin terdapat baris
	 * eksemplar yatim yang tidak terhubung ke judul mana pun &mdash; kondisi yang tidak pernah
	 * sah secara bisnis dan sebaiknya dicegah di lapisan action.</p>
	 *
	 * @return baris induk per judul, atau {@code null} bila belum dikaitkan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "saldo_awal_detail", nullable = true)
	public SaldoAwalDetail getSaldoAwalDetail() {
		return saldoAwalDetail;
	}

	/**
	 * Menyetel baris saldo awal tingkat kedua pemilik baris eksemplar ini.
	 *
	 * <p>Pemanggil juga bertanggung jawab menyetel
	 * {@link SaldoAwalDetail#setDataPerItem(Boolean)} bernilai benar pada induknya, karena
	 * keberadaan baris tingkat ketiga saja tidak otomatis mengalihkan induk ke mode per
	 * eksemplar.</p>
	 *
	 * @param saldoAwalDetail baris induk per judul.
	 */
	public void setSaldoAwalDetail(SaldoAwalDetail saldoAwalDetail) {
		this.saldoAwalDetail = saldoAwalDetail;
	}

	/**
	 * Mengembalikan nomor barcode eksemplar.
	 *
	 * <p>Disimpan sebagai teks bebas, bukan kunci asing ke {@link ItemPunyaBarcode}. Nilai ini
	 * adalah identitas sesungguhnya baris ini &mdash; bukan {@link #toString()} yang justru
	 * menampilkan keterangan. Getter tidak menormalkan {@code null} dan tidak memangkas spasi,
	 * sehingga barcode hasil pemindaian yang membawa spasi di ujung akan tersimpan apa adanya
	 * dan gagal cocok saat dibandingkan dengan barcode lain.</p>
	 *
	 * @return nomor barcode eksemplar, atau {@code null} bila belum diisi.
	 */
	public String getBarcode() {
		return barcode;
	}

	/**
	 * Menyetel nomor barcode eksemplar.
	 *
	 * <p><b>Catatan integritas:</b> setter tidak memvalidasi format, tidak memangkas spasi,
	 * tidak memeriksa keunikan, dan tidak memastikan bahwa barcode tersebut memang terdaftar
	 * sebagai {@link ItemPunyaBarcode}. Barcode ganda pada dua baris saldo awal &mdash; atau
	 * barcode yang sudah ada dari alur pengadaan &mdash; akan lolos tersimpan dan menghasilkan
	 * penghitungan stok pembuka yang berlebih.</p>
	 *
	 * @param barcode nomor barcode eksemplar.
	 */
	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	/**
	 * Mengembalikan kondisi fisik eksemplar pada saat pendataan awal.
	 *
	 * <p>Relasi dipetakan {@link FetchMode#SELECT} sehingga aman dibaca dari renderer.
	 * Kolomnya {@code nullable}: baris tanpa status tetap tersimpan, dan laporan kondisi
	 * koleksi harus menyiapkan kategori "tidak diketahui" untuk kasus itu.</p>
	 *
	 * @return kondisi fisik eksemplar, atau {@code null} bila tidak dicatat.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "status_item", nullable = true)
	public StatusItem getStatusItem() {
		return statusItem;
	}

	/**
	 * Menyetel kondisi fisik eksemplar pada saat pendataan awal.
	 *
	 * @param statusItem kondisi fisik eksemplar.
	 */
	public void setStatusItem(StatusItem statusItem) {
		this.statusItem = statusItem;
	}

}
