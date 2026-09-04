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
import ais.database.model.inventory.Toko;

/**
 * Dokumen piutang customer / invoice AR (layar legacy 31-33, varian Inventory &amp; Sales) --
 * cermin AP {@link PayableFakturInfo} di sisi penjualan. Lahir dari posting
 * {@link SalesOrderLapangan} ({@code si_sales_order_invoice}) atau entri manual pemilik
 * (faktur lama/migrasi legacy TRAN_PIU.DBF menyusul).
 *
 * <p>OUTSTANDING TIDAK DISIMPAN -- selalu dihitung: {@code totalFaktur - dibayarAwal -
 * SUM(alokasi penerimaan)} (register event, pola sama persis ledger AP P3 -- pelunasan tidak
 * pernah menghapus/menimpa dokumen; filter "lunas" murni visual, layar 33).</p>
 *
 * <p>CATATAN sub-ledger (D-12): piutang POS existing (belanja kasir ber-cara-bayar
 * "masuk sebagai hutang" &minus; pembayaran_hutang) adalah ledger TERPISAH yang sudah punya
 * layar Mutasi Hutang sendiri; saldo customer gabungan = ledger POS + outstanding dokumen ini
 * (keduanya ditampilkan terpisah, tidak dicampur -- tanpa duplikasi pencatatan).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "piutang_customer_doc")
public class PiutangCustomerDoc extends GeneralValueObject {

	/** Versi serialisasi tetap; dipertahankan hanya krn kontrak {@link GeneralValueObject}/
	 * {@code Serializable}, entity ini tidak dikirim lewat Java serialization jarak jauh. */
	private static final long serialVersionUID = 1L;

	/** Nilai default/normal {@link #status} -- faktur berlaku, ikut dihitung outstanding &amp;
	 * aging. Dikembalikan getter bila kolom NULL/kosong (lihat {@link #getStatus()}). */
	public static final String STATUS_AKTIF = "AKTIF";
	/** Faktur dibatalkan -- dokumen TIDAK dihapus (lihat catatan register event di Javadoc
	 * kelas), hanya ditandai lewat {@link #setStatus(String)} disertai {@link #setAlasanBatal
	 * (String)}. */
	public static final String STATUS_BATAL = "BATAL";

	/** PK auto-generated (identity). Lihat {@link #getId()}. */
	private Long id;
	/** Nomor faktur (teks), diisi pasca-insert dari {@link #id}. Lihat {@link #getNomor()}. */
	private String nomor;
	/** Toko/unit penerbit faktur ini. Wajib diisi. Lihat {@link #getToko()}. */
	private Toko toko;
	/** Anggota koperasi (pelanggan) yang berutang lewat faktur ini. Wajib diisi. Lihat
	 * {@link #getCustomer()}. */
	private AnggotaKoperasi customer;
	/** Sales inventory yang menerbitkan/bertanggung jawab atas faktur ini (nullable). Lihat
	 * {@link #getSales()}. */
	private SalesInventory sales;
	/** Order lapangan asal faktur ini (nullable -- dokumen manual/migrasi tidak punya order).
	 * Lihat {@link #getSalesOrder()}. */
	private SalesOrderLapangan salesOrder;
	/** Tanggal terbit faktur. Lihat {@link #getTanggal()}. */
	private Date tanggal;
	/** Jangka waktu kredit dalam hari, dihitung dari {@link #tanggal}. Lihat
	 * {@link #getTerminHari()}. */
	private Integer terminHari;
	/** Tanggal jatuh tempo pembayaran ({@link #tanggal} + {@link #terminHari}), disimpan sbg
	 * kolom sendiri. Lihat {@link #getJatuhTempo()}. */
	private Date jatuhTempo;
	/** Total nilai faktur (nilai piutang kotor sebelum dikurangi pembayaran mana pun). Lihat
	 * {@link #getTotalFaktur()}. */
	private BigDecimal totalFaktur;
	/** Bagian yang sudah dibayar SAAT faktur diterbitkan (uang muka/tunai sebagian), bukan hasil
	 * penagihan belakangan. Lihat {@link #getDibayarAwal()}. */
	private BigDecimal dibayarAwal;
	/** Status dokumen: {@link #STATUS_AKTIF} (default) atau {@link #STATUS_BATAL}. Lihat
	 * {@link #getStatus()}. */
	private String status;
	/** Catatan bebas ttg faktur ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Alasan pembatalan, diisi hanya saat {@link #status} diubah ke {@link #STATUS_BATAL}. Lihat
	 * {@link #getAlasanBatal()}. */
	private String alasanBatal;
	/** Kunci idempoten create (UUID klien / turunan order). Lihat {@link #getKodeUnik()}. */
	private String kodeUnik;

	/** Nama petugas/proses yang membuat baris ini (jejak audit tampilan, bukan FK). Lihat
	 * {@link #getOleh()}. */
	private String oleh;
	/** ID/username petugas/proses yang membuat baris ini (jejak audit, pasangan {@link #oleh}).
	 * Lihat {@link #getOlehId()}. */
	private String olehId;
	/** Waktu baris ini dicatat. Lihat {@link #getWaktu()}. */
	private Date waktu;
	/**
	 * Callback JPA {@code @PreUpdate}: menandai kapan baris faktur ini TERAKHIR diubah, dengan
	 * menuliskan waktu sekarang ke {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Dipanggil otomatis
	 * oleh Hibernate sebelum setiap {@code UPDATE} (mis. saat status diubah menjadi
	 * {@link #STATUS_BATAL}), tidak pernah dipanggil manual dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor tanpa argumen wajib JPA/Hibernate; jangan dipakai langsung dari kode aplikasi --
	 * dokumen ini dibentuk oleh proses posting {@link SalesOrderLapangan} atau helper entri manual,
	 * bukan dibangun bebas lalu di-{@code save} sembarangan (lihat invariant "outstanding tidak
	 * disimpan" pada Javadoc kelas). */
	public PiutangCustomerDoc() {
	}

	/**
	 * PK identity baris faktur ini. {@code null} sebelum entity di-{@code save}/{@code flush} ke
	 * Hibernate (ID baru dibuat DB saat insert, strategi {@link IDENTITY}); nilai ini yang lalu
	 * dipakai membentuk {@link #getNomor()}.
	 *
	 * @return id baris faktur, atau {@code null} bila belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Setter PK -- dipanggil Hibernate saat memuat entity dari DB. Kode aplikasi normal tidak
	 * perlu memanggil ini secara eksplisit.
	 *
	 * @param id id baris faktur.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/** Nomor faktur (teks) -- diisi pasca-insert dari id ({@code INV-{toko}-{id 6 digit}}),
	 *  unik tanpa MAX+1. */
	@Column(name = "nomor", length = 60, unique = true)
	public String getNomor() {
		return nomor;
	}

	/**
	 * Menetapkan nomor faktur. Normalnya diisi otomatis oleh helper posting pasca-insert (dari
	 * {@link #getId()}), bukan dientri manual -- setter ini tidak memvalidasi keunikan sendiri
	 * (mengandalkan constraint {@code unique = true} pada kolomnya di DB).
	 *
	 * @param nomor nomor faktur baru.
	 */
	public void setNomor(String nomor) {
		this.nomor = nomor;
	}

	/**
	 * Toko/unit penerbit faktur ini. {@code nullable = false}. Relasi {@code LAZY}: mengakses
	 * field pada objek yang dikembalikan di luar sesi Hibernate yang masih terbuka akan melempar
	 * {@code LazyInitializationException}.
	 *
	 * @return toko penerbit faktur.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko", nullable = false)
	public Toko getToko() {
		toko = check(toko);
		return toko;
	}

	/**
	 * Menetapkan toko penerbit faktur.
	 *
	 * @param toko toko penerbit.
	 */
	public void setToko(Toko toko) {
		this.toko = toko;
	}

	/**
	 * Anggota koperasi (pelanggan) yang berutang lewat faktur ini. {@code nullable = false}.
	 * Relasi {@code LAZY} -- sama catatan lazy-loading dgn {@link #getToko()}.
	 *
	 * @return customer pemegang piutang ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "customer", nullable = false)
	public AnggotaKoperasi getCustomer() {
		customer = check(customer);
		return customer;
	}

	/**
	 * Menetapkan customer pemegang piutang faktur ini.
	 *
	 * @param customer anggota koperasi (pelanggan) terkait.
	 */
	public void setCustomer(AnggotaKoperasi customer) {
		this.customer = customer;
	}

	/**
	 * Sales inventory penerbit/penanggung jawab faktur ini (nullable -- faktur bisa dientri
	 * tanpa sales terikat, mis. entri manual kantor).
	 *
	 * @return sales terkait, atau {@code null} bila tidak diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sales")
	public SalesInventory getSales() {
		sales = check(sales);
		return sales;
	}

	/**
	 * Menetapkan sales terkait faktur ini.
	 *
	 * @param sales sales inventory, boleh {@code null}.
	 */
	public void setSales(SalesInventory sales) {
		this.sales = sales;
	}

	/** Order asal (nullable -- dokumen manual/migrasi tidak punya order).
	 *
	 * @return order lapangan asal faktur ini, atau {@code null} bila dokumen manual/migrasi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sales_order")
	public SalesOrderLapangan getSalesOrder() {
		salesOrder = check(salesOrder);
		return salesOrder;
	}

	/**
	 * Menetapkan order lapangan asal faktur ini. Dipanggil helper posting order (lihat Javadoc
	 * kelas); tidak divalidasi ulang di sini bahwa order tsb belum pernah membuat faktur lain.
	 *
	 * @param salesOrder order lapangan asal, boleh {@code null}.
	 */
	public void setSalesOrder(SalesOrderLapangan salesOrder) {
		this.salesOrder = salesOrder;
	}

	/**
	 * Tanggal terbit faktur. Getter null-safe: mengembalikan waktu SEKARANG bila kolom belum
	 * diisi (mis. objek baru yang belum di-{@code set}), BUKAN {@code null} -- perhatikan bahwa
	 * ini berarti dua pemanggilan berturut-turut pada objek transient yang sama bisa
	 * mengembalikan nilai berbeda selama field {@link #tanggal} tetap {@code null}.
	 *
	 * @return tanggal faktur, tidak pernah {@code null}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal", nullable = false)
	public Date getTanggal() {
		return tanggal == null ? ais.ui.util.WaktuUtil.getDate() : tanggal;
	}

	/**
	 * Menetapkan tanggal terbit faktur.
	 *
	 * @param tanggal tanggal faktur baru.
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Jangka waktu kredit dalam hari. Getter null-safe: mengembalikan {@code 0} (tunai/tanpa
	 * termin) bila kolom NULL di DB.
	 *
	 * @return termin hari, tidak pernah {@code null}.
	 */
	@Column(name = "termin_hari")
	public Integer getTerminHari() {
		return terminHari == null ? Integer.valueOf(0) : terminHari;
	}

	/**
	 * Menetapkan termin hari. Tidak dipakai untuk menghitung ulang {@link #getJatuhTempo()}
	 * secara otomatis -- kolom itu disimpan terpisah dan harus disetel sendiri oleh pemanggil.
	 *
	 * @param terminHari jangka waktu kredit dalam hari.
	 */
	public void setTerminHari(Integer terminHari) {
		this.terminHari = terminHari;
	}

	/** Jatuh tempo = tanggal + termin, kolom sendiri (query aging tanpa join, bisa dikoreksi).
	 *
	 * @return tanggal jatuh tempo, atau {@code null} bila belum dihitung/diisi.
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "jatuh_tempo")
	public Date getJatuhTempo() {
		return jatuhTempo;
	}

	/**
	 * Menetapkan tanggal jatuh tempo secara langsung (mis. hasil koreksi manual). Tidak
	 * memvalidasi konsistensi terhadap {@link #getTanggal()} + {@link #getTerminHari()}.
	 *
	 * @param jatuhTempo tanggal jatuh tempo baru.
	 */
	public void setJatuhTempo(Date jatuhTempo) {
		this.jatuhTempo = jatuhTempo;
	}

	/**
	 * Total nilai faktur (piutang kotor, sebelum dikurangi {@link #getDibayarAwal()} maupun
	 * alokasi penerimaan mana pun -- lihat rumus outstanding di Javadoc kelas). Getter
	 * null-safe: mengembalikan {@link BigDecimal#ZERO} bila kolom NULL di DB.
	 *
	 * @return total faktur, tidak pernah {@code null}.
	 */
	@Column(name = "total_faktur", precision = 19, scale = 2)
	public BigDecimal getTotalFaktur() {
		return totalFaktur == null ? BigDecimal.ZERO : totalFaktur;
	}

	/**
	 * Menetapkan total faktur. Tidak divalidasi di sini (boleh negatif/nol bila dipanggil
	 * langsung) -- helper posting yang membentuk nilai ini dari total order/nota terkait.
	 *
	 * @param totalFaktur total faktur baru.
	 */
	public void setTotalFaktur(BigDecimal totalFaktur) {
		this.totalFaktur = totalFaktur;
	}

	/** Dibayar saat faktur terbit (uang muka/tunai sebagian) -- bukan hasil penagihan.
	 *
	 * @return nominal dibayar awal, tidak pernah {@code null} (default {@link BigDecimal#ZERO}).
	 */
	@Column(name = "dibayar_awal", precision = 19, scale = 2)
	public BigDecimal getDibayarAwal() {
		return dibayarAwal == null ? BigDecimal.ZERO : dibayarAwal;
	}

	/**
	 * Menetapkan nominal dibayar awal.
	 *
	 * @param dibayarAwal nominal dibayar awal baru.
	 */
	public void setDibayarAwal(BigDecimal dibayarAwal) {
		this.dibayarAwal = dibayarAwal;
	}

	/**
	 * Status dokumen. Getter null-safe: mengembalikan {@link #STATUS_AKTIF} bila kolom
	 * NULL/kosong di DB.
	 *
	 * @return status dokumen, tidak pernah {@code null}/kosong.
	 */
	@Column(name = "status", length = 20)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_AKTIF : status;
	}

	/**
	 * Menetapkan status dokumen. Tidak memvalidasi nilai terhadap {@link #STATUS_AKTIF}/
	 * {@link #STATUS_BATAL} -- pemanggil bertanggung jawab memakai konstanta yang benar.
	 *
	 * @param status status dokumen baru.
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Catatan bebas ttg faktur ini.
	 *
	 * @return keterangan, atau {@code null}/kosong bila tidak diisi.
	 */
	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menetapkan keterangan.
	 *
	 * @param keterangan catatan bebas baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Alasan pembatalan faktur, diisi saat {@link #getStatus()} diubah ke {@link #STATUS_BATAL}.
	 *
	 * @return alasan batal, atau {@code null} bila faktur belum pernah dibatalkan.
	 */
	@Column(name = "alasan_batal", columnDefinition = "text")
	public String getAlasanBatal() {
		return alasanBatal;
	}

	/**
	 * Menetapkan alasan pembatalan. Tidak dipaksa terkait dgn {@link #setStatus(String)} --
	 * bisa diisi tanpa mengubah status, tergantung disiplin pemanggil.
	 *
	 * @param alasanBatal alasan pembatalan baru.
	 */
	public void setAlasanBatal(String alasanBatal) {
		this.alasanBatal = alasanBatal;
	}

	/** Kunci idempoten create (UUID klien / turunan order) -- retry ganda aman.
	 *
	 * @return kode unik idempoten, atau {@code null} bila tidak dipakai jalur idempoten.
	 */
	@Column(name = "kode_unik", length = 80, unique = true)
	public String getKodeUnik() {
		return kodeUnik;
	}

	/**
	 * Menetapkan kode unik idempoten. Keunikannya ditegakkan constraint DB
	 * ({@code unique = true}), bukan dicek manual di setter ini.
	 *
	 * @param kodeUnik kode unik baru.
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Nama petugas/proses yang membuat baris faktur ini (jejak audit tampilan, bukan FK).
	 *
	 * @return nama pencatat, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menetapkan {@link #oleh}. Guard null/blank: nilai {@code null} atau string kosong/spasi
	 * DIABAIKAN (early return) -- field yang sudah terisi TIDAK PERNAH ditimpa balik ke kosong
	 * oleh pemanggilan setter ini dengan argumen kosong. Pola guard yg sama dipakai
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pencatat; diabaikan bila {@code null} atau blank.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * ID/username petugas/proses yang membuat baris faktur ini, pasangan {@link #getOleh()}.
	 *
	 * @return id/username pencatat, atau {@code null} bila belum pernah diisi.
	 */
	@Column(name = "oleh_id")
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan {@link #olehId}. Guard null/blank sama seperti {@link #setOleh(String)}: nilai
	 * {@code null}/kosong/spasi diabaikan, nilai lama dipertahankan.
	 *
	 * @param olehId id/username pencatat; diabaikan bila {@code null} atau blank.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Waktu baris faktur ini dicatat. Getter null-safe: mengembalikan waktu SEKARANG bila kolom
	 * belum diisi, BUKAN {@code null} -- catatan yang sama seperti {@link #getTanggal()}.
	 *
	 * @return waktu pencatatan, tidak pernah {@code null}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu")
	public Date getWaktu() {
		return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
	}

	/**
	 * Menetapkan waktu pencatatan baris ini.
	 *
	 * @param waktu waktu pencatatan baru.
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Timestamp perubahan terakhir baris faktur ini, diisi otomatis oleh {@link #onUpdate()}
	 * setiap kali Hibernate melakukan {@code UPDATE}. Nilai awal (sebelum ada update apa pun)
	 * adalah waktu instansiasi objek Java, BUKAN waktu insert DB sesungguhnya.
	 *
	 * @return waktu perubahan terakhir baris ini.
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
