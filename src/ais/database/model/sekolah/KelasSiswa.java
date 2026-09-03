package ais.database.model.sekolah;
// Generated 30 Sep 18 6:57:43 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
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

import org.apache.commons.lang.StringUtils;
import org.hibernate.envers.Audited;
import org.json.JSONArray;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.Ruang;
import ais.database.model.Statusabsensi;
import ais.database.model.Tbmuser;
import ais.database.model.VoKunci;
import ais.database.model.sop.DisposisiSop;

/**
 * Entity INDUK kelas/rombongan belajar sekolah — satu baris tabel
 * {@code sekolah.kelas} mewakili satu rombongan belajar pada satu tahun ajaran,
 * misal "VII-A 2025/2026".
 *
 * <h3>Peran dalam modul sekolah</h3>
 * <p>Kelas ini adalah simpul induk (parent) dari roster kelas. Anggotanya
 * <b>tidak</b> disimpan sebagai koleksi di sini melainkan sebagai baris-baris
 * {@code ais.database.model.sekolah.KelasSiswaPunyaSiswa} yang menunjuk BALIK ke
 * kelas ini lewat kolom {@code kelas_siswa} — jadi arah relasi sesungguhnya adalah
 * <b>anak → induk</b> saja (unidirectional {@code ManyToOne} dari sisi roster).
 * Entity induk ini sama sekali tidak punya properti {@code Set&lt;KelasSiswaPunyaSiswa&gt;};
 * setiap pemanggil yang butuh daftar anggota harus membuat query sendiri
 * ({@code createCriteria(KelasSiswaPunyaSiswa.class).add(Restrictions.eq("kelasSiswa", kelas))}),
 * pola yang memang dipakai di {@code KelasSiswaAction}, {@code DetailKelasSiswaHelper},
 * {@code AbsensiSiswaHelper}, {@code DetailPenilaianSiswaHelper}, dan puluhan
 * laporan. Konsekuensinya: <b>tidak ada cascade delete</b> dari kelas ke roster —
 * menghapus kelas lewat layar master akan gagal karena FK, bukan menghapus
 * anggotanya diam-diam.</p>
 *
 * <p>Selain roster, kelas ini dirujuk sebagai FK oleh (antara lain)
 * {@code Siswa} (kolom {@code current_kelas_id}, "kelas aktif siswa saat ini"),
 * {@code JadwalPelajaran}, {@code AbsenPiket}, {@code CatatanKelasSiswa},
 * {@code CatatanSiswa}, {@code PrestasiSiswa}, {@code Tagihan},
 * {@code PembayaranSiswaDetail}, {@code PengaturanBiaya}, {@code KelasSiswaPSB},
 * {@code CalonSiswa}, {@code VirtualAccountBank}, {@code ParameterTambahan},
 * serta modul antar-jemput. Total ± 124 berkas Java merujuk tipe ini, 52 di
 * antaranya berada di paket model — menjadikannya salah satu entity paling
 * sentral pada modul sekolah, setara {@code Siswa} dan {@code Matapelajaran}.</p>
 *
 * <h3>Pengelompokan anggota kelas ini</h3>
 * <ol>
 *   <li><b>Jejak audit &amp; kunci</b> — {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()}, {@link #onUpdate()}, {@link #getDikunci()},
 *       {@link #getDisposisiSop()}. Field {@code oleh}/{@code olehId}/
 *       {@code tanggal_dirubah}/{@code id} <b>sengaja dideklarasikan ulang</b> di
 *       sini; lihat catatan "Warisan {@code GeneralValueObject}" di bawah.</li>
 *   <li><b>Identitas kelas</b> — {@link #getNama()} beserta tiga varian bahasa
 *       {@link #getNamaEn()}/{@link #getNamaAr()}/{@link #getNamaCh()},
 *       {@link #getTingkat()}, {@link #getKeterangan()}, {@link #getAktif()},
 *       {@link #getTahunAjaran()}.</li>
 *   <li><b>Penempatan organisasi</b> — {@link #getSekolah()}, {@link #getYayasan()},
 *       {@link #getRuang()}, {@link #getKurikulumSekolah()}.</li>
 *   <li><b>Penanggung jawab</b> — {@link #getGuruPembina()} (wali kelas) dan
 *       {@link #getGuruBk()} (guru BK).</li>
 *   <li><b>Kebijakan absensi &amp; penilaian</b> — {@link #getAbsensiharusGuruPembina()},
 *       {@link #getAbsensiharusGuruBk()}, {@link #getPublikasiNilaiHarusTelahDiverifikasi()},
 *       {@link #getGuruBolehMemverifikasiSendiri()}. Keempatnya benar-benar
 *       ditegakkan di layar lain (lihat Javadoc masing-masing), bukan sekadar
 *       penanda kosmetik.</li>
 *   <li><b>Blob absensi piket</b> — {@link #getAbsensi()}, {@link #populate},
 *       dan tujuh pembaca {@code retreiveAbsensi*}. Satu kolom {@code text}
 *       menyimpan banyak catatan kehadiran dalam format CSV bertingkat.</li>
 *   <li><b>Pengecualian mata pelajaran tingkat kelas</b> —
 *       {@link #getMpYgTidakDiambil()}, {@link #ambilMk()}, dan
 *       {@link #filterMk(List, Matapelajaran)}.</li>
 * </ol>
 *
 * <h3>Warisan {@code GeneralValueObject} (jangan "dirapikan")</h3>
 * <p>Rantai pewarisannya {@code KelasSiswa} → {@link VoKunci} →
 * {@code ais.database.model.sop.DataSop} →
 * {@link ais.database.model.GeneralValueObject}. Kelas dasar tersebut
 * <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} — ia POJO abstrak
 * biasa sehingga Hibernate <b>tidak</b> memetakan properti miliknya. Karena itu
 * pengulangan deklarasi field {@code id}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} di berkas ini <b>bukan duplikasi yang perlu dibersihkan,
 * melainkan keharusan teknis</b>: tanpa deklarasi ulang, keempat kolom itu tidak
 * akan ada di tabel.</p>
 *
 * <h3>Hal non-obvious yang perlu diketahui pemanggil</h3>
 * <ul>
 *   <li><b>{@link #getYayasan()} menulis balik.</b> Getter ini menimpa field
 *       {@code yayasan} dengan {@code getSekolah().getYayasan()} setiap kali
 *       dibaca. Yayasan yang di-set eksplisit lewat {@link #setYayasan(Yayasan)}
 *       akan lenyap begitu getter dipanggil, dan karena {@code yayasan} adalah
 *       properti terpetakan, perubahan itu ikut ter-flush ke kolom
 *       {@code yayasan_id}. Praktis: kolom itu adalah turunan dari sekolah, bukan
 *       nilai mandiri.</li>
 *   <li><b>{@link #getAbsensi()} juga menulis balik</b> (perbaikan format jam
 *       "9.400" → "09.40"), dengan konsekuensi flush yang sama.</li>
 *   <li><b>{@link #getTahunAjaran()} berbohong untuk baris lama.</b> Bila kolom
 *       {@code tahunAjaran} {@code null}/kosong, getter mengembalikan tahun
 *       akademik <i>berjalan</i> — nilai yang berubah tiap pergantian tahun dan
 *       tidak pernah ikut tersimpan. Karena {@code DetailKelasSiswaHelper} dan
 *       {@code KelasSiswaAction} memakai perbandingan
 *       {@code getTahunAjaran().equals(Common.getCurrentTahunAkademik())} sebagai
 *       gerbang untuk menulis {@code siswa.current_kelas_id}, kelas dengan tahun
 *       ajaran kosong akan SELALU dianggap kelas tahun berjalan dan merebut kelas
 *       aktif siswa.</li>
 *   <li><b>Penguncian ({@link #getDikunci()}) hanya setengah ditegakkan.</b>
 *       {@code KelasSiswaAction.init()} menyembunyikan tombol "Simpan" pada form
 *       ubah bila kelas terkunci, tetapi tombol Hapus/Copy di baris grid, checkbox
 *       "Aktif", dan SELURUH panel detail roster
 *       ({@code DetailKelasSiswaHelper}) tidak memeriksa {@code dikunci} sama
 *       sekali — kelas terkunci tetap bisa dihapus dan rosternya tetap bisa
 *       dikosongkan.</li>
 *   <li><b>{@link #filterMk(List, Matapelajaran)} adalah kode mati.</b> Tidak ada
 *       satu pun pemanggil di seluruh basis kode; semua layar memakai
 *       {@code KelasSiswaPunyaSiswa.filterMk} yang lebih lengkap.</li>
 * </ul>
 *
 * <h3>Catatan keamanan yang terlihat dari sisi entity induk</h3>
 * <p>Layar master {@code KelasSiswaAction} pada dasarnya <b>bergerbang benar</b>:
 * {@code doBeforeCompose()} memanggil {@code Common.doCheckSecurity()}, tombol
 * Tambah memakai {@code CommonPrivilages.CREATE}, Ubah/Hapus memakai
 * {@code UPDATE}/{@code DELETE}, tombol unggah Excel bahkan menuntut ketiganya
 * sekaligus, dan checkbox "Aktif" per baris dinonaktifkan tanpa hak
 * {@code UPDATE}. Namun ada tiga celah yang tetap perlu dicatat:</p>
 * <ol>
 *   <li><b>Tombol "Singkronkan" tanpa gerbang hak.</b> Tombol ini dipasang tanpa
 *       pemeriksaan privilese apa pun, lalu menjalankan
 *       {@code update sekolah.siswa set current_kelas_id=... } native untuk
 *       seluruh anggota SEMUA kelas pada tahun ajaran terpilih. Filter
 *       yayasan/sekolah-nya <i>fail-open</i>: bila combo dibiarkan kosong,
 *       kondisinya menjadi {@code Restrictions.sqlRestriction("true")} sehingga
 *       jangkauannya seluruh instalasi lintas tenant.</li>
 *   <li><b>Panel detail roster tetap bocor</b> — lihat blok berikutnya.</li>
 *   <li><b>Pewarisan hak lewat menu induk.</b> {@code SiswaAction.onManajemenKelas()}
 *       menyisipkan {@code kelas_siswa.zul} sebagai tab di dalam layar Siswa,
 *       sehingga seluruh layar kelas (termasuk dua celah di atas) ikut terbuka
 *       bagi pemegang hak menu "Siswa" saja.</li>
 * </ol>
 * <p><b>Verifikasi ulang temuan panel detail (b62).</b> Dari sisi induk temuan itu
 * <b>TERKONFIRMASI dan masih berlaku</b>: {@code DetailKelasSiswaHelper} hanya
 * menggerbangi dua tombol ("Ambil Siswa" dengan {@code CREATE}, "Hapus" per baris
 * dengan {@code DELETE}), sedangkan <b>"Bersihkan" (menghapus SELURUH roster
 * kelas), "Copy siswa dari kelas lain", unggah Excel roster, dan {@code Intbox}
 * nomor urut yang menyimpan langsung saat diubah tidak punya gerbang hak sama
 * sekali</b> — hak BACA pada menu Kelas Siswa (atau menu Siswa, lewat pewarisan
 * di atas) sudah cukup untuk mengosongkan rombongan belajar mana pun. Tombol
 * "Edit" pengecualian mata pelajaran pun salah gerbang: memakai {@code DELETE},
 * bukan {@code UPDATE}. Panel itu dibuka dari {@code KelasSiswaRenderer} di layar
 * master ini, jadi celahnya benar-benar terjangkau dari entry point induk —
 * bukan sekadar teoretis.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see VoKunci
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "kelas", schema = "sekolah")
public class KelasSiswa extends VoKunci {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9157912161411433979L;
	/** Kunci utama tabel {@code sekolah.kelas}; dideklarasikan ulang karena kelas dasar tidak dipetakan Hibernate. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (jejak audit); diisi {@code AuditTimestampInterceptor}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini (jejak audit); diisi {@code AuditTimestampInterceptor}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p>Nilai {@code null} atau string kosong <b>diabaikan</b> (field lama
	 * dipertahankan), sehingga jejak audit tidak bisa dihapus dengan menyetel
	 * nilai kosong.</p>
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
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: nilai {@code null}/kosong
	 * diabaikan agar jejak audit tidak terhapus.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mengisi jejak audit ({@code oleh},
	 * {@code olehId}, {@code tanggal_dirubah}) tepat sebelum baris di-{@code UPDATE}.
	 *
	 * <p>Dijalankan otomatis oleh provider JPA/Hibernate, bukan dipanggil kode
	 * aplikasi. Isian diambil dari konteks pengguna aktif oleh
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(Object)}; bila
	 * tidak ada konteks pengguna (mis. proses latar seperti tombol
	 * "Singkronkan"), jejak audit dibiarkan seperti apa adanya.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Waktu perubahan terakhir; diberi nilai awal waktu server saat objek dibuat, lalu diperbarui {@link #onUpdate()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan; umumnya diisi oleh interceptor audit
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada objek baru
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Ruangan fisik tempat rombongan belajar ini berkegiatan; opsional. */
	private Ruang ruang;

	/** Nama kelas dalam bahasa Indonesia, mis. "VII-A"; wajib pada tingkat kolom. */
	private String nama;
	/** Keterangan bebas; ikut ditampilkan sebagai kolom grid dan deskripsi pada combo pemilih kelas. */
	private String keterangan;
	/** Penanda kelas masih dipakai; {@code null} diperlakukan sebagai {@code true}. */
	private Boolean aktif;
	/** Tingkat/jenjang kelas (7, 8, 9, ...); {@code null} dibaca sebagai 0. */
	private Integer tingkat;
	/** Sekolah pemilik kelas ini — batas tenant utama entity ini. */
	private Sekolah sekolah;
	/** Yayasan induk; nilai turunan dari {@link #sekolah}, lihat {@link #getYayasan()}. */
	private Yayasan yayasan;
	/** Tahun ajaran kelas, mis. "2025/2026"; pembeda utama antar-angkatan rombongan belajar. */
	private String tahunAjaran;
	/** Kurikulum sekolah yang dipakai kelas ini; menentukan daftar mata pelajaran yang berlaku. */
	private KurikulumSekolah kurikulumSekolah;
	/** Wali kelas. */
	private Guru guruPembina;
	/** Guru bimbingan konseling yang mendampingi kelas ini. */
	private Guru guruBk;
	/** Blob teks catatan absensi piket tingkat kelas; lihat {@link #getAbsensi()} dan {@link #populate}. */
	private String absensi;
	/** Nama kelas dalam aksara Arab; kosong berarti ikut {@link #getNama()}. */
	private String namaAr;
	/** Nama kelas dalam bahasa Inggris; kosong berarti ikut {@link #getNama()}. */
	private String namaEn;
	/** Nama kelas dalam aksara Tionghoa; kosong berarti ikut {@link #getNama()}. */
	private String namaCh;
	/** String JSON array id mata pelajaran yang tidak diajarkan di kelas ini. */
	private String mpYgTidakDiambil;

	/** Bila {@code true}, hanya wali kelas yang boleh mengabsen kelas ini. */
	private Boolean absensiharusGuruPembina;
	/** Bila {@code true}, hanya guru BK yang boleh mengabsen kelas ini. */
	private Boolean absensiharusGuruBk;
	/** Bila {@code true}, nilai baru boleh tampil ke siswa/wali setelah diverifikasi. */
	private Boolean publikasiNilaiHarusTelahDiverifikasi;
	/** Bila {@code true}, guru pengampu boleh memverifikasi nilainya sendiri. */
	private Boolean guruBolehMemverifikasiSendiri;

	/** Pengguna yang mengunci baris ini; non-{@code null} berarti data terkunci. */
	private Tbmuser dikunci;
	/** Disposisi SOP asal bila baris ini lahir dari alur SOP. */
	private DisposisiSop disposisiSop;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate dan dipakai layar
	 * "Tambah Kelas Siswa" ({@code KelasSiswaAction.onAdd}).
	 */
	public KelasSiswa() {
	}

	/**
	 * Konstruktor lengkap gaya hbm2java untuk membuat objek kelas secara
	 * programatis.
	 *
	 * <p>Menyetel field secara langsung (tanpa lewat setter), sehingga normalisasi
	 * yang ada di setter tidak berlaku. Tidak dipakai oleh layar mana pun saat
	 * ini; disediakan untuk kompatibilitas kode lama/pengujian.</p>
	 *
	 * @param id      kunci utama
	 * @param ruang   ruangan kelas (boleh {@code null})
	 * @param nama    nama kelas
	 * @param tingkat tingkat/jenjang kelas
	 */
	public KelasSiswa(long id, Ruang ruang, String nama, Integer tingkat) {
		this.id = id;
		this.ruang = ruang;
		this.nama = nama;
		this.tingkat = tingkat;
	}

	/**
	 * Mengembalikan kunci utama baris kelas ini.
	 *
	 * <p>Kolom {@code id} bertipe IDENTITY dan ditandai {@code insertable = false}
	 * sehingga tidak ikut disertakan pada {@code INSERT} — nilainya diisi
	 * database. Nilai {@code null} berarti objek belum pernah disimpan (dipakai
	 * {@code KelasSiswaAction.init} untuk memilih judul "Tambah" vs "Ubah").</p>
	 *
	 * @return kunci utama, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama.
	 *
	 * @param id kunci utama; normalnya hanya diisi Hibernate
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan pengguna yang mengunci baris kelas ini.
	 *
	 * <p>Implementasi kontrak {@link VoKunci#getDikunci()}. Relasi {@code LAZY};
	 * getter meresolusi proxy lewat {@code check()} warisan
	 * {@link ais.database.model.GeneralValueObject} sehingga aman dipanggil di
	 * luar session asal.</p>
	 *
	 * <p><b>Penegakan kunci hanya sebagian.</b> Nilai non-{@code null} membuat
	 * {@code KelasSiswaAction.init()} menyembunyikan tombol "Simpan" dan
	 * membekukan form ubah, tetapi tombol Hapus/Copy di grid, checkbox "Aktif",
	 * dan seluruh tombol pada {@code DetailKelasSiswaHelper} tidak memeriksanya
	 * sama sekali.</p>
	 *
	 * @return pengguna pengunci, atau {@code null} bila kelas tidak terkunci
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dikunci")
	public Tbmuser getDikunci() {
		dikunci = check(dikunci);
		return dikunci;
	}

	/**
	 * Menyetel/melepas pengunci baris.
	 *
	 * <p>Dipanggil dari {@code GeneralValueObject.tampilKunci()} saat pengguna
	 * menekan tombol "Kunci"/"Buka Kunci" di grid; penyimpanan dilakukan pemanggil
	 * lewat {@code Common.refreshUpdate}.</p>
	 *
	 * @param dikunci pengguna pengunci, atau {@code null} untuk membuka kunci
	 */
	public void setDikunci(Tbmuser dikunci) {
		this.dikunci = dikunci;
	}

	/**
	 * Mengembalikan disposisi SOP yang melahirkan baris ini, bila kelas dibuat
	 * lewat alur SOP.
	 *
	 * <p>Relasi {@code LAZY}, proxy diresolusi lewat {@code check()}. Bernilai
	 * {@code null} untuk kelas yang dibuat lewat layar master biasa.</p>
	 *
	 * @return disposisi SOP asal, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menyetel disposisi SOP asal.
	 *
	 * <p>Nilai {@code null} atau disposisi tanpa id diabaikan lewat penjagaan awal,
	 * jadi kaitan SOP yang sudah ada tidak bisa dihapus lewat setter ini.</p>
	 *
	 * <p><b>Kuirk:</b> ekspresi ternary sesudah penjagaan awal sebenarnya
	 * <b>mati</b> — pada titik itu {@code disposisiSop} dijamin non-{@code null}
	 * dan ber-id, sehingga syarat kedua ternary selalu {@code false} dan cabang
	 * yang dieksekusi selalu penugasan biasa. Bentuk berbelit ini dipertahankan
	 * apa adanya karena tidak mengubah perilaku.</p>
	 *
	 * @param disposisiSop disposisi SOP; diabaikan bila {@code null} atau belum ber-id
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
	}

	/**
	 * Mengembalikan keterangan bebas kelas.
	 *
	 * @return keterangan, atau {@code null} bila kosong
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel keterangan bebas kelas.
	 *
	 * @param keterangan teks keterangan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan ruangan fisik kelas.
	 *
	 * <p>Relasi {@code LAZY}, proxy diresolusi lewat {@code check()}. Dipakai
	 * antara lain sebagai kolom grid dan sebagai kriteria pencarian
	 * ({@code KelasSiswaAction.initCriteria} mencari nama/kode ruangan lewat
	 * alias {@code ruang}).</p>
	 *
	 * @return ruangan kelas, atau {@code null} bila belum ditentukan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ruang_id", nullable = true)
	public Ruang getRuang() {
		ruang = check(ruang);
		return this.ruang;
	}

	/**
	 * Menyetel ruangan fisik kelas.
	 *
	 * @param ruang ruangan; boleh {@code null}
	 */
	public void setRuang(Ruang ruang) {
		this.ruang = ruang;
	}

	/**
	 * Mengembalikan sekolah pemilik kelas ini.
	 *
	 * <p>Ini adalah batas tenant utama entity ini. Relasi {@code LAZY}, proxy
	 * diresolusi lewat {@code check()}.</p>
	 *
	 * <p><b>Perhatian cakupan tenant:</b> kolom ini <i>tidak</i> ditegakkan pada
	 * sisi query. {@code KelasSiswaAction.initCriteria()} hanya menambahkan filter
	 * sekolah bila combo pencarian dipilih; bila dibiarkan kosong, kondisinya
	 * menjadi {@code Restrictions.sqlRestriction("1=1")} sehingga daftar kelas
	 * mencakup seluruh instalasi. Pembatasan tenant sepenuhnya bergantung pada isi
	 * combo yang disiapkan {@code Common.initYayasanDanSekolahDanSemua()} — sebuah
	 * penjagaan di lapisan tampilan, bukan di lapisan data.</p>
	 *
	 * @return sekolah pemilik, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik kelas.
	 *
	 * <p>Objek {@code Sekolah} tanpa id (mis. hasil pilihan combo "== semua ==")
	 * dinormalkan menjadi {@code null} agar tidak memicu penyimpanan entity
	 * transient lewat cascade {@code PERSIST}.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau tanpa id disimpan sebagai {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Mengembalikan yayasan induk kelas ini.
	 *
	 * <p><b>Getter dengan efek tulis balik (destruktif).</b> Sebelum
	 * mengembalikan nilai, method ini <b>menimpa</b> field {@code yayasan} dengan
	 * {@code getSekolah().getYayasan()} setiap kali dipanggil. Karena
	 * {@code yayasan} adalah properti terpetakan, perubahan itu terdeteksi
	 * dirty-check Hibernate dan ikut ter-flush ke kolom {@code yayasan_id} pada
	 * transaksi berjalan — cukup dengan merender kelas di grid mana pun. Efeknya:
	 * nilai yang di-set lewat {@link #setYayasan(Yayasan)} bersifat sementara
	 * selama sekolah sudah terisi, dan kolom {@code yayasan_id} praktis merupakan
	 * turunan dari sekolah, bukan nilai mandiri.</p>
	 *
	 * <p>Bila {@link #getSekolah()} {@code null}, nilai lama dipertahankan dan
	 * hanya diresolusi proxy-nya lewat {@code check()}.</p>
	 *
	 * @return yayasan induk, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan_id")
	public Yayasan getYayasan() {
		sekolah = getSekolah();
		if (sekolah != null) {
			yayasan = sekolah.getYayasan();
		}
		yayasan = check(yayasan);
		return this.yayasan;
	}

	/**
	 * Menyetel yayasan induk.
	 *
	 * <p>Objek tanpa id dinormalkan menjadi {@code null}, sama seperti
	 * {@link #setSekolah(Sekolah)}. Perhatikan bahwa nilai yang disetel di sini
	 * akan ditimpa {@link #getYayasan()} bila kelas sudah punya sekolah.</p>
	 *
	 * @param yayasan yayasan induk; {@code null} atau tanpa id disimpan sebagai {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Mengembalikan nama kelas (bahasa Indonesia) sudah ter-{@code trim}.
	 *
	 * <p>Nama yang kosong/berisi spasi saja dikembalikan sebagai {@code null}.
	 * Kolomnya {@code nullable = false}, jadi menyimpan kelas tanpa nama akan
	 * memicu pelanggaran constraint di tingkat database — {@code KelasSiswaAction}
	 * mencegahnya lebih dulu dengan validasi form.</p>
	 *
	 * <p><b>Catatan:</b> berbeda dengan {@link #getYayasan()}/{@link #getAbsensi()},
	 * getter ini <b>tidak</b> menulis balik ke field, jadi hasil {@code trim} tidak
	 * ikut tersimpan.</p>
	 *
	 * @return nama kelas ter-{@code trim}, atau {@code null} bila kosong
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama == null || this.nama.trim().isEmpty() ? null : nama.trim();
	}

	/**
	 * Menyetel nama kelas (bahasa Indonesia).
	 *
	 * @param nama nama kelas apa adanya (tanpa normalisasi)
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan tingkat/jenjang kelas.
	 *
	 * <p>Nilai {@code null} dikembalikan sebagai {@code 0} agar pemanggil seperti
	 * {@code KelasSiswaRenderer} bisa langsung memanggil {@code toString()} tanpa
	 * risiko {@code NullPointerException}.</p>
	 *
	 * @return tingkat kelas; {@code 0} bila belum diisi
	 */
	@Column(name = "tingkat", nullable = false)
	public Integer getTingkat() {
		return this.tingkat == null ? 0 : tingkat;
	}

	/**
	 * Menyetel tingkat/jenjang kelas.
	 *
	 * @param tingkat tingkat kelas
	 */
	public void setTingkat(Integer tingkat) {
		this.tingkat = tingkat;
	}

	/**
	 * Mengembalikan status aktif kelas.
	 *
	 * <p>Nilai {@code null} diperlakukan sebagai {@code true} (kelas lama yang
	 * dibuat sebelum kolom ini ada dianggap aktif). Kriteria pencarian di
	 * {@code KelasSiswaAction} dan pada combo pemilih kelas konsisten dengan
	 * bawaan ini: mereka memakai {@code isNull("aktif") OR eq("aktif", true)}.</p>
	 *
	 * @return {@code true} bila kelas masih dipakai
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif kelas.
	 *
	 * <p>Dipanggil langsung dari checkbox "Aktif" pada baris grid
	 * {@code KelasSiswaAction} yang menyimpan seketika lewat
	 * {@code Common.refreshSaveOrUpdate}; checkbox tersebut dinonaktifkan bila
	 * pengguna tidak punya hak {@code UPDATE}.</p>
	 *
	 * @param aktif status aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan tahun ajaran kelas, mis. {@code "2025/2026"}.
	 *
	 * <p><b>Bawaan yang menipu.</b> Bila kolom {@code null}/kosong, yang
	 * dikembalikan adalah tahun akademik <i>berjalan</i>
	 * ({@code Common.getCurrentTahunAkademik()}) — nilai yang berubah setiap
	 * pergantian tahun ajaran dan tidak pernah ikut tersimpan ke kolom. Ini bukan
	 * sekadar kosmetik: {@code DetailKelasSiswaHelper} dan
	 * {@code DetailAbsenPiketHelper} memakai perbandingan
	 * {@code getTahunAjaran().equals(Common.getCurrentTahunAkademik())} sebagai
	 * gerbang untuk menulis {@code siswa.current_kelas_id}, sehingga kelas berkolom
	 * kosong akan SELALU lolos gerbang itu dan bisa merebut "kelas aktif" siswa
	 * dari kelas tahun berjalan yang sebenarnya.</p>
	 *
	 * @return tahun ajaran; tidak pernah {@code null}
	 */
	public String getTahunAjaran() {
		return tahunAjaran == null || tahunAjaran.trim().isEmpty() ? Common.getCurrentTahunAkademik() : tahunAjaran;
	}

	/**
	 * Menyetel tahun ajaran kelas.
	 *
	 * @param tahunAjaran tahun ajaran dalam format {@code "2025/2026"}
	 */
	public void setTahunAjaran(String tahunAjaran) {
		this.tahunAjaran = tahunAjaran;
	}

	/**
	 * Mengembalikan kurikulum sekolah yang dipakai kelas ini.
	 *
	 * <p>Menentukan daftar mata pelajaran yang berlaku: {@code KelasSiswaAction}
	 * dan {@code DetailKelasSiswaHelper} membangun daftar centang "mata pelajaran
	 * yang tidak diajarkan/diikuti" dari {@code KurikulumPunyaMatapelajaran} milik
	 * kurikulum ini. Relasi {@code LAZY}, proxy diresolusi lewat
	 * {@code check()}.</p>
	 *
	 * <p><b>Catatan:</b> {@code DetailKelasSiswaHelper} memanggil
	 * {@code getKurikulumSekolah().getNama()} tanpa penjagaan {@code null},
	 * sehingga kelas tanpa kurikulum membuat dialog pengecualian mata pelajaran
	 * gagal dengan {@code NullPointerException}.</p>
	 *
	 * @return kurikulum sekolah, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kurikulum_sekolah_id")
	public KurikulumSekolah getKurikulumSekolah() {
		kurikulumSekolah = check(kurikulumSekolah);
		return kurikulumSekolah;
	}

	/**
	 * Menyetel kurikulum sekolah kelas ini.
	 *
	 * @param kurikulumSekolah kurikulum yang berlaku
	 */
	public void setKurikulumSekolah(KurikulumSekolah kurikulumSekolah) {
		this.kurikulumSekolah = kurikulumSekolah;
	}

	/**
	 * Mengembalikan wali kelas.
	 *
	 * <p>Relasi {@code LAZY}, proxy diresolusi lewat {@code check()}. Selain
	 * ditampilkan sebagai kolom grid, nilai ini menjadi <b>penentu hak absensi</b>
	 * bila {@link #getAbsensiharusGuruPembina()} bernilai {@code true}: combo
	 * pemilih kelas untuk absensi ({@code AmbilDataKelasSiswaBanbox}) dan
	 * {@code AbsenPiketAction} hanya menampilkan kelas yang wali kelasnya adalah
	 * guru yang sedang login.</p>
	 *
	 * @return wali kelas, atau {@code null} bila belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru_pembina", nullable = true)
	public Guru getGuruPembina() {
		guruPembina = check(guruPembina);
		return guruPembina;
	}

	/**
	 * Menyetel wali kelas.
	 *
	 * @param guruPembina guru wali kelas; boleh {@code null}
	 */
	public void setGuruPembina(Guru guruPembina) {
		this.guruPembina = guruPembina;
	}

	/**
	 * Mengembalikan blob catatan absensi piket tingkat kelas.
	 *
	 * <h4>Format</h4>
	 * <p>Satu kolom {@code text} menampung banyak catatan kehadiran, dipisah
	 * titik-koma antar-catatan dan koma antar-ruas:</p>
	 * <pre>ref,idStatus,kodeStatus,namaStatus,0,keterangan,mulai,sampai,jenis;ref,...</pre>
	 * <p>{@code ref} untuk kelas berbentuk {@code "&lt;idSiswa&gt;_&lt;idAbsenPiket&gt;"}
	 * (lihat {@code DetailAbsenPiketHelper} dan {@code ElearningApiUtil}). Ruas
	 * kelima selalu {@code "0"} (cadangan tak terpakai). Tujuh method
	 * {@code retreiveAbsensi*} membaca ruas-ruas tersebut, {@link #populate}
	 * menuliskannya.</p>
	 *
	 * <h4>Efek samping — getter menulis balik</h4>
	 * <p>Bila blob mengandung teks {@code "9.400"}, getter <b>mengganti</b> isinya
	 * menjadi {@code "09.40"} dan menyimpan hasilnya kembali ke field
	 * {@code absensi}. Karena ini properti terpetakan, perbaikan tersebut ikut
	 * ter-flush ke database pada transaksi berjalan hanya dengan membaca objek.
	 * Perbaikan data lama yang tampaknya disengaja, tetapi tetap perlu diketahui:
	 * pembacaan murni bisa menghasilkan {@code UPDATE}.</p>
	 *
	 * <h4>Peran dalam alur absen piket</h4>
	 * <p>Sumber kebenaran absen piket sesungguhnya adalah {@code AbsenPiketDetail}.
	 * Blob di kelas ini berfungsi ganda: (1) sebagai <b>benih</b> —
	 * {@code AbsenPiketDetail.ambil(..., kelasSiswa.getAbsensi(), ...)} menyalin
	 * seluruh isi blob kelas ke baris detail baru saat baris itu pertama kali
	 * dibuat, sehingga baris milik satu siswa dapat berisi catatan seluruh
	 * teman sekelasnya; dan (2) sebagai <b>cermin</b> — setiap penandaan kehadiran
	 * juga ditulis balik ke sini lewat {@link #populate}. Karena tidak pernah
	 * dipangkas, blob ini tumbuh terus sepanjang tahun ajaran (satu entri per
	 * siswa per sesi piket) di dalam satu baris kelas.</p>
	 *
	 * @return isi blob absensi ter-{@code trim}; string kosong bila belum ada catatan
	 */
	@Column(name = "absensi", columnDefinition = "text")
	public String getAbsensi() {
		if (absensi != null && StringUtils.contains(absensi, "9.400")) {
			absensi = org.apache.commons.lang3.StringUtils.replace(absensi, "9.400", "09.40");
		}
		return absensi == null ? "" : absensi.trim();
	}

	/**
	 * Menyetel blob catatan absensi piket secara utuh.
	 *
	 * <p>Umumnya tidak dipanggil langsung — perubahan per-catatan dilakukan lewat
	 * {@link #populate}. Menyetel nilai sembarang di sini akan menghapus seluruh
	 * riwayat kehadiran yang tersimpan pada kelas.</p>
	 *
	 * @param absensi blob absensi baru
	 */
	public void setAbsensi(String absensi) {
		this.absensi = absensi;
	}

	/**
	 * Mengambil <b>kode</b> status kehadiran (ruas ke-3) untuk satu {@code ref}
	 * dari blob {@link #getAbsensi()}.
	 *
	 * <p>Blob dipecah per titik-koma lalu per koma; catatan pertama yang ruas
	 * pertamanya sama persis dengan {@code ref} yang menang. Kegagalan penguraian
	 * satu catatan (mis. ruas kurang) ditelan dan hanya dicatat ke
	 * {@code ErrorAuditUtil}, lalu penelusuran dilanjutkan ke catatan berikutnya.</p>
	 *
	 * <p>Dipakai internal sebagai pasangan {@link #populate}; pembacaan absen piket
	 * di layar dilakukan pada {@code AbsenPiketDetail}, bukan lewat method ini.</p>
	 *
	 * @param ref kunci catatan, untuk kelas berbentuk {@code "<idSiswa>_<idAbsenPiket>"};
	 *            {@code null} langsung menghasilkan nilai bawaan
	 * @return kode status kehadiran, atau {@code "-"} bila tidak ditemukan
	 */
	public String retreiveAbsensiKode(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					String formatId = (s[0]);
					if (ref.equals(formatId)) {
						return s[2];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/KelasSiswa.java:289");

				}
			}
		}

		return "-";
	}

	/**
	 * Mengambil <b>nama</b> status kehadiran (ruas ke-4) untuk satu {@code ref}
	 * dari blob {@link #getAbsensi()}.
	 *
	 * <p>Mekanismenya identik {@link #retreiveAbsensiKode(String)}: telusuri
	 * catatan sampai ruas pertama cocok, exception per catatan ditelan dan
	 * dicatat.</p>
	 *
	 * @param ref kunci catatan; {@code null} langsung menghasilkan nilai bawaan
	 * @return nama status kehadiran, atau {@code "-"} bila tidak ditemukan
	 */
	public String retreiveAbsensiNama(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					String formatId = (s[0]);
					if (ref.equals(formatId)) {
						return s[3];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/KelasSiswa.java:309");

				}
			}
		}

		return "-";
	}

	/**
	 * Mengambil <b>keterangan</b> kehadiran (ruas ke-6) untuk satu {@code ref}
	 * dari blob {@link #getAbsensi()}.
	 *
	 * <p>Berbeda dengan {@link #retreiveAbsensiKode(String)}, pemecahan memakai
	 * {@code split(",", 9)} sehingga jumlah ruas dibatasi sembilan dan koma yang
	 * tersisa di ruas terakhir tidak memecah data lebih jauh. {@link #populate}
	 * sendiri sudah mengganti koma pada keterangan menjadi garis bawah sebelum
	 * menyimpan, jadi batas ini bersifat pengaman berlapis.</p>
	 *
	 * <p>Dipanggil juga oleh {@link #populate} sebagai nilai jatuh-tempo ketika
	 * argumen keterangan bernilai {@code null} (artinya "pertahankan nilai lama").</p>
	 *
	 * @param ref kunci catatan; {@code null} langsung menghasilkan nilai bawaan
	 * @return keterangan kehadiran, atau string kosong bila tidak ditemukan
	 */
	public String retreiveAbsensiKeterangan(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					String formatId = (s[0]);
					if (ref.equals(formatId)) {
						return s[5];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/KelasSiswa.java:329");

				}
			}
		}

		return "";
	}

	/**
	 * Mengambil <b>id {@link Statusabsensi}</b> (ruas ke-2) untuk satu {@code ref}
	 * dari blob {@link #getAbsensi()}.
	 *
	 * <p>Nilai kembalian biasanya dipakai untuk mencari objek status lewat cache
	 * {@code ConstantValues.ambil(Statusabsensi.class.getName(), id)}. Kegagalan
	 * {@code Long.parseLong} pada catatan rusak ditelan dan dicatat, lalu
	 * penelusuran dilanjutkan.</p>
	 *
	 * @param ref kunci catatan; {@code null} langsung menghasilkan nilai bawaan
	 * @return id status kehadiran, atau {@code -1L} bila tidak ditemukan/rusak
	 */
	public Long retreiveAbsensiId(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					String formatId = (s[0]);
					if (ref.equals(formatId)) {
						return Long.parseLong(s[1]);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/KelasSiswa.java:349");

				}
			}
		}

		return -1L;
	}

	/**
	 * Mengambil <b>jam mulai</b> (ruas ke-7) untuk satu {@code ref} dari blob
	 * {@link #getAbsensi()}.
	 *
	 * <p>Ruas ini hanya terisi untuk status berkode {@code "M"} (masuk);
	 * {@link #populate} mengosongkannya untuk status lain. Dipanggil juga oleh
	 * {@link #populate} sebagai nilai jatuh-tempo saat argumen {@code mulai}
	 * bernilai {@code null}.</p>
	 *
	 * @param ref kunci catatan; {@code null} langsung menghasilkan nilai bawaan
	 * @return jam mulai sebagai teks, atau string kosong bila tidak ditemukan
	 */
	public String retreiveAbsensiMulai(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					String formatId = (s[0]);
					if (ref.equals(formatId)) {
						return s[6];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/KelasSiswa.java:369");

				}
			}
		}

		return "";
	}

	/**
	 * Mengambil <b>jam selesai</b> (ruas ke-8) untuk satu {@code ref} dari blob
	 * {@link #getAbsensi()}.
	 *
	 * <p>Berpasangan dengan {@link #retreiveAbsensiMulai(String)} dan tunduk pada
	 * aturan yang sama (hanya terisi untuk status berkode {@code "M"}).</p>
	 *
	 * @param ref kunci catatan; {@code null} langsung menghasilkan nilai bawaan
	 * @return jam selesai sebagai teks, atau string kosong bila tidak ditemukan
	 */
	public String retreiveAbsensiSampai(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					String formatId = (s[0]);
					if (ref.equals(formatId)) {
						return s[7];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/KelasSiswa.java:389");

				}
			}
		}

		return "";
	}

	/**
	 * Mengambil <b>jenis</b> catatan (ruas ke-9) untuk satu {@code ref} dari blob
	 * {@link #getAbsensi()}.
	 *
	 * <p>Nilai yang ditulis pemanggil saat ini selalu {@code "AbsenPiket"}, dipakai
	 * untuk membedakan asal catatan bila kelak blob dipakai lebih dari satu jenis
	 * absensi.</p>
	 *
	 * @param ref kunci catatan; {@code null} langsung menghasilkan nilai bawaan
	 * @return jenis catatan, atau string kosong bila tidak ditemukan
	 */
	public String retreiveAbsensiJenis(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					String formatId = (s[0]);
					if (ref.equals(formatId)) {
						return s[8];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/KelasSiswa.java:409");

				}
			}
		}

		return "";
	}

	/**
	 * Menulis/memperbarui satu catatan kehadiran di dalam blob
	 * {@link #getAbsensi()}.
	 *
	 * <h4>Alur kerja</h4>
	 * <ol>
	 *   <li>Bila kode status bukan {@code "M"} (masuk), {@code mulai} dan
	 *       {@code sampai} dipaksa menjadi string kosong — jam hadir hanya bermakna
	 *       untuk siswa yang benar-benar masuk.</li>
	 *   <li>Karakter {@code ";"} pada keterangan diganti {@code "..\n"} dan
	 *       {@code ","} diganti {@code "_"} agar tidak merusak pemisah format.
	 *       Perhatikan bahwa penggantian ini <b>tidak dapat dibalik</b>: pembaca
	 *       ({@code DetailAbsenPiketHelper}) mengembalikan {@code "_"} menjadi
	 *       {@code ","}, sehingga garis bawah yang memang diketik pengguna ikut
	 *       berubah menjadi koma saat ditampilkan.</li>
	 *   <li>Seluruh catatan lama ditelusuri. Catatan yang {@code ref}-nya cocok
	 *       ditulis ulang dengan nilai baru; catatan lain disalin apa adanya.
	 *       Himpunan {@code udahAda} membuang <b>catatan duplikat untuk ref yang
	 *       sama</b> (hanya kemunculan pertama yang dipertahankan).</li>
	 *   <li>Bila {@code ref} belum pernah ada, catatan baru ditambahkan di akhir.</li>
	 *   <li>Hasil akhir ditulis <b>langsung ke field</b> {@code absensi} (bukan
	 *       lewat {@link #setAbsensi(String)}).</li>
	 * </ol>
	 *
	 * <p>Argumen {@code keterangan}/{@code mulai}/{@code sampai}/{@code jenis}
	 * bernilai {@code null} berarti "pertahankan nilai lama": nilainya diambil
	 * kembali lewat {@code retreiveAbsensi*} yang bersesuaian. String kosong
	 * <b>bukan</b> {@code null}, jadi mengirim {@code ""} benar-benar mengosongkan
	 * ruas tersebut.</p>
	 *
	 * <p><b>Efek samping:</b> method ini hanya mengubah state objek; penyimpanan
	 * dilakukan pemanggil ({@code Common.refreshUpdate(kelasSiswa)}). Kegagalan
	 * penguraian satu catatan lama ditampilkan lewat
	 * {@code Common.tampilErrorJikaAdmin} dan catatan tersebut dilewati — artinya
	 * catatan rusak bisa hilang senyap dari blob setelah penulisan berikutnya.</p>
	 *
	 * <p><b>Dipanggil dari:</b> {@code DetailAbsenPiketHelper} (layar absen piket
	 * kelas, saat radio kehadiran diubah dan saat pemrosesan massal) serta
	 * {@code ElearningApiUtil} (endpoint absen piket aplikasi mobile). Keduanya
	 * memakai {@code ref = idSiswa + "_" + idAbsenPiket} dan {@code jenis =
	 * "AbsenPiket"}, dan keduanya menulis catatan yang sama ke
	 * {@code AbsenPiketDetail} — blob di kelas berperan sebagai cermin, bukan
	 * sumber kebenaran.</p>
	 *
	 * @param ref           kunci catatan; bila {@code null} method tidak melakukan apa pun
	 * @param statusabsensi status kehadiran yang dicatat; bila {@code null} method
	 *                      tidak melakukan apa pun
	 * @param keterangan    keterangan kehadiran; {@code null} berarti pertahankan nilai lama
	 * @param mulai         jam mulai; {@code null} berarti pertahankan nilai lama,
	 *                      dan diabaikan bila status bukan {@code "M"}
	 * @param sampai        jam selesai; aturan sama dengan {@code mulai}
	 * @param jenis         jenis catatan (mis. {@code "AbsenPiket"}); {@code null}
	 *                      berarti pertahankan nilai lama
	 */
	public void populate(String ref, Statusabsensi statusabsensi, String keterangan, String mulai, String sampai,
			String jenis) {
		if (ref != null && statusabsensi != null) {

			if (statusabsensi.getKode() == null || !statusabsensi.getKode().equals("M")) {
				mulai = "";
				sampai = "";
			}

			keterangan = org.apache.commons.lang3.StringUtils.replace(keterangan, ";", "..\n");
			keterangan = org.apache.commons.lang3.StringUtils.replace(keterangan, ",", "_");
			String formatBaru = "";
			String[] nilais = getAbsensi().split(";");
			Boolean ada = false;
			Set<String> udahAda = new HashSet<String>();
			for (String nn : nilais) {
				try {
					String aformatBaru = "";
					String[] s = nn.split(",");
					if (!s[0].trim().isEmpty()) {
						String formatId = (s[0]);
						if (!udahAda.contains(formatId)) {
							if (ref.equals(formatId)) {
								udahAda.add(formatId);
								aformatBaru = ref + "," + statusabsensi.getId() + "," + statusabsensi.getKode() + ","
										+ statusabsensi.getNama() + ",0,"
										+ (keterangan == null ? retreiveAbsensiKeterangan(ref) : keterangan) + ","
										+ (mulai == null ? retreiveAbsensiMulai(ref) : mulai) + ","
										+ (sampai == null ? retreiveAbsensiSampai(ref) : sampai) + ","
										+ (jenis == null ? retreiveAbsensiJenis(ref) : jenis);
								ada = true;
							} else {
								aformatBaru = nn;
							}
							if (!aformatBaru.trim().isEmpty()) {
								formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
							}
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			if (!ada) {
				String aformatBaru = ref + "," + statusabsensi.getId() + "," + statusabsensi.getKode() + ","
						+ statusabsensi.getNama() + ",0,"
						+ (keterangan == null ? retreiveAbsensiKeterangan(ref) : keterangan) + ","
						+ (mulai == null ? retreiveAbsensiMulai(ref) : mulai) + ","
						+ (sampai == null ? retreiveAbsensiSampai(ref) : sampai) + ","
						+ (jenis == null ? retreiveAbsensiJenis(ref) : jenis);
				formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
			}

			// System.out.println("formatBaru => " + formatBaru);

			absensi = formatBaru;
		}
	}

	/**
	 * Mengembalikan nama kelas dalam bahasa Inggris.
	 *
	 * <p>Bila kolom {@code null}, dikembalikan {@link #getNama()} sebagai
	 * jatuh-tempo. {@code KelasSiswaRenderer} memanfaatkan ini untuk menyembunyikan
	 * label varian bahasa yang isinya sama dengan nama Indonesia.</p>
	 *
	 * <p><b>Kuirk:</b> jatuh-tempo ini <i>materialisasi</i> begitu kelas disunting:
	 * form ubah mengisi kotak isian dengan {@code getNamaEn()} lalu
	 * {@code onSave()} menuliskannya kembali, sehingga kolom yang semula
	 * {@code null} terisi salinan nama Indonesia setelah satu kali Simpan.
	 * Perhatikan pula bahwa string kosong <b>tidak</b> memicu jatuh-tempo (hanya
	 * {@code null} yang memicu).</p>
	 *
	 * @return nama kelas versi Inggris, atau nama Indonesia bila belum diisi
	 */
	public String getNamaEn() {
		return namaEn == null ? getNama() : namaEn;
	}

	/**
	 * Menyetel nama kelas dalam bahasa Inggris.
	 *
	 * @param namaEn nama versi Inggris
	 */
	public void setNamaEn(String namaEn) {
		this.namaEn = namaEn;
	}

	/**
	 * Mengembalikan nama kelas dalam aksara Arab.
	 *
	 * <p>Aturannya sama dengan {@link #getNamaEn()}: jatuh-tempo ke
	 * {@link #getNama()} bila kolom {@code null}. Baris isiannya di form ubah
	 * disembunyikan secara bawaan ({@code row.setVisible(false)}) sehingga kolom
	 * ini praktis hanya terisi lewat unggah data atau instalasi berbahasa Arab.</p>
	 *
	 * @return nama kelas versi Arab, atau nama Indonesia bila belum diisi
	 */
	public String getNamaAr() {
		return namaAr == null ? getNama() : namaAr;
	}

	/**
	 * Menyetel nama kelas dalam aksara Arab.
	 *
	 * @param namaAr nama versi Arab
	 */
	public void setNamaAr(String namaAr) {
		this.namaAr = namaAr;
	}

	/**
	 * Mengembalikan nama kelas dalam aksara Tionghoa.
	 *
	 * <p>Aturan jatuh-tempo dan visibilitas form-nya sama dengan
	 * {@link #getNamaAr()}.</p>
	 *
	 * @return nama kelas versi Tionghoa, atau nama Indonesia bila belum diisi
	 */
	public String getNamaCh() {
		return namaCh == null ? getNama() : namaCh;
	}

	/**
	 * Menyetel nama kelas dalam aksara Tionghoa.
	 *
	 * @param namaCh nama versi Tionghoa
	 */
	public void setNamaCh(String namaCh) {
		this.namaCh = namaCh;
	}

	/**
	 * Mengembalikan guru BK pendamping kelas.
	 *
	 * <p>Relasi {@code LAZY}, proxy diresolusi lewat {@code check()}. Seperti
	 * {@link #getGuruPembina()}, nilai ini menjadi penentu hak absensi bila
	 * {@link #getAbsensiharusGuruBk()} bernilai {@code true}.</p>
	 *
	 * @return guru BK, atau {@code null} bila belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru_bk", nullable = true)
	public Guru getGuruBk() {
		guruBk = check(guruBk);
		return guruBk;
	}

	/**
	 * Menyetel guru BK pendamping kelas.
	 *
	 * @param guruBk guru BK; boleh {@code null}
	 */
	public void setGuruBk(Guru guruBk) {
		this.guruBk = guruBk;
	}

	/**
	 * Konstanta JSON array kosong ({@code "[]"}) yang dipakai sebagai nilai bawaan
	 * {@link #getMpYgTidakDiambil()}.
	 *
	 * <p><b>Catatan:</b> instance ini {@code static} dan berbagi pakai antar-objek;
	 * ia hanya pernah dibaca lewat {@code toString()} dan tidak boleh dimutasi.
	 * Variabel lokal bernama sama di {@link #ambilMk()} <i>menutupi</i> (shadow)
	 * field ini — pola yang sama persis dengan
	 * {@code KelasSiswaPunyaSiswa.array}.</p>
	 */
	final static JSONArray array = new JSONArray();

	/**
	 * Mengembalikan daftar id {@link Matapelajaran} yang <b>tidak diajarkan</b> di
	 * kelas ini, sebagai string JSON array.
	 *
	 * <p>Diisi lewat daftar centang "Matapelajaran yang tidak diajarkan" pada form
	 * ubah kelas ({@code KelasSiswaAction}), yang menyusun daftar dari
	 * {@code KurikulumPunyaMatapelajaran} milik {@link #getKurikulumSekolah()}.
	 * Kolom {@code null}/kosong dikembalikan sebagai {@code "[]"} agar
	 * {@link #ambilMk()} selalu dapat mengurainya.</p>
	 *
	 * <p>Pengecualian setara juga ada pada tingkat siswa
	 * ({@code KelasSiswaPunyaSiswa.getMpYgTidakDiambil()}) dan tingkat jenis rapor
	 * ({@code JenisRaporSiswa}); penggabungan tingkat siswa + tingkat kelas
	 * dilakukan oleh {@code KelasSiswaPunyaSiswa.ambilMk()}, bukan di sini.</p>
	 *
	 * @return string JSON array id mata pelajaran; tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getMpYgTidakDiambil() {
		return mpYgTidakDiambil == null || mpYgTidakDiambil.trim().isEmpty() ? array.toString() : mpYgTidakDiambil;
	}

	/**
	 * Menyetel daftar id mata pelajaran yang tidak diajarkan di kelas ini.
	 *
	 * @param mpYgTidakDiambil string JSON array id mata pelajaran; {@code null}/kosong
	 *                         dibaca sebagai {@code "[]"}
	 */
	public void setMpYgTidakDiambil(String mpYgTidakDiambil) {
		this.mpYgTidakDiambil = mpYgTidakDiambil;
	}

	/**
	 * Menyaring daftar anggota kelas menjadi hanya anggota yang <b>mengambil</b>
	 * mata pelajaran tertentu.
	 *
	 * <p>Untuk tiap anggota, daftar pengecualian gabungan
	 * ({@code KelasSiswaPunyaSiswa.ambilMk()}) diperiksa; anggota dipertahankan
	 * bila id {@code matapelajaran} tidak ada di dalamnya.</p>
	 *
	 * <p><b>KODE MATI — jangan dijadikan acuan.</b> Tidak ada satu pun pemanggil
	 * method ini di seluruh basis kode; semua layar absensi, penilaian, pertemuan,
	 * dan jadwal memakai {@code KelasSiswaPunyaSiswa.filterMk(...)}. Versi di sana
	 * berbeda dalam dua hal penting: (1) ia juga membuang anggota non-aktif
	 * ({@code getAktif() == false}), yang <b>tidak</b> dilakukan versi ini; dan
	 * (2) ia menjaga argumen {@code matapelajaran} yang {@code null} sehingga
	 * mengembalikan daftar kosong, sedangkan versi ini akan melempar
	 * {@code NullPointerException} pada {@code matapelajaran.getId()}. Bila suatu
	 * saat method ini dipakai, kedua perbedaan itu harus disadari.</p>
	 *
	 * @param siswa         daftar anggota kelas yang akan disaring; tidak boleh {@code null}
	 * @param matapelajaran mata pelajaran acuan; tidak boleh {@code null}
	 * @return daftar anggota yang mengambil mata pelajaran tersebut
	 * @see ais.database.model.sekolah.KelasSiswaPunyaSiswa#filterMk(List, Matapelajaran)
	 */
	public static List<KelasSiswaPunyaSiswa> filterMk(List<KelasSiswaPunyaSiswa> siswa, Matapelajaran matapelajaran) {
		List<KelasSiswaPunyaSiswa> kelasSiswaPunyaSiswas = new ArrayList<KelasSiswaPunyaSiswa>();
		for (KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa : siswa) {
			List<Long> longs = kelasSiswaPunyaSiswa.ambilMk();
			if (!longs.contains(matapelajaran.getId())) {
				kelasSiswaPunyaSiswas.add(kelasSiswaPunyaSiswa);
			}
			longs = null;
		}
		return kelasSiswaPunyaSiswas;
	}

	/**
	 * Mengurai {@link #getMpYgTidakDiambil()} menjadi daftar id
	 * {@link Matapelajaran} yang tidak diajarkan di kelas ini.
	 *
	 * <p>Tiap elemen JSON di-{@code parse} menjadi {@code Long} lalu diverifikasi
	 * keberadaannya lewat cache {@code ConstantValues.ambil(...)}. Id yang tidak
	 * lagi merujuk mata pelajaran yang ada <b>dibuang diam-diam</b>, sehingga
	 * daftar hasil tidak pernah memuat id yatim (mis. sisa mata pelajaran yang
	 * sudah dihapus).</p>
	 *
	 * <p>Kedua tingkat {@code try/catch} menelan exception dan hanya mencatatnya:
	 * JSON yang rusak menghasilkan daftar kosong, artinya kelas dianggap
	 * mengajarkan SEMUA mata pelajaran — <i>fail-open</i> yang aman secara akademik
	 * (siswa tetap muncul di absensi/penilaian) tetapi menyembunyikan kerusakan
	 * data.</p>
	 *
	 * <p><b>Dipanggil dari:</b> {@code KelasSiswaAction} (menandai centang saat
	 * membuka form), {@code JadwalPelajaranAction},
	 * {@code RekapitulasiJadwalPelajaranHelper}, {@code RekapitulasiUjianHelper},
	 * {@code PenilaianSiswaAction}, {@code DetailPenilaianSiswaHelper},
	 * {@code LaporanRekapTotalNilai}, {@code ElearningApiUtil}, dan — secara tidak
	 * langsung — {@code KelasSiswaPunyaSiswa.ambilMk()} yang menggabungkan hasil
	 * di sini dengan pengecualian tingkat siswa.</p>
	 *
	 * @return daftar id mata pelajaran yang tidak diajarkan; tidak pernah
	 *         {@code null}, bisa kosong
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
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/KelasSiswa.java:550");
				}
			}
			return longs;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/KelasSiswa.java:554");
		}
		return longs;
	}

	/**
	 * Apakah absensi kelas ini hanya boleh dilakukan wali kelas?
	 *
	 * <p>Bawaannya {@code true} bila kolom {@code null} — jadi kelas lama otomatis
	 * bersifat ketat. Ditegakkan pada {@code AmbilDataKelasSiswaBanbox} (combo
	 * pemilih kelas untuk absensi) dan {@code AbsenPiketAction}: kelas dengan flag
	 * ini hanya muncul bagi guru yang tercatat sebagai {@link #getGuruPembina()}.</p>
	 *
	 * @return {@code true} bila hanya wali kelas yang boleh mengabsen
	 */
	public Boolean getAbsensiharusGuruPembina() {
		return absensiharusGuruPembina == null ? true : absensiharusGuruPembina;
	}

	/**
	 * Menyetel kebijakan "hanya wali kelas yang boleh mengabsen".
	 *
	 * @param absensiharusGuruPembina {@code true} untuk membatasi absensi ke wali kelas
	 */
	public void setAbsensiharusGuruPembina(Boolean absensiharusGuruPembina) {
		this.absensiharusGuruPembina = absensiharusGuruPembina;
	}

	/**
	 * Apakah absensi kelas ini hanya boleh dilakukan guru BK?
	 *
	 * <p>Berbeda dengan {@link #getAbsensiharusGuruPembina()}, bawaannya
	 * {@code false} — pembatasan ke guru BK bersifat opsional dan harus dinyalakan
	 * eksplisit. Ditegakkan di tempat yang sama
	 * ({@code AmbilDataKelasSiswaBanbox}, {@code AbsenPiketAction}) dengan acuan
	 * {@link #getGuruBk()}.</p>
	 *
	 * @return {@code true} bila hanya guru BK yang boleh mengabsen
	 */
	public Boolean getAbsensiharusGuruBk() {
		return absensiharusGuruBk == null ? false : absensiharusGuruBk;
	}

	/**
	 * Menyetel kebijakan "hanya guru BK yang boleh mengabsen".
	 *
	 * @param absensiharusGuruBk {@code true} untuk membatasi absensi ke guru BK
	 */
	public void setAbsensiharusGuruBk(Boolean absensiharusGuruBk) {
		this.absensiharusGuruBk = absensiharusGuruBk;
	}

	/**
	 * Apakah nilai baru boleh dipublikasikan hanya setelah diverifikasi?
	 *
	 * <p>Bawaannya {@code false} (nilai langsung tampil). Bila {@code true},
	 * {@code VoKelasPunyaSiswa} — kelas dasar bersama
	 * {@code KelasSiswaPunyaSiswa} dan {@code KelasLesSiswaPunyaSiswa} —
	 * menyembunyikan nilai yang belum berstatus terverifikasi dari layar
	 * siswa/wali. Jadi flag ini benar-benar mengubah apa yang dilihat orang tua,
	 * bukan sekadar penanda administratif.</p>
	 *
	 * @return {@code true} bila publikasi nilai menuntut verifikasi lebih dulu
	 */
	public Boolean getPublikasiNilaiHarusTelahDiverifikasi() {
		return publikasiNilaiHarusTelahDiverifikasi == null ? false : publikasiNilaiHarusTelahDiverifikasi;
	}

	/**
	 * Menyetel kebijakan publikasi nilai harus terverifikasi.
	 *
	 * @param publikasiNilaiHarusTelahDiverifikasi {@code true} untuk menahan nilai
	 *                                             sampai diverifikasi
	 */
	public void setPublikasiNilaiHarusTelahDiverifikasi(Boolean publikasiNilaiHarusTelahDiverifikasi) {
		this.publikasiNilaiHarusTelahDiverifikasi = publikasiNilaiHarusTelahDiverifikasi;
	}

	/**
	 * Apakah guru pengampu boleh memverifikasi sendiri nilai yang ia masukkan?
	 *
	 * <p>Bawaannya {@code true}. Ditegakkan di
	 * {@code DetailPenilaianSiswaHelper}: bila {@code false}, akun guru hanya
	 * melihat penanda status (ikon centang) alih-alih tombol verifikasi, sehingga
	 * verifikasi harus dilakukan pihak lain (kurikulum/kepala sekolah).
	 * Berpasangan dengan {@link #getPublikasiNilaiHarusTelahDiverifikasi()}:
	 * kombinasi "publikasi butuh verifikasi" + "guru tidak boleh verifikasi
	 * sendiri" adalah konfigurasi paling ketat.</p>
	 *
	 * @return {@code true} bila guru boleh memverifikasi nilainya sendiri
	 */
	public Boolean getGuruBolehMemverifikasiSendiri() {
		return guruBolehMemverifikasiSendiri == null ? true : guruBolehMemverifikasiSendiri;
	}

	/**
	 * Menyetel kebijakan verifikasi mandiri oleh guru.
	 *
	 * @param guruBolehMemverifikasiSendiri {@code true} bila guru boleh
	 *                                      memverifikasi nilainya sendiri
	 */
	public void setGuruBolehMemverifikasiSendiri(Boolean guruBolehMemverifikasiSendiri) {
		this.guruBolehMemverifikasiSendiri = guruBolehMemverifikasiSendiri;
	}
}
