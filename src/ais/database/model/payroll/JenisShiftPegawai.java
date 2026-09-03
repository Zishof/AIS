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

import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.asset.Lokasi;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.WaktuUtil;

/**
 * Katalog/master <b>jenis shift kerja</b> pegawai — mis. "Shift Pagi", "Shift Malam", "Shift
 * Rotasi 3-2-3" — dipetakan ke tabel <code>payroll.jenis_shift_pegawai</code>. Kelas ini adalah
 * <b>header/template</b>: ia sendiri tidak menyimpan jam masuk/pulang. Jam per hari/putaran
 * disimpan sebagai baris anak di {@link DetailJenisShiftPegawai} (relasi
 * {@code DetailJenisShiftPegawai.jenisShiftPegawai}, <code>@ManyToOne</code> — satu jenis shift
 * bisa punya banyak baris detail, satu per hari-ke dalam siklus rotasi).
 *
 * <h3>1. Rantai relasi shift → absensi → gaji (diverifikasi dari kode)</h3>
 * <p>Entity ini <b>tidak</b> berelasi FK langsung ke pegawai maupun ke catatan kehadiran harian.
 * Rantainya berlapis tiga:</p>
 * <ol>
 *   <li>{@code JenisShiftPegawai} (kelas ini) — katalog jenis shift, satu baris per "jenis".</li>
 *   <li>{@link JenisShiftPunyaPegawai} — baris penugasan: memasangkan satu {@code JenisShiftPegawai}
 *       ke satu {@code Pegawai} (atau Mahasiswa/Siswa) untuk suatu rentang berlaku.</li>
 *   <li>{@link DetailJenisShiftPegawai} — baris jam per hari-ke dalam siklus shift tersebut
 *       ({@code mulai}, {@code sampai}, {@code jumlahSecond}, toleransi telat/pulang cepat,
 *       potongan telat 1-4). Baris inilah yang benar-benar dirujuk oleh mesin absensi harian
 *       <code>StatuskehadiranKaryawanHarian</code> (field {@code detailJenisShiftPegawai} dan
 *       {@code detailJenisShiftPegawaiLembur}) untuk menghitung telat, pulang cepat, dan lembur —
 *       dan angka-angka itulah yang pada akhirnya mengalir ke transaksi/potongan gaji pegawai.
 *       {@code JenisShiftPegawai} sendiri hanya dijangkau secara tidak langsung dari sana lewat
 *       {@code detailJenisShiftPegawai.getJenisShiftPegawai()} — terutama untuk membaca
 *       {@link #getWaktuBekerjaMinimal()} (ambang jam kerja minimal supaya kehadiran dihitung sah)
 *       dan {@link #getHarusMengikutiStateMasukDanPulang()} (aturan urutan state absen).</li>
 * </ol>
 * <p><b>Catatan penting:</b> entity {@code AbsenPegawaiDetail} di package yang sama (blob teks
 * absensi bulanan per pegawai) — meskipun namanya mirip dan sama-sama "payroll" — <b>bukan</b>
 * bagian dari rantai ini. Tidak ada satu pun referensi silang antara {@code JenisShiftPegawai}/
 * {@code DetailJenisShiftPegawai} dan {@code AbsenPegawaiDetail} di seluruh kode; keduanya adalah
 * konsep serumpun (sama-sama "absensi pegawai") tanpa link struktural. Lihat javadoc kelas
 * {@code AbsenPegawaiDetail} untuk detail temuan bahwa entity itu tampaknya tidak lagi dipakai
 * mesin absensi aktual.</p>
 *
 * <h3>2. Mekanisme "shift default" — penugasan implisit tanpa baris JenisShiftPunyaPegawai</h3>
 * <p>Lima flag {@code defaultAbsenGuru}, {@code defaultAbsenDosen}, {@code defaultAbsenPegawai},
 * {@code defaultSiswa}, {@code defaultMahasiswa} membuat sebuah {@code JenisShiftPegawai} menjadi
 * <b>shift jatuh-balik (fallback)</b> untuk kategori orang tersebut ketika mereka <b>tidak</b>
 * punya baris {@link JenisShiftPunyaPegawai} eksplisit. Resolusinya dilakukan
 * <code>ais.common.DetailJenisShiftPegawaiHelper</code> (method privat
 * <code>getDefaultShiftIds</code>), diverifikasi dari kode sebagai berikut:</p>
 * <ul>
 *   <li>Guru — dicari bertingkat: cocok <code>sekolah</code>+<code>yayasan</code>, lalu hanya
 *       <code>yayasan</code>, lalu tanpa keduanya (shift default umum untuk semua guru).</li>
 *   <li>Siswa — pola sama, discope oleh <code>sekolah</code>/<code>yayasan</code> milik siswa.</li>
 *   <li>Mahasiswa — discope oleh <code>jurusan</code>/<code>fakultas</code>.</li>
 *   <li>Dosen dan Pegawai (non-guru) — <b>tidak</b> discope sama sekali; hanya
 *       <code>defaultAbsenDosen = true</code> atau <code>defaultAbsenPegawai = true</code> tanpa
 *       filter organisasi lain. Bila lebih dari satu baris punya flag ini tanpa scoping, hasilnya
 *       tak terprediksi (bergantung urutan query).</li>
 * </ul>
 * <p>Semua pencarian itu juga memfilter validitas terhadap {@link #getBerlakuMulai()}/
 * {@link #getBerlakuSampai()} pada tanggal yang diminta — shift default yang sudah kedaluwarsa
 * ({@code berlakuSampai} terlewati) tidak lagi ikut dipilih.</p>
 *
 * <h3>3. Geofencing lokasi — hingga 10 titik, radius dalam kilometer</h3>
 * <p>Sepuluh field {@link #getLokasi() lokasi}..{@link #getLokasi10() lokasi10} menampung hingga
 * sepuluh titik {@link Lokasi} (koordinat) yang sah untuk absen pada shift ini — dipakai untuk
 * absen multi-cabang/multi-gedung. {@link #getJarak()} adalah radius toleransi maksimum dalam
 * <b>kilometer</b>. Diverifikasi dipakai nyata di
 * <code>ais.action.servlet.api.AbsensiApiAction</code> (endpoint absen mobile/API): jarak GPS
 * pegawai ke titik {@code Lokasi} terdekat (dihitung lewat
 * {@code DetailJenisShiftPegawai.ambilJarakDanLokasiTerdekat}, yang mengumpulkan
 * {@code lokasi}..{@code lokasi10} dari {@code JenisShiftPegawai} induknya) dibandingkan terhadap
 * {@link #getJarak()}; bila melebihi, absen ditolak dengan status "91" dan pesan jarak dalam km.
 * Pembanding ini bisa dilewati per-pegawai lewat {@code JenisShiftPunyaPegawai.getAbaikanJarak()}
 * atau {@code StatuskehadiranKaryawanHarian.getAbaikanJarak()}.</p>
 *
 * <h3>4. Pola arsitektur berulang di kelas ini</h3>
 * <ul>
 *   <li><b>Field audit shadow</b> — {@link #getOleh()}/{@link #getOlehId()}/
 *       {@link #getTanggal_dirubah()} tidak beranotasi <code>@Column</code>; ini keharusan
 *       teknis karena {@link GeneralValueObject} bukan <code>@Entity</code> Hibernate murni,
 *       bukan bug (pola yang sama berulang di seluruh model AIS).</li>
 *   <li><b>Getter destruktif berantai</b> — {@link #getYayasan()} menimpa dirinya dari
 *       {@link #getSekolah()}.{@code getYayasan()} bila sekolah terisi, dan {@link #getFakultas()}
 *       menimpa dirinya dari {@link #getJurusan()}.{@code getFakultas()} bila jurusan terisi.
 *       Efeknya: field {@code yayasan}/{@code fakultas} yang tersimpan eksplisit bisa
 *       "kalah" secara diam-diam oleh turunan dari sekolah/jurusan setiap kali getter dipanggil
 *       dalam sesi Hibernate aktif (pola tulis-balik getter yang sama seperti pada
 *       {@code TransaksiPegawai}).</li>
 *   <li><b>Filter tenant/satuan-kerja</b> — tidak ada kolom <code>satuanKerja</code>/tenant
 *       eksplisit pada entity ini; pemisahan multi-yayasan/multi-sekolah semata bertumpu pada
 *       field {@code yayasan}/{@code sekolah}/{@code fakultas}/{@code jurusan} di atas, yang
 *       nullable — baris tanpa scoping berlaku lintas seluruh tenant untuk flag default-nya.</li>
 * </ul>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "payroll", name = "jenis_shift_pegawai")
public class JenisShiftPegawai extends GeneralValueObject {

	/**
	 * Versi serialisasi Java untuk kompatibilitas {@link java.io.Serializable}. Nilainya tidak
	 * pernah diubah manual sejak generate awal; tidak berkaitan dengan skema tabel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci primer baris jenis shift, auto-increment ({@code IDENTITY}) pada kolom {@code id}. */
	private Long id;

	/**
	 * Nama/username pengguna yang terakhir membuat/mengubah baris ini. Field audit shadow — lihat
	 * catatan pola arsitektur pada javadoc kelas.
	 */
	private String oleh;

	/**
	 * ID pengguna (mis. ID pegawai/akun) yang terakhir membuat/mengubah baris ini. Field audit
	 * shadow — lihat catatan pola arsitektur pada javadoc kelas.
	 */
	private String olehId;

	/**
	 * Mengambil ID pengguna yang terakhir mengubah baris ini.
	 *
	 * @return ID pengguna, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna yang mengubah baris ini. Nilai {@code null} atau string kosong/blank
	 * <b>diabaikan</b> (dibiarkan tidak berubah) — bukan ditimpa jadi {@code null}, sehingga jejak
	 * audit sebelumnya tidak hilang begitu saja oleh pemanggilan dengan nilai kosong.
	 *
	 * @param olehId ID pengguna baru; diabaikan bila {@code null} atau kosong setelah di-trim.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Representasi string singkat entity ini, dipakai kombo/label UI: hanya {@link #getNama()}
	 * (mengakses field {@code nama} langsung, bukan lewat getter, sehingga tidak memicu logika
	 * apa pun — namun juga bisa mengembalikan {@code null} bila field belum pernah diset).
	 *
	 * @return nama jenis shift, boleh {@code null}.
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Menyetel nama/username pengguna yang mengubah baris ini. Nilai {@code null} atau
	 * kosong/blank <b>diabaikan</b>, sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna baru; diabaikan bila {@code null} atau kosong setelah di-trim.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama/username pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum statement
	 * UPDATE dikirim. Mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang bertugas
	 * memperbarui stempel waktu perubahan ({@link #getTanggal_dirubah()}) dan/atau field audit
	 * terkait secara terpusat, sehingga logikanya seragam untuk seluruh entity yang memakai
	 * interceptor yang sama.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir pada baris ini. Diinisialisasi ke waktu server saat
	 * instance dibuat ({@link ais.ui.util.WaktuUtil#getDate()}), lalu diperbarui otomatis oleh
	 * {@link #onUpdate()} pada setiap UPDATE.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan secara manual. Umumnya tidak perlu dipanggil langsung
	 * karena {@link #onUpdate()} sudah mengurusnya otomatis pada setiap UPDATE.
	 *
	 * @param tanggal_dirubah stempel waktu baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil stempel waktu perubahan terakhir baris ini.
	 *
	 * @return tanggal/jam perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nama jenis shift yang ditampilkan ke pengguna, mis. "Shift Pagi". Wajib diisi (not-null). */
	private String nama;

	/** Keterangan/deskripsi bebas untuk jenis shift ini. Opsional. */
	private String keterangan;

	/**
	 * Jumlah slot shift berbeda dalam satu siklus rotasi (mis. 3 untuk pola "Shift 1/2/3").
	 * Default 1 (shift tunggal, tidak berotasi). Lihat juga {@link #getBerotasi()} dan
	 * {@link #getJumlahHari()}.
	 */
	private Integer jumlahShift = 1;

	/**
	 * Jumlah hari dalam satu siklus rotasi shift. Default 1. Nilai efektifnya bisa ditimpa
	 * otomatis oleh {@link #getJumlahHari()} bila {@link #getJumlahHariSamaDenganJumlahShift()}
	 * bernilai true — lihat javadoc method tersebut.
	 */
	private Integer jumlahHari = 1;

	/**
	 * Flag: apakah jumlah hari dalam siklus rotasi otomatis disamakan dengan
	 * {@link #getJumlahShift()} (true), atau dikonfigurasi terpisah lewat
	 * {@link #getJumlahHari()} (false, khusus rotasi yang jumlah harinya tidak sama dengan
	 * jumlah slot shift). Lihat getter untuk logika penentuan nilai efektifnya.
	 */
	private Boolean jumlahHariSamaDenganJumlahShift;

	/**
	 * Batas jam kedatangan supaya pegawai masih dianggap berhak atas uang makan datang. Diambil
	 * hanya bagian jamnya (field ini bertipe {@link Date} penuh, tapi secara konvensi tanggalnya
	 * diabaikan oleh pemanggil — hanya jam:menit yang relevan). Diinisialisasi ke waktu server
	 * saat instance dibuat.
	 */
	private Date dapatUangMakanDatangSebelumJam = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Tanggal mulai berlakunya konfigurasi jenis shift ini. Diinisialisasi ke waktu server saat
	 * instance dibuat. Dipakai bersama {@link #berlakuSampai} sebagai jendela validitas saat
	 * mesin shift-default ({@code DetailJenisShiftPegawaiHelper}) mencari kandidat shift untuk
	 * suatu tanggal — lihat javadoc kelas bagian 2.
	 */
	private Date berlakuMulai = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Tanggal berakhirnya masa berlaku konfigurasi jenis shift ini. {@code null} berarti berlaku
	 * tanpa batas akhir.
	 */
	private Date berlakuSampai;

	/**
	 * Titik lokasi geofencing ke-1 untuk absen pada shift ini. Lihat {@link #getLokasi()} dan
	 * javadoc kelas bagian 3 untuk mekanisme geofencing multi-lokasi.
	 */
	private Lokasi lokasi;

	/** Titik lokasi geofencing ke-2. Lihat {@link #getLokasi2()}. */
	private Lokasi lokasi2;

	/** Titik lokasi geofencing ke-3. Lihat {@link #getLokasi3()}. */
	private Lokasi lokasi3;

	/** Titik lokasi geofencing ke-4. Lihat {@link #getLokasi4()}. */
	private Lokasi lokasi4;

	/** Titik lokasi geofencing ke-5. Lihat {@link #getLokasi5()}. */
	private Lokasi lokasi5;

	/** Titik lokasi geofencing ke-6. Lihat {@link #getLokasi6()}. */
	private Lokasi lokasi6;

	/** Titik lokasi geofencing ke-7. Lihat {@link #getLokasi7()}. */
	private Lokasi lokasi7;

	/** Titik lokasi geofencing ke-8. Lihat {@link #getLokasi8()}. */
	private Lokasi lokasi8;

	/** Titik lokasi geofencing ke-9. Lihat {@link #getLokasi9()}. */
	private Lokasi lokasi9;

	/** Titik lokasi geofencing ke-10. Lihat {@link #getLokasi10()}. */
	private Lokasi lokasi10;

	/**
	 * Radius toleransi geofencing dalam <b>kilometer</b>, diukur dari titik {@link Lokasi}
	 * terdekat di antara {@link #getLokasi()}..{@link #getLokasi10()}. Default efektif 1.0 km
	 * (lihat {@link #getJarak()}). Diverifikasi dipakai nyata sebagai ambang penolakan absen
	 * mobile/API di {@code AbsensiApiAction} — lihat javadoc kelas bagian 3.
	 */
	private Double jarak;

	/**
	 * Ambang jam kerja minimal (dalam jam, pecahan diperbolehkan) supaya suatu kehadiran dianggap
	 * sah/dihitung. Default efektif 3.0 jam (lihat {@link #getWaktuBekerjaMinimal()}). Dibaca oleh
	 * mesin absensi harian ({@code StatuskehadiranKaryawanHarian}, {@code ProsesAbsensiPegawai})
	 * lewat rute {@code detailJenisShiftPegawai.getJenisShiftPegawai().getWaktuBekerjaMinimal()}.
	 */
	private Double waktuBekerjaMinimal;

	/**
	 * Flag: jadikan jenis shift ini sebagai shift jatuh-balik (default) untuk <b>guru</b> yang
	 * belum punya penugasan shift eksplisit ({@link JenisShiftPunyaPegawai}). Discope oleh
	 * {@link #getSekolah()}/{@link #getYayasan()} — lihat javadoc kelas bagian 2.
	 */
	private Boolean defaultAbsenGuru;

	/**
	 * Flag: jadikan jenis shift ini sebagai shift jatuh-balik (default) untuk <b>dosen</b> yang
	 * belum punya penugasan shift eksplisit. Diverifikasi dari kode <b>tidak</b> discope oleh
	 * fakultas/jurusan sama sekali — lihat catatan risiko pada javadoc kelas bagian 2.
	 */
	private Boolean defaultAbsenDosen;

	/**
	 * Flag: jadikan jenis shift ini sebagai shift jatuh-balik (default) untuk <b>pegawai</b>
	 * (non-guru/dosen) yang belum punya penugasan shift eksplisit. Diverifikasi dari kode
	 * <b>tidak</b> discope oleh unit organisasi apa pun — lihat catatan risiko pada javadoc kelas
	 * bagian 2.
	 */
	private Boolean defaultAbsenPegawai;

	/**
	 * Flag: jadikan jenis shift ini sebagai shift jatuh-balik (default) untuk <b>siswa</b> yang
	 * belum punya penugasan shift eksplisit. Discope oleh {@link #getSekolah()}/
	 * {@link #getYayasan()}.
	 */
	private Boolean defaultSiswa;

	/**
	 * Flag: jadikan jenis shift ini sebagai shift jatuh-balik (default) untuk <b>mahasiswa</b>
	 * yang belum punya penugasan shift eksplisit. Discope oleh {@link #getJurusan()}/
	 * {@link #getFakultas()}.
	 */
	private Boolean defaultMahasiswa;

	/**
	 * Yayasan pemilik/pemakai konfigurasi jenis shift ini, untuk scoping shift-default guru/siswa.
	 * Nilai efektifnya bisa ditimpa oleh yayasan milik {@link #getSekolah()} — lihat
	 * {@link #getYayasan()}.
	 */
	private Yayasan yayasan;

	/**
	 * Sekolah pemilik/pemakai konfigurasi jenis shift ini, untuk scoping shift-default guru/siswa.
	 */
	private Sekolah sekolah;

	/**
	 * Fakultas pemilik/pemakai konfigurasi jenis shift ini, untuk scoping shift-default
	 * dosen/mahasiswa. Nilai efektifnya bisa ditimpa oleh fakultas milik {@link #getJurusan()} —
	 * lihat {@link #getFakultas()}.
	 */
	private Fakultas fakultas;

	/**
	 * Jurusan pemilik/pemakai konfigurasi jenis shift ini, untuk scoping shift-default mahasiswa.
	 */
	private Jurusan jurusan;

	/**
	 * Flag: apakah jenis shift ini berotasi (siklus berganti-ganti jam/hari, mis. shift pagi
	 * minggu ini lalu shift malam minggu depan) atau tetap/statis. Menentukan nilai efektif
	 * {@link #getJumlahHariSamaDenganJumlahShift()} — lihat getter tersebut.
	 */
	private Boolean berotasi;

	/**
	 * Flag: apakah hari libur untuk shift ini ditentukan secara eksplisit (per baris
	 * {@link DetailJenisShiftPegawai}) alih-alih mengikuti pola hari libur mingguan default.
	 */
	private Boolean hariLiburDitentukan;

	/**
	 * Flag status aktif/nonaktif jenis shift ini. Default efektif true (aktif) bila belum pernah
	 * diset — lihat {@link #getAktif()}.
	 */
	private Boolean aktif;

	/**
	 * Flag: apakah kehadiran pada shift ini wajib mengikuti urutan state masuk-lalu-pulang yang
	 * ketat (mis. tidak boleh "pulang" sebelum ada "masuk" yang tercatat). Dibaca oleh
	 * {@code ProsesAbsensiPegawai} lewat rute
	 * {@code detailJenisShiftPegawai.getJenisShiftPegawai().getHarusMengikutiStateMasukDanPulang()}
	 * untuk memutuskan apakah validasi urutan state itu ditegakkan.
	 */
	private Boolean harusMengikutiStateMasukDanPulang;

	/**
	 * Konstruktor default (kosong), dipakai Hibernate saat instansiasi entity dari hasil query
	 * serta oleh kode aplikasi saat membuat baris jenis shift baru sebelum field-nya diisi.
	 */
	public JenisShiftPegawai() {
	}

	/**
	 * Mengambil kunci primer baris ini.
	 *
	 * @return ID baris, atau {@code null} bila belum tersimpan (transient).
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci primer baris ini. Kolomnya {@code insertable = false} pada mapping Hibernate
	 * (nilai dihasilkan DB lewat IDENTITY), jadi setter ini normalnya hanya relevan saat Hibernate
	 * mengisi field dari hasil query, bukan untuk di-set manual sebelum insert.
	 *
	 * @param id ID baru.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil nama jenis shift.
	 *
	 * @return nama jenis shift, mis. "Shift Pagi".
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menyetel nama jenis shift. Kolom {@code nama} bersifat wajib (not-null) di database, jadi
	 * baris tanpa nama akan gagal disimpan.
	 *
	 * @param nama nama baru jenis shift.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil keterangan/deskripsi bebas jenis shift ini.
	 *
	 * @return keterangan, boleh {@code null}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan/deskripsi bebas jenis shift ini.
	 *
	 * @param keterangan keterangan baru, boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengambil jumlah slot shift dalam satu siklus rotasi, dengan fallback ke 1 (shift tunggal)
	 * bila field belum pernah diset. <b>Catatan:</b> getter ini menulis balik nilai fallback ke
	 * field {@code jumlahShift} (bukan sekadar mengembalikan nilai literal), sehingga instance
	 * yang baru dibuat lalu di-{@code getJumlahShift()} akan punya field {@code jumlahShift = 1}
	 * secara permanen sejak pemanggilan pertama.
	 *
	 * @return jumlah slot shift, tidak pernah {@code null} (minimal 1).
	 */
	public Integer getJumlahShift() {
		if (jumlahShift == null) {
			jumlahShift = 1;
		}
		return jumlahShift;
	}

	/**
	 * Menyetel jumlah slot shift dalam satu siklus rotasi.
	 *
	 * @param jumlahShift jumlah slot shift baru.
	 */
	public void setJumlahShift(Integer jumlahShift) {
		this.jumlahShift = jumlahShift;
	}

	/**
	 * Mengambil tanggal mulai berlakunya konfigurasi ini, dengan fallback ke
	 * {@link WaktuUtil#kemarin() kemarin} (bukan hari ini) bila field belum pernah diset. Berbeda
	 * dengan {@link #getJumlahShift()}, getter ini <b>tidak</b> menulis balik nilai fallback ke
	 * field — {@code berlakuMulai} tetap {@code null} di memori/DB, hanya nilai kembaliannya yang
	 * disamarkan jadi "kemarin".
	 *
	 * @return tanggal mulai berlaku; {@code WaktuUtil.kemarin()} bila field-nya {@code null}.
	 */
	public Date getBerlakuMulai() {
		return berlakuMulai == null ? WaktuUtil.kemarin() : berlakuMulai;
	}

	/**
	 * Menyetel tanggal mulai berlakunya konfigurasi jenis shift ini.
	 *
	 * @param berlakuMulai tanggal mulai berlaku baru.
	 */
	public void setBerlakuMulai(Date berlakuMulai) {
		this.berlakuMulai = berlakuMulai;
	}

	/**
	 * Mengambil tanggal berakhirnya masa berlaku konfigurasi ini.
	 *
	 * @return tanggal berakhir, atau {@code null} bila berlaku tanpa batas akhir.
	 */
	public Date getBerlakuSampai() {
		return berlakuSampai;
	}

	/**
	 * Menyetel tanggal berakhirnya masa berlaku konfigurasi jenis shift ini.
	 *
	 * @param berlakuSampai tanggal berakhir baru; {@code null} berarti tanpa batas akhir.
	 */
	public void setBerlakuSampai(Date berlakuSampai) {
		this.berlakuSampai = berlakuSampai;
	}

	/**
	 * Mengambil batas jam kedatangan untuk berhak atas uang makan datang.
	 *
	 * @return jam batas kedatangan (bagian tanggal pada {@link Date} ini konvensinya diabaikan).
	 */
	public Date getDapatUangMakanDatangSebelumJam() {
		return dapatUangMakanDatangSebelumJam;
	}

	/**
	 * Menyetel batas jam kedatangan untuk berhak atas uang makan datang.
	 *
	 * @param dapatUangMakanDatangSebelumJam jam batas baru.
	 */
	public void setDapatUangMakanDatangSebelumJam(Date dapatUangMakanDatangSebelumJam) {
		this.dapatUangMakanDatangSebelumJam = dapatUangMakanDatangSebelumJam;
	}

	/**
	 * Mengambil titik lokasi geofencing ke-1 untuk shift ini, sambil menyegarkan proxy Hibernate
	 * lewat {@code check(lokasi)} (pola standar di seluruh model AIS untuk relasi lazy). Titik ini
	 * adalah salah satu dari hingga 10 titik ({@link #getLokasi()}..{@link #getLokasi10()}) yang
	 * dikumpulkan {@code DetailJenisShiftPegawai.ambilJarakDanLokasiTerdekat} untuk dicari yang
	 * terdekat dari koordinat GPS pegawai saat absen — lihat javadoc kelas bagian 3.
	 *
	 * @return lokasi ke-1, boleh {@code null} bila tidak dikonfigurasi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi", nullable = true)
	public Lokasi getLokasi() {
		lokasi = check(lokasi);
		return lokasi;
	}

	/**
	 * Menyetel titik lokasi geofencing ke-1.
	 *
	 * @param lokasi lokasi baru, boleh {@code null} untuk menghapus titik ini.
	 */
	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * Mengambil titik lokasi geofencing ke-2. Lihat {@link #getLokasi()} untuk penjelasan
	 * mekanisme geofencing multi-lokasi.
	 *
	 * @return lokasi ke-2, boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi2", nullable = true)
	public Lokasi getLokasi2() {
		lokasi2 = check(lokasi2);
		return lokasi2;
	}

	/**
	 * Menyetel titik lokasi geofencing ke-2.
	 *
	 * @param lokasi2 lokasi baru, boleh {@code null}.
	 */
	public void setLokasi2(Lokasi lokasi2) {
		this.lokasi2 = lokasi2;
	}

	/**
	 * Mengambil titik lokasi geofencing ke-3. Lihat {@link #getLokasi()}.
	 *
	 * @return lokasi ke-3, boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi3", nullable = true)
	public Lokasi getLokasi3() {
		lokasi3 = check(lokasi3);
		return lokasi3;
	}

	/**
	 * Menyetel titik lokasi geofencing ke-3.
	 *
	 * @param lokasi3 lokasi baru, boleh {@code null}.
	 */
	public void setLokasi3(Lokasi lokasi3) {
		this.lokasi3 = lokasi3;
	}

	/**
	 * Mengambil titik lokasi geofencing ke-4. Lihat {@link #getLokasi()}.
	 *
	 * @return lokasi ke-4, boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi4", nullable = true)
	public Lokasi getLokasi4() {
		lokasi4 = check(lokasi4);
		return lokasi4;
	}

	/**
	 * Menyetel titik lokasi geofencing ke-4.
	 *
	 * @param lokasi4 lokasi baru, boleh {@code null}.
	 */
	public void setLokasi4(Lokasi lokasi4) {
		this.lokasi4 = lokasi4;
	}

	/**
	 * Mengambil titik lokasi geofencing ke-5. Lihat {@link #getLokasi()}.
	 *
	 * @return lokasi ke-5, boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi5", nullable = true)
	public Lokasi getLokasi5() {
		lokasi5 = check(lokasi5);
		return lokasi5;
	}

	/**
	 * Menyetel titik lokasi geofencing ke-5.
	 *
	 * @param lokasi5 lokasi baru, boleh {@code null}.
	 */
	public void setLokasi5(Lokasi lokasi5) {
		this.lokasi5 = lokasi5;
	}

	/**
	 * Mengambil radius toleransi geofencing dalam kilometer, dengan fallback ke 1.0 km bila
	 * belum pernah diset. Diverifikasi dipakai nyata sebagai ambang penolakan absen mobile/API —
	 * lihat javadoc kelas bagian 3 dan {@code AbsensiApiAction} (perbandingan
	 * {@code jarakKm > jenis.getJenisShiftPegawai().getJarak()}).
	 *
	 * @return radius toleransi dalam km, tidak pernah {@code null}.
	 */
	public Double getJarak() {
		return jarak == null ? 1.0 : jarak;
	}

	/**
	 * Menyetel radius toleransi geofencing dalam kilometer.
	 *
	 * @param jarak radius baru dalam km.
	 */
	public void setJarak(Double jarak) {
		this.jarak = jarak;
	}

	/**
	 * Mengambil status aktif/nonaktif jenis shift ini, dengan fallback ke {@code true} (aktif)
	 * bila belum pernah diset — jenis shift baru dianggap aktif secara default.
	 *
	 * @return {@code true} bila aktif, tidak pernah {@code null}.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif/nonaktif jenis shift ini.
	 *
	 * @param aktif status baru.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengambil flag shift-default untuk guru, dengan fallback ke {@code false} bila belum pernah
	 * diset. Lihat javadoc kelas bagian 2 untuk mekanisme resolusi shift-default.
	 *
	 * @return {@code true} bila jenis shift ini adalah default untuk guru, tidak pernah
	 *         {@code null}.
	 */
	public Boolean getDefaultAbsenGuru() {
		return defaultAbsenGuru == null ? false : defaultAbsenGuru;
	}

	/**
	 * Menyetel flag shift-default untuk guru.
	 *
	 * @param defaultAbsenGuru nilai flag baru.
	 */
	public void setDefaultAbsenGuru(Boolean defaultAbsenGuru) {
		this.defaultAbsenGuru = defaultAbsenGuru;
	}

	/**
	 * Mengambil flag shift-default untuk dosen, dengan fallback ke {@code false}. Diverifikasi
	 * dari kode: flag ini <b>tidak</b> discope oleh fakultas/jurusan sama sekali — lihat javadoc
	 * kelas bagian 2.
	 *
	 * @return {@code true} bila jenis shift ini adalah default untuk dosen, tidak pernah
	 *         {@code null}.
	 */
	public Boolean getDefaultAbsenDosen() {
		return defaultAbsenDosen == null ? false : defaultAbsenDosen;
	}

	/**
	 * Menyetel flag shift-default untuk dosen.
	 *
	 * @param defaultAbsenDosen nilai flag baru.
	 */
	public void setDefaultAbsenDosen(Boolean defaultAbsenDosen) {
		this.defaultAbsenDosen = defaultAbsenDosen;
	}

	/**
	 * Mengambil flag shift-default untuk pegawai (non-guru/dosen), dengan fallback ke
	 * {@code false}. Diverifikasi dari kode: flag ini <b>tidak</b> discope oleh unit organisasi
	 * apa pun — lihat javadoc kelas bagian 2.
	 *
	 * @return {@code true} bila jenis shift ini adalah default untuk pegawai, tidak pernah
	 *         {@code null}.
	 */
	public Boolean getDefaultAbsenPegawai() {
		return defaultAbsenPegawai == null ? false : defaultAbsenPegawai;
	}

	/**
	 * Menyetel flag shift-default untuk pegawai.
	 *
	 * @param defaultAbsenPegawai nilai flag baru.
	 */
	public void setDefaultAbsenPegawai(Boolean defaultAbsenPegawai) {
		this.defaultAbsenPegawai = defaultAbsenPegawai;
	}

	/**
	 * Mengambil flag shift-default untuk siswa, dengan fallback ke {@code false}. Discope oleh
	 * {@link #getSekolah()}/{@link #getYayasan()} — lihat javadoc kelas bagian 2.
	 *
	 * @return {@code true} bila jenis shift ini adalah default untuk siswa, tidak pernah
	 *         {@code null}.
	 */
	public Boolean getDefaultSiswa() {
		return defaultSiswa == null ? false : defaultSiswa;
	}

	/**
	 * Menyetel flag shift-default untuk siswa.
	 *
	 * @param defaultSiswa nilai flag baru.
	 */
	public void setDefaultSiswa(Boolean defaultSiswa) {
		this.defaultSiswa = defaultSiswa;
	}

	/**
	 * Mengambil flag shift-default untuk mahasiswa, dengan fallback ke {@code false}. Discope
	 * oleh {@link #getJurusan()}/{@link #getFakultas()} — lihat javadoc kelas bagian 2.
	 *
	 * @return {@code true} bila jenis shift ini adalah default untuk mahasiswa, tidak pernah
	 *         {@code null}.
	 */
	public Boolean getDefaultMahasiswa() {
		return defaultMahasiswa == null ? false : defaultMahasiswa;
	}

	/**
	 * Menyetel flag shift-default untuk mahasiswa.
	 *
	 * @param defaultMahasiswa nilai flag baru.
	 */
	public void setDefaultMahasiswa(Boolean defaultMahasiswa) {
		this.defaultMahasiswa = defaultMahasiswa;
	}

	/**
	 * Mengambil yayasan scoping untuk konfigurasi ini. <b>Getter destruktif berantai:</b> bila
	 * {@link #getSekolah()} terisi, nilai {@code yayasan} <b>ditimpa</b> oleh yayasan pemilik
	 * sekolah tersebut ({@code getSekolah().getYayasan()}) — field {@code yayasan} yang tersimpan
	 * eksplisit di baris ini bisa "kalah" secara diam-diam setiap kali getter dipanggil dalam
	 * sesi Hibernate aktif. Lihat javadoc kelas bagian 4.
	 *
	 * @return yayasan efektif (turunan dari sekolah bila ada, atau nilai field langsung),
	 *         boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		yayasan = check(yayasan);
		if (getSekolah() != null) {
			yayasan = getSekolah().getYayasan();
		}
		return yayasan;
	}

	/**
	 * Menyetel yayasan scoping untuk konfigurasi ini. Nilai dengan {@code id null} (entity
	 * transient/belum tersimpan) dinormalisasi jadi {@code null} — mencegah baris yayasan yang
	 * belum persisten ikut tersimpan sebagai referensi.
	 *
	 * @param yayasan yayasan baru; diabaikan (jadi {@code null}) bila {@code null} atau belum
	 *                punya ID.
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Mengambil sekolah scoping untuk konfigurasi ini, dipakai untuk resolusi shift-default guru
	 * dan siswa — lihat javadoc kelas bagian 2.
	 *
	 * @return sekolah, boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return sekolah;
	}

	/**
	 * Menyetel sekolah scoping untuk konfigurasi ini. Nilai dengan {@code id null} dinormalisasi
	 * jadi {@code null}, sama seperti {@link #setYayasan(Yayasan)}.
	 *
	 * @param sekolah sekolah baru; diabaikan (jadi {@code null}) bila {@code null} atau belum
	 *                punya ID.
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Mengambil fakultas scoping untuk konfigurasi ini. <b>Getter destruktif berantai:</b> bila
	 * {@link #getJurusan()} terisi, nilai {@code fakultas} <b>ditimpa</b> oleh fakultas pemilik
	 * jurusan tersebut ({@code getJurusan().getFakultas()}) — pola yang sama seperti
	 * {@link #getYayasan()}. Lihat javadoc kelas bagian 4.
	 *
	 * @return fakultas efektif (turunan dari jurusan bila ada, atau nilai field langsung), boleh
	 *         {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		if (getJurusan() != null) {
			fakultas = getJurusan().getFakultas();
		}
		return fakultas;
	}

	/**
	 * Menyetel fakultas scoping untuk konfigurasi ini. Berbeda dengan {@link #setYayasan(Yayasan)}
	 * dan {@link #setSekolah(Sekolah)}, setter ini <b>tidak</b> menormalisasi entity ber-ID
	 * {@code null} menjadi {@code null} — nilai apa pun diterima apa adanya.
	 *
	 * @param fakultas fakultas baru.
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Mengambil jurusan scoping untuk konfigurasi ini, dipakai untuk resolusi shift-default dosen
	 * dan mahasiswa — lihat javadoc kelas bagian 2.
	 *
	 * @return jurusan, boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Menyetel jurusan scoping untuk konfigurasi ini.
	 *
	 * @param jurusan jurusan baru.
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengambil flag rotasi, dengan fallback ke {@code false} (shift tetap/statis) bila belum
	 * pernah diset. Menentukan cabang logika di {@link #getJumlahHariSamaDenganJumlahShift()}.
	 *
	 * @return {@code true} bila jenis shift ini berotasi, tidak pernah {@code null}.
	 */
	public Boolean getBerotasi() {
		return berotasi == null ? false : berotasi;
	}

	/**
	 * Menyetel flag rotasi jenis shift ini.
	 *
	 * @param berotasi nilai flag baru.
	 */
	public void setBerotasi(Boolean berotasi) {
		this.berotasi = berotasi;
	}

	/**
	 * Mengambil flag "hari libur ditentukan eksplisit", dengan fallback ke {@code false}.
	 *
	 * @return {@code true} bila hari libur shift ini didefinisikan eksplisit per baris
	 *         {@link DetailJenisShiftPegawai}, tidak pernah {@code null}.
	 */
	public Boolean getHariLiburDitentukan() {
		return hariLiburDitentukan == null ? false : hariLiburDitentukan;
	}

	/**
	 * Menyetel flag "hari libur ditentukan eksplisit".
	 *
	 * @param hariLiburDitentukan nilai flag baru.
	 */
	public void setHariLiburDitentukan(Boolean hariLiburDitentukan) {
		this.hariLiburDitentukan = hariLiburDitentukan;
	}

	/**
	 * Mengambil jumlah hari efektif dalam satu siklus rotasi shift.
	 *
	 * <p>Ini adalah salah satu method paling rawan salah-baca di kelas ini karena punya <b>dua
	 * lapis penulisan balik ke field</b> yang saling bergantung:</p>
	 * <ol>
	 *   <li>Bila field {@code jumlahHari} masih {@code null}, ia diisi paksa jadi 1 (fallback
	 *       shift satu-hari) — pola yang sama seperti {@link #getJumlahShift()}.</li>
	 *   <li>Lalu, terlepas dari langkah pertama, bila {@link #getJumlahHariSamaDenganJumlahShift()}
	 *       bernilai {@code true}, field {@code jumlahHari} <b>ditimpa lagi</b> dengan nilai
	 *       {@link #getJumlahShift()} — mengabaikan sepenuhnya nilai yang baru saja
	 *       di-fallback-kan di langkah pertama maupun nilai yang tersimpan eksplisit di DB.</li>
	 * </ol>
	 * <p>Konsekuensinya: pada konfigurasi non-rotasi ({@link #getBerotasi()} bernilai
	 * {@code false}), {@link #getJumlahHariSamaDenganJumlahShift()} <b>selalu</b> memaksa
	 * {@code true} (lihat javadoc method itu), sehingga {@code getJumlahHari()} pada praktiknya
	 * <b>selalu sama dengan</b> {@link #getJumlahShift()} kecuali shift tersebut eksplisit
	 * berotasi <i>dan</i> {@code jumlahHariSamaDenganJumlahShift} eksplisit diset {@code false}.
	 * Nilai yang disimpan lewat {@link #setJumlahHari(Integer)} pada kasus non-rotasi karenanya
	 * <b>tidak pernah benar-benar terpakai</b> — akan selalu ditimpa saat dibaca kembali. Method
	 * ini juga mewarisi efek samping tulis-balik dari {@link #getJumlahHariSamaDenganJumlahShift()}
	 * dan {@link #getJumlahShift()} yang dipanggilnya.</p>
	 *
	 * @return jumlah hari efektif dalam siklus rotasi, tidak pernah {@code null} (minimal 1).
	 */
	public Integer getJumlahHari() {
		if (jumlahHari == null) {
			jumlahHari = 1;
		}

		if (getJumlahHariSamaDenganJumlahShift()) {
			jumlahHari = getJumlahShift();
		}

		return jumlahHari;
	}

	/**
	 * Menyetel jumlah hari dalam siklus rotasi shift. <b>Perhatian:</b> nilai yang diset lewat
	 * setter ini bisa langsung ditimpa lagi saat {@link #getJumlahHari()} dipanggil berikutnya,
	 * bila {@link #getJumlahHariSamaDenganJumlahShift()} bernilai {@code true} — lihat javadoc
	 * getter tersebut untuk detail lengkap.
	 *
	 * @param jumlahHari jumlah hari baru.
	 */
	public void setJumlahHari(Integer jumlahHari) {
		this.jumlahHari = jumlahHari;
	}

	/**
	 * Mengambil flag "jumlah hari sama dengan jumlah shift", dengan aturan penentuan nilai
	 * efektif yang <b>menulis balik field</b>: bila {@link #getBerotasi()} bernilai
	 * {@code false} (shift tidak berotasi), field {@code jumlahHariSamaDenganJumlahShift}
	 * <b>dipaksa jadi {@code true}</b> tanpa syarat — menimpa apa pun nilai yang tersimpan
	 * eksplisit di DB untuk konfigurasi non-rotasi. Hanya pada shift yang eksplisit berotasi
	 * ({@code berotasi = true}) nilai field yang tersimpan benar-benar dihormati; dan bila field
	 * itu sendiri {@code null}, fallback keduanya (baik dari cabang paksa maupun dari
	 * ekspresi ternary terakhir) tetap {@code true}.
	 *
	 * <p>Implikasi praktis: mengatur shift non-rotasi agar "jumlah hari tidak sama dengan jumlah
	 * shift" mustahil dilakukan lewat {@link #setJumlahHariSamaDenganJumlahShift(Boolean)} saja —
	 * harus {@link #getBerotasi()} bernilai {@code true} lebih dulu, baru nilai {@code false}
	 * yang diset akan bertahan saat dibaca kembali.</p>
	 *
	 * @return {@code true} bila jumlah hari mengikuti jumlah shift; tidak pernah {@code null}.
	 */
	public Boolean getJumlahHariSamaDenganJumlahShift() {
		if (!getBerotasi()) {
			jumlahHariSamaDenganJumlahShift = true;
		}
		return jumlahHariSamaDenganJumlahShift == null ? true : jumlahHariSamaDenganJumlahShift;
	}

	/**
	 * Menyetel flag "jumlah hari sama dengan jumlah shift". Lihat catatan penting pada
	 * {@link #getJumlahHariSamaDenganJumlahShift()}: nilai {@code false} yang diset di sini hanya
	 * bertahan bila {@link #getBerotasi()} juga bernilai {@code true}.
	 *
	 * @param jumlahHariSamaDenganJumlahShift nilai flag baru.
	 */
	public void setJumlahHariSamaDenganJumlahShift(Boolean jumlahHariSamaDenganJumlahShift) {
		this.jumlahHariSamaDenganJumlahShift = jumlahHariSamaDenganJumlahShift;
	}

	/**
	 * Mengambil ambang jam kerja minimal (dalam jam) supaya kehadiran dianggap sah, dengan
	 * fallback ke 3.0 jam bila belum pernah diset. Dibaca lintas-entity oleh mesin absensi harian
	 * lewat rute {@code detailJenisShiftPegawai.getJenisShiftPegawai().getWaktuBekerjaMinimal()} —
	 * lihat javadoc kelas bagian 1.
	 *
	 * @return ambang jam kerja minimal, tidak pernah {@code null}.
	 */
	public Double getWaktuBekerjaMinimal() {
		return waktuBekerjaMinimal == null ? 3.0 : waktuBekerjaMinimal;
	}

	/**
	 * Menyetel ambang jam kerja minimal.
	 *
	 * @param waktuBekerjaMinimal ambang baru dalam jam.
	 */
	public void setWaktuBekerjaMinimal(Double waktuBekerjaMinimal) {
		this.waktuBekerjaMinimal = waktuBekerjaMinimal;
	}

	/**
	 * Mengambil titik lokasi geofencing ke-6. Lihat {@link #getLokasi()}.
	 *
	 * @return lokasi ke-6, boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi6", nullable = true)
	public Lokasi getLokasi6() {
		lokasi6 = check(lokasi6);
		return lokasi6;
	}

	/**
	 * Menyetel titik lokasi geofencing ke-6.
	 *
	 * @param lokasi6 lokasi baru, boleh {@code null}.
	 */
	public void setLokasi6(Lokasi lokasi6) {
		this.lokasi6 = lokasi6;
	}

	/**
	 * Mengambil titik lokasi geofencing ke-7. Lihat {@link #getLokasi()}.
	 *
	 * @return lokasi ke-7, boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi7", nullable = true)
	public Lokasi getLokasi7() {
		lokasi7 = check(lokasi7);
		return lokasi7;
	}

	/**
	 * Menyetel titik lokasi geofencing ke-7.
	 *
	 * @param lokasi7 lokasi baru, boleh {@code null}.
	 */
	public void setLokasi7(Lokasi lokasi7) {
		this.lokasi7 = lokasi7;
	}

	/**
	 * Mengambil titik lokasi geofencing ke-8. Lihat {@link #getLokasi()}.
	 *
	 * @return lokasi ke-8, boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi8", nullable = true)
	public Lokasi getLokasi8() {
		lokasi8 = check(lokasi8);
		return lokasi8;
	}

	/**
	 * Menyetel titik lokasi geofencing ke-8.
	 *
	 * @param lokasi8 lokasi baru, boleh {@code null}.
	 */
	public void setLokasi8(Lokasi lokasi8) {
		this.lokasi8 = lokasi8;
	}

	/**
	 * Mengambil titik lokasi geofencing ke-9. Lihat {@link #getLokasi()}.
	 *
	 * @return lokasi ke-9, boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi9", nullable = true)
	public Lokasi getLokasi9() {
		lokasi9 = check(lokasi9);
		return lokasi9;
	}

	/**
	 * Menyetel titik lokasi geofencing ke-9.
	 *
	 * @param lokasi9 lokasi baru, boleh {@code null}.
	 */
	public void setLokasi9(Lokasi lokasi9) {
		this.lokasi9 = lokasi9;
	}

	/**
	 * Mengambil titik lokasi geofencing ke-10 — titik terakhir dari sepuluh yang didukung. Lihat
	 * {@link #getLokasi()}.
	 *
	 * @return lokasi ke-10, boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi10", nullable = true)
	public Lokasi getLokasi10() {
		lokasi10 = check(lokasi10);
		return lokasi10;
	}

	/**
	 * Menyetel titik lokasi geofencing ke-10.
	 *
	 * @param lokasi10 lokasi baru, boleh {@code null}.
	 */
	public void setLokasi10(Lokasi lokasi10) {
		this.lokasi10 = lokasi10;
	}

	/**
	 * Mengambil flag "wajib mengikuti urutan state masuk dan pulang", dengan fallback ke
	 * {@code false}. Dibaca lintas-entity oleh {@code ProsesAbsensiPegawai} lewat rute
	 * {@code detailJenisShiftPegawai.getJenisShiftPegawai().getHarusMengikutiStateMasukDanPulang()}
	 * untuk memutuskan apakah urutan state absen (mis. tidak boleh "pulang" tanpa "masuk" lebih
	 * dulu) ditegakkan secara ketat pada shift ini.
	 *
	 * @return {@code true} bila urutan state ditegakkan ketat, tidak pernah {@code null}.
	 */
	public Boolean getHarusMengikutiStateMasukDanPulang() {
		return harusMengikutiStateMasukDanPulang == null ? false : harusMengikutiStateMasukDanPulang;
	}

	/**
	 * Menyetel flag "wajib mengikuti urutan state masuk dan pulang".
	 *
	 * @param harusMengikutiStateMasukDanPulang nilai flag baru.
	 */
	public void setHarusMengikutiStateMasukDanPulang(Boolean harusMengikutiStateMasukDanPulang) {
		this.harusMengikutiStateMasukDanPulang = harusMengikutiStateMasukDanPulang;
	}
}
