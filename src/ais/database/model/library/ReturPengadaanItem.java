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
 * Entity <b>header</b> dokumen <b>retur pengadaan item perpustakaan</b> (tabel
 * {@code library.retur_pengadaan_item}). Satu baris merepresentasikan satu nota pengembalian
 * bahan pustaka dari perpustakaan <em>kembali ke pemasok/penyedia</em>, biasanya karena barang
 * yang diterima cacat, salah judul/edisi, kelebihan kirim, atau tidak sesuai pesanan.
 *
 * <p><b>Posisi dalam alur pengadaan perpustakaan.</b> Rantai dokumen pengadaan di modul
 * {@code library} berjalan kira-kira sebagai berikut:
 * {@link PermintaanPengadaanItem} (usulan) &rarr; {@link PemesananPengadaanItem} (pesanan ke
 * penyedia) &rarr; {@link PenerimaanPengadaanItem} (penerimaan fisik dari penyedia) &rarr;
 * <b>{@code ReturPengadaanItem}</b> (pengembalian sebagian/seluruh penerimaan ke penyedia).
 * Karena itu entity ini menyimpan dua referensi kunci sekaligus: {@link #getPenyedia() penyedia}
 * (kepada siapa barang dikembalikan) dan {@link #getPenerimaanPengadaanItem() penerimaan}
 * (dokumen penerimaan mana yang diretur). Rincian baris per judul/eksemplar berada pada
 * {@link ReturPengadaanItemDetail} yang menunjuk balik ke header ini.</p>
 *
 * <p><b>Perbedaan dengan {@code KembaliPengadaanItem}.</b> Nama kedua entity mirip, tetapi
 * artinya berlawanan arah: <b>{@code ReturPengadaanItem}</b> adalah arus <em>keluar ke penyedia</em>
 * (perpustakaan mengembalikan barang pengadaan), sedangkan {@link KembaliPengadaanItem} adalah
 * arus <em>masuk dari anggota</em> (anggota mengembalikan buku yang dipinjam). Jangan tertukar
 * ketika menulis laporan stok atau query mutasi.</p>
 *
 * <p><b>Pola dokumen dua tahap.</b> Seperti hampir seluruh dokumen transaksi AIS, entity ini
 * memakai pasangan atribut pembuatan/persetujuan: {@link #getDibuatOleh() dibuatOleh} +
 * {@link #getTanggalPembuatan() tanggalPembuatan} untuk tahap draf, dan
 * {@link #getDisetujuiOleh() disetujuiOleh} + {@link #getTanggalPersetujuan() tanggalPersetujuan}
 * untuk tahap persetujuan/posting. Kolom persetujuan bersifat <i>nullable</i>: nilai {@code null}
 * berarti dokumen masih berstatus draf dan belum boleh memengaruhi stok. <b>Penting:</b> entity
 * ini <em>tidak</em> menegakkan sendiri aturan tersebut &mdash; tidak ada guard yang mencegah
 * pengisian {@code disetujuiOleh} oleh pengguna yang sama dengan {@code dibuatOleh}, tidak ada
 * pemeriksaan hak akses, dan tidak ada pengecekan apakah dokumen sudah pernah diposting sebelum
 * baris detail diubah atau dihapus. Seluruh gerbang tersebut adalah tanggung jawab lapisan
 * action/service ({@code ais.action.master.library.ReturPengadaanItemAction} dan helper-nya).</p>
 *
 * <p><b>Penjaga keseimbangan kuantitas.</b> Model ini sama sekali tidak memvalidasi bahwa jumlah
 * yang diretur pada baris-baris {@link ReturPengadaanItemDetail} tidak melebihi jumlah yang
 * benar-benar diterima pada {@link PenerimaanPengadaanItem} yang dirujuk. Validasi kuantitas
 * hanya dilakukan di layar ZK ({@code helper/ReturPengadaanItemDetailAction}) yang menghitung
 * sisa {@code jumlah - dikembalikan}. Query, importer massal, atau jalur API yang menulis
 * langsung ke entity ini dapat menghasilkan retur berlebih tanpa peringatan apa pun.</p>
 *
 * <p><b>Multi-tenant.</b> Ruang lingkup data dibatasi oleh {@link #getPerpustakaan() perpustakaan}.
 * Getter-nya bersifat <i>self-healing</i>: bila nilainya {@code null} ia mengambil perpustakaan
 * aktif dari sesi melalui {@link Common#getCurrentPerpustakaan()}. Perilaku ini memudahkan
 * pengisian formulir, tetapi berarti pembatasan tenant sesungguhnya tetap harus ditegakkan pada
 * kriteria query di DAO/action &mdash; entity tidak pernah menolak perpustakaan yang salah.</p>
 *
 * <p><b>Jejak audit.</b> Kelas ditandai {@link Audited} sehingga Hibernate Envers merekam setiap
 * revisi ke tabel bayangan. Selain itu terdapat trio field audit ringan {@link #getOleh() oleh},
 * {@link #getOlehId() olehId}, dan {@link #getTanggal_dirubah() tanggal_dirubah} yang diperbarui
 * oleh callback {@link #onUpdate()}. Ketiganya merupakan keharusan teknis mekanisme audit AIS,
 * bukan duplikasi yang perlu dibersihkan.</p>
 *
 * <p><b>Efek samping.</b> Accessor dan mutator hanya membaca atau mengubah state di memori.
 * Karena Hibernate memakai <i>property access</i> (anotasi dipasang pada getter), setiap getter
 * yang mengubah state internal akan ikut memengaruhi nilai yang ditulis saat <i>flush</i>.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab
 * DAO/service dengan session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see ReturPengadaanItemDetail
 * @see PenerimaanPengadaanItem
 * @see Penyedia
 * @see KembaliPengadaanItem
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "library", name = "retur_pengadaan_item")

public class ReturPengadaanItem extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya sengaja disamakan di seluruh entity modul
	 * {@code library} karena kelas-kelas ini dibangkitkan dari template yang sama; jangan
	 * diubah agar sesi ZK/HTTP yang sudah terserialisasi tetap dapat dibaca.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama (surrogate key) baris ini, dibangkitkan oleh database. */
	private Long id;
	/** Nomor urut tampilan pada grid ZK; bukan kolom bisnis dan tidak wajib berurutan. */
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
	 * hanya berisi spasi</b>: nilai lama sengaja dipertahankan agar jejak audit tidak terhapus
	 * oleh pemanggil yang tidak membawa konteks pengguna (misalnya job batch atau importer).
	 *
	 * @param olehId ID pengguna baru; diabaikan bila {@code null}/blank.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks dokumen untuk ditampilkan pada combobox, listbox, dan log.
	 *
	 * <p>Sengaja membaca field {@link #kode} secara langsung, bukan lewat
	 * {@link #getKode()}, sehingga tidak memicu inisialisasi proxy apa pun.</p>
	 *
	 * @return kode dokumen retur; dapat {@code null} untuk objek yang belum diberi kode.
	 */
	public String toString() {
		return kode;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)},
	 * bersifat no-op bila nilai baru kosong/blank agar jejak audit lama tidak tertimpa.
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
	 * Callback JPA {@code @PreUpdate}. Dipanggil Hibernate tepat sebelum pernyataan
	 * {@code UPDATE} dieksekusi, lalu mendelegasikan pengisian trio field audit
	 * ({@code oleh}, {@code olehId}, {@code tanggal_dirubah}) kepada
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang membaca
	 * pengguna aktif dari sesi.
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

	/** Kode/nomor dokumen retur; unik pada tabel dan dipakai sebagai identitas manusiawi. */
	private String kode;
	/** Catatan bebas, umumnya berisi alasan retur (cacat cetak, salah edisi, kelebihan kirim). */
	private String keterangan;
	/** Perpustakaan (tenant) pemilik dokumen retur ini. */
	private Perpustakaan perpustakaan;
	/** Penyedia/pemasok tujuan pengembalian barang. */
	private Penyedia penyedia;
	/** Dokumen penerimaan pengadaan yang menjadi dasar retur ini. */
	private PenerimaanPengadaanItem penerimaanPengadaanItem;
	/** Tanggal dokumen dibuat (tahap draf). */
	private Date tanggalPembuatan;
	/** Tanggal dokumen disetujui; {@code null} selama dokumen masih draf. */
	private Date tanggalPersetujuan;
	/** Pengguna pembuat dokumen. */
	private Tbmuser dibuatOleh;
	/** Pengguna penyetuju dokumen; {@code null} selama dokumen masih draf. */
	private Tbmuser disetujuiOleh;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate/ZK untuk instansiasi via refleksi.
	 * Seluruh field bernilai default; pengisian dilakukan oleh setter atau oleh Hibernate.
	 */
	public ReturPengadaanItem() {
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
	 * Menyetel kunci utama baris ini. Umumnya hanya dipanggil Hibernate setelah
	 * {@code INSERT}; menyetelnya secara manual pada objek yang sudah persisten berisiko
	 * membuat entity terlepas dari identitas aslinya.
	 *
	 * @param id ID baris baru.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode/nomor dokumen retur.
	 *
	 * @return kode dokumen; wajib terisi dan unik pada tabel.
	 */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return this.kode;
	}

	/**
	 * Menyetel kode/nomor dokumen retur. Keunikan hanya dijaga oleh constraint database;
	 * pembangkitan nomor otomatis dilakukan oleh lapisan action.
	 *
	 * @param kode kode dokumen baru.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan keterangan/alasan retur.
	 *
	 * @return keterangan bebas, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan/alasan retur.
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
	 * Mengembalikan pengguna pembuat dokumen. Kolom bersifat {@code NOT NULL} sehingga
	 * dokumen yang tersimpan selalu punya pembuat.
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
	 * <p><b>Catatan integritas:</b> setter ini tidak memeriksa apakah penyetuju berbeda dari
	 * pembuat, tidak memeriksa hak akses, dan tidak menolak perubahan pada dokumen yang sudah
	 * disetujui. Pemisahan tugas (<i>segregation of duties</i>) harus ditegakkan di lapisan
	 * action sebelum memanggil setter ini.</p>
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
	 * <p><b>Peringatan:</b> fallback ini hanya dikembalikan, <em>tidak</em> ditulis balik ke
	 * field. Karena Hibernate membaca nilai lewat getter (property access), baris yang belum
	 * pernah mengisi {@code tanggalPembuatan} akan tersimpan dengan waktu saat <i>flush</i>
	 * terjadi &mdash; bukan waktu saat dokumen benar-benar dibuat. Untuk dokumen yang berumur
	 * panjang dalam satu sesi ZK, selisihnya bisa terasa. Isilah tanggal secara eksplisit di
	 * lapisan action bila akurasi diperlukan.</p>
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
	 * Mengembalikan tanggal persetujuan dokumen. Berbeda dengan
	 * {@link #getTanggalPembuatan()}, getter ini <b>tidak</b> memakai fallback: nilai
	 * {@code null} adalah penanda sah bahwa dokumen belum disetujui.
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
	 * {@link GeneralValueObject} untuk mengganti proxy Hibernate yang sudah tidak terhubung
	 * session dengan instance yang aman dibaca. Nilai hasil ditulis balik ke field, sehingga
	 * getter ini <b>mengubah state objek</b> &mdash; sebuah getter destruktif ringan yang
	 * sengaja dipakai agar formulir ZK tidak perlu mengisi tenant secara manual.</p>
	 *
	 * <p><b>Konsekuensi keamanan:</b> karena nilai diambil dari sesi pemanggil, dokumen yang
	 * dimuat oleh pengguna tenant lain dan kemudian di-<i>flush</i> dapat berpindah tenant bila
	 * field-nya kebetulan {@code null}. Pembatasan tenant yang sesungguhnya harus berupa
	 * kriteria {@code Restrictions.eq("perpustakaan", ...)} pada query DAO/action, bukan
	 * mengandalkan getter ini.</p>
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
	 * Mengembalikan nomor urut tampilan pada grid ZK. Nilai ini murni kosmetik; tidak
	 * dipetakan sebagai kolom bisnis dan tidak boleh dipakai sebagai identitas.
	 *
	 * @return nomor urut tampilan, atau {@code null} bila belum diisi renderer.
	 */
	public Long getIndex() {
		return index;
	}

	/**
	 * Mengembalikan penyedia/pemasok tujuan pengembalian barang.
	 *
	 * <p>Relasi ini bersifat <i>eager</i> lewat {@link FetchMode#SELECT}, sehingga aman dibaca
	 * pada renderer ZK meski session sudah ditutup. Kolomnya {@code nullable}: dokumen retur
	 * yang belum menentukan penyedia tetap dapat disimpan sebagai draf.</p>
	 *
	 * @return penyedia tujuan retur, atau {@code null} bila belum ditentukan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penyedia", nullable = true)
	public Penyedia getPenyedia() {
		return penyedia;
	}

	/**
	 * Menyetel penyedia/pemasok tujuan pengembalian barang.
	 *
	 * <p><b>Catatan integritas:</b> setter ini tidak memverifikasi bahwa penyedia yang dipilih
	 * sama dengan penyedia pada {@link #getPenerimaanPengadaanItem() dokumen penerimaan} yang
	 * dirujuk. Dokumen retur karenanya dapat menunjuk penerimaan dari penyedia A namun
	 * mengembalikan barang ke penyedia B tanpa peringatan apa pun dari model.</p>
	 *
	 * @param penyedia penyedia tujuan retur.
	 */
	public void setPenyedia(Penyedia penyedia) {
		this.penyedia = penyedia;
	}

	/**
	 * Mengembalikan dokumen penerimaan pengadaan yang menjadi dasar retur ini.
	 *
	 * <p>Referensi inilah yang menghubungkan retur dengan barang yang benar-benar pernah
	 * masuk. Kolomnya {@code nullable}, sehingga secara struktur dimungkinkan membuat retur
	 * "melayang" tanpa dokumen penerimaan &mdash; kondisi yang membuat rekonsiliasi stok tidak
	 * dapat ditelusuri dan sebaiknya dicegah di lapisan action.</p>
	 *
	 * @return dokumen penerimaan sumber, atau {@code null} bila belum ditentukan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penerimaan_pengadaan_item", nullable = true)
	public PenerimaanPengadaanItem getPenerimaanPengadaanItem() {
		return penerimaanPengadaanItem;
	}

	/**
	 * Menyetel dokumen penerimaan pengadaan yang menjadi dasar retur ini.
	 *
	 * @param penerimaanPengadaanItem dokumen penerimaan sumber.
	 */
	public void setPenerimaanPengadaanItem(PenerimaanPengadaanItem penerimaanPengadaanItem) {
		this.penerimaanPengadaanItem = penerimaanPengadaanItem;
	}

}
