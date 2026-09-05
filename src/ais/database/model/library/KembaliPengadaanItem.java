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
 * Entity <b>header</b> dokumen <b>pengembalian buku oleh anggota</b> (tabel
 * {@code library.kembali_pengadaan_item}). Satu baris merepresentasikan satu transaksi
 * sirkulasi ketika anggota mengembalikan satu atau beberapa eksemplar yang sebelumnya dipinjam.
 * Rincian per eksemplar &mdash; termasuk denda yang dikenakan &mdash; berada pada
 * {@link KembaliPengadaanItemDetail}.
 *
 * <p><b>Nama yang menyesatkan.</b> Meski mengandung kata "PengadaanItem", entity ini
 * <em>bukan</em> bagian dari alur pengadaan dengan penyedia. Ia adalah dokumen
 * <b>sirkulasi/peminjaman</b>: pasangannya adalah {@link PeminjamanPengadaanItem} (peminjaman
 * oleh anggota), bukan {@link PenerimaanPengadaanItem} atau {@link ReturPengadaanItem}. Buktinya
 * ada pada relasi yang dimilikinya &mdash; {@link #getPeminjamanPengadaanItem() peminjaman} dan
 * {@link #getKunjunganAnggota() kunjungan anggota} &mdash; dan pada ketiadaan referensi apa pun
 * ke {@link Penyedia}. Bandingkan dengan {@link ReturPengadaanItem} yang justru merupakan
 * pengembalian ke penyedia; kedua kata "kembali/retur" itu berlawanan arah.</p>
 *
 * <p><b>Penunjuk dua arah tanpa {@code mappedBy}.</b> Kelas ini menyimpan
 * {@link #getPeminjamanPengadaanItem() peminjamanPengadaanItem}, sementara
 * {@link PeminjamanPengadaanItem} juga menyimpan {@code kembaliPengadaanItem}. Keduanya kolom
 * {@code ManyToOne} yang berdiri sendiri, bukan pasangan {@code mappedBy}, sehingga Hibernate
 * tidak menjaga konsistensinya. Pengembalian sebagian (sebagian eksemplar dikembalikan lebih
 * dulu) membuat hubungan ini semakin longgar: pemetaan yang benar-benar dipakai untuk menghitung
 * keterlambatan justru pasangan pada tingkat rincian, yaitu
 * {@link PeminjamanPengadaanItemDetail#getKembaliPengadaanItemDetail()}.</p>
 *
 * <h3>Denda: dihitung di luar entity ini</h3>
 * <p>Kelas ini <b>tidak</b> menyimpan maupun menghitung denda. Field {@code denda} dan
 * {@code diskonDenda} beserta method {@code hitungDenda()} dan {@code getDiskonDenda()} masih
 * tersisa sebagai komentar sumber di bagian bawah berkas &mdash; peninggalan desain lama ketika
 * denda diakumulasi pada header. Dalam kode yang aktif sekarang:</p>
 * <ul>
 *   <li>denda per eksemplar dihitung {@code LibraryUtil.hitungDendaItem(...)} berdasarkan
 *       {@link DendaKeterlambatanItem} yang cocok dengan profil anggota, lalu <b>disimpan pada
 *       baris rincian</b> ({@link KembaliPengadaanItemDetail#getDenda()});</li>
 *   <li>total denda satu dokumen diperoleh dengan menjumlahkan kolom itu lewat
 *       {@code LibraryUtil.hitungDendaPerItem(KembaliPengadaanItem)}.</li>
 * </ul>
 * <p>Karena {@code diskonDenda} dinonaktifkan, <b>fasilitas potongan denda tidak lagi
 * tersedia</b>; keringanan hanya bisa diberikan lewat pembebasan (<i>waiver</i>) pada tingkat
 * rincian. Jangan mengaktifkan kembali blok komentar tersebut tanpa memeriksa
 * {@code LibraryUtil} terlebih dahulu, karena rumus lama mengandaikan denda tersimpan di header
 * dan akan menghitung ganda.</p>
 *
 * <p><b>Pola dokumen dua tahap.</b> Entity memakai {@link #getDibuatOleh() dibuatOleh}/
 * {@link #getTanggalPembuatan() tanggalPembuatan} dan {@link #getDisetujuiOleh() disetujuiOleh}/
 * {@link #getTanggalPersetujuan() tanggalPersetujuan}. Untuk dokumen sirkulasi,
 * {@code tanggalPersetujuan} juga berperan sebagai tanggal efektif pengembalian: bila baris
 * rincian belum punya tanggalnya sendiri,
 * {@link KembaliPengadaanItemDetail#getTanggal()} akan mengambil nilai dari sini. Tanggal itulah
 * yang menentukan berapa hari keterlambatan dihitung, sehingga kekeliruan pada
 * {@code tanggalPersetujuan} langsung berdampak pada besaran denda anggota.</p>
 *
 * <p><b>Multi-tenant.</b> Ruang lingkup dibatasi oleh {@link #getPerpustakaan() perpustakaan},
 * yang getter-nya mengisi diri sendiri dari sesi bila {@code null}. Pembatasan tenant yang
 * sesungguhnya tetap harus berupa kriteria query di DAO/action.</p>
 *
 * <p><b>Efek samping.</b> Accessor dan mutator hanya membaca atau mengubah state di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab
 * DAO/service dengan session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see KembaliPengadaanItemDetail
 * @see PeminjamanPengadaanItem
 * @see DendaKeterlambatanItem
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "library", name = "kembali_pengadaan_item")
public class KembaliPengadaanItem extends GeneralValueObject {

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
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks dokumen untuk combobox, listbox, dan log.
	 *
	 * <p>Membaca field {@link #kode} secara langsung sehingga tidak memicu inisialisasi proxy
	 * apa pun. Nama anggota dan besaran denda tidak ikut tampil.</p>
	 *
	 * @return kode dokumen pengembalian; dapat {@code null} untuk objek yang belum diberi kode.
	 */
	public String toString() {
		return kode;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir; no-op bila nilai baru kosong/blank.
	 *
	 * @param oleh nama pengguna baru; diabaikan bila {@code null}/blank.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
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
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

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

	/** Kode/nomor dokumen pengembalian; unik pada tabel dan dipakai sebagai identitas manusiawi. */
	private String kode;
	/** Catatan bebas, umumnya kondisi buku saat dikembalikan atau alasan keringanan. */
	private String keterangan;
	/** Dokumen peminjaman yang dikembalikan lewat dokumen ini. */
	private PeminjamanPengadaanItem peminjamanPengadaanItem;
	/** Perpustakaan (tenant) tempat pengembalian dilayani. */
	private Perpustakaan perpustakaan;
	// private Double diskonDenda;
	/** Tanggal dokumen dibuat (petugas mulai memproses pengembalian). */
	private Date tanggalPembuatan;
	/** Tanggal dokumen disetujui; juga menjadi tanggal efektif pengembalian bagi baris rincian. */
	private Date tanggalPersetujuan;
	/** Petugas pembuat dokumen. */
	private Tbmuser dibuatOleh;
	/** Petugas penyetuju dokumen; {@code null} selama dokumen masih draf. */
	private Tbmuser disetujuiOleh;

	// private Double denda;
	/** Kunjungan anggota tempat transaksi pengembalian ini terjadi (statistik kunjungan). */
	private KunjunganAnggota kunjunganAnggota;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate/ZK untuk instansiasi via refleksi.
	 */
	public KembaliPengadaanItem() {
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
	 * Mengembalikan kode/nomor dokumen pengembalian.
	 *
	 * @return kode dokumen; wajib terisi dan unik pada tabel.
	 */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return this.kode;
	}

	/**
	 * Menyetel kode/nomor dokumen pengembalian. Keunikan hanya dijaga oleh constraint database.
	 *
	 * @param kode kode dokumen baru.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan keterangan dokumen.
	 *
	 * @return keterangan bebas, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
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
	 * Menyetel petugas pembuat dokumen.
	 *
	 * @param dibuatOleh petugas pembuat.
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengembalikan petugas pembuat dokumen.
	 *
	 * <p>Getter menjalankan {@code check(...)} lalu menulis hasilnya balik ke field (getter
	 * destruktif ringan) sehingga proxy yang sudah terlepas session tetap aman dibaca dari
	 * renderer. Kolom bersifat {@code NOT NULL}.</p>
	 *
	 * @return petugas pembuat dokumen.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = false)
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);
		return dibuatOleh;
	}

	/**
	 * Menyetel petugas penyetuju dokumen.
	 *
	 * <p><b>Catatan integritas:</b> setter tidak memeriksa bahwa penyetuju berbeda dari
	 * pembuat, tidak memeriksa hak akses, dan tidak menolak perubahan pada dokumen yang sudah
	 * disetujui. Pada dokumen pengembalian hal ini penting karena persetujuan sekaligus
	 * membekukan tanggal efektif yang dipakai menghitung denda.</p>
	 *
	 * @param disetujuiOleh petugas penyetuju; {@code null} mengembalikan dokumen ke status draf.
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengembalikan petugas penyetuju dokumen.
	 *
	 * <p>Seperti {@link #getDibuatOleh()}, getter menjalankan {@code check(...)} dan menulis
	 * hasilnya balik ke field.</p>
	 *
	 * @return petugas penyetuju, atau {@code null} bila dokumen masih berstatus draf.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujuiOleh = check(disetujuiOleh);
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
	 * Mengembalikan tanggal pembuatan dokumen, dengan pengisian otomatis waktu server bila
	 * belum diisi.
	 *
	 * <p>Berbeda dari {@link ReturPengadaanItem#getTanggalPembuatan()} yang hanya
	 * mengembalikan nilai fallback, getter ini <b>menuliskannya balik ke field</b> lebih dahulu
	 * sehingga nilai yang tersimpan konsisten dengan yang dibaca. Baris ternary di akhir method
	 * karenanya sudah tidak pernah mengambil cabang {@code null}-nya; ia peninggalan versi
	 * sebelumnya yang dibiarkan apa adanya.</p>
	 *
	 * <p>Efek sampingnya tetap perlu disadari: memanggil getter ini pada objek yang dikelola
	 * session akan menandai entity sebagai kotor dan memicu {@code UPDATE} pada flush
	 * berikutnya, meski pemanggil hanya bermaksud membaca.</p>
	 *
	 * @return tanggal pembuatan dokumen; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		if (tanggalPembuatan == null) {
			tanggalPembuatan = ais.ui.util.WaktuUtil.getDate();
		}
		return tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan;
	}

	/**
	 * Menyetel tanggal persetujuan dokumen.
	 *
	 * <p><b>Berdampak pada denda.</b> Nilai ini menjadi tanggal efektif pengembalian bagi baris
	 * rincian yang belum punya tanggalnya sendiri (lihat
	 * {@link KembaliPengadaanItemDetail#getTanggal()}), dan tanggal itulah yang dipakai
	 * menghitung jumlah hari keterlambatan. Memundurkan atau memajukan nilai ini secara efektif
	 * mengubah besaran denda anggota, dan model tidak membatasinya sama sekali &mdash; tanggal
	 * di masa depan maupun sebelum tanggal peminjaman sama-sama diterima.</p>
	 *
	 * @param tanggalPersetujuan tanggal persetujuan; {@code null} berarti belum disetujui.
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengembalikan tanggal persetujuan dokumen. Tidak memakai fallback: nilai {@code null}
	 * adalah penanda sah bahwa pengembalian belum disahkan petugas.
	 *
	 * @return tanggal persetujuan, atau {@code null} bila dokumen masih draf.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {
		return tanggalPersetujuan;
	}

	/**
	 * Menyetel dokumen peminjaman yang dikembalikan lewat dokumen ini.
	 *
	 * <p>Pemanggil bertanggung jawab mengisi pula sisi sebaliknya
	 * ({@code peminjamanPengadaanItem.setKembaliPengadaanItem(this)}) karena kedua penunjuk
	 * adalah kolom {@code ManyToOne} mandiri yang tidak disinkronkan Hibernate.</p>
	 *
	 * @param peminjamanPengadaanItem dokumen peminjaman asal.
	 */
	public void setPeminjamanPengadaanItem(PeminjamanPengadaanItem peminjamanPengadaanItem) {
		this.peminjamanPengadaanItem = peminjamanPengadaanItem;
	}

	/**
	 * Mengembalikan dokumen peminjaman yang dikembalikan lewat dokumen ini.
	 *
	 * <p>Relasi ini bersifat kasar: satu dokumen pengembalian menunjuk satu dokumen peminjaman.
	 * Untuk pengembalian sebagian, pemetaan yang akurat justru berada pada tingkat rincian
	 * ({@link KembaliPengadaanItemDetail#getPeminjamanPengadaanItemDetail()}), dan itulah yang
	 * dibaca {@code LibraryUtil} saat menghitung keterlambatan per eksemplar.</p>
	 *
	 * @return dokumen peminjaman asal, atau {@code null} bila belum dikaitkan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "peminjaman_pengadaan_item", nullable = true)
	public PeminjamanPengadaanItem getPeminjamanPengadaanItem() {
		return peminjamanPengadaanItem;
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
	 * Mengembalikan perpustakaan (tenant) tempat pengembalian dilayani, dengan <b>pengisian
	 * otomatis</b> dari sesi bila belum diisi.
	 *
	 * <p>Alur getter ini: (1) bila field masih {@code null}, ambil perpustakaan aktif melalui
	 * {@link Common#getCurrentPerpustakaan()}; (2) jalankan {@code check(...)} milik
	 * {@link GeneralValueObject}. Hasilnya ditulis balik ke field, sehingga getter ini mengubah
	 * state objek (getter destruktif ringan).</p>
	 *
	 * <p><b>Konsekuensi pada denda.</b> Perpustakaan bukan sekadar penanda tenant di sini:
	 * {@code LibraryUtil.hitungDendaItem(...)} dan {@code LibraryUtil.getJumlahHariBatas(...)}
	 * memilih aturan {@link DendaKeterlambatanItem} dan {@link BatasWaktuPeminjamanItem}
	 * berdasarkan perpustakaan. Nilai yang salah karena terisi otomatis dari sesi petugas yang
	 * berbeda akan menghasilkan tarif denda dan tenggat yang salah pula.</p>
	 *
	 * @return perpustakaan pelayan pengembalian; dapat {@code null} bila sesi juga tidak
	 *         memilikinya.
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
	 * Menyetel perpustakaan (tenant) tempat pengembalian dilayani.
	 *
	 * @param perpustakaan perpustakaan pelayan.
	 */
	public void setPerpustakaan(Perpustakaan perpustakaan) {
		this.perpustakaan = perpustakaan;
	}

	// public Double getDenda() {
	// denda = peminjamanPengadaanItem == null ? 0.0 :
	// peminjamanPengadaanItem.getDendaTotal();
	// return denda;
	// }
	//
	// public void setDenda(Double denda) {
	// this.denda = denda;
	// }

	/**
	 * Mengembalikan kunjungan anggota tempat transaksi pengembalian ini terjadi.
	 *
	 * <p>Dipakai untuk statistik kunjungan perpustakaan; kolomnya {@code nullable} sehingga
	 * pengembalian yang diproses tanpa pencatatan kunjungan (misalnya lewat API atau
	 * <i>bookdrop</i>) tetap tersimpan.</p>
	 *
	 * @return kunjungan anggota terkait, atau {@code null} bila tidak dicatat.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kunjungan_anggota", nullable = true)
	public KunjunganAnggota getKunjunganAnggota() {
		return kunjunganAnggota;
	}

	/**
	 * Menyetel kunjungan anggota tempat transaksi pengembalian ini terjadi.
	 *
	 * @param kunjunganAnggota kunjungan anggota terkait.
	 */
	public void setKunjunganAnggota(KunjunganAnggota kunjunganAnggota) {
		this.kunjunganAnggota = kunjunganAnggota;
	}

	// public boolean hitungDenda() {
	//
	// System.out.println("peminjamanPengadaanItem ==> " +
	// peminjamanPengadaanItem);
	//
	// if (id != null && peminjamanPengadaanItem != null) {
	//
	// peminjamanPengadaanItem.setDendaKeterlambatanPerItem(LibraryUtil.hitungDendaPerItem(this));
	// peminjamanPengadaanItem
	// .setDendaKeterlambatan(LibraryUtil.hitungDenda(peminjamanPengadaanItem,
	// getTanggalPembuatan()));
	//
	// int total = (int)
	// (peminjamanPengadaanItem.getDendaKeterlambatanPerItem().doubleValue()
	// + peminjamanPengadaanItem.getDendaKeterlambatan().doubleValue());
	// if (peminjamanPengadaanItem.getDendaTotal() == null
	// || peminjamanPengadaanItem.getDendaTotal().intValue() != total) {
	// return true;
	// }
	// }
	// return false;
	// }
	//
	// public Double getDiskonDenda() {
	// if (diskonDenda == null) {
	// diskonDenda = 0.0;
	// }
	// return diskonDenda;
	// }
	//
	// public void setDiskonDenda(Double diskonDenda) {
	// this.diskonDenda = diskonDenda;
	// }
}
