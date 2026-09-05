package ais.database.model.sirs;

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
 * Entitas baris (detail) <b>Pemakaian Retur Item</b> medis pada schema
 * {@code sirs} (tabel {@code pemakaian_retur_item_detail}). Setiap baris
 * mencatat satu {@link ItemMedis} yang dikembalikan ke gudang pada satu dokumen
 * {@link PemakaianReturItem}, beserta kuantitas ({@link #getJumlah()}),
 * nilainya ({@link #getHarga()}), dan potret stok sebelum
 * ({@link #getStok()}) serta sesudahnya ({@link #getStokmenjadi()}).
 *
 * <h2>Baris ini MENAMBAH stok</h2>
 * <p>
 * Saat dokumen induknya disetujui, {@link #getJumlah()} pada baris ini
 * MENAMBAH stok {@link #getItem()} di gudang
 * {@link PemakaianReturItem#getLokasi()} — kebalikan persis dari
 * {@link PemakaianItemDetail} yang menguranginya.
 * </p>
 *
 * <h2>Kembaran struktural PemakaianItemDetail</h2>
 * <p>
 * Entitas ini dan {@link PemakaianItemDetail} memiliki kolom yang <b>identik
 * satu per satu</b>: {@code item}, {@code jumlah}, {@code harga},
 * {@code keterangan}, {@code stok}, {@code stokmenjadi}, ditambah blok field
 * audit yang sama. Yang membedakan hanyalah:
 * </p>
 * <ul>
 *   <li>nama tabel — {@code sirs.pemakaian_retur_item_detail} di sini versus
 *       {@code sirs.pemakaian_item_detail};</li>
 *   <li>nama kolom induk — {@code pemakaian_retur_item} di sini versus
 *       {@code pemakaian_item};</li>
 *   <li>ARAH mutasi stok — menambah di sini, mengurangi di sana.</li>
 * </ul>
 * <p>
 * Ketiga perbedaan itu semuanya berupa NAMA di dalam string atau tanda pada
 * perhitungan — tidak satu pun berupa perbedaan tipe yang dapat ditangkap
 * kompilator. Kode yang lahir dari penyalinan antara kedua sisi karena itu
 * dapat tetap kompilasi dan tetap berjalan meskipun salah satunya tertinggal
 * belum disesuaikan. Kekeliruan arah akan menambah stok pada dokumen yang
 * seharusnya mengurangi (atau sebaliknya); kekeliruan nama tabel atau kolom
 * akan menyentuh baris milik dokumen jenis lain — dan karena PK kedua tabel
 * berjalan pada sequence terpisah, ID yang kebetulan cocok membuatnya diam-diam
 * berhasil alih-alih gagal. Kedua jenis kekeliruan itu hanya akan terlihat
 * sebagai selisih stok yang tak terjelaskan, jauh setelah kejadiannya.
 * </p>
 *
 * <h2>{@code stok} dan {@code stokmenjadi} adalah REKAMAN, bukan penjaga</h2>
 * <p>
 * Sama seperti pada {@link PemakaianItemDetail} dan
 * {@link KoreksiItemMedisDetail}, kedua kolom itu adalah potret yang dihitung
 * sekali saat baris disusun dan tidak diturunkan ulang saat dibaca. Ia bisa
 * basi bila ada transaksi lain di gudang yang sama, dan ia tidak menolak apa
 * pun.
 * </p>
 * <p>
 * Perlu dicatat bahwa pada dokumen retur, arah pemeriksaannya BERBEDA dari
 * pada dokumen pemakaian. Karena baris ini menambah stok, {@code stokmenjadi}
 * di sini tidak akan menjadi negatif dan penjaga "stok tidak boleh minus"
 * memang tidak relevan. Yang relevan justru penjaga jenis lain: batas bahwa
 * yang dikembalikan tidak melebihi yang pernah dipakai. Penjaga itulah yang
 * TIDAK dapat ditulis pada struktur ini, karena — berbeda dari
 * {@link PenerimaanOrderKembaliDetail} yang menyimpan tautan ke baris
 * penerimaan asalnya — baris retur pemakaian ini TIDAK memiliki tautan apa pun
 * ke {@link PemakaianItemDetail} yang menjadi asal barangnya. Tanpa tautan itu,
 * tidak ada nilai pembanding yang dapat dijangkau, dan penambahan stok lewat
 * dokumen ini tidak terikat pada batas apa pun.
 * </p>
 * <p>
 * Entitas ini tidak memiliki kolom {@code satuanItem}. Seluruh relasinya
 * OPSIONAL ({@code nullable = true}), termasuk
 * {@link #getPemakaianReturItem()}, sehingga baris detail yatim sah secara
 * skema.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "pemakaian_retur_item_detail")
public class PemakaianReturItemDetail extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java untuk entitas ini, dipatok agar object
	 * yang sudah terserialisasi tetap kompatibel meski struktur kelas berubah.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String olehId;

	/**
	 * Mengambil ID pengguna yang terakhir mengubah baris retur pemakaian ini.
	 * Field audit shadow yang diisi lewat {@link #setOlehId(String)}.
	 *
	 * @return ID pengguna terakhir yang mengubah, atau {@code null} jika belum
	 *         pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna yang mengubah baris retur pemakaian ini. Nilai
	 * kosong/blank SENGAJA diabaikan (early return) agar field audit ini tidak
	 * pernah ditimpa jadi kosong — pola field audit shadow yang merupakan
	 * KEHARUSAN TEKNIS berulang di paket model AIS, bukan bug.
	 *
	 * @param olehId ID pengguna; nilai {@code null} atau string
	 *               kosong/whitespace-saja tidak akan mengubah nilai tersimpan.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	private String oleh;

	/**
	 * Representasi ringkas baris retur pemakaian ini untuk tampilan/log, berupa
	 * {@link #getItem()} yang dirangkai jadi teks. Konkatenasi dengan
	 * {@code ""} dipakai agar item yang belum diisi menghasilkan teks
	 * {@code "null"} alih-alih melempar {@link NullPointerException}.
	 *
	 * @return teks representasi item pada baris ini.
	 */
	public String toString() {
		return item + "";
	}

	/**
	 * Menetapkan nama pengguna yang mengubah baris retur pemakaian ini. Nilai
	 * kosong/blank diabaikan agar field audit ini tidak pernah ditimpa kosong.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau kosong/whitespace-saja
	 *             tidak akan mengubah nilai tersimpan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah baris retur pemakaian ini.
	 *
	 * @return nama pengguna terakhir yang mengubah, atau {@code null} jika
	 *         belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate} yang memperbarui {@link #tanggal_dirubah}
	 * otomatis sebelum baris ini di-UPDATE, lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = new Date();

	/**
	 * Menetapkan timestamp perubahan terakhir baris ini secara manual.
	 *
	 * @param tanggal_dirubah timestamp perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil timestamp perubahan terakhir baris ini, diperbarui otomatis
	 * oleh {@link #onUpdate()} setiap kali baris di-UPDATE.
	 *
	 * @return timestamp perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	private ItemMedis item;
	private Double jumlah;
	private Double harga;
	private PemakaianReturItem pemakaianReturItem;
	private String keterangan;
	private Double stok;
	private Double stokmenjadi;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public PemakaianReturItemDetail() {
	}

	/**
	 * Primary key baris retur pemakaian ini, auto-increment (IDENTITY) dan
	 * diisi database.
	 *
	 * @return ID unik baris retur pemakaian ini, atau {@code null} untuk baris
	 *         yang belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID baris retur pemakaian ini.
	 *
	 * @param id ID baris retur pemakaian.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil keterangan bebas baris retur pemakaian ini. Karena tidak ada
	 * tautan ke baris pemakaian asal, teks bebas inilah satu-satunya tempat
	 * kaitan dengan pemakaian asal dapat dicatat pada tingkat baris.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas baris retur pemakaian ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan item medis yang dikembalikan pada baris ini.
	 *
	 * @param item item medis yang dikembalikan.
	 */
	public void setItem(ItemMedis item) {
		this.item = item;
	}

	/**
	 * Mengambil item medis yang dikembalikan pada baris ini — relasi OPSIONAL
	 * ke {@link ItemMedis}. Inilah item yang stoknya BERTAMBAH di gudang
	 * {@link PemakaianReturItem#getLokasi()} saat dokumen disetujui.
	 *
	 * <p>
	 * Skema tidak memeriksa bahwa item ini pernah benar-benar dikeluarkan lewat
	 * dokumen {@link PemakaianItem} mana pun — tidak ada tautan yang
	 * memungkinkan pemeriksaan itu. Akibatnya item apa pun dapat
	 * "dikembalikan", termasuk item yang tidak pernah dipakai sama sekali,
	 * sehingga dokumen retur pemakaian dapat menambah stok item apa pun tanpa
	 * dasar.
	 * </p>
	 * <p>
	 * Getter ini memanggil {@code check(...)} milik
	 * {@link ais.database.model.GeneralValueObject} untuk meresolusi proxy lazy
	 * dan menugaskan hasilnya kembali ke field — sehingga bukan getter murni:
	 * ia bisa mengubah state object dan membuka koneksi database sendiri saat
	 * sesi Hibernate asalnya sudah tertutup.
	 * </p>
	 *
	 * @return item medis yang dikembalikan, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = true)
	public ItemMedis getItem() {
		item = check(item);
		return item;
	}

	/**
	 * Menetapkan kuantitas yang dikembalikan pada baris ini. Tidak ada
	 * penolakan nilai negatif maupun nol, dan tidak ada pembatasan terhadap
	 * kuantitas yang pernah dipakai, di level entitas.
	 *
	 * @param jumlah kuantitas yang dikembalikan.
	 */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * Mengambil kuantitas yang dikembalikan pada baris ini — angka yang
	 * MENAMBAH stok {@link #getItem()} di gudang
	 * {@link PemakaianReturItem#getLokasi()} saat dokumen induk disetujui.
	 *
	 * <p>
	 * Skema tidak memasang batas apa pun pada angka ini, dan yang khas untuk
	 * jalur ini: batas yang seharusnya berlaku — bahwa yang dikembalikan tidak
	 * melebihi yang pernah dipakai — TIDAK dapat ditegakkan pada struktur ini,
	 * karena baris ini tidak menyimpan tautan apa pun ke
	 * {@link PemakaianItemDetail} yang menjadi asal barangnya. Bandingkan
	 * dengan {@link PenerimaanOrderKembaliDetail#getJumlah()} pada jalur
	 * pembelian, yang setidaknya punya tautan
	 * {@link PenerimaanOrderKembaliDetail#getPenerimaanOrderDetail()} sehingga
	 * penjaganya bisa ditulis meskipun belum dipasang. Di sini bahan
	 * penjaganya sendiri tidak tersedia.
	 * </p>
	 * <p>
	 * Konsekuensinya dokumen retur pemakaian merupakan jalur penambahan stok
	 * yang tidak terikat pada apa pun: kuantitas berapa pun, atas item apa pun,
	 * ke gudang mana pun. Kewenangan itu setara dengan
	 * {@link KoreksiItemMedis}, namun tanpa bentuk dokumen yang menandakannya
	 * sebagai koreksi — sehingga pengawasan yang lazim diarahkan pada dokumen
	 * koreksi belum tentu ikut menjangkau dokumen ini.
	 * </p>
	 * <p>
	 * Nilai NEGATIF perlu diwaspadai: ia akan menjadi penambahan atas angka
	 * negatif, yaitu PENGURANGAN stok lewat dokumen yang bentuknya
	 * pengembalian barang. Nilainya bisa {@code null} karena tidak di-default;
	 * pemanggil yang memakainya untuk mutasi stok WAJIB menangani {@code null}
	 * agar tidak melempar {@link NullPointerException} saat auto-unboxing.
	 * Karena entitas ini tidak punya kolom satuan, angka ini hanya bermakna
	 * bila disepakati mengacu pada satuan dasar item.
	 * </p>
	 *
	 * @return kuantitas yang dikembalikan, atau {@code null} bila belum diisi.
	 */
	public Double getJumlah() {
		return jumlah;
	}

	/**
	 * Menetapkan dokumen retur pemakaian induk baris ini.
	 *
	 * @param pemakaianReturItem dokumen retur pemakaian induk.
	 */
	public void setPemakaianReturItem(PemakaianReturItem pemakaianReturItem) {
		this.pemakaianReturItem = pemakaianReturItem;
	}

	/**
	 * Mengambil dokumen {@link PemakaianReturItem} yang menjadi induk
	 * struktural baris ini, dipetakan ke kolom {@code pemakaian_retur_item}.
	 * Lewat induk inilah baris ini memperoleh gudang yang stoknya bertambah dan
	 * status persetujuan yang menentukan apakah pengembalian sudah terjadi.
	 *
	 * <p>
	 * PERHATIKAN nama-namanya dengan saksama. Kolom induk di sini bernama
	 * {@code pemakaian_retur_item}, sedangkan padanannya di
	 * {@link PemakaianItemDetail} bernama {@code pemakaian_item}; tabel entitas
	 * ini bernama {@code sirs.pemakaian_retur_item_detail}, sedangkan
	 * padanannya {@code sirs.pemakaian_item_detail}. Perbedaannya hanya satu
	 * kata, dan seluruhnya hidup di dalam string SQL sehingga kompilator tidak
	 * dapat menolong. Query mentah yang memakai pasangan nama milik jenis
	 * dokumen yang keliru tidak akan gagal dengan jelas: karena PK kedua tabel
	 * induk berjalan pada sequence masing-masing, ID dokumen retur dapat
	 * kebetulan cocok dengan ID dokumen pemakaian yang berbeda, sehingga query
	 * akan diam-diam menyentuh baris milik dokumen lain — menghapus atau
	 * mengubah mutasi stok yang sah tanpa jejak.
	 * </p>
	 * <p>
	 * Relasi OPSIONAL ({@code nullable = true}) walaupun secara bisnis wajib,
	 * sehingga baris detail yatim tetap sah secara skema dan tidak akan pernah
	 * ikut bergerak saat dokumen mana pun disetujui.
	 * </p>
	 *
	 * @return dokumen retur pemakaian induk, atau {@code null} bila baris ini
	 *         yatim.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pemakaian_retur_item", nullable = true)
	public PemakaianReturItem getPemakaianReturItem() {
		return pemakaianReturItem;
	}

	/**
	 * Menetapkan harga satuan item pada baris retur pemakaian ini.
	 *
	 * @param harga harga satuan item.
	 */
	public void setHarga(Double harga) {
		this.harga = harga;
	}

	/**
	 * Mengambil harga satuan item pada baris retur pemakaian ini — dasar
	 * penilaian rupiah dari barang yang dikembalikan, dan karenanya besarnya
	 * beban yang DIKURANGKAN kembali.
	 *
	 * <p>
	 * Skema tidak memeriksa bahwa angka ini sama dengan harga yang dulu dipakai
	 * saat barangnya dikeluarkan lewat {@link PemakaianItemDetail#getHarga()} —
	 * memang tidak bisa, karena tidak ada tautan ke baris pemakaian asal.
	 * Akibatnya nilai yang dikembalikan dapat berbeda dari nilai yang dulu
	 * dibebankan, sehingga pasangan pemakaian dan returnya tidak selalu saling
	 * meniadakan pada pembukuan meskipun kuantitasnya sama persis.
	 * </p>
	 *
	 * @return harga satuan item, atau {@code null} bila belum diisi.
	 */
	public Double getHarga() {
		return harga;
	}

	/**
	 * Menetapkan potret stok sebelum pengembalian pada baris ini.
	 *
	 * @param stok angka stok sebelum pengembalian.
	 */
	public void setStok(Double stok) {
		this.stok = stok;
	}

	/**
	 * Mengambil POTRET stok item ini di gudang yang bersangkutan pada saat
	 * baris retur disusun — sebuah rekaman keadaan untuk membantu pembaca
	 * dokumen, BUKAN sumber kebenaran stok. Sifatnya sebagai potret berarti
	 * angka ini bisa BASI bila ada transaksi lain di gudang yang sama antara
	 * saat baris disusun dan saat dokumen disetujui.
	 *
	 * @return potret stok sebelum pengembalian, atau {@code null} bila tidak
	 *         direkam.
	 */
	public Double getStok() {
		return stok;
	}

	/**
	 * Menetapkan potret stok setelah pengembalian pada baris ini.
	 *
	 * @param stokmenjadi angka stok setelah pengembalian.
	 */
	public void setStokmenjadi(Double stokmenjadi) {
		this.stokmenjadi = stokmenjadi;
	}

	/**
	 * Mengambil POTRET stok yang diperkirakan setelah pengembalian ini
	 * diterapkan — secara logis merupakan {@link #getStok()} DITAMBAH
	 * {@link #getJumlah()}, kebalikan dari
	 * {@link PemakaianItemDetail#getStokmenjadi()} yang menguranginya. Nilai
	 * TERSIMPAN yang dihitung sekali saat baris disusun dan tidak diturunkan
	 * ulang saat dibaca.
	 *
	 * <p>
	 * Karena arahnya menambah, kolom ini tidak akan menjadi negatif dalam
	 * pemakaian normal, sehingga penjaga "stok tidak boleh minus" memang tidak
	 * relevan di sini. Justru karena itu kolom ini TIDAK boleh dipakai sebagai
	 * pengganti pemeriksaan yang benar-benar relevan bagi dokumen retur, yaitu
	 * batas bahwa yang dikembalikan tidak melebihi yang pernah dipakai —
	 * pemeriksaan yang tidak dapat ditulis pada struktur ini karena tiadanya
	 * tautan ke baris pemakaian asal.
	 * </p>
	 * <p>
	 * Perlu diperhatikan pula bahwa tanda perhitungan kolom ini adalah satu
	 * dari sedikit hal yang membedakan entitas ini dari kembarannya
	 * {@link PemakaianItemDetail}. Bila logika perhitungannya disalin antar
	 * kedua sisi tanpa membalik tandanya, hasilnya berupa potret yang
	 * berlawanan dengan mutasi yang sebenarnya tertulis — dokumen akan
	 * menampilkan angka yang tampak wajar sementara stok bergerak ke arah lain.
	 * </p>
	 *
	 * @return potret stok setelah pengembalian, atau {@code null} bila tidak
	 *         direkam.
	 */
	public Double getStokmenjadi() {
		return stokmenjadi;
	}

}
