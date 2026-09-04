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
import javax.persistence.Version;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.Toko;

/**
 * Surat Perintah Sales Jalan / SPJ (layar legacy 39, ERD &sect;3.2) -- pusat assignment
 * barang dibawa ({@link SpjSalesBarang}) dan nota/invoice dibawa ({@link SpjSalesNota})
 * untuk satu keberangkatan sales. State machine (ERD &sect;6):
 * {@code DRAFT -> SUBMITTED -> APPROVED -> ACTIVE -> RETURNED -> RECONCILING -> CLOSED};
 * {@code DRAFT/SUBMITTED -> CANCELLED}; {@code APPROVED -> CANCELLED} hanya sebelum
 * berangkat dan wajib beralasan. Realisasinya = satu {@link NotaSalesSession} (dibuat
 * saat mulai jalan). Nomor {@code SPJ-{toko}-{id 6 digit}} pasca-insert (tanpa MAX+1).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "surat_perintah_sales_jalan")
public class SuratPerintahSalesJalan extends GeneralValueObject {

	/** ID versi serialisasi Java untuk kompatibilitas {@link java.io.Serializable}. */
	private static final long serialVersionUID = 1L;

	/** Status awal: SPJ dibuat, belum diajukan untuk persetujuan. */
	public static final String STATUS_DRAFT = "DRAFT";
	/** SPJ sudah diajukan, menunggu persetujuan. */
	public static final String STATUS_SUBMITTED = "SUBMITTED";
	/** SPJ disetujui ({@link #getDisetujuiOleh()} terisi) -- gerbang approval sebelum berangkat;
	 *  masih boleh dibatalkan ({@link #STATUS_CANCELLED}) selama belum {@link #STATUS_ACTIVE}. */
	public static final String STATUS_APPROVED = "APPROVED";
	/** Sales sudah berangkat -- realisasi lewat satu {@link NotaSalesSession} yang dibuat saat
	 *  mulai jalan (lihat javadoc kelas). */
	public static final String STATUS_ACTIVE = "ACTIVE";
	/** Sales sudah kembali fisik ke toko, sebelum rekonsiliasi barang/uang dimulai. */
	public static final String STATUS_RETURNED = "RETURNED";
	/** Sedang direkonsiliasi -- transisi masuk status ini DIBLOKIR sampai seluruh
	 *  {@link SpjSalesBarang} milik SPJ ini habis teralokasi (invariant ERD &sect;3.3, ditegakkan
	 *  {@code SalesInventoryTripHelper.ubahStatusSesi(cekBarang=true)}). */
	public static final String STATUS_RECONCILING = "RECONCILING";
	/** Status akhir/final: sesi sudah ditutup ({@code SalesInventoryTripHelper.tripClose},
	 *  WAJIB Pemilik/Admin) -- barang dan nota ikut berpindah ke status {@code RECONCILED}. */
	public static final String STATUS_CLOSED = "CLOSED";
	/** SPJ dibatalkan -- hanya dari {@code DRAFT}/{@code SUBMITTED} (bebas) atau dari
	 *  {@code APPROVED} (wajib beralasan, hanya sebelum berangkat); lihat javadoc kelas. */
	public static final String STATUS_CANCELLED = "CANCELLED";

	/** ID baris (primary key), diisi otomatis DB; lihat {@link #getId()}. */
	private Long id;
	/** Nomor dokumen teks, diisi pasca-insert; lihat {@link #getNomor()}. */
	private String nomor;
	/** Toko yang menugaskan SPJ (menentukan skema penomoran dan cakupan); lihat {@link #getToko()}. */
	private Toko toko;
	/** Sales yang ditugaskan menjalankan SPJ ini; lihat {@link #getSales()}. */
	private SalesInventory sales;
	/** Tanggal rencana keberangkatan; lihat {@link #getTanggalBerangkatRencana()}. */
	private Date tanggalBerangkatRencana;
	/** Tanggal aktual mulai berangkat, diisi saat trip dimulai; lihat {@link #getTanggalMulaiAktual()}. */
	private Date tanggalMulaiAktual;
	/** Tanggal aktual kembali ke toko, diisi saat sesi berpindah ke {@link #STATUS_RETURNED}; lihat
	 *  {@link #getTanggalKembaliAktual()}. */
	private Date tanggalKembaliAktual;
	/** Rute rencana kunjungan (bebas teks); lihat {@link #getRute()}. */
	private String rute;
	/** Kendaraan yang dipakai; lihat {@link #getKendaraan()}. */
	private String kendaraan;
	/** Kas awal operasional yang dibawa sales; lihat {@link #getUangMukaOperasional()}. */
	private BigDecimal uangMukaOperasional;
	/** Catatan bebas untuk SPJ; lihat {@link #getCatatan()}. */
	private String catatan;
	/** Status siklus hidup SPJ mengikuti state machine kelas ini; lihat {@link #getStatus()}. */
	private String status;
	/** Alasan pembatalan, wajib diisi saat membatalkan dari {@link #STATUS_APPROVED}; lihat
	 *  {@link #getAlasanBatal()}. */
	private String alasanBatal;
	/** Pengguna yang membuat SPJ; lihat {@link #getDibuatOleh()}. */
	private Tbmuser dibuatOleh;
	/** Pengguna yang menyetujui SPJ (gerbang approval {@link #STATUS_APPROVED}); lihat
	 *  {@link #getDisetujuiOleh()}. */
	private Tbmuser disetujuiOleh;
	/** Kunci idempoten create dari klien; lihat {@link #getKodeUnik()}. */
	private String kodeUnik;
	/** Nomor versi optimistic locking ({@code @Version}); lihat {@link #getVersion()}. */
	private Long version;

	/** Nama/username audit pembuat-atau-pengubah terakhir; lihat {@link #getOleh()}. */
	private String oleh;
	/** ID pengguna audit pembuat-atau-pengubah terakhir; lihat {@link #getOlehId()}. */
	private String olehId;
	/** Stempel waktu audit pembuatan/perubahan terakhir; lihat {@link #getWaktu()}. */
	private Date waktu;
	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis Hibernate sebelum baris ini
	 * di-{@code UPDATE}, mendelegasikan pencatatan stempel waktu perubahan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(GeneralValueObject)} yang
	 * memutakhirkan {@link #tanggal_dirubah}. Tidak dipanggil pada {@code INSERT} pertama --
	 * field diinisialisasi saat konstruksi lewat {@link ais.ui.util.WaktuUtil#getDate()}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor kosong wajib Hibernate/JavaBean; field diisi belakangan lewat setter atau reflection. */
	public SuratPerintahSalesJalan() {
	}

	/**
	 * Primary key baris, dipetakan {@code IDENTITY} (auto-increment DB).
	 *
	 * @return ID baris, atau {@code null} untuk object baru yang belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyimpan ID baris. Dipanggil Hibernate saat hidrasi dari DB.
	 *
	 * @param id ID baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nomor dokumen (teks) -- diisi pasca-insert dari id ({@code SPJ-{toko}-{id 6 digit}}, lihat
	 * javadoc kelas), unik tanpa MAX+1.
	 *
	 * @return nomor dokumen, {@code null} sebelum diisi pasca-insert
	 */
	@Column(name = "nomor", length = 60, unique = true)
	public String getNomor() {
		return nomor;
	}

	/**
	 * Menyimpan nomor dokumen.
	 *
	 * @param nomor nomor baru
	 */
	public void setNomor(String nomor) {
		this.nomor = nomor;
	}

	/**
	 * Toko yang menugaskan SPJ, kolom wajib ({@code nullable = false}), relasi lazy -- dipakai
	 * sebagai bagian awalan {@link #getNomor()} ({@code SPJ-{toko}-...}).
	 *
	 * @return toko penugas
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko", nullable = false)
	public Toko getToko() {
		toko = check(toko);
		return toko;
	}

	/**
	 * Menyimpan toko penugas.
	 *
	 * @param toko toko baru
	 */
	public void setToko(Toko toko) {
		this.toko = toko;
	}

	/**
	 * Sales yang ditugaskan menjalankan SPJ ini, kolom wajib ({@code nullable = false}), relasi
	 * lazy.
	 *
	 * @return sales yang ditugaskan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sales", nullable = false)
	public SalesInventory getSales() {
		sales = check(sales);
		return sales;
	}

	/**
	 * Menyimpan sales yang ditugaskan.
	 *
	 * @param sales sales baru
	 */
	public void setSales(SalesInventory sales) {
		this.sales = sales;
	}

	/**
	 * Tanggal rencana keberangkatan, kolom wajib ({@code nullable = false}).
	 *
	 * @return tanggal rencana berangkat; default waktu-sekarang bila field mentah {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_berangkat_rencana", nullable = false)
	public Date getTanggalBerangkatRencana() {
		return tanggalBerangkatRencana == null ? ais.ui.util.WaktuUtil.getDate() : tanggalBerangkatRencana;
	}

	/**
	 * Menyimpan tanggal rencana keberangkatan.
	 *
	 * @param tanggalBerangkatRencana tanggal baru
	 */
	public void setTanggalBerangkatRencana(Date tanggalBerangkatRencana) {
		this.tanggalBerangkatRencana = tanggalBerangkatRencana;
	}

	/**
	 * Tanggal aktual mulai berangkat, diisi saat trip benar-benar dimulai (SPJ berpindah ke
	 * {@link #STATUS_ACTIVE}).
	 *
	 * @return tanggal mulai aktual, {@code null} sebelum trip dimulai
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_mulai_aktual")
	public Date getTanggalMulaiAktual() {
		return tanggalMulaiAktual;
	}

	/**
	 * Menyimpan tanggal mulai aktual.
	 *
	 * @param tanggalMulaiAktual tanggal baru
	 */
	public void setTanggalMulaiAktual(Date tanggalMulaiAktual) {
		this.tanggalMulaiAktual = tanggalMulaiAktual;
	}

	/**
	 * Tanggal aktual kembali ke toko, diisi saat sesi berpindah ke {@link #STATUS_RETURNED}
	 * ({@code SalesInventoryTripHelper.ubahStatusSesi}).
	 *
	 * @return tanggal kembali aktual, {@code null} sebelum sales kembali
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_kembali_aktual")
	public Date getTanggalKembaliAktual() {
		return tanggalKembaliAktual;
	}

	/**
	 * Menyimpan tanggal kembali aktual.
	 *
	 * @param tanggalKembaliAktual tanggal baru
	 */
	public void setTanggalKembaliAktual(Date tanggalKembaliAktual) {
		this.tanggalKembaliAktual = tanggalKembaliAktual;
	}

	/**
	 * Rute rencana kunjungan, bebas teks -- murni deskriptif, tidak divalidasi/dipakai kalkulasi
	 * apa pun oleh entity ini.
	 *
	 * @return rute, bisa {@code null}
	 */
	@Column(name = "rute", columnDefinition = "text")
	public String getRute() {
		return rute;
	}

	/**
	 * Menyimpan rute rencana kunjungan.
	 *
	 * @param rute rute baru
	 */
	public void setRute(String rute) {
		this.rute = rute;
	}

	/**
	 * Kendaraan yang dipakai, bebas teks.
	 *
	 * @return kendaraan, bisa {@code null}
	 */
	@Column(name = "kendaraan", length = 100)
	public String getKendaraan() {
		return kendaraan;
	}

	/**
	 * Menyimpan kendaraan.
	 *
	 * @param kendaraan kendaraan baru
	 */
	public void setKendaraan(String kendaraan) {
		this.kendaraan = kendaraan;
	}

	/** Kas awal operasional (OPENING_ADVANCE ledger kas sesi saat mulai jalan).
	 *
	 * @return kas awal operasional, tidak pernah {@code null} (nol bila belum diisi)
	 */
	@Column(name = "uang_muka_operasional", precision = 19, scale = 2)
	public BigDecimal getUangMukaOperasional() {
		return uangMukaOperasional == null ? BigDecimal.ZERO : uangMukaOperasional;
	}

	/**
	 * Menyimpan kas awal operasional.
	 *
	 * @param uangMukaOperasional nilai baru
	 */
	public void setUangMukaOperasional(BigDecimal uangMukaOperasional) {
		this.uangMukaOperasional = uangMukaOperasional;
	}

	/**
	 * Catatan bebas untuk SPJ.
	 *
	 * @return catatan, bisa {@code null}
	 */
	@Column(name = "catatan", columnDefinition = "text")
	public String getCatatan() {
		return catatan;
	}

	/**
	 * Menyimpan catatan.
	 *
	 * @param catatan catatan baru
	 */
	public void setCatatan(String catatan) {
		this.catatan = catatan;
	}

	/**
	 * Status siklus hidup SPJ mengikuti state machine {@code DRAFT -> SUBMITTED -> APPROVED ->
	 * ACTIVE -> RETURNED -> RECONCILING -> CLOSED} (plus jalur {@code CANCELLED}, lihat javadoc
	 * kelas dan konstanta {@code STATUS_*}). Getter memakai default lazy {@link #STATUS_DRAFT}
	 * bila field mentah {@code null}/kosong.
	 *
	 * @return status saat ini, tidak pernah {@code null}/kosong
	 */
	@Column(name = "status", length = 30)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_DRAFT : status;
	}

	/**
	 * Menyimpan status SPJ.
	 *
	 * @param status status baru
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Alasan pembatalan, wajib diisi ({@code SalesInventoryTripHelper} menolak permintaan tanpa
	 * alasan) saat membatalkan SPJ yang sudah {@link #STATUS_APPROVED}; opsional saat membatalkan
	 * dari {@link #STATUS_DRAFT}/{@link #STATUS_SUBMITTED}.
	 *
	 * @return alasan batal, bisa {@code null}
	 */
	@Column(name = "alasan_batal", columnDefinition = "text")
	public String getAlasanBatal() {
		return alasanBatal;
	}

	/**
	 * Menyimpan alasan pembatalan.
	 *
	 * @param alasanBatal alasan baru
	 */
	public void setAlasanBatal(String alasanBatal) {
		this.alasanBatal = alasanBatal;
	}

	/**
	 * Pengguna yang membuat SPJ, relasi lazy, opsional.
	 *
	 * @return pengguna pembuat, bisa {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh")
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);
		return dibuatOleh;
	}

	/**
	 * Menyimpan pengguna pembuat.
	 *
	 * @param dibuatOleh pengguna baru
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Pengguna yang menyetujui SPJ, relasi lazy -- diisi otomatis oleh
	 * {@code SalesInventoryTripHelper} (bukan input manual pemanggil) tepat saat transisi
	 * {@link #STATUS_SUBMITTED} &rarr; {@link #STATUS_APPROVED} berhasil, dan hanya bisa
	 * dipicu pengguna yang lolos pemeriksaan Pemilik/Admin ({@code pemilikAtauAdmin(ctx)}) --
	 * gerbang approval eksplisit sebelum SPJ boleh berangkat.
	 *
	 * @return pengguna penyetuju, {@code null} sebelum disetujui
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh")
	public Tbmuser getDisetujuiOleh() {
		disetujuiOleh = check(disetujuiOleh);
		return disetujuiOleh;
	}

	/**
	 * Menyimpan pengguna penyetuju.
	 *
	 * @param disetujuiOleh pengguna baru
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Kunci idempoten create dari klien (UUID) -- retry ganda mengembalikan SPJ pertama.
	 *
	 * @return kode unik idempoten, bisa {@code null}
	 */
	@Column(name = "kode_unik", length = 80, unique = true)
	public String getKodeUnik() {
		return kodeUnik;
	}

	/**
	 * Menyimpan kode unik idempoten.
	 *
	 * @param kodeUnik kode baru
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Nomor versi optimistic locking Hibernate ({@code @Version}) -- dinaikkan otomatis tiap
	 * {@code UPDATE}, dipakai mencegah lost-update pada edit konkuren SPJ yang sama.
	 *
	 * @return nomor versi saat ini
	 */
	@Version
	@Column(name = "version")
	public Long getVersion() {
		return version;
	}

	/**
	 * Menyimpan nomor versi. Jarang dipanggil manual -- biasanya dikelola Hibernate.
	 *
	 * @param version versi baru
	 */
	public void setVersion(Long version) {
		this.version = version;
	}

	/**
	 * Mengembalikan nama/username pengguna yang terakhir membuat/mengubah baris SPJ ini.
	 *
	 * @return nama/username audit terakhir, bisa {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menyimpan nama/username audit. Guard di awal method membuat setter ini <b>diam-diam
	 * menolak</b> nilai {@code null} atau string kosong/whitespace, mempertahankan
	 * {@link #oleh} lama alih-alih mengosongkannya -- pola sama dipakai
	 * {@link #setOlehId(String)} dan di banyak entity {@code GeneralValueObject} lain di
	 * codebase ini.
	 *
	 * @param oleh nama/username baru; diabaikan bila kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan ID pengguna yang terakhir membuat/mengubah baris SPJ ini.
	 *
	 * @return ID pengguna audit terakhir, bisa {@code null} bila belum pernah diisi
	 */
	@Column(name = "oleh_id")
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan ID pengguna audit. Guard di awal method membuat setter ini <b>diam-diam
	 * menolak</b> nilai {@code null} atau string kosong/whitespace -- sama seperti
	 * {@link #setOleh(String)}.
	 *
	 * @param olehId ID pengguna baru; diabaikan bila kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Stempel waktu audit pembuatan/perubahan terakhir baris SPJ ini.
	 *
	 * @return stempel waktu audit; default waktu-sekarang bila field mentah {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu")
	public Date getWaktu() {
		return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
	}

	/**
	 * Menyimpan stempel waktu audit.
	 *
	 * @param waktu stempel waktu baru
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Mengembalikan stempel waktu terakhir baris ini dirubah, dipetakan sebagai kolom
	 * {@code @Temporal(TIMESTAMP)}. Diinisialisasi ke waktu-sekarang saat object dikonstruksi dan
	 * dimutakhirkan otomatis oleh {@link #onUpdate()} setiap kali baris di-{@code UPDATE}.
	 *
	 * @return stempel waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Menyimpan stempel waktu terakhir baris ini dirubah.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
