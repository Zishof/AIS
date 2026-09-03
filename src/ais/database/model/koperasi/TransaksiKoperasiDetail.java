package ais.database.model.koperasi;

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Satu baris rincian ANGSURAN dari transaksi pinjaman koperasi (header {@link TransaksiKoperasi},
 * modul Unit Simpan Pinjam Koperasi/USPK) -- BUKAN baris rincian produk/inventory (tidak ada
 * relasi ke {@code ProdukKoperasi} atau jurnal langsung di kelas ini; produk pinjaman dan
 * perhitungan bunga/margin ada di header {@link TransaksiKoperasi#getProdukKoperasi()}, baris ini
 * hanya memecah total pinjaman header menjadi jadwal cicilan per bulan). Satu header
 * {@code TransaksiKoperasi} punya banyak baris ini, diurutkan {@link #getKe()} (nomor angsuran
 * ke-1, ke-2, dst).
 *
 * <p>Tiap baris membawa komponen {@link #getPokok()} (pokok pinjaman) dan {@link #getMargin()}
 * (bunga/margin) angsuran tsb secara terpisah -- keduanya dijumlahkan oleh kode pemanggil (mis.
 * {@code TunaiAnggotaKoperasiCommon}) menjadi nominal tagihan angsuran. Status LUNAS/BELUM baris
 * ini ditentukan oleh {@link #getPembayaranAnggotaKoperasiDetail()}: {@code null} berarti belum
 * dibayar, terisi berarti sudah dilunasi lewat baris {@link PembayaranAnggotaKoperasiDetail}
 * tsb -- field ini SEKALI diisi lalu tidak pernah dikosongkan lagi kembali oleh alur pembayaran
 * normal (satu arah, penanda "sudah dibayar" permanen), dan pengecekan {@code == null} pada versi
 * terkelola (bukan versi yang dibawa caller) inilah yang menjadi penjaga terhadap pembayaran ganda
 * pada {@code TunaiAnggotaKoperasiCommon.onSave} -- lihat Javadoc kelas tsb.
 *
 * <p>Field {@link #getVa()}/{@link #getExpired()}/{@link #getLink()} adalah dukungan pembayaran
 * ONLINE per-angsuran (virtual account bank / link pembayaran pihak ketiga) -- dipakai layar
 * unduh tagihan online ({@code DownloadTagihanAnggotaKoperasiBankOnline}) dan alur
 * {@code PembayaranKoperasiOnline}; {@link #getExpired()} menandai kedaluwarsa VA/link tsb
 * (diperiksa terpisah oleh kode pemanggil, BUKAN oleh getter di kelas ini -- getter
 * {@code getVa()}/{@code getLink()} tidak tahu apakah nilainya sudah kedaluwarsa).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "transaksi_koperasi_detail")
public class TransaksiKoperasiDetail extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap; dipertahankan hanya krn kontrak {@link GeneralValueObject}/
	 * {@code Serializable}.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** PK auto-generated (identity). Lihat {@link #getId()}. */
	private Long id;
	/** Nama petugas yang membuat/mengubah baris angsuran ini (jejak audit tampilan, bukan FK).
	 * Lihat {@link #getOleh()}. */
	private String oleh;
	/** ID/username petugas yang membuat/mengubah baris angsuran ini. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * ID/username petugas yang membuat/mengubah baris angsuran ini.
	 *
	 * @return id/username petugas, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan {@link #olehId}. Guard null/blank: nilai {@code null}/kosong/spasi DIABAIKAN
	 * (early return) -- field yang sudah terisi tidak ditimpa balik ke kosong.
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
	 * Menetapkan {@link #oleh}. Guard null/blank sama seperti {@link #setOlehId(String)}.
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
	 * Nama petugas yang membuat/mengubah baris angsuran ini.
	 *
	 * @return nama petugas, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: menandai kapan baris ini TERAKHIR diubah, dengan menuliskan
	 * waktu sekarang ke {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Dipanggil otomatis
	 * oleh Hibernate sebelum {@code UPDATE} -- termasuk saat baris ini ditandai lunas lewat
	 * {@link #setPembayaranAnggotaKoperasiDetail(PembayaranAnggotaKoperasiDetail)}.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Waktu baris ini terakhir diubah, diisi otomatis lewat {@link #onUpdate()}. Lihat
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
	 * Representasi teks singkat baris angsuran ini, dipakai a.l. oleh komponen UI ZK (mis. combo
	 * box) yang menampilkan objek lewat {@code toString()}. Format: {@code "<id>-<keterangan>"}.
	 * Tidak null-safe terhadap {@link #keterangan}: bila {@code keterangan} {@code null}, hasilnya
	 * memuat literal {@code "null"} pada bagian setelah tanda hubung (perilaku standar konkatenasi
	 * String Java thd referensi null), bukan galat.
	 *
	 * @return string {@code "<id>-<keterangan>"}.
	 */
	public String toString() {
		return id + "-" + keterangan;
	}

	/** Nomor urut angsuran ini dalam jadwal cicilan header {@link #transaksiKoperasi} (ke-1, ke-2,
	 * dst). Lihat {@link #getKe()}. */
	private Integer ke;
	/** Tanggal jatuh tempo/jadwal angsuran ini. Lihat {@link #getTanggal()}. */
	private Date tanggal;
	/** Catatan bebas ttg baris angsuran ini; juga dipakai {@link #toString()}. Lihat
	 * {@link #getKeterangan()}. */
	private String keterangan;
	/** Header transaksi pinjaman koperasi pemilik jadwal angsuran ini. Lihat
	 * {@link #getTransaksiKoperasi()}. */
	private TransaksiKoperasi transaksiKoperasi;
	/** Baris pembayaran yang melunasi angsuran ini; {@code null} = belum dibayar. Lihat
	 * {@link #getPembayaranAnggotaKoperasiDetail()}. */
	private PembayaranAnggotaKoperasiDetail pembayaranAnggotaKoperasiDetail;
	/** Komponen pokok pinjaman pada angsuran ini (terpisah dari {@link #margin}). Lihat
	 * {@link #getPokok()}. */
	private Double pokok;
	/** Komponen bunga/margin pada angsuran ini (terpisah dari {@link #pokok}). Lihat
	 * {@link #getMargin()}. */
	private Double margin;
	/** Sisa pokok pinjaman SETELAH angsuran ini (saldo berjalan/running balance), dipakai laporan
	 * simpan-pinjam. Lihat {@link #getSisa()}. */
	private Double sisa;
	/** Status aktif baris angsuran ini; dipakai memfilter baris yang berlaku dari jadwal yang
	 * sudah diganti/dibatalkan. Lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Nomor virtual account bank utk pembayaran online angsuran ini. Lihat {@link #getVa()}. */
	private String va;
	/** Waktu kedaluwarsa {@link #va}/{@link #link}. Lihat {@link #getExpired()}. */
	private Date expired;
	/** URL link pembayaran online pihak ketiga utk angsuran ini. Lihat {@link #getLink()}. */
	private String link;

	/** Konstruktor tanpa argumen wajib JPA/Hibernate. */
	public TransaksiKoperasiDetail() {
	}

	/**
	 * PK identity baris angsuran ini. {@code null} sebelum entity di-{@code save}/{@code flush} ke
	 * Hibernate. {@code insertable = false} pada {@code @Column} -- kolom {@code id} TIDAK
	 * disertakan pada statement {@code INSERT} yang dibuat Hibernate utk kelas ini (nilainya
	 * murni ditentukan DB lewat {@link IDENTITY}, konsisten dgn {@code insertable=false}, hanya
	 * saja anotasi ini biasanya berpasangan dgn kolom yang DIISI trigger/default DB, bukan sekadar
	 * identity biasa -- pola yang sama juga muncul di beberapa entity hbm2java lain, dicatat di
	 * sini sbg observasi struktur, bukan bug).
	 *
	 * @return id baris angsuran, atau {@code null} bila belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Setter PK -- dipanggil Hibernate saat memuat entity dari DB. Kode aplikasi normal tidak
	 * perlu memanggil ini; id baru dibuat otomatis oleh DB saat insert.
	 *
	 * @param id id baris angsuran.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Catatan bebas ttg baris angsuran ini; juga dipakai {@link #toString()}.
	 *
	 * @return keterangan, atau {@code null} bila belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan. Tidak ada guard null/blank -- memanggil dgn string kosong/null akan
	 * menimpa nilai lama (dan mempengaruhi output {@link #toString()}).
	 *
	 * @param keterangan catatan bebas baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Status aktif baris angsuran ini. Getter null-safe: mengembalikan {@code true} (primitif,
	 * auto-unboxing dari literal {@code true}) bila field {@code null} di DB -- DEFAULT AKTIF,
	 * bukan default nonaktif; sama pola dgn flag {@code aktif} pada entity lain di paket ini
	 * (mis. {@link CustomerInventoryProfile#getAktif()}). Tidak ada anotasi {@code @Column}
	 * eksplisit -- kolomnya dipetakan otomatis dari nama field {@code aktif} oleh Hibernate.
	 *
	 * @return {@code true} bila baris angsuran ini aktif/berlaku, {@code false} bila tidak.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan status aktif baris angsuran ini.
	 *
	 * @param aktif status aktif baru; {@code null} diperlakukan sbg aktif oleh getter.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Header transaksi pinjaman koperasi pemilik jadwal angsuran ini. {@code nullable = false} --
	 * setiap baris angsuran wajib terikat satu {@link TransaksiKoperasi}. Getter memanggil
	 * {@link GeneralValueObject#check(Object)} sebelum mengembalikan nilai -- meresolusi proxy
	 * lazy Hibernate yang mungkin sudah "basi" (sesi asalnya tertutup) lewat cache identity map
	 * internal, supaya pemanggil di luar sesi Hibernate tidak selalu menabrak
	 * {@code LazyInitializationException}; lihat Javadoc {@link GeneralValueObject#check(Object)}
	 * utk detail mekanisme &amp; batasannya (kegagalan resolusi bersifat senyap, proxy dikembalikan
	 * apa adanya bila tidak bisa diresolusi).
	 *
	 * @return header transaksi pinjaman koperasi pemilik baris angsuran ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "transaksi_koperasi", nullable = false)
	public TransaksiKoperasi getTransaksiKoperasi() {
		transaksiKoperasi = check(transaksiKoperasi);
		return transaksiKoperasi;
	}

	/**
	 * Menetapkan header transaksi pinjaman koperasi pemilik baris angsuran ini.
	 *
	 * @param transaksiKoperasi header transaksi pinjaman koperasi.
	 */
	public void setTransaksiKoperasi(TransaksiKoperasi transaksiKoperasi) {
		this.transaksiKoperasi = transaksiKoperasi;
	}

	/**
	 * Tanggal jatuh tempo/jadwal angsuran ini (kolom tipe {@code DATE}, tanpa komponen jam).
	 * {@code nullable = false}.
	 *
	 * @return tanggal jadwal angsuran ini.
	 */
	@Temporal(TemporalType.DATE)
	@Column(nullable = false)
	public Date getTanggal() {
		return tanggal;
	}

	/**
	 * Menetapkan tanggal jadwal angsuran ini.
	 *
	 * @param tanggal tanggal jadwal baru.
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Komponen pokok pinjaman pada angsuran ini (terpisah dari {@link #getMargin()}). Getter
	 * null-safe: mengembalikan {@code 0.0} bila kolom NULL di DB.
	 *
	 * @return komponen pokok angsuran ini, tidak pernah {@code null}.
	 */
	public Double getPokok() {
		return pokok == null ? 0.0 : pokok;
	}

	/**
	 * Menetapkan komponen pokok angsuran ini.
	 *
	 * @param pokok komponen pokok baru.
	 */
	public void setPokok(Double pokok) {
		this.pokok = pokok;
	}

	/**
	 * Komponen bunga/margin pada angsuran ini (terpisah dari {@link #getPokok()}); bersama
	 * {@code pokok} membentuk nominal tagihan angsuran ini (lihat Javadoc kelas). Getter
	 * null-safe: mengembalikan {@code 0.0} bila kolom NULL di DB.
	 *
	 * @return komponen margin/bunga angsuran ini, tidak pernah {@code null}.
	 */
	public Double getMargin() {
		return margin == null ? 0.0 : margin;
	}

	/**
	 * Menetapkan komponen margin/bunga angsuran ini.
	 *
	 * @param margin komponen margin baru.
	 */
	public void setMargin(Double margin) {
		this.margin = margin;
	}

	/**
	 * Nomor urut angsuran ini dalam jadwal cicilan header {@link #getTransaksiKoperasi()} (ke-1,
	 * ke-2, dst) -- dipakai a.l. utk menyusun teks ringkasan "angsuran ke berapa saja yang dibayar"
	 * pada {@code TunaiAnggotaKoperasiCommon}. {@code nullable = false}.
	 *
	 * @return nomor urut angsuran ini.
	 */
	@Column(nullable = false)
	public Integer getKe() {
		return ke;
	}

	/**
	 * Menetapkan nomor urut angsuran ini.
	 *
	 * @param ke nomor urut baru.
	 */
	public void setKe(Integer ke) {
		this.ke = ke;
	}

	/**
	 * Sisa pokok pinjaman SETELAH angsuran ini (saldo berjalan/running balance), dipakai laporan
	 * simpan-pinjam ({@code LaporanSimpanPinjamAction}/{@code SimpanPinjamReportService}).
	 *
	 * <p><b>Catatan integritas:</b> BERBEDA dari kebanyakan getter numerik lain di kelas ini
	 * ({@link #getPokok()}, {@link #getMargin()}) -- getter ini TIDAK null-safe, mengembalikan
	 * {@code null} apa adanya bila kolom NULL di DB. Sebagian besar pemanggil menjaga diri sendiri
	 * (mis. {@code d.getSisa() == null ? 0.0 : d.getSisa()} pada
	 * {@code LaporanSimpanPinjamAction}/{@code SimpanPinjamReportService}), tetapi
	 * {@code TransaksiKoperasiDetailAction} memformat hasilnya langsung lewat
	 * {@code Common.numberFormat.get().format(transaksiKoperasiDetail.getSisa())} tanpa null-check
	 * -- berisiko {@code NullPointerException} saat memformat baris yang {@code sisa}-nya belum
	 * pernah diisi. Dicatat sbg observasi (risiko tampilan/laporan pada baris data belum lengkap),
	 * bukan diajukan sbg task baru krn dampaknya kegagalan render UI utk kasus tepi, bukan celah
	 * keamanan atau integritas finansial.
	 *
	 * @return sisa pokok pinjaman setelah angsuran ini, atau {@code null} bila belum dihitung/diisi.
	 */
	public Double getSisa() {
		return sisa;
	}

	/**
	 * Menetapkan sisa pokok pinjaman setelah angsuran ini.
	 *
	 * @param sisa sisa pokok baru; boleh {@code null} (lihat catatan {@link #getSisa()}).
	 */
	public void setSisa(Double sisa) {
		this.sisa = sisa;
	}

	/**
	 * Baris pembayaran yang melunasi angsuran ini. {@code nullable = true}: {@code null} berarti
	 * angsuran ini BELUM DIBAYAR; nilai terisi berarti sudah dilunasi. Field ini SEKALI diisi lalu
	 * TIDAK pernah dikosongkan lagi oleh alur pembayaran normal ({@code TunaiAnggotaKoperasiCommon
	 * .onSave}) -- penanda satu-arah, konsisten dgn pola flag "aktif satu-arah" yang berulang pada
	 * domain finansial AIS (lihat Javadoc kelas). Pengecekan {@code == null} pada versi TERKELOLA
	 * (hasil {@code session.get(...)} ulang dari DB, bukan objek yang dibawa caller dari UI) inilah
	 * yang mencegah satu angsuran dibayar dua kali dalam satu pemanggilan {@code onSave} -- lihat
	 * Javadoc kelas &amp; {@code TunaiAnggotaKoperasiCommon}. Relasi {@code @ManyToOne} dgn
	 * {@code @Fetch(FetchMode.SELECT)} eksplisit (bukan default join) -- pola query terpisah per
	 * baris, bukan JOIN pada query utama yang memuat {@code TransaksiKoperasiDetail}.
	 *
	 * @return baris pembayaran yang melunasi angsuran ini, atau {@code null} bila belum dibayar.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pembayaran_anggota_koperasi_detail", nullable = true)
	public PembayaranAnggotaKoperasiDetail getPembayaranAnggotaKoperasiDetail() {
		return pembayaranAnggotaKoperasiDetail;
	}

	/**
	 * Menandai angsuran ini sudah dibayar dgn menautkannya ke baris {@link
	 * PembayaranAnggotaKoperasiDetail} yang bersangkutan. Tidak ada guard di level entity yang
	 * mencegah pemanggil menimpa nilai yang sudah terisi (mis. balik ke {@code null}, atau ganti ke
	 * baris pembayaran lain) -- disiplin "sekali diisi, jangan diubah lagi" murni konvensi kode
	 * pemanggil ({@code TunaiAnggotaKoperasiCommon}), bukan dijaga entity ini; lihat Javadoc
	 * {@link #getPembayaranAnggotaKoperasiDetail()}.
	 *
	 * @param pembayaranAnggotaKoperasiDetail baris pembayaran yang melunasi angsuran ini.
	 */
	public void setPembayaranAnggotaKoperasiDetail(PembayaranAnggotaKoperasiDetail pembayaranAnggotaKoperasiDetail) {
		this.pembayaranAnggotaKoperasiDetail = pembayaranAnggotaKoperasiDetail;
	}

	/**
	 * Nomor virtual account bank utk pembayaran online angsuran ini (lihat Javadoc kelas). Getter
	 * null-safe SEBAGIAN: mengembalikan {@code null} (bukan string kosong) bila kolom kosong/blank
	 * di DB -- berguna utk pengecekan {@code isEmpty(detail.getVa())} di kode pemanggin
	 * ({@code PembayaranKoperasiOnline}) tanpa perlu null-check terpisah. Kedaluwarsa VA ini
	 * ditentukan oleh {@link #getExpired()}, DIPERIKSA TERPISAH oleh pemanggil -- getter ini tidak
	 * menyembunyikan nilai yang sudah kedaluwarsa.
	 *
	 * @return nomor virtual account, atau {@code null} bila belum/tidak ada VA aktif.
	 */
	public String getVa() {
		return va == null || va.isEmpty() ? null : va;
	}

	/**
	 * Menetapkan nomor virtual account bank utk angsuran ini.
	 *
	 * @param va nomor virtual account baru.
	 */
	public void setVa(String va) {
		this.va = va;
	}

	/**
	 * Waktu kedaluwarsa {@link #getVa()}/{@link #getLink()}. Dipakai kode pemanggin
	 * ({@code PembayaranKoperasiOnline.isBelumExpired}) utk memutuskan apakah VA/link pembayaran
	 * online masih boleh ditampilkan ke anggota -- perbandingan tsb dilakukan DI LUAR kelas ini
	 * (getter ini murni mengembalikan nilai kolom apa adanya, tanpa logika kedaluwarsa).
	 *
	 * @return waktu kedaluwarsa VA/link, atau {@code null} bila tidak ada batas kedaluwarsa.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getExpired() {
		return expired;
	}

	/**
	 * Menetapkan waktu kedaluwarsa VA/link pembayaran online angsuran ini.
	 *
	 * @param expired waktu kedaluwarsa baru.
	 */
	public void setExpired(Date expired) {
		this.expired = expired;
	}

	/**
	 * URL link pembayaran online pihak ketiga utk angsuran ini (lihat Javadoc kelas). Getter TIDAK
	 * sekadar mengembalikan nilai kolom -- melakukan normalisasi: string kosong/{@code null} jadi
	 * {@code ""} (bukan {@code null}, berbeda dari pola {@link #getVa()}); nilai yang tidak berawal
	 * {@code "https"} otomatis DIBERI PREFIX {@code "https://"}; nilai lainnya di-{@code trim()}.
	 * Efek sampingnya: kode pemanggil yang membandingkan hasil getter ini dgn nilai yang aslinya
	 * disimpan via {@link #setLink(String)} bisa mendapati string berbeda (mis. link tanpa skema
	 * yang disimpan {@code "contoh.com/bayar"} akan terbaca {@code "https://contoh.com/bayar"}
	 * lewat getter, walau kolom DB tetap menyimpan versi asli tanpa skema) -- transformasi tampilan
	 * ini terjadi SETIAP getter dipanggil, bukan sekali saat simpan.
	 *
	 * @return link pembayaran online, dinormalisasi dgn prefix {@code https://} bila perlu; string
	 *         kosong ({@code ""}) bila belum diisi (tidak pernah {@code null}).
	 */
	@Column(columnDefinition = "text")
	public String getLink() {
		return link == null || link.isEmpty() ? "" : !link.startsWith("https") ? "https://" + link : link.trim();
	}

	/**
	 * Menetapkan link pembayaran online angsuran ini. Nilai disimpan APA ADANYA (tanpa normalisasi
	 * prefix {@code https://} -- normalisasi tsb hanya terjadi di {@link #getLink()} saat dibaca).
	 *
	 * @param link link pembayaran online baru.
	 */
	public void setLink(String link) {
		this.link = link;
	}
}
