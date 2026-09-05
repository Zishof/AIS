package ais.database.model.sirs;

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
import ais.database.model.Tbmuser;
import ais.database.model.asset.Lokasi;

/**
 * Entitas <b>Penerimaan Order Kembali</b> (retur pembelian) item medis pada
 * schema {@code sirs} (tabel {@code penerimaan_order_kembali}). Merupakan
 * dokumen HEADER tahap KEEMPAT sekaligus terakhir dari alur pengadaan: barang
 * yang sudah diterima lewat {@link PenerimaanOrder} dikembalikan ke vendor
 * (rusak, salah kirim, mendekati kadaluarsa), sehingga stok BERKURANG kembali.
 * Baris-baris itemnya ada di {@link PenerimaanOrderKembaliDetail}.
 *
 * <h2>Posisi dalam alur pengadaan item medis</h2>
 * <pre>
 * PermintaanPembelian        --&gt; PesananPembelian        --&gt; PenerimaanOrder        --&gt; PenerimaanOrderKembali
 * PermintaanPembelianDetail  --&gt; PesananPembelianDetail  --&gt; PenerimaanOrderDetail  --&gt; PenerimaanOrderKembaliDetail
 * </pre>
 * <p>
 * Sama seperti {@link PenerimaanOrder} terhadap PO-nya, entitas ini menautkan
 * diri ke dokumen penerimaan asalnya lewat {@link #getPenerimaanOrder()} yang
 * bersifat <b>WAJIB</b> ({@code nullable = false}) — tidak ada retur pembelian
 * tanpa penerimaan yang menjadi dasarnya.
 * </p>
 *
 * <h2>Dokumen kebalikan yang mengurangi stok</h2>
 * <p>
 * Entitas ini adalah cerminan {@link PenerimaanOrder} dengan arah stok
 * terbalik: bila penerimaan yang disetujui MENAMBAH stok, retur yang disetujui
 * MENGURANGINYA. Karena itu seluruh kewaspadaan yang berlaku pada penerimaan
 * berlaku pula di sini, dengan tambahan satu risiko khas: retur yang melebihi
 * jumlah yang pernah diterima akan menghasilkan stok negatif atau menghapus
 * stok yang berasal dari penerimaan lain.
 * </p>
 * <p>
 * <b>Tidak ada penjaga jumlah-retur di level skema.</b> Sama seperti pada
 * hubungan penerimaan-pesanan, tautan yang dibutuhkan SUDAH tersedia — baris
 * retur menunjuk baris penerimaan asalnya lewat
 * {@link PenerimaanOrderKembaliDetail#getPenerimaanOrderDetail()} — namun
 * perbandingan {@code jumlah retur <= jumlah diterima} tidak dilakukan oleh
 * entitas maupun oleh constraint database. Penjaga yang memadai harus
 * mengagregasi seluruh retur atas baris penerimaan yang sama (satu penerimaan
 * boleh diretur bertahap), bukan sekadar membandingkan satu dokumen retur
 * terhadap penerimaannya.
 * </p>
 *
 * <h2>Perbedaan penting dari PenerimaanOrder</h2>
 * <p>
 * Entitas ini TIDAK memiliki relasi ke {@code PostingHistory}, tidak memiliki
 * kolom nilai apa pun ({@code total}, {@code diskon}, {@code pajak}), dan
 * tidak memiliki {@code noref}/{@code dateref}. Artinya dampak keuangan retur
 * — nota kredit dari vendor, koreksi nilai persediaan — tidak terekam di
 * dokumen ini sama sekali; yang terekam hanya perpindahan barangnya. Setiap
 * rekonsiliasi nilai antara barang yang diretur dan tagihan vendor karena itu
 * harus bersandar pada harga di baris penerimaan asalnya, bukan pada dokumen
 * retur ini.
 * </p>
 * <p>
 * Sama seperti {@link PenerimaanOrder}, entitas ini punya DUA kolom bermakna
 * persetujuan ({@link #getTanggalDisetujui()} dan
 * {@link #getTanggalPersetujuan()}) yang berdiri sendiri dan tidak
 * disinkronkan — lihat peringatan pada masing-masing getter.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "penerimaan_order_kembali")
public class PenerimaanOrderKembali extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java untuk entitas ini, dipatok agar object
	 * yang sudah terserialisasi tetap kompatibel meski struktur kelas berubah.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private Long index;
	private String olehId;

	/**
	 * Mengambil ID pengguna yang terakhir mengubah dokumen retur ini. Field
	 * audit shadow yang diisi lewat {@link #setOlehId(String)}.
	 *
	 * @return ID pengguna terakhir yang mengubah, atau {@code null} jika belum
	 *         pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna yang mengubah dokumen retur ini. Nilai
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
	 * Representasi ringkas dokumen retur ini untuk tampilan combobox/listbox ZK
	 * dan log, memakai {@link #getKode()} sebagai label. Akan mengembalikan
	 * {@code null} bila kode belum di-generate.
	 *
	 * @return kode dokumen retur ini.
	 */
	public String toString() {
		return kode;
	}

	/**
	 * Menetapkan nama pengguna yang mengubah dokumen retur ini. Nilai
	 * kosong/blank diabaikan agar field audit ini tidak pernah ditimpa kosong.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau kosong/whitespace-saja
	 *             tidak akan mengubah nilai tersimpan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah dokumen retur ini.
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
	private Date tanggalKembali;
	private Tbmuser dibuatOleh;
	private Tbmuser disetujuiOleh;
	private PenerimaanOrder penerimaanOrder;
	private Lokasi lokasi;

	private Date tanggalPersetujuan;
	private Date tanggalPembatalan;
	private Tbmuser dibatalkanOleh;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public PenerimaanOrderKembali() {
	}

	/**
	 * Primary key dokumen retur ini, auto-increment (IDENTITY) dan diisi
	 * database.
	 *
	 * @return ID unik dokumen retur ini, atau {@code null} untuk baris yang
	 *         belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID dokumen retur ini.
	 *
	 * @param id ID dokumen retur.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil kode/nomor dokumen retur ini. Kolom {@code NOT NULL} dan
	 * {@code UNIQUE} di database sehingga berperan sebagai identitas bisnis
	 * dokumen, di samping {@link #getId()} yang merupakan identitas teknis.
	 *
	 * <p>
	 * Perlu diketahui bahwa PK tabel ini dan PK tabel-tabel dokumen lain di
	 * schema {@code sirs} berjalan pada sequence masing-masing, sehingga
	 * dokumen retur dengan ID tertentu bisa memiliki ID yang sama dengan
	 * dokumen dari tabel lain. Kode SQL mentah yang mencocokkan ID dokumen
	 * dengan kolom milik tabel yang keliru karena itu tidak akan gagal dengan
	 * jelas — ia akan diam-diam mengenai baris milik dokumen lain. Selalu
	 * pastikan nama tabel dan nama kolom dalam query mentah cocok dengan
	 * entitas yang ID-nya sedang dipakai.
	 * </p>
	 *
	 * @return kode dokumen retur, atau {@code null} bila belum di-generate.
	 */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return this.kode;
	}

	/**
	 * Menetapkan kode/nomor dokumen retur ini. Tidak ada validasi format
	 * maupun pengecekan keunikan di level entitas — keunikan ditegakkan oleh
	 * constraint {@code UNIQUE} database.
	 *
	 * @param kode kode dokumen retur.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengambil keterangan bebas dokumen retur ini. Karena entitas tidak punya
	 * kolom terstruktur untuk ALASAN retur (rusak, salah kirim, kadaluarsa),
	 * praktiknya kolom bebas inilah satu-satunya tempat alasan tersebut
	 * dicatat — sehingga tidak dapat diandalkan untuk pelaporan atau analisis
	 * mutu vendor.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas dokumen retur ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan pengguna pembuat dokumen retur ini.
	 *
	 * @param dibuatOleh pengguna pembuat dokumen.
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengambil pengguna pembuat dokumen retur ini — relasi OPSIONAL
	 * ({@code nullable = true}), sama seperti pada {@link PenerimaanOrder} dan
	 * berbeda dari {@link PermintaanPembelian}/{@link PesananPembelian} yang
	 * mewajibkannya. Dokumen retur tanpa pembuat teridentifikasi karena itu sah
	 * secara skema, melemahkan jejak audit pada dokumen yang mengurangi stok.
	 *
	 * @return pengguna pembuat dokumen, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "dibuat_oleh", nullable = true)
	public Tbmuser getDibuatOleh() {
		return dibuatOleh;
	}

	/**
	 * Menetapkan pengguna yang menyetujui dokumen retur ini.
	 *
	 * @param disetujuiOleh pengguna penyetuju dokumen.
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengambil pengguna yang menyetujui dokumen retur ini — relasi OPSIONAL,
	 * kosong selama retur belum disetujui. Persetujuanlah yang memicu
	 * pengurangan stok, sehingga terisinya kolom ini menandai titik saat
	 * dokumen mulai berdampak pada persediaan. Entitas TIDAK memaksakan
	 * penyetuju berbeda dari {@link #getDibuatOleh()}.
	 *
	 * @return pengguna penyetuju, atau {@code null} bila belum disetujui.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		return disetujuiOleh;
	}

	/**
	 * Menetapkan tanggal pembuatan dokumen retur ini.
	 *
	 * @param tanggalPembuatan timestamp pembuatan dokumen.
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Mengambil tanggal pembuatan dokumen retur ini. Diisi sekali oleh lapisan
	 * action dan tidak berubah lagi.
	 *
	 * @return timestamp pembuatan dokumen, atau {@code null} bila belum diisi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		return tanggalPembuatan;
	}

	/**
	 * Menetapkan tanggal pengembalian fisik barang ke vendor.
	 *
	 * @param tanggalKembali timestamp pengembalian barang.
	 */
	public void setTanggalKembali(Date tanggalKembali) {
		this.tanggalKembali = tanggalKembali;
	}

	/**
	 * Mengambil tanggal pengembalian FISIK barang ke vendor — cerminan dari
	 * {@link PenerimaanOrder#getTanggalDatang()} pada arah sebaliknya. Berbeda
	 * konsep dari tanggal pembuatan dokumen maupun tanggal persetujuan, dan
	 * skema tidak memaksakan urutan kronologis apa pun di antara ketiganya.
	 * Skema juga tidak memeriksa bahwa tanggal ini berada SETELAH tanggal
	 * barang tersebut diterima, sehingga retur bertanggal mendahului
	 * penerimaannya tetap tersimpan tanpa keluhan.
	 *
	 * @return timestamp pengembalian barang, atau {@code null} bila belum
	 *         diisi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_kembali")
	public Date getTanggalKembali() {
		return tanggalKembali;
	}

	/**
	 * Menetapkan dokumen penerimaan yang menjadi dasar retur ini.
	 *
	 * @param penerimaanOrder dokumen penerimaan dasar.
	 */
	public void setPenerimaanOrder(PenerimaanOrder penerimaanOrder) {
		this.penerimaanOrder = penerimaanOrder;
	}

	/**
	 * Mengambil dokumen {@link PenerimaanOrder} yang menjadi dasar retur ini —
	 * relasi <b>WAJIB</b> ({@code nullable = false}), sehingga tidak ada retur
	 * pembelian tanpa penerimaan yang mendasarinya.
	 *
	 * <p>
	 * Seperti halnya {@link PenerimaanOrder#getPesananPembelian()}, yang
	 * dijamin hanyalah KEBERADAAN tautannya, bukan kesesuaian isinya. Skema
	 * tidak memeriksa apakah item pada baris-baris retur benar-benar termasuk
	 * dalam penerimaan yang ditunjuk di sini, tidak memeriksa apakah kuantitas
	 * yang diretur masih dalam batas yang pernah diterima, dan tidak memeriksa
	 * apakah dokumen penerimaan tersebut sudah disetujui atau justru sudah
	 * dibatalkan. Meretur barang dari penerimaan yang telah dibatalkan
	 * (sehingga stoknya sudah dikembalikan) akan mengurangi stok untuk kedua
	 * kalinya.
	 * </p>
	 * <p>
	 * Tautan tingkat header ini juga tidak dijaga konsisten dengan tautan
	 * tingkat baris di
	 * {@link PenerimaanOrderKembaliDetail#getPenerimaanOrderDetail()}.
	 * </p>
	 *
	 * @return dokumen penerimaan dasar retur ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penerimaan_order", nullable = false)
	public PenerimaanOrder getPenerimaanOrder() {
		return penerimaanOrder;
	}

	/**
	 * Menetapkan lokasi/gudang asal barang yang diretur.
	 *
	 * @param lokasi lokasi gudang asal retur.
	 */
	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * Mengambil lokasi/gudang asal barang yang diretur — inilah lokasi yang
	 * stoknya BERKURANG saat dokumen disetujui, sekaligus satu-satunya sumbu
	 * pembatas lingkup data yang tersedia (modul {@code sirs} tidak punya sumbu
	 * tenant/satuan kerja).
	 *
	 * <p>
	 * Relasi OPSIONAL, sehingga dokumen retur tanpa lokasi tetap sah secara
	 * skema — kondisi yang berbahaya karena tidak jelas gudang mana yang
	 * stoknya berkurang. Skema juga tidak menjamin lokasi ini sama dengan
	 * {@link PenerimaanOrder#getLokasi()} pada dokumen penerimaan yang menjadi
	 * dasarnya, sehingga barang bisa "diretur" dari gudang yang berbeda dari
	 * gudang yang dulu menerimanya — mengurangi stok gudang yang tidak pernah
	 * menerima barang tersebut.
	 * </p>
	 *
	 * @return lokasi gudang asal retur, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "lokasi", nullable = true)
	public Lokasi getLokasi() {
		return lokasi;
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
	 * PERHATIAN: sama seperti {@link PenerimaanOrder}, entitas ini punya DUA
	 * kolom bermakna persetujuan — {@code tanggal_disetujui} (getter ini) dan
	 * {@code tanggal_persetujuan} ({@link #getTanggalPersetujuan()}). Keduanya
	 * berdiri sendiri dan tidak disinkronkan oleh skema maupun entitas. Kode
	 * yang menilai status dokumen WAJIB memastikan ia membaca kolom yang
	 * benar-benar diisi alur kerjanya; bila menulis kode baru yang bergantung
	 * pada status, periksa KEDUANYA.
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
	 * {@code tanggal_persetujuan} — kolom persetujuan KEDUA pada entitas ini.
	 * Lihat peringatan lengkap tentang duplikasi ini pada
	 * {@link #getTanggalDisetujui()}. Terisinya kolom ini juga tidak cukup
	 * untuk menyatakan dokumen aktif: {@link #getTanggalPembatalan()} harus
	 * ikut diperiksa karena skema mengizinkan keduanya terisi bersamaan.
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
	 * Menetapkan tanggal pembatalan dokumen retur ini.
	 *
	 * @param tanggalPembatalan timestamp pembatalan.
	 */
	public void setTanggalPembatalan(Date tanggalPembatalan) {
		this.tanggalPembatalan = tanggalPembatalan;
	}

	/**
	 * Mengambil tanggal pembatalan dokumen retur ini. Pembatalan bersifat
	 * penandaan (soft): baris dokumen dan detailnya tetap ada sehingga jejak
	 * audit terjaga.
	 *
	 * <p>
	 * Karena persetujuan dokumen ini MENGURANGI stok, pembatalannya harus
	 * disertai pembalikan mutasi stok yang sudah dituliskan — yaitu
	 * mengembalikan stok yang tadi dikurangi. Pembalikan itu sepenuhnya urusan
	 * lapisan action; entitas ini hanya menyimpan penandanya. Bila pembalikan
	 * gagal atau tidak lengkap, pengurangan stok akan menetap permanen padahal
	 * dokumennya sudah dinyatakan batal — selisih yang tidak akan terdeteksi
	 * oleh mekanisme apa pun di level model, karena tidak ada kolom di sini
	 * yang menyimpan apakah pembalikan sudah benar-benar terjadi.
	 * </p>
	 *
	 * @return timestamp pembatalan, atau {@code null} bila belum dibatalkan.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembatalan")
	public Date getTanggalPembatalan() {
		return tanggalPembatalan;
	}

	/**
	 * Menetapkan pengguna yang membatalkan dokumen retur ini.
	 *
	 * @param dibatalkanOleh pengguna pembatal dokumen.
	 */
	public void setDibatalkanOleh(Tbmuser dibatalkanOleh) {
		this.dibatalkanOleh = dibatalkanOleh;
	}

	/**
	 * Mengambil pengguna yang membatalkan dokumen retur ini — relasi OPSIONAL,
	 * pasangan dari {@link #getTanggalPembatalan()}.
	 *
	 * @return pengguna pembatal, atau {@code null} bila belum dibatalkan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "dibatalkan_oleh", nullable = true)
	public Tbmuser getDibatalkanOleh() {
		return dibatalkanOleh;
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

}
