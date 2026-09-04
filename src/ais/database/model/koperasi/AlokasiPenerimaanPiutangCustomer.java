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
 * Alokasi satu {@link PenerimaanPiutangCustomer} ke satu {@link PiutangCustomerDoc} --
 * cermin AP {@link AlokasiPembayaranHutangSupplier}. Invariant ditegakkan helper saat create:
 * &Sigma; alokasi = nominal penerimaan, dan tiap alokasi &le; outstanding fakturnya
 * (FOR UPDATE per faktur, tidak boleh overpayment).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "alokasi_penerimaan_piutang_customer")
public class AlokasiPenerimaanPiutangCustomer extends GeneralValueObject {

	/** Versi serialisasi tetap; entity ini tidak pernah dikirim lewat Java serialization jarak jauh,
	 * dipertahankan hanya krn kontrak {@link GeneralValueObject}/{@code Serializable}. */
	private static final long serialVersionUID = 1L;

	/** PK auto-generated (identity). Lihat {@link #getId()}. */
	private Long id;
	/** Sisi "sumber dana" alokasi -- satu penerimaan boleh dipecah ke banyak baris alokasi
	 * (satu per faktur yg dilunasi/dicicil). Lihat {@link #getPenerimaan()}. */
	private PenerimaanPiutangCustomer penerimaan;
	/** Sisi "target utang" alokasi -- faktur piutang customer yang menerima potongan outstanding
	 * dari baris ini. Lihat {@link #getPiutangDoc()}. */
	private PiutangCustomerDoc piutangDoc;
	/** Nominal yang dialokasikan dari {@link #penerimaan} ke {@link #piutangDoc} pada baris ini
	 * (bukan nominal penerimaan/faktur secara keseluruhan). Lihat {@link #getNominal()}. */
	private BigDecimal nominal;

	/**
	 * Callback JPA {@code @PreUpdate}: menandai kapan baris alokasi ini TERAKHIR diubah, dengan
	 * menuliskan waktu sekarang ke {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Dipanggil otomatis
	 * oleh Hibernate sebelum setiap {@code UPDATE}, TIDAK pernah dipanggil manual dari kode
	 * aplikasi. Baris alokasi pada praktiknya jarang di-update setelah dibuat (koreksi biasanya
	 * berupa hapus+buat ulang oleh helper penerimaan), sehingga field ini terutama berguna untuk
	 * jejak audit saat pembatalan/penyesuaian.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor tanpa argumen wajib JPA/Hibernate; jangan dipakai langsung dari kode aplikasi --
	 * gunakan setter untuk mengisi {@link #penerimaan}, {@link #piutangDoc}, dan {@link #nominal}
	 * sebelum {@code save}, sesuai invariant yang ditegakkan helper create (lihat Javadoc kelas). */
	public AlokasiPenerimaanPiutangCustomer() {
	}

	/**
	 * PK identity baris alokasi ini. {@code null} sebelum entity di-{@code save}/{@code flush} ke
	 * Hibernate (ID baru dibuat DB saat insert, strategi {@link IDENTITY}).
	 *
	 * @return id baris alokasi, atau {@code null} bila belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Setter PK -- dipanggil Hibernate saat memuat entity dari DB atau pengujian yang menyusun
	 * objek manual. Kode aplikasi normal tidak perlu memanggil ini; id baru dibuat otomatis oleh
	 * DB saat insert.
	 *
	 * @param id id baris alokasi.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Penerimaan piutang (sisi kas masuk dari customer) yang sebagian/seluruh nominalnya
	 * dialokasikan lewat baris ini. {@code nullable = false} -- setiap alokasi WAJIB berasal dari
	 * satu penerimaan. Relasi {@code LAZY}: mengakses field pada objek yang dikembalikan di luar
	 * sesi Hibernate yang masih terbuka akan melempar {@code LazyInitializationException}.
	 *
	 * @return penerimaan sumber dana alokasi ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "penerimaan", nullable = false)
	public PenerimaanPiutangCustomer getPenerimaan() {
		penerimaan = check(penerimaan);
		return penerimaan;
	}

	/**
	 * Menetapkan penerimaan sumber dana alokasi ini. Dipanggil helper create sebelum simpan; tidak
	 * mengecek ulang bahwa {@link #getNominal()} masih dalam batas sisa penerimaan -- penjagaan
	 * itu tugas pemanggil (lihat invariant di Javadoc kelas), bukan setter ini.
	 *
	 * @param penerimaan baris {@link PenerimaanPiutangCustomer} sumber dana.
	 */
	public void setPenerimaan(PenerimaanPiutangCustomer penerimaan) {
		this.penerimaan = penerimaan;
	}

