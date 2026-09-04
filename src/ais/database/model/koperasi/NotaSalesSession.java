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

/**
 * Sesi Nota Sales -- SATU realisasi untuk satu SPJ (layar legacy 40-42, ERD &sect;3.5).
 * Status: {@code NOT_STARTED -> ACTIVE -> RETURNED -> RECONCILING -> CLOSED}
 * (+SUSPENDED exception). CLOSED wajib approval Pemilik/Admin. Total-total di sini adalah
 * SNAPSHOT hasil rekonsiliasi saat tutup (sumber kebenaran tetap ledger
 * {@link NotaSalesKas}/biaya/penerimaan -- tidak pernah hanya agregat).
 *
 * <p>Rumus (ERD &sect;4): {@code HASIL_BERSIH = TOTAL_PIUTANG_DIBAYAR - TOTAL_BIAYA -
 * TOTAL_PEMBAYARAN_AKTUAL_PEMBELIAN}; {@code KAS_SEHARUSNYA = UANG_MUKA +
 * PENERIMAAN_TUNAI + PENJUALAN_TUNAI + REFUND - BIAYA_TUNAI - PEMBAYARAN_PEMBELIAN_TUNAI -
 * SETORAN}; {@code SELISIH_KAS = KAS_AKTUAL - KAS_SEHARUSNYA}. Dua rumus BERBEDA, dua-duanya
 * ditampilkan.</p>
 *
 * <p><b>Jalur legacy, bukan satu-satunya jalur.</b> Ini adalah class model legacy yang
 * dipakai {@code ais.action.servlet.api.SalesInventoryTripHelper} hanya ketika
 * {@code SalesInventoryTripTenant.aktif(ctx)} bernilai {@code false} untuk tenant yang
 * bersangkutan; tenant yang sudah bermigrasi memakai model tenant-schema paralel
 * ({@code {S}.sales_trip}/{@code sales_trip_kas}, kontraknya didokumentasikan pada
 * {@link ais.service.tenant.TenantKasTrip}) yang MENURUNKAN uang muka awal dari ledger-nya
 * sendiri alih-alih menyimpannya dua kali seperti {@link #saldoKasAwal} di sini -- lihat
 * javadoc {@link ais.service.tenant.TenantKasTrip} bagian "Uang muka awal DITURUNKAN".
 * Kedua jalur dipertahankan berdampingan sehingga tenant lama dan baru dapat dibandingkan
 * langsung pada uji kesetaraan selama migrasi berjalan.</p>
 *
 * <p><b>Chain dokumen turunan.</b> Tiga entity anak menunjuk balik ke sesi ini lewat kolom
 * {@code sesi} (semuanya {@code @ManyToOne nullable = false}): {@link NotaSalesKas} (ledger
 * kas append-only, satu-satunya sumber kebenaran saldo kas berjalan), {@link NotaSalesBiaya}
 * (biaya operasional lapangan -- satu-satunya dari ketiganya yang benar-benar dijurnal ke
 * buku besar, lewat {@code PostingBiayaSalesUtil}, lihat dok 61 butir E), dan
 * {@link NotaSalesPembelian} (tautan ke faktur kulakan yang sudah punya jalur posting
 * sendiri, bukan duplikasi). Ketiganya TIDAK menyimpan nominal langsung ke field snapshot
 * sesi ini -- field snapshot ({@link #totalBiaya}, {@link #totalPembayaranPembelian}, dst.)
 * baru diisi sekali, di titik tutup sesi ({@code SalesInventoryTripHelper.tripClose}), dari
 * agregasi SQL langsung atas ketiga tabel anak tersebut.</p>
 *
 * <p><b>Tidak ada penjaga keseimbangan kas yang memblokir penutupan.</b> {@link #selisihKas}
 * dihitung dan disimpan sebagai snapshot ({@code kasFisikAktual - kasSeharusnya}) di titik
 * tutup, tetapi TIDAK PERNAH diperiksa terhadap ambang batas apa pun sebelum sesi
 * diperbolehkan pindah ke {@link #STATUS_CLOSED} -- selisih sebesar apa pun (termasuk yang
 * sangat besar, indikasi kas hilang/salah catat) tetap membiarkan sesi ditutup selama
 * approver (Pemilik/Admin) mengeklik tutup. Nilainya murni informasional, ditampilkan ke
 * approver untuk keputusan manual. Ini adalah instans dari pola "tidak ada penjaga
 * keseimbangan otomatis" yang sudah ditemukan berulang di domain akunting/koperasi lain
 * pada inisiatif javadoc ini -- bukan temuan baru.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "nota_sales_session")
public class NotaSalesSession extends GeneralValueObject {

	/**
	 * Nomor versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}.
	 */
	private static final long serialVersionUID = 1L;

	/** Sesi baru dibuat dari SPJ tetapi belum ada aktivitas lapangan tercatat. Status awal default. */
	public static final String STATUS_NOT_STARTED = "NOT_STARTED";
	/** Sales sudah berangkat, sesi berjalan aktif -- ledger kas/biaya/pembelian boleh ditulis. */
	public static final String STATUS_ACTIVE = "ACTIVE";
	/** Sales sudah kembali ke kantor, menunggu proses rekonsiliasi dimulai. */
	public static final String STATUS_RETURNED = "RETURNED";
	/** Rekonsiliasi kas & dokumen sedang berlangsung, sebelum penutupan final. */
	public static final String STATUS_RECONCILING = "RECONCILING";
	/**
	 * Sesi ditutup final dengan approval Pemilik/Admin -- snapshot total ({@link #totalBiaya}
	 * dkk.) dan {@link #selisihKas} sudah terisi permanen, dokumen anak yang sudah AKTIF pada
	 * sesi berstatus ini tidak boleh direversal lagi (lihat pemeriksaan
	 * {@code SalesInventoryReversalHelper} yang menolak reversal biaya pada sesi CLOSED).
	 */
	public static final String STATUS_CLOSED = "CLOSED";
	/** Status pengecualian di luar alur linear normal (mis. sesi dibekukan sementara). */
	public static final String STATUS_SUSPENDED = "SUSPENDED";

	/** Primary key baris {@code nota_sales_session}, di-generate DB (identity). */
	private Long id;

	/** Nomor dokumen sesi, unik, dipakai sebagai label tampilan dan referensi pencarian. */
	private String nomor;

	/**
	 * Pengait wajib &amp; unik ke {@link SuratPerintahSalesJalan} (SPJ) induk -- SATU sesi
	 * untuk SATU SPJ ({@code unique = true} pada kolom {@code spj}), sesuai definisi kelas
	 * ini sebagai "SATU realisasi untuk satu SPJ".
	 */
	private SuratPerintahSalesJalan spj;

	/** Status siklus hidup sesi, lihat javadoc kelas untuk diagram transisi lengkap. */
	private String status;

	/** Waktu sales berangkat/memulai sesi (transisi ke {@link #STATUS_ACTIVE}). */
	private Date waktuMulai;

	/** Waktu sales kembali ke kantor (transisi ke {@link #STATUS_RETURNED}). */
	private Date waktuKembali;

	/** Waktu sesi ditutup final (transisi ke {@link #STATUS_CLOSED}). */
	private Date waktuTutup;

	/**
	 * Uang muka operasional yang dibawa saat berangkat -- padanan baris ledger
	 * {@link NotaSalesKas#JENIS_OPENING} pada jalur legacy ini (berbeda dari model tenant
	 * paralel yang menurunkan angka ini dari ledgernya sendiri, lihat javadoc kelas).
	 */
	private BigDecimal saldoKasAwal;

	/** Snapshot total penerimaan piutang customer yang dibayar TUNAI selama sesi (diisi saat tutup). */
	private BigDecimal totalPenerimaanTunai;

	/** Snapshot total penerimaan piutang customer yang dibayar NON-TUNAI selama sesi (diisi saat tutup). */
	private BigDecimal totalPenerimaanNonTunai;

	/**
	 * Snapshot total nilai {@link NotaSalesBiaya#getNilai()} seluruh dokumen biaya sesi ini
	 * (diisi saat tutup dari agregasi SQL langsung atas {@code koperasi.nota_sales_biaya}).
	 */
	private BigDecimal totalBiaya;

	/**
	 * Snapshot total {@link NotaSalesPembelian#getDibayarSesi()} seluruh dokumen pembelian
	 * sesi ini (diisi saat tutup) -- SATU-SATUNYA komponen pembelian yang mengurangi hasil
	 * bersih sesi, konsisten dengan javadoc {@link NotaSalesPembelian}.
	 */
	private BigDecimal totalPembayaranPembelian;

	/** Snapshot total setoran kembali ke pemilik ({@link NotaSalesKas#JENIS_OWNER_DEPOSIT}) selama sesi. */
	private BigDecimal totalSetoran;

	/** Kas fisik hasil hitung tangan saat penutupan sesi, diinput manual oleh approver. */
	private BigDecimal kasFisikAktual;

	/**
	 * Selisih {@code kasFisikAktual - kasSeharusnya} pada titik tutup -- lihat peringatan
	 * pada javadoc kelas: nilai ini murni informasional, TIDAK memblokir penutupan sesi
	 * berapa pun besarnya.
	 */
	private BigDecimal selisihKas;

	/** Catatan bebas approver saat menutup sesi (mis. penjelasan atas {@link #selisihKas}). */
	private String catatanPenutupan;

	/** Pengguna (Pemilik/Admin) yang menyetujui penutupan sesi ini. */
	private Tbmuser disetujuiOleh;

	/** Kolom versi optimistic-locking Hibernate ({@code @Version}), mencegah lost-update konkuren. */
	private Long version;

	/**
	 * Kolom audit shadow: nama pengguna yang membuat/mengubah baris ini. Bukan kolom
	 * Hibernate ber-anotasi {@code @Column} -- diisi manual oleh kode pemanggil. Ini
	 * keharusan teknis (superclass {@link GeneralValueObject} bukan {@code @Entity} JPA),
	 * bukan bug, konsisten dengan pola audit shadow pada entity koperasi lain (mis.
	 * {@link JenisTransaksiKoperasi#oleh}).
	 */
	private String oleh;

	/** Kolom audit shadow: id/username pengguna yang membuat/mengubah baris ini. Lihat {@link #oleh}. */
	private String olehId;

	/** Timestamp audit umum, diinisialisasi lazy oleh getter-nya bila belum pernah diisi. */
	private Date waktu;

	/**
	 * Hook Hibernate {@code @PreUpdate}: dipanggil otomatis sebelum setiap UPDATE terhadap
	 * baris ini, mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk mengisi
	 * ulang {@link #tanggal_dirubah} dengan waktu saat ini.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor default (dibutuhkan Hibernate/JPA), tidak menginisialisasi field apa pun. */
	public NotaSalesSession() {
	}

	/**
	 * Mengambil primary key baris {@code NotaSalesSession} ini.
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
	 * Mengambil nomor dokumen sesi.
	 *
	 * @return {@link #nomor} apa adanya, bisa {@code null}
	 */
	@Column(name = "nomor", length = 60, unique = true)
	public String getNomor() {
		return nomor;
	}

	/**
	 * Mengisi nomor dokumen sesi. Kolom ini {@code unique = true} pada DB -- penyimpanan
	 * dengan nomor yang sudah dipakai baris lain akan gagal dengan
	 * {@code ConstraintViolationException}.
	 *
	 * @param nomor nomor dokumen baru
	 */
	public void setNomor(String nomor) {
		this.nomor = nomor;
	}

	/**
	 * Mengambil {@link SuratPerintahSalesJalan} (SPJ) induk sesi ini.
	 *
	 * @return {@link #spj}, seharusnya tidak pernah {@code null} pada baris yang sudah
	 *         tersimpan ({@code nullable = false} pada kolom {@code spj})
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "spj", nullable = false, unique = true)
	public SuratPerintahSalesJalan getSpj() {
		spj = check(spj);
		return spj;
	}

	/**
	 * Mengisi pengait SPJ induk sesi ini. Kolom {@code spj} juga {@code unique = true} --
	 * satu SPJ hanya boleh punya satu sesi realisasi.
	 *
	 * @param spj SPJ baru
	 */
	public void setSpj(SuratPerintahSalesJalan spj) {
		this.spj = spj;
	}

	/**
	 * Mengambil status siklus hidup sesi, dengan default null-safe.
	 *
	 * @return {@link #status} apa adanya bila sudah diisi dan tidak blank; {@link
	 *         #STATUS_NOT_STARTED} bila {@code null} atau kosong/spasi saja
	 */
	@Column(name = "status", length = 30)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_NOT_STARTED : status;
	}

	/**
	 * Mengisi status siklus hidup sesi secara langsung, tanpa validasi transisi apa pun di
	 * level entity ini -- penjagaan urutan transisi (mis. hanya sesi {@code RECONCILING}
	 * yang boleh ditutup) dilakukan di lapisan pemanggil
	 * ({@code SalesInventoryTripHelper}), bukan di sini.
	 *
	 * @param status nilai status baru, sebaiknya salah satu konstanta {@code STATUS_*}
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Mengambil waktu sales berangkat/memulai sesi.
	 *
	 * @return {@link #waktuMulai}, bisa {@code null} bila sesi belum dimulai
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu_mulai")
	public Date getWaktuMulai() {
		return waktuMulai;
	}

	/**
	 * Mengisi waktu mulai sesi.
	 *
	 * @param waktuMulai timestamp baru
	 */
	public void setWaktuMulai(Date waktuMulai) {
		this.waktuMulai = waktuMulai;
	}

	/**
	 * Mengambil waktu sales kembali ke kantor.
	 *
	 * @return {@link #waktuKembali}, bisa {@code null} bila sales belum kembali
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu_kembali")
	public Date getWaktuKembali() {
		return waktuKembali;
	}

	/**
	 * Mengisi waktu kembali sesi.
	 *
	 * @param waktuKembali timestamp baru
	 */
	public void setWaktuKembali(Date waktuKembali) {
		this.waktuKembali = waktuKembali;
	}

	/**
	 * Mengambil waktu sesi ditutup final.
	 *
	 * @return {@link #waktuTutup}, bisa {@code null} bila sesi belum ditutup
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu_tutup")
	public Date getWaktuTutup() {
		return waktuTutup;
	}

	/**
	 * Mengisi waktu tutup sesi.
	 *
	 * @param waktuTutup timestamp baru
	 */
	public void setWaktuTutup(Date waktuTutup) {
		this.waktuTutup = waktuTutup;
	}

	/**
	 * Mengambil uang muka operasional yang dibawa saat berangkat, dengan default null-safe.
	 *
	 * @return {@link #saldoKasAwal}, atau {@link BigDecimal#ZERO} bila belum diisi
	 */
	@Column(name = "saldo_kas_awal", precision = 19, scale = 2)
	public BigDecimal getSaldoKasAwal() {
		return saldoKasAwal == null ? BigDecimal.ZERO : saldoKasAwal;
	}

	/**
	 * Mengisi uang muka operasional awal sesi.
	 *
	 * @param saldoKasAwal nilai baru
	 */
	public void setSaldoKasAwal(BigDecimal saldoKasAwal) {
		this.saldoKasAwal = saldoKasAwal;
	}

	/**
	 * Mengambil snapshot total penerimaan piutang customer TUNAI selama sesi.
	 *
	 * @return {@link #totalPenerimaanTunai}, atau {@link BigDecimal#ZERO} bila belum diisi
	 *         (sebelum sesi ditutup)
	 */
	@Column(name = "total_penerimaan_tunai", precision = 19, scale = 2)
	public BigDecimal getTotalPenerimaanTunai() {
		return totalPenerimaanTunai == null ? BigDecimal.ZERO : totalPenerimaanTunai;
	}

	/**
	 * Mengisi snapshot total penerimaan tunai. Umumnya hanya dipanggil sekali oleh
	 * {@code SalesInventoryTripHelper.tripClose} dari hasil agregasi SQL, bukan oleh kode
	 * aplikasi lain.
	 *
	 * @param totalPenerimaanTunai nilai baru
	 */
	public void setTotalPenerimaanTunai(BigDecimal totalPenerimaanTunai) {
		this.totalPenerimaanTunai = totalPenerimaanTunai;
	}

	/**
	 * Mengambil snapshot total penerimaan piutang customer NON-TUNAI selama sesi.
	 *
	 * @return {@link #totalPenerimaanNonTunai}, atau {@link BigDecimal#ZERO} bila belum diisi
	 */
	@Column(name = "total_penerimaan_non_tunai", precision = 19, scale = 2)
	public BigDecimal getTotalPenerimaanNonTunai() {
		return totalPenerimaanNonTunai == null ? BigDecimal.ZERO : totalPenerimaanNonTunai;
	}

	/**
	 * Mengisi snapshot total penerimaan non-tunai.
	 *
	 * @param totalPenerimaanNonTunai nilai baru
	 */
	public void setTotalPenerimaanNonTunai(BigDecimal totalPenerimaanNonTunai) {
		this.totalPenerimaanNonTunai = totalPenerimaanNonTunai;
	}

	/**
	 * Mengambil snapshot total nilai dokumen {@link NotaSalesBiaya} sesi ini.
	 *
	 * @return {@link #totalBiaya}, atau {@link BigDecimal#ZERO} bila belum diisi
	 */
	@Column(name = "total_biaya", precision = 19, scale = 2)
	public BigDecimal getTotalBiaya() {
		return totalBiaya == null ? BigDecimal.ZERO : totalBiaya;
	}

	/**
	 * Mengisi snapshot total biaya sesi.
	 *
	 * @param totalBiaya nilai baru
	 */
	public void setTotalBiaya(BigDecimal totalBiaya) {
		this.totalBiaya = totalBiaya;
	}

	/**
	 * Mengambil snapshot total {@link NotaSalesPembelian#getDibayarSesi()} sesi ini.
	 *
	 * @return {@link #totalPembayaranPembelian}, atau {@link BigDecimal#ZERO} bila belum diisi
	 */
	@Column(name = "total_pembayaran_pembelian", precision = 19, scale = 2)
	public BigDecimal getTotalPembayaranPembelian() {
		return totalPembayaranPembelian == null ? BigDecimal.ZERO : totalPembayaranPembelian;
	}

	/**
	 * Mengisi snapshot total pembayaran pembelian sesi.
	 *
	 * @param totalPembayaranPembelian nilai baru
	 */
	public void setTotalPembayaranPembelian(BigDecimal totalPembayaranPembelian) {
		this.totalPembayaranPembelian = totalPembayaranPembelian;
	}

	/**
	 * Mengambil snapshot total setoran kembali ke pemilik selama sesi.
	 *
	 * @return {@link #totalSetoran}, atau {@link BigDecimal#ZERO} bila belum diisi
	 */
	@Column(name = "total_setoran", precision = 19, scale = 2)
	public BigDecimal getTotalSetoran() {
		return totalSetoran == null ? BigDecimal.ZERO : totalSetoran;
	}

	/**
	 * Mengisi snapshot total setoran sesi.
	 *
	 * @param totalSetoran nilai baru
	 */
	public void setTotalSetoran(BigDecimal totalSetoran) {
		this.totalSetoran = totalSetoran;
	}

	/**
	 * Mengambil kas fisik hasil hitung tangan saat penutupan sesi. Berbeda dari kebanyakan
	 * field {@link BigDecimal} lain pada entity ini, getter ini TIDAK menormalkan
	 * {@code null} menjadi {@link BigDecimal#ZERO} -- {@code null} berarti belum pernah
	 * dihitung, dibedakan sengaja dari nilai fisik nol yang valid.
	 *
	 * @return {@link #kasFisikAktual} apa adanya, bisa {@code null}
	 */
	@Column(name = "kas_fisik_aktual", precision = 19, scale = 2)
	public BigDecimal getKasFisikAktual() {
		return kasFisikAktual;
	}

	/**
	 * Mengisi kas fisik aktual hasil hitung tangan approver.
	 *
	 * @param kasFisikAktual nilai baru
	 */
	public void setKasFisikAktual(BigDecimal kasFisikAktual) {
		this.kasFisikAktual = kasFisikAktual;
	}

	/**
	 * Mengambil selisih kas ({@code kasFisikAktual - kasSeharusnya}) hasil penutupan sesi.
	 * Sama seperti {@link #getKasFisikAktual()}, {@code null} TIDAK dinormalkan ke nol.
	 *
	 * @return {@link #selisihKas} apa adanya, bisa {@code null} bila sesi belum ditutup;
	 *         lihat javadoc kelas -- nilai ini tidak pernah memblokir penutupan berapa pun
	 *         besarnya
	 */
	@Column(name = "selisih_kas", precision = 19, scale = 2)
	public BigDecimal getSelisihKas() {
		return selisihKas;
	}

	/**
	 * Mengisi selisih kas hasil perhitungan penutupan sesi.
	 *
	 * @param selisihKas nilai baru
	 */
	public void setSelisihKas(BigDecimal selisihKas) {
		this.selisihKas = selisihKas;
	}

	/**
	 * Mengambil catatan bebas approver saat menutup sesi.
	 *
	 * @return {@link #catatanPenutupan} apa adanya, bisa {@code null}
	 */
	@Column(name = "catatan_penutupan", columnDefinition = "text")
	public String getCatatanPenutupan() {
		return catatanPenutupan;
	}

	/**
	 * Mengisi catatan penutupan sesi.
	 *
	 * @param catatanPenutupan teks catatan baru
	 */
	public void setCatatanPenutupan(String catatanPenutupan) {
		this.catatanPenutupan = catatanPenutupan;
	}

	/**
	 * Mengambil pengguna (Pemilik/Admin) yang menyetujui penutupan sesi.
	 *
	 * @return {@link #disetujuiOleh}, {@code null} sebelum sesi ditutup/disetujui
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh")
	public Tbmuser getDisetujuiOleh() {
		disetujuiOleh = check(disetujuiOleh);
		return disetujuiOleh;
	}

	/**
	 * Mengisi pengait pengguna yang menyetujui penutupan sesi.
	 *
	 * @param disetujuiOleh pengguna approver baru
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengambil nomor versi optimistic-locking Hibernate baris ini.
	 *
	 * @return {@link #version}, dikelola otomatis oleh Hibernate lewat anotasi {@code @Version}
	 */
	@Version
	@Column(name = "version")
	public Long getVersion() {
		return version;
	}

	/**
	 * Mengisi nomor versi secara manual. Umumnya tidak perlu dipanggil kode aplikasi karena
	 * Hibernate mengelola nilai ini otomatis pada setiap UPDATE.
	 *
	 * @param version nomor versi baru
	 */
	public void setVersion(Long version) {
		this.version = version;
	}

	/**
	 * Mengambil nama pengguna audit shadow yang terakhir membuat/mengubah baris ini.
	 *
	 * @return nilai {@link #oleh} apa adanya, bisa {@code null}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Mengisi nama pengguna audit ({@link #oleh}). Setter ini sengaja mengabaikan (no-op)
	 * input {@code null} atau string kosong/spasi saja -- nilai lama TIDAK pernah tertimpa
	 * oleh input kosong.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau blank
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengambil id/username pengguna audit shadow yang terakhir membuat/mengubah baris ini.
	 *
	 * @return nilai {@link #olehId} apa adanya, bisa {@code null}
	 */
	@Column(name = "oleh_id")
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id/username pengguna audit ({@link #olehId}). Sama seperti
	 * {@link #setOleh(String)}, input {@code null}/blank diabaikan.
	 *
	 * @param olehId id/username pengguna; diabaikan bila {@code null} atau blank
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengambil timestamp audit umum, diinisialisasi lazy bila belum pernah diisi.
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
	 * Mengisi timestamp audit umum secara manual.
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
	 * langsung karena {@link #onUpdate()} sudah mengisinya otomatis setiap UPDATE; tersedia
	 * untuk kasus seperti seeding/migrasi data.
	 *
	 * @param tanggal_dirubah timestamp baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
