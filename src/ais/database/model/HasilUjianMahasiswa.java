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

	/**
	 * Durasi pengerjaan ujian sebagai jam:menit:detik.
	 *
	 * <h4>Dua sumber nilai, satu prioritas</h4>
	 * <ol>
	 *   <li><b>Nilai tercatat (diutamakan).</b> Bila {@code lamaPengerjaan} sudah pernah diisi —
	 *       biasanya oleh {@code ProsesUjianHelper.hitungWaktu(...)} saat peserta benar-benar
	 *       mengakhiri ujian, dihitung dari timestamp jawaban pertama sampai terakhir — nilai itu
	 *       dikembalikan apa adanya dan <b>tidak pernah dihitung ulang</b>.</li>
	 *   <li><b>Fallback estimasi.</b> Hanya bila belum pernah tercatat: durasi ditaksir sebagai
	 *       {@code pertemuanPunyaUjian.getLama() - getSisaWaktuPengerjaan()}, dipecah jadi
	 *       jam/menit/detik lewat modulo, lalu dibungkus {@code GregorianCalendar(0,0,0,h,m,s)}.
	 *       Estimasi ini hanya berlaku untuk ujian yang <b>bukan</b> mode waktu-per-soal
	 *       ({@code !getTiapSoal()}) dan yang punya durasi terdefinisi.</li>
	 * </ol>
	 *
	 * <p><b>Kenapa prioritas itu penting (regresi nyata, 29 Juli 2026).</b> Fallback bergantung
	 * pada {@link #getSisaWaktuPengerjaan()} yang nilainya berasal dari cache berkas "live" dan
	 * bisa basi setelah ujian selesai (sering terbaca nol = "waktu terpakai penuh"). Dulu
	 * hitung-ulang dilakukan tanpa syarat sehingga menimpa durasi asli dengan angka ngawur pada
	 * laporan waktu ujian. Jangan melepas penjaga {@code if (lamaPengerjaan != null)}.</p>
	 *
	 * <p><b>Efek samping:</b> pada jalur fallback, hasil hitungan ditulis balik ke field
	 * {@code lamaPengerjaan} sehingga ikut tersimpan ke database pada flush berikutnya. Method
	 * juga memanggil {@link #getPertemuanPunyaUjian()} dan {@link #getSisaWaktuPengerjaan()} yang
	 * masing-masing punya efek sampingnya sendiri. Seluruh badan dibungkus {@code try/catch} lebar
	 * (mis. {@code getLama()} bisa {@code null}); kegagalan dicatat ke audit error dan method tetap
	 * mengembalikan nilai field apa adanya.</p>
	 *
	 * <p>Dipanggil dari rekap hasil ujian, {@link #getJumlahIkut()}, dan laporan pengawasan.</p>
	 *
	 * @return durasi pengerjaan sebagai {@code TIME}, atau {@code null} bila tidak bisa ditentukan
	 * @see ais.action.master.helper.ProsesUjianHelper#hitungWaktu(HasilUjianMahasiswa, java.util.Map)
	 */
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

	/**
	 * Menyetel durasi pengerjaan.
	 *
	 * <p>Sumber utama pemanggilnya adalah {@code ProsesUjianHelper.hitungWaktu(...)} dan timer
	 * ujian di {@code ProsesUjianHelper}. Begitu nilai non-{@code null} tersimpan,
	 * {@link #getLamaPengerjaan()} berhenti menghitung ulang.</p>
	 *
	 * @param lamaPengerjaan durasi sebagai {@code TIME}; {@code null} mengaktifkan kembali jalur
	 *                       estimasi di getter
	 */
	public void setLamaPengerjaan(Date lamaPengerjaan) {
		this.lamaPengerjaan = lamaPengerjaan;
	}

	/**
	 * Sisa waktu pengerjaan yang belum terpakai (jam:menit:detik).
	 *
	 * <h4>Sumber nilai: cache berkas, bukan kolom database</h4>
	 * <p>Untuk ujian yang <b>bukan</b> mode waktu-per-soal ({@code !getTiapSoal()}), method ini
	 * membaca penanda waktu "hidup" yang ditulis timer ujian ke cache berkas per-entity lewat
	 * {@link GeneralValueObject#retreive()}, mem-parsing-nya dengan
	 * {@code Common.databaseDateFormat1}, lalu <b>menimpa field {@code sisaWaktuPengerjaan}</b>
	 * dengan nilai itu. Artinya nilai kolom database yang sudah tersimpan bisa tergantikan oleh
	 * isi cache berkas setiap kali getter ini dipanggil — akurat selama ujian berlangsung, dan
	 * berpotensi <b>basi setelah ujian selesai</b>. Inilah alasan {@link #getLamaPengerjaan()} dan
	 * {@link #getSelesaiPada()} sengaja tidak mempercayainya lagi begitu nilai aslinya tercatat.</p>
	 *
	 * <h4>Normalisasi jam &gt; 22</h4>
	 * <p>Bila sisa waktu hasil pembacaan jatuh pada jam ke-23 ke atas, itu gejala pengurangan
	 * waktu yang "membalik" melewati tengah malam (hasil negatif yang di-wrap). Nilainya
	 * dinormalkan menjadi {@code 00:00:01} — artinya "praktis habis" — supaya tampilan tidak
	 * memperlihatkan sisa waktu 23 jam pada ujian berdurasi satu jam.</p>
	 *
	 * <p><b>Efek samping:</b> menulis balik ke field (ikut tersimpan saat flush), memanggil
	 * {@link #getPertemuanPunyaUjian()} (bisa memicu query), dan membaca berkas cache. Seluruh
	 * badan dibungkus {@code try/catch}; kegagalan parsing dicatat ke audit error dan nilai field
	 * dikembalikan apa adanya.</p>
	 *
	 * @return sisa waktu sebagai {@code TIME}, atau {@code null} bila tidak tersedia
	 * @see GeneralValueObject#retreive()
	 * @see GeneralValueObject#put(String, String)
	 */
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

	/**
	 * Menyetel sisa waktu pengerjaan.
	 *
	 * <p>Dipanggil {@code ProsesUjianHelper.hitungWaktu(...)} saat ujian diakhiri, oleh dialog
	 * "ubah sisa waktu" di {@code HasilUjianMahasiswaHelper}/{@code HasilUjianSiswaHelper}
	 * (pengawas memberi tambahan waktu), dan dengan argumen {@code null} oleh fitur reset.
	 * Perhatikan bahwa nilai yang disetel di sini bisa <b>tertimpa lagi</b> oleh
	 * {@link #getSisaWaktuPengerjaan()} bila cache berkas masih berisi penanda waktu lama.</p>
	 *
	 * @param sisaWaktuPengerjaan sisa waktu sebagai {@code TIME}; boleh {@code null}
	 */
	public void setSisaWaktuPengerjaan(Date sisaWaktuPengerjaan) {
		this.sisaWaktuPengerjaan = sisaWaktuPengerjaan;
	}

	/**
	 * Apakah peserta sudah pernah masuk/mengikuti sesi ujian ini.
	 *
	 * <p>Dipakai bersama {@link #getLengkapiJawaban()} oleh {@code PertemuanPunyaUjianHelper}
	 * untuk memutuskan apakah tombol ujian ditampilkan sebagai "Ikut Ujian", "Lanjutkan", atau
	 * dikunci; juga menjadi kolom status di rekap pengawasan.</p>
	 *
	 * @return {@code true} bila sudah pernah ikut; {@code false} bila belum (nilai {@code null}
	 *         dinormalkan menjadi {@code false} agar aman di-unbox)
	 */
	public Boolean getTelahIkutUjian() {
		return telahIkutUjian == null ? false : telahIkutUjian;
	}

	/**
	 * Menandai peserta sudah mengikuti sesi ujian ini. Disetel {@code true} oleh
	 * {@code ProsesUjianHelper} ketika peserta membuka lembar ujian.
	 *
	 * @param telahIkutUjian status keikutsertaan
	 */
	public void setTelahIkutUjian(Boolean telahIkutUjian) {
		this.telahIkutUjian = telahIkutUjian;
	}

	/**
	 * Jumlah soal yang diujikan kepada peserta ini.
	 *
	 * <p><b>Nilai tersimpan praktis diabaikan.</b> Selama {@link #getPertemuanPunyaUjian()} bisa
	 * diresolusi, field {@code jumlahSoal} selalu ditimpa dengan
	 * {@code pertemuanPunyaUjian.getJmlDitampilkan()} — jadi kolom ini efektif hanya bayangan dari
	 * konfigurasi sesi ujian, bukan cacah historis soal yang benar-benar pernah tampil. Bila
	 * konfigurasi jumlah soal diubah setelah ujian berlangsung, angka lama peserta ikut berubah.
	 * Nilai {@code null} dinormalkan menjadi {@code 0.0} lebih dulu.</p>
	 *
	 * <p><b>Efek samping:</b> menulis balik ke field (ikut tersimpan saat flush) dan memicu
	 * resolusi relasi sesi ujian. Dipakai {@link #getNilai()} sebagai penyebut cadangan ketika
	 * {@code jawabanBenarMax} belum terisi.</p>
	 *
	 * @return jumlah soal sebagai {@code Double}; tidak pernah {@code null}
	 */
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

	/**
	 * Menyetel jumlah soal. Praktis tidak berpengaruh karena {@link #getJumlahSoal()} akan
	 * menimpanya kembali dari konfigurasi sesi ujian.
	 *
	 * @param jumlahSoal jumlah soal
	 */
	public void setJumlahSoal(Double jumlahSoal) {
		this.jumlahSoal = jumlahSoal;
	}

	/**
	 * Total <b>skor</b> yang berhasil diraih peserta atas soal-soal pilihan ganda.
	 *
	 * <p>Meski namanya "jawaban benar", isinya adalah akumulasi <i>skor</i> — bukan cacah soal —
	 * karena {@code ProsesUjianHelper.hitung(...)} memberi bobot berbeda per soal/opsi. Angka ini
	 * menjadi pembilang pada {@link #getNilai()}. Nilai {@code null} dinormalkan menjadi
	 * {@code 0.0} <b>dan ditulis balik ke field</b>.</p>
	 *
	 * @return total skor yang diraih; tidak pernah {@code null}
	 * @see ais.action.master.helper.ProsesUjianHelper#hitungPilihanGanda(HasilUjianMahasiswa, java.util.Map)
	 */
	public Double getJawabanBenar() {
		if (jawabanBenar == null) {
			jawabanBenar = 0.0;
		}
		return jawabanBenar;
	}

	/**
	 * Menyetel total skor yang diraih peserta. Diisi {@code ProsesUjianHelper.hitungPilihanGanda}
	 * (jalur koreksi otomatis) dan disetel {@code 0.0} untuk ujian esai yang belum dikoreksi.
	 *
	 * @param jawabanBenar total skor yang diraih
	 */
	public void setJawabanBenar(Double jawabanBenar) {
		this.jawabanBenar = jawabanBenar;
	}

	/**
	 * <b>Nilai akhir ujian dalam persen (0..100)</b> — inti koreksi otomatis kelas ini.
	 *
	 * <h4>Rumus</h4>
	 * <p>Perhitungan ulang hanya dilakukan bila jenis ujian adalah
	 * {@link BankSoal#PILIHAN_GANDA}, dengan dua penyebut berjenjang:</p>
	 * <ol>
	 *   <li>{@code jawabanBenar / jawabanBenarMax * 100} — dipakai bila
	 *       {@link #getJawabanBenarMax()} terisi dan lebih dari 0,1 (jalur normal, memperhitungkan
	 *       bobot skor per soal);</li>
	 *   <li>{@code jawabanBenar / jumlahSoal * 100} — cadangan bila skor maksimal belum pernah
	 *       dihitung; menganggap tiap soal bernilai 1.</li>
	 * </ol>
	 * <p>Untuk jenis ujian lain (esai) tidak ada perhitungan: nilai tersimpan dikembalikan apa
	 * adanya, karena penilaiannya manual lewat {@code KoreksiHasilUjian}.</p>
	 *
	 * <h4>Pengaman batas atas 100</h4>
	 * <p>Skala {@code jawabanBenarMax} tidak selalu konsisten (lihat catatan pada
	 * {@code ProsesUjianHelper.hitungPilihanGanda}), sehingga rasio bisa melebihi 100 —
	 * mis. 122/50 &times; 100 = 244. Hasil di atas 100 dipangkas ke 100 agar tampilan dan laporan
	 * tidak absurd. Ini penambal gejala; perbaikan akar plus menjalankan "Hitung Ulang" yang
	 * membetulkan datanya.</p>
	 *
	 * <p><b>Efek samping:</b> menulis balik ke field {@code nilai} (termasuk hasil pemangkasan),
	 * sehingga angka yang tampil di layar ikut tersimpan ke database pada flush berikutnya. Juga
	 * memicu {@link #getPertemuanPunyaUjian()}, {@link #getJumlahSoal()}, dan
	 * {@link GeneralValueObject#check(Object)} atas {@link Ujian}. Exception apa pun ditelan dan
	 * dicatat ke audit error; nilai {@code null} dinormalkan menjadi {@code 0.0}.</p>
	 *
	 * @return nilai ujian dalam persen, dijamin berada di rentang 0..100 dan tidak {@code null}
	 * @see #getLulus()
	 * @see #getTotalNilai()
	 */
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

	/**
	 * Menyetel nilai ujian (persen). Perhatikan bahwa untuk ujian pilihan ganda
	 * {@link #getNilai()} akan menghitung ulang dan menimpa nilai ini pada pembacaan berikutnya —
	 * setter ini hanya benar-benar "menempel" untuk ujian esai.
	 *
	 * @param nilai nilai dalam persen
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Status kelulusan peserta pada sesi ujian ini.
	 *
	 * <p>Dihitung ulang setiap kali dibaca sebagai {@code ujian.getNilaiLulus() <= getNilai()},
	 * yaitu ambang batas kelulusan yang dikonfigurasi pada {@link Ujian} dibandingkan dengan nilai
	 * persen hasil {@link #getNilai()}. Ambang bersifat inklusif — nilai persis sama dengan ambang
	 * dinyatakan lulus. Bila relasi ujian tidak bisa diresolusi, nilai lama dipertahankan.</p>
	 *
	 * <p><b>Efek samping:</b> menulis balik ke field {@code lulus} (ikut tersimpan saat flush) dan
	 * memanggil {@link #getNilai()} beserta seluruh efek sampingnya. Exception ditelan dan dicatat
	 * ke audit error; {@code null} dinormalkan menjadi {@code false}.</p>
	 *
	 * @return {@code true} bila nilai mencapai ambang kelulusan; tidak pernah {@code null}
	 */
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

	/**
	 * Menyetel status kelulusan. Akan dihitung ulang dan ditimpa oleh {@link #getLulus()} selama
	 * relasi ujian bisa diresolusi.
	 *
	 * @param lulus status kelulusan
	 */
	public void setLulus(Boolean lulus) {
		this.lulus = lulus;
	}

	/**
	 * Total skor maksimal yang mungkin diraih pada sesi ujian ini — penyebut utama
	 * {@link #getNilai()}.
	 *
	 * <p>Diisi {@code ProsesUjianHelper.hitungPilihanGanda(...)} dengan menjumlahkan skor
	 * tertinggi tiap soal unik. Bila belum pernah dihitung (bernilai {@code null} atau &le; 0,1),
	 * {@link #getNilai()} beralih memakai {@link #getJumlahSoal()} sebagai penyebut.</p>
	 *
	 * @return skor maksimal; bisa {@code null} untuk baris yang belum pernah dikoreksi
	 */
	public Double getJawabanBenarMax() {
		return jawabanBenarMax;
	}

	/**
	 * Menyetel total skor maksimal yang mungkin diraih.
	 *
	 * @param jawabanBenarMax skor maksimal hasil agregasi koreksi otomatis
	 */
	public void setJawabanBenarMax(Double jawabanBenarMax) {
		this.jawabanBenarMax = jawabanBenarMax;
	}

	/**
	 * Waktu peserta menyelesaikan ujian (timestamp penuh).
	 *
	 * <h4>Dua sumber nilai, satu prioritas</h4>
	 * <ol>
	 *   <li><b>Nilai tercatat (diutamakan).</b> Bila {@code selesaiPada} sudah pernah diisi
	 *       {@code ProsesUjianHelper.hitungWaktu(...)} — berdasarkan timestamp jawaban terakhir
	 *       yang sungguhan — nilai itu dikembalikan apa adanya tanpa hitung ulang.</li>
	 *   <li><b>Fallback estimasi.</b> Hanya bila belum pernah tercatat dan ujian bukan mode
	 *       waktu-per-soal: {@code mulaiPada + (lama ujian - sisaWaktuPengerjaan)}, ditambahkan
	 *       lewat {@link Calendar} dengan field {@code HOUR_OF_DAY}/{@code MINUTE}/{@code SECOND}.</li>
	 * </ol>
	 *
	 * <h4>Dua bekas bug yang penjaganya jangan dilepas</h4>
	 * <ul>
	 *   <li><b>"Selesai di masa depan".</b> Dulu hitung-ulang dilakukan tanpa syarat dan menimpa
	 *       waktu asli. Karena {@link #getSisaWaktuPengerjaan()} bersumber dari cache berkas yang
	 *       basi setelah ujian (kerap terbaca 0), estimasinya melompat ke
	 *       {@code mulaiPada + durasi PENUH} — mis. 24 jam ke depan — walau peserta submit jauh
	 *       lebih awal. Penjaga {@code if (selesaiPada != null)} adalah perbaikannya.</li>
	 *   <li><b>Pergeseran 12 jam.</b> Penambahan jam sempat memakai {@code Calendar.HOUR}
	 *       (format 12 jam, tanpa informasi AM/PM saat di-set ulang) sehingga hasilnya bisa
	 *       bergeser setengah hari. Sekarang memakai {@code Calendar.HOUR_OF_DAY} yang tidak
	 *       ambigu. Perhatikan bahwa {@code Calendar} bersifat lenient, jadi nilai jam &gt; 23
	 *       sengaja dibiarkan meluber ke hari berikutnya.</li>
	 * </ul>
	 *
	 * <p><b>Efek samping:</b> pada jalur fallback, hasil hitungan ditulis balik ke field
	 * {@code selesaiPada} dan ikut tersimpan ke database. Memanggil
	 * {@link #getPertemuanPunyaUjian()}, {@link #getMulaiPada()}, dan
	 * {@link #getSisaWaktuPengerjaan()}. Exception ditelan dan dicatat ke audit error.</p>
	 *
	 * @return waktu selesai, atau {@code null} bila belum tercatat dan tidak bisa diestimasi
	 */
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

	/**
	 * Menyetel waktu selesai ujian. Sekali terisi non-{@code null}, {@link #getSelesaiPada()}
	 * berhenti mengestimasi.
	 *
	 * @param selesaiPada waktu selesai; {@code null} mengaktifkan kembali jalur estimasi
	 */
	public void setSelesaiPada(Date selesaiPada) {
		this.selesaiPada = selesaiPada;
	}

	/**
	 * Waktu peserta mulai mengerjakan ujian (timestamp penuh).
	 *
	 * <p>Getter murni — tidak menghitung apa pun. Menjadi titik awal estimasi di
	 * {@link #getSelesaiPada()}.</p>
	 *
	 * @return waktu mulai, atau {@code null} bila peserta belum pernah memulai
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getMulaiPada() {
		return mulaiPada;
	}

	/**
	 * Menyetel waktu mulai ujian — <b>setter sekali-tulis</b>.
	 *
	 * <p>Bila field sudah terisi, argumen <b>diabaikan diam-diam</b>. Tujuannya melindungi waktu
	 * mulai yang sebenarnya: peserta bisa keluar-masuk lembar ujian berkali-kali (dan
	 * {@code ProsesUjianHelper} memanggil setter ini di setiap pembukaan), tetapi durasi
	 * pengerjaan harus dihitung dari kunjungan pertama. Konsekuensinya waktu mulai <b>tidak bisa
	 * dikoreksi maupun dikosongkan</b> lewat setter ini; satu-satunya jalan mengulang dari nol
	 * adalah {@link #reset()} yang menyentuh field secara langsung.</p>
	 *
	 * @param mulaiPada waktu mulai yang diusulkan; diabaikan bila sudah ada nilai sebelumnya
	 */
	public void setMulaiPada(Date mulaiPada) {
		if (this.mulaiPada == null) {
			this.mulaiPada = mulaiPada;
		}
	}

	/**
	 * Mengosongkan seluruh jejak pengerjaan sehingga peserta bisa mengulang ujian dari nol.
	 *
	 * <p>Menyetel {@code mulaiPada}, {@code selesaiPada}, {@code jawabanBenarMax}, {@code nilai},
	 * {@code jawabanBenar}, {@code jumlahIkut}, {@code lamaPengerjaan}, dan
	 * {@code lengkapiJawaban} menjadi {@code null}, serta {@code telahIkutUjian} menjadi
	 * {@code false}. Field disentuh <b>langsung</b>, bukan lewat setter — penting karena
	 * {@link #setMulaiPada(Date)} bersifat sekali-tulis dan tidak akan pernah bisa mengosongkan
	 * waktu mulai.</p>
	 *
	 * <p><b>Efek samping tambahan:</b> memanggil {@code put("1", "index")} yang menulis nilai
	 * {@code "1"} ke cache berkas per-entity dengan sufiks {@code "index"} — penanda posisi soal
	 * dikembalikan ke soal pertama. Perhatikan bahwa <b>jawaban peserta
	 * ({@link HasilUjianMahasiswaDetail}) tidak ikut dihapus</b> di sini; pemanggil
	 * ({@code HasilUjianMahasiswaHelper}/{@code HasilUjianSiswaHelper}, aksi "ulangi ujian")
	 * yang bertanggung jawab menghapusnya. Baris hasil sendiri juga tidak otomatis disimpan —
	 * penyimpanan mengikuti transaksi pemanggil.</p>
	 */
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

	/**
	 * Peserta ujian bila ia seorang siswa sekolah (modul {@code ais.database.model.sekolah}).
	 *
	 * <p>Me-resolusi proxy lazy lewat {@link GeneralValueObject#check(Object)} dan menulis balik
	 * hasilnya ke field, sama seperti getter relasi lain di kelas ini.</p>
	 *
	 * @return siswa peserta, atau {@code null} bila peserta berjenis lain
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = true)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	/**
	 * Menyetel peserta siswa sekolah.
	 *
	 * @param siswa siswa peserta; {@code null} bila peserta berjenis lain
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Peserta ujian bila ia seorang calon siswa (PPDB).
	 *
	 * <p>Me-resolusi proxy lazy lewat {@link GeneralValueObject#check(Object)} dan menulis balik
	 * hasilnya ke field, sama seperti getter relasi lain di kelas ini.</p>
	 *
	 * @return calon siswa peserta, atau {@code null} bila peserta berjenis lain
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "calon_siswa", nullable = true)
	public CalonSiswa getCalonSiswa() {
		calonSiswa = check(calonSiswa);
		return calonSiswa;
	}

	/**
	 * Menyetel peserta calon siswa (PPDB).
	 *
	 * @param calonSiswa calon siswa peserta; {@code null} bila peserta berjenis lain
	 */
	public void setCalonSiswa(CalonSiswa calonSiswa) {
		this.calonSiswa = calonSiswa;
	}

	/**
	 * Menghitung berapa banyak soal yang sudah dijawab peserta.
	 *
	 * <p>Pintasan tipis: memanggil
	 * {@link #ambilBankSoalIdTerjawab(int, MyArrayList, boolean)} dengan {@code refresh = false}
	 * lalu mengembalikan ukuran himpunannya. Karena berbasis himpunan id {@link BankSoal},
	 * beberapa baris jawaban atas soal yang sama hanya dihitung sekali.</p>
	 *
	 * <p><b>Catatan pemeliharaan:</b> pencarian menyeluruh atas repo menunjukkan method ini
	 * <b>tidak dipanggil dari mana pun</b> (kode mati). Dibiarkan apa adanya. Penugasan
	 * {@code terjawab = null} sebelum {@code return} tidak berpengaruh apa pun — itu variabel
	 * lokal; idiom "membuang referensi" semacam ini muncul berulang di file ini.</p>
	 *
	 * @param jumlahDiujikan      jumlah soal yang diujikan (kapasitas awal peta hasil)
	 * @param ujianPunyaSoalsData daftar id {@link UjianPunyaSoal} yang menjadi filter; boleh
	 *                            {@code null}/kosong berarti tanpa filter
	 * @return cacah soal unik yang sudah terjawab
	 */
	public int ambilJumlahTerjawab(int jumlahDiujikan, MyArrayList<Long> ujianPunyaSoalsData) {
		Set<Long> terjawab = ambilBankSoalIdTerjawab(jumlahDiujikan, ujianPunyaSoalsData, false);
		int size = terjawab.size();
		terjawab = null;
		return size;
	}

	/**
	 * Pintasan {@link #ambilBankSoalIdTerjawab(int, MyArrayList, boolean)} dengan
	 * {@code refresh = false} (memakai cache jawaban yang ada).
	 *
	 * @param jumlahDiujikan      jumlah soal yang diujikan
	 * @param ujianPunyaSoalsData daftar id {@link UjianPunyaSoal} sebagai filter; boleh kosong
	 * @return himpunan id {@link BankSoal} yang sudah terjawab
	 * @see #ambilBankSoalIdTerjawab(int, MyArrayList, boolean)
	 */
	public Set<Long> ambilBankSoalIdTerjawab(int jumlahDiujikan, MyArrayList<Long> ujianPunyaSoalsData) {
		return ambilBankSoalIdTerjawab(jumlahDiujikan, ujianPunyaSoalsData, false);
	}

	/**
	 * Himpunan id {@link BankSoal} yang sudah dijawab peserta, dihitung dari nol: memuat dulu peta
	 * jawaban lewat {@link #ambilHasilUjianMahasiswaDetail(int, MyArrayList, boolean)} lalu
	 * menyaringnya dengan aturan yang sama seperti
	 * {@link #ambilBankSoalIdTerjawab(Map)}.
	 *
	 * <p>Sebuah soal dianggap terjawab bila detailnya punya {@link BankSoalDetail} terpilih
	 * (jawaban pilihan ganda) <b>atau</b> teks jawaban yang tidak kosong (jawaban esai).</p>
	 *
	 * <p>Dipakai {@code ProsesUjianHelper} untuk menandai soal yang sudah/belum dijawab pada
	 * navigator soal, dan oleh {@code HasilUjianMahasiswaHelper}/{@code HasilUjianSiswaHelper}
	 * saat menyusun rekap.</p>
	 *
	 * @param jumlahDiujikan      jumlah soal yang diujikan (kapasitas awal peta hasil)
	 * @param ujianPunyaSoalsData daftar id {@link UjianPunyaSoal} sebagai filter; boleh
	 *                            {@code null}/kosong berarti tanpa filter
	 * @param refresh             {@code true} memaksa muat ulang daftar jawaban dari database dan
	 *                            menyegarkan cache — wajib dipakai tepat setelah jawaban baru
	 *                            disimpan, jika tidak hasilnya bisa basi
	 * @return himpunan id {@link BankSoal} yang sudah terjawab; kosong bila belum ada jawaban
	 */
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

	/**
	 * Varian hemat dari {@link #ambilBankSoalIdTerjawab(int, MyArrayList, boolean)}: menyaring
	 * peta jawaban yang <b>sudah dimuat pemanggil</b>, tanpa query/muat ulang.
	 *
	 * <p>Pakai varian ini bila beberapa penyaringan dilakukan berturut-turut atas peta yang sama —
	 * mis. {@code KoreksiHasilUjian.loadData} memanggil ketiga penyaring
	 * ({@code ...Terjawab}, {@code ...TerjawabBenar}, {@code ...TerjawabDinilai}) atas satu peta
	 * untuk mengisi filter "sudah dijawab / belum dijawab / benar / salah / sudah dinilai".</p>
	 *
	 * <p><b>Kriteria terjawab</b>: detail memiliki {@link BankSoalDetail} terpilih dengan id
	 * non-{@code null} (pilihan ganda), <b>atau</b> teks jawabannya tidak kosong setelah
	 * {@code trim()} (esai). Detail yang id-nya tidak ditemukan di cache
	 * ({@link GeneralValueObject#ambilData(Class, String)} mengembalikan {@code null}) dilewati
	 * diam-diam.</p>
	 *
	 * @param hasilUjianMahasiswaDetailsa peta {@code bankSoalId -> himpunan id
	 *                                    HasilUjianMahasiswaDetail} hasil
	 *                                    {@code ambilHasilUjianMahasiswaDetail(...)}
	 * @return himpunan id {@link BankSoal} yang sudah terjawab
	 */
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

	/**
	 * Himpunan id {@link BankSoal} yang jawabannya <b>sudah diberi nilai</b> (skor detail
	 * &gt; 0,01), disaring dari peta jawaban yang sudah dimuat pemanggil.
	 *
	 * <p>Berbeda dari {@link #ambilBankSoalIdTerjawab(Map)} yang menanyakan "sudah dijawab?",
	 * method ini menanyakan "sudah dinilai?" — relevan untuk ujian esai yang koreksinya manual.
	 * Ambang 0,01 dipakai sebagai pengganti perbandingan {@code == 0} pada bilangan pecahan.</p>
	 *
	 * <p><b>Konsekuensi ambang itu:</b> soal esai yang sudah dikoreksi tetapi diberi nilai
	 * <b>nol</b> (jawaban salah total) tidak masuk himpunan ini — dari sudut pandang filter
	 * "sudah dinilai" ia tampak belum dikoreksi. Perilaku ini dibiarkan apa adanya, cukup
	 * disadari saat membaca filter di {@code KoreksiHasilUjian}.</p>
	 *
	 * <p><b>Perhatian:</b> tidak seperti saudara-saudaranya, method ini memanggil
	 * {@code getBankSoal().getId()} tanpa memeriksa {@code null} lebih dulu; detail yatim
	 * (tanpa {@link BankSoal}) akan melempar {@code NullPointerException} ke pemanggil. Dalam
	 * praktik hal itu tidak terjadi karena {@link #ambilDataAsli(MyArrayList, boolean)} sudah
	 * menyaring {@code Restrictions.isNotNull("bankSoal")}.</p>
	 *
	 * @param hasilUjianMahasiswaDetailsa peta {@code bankSoalId -> himpunan id detail}
	 * @return himpunan id {@link BankSoal} yang sudah memperoleh nilai
	 */
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

	/**
	 * Himpunan id {@link BankSoal} yang dijawab <b>benar</b>, disaring dari peta jawaban yang
	 * sudah dimuat pemanggil.
	 *
	 * <p>Kriterianya murni pilihan ganda: detail harus punya {@link BankSoalDetail} terpilih
	 * (opsi yang diklik peserta) dan opsi itu ditandai sebagai kunci jawaban
	 * ({@code bankSoalDetail.getBetul()}). Soal esai tidak pernah masuk himpunan ini karena tidak
	 * punya {@code bankSoalDetail} — untuk esai gunakan
	 * {@link #ambilBankSoalIdTerjawabDinilai(Map)}.</p>
	 *
	 * <p>Dipakai {@code KoreksiHasilUjian} untuk filter "benar dijawab"; komplemennya (filter
	 * "salah dijawab") dihitung di sana sebagai negasi himpunan ini.</p>
	 *
	 * @param hasilUjianMahasiswaDetailsa peta {@code bankSoalId -> himpunan id detail}
	 * @return himpunan id {@link BankSoal} yang dijawab benar
	 */
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

	/**
	 * Menyusun <b>daftar soal yang akan ditampilkan ulang</b> kepada peserta, dalam bentuk daftar
	 * id {@link UjianPunyaSoal}, dengan mendahulukan soal yang sudah pernah dijawab.
	 *
	 * <p>Ini adalah method yang membuat fitur "lanjutkan ujian" bekerja: ketika peserta masuk
	 * kembali, soal yang jawabannya sudah tersimpan harus muncul lagi persis seperti sebelumnya,
	 * baru sisanya diisi soal lain sampai kuota terpenuhi.</p>
	 *
	 * <h4>Alur</h4>
	 * <ol>
	 *   <li>Mengambil seluruh id {@link HasilUjianMahasiswaDetail} milik peserta lewat
	 *       {@link #ambilDataAsli(MyArrayList, boolean)} (tanpa filter soal).</li>
	 *   <li><b>Pengurutan prioritas</b>: detail yang sudah punya jawaban (ada
	 *       {@code bankSoalDetail} atau teks jawaban tidak kosong) diletakkan di depan, sisanya
	 *       ditambahkan setelahnya.</li>
	 *   <li><b>Lintasan pertama</b>: menyalin id {@code ujianPunyaSoal} dari detail yang sudah
	 *       dijawab, sampai kuota {@code maxSize} terpenuhi.</li>
	 *   <li><b>Lintasan kedua</b>: bila kuota belum penuh, mengisi dari sisa detail (yang belum
	 *       dijawab), dengan pemeriksaan duplikat.</li>
	 *   <li>Setiap kali sebuah soal ditambahkan, {@code label} diperbarui dengan teks progres
	 *       "harap tunggu.. Sedang memasukkan soal (xx %)" — ini yang terlihat pengguna saat
	 *       lembar ujian sedang dibangun.</li>
	 * </ol>
	 *
	 * <p><b>Kuirk yang perlu diketahui:</b> lintasan pertama <b>tidak</b> memeriksa duplikat
	 * (hanya lintasan kedua yang melakukannya), sehingga id {@code ujianPunyaSoal} yang sama bisa
	 * masuk dua kali bila peserta punya lebih dari satu baris jawaban untuk soal yang sama —
	 * kasus yang mungkin terjadi pada soal multiple-choice berjawaban jamak. Selain itu
	 * {@code maxSize} negatif dinormalkan menjadi {@code 0} (mengembalikan daftar kosong), dan
	 * persentase progres memakai penyebut jumlah <i>detail</i>, bukan {@code maxSize}, sehingga
	 * angka yang tampil bisa tidak mencapai 100&nbsp;%.</p>
	 *
	 * <p><b>Biaya:</b> mahal. Setiap detail diambil lewat
	 * {@link GeneralValueObject#ambilData(Class, String, boolean)} dan seluruh daftar dilintasi
	 * hingga empat kali. Umumnya dipanggil sekali per pembukaan lembar ujian/rekap.</p>
	 *
	 * @param maxSize jumlah soal maksimum yang dikembalikan; biasanya
	 *                {@code pertemuanPunyaUjian.getJmlDitampilkan()}. Nilai negatif diperlakukan
	 *                sebagai 0
	 * @param label   komponen ZK tempat menampilkan progres; boleh {@code null} bila tidak ada UI
	 *                (pemanggil non-UI kerap mengoper {@code new Label()} sekadar pengisi)
	 * @param refresh {@code true} memaksa muat ulang daftar jawaban dari database sebelum
	 *                menyusun daftar soal
	 * @return daftar id {@link UjianPunyaSoal} berurut prioritas, panjang maksimal {@code maxSize}
	 */
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

	/**
	 * Pintasan {@link #ambilHasilUjianMahasiswaDetail(boolean, int, Label, MyArrayList)} tanpa
	 * label progres dan tanpa refresh.
	 *
	 * @param jumlahDiujikan  jumlah soal yang diujikan (kapasitas awal peta hasil)
	 * @param ujianPunyaSoals daftar id {@link UjianPunyaSoal} sebagai filter; boleh {@code null}
	 * @return peta {@code bankSoalId -> himpunan id HasilUjianMahasiswaDetail}
	 * @see #ambilHasilUjianMahasiswaDetail(boolean, int, Label, MyArrayList)
	 */
	public Map<Long, Set<Long>> ambilHasilUjianMahasiswaDetail(int jumlahDiujikan, MyArrayList<Long> ujianPunyaSoals) {
		return ambilHasilUjianMahasiswaDetail(jumlahDiujikan, ujianPunyaSoals, false);
	}

	/**
	 * Pintasan {@link #ambilHasilUjianMahasiswaDetail(boolean, int, Label, MyArrayList)} tanpa
	 * label progres.
	 *
	 * @param jumlahDiujikan  jumlah soal yang diujikan (kapasitas awal peta hasil)
	 * @param ujianPunyaSoals daftar id {@link UjianPunyaSoal} sebagai filter; boleh {@code null}
	 * @param refresh         {@code true} memaksa muat ulang dari database dan menyegarkan cache
	 * @return peta {@code bankSoalId -> himpunan id HasilUjianMahasiswaDetail}
	 * @see #ambilHasilUjianMahasiswaDetail(boolean, int, Label, MyArrayList)
	 */
	public Map<Long, Set<Long>> ambilHasilUjianMahasiswaDetail(int jumlahDiujikan, MyArrayList<Long> ujianPunyaSoals,
			boolean refresh) {
		return ambilHasilUjianMahasiswaDetail(refresh, jumlahDiujikan, null, ujianPunyaSoals);
	}

	/**
	 * Cache statik lintas-JVM: {@code hasilUjianMahasiswa.id -> daftar id
	 * HasilUjianMahasiswaDetail} miliknya, terurut menaik.
	 *
	 * <p>Peta didapat dari {@code ais.common.MemoryCacheUtil.get("SoalYgSudahDiambils")} dan
	 * diinisialisasi malas dengan <i>double-checked locking</i> di
	 * {@link #ambilDataAsli(MyArrayList, boolean)}. Karena statik, isinya dibagi <b>seluruh
	 * pengguna dan seluruh thread</b> — setiap akses ke peta ini WAJIB berada dalam blok
	 * {@code synchronized}, dan data basi hanya bisa dibersihkan dengan memanggil ulang
	 * {@code ambilDataAsli(..., refresh = true)}.</p>
	 */
	private static Map<Long, List<Long>> ygSudahDiambils = null;

	/**
	 * <b>Mesin cache jawaban peserta</b>: mengembalikan daftar id
	 * {@link HasilUjianMahasiswaDetail} milik baris hasil ujian ini, dengan cache statik lintas
	 * permintaan.
	 *
	 * <p>Seluruh keluarga {@code ambilHasilUjianMahasiswaDetail(...)},
	 * {@code ambilBankSoalIdTerjawab*(...)}, dan {@link #ambilUjianPunyaSoals(int, Label, boolean)}
	 * bermuara ke sini. Relasi ke detail sengaja <b>tidak</b> dipetakan sebagai koleksi Hibernate
	 * agar pemuatannya bisa dikendalikan dan di-cache seperti ini.</p>
	 *
	 * <h4>Alur</h4>
	 * <ol>
	 *   <li>Inisialisasi malas {@code ygSudahDiambils} dari {@code MemoryCacheUtil} dengan
	 *       double-checked locking.</li>
	 *   <li>Bila entity belum punya id (baris transient), langsung mengembalikan daftar kosong.</li>
	 *   <li>Cache dibaca dalam blok {@code synchronized}. Bila kosong atau {@code refresh}
	 *       diminta, dijalankan query {@link Criteria} atas {@link HasilUjianMahasiswaDetail}
	 *       dengan syarat {@code bankSoal} tidak null, {@code hasilUjianMahasiswa.id} sama dengan
	 *       id ini, opsional dibatasi daftar {@code ujianPunyaSoal.id}, diurutkan menaik, dan
	 *       hanya memproyeksikan kolom {@code id}.</li>
	 *   <li>Untuk tiap id hasil query, isi detailnya dihangatkan ke cache object
	 *       ({@link GeneralValueObject#masukkanData(Class, GeneralValueObject)}) — dimuat ulang
	 *       dari database bila {@code refresh} diminta atau bila cache meleset.</li>
	 *   <li>Daftar id disimpan ke cache statik, lalu <b>salinannya</b> yang dikembalikan.</li>
	 * </ol>
	 *
	 * <h4>Tiga keputusan desain yang jangan diubah tanpa memahami sebabnya</h4>
	 * <ul>
	 *   <li><b>Session terdedikasi.</b> Sengaja memakai {@code openSession()}, bukan
	 *       {@code currentNativeSession()}. Method ini kerap dipanggil dari proses ekspor yang
	 *       sudah memegang session sendiri; menutup session bersama di blok {@code finally} akan
	 *       menutup {@code ResultSet} milik pemanggil. Penutupannya bertahap
	 *       ({@code clear} &rarr; {@code disconnect} &rarr; {@code close}), masing-masing
	 *       dibungkus {@code try/catch} sendiri agar satu kegagalan tidak menggagalkan sisanya.</li>
	 *   <li><b>Muat ulang saat {@code refresh}.</b> Cache MapDB menyimpan <i>salinan</i>
	 *       ter-serialisasi. Tanpa muat ulang eksplisit, jawaban yang baru saja disimpan tidak
	 *       terbaca sehingga skor keluar 0 walau peserta menjawab benar.</li>
	 *   <li><b>Mengembalikan salinan.</b> Yang dikembalikan adalah {@code new ArrayList<>(...)},
	 *       bukan daftar di cache. Pemanggil yang memanggil {@code .clear()} atas hasilnya pernah
	 *       merusak cache peserta; salinan juga mencegah
	 *       {@code ConcurrentModificationException} saat daftar yang sama dilintasi banyak
	 *       thread.</li>
	 * </ul>
	 *
	 * <p><b>Penanganan galat:</b> kegagalan query ditelan lewat
	 * {@code Common.tampilErrorJikaAdmin(e)} (hanya tampil bagi admin) dan method tetap
	 * mengembalikan isi cache lama atau daftar kosong.</p>
	 *
	 * @param ujianPunyaSoals daftar id {@link UjianPunyaSoal} untuk membatasi hasil; {@code null}
	 *                        atau kosong berarti seluruh jawaban peserta diambil
	 * @param refresh         {@code true} melewati cache, memuat ulang dari database, dan
	 *                        menyegarkan cache object tiap detail
	 * @return salinan daftar id detail terurut menaik; tidak pernah {@code null}
	 */
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

	/**
	 * <b>Penentu jawaban resmi peserta per soal</b> — method paling berlogika di kelas ini.
	 *
	 * <p>Masalah yang dipecahkan: satu peserta bisa punya <b>beberapa</b> baris
	 * {@link HasilUjianMahasiswaDetail} untuk soal yang sama (peserta mengubah jawaban, koneksi
	 * putus lalu jawaban dikirim ulang, atau peserta mengulang ujian). Method ini memilih
	 * <i>satu</i> baris yang dianggap jawaban sah untuk tiap soal, lalu mengelompokkannya menjadi
	 * peta {@code bankSoalId -> himpunan id detail} yang dipakai seluruh perhitungan skor dan
	 * tampilan koreksi.</p>
	 *
	 * <h4>Tahap 1 — deduplikasi ke {@code detailSoalUjian}</h4>
	 * <p>Tiap detail diberi kunci. Untuk <b>esai</b> ({@link BankSoal#ESAY}) serta pilihan ganda
	 * berjenis {@link BankSoal#MULTIPLE_COICE} dan {@link BankSoal#BENAR_SALAH}, kuncinya cukup
	 * {@code bankSoalId} (satu jawaban per soal). Untuk jenis lain kuncinya
	 * {@code bankSoalId + "_" + bankSoalDetailId} sehingga tiap opsi yang dipilih dipertahankan
	 * terpisah (soal berjawaban jamak).</p>
	 * <p>Bila dua detail berebut kunci yang sama, pemenangnya ditentukan berjenjang:</p>
	 * <ul>
	 *   <li><b>Esai</b>: detail yang <i>ada</i> {@code waktuJawab} mengalahkan yang tidak punya;
	 *       detail yang jawabannya tidak kosong mengalahkan yang kosong; setelah itu
	 *       {@code waktuJawab} termuda menang; bila keduanya sama, {@code tanggal_dirubah}
	 *       termuda yang menang.</li>
	 *   <li><b>Pilihan ganda</b>: detail yang punya {@code bankSoalDetail} (opsi benar-benar
	 *       dipilih) mengalahkan yang tidak; selebihnya {@code tanggal_dirubah} termuda menang.</li>
	 * </ul>
	 * <p>Intinya: <b>jawaban yang ada isinya dan paling baru selalu menang</b> — jawaban kosong
	 * yang datang belakangan tidak boleh menghapus jawaban lama yang sudah terisi.</p>
	 *
	 * <h4>Tahap 2 — pengelompokan ke hasil akhir</h4>
	 * <p>Pemenang tiap kunci dikelompokkan per {@code bankSoalId}. Untuk esai/multiple-choice/
	 * benar-salah, himpunan sengaja <b>diganti</b> (selalu berisi tepat satu detail); untuk jenis
	 * lain detail-detail ditumpuk ke himpunan yang sama sehingga satu soal bisa memuat beberapa
	 * opsi terpilih.</p>
	 *
	 * <h4>Penyaringan opsional</h4>
	 * <p>Bila {@code ujianPunyaSoals} diisi, daftar itu lebih dulu diterjemahkan menjadi himpunan
	 * {@code bankSoalId} dan hanya soal di dalamnya yang diproses. Daftar {@code null}/kosong
	 * berarti semua soal ikut.</p>
	 *
	 * <p><b>Efek samping &amp; catatan:</b> memanggil {@link #ambilDataAsli(MyArrayList, boolean)}
	 * (bisa membuka session dan query database) dan {@link #getPertemuanPunyaUjian()} yang
	 * hasilnya ditulis balik ke field. Kegagalan per-detail ditelan lewat
	 * {@code Common.tampilErrorJikaAdmin(e)} sehingga satu baris rusak tidak menggagalkan seluruh
	 * rekap. Daftar dari {@code ambilDataAsli} sengaja <b>tidak</b> di-{@code clear()} di akhir —
	 * dulu itu merusak cache statik; cukup referensi lokalnya yang dilepas. Parameter
	 * {@code label} saat ini <b>tidak dipakai</b> di badan method (sisa dari versi yang
	 * menampilkan progres); pemanggil tetap mengopernya demi kompatibilitas tanda tangan.</p>
	 *
	 * @param refresh         {@code true} memaksa muat ulang jawaban dari database sebelum
	 *                        diproses — wajib setelah menyimpan jawaban baru
	 * @param jumlahDiujikan  jumlah soal yang diujikan; dipakai sebagai kapasitas awal
	 *                        {@link MyHashMap} hasil
	 * @param label           komponen ZK untuk progres; <b>tidak digunakan</b>, boleh {@code null}
	 * @param ujianPunyaSoals daftar id {@link UjianPunyaSoal} sebagai penyaring; boleh
	 *                        {@code null}/kosong
	 * @return peta {@code bankSoalId -> himpunan id HasilUjianMahasiswaDetail} berisi jawaban
	 *         terpilih; tidak pernah {@code null}
	 * @see ais.action.master.helper.ProsesUjianHelper#hitungPilihanGanda(HasilUjianMahasiswa, java.util.Map)
	 * @see ais.action.master.helper.KoreksiHasilUjian
	 */
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

	/**
	 * Mengambil himpunan id detail jawaban untuk <b>satu soal</b> dari peta yang sudah dimuat
	 * pemanggil.
	 *
	 * <p>Sekadar pencarian pada peta dengan normalisasi hasil: bila soal belum pernah dijawab,
	 * yang dikembalikan himpunan kosong, bukan {@code null} — sehingga pemanggil
	 * ({@code ProsesUjianHelper} saat merender satu soal) bisa langsung mengiterasinya.</p>
	 *
	 * <p>Penugasan {@code hasilUjianMahasiswaDetailsa = null} sebelum {@code return} hanya
	 * mengosongkan variabel parameter di dalam method; peta milik pemanggil tidak terpengaruh.</p>
	 *
	 * @param hasilUjianMahasiswaDetailsa peta {@code bankSoalId -> himpunan id detail}
	 * @param bankSoal                    soal yang dicari jawabannya
	 * @return himpunan id {@link HasilUjianMahasiswaDetail} untuk soal tersebut; kosong bila belum
	 *         dijawab
	 */
	public Set<Long> ambilHasilUjianMahasiswaDetail(Map<Long, Set<Long>> hasilUjianMahasiswaDetailsa,
			BankSoal bankSoal) {
		Set<Long> hasilUjianMahasiswaDetail = hasilUjianMahasiswaDetailsa.get(bankSoal.getId());
		if (hasilUjianMahasiswaDetail == null) {
			hasilUjianMahasiswaDetail = new HashSet<Long>();
		}
		hasilUjianMahasiswaDetailsa = null;
		return hasilUjianMahasiswaDetail;
	}

	/**
	 * Varian "sekali jalan" dari {@link #ambilHasilUjianMahasiswaDetail(Map, BankSoal)}: memuat
	 * dulu peta jawaban lengkap, baru mengambil bagian untuk satu soal.
	 *
	 * <p><b>Hindari di dalam loop.</b> Setiap pemanggilan memuat ulang seluruh peta jawaban
	 * peserta hanya untuk mengambil satu soal — bila perlu banyak soal, muat peta sekali lalu
	 * pakai {@link #ambilHasilUjianMahasiswaDetail(Map, BankSoal)}. Pencarian menyeluruh atas repo
	 * menunjukkan varian ini <b>tidak dipanggil dari mana pun</b> (kode mati); dibiarkan apa
	 * adanya.</p>
	 *
	 * @param jumlahDiujikan      jumlah soal yang diujikan (kapasitas awal peta)
	 * @param ujianPunyaSoalsData daftar id {@link UjianPunyaSoal} sebagai penyaring
	 * @param bankSoal            soal yang dicari jawabannya
	 * @return himpunan id {@link HasilUjianMahasiswaDetail} untuk soal tersebut; kosong bila belum
	 *         dijawab
	 */
	public Set<Long> ambilHasilUjianMahasiswaDetail(int jumlahDiujikan, MyArrayList<Long> ujianPunyaSoalsData,
			BankSoal bankSoal) {
		Map<Long, Set<Long>> hasilUjianMahasiswaDetailsa = ambilHasilUjianMahasiswaDetail(jumlahDiujikan,
				ujianPunyaSoalsData);
		return ambilHasilUjianMahasiswaDetail(hasilUjianMahasiswaDetailsa, bankSoal);
	}

	/**
	 * Menyaring koleksi detail jawaban yang sudah ada di memori, mengambil hanya yang merujuk ke
	 * satu {@link BankSoalDetail} tertentu (satu opsi jawaban).
	 *
	 * <p>Berbeda dari saudara-saudaranya, method ini bekerja atas objek {@code entity} penuh, bukan
	 * atas id, dan sama sekali tidak menyentuh cache maupun database — murni penyaringan
	 * dalam-memori.</p>
	 *
	 * <p><b>Catatan pemeliharaan:</b> parameter {@code session} <b>tidak pernah dipakai</b> di
	 * badan method (sisa dari versi yang dulu melakukan query), dan pencarian menyeluruh atas repo
	 * menunjukkan method ini <b>tidak dipanggil dari mana pun</b> (kode mati). Keduanya dicatat
	 * apa adanya, tidak diubah. Penugasan {@code hasilUjianMahasiswaDetailsa = null} sebelum
	 * {@code return} hanya mengosongkan variabel parameter dan tidak berefek bagi pemanggil.</p>
	 *
	 * @param session                     tidak digunakan; boleh {@code null}
	 * @param hasilUjianMahasiswaDetailsa koleksi detail jawaban yang akan disaring
	 * @param bankSoalDetail              opsi jawaban yang dicari; {@code null} atau tanpa id
	 *                                    menghasilkan daftar kosong
	 * @return daftar detail yang merujuk ke opsi tersebut; kosong bila tidak ada yang cocok
	 */
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

	/**
	 * Berapa kali peserta sudah mengikuti sesi ujian ini.
	 *
	 * <p>Dibandingkan dengan {@code pertemuanPunyaUjian.getJumlahBolehIkut()} oleh
	 * {@code PertemuanPunyaUjianHelper} untuk menentukan apakah tombol "Ikut Ujian" masih boleh
	 * ditekan, dan ditampilkan ke pengguna sebagai "sudah ikut N kali".</p>
	 *
	 * <p><b>Perkiraan untuk data lama.</b> Bila kolomnya {@code null} (baris dari sebelum fitur
	 * ini ada), nilainya <i>ditaksir</i>: {@code 1} bila {@link #getLamaPengerjaan()} terisi —
	 * artinya peserta jelas pernah mengerjakan — dan {@code 0} bila tidak. Perkiraan ini
	 * <b>tidak</b> ditulis balik ke field, jadi hanya berlaku saat dibaca. Perhatikan bahwa
	 * pemanggilan {@code getLamaPengerjaan()} di jalur ini ikut membawa efek samping getter
	 * tersebut.</p>
	 *
	 * @return cacah keikutsertaan; tidak pernah {@code null}
	 */
	public Integer getJumlahIkut() {
		return jumlahIkut == null ? (getLamaPengerjaan() != null ? 1 : 0) : jumlahIkut;
	}

	/**
	 * Menyetel cacah keikutsertaan. Dinaikkan satu oleh {@code ProsesUjianHelper} setiap peserta
	 * memulai sesi, disetel {@code 0} saat baris dibuat
	 * {@link #ambilByKey(PertemuanPunyaUjian, Mahasiswa, BiodataCalonMahasiswa, Siswa, CalonSiswa)},
	 * dan dapat disunting manual pengawas lewat dialog di {@code HasilUjianMahasiswaHelper}.
	 *
	 * @param jumlahIkut cacah keikutsertaan baru
	 */
	public void setJumlahIkut(Integer jumlahIkut) {
		this.jumlahIkut = jumlahIkut;
	}

	/**
	 * Membuka kembali lembar ujian yang belum selesai begitu peserta login ulang.
	 *
	 * <p>Dipanggil {@code MainAction}/{@code MainAction2} saat membangun halaman utama untuk
	 * pengguna berperan Mahasiswa atau Siswa. Id hasil ujian tidak berasal dari input pengguna
	 * melainkan dari cache berkas milik peserta itu sendiri
	 * ({@code mahasiswa.retreive("hasilUjianMahasiswa")}) yang ditulis server saat ujian dimulai —
	 * itulah sebabnya argumennya bertipe {@link String} dan boleh kosong.</p>
	 *
	 * <h4>Syarat tampil</h4>
	 * <p>Lembar ujian hanya dimunculkan bila SEMUA terpenuhi: baris hasil ujian ditemukan, sesi
	 * ujiannya menyalakan {@code getOtomatisMunculKetikaBelumSelesai()}, dan waktu sekarang masih
	 * berada di dalam jendela ujian ({@code mulaiUjian} sudah lewat atau tidak diset, dan
	 * {@code sampaiUjian} belum lewat atau tidak diset). Bila tidak, method diam saja.</p>
	 *
	 * <h4>Yang ditampilkan</h4>
	 * <p>{@code ProsesUjianHelper.tampil(...)} membuka lembar ujian; ketika peserta selesai,
	 * {@link EventListener} yang dioper akan membuka {@link MyWindow} modal berisi rekap hasil —
	 * {@code TampilanUjianCalonMahasiswa} untuk pendaftar PMB, atau
	 * {@code PertemuanPunyaUjianHelper.display(...)} untuk peserta lainnya.</p>
	 *
	 * <p><b>Catatan keamanan (bukan celah yang bisa dieksploitasi hari ini, tapi rapuh):</b>
	 * method ini <b>tidak memverifikasi kepemilikan</b> — id apa pun yang dioper akan dimuat dan
	 * ditampilkan, termasuk milik peserta lain. Perlindungannya semata-mata terletak pada fakta
	 * bahwa satu-satunya pemanggil mengambil id dari cache milik pengguna yang sedang login. Bila
	 * suatu saat ada pemanggil baru yang mengambil id dari parameter permintaan HTTP, ini menjadi
	 * celah IDOR. Menambahkan pemeriksaan "peserta pada baris ini == pengguna login" akan menutup
	 * risiko itu.</p>
	 *
	 * <p>Seluruh badan dibungkus {@code try/catch}; kegagalan (termasuk
	 * {@code NumberFormatException} untuk string bukan angka) ditelan dan dicatat ke audit error
	 * agar halaman utama tetap terbuka. Method juga menulis baris diagnostik ke
	 * {@code System.out}.</p>
	 *
	 * @param strHasilUjianMahasiswa id baris hasil ujian dalam bentuk teks; {@code null}/kosong
	 *                               berarti tidak ada ujian tertunda dan method tidak melakukan
	 *                               apa-apa
	 */
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

										/**
										 * Dipanggil ZK setelah peserta menuntaskan lembar ujian:
										 * membuat {@link MyWindow} modal 99%&times;99% berisi rekap
										 * hasil, lalu menempelkannya ke akar halaman aktif.
										 * Untuk pendaftar PMB dipakai
										 * {@code TampilanUjianCalonMahasiswa}; untuk peserta lain
										 * {@code PertemuanPunyaUjianHelper.display(...)} atas
										 * pertemuan yang bersangkutan.
										 *
										 * @param arg0 event pemicu dari ZK; tidak dipakai
										 * @throws Exception bila pembangunan komponen ZK gagal —
										 *         ditangkap oleh {@code try/catch} pembungkus di
										 *         {@link HasilUjianMahasiswa#tampilkanUjianKembali(String)}
										 */
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
							// Argumen terakhir `false` = jangan paksa mulai ulang; lembar ujian
							// dilanjutkan dari posisi terakhir peserta.

						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/HasilUjianMahasiswa.java:1011");
		}
	}

	/**
	 * Apakah peserta masih diizinkan masuk kembali untuk <b>melengkapi jawaban</b> pada sesi ujian
	 * yang sama.
	 *
	 * <p>Dipakai bersama {@link #getTelahIkutUjian()} oleh {@code PertemuanPunyaUjianHelper}:
	 * peserta yang sudah ikut ujian tetapi tidak diberi izin melengkapi akan melihat lembar ujian
	 * terkunci. Disetel {@code false} oleh {@code ProsesUjianHelper} saat peserta menambah
	 * keikutsertaan, dan bisa dicentang manual pengawas lewat rekap hasil ujian.</p>
	 *
	 * @return {@code true} bila boleh melengkapi jawaban; {@code null} dinormalkan menjadi
	 *         {@code false}
	 */
	public Boolean getLengkapiJawaban() {
		return lengkapiJawaban == null ? false : lengkapiJawaban;
	}

	/**
	 * Menyetel izin melengkapi jawaban.
	 *
	 * @param lengkapiJawaban {@code true} bila peserta boleh masuk kembali menuntaskan jawaban
	 */
	public void setLengkapiJawaban(Boolean lengkapiJawaban) {
		this.lengkapiJawaban = lengkapiJawaban;
	}

	/**
	 * Membangun kunci unik yang mengidentifikasi pasangan <i>satu sesi ujian &times; satu
	 * peserta</i>.
	 *
	 * <p>Bentuknya {@code <pertemuanPunyaUjianId>_<awalanJenis>_<pesertaId>} dengan awalan yang
	 * membedakan keempat jenis peserta: {@code mhs_} (mahasiswa), {@code cal_mhs_} (pendaftar
	 * PMB), {@code siswa_} (siswa sekolah), {@code cal_siswa_} (calon siswa). Kunci ini disimpan
	 * di kolom {@code keyhasil} yang ber-constraint unik, sekaligus menjadi kunci cache cepat
	 * {@link GeneralValueObject#ambilDataLangsung(Class, String)}.</p>
	 *
	 * <p><b>Urutan pemeriksaan menentukan.</b> Bila lebih dari satu peserta ikut terisi — kondisi
	 * yang seharusnya tidak terjadi dan tidak dijaga oleh constraint database — hanya yang
	 * pertama cocok menurut urutan mahasiswa &rarr; pendaftar PMB &rarr; siswa &rarr; calon siswa
	 * yang dipakai; sisanya diabaikan diam-diam.</p>
	 *
	 * @param pertemuanPunyaUjian   sesi ujian; {@code null} atau tanpa id membuat kunci gagal
	 *                              dibentuk
	 * @param mahasiswa             peserta mahasiswa, atau {@code null}
	 * @param biodataCalonMahasiswa peserta pendaftar PMB, atau {@code null}
	 * @param siswa                 peserta siswa sekolah, atau {@code null}
	 * @param calonSiswa            peserta calon siswa, atau {@code null}
	 * @return kunci unik, atau {@code null} bila sesi ujian tidak valid atau keempat peserta
	 *         bernilai {@code null}
	 */
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

	/**
	 * <b>Get-or-create</b> baris hasil ujian untuk satu peserta pada satu sesi ujian — pintu masuk
	 * utama seluruh modul ujian.
	 *
	 * <p><b>Namanya menipu: method ini MENULIS ke database.</b> Meski diawali "ambil", ia membuat
	 * dan menyimpan baris baru bila belum ada, dan melengkapi {@code keyhasil} pada baris lama
	 * yang belum punya. Jangan dipakai di jalur yang harus bebas efek samping (laporan read-only,
	 * ekspor, dsb).</p>
	 *
	 * <h4>Empat tahap pencarian</h4>
	 * <ol>
	 *   <li><b>Cache cepat.</b> {@link GeneralValueObject#ambilDataLangsung(Class, String)} dengan
	 *       kunci hasil {@link #genKey(PertemuanPunyaUjian, Mahasiswa, BiodataCalonMahasiswa,
	 *       Siswa, CalonSiswa)}. Bila kena, langsung dikembalikan tanpa menyentuh database.</li>
	 *   <li><b>Query berdasarkan {@code keyhasil}.</b> Jalur normal untuk baris yang sudah punya
	 *       kunci.</li>
	 *   <li><b>Query warisan.</b> Untuk baris lama yang {@code keyhasil}-nya masih {@code null},
	 *       dicari lewat {@link HasilUjianMahasiswaDetail}: detail mana pun yang punya jawaban
	 *       (ada {@code bankSoalDetail} atau teks jawaban tidak kosong) pada sesi ujian ini dan
	 *       peserta yang cocok, diambil satu ({@code setMaxResults(1)}). Peserta yang
	 *       {@code null} diterjemahkan menjadi {@code sqlRestriction("true")} sehingga kriterianya
	 *       tidak membatasi apa-apa.</li>
	 *   <li><b>Buat baru.</b> Bila tetap tidak ketemu, dibuat instance baru berisi keempat relasi
	 *       peserta, sesi ujian, {@code totalNilai = 0.0}, {@code jumlahIkut = 0}, lalu
	 *       {@code save} dalam transaksi tersendiri.</li>
	 * </ol>
	 * <p>Bila tahap 3 menemukan baris tanpa {@code keyhasil}, kuncinya diisi dan baris di-{@code
	 * update} dalam transaksi tersendiri. Terakhir hasilnya dimasukkan ke cache cepat lewat
	 * {@link GeneralValueObject#masukkanDataLangsung(Class, GeneralValueObject, String)}.</p>
	 *
	 * <h4>Kuirk pengelolaan session &amp; transaksi</h4>
	 * <ul>
	 *   <li>Memakai {@code HibernateUtil.currentNativeSession()} — session milik thread saat ini —
	 *       lalu <b>menutupnya</b> ({@code disconnect} + {@code close}) dan memanggil
	 *       {@code HibernateUtil.closeSession()} di akhir. Pemanggil yang masih memegang cursor
	 *       terbuka atas session yang sama akan terdampak; {@code HasilUjianMahasiswaHelper}
	 *       secara eksplisit membuang session-nya sebelum memanggil method ini justru karena
	 *       alasan itu.</li>
	 *   <li>Transaksi dibuka dengan {@code session.getTransaction().begin()} dan di-commit tanpa
	 *       blok {@code rollback} pada jalur gagal; kegagalan hanya ditelan {@code try/catch}
	 *       terluar dan dicatat ke audit error.</li>
	 *   <li>{@code masukkanDataLangsung} dipanggil di luar {@code try}, jadi tetap berjalan meski
	 *       terjadi exception — pada kasus itu argumennya bisa bernilai {@code null}.</li>
	 * </ul>
	 *
	 * <p><b>Catatan SQL:</b> {@code Restrictions.sqlRestriction("true")} yang dipakai sebagai
	 * "kriteria kosong" adalah literal konstan, bukan gabungan input pengguna — tidak ada risiko
	 * injeksi di sini.</p>
	 *
	 * @param pertemuanPunyaUjian   sesi ujian yang diikuti
	 * @param mahasiswa             peserta mahasiswa, atau {@code null}
	 * @param biodataCalonMahasiswa peserta pendaftar PMB, atau {@code null}
	 * @param siswa                 peserta siswa sekolah, atau {@code null}
	 * @param calonSiswa            peserta calon siswa, atau {@code null}
	 * @return baris hasil ujian yang sudah ada atau yang baru dibuat; {@code null} bila kunci
	 *         tidak bisa dibentuk (sesi ujian tidak valid atau keempat peserta {@code null})
	 * @see #genKey(PertemuanPunyaUjian, Mahasiswa, BiodataCalonMahasiswa, Siswa, CalonSiswa)
	 */
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

	/**
	 * Kunci unik gabungan sesi ujian &times; peserta — <b>selalu dihitung ulang</b> saat dibaca.
	 *
	 * <p>Getter ini me-resolusi keempat relasi peserta dan relasi sesi ujian (masing-masing lewat
	 * getter-nya, jadi dengan seluruh efek sampingnya), memanggil
	 * {@link #genKey(PertemuanPunyaUjian, Mahasiswa, BiodataCalonMahasiswa, Siswa, CalonSiswa)},
	 * lalu <b>menimpa field {@code keyhasil}</b> dengan hasilnya.</p>
	 *
	 * <p><b>Konsekuensi yang harus disadari.</b> Karena kolomnya {@code unique} dan Hibernate
	 * memanggil getter ini saat dirty-check/flush, mengubah relasi peserta atau sesi ujian pada
	 * baris yang sudah tersimpan akan mengubah nilai kolom uniknya secara diam-diam — dan bila
	 * kunci barunya bentrok dengan baris lain, {@code UPDATE} gagal dengan pelanggaran constraint
	 * di tempat yang tampak tidak berhubungan. Bila keempat peserta kosong atau sesi ujian tidak
	 * valid, {@code genKey} mengembalikan {@code null} sehingga kunci yang sudah tersimpan pun
	 * ikut terhapus. Untuk membaca nilai kolom apa adanya tanpa hitung ulang, ambil langsung dari
	 * hasil query, bukan lewat getter ini.</p>
	 *
	 * @return kunci unik, atau {@code null} bila tidak bisa dibentuk
	 */
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

	/**
	 * Menyetel kunci unik. Dipanggil
	 * {@link #ambilByKey(PertemuanPunyaUjian, Mahasiswa, BiodataCalonMahasiswa, Siswa, CalonSiswa)}
	 * untuk melengkapi baris warisan yang belum punya kunci; nilai yang disetel akan dihitung
	 * ulang oleh {@link #getKeyhasil()} pada pembacaan berikutnya.
	 *
	 * @param keyhasil kunci unik
	 */
	public void setKeyhasil(String keyhasil) {
		this.keyhasil = keyhasil;
	}

	/**
	 * Rincian nilai OBE (<i>Outcome-Based Education</i>) untuk hasil ujian ini, disimpan sebagai
	 * string JSON di kolom bertipe {@code text}.
	 *
	 * <p>Isinya adalah pemetaan capaian pembelajaran ke skor yang disusun
	 * {@code ProsesUjianHelper} dan dibaca kembali oleh {@code NilaiObeAction},
	 * {@code HitungUlangNilaiObeHelper}, serta laporan {@code RekapHasilTugasPerTugasDanUjianObe}.
	 * Struktur JSON-nya sengaja tidak diikat skema di lapisan entity.</p>
	 *
	 * <p><b>Normalisasi:</b> bila kolom {@code null} atau berisi string kosong/spasi, yang
	 * dikembalikan adalah {@code Tugas.JSON} — objek JSON kosong {@code "{}"} — supaya pemanggil
	 * bisa langsung membangun {@code JSONObject} tanpa memeriksa {@code null}. Nilai default ini
	 * <b>tidak</b> ditulis balik ke field, jadi kolom di database tetap kosong.</p>
	 *
	 * @return string JSON rincian nilai OBE; tidak pernah {@code null} maupun kosong
	 */
	@Column(columnDefinition = "text")
	public String getNilaiObe() {
		return nilaiObe == null || nilaiObe.trim().isEmpty() ? Tugas.JSON : nilaiObe;
	}

	/**
	 * Menyetel rincian nilai OBE dalam bentuk string JSON.
	 *
	 * @param nilaiObe string JSON rincian nilai OBE; {@code null}/kosong berarti getter akan
	 *                 mengembalikan objek JSON kosong
	 */
	public void setNilaiObe(String nilaiObe) {
		this.nilaiObe = nilaiObe;
	}
}
