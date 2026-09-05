package ais.database.model.spmi;

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
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * Entitas Hibernate yang memetakan tabel {@code public.hasil_spmi} pada modul
 * SPMI (Sistem Penjaminan Mutu Internal) perguruan tinggi. Merepresentasikan
 * satu <b>sesi/header evaluasi (Audit Mutu Internal — AMI)</b> — pengajuan
 * yang mengumpulkan sekumpulan {@link HasilTemuanSPMI} (temuan) hasil
 * pemeriksaan atas skenario-skenario di bawah satu {@link JenisSPMI}, untuk
 * satu tahun akademik/semester dan (opsional) satu ruang lingkup organisasi
 * (perguruan tinggi/fakultas/jurusan). Ini adalah simpul fase
 * <i>Evaluasi</i> pada siklus PPEPP (Penetapan-Pelaksanaan-Evaluasi-
 * Pengendalian-Peningkatan), sekaligus tempat status persetujuan sesi
 * evaluasi itu sendiri dikelola.
 *
 * <p><b>Mesin persetujuan — memakai SOP generik:</b> berbeda dari entitas
 * master SPMI lain di paket ini, kelas ini meng-{@code extends}
 * {@link DataSop} dan menautkan diri ke {@link DisposisiSop} (lihat
 * {@link #getDisposisiSop()}/{@link #setDisposisiSop(DisposisiSop)}) —
 * artinya alur pengajuan-persetujuan sesi evaluasi ini berjalan di atas mesin
 * {@code AlurSop}/{@code DisposisiSop} generik yang dipakai bersama oleh
 * banyak modul SOP lain di seluruh basis kode. Sebagian besar getter terkait
 * status/approval di kelas ini ({@link #getDibuatOleh()}, {@link #getDisetujuiOleh()},
 * {@link #getTanggalPersetujuan()}, {@link #getStatus()}) membaca ulang data
 * dari {@code DisposisiSop} tersebut secara lazy setiap kali dipanggil,
 * dengan fallback ke field lokal bila proxy Hibernate sudah stale/di luar
 * session ({@code LazyInitializationException} — lihat komentar
 * "FIX LazyInitializationException" pada masing-masing getter). Bila
 * ditemukan celah bypass persetujuan pada mesin {@code AlurSop}/{@code DisposisiSop}
 * generik itu sendiri, kelas ini turut terdampak sebagai konsumennya —
 * bukan kasus terpisah.</p>
 *
 * <p><b>Konteks organisasi/tenant:</b> berbeda dari {@link JenisSPMI},
 * {@link StandarSPMI}, {@link ButirMutuSPMI}, {@link IndikatorSPMI}, dan
 * {@link SkenarioSPMI} (yang merupakan data master tanpa kolom tenant
 * sendiri), entitas ini membawa referensi eksplisit {@link #getPerguruanTinggi()},
 * {@link #getFakultas()}, dan {@link #getJurusan()} — di sinilah ruang
 * lingkup organisasi sesi evaluasi ditentukan. Kolom {@code perguruan_tinggi}
 * sendiri {@code nullable = true} pada level basis data, namun getter-nya
 * memiliki fallback (lihat {@link #getPerguruanTinggi()}) yang mengisi nilai
 * dari konteks sesi pengguna aktif bila belum diset eksplisit pada baris.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "hasil_spmi")
public class HasilSPMI extends DataSop {

	/** Kode status: sesi evaluasi sudah dibuat/diajukan tapi belum ada keputusan persetujuan/penolakan. Juga nilai fallback default dari {@link #getStatus()}. */
	public static final String PENGAJUAN = "Pengajuan";
	/** Kode status: sesi evaluasi telah disetujui — turunan dari {@link #getDisetujuiOleh()} bukan null, bukan disimpan independen. */
	public static final String DISETUJU = "Disetujui";
	/** Kode status: sesi evaluasi ditolak melalui alur {@code DisposisiSop} (lihat pengecekan {@code getPenolakanAdaDiSini()} pada {@link #getStatus()}). */
	public static final String DITOLAK = "Ditolak";

	/**
	 * Nomor versi serialisasi tetap untuk kontrak {@link java.io.Serializable}.
	 * Nilai literal ini disalin dari template hbm2java bersama entitas SPMI
	 * lain di paket ini (bukan dihitung ulang per kelas).
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * @return nilai mentah kolom audit shadow {@code olehId} (identitas
	 *         pengguna yang terakhir menyimpan/mengubah baris ini), atau
	 *         {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan identitas pengguna (kolom audit shadow {@code olehId}). Setter
	 * ini sengaja mengabaikan nilai {@code null} atau kosong (guard di baris
	 * pertama) — kebutuhan teknis (bukan bug): nilai yang sudah tercatat oleh
	 * interceptor audit tidak boleh tertimpa oleh panggilan berikutnya yang
	 * membawa nilai kosong/null.
	 *
	 * @param olehId identitas pengguna; diabaikan bila null/kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna (kolom audit shadow {@code oleh}), dengan guard
	 * yang sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna; diabaikan bila null/kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna yang tercatat pada kolom audit shadow {@code oleh},
	 *         atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil otomatis oleh provider persistence
	 * sesaat sebelum baris ini di-{@code UPDATE}, mendelegasikan pencatatan
	 * timestamp perubahan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 * Bukan API publik — tidak dipanggil manual dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah timestamp perubahan terakhir; biasanya diisi
	 *                        otomatis oleh {@link #onUpdate()}.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return timestamp terakhir kali baris ini diubah, diinisialisasi ke
	 *         waktu saat objek dibuat dan diperbarui otomatis oleh
	 *         {@link #onUpdate()} saat baris diperbarui di database.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return representasi ringkas berupa {@code id + "-" + nama}, dipakai
	 *         untuk log/debug dan tampilan singkat, bukan identitas bisnis.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	private PerguruanTinggi perguruanTinggi;
	private Fakultas fakultas;
	private Jurusan jurusan;
	private DisposisiSop disposisiSop;
	private JenisSPMI jenisSPMI;
	private Date tanggal;
	private String ta;
	private String semester;
	private String nama;
	private String keterangan;
	private Boolean aktif;
	private String status;
	private Tbmuser dibuatOleh;
	private Tbmuser disetujuiOleh;
	private Date tanggalPembuatan;
	private Date tanggalPersetujuan;
	private Date tanggalPersetujuanManual;
	// Nama auditor (lead auditor / tim audit) dan auditee (pihak yang diaudit)
	// DDL: ALTER TABLE hasil_spmi ADD COLUMN auditor_nama TEXT; ADD COLUMN auditee_nama TEXT;
	private String auditorNama;
	private String auditeeNama;

	/** Konstruktor kosong wajib bagi Hibernate untuk membentuk proxy/instance entitas. */
	public HasilSPMI() {
	}

	/**
	 * @return primary key baris ini. Kolom {@code id} bertipe {@code IDENTITY}
	 *         (auto-increment oleh database) dan ditandai {@code insertable = false}.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id primary key; jarang dipanggil manual karena {@code id} adalah IDENTITY. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return nama/judul sesi evaluasi ini, di-{@code trim()} terlebih dahulu;
	 *         dikembalikan sebagai string kosong (bukan {@code null}) bila
	 *         belum diisi.
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? "" : this.nama.trim();
	}

	/** @param nama nama/judul sesi evaluasi; wajib diisi (kolom {@code NOT NULL}). */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan/deskripsi tambahan bagi sesi evaluasi ini; boleh {@code null}. */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return {@code true} bila sesi evaluasi ini masih aktif/berlaku,
	 *         {@code false} bila dinonaktifkan (soft delete). Default
	 *         {@code true} bila kolom belum pernah diisi — pola flag aktif
	 *         "default aman" yang konsisten dengan entitas SPMI lain di
	 *         paket ini.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif status aktif/nonaktif sesi evaluasi ini; lihat {@link #getAktif()}. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * @return perguruan tinggi yang menjadi ruang lingkup sesi evaluasi ini.
	 *         Getter memanggil {@link #check(Object)} warisan dari
	 *         {@link GeneralValueObject} untuk menangani proxy Hibernate yang
	 *         stale/terputus dari session; bila hasilnya masih {@code null}
	 *         (baris belum pernah menyimpan referensi eksplisit — kolom
	 *         {@code perguruan_tinggi} memang {@code nullable = true}),
	 *         getter jatuh ke fallback
	 *         {@link ais.action.master.helper.util.PerguruanTinggiUtil#getPerguruanTinggi()}
	 *         yang mengambil perguruan tinggi dari konteks sesi pengguna
	 *         aktif. Kegagalan pada fallback ini ditangkap dan direkam lewat
	 *         {@link ais.common.ErrorAuditUtil#record} tanpa dilempar ulang,
	 *         sehingga getter tidak pernah gagal karena masalah pada
	 *         resolusi tenant. Nilai kembalian tidak otomatis disimpan
	 *         (di-{@code set}) ke field {@code perguruanTinggi}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "perguruan_tinggi", nullable = true)
	public PerguruanTinggi getPerguruanTinggi() {
		perguruanTinggi = check(perguruanTinggi);
		try {
			if (perguruanTinggi == null) {
				perguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/spmi/HasilSPMI.java:166");
		}
		return perguruanTinggi;
	}

	/** @param perguruanTinggi ruang lingkup perguruan tinggi sesi evaluasi ini; lihat {@link #getPerguruanTinggi()}. */
	public void setPerguruanTinggi(PerguruanTinggi perguruanTinggi) {
		this.perguruanTinggi = perguruanTinggi;
	}

	/**
	 * @return tanggal pelaksanaan sesi evaluasi ini; bila belum pernah diisi,
	 *         dikembalikan tanggal hari ini ({@link WaktuUtil#getDate()})
	 *         sebagai nilai fallback tampilan (bukan nilai yang otomatis
	 *         disimpan ke database).
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggal() {
		return tanggal == null ? WaktuUtil.getDate() : tanggal;
	}

	/** @param tanggal tanggal pelaksanaan sesi evaluasi ini; opsional. */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * @return kode tahun akademik sesi evaluasi ini; bila belum pernah diisi,
	 *         jatuh ke {@link Common#getCurrentTahunAkademik()} (tahun
	 *         akademik berjalan saat ini) sebagai nilai fallback.
	 */
	public String getTa() {
		return ta == null ? Common.getCurrentTahunAkademik() : ta;
	}

	/** @param ta kode tahun akademik sesi evaluasi ini; opsional, lihat {@link #getTa()}. */
	public void setTa(String ta) {
		this.ta = ta;
	}

	/**
	 * @return kode semester sesi evaluasi ini ({@link Perkuliahan#GANJIL}
	 *         atau {@link Perkuliahan#GENAP}); bila kolom kosong/belum diisi,
	 *         jatuh ke semester berjalan saat ini yang ditentukan oleh
	 *         {@link Common#isNowSemensterGanjil()} sebagai nilai fallback.
	 */
	public String getSemester() {
		return semester == null || semester.trim().isEmpty()
				? (Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP)
				: semester;
	}

	/** @param semester kode semester sesi evaluasi ini; opsional, lihat {@link #getSemester()}. */
	public void setSemester(String semester) {
		this.semester = semester;
	}

	/**
	 * @return fakultas yang menjadi ruang lingkup sesi evaluasi ini (lebih
	 *         sempit dari {@link #getPerguruanTinggi()}); boleh {@code null}
	 *         bila sesi evaluasi berlingkup seluruh institusi. Getter
	 *         memanggil {@link #check(Object)} untuk menangani proxy
	 *         Hibernate yang stale/terputus dari session.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/** @param fakultas ruang lingkup fakultas sesi evaluasi ini; opsional, lihat {@link #getFakultas()}. */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * @return jurusan yang menjadi ruang lingkup sesi evaluasi ini (lebih
	 *         sempit dari {@link #getFakultas()}); boleh {@code null} bila
	 *         sesi evaluasi berlingkup seluruh fakultas/institusi. Getter
	 *         memanggil {@link #check(Object)} untuk menangani proxy
	 *         Hibernate yang stale/terputus dari session.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/** @param jurusan ruang lingkup jurusan sesi evaluasi ini; opsional, lihat {@link #getJurusan()}. */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * @return {@link DisposisiSop} yang mengikat sesi evaluasi ini ke mesin
	 *         SOP generik ({@code AlurSop}/{@code DisposisiSop}) — instance
	 *         pengajuan/disposisi konkret yang sedang berjalan untuk sesi
	 *         evaluasi ini. Getter memanggil {@link #check(Object)} untuk
	 *         menangani proxy Hibernate yang stale/terputus dari session.
	 *         Boleh {@code null} bila sesi evaluasi belum/tidak diajukan
	 *         lewat alur SOP. Seluruh logika status/persetujuan di kelas ini
	 *         ({@link #getDibuatOleh()}, {@link #getDisetujuiOleh()},
	 *         {@link #getTanggalPersetujuan()}, {@link #getStatus()})
	 *         bergantung pada nilai balik method ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menyimpan {@link DisposisiSop} pengikat sesi evaluasi ini. Guard di
	 * awal method menolak nilai {@code null} atau belum ter-{@code persist}
	 * (belum punya {@code id}) dan langsung {@code return} tanpa mengubah
	 * apa pun. Baris penugasan berikutnya memakai ekspresi ternary yang
	 * tampak defensif — <i>namun</i> kondisinya
	 * ({@code disposisiSop == null || disposisiSop.getId() == null}) pada
	 * titik itu <b>selalu bernilai false</b> (guard di atas sudah menyaring
	 * kasus tersebut lebih dulu), sehingga cabang {@code this.disposisiSop}
	 * pada ternary tidak pernah tereksekusi — method ini secara efektif
	 * setara dengan {@code this.disposisiSop = disposisiSop;} setelah guard.
	 * Kode mati ini dipertahankan apa adanya (bukan bug fungsional, hanya
	 * redundansi) — bukan bagian dari cakupan dokumentasi ini untuk diubah.
	 *
	 * @param disposisiSop disposisi SOP baru; diabaikan bila null atau belum punya id
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
	}

	/**
	 * Setter murni tanpa validasi — berbeda dari getter {@link #getDibuatOleh()}
	 * (yang bisa menimpa nilai field ini dengan hasil turunan dari
	 * {@link #getDisposisiSop()}), setter ini hanya menyimpan nilai apa
	 * adanya, termasuk {@code null}.
	 *
	 * @param dibuatOleh pengguna pembuat sesi evaluasi ini (nilai awal sebelum kemungkinan ditimpa oleh getter)
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * @return {@link Tbmuser} yang membuat/mengajukan sesi evaluasi ini.
	 *         Getter ini <b>mengutamakan data dari mesin SOP</b>: dimulai dari
	 *         nilai lokal ({@link #check(Object)} atas field {@code dibuatOleh}),
	 *         kemudian bila {@link #getDisposisiSop()} beserta langkah
	 *         mulainya ({@code getDisposisiStart()}) tersedia dan sudah
	 *         diajukan oleh seseorang, field lokal <b>ditimpa</b> dengan
	 *         pengaju dari {@code DisposisiSop} tersebut — artinya nilai yang
	 *         dikembalikan bisa berbeda dari yang terakhir di-{@code set}
	 *         lewat {@link #setDibuatOleh(Tbmuser)} bila sesi evaluasi ini
	 *         sudah terikat ke alur SOP. Seluruh akses ke rantai
	 *         {@code DisposisiSop} dibungkus {@code try/catch}: proxy
	 *         Hibernate milik {@code disposisiSop} bisa berupa instance
	 *         canonical/shared (dipakai lintas request oleh
	 *         {@code AuditTimestampInterceptor}) yang session pemiliknya
	 *         sudah closed saat getter ini dipanggil — {@code LazyInitializationException}
	 *         yang muncul dari sini ditangkap dan direkam lewat
	 *         {@link ais.common.ErrorAuditUtil#record}, getter tetap
	 *         mengembalikan nilai fallback (field lokal) tanpa melempar
	 *         exception ke pemanggil.
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
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/spmi/HasilSPMI.java:getDibuatOleh-lazy");
		}
		return dibuatOleh;
	}

	/**
	 * Setter murni tanpa validasi — nilai yang diset lewat method ini bisa
	 * ditimpa kembali oleh {@link #getDisetujuiOleh()} bila sesi evaluasi ini
	 * sudah memiliki keputusan persetujuan pada {@link #getDisposisiSop()}.
	 *
	 * @param disetujuiOleh pengguna penyetuju sesi evaluasi ini (nilai awal sebelum kemungkinan ditimpa oleh getter)
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * @return {@link Tbmuser} yang menyetujui sesi evaluasi ini, atau
	 *         {@code null} bila belum/tidak disetujui. Sama seperti
	 *         {@link #getDibuatOleh()}, getter ini membaca ulang keputusan
	 *         dari {@link #getDisposisiSop()} setiap kali dipanggil: bila
	 *         langkah persetujuan ({@code getDisposisiSetuju()}) pada
	 *         disposisi tersebut sudah memiliki pengaju, field lokal ditimpa
	 *         dengan pengaju itu; sebaliknya, bila disposisi ada namun langkah
	 *         persetujuannya belum/tidak memiliki pengaju, field lokal
	 *         secara eksplisit di-{@code null}-kan (baris ini efektif
	 *         "mencabut" persetujuan lama bila status pada mesin SOP berubah
	 *         menjadi belum disetujui). Seluruh akses ke {@code DisposisiSop}
	 *         dibungkus {@code try/catch} dengan alasan yang sama seperti
	 *         {@link #getDibuatOleh()} ({@code LazyInitializationException}
	 *         pada proxy canonical/shared yang session-nya sudah closed).
	 *         Sebagai efek samping tambahan, bila
	 *         {@link #getTanggalPersetujuanManual()} terisi dan hasil akhir
	 *         {@code disetujuiOleh} tidak null, field {@code tanggalPersetujuan}
	 *         ditimpa dengan tanggal persetujuan manual tersebut — lihat
	 *         {@link #getTanggalPersetujuan()} untuk konsumsi nilai ini.
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
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/spmi/HasilSPMI.java:getDisetujuiOleh-lazy");
		}

		disetujuiOleh = check(disetujuiOleh);
		if (getTanggalPersetujuanManual() != null && disetujuiOleh != null) {
			tanggalPersetujuan = getTanggalPersetujuanManual();
		}

		return disetujuiOleh;
	}

	/**
	 * Setter murni tanpa validasi — nilai bisa ditimpa kembali oleh
	 * {@link #getTanggalPersetujuan()} berdasarkan data {@link #getDisposisiSop()}.
	 *
	 * @param tanggalPersetujuan tanggal persetujuan (nilai awal sebelum kemungkinan ditimpa oleh getter)
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * @return tanggal/waktu persetujuan sesi evaluasi ini, atau {@code null}
	 *         bila belum disetujui. Sama seperti {@link #getDisetujuiOleh()},
	 *         getter ini membaca ulang data dari {@link #getDisposisiSop()}
	 *         setiap kali dipanggil: bila langkah persetujuan sudah memiliki
	 *         pengaju, field lokal ditimpa dengan waktu disposisi persetujuan
	 *         tersebut; bila langkah persetujuan belum/tidak memiliki
	 *         pengaju, field lokal di-{@code null}-kan. Dibungkus
	 *         {@code try/catch} dengan alasan yang sama seperti
	 *         {@link #getDibuatOleh()} ({@code LazyInitializationException}
	 *         pada proxy canonical/shared). Catatan: bila
	 *         {@link #getDisetujuiOleh()} dipanggil lebih dulu dan
	 *         {@link #getTanggalPersetujuanManual()} terisi, field ini bisa
	 *         sudah ditimpa oleh tanggal manual tersebut sebelum method ini
	 *         sempat membaca ulang dari {@code DisposisiSop} — urutan
	 *         pemanggilan kedua getter ini memengaruhi nilai akhir yang
	 *         terlihat.
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
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/spmi/HasilSPMI.java:getTanggalPersetujuan-lazy");
		}
		return tanggalPersetujuan;
	}

	/** @param tanggalPembuatan tanggal/waktu pembuatan sesi evaluasi ini; lihat {@link #getTanggalPembuatan()}. */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * @return tanggal/waktu pembuatan sesi evaluasi ini; bila belum pernah
	 *         diisi, dikembalikan waktu saat ini ({@code new Date()}) sebagai
	 *         nilai fallback tampilan (bukan nilai yang otomatis disimpan ke
	 *         database — berbeda dari {@link #getTanggal_dirubah()} yang
	 *         diinisialisasi sekali di deklarasi field).
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		return tanggalPembuatan == null ? new Date() : tanggalPembuatan;
	}

	/**
	 * @return kode status sesi evaluasi ini — salah satu dari
	 *         {@link #PENGAJUAN}, {@link #DISETUJU}, atau {@link #DITOLAK}.
	 *         Logikanya bertingkat: (1) bila {@link #getDisetujuiOleh()}
	 *         mengembalikan bukan null, status dipaksa menjadi
	 *         {@link #DISETUJU}; (2) sebaliknya, bila field lokal
	 *         {@code status} sebelumnya berisi {@link #DISETUJU} (kini
	 *         terbukti tidak sinkron dengan mesin SOP karena langkah (1)
	 *         gagal), status "dikoreksi mundur" menjadi {@link #PENGAJUAN};
	 *         (3) kemudian, jika {@link #getDisposisiSop()} beserta langkah
	 *         akhirnya ({@code getDisposisiEnd()}) menunjukkan alur SOP ini
	 *         memiliki penolakan di titik saat ini
	 *         ({@code getAlurSop().getPenolakanAdaDiSini()}), status ditimpa
	 *         menjadi {@link #DITOLAK} — mengalahkan hasil langkah (1)/(2).
	 *         Seluruh pemeriksaan mesin SOP pada langkah (3) dibungkus
	 *         {@code try/catch} ({@code LazyInitializationException} pada
	 *         proxy canonical/shared, sama seperti getter lain di kelas ini).
	 *         Nilai akhir yang dikembalikan tidak pernah {@code null} atau
	 *         kosong — jatuh ke {@link #PENGAJUAN} sebagai fallback paling
	 *         akhir.
	 */
	public String getStatus() {
		if (getDisetujuiOleh() != null) {
			status = DISETUJU;
		} else if (status != null && status.equals(DISETUJU)) {
			status = PENGAJUAN;
		}

		try {
			// FIX LazyInitializationException: disposisiSop bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			disposisiSop = getDisposisiSop();
			if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
					&& disposisiSop.getDisposisiEnd().getAlurSop() != null
					&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
				status = DITOLAK;
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/spmi/HasilSPMI.java:getStatus-lazy");
		}

		return status == null || status.trim().isEmpty() ? PENGAJUAN : status;
	}

	/**
	 * Menyimpan kode status ini secara manual. Bila nilai yang diset adalah
	 * {@link #DITOLAK}, method ini secara eksplisit membersihkan jejak
	 * persetujuan lokal — memanggil {@link #setDisetujuiOleh(Tbmuser)} dengan
	 * {@code null} dan {@link #setTanggalPersetujuan(Date)} dengan {@code null}
	 * — agar status "Ditolak" tidak tampak berdampingan dengan sisa data
	 * persetujuan yang sudah usang. Perlu diperhatikan bahwa
	 * {@link #getStatus()} pada dasarnya menghitung ulang status dari
	 * {@link #getDisposisiSop()}/{@link #getDisetujuiOleh()} setiap kali
	 * dipanggil, sehingga nilai yang diset lewat method ini bisa ditimpa lagi
	 * saat {@code getStatus()} berikutnya dipanggil jika sesi evaluasi ini
	 * terikat ke mesin SOP.
	 *
	 * @param status kode status baru; lihat {@link #getStatus()}
	 */
	public void setStatus(String status) {

		if (status != null && status.equals(DITOLAK)) {
			setDisetujuiOleh(null);
			setTanggalPersetujuan(null);
		}

		this.status = status;
	}

	/**
	 * @return tanggal persetujuan yang diinput manual (di luar mesin SOP),
	 *         atau {@code null} bila tidak ada override manual. Dipakai oleh
	 *         {@link #getDisetujuiOleh()} untuk menimpa {@code tanggalPersetujuan}
	 *         bila nilai ini terisi dan sesi evaluasi sudah memiliki penyetuju
	 *         — mekanisme untuk mengoreksi tanggal persetujuan tanpa mengubah
	 *         data pada {@link #getDisposisiSop()} itu sendiri.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggalPersetujuanManual() {
		return tanggalPersetujuanManual;
	}

	/** @param tanggalPersetujuanManual tanggal persetujuan manual (override); lihat {@link #getTanggalPersetujuanManual()}. */
	public void setTanggalPersetujuanManual(Date tanggalPersetujuanManual) {
		this.tanggalPersetujuanManual = tanggalPersetujuanManual;
	}

	/**
	 * @return nama auditor (lead auditor/tim audit) yang bertugas pada sesi
	 *         evaluasi ini; kolom teks bebas (bukan referensi ke {@link Tbmuser}),
	 *         ditambahkan belakangan lewat migrasi manual
	 *         ({@code ALTER TABLE hasil_spmi ADD COLUMN auditor_nama TEXT})
	 *         — lihat komentar pada deklarasi field di atas.
	 */
	@Column(name = "auditor_nama", nullable = true, columnDefinition = "text")
	public String getAuditorNama() {
		return auditorNama;
	}

	/** @param auditorNama nama auditor/tim audit; opsional, lihat {@link #getAuditorNama()}. */
	public void setAuditorNama(String auditorNama) {
		this.auditorNama = auditorNama;
	}

	/**
	 * @return nama auditee (pihak/unit yang diaudit) pada sesi evaluasi ini;
	 *         kolom teks bebas, ditambahkan belakangan lewat migrasi manual
	 *         yang sama seperti {@link #getAuditorNama()}
	 *         ({@code ALTER TABLE hasil_spmi ADD COLUMN auditee_nama TEXT}).
	 */
	@Column(name = "auditee_nama", nullable = true, columnDefinition = "text")
	public String getAuditeeNama() {
		return auditeeNama;
	}

	/** @param auditeeNama nama auditee (pihak yang diaudit); opsional, lihat {@link #getAuditeeNama()}. */
	public void setAuditeeNama(String auditeeNama) {
		this.auditeeNama = auditeeNama;
	}

	/**
	 * @return {@link JenisSPMI} (mis. "Lembar Kerja AMI") yang menjadi acuan
	 *         standar/skenario bagi sesi evaluasi ini. Getter memanggil
	 *         {@link #check(Object)} untuk menangani proxy Hibernate yang
	 *         stale/terputus dari session. Berbeda dari {@link StandarSPMI#getJenisSPMI()}
	 *         dkk. (yang mewajibkan {@code nullable = false}), kolom
	 *         {@code jenis_spmi} di sini {@code nullable = true} — sebuah
	 *         sesi evaluasi secara teknis boleh belum terikat ke jenis SPMI
	 *         tertentu.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_spmi", nullable = true)
	public JenisSPMI getJenisSPMI() {
		jenisSPMI = check(jenisSPMI);
		return jenisSPMI;
	}

	/** @param jenisSPMI jenis SPMI acuan sesi evaluasi ini; opsional, lihat {@link #getJenisSPMI()}. */
	public void setJenisSPMI(JenisSPMI jenisSPMI) {
		this.jenisSPMI = jenisSPMI;
	}
}
