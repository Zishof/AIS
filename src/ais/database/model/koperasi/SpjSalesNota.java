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
 * Nota/invoice piutang dibawa per SPJ (layar legacy 39-42, ERD &sect;3.4). Satu invoice
 * TIDAK boleh aktif dibawa dua SPJ berbeda (ditegakkan helper saat assign: cek baris lain
 * ber-status belum-final utk piutang_doc yang sama). Status hasil kunjungan mengikuti state
 * machine ERD: CARRIED -> UNPAID | PROMISE_TO_PAY | PARTIAL_COLLECTED | PAID | RETURNED |
 * DISPUTED | LOST -> RECONCILED. {@code nilaiTertagih} diakumulasi dari collection ber-sesi.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "spj_sales_nota")
public class SpjSalesNota extends GeneralValueObject {

	/** ID versi serialisasi Java untuk kompatibilitas {@link java.io.Serializable}. */
	private static final long serialVersionUID = 1L;

	/** Status awal: nota di-assign ke SPJ tapi trip belum berangkat (belum {@code CARRIED}). */
	public static final String STATUS_ASSIGNED = "ASSIGNED";
	/** Nota resmi dibawa sales -- SPJ sudah {@code ACTIVE}/berangkat; state transit sebelum
	 *  hasil kunjungan (UNPAID/PROMISE/dst.) dicatat. */
	public static final String STATUS_CARRIED = "CARRIED";
	/** Hasil kunjungan: customer tidak membayar sama sekali saat dikunjungi. */
	public static final String STATUS_UNPAID = "UNPAID";
	/** Hasil kunjungan: customer berjanji membayar kemudian; lihat {@link #getJanjiBayar()}. */
	public static final String STATUS_PROMISE = "PROMISE_TO_PAY";
	/** Hasil kunjungan: customer membayar sebagian; lihat {@link #getNilaiTertagih()}. */
	public static final String STATUS_PARTIAL = "PARTIAL_COLLECTED";
	/** Hasil kunjungan: nota lunas dibayar penuh saat kunjungan. */
	public static final String STATUS_PAID = "PAID";
	/** Hasil kunjungan: nota dikembalikan (barang/klaim retur terkait faktur ini). */
	public static final String STATUS_RETURNED = "RETURNED";
	/** Hasil kunjungan: customer mendisputkan tagihan (nilai/keabsahan dipersoalkan). */
	public static final String STATUS_DISPUTED = "DISPUTED";
	/** Hasil kunjungan: nota dinyatakan tidak tertagih/hilang. */
	public static final String STATUS_LOST = "LOST";
	/** Status akhir/final: baris sudah direkonsiliasi tuntas saat sesi ditutup -- tidak berubah
	 *  lagi setelahnya. */
	public static final String STATUS_RECONCILED = "RECONCILED";

