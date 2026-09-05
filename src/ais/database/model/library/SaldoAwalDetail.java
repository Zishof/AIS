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
 * Entity <b>baris rincian tingkat kedua</b> dokumen saldo awal koleksi perpustakaan (tabel
 * {@code library.saldo_awal_detail}). Satu baris menyatakan satu judul/koleksi {@link Item} yang
 * dicatat sebagai stok pembuka, dan menunjuk balik ke header {@link SaldoAwal}.
 *
 * <h3>Dua mode pencatatan</h3>
 * <p>Baris ini dapat bekerja dalam dua mode yang dibedakan oleh penanda
 * {@link #getDataPerItem() dataPerItem}:</p>
 * <ul>
 *   <li><b>Mode kuantitas</b> ({@code dataPerItem} salah, keadaan awal) &mdash; baris hanya
 *       mencatat {@link #getJumlah() jumlah} eksemplar. Cepat, tetapi tidak ada barcode maupun
 *       kondisi fisik yang terekam.</li>
 *   <li><b>Mode per eksemplar</b> ({@code dataPerItem} benar) &mdash; rincian sesungguhnya
 *       pindah ke tingkat ketiga, yaitu kumpulan {@link SaldoAwalDetailDetail} yang masing-masing
 *       membawa barcode dan {@link StatusItem}. Layar
 *       ({@code helper/SaldoAwalDetailAction}) menonaktifkan kotak kuantitas begitu mode ini
 *       aktif, karena jumlah sebenarnya adalah cacah baris tingkat ketiga.</li>
 * </ul>
 *
 * <p><b>{@code dataPerItem} adalah penanda satu arah.</b> Di seluruh basis kode, penanda ini
 * hanya pernah disetel bernilai <em>benar</em> &mdash; oleh
 * {@code helper/SaldoAwalDetailAction} dan {@code helper/ItemPunyaBarcodeHelper} &mdash; dan
 * tidak pernah dikembalikan ke salah. Sekali sebuah baris beralih ke mode per eksemplar, ia
 * tidak dapat kembali ke mode kuantitas lewat antarmuka mana pun; kotak kuantitasnya terkunci
 * permanen. Bila pengguna keliru mengaktifkannya, satu-satunya jalan keluar adalah menghapus
 * baris dan membuatnya ulang. Perlu dicatat pula bahwa <b>kolom {@code jumlah} yang lama tetap
 * tersimpan apa adanya</b> saat mode beralih: nilainya tidak dinolkan dan tidak disesuaikan
 * dengan cacah baris tingkat ketiga, sehingga laporan yang menjumlahkan kolom {@code jumlah}
 * tanpa memeriksa {@code dataPerItem} akan menghitung ganda atau menghitung angka basi.</p>
 *
 * <p><b>Tidak ada penjaga keseimbangan.</b> Model tidak memastikan bahwa {@code jumlah} sama
 * dengan cacah {@link SaldoAwalDetailDetail} yang tergantung padanya, tidak menolak kuantitas
 * negatif, tidak mencegah satu judul muncul pada beberapa baris dokumen yang sama, dan tidak
 * memeriksa bahwa judul tersebut belum pernah dicatat pada dokumen saldo awal sebelumnya.</p>
 *
 * <p><b>Jejak audit.</b> Kelas ditandai {@link Audited}. Trio field audit ringan
 * {@code oleh}/{@code olehId}/{@code tanggal_dirubah} beserta callback {@link #onUpdate()}
 * merupakan keharusan teknis mekanisme audit AIS.</p>
 *
 * <p><b>Efek samping.</b> Accessor dan mutator hanya membaca atau mengubah state di memori,
 * kecuali {@link #getItem()} dan {@link #getJumlah()} yang menulis balik hasilnya ke field.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab
 * DAO/service dengan session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see SaldoAwal
 * @see SaldoAwalDetailDetail
 * @see BatchItemPunyaBarcode
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "library", name = "saldo_awal_detail")



public class SaldoAwalDetail extends GeneralValueObject {

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
	 * sehingga aman terhadap {@code null}. Mode pencatatan dan kuantitas tidak ikut tampil.</p>
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

	/** Judul/koleksi yang dicatat sebagai stok pembuka pada baris ini. */
	private Item item;
	/** Kuantitas stok pembuka; hanya bermakna selama {@link #dataPerItem} bernilai salah. */
	private Double jumlah;
	/** Header dokumen saldo awal pemilik baris ini. */
	private SaldoAwal saldoAwal;
	/** Catatan bebas pada tingkat baris. */
	private String keterangan;
	/** Penanda mode per eksemplar; satu arah, tidak pernah dikembalikan ke salah oleh kode mana pun. */
	private Boolean dataPerItem = false;

	/** Batch pembangkitan barcode yang dipakai untuk mencetak label eksemplar baris ini. */
	private BatchItemPunyaBarcode batchItemPunyaBarcode;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate/ZK untuk instansiasi via refleksi.
	 * {@code dataPerItem} terinisialisasi {@code false}, sehingga baris baru selalu bermula
	 * pada mode kuantitas.
	 */
	public SaldoAwalDetail() {
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
	 * Menyetel judul/koleksi yang dicatat pada baris ini.
	 *
	 * @param item judul yang dicatat.
	 */
	public void setItem(Item item) {
		this.item = item;
	}

	/**
	 * Mengembalikan judul/koleksi yang dicatat pada baris ini.
	 *
	 * <p>Getter menjalankan {@code check(...)} milik {@link GeneralValueObject} untuk menukar
	 * proxy Hibernate yang sudah terlepas session dengan instance yang aman dibaca, lalu
	 * <b>menulis hasilnya balik ke field</b> (getter destruktif ringan).</p>
	 *
	 * @return judul yang dicatat, atau {@code null} bila belum ditentukan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = true)
	public Item getItem() {
		item = check(item);
		return item;
	}

	/**
	 * Menyetel kuantitas stok pembuka baris ini.
	 *
	 * <p><b>Catatan integritas:</b> setter tidak menolak nilai negatif dan tidak memeriksa
	 * apakah baris sedang berada pada mode per eksemplar. Menyetel kuantitas pada baris yang
	 * {@link #getDataPerItem() dataPerItem}-nya benar menghasilkan angka yang bertentangan
	 * dengan cacah {@link SaldoAwalDetailDetail}-nya, dan model tidak akan menolaknya.</p>
	 *
	 * @param jumlah kuantitas stok pembuka.
	 */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * Mengembalikan kuantitas stok pembuka baris ini, dinormalkan ke {@code 1.0} bila belum
	 * diisi.
	 *
	 * <p><b>Perhatikan nilai bakunya: satu, bukan nol.</b> Baris yang dibuat lalu disimpan
	 * tanpa pernah diisi kuantitas akan tercatat sebagai satu eksemplar, bukan kosong. Untuk
	 * dokumen saldo awal pilihan ini masuk akal (sebuah baris memang berarti ada koleksinya),
	 * tetapi berbeda dari {@link ReturPengadaanItemDetail#getJumlah()} yang membiarkan
	 * {@code null} lolos dan dari {@link KoreksiItemDetail#getJumlah()} yang juga tidak
	 * menormalkan. Normalisasi ditulis balik ke field, sehingga getter ini mengubah state
	 * objek.</p>
	 *
	 * <p>Nilai ini hanya bermakna dalam mode kuantitas. Bila
	 * {@link #getDataPerItem() dataPerItem} bernilai benar, kuantitas yang sahih adalah cacah
	 * {@link SaldoAwalDetailDetail}; nilai pada kolom ini bisa jadi angka basi dari sebelum
	 * mode beralih.</p>
	 *
	 * @return kuantitas stok pembuka; tidak pernah {@code null}.
	 */
	public Double getJumlah() {
		if (jumlah == null) {
			jumlah = 1.0;
		}
		return jumlah;
	}

	/**
	 * Menyetel header dokumen saldo awal pemilik baris ini.
	 *
	 * @param saldoAwal header dokumen saldo awal.
	 */
	public void setSaldoAwal(SaldoAwal saldoAwal) {
		this.saldoAwal = saldoAwal;
	}

	/**
	 * Mengembalikan header dokumen saldo awal pemilik baris ini.
	 *
	 * <p>Relasi dipetakan {@link FetchMode#SELECT} (eager per baris) sehingga aman dibaca dari
	 * renderer. Kolomnya {@code nullable}, sehingga secara struktur mungkin terdapat baris
	 * rincian yatim tanpa header &mdash; kondisi yang tidak pernah sah secara bisnis dan
	 * sebaiknya dicegah di lapisan action.</p>
	 *
	 * @return header dokumen saldo awal, atau {@code null} bila baris belum dikaitkan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "saldo_awal", nullable = true)
	public SaldoAwal getSaldoAwal() {
		return saldoAwal;
	}

	/**
	 * Mengembalikan penanda mode per eksemplar.
	 *
	 * <p>Bernilai benar berarti rincian sesungguhnya berada pada kumpulan
	 * {@link SaldoAwalDetailDetail} (barcode + {@link StatusItem} per eksemplar) dan kolom
	 * {@link #getJumlah() jumlah} tidak lagi menjadi sumber kebenaran. Layar memakai nilai ini
	 * untuk menonaktifkan kotak kuantitas.</p>
	 *
	 * <p><b>Getter tidak menormalkan {@code null}.</b> Berbeda dari
	 * {@link BatasWaktuPeminjamanItem#getDefaultItem()} dan
	 * {@link DendaKeterlambatanItem#getDefaultItem()} yang mengubah {@code null} menjadi
	 * {@code false}, getter ini mengembalikan {@code null} apa adanya untuk baris lama yang
	 * kolomnya belum terisi. Karena itu seluruh pemanggil di basis kode memakai pola
	 * {@code getDataPerItem() != null && getDataPerItem()}; pemanggil baru harus melakukan hal
	 * yang sama agar tidak memicu {@code NullPointerException} saat <i>auto-unboxing</i>.</p>
	 *
	 * @return {@code true} bila baris memakai mode per eksemplar, {@code false} bila mode
	 *         kuantitas, atau {@code null} untuk baris lama yang belum menentukan.
	 */
	@Column(name = "data_per_item", nullable = true)
	public Boolean getDataPerItem() {
		return dataPerItem;
	}

	/**
	 * Menyetel penanda mode per eksemplar.
	 *
	 * <p><b>Praktis satu arah.</b> Di seluruh basis kode setter ini hanya pernah dipanggil
	 * dengan nilai {@code true} &mdash; oleh {@code helper/SaldoAwalDetailAction} ketika
	 * pengguna mulai mendaftarkan barcode, dan oleh {@code helper/ItemPunyaBarcodeHelper}.
	 * Tidak ada satu pun jalur yang mengembalikannya ke {@code false}, sehingga baris yang
	 * terlanjur beralih mode tidak dapat dikembalikan lewat antarmuka; kotak kuantitasnya
	 * terkunci permanen dan satu-satunya jalan keluar adalah menghapus lalu membuat ulang
	 * baris.</p>
	 *
	 * <p>Setter juga tidak menyentuh {@link #getJumlah() jumlah}. Nilai kuantitas lama tetap
	 * tersimpan setelah peralihan mode, sehingga laporan yang menjumlahkan kolom itu tanpa
	 * memeriksa penanda ini akan menghitung angka basi.</p>
	 *
	 * @param dataPerItem penanda mode per eksemplar.
	 */
	public void setDataPerItem(Boolean dataPerItem) {
		this.dataPerItem = dataPerItem;
	}

	/**
	 * Mengembalikan batch pembangkitan barcode yang dipakai mencetak label eksemplar baris ini.
	 *
	 * <p>Relasi ke {@link BatchItemPunyaBarcode} menghubungkan baris saldo awal dengan
	 * sekumpulan label barcode yang dibangkitkan sekaligus, sehingga petugas dapat mencetak
	 * ulang atau menelusuri asal-usul barcode. Kolomnya {@code nullable} karena mode kuantitas
	 * tidak memerlukan barcode sama sekali.</p>
	 *
	 * @return batch barcode terkait, atau {@code null} bila belum ada.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "batch_item_punya_barcode", nullable = true)
	public BatchItemPunyaBarcode getBatchItemPunyaBarcode() {
		return batchItemPunyaBarcode;
	}

	/**
	 * Menyetel batch pembangkitan barcode untuk baris ini.
	 *
	 * @param batchItemPunyaBarcode batch barcode terkait.
	 */
	public void setBatchItemPunyaBarcode(BatchItemPunyaBarcode batchItemPunyaBarcode) {
		this.batchItemPunyaBarcode = batchItemPunyaBarcode;
	}

}
