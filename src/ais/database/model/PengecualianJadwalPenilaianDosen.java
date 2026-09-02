package ais.database.model;

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
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * Entity JPA/Hibernate untuk tabel {@code public.pengecualian_jadwal_penilaian_dosen} — satu baris
 * mewakili SATU IZIN/DISPENSASI yang membuka kembali kemampuan seorang dosen (atau seorang petugas
 * pengelola) untuk MENGINPUT NILAI DI LUAR JADWAL PENILAIAN BAKU, terbatas pada satu
 * {@code tahunAkademik} + {@code jenisSemester} dan satu rentang tanggal
 * {@code tanggalMulai}..{@code tanggalSampai}.
 *
 * <h2>Peran dalam alur dispensasi penilaian</h2>
 * <ol>
 * <li><b>Pengajuan</b> — dosen (atau petugas yang membantu) membuat baris baru lewat
 * {@link ais.action.master.helper.PengecualianJadwalPenilaianDosenHelper} (dibuka dari
 * {@code PenilaianAction.onAjukanIzinPenilaian}/{@code onPengecualianJadwalPenilaianDosen}) atau
 * lewat layar {@link ais.action.master.PengecualianJadwalPenilaianDosenAction}. Baris baru selalu
 * disimpan dengan {@link #setStatus(String)} = {@link #PENGAJUAN}, {@code disetujuiOleh = null} dan
 * {@code tanggalPersetujuan = null}.</li>
 * <li><b>Persetujuan</b> — dapat melalui DUA jalur yang berbeda dan saling menimpa:
 * <ul>
 * <li><i>Jalur SOP/disposisi</i> — bila {@link #getDisposisiSop()} terisi, status persetujuan
 * DITURUNKAN dari langkah-langkah disposisi ({@code getDisposisiSetuju()} /
 * {@code getDisposisiEnd()}), lihat {@link #getDisetujuiOleh()},
 * {@link #getTanggalPersetujuan()} dan {@link #getStatus()}.</li>
 * <li><i>Jalur manual</i> — admin memilih status pada combobox baris; nilai
 * {@link #DISETUJU} menulis {@code disetujuiOleh} = pengguna aktif dan
 * {@link #setTanggalPersetujuanManual(Date)} = waktu sekarang.</li>
 * </ul>
 * </li>
 * <li><b>Pemakaian</b> — {@link ais.common.CommonHelperClass#checkApakahDosenBolehMenilai} menanyakan
 * tabel ini secara langsung (bukan lewat getter kelas ini) untuk memutuskan apakah gerbang input
 * nilai dibuka HARI INI. Predikatnya: {@code status IN ('Disetujui', NULL)} DAN
 * {@code tahun_akademik = ?} DAN {@code jenis_semester = ?} DAN {@code tanggal_mulai <= hari ini}
 * DAN {@code tanggal_sampai >= hari ini} DAN identitas cocok ({@code dosen} untuk role
 * {@code DOSEN}, {@code tbmuser.userId} untuk role pengelola).</li>
 * <li><b>Cetak</b> — {@link ais.action.report.CommonReportHelper#onCetakPengecualianJadwalPenilaianDosen}
 * mencetak surat izin per baris.</li>
 * </ol>
 *
 * <h2>Dua "wajah" dari satu tabel</h2>
 * <p>
 * Tabel ini dipakai oleh DUA layar yang menyaring baris secara saling eksklusif:
 * {@link ais.action.master.PengecualianJadwalPenilaianDosenAction} hanya menampilkan baris dengan
 * relasi {@code dosen} terisi (INNER JOIN ke {@code dosen}), sedangkan
 * {@link ais.action.master.PengecualianJadwalPenilaianAdminAction} hanya menampilkan baris dengan
 * {@code dosen IS NULL} dan {@code tbmuser} terisi (izin untuk akun pengelola/admin prodi). Karena
 * itu kedua kolom relasi ({@link #getDosen()} dan {@link #getTbmuser()}) bersifat "salah satu",
 * bukan keduanya.
 * </p>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ul>
 * <li><b>Konstanta status</b> — {@link #PENGAJUAN}, {@link #DISETUJU}, {@link #DITOLAK}.</li>
 * <li><b>Jejak audit warisan</b> (deklarasi ulang dari {@link GeneralValueObject}, lihat catatan di
 * bawah) — {@link #getOleh()}, {@link #getOlehId()}, {@link #getTanggal_dirubah()},
 * {@link #onUpdate()}, {@link #getId()}.</li>
 * <li><b>Identitas penerima izin</b> — {@link #getDosen()}, {@link #getTbmuser()}.</li>
 * <li><b>Lingkup izin</b> — {@link #getTahunAkademik()}, {@link #getJenisSemester()},
 * {@link #getTanggalMulai()}, {@link #getTanggalSampai()}, {@link #getKeterangan()}.</li>
 * <li><b>Alur persetujuan</b> — {@link #getStatus()}, {@link #getDisposisiSop()},
 * {@link #getDibuatOleh()}, {@link #getDisetujuiOleh()}, {@link #getTanggalPersetujuan()},
 * {@link #getTanggalPersetujuanManual()}, {@link #getTanggalPembuatan()}.</li>
 * </ul>
 *
 * <h2>Hal-hal NON-OBVIOUS yang wajib diketahui pemanggil</h2>
 * <ol>
 * <li><b>Hampir SEMUA getter di kelas ini bukan getter murni.</b> {@link #getDosen()},
 * {@link #getTbmuser()}, {@link #getDisposisiSop()}, {@link #getDibuatOleh()},
 * {@link #getDisetujuiOleh()}, {@link #getTanggalPersetujuan()} dan {@link #getStatus()} MENULIS
 * BALIK ke field instance. Karena kelas ini {@code @Entity} dengan {@code dynamicUpdate = true},
 * sekadar MEMBACA properti pada entity yang masih ter-attach ke {@code Session} dapat membuat
 * entity menjadi <i>dirty</i> dan tulisan tersebut ikut ter-<i>flush</i> ke database pada akhir
 * transaksi — termasuk masuk ke tabel revisi Envers karena kelas ini {@link Audited}. Rincian per
 * method didokumentasikan masing-masing.</li>
 * <li><b>Getter mengembalikan nilai DEFAULT yang tidak ada di database.</b>
 * {@link #getTahunAkademik()}, {@link #getJenisSemester()}, {@link #getTanggalMulai()},
 * {@link #getTanggalSampai()} dan {@link #getTanggalPembuatan()} mengganti {@code null}/kosong
 * dengan nilai turunan (tahun akademik berjalan, semester berjalan, kemarin, besok-lusa, sekarang)
 * TANPA menuliskannya ke field. Akibatnya nilai yang tampil di UI/laporan bisa BERBEDA dari nilai
 * yang benar-benar tersimpan di kolom — dan gerbang
 * {@link ais.common.CommonHelperClass#checkApakahDosenBolehMenilai} membandingkan KOLOM, bukan
 * hasil getter. Baris dengan kolom {@code tanggal_mulai}/{@code tanggal_sampai} NULL akan terlihat
 * "sedang berlaku" di layar tetapi TIDAK pernah lolos gerbang tersebut.</li>
 * <li><b>Status NULL dibaca sebagai "Disetujui" untuk baris yang sudah tersimpan.</b> Lihat baris
 * terakhir {@link #getStatus()} dan predikat {@code Restrictions.isNull("status")} pada
 * {@link ais.common.CommonHelperClass#checkApakahDosenBolehMenilai}. Ini disengaja demi kompatibilitas
 * data lama, tetapi berarti setiap baris yang lolos ke tabel dengan {@code status} kosong menjadi
 * dispensasi AKTIF tanpa pernah melewati persetujuan.</li>
 * <li><b>{@link #getDisetujuiOleh()} dan {@link #getTanggalPersetujuan()} dapat MEMBUAT jejak
 * persetujuan yang tidak pernah terjadi</b> (pengaju dicatat sebagai penyetuju dirinya sendiri;
 * tanggal persetujuan diisi dari {@code tanggalMulai}). Lihat catatan pada masing-masing method.</li>
 * <li><b>Komentar generator salah salin-tempel.</b> Blok {@code /** Bank generated by hbm2java *}{@code /}
 * asli menyebut "Bank" — tidak ada hubungannya dengan entity ini; pola yang sama ditemukan di
 * beberapa entity lain hasil hbm2java pada repo ini.</li>
 * <li><b>Ejaan konstanta {@link #DISETUJU}</b> kehilangan huruf "i" pada nama konstanta, sedangkan
 * NILAI-nya {@code "Disetujui"} sudah benar. Nama konstanta TIDAK boleh diperbaiki tanpa menyapu
 * seluruh pemanggil.</li>
 * </ol>
 *
 * <h2>Catatan teknis tentang deklarasi ulang field warisan</h2>
 * <p>
 * {@link GeneralValueObject} (lewat {@link DataSop}) BUKAN {@code @Entity} maupun
 * {@code @MappedSuperclass} — ia POJO abstrak biasa, sehingga Hibernate TIDAK memetakan properti
 * yang dideklarasikan di sana. Karena itu {@code id}, {@code oleh}, {@code olehId} dan
 * {@code tanggal_dirubah} SENGAJA dideklarasikan ulang di kelas ini; ini KEHARUSAN TEKNIS agar
 * kolom-kolom tersebut ikut dipetakan, BUKAN duplikasi yang keliru.
 * </p>
 *
 * @see DataSop
 * @see GeneralValueObject
 * @see DisposisiSop
 * @see ais.common.CommonHelperClass#checkApakahDosenBolehMenilai
 * @see ais.action.master.helper.PengecualianJadwalPenilaianDosenHelper
 * @see ais.action.master.PengecualianJadwalPenilaianDosenAction
 * @see ais.action.master.PengecualianJadwalPenilaianAdminAction
 * @see PengecualianJadwalPengisianKRSMahasiswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "pengecualian_jadwal_penilaian_dosen")
public class PengecualianJadwalPenilaianDosen extends DataSop {

	/**
	 * Status awal setiap baris baru: izin sudah diajukan tetapi BELUM berlaku. Baris dengan status
	 * ini tidak lolos gerbang {@link ais.common.CommonHelperClass#checkApakahDosenBolehMenilai}.
	 * Nilai literal {@code "Pengajuan"} disimpan apa adanya ke kolom {@code status}.
	 */
	public static final String PENGAJUAN = "Pengajuan";
	/**
	 * Status izin DISETUJUI — satu-satunya nilai eksplisit yang membuka gerbang input nilai
	 * (bersama baris warisan yang kolom {@code status}-nya masih {@code NULL}).
	 *
	 * <p><b>Catatan ejaan:</b> nama konstanta kehilangan huruf "i" ({@code DISETUJU}), sedangkan
	 * nilainya {@code "Disetujui"}. Perbandingan di seluruh repo memakai nilai ini, jadi jangan
	 * mengganti literalnya.</p>
	 */
	public static final String DISETUJU = "Disetujui";
	/**
	 * Status izin DITOLAK. {@link #setStatus(String)} memberi perlakuan khusus pada nilai ini:
	 * jejak persetujuan ({@code disetujuiOleh} dan {@code tanggalPersetujuan}) dibersihkan.
	 */
	public static final String DITOLAK = "Ditolak";

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama {@code id} (identity/serial PostgreSQL); lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; lihat {@link #getOleh()}. */
	private String oleh;
	/** {@code userId} pengguna terakhir yang mengubah baris ini; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan {@code userId} (username) pengguna yang terakhir mengubah baris ini.
	 *
	 * <p>Diisi otomatis oleh {@link ais.database.hibernate.AuditTimestampInterceptor} lewat
	 * {@link #onUpdate()}. Getter murni, tanpa efek samping.</p>
	 *
	 * @return {@code userId} pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel {@code userId} pengubah terakhir.
	 *
	 * <p><b>Non-obvious:</b> setter ini MENGABAIKAN {@code null} dan string kosong/spasi — nilai
	 * lama dipertahankan. Konsekuensinya jejak audit TIDAK dapat dikosongkan lewat setter ini
	 * (perilaku sengaja: mencegah interceptor menghapus jejak saat konteks pengguna tidak
	 * tersedia).</p>
	 *
	 * @param olehId {@code userId} pengubah; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p><b>Non-obvious:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null} atau
	 * kosong DIABAIKAN sehingga nilai lama bertahan.</p>
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini. Getter murni.
	 *
	 * @return nama pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate} yang menyerahkan pengisian jejak audit
	 * ({@link #setOleh(String)}/{@link #setOlehId(String)}/{@link #setTanggal_dirubah(Date)}) kepada
	 * {@link ais.database.hibernate.AuditTimestampInterceptor}. Dipanggil OLEH Hibernate, bukan oleh
	 * kode aplikasi.
	 *
	 * <p><b>Perhatian format:</b> pada satu baris fisik yang sama juga dideklarasikan field
	 * {@code tanggal_dirubah} dengan nilai awal {@link WaktuUtil#getDate()} (waktu server yang sudah
	 * dikoreksi zona waktu kampus). Tata letak padat ini adalah pola injeksi otomatis yang dipakai
	 * di seluruh entity repo ini — jangan dirapikan tanpa menyapu seluruh model.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir; {@code null} diterima apa adanya (berbeda
	 *                        dari {@link #setOleh(String)}/{@link #setOlehId(String)} yang menolak
	 *                        nilai kosong)
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir. Getter murni.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk instance baru karena field
	 *         diinisialisasi dengan {@link WaktuUtil#getDate()} saat konstruksi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks entity ini: langsung mengembalikan field {@code keterangan} (BUKAN
	 * {@link #getKeterangan()}).
	 *
	 * <p><b>Non-obvious:</b> dapat mengembalikan {@code null} bila keterangan belum diisi. Pemanggil
	 * yang merangkai string (mis. {@code "" + obj}) akan menghasilkan literal {@code "null"}, dan
	 * komponen ZK yang menerima label dari {@code toString()} akan menampilkan teks kosong/"null"
	 * alih-alih identitas baris.</p>
	 *
	 * @return isi kolom {@code keterangan}, mungkin {@code null}
	 */
	public String toString() {
		return keterangan;
	}

	/** Dosen penerima izin (jalur layar dosen); lihat {@link #getDosen()}. */
	private Dosen dosen;
	/** Akun pengguna penerima izin (jalur layar admin/pengelola); lihat {@link #getTbmuser()}. */
	private Tbmuser tbmuser;
	/** Tahun akademik yang dibuka oleh izin ini; lihat {@link #getTahunAkademik()}. */
	private String tahunAkademik;
	/** Jenis semester yang dibuka oleh izin ini; lihat {@link #getJenisSemester()}. */
	private String jenisSemester;
	/** Tanggal awal berlakunya izin; lihat {@link #getTanggalMulai()}. */
	private Date tanggalMulai;
	/** Tanggal akhir berlakunya izin; lihat {@link #getTanggalSampai()}. */
	private Date tanggalSampai;
	/** Alasan/keterangan pengajuan izin; juga dipakai oleh {@link #toString()}. */
	private String keterangan;
	/** Status alur persetujuan; lihat {@link #getStatus()}. */
	private String status;
	/** Kaitan ke alur SOP/disposisi bila izin diproses lewat jalur SOP; lihat {@link #getDisposisiSop()}. */
	private DisposisiSop disposisiSop;
	/** Akun pembuat pengajuan; lihat {@link #getDibuatOleh()}. */
	private Tbmuser dibuatOleh;
	/** Akun penyetuju; lihat {@link #getDisetujuiOleh()}. */
	private Tbmuser disetujuiOleh;
	/** Stempel waktu persetujuan yang diisi manual oleh admin; lihat {@link #getTanggalPersetujuanManual()}. */
	private Date tanggalPersetujuanManual;
	/** Stempel waktu persetujuan efektif (turunan SOP atau manual); lihat {@link #getTanggalPersetujuan()}. */
	private Date tanggalPersetujuan;
	/** Stempel waktu pembuatan baris; lihat {@link #getTanggalPembuatan()}. */
	private Date tanggalPembuatan;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA dan dipakai layar "Tambah" untuk
	 * membuat pengajuan kosong. Seluruh properti dibiarkan {@code null} kecuali
	 * {@code tanggal_dirubah} yang sudah terisi dari inisialisasi field.
	 */
	public PengecualianJadwalPenilaianDosen() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p><b>Non-obvious:</b> {@code null} berarti baris BELUM tersimpan, dan
	 * {@link #getStatus()} memakai fakta ini sebagai pembeda: baris belum tersimpan yang
	 * status-nya kosong dianggap {@link #PENGAJUAN}, sedangkan baris yang SUDAH tersimpan dengan
	 * status kosong dianggap {@link #DISETUJU}.</p>
	 *
	 * @return id baris, atau {@code null} bila belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Umumnya HANYA dipanggil oleh Hibernate setelah {@code INSERT}; kode
	 * aplikasi tidak boleh menyetelnya secara manual.
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan alasan/keterangan pengajuan izin. Getter murni.
	 *
	 * @return isi kolom {@code keterangan}, mungkin {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel alasan/keterangan pengajuan izin. Diisi dari textbox "Keterangan" pada form
	 * pengajuan; nilai {@code null}/kosong diterima apa adanya (form menandainya wajib hanya lewat
	 * label bintang, tanpa validasi server).
	 *
	 * @param keterangan alasan pengajuan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan dosen penerima izin, setelah direhidrasi lewat
	 * {@link GeneralValueObject#check(Object)}.
	 *
	 * <p><b>Efek samping (pola berulang di repo ini — TERVERIFIKASI pada file ini):</b> hasil
	 * {@code check()} DITULIS BALIK ke field {@code dosen}. {@code check()} mengganti proxy lazy
	 * yang sudah tidak dapat diinisialisasi dengan instance kanonik dari
	 * {@code EntityIdentityMap}/query ulang. Karena penulisan balik ini, memanggil getter pada
	 * entity yang masih ter-attach dapat menandai entity sebagai dirty. Tidak destruktif: nilai
	 * {@code null} tetap {@code null} dan tidak ada data yang dihapus.</p>
	 *
	 * @return dosen penerima izin, atau {@code null} bila baris ini adalah izin untuk akun
	 *         pengelola (lihat {@link #getTbmuser()})
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen", nullable = true)
	public Dosen getDosen() {
		dosen = check(dosen);
		return dosen;
	}

	/**
	 * Menyetel dosen penerima izin.
	 *
	 * <p>Dipanggil dari {@code PengecualianJadwalPenilaianDosenAction.onSave} dan
	 * {@code PengecualianJadwalPenilaianDosenHelper}. Untuk pengguna ber-role {@code DOSEN}, kedua
	 * pemanggil MEMAKSA nilai ini menjadi dosen milik akun aktif (mengabaikan pilihan combobox),
	 * sehingga dosen tidak dapat membuat izin atas nama dosen lain.</p>
	 *
	 * @param dosen dosen penerima izin; {@code null} berarti baris ini untuk akun pengelola
	 */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/**
	 * Mengembalikan tahun akademik yang dibuka izin ini, dengan FALLBACK ke tahun akademik berjalan.
	 *
	 * <p><b>Non-obvious — nilai tampil vs nilai tersimpan:</b> bila kolom {@code tahun_akademik}
	 * kosong, getter mengembalikan {@link Common#getCurrentTahunAkademik()} TANPA menuliskannya ke
	 * field. Layar dan surat cetak jadi menampilkan tahun berjalan, padahal database menyimpan
	 * {@code NULL}. Gerbang {@link ais.common.CommonHelperClass#checkApakahDosenBolehMenilai}
	 * membandingkan KOLOM dengan {@code Restrictions.eq("tahunAkademik", ...)}, sehingga baris
	 * seperti itu TIDAK PERNAH cocok dan izinnya tidak berlaku meski terlihat valid di layar.</p>
	 *
	 * @return tahun akademik tersimpan, atau tahun akademik berjalan bila kolom kosong
	 */
	@Column(name = "tahun_akademik", nullable = true)
	public String getTahunAkademik() {
		return tahunAkademik == null || tahunAkademik.trim().isEmpty() ? Common.getCurrentTahunAkademik()
				: tahunAkademik;
	}

	/**
	 * Menyetel tahun akademik yang dibuka izin ini.
	 *
	 * <p>Diisi dari combobox tahun ajaran pada form maupun dari combobox in-line pada baris grid.
	 * Nilai {@code null} diterima apa adanya — lihat peringatan pada {@link #getTahunAkademik()}
	 * mengenai konsekuensinya terhadap gerbang pemakaian izin.</p>
	 *
	 * @param tahunAkademik tahun akademik format {@code "YYYY/YYYY"}
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan jenis semester yang dibuka izin ini, dengan FALLBACK ke semester berjalan.
	 *
	 * <p><b>Non-obvious:</b> bila kolom kosong, getter mengembalikan {@link Perkuliahan#GANJIL} atau
	 * {@link Perkuliahan#GENAP} berdasarkan {@link Common#isNowSemensterGanjil()} — TANPA menulis ke
	 * field, dan TANPA pernah menghasilkan {@link Perkuliahan#SP} walaupun combobox pada form
	 * menyediakan pilihan SP. Berlaku peringatan yang sama seperti {@link #getTahunAkademik()}:
	 * kolom {@code NULL} tidak akan cocok pada gerbang
	 * {@link ais.common.CommonHelperClass#checkApakahDosenBolehMenilai}.</p>
	 *
	 * @return jenis semester tersimpan, atau semester berjalan bila kolom kosong
	 */
	@Column(name = "jenis_semester", nullable = true)
	public String getJenisSemester() {
		return jenisSemester == null || jenisSemester.trim().isEmpty()
				? (Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP)
				: jenisSemester;
	}

	/**
	 * Menyetel jenis semester yang dibuka izin ini.
	 *
	 * @param jenisSemester {@link Perkuliahan#GANJIL}, {@link Perkuliahan#GENAP}, atau
	 *                      {@link Perkuliahan#SP}
	 */
	public void setJenisSemester(String jenisSemester) {
		this.jenisSemester = jenisSemester;
	}

	/**
	 * Mengembalikan tanggal AWAL berlakunya izin, dengan FALLBACK ke {@link WaktuUtil#kemarin()}.
	 *
	 * <p><b>Non-obvious — fallback "membuka" secara tampilan:</b> bila kolom {@code tanggal_mulai}
	 * kosong, getter mengembalikan tanggal KEMARIN tanpa menulis ke field, sehingga rentang izin
	 * pada layar/laporan terlihat sudah aktif. Kolom sesungguhnya tetap {@code NULL}, dan predikat
	 * {@code tanggal_mulai <= hari ini} pada
	 * {@link ais.common.CommonHelperClass#checkApakahDosenBolehMenilai} tidak pernah terpenuhi untuk
	 * {@code NULL} — jadi gerbang sebenarnya TIDAK ikut terbuka. Ketidaksesuaian tampilan-vs-gerbang
	 * ini juga mempengaruhi {@link #getTanggalPersetujuan()} yang memakai getter ini sebagai sumber
	 * tanggal persetujuan darurat.</p>
	 *
	 * @return tanggal mulai tersimpan, atau tanggal kemarin bila kolom kosong; tidak pernah
	 *         {@code null}
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_mulai", nullable = true)
	public Date getTanggalMulai() {
		return tanggalMulai == null ? WaktuUtil.kemarin() : tanggalMulai;
	}

	/**
	 * Menyetel tanggal awal berlakunya izin.
	 *
	 * <p>Pada pembuatan pengajuan baru, {@code PengecualianJadwalPenilaianDosenAction.form} menyetel
	 * nilai awal ke hari ini pukul 08:00. Setelah itu nilai berasal dari datebox pada form maupun
	 * dari datebox in-line pada baris grid.</p>
	 *
	 * @param tanggalMulai tanggal awal berlakunya izin
	 */
	public void setTanggalMulai(Date tanggalMulai) {
		this.tanggalMulai = tanggalMulai;
	}

	/**
	 * Mengembalikan tanggal AKHIR berlakunya izin, dengan FALLBACK ke
	 * {@link WaktuUtil#besoklusa()}.
	 *
	 * <p><b>Non-obvious:</b> berlaku peringatan yang sama seperti {@link #getTanggalMulai()} —
	 * kolom {@code NULL} ditampilkan sebagai "lusa" sehingga rentang terlihat masih berjalan,
	 * padahal predikat {@code tanggal_sampai >= hari ini} pada gerbang pemakaian tidak akan cocok.
	 * Tidak ada validasi bahwa {@code tanggalSampai >= tanggalMulai}, baik di kelas ini maupun di
	 * {@code onSave} kedua Action — rentang terbalik dapat disimpan.</p>
	 *
	 * @return tanggal sampai tersimpan, atau tanggal lusa bila kolom kosong; tidak pernah
	 *         {@code null}
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_sampai", nullable = true)
	public Date getTanggalSampai() {
		return tanggalSampai == null ? WaktuUtil.besoklusa() : tanggalSampai;
	}

	/**
	 * Menyetel tanggal akhir berlakunya izin.
	 *
	 * @param tanggalSampai tanggal akhir berlakunya izin
	 */
	public void setTanggalSampai(Date tanggalSampai) {
		this.tanggalSampai = tanggalSampai;
	}

	/**
	 * Mengembalikan akun pengguna penerima izin (jalur pengelola/admin prodi), setelah direhidrasi
	 * lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * <p><b>Efek samping:</b> hasil {@code check()} DITULIS BALIK ke field {@code tbmuser} (pola
	 * yang sama dengan {@link #getDosen()}).</p>
	 *
	 * <p><b>Peran ganda:</b> selain menandai penerima izin jalur pengelola, relasi ini juga dipakai
	 * sebagai NILAI CADANGAN oleh {@link #getDibuatOleh()} dan {@link #getDisetujuiOleh()} bila
	 * jejak pembuat/penyetuju yang sebenarnya tidak tersedia.</p>
	 *
	 * @return akun penerima izin, atau {@code null} bila baris ini adalah izin untuk dosen
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		tbmuser = check(tbmuser);
		return tbmuser;
	}

	/**
	 * Menyetel akun pengguna penerima izin (jalur pengelola).
	 *
	 * @param tbmuser akun penerima izin
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * Mengembalikan kaitan ke alur SOP/disposisi yang memproses izin ini, setelah direhidrasi lewat
	 * {@link GeneralValueObject#check(Object)}. Implementasi dari kontrak abstrak
	 * {@link DataSop#getDisposisiSop()}.
	 *
	 * <p><b>Efek samping:</b> hasil {@code check()} DITULIS BALIK ke field {@code disposisiSop}.</p>
	 *
	 * <p><b>Penting:</b> bila nilainya TIDAK {@code null}, alur SOP menjadi OTORITAS status —
	 * {@link #getStatus()}, {@link #getDisetujuiOleh()} dan {@link #getTanggalPersetujuan()}
	 * menurunkan nilainya dari langkah disposisi dan MENIMPA apa pun yang tersimpan di kolom
	 * masing-masing. UI juga men-disable combobox status untuk baris seperti ini.</p>
	 *
	 * @return disposisi SOP yang memproses izin ini, atau {@code null} bila izin diproses lewat
	 *         jalur persetujuan manual
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menyetel kaitan ke alur SOP/disposisi. Implementasi dari kontrak abstrak
	 * {@link DataSop#setDisposisiSop(DisposisiSop)}.
	 *
	 * <p><b>Non-obvious — setter "sekali pasang":</b> penjaga di awal method langsung KELUAR bila
	 * argumen {@code null} atau belum punya id, sehingga kaitan SOP yang sudah terpasang TIDAK
	 * DAPAT dilepas lewat setter ini.</p>
	 *
	 * <p><b>Kode mati:</b> karena penjaga tersebut, ekspresi ternary di badan method sudah pasti
	 * mengevaluasi {@code (disposisiSop == null || disposisiSop.getId() == null)} sebagai
	 * {@code false}, sehingga cabang "pertahankan nilai lama" TIDAK PERNAH terpakai dan method
	 * selalu menugaskan argumen. Ini sisa dari penulisan defensif sebelum penjaga ditambahkan —
	 * dicatat apa adanya, jangan diubah tanpa uji regresi alur SOP.</p>
	 *
	 * @param disposisiSop disposisi SOP yang memproses izin ini; diabaikan bila {@code null} atau
	 *                     belum tersimpan (id {@code null})
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {if(disposisiSop==null||disposisiSop.getId()==null) {return;}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
	}

	/**
	 * Menyetel akun pembuat pengajuan.
	 *
	 * <p>Dipanggil oleh {@code onSave} HANYA pada pembuatan baris baru, dengan nilai
	 * {@link Common#getCurrentUser()}. Nilai ini kemudian dipakai
	 * {@code PengecualianJadwalPenilaianDosenHelper.diajukanOlehPenggunaAktif} untuk mencegah
	 * seorang admin menyetujui pengajuan yang dibuatnya sendiri.</p>
	 *
	 * @param dibuatOleh akun pembuat pengajuan
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengembalikan akun PEMBUAT pengajuan, dengan penurunan berjenjang.
	 *
	 * <p><b>Urutan penentuan (setiap langkah MENIMPA field {@code dibuatOleh}):</b></p>
	 * <ol>
	 * <li>Rehidrasi nilai tersimpan lewat {@link GeneralValueObject#check(Object)}.</li>
	 * <li>Bila ada {@link #getDisposisiSop()} dengan langkah awal
	 * ({@code getDisposisiStart()}) yang punya pengaju, pakai pengaju langkah SOP tersebut.</li>
	 * <li>Bila tidak, dan {@link #getTbmuser()} terisi, pakai {@code tbmuser} (penerima izin
	 * dianggap sebagai pembuatnya).</li>
	 * </ol>
	 *
	 * <p><b>Efek samping (getter destruktif — TERVERIFIKASI):</b> langkah 2 dan 3 MENIMPA field
	 * {@code dibuatOleh}. Pada entity yang masih ter-attach, nilai kolom {@code dibuat_oleh} yang
	 * sesungguhnya dapat tergantikan dan ter-flush ke database hanya karena properti ini DIBACA —
	 * termasuk saat grid me-render baris atau saat surat izin dicetak. Jejak "siapa yang benar-benar
	 * mengajukan" karenanya tidak dapat diandalkan untuk baris yang punya {@code tbmuser} atau
	 * disposisi SOP.</p>
	 *
	 * <p><b>Dipanggil dari:</b> {@code Common.insertProperty} saat mencetak surat izin, renderer
	 * grid, dan {@code PengecualianJadwalPenilaianDosenHelper.diajukanOlehPenggunaAktif} (pemeriksaan
	 * anti self-approval).</p>
	 *
	 * @return akun pembuat pengajuan hasil penurunan di atas, atau {@code null} bila tidak satu pun
	 *         sumber tersedia
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = true)
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);
		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
				&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
			dibuatOleh = getDisposisiSop().getDisposisiStart().getDiajukanOleh();
		} else if (getTbmuser() != null) {
			dibuatOleh = getTbmuser();
		}
		return dibuatOleh;
	}

	/**
	 * Menyetel akun penyetuju izin.
	 *
	 * <p>Dipanggil dengan {@link Common#getCurrentUser()} saat admin memilih status
	 * {@link #DISETUJU} pada combobox baris, dan dengan {@code null} saat baris baru disimpan atau
	 * saat status di-set {@link #DITOLAK} lewat {@link #setStatus(String)}.</p>
	 *
	 * @param disetujuiOleh akun penyetuju, atau {@code null} untuk membersihkan jejak persetujuan
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengembalikan akun PENYETUJU izin, dengan penurunan berjenjang dari alur SOP dan sejumlah
	 * penambalan (patch) nilai.
	 *
	 * <p><b>Urutan penentuan (setiap langkah MENIMPA field {@code disetujuiOleh}):</b></p>
	 * <ol>
	 * <li>Rehidrasi nilai tersimpan lewat {@link GeneralValueObject#check(Object)}.</li>
	 * <li>Bila ada {@link #getDisposisiSop()} dengan langkah persetujuan
	 * ({@code getDisposisiSetuju()}) yang punya pengaju, pakai pengaju langkah tersebut.</li>
	 * <li>Bila ada disposisi SOP TETAPI langkah persetujuannya belum ada/kosong,
	 * {@code disetujuiOleh} DI-NULL-KAN — nilai kolom {@code disetujui_oleh} yang tersimpan
	 * dibuang.</li>
	 * <li>Bila {@link #getTanggalPersetujuanManual()} terisi dan penyetuju sudah ada, field
	 * {@code tanggalPersetujuan} ikut disalin dari tanggal persetujuan manual.</li>
	 * <li>Bila {@link #getStatus()} sudah {@link #DISETUJU} tetapi penyetuju masih kosong dan
	 * {@link #getTbmuser()} terisi, {@code tbmuser} DIPAKAI SEBAGAI PENYETUJU.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping (getter destruktif — TERVERIFIKASI):</b> method ini menulis ke DUA field
	 * berbeda ({@code disetujuiOleh} DAN {@code tanggalPersetujuan}) hanya karena dibaca, dan
	 * langkah 3 bersifat MENGHAPUS. Pada entity ter-attach, hasilnya dapat ter-flush ke database.</p>
	 *
	 * <p><b>Catatan integritas jejak audit:</b> langkah 5 dapat mencatat penerima izin sendiri
	 * sebagai penyetujunya. Untuk baris jalur pengelola ({@code tbmuser} terisi) yang status-nya
	 * {@link #DISETUJU} tanpa penyetuju tercatat — termasuk baris warisan yang status-nya
	 * {@code NULL} lalu dibaca sebagai {@link #DISETUJU} oleh {@link #getStatus()} — layar dan surat
	 * cetak akan menampilkan "disetujui oleh" yang identik dengan penerima izin, tanpa persetujuan
	 * nyata pernah terjadi. Perilaku ini murni turunan pembacaan, tidak melalui pemeriksaan hak
	 * akses apa pun.</p>
	 *
	 * @return akun penyetuju hasil penurunan di atas, atau {@code null} bila izin belum/tidak
	 *         disetujui
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujuiOleh = check(disetujuiOleh);

		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
				&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
			disetujuiOleh = getDisposisiSop().getDisposisiSetuju().getDiajukanOleh();
		}

		if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
				|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
			disetujuiOleh = null;
		}

		disetujuiOleh = check(disetujuiOleh);
		if (getTanggalPersetujuanManual() != null && disetujuiOleh != null) {
			tanggalPersetujuan = getTanggalPersetujuanManual();
		}

		if (getStatus().equals(DISETUJU) && disetujuiOleh == null && getTbmuser() != null) {
			disetujuiOleh = getTbmuser();
		}

		return disetujuiOleh;
	}

	/**
	 * Menyetel stempel waktu persetujuan efektif.
	 *
	 * <p>Dipanggil dengan {@code null} saat baris baru disimpan dan saat status di-set
	 * {@link #DITOLAK} lewat {@link #setStatus(String)}. Perhatikan bahwa nilai yang disetel di sini
	 * dapat DITIMPA saat {@link #getTanggalPersetujuan()} atau {@link #getDisetujuiOleh()}
	 * dibaca.</p>
	 *
	 * @param tanggalPersetujuan stempel waktu persetujuan
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengembalikan stempel waktu persetujuan efektif, dengan penurunan dari alur SOP.
	 *
	 * <p><b>Urutan penentuan (setiap langkah MENIMPA field {@code tanggalPersetujuan}):</b></p>
	 * <ol>
	 * <li>Bila langkah persetujuan SOP ada dan punya pengaju, pakai waktu langkah SOP tersebut
	 * ({@code getDisposisiSetuju().getWaktu()}).</li>
	 * <li>Bila ada disposisi SOP tetapi langkah persetujuannya belum ada/kosong, field DI-NULL-KAN
	 * (nilai tersimpan dibuang).</li>
	 * <li>Bila {@link #getStatus()} sudah {@link #DISETUJU} tetapi tanggal masih kosong, tanggal
	 * DIISI dari {@link #getTanggalMulai()}.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping (getter destruktif — TERVERIFIKASI):</b> ketiga langkah menulis ke field.
	 * Pada entity ter-attach hasilnya dapat ter-flush ke database dan tercatat di revisi Envers.</p>
	 *
	 * <p><b>Catatan integritas jejak audit:</b> langkah 3 MENGARANG tanggal persetujuan — nilainya
	 * diambil dari tanggal MULAI berlakunya izin, yang bila kolomnya {@code NULL} sendiri sudah
	 * berupa nilai turunan {@link WaktuUtil#kemarin()}. Akibatnya baris berstatus disetujui tanpa
	 * tanggal persetujuan nyata akan menampilkan tanggal persetujuan mundur (kemarin), bukan tanggal
	 * persetujuan yang sebenarnya. Perhatikan pula bahwa {@link #getDisetujuiOleh()} juga menulis ke
	 * field yang sama, sehingga nilai akhir bergantung pada URUTAN pemanggilan kedua getter.</p>
	 *
	 * @return stempel waktu persetujuan, atau {@code null} bila izin belum/tidak disetujui
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {

		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
				&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
			tanggalPersetujuan = getDisposisiSop().getDisposisiSetuju().getWaktu();
		}

		if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
				|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
			tanggalPersetujuan = null;
		}

		if (getStatus().equals(DISETUJU) && tanggalPersetujuan == null) {
			tanggalPersetujuan = getTanggalMulai();
		}

		return tanggalPersetujuan;
	}

	/**
	 * Menyetel stempel waktu pembuatan baris.
	 *
	 * <p>Dipanggil {@code onSave} pada pembuatan baris baru dengan {@link WaktuUtil#getDate()}.</p>
	 *
	 * @param tanggalPembuatan stempel waktu pembuatan
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Mengembalikan stempel waktu pembuatan baris, dengan FALLBACK ke waktu sekarang.
	 *
	 * <p><b>Non-obvious:</b> bila kolom kosong, getter mengembalikan {@code new Date()} yang berbeda
	 * pada setiap pemanggilan dan TIDAK ditulis balik ke field — jadi bukan nilai stabil dan bukan
	 * cerminan isi database. Baris warisan tanpa {@code tanggal_pembuatan} akan tampak "baru dibuat
	 * barusan" setiap kali layar dimuat.</p>
	 *
	 * <p><b>Ketidakkonsistenan zona waktu:</b> fallback ini memakai {@code new Date()} MENTAH,
	 * sedangkan seluruh bagian lain file ini (inisialisasi {@code tanggal_dirubah},
	 * {@link #getTanggalMulai()}, {@link #getTanggalSampai()}) memakai {@link WaktuUtil} yang
	 * menerapkan koreksi zona waktu kampus ({@code PENAMBAHAN_WAKTU}). Pada instalasi WITA/WIT nilai
	 * fallback ini meleset 1-2 jam dari stempel waktu lain pada baris yang sama. Dicatat apa adanya,
	 * tidak diperbaiki.</p>
	 *
	 * @return stempel waktu pembuatan tersimpan, atau waktu sekarang bila kolom kosong; tidak pernah
	 *         {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		return tanggalPembuatan == null ? new Date() : tanggalPembuatan;
	}

	/**
	 * Mengembalikan status alur persetujuan izin ini — INTI keputusan apakah izin berlaku.
	 *
	 * <p><b>Urutan penentuan:</b></p>
	 * <ol>
	 * <li>Bila field {@code status} kosong TETAPI field {@code disetujuiOleh} (dibaca LANGSUNG dari
	 * field, bukan lewat {@link #getDisetujuiOleh()}, untuk menghindari rekursi tak berujung karena
	 * getter tersebut memanggil balik method ini) terisi setelah {@code check()}, field
	 * {@code status} DITULIS menjadi {@link #DISETUJU}.</li>
	 * <li>Bila {@link #getDisposisiSop()} punya langkah akhir yang alur SOP-nya menandai
	 * {@code getPenolakanAdaDiSini()}, field {@code status} DITULIS menjadi {@link #DITOLAK} —
	 * penolakan SOP MENIMPA status manual apa pun.</li>
	 * <li>Bila setelah kedua langkah di atas status masih kosong, nilai KEMBALIAN (tanpa ditulis ke
	 * field) adalah {@link #PENGAJUAN} untuk baris yang belum tersimpan ({@link #getId()}
	 * {@code == null}) dan {@link #DISETUJU} untuk baris yang SUDAH tersimpan.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping (getter destruktif — TERVERIFIKASI):</b> langkah 1 dan 2 menulis ke field
	 * {@code status} dan {@code disposisiSop}; langkah 3 TIDAK menulis (inkonsistensi yang membuat
	 * nilai kembalian bisa berbeda dari nilai yang akan di-flush).</p>
	 *
	 * <p><b>Perilaku "fail-open" pada data warisan:</b> langkah 3 berarti setiap baris yang sudah
	 * tersimpan dengan kolom {@code status} {@code NULL}/kosong dibaca sebagai izin DISETUJUI.
	 * Perilaku ini konsisten dengan gerbang pemakaian
	 * {@link ais.common.CommonHelperClass#checkApakahDosenBolehMenilai} yang memang menerima
	 * {@code Restrictions.isNull("status")} sebagai setara "Disetujui" (disengaja, demi data versi
	 * lama). Konsekuensinya: baris apa pun yang masuk ke tabel ini tanpa mengisi kolom {@code status}
	 * — misalnya lewat impor Excel massal, skrip, atau kode pemanggil baru yang lupa memanggil
	 * {@link #setStatus(String)} — otomatis menjadi dispensasi AKTIF tanpa pernah melewati
	 * persetujuan. Kedua {@code onSave} pada Action yang ada saat ini selalu mengisi
	 * {@link #PENGAJUAN} secara eksplisit, sehingga jalur UI normal aman.</p>
	 *
	 * @return {@link #PENGAJUAN}, {@link #DISETUJU}, {@link #DITOLAK}, atau nilai bebas lain yang
	 *         pernah tersimpan di kolom; tidak pernah {@code null}
	 */
	public String getStatus() {

		if (status == null || status.trim().isEmpty()) {
			disetujuiOleh = check(disetujuiOleh);
			if (disetujuiOleh != null) {
				status = DISETUJU;
			}
		}

		disposisiSop = getDisposisiSop();
		if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
			status = DITOLAK;
		}

		return status == null || status.trim().isEmpty() ? (getId() == null ? PENGAJUAN : DISETUJU) : status;
	}

	/**
	 * Menyetel status alur persetujuan, dengan pembersihan jejak persetujuan saat DITOLAK.
	 *
	 * <p><b>Efek samping:</b> bila {@code status} sama dengan {@link #DITOLAK}, method ini juga
	 * memanggil {@link #setDisetujuiOleh(Tbmuser)} dan {@link #setTanggalPersetujuan(Date)} dengan
	 * {@code null} — jejak "pernah disetujui oleh siapa dan kapan" DIHAPUS PERMANEN dari baris ini
	 * (hanya tersisa di tabel revisi Envers). Menyetel status kembali ke {@link #DISETUJU} tidak
	 * memulihkannya.</p>
	 *
	 * <p><b>Asimetri:</b> perlakuan pembersihan TIDAK berlaku untuk {@link #PENGAJUAN}. Mengembalikan
	 * baris yang sudah disetujui ke status {@link #PENGAJUAN} akan menyisakan {@code disetujuiOleh}
	 * dan {@code tanggalPersetujuan} lama pada baris tersebut.</p>
	 *
	 * <p><b>Tidak ada validasi nilai:</b> string apa pun diterima. Nilai di luar ketiga konstanta
	 * akan gagal semua perbandingan {@code equals(DISETUJU)} sehingga izin efektif tidak berlaku,
	 * tetapi juga tidak terbaca sebagai ditolak.</p>
	 *
	 * <p><b>Dipanggil dari:</b> {@code onSave} kedua Action (selalu {@link #PENGAJUAN} untuk baris
	 * baru), listener {@code onChange} combobox status pada renderer grid, dan
	 * {@code PengecualianJadwalPenilaianDosenHelper}.</p>
	 *
	 * @param status status baru; {@link #DITOLAK} memicu pembersihan jejak persetujuan
	 */
	public void setStatus(String status) {

		if (status != null && status.equals(DITOLAK)) {
			setDisetujuiOleh(null);
			setTanggalPersetujuan(null);
		}

		this.status = status;
	}

	/**
	 * Mengembalikan stempel waktu persetujuan yang diisi MANUAL oleh admin (jalur non-SOP). Getter
	 * murni, tanpa efek samping.
	 *
	 * <p>Dibaca oleh {@link #getDisetujuiOleh()} sebagai sumber untuk mengisi field
	 * {@code tanggalPersetujuan}. Nilai ini terpisah dari {@link #getTanggalPersetujuan()}: yang ini
	 * adalah masukan mentah dari admin, yang itu adalah hasil akhir setelah alur SOP diperhitungkan.</p>
	 *
	 * @return stempel waktu persetujuan manual, atau {@code null} bila persetujuan tidak lewat jalur
	 *         manual
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggalPersetujuanManual() {
		return tanggalPersetujuanManual;
	}

	/**
	 * Menyetel stempel waktu persetujuan manual.
	 *
	 * <p>Dipanggil dengan {@link WaktuUtil#getDate()} tepat sebelum {@link #setStatus(String)}
	 * {@link #DISETUJU} pada listener combobox status.</p>
	 *
	 * @param tanggalPersetujuanManual stempel waktu persetujuan manual
	 */
	public void setTanggalPersetujuanManual(Date tanggalPersetujuanManual) {
		this.tanggalPersetujuanManual = tanggalPersetujuanManual;
	}
}
