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
 * Entitas <b>Saldo Awal Medis</b> pada schema {@code sirs} (tabel
 * {@code saldo_awal_medis}). Merupakan dokumen HEADER yang mencatat stok awal
 * item medis di sebuah {@link Lokasi} — keadaan persediaan pada saat sistem
 * mulai dipakai, atau pada awal periode setelah tutup buku. Baris-baris itemnya
 * ada di {@link SaldoAwalMedisDetail}.
 *
 * <h2>Satu-satunya dokumen yang menciptakan stok dari ketiadaan</h2>
 * <p>
 * Setiap dokumen lain di klaster inventaris {@code sirs} menambah stok dengan
 * dasar: {@link PenerimaanOrder} berdasar pesanan kepada vendor,
 * {@link Produksi} berdasar bahan baku yang dikonsumsi, {@link TransferItem}
 * berdasar pengurangan di gudang lain, {@link PemakaianReturItem} berdasar
 * barang yang pernah dikeluarkan. Dokumen ini tidak berdasar apa pun — memang
 * demikian sifatnya, karena ia justru menyatakan titik nol tempat seluruh
 * perhitungan stok berikutnya bermula.
 * </p>
 * <p>
 * Justru karena itu dokumen ini yang paling perlu dikendalikan secara ketat
 * dari sisi kewenangan, dan paling perlu dibatasi dari sisi kesempatan.
 * Sebuah sistem umumnya hanya membutuhkan saldo awal SEKALI per item per
 * lokasi. Skema tidak menegakkan pembatasan itu: tidak ada constraint yang
 * mencegah dokumen saldo awal kedua, ketiga, dan seterusnya dibuat untuk lokasi
 * dan item yang sama, dan setiap dokumen tambahan akan menambah stok lagi.
 * Pembatasan tersebut sepenuhnya harus ditegakkan lapisan action.
 * </p>
 *
 * <h2>Catatan integritas</h2>
 * <p>
 * Satu-satunya pengaman yang tersedia di level model adalah jejak
 * {@link #getDibuatOleh()} dan {@link #getDisetujuiOleh()}, dan entitas TIDAK
 * memaksa keduanya berbeda — sehingga seorang pengguna dapat menetapkan saldo
 * awal seorang diri. Karena dokumen ini menciptakan persediaan tanpa dokumen
 * pembanding apa pun, konsekuensi absennya pemisahan wewenang di sini setara
 * dengan pada {@link KoreksiItemMedis} dan {@link PemakaianReturItem}.
 * </p>
 * <p>
 * Seperti sebagian besar dokumen inventaris di paket ini, entitas ini TIDAK
 * memiliki jejak pembatalan ({@code tanggalPembatalan}/{@code dibatalkanOleh})
 * maupun relasi ke {@code PostingHistory}. Ia juga tidak memiliki kolom
 * PERIODE: tidak ada penanda saldo awal ini berlaku untuk periode akuntansi
 * yang mana, sehingga satu-satunya petunjuk waktu adalah
 * {@link #getTanggalPembuatan()} dan {@link #getTanggalPersetujuan()}.
 * Akibatnya pemisahan antara "saldo awal saat sistem mulai dipakai" dan "saldo
 * awal periode berikutnya" tidak terekam secara terstruktur, dan tidak ada
 * mekanisme yang mencegah satu periode memiliki lebih dari satu dokumen saldo
 * awal.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "saldo_awal_medis")
public class SaldoAwalMedis extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java untuk entitas ini, dipatok agar object
	 * yang sudah terserialisasi tetap kompatibel meski struktur kelas berubah.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private Long index;
	private String olehId;

	/**
	 * Mengambil ID pengguna yang terakhir mengubah dokumen saldo awal ini.
	 * Field audit shadow yang diisi lewat {@link #setOlehId(String)}.
	 *
	 * @return ID pengguna terakhir yang mengubah, atau {@code null} jika belum
	 *         pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna yang mengubah dokumen saldo awal ini. Nilai
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
	 * Representasi ringkas dokumen saldo awal ini untuk tampilan
	 * combobox/listbox ZK dan log, memakai {@link #getKode()} sebagai label.
	 * Akan mengembalikan {@code null} bila kode belum di-generate.
	 *
	 * @return kode dokumen saldo awal ini.
	 */
	public String toString() {
		return kode;
	}

	/**
	 * Menetapkan nama pengguna yang mengubah dokumen saldo awal ini. Nilai
	 * kosong/blank diabaikan agar field audit ini tidak pernah ditimpa kosong.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau kosong/whitespace-saja
	 *             tidak akan mengubah nilai tersimpan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah dokumen saldo awal ini.
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

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public SaldoAwalMedis() {
	}

	/**
	 * Primary key dokumen saldo awal ini, auto-increment (IDENTITY) dan diisi
	 * database.
	 *
	 * @return ID unik dokumen saldo awal ini, atau {@code null} untuk baris
	 *         yang belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID dokumen saldo awal ini.
	 *
	 * @param id ID dokumen saldo awal.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil kode/nomor dokumen saldo awal ini. Kolom {@code NOT NULL} dan
	 * {@code UNIQUE} di database sehingga berperan sebagai identitas bisnis
	 * dokumen, di samping {@link #getId()} yang merupakan identitas teknis.
	 *
	 * <p>
	 * Perlu ditegaskan bahwa keunikan yang ditegakkan di sini hanyalah keunikan
	 * KODE, bukan keunikan saldo awal. Constraint {@code UNIQUE} pada kolom ini
	 * sama sekali tidak mencegah lahirnya dokumen saldo awal kedua untuk lokasi
	 * dan item yang sama — dokumen kedua itu hanya perlu memakai kode yang
	 * berbeda, dan stok akan bertambah lagi.
	 * </p>
	 *
	 * @return kode dokumen saldo awal, atau {@code null} bila belum
	 *         di-generate.
	 */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return this.kode;
	}

	/**
	 * Menetapkan kode/nomor dokumen saldo awal ini. Tidak ada validasi format
	 * maupun pengecekan keunikan di level entitas — keunikan ditegakkan oleh
	 * constraint {@code UNIQUE} database.
	 *
	 * @param kode kode dokumen saldo awal.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengambil keterangan bebas dokumen saldo awal ini. Karena entitas tidak
	 * punya kolom periode, teks bebas inilah satu-satunya tempat penjelasan
	 * "saldo awal untuk periode apa" dapat dicatat — sehingga tidak dapat
	 * diandalkan untuk penyaringan maupun pelaporan per periode.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas dokumen saldo awal ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan pengguna pembuat dokumen saldo awal ini.
	 *
	 * @param dibuatOleh pengguna pembuat dokumen.
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengambil pengguna pembuat dokumen saldo awal ini. Relasi WAJIB
	 * ({@code nullable = false}) ke {@link Tbmuser} — setiap penetapan saldo
	 * awal harus punya pembuat yang teridentifikasi. Bersama
	 * {@link #getDisetujuiOleh()}, inilah satu-satunya pengendalian yang
	 * tersedia di level model atas dokumen yang menciptakan persediaan tanpa
	 * dasar apa pun.
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
	 * Menetapkan pengguna yang menyetujui dokumen saldo awal ini.
	 *
	 * @param disetujuiOleh pengguna penyetuju dokumen.
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengambil pengguna yang menyetujui dokumen saldo awal ini — relasi
	 * OPSIONAL, kosong selama saldo awal belum disetujui. Persetujuanlah yang
	 * memicu penulisan mutasi stok pembuka.
	 *
	 * <p>
	 * Entitas TIDAK memaksakan penyetuju berbeda dari
	 * {@link #getDibuatOleh()}. Pada dokumen saldo awal, absennya pemaksaan itu
	 * berkonsekuensi sama beratnya dengan pada {@link KoreksiItemMedis}: karena
	 * saldo awal tidak punya dokumen pembanding, seorang pengguna yang membuat
	 * sekaligus menyetujui dokumennya sendiri dapat menciptakan persediaan
	 * bernilai berapa pun tanpa pihak kedua yang terlibat — dan berbeda dari
	 * koreksi, dokumen saldo awal secara wajar memang berisi angka besar untuk
	 * banyak item sekaligus, sehingga jumlah besar di sini justru tidak
	 * menimbulkan kecurigaan.
	 * </p>
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
	 * Menetapkan tanggal pembuatan dokumen saldo awal ini.
	 *
	 * @param tanggalPembuatan timestamp pembuatan dokumen.
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Mengambil tanggal pembuatan dokumen saldo awal ini. Karena entitas tidak
	 * punya kolom periode, timestamp inilah — bersama
	 * {@link #getTanggalPersetujuan()} — satu-satunya petunjuk waktu yang
	 * terstruktur untuk menentukan saldo awal ini menyangkut kapan.
	 *
	 * @return timestamp pembuatan dokumen, atau {@code null} bila belum diisi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		return tanggalPembuatan;
	}

	/**
	 * Menetapkan tanggal persetujuan dokumen saldo awal ini.
	 *
	 * @param tanggalPersetujuan timestamp persetujuan dokumen.
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengambil tanggal persetujuan dokumen saldo awal ini — satu-satunya
	 * penanda status pada entitas ini, sekaligus penanda bahwa stok pembuka
	 * sudah dituliskan. Nilai {@code null} berarti saldo awal masih draft.
	 *
	 * <p>
	 * Karena entitas ini tidak punya kolom pembatalan, membatalkan saldo awal
	 * yang sudah disetujui berarti mengosongkan kembali kolom ini, sehingga
	 * dokumen akan tampak seperti draft yang belum pernah disetujui sementara
	 * mutasi stok pembuka yang terlanjur tertulis harus dibalik lapisan action
	 * secara terpisah. Pada dokumen saldo awal risiko itu berlipat: siklus
	 * setujui-batalkan-setujui yang pembalikannya tidak lengkap akan
	 * menggandakan seluruh stok pembuka sekaligus untuk semua item pada
	 * dokumen, dan karena angka saldo awal memang wajar berjumlah besar,
	 * penggandaannya sulit dikenali sebagai anomali.
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
	 * Menetapkan lokasi/gudang yang ditetapkan saldo awalnya.
	 *
	 * @param lokasi lokasi gudang saldo awal.
	 */
	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * Mengambil lokasi/gudang yang ditetapkan saldo awalnya — gudang yang
	 * stoknya BERTAMBAH saat dokumen disetujui, sekaligus satu-satunya sumbu
	 * pembatas lingkup data yang tersedia (modul {@code sirs} tidak punya sumbu
	 * tenant/satuan kerja).
	 *
	 * <p>
	 * Relasi OPSIONAL, sehingga dokumen saldo awal tanpa lokasi tetap sah
	 * secara skema — tidak jelas gudang mana yang stoknya ditetapkan, dan
	 * dokumen tersebut lolos dari setiap filter berbasis lokasi termasuk filter
	 * kewenangan. Bersama tiadanya pembatasan jumlah dokumen saldo awal per
	 * lokasi, kolom opsional ini melemahkan satu-satunya sumbu yang dapat
	 * dipakai untuk memastikan setiap gudang hanya ditetapkan saldo awalnya
	 * sekali.
	 * </p>
	 *
	 * @return lokasi gudang saldo awal, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "lokasi", nullable = true)
	public Lokasi getLokasi() {
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

}
