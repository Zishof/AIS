package ais.database.model;

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.envers.Audited;
import org.json.JSONObject;

import ais.database.model.sekolah.GrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.GrupPenilaian;
import ais.database.model.sekolah.JenisItemPenilaianSiswa;
import ais.ui.util.WaktuUtil;

/**
 * Entity <b>tugas mandiri</b> (tugas perorangan) pada sebuah pertemuan e-Learning — satu baris
 * tabel {@code public.tugas_pertemuan} adalah SATU PENUGASAN individu yang diberikan dosen/guru
 * pada satu {@link Pertemuan}.
 *
 * <h3>Peran dalam alur pemberian tugas</h3>
 * <ol>
 *   <li>Dosen membuka tab "Tugas" pada sebuah pertemuan; layar pengelolanya adalah
 *       {@code ais.action.master.helper.TugasMandiriHelper} (judul modul: <b>Tugas Mandiri</b>).
 *       Baris {@code TugasPertemuan} dibuat dengan {@link #setPertemuan(Long)} menunjuk pertemuan
 *       yang sedang dibuka, lalu diisi {@link #setJudultugas(String)} (perintah singkat) dan
 *       {@link #setIsitugas(String)} (uraian panjang, kolom {@code text}).</li>
 *   <li>Jendela pengerjaan dibuka/ditutup lewat {@link #getMulai()} dan {@link #getSelesai()};
 *       prasyarat tambahan agar peserta boleh mengumpulkan diatur lewat
 *       {@link #getSyaratMengumpulkanTugas()} dan {@link #getSyaratAkses()}.</li>
 *   <li>Peserta mengunggah jawaban. <b>Berkas jawaban TIDAK disimpan di entity ini</b> melainkan
 *       di {@code ais.database.model.file.TugasFileContent}, dan seluruh mesinnya
 *       ({@link Tugas#ambilTugasFileContentTotal()}, {@link Tugas#reInitTugasFileContent()},
 *       {@link Tugas#ambilJumlahTugasFileContent()}, indeks JSON pada berkas
 *       {@code tugas_file_content_&lt;id&gt;}) dimiliki kelas induk {@link Tugas}.</li>
 *   <li>Dosen menilai. Nilainya juga <b>tidak</b> menjadi baris tabel tersendiri, melainkan
 *       menumpuk sebagai JSON pada {@link #getKeteranganNilai()} — lihat bagian "kolom JSON" di
 *       bawah.</li>
 * </ol>
 *
 * <h3>Kedudukan dalam hierarki kelas</h3>
 * <p>{@code extends} {@link Tugas} (bukan langsung {@link GeneralValueObject}). {@link Tugas}
 * adalah kelas abstrak "sesuatu yang bisa dikumpulkan berkas oleh peserta"; ia mendeklarasikan
 * hampir semua properti yang diimplementasikan di sini sebagai {@code abstract}
 * ({@code judultugas}, {@code isitugas}, {@code mulai}, {@code selesai}, {@code prosentase},
 * {@code formatNilai}, {@code formatNilais}, {@code keteranganNilai}, {@code mhsYgTidakIkut},
 * {@code mhsBolehUploadUlang}, {@code syaratAkses}, {@code syaratMengumpulkanTugas}, dan trio
 * klasifikasi modul sekolah). Turunan konkret {@link Tugas} hanya ada TIGA:</p>
 * <ul>
 *   <li>{@code TugasPertemuan} — tugas mandiri/perorangan (kelas ini);</li>
 *   <li>{@link TugasKelompok} — tugas berkelompok. <b>Nyaris kembar dengan kelas ini</b>; banyak
 *       method di sini merupakan salinan baris-demi-baris dari sana (dan sebaliknya). Bila
 *       memperbaiki cacat di salah satu, periksa apakah cacat yang sama ada di saudaranya.</li>
 *   <li>{@link Pertemuan} sendiri — pertemuannya ikut menjadi {@link Tugas} agar berkas
 *       "tugas pertemuan bawaan" (kolom {@code judultugas} pada tabel {@code pertemuan}) bisa
 *       memakai mesin berkas yang sama.</li>
 * </ul>
 * <p>Kontrak umum id/{@code equals}/{@code compareTo}/{@link GeneralValueObject#check(Object)}/
 * cache/audit trail tetap milik {@link GeneralValueObject} — lihat Javadoc kelas itu, jangan
 * diulang di sini.</p>
 *
 * <h3>Relasi ke {@link Pertemuan}: satu kolom, TIGA cara baca</h3>
 * <p>Kolom {@code pertemuan} ({@code NOT NULL}) adalah satu-satunya pengait ke induk, tetapi
 * dipetakan/diakses lewat tiga jalur berbeda yang mudah tertukar:</p>
 * <table border="1">
 *   <caption>Tiga akses ke pertemuan induk</caption>
 *   <tr><th>Akses</th><th>Pemetaan</th><th>Sifat</th></tr>
 *   <tr>
 *     <td>{@link #getPertemuan()} / {@link #setPertemuan(Long)}</td>
 *     <td>{@code @Column(name = "pertemuan", nullable = false)} — {@code Long} mentah</td>
 *     <td><b>Satu-satunya jalur TULIS.</b> Murah, tidak menyentuh basis data.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #getPertemuanData()} / {@link #setPertemuanData(Pertemuan)}</td>
 *     <td>{@code @ManyToOne} ke kolom yang SAMA dengan
 *         {@code insertable = false, updatable = false} + {@code @NotFound(IGNORE)}</td>
 *     <td>Baca-saja. Menyetel setternya saja TIDAK akan tersimpan. {@code @NotFound(IGNORE)}
 *         membuat baris yatim (pertemuan sudah dihapus) menghasilkan {@code null}, bukan
 *         exception.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #ambilPertemuan()}</td>
 *     <td>bukan properti persistence</td>
 *     <td>Memuat ulang lewat {@link GeneralValueObject#ambilData(Class, String)} (cache/session
 *         sendiri). <b>Bisa memukul basis data</b>; jangan dipanggil di dalam loop besar.</td>
 *   </tr>
 * </table>
 * <p><b>Arah sebaliknya TIDAK langsung.</b> {@link Pertemuan} tidak memegang koleksi
 * {@code @OneToMany} ke kelas ini. Daftar tugas milik sebuah pertemuan dibangun dari indeks JSON
 * berbasis berkas — {@link Pertemuan#reInitTugasPertemuan(org.hibernate.Session)} men-scan
 * {@code Criteria} sekali lalu menuliskan petanya, dan
 * {@link Pertemuan#ambilTugasPertemuanTotal()} membacanya kembali. Jadi menambah/menghapus baris
 * {@code tugas_pertemuan} tanpa memperbarui indeks itu akan membuat daftar di layar basi.</p>
 * <p>Jejak historisnya masih terlihat: blok {@code @ManyToOne getPertemuan()} versi lama dibiarkan
 * dikomentari tepat di atas {@link #ambilPertemuan()}.</p>
 *
 * <h3>Empat kolom {@code text} berisi JSON — jangan tertukar</h3>
 * <table border="1">
 *   <caption>Kolom JSON pada {@code tugas_pertemuan}</caption>
 *   <tr><th>Properti</th><th>Kolom</th><th>Isi</th></tr>
 *   <tr>
 *     <td>{@link #getFormatNilais()}</td><td>{@code formatnilais}</td>
 *     <td><b>Komponen mana</b> yang dinilai: kunci = id {@link FormatNilai} (Sub-CPMK), nilai =
 *     bobotnya. Sekaligus <b>saklar mode OBE</b>: selama isinya masih literal {@link Tugas#JSON}
 *     ({@code "{}"}), tugas dianggap memakai penilaian standar satu-angka.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #getKeteranganNilai()}</td><td>{@code keterangan_nilai_baru}</td>
 *     <td><b>Nilai peserta.</b> Kunci berbasis peserta:
 *     {@code "&lt;id&gt;_mhs"}, {@code "&lt;id&gt;_siswa"}, {@code "&lt;id&gt;_cal_mhs"}, atau
 *     {@code "&lt;id&gt;_cal_siswa"}, dengan akhiran {@code _nilai} (mode standar),
 *     {@code _nilai_&lt;idFormatNilai&gt;} (mode OBE), dan {@code _ket} (keterangan teks).</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #getKeteranganNilaiLama()}</td><td>{@code keterangannilai}</td>
 *     <td>Isi yang sama versi LAMA, sebelum kolom dipindah. Hanya dibaca sebagai cadangan.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #getNilaiManualJson()}</td><td>{@code nilai_manual_json}</td>
 *     <td>Nilai yang dimasukkan dosen SECARA MANUAL untuk peserta yang tidak mengumpulkan berkas.
 *     Bentuk bersarang:
 *     {@code {"&lt;idMahasiswa&gt;": {"fn_&lt;idFormatNilai&gt;": angka,
 *     "fn_&lt;idFormatNilai&gt;_ket": teks, "paksa": bool}}}.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #getSubCpmkPerPeserta()}</td><td>{@code sub_cpmk_per_peserta}</td>
 *     <td>Untuk tugas remedial/perbaikan: Sub-CPMK mana saja yang wajib diulang peserta tertentu.
 *     Bentuk {@code {"&lt;idMahasiswa&gt;": ["&lt;idFormatNilai&gt;", ...]}}; peserta tanpa entri
 *     dianggap mengerjakan SEMUA Sub-CPMK. Dibaca lewat {@link #ambilSubCpmkPeserta(Long)}.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #getSyaratAkses()}</td><td>{@code syaratakses}</td>
 *     <td>Prasyarat agar peserta boleh MELIHAT tugas ini (mis. harus sudah menonton video/membaca
 *     materi tertentu). Diperiksa oleh {@code ProfileUtil.chekSyarat(...)} dan disusun oleh
 *     {@link Tugas#tampilanSyarat}.</td>
 *   </tr>
 * </table>
 * <p><b>Konsekuensi menyimpan nilai sebagai satu string JSON:</b> nilai per komponen tidak bisa
 * di-query dengan SQL biasa (tidak ada JOIN maupun agregasi di sisi basis data — lihat
 * {@code RekapHasilTugas}, {@code RekapHasilTugasPerTugasDanUjianObe}, dan
 * {@code NilaiObeAction} yang semuanya menyusun ulang kunci secara manual), dan dua penilai yang
 * menyimpan bersamaan saling menimpa nilai SELURUH peserta, bukan hanya barisnya sendiri. Karena
 * itulah {@code TugasMandiriHelper} memanggil {@code session.refresh(tp)} tepat sebelum setiap
 * penulisan JSON — mitigasi, bukan penyelesaian.</p>
 *
 * <h3>Pengelompokan method</h3>
 * <ol>
 *   <li><b>Jejak audit bayangan</b> — {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()}, {@link #onUpdate()}. Lihat catatan khusus di bawah.</li>
 *   <li><b>Identitas</b> — {@link #getId()}, {@link #toString()}, konstruktor.</li>
 *   <li><b>Isi tugas</b> — {@link #getJudultugas()}, {@link #getIsitugas()}.</li>
 *   <li><b>Jadwal</b> — {@link #getMulai()}, {@link #getSelesai()}.</li>
 *   <li><b>Relasi pertemuan</b> — {@link #getPertemuan()}, {@link #getPertemuanData()},
 *       {@link #ambilPertemuan()}.</li>
 *   <li><b>Penilaian</b> — {@link #getFormatNilai()}, {@link #getFormatNilais()},
 *       {@link #getProsentase()}, {@link #getKeteranganNilai()},
 *       {@link #getKeteranganNilaiLama()}, {@link #getNilaiManualJson()},
 *       {@link #getSubCpmkPerPeserta()}, {@link #ambilSubCpmkPeserta(Long)}.</li>
 *   <li><b>Klasifikasi penilaian modul sekolah</b> — {@link #getJenisItemPenilaianSiswa()},
 *       {@link #getGrupPenilaian()}, {@link #getGrupKategoriItemPenilaianSiswa()}.</li>
 *   <li><b>Daftar CSV peserta</b> — {@link #getMhsYgTidakIkut()},
 *       {@link #getMhsBolehUploadUlang()}.</li>
 *   <li><b>Kontrol akses &amp; status</b> — {@link #getSyaratMengumpulkanTugas()},
 *       {@link #getSyaratAkses()}, {@link #getAktif()}.</li>
 * </ol>
 *
 * <h3>Hal non-obvious yang WAJIB diketahui sebelum menyentuh kelas ini</h3>
 * <ul>
 *   <li><b>Tiga getter TIDAK murni — mereka menulis balik ke field yang DIPETAKAN</b>, sehingga
 *       sekadar MEMBACA entity ini dapat menandainya <i>dirty</i> dan (karena
 *       {@code dynamicUpdate = true}) memicu {@code UPDATE} saat flush tanpa ada aksi simpan dari
 *       pengguna: {@link #getMhsYgTidakIkut()}, {@link #getMhsBolehUploadUlang()}, dan
 *       {@link #getAktif()}.</li>
 *   <li><b>Empat getter relasi menulis balik REFERENSI hasil
 *       {@link GeneralValueObject#check(Object)}</b> ke fieldnya:
 *       {@link #getSyaratMengumpulkanTugas()}, {@link #getJenisItemPenilaianSiswa()},
 *       {@link #getGrupKategoriItemPenilaianSiswa()}, {@link #getGrupPenilaian()}. Itu memang pola
 *       standar entity AIS (instance kanonik, bukan proxy), tetapi {@code check()} pada tahap
 *       terakhirnya <b>bisa membuka session Hibernate sendiri</b> dan menutupnya lagi.</li>
 *   <li><b>Tidak ada satu pun getter di kelas ini yang menutup session milik pemanggil</b> — file
 *       ini sama sekali tidak menyentuh {@code Session}/{@code HibernateUtil}/{@code Criteria}.
 *       Jalur tak langsung hanya {@link GeneralValueObject#check(Object)} dan
 *       {@link GeneralValueObject#ambilData(Class, String)} (dipakai {@link #ambilPertemuan()}),
 *       yang keduanya mengurus session sendiri di blok {@code finally}.</li>
 *   <li><b>Tidak ada getter destruktif</b> (yang mengosongkan/menghapus state setelah dibaca) di
 *       kelas ini.</li>
 *   <li><b>Setter {@link #setOleh(String)} dan {@link #setOlehId(String)} diam-diam menolak nilai
 *       kosong</b> — nilai lama tidak akan pernah bisa dibersihkan lewat setter.</li>
 *   <li>Kelas ini mendeklarasikan ULANG field {@code id}, {@code oleh}, {@code olehId},
 *       {@code tanggal_dirubah} beserta {@link #onUpdate()} yang sudah ada identik di
 *       {@link GeneralValueObject}. Itu <b>bukan bug</b>: {@link GeneralValueObject} adalah POJO
 *       abstrak biasa — bukan {@code @Entity} maupun {@code @MappedSuperclass} — sehingga
 *       Hibernate TIDAK memetakan propertinya, dan setiap entity konkret harus mendeklarasikan
 *       sendiri kolom-kolom itu agar tersimpan.</li>
 *   <li>Tugas yang belum berjudul otomatis dianggap tidak ada: {@link #getAktif()} dan beberapa
 *       query SQL native ({@code LinimasaApi}, {@code TampilanELearningAction}) sama-sama memakai
 *       predikat "judultugas terisi" sebagai penanda keberadaan tugas.</li>
 * </ul>
 *
 * @see Tugas
 * @see TugasKelompok
 * @see Pertemuan
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "tugas_pertemuan")
public class TugasPertemuan extends Tugas {

	/**
	 * Versi serialisasi Java. Nilainya dibangkitkan Hibernate Tools saat kelas ini dibuat dan
	 * tidak boleh diubah agar object yang tersimpan di session HTTP/cluster lama tetap terbaca.
	 */
	private static final long serialVersionUID = 8996611659323620994L;

	/** Kunci utama (kolom {@code id}, {@code IDENTITY}). @see #getId() */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini. @see #getOleh() */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini. @see #getOlehId() */
	private String olehId;

	/**
	 * Id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah tersentuh audit
	 * @see #setOlehId(String)
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Setel id pengguna pengubah terakhir.
	 *
	 * <p><b>Menolak nilai kosong secara diam-diam:</b> {@code null} maupun string yang hanya berisi
	 * spasi diabaikan dan nilai lama dipertahankan. Akibatnya jejak audit tidak pernah bisa
	 * dikosongkan lewat setter ini — perilaku sengaja, agar {@code AuditTimestampInterceptor} yang
	 * berjalan tanpa konteks pengguna tidak menghapus jejak yang sudah ada.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong diabaikan
	 * @see #getOlehId()
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Setel nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong ditolak diam-diam.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan
	 * @see #getOleh()
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah tersentuh audit
	 * @see #setOleh(String)
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait lifecycle JPA yang dijalankan tepat sebelum {@code UPDATE} baris ini.
	 *
	 * <p>Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)}, yang mengisi
	 * {@link #setOleh(String)}/{@link #setOlehId(String)} dari pengguna login dan menyegarkan
	 * {@link #setTanggal_dirubah(Date)}. Jangan dipanggil manual — Hibernate/JPA yang
	 * memanggilnya.</p>
	 *
	 * <p>Perhatikan bahwa hanya ada kait {@code @PreUpdate}; pengisian awal saat {@code INSERT}
	 * bergantung pada nilai bawaan field dan pada pemanggil yang menyetel {@code oleh} sendiri.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir. Diinisialisasi ke waktu server saat object dibuat sehingga baris
	 * baru selalu punya nilai, lalu disegarkan oleh {@link #onUpdate()}.
	 *
	 * @see #getTanggal_dirubah()
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Setel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan; {@code null} diizinkan
	 * @see #getTanggal_dirubah()
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Waktu perubahan terakhir baris ini (kolom {@code tanggal_dirubah}, presisi timestamp).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk object yang baru dibuat
	 * @see #onUpdate()
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Uraian panjang perintah tugas (kolom {@code isitugas}, {@code text}). @see #getIsitugas() */
	private String isitugas = "";
	/** Judul/perintah singkat tugas. Kosong berarti tugas dianggap belum ada. @see #getJudultugas() */
	private String judultugas = "";

	/** Awal jendela pengerjaan. @see #getMulai() */
	private Date mulai;
	/** Batas akhir (deadline) pengumpulan. @see #getSelesai() */
	private Date selesai;

	/** Komponen nilai tunggal pada mode penilaian STANDAR. @see #getFormatNilai() */
	private FormatNilai formatNilai;
	/** JSON komponen nilai mode OBE, sekaligus saklar mode. @see #getFormatNilais() */
	private String formatNilais;
	/** Bobot tugas ini terhadap nilai akhir, dalam persen. @see #getProsentase() */
	private Double prosentase;

	/** Prasyarat agar peserta boleh mengumpulkan. @see #getSyaratMengumpulkanTugas() */
	private SyaratUjian syaratMengumpulkanTugas;

	/** Id {@link Pertemuan} induk — satu-satunya jalur TULIS relasi. @see #getPertemuan() */
	private Long pertemuan;
	/** Relasi baca-saja ke {@link Pertemuan} induk atas kolom yang sama. @see #getPertemuanData() */
	private Pertemuan pertemuanData;

	/** CSV id peserta yang dikecualikan dari tugas ini. @see #getMhsYgTidakIkut() */
	private String mhsYgTidakIkut;

	/** CSV id peserta yang diizinkan mengunggah ulang jawaban. @see #getMhsBolehUploadUlang() */
	private String mhsBolehUploadUlang;

	/** JSON nilai manual untuk peserta yang tidak mengumpulkan. @see #getNilaiManualJson() */
	private String nilaiManualJson;

	/** JSON Sub-CPMK yang wajib diulang per peserta. @see #getSubCpmkPerPeserta() */
	private String subCpmkPerPeserta;

	/** JSON prasyarat agar tugas ini boleh DILIHAT peserta. @see #getSyaratAkses() */
	private String syaratAkses;

	/** Klasifikasi jenis item penilaian (modul sekolah). @see #getJenisItemPenilaianSiswa() */
	private JenisItemPenilaianSiswa jenisItemPenilaianSiswa;

	/** Grup penilaian (modul sekolah). @see #getGrupPenilaian() */
	private GrupPenilaian grupPenilaian;

	/** Grup kategori item penilaian (modul sekolah). @see #getGrupKategoriItemPenilaianSiswa() */
	private GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa;

	/** JSON nilai peserta pada kolom BARU {@code keterangan_nilai_baru}. @see #getKeteranganNilai() */
	private String keteranganNilai;

	/** JSON nilai peserta pada kolom LAMA {@code keterangannilai}. @see #getKeteranganNilaiLama() */
	private String keteranganNilaiLama;

	/** Penanda aktif turunan; selalu ditulis ulang oleh getternya. @see #getAktif() */
	private Boolean aktif;

	/**
	 * Representasi teks singkat untuk log dan komponen ZK.
	 *
	 * <p>Membaca field {@code judultugas} LANGSUNG (bukan lewat {@link #getJudultugas()}), sehingga
	 * hasilnya bisa memuat spasi tepi atau literal {@code null} bila judul belum diisi — berbeda
	 * dengan {@link #getJudultugas()} yang menormalkan keduanya.</p>
	 *
	 * @return gabungan {@code "&lt;id&gt;-&lt;judultugas&gt;"}
	 */
	public String toString() {
		return id + "-" + judultugas;
	}

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate dan pengikatan data ZK.
	 *
	 * <p>Baris yang baru dibuat belum punya pertemuan induk; pemanggil WAJIB memanggil
	 * {@link #setPertemuan(Long)} sebelum menyimpan karena kolomnya {@code NOT NULL}.</p>
	 */
	public TugasPertemuan() {
	}

	/**
	 * Kunci utama baris ini.
	 *
	 * <p>Kolom {@code id} bertipe {@code IDENTITY} dan ditandai {@code insertable = false} — nilai
	 * dibangkitkan basis data, bukan aplikasi. Id ini juga menjadi kunci indeks berkas jawaban
	 * ({@code tugas_file_content_&lt;id&gt;}) pada {@link Tugas}.</p>
	 *
	 * @return id baris; {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Setel kunci utama. Hanya dipakai Hibernate dan jalur pencarian/salin data.
	 *
	 * @param id kunci utama
	 * @see #getId()
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Prasyarat yang harus dipenuhi peserta agar boleh MENGUMPULKAN tugas ini.
	 *
	 * <p>Relasi {@code @ManyToOne} lazy ke {@link SyaratUjian} (kolom
	 * {@code syarat_mengumpulkan_tugas}, boleh {@code null} = tanpa prasyarat) dengan
	 * {@code cascade = PERSIST, MERGE} — object yang diset di sini ikut terbawa ke dalam graf
	 * persistence saat tugas disimpan.</p>
	 *
	 * <p><b>Getter ini tidak murni:</b> hasil {@link GeneralValueObject#check(Object)} ditulis balik
	 * ke field agar proxy lazy yang sudah <i>detached</i> tidak meledak di pemanggil. Instance yang
	 * dikembalikan bisa berbeda dari yang tersimpan sebelumnya (instance kanonik dari
	 * {@code EntityIdentityMap} atau hasil reload lewat session baru).</p>
	 *
	 * @return prasyarat pengumpulan, atau {@code null} bila tidak ada
	 * @see GeneralValueObject#check(Object)
	 * @see #getSyaratAkses()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "syarat_mengumpulkan_tugas", nullable = true)
	public SyaratUjian getSyaratMengumpulkanTugas() {
		syaratMengumpulkanTugas = check(syaratMengumpulkanTugas);
		return syaratMengumpulkanTugas;
	}

	/**
	 * Setel prasyarat pengumpulan tugas.
	 *
	 * @param syaratMengumpulkanTugas prasyarat; {@code null} berarti tanpa prasyarat
	 * @see #getSyaratMengumpulkanTugas()
	 */
	public void setSyaratMengumpulkanTugas(SyaratUjian syaratMengumpulkanTugas) {
		this.syaratMengumpulkanTugas = syaratMengumpulkanTugas;
	}

	/**
	 * Komponen nilai TUNGGAL yang dipakai pada mode penilaian standar (non-OBE).
	 *
	 * <p>Relasi {@code @ManyToOne} ke {@link FormatNilai} (kolom {@code format_nilai}, boleh
	 * {@code null}) dengan {@code FetchMode.SELECT} sehingga dimuat lewat query terpisah, bukan
	 * JOIN. Pada mode OBE properti ini tidak dipakai — daftar komponennya ada di
	 * {@link #getFormatNilais()}.</p>
	 *
	 * <p>Berbeda dari empat relasi lain di kelas ini, getter ini <b>TIDAK</b> memanggil
	 * {@link GeneralValueObject#check(Object)}; membacanya pada object <i>detached</i> dapat
	 * melempar {@code LazyInitializationException} (relasi ini memang tidak lazy, tetapi proxy
	 * masih mungkin muncul lewat cascade dari sisi lain).</p>
	 *
	 * @return komponen nilai standar, atau {@code null}
	 * @see #getFormatNilais()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "format_nilai", nullable = true)
	public FormatNilai getFormatNilai() {
		return formatNilai;
	}

	/**
	 * Setel komponen nilai standar.
	 *
	 * @param formatNilai komponen nilai; {@code null} diizinkan
	 * @see #getFormatNilai()
	 */
	public void setFormatNilai(FormatNilai formatNilai) {
		this.formatNilai = formatNilai;
	}

	/**
	 * Bobot tugas ini terhadap nilai akhir, dalam persen.
	 *
	 * <p><b>Nilai kosong dibaca sebagai 100.0</b>, bukan {@code null} dan bukan 0 — tugas yang
	 * belum pernah diatur bobotnya dianggap berbobot penuh. Normalisasi hanya terjadi pada
	 * pembacaan; field tetap {@code null} sehingga kolom di basis data tidak ikut terisi.</p>
	 *
	 * @return bobot dalam persen; {@code 100.0} bila belum diatur, tidak pernah {@code null}
	 */
	public Double getProsentase() {
		return prosentase == null ? 100.0 : prosentase;
	}

	/**
	 * Setel bobot tugas dalam persen.
	 *
	 * @param prosentase bobot; {@code null} berarti kembali ke bawaan 100%
	 * @see #getProsentase()
	 */
	public void setProsentase(Double prosentase) {
		this.prosentase = prosentase;
	}

	/**
	 * Judul/perintah singkat tugas — sekaligus penanda keberadaan tugas.
	 *
	 * <p>Hasilnya dinormalkan: {@code null} menjadi string kosong dan spasi tepi dipangkas. Judul
	 * kosong berarti "tidak ada tugas pada pertemuan ini" — dipakai {@link #getAktif()} dan juga
	 * oleh query SQL native di {@code LinimasaApi} serta {@code TampilanELearningAction}
	 * ({@code judultugas is not null and judultugas != ''}).</p>
	 *
	 * <p>Normalisasi hanya di sisi baca; field dan kolomnya tetap menyimpan teks aslinya.</p>
	 *
	 * @return judul tugas terpangkas; tidak pernah {@code null}
	 * @see #getAktif()
	 */
	public String getJudultugas() {
		return judultugas == null ? "" : judultugas.trim();
	}

	/**
	 * Setel judul/perintah singkat tugas.
	 *
	 * <p>Menyetel string kosong secara efektif "menghapus" tugas dari sudut pandang layar peserta
	 * karena {@link #getAktif()} akan bernilai {@code false} — baris tabelnya sendiri tetap ada.</p>
	 *
	 * @param judultugas judul tugas
	 * @see #getJudultugas()
	 */
	public void setJudultugas(String judultugas) {
		this.judultugas = judultugas;
	}

	/**
	 * Setel uraian panjang perintah tugas.
	 *
	 * @param isitugas uraian tugas (boleh memuat markup yang dirender di sisi UI)
	 * @see #getIsitugas()
	 */
	public void setIsitugas(String isitugas) {
		this.isitugas = isitugas;
	}

	/**
	 * Uraian panjang perintah tugas (kolom {@code isitugas} bertipe {@code text}).
	 *
	 * <p>Berbeda dari {@link #getJudultugas()}, getter ini TIDAK menormalkan apa pun: nilainya
	 * dikembalikan apa adanya, dan bisa {@code null} bila baris lama dimuat dari basis data
	 * (inisialisasi {@code ""} pada field hanya berlaku untuk object yang baru dibuat).</p>
	 *
	 * @return uraian tugas; bisa {@code null} untuk baris lama
	 */
	@Column(name = "isitugas", columnDefinition = "text")
	public String getIsitugas() {
		return isitugas;
	}

	/**
	 * Awal jendela pengerjaan tugas.
	 *
	 * <p><b>Sengaja tidak diberi nilai bawaan.</b> Berbeda dengan beberapa entity lain di paket ini
	 * yang mengisi tanggal berjalan bila {@code null}, tugas boleh dijadwalkan mundur atau maju
	 * bebas oleh dosen — lihat komentar di dalam badan method. Pemanggil yang butuh tanggal jatuh
	 * biasanya memakai tanggal {@link Pertemuan} induk sebagai cadangan (lihat query
	 * {@code LinimasaApi}: {@code case when aa.mulai is null then bb.tanggal else aa.mulai end}).</p>
	 *
	 * @return waktu mulai, atau {@code null} bila tidak dibatasi
	 * @see #getSelesai()
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getMulai() {
		// Jangan mengisi waktu saat ini secara implisit. Dosen menentukan tanggal
		// tugas sendiri, termasuk untuk SP, remedial, dan perubahan hari kuliah.
		return mulai;
	}

	/**
	 * Setel awal jendela pengerjaan.
	 *
	 * @param mulai waktu mulai; {@code null} berarti tidak dibatasi
	 * @see #getMulai()
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Batas akhir (deadline) pengumpulan tugas.
	 *
	 * <p>Sama seperti {@link #getMulai()}, tidak ada nilai bawaan implisit; {@code null} berarti
	 * pengumpulan tidak dibatasi waktu. Penegakan deadline dilakukan di lapisan UI/servis
	 * ({@code TugasMandiriHelper}, {@code LinimasaApi}), bukan di entity ini.</p>
	 *
	 * @return batas akhir pengumpulan, atau {@code null}
	 * @see #getMulai()
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getSelesai() {
		return selesai;
	}

	/**
	 * Setel batas akhir pengumpulan.
	 *
	 * @param selesai batas akhir; {@code null} berarti tanpa batas
	 * @see #getSelesai()
	 */
	public void setSelesai(Date selesai) {
		this.selesai = selesai;
	}

//	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
//	@Fetch(FetchMode.SELECT)
//	@JoinColumn(name = "pertemuan", nullable = false)
//	public Pertemuan getPertemuan() {
//		return pertemuan;
//	}
//
//	public void setPertemuan(Pertemuan pertemuan) {
//		this.pertemuan = pertemuan;
//	}

	/**
	 * Muat object {@link Pertemuan} induk berdasarkan id pada {@link #getPertemuan()}.
	 *
	 * <p><b>Bukan properti persistence</b> (namanya sengaja tidak diawali {@code get} agar Hibernate
	 * mengabaikannya) dan <b>bisa memukul basis data</b>: {@link GeneralValueObject#ambilData(Class,
	 * String)} akan mencoba cache lebih dulu, lalu membuka session sendiri bila perlu dan
	 * menutupnya kembali. Hindari memanggilnya di dalam loop besar — untuk kebutuhan di dalam satu
	 * transaksi yang sudah punya session, {@link #getPertemuanData()} jauh lebih murah.</p>
	 *
	 * <p>Dipakai antara lain oleh {@code TugasMandiriHelper} (menentukan pertemuan yang sedang
	 * dibuka) dan {@code PertemuanPunyaDiskusiHelper} (mencari pertemuan induk dari sebuah
	 * {@link Tugas} generik lewat cabang {@code instanceof}).</p>
	 *
	 * @return pertemuan induk, atau {@code null} bila {@link #getPertemuan()} kosong atau barisnya
	 *         sudah tidak ada
	 * @see #getPertemuanData()
	 * @see GeneralValueObject#ambilData(Class, String)
	 */
	public Pertemuan ambilPertemuan() {
		return getPertemuan() == null ? null
				: (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, getPertemuan().toString());
	}

	/**
	 * Id {@link Pertemuan} induk sebagai {@code Long} mentah (kolom {@code pertemuan},
	 * {@code NOT NULL}).
	 *
	 * <p>Inilah satu-satunya jalur TULIS ke relasi induk; {@link #getPertemuanData()} dipetakan ke
	 * kolom yang sama tetapi baca-saja. Getter ini murah dan tidak menyentuh basis data.</p>
	 *
	 * @return id pertemuan induk; {@code null} hanya pada object yang belum lengkap
	 * @see #setPertemuan(Long)
	 * @see #ambilPertemuan()
	 */
	@Column(name = "pertemuan", nullable = false)
	public Long getPertemuan() {
		return pertemuan;
	}

	/**
	 * Setel id {@link Pertemuan} induk.
	 *
	 * <p>WAJIB diisi sebelum menyimpan (kolomnya {@code NOT NULL}). Menyetel
	 * {@link #setPertemuanData(Pertemuan)} saja tidak cukup — relasi object-nya baca-saja.
	 * Perhatikan pula bahwa {@link Pertemuan} tidak memelihara koleksi balik ke tugas; setelah
	 * menambah/memindahkan baris, indeks berkas milik pertemuan perlu disegarkan lewat
	 * {@link Pertemuan#reInitTugasPertemuan(org.hibernate.Session)} agar daftar di layar tidak
	 * basi.</p>
	 *
	 * @param pertemuan id pertemuan induk
	 * @see #getPertemuan()
	 */
	public void setPertemuan(Long pertemuan) {
		this.pertemuan = pertemuan;
	}

	/**
	 * Daftar id peserta yang DIKECUALIKAN dari tugas ini (dianggap tidak wajib mengerjakan).
	 *
	 * <p><b>Bentuk data:</b> CSV yang dibungkus koma di kedua ujungnya, mis.
	 * {@code ",12,88,301,"}. Bungkus koma itu penting karena seluruh pemanggil memeriksa
	 * keanggotaan dengan {@code getMhsYgTidakIkut().contains("," + id + ",")} — tanpa bungkus,
	 * id {@code 1} akan cocok dengan {@code 12}. Lihat {@code RekapHasilTugas},
	 * {@code RekapNilaiView}, {@code PertemuanPunyaDiskusiHelper}.</p>
	 *
	 * <p><b>PERINGATAN — getter ini TIDAK murni.</b> Ia menormalkan nilai lalu <b>menulis hasilnya
	 * kembali ke field yang dipetakan</b>: menambahkan bungkus koma, meruntuhkan koma ganda (tiga
	 * kali {@code replaceAll(",,", ",")} berurutan, jadi runtutan koma yang sangat panjang bisa
	 * tidak habis), dan memaksa hasil menjadi string kosong bila hanya berisi koma. Karena field
	 * ini properti persistence dan kelas memakai {@code dynamicUpdate = true}, <b>sekadar membaca
	 * getter ini dapat menandai entity dirty dan memicu {@code UPDATE}</b> saat flush, tanpa aksi
	 * simpan dari pengguna.</p>
	 *
	 * <p>Pemeriksaan {@code == null} pada baris {@code return} sudah tidak mungkin bernilai benar
	 * karena field dipastikan non-null beberapa baris di atasnya — sisa kode lama, dibiarkan apa
	 * adanya.</p>
	 *
	 * @return CSV id peserta berbungkus koma, atau string kosong; tidak pernah {@code null}
	 * @see #getMhsBolehUploadUlang()
	 */
	@Column(columnDefinition = "text")
	public String getMhsYgTidakIkut() {
		mhsYgTidakIkut = (mhsYgTidakIkut == null || mhsYgTidakIkut.trim().equalsIgnoreCase(",") ? ""
				: "," + mhsYgTidakIkut.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (mhsYgTidakIkut.equals(",")) {
			mhsYgTidakIkut = "";
		} else if (mhsYgTidakIkut.equals(",,")) {
			mhsYgTidakIkut = "";
		} else if (mhsYgTidakIkut.equals(",,,")) {
			mhsYgTidakIkut = "";
		}
		return mhsYgTidakIkut == null ? "" : mhsYgTidakIkut.trim();
	}

	/**
	 * Setel daftar id peserta yang dikecualikan.
	 *
	 * <p>Nilai disimpan apa adanya; normalisasi bungkus koma baru terjadi pada pembacaan
	 * berikutnya lewat {@link #getMhsYgTidakIkut()}. Pemanggil di UI biasanya membaca getter,
	 * menyisipkan/menghapus potongan {@code ",<id>,"}, lalu memanggil setter ini.</p>
	 *
	 * @param mhsYgTidakIkut CSV id peserta
	 * @see #getMhsYgTidakIkut()
	 */
	public void setMhsYgTidakIkut(String mhsYgTidakIkut) {
		this.mhsYgTidakIkut = mhsYgTidakIkut;
	}

	/**
	 * Daftar id peserta yang secara khusus DIIZINKAN mengunggah ulang jawaban.
	 *
	 * <p>Bentuk data, cara pemeriksaan, dan <b>ketidakmurnian getter</b> persis sama dengan
	 * {@link #getMhsYgTidakIkut()} — implementasinya salinan baris-demi-baris. Dipakai
	 * {@code TugasMandiriHelper} dan {@code LinimasaApi} untuk memutuskan apakah tombol unggah
	 * masih ditampilkan setelah peserta pernah mengumpulkan.</p>
	 *
	 * @return CSV id peserta berbungkus koma, atau string kosong; tidak pernah {@code null}
	 * @see #getMhsYgTidakIkut()
	 */
	@Column(columnDefinition = "text")
	public String getMhsBolehUploadUlang() {
		mhsBolehUploadUlang = (mhsBolehUploadUlang == null || mhsBolehUploadUlang.trim().equalsIgnoreCase(",") ? ""
				: "," + mhsBolehUploadUlang.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",")
				.replaceAll(",,", ",");

		if (mhsBolehUploadUlang.equals(",")) {
			mhsBolehUploadUlang = "";
		} else if (mhsBolehUploadUlang.equals(",,")) {
			mhsBolehUploadUlang = "";
		} else if (mhsBolehUploadUlang.equals(",,,")) {
			mhsBolehUploadUlang = "";
		}
		return mhsBolehUploadUlang == null ? "" : mhsBolehUploadUlang.trim();
	}

	/**
	 * Setel daftar id peserta yang boleh mengunggah ulang.
	 *
	 * @param mhsBolehUploadUlang CSV id peserta
	 * @see #getMhsBolehUploadUlang()
	 */
	public void setMhsBolehUploadUlang(String mhsBolehUploadUlang) {
		this.mhsBolehUploadUlang = mhsBolehUploadUlang;
	}

	/**
	 * Nilai yang dimasukkan dosen SECARA MANUAL untuk peserta yang tidak mengumpulkan berkas.
	 *
	 * <p><b>Bentuk data</b> (dibaca/ditulis {@code TugasMandiriHelper}): objek JSON bersarang,
	 * kunci luar = id mahasiswa, kunci dalam =
	 * {@code "fn_&lt;idFormatNilai&gt;"} (angka nilai),
	 * {@code "fn_&lt;idFormatNilai&gt;_ket"} (keterangan teks), dan
	 * {@code "paksa"} ({@code boolean} — paksa pakai nilai manual meskipun peserta sebenarnya
	 * mengumpulkan).</p>
	 *
	 * <p>Kosong dinormalkan menjadi {@code "{}"} sehingga getter ini <b>tidak pernah</b>
	 * mengembalikan {@code null} — berbeda dari properti bernama sama pada
	 * {@link PertemuanPunyaUjian#getNilaiManualJson()} yang justru mengembalikan nilai mentah
	 * (bisa {@code null}). Kode yang menangani kedua entity itu bersama-sama harus tetap
	 * memeriksa {@code null}.</p>
	 *
	 * <p>Karena satu string memuat nilai SEMUA peserta, penulisan parsial akan menghapus nilai
	 * peserta lain; itulah sebabnya pemanggil selalu membaca dahulu, menyisipkan satu entri, lalu
	 * menulis ulang seluruh JSON (dan mendahuluinya dengan {@code session.refresh(...)}).</p>
	 *
	 * @return teks JSON nilai manual; {@code "{}"} bila belum ada, tidak pernah {@code null}
	 * @see #getKeteranganNilai()
	 */
	@Column(columnDefinition = "text", name = "nilai_manual_json")
	public String getNilaiManualJson() {
		return nilaiManualJson == null || nilaiManualJson.trim().isEmpty() ? new org.json.JSONObject().toString()
				: nilaiManualJson;
	}

	/**
	 * Setel JSON nilai manual.
	 *
	 * <p>Pemanggil bertanggung jawab menyusun JSON yang utuh untuk SELURUH peserta — lihat
	 * peringatan pada {@link #getNilaiManualJson()}.</p>
	 *
	 * @param nilaiManualJson teks JSON nilai manual; {@code null}/kosong dibaca sebagai {@code "{}"}
	 * @see #getNilaiManualJson()
	 */
	public void setNilaiManualJson(String nilaiManualJson) {
		this.nilaiManualJson = nilaiManualJson;
	}

	/**
	 * Pemetaan Sub-CPMK yang wajib dikerjakan per peserta — dipakai pada tugas remedial/perbaikan.
	 *
	 * <p><b>Bentuk data:</b> {@code {"&lt;idMahasiswa&gt;": ["&lt;idFormatNilai&gt;", ...]}}
	 * (perhatikan: id di dalam array disimpan sebagai STRING, bukan angka). Peserta yang tidak
	 * punya entri dianggap mengerjakan SEMUA Sub-CPMK yang terpilih pada tugas — karena itu
	 * {@code TugasMandiriHelper} justru MENGHAPUS kunci pesertanya ketika semua kotak centang
	 * dicentang.</p>
	 *
	 * <p>Kosong dinormalkan menjadi {@code "{}"}. Untuk membacanya sebagai himpunan id, pakai
	 * {@link #ambilSubCpmkPeserta(Long)}.</p>
	 *
	 * @return teks JSON pemetaan Sub-CPMK per peserta; tidak pernah {@code null}
	 * @see #ambilSubCpmkPeserta(Long)
	 * @see #getFormatNilais()
	 */
	@Column(columnDefinition = "text", name = "sub_cpmk_per_peserta")
	public String getSubCpmkPerPeserta() {
		return subCpmkPerPeserta == null || subCpmkPerPeserta.trim().isEmpty()
				? new org.json.JSONObject().toString()
				: subCpmkPerPeserta;
	}

	/**
	 * Setel pemetaan Sub-CPMK per peserta.
	 *
	 * <p>Seperti kolom JSON lain di kelas ini, isinya memuat SELURUH peserta sekaligus; tulis ulang
	 * JSON utuh, jangan potongan.</p>
	 *
	 * @param subCpmkPerPeserta teks JSON pemetaan; {@code null}/kosong dibaca sebagai {@code "{}"}
	 * @see #getSubCpmkPerPeserta()
	 */
	public void setSubCpmkPerPeserta(String subCpmkPerPeserta) {
		this.subCpmkPerPeserta = subCpmkPerPeserta;
	}

	/** Ambil set FormatNilai-ID Sub-CPMK yang terpilih untuk mahasiswa tertentu.
	 *  Mengembalikan null jika semua Sub-CPMK terpilih (default).
	 *
	 *  <p>Mem-parsing {@link #getSubCpmkPerPeserta()} dan mengubah array string pada kunci
	 *  {@code mahasiswaId} menjadi {@code Set&lt;Long&gt;} berurutan
	 *  ({@code LinkedHashSet}, urutan penyimpanan dipertahankan). Entri yang bukan angka
	 *  dilewati diam-diam, dan <b>seluruh kegagalan parsing dikembalikan sebagai {@code null}</b> —
	 *  artinya JSON yang rusak tidak dapat dibedakan dari "peserta mengerjakan semua Sub-CPMK".</p>
	 *
	 *  <p>Ditandai {@code @Transient} sehingga Hibernate mengabaikannya. Murni baca; tidak
	 *  menyentuh basis data maupun mengubah state. Dipanggil dari
	 *  {@code TugasMandiriHelper} saat membangun kotak centang Sub-CPMK per peserta dan saat
	 *  menentukan komponen nilai mana yang ditampilkan.</p>
	 *
	 *  @param mahasiswaId id peserta; {@code null} menghasilkan {@code null}
	 *  @return himpunan id {@link FormatNilai} yang wajib dikerjakan peserta, atau {@code null}
	 *          bila peserta mengerjakan seluruh Sub-CPMK (juga bila JSON tidak terbaca)
	 *  @see #getSubCpmkPerPeserta() */
	@javax.persistence.Transient
	public java.util.Set<Long> ambilSubCpmkPeserta(Long mahasiswaId) {
		if (mahasiswaId == null) {
			return null;
		}
		try {
			org.json.JSONObject j = new org.json.JSONObject(getSubCpmkPerPeserta());
			String key = mahasiswaId.toString();
			if (j.isNull(key)) {
				return null;
			}
			org.json.JSONArray arr = j.optJSONArray(key);
			if (arr == null) {
				return null;
			}
			java.util.Set<Long> hasil = new java.util.LinkedHashSet<Long>();
			for (int i = 0; i < arr.length(); i++) {
				try {
					hasil.add(Long.parseLong(arr.getString(i)));
				} catch (Exception ex) { /* skip entri tidak valid */ }
			}
			return hasil;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Prasyarat agar tugas ini boleh DILIHAT peserta — teks JSON.
	 *
	 * <p>Berbeda dari {@link #getSyaratMengumpulkanTugas()} yang mengatur hak MENGUMPULKAN,
	 * properti ini mengatur hak MELIHAT: mis. peserta harus sudah menonton video atau membuka
	 * materi tertentu pada pertemuan yang sama. Formulir penyusunnya dibangun oleh
	 * {@link Tugas#tampilanSyarat} dan penegakannya dilakukan {@code ProfileUtil.chekSyarat(...)}
	 * di lapisan tampilan e-Learning.</p>
	 *
	 * <p>Kosong dinormalkan menjadi {@code "{}"} sehingga aman langsung diumpankan ke
	 * {@code new JSONObject(...)} tanpa pemeriksaan {@code null}.</p>
	 *
	 * @return teks JSON prasyarat akses; {@code "{}"} bila tanpa prasyarat, tidak pernah
	 *         {@code null}
	 * @see #getSyaratMengumpulkanTugas()
	 */
	@Column(columnDefinition = "text")
	public String getSyaratAkses() {
		return syaratAkses == null || syaratAkses.trim().isEmpty() ? new JSONObject().toString() : syaratAkses;
	}

	/**
	 * Setel prasyarat akses.
	 *
	 * @param syaratAkses teks JSON prasyarat; {@code null}/kosong dibaca sebagai {@code "{}"}
	 * @see #getSyaratAkses()
	 */
	public void setSyaratAkses(String syaratAkses) {
		this.syaratAkses = syaratAkses;
	}

	/**
	 * Jenis item penilaian (modul SEKOLAH) tempat nilai tugas ini digolongkan.
	 *
	 * <p>Relasi {@code @ManyToOne} lazy ke {@link JenisItemPenilaianSiswa} (kolom
	 * {@code jenis_item_penilaian_siswa}, boleh {@code null}); tidak dipakai pada modul perguruan
	 * tinggi. Bersama {@link #getGrupPenilaian()} dan
	 * {@link #getGrupKategoriItemPenilaianSiswa()}, trio ini menentukan letak tugas dalam struktur
	 * rapor sekolah.</p>
	 *
	 * <p><b>Getter tidak murni:</b> hasil {@link GeneralValueObject#check(Object)} ditulis balik ke
	 * field — lihat catatan pada {@link #getSyaratMengumpulkanTugas()}.</p>
	 *
	 * @return jenis item penilaian, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_item_penilaian_siswa", nullable = true)
	public JenisItemPenilaianSiswa getJenisItemPenilaianSiswa() {
		jenisItemPenilaianSiswa = check(jenisItemPenilaianSiswa);
		return jenisItemPenilaianSiswa;
	}

	/**
	 * Setel jenis item penilaian (modul sekolah).
	 *
	 * @param jenisItemPenilaianSiswa jenis item penilaian; {@code null} diizinkan
	 * @see #getJenisItemPenilaianSiswa()
	 */
	public void setJenisItemPenilaianSiswa(JenisItemPenilaianSiswa jenisItemPenilaianSiswa) {
		this.jenisItemPenilaianSiswa = jenisItemPenilaianSiswa;
	}

	/**
	 * Grup kategori item penilaian (modul SEKOLAH).
	 *
	 * <p>Relasi {@code @ManyToOne} lazy ke {@link GrupKategoriItemPenilaianSiswa} (kolom
	 * {@code grup_kategori_item_penilaian_siswa}, boleh {@code null}). Sama seperti saudaranya,
	 * getter ini menulis balik hasil {@link GeneralValueObject#check(Object)} ke field.</p>
	 *
	 * @return grup kategori item penilaian, atau {@code null}
	 * @see #getJenisItemPenilaianSiswa()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "grup_kategori_item_penilaian_siswa", nullable = true)
	public GrupKategoriItemPenilaianSiswa getGrupKategoriItemPenilaianSiswa() {
		grupKategoriItemPenilaianSiswa = check(grupKategoriItemPenilaianSiswa);
		return grupKategoriItemPenilaianSiswa;
	}

	/**
	 * Setel grup kategori item penilaian (modul sekolah).
	 *
	 * @param grupKategoriItemPenilaianSiswa grup kategori; {@code null} diizinkan
	 * @see #getGrupKategoriItemPenilaianSiswa()
	 */
	public void setGrupKategoriItemPenilaianSiswa(GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa) {
		this.grupKategoriItemPenilaianSiswa = grupKategoriItemPenilaianSiswa;
	}

	/**
	 * Grup penilaian (modul SEKOLAH).
	 *
	 * <p>Relasi {@code @ManyToOne} lazy ke {@link GrupPenilaian} (kolom {@code grup_penilaian},
	 * boleh {@code null}). Getter menulis balik hasil {@link GeneralValueObject#check(Object)} ke
	 * field.</p>
	 *
	 * @return grup penilaian, atau {@code null}
	 * @see #getJenisItemPenilaianSiswa()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "grup_penilaian", nullable = true)
	public GrupPenilaian getGrupPenilaian() {
		grupPenilaian = check(grupPenilaian);
		return grupPenilaian;
	}

	/**
	 * Setel grup penilaian (modul sekolah).
	 *
	 * @param grupPenilaian grup penilaian; {@code null} diizinkan
	 * @see #getGrupPenilaian()
	 */
	public void setGrupPenilaian(GrupPenilaian grupPenilaian) {
		this.grupPenilaian = grupPenilaian;
	}

	/**
	 * Formula/nilai bawaan saat kolom nilai masih kosong — praktisnya string {@code "{}"}.
	 *
	 * <p><b>{@code public static} tanpa {@code final}</b>: konstanta ini secara teknis dapat diubah
	 * saat runtime oleh kode mana pun, dan perubahannya akan langsung memengaruhi seluruh instance
	 * (pola yang sama juga ada pada {@link Tugas#JSON}). Dicatat apa adanya, bukan disarankan.</p>
	 *
	 * @see #getKeteranganNilaiLama()
	 */
	public static String DEFAULT_FORMULA = new JSONObject().toString();

	/**
	 * Nilai peserta versi LAMA, dari kolom {@code keterangannilai}.
	 *
	 * <p>Isinya bentuk JSON yang sama dengan {@link #getKeteranganNilai()}; kolom ini ditinggalkan
	 * saat data dipindah ke {@code keterangan_nilai_baru} dan kini hanya dibaca sebagai cadangan
	 * oleh {@link #getKeteranganNilai()}. Kosong dinormalkan menjadi {@link #DEFAULT_FORMULA}.</p>
	 *
	 * @return teks JSON nilai versi lama; {@code "{}"} bila kosong, tidak pernah {@code null}
	 * @see #getKeteranganNilai()
	 */
	@Column(columnDefinition = "text", name = "keterangannilai")
	public String getKeteranganNilaiLama() {
		return keteranganNilaiLama == null || keteranganNilaiLama.trim().isEmpty() ? DEFAULT_FORMULA
				: keteranganNilaiLama;
	}

	/**
	 * Setel nilai peserta pada kolom LAMA.
	 *
	 * <p>Kode baru sebaiknya memakai {@link #setKeteranganNilai(String)}; kolom lama hanya disentuh
	 * jalur migrasi.</p>
	 *
	 * @param keteranganNilaiLama teks JSON nilai versi lama
	 * @see #getKeteranganNilaiLama()
	 */
	public void setKeteranganNilaiLama(String keteranganNilaiLama) {
		this.keteranganNilaiLama = keteranganNilaiLama;
	}

	/**
	 * Nilai peserta yang BERLAKU untuk tugas ini — teks JSON.
	 *
	 * <p><b>Bentuk data.</b> Objek JSON datar dengan kunci berbasis peserta:
	 * {@code "&lt;id&gt;_mhs"} (mahasiswa), {@code "&lt;id&gt;_siswa"} (siswa),
	 * {@code "&lt;id&gt;_cal_mhs"} (calon mahasiswa), {@code "&lt;id&gt;_cal_siswa"} (calon siswa),
	 * ditambah akhiran:</p>
	 * <ul>
	 *   <li>{@code _nilai} — satu angka, mode penilaian STANDAR;</li>
	 *   <li>{@code _nilai_&lt;idFormatNilai&gt;} — angka per Sub-CPMK, mode OBE;</li>
	 *   <li>{@code _ket} — keterangan teks dari penilai.</li>
	 * </ul>
	 * <p>Kunci yang tidak ada dibaca sebagai {@code 0.0}. Penyusunnya tersebar:
	 * {@code TugasMandiriHelper} (edit inline, unggah/unduh Excel), {@code RekapHasilTugas},
	 * {@code RekapNilaiView}, dan {@code RekapHasilTugasPerTugasDanUjianObe}.</p>
	 *
	 * <p><b>Perpindahan kolom disembunyikan di sini:</b> nilai diambil dari kolom baru
	 * {@code keterangan_nilai_baru} bila terisi, dan jatuh ke kolom lama
	 * ({@link #getKeteranganNilaiLama()}) bila tidak. Karena fallback itu, getter ini tidak pernah
	 * mengembalikan {@code null} maupun string kosong — paling buruk {@code "{}"}.</p>
	 *
	 * <p>Menimpa properti abstrak bernama sama dari {@link Tugas}.</p>
	 *
	 * @return teks JSON nilai yang berlaku; {@code "{}"} bila kedua kolom kosong
	 * @see #getKeteranganNilaiLama()
	 * @see #getFormatNilais()
	 * @see #getNilaiManualJson()
	 */
	@Override
	@Column(columnDefinition = "text", name = "keterangan_nilai_baru")
	public String getKeteranganNilai() {
		return keteranganNilai == null || keteranganNilai.trim().isEmpty() ? getKeteranganNilaiLama() : keteranganNilai;
	}

	/**
	 * Setel nilai peserta pada kolom BARU {@code keterangan_nilai_baru}.
	 *
	 * <p>Kolom lama tidak ikut diubah sehingga isinya tetap tersimpan sebagai cadangan. Pemanggil
	 * bertanggung jawab menyusun JSON yang utuh: karena satu string memuat nilai SEMUA peserta,
	 * menulis JSON parsial akan menghapus nilai peserta lain.</p>
	 *
	 * @param keteranganNilai teks JSON nilai per peserta
	 * @see #getKeteranganNilai()
	 */
	@Override
	public void setKeteranganNilai(String keteranganNilai) {
		this.keteranganNilai = keteranganNilai;
	}

	/**
	 * Daftar komponen {@link FormatNilai} (Sub-CPMK) yang dinilai pada tugas ini — teks JSON,
	 * sekaligus <b>saklar mode OBE</b>.
	 *
	 * <p><b>Bentuk data.</b> Objek JSON yang kuncinya adalah id {@link FormatNilai} dan nilainya
	 * bobot komponen tersebut. Hanya komponen yang kuncinya ada di sini yang ikut dinilai.</p>
	 *
	 * <p><b>Peran sebagai saklar.</b> Pemanggil membandingkan hasilnya dengan {@link Tugas#JSON}:
	 * bila masih literal {@code "{}"}, tugas dianggap memakai penilaian STANDAR (satu angka lewat
	 * {@link #getFormatNilai()}); bila sudah berisi komponen, tugas masuk mode OBE dan nilainya
	 * dibaca dari {@link #getKeteranganNilai()} dengan kunci
	 * {@code _nilai_&lt;idFormatNilai&gt;}.</p>
	 *
	 * <p>Kosong dinormalkan menjadi {@link Tugas#JSON}. Perhatikan bahwa {@link Tugas#JSON} sendiri
	 * {@code public static} non-{@code final}, jadi konstanta pembanding itu secara teknis bisa
	 * diubah saat runtime.</p>
	 *
	 * @return teks JSON komponen nilai; {@link Tugas#JSON} ({@code "{}"}) bila belum diatur, tidak
	 *         pernah {@code null}
	 * @see #getKeteranganNilai()
	 * @see #getFormatNilai()
	 * @see #getSubCpmkPerPeserta()
	 */
	@Column(columnDefinition = "text")
	public String getFormatNilais() {
		return formatNilais == null || formatNilais.trim().isEmpty() ? Tugas.JSON : formatNilais;
	}

	/**
	 * Setel daftar komponen nilai OBE.
	 *
	 * <p>Menyetel nilai selain {@code "{}"} berarti MENGUBAH MODE PENILAIAN tugas ini menjadi OBE;
	 * nilai lama bermode standar (kunci {@code _nilai}) tidak ikut dikonversi.</p>
	 *
	 * @param formatNilais teks JSON komponen nilai; {@code null}/kosong dibaca sebagai {@code "{}"}
	 * @see #getFormatNilais()
	 */
	public void setFormatNilais(String formatNilais) {
		this.formatNilais = formatNilais;
	}

	/**
	 * {@link Pertemuan} induk sebagai object — pemetaan BACA-SAJA atas kolom yang sama dengan
	 * {@link #getPertemuan()}.
	 *
	 * <p>Ditandai {@code insertable = false, updatable = false} sehingga menyetelnya tidak pernah
	 * tersimpan; penulisan HANYA lewat {@link #setPertemuan(Long)}. {@code FetchMode.SELECT}
	 * membuatnya dimuat lewat query terpisah, dan {@code @NotFound(NotFoundAction.IGNORE)} membuat
	 * baris yatim (pertemuan sudah dihapus, mis. oleh pembersihan native di
	 * {@code PenjadwalanHelper}) menghasilkan {@code null} alih-alih melempar
	 * {@code EntityNotFoundException}.</p>
	 *
	 * <p>Getter ini murni dan tidak memanggil {@link GeneralValueObject#check(Object)} — pada
	 * object yang sudah <i>detached</i> nilainya bisa {@code null} atau berupa proxy yang belum
	 * terinisialisasi. Bila butuh kepastian, pakai {@link #ambilPertemuan()} (lebih mahal).</p>
	 *
	 * @return pertemuan induk, atau {@code null}
	 * @see #getPertemuan()
	 * @see #ambilPertemuan()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@NotFound(action = NotFoundAction.IGNORE)
	@JoinColumn(name = "pertemuan", nullable = true, insertable = false, updatable = false)
	public Pertemuan getPertemuanData() {
		return pertemuanData;
	}

	/**
	 * Setel object pertemuan induk.
	 *
	 * <p><b>Tidak tersimpan ke basis data</b> (pemetaannya baca-saja) — gunanya hanya mengisi cache
	 * di memori agar {@link #getPertemuanData()} tidak perlu query, mis. saat
	 * {@code TugasMandiriHelper} memindahkan tugas ke pertemuan lain dan sudah memegang object
	 * pertemuannya. Untuk benar-benar memindahkan induk, panggil {@link #setPertemuan(Long)}.</p>
	 *
	 * @param pertemuanData object pertemuan induk
	 * @see #getPertemuanData()
	 */
	public void setPertemuanData(Pertemuan pertemuanData) {
		this.pertemuanData = pertemuanData;
	}

	/**
	 * Penanda apakah tugas ini "aktif".
	 *
	 * <p><b>Nilai turunan, bukan nilai tersimpan.</b> Setiap pemanggilan menghitung ulang
	 * {@code aktif = !getJudultugas().isEmpty()} — tugas dianggap aktif semata-mata karena judulnya
	 * terisi — lalu <b>menulis hasilnya ke field {@link #aktif}</b>. Karena field itu tidak ditandai
	 * {@code @Transient}, Hibernate memetakannya sebagai properti biasa; membaca getter ini
	 * karenanya dapat menandai entity <i>dirty</i> dan menimpa nilai kolomnya di basis data. Apa pun
	 * yang pernah diset lewat {@link #setAktif(Boolean)} tidak akan bertahan melewati pembacaan
	 * berikutnya.</p>
	 *
	 * <p>Implementasi ini identik baris demi baris dengan {@link TugasKelompok#getAktif()}.
	 * Maknanya BERBEDA dari {@link Pertemuan#getAktif()}, yang merupakan penanda hapus-lunak
	 * sungguhan dan benar-benar tersimpan.</p>
	 *
	 * @return {@code true} bila judul tugas tidak kosong; tidak pernah {@code null}
	 * @see #getJudultugas()
	 */
	public Boolean getAktif() {
		aktif = !getJudultugas().isEmpty();
		return aktif;
	}

	/**
	 * Setel penanda aktif.
	 *
	 * <p><b>Praktis tidak berguna:</b> nilai apa pun yang diset di sini akan ditimpa oleh
	 * {@link #getAktif()} pada pembacaan berikutnya. Setter ini ada terutama agar properti
	 * {@code aktif} memenuhi konvensi JavaBean yang dibutuhkan Hibernate dan pengikatan data
	 * ZK.</p>
	 *
	 * @param aktif penanda aktif; akan ditimpa oleh nilai turunan saat dibaca
	 * @see #getAktif()
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}
}
