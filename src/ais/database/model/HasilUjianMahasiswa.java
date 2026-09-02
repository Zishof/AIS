package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Label;

import ais.action.master.helper.PertemuanPunyaUjianHelper;
import ais.action.master.helper.ProsesUjianHelper;
import ais.action.master.pmb.TampilanUjianCalonMahasiswa;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyArrayList;
import ais.ui.util.MyHashMap;
import ais.ui.util.MyWindow;

/**
 * Entity <b>hasil ujian per peserta</b> (tabel {@code public.hasil_ujian_mahasiswa}) — satu baris
 * mewakili satu peserta yang mengikuti satu sesi ujian: kapan mulai, kapan selesai, berapa lama
 * dikerjakan, berapa soal terjawab, berapa skor, lulus atau tidak, sampai rekap pelanggaran
 * pengawasan (anti-curang). Baris ini adalah <i>induk</i> dari kumpulan
 * {@link HasilUjianMahasiswaDetail} (satu baris per jawaban terhadap satu soal).
 *
 * <h3>Empat jenis peserta dalam satu tabel</h3>
 * <p>Modul ujian dipakai lintas domain, sehingga entity ini punya <b>empat</b> relasi peserta yang
 * saling eksklusif — tepat satu yang terisi, tiga sisanya {@code null}:</p>
 * <ul>
 *   <li>{@link #getMahasiswa()} — mahasiswa aktif (ujian perkuliahan / e-learning);</li>
 *   <li>{@link #getBiodataCalonMahasiswa()} — pendaftar PMB (ujian saringan masuk);</li>
 *   <li>{@link #getSiswa()} — siswa sekolah (modul {@code ais.database.model.sekolah});</li>
 *   <li>{@link #getCalonSiswa()} — calon siswa (PPDB).</li>
 * </ul>
 * <p>Konsekuensinya hampir semua kode pemanggil menerima keempatnya sekaligus (lihat
 * {@link #genKey(PertemuanPunyaUjian, Mahasiswa, BiodataCalonMahasiswa, Siswa, CalonSiswa)} dan
 * {@link #ambilByKey(PertemuanPunyaUjian, Mahasiswa, BiodataCalonMahasiswa, Siswa, CalonSiswa)}),
 * dan tidak ada constraint database yang memaksa "tepat satu" — itu murni konvensi kode.</p>
 *
 * <h3>Relasi utama</h3>
 * <ul>
 *   <li>{@link PertemuanPunyaUjian} (wajib, {@code nullable = false}) — sesi ujian yang diikuti;
 *       dari sini didapat {@link Ujian} (jenis soal, nilai lulus), durasi ({@code getLama()}),
 *       jumlah soal ditampilkan ({@code getJmlDitampilkan()}), mode "waktu per soal"
 *       ({@code getTiapSoal()}), dan jendela waktu ujian.</li>
 *   <li>{@link HasilUjianMahasiswaDetail} — <b>tidak</b> dipetakan sebagai koleksi Hibernate.
 *       Anak-anaknya diambil lewat query manual di {@link #ambilDataAsli(MyArrayList, boolean)}
 *       (sengaja: satu peserta bisa punya ratusan detail dan aksesnya perlu cache sendiri).</li>
 *   <li>{@link BankSoal}/{@link BankSoalDetail}/{@link UjianPunyaSoal} — dicapai secara tidak
 *       langsung lewat {@code HasilUjianMahasiswaDetail}.</li>
 * </ul>
 *
 * <h3>Pengelompokan method</h3>
 * <ol>
 *   <li><b>Field audit bayangan</b> ({@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()}, {@link #onUpdate()}) — lihat catatan arsitektur di bawah.</li>
 *   <li><b>Getter/setter sederhana</b> — id, keterangan, relasi peserta, log pelanggaran, dst.</li>
 *   <li><b>Getter berlogika (turunan &amp; menulis balik)</b> — {@link #getNilai()},
 *       {@link #getLulus()}, {@link #getJumlahSoal()}, {@link #getLamaPengerjaan()},
 *       {@link #getSisaWaktuPengerjaan()}, {@link #getSelesaiPada()}, {@link #getKeyhasil()}.
 *       Kelompok ini paling penting dan paling berbahaya; lihat bagian berikutnya.</li>
 *   <li><b>Pengambil jawaban peserta</b> — keluarga {@code ambilHasilUjianMahasiswaDetail(...)},
 *       {@code ambilBankSoalIdTerjawab*(...)}, {@link #ambilUjianPunyaSoals(int, Label, boolean)},
 *       dan mesin cache statik {@link #ambilDataAsli(MyArrayList, boolean)}.</li>
 *   <li><b>Utilitas statik</b> — {@link #genKey(PertemuanPunyaUjian, Mahasiswa,
 *       BiodataCalonMahasiswa, Siswa, CalonSiswa)},
 *       {@link #ambilByKey(PertemuanPunyaUjian, Mahasiswa, BiodataCalonMahasiswa, Siswa,
 *       CalonSiswa)} (get-or-create), dan {@link #tampilkanUjianKembali(String)} (melanjutkan
 *       ujian yang belum selesai saat pengguna login ulang).</li>
 * </ol>
 *
 * <h3>Hal non-obvious yang WAJIB diketahui sebelum menyentuh file ini</h3>
 * <ol>
 *   <li><b>Banyak getter menulis balik ke field-nya sendiri.</b> {@link #getNilai()},
 *       {@link #getLulus()}, {@link #getJumlahSoal()}, {@link #getLamaPengerjaan()},
 *       {@link #getSisaWaktuPengerjaan()}, {@link #getSelesaiPada()}, {@link #getKeyhasil()} dan
 *       semua getter relasi (lewat {@link GeneralValueObject#check(Object)}) menugaskan hasil
 *       hitungan ke field instance. Karena kelas ini memakai <b>akses properti</b> (anotasi ada di
 *       getter), Hibernate memanggil getter tersebut saat dirty-check/flush — sehingga
 *       <b>nilai hasil hitungan itu ikut tersimpan ke database</b> walau tidak ada
 *       {@code setXxx()} yang pernah dipanggil kode aplikasi. Ini disengaja (kolom dipakai untuk
 *       laporan &amp; ekspor), tetapi berarti mengubah rumus di getter = mengubah data tersimpan.</li>
 *   <li><b>Dua getter waktu memprioritaskan nilai tercatat.</b> {@link #getLamaPengerjaan()} dan
 *       {@link #getSelesaiPada()} mengembalikan nilai lama apa adanya bila sudah pernah diisi
 *       {@code ProsesUjianHelper.hitungWaktu(...)}; hitung-ulang di dalamnya hanya
 *       <i>fallback</i>. Penjaga ini ditambahkan setelah insiden "waktu selesai di masa depan"
 *       (lihat komentar di masing-masing method) — jangan dilepas.</li>
 *   <li><b>{@link #getSisaWaktuPengerjaan()} membaca cache berkas, bukan kolom database.</b>
 *       Selama ujian berlangsung, sisa waktu "hidup" ditulis ke cache berkas per-entity
 *       ({@link GeneralValueObject#put(String, String)}/{@link GeneralValueObject#retreive()}).
 *       Getter ini menimpa field dari cache itu — akurat saat ujian berjalan, <b>bisa basi</b>
 *       setelah ujian selesai. Itulah akar masalah yang dijinakkan oleh butir 2.</li>
 *   <li><b>{@link #ambilByKey} melakukan tulis ke database.</b> Namanya terdengar seperti "ambil",
 *       tetapi ia membuat dan menyimpan baris baru bila belum ada (pola <i>get-or-create</i>),
 *       serta mengisi {@code keyhasil} untuk baris lama yang belum punya. Jangan dipanggil dari
 *       jalur yang seharusnya bebas efek samping (mis. laporan read-only).</li>
 *   <li><b>Cache statik lintas-thread.</b> {@code ygSudahDiambils} menyimpan daftar id detail per
 *       {@code hasilUjianMahasiswa.id} untuk seluruh JVM. Data basi disegarkan hanya dengan
 *       {@code refresh = true}. Semua akses peta disinkronkan karena {@code loadData} memakai
 *       thread-pool.</li>
 *   <li><b>Skala nilai.</b> {@link #getNilai()} adalah <i>persentase</i> 0..100 hasil koreksi
 *       otomatis pilihan ganda, sedangkan {@link #getTotalNilai()} adalah kolom pasif untuk skor
 *       yang ditetapkan di luar (mis. koreksi manual esai). Keduanya berbeda dan tidak
 *       disinkronkan otomatis.</li>
 * </ol>
 *
 * <h3>Catatan arsitektur: field audit dideklarasikan ulang</h3>
 * <p>Field {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} tampak
 * "diduplikasi" dari {@link GeneralValueObject}. Itu <b>bukan kelalaian</b>:
 * {@code GeneralValueObject} bukan {@code @Entity} maupun {@code @MappedSuperclass} — ia POJO
 * abstrak biasa, sehingga Hibernate sama sekali tidak memetakan propertinya. Setiap entity
 * konkret wajib mendeklarasikan ulang field auditnya sendiri agar terpetakan ke kolom. Perilaku
 * non-persisten yang diwarisi (cache berkas {@code put}/{@code retreive}, resolusi lazy
 * {@code check}, cache object {@code ambilData}) tetap berlaku penuh.</p>
 *
 * <p>Entity ini {@code @Audited} (Hibernate Envers, riwayat revisi disimpan) dan memakai
 * {@code dynamicInsert}/{@code dynamicUpdate} agar SQL hanya menyertakan kolom yang benar-benar
 * berubah — penting karena tabel ini lebar dan sering di-update satu kolom saja saat ujian
 * berjalan.</p>
 *
 * @see GeneralValueObject
 * @see HasilUjianMahasiswaDetail
 * @see PertemuanPunyaUjian
 * @see ais.action.master.helper.ProsesUjianHelper
 * @see ais.action.master.helper.HasilUjianMahasiswaHelper
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "hasil_ujian_mahasiswa")
public class HasilUjianMahasiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi. Nilainya dikunci karena instance entity ini ikut diserialisasi ke cache
	 * berkas/MapDB ({@link GeneralValueObject#masukkanData(Class, GeneralValueObject)} dan
	 * kerabatnya); mengubahnya membuat cache lama tak terbaca.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Primary key {@code hasil_ujian_mahasiswa.id}, {@code IDENTITY} (diisi database). */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris ini (field audit; lihat catatan class-level). */
	private String oleh;

	/** Id pengguna terakhir yang mengubah baris ini (field audit; lihat catatan class-level). */
	private String olehId;

	/**
	 * Id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah tercatat
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Perilaku non-standar:</b> nilai {@code null} atau string kosong/spasi
	 * <b>diabaikan diam-diam</b> — field lama dipertahankan. Ini disengaja agar jejak audit yang
	 * sudah ada tidak terhapus oleh pemanggil yang kebetulan tidak punya konteks pengguna
	 * (mis. proses batch/scheduler). Konsekuensinya field audit tidak bisa dikosongkan lewat
	 * setter ini.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong = tidak melakukan apa-apa
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: nilai {@code null}/kosong diabaikan diam-diam
	 * sehingga jejak audit lama tidak tertimpa.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong = tidak melakukan apa-apa
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah tercatat
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: menyegarkan stempel waktu audit tepat sebelum baris
	 * di-{@code UPDATE}, dengan mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}.
	 *
	 * <p>Dipanggil oleh provider persistence, bukan oleh kode aplikasi. Pada baris yang sama juga
	 * dideklarasikan field {@code tanggal_dirubah} yang diinisialisasi ke waktu server saat ini
	 * ({@code WaktuUtil.getDate()}) agar baris baru pun sudah punya stempel waktu tanpa menunggu
	 * update pertama. Penggabungan callback dan deklarasi field dalam satu baris adalah gaya
	 * penyisipan otomatis yang dipakai konsisten di seluruh entity repo ini — jangan dipecah
	 * sembarangan karena skrip pemeliharaan mencocokkan pola ini.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan; biasanya diisi otomatis oleh {@link #onUpdate()}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir baris ini.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk instance baru karena
	 *         field-nya diinisialisasi ke waktu sekarang
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat: {@code mahasiswa-biodataCalonMahasiswa-pertemuanPunyaUjian-nilai}.
	 *
	 * <p><b>Perhatian — bukan {@code toString()} yang murah.</b> Method ini memanggil
	 * {@link #getMahasiswa()}, {@link #getBiodataCalonMahasiswa()}, dan
	 * {@link #getPertemuanPunyaUjian()} yang masing-masing me-resolusi proxy lazy lewat
	 * {@link GeneralValueObject#check(Object)} (bisa memicu query database) <b>dan menulis balik
	 * hasilnya ke field instance</b>. Jadi memanggilnya di dalam logging bervolume tinggi atau di
	 * dalam loop dapat menimbulkan lonjakan query. Nilai yang ditampilkan adalah field
	 * {@code nilai} mentah (bukan hasil {@link #getNilai()}), sehingga bisa berbeda dari angka
	 * yang dilihat pengguna.</p>
	 *
	 * @return ringkasan peserta + sesi ujian + nilai mentah
	 */
	public String toString() {
		mahasiswa = getMahasiswa();
		biodataCalonMahasiswa = getBiodataCalonMahasiswa();
		pertemuanPunyaUjian = getPertemuanPunyaUjian();
		return mahasiswa + "-" + biodataCalonMahasiswa + "-" + pertemuanPunyaUjian + "-" + nilai;
	}

	/** Peserta berupa mahasiswa aktif; salah satu dari empat relasi peserta yang saling eksklusif. */
	private Mahasiswa mahasiswa;

	/** Peserta berupa pendaftar PMB (ujian saringan masuk perguruan tinggi). */
	private BiodataCalonMahasiswa biodataCalonMahasiswa;

	/** Peserta berupa siswa sekolah (modul {@code ais.database.model.sekolah}). */
	private Siswa siswa;

	/** Peserta berupa calon siswa (PPDB). */
	private CalonSiswa calonSiswa;

	/** Sesi ujian yang diikuti (wajib). Sumber jenis soal, durasi, jumlah soal, dan nilai lulus. */
	private PertemuanPunyaUjian pertemuanPunyaUjian;

	/** Catatan bebas pengawas/dosen atas hasil ini; ditampilkan di rekap peserta. */
	private String keterangan;

	/**
	 * Skor final yang ditetapkan dari luar (mis. hasil koreksi manual esai atau bobot OBE).
	 * Kolom <b>pasif</b> — entity ini tidak pernah menghitungnya sendiri; bandingkan dengan
	 * {@link #nilai} yang merupakan persentase hasil koreksi otomatis.
	 */
	private Double totalNilai = 0.0;

	/**
	 * Durasi pengerjaan sebagai jam:menit:detik (disimpan bertipe {@code TIME}, bukan timestamp).
	 * Diisi {@code ProsesUjianHelper.hitungWaktu(...)} saat ujian benar-benar diakhiri.
	 */
	private Date lamaPengerjaan;

	/**
	 * Sisa waktu yang belum terpakai, juga bertipe {@code TIME}. Selama ujian berlangsung nilai
	 * "hidup"-nya berasal dari cache berkas per-entity, bukan dari kolom database — lihat
	 * {@link #getSisaWaktuPengerjaan()}.
	 */
	private Date sisaWaktuPengerjaan;

	/** Waktu peserta mengakhiri ujian (timestamp penuh). */
	private Date selesaiPada;

	/**
	 * Waktu peserta memulai ujian (timestamp penuh). Hanya bisa diisi sekali lewat setter —
	 * lihat {@link #setMulaiPada(Date)}.
	 */
	private Date mulaiPada;

	/** Penanda peserta sudah pernah masuk ke sesi ujian ini. */
	private Boolean telahIkutUjian = false;

	/**
	 * Penanda peserta diizinkan/diminta melengkapi jawaban (masuk kembali ke ujian yang sama
	 * untuk menuntaskan soal yang belum terjawab).
	 */
	private Boolean lengkapiJawaban;

	/**
	 * Jumlah soal yang diujikan. Kolom ini praktis hanya bayangan: {@link #getJumlahSoal()}
	 * selalu menimpanya dengan {@code pertemuanPunyaUjian.getJmlDitampilkan()}.
	 */
	private Double jumlahSoal = 0.0;

	/** Total skor yang berhasil diraih peserta; diisi {@code ProsesUjianHelper.hitungPilihanGanda}. */
	private Double jawabanBenar = 0.0;

	/** Total skor maksimal yang mungkin diraih; diisi {@code ProsesUjianHelper.hitungPilihanGanda}. */
	private Double jawabanBenarMax = 0.0;

	/** Nilai akhir dalam <b>persen</b> (0..100); dihitung ulang oleh {@link #getNilai()}. */
	private Double nilai = 0.0;

	/** Status lulus; dihitung ulang oleh {@link #getLulus()} terhadap {@code ujian.getNilaiLulus()}. */
	private Boolean lulus;

	/**
	 * Berapa kali peserta sudah masuk/mengikuti sesi ujian ini. Dibandingkan dengan
	 * {@code pertemuanPunyaUjian.getJumlahBolehIkut()} oleh {@code PertemuanPunyaUjianHelper}
	 * untuk memutuskan tombol "Ikut Ujian" masih boleh ditekan atau tidak.
	 */
	private Integer jumlahIkut;

	// Rekap pengawasan ujian (anti-curang): jumlah pelanggaran + log kejadian.
	/**
	 * Cacah pelanggaran pengawasan (pindah tab, keluar layar penuh, dsb) yang terdeteksi selama
	 * ujian. Dinaikkan oleh {@code ProsesUjianHelper} setiap kejadian.
	 */
	private Integer jumlahPelanggaran = 0;

	/**
	 * Log kejadian pelanggaran dalam bentuk teks bebas (kolom {@code text}), ditampilkan apa
	 * adanya oleh {@code RekapPengawasanUjianHelper}.
	 */
	private String logPelanggaran;

	/**
	 * Rincian nilai OBE (Outcome-Based Education) dalam bentuk string JSON — pemetaan capaian
	 * pembelajaran ke skor. Default bila kosong adalah objek JSON kosong ({@code Tugas.JSON}).
	 */
	private String nilaiObe;

	/**
	 * Kunci unik gabungan {@code <pertemuanPunyaUjianId>_<jenisPeserta>_<pesertaId>}, dipakai
	 * sebagai kunci cache cepat dan sebagai constraint unik di database. Dibangkitkan
	 * {@link #genKey(PertemuanPunyaUjian, Mahasiswa, BiodataCalonMahasiswa, Siswa, CalonSiswa)}.
	 */
	private String keyhasil;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA untuk instansiasi lewat refleksi.
	 */
	public HasilUjianMahasiswa() {
	}

	/**
	 * Konstruktor pintas yang hanya mengisi primary key — dipakai untuk membentuk referensi
	 * ringan ke sebuah baris tanpa memuat isinya.
	 *
	 * @param id primary key {@code hasil_ujian_mahasiswa.id}
	 */
	public HasilUjianMahasiswa(Long id) {
		this.id = id;
	}

	// public HasilUjianMahasiswa(Date mulaiPada) {
	// this.mulaiPada = mulaiPada;
	// }

	/**
	 * Primary key baris hasil ujian ini.
	 *
	 * <p>Kolomnya {@code insertable = false} karena nilainya dibangkitkan database
	 * ({@code IDENTITY}). Id ini juga menjadi kunci cache statik
	 * {@link #ambilDataAsli(MyArrayList, boolean)} dan kunci cache berkas
	 * {@link GeneralValueObject#retreive()}.</p>
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
	 * Menyetel primary key. Umumnya hanya dipakai Hibernate.
	 *
	 * @param id primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Catatan bebas atas hasil ujian ini (mis. alasan diskualifikasi atau nilai manual).
	 *
	 * @return keterangan yang sudah di-{@code trim()}; {@code ""} bila kosong — tidak pernah
	 *         {@code null}, sehingga pemanggil seperti {@code PertemuanPunyaUjianHelper} aman
	 *         memanggil {@code isEmpty()} langsung
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan == null ? "" : keterangan.trim();
	}

	/**
	 * Menyetel catatan bebas atas hasil ujian ini.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Cacah pelanggaran pengawasan ujian yang terdeteksi (pindah tab, keluar mode layar penuh,
	 * dan sejenisnya) — dinaikkan satu per kejadian oleh {@code ProsesUjianHelper}.
	 *
	 * @return jumlah pelanggaran; {@code 0} bila belum pernah tercatat (tidak pernah {@code null})
	 * @see ais.action.master.helper.RekapPengawasanUjianHelper
	 */
	@Column(name = "jumlah_pelanggaran", nullable = true)
	public Integer getJumlahPelanggaran() {
		return this.jumlahPelanggaran == null ? 0 : this.jumlahPelanggaran;
	}

	/**
	 * Menyetel cacah pelanggaran pengawasan.
	 *
	 * @param jumlahPelanggaran cacah baru; {@code null} dipakai oleh fitur "reset pengawasan"
	 *                          untuk mengosongkan rekap
	 */
	public void setJumlahPelanggaran(Integer jumlahPelanggaran) {
		this.jumlahPelanggaran = jumlahPelanggaran;
	}

	/**
	 * Log kejadian pelanggaran pengawasan dalam bentuk teks bebas (satu baris per kejadian,
	 * disusun oleh {@code ProsesUjianHelper}).
	 *
	 * @return isi log apa adanya; bisa {@code null} bila belum ada pelanggaran
	 */
	@Column(name = "log_pelanggaran", nullable = true, columnDefinition = "text")
	public String getLogPelanggaran() {
		return this.logPelanggaran;
	}

	/**
	 * Menyetel log kejadian pelanggaran pengawasan.
	 *
	 * @param logPelanggaran isi log; {@code null} untuk mengosongkan rekap
	 */
	public void setLogPelanggaran(String logPelanggaran) {
		this.logPelanggaran = logPelanggaran;
	}

	/**
	 * Peserta ujian bila ia seorang mahasiswa aktif.
	 *
	 * <p><b>Pola berulang di entity ini:</b> getter relasi memanggil
	 * {@link GeneralValueObject#check(Object)} lalu <b>menugaskan hasilnya kembali ke field</b>.
	 * {@code check} me-resolusi proxy lazy yang mungkin sudah lepas dari session (bisa membuka
	 * session sendiri untuk memuat ulang) dan mengembalikan instance kanonik dari
	 * {@code EntityIdentityMap}. Penugasan balik itulah yang membuat instance kanonik "menempel"
	 * pada baris hasil ujian ini — tanpa itu, setiap pemanggilan akan mengulang resolusi. Efek
	 * sampingnya: getter ini <b>tidak murni</b> dan bisa memicu query database.</p>
	 *
	 * @return mahasiswa peserta, atau {@code null} bila peserta bukan mahasiswa
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * Menyetel peserta mahasiswa.
	 *
	 * @param mahasiswa mahasiswa peserta; {@code null} bila peserta berjenis lain
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Peserta ujian bila ia seorang pendaftar PMB (ujian saringan masuk).
	 *
	 * <p>Sama seperti {@link #getMahasiswa()}: me-resolusi proxy lazy lewat
	 * {@link GeneralValueObject#check(Object)} dan menulis balik hasilnya ke field.</p>
	 *
	 * @return biodata calon mahasiswa, atau {@code null} bila peserta bukan pendaftar PMB
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "biodata_calon_mahasiswa", nullable = true)
	public BiodataCalonMahasiswa getBiodataCalonMahasiswa() {
		biodataCalonMahasiswa = check(biodataCalonMahasiswa);
		return biodataCalonMahasiswa;
	}

	/**
	 * Menyetel peserta pendaftar PMB.
	 *
	 * @param biodataCalonMahasiswa biodata pendaftar; {@code null} bila peserta berjenis lain
	 */
	public void setBiodataCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	/**
	 * Sesi ujian yang diikuti peserta — relasi <b>wajib</b> ({@code nullable = false}) dan sumber
	 * hampir semua parameter perhitungan di kelas ini: jenis soal ({@code getUjian().getJenis()}),
	 * nilai lulus, durasi ({@code getLama()}), mode waktu-per-soal ({@code getTiapSoal()}),
	 * jumlah soal ditampilkan ({@code getJmlDitampilkan()}), dan jendela waktu ujian.
	 *
	 * <p>Me-resolusi proxy lazy lewat {@link GeneralValueObject#check(Object)} dan menulis balik
	 * hasilnya ke field, sama seperti getter relasi lain di kelas ini.</p>
	 *
	 * @return sesi ujian; secara teori tidak pernah {@code null} untuk baris tersimpan, tetapi
	 *         kode di kelas ini tetap memeriksa {@code null} karena baris transient belum terisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pertemuan_punya_ujian", nullable = false)
	public PertemuanPunyaUjian getPertemuanPunyaUjian() {
		pertemuanPunyaUjian = check(pertemuanPunyaUjian);
		return pertemuanPunyaUjian;
	}

	/**
	 * Menyetel sesi ujian yang diikuti peserta.
	 *
	 * @param pertemuanPunyaUjian sesi ujian
	 */
	public void setPertemuanPunyaUjian(PertemuanPunyaUjian pertemuanPunyaUjian) {
		this.pertemuanPunyaUjian = pertemuanPunyaUjian;
	}

	/**
	 * Skor final yang ditetapkan dari luar entity ini.
	 *
	 * <p><b>Jangan dikacaukan dengan {@link #getNilai()}.</b> {@code getNilai()} adalah persentase
	 * 0..100 hasil koreksi otomatis pilihan ganda yang dihitung ulang setiap dibaca;
	 * {@code totalNilai} adalah kolom pasif yang hanya berubah bila ada kode luar memanggil
	 * {@link #setTotalNilai(Double)} (mis. koreksi manual esai atau agregasi OBE). Keduanya tidak
	 * disinkronkan otomatis.</p>
	 *
	 * @return skor final tersimpan; bisa {@code null} untuk baris lama
	 */
	@Column(name = "total_nilai", nullable = true)
	public Double getTotalNilai() {
		return totalNilai;
	}

	/**
	 * Menyetel skor final hasil penilaian dari luar.
	 *
	 * @param totalNilai skor final
	 */
	public void setTotalNilai(Double totalNilai) {
		this.totalNilai = totalNilai;
	}

	@Temporal(TemporalType.TIME)
	@Column(name = "lama_pengerjaan", nullable = true)
	public Date getLamaPengerjaan() {
		// FIX (Rabu 29-07-2026, laporan "waktu selesai" ujian keliru): bila NILAI ASLI sudah pernah
		// dicatat saat ujian benar-benar diakhiri (lihat ProsesUjianHelper.hitungWaktu -> setLamaPengerjaan),
		// JANGAN dihitung ulang di sini. Penghitungan ulang di bawah bergantung pada
		// getSisaWaktuPengerjaan() yang nilainya berasal dari cache file "live" (retreive()) dan bisa
		// basi/salah setelah ujian selesai -> hasil hitung ulang jadi ngawur (mis. seolah waktu habis
		// penuh dipakai). Nilai asli yang sudah tercatat harus diutamakan.
		if (lamaPengerjaan != null) {
			return lamaPengerjaan;
		}
		try {
			pertemuanPunyaUjian = getPertemuanPunyaUjian();
			if (pertemuanPunyaUjian != null && !pertemuanPunyaUjian.getTiapSoal()
					&& pertemuanPunyaUjian.getLama() != null) {
				getSisaWaktuPengerjaan();
				if (sisaWaktuPengerjaan != null) {
					long durationInMillis = pertemuanPunyaUjian.getLama().getTime() - sisaWaktuPengerjaan.getTime();

					long second = (durationInMillis / 1000) % 60;
					long minute = (durationInMillis / (1000 * 60)) % 60;
					long hour = (durationInMillis / (1000 * 60 * 60)) % 24;

					// System.out.println("lama pengerjaan durationInMillis => "
					// + durationInMillis + " second " + second
					// + " minute " + minute + " hour " + hour + " ");

					lamaPengerjaan = new GregorianCalendar(0, 0, 0, (int) hour, (int) minute, (int) second).getTime();

				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/HasilUjianMahasiswa.java:251");
		}
		return lamaPengerjaan;
	}

	public void setLamaPengerjaan(Date lamaPengerjaan) {
		this.lamaPengerjaan = lamaPengerjaan;
	}

	@Temporal(TemporalType.TIME)
	@Column(name = "sisa_waktu_pengerjaan", nullable = true)
	public Date getSisaWaktuPengerjaan() {
		try {
			pertemuanPunyaUjian = getPertemuanPunyaUjian();
			if (pertemuanPunyaUjian != null && !pertemuanPunyaUjian.getTiapSoal()) {
				String yglalu = retreive();
				if (yglalu != null && !yglalu.trim().isEmpty()) {
					Date timeLalu = Common.databaseDateFormat1.get().parse(yglalu);
					sisaWaktuPengerjaan = timeLalu;
				}
			}

			if (sisaWaktuPengerjaan != null) {
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(sisaWaktuPengerjaan);
				int hour = calendar.get(Calendar.HOUR_OF_DAY);
				if (hour > 22) {
					calendar.set(Calendar.HOUR_OF_DAY, 0);
					calendar.set(Calendar.MINUTE, 0);
					calendar.set(Calendar.SECOND, 1);
					sisaWaktuPengerjaan = calendar.getTime();
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/HasilUjianMahasiswa.java:284");
			// e.printStackTrace();
		}

		return sisaWaktuPengerjaan;
	}

	public void setSisaWaktuPengerjaan(Date sisaWaktuPengerjaan) {
		this.sisaWaktuPengerjaan = sisaWaktuPengerjaan;
	}

	public Boolean getTelahIkutUjian() {
		return telahIkutUjian == null ? false : telahIkutUjian;
	}

	public void setTelahIkutUjian(Boolean telahIkutUjian) {
		this.telahIkutUjian = telahIkutUjian;
	}

	public Double getJumlahSoal() {
		if (jumlahSoal == null) {
			jumlahSoal = 0.0;
		}
		pertemuanPunyaUjian = getPertemuanPunyaUjian();
		if (pertemuanPunyaUjian != null) {
			jumlahSoal = pertemuanPunyaUjian.getJmlDitampilkan().doubleValue();
		}

		return jumlahSoal;
	}

	public void setJumlahSoal(Double jumlahSoal) {
		this.jumlahSoal = jumlahSoal;
	}

	public Double getJawabanBenar() {
		if (jawabanBenar == null) {
			jawabanBenar = 0.0;
		}
		return jawabanBenar;
	}

	public void setJawabanBenar(Double jawabanBenar) {
		this.jawabanBenar = jawabanBenar;
	}

	public Double getNilai() {
		try {
			pertemuanPunyaUjian = getPertemuanPunyaUjian();
			Ujian ujian = pertemuanPunyaUjian == null ? null : pertemuanPunyaUjian.getUjian();
			check(ujian);
			if (ujian != null && ujian.getJenis().equals(BankSoal.PILIHAN_GANDA) && getJawabanBenarMax() != null
					&& getJawabanBenarMax() > 0.1) {
				nilai = getJawabanBenar() * 100.0 / getJawabanBenarMax();
			} else if (ujian != null && ujian.getJenis().equals(BankSoal.PILIHAN_GANDA) && getJumlahSoal() > 0.1) {
				nilai = getJawabanBenar() * 100.0 / (getJumlahSoal() * 1.0);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/HasilUjianMahasiswa.java:342");
		}

		if (nilai == null) {
			nilai = 0.0;
		}
		// Pengaman: nilai ujian (persentase) TIDAK PERNAH melebihi 100. Bila jawabanBenar >
		// jawabanBenarMax (skala jawabanBenarMax tak konsisten - lihat ProsesUjianHelper
		// .hitungPilihanGanda), rasio bisa >100 (mis. 122/50*100 = 244). Batasi ke 100 agar
		// tampilan/laporan tidak absurd; perbaikan akar + jalankan Hitung Ulang membetulkan data.
		if (nilai > 100.0) {
			nilai = 100.0;
		}
		return nilai;
	}

	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	public Boolean getLulus() {
		try {
			pertemuanPunyaUjian = getPertemuanPunyaUjian();
			Ujian ujian = pertemuanPunyaUjian == null ? null : pertemuanPunyaUjian.getUjian();
			check(ujian);
			if (ujian != null) {
				lulus = ujian.getNilaiLulus() <= getNilai();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/HasilUjianMahasiswa.java:371");
		}
		if (lulus == null) {
			lulus = false;
		}
		return lulus;
	}

	public void setLulus(Boolean lulus) {
		this.lulus = lulus;
	}

	public Double getJawabanBenarMax() {
		return jawabanBenarMax;
	}

	public void setJawabanBenarMax(Double jawabanBenarMax) {
		this.jawabanBenarMax = jawabanBenarMax;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getSelesaiPada() {
		// FIX (laporan: "Selesai dikerjakan pada" menampilkan waktu di MASA DEPAN dibanding jam
		// sekarang): waktu selesai ASLI dicatat sekali saat peserta benar-benar mengakhiri ujian
		// (ProsesUjianHelper.hitungWaktu -> setSelesaiPada(endDate), berdasarkan timestamp jawaban
		// terakhir yang sungguhan). Kode di bawah dulu SELALU menghitung ULANG & MENIMPA nilai asli
		// itu dengan estimasi (mulaiPada + (lama - sisaWaktuPengerjaan)) -- dan sisaWaktuPengerjaan
		// sendiri berasal dari cache file "live" yang basi setelah ujian selesai (sering terbaca 0
		// = "waktu penuh terpakai"), sehingga estimasi ini bisa melompat ke mulaiPada + durasi PENUH
		// (mis. 24 jam) walau peserta submit jauh lebih awal -> tampak "selesai" di masa depan.
		// Nilai asli yang sudah tercatat WAJIB diutamakan; hitung-ulang di bawah hanya fallback bila
		// belum pernah tercatat sama sekali.
		if (selesaiPada != null) {
			return selesaiPada;
		}
		try {
			pertemuanPunyaUjian = getPertemuanPunyaUjian();
			if (pertemuanPunyaUjian != null && !pertemuanPunyaUjian.getTiapSoal() && getMulaiPada() != null) {
				getSisaWaktuPengerjaan();
				if (sisaWaktuPengerjaan != null) {
					long durationInMillis = pertemuanPunyaUjian.getLama().getTime() - sisaWaktuPengerjaan.getTime();
					long second = (durationInMillis / 1000) % 60;
					long minute = (durationInMillis / (1000 * 60)) % 60;
					long hour = (durationInMillis / (1000 * 60 * 60)) % 24;

					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.setTime(getMulaiPada());

					// FIX: Calendar.HOUR = jam 12-jam (0-11, tanpa info AM/PM saat di-set ulang) --
					// menambah & menyetel ulang dgn field ini bisa menggeser hasil 12 jam (AM/PM
					// tertukar). Pakai HOUR_OF_DAY (0-23) yang tidak ambigu.
					int ditambahJam = (int) (calendar.get(Calendar.HOUR_OF_DAY) + hour);
					int ditambahMenit = (int) (calendar.get(Calendar.MINUTE) + minute);
					int ditambahDetik = (int) (calendar.get(Calendar.SECOND) + second);

					calendar.set(Calendar.HOUR_OF_DAY, ditambahJam);
					calendar.set(Calendar.MINUTE, ditambahMenit);
					calendar.set(Calendar.SECOND, ditambahDetik);

					// System.out.println("selesaiPada durationInMillis => " +
					// durationInMillis + " second " + second
					// + " minute " + minute + " hour " + hour + " ditambahJam "
					// + ditambahJam + ", ditambahMenit "
					// + ditambahMenit + ", ditambahDetik " + ditambahDetik);

					selesaiPada = calendar.getTime();

				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/HasilUjianMahasiswa.java:425");
		}
		return selesaiPada;
	}

	public void setSelesaiPada(Date selesaiPada) {
		this.selesaiPada = selesaiPada;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getMulaiPada() {
		return mulaiPada;
	}

	public void setMulaiPada(Date mulaiPada) {
		if (this.mulaiPada == null) {
			this.mulaiPada = mulaiPada;
		}
	}

	public void reset() {
		mulaiPada = null;
		selesaiPada = null;
		jawabanBenarMax = null;
		nilai = null;
		jawabanBenar = null;
		telahIkutUjian = false;
		jumlahIkut = null;
		lamaPengerjaan = null;
		lengkapiJawaban = null;
		put("1", "index");
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = true)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "calon_siswa", nullable = true)
	public CalonSiswa getCalonSiswa() {
		calonSiswa = check(calonSiswa);
		return calonSiswa;
	}

	public void setCalonSiswa(CalonSiswa calonSiswa) {
		this.calonSiswa = calonSiswa;
	}

	public int ambilJumlahTerjawab(int jumlahDiujikan, MyArrayList<Long> ujianPunyaSoalsData) {
		Set<Long> terjawab = ambilBankSoalIdTerjawab(jumlahDiujikan, ujianPunyaSoalsData, false);
		int size = terjawab.size();
		terjawab = null;
		return size;
	}

	public Set<Long> ambilBankSoalIdTerjawab(int jumlahDiujikan, MyArrayList<Long> ujianPunyaSoalsData) {
		return ambilBankSoalIdTerjawab(jumlahDiujikan, ujianPunyaSoalsData, false);
	}

	public Set<Long> ambilBankSoalIdTerjawab(int jumlahDiujikan, MyArrayList<Long> ujianPunyaSoalsData,
			boolean refresh) {
		Map<Long, Set<Long>> hasilUjianMahasiswaDetailsa = ambilHasilUjianMahasiswaDetail(jumlahDiujikan,
				ujianPunyaSoalsData, refresh);
		Set<Long> ujianPunyaSoals = new HashSet<Long>();
		for (Set<Long> a : hasilUjianMahasiswaDetailsa.values()) {
			for (Long id : a) {
				HasilUjianMahasiswaDetail ujianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
						.ambilData(HasilUjianMahasiswaDetail.class, id.toString());
				if (ujianMahasiswaDetail != null) {
					if (ujianMahasiswaDetail.getBankSoalDetail() != null && ujianMahasiswaDetail.getBankSoal() != null
							&& ujianMahasiswaDetail.getBankSoalDetail().getId() != null) {
						ujianPunyaSoals.add(ujianMahasiswaDetail.getBankSoal().getId());
					} else if (!ujianMahasiswaDetail.getJawaban().trim().isEmpty()
							&& ujianMahasiswaDetail.getBankSoal() != null) {
						ujianPunyaSoals.add(ujianMahasiswaDetail.getBankSoal().getId());
					}
				}
			}
		}
		hasilUjianMahasiswaDetailsa = null;
		return ujianPunyaSoals;
	}

	public Set<Long> ambilBankSoalIdTerjawab(Map<Long, Set<Long>> hasilUjianMahasiswaDetailsa) {
		Set<Long> ujianPunyaSoals = new HashSet<Long>();
		for (Set<Long> s : hasilUjianMahasiswaDetailsa.values()) {
			for (Long id : s) {
				HasilUjianMahasiswaDetail ujianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
						.ambilData(HasilUjianMahasiswaDetail.class, id.toString());
				if (ujianMahasiswaDetail != null) {
					if (ujianMahasiswaDetail.getBankSoalDetail() != null && ujianMahasiswaDetail.getBankSoal() != null
							&& ujianMahasiswaDetail.getBankSoalDetail().getId() != null) {
						ujianPunyaSoals.add(ujianMahasiswaDetail.getBankSoal().getId());
					} else if (!ujianMahasiswaDetail.getJawaban().trim().isEmpty()
							&& ujianMahasiswaDetail.getBankSoal() != null) {
						ujianPunyaSoals.add(ujianMahasiswaDetail.getBankSoal().getId());
					}
				}
			}
		}

		return ujianPunyaSoals;
	}

	public Set<Long> ambilBankSoalIdTerjawabDinilai(Map<Long, Set<Long>> hasilUjianMahasiswaDetailsa) {
		Set<Long> ujianPunyaSoals = new HashSet<Long>();
		for (Set<Long> s : hasilUjianMahasiswaDetailsa.values()) {
			for (Long id : s) {
				HasilUjianMahasiswaDetail ujianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
						.ambilData(HasilUjianMahasiswaDetail.class, id.toString());
				if (ujianMahasiswaDetail != null) {
					if (ujianMahasiswaDetail.getNilai() > 0.01) {
						ujianPunyaSoals.add(ujianMahasiswaDetail.getBankSoal().getId());
					}
				}
			}
		}

		return ujianPunyaSoals;
	}

	public Set<Long> ambilBankSoalIdTerjawabBenar(Map<Long, Set<Long>> hasilUjianMahasiswaDetailsa) {
		Set<Long> ujianPunyaSoals = new HashSet<Long>();
		for (Set<Long> s : hasilUjianMahasiswaDetailsa.values()) {
			for (Long id : s) {
				HasilUjianMahasiswaDetail ujianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
						.ambilData(HasilUjianMahasiswaDetail.class, id.toString());
				if (ujianMahasiswaDetail != null) {
					if (ujianMahasiswaDetail.getBankSoalDetail() != null && ujianMahasiswaDetail.getBankSoal() != null
							&& ujianMahasiswaDetail.getBankSoalDetail().getId() != null
							&& ujianMahasiswaDetail.getBankSoalDetail().getBetul()) {
						ujianPunyaSoals.add(ujianMahasiswaDetail.getBankSoal().getId());
					}
				}
			}
		}

		return ujianPunyaSoals;
	}

	public MyArrayList<Long> ambilUjianPunyaSoals(int maxSize, Label label, boolean refresh) {
		List<Long> hasilUjianMahasiswaDetailsatemp = ambilDataAsli(null, refresh);
		if (hasilUjianMahasiswaDetailsatemp == null) {
			hasilUjianMahasiswaDetailsatemp = new ArrayList<Long>();
		}
		if (maxSize < 0) {
			maxSize = 0;
		}

		List<Long> hasilUjianMahasiswaDetailsa = new ArrayList<Long>();
		for (Long id : hasilUjianMahasiswaDetailsatemp) {
			if (id == null) {
				continue;
			}
			HasilUjianMahasiswaDetail ujianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
					.ambilData(HasilUjianMahasiswaDetail.class, id.toString(), true);
			if (ujianMahasiswaDetail != null && (ujianMahasiswaDetail.getBankSoalDetail() != null
					|| (ujianMahasiswaDetail.getJawaban() != null
							&& !ujianMahasiswaDetail.getJawaban().isEmpty()))) {
				hasilUjianMahasiswaDetailsa.add(id);
			}
		}

		for (Long id : hasilUjianMahasiswaDetailsatemp) {
			if (!hasilUjianMahasiswaDetailsa.contains(id)) {
				hasilUjianMahasiswaDetailsa.add(id);
			}
		}
		hasilUjianMahasiswaDetailsatemp = null;

		MyArrayList<Long> ujianPunyaSoals = new MyArrayList<Long>(maxSize);
		int size = hasilUjianMahasiswaDetailsa.size();
		int index = 0;

		for (Long id : hasilUjianMahasiswaDetailsa) {
			if (ujianPunyaSoals.size() < maxSize) {
				if (id == null) {
					continue;
				}
				HasilUjianMahasiswaDetail ujianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
						.ambilData(HasilUjianMahasiswaDetail.class, id.toString(), true);
				if (ujianMahasiswaDetail != null && (ujianMahasiswaDetail.getBankSoalDetail() != null
						|| (ujianMahasiswaDetail.getJawaban() != null
								&& !ujianMahasiswaDetail.getJawaban().isEmpty()))) {
					index++;
					if (ujianMahasiswaDetail.getUjianPunyaSoal() != null
							&& ujianMahasiswaDetail.getUjianPunyaSoal().getId() != null) {
						ujianPunyaSoals.add(ujianMahasiswaDetail.getUjianPunyaSoal().getId());
					}
					if (label != null) {
						label.setValue("harap tunggu.. Sedang memasukkan soal ("
								+ Common.numberFormat.get().format((index * 100.0) / size) + " %)");
					}
				}
			}
		}

		for (Long id : hasilUjianMahasiswaDetailsa) {
			if (ujianPunyaSoals.size() < maxSize) {
				if (id == null) {
					continue;
				}
				HasilUjianMahasiswaDetail ujianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
						.ambilData(HasilUjianMahasiswaDetail.class, id.toString(), true);
				if (ujianMahasiswaDetail != null) {
					index++;
					if (ujianMahasiswaDetail.getUjianPunyaSoal() != null
							&& ujianMahasiswaDetail.getUjianPunyaSoal().getId() != null
							&& !ujianPunyaSoals.contains(ujianMahasiswaDetail.getUjianPunyaSoal().getId())) {
						ujianPunyaSoals.add(ujianMahasiswaDetail.getUjianPunyaSoal().getId());
					}
					if (label != null) {
						label.setValue("harap tunggu.. Sedang memasukkan soal ("
								+ Common.numberFormat.get().format((index * 100.0) / size) + " %)");
					}
				}
			}
		}

		hasilUjianMahasiswaDetailsa = null;
		return ujianPunyaSoals;
	}

	public Map<Long, Set<Long>> ambilHasilUjianMahasiswaDetail(int jumlahDiujikan, MyArrayList<Long> ujianPunyaSoals) {
		return ambilHasilUjianMahasiswaDetail(jumlahDiujikan, ujianPunyaSoals, false);
	}

	public Map<Long, Set<Long>> ambilHasilUjianMahasiswaDetail(int jumlahDiujikan, MyArrayList<Long> ujianPunyaSoals,
			boolean refresh) {
		return ambilHasilUjianMahasiswaDetail(refresh, jumlahDiujikan, null, ujianPunyaSoals);
	}

	private static Map<Long, List<Long>> ygSudahDiambils = null;

	@SuppressWarnings("unchecked")
	private List<Long> ambilDataAsli(MyArrayList<Long> ujianPunyaSoals, boolean refresh) {

		if (ygSudahDiambils == null) {
			synchronized (HasilUjianMahasiswa.class) {
				if (ygSudahDiambils == null) {
					ygSudahDiambils = ais.common.MemoryCacheUtil.get("SoalYgSudahDiambils");
				}
			}
		}

		if (this.getId() != null) {
			// Cache statik dibaca/ditulis oleh banyak thread (loadData memakai
			// thread-pool 100). Akses peta HARUS tersinkron agar HashMap tak korup.
			List<Long> dataCache;
			synchronized (ygSudahDiambils) {
				dataCache = HasilUjianMahasiswa.ygSudahDiambils.get(this.getId());
			}
			if (dataCache == null || dataCache.isEmpty() || refresh) {
				// Metode ini kerap dipanggil dari proses ekspor yang sudah memiliki session
				// sendiri. currentNativeSession() lalu ditutup di sini akan menutup ResultSet
				// milik pemanggil. Gunakan session terdedikasi agar lifecycle tidak bertumpuk.
				Session session = HibernateUtil.getSessionFactory().openSession();

				try {
					List<Long> hasil = getId() == null ? new ArrayList<Long>()
							: session.createCriteria(HasilUjianMahasiswaDetail.class)
									.add(Restrictions.isNotNull("bankSoal"))
									.add(ujianPunyaSoals == null || ujianPunyaSoals.isEmpty()
											? Restrictions.sqlRestriction("true")
											: Restrictions.in("ujianPunyaSoal.id", ujianPunyaSoals))
									.add(Restrictions.eq("hasilUjianMahasiswa.id", getId())).addOrder(Order.asc("id"))
									.setProjection(Projections.property("id")).list();
					for (Long id : hasil) {
						HasilUjianMahasiswaDetail hasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
								.ambilData(HasilUjianMahasiswaDetail.class, id.toString());
						// PENTING (koherensi cache): cache MapDB (CLASS_IZINKAN) menyimpan SALINAN
						// ter-serialisasi. Saat refresh=true, WAJIB muat ulang dari DB lalu
						// segarkan cache — kalau tidak, jawaban yang baru disimpan (bankSoalDetail)
						// tak terbaca shg skor 0 walau benar. Cache-miss (null) juga tetap dimuat.
						if (refresh || hasilUjianMahasiswaDetail == null) {
							hasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) session
									.createCriteria(HasilUjianMahasiswaDetail.class).add(Restrictions.idEq(id))
									.uniqueResult();
							if (hasilUjianMahasiswaDetail != null) {
								GeneralValueObject.masukkanData(HasilUjianMahasiswaDetail.class, hasilUjianMahasiswaDetail);
							}
						}
					}

					synchronized (ygSudahDiambils) {
						HasilUjianMahasiswa.ygSudahDiambils.put(this.getId(), hasil);
					}
					dataCache = hasil;
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				} finally {
					try { if (session != null && session.isOpen()) session.clear(); } catch (Exception e) { }
					try { if (session != null && session.isOpen()) session.disconnect(); } catch (Exception e) { }
					try { if (session != null && session.isOpen()) session.close(); } catch (Exception e) { }
				}
			}

			// Kembalikan SALINAN: cegah pemanggil memutasi (mis. .clear()) list yang
			// tersimpan di cache statik, sekaligus hindari ConcurrentModification
			// saat list yang sama di-iterasi banyak thread.
			synchronized (ygSudahDiambils) {
				List<Long> terbaru = HasilUjianMahasiswa.ygSudahDiambils.get(this.getId());
				if (terbaru != null) {
					dataCache = terbaru;
				}
				return dataCache == null ? new ArrayList<Long>() : new ArrayList<Long>(dataCache);
			}
		} else {
			return new ArrayList<Long>();
		}
	}

	public MyHashMap<Long, Set<Long>> ambilHasilUjianMahasiswaDetail(boolean refresh, int jumlahDiujikan, Label label,
			MyArrayList<Long> ujianPunyaSoals) {

		Set<Long> soals = new HashSet<Long>();
		if (ujianPunyaSoals != null) {
			for (Long ujianPunyaSoalId : ujianPunyaSoals) {
				UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class,
						ujianPunyaSoalId.toString());
				if (ujianPunyaSoal != null) {
					soals.add(ujianPunyaSoal.getBankSoal().getId());
				}
			}
		}

		List<Long> hasilUjianMahasiswaDetailsa = ambilDataAsli(ujianPunyaSoals, refresh);

		pertemuanPunyaUjian = getPertemuanPunyaUjian();

		Map<String, Long> detailSoalUjian = new HashMap<String, Long>();
		synchronized (hasilUjianMahasiswaDetailsa) {
			for (Long hasilUjianMahasiswaDetailid : hasilUjianMahasiswaDetailsa) {
				try {

					HasilUjianMahasiswaDetail hasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
							.ambilData(HasilUjianMahasiswaDetail.class, hasilUjianMahasiswaDetailid.toString());
					if (hasilUjianMahasiswaDetail != null) {
						BankSoal bankSoal = hasilUjianMahasiswaDetail.getBankSoal();
						String key = bankSoal.getId() + "_"
								+ (hasilUjianMahasiswaDetail.getBankSoalDetail() == null ? ""
										: hasilUjianMahasiswaDetail.getBankSoalDetail().getId());
						Ujian ujian = pertemuanPunyaUjian != null ? pertemuanPunyaUjian.getUjian() : null;
						if (ujian != null && (ujian.getJenis().equals(BankSoal.ESAY)
								|| hasilUjianMahasiswaDetail.getBankSoal().getJenisPilihanGanda()
										.equals(BankSoal.MULTIPLE_COICE)
								|| hasilUjianMahasiswaDetail.getBankSoal().getJenisPilihanGanda()
										.equals(BankSoal.BENAR_SALAH))) {
							key = bankSoal.getId().toString();
						}

						boolean masuk = soals.isEmpty() || soals.contains(bankSoal.getId());

						// System.out.println("masuk -> " + masuk + ", Soal -> " +
						// bankSoal.getSoal() + " key -> " + key);
						if (masuk) {
							Long ygSudahAdaId = detailSoalUjian.get(key);
							HasilUjianMahasiswaDetail ygSudahAda = ygSudahAdaId == null ? null
									: (HasilUjianMahasiswaDetail) GeneralValueObject
											.ambilData(HasilUjianMahasiswaDetail.class, ygSudahAdaId.toString());
							if (ujian != null && ujian.getJenis().equals(BankSoal.ESAY)) {

								if (ygSudahAda != null && (ygSudahAda.getWaktuJawab() != null
										&& hasilUjianMahasiswaDetail.getWaktuJawab() == null)) {
									// System.out.println(
									// "data tidak dimasukkan essay karena jawaban
									// tidak
									// ada
									// -> soal " + bankSoal.getSoal()
									// + ", waktu jawab lama " +
									// Common.dateFormat3.get().format(ygSudahAda.getWaktuJawab())
									// + ", jawaban yang dimasukkan " + ygSudahAda);
								} else if (ygSudahAda != null && (!ygSudahAda.getJawaban().isEmpty()
										&& hasilUjianMahasiswaDetail.getJawaban().isEmpty())) {
									// System.out.println("data tidak dimasukkan
									// essay
									// karena jawaban tidak ada -> soal "
									// + bankSoal.getSoal() + ", jawaban lama " +
									// ygSudahAda.getJawaban()
									// + ", jawaban yang dimasukkan " + ygSudahAda);
								} else {
									if (ygSudahAda != null && ygSudahAda.getWaktuJawab() != null
											&& hasilUjianMahasiswaDetail.getWaktuJawab() != null && ygSudahAda
													.getWaktuJawab().after(hasilUjianMahasiswaDetail.getWaktuJawab())) {
										// System.out.println(
										// "data tidak dimasukkan essay karena
										// tanggal /
										// waktu men-jawab lebih lama -> soal "
										// + bankSoal.getSoal() + ", waktu jawab
										// lama "
										// +
										// Common.dateFormat3.get().format(ygSudahAda.getWaktuJawab())
										// + ", waktu jawab baru "
										// +
										// Common.dateFormat3.get().format(hasilUjianMahasiswaDetail.getWaktuJawab())
										// + ", jawaban yang dimasukkan " +
										// ygSudahAda);
									} else if (ygSudahAda != null && ygSudahAda.getTanggal_dirubah()
											.after(hasilUjianMahasiswaDetail.getTanggal_dirubah())) {
										// System.out.println(
										// "data tidak dimasukkan essay karena
										// tanggal /
										// waktu bikin lebih lama -> soal "
										// + bankSoal.getSoal() + ", waktu jawab
										// lama "
										// +
										// Common.dateFormat3.get().format(ygSudahAda.getTanggal_dirubah())
										// + ", waktu jawab baru "
										// +
										// Common.dateFormat3.get().format(hasilUjianMahasiswaDetail.getTanggal_dirubah())
										// + ", jawaban yang dimasukkan " +
										// ygSudahAda);
									} else {
										// System.out.println("ditambahkan key -> "
										// +
										// key +
										// ", jawaban "
										// +
										// hasilUjianMahasiswaDetail.getJawaban());

										detailSoalUjian.put(key, hasilUjianMahasiswaDetail.getId());
									}
								}
							} else {
								if (ygSudahAda != null && ygSudahAda.getBankSoalDetail() != null
										&& hasilUjianMahasiswaDetail.getBankSoalDetail() == null) {
									// System.out.println(
									// "data tidak dimasukkan pilihan ganda karena
									// jawaban
									// tidak ada -> hasilUjianMahasiswaDetail "
									// + hasilUjianMahasiswaDetail + ", jawaban yang
									// dimasukkan " + ygSudahAda);
								} else {

									if (ygSudahAda != null && ygSudahAda.getTanggal_dirubah()
											.after(hasilUjianMahasiswaDetail.getTanggal_dirubah())) {
										// System.out.println(
										// "data tidak dimasukkan pilihan ganda
										// karena
										// tanggal / waktu lebih lama ->
										// hasilUjianMahasiswaDetail "
										// + hasilUjianMahasiswaDetail + ", jawaban
										// yang
										// dimasukkan " + ygSudahAda);
									} else {
										// System.out.println("ditambahkan key -> "
										// +
										// key);
										detailSoalUjian.put(key, hasilUjianMahasiswaDetail.getId());
									}
								}
							}
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		}

		// System.out.println("detailSoalUjian -> " + detailSoalUjian.keySet());

		MyHashMap<Long, Set<Long>> hasilUjianMahasiswaDetailsHasil = new MyHashMap<Long, Set<Long>>(jumlahDiujikan);
		for (Long hasilUjianMahasiswaDetaild : detailSoalUjian.values()) {
			try {

				HasilUjianMahasiswaDetail hasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
						.ambilData(HasilUjianMahasiswaDetail.class, hasilUjianMahasiswaDetaild.toString());
				if (hasilUjianMahasiswaDetail != null) {
					Ujian ujian = pertemuanPunyaUjian != null ? pertemuanPunyaUjian.getUjian() : null;
					if (ujian != null && (ujian.getJenis().equals(BankSoal.ESAY)
							|| hasilUjianMahasiswaDetail.getBankSoal().getJenisPilihanGanda()
									.equals(BankSoal.MULTIPLE_COICE)
							|| hasilUjianMahasiswaDetail.getBankSoal().getJenisPilihanGanda()
									.equals(BankSoal.BENAR_SALAH))) {
						Set<Long> hasilUjianMahasiswaDetails = new HashSet<Long>();
						hasilUjianMahasiswaDetails.add(hasilUjianMahasiswaDetail.getId());
						hasilUjianMahasiswaDetailsHasil.put(hasilUjianMahasiswaDetail.getBankSoal().getId(),
								hasilUjianMahasiswaDetails);
					} else {
						if (hasilUjianMahasiswaDetailsHasil
								.containsKey(hasilUjianMahasiswaDetail.getBankSoal().getId())) {
							hasilUjianMahasiswaDetailsHasil.get(hasilUjianMahasiswaDetail.getBankSoal().getId())
									.add(hasilUjianMahasiswaDetail.getId());
						} else {
							Set<Long> hasilUjianMahasiswaDetails = new HashSet<Long>();
							hasilUjianMahasiswaDetails.add(hasilUjianMahasiswaDetail.getId());
							hasilUjianMahasiswaDetailsHasil.put(hasilUjianMahasiswaDetail.getBankSoal().getId(),
									hasilUjianMahasiswaDetails);
						}
					}
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		detailSoalUjian.clear();
		detailSoalUjian = null;
		// JANGAN .clear() di sini: dulu list ini = referensi cache statik sehingga
		// meng-kosongkannya merusak cache peserta. Cukup lepas referensi lokal.
		hasilUjianMahasiswaDetailsa = null;
		return hasilUjianMahasiswaDetailsHasil;

	}

	public Set<Long> ambilHasilUjianMahasiswaDetail(Map<Long, Set<Long>> hasilUjianMahasiswaDetailsa,
			BankSoal bankSoal) {
		Set<Long> hasilUjianMahasiswaDetail = hasilUjianMahasiswaDetailsa.get(bankSoal.getId());
		if (hasilUjianMahasiswaDetail == null) {
			hasilUjianMahasiswaDetail = new HashSet<Long>();
		}
		hasilUjianMahasiswaDetailsa = null;
		return hasilUjianMahasiswaDetail;
	}

	public Set<Long> ambilHasilUjianMahasiswaDetail(int jumlahDiujikan, MyArrayList<Long> ujianPunyaSoalsData,
			BankSoal bankSoal) {
		Map<Long, Set<Long>> hasilUjianMahasiswaDetailsa = ambilHasilUjianMahasiswaDetail(jumlahDiujikan,
				ujianPunyaSoalsData);
		return ambilHasilUjianMahasiswaDetail(hasilUjianMahasiswaDetailsa, bankSoal);
	}

	public List<HasilUjianMahasiswaDetail> ambilHasilUjianMahasiswaDetail(Session session,
			Collection<HasilUjianMahasiswaDetail> hasilUjianMahasiswaDetailsa, BankSoalDetail bankSoalDetail) {
		List<HasilUjianMahasiswaDetail> hasilUjianMahasiswaDetail = new ArrayList<HasilUjianMahasiswaDetail>();
		for (HasilUjianMahasiswaDetail ujianMahasiswaDetail : hasilUjianMahasiswaDetailsa) {
			if (bankSoalDetail != null && bankSoalDetail.getId() != null
					&& ujianMahasiswaDetail.getBankSoalDetail() != null
					&& bankSoalDetail.getId().equals(ujianMahasiswaDetail.getBankSoalDetail().getId())) {
				hasilUjianMahasiswaDetail.add(ujianMahasiswaDetail);
			}
		}

		hasilUjianMahasiswaDetailsa = null;
		return hasilUjianMahasiswaDetail;
	}

	public Integer getJumlahIkut() {
		return jumlahIkut == null ? (getLamaPengerjaan() != null ? 1 : 0) : jumlahIkut;
	}

	public void setJumlahIkut(Integer jumlahIkut) {
		this.jumlahIkut = jumlahIkut;
	}

	public static void tampilkanUjianKembali(String strHasilUjianMahasiswa) {
		try {
			if (strHasilUjianMahasiswa != null && !strHasilUjianMahasiswa.trim().isEmpty()) {
				Long id = Long.parseLong(strHasilUjianMahasiswa);
				if (id != null) {
					final HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) HibernateUtil.currentSession()
							.createCriteria(HasilUjianMahasiswa.class).add(Restrictions.idEq(id)).uniqueResult();
					System.out.println("id -> " + id + ", strHasilUjianMahasiswa " + strHasilUjianMahasiswa
							+ ", hasilUjianMahasiswa => " + hasilUjianMahasiswa);
					if (hasilUjianMahasiswa != null) {
						if (hasilUjianMahasiswa.getPertemuanPunyaUjian().getOtomatisMunculKetikaBelumSelesai()
								&& hasilUjianMahasiswa.getPertemuanPunyaUjian() != null
								&& (hasilUjianMahasiswa.getPertemuanPunyaUjian().getSampaiUjian() == null
										|| hasilUjianMahasiswa.getPertemuanPunyaUjian().getSampaiUjian()
												.after(ais.ui.util.WaktuUtil.getDate()))

								&& (hasilUjianMahasiswa.getPertemuanPunyaUjian().getMulaiUjian() == null
										|| hasilUjianMahasiswa.getPertemuanPunyaUjian().getMulaiUjian()
												.before(ais.ui.util.WaktuUtil.getDate()))) {

							ProsesUjianHelper.tampil(hasilUjianMahasiswa.getMahasiswa(),
									hasilUjianMahasiswa.getBiodataCalonMahasiswa(), hasilUjianMahasiswa.getSiswa(),
									hasilUjianMahasiswa.getCalonSiswa(), hasilUjianMahasiswa.getPertemuanPunyaUjian(),
									false, new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											MyWindow window = new MyWindow("Hasil Ujian", "none", true);
											ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
													.appendChild(window);

											window.setWidth("99%");
											window.setHeight("99%");

											if (hasilUjianMahasiswa.getBiodataCalonMahasiswa() != null) {
												TampilanUjianCalonMahasiswa tampilanUjianCalonMahasiswa = new TampilanUjianCalonMahasiswa(
														false);
												tampilanUjianCalonMahasiswa
														.init(hasilUjianMahasiswa.getBiodataCalonMahasiswa());
												tampilanUjianCalonMahasiswa.setParent(window);
												tampilanUjianCalonMahasiswa.setHeight("100%");
												tampilanUjianCalonMahasiswa.setWidth("100%");
											} else {
												new PertemuanPunyaUjianHelper(hasilUjianMahasiswa.getMahasiswa(),
														hasilUjianMahasiswa.getBiodataCalonMahasiswa())
														.display(hasilUjianMahasiswa.getPertemuanPunyaUjian()
																.getPertemuan(), window);
											}

											window.onModal();
										}
									}, false);

						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/HasilUjianMahasiswa.java:1011");
		}
	}

	public Boolean getLengkapiJawaban() {
		return lengkapiJawaban == null ? false : lengkapiJawaban;
	}

	public void setLengkapiJawaban(Boolean lengkapiJawaban) {
		this.lengkapiJawaban = lengkapiJawaban;
	}

	public static String genKey(PertemuanPunyaUjian pertemuanPunyaUjian, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, Siswa siswa, CalonSiswa calonSiswa) {
		if (mahasiswa == null && biodataCalonMahasiswa == null && siswa == null && calonSiswa == null) {
			return null;
		}
		if (pertemuanPunyaUjian == null || pertemuanPunyaUjian.getId() == null) {
			return null;
		}
		String keyhasil = pertemuanPunyaUjian.getId() + "_";
		if (mahasiswa != null) {
			keyhasil += "mhs_" + mahasiswa.getId();
		} else if (biodataCalonMahasiswa != null) {
			keyhasil += "cal_mhs_" + biodataCalonMahasiswa.getId();
		} else if (siswa != null) {
			keyhasil += "siswa_" + siswa.getId();
		} else if (calonSiswa != null) {
			keyhasil += "cal_siswa_" + calonSiswa.getId();
		}
		return keyhasil;
	}

	public static HasilUjianMahasiswa ambilByKey(PertemuanPunyaUjian pertemuanPunyaUjian, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, Siswa siswa, CalonSiswa calonSiswa) {
		String keyhasil = genKey(pertemuanPunyaUjian, mahasiswa, biodataCalonMahasiswa, siswa, calonSiswa);
		if (keyhasil == null) {
			return null;
		}
		HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) GeneralValueObject
				.ambilDataLangsung(HasilUjianMahasiswa.class, keyhasil);
		if (hasilUjianMahasiswa != null) {
			return hasilUjianMahasiswa;
		}

		Session session = HibernateUtil.currentNativeSession();
		try {

			hasilUjianMahasiswa = (HasilUjianMahasiswa) session.createCriteria(HasilUjianMahasiswa.class)
					.add(Restrictions.eq("keyhasil", keyhasil)).uniqueResult();
			if (hasilUjianMahasiswa == null) {

				Criteria criteria = session.createCriteria(HasilUjianMahasiswaDetail.class)
						.add(Restrictions.or(Restrictions.isNotNull("bankSoalDetail"), Restrictions.ne("jawaban", "")))
						.add(Restrictions.isNotNull("hasilUjianMahasiswa"))
						.setProjection(Projections.property("hasilUjianMahasiswa"))
						.createAlias("hasilUjianMahasiswa", "hasilUjianMahasiswa")
						.add(Restrictions.eq("hasilUjianMahasiswa.pertemuanPunyaUjian", pertemuanPunyaUjian))
						.addOrder(Order.asc("hasilUjianMahasiswa")).addOrder(Order.asc("id"));

				hasilUjianMahasiswa = (HasilUjianMahasiswa) criteria
						.add(mahasiswa != null ? Restrictions.eq("hasilUjianMahasiswa.mahasiswa", mahasiswa)
								: Restrictions.sqlRestriction("true"))
						.add(biodataCalonMahasiswa != null
								? Restrictions.eq("hasilUjianMahasiswa.biodataCalonMahasiswa", biodataCalonMahasiswa)
								: Restrictions.sqlRestriction("true"))
						.add(siswa != null ? Restrictions.eq("hasilUjianMahasiswa.siswa", siswa)
								: Restrictions.sqlRestriction("true"))
						.add(calonSiswa != null ? Restrictions.eq("hasilUjianMahasiswa.calonSiswa", calonSiswa)
								: Restrictions.sqlRestriction("true"))
						.setMaxResults(1).uniqueResult();
			}

			if (hasilUjianMahasiswa == null) {
				hasilUjianMahasiswa = new HasilUjianMahasiswa();
				hasilUjianMahasiswa.setMahasiswa(mahasiswa);
				hasilUjianMahasiswa.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
				hasilUjianMahasiswa.setCalonSiswa(calonSiswa);
				hasilUjianMahasiswa.setSiswa(siswa);
				hasilUjianMahasiswa.setPertemuanPunyaUjian(pertemuanPunyaUjian);
				hasilUjianMahasiswa.setTotalNilai(0.0);
				hasilUjianMahasiswa.setJumlahIkut(0);
				session.getTransaction().begin();
				session.save(hasilUjianMahasiswa);
				session.getTransaction().commit();
			} else if (hasilUjianMahasiswa != null && hasilUjianMahasiswa.keyhasil == null) {
				hasilUjianMahasiswa.setKeyhasil(keyhasil);
				session.getTransaction().begin();
				session.update(hasilUjianMahasiswa);
				session.getTransaction().commit();
			}
			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/HasilUjianMahasiswa.java:1105");
		}
		HibernateUtil.closeSession();
		GeneralValueObject.masukkanDataLangsung(HasilUjianMahasiswa.class, hasilUjianMahasiswa, keyhasil);
		return hasilUjianMahasiswa;
	}

	@Column(unique = true)
	public String getKeyhasil() {
		pertemuanPunyaUjian = getPertemuanPunyaUjian();
		mahasiswa = getMahasiswa();
		biodataCalonMahasiswa = getBiodataCalonMahasiswa();
		siswa = getSiswa();
		calonSiswa = getCalonSiswa();
		keyhasil = HasilUjianMahasiswa.genKey(pertemuanPunyaUjian, mahasiswa, biodataCalonMahasiswa, siswa, calonSiswa);
		return keyhasil;
	}

	public void setKeyhasil(String keyhasil) {
		this.keyhasil = keyhasil;
	}

	@Column(columnDefinition = "text")
	public String getNilaiObe() {
		return nilaiObe == null || nilaiObe.trim().isEmpty() ? Tugas.JSON : nilaiObe;
	}

	public void setNilaiObe(String nilaiObe) {
		this.nilaiObe = nilaiObe;
	}
}
