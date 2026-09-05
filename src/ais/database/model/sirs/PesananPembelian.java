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
import ais.database.model.library.Penyedia;

/**
 * Entitas <b>Pesanan Pembelian</b> (purchase order / PO) item medis pada schema
 * {@code sirs} (tabel {@code pesanan_pembelian}). Merupakan dokumen HEADER yang
 * menyatakan komitmen pemesanan barang medis kepada satu vendor
 * ({@link Penyedia}) untuk satu lokasi/gudang tujuan ({@link Lokasi}). Baris-baris
 * itemnya disimpan terpisah di {@link PesananPembelianDetail} (relasi satu-arah:
 * detail menunjuk ke header lewat
 * {@link PesananPembelianDetail#getPesananPembelian()}, header TIDAK menyimpan
 * koleksi detail).
 *
 * <h2>Posisi dalam alur pengadaan item medis</h2>
 * <p>
 * Kelas ini adalah mata rantai KEDUA dari rantai pengadaan empat tingkat di
 * paket {@code sirs}, dan rantai tersebut memakai <b>FK NYATA berlapis</b> —
 * bukan pola "antrean kerja tanpa FK" yang dipakai sebagian modul pengadaan lain
 * di AIS:
 * </p>
 * <pre>
 * PermintaanPembelian        --&gt; PesananPembelian        --&gt; PenerimaanOrder        --&gt; PenerimaanOrderKembali
 * PermintaanPembelianDetail  --&gt; PesananPembelianDetail  --&gt; PenerimaanOrderDetail  --&gt; PenerimaanOrderKembaliDetail
 * </pre>
 * <p>
 * Jejak FK tersedia di KEDUA tingkat: header ini menunjuk ke
 * {@link #getPermintaanPembelian()}, dan setiap barisnya menunjuk ke baris
 * permintaan asalnya lewat
 * {@link PesananPembelianDetail#getPermintaanPembelianDetail()}. Artinya
 * penelusuran "PO ini berasal dari permintaan yang mana, baris per baris" bisa
 * dilakukan murni lewat join database, tanpa perlu pencocokan heuristik.
 * </p>
 *
 * <h2>Catatan integritas</h2>
 * <p>
 * Seluruh FK asal bersifat OPSIONAL ({@code nullable = true}), termasuk
 * {@link #getPermintaanPembelian()} dan {@link #getPenyedia()}. Konsekuensinya
 * sebuah PO SAH secara skema meskipun tanpa permintaan asal (PO langsung/tanpa
 * permintaan) dan bahkan tanpa vendor. Aturan bisnis yang mengharuskan PO punya
 * vendor, atau yang mengharuskan baris PO tidak melebihi baris permintaan
 * asalnya, TIDAK dijaga di level entitas ini dan harus ditegakkan di lapisan
 * action/service.
 * </p>
 * <p>
 * Entitas ini menyimpan tiga pasang jejak persetujuan/pembatalan
 * ({@link #getTanggalPersetujuan()}/{@link #getDisetujuiOleh()} dan
 * {@link #getTanggalPembatalan()}/{@link #getDibatalkanOleh()}) namun TIDAK
 * memiliki kolom status eksplisit: status dokumen disimpulkan dari kombinasi
 * ketiga timestamp tersebut. Kode pemanggil yang menilai "PO sudah disetujui"
 * WAJIB juga memeriksa {@link #getTanggalPembatalan()}, karena secara skema
 * sebuah baris bisa memiliki tanggal persetujuan DAN tanggal pembatalan
 * sekaligus.
 * </p>
 * <p>
 * Berbeda dengan {@link PenerimaanOrder}, entitas ini TIDAK memiliki relasi ke
 * {@code PostingHistory} — pemesanan belum menimbulkan jurnal akuntansi; jurnal
 * baru lahir pada saat penerimaan barang.
 * </p>
 * <p>
 * Kelas ini di-{@link Audited} (Hibernate Envers), sehingga setiap perubahan
 * baris terekam pada tabel revisi. Perlu diingat modul {@code sirs} tidak
 * memiliki sumbu tenant/satuan kerja sendiri — pembatasan lingkup data hanya
 * bisa lewat {@link #getLokasi()}.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "pesanan_pembelian")
public class PesananPembelian extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java untuk entitas ini. Nilainya sengaja
	 * dipatok agar object yang sudah terserialisasi (mis. di sesi ZK atau
	 * cache) tetap kompatibel meski struktur kelas berubah.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private Long index;
	private String olehId;

	/**
	 * Mengambil ID pengguna yang terakhir mengubah baris PO ini. Field audit
	 * shadow yang diisi lewat {@link #setOlehId(String)}.
	 *
	 * @return ID pengguna terakhir yang mengubah, atau {@code null} jika belum
	 *         pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna yang mengubah baris PO ini. Nilai kosong/blank
	 * SENGAJA diabaikan (early return) agar field audit ini tidak pernah
	 * ditimpa jadi kosong — pola field audit shadow yang merupakan KEHARUSAN
	 * TEKNIS berulang di paket model AIS, bukan bug.
	 *
	 * @param olehId ID pengguna; nilai {@code null} atau string
	 *               kosong/whitespace-saja tidak akan mengubah nilai tersimpan.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	private String oleh;

	/**
	 * Representasi ringkas dokumen PO ini untuk tampilan combobox/listbox ZK
	 * dan log. Memakai {@link #getKode()} sebagai label, sehingga akan
	 * mengembalikan {@code null} bila kode belum di-generate (baris baru yang
	 * belum disimpan).
	 *
	 * @return kode dokumen PO ini.
	 */
	public String toString() {
		return kode;
	}

	/**
	 * Menetapkan nama pengguna yang mengubah baris PO ini. Nilai kosong/blank
	 * diabaikan agar field audit ini tidak pernah ditimpa kosong.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau kosong/whitespace-saja
	 *             tidak akan mengubah nilai tersimpan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah baris PO ini.
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
	 * Menetapkan timestamp perubahan terakhir baris PO ini secara manual.
	 *
	 * @param tanggal_dirubah timestamp perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil timestamp perubahan terakhir baris PO ini, diperbarui otomatis
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
	private Date tanggalPembatalan;
	private Tbmuser dibuatOleh;
	private Tbmuser disetujuiOleh;
	private Tbmuser dibatalkanOleh;
	private PermintaanPembelian permintaanPembelian;
	private Penyedia vendor;

	private JenisBiayaLain jenisBiayaLain;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public PesananPembelian() {
	}

	/**
	 * Primary key dokumen PO ini, auto-increment (IDENTITY) dan diisi database.
	 *
	 * @return ID unik dokumen PO ini, atau {@code null} untuk baris yang belum
	 *         pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID dokumen PO ini.
	 *
	 * @param id ID dokumen PO.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil kode/nomor dokumen PO ini. Kolom bersifat {@code NOT NULL} dan
	 * {@code UNIQUE} di database, sehingga kode berperan sebagai identitas
	 * bisnis dokumen (di samping {@link #getId()} yang merupakan identitas
	 * teknis). Nilainya di-generate oleh lapisan action saat dokumen dibuat;
	 * karena keunikannya ditegakkan oleh constraint database, dua sesi yang
	 * mencoba menyimpan kode sama akan berakhir dengan kegagalan constraint
	 * pada sesi kedua, bukan dengan data ganda yang diam-diam masuk.
	 *
	 * @return kode dokumen PO, atau {@code null} bila belum di-generate.
	 */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return this.kode;
	}

	/**
	 * Menetapkan kode/nomor dokumen PO ini. Tidak ada validasi format maupun
	 * pengecekan keunikan di level entitas — keunikan ditegakkan oleh
	 * constraint {@code UNIQUE} database.
	 *
	 * @param kode kode dokumen PO.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengambil keterangan bebas dokumen PO ini.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas dokumen PO ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan pengguna pembuat dokumen PO ini.
	 *
	 * @param dibuatOleh pengguna pembuat dokumen.
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengambil pengguna pembuat dokumen PO ini. Relasi WAJIB
	 * ({@code nullable = false}) ke {@link Tbmuser} — setiap PO harus punya
	 * pembuat yang teridentifikasi, yang bersama {@link #getDisetujuiOleh()}
	 * membentuk jejak pemisahan wewenang buat/setujui.
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
	 * Menetapkan pengguna yang menyetujui dokumen PO ini.
	 *
	 * @param disetujuiOleh pengguna penyetuju dokumen.
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengambil pengguna yang menyetujui dokumen PO ini — relasi OPSIONAL,
	 * kosong selama PO belum disetujui. Entitas ini TIDAK memaksakan bahwa
	 * penyetuju harus berbeda dari {@link #getDibuatOleh()}; pemisahan wewenang
	 * (anti self-approval) sepenuhnya menjadi tanggung jawab lapisan action.
	 *
	 * @return pengguna penyetuju, atau {@code null} bila PO belum disetujui.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		return disetujuiOleh;
	}

	/**
	 * Menetapkan tanggal pembuatan dokumen PO ini.
	 *
	 * @param tanggalPembuatan timestamp pembuatan dokumen.
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Mengambil tanggal pembuatan dokumen PO ini. Berbeda dengan
	 * {@link #getTanggal_dirubah()} yang otomatis diperbarui tiap UPDATE,
	 * nilai ini diisi sekali oleh lapisan action dan tidak berubah lagi.
	 *
	 * @return timestamp pembuatan dokumen, atau {@code null} bila belum diisi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		return tanggalPembuatan;
	}

	/**
	 * Menetapkan tanggal persetujuan dokumen PO ini.
	 *
	 * @param tanggalPersetujuan timestamp persetujuan dokumen.
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengambil tanggal persetujuan dokumen PO ini. Karena entitas tidak punya
	 * kolom status, timestamp inilah penanda de facto bahwa PO sudah disetujui
	 * dan boleh diterima barangnya. Nilai {@code null} berarti PO masih draft.
	 *
	 * <p>
	 * PERHATIAN: nilai bukan-{@code null} di sini TIDAK cukup untuk menyatakan
	 * PO aktif — {@link #getTanggalPembatalan()} harus ikut diperiksa, karena
	 * skema mengizinkan kedua timestamp terisi bersamaan pada baris yang sama.
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
	 * Menetapkan tanggal pembatalan dokumen PO ini.
	 *
	 * @param tanggalPembatalan timestamp pembatalan dokumen.
	 */
	public void setTanggalPembatalan(Date tanggalPembatalan) {
		this.tanggalPembatalan = tanggalPembatalan;
	}

	/**
	 * Mengambil tanggal pembatalan dokumen PO ini — penanda de facto bahwa PO
	 * sudah dibatalkan dan tidak boleh lagi dipakai sebagai dasar penerimaan
	 * barang. Pembatalan di sini bersifat penandaan (soft), baris PO tetap ada
	 * beserta detailnya sehingga jejak audit tidak hilang.
	 *
	 * @return timestamp pembatalan, atau {@code null} bila PO belum dibatalkan.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembatalan")
	public Date getTanggalPembatalan() {
		return tanggalPembatalan;
	}

	/**
	 * Menetapkan pengguna yang membatalkan dokumen PO ini.
	 *
	 * @param dibatalkanOleh pengguna pembatal dokumen.
	 */
	public void setDibatalkanOleh(Tbmuser dibatalkanOleh) {
		this.dibatalkanOleh = dibatalkanOleh;
	}

	/**
	 * Mengambil pengguna yang membatalkan dokumen PO ini — relasi OPSIONAL,
	 * pasangan dari {@link #getTanggalPembatalan()}.
	 *
	 * @return pengguna pembatal, atau {@code null} bila PO belum dibatalkan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "dibatalkan_oleh", nullable = true)
	public Tbmuser getDibatalkanOleh() {
		return dibatalkanOleh;
	}

	/**
	 * Menetapkan dokumen permintaan pembelian yang menjadi asal PO ini.
	 *
	 * @param permintaanPembelian dokumen permintaan asal.
	 */
	public void setPermintaanPembelian(PermintaanPembelian permintaanPembelian) {
		this.permintaanPembelian = permintaanPembelian;
	}

	/**
	 * Mengambil dokumen {@link PermintaanPembelian} yang menjadi asal PO ini —
	 * mata rantai FK NYATA pertama dari alur pengadaan item medis
	 * (permintaan &rarr; pesanan &rarr; penerimaan &rarr; retur penerimaan).
	 *
	 * <p>
	 * Relasi ini OPSIONAL ({@code nullable = true}). Ada dua tafsir sah untuk
	 * nilai {@code null}: (a) PO dibuat langsung tanpa didahului permintaan
	 * internal, atau (b) PO memang berasal dari permintaan tetapi tautannya
	 * tidak diisi oleh operator. Skema tidak bisa membedakan keduanya,
	 * sehingga laporan yang mengukur "berapa persen pembelian yang melewati
	 * proses permintaan" akan menghitung kedua kasus itu sama saja.
	 * </p>
	 * <p>
	 * Perlu diperhatikan bahwa tautan tingkat header ini berdiri SENDIRI dan
	 * tidak dijaga konsisten dengan tautan tingkat baris di
	 * {@link PesananPembelianDetail#getPermintaanPembelianDetail()}: tidak ada
	 * constraint yang memaksa setiap {@code permintaanPembelianDetail} pada
	 * baris-baris PO ini berinduk pada permintaan yang sama dengan yang
	 * ditunjuk di sini. Secara skema sangat mungkin sebuah PO menunjuk
	 * permintaan A di header sementara sebagian barisnya menunjuk baris milik
	 * permintaan B. Kode yang menelusuri asal-usul pengadaan sebaiknya bertumpu
	 * pada tautan tingkat BARIS (yang lebih presisi untuk pencocokan kuantitas)
	 * dan memperlakukan tautan header ini sebagai informasi ringkas saja.
	 * </p>
	 * <p>
	 * Perlu dicatat pula bahwa entitas ini tidak menyimpan kuantitas apa pun —
	 * seluruh kuantitas ada di {@link PesananPembelianDetail#getJumlah()}.
	 * Karena itu penjaga "jumlah yang dipesan tidak melebihi jumlah yang
	 * diminta" tidak mungkin ditegakkan dari kelas ini; penjaga semacam itu
	 * harus hidup di lapisan action yang menyimpan baris detailnya.
	 * </p>
	 *
	 * @return dokumen permintaan pembelian asal, atau {@code null} bila PO ini
	 *         tidak ditautkan ke permintaan mana pun.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "permintaan_pembelian", nullable = true)
	public PermintaanPembelian getPermintaanPembelian() {
		return permintaanPembelian;
	}

	/**
	 * Menetapkan vendor/penyedia yang dituju oleh PO ini.
	 *
	 * <p>
	 * Perhatikan ketidakselarasan penamaan yang disengaja: nama method memakai
	 * istilah {@code Penyedia} (mengikuti nama kelas {@link Penyedia}),
	 * sedangkan field dan kolom database memakai istilah {@code vendor}. Nama
	 * property JavaBean yang dikenali Hibernate maupun ZK adalah
	 * {@code penyedia}, bukan {@code vendor}.
	 * </p>
	 *
	 * @param vendor penyedia/vendor tujuan pemesanan.
	 */
	public void setPenyedia(Penyedia vendor) {
		this.vendor = vendor;
	}

	/**
	 * Mengambil vendor/penyedia yang dituju oleh PO ini, dipetakan ke kolom
	 * {@code vendor}. Relasi OPSIONAL ({@code nullable = true}), sehingga
	 * secara skema sebuah PO boleh tersimpan tanpa vendor sama sekali —
	 * kondisi yang secara bisnis tidak masuk akal untuk dokumen pemesanan dan
	 * karenanya harus divalidasi di lapisan action sebelum PO disetujui.
	 *
	 * <p>
	 * Vendor juga bisa ditetapkan per baris pada
	 * {@link PermintaanPembelianDetail#getPenyedia()} di tingkat permintaan.
	 * Tidak ada mekanisme di skema yang menjamin vendor header PO ini sama
	 * dengan vendor yang tercatat pada baris permintaan asalnya.
	 * </p>
	 *
	 * @return penyedia/vendor tujuan pemesanan, atau {@code null} bila belum
	 *         diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "vendor", nullable = true)
	public Penyedia getPenyedia() {
		return vendor;
	}

	/**
	 * Menetapkan lokasi/gudang tujuan PO ini.
	 *
	 * @param lokasi lokasi gudang tujuan.
	 */
	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * Mengambil lokasi/gudang tujuan PO ini — relasi ke {@link Lokasi} pada
	 * paket {@code asset}. Karena modul {@code sirs} tidak memiliki sumbu
	 * tenant/satuan kerja sendiri, kolom inilah satu-satunya sumbu yang bisa
	 * dipakai untuk membatasi lingkup data PO per unit. Relasi OPSIONAL,
	 * sehingga PO tanpa lokasi akan lolos dari filter berbasis lokasi apa pun.
	 *
	 * @return lokasi gudang tujuan, atau {@code null} bila belum diisi.
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

	/**
	 * Mengambil jenis biaya lain yang melekat pada PO ini — relasi OPSIONAL ke
	 * {@link JenisBiayaLain}, dipakai untuk mengklasifikasikan komponen biaya
	 * tambahan (mis. ongkos kirim) agar bisa dijurnalkan ke akun yang tepat
	 * saat penerimaan barang diposting.
	 *
	 * @return jenis biaya lain, atau {@code null} bila tidak ada biaya
	 *         tambahan yang diklasifikasikan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jenis_biaya_lain", nullable = true)
	public JenisBiayaLain getJenisBiayaLain() {
		return jenisBiayaLain;
	}

	/**
	 * Menetapkan jenis biaya lain yang melekat pada PO ini.
	 *
	 * @param jenisBiayaLain jenis biaya lain.
	 */
	public void setJenisBiayaLain(JenisBiayaLain jenisBiayaLain) {
		this.jenisBiayaLain = jenisBiayaLain;
	}

}
