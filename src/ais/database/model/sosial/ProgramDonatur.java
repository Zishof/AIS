package ais.database.model.sosial;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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
import org.json.JSONArray;

import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * Entitas Hibernate untuk program donasi/kegiatan sosial pada modul sosial legacy AIS — dipetakan
 * ke tabel {@code public.program_donatur}. Merupakan simpul PALING SENTRAL dari klaster donasi
 * legacy berpenamaan Indonesia: {@link PenyaluranDonasi} (event penyaluran) mereferensikan
 * program lewat {@link PenyaluranDonasi#getProgramDonatur()}, {@link Donatur} (donatur/pemberi
 * dana) tidak terhubung langsung ke program ini lewat FK melainkan lewat daftar id berpisah-koma
 * pada {@link #getDonaturs()}, dan entitas generasi lebih baru {@code DetailPenyaluranDonasi}
 * (berpenamaan campur Inggris, di paket yang sama) turut mereferensikan kelas ini lewat
 * {@code getProgram()} sebagai penanda opsional bila satu penyaluran dana modern terikat pada
 * satu program donasi legacy tertentu — sehingga kelas ini menjadi titik temu antara dua generasi
 * model donasi yang hidup berdampingan di paket {@code ais.database.model.sosial}.
 *
 * <h2>Peran bisnis</h2>
 * <p>Satu baris merepresentasikan satu program/kegiatan sosial (mis. "Bantuan Bencana Alam X",
 * "Beasiswa Yatim Piatu Tahun Y") yang dikelola oleh satu {@link ais.database.model.rab.SatuanKerja}
 * dan dikategorikan lewat {@link KategoriProgramDonatur}. Program membentang dari
 * {@link #getTanggalMulai()} hingga {@link #getTanggalSampai()}, membawa konten publikasi (galeri
 * foto {@link #getGambars()}, video {@link #getVideos()}, tautan luar {@link #getLinkUrl()}, dan
 * tautan peta lokasi {@link #getLinkPeta()}) yang lazimnya ditampilkan pada halaman publik/portal
 * donasi, serta daftar donatur peserta program lewat {@link #getDonaturs()}.</p>
 *
 * <h2>Alur pembuatan dan persetujuan (SOP disposisi)</h2>
 * <p>Kelas ini mewarisi {@link DataSop}, artinya setiap baris <b>boleh</b> (opsional, lihat
 * {@code nullable = true} pada {@link #getDisposisiSop()}) diikat ke satu
 * {@link ais.database.model.sop.DisposisiSop} — mekanisme alur-kerja disposisi/persetujuan generik
 * AIS yang dipakai lintas modul (bukan mekanisme khusus donasi). Selama diikat, empat getter
 * ({@link #getDibuatOleh()}, {@link #getDisetujuiOleh()}, {@link #getTanggalPembuatan()},
 * {@link #getTanggalPersetujuan()}) MENIMPA nilai kolom tersimpannya sendiri dengan nilai yang
 * diturunkan secara live dari disposisi terkait ({@code getDisposisiStart()}/
 * {@code getDisposisiSetuju()}) setiap kali dipanggil — lihat javadoc masing-masing getter untuk
 * detail dan implikasi keamanannya. <b>Penting:</b> entitas ini sendiri TIDAK membawa nominal/
 * jumlah dana apa pun (tidak ada kolom {@code jumlah}/{@code nominal}) — ia murni metadata
 * program/kegiatan beserta jejak audit pembuatan dan persetujuannya. Gerbang persetujuan
 * berbasis SOP di sini karenanya bersifat administratif (menandatangani/mencatat sah-tidaknya
 * program), BUKAN gerbang atas pergerakan uang; validasi keuangan yang sesungguhnya (saldo
 * alokasi, kompatibilitas jenis dana, dsb.) berada pada baris {@code DetailPenyaluranDonasi} milik
 * generasi model yang lebih baru, yang digerbangi lewat layanan {@code SocialDistributionService}
 * terpisah (privilese {@code FINANCE} + mesin status, bukan lewat kelas ini).</p>
 *
 * <h2>Field warisan generik ({@code oleh}/{@code olehId}/{@code tanggal_dirubah})</h2>
 * <p>Field {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} beserta accessor-nya di
 * bagian atas kelas ini adalah <b>duplikasi tekstual persis</b> dari field bernama sama yang sudah
 * disediakan induk {@link ais.database.model.GeneralValueObject} (lewat {@link DataSop}) —
 * keharusan teknis peninggalan hbm2java (setiap entity men-declare ulang field auditnya sendiri
 * alih-alih mewarisi field induk), <b>bukan</b> bug baru. {@code onUpdate()} men-delegasikan ke
 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang menyetel
 * {@code tanggal_dirubah} otomatis lewat callback JPA {@link javax.persistence.PreUpdate} setiap
 * kali baris diperbarui.</p>
 *
 * @see PenyaluranDonasi
 * @see KategoriProgramDonatur
 * @see Donatur
 * @see DataSop
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "program_donatur")
public class ProgramDonatur extends DataSop {

	/**
	 * Versi serialisasi. Nilai ini disalin apa adanya dari template hbm2java bersama entitas
	 * legacy lain di paket ini (identik dengan {@link PenyaluranDonasi#serialVersionUID}); jangan
	 * diubah karena baris tersimpan mungkin sudah diserialkan ke cache/session ZK dengan nilai ini.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris {@code program_donatur}. */
	private Long id;
	/**
	 * Nama pengguna terakhir yang mengubah baris ini. Duplikasi field audit dari
	 * {@link ais.database.model.GeneralValueObject#getOleh()} — lihat catatan kelas di atas.
	 */
	private String oleh;
	/**
	 * Id pengguna terakhir yang mengubah baris ini. Duplikasi field audit dari
	 * {@link ais.database.model.GeneralValueObject#getOlehId()} — lihat catatan kelas di atas.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna pengubah terakhir.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir. Nilai {@code null}/kosong/spasi diabaikan diam-diam
	 * (jejak audit yang sudah terisi tidak bisa terhapus oleh jalur simpan yang tidak membawa
	 * konteks pengguna, mis. proses batch).
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan validasi abaikan-nilai-kosong yang sama
	 * seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna pengubah terakhir.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@link javax.persistence.PreUpdate}: menyetel {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} setiap kali baris ini
	 * diperbarui oleh Hibernate.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}
	/** Stempel waktu perubahan terakhir; diinisialisasi ke waktu pembuatan object. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat: {@code "<id>-<nama>"}. Dipakai komponen ZK (label, combobox) yang
	 * menampilkan baris ini; perhatikan format berbeda dari konvensi {@code "kode - nama"} pada
	 * kebanyakan entity AIS lain yang mewarisi {@link ais.database.model.GeneralValueObject#toString()}
	 * apa adanya.
	 *
	 * @return gabungan id dan nama program
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode ringkas program; ditampilkan berdampingan dengan {@link #nama} pada layar daftar. */
	private String kode;
	/** Kategori program donasi ini (mis. Bencana Alam, Pendidikan, Kesehatan). Wajib diisi. */
	private KategoriProgramDonatur kategoriProgramDonatur;
	/** Tanggal mulai kegiatan/program. */
	private Date tanggalMulai;
	/** Tanggal berakhir kegiatan/program. */
	private Date tanggalSampai;
	/**
	 * Tanggal pembuatan baris, tersimpan sebagai kolom database. Bila baris ini diikat ke
	 * {@link #disposisiSop}, getter {@link #getTanggalPembuatan()} akan menimpa nilai field ini
	 * secara live dari disposisi terkait — lihat javadoc getter.
	 */
	private Date tanggalPembuatan;
	/**
	 * Tanggal persetujuan, tersimpan sebagai kolom database. Bila baris ini diikat ke
	 * {@link #disposisiSop}, getter {@link #getTanggalPersetujuan()} akan menimpa nilai field ini
	 * secara live dari disposisi terkait — lihat javadoc getter.
	 */
	private Date tanggalPersetujuan;
	/**
	 * Pengguna yang membuat baris ini, tersimpan sebagai kolom database. Bila baris ini diikat ke
	 * {@link #disposisiSop}, getter {@link #getDibuatOleh()} akan menimpa nilai field ini secara
	 * live dari disposisi terkait — lihat javadoc getter.
	 */
	private Tbmuser dibuatOleh;
	/**
	 * Pengguna yang menyetujui baris ini, tersimpan sebagai kolom database. Bila baris ini diikat
	 * ke {@link #disposisiSop}, getter {@link #getDisetujuiOleh()} akan menimpa nilai field ini
	 * secara live dari disposisi terkait — lihat javadoc getter.
	 */
	private Tbmuser disetujuiOleh;
	/**
	 * Disposisi SOP (alur persetujuan generik AIS) yang diikat ke baris ini; opsional
	 * ({@code nullable = true}). Bila terisi, disposisi inilah yang menjadi sumber kebenaran untuk
	 * {@link #getDibuatOleh()}, {@link #getDisetujuiOleh()}, {@link #getTanggalPembuatan()}, dan
	 * {@link #getTanggalPersetujuan()} — lihat javadoc masing-masing getter.
	 */
	private DisposisiSop disposisiSop;
	/** Satuan kerja pengelola program ini; dipakai juga sebagai filter cakupan pada layar daftar. */
	private SatuanKerja satuanKerja;
	/** Judul/nama program donasi. Wajib diisi, maksimum 255 karakter. */
	private String nama;
	/** Deskripsi/keterangan bebas mengenai program. */
	private String keterangan;
	/** Penanda status aktif program; {@code null} diperlakukan sebagai aktif (lihat {@link #getAktif()}). */
	private Boolean aktif;
	/**
	 * Daftar id {@link Donatur} peserta program ini, disimpan sebagai satu string id dipisah koma
	 * (mis. {@code ",12,45,90,"}) — BUKAN relasi {@code @ManyToMany} terpetakan. Lihat
	 * {@link #getDonaturs()} untuk perilaku normalisasi format pada getter.
	 */
	private String donaturs;
	/** Tautan peta lokasi kegiatan (mis. Google Maps), ditampilkan sebagai hyperlink di UI. */
	private String linkPeta;

	/** Daftar url/lampiran galeri foto program, disimpan sebagai teks JSON array mentah. */
	private String gambars;
	/** Daftar url/lampiran video program, disimpan sebagai teks JSON array mentah. */
	private String videos;
	/** Daftar tautan url eksternal terkait program, disimpan sebagai teks JSON array mentah. */
	private String linkUrl;

	/** Constructor default tanpa argumen, dibutuhkan Hibernate untuk hidrasi entity. */
	public ProgramDonatur() {
	}

	/**
	 * Mengembalikan primary key baris {@code program_donatur}.
	 *
	 * @return primary key, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Tanpa validasi; normalnya hanya diisi oleh Hibernate saat hidrasi atau
	 * saat membuat object "penunjuk" berisi id saja.
	 *
	 * @param id nilai primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode program, dengan normalisasi: string kosong/{@code null} dinormalkan
	 * menjadi {@code null}, selain itu di-{@code trim()}.
	 *
	 * @return kode program yang sudah di-trim, atau {@code null} bila kosong/belum diisi
	 */
	public String getKode() {
		return kode == null || kode.isEmpty() ? null : kode.trim();
	}

	/**
	 * Menyetel kode program. Tanpa validasi/trim pada setter — normalisasi dikerjakan di
	 * {@link #getKode()} setiap kali dibaca.
	 *
	 * @param kode kode program baru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan judul/nama program, di-{@code trim()} bila tidak {@code null}.
	 *
	 * @return nama program yang sudah di-trim, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel judul/nama program. Tanpa validasi/trim pada setter.
	 *
	 * @param nama nama program baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan/deskripsi program apa adanya (tanpa trim).
	 *
	 * @return keterangan program, boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan/deskripsi program.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif program. Mengikuti pola flag aktif umum di AIS: nilai
	 * {@code null} (mis. baris lama sebelum kolom ini ada, atau baris baru yang belum pernah
	 * disetel eksplisit) diperlakukan sebagai <b>aktif</b>, bukan tidak-aktif — "fail open" secara
	 * default. Kolom ini tidak dipetakan lewat anotasi {@code @Column} eksplisit (memakai
	 * penamaan default Hibernate).
	 *
	 * @return {@code true} bila aktif atau belum pernah disetel; {@code false} hanya bila eksplisit
	 *         disetel {@code false}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif program. Tanpa validasi; dipanggil langsung dari checkbox "Aktif" pada
	 * layar daftar ({@code ProgramDonaturAction}) dan langsung disimpan ({@code refreshSaveOrUpdate})
	 * tanpa melalui alur persetujuan/disposisi apa pun — mengubah status aktif tidak memerlukan
	 * hak lebih dari privilese {@code UPDATE} biasa.
	 *
	 * @param aktif status aktif baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan kategori program donasi ini, meresolusi proxy lazy lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} sebelum dikembalikan (pola
	 * standar seluruh getter relasi {@code @ManyToOne} di AIS — lihat javadoc {@code check()} untuk
	 * alasan dan mekanisme lengkapnya).
	 *
	 * @return kategori program yang sudah diresolusi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kategori_program_donatur", nullable = false)
	public KategoriProgramDonatur getKategoriProgramDonatur() {
		kategoriProgramDonatur = check(kategoriProgramDonatur);
		return kategoriProgramDonatur;
	}

	/**
	 * Menyetel kategori program. Tanpa validasi.
	 *
	 * @param kategoriProgramDonatur kategori program baru
	 */
	public void setKategoriProgramDonatur(KategoriProgramDonatur kategoriProgramDonatur) {
		this.kategoriProgramDonatur = kategoriProgramDonatur;
	}

	/**
	 * Menyetel pengguna pembuat baris secara langsung pada field tersimpan. Tanpa validasi.
	 * <b>Catatan penting:</b> nilai yang disetel lewat method ini bisa saja tidak pernah terlihat
	 * oleh pemanggil {@link #getDibuatOleh()} bila baris ini kelak diikat ke {@link #disposisiSop}
	 * yang punya {@code getDisposisiStart()} valid — getter tersebut akan menimpanya. Dipanggil
	 * dari {@code ProgramDonaturAction.onSave()} hanya pada jalur pembuatan baris baru (bersamaan
	 * dengan {@code setTanggalPembuatan(WaktuUtil.getDate())}), memakai {@code Common.getCurrentUser()}
	 * sebagai nilai — jadi pada praktiknya field ini menjadi jejak "siapa yang menekan simpan
	 * pertama kali", terlepas dari apakah kelak ditimpa disposisi atau tidak.
	 *
	 * @param dibuatOleh pengguna pembuat baris
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengembalikan pengguna pembuat baris ini — <b>getter dengan efek samping tersembunyi</b> yang
	 * dapat menimpa nilai field {@link #dibuatOleh} tersimpan.
	 *
	 * <p>Perilaku: field {@link #dibuatOleh} diresolusi lazy lewat {@code check()} lebih dulu
	 * sebagai nilai fallback/default. Kemudian, bila {@link #getDisposisiSop()} tidak {@code null}
	 * DAN disposisi itu punya tahap {@code getDisposisiStart()} yang tidak {@code null} DAN tahap
	 * itu punya {@code getDiajukanOleh()} yang tidak {@code null}, maka nilai field
	 * {@link #dibuatOleh} DITIMPA dengan pengguna yang mengajukan disposisi awal tersebut —
	 * menjadikan disposisi SOP sebagai <b>sumber kebenaran yang lebih diutamakan</b> daripada
	 * kolom {@code dibuat_oleh} milik baris ini sendiri, setiap kali getter ini dipanggil (bukan
	 * hanya sekali saat baris pertama dibuat).</p>
	 *
	 * <p>Konsekuensi praktis: selama baris <b>belum</b> diikat ke disposisi apa pun (kasus paling
	 * umum, karena {@code disposisiSop} bersifat opsional dan pada {@code ProgramDonaturAction}
	 * field lokal {@code disposisiSop} secara eksplisit di-{@code null}-kan sebelum setiap
	 * pemanggilan {@code form(...)} — lihat {@code init(ProgramDonatur)}), getter ini berperilaku
	 * seperti getter biasa: hanya mengembalikan apa yang tersimpan lewat
	 * {@link #setDibuatOleh(Tbmuser)}. Nilai hasil timpaan hanya muncul pada alur yang benar-benar
	 * memanggil {@code form(obj, disposisiSop, ...)} dengan disposisi valid — yaitu dasbor SOP
	 * generik yang mengorkestrasi alur persetujuan lintas modul, bukan {@code ProgramDonaturAction}
	 * itu sendiri.</p>
	 *
	 * <p><b>Penanganan kegagalan lazy-init:</b> seluruh logika penimpaan di atas dibungkus
	 * {@code try/catch} dengan komentar eksplisit di kode: {@link #disposisiSop} bisa berupa
	 * instance canonical/shared (hasil {@link ais.database.hibernate.AuditTimestampInterceptor})
	 * yang proxy relasinya (mis. {@code getDisposisiStart()}) terikat ke {@link org.hibernate.Session}
	 * lain yang sudah tertutup. Alih-alih membiarkan getter ini melempar
	 * {@code LazyInitializationException} ke pemanggil (yang lazimnya kode render UI ZK, sehingga
	 * satu baris bermasalah bisa merusak seluruh render layar daftar), exception ditelan dan
	 * dicatat ke {@link ais.common.ErrorAuditUtil}, dan nilai fallback ({@link #dibuatOleh}
	 * hasil {@code check()} di awal method) tetap dipertahankan/dikembalikan. Ini adalah pola
	 * "fail-soft" yang disengaja: keutuhan layar UI diprioritaskan di atas akurasi sesaat kolom
	 * audit ini pada kondisi proxy detached yang jarang terjadi.</p>
	 *
	 * @return pengguna pembuat baris — hasil timpaan dari disposisi bila tersedia, atau nilai
	 *         kolom {@code dibuat_oleh} tersimpan sebagai fallback
	 * @see #getDisposisiSop()
	 * @see #getDisetujuiOleh()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = false)
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);
		try {
			// FIX LazyInitializationException: disposisiSop bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
					&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
				dibuatOleh = getDisposisiSop().getDisposisiStart().getDiajukanOleh();
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/sosial/ProgramDonatur.java:getDibuatOleh-lazy");
		}
		return dibuatOleh;
	}

	/**
	 * Mengembalikan pengguna yang menyetujui baris ini — <b>gerbang persetujuan program dilihat
	 * dari sisi model data</b>; getter ini adalah titik paling penting untuk memahami bagaimana
	 * status "disetujui" program donasi legacy sesungguhnya ditentukan.
	 *
	 * <p>Perilaku: field {@link #disetujuiOleh} diresolusi lazy dulu sebagai fallback. Lalu, bila
	 * {@link #getDisposisiSop()} tidak {@code null} dan tahap {@code getDisposisiSetuju()}-nya
	 * tidak {@code null} dan tahap itu punya {@code getDiajukanOleh()} tidak {@code null}, field
	 * DITIMPA dengan pengguna yang menyetujui tahap tersebut pada disposisi. Sebaliknya — dan ini
	 * bagian yang berbeda dari {@link #getDibuatOleh()} — bila disposisi ADA tetapi tahap setuju
	 * BELUM terisi (belum ada yang menyetujui, atau {@code getDiajukanOleh()} kosong), field
	 * DITIMPA MENJADI {@code null} secara eksplisit, bukan dibiarkan memakai nilai kolom lama.
	 * Artinya: begitu satu baris diikat ke disposisi, status "siapa yang menyetujui" sepenuhnya
	 * dan secara ketat mengikuti keadaan disposisi terkini — nilai kolom {@code disetujui_oleh}
	 * lama tidak bisa "nyangkut" dari persetujuan sebelumnya bila disposisi baru belum disetujui.</p>
	 *
	 * <p><b>Gerbang persetujuan yang sesungguhnya berlaku:</b> perlu ditekankan bahwa getter ini
	 * (dan {@link #getTanggalPersetujuan()}) hanya <i>mencerminkan</i> status persetujuan disposisi
	 * SOP — ia BUKAN mekanisme yang MENCEGAH operasi apa pun. Tidak ada kode di
	 * {@code ProgramDonaturAction} maupun kelas ini yang menolak {@code onSave()}/aktivasi program
	 * bila {@code getDisetujuiOleh() == null}; nilai ini murni informasional/tampilan (ditampilkan
	 * di kolom cetak laporan dan pada renderer daftar). Penegakan alur persetujuan yang
	 * sesungguhnya (bila ada) terjadi di lapisan dasbor SOP generik yang mengelola
	 * {@link ais.database.model.sop.DisposisiSop} itu sendiri (di luar paket ini), bukan di
	 * entitas atau action donasi. Karena entitas ini tidak membawa nominal dana, tidak relevan
	 * membicarakan "gerbang penyaluran dana" pada level program — lihat javadoc kelas.</p>
	 *
	 * <p>Sama seperti {@link #getDibuatOleh()}, seluruh logika di atas dibungkus {@code try/catch}
	 * untuk menahan {@code LazyInitializationException} akibat instance {@link #disposisiSop}
	 * canonical/shared yang proxy relasinya terikat ke session lain yang sudah tertutup;
	 * kegagalan dicatat ke {@link ais.common.ErrorAuditUtil} dan nilai fallback dipertahankan.</p>
	 *
	 * @return pengguna penyetuju — hasil timpaan dari disposisi (termasuk ditimpa {@code null} bila
	 *         disposisi ada tapi belum disetujui), atau nilai kolom tersimpan bila tidak ada
	 *         disposisi terikat
	 * @see #getDibuatOleh()
	 * @see #getTanggalPersetujuan()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujuiOleh = check(disetujuiOleh);

		try {
			// FIX LazyInitializationException: disposisiSop bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
					&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
				disetujuiOleh = getDisposisiSop().getDisposisiSetuju().getDiajukanOleh();
			}

			if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
					|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
				disetujuiOleh = null;
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/sosial/ProgramDonatur.java:getDisetujuiOleh-lazy");
		}
		return disetujuiOleh;
	}

	/**
	 * Menyetel tanggal persetujuan pada field tersimpan. Tanpa validasi. Sama seperti
	 * {@link #setDibuatOleh(Tbmuser)}, nilai yang disetel di sini bisa ditimpa oleh
	 * {@link #getTanggalPersetujuan()} bila baris kelak diikat ke disposisi.
	 *
	 * @param tanggalPersetujuan tanggal persetujuan baru
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengembalikan tanggal persetujuan — mengikuti pola timpa-dari-disposisi yang sama persis
	 * dengan {@link #getDisetujuiOleh()}: bila disposisi punya tahap setuju yang diajukan, tanggal
	 * DITIMPA dengan {@code getDisposisiSetuju().getWaktu()}; bila disposisi ada tapi tahap setuju
	 * belum terisi, tanggal DITIMPA MENJADI {@code null}. Tanpa disposisi terikat, nilai kolom
	 * tersimpan dikembalikan apa adanya. Kegagalan lazy-init pada proxy disposisi ditelan dan
	 * dicatat ke {@link ais.common.ErrorAuditUtil}, mempertahankan nilai fallback — lihat
	 * penjelasan lengkap pola ini di {@link #getDisetujuiOleh()}.
	 *
	 * @return tanggal persetujuan efektif, bisa {@code null}
	 * @see #getDisetujuiOleh()
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {

		try {
			// FIX LazyInitializationException: disposisiSop bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
					&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
				tanggalPersetujuan = getDisposisiSop().getDisposisiSetuju().getWaktu();
			}

			if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
					|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
				tanggalPersetujuan = null;
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/sosial/ProgramDonatur.java:getTanggalPersetujuan-lazy");
		}
		return tanggalPersetujuan;
	}

	/**
	 * Menyetel pengguna penyetuju pada field tersimpan. Tanpa validasi. Nilai ini tidak dipanggil
	 * dari {@code ProgramDonaturAction} pada berkas ini (tidak ada alur "tombol setujui" lokal);
	 * kemungkinan hanya dipanggil dari dasbor SOP generik saat memproses disposisi.
	 *
	 * @param disetujuiOleh pengguna penyetuju baru
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Menyetel tanggal pembuatan pada field tersimpan. Tanpa validasi. Dipanggil dari
	 * {@code ProgramDonaturAction.onSave()} dengan {@code WaktuUtil.getDate()} hanya pada jalur
	 * pembuatan baris baru (bukan pembaruan).
	 *
	 * @param tanggalPembuatan tanggal pembuatan baru
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Mengembalikan tanggal pembuatan — mengikuti pola timpa-dari-disposisi yang sama dengan
	 * {@link #getDibuatOleh()}: bila disposisi punya tahap awal ({@code getDisposisiStart()}) yang
	 * diajukan, tanggal DITIMPA dengan waktu tahap tersebut. Berbeda dari
	 * {@link #getTanggalPersetujuan()}, di sini TIDAK ada cabang yang menimpa menjadi {@code null}
	 * ketika tahap awal belum terisi — nilai kolom tersimpan tetap dipertahankan pada kondisi itu.
	 * Selain itu, bila hasil akhirnya tetap {@code null} (baik dari kolom maupun dari disposisi),
	 * method mengembalikan {@link ais.ui.util.WaktuUtil#getDate()} (waktu saat ini) sebagai
	 * fallback terakhir alih-alih {@code null} — sehingga getter ini, tidak seperti
	 * {@link #getTanggalPersetujuan()}, dijamin tidak pernah mengembalikan {@code null}. Kegagalan
	 * lazy-init pada proxy disposisi ditelan dan dicatat ke {@link ais.common.ErrorAuditUtil}.
	 *
	 * @return tanggal pembuatan efektif; tidak pernah {@code null}
	 * @see #getDibuatOleh()
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		try {
			// FIX LazyInitializationException: disposisiSop bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
					&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
				tanggalPembuatan = getDisposisiSop().getDisposisiStart().getWaktu();
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/sosial/ProgramDonatur.java:getTanggalPembuatan-lazy");
		}

		return tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan;
	}

	/**
	 * Mengembalikan disposisi SOP yang diikat ke baris ini, meresolusi proxy lazy lewat
	 * {@code check()}.
	 *
	 * @return disposisi terikat, atau {@code null} bila baris ini belum/tidak diikat ke disposisi
	 *         apa pun
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Mengikat baris ini ke sebuah disposisi SOP, dengan guard: argumen {@code null} atau disposisi
	 * yang belum tersimpan ({@code getId() == null}) diabaikan diam-diam (method langsung
	 * {@code return}). Konsekuensinya: disposisi yang sudah terikat <b>tidak pernah bisa dilepas</b>
	 * lewat setter ini (tidak ada jalur untuk menyetelnya kembali ke {@code null}).
	 *
	 * <p><b>Catatan kode:</b> baris kedua method ini (ekspresi ternary yang membandingkan
	 * {@code this.disposisiSop} dengan argumen) adalah <b>kode mati/berlebihan</b> — pada titik ini
	 * guard di baris pertama sudah menjamin {@code disposisiSop} bukan {@code null} dan punya id,
	 * sehingga cabang kondisi ternary yang mempertahankan {@code this.disposisiSop} lama tidak
	 * pernah bisa tercapai; hasil akhirnya method ini selalu berperilaku sebagai
	 * {@code this.disposisiSop = disposisiSop;} bila lolos guard. Pola identik (guard early-return
	 * lalu ternary yang sama) juga muncul apa adanya di {@link PenyaluranDonasi#setDisposisiSop},
	 * mengindikasikan keduanya berasal dari templat/salinan yang sama.</p>
	 *
	 * @param disposisiSop disposisi baru; diabaikan bila {@code null} atau belum tersimpan
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {if(disposisiSop==null||disposisiSop.getId()==null) {return;}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
	}

	/**
	 * Mengembalikan satuan kerja pengelola program ini, meresolusi proxy lazy lewat {@code check()}.
	 *
	 * @return satuan kerja pengelola, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Menyetel satuan kerja pengelola program. Tanpa validasi.
	 *
	 * @param satuanKerja satuan kerja baru
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Mengembalikan daftar id donatur peserta program, sebagai string dipisah koma — <b>getter
	 * dengan efek samping</b>: setiap pemanggilan menormalkan ULANG format field {@link #donaturs}
	 * dan MENULISKAN hasilnya kembali ke field tersebut (bukan sekadar membaca).
	 *
	 * <p>Alur normalisasi: nilai kosong/{@code null}/hanya berisi {@code ","} dianggap kosong
	 * ({@code ""}); selain itu field dibungkus koma di kedua ujung ({@code ",<isi>,"}) lalu koma
	 * ganda dilipat berulang ({@code replaceAll(",,", ",")} dipanggil tiga kali berturut-turut —
	 * cukup untuk melipat hingga 8 koma berurutan menjadi satu, pola penanganan yang rapuh/berulang
	 * alih-alih memakai regex {@code ",+"} sekali jalan). Hasil yang berupa kombinasi 1-4 koma
	 * murni ({@code ","}, {@code ",,"}, {@code ",,,"}, {@code ",,,,"}) dinormalkan menjadi string
	 * kosong. Nilai akhir (setelah di-{@code trim()}) inilah yang disimpan kembali ke field DAN
	 * yang dikembalikan ke pemanggil — panggilan getter berikutnya akan bekerja di atas hasil yang
	 * sudah dinormalkan, bukan nilai asli sebelum getter pertama dipanggil.</p>
	 *
	 * <p>Format ini (id dipisah koma pada kolom teks, bukan tabel relasi {@code @ManyToMany} atau
	 * {@code @ElementCollection}) dipakai luas di {@code ProgramDonaturAction}/
	 * {@code PenyaluranDonasiAction} untuk memuat daftar {@link Donatur} terkait lewat
	 * {@code Restrictions.in("id", ids)} — parsing id dari string ini di action tidak melalui
	 * validasi ketat (kegagalan {@code Long.parseLong} pada satu token ditelan lewat
	 * {@code catch} kosong dan token itu dilewati begitu saja, bukan menggagalkan seluruh proses).</p>
	 *
	 * @return daftar id donatur berformat {@code ",id1,id2,...,"} yang sudah dinormalkan, atau
	 *         string kosong bila tidak ada donatur terkait; tidak pernah {@code null}
	 */
	@Column(name = "donaturs", nullable = true, columnDefinition = "text")
	public String getDonaturs() {
		donaturs = (donaturs == null || donaturs.trim().equalsIgnoreCase(",") ? "" : "," + donaturs.trim() + ",")
				.replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (donaturs.equals(",")) {
			donaturs = "";
		} else if (donaturs.equals(",,")) {
			donaturs = "";
		} else if (donaturs.equals(",,,")) {
			donaturs = "";
		} else if (donaturs.equals(",,,,")) {
			donaturs = "";
		}
		return donaturs == null ? "" : donaturs.trim();
	}

	/**
	 * Menyetel daftar id donatur mentah (tanpa normalisasi pada setter ini — normalisasi
	 * dikerjakan {@link #getDonaturs()} saat dibaca).
	 *
	 * @param donaturs daftar id donatur baru, format bebas (akan dinormalkan saat dibaca)
	 */
	public void setDonaturs(String donaturs) {
		this.donaturs = donaturs;
	}

	/**
	 * Mengembalikan tanggal mulai kegiatan/program apa adanya.
	 *
	 * @return tanggal mulai, boleh {@code null}
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalMulai() {
		return tanggalMulai;
	}

	/**
	 * Menyetel tanggal mulai kegiatan/program.
	 *
	 * @param tanggalMulai tanggal mulai baru
	 */
	public void setTanggalMulai(Date tanggalMulai) {
		this.tanggalMulai = tanggalMulai;
	}

	/**
	 * Mengembalikan tanggal berakhir kegiatan/program apa adanya.
	 *
	 * @return tanggal berakhir, boleh {@code null}
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalSampai() {
		return tanggalSampai;
	}

	/**
	 * Menyetel tanggal berakhir kegiatan/program.
	 *
	 * @param tanggalSampai tanggal berakhir baru
	 */
	public void setTanggalSampai(Date tanggalSampai) {
		this.tanggalSampai = tanggalSampai;
	}

	/** Representasi teks array JSON kosong ({@code "[]"}), dipakai sebagai nilai fallback oleh
	 * {@link #getGambars()}, {@link #getVideos()}, dan {@link #getLinkUrl()} ketika kolomnya
	 * belum diisi — memastikan konsumer di sisi klien selalu menerima JSON array valid, bukan
	 * {@code null} atau string kosong yang gagal di-parse. */
	private static String T = new JSONArray().toString();

	/**
	 * Mengembalikan daftar galeri foto program sebagai teks JSON array mentah (tidak divalidasi
	 * skema di sini), atau {@link #T} ({@code "[]"}) bila belum diisi.
	 *
	 * @return teks JSON array foto, tidak pernah {@code null}/kosong
	 */
	@Column(name = "gambars", nullable = true, columnDefinition = "text")
	public String getGambars() {
		return gambars == null || gambars.isEmpty() ? T : gambars;
	}

	/**
	 * Menyetel daftar galeri foto (teks JSON array mentah).
	 *
	 * @param gambars teks JSON array foto baru
	 */
	public void setGambars(String gambars) {
		this.gambars = gambars;
	}

	/**
	 * Mengembalikan daftar video program sebagai teks JSON array mentah, atau {@link #T}
	 * ({@code "[]"}) bila belum diisi.
	 *
	 * @return teks JSON array video, tidak pernah {@code null}/kosong
	 */
	@Column(name = "videos", nullable = true, columnDefinition = "text")
	public String getVideos() {
		return videos == null || videos.isEmpty() ? T : videos;
	}

	/**
	 * Menyetel daftar video (teks JSON array mentah).
	 *
	 * @param videos teks JSON array video baru
	 */
	public void setVideos(String videos) {
		this.videos = videos;
	}

	/**
	 * Mengembalikan daftar tautan url eksternal sebagai teks JSON array mentah, atau {@link #T}
	 * ({@code "[]"}) bila belum diisi.
	 *
	 * @return teks JSON array tautan, tidak pernah {@code null}/kosong
	 */
	@Column(name = "link_url", nullable = true, columnDefinition = "text")
	public String getLinkUrl() {
		return linkUrl == null || linkUrl.isEmpty() ? T : linkUrl;
	}

	/**
	 * Menyetel daftar tautan url eksternal (teks JSON array mentah).
	 *
	 * @param linkUrl teks JSON array tautan baru
	 */
	public void setLinkUrl(String linkUrl) {
		this.linkUrl = linkUrl;
	}

	/**
	 * Mengembalikan tautan peta lokasi, di-{@code trim()}; string kosong dikembalikan (bukan
	 * {@code null}) bila belum diisi — berbeda dari {@link #getKode()} yang menormalkan kosong
	 * menjadi {@code null}.
	 *
	 * @return tautan peta lokasi yang sudah di-trim, tidak pernah {@code null}
	 */
	@Column(name = "link_peta", nullable = true, columnDefinition = "text")
	public String getLinkPeta() {
		return linkPeta == null ? "" : linkPeta.trim();
	}

	/**
	 * Menyetel tautan peta lokasi. Tanpa validasi/trim pada setter.
	 *
	 * @param linkPeta tautan peta lokasi baru
	 */
	public void setLinkPeta(String linkPeta) {
		this.linkPeta = linkPeta;
	}
}
