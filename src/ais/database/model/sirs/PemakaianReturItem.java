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

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.asset.Lokasi;

/**
 * Entitas <b>Pemakaian Retur Item</b> medis pada schema {@code sirs} (tabel
 * {@code pemakaian_retur_item}). Merepresentasikan pengembalian item medis yang
 * sebelumnya sudah dikeluarkan lewat {@link PemakaianItem} namun ternyata tidak
 * jadi dipakai, sehingga stok di {@link #getLokasi()} BERTAMBAH kembali dan
 * beban yang sudah timbul berkurang. Baris-baris itemnya ada di
 * {@link PemakaianReturItemDetail}.
 *
 * <h2>Cermin dari PemakaianItem</h2>
 * <p>
 * Entitas ini adalah pasangan kebalikan {@link PemakaianItem}, dan
 * kemiripannya hampir sempurna: keduanya punya {@link #getKode()},
 * {@link #getKeterangan()}, {@link #getLokasi()}, {@link #getPegawai()}, jejak
 * pembuatan dan persetujuan yang sama bentuknya, sama-sama tanpa jejak
 * pembatalan, sama-sama tanpa relasi ke {@code PostingHistory}, dan baris
 * detailnya identik struktur kolomnya. Satu-satunya pembeda struktural adalah
 * kolom {@code keperluan} yang dimiliki {@link PemakaianItem} namun tidak ada
 * di sini — wajar, karena pengembalian barang tidak punya "keperluan".
 * </p>
 * <p>
 * Selebihnya yang membedakan keduanya hanyalah nama: nama kelas, nama tabel
 * ({@code sirs.pemakaian_retur_item} versus
 * {@code sirs.pemakaian_item}), nama kolom induk pada baris detailnya
 * ({@code pemakaian_retur_item} versus {@code pemakaian_item}), dan ARAH mutasi
 * stoknya.
 * </p>
 * <p>
 * <b>Kewaspadaan yang dituntut kemiripan ini.</b> Kode yang menangani retur
 * pemakaian sangat mudah lahir dari penyalinan kode pemakaian. Bila dalam
 * penyalinan itu ada satu nama tabel atau satu nama kolom yang tertinggal
 * belum disesuaikan, tidak ada yang akan menolaknya: kompilasi tetap berhasil
 * karena nama tabel dan kolom hidup di dalam string SQL, dan eksekusinya pun
 * tetap berhasil karena tabel yang salah itu memang ada. Yang lebih berbahaya,
 * PK kedua tabel berjalan pada sequence masing-masing, sehingga ID dokumen
 * retur dapat kebetulan sama dengan ID dokumen pemakaian yang berbeda —
 * akibatnya query akan diam-diam mengenai baris milik dokumen orang lain
 * alih-alih gagal. Kesalahan semacam itu tidak akan tampak pada pengujian
 * biasa dan hanya terlihat sebagai selisih stok yang tak terjelaskan.
 * </p>
 *
 * <h2>Catatan integritas</h2>
 * <p>
 * Skema tidak menautkan dokumen ini ke dokumen {@link PemakaianItem} yang
 * menjadi asal barangnya — tidak di tingkat header maupun di tingkat baris.
 * Ini berbeda dari jalur pembelian, yang menautkan retur ke penerimaannya
 * lewat {@link PenerimaanOrderKembali#getPenerimaanOrder()} dan
 * {@link PenerimaanOrderKembaliDetail#getPenerimaanOrderDetail()}. Akibatnya
 * penjaga "retur tidak melebihi yang pernah dipakai" TIDAK dapat ditulis
 * dengan cara yang sama seperti pada jalur pembelian: tidak ada tautan untuk
 * ditelusuri, sehingga pencocokan hanya mungkin secara heuristik lewat item
 * dan lokasi. Retur pemakaian karena itu praktis merupakan jalur PENAMBAHAN
 * stok yang tidak terikat pada apa pun — mendekati kewenangan
 * {@link KoreksiItemMedis}, tetapi tanpa kesan sebagai dokumen koreksi.
 * </p>
 * <p>
 * Tidak ada pula jejak pembatalan, sehingga membatalkan retur berarti
 * mengosongkan kembali {@link #getTanggalPersetujuan()} dan menghapus jejak
 * bahwa stok pernah ditambah.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "pemakaian_retur_item")
public class PemakaianReturItem extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java untuk entitas ini, dipatok agar object
	 * yang sudah terserialisasi tetap kompatibel meski struktur kelas berubah.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private Long index;
	private String olehId;

	/**
	 * Mengambil ID pengguna yang terakhir mengubah dokumen retur pemakaian ini.
	 * Field audit shadow yang diisi lewat {@link #setOlehId(String)}.
	 *
	 * @return ID pengguna terakhir yang mengubah, atau {@code null} jika belum
	 *         pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna yang mengubah dokumen retur pemakaian ini. Nilai
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
	 * Representasi ringkas dokumen retur pemakaian ini untuk tampilan
	 * combobox/listbox ZK dan log, memakai {@link #getKode()} sebagai label.
	 * Akan mengembalikan {@code null} bila kode belum di-generate.
	 *
	 * @return kode dokumen retur pemakaian ini.
	 */
	public String toString() {
		return kode;
	}

	/**
	 * Menetapkan nama pengguna yang mengubah dokumen retur pemakaian ini. Nilai
	 * kosong/blank diabaikan agar field audit ini tidak pernah ditimpa kosong.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau kosong/whitespace-saja
	 *             tidak akan mengubah nilai tersimpan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah dokumen retur pemakaian
	 * ini.
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
	private Lokasi lokasi;
	private Date tanggalPembuatan;
	private Date tanggalPersetujuan;
	private Tbmuser dibuatOleh;
	private Tbmuser disetujuiOleh;

	private Pegawai pegawai;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public PemakaianReturItem() {
	}

	/**
	 * Primary key dokumen retur pemakaian ini, auto-increment (IDENTITY) dan
	 * diisi database.
	 *
	 * @return ID unik dokumen retur pemakaian ini, atau {@code null} untuk
	 *         baris yang belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID dokumen retur pemakaian ini.
	 *
	 * @param id ID dokumen retur pemakaian.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil kode/nomor dokumen retur pemakaian ini. Kolom {@code NOT NULL}
	 * dan {@code UNIQUE} di database sehingga berperan sebagai identitas bisnis
	 * dokumen, di samping {@link #getId()} yang merupakan identitas teknis.
	 *
	 * <p>
	 * Perlu diingat bahwa {@link #getId()} milik dokumen retur pemakaian
	 * berasal dari sequence yang BERBEDA dari sequence dokumen
	 * {@link PemakaianItem}, sehingga nilai ID yang sama bisa dimiliki oleh
	 * dokumen dari kedua tabel. Hanya kode inilah yang unik lintas dokumen di
	 * dalam jenisnya sendiri; untuk pembedaan lintas jenis dokumen, ID saja
	 * tidak pernah cukup.
	 * </p>
	 *
	 * @return kode dokumen retur pemakaian, atau {@code null} bila belum
	 *         di-generate.
	 */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return this.kode;
	}

	/**
	 * Menetapkan kode/nomor dokumen retur pemakaian ini. Tidak ada validasi
	 * format maupun pengecekan keunikan di level entitas — keunikan ditegakkan
	 * oleh constraint {@code UNIQUE} database.
	 *
	 * @param kode kode dokumen retur pemakaian.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengambil keterangan bebas dokumen retur pemakaian ini. Karena entitas
	 * ini tidak punya kolom terstruktur untuk alasan pengembalian maupun
	 * tautan ke dokumen pemakaian asalnya, teks bebas inilah satu-satunya
	 * tempat kaitan dengan pemakaian asal dapat dicatat — dan karenanya tidak
	 * dapat diandalkan untuk penelusuran maupun pelaporan.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas dokumen retur pemakaian ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan pengguna pembuat dokumen retur pemakaian ini.
	 *
	 * @param dibuatOleh pengguna pembuat dokumen.
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengambil pengguna pembuat dokumen retur pemakaian ini. Relasi WAJIB
	 * ({@code nullable = false}) ke {@link Tbmuser}. Perlu dibedakan dari
	 * {@link #getPegawai()}: yang di sini adalah pengguna sistem yang MENCATAT
	 * dokumen, sedangkan {@link #getPegawai()} adalah pegawai yang
	 * MENGEMBALIKAN barangnya.
	 *
	 * <p>
	 * Getter ini memanggil {@code check(...)} milik
	 * {@link ais.database.model.GeneralValueObject} untuk meresolusi proxy lazy
	 * dan menugaskan hasilnya kembali ke field — sehingga bukan getter murni:
	 * ia bisa mengubah state object dan membuka koneksi database sendiri saat
	 * sesi Hibernate asalnya sudah tertutup.
	 * </p>
	 *
	 * @return pengguna pembuat dokumen.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = false)
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);
		return dibuatOleh;
	}

	/**
	 * Menetapkan pengguna yang menyetujui dokumen retur pemakaian ini.
	 *
	 * @param disetujuiOleh pengguna penyetuju dokumen.
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengambil pengguna yang menyetujui dokumen retur pemakaian ini — relasi
	 * OPSIONAL, kosong selama retur belum disetujui. Persetujuanlah yang memicu
	 * PENAMBAHAN stok sekaligus pengurangan beban yang sudah terlanjur timbul.
	 *
	 * <p>
	 * Entitas TIDAK memaksakan penyetuju berbeda dari
	 * {@link #getDibuatOleh()}. Karena dokumen ini menambah stok tanpa terikat
	 * pada dokumen pemakaian mana pun (lihat Javadoc kelas), absennya
	 * pemisahan wewenang di sini berkonsekuensi setara dengan pada
	 * {@link KoreksiItemMedis}: seorang pengguna dapat menambah persediaan
	 * seorang diri lewat dokumen yang tampak sepenuhnya wajar. Getter ini
	 * memanggil {@code check(...)} sehingga bukan getter murni (lihat
	 * {@link #getDibuatOleh()}).
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
	 * Menetapkan tanggal pembuatan dokumen retur pemakaian ini.
	 *
	 * @param tanggalPembuatan timestamp pembuatan dokumen.
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Mengambil tanggal pembuatan dokumen retur pemakaian ini. Diisi sekali
	 * oleh lapisan action dan tidak berubah lagi.
	 *
	 * @return timestamp pembuatan dokumen, atau {@code null} bila belum diisi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		return tanggalPembuatan;
	}

	/**
	 * Menetapkan tanggal persetujuan dokumen retur pemakaian ini.
	 *
	 * @param tanggalPersetujuan timestamp persetujuan dokumen.
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengambil tanggal persetujuan dokumen retur pemakaian ini — satu-satunya
	 * penanda status pada entitas ini, sekaligus penanda bahwa stok sudah
	 * ditambahkan kembali. Nilai {@code null} berarti retur masih draft.
	 *
	 * <p>
	 * Karena entitas ini tidak punya kolom pembatalan, membatalkan retur yang
	 * sudah disetujui berarti mengosongkan kembali kolom ini. Setelah itu
	 * dokumen akan tampak seperti draft yang belum pernah disetujui, sementara
	 * penambahan stok yang terlanjur tertulis harus dibalik lapisan action
	 * secara terpisah — dan tidak ada kolom di sini yang menyimpan apakah
	 * pembalikan benar-benar terjadi. Siklus setujui-batalkan-setujui yang
	 * pembalikannya tidak lengkap akan menggandakan PENAMBAHAN stok, yaitu
	 * menciptakan persediaan yang tidak pernah ada, tanpa terlihat pada
	 * dokumennya.
	 * </p>
	 *
	 * @return timestamp persetujuan, atau {@code null} bila belum disetujui.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {
		return tanggalPersetujuan;
	}

	/**
	 * Menetapkan lokasi/gudang tujuan pengembalian ini.
	 *
	 * @param lokasi lokasi gudang tujuan pengembalian.
	 */
	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * Mengambil lokasi/gudang tujuan pengembalian ini — gudang yang stoknya
	 * BERTAMBAH saat dokumen disetujui, sekaligus satu-satunya sumbu pembatas
	 * lingkup data yang tersedia (modul {@code sirs} tidak punya sumbu
	 * tenant/satuan kerja).
	 *
	 * <p>
	 * Skema tidak menjamin gudang ini sama dengan gudang yang dulu
	 * mengeluarkan barangnya lewat {@link PemakaianItem#getLokasi()} — memang
	 * tidak bisa menjamin, karena tidak ada tautan ke dokumen pemakaian asal
	 * sama sekali. Akibatnya barang dapat "dikembalikan" ke gudang yang tidak
	 * pernah mengeluarkannya, memindahkan stok antar gudang lewat sepasang
	 * dokumen pemakaian dan retur tanpa melalui {@link TransferItem} dan tanpa
	 * pengakuan dua pihak yang menjadi pengendalian transfer.
	 * </p>
	 * <p>
	 * Relasi OPSIONAL, sehingga dokumen tanpa lokasi tetap sah secara skema dan
	 * lolos dari setiap filter berbasis lokasi termasuk filter kewenangan.
	 * Getter ini memanggil {@code check(...)} sehingga bukan getter murni
	 * (lihat {@link #getDibuatOleh()}).
	 * </p>
	 *
	 * @return lokasi gudang tujuan pengembalian, atau {@code null} bila belum
	 *         diisi.
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
	 * Menetapkan pegawai yang mengembalikan barang pada dokumen ini.
	 *
	 * @param pegawai pegawai yang mengembalikan barang.
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Mengambil {@link Pegawai} yang mengembalikan barang pada dokumen ini —
	 * cermin dari {@link PemakaianItem#getPegawai()} yang menerima barangnya.
	 * Perlu dibedakan dari {@link #getDibuatOleh()} dan
	 * {@link #getDisetujuiOleh()} yang merupakan pengguna sistem.
	 *
	 * <p>
	 * Skema tidak memeriksa bahwa pegawai di sini sama dengan pegawai yang
	 * dulu menerima barangnya — sekali lagi karena tidak ada tautan ke dokumen
	 * pemakaian asal. Rangkaian "siapa menerima, siapa mengembalikan" karena
	 * itu tidak dapat ditutup oleh data; keduanya hanya dua catatan terpisah
	 * yang kebetulan menyebut orang.
	 * </p>
	 * <p>
	 * Relasi OPSIONAL ({@code nullable = true}). Getter ini memanggil
	 * {@code check(...)} sehingga bukan getter murni (lihat
	 * {@link #getDibuatOleh()}).
	 * </p>
	 *
	 * @return pegawai yang mengembalikan barang, atau {@code null} bila belum
	 *         diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = true)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

}
