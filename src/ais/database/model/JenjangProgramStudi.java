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

import org.apache.commons.lang.StringUtils;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.model.epsbed.EpsbedFrekuensiKurikulum;
import ais.database.model.epsbed.EpsbedPelaksanaanKurikulum;
import ais.database.model.epsbed.EpsbedStatus;
import ais.database.model.epsbed.EpsbedStatusAkreditasi;

/**
 * Entity <b>profil/identitas resmi satu program studi</b> — di layar dikenal sebagai tab
 * <i>"Tentang Prodi"</i> pada {@code JurusanAction}, dan di pelaporan dikenal sebagai baris
 * <i>tabel program studi</i> EPSBED/DIKTI. Dipetakan ke tabel {@code public.jenjang_program_studi},
 * beranotasi {@link Audited} (seluruh perubahan direkam Hibernate Envers) serta
 * {@code dynamicInsert}/{@code dynamicUpdate}.
 *
 * <h2>Namanya menyesatkan — bacalah bagian ini dulu</h2>
 *
 * <p>Nama kelas membuat orang mengira ini <b>master jenjang</b> (daftar S1/S2/S3/D3). Bukan.
 * Master jenjang adalah {@link Jenjang} (tabel {@code public.jenjang}, punya {@code kode},
 * {@code jumlahSemester}, {@code jenjangEpsbed}, dst.). Kelas ini adalah <b>baris detail milik
 * satu {@link Jurusan}</b>: satu paket data administratif prodi tersebut — SK pendirian DIKTI,
 * SK akreditasi, kontak &amp; pejabat prodi, syarat kelulusan SKS, standar TOEFL/TOAFL, dan
 * kode-kode pelaporan EPSBED. Kolom {@code jurusan} bahkan {@code NOT NULL}: tidak ada baris
 * kelas ini yang berdiri sendiri tanpa prodi induk.</p>
 *
 * <p>Relasi {@code jenjang} di sini hanyalah <i>salinan</i> jenjang milik prodi induk. Seluruh
 * kode yang membuat baris baru (lihat {@code JurusanAction.initTentangProdi} dan
 * {@code sapto/LaporanFormatSarjana}) mengisinya dengan {@code jurusan.getJenjang()} — bukan
 * pilihan mandiri pengguna.</p>
 *
 * <h2>Kardinalitas: 1&ndash;1 dalam praktik, 1&ndash;N menurut skema</h2>
 *
 * <p>Pasangan relasinya <b>melingkar</b> dan keduanya {@code ManyToOne}:</p>
 * <ul>
 *   <li>{@link #getJurusan()} &rarr; FK {@code jenjang_program_studi.jurusan} ({@code NOT NULL});</li>
 *   <li>{@link Jurusan#getJenjangProgramStudi()} &rarr; FK {@code jurusan.jenjang_program_studi}
 *   (opsional), yang menunjuk balik ke baris ini.</li>
 * </ul>
 *
 * <p>Tidak ada {@code unique constraint} pada {@code jurusan}, dan pemeriksaan duplikat yang
 * pernah ditulis ({@code JenjangProgramStudiAction.checkJenjangProgramStudi}) sekarang
 * <b>dinonaktifkan</b> — pemanggilnya dikomentari dan method-nya bertanda
 * {@code @SuppressWarnings("unused")}. Jadi secara teknis satu {@link Jurusan} <b>bisa</b> punya
 * banyak baris kelas ini. Kode pembaca menyiasatinya dengan mengambil satu baris saja:</p>
 * <ul>
 *   <li>{@code JurusanAction.initTentangProdi} dan {@code LaporanFormatSarjana} memakai
 *   {@code order by id desc limit 1} — <b>baris terbaru menang</b>, lalu hasilnya "dipatri"
 *   ke {@link Jurusan#setJenjangProgramStudi(JenjangProgramStudi)};</li>
 *   <li>{@code KrsNonPaketHelper} (cetak tagihan) dan {@code epsbed/MasterProgramStudi} (ekspor
 *   EPSBED) memakai {@code limit 1} <b>tanpa {@code order by}</b> — jika duplikat sempat
 *   terbentuk, baris yang terpilih tidak deterministik.</li>
 * </ul>
 *
 * <p>Kedua pemanggil pertama juga <b>membuat baris otomatis</b> bila belum ada (auto-seed:
 * {@code new JenjangProgramStudi()} + {@code setJurusan} + {@code setJenjang} + {@code save} +
 * {@code commit}). Membuka tab "Tentang Prodi" karena itu bukan operasi baca-saja.</p>
 *
 * <h2>Pengelompokan properti</h2>
 * <ol>
 *   <li><b>Identitas &amp; relasi.</b> {@link #getId()}, {@link #getJurusan()},
 *   {@link #getJenjang()}, {@link #getNama()} (lihat peringatan di bawah).</li>
 *   <li><b>Pendirian &amp; izin DIKTI.</b> {@link #getTanggalBerdiri()},
 *   {@link #getPejabatSkBerdiri()}, {@link #getNoSKDikti()}, {@link #getTglMulaiSKDikti()},
 *   {@link #getTglAkhirSKDikti()}, {@link #getTglMulaiOperasional()},
 *   {@link #getDimulaiDariSemester()}, {@link #getStatus()}.</li>
 *   <li><b>Akreditasi.</b> {@link #getNoSKAkreditasi()}, {@link #getTglMulaiSKAkreditasi()},
 *   {@link #getTglAkhirSKAkreditasi()}, {@link #getStatusAkreditasi()}. Perhatikan
 *   {@link Jurusan} <i>juga</i> menyimpan set akreditasinya sendiri (BAN-PT dan LAM-PTKes);
 *   field di sini adalah set ketiga yang dipakai layar "Tentang Prodi"/pelaporan, dan tidak ada
 *   sinkronisasi otomatis di antara ketiganya.</li>
 *   <li><b>Syarat kelulusan.</b> {@link #getSksLulus()} ({@code String}, bukan angka),
 *   {@link #getSksWajibLulus()}, {@link #getSksPilihanLulus()}, {@link #getSksPerSemester()},
 *   {@link #getStandardToefl()}, {@link #getStandardToafl()}.</li>
 *   <li><b>Kontak &amp; pejabat prodi.</b> {@link #getNidnKaPS()}, {@link #getNmKaPS()},
 *   {@link #getTelpKaPS()}, {@link #getTelpPS()}, {@link #getFaxPS()},
 *   {@link #getHomepagePS()}, {@link #getEmail()}, {@link #appendEmail(String)},
 *   {@link #getNamaOperator()}, {@link #getHpOperator()}.</li>
 *   <li><b>Kurikulum.</b> {@link #getFrekuensiKurikulum()},
 *   {@link #getPelaksanaanKurikulum()} — versi teks bebas, berdampingan dengan versi
 *   ber-referensi {@link #getEpsbedFrekuensiKurikulum()} /
 *   {@link #getEpsbedPelaksanaanKurikulum()}.</li>
 *   <li><b>Kode pelaporan EPSBED.</b> {@link #getEpsbedStatus()},
 *   {@link #getEpsbedTahunHapus()}, {@link #getEpsbedStatusAkreditasi()},
 *   {@link #getEpsbedFrekuensiKurikulum()}, {@link #getEpsbedPelaksanaanKurikulum()}.</li>
 *   <li><b>Audit.</b> {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()} dan kait {@link #onUpdate()}.</li>
 * </ol>
 *
 * <h2>Hal non-obvious yang wajib diketahui sebelum menyunting</h2>
 *
 * <p><b>1. Pemetaan memakai akses property.</b> Anotasi JPA dipasang pada <i>getter</i>, jadi
 * Hibernate membaca nilai kolom lewat getter. Setiap "normalisasi" di dalam getter bukan sekadar
 * kosmetik tampilan: nilai itulah yang ikut ter-{@code INSERT}/{@code UPDATE}.</p>
 *
 * <p><b>2. Getter yang menulis balik ke field.</b> Ada tiga pola di kelas ini:</p>
 * <ul>
 *   <li>{@link #getJurusan()} dan {@link #getJenjang()} — {@code field = check(field)}, resolusi
 *   proxy lazy standar seluruh repo; lihat {@link GeneralValueObject#check(Object)};</li>
 *   <li>{@link #getEmail()} — normalisasi diam-diam ({@code ",,"} dirapatkan, {@code null} dan
 *   {@code ","} jadi {@code ""}) yang <b>menimpa field</b>;</li>
 *   <li>{@link #getNama()} — <b>menimpa field dengan {@code "-"}</b> pada hampir semua baris
 *   nyata. Lihat peringatan di method itu; ini paling mungkin salah ketik lama, tetapi
 *   perilakunya sudah tertanam di data produksi sehingga <b>jangan diubah tanpa migrasi</b>.</li>
 * </ul>
 *
 * <p>Penulisan balik hanya benar-benar tersimpan bila instance-nya <i>attached</i> pada session
 * Hibernate yang aktif; pada instance <i>detached</i> (hasil cache/serialisasi) efeknya hanya di
 * memori. Getter default murni ({@link #getSksPerSemester()}, {@link #getSksWajibLulus()},
 * {@link #getSksPilihanLulus()}) <b>tidak</b> menulis balik — nilai default hanya dikembalikan,
 * kolomnya tetap {@code NULL} di DB.</p>
 *
 * <p><b>3. Tidak ada getter di kelas ini yang menutup session Hibernate.</b> Semua akses DB yang
 * terjadi berasal dari {@link GeneralValueObject#check(Object)} (yang mengurus session
 * sementaranya sendiri). Penutupan session pada alur simpan dilakukan oleh
 * {@code JenjangProgramStudiAction.onSave}, bukan oleh entity ini.</p>
 *
 * <p><b>4. Kolom tanpa {@code @Column}.</b> {@link #getHomepagePS()},
 * {@link #getPejabatSkBerdiri()}, {@link #getSksWajibLulus()}, {@link #getSksPilihanLulus()} dan
 * {@link #getTglMulaiOperasional()} tidak dianotasi nama kolom. Karena
 * {@code ais.database.hibernate.MyNamingStrategy} adalah turunan {@code DefaultNamingStrategy}
 * (nama kolom = nama properti apa adanya, tanpa konversi ke {@code snake_case}), kolomnya
 * bernama persis seperti nama properti — bukan {@code homepage_ps} atau {@code sks_wajib_lulus}.
 * Menambahkan {@code @Column} "supaya rapi" akan menunjuk kolom yang tidak ada.</p>
 *
 * <p><b>5. Field audit dideklarasikan ulang.</b> {@code id}, {@code oleh}, {@code olehId} dan
 * {@code tanggal_dirubah} muncul lagi di sini meskipun {@link GeneralValueObject} punya
 * padanannya. Itu <b>bukan bug</b>: {@link GeneralValueObject} adalah POJO abstrak biasa —
 * bukan {@code @Entity} maupun {@code @MappedSuperclass} — sehingga Hibernate tidak memetakan
 * properti induk sama sekali. Deklarasi ulang di setiap entity adalah keharusan teknis.</p>
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap
 * dimiliki {@link GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar
 * spesifik untuk entity ini; perubahan yang berlaku bagi seluruh keluarga entity harus
 * ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang tindih. Panggil operasinya
 * lewat alur service dengan session, transaksi, dan otorisasi yang sesuai — jangan menyalin
 * perilakunya ke tempat lain.</p>
 *
 * @see GeneralValueObject
 * @see Jurusan
 * @see Jenjang
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "jenjang_program_studi")
public class JenjangProgramStudi extends GeneralValueObject {

	/**
	 * Versi serialisasi. Entity ini ikut diserialisasi ke session ZK dan ke cache; jangan diubah
	 * kecuali memang menginginkan instance lama tidak bisa dibaca lagi.
	 */
	private static final long serialVersionUID = -2314772569384463271L;

	/** Primary key {@code jenjang_program_studi.id}; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah tersentuh audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah.
	 *
	 * <p><b>Menolak diam-diam</b> nilai {@code null} maupun string kosong/spasi: jejak audit yang
	 * sudah ada tidak boleh terhapus oleh alur yang kebetulan tidak punya konteks pengguna
	 * (mis. proses batch). Pola yang sama dipakai di seluruh entity repo ini.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null}/kosong <b>diabaikan</b> agar jejak audit lama tidak tertimpa.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum {@code UPDATE} baris ini,
	 * lalu mendelegasikan pengisian stempel audit ({@code oleh}/{@code olehId}/
	 * {@code tanggal_dirubah}) ke {@code ais.database.hibernate.AuditTimestampInterceptor#ubah}.
	 *
	 * <p>Tidak berlaku pada {@code INSERT} — nilai awal {@code tanggal_dirubah} berasal dari
	 * inisialisasi field di baris yang sama ({@code ais.ui.util.WaktuUtil.getDate()}).</p>
	 *
	 * <p><b>Catatan format:</b> deklarasi field {@code tanggal_dirubah} sengaja ditempel pada
	 * baris yang sama dengan method ini di seluruh entity repo (hasil penyuntingan massal).
	 * Jangan dipisah tanpa alasan — perubahan whitespace di ribuan file mempersulit
	 * {@code svn blame}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Umumnya diisi otomatis lewat {@link #onUpdate()};
	 * pemanggilan manual hanya untuk kebutuhan migrasi/impor.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (kolom {@code timestamp}).
	 *
	 * @return stempel waktu; tidak pernah {@code null} pada instance baru karena field-nya
	 *         diinisialisasi saat objek dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks entity ini: <b>hanya nama jenjang</b> ({@code "S1"}, {@code "S2"}, ...),
	 * bukan nama prodi dan bukan id.
	 *
	 * <p>Dipakai oleh komponen ZK (mis. isi {@code Listitem}/{@code Combobox}) dan oleh
	 * {@code ManajemenProperty.safeToString()} pada jalur report/API. Karena itu method ini
	 * <b>wajib aman dipanggil di luar session Hibernate</b>.</p>
	 *
	 * <p>Mengembalikan string kosong pada dua keadaan: relasi {@code jenjang} memang
	 * {@code null}, atau proxy lazy-nya belum ter-inisialisasi. Keadaan kedua sengaja tidak
	 * dipaksa ter-load (tidak memanggil {@link #getJenjang()}/{@code check()}) agar toString()
	 * tidak diam-diam menembak database dari thread report. Konsekuensinya: label bisa tampil
	 * kosong padahal datanya ada — panggil {@link #getJenjang()} lebih dulu bila memang butuh
	 * nilainya.</p>
	 *
	 * @return nama jenjang, atau {@code ""} bila tidak tersedia/aman diambil
	 */
	// FIX LazyInitializationException: "jenjang" bisa berupa proxy Hibernate
	// lazy yang belum di-initialize saat toString() dipanggil dari luar sesi
	// (mis. via ManajemenProperty.safeToString() pada thread report/API login
	// setelah sesi ditutup). "jenjang + \"\"" memaksa toString() proxy tsb
	// tanpa cek -> LazyInitializationException mentah yg lolos ke caller.
	// Cek isInitialized dulu; jika belum, kembalikan representasi aman.
	public String toString() {
		if (jenjang == null) {
			return "";
		}
		if (!Hibernate.isInitialized(jenjang)) {
			return "";
		}
		return jenjang + "";
	}

	/** Kolom {@code nama} (50 karakter). Praktisnya selalu bernilai {@code "-"}; lihat {@link #getNama()}. */
	private String nama;

	/**
	 * Mengembalikan kolom {@code nama} — dalam praktik <b>selalu {@code "-"}</b>.
	 *
	 * <p><b>PERINGATAN — perilaku yang hampir pasti tidak disengaja.</b> Syarat penulisan balik
	 * di sini adalah {@code jenjang != null}, bukan {@code nama == null}. Bandingkan dengan
	 * {@link Jenjang#getNama()} yang berbunyi {@code if (nama == null) nama = "-";} — bentuk itu
	 * masuk akal (mengisi kekosongan), sedangkan bentuk di sini <b>menimpa nama yang sudah
	 * terisi</b> setiap kali getter dipanggil, selama relasi jenjang ada. Karena pemetaan
	 * memakai akses property dan hampir semua baris nyata punya {@code jenjang}, nilai
	 * {@code "-"} ikut tersimpan ke kolom {@code nama} pada {@code UPDATE} berikutnya.</p>
	 *
	 * <p>Dampaknya kecil karena tidak ada layar yang membaca atau mengisi properti ini:
	 * {@code JenjangProgramStudiAction.onSave} tidak pernah memanggil {@link #setNama(String)},
	 * dan label prodi selalu diambil dari {@link Jurusan#getNama()}. Tetap saja:
	 * <b>jangan "perbaiki" kondisinya tanpa memeriksa data produksi</b> — nilai lama sudah
	 * telanjur tergerus menjadi {@code "-"} beserta seluruh revisi Envers-nya, jadi mengubah
	 * kondisi tidak akan mengembalikan nama yang hilang, hanya mengubah perilaku ke depan.</p>
	 *
	 * @return {@code "-"} untuk baris yang punya jenjang; selain itu nilai kolom yang sudah
	 *         di-{@code trim}, atau {@code null} bila kolomnya kosong
	 */
	@Column(name = "nama", length = 50)
	public String getNama() {
		if (jenjang != null) {
			nama = "-";
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel kolom {@code nama}. Tidak dipakai layar mana pun, dan nilainya akan tertimpa
	 * {@code "-"} pada pemanggilan {@link #getNama()} berikutnya bila {@code jenjang} terisi.
	 *
	 * @param nama nilai baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** Prodi induk (wajib, kolom {@code jurusan} {@code NOT NULL}). Lihat {@link #getJurusan()}. */
	private Jurusan jurusan;
	/** Salinan jenjang prodi induk (opsional). Lihat {@link #getJenjang()}. */
	private Jenjang jenjang;

	/** Tanggal berdirinya prodi; dipakai ekspor EPSBED. Lihat {@link #getTanggalBerdiri()}. */
	private Date tanggalBerdiri;
	/** Daftar email prodi, dipisah koma dalam satu kolom. Lihat {@link #getEmail()}. */
	private String email;
	/** Total SKS syarat lulus — bertipe {@code String}, lihat {@link #getSksLulus()}. */
	private String sksLulus;
	/** Batas SKS per semester; kosong dianggap 20. Lihat {@link #getSksPerSemester()}. */
	private Integer sksPerSemester;
	/** SKS mata kuliah wajib untuk lulus; kosong dianggap 0. Lihat {@link #getSksWajibLulus()}. */
	private Integer sksWajibLulus;
	/** SKS mata kuliah pilihan untuk lulus; kosong dianggap 0. Lihat {@link #getSksPilihanLulus()}. */
	private Integer sksPilihanLulus;
	/** Status penyelenggaraan prodi (teks bebas). Lihat {@link #getStatus()}. */
	private String status;
	/** Semester awal prodi mulai menerima mahasiswa. Lihat {@link #getDimulaiDariSemester()}. */
	private String dimulaiDariSemester;
	/** NIDN ketua prodi (disalin manual, bukan relasi ke {@link Dosen}). Lihat {@link #getNidnKaPS()}. */
	private String nidnKaPS;
	/** Nama ketua prodi (disalin manual). Lihat {@link #getNmKaPS()}. */
	private String nmKaPS;
	/** Telepon ketua prodi. Lihat {@link #getTelpKaPS()}. */
	private String telpKaPS;
	/** Telepon kantor prodi. Lihat {@link #getTelpPS()}. */
	private String telpPS;
	/** Faksimile prodi. Lihat {@link #getFaxPS()}. */
	private String faxPS;
	/** Situs web prodi; kolom tanpa {@code @Column}. Lihat {@link #getHomepagePS()}. */
	private String homepagePS;
	/** Nama operator/petugas pelaporan prodi. Lihat {@link #getNamaOperator()}. */
	private String namaOperator;
	/** Nomor HP operator pelaporan. Lihat {@link #getHpOperator()}. */
	private String hpOperator;
	/** Frekuensi peninjauan kurikulum, versi teks bebas. Lihat {@link #getFrekuensiKurikulum()}. */
	private String frekuensiKurikulum;
	/** Pelaksanaan kurikulum, versi teks bebas. Lihat {@link #getPelaksanaanKurikulum()}. */
	private String pelaksanaanKurikulum;
	/** Nomor SK izin penyelenggaraan dari DIKTI. Lihat {@link #getNoSKDikti()}. */
	private String noSKDikti;
	/** Tanggal mulai berlaku SK DIKTI. Lihat {@link #getTglMulaiSKDikti()}. */
	private Date tglMulaiSKDikti;
	/** Tanggal akhir berlaku SK DIKTI. Lihat {@link #getTglAkhirSKDikti()}. */
	private Date tglAkhirSKDikti;
	/** Tanggal prodi mulai beroperasi; kolom tanpa {@code @Column}. Lihat {@link #getTglMulaiOperasional()}. */
	private Date tglMulaiOperasional;
	/** Nomor SK akreditasi. Lihat {@link #getNoSKAkreditasi()}. */
	private String noSKAkreditasi;
	/** Tanggal mulai berlaku SK akreditasi. Lihat {@link #getTglMulaiSKAkreditasi()}. */
	private Date tglMulaiSKAkreditasi;
	/** Tanggal akhir berlaku SK akreditasi. Lihat {@link #getTglAkhirSKAkreditasi()}. */
	private Date tglAkhirSKAkreditasi;
	/** Pejabat penanda tangan SK pendirian; kolom tanpa {@code @Column}. Lihat {@link #getPejabatSkBerdiri()}. */
	private String pejabatSkBerdiri;
	/** Peringkat akreditasi sebagai teks bebas (A/B/C/...). Lihat {@link #getStatusAkreditasi()}. */
	private String statusAkreditasi;
	/** Skor TOEFL minimal syarat lulus. Lihat {@link #getStandardToefl()}. */
	private Integer standardToefl;
	/** Skor TOAFL (bahasa Arab) minimal syarat lulus. Lihat {@link #getStandardToafl()}. */
	private Integer standardToafl;

	/** Kode status prodi versi EPSBED; kolom {@code epsbed_status_jurusan}. Lihat {@link #getEpsbedStatus()}. */
	private EpsbedStatus epsbedStatus;
	/** Tahun prodi dihapus/ditutup menurut pelaporan EPSBED. Lihat {@link #getEpsbedTahunHapus()}. */
	private String epsbedTahunHapus;
	/** Kode status akreditasi versi EPSBED. Lihat {@link #getEpsbedStatusAkreditasi()}. */
	private EpsbedStatusAkreditasi epsbedStatusAkreditasi;
	/** Kode frekuensi kurikulum versi EPSBED. Lihat {@link #getEpsbedFrekuensiKurikulum()}. */
	private EpsbedFrekuensiKurikulum epsbedFrekuensiKurikulum;
	/** Kode pelaksanaan kurikulum versi EPSBED. Lihat {@link #getEpsbedPelaksanaanKurikulum()}. */
	private EpsbedPelaksanaanKurikulum epsbedPelaksanaanKurikulum;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA dan dipakai layar saat menambah baris
	 * baru. Seluruh field kosong kecuali {@code tanggal_dirubah} (diisi waktu sekarang).
	 *
	 * <p>Baris baru <b>belum sah</b> sebelum {@link #setJurusan(Jurusan)} dipanggil — kolom
	 * {@code jurusan} {@code NOT NULL}.</p>
	 */
	public JenjangProgramStudi() {
	}

	/**
	 * Konstruktor pintasan berisi primary key saja, untuk membentuk referensi ringan tanpa
	 * membaca database (mis. sebagai nilai pembanding pada {@code Criteria}).
	 *
	 * @param id primary key baris yang diacu
	 */
	public JenjangProgramStudi(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * <p>Kolom dipetakan {@code insertable = false} dengan strategi {@code IDENTITY}: nilainya
	 * dibangkitkan oleh database (sequence tabel), bukan oleh aplikasi. Karena itu
	 * {@code getId() == null} adalah penanda baku "entity belum tersimpan" — dipakai antara lain
	 * oleh {@code JurusanAction.initTentangProdi} dan {@code JenjangProgramStudiAction.onSave}
	 * untuk memilih antara {@code save()} dan {@code update()}.</p>
	 *
	 * @return primary key, atau {@code null} bila entity belum di-{@code flush} ke database
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/**
	 * Menyetel primary key. Hanya untuk kebutuhan pemuatan/impor; menyetel id pada entity yang
	 * sudah dikelola session akan mengacaukan identitas Hibernate.
	 *
	 * @param id primary key
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/*
	 * @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	 *
	 * @Column(name="fakultas",nullable=false) public Fakultas getFakultas() {
	 * return fakultas; }
	 *
	 * public void setFakultas(Fakultas fakultas) { this.fakultas = fakultas; }
	 */

	/**
	 * Mengembalikan prodi induk baris ini (FK {@code jurusan}, {@code NOT NULL}).
	 *
	 * <p><b>Efek samping:</b> memanggil {@link GeneralValueObject#check(Object)} dan
	 * <b>menugaskan hasilnya kembali ke field</b>. Bila relasinya masih berupa proxy lazy yang
	 * belum ter-inisialisasi, {@code check()} akan mencoba berturut-turut: flag {@code initData},
	 * session yang sedang aktif, cache in-memory, lalu pembacaan ulang lewat session baru.
	 * Artinya getter ini <b>bisa menembak database</b> dan tidak murni. Bila keempat cara gagal,
	 * {@code check()} mengembalikan argumen apa adanya (proxy tetap belum ter-inisialisasi) —
	 * bukan {@code null} dan bukan exception.</p>
	 *
	 * <p>Relasi memakai {@code cascade = {PERSIST, MERGE}}: menyimpan baris ini ikut menyimpan
	 * perubahan pada {@link Jurusan} yang tertaut. Tidak ada {@code cascade REMOVE}.</p>
	 *
	 * @return prodi induk; secara skema tidak boleh {@code null}, tetapi pada instance yang
	 *         belum diisi/lolos {@code check()} bisa saja {@code null}
	 * @see GeneralValueObject#check(Object)
	 * @see Jurusan#getJenjangProgramStudi()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = false)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Menyetel prodi induk. Tidak ada validasi bahwa prodi tersebut belum punya baris
	 * "Tentang Prodi" lain — duplikat dimungkinkan, lihat catatan kardinalitas pada Javadoc
	 * kelas.
	 *
	 * @param jurusan prodi induk; menyimpan dengan nilai {@code null} akan ditolak database
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan jenjang ({@code S1}/{@code S2}/{@code D3}/...) yang tercatat pada baris ini.
	 *
	 * <p>Nilainya adalah <b>salinan</b> {@link Jurusan#getJenjang()} saat baris dibuat; tidak ada
	 * mekanisme yang menyinkronkannya kembali bila jenjang prodi induk kemudian diubah.</p>
	 *
	 * <p><b>Efek samping:</b> sama dengan {@link #getJurusan()} — {@code field = check(field)},
	 * berpotensi membaca database untuk menuntaskan proxy lazy. Perhatikan bahwa
	 * {@link #getNama()} dan {@link #toString()} membaca field {@code jenjang} <b>secara
	 * langsung</b> tanpa lewat getter ini, sehingga keduanya melihat keadaan proxy apa adanya.</p>
	 *
	 * @return jenjang, atau {@code null} bila kolom {@code jenjang} kosong (kolomnya opsional)
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenjang")
	public Jenjang getJenjang() {
		jenjang = check(jenjang);
		return jenjang;
	}

	/**
	 * Menyetel jenjang. Pemanggil di repo selalu mengisinya dengan {@code jurusan.getJenjang()};
	 * tidak ada validasi kecocokan dengan prodi induk.
	 *
	 * @param jenjang jenjang baru, boleh {@code null}
	 */
	public void setJenjang(Jenjang jenjang) {
		this.jenjang = jenjang;
	}

	/**
	 * Mengembalikan tanggal berdirinya prodi (kolom {@code tanggal_berdiri}). Dipakai kolom
	 * "tanggal berdiri" pada ekspor EPSBED {@code epsbed/MasterProgramStudi}.
	 *
	 * @return tanggal berdiri, atau {@code null} bila belum diisi
	 */
	@Column(name = "tanggal_berdiri")
	public Date getTanggalBerdiri() {
		return tanggalBerdiri;
	}

	/**
	 * Menyetel tanggal berdirinya prodi.
	 *
	 * @param tanggalBerdiri tanggal baru, boleh {@code null}
	 */
	public void setTanggalBerdiri(Date tanggalBerdiri) {
		this.tanggalBerdiri = tanggalBerdiri;
	}

	/**
	 * Mengembalikan daftar email prodi — <b>satu kolom teks berisi banyak alamat yang dipisah
	 * koma</b>, bukan satu alamat.
	 *
	 * <p><b>Efek samping (normalisasi diam-diam yang menulis balik ke field):</b></p>
	 * <ol>
	 *   <li>bila mengandung {@code ",,"}, koma ganda dirapatkan — dijalankan berulang
	 *   <b>maksimal 5 kali</b>, jadi rentetan koma yang sangat panjang bisa tersisa;</li>
	 *   <li>{@code null} diganti string kosong, sehingga getter ini <b>tidak pernah
	 *   mengembalikan {@code null}</b> setelah dipanggil sekali;</li>
	 *   <li>nilai yang persis {@code ","} (setelah {@code trim}) dianggap kosong.</li>
	 * </ol>
	 *
	 * <p>Karena pemetaan memakai akses property, hasil normalisasi ikut tersimpan pada
	 * {@code UPDATE} berikutnya bila instance-nya <i>attached</i>. Perhatikan juga nilai yang
	 * dikembalikan adalah {@code this.email} <b>tanpa {@code trim}</b> — berbeda dari pemeriksaan
	 * internalnya yang memakai {@code trim()}, sehingga {@code " , "} lolos apa adanya.</p>
	 *
	 * @return daftar email dipisah koma; {@code ""} bila kosong, tidak pernah {@code null} pada
	 *         pemanggilan setelah yang pertama
	 */
	@Column(name = "email", length = 255)
	public String getEmail() {
		if (email != null && email.contains(",,")) {
			for (int i = 0; i < 5; i++) {
				email = email.replaceAll(",,", ",");
			}
		}
		if (email == null) {
			email = "";
		}
		if (email.trim().equals(",")) {
			email = "";
		}
		return this.email;
	}

	/**
	 * Mengganti seluruh daftar email dengan nilai baru (menimpa, bukan menambah). Dipakai
	 * {@code JenjangProgramStudiAction.onSave} dari isian layar.
	 *
	 * @param email daftar email dipisah koma
	 * @see #appendEmail(String)
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Menambahkan satu alamat email ke daftar tanpa menghapus yang sudah ada — bentuk yang sama
	 * dipakai {@code Mahasiswa}, {@code Siswa}, {@code Penduduk}, dan {@code TbmUser} untuk
	 * mengumpulkan email dari berbagai kanal (API, verifikasi, impor).
	 *
	 * <p>Alur pemeriksaannya:</p>
	 * <ol>
	 *   <li><b>Anti-duplikat:</b> bila daftar sekarang tidak {@code null} dan sudah
	 *   <i>mengandung</i> teks {@code email}, method langsung berhenti. Pemeriksaan memakai
	 *   {@link StringUtils#contains(String, String)} — pencocokan <b>substring</b>, bukan
	 *   per-elemen. Akibatnya {@code "budi@x.com"} dianggap sudah ada di dalam
	 *   {@code "abudi@x.com"} dan tidak akan ditambahkan.</li>
	 *   <li><b>Validasi:</b> nilai kosong ditolak, alamat harus lolos
	 *   {@link Common#isValidEmailAddress(String)}, dan tidak boleh diawali {@code "@"}.</li>
	 *   <li><b>Penggabungan:</b> bila daftar lama kosong, alamat menjadi satu-satunya isi;
	 *   selain itu ditempel dengan pemisah koma.</li>
	 * </ol>
	 *
	 * <p>Method ini <b>tidak</b> menyentuh database — perubahan baru tersimpan bila entity-nya
	 * ikut di-{@code flush}. Pada kelas ini belum ada satu pun pemanggil di repo: implementasinya
	 * ada karena pola bersama antar-entity, bukan karena alur yang aktif.</p>
	 *
	 * @param email alamat yang hendak ditambahkan; {@code null}, kosong, tidak valid, atau
	 *              diawali {@code "@"} diabaikan tanpa pesan
	 */
	public void appendEmail(String email) {
		if (this.email != null && email != null && !email.trim().isEmpty() && StringUtils.contains(this.email, email)) {
			return;
		}
		if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email) && !email.startsWith("@")) {
			this.email = this.email == null || this.email.trim().isEmpty() ? email : this.email + "," + email;
		}
	}

	/**
	 * Mengembalikan total SKS syarat kelulusan (kolom {@code sks_lulus}, 20 karakter).
	 *
	 * <p>Bertipe {@code String}, bukan angka: layar mengisinya lewat kotak teks dan ekspor EPSBED
	 * menuliskannya apa adanya, sehingga isinya bisa saja bukan bilangan murni. Lakukan
	 * konversi sendiri (dengan penanganan galat) bila butuh nilai numerik.</p>
	 *
	 * @return SKS syarat lulus sebagai teks, atau {@code null} bila belum diisi
	 */
	@Column(name = "sks_lulus", length = 20)
	public String getSksLulus() {
		return sksLulus;
	}

	/**
	 * Menyetel total SKS syarat kelulusan.
	 *
	 * @param sksLulus nilai baru (teks bebas)
	 */
	public void setSksLulus(String sksLulus) {
		this.sksLulus = sksLulus;
	}

	/**
	 * Mengembalikan status penyelenggaraan prodi (kolom {@code status}, 20 karakter, teks bebas
	 * dari isian layar — tidak ada daftar nilai baku).
	 *
	 * @return status, atau {@code null} bila belum diisi
	 */
	@Column(name = "status", length = 20)
	public String getStatus() {
		return status;
	}

	/**
	 * Menyetel status penyelenggaraan prodi.
	 *
	 * @param status nilai baru
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Mengembalikan semester saat prodi mulai menerima mahasiswa (kolom
	 * {@code dimulai_dari_semester}), dipakai pada pelaporan EPSBED.
	 *
	 * @return kode semester awal, atau {@code null} bila belum diisi
	 */
	@Column(name = "dimulai_dari_semester", length = 20)
	public String getDimulaiDariSemester() {
		return dimulaiDariSemester;
	}

	/**
	 * Menyetel semester awal penyelenggaraan prodi.
	 *
	 * @param dimulaiDariSemester kode semester baru
	 */
	public void setDimulaiDariSemester(String dimulaiDariSemester) {
		this.dimulaiDariSemester = dimulaiDariSemester;
	}

	/**
	 * Mengembalikan NIDN ketua program studi (kolom {@code nidn_ka_ps}).
	 *
	 * <p>Disimpan sebagai teks lepas, <b>bukan relasi</b> ke {@link Dosen}: bila ketua prodi
	 * berganti, nilai di sini tidak ikut berubah. {@code KrsNonPaketHelper} memakainya sebagai
	 * sumber utama tanda tangan pada cetak tagihan, dan baru jatuh ke
	 * {@link Jurusan#getKaprodi()} bila {@link #getNmKaPS()} kosong. Kedua isian ini juga
	 * <b>dikomentari</b> di {@code JenjangProgramStudiAction.onSave}, jadi tidak bisa diubah
	 * lewat layar itu.</p>
	 *
	 * @return NIDN ketua prodi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nidn_ka_ps", length = 50)
	public String getNidnKaPS() {
		return nidnKaPS;
	}

	/**
	 * Menyetel NIDN ketua program studi.
	 *
	 * @param nidnKaPS NIDN baru
	 */
	public void setNidnKaPS(String nidnKaPS) {
		this.nidnKaPS = nidnKaPS;
	}

	/**
	 * Mengembalikan nomor telepon ketua program studi (kolom {@code telp_ka_ps}).
	 *
	 * @return nomor telepon, atau {@code null} bila belum diisi
	 */
	@Column(name = "telp_ka_ps", length = 20)
	public String getTelpKaPS() {
		return telpKaPS;
	}

	/**
	 * Menyetel nomor telepon ketua program studi.
	 *
	 * @param telpKaPS nomor telepon baru
	 */
	public void setTelpKaPS(String telpKaPS) {
		this.telpKaPS = telpKaPS;
	}

	/**
	 * Mengembalikan nomor telepon kantor program studi (kolom {@code telp_ps}).
	 *
	 * @return nomor telepon, atau {@code null} bila belum diisi
	 */
	@Column(name = "telp_ps", length = 20)
	public String getTelpPS() {
		return telpPS;
	}

	/**
	 * Menyetel nomor telepon kantor program studi.
	 *
	 * @param telpPS nomor telepon baru
	 */
	public void setTelpPS(String telpPS) {
		this.telpPS = telpPS;
	}

	/**
	 * Mengembalikan nomor faksimile program studi (kolom {@code fax_ps}).
	 *
	 * @return nomor faksimile, atau {@code null} bila belum diisi
	 */
	@Column(name = "fax_ps", length = 20)
	public String getFaxPS() {
		return faxPS;
	}

	/**
	 * Menyetel nomor faksimile program studi.
	 *
	 * @param faxPS nomor faksimile baru
	 */
	public void setFaxPS(String faxPS) {
		this.faxPS = faxPS;
	}

	/**
	 * Mengembalikan nama operator/petugas yang mengurus pelaporan prodi (kolom
	 * {@code nama_operator}) — data kontak untuk keperluan EPSBED, bukan akun pengguna sistem.
	 *
	 * @return nama operator, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama_operator", length = 255)
	public String getNamaOperator() {
		return namaOperator;
	}

	/**
	 * Menyetel nama operator pelaporan prodi.
	 *
	 * @param namaOperator nama baru
	 */
	public void setNamaOperator(String namaOperator) {
		this.namaOperator = namaOperator;
	}

	/**
	 * Mengembalikan nomor HP operator pelaporan prodi (kolom {@code hp_operator}).
	 *
	 * @return nomor HP, atau {@code null} bila belum diisi
	 */
	@Column(name = "hp_operator", length = 20)
	public String getHpOperator() {
		return hpOperator;
	}

	/**
	 * Menyetel nomor HP operator pelaporan prodi.
	 *
	 * @param hpOperator nomor HP baru
	 */
	public void setHpOperator(String hpOperator) {
		this.hpOperator = hpOperator;
	}

	/**
	 * Mengembalikan frekuensi peninjauan kurikulum sebagai teks bebas (kolom
	 * {@code frekuensi_kurikulum}).
	 *
	 * <p>Berdampingan — dan tidak tersinkronisasi — dengan
	 * {@link #getEpsbedFrekuensiKurikulum()} yang merupakan referensi berkode untuk pelaporan.
	 * Keduanya diisi terpisah pada layar yang sama.</p>
	 *
	 * @return frekuensi kurikulum, atau {@code null} bila belum diisi
	 */
	@Column(name = "frekuensi_kurikulum", length = 20)
	public String getFrekuensiKurikulum() {
		return frekuensiKurikulum;
	}

	/**
	 * Menyetel frekuensi peninjauan kurikulum (teks bebas).
	 *
	 * @param frekuensiKurikulum nilai baru
	 */
	public void setFrekuensiKurikulum(String frekuensiKurikulum) {
		this.frekuensiKurikulum = frekuensiKurikulum;
	}

	/**
	 * Mengembalikan keterangan pelaksanaan kurikulum sebagai teks bebas (kolom
	 * {@code pelaksanaan_kurikulum}); padanan berkodenya adalah
	 * {@link #getEpsbedPelaksanaanKurikulum()}.
	 *
	 * @return pelaksanaan kurikulum, atau {@code null} bila belum diisi
	 */
	@Column(name = "pelaksanaan_kurikulum", length = 20)
	public String getPelaksanaanKurikulum() {
		return pelaksanaanKurikulum;
	}

	/**
	 * Menyetel keterangan pelaksanaan kurikulum (teks bebas).
	 *
	 * @param pelaksanaanKurikulum nilai baru
	 */
	public void setPelaksanaanKurikulum(String pelaksanaanKurikulum) {
		this.pelaksanaanKurikulum = pelaksanaanKurikulum;
	}

	/**
	 * Mengembalikan nomor SK izin penyelenggaraan prodi dari DIKTI (kolom {@code no_sk_dikti},
	 * hanya 20 karakter — nomor SK yang panjang bisa terpotong saat disimpan).
	 *
	 * @return nomor SK DIKTI, atau {@code null} bila belum diisi
	 */
	@Column(name = "no_sk_dikti", length = 20)
	public String getNoSKDikti() {
		return noSKDikti;
	}

	/**
	 * Menyetel nomor SK izin penyelenggaraan dari DIKTI.
	 *
	 * @param noSKDikti nomor SK baru
	 */
	public void setNoSKDikti(String noSKDikti) {
		this.noSKDikti = noSKDikti;
	}

	/**
	 * Mengembalikan tanggal mulai berlakunya SK DIKTI (kolom {@code tgl_mulai_sk_dikti}).
	 *
	 * @return tanggal mulai, atau {@code null} bila belum diisi
	 */
	@Column(name = "tgl_mulai_sk_dikti")
	public Date getTglMulaiSKDikti() {
		return tglMulaiSKDikti;
	}

	/**
	 * Menyetel tanggal mulai berlakunya SK DIKTI.
	 *
	 * @param tglMulaiSKDikti tanggal baru
	 */
	public void setTglMulaiSKDikti(Date tglMulaiSKDikti) {
		this.tglMulaiSKDikti = tglMulaiSKDikti;
	}

	/**
	 * Mengembalikan tanggal berakhirnya SK DIKTI (kolom {@code tgl_akhir_sk_dikti}) — penanda
	 * kapan izin penyelenggaraan prodi harus diperpanjang.
	 *
	 * @return tanggal akhir, atau {@code null} bila belum diisi
	 */
	@Column(name = "tgl_akhir_sk_dikti")
	public Date getTglAkhirSKDikti() {
		return tglAkhirSKDikti;
	}

	/**
	 * Menyetel tanggal berakhirnya SK DIKTI.
	 *
	 * @param tglAkhirSKDikti tanggal baru
	 */
	public void setTglAkhirSKDikti(Date tglAkhirSKDikti) {
		this.tglAkhirSKDikti = tglAkhirSKDikti;
	}

	/**
	 * Mengembalikan nomor SK akreditasi prodi (kolom {@code no_sk_akreditasi}).
	 *
	 * <p>Ini set akreditasi milik layar "Tentang Prodi"; {@link Jurusan} punya set akreditasi
	 * BAN-PT dan LAM-PTKes sendiri yang tidak disinkronkan dengan field ini.</p>
	 *
	 * @return nomor SK akreditasi, atau {@code null} bila belum diisi
	 */
	@Column(name = "no_sk_akreditasi", length = 30)
	public String getNoSKAkreditasi() {
		return noSKAkreditasi;
	}

	/**
	 * Menyetel nomor SK akreditasi prodi.
	 *
	 * @param noSKAkreditasi nomor SK baru
	 */
	public void setNoSKAkreditasi(String noSKAkreditasi) {
		this.noSKAkreditasi = noSKAkreditasi;
	}

	/**
	 * Mengembalikan tanggal mulai berlakunya SK akreditasi (kolom
	 * {@code tgl_mulai_sk_akreditasi}).
	 *
	 * @return tanggal mulai, atau {@code null} bila belum diisi
	 */
	@Column(name = "tgl_mulai_sk_akreditasi")
	public Date getTglMulaiSKAkreditasi() {
		return tglMulaiSKAkreditasi;
	}

	/**
	 * Menyetel tanggal mulai berlakunya SK akreditasi.
	 *
	 * @param tglMulaiSKAkreditasi tanggal baru
	 */
	public void setTglMulaiSKAkreditasi(Date tglMulaiSKAkreditasi) {
		this.tglMulaiSKAkreditasi = tglMulaiSKAkreditasi;
	}

	/**
	 * Mengembalikan tanggal berakhirnya SK akreditasi (kolom {@code tgl_akhir_sk_akreditasi}).
	 *
	 * <p><b>Catatan:</b> isian layar untuk properti ini <b>dikomentari</b> di
	 * {@code JenjangProgramStudiAction.onSave}, sehingga nilainya tidak pernah berubah lewat
	 * layar "Tentang Prodi" — hanya lewat impor/migrasi data.</p>
	 *
	 * @return tanggal akhir, atau {@code null} bila belum diisi
	 */
	@Column(name = "tgl_akhir_sk_akreditasi")
	public Date getTglAkhirSKAkreditasi() {
		return tglAkhirSKAkreditasi;
	}

	/**
	 * Menyetel tanggal berakhirnya SK akreditasi.
	 *
	 * @param tglAkhirSKAkreditasi tanggal baru
	 */
	public void setTglAkhirSKAkreditasi(Date tglAkhirSKAkreditasi) {
		this.tglAkhirSKAkreditasi = tglAkhirSKAkreditasi;
	}

	/**
	 * Mengembalikan status/peringkat akreditasi sebagai teks bebas (kolom
	 * {@code status_akreditasi}, 200 karakter). Padanan berkodenya untuk pelaporan adalah
	 * {@link #getEpsbedStatusAkreditasi()}.
	 *
	 * @return status akreditasi, atau {@code null} bila belum diisi
	 */
	@Column(name = "status_akreditasi", length = 200)
	public String getStatusAkreditasi() {
		return statusAkreditasi;
	}

	/**
	 * Menyetel status/peringkat akreditasi (teks bebas).
	 *
	 * @param statusAkreditasi nilai baru
	 */
	public void setStatusAkreditasi(String statusAkreditasi) {
		this.statusAkreditasi = statusAkreditasi;
	}

	/**
	 * Mengembalikan nama ketua program studi (kolom {@code nm_ka_ps}).
	 *
	 * <p>Sama seperti {@link #getNidnKaPS()}: teks lepas, bukan relasi ke {@link Dosen}, dan
	 * isiannya dikomentari di {@code JenjangProgramStudiAction.onSave}. Nilai kosong pada
	 * properti inilah yang membuat {@code KrsNonPaketHelper} beralih memakai
	 * {@link Jurusan#getKaprodi()} sebagai penanda tangan cetak tagihan.</p>
	 *
	 * @return nama ketua prodi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nm_ka_ps", length = 100)
	public String getNmKaPS() {
		return nmKaPS;
	}

	/**
	 * Menyetel nama ketua program studi.
	 *
	 * @param nmKaPS nama baru
	 */
	public void setNmKaPS(String nmKaPS) {
		this.nmKaPS = nmKaPS;
	}

	/**
	 * Mengembalikan skor TOEFL minimal sebagai syarat kelulusan prodi ini (kolom
	 * {@code standard_toefl}).
	 *
	 * <p>Atribut {@code length = 100} pada anotasi tidak berpengaruh untuk tipe numerik —
	 * sisa penyuntingan lama, bukan penanda batas nilai.</p>
	 *
	 * @return skor minimal, atau {@code null} bila prodi tidak mensyaratkan TOEFL
	 */
	@Column(name = "standard_toefl", length = 100)
	public Integer getStandardToefl() {
		return standardToefl;
	}

	/**
	 * Menyetel skor TOEFL minimal syarat kelulusan.
	 *
	 * @param standardToefl skor minimal, boleh {@code null}
	 */
	public void setStandardToefl(Integer standardToefl) {
		this.standardToefl = standardToefl;
	}

	/**
	 * Mengembalikan skor TOAFL (tes bahasa Arab) minimal sebagai syarat kelulusan (kolom
	 * {@code standard_toafl}); dipakai perguruan tinggi keagamaan Islam. Sama seperti
	 * {@link #getStandardToefl()}, {@code length = 100} tidak bermakna untuk tipe numerik.
	 *
	 * @return skor minimal, atau {@code null} bila tidak disyaratkan
	 */
	@Column(name = "standard_toafl", length = 100)
	public Integer getStandardToafl() {
		return standardToafl;
	}

	/**
	 * Menyetel skor TOAFL minimal syarat kelulusan.
	 *
	 * @param standardToafl skor minimal, boleh {@code null}
	 */
	public void setStandardToafl(Integer standardToafl) {
		this.standardToafl = standardToafl;
	}

	/**
	 * Menyetel kode status prodi versi EPSBED.
	 *
	 * @param epsbedStatus referensi status EPSBED, boleh {@code null}
	 */
	public void setEpsbedStatus(EpsbedStatus epsbedStatus) {
		this.epsbedStatus = epsbedStatus;
	}

	/**
	 * Mengembalikan kode status prodi versi EPSBED (aktif/tutup/dsb.), dipakai kolom status pada
	 * ekspor {@code epsbed/MasterProgramStudi}.
	 *
	 * <p><b>Nama kolomnya tidak sejalan dengan nama properti:</b> FK-nya adalah
	 * {@code epsbed_status_jurusan}, bukan {@code epsbed_status} — peninggalan masa ketika data
	 * ini melekat pada jurusan.</p>
	 *
	 * <p>Relasi ini {@code ManyToOne} <b>tanpa {@code fetch = LAZY}</b>, jadi bersifat
	 * <i>eager</i> dan dimuat lewat {@code SELECT} terpisah ({@link FetchMode#SELECT}) setiap
	 * kali entity dibaca. Karena eager, getter ini tidak perlu — dan memang tidak — memanggil
	 * {@link GeneralValueObject#check(Object)}; tidak ada efek samping di sini.</p>
	 *
	 * @return referensi status EPSBED, atau {@code null} bila belum dipilih
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "epsbed_status_jurusan")
	public EpsbedStatus getEpsbedStatus() {
		return epsbedStatus;
	}

	/**
	 * Menyetel tahun prodi dihapus/ditutup menurut pelaporan EPSBED.
	 *
	 * @param epsbedTahunHapus tahun (teks), boleh {@code null}
	 */
	public void setEpsbedTahunHapus(String epsbedTahunHapus) {
		this.epsbedTahunHapus = epsbedTahunHapus;
	}

	/**
	 * Mengembalikan tahun prodi dihapus/ditutup menurut pelaporan EPSBED (kolom
	 * {@code epsbed_tahun_hapus}); ikut ditulis ke berkas ekspor EPSBED.
	 *
	 * @return tahun sebagai teks, atau {@code null} bila prodi masih berjalan
	 */
	@Column(name = "epsbed_tahun_hapus")
	public String getEpsbedTahunHapus() {
		return epsbedTahunHapus;
	}

	/**
	 * Menyetel kode status akreditasi versi EPSBED.
	 *
	 * @param epsbedStatusAkreditasi referensi status akreditasi EPSBED, boleh {@code null}
	 */
	public void setEpsbedStatusAkreditasi(EpsbedStatusAkreditasi epsbedStatusAkreditasi) {
		this.epsbedStatusAkreditasi = epsbedStatusAkreditasi;
	}

	/**
	 * Mengembalikan kode status akreditasi versi EPSBED (FK
	 * {@code epsbed_status_akreditasi}) — padanan berkode dari
	 * {@link #getStatusAkreditasi()} yang berupa teks bebas.
	 *
	 * <p>Relasi eager dengan {@link FetchMode#SELECT}; tanpa efek samping.</p>
	 *
	 * @return referensi status akreditasi EPSBED, atau {@code null} bila belum dipilih
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "epsbed_status_akreditasi")
	public EpsbedStatusAkreditasi getEpsbedStatusAkreditasi() {
		return epsbedStatusAkreditasi;
	}

	/**
	 * Menyetel kode frekuensi peninjauan kurikulum versi EPSBED.
	 *
	 * @param epsbedFrekuensiKurikulum referensi frekuensi kurikulum EPSBED, boleh {@code null}
	 */
	public void setEpsbedFrekuensiKurikulum(EpsbedFrekuensiKurikulum epsbedFrekuensiKurikulum) {
		this.epsbedFrekuensiKurikulum = epsbedFrekuensiKurikulum;
	}

	/**
	 * Mengembalikan kode frekuensi peninjauan kurikulum versi EPSBED (FK
	 * {@code epsbed_frekuensi_kurikulum}) — padanan berkode dari
	 * {@link #getFrekuensiKurikulum()}.
	 *
	 * <p>Relasi eager dengan {@link FetchMode#SELECT}; tanpa efek samping.</p>
	 *
	 * @return referensi frekuensi kurikulum EPSBED, atau {@code null} bila belum dipilih
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "epsbed_frekuensi_kurikulum")
	public EpsbedFrekuensiKurikulum getEpsbedFrekuensiKurikulum() {
		return epsbedFrekuensiKurikulum;
	}

	/**
	 * Menyetel kode pelaksanaan kurikulum versi EPSBED.
	 *
	 * @param epsbedPelaksanaanKurikulum referensi pelaksanaan kurikulum EPSBED, boleh
	 *                                   {@code null}
	 */
	public void setEpsbedPelaksanaanKurikulum(EpsbedPelaksanaanKurikulum epsbedPelaksanaanKurikulum) {
		this.epsbedPelaksanaanKurikulum = epsbedPelaksanaanKurikulum;
	}

	/**
	 * Mengembalikan kode pelaksanaan kurikulum versi EPSBED (FK
	 * {@code epsbed_pelaksanaan_kurikulum}) — padanan berkode dari
	 * {@link #getPelaksanaanKurikulum()}.
	 *
	 * <p>Relasi eager dengan {@link FetchMode#SELECT}; tanpa efek samping.</p>
	 *
	 * @return referensi pelaksanaan kurikulum EPSBED, atau {@code null} bila belum dipilih
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "epsbed_pelaksanaan_kurikulum")
	public EpsbedPelaksanaanKurikulum getEpsbedPelaksanaanKurikulum() {
		return epsbedPelaksanaanKurikulum;
	}

	/**
	 * Mengembalikan batas SKS yang boleh diambil per semester (kolom
	 * {@code sks_per_semester}), dengan <b>default 20</b> bila kolomnya kosong.
	 *
	 * <p>Default hanya dikembalikan, <b>tidak ditulis balik</b> ke field — kolomnya tetap
	 * {@code NULL} di database dan angka 20 akan dihitung ulang setiap pemanggilan. Karena
	 * pemetaan memakai akses property, nilai yang disimpan Hibernate saat {@code INSERT}/
	 * {@code UPDATE} adalah <b>hasil getter ini</b>, yaitu 20 — jadi baris yang pernah disimpan
	 * lewat aplikasi tidak lagi {@code NULL}. Perbedaan itu penting bila ada query SQL langsung
	 * yang membedakan "belum diatur" dari "diatur 20".</p>
	 *
	 * @return batas SKS per semester; 20 bila belum diatur
	 */
	@Column(name = "sks_per_semester")
	public Integer getSksPerSemester() {
		return sksPerSemester == null ? 20 : sksPerSemester;
	}

	/**
	 * Menyetel batas SKS per semester.
	 *
	 * @param sksPerSemester batas baru; {@code null} berarti kembali ke default 20 pada getter
	 */
	public void setSksPerSemester(Integer sksPerSemester) {
		this.sksPerSemester = sksPerSemester;
	}

	/**
	 * Mengembalikan tanggal prodi mulai beroperasi (tipe {@code DATE}, tanpa jam).
	 *
	 * <p>Tidak ada anotasi {@code @Column}, sehingga nama kolomnya mengikuti default
	 * {@code MyNamingStrategy} (turunan {@code DefaultNamingStrategy}) — yaitu nama properti apa
	 * adanya, {@code tglMulaiOperasional}, bukan {@code tgl_mulai_operasional}.</p>
	 *
	 * @return tanggal mulai operasional, atau {@code null} bila belum diisi
	 */
	@Temporal(TemporalType.DATE)
	public Date getTglMulaiOperasional() {
		return tglMulaiOperasional;
	}

	/**
	 * Menyetel tanggal prodi mulai beroperasi.
	 *
	 * @param tglMulaiOperasional tanggal baru
	 */
	public void setTglMulaiOperasional(Date tglMulaiOperasional) {
		this.tglMulaiOperasional = tglMulaiOperasional;
	}

	/**
	 * Mengembalikan alamat situs web program studi. Tanpa anotasi {@code @Column}, jadi nama
	 * kolomnya persis {@code homepagePS} (lihat catatan penamaan pada Javadoc kelas).
	 *
	 * @return URL situs prodi, atau {@code null} bila belum diisi
	 */
	public String getHomepagePS() {
		return homepagePS;
	}

	/**
	 * Menyetel alamat situs web program studi. Tidak ada validasi bentuk URL.
	 *
	 * @param homepagePS URL baru
	 */
	public void setHomepagePS(String homepagePS) {
		this.homepagePS = homepagePS;
	}

	/**
	 * Mengembalikan nama pejabat penanda tangan SK pendirian prodi. Tanpa anotasi
	 * {@code @Column} — kolomnya bernama {@code pejabatSkBerdiri}.
	 *
	 * @return nama pejabat, atau {@code null} bila belum diisi
	 */
	public String getPejabatSkBerdiri() {
		return pejabatSkBerdiri;
	}

	/**
	 * Menyetel nama pejabat penanda tangan SK pendirian prodi.
	 *
	 * @param pejabatSkBerdiri nama baru
	 */
	public void setPejabatSkBerdiri(String pejabatSkBerdiri) {
		this.pejabatSkBerdiri = pejabatSkBerdiri;
	}

	/**
	 * Mengembalikan jumlah SKS mata kuliah <b>wajib</b> yang harus ditempuh untuk lulus, dengan
	 * <b>default 0</b> bila kolomnya kosong. Tanpa anotasi {@code @Column} — kolomnya bernama
	 * {@code sksWajibLulus}.
	 *
	 * <p>Seperti {@link #getSksPerSemester()}, default tidak ditulis balik ke field, tetapi
	 * karena pemetaan memakai akses property nilainya tetap ikut tersimpan sebagai 0 pada
	 * penyimpanan berikutnya.</p>
	 *
	 * @return SKS wajib; 0 bila belum diatur
	 */
	public Integer getSksWajibLulus() {
		return sksWajibLulus == null ? 0 : sksWajibLulus;
	}

	/**
	 * Menyetel jumlah SKS mata kuliah wajib syarat lulus.
	 *
	 * @param sksWajibLulus jumlah SKS; {@code null} berarti getter mengembalikan 0
	 */
	public void setSksWajibLulus(Integer sksWajibLulus) {
		this.sksWajibLulus = sksWajibLulus;
	}

	/**
	 * Mengembalikan jumlah SKS mata kuliah <b>pilihan</b> yang harus ditempuh untuk lulus,
	 * dengan <b>default 0</b> bila kolomnya kosong. Tanpa anotasi {@code @Column} — kolomnya
	 * bernama {@code sksPilihanLulus}.
	 *
	 * <p>Tidak ada pemeriksaan konsistensi bahwa
	 * {@link #getSksWajibLulus()} + {@link #getSksPilihanLulus()} sama dengan
	 * {@link #getSksLulus()}; ketiganya diisi terpisah oleh pengguna.</p>
	 *
	 * @return SKS pilihan; 0 bila belum diatur
	 */
	public Integer getSksPilihanLulus() {
		return sksPilihanLulus == null ? 0 : sksPilihanLulus;
	}

	/**
	 * Menyetel jumlah SKS mata kuliah pilihan syarat lulus.
	 *
	 * @param sksPilihanLulus jumlah SKS; {@code null} berarti getter mengembalikan 0
	 */
	public void setSksPilihanLulus(Integer sksPilihanLulus) {
		this.sksPilihanLulus = sksPilihanLulus;
	}

}
