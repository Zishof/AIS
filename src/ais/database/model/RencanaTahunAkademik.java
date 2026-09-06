package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
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

import org.apache.commons.lang.StringUtils;
import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

/**
 * Entity Hibernate/JPA untuk tabel {@code public.rencana_tahun_akademik} — <b>rencana kalender
 * satu tahun akademik/semester</b>: nama periode (format {@code "YYYY/YYYY"}, mis. "2024/2025"),
 * semester ({@link Perkuliahan#GANJIL}/{@link Perkuliahan#GENAP}), rentang tanggal mulai/sampai
 * periode tersebut, tanggal mulai belajar-mengajar, serta cakupan filter (Fakultas/Jurusan/
 * Program/Status Awal Mahasiswa/Tahun Angkatan) dan penanda satuan pendidikan (Yayasan/Sekolah
 * untuk modul sekolah).
 *
 * <p><b>Nama, semester, dan rentang tanggal bersifat derivable</b> — bila tidak diisi eksplisit,
 * ketiganya diturunkan otomatis: {@link #getNama()} dari bulan/tahun berjalan, {@link
 * #getSemester()} dari {@link Common#isNowSemensterGanjil()}, dan {@link #getTanggalMulai()}/
 * {@link #getTanggalSampai()} dari kombinasi nama+semester (1 Maret&ndash;31 Agustus untuk
 * genap, 1 September&ndash;28/29 Februari untuk ganjil). Lihat javadoc masing-masing getter
 * untuk detail penurunan dan efek tulis-baliknya.</p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "rencana_tahun_akademik")
public class RencanaTahunAkademik extends GeneralValueObject {

	/**
	 * ID versi serialisasi Java untuk kompatibilitas antar build (bukan kolom database).
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris {@code rencana_tahun_akademik}, kolom {@code id} (identity, auto-generate). */
	private Long id;
	/** Nama/username aktor yang membuat/terakhir mengubah baris ini (field audit longgar, bukan FK). */
	private String oleh;
	/** ID aktor yang membuat/terakhir mengubah baris ini (pasangan {@link #oleh}, bukan FK). */
	private String olehId;

	/**
	 * @return ID aktor ({@link #olehId}) yang tercatat membuat/mengubah baris ini; boleh {@code null}.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID aktor audit. Setter ini <b>fail-closed diam-diam</b>: nilai {@code null} atau
	 * string kosong/berspasi diabaikan sepenuhnya (nilai lama tetap dipertahankan), tanpa
	 * exception maupun log.
	 *
	 * @param olehId ID aktor baru; nilai kosong/{@code null} tidak berefek
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama aktor audit. Sama seperti {@link #setOlehId(String)}: nilai {@code null} atau
	 * kosong/berspasi diabaikan diam-diam, nilai lama dipertahankan.
	 *
	 * @param oleh nama aktor baru; nilai kosong/{@code null} tidak berefek
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama aktor ({@link #oleh}) yang tercatat membuat/mengubah baris ini; boleh {@code null}.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum {@code
	 * UPDATE} dieksekusi, mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah}
	 * untuk memperbarui jejak audit "terakhir diubah" milik entity ini.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu "terakhir diubah" secara manual. Field ini juga diinisialisasi ke
	 * waktu sekarang saat instance dibuat, dan ditulis ulang otomatis oleh {@link #onUpdate()}
	 * setiap kali baris di-{@code UPDATE}.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return stempel waktu terakhir baris ini diubah (kolom timestamp), diisi otomatis oleh
	 *         {@link #onUpdate()} pada setiap {@code UPDATE}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas untuk log/debug: {@code "<id>-<nama> <tanggalMulai> s.d
	 * <tanggalSampai> <semester>"}.
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #getTanggalMulai()}/{@link #getTanggalSampai()},
	 * yang bisa memicu penurunan-dan-tulis-balik tanggal (lihat javadoc masing-masing getter)
	 * sekadar dari pemanggilan {@code toString()}.</p>
	 *
	 * @return string ringkas identitas periode ini
	 */
	public String toString() {
		return id + "-" + nama + " " + Common.dateFormat4.get().format(getTanggalMulai()) + " s.d "
				+ Common.dateFormat4.get().format(getTanggalSampai()) + " " + semester;
	}

	/** Nama periode, format {@code "YYYY/YYYY"}; lazy-diturunkan dari bulan/tahun berjalan bila kosong, lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas untuk rencana tahun akademik ini. */
	private String keterangan;
	/** Semester periode ini ({@link Perkuliahan#GANJIL}/{@link Perkuliahan#GENAP}); lazy-diturunkan bila kosong, lihat {@link #getSemester()}. */
	private String semester;
	/** Tanggal mulai periode; lazy-diturunkan dari {@link #nama}+{@link #semester} bila kosong, lihat {@link #getTanggalMulai()}. */
	private Date tanggalMulai;
	/** Tanggal sampai periode; lazy-diturunkan dari {@link #nama}+{@link #semester} bila kosong, lihat {@link #getTanggalSampai()}. */
	private Date tanggalSampai;
	/** Tanggal mulai belajar-mengajar (berbeda dari {@link #tanggalMulai} periode administratif); boleh {@code null}. */
	private Date tanggalMulaiBelajarMengajar;

	/** Filter Fakultas (Institusi) cakupan rencana ini. */
	private Fakultas fakultas;
	/** Filter Jurusan (Prodi) cakupan rencana ini. */
	private Jurusan jurusan;
	/** Filter Program (string bebas) cakupan rencana ini. */
	private String program;
	/** Filter Status Awal Mahasiswa cakupan rencana ini. */
	private StatusAwalMahasiswa statusAwalMahasiswa;
	/** Filter tahun angkatan mahasiswa cakupan rencana ini. */
	private Integer tahunAngkatan;

	/** Yayasan (modul sekolah) pemilik rencana ini; lihat {@link #setYayasan(Yayasan)} untuk normalisasi ID kosong. */
	private Yayasan yayasan;
	/** Sekolah (modul sekolah) pemilik rencana ini; lihat {@link #setSekolah(Sekolah)} untuk normalisasi ID kosong. */
	private Sekolah sekolah;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate untuk instansiasi entity via refleksi.
	 */
	public RencanaTahunAkademik() {
	}

	/**
	 * @return primary key baris {@code rencana_tahun_akademik}; {@code null} sebelum baris
	 *         di-{@code INSERT}.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id primary key; biasanya tidak perlu diset manual karena kolomnya {@code
	 *           insertable = false} (identity, dibangkitkan database).
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama periode, format {@code "YYYY/YYYY"}.
	 *
	 * <p><b>Getter yang menulis balik (lazy-derive):</b> bila field mentah {@code null},
	 * dihitung dan disimpan permanen berdasar bulan/tahun BERJALAN (bukan tanggal baris
	 * dibuat): bulan {@code > 5} (Juni ke atas) menghasilkan {@code "<tahunIni>/<tahunIni+1>"},
	 * selebihnya {@code "<tahunIni-1>/<tahunIni>"}. Method ini juga menjadi dasar penurunan
	 * {@link #getTanggalMulai()}/{@link #getTanggalSampai()} (keduanya mem-parse tahun dari
	 * hasil method ini), sehingga membekukan nama pada pembacaan pertama transitif ikut
	 * membekukan rentang tanggal turunan.</p>
	 *
	 * @return nama periode, di-{@code trim()}.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if (nama == null) {
			nama = "";
			if (ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) > 5) {
				nama = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) + "/"
						+ (ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) + 1);
			} else {
				nama = (ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 1) + "/"
						+ (ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));
			}
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * @param nama nama periode baru (format {@code "YYYY/YYYY"}); {@code null} untuk memakai
	 *             penurunan otomatis lewat {@link #getNama()}.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return keterangan bebas periode ini; boleh {@code null}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * @param keterangan keterangan baru untuk periode ini.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Semester periode ini.
	 *
	 * <p><b>Getter yang menulis balik (lazy-derive):</b> bila field mentah {@code null},
	 * ditentukan dan disimpan permanen dari {@link Common#isNowSemensterGanjil()} (semester
	 * SAAT DIBACA, bukan semester saat baris dibuat).</p>
	 *
	 * @return {@link Perkuliahan#GANJIL} atau {@link Perkuliahan#GENAP}; tidak pernah {@code null}.
	 */
	public String getSemester() {
		if (semester == null) {
			semester = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
		}
		return semester;
	}

	/**
	 * @param semester semester baru untuk periode ini.
	 */
	public void setSemester(String semester) {
		this.semester = semester;
	}

	/**
	 * Tanggal mulai periode ini.
	 *
	 * <p><b>Getter yang menulis balik (lazy-derive dua tahap):</b> bila field mentah {@code
	 * null}, dihitung dari {@link #getSemester()}+{@link #getNama()}: semester genap dimulai
	 * 1 Maret pada tahun KEDUA nama periode; semester ganjil dimulai 1 September pada tahun
	 * PERTAMA nama periode. Setelah itu (baik hasil penurunan maupun field yang sudah terisi),
	 * jam/menit/detik SELALU dinormalkan ke awal hari (00:00:00) dan ditulis balik permanen ke
	 * field &mdash; berarti bahkan tanggal yang sudah diset manual dengan komponen waktu tetap
	 * dinormalkan setiap kali getter ini dipanggil.</p>
	 * <p><b>Catatan kode:</b> baris {@code tanggalMulai = calendar.getTime();} muncul dua kali
	 * berturutan pada badan method — duplikasi tanpa efek tambahan (idempoten), tampaknya
	 * artefak salin-tempel; dicatat apa adanya, tidak dibersihkan di sesi dokumentasi ini.</p>
	 *
	 * @return tanggal mulai periode, dinormalkan ke awal hari; tidak pernah {@code null}.
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalMulai() {
		if (tanggalMulai == null) {
			if (getSemester().equals(Perkuliahan.GENAP)) {
				int tahun = Integer.parseInt(StringUtils.split(getNama(), "/")[1]);
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.set(Calendar.DATE, 1);
				calendar.set(Calendar.MONTH, Calendar.MARCH);
				calendar.set(Calendar.YEAR, tahun);
				tanggalMulai = calendar.getTime();
			} else {
				int tahun = Integer.parseInt(StringUtils.split(getNama(), "/")[0]);
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.set(Calendar.DATE, 1);
				calendar.set(Calendar.MONTH, Calendar.SEPTEMBER);
				calendar.set(Calendar.YEAR, tahun);
				tanggalMulai = calendar.getTime();
			}
		}

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(tanggalMulai);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		tanggalMulai = calendar.getTime();

		tanggalMulai = calendar.getTime();

		return tanggalMulai;
	}

	/**
	 * @param tanggalMulai tanggal mulai baru; komponen waktunya akan dinormalkan ke awal hari
	 *                     saat berikutnya dibaca via {@link #getTanggalMulai()}.
	 */
	public void setTanggalMulai(Date tanggalMulai) {
		this.tanggalMulai = tanggalMulai;
	}

	/**
	 * Tanggal sampai periode ini.
	 *
	 * <p><b>Getter yang menulis balik (lazy-derive dua tahap):</b> pola sama seperti {@link
	 * #getTanggalMulai()}, dengan tanggal akhir yang berbeda: semester genap berakhir 31
	 * Agustus pada tahun KEDUA nama periode; semester ganjil berakhir 31... (dikodekan sebagai
	 * tanggal 31 Februari, yang akan di-overflow otomatis oleh {@link Calendar} menjadi awal
	 * Maret pada tahun kabisat/bukan &mdash; lihat catatan di bawah) pada tahun PERTAMA nama
	 * periode. Setelah itu jam/menit/detik SELALU dinormalkan ke akhir hari (23:59:59) dan
	 * ditulis balik permanen ke field.</p>
	 * <p><b>Catatan potensi anomali tanggal:</b> {@code calendar.set(Calendar.DATE, 31)} diikuti
	 * {@code calendar.set(Calendar.MONTH, Calendar.FEBRUARY)} — karena Februari tidak pernah
	 * punya 31 hari, {@link Calendar} akan meluap (overflow) ke bulan berikutnya (awal/
	 * pertengahan Maret, tergantung tahun kabisat), BUKAN 28/29 Februari seperti mungkin
	 * dimaksudkan. Akibatnya tanggal akhir semester ganjil yang dihasilkan bisa meleset
	 * beberapa hari ke bulan Maret. Dicatat apa adanya; tidak diperbaiki di sesi dokumentasi
	 * ini.</p>
	 *
	 * @return tanggal sampai periode, dinormalkan ke akhir hari; tidak pernah {@code null}.
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalSampai() {
		if (tanggalSampai == null) {
			if (getSemester().equals(Perkuliahan.GENAP)) {
				int tahun = Integer.parseInt(StringUtils.split(getNama(), "/")[1]);
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.set(Calendar.DATE, 31);
				calendar.set(Calendar.MONTH, Calendar.AUGUST);
				calendar.set(Calendar.YEAR, tahun);
				tanggalSampai = calendar.getTime();
			} else {
				int tahun = Integer.parseInt(StringUtils.split(getNama(), "/")[0]);
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.set(Calendar.DATE, 31);
				calendar.set(Calendar.MONTH, Calendar.FEBRUARY);
				calendar.set(Calendar.YEAR, tahun);
				tanggalSampai = calendar.getTime();
			}
		}

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(tanggalSampai);
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		tanggalSampai = calendar.getTime();

		return tanggalSampai;
	}

	/**
	 * @param tanggalSampai tanggal sampai baru; komponen waktunya akan dinormalkan ke akhir
	 *                      hari saat berikutnya dibaca via {@link #getTanggalSampai()}.
	 */
	public void setTanggalSampai(Date tanggalSampai) {
		this.tanggalSampai = tanggalSampai;
	}

	/**
	 * @return filter Jurusan (Prodi) cakupan rencana ini (proxy lazy diresolusi via {@code
	 *         check()}); {@code null} berarti tidak difilter berdasarkan jurusan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * @param jurusan filter jurusan baru; {@code null} untuk menghapus filter.
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * @return filter Fakultas (Institusi) cakupan rencana ini (proxy lazy diresolusi via {@code
	 *         check()}); {@code null} berarti tidak difilter berdasarkan fakultas.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * @param fakultas filter fakultas baru; {@code null} untuk menghapus filter.
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * @return filter Program (string bebas) cakupan rencana ini; boleh {@code null}.
	 */
	public String getProgram() {
		return program;
	}

	/**
	 * @param program filter program baru.
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * @return filter Status Awal Mahasiswa cakupan rencana ini (proxy lazy diresolusi via
	 *         {@code check()}); {@code null} berarti tidak difilter berdasarkan status awal.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_awal_mahasiswa", nullable = true)
	public StatusAwalMahasiswa getStatusAwalMahasiswa() {
		statusAwalMahasiswa = check(statusAwalMahasiswa);
		return statusAwalMahasiswa;
	}

	/**
	 * @param statusAwalMahasiswa filter status awal baru; {@code null} untuk menghapus filter.
	 */
	public void setStatusAwalMahasiswa(StatusAwalMahasiswa statusAwalMahasiswa) {
		this.statusAwalMahasiswa = statusAwalMahasiswa;
	}

	/**
	 * @return filter tahun angkatan mahasiswa cakupan rencana ini; boleh {@code null}.
	 */
	public Integer getTahunAngkatan() {
		return tahunAngkatan;
	}

	/**
	 * @param tahunAngkatan filter tahun angkatan baru.
	 */
	public void setTahunAngkatan(Integer tahunAngkatan) {
		this.tahunAngkatan = tahunAngkatan;
	}

	/**
	 * Menautkan yayasan pemilik rencana ini. Berbeda dari kebanyakan setter relasi lain di
	 * kelas ini, setter ini MENORMALKAN input: yayasan yang {@code null} ATAU yang ID-nya
	 * {@code null} (belum tersimpan/transient) sama-sama disimpan sebagai {@code null} pada
	 * field, mencegah tautan ke entity yayasan yang belum ter-{@code persist}.
	 *
	 * @param yayasan yayasan baru; entity tanpa ID diperlakukan sama seperti {@code null}.
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * @return yayasan (modul sekolah) pemilik rencana ini (proxy lazy diresolusi via {@code
	 *         check()}); boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		yayasan = check(yayasan);
		return yayasan;
	}

	/**
	 * @return sekolah (modul sekolah) pemilik rencana ini (proxy lazy diresolusi via {@code
	 *         check()}); boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return sekolah;
	}

	/**
	 * Menautkan sekolah pemilik rencana ini. Normalisasi input sama seperti {@link
	 * #setYayasan(Yayasan)}: sekolah {@code null} atau tanpa ID disimpan sebagai {@code null}.
	 *
	 * @param sekolah sekolah baru; entity tanpa ID diperlakukan sama seperti {@code null}.
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * @return tanggal mulai belajar-mengajar (berbeda dari {@link #getTanggalMulai()} periode
	 *         administratif); boleh {@code null}.
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_mulai_belajar_mengajar", nullable = true)
	public Date getTanggalMulaiBelajarMengajar() {
		return tanggalMulaiBelajarMengajar;
	}

	/**
	 * @param tanggalMulaiBelajarMengajar tanggal mulai belajar-mengajar baru.
	 */
	public void setTanggalMulaiBelajarMengajar(Date tanggalMulaiBelajarMengajar) {
		this.tanggalMulaiBelajarMengajar = tanggalMulaiBelajarMengajar;
	}

}
