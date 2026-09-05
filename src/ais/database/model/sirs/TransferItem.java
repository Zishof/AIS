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
 * Entitas <b>Transfer Item</b> (mutasi antar gudang) item medis pada schema
 * {@code sirs} (tabel {@code transfer_item}). Merepresentasikan perpindahan
 * item medis dari satu {@link Lokasi} ke lokasi lain di dalam organisasi yang
 * sama — mis. dari gudang farmasi pusat ke depo ruang rawat. Baris-baris
 * itemnya ada di {@link TransferItemDetail}.
 *
 * <h2>Dokumen dua tahap: kirim lalu terima</h2>
 * <p>
 * Berbeda dari dokumen lain di klaster ini yang hanya punya satu titik
 * pengesahan, transfer memiliki DUA tahap yang tercermin pada dua pasang
 * kolom:
 * </p>
 * <ul>
 *   <li><b>Pengiriman</b> — {@link #getTanggalPersetujuan()} dan
 *       {@link #getDisetujuiOleh()}: barang keluar dari
 *       {@link #getLokasi()}.</li>
 *   <li><b>Penerimaan</b> — {@link #getTanggalPenerimaan()} dan
 *       {@link #getDiterimaOleh()}: barang masuk ke
 *       {@link #getLokasiTujuan()}.</li>
 * </ul>
 * <p>
 * Pemisahan dua tahap ini bermakna adanya keadaan ANTARA yang sah: barang
 * sudah keluar dari gudang asal tetapi belum diterima di gudang tujuan
 * (barang dalam perjalanan). Pada keadaan itu, kuantitas yang bersangkutan
 * tidak tercatat di gudang mana pun. Setiap perhitungan persediaan menyeluruh
 * yang menjumlahkan stok seluruh lokasi karena itu akan KEHILANGAN barang yang
 * sedang dalam perjalanan, kecuali bila ia secara khusus ikut menghitung
 * transfer yang sudah dikirim namun belum diterima.
 * </p>
 * <p>
 * Tahap kedua itulah yang menjelaskan keberadaan
 * {@link TransferItemDetail#getJumlahDiterima()} dan
 * {@link TransferItemDetail#getSelisih()}: jumlah yang diterima boleh berbeda
 * dari jumlah yang dikirim, dan selisihnya direkam. Skema tidak menetapkan apa
 * yang terjadi pada selisih tersebut — apakah ia menjadi susut yang dibebankan,
 * atau tetap tercatat sebagai barang dalam perjalanan.
 * </p>
 *
 * <h2>Catatan integritas</h2>
 * <p>
 * Tidak ada penjaga keseimbangan stok di level skema: tidak ada pemeriksaan
 * bahwa stok item di {@link #getLokasi()} mencukupi sebelum transfer dikirim,
 * sehingga gudang asal dapat mengirim barang yang tidak dimilikinya dan
 * berakhir dengan stok negatif. Berbeda dari {@link PemakaianItemDetail} dan
 * {@link KoreksiItemMedisDetail} yang setidaknya MEREKAM potret stok,
 * {@link TransferItemDetail} bahkan tidak memiliki kolom {@code stok} maupun
 * {@code stokmenjadi} sama sekali — sehingga tidak ada jejak keadaan stok pada
 * saat transfer disusun.
 * </p>
 * <p>
 * {@link #getLokasi()} dan {@link #getLokasiTujuan()} sama-sama OPSIONAL, dan
 * skema tidak mencegah keduanya bernilai SAMA. Transfer dengan asal dan tujuan
 * identik akan mengurangi lalu menambah stok pada gudang yang sama — tidak
 * berbahaya bagi saldo akhir, tetapi mengotori riwayat mutasi. Transfer dengan
 * salah satu lokasi kosong jauh lebih berbahaya: ia menjadi mutasi satu arah
 * yang menghilangkan atau menciptakan stok tanpa lawan.
 * </p>
 * <p>
 * Seperti {@link PermintaanPembelian}, {@link Produksi} dan
 * {@link KoreksiItemMedis}, entitas ini TIDAK memiliki jejak pembatalan
 * ({@code tanggalPembatalan}/{@code dibatalkanOleh}) maupun relasi ke
 * {@code PostingHistory}.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "transfer_item")
public class TransferItem extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java untuk entitas ini, dipatok agar object
	 * yang sudah terserialisasi tetap kompatibel meski struktur kelas berubah.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private Long index;
	private String olehId;

	/**
	 * Mengambil ID pengguna yang terakhir mengubah dokumen transfer ini. Field
	 * audit shadow yang diisi lewat {@link #setOlehId(String)}.
	 *
	 * @return ID pengguna terakhir yang mengubah, atau {@code null} jika belum
	 *         pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna yang mengubah dokumen transfer ini. Nilai
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
	 * Representasi ringkas dokumen transfer ini untuk tampilan combobox/listbox
	 * ZK dan log, memakai {@link #getKode()} sebagai label. Akan mengembalikan
	 * {@code null} bila kode belum di-generate.
	 *
	 * @return kode dokumen transfer ini.
	 */
	public String toString() {
		return kode;
	}

	/**
	 * Menetapkan nama pengguna yang mengubah dokumen transfer ini. Nilai
	 * kosong/blank diabaikan agar field audit ini tidak pernah ditimpa kosong.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau kosong/whitespace-saja
	 *             tidak akan mengubah nilai tersimpan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah dokumen transfer ini.
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
	private Lokasi lokasiTujuan;
	private Date tanggalPembuatan;
	private Date tanggalPersetujuan;
	private Date tanggalPenerimaan;
	private Tbmuser dibuatOleh;
	private Tbmuser disetujuiOleh;
	private Tbmuser diterimaOleh;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public TransferItem() {
	}

	/**
	 * Primary key dokumen transfer ini, auto-increment (IDENTITY) dan diisi
	 * database.
	 *
	 * @return ID unik dokumen transfer ini, atau {@code null} untuk baris yang
	 *         belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID dokumen transfer ini.
	 *
	 * @param id ID dokumen transfer.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil kode/nomor dokumen transfer ini. Kolom {@code NOT NULL} dan
	 * {@code UNIQUE} di database sehingga berperan sebagai identitas bisnis
	 * dokumen, di samping {@link #getId()} yang merupakan identitas teknis.
	 *
	 * @return kode dokumen transfer, atau {@code null} bila belum di-generate.
	 */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return this.kode;
	}

	/**
	 * Menetapkan kode/nomor dokumen transfer ini. Tidak ada validasi format
	 * maupun pengecekan keunikan di level entitas — keunikan ditegakkan oleh
	 * constraint {@code UNIQUE} database.
	 *
	 * @param kode kode dokumen transfer.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengambil keterangan bebas dokumen transfer ini.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas dokumen transfer ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan pengguna pembuat dokumen transfer ini.
	 *
	 * @param dibuatOleh pengguna pembuat dokumen.
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengambil pengguna pembuat dokumen transfer ini. Relasi WAJIB
	 * ({@code nullable = false}) ke {@link Tbmuser} — setiap transfer harus
	 * punya pembuat yang teridentifikasi.
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
	 * Menetapkan pengguna yang menyetujui pengiriman transfer ini.
	 *
	 * @param disetujuiOleh pengguna penyetuju pengiriman.
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengambil pengguna yang menyetujui PENGIRIMAN transfer ini — pihak di
	 * gudang asal yang mengesahkan barang keluar. Berpasangan dengan
	 * {@link #getTanggalPersetujuan()}, dan merupakan tahap PERTAMA dari dua
	 * tahap transfer; tahap kedua diwakili {@link #getDiterimaOleh()} di gudang
	 * tujuan.
	 *
	 * <p>
	 * Pemisahan penyetuju pengiriman dan penerima inilah pengendalian bawaan
	 * transfer: barang yang keluar diakui oleh satu pihak dan barang yang masuk
	 * diakui oleh pihak lain, sehingga selisih di antara keduanya terlihat.
	 * Namun entitas TIDAK memaksakan bahwa keduanya orang yang berbeda, dan
	 * juga tidak memaksakan keduanya berbeda dari {@link #getDibuatOleh()} —
	 * bila satu pengguna mengisi ketiga peran, pengendalian tersebut hilang
	 * seluruhnya dan dokumennya tetap tampak sah.
	 * </p>
	 * <p>
	 * Getter ini memanggil {@code check(...)} sehingga bukan getter murni
	 * (lihat {@link #getDibuatOleh()}).
	 * </p>
	 *
	 * @return pengguna penyetuju pengiriman, atau {@code null} bila transfer
	 *         belum dikirim.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujuiOleh = check(disetujuiOleh);
		return disetujuiOleh;
	}

	/**
	 * Menetapkan tanggal pembuatan dokumen transfer ini.
	 *
	 * @param tanggalPembuatan timestamp pembuatan dokumen.
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Mengambil tanggal pembuatan dokumen transfer ini. Diisi sekali oleh
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
	 * Menetapkan tanggal persetujuan pengiriman transfer ini.
	 *
	 * @param tanggalPersetujuan timestamp persetujuan pengiriman.
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengambil tanggal persetujuan PENGIRIMAN transfer ini — penanda bahwa
	 * barang sudah keluar dari {@link #getLokasi()} dan stok gudang asal sudah
	 * berkurang. Tahap PERTAMA dari dua tahap transfer.
	 *
	 * <p>
	 * Kombinasi kolom ini yang terisi dengan
	 * {@link #getTanggalPenerimaan()} yang masih kosong menandai keadaan
	 * "barang dalam perjalanan": kuantitasnya sudah tidak ada di gudang asal
	 * dan belum ada di gudang tujuan. Keadaan itu sah dan bisa berlangsung
	 * lama; yang perlu dipantau adalah transfer yang TERTINGGAL dalam keadaan
	 * ini terlalu lama, karena barangnya secara efektif hilang dari seluruh
	 * pelaporan persediaan per lokasi.
	 * </p>
	 * <p>
	 * Karena entitas ini tidak punya kolom pembatalan, membatalkan pengiriman
	 * berarti mengosongkan kembali kolom ini, yang menghapus jejak bahwa
	 * barang pernah keluar.
	 * </p>
	 *
	 * @return timestamp persetujuan pengiriman, atau {@code null} bila belum
	 *         dikirim.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {
		return tanggalPersetujuan;
	}

	/**
	 * Menetapkan lokasi/gudang ASAL transfer ini.
	 *
	 * @param lokasi lokasi gudang asal.
	 */
	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * Mengambil lokasi/gudang ASAL transfer ini — gudang yang stoknya
	 * BERKURANG saat pengiriman disetujui. Pasangannya adalah
	 * {@link #getLokasiTujuan()}.
	 *
	 * <p>
	 * Relasi OPSIONAL ({@code nullable = true}). Transfer tanpa lokasi asal
	 * menjadi mutasi satu arah yang MENCIPTAKAN stok di gudang tujuan tanpa
	 * mengurangi stok di mana pun — penambahan persediaan tanpa lawan, dengan
	 * bentuk dokumen yang tampak sebagai perpindahan biasa. Skema juga tidak
	 * mencegah kolom ini bernilai sama dengan {@link #getLokasiTujuan()}.
	 * </p>
	 * <p>
	 * Tidak ada pemeriksaan bahwa stok di gudang ini mencukupi sebelum
	 * pengiriman disetujui, sehingga gudang asal dapat mengirim barang yang
	 * tidak dimilikinya dan berakhir dengan stok negatif. Kolom ini juga
	 * satu-satunya sumbu pembatas lingkup data (modul {@code sirs} tidak punya
	 * sumbu tenant/satuan kerja) — perlu diperhatikan bahwa pembatasan
	 * kewenangan pada dokumen ini menyangkut DUA gudang, sehingga pemeriksaan
	 * yang hanya melihat satu di antaranya tidak memadai.
	 * </p>
	 * <p>
	 * Getter ini memanggil {@code check(...)} sehingga bukan getter murni
	 * (lihat {@link #getDibuatOleh()}).
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
	 * Menetapkan lokasi/gudang TUJUAN transfer ini.
	 *
	 * @param lokasiTujuan lokasi gudang tujuan.
	 */
	public void setLokasiTujuan(Lokasi lokasiTujuan) {
		this.lokasiTujuan = lokasiTujuan;
	}

	/**
	 * Mengambil lokasi/gudang TUJUAN transfer ini — gudang yang stoknya
	 * BERTAMBAH saat penerimaan dicatat. Pasangannya adalah
	 * {@link #getLokasi()}.
	 *
	 * <p>
	 * Relasi OPSIONAL ({@code nullable = true}). Transfer tanpa lokasi tujuan
	 * menjadi mutasi satu arah yang MENGHILANGKAN stok dari gudang asal tanpa
	 * menambahkannya di mana pun — pengeluaran persediaan tanpa lawan, dengan
	 * bentuk dokumen yang tampak sebagai perpindahan biasa. Bersama kondisi
	 * sebaliknya pada {@link #getLokasi()}, dua kolom opsional inilah celah
	 * paling langsung untuk menghilangkan atau menciptakan persediaan lewat
	 * dokumen transfer.
	 * </p>
	 * <p>
	 * Getter ini memanggil {@code check(...)} sehingga bukan getter murni
	 * (lihat {@link #getDibuatOleh()}).
	 * </p>
	 *
	 * @return lokasi gudang tujuan, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi_tujuan", nullable = true)
	public Lokasi getLokasiTujuan() {
		lokasiTujuan = check(lokasiTujuan);
		return lokasiTujuan;
	}

	/**
	 * Menetapkan tanggal penerimaan transfer di gudang tujuan.
	 *
	 * @param tanggalPenerimaan timestamp penerimaan.
	 */
	public void setTanggalPenerimaan(Date tanggalPenerimaan) {
		this.tanggalPenerimaan = tanggalPenerimaan;
	}

	/**
	 * Mengambil tanggal PENERIMAAN transfer di gudang tujuan — penanda bahwa
	 * barang sudah masuk ke {@link #getLokasiTujuan()} dan stok gudang tujuan
	 * sudah bertambah. Tahap KEDUA dari dua tahap transfer.
	 *
	 * <p>
	 * Selama kolom ini kosong sementara {@link #getTanggalPersetujuan()} sudah
	 * terisi, barang berada dalam keadaan "dalam perjalanan" dan tidak tercatat
	 * di gudang mana pun. Skema tidak memaksakan bahwa tanggal ini berada
	 * setelah tanggal pengiriman, sehingga transfer yang tercatat diterima
	 * sebelum dikirim tetap tersimpan tanpa keluhan.
	 * </p>
	 * <p>
	 * Perlu dicatat bahwa kuantitas yang bertambah di gudang tujuan adalah
	 * {@link TransferItemDetail#getJumlahDiterima()}, yang boleh berbeda dari
	 * {@link TransferItemDetail#getJumlah()} yang dikirim — selisihnya direkam
	 * pada {@link TransferItemDetail#getSelisih()} tanpa perlakuan yang
	 * ditetapkan skema.
	 * </p>
	 *
	 * @return timestamp penerimaan, atau {@code null} bila belum diterima di
	 *         gudang tujuan.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_penerimaan")
	public Date getTanggalPenerimaan() {
		return tanggalPenerimaan;
	}

	/**
	 * Menetapkan pengguna yang menerima transfer di gudang tujuan.
	 *
	 * @param diterimaOleh pengguna penerima di gudang tujuan.
	 */
	public void setDiterimaOleh(Tbmuser diterimaOleh) {
		this.diterimaOleh = diterimaOleh;
	}

	/**
	 * Mengambil pengguna yang MENERIMA transfer di gudang tujuan — pihak yang
	 * mengesahkan barang masuk, berpasangan dengan
	 * {@link #getTanggalPenerimaan()}. Tahap KEDUA dari dua tahap transfer,
	 * pelengkap dari {@link #getDisetujuiOleh()} di sisi pengirim.
	 *
	 * <p>
	 * Entitas tidak memaksakan bahwa penerima berbeda dari penyetuju
	 * pengiriman; bila keduanya orang yang sama, pengakuan dua pihak yang
	 * menjadi pengendalian bawaan transfer hilang, dan selisih antara jumlah
	 * dikirim dan jumlah diterima tidak lagi merupakan kesaksian dua pihak
	 * melainkan pernyataan sepihak. Getter ini memanggil {@code check(...)}
	 * sehingga bukan getter murni (lihat {@link #getDibuatOleh()}).
	 * </p>
	 *
	 * @return pengguna penerima di gudang tujuan, atau {@code null} bila
	 *         transfer belum diterima.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "diterima_oleh", nullable = true)
	public Tbmuser getDiterimaOleh() {
		diterimaOleh = check(diterimaOleh);
		return diterimaOleh;
	}

}
