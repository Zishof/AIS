package ais.database.model.crm;

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
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;

/**
 * <h3>CrmLead — entitas inti modul CRM (Lead &amp; Peluang dalam satu tabel)</h3>
 *
 * <p>Mengikuti model CRM Odoo: Lead dan Peluang (Opportunity) adalah satu jenis data yang sama,
 * dibedakan oleh {@link #tipe} — Lead adalah prospek mentah sebelum dikualifikasi, Peluang adalah
 * prospek yang sudah dikonfirmasi layak ditindaklanjuti lewat {@link CrmStage} pipeline.</p>
 *
 * <p><b>Generik lintas domain.</b> {@link #pipelineType} menentukan konteks penggunaan (admisi
 * calon mahasiswa/siswa, kemitraan/vendor, donasi alumni, dst — dikonfigurasi admin, lihat
 * {@link CrmPipelineType}). {@link #jenisEntitasTerkait}/{@link #entitasTerkaitId} adalah pranala
 * polimorfik OPSIONAL ke record asal (mis. CalonMahasiswa/CalonSiswa) — memakai pola field string
 * (bukan FK keras), sama seperti {@code ais.database.model.ticket.Ticket#pengajuUserId}/
 * {@code pengajuTipe}, supaya satu entity ini bisa dipakai lintas domain tanpa FK ke banyak tabel
 * berbeda.</p>
 *
 * <p>Tabel {@code public.crm_lead} dibuat otomatis oleh {@code hbm2ddl=update} saat restart.
 * Di-audit Envers untuk riwayat/recovery.</p>
 *
 * <h4>Verifikasi tujuan modul (dari kode, bukan asumsi)</h4>
 * <p>Model ini sendiri TIDAK mengunci diri ke satu domain: satu-satunya penanda domain adalah
 * {@link #pipelineType} (baris konfigurasi admin di {@link CrmPipelineType}, mis. "Admisi
 * Mahasiswa Baru"/"Kemitraan/Vendor"/"Donasi Alumni" — lihat teks bantuan pada
 * {@code ais.action.master.ticket.CrmKonfigurasiHelper}) dan pranala longgar
 * {@link #jenisEntitasTerkait}/{@link #entitasTerkaitId} yang TIDAK memiliki FK database maupun
 * kode yang membaca/menulis tabel {@code CalonMahasiswa}/{@code CalonSiswa}/{@code BiodataCalonMahasiswa}
 * di mana pun pada paket {@code ais.action.master.ticket} (jalur reguler pemakai modul ini, lihat
 * {@code CrmPipelineHelper}/{@code CrmDashboardHelper}/{@code CrmNotifikasi} serta
 * {@code ais.common.newui.ticket.NewUiTicketController}). Dengan kata lain: modul CRM ini adalah
 * mesin pipeline penjualan/prospek GENERIK yang dibangun sebagai bagian dari modul Ticketing
 * (lihat javadoc kelas-kelas action tersebut), <b>bukan</b> terintegrasi langsung ke alur PMB/PSB
 * AIS yang sudah ada — integrasi PMB/PSB, bila dipakai, sepenuhnya bersifat manual/administratif
 * lewat pengisian {@link #jenisEntitasTerkait} = {@link #ENTITAS_ADMISI_MAHASISWA}/
 * {@link #ENTITAS_ADMISI_SISWA} dan {@link #entitasTerkaitId} oleh pengguna, tanpa sinkronisasi
 * otomatis dua arah dengan entity admisi yang sesungguhnya.</p>
 *
 * <h4>Catatan arsitektur: tanpa pemisahan tenant/satuan kerja</h4>
 * <p>Berbeda dengan {@code ais.database.model.ticket.Ticket} (yang dibatasi
 * {@code TicketingAction#scopedCriteria(Session, Tbmuser)} berdasar satuan kerja), entity ini
 * TIDAK memiliki field {@code satuanKerja} sama sekali, dan jalur baca di
 * {@code NewUiTicketController} (mis. {@code crmLead(Session, Long)}) memuat baris memakai
 * {@code session.get(...)} langsung tanpa kriteria kepemilikan/unit apa pun. Artinya seluruh
 * pengguna yang memiliki akses ke fitur CRM pada modul Ticketing dapat melihat SEMUA lead/peluang
 * lintas unit kerja — ini pola arsitektur yang sama seperti {@link CrmCatatan}/{@link CrmActivity}
 * (lihat javadoc masing-masing), bukan bug yang unik pada satu titik.</p>
 *
 * <p>Tidak ditemukan pola bolt-on modern (mis. {@code @MappedSuperclass} keamanan, HMAC, kunci
 * idempotency, atau registry tenant eksplisit) pada paket {@code ais.database.model.crm} —
 * seluruh 8 entity mengikuti konvensi lama AIS ({@code extends GeneralValueObject}, pasangan
 * {@code oleh}/{@code olehId}/{@code tanggal_dirubah}, flag {@code aktif} default true,
 * {@code @Audited} Envers, {@code onUpdate()} mendelegasikan ke
 * {@code AuditTimestampInterceptor.ubah(this)}).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "crm_lead")
public class CrmLead extends GeneralValueObject {

	private static final long serialVersionUID = 3120260815006L;

	/** Tipe data: prospek mentah, atau sudah dikualifikasi jadi peluang. */
	public static final String TIPE_LEAD = "LEAD";
	public static final String TIPE_PELUANG = "PELUANG";

	/** Status menang/kalah pipeline. */
	public static final String STATUS_OPEN = "OPEN";
	public static final String STATUS_WON = "WON";
	public static final String STATUS_LOST = "LOST";

	/** Jenis entitas terkait yang umum dipakai — bebas diisi nilai lain (LAINNYA) oleh UI. */
	public static final String ENTITAS_ADMISI_MAHASISWA = "ADMISI_MAHASISWA";
	public static final String ENTITAS_ADMISI_SISWA = "ADMISI_SISWA";
	public static final String ENTITAS_MITRA_VENDOR = "MITRA_VENDOR";
	public static final String ENTITAS_ALUMNI_DONATUR = "ALUMNI_DONATUR";
	public static final String ENTITAS_LAINNYA = "LAINNYA";

	/** Primary key baris {@code crm_lead}. */
	private Long id;
	/** Tipe data mentah; lihat {@link #getTipe()} untuk perlakuan default-nya. */
	private String tipe;
	/** Jenis pipeline (domain) tempat lead ini berada; lihat {@link CrmPipelineType}. */
	private CrmPipelineType pipelineType;
	/** Tahap Kanban saat ini pada {@link #pipelineType}; lihat {@link CrmStage}. */
	private CrmStage stage;

	/** Judul singkat lead/peluang, ditampilkan sebagai label utama pada kartu Kanban. */
	private String judul;
	/** Nama kontak person yang dihubungi; teks bebas, tidak berelasi ke entity pengguna/siswa. */
	private String kontakNama;
	/** Alamat email kontak; teks bebas, tidak divalidasi format di level entity. */
	private String kontakEmail;
	/** Nomor telepon kontak; teks bebas, tidak dinormalisasi (format internasional/lokal campur). */
	private String kontakTelepon;
	/** Nama instansi/asal kontak (mis. nama sekolah asal, nama perusahaan mitra). */
	private String kontakInstansi;

	/** Jenis entity asal pada pranala polimorfik opsional; lihat konstanta {@code ENTITAS_*}. */
	private String jenisEntitasTerkait;
	/** Primary key baris pada entity asal yang ditunjuk {@link #jenisEntitasTerkait}; tanpa FK database. */
	private Long entitasTerkaitId;

	/** Sumber prospek (mis. "Pameran", "Rujukan", "Website"); teks bebas. */
	private String sumber;

	/** Tim penjualan/penanganan yang bertanggung jawab atas lead ini; lihat {@link CrmSalesTeam}. */
	private CrmSalesTeam salesTeam;
	/** Pengguna individu yang ditugaskan menindaklanjuti lead ini. */
	private Tbmuser ditugaskanUser;

	/** Estimasi nilai finansial peluang (mis. potensi biaya kuliah/nilai kontrak mitra). */
	private BigDecimal nilaiEstimasi;
	/** Persentase probabilitas menang (0-100); lihat {@link #getProbabilitas()} untuk default-nya. */
	private Integer probabilitas;

	/** Tanggal target penutupan peluang yang diharapkan (perkiraan, bukan tanggal aktual). */
	private Date tanggalTutupDiharapkan;
	/** Status akhir menang/kalah/masih berjalan; lihat konstanta {@code STATUS_*} dan {@link #getStatusMenangKalah()}. */
	private String statusMenangKalah;
	/** Alasan kalah terstruktur, terisi hanya bila {@link #statusMenangKalah} = {@link #STATUS_LOST}; lihat {@link CrmLostReason}. */
	private CrmLostReason lostReason;
	/** Catatan bebas tambahan seputar alasan kalah, melengkapi {@link #lostReason}. */
	private String catatanKalah;

	/** Tanggal lead pertama kali dibuat; default waktu instansiasi object (bukan waktu commit ke DB). */
	private Date tanggalDibuat = ais.ui.util.WaktuUtil.getDate();
	/** Tanggal saat {@link #tipe} berubah dari {@link #TIPE_LEAD} menjadi {@link #TIPE_PELUANG}. */
	private Date tanggalDikonversiPeluang;
	/** Tanggal pipeline ditutup (menang atau kalah). */
	private Date tanggalDitutup;

	/** Flag aktif/nonaktif (soft delete); lihat {@link #getAktif()} untuk default-nya. */
	private Boolean aktif;
	/** Nama pengguna pengubah terakhir; diisi jalur audit, lihat {@link #setOleh(String)}. */
	private String oleh;
	/** Id pengguna pengubah terakhir; diisi jalur audit, lihat {@link #setOlehId(String)}. */
	private String olehId;
	/** Stempel waktu perubahan terakhir; diperbarui otomatis lewat {@link #onUpdate()} saat update. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Constructor default tanpa argumen, dibutuhkan Hibernate untuk hidrasi entity. */
	public CrmLead() {
	}

	/**
	 * Mengembalikan primary key baris {@code crm_lead}.
	 *
	 * @return primary key, atau {@code null} bila entity belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/**
	 * Menyetel primary key. Tanpa validasi; kolom {@code insertable = false} sehingga nilai ini
	 * hanya relevan setelah baris ada (mis. hasil {@code session.get}), bukan saat insert baru.
	 *
	 * @param id nilai primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan tipe data: {@link #TIPE_LEAD} (prospek mentah) atau {@link #TIPE_PELUANG}
	 * (sudah dikualifikasi). Nilai {@code null}/kosong dianggap {@link #TIPE_LEAD} — lead yang baru
	 * dibuat tanpa tipe eksplisit selalu tampil sebagai prospek mentah, bukan peluang.
	 *
	 * @return tipe data, tidak pernah {@code null}/kosong
	 */
	@Column(name = "tipe", nullable = false, length = 32)
	public String getTipe() {
		return tipe == null || tipe.trim().isEmpty() ? TIPE_LEAD : tipe;
	}

	/**
	 * Menyetel tipe data mentah. Tanpa validasi terhadap konstanta {@code TIPE_*}; pemanggil
	 * bertanggung jawab memakai {@link #TIPE_LEAD}/{@link #TIPE_PELUANG} agar konsisten dengan
	 * logika default di {@link #getTipe()}.
	 *
	 * @param tipe tipe data baru
	 */
	public void setTipe(String tipe) {
		this.tipe = tipe;
	}

	/**
	 * Mengembalikan jenis pipeline (domain konfigurasi) tempat lead ini berada, setelah diresolusi
	 * lewat {@link GeneralValueObject#check(Object)} agar proxy lazy yang sudah detached tidak
	 * meledak saat diakses lintas request/cache. Kolom database {@code nullable = false} — setiap
	 * lead WAJIB memiliki pipeline type pada tingkat skema, meski getter ini sendiri toleran
	 * terhadap nilai {@code null} in-memory (mengembalikannya apa adanya bila {@code check()} tidak
	 * bisa meresolusinya).
	 *
	 * @return jenis pipeline lead ini
	 * @see CrmPipelineType
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pipeline_type", nullable = false)
	public CrmPipelineType getPipelineType() {
		pipelineType = check(pipelineType);
		return pipelineType;
	}

	/**
	 * Menyetel jenis pipeline. Relasi memakai {@code cascade = PERSIST, MERGE} sehingga menyimpan
	 * lead baru dengan {@link CrmPipelineType} yang belum tersimpan akan ikut menyimpan baris
	 * pipeline type tersebut dalam transaksi yang sama.
	 *
	 * @param pipelineType jenis pipeline baru
	 */
	public void setPipelineType(CrmPipelineType pipelineType) {
		this.pipelineType = pipelineType;
	}

	/**
	 * Mengembalikan tahap Kanban saat ini, setelah diresolusi lewat {@link GeneralValueObject#check(Object)}.
	 * Kolom database mengizinkan {@code null} — lead yang baru dibuat sebelum dipindahkan ke kolom
	 * Kanban pertama bisa saja belum memiliki tahap.
	 *
	 * @return tahap Kanban saat ini, atau {@code null} bila belum ditetapkan
	 * @see CrmStage
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "stage", nullable = true)
	public CrmStage getStage() {
		stage = check(stage);
		return stage;
	}

	/**
	 * Menyetel tahap Kanban. Tidak ada validasi bahwa {@link CrmStage} yang disetel benar-benar
	 * milik {@link #pipelineType} lead ini — konsistensi itu adalah tanggung jawab pemanggil (UI
	 * drag-and-drop Kanban di {@code CrmPipelineHelper}).
	 *
	 * @param stage tahap Kanban baru
	 */
	public void setStage(CrmStage stage) {
		this.stage = stage;
	}

	/**
	 * Mengembalikan judul singkat lead/peluang, ditampilkan sebagai label utama pada kartu Kanban
	 * dan sebagai bagian dari {@link #toString()}.
	 *
	 * @return judul lead/peluang
	 */
	@Column(name = "judul", nullable = false, columnDefinition = "text")
	public String getJudul() {
		return judul;
	}

	/**
	 * Menyetel judul singkat. Tanpa validasi meski kolom database {@code nullable = false} —
	 * pelanggaran constraint baru muncul saat flush ke database, bukan saat setter dipanggil.
	 *
	 * @param judul judul baru
	 */
	public void setJudul(String judul) {
		this.judul = judul;
	}

	/**
	 * Alias agar kompatibel dengan {@code getNama()} milik {@link GeneralValueObject} (dipakai
	 * combo/label umum). Mengembalikan {@link #judul} apa adanya, tanpa fallback ke kode seperti
	 * {@code toString()} default kelas induk.
	 *
	 * @return {@link #judul}
	 */
	public String getNama() {
		return judul;
	}

	/**
	 * Mengembalikan nama kontak person yang dihubungi.
	 *
	 * @return nama kontak, teks bebas
	 */
	@Column(name = "kontak_nama", nullable = true, length = 255)
	public String getKontakNama() {
		return kontakNama;
	}

	/**
	 * Menyetel nama kontak.
	 *
	 * @param kontakNama nama kontak baru
	 */
	public void setKontakNama(String kontakNama) {
		this.kontakNama = kontakNama;
	}

	/**
	 * Mengembalikan alamat email kontak. Tidak divalidasi format di level entity.
	 *
	 * @return email kontak
	 */
	@Column(name = "kontak_email", nullable = true, length = 255)
	public String getKontakEmail() {
		return kontakEmail;
	}

	/**
	 * Menyetel alamat email kontak.
	 *
	 * @param kontakEmail email kontak baru
	 */
	public void setKontakEmail(String kontakEmail) {
		this.kontakEmail = kontakEmail;
	}

	/**
	 * Mengembalikan nomor telepon kontak. Teks bebas, tidak dinormalisasi.
	 *
	 * @return nomor telepon kontak
	 */
	@Column(name = "kontak_telepon", nullable = true, length = 64)
	public String getKontakTelepon() {
		return kontakTelepon;
	}

	/**
	 * Menyetel nomor telepon kontak.
	 *
	 * @param kontakTelepon nomor telepon baru
	 */
	public void setKontakTelepon(String kontakTelepon) {
		this.kontakTelepon = kontakTelepon;
	}

	/**
	 * Mengembalikan nama instansi/asal kontak (mis. nama sekolah asal, nama perusahaan mitra).
	 *
	 * @return nama instansi kontak
	 */
	@Column(name = "kontak_instansi", nullable = true, length = 255)
	public String getKontakInstansi() {
		return kontakInstansi;
	}

	/**
	 * Menyetel nama instansi/asal kontak.
	 *
	 * @param kontakInstansi nama instansi baru
	 */
	public void setKontakInstansi(String kontakInstansi) {
		this.kontakInstansi = kontakInstansi;
	}

	/**
	 * Mengembalikan jenis entity asal pada pranala polimorfik opsional (lihat konstanta
	 * {@code ENTITAS_*} dan penjelasan pola di javadoc kelas). Nilai ini murni informatif/teks
	 * bebas — tidak ada validasi bahwa entity dengan {@link #entitasTerkaitId} pada tabel yang
	 * bersangkutan benar-benar ada.
	 *
	 * @return jenis entity terkait, atau {@code null} bila lead tidak dipranalakan ke entity lain
	 */
	@Column(name = "jenis_entitas_terkait", nullable = true, length = 64)
	public String getJenisEntitasTerkait() {
		return jenisEntitasTerkait;
	}

	/**
	 * Menyetel jenis entity asal pranala polimorfik.
	 *
	 * @param jenisEntitasTerkait jenis entity baru
	 */
	public void setJenisEntitasTerkait(String jenisEntitasTerkait) {
		this.jenisEntitasTerkait = jenisEntitasTerkait;
	}

	/**
	 * Mengembalikan primary key baris pada entity asal yang ditunjuk {@link #jenisEntitasTerkait}.
	 * Disimpan sebagai kolom biasa ({@code Long}), BUKAN foreign key database — tidak ada
	 * constraint referential integrity yang menjaga baris tujuan tetap ada; menghapus baris asal
	 * tidak akan ditolak maupun memicu cascade pada lead ini.
	 *
	 * @return id entity asal, atau {@code null} bila lead tidak dipranalakan ke entity lain
	 */
	@Column(name = "entitas_terkait_id", nullable = true)
	public Long getEntitasTerkaitId() {
		return entitasTerkaitId;
	}

	/**
	 * Menyetel id entity asal pranala polimorfik.
	 *
	 * @param entitasTerkaitId id entity asal baru
	 */
	public void setEntitasTerkaitId(Long entitasTerkaitId) {
		this.entitasTerkaitId = entitasTerkaitId;
	}

	/**
	 * Mengembalikan sumber prospek (mis. "Pameran", "Rujukan", "Website"). Teks bebas, bukan
	 * lookup terstruktur seperti {@link CrmLostReason}/{@link CrmPipelineType}.
	 *
	 * @return sumber prospek
	 */
	@Column(name = "sumber", nullable = true, length = 255)
	public String getSumber() {
		return sumber;
	}

	/**
	 * Menyetel sumber prospek.
	 *
	 * @param sumber sumber prospek baru
	 */
	public void setSumber(String sumber) {
		this.sumber = sumber;
	}

	/**
	 * Mengembalikan tim penjualan/penanganan yang bertanggung jawab atas lead ini, setelah
	 * diresolusi lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * @return tim penjualan penanggung jawab, atau {@code null} bila belum ditugaskan ke tim mana pun
	 * @see CrmSalesTeam
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sales_team", nullable = true)
	public CrmSalesTeam getSalesTeam() {
		salesTeam = check(salesTeam);
		return salesTeam;
	}

	/**
	 * Menyetel tim penjualan penanggung jawab.
	 *
	 * @param salesTeam tim penjualan baru
	 */
	public void setSalesTeam(CrmSalesTeam salesTeam) {
		this.salesTeam = salesTeam;
	}

	/**
	 * Mengembalikan pengguna individu yang ditugaskan menindaklanjuti lead ini, setelah diresolusi
	 * lewat {@link GeneralValueObject#check(Object)}. Tidak divalidasi bahwa pengguna ini adalah
	 * anggota {@link #salesTeam} — lihat {@link CrmSalesTeamMember} untuk keanggotaan tim yang
	 * sesungguhnya; kedua relasi ini independen satu sama lain pada level entity.
	 *
	 * @return pengguna yang ditugaskan, atau {@code null} bila belum ada penugasan individu
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ditugaskan_user", nullable = true)
	public Tbmuser getDitugaskanUser() {
		ditugaskanUser = check(ditugaskanUser);
		return ditugaskanUser;
	}

	/**
	 * Menyetel pengguna yang ditugaskan menindaklanjuti lead ini.
	 *
	 * @param ditugaskanUser pengguna baru yang ditugaskan
	 */
	public void setDitugaskanUser(Tbmuser ditugaskanUser) {
		this.ditugaskanUser = ditugaskanUser;
	}

	/**
	 * Mengembalikan estimasi nilai finansial peluang (mis. potensi biaya kuliah/nilai kontrak
	 * mitra). Satuan mata uang tidak eksplisit pada entity ini — diasumsikan mata uang lokal
	 * institusi seperti kebanyakan field nominal AIS lainnya.
	 *
	 * @return estimasi nilai, atau {@code null} bila belum diisi
	 */
	@Column(name = "nilai_estimasi", nullable = true)
	public BigDecimal getNilaiEstimasi() {
		return nilaiEstimasi;
	}

	/**
	 * Menyetel estimasi nilai finansial peluang.
	 *
	 * @param nilaiEstimasi estimasi nilai baru
	 */
	public void setNilaiEstimasi(BigDecimal nilaiEstimasi) {
		this.nilaiEstimasi = nilaiEstimasi;
	}

	/**
	 * Mengembalikan persentase probabilitas menang (0-100). Nilai {@code null} dinormalkan menjadi
	 * {@code 0} — lead baru tanpa probabilitas eksplisit dianggap belum punya peluang menang sama
	 * sekali, bukan mewarisi {@link CrmStage#getProbabilitasDefault()} tahapnya secara otomatis;
	 * penyalinan nilai default tahap ke field ini (bila dilakukan) adalah tanggung jawab kode
	 * pemanggil saat memindahkan lead antar tahap.
	 *
	 * @return probabilitas menang, tidak pernah {@code null}
	 */
	@Column(name = "probabilitas", nullable = true)
	public Integer getProbabilitas() {
		return probabilitas == null ? 0 : probabilitas;
	}

	/**
	 * Menyetel persentase probabilitas menang. Tanpa validasi rentang 0-100.
	 *
	 * @param probabilitas probabilitas baru
	 */
	public void setProbabilitas(Integer probabilitas) {
		this.probabilitas = probabilitas;
	}

	/**
	 * Mengembalikan tanggal target penutupan peluang yang diharapkan (perkiraan, bukan tanggal
	 * aktual penutupan — lihat {@link #getTanggalDitutup()} untuk itu).
	 *
	 * @return tanggal target penutupan, atau {@code null} bila belum diperkirakan
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_tutup_diharapkan", nullable = true)
	public Date getTanggalTutupDiharapkan() {
		return tanggalTutupDiharapkan;
	}

	/**
	 * Menyetel tanggal target penutupan peluang.
	 *
	 * @param tanggalTutupDiharapkan tanggal target baru
	 */
	public void setTanggalTutupDiharapkan(Date tanggalTutupDiharapkan) {
		this.tanggalTutupDiharapkan = tanggalTutupDiharapkan;
	}

	/**
	 * Mengembalikan status akhir pipeline: {@link #STATUS_OPEN} (masih berjalan),
	 * {@link #STATUS_WON} (menang), atau {@link #STATUS_LOST} (kalah). Nilai {@code null}/kosong
	 * dianggap {@link #STATUS_OPEN} — lead yang baru dibuat tanpa status eksplisit selalu tampil
	 * sebagai masih berjalan, konsisten dengan pola default {@link #getTipe()}.
	 *
	 * @return status menang/kalah, tidak pernah {@code null}/kosong
	 */
	@Column(name = "status_menang_kalah", nullable = true, length = 16)
	public String getStatusMenangKalah() {
		return statusMenangKalah == null || statusMenangKalah.trim().isEmpty() ? STATUS_OPEN : statusMenangKalah;
	}

	/**
	 * Menyetel status akhir pipeline. Tanpa validasi terhadap konstanta {@code STATUS_*}; tidak ada
	 * pemeriksaan konsistensi otomatis dengan {@link CrmStage#getIsWon()}/{@link CrmStage#getIsLost()}
	 * pada {@link #stage} saat ini — menjaga konsistensi keduanya adalah tanggung jawab kode
	 * pemanggil (popup alasan kalah/tanggal penutupan pada UI Kanban).
	 *
	 * @param statusMenangKalah status baru
	 */
	public void setStatusMenangKalah(String statusMenangKalah) {
		this.statusMenangKalah = statusMenangKalah;
	}

	/**
	 * Mengembalikan alasan kalah terstruktur, setelah diresolusi lewat
	 * {@link GeneralValueObject#check(Object)}. Relevan hanya bila {@link #getStatusMenangKalah()}
	 * bernilai {@link #STATUS_LOST}; tidak ada constraint yang memaksa hal itu pada level entity —
	 * field ini bisa saja terisi meski status masih {@link #STATUS_OPEN}, atau kosong meski status
	 * sudah {@link #STATUS_LOST}.
	 *
	 * @return alasan kalah, atau {@code null} bila belum diisi
	 * @see CrmLostReason
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lost_reason", nullable = true)
	public CrmLostReason getLostReason() {
		lostReason = check(lostReason);
		return lostReason;
	}

	/**
	 * Menyetel alasan kalah terstruktur.
	 *
	 * @param lostReason alasan kalah baru
	 */
	public void setLostReason(CrmLostReason lostReason) {
		this.lostReason = lostReason;
	}

	/**
	 * Mengembalikan catatan bebas tambahan seputar alasan kalah, melengkapi {@link #lostReason}
	 * terstruktur dengan penjelasan naratif.
	 *
	 * @return catatan kalah, teks bebas
	 */
	@Column(name = "catatan_kalah", nullable = true, columnDefinition = "text")
	public String getCatatanKalah() {
		return catatanKalah;
	}

	/**
	 * Menyetel catatan bebas alasan kalah.
	 *
	 * @param catatanKalah catatan kalah baru
	 */
	public void setCatatanKalah(String catatanKalah) {
		this.catatanKalah = catatanKalah;
	}

	/**
	 * Mengembalikan tanggal lead pertama kali dibuat. Diinisialisasi ke waktu instansiasi object
	 * ({@code WaktuUtil.getDate()}) sehingga entity baru selalu punya nilai walau jalur simpan lupa
	 * mengisinya secara eksplisit — namun nilai ini bisa berbeda dari waktu commit sesungguhnya ke
	 * database bila object dibuat lebih dulu lalu disimpan belakangan.
	 *
	 * @return tanggal dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dibuat", nullable = true)
	public Date getTanggalDibuat() {
		return tanggalDibuat;
	}

	/**
	 * Menyetel tanggal dibuat.
	 *
	 * @param tanggalDibuat tanggal dibuat baru
	 */
	public void setTanggalDibuat(Date tanggalDibuat) {
		this.tanggalDibuat = tanggalDibuat;
	}

	/**
	 * Mengembalikan tanggal saat {@link #tipe} berubah dari {@link #TIPE_LEAD} menjadi
	 * {@link #TIPE_PELUANG}. Tidak diisi otomatis oleh entity ini sendiri — mengisi field ini
	 * bersamaan dengan perubahan {@link #tipe} adalah tanggung jawab kode pemanggil (aksi "Konversi
	 * ke Peluang" pada UI).
	 *
	 * @return tanggal konversi ke peluang, atau {@code null} bila belum pernah dikonversi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dikonversi_peluang", nullable = true)
	public Date getTanggalDikonversiPeluang() {
		return tanggalDikonversiPeluang;
	}

	/**
	 * Menyetel tanggal konversi ke peluang.
	 *
	 * @param tanggalDikonversiPeluang tanggal konversi baru
	 */
	public void setTanggalDikonversiPeluang(Date tanggalDikonversiPeluang) {
		this.tanggalDikonversiPeluang = tanggalDikonversiPeluang;
	}

	/**
	 * Mengembalikan tanggal pipeline ditutup (menang atau kalah). Sama seperti
	 * {@link #tanggalDikonversiPeluang}, tidak diisi otomatis oleh entity — pengisiannya adalah
	 * tanggung jawab kode pemanggil saat lead dipindahkan ke tahap penutup.
	 *
	 * @return tanggal ditutup, atau {@code null} bila pipeline masih berjalan
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_ditutup", nullable = true)
	public Date getTanggalDitutup() {
		return tanggalDitutup;
	}

	/**
	 * Menyetel tanggal ditutup.
	 *
	 * @param tanggalDitutup tanggal ditutup baru
	 */
	public void setTanggalDitutup(Date tanggalDitutup) {
		this.tanggalDitutup = tanggalDitutup;
	}

	/**
	 * Mengembalikan status aktif/nonaktif (soft delete). Nilai {@code null} dianggap {@code true} —
	 * lead yang baru dibuat tanpa flag eksplisit selalu tampil aktif, konsisten dengan pola default
	 * di seluruh entity AIS ber-flag {@code aktif}.
	 *
	 * @return {@code true} bila aktif (default), {@code false} bila dinonaktifkan
	 */
	@Column(name = "aktif", nullable = true)
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif/nonaktif.
	 *
	 * @param aktif status aktif baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah entity ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	@Column(name = "oleh")
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan validasi non-trivial: nilai {@code null}
	 * atau string kosong/spasi diabaikan diam-diam (method langsung {@code return} tanpa mengubah
	 * apa pun) — pola yang sama seperti {@link GeneralValueObject#setOleh(String)} — sehingga jejak
	 * audit yang sudah terisi tidak bisa terhapus oleh jalur simpan yang kebetulan tidak membawa
	 * informasi pengguna (mis. proses batch tanpa sesi login).
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah entity ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	@Column(name = "oleh_id")
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir, dengan validasi non-trivial yang sama seperti
	 * {@link #setOleh(String)}: nilai {@code null}/kosong diabaikan diam-diam.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dirubah")
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi; nilainya akan ditimpa otomatis
	 * oleh {@link #onUpdate()} pada jalur update Hibernate.
	 *
	 * @param tanggal_dirubah waktu perubahan baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Hook siklus hidup Hibernate {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat
	 * sebelum statement {@code UPDATE} dieksekusi untuk baris ini, mendelegasikan pembaruan stempel
	 * waktu ke {@code AuditTimestampInterceptor.ubah(this)} — pola audit standar seluruh entity
	 * AIS yang mewarisi {@link GeneralValueObject}.
	 */
	@PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Representasi teks ringkas untuk logging/debugging: {@code "<id>-<judul>"}.
	 *
	 * @return representasi teks entity ini
	 */
	public String toString() {
		return id + "-" + judul;
	}
}
