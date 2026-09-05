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
 * Entity <b>header</b> dokumen <b>peminjaman buku oleh anggota</b> (tabel
 * {@code library.peminjaman_pengadaan_item}). Satu baris merepresentasikan satu transaksi
 * sirkulasi ketika seorang {@link Anggota} meminjam satu atau beberapa eksemplar dari
 * perpustakaan. Rincian per eksemplar &mdash; termasuk tenggat dan perpanjangan &mdash; berada
 * pada {@link PeminjamanPengadaanItemDetail}.
 *
 * <h3>Nama yang menyesatkan: ini sirkulasi, bukan pengadaan</h3>
 * <p>Meski namanya mengandung kata "PengadaanItem", entity ini <b>tidak ada hubungannya dengan
 * pengadaan barang dari pemasok</b>. Ia adalah dokumen peminjaman koleksi kepada anggota. Yang
 * membuktikannya adalah isi kelas ini sendiri:</p>
 * <ul>
 *   <li>relasi ke {@link #getAnggota() Anggota} &mdash; peminjamnya adalah anggota
 *       perpustakaan, bukan penyedia;</li>
 *   <li><b>tidak ada</b> relasi ke {@link Penyedia}, {@link PemesananPengadaanItem}, maupun
 *       {@link PenerimaanPengadaanItem} &mdash; padahal ketiganya adalah ciri wajib dokumen
 *       pengadaan sesungguhnya seperti {@link ReturPengadaanItem};</li>
 *   <li>relasi ke {@link #getKembaliPengadaanItem() KembaliPengadaanItem} (pengembalian oleh
 *       anggota) dan ke {@link #getKunjunganAnggota() KunjunganAnggota} (statistik kunjungan);</li>
 *   <li>parameter sirkulasi {@link #getJumlahHariBatas() jumlahHariBatas} (durasi pinjam) dan
 *       {@link #getJumlahMaksimalPeminjaman() jumlahMaksimalPeminjaman} (kuota eksemplar), yang
 *       diambil dari {@link BatasWaktuPeminjamanItem} sesuai profil anggota;</li>
 *   <li>rinciannya menyimpan {@link ItemPunyaBarcode} (eksemplar fisik yang dipindai di meja
 *       sirkulasi), tanggal batas pengembalian, jumlah hari terlambat, dan cacah perpanjangan.</li>
 * </ul>
 * <p>Kata "Pengadaan" pada nama ini semata-mata warisan penamaan seragam ketika seluruh dokumen
 * modul {@code library} dibangkitkan dari template yang sama; jangan menyimpulkan makna dari nama
 * kelas di paket ini. Ringkasnya, pasangan yang benar adalah:
 * <b>{@code PeminjamanPengadaanItem} &harr; {@link KembaliPengadaanItem}</b> (sirkulasi anggota),
 * sementara {@link PenerimaanPengadaanItem} &harr; {@link ReturPengadaanItem} adalah pasangan
 * pengadaan dengan penyedia.</p>
 *
 * <h3>Batas waktu dan kuota</h3>
 * <p>Dua parameter sirkulasi disalin ke header ini pada saat transaksi dibuat, bukan dibaca ulang
 * setiap kali diperlukan:</p>
 * <ul>
 *   <li>{@link #getJumlahHariBatas() jumlahHariBatas} &mdash; durasi pinjam dasar dalam hari
 *       kerja, diambil {@code LibraryUtil.getJumlahHariBatas(anggota, perpustakaan)} dari
 *       {@link BatasWaktuPeminjamanItem} yang paling cocok dengan jenis anggota, tipe anggota,
 *       fakultas, jurusan, semester berjalan, dan perpustakaan. Rincian mengalikannya dengan
 *       {@code (jumlahPerpanjangan + 1)} untuk memperoleh tenggat efektif;</li>
 *   <li>{@link #getJumlahMaksimalPeminjaman() jumlahMaksimalPeminjaman} &mdash; kuota eksemplar
 *       yang boleh dipinjam serentak, dari {@code LibraryUtil.getJumlahMaksimalPeminjaman(...)}.
 *       Kuota ini <b>benar-benar ditegakkan</b>: {@code LibraryUtil.getKuota(...)} menghitung
 *       peminjaman yang masih berjalan dan {@code helper/PeminjamanPengadaanItemPunyaItemHelper}
 *       menolak penambahan eksemplar bila kuota terlampaui.</li>
 * </ul>
 * <p>Karena keduanya disalin, mengubah aturan {@link BatasWaktuPeminjamanItem} <b>tidak</b>
 * memengaruhi peminjaman yang sudah berjalan &mdash; perilaku yang memang diinginkan agar tenggat
 * yang sudah diberitahukan kepada anggota tidak berubah sepihak.</p>
 *
 * <h3>Denda: dipindahkan keluar dari entity ini</h3>
 * <p>Blok besar di bagian bawah berkas ini &mdash; field {@code dendaKeterlambatanPerItem},
 * {@code dendaKeterlambatan}, {@code dendaTotal} beserta {@code getDendaTotal()},
 * {@code hitungDendaTotal()}, dan {@code hitungDenda()} &mdash; seluruhnya dinonaktifkan sebagai
 * komentar sumber. Desain lama mengakumulasi denda pada header peminjaman; desain yang berlaku
 * sekarang menghitung denda per eksemplar lewat {@code LibraryUtil.hitungDendaItem(...)} dan
 * menyimpannya pada {@link KembaliPengadaanItemDetail#getDenda()}. Konsekuensi penting: mekanisme
 * <i>diskon denda</i> yang dulu ada pada rumus {@code getDendaTotal()} ikut hilang dan tidak
 * digantikan &mdash; keringanan sekarang hanya mungkin lewat pembebasan pada tingkat rincian
 * pengembalian. Jangan mengaktifkan kembali blok komentar tersebut tanpa memeriksa
 * {@code LibraryUtil} terlebih dahulu, karena rumus lama akan menghitung ganda.</p>
 *
 * <p><b>Tidak ada gerbang tunggakan.</b> Perlu ditegaskan bahwa entity ini &mdash; dan alur
 * peminjaman pada umumnya &mdash; tidak memeriksa apakah anggota masih punya denda yang belum
 * dibayar sebelum mengizinkan peminjaman baru. Yang ditegakkan hanyalah kuota jumlah eksemplar.
 * Pemblokiran anggota bergantung pada entity terpisah {@code AnggotaYangDiblokir} yang diisi
 * petugas secara manual, bukan diturunkan otomatis dari tunggakan denda.</p>
 *
 * <p><b>Efek samping.</b> Accessor dan mutator hanya membaca atau mengubah state di memori,
 * kecuali {@link #getDibuatOleh()}, {@link #getDisetujuiOleh()}, {@link #getPerpustakaan()},
 * {@link #getTanggalPembuatan()}, dan {@link #getJumlahHariBatas()} yang menulis balik hasilnya
 * ke field. Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung
 * jawab DAO/service dengan session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see PeminjamanPengadaanItemDetail
 * @see KembaliPengadaanItem
 * @see BatasWaktuPeminjamanItem
 * @see Anggota
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "library", name = "peminjaman_pengadaan_item")
public class PeminjamanPengadaanItem extends GeneralValueObject {

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
	 * {@link Anggota}. Nama peminjam tidak ikut tampil.</p>
	 *
	 * @return kode dokumen peminjaman; dapat {@code null} untuk objek yang belum diberi kode.
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

	/** Kode/nomor dokumen peminjaman; unik pada tabel dan dipakai sebagai identitas manusiawi. */
	private String kode;
	/** Catatan bebas pada dokumen peminjaman. */
	private String keterangan;
	/** Perpustakaan (tenant) tempat peminjaman dilayani; menentukan aturan tenggat dan denda. */
	private Perpustakaan perpustakaan;
	/** Anggota peminjam &mdash; bukti bahwa dokumen ini adalah sirkulasi, bukan pengadaan. */
	private Anggota anggota;
	/** Tanggal peminjaman; menjadi titik awal perhitungan tenggat dan keterlambatan. */
	private Date tanggalPembuatan;
	/** Tanggal dokumen disetujui; {@code null} selama dokumen masih draf. */
	private Date tanggalPersetujuan;
	/** Petugas pembuat dokumen. */
	private Tbmuser dibuatOleh;
	/** Petugas penyetuju dokumen; {@code null} selama dokumen masih draf. */
	private Tbmuser disetujuiOleh;

	/** Dokumen pengembalian pasangannya; {@code null} selama pinjaman belum dikembalikan. */
	private KembaliPengadaanItem kembaliPengadaanItem;

	/** Kunjungan anggota tempat transaksi peminjaman ini terjadi (statistik kunjungan). */
	private KunjunganAnggota kunjunganAnggota;

//	private Double dendaKeterlambatanPerItem;
//	private Double dendaKeterlambatan;
//	private Double dendaTotal;
	/** Durasi pinjam dasar dalam hari kerja, disalin dari {@link BatasWaktuPeminjamanItem}. */
	private Integer jumlahHariBatas;
	/** Kuota eksemplar yang boleh dipinjam serentak, disalin dari {@link BatasWaktuPeminjamanItem}. */
	private Integer jumlahMaksimalPeminjaman;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate/ZK untuk instansiasi via refleksi.
	 */
	public PeminjamanPengadaanItem() {
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
	 * Mengembalikan kode/nomor dokumen peminjaman.
	 *
	 * @return kode dokumen; wajib terisi dan unik pada tabel.
	 */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return this.kode;
	}

	/**
	 * Menyetel kode/nomor dokumen peminjaman. Keunikan hanya dijaga oleh constraint database.
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
	 * disetujui.</p>
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
	 * Menyetel tanggal peminjaman.
	 *
	 * <p><b>Berdampak pada tenggat dan denda:</b> nilai ini menjadi titik awal perhitungan
	 * {@link PeminjamanPengadaanItemDetail#getBatasWaktupengembalian()} dan
	 * {@link PeminjamanPengadaanItemDetail#getJumlahSelisihHari()}. Memajukan tanggal
	 * peminjaman memperpanjang tenggat sekaligus mengurangi hari keterlambatan, dan model tidak
	 * membatasi rentangnya sama sekali.</p>
	 *
	 * @param tanggalPembuatan tanggal peminjaman.
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Mengembalikan tanggal peminjaman, dengan pengisian otomatis waktu server bila belum
	 * diisi.
	 *
	 * <p>Berbeda dari {@link ReturPengadaanItem#getTanggalPembuatan()} yang hanya mengembalikan
	 * nilai fallback, getter ini <b>menuliskannya balik ke field</b> lebih dahulu sehingga nilai
	 * yang tersimpan konsisten dengan yang dibaca. Baris ternary di akhir method karenanya sudah
	 * tidak pernah mengambil cabang {@code null}-nya; ia peninggalan versi sebelumnya yang
	 * dibiarkan apa adanya.</p>
	 *
	 * <p>Efek sampingnya perlu disadari: memanggil getter ini pada objek yang dikelola session
	 * akan menandai entity sebagai kotor dan memicu {@code UPDATE} pada flush berikutnya, meski
	 * pemanggil hanya bermaksud membaca. Karena tanggal ini adalah titik awal perhitungan
	 * tenggat, pembacaan yang tidak disengaja pada objek yang belum punya tanggal akan
	 * membekukan "hari ini" sebagai tanggal pinjam.</p>
	 *
	 * @return tanggal peminjaman; tidak pernah {@code null} setelah pemanggilan pertama.
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
	 * @param tanggalPersetujuan tanggal persetujuan; {@code null} berarti belum disetujui.
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengembalikan tanggal persetujuan dokumen. Tidak memakai fallback: nilai {@code null}
	 * adalah penanda sah bahwa peminjaman belum disahkan petugas.
	 *
	 * @return tanggal persetujuan, atau {@code null} bila dokumen masih draf.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {
		return tanggalPersetujuan;
	}

	/**
	 * Menyetel perpustakaan (tenant) tempat peminjaman dilayani.
	 *
	 * @param perpustakaan perpustakaan pelayan.
	 */
	public void setPerpustakaan(Perpustakaan perpustakaan) {
		this.perpustakaan = perpustakaan;
	}

	/**
	 * Mengembalikan perpustakaan (tenant) tempat peminjaman dilayani, dengan <b>pengisian
	 * otomatis</b> dari sesi bila belum diisi.
	 *
	 * <p>Alur getter ini: (1) bila field masih {@code null}, ambil perpustakaan aktif melalui
	 * {@link Common#getCurrentPerpustakaan()}; (2) jalankan {@code check(...)} milik
	 * {@link GeneralValueObject}. Hasilnya ditulis balik ke field, sehingga getter ini mengubah
	 * state objek (getter destruktif ringan).</p>
	 *
	 * <p><b>Perpustakaan menentukan aturan, bukan sekadar tenant.</b>
	 * {@code LibraryUtil.getJumlahHariBatas(...)},
	 * {@code LibraryUtil.getJumlahMaksimalPeminjaman(...)}, dan
	 * {@code LibraryUtil.hitungDendaItem(...)} semuanya menyaring
	 * {@link BatasWaktuPeminjamanItem} dan {@link DendaKeterlambatanItem} berdasarkan nilai ini.
	 * Nilai yang terisi otomatis dari sesi petugas yang sedang bertugas di perpustakaan lain
	 * akan menghasilkan durasi pinjam, kuota, dan tarif denda milik perpustakaan yang keliru.</p>
	 *
	 * @return perpustakaan pelayan peminjaman; dapat {@code null} bila sesi juga tidak
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
	 * Mengembalikan anggota peminjam.
	 *
	 * <p>Relasi ini adalah inti dokumen: dari {@link Anggota} inilah {@code LibraryUtil}
	 * mengambil jenis anggota, tipe anggota, serta fakultas/jurusan (lewat mahasiswa atau dosen
	 * yang tertaut) untuk memilih aturan {@link BatasWaktuPeminjamanItem} dan
	 * {@link DendaKeterlambatanItem} yang berlaku. Relasi dipetakan {@link FetchMode#SELECT}
	 * (eager per baris) sehingga aman dibaca dari renderer ZK.</p>
	 *
	 * <p><b>Catatan:</b> kolomnya {@code nullable}. Dokumen peminjaman tanpa anggota tidak
	 * bermakna secara bisnis, dan lebih jauh lagi akan membuat
	 * {@code LibraryUtil.hitungDendaItem(...)} melempar {@code NullPointerException} karena
	 * langsung memanggil {@code getAnggota().getJenisAnggota()} tanpa pemeriksaan
	 * {@code null}.</p>
	 *
	 * @return anggota peminjam, atau {@code null} bila belum ditentukan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "anggota", nullable = true)
	public Anggota getAnggota() {
		return anggota;
	}

	/**
	 * Menyetel anggota peminjam.
	 *
	 * <p><b>Catatan integritas:</b> setter tidak memeriksa status keanggotaan (aktif,
	 * kedaluwarsa, diblokir) dan tidak memeriksa apakah anggota masih memiliki tunggakan denda.
	 * Kedua pemeriksaan itu &mdash; bila memang dikehendaki &mdash; harus dilakukan lapisan
	 * action sebelum dokumen disimpan.</p>
	 *
	 * @param anggota anggota peminjam.
	 */
	public void setAnggota(Anggota anggota) {
		this.anggota = anggota;
	}

	/**
	 * Mengembalikan dokumen pengembalian pasangan peminjaman ini.
	 *
	 * <p>Nilai {@code null} berarti pinjaman belum dikembalikan. Penunjuk ini berpasangan
	 * dengan {@code peminjamanPengadaanItem} pada {@link KembaliPengadaanItem}, tetapi keduanya
	 * adalah kolom {@code ManyToOne} mandiri (bukan {@code mappedBy}), sehingga Hibernate tidak
	 * menyinkronkannya.</p>
	 *
	 * <p><b>Jangan memakai penunjuk ini untuk menentukan apakah sebuah eksemplar sudah
	 * kembali.</b> Untuk pengembalian sebagian, relasi tingkat header terlalu kasar; yang sahih
	 * adalah {@link PeminjamanPengadaanItemDetail#getKembaliPengadaanItemDetail()} pada tingkat
	 * rincian, dan itulah yang dibaca perhitungan keterlambatan.</p>
	 *
	 * @return dokumen pengembalian pasangannya, atau {@code null} bila belum dikembalikan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kembali_pengadaan_item", nullable = true)
	public KembaliPengadaanItem getKembaliPengadaanItem() {
		return kembaliPengadaanItem;
	}

	/**
	 * Menyetel dokumen pengembalian pasangan peminjaman ini.
	 *
	 * @param kembaliPengadaanItem dokumen pengembalian pasangannya.
	 */
	public void setKembaliPengadaanItem(KembaliPengadaanItem kembaliPengadaanItem) {
		this.kembaliPengadaanItem = kembaliPengadaanItem;
	}

	/**
	 * Mengembalikan kunjungan anggota tempat transaksi peminjaman ini terjadi.
	 *
	 * <p>Dipakai untuk statistik kunjungan perpustakaan; kolomnya {@code nullable} sehingga
	 * peminjaman yang diproses tanpa pencatatan kunjungan (misalnya lewat API layanan mandiri)
	 * tetap tersimpan.</p>
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
	 * Menyetel kunjungan anggota tempat transaksi peminjaman ini terjadi.
	 *
	 * @param kunjunganAnggota kunjungan anggota terkait.
	 */
	public void setKunjunganAnggota(KunjunganAnggota kunjunganAnggota) {
		this.kunjunganAnggota = kunjunganAnggota;
	}

//	public Double getDendaKeterlambatanPerItem() {
//		if (dendaKeterlambatanPerItem == null) {
//			dendaKeterlambatanPerItem = 0.0;
//		}
//		return dendaKeterlambatanPerItem;
//	}
//
//	public void setDendaKeterlambatanPerItem(Double dendaKeterlambatanPerItem) {
//		this.dendaKeterlambatanPerItem = dendaKeterlambatanPerItem;
//	}
//
//	public Double getDendaKeterlambatan() {
//		if (dendaKeterlambatan == null) {
//			dendaKeterlambatan = 0.0;
//		}
//		return dendaKeterlambatan;
//	}
//
//	public void setDendaKeterlambatan(Double dendaKeterlambatan) {
//		this.dendaKeterlambatan = dendaKeterlambatan;
//	}
//
//	public Double getDendaTotal() {
//		dendaTotal = getDendaKeterlambatan() + getDendaKeterlambatanPerItem();
//		if (kembaliPengadaanItem != null) {
//			dendaTotal = dendaTotal - (dendaTotal * kembaliPengadaanItem.getDiskonDenda() / 100.0);
//		}
//		return dendaTotal;
//	}

//	public Double hitungDendaTotal(KembaliPengadaanItem kembaliPengadaanItem) {
//		dendaTotal = getDendaKeterlambatan() + getDendaKeterlambatanPerItem();
//		if (kembaliPengadaanItem != null) {
//			dendaTotal = dendaTotal - (dendaTotal * kembaliPengadaanItem.getDiskonDenda() / 100.0);
//		}
//		return dendaTotal;
//	}
//
//	public void setDendaTotal(Double dendaTotal) {
//		this.dendaTotal = dendaTotal;
//	}

//	public boolean hitungDenda() {
//		if (id != null) {
//
//			System.out.println("kembaliPengadaanItem => " + kembaliPengadaanItem);
//
//			dendaKeterlambatanPerItem = LibraryUtil.hitungDendaPerItem(kembaliPengadaanItem);
//			dendaKeterlambatan = LibraryUtil.hitungDenda(this,
//					kembaliPengadaanItem == null ? ais.ui.util.WaktuUtil.getDate() : kembaliPengadaanItem.getTanggalPembuatan());
//
//			int total = (int) (dendaKeterlambatanPerItem.doubleValue() + dendaKeterlambatan.doubleValue());
//			if (dendaTotal == null || dendaTotal.intValue() != total) {
//				return true;
//			}
//		}
//		return false;
//	}

	/**
	 * Mengembalikan durasi pinjam dasar dalam hari kerja, dinormalkan ke {@code 0} bila belum
	 * diisi.
	 *
	 * <p>Nilai ini disalin dari {@link BatasWaktuPeminjamanItem} oleh
	 * {@code LibraryUtil.getJumlahHariBatas(anggota, perpustakaan)} pada saat transaksi dibuat,
	 * dan sejak itu menjadi milik dokumen &mdash; mengubah aturan tenggat di kemudian hari tidak
	 * memengaruhi peminjaman yang sudah berjalan. Rincian mengalikannya dengan
	 * {@code (jumlahPerpanjangan + 1)} lewat
	 * {@link PeminjamanPengadaanItemDetail#getJumlahHariBatas()} untuk memperoleh tenggat
	 * efektif setelah perpanjangan.</p>
	 *
	 * <p><b>Nilai baku nol berarti tanpa tenggat, bukan tenggat nol hari yang aman.</b>
	 * {@code LibraryUtil.getJumlahHariBatas(...)} mengembalikan {@code 0} untuk tiga keadaan
	 * yang berbeda: tidak ada aturan {@link BatasWaktuPeminjamanItem} yang cocok dengan profil
	 * anggota, anggota atau perpustakaan bernilai {@code null}, dan terjadi galat saat
	 * membaca aturan (blok {@code catch}-nya mengembalikan {@code 0}). Ketiganya menghasilkan
	 * batas nol hari, yang berarti seluruh pinjaman langsung dianggap terlambat sejak hari
	 * pertama. Ini kegagalan yang <i>fail-closed</i> terhadap anggota (denda maksimal) namun
	 * senyap: tidak ada peringatan bahwa aturan tenggatnya sebenarnya tidak ditemukan.</p>
	 *
	 * <p>Normalisasi {@code null} ditulis balik ke field, sehingga getter ini mengubah state
	 * objek.</p>
	 *
	 * @return durasi pinjam dasar dalam hari kerja; tidak pernah {@code null}.
	 */
	public Integer getJumlahHariBatas() {
		if (jumlahHariBatas == null) {
			jumlahHariBatas = 0;
		}
		return jumlahHariBatas;
	}

	/**
	 * Menyetel durasi pinjam dasar dalam hari kerja.
	 *
	 * <p>Dipanggil {@code LibraryUtil.hitungDenda(...)} dan helper sirkulasi dengan hasil
	 * pencarian aturan {@link BatasWaktuPeminjamanItem}. Setter tidak menolak nilai negatif,
	 * yang akan membuat tenggat jatuh sebelum tanggal peminjaman.</p>
	 *
	 * @param jumlahHariBatas durasi pinjam dasar dalam hari kerja.
	 */
	public void setJumlahHariBatas(Integer jumlahHariBatas) {
		this.jumlahHariBatas = jumlahHariBatas;
	}

	/**
	 * Mengembalikan kuota eksemplar yang boleh dipinjam serentak oleh anggota ini.
	 *
	 * <p>Disalin dari {@link BatasWaktuPeminjamanItem#getJumlahMaksimalItemYangDipinjam()} oleh
	 * {@code LibraryUtil.getJumlahMaksimalPeminjaman(...)}. Kuota ini <b>benar-benar
	 * ditegakkan</b>: {@code LibraryUtil.getKuota(...)} menghitung selisih antara peminjaman dan
	 * pengembalian yang tercatat, lalu {@code helper/PeminjamanPengadaanItemPunyaItemHelper}
	 * menolak penambahan eksemplar bila kuota terlampaui.</p>
	 *
	 * <p><b>Berbeda dari {@link #getJumlahHariBatas()}, getter ini tidak menormalkan
	 * {@code null}.</b> Dokumen lama yang kolomnya belum terisi akan mengembalikan {@code null},
	 * dan pemanggil yang langsung melakukan {@code intValue()} atau membandingkannya dengan
	 * {@code int} akan memicu {@code NullPointerException} saat <i>auto-unboxing</i>.
	 * Ketidakseragaman ini terjadi karena kedua field ditambahkan pada waktu yang berbeda.</p>
	 *
	 * @return kuota eksemplar serentak, atau {@code null} bila belum disalin.
	 */
	public Integer getJumlahMaksimalPeminjaman() {
		return jumlahMaksimalPeminjaman;
	}

	/**
	 * Menyetel kuota eksemplar yang boleh dipinjam serentak oleh anggota ini.
	 *
	 * @param jumlahMaksimalPeminjaman kuota eksemplar serentak.
	 */
	public void setJumlahMaksimalPeminjaman(Integer jumlahMaksimalPeminjaman) {
		this.jumlahMaksimalPeminjaman = jumlahMaksimalPeminjaman;
	}

}
