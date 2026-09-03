package ais.database.model.sekolah;
// Generated 30 Sep 18 6:57:43 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import org.json.JSONObject;

import ais.common.ConstantValues;

/**
 * Baris keanggotaan seorang siswa di dalam satu kelas — <b>satu baris roster kelas</b>
 * (tabel {@code sekolah.kelas_punya_siswa}).
 *
 * <h2>Peran dalam arsitektur</h2>
 * <p>Entity ini adalah tabel penghubung many-to-many yang <i>diperkaya</i> antara
 * {@link ais.database.model.sekolah.KelasSiswa} (rombongan belajar / kelas, tabel
 * {@code sekolah.kelas}) dan {@link ais.database.model.sekolah.Siswa} (peserta didik). Arah relasi
 * sudah <b>diverifikasi dari kode</b>: kedua FK ({@code kelas_id} dan {@code siswa_id}) berada di
 * sisi entity ini, keduanya {@code nullable = false}, dan {@code KelasSiswa} sendiri
 * <b>tidak</b> mendeklarasikan koleksi balik ke entity ini — seluruh navigasi kelas &rarr; anggota
 * dilakukan lewat {@code Criteria} atas kelas ini dengan
 * {@code Restrictions.eq("kelasSiswa", kelasSiswa)} (lihat
 * {@code DetailKelasSiswaHelper}, {@code AbsensiSiswaHelper}, {@code DetailpertemuanHelper},
 * {@code CommonPSB.masukkanKelas}).</p>
 *
 * <p>Karena roster inilah yang menentukan "siapa saja yang ada di kelas X", entity ini menjadi
 * <b>simpul terpadat modul sekolah</b>: 69 berkas Java merujuknya secara langsung — mulai dari
 * absensi harian ({@code AbsensiAction}, {@code AbsensiSiswaHelper}), pertemuan/e-learning
 * ({@code DetailpertemuanHelper}, {@code ElearningApiUtil}), penilaian &amp; rapor
 * ({@code PenilaianSiswaAction}, {@code DetailPenilaianSiswaHelper}, {@code LaporanRaporSiswa},
 * {@code LaporanRekapTotalNilai}, {@code NilaiSiswaApi}), keuangan
 * ({@code DetailTagihanSiswaHelper}, {@code TagihanUtil},
 * {@code LaporanRincianPembayaranSiswa}), sampai dasbor statistik dan promosi PPDB
 * ({@code CommonPSB}). Perubahan pada entity ini berdampak lintas seluruh modul tersebut.</p>
 *
 * <h2>Perbedaan dengan kerabat dekatnya</h2>
 * <ul>
 *   <li>{@link ais.database.model.sekolah.KelasSiswa} — <b>induk</b>: kelasnya sendiri (nama,
 *       tingkat, tahun ajaran, wali kelas, sekolah/yayasan pemilik). Kolom tenant
 *       ({@code sekolah}, {@code yayasan}) ada di sana, <b>tidak</b> di sini; pembatasan tenant
 *       untuk entity ini selalu bersifat tidak langsung lewat {@code kelasSiswa}.</li>
 *   <li>{@link ais.database.model.sekolah.KelasSiswaPSB} — <b>bukan</b> roster: entity itu memuat
 *       konfigurasi kuota/penempatan kelas untuk jalur PSB (calon siswa yang belum jadi siswa).
 *       Ketika calon siswa diresmikan, {@code CommonPSB.masukkanKelas()} membuat baris
 *       <i>kelas ini</i> dengan {@code calonSiswa} dan {@code siswa} terisi bersamaan.</li>
 *   <li>{@link ais.database.model.sekolah.KelasLesSiswaPunyaSiswa} — kembar struktural untuk
 *       kelas les/ekstra; hampir seluruh method di berkas ini punya pasangannya di sana
 *       (termasuk {@code keyUrut()} yang identik).</li>
 *   <li>{@link ais.database.model.sekolah.VoKelasPunyaSiswa} — kelas induk abstrak yang
 *       menampung seluruh mesin nilai berbasis string ({@code retreiveDetailNilai},
 *       {@code populateDetailNilai}, {@code retreiveTotalNilai}, dan seterusnya). Berkas ini
 *       hanya menyediakan pemetaan persistence dan implementasi accessor yang dipakai mesin
 *       tersebut.</li>
 * </ul>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ol>
 *   <li><b>Identitas &amp; kunci urut</b>: {@link #getId()}, {@link #keyUrut()},
 *       {@link #toString()}, {@link #getNomorUrut()}.</li>
 *   <li><b>Jejak audit</b>: {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()}, {@link #onUpdate()}. Lihat catatan
 *       "deklarasi ulang" di bawah.</li>
 *   <li><b>Relasi inti</b>: {@link #getKelasSiswa()} (kelas), {@link #getSiswa()} (siswa),
 *       {@link #getCalonSiswa()} (asal-usul PPDB, opsional).</li>
 *   <li><b>Payload nilai per siswa</b>: {@link #getDetailNilai()} (nilai per item penilaian),
 *       {@link #getDetailNilaiTotal()} (nilai agregat per kategori),
 *       {@link #getKeterangan1()}/{@link #getKeterangan2()} (catatan rapor semester 1/2 dalam
 *       bentuk JSON). Format string-nya diuraikan di
 *       {@link ais.database.model.sekolah.VoKelasPunyaSiswa}.</li>
 *   <li><b>Pengecualian mata pelajaran</b>: {@link #getMpYgTidakDiambil()}, {@link #ambilMk()},
 *       {@link #filterMk(List, Matapelajaran)}.</li>
 *   <li><b>Status &amp; sisa kolom</b>: {@link #getAktif()}, {@link #getKeterangan()},
 *       {@link #getNoUts()}, {@link #getNoUas()}.</li>
 * </ol>
 *
 * <h2>Catatan penting soal deklarasi ulang {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah}</h2>
 * <p>Kelas ini mewarisi {@link ais.database.model.sekolah.VoKelasPunyaSiswa} yang mewarisi
 * {@link ais.database.model.GeneralValueObject}. {@code GeneralValueObject} <b>bukan</b>
 * {@code @Entity} maupun {@code @MappedSuperclass} — ia POJO abstrak biasa, sehingga Hibernate
 * <b>tidak memetakan satu pun properti induknya</b>. Karena itu deklarasi ulang {@code id},
 * {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} di berkas ini <b>bukan duplikasi yang
 * keliru</b>, melainkan keharusan teknis agar kolom-kolom tersebut benar-benar ada di tabel.
 * Jangan "membersihkannya".</p>
 *
 * <h2>Hal-hal non-obvious yang wajib diketahui</h2>
 * <ul>
 *   <li><b>{@link #getSiswa()} menulis balik (getter destruktif).</b> Bila {@code calonSiswa}
 *       terisi dan calon siswa itu sudah punya {@code Siswa}, getter <b>menimpa field
 *       {@code siswa}</b> dengan {@code calonSiswa.getSiswa()}. Hibernate memakai <i>property
 *       access</i>, jadi nilai hasil timpa itulah yang ikut ter-{@code flush} ke kolom
 *       {@code siswa_id}. Efeknya: setiap penugasan ulang manual {@link #setSiswa(Siswa)} pada
 *       baris yang punya {@code calonSiswa} akan dikembalikan diam-diam begitu baris tersentuh —
 *       instance keluarga "getter write-back" yang sudah dikenal, di sini pada FK identitas
 *       roster.</li>
 *   <li><b>{@link #getNomorUrut()} tidak pernah {@code null}.</b> {@code compareTo()} di
 *       {@link ais.database.model.GeneralValueObject} memakai {@code getNomorUrut()} sebagai kunci
 *       PERTAMA dan hanya melewatinya bila kedua sisi {@code null}. Karena override di sini
 *       meng-coalesce {@code null} &rarr; {@code 0}, cabang itu <b>selalu</b> dipakai; pada
 *       instalasi yang tidak mengisi nomor urut, semua baris "setara" dan sebuah
 *       {@code TreeSet}/{@code TreeMap} berkunci entity ini akan menciut jadi satu elemen.
 *       <b>Verifikasi berkas ini: bug penciutan TIDAK aktif</b> — di seluruh repo entity ini
 *       hanya masuk {@code TreeMap<Long, KelasSiswaPunyaSiswa>} ({@code LaporanRaporSiswa},
 *       {@code LaporanApi}) yang berkunci {@code Long}, tidak ada
 *       {@code TreeSet<KelasSiswaPunyaSiswa>} maupun
 *       {@code Collections.sort(List<KelasSiswaPunyaSiswa>)}. Risikonya laten, bukan aktual.</li>
 *   <li><b>{@link #keyUrut()} adalah kunci urut yang stabil.</b> Ia menggabungkan nomor urut
 *       ter-<i>pad</i>, nomor induk siswa, dan {@code id} — sehingga bila suatu saat entity ini
 *       dipakai sebagai kunci String terurut, kuncinya tetap unik walau nomor urut kosong.</li>
 *   <li><b>{@link #getKeterangan()} DIPETAKAN, tapi tak pernah ditulis.</b> Berbeda dengan pola
 *       "{@code getKeterangan()} tidak dipetakan" pada katalog-katalog yang mengandalkan
 *       {@code GeneralValueObject}, di sini properti {@code keterangan} dideklarasikan ulang
 *       secara lokal sehingga benar-benar punya kolom. Namun <b>tidak ada satu pun pemanggil
 *       {@code setKeterangan()}</b> di repo; satu-satunya pembaca
 *       ({@code DetailpertemuanHelper} yang merender {@code Label}) karenanya selalu kosong.</li>
 *   <li><b>{@link #getNoUts()}/{@link #getNoUas()} adalah kolom mati.</b> Pada padanan modul PT
 *       ({@code KrsMahasiswa}) kedua getter membangkitkan nomor peserta ujian sekali jalan; klon
 *       sekolah ini menyalin field-nya tapi membuang logika pembangkitnya, dan tidak ada
 *       {@code setNoUts()}/{@code setNoUas()} di mana pun. Akibatnya parameter
 *       {@code nomor_ujian} pada cetak absensi/kartu UTS-UAS sekolah
 *       ({@code CommonReportHelper}) selalu kosong.</li>
 *   <li><b>{@link #setAktif(Boolean)} tak pernah dipanggil.</b> Kolom {@code aktif} hanya terisi
 *       lewat coalescing {@link #getAktif()} saat {@code INSERT}. Pemanggil yang benar sudah
 *       memakai {@code Restrictions.or(isNull("aktif"), eq("aktif", true))}
 *       ({@code AbsensiSiswaHelper}) sehingga baris lama ber-{@code NULL} tidak hilang — contoh
 *       POSITIF penanganan pola ini.</li>
 *   <li><b>{@link #filterMk(List, Matapelajaran)} mengembalikan daftar KOSONG bila
 *       {@code matapelajaran} {@code null}</b> — lihat peringatan rinci pada method tersebut.
 *       Ini penyebab roster kelas bisa hilang total dari layar absensi/penilaian/pertemuan.</li>
 * </ul>
 *
 * <h2>Catatan hak akses (hasil audit menyertai dokumentasi ini)</h2>
 * <p>Layar pengelola roster ini, {@code DetailKelasSiswaHelper} (dipakai bersama oleh
 * {@code KelasSiswaAction} dan {@code KelasSiswaPSBAction}), menggerbangi hanya sebagian
 * kontrolnya: "Ambil Siswa" memakai {@code CREATE}, tombol "Edit"/"Hapus" per baris memakai
 * {@code DELETE}. Namun <b>"Bersihkan" (menghapus SELURUH anggota kelas), "Copy siswa dari kelas
 * lain" (menyisipkan massal), unggah Excel massal, dan {@code Intbox} nomor urut per baris
 * dirender tanpa gerbang hak sama sekali</b> — hak BACA saja cukup untuk mengosongkan roster satu
 * kelas. Karena absensi, nilai, rapor, dan tagihan semuanya bergantung pada baris-baris entity
 * ini, dampaknya jauh melampaui layar tersebut. Jangan menambah kontrol baru di layar itu tanpa
 * {@code setVisible(create/delete)} yang eksplisit.</p>
 *
 * @see ais.database.model.sekolah.KelasSiswa
 * @see ais.database.model.sekolah.VoKelasPunyaSiswa
 * @see ais.database.model.sekolah.KelasLesSiswaPunyaSiswa
 * @see ais.database.model.sekolah.KelasSiswaPSB
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "kelas_punya_siswa", schema = "sekolah")
public class KelasSiswaPunyaSiswa extends VoKelasPunyaSiswa {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9157912161411433979L;
	/**
	 * Primary key {@code sekolah.kelas_punya_siswa.id}. Dideklarasikan ulang di sini karena
	 * {@link ais.database.model.GeneralValueObject} bukan {@code @MappedSuperclass}.
	 */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris; diisi {@code AuditTimestampInterceptor}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris; diisi {@code AuditTimestampInterceptor}. */
	private String olehId;

	/**
	 * Membangun kunci pengurutan/identitas yang stabil untuk satu baris roster, berbentuk
	 * <code>&lt;nomorUrut ter-pad&gt;_&lt;nomorInduk siswa&gt;_&lt;id&gt;</code>.
	 *
	 * <p>Nomor urut di-<i>pad</i> dengan cara menempelkan dua belas karakter {@code '0'} di depan
	 * lalu memangkas lima karakter pertama, sehingga untuk nomor urut satu digit hasilnya delapan
	 * digit ({@code "00000012"}) dan urutan leksikografisnya sama dengan urutan numerik. Karena
	 * {@link #getNomorUrut()} tidak pernah {@code null}, method ini tidak pernah menghasilkan
	 * teks {@code "null"} dan tidak pernah melempar {@code StringIndexOutOfBoundsException}.</p>
	 *
	 * <p>Penambahan nomor induk siswa dan {@code id} membuat kunci tetap <b>unik</b> walaupun
	 * seluruh baris ber-nomor urut kosong — inilah yang menjadikan entity ini kebal dari pola
	 * penciutan {@code TreeSet} yang diakibatkan {@link #getNomorUrut()} non-null.</p>
	 *
	 * <p><b>Peringatan:</b> memanggil {@link #getSiswa()} berarti method ini ikut memicu
	 * penulisan balik field {@code siswa} (lihat {@link #getSiswa()}), dan akan melempar
	 * {@code NullPointerException} bila baris berada dalam keadaan tak wajar tanpa siswa
	 * (kolom {@code siswa_id} sendiri {@code nullable = false}).</p>
	 *
	 * @return kunci urut unik untuk baris roster ini
	 * @see #toString()
	 */
	public String keyUrut() {
		String urut = "000000000000" + getNomorUrut();
		urut = urut.substring(5);

		return urut + "_" + getSiswa().getNomorInduk() + "_" + getId();
	}

	/**
	 * Representasi teks baris roster; sepenuhnya mendelegasikan ke {@link #keyUrut()} sehingga
	 * yang tampil adalah kunci urut, bukan nama siswa.
	 *
	 * @return hasil {@link #keyUrut()}
	 */
	public String toString() {
		return keyUrut();
	}

	/** @return id pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum pernah terisi. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah. Nilai {@code null} atau berisi spasi saja <b>diabaikan</b>
	 * (nilai lama dipertahankan) agar jejak audit tidak terhapus oleh pemanggil yang lalai.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null}/kosong diabaikan sehingga nilai lama tetap bertahan.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/** @return nama pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum pernah terisi. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mengisi {@code oleh}/{@code olehId}/{@code tanggal_dirubah}
	 * dari pengguna sesi berjalan lewat {@code AuditTimestampInterceptor.ubah(this)}.
	 *
	 * <p>Hanya berjalan pada {@code UPDATE} yang melalui session Hibernate; operasi massal
	 * (HQL/native bulk) melewatinya, sehingga jejak audit baris yang diubah secara massal tidak
	 * ikut diperbarui.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Waktu perubahan terakhir; diinisialisasi ke waktu server saat object dibuat. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini (presisi {@code TIMESTAMP}). */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Kelas (rombongan belajar) tempat siswa terdaftar; FK {@code kelas_id}, wajib. */
	private KelasSiswa kelasSiswa;
	/** Siswa yang menjadi anggota kelas; FK {@code siswa_id}, wajib. */
	private Siswa siswa;
	/** Calon siswa asal baris ini bila keanggotaan lahir dari promosi PPDB; FK {@code calon_siswa}, opsional. */
	private CalonSiswa calonSiswa;
	/** Catatan bebas per anggota kelas; dipetakan, namun tidak pernah ditulis oleh kode mana pun. */
	private String keterangan;
	/** Catatan rapor semester 1 dalam bentuk JSON object; lihat {@link #getKeterangan1()}. */
	private String keterangan1;
	/** Catatan rapor semester 2 dalam bentuk JSON object; lihat {@link #getKeterangan2()}. */
	private String keterangan2;
	/** Penanda keanggotaan masih berlaku; lihat {@link #getAktif()} soal nilai {@code null}. */
	private Boolean aktif;

	/** Nomor peserta UTS; kolom mati — tidak ada penulisnya di repo, lihat {@link #getNoUts()}. */
	private String noUts;
	/** Nomor peserta UAS; kolom mati — tidak ada penulisnya di repo, lihat {@link #getNoUas()}. */
	private String noUas;

	/** Array JSON berisi id {@link Matapelajaran} yang TIDAK diambil siswa ini; lihat {@link #ambilMk()}. */
	private String mpYgTidakDiambil;

	/** Nomor urut/absen siswa dalam kelas; lihat {@link #getNomorUrut()} soal coalescing ke {@code 0}. */
	private Integer nomorUrut;

	/** Konstruktor kosong yang diwajibkan Hibernate/JPA. */
	public KelasSiswaPunyaSiswa() {
	}

	/**
	 * @return primary key baris roster ini, atau {@code null} bila belum tersimpan. Kolom
	 *         {@code id} ditandai {@code insertable = false} karena dibangkitkan sekuens/identity
	 *         di sisi basis data.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Hanya untuk Hibernate dan skenario penyalinan object; jangan dipakai
	 * kode aplikasi biasa.
	 *
	 * @param id primary key
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan catatan bebas per anggota kelas.
	 *
	 * <p><b>Perhatian:</b> properti ini dipetakan ke kolom {@code keterangan} (deklarasi lokal,
	 * bukan warisan {@code GeneralValueObject} yang tak dipetakan), tetapi <b>tidak ada satu pun
	 * pemanggil {@link #setKeterangan(String)}</b> di seluruh repo. Satu-satunya pembaca adalah
	 * {@code DetailpertemuanHelper} yang merender nilainya sebagai {@code Label}, sehingga label
	 * itu praktis selalu kosong. Perhatikan pula bahwa override ini boleh mengembalikan
	 * {@code null}, berbeda dari {@code GeneralValueObject.getKeterangan()} yang menjamin
	 * {@code ""}.</p>
	 *
	 * @return catatan bebas, atau {@code null} bila belum pernah diisi
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel catatan bebas per anggota kelas.
	 *
	 * @param keterangan catatan bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status keaktifan keanggotaan, dengan {@code null} dianggap <b>aktif</b>.
	 *
	 * <p>Karena Hibernate memakai <i>property access</i>, nilai hasil coalescing inilah yang
	 * ditulis ke kolom saat {@code INSERT} — baris yang dibuat lewat alur normal karenanya berisi
	 * {@code true}, bukan {@code NULL}. Baris yang masuk lewat SQL mentah/migrasi tetap bisa
	 * {@code NULL}; pemanggil yang benar sudah mengantisipasinya dengan
	 * {@code Restrictions.or(isNull("aktif"), eq("aktif", true))} (lihat
	 * {@code AbsensiSiswaHelper}).</p>
	 *
	 * <p>Dipakai {@link #filterMk(List, Matapelajaran)} untuk membuang anggota non-aktif dari
	 * daftar absensi/penilaian. Tidak ada pemanggil {@link #setAktif(Boolean)} di repo, jadi
	 * secara praktis seluruh anggota selalu aktif dan penonaktifan dilakukan dengan menghapus
	 * baris.</p>
	 *
	 * @return {@code true} bila keanggotaan aktif (termasuk saat kolom {@code NULL})
	 */
	public Boolean getAktif() {

		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status keaktifan keanggotaan.
	 *
	 * @param aktif status aktif; {@code null} akan dibaca sebagai aktif oleh {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan kelas tempat siswa ini terdaftar (sisi "banyak ke satu" ke
	 * {@link ais.database.model.sekolah.KelasSiswa}).
	 *
	 * <p>Relasi di-{@code LAZY}-kan, sehingga getter memanggil
	 * {@code GeneralValueObject.check()} lebih dulu untuk meresolusi proxy (cache &rarr; session
	 * berjalan &rarr; reload lewat session baru). {@code check()} tidak pernah melempar exception
	 * dan tidak pernah mengembalikan {@code null} untuk argumen non-null; penulisan balik ke
	 * field hanyalah pemasangan instance ter-resolusi dengan {@code id} yang sama, jadi
	 * <b>tidak</b> destruktif.</p>
	 *
	 * <p>Inilah satu-satunya jalur pembacaan kelas pemilik baris — sekaligus satu-satunya jalur
	 * pembatasan tenant (sekolah/yayasan) untuk entity ini, karena berkas ini tidak punya kolom
	 * tenant sendiri.</p>
	 *
	 * @return kelas pemilik baris roster; secara skema tidak pernah {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelas_id", nullable = false)
	public KelasSiswa getKelasSiswa() {
		kelasSiswa = check(kelasSiswa);
		return kelasSiswa;
	}

	/**
	 * Menyetel kelas pemilik baris roster.
	 *
	 * <p>Relasi memakai {@code cascade = PERSIST, MERGE}: menyimpan baris roster ikut menyimpan
	 * kelas yang belum tersimpan. Jangan memasang instance {@code KelasSiswa} lepas hasil
	 * konstruksi manual.</p>
	 *
	 * @param kelasSiswa kelas pemilik
	 */
	public void setKelasSiswa(KelasSiswa kelasSiswa) {
		this.kelasSiswa = kelasSiswa;
	}

	/**
	 * Mengembalikan siswa anggota kelas.
	 *
	 * <p><b>Getter ini menulis balik dan berpotensi destruktif.</b> Selain meresolusi proxy lewat
	 * {@code check()}, method memeriksa {@link #getCalonSiswa()}: bila baris ini berasal dari
	 * promosi PPDB dan calon siswa tersebut sudah tertaut ke seorang {@code Siswa}, field
	 * {@code siswa} <b>ditimpa</b> dengan {@code calonSiswa.getSiswa()}. Karena Hibernate memakai
	 * <i>property access</i>, nilai hasil timpa itulah yang ikut ter-{@code flush} ke kolom
	 * {@code siswa_id}.</p>
	 *
	 * <p>Pada alur normal hal ini tidak terlihat: {@code CommonPSB.masukkanKelas()} mengisi
	 * {@code calonSiswa} dan {@code siswa} dengan pasangan yang konsisten. Namun bila seseorang
	 * memindahkan baris roster ke siswa lain lewat {@link #setSiswa(Siswa)} sementara
	 * {@code calonSiswa} dibiarkan, perubahan itu akan <b>dikembalikan diam-diam</b> pada
	 * pembacaan berikutnya — tanpa pesan, tanpa jejak selain revisi Envers. Perilaku ini setara
	 * dengan keluarga bug "getter destruktif" yang sudah ditemukan pada entity lain, dan di sini
	 * mengenai FK identitas roster.</p>
	 *
	 * <p>Dipanggil sangat luas: renderer daftar kelas, absensi, penilaian, rapor, dan tagihan.
	 * Implementasi abstrak yang dijanjikan {@link ais.database.model.sekolah.VoKelasPunyaSiswa}.</p>
	 *
	 * @return siswa anggota kelas; secara skema tidak pernah {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa_id", nullable = false)
	public Siswa getSiswa() {
		siswa = check(siswa);
		if (getCalonSiswa() != null && getCalonSiswa().getSiswa() != null) {
			siswa = getCalonSiswa().getSiswa();
		}
		return siswa;
	}

	/**
	 * Menyetel siswa anggota kelas.
	 *
	 * <p><b>Tidak bertahan</b> bila {@code calonSiswa} terisi dan sudah punya siswa — lihat
	 * peringatan pada {@link #getSiswa()}. Relasi memakai {@code cascade = PERSIST, MERGE}.</p>
	 *
	 * @param siswa siswa anggota kelas
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Mengembalikan payload nilai per item penilaian untuk siswa ini, dalam bentuk satu string
	 * panjang bertipe kolom {@code text}.
	 *
	 * <p>Formatnya adalah daftar rekaman dipisah {@code ';'}, tiap rekaman delapan ruas dipisah
	 * {@code '|'}:
	 * <code>jenisItemPenilaianId|matapelajaranId|nilai|0|0|terverifikasi|semester|grupKategoriId</code>.
	 * Seluruh pembacaan/penulisannya dilakukan mesin di
	 * {@link ais.database.model.sekolah.VoKelasPunyaSiswa}
	 * ({@code retreiveDetailNilai}, {@code retreiveDetailVerify}, {@code retreiveTotalNilai},
	 * {@code populateDetailNilai}) — <b>jangan mengurai string ini secara manual</b>.</p>
	 *
	 * <p>Nilai awal {@code ""} (bukan {@code null}) agar mesin nilai tidak perlu menangani
	 * {@code null} pada baris yang baru dibuat.</p>
	 *
	 * @return string payload nilai; {@code ""} bila belum ada nilai
	 */
	@Column(columnDefinition = "text", name = "detail_nilai")
	public String getDetailNilai() {
		return detailNilai;
	}

	/**
	 * Menyetel payload nilai per item penilaian.
	 *
	 * <p>Dipanggil dari {@code VoKelasPunyaSiswa.populateDetailNilai()} setelah menyusun ulang
	 * seluruh rekaman; pemanggil lain sebaiknya lewat method itu agar format tetap sah.</p>
	 *
	 * @param detailNilai string payload nilai berformat seperti dijelaskan di
	 *                    {@link #getDetailNilai()}
	 */
	public void setDetailNilai(String detailNilai) {
		this.detailNilai = detailNilai;
	}

	/** Backing field {@link #getDetailNilai()}; sengaja diinisialisasi {@code ""} bukan {@code null}. */
	private String detailNilai = "";

	/**
	 * Mengembalikan nomor peserta UTS.
	 *
	 * <p><b>Kolom mati.</b> Pada padanan modul PT ({@code KrsMahasiswa.getNoUts()}) nomor ini
	 * dibangkitkan sekali di dalam getter; klon sekolah ini menyalin field-nya tanpa logika
	 * pembangkitnya, dan tidak ada pemanggil {@link #setNoUts(String)} di repo. Akibatnya
	 * parameter {@code nomor_ujian} pada cetak absensi/kartu UTS sekolah
	 * ({@code CommonReportHelper}) selalu kosong.</p>
	 *
	 * @return nomor peserta UTS; pada praktiknya selalu {@code null}
	 */
	public String getNoUts() {
		return noUts;
	}

	/**
	 * Menyetel nomor peserta UTS. Tidak dipanggil di mana pun saat ini.
	 *
	 * @param noUts nomor peserta UTS
	 */
	public void setNoUts(String noUts) {
		this.noUts = noUts;
	}

	/**
	 * Mengembalikan nomor peserta UAS. Berperilaku sama dengan {@link #getNoUts()} — kolom mati
	 * tanpa penulis, sehingga {@code nomor_ujian} pada cetak absensi/kartu UAS selalu kosong.
	 *
	 * @return nomor peserta UAS; pada praktiknya selalu {@code null}
	 */
	public String getNoUas() {
		return noUas;
	}

	/**
	 * Menyetel nomor peserta UAS. Tidak dipanggil di mana pun saat ini.
	 *
	 * @param noUas nomor peserta UAS
	 */
	public void setNoUas(String noUas) {
		this.noUas = noUas;
	}

	/**
	 * Mengembalikan payload nilai agregat per grup kategori item penilaian, satu tingkat di atas
	 * {@link #getDetailNilai()}.
	 *
	 * <p>Formatnya sama-sama delapan ruas dipisah {@code '|'} dan rekaman dipisah {@code ';'},
	 * hanya saja ruas pertama selalu {@code "0"} (tidak ada jenis item penilaian pada tingkat
	 * agregat):
	 * <code>0|matapelajaranId|nilai|0|0|terverifikasi|semester|grupKategoriId</code>. Dibaca dan
	 * ditulis oleh {@code retreiveDetailNilaiTotal()}, {@code retreiveTotalNilaiTotal()}, dan
	 * {@code populateDetailNilaiTotal()} di {@link ais.database.model.sekolah.VoKelasPunyaSiswa}.</p>
	 *
	 * @return string payload nilai agregat; {@code ""} bila belum ada
	 */
	@Column(columnDefinition = "text", name = "detail_nilai_total")
	public String getDetailNilaiTotal() {
		return detailNilaiTotal;
	}

	/**
	 * Menyetel payload nilai agregat per grup kategori.
	 *
	 * <p>Dipanggil dari {@code VoKelasPunyaSiswa.populateDetailNilaiTotal()}.</p>
	 *
	 * @param detailNilaiTotal string payload berformat seperti dijelaskan di
	 *                         {@link #getDetailNilaiTotal()}
	 */
	public void setDetailNilaiTotal(String detailNilaiTotal) {
		this.detailNilaiTotal = detailNilaiTotal;
	}

	/** Backing field {@link #getDetailNilaiTotal()}; sengaja diinisialisasi {@code ""} bukan {@code null}. */
	private String detailNilaiTotal = "";

	/**
	 * Konstanta JSON object kosong ({@code "{}"}) yang dipakai sebagai nilai bawaan
	 * {@link #getKeterangan1()}/{@link #getKeterangan2()} agar pemanggil selalu bisa langsung
	 * membungkusnya dengan {@code new JSONObject(...)} tanpa memeriksa {@code null}.
	 */
	final static String D = new JSONObject().toString();

	/**
	 * Mengembalikan catatan rapor semester 1 sebagai string JSON object.
	 *
	 * <p>Isinya adalah peta bebas berisi catatan wali kelas/kepribadian yang dirender pada rapor
	 * ({@code LaporanRaporSiswa}, {@code LaporanRekapTotalNilai}) dan diedit lewat
	 * {@code DetailPenilaianSiswaHelper}/{@code PenilaianSiswaAction}. Bila kolom masih
	 * {@code null}, dikembalikan {@link #D} ({@code "{}"}) sehingga pemanggil selalu memperoleh
	 * JSON yang sah.</p>
	 *
	 * <p><b>Konsekuensi tak kentara:</b> karena Hibernate memakai <i>property access</i>, baris
	 * yang tersentuh akan menulis {@code "{}"} ke kolom yang semula {@code NULL} — perubahan
	 * kosmetik, bukan kehilangan data, karena semantik keduanya sama.</p>
	 *
	 * @return string JSON object catatan semester 1; tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan1() {
		return keterangan1 == null ? D : keterangan1;
	}

	/**
	 * Menyetel catatan rapor semester 1.
	 *
	 * @param keterangan1 string JSON object; {@code null} akan dibaca sebagai {@code "{}"}
	 */
	public void setKeterangan1(String keterangan1) {
		this.keterangan1 = keterangan1;
	}

	/**
	 * Mengembalikan catatan rapor semester 2 sebagai string JSON object. Berperilaku persis sama
	 * dengan {@link #getKeterangan1()}, dipilih oleh pemanggil berdasarkan nomor semester
	 * ({@code smt == 1 ? getKeterangan1() : getKeterangan2()}).
	 *
	 * @return string JSON object catatan semester 2; tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan2() {
		return keterangan2 == null ? D : keterangan2;
	}

	/**
	 * Menyetel catatan rapor semester 2.
	 *
	 * @param keterangan2 string JSON object; {@code null} akan dibaca sebagai {@code "{}"}
	 */
	public void setKeterangan2(String keterangan2) {
		this.keterangan2 = keterangan2;
	}

	/**
	 * Konstanta JSON array kosong ({@code "[]"}) yang dipakai sebagai nilai bawaan
	 * {@link #getMpYgTidakDiambil()}.
	 *
	 * <p><b>Catatan:</b> instance {@code JSONArray} ini {@code static} dan berbagi pakai; ia
	 * hanya pernah dibaca lewat {@code toString()} dan tidak boleh dimutasi. Perhatikan pula
	 * bahwa variabel lokal bernama sama di {@link #ambilMk()} <i>menutupi</i> (shadow) field ini
	 * — pembacaan sekilas mudah keliru.</p>
	 */
	final static JSONArray array = new JSONArray();

	/**
	 * Mengembalikan daftar id {@link Matapelajaran} yang <b>tidak</b> diambil siswa ini, sebagai
	 * string JSON array.
	 *
	 * <p>Diisi lewat dialog "Pilih Matapelajaran yang tidak diikuti" pada
	 * {@code DetailKelasSiswaHelper}. Bila kolom {@code null} atau berisi spasi saja,
	 * dikembalikan {@code "[]"} ({@link #array}) agar {@link #ambilMk()} selalu bisa mengurainya.</p>
	 *
	 * <p>Pengecualian yang setara juga ada pada tingkat kelas
	 * ({@code KelasSiswa.getMpYgTidakDiambil()}) dan pada jenis rapor
	 * ({@code JenisRaporSiswa}); {@link #ambilMk()}-lah yang menggabungkan tingkat siswa dengan
	 * tingkat kelas.</p>
	 *
	 * @return string JSON array id mata pelajaran; tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getMpYgTidakDiambil() {
		return mpYgTidakDiambil == null || mpYgTidakDiambil.trim().isEmpty() ? array.toString() : mpYgTidakDiambil;
	}

	/**
	 * Menyetel daftar id mata pelajaran yang tidak diambil siswa ini.
	 *
	 * @param mpYgTidakDiambil string JSON array id mata pelajaran; {@code null}/kosong akan
	 *                         dibaca sebagai {@code "[]"}
	 */
	public void setMpYgTidakDiambil(String mpYgTidakDiambil) {
		this.mpYgTidakDiambil = mpYgTidakDiambil;
	}

	/**
	 * Menyaring daftar anggota kelas menjadi hanya anggota <b>aktif</b> yang <b>mengambil</b>
	 * mata pelajaran tertentu.
	 *
	 * <p>Untuk tiap anggota: bila {@link #getAktif()} bernilai {@code false} anggota dibuang;
	 * selebihnya anggota dipertahankan hanya bila id {@code matapelajaran} <b>tidak</b> muncul di
	 * {@link #ambilMk()} (daftar pengecualian gabungan siswa + kelas).</p>
	 *
	 * <p><b>PERINGATAN — perilaku saat {@code matapelajaran} {@code null}.</b> Syarat penyaringan
	 * ditulis sebagai {@code matapelajaran != null && !longs.contains(...)}, sehingga bila
	 * argumen {@code matapelajaran} bernilai {@code null} <b>tidak ada satu pun anggota yang
	 * dimasukkan</b> dan method mengembalikan daftar KOSONG — bukan "semua anggota" seperti yang
	 * intuitif diharapkan. Pemanggil di {@code AbsensiSiswaHelper},
	 * {@code DetailJadwalMatapelajaranHelper}, dan {@code DetailpertemuanHelper} meneruskan
	 * {@code jadwalPelajaran.getMatapelajaran()} <b>tanpa penjagaan null</b>, sehingga jadwal
	 * pelajaran yang mata pelajarannya belum/tidak diisi membuat seluruh roster kelas hilang
	 * diam-diam dari layar absensi, penilaian, dan pertemuan. {@code DetailPenilaianSiswaHelper}
	 * bahkan secara eksplisit dapat menghasilkan {@code matapelajaran == null} ketika kurikulum
	 * tidak ditemukan.</p>
	 *
	 * <p>Ada tiga salinan hampir identik dari penyaring ini di modul sekolah
	 * ({@code KelasSiswa.filterMk}, {@code JenisRaporSiswa.filterMk}, dan yang ini); versi di
	 * berkas inilah yang menerima {@code List<? extends VoKelasPunyaSiswa>} sehingga bisa dipakai
	 * untuk kelas reguler maupun kelas les.</p>
	 *
	 * @param siswa         daftar anggota kelas yang akan disaring; tidak boleh {@code null}
	 * @param matapelajaran mata pelajaran acuan; bila {@code null} hasilnya selalu daftar kosong
	 * @return daftar anggota aktif yang mengambil mata pelajaran tersebut
	 * @see #ambilMk()
	 */
	public static List<VoKelasPunyaSiswa> filterMk(List<? extends VoKelasPunyaSiswa> siswa, Matapelajaran matapelajaran) {
		List< VoKelasPunyaSiswa> kelasSiswaPunyaSiswas = new ArrayList<VoKelasPunyaSiswa>();
		for (VoKelasPunyaSiswa kelasSiswaPunyaSiswa : siswa) {
			if (kelasSiswaPunyaSiswa.getAktif()) {
				List<Long> longs = kelasSiswaPunyaSiswa.ambilMk();
				if (matapelajaran != null && !longs.contains(matapelajaran.getId())) {
					kelasSiswaPunyaSiswas.add(kelasSiswaPunyaSiswa);
				}
				longs = null;
			}
		}
		return kelasSiswaPunyaSiswas;
	}

	/**
	 * Mengurai {@link #getMpYgTidakDiambil()} menjadi daftar id {@link Matapelajaran} yang tidak
	 * diambil siswa ini, <b>digabung</b> dengan pengecualian tingkat kelas.
	 *
	 * <p>Alur kerja:</p>
	 * <ol>
	 *   <li>String JSON milik siswa diurai; tiap elemen di-{@code parse} jadi {@code Long} lalu
	 *       diverifikasi keberadaannya lewat cache {@code ConstantValues.ambil(...)}. Id yang
	 *       tidak lagi merujuk mata pelajaran yang ada <b>dibuang diam-diam</b>, sehingga daftar
	 *       tidak pernah memuat id yatim.</li>
	 *   <li>Selanjutnya pengecualian tingkat kelas ({@code getKelasSiswa().ambilMk()})
	 *       ditambahkan, <b>kecuali</b> bila string pengecualian milik kelas persis sama secara
	 *       tekstual dengan string milik siswa. Perbandingan tekstual ini adalah penjaga
	 *       duplikasi yang rapuh: bila kedua daftar berisi id yang sama namun urutan/format
	 *       JSON-nya berbeda, id bisa muncul dua kali di hasil. Duplikat tidak berbahaya bagi
	 *       {@link #filterMk(List, Matapelajaran)} (memakai {@code contains}), tetapi membuat
	 *       label ringkasan mata pelajaran di {@code DetailKelasSiswaHelper} menampilkan nama yang
	 *       sama berulang.</li>
	 * </ol>
	 *
	 * <p>Kedua tingkat {@code try/catch} menelan exception dan hanya mencatatnya ke
	 * {@code ErrorAuditUtil}: JSON yang rusak menghasilkan daftar kosong, artinya siswa dianggap
	 * mengambil SEMUA mata pelajaran — <i>fail-open</i> yang aman secara akademik (siswa muncul di
	 * absensi/penilaian) tetapi menyembunyikan kerusakan data.</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #getKelasSiswa()} sehingga proxy kelas ikut
	 * diresolusi (berpotensi membuka session baru lewat {@code check()}). Implementasi abstrak
	 * yang dijanjikan {@link ais.database.model.sekolah.VoKelasPunyaSiswa}.</p>
	 *
	 * @return daftar id mata pelajaran yang tidak diambil; tidak pernah {@code null}, bisa kosong
	 */
	public List<Long> ambilMk() {
		List<Long> longs = new ArrayList<Long>();
		try {
			JSONArray array = new JSONArray(getMpYgTidakDiambil());

			for (int i = 0; i < array.length(); i++) {
				try {
					Long key = Long.parseLong(array.get(i).toString());
					Matapelajaran matapelajaran = (Matapelajaran) ConstantValues.ambil(Matapelajaran.class.getName(),
							key);
					if (matapelajaran != null) {
						longs.add(matapelajaran.getId());
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/KelasSiswaPunyaSiswa.java:262");
				}
			}

			if (getKelasSiswa() != null && !getKelasSiswa().getMpYgTidakDiambil().equals(array.toString())) {
				longs.addAll(getKelasSiswa().ambilMk());
			}

			return longs;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/KelasSiswaPunyaSiswa.java:271");
		}
		return longs;
	}

	/**
	 * Mengembalikan calon siswa (PPDB) yang menjadi asal-usul keanggotaan ini, bila ada.
	 *
	 * <p>Kolom {@code calon_siswa} bersifat opsional: baris yang dibuat lewat layar roster biasa
	 * ("Ambil Siswa", "Copy siswa dari kelas lain", unggah Excel) meninggalkannya {@code null},
	 * sementara baris hasil promosi PPDB ({@code CommonPSB.masukkanKelas()}) mengisinya. Nilainya
	 * dipakai {@code Siswa.ambilkelas()} agar siswa yang baru diresmikan tetap dikenali sebagai
	 * anggota kelas yang dipilih saat pendaftaran.</p>
	 *
	 * <p>Relasi {@code LAZY}; getter meresolusi proxy lewat {@code check()}. Perhatikan bahwa
	 * {@link #getSiswa()} memanggil getter ini dan memakai hasilnya untuk <b>menimpa</b> field
	 * {@code siswa}.</p>
	 *
	 * @return calon siswa asal, atau {@code null} bila keanggotaan tidak berasal dari PPDB
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "calon_siswa", nullable = true)
	public CalonSiswa getCalonSiswa() {
		calonSiswa = check(calonSiswa);
		return calonSiswa;
	}

	/**
	 * Menyetel calon siswa asal keanggotaan.
	 *
	 * <p>Mengisi properti ini pada baris yang sudah ada akan <b>mengaktifkan</b> perilaku penulisan
	 * balik {@link #getSiswa()}; jangan memakainya untuk sekadar "menandai" asal pendaftaran.
	 * Relasi memakai {@code cascade = PERSIST, MERGE}.</p>
	 *
	 * @param calonSiswa calon siswa asal; boleh {@code null}
	 */
	public void setCalonSiswa(CalonSiswa calonSiswa) {
		this.calonSiswa = calonSiswa;
	}

	/**
	 * Mengembalikan nomor urut/absen siswa dalam kelas, dengan {@code null} di-coalesce menjadi
	 * {@code 0}.
	 *
	 * <p>Dipakai untuk pengurutan daftar ({@code addOrder(Order.asc("nomorUrut"))} — perhatikan
	 * pengurutan itu berjalan di sisi basis data atas kolom aslinya, jadi baris {@code NULL}
	 * tetap diperlakukan sebagai {@code NULL} oleh SQL), untuk kolom "Nomor Urut" pada cetak
	 * daftar siswa, dan untuk {@link #keyUrut()}.</p>
	 *
	 * <p><b>Konsekuensi override:</b> {@code GeneralValueObject.compareTo()} memakai
	 * {@code getNomorUrut()} sebagai kunci pertama dan hanya melanjutkan ke NIM/nama/keterangan
	 * bila salah satu sisi {@code null}. Karena override ini menjamin non-null, cabang pertama
	 * selalu terpakai; pada instalasi yang tidak mengisi nomor urut semua baris membandingkan
	 * {@code 0} dan dianggap setara. Sebuah {@code TreeSet<KelasSiswaPunyaSiswa>} karenanya akan
	 * menciut menjadi satu elemen. <b>Sudah diverifikasi bahwa hal itu tidak terjadi di repo
	 * ini</b> (entity hanya masuk {@code TreeMap} berkunci {@code Long}), tetapi jangan
	 * memperkenalkan koleksi terurut berkunci entity ini.</p>
	 *
	 * @return nomor urut siswa dalam kelas; {@code 0} bila kolom kosong
	 */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 0 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut/absen siswa dalam kelas.
	 *
	 * <p>Satu-satunya pemanggil di UI adalah {@code Intbox} kolom "Nomor Urut" pada
	 * {@code DetailKelasSiswaHelper} (langsung disimpan lewat {@code Common.refreshUpdate} pada
	 * event {@code onChange}) dan alur unggah Excel massal di helper yang sama. Kedua jalur itu
	 * <b>tidak digerbangi hak akses</b> — lihat catatan hak akses pada dokumentasi kelas.</p>
	 *
	 * @param nomorUrut nomor urut; {@code null} akan dibaca sebagai {@code 0}
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Implementasi kontrak {@link ais.database.model.sekolah.VoKelasPunyaSiswa#ambilKelasSiswa()}:
	 * mengembalikan kelas pemilik baris ini.
	 *
	 * <p>Method ini ada agar mesin nilai di kelas induk dapat menanyakan kelas tanpa perlu tahu
	 * apakah dirinya kelas reguler ({@code kelasSiswa}) atau kelas les ({@code kelasLesSiswa}).
	 * Dipakai {@code retreiveDetailNilai()} dan {@code retreiveTotalNilai()} untuk membaca
	 * {@code getPublikasiNilaiHarusTelahDiverifikasi()} — yaitu untuk memutuskan apakah nilai yang
	 * belum diverifikasi guru boleh ikut ditampilkan/dihitung.</p>
	 *
	 * <p>Delegasi penuh ke {@link #getKelasSiswa()}, jadi efek resolusi proxy {@code check()}
	 * ikut berlaku.</p>
	 *
	 * @return kelas pemilik baris roster ini
	 */
	@Override
	public KelasSiswa ambilKelasSiswa() {
		// TODO Auto-generated method stub
		return getKelasSiswa();
	}

}
