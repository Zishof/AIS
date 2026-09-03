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
import ais.database.model.sirs.Gudang;

/**
 * Pengajuan pembelian/pemesanan stok -- baris "kerja" (work item) yang perlu ditindaklanjuti staf
 * gudang, hasil fitur "Purchase: notifikasi stok minimum otomatis 2 tingkat" (gap analisis PDF
 * klien 2026-07-26). Bisa dibuat OTOMATIS ({@link #otomatis}=true, oleh
 * {@link ais.common.StokThresholdScheduler} saat stok sebuah {@link Produk} di sebuah
 * {@link Gudang} menyentuh {@link AmbangStokGudang}), atau MANUAL oleh staf lewat layar admin.
 *
 * <p><b>Kenapa entity BARU, bukan reuse {@code PermintaanPembelian}/{@code PengadaanProduk} yang
 * sudah ada</b> (riset sebelum implementasi, 2026-07-26): {@code PermintaanPembelian}
 * (schema {@code sirs}) dibangun utk {@code ItemMedis}/farmasi, bukan {@code Produk}/gudang
 * kantin-koperasi; {@code PengadaanProduk} adalah catatan barang MASUK langsung (tanpa status
 * draft/approval) dan tidak punya relasi ke {@code Gudang} sama sekali. Memaksakan salah satunya
 * akan mengaburkan makna field-nya utk pemakai modul lain. Baris di sini murni "antrean kerja"
 * ringan -- setelah staf memprosesnya, pencatatan stok MASUK sungguhan tetap lewat mekanisme yang
 * SUDAH ADA ({@code PengadaanProduk} utk gudang pusat→vendor, {@code PengirimanGudangUtil} utk
 * cabang→pusat) -- entity ini TIDAK menduplikasi logika stok apa pun, murni penanda "perlu
 * ditindaklanjuti".</p>
 *
 * <p><b>Arah pengajuan</b> ditentukan oleh {@link #gudangTujuan}: bila terisi (gudang cabang
 * mengajukan ke gudang induknya), berarti permintaan INTERNAL antar gudang (staf akan
 * menindaklanjuti lewat "Pengiriman Antar Gudang"). Bila {@code null} (gudang tanpa
 * {@code gudangInduk}, artinya sudah di gudang pusat/puncak hierarki), berarti permintaan ke
 * VENDOR EKSTERNAL (staf akan menindaklanjuti lewat layar Pengadaan/Kulakan yang sudah ada).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "pengajuan_pembelian_gudang")
public class PengajuanPembelianGudang extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	/** Baru dibuat, belum ditindaklanjuti sama sekali. */
	public static final String STATUS_BARU = "BARU";
	/** Staf sudah mulai memproses (mis. sudah membuat draft Pengadaan/Pengiriman terkait). */
	public static final String STATUS_DIPROSES = "DIPROSES";
	/** Stok sudah terisi ulang / pengajuan tuntas. */
	public static final String STATUS_SELESAI = "SELESAI";
	/** Dibatalkan (mis. ternyata stok sudah cukup / duplikat). */
	public static final String STATUS_DIBATALKAN = "DIBATALKAN";

	/** PK auto-generated (identity, {@code insertable = false} -- lihat catatan pada
	 * {@link #getId()}). */
	private Long id;
	/** Nama petugas yang membuat/mengubah baris pengajuan ini (jejak audit tampilan, bukan FK) --
	 * {@code null} utk pengajuan OTOMATIS ({@link #otomatis}=true, dibuat scheduler/proses sistem
	 * tanpa aktor manusia). Lihat {@link #getOleh()}. */
	private String oleh;
	/** ID/username petugas yang membuat/mengubah baris pengajuan ini. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * ID/username petugas yang membuat/mengubah baris pengajuan ini.
	 *
	 * @return id/username petugas, atau {@code null} bila pengajuan otomatis/belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan {@link #olehId}. Guard null/blank: nilai {@code null}/kosong/spasi DIABAIKAN
	 * (early return) -- field yang sudah terisi tidak ditimpa balik ke kosong, pola yang sama dgn
	 * {@link ais.database.model.koperasi.PayableFakturInfo#setOlehId(String)}.
	 *
	 * @param olehId id/username petugas; diabaikan bila {@code null} atau blank.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan {@link #oleh}. Guard null/blank sama seperti {@link #setOlehId(String)}: nilai
	 * {@code null}/kosong/spasi DIABAIKAN (early return).
	 *
	 * @param oleh nama petugas; diabaikan bila {@code null} atau blank.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama petugas yang membuat/mengubah baris pengajuan ini.
	 *
	 * @return nama petugas, atau {@code null} bila pengajuan otomatis/belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: menandai kapan baris ini TERAKHIR diubah, dengan menuliskan
	 * waktu sekarang ke {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Dipanggil otomatis oleh
	 * Hibernate sebelum {@code UPDATE} -- termasuk saat staf mengganti {@link #status} lewat
	 * dropdown di {@code PengajuanPembelianGudangAction}.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Timestamp perubahan terakhir baris ini, diisi otomatis oleh {@link #onUpdate()}. Lihat
	 * {@link #getTanggal_dirubah()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Setter manual utk {@link #tanggal_dirubah}. Jarang dipakai langsung -- field ini biasanya
	 * diisi otomatis oleh {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah waktu perubahan yang ingin dicatat.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Timestamp perubahan terakhir baris ini, diisi otomatis oleh {@link #onUpdate()}.
	 *
	 * @return waktu perubahan terakhir, atau waktu instansiasi objek bila belum pernah di-update.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat baris ini utk debug/log ({@code "id-produk-status"}). <b>Efek
	 * samping:</b> memanggil {@link #toString()} MEMICU {@link #getProduk()} yang melewati
	 * {@link GeneralValueObject#check(Object)} (dedup/refresh dari cache identitas JVM-wide) --
	 * bukan sekadar pembacaan pasif; memanggil {@code toString()} pada entity yang sesi Hibernate-nya
	 * sudah tertutup dapat memicu {@code LazyInitializationException} yang sama seperti memanggil
	 * {@link #getProduk()} langsung, mengejutkan bagi pemanggil yang mengira {@code toString()}
	 * selalu aman/tanpa efek samping (mis. dipanggil debugger/logger pada thread lain).
	 *
	 * @return string {@code "id-produk-status"}.
	 */
	public String toString() {
		produk = getProduk();
		return id + "-" + produk + "-" + status;
	}

	/** Produk yang diajukan pembeliannya. Lihat {@link #getProduk()}. */
	private Produk produk;
	/** Gudang yang stoknya menipis -- sumber pengajuan. Lihat {@link #getGudangAsal()}. */
	private Gudang gudangAsal;
	/** Gudang tujuan permintaan internal; {@code null} = ke vendor eksternal. Lihat
	 * {@link #getGudangTujuan()}. */
	private Gudang gudangTujuan;
	/** Qty yang diajukan/disarankan utk dibeli/dikirim -- lihat {@link #getQtyDiminta()}. */
	private Double qtyDiminta;
	/** Stok yang terekam SAAT pengajuan dibuat -- arsip/bukti, bukan nilai live. Lihat
	 * {@link #getStokSaatDiajukan()}. */
	private Double stokSaatDiajukan;
	/** Status alur kerja pengajuan ini: {@link #STATUS_BARU}/{@link #STATUS_DIPROSES}/
	 * {@link #STATUS_SELESAI}/{@link #STATUS_DIBATALKAN}. Lihat {@link #getStatus()}. */
	private String status;
	/** {@code true} = diterbitkan otomatis oleh {@link ais.common.StokThresholdScheduler}/pemicu
	 * WO-MTO; {@code false}/{@code null} = dibuat manual staf. Lihat {@link #getOtomatis()}. */
	private Boolean otomatis;
	/** Waktu baris pengajuan ini dibuat. Lihat {@link #getWaktuDibuat()}. */
	private Date waktuDibuat;
	/** Catatan bebas ttg pengajuan ini (mis. rincian perhitungan saran qty, atau alasan pemicu WO/SO).
	 * Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Id {@code ProduksiDokumen} WO pemicu. Lihat Javadoc {@link #getWoId()}. */
	private Long woId;
	/** Id {@code SalesOrderLapangan} pemicu. Lihat Javadoc {@link #getSoId()}. */
	private Long soId;

	/** Konstruktor tanpa argumen wajib JPA/Hibernate. */
	public PengajuanPembelianGudang() {
	}

	/**
	 * PK identity baris pengajuan ini. {@code null} sebelum entity di-{@code save}/{@code flush} ke
	 * Hibernate (ID baru dibuat DB saat insert, strategi {@link IDENTITY}). Kolom bertanda
	 * {@code insertable = false}: Hibernate TIDAK PERNAH menyertakan kolom ini pada perintah
	 * {@code INSERT} apa pun nilai field {@link #id} saat itu (konsisten dgn identity generation --
	 * DB sendiri yang mengisi via sequence/auto-increment).
	 *
	 * @return id baris pengajuan, atau {@code null} bila belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Setter PK -- dipanggil Hibernate saat memuat entity dari DB. Kode aplikasi normal tidak perlu
	 * memanggil ini; id baru dibuat otomatis oleh DB saat insert (lihat catatan
	 * {@code insertable = false} pada Javadoc {@link #getId()}).
	 *
	 * @param id id baris pengajuan.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Produk yang diajukan pembeliannya -- {@code nullable = false}. Getter memakai
	 * {@link GeneralValueObject#check(Object)}: menormalkan referensi ke instance kanonik dari cache
	 * identitas JVM-wide sebelum dikembalikan (lihat {@link #toString()} soal efek samping ini
	 * terpanggil tanpa disangka).
	 *
	 * @return produk yang diajukan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk", nullable = false)
	public Produk getProduk() {
		produk = check(produk);
		return produk;
	}

	/**
	 * Menetapkan produk yang diajukan.
	 *
	 * @param produk produk baru.
	 */
	public void setProduk(Produk produk) {
		this.produk = produk;
	}

	/**
	 * Gudang yang stoknya menipis -- sumber pengajuan, {@code nullable = false}. Getter memakai
	 * {@link GeneralValueObject#check(Object)} (lihat catatan pada Javadoc {@link #getProduk()}).
	 *
	 * @return gudang asal/sumber pengajuan ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gudang_asal", nullable = false)
	public Gudang getGudangAsal() {
		gudangAsal = check(gudangAsal);
		return gudangAsal;
	}

	/**
	 * Menetapkan gudang asal/sumber pengajuan.
	 *
	 * @param gudangAsal gudang asal baru.
	 */
	public void setGudangAsal(Gudang gudangAsal) {
		this.gudangAsal = gudangAsal;
	}

	/**
	 * Gudang tujuan permintaan INTERNAL antar gudang; {@code null} = ke VENDOR EKSTERNAL (gudang
	 * asal sudah di puncak hierarki {@code gudangInduk}) -- lihat Javadoc kelas soal "arah pengajuan".
	 * Getter memakai {@link GeneralValueObject#check(Object)} (lihat catatan pada Javadoc
	 * {@link #getProduk()}).
	 *
	 * @return gudang tujuan, atau {@code null} bila tujuannya vendor eksternal.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gudang_tujuan", nullable = true)
	public Gudang getGudangTujuan() {
		gudangTujuan = check(gudangTujuan);
		return gudangTujuan;
	}

	/**
	 * Menetapkan gudang tujuan. Setter ini tidak memvalidasi konsistensi dgn hierarki
	 * {@link #gudangAsal}{@code .getGudangInduk()} -- kode pembuat pengajuan (mis.
	 * {@link ais.common.StokThresholdScheduler}) yang bertanggung jawab menentukan nilai yang benar
	 * sesuai hierarki gudang.
	 *
	 * @param gudangTujuan gudang tujuan baru, atau {@code null} utk vendor eksternal.
	 */
	public void setGudangTujuan(Gudang gudangTujuan) {
		this.gudangTujuan = gudangTujuan;
	}

	/**
	 * Qty yang diajukan/disarankan utk dibeli/dikirim. TIDAK ada penjaga keseimbangan di level
	 * entity ini yang menautkan nilai ini ke qty yang BENAR-BENAR diterima belakangan (mis. lewat
	 * {@link PengadaanProduk#getQty()} atau pengiriman antar gudang) -- lihat Javadoc kelas: entity
	 * ini murni "antrean kerja"/saran, TIDAK berelasi FK ke dokumen realisasi apa pun, sehingga tidak
	 * ada mekanisme yang mencegah realisasi melebihi/kurang dari qty yang diajukan di sini; staf
	 * bebas menyesuaikan qty riil saat memproses lewat layar Pengadaan/Pengiriman yang sudah ada.
	 * Getter TIDAK null-safe (mengembalikan {@code null} apa adanya, beda dari kebanyakan getter
	 * numerik lain di model AIS yang fallback ke {@code 0.0}).
	 *
	 * @return qty yang diajukan, atau {@code null} bila tidak diisi (mis. rute produksi yang qty-nya
	 *         dicatat di WO, bukan di sini).
	 */
	@Column(name = "qty_diminta", nullable = true)
	public Double getQtyDiminta() {
		return qtyDiminta;
	}

	/**
	 * Menetapkan qty yang diajukan/disarankan.
	 *
	 * @param qtyDiminta qty baru.
	 */
	public void setQtyDiminta(Double qtyDiminta) {
		this.qtyDiminta = qtyDiminta;
	}

	/**
	 * Stok yang terekam SAAT pengajuan dibuat -- arsip/bukti, bukan nilai live. Getter TIDAK
	 * null-safe (mengembalikan {@code null} apa adanya) -- pembaca yang butuh nilai stok TERKINI
	 * harus query ulang lewat {@code StokLokasiUtil}/layar stok, BUKAN membaca field ini.
	 *
	 * @return stok pada saat pengajuan dibuat, atau {@code null} bila tidak dicatat (mis. pengajuan
	 *         manual lama sebelum field ini diisi konsisten).
	 */
	@Column(name = "stok_saat_diajukan", nullable = true)
	public Double getStokSaatDiajukan() {
		return stokSaatDiajukan;
	}

	/**
	 * Menetapkan snapshot stok saat pengajuan dibuat.
	 *
	 * @param stokSaatDiajukan nilai stok baru.
	 */
	public void setStokSaatDiajukan(Double stokSaatDiajukan) {
		this.stokSaatDiajukan = stokSaatDiajukan;
	}

	/**
	 * Status alur kerja pengajuan ini. Getter null-safe: mengembalikan {@link #STATUS_BARU} bila
	 * kolom NULL di DB. Perubahan status TIDAK memicu efek samping apa pun di level entity (mis.
	 * transisi ke {@link #STATUS_SELESAI} tidak otomatis membuat/menautkan dokumen realisasi apa
	 * pun) -- murni penanda alur kerja manual yang diubah staf lewat dropdown di
	 * {@code PengajuanPembelianGudangAction} (siapa pun pemegang hak {@code UPDATE} pada layar itu
	 * dapat mengubah status baris MANA PUN, tanpa gerbang persetujuan/self-approval krn memang tidak
	 * ada konsep "approval" di entity ini, hanya status kerja linear).
	 *
	 * @return status pengajuan, tidak pernah {@code null}.
	 */
	@Column(name = "status", nullable = false, length = 20)
	public String getStatus() {
		return status == null ? STATUS_BARU : status;
	}

	/**
	 * Menetapkan status pengajuan. Tidak ada validasi di level entity bahwa nilainya salah satu dari
	 * {@link #STATUS_BARU}/{@link #STATUS_DIPROSES}/{@link #STATUS_SELESAI}/
	 * {@link #STATUS_DIBATALKAN}, maupun validasi urutan transisi (mis. tidak ada yang mencegah
	 * lompat dari {@code SELESAI} balik ke {@code BARU}) -- kolom {@code varchar} bebas, nilai lain
	 * lolos apa adanya di DB namun hanya string kosong/null yang jatuh fallback ke
	 * {@link #STATUS_BARU} saat dibaca (lihat Javadoc {@link #getStatus()}).
	 *
	 * @param status status baru.
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * {@code true} = pengajuan diterbitkan OTOMATIS (oleh {@link ais.common.StokThresholdScheduler}
	 * saat stok menyentuh ambang, atau pemicu WO-MTO/SO-MTO); {@code false} = dibuat manual staf.
	 * Getter null-safe: mengembalikan {@code false} bila kolom NULL di DB.
	 *
	 * @return {@code true} bila otomatis, tidak pernah {@code null}.
	 */
	public Boolean getOtomatis() {
		return otomatis == null ? false : otomatis;
	}

	/**
	 * Menetapkan penanda otomatis/manual.
	 *
	 * @param otomatis {@code true} bila diterbitkan otomatis oleh sistem.
	 */
	public void setOtomatis(Boolean otomatis) {
		this.otomatis = otomatis;
	}

	/**
	 * Waktu baris pengajuan ini dibuat. Getter TIDAK null-safe (mengembalikan {@code null} apa
	 * adanya) -- BEDA dari pola getter tanggal lain di model AIS yang umumnya fallback ke waktu
	 * sekarang bila kolom NULL.
	 *
	 * @return waktu pembuatan pengajuan, atau {@code null} bila tidak diisi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu_dibuat", nullable = true)
	public Date getWaktuDibuat() {
		return waktuDibuat;
	}

	/**
	 * Menetapkan waktu pembuatan pengajuan.
	 *
	 * @param waktuDibuat waktu baru.
	 */
	public void setWaktuDibuat(Date waktuDibuat) {
		this.waktuDibuat = waktuDibuat;
	}

	/**
	 * Id {@code ProduksiDokumen} WO pemicu (Fase D dok. 48 P4): pengajuan yang lahir dari
	 * kekurangan komponen saat rilis Work Order. {@code null} = pengajuan biasa (ambang stok
	 * atau manual) -- data lama tidak berubah makna.
	 *
	 * @return id WO pemicu, atau {@code null} bila bukan pengajuan bertipe ini.
	 */
	@Column(name = "wo_id", nullable = true)
	public Long getWoId() {
		return woId;
	}

	/**
	 * Menetapkan id WO pemicu.
	 *
	 * @param woId id {@code ProduksiDokumen} WO baru, atau {@code null} utk melepas tautan.
	 */
	public void setWoId(Long woId) {
		this.woId = woId;
	}

	/**
	 * Id {@code SalesOrderLapangan} pemicu (Fase E, MTO_BELI): pengajuan yang lahir saat SO
	 * dikonfirmasi. {@code null} = pengajuan biasa -- data lama tidak berubah makna.
	 *
	 * @return id SO pemicu, atau {@code null} bila bukan pengajuan bertipe ini.
	 */
	@Column(name = "so_id", nullable = true)
	public Long getSoId() {
		return soId;
	}

	/**
	 * Menetapkan id SO pemicu.
	 *
	 * @param soId id {@code SalesOrderLapangan} baru, atau {@code null} utk melepas tautan.
	 */
	public void setSoId(Long soId) {
		this.soId = soId;
	}

	/**
	 * Catatan bebas ttg pengajuan ini -- biasanya diisi otomatis oleh proses penerbit (rincian
	 * perhitungan saran qty/ambang, atau alasan pemicu WO/SO), boleh diedit staf.
	 *
	 * @return keterangan, atau {@code null}/kosong bila tidak diisi.
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menetapkan keterangan. Tidak ada guard null/blank -- memanggil dgn string kosong akan menimpa
	 * nilai lama.
	 *
	 * @param keterangan catatan bebas baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}
}
