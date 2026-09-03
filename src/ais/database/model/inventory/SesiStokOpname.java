package ais.database.model.inventory;

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

/**
 * <h2>SesiStokOpname — Sesi/Jadwal Stock Opname (formal) untuk Toko/Kantin.</h2>
 *
 * <p>
 * Entity BARU yang melengkapi pencatatan opname baris-per-baris ({@link StokOpname}) dengan
 * <b>kepala kegiatan opname</b>: jadwal, lokasi/toko, kategori, petugas, status, dan catatan.
 * Diperlukan untuk laporan katalog §2.6 <i>Jadwal Stock Opname</i> dan <i>Berita Acara Stock
 * Opname</i> yang belum bisa dibuat karena {@code stok_opname} hanya menyimpan hasil per barang
 * tanpa konsep sesi. Baris hasil opname tetap pada {@code koperasi.stok_opname}; berita acara
 * dirakit dengan meng-agregat baris tersebut pada rentang tanggal &amp; toko sesi ini (tanpa mengubah
 * tabel {@code stok_opname}).
 * </p>
 *
 * <p>
 * Dengan pendaftaran di {@code hibernate.cfg.xml}, tabel {@code koperasi.sesi_stok_opname} otomatis
 * dibuat (hbm2ddl=update). Penamaan kolom mengikuti aturan proyek (field tanpa @Column ter-fold ke
 * huruf kecil tanpa underscore, mis. {@code tanggalRencana}→{@code tanggalrencana}). Kompatibel
 * Java 1.7 / Hibernate 3.
 * </p>
 *
 * @author AIS e-Kantin (modul stock opname)
 * @see StokOpname
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "sesi_stok_opname")
public class SesiStokOpname extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	/** Baru dijadwalkan, belum dikerjakan. */
	public static final String STATUS_RENCANA = "RENCANA";
	/** Sedang berjalan. */
	public static final String STATUS_BERJALAN = "BERJALAN";
	/**
	 * Sudah selesai (berita acara final).
	 *
	 * <p><b>Temuan audit integritas (klaster stok/opname):</b> transisi ke status ini ({@code
	 * KantinHelper.soSesiSelesai}) TIDAK memeriksa satu pun baris {@link StokOpname} terkait sesi ini
	 * sebelum mengizinkan penutupan -- tidak ada agregasi total selisih, tidak ada ambang toleransi,
	 * tidak ada gerbang approval berjenjang untuk selisih besar. Sesi bisa berpindah dari {@link
	 * #STATUS_BERJALAN} ke status ini kapan pun petugas menekan tombol "selesai", terlepas dari
	 * seberapa besar total kerugian/selisih yang tercatat pada baris-baris opname yang menjadi
	 * cakupannya. Ini adalah pola "soft-check, bukan hard-block" yang sama seperti yang sudah
	 * terdokumentasi pada {@code koperasi.SalesInventoryTripHelper}: selisih dicatat dan memengaruhi
	 * stok produk (via {@code recomputeStokProdukNative}), tetapi tidak pernah memblokir penutupan
	 * sesi/dokumen secara otomatis. Lihat javadoc {@link #getStatus()} untuk pembahasan lengkap alur
	 * status dan implikasinya.</p>
	 */
	public static final String STATUS_SELESAI = "SELESAI";

	/** Primary key sesi. Digenerasi database ({@code IDENTITY}); {@code null} sebelum baris pertama kali disimpan. */
	private Long id;
	/** Toko/outlet tempat sesi opname ini diselenggarakan. Opsional secara skema (tanpa {@code nullable = false}), tapi {@link #getToko()} menormalisasi nilai kosong via {@code check()} dari kelas induk. */
	private Toko toko;
	/** Kode/label sesi yang dibuat otomatis saat sesi dimulai, format {@code "SO-yyyyMMdd-HHmmss"} (lihat {@code KantinHelper.soSesiMulai}) -- dipakai sebagai identitas ramah-manusia pada laporan Berita Acara, terpisah dari {@link #id} numerik internal. */
	private String kode;
	/** Tanggal sesi opname DIRENCANAKAN dilaksanakan (bisa berbeda dari tanggal sesi benar-benar dimulai, {@link #tanggalMulai}). */
	private Date tanggalRencana;
	/** Tanggal/waktu sesi opname benar-benar mulai dikerjakan (status berpindah ke {@link #STATUS_BERJALAN}). {@code null} selama sesi masih berstatus {@link #STATUS_RENCANA}. */
	private Date tanggalMulai;
	/** Tanggal/waktu sesi opname ditutup (status berpindah ke {@link #STATUS_SELESAI}). {@code null} selama sesi belum ditutup. */
	private Date tanggalSelesai;
	/** Kategori/cakupan opname bebas teks (mis. "Semua Rak", "Minuman") -- label deskriptif untuk laporan, tidak dipakai sebagai filter terhadap baris {@link StokOpname} manapun (sesi dan baris opname tidak ber-foreign-key langsung). */
	private String kategori;
	/** Nama/identitas petugas yang mengerjakan opname pada sesi ini, bebas teks (bukan FK ke tabel user). Default userid pemanggil bila tidak diisi eksplisit saat sesi dibuat. */
	private String petugas;
	/** Status alur sesi: {@link #STATUS_RENCANA}, {@link #STATUS_BERJALAN}, atau {@link #STATUS_SELESAI}. Lihat {@link #getStatus()} untuk pembahasan lengkap transisi dan temuan audit terkait. */
	private String status;
	/** Catatan penutup/berita acara ringkas untuk sesi ini, bebas teks, opsional. Diisi paling umum saat sesi ditutup ({@code soSesiSelesai}), tapi setter tidak membatasi kapan field ini boleh diubah. */
	private String keterangan;
	/** Userid/nama pembuat sesi (jejak audit ringan, bebas teks, tidak ber-FK), terpisah dari mekanisme envers ({@code @Audited}). */
	private String oleh;
	/** Id user pembuat sesi, bila tersedia (pelengkap {@link #oleh} untuk pencarian presisi berbasis id, bukan nama yang bisa berubah/duplikat). */
	private String olehId;

	/**
	 * Hook lifecycle Hibernate dipanggil otomatis sebelum setiap {@code UPDATE} terhadap baris sesi ini
	 * (dipicu anotasi {@link javax.persistence.PreUpdate}, BUKAN dipanggil manual). Mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang menstempel ulang {@link
	 * #tanggal_dirubah} dengan waktu server saat ini. Method ini murni hook siklus hidup entity --
	 * tidak melakukan validasi transisi status (mis. mencegah lompat dari {@link #STATUS_RENCANA}
	 * langsung ke {@link #STATUS_SELESAI} tanpa melalui {@link #STATUS_BERJALAN}); validasi semacam itu,
	 * bila ada, berada di lapisan service/action pemanggil, bukan di model ini.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu terakhir baris sesi ini diubah -- field audit shadow yang diisi otomatis oleh {@link
	 * #onUpdate()} pada tiap {@code UPDATE}, terpisah dari riwayat versi penuh envers ({@code @Audited}).
	 * Inisialisasi default memakai waktu konstruksi objek Java, bukan waktu commit transaksi.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Constructor default tanpa argumen, dipakai Hibernate untuk instansiasi entity via refleksi saat memuat baris dari database; kode aplikasi yang membuat sesi baru juga memakainya lalu mengisi field lewat setter (lihat {@code KantinHelper.soSesiMulai}). */
	public SesiStokOpname() {
	}

	/**
	 * Primary key sesi ini. Digenerasi database via strategi {@code IDENTITY} saat baris pertama kali
	 * disimpan; {@code null} pada objek yang belum pernah di-{@code save}.
	 * @return id sesi, atau {@code null} jika belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id id sesi. Normalnya TIDAK diisi manual -- dibiarkan Hibernate mengisinya via {@code IDENTITY} saat insert. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Toko/outlet tempat sesi opname ini diselenggarakan. Relasi {@code LAZY}; getter memanggil {@code
	 * check(toko)} milik {@link GeneralValueObject} yang menormalisasi proxy/nilai kosong sebelum
	 * dikembalikan (perilaku normalisasi generik kelas induk, bukan logika khusus kelas ini).
	 * @return toko sesi (bisa proxy lazy, dinormalisasi via {@code check()}).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko")
	public Toko getToko() {
		toko = check(toko);
		return toko;
	}

	/** @param toko toko tempat sesi opname ini diselenggarakan. */
	public void setToko(Toko toko) {
		this.toko = toko;
	}

	/**
	 * Kode/label sesi berformat {@code "SO-yyyyMMdd-HHmmss"}, dibuat otomatis saat sesi dimulai. Identitas
	 * ramah-manusia untuk laporan Berita Acara, tidak dijamin unik secara skema (tidak ada
	 * {@code unique constraint} eksplisit pada kolom ini di level entity).
	 * @return kode sesi, atau {@code null} bila belum diisi.
	 */
	public String getKode() {
		return kode;
	}

	/** @param kode kode/label sesi. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Tanggal sesi opname DIRENCANAKAN dilaksanakan. {@code null} dibaca sebagai waktu-baca saat ini
	 * ({@link ais.ui.util.WaktuUtil#getDate()}) -- nilai ini dihitung ulang setiap getter dipanggil
	 * selama field mentah masih {@code null}, sama seperti pola default-waktu pada {@link
	 * StokOpname#getWaktuOpname()}.
	 * @return tanggal rencana; default waktu-baca saat ini bila belum pernah diisi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggalRencana() {
		return tanggalRencana == null ? ais.ui.util.WaktuUtil.getDate() : tanggalRencana;
	}

	/** @param tanggalRencana tanggal sesi opname direncanakan. */
	public void setTanggalRencana(Date tanggalRencana) {
		this.tanggalRencana = tanggalRencana;
	}

	/**
	 * Tanggal/waktu sesi opname benar-benar mulai dikerjakan. Berbeda dari {@link #getTanggalRencana()},
	 * getter ini TIDAK memiliki fallback waktu-baca -- {@code null} apa adanya dikembalikan bila sesi
	 * belum pernah dimulai (masih {@link #STATUS_RENCANA}), sehingga {@code null} pada field ini bisa
	 * dipakai sebagai sinyal "sesi belum berjalan" oleh kode pemanggil.
	 * @return waktu mulai sesi, atau {@code null} bila sesi belum pernah berstatus {@link #STATUS_BERJALAN}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggalMulai() {
		return tanggalMulai;
	}

	/** @param tanggalMulai waktu sesi opname mulai dikerjakan. */
	public void setTanggalMulai(Date tanggalMulai) {
		this.tanggalMulai = tanggalMulai;
	}

	/**
	 * Tanggal/waktu sesi opname ditutup. Sama seperti {@link #getTanggalMulai()}, tidak ada fallback
	 * waktu-baca -- {@code null} berarti sesi belum pernah ditutup.
	 * @return waktu selesai sesi, atau {@code null} bila sesi belum berstatus {@link #STATUS_SELESAI}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggalSelesai() {
		return tanggalSelesai;
	}

	/** @param tanggalSelesai waktu sesi opname ditutup. */
	public void setTanggalSelesai(Date tanggalSelesai) {
		this.tanggalSelesai = tanggalSelesai;
	}

	/**
	 * Kategori/cakupan opname bebas teks (mis. "Semua Rak", "Minuman"). Label deskriptif murni untuk
	 * laporan -- TIDAK ada relasi/foreign key ke baris {@link StokOpname} manapun, jadi filter cakupan
	 * pada laporan Berita Acara dilakukan lewat kombinasi tanggal &amp; toko sesi ini, bukan lewat
	 * kategori ini.
	 * @return kategori/cakupan opname, atau {@code null} bila tidak diisi.
	 */
	public String getKategori() {
		return kategori;
	}

	/** @param kategori kategori/cakupan opname. */
	public void setKategori(String kategori) {
		this.kategori = kategori;
	}

	/**
	 * Nama/identitas petugas yang mengerjakan opname pada sesi ini. Bebas teks, tidak ber-FK ke tabel
	 * user -- field pelaporan (ditampilkan di Berita Acara), berbeda dari {@link #getOleh()}/{@link
	 * #getOlehId()} yang merupakan jejak audit siapa yang MEMBUAT baris sesi ini di sistem (bisa berbeda
	 * orang, mis. admin yang menjadwalkan sesi untuk petugas lapangan lain).
	 * @return nama petugas pelaksana, atau {@code null} bila tidak diisi.
	 */
	public String getPetugas() {
		return petugas;
	}

	/** @param petugas nama/identitas petugas pelaksana opname sesi ini. */
	public void setPetugas(String petugas) {
		this.petugas = petugas;
	}

	/**
	 * Status alur sesi opname: {@link #STATUS_RENCANA} (default bila belum diisi) &rarr; {@link
	 * #STATUS_BERJALAN} &rarr; {@link #STATUS_SELESAI}.
	 *
	 * <p><b>Alur transisi di kode pemanggil (bukan di model ini).</b> Model ini TIDAK menegakkan urutan
	 * transisi -- tidak ada validasi state-machine di level entity/setter yang mencegah lompatan status
	 * (mis. {@code STATUS_RENCANA} langsung ke {@code STATUS_SELESAI} tanpa melalui {@code
	 * STATUS_BERJALAN}, atau bahkan mundur dari {@code STATUS_SELESAI} kembali ke status sebelumnya).
	 * Penegakan urutan, sejauh ada, sepenuhnya berada di kode pemanggil: {@code
	 * KantinHelper.soSesiMulai} membuat sesi baru langsung berstatus {@link #STATUS_BERJALAN} (skip
	 * {@link #STATUS_RENCANA}) atau -- secara idempoten -- mengembalikan sesi {@code BERJALAN} yang
	 * sudah ada hari itu tanpa membuat duplikat; {@code KantinHelper.soSesiSelesai} memuat sesi
	 * berdasarkan id lalu langsung men-set status ke {@link #STATUS_SELESAI} TANPA memeriksa status
	 * sebelumnya (bisa dipanggil pada sesi yang statusnya masih {@link #STATUS_RENCANA}, atau bahkan
	 * yang SUDAH {@link #STATUS_SELESAI} -- operasi idempoten murni dari sisi status, tanpa error).</p>
	 *
	 * <p><b>Temuan audit integritas -- penutupan sesi TANPA pemeriksaan selisih (soft-check, bukan
	 * hard-block).</b> Yang secara khusus relevan untuk domain "rawan kecurangan/selisih fisik" modul
	 * ini: transisi ke {@link #STATUS_SELESAI} lewat {@code KantinHelper.soSesiSelesai} TIDAK melakukan
	 * query atau agregasi apa pun terhadap baris {@link StokOpname} yang termasuk cakupan sesi ini (baik
	 * lewat rentang tanggal, toko, maupun kategori) sebelum mengizinkan penutupan. Tidak ada perhitungan
	 * total selisih (jumlah {@code stokFisik - stokSistem} lintas seluruh baris opname sesi ini), tidak
	 * ada perbandingan terhadap ambang toleransi kerugian, dan tidak ada gerbang approval berjenjang
	 * (mis. wajib disetujui atasan bila total selisih negatif melebihi nilai tertentu) sebelum status
	 * berubah menjadi final/{@code SELESAI}. Setelah status menjadi {@link #STATUS_SELESAI}, sesi
	 * dianggap final untuk kebutuhan Berita Acara -- tapi baris {@link StokOpname} yang menjadi
	 * cakupannya TETAP BISA diedit/ditambah/dihapus secara independen (tidak ada foreign key yang
	 * mengunci baris opname ke sesi tertutup), karena keduanya hanya terhubung longgar lewat rentang
	 * tanggal &amp; toko, bukan relasi database formal.</p>
	 *
	 * <p>Pola ini KONSISTEN dengan pola "soft-check, bukan hard-block" yang sudah terdokumentasi pada
	 * {@code koperasi.SalesInventoryTripHelper} (modul lain, batch javadoc sebelumnya): selisih dicatat
	 * dan langsung memengaruhi stok produk secara otomatis (via {@code
	 * StokKantinUtil.recomputeStokProdukNative} yang dipanggil saat baris opname disimpan), tetapi
	 * penutupan dokumen/sesi tidak pernah diblokir oleh besaran selisih tersebut -- mitigasi kecurangan
	 * sepenuhnya bergantung pada review manual laporan Berita Acara setelah fakta, bukan pencegahan
	 * preventif di level data model atau alur closing. Karena ini memperkuat pola arsitektur yang sudah
	 * tercatat sebelumnya (bukan kerentanan baru yang genuinely berbeda), temuan ini didokumentasikan di
	 * sini sebagai referensi audit, tanpa mengajukan task eskalasi terpisah.</p>
	 *
	 * @return status sesi saat ini; {@link #STATUS_RENCANA} bila field mentah {@code null}.
	 */
	public String getStatus() {
		return status == null ? STATUS_RENCANA : status;
	}

	/**
	 * @param status status baru sesi ini. TIDAK divalidasi terhadap status sebelumnya -- lihat javadoc
	 *               {@link #getStatus()} untuk pembahasan lengkap. Nilai yang lazim dipakai adalah salah
	 *               satu dari {@link #STATUS_RENCANA}, {@link #STATUS_BERJALAN}, {@link #STATUS_SELESAI},
	 *               tetapi setter ini menerima string apa pun tanpa pengecekan terhadap konstanta tersebut.
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Catatan penutup/berita acara ringkas untuk sesi ini. Kolom {@code text} tanpa batas panjang keras,
	 * bebas format -- diisi paling umum saat penutupan sesi ({@code soSesiSelesai}), tapi tidak ada
	 * pembatasan pada model yang mencegah pengisian di tahap lain.
	 * @return catatan/keterangan sesi, atau {@code null} bila tidak diisi.
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/** @param keterangan catatan penutup/keterangan sesi. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Userid/nama pembuat baris sesi ini (jejak audit ringan, bebas teks, tidak ber-FK ke tabel user).
	 * @return identitas pembuat sesi, atau {@code null} bila tidak diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/** @param oleh identitas pembuat sesi ini. */
	public void setOleh(String oleh) {
		this.oleh = oleh;
	}

	/**
	 * Id user pembuat sesi, pelengkap {@link #getOleh()} untuk pencarian presisi berbasis id (nama pada
	 * {@link #oleh} bisa berubah atau ambigu/duplikat antar user).
	 * @return id user pembuat sesi, atau {@code null} bila tidak diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/** @param olehId id user pembuat sesi ini. */
	public void setOlehId(String olehId) {
		this.olehId = olehId;
	}

	/**
	 * Stempel waktu terakhir baris sesi ini diubah, diisi otomatis oleh {@link #onUpdate()} pada tiap
	 * {@code UPDATE}. Lihat javadoc field {@link #tanggal_dirubah} untuk detail perbedaannya dari
	 * mekanisme envers.
	 * @return waktu terakhir diubah.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @param tanggal_dirubah waktu terakhir diubah. Normalnya TIDAK diisi manual oleh kode aplikasi --
	 *                        dikelola otomatis oleh {@link #onUpdate()}; setter ini ada untuk kebutuhan
	 *                        Hibernate (property accessor) dan skenario migrasi/backfill data historis.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
