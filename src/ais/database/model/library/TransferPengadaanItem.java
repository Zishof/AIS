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
 * Entity <b>header</b> dokumen <b>transfer (mutasi antar perpustakaan)</b> bahan pustaka (tabel
 * {@code library.transfer_pengadaan_item}). Satu baris merepresentasikan satu nota pengiriman
 * koleksi dari satu perpustakaan/unit ke perpustakaan lain dalam institusi yang sama &mdash;
 * misalnya pemindahan eksemplar dari perpustakaan pusat ke perpustakaan fakultas.
 *
 * <p><b>Dua sisi satu perpindahan.</b> Perpindahan koleksi direkam sebagai sepasang dokumen:</p>
 * <ul>
 *   <li><b>{@code TransferPengadaanItem}</b> (kelas ini) &mdash; sisi <em>pengirim</em>. Field
 *       {@link #getPerpustakaan() perpustakaan} adalah asal, {@link #getPerpustakaanTujuan()
 *       perpustakaanTujuan} adalah tujuan.</li>
 *   <li>{@link TerimaPengadaanItem} &mdash; sisi <em>penerima</em>, yang dibuat di perpustakaan
 *       tujuan sebagai konfirmasi barang sudah sampai.</li>
 * </ul>
 * <p><b>Perhatikan duplikasi penunjuk dua arah.</b> Kelas ini menyimpan
 * {@link #getTerimaPengadaanItem() terimaPengadaanItem}, sementara {@link TerimaPengadaanItem}
 * juga menyimpan {@code transferPengadaanItem}. Keduanya adalah kolom {@code ManyToOne} yang
 * berdiri sendiri, <b>bukan</b> pasangan {@code mappedBy}, sehingga Hibernate tidak menjaga
 * konsistensinya. Bila hanya satu sisi yang diisi (atau keduanya diisi menunjuk dokumen
 * berbeda), tidak ada mekanisme apa pun di model yang mendeteksinya; menyinkronkan kedua
 * penunjuk adalah tanggung jawab lapisan action.</p>
 *
 * <p><b>Jangan tertukar dengan dokumen bernama mirip.</b> Modul {@code library} memuat beberapa
 * dokumen dengan akhiran {@code PengadaanItem} yang artinya sangat berbeda:
 * {@link PenerimaanPengadaanItem} (barang masuk dari <em>penyedia</em>),
 * {@link ReturPengadaanItem} (barang keluar ke <em>penyedia</em>),
 * {@link TerimaPengadaanItem} (barang masuk dari <em>perpustakaan lain</em>),
 * {@link PeminjamanPengadaanItem} (peminjaman buku oleh <em>anggota</em>), dan
 * {@link KembaliPengadaanItem} (pengembalian buku oleh <em>anggota</em>). Kelas ini adalah
 * satu-satunya yang memindahkan koleksi antar tenant internal.</p>
 *
 * <p><b>Pola dokumen dua tahap.</b> Sama seperti dokumen transaksi AIS lainnya, entity ini
 * memakai pasangan {@link #getDibuatOleh() dibuatOleh}/{@link #getTanggalPembuatan()
 * tanggalPembuatan} dan {@link #getDisetujuiOleh() disetujuiOleh}/{@link
 * #getTanggalPersetujuan() tanggalPersetujuan}. Kolom persetujuan {@code nullable}: {@code null}
 * berarti dokumen masih draf. Model <b>tidak</b> menegakkan aturan ini &mdash; tidak ada
 * pemisahan tugas antara pembuat dan penyetuju, tidak ada pemeriksaan hak akses, dan tidak ada
 * penguncian baris rincian setelah dokumen disetujui/diposting.</p>
 *
 * <p><b>Penjaga keseimbangan.</b> Model tidak memverifikasi bahwa eksemplar yang ditransfer
 * benar-benar tersedia di perpustakaan asal, tidak mencegah asal dan tujuan bernilai sama, dan
 * tidak mencegah satu eksemplar ditransfer dua kali sebelum dokumen pertama diterima. Seluruh
 * pemeriksaan itu berada di lapisan action
 * ({@code ais.action.master.library.TransferPengadaanItemAction}) &mdash; kalau ada.</p>
 *
 * <p><b>Multi-tenant.</b> {@link #getPerpustakaan()} bersifat <i>self-healing</i>: bila
 * {@code null} ia mengambil perpustakaan aktif dari sesi melalui
 * {@link Common#getCurrentPerpustakaan()}. Karena dokumen ini sengaja melintasi tenant,
 * pembatasan akses harus dilakukan lewat kriteria query di DAO/action, bukan mengandalkan
 * getter tersebut.</p>
 *
 * <p><b>Jejak audit.</b> Kelas ditandai {@link Audited} sehingga Hibernate Envers merekam setiap
 * revisi. Trio field audit ringan {@code oleh}/{@code olehId}/{@code tanggal_dirubah} beserta
 * callback {@link #onUpdate()} merupakan keharusan teknis mekanisme audit AIS.</p>
 *
 * <p><b>Efek samping.</b> Accessor dan mutator hanya membaca atau mengubah state di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab
 * DAO/service dengan session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see TransferPengadaanItemDetail
 * @see TerimaPengadaanItem
 * @see Perpustakaan
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "library", name = "transfer_pengadaan_item")

public class TransferPengadaanItem extends GeneralValueObject {

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

	/**
	 * Representasi teks dokumen untuk combobox, listbox, dan log.
	 *
	 * <p>Sengaja membaca field {@link #kode} secara langsung sehingga tidak memicu inisialisasi
	 * proxy apa pun.</p>
	 *
	 * @return kode dokumen transfer; dapat {@code null} untuk objek yang belum diberi kode.
	 */
	public String toString() {
		return kode;
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

	/** Kode/nomor dokumen transfer; unik pada tabel dan dipakai sebagai identitas manusiawi. */
	private String kode;
	/** Catatan bebas, umumnya alasan pemindahan koleksi. */
	private String keterangan;
	/** Perpustakaan <b>asal</b> (tenant pemilik dokumen pengiriman). */
	private Perpustakaan perpustakaan;
	/** Perpustakaan <b>tujuan</b> pemindahan koleksi. */
	private Perpustakaan perpustakaanTujuan;
	/** Tanggal dokumen dibuat (tahap draf). */
	private Date tanggalPembuatan;
	/** Tanggal dokumen disetujui; {@code null} selama dokumen masih draf. */
	private Date tanggalPersetujuan;
	/** Pengguna pembuat dokumen. */
	private Tbmuser dibuatOleh;
	/** Pengguna penyetuju dokumen; {@code null} selama dokumen masih draf. */
	private Tbmuser disetujuiOleh;

	/** Dokumen konfirmasi penerimaan di perpustakaan tujuan; {@code null} bila belum diterima. */
	private TerimaPengadaanItem terimaPengadaanItem;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate/ZK untuk instansiasi via refleksi.
	 */
	public TransferPengadaanItem() {
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
	 * Mengembalikan kode/nomor dokumen transfer.
	 *
	 * @return kode dokumen; wajib terisi dan unik pada tabel.
	 */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return this.kode;
	}

	/**
	 * Menyetel kode/nomor dokumen transfer. Keunikan hanya dijaga oleh constraint database;
	 * pembangkitan nomor otomatis dilakukan oleh lapisan action.
	 *
	 * @param kode kode dokumen baru.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan keterangan/alasan transfer.
	 *
	 * @return keterangan bebas, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan/alasan transfer.
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
	 * <p><b>Catatan integritas:</b> setter tidak memeriksa bahwa penyetuju berbeda dari
	 * pembuat, tidak memeriksa hak akses, dan tidak menolak perubahan pada dokumen yang sudah
	 * disetujui. Pemisahan tugas harus ditegakkan di lapisan action.</p>
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
	 * {@code tanggalPembuatan} akan tersimpan dengan waktu saat <i>flush</i> terjadi, bukan
	 * waktu dokumen benar-benar dibuat. Isilah tanggal secara eksplisit di lapisan action bila
	 * akurasi diperlukan.</p>
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
	 * {@code null} adalah penanda sah bahwa dokumen belum disetujui, dan dipakai layar cetak
	 * untuk membedakan label "Belum disetujui" dari "Disetujui oleh ...".
	 *
	 * @return tanggal persetujuan, atau {@code null} bila dokumen masih draf.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {
		return tanggalPersetujuan;
	}

	/**
	 * Menyetel perpustakaan asal (tenant pemilik dokumen pengiriman).
	 *
	 * @param perpustakaan perpustakaan asal.
	 */
	public void setPerpustakaan(Perpustakaan perpustakaan) {
		this.perpustakaan = perpustakaan;
	}

	/**
	 * Mengembalikan perpustakaan asal, dengan <b>pengisian otomatis</b> dari sesi bila belum
	 * diisi.
	 *
	 * <p>Alur getter ini: (1) bila field masih {@code null}, ambil perpustakaan aktif melalui
	 * {@link Common#getCurrentPerpustakaan()}; (2) jalankan {@code check(...)} milik
	 * {@link GeneralValueObject} untuk menukar proxy Hibernate yang sudah terlepas session
	 * dengan instance yang aman dibaca. Hasilnya ditulis balik ke field, sehingga getter ini
	 * <b>mengubah state objek</b> (getter destruktif ringan).</p>
	 *
	 * <p><b>Konsekuensi keamanan:</b> karena nilai diambil dari sesi pemanggil, dokumen yang
	 * dimuat pengguna tenant lain dan kemudian di-<i>flush</i> dapat berpindah asal bila
	 * field-nya kebetulan {@code null}. Pada dokumen transfer hal ini lebih berbahaya daripada
	 * di entity lain karena dokumen ini memang bertugas memindahkan koleksi antar tenant.
	 * Batasi ruang lingkup lewat kriteria query, bukan lewat getter ini.</p>
	 *
	 * @return perpustakaan asal; dapat {@code null} bila sesi juga tidak memilikinya.
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

	/**
	 * Mengembalikan perpustakaan tujuan pemindahan koleksi.
	 *
	 * <p>Berbeda dengan {@link #getPerpustakaan()}, getter ini <b>tidak</b> mengisi otomatis
	 * dari sesi &mdash; hanya menjalankan {@code check(...)} untuk mengamankan proxy. Perilaku
	 * ini benar: tujuan transfer harus dipilih pengguna secara eksplisit dan tidak boleh
	 * diam-diam menjadi perpustakaan yang sedang aktif.</p>
	 *
	 * <p><b>Catatan integritas:</b> tidak ada pemeriksaan bahwa tujuan berbeda dari asal.
	 * Dokumen transfer dengan asal dan tujuan yang sama akan tersimpan tanpa keluhan dan
	 * menghasilkan mutasi stok yang saling meniadakan namun tetap membebani riwayat.</p>
	 *
	 * @return perpustakaan tujuan, atau {@code null} bila belum ditentukan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "perpustakaan_tujuan", nullable = true)
	public Perpustakaan getPerpustakaanTujuan() {
		perpustakaanTujuan = check(perpustakaanTujuan);
		return perpustakaanTujuan;
	}

	/**
	 * Menyetel perpustakaan tujuan pemindahan koleksi.
	 *
	 * @param perpustakaanTujuan perpustakaan tujuan.
	 */
	public void setPerpustakaanTujuan(Perpustakaan perpustakaanTujuan) {
		this.perpustakaanTujuan = perpustakaanTujuan;
	}

	/**
	 * Mengembalikan dokumen konfirmasi penerimaan di perpustakaan tujuan.
	 *
	 * <p>Nilai {@code null} berarti kiriman belum dikonfirmasi diterima &mdash; koleksi sedang
	 * "dalam perjalanan". Penunjuk ini berpasangan dengan {@code transferPengadaanItem} pada
	 * {@link TerimaPengadaanItem}, tetapi keduanya adalah kolom {@code ManyToOne} mandiri
	 * (bukan {@code mappedBy}), sehingga Hibernate tidak menyinkronkannya. Bila hanya satu sisi
	 * diisi, kedua dokumen akan terlihat tidak terhubung dari salah satu arah tanpa ada galat
	 * yang muncul.</p>
	 *
	 * @return dokumen penerimaan pasangannya, atau {@code null} bila belum diterima.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "terima_pengadaan_item", nullable = true)
	public TerimaPengadaanItem getTerimaPengadaanItem() {
		return terimaPengadaanItem;
	}

	/**
	 * Menyetel dokumen konfirmasi penerimaan di perpustakaan tujuan.
	 *
	 * <p>Pemanggil bertanggung jawab mengisi pula sisi sebaliknya
	 * ({@code terimaPengadaanItem.setTransferPengadaanItem(this)}) agar kedua penunjuk tetap
	 * konsisten.</p>
	 *
	 * @param terimaPengadaanItem dokumen penerimaan pasangannya.
	 */
	public void setTerimaPengadaanItem(TerimaPengadaanItem terimaPengadaanItem) {
		this.terimaPengadaanItem = terimaPengadaanItem;
	}

}
