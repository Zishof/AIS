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
import ais.database.model.Tbmuser;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.asset.Lokasi;

/**
 * Entitas <b>Penerimaan Order</b> (goods receipt) item medis pada schema
 * {@code sirs} (tabel {@code penerimaan_order}). Merupakan dokumen HEADER
 * tahap KETIGA alur pengadaan: barang yang dipesan lewat
 * {@link PesananPembelian} benar-benar datang, diterima di sebuah
 * {@link Lokasi}, dan menambah stok. Baris-baris itemnya ada di
 * {@link PenerimaanOrderDetail}.
 *
 * <h2>Posisi dalam alur pengadaan item medis</h2>
 * <pre>
 * PermintaanPembelian        --&gt; PesananPembelian        --&gt; PenerimaanOrder        --&gt; PenerimaanOrderKembali
 * PermintaanPembelianDetail  --&gt; PesananPembelianDetail  --&gt; PenerimaanOrderDetail  --&gt; PenerimaanOrderKembaliDetail
 * </pre>
 * <p>
 * Berbeda dengan tautan-tautan lain di rantai ini yang serba opsional,
 * {@link #getPesananPembelian()} bersifat <b>WAJIB</b>
 * ({@code nullable = false}). Artinya di modul {@code sirs} tidak ada
 * penerimaan barang tanpa PO — setiap barang yang masuk harus punya dasar
 * pemesanan. Ini satu-satunya titik di seluruh rantai yang benar-benar
 * ditegakkan oleh constraint database.
 * </p>
 *
 * <h2>Dokumen inilah yang menggerakkan stok dan uang</h2>
 * <p>
 * Dua hal membuat entitas ini paling sensitif di klaster pengadaan medis:
 * </p>
 * <ul>
 *   <li><b>Stok</b> — penerimaan yang disetujui menuliskan baris mutasi stok
 *       (bertanda menambah) untuk setiap barisnya.</li>
 *   <li><b>Uang</b> — entitas ini menyimpan ringkasan nilai dokumen
 *       ({@link #getTotal()}, {@link #getDiskon()},
 *       {@link #getTotalSetelahDiskon()}, {@link #getPajak()},
 *       {@link #getTotalSetelahPPN()}) dan satu-satunya relasi ke
 *       {@link PostingHistory} di seluruh rantai pengadaan {@code sirs}.</li>
 * </ul>
 *
 * <h2>Catatan integritas penting</h2>
 * <p>
 * <b>Tidak ada penjaga anti-lebih-terima di level skema.</b> Kuantitas
 * penerimaan seluruhnya ada di {@link PenerimaanOrderDetail#getJumlah()} dan
 * {@link PenerimaanOrderDetail#getJumlahBonus()}, sementara plafon
 * pemesanannya ada di {@link PesananPembelianDetail#getJumlah()}. Tautan antar
 * keduanya MEMANG tersedia lewat
 * {@link PenerimaanOrderDetail#getPesananPembelianDetail()}, sehingga penjaga
 * "jumlah diterima kumulatif tidak melebihi jumlah dipesan" secara teknis bisa
 * ditulis — tetapi ia harus mengagregasi seluruh dokumen penerimaan yang
 * menunjuk baris PO yang sama, karena satu PO boleh diterima bertahap lewat
 * beberapa dokumen. Perbandingan satu dokumen penerimaan saja terhadap PO-nya
 * adalah pemeriksaan yang bisa ditembus dengan memecah penerimaan.
 * </p>
 * <p>
 * <b>Kolom-kolom nilai tidak diturunkan otomatis.</b>
 * {@link #getTotal()}, {@link #getTotalSetelahDiskon()} dan
 * {@link #getTotalSetelahPPN()} adalah nilai TERSIMPAN yang dihitung dan
 * dituliskan oleh lapisan action, bukan hasil perhitungan dari baris detail
 * pada saat dibaca. Bila baris detail berubah setelah ringkasan ini terisi,
 * skema tidak akan menyelaraskannya dan tidak akan mengeluh — ringkasan bisa
 * menyimpang dari jumlah barisnya tanpa terdeteksi.
 * </p>
 * <p>
 * <b>Status dokumen tersebar di banyak timestamp.</b> Entitas ini punya
 * {@link #getTanggalDisetujui()} DAN {@link #getTanggalPersetujuan()} — dua
 * kolom berbeda yang sama-sama bermakna "disetujui" — di samping
 * {@link #getTanggalPembatalan()} dan {@link #getTanggalDatang()}. Duplikasi
 * ini adalah warisan skema; kode pemanggil harus tahu persis kolom mana yang
 * benar-benar diisi alur kerjanya, karena memeriksa kolom yang salah akan
 * menghasilkan penilaian status yang keliru.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "penerimaan_order")
public class PenerimaanOrder extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java untuk entitas ini, dipatok agar object
	 * yang sudah terserialisasi tetap kompatibel meski struktur kelas berubah.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private Long index;
	private String olehId;

	/**
	 * Mengambil ID pengguna yang terakhir mengubah dokumen penerimaan ini.
	 * Field audit shadow yang diisi lewat {@link #setOlehId(String)}.
	 *
	 * @return ID pengguna terakhir yang mengubah, atau {@code null} jika belum
	 *         pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna yang mengubah dokumen penerimaan ini. Nilai
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
	 * Representasi ringkas dokumen penerimaan ini untuk tampilan
	 * combobox/listbox ZK dan log, memakai {@link #getKode()} sebagai label.
	 * Akan mengembalikan {@code null} bila kode belum di-generate.
	 *
	 * @return kode dokumen penerimaan ini.
	 */
	public String toString() {
		return kode;
	}

	/**
	 * Menetapkan nama pengguna yang mengubah dokumen penerimaan ini. Nilai
	 * kosong/blank diabaikan agar field audit ini tidak pernah ditimpa kosong.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau kosong/whitespace-saja
	 *             tidak akan mengubah nilai tersimpan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah dokumen penerimaan ini.
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
	 * Menetapkan timestamp perubahan terakhir dokumen ini secara manual.
	 *
	 * @param tanggal_dirubah timestamp perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil timestamp perubahan terakhir dokumen ini, diperbarui otomatis
	 * oleh {@link #onUpdate()} setiap kali baris di-UPDATE.
	 *
	 * @return timestamp perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	private String kode;
	private String keterangan;
	private Date tanggalPembuatan;
	private Date tanggalDisetujui;
	private Date tanggalDatang;
	private Tbmuser dibuatOleh;
	private Tbmuser disetujuiOleh;
	private Double pajak;
	private Double diskon;
	private PesananPembelian pesananPembelian;
	private Lokasi lokasi;
	private Double totalSetelahPPN;
	private Double total;
	private Double totalSetelahDiskon;
	private PajakMedis jenisPajak;

	private Date tanggalPersetujuan;
	private Date tanggalPembatalan;
	private Tbmuser dibatalkanOleh;

	private String noref;
	private Date dateref;

	private JenisBiayaLain jenisBiayaLain;
	private PostingHistory postingHistory;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public PenerimaanOrder() {
	}

	/**
	 * Primary key dokumen penerimaan ini, auto-increment (IDENTITY) dan diisi
	 * database.
	 *
	 * @return ID unik dokumen penerimaan ini, atau {@code null} untuk baris
	 *         yang belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID dokumen penerimaan ini.
	 *
	 * @param id ID dokumen penerimaan.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil kode/nomor dokumen penerimaan ini. Kolom {@code NOT NULL} dan
	 * {@code UNIQUE} di database sehingga berperan sebagai identitas bisnis
	 * dokumen. Kode inilah yang menjadi rujukan pada laporan penerimaan barang
	 * dan pada dokumen retur yang mengacu balik ke penerimaan ini.
	 *
	 * @return kode dokumen penerimaan, atau {@code null} bila belum
	 *         di-generate.
	 */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return this.kode;
	}

	/**
	 * Menetapkan kode/nomor dokumen penerimaan ini. Tidak ada validasi format
	 * maupun pengecekan keunikan di level entitas — keunikan ditegakkan oleh
	 * constraint {@code UNIQUE} database.
	 *
	 * @param kode kode dokumen penerimaan.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengambil keterangan bebas dokumen penerimaan ini.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas dokumen penerimaan ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan pengguna pembuat dokumen penerimaan ini.
	 *
	 * @param dibuatOleh pengguna pembuat dokumen.
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengambil pengguna pembuat dokumen penerimaan ini — relasi OPSIONAL
	 * ({@code nullable = true}), berbeda dari {@link PesananPembelian} dan
	 * {@link PermintaanPembelian} yang mewajibkannya. Dokumen penerimaan tanpa
	 * pembuat teridentifikasi karena itu sah secara skema, yang melemahkan
	 * jejak audit justru pada dokumen yang menggerakkan stok dan nilai uang.
	 *
	 * <p>
	 * Getter ini memanggil {@code check(...)} milik
	 * {@link ais.database.model.GeneralValueObject} untuk meresolusi proxy lazy
	 * sebelum mengembalikan nilai, dan hasilnya ditugaskan kembali ke field.
	 * Efek sampingnya, getter ini TIDAK murni: ia bisa mengubah state object
	 * dan membuka koneksi database sendiri saat sesi Hibernate asalnya sudah
	 * tertutup.
	 * </p>
	 *
	 * @return pengguna pembuat dokumen, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = true)
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);
		return dibuatOleh;
	}

	/**
	 * Menetapkan pengguna yang menyetujui dokumen penerimaan ini.
	 *
	 * @param disetujuiOleh pengguna penyetuju dokumen.
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengambil pengguna yang menyetujui dokumen penerimaan ini — relasi
	 * OPSIONAL, kosong selama penerimaan belum disetujui. Persetujuan di sini
	 * bukan formalitas: persetujuanlah yang memicu penulisan mutasi stok,
	 * sehingga terisinya kolom ini menandai titik saat dokumen mulai
	 * berdampak pada persediaan.
	 *
	 * <p>
	 * Entitas TIDAK memaksakan bahwa penyetuju berbeda dari
	 * {@link #getDibuatOleh()}; pemisahan wewenang sepenuhnya tanggung jawab
	 * lapisan action. Getter ini juga memanggil {@code check(...)} sehingga
	 * bukan getter murni (lihat {@link #getDibuatOleh()}).
	 * </p>
	 *
	 * @return pengguna penyetuju, atau {@code null} bila belum disetujui.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujuiOleh = check(disetujuiOleh);
		return disetujuiOleh;
	}

	/**
	 * Menetapkan tanggal pembuatan dokumen penerimaan ini.
	 *
	 * @param tanggalPembuatan timestamp pembuatan dokumen.
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Mengambil tanggal pembuatan dokumen penerimaan ini. Diisi sekali oleh
	 * lapisan action dan tidak berubah lagi.
	 *
	 * @return timestamp pembuatan dokumen, atau {@code null} bila belum diisi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		return tanggalPembuatan;
	}

	/**
	 * Menetapkan tanggal kedatangan fisik barang.
	 *
	 * @param tanggalDatang timestamp kedatangan barang.
	 */
	public void setTanggalDatang(Date tanggalDatang) {
		this.tanggalDatang = tanggalDatang;
	}

	/**
	 * Mengambil tanggal kedatangan FISIK barang — berbeda konsep dari
	 * {@link #getTanggalPembuatan()} (kapan dokumennya dibuat) maupun dari
	 * tanggal persetujuan (kapan dokumennya sah dan stok bergerak). Ketiganya
	 * bisa berbeda jauh, dan skema tidak memaksakan urutan kronologis apa pun
	 * di antara mereka: dokumen dengan tanggal datang mendahului tanggal
	 * pembuatan, atau sebaliknya, sama-sama akan tersimpan tanpa keluhan.
	 *
	 * @return timestamp kedatangan barang, atau {@code null} bila belum diisi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_datang")
	public Date getTanggalDatang() {
		return tanggalDatang;
	}

	/**
	 * Menetapkan dokumen PO yang menjadi dasar penerimaan ini.
	 *
	 * @param pesananPembelian dokumen PO dasar.
	 */
	public void setPesananPembelian(PesananPembelian pesananPembelian) {
		this.pesananPembelian = pesananPembelian;
	}

	/**
	 * Mengambil dokumen {@link PesananPembelian} yang menjadi dasar penerimaan
	 * ini — relasi <b>WAJIB</b> ({@code nullable = false}), satu-satunya
	 * tautan di seluruh rantai pengadaan {@code sirs} yang benar-benar
	 * ditegakkan oleh constraint database. Karenanya di modul ini tidak ada
	 * penerimaan barang medis tanpa dasar pemesanan.
	 *
	 * <p>
	 * Perlu ditekankan bahwa yang dijamin hanyalah KEBERADAAN tautannya, bukan
	 * kesesuaian isinya. Skema tidak memeriksa apakah item pada
	 * baris-baris {@link PenerimaanOrderDetail} benar-benar termasuk dalam
	 * PO yang ditunjuk di sini, tidak memeriksa apakah kuantitas yang diterima
	 * masih dalam batas yang dipesan, dan tidak memeriksa apakah PO tersebut
	 * sudah disetujui atau justru sudah dibatalkan lewat
	 * {@link PesananPembelian#getTanggalPembatalan()}. Ketiga pemeriksaan itu
	 * harus dilakukan lapisan action sebelum penerimaan disetujui.
	 * </p>
	 * <p>
	 * Tautan tingkat header ini juga tidak dijaga konsisten dengan tautan
	 * tingkat baris di
	 * {@link PenerimaanOrderDetail#getPesananPembelianDetail()}: secara skema
	 * sebuah dokumen penerimaan bisa menunjuk PO tertentu di header sementara
	 * sebagian barisnya menunjuk baris milik PO yang berbeda.
	 * </p>
	 *
	 * @return dokumen PO dasar penerimaan ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pesanan_pembelian", nullable = false)
	public PesananPembelian getPesananPembelian() {
		return pesananPembelian;
	}

	/**
	 * Menetapkan nilai pajak dokumen penerimaan ini.
	 *
	 * @param pajak nilai pajak.
	 */
	public void setPajak(Double pajak) {
		this.pajak = pajak;
	}

	/**
	 * Mengambil nilai pajak dokumen penerimaan ini. Angka ini TERSIMPAN, bukan
	 * dihitung ulang saat dibaca, dan tidak terikat secara otomatis pada
	 * {@link #getJenisPajak()} — mengganti jenis pajak tidak akan menyesuaikan
	 * nilai di sini dengan sendirinya.
	 *
	 * @return nilai pajak, atau {@code null} bila belum diisi.
	 */
	public Double getPajak() {
		return pajak;
	}

	/**
	 * Menetapkan total nilai dokumen setelah PPN.
	 *
	 * @param totalSetelahPPN total setelah PPN.
	 */
	public void setTotalSetelahPPN(Double totalSetelahPPN) {
		this.totalSetelahPPN = totalSetelahPPN;
	}

	/**
	 * Mengambil total nilai dokumen setelah PPN — nilai akhir yang menjadi
	 * dasar tagihan vendor. Merupakan nilai TERSIMPAN yang dituliskan lapisan
	 * action, bukan hasil perhitungan otomatis dari
	 * {@link #getTotalSetelahDiskon()} ditambah {@link #getPajak()}. Skema
	 * tidak menjamin ketiga angka ringkasan itu konsisten satu sama lain
	 * maupun konsisten dengan jumlah baris detailnya.
	 *
	 * @return total setelah PPN, atau {@code null} bila belum dihitung.
	 */
	@Column(name = "total_setelah_ppn", nullable = true)
	public Double getTotalSetelahPPN() {
		return totalSetelahPPN;
	}

	/**
	 * Menetapkan total nilai dokumen sebelum diskon dan pajak.
	 *
	 * @param total total bruto dokumen.
	 */
	public void setTotal(Double total) {
		this.total = total;
	}

	/**
	 * Mengambil total nilai dokumen sebelum diskon dan pajak (bruto). Nilai
	 * TERSIMPAN yang seharusnya merupakan penjumlahan
	 * {@code jumlah x hargaBeli} seluruh {@link PenerimaanOrderDetail} milik
	 * dokumen ini, namun tidak dijamin demikian oleh skema: bila baris detail
	 * berubah setelah angka ini terisi, keduanya akan menyimpang diam-diam.
	 *
	 * @return total bruto dokumen, atau {@code null} bila belum dihitung.
	 */
	public Double getTotal() {
		return total;
	}

	/**
	 * Menetapkan nilai diskon dokumen penerimaan ini.
	 *
	 * @param diskon nilai diskon.
	 */
	public void setDiskon(Double diskon) {
		this.diskon = diskon;
	}

	/**
	 * Mengambil nilai diskon tingkat DOKUMEN. Perlu dibedakan dari
	 * {@link PenerimaanOrderDetail#getHargaDiskon()} yang merupakan diskon
	 * tingkat BARIS. Keduanya bisa terisi bersamaan dan skema tidak
	 * mendefinisikan hubungan di antara keduanya — apakah diskon dokumen sudah
	 * mencakup diskon baris atau justru tambahan di atasnya adalah kesepakatan
	 * yang hidup di lapisan action, bukan di model.
	 *
	 * @return nilai diskon dokumen, atau {@code null} bila tidak ada diskon.
	 */
	public Double getDiskon() {
		return diskon;
	}

	/**
	 * Menetapkan total nilai dokumen setelah diskon.
	 *
	 * @param totalSetelahDiskon total setelah diskon.
	 */
	public void setTotalSetelahDiskon(Double totalSetelahDiskon) {
		this.totalSetelahDiskon = totalSetelahDiskon;
	}

	/**
	 * Mengambil total nilai dokumen setelah diskon, tahap antara
	 * {@link #getTotal()} dan {@link #getTotalSetelahPPN()}. Sama seperti dua
	 * angka ringkasan lainnya, ini nilai TERSIMPAN yang tidak dihitung ulang
	 * saat dibaca.
	 *
	 * @return total setelah diskon, atau {@code null} bila belum dihitung.
	 */
	@Column(name = "total_setelah_diskon", nullable = true)
	public Double getTotalSetelahDiskon() {
		return totalSetelahDiskon;
	}

	/**
	 * Menetapkan tanggal persetujuan dokumen (kolom {@code tanggal_disetujui}).
	 *
	 * @param tanggalDisetujui timestamp persetujuan.
	 */
	public void setTanggalDisetujui(Date tanggalDisetujui) {
		this.tanggalDisetujui = tanggalDisetujui;
	}

	/**
	 * Mengambil tanggal persetujuan dokumen dari kolom
	 * {@code tanggal_disetujui}.
	 *
	 * <p>
	 * PERHATIAN: entitas ini punya DUA kolom bermakna persetujuan —
	 * {@code tanggal_disetujui} (getter ini) dan {@code tanggal_persetujuan}
	 * ({@link #getTanggalPersetujuan()}). Keduanya berdiri sendiri dan tidak
	 * disinkronkan oleh skema maupun oleh entitas ini. Kode yang menilai
	 * status dokumen WAJIB memastikan ia membaca kolom yang benar-benar diisi
	 * oleh alur kerja yang bersangkutan; memeriksa kolom yang salah akan
	 * membuat dokumen yang sudah disetujui tampak masih draft, atau sebaliknya.
	 * Bila menulis kode baru yang bergantung pada status, periksa KEDUANYA.
	 * </p>
	 *
	 * @return timestamp persetujuan versi kolom {@code tanggal_disetujui},
	 *         atau {@code null} bila kolom itu tidak diisi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_disetujui")
	public Date getTanggalDisetujui() {
		return tanggalDisetujui;
	}

	/**
	 * Menetapkan jenis pajak yang dikenakan pada dokumen penerimaan ini.
	 *
	 * @param jenisPajak jenis pajak.
	 */
	public void setJenisPajak(PajakMedis jenisPajak) {
		this.jenisPajak = jenisPajak;
	}

	/**
	 * Mengambil jenis pajak yang dikenakan pada dokumen ini — relasi OPSIONAL
	 * ke {@link PajakMedis}. Berfungsi sebagai penanda klasifikasi; nilai
	 * pajak yang benar-benar dipakai tersimpan terpisah di
	 * {@link #getPajak()} dan tidak dihitung ulang dari tarif jenis pajak ini
	 * saat dibaca. Akibatnya, perubahan tarif pada master {@link PajakMedis}
	 * di kemudian hari TIDAK akan mengubah nilai pajak dokumen lama — perilaku
	 * yang justru diinginkan untuk dokumen historis.
	 *
	 * <p>
	 * Getter ini memanggil {@code check(...)} sehingga bukan getter murni
	 * (lihat {@link #getDibuatOleh()}).
	 * </p>
	 *
	 * @return jenis pajak, atau {@code null} bila tidak diklasifikasikan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pajak", nullable = true)
	public PajakMedis getJenisPajak() {
		jenisPajak = check(jenisPajak);
		return jenisPajak;
	}

	/**
	 * Menetapkan tanggal persetujuan dokumen (kolom
	 * {@code tanggal_persetujuan}).
	 *
	 * @param tanggalPersetujuan timestamp persetujuan.
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengambil tanggal persetujuan dokumen dari kolom
	 * {@code tanggal_persetujuan} — kolom persetujuan KEDUA pada entitas ini,
	 * berdampingan dengan {@link #getTanggalDisetujui()}. Lihat peringatan
	 * lengkap tentang duplikasi ini pada {@link #getTanggalDisetujui()}.
	 *
	 * <p>
	 * Sama seperti pada {@link PesananPembelian}, terisinya kolom ini tidak
	 * cukup untuk menyatakan dokumen aktif — {@link #getTanggalPembatalan()}
	 * harus ikut diperiksa karena skema mengizinkan keduanya terisi bersamaan.
	 * </p>
	 *
	 * @return timestamp persetujuan versi kolom {@code tanggal_persetujuan},
	 *         atau {@code null} bila kolom itu tidak diisi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {
		return tanggalPersetujuan;
	}

	/**
	 * Menetapkan tanggal pembatalan dokumen penerimaan ini.
	 *
	 * @param tanggalPembatalan timestamp pembatalan.
	 */
	public void setTanggalPembatalan(Date tanggalPembatalan) {
		this.tanggalPembatalan = tanggalPembatalan;
	}

	/**
	 * Mengambil tanggal pembatalan dokumen penerimaan ini. Pembatalan bersifat
	 * penandaan (soft): baris dokumen dan detailnya tetap ada sehingga jejak
	 * audit terjaga. Karena persetujuan dokumen ini menggerakkan stok,
	 * pembatalannya harus disertai pembalikan mutasi stok yang sudah
	 * dituliskan — dan pembalikan itu sepenuhnya urusan lapisan action; entitas
	 * ini hanya menyimpan penandanya.
	 *
	 * @return timestamp pembatalan, atau {@code null} bila belum dibatalkan.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembatalan")
	public Date getTanggalPembatalan() {
		return tanggalPembatalan;
	}

	/**
	 * Menetapkan pengguna yang membatalkan dokumen penerimaan ini.
	 *
	 * @param dibatalkanOleh pengguna pembatal dokumen.
	 */
	public void setDibatalkanOleh(Tbmuser dibatalkanOleh) {
		this.dibatalkanOleh = dibatalkanOleh;
	}

	/**
	 * Mengambil pengguna yang membatalkan dokumen penerimaan ini — relasi
	 * OPSIONAL, pasangan dari {@link #getTanggalPembatalan()}. Getter ini
	 * memanggil {@code check(...)} sehingga bukan getter murni (lihat
	 * {@link #getDibuatOleh()}).
	 *
	 * @return pengguna pembatal, atau {@code null} bila belum dibatalkan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibatalkan_oleh", nullable = true)
	public Tbmuser getDibatalkanOleh() {
		dibatalkanOleh = check(dibatalkanOleh);
		return dibatalkanOleh;
	}

	/**
	 * Menetapkan lokasi/gudang penerima barang.
	 *
	 * @param lokasi lokasi gudang penerima.
	 */
	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * Mengambil lokasi/gudang penerima barang — relasi ke {@link Lokasi} pada
	 * paket {@code asset}. Inilah lokasi yang stoknya BERTAMBAH saat dokumen
	 * disetujui, sekaligus satu-satunya sumbu pembatas lingkup data yang
	 * tersedia (modul {@code sirs} tidak punya sumbu tenant/satuan kerja).
	 *
	 * <p>
	 * Relasi OPSIONAL, sehingga dokumen penerimaan tanpa lokasi tetap sah
	 * secara skema. Dokumen semacam itu tidak hanya lolos dari filter berbasis
	 * lokasi, tetapi juga tidak jelas gudang mana yang stoknya bertambah —
	 * kondisi yang perlu ditolak lapisan action sebelum penerimaan disetujui.
	 * Skema juga tidak menjamin lokasi ini sama dengan
	 * {@link PesananPembelian#getLokasi()} pada PO yang menjadi dasarnya,
	 * sehingga barang bisa diterima di gudang yang berbeda dari yang dipesan
	 * tanpa jejak penyimpangan.
	 * </p>
	 * <p>
	 * Getter ini memanggil {@code check(...)} sehingga bukan getter murni
	 * (lihat {@link #getDibuatOleh()}).
	 * </p>
	 *
	 * @return lokasi gudang penerima, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi", nullable = true)
	public Lokasi getLokasi() {
		lokasi = check(lokasi);
		return lokasi;
	}

	/**
	 * Menetapkan nomor urut tampilan baris ini.
	 *
	 * @param index nomor urut tampilan.
	 */
	public void setIndex(Long index) {
		this.index = index;
	}

	/**
	 * Mengambil nomor urut tampilan baris ini. Dipakai grid/listbox ZK untuk
	 * penomoran baris; bukan bagian dari identitas dokumen.
	 *
	 * @return nomor urut tampilan, atau {@code null} bila belum diisi.
	 */
	public Long getIndex() {
		return index;
	}

	/**
	 * Menetapkan nomor referensi eksternal dokumen (mis. nomor faktur vendor).
	 *
	 * @param noref nomor referensi eksternal.
	 */
	public void setNoref(String noref) {
		this.noref = noref;
	}

	/**
	 * Mengambil nomor referensi eksternal dokumen — umumnya nomor faktur atau
	 * surat jalan dari vendor, pasangan dari {@link #getDateref()}. Berupa teks
	 * bebas tanpa constraint keunikan, sehingga nomor faktur yang sama bisa
	 * dicatat pada beberapa dokumen penerimaan tanpa terdeteksi. Bagi
	 * pengendalian pembayaran ganda ke vendor, deteksi duplikasi nomor faktur
	 * karena itu harus dilakukan di lapisan action atau saat rekonsiliasi.
	 *
	 * @return nomor referensi eksternal, atau {@code null} bila belum diisi.
	 */
	public String getNoref() {
		return noref;
	}

	/**
	 * Menetapkan tanggal dokumen referensi eksternal.
	 *
	 * @param dateref tanggal dokumen referensi.
	 */
	public void setDateref(Date dateref) {
		this.dateref = dateref;
	}

	/**
	 * Mengambil tanggal dokumen referensi eksternal (tanggal faktur/surat jalan
	 * vendor), pasangan dari {@link #getNoref()}. Berdiri sendiri dari seluruh
	 * timestamp internal dokumen ini dan tidak dibandingkan dengan satu pun di
	 * antaranya.
	 *
	 * @return tanggal dokumen referensi, atau {@code null} bila belum diisi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getDateref() {
		return dateref;
	}

	/**
	 * Mengambil jenis biaya lain yang melekat pada dokumen penerimaan ini —
	 * relasi OPSIONAL ke {@link JenisBiayaLain}, dipakai untuk
	 * mengklasifikasikan komponen biaya tambahan (mis. ongkos kirim) agar bisa
	 * dijurnalkan ke akun yang tepat. Getter ini memanggil {@code check(...)}
	 * sehingga bukan getter murni (lihat {@link #getDibuatOleh()}).
	 *
	 * @return jenis biaya lain, atau {@code null} bila tidak ada biaya tambahan
	 *         yang diklasifikasikan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_biaya_lain", nullable = true)
	public JenisBiayaLain getJenisBiayaLain() {
		jenisBiayaLain = check(jenisBiayaLain);
		return jenisBiayaLain;
	}

	/**
	 * Menetapkan jenis biaya lain yang melekat pada dokumen ini.
	 *
	 * @param jenisBiayaLain jenis biaya lain.
	 */
	public void setJenisBiayaLain(JenisBiayaLain jenisBiayaLain) {
		this.jenisBiayaLain = jenisBiayaLain;
	}

	/**
	 * Mengambil jejak posting jurnal akuntansi dokumen penerimaan ini — relasi
	 * OPSIONAL ke {@link PostingHistory}, dan SATU-SATUNYA relasi ke akuntansi
	 * di seluruh rantai pengadaan {@code sirs} (baik
	 * {@link PermintaanPembelian}, {@link PesananPembelian}, maupun
	 * {@link PenerimaanOrderKembali} tidak memilikinya). Hal ini konsisten
	 * dengan prinsipnya: permintaan dan pemesanan belum menimbulkan kewajiban
	 * akuntansi, penerimaan barang-lah yang menimbulkannya.
	 *
	 * <p>
	 * Nilai bukan-{@code null} pada kolom ini dimaksudkan sebagai penanda
	 * "dokumen sudah masuk buku besar", yaitu titik setelah mana dokumen tidak
	 * boleh lagi diubah, disetujui ulang, dibatalkan, atau dihapus — karena
	 * perubahannya akan membuat persediaan dan jurnal tidak lagi cocok.
	 * </p>
	 * <p>
	 * PERINGATAN untuk kode yang bersandar pada kolom ini sebagai penjaga:
	 * jejak posting hanya terisi bila ada kode yang memanggil
	 * {@link #setPostingHistory(PostingHistory)} atau menuliskan kolom
	 * {@code posting_history} secara langsung. Bila di suatu instalasi alur
	 * posting buku besar untuk dokumen penerimaan medis belum terpasang, kolom
	 * ini akan selalu {@code null} dan setiap penjaga yang berbentuk
	 * {@code if (getPostingHistory() != null)} tidak akan pernah aktif —
	 * penjaga yang tampak ada di kode namun tidak pernah menahan apa pun.
	 * Karena itu penjaga berbasis kolom ini sebaiknya TIDAK dijadikan
	 * satu-satunya pengaman terhadap perubahan dokumen yang sudah berdampak;
	 * pemeriksaan status persetujuan ({@link #getTanggalPersetujuan()} dan
	 * {@link #getTanggalDisetujui()}) perlu tetap dipasang berdampingan.
	 * </p>
	 *
	 * @return jejak posting jurnal, atau {@code null} bila dokumen ini belum
	 *         (atau tidak pernah) diposting ke buku besar.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * Menetapkan jejak posting jurnal akuntansi dokumen penerimaan ini.
	 * Pemanggilan method inilah yang mengaktifkan seluruh penjaga berbentuk
	 * {@code getPostingHistory() != null} di lapisan action — lihat peringatan
	 * pada {@link #getPostingHistory()}.
	 *
	 * @param postingHistory jejak posting jurnal.
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

}
