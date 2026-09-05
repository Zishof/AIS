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
 * Entitas <b>Permintaan Pembelian</b> (purchase requisition) item medis pada
 * schema {@code sirs} (tabel {@code permintaan_pembelian}). Merupakan dokumen
 * HEADER tahap PERTAMA dari alur pengadaan item medis: unit/gudang
 * ({@link Lokasi}) mengajukan kebutuhan barang, yang setelah disetujui dapat
 * ditindaklanjuti menjadi satu atau lebih {@link PesananPembelian}.
 *
 * <h2>Posisi dalam alur pengadaan item medis</h2>
 * <pre>
 * PermintaanPembelian        --&gt; PesananPembelian        --&gt; PenerimaanOrder        --&gt; PenerimaanOrderKembali
 * PermintaanPembelianDetail  --&gt; PesananPembelianDetail  --&gt; PenerimaanOrderDetail  --&gt; PenerimaanOrderKembaliDetail
 * </pre>
 * <p>
 * Rantai ini memakai <b>FK NYATA berlapis</b> pada KEDUA tingkat (dokumen dan
 * baris), bukan pola "antrean kerja tanpa FK". Dokumen ini adalah hulu rantai
 * tersebut, dan dirujuk dari hilir lewat
 * {@link PesananPembelian#getPermintaanPembelian()} serta — lebih presisi —
 * lewat {@link PesananPembelianDetail#getPermintaanPembelianDetail()}.
 * </p>
 * <p>
 * Arah rujukan seluruhnya dari hilir ke hulu: entitas ini TIDAK menyimpan
 * koleksi {@link PermintaanPembelianDetail} maupun daftar PO yang lahir
 * darinya. Untuk mengetahui apakah sebuah permintaan sudah ditindaklanjuti,
 * kode pemanggil harus melakukan query balik dari sisi PO.
 * </p>
 *
 * <h2>Catatan integritas</h2>
 * <p>
 * Dokumen ini menyimpan jejak pembuatan dan persetujuan
 * ({@link #getTanggalPembuatan()}/{@link #getDibuatOleh()} dan
 * {@link #getTanggalPersetujuan()}/{@link #getDisetujuiOleh()}) namun — berbeda
 * dengan {@link PesananPembelian} dan {@link PenerimaanOrder} — TIDAK memiliki
 * jejak pembatalan sama sekali (tidak ada {@code tanggalPembatalan} maupun
 * {@code dibatalkanOleh}). Permintaan yang dibatalkan karena itu tidak punya
 * tempat penyimpanan yang semestinya di skema ini; satu-satunya cara membatalkan
 * adalah menghapus barisnya, yang berarti kehilangan jejak beserta seluruh
 * baris detailnya. Ketidaksimetrisan ini perlu diketahui saat menulis laporan
 * "permintaan yang belum ditindaklanjuti", karena permintaan mati akan tetap
 * terhitung sebagai terbuka.
 * </p>
 * <p>
 * Tidak ada kolom status; status disimpulkan dari terisi atau tidaknya
 * {@link #getTanggalPersetujuan()}. Modul {@code sirs} juga tidak punya sumbu
 * tenant/satuan kerja, sehingga {@link #getLokasi()} adalah satu-satunya sumbu
 * pembatas lingkup data — dan relasinya OPSIONAL, sehingga baris tanpa lokasi
 * lolos dari filter berbasis lokasi apa pun.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "permintaan_pembelian")
public class PermintaanPembelian extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java untuk entitas ini, dipatok agar object
	 * yang sudah terserialisasi tetap kompatibel meski struktur kelas berubah.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private Long index;
	private String olehId;

	/**
	 * Mengambil ID pengguna yang terakhir mengubah dokumen permintaan ini.
	 * Field audit shadow yang diisi lewat {@link #setOlehId(String)}.
	 *
	 * @return ID pengguna terakhir yang mengubah, atau {@code null} jika belum
	 *         pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna yang mengubah dokumen permintaan ini. Nilai
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
	 * Representasi ringkas dokumen permintaan ini untuk tampilan
	 * combobox/listbox ZK dan log, memakai {@link #getKode()} sebagai label.
	 * Akan mengembalikan {@code null} bila kode belum di-generate.
	 *
	 * @return kode dokumen permintaan ini.
	 */
	public String toString() {
		return kode;
	}

	/**
	 * Menetapkan nama pengguna yang mengubah dokumen permintaan ini. Nilai
	 * kosong/blank diabaikan agar field audit ini tidak pernah ditimpa kosong.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau kosong/whitespace-saja
	 *             tidak akan mengubah nilai tersimpan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah dokumen permintaan ini.
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
	private Date tanggalPersetujuan;
	private Lokasi lokasi;
	private Tbmuser dibuatOleh;
	private Tbmuser disetujuiOleh;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public PermintaanPembelian() {
	}

	/**
	 * Primary key dokumen permintaan ini, auto-increment (IDENTITY) dan diisi
	 * database.
	 *
	 * @return ID unik dokumen permintaan ini, atau {@code null} untuk baris
	 *         yang belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID dokumen permintaan ini.
	 *
	 * @param id ID dokumen permintaan.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil kode/nomor dokumen permintaan ini. Kolom {@code NOT NULL} dan
	 * {@code UNIQUE} di database sehingga berperan sebagai identitas bisnis
	 * dokumen, di samping {@link #getId()} yang merupakan identitas teknis.
	 * Nilainya di-generate lapisan action saat dokumen dibuat; keunikannya
	 * ditegakkan oleh constraint database, bukan oleh entitas ini.
	 *
	 * @return kode dokumen permintaan, atau {@code null} bila belum
	 *         di-generate.
	 */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return this.kode;
	}

	/**
	 * Menetapkan kode/nomor dokumen permintaan ini. Tidak ada validasi format
	 * maupun pengecekan keunikan di level entitas.
	 *
	 * @param kode kode dokumen permintaan.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengambil keterangan bebas dokumen permintaan ini.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas dokumen permintaan ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan pengguna pembuat dokumen permintaan ini.
	 *
	 * @param dibuatOleh pengguna pembuat dokumen.
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengambil pengguna pembuat dokumen permintaan ini. Relasi WAJIB
	 * ({@code nullable = false}) ke {@link Tbmuser} — setiap permintaan harus
	 * punya pengaju yang teridentifikasi.
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
	 * Menetapkan pengguna yang menyetujui dokumen permintaan ini.
	 *
	 * @param disetujuiOleh pengguna penyetuju dokumen.
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengambil pengguna yang menyetujui dokumen permintaan ini — relasi
	 * OPSIONAL, kosong selama permintaan belum disetujui. Entitas TIDAK
	 * memaksakan bahwa penyetuju berbeda dari {@link #getDibuatOleh()};
	 * pemisahan wewenang (anti self-approval) sepenuhnya tanggung jawab
	 * lapisan action.
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
	 * Menetapkan tanggal pembuatan dokumen permintaan ini.
	 *
	 * @param tanggalPembuatan timestamp pembuatan dokumen.
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Mengambil tanggal pembuatan dokumen permintaan ini. Diisi sekali oleh
	 * lapisan action dan tidak berubah lagi, berbeda dengan
	 * {@link #getTanggal_dirubah()} yang otomatis diperbarui tiap UPDATE.
	 *
	 * @return timestamp pembuatan dokumen, atau {@code null} bila belum diisi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		return tanggalPembuatan;
	}

	/**
	 * Menetapkan tanggal persetujuan dokumen permintaan ini.
	 *
	 * @param tanggalPersetujuan timestamp persetujuan dokumen.
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengambil tanggal persetujuan dokumen permintaan ini — satu-satunya
	 * penanda status pada entitas ini. Nilai {@code null} berarti permintaan
	 * masih menunggu persetujuan; nilai terisi berarti permintaan boleh
	 * ditindaklanjuti menjadi {@link PesananPembelian}.
	 *
	 * <p>
	 * Karena entitas ini tidak punya kolom pembatalan, tidak ada nilai yang
	 * bisa membedakan "permintaan hidup yang belum disetujui" dari "permintaan
	 * yang sudah tidak relevan tetapi barisnya masih ada". Keduanya sama-sama
	 * tampil sebagai {@code null} di sini.
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
	 * Menetapkan lokasi/gudang pengaju permintaan ini.
	 *
	 * @param lokasi lokasi gudang pengaju.
	 */
	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * Mengambil lokasi/gudang pengaju permintaan ini — relasi ke {@link Lokasi}
	 * pada paket {@code asset}. Karena modul {@code sirs} tidak punya sumbu
	 * tenant/satuan kerja sendiri, kolom inilah satu-satunya sumbu pembatas
	 * lingkup data permintaan per unit. Relasi OPSIONAL, sehingga permintaan
	 * tanpa lokasi lolos dari filter berbasis lokasi apa pun.
	 *
	 * @return lokasi gudang pengaju, atau {@code null} bila belum diisi.
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
