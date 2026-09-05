package ais.database.model.kpi;

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

import ais.common.Common;
import ais.database.model.Tbmuser;
import ais.database.model.asset.NomorSuratAlurPengadaan;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * Entitas pengajuan (usulan) KPI (Key Performance Indicator) pegawai per satuan kerja dan tahun
 * akademik/anggaran ({@link #getTa()}). Satu {@code PengajuanKpi} membungkus sekumpulan baris
 * {@link PenilaianKpi} (dirujuk lewat kolom CSV {@link #getPenilaianKpis()}, satu per pegawai) yang
 * dinilai bersama-sama dalam satu paket pengajuan; pengajuan itu sendiri yang melalui alur
 * persetujuan atasan/pejabat berwenang sebelum nilai KPI di dalamnya dianggap final dan dipakai oleh
 * {@link PenilaianKpi#hitungKpi(org.hibernate.Session, ais.database.model.Pegawai, Date)}.
 *
 * <p><b>TEMUAN ARSITEKTUR — dua jalur persetujuan yang berdampingan dan TIDAK SINKRON:</b></p>
 * <p>Kelas ini mewarisi {@link DataSop} dan memiliki relasi opsional ke {@link DisposisiSop} (mesin
 * alur/SOP generik paket {@code ais.database.model.sop}, dipakai lintas banyak domain lain di AIS).
 * Ketika {@link #getDisposisiSop()} terisi, getter-getter warisan konsep SOP di kelas ini —
 * {@link #getDibuatOleh()}, {@link #getDisetujuiOleh()}, {@link #getTanggalPembuatan()},
 * {@link #getTanggalPersetujuan()}, {@link #getAktif()} — MENCOBA menurunkan (derive) nilai yang
 * ditampilkan langsung dari state {@code DisposisiSop}/{@code AlurSop} pada saat getter dipanggil,
 * BUKAN dari kolom mentah tabel {@code employ.pengajuan_kpi}. Inilah jalur "mesin SOP generik" yang
 * saat ini sedang diinvestigasi mendalam secara terpisah oleh sesi lain (audit {@code AlurSop}/
 * {@code DisposisiSop}); dokumentasi di kelas ini TIDAK menduplikasi audit tersebut.</p>
 * <p>Namun demikian, {@code ais.action.master.kpi.PengajuanKpiAction} (layar daftar/tabel pengajuan
 * KPI) memiliki tombol Setujui/Tolak/Batalkan SENDIRI pada setiap baris yang memanggil
 * {@link #setDisetujuiOleh(Tbmuser)}, {@link #setTanggalPersetujuan(Date)}, {@link
 * #setDitolakOleh(Tbmuser)}, dan {@link #setTanggalDitolak(Date)} SECARA LANGSUNG di dalam listener
 * {@code onClick}-nya, dan mempersistennya lewat {@code Common.refreshUpdate(...)} — tanpa pernah
 * membuat, membaca, atau menyentuh {@link DisposisiSop}/{@code AlurSop} sama sekali. Jalur kedua ini
 * adalah alur persetujuan MANDIRI (independen dari mesin SOP generik) yang hidup berdampingan dengan
 * jalur pertama pada entitas dan kolom database yang sama.</p>
 * <p>Visibilitas tombol Setujui/Tolak pada jalur mandiri tersebut memang dihitung dari privilese
 * sisi-server ({@code CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE/REJECT)}, dibaca satu
 * kali di {@code doAfterCompose()} lalu disimpan sebagai field boolean composer), tetapi listener
 * {@code onClick} tombol Setujui/Tolak/Batalkan TIDAK memverifikasi ulang privilese tersebut sebelum
 * memanggil setter persetujuan/penolakan — flag privilese hanya dipakai untuk {@code setVisible(...)}
 * pada tombol (mengendalikan tampilan), bukan untuk mengunci mutasi di dalam handler mutasi itu
 * sendiri. Ini persis pola "bypass-persetujuan UI-only" (gerbang otorisasi hanya mengatur apa yang
 * TERLIHAT pada layar, bukan apa yang BOLEH DIEKSEKUSI) yang sudah dikonfirmasi berulang di 5 domain
 * independen lain (kepegawaian, persuratan x2, penelitian, rekrutmen) — modul KPI ini menjadi
 * konfirmasi domain KEENAM, dan alur mandirinya independen dari investigasi {@code AlurSop} yang
 * sedang berjalan sehingga dicatat sebagai temuan baru tersendiri.</p>
 * <p>Konsekuensi praktis dari dua jalur yang tidak sinkron: jalur SOP-generik hanya MENURUNKAN nilai
 * {@code disetujuiOleh}/{@code tanggalPersetujuan} secara dinamis pada getter tanpa pernah memanggil
 * setter-nya atau menyimpan ke kolom mentah, sedangkan jalur tombol mandiri MEMANGGIL setter dan
 * benar-benar MEMPERSISTEN kolom mentah {@code disetujui_oleh}/{@code tanggal_persetujuan}. Akibatnya,
 * query Hibernate Criteria yang menyaring langsung berdasar kolom mentah tersebut — misalnya
 * {@link PenilaianKpi#hitungKpi(org.hibernate.Session, ais.database.model.Pegawai, Date)} yang memakai
 * {@code Restrictions.isNotNull("pengajuanKpi.disetujuiOleh")} — TIDAK AKAN menemukan pengajuan yang
 * "disetujui" hanya lewat jalur SOP-generik (karena kolom mentahnya tidak pernah benar-benar ditulis),
 * hanya pengajuan yang disetujui lewat tombol mandiri yang datanya benar-benar tersimpan di kolom itu
 * yang akan cocok. Ini berpotensi membuat perhitungan KPI pegawai yang "disetujui" lewat alur SOP
 * jatuh diam-diam ke nilai default ({@code NilaiDefaultKpi}) alih-alih nilai penilaian sebenarnya —
 * lihat catatan lebih lanjut pada javadoc {@link #getDisetujuiOleh()} dan pada {@code hitungKpi}.</p>
 *
 * Bank generated by hbm2java
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "employ", name = "pengajuan_kpi")
public class PengajuanKpi extends DataSop {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Primary key numerik, digenerasi database (identity). */
	private Long id;

	/** Field audit shadow legacy: penanda "oleh" (username/label pembuat perubahan) — bukan relasi FK, hanya string bebas untuk jejak audit generik yang dipakai lintas entitas AIS (keharusan teknis pola audit, bukan bug). */
	private String oleh;

	/** Field audit shadow legacy: id pengguna (string) pasangan dari {@link #oleh}, dipertahankan agar payload audit tetap kompatibel dengan konsumen lama. */
	private String olehId;

	/**
	 * Mengambil id pengguna (dalam bentuk {@link String}, bukan relasi FK) yang tercatat sebagai
	 * pelaku perubahan terakhir pada baris audit shadow ini. Field ini terpisah dari relasi resmi
	 * {@link #getDibuatOleh()}/{@link #getDisetujuiOleh()}/{@link #getDitolakOleh()} dan hanya
	 * dipakai untuk kebutuhan jejak audit generik/legacy.
	 *
	 * @return id pengguna sebagai string, bisa {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi {@link #olehId}. Nilai {@code null} atau string kosong/berisi spasi saja SENGAJA
	 * diabaikan (early return tanpa mengubah field) sehingga nilai lama yang sudah tersimpan tidak
	 * pernah tertimpa oleh nilai kosong — pola "write-once/keep-last-good" yang umum dipakai pada
	 * field audit shadow di banyak entitas AIS agar jejak audit tidak hilang akibat pemanggilan
	 * setter dengan payload kosong.
	 *
	 * @param olehId id pengguna baru; diabaikan bila {@code null} atau kosong setelah di-trim
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Representasi string ringkas entitas ini untuk ditampilkan pada komponen UI (mis. combobox,
	 * label riwayat revisi) yang hanya mengharapkan satu baris teks.
	 *
	 * @return nilai {@link #getKeterangan()} apa adanya (bisa {@code null})
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Mengisi {@link #oleh}. Sama seperti {@link #setOlehId(String)}, nilai {@code null} atau
	 * kosong/berisi spasi saja diabaikan agar nilai audit yang sudah ada tidak tertimpa kosong.
	 *
	 * @param oleh label pelaku perubahan; diabaikan bila {@code null} atau kosong setelah di-trim
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil label pelaku perubahan terakhir (audit shadow legacy).
	 *
	 * @return nilai {@link #oleh}, bisa {@code null}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil otomatis oleh provider persistence tepat sebelum
	 * UPDATE dieksekusi, mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}
	 * untuk memperbarui field {@link #tanggal_dirubah} milik entitas ini ke waktu saat ini. Ini
	 * adalah mekanisme audit timestamp standar yang dipakai seragam di banyak entitas AIS.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi waktu perubahan terakhir. Biasanya hanya dipanggil oleh mekanisme audit
	 * ({@link #onUpdate()}) atau proses restorasi data, bukan oleh kode aplikasi biasa.
	 *
	 * @param tanggal_dirubah waktu perubahan baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil waktu perubahan terakhir pada baris ini, diinisialisasi ke waktu saat objek dibuat
	 * di memori dan diperbarui otomatis oleh {@link #onUpdate()} setiap kali baris di-UPDATE.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Judul/nama pengajuan KPI, diisi bebas oleh pembuat (mis. "Pengajuan KPI Semester Ganjil 2026"). */
	private String nama;

	/** Kode tahun akademik/anggaran pengajuan ini; lihat {@link #getTa()} untuk fallback bila kosong. */
	private String ta;

	/** Deskripsi/catatan bebas terkait pengajuan, juga dipakai sebagai representasi {@link #toString()}. */
	private String keterangan;

	/** Kolom mentah tanggal pembuatan pengajuan; lihat {@link #getTanggalPembuatan()} untuk logika derive dari {@link DisposisiSop}. */
	private Date tanggalPembuatan;

	/** Kolom mentah tanggal persetujuan; lihat {@link #getTanggalPersetujuan()} untuk logika derive dari {@link DisposisiSop} dan penolakan yang membatalkannya. */
	private Date tanggalPersetujuan;

	/** Kolom mentah relasi pengguna pembuat pengajuan; lihat {@link #getDibuatOleh()} untuk logika derive dari {@link DisposisiSop}. */
	private Tbmuser dibuatOleh;

	/** Kolom mentah relasi pengguna yang menyetujui pengajuan (jalur mandiri di {@code PengajuanKpiAction}); lihat {@link #getDisetujuiOleh()} untuk logika derive/override dari {@link DisposisiSop} dan penolakan. */
	private Tbmuser disetujuiOleh;

	/** Satuan kerja pemilik/pengaju KPI ini — batas kepemilikan/tenant data pengajuan. */
	private SatuanKerja satuanKerja;

	/** Relasi opsional ke node disposisi pada mesin alur/SOP generik ({@code ais.database.model.sop}); bila terisi, sejumlah getter di kelas ini menurunkan nilainya dari sini alih-alih dari kolom mentah masing-masing. */
	private DisposisiSop disposisiSop;

	/** Daftar id {@link PenilaianKpi} anggota pengajuan ini, disimpan sebagai string CSV (bukan relasi one-to-many formal); lihat {@link #getPenilaianKpis()} untuk logika normalisasi separator koma. */
	private String penilaianKpis;

	/** Konstruktor default kosong, dibutuhkan oleh Hibernate untuk instansiasi entitas via reflection. */
	public PengajuanKpi() {
	}

	/**
	 * Mengambil primary key baris ini.
	 *
	 * @return id numerik, {@code null} bila entitas belum pernah disimpan (transient)
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi id. Kolom database bersifat {@code insertable = false} (nilai identity digenerasi
	 * database saat INSERT), sehingga setter ini pada praktiknya hanya dipakai Hibernate saat
	 * memuat baris dari database, bukan oleh kode aplikasi untuk menentukan id baru.
	 *
	 * @param id nilai id baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil deskripsi/catatan pengajuan.
	 *
	 * @return teks keterangan, bisa {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi deskripsi/catatan pengajuan.
	 *
	 * @param keterangan teks keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengisi kolom mentah pembuat pengajuan. Dipanggil aplikasi (mis. saat {@code onSave()} untuk
	 * pengajuan baru) untuk mencatat pembuat awal; nilai ini dapat kemudian ditimpa secara dinamis
	 * oleh getter {@link #getDibuatOleh()} bila pengajuan terhubung ke {@link DisposisiSop}.
	 *
	 * @param dibuatOleh pengguna pembuat pengajuan
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengambil pengguna yang membuat/mengajukan KPI ini.
	 *
	 * <p>Bila pengajuan ini terhubung ke node {@link DisposisiSop} milik mesin alur/SOP generik
	 * (lihat {@link #getDisposisiSop()}) dan disposisi tersebut memiliki tahap "start" dengan
	 * pengaju tercatat, nilai kolom mentah {@link #dibuatOleh} ditimpa (di memori, tidak
	 * dipersisten kembali oleh getter ini) dengan pengaju dari disposisi tersebut — sehingga bila
	 * pengajuan diproses lewat mesin SOP generik, tampilan "dibuat oleh" selalu mengikuti data
	 * disposisi terbaru, bukan snapshot yang disimpan saat baris dibuat.</p>
	 * <p>Seluruh akses ke {@link #getDisposisiSop()} dan turunannya dibungkus {@code try/catch}
	 * karena instance {@code DisposisiSop} yang dipegang bisa berupa instance kanonikal/shared
	 * (dipakai ulang oleh {@code AuditTimestampInterceptor}) yang proxy Hibernate-nya terikat ke
	 * {@code Session} lain yang sudah tertutup; jika terjadi {@code LazyInitializationException}
	 * atau error sejenis, exception ditelan (dicatat ke {@link ais.common.ErrorAuditUtil}) dan
	 * getter tetap mengembalikan nilai fallback (kolom mentah) alih-alih membuat layar gagal total
	 * hanya karena gagal menampilkan satu label turunan.</p>
	 *
	 * @return pengguna pembuat pengajuan; bisa {@code null} bila belum pernah diisi dan tidak ada
	 *         disposisi SOP dengan data pengaju
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = true)
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
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/kpi/PengajuanKpi.java:getDibuatOleh-lazy");
		}

		return dibuatOleh;
	}

	/**
	 * Mengisi kolom mentah penyetuju pengajuan. Dipanggil langsung oleh jalur persetujuan mandiri
	 * di {@code ais.action.master.kpi.PengajuanKpiAction} (tombol Setujui/Batalkan pada daftar
	 * pengajuan) — lihat catatan keamanan pada javadoc kelas dan pada {@link #getDisetujuiOleh()}.
	 * Tidak ada validasi/normalisasi di setter ini; pemanggil bertanggung jawab memastikan hanya
	 * dipanggil untuk pengguna yang berwenang.
	 *
	 * @param disetujuiOleh pengguna penyetuju, atau {@code null} untuk membatalkan persetujuan
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengambil pengguna yang menyetujui pengajuan KPI ini — inti gerbang persetujuan entitas ini.
	 *
	 * <p><b>Dua jalur yang saling tumpang tindih:</b> nilai kolom mentah {@link #disetujuiOleh}
	 * awalnya diisi langsung oleh jalur persetujuan MANDIRI (tombol Setujui pada
	 * {@code ais.action.master.kpi.PengajuanKpiAction}, yang memanggil
	 * {@link #setDisetujuiOleh(Tbmuser)} lalu mempersistennya). Namun getter ini SELALU MENCOBA
	 * menimpa nilai tersebut (hanya di memori, tidak menulis balik ke database) dengan nilai yang
	 * diturunkan dari {@link #getDisposisiSop()} bila node disposisi tersebut punya tahap "setuju"
	 * ({@code getDisposisiSetuju()}) dengan pengaju tercatat — dan sebaliknya, bila disposisi ADA
	 * tetapi tahap "setuju"-nya BELUM ada/belum punya pengaju, nilai secara eksplisit di-null-kan
	 * (baris {@code if (... == null || ... == null) { disetujuiOleh = null; }}), yang berarti
	 * persetujuan yang sudah tersimpan lewat jalur mandiri BISA "hilang" dari tampilan begitu
	 * pengajuan ini juga dikaitkan ke sebuah {@link DisposisiSop} yang belum disetujui pada mesin
	 * SOP — dua sumber kebenaran (jalur mandiri vs jalur SOP) dapat saling menimpa secara tidak
	 * terduga tergantung urutan pemanggilan/keterkaitan data.</p>
	 * <p>Akses ke {@link #getDisposisiSop()} dan turunannya dibungkus {@code try/catch} dengan
	 * alasan yang sama seperti {@link #getDibuatOleh()} (proxy Hibernate lintas-session yang bisa
	 * lazy-init exception); kegagalan ditelan dan dicatat ke {@link ais.common.ErrorAuditUtil},
	 * nilai fallback dipertahankan.</p>
	 * <p>Setelah logika di atas, ada satu override tambahan: bila {@link #getDitolakOleh()} tidak
	 * {@code null} (pengajuan sudah ditolak lewat jalur mandiri), maka {@code disetujuiOleh}
	 * dipaksa menjadi {@code null} apa pun hasil sebelumnya — penolakan selalu menang atas
	 * persetujuan pada representasi yang ditampilkan, mencegah baris yang tampak "disetujui DAN
	 * ditolak" sekaligus di UI.</p>
	 * <p><b>Dampak pada perhitungan skor:</b> karena getter ini HANYA mengubah nilai di memori
	 * (tidak pernah menulis balik kolom {@code disetujui_oleh} di database), sedangkan
	 * {@link PenilaianKpi#hitungKpi(org.hibernate.Session, ais.database.model.Pegawai, Date)}
	 * menyaring lewat Hibernate Criteria langsung terhadap kolom mentah tersebut
	 * ({@code Restrictions.isNotNull("pengajuanKpi.disetujuiOleh")}), maka: (a) pengajuan yang
	 * "tampak" disetujui di UI semata-mata karena diturunkan dari {@code DisposisiSop} — tanpa
	 * kolom mentahnya pernah benar-benar ditulis lewat {@link #setDisetujuiOleh(Tbmuser)} — TIDAK
	 * akan pernah cocok dengan query tersebut, sehingga skor KPI-nya tidak pernah ditemukan dan
	 * jatuh ke nilai default; dan (b) pengajuan yang kolom mentahnya sudah terisi lewat jalur
	 * mandiri tetapi kemudian dikaitkan ke {@code DisposisiSop} yang belum/tidak disetujui akan
	 * TETAP cocok dengan query database (karena query membaca kolom mentah, bukan getter ini),
	 * walau tampilan UI-nya sendiri sudah menunjukkan "belum disetujui". Kedua arah ketidaksinkronan
	 * ini adalah konsekuensi langsung dari mencampur derive-di-getter dengan query-di-kolom-mentah
	 * pada satu field yang sama.</p>
	 *
	 * @return pengguna penyetuju efektif untuk ditampilkan; {@code null} bila belum disetujui atau
	 *         bila sudah ditolak
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
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/kpi/PengajuanKpi.java:getDisetujuiOleh-lazy");
		}

		if (getDitolakOleh() != null) {
			disetujuiOleh = null;
		}

		return disetujuiOleh;
	}

	/**
	 * Mengisi kolom mentah tanggal persetujuan. Umumnya dipanggil bersamaan dengan
	 * {@link #setDisetujuiOleh(Tbmuser)} pada jalur persetujuan mandiri.
	 *
	 * @param tanggalPersetujuan waktu persetujuan baru, atau {@code null} untuk membatalkannya
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengambil waktu persetujuan pengajuan ini, dengan logika derive/override dari
	 * {@link DisposisiSop} dan dari status penolakan yang sepenuhnya sejajar dengan
	 * {@link #getDisetujuiOleh()} — lihat javadoc method tersebut untuk penjelasan lengkap
	 * mengenai dua jalur persetujuan yang tumpang tindih dan konsekuensinya terhadap perhitungan
	 * skor KPI. Ringkasnya: bila {@link DisposisiSop} terkait punya tahap "setuju" dengan pengaju
	 * tercatat, waktu disposisi tersebut dipakai (menimpa kolom mentah, hanya di memori); bila
	 * disposisi ada tapi tahap "setuju"-nya belum/tidak lengkap, tanggal dipaksa {@code null}; dan
	 * bila pengajuan sudah ditolak ({@link #getDitolakOleh()} tidak {@code null}), tanggal
	 * persetujuan selalu dipaksa {@code null} apa pun nilai sebelumnya. Akses ke
	 * {@link #getDisposisiSop()} dibungkus {@code try/catch} untuk memitigasi
	 * {@code LazyInitializationException} pada proxy lintas-session.
	 *
	 * @return waktu persetujuan efektif; {@code null} bila belum disetujui atau sudah ditolak
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
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/kpi/PengajuanKpi.java:getTanggalPersetujuan-lazy");
		}

		if (getDitolakOleh() != null) {
			tanggalPersetujuan = null;
		}

		return tanggalPersetujuan;
	}

	/**
	 * Mengisi kolom mentah tanggal pembuatan pengajuan.
	 *
	 * @param tanggalPembuatan waktu pembuatan baru
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Mengambil waktu pembuatan pengajuan ini. Sama seperti {@link #getDibuatOleh()}, nilai ini
	 * ditimpa (hanya di memori) oleh waktu tahap "start" pada {@link DisposisiSop} terkait bila
	 * tersedia, dengan akses yang dibungkus {@code try/catch} untuk memitigasi
	 * {@code LazyInitializationException} pada proxy lintas-session. Berbeda dari getter tanggal
	 * lainnya di kelas ini, method ini TIDAK PERNAH mengembalikan {@code null}: bila kolom mentah
	 * maupun turunan disposisi sama-sama kosong, method jatuh ke {@link WaktuUtil#getDate()}
	 * (waktu saat ini) sebagai fallback — pola yang perlu diwaspadai bila kode pemanggil
	 * mengandalkan {@code null} sebagai penanda "pengajuan belum diberi tanggal pembuatan".
	 *
	 * @return waktu pembuatan efektif, tidak pernah {@code null}
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
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/kpi/PengajuanKpi.java:getTanggalPembuatan-lazy");
		}

		return tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan;
	}

	/**
	 * Mengambil satuan kerja pemilik pengajuan ini (batas kepemilikan/tenant data). Nilai proxy
	 * di-refresh lewat {@code check(...)} sebelum dikembalikan (pola umum di banyak entitas AIS
	 * untuk memastikan proxy Hibernate yang dipegang masih valid/terikat session yang benar).
	 *
	 * @return satuan kerja pengaju, bisa {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);

		return satuanKerja;
	}

	/**
	 * Mengisi satuan kerja pemilik pengajuan.
	 *
	 * @param satuanKerja satuan kerja baru
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/** Kolom mentah tanggal penolakan; lihat {@link #getTanggalDitolak()}. */
	private Date tanggalDitolak;

	/** Kolom mentah relasi pengguna yang menolak pengajuan (jalur mandiri); lihat {@link #getDitolakOleh()}. */
	private Tbmuser ditolakOleh;

	/** Flag aktif/nonaktif pengajuan; lihat {@link #getAktif()} untuk logika override otomatis dari status disposisi SOP. */
	private Boolean aktif;

	/** Konfigurasi nomor surat/alur pengadaan yang dipakai saat pengajuan ini diregistrasikan ke mesin nomor surat; lihat {@link #getNomorSuratAlurPengadaan()} untuk nilai default. */
	private NomorSuratAlurPengadaan nomorSuratAlurPengadaan;

	/**
	 * Mengambil node disposisi pada mesin alur/SOP generik yang terkait dengan pengajuan ini, bila
	 * pengajuan ini pernah diregistrasikan/diproses lewat mesin tersebut. Nilai proxy di-refresh
	 * lewat {@code check(...)} sebelum dikembalikan.
	 *
	 * @return node disposisi SOP terkait, atau {@code null} bila pengajuan ini tidak/belum memakai
	 *         mesin SOP generik (mis. hanya diproses lewat jalur persetujuan mandiri)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Mengaitkan pengajuan ini ke sebuah node disposisi SOP. Setter ini bersifat "write-once/
	 * keep-last-good" berlapis dua: baris pertama langsung mengembalikan tanpa melakukan apa pun
	 * bila {@code disposisiSop} yang diberikan {@code null} atau belum punya id (transient) —
	 * sehingga baris kedua (ternary yang membandingkan ulang kondisi yang sama) pada praktiknya
	 * TIDAK PERNAH mengeksekusi cabang "pertahankan nilai lama"-nya karena kondisi tersebut sudah
	 * pasti salah pada titik itu (kode mati/redundant, bukan bug fungsional — hanya sisa refactor
	 * yang tidak dibersihkan). Efek bersihnya: pemanggilan dengan {@code disposisiSop} valid akan
	 * selalu menimpa field, sedangkan pemanggilan dengan {@code null}/objek transient akan selalu
	 * diabaikan sepenuhnya.
	 *
	 * @param disposisiSop node disposisi SOP baru; diabaikan bila {@code null} atau belum tersimpan
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {if(disposisiSop==null||disposisiSop.getId()==null) {return;}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
	}

	/**
	 * Mengambil pengguna yang menolak pengajuan ini lewat jalur persetujuan mandiri (tombol Tolak
	 * pada {@code ais.action.master.kpi.PengajuanKpiAction}). Berbeda dari
	 * {@link #getDisetujuiOleh()}/{@link #getTanggalPersetujuan()}, getter ini TIDAK memiliki
	 * logika derive dari {@link DisposisiSop} — nilainya murni kolom mentah yang di-refresh lewat
	 * {@code check(...)}, sehingga penolakan hanya pernah tercatat lewat jalur mandiri, tidak
	 * pernah lewat mesin SOP generik.
	 *
	 * @return pengguna penolak, atau {@code null} bila belum/tidak ditolak
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ditolak_oleh", nullable = true)
	public Tbmuser getDitolakOleh() {
		ditolakOleh = check(ditolakOleh);
		return ditolakOleh;
	}

	/**
	 * Mengisi kolom mentah penolak pengajuan. Dipanggil langsung oleh tombol Tolak/Batalkan pada
	 * jalur persetujuan mandiri; lihat catatan keamanan pada javadoc kelas mengenai gerbang
	 * privilese yang hanya bersifat UI-only (visibilitas tombol) tanpa verifikasi ulang di dalam
	 * handler mutasi.
	 *
	 * @param ditolakOleh pengguna penolak, atau {@code null} untuk membatalkan penolakan
	 */
	public void setDitolakOleh(Tbmuser ditolakOleh) {
		this.ditolakOleh = ditolakOleh;
	}

	/**
	 * Mengambil waktu penolakan pengajuan ini.
	 *
	 * @return waktu penolakan, atau {@code null} bila belum/tidak ditolak
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_ditolak")
	public Date getTanggalDitolak() {
		return tanggalDitolak;
	}

	/**
	 * Mengisi waktu penolakan pengajuan.
	 *
	 * @param tanggalDitolak waktu penolakan baru
	 */
	public void setTanggalDitolak(Date tanggalDitolak) {
		this.tanggalDitolak = tanggalDitolak;
	}

	/**
	 * Mengambil status aktif/nonaktif pengajuan ini untuk kebutuhan filter tampilan (baris tidak
	 * aktif umumnya disembunyikan dari pencarian daftar pengajuan secara default).
	 *
	 * <p>Selain nilai kolom mentah {@link #aktif} (default {@code true} bila belum pernah diisi),
	 * method ini memaksa hasil menjadi {@code false} pada dua kondisi tambahan yang diturunkan dari
	 * {@link #getDisposisiSop()}: (1) bila disposisi SOP terkait sendiri sudah tidak aktif
	 * ({@code !disposisiSop.getAktif()}); atau (2) bila tahap akhir disposisi ({@code
	 * getDisposisiEnd()}) memiliki {@code AlurSop} yang menandai "penolakan ada di sini"
	 * ({@code getPenolakanAdaDiSini()}) — yaitu titik alur tempat penolakan SOP terjadi. Kedua
	 * override ini HANYA berlaku menimpa arah "matikan" (memaksa {@code false}); tidak ada logika
	 * yang memaksa balik ke {@code true}, sehingga begitu salah satu kondisi tercapai, method ini
	 * akan konsisten mengembalikan {@code false} pada pemanggilan berikutnya juga (nilai
	 * {@link #aktif} sendiri ikut ditimpa menjadi {@code false} sebagai efek samping pembacaan).</p>
	 *
	 * @return {@code true} bila pengajuan dianggap aktif, {@code false} bila dinonaktifkan secara
	 *         eksplisit atau dinonaktifkan otomatis lewat status disposisi/alur SOP terkait
	 */
	public Boolean getAktif() {
		disposisiSop = getDisposisiSop();
		if (disposisiSop != null && !disposisiSop.getAktif()) {
			aktif = false;
		}
		if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
			aktif = false;
		}
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengisi flag aktif/nonaktif secara eksplisit. Nilai ini bisa ditimpa kembali menjadi
	 * {@code false} oleh {@link #getAktif()} pada pembacaan berikutnya bila kondisi disposisi/alur
	 * SOP terkait mengharuskannya (lihat javadoc {@link #getAktif()}).
	 *
	 * @param aktif status aktif baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengambil kode tahun akademik/anggaran pengajuan ini.
	 *
	 * @return nilai kolom {@link #ta}, atau {@link Common#getCurrentTahunAkademik()} sebagai
	 *         fallback bila belum diisi
	 */
	public String getTa() {
		return ta == null ? Common.getCurrentTahunAkademik() : ta;
	}

	/**
	 * Mengisi kode tahun akademik/anggaran pengajuan.
	 *
	 * @param ta kode tahun akademik/anggaran baru
	 */
	public void setTa(String ta) {
		this.ta = ta;
	}

	/**
	 * Mengambil daftar id {@link PenilaianKpi} anggota pengajuan ini dalam format CSV yang sudah
	 * dinormalisasi: getter ini SELALU menormalisasi ulang field {@link #penilaianKpis} setiap kali
	 * dipanggil (efek samping menulis field dari getter) dengan cara membungkusnya dengan koma di
	 * awal/akhir ({@code "," + trim + ","}) lalu melipat pasangan koma ganda ({@code ",,"} menjadi
	 * {@code ","}) sebanyak tiga kali berturut-turut — jumlah iterasi tetap (bukan loop sampai
	 * stabil) ini cukup untuk kasus umum tetapi secara teoritis bisa menyisakan koma ganda yang
	 * belum terlipat sempurna pada input dengan pola koma yang sangat berulang/pathological. Setelah
	 * itu, hasil yang sama persis dengan {@code ","}, {@code ",,"}, {@code ",,,"}, atau {@code
	 * ",,,,"} secara eksplisit direset menjadi string kosong (menangani sisa kasus setelah koma
	 * pembungkus dilipat balik).
	 *
	 * @return string CSV id {@link PenilaianKpi} yang sudah dinormalisasi (dibungkus koma di kedua
	 *         sisi bila tidak kosong), tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getPenilaianKpis() {
		penilaianKpis = (penilaianKpis == null || penilaianKpis.trim().equalsIgnoreCase(",") ? ""
				: "," + penilaianKpis.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (penilaianKpis.equals(",")) {
			penilaianKpis = "";
		} else if (penilaianKpis.equals(",,")) {
			penilaianKpis = "";
		} else if (penilaianKpis.equals(",,,")) {
			penilaianKpis = "";
		} else if (penilaianKpis.equals(",,,,")) {
			penilaianKpis = "";
		}

		return penilaianKpis == null ? "" : penilaianKpis.trim();
	}

	/**
	 * Mengisi daftar id {@link PenilaianKpi} anggota pengajuan (format CSV mentah, akan
	 * dinormalisasi saat berikutnya dibaca lewat {@link #getPenilaianKpis()}).
	 *
	 * @param penilaianKpis string CSV id baru
	 */
	public void setPenilaianKpis(String penilaianKpis) {
		this.penilaianKpis = penilaianKpis;
	}

	/**
	 * Mengambil judul/nama pengajuan.
	 *
	 * @return nama pengajuan, bisa {@code null}
	 */
	public String getNama() {
		return nama;
	}

	/**
	 * Mengisi judul/nama pengajuan.
	 *
	 * @param nama judul baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil konfigurasi nomor surat/alur pengadaan yang dipakai pengajuan ini saat
	 * diregistrasikan ke mesin nomor surat. Bila belum pernah diisi, method ini mengembalikan
	 * konstanta default {@link NomorSuratAlurPengadaan#PENGAJUAN_KPI_PEGAWAI} (dan MENYIMPAN
	 * konstanta tersebut ke field di memori sebagai efek samping pembacaan) alih-alih
	 * mengembalikan {@code null} — pola umum di banyak entitas AIS yang memakai
	 * {@link NomorSuratAlurPengadaan} untuk memastikan setiap baris selalu punya konfigurasi nomor
	 * surat yang valid tanpa perlu migrasi data mengisi kolom lama.
	 *
	 * @return konfigurasi nomor surat/alur pengadaan efektif, tidak pernah {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat_alur_pengadaan", nullable = true)
	public NomorSuratAlurPengadaan getNomorSuratAlurPengadaan() {
		if (nomorSuratAlurPengadaan == null) {
			nomorSuratAlurPengadaan = NomorSuratAlurPengadaan.PENGAJUAN_KPI_PEGAWAI;
		} else {
			nomorSuratAlurPengadaan = check(nomorSuratAlurPengadaan);
		}
		return nomorSuratAlurPengadaan;
	}

	/**
	 * Mengisi konfigurasi nomor surat/alur pengadaan.
	 *
	 * @param nomorSuratAlurPengadaan konfigurasi baru
	 */
	public void setNomorSuratAlurPengadaan(NomorSuratAlurPengadaan nomorSuratAlurPengadaan) {
		this.nomorSuratAlurPengadaan = nomorSuratAlurPengadaan;
	}
}
