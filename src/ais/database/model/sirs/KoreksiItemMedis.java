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
import ais.database.model.Tbmuser;
import ais.database.model.asset.Lokasi;

/**
 * Entitas <b>Koreksi Item Medis</b> (penyesuaian/stock adjustment) pada schema
 * {@code sirs} (tabel {@code koreksi_item_medis}). Merupakan dokumen HEADER
 * untuk menyesuaikan stok item medis di sebuah {@link Lokasi} agar cocok dengan
 * kenyataan fisik — mis. setelah stok opname, atau untuk mencatat barang
 * rusak/hilang/kadaluarsa. Baris-baris itemnya ada di
 * {@link KoreksiItemMedisDetail}.
 *
 * <h2>Dokumen dengan kewenangan terbesar atas persediaan</h2>
 * <p>
 * Berbeda dari seluruh dokumen lain di klaster ini yang mutasi stoknya selalu
 * punya lawan — penerimaan punya pesanan, retur punya penerimaan, produksi
 * punya bahan baku, transfer punya lokasi tujuan — dokumen koreksi TIDAK punya
 * lawan sama sekali. Ia dapat menaikkan atau menurunkan stok tanpa dokumen
 * pembanding apa pun, dengan arah yang ditentukan oleh
 * {@link KoreksiItemMedisDetail#getKodeTransaksi()}.
 * </p>
 * <p>
 * Karena itulah dokumen inilah yang paling perlu dikendalikan secara
 * organisasi, bukan secara skema: satu-satunya pengaman yang tersedia di model
 * adalah jejak {@link #getDibuatOleh()} dan {@link #getDisetujuiOleh()}. Kedua
 * kolom itu tidak dipaksa berbeda oleh entitas, sehingga pemisahan wewenang
 * (pembuat tidak boleh menyetujui koreksinya sendiri) sepenuhnya bergantung
 * pada lapisan action. Tanpa pemisahan itu, satu pengguna dapat menambah atau
 * menghapus persediaan bernilai tinggi seorang diri, dan dokumennya akan
 * tampak sah sepenuhnya.
 * </p>
 *
 * <h2>Catatan integritas</h2>
 * <p>
 * Tidak ada penjaga keseimbangan stok di level skema: tidak ada pemeriksaan
 * bahwa koreksi yang menurunkan stok tidak melampaui stok yang tersedia,
 * sehingga stok negatif dapat dihasilkan. Tidak ada pula kolom terstruktur
 * untuk ALASAN koreksi — {@link #getKeterangan()} yang berupa teks bebas
 * menjadi satu-satunya tempatnya, sehingga koreksi tidak dapat dianalisis
 * menurut sebabnya (susut wajar, kerusakan, kesalahan pencatatan, kehilangan)
 * dan pola penyalahgunaan sulit terlihat dari data.
 * </p>
 * <p>
 * Seperti {@link PermintaanPembelian} dan {@link Produksi}, entitas ini TIDAK
 * memiliki jejak pembatalan ({@code tanggalPembatalan}/{@code dibatalkanOleh}),
 * sehingga membatalkan koreksi berarti mengosongkan kembali
 * {@link #getTanggalPersetujuan()} dan menghapus jejak bahwa dokumen pernah
 * menggerakkan stok. Entitas ini juga tidak punya relasi ke
 * {@code PostingHistory}, padahal koreksi persediaan lazimnya berdampak
 * langsung pada beban di laporan laba rugi.
 * </p>
 * <p>
 * Perlu diperhatikan bahwa entitas ini memetakan ke tabel
 * {@code sirs.koreksi_item_medis} — bukan {@code koreksi_item}, yang merupakan
 * nama tabel milik modul {@code library}. Kode SQL mentah yang menyebut nama
 * tabel tanpa prefix schema atau dengan nama yang keliru akan menyasar tabel
 * modul lain alih-alih gagal dengan jelas.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "koreksi_item_medis")
public class KoreksiItemMedis extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java untuk entitas ini, dipatok agar object
	 * yang sudah terserialisasi tetap kompatibel meski struktur kelas berubah.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private Long index;
	private String olehId;

	/**
	 * Mengambil ID pengguna yang terakhir mengubah dokumen koreksi ini. Field
	 * audit shadow yang diisi lewat {@link #setOlehId(String)}.
	 *
	 * @return ID pengguna terakhir yang mengubah, atau {@code null} jika belum
	 *         pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna yang mengubah dokumen koreksi ini. Nilai
	 * kosong/blank SENGAJA diabaikan (early return) agar field audit ini tidak
	 * pernah ditimpa jadi kosong — pola field audit shadow yang merupakan
	 * KEHARUSAN TEKNIS berulang di paket model AIS, bukan bug. Untuk dokumen
	 * koreksi yang kewenangannya besar atas persediaan, terjaganya field audit
	 * ini bernilai lebih dari sekadar kerapian data.
	 *
	 * @param olehId ID pengguna; nilai {@code null} atau string
	 *               kosong/whitespace-saja tidak akan mengubah nilai tersimpan.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	private String oleh;

	/**
	 * Representasi ringkas dokumen koreksi ini untuk tampilan combobox/listbox
	 * ZK dan log, memakai {@link #getKode()} sebagai label. Akan mengembalikan
	 * {@code null} bila kode belum di-generate.
	 *
	 * @return kode dokumen koreksi ini.
	 */
	public String toString() {
		return kode;
	}

	/**
	 * Menetapkan nama pengguna yang mengubah dokumen koreksi ini. Nilai
	 * kosong/blank diabaikan agar field audit ini tidak pernah ditimpa kosong.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau kosong/whitespace-saja
	 *             tidak akan mengubah nilai tersimpan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah dokumen koreksi ini.
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
	public KoreksiItemMedis() {
	}

	/**
	 * Primary key dokumen koreksi ini, auto-increment (IDENTITY) dan diisi
	 * database.
	 *
	 * @return ID unik dokumen koreksi ini, atau {@code null} untuk baris yang
	 *         belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID dokumen koreksi ini.
	 *
	 * @param id ID dokumen koreksi.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil kode/nomor dokumen koreksi ini. Kolom {@code NOT NULL} dan
	 * {@code UNIQUE} di database sehingga berperan sebagai identitas bisnis
	 * dokumen, di samping {@link #getId()} yang merupakan identitas teknis.
	 *
	 * @return kode dokumen koreksi, atau {@code null} bila belum di-generate.
	 */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return this.kode;
	}

	/**
	 * Menetapkan kode/nomor dokumen koreksi ini. Tidak ada validasi format
	 * maupun pengecekan keunikan di level entitas — keunikan ditegakkan oleh
	 * constraint {@code UNIQUE} database.
	 *
	 * @param kode kode dokumen koreksi.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengambil keterangan bebas dokumen koreksi ini. Karena entitas TIDAK
	 * memiliki kolom terstruktur untuk alasan koreksi, teks bebas inilah
	 * satu-satunya tempat sebab penyesuaian dicatat — akibatnya koreksi
	 * persediaan tidak dapat dikelompokkan menurut sebabnya untuk pelaporan
	 * maupun untuk mendeteksi pola yang mencurigakan.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas dokumen koreksi ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan pengguna pembuat dokumen koreksi ini.
	 *
	 * @param dibuatOleh pengguna pembuat dokumen.
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengambil pengguna pembuat dokumen koreksi ini. Relasi WAJIB
	 * ({@code nullable = false}) ke {@link Tbmuser} — setiap koreksi stok harus
	 * punya pembuat yang teridentifikasi. Bersama {@link #getDisetujuiOleh()},
	 * inilah satu-satunya pengendalian yang tersedia di level model atas
	 * dokumen yang dapat mengubah persediaan tanpa dokumen pembanding.
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
	 * Menetapkan pengguna yang menyetujui dokumen koreksi ini.
	 *
	 * @param disetujuiOleh pengguna penyetuju dokumen.
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengambil pengguna yang menyetujui dokumen koreksi ini — relasi OPSIONAL,
	 * kosong selama koreksi belum disetujui. Persetujuanlah yang memicu
	 * penulisan mutasi stok.
	 *
	 * <p>
	 * Entitas TIDAK memaksakan bahwa penyetuju berbeda dari
	 * {@link #getDibuatOleh()}. Pada dokumen koreksi, absennya pemaksaan itu
	 * lebih berat konsekuensinya dibanding pada dokumen lain di klaster ini:
	 * karena koreksi tidak punya dokumen pembanding, seorang pengguna yang
	 * membuat sekaligus menyetujui koreksinya sendiri dapat menambah atau
	 * menghapus persediaan bernilai tinggi tanpa satu pun pihak kedua yang
	 * terlibat, dan hasilnya tidak akan terlihat berbeda dari koreksi yang
	 * sah. Pemisahan wewenang untuk dokumen ini karena itu harus ditegakkan
	 * lapisan action, bukan diserahkan pada kebiasaan pemakaian.
	 * </p>
	 * <p>
	 * Getter ini memanggil {@code check(...)} sehingga bukan getter murni
	 * (lihat {@link #getDibuatOleh()}).
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
	 * Menetapkan tanggal pembuatan dokumen koreksi ini.
	 *
	 * @param tanggalPembuatan timestamp pembuatan dokumen.
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Mengambil tanggal pembuatan dokumen koreksi ini. Diisi sekali oleh
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
	 * Menetapkan tanggal persetujuan dokumen koreksi ini.
	 *
	 * @param tanggalPersetujuan timestamp persetujuan dokumen.
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengambil tanggal persetujuan dokumen koreksi ini — satu-satunya penanda
	 * status pada entitas ini, sekaligus penanda bahwa mutasi stok sudah
	 * dituliskan. Nilai {@code null} berarti koreksi masih draft.
	 *
	 * <p>
	 * Karena entitas ini tidak punya kolom pembatalan, membatalkan koreksi yang
	 * sudah disetujui berarti mengosongkan kembali kolom ini. Setelah itu
	 * dokumen akan tampak seperti draft yang belum pernah disetujui, sementara
	 * mutasi stok yang terlanjur tertulis harus dibalik oleh lapisan action
	 * secara terpisah. Tidak ada kolom di sini yang menyimpan apakah pembalikan
	 * benar-benar terjadi, sehingga pembalikan yang gagal atau tidak lengkap
	 * akan menetap tanpa terdeteksi di level model.
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
	 * Menetapkan lokasi/gudang yang stoknya dikoreksi.
	 *
	 * @param lokasi lokasi gudang yang dikoreksi.
	 */
	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * Mengambil lokasi/gudang yang stoknya dikoreksi — relasi ke {@link Lokasi}
	 * pada paket {@code asset}. Inilah gudang yang stoknya berubah saat dokumen
	 * disetujui, sekaligus satu-satunya sumbu pembatas lingkup data yang
	 * tersedia (modul {@code sirs} tidak punya sumbu tenant/satuan kerja).
	 *
	 * <p>
	 * Relasi OPSIONAL, sehingga dokumen koreksi tanpa lokasi tetap sah secara
	 * skema. Pada dokumen koreksi kondisi itu bermakna ganda buruknya: tidak
	 * jelas gudang mana yang stoknya diubah, DAN dokumen tersebut lolos dari
	 * setiap filter berbasis lokasi — termasuk filter yang dipakai untuk
	 * membatasi siapa yang boleh melihat serta menyetujui koreksi di gudang
	 * tertentu. Lapisan action perlu mewajibkan kolom ini terisi.
	 * </p>
	 * <p>
	 * Getter ini memanggil {@code check(...)} sehingga bukan getter murni
	 * (lihat {@link #getDibuatOleh()}).
	 * </p>
	 *
	 * @return lokasi gudang yang dikoreksi, atau {@code null} bila belum diisi.
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

}
