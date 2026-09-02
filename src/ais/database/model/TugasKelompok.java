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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.envers.Audited;
import org.json.JSONObject;

import ais.database.model.kkn.KelompokKkn;
import ais.database.model.pkl.KelompokPkl;
import ais.database.model.sekolah.DetailJenisPenilaian;
import ais.database.model.sekolah.GrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.GrupPenilaian;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.JenisItemPenilaianSiswa;

/**
 * Entity <b>tugas kelompok</b> — satu baris tabel {@code public.tugas_kelompok} adalah SATU
 * PENUGASAN yang dikerjakan secara berkelompok pada sebuah pertemuan e-Learning.
 *
 * <h3>Peringatan penamaan (mudah salah tafsir)</h3>
 * <p>Meski namanya {@code TugasKelompok}, baris entity ini <b>bukan</b> sebuah kelompok
 * mahasiswa. Ia adalah <i>tugasnya</i>. Hierarki sebenarnya bertingkat tiga:</p>
 * <ol>
 *   <li>{@code TugasKelompok} — penugasannya (judul, isi, jadwal buka/tutup, bobot, format
 *       nilai). Satu per pertemuan per penugasan.</li>
 *   <li>{@link NamaTugasKelompok} — <b>kelompok</b>-nya (nama kelompok, kuota, keterangan),
 *       banyak baris menunjuk balik ke satu {@code TugasKelompok} lewat kolom
 *       {@code nama_tugas_kelompok.tugas_kelompok}.</li>
 *   <li>{@link NamaTugasKelompokPunyaMahasiswa} — <b>keanggotaan</b>: satu baris per mahasiswa
 *       per kelompok.</li>
 * </ol>
 * <p>Jadi "berapa kelompok di tugas ini" dijawab dengan menghitung {@link NamaTugasKelompok},
 * bukan {@code TugasKelompok}. Layar pengelolanya adalah
 * {@code ais.action.master.helper.NamaTugasKelompokHelper} (daftar kelompok + anggota) dan
 * {@code ais.action.master.helper.TugasKelompokHelper} (penugasannya sendiri).</p>
 *
 * <h3>Kedudukan dalam hierarki kelas</h3>
 * <p>{@code extends} {@link Tugas} (bukan langsung {@link GeneralValueObject}). {@link Tugas}
 * adalah kelas abstrak yang mendeklarasikan kontrak "sesuatu yang bisa dikumpulkan berkas oleh
 * peserta" dan sekaligus memuat seluruh mesin berkas jawaban
 * ({@link Tugas#ambilTugasFileContentTotal()}, {@link Tugas#reInitTugasFileContent()},
 * {@link Tugas#ambilJumlahTugasFileContent()}, indeks JSON di berkas
 * {@code tugas_file_content_&lt;id&gt;}). Saudara sekandungnya adalah
 * {@link TugasPertemuan} (tugas mandiri/individu) — dua kelas ini nyaris kembar dan banyak
 * method di sini merupakan salinan persis dari sana; bila memperbaiki sesuatu di sini,
 * periksa apakah cacat yang sama juga ada di {@link TugasPertemuan}.</p>
 * <p>Kontrak umum id/{@code equals}/{@code compareTo}/{@link GeneralValueObject#check(Object)}/
 * cache/audit trail tetap milik {@link GeneralValueObject} — lihat Javadoc kelas itu, jangan
 * diulang di sini.</p>
 *
 * <h3>Mekanisme {@code keteranganNilai}: nilai OBE disimpan sebagai JSON, bukan baris tabel</h3>
 * <p>Ini bagian paling non-obvious dari entity ini. Ada TIGA kolom teks yang semuanya berisi
 * JSON dan gampang tertukar:</p>
 * <table border="1">
 *   <caption>Tiga kolom JSON pada {@code tugas_kelompok}</caption>
 *   <tr><th>Properti</th><th>Kolom</th><th>Isi</th></tr>
 *   <tr>
 *     <td>{@link #getFormatNilais()}</td><td>{@code format_nilais}</td>
 *     <td><b>Komponen mana</b> yang dinilai. Kunci = id {@link FormatNilai} (CPMK/capaian
 *     pembelajaran), isi = bobot komponen. Berperan juga sebagai <b>saklar mode OBE</b>:
 *     selama nilainya masih literal {@link Tugas#JSON} ({@code "{}"}), tugas ini dianggap
 *     memakai penilaian standar (satu angka saja), bukan OBE.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #getKeteranganNilai()}</td><td>{@code keterangan_nilai_baru}</td>
 *     <td><b>Nilai per mahasiswa per komponen</b> pada mode OBE. Kunci berbentuk
 *     {@code "&lt;idMahasiswa&gt;_mhs_nilai_&lt;idFormatNilai&gt;"}, nilai = {@code double}
 *     (default {@code 0.0} bila kunci belum ada).</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #getKeteranganNilaiLama()}</td><td>{@code keterangannilai}</td>
 *     <td>Isi yang sama versi LAMA, sebelum kolom dipindah. Hanya dibaca sebagai cadangan.</td>
 *   </tr>
 * </table>
 * <p><b>Konsekuensi penting.</b> Pada mode OBE, nilai per komponen TIDAK disimpan sebagai baris
 * tersendiri dan TIDAK memakai kolom {@code nilai} pada
 * {@link NamaTugasKelompokPunyaMahasiswa}; semuanya menumpuk dalam satu string JSON di satu
 * baris {@code tugas_kelompok}. Akibatnya:</p>
 * <ul>
 *   <li>Nilai <b>tidak bisa di-query dengan SQL biasa</b> (tidak ada JOIN, tidak ada agregasi
 *       per komponen di sisi basis data). Semua pembaca harus mem-parsing JSON sendiri —
 *       lihat {@code NamaTugasKelompokHelper.downloadDataKelompokObe()},
 *       {@code NamaTugasKelompokPunyaMahasiswaHelper}, {@code NilaiObeAction} dan
 *       {@code RekapHasilTugasPerTugasDanUjianObe} yang semuanya menyusun ulang kunci
 *       {@code "&lt;idMahasiswa&gt;_mhs_nilai_&lt;idFormatNilai&gt;"} secara manual.</li>
 *   <li>Dua pengguna yang menyimpan nilai bersamaan saling menimpa seluruh JSON (last-write-wins
 *       untuk SEMUA mahasiswa sekaligus, bukan hanya barisnya sendiri).</li>
 *   <li>Pembaca di beberapa tempat memanggil {@code .replace('\0', ' ')} sebelum mem-parsing —
 *       artinya pernah ditemukan byte NUL menyusup ke kolom teks ini di data produksi.</li>
 * </ul>
 * <p>Karena keanggotaan kelompok berada di tabel terpisah sementara nilainya tidak, entity ini
 * bisa punya nilai OBE untuk mahasiswa yang <b>tidak punya baris keanggotaan sama sekali</b>
 * (kunci JSON tetap tersimpan). Kondisi itu memang pernah terjadi akibat bug importer OBE versi
 * lama yang tidak pernah membuat baris {@link NamaTugasKelompokPunyaMahasiswa} — lihat catatan
 * di {@code NamaTugasKelompokHelper}.</p>
 *
 * <h3>Relasi utama</h3>
 * <ul>
 *   <li><b>{@link Pertemuan}</b> — dipetakan DUA KALI ke kolom yang sama {@code pertemuan}:
 *       {@link #getPertemuan()} sebagai {@code Long} mentah yang bisa ditulis, dan
 *       {@link #getPertemuanData()} sebagai relasi {@code @ManyToOne} baca-saja
 *       ({@code insertable=false, updatable=false}). Penulisan HANYA lewat
 *       {@link #setPertemuan(Long)}; menyetel {@link #setPertemuanData(Pertemuan)} saja tidak
 *       akan tersimpan.</li>
 *   <li><b>{@link Perkuliahan}</b>, <b>{@link KelompokKkn}</b>, <b>{@link KelompokPkl}</b> —
 *       konteks tempat tugas diberikan (kuliah reguler / KKN / PKL). Ketiganya <b>diturunkan
 *       ulang dari {@link #getPertemuanData()} setiap kali getternya dibaca</b>; lihat
 *       peringatan efek samping di bawah.</li>
 *   <li><b>{@link JadwalPelajaran}</b> — padanan {@link Perkuliahan} untuk modul sekolah.</li>
 *   <li><b>{@link FormatNilai}</b> + {@link DetailJenisPenilaian} +
 *       {@link JenisItemPenilaianSiswa} + {@link GrupPenilaian} +
 *       {@link GrupKategoriItemPenilaianSiswa} — klasifikasi nilai; kelompok terakhir khusus
 *       modul sekolah.</li>
 *   <li><b>{@link SyaratUjian}</b> ({@link #getSyaratMengumpulkanTugas()}) — prasyarat agar
 *       peserta boleh mengumpulkan.</li>
 * </ul>
 *
 * <h3>Pengelompokan method</h3>
 * <ol>
 *   <li><b>Jejak audit bayangan</b> — {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()}, {@link #onUpdate()}. Lihat catatan khusus di bawah.</li>
 *   <li><b>Identitas &amp; teks</b> — {@link #getId()}, {@link #getNama()},
 *       {@link #getJudul()}, {@link #getKeterangan()}, {@link #toString()}.</li>
 *   <li><b>Alias kontrak {@link Tugas}</b> — {@link #getJudultugas()} dan
 *       {@link #getIsitugas()} adalah nama lain untuk kolom {@code judul} dan {@code nama};
 *       keduanya dipetakan baca-saja ke kolom yang sama.</li>
 *   <li><b>Jadwal</b> — {@link #getTanggal()}, {@link #getMulai()}, {@link #getSelesai()}.</li>
 *   <li><b>Relasi konteks</b> — {@link #getPerkuliahan()}, {@link #getKelompokKkn()},
 *       {@link #getKelompokPkl()}, {@link #getJadwalPelajaran()}, {@link #getPertemuan()},
 *       {@link #getPertemuanData()}, {@link #ambilPertemuan()}.</li>
 *   <li><b>Penilaian</b> — {@link #getFormatNilai()}, {@link #getFormatNilais()},
 *       {@link #getProsentase()}, {@link #getKeteranganNilai()},
 *       {@link #getKeteranganNilaiLama()}, dan kelompok klasifikasi modul sekolah.</li>
 *   <li><b>Daftar CSV peserta</b> — {@link #getMhsYgTidakIkut()} dan
 *       {@link #getMhsBolehUploadUlang()}, keduanya string CSV ber-koma-pembungkus.</li>
 *   <li><b>Kontrol akses</b> — {@link #getSyaratAkses()} (JSON prasyarat),
 *       {@link #getAktif()}.</li>
 * </ol>
 *
 * <h3>Hal non-obvious yang WAJIB diketahui sebelum menyentuh kelas ini</h3>
 * <ul>
 *   <li><b>Beberapa getter TIDAK murni — mereka menulis balik ke field yang dipetakan</b>
 *       sehingga sekadar membaca entity ini bisa menandainya <i>dirty</i> dan memicu
 *       {@code UPDATE} saat flush. Yang terbukti: {@link #getPerkuliahan()},
 *       {@link #getKelompokKkn()}, {@link #getKelompokPkl()}, {@link #getJadwalPelajaran()},
 *       {@link #getMhsYgTidakIkut()}, {@link #getMhsBolehUploadUlang()}, dan
 *       {@link #getAktif()}. Empat yang pertama bahkan memakai
 *       {@code cascade = PERSIST, MERGE}, jadi object yang dipungut dari {@link Pertemuan}
 *       ikut terbawa ke dalam graf persistence.</li>
 *   <li><b>{@link #getJadwalPelajaran()} dan {@link #ambilPertemuan()} bisa memukul basis
 *       data/cache</b> dari dalam sebuah getter (lewat
 *       {@link GeneralValueObject#ambilData(Class, String)}) — hindari memanggilnya di dalam
 *       loop besar atau dari thread tanpa session.</li>
 *   <li><b>Setter {@link #setOleh(String)} dan {@link #setOlehId(String)} diam-diam menolak
 *       nilai kosong</b> — nilai lama tidak akan pernah bisa dibersihkan lewat setter.</li>
 *   <li>Kelas ini mendeklarasikan ulang field {@code oleh}, {@code olehId},
 *       {@code tanggal_dirubah} beserta {@link #onUpdate()} yang sudah ada identik di
 *       {@link GeneralValueObject} — lihat {@link #getOleh()}.</li>
 * </ul>
 *
 * @see Tugas
 * @see TugasPertemuan
 * @see NamaTugasKelompok
 * @see NamaTugasKelompokPunyaMahasiswa
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "tugas_kelompok")
public class TugasKelompok extends Tugas {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama (kolom {@code id}, {@code IDENTITY}). @see #getId() */
	private Long id;

	/**
	 * Nama pengguna terakhir yang mengubah baris ini (jejak audit).
	 *
	 * <p>Field bayangan: {@link GeneralValueObject} sudah punya field bernama sama.
	 * Lihat {@link #getOleh()}.</p>
	 */
	private String oleh;

	/**
	 * Id pengguna terakhir yang mengubah baris ini (jejak audit).
	 *
	 * <p>Field bayangan, lihat {@link #getOleh()}.</p>
	 */
	private String olehId;

	/**
	 * Id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah diisi
	 * @see #getOleh()
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Setel id pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatikan:</b> argumen {@code null} atau berisi spasi saja DIABAIKAN diam-diam —
	 * nilai lama dipertahankan. Nilai audit karenanya tidak pernah bisa dikosongkan lewat
	 * setter ini; hapus lewat SQL/HQL bila memang perlu.</p>
	 *
	 * @param olehId id pengguna; kosong/{@code null} berarti "jangan ubah"
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
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai kosong/{@code null} diabaikan
	 * diam-diam.</p>
	 *
	 * @param oleh nama pengguna; kosong/{@code null} berarti "jangan ubah"
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
	 * <p><b>Catatan arsitektur (pola berulang di seluruh paket {@code ais.database.model}).</b>
	 * Pasangan {@code oleh}/{@code olehId}/{@code tanggal_dirubah} beserta hook
	 * {@link #onUpdate()} sudah dideklarasikan identik di {@link GeneralValueObject}, namun
	 * dideklarasikan ULANG di sini. Field induk karenanya <i>di-shadow</i>: kode yang menulis
	 * lewat referensi bertipe {@code GeneralValueObject} dan kode yang membaca lewat referensi
	 * bertipe {@code TugasKelompok} dapat melihat dua nilai berbeda pada object yang sama.
	 * Hibernate memakai akses properti, sehingga yang tersimpan adalah yang terlihat dari getter
	 * kelas ini. Pola ini konsisten muncul di seluruh entity model dan sengaja TIDAK diperbaiki
	 * di sini karena berisiko mengubah perilaku persistence.</p>
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA yang dipanggil Hibernate tepat sebelum {@code UPDATE} baris ini.
	 *
	 * <p>Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #setTanggal_dirubah(Date)} (dan jejak audit lain) dengan waktu server. Tidak
	 * pernah dipanggil manual dari kode aplikasi.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir. Diinisialisasi ke waktu server saat object dibuat, lalu
	 * dimutakhirkan otomatis oleh {@link #onUpdate()}.
	 *
	 * <p>Field bayangan, lihat {@link #getOleh()}.</p>
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Setel waktu perubahan terakhir secara manual.
	 *
	 * <p>Umumnya tidak perlu dipanggil: {@link #onUpdate()} mengisinya otomatis.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Waktu perubahan terakhir baris ini.
	 *
	 * @return timestamp perubahan terakhir; tidak pernah {@code null} pada object baru
	 * @see #onUpdate()
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks object — dipakai ZK sebagai label bawaan di combobox/listbox.
	 *
	 * <p>Membaca field {@link #nama} LANGSUNG, bukan lewat {@link #getNama()}, sehingga hasilnya
	 * bisa {@code null} (getter-nya menormalkan {@code null} menjadi string kosong). Komponen UI
	 * yang menampilkannya harus tahan terhadap {@code null}.</p>
	 *
	 * @return isi tugas apa adanya, mungkin {@code null}
	 */
	public String toString() {
		return nama;
	}

	/** Judul tugas (kolom {@code judul}). @see #getJudul() */
	private String judul;

	/** Isi/uraian tugas (kolom {@code nama}). @see #getNama() */
	private String nama;

	/** Keterangan tambahan bebas (kolom {@code keterangan}). @see #getKeterangan() */
	private String keterangan;

	/** Perkuliahan tempat tugas diberikan; diturunkan ulang dari pertemuan. @see #getPerkuliahan() */
	private Perkuliahan perkuliahan;

	/** Padanan {@link #perkuliahan} untuk modul sekolah. @see #getJadwalPelajaran() */
	private JadwalPelajaran jadwalPelajaran;

	/** Kelompok KKN pemilik tugas, bila konteksnya KKN. @see #getKelompokKkn() */
	private KelompokKkn kelompokKkn;

	/** Kelompok PKL pemilik tugas, bila konteksnya PKL. @see #getKelompokPkl() */
	private KelompokPkl kelompokPkl;

	/** Tanggal pembuatan tugas; default waktu server saat object dibuat. @see #getTanggal() */
	private Date tanggal = ais.ui.util.WaktuUtil.getDate();

	/** Awal jendela pengumpulan. @see #getMulai() */
	private Date mulai;

	/** Akhir jendela pengumpulan (tenggat). @see #getSelesai() */
	private Date selesai;

	/** Format/komponen nilai tunggal untuk mode non-OBE. @see #getFormatNilai() */
	private FormatNilai formatNilai;

	/** Klasifikasi jenis penilaian modul sekolah. @see #getDetailJenisPenilaian() */
	private DetailJenisPenilaian detailJenisPenilaian;

	/** Bobot tugas dalam persen; default 100. @see #getProsentase() */
	private Double prosentase;

	/** Prasyarat yang harus dipenuhi agar peserta boleh mengumpulkan. @see #getSyaratMengumpulkanTugas() */
	private SyaratUjian syaratMengumpulkanTugas;

	/** CSV id peserta yang dikecualikan dari tugas ini. @see #getMhsYgTidakIkut() */
	private String mhsYgTidakIkut;

	/**
	 * Id {@link Pertemuan} induk sebagai angka mentah (kolom {@code pertemuan}, satu-satunya
	 * sisi yang boleh ditulis).
	 *
	 * @see #getPertemuan()
	 * @see #getPertemuanData()
	 */
	private Long pertemuan;

	/** CSV id peserta yang diizinkan mengunggah ulang jawaban. @see #getMhsBolehUploadUlang() */
	private String mhsBolehUploadUlang;

	/** JSON prasyarat akses konten e-Learning. @see #getSyaratAkses() */
	private String syaratAkses;

	/** Jenis item penilaian (modul sekolah). @see #getJenisItemPenilaianSiswa() */
	private JenisItemPenilaianSiswa jenisItemPenilaianSiswa;

	/** Grup penilaian (modul sekolah). @see #getGrupPenilaian() */
	private GrupPenilaian grupPenilaian;

	/** Grup kategori item penilaian (modul sekolah). @see #getGrupKategoriItemPenilaianSiswa() */
	private GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa;

	/**
	 * JSON nilai OBE per mahasiswa per komponen, kolom BARU {@code keterangan_nilai_baru}.
	 *
	 * @see #getKeteranganNilai()
	 */
	private String keteranganNilai;

	/**
	 * JSON nilai OBE versi LAMA, kolom {@code keterangannilai}.
	 *
	 * @see #getKeteranganNilaiLama()
	 */
	private String keteranganNilaiLama;

	/**
	 * JSON komponen {@link FormatNilai} yang dinilai sekaligus saklar mode OBE.
	 *
	 * @see #getFormatNilais()
	 */
	private String formatNilais;

	/**
	 * Sisi relasi baca-saja ke {@link Pertemuan} induk, memetakan kolom yang sama dengan
	 * {@link #pertemuan}.
	 *
	 * @see #getPertemuanData()
	 */
	private Pertemuan pertemuanData;

	/** Penanda aktif turunan; selalu ditulis ulang oleh getternya. @see #getAktif() */
	private Boolean aktif;

	/**
	 * Konstruktor kosong wajib Hibernate/ZK.
	 *
	 * <p>Membiarkan seluruh field pada nilai bawaannya; {@link #tanggal} dan
	 * {@link #tanggal_dirubah} sudah terisi waktu server lewat inisialisasi field.</p>
	 */
	public TugasKelompok() {
	}

	/**
	 * Kunci utama baris ini.
	 *
	 * <p>Di-generate basis data ({@code IDENTITY}), karena itu kolomnya
	 * {@code insertable = false}. Bernilai {@code null} selama object belum disimpan — banyak
	 * method warisan {@link Tugas} (mis. {@link Tugas#ambilJumlahTugasFileContent()}) memeriksa
	 * kondisi ini dan pulang lebih awal.</p>
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan
	 * @see GeneralValueObject
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Setel kunci utama.
	 *
	 * <p>Umumnya hanya dipanggil Hibernate atau kode yang sengaja menyalin identitas baris.</p>
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Isi/uraian tugas (kolom {@code nama}, bertipe {@code text}).
	 *
	 * <p>Meski namanya "nama", secara domain kolom inilah <b>isi soal/instruksi</b> tugas —
	 * lihat {@link #getIsitugas()} yang memetakan kolom yang sama dengan penamaan yang lebih
	 * jujur. Judul singkatnya ada di {@link #getJudul()}.</p>
	 *
	 * <p>{@code null} dinormalkan menjadi string kosong dan hasilnya di-{@code trim}, sehingga
	 * pemanggil tidak perlu menjaga {@code null}. Perhatikan {@link #toString()} TIDAK memakai
	 * getter ini dan masih bisa mengembalikan {@code null}.</p>
	 *
	 * @return isi tugas; tidak pernah {@code null}
	 */
	@Column(name = "nama", columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? "" : this.nama.trim();
	}

	/**
	 * Setel isi/uraian tugas.
	 *
	 * @param nama isi tugas; disimpan apa adanya tanpa trim
	 * @see #getNama()
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan tambahan bebas untuk tugas ini.
	 *
	 * @return keterangan apa adanya, mungkin {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Setel keterangan tambahan.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Setel perkuliahan induk secara eksplisit.
	 *
	 * <p><b>Perhatikan:</b> nilai yang diset di sini bisa DITIMPA kembali begitu
	 * {@link #getPerkuliahan()} dipanggil, bila {@link #getPertemuanData()} sudah terisi dan
	 * pertemuan tersebut punya perkuliahan sendiri. Untuk mengubah konteks tugas secara
	 * permanen, ubah {@link #setPertemuan(Long)}.</p>
	 *
	 * @param perkuliahan perkuliahan induk; boleh {@code null}
	 * @see #getPerkuliahan()
	 */
	public void setPerkuliahan(Perkuliahan perkuliahan) {
		this.perkuliahan = perkuliahan;
	}

	/**
	 * Perkuliahan (mata kuliah + kelas + tahun ajaran) tempat tugas ini diberikan.
	 *
	 * <p><b>Getter ini TIDAK murni.</b> Bila {@link #getPertemuanData()} tersedia dan pertemuan
	 * itu punya perkuliahan, nilai field {@link #perkuliahan} <b>ditimpa</b> dengan perkuliahan
	 * milik pertemuan — jadi pertemuan selalu menang atas apa pun yang pernah diset lewat
	 * {@link #setPerkuliahan(Perkuliahan)}. Karena relasi ini ber-{@code cascade} PERSIST dan
	 * MERGE, penulisan tersebut ikut masuk graf persistence dan dapat menghasilkan {@code UPDATE}
	 * pada baris {@code tugas_kelompok} hanya karena getternya dibaca di dalam session
	 * terbuka.</p>
	 *
	 * <p>Hasil akhirnya dilewatkan {@link GeneralValueObject#check(Object)} untuk memaksa resolusi
	 * proxy lazy (aman dipanggil pada object detached — {@code check} membuka session sendiri bila
	 * perlu dan tidak pernah melempar exception).</p>
	 *
	 * <p>Dipakai antara lain oleh {@code NamaTugasKelompokHelper.ambilObeFormatNilais()} untuk
	 * menentukan apakah kurikulum perkuliahan ini memakai OBE pada tahun ajaran bersangkutan.</p>
	 *
	 * @return perkuliahan induk, atau {@code null} bila tugas ini bukan konteks perkuliahan
	 *         (mis. KKN, PKL, atau modul sekolah)
	 * @see #getKelompokKkn()
	 * @see #getJadwalPelajaran()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "perkuliahan", nullable = true)
	public Perkuliahan getPerkuliahan() {

		if (getPertemuanData() != null) {
			Pertemuan pertemuan = getPertemuanData();
			if (pertemuan != null && pertemuan.getPerkuliahan() != null) {
				perkuliahan = pertemuan.getPerkuliahan();
			}
		}
		perkuliahan = check(perkuliahan);

		return perkuliahan;
	}

	/**
	 * Setel tanggal pembuatan tugas.
	 *
	 * @param tanggal tanggal pembuatan
	 * @see #getTanggal()
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Tanggal pembuatan tugas (bukan tenggat).
	 *
	 * <p>Jendela pengumpulan ada di {@link #getMulai()}–{@link #getSelesai()}. Nilai bawaannya
	 * waktu server saat object dibuat, jadi tidak pernah {@code null} pada object baru.</p>
	 *
	 * @return tanggal pembuatan
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal() {
		return tanggal;
	}

	/**
	 * Judul singkat tugas (kolom {@code judul}, bertipe {@code text}).
	 *
	 * <p>Ini sisi yang BOLEH ditulis untuk kolom {@code judul}; {@link #getJudultugas()}
	 * memetakan kolom yang sama secara baca-saja. Berbeda dari {@link #getJudultugas()}, getter
	 * ini mengembalikan nilai apa adanya, termasuk {@code null}.</p>
	 *
	 * @return judul tugas, mungkin {@code null}
	 */
	@Column(name = "judul", columnDefinition = "text")
	public String getJudul() {
		return judul;
	}

	/**
	 * Setel judul singkat tugas.
	 *
	 * <p>Berpengaruh langsung pada {@link #getAktif()}, yang menganggap tugas "aktif" hanya bila
	 * judulnya tidak kosong.</p>
	 *
	 * @param judul judul tugas
	 */
	public void setJudul(String judul) {
		this.judul = judul;
	}

	/**
	 * Kelompok KKN pemilik tugas ini, bila tugas diberikan dalam konteks KKN.
	 *
	 * <p><b>Getter tidak murni</b>, persis pola {@link #getPerkuliahan()}: bila
	 * {@link #getPertemuanData()} punya {@link KelompokKkn}, field lokal ditimpa dengan milik
	 * pertemuan, lalu hasilnya dilewatkan {@link GeneralValueObject#check(Object)}. Relasi
	 * ber-{@code cascade} PERSIST/MERGE sehingga penimpaan tersebut dapat ikut tersimpan.</p>
	 *
	 * @return kelompok KKN induk, atau {@code null} bila konteksnya bukan KKN
	 * @see #getPerkuliahan()
	 * @see #getKelompokPkl()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_kkn", nullable = true)
	public KelompokKkn getKelompokKkn() {
		if (getPertemuanData() != null) {
			Pertemuan pertemuan = getPertemuanData();
			if (pertemuan != null && pertemuan.getKelompokKkn() != null) {
				kelompokKkn = pertemuan.getKelompokKkn();
			}
		}
		kelompokKkn = check(kelompokKkn);
		return kelompokKkn;
	}

	/**
	 * Setel kelompok KKN induk.
	 *
	 * <p>Sama seperti {@link #setPerkuliahan(Perkuliahan)}, nilai ini bisa ditimpa kembali oleh
	 * {@link #getKelompokKkn()} bila pertemuan induk punya kelompok KKN sendiri.</p>
	 *
	 * @param kelompokKkn kelompok KKN; boleh {@code null}
	 */
	public void setKelompokKkn(KelompokKkn kelompokKkn) {
		this.kelompokKkn = kelompokKkn;
	}

	/**
	 * Kelompok PKL pemilik tugas ini, bila tugas diberikan dalam konteks PKL/magang.
	 *
	 * <p><b>Getter tidak murni</b>, pola identik dengan {@link #getKelompokKkn()}: nilai
	 * diturunkan ulang dari {@link #getPertemuanData()} bila ada, lalu di-{@code check}.</p>
	 *
	 * @return kelompok PKL induk, atau {@code null} bila konteksnya bukan PKL
	 * @see #getKelompokKkn()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_pkl", nullable = true)
	public KelompokPkl getKelompokPkl() {
		if (getPertemuanData() != null) {
			Pertemuan pertemuan = getPertemuanData();
			if (pertemuan != null && pertemuan.getKelompokPkl() != null) {
				kelompokPkl = pertemuan.getKelompokPkl();
			}
		}
		kelompokPkl = check(kelompokPkl);
		return kelompokPkl;
	}

	/**
	 * Setel kelompok PKL induk.
	 *
	 * @param kelompokPkl kelompok PKL; boleh {@code null}
	 * @see #getKelompokPkl()
	 */
	public void setKelompokPkl(KelompokPkl kelompokPkl) {
		this.kelompokPkl = kelompokPkl;
	}

	/**
	 * Waktu MULAI jendela pengumpulan tugas.
	 *
	 * <p>Komentar di dalam kode mencatat perubahan perilaku yang disengaja: dahulu nilai ini
	 * pernah jatuh ke tanggal pembuatan/perubahan bila kosong, sekarang TIDAK lagi — nilainya
	 * harus berasal dari pilihan eksplisit dosen pada form. Konsekuensinya getter ini dapat
	 * mengembalikan {@code null} untuk tugas yang jadwalnya belum diisi, dan pemanggil wajib
	 * menanganinya.</p>
	 *
	 * @return waktu mulai pengumpulan, atau {@code null} bila belum dijadwalkan
	 * @see #getSelesai()
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getMulai() {
		// Tidak lagi mengambil tanggal pembuatan/perubahan sebagai tanggal tugas.
		// Nilai ini harus berasal dari pilihan eksplisit dosen pada form.
		return mulai;
	}

	/**
	 * Setel waktu mulai jendela pengumpulan.
	 *
	 * @param mulai waktu mulai; {@code null} berarti belum dijadwalkan
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Waktu SELESAI jendela pengumpulan (tenggat tugas).
	 *
	 * <p>Sama seperti {@link #getMulai()}, tidak ada nilai turunan: {@code null} berarti belum
	 * dijadwalkan, bukan "tanpa batas" secara implisit.</p>
	 *
	 * @return tenggat pengumpulan, atau {@code null} bila belum dijadwalkan
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getSelesai() {
		return selesai;
	}

	/**
	 * Setel tenggat pengumpulan.
	 *
	 * @param selesai waktu selesai; {@code null} berarti belum dijadwalkan
	 */
	public void setSelesai(Date selesai) {
		this.selesai = selesai;
	}

	/**
	 * Prasyarat yang harus dipenuhi peserta sebelum boleh mengumpulkan tugas ini.
	 *
	 * <p>Memakai entity {@link SyaratUjian} yang sama dengan prasyarat ujian (dipakai ulang,
	 * bukan tipe tersendiri). Hasilnya dilewatkan {@link GeneralValueObject#check(Object)}
	 * sehingga proxy lazy diresolusi lebih dulu; berbeda dari {@link #getPerkuliahan()}, tidak
	 * ada penurunan ulang dari pertemuan di sini.</p>
	 *
	 * <p>Jangan tertukar dengan {@link #getSyaratAkses()} yang mengatur prasyarat MEMBUKA konten,
	 * bukan prasyarat mengumpulkan.</p>
	 *
	 * @return prasyarat pengumpulan, atau {@code null} bila tanpa prasyarat
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
	 * Format/komponen nilai TUNGGAL yang dipakai tugas ini pada mode penilaian standar.
	 *
	 * <p>Berbeda dari saudaranya {@link #getFormatNilais()} (berakhiran "s") yang berisi JSON
	 * BANYAK komponen untuk mode OBE. Relasi ini di-fetch {@code SELECT} eager, dan — tidak
	 * seperti kebanyakan relasi lain di kelas ini — TIDAK dilewatkan
	 * {@link GeneralValueObject#check(Object)}, jadi nilainya bisa saja berupa proxy Hibernate
	 * bila object sudah detached.</p>
	 *
	 * @return format nilai tunggal, atau {@code null} bila belum diset
	 * @see #getFormatNilais()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "format_nilai", nullable = true)
	public FormatNilai getFormatNilai() {
		return formatNilai;
	}

	/**
	 * Setel format nilai tunggal.
	 *
	 * @param formatNilai format nilai; boleh {@code null}
	 * @see #getFormatNilai()
	 */
	public void setFormatNilai(FormatNilai formatNilai) {
		this.formatNilai = formatNilai;
	}

	/**
	 * Bobot tugas ini dalam persen terhadap komponen nilai induknya.
	 *
	 * <p>Nilai {@code null} diartikan sebagai {@code 100.0} (tugas menyumbang penuh), sehingga
	 * data lama yang belum pernah mengisi bobot tetap terhitung wajar. Nilai bawaan ini TIDAK
	 * ditulis balik ke field — getter ini murni.</p>
	 *
	 * @return bobot dalam persen; tidak pernah {@code null}
	 */
	public Double getProsentase() {
		return prosentase == null ? 100.0 : prosentase;
	}

	/**
	 * Setel bobot tugas dalam persen.
	 *
	 * @param prosentase bobot; {@code null} berarti kembali ke bawaan 100
	 * @see #getProsentase()
	 */
	public void setProsentase(Double prosentase) {
		this.prosentase = prosentase;
	}

	/**
	 * Rincian jenis penilaian pada modul SEKOLAH (bukan perguruan tinggi).
	 *
	 * <p>Diresolusi lewat {@link GeneralValueObject#check(Object)}. Hanya terisi pada instalasi
	 * yang memakai modul sekolah bersama {@link #getJadwalPelajaran()},
	 * {@link #getJenisItemPenilaianSiswa()}, {@link #getGrupPenilaian()}, dan
	 * {@link #getGrupKategoriItemPenilaianSiswa()}.</p>
	 *
	 * @return detail jenis penilaian, atau {@code null} bila bukan konteks sekolah
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "detail_jenis_penilaian", nullable = true)
	public DetailJenisPenilaian getDetailJenisPenilaian() {
		detailJenisPenilaian = check(detailJenisPenilaian);
		return detailJenisPenilaian;
	}

	/**
	 * Setel rincian jenis penilaian modul sekolah.
	 *
	 * @param detailJenisPenilaian rincian jenis penilaian; boleh {@code null}
	 */
	public void setDetailJenisPenilaian(DetailJenisPenilaian detailJenisPenilaian) {
		this.detailJenisPenilaian = detailJenisPenilaian;
	}

	/**
	 * Jadwal pelajaran (padanan {@link Perkuliahan} untuk modul sekolah) tempat tugas diberikan.
	 *
	 * <p><b>Getter tidak murni DAN berpotensi memukul basis data.</b> Berbeda dari
	 * {@link #getPerkuliahan()} yang memakai relasi {@link #getPertemuanData()} di memori,
	 * method ini menguji {@link #getPertemuan()} (id mentah) lalu memanggil
	 * {@link #ambilPertemuan()} yang melakukan pencarian cache/basis data. Bila pertemuan
	 * ditemukan dan punya jadwal pelajaran, field lokal DITIMPA — dan karena relasinya
	 * ber-{@code cascade} PERSIST/MERGE, perubahan itu dapat ikut tersimpan.</p>
	 *
	 * <p>Konsekuensi praktis: memanggil getter ini di dalam loop besar (mis. saat merender
	 * daftar tugas) berarti satu pencarian pertemuan per baris. Jalur inkonsistensi ini
	 * tampaknya tidak disengaja — tiga saudaranya
	 * ({@link #getPerkuliahan()}, {@link #getKelompokKkn()}, {@link #getKelompokPkl()}) memakai
	 * {@link #getPertemuanData()} yang jauh lebih murah, dicatat apa adanya tanpa diperbaiki.</p>
	 *
	 * @return jadwal pelajaran induk, atau {@code null} bila bukan konteks sekolah
	 * @see #ambilPertemuan()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jadwal_pelajaran", nullable = true)
	public JadwalPelajaran getJadwalPelajaran() {
		if (getPertemuan() != null) {
			Pertemuan pertemuan = ambilPertemuan();
			if (pertemuan != null && pertemuan.getJadwalPelajaran() != null) {
				jadwalPelajaran = pertemuan.getJadwalPelajaran();
			}
		}
		jadwalPelajaran = check(jadwalPelajaran);
		return jadwalPelajaran;
	}

	/**
	 * Setel jadwal pelajaran induk.
	 *
	 * <p>Bisa ditimpa kembali oleh {@link #getJadwalPelajaran()} bila pertemuan induk punya
	 * jadwal pelajaran sendiri.</p>
	 *
	 * @param jadwalPelajaran jadwal pelajaran; boleh {@code null}
	 */
	public void setJadwalPelajaran(JadwalPelajaran jadwalPelajaran) {
		this.jadwalPelajaran = jadwalPelajaran;
	}

	/**
	 * Daftar peserta yang DIKECUALIKAN dari tugas ini, sebagai string CSV.
	 *
	 * <p><b>Format.</b> Nilai dikembalikan dalam bentuk "koma-terbungkus":
	 * {@code ",12,34,56,"} — setiap id diapit koma di kedua sisi. Bentuk itu sengaja dipilih
	 * agar pengujian keanggotaan cukup dengan pencocokan substring
	 * {@code getMhsYgTidakIkut().contains("," + id + ",")}, tanpa perlu memecah string; lihat
	 * pemakaiannya di {@link Tugas#ambilTugasFileContentTotal()} yang menyaring berkas jawaban
	 * milik peserta yang dikecualikan. Daftar kosong dikembalikan sebagai string kosong, bukan
	 * {@code ","}.</p>
	 *
	 * <p><b>Getter ini TIDAK murni: ia MENULIS ke field yang dipetakan.</b> Setiap pemanggilan
	 * menormalkan ulang isi field {@link #mhsYgTidakIkut} (menambahkan koma pembungkus dan
	 * meruntuhkan koma ganda lewat tiga kali {@code replaceAll(",,", ",")}) lalu menyimpannya
	 * kembali. Artinya membaca saja dapat menandai entity <i>dirty</i> dan memicu {@code UPDATE}
	 * kolom {@code mhsYgTidakIkut} saat flush, meski tidak ada yang bermaksud mengubah data.
	 * Normalisasinya idempoten, jadi pemanggilan berulang menghasilkan nilai yang sama.</p>
	 *
	 * <p><b>Batas yang perlu diwaspadai:</b> penghapusan koma ganda dilakukan tepat tiga kali,
	 * bukan sampai konvergen. Rentetan koma yang lebih panjang dari itu (mis. hasil suntingan
	 * manual di basis data) tidak akan bersih seluruhnya.</p>
	 *
	 * @return CSV id peserta yang dikecualikan dalam bentuk koma-terbungkus; string kosong bila
	 *         tidak ada, tidak pernah {@code null}
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
	 * Setel daftar peserta yang dikecualikan.
	 *
	 * <p>Disimpan apa adanya tanpa normalisasi; pembungkusan koma baru terjadi saat
	 * {@link #getMhsYgTidakIkut()} dipanggil.</p>
	 *
	 * @param mhsYgTidakIkut CSV id peserta; boleh {@code null} atau tanpa koma pembungkus
	 */
	public void setMhsYgTidakIkut(String mhsYgTidakIkut) {
		this.mhsYgTidakIkut = mhsYgTidakIkut;
	}

	/**
	 * Memuat object {@link Pertemuan} induk berdasarkan id mentah {@link #getPertemuan()}.
	 *
	 * <p>Berbeda dari {@link #getPertemuanData()} yang hanya mengembalikan relasi yang sudah
	 * dipetakan Hibernate, method ini <b>aktif mencari</b> lewat
	 * {@link GeneralValueObject#ambilData(Class, String)} — konstanta in-memory dulu, lalu cache
	 * MapDB, tanpa fallback basis data (overload dua-argumen memakai
	 * {@code jikaNggakKetemucari = false}). Karena itu hasilnya bisa {@code null} untuk pertemuan
	 * yang belum masuk cache walaupun barisnya ada di basis data.</p>
	 *
	 * <p>Bukan properti Hibernate (namanya tidak berawalan {@code get}), jadi tidak dipetakan ke
	 * kolom mana pun. Dipanggil dari {@link #getJadwalPelajaran()}.</p>
	 *
	 * @return pertemuan induk dari cache, atau {@code null} bila id kosong atau tidak ditemukan
	 * @see #getPertemuanData()
	 */
	public Pertemuan ambilPertemuan() {
		return getPertemuan() == null ? null
				: (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, getPertemuan().toString());
	}

	/**
	 * Id {@link Pertemuan} induk sebagai angka mentah.
	 *
	 * <p>Inilah SATU-SATUNYA sisi kolom {@code pertemuan} yang boleh ditulis. Relasi objeknya,
	 * {@link #getPertemuanData()}, memetakan kolom yang sama dengan
	 * {@code insertable = false, updatable = false} — pemetaan ganda semacam ini wajib punya
	 * tepat satu sisi yang bisa menulis agar Hibernate tidak menolak konfigurasinya.</p>
	 *
	 * @return id pertemuan induk, atau {@code null} bila tugas belum terkait pertemuan
	 * @see #getPertemuanData()
	 * @see #ambilPertemuan()
	 */
	@Column(name = "pertemuan")
	public Long getPertemuan() {
		return pertemuan;
	}

	/**
	 * Setel id pertemuan induk — cara YANG BENAR untuk memindahkan tugas ke pertemuan lain.
	 *
	 * <p>Menyetel {@link #setPertemuanData(Pertemuan)} saja tidak akan pernah tersimpan karena
	 * sisi relasi itu baca-saja.</p>
	 *
	 * @param pertemuan id pertemuan induk; boleh {@code null}
	 */
	public void setPertemuan(Long pertemuan) {
		this.pertemuan = pertemuan;
	}

	/**
	 * Judul tugas menurut kontrak abstrak {@link Tugas} — alias BACA-SAJA untuk kolom
	 * {@code judul}.
	 *
	 * <p>Dipetakan ke kolom yang sama dengan {@link #getJudul()} tetapi dengan
	 * {@code insertable = false, updatable = false}, karena satu kolom tidak boleh punya dua
	 * properti yang sama-sama bisa menulis. Yang menulis adalah {@link #setJudul(String)} —
	 * atau {@link #setJudultugas(String)}, yang juga menulis ke field {@link #judul} yang sama.</p>
	 *
	 * <p>Berbeda dari {@link #getJudul()}, getter ini menormalkan {@code null} menjadi string
	 * kosong dan mem-{@code trim} hasilnya, sehingga aman dipakai langsung di UI dan di
	 * {@link #getAktif()}.</p>
	 *
	 * @return judul tugas yang sudah di-trim; tidak pernah {@code null}
	 */
	@Override
	@Column(name = "judul", columnDefinition = "text", insertable = false, updatable = false)
	public String getJudultugas() {
		// TODO Auto-generated method stub
		return judul == null ? "" : judul.trim();
	}

	/**
	 * Setel judul tugas lewat nama properti kontrak {@link Tugas}.
	 *
	 * <p>Menulis ke field {@link #judul} yang sama dengan {@link #setJudul(String)} — keduanya
	 * setara.</p>
	 *
	 * @param judultugas judul tugas
	 */
	@Override
	public void setJudultugas(String judultugas) {
		this.judul = judultugas;
	}

	/**
	 * Setel isi tugas lewat nama properti kontrak {@link Tugas}.
	 *
	 * <p>Menulis ke field {@link #nama} yang sama dengan {@link #setNama(String)}.</p>
	 *
	 * @param isitugas isi/uraian tugas
	 */
	@Override
	public void setIsitugas(String isitugas) {
		this.nama = isitugas;
	}

	/**
	 * Isi tugas menurut kontrak abstrak {@link Tugas} — alias BACA-SAJA untuk kolom
	 * {@code nama}.
	 *
	 * <p>Pola pemetaan gandanya sama persis dengan {@link #getJudultugas()}. Perhatikan bedanya
	 * dengan {@link #getNama()}: getter ini mengembalikan field apa adanya (bisa {@code null},
	 * tanpa trim), sedangkan {@link #getNama()} menormalkannya.</p>
	 *
	 * @return isi tugas apa adanya, mungkin {@code null}
	 */
	@Override
	@Column(name = "nama", columnDefinition = "text", insertable = false, updatable = false)
	public String getIsitugas() {
		// TODO Auto-generated method stub
		return nama;
	}

	/**
	 * Daftar peserta yang DIIZINKAN mengunggah ulang jawaban, sebagai string CSV.
	 *
	 * <p>Secara bawaan setiap peserta hanya boleh mengumpulkan sekali; id yang tercantum di sini
	 * mendapat izin unggah ulang (biasanya diberikan dosen setelah peserta salah kirim berkas).</p>
	 *
	 * <p>Format, sifat tidak-murni, dan batas normalisasinya <b>identik dengan</b>
	 * {@link #getMhsYgTidakIkut()} — kode kedua method ini adalah salinan satu sama lain, hanya
	 * berbeda nama field. Berlaku peringatan yang sama: membaca getter ini menulis balik ke field
	 * yang dipetakan dan dapat memicu {@code UPDATE} saat flush.</p>
	 *
	 * @return CSV id peserta yang boleh unggah ulang dalam bentuk koma-terbungkus; string kosong
	 *         bila tidak ada, tidak pernah {@code null}
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
	 * Setel daftar peserta yang boleh mengunggah ulang.
	 *
	 * <p>Disimpan apa adanya; normalisasi baru terjadi saat
	 * {@link #getMhsBolehUploadUlang()} dipanggil.</p>
	 *
	 * @param mhsBolehUploadUlang CSV id peserta; boleh {@code null}
	 */
	public void setMhsBolehUploadUlang(String mhsBolehUploadUlang) {
		this.mhsBolehUploadUlang = mhsBolehUploadUlang;
	}

	/**
	 * Prasyarat MEMBUKA tugas ini, dalam bentuk teks JSON.
	 *
	 * <p><b>Bentuk data.</b> Sebuah JSON objek yang kuncinya adalah penanda item konten
	 * e-Learning yang harus diselesaikan lebih dulu, berformat
	 * {@code "&lt;id&gt;_&lt;NamaKelasSederhana&gt;"} — misalnya
	 * {@code "1234_PertemuanFileContent"} (materi harus dibuka),
	 * {@code "88_TugasPertemuan"} (tugas mandiri harus dikumpulkan), atau
	 * {@code "7_PertemuanPunyaUjian"} (ujian harus dikerjakan). Pemeriksaannya dilakukan
	 * {@code ProfileUtil.chekSyarat(...)}, yang menampilkan pesan berisi daftar syarat yang
	 * belum terpenuhi dan menolak akses.</p>
	 *
	 * <p>Nilai kosong dinormalkan menjadi {@code "{}"} (JSON objek kosong) sehingga pemanggil
	 * selalu aman langsung mem-parsingnya dengan {@link JSONObject} tanpa memeriksa
	 * {@code null} — dan JSON objek kosong berarti "tanpa prasyarat, akses bebas".</p>
	 *
	 * <p>Jangan tertukar dengan {@link #getSyaratMengumpulkanTugas()} yang mengatur syarat
	 * MENGUMPULKAN, bukan syarat membuka.</p>
	 *
	 * @return teks JSON prasyarat akses; {@code "{}"} bila tanpa prasyarat, tidak pernah
	 *         {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getSyaratAkses() {
		return syaratAkses == null || syaratAkses.trim().isEmpty() ? new JSONObject().toString() : syaratAkses;
	}

	/**
	 * Setel prasyarat akses.
	 *
	 * @param syaratAkses teks JSON prasyarat; {@code null}/kosong berarti tanpa prasyarat
	 * @see #getSyaratAkses()
	 */
	public void setSyaratAkses(String syaratAkses) {
		this.syaratAkses = syaratAkses;
	}

	/**
	 * Jenis item penilaian siswa (modul SEKOLAH).
	 *
	 * <p>Diresolusi lewat {@link GeneralValueObject#check(Object)}. Bersama
	 * {@link #getGrupPenilaian()} dan {@link #getGrupKategoriItemPenilaianSiswa()} membentuk
	 * klasifikasi tiga tingkat rapor sekolah; ketiganya {@code null} pada instalasi perguruan
	 * tinggi.</p>
	 *
	 * @return jenis item penilaian, atau {@code null} bila bukan konteks sekolah
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_item_penilaian_siswa", nullable = true)
	public JenisItemPenilaianSiswa getJenisItemPenilaianSiswa() {
		jenisItemPenilaianSiswa = check(jenisItemPenilaianSiswa);
		return jenisItemPenilaianSiswa;
	}

	/**
	 * Setel jenis item penilaian siswa.
	 *
	 * @param jenisItemPenilaianSiswa jenis item penilaian; boleh {@code null}
	 */
	public void setJenisItemPenilaianSiswa(JenisItemPenilaianSiswa jenisItemPenilaianSiswa) {
		this.jenisItemPenilaianSiswa = jenisItemPenilaianSiswa;
	}

	/**
	 * Grup kategori item penilaian siswa (modul SEKOLAH) — tingkat klasifikasi terluar.
	 *
	 * <p>Diresolusi lewat {@link GeneralValueObject#check(Object)}.</p>
	 *
	 * @return grup kategori item penilaian, atau {@code null} bila bukan konteks sekolah
	 * @see #getJenisItemPenilaianSiswa()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "grup_kategori_item_penilaian_siswa", nullable = true)
	public GrupKategoriItemPenilaianSiswa getGrupKategoriItemPenilaianSiswa() {
		grupKategoriItemPenilaianSiswa = check(grupKategoriItemPenilaianSiswa);
		return grupKategoriItemPenilaianSiswa;
	}

	/**
	 * Setel grup kategori item penilaian siswa.
	 *
	 * @param grupKategoriItemPenilaianSiswa grup kategori; boleh {@code null}
	 */
	public void setGrupKategoriItemPenilaianSiswa(GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa) {
		this.grupKategoriItemPenilaianSiswa = grupKategoriItemPenilaianSiswa;
	}

	/**
	 * Grup penilaian (modul SEKOLAH) — tingkat klasifikasi menengah.
	 *
	 * <p>Diresolusi lewat {@link GeneralValueObject#check(Object)}.</p>
	 *
	 * @return grup penilaian, atau {@code null} bila bukan konteks sekolah
	 * @see #getJenisItemPenilaianSiswa()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "grup_penilaian", nullable = true)
	public GrupPenilaian getGrupPenilaian() {
		grupPenilaian = check(grupPenilaian);
		return grupPenilaian;
	}

	/**
	 * Setel grup penilaian.
	 *
	 * @param grupPenilaian grup penilaian; boleh {@code null}
	 */
	public void setGrupPenilaian(GrupPenilaian grupPenilaian) {
		this.grupPenilaian = grupPenilaian;
	}

	/**
	 * Nilai bawaan untuk kolom formula/nilai penilaian: sebuah JSON objek kosong
	 * ({@code "{}"}).
	 *
	 * <p><b>Perhatikan:</b> {@code public static} TANPA {@code final}, sehingga isinya dapat
	 * diubah dari mana saja di dalam JVM dan perubahan itu langsung memengaruhi nilai bawaan
	 * yang dikembalikan {@link #getKeteranganNilaiLama()} untuk SELURUH baris. Konstanta serupa
	 * dengan sifat yang sama juga ada di {@link Pertemuan#DEFAULT_FORMULA} dan
	 * {@link Tugas#JSON}.</p>
	 *
	 * @see #getKeteranganNilaiLama()
	 */
	public static String DEFAULT_FORMULA = new JSONObject().toString();

	/**
	 * Nilai/formula penilaian versi LAMA, tersimpan di kolom {@code keterangannilai}.
	 *
	 * <p>Dipertahankan demi kompatibilitas data yang belum dimigrasikan ke kolom baru. Jalur
	 * baca yang benar adalah {@link #getKeteranganNilai()}, yang memakai kolom baru bila terisi
	 * dan jatuh ke kolom ini bila tidak.</p>
	 *
	 * <p>Nilai kosong dikembalikan sebagai {@link #DEFAULT_FORMULA} sehingga pemanggil selalu
	 * aman mem-parsingnya sebagai JSON.</p>
	 *
	 * @return teks JSON nilai penilaian lama; {@code "{}"} bila belum ada
	 * @see #getKeteranganNilai()
	 */
	@Column(columnDefinition = "text", name = "keterangannilai")
	public String getKeteranganNilaiLama() {
		return keteranganNilaiLama == null || keteranganNilaiLama.trim().isEmpty() ? DEFAULT_FORMULA
				: keteranganNilaiLama;
	}

	/**
	 * Setel nilai/formula penilaian versi LAMA.
	 *
	 * <p>Kode baru sebaiknya memakai {@link #setKeteranganNilai(String)}; kolom lama hanya
	 * disentuh oleh jalur migrasi.</p>
	 *
	 * @param keteranganNilaiLama teks JSON nilai penilaian
	 * @see #getKeteranganNilaiLama()
	 */
	public void setKeteranganNilaiLama(String keteranganNilaiLama) {
		this.keteranganNilaiLama = keteranganNilaiLama;
	}

	/**
	 * Nilai penilaian yang BERLAKU untuk tugas kelompok ini — teks JSON berisi nilai OBE per
	 * mahasiswa per komponen.
	 *
	 * <p><b>Bentuk data.</b> JSON objek dengan kunci
	 * {@code "&lt;idMahasiswa&gt;_mhs_nilai_&lt;idFormatNilai&gt;"} dan nilai berupa angka
	 * ({@code double}); kunci yang tidak ada berarti nilai {@code 0.0}. Karena SELURUH nilai
	 * seluruh mahasiswa tersimpan dalam satu string di satu baris, nilai per komponen tidak bisa
	 * di-query lewat SQL dan penyimpanan bersamaan oleh dua pengguna saling menimpa. Uraian
	 * lengkap beserta daftar pemanggilnya ada di Javadoc kelas.</p>
	 *
	 * <p><b>Perpindahan kolom disembunyikan di sini:</b> nilai diambil dari kolom baru
	 * {@code keterangan_nilai_baru} bila terisi, dan jatuh ke kolom lama
	 * ({@link #getKeteranganNilaiLama()}) bila tidak. Karena fallback itu pula, getter ini tidak
	 * pernah mengembalikan {@code null} maupun string kosong — paling buruk {@code "{}"}.</p>
	 *
	 * <p>Menimpa properti abstrak bernama sama dari {@link Tugas}.</p>
	 *
	 * @return teks JSON nilai yang berlaku; {@code "{}"} bila kedua kolom kosong
	 * @see #getKeteranganNilaiLama()
	 * @see #getFormatNilais()
	 */
	@Override
	@Column(columnDefinition = "text", name = "keterangan_nilai_baru")
	public String getKeteranganNilai() {
		return keteranganNilai == null || keteranganNilai.trim().isEmpty() ? getKeteranganNilaiLama() : keteranganNilai;
	}

	/**
	 * Setel nilai penilaian pada kolom BARU {@code keterangan_nilai_baru}.
	 *
	 * <p>Kolom lama tidak ikut diubah sehingga isinya tetap tersimpan sebagai cadangan. Pemanggil
	 * bertanggung jawab menyusun JSON yang utuh: karena satu string memuat nilai SEMUA mahasiswa,
	 * menulis JSON parsial akan menghapus nilai mahasiswa lain.</p>
	 *
	 * @param keteranganNilai teks JSON nilai per mahasiswa per komponen
	 * @see #getKeteranganNilai()
	 */
	@Override
	public void setKeteranganNilai(String keteranganNilai) {
		this.keteranganNilai = keteranganNilai;
	}

	/**
	 * Daftar komponen {@link FormatNilai} yang dinilai pada tugas ini — teks JSON, sekaligus
	 * <b>saklar mode OBE</b>.
	 *
	 * <p><b>Bentuk data.</b> JSON objek yang kuncinya adalah id {@link FormatNilai} (komponen
	 * capaian pembelajaran/CPMK) dan nilainya bobot komponen tersebut. Hanya komponen yang
	 * kuncinya ada di sini yang ikut dinilai.</p>
	 *
	 * <p><b>Peran sebagai saklar.</b> Pemanggil di seluruh repo memakai perbandingan
	 * {@code getFormatNilais().equalsIgnoreCase(Tugas.JSON)} untuk memutuskan mode penilaian:
	 * bila hasilnya masih literal {@code "{}"}, tugas dianggap memakai penilaian STANDAR (satu
	 * angka lewat {@link #getFormatNilai()}); bila sudah berisi komponen, tugas masuk mode OBE
	 * dan nilainya dibaca dari {@link #getKeteranganNilai()}. Lihat
	 * {@code NamaTugasKelompokHelper.ambilObeFormatNilais()},
	 * {@code NamaTugasKelompokPunyaMahasiswaHelper}, dan
	 * {@code RekapHasilTugasPerTugasDanUjianObe}.</p>
	 *
	 * <p>Nilai kosong dinormalkan menjadi {@link Tugas#JSON} — perhatikan bahwa {@link Tugas#JSON}
	 * juga {@code public static} non-{@code final}, jadi konstanta pembanding itu sendiri secara
	 * teknis bisa diubah saat runtime.</p>
	 *
	 * @return teks JSON komponen nilai; {@link Tugas#JSON} ({@code "{}"}) bila belum diatur,
	 *         tidak pernah {@code null}
	 * @see #getKeteranganNilai()
	 * @see #getFormatNilai()
	 */
	@Column(columnDefinition = "text")
	public String getFormatNilais() {
		return formatNilais == null || formatNilais.trim().isEmpty() ? JSON : formatNilais;
	}

	/**
	 * Setel daftar komponen nilai (JSON) — sekaligus MENGAKTIFKAN atau mematikan mode OBE untuk
	 * tugas ini.
	 *
	 * <p>Menyetel nilai selain {@code "{}"} membuat seluruh pembaca beralih membaca nilai dari
	 * {@link #getKeteranganNilai()}; mengosongkannya kembali membuat nilai OBE yang sudah
	 * tersimpan di sana tidak lagi terbaca (datanya tetap ada, hanya tidak dipakai).</p>
	 *
	 * @param formatNilais teks JSON komponen nilai
	 * @see #getFormatNilais()
	 */
	public void setFormatNilais(String formatNilais) {
		this.formatNilais = formatNilais;
	}

	/**
	 * Object {@link Pertemuan} induk — sisi relasi BACA-SAJA dari kolom {@code pertemuan}.
	 *
	 * <p>Memetakan kolom yang sama dengan {@link #getPertemuan()}, tetapi dengan
	 * {@code insertable = false, updatable = false}: perubahan pada properti ini TIDAK pernah
	 * disimpan. Gunakan {@link #setPertemuan(Long)} untuk memindahkan tugas ke pertemuan lain.</p>
	 *
	 * <p>{@code @NotFound(IGNORE)} membuat baris yatim (id pertemuan menunjuk baris yang sudah
	 * terhapus) menghasilkan {@code null} alih-alih {@code EntityNotFoundException} — kondisi
	 * yang nyata terjadi di data lama, sehingga pemanggil wajib memeriksa {@code null} meskipun
	 * {@link #getPertemuan()} berisi angka.</p>
	 *
	 * <p>Getter ini murni (tidak menulis apa pun) dan tidak memanggil
	 * {@link GeneralValueObject#check(Object)}, sehingga nilainya bisa berupa proxy Hibernate.
	 * Ia dipanggil dari {@link #getPerkuliahan()}, {@link #getKelompokKkn()}, dan
	 * {@link #getKelompokPkl()} — yang justru menjadikan ketiga getter itu tidak murni.</p>
	 *
	 * @return pertemuan induk, atau {@code null} bila tidak terkait / barisnya sudah hilang
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
	 * <p><b>Tidak berpengaruh pada basis data</b> — sisi relasi ini baca-saja. Berguna hanya
	 * untuk menghindari pemuatan ulang di dalam satu proses (dan, sebagai efek sampingnya,
	 * mengubah apa yang diturunkan {@link #getPerkuliahan()}, {@link #getKelompokKkn()}, dan
	 * {@link #getKelompokPkl()}). Untuk mengubah data, pakai {@link #setPertemuan(Long)}.</p>
	 *
	 * @param pertemuanData pertemuan induk; boleh {@code null}
	 */
	public void setPertemuanData(Pertemuan pertemuanData) {
		this.pertemuanData = pertemuanData;
	}

	/**
	 * Penanda apakah tugas ini "aktif".
	 *
	 * <p><b>Nilai turunan, bukan nilai tersimpan.</b> Setiap pemanggilan menghitung ulang
	 * {@code aktif = !getJudultugas().isEmpty()} — tugas dianggap aktif semata-mata karena
	 * judulnya terisi — lalu <b>menulis hasilnya ke field {@link #aktif}</b>. Karena field itu
	 * tidak ditandai {@code @Transient}, Hibernate memetakannya sebagai properti biasa; membaca
	 * getter ini karenanya dapat menandai entity <i>dirty</i> dan menimpa nilai kolomnya di basis
	 * data. Apa pun yang pernah diset lewat {@link #setAktif(Boolean)} tidak akan pernah bertahan
	 * melewati pembacaan berikutnya.</p>
	 *
	 * <p>Implementasi ini identik baris demi baris dengan
	 * {@code TugasPertemuan.getAktif()}. Perhatikan bahwa maknanya BERBEDA dari
	 * {@link Pertemuan#getAktif()}, yang merupakan penanda hapus-lunak sungguhan dan tersimpan
	 * di basis data.</p>
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