	/** ID baris (primary key), diisi otomatis DB; lihat {@link #getId()}. */
	private Long id;
	/** SPJ induk yang membawa nota ini; lihat {@link #getSpj()}. */
	private SuratPerintahSalesJalan spj;
	/** Dokumen piutang/invoice yang ditagihkan; lihat {@link #getPiutangDoc()}. */
	private PiutangCustomerDoc piutangDoc;
	/** Customer pemilik piutang; lihat {@link #getCustomer()}. */
	private AnggotaKoperasi customer;
	/** Nilai awal faktur (nominal penuh saat diterbitkan); lihat {@link #getNilaiAwal()}. */
	private BigDecimal nilaiAwal;
	/** Snapshot outstanding faktur saat nota di-assign ke SPJ; lihat {@link #getSaldoSaatAssign()}. */
	private BigDecimal saldoSaatAssign;
	/** Tanggal jatuh tempo faktur; lihat {@link #getJatuhTempo()}. */
	private Date jatuhTempo;
	/** Status siklus hidup baris mengikuti state machine kelas ini; lihat {@link #getStatus()}. */
	private String status;
	/** Catatan bebas hasil kunjungan sales ke customer; lihat {@link #getHasilKunjungan()}. */
	private String hasilKunjungan;
	/** Tanggal janji bayar bila status {@link #STATUS_PROMISE}; lihat {@link #getJanjiBayar()}. */
	private Date janjiBayar;
	/** Alasan gagal tertagih (dispute/hilang/dsb.); lihat {@link #getAlasanGagal()}. */
	private String alasanGagal;
	/** Akumulasi nilai yang berhasil ditagih dari faktur ini selama trip; lihat
	 *  {@link #getNilaiTertagih()}. */
	private BigDecimal nilaiTertagih;

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis Hibernate sebelum baris ini
	 * di-{@code UPDATE}, mendelegasikan pencatatan stempel waktu perubahan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(GeneralValueObject)} yang
	 * memutakhirkan {@link #tanggal_dirubah}. Tidak dipanggil pada {@code INSERT} pertama --
	 * field diinisialisasi saat konstruksi lewat {@link ais.ui.util.WaktuUtil#getDate()}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor kosong wajib Hibernate/JavaBean; field diisi belakangan lewat setter atau reflection. */
	public SpjSalesNota() {
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
	 * SPJ (keberangkatan sales) induk yang membawa nota ini, kolom wajib
	 * ({@code nullable = false}), relasi lazy.
	 *
	 * @return SPJ induk
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "spj", nullable = false)
	public SuratPerintahSalesJalan getSpj() {
		spj = check(spj);
		return spj;
	}

	/**
	 * Menyimpan SPJ induk.
	 *
	 * @param spj SPJ baru
	 */
	public void setSpj(SuratPerintahSalesJalan spj) {
		this.spj = spj;
	}

	/**
	 * Dokumen piutang/invoice ({@link PiutangCustomerDoc}) yang ditagihkan lewat kunjungan ini,
	 * kolom wajib ({@code nullable = false}), relasi lazy. Saat baris ini dibuat (assignment nota
	 * ke SPJ, {@code SalesInventoryTripHelper}), helper menolak faktur yang statusnya bukan
	 * {@code AKTIF} dan menolak faktur yang sedang dibawa SPJ lain yang belum {@code CLOSED}/
	 * {@code CANCELLED} (query {@code JOIN spj_sales_nota ... WHERE piutang_doc = ? AND spj <> ?
	 * AND status NOT IN ('CLOSED','CANCELLED')}) -- menegakkan invariant "satu invoice tidak
	 * boleh aktif dibawa dua SPJ berbeda" yang disebut di javadoc kelas.
	 *
	 * @return dokumen piutang yang ditagihkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "piutang_doc", nullable = false)
	public PiutangCustomerDoc getPiutangDoc() {
		piutangDoc = check(piutangDoc);
		return piutangDoc;
	}

	/**
	 * Menyimpan dokumen piutang yang ditagihkan.
	 *
	 * @param piutangDoc dokumen piutang baru
	 */
	public void setPiutangDoc(PiutangCustomerDoc piutangDoc) {
		this.piutangDoc = piutangDoc;
	}

	/**
	 * Customer pemilik piutang, kolom wajib ({@code nullable = false}), relasi lazy -- disalin
	 * dari {@code piutangDoc.getCustomer()} saat baris dibuat (denormalisasi utk kueri tanpa
	 * join balik ke dokumen piutang).
	 *
	 * @return customer pemilik piutang
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "customer", nullable = false)
	public AnggotaKoperasi getCustomer() {
		customer = check(customer);
		return customer;
	}

	/**
	 * Menyimpan customer pemilik piutang.
	 *
	 * @param customer customer baru
	 */
	public void setCustomer(AnggotaKoperasi customer) {
		this.customer = customer;
	}

	/**
	 * Nilai awal faktur (nominal penuh saat diterbitkan, sebelum pembayaran/alokasi apa pun) --
	 * disalin dari {@code piutangDoc.getTotalFaktur()} saat baris dibuat.
	 *
	 * @return nilai awal faktur, tidak pernah {@code null} (nol bila belum diisi)
	 */
	@Column(name = "nilai_awal", precision = 19, scale = 2)
	public BigDecimal getNilaiAwal() {
		return nilaiAwal == null ? BigDecimal.ZERO : nilaiAwal;
	}

	/**
	 * Menyimpan nilai awal faktur.
	 *
	 * @param nilaiAwal nilai awal baru
	 */
	public void setNilaiAwal(BigDecimal nilaiAwal) {
		this.nilaiAwal = nilaiAwal;
	}

	/**
	 * Outstanding faktur SAAT di-assign (snapshot -- bukti berapa yang dibawa sales).
	 *
	 * @return saldo saat assign, tidak pernah {@code null} (nol bila belum diisi)
	 */
	@Column(name = "saldo_saat_assign", precision = 19, scale = 2)
	public BigDecimal getSaldoSaatAssign() {
		return saldoSaatAssign == null ? BigDecimal.ZERO : saldoSaatAssign;
	}

	/**
	 * Menyimpan saldo saat assign.
	 *
	 * @param saldoSaatAssign saldo baru
	 */
	public void setSaldoSaatAssign(BigDecimal saldoSaatAssign) {
		this.saldoSaatAssign = saldoSaatAssign;
	}

	/**
	 * Tanggal jatuh tempo faktur, disalin dari dokumen piutang saat baris dibuat.
	 *
	 * @return tanggal jatuh tempo, bisa {@code null}
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "jatuh_tempo")
	public Date getJatuhTempo() {
		return jatuhTempo;
	}

	/**
	 * Menyimpan tanggal jatuh tempo.
	 *
	 * @param jatuhTempo tanggal baru
	 */
	public void setJatuhTempo(Date jatuhTempo) {
		this.jatuhTempo = jatuhTempo;
	}

	/**
	 * Status siklus hidup baris, mengikuti state machine {@code ASSIGNED -> CARRIED ->
	 * (UNPAID|PROMISE_TO_PAY|PARTIAL_COLLECTED|PAID|RETURNED|DISPUTED|LOST) -> RECONCILED}
	 * (lihat javadoc kelas). Getter memakai default lazy {@link #STATUS_ASSIGNED} bila field
	 * mentah {@code null}/kosong.
	 *
	 * @return status saat ini, tidak pernah {@code null}/kosong
	 */
	@Column(name = "status", length = 30)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_ASSIGNED : status;
	}

	/**
	 * Menyimpan status baris.
	 *
	 * @param status status baru
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Catatan bebas hasil kunjungan sales ke customer (mis. alasan tidak bayar, kondisi di
	 * lapangan) -- murni naratif, tidak divalidasi/diverifikasi otomatis oleh entity ini.
	 *
	 * @return catatan hasil kunjungan, bisa {@code null}
	 */
	@Column(name = "hasil_kunjungan", columnDefinition = "text")
	public String getHasilKunjungan() {
		return hasilKunjungan;
	}

	/**
	 * Menyimpan catatan hasil kunjungan.
	 *
	 * @param hasilKunjungan catatan baru
	 */
	public void setHasilKunjungan(String hasilKunjungan) {
		this.hasilKunjungan = hasilKunjungan;
	}

	/**
	 * Tanggal janji bayar yang disepakati customer, relevan saat status
	 * {@link #STATUS_PROMISE}.
	 *
	 * @return tanggal janji bayar, bisa {@code null}
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "janji_bayar")
	public Date getJanjiBayar() {
		return janjiBayar;
	}

	/**
	 * Menyimpan tanggal janji bayar.
	 *
	 * @param janjiBayar tanggal baru
	 */
	public void setJanjiBayar(Date janjiBayar) {
		this.janjiBayar = janjiBayar;
	}

	/**
	 * Alasan gagal tertagih (mis. dispute/hilang/retur) -- murni naratif, tidak
	 * divalidasi/diverifikasi otomatis oleh entity ini.
	 *
	 * @return alasan gagal, bisa {@code null}
	 */
	@Column(name = "alasan_gagal", columnDefinition = "text")
	public String getAlasanGagal() {
		return alasanGagal;
	}

	/**
	 * Menyimpan alasan gagal tertagih.
	 *
	 * @param alasanGagal alasan baru
	 */
	public void setAlasanGagal(String alasanGagal) {
		this.alasanGagal = alasanGagal;
	}

	/**
	 * Akumulasi nilai yang berhasil ditagih dari faktur ini selama trip. Pada jalur legacy
	 * (non-tenant), field ini adalah <b>shadow field yang diakumulasi manual</b>: tiap kali
	 * penerimaan piutang dialokasikan ke faktur ({@code SalesInventoryReceivableHelper}), nilainya
	 * di-{@code add()} langsung ({@code notaBawa.setNilaiTertagih(getNilaiTertagih().add(nominal))})
	 * -- BUKAN dihitung ulang dari sumber. Sebaliknya, pembatalan alokasi
	 * ({@code SalesInventoryReversalHelper}) harus mengurangi field ini secara eksplisit lalu
	 * menjepitnya ke nol ({@code sisaTagih.signum() < 0 ? ZERO : sisaTagih}) supaya tidak negatif
	 * -- keharusan teknis menjaga shadow field tetap konsisten dengan ledger alokasi yang
	 * sebenarnya, bukan bug. Pada jalur schema tenant, kolom setara TIDAK disimpan sama sekali --
	 * selalu dihitung ulang (derive) langsung dari alokasi penerimaan saat dibaca
	 * ({@code SalesInventoryTripTenant.nilaiTertagihNota}), sehingga tidak butuh penjagaan
	 * akumulasi/rollback manual seperti jalur legacy.
	 *
	 * @return nilai tertagih, tidak pernah {@code null} (nol bila belum diisi)
	 */
	@Column(name = "nilai_tertagih", precision = 19, scale = 2)
	public BigDecimal getNilaiTertagih() {
		return nilaiTertagih == null ? BigDecimal.ZERO : nilaiTertagih;
	}

	/**
	 * Menyimpan nilai tertagih.
	 *
	 * @param nilaiTertagih nilai baru
	 */
	public void setNilaiTertagih(BigDecimal nilaiTertagih) {
		this.nilaiTertagih = nilaiTertagih;
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
