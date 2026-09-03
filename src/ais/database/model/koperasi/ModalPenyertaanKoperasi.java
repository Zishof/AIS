package ais.database.model.koperasi;

// Bagian dari sub-modul Simpan Pinjam (USP) Koperasi — fitur Modal Penyertaan (penguatan modal).

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

import org.hibernate.envers.Audited;

/**
 * <h2>ModalPenyertaanKoperasi — Modal Penyertaan (Penguatan Modal Koperasi)</h2>
 *
 * <p>
 * Entity ini mencatat <b>modal penyertaan</b>, yaitu dana yang ditanamkan ke koperasi untuk
 * memperkuat permodalan di luar simpanan pokok dan simpanan wajib. Sesuai UU Perkoperasian dan SOM
 * USPK, modal penyertaan dapat berasal dari <b>anggota</b> maupun <b>pihak lain (non-anggota/investor)</b>,
 * disertai perjanjian yang mengatur jumlah, jangka waktu, imbal hasil, serta kesediaan penyerta ikut
 * menanggung risiko usaha. Berbeda dengan simpanan yang merupakan kewajiban koperasi kepada anggota,
 * modal penyertaan tergolong <b>modal/ekuitas</b> yang memperbesar Modal Sendiri dan kemampuan
 * koperasi menyalurkan pinjaman.
 * </p>
 *
 * <h3>Mengapa dibutuhkan?</h3>
 * <p>
 * Fitur ini melengkapi sub-modul simpan pinjam agar seluruh komponen permodalan koperasi tercatat
 * rapi. Dengan mengetahui total modal penyertaan yang masih aktif, pengurus dapat menghitung Modal
 * Sendiri secara lebih tepat (memengaruhi rasio permodalan, batas maksimum pemberian pinjaman/BMPP,
 * dan penilaian kesehatan), sekaligus memantau kewajiban imbal hasil kepada para penyerta.
 * </p>
 *
 * <h3>Isi pokok</h3>
 * <ul>
 * <li>{@link #getJenisPenyerta()} — {@link #JENIS_ANGGOTA} atau {@link #JENIS_NON_ANGGOTA}.</li>
 * <li>{@link #getNamaPenyerta()} — nama penyerta (anggota atau investor luar).</li>
 * <li>{@link #getNomorPerjanjian()} — nomor akta/perjanjian penyertaan.</li>
 * <li>{@link #getNominal()} — besar modal yang disetor.</li>
 * <li>{@link #getTanggalMasuk()} — tanggal dana masuk.</li>
 * <li>{@link #getJangkaWaktuBulan()} — tenor (0 berarti tanpa batas waktu).</li>
 * <li>{@link #getImbalHasilPersen()} — imbal hasil/bagi hasil per tahun (persen).</li>
 * <li>{@link #getStatus()} — {@link #STATUS_AKTIF}, {@link #STATUS_JATUH_TEMPO}, atau
 * {@link #STATUS_DITARIK}.</li>
 * </ul>
 *
 * <p>
 * Metode {@code @Transient} menyediakan turunan yang berguna tanpa disimpan:
 * {@link #getTanggalJatuhTempo()} (tanggal masuk + jangka waktu) dan
 * {@link #getEstimasiImbalHasilTahunan()} (nominal &times; imbal hasil %).
 * </p>
 *
 * <h3>Catatan desain</h3>
 * <p>
 * Mengikuti pola rumah AIS: kunci {@code IDENTITY}, relasi {@link Koperasi} lazy dengan {@code check(...)},
 * hook audit {@code @PreUpdate}, {@code @Audited} (modal penyertaan adalah komitmen penting yang harus
 * dapat ditelusuri), getter numerik/boolean aman-null, dan kompatibel Java 1.7. Terdaftar di
 * {@code hibernate.cfg.xml} sehingga {@code hbm2ddl=update} membuat tabel
 * <code>koperasi.modal_penyertaan</code> otomatis. Entity tidak menyentuh basis data langsung dan
 * tidak mengubah perilaku entity lain.
 * </p>
 *
 * @see PembagianShu
 * @see ais.action.master.koperasi.ModalPenyertaanKoperasiAction
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "modal_penyertaan")
public class ModalPenyertaanKoperasi extends ais.database.model.GeneralValueObject {

	private static final long serialVersionUID = 6620200014412991001L;

	/** Nilai {@link #jenisPenyerta} untuk modal penyertaan yang berasal dari anggota koperasi sendiri. */
	public static final String JENIS_ANGGOTA = "ANGGOTA";
	/** Nilai {@link #jenisPenyerta} untuk modal penyertaan yang berasal dari pihak luar (investor/non-anggota). */
	public static final String JENIS_NON_ANGGOTA = "NON_ANGGOTA";

	/** Nilai {@link #status}: dana masih tertanam, belum jatuh tempo maupun ditarik. */
	public static final String STATUS_AKTIF = "AKTIF";
	/** Nilai {@link #status}: masa tenor ({@link #jangkaWaktuBulan}) telah lewat namun dana belum ditarik. */
	public static final String STATUS_JATUH_TEMPO = "JATUH_TEMPO";
	/** Nilai {@link #status}: dana telah dikembalikan kepada penyerta (kaki kedua, lihat {@link #postingHistoryKembali}). */
	public static final String STATUS_DITARIK = "DITARIK";

	/** Primary key, IDENTITY dari kolom {@code id}. */
	private Long id;
	/** Nama pengguna (username) pembuat/pengubah terakhir baris ini, untuk audit ringan. */
	private String oleh;
	/** Id pengguna pembuat/pengubah terakhir baris ini, pasangan {@link #oleh}. */
	private String olehId;

	/** Koperasi pemilik baris modal penyertaan ini. */
	private Koperasi koperasi;
	/** {@link #JENIS_ANGGOTA} atau {@link #JENIS_NON_ANGGOTA}; default {@link #JENIS_ANGGOTA}. */
	private String jenisPenyerta = JENIS_ANGGOTA;
	/** Nama penyerta modal (nama anggota atau nama investor luar). */
	private String namaPenyerta;
	/** Nomor akta/perjanjian penyertaan modal yang mengikat kedua belah pihak. */
	private String nomorPerjanjian;
	/** Nominal modal yang disetorkan (rupiah). */
	private Double nominal = 0.0;
	/** Tanggal dana modal penyertaan diterima koperasi (dasar tanggal kaki jurnal masuk). */
	private Date tanggalMasuk;
	/** Tenor penyertaan dalam bulan; {@code 0} berarti tanpa batas waktu (evergreen). */
	private Integer jangkaWaktuBulan = 0;
	/** Imbal hasil/bagi hasil yang dijanjikan per tahun, dalam persen dari {@link #nominal}. */
	private Double imbalHasilPersen = 0.0;
	/** {@link #STATUS_AKTIF}, {@link #STATUS_JATUH_TEMPO}, atau {@link #STATUS_DITARIK}; default {@link #STATUS_AKTIF}. */
	private String status = STATUS_AKTIF;
	/** Tanggal dana dikembalikan ke penyerta (dasar tanggal kaki jurnal kembali); {@code null} selama belum ditarik. */
	private Date tanggalKembali;
	/** Catatan bebas mengenai baris modal penyertaan ini. */
	private String keterangan;
	/** Penanda baris masih berlaku untuk ditampilkan/dipilih di layar; {@code true} secara default. */
	private Boolean aktif = true;

	/** Konstruktor kosong, dipakai Hibernate dan saat membangun baris modal penyertaan baru sebelum diisi. */
	public ModalPenyertaanKoperasi() {
	}

	/**
	 * Konstruktor pintasan untuk merujuk sebuah baris modal penyertaan yang sudah ada hanya lewat id-nya.
	 *
	 * @param id primary key baris modal penyertaan yang sudah ada
	 */
	public ModalPenyertaanKoperasi(Long id) {
		this.id = id;
	}

	/** @return primary key baris modal penyertaan ini, atau {@code null} bila belum tersimpan. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id primary key; kolom {@code insertable = false} sehingga id sesungguhnya berasal dari IDENTITY database. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return id pengguna pembuat/pengubah terakhir baris ini, atau {@code null} bila belum pernah di-set. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Set id pengguna pembuat/pengubah. Nilai kosong/hanya-spasi diabaikan (tidak menimpa nilai
	 * lama) agar audit tidak pernah kehilangan jejak pengguna karena panggilan kosong yang tidak
	 * disengaja.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Set nama pengguna pembuat/pengubah. Sama seperti {@link #setOlehId(String)}, nilai
	 * kosong/hanya-spasi diabaikan.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/** @return nama pengguna pembuat/pengubah terakhir baris ini. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook Hibernate yang dipanggil otomatis sebelum setiap {@code UPDATE}. Mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk memperbarui
	 * {@link #tanggal_dirubah} (dan field audit sejenis) tanpa perlu campur tangan kode pemanggil.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Waktu terakhir baris ini diubah; default saat objek dibuat, diperbarui oleh {@link #onUpdate()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah waktu perubahan terakhir yang hendak diset secara manual. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu terakhir baris ini diubah. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return koperasi pemilik baris modal penyertaan ini, dimuat lazy lewat {@code check(...)}.
	 *         Catatan: relasi ini {@code nullable = true} sehingga secara skema tidak memaksa setiap
	 *         baris memiliki koperasi — penyaringan menurut koperasi berjalan (bila ada) dilakukan di
	 *         lapisan Action/query pemanggil, bukan dipaksa di sini (pola tenant-filter yang sama
	 *         dijumpai pada entity koperasi lain).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "koperasi", nullable = true)
	public Koperasi getKoperasi() {
		koperasi = check(koperasi);
		return koperasi;
	}

	/**
	 * Set koperasi pemilik. Ditolak diam-diam (field tetap {@code null}) bila {@code koperasi}
	 * bernilai {@code null} atau belum memiliki id (belum tersimpan) — mencegah baris modal
	 * penyertaan tertaut ke koperasi yang belum valid di database.
	 *
	 * @param koperasi koperasi pemilik; diabaikan (diset {@code null}) bila belum memiliki id
	 */
	public void setKoperasi(Koperasi koperasi) {
		this.koperasi = koperasi == null || koperasi.getId() == null ? null : koperasi;
	}

	/**
	 * @return jenis penyerta modal: {@link #JENIS_ANGGOTA} (default bila kosong) atau
	 *         {@link #JENIS_NON_ANGGOTA}. Tidak pernah {@code null}/kosong.
	 */
	@Column(name = "jenis_penyerta", length = 20)
	public String getJenisPenyerta() {
		return jenisPenyerta == null || jenisPenyerta.trim().isEmpty() ? JENIS_ANGGOTA : jenisPenyerta;
	}

	/** @param jenisPenyerta {@link #JENIS_ANGGOTA} atau {@link #JENIS_NON_ANGGOTA}. */
	public void setJenisPenyerta(String jenisPenyerta) {
		this.jenisPenyerta = jenisPenyerta;
	}

	/** @return nama penyerta modal (anggota atau investor luar), atau {@code null} bila belum diisi. */
	@Column(name = "nama_penyerta", length = 255)
	public String getNamaPenyerta() {
		return namaPenyerta;
	}

	/** @param namaPenyerta nama penyerta modal (anggota atau investor luar). */
	public void setNamaPenyerta(String namaPenyerta) {
		this.namaPenyerta = namaPenyerta;
	}

	/** @return nomor akta/perjanjian penyertaan modal, atau {@code null} bila belum diisi. */
	@Column(name = "nomor_perjanjian", length = 100)
	public String getNomorPerjanjian() {
		return nomorPerjanjian;
	}

	/** @param nomorPerjanjian nomor akta/perjanjian penyertaan modal. */
	public void setNomorPerjanjian(String nomorPerjanjian) {
		this.nomorPerjanjian = nomorPerjanjian;
	}

	/** @return nominal modal yang disetorkan (rupiah); tidak pernah {@code null} ({@code 0.0} sebagai fallback). */
	@Column(name = "nominal")
	public Double getNominal() {
		return nominal == null ? 0.0 : nominal;
	}

	/** @param nominal nominal modal yang disetorkan (rupiah); dasar perhitungan {@link #getEstimasiImbalHasilTahunan()}. */
	public void setNominal(Double nominal) {
		this.nominal = nominal;
	}

	/** @return tanggal dana modal penyertaan diterima koperasi, dasar tanggal kaki jurnal masuk dan {@link #getTanggalJatuhTempo()}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_masuk")
	public Date getTanggalMasuk() {
		return tanggalMasuk;
	}

	/** @param tanggalMasuk tanggal dana modal penyertaan diterima koperasi. */
	public void setTanggalMasuk(Date tanggalMasuk) {
		this.tanggalMasuk = tanggalMasuk;
	}

	/** @return tenor penyertaan dalam bulan; {@code 0} berarti tanpa batas waktu. Tidak pernah {@code null}. */
	@Column(name = "jangka_waktu_bulan")
	public Integer getJangkaWaktuBulan() {
		return jangkaWaktuBulan == null ? 0 : jangkaWaktuBulan;
	}

	/** @param jangkaWaktuBulan tenor penyertaan dalam bulan; {@code 0} atau {@code null} berarti tanpa batas waktu. */
	public void setJangkaWaktuBulan(Integer jangkaWaktuBulan) {
		this.jangkaWaktuBulan = jangkaWaktuBulan;
	}

	/** @return imbal hasil/bagi hasil per tahun dalam persen dari nominal; tidak pernah {@code null} ({@code 0.0} sebagai fallback). */
	@Column(name = "imbal_hasil_persen")
	public Double getImbalHasilPersen() {
		return imbalHasilPersen == null ? 0.0 : imbalHasilPersen;
	}

	/** @param imbalHasilPersen imbal hasil/bagi hasil per tahun dalam persen; dasar perhitungan {@link #getEstimasiImbalHasilTahunan()}. */
	public void setImbalHasilPersen(Double imbalHasilPersen) {
		this.imbalHasilPersen = imbalHasilPersen;
	}

	/**
	 * @return status baris modal penyertaan: {@link #STATUS_AKTIF} (default bila kosong),
	 *         {@link #STATUS_JATUH_TEMPO}, atau {@link #STATUS_DITARIK}. Tidak pernah
	 *         {@code null}/kosong. Catatan: transisi status (mis. AKTIF → DITARIK) tidak dijaga
	 *         otomatis oleh entity ini — nilai diset langsung oleh Action pemanggil; kaki jurnal
	 *         pengembalian sesungguhnya baru diposting saat {@link #status} == {@link #STATUS_DITARIK}
	 *         dan {@link #tanggalKembali} terisi (lihat {@code PostingDanaAnggotaUtil}), sehingga
	 *         status inilah yang menjadi pemicu kaki kedua, bukan sekadar label tampilan.
	 */
	@Column(name = "status", length = 20)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_AKTIF : status;
	}

	/** @param status {@link #STATUS_AKTIF}, {@link #STATUS_JATUH_TEMPO}, atau {@link #STATUS_DITARIK}. */
	public void setStatus(String status) {
		this.status = status;
	}

	/** @return tanggal dana dikembalikan ke penyerta (dasar tanggal kaki jurnal kembali), atau {@code null} selama belum ditarik. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_kembali")
	public Date getTanggalKembali() {
		return tanggalKembali;
	}

	/** @param tanggalKembali tanggal dana dikembalikan ke penyerta. */
	public void setTanggalKembali(Date tanggalKembali) {
		this.tanggalKembali = tanggalKembali;
	}

	/** @return catatan bebas mengenai baris modal penyertaan ini, atau {@code null} bila belum diisi. */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return keterangan;
	}

	/** @param keterangan catatan bebas mengenai baris modal penyertaan ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return penanda baris masih berlaku untuk ditampilkan/dipilih di layar; tidak pernah {@code null} ({@code true} sebagai fallback). */
	@Column(name = "aktif")
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif penanda baris masih berlaku untuk ditampilkan/dipilih di layar. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * <h3>Perkiraan tanggal jatuh tempo</h3>
	 *
	 * <p>
	 * Dihitung sebagai {@link #getTanggalMasuk()} ditambah {@link #getJangkaWaktuBulan()} bulan,
	 * memakai {@link java.util.Calendar#add(int, int)} sehingga secara otomatis menghormati jumlah
	 * hari tiap bulan (mis. akhir Januari + 1 bulan tetap jatuh di akhir/awal Februari yang benar,
	 * bukan menambah 30 hari secara kasar). Method ini murni turunan ({@code @Transient}, tidak
	 * dipersist) — dihitung ulang setiap kali dipanggil dari nilai {@link #tanggalMasuk} dan
	 * {@link #jangkaWaktuBulan} yang tersimpan saat itu, sehingga selalu konsisten dengan kedua
	 * field tersebut tanpa risiko data turunan menjadi basi (stale) di database.
	 * </p>
	 *
	 * <p>
	 * Mengembalikan {@code null} pada dua kondisi: (1) {@link #tanggalMasuk} belum diisi — tidak
	 * ada titik awal untuk dihitung; atau (2) {@link #getJangkaWaktuBulan()} &le; 0, yang menurut
	 * konvensi field tersebut berarti penyertaan <b>tanpa batas waktu</b> (evergreen) sehingga
	 * secara definisi tidak memiliki tanggal jatuh tempo — mengembalikan tanggal apa pun di sini
	 * (mis. tanggal masuk itu sendiri) akan menyesatkan tampilan/laporan yang memakainya untuk
	 * menandai keterlambatan penarikan.
	 * </p>
	 *
	 * <p>
	 * <b>Catatan penting bagi pemanggil:</b> nilai ini adalah perkiraan berbasis tenor yang
	 * dijanjikan, bukan status aktual. Entity tidak memiliki job/scheduler yang secara otomatis
	 * mengubah {@link #status} menjadi {@link #STATUS_JATUH_TEMPO} begitu tanggal ini terlewati —
	 * transisi status tersebut (bila ada) harus dilakukan eksplisit oleh Action atau proses batch
	 * terpisah yang membandingkan tanggal ini dengan tanggal berjalan. Selama itu belum ada,
	 * baris yang sudah lewat tanggal jatuh tempo tetap tampil {@link #STATUS_AKTIF} sampai
	 * seseorang mengubahnya secara manual.
	 * </p>
	 *
	 * @return perkiraan tanggal jatuh tempo, atau {@code null} bila belum ada tanggal masuk atau
	 *         penyertaan tanpa batas waktu
	 */
	@javax.persistence.Transient
	public Date getTanggalJatuhTempo() {
		if (tanggalMasuk == null || getJangkaWaktuBulan() <= 0) {
			return null;
		}
		java.util.Calendar c = java.util.Calendar.getInstance();
		c.setTime(tanggalMasuk);
		c.add(java.util.Calendar.MONTH, getJangkaWaktuBulan());
		return c.getTime();
	}

	/**
	 * <h3>Perkiraan imbal hasil setahun (rupiah)</h3>
	 *
	 * <p>
	 * Rumus: <code>estimasi = nominal &times; imbalHasilPersen / 100</code> — bunga sederhana
	 * (simple interest) atas satu tahun penuh, dihitung dari {@link #getNominal()} dan
	 * {@link #getImbalHasilPersen()} yang tersimpan saat itu. Seperti
	 * {@link #getTanggalJatuhTempo()}, ini murni method turunan ({@code @Transient}): tidak
	 * dipersist, dan selalu dihitung ulang dari kedua field sumbernya sehingga tidak mungkin
	 * menjadi basi terhadap perubahan nominal atau persentase.
	 * </p>
	 *
	 * <p>
	 * <b>Yang PERLU diperhatikan pemanggil (potensi salah baca angka):</b>
	 * </p>
	 * <ul>
	 * <li><b>Selalu basis satu tahun penuh, tidak diprorata terhadap tenor.</b> Nama method secara
	 * eksplisit menyebut "Tahunan" sehingga secara semantik ini memang estimasi <i>laju tahunan</i>
	 * (annualized rate), bukan total imbal hasil yang akan diterima penyerta selama masa
	 * penyertaannya. Bila {@link #getJangkaWaktuBulan()} kurang dari 12 bulan (mis. penyertaan
	 * 6&nbsp;bulan), total imbal hasil riil yang jatuh tempo pada akhir tenor secara wajar adalah
	 * <i>lebih kecil</i> dari angka yang dikembalikan method ini — pemanggil yang menampilkan nilai
	 * ini sebagai "total yang akan diterima penyerta" tanpa memprorata dulu terhadap
	 * {@code jangkaWaktuBulan / 12.0} akan menyesatkan pengurus maupun penyerta modal. Sebaliknya
	 * bila tenor lebih dari 12&nbsp;bulan, angka ini juga bukan total kumulatif seluruh tenor,
	 * melainkan tetap laju satu tahun.</li>
	 * <li><b>Bunga sederhana, bukan majemuk (compound).</b> Bila imbal hasil dibukukan/ditambahkan
	 * kembali ke pokok secara berkala, perhitungan riil bisa berbeda dari sekadar mengalikan nominal
	 * awal dengan persentase — method ini tidak memodelkan itu, karena tidak ada mekanisme
	 * penambahan-ke-pokok (reinvestasi) untuk modal penyertaan di codebase saat ini.</li>
	 * <li><b>Bukan kewajiban yang tercatat di neraca.</b> Nilai ini murni proyeksi/estimasi untuk
	 * kebutuhan tampilan (mis. kartu ringkas dashboard); tidak ada jurnal akrual bulanan yang
	 * mencatat kewajiban imbal hasil berjalan berdasarkan angka ini — pencatatan jurnal yang
	 * benar-benar terjadi hanya dua kaki (masuk dan kembali) lewat {@link #postingHistory} dan
	 * {@link #postingHistoryKembali}, dan keduanya tidak menjurnal komponen imbal hasil secara
	 * terpisah dari pokok modal penyertaan itu sendiri (imbal hasil, bila dibayarkan, perlu jalur
	 * pencatatan tersendiri di luar dua cap posting ini — tidak ditemukan jalur tersebut di
	 * codebase saat ini, sehingga pembayaran imbal hasil riil kemungkinan masih ditangani manual di
	 * luar sistem, atau memang belum diimplementasikan).</li>
	 * </ul>
	 *
	 * @return estimasi imbal hasil satu tahun penuh (rupiah) = nominal &times; imbal hasil % / 100;
	 *         {@code 0.0} bila salah satu komponennya {@code 0} atau belum diisi
	 */
	@javax.persistence.Transient
	public double getEstimasiImbalHasilTahunan() {
		return getNominal() * getImbalHasilPersen() / 100.0;
	}

	/**
	 * @return representasi ringkas "nama penyerta - nominal" untuk debug/log/tampilan sederhana.
	 *         Nama penyerta kosong ditampilkan sebagai string kosong (tidak melempar exception bila
	 *         {@link #namaPenyerta} belum diisi).
	 */
	@Override
	public String toString() {
		return "Modal Penyertaan " + (namaPenyerta == null ? "" : namaPenyerta) + " - " + getNominal();
	}

	private ais.database.model.akunting.PostingHistory postingHistory;

	/**
	 * Riwayat posting jurnal (dok 61 butir B tahap 2): terisi begitu mesin
	 * {@code PostingDanaAnggotaUtil} menjurnalkan dokumen ini.
	 */
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "posting_history", nullable = true)
	public ais.database.model.akunting.PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/** @param postingHistory cap posting kaki masuk; diisi mesin posting begitu jurnal setoran dibuat, {@code null} bila belum diposting. */
	public void setPostingHistory(ais.database.model.akunting.PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	private ais.database.model.akunting.PostingHistory postingHistoryKembali;

	/**
	 * Cap posting KAKI KEDUA: pengembalian modal kepada penyerta.
	 *
	 * <p>Satu baris modal penyertaan memuat DUA peristiwa uang — setoran saat
	 * {@code tanggalMasuk} dan pengembalian saat status DITARIK / {@code tanggalKembali}. Satu cap
	 * tidak cukup: memakai cap yang sama akan membuat pembatalan salah satu kaki menghapus jurnal
	 * kaki lainnya. Pola dua cap ini sama dengan yang sudah dipakai keuangan siswa/mahasiswa
	 * ({@code postingHistoryDenda}, {@code postingHistoryDiskon}, dst).</p>
	 */
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "posting_history_kembali", nullable = true)
	public ais.database.model.akunting.PostingHistory getPostingHistoryKembali() {
		return postingHistoryKembali;
	}

	/** @param postingHistoryKembali cap posting kaki kembali; diisi mesin posting begitu jurnal pengembalian dibuat, {@code null} bila belum diposting. */
	public void setPostingHistoryKembali(
			ais.database.model.akunting.PostingHistory postingHistoryKembali) {
		this.postingHistoryKembali = postingHistoryKembali;
	}

}
