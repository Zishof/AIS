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

/**
 * Ledger kas sesi sales lapangan -- APPEND-ONLY (ERD &sect;3.10): tidak pernah update/delete,
 * koreksi = baris REVERSAL. Jenis: OPENING_ADVANCE, COLLECTION_CASH, CASH_SALE, EXPENSE_CASH,
 * PURCHASE_PAYMENT, OWNER_DEPOSIT, REFUND, ADJUSTMENT, REVERSAL. Saldo kas fisik seharusnya
 * SELALU dihitung dari ledger ini (bukan agregat tersimpan).
 *
 * <p><b>Padanan tenant-schema.</b> Tabel {@code {S}.sales_trip_kas} pada model tenant paralel
 * (dipakai saat {@code SalesInventoryTripTenant.aktif(ctx)} true) adalah padanan langsung
 * class ini dengan sembilan jenis dan aturan tanda yang SAMA PERSIS -- lihat kontrak
 * lengkapnya (tabel jenis/tanda, alasan tidak dipasang {@code CHECK} constraint) pada
 * {@link ais.service.tenant.TenantKasTrip}. Nilai konstanta {@code JENIS_*} di sini sengaja
 * disamakan dengan konstanta {@link ais.service.tenant.TenantKasTrip} supaya pemindahan data
 * legacy tidak memerlukan tabel terjemahan.</p>
 *
 * <p><b>Kontrol operasional, BUKAN jalur posting jurnal.</b> Berbeda dari
 * {@link NotaSalesBiaya} (yang nilainya dijurnal ke buku besar lewat
 * {@code PostingBiayaSalesUtil}, dok 61 butir E), ledger kas sesi ini murni catatan laci kas
 * operasional -- sejenis sesi kas kasir yang memang tidak dijurnal per baris/per sesi. Baris
 * {@link #JENIS_EXPENSE_CASH} dan {@link #JENIS_PURCHASE_PAYMENT} DITULIS BERSAMAAN
 * (dari kode pemanggil yang sama, {@code SalesInventoryTripHelper.catatKas}) saat dokumen
 * {@link NotaSalesBiaya}/{@link NotaSalesPembelian} metode TUNAI disimpan, tetapi baris kas
 * itu sendiri tidak membawa referensi ke {@link ais.database.model.akunting.PostingHistory}
 * apa pun.</p>
 *
 * <p><b>Tanpa penjaga keseimbangan otomatis.</b> Tidak ada trigger/constraint DB maupun
 * pemeriksaan sisi aplikasi yang memaksa {@code SUM(nominal)} ledger ini sama dengan kas
 * fisik -- rekonsiliasi ({@link NotaSalesSession#getSelisihKas()}) murni informasional dan
 * tidak memblokir penutupan sesi, lihat javadoc {@link NotaSalesSession}.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "nota_sales_kas")
public class NotaSalesKas extends GeneralValueObject {

	/**
	 * Nomor versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}.
	 */
	private static final long serialVersionUID = 1L;

	/** Uang muka operasional saat berangkat. Tanda positif (masuk kas). */
	public static final String JENIS_OPENING = "OPENING_ADVANCE";
	/** Penagihan piutang customer yang diterima tunai di lapangan. Tanda positif. */
	public static final String JENIS_COLLECTION_CASH = "COLLECTION_CASH";
	/** Penjualan tunai langsung di lapangan. Tanda positif. */
	public static final String JENIS_CASH_SALE = "CASH_SALE";
	/**
	 * Biaya operasional dibayar tunai. Tanda negatif -- ditulis otomatis oleh
	 * {@code SalesInventoryTripHelper.catatKas} (nilai {@code nilai.negate()}) saat dokumen
	 * {@link NotaSalesBiaya} dengan {@link NotaSalesBiaya#METODE_TUNAI} disimpan.
	 */
	public static final String JENIS_EXPENSE_CASH = "EXPENSE_CASH";
	/**
	 * Pembayaran ke pemasok dari kas yang dipegang sales. Tanda negatif -- ditulis otomatis
	 * saat {@link NotaSalesPembelian#getDibayarSesi()} &gt; 0.
	 */
	public static final String JENIS_PURCHASE_PAYMENT = "PURCHASE_PAYMENT";
	/** Setoran kembali ke pemilik. Tanda negatif. */
	public static final String JENIS_OWNER_DEPOSIT = "OWNER_DEPOSIT";
	/** Pengembalian uang ke pelanggan. Tanda negatif. */
	public static final String JENIS_REFUND = "REFUND";
	/** Koreksi selisih hitung fisik. Boleh bertanda positif maupun negatif. */
	public static final String JENIS_ADJUSTMENT = "ADJUSTMENT";
	/**
	 * Baris pembalik atas satu baris lain -- tandanya berlawanan dengan baris yang dibalik.
	 * Ditulis oleh {@code SalesInventoryReversalHelper} saat dokumen biaya/pembelian TUNAI
	 * direversal, dengan {@link #referensi} mengarah ke kode reversal dokumen induknya.
	 */
	public static final String JENIS_REVERSAL = "REVERSAL";

	/** Primary key baris {@code nota_sales_kas}, di-generate DB (identity). */
	private Long id;

	/** Pengait wajib ke {@link NotaSalesSession} pemilik baris ledger ini. */
	private NotaSalesSession sesi;

	/** Jenis baris kas, salah satu konstanta {@code JENIS_*} (tidak divalidasi CHECK di DB). */
	private String jenis;

	/** Nominal BERTANDA (lihat javadoc {@link #getNominal()}); saldo = SUM(nominal) polos. */
	private BigDecimal nominal;

	/** Referensi bebas ke dokumen sumber baris ini (mis. {@code "BIAYA-123"}, {@code "BELI-45"}). */
	private String referensi;

	/** Keterangan/uraian bebas baris kas ini. */
	private String keterangan;

	/** Waktu baris ledger ditulis, diinisialisasi lazy oleh getter bila belum pernah diisi. */
	private Date waktu;

	/**
	 * Hook Hibernate {@code @PreUpdate}: dipanggil otomatis sebelum setiap UPDATE terhadap
	 * baris ini, mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk mengisi
	 * ulang {@link #tanggal_dirubah}. Dalam praktiknya baris ledger ini bersifat append-only
	 * (lihat javadoc kelas) sehingga hook UPDATE ini seharusnya jarang/tidak pernah terpicu
	 * dari jalur normal aplikasi -- ada murni sebagai jaring pengaman konsisten dengan
	 * entity lain di basis kode ini.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor default (dibutuhkan Hibernate/JPA), tidak menginisialisasi field apa pun. */
	public NotaSalesKas() {
	}

	/**
	 * Mengambil primary key baris {@code NotaSalesKas} ini.
	 *
	 * @return {@link #id}, {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi primary key secara manual.
	 *
	 * @param id primary key yang ingin diset pada objek in-memory
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil {@link NotaSalesSession} pemilik baris ledger ini.
	 *
	 * @return {@link #sesi}, seharusnya tidak pernah {@code null} pada baris yang sudah
	 *         tersimpan ({@code nullable = false} pada kolom {@code sesi})
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sesi", nullable = false)
	public NotaSalesSession getSesi() {
		sesi = check(sesi);
		return sesi;
	}

	/**
	 * Mengisi pengait sesi pemilik baris ledger ini.
	 *
	 * @param sesi sesi baru
	 */
	public void setSesi(NotaSalesSession sesi) {
		this.sesi = sesi;
	}

	/**
	 * Mengambil jenis baris kas.
	 *
	 * @return {@link #jenis} apa adanya, sebaiknya salah satu konstanta {@code JENIS_*}
	 *         tetapi TIDAK divalidasi CHECK di DB maupun di getter ini
	 */
	@Column(name = "jenis", length = 30, nullable = false)
	public String getJenis() {
		return jenis;
	}

	/**
	 * Mengisi jenis baris kas tanpa validasi apa pun terhadap daftar konstanta {@code JENIS_*}
	 * -- pemanggil bertanggung jawab memakai nilai yang sah.
	 *
	 * @param jenis jenis baru
	 */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/** Bertanda: masuk kas = positif, keluar kas = negatif (EXPENSE/PURCHASE/DEPOSIT ditulis
	 *  negatif oleh helper) -- saldo = SUM(nominal) polos. */
	@Column(name = "nominal", precision = 19, scale = 2)
	public BigDecimal getNominal() {
		return nominal == null ? BigDecimal.ZERO : nominal;
	}

	/**
	 * Mengisi nominal bertanda baris kas ini. Setter ini TIDAK memaksa tanda sesuai jenis
	 * (mis. tidak menolak {@link #JENIS_EXPENSE_CASH} bernilai positif) -- kebenaran tanda
	 * sepenuhnya bergantung pada disiplin kode pemanggil, konsisten dengan padanan tenant
	 * {@link ais.service.tenant.TenantKasTrip#selaluKeluar(String)}/
	 * {@link ais.service.tenant.TenantKasTrip#selaluMasuk(String)} yang di jalur legacy ini
	 * TIDAK ADA pemeriksaan setara.
	 *
	 * @param nominal nilai baru, bertanda sesuai konvensi jenis baris
	 */
	public void setNominal(BigDecimal nominal) {
		this.nominal = nominal;
	}

	/**
	 * Mengambil referensi bebas ke dokumen sumber baris ini.
	 *
	 * @return {@link #referensi} apa adanya, bisa {@code null}
	 */
	@Column(name = "referensi", length = 120)
	public String getReferensi() {
		return referensi;
	}

	/**
	 * Mengisi referensi bebas ke dokumen sumber baris ini (mis. {@code "BIAYA-" + id},
	 * {@code "BELI-" + id}, atau kode reversal).
	 *
	 * @param referensi teks referensi baru
	 */
	public void setReferensi(String referensi) {
		this.referensi = referensi;
	}

	/**
	 * Mengambil keterangan/uraian bebas baris kas ini.
	 *
	 * @return {@link #keterangan} apa adanya, bisa {@code null}
	 */
	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Mengisi keterangan/uraian bebas baris kas ini.
	 *
	 * @param keterangan teks keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengambil waktu baris ledger ditulis, diinisialisasi lazy bila belum pernah diisi.
	 *
	 * @return {@link #waktu} bila sudah diisi; {@link ais.ui.util.WaktuUtil#getDate()} (waktu
	 *         saat ini) bila belum -- TIDAK PERNAH mengembalikan {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu")
	public Date getWaktu() {
		return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
	}

	/**
	 * Mengisi waktu baris ledger secara manual.
	 *
	 * @param waktu timestamp baru
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Mengambil timestamp perubahan terakhir baris ini.
	 *
	 * @return {@link #tanggal_dirubah}, tidak pernah {@code null} setelah objek dikonstruksi
	 *         (inisialisasi eager pada deklarasi field, diperbarui otomatis oleh
	 *         {@link #onUpdate()} pada setiap UPDATE)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Mengisi timestamp perubahan terakhir secara manual. Umumnya tidak perlu dipanggil
	 * langsung karena {@link #onUpdate()} sudah mengisinya otomatis setiap UPDATE.
	 *
	 * @param tanggal_dirubah timestamp baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
