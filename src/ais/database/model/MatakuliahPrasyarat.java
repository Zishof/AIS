package ais.database.model;

// Generated Dec 22, 2009 12:14:16 PM by Hibernate Tools 3.2.4.CR1

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
 * Entity Hibernate untuk <b>aturan prasyarat mata kuliah</b> pada tabel
 * {@code public.matakuliah_prasyarat}: satu baris = satu aturan "mata kuliah X baru boleh
 * diambil bila syarat Y sudah terpenuhi".
 *
 * <h3>Di sinilah relasi prasyarat sungguhan disimpan</h3>
 * <p>{@link Matakuliah} <b>tidak</b> punya field/koleksi prasyarat sama sekali — dokumentasi
 * kelas itu sudah mencatat bahwa relasinya "dipegang entity lain". Kelas inilah pemiliknya.
 * Karena tidak ada sisi terbalik ({@code inverse}) yang dipetakan, satu-satunya cara menemukan
 * prasyarat sebuah mata kuliah adalah <b>query eksplisit</b> ke tabel ini, persis seperti yang
 * dilakukan mesin validasinya:</p>
 *
 * <pre>
 * session.createCriteria(MatakuliahPrasyarat.class)
 *        .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
 *        .add(Restrictions.eq("matakuliah", matakuliah))
 * </pre>
 *
 * <p>Jangan mencari {@code getMatakuliahPrasyarats()} di {@link Matakuliah} — method itu tidak
 * ada dan memang tidak pernah ada.</p>
 *
 * <h3>Bentuk aturan: OR di dalam baris, AND antar baris</h3>
 * <p>Satu baris memuat <b>sampai sepuluh slot</b> mata kuliah prasyarat
 * ({@code matakuliahPrasyarat} sampai {@code matakuliahPrasyarat10}) yang bersifat
 * <b>alternatif</b> — dihubungkan dengan <i>ATAU</i>. Baris dianggap terpenuhi bila mahasiswa
 * sudah lulus <b>salah satu</b> dari slot yang terisi. Pesan kesalahan yang dibangun mesin
 * validasi memakai kata "atau" untuk slot ke-2 dan seterusnya, jadi maknanya konsisten.</p>
 * <p>Sebaliknya, satu {@link Matakuliah} boleh punya <b>banyak baris</b> di tabel ini, dan
 * seluruh baris harus terpenuhi — jadi antar baris berlaku <i>DAN</i>. Pola pemakaiannya:
 * satu baris per "kelompok syarat", tiap kelompok diisi alternatif-alternatifnya.</p>
 *
 * <h3>Tiga jenis syarat yang bisa dipasang pada satu baris</h3>
 * <ol>
 * <li><b>Syarat kelulusan mata kuliah</b> — slot 1..10 + {@link #getMinimalNilaiLulus()}.
 * Mahasiswa harus punya {@link Detailperkuliahan} berstatus {@code DISETUJUI} atas salah satu
 * MK slot, dengan {@code totalNilai} &ge; {@code minimalNilaiLulus}.</li>
 * <li><b>Syarat SKS lulus</b> — {@link #getMinimalSks()} dibandingkan dengan
 * {@code KrsMahasiswa.getSksk()}.</li>
 * <li><b>Syarat IPK</b> — {@link #getMinimalIpk()} dibandingkan dengan
 * {@code KrsMahasiswa.getIpk()}.</li>
 * </ol>
 * <p>Ketiganya boleh dicampur dalam satu baris, atau dipisah ke baris-baris berbeda. Baris yang
 * <b>hanya</b> memuat syarat SKS/IPK sah: slot 1 boleh {@code null}
 * ({@code matakuliah_prasyarat nullable = true}) dan mesin validasi memang melewati pemeriksaan
 * MK untuk baris semacam itu. Yang WAJIB terisi hanyalah {@link #getMatakuliah()}
 * ({@code nullable = false}) — mata kuliah yang <i>disyarati</i>.</p>
 *
 * <h3>Mesin validasi dan pemanggilnya</h3>
 * <p>Seluruh aturan di tabel ini dievaluasi oleh <b>satu</b> method:
 * {@code CommonAcademicSyncHelper.checkMatakuliahPrasyarat(Matakuliah, Mahasiswa, Integer)},
 * yang dipanggil lewat fasad {@code Common.checkMatakuliahPrasyarat(...)}. Alurnya:</p>
 * <ol>
 * <li>Ambil semua baris aktif untuk mata kuliah target (query di atas). Bila kosong &rarr;
 * langsung {@code true} (tidak ada prasyarat).</li>
 * <li>Saring baris yang punya {@code minimalSks &gt; 0} <b>atau</b> {@code minimalIpk &gt; 0.01},
 * lalu bandingkan dengan {@code KrsMahasiswa} hasil {@code Common.singkronkanKrsMahasiswa(...)}.</li>
 * <li>Untuk tiap baris yang slot-1-nya terisi: kumpulkan kode (atau id) seluruh slot terisi,
 * telusuri {@code mahasiswa.ambilDetailperkuliahan()}, hitung berapa yang cocok DAN disetujui
 * DAN nilainya mencukupi. {@code count == 0} berarti baris belum terpenuhi.</li>
 * <li>Bila ada satu saja syarat yang belum terpenuhi &rarr; tampilkan {@code MyMessageboxConfig}
 * berisi rincian dan kembalikan {@code false}.</li>
 * </ol>
 * <p><b>Efek samping penting:</b> mesin validasi itu <b>menampilkan messagebox ZK</b> saat gagal,
 * jadi ia hanya boleh dipanggil dari event thread UI, bukan dari batch/scheduler. Pemanggilnya
 * saat ini: {@code AmbilDataPerkuliahanHelper}, {@code AmbilDataPerkuliahanNonPaketHelper},
 * {@code AmbilDataMahasiswaHelper}, {@code AmbilDataMahasiswaForPaketPerkuliahanHelper},
 * {@code AmbilDataPaketPerkuliahanHelper}, {@code AmbilDataKurikulumPerkuliahanHelper}, dan
 * {@code GenerateKRSPaketMahasiswaOtomatisWindow} — semuanya jalur pengisian KRS.</p>
 *
 * <h3>Pencocokan by kode vs by id ({@code hanyaBerdasarkanKode})</h3>
 * <p>{@link #getHanyaBerdasarkanKode()} (default {@code true}) menentukan bagaimana riwayat kuliah
 * mahasiswa dicocokkan dengan slot prasyarat. Ini bukan detail sepele: revisi kurikulum di repo ini
 * lazim melahirkan baris {@link Matakuliah} <i>baru</i> dengan kode sama (lihat catatan
 * "Ekivalensi" di {@link Matakuliah}). Dengan {@code true}, mata kuliah lama yang kodenya sama
 * tetap diakui; dengan {@code false}, hanya baris {@code Matakuliah} dengan id persis itu yang
 * diakui, sehingga mahasiswa angkatan lama bisa tertahan. Ubah ke {@code false} hanya bila memang
 * ingin mengikat aturan ke satu baris kurikulum tertentu.</p>
 *
 * <h3>Pengelompokan anggota kelas ini</h3>
 * <ul>
 * <li><b>Jejak audit</b> — {@link #getOleh()}, {@link #getOlehId()},
 * {@link #getTanggal_dirubah()}, {@link #onUpdate()}. Lihat catatan "field bayangan" di bawah.</li>
 * <li><b>Identitas</b> — {@link #getId()}, {@link #toString()}.</li>
 * <li><b>Sisi yang disyarati</b> — {@link #getMatakuliah()} (wajib).</li>
 * <li><b>Sepuluh slot alternatif prasyarat</b> — {@link #getMatakuliahPrasyarat()} sampai
 * {@link #getMatakuliahPrasyarat10()}.</li>
 * <li><b>Ambang syarat</b> — {@link #getMinimalNilaiLulus()}, {@link #getMinimalSks()},
 * {@link #getMinimalIpk()}.</li>
 * <li><b>Sakelar &amp; catatan</b> — {@link #getAktif()}, {@link #getHanyaBerdasarkanKode()},
 * {@link #getKeterangan()}.</li>
 * </ul>
 *
 * <h3>Jebakan: getter yang tidak bebas efek samping</h3>
 * <p>Pemetaan kelas ini memakai <i>property access</i> (anotasi menempel pada getter), sehingga
 * <b>yang dibaca Hibernate saat flush adalah nilai kembalian getter, bukan isi field mentah</b>.
 * Konsekuensinya:</p>
 * <ul>
 * <li>Sebelas getter relasi memanggil {@link GeneralValueObject#check(Object)} dan
 * <b>menugaskan hasilnya kembali ke field</b> ({@code matakuliah = check(matakuliah)}). Ini
 * resolusi proxy lazy yang bisa menyentuh cache atau membuka session baru; murah pada kasus umum,
 * tetapi tetap berarti "sekadar membaca" bisa memicu I/O.</li>
 * <li>{@link #getAktif()} <b>menulis balik</b> {@code true} ke field bila nilainya {@code null}.
 * Kolom {@code aktif} adalah properti terpetakan sungguhan (bukan {@code @Transient}), jadi nilai
 * hasil logika getter itulah yang ikut tertulis ke DB pada {@code INSERT}/{@code UPDATE}
 * berikutnya. Justru karena baris warisan di DB masih bisa berisi {@code NULL}, query mesin
 * validasi sengaja memakai {@code isNull("aktif") OR aktif = true}.</li>
 * <li>{@link #getMinimalNilaiLulus()}, {@link #getMinimalSks()}, {@link #getMinimalIpk()}, dan
 * {@link #getHanyaBerdasarkanKode()} menormalkan {@code null} menjadi {@code 0.0}/{@code 0}/
 * {@code true} <b>tanpa</b> menugaskannya balik ke field — normalisasi hanya di nilai kembalian.
 * Tetap saja, karena property access, baris baru yang di-{@code INSERT} akan membawa angka
 * normal itu, bukan {@code NULL}.</li>
 * <li>{@link #toString()} memanggil dua getter relasi dan menugaskan hasilnya ke field — jadi
 * bahkan {@code toString()} pun bukan operasi baca murni.</li>
 * </ul>
 * <p>Tidak ada method di kelas ini yang menutup {@code Session} Hibernate milik pemanggil
 * (berbeda dengan {@code Matakuliah.reInitEkivalen()}); satu-satunya jalur yang bisa membuka
 * session sendiri adalah {@code check()} milik kelas induk, dan session itu ditutupnya sendiri.</p>
 *
 * <h3>Warisan {@link GeneralValueObject}</h3>
 * <p>Kontrak umum {@code id}/{@code equals}/{@code hashCode}/{@code compareTo}, resolusi proxy
 * lazy lewat {@link GeneralValueObject#check(Object)}, cache entity, dan penanda
 * {@code udah}/{@code belum} dijelaskan lengkap di {@link ais.database.model.GeneralValueObject} —
 * jangan diulang di sini.</p>
 *
 * <h3>Layar pengelola</h3>
 * <p>Master CRUD-nya adalah {@code ais.action.master.MatakuliahPrasyaratAction} (mendukung
 * cetak/unggah massal dengan urutan kolom {@code id, matakuliah, matakuliahPrasyarat..10,
 * minimalNilaiLulus, minimalSks, minimalIpk, hanyaBerdasarkanKode, aktif, keterangan}), dengan
 * panel bantu {@code ais.action.master.helper.MatakuliahPrasyaratHelper} yang menempel pada layar
 * mata kuliah. Kelas ini {@link Audited}, jadi setiap perubahan memunculkan revisi Envers dan
 * baris {@code RevisiHelper} pada layar.</p>
 *
 * @see Matakuliah
 * @see MatakuliahEkivalen
 * @see KurikulumPunyaMatakuliah
 * @see Detailperkuliahan
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "matakuliah_prasyarat")

public class MatakuliahPrasyarat extends GeneralValueObject {

	/**
	 * Versi serialisasi. Entity ini ikut diserialisasi ke cache in-memory milik
	 * {@link GeneralValueObject}, jadi nilainya tidak boleh diubah tanpa alasan kuat.
	 */
	private static final long serialVersionUID = 1950126270979098967L;
	/** Primary key {@code public.matakuliah_prasyarat.id}, IDENTITY. */
	private Long id;
	/**
	 * Nama pengguna terakhir yang mengubah baris ini.
	 *
	 * <p><b>Field bayangan — bukan bug.</b> {@link GeneralValueObject} <i>bukan</i>
	 * {@code @Entity} maupun {@code @MappedSuperclass}, melainkan POJO abstrak biasa, sehingga
	 * Hibernate <b>tidak</b> memetakan properti milik induk. Karena itu setiap entity yang ingin
	 * punya kolom {@code oleh}/{@code olehId}/{@code tanggal_dirubah}/{@code id} <b>harus</b>
	 * mendeklarasikan ulang field beserta getter/setter-nya di kelasnya sendiri — ini keharusan
	 * teknis, bukan duplikasi yang lupa dibersihkan. Efek sampingnya: {@code this.oleh} dan
	 * {@code ((GeneralValueObject) this).oleh} adalah dua slot berbeda, jadi selalu akses lewat
	 * {@link #getOleh()}/{@link #setOleh(String)}.</p>
	 */
	private String oleh;
	/**
	 * Id pengguna terakhir yang mengubah baris ini. Field bayangan atas
	 * {@code GeneralValueObject.olehId} — lihat catatan pada {@link #oleh}.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir pengubah baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah.
	 *
	 * <p>Nilai {@code null} atau string kosong <b>diabaikan diam-diam</b> (bukan disimpan sebagai
	 * null) sehingga jejak audit yang sudah ada tidak bisa terhapus oleh jalur penyimpanan yang
	 * kebetulan tidak menyertakan identitas pengguna.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila null/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah. Nilai null/kosong diabaikan, sama seperti
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna; diabaikan bila null/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir pengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: menyegarkan stempel waktu audit tepat sebelum
	 * {@code UPDATE} dikirim ke DB.
	 *
	 * <p>Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)}. Dipanggil oleh penyedia
	 * persistensi, bukan oleh kode aplikasi.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}
	/**
	 * Stempel waktu perubahan terakhir. Field bayangan atas
	 * {@code GeneralValueObject.tanggal_dirubah} (lihat catatan pada {@link #oleh});
	 * diinisialisasi ke waktu sekarang saat object dibuat, lalu diperbarui {@link #onUpdate()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir (presisi TIMESTAMP).
	 *
	 * <p>Tanpa {@code @Column}, sehingga nama kolomnya jatuh ke {@code MyNamingStrategy}
	 * (turunan {@code DefaultNamingStrategy}: nama kolom = nama properti apa adanya) &mdash;
	 * yaitu {@code tanggal_dirubah}.</p>
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks {@code "<mata kuliah>_<prasyarat slot 1>"}, mis.
	 * {@code "1204-RPL211401-REKAYASA PERANGKAT LUNAK_980-RPL211201-ALGORITMA"}.
	 *
	 * <p>Kedua bagian memakai {@code Matakuliah.toString()} yang berformat
	 * {@code "<id>-<kode>-<nama>"}, dipisah karakter garis bawah.</p>
	 *
	 * <p><b>Bukan operasi baca murni.</b> Method ini memanggil {@link #getMatakuliah()} dan
	 * {@link #getMatakuliahPrasyarat()} lalu <b>menugaskan hasilnya kembali ke field</b>, jadi ia
	 * ikut memicu resolusi proxy lazy lewat {@link GeneralValueObject#check(Object)} — bisa
	 * menyentuh cache atau database. Hati-hati memakainya di dalam logging yang ramai.</p>
	 *
	 * <p><b>Catatan:</b> hanya slot pertama yang ditampilkan; slot 2..10 tidak muncul sama sekali.
	 * Bila slot 1 kosong (baris yang hanya memuat syarat SKS/IPK), hasilnya berakhir dengan
	 * literal {@code "null"}.</p>
	 *
	 * @return teks gabungan mata kuliah dan prasyarat slot pertama
	 */
	public String toString() {
		matakuliah = getMatakuliah();
		matakuliahPrasyarat = getMatakuliahPrasyarat();
		return matakuliah + "_" + matakuliahPrasyarat;
	}

	/** Mata kuliah yang <i>disyarati</i> — sisi kiri aturan. Wajib terisi. */
	private Matakuliah matakuliah;
	/** Slot alternatif prasyarat ke-1. Boleh {@code null} bila baris hanya memuat syarat SKS/IPK. */
	private Matakuliah matakuliahPrasyarat;
	/** Slot alternatif prasyarat ke-2 (opsional). Dihubungkan <i>ATAU</i> dengan slot lainnya. */
	private Matakuliah matakuliahPrasyarat2;
	/** Slot alternatif prasyarat ke-3 (opsional). */
	private Matakuliah matakuliahPrasyarat3;
	/** Slot alternatif prasyarat ke-4 (opsional). */
	private Matakuliah matakuliahPrasyarat4;
	/** Slot alternatif prasyarat ke-5 (opsional). */
	private Matakuliah matakuliahPrasyarat5;
	/** Slot alternatif prasyarat ke-6 (opsional). */
	private Matakuliah matakuliahPrasyarat6;
	/** Slot alternatif prasyarat ke-7 (opsional). */
	private Matakuliah matakuliahPrasyarat7;
	/** Slot alternatif prasyarat ke-8 (opsional). */
	private Matakuliah matakuliahPrasyarat8;
	/** Slot alternatif prasyarat ke-9 (opsional). */
	private Matakuliah matakuliahPrasyarat9;
	/** Slot alternatif prasyarat ke-10 (opsional) — slot terakhir yang tersedia. */
	private Matakuliah matakuliahPrasyarat10;
	/** Ambang nilai akhir minimal agar sebuah slot dianggap "lulus"; {@code null} dibaca 0. */
	private Double minimalNilaiLulus;
	/** Catatan bebas operator; murni informatif, tidak ikut dievaluasi mesin validasi. */
	private String keterangan;
	/** Sakelar aktif/nonaktif aturan; {@code null} diperlakukan sama dengan aktif. */
	private Boolean aktif;
	/** Cocokkan riwayat kuliah berdasarkan kode MK (default) atau berdasarkan id baris MK. */
	private Boolean hanyaBerdasarkanKode;
	/** Ambang SKS lulus minimal; {@code null} dibaca 0 (berarti tidak ada syarat SKS). */
	private Integer minimalSks;
	/** Ambang IPK minimal; {@code null} dibaca 0.0 (berarti tidak ada syarat IPK). */
	private Double minimalIpk;

	/**
	 * Konstruktor tanpa argumen yang dibutuhkan Hibernate/JPA untuk instansiasi entity, dan
	 * dipakai layar master saat menambah aturan baru. Seluruh field dibiarkan pada nilai
	 * bawaannya kecuali {@code tanggal_dirubah} yang diisi waktu sekarang.
	 */
	public MatakuliahPrasyarat() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * <p>Kolom {@code id} dipetakan {@code insertable = false} karena nilainya dibangkitkan
	 * database ({@code GenerationType.IDENTITY}).</p>
	 *
	 * @return id baris, atau {@code null} bila object belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Umumnya hanya dipanggil Hibernate saat hidrasi entity.
	 *
	 * @param id primary key baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan mata kuliah yang <b>disyarati</b> aturan ini — sisi kiri relasi, kolom
	 * {@code matakuliah} ({@code nullable = false}).
	 *
	 * <p>Inilah kolom yang dipakai mesin validasi untuk menemukan aturan:
	 * {@code Restrictions.eq("matakuliah", matakuliah)}. Jangan tertukar dengan
	 * {@link #getMatakuliahPrasyarat()} yang merupakan sisi kanan (mata kuliah yang harus sudah
	 * lulus lebih dulu).</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@link GeneralValueObject#check(Object)} dan menugaskan
	 * hasilnya kembali ke field, sehingga proxy lazy diresolusi (bisa lewat cache atau session
	 * baru) sebelum dikembalikan.</p>
	 *
	 * @return mata kuliah yang disyarati; secara praktik tidak pernah {@code null} untuk baris
	 *         yang tersimpan di database
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "matakuliah", nullable = false)
	public Matakuliah getMatakuliah() {
		matakuliah = check(matakuliah);
		return this.matakuliah;
	}

	/**
	 * Menyetel mata kuliah yang disyarati (sisi kiri aturan).
	 *
	 * @param matakuliah mata kuliah target; wajib terisi sebelum baris disimpan
	 */
	public void setMatakuliah(Matakuliah matakuliah) {
		this.matakuliah = matakuliah;
	}

	/**
	 * Mengembalikan <b>slot alternatif prasyarat ke-1</b> — kolom {@code matakuliah_prasyarat}
	 * ({@code nullable = true}).
	 *
	 * <p>Slot ini istimewa dibanding slot 2..10: mesin validasi memakainya sebagai penanda
	 * "baris ini memuat syarat mata kuliah atau tidak". Bila slot 1 {@code null}, seluruh
	 * pemeriksaan mata kuliah untuk baris tersebut <b>dilewati</b> ({@code continue}) — slot 2..10
	 * tidak diperiksa sama sekali walaupun terisi. Baris seperti itu hanya berguna untuk syarat
	 * SKS/IPK. Slot 1 juga satu-satunya yang ikut muncul di {@link #toString()}.</p>
	 *
	 * <p><b>Efek samping:</b> resolusi proxy lazy dengan tulis balik ke field, sama seperti
	 * {@link #getMatakuliah()}.</p>
	 *
	 * @return mata kuliah prasyarat slot ke-1, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "matakuliah_prasyarat", nullable = true)
	public Matakuliah getMatakuliahPrasyarat() {
		matakuliahPrasyarat = check(matakuliahPrasyarat);
		return this.matakuliahPrasyarat;
	}

	/**
	 * Menyetel slot alternatif prasyarat ke-1.
	 *
	 * @param matakuliahPrasyarat mata kuliah prasyarat; {@code null} berarti baris ini tidak
	 *        memuat syarat mata kuliah sama sekali (lihat {@link #getMatakuliahPrasyarat()})
	 */
	public void setMatakuliahPrasyarat(Matakuliah matakuliahPrasyarat) {
		this.matakuliahPrasyarat = matakuliahPrasyarat;
	}

	/**
	 * Mengembalikan ambang <b>nilai akhir minimal</b> agar sebuah mata kuliah slot dianggap lulus.
	 *
	 * <p>Dibandingkan dengan {@code Detailperkuliahan.getTotalNilai()}: riwayat dengan nilai
	 * {@code null} atau lebih kecil dari ambang ini tidak dihitung. Nilai {@code 0.0} (juga hasil
	 * normalisasi {@code null}) berarti "asal mata kuliahnya pernah diambil dan disetujui" —
	 * praktis tanpa syarat nilai.</p>
	 *
	 * <p>Berlaku untuk <b>seluruh</b> slot 1..10 pada baris ini; tidak ada ambang per-slot.</p>
	 *
	 * <p>Kolom tanpa {@code @Column}, jadi namanya mengikuti {@code MyNamingStrategy}
	 * (nama properti apa adanya: {@code minimalNilaiLulus}).</p>
	 *
	 * @return ambang nilai minimal, atau {@code 0.0} bila belum diisi
	 */
	public Double getMinimalNilaiLulus() {
		return minimalNilaiLulus == null ? 0.0 : minimalNilaiLulus;
	}

	/**
	 * Menyetel ambang nilai akhir minimal kelulusan prasyarat.
	 *
	 * <p>Dipanggil dari layar master dan dari panel {@code MatakuliahPrasyaratHelper} (event
	 * {@code onChange} pada {@code MyDoublebox}).</p>
	 *
	 * @param minimalNilaiLulus ambang nilai; {@code null} akan dibaca sebagai {@code 0.0}
	 */
	public void setMinimalNilaiLulus(Double minimalNilaiLulus) {
		this.minimalNilaiLulus = minimalNilaiLulus;
	}

	/**
	 * Mengembalikan catatan bebas operator untuk aturan ini.
	 *
	 * <p>Murni informatif: hanya ditampilkan di kolom terakhir layar master dan ikut cetak/unggah
	 * massal. Tidak pernah dibaca mesin validasi.</p>
	 *
	 * @return keterangan, atau {@code null} bila kosong
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel catatan bebas operator.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan sakelar aktif aturan ini; {@code null} diperlakukan sebagai <b>aktif</b>.
	 *
	 * <p><b>Getter yang menulis balik.</b> Bila field masih {@code null}, method ini mengisinya
	 * dengan {@code true} lebih dulu, baru mengembalikannya. Karena {@code aktif} adalah properti
	 * terpetakan sungguhan (bukan {@code @Transient}) dan pemetaan kelas ini memakai
	 * <i>property access</i>, nilai hasil logika inilah yang ikut tertulis ke database pada
	 * {@code INSERT}/{@code UPDATE} berikutnya — bukan {@code null} aslinya.</p>
	 *
	 * <p>Justru karena baris lama di database masih bisa berisi {@code NULL}, mesin validasi tidak
	 * boleh menyaring dengan {@code aktif = true} saja; query yang dipakai adalah
	 * {@code Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))}.
	 * Menonaktifkan aturan berarti aturan itu benar-benar hilang dari pemeriksaan KRS.</p>
	 *
	 * @return {@code true} bila aturan aktif (termasuk saat nilainya belum pernah diisi)
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menyetel sakelar aktif aturan.
	 *
	 * <p>Dipanggil dari checkbox "Aktif" pada layar master, yang langsung disusul
	 * {@code Common.refreshSaveOrUpdate(...)} sehingga perubahannya tersimpan seketika.</p>
	 *
	 * @param aktif {@code true} untuk mengaktifkan, {@code false} untuk menonaktifkan
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan slot alternatif prasyarat ke-2 — kolom {@code matakuliah_prasyarat2}.
	 *
	 * <p>Alternatif (<i>ATAU</i>) terhadap slot lain pada baris yang sama. Hanya diperiksa bila
	 * {@link #getMatakuliahPrasyarat()} tidak {@code null}. Efek samping resolusi proxy lazy sama
	 * seperti {@link #getMatakuliah()}.</p>
	 *
	 * @return mata kuliah prasyarat slot ke-2, atau {@code null} bila slot tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "matakuliah_prasyarat2", nullable = true)
	public Matakuliah getMatakuliahPrasyarat2() {
		matakuliahPrasyarat2 = check(matakuliahPrasyarat2);
		return matakuliahPrasyarat2;
	}

	/**
	 * Menyetel slot alternatif prasyarat ke-2.
	 *
	 * @param matakuliahPrasyarat2 mata kuliah alternatif; {@code null} untuk mengosongkan slot
	 */
	public void setMatakuliahPrasyarat2(Matakuliah matakuliahPrasyarat2) {
		this.matakuliahPrasyarat2 = matakuliahPrasyarat2;
	}

	/**
	 * Mengembalikan slot alternatif prasyarat ke-3 — kolom {@code matakuliah_prasyarat3}.
	 * Semantik dan efek sampingnya identik dengan {@link #getMatakuliahPrasyarat2()}.
	 *
	 * @return mata kuliah prasyarat slot ke-3, atau {@code null} bila slot tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "matakuliah_prasyarat3", nullable = true)
	public Matakuliah getMatakuliahPrasyarat3() {
		matakuliahPrasyarat3 = check(matakuliahPrasyarat3);
		return matakuliahPrasyarat3;
	}

	/**
	 * Menyetel slot alternatif prasyarat ke-3.
	 *
	 * @param matakuliahPrasyarat3 mata kuliah alternatif; {@code null} untuk mengosongkan slot
	 */
	public void setMatakuliahPrasyarat3(Matakuliah matakuliahPrasyarat3) {
		this.matakuliahPrasyarat3 = matakuliahPrasyarat3;
	}

	/**
	 * Mengembalikan slot alternatif prasyarat ke-5 — kolom {@code matakuliah_prasyarat5}.
	 * Semantik dan efek sampingnya identik dengan {@link #getMatakuliahPrasyarat2()}.
	 *
	 * <p><b>Catatan kerapian:</b> pasangan getter/setter slot ke-5 sengaja dibiarkan berada
	 * <i>sebelum</i> slot ke-4 di berkas sumber ini, persis seperti aslinya. Urutan deklarasi
	 * tidak berpengaruh pada pemetaan maupun pada mesin validasi (yang memeriksa slot 2..10
	 * berurutan), tetapi mudah membingungkan saat membaca cepat.</p>
	 *
	 * @return mata kuliah prasyarat slot ke-5, atau {@code null} bila slot tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "matakuliah_prasyarat5", nullable = true)
	public Matakuliah getMatakuliahPrasyarat5() {
		matakuliahPrasyarat5 = check(matakuliahPrasyarat5);
		return matakuliahPrasyarat5;
	}

	/**
	 * Menyetel slot alternatif prasyarat ke-5.
	 *
	 * @param matakuliahPrasyarat5 mata kuliah alternatif; {@code null} untuk mengosongkan slot
	 */
	public void setMatakuliahPrasyarat5(Matakuliah matakuliahPrasyarat5) {
		this.matakuliahPrasyarat5 = matakuliahPrasyarat5;
	}

	/**
	 * Mengembalikan slot alternatif prasyarat ke-4 — kolom {@code matakuliah_prasyarat4}.
	 * Semantik dan efek sampingnya identik dengan {@link #getMatakuliahPrasyarat2()}.
	 *
	 * @return mata kuliah prasyarat slot ke-4, atau {@code null} bila slot tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "matakuliah_prasyarat4", nullable = true)
	public Matakuliah getMatakuliahPrasyarat4() {
		matakuliahPrasyarat4 = check(matakuliahPrasyarat4);
		return matakuliahPrasyarat4;
	}

	/**
	 * Menyetel slot alternatif prasyarat ke-4.
	 *
	 * @param matakuliahPrasyarat4 mata kuliah alternatif; {@code null} untuk mengosongkan slot
	 */
	public void setMatakuliahPrasyarat4(Matakuliah matakuliahPrasyarat4) {
		this.matakuliahPrasyarat4 = matakuliahPrasyarat4;
	}

	/**
	 * Mengembalikan cara pencocokan riwayat kuliah mahasiswa dengan slot prasyarat;
	 * {@code null} diperlakukan sebagai {@code true}.
	 *
	 * <ul>
	 * <li>{@code true} (default) — cocokkan berdasarkan <b>kode</b> mata kuliah
	 * ({@code Matakuliah.getKode()}). Semua baris {@code Matakuliah} berkode sama diakui, dari
	 * kurikulum atau prodi mana pun. Ini yang biasanya diinginkan, karena revisi kurikulum
	 * melahirkan baris mata kuliah baru dengan kode yang sama.</li>
	 * <li>{@code false} — cocokkan berdasarkan <b>id</b> baris {@code Matakuliah}. Ketat: hanya
	 * baris kurikulum persis itu yang diakui, sehingga mahasiswa angkatan lama dapat tertahan.</li>
	 * </ul>
	 *
	 * <p>Perhatikan bahwa pencocokan berbasis kode <b>tidak</b> menyaring prodi atau kurikulum,
	 * jadi kode yang kebetulan sama antar prodi akan saling diakui.</p>
	 *
	 * <p>Berbeda dengan {@link #getAktif()}, method ini <b>tidak</b> menulis balik ke field;
	 * normalisasinya hanya pada nilai kembalian.</p>
	 *
	 * @return {@code true} bila pencocokan memakai kode mata kuliah, {@code false} bila memakai id
	 */
	public Boolean getHanyaBerdasarkanKode() {
		return hanyaBerdasarkanKode == null ? true : hanyaBerdasarkanKode;
	}

	/**
	 * Menyetel cara pencocokan riwayat kuliah (kode vs id).
	 *
	 * <p>Dipanggil dari checkbox "Hanya Berdasarkan Kode MK" pada layar master, yang langsung
	 * disusul {@code Common.refreshSaveOrUpdate(...)}.</p>
	 *
	 * @param hanyaBerdasarkanKode {@code true} untuk cocokkan by kode, {@code false} untuk by id;
	 *        {@code null} akan dibaca sebagai {@code true}
	 */
	public void setHanyaBerdasarkanKode(Boolean hanyaBerdasarkanKode) {
		this.hanyaBerdasarkanKode = hanyaBerdasarkanKode;
	}

	/**
	 * Mengembalikan ambang <b>SKS lulus minimal</b> yang harus sudah dikumpulkan mahasiswa;
	 * {@code null} dibaca {@code 0}.
	 *
	 * <p>Dibandingkan dengan {@code KrsMahasiswa.getSksk()}. Nilai {@code 0} berarti tidak ada
	 * syarat SKS: mesin validasi hanya menyertakan baris yang {@code minimalSks &gt; 0} (atau
	 * {@code minimalIpk &gt; 0.01}) ke dalam pemeriksaan tahap ini.</p>
	 *
	 * <p>Syarat SKS/IPK berdiri sendiri terhadap slot mata kuliah: sebuah baris boleh memuat
	 * hanya syarat ini, dengan seluruh slot dikosongkan.</p>
	 *
	 * @return ambang SKS minimal, atau {@code 0} bila belum diisi
	 */
	public Integer getMinimalSks() {
		return minimalSks == null ? 0 : minimalSks;
	}

	/**
	 * Menyetel ambang SKS lulus minimal.
	 *
	 * @param minimalSks ambang SKS; {@code null} akan dibaca sebagai {@code 0} (tanpa syarat)
	 */
	public void setMinimalSks(Integer minimalSks) {
		this.minimalSks = minimalSks;
	}

	/**
	 * Mengembalikan ambang <b>IPK minimal</b> yang harus dicapai mahasiswa; {@code null} dibaca
	 * {@code 0.0}.
	 *
	 * <p>Dibandingkan dengan {@code KrsMahasiswa.getIpk()}. <b>Perhatikan ambang penyaringnya:</b>
	 * mesin validasi hanya memperhitungkan baris dengan {@code minimalIpk &gt; 0.01} — bukan
	 * {@code &gt; 0} — sehingga syarat IPK bernilai {@code 0.01} atau lebih kecil diabaikan diam-diam.
	 * Nilai sekecil itu memang tidak bermakna dalam praktik, tetapi perbedaan ambang ini perlu
	 * diingat saat menelusuri kasus "syarat kok tidak jalan".</p>
	 *
	 * @return ambang IPK minimal, atau {@code 0.0} bila belum diisi
	 */
	public Double getMinimalIpk() {
		return minimalIpk == null ? 0.0 : minimalIpk;
	}

	/**
	 * Menyetel ambang IPK minimal.
	 *
	 * @param minimalIpk ambang IPK; {@code null} akan dibaca sebagai {@code 0.0} (tanpa syarat)
	 */
	public void setMinimalIpk(Double minimalIpk) {
		this.minimalIpk = minimalIpk;
	}

	/**
	 * Mengembalikan slot alternatif prasyarat ke-6 — kolom {@code matakuliah_prasyarat6}.
	 * Semantik dan efek sampingnya identik dengan {@link #getMatakuliahPrasyarat2()}.
	 *
	 * @return mata kuliah prasyarat slot ke-6, atau {@code null} bila slot tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "matakuliah_prasyarat6", nullable = true)
	public Matakuliah getMatakuliahPrasyarat6() {
		matakuliahPrasyarat6 = check(matakuliahPrasyarat6);
		return matakuliahPrasyarat6;
	}

	/**
	 * Menyetel slot alternatif prasyarat ke-6.
	 *
	 * @param matakuliahPrasyarat6 mata kuliah alternatif; {@code null} untuk mengosongkan slot
	 */
	public void setMatakuliahPrasyarat6(Matakuliah matakuliahPrasyarat6) {
		this.matakuliahPrasyarat6 = matakuliahPrasyarat6;
	}

	/**
	 * Mengembalikan slot alternatif prasyarat ke-7 — kolom {@code matakuliah_prasyarat7}.
	 * Semantik dan efek sampingnya identik dengan {@link #getMatakuliahPrasyarat2()}.
	 *
	 * @return mata kuliah prasyarat slot ke-7, atau {@code null} bila slot tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "matakuliah_prasyarat7", nullable = true)
	public Matakuliah getMatakuliahPrasyarat7() {
		matakuliahPrasyarat7 = check(matakuliahPrasyarat7);
		return matakuliahPrasyarat7;
	}

	/**
	 * Menyetel slot alternatif prasyarat ke-7.
	 *
	 * @param matakuliahPrasyarat7 mata kuliah alternatif; {@code null} untuk mengosongkan slot
	 */
	public void setMatakuliahPrasyarat7(Matakuliah matakuliahPrasyarat7) {
		this.matakuliahPrasyarat7 = matakuliahPrasyarat7;
	}

	/**
	 * Mengembalikan slot alternatif prasyarat ke-8 — kolom {@code matakuliah_prasyarat8}.
	 * Semantik dan efek sampingnya identik dengan {@link #getMatakuliahPrasyarat2()}.
	 *
	 * @return mata kuliah prasyarat slot ke-8, atau {@code null} bila slot tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "matakuliah_prasyarat8", nullable = true)
	public Matakuliah getMatakuliahPrasyarat8() {
		matakuliahPrasyarat8 = check(matakuliahPrasyarat8);
		return matakuliahPrasyarat8;
	}

	/**
	 * Menyetel slot alternatif prasyarat ke-8.
	 *
	 * @param matakuliahPrasyarat8 mata kuliah alternatif; {@code null} untuk mengosongkan slot
	 */
	public void setMatakuliahPrasyarat8(Matakuliah matakuliahPrasyarat8) {
		this.matakuliahPrasyarat8 = matakuliahPrasyarat8;
	}

	/**
	 * Mengembalikan slot alternatif prasyarat ke-9 — kolom {@code matakuliah_prasyarat9}.
	 * Semantik dan efek sampingnya identik dengan {@link #getMatakuliahPrasyarat2()}.
	 *
	 * @return mata kuliah prasyarat slot ke-9, atau {@code null} bila slot tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "matakuliah_prasyarat9", nullable = true)
	public Matakuliah getMatakuliahPrasyarat9() {
		matakuliahPrasyarat9 = check(matakuliahPrasyarat9);
		return matakuliahPrasyarat9;
	}

	/**
	 * Menyetel slot alternatif prasyarat ke-9.
	 *
	 * @param matakuliahPrasyarat9 mata kuliah alternatif; {@code null} untuk mengosongkan slot
	 */
	public void setMatakuliahPrasyarat9(Matakuliah matakuliahPrasyarat9) {
		this.matakuliahPrasyarat9 = matakuliahPrasyarat9;
	}

	/**
	 * Mengembalikan slot alternatif prasyarat ke-10 — kolom {@code matakuliah_prasyarat10}, slot
	 * terakhir yang tersedia. Semantik dan efek sampingnya identik dengan
	 * {@link #getMatakuliahPrasyarat2()}.
	 *
	 * <p>Bila sebuah mata kuliah butuh lebih dari sepuluh alternatif, tabel ini tidak bisa
	 * menampungnya dalam satu baris; batasnya keras karena slot dipetakan sebagai kolom, bukan
	 * koleksi.</p>
	 *
	 * @return mata kuliah prasyarat slot ke-10, atau {@code null} bila slot tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "matakuliah_prasyarat10", nullable = true)
	public Matakuliah getMatakuliahPrasyarat10() {
		matakuliahPrasyarat10 = check(matakuliahPrasyarat10);
		return matakuliahPrasyarat10;
	}

	/**
	 * Menyetel slot alternatif prasyarat ke-10.
	 *
	 * @param matakuliahPrasyarat10 mata kuliah alternatif; {@code null} untuk mengosongkan slot
	 */
	public void setMatakuliahPrasyarat10(Matakuliah matakuliahPrasyarat10) {
		this.matakuliahPrasyarat10 = matakuliahPrasyarat10;
	}

}
