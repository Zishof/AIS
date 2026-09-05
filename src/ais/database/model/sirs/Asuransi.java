package ais.database.model.sirs;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Entitas katalog master penjamin/payer pasien pada schema {@code sirs}
 * (tabel {@code asuransi}) — MESKI namanya "Asuransi", kelas ini adalah
 * MASTER PAYER TUNGGAL untuk seluruh jenis penjamin pembayaran pasien,
 * bukan sebatas perusahaan asuransi. Diverifikasi dari kode
 * ({@code PenjaminResolver}, Blueprint Integrasi SIRS Fase 2): baris
 * "Umum" (bayar sendiri), BPJS, asuransi swasta, perusahaan (penjamin
 * korporat), maupun jenis penjamin lain semuanya adalah baris di
 * katalog ini, dibedakan lewat diskriminator {@link #getJenisPayer()}
 * dengan konstanta {@link #PAYER_UMUM}, {@link #PAYER_BPJS},
 * {@link #PAYER_ASURANSI_SWASTA}, {@link #PAYER_PERUSAHAAN}, dan
 * {@link #PAYER_LAINNYA} — bukan lima entitas/tabel terpisah.
 *
 * <p>
 * Dipakai luas sebagai relasi {@code ManyToOne} penentu tarif dari
 * berbagai entitas tindakan/item/alat medis (lihat pemakaian di
 * {@code CommonTarifTindakan}, {@code CommonTarifItem},
 * {@code CommonTarifAlatMedis}, {@code CommonTarif},
 * {@code CommonPendaftaranUtil}, {@code TarifKhususAction},
 * {@code DiskonAction}, {@code PajakAction}) — mis. tarif tindakan bisa
 * berbeda untuk pasien ber-BPJS dibanding pasien umum. Diagnosa
 * penjamin efektif suatu kunjungan diresolusi lewat
 * {@code PenjaminResolver.asuransiEfektif(Pendaftaran)}, yang jatuh
 * balik ke penjamin milik {@link Pasien} bila {@code Pendaftaran} tidak
 * membawa penjamin sendiri.
 * </p>
 *
 * <p>
 * Empat field {@link #getJenisPayer()}, {@link #getKodePayer()},
 * {@link #getNomorPks()}, dan {@link #getAktif()} adalah PENGAYAAN
 * belakangan (Blueprint Fase 2) yang SEMUANYA nullable/aditif demi
 * kompatibilitas data lama — baris lama tanpa {@code jenisPayer} terisi
 * otomatis diperlakukan sebagai {@link #PAYER_UMUM} lewat
 * {@link #getJenisPayerEfektif()}, BUKAN {@code null}/error.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "asuransi")
public class Asuransi extends GeneralValueObject {

	// --- Diskriminator tipe penjamin (Blueprint Integrasi SIRS — Fase 2) ---
	// Asuransi TETAP master payer tunggal; BPJS hanyalah salah satu tipe di sini.
	// Nilai null diperlakukan sebagai pembayaran UMUM (kompatibel data lama).
	/** Nilai {@link #getJenisPayer()} untuk penjamin "Umum" (bayar sendiri); juga nilai efektif default bila {@code jenisPayer} kosong — lihat {@link #getJenisPayerEfektif()}. */
	public static final String PAYER_UMUM = "UMUM";
	/** Nilai {@link #getJenisPayer()} untuk penjamin BPJS. */
	public static final String PAYER_BPJS = "BPJS";
	/** Nilai {@link #getJenisPayer()} untuk penjamin asuransi swasta (non-BPJS). */
	public static final String PAYER_ASURANSI_SWASTA = "ASURANSI_SWASTA";
	/** Nilai {@link #getJenisPayer()} untuk penjamin korporat/perusahaan. */
	public static final String PAYER_PERUSAHAAN = "PERUSAHAAN";
	/** Nilai {@link #getJenisPayer()} untuk jenis penjamin lain di luar kategori baku. */
	public static final String PAYER_LAINNYA = "LAINNYA";

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String olehId;

	/**
	 * Mengambil ID pengguna (username/oleh-id) yang terakhir mengubah baris
	 * ini. Field audit shadow yang diisi lewat {@link #setOlehId(String)}.
	 *
	 * @return ID pengguna terakhir yang mengubah, atau {@code null} jika
	 *         belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna yang mengubah baris ini. Nilai kosong/blank
	 * SENGAJA diabaikan (early return) agar field audit ini tidak pernah
	 * ditimpa jadi kosong — pola field audit shadow yang merupakan
	 * KEHARUSAN TEKNIS berulang di paket model AIS, bukan bug.
	 *
	 * @param olehId ID pengguna; nilai {@code null} atau string
	 *               kosong/whitespace-saja tidak akan mengubah nilai
	 *               tersimpan.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	private String oleh;

	/**
	 * Representasi ringkas penjamin/payer ini untuk keperluan
	 * tampilan/log.
	 *
	 * @return nama penjamin.
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Menetapkan nama pengguna yang mengubah baris ini. Nilai kosong/blank
	 * diabaikan agar field audit ini tidak pernah ditimpa kosong.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau
	 *             kosong/whitespace-saja tidak akan mengubah nilai
	 *             tersimpan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah baris ini.
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
	 * Menetapkan timestamp perubahan terakhir baris ini secara manual.
	 *
	 * @param tanggal_dirubah timestamp perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil timestamp perubahan terakhir baris ini, diperbarui
	 * otomatis oleh {@link #onUpdate()} setiap kali baris di-UPDATE.
	 *
	 * @return timestamp perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	private String nama;
	private String keterangan;

	// Pengayaan master payer — semua nullable/aditif.
	private String jenisPayer; // salah satu konstanta PAYER_* di atas; null = UMUM
	private String kodePayer; // kode eksternal payer (mis. kode BPJS)
	private String nomorPks; // Nomor Perjanjian Kerja Sama dengan payer
	private Boolean aktif = Boolean.TRUE;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public Asuransi() {
	}

	/**
	 * Primary key baris penjamin/payer, auto-increment (IDENTITY) dan
	 * diisi database.
	 *
	 * @return ID unik penjamin/payer ini.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID penjamin/payer.
	 *
	 * @param id ID penjamin/payer.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil nama penjamin/payer (mis. "Umum", "BPJS Kesehatan",
	 * nama perusahaan asuransi/korporat).
	 *
	 * @return nama penjamin/payer.
	 */
	@Column(name = "nama", nullable = false, length = 50)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menetapkan nama penjamin/payer.
	 *
	 * @param nama nama penjamin/payer.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil keterangan bebas penjamin/payer ini.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas penjamin/payer ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengambil diskriminator tipe payer baris ini — salah satu konstanta
	 * {@link #PAYER_UMUM}/{@link #PAYER_BPJS}/{@link #PAYER_ASURANSI_SWASTA}/
	 * {@link #PAYER_PERUSAHAAN}/{@link #PAYER_LAINNYA}, atau {@code null}
	 * pada baris data lama yang dibuat sebelum pengayaan Fase 2 ada. Untuk
	 * nilai yang sudah memperlakukan {@code null} sebagai UMUM, pakai
	 * {@link #getJenisPayerEfektif()}, bukan getter mentah ini.
	 *
	 * @return nilai jenis payer mentah, atau {@code null} jika belum
	 *         pernah diisi (data lama).
	 */
	@Column(name = "jenis_payer", nullable = true, length = 30)
	public String getJenisPayer() {
		return jenisPayer;
	}

	/**
	 * Menetapkan diskriminator tipe payer baris ini.
	 *
	 * @param jenisPayer salah satu konstanta {@code PAYER_*}, atau
	 *                   {@code null} untuk memperlakukannya sebagai
	 *                   {@link #PAYER_UMUM} (lihat
	 *                   {@link #getJenisPayerEfektif()}).
	 */
	public void setJenisPayer(String jenisPayer) {
		this.jenisPayer = jenisPayer;
	}

	/**
	 * Tipe payer efektif: null diperlakukan sebagai UMUM agar kompatibel data lama.
	 * Derived, bukan kolom DB — WAJIB @Transient agar Hibernate tidak mencari setter
	 * "jenisPayerEfektif" (properti tak bernama field asli akan gagal saat startup).
	 */
	@Transient
	public String getJenisPayerEfektif() {
		return (jenisPayer == null || jenisPayer.trim().isEmpty()) ? PAYER_UMUM : jenisPayer;
	}

	/** Derived, bukan kolom DB — WAJIB @Transient agar Hibernate tidak mencari setter "bpjs". */
	@Transient
	public boolean isBpjs() {
		return PAYER_BPJS.equals(getJenisPayerEfektif());
	}

	/**
	 * Mengambil kode eksternal payer ini (mis. kode BPJS resmi), dipakai
	 * untuk integrasi dengan sistem eksternal payer. Pengayaan Fase 2,
	 * nullable — data lama tidak mengisinya.
	 *
	 * @return kode eksternal payer, atau {@code null} jika belum diisi.
	 */
	@Column(name = "kode_payer", nullable = true, length = 30)
	public String getKodePayer() {
		return kodePayer;
	}

	/**
	 * Menetapkan kode eksternal payer ini.
	 *
	 * @param kodePayer kode eksternal payer.
	 */
	public void setKodePayer(String kodePayer) {
		this.kodePayer = kodePayer;
	}

	/**
	 * Mengambil nomor Perjanjian Kerja Sama (PKS) rumah sakit dengan
	 * payer ini. Pengayaan Fase 2, nullable — data lama tidak
	 * mengisinya.
	 *
	 * @return nomor PKS, atau {@code null} jika belum diisi.
	 */
	@Column(name = "nomor_pks", nullable = true, length = 50)
	public String getNomorPks() {
		return nomorPks;
	}

	/**
	 * Menetapkan nomor Perjanjian Kerja Sama (PKS) dengan payer ini.
	 *
	 * @param nomorPks nomor PKS.
	 */
	public void setNomorPks(String nomorPks) {
		this.nomorPks = nomorPks;
	}

	/**
	 * Mengambil flag aktif/tidak-aktif penjamin/payer ini. Field
	 * diinisialisasi ke {@code Boolean.TRUE} sejak deklarasi, dan getter
	 * ini juga membaca {@code null} (mis. dari baris lama sebelum kolom
	 * ini ada) sebagai {@code true} — sehingga hasilnya tidak pernah
	 * {@code null} meski field {@link #aktif} sendiri bisa saja
	 * {@code null} pada data lama.
	 *
	 * @return {@code true} jika payer aktif; default {@code true} bila
	 *         belum pernah diset/pada data lama.
	 */
	@Column(name = "aktif", nullable = true)
	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	/**
	 * Menetapkan flag aktif/tidak-aktif penjamin/payer ini.
	 *
	 * @param aktif {@code true} jika payer aktif dipakai/ditampilkan.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
