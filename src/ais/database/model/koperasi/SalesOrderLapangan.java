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
 * Sales Order lapangan (varian "eBisnis Inventory &amp; Sales", layar legacy 30 "Menu
 * Penjualan" -- ERD &sect;3.6). Order BUKAN invoice: piutang baru lahir saat order
 * di-posting jadi {@link PiutangCustomerDoc} (aksi {@code si_sales_order_invoice},
 * status {@link #STATUS_SIAP_TAGIH}) -- sesuai mapping layar 30 "jangan samakan order
 * dengan invoice".
 *
 * <p>Status "Mode Sales Lapangan" persis permintaan mapping: {@link #STATUS_PESAN} &rarr;
 * {@link #STATUS_SIAP_KIRIM} &rarr; {@link #STATUS_TERKIRIM} &rarr; {@link #STATUS_SIAP_TAGIH};
 * ditambah {@link #STATUS_DRAFT} (belum dikonfirmasi), {@link #STATUS_LUNAS} (turunan dari
 * pelunasan piutang), {@link #STATUS_BATAL} (soft-cancel, dokumen tidak pernah dihapus fisik).
 * Transisi divalidasi server ({@code SalesInventoryReceivableHelper.salesOrderStatus}).</p>
 *
 * <p>KEPUTUSAN P4 (D-13, dicatat di docs/pos-inventory-sales/02-decisions.md): TERKIRIM
 * TIDAK menggerakkan stok di fase ini -- movement fisik barang sales dicatat lewat SPJ
 * "barang dibawa" (P5, TRIP-002) supaya tidak dobel-hitung saat kedua fitur digabung.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "sales_order_lapangan")
public class SalesOrderLapangan extends GeneralValueObject {

	/** ID versi serialisasi Java untuk kompatibilitas {@link java.io.Serializable}. */
	private static final long serialVersionUID = 1L;

	/** Status awal: order dibuat tapi belum dikonfirmasi customer/sales. */
	public static final String STATUS_DRAFT = "DRAFT";
	/** Order sudah dikonfirmasi (dipesan) -- awal state machine "Mode Sales Lapangan". */
	public static final String STATUS_PESAN = "PESAN";
	/** Order siap dikirim (barang sudah disiapkan/dipilih, menunggu keberangkatan). */
	public static final String STATUS_SIAP_KIRIM = "SIAP_KIRIM";
	/** Barang sudah terkirim ke customer -- lihat KEPUTUSAN P4 di javadoc kelas: transisi ini
	 *  TIDAK menggerakkan stok pada fase ini. */
	public static final String STATUS_TERKIRIM = "TERKIRIM";
	/** Order siap ditagih -- dicapai hanya lewat posting {@code si_sales_order_invoice} yang
	 *  menerbitkan {@link PiutangCustomerDoc}, bukan transisi status manual biasa. */
	public static final String STATUS_SIAP_TAGIH = "SIAP_TAGIH";
	/** Turunan: piutang hasil posting order ini sudah lunas dibayar penuh. */
	public static final String STATUS_LUNAS = "LUNAS";
	/** Soft-cancel -- dokumen tidak pernah dihapus fisik, hanya ditandai batal. */
	public static final String STATUS_BATAL = "BATAL";

	/** ID baris (primary key), diisi otomatis DB; lihat {@link #getId()}. */
	private Long id;
	/** Nomor dokumen teks, diisi pasca-insert; lihat {@link #getNomor()}. */
	private String nomor;
	/** Toko yang memproses order (menentukan skema penomoran dan cakupan); lihat {@link #getToko()}. */
	private Toko toko;
	/** Customer pemesan; lihat {@link #getCustomer()}. */
	private AnggotaKoperasi customer;
	/** Sales lapangan yang menangani order (opsional); lihat {@link #getSales()}. */
	private SalesInventory sales;
	/** Tanggal bisnis order; lihat {@link #getTanggal()}. */
	private Date tanggal;
	/** Status siklus hidup order mengikuti state machine kelas ini; lihat {@link #getStatus()}. */
	private String status;
	/** Total order, denormal dari jumlah subtotal item; lihat {@link #getTotal()}. */
	private BigDecimal total;
	/** Catatan bebas untuk order; lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Alasan pembatalan, wajib diisi UI saat order dibatalkan; lihat {@link #getAlasanBatal()}. */
	private String alasanBatal;
	/** Kunci idempoten create dari klien; lihat {@link #getKodeUnik()}. */
	private String kodeUnik;
	/** Pengguna yang membuat order; lihat {@link #getDibuatOleh()}. */
	private Tbmuser dibuatOleh;
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
	public SalesOrderLapangan() {
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

	/** Nomor dokumen (teks) -- diisi SETELAH insert dari id ({@code SO-{toko}-{id 6 digit}}):
	 *  unik tanpa MAX+1 dan tanpa sequence tambahan (larangan ERD &sect;1.6).
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
	 * Toko yang memproses order, kolom wajib ({@code nullable = false}), relasi lazy -- dipakai
	 * sebagai bagian awalan {@link #getNomor()} ({@code SO-{toko}-...}).
	 *
	 * @return toko pemroses
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko", nullable = false)
	public Toko getToko() {
		return toko;
	}

	/**
	 * Menyimpan toko pemroses.
	 *
	 * @param toko toko baru
	 */
	public void setToko(Toko toko) {
		this.toko = toko;
	}

	/**
	 * Customer pemesan, kolom wajib ({@code nullable = false}), relasi lazy.
	 *
	 * @return customer pemesan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "customer", nullable = false)
	public AnggotaKoperasi getCustomer() {
		return customer;
	}

	/**
	 * Menyimpan customer pemesan.
	 *
	 * @param customer customer baru
	 */
	public void setCustomer(AnggotaKoperasi customer) {
		this.customer = customer;
	}

	/**
	 * Sales lapangan yang menangani order, kolom opsional (tanpa {@code nullable = false}),
	 * relasi lazy -- order bisa dibuat tanpa sales tertentu (mis. dipesan langsung ke toko).
	 *
	 * @return sales penangan, bisa {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sales")
	public SalesInventory getSales() {
		return sales;
	}

	/**
	 * Menyimpan sales penangan.
	 *
	 * @param sales sales baru
	 */
	public void setSales(SalesInventory sales) {
		this.sales = sales;
	}

	/** Tanggal bisnis order (bukan timestamp teknis -- itu {@link #getWaktu()}).
	 *
	 * @return tanggal order; default waktu-sekarang bila field mentah {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal", nullable = false)
	public Date getTanggal() {
		return tanggal == null ? ais.ui.util.WaktuUtil.getDate() : tanggal;
	}

	/**
	 * Menyimpan tanggal bisnis order.
	 *
	 * @param tanggal tanggal baru
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Status siklus hidup order mengikuti state machine "Mode Sales Lapangan" (lihat javadoc
	 * kelas). Getter memakai default lazy {@link #STATUS_DRAFT} bila field mentah
	 * {@code null}/kosong.
	 *
	 * @return status saat ini, tidak pernah {@code null}/kosong
	 */
	@Column(name = "status", length = 30)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_DRAFT : status;
	}

	/**
	 * Menyimpan status order.
	 *
	 * @param status status baru
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/** Total order (denormal dari &Sigma; subtotal item, di-recompute tiap simpan item --
	 *  untuk list/aging tanpa join; sumber kebenaran tetap item).
	 *
	 * @return total order, tidak pernah {@code null} (nol bila belum diisi)
	 */
	@Column(name = "total", precision = 19, scale = 2)
	public BigDecimal getTotal() {
		return total == null ? BigDecimal.ZERO : total;
	}

	/**
	 * Menyimpan total order.
	 *
	 * @param total total baru
	 */
	public void setTotal(BigDecimal total) {
		this.total = total;
	}

	/**
	 * Catatan bebas untuk order.
	 *
	 * @return keterangan, bisa {@code null}
	 */
	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyimpan keterangan order.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Alasan pembatalan order, diisi saat status berpindah ke {@link #STATUS_BATAL}.
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

	/** Kunci idempoten create dari klien (UUID) -- retry ganda mengembalikan order pertama.
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
	 * Pengguna yang membuat order, relasi lazy, opsional.
	 *
	 * @return pengguna pembuat, bisa {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh")
	public Tbmuser getDibuatOleh() {
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
	 * Nomor versi optimistic locking Hibernate ({@code @Version}) -- dinaikkan otomatis tiap
	 * {@code UPDATE}, dipakai mencegah lost-update pada edit konkuren order yang sama.
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
	 * Mengembalikan nama/username pengguna yang terakhir membuat/mengubah baris order ini.
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
	 * Mengembalikan ID pengguna yang terakhir membuat/mengubah baris order ini.
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
	 * Stempel waktu audit pembuatan/perubahan terakhir baris order ini.
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
