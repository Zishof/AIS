package ais.database.model.koperasi;

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
 * <h3>Fitur "Mutasi Hutang" (Pelanggan) -- pembayaran/pelunasan hutang anggota koperasi.</h3>
 *
 * <p>Pasangan KREDIT dari sisi DEBIT "hutang bertambah" (yg dihitung dari
 * {@code koperasi.pembelian_anggota_koperasi} yang cara pembayarannya ditandai
 * {@link CaraPembayaranKoperasi#getMasukSebagaiHutang()} -- lihat JavaDoc di sana). Baris di sini
 * murni entri manual "member bayar cicilan/lunas hutang", polanya SENGAJA disamakan persis dgn
 * {@link ais.database.model.Deposit} (Topup) -- SATU baris = satu pembayaran, TIDAK ada status
 * "sudah dialokasikan ke transaksi mana" (mengikuti hutang seperti mengikuti tabungan: yg dilacak
 * hanya SALDO BERJALAN per anggota, bukan pelunasan per-transaksi).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "pembayaran_hutang")
public class PembayaranHutang extends GeneralValueObject {

	/** Versi serialisasi tetap; angka acak legacy (bukan {@code 1L}), dipertahankan sebagaimana
	 * adanya krn kontrak {@code Serializable} -- mengubahnya memutus kompatibilitas serialisasi
	 * lama bila entity ini pernah diserialisasi/di-cache secara biner di suatu tempat. */
	private static final long serialVersionUID = 2463821577548439809L;
	/** PK auto-generated (identity). Lihat {@link #getId()}. Catatan: {@code insertable = false}
	 * pada {@link Column} di getter -- lihat Javadoc {@link #getId()}. */
	private Long id;
	/** Nama petugas/kasir yang mencatat baris pembayaran ini (jejak audit tampilan, bukan FK).
	 * Lihat {@link #getOleh()}. */
	private String oleh;
	/** ID/username petugas yang mencatat baris pembayaran ini (jejak audit, pasangan
	 * {@link #oleh}). Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * ID/username petugas yang mencatat baris pembayaran hutang ini. Dipakai bersama {@link #oleh}
	 * sbg jejak audit "siapa yang input" -- keduanya field shadow tampilan/audit, bukan relasi FK
	 * ke tabel user, sehingga tidak ikut ter-cascade/tervalidasi Hibernate.
	 *
	 * @return id/username petugas pencatat, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan {@link #olehId}. Guard null/blank: nilai {@code null} atau string kosong/spasi
	 * DIABAIKAN (early return) -- field yang sudah terisi TIDAK PERNAH ditimpa balik ke kosong
	 * oleh pemanggilan setter ini dengan argumen kosong. Pola guard yg sama dipakai
	 * {@link #setOleh(String)}.
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
	 * {@code null}/kosong/spasi diabaikan, nilai lama dipertahankan.
	 *
	 * @param oleh nama petugas pencatat; diabaikan bila {@code null} atau blank.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama petugas/kasir yang mencatat baris pembayaran hutang ini.
	 *
	 * @return nama petugas pencatat, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: menandai kapan baris pembayaran ini TERAKHIR diubah, dengan
	 * menuliskan waktu sekarang ke {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Dipanggil otomatis
	 * oleh Hibernate sebelum setiap {@code UPDATE} (mis. saat {@code hutangBayarSimpan} mengedit
	 * baris yang sudah ada), tidak pernah dipanggil manual dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Timestamp perubahan terakhir baris ini, diisi otomatis oleh {@link #onUpdate()}. Nilai awal
	 * (sebelum ada update apa pun) adalah waktu instansiasi objek Java, bukan waktu insert DB
	 * sesungguhnya. Lihat {@link #getTanggal_dirubah()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Setter manual utk {@link #tanggal_dirubah}. Jarang dipakai langsung -- field ini biasanya
	 * diisi otomatis oleh {@link #onUpdate()}; memanggil setter ini eksplisit dari kode aplikasi
	 * akan ditimpa lagi oleh callback tsb pada {@code UPDATE} berikutnya.
	 *
	 * @param tanggal_dirubah waktu perubahan yang ingin dicatat.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Timestamp perubahan terakhir baris pembayaran hutang ini.
	 *
	 * @return waktu perubahan terakhir, atau waktu instansiasi objek bila belum pernah di-update.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas utk log/debug: {@code "<id>-<nama anggota>"} (nama kosong bila
	 * {@link #anggotaKoperasi} belum di-set, mis. pada objek transient sebelum di-{@code save}).
	 * Mengakses {@link #anggotaKoperasi} di sini bisa memicu lazy-load bila dipanggil di luar
	 * sesi Hibernate yang masih terbuka dan field tsb belum ter-inisialisasi.
	 *
	 * @return string ringkas {@code "id-namaAnggota"}.
	 */
	public String toString() {
		return id + "-" + (anggotaKoperasi == null ? "" : anggotaKoperasi.getNama());
	}

	/** Anggota koperasi (customer/member) yang membayar cicilan/pelunasan hutang lewat baris ini.
	 * Wajib diisi ({@code nullable = false}). Lihat {@link #getAnggotaKoperasi()}. */
	private AnggotaKoperasi anggotaKoperasi;
	/** Nominal pembayaran pada baris ini, dalam satuan rupiah penuh (bukan {@link
	 * java.math.BigDecimal} seperti kelas alokasi piutang/hutang lain di paket ini -- lihat catatan
	 * tipe di {@link #getNominal()}). Lihat {@link #getNominal()}. */
	private Double nominal;
	/** Waktu pembayaran dicatat/terjadi (bukan waktu perubahan baris -- lihat
	 * {@link #tanggal_dirubah} utk itu). Lihat {@link #getWaktu()}. */
	private Date waktu;
	/** Catatan bebas ttg pembayaran ini (mis. alasan/metode/nomor kuitansi manual). Lihat
	 * {@link #getKeterangan()}. */
	private String keterangan;

	/** Konstruktor tanpa argumen wajib JPA/Hibernate; jangan dipakai langsung dari kode aplikasi --
	 * gunakan setter (via {@code KantinHelper.hutangBayarSimpan}) utk mengisi
	 * {@link #anggotaKoperasi} dan {@link #nominal} sebelum {@code save}. */
	public PembayaranHutang() {
	}

	/**
	 * PK identity baris pembayaran ini. {@code null} sebelum entity di-{@code save}/{@code flush}
	 * ke Hibernate (ID baru dibuat DB saat insert, strategi {@link IDENTITY}). Catatan:
	 * {@code @Column(insertable = false)} di sini berbeda dari empat entity lain di paket ini
	 * (yang tidak menyetel {@code insertable} sama sekali) -- keduanya tidak berpengaruh praktis
	 * krn kolom identity memang selalu dibuat DB, bukan dikirim Hibernate saat insert.
	 *
	 * @return id baris pembayaran, atau {@code null} bila belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Setter PK -- dipanggil Hibernate saat memuat entity dari DB, atau oleh helper
	 * {@code hutangBayarSimpan}/{@code hutangBayarHapus} lewat {@code session.get(..., id)} yang
	 * mengembalikan entity dgn id sudah terisi. Kode aplikasi normal tidak perlu memanggil ini
	 * scr eksplisit.
	 *
	 * @param id id baris pembayaran.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Anggota koperasi yang membayar lewat baris ini. {@code nullable = false} -- setiap
	 * pembayaran hutang WAJIB terikat satu anggota. Relasi EAGER (tanpa {@code fetch = LAZY},
	 * berbeda dari relasi di {@link AlokasiPenerimaanPiutangCustomer}/{@link
	 * AlokasiPembayaranHutangSupplier} yang eksplisit {@code LAZY}) -- mengakses field ini selalu
	 * aman tanpa risiko {@code LazyInitializationException}, dengan trade-off satu join tambahan
	 * setiap kali baris ini dimuat.
	 *
	 * @return anggota koperasi pembayar.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@JoinColumn(name = "anggota_koperasi", nullable = false)
	public AnggotaKoperasi getAnggotaKoperasi() {
		return anggotaKoperasi;
	}

	/**
	 * Menetapkan anggota koperasi pembayar. Setter entity ini tidak menghitung saldo, tetapi jalur
	 * aplikasi {@code KantinHelper.hutangBayarSimpan} mengunci baris anggota dan memvalidasi bahwa
	 * nominal pelunasan tidak melebihi saldo piutang berjalan.
	 *
	 * @param anggotaKoperasi anggota koperasi pembayar.
	 */
	public void setAnggotaKoperasi(AnggotaKoperasi anggotaKoperasi) {
		this.anggotaKoperasi = anggotaKoperasi;
	}

	/**
	 * Nominal pembayaran pada baris ini. Getter null-safe: mengembalikan {@code 0.0} bila kolom
	 * NULL di DB.
	 *
	 * <p><b>Catatan tipe:</b> field ini {@link Double} (floating point), BUKAN
	 * {@link java.math.BigDecimal} seperti {@code nominal} pada {@link
	 * AlokasiPenerimaanPiutangCustomer}/{@link AlokasiPembayaranHutangSupplier} di paket yang
	 * sama. Kelas ini murni entri manual "member bayar cicilan/lunas hutang" (lihat Javadoc
	 * kelas) yang polanya SENGAJA disamakan dgn {@link ais.database.model.Deposit} (Topup) --
	 * saldo hutang dihitung sbg SUM semua baris {@code pembayaran_hutang} DIKURANGI dari total
	 * hutang berjalan (lihat query {@code saldo_hutang} di {@code KantinHelper}), bukan dicocokkan
	 * ke transaksi tertentu. Karena itu TIDAK ADA baris "alokasi" utk kelas ini (berbeda dari
	 * piutang/hutang supplier yg dialokasikan per faktur via {@link AlokasiPenerimaanPiutangCustomer}
	 * /{@link AlokasiPembayaranHutangSupplier}). Jalur simpan standar mengunci baris anggota dan
	 * menolak nominal nol, bukan angka, atau lebih besar daripada saldo berjalan. Data historis yang
	 * pernah masuk melalui jalur lama tetap mungkin mengandung kelebihan bayar dan harus diaudit
	 * terpisah bila ditemukan.
	 *
	 * @return nominal pembayaran, tidak pernah {@code null} (default {@code 0.0}).
	 */
	@Column(name = "nominal")
	public Double getNominal() {
		return nominal == null ? 0.0 : nominal;
	}

	/**
	 * Menetapkan nominal pembayaran baris ini. Tidak melakukan validasi apa pun di level entity
	 * (boleh negatif/nol/{@code null} bila dipanggil langsung) -- validasi angka positif dan batas
	 * saldo dilakukan {@code KantinHelper.hutangBayarSimpan} sebelum entity ini disimpan, bukan di
	 * setter.
	 *
	 * @param nominal nominal pembayaran baru.
	 */
	public void setNominal(Double nominal) {
		this.nominal = nominal;
	}

	/**
	 * Waktu pembayaran ini tercatat/terjadi (diisi user, biasanya sama dgn waktu entry kecuali
	 * dientri mundur/susulan). {@code Column} tanpa {@code nullable = false} scr eksplisit, tetapi
	 * {@code KantinHelper.hutangBayarSimpan} selalu mengisinya (dari parameter {@code waktu} bila
	 * ada, atau {@link ais.ui.util.WaktuUtil#getDate()} saat create baru).
	 *
	 * @return waktu pembayaran, bisa {@code null} bila baris dibuat di luar jalur helper standar.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu")
	public Date getWaktu() {
		return waktu;
	}

	/**
	 * Menetapkan waktu pembayaran. Tidak ada guard null/blank di sini (berbeda dgn
	 * {@link #setOleh}/{@link #setOlehId}) -- {@code null} diterima apa adanya.
	 *
	 * @param waktu waktu pembayaran.
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Catatan bebas ttg pembayaran ini.
	 *
	 * @return keterangan, atau {@code null}/string kosong bila tidak diisi.
	 */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menetapkan keterangan. Tidak ada guard null/blank -- berbeda dari {@link #setOleh}/
	 * {@link #setOlehId}, memanggil dgn string kosong AKAN menimpa nilai lama (mengosongkannya).
	 *
	 * @param keterangan catatan bebas ttg pembayaran ini.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}
}
