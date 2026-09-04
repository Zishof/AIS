package ais.database.model.koperasi;

import static javax.persistence.GenerationType.IDENTITY;

import java.math.BigDecimal;
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
import ais.database.model.inventory.PengadaanFaktur;

/**
 * Info hutang per faktur kulakan (layar legacy 20-23, varian Inventory &amp; Sales) -- EXTENSION
 * 1:1 di atas {@link PengadaanFaktur} existing (kontrak kulakan_faktur_* TIDAK diubah): jenis
 * pembayaran {@link #JENIS_CASH}/{@link #JENIS_DP}/{@link #JENIS_CREDIT}, termin (dasar jatuh
 * tempo, pola SYARAT_BYR/TRAN_HUT.DBF), dan nilai dibayar saat faktur (cash penuh / DP).
 *
 * <p>OUTSTANDING TIDAK DISIMPAN -- selalu dihitung: {@code totalFakturFinal - dibayarAwal -
 * SUM(alokasi pembayaran)} (register event, Matriks layar 22). Faktur legacy TANPA baris info
 * dianggap CASH lunas (alur kulakan lama memang tunai) -- TIDAK menimbulkan hutang diam-diam;
 * pemilik dapat melengkapi info lewat aksi {@code si_purchase_terms_save} bila faktur lama
 * ternyata kredit (backfill sadar, bukan tebakan migrasi).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "payable_faktur_info")
public class PayableFakturInfo extends GeneralValueObject {

	/** Versi serialisasi tetap; dipertahankan hanya krn kontrak {@link GeneralValueObject}/
	 * {@code Serializable}. */
	private static final long serialVersionUID = 1L;

	/** Nilai {@link #jenisPembayaran}: faktur dibayar tunai penuh saat faktur dibuat -- TIDAK
	 * menimbulkan hutang. Ini juga nilai DEFAULT yang dikembalikan {@link #getJenisPembayaran()}
	 * bila kolom kosong (lihat Javadoc kelas & getter). */
	public static final String JENIS_CASH = "CASH";
	/** Nilai {@link #jenisPembayaran}: dibayar sebagian di muka ({@link #dibayarAwal}), sisanya
	 * jadi hutang kredit hingga {@link #jatuhTempo}. */
	public static final String JENIS_DP = "DP";
	/** Nilai {@link #jenisPembayaran}: seluruh nilai faktur menjadi hutang kredit hingga
	 * {@link #jatuhTempo}, tanpa pembayaran di muka. */
	public static final String JENIS_CREDIT = "CREDIT";

	/** PK auto-generated (identity). Lihat {@link #getId()}. */
	private Long id;
	/** Faktur kulakan (pengadaan) yang info hutangnya diatur baris ini -- relasi 1:1 EXTENSION,
	 * lihat Javadoc kelas. Lihat {@link #getPengadaanFaktur()}. */
	private PengadaanFaktur pengadaanFaktur;
	/** Jenis pembayaran faktur: {@link #JENIS_CASH}/{@link #JENIS_DP}/{@link #JENIS_CREDIT}. Lihat
	 * {@link #getJenisPembayaran()}. */
	private String jenisPembayaran;
	/** Termin (hari) dasar perhitungan {@link #jatuhTempo}, pola legacy SYARAT_BYR/TRAN_HUT.DBF.
	 * Lihat {@link #getTerminHari()}. */
	private Integer terminHari;
	/** Tanggal jatuh tempo hutang faktur ini = tanggal faktur + {@link #terminHari}. Lihat
	 * {@link #getJatuhTempo()}. */
	private Date jatuhTempo;
	/** Nilai yang sudah dibayar SAAT faktur dibuat (cash penuh, atau DP) -- TIDAK termasuk
	 * pembayaran/alokasi belakangan (lihat {@code AlokasiPembayaranHutangSupplier}). Lihat
	 * {@link #getDibayarAwal()}. */
	private BigDecimal dibayarAwal;
	/** Catatan bebas ttg info hutang faktur ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Nama petugas yang membuat/mengubah baris info ini (jejak audit tampilan, bukan FK). Lihat
	 * {@link #getOleh()}. */
	private String oleh;
	/** ID/username petugas yang membuat/mengubah baris info ini. Lihat {@link #getOlehId()}. */
	private String olehId;
	/** Waktu baris ini dibuat/dientri. Lihat {@link #getWaktu()}. */
	private Date waktu;
	/**
	 * Callback JPA {@code @PreUpdate}: menandai kapan baris ini TERAKHIR diubah, dengan menuliskan
	 * waktu sekarang ke {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Dipanggil otomatis
	 * oleh Hibernate sebelum {@code UPDATE}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor tanpa argumen wajib JPA/Hibernate. */
	public PayableFakturInfo() {
	}

	/**
	 * PK identity baris info ini. {@code null} sebelum entity di-{@code save}/{@code flush} ke
	 * Hibernate (ID baru dibuat DB saat insert, strategi {@link IDENTITY}).
	 *
	 * @return id baris info, atau {@code null} bila belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Setter PK -- dipanggil Hibernate saat memuat entity dari DB. Kode aplikasi normal tidak
	 * perlu memanggil ini; id baru dibuat otomatis oleh DB saat insert.
	 *
	 * @param id id baris info.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Faktur kulakan (pengadaan) yang info hutangnya diatur baris ini. {@code nullable = false} --
	 * relasi 1:1 EXTENSION di atas {@link PengadaanFaktur} (lihat Javadoc kelas): faktur legacy
	 * TANPA baris {@code PayableFakturInfo} sama sekali dianggap CASH lunas oleh kode pemanggil,
	 * BUKAN oleh getter ini -- objek ini sendiri baru ada bila baris info sudah dibuat. Relasi
	 * {@code LAZY}: mengakses field pada objek di luar sesi Hibernate yang masih terbuka akan
	 * melempar {@code LazyInitializationException}.
	 *
	 * @return faktur kulakan terkait info hutang ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pengadaan_faktur", nullable = false)
	public PengadaanFaktur getPengadaanFaktur() {
		pengadaanFaktur = check(pengadaanFaktur);
		return pengadaanFaktur;
	}

	/**
	 * Menetapkan faktur kulakan terkait. Dipakai saat membangun baris info baru (upsert lewat aksi
	 * {@code si_purchase_terms_save} -- lihat Javadoc kelas); tidak ada guard di level entity yang
	 * mencegah baris info kedua dibuat utk faktur yang sama -- pencegahan duplikat (bila ada)
	 * adalah tanggung jawab helper pemanggil (mis. cek {@code createCriteria} sebelum insert).
	 *
	 * @param pengadaanFaktur faktur kulakan yang diatur info hutangnya.
	 */
	public void setPengadaanFaktur(PengadaanFaktur pengadaanFaktur) {
		this.pengadaanFaktur = pengadaanFaktur;
	}

	/**
	 * Jenis pembayaran faktur ini: {@link #JENIS_CASH}, {@link #JENIS_DP}, atau
	 * {@link #JENIS_CREDIT}. Getter null-safe: mengembalikan {@link #JENIS_CASH} bila kolom NULL
	 * atau kosong di DB -- DEFAULT CASH, konsisten dgn perlakuan "faktur tanpa baris info = CASH
	 * lunas" pada Javadoc kelas (baris yang sudah ada tapi belum diisi jenisnya juga jatuh ke CASH,
	 * bukan galat).
	 *
	 * @return jenis pembayaran, tidak pernah {@code null}/kosong.
	 */
	@Column(name = "jenis_pembayaran", length = 20)
	public String getJenisPembayaran() {
		return jenisPembayaran == null || jenisPembayaran.trim().isEmpty() ? JENIS_CASH : jenisPembayaran;
	}

	/**
	 * Menetapkan jenis pembayaran faktur. Tidak ada validasi di level entity bahwa nilainya salah
	 * satu dari {@link #JENIS_CASH}/{@link #JENIS_DP}/{@link #JENIS_CREDIT} -- validasi tsb
	 * dilakukan di helper aksi ({@code si_purchase_terms_save}) sebelum dipanggil; menyimpan
	 * langsung via entity dgn nilai lain akan diam-diam LOLOS di level DB (kolom {@code varchar}
	 * bebas), tetapi kemudian selalu jatuh balik ke {@link #JENIS_CASH} saat dibaca lewat
	 * {@link #getJenisPembayaran()} krn getter hanya mengenali string kosong/null sbg alasan
	 * fallback -- string tak dikenal lain (mis. salah ketik) akan LOLOS APA ADANYA tanpa fallback,
	 * berpotensi membingungkan kode pemanggil yang membandingkan persis dgn konstanta. Dicatat sbg
	 * observasi (pola longgar validasi enum-as-String yang umum di model AIS), bukan diajukan sbg
	 * task baru krn jalur masuk satu-satunya (helper aksi) sudah memvalidasi nilai sebelum sampai
	 * ke setter ini.
	 *
	 * @param jenisPembayaran jenis pembayaran baru.
	 */
	public void setJenisPembayaran(String jenisPembayaran) {
		this.jenisPembayaran = jenisPembayaran;
	}

	/**
	 * Termin (hari) dasar perhitungan {@link #getJatuhTempo()}, pola legacy
	 * SYARAT_BYR/TRAN_HUT.DBF. Getter null-safe: mengembalikan {@code 0} bila kolom NULL di DB.
	 *
	 * @return termin hari, tidak pernah {@code null}.
	 */
	@Column(name = "termin_hari")
	public Integer getTerminHari() {
		return terminHari == null ? Integer.valueOf(0) : terminHari;
	}

	/**
	 * Menetapkan termin pembayaran faktur ini. Mengubah nilai ini TIDAK otomatis menghitung ulang
	 * {@link #getJatuhTempo()} -- {@code jatuhTempo} adalah kolom tersimpan terpisah yang dihitung
	 * ULANG secara eksplisit oleh helper simpan saat info disimpan (lihat Javadoc
	 * {@link #getJatuhTempo()}), bukan derivasi otomatis dari getter ini.
	 *
	 * @param terminHari termin baru dalam hari.
	 */
	public void setTerminHari(Integer terminHari) {
		this.terminHari = terminHari;
	}

	/** Jatuh tempo = tanggal faktur + termin (dihitung &amp; disimpan saat simpan info -- kolom
	 *  sendiri supaya bisa di-query aging tanpa join berulang, dan bisa dikoreksi manual). */
	@Temporal(TemporalType.DATE)
	@Column(name = "jatuh_tempo")
	public Date getJatuhTempo() {
		return jatuhTempo;
	}

	/**
	 * Menetapkan tanggal jatuh tempo. Biasanya diisi otomatis (tanggal faktur + termin) oleh
	 * helper simpan saat info disimpan/diubah -- lihat Javadoc {@link #getJatuhTempo()}; setter ini
	 * tidak melarang pengisian manual, sesuai catatan "bisa dikoreksi manual" pada Javadoc getter.
	 *
	 * @param jatuhTempo tanggal jatuh tempo baru.
	 */
	public void setJatuhTempo(Date jatuhTempo) {
		this.jatuhTempo = jatuhTempo;
	}

	/**
	 * Nilai yang sudah dibayar SAAT faktur dibuat (cash penuh, atau DP) -- lihat Javadoc kelas &
	 * {@link #JENIS_CASH}/{@link #JENIS_DP}. Getter null-safe: mengembalikan {@link BigDecimal#ZERO}
	 * bila kolom NULL di DB (mis. faktur {@link #JENIS_CREDIT} tanpa pembayaran di muka sama
	 * sekali).
	 *
	 * <p><b>Catatan integritas:</b> nilai ini TIDAK termasuk pembayaran/alokasi hutang yang terjadi
	 * belakangan (register event terpisah, lihat {@code AlokasiPembayaranHutangSupplier}) -- rumus
	 * outstanding lengkap adalah {@code totalFakturFinal - dibayarAwal - SUM(alokasi pembayaran)}
	 * (lihat Javadoc kelas), sehingga membaca field ini SENDIRIAN tanpa menjumlahkan alokasi akan
	 * meremehkan seberapa besar hutang sudah terbayar.
	 *
	 * @return nilai dibayar awal, tidak pernah {@code null}.
	 */
	@Column(name = "dibayar_awal", precision = 19, scale = 2)
	public BigDecimal getDibayarAwal() {
		return dibayarAwal == null ? BigDecimal.ZERO : dibayarAwal;
	}

	/**
	 * Menetapkan nilai dibayar awal. Tidak ada validasi di level entity bahwa nilainya &le; total
	 * faktur -- validasi tsb (bila ada) dilakukan di helper aksi sebelum simpan; lihat catatan
	 * serupa pada Javadoc {@code getHarga()} milik {@link HargaJualCustomer}/
	 * {@link HargaBeliSupplier} soal pola longgar validasi nilai finansial pada model AIS.
	 *
	 * @param dibayarAwal nilai dibayar awal baru.
	 */
	public void setDibayarAwal(BigDecimal dibayarAwal) {
		this.dibayarAwal = dibayarAwal;
	}

	/**
	 * Catatan bebas ttg info hutang faktur ini.
	 *
	 * @return keterangan, atau {@code null}/kosong bila tidak diisi.
	 */
	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menetapkan keterangan. Tidak ada guard null/blank -- memanggil dgn string kosong akan
	 * menimpa nilai lama.
	 *
	 * @param keterangan catatan bebas baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Nama petugas yang membuat/mengubah baris info ini.
	 *
	 * @return nama petugas, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menetapkan {@link #oleh}. Guard null/blank: nilai {@code null}/kosong/spasi DIABAIKAN
	 * (early return) -- field yang sudah terisi tidak ditimpa balik ke kosong.
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
	 * ID/username petugas yang membuat/mengubah baris info ini.
	 *
	 * @return id/username petugas, atau {@code null} bila belum pernah diisi.
	 */
	@Column(name = "oleh_id")
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan {@link #olehId}. Guard null/blank sama seperti {@link #setOleh(String)}.
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
	 * Waktu baris info ini dibuat/dientri. Getter null-safe: mengembalikan waktu SEKARANG
	 * ({@link ais.ui.util.WaktuUtil#getDate()}) bila kolom NULL, dihitung ULANG setiap kali getter
	 * dipanggil pada baris yang kolomnya NULL (bukan waktu tetap saat objek dibuat).
	 *
	 * @return waktu entry baris ini, atau waktu panggilan getter saat ini bila kolom NULL.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu")
	public Date getWaktu() {
		return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
	}

	/**
	 * Menetapkan waktu entry baris ini.
	 *
	 * @param waktu waktu baris ini dibuat/dientri.
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
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
	 * Setter manual utk {@link #tanggal_dirubah}. Jarang dipakai langsung -- field ini biasanya
	 * diisi otomatis oleh {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah waktu perubahan yang ingin dicatat.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
