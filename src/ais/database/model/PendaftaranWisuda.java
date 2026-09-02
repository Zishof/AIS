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
import org.json.JSONObject;

/**
 * Entity <b>pendaftaran wisuda mahasiswa</b> — tabel {@code public.pendaftaran_wisuda},
 * ber-{@code @Audited} (Envers merekam setiap versi baris ke tabel bayangan) dan memakai
 * {@code dynamicInsert}/{@code dynamicUpdate} sehingga hanya kolom yang benar-benar berubah
 * ikut dalam pernyataan SQL.
 *
 * <p>Satu baris = <b>satu mahasiswa mendaftar pada satu acara wisuda</b>. Baris inilah yang
 * dilewatkan seluruh modul wisuda: formulir pendaftaran mahasiswa, lima meja pengecekan
 * berkas, penerbitan No. Registrasi dan No. Kursi, undangan, album, sampai laporan peserta.</p>
 *
 * <h3>Relasi utama</h3>
 * <ul>
 *   <li>{@link #getMahasiswa()} — peserta wisuda. Semua data identitas (NIM, nama, jurusan,
 *   fakultas, tahun angkatan, tahun lulus, foto) diambil lewat relasi ini, tidak disalin
 *   ke baris pendaftaran.</li>
 *   <li>{@link #getSkripsi()} — tugas akhir/skripsi peserta; dipakai terutama untuk mengambil
 *   judul yang dicetak di undangan, album, dan laporan.</li>
 *   <li>{@link #getWisuda()} — acara wisuda (gelombang/"wisuda ke-N") tempat mahasiswa ini
 *   didaftarkan. {@link ais.database.model.Wisuda} memegang tanggal, moto, kuota maksimal,
 *   dan status aktif acara.</li>
 * </ul>
 * <p>Ketiganya {@code @ManyToOne} {@code LAZY}, {@code nullable = true}, dengan cascade
 * {@code PERSIST}+{@code MERGE}. <b>Tidak ada</b> unique constraint gabungan
 * (mahasiswa, wisuda) di level basis data — pencegahan pendaftaran ganda dilakukan di kode
 * aplikasi ({@code PendaftaranWisudaMahasiswaAction} mencari dulu baris (mahasiswa, wisuda)
 * dengan {@code Criteria ... setMaxResults(1)} lalu memakai ulang baris yang ditemukan).
 * Bila dua sesi menyimpan bersamaan, baris kembar masih mungkin terbentuk.</p>
 *
 * <h3>Pengelompokan method</h3>
 * <ol>
 *   <li><b>Jejak audit yang dideklarasikan ulang</b> — {@link #getId()}, {@link #getOleh()},
 *   {@link #getOlehId()}, {@link #getTanggal_dirubah()}, dan kait {@link #onUpdate()}.</li>
 *   <li><b>Relasi</b> — {@link #getMahasiswa()}, {@link #getSkripsi()},
 *   {@link #getWisuda()}.</li>
 *   <li><b>Lima tahap persetujuan berkas</b> — {@link #getStatusPersetujuanAdministrasi()},
 *   {@link #getStatusPersetujuanAdministrasiFakultas()},
 *   {@link #getStatusPersetujuanKeuangan()}, {@link #getStatusPersetujuanPerpustakaan()},
 *   {@link #getStatusPersetujuanPerpustakaanFakultas()}.</li>
 *   <li><b>Rincian ceklis per tahap (JSON)</b> — {@link #getStatusPendaftaran()}.</li>
 *   <li><b>Kolom ceklis lama yang sudah tidak dipakai</b> — sembilan properti
 *   {@code statusFotoCopy*}/{@code statusBiayaWisuda}/{@code statusTandaLulusTOAFLTOEFL}/
 *   {@code statusPasPhoto}; lihat catatan "kolom mati" di bawah.</li>
 *   <li><b>Penomoran acara</b> — {@link #getNoRegistrasiWisuda()},
 *   {@link #getNoKursi()}.</li>
 *   <li><b>Atribut pendaftaran lain</b> — {@link #getTanggalDaftarWisuda()},
 *   {@link #getUkuranToga()}, {@link #getPersetujuanWisuda()},
 *   {@link #getKeterangan()}.</li>
 * </ol>
 *
 * <h3>Alur persetujuan lima tahap</h3>
 * <p>Lima kolom {@code status_persetujuan_*} masing-masing dikendalikan satu layar
 * {@code PengecekanPendaftaranWisuda*Action} (Administrasi, Administrasi Fakultas, Keuangan,
 * Perpustakaan, Perpustakaan Fakultas). Nilainya hanya <b>0 = belum disetujui</b> dan
 * <b>1 = disetujui</b>; tombol "Setuju" menulis 1, tombol "Tolak" menulis kembali 0 (jadi
 * penolakan tidak punya kode tersendiri, hanya mengembalikan ke keadaan belum disetujui).
 * Kelima tahap saling <b>bebas</b> — tidak ada urutan yang dipaksakan dan tidak ada kolom
 * agregat yang otomatis terisi ketika kelimanya bernilai 1.</p>
 *
 * <p>Persetujuan akhir justru berdiri sendiri: {@link #getPersetujuanWisuda()} adalah
 * {@code Boolean} yang <b>disetel manual</b> lewat satu checkbox di
 * {@code MahasiswaRegistrasiWisudaAction}, bukan turunan dari kelima status di atas. Nilai
 * inilah yang menjadi gerbang penerbitan No. Registrasi/No. Kursi dan penarikan peserta
 * pada helper daftar hadir. Konsekuensinya operator bisa menyetujui wisuda seorang
 * mahasiswa meski sebagian (atau seluruh) tahap pengecekan berkas masih 0.</p>
 *
 * <h3>Nomor Registrasi Wisuda dan Nomor Kursi berasal dari sumber yang sama</h3>
 * <p>Ini kuirk terpenting entity ini dan sudah diverifikasi dari kedua sisi (entity dan
 * window pembangkitnya). {@link #getNoRegistrasiWisuda()} dan {@link #getNoKursi()} adalah
 * dua kolom {@code varchar(50)} yang <b>terpisah</b> di basis data, tetapi keduanya diisi
 * dengan formula yang <b>persis sama</b> di
 * {@code ais.action.master.helper.GenerateNoKursiDanNoRegistrasiWindow}:</p>
 * <pre>
 *     String nomor = pendaftaranWisuda.getId().toString();
 *     while (nomor.length() &lt; 8) { nomor = "0" + nomor; }
 * </pre>
 * <p>Artinya keduanya adalah <b>primary key baris ini</b> ({@link #getId()}) yang di-pad nol
 * di depan sampai delapan digit — bukan sequence atau counter yang terpisah, bukan pula
 * nomor urut per acara wisuda. Untuk satu mahasiswa yang sama, <b>No. Registrasi Wisuda dan
 * No. Kursi selalu identik</b> (mis. id 1234 → {@code "00001234"} pada kedua kolom). Yang
 * membedakan hanyalah tombol mana yang sudah ditekan operator; selama salah satunya belum
 * dibangkitkan, kolomnya {@code null}. Karena berbasis id global, nomor kursi juga tidak
 * pernah dimulai dari 1 pada tiap acara wisuda dan tidak berurutan rapat — id yang terpakai
 * untuk acara lain membuat lompatan. Jika id pernah melampaui 99.999.999, hasil pad menjadi
 * lebih dari 8 karakter (formula hanya menambah, tidak pernah memotong).</p>
 *
 * <h3>Kolom ceklis lama yang sudah mati</h3>
 * <p>Sembilan properti {@code statusFotoCopyTranskripAkademik2Lembar},
 * {@code statusFotoCopyBebasBiayaPerkuliahan}, {@code statusBiayaWisuda},
 * {@code statusFotoCopyTandaLulusUjianKomprehensive},
 * {@code statusFotoCopyIjazahSLTA2Lembar}, {@code statusFotoCopyPropesa},
 * {@code statusFotoCopyLembarPengesahanSkripsi}, {@code statusTandaLulusTOAFLTOEFL}, dan
 * {@code statusPasPhoto} <b>tidak dibaca maupun ditulis dari mana pun di luar file ini</b>
 * (diverifikasi dengan penelusuran seluruh pohon sumber). Semuanya sudah digantikan oleh
 * satu kolom JSON {@link #getStatusPendaftaran()} yang daftar itemnya dinamis mengikuti
 * konfigurasi. Kolomnya tetap dipertahankan agar baris lama dan riwayat Envers tidak rusak;
 * jangan dijadikan sumber kebenaran kelengkapan berkas.</p>
 *
 * <h3>Ceklis dinamis di kolom {@code status_pendaftaran}</h3>
 * <p>{@link #getStatusPendaftaran()} menyimpan satu dokumen JSON datar berisi pasangan
 * {@code kunci: "1"} untuk tiap item berkas yang sudah dicentang. Daftar itemnya diambil
 * dari konfigurasi (mis. kunci {@code wisuda_administrasi}, dipisah titik koma), sedangkan
 * kunci JSON-nya dirakit di layar pengecekan sebagai
 * {@code namaKelasAction.toLowerCase() + "_" + namaItem.toLowerCase().replaceAll(" ", "_")}.
 * Dua konsekuensinya: (a) satu dokumen JSON menampung ceklis kelima meja sekaligus karena
 * prefiks nama kelas membuat kuncinya tidak bertabrakan; (b) kunci <b>terikat pada nama
 * kelas Java</b> — mengganti nama kelas Action atau mengubah teks item di konfigurasi
 * membuat centang lama menjadi yatim (tidak terbaca lagi, tetapi tetap tersimpan di kolom).</p>
 *
 * <h3>Catatan pewarisan</h3>
 * <p>{@link ais.database.model.GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass}, melainkan POJO abstrak biasa, sehingga Hibernate tidak
 * memetakan properti yang dideklarasikan di sana. Karena itu {@code id}, {@code oleh},
 * {@code olehId}, dan {@code tanggal_dirubah} <b>wajib</b> dideklarasikan ulang di setiap
 * entity turunan seperti di file ini — pengulangan tersebut adalah keharusan teknis, bukan
 * duplikasi yang bisa dibersihkan. Perilaku {@code check()}, penulisan-balik getter relasi,
 * dan semantik jejak audit dijelaskan lengkap di class induk.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.Wisuda
 * @see ais.database.model.Mahasiswa
 * @see ais.database.model.Skripsi
 * @see ais.database.dao.PendaftaranWisudaDao
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "pendaftaran_wisuda")
public class PendaftaranWisuda extends GeneralValueObject {

	/**
	 * Versi serialisasi. Nilainya dikunci tetap agar baris yang pernah diserialisasi (mis. ke
	 * cache antar-sesi) tetap bisa dibaca meski daftar field berubah.
	 */
	private static final long serialVersionUID = 2463852577548439808L;
	/** Primary key baris; lihat {@link #getId()} — sekaligus basis No. Registrasi/No. Kursi. */
	private Long id;
	/** Nama pengguna pengubah terakhir; lihat {@link #getOleh()}. */
	private String oleh;
	/** Identitas (id pengguna) pengubah terakhir; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna yang terakhir mengubah baris ini. Getter sederhana tanpa
	 * efek samping; nilainya diisi otomatis oleh interceptor audit lewat {@link #onUpdate()}.
	 *
	 * @return id pengguna pengubah terakhir; boleh {@code null} untuk baris yang belum pernah
	 *         diubah setelah fitur audit aktif
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel identitas pengguna pengubah. <b>Nilai {@code null} atau kosong sengaja
	 * diabaikan</b> (method langsung {@code return}) supaya jejak audit yang sudah ada tidak
	 * tertimpa nilai hampa oleh pemanggil yang tidak mengetahui pengguna aktif.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 * @see ais.database.model.GeneralValueObject
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null}/kosong <b>diabaikan</b> agar jejak lama tidak terhapus.
	 *
	 * @param oleh nama pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini. Getter sederhana tanpa efek
	 * samping.
	 *
	 * @return nama pengubah terakhir; boleh {@code null}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil <b>oleh Hibernate</b>, bukan oleh kode aplikasi,
	 * tepat sebelum setiap {@code UPDATE} baris pendaftaran wisuda. Mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #getTanggal_dirubah()} serta {@link #getOleh()}/{@link #getOlehId()} dari pengguna
	 * sesi aktif.
	 *
	 * <p>Ini satu-satunya method {@code abstract} yang diwariskan
	 * {@link ais.database.model.GeneralValueObject} dan karena itu <b>wajib</b> diimplementasikan
	 * setiap entity. Jangan dipanggil manual. Perlu dicatat: kait ini hanya berjalan pada
	 * {@code UPDATE}, tidak pada {@code INSERT} — pengisian awal {@code tanggal_dirubah} untuk
	 * baris baru datang dari inisialisasi field di bawah.</p>
	 *
	 * @see ais.database.model.GeneralValueObject
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Umumnya <b>tidak</b> dipanggil kode aplikasi —
	 * nilai ini diisi otomatis oleh interceptor audit lewat {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini. Getter sederhana tanpa efek
	 * samping; field-nya sudah diinisialisasi ke waktu server saat object dibuat sehingga tidak
	 * pernah {@code null} pada instance baru.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris pendaftaran, dirakit sebagai
	 * {@code mahasiswa + "-" + skripsi}.
	 *
	 * <p><b>Awas efek samping:</b> method ini memanggil {@link #getMahasiswa()} dan
	 * {@link #getSkripsi()}, yang keduanya melewati {@code check()} sehingga dapat memicu
	 * inisialisasi proxy lazy — termasuk membuka dan menutup sesi Hibernate sendiri bila
	 * instance sudah <i>detached</i>. Jadi {@code toString()} di sini <b>tidak murni</b> dan
	 * sebaiknya tidak dipanggil di dalam loop besar atau di jalur logging panas. Bila kedua
	 * relasi kosong, hasilnya adalah string {@code "null-null"}.</p>
	 *
	 * @return gabungan teks mahasiswa dan skripsi yang dipisah tanda hubung
	 */
	public String toString() {
		return getMahasiswa() + "-" + getSkripsi();
	}

	/** Peserta wisuda; lihat {@link #getMahasiswa()}. */
	private Mahasiswa mahasiswa;
	/** Tugas akhir peserta; lihat {@link #getSkripsi()}. */
	private Skripsi skripsi;
	/** Tahap persetujuan bagian keuangan (0/1); lihat {@link #getStatusPersetujuanKeuangan()}. */
	private Integer statusPersetujuanKeuangan = 0;
	/** Tahap persetujuan administrasi pusat (0/1); lihat {@link #getStatusPersetujuanAdministrasi()}. */
	private Integer statusPersetujuanAdministrasi = 0;
	/** Tahap persetujuan perpustakaan pusat (0/1); lihat {@link #getStatusPersetujuanPerpustakaan()}. */
	private Integer statusPersetujuanPerpustakaan = 0;
	/** Tahap persetujuan perpustakaan fakultas (0/1); lihat {@link #getStatusPersetujuanPerpustakaanFakultas()}. */
	private Integer statusPersetujuanPerpustakaanFakultas = 0;
	/** Tahap persetujuan administrasi fakultas (0/1); lihat {@link #getStatusPersetujuanAdministrasiFakultas()}. */
	private Integer statusPersetujuanAdministrasiFakultas = 0;
	/** Catatan bebas operator/mahasiswa; lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Tanggal pendaftaran wisuda; lihat {@link #getTanggalDaftarWisuda()}. */
	private Date tanggalDaftarWisuda;

	/** No. Registrasi Wisuda hasil generate; lihat {@link #getNoRegistrasiWisuda()}. */
	private String noRegistrasiWisuda;
	/** No. Kursi hasil generate; lihat {@link #getNoKursi()}. */
	private String noKursi;
	/** Kode ukuran toga 1..4 (S/M/L/XL); lihat {@link #getUkuranToga()}. */
	private Integer ukuranToga = 0;

	/** Acara wisuda tempat mahasiswa didaftarkan; lihat {@link #getWisuda()}. */
	private Wisuda wisuda;

	/** Persetujuan akhir keikutsertaan wisuda; lihat {@link #getPersetujuanWisuda()}. */
	private Boolean persetujuanWisuda = false;

	// /modul akademik
	// Sembilan field di bawah adalah ceklis berkas versi lama yang sudah tidak dibaca/ditulis
	// dari mana pun di luar file ini; penggantinya adalah kolom JSON `statusPendaftaran`.
	// Dipertahankan agar baris lama dan riwayat Envers tetap utuh.
	/** Kolom ceklis lama (mati): fotokopi transkrip akademik 2 lembar. */
	private Integer statusFotoCopyTranskripAkademik2Lembar = 0;
	/** Kolom ceklis lama (mati): fotokopi bukti bebas biaya perkuliahan. */
	private Integer statusFotoCopyBebasBiayaPerkuliahan = 0;
	/** Kolom ceklis lama (mati): pelunasan biaya wisuda. */
	private Integer statusBiayaWisuda = 0;
	/** Kolom ceklis lama (mati): fotokopi tanda lulus ujian komprehensif. */
	private Integer statusFotoCopyTandaLulusUjianKomprehensive = 0;
	/** Kolom ceklis lama (mati): fotokopi ijazah SLTA 2 lembar. */
	private Integer statusFotoCopyIjazahSLTA2Lembar = 0;
	/** Kolom ceklis lama (mati): fotokopi bukti Propesa (masa orientasi). */
	private Integer statusFotoCopyPropesa = 0;
	/** Kolom ceklis lama (mati): fotokopi lembar pengesahan skripsi. */
	private Integer statusFotoCopyLembarPengesahanSkripsi = 0;
	/** Kolom ceklis lama (mati): tanda lulus TOAFL/TOEFL. */
	private Integer statusTandaLulusTOAFLTOEFL = 0;
	/** Kolom ceklis lama (mati): pas photo. */
	private Integer statusPasPhoto = 0;

	/** Dokumen JSON ceklis berkas dinamis; lihat {@link #getStatusPendaftaran()}. */
	private String statusPendaftaran;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA untuk membentuk instance lewat
	 * refleksi. Seluruh field {@code Integer} status dan {@link #getPersetujuanWisuda()} sudah
	 * berisi nilai awal dari inisialisasi field, jadi instance baru langsung berada pada
	 * keadaan "belum disetujui" tanpa perlu penyetelan tambahan.
	 */
	public PendaftaranWisuda() {
	}

	/**
	 * Mengembalikan primary key baris pendaftaran ini. Kolom {@code id} bertipe identity
	 * ({@code insertable = false}) sehingga nilainya baru terisi <b>setelah</b> baris berhasil
	 * disimpan dan sesi di-{@code flush}.
	 *
	 * <p><b>Penting:</b> id ini bukan sekadar kunci teknis — ia adalah <b>basis No. Registrasi
	 * Wisuda dan No. Kursi</b>. {@code GenerateNoKursiDanNoRegistrasiWindow} mengambil
	 * {@code getId().toString()} lalu memberi imbuhan nol di depan sampai delapan digit untuk
	 * mengisi {@link #setNoRegistrasiWisuda(String)} maupun {@link #setNoKursi(String)}.
	 * Karena itu tombol generate hanya bermakna untuk baris yang sudah tersimpan; pada baris
	 * baru yang id-nya masih {@code null}, pemanggilan generate akan gagal dengan
	 * {@code NullPointerException}.</p>
	 *
	 * @return id baris; {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Diisi Hibernate setelah {@code INSERT}; jangan disetel manual oleh
	 * kode aplikasi.
	 *
	 * @param id primary key baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan catatan bebas yang menyertai pendaftaran (mis. keterangan tambahan yang
	 * diketik operator pada layar registrasi wisuda). Getter sederhana tanpa efek samping;
	 * kolom {@code keterangan} tanpa batas panjang eksplisit.
	 *
	 * @return catatan bebas; boleh {@code null}
	 */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel catatan bebas pendaftaran. Dipanggil dari layar
	 * {@code MahasiswaRegistrasiWisudaAction} saat operator menyimpan formulir.
	 *
	 * @param keterangan catatan bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menyetel mahasiswa peserta wisuda.
	 *
	 * @param mahasiswa peserta wisuda; boleh {@code null}
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Mengembalikan mahasiswa peserta wisuda. Seluruh identitas peserta (NIM, nama, jurusan,
	 * fakultas, tahun angkatan/lulus, foto) diambil dari sini, tidak disalin ke baris
	 * pendaftaran.
	 *
	 * <p><b>Efek samping:</b> relasi ini {@code LAZY}, sehingga getter memanggil
	 * {@code check()} untuk meresolusi proxy dan <b>menulis balik hasilnya ke field</b>
	 * ({@code mahasiswa = check(mahasiswa)}). {@code check()} dapat membaca cache, memakai
	 * sesi Hibernate yang sedang aktif, atau — bila instance sudah <i>detached</i> — membuka
	 * sesi baru sendiri dan menutupnya kembali di blok {@code finally}. Method ini tidak
	 * pernah melempar exception dan tidak mengubah {@code null} menjadi non-{@code null}.</p>
	 *
	 * @return peserta wisuda yang sudah teresolusi; boleh {@code null}
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * Menyetel skripsi/tugas akhir peserta.
	 *
	 * @param skripsi tugas akhir peserta; boleh {@code null}
	 */
	public void setSkripsi(Skripsi skripsi) {
		this.skripsi = skripsi;
	}

	/**
	 * Mengembalikan skripsi/tugas akhir peserta — sumber judul yang dicetak di undangan,
	 * album wisuda, dan laporan peserta.
	 *
	 * <p><b>Efek samping:</b> sama dengan {@link #getMahasiswa()} — relasi {@code LAZY} yang
	 * diresolusi lewat {@code check()} dan hasilnya <b>ditulis balik ke field</b>, dengan
	 * kemungkinan membuka serta menutup sesi Hibernate tersendiri untuk instance
	 * <i>detached</i>.</p>
	 *
	 * @return skripsi peserta yang sudah teresolusi; boleh {@code null} (mahasiswa jalur
	 *         non-skripsi atau data belum lengkap)
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "skripsi", nullable = true)
	public Skripsi getSkripsi() {
		skripsi = check(skripsi);
		return skripsi;
	}

	/**
	 * Menyetel status persetujuan bagian keuangan. Ditulis {@code 1} oleh tombol "Setuju" dan
	 * kembali {@code 0} oleh tombol "Tolak" pada
	 * {@code PengecekanPendaftaranWisudaKeuanganAction}.
	 *
	 * @param statusPersetujuanKeuangan 0 = belum disetujui, 1 = disetujui
	 */
	public void setStatusPersetujuanKeuangan(Integer statusPersetujuanKeuangan) {
		this.statusPersetujuanKeuangan = statusPersetujuanKeuangan;
	}

	/**
	 * Mengembalikan status persetujuan bagian keuangan atas berkas wisuda mahasiswa ini.
	 *
	 * <p>Getter ini <b>menormalkan {@code null} menjadi 0</b> agar pemanggil aman memakai
	 * {@code .equals(0)}/{@code .equals(1)} tanpa pemeriksaan null. Normalisasi ini
	 * <b>tidak</b> ditulis balik ke field, jadi baris lama yang kolomnya {@code NULL} tetap
	 * {@code NULL} di basis data — tampil sebagai 0 di layar tanpa memicu {@code UPDATE}.</p>
	 *
	 * @return 0 bila belum disetujui (termasuk saat kolom {@code NULL}), 1 bila sudah
	 */
	@Column(name = "status_persetujuan_keuangan", length = 1)
	public Integer getStatusPersetujuanKeuangan() {
		return statusPersetujuanKeuangan == null ? 0 : statusPersetujuanKeuangan;
	}

	/**
	 * Menyetel status persetujuan administrasi (pusat). Ditulis {@code 1}/{@code 0} oleh tombol
	 * Setuju/Tolak pada {@code PengecekanPendaftaranWisudaAdministrasiAction}, bersamaan dengan
	 * pembaruan ceklis JSON lewat {@link #setStatusPendaftaran(String)}.
	 *
	 * @param statusPersetujuanAdministrasi 0 = belum disetujui, 1 = disetujui
	 */
	public void setStatusPersetujuanAdministrasi(Integer statusPersetujuanAdministrasi) {
		this.statusPersetujuanAdministrasi = statusPersetujuanAdministrasi;
	}

	/**
	 * Mengembalikan status persetujuan administrasi (pusat). Sama seperti
	 * {@link #getStatusPersetujuanKeuangan()}, {@code null} dinormalkan menjadi 0 tanpa ditulis
	 * balik ke field.
	 *
	 * @return 0 bila belum disetujui, 1 bila sudah
	 */
	@Column(name = "status_persetujuan_administrasi", length = 1)
	public Integer getStatusPersetujuanAdministrasi() {
		return statusPersetujuanAdministrasi == null ? 0 : statusPersetujuanAdministrasi;
	}

	/**
	 * Menyetel status persetujuan perpustakaan (pusat) — biasanya menandai mahasiswa sudah
	 * bebas pinjaman dan sudah menyerahkan skripsi.
	 *
	 * @param statusPersetujuanPerpustakaan 0 = belum disetujui, 1 = disetujui
	 */
	public void setStatusPersetujuanPerpustakaan(Integer statusPersetujuanPerpustakaan) {
		this.statusPersetujuanPerpustakaan = statusPersetujuanPerpustakaan;
	}

	/**
	 * Mengembalikan status persetujuan perpustakaan (pusat). {@code null} dinormalkan menjadi 0
	 * tanpa ditulis balik ke field.
	 *
	 * @return 0 bila belum disetujui, 1 bila sudah
	 */
	@Column(name = "status_persetujuan_perpustakaan", length = 1)
	public Integer getStatusPersetujuanPerpustakaan() {
		return statusPersetujuanPerpustakaan == null ? 0 : statusPersetujuanPerpustakaan;
	}

	/**
	 * Menyetel status persetujuan administrasi tingkat fakultas — tahap yang berdiri sendiri
	 * dari administrasi pusat, dikendalikan
	 * {@code PengecekanPendaftaranWisudaAdministrasiFakultasAction}.
	 *
	 * @param statusPersetujuanAdministrasiFakultas 0 = belum disetujui, 1 = disetujui
	 */
	public void setStatusPersetujuanAdministrasiFakultas(Integer statusPersetujuanAdministrasiFakultas) {
		this.statusPersetujuanAdministrasiFakultas = statusPersetujuanAdministrasiFakultas;
	}

	/**
	 * Mengembalikan status persetujuan administrasi tingkat fakultas. {@code null} dinormalkan
	 * menjadi 0 tanpa ditulis balik ke field.
	 *
	 * @return 0 bila belum disetujui, 1 bila sudah
	 */
	@Column(name = "status_persetujuan_administrasi_fakultas", length = 1)
	public Integer getStatusPersetujuanAdministrasiFakultas() {
		return statusPersetujuanAdministrasiFakultas == null ? 0 : statusPersetujuanAdministrasiFakultas;
	}

	/**
	 * Menyetel tanggal pendaftaran wisuda. Diisi layar pendaftaran dengan waktu server saat
	 * penyimpanan, lalu ditimpa lagi dengan nilai kotak tanggal yang dipilih operator.
	 *
	 * @param tanggalDaftarWisuda tanggal pendaftaran
	 */
	public void setTanggalDaftarWisuda(Date tanggalDaftarWisuda) {
		this.tanggalDaftarWisuda = tanggalDaftarWisuda;
	}

	/**
	 * Mengembalikan tanggal pendaftaran wisuda.
	 *
	 * <p><b>Getter yang menulis balik ke field:</b> bila {@code tanggalDaftarWisuda} masih
	 * {@code null}, method ini <b>mengisinya dengan waktu server saat itu juga</b>
	 * ({@code WaktuUtil.getDate()}) sebelum mengembalikannya. Nilai hasil pengisian otomatis
	 * ini ikut tersimpan bila instance sedang <i>attached</i> pada sesi Hibernate aktif dan
	 * sesi tersebut kemudian di-{@code flush}. Akibatnya, baris lama yang kolomnya {@code NULL}
	 * bisa "mendapat" tanggal pendaftaran sama dengan <b>waktu pertama kali baris itu dibuka
	 * di layar</b>, bukan tanggal pendaftaran sebenarnya — dan perubahan itu juga terekam
	 * Envers. Untuk instance <i>detached</i> (mis. hasil deserialisasi cache) nilainya hanya
	 * berlaku di memori. Gunakan field-nya langsung bila butuh membedakan "belum pernah
	 * diisi" dari "diisi hari ini".</p>
	 *
	 * @return tanggal pendaftaran; tidak pernah {@code null} setelah pemanggilan pertama
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_daftar_wisuda", length = 0)
	public Date getTanggalDaftarWisuda() {
		if (tanggalDaftarWisuda == null) {
			tanggalDaftarWisuda = ais.ui.util.WaktuUtil.getDate();
		}
		return tanggalDaftarWisuda;
	}

	/**
	 * Menyetel status persetujuan perpustakaan tingkat fakultas — tahap terpisah dari
	 * perpustakaan pusat, dikendalikan
	 * {@code PengecekanPendaftaranWisudaPerpustakaanFakultasAction}.
	 *
	 * @param statusPersetujuanPerpustakaanFakultas 0 = belum disetujui, 1 = disetujui
	 */
	public void setStatusPersetujuanPerpustakaanFakultas(Integer statusPersetujuanPerpustakaanFakultas) {
		this.statusPersetujuanPerpustakaanFakultas = statusPersetujuanPerpustakaanFakultas;
	}

	/**
	 * Mengembalikan status persetujuan perpustakaan tingkat fakultas. {@code null} dinormalkan
	 * menjadi 0 tanpa ditulis balik ke field.
	 *
	 * @return 0 bila belum disetujui, 1 bila sudah
	 */
	@Column(name = "status_persetujuan_perpustakaan_fakultas", length = 1)
	public Integer getStatusPersetujuanPerpustakaanFakultas() {
		return statusPersetujuanPerpustakaanFakultas == null ? 0 : statusPersetujuanPerpustakaanFakultas;
	}

	/**
	 * Menyetel No. Registrasi Wisuda. Satu-satunya pemanggil yang membangkitkan nilainya adalah
	 * {@code GenerateNoKursiDanNoRegistrasiWindow#onGenerateNoRegistrasiWisuda}, yang mengisi
	 * {@code getId()} ber-imbuhan nol sampai delapan digit lalu menyimpannya lewat
	 * {@code Common.refreshSaveOrUpdate(...)}. Tombolnya baru aktif bila
	 * {@link #getPersetujuanWisuda()} bernilai {@code true} dan nomor belum pernah dibuat.
	 *
	 * @param noRegistrasiWisuda nomor registrasi wisuda (delapan digit ber-imbuhan nol)
	 */
	public void setNoRegistrasiWisuda(String noRegistrasiWisuda) {
		this.noRegistrasiWisuda = noRegistrasiWisuda;
	}

	/**
	 * Mengembalikan No. Registrasi Wisuda. Getter sederhana tanpa efek samping; {@code null}
	 * berarti nomor belum pernah dibangkitkan (sejumlah layar memakai kondisi ini untuk
	 * mengunci tombol cetak).
	 *
	 * <p><b>Kuirk penting:</b> nomor ini <b>bukan sequence tersendiri</b>, melainkan
	 * {@link #getId()} yang di-pad nol sampai delapan digit — nilai yang sama persis dengan
	 * yang dipakai {@link #getNoKursi()}. Untuk satu mahasiswa, No. Registrasi Wisuda dan
	 * No. Kursi selalu identik. Lihat penjelasan lengkap pada Javadoc class.</p>
	 *
	 * @return nomor registrasi wisuda; {@code null} bila belum digenerate
	 */
	@Column(name = "no_registrasi_wisuda", length = 50)
	public String getNoRegistrasiWisuda() {
		return noRegistrasiWisuda;
	}

	/**
	 * Menyetel No. Kursi wisuda. Dibangkitkan
	 * {@code GenerateNoKursiDanNoRegistrasiWindow#onGenerateNoKursiWisuda} (dan
	 * {@code GenerateNoKursiWindow}) dengan formula yang <b>identik</b> dengan
	 * {@link #setNoRegistrasiWisuda(String)}: {@code getId()} ber-imbuhan nol delapan digit.
	 *
	 * @param noKursi nomor kursi (delapan digit ber-imbuhan nol)
	 */
	public void setNoKursi(String noKursi) {
		this.noKursi = noKursi;
	}

	/**
	 * Mengembalikan No. Kursi pada acara wisuda. Getter sederhana tanpa efek samping.
	 *
	 * <p><b>Kuirk penting:</b> karena diturunkan dari {@link #getId()} (bukan nomor urut per
	 * acara), nomor kursi tidak pernah dimulai dari 1 di tiap acara wisuda, tidak berurutan
	 * rapat antar-peserta satu acara, dan nilainya sama dengan
	 * {@link #getNoRegistrasiWisuda()} untuk mahasiswa yang sama. Lihat penjelasan lengkap
	 * pada Javadoc class.</p>
	 *
	 * @return nomor kursi; {@code null} bila belum digenerate
	 */
	@Column(name = "no_kursi", length = 50)
	public String getNoKursi() {
		return noKursi;
	}

	/**
	 * Menyetel acara wisuda tempat mahasiswa ini didaftarkan.
	 *
	 * @param wisuda acara wisuda tujuan; boleh {@code null}
	 */
	public void setWisuda(Wisuda wisuda) {
		this.wisuda = wisuda;
	}

	/**
	 * Mengembalikan acara wisuda (gelombang "wisuda ke-N") tempat mahasiswa ini didaftarkan.
	 * {@link ais.database.model.Wisuda} memegang tanggal acara, moto, kuota maksimal, dan
	 * status aktif — kuota inilah yang dipakai layar pendaftaran untuk menolak peserta baru
	 * saat gelombang sudah penuh.
	 *
	 * <p><b>Efek samping:</b> relasi {@code LAZY} yang diresolusi lewat {@code check()} dan
	 * hasilnya <b>ditulis balik ke field</b>, dengan kemungkinan membuka lalu menutup sesi
	 * Hibernate tersendiri untuk instance <i>detached</i>.</p>
	 *
	 * @return acara wisuda yang sudah teresolusi; boleh {@code null}
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "wisuda", nullable = true)
	public Wisuda getWisuda() {
		wisuda = check(wisuda);
		return wisuda;
	}

	/**
	 * Kolom ceklis lama (sudah mati): fotokopi transkrip akademik 2 lembar. {@code null}
	 * dinormalkan menjadi 0 tanpa ditulis balik ke field.
	 *
	 * <p>Tidak dibaca dari mana pun di luar file ini; penggantinya adalah item bernama sama di
	 * dalam dokumen JSON {@link #getStatusPendaftaran()}.</p>
	 *
	 * @return 0 = belum, 1 = sudah (nilai historis)
	 */
	@Column(name = "status_fotocopy_transkripakademik", length = 1)
	public Integer getStatusFotoCopyTranskripAkademik2Lembar() {
		return statusFotoCopyTranskripAkademik2Lembar == null ? 0 : statusFotoCopyTranskripAkademik2Lembar;
	}

	/**
	 * Menyetel kolom ceklis lama (sudah mati) fotokopi transkrip akademik 2 lembar. Tidak
	 * dipanggil kode aplikasi mana pun.
	 *
	 * @param statusFotoCopyTranskripAkademik2Lembar 0 = belum, 1 = sudah
	 */
	public void setStatusFotoCopyTranskripAkademik2Lembar(Integer statusFotoCopyTranskripAkademik2Lembar) {
		this.statusFotoCopyTranskripAkademik2Lembar = statusFotoCopyTranskripAkademik2Lembar;
	}

	/**
	 * Kolom ceklis lama (sudah mati): fotokopi bukti bebas biaya perkuliahan. {@code null}
	 * dinormalkan menjadi 0 tanpa ditulis balik ke field. Sudah digantikan item pada
	 * {@link #getStatusPendaftaran()}.
	 *
	 * @return 0 = belum, 1 = sudah (nilai historis)
	 */
	@Column(name = "status_fotocopy_bebasbiayakuliah", length = 1)
	public Integer getStatusFotoCopyBebasBiayaPerkuliahan() {
		return statusFotoCopyBebasBiayaPerkuliahan == null ? 0 : statusFotoCopyBebasBiayaPerkuliahan;
	}

	/**
	 * Menyetel kolom ceklis lama (sudah mati) fotokopi bebas biaya perkuliahan. Tidak dipanggil
	 * kode aplikasi mana pun.
	 *
	 * @param statusFotoCopyBebasBiayaPerkuliahan 0 = belum, 1 = sudah
	 */
	public void setStatusFotoCopyBebasBiayaPerkuliahan(Integer statusFotoCopyBebasBiayaPerkuliahan) {
		this.statusFotoCopyBebasBiayaPerkuliahan = statusFotoCopyBebasBiayaPerkuliahan;
	}

	/**
	 * Kolom ceklis lama (sudah mati): pelunasan biaya wisuda. {@code null} dinormalkan menjadi
	 * 0 tanpa ditulis balik ke field. Sudah digantikan item pada
	 * {@link #getStatusPendaftaran()}; pembuktian pembayaran yang berlaku sekarang ada di modul
	 * keuangan, bukan di kolom ini.
	 *
	 * @return 0 = belum, 1 = sudah (nilai historis)
	 */
	@Column(name = "status_biayawisuda", length = 1)
	public Integer getStatusBiayaWisuda() {
		return statusBiayaWisuda == null ? 0 : statusBiayaWisuda;
	}

	/**
	 * Menyetel kolom ceklis lama (sudah mati) pelunasan biaya wisuda. Tidak dipanggil kode
	 * aplikasi mana pun.
	 *
	 * @param statusBiayaWisuda 0 = belum, 1 = sudah
	 */
	public void setStatusBiayaWisuda(Integer statusBiayaWisuda) {
		this.statusBiayaWisuda = statusBiayaWisuda;
	}

	/**
	 * Kolom ceklis lama (sudah mati): fotokopi tanda lulus ujian komprehensif. {@code null}
	 * dinormalkan menjadi 0 tanpa ditulis balik ke field. Sudah digantikan item pada
	 * {@link #getStatusPendaftaran()}.
	 *
	 * @return 0 = belum, 1 = sudah (nilai historis)
	 */
	@Column(name = "status_fotocopy_tandalulusujiankomprehensive", length = 1)
	public Integer getStatusFotoCopyTandaLulusUjianKomprehensive() {
		return statusFotoCopyTandaLulusUjianKomprehensive == null ? 0 : statusFotoCopyTandaLulusUjianKomprehensive;
	}

	/**
	 * Menyetel kolom ceklis lama (sudah mati) fotokopi tanda lulus ujian komprehensif. Tidak
	 * dipanggil kode aplikasi mana pun.
	 *
	 * @param statusFotoCopyTandaLulusUjianKomprehensive 0 = belum, 1 = sudah
	 */
	public void setStatusFotoCopyTandaLulusUjianKomprehensive(Integer statusFotoCopyTandaLulusUjianKomprehensive) {
		this.statusFotoCopyTandaLulusUjianKomprehensive = statusFotoCopyTandaLulusUjianKomprehensive;
	}

	/**
	 * Kolom ceklis lama (sudah mati): fotokopi ijazah SLTA 2 lembar. {@code null} dinormalkan
	 * menjadi 0 tanpa ditulis balik ke field. Sudah digantikan item pada
	 * {@link #getStatusPendaftaran()}.
	 *
	 * @return 0 = belum, 1 = sudah (nilai historis)
	 */
	@Column(name = "status_fotocopy_ijazahslta", length = 1)
	public Integer getStatusFotoCopyIjazahSLTA2Lembar() {
		return statusFotoCopyIjazahSLTA2Lembar == null ? 0 : statusFotoCopyIjazahSLTA2Lembar;
	}

	/**
	 * Menyetel kolom ceklis lama (sudah mati) fotokopi ijazah SLTA 2 lembar. Tidak dipanggil
	 * kode aplikasi mana pun.
	 *
	 * @param statusFotoCopyIjazahSLTA2Lembar 0 = belum, 1 = sudah
	 */
	public void setStatusFotoCopyIjazahSLTA2Lembar(Integer statusFotoCopyIjazahSLTA2Lembar) {
		this.statusFotoCopyIjazahSLTA2Lembar = statusFotoCopyIjazahSLTA2Lembar;
	}

	/**
	 * Kolom ceklis lama (sudah mati): fotokopi bukti Propesa (program orientasi mahasiswa
	 * baru). {@code null} dinormalkan menjadi 0 tanpa ditulis balik ke field. Sudah digantikan
	 * item pada {@link #getStatusPendaftaran()}.
	 *
	 * @return 0 = belum, 1 = sudah (nilai historis)
	 */
	@Column(name = "status_fotocopy_propesa", length = 1)
	public Integer getStatusFotoCopyPropesa() {
		return statusFotoCopyPropesa == null ? 0 : statusFotoCopyPropesa;
	}

	/**
	 * Menyetel kolom ceklis lama (sudah mati) fotokopi bukti Propesa. Tidak dipanggil kode
	 * aplikasi mana pun.
	 *
	 * @param statusFotoCopyPropesa 0 = belum, 1 = sudah
	 */
	public void setStatusFotoCopyPropesa(Integer statusFotoCopyPropesa) {
		this.statusFotoCopyPropesa = statusFotoCopyPropesa;
	}

	/**
	 * Kolom ceklis lama (sudah mati): fotokopi lembar pengesahan skripsi. {@code null}
	 * dinormalkan menjadi 0 tanpa ditulis balik ke field. Sudah digantikan item pada
	 * {@link #getStatusPendaftaran()}.
	 *
	 * @return 0 = belum, 1 = sudah (nilai historis)
	 */
	@Column(name = "status_fotocopy_lembarpengesahanskripsi", length = 1)
	public Integer getStatusFotoCopyLembarPengesahanSkripsi() {
		return statusFotoCopyLembarPengesahanSkripsi == null ? 0 : statusFotoCopyLembarPengesahanSkripsi;
	}

	/**
	 * Menyetel kolom ceklis lama (sudah mati) fotokopi lembar pengesahan skripsi. Tidak
	 * dipanggil kode aplikasi mana pun.
	 *
	 * @param statusFotoCopyLembarPengesahanSkripsi 0 = belum, 1 = sudah
	 */
	public void setStatusFotoCopyLembarPengesahanSkripsi(Integer statusFotoCopyLembarPengesahanSkripsi) {
		this.statusFotoCopyLembarPengesahanSkripsi = statusFotoCopyLembarPengesahanSkripsi;
	}

	/**
	 * Kolom ceklis lama (sudah mati): tanda lulus TOAFL/TOEFL (uji kemampuan bahasa Arab/
	 * Inggris). {@code null} dinormalkan menjadi 0 tanpa ditulis balik ke field. Sudah
	 * digantikan item pada {@link #getStatusPendaftaran()}.
	 *
	 * @return 0 = belum, 1 = sudah (nilai historis)
	 */
	@Column(name = "status_tandalulustoafltoefl", length = 1)
	public Integer getStatusTandaLulusTOAFLTOEFL() {
		return statusTandaLulusTOAFLTOEFL == null ? 0 : statusTandaLulusTOAFLTOEFL;
	}

	/**
	 * Menyetel kolom ceklis lama (sudah mati) tanda lulus TOAFL/TOEFL. Tidak dipanggil kode
	 * aplikasi mana pun.
	 *
	 * @param statusTandaLulusTOAFLTOEFL 0 = belum, 1 = sudah
	 */
	public void setStatusTandaLulusTOAFLTOEFL(Integer statusTandaLulusTOAFLTOEFL) {
		this.statusTandaLulusTOAFLTOEFL = statusTandaLulusTOAFLTOEFL;
	}

	/**
	 * Kolom ceklis lama (sudah mati): penyerahan pas photo. {@code null} dinormalkan menjadi 0
	 * tanpa ditulis balik ke field. Sudah digantikan item pada
	 * {@link #getStatusPendaftaran()}; foto yang dipakai album/undangan wisuda diambil dari
	 * data foto mahasiswa, bukan dari kolom ini.
	 *
	 * @return 0 = belum, 1 = sudah (nilai historis)
	 */
	@Column(name = "status_pasphoto", length = 1)
	public Integer getStatusPasPhoto() {
		return statusPasPhoto == null ? 0 : statusPasPhoto;
	}

	/**
	 * Menyetel kolom ceklis lama (sudah mati) penyerahan pas photo. Tidak dipanggil kode
	 * aplikasi mana pun.
	 *
	 * @param statusPasPhoto 0 = belum, 1 = sudah
	 */
	public void setStatusPasPhoto(Integer statusPasPhoto) {
		this.statusPasPhoto = statusPasPhoto;
	}

	/**
	 * Mengembalikan kode ukuran toga yang dipesan peserta. Kode dipetakan ke huruf oleh
	 * pemakainya ({@code DetailwisudaHelper}, {@code LaporanMahasiswaWisuda}):
	 * <b>1 = S, 2 = M, 3 = L, selain itu (termasuk 4) = XL</b>. Nilai awal field adalah 0 —
	 * yaitu "belum memilih", yang ikut jatuh ke label "XL" bila baris terlanjur tersimpan
	 * tanpa pilihan.
	 *
	 * <p>Berbeda dengan getter {@code Integer} lain di kelas ini, method ini <b>tidak</b>
	 * menormalkan {@code null} menjadi 0 — pemanggil wajib memeriksa null sendiri (dan memang
	 * semua pemanggil yang ada melakukannya).</p>
	 *
	 * @return kode ukuran toga; boleh {@code null} untuk baris lama
	 */
	@Column(name = "ukurantoga", length = 10)
	public Integer getUkuranToga() {
		return ukuranToga;
	}

	/**
	 * Menyetel kode ukuran toga (1 = S, 2 = M, 3 = L, 4 = XL). Diisi dari combobox pada
	 * {@code PendaftaranWisudaMahasiswaAction} saat mahasiswa/operator menyimpan formulir
	 * pendaftaran.
	 *
	 * @param ukuranToga kode ukuran toga
	 */
	public void setUkuranToga(Integer ukuranToga) {
		this.ukuranToga = ukuranToga;
	}

	/**
	 * Menyetel persetujuan akhir keikutsertaan wisuda. Satu-satunya pemanggil adalah checkbox
	 * pada {@code MahasiswaRegistrasiWisudaAction} — nilainya <b>tidak</b> dihitung otomatis
	 * dari kelima status persetujuan berkas.
	 *
	 * @param persetujuanWisuda {@code true} bila mahasiswa disetujui ikut wisuda
	 */
	public void setPersetujuanWisuda(Boolean persetujuanWisuda) {
		this.persetujuanWisuda = persetujuanWisuda;
	}

	/**
	 * Mengembalikan persetujuan akhir keikutsertaan wisuda. Getter sederhana tanpa efek
	 * samping; nilai awal field adalah {@code false}, namun baris lama masih bisa {@code null}
	 * sehingga seluruh pemanggil memeriksa null lebih dulu.
	 *
	 * <p>Nilai ini adalah <b>gerbang operasional</b> modul wisuda: window generate hanya
	 * mengaktifkan tombol "Generate No Registrasi"/"Generate No Kursi" bila bernilai
	 * {@code true}, dan helper daftar hadir hanya menarik peserta yang sudah disetujui. Karena
	 * disetel manual, nilainya bisa {@code true} walaupun sebagian tahap pengecekan berkas
	 * masih 0 — dan sebaliknya bisa {@code false} meski kelima tahap sudah 1.</p>
	 *
	 * @return {@code true} bila disetujui ikut wisuda; boleh {@code null} pada baris lama
	 */
	@Column(name = "persetujuan_wisuda")
	public Boolean getPersetujuanWisuda() {
		return persetujuanWisuda;
	}

	/**
	 * Mengembalikan dokumen JSON ceklis kelengkapan berkas — kolom {@code text} yang menampung
	 * rincian centang untuk <b>kelima</b> meja pengecekan sekaligus.
	 *
	 * <p>Isinya satu object JSON datar berisi pasangan {@code kunci: "1"} untuk tiap item yang
	 * sudah dicentang. Kunci dirakit di layar pengecekan sebagai
	 * {@code namaKelasAction.toLowerCase() + "_" + namaItem.toLowerCase().replaceAll(" ", "_")}
	 * (mis. {@code "pengecekanpendaftaranwisudaadministrasiaction_transkrip_akademik"}), dan
	 * daftar itemnya sendiri diambil dari konfigurasi (mis. kunci {@code wisuda_administrasi})
	 * yang dipisah titik koma. Prefiks nama kelas itulah yang mencegah item milik meja berbeda
	 * saling menimpa.</p>
	 *
	 * <p><b>Perilaku dan kuirk:</b></p>
	 * <ul>
	 *   <li>Bila field masih {@code null}, method mengembalikan {@code "{}"} — object JSON
	 *   kosong hasil {@code new JSONObject().toString()} — sehingga pemanggil aman langsung
	 *   melakukan {@code new JSONObject(getStatusPendaftaran())} tanpa memeriksa null. Nilai
	 *   pengganti ini <b>tidak</b> ditulis balik ke field, jadi kolom di basis data tetap
	 *   {@code NULL} (berbeda dari {@link #getTanggalDaftarWisuda()} yang menulis balik).</li>
	 *   <li>Cabang non-null memanggil {@code statusPendaftaran.toString()} pada nilai yang
	 *   sudah bertipe {@code String} — pemanggilan itu mubazir dan tidak mengubah apa pun,
	 *   kemungkinan sisa dari versi lama saat field bertipe {@code JSONObject}.</li>
	 *   <li>Isinya <b>tidak divalidasi</b> di sini. String apa pun yang disetel lewat
	 *   {@link #setStatusPendaftaran(String)} akan dikembalikan apa adanya, dan JSON rusak baru
	 *   meledak di pemanggil saat diurai.</li>
	 *   <li>Karena kunci terikat nama kelas Java dan teks item konfigurasi, mengganti nama
	 *   kelas Action atau mengubah label item membuat centang lama menjadi yatim: tetap
	 *   tersimpan di kolom tetapi tidak pernah terbaca lagi.</li>
	 * </ul>
	 *
	 * @return dokumen JSON ceklis; {@code "{}"} bila belum ada satu pun centang tersimpan,
	 *         tidak pernah {@code null}
	 */
	@Column(name = "status_pendaftaran", columnDefinition = "text")
	public String getStatusPendaftaran() {
		return statusPendaftaran == null ? new JSONObject().toString() : statusPendaftaran.toString();
	}

	/**
	 * Menyetel dokumen JSON ceklis kelengkapan berkas. Dipanggil tombol "Setuju" pada setiap
	 * layar {@code PengecekanPendaftaranWisuda*Action} dengan hasil
	 * {@code jsonObject.toString()} setelah seluruh item pada meja tersebut dicentang —
	 * tombol menolak menyimpan bila masih ada item yang belum dicentang.
	 *
	 * <p>Karena setiap layar mengurai dokumen yang ada lalu menuliskannya kembali secara utuh,
	 * dua meja pengecekan yang menyetujui <b>bersamaan</b> pada baris yang sama berpotensi
	 * saling menimpa centang (yang menyimpan terakhir menang). Format isinya tidak divalidasi
	 * di setter ini.</p>
	 *
	 * @param statusPendaftaran dokumen JSON ceklis; boleh {@code null} (dibaca kembali sebagai
	 *                          {@code "{}"})
	 */
	public void setStatusPendaftaran(String statusPendaftaran) {
		this.statusPendaftaran = statusPendaftaran;
	}

}
