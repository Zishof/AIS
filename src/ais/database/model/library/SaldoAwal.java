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

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.ui.util.WaktuUtil;

/**
 * Entity <b>header</b> dokumen <b>saldo awal koleksi perpustakaan</b> (tabel
 * {@code library.saldo_awal}). Satu baris merepresentasikan satu nota pencatatan stok pembuka
 * &mdash; koleksi yang sudah dimiliki perpustakaan sebelum sistem dipakai, atau sebelum periode
 * pembukuan baru dimulai &mdash; sehingga persediaan sistem punya titik berangkat tanpa harus
 * mengarang dokumen pengadaan fiktif.
 *
 * <h3>Struktur bertingkat tiga</h3>
 * <p>Berbeda dari dokumen lain di klaster ini yang hanya punya dua tingkat (header dan rincian),
 * saldo awal punya <b>tiga</b>:</p>
 * <ol>
 *   <li><b>{@code SaldoAwal}</b> (kelas ini) &mdash; header dokumen;</li>
 *   <li>{@link SaldoAwalDetail} &mdash; satu baris per judul, membawa kuantitas dan opsional
 *       satu {@link BatchItemPunyaBarcode};</li>
 *   <li>{@link SaldoAwalDetailDetail} &mdash; satu baris per <em>eksemplar</em>, membawa barcode
 *       dan {@link StatusItem}. Tingkat ini hanya dipakai bila
 *       {@link SaldoAwalDetail#getDataPerItem()} bernilai benar.</li>
 * </ol>
 * <p>Tingkat ketiga inilah yang memungkinkan perpustakaan mendaftarkan koleksi lama sekaligus
 * dengan barcode masing-masing dan kondisi fisiknya, tanpa harus melewati alur pengadaan.</p>
 *
 * <p><b>Dokumen paling kuat, pengendalian paling sedikit.</b> Seperti {@link KoreksiItem},
 * dokumen saldo awal tidak memiliki dokumen sumber apa pun: tidak ada penyedia, tidak ada
 * pesanan, tidak ada penerimaan. Bedanya, saldo awal umumnya menambahkan stok dalam jumlah besar
 * sekaligus dan dapat membuat entri barcode baru. Satu-satunya pengendalian yang tersisa adalah
 * persetujuan manusia lewat {@link #getDisetujuiOleh() disetujuiOleh} dan jejak {@link Audited
 * Envers}. Model <b>tidak</b> membatasi apa pun: tidak ada pemeriksaan bahwa perpustakaan yang
 * bersangkutan memang belum punya saldo awal, tidak ada pembatasan periode sehingga dokumen
 * saldo awal kedua dapat dibuat kapan saja, dan tidak ada pemisahan tugas antara pembuat dan
 * penyetuju. Gerbang di lapisan action adalah pengendalian yang menentukan.</p>
 *
 * <p><b>Multi-tenant.</b> Ruang lingkup dibatasi {@link #getPerpustakaan() perpustakaan}, yang
 * getter-nya mengisi diri sendiri dari sesi bila {@code null}. Pembatasan tenant yang
 * sesungguhnya tetap harus berupa kriteria query di DAO/action.</p>
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
 * @see SaldoAwalDetailDetail
 * @see KoreksiItem
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "library", name = "saldo_awal")

public class SaldoAwal extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya sengaja disamakan di seluruh entity modul
	 * {@code library} karena kelas-kelas ini dibangkitkan dari template yang sama; jangan
	 * diubah agar sesi ZK/HTTP yang sudah terserialisasi tetap dapat dibaca.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama (surrogate key) baris ini, dibangkitkan oleh database. */
	private Long id;
	/** Nomor urut tampilan pada grid ZK; bukan kolom bisnis. */
	private Long index;
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

	// public String toString() {
	// return kode;
	// }

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

	/** Kode/nomor dokumen saldo awal; unik pada tabel. */
	private String kode;
	/**
	 * Catatan bebas. Karena dokumen saldo awal tidak punya dokumen sumber, teks inilah
	 * penjelasan satu-satunya mengenai asal-usul koleksi yang dicatat.
	 */
	private String keterangan;
	/** Perpustakaan (tenant) pemilik dokumen saldo awal ini. */
	private Perpustakaan perpustakaan;
	/** Tanggal dokumen dibuat (tahap draf). */
	private Date tanggalPembuatan;
	/** Tanggal dokumen disetujui; {@code null} selama dokumen masih draf. */
	private Date tanggalPersetujuan;
	/** Pengguna pembuat dokumen. */
	private Tbmuser dibuatOleh;
	/** Pengguna penyetuju dokumen; {@code null} selama dokumen masih draf. */
	private Tbmuser disetujuiOleh;

	/**
	 * Representasi teks dokumen untuk combobox, listbox, dan log.
	 *
	 * <p>Menggabungkan {@link #id} dan {@link #kode} dengan tanda hubung. Bentuk ini berbeda
	 * dari saudara-saudaranya di klaster pengadaan ({@link ReturPengadaanItem},
	 * {@link TransferPengadaanItem}, {@link KoreksiItem}) yang hanya menampilkan kode; versi
	 * lama yang hanya mengembalikan {@code kode} masih tersisa sebagai komentar sumber di atas.
	 * Menyertakan ID membuat dua dokumen berkode sama tetap dapat dibedakan di layar, tetapi
	 * juga berarti teks ini <b>tidak cocok dipakai sebagai label cetak</b> yang dilihat
	 * pengguna akhir.</p>
	 *
	 * <p>Kedua nilai dibaca langsung dari field sehingga tidak memicu inisialisasi proxy.</p>
	 *
	 * @return gabungan {@code id-kode}; bagiannya dapat berisi {@code "null"} untuk objek baru.
	 */
	public String toString() {
		return id + "-" + kode;
	}

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate/ZK untuk instansiasi via refleksi.
	 */
	public SaldoAwal() {
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
	 * Mengembalikan kode/nomor dokumen saldo awal.
	 *
	 * @return kode dokumen; wajib terisi dan unik pada tabel.
	 */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return this.kode;
	}

	/**
	 * Menyetel kode/nomor dokumen saldo awal. Keunikan hanya dijaga oleh constraint database;
	 * pembangkitan nomor otomatis dilakukan oleh lapisan action.
	 *
	 * @param kode kode dokumen baru.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan keterangan dokumen, dinormalkan ke string kosong bila belum diisi.
	 *
	 * <p>Normalisasi tidak ditulis balik ke field, sehingga getter ini tidak mengubah state.
	 * Perhatikan bahwa perilakunya berbeda dari {@link KoreksiItem#getKeterangan()} yang
	 * mengembalikan {@code null} apa adanya &mdash; ketidakseragaman kecil yang perlu diingat
	 * saat menulis laporan yang membaca kedua entity sekaligus.</p>
	 *
	 * @return keterangan dokumen; tidak pernah {@code null}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan == null ? "" : keterangan;
	}

	/**
	 * Menyetel keterangan dokumen.
	 *
	 * @param keterangan teks keterangan baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menyetel pengguna pembuat dokumen.
	 *
	 * @param dibuatOleh pengguna pembuat.
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengembalikan pengguna pembuat dokumen. Kolom bersifat {@code NOT NULL} sehingga dokumen
	 * yang tersimpan selalu punya pembuat.
	 *
	 * @return pengguna pembuat dokumen.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "dibuat_oleh", nullable = false)
	public Tbmuser getDibuatOleh() {
		return dibuatOleh;
	}

	/**
	 * Menyetel pengguna penyetuju dokumen.
	 *
	 * <p><b>Catatan integritas:</b> setter tidak memeriksa bahwa penyetuju berbeda dari
	 * pembuat, tidak memeriksa hak akses, dan tidak menolak perubahan pada dokumen yang sudah
	 * disetujui. Untuk dokumen saldo awal &mdash; yang dapat menambahkan stok dalam jumlah
	 * besar tanpa dokumen pembanding &mdash; persetujuan adalah pengendalian terakhir, sehingga
	 * pemisahan tugas wajib ditegakkan lapisan action.</p>
	 *
	 * @param disetujuiOleh pengguna penyetuju; {@code null} mengembalikan dokumen ke status draf.
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengembalikan pengguna penyetuju dokumen.
	 *
	 * @return pengguna penyetuju, atau {@code null} bila dokumen masih berstatus draf.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		return disetujuiOleh;
	}

	/**
	 * Menyetel tanggal pembuatan dokumen.
	 *
	 * @param tanggalPembuatan tanggal pembuatan baru.
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Mengembalikan tanggal pembuatan dokumen, dengan <b>fallback ke waktu server</b> bila
	 * belum diisi.
	 *
	 * <p><b>Peringatan:</b> fallback hanya dikembalikan, <em>tidak</em> ditulis balik ke field.
	 * Karena Hibernate membaca nilai lewat getter (property access), baris tanpa
	 * {@code tanggalPembuatan} tersimpan dengan waktu saat <i>flush</i> terjadi. Untuk saldo
	 * awal hal ini berarti tanggal berlakunya stok pembuka dapat bergeser dari tanggal
	 * <i>cut-off</i> yang dimaksudkan; isilah secara eksplisit di lapisan action.</p>
	 *
	 * @return tanggal pembuatan tersimpan, atau waktu server saat ini bila belum diisi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		return tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan;
	}

	/**
	 * Menyetel tanggal persetujuan dokumen.
	 *
	 * @param tanggalPersetujuan tanggal persetujuan; {@code null} berarti belum disetujui.
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengembalikan tanggal persetujuan dokumen. Tidak memakai fallback: nilai {@code null}
	 * adalah penanda sah bahwa dokumen belum disetujui dan karenanya stok pembukanya belum
	 * boleh dihitung.
	 *
	 * @return tanggal persetujuan, atau {@code null} bila dokumen masih draf.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {
		return tanggalPersetujuan;
	}

	/**
	 * Menyetel perpustakaan (tenant) pemilik dokumen.
	 *
	 * @param perpustakaan perpustakaan pemilik.
	 */
	public void setPerpustakaan(Perpustakaan perpustakaan) {
		this.perpustakaan = perpustakaan;
	}

	/**
	 * Mengembalikan perpustakaan (tenant) pemilik dokumen, dengan <b>pengisian otomatis</b>
	 * dari sesi bila belum diisi.
	 *
	 * <p>Alur getter ini: (1) bila field masih {@code null}, ambil perpustakaan aktif melalui
	 * {@link Common#getCurrentPerpustakaan()}; (2) jalankan {@code check(...)} milik
	 * {@link GeneralValueObject} untuk menukar proxy Hibernate yang sudah terlepas session
	 * dengan instance yang aman dibaca. Hasilnya ditulis balik ke field, sehingga getter ini
	 * mengubah state objek (getter destruktif ringan).</p>
	 *
	 * <p><b>Konsekuensi keamanan:</b> dokumen saldo awal yang dimuat pengguna tenant lain dan
	 * kemudian di-<i>flush</i> dapat berpindah tenant bila field-nya kebetulan {@code null}
	 * &mdash; memindahkan seluruh stok pembuka ke perpustakaan yang salah. Pembatasan tenant
	 * yang sesungguhnya harus berupa kriteria {@code Restrictions.eq("perpustakaan", ...)} pada
	 * query DAO/action.</p>
	 *
	 * @return perpustakaan pemilik dokumen; dapat {@code null} bila sesi juga tidak memilikinya.
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
	 * Menyetel nomor urut tampilan pada grid ZK.
	 *
	 * @param index nomor urut tampilan.
	 */
	public void setIndex(Long index) {
		this.index = index;
	}

	/**
	 * Mengembalikan nomor urut tampilan pada grid ZK. Nilai murni kosmetik dan tidak boleh
	 * dipakai sebagai identitas.
	 *
	 * @return nomor urut tampilan, atau {@code null} bila belum diisi renderer.
	 */
	public Long getIndex() {
		return index;
	}

}
