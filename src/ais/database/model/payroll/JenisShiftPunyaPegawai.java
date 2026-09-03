package ais.database.model.payroll;

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

import ais.database.model.GeneralValueObject;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.sekolah.Siswa;

/**
 * Baris <b>penugasan shift</b> — memasangkan satu {@link JenisShiftPegawai} (katalog/header definisi
 * shift) ke satu pemilik ({@link Pegawai}, {@link Mahasiswa}, atau {@link Siswa}), dipetakan ke tabel
 * <code>payroll.jenis_shift_punya_pegawai</code>. Ini adalah lapisan TENGAH dari rantai tiga-lapis shift
 * kerja: {@link JenisShiftPegawai} (katalog jenis shift) &rarr; <b>{@code JenisShiftPunyaPegawai}
 * (kelas ini, baris penugasan)</b> &rarr; {@link DetailJenisShiftPegawai} (baris jam per hari-rotasi) —
 * lihat javadoc {@link JenisShiftPegawai} bagian 1 untuk gambaran rantai lengkap sampai ke mesin absensi
 * harian {@code StatuskehadiranKaryawanHarian}.
 *
 * <h3>Peran dalam resolusi shift pegawai (diverifikasi dari kode)</h3>
 * <p>Entity ini dicari oleh {@code ais.common.DetailJenisShiftPegawaiHelper.getJenisShiftPunyaPegawai}
 * saat sistem perlu menentukan shift efektif seorang pemilik pada tanggal tertentu. Method helper
 * tersebut, diverifikasi dari kode:</p>
 * <ol>
 * <li>Memfilter berdasarkan pemilik lewat {@code applyOwnerRestriction} — cocokkan
 * {@link #getMahasiswa()} bila pemanggil membawa objek mahasiswa (dengan ID valid); jika tidak, cocokkan
 * {@link #getSiswa()}; jika masih tidak, baru cocokkan {@link #getPegawai()}. <b>Catatan kewaspadaan:</b>
 * urutan prioritas ini (mahasiswa &gt; siswa &gt; pegawai) diterapkan lewat rantai {@code if}/{@code else
 * if}, BUKAN filter gabungan — bila pemanggil (secara keliru) membawa lebih dari satu identitas pemilik
 * sekaligus, hanya identitas dengan prioritas tertinggi yang benar-benar dipakai untuk query, yang lain
 * diam-diam diabaikan. Entity ini sendiri TIDAK memaksakan constraint "hanya satu dari
 * pegawai/mahasiswa/siswa yang boleh terisi" pada level Java maupun lewat anotasi apa pun di file ini —
 * ketiga field {@link #pegawai}, {@link #mahasiswa}, {@link #siswa} sama-sama {@code nullable = true}
 * dan bisa saja ketiganya terisi sekaligus pada satu baris tanpa ditolak oleh entity ini.</li>
 * <li>Memfilter validitas dan status aktif lewat {@code applyActiveAndDateRestrictions} yang DITERAPKAN
 * PADA ALIAS {@code jenisShiftPegawai} (header), BUKAN pada baris penugasan ini — artinya rentang
 * berlaku ({@code berlakuMulai}/{@code berlakuSampai}) dan flag {@code aktif} yang menentukan apakah
 * suatu penugasan "berlaku hari ini" berasal sepenuhnya dari {@link #getJenisShiftPegawai()}
 * ({@link JenisShiftPegawai#getBerlakuMulai()}/{@link JenisShiftPegawai#getBerlakuSampai()}/
 * {@link JenisShiftPegawai#getAktif()}). Entity {@code JenisShiftPunyaPegawai} ini sendiri TIDAK punya
 * field tanggal-berlaku atau flag-aktif sendiri — masa berlaku suatu penugasan sepenuhnya mengikuti masa
 * berlaku header shift yang ditunjuknya, tidak bisa diatur berbeda per-penugasan.</li>
 * <li>Pencarian dilakukan DUA TAHAP: tahap pertama hanya mencari baris yang punya
 * {@link #getDetailJenisShiftPegawai()} terisi (penugasan yang "dipin" ke satu baris detail shift
 * spesifik — lihat di bawah); bila tidak ketemu, tahap kedua mencari baris penugasan apa pun (dengan
 * {@code detailJenisShiftPegawai} boleh {@code null}) yang cocok kriteria pemilik dan tanggal yang sama.
 * Ini memberi prioritas ke penugasan yang eksplisit menunjuk satu detail shift dibanding penugasan
 * "generik" yang hanya menunjuk header shift saja.</li>
 * </ol>
 *
 * <h3>Field {@link #detailJenisShiftPegawai} — pin opsional ke satu baris detail shift spesifik</h3>
 * <p>Selain menunjuk header {@link JenisShiftPegawai}, satu baris penugasan bisa (opsional) menunjuk
 * LANGSUNG ke satu baris {@link DetailJenisShiftPegawai} tertentu lewat field ini — dipakai ketika
 * seorang pemilik perlu dikunci ke satu segmen jam/hari-rotasi spesifik dalam siklus shift header-nya,
 * alih-alih membiarkan mesin absensi mencari baris detail "terdekat" secara otomatis berdasarkan jam
 * absen aktual (lihat {@code DetailJenisShiftPegawaiHelper.shiftDetail}). Relasi ini juga dibaca balik
 * secara transient oleh {@link DetailJenisShiftPegawai#getJenisShiftPunyaPegawai()} untuk membawa
 * konteks "siapa pemilik penugasan ini" tanpa query tambahan setelah baris detail ditemukan.</p>
 *
 * <h3>Field {@link #abaikanJarak} — override geofencing per-penugasan</h3>
 * <p>Diverifikasi dipakai nyata di {@code ais.action.servlet.api.AbsensiApiAction} dan
 * {@code ais.action.master.ScanBerhasilAction}: validasi jarak GPS terhadap radius
 * {@link JenisShiftPegawai#getJarak()} (lihat javadoc {@link JenisShiftPegawai} bagian 3) dilewati bila
 * flag ini {@code true} <b>ATAU</b> {@code StatuskehadiranKaryawanHarian.getAbaikanJarak()} bertepatan
 * {@code true} — kedua sumber di-OR-kan lewat pengecekan {@code !a.getAbaikanJarak() &&
 * !b.getAbaikanJarak()} pada seluruh titik pemanggilan yang diverifikasi; salah satu saja bernilai
 * {@code true} sudah cukup untuk menonaktifkan pembatasan jarak pada absen pegawai bersangkutan.</p>
 *
 * <h3>Pola arsitektur berulang di kelas ini</h3>
 * <ul>
 * <li><b>Field audit shadow</b> — {@link #getOleh()}/{@link #getOlehId()}/{@link #getTanggal_dirubah()}
 * tidak beranotasi {@code @Column}; ini keharusan teknis karena {@link GeneralValueObject} bukan
 * {@code @Entity} JPA murni (tidak bisa memaksakan listener audit generik di superclass), bukan bug —
 * pola yang sama berulang di seluruh model AIS termasuk {@link JenisShiftPegawai} dan
 * {@link DetailJenisShiftPegawai}.</li>
 * <li><b>Getter proxy-resolving (bukan getter destruktif nilai)</b> — {@link #getJenisShiftPegawai()},
 * {@link #getPegawai()}, {@link #getMahasiswa()}, {@link #getSiswa()}, dan
 * {@link #getDetailJenisShiftPegawai()} semuanya memanggil {@code GeneralValueObject.check(Object)} yang
 * meresolusi proxy lazy Hibernate dan menulis balik hasilnya ke field — pola standar di seluruh entity
 * AIS, bukan kalkulasi/transformasi nilai seperti pada getter destruktif di {@link JenisShiftPegawai}
 * atau {@link DetailJenisShiftPegawai}.</li>
 * <li><b>Filter tenant/satuan-kerja</b> — tidak ada kolom {@code satuanKerja}/tenant eksplisit pada
 * entity ini; scoping organisasi (yayasan/sekolah/fakultas/jurusan) sepenuhnya diwariskan dari header
 * {@link JenisShiftPegawai} yang ditunjuknya, konsisten dengan catatan pada javadoc kelas tersebut
 * bagian 4.</li>
 * </ul>
 *
 * Bank generated by hbm2java
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "payroll", name = "jenis_shift_punya_pegawai")
public class JenisShiftPunyaPegawai extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable} antar build. Nilai ini
	 * warisan generator hbm2java dan sengaja tidak diubah kecuali struktur field berubah tak-kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Primary key baris penugasan shift ini, di-generate database (identity) — lihat {@link #getId()}. */
	private Long id;

	/** Nama/username user yang terakhir membuat atau mengubah baris ini (field audit shadow). */
	private String oleh;

	/** ID user yang terakhir membuat atau mengubah baris ini (field audit shadow), pasangan {@link #oleh}. */
	private String olehId;

	/**
	 * Mengembalikan ID user yang terakhir mengubah baris ini.
	 *
	 * @return ID user audit, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi ID user audit ({@link #olehId}). Guard: nilai {@code null} atau string kosong/hanya-spasi
	 * diabaikan diam-diam agar baris audit yang sudah ada tidak tertimpa kosong oleh pemanggil yang lupa
	 * menyertakan identitas user (mis. proses batch/background). Pola identik dengan setter audit pada
	 * {@link WaktuShift#setOlehId(String)}, {@link JenisShiftPegawai#setOlehId(String)}, dan
	 * {@link DetailJenisShiftPegawai#setOlehId(String)}.
	 *
	 * @param olehId ID user yang melakukan perubahan; diabaikan bila kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks ringkas baris penugasan shift ini untuk log/debug/tampilan combo-box, berformat
	 * {@code "<JenisShiftPegawai> <Pegawai>"}.
	 *
	 * <p><b>Efek samping:</b> method ini memanggil {@link #getJenisShiftPegawai()} dan
	 * {@link #getPegawai()} yang masing-masing menulis balik field {@link #jenisShiftPegawai}/
	 * {@link #pegawai} hasil resolusi proxy lazy (lihat javadoc kedua getter tersebut) — bukan operasi
	 * baca murni. <b>Catatan:</b> bila baris ini sebenarnya milik {@link Mahasiswa} atau {@link Siswa}
	 * (bukan {@link Pegawai}), bagian "{@code <Pegawai>}" dari string akan tampil sebagai literal
	 * "{@code null}" karena {@link #pegawai} memang tidak terisi untuk kasus itu — {@link #toString()}
	 * tidak memiliki fallback ke {@link #getMahasiswa()}/{@link #getSiswa()}.</p>
	 *
	 * @return string deskriptif baris penugasan, tidak pernah {@code null} secara struktur
	 */
	public String toString() {
		jenisShiftPegawai = getJenisShiftPegawai();
		pegawai = getPegawai();
		return jenisShiftPegawai + " " + pegawai;
	}

	/**
	 * Mengisi nama/username user audit ({@link #oleh}). Guard sama seperti {@link #setOlehId(String)}:
	 * nilai {@code null} atau kosong/hanya-spasi diabaikan diam-diam supaya nilai audit lama tidak
	 * tertimpa kosong.
	 *
	 * @param oleh username user yang melakukan perubahan; diabaikan bila kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan username user yang terakhir mengubah baris ini.
	 *
	 * @return username user audit, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook siklus-hidup JPA {@code @PreUpdate} — dipanggil otomatis oleh Hibernate tepat sebelum statement
	 * {@code UPDATE} dikirim ke database untuk entity ini. Mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang mengisi
	 * {@link #tanggal_dirubah} dengan waktu saat ini, memastikan jejak "kapan terakhir diubah" selalu
	 * konsisten tanpa bergantung pada pemanggil yang mengeset manual.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Timestamp perubahan terakhir baris ini (field audit shadow). Diinisialisasi ke waktu saat object
	 * dibuat di JVM dan ditimpa otomatis oleh {@link #onUpdate()} setiap kali baris di-{@code UPDATE}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi timestamp perubahan terakhir secara manual. Biasanya tidak perlu dipanggil langsung karena
	 * {@link #onUpdate()} sudah mengelolanya otomatis pada setiap update.
	 *
	 * @param tanggal_dirubah timestamp baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan timestamp perubahan terakhir baris ini.
	 *
	 * @return timestamp audit, dipetakan sebagai {@code TIMESTAMP}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Header definisi shift (katalog) yang ditugaskan lewat baris ini — lihat {@link #getJenisShiftPegawai()}.
	 * Wajib diisi (kolom {@code nullable = false}); masa berlaku dan status aktif efektif penugasan ini
	 * sepenuhnya mengikuti header ini (lihat javadoc kelas).
	 */
	private JenisShiftPegawai jenisShiftPegawai;

	/**
	 * Pegawai pemilik penugasan shift ini, bila pemiliknya seorang pegawai (bukan mahasiswa/siswa).
	 * Nullable — lihat javadoc kelas untuk catatan kewaspadaan mengenai tidak adanya constraint
	 * exclusivity antara {@link #pegawai}/{@link #mahasiswa}/{@link #siswa}.
	 */
	private Pegawai pegawai;

	/** Mahasiswa pemilik penugasan shift ini, bila pemiliknya seorang mahasiswa. Nullable. */
	private Mahasiswa mahasiswa;

	/** Siswa pemilik penugasan shift ini, bila pemiliknya seorang siswa. Nullable. */
	private Siswa siswa;

	/**
	 * Flag override: lewati validasi jarak/geofencing untuk pemilik penugasan ini; default efektif
	 * {@code false} — lihat {@link #getAbaikanJarak()} dan javadoc kelas untuk detail pemakaian nyata di
	 * pipeline absen mobile/API.
	 */
	private Boolean abaikanJarak;

	/**
	 * Pin opsional ke satu baris {@link DetailJenisShiftPegawai} spesifik dalam siklus rotasi header
	 * {@link #jenisShiftPegawai} — lihat javadoc kelas untuk penjelasan mekanisme dan prioritas
	 * pencariannya di {@code DetailJenisShiftPegawaiHelper}. {@code null} berarti pemilik mengikuti
	 * resolusi otomatis baris detail terdekat berdasarkan jam absen aktual.
	 */
	private DetailJenisShiftPegawai detailJenisShiftPegawai;

	/** Keterangan bebas (opsional) untuk baris penugasan shift ini. */
	private String keterangan;

	/**
	 * Konstruktor default tanpa argumen, dibutuhkan oleh Hibernate untuk instansiasi entity lewat
	 * reflection saat hydrating hasil query, serta dipakai kode aplikasi (mis.
	 * {@code ais.action.master.payroll.detail.JenisShiftPunyaPegawaiAction}) saat membuat baris
	 * penugasan baru sebelum field-nya diisi.
	 */
	public JenisShiftPunyaPegawai() {
	}

	/**
	 * Mengembalikan primary key baris penugasan shift ini.
	 *
	 * @return ID baris, {@code null} untuk instance yang belum dipersistensikan; kolom identity
	 *         database ({@code insertable = false}) sehingga tidak boleh diisi manual saat insert
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi ID baris. Karena kolom database bersifat {@code insertable = false} (identity), setter ini
	 * pada praktiknya hanya relevan untuk keperluan Hibernate hydration/testing, bukan untuk menetapkan ID
	 * baru secara manual sebelum insert.
	 *
	 * @param id ID baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan header definisi shift ({@link JenisShiftPegawai}) yang ditugaskan lewat baris ini —
	 * relasi {@code @ManyToOne} lewat kolom FK {@code jenis_shift_pegawai} (wajib diisi).
	 *
	 * <p><b>Efek samping:</b> memanggil {@code GeneralValueObject.check(Object)} yang meresolusi proxy lazy
	 * Hibernate (bila {@link #jenisShiftPegawai} masih berupa proxy yang belum diinisialisasi) dan
	 * berpotensi menggantinya dengan instance kanonik dari {@code EntityIdentityMap} — hasil resolusi ini
	 * ditulis balik ke field {@link #jenisShiftPegawai}. Ini bukan getter baca-murni, tetapi pola standar di
	 * seluruh entity AIS untuk memastikan satu object Java per ID entity di JVM yang sama.</p>
	 *
	 * @return header shift yang ditugaskan lewat baris ini, {@code null} hanya bila kolom FK belum
	 *         terisi (transient/belum tersimpan) — pada baris tersimpan kolom ini {@code not-null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_shift_pegawai", nullable = false)
	public JenisShiftPegawai getJenisShiftPegawai() {
		jenisShiftPegawai = check(jenisShiftPegawai);
		return jenisShiftPegawai;
	}

	/**
	 * Mengaitkan baris penugasan ini dengan header definisi shift tertentu.
	 *
	 * @param jenisShiftPegawai header shift baru; kolom FK bersifat {@code nullable = false} sehingga
	 *                          nilai {@code null} akan gagal disimpan
	 */
	public void setJenisShiftPegawai(JenisShiftPegawai jenisShiftPegawai) {
		this.jenisShiftPegawai = jenisShiftPegawai;
	}

	/**
	 * Mengembalikan pegawai pemilik penugasan shift ini, relasi {@code @ManyToOne} lewat kolom FK
	 * {@code pegawai}.
	 *
	 * <p><b>Efek samping:</b> menyegarkan proxy lazy Hibernate lewat {@code check(pegawai)} dan menulis
	 * balik hasilnya ke field {@link #pegawai} — pola standar yang sama seperti
	 * {@link #getJenisShiftPegawai()}.</p>
	 *
	 * @return pegawai pemilik, atau {@code null} bila baris ini milik {@link Mahasiswa}/{@link Siswa}
	 *         (bukan pegawai) atau kolom FK memang kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = true)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/**
	 * Mengaitkan baris penugasan ini dengan pegawai pemilik tertentu. Lihat javadoc kelas untuk catatan
	 * kewaspadaan: entity ini tidak memvalidasi bahwa {@link #mahasiswa}/{@link #siswa} kosong saat
	 * {@code pegawai} diisi (tidak ada constraint exclusivity antar ketiga field pemilik).
	 *
	 * @param pegawai pegawai baru; {@code null} melepas asosiasi
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Mengembalikan keterangan bebas untuk baris penugasan shift ini.
	 *
	 * @return teks keterangan, boleh {@code null}
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Mengisi keterangan bebas untuk baris penugasan shift ini.
	 *
	 * @param keterangan teks keterangan baru, boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan mahasiswa pemilik penugasan shift ini, relasi {@code @ManyToOne} lewat kolom FK
	 * {@code mahasiswa}. Pola efek samping (resolusi proxy lazy, tulis balik ke field) identik dengan
	 * {@link #getPegawai()}.
	 *
	 * @return mahasiswa pemilik, atau {@code null} bila baris ini bukan milik mahasiswa atau kolom FK
	 *         kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * Mengaitkan baris penugasan ini dengan mahasiswa pemilik tertentu. Lihat catatan kewaspadaan pada
	 * {@link #setPegawai(Pegawai)} mengenai tidak adanya constraint exclusivity antar field pemilik.
	 *
	 * @param mahasiswa mahasiswa baru; {@code null} melepas asosiasi
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Mengembalikan siswa pemilik penugasan shift ini, relasi {@code @ManyToOne} lewat kolom FK
	 * {@code siswa}. Pola efek samping identik dengan {@link #getPegawai()}/{@link #getMahasiswa()}.
	 *
	 * <p>Diverifikasi dari {@code DetailJenisShiftPegawaiHelper.applyOwnerRestriction}: dalam pencarian
	 * penugasan aktif, kecocokan {@code siswa} diperiksa dengan prioritas KEDUA — hanya dipakai bila
	 * pemanggil tidak membawa objek {@code mahasiswa} yang valid — lihat javadoc kelas.</p>
	 *
	 * @return siswa pemilik, atau {@code null} bila baris ini bukan milik siswa atau kolom FK kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = true)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	/**
	 * Mengaitkan baris penugasan ini dengan siswa pemilik tertentu. Lihat catatan kewaspadaan pada
	 * {@link #setPegawai(Pegawai)} mengenai tidak adanya constraint exclusivity antar field pemilik.
	 *
	 * @param siswa siswa baru; {@code null} melepas asosiasi
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Mengembalikan baris {@link DetailJenisShiftPegawai} spesifik yang di-pin untuk penugasan ini, relasi
	 * {@code @ManyToOne} lewat kolom FK {@code detail_jenis_shift_pegawai}.
	 *
	 * <p><b>Efek samping:</b> menyegarkan proxy lazy Hibernate lewat {@code check(detailJenisShiftPegawai)}
	 * dan menulis balik hasilnya ke field — pola standar yang sama seperti getter relasi lain di kelas
	 * ini.</p>
	 *
	 * <p>Lihat javadoc kelas untuk peran field ini dalam resolusi shift dua-tahap di
	 * {@code DetailJenisShiftPegawaiHelper.getJenisShiftPunyaPegawai}: baris dengan field ini terisi
	 * diprioritaskan di atas baris penugasan generik saat mencari penugasan aktif suatu pemilik pada
	 * tanggal tertentu.</p>
	 *
	 * @return baris detail shift yang di-pin, atau {@code null} bila penugasan ini mengikuti resolusi
	 *         otomatis baris detail terdekat (tidak di-pin ke baris spesifik)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "detail_jenis_shift_pegawai", nullable = true)
	public DetailJenisShiftPegawai getDetailJenisShiftPegawai() {
		detailJenisShiftPegawai = check(detailJenisShiftPegawai);
		return detailJenisShiftPegawai;
	}

	/**
	 * Mengaitkan (pin) baris penugasan ini ke satu baris {@link DetailJenisShiftPegawai} spesifik.
	 *
	 * @param detailJenisShiftPegawai baris detail shift baru; {@code null} melepas pin (penugasan
	 *                                kembali mengikuti resolusi otomatis)
	 */
	public void setDetailJenisShiftPegawai(DetailJenisShiftPegawai detailJenisShiftPegawai) {
		this.detailJenisShiftPegawai = detailJenisShiftPegawai;
	}

	/**
	 * Mengembalikan flag override "abaikan validasi jarak/geofencing" untuk pemilik penugasan ini, dengan
	 * fallback ke {@code false} bila field belum pernah diset — penugasan baru dianggap TIDAK
	 * melewati validasi jarak secara default (validasi jarak tetap ditegakkan).
	 *
	 * <p>Diverifikasi dipakai nyata di {@code AbsensiApiAction} dan {@code ScanBerhasilAction}: nilai
	 * {@code true} pada flag ini (di-OR-kan dengan {@code StatuskehadiranKaryawanHarian.getAbaikanJarak()})
	 * melewati pembandingan jarak GPS terhadap radius {@link JenisShiftPegawai#getJarak()} — lihat javadoc
	 * kelas untuk detail lengkap kedua titik pemakaian tersebut.</p>
	 *
	 * @return {@code true} bila validasi jarak dilewati untuk penugasan ini, tidak pernah {@code null}
	 */
	public Boolean getAbaikanJarak() {
		return abaikanJarak == null ? false : abaikanJarak;
	}

	/**
	 * Mengisi flag override "abaikan validasi jarak/geofencing" untuk pemilik penugasan ini.
	 *
	 * @param abaikanJarak nilai flag baru
	 */
	public void setAbaikanJarak(Boolean abaikanJarak) {
		this.abaikanJarak = abaikanJarak;
	}

}