	/**
	 * Faktur/dokumen piutang customer yang outstanding-nya dikurangi oleh nominal baris ini.
	 * {@code nullable = false}. Relasi {@code LAZY} -- sama catatan lazy-loading dgn
	 * {@link #getPenerimaan()}.
	 *
	 * @return dokumen piutang target alokasi ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "piutang_doc", nullable = false)
	public PiutangCustomerDoc getPiutangDoc() {
		piutangDoc = check(piutangDoc);
		return piutangDoc;
	}

	/**
	 * Menetapkan dokumen piutang target alokasi ini. Sama seperti {@link #setPenerimaan}, setter
	 * ini TIDAK memvalidasi bahwa nominal &le; outstanding faktur saat dipanggil -- validasi
	 * "FOR UPDATE per faktur" dilakukan helper create di lapisan servlet sebelum entity ini
	 * dibangun, bukan di setter.
	 *
	 * @param piutangDoc baris {@link PiutangCustomerDoc} target alokasi.
	 */
	public void setPiutangDoc(PiutangCustomerDoc piutangDoc) {
		this.piutangDoc = piutangDoc;
	}

	/**
	 * Nominal rupiah yang dialokasikan dari {@link #getPenerimaan()} ke {@link #getPiutangDoc()}
	 * pada baris ini. Getter null-safe: mengembalikan {@link BigDecimal#ZERO} bila kolom NULL di
	 * DB (baris lama/rusak), supaya kalkulasi SUM di lapisan pemanggil tidak perlu null-check
	 * berulang dan tidak melempar {@code NullPointerException} pada operasi aritmatika BigDecimal.
	 *
	 * @return nominal alokasi, tidak pernah {@code null}.
	 */
	@Column(name = "nominal", precision = 19, scale = 2)
	public BigDecimal getNominal() {
		return nominal == null ? BigDecimal.ZERO : nominal;
	}

	/**
	 * Menetapkan nominal alokasi baris ini. Tidak melakukan validasi apa pun (boleh negatif/nol
	 * bila dipanggil langsung) -- penjagaan "&Sigma; alokasi = nominal penerimaan" dan "alokasi
	 * &le; outstanding faktur" ada di helper create (query {@code FOR UPDATE}), BUKAN di sini.
	 * Memanggil setter ini di luar jalur helper tsb melewati penjagaan tersebut.
	 *
	 * @param nominal nominal alokasi baru, boleh {@code null} (diperlakukan sbg nol oleh getter).
	 */
	public void setNominal(BigDecimal nominal) {
		this.nominal = nominal;
	}

	/**
	 * Timestamp perubahan terakhir baris ini, diisi otomatis oleh {@link #onUpdate()} setiap kali
	 * Hibernate melakukan {@code UPDATE}. Nilai awal (saat objek dibuat, sebelum ada update
	 * apa pun) adalah waktu instansiasi objek Java ({@link ais.ui.util.WaktuUtil#getDate()}), BUKAN
	 * waktu insert DB sesungguhnya -- jangan dipakai sebagai "tanggal dibuat".
	 *
	 * @return waktu perubahan terakhir baris alokasi ini.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

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
}
