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
 * Entitas <b>Pemakaian Item</b> medis pada schema {@code sirs} (tabel
 * {@code pemakaian_item}). Merepresentasikan pengeluaran item medis dari
 * sebuah {@link Lokasi} untuk DIPAKAI — bukan dijual dan bukan dipindahkan —
 * mis. bahan habis pakai untuk keperluan ruangan atau unit. Baris-baris itemnya
 * ada di {@link PemakaianItemDetail}.
 *
 * <h2>Titik akhir persediaan</h2>
 * <p>
 * Berbeda dari {@link TransferItem} yang memindahkan stok antar gudang dan dari
 * {@link PenerimaanOrderKembali} yang mengembalikannya ke vendor, dokumen ini
 * mengeluarkan barang dari sistem persediaan sepenuhnya: stok berkurang dan
 * tidak bertambah di mana pun. Nilainya menjadi beban.
 * </p>
 * <p>
 * Pasangan kebalikannya adalah {@link PemakaianReturItem}, yang mengembalikan
 * barang yang terlanjur dikeluarkan namun tidak jadi dipakai. Kedua entitas itu
 * hampir identik strukturnya — sama-sama punya {@link #getPegawai()},
 * {@link #getLokasi()}, jejak pembuatan dan persetujuan, dan baris detail
 * dengan bentuk yang sama persis — dan HANYA berbeda pada arah mutasi stoknya
 * serta pada keberadaan {@link #getKeperluan()} yang tidak dimiliki entitas
 * retur.
 * </p>
 * <p>
 * Kemiripan yang nyaris sempurna itu perlu disadari saat memelihara kode di
 * sekitar kedua entitas ini: kode yang menangani pemakaian dan kode yang
 * menangani retur pemakaian berbeda hanya pada nama entitas, nama tabel, nama
 * kolom induk, dan arah stoknya. Kekeliruan menyalin salah satu menjadi yang
 * lain — tertinggalnya satu nama tabel atau satu nama kolom — tidak akan
 * ditolak oleh apa pun, dan akibatnya akan berupa mutasi stok yang menyentuh
 * dokumen milik jenis yang lain atau arah yang terbalik. Karena PK kedua tabel
 * berjalan pada sequence masing-masing, ID dokumen retur dapat kebetulan sama
 * dengan ID dokumen pemakaian, sehingga kesalahan semacam itu tidak gagal
 * dengan jelas melainkan diam-diam mengenai baris milik dokumen lain.
 * </p>
 *
 * <h2>Catatan integritas</h2>
 * <p>
 * Tidak ada penjaga keseimbangan stok di level skema: tidak ada pemeriksaan
 * bahwa stok item di {@link #getLokasi()} mencukupi sebelum pemakaian
 * disetujui. Baris detailnya memang MEREKAM potret stok
 * ({@link PemakaianItemDetail#getStok()} dan
 * {@link PemakaianItemDetail#getStokmenjadi()}), tetapi rekaman itu tidak
 * menahan apa pun — nilai {@code stokmenjadi} yang negatif tersimpan begitu
 * saja.
 * </p>
 * <p>
 * Seperti {@link PermintaanPembelian}, {@link Produksi},
 * {@link KoreksiItemMedis} dan {@link TransferItem}, entitas ini TIDAK memiliki
 * jejak pembatalan ({@code tanggalPembatalan}/{@code dibatalkanOleh}) maupun
 * relasi ke {@code PostingHistory} — padahal pemakaian item adalah dokumen yang
 * secara langsung menimbulkan beban.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "pemakaian_item")
public class PemakaianItem extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java untuk entitas ini, dipatok agar object
	 * yang sudah terserialisasi tetap kompatibel meski struktur kelas berubah.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private Long index;
	private String olehId;

	/**
	 * Mengambil ID pengguna yang terakhir mengubah dokumen pemakaian ini.
	 * Field audit shadow yang diisi lewat {@link #setOlehId(String)}.
	 *
	 * @return ID pengguna terakhir yang mengubah, atau {@code null} jika belum
	 *         pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna yang mengubah dokumen pemakaian ini. Nilai
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
	 * Representasi ringkas dokumen pemakaian ini untuk tampilan
	 * combobox/listbox ZK dan log, memakai {@link #getKode()} sebagai label.
	 * Akan mengembalikan {@code null} bila kode belum di-generate.
	 *
	 * @return kode dokumen pemakaian ini.
	 */
	public String toString() {
		return kode;
	}

	/**
	 * Menetapkan nama pengguna yang mengubah dokumen pemakaian ini. Nilai
	 * kosong/blank diabaikan agar field audit ini tidak pernah ditimpa kosong.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau kosong/whitespace-saja
	 *             tidak akan mengubah nilai tersimpan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah dokumen pemakaian ini.
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
	private String keperluan;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public PemakaianItem() {
	}

	/**
	 * Primary key dokumen pemakaian ini, auto-increment (IDENTITY) dan diisi
	 * database.
	 *
	 * @return ID unik dokumen pemakaian ini, atau {@code null} untuk baris yang
	 *         belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID dokumen pemakaian ini.
	 *
	 * @param id ID dokumen pemakaian.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil kode/nomor dokumen pemakaian ini. Kolom {@code NOT NULL} dan
	 * {@code UNIQUE} di database sehingga berperan sebagai identitas bisnis
	 * dokumen, di samping {@link #getId()} yang merupakan identitas teknis.
	 *
	 * @return kode dokumen pemakaian, atau {@code null} bila belum di-generate.
	 */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return this.kode;
	}

	/**
	 * Menetapkan kode/nomor dokumen pemakaian ini. Tidak ada validasi format
	 * maupun pengecekan keunikan di level entitas — keunikan ditegakkan oleh
	 * constraint {@code UNIQUE} database.
	 *
	 * @param kode kode dokumen pemakaian.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengambil keterangan bebas dokumen pemakaian ini. Berbeda dari
	 * {@link #getKeperluan()} yang khusus menyatakan untuk apa barang dipakai,
	 * kolom ini adalah catatan umum dokumen.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas dokumen pemakaian ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan pengguna pembuat dokumen pemakaian ini.
	 *
	 * @param dibuatOleh pengguna pembuat dokumen.
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengambil pengguna pembuat dokumen pemakaian ini. Relasi WAJIB
	 * ({@code nullable = false}) ke {@link Tbmuser} — setiap pemakaian harus
	 * punya pembuat yang teridentifikasi. Perlu dibedakan dari
	 * {@link #getPegawai()}: yang di sini adalah pengguna sistem yang MENCATAT
	 * dokumen, sedangkan {@link #getPegawai()} adalah pegawai yang MENERIMA
	 * atau memakai barangnya.
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
	 * Menetapkan pengguna yang menyetujui dokumen pemakaian ini.
	 *
	 * @param disetujuiOleh pengguna penyetuju dokumen.
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengambil pengguna yang menyetujui dokumen pemakaian ini — relasi
	 * OPSIONAL, kosong selama pemakaian belum disetujui. Persetujuanlah yang
	 * memicu pengurangan stok sekaligus timbulnya beban. Entitas TIDAK
	 * memaksakan penyetuju berbeda dari {@link #getDibuatOleh()}, sehingga
	 * pemisahan wewenang sepenuhnya tanggung jawab lapisan action. Getter ini
	 * memanggil {@code check(...)} sehingga bukan getter murni (lihat
	 * {@link #getDibuatOleh()}).
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
	 * Menetapkan tanggal pembuatan dokumen pemakaian ini.
	 *
	 * @param tanggalPembuatan timestamp pembuatan dokumen.
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Mengambil tanggal pembuatan dokumen pemakaian ini. Diisi sekali oleh
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
	 * Menetapkan tanggal persetujuan dokumen pemakaian ini.
	 *
	 * @param tanggalPersetujuan timestamp persetujuan dokumen.
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengambil tanggal persetujuan dokumen pemakaian ini — satu-satunya
	 * penanda status pada entitas ini, sekaligus penanda bahwa stok sudah
	 * berkurang dan beban sudah timbul. Nilai {@code null} berarti pemakaian
	 * masih draft.
	 *
	 * <p>
	 * Karena entitas ini tidak punya kolom pembatalan, membatalkan pemakaian
	 * yang sudah disetujui berarti mengosongkan kembali kolom ini. Setelah itu
	 * dokumen akan tampak seperti draft yang belum pernah disetujui, sementara
	 * pengurangan stok yang terlanjur tertulis harus dibalik lapisan action
	 * secara terpisah — dan tidak ada kolom di sini yang menyimpan apakah
	 * pembalikan benar-benar terjadi. Siklus setujui-batalkan-setujui yang
	 * pembalikannya tidak lengkap akan menggandakan pengurangan stok tanpa
	 * terlihat pada dokumennya.
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
	 * Menetapkan lokasi/gudang asal pemakaian ini.
	 *
	 * @param lokasi lokasi gudang asal.
	 */
	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * Mengambil lokasi/gudang asal pemakaian ini — gudang yang stoknya
	 * BERKURANG saat dokumen disetujui, sekaligus satu-satunya sumbu pembatas
	 * lingkup data yang tersedia (modul {@code sirs} tidak punya sumbu
	 * tenant/satuan kerja).
	 *
	 * <p>
	 * Relasi OPSIONAL, sehingga dokumen pemakaian tanpa lokasi tetap sah secara
	 * skema — kondisi berbahaya karena tidak jelas gudang mana yang stoknya
	 * berkurang, dan dokumen tersebut lolos dari setiap filter berbasis lokasi
	 * termasuk filter kewenangan. Getter ini memanggil {@code check(...)}
	 * sehingga bukan getter murni (lihat {@link #getDibuatOleh()}).
	 * </p>
	 *
	 * @return lokasi gudang asal, atau {@code null} bila belum diisi.
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
	 * Menetapkan pegawai yang menerima/memakai barang pada dokumen ini.
	 *
	 * @param pegawai pegawai penerima/pemakai barang.
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Mengambil {@link Pegawai} yang menerima atau memakai barang pada dokumen
	 * ini — pihak yang bertanggung jawab atas barang setelah keluar dari
	 * gudang. Perlu dibedakan dari {@link #getDibuatOleh()} dan
	 * {@link #getDisetujuiOleh()} yang merupakan pengguna sistem: yang di sini
	 * adalah orang yang menerima barangnya, bukan yang mencatat atau
	 * mengesahkan dokumennya.
	 *
	 * <p>
	 * Relasi OPSIONAL ({@code nullable = true}), sehingga dokumen pemakaian
	 * tanpa pegawai penanggung jawab tetap sah secara skema. Untuk dokumen yang
	 * mengeluarkan barang dari sistem persediaan secara permanen, absennya
	 * penanggung jawab berarti tidak ada seorang pun yang dapat dimintai
	 * pertanggungjawaban atas barang yang sudah keluar — pengendalian yang
	 * perlu ditegakkan lapisan action.
	 * </p>
	 * <p>
	 * Getter ini memanggil {@code check(...)} sehingga bukan getter murni
	 * (lihat {@link #getDibuatOleh()}).
	 * </p>
	 *
	 * @return pegawai penerima/pemakai barang, atau {@code null} bila belum
	 *         diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = true)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/**
	 * Menetapkan keperluan pemakaian pada dokumen ini.
	 *
	 * @param keperluan teks keperluan pemakaian.
	 */
	public void setKeperluan(String keperluan) {
		this.keperluan = keperluan;
	}

	/**
	 * Mengambil keperluan pemakaian pada dokumen ini — untuk apa barang yang
	 * dikeluarkan akan dipakai. Kolom inilah satu-satunya pembeda struktural
	 * antara entitas ini dan pasangan kebalikannya
	 * {@link PemakaianReturItem}, yang tidak memilikinya (memang wajar, karena
	 * pengembalian barang tidak punya "keperluan").
	 *
	 * <p>
	 * Berupa teks BEBAS, bukan relasi ke master mana pun, sehingga pemakaian
	 * tidak dapat dikelompokkan menurut keperluannya untuk pelaporan maupun
	 * untuk analisis konsumsi per kegiatan. Skema juga tidak mewajibkannya
	 * terisi.
	 * </p>
	 *
	 * @return teks keperluan pemakaian, atau {@code null} bila belum diisi.
	 */
	public String getKeperluan() {
		return keperluan;
	}

}
