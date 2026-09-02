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

/**
 * Master <b>paket perkuliahan</b>: aturan yang memetakan sekelompok mahasiswa ke sebuah
 * {@link Kurikulum}, sehingga KRS mereka dapat diisi sekaligus ("KRS paket") alih-alih memilih
 * matakuliah satu per satu.
 *
 * <p>Satu baris tabel {@code paket_perkuliahan} kira-kira menyatakan kalimat berikut:
 * <i>"mahasiswa angkatan {@code mulai}&hellip;{@code sampai}, yang sedang berada di semester
 * {@code minsmt}&hellip;{@code maxsmt}, pada tahun akademik {@code tahunAkademik}, mengambil
 * matakuliah sesuai {@link Kurikulum} ini"</i>. Prodi/fakultas/program <b>tidak</b> disimpan di
 * entity ini &mdash; semuanya diturunkan dari {@link #getKurikulum()} (lihat
 * {@link Kurikulum#getJurusan()} dan {@link Kurikulum#getProgram()}).</p>
 *
 * <h2>Posisi dalam alur perkuliahan</h2>
 *
 * <ol>
 *   <li><b>Penyusunan</b> &mdash; petugas akademik membuat/mengubah baris paket lewat layar
 *       {@code ais.action.master.PaketPerkuliahanAction} (tabel + jendela "Tambah/Ubah Paket
 *       Perkuliahan"). Layar itu hanya mengisi enam kolom: kurikulum, tahun akademik, angkatan
 *       mulai/sampai, semester minimal/maksimal, dan keterangan.</li>
 *   <li><b>Pencocokan</b> &mdash; saat seorang mahasiswa (atau petugas atas nama mahasiswa)
 *       hendak mengambil KRS paket, kode <b>mencari satu</b> baris paket yang cocok dengan data
 *       mahasiswa. Kriterianya diduplikasi di beberapa tempat; lihat bagian berikutnya.</li>
 *   <li><b>Pengambilan</b> &mdash; setelah paket ketemu, seluruh {@link KurikulumPunyaMatakuliah}
 *       pada {@code paket.kurikulum} untuk semester tersebut dibaca, dicarikan
 *       {@link Perkuliahan} (kelas/jadwal) yang sesuai, lalu dibuatkan baris
 *       {@link Detailperkuliahan} (baris KRS) untuk mahasiswa.</li>
 *   <li><b>Penelusuran</b> &mdash; baris KRS hasil paket menyimpan penunjuk balik lewat
 *       {@link Detailperkuliahan#getPaketPerkuliahan()}. Penunjuk itulah yang dipakai
 *       {@code DetailPaketPerkuliahanHelper} untuk menampilkan "daftar mahasiswa yang mengikuti
 *       paket perkuliahan X" &mdash; jadi <b>keanggotaan paket tidak disimpan sebagai relasi
 *       tersendiri</b>, melainkan disimpulkan dari baris-baris KRS yang pernah dibuat lewat paket
 *       itu.</li>
 * </ol>
 *
 * <h2>Kriteria pencocokan paket &mdash; disalin di tiga tempat</h2>
 *
 * <p>Kriteria yang sama (nyaris kata per kata) muncul di tiga kelas berbeda, dan ketiganya harus
 * ikut disunting bila aturan pencocokan diubah:</p>
 * <ul>
 *   <li>{@code ais.action.master.helper.AmbilDataPaketPerkuliahanHelper.cariPaket(...)}
 *       &mdash; jalur "Ambil KRS Paket" pada UI lama;</li>
 *   <li>{@code ais.action.master.helper.AmbilDataKurikulumPerkuliahanHelper.onSearchDefault(...)}
 *       &mdash; jalur pemilihan kurikulum/matakuliah paket;</li>
 *   <li>{@code ais.common.newui.akademik.NewUiKrsPaketController.cariPaket(...)}
 *       &mdash; jalur API tampilan baru (komentar di sana menyatakan terang-terangan bahwa
 *       kriterianya "disalin dari layar pemilih paket").</li>
 * </ul>
 *
 * <p>Bentuk kriterianya:</p>
 * <pre>
 *   &lt;semester&gt;            between minsmt and maxsmt      (SQL mentah)
 *   &lt;tahunangkatan mhs&gt;    between mulai  and sampai       (SQL mentah)
 *   statusSemesterPendek is null   (reguler)   ATAU  = &lt;kode semester pendek&gt;
 *   tahunAkademik = &lt;tahun akademik berjalan/terpilih&gt;
 *   order by angkatanMulai desc, angkatanSampai desc, id desc
 *   setMaxResults(1)
 * </pre>
 *
 * <p>Dua konsekuensi yang perlu diingat:</p>
 * <ul>
 *   <li>Bila beberapa paket cocok sekaligus, <b>yang menang adalah paket dengan angkatan mulai
 *       terbesar</b>, lalu angkatan sampai terbesar, lalu id terbesar (paling baru). Paket
 *       "khusus angkatan tertentu" karenanya otomatis mengalahkan paket "Semua angkatan"
 *       &mdash; ini perilaku yang dikehendaki, bukan kebetulan.</li>
 *   <li>Dua ketentuan pertama memakai <b>SQL mentah</b> ({@code Restrictions.sqlRestriction}) atas
 *       nama kolom fisik ({@code minsmt}, {@code maxsmt}, {@code mulai}, {@code sampai}), bukan
 *       nama properti Java. Nilai default yang diisi getter di kelas ini (lihat "Kuirk" di bawah)
 *       <b>tidak berlaku</b> di sana: {@code BETWEEN} dengan batas {@code NULL} bernilai
 *       {@code NULL}, sehingga baris dengan kolom kosong tidak pernah terpilih.</li>
 * </ul>
 *
 * <h2>Pemetaan kolom</h2>
 *
 * <p>Anotasi {@code @Id} berada pada getter, jadi Hibernate memakai <b>akses properti</b>: setiap
 * getter publik ikut dipetakan meski tanpa {@code @Column}, dengan nama kolom default sesuai nama
 * properti. Ringkasnya:</p>
 * <ul>
 *   <li>{@link #getId()} &rarr; {@code id} (identity, {@code insertable = false});</li>
 *   <li>{@link #getNama()} &rarr; {@code nama} ({@code not null}) &mdash; <b>dihitung</b>, lihat
 *       peringatan di getter-nya;</li>
 *   <li>{@link #getKeterangan()} &rarr; {@code keterangan};</li>
 *   <li>{@link #getKurikulum()} &rarr; FK {@code kurikulum} ({@code not null}, lazy);</li>
 *   <li>{@link #getAngkatanMulai()} &rarr; <b>{@code mulai}</b>,
 *       {@link #getAngkatanSampai()} &rarr; <b>{@code sampai}</b> (nama kolom sengaja dipendekkan,
 *       inilah kolom yang dipakai SQL mentah di atas);</li>
 *   <li>{@link #getMinSmt()} &rarr; {@code minsmt}, {@link #getMaxSmt()} &rarr; {@code maxsmt};</li>
 *   <li>{@link #getTahunAkademik()} &rarr; {@code tahunAkademik};</li>
 *   <li>{@link #getStatusSemesterPendek()} &rarr; {@code statusSemesterPendek};</li>
 *   <li>{@link #getOleh()}/{@link #getOlehId()}/{@link #getTanggal_dirubah()} &rarr; metadata
 *       audit generik.</li>
 * </ul>
 *
 * <p>Entity ini {@code @Audited} (Hibernate Envers): setiap perubahan menghasilkan revisi pada
 * tabel bayangan, dan tombol riwayat pada layarnya dibangun lewat
 * {@code RevisiHelper.createNewRevisi(PaketPerkuliahan.class, ...)}. Karena itu getter yang
 * menulis balik ke field (lihat di bawah) juga berpotensi menambah revisi audit tanpa ada
 * penyuntingan oleh manusia.</p>
 *
 * <h2>Field audit yang dideklarasikan ulang</h2>
 *
 * <p>{@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} dideklarasikan ulang di
 * kelas ini walaupun {@link GeneralValueObject} juga memilikinya. Itu <b>bukan duplikasi yang
 * keliru</b> melainkan keharusan teknis: {@link GeneralValueObject} adalah POJO abstrak biasa,
 * bukan {@code @Entity} maupun {@code @MappedSuperclass}, sehingga Hibernate <b>tidak</b>
 * memetakan properti milik induk. Tanpa deklarasi ulang, keempat kolom itu tidak akan ada di
 * pemetaan entity ini. Penjelasan lengkap ada di {@link GeneralValueObject}.</p>
 *
 * <h2>Kuirk dan jebakan</h2>
 *
 * <ol>
 *   <li><b>{@link #getNama()} adalah getter yang menulis</b> &mdash; nama paket tidak pernah
 *       diketik pengguna; ia dirakit ulang dari kurikulum + tahun akademik pada <b>setiap</b>
 *       pembacaan, lalu ditimpakan ke field {@code nama} (dan karenanya ke kolom {@code nama} saat
 *       flush). Lihat javadoc getter tersebut untuk daftar akibatnya, termasuk risiko
 *       {@code NullPointerException}.</li>
 *   <li><b>Nilai default yang ditulis balik</b> &mdash; {@link #getTahunAkademik()},
 *       {@link #getAngkatanMulai()}, {@link #getAngkatanSampai()}, dan {@link #getMaxSmt()}
 *       mengisi field bila masih {@code null}. Pada entity terkelola, sekadar <i>membaca</i>
 *       paket bisa mengubah isinya di basis data pada flush berikutnya.
 *       {@link #getMinSmt()} <b>tidak</b> ikut menulis balik &mdash; asimetri yang mudah
 *       terlewat.</li>
 *   <li><b>{@link #getStatusSemesterPendek()} praktis mati</b> &mdash;
 *       {@link #setStatusSemesterPendek(Integer)} tidak pernah dipanggil di manapun dalam basis
 *       kode (layar CRUD-nya pun tidak menyediakan kolom itu), padahal ketiga jalur pencocokan
 *       menyaring dengan kolom ini. Akibat praktisnya: kolomnya selalu {@code NULL}, sehingga
 *       hanya cabang "reguler" ({@code is null}) yang bisa menemukan paket; pencarian paket untuk
 *       <b>semester pendek</b> ({@code = kode}) selalu berakhir "Paket perkuliahan tidak
 *       ditemukan". Jangan disimpulkan sebagai kolom sisa yang aman dihapus &mdash; ia dibaca,
 *       hanya tidak pernah ditulis.</li>
 *   <li><b>{@link #toString()} membaca field mentah</b>, bukan {@link #getNama()}. Pada objek baru
 *       yang belum pernah disimpan/dibaca namanya, hasilnya {@code null}.</li>
 *   <li><b>{@code minSmt}/{@code maxSmt} tidak divalidasi saat simpan</b> &mdash;
 *       {@code PaketPerkuliahanAction.onSave} mewajibkan kurikulum, tahun akademik, dan kedua
 *       angkatan, tetapi membiarkan kedua {@code Intbox} semester kosong. Kolom {@code minsmt}/
 *       {@code maxsmt} lalu tersimpan {@code NULL} dan paket itu <b>tidak akan pernah ditemukan</b>
 *       oleh {@code BETWEEN} di atas, meski di layar tampak wajar (getter Java menampilkan 0/30).</li>
 * </ol>
 *
 * @see GeneralValueObject
 * @see Kurikulum
 * @see KurikulumPunyaMatakuliah
 * @see Detailperkuliahan#getPaketPerkuliahan()
 * @see ais.action.master.PaketPerkuliahanAction
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "paket_perkuliahan")
public class PaketPerkuliahan extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya dibekukan sejak entity dibuat; jangan diubah agar objek yang
	 * pernah diserialisasi (mis. state sesi ZK) tetap dapat dibaca kembali.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/**
	 * Kunci utama tabel {@code paket_perkuliahan}, diisi basis data ({@code identity}).
	 * Dideklarasikan ulang dari {@link GeneralValueObject} karena induknya tidak dipetakan
	 * Hibernate.
	 */
	private Long id;

	/**
	 * Nama pengguna terakhir yang mengubah baris ini (metadata audit generik). Diisi otomatis oleh
	 * {@code AuditTimestampInterceptor}, bukan oleh layar.
	 */
	private String oleh;

	/**
	 * Identitas teknis pengubah terakhir (id pengguna + asal pemanggilan + IP), diisi otomatis oleh
	 * {@code AuditTimestampInterceptor}.
	 */
	private String olehId;

	/**
	 * Mengembalikan identitas teknis pengubah terakhir baris ini.
	 *
	 * @return isi kolom {@code olehId}, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi identitas teknis pengubah terakhir.
	 *
	 * <p><b>Perhatikan:</b> argumen {@code null} atau berisi spasi saja <b>diabaikan diam-diam</b>
	 * &mdash; nilai lama dipertahankan. Ini disengaja agar jejak audit tidak terhapus oleh
	 * pemanggil yang meneruskan nilai kosong.</p>
	 *
	 * @param olehId identitas teknis pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong <b>diabaikan
	 * diam-diam</b> supaya jejak audit yang sudah ada tidak tertimpa.</p>
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
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
	 * @return isi kolom {@code oleh}, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: menyegarkan metadata audit tepat sebelum baris diperbarui.
	 *
	 * <p>Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)}, yang mengisi
	 * {@link #setTanggal_dirubah(Date)}, {@link #setOleh(String)}, dan
	 * {@link #setOlehId(String)} dari konteks pengguna yang sedang aktif. Interceptor tersebut
	 * melewati pengisian bila ia menilai tidak ada perubahan data bisnis, sehingga pembaruan
	 * "kosong" tidak menghasilkan revisi audit palsu.</p>
	 *
	 * <p>Dipanggil oleh penyedia JPA/Hibernate, bukan oleh kode aplikasi.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir baris ini. Diberi nilai awal waktu pembuatan objek supaya baris baru
	 * tetap punya stempel walau belum pernah melewati {@link #onUpdate()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi waktu perubahan terakhir baris ini.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (kolom {@code timestamp}).
	 *
	 * @return stempel waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks paket, dipakai antara lain sebagai label pada komponen ZK.
	 *
	 * <p><b>Perhatikan:</b> method ini membaca <b>field</b> {@code nama} apa adanya dan
	 * <b>tidak</b> memanggil {@link #getNama()}. Akibatnya nama tidak dirakit ulang di sini:
	 * objek yang baru dibuat ({@code new PaketPerkuliahan()}) dan belum pernah dibaca namanya akan
	 * menghasilkan {@code null}, sedangkan objek hasil pembacaan basis data menghasilkan nama
	 * tersimpan (yang bisa saja sudah usang bila kurikulum/tahun akademiknya berubah).</p>
	 *
	 * @return isi field {@code nama} apa adanya, mungkin {@code null}
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Nilai sentinel untuk angkatan mulai yang berarti <b>"Semua angkatan"</b>.
	 *
	 * <p>Dipakai dua arah: layar {@code PaketPerkuliahanAction} memasang pilihan combobox
	 * berlabel "Semua" dengan nilai ini, dan renderer barisnya menampilkan kembali teks "Semua"
	 * bila {@link #getAngkatanMulai()} sama dengan nilai ini. Karena batas bawahnya 0, kondisi
	 * {@code tahunangkatan between mulai and sampai} otomatis mencakup semua angkatan.</p>
	 *
	 * <p><b>Catatan:</b> field ini {@code public static} tanpa {@code final}, jadi secara teknis
	 * bisa diubah dari mana saja saat aplikasi berjalan &mdash; perlakukan sebagai konstanta.</p>
	 */
	public static Integer DEFAULT_ANGKATAN_MULAI = 0;

	/**
	 * Nilai sentinel untuk angkatan sampai yang berarti <b>"Semua angkatan"</b>, pasangan dari
	 * {@link #DEFAULT_ANGKATAN_MULAI}. Angka 2200 dipilih sekadar sebagai tahun yang mustahil
	 * tercapai sehingga rentang {@code 0..2200} mencakup seluruh angkatan.
	 *
	 * <p>Sama seperti pasangannya, field ini {@code public static} tanpa {@code final}.</p>
	 */
	public static Integer DEFAULT_ANGKATAN_SAMPAI = 2200;

	/**
	 * Nama paket. <b>Tidak pernah diketik pengguna</b> &mdash; dirakit ulang oleh
	 * {@link #getNama()} dari kurikulum + tahun akademik setiap kali dibaca.
	 */
	private String nama;

	/** Catatan bebas milik petugas akademik; satu-satunya kolom teks yang benar-benar diisi manual. */
	private String keterangan;

	/**
	 * Kurikulum yang isinya dipakai paket ini. Sumber tidak langsung untuk prodi, fakultas, dan
	 * program &mdash; entity ini tidak menyimpan ketiganya sendiri.
	 */
	private Kurikulum kurikulum;

	/** Tahun akademik berlakunya paket; ikut jadi penyaring wajib saat pencocokan paket. */
	private String tahunAkademik;

	/**
	 * Penanda semester pendek. {@code null} berarti paket semester reguler.
	 *
	 * <p>Dibaca oleh ketiga jalur pencocokan paket, tetapi <b>tidak pernah ditulis</b> oleh kode
	 * manapun &mdash; lihat bagian "Kuirk" pada javadoc kelas.</p>
	 */
	private Integer statusSemesterPendek;

	/** Batas bawah tahun angkatan mahasiswa yang tercakup paket ini (kolom {@code mulai}). */
	private Integer angkatanMulai;

	/** Batas atas tahun angkatan mahasiswa yang tercakup paket ini (kolom {@code sampai}). */
	private Integer angkatanSampai;

	/** Semester terendah mahasiswa yang boleh mengambil paket ini (kolom {@code minsmt}). */
	private Integer minSmt;

	/** Semester tertinggi mahasiswa yang boleh mengambil paket ini (kolom {@code maxsmt}). */
	private Integer maxSmt;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA, sekaligus dipakai layar
	 * {@code PaketPerkuliahanAction.onAdd} untuk menyiapkan form "Tambah Paket Perkuliahan".
	 *
	 * <p>Semua field dibiarkan {@code null}; nilai default baru muncul saat getter yang
	 * bersangkutan dibaca (lihat {@link #getTahunAkademik()}, {@link #getAngkatanMulai()},
	 * {@link #getAngkatanSampai()}, {@link #getMaxSmt()}).</p>
	 */
	public PaketPerkuliahan() {
	}

	/**
	 * Mengembalikan kunci utama baris paket ini.
	 *
	 * <p>Dipakai luas sebagai penanda "baris baru vs baris tersimpan": {@code null} berarti paket
	 * belum pernah disimpan (judul jendela jadi "Tambah&hellip;", kombo kurikulum masih boleh
	 * diubah, dan {@code onSave} memilih {@code save()} alih-alih {@code update()}).</p>
	 *
	 * @return id baris, atau {@code null} bila objek belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama baris paket ini. Normalnya hanya dipanggil Hibernate.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama paket &mdash; <b>dirakit ulang setiap kali dipanggil</b>, bukan sekadar
	 * dibaca.
	 *
	 * <p><b>Cara kerja.</b> Method ini memanggil {@link #getKurikulum()} (yang meresolusi proxy
	 * lazy dan menugaskan hasilnya kembali ke field {@code kurikulum}). Bila kurikulum ada, field
	 * {@code nama} <b>ditimpa</b> dengan pola:</p>
	 * <pre>
	 *   "Paket kurikulum " + kurikulum.jurusan.nama + " tahun " + kurikulum.tahun
	 *                      + " tahun akademik " + tahunAkademik
	 * </pre>
	 * <p>Nilai yang dikembalikan adalah field tersebut setelah {@code trim()}; bila kurikulum
	 * {@code null}, nama lama dikembalikan apa adanya (bisa {@code null}).</p>
	 *
	 * <p><b>Efek samping yang perlu disadari:</b></p>
	 * <ul>
	 *   <li>Ini adalah getter yang dipetakan Hibernate ke kolom {@code nama} ({@code not null}).
	 *       Saat flush, nilai yang <i>baru dirakit</i>-lah yang tersimpan. Kolom {@code nama}
	 *       karenanya ikut berubah sendiri bila nama jurusan, tahun kurikulum, atau tahun akademik
	 *       paket berubah &mdash; tanpa ada yang menyunting paket. Karena entity ini
	 *       {@code @Audited}, perubahan diam-diam itu juga bisa memunculkan revisi baru.</li>
	 *   <li>Layar CRUD <b>tidak menyediakan</b> kolom isian nama; {@link #setNama(String)}
	 *       praktis tidak pernah dipanggil kode aplikasi. Jadi pencarian di layar daftar
	 *       ({@code ilike} pada kolom {@code nama}) sesungguhnya mencari string hasil rakitan
	 *       ini &mdash; termasuk potongan kata "Paket kurikulum &hellip; tahun &hellip;".</li>
	 *   <li>Yang dipakai adalah <b>field</b> {@code tahunAkademik}, bukan
	 *       {@link #getTahunAkademik()}. Bila field masih {@code null}, nama yang terbentuk
	 *       memuat teks harfiah {@code "null"} alih-alih tahun akademik berjalan.</li>
	 *   <li>{@code kurikulum.getJurusan()} tidak diperiksa null: kurikulum tanpa jurusan membuat
	 *       method ini melempar {@code NullPointerException}, dan karena Hibernate memanggilnya
	 *       saat flush, kegagalan bisa muncul jauh dari kode yang menyebabkannya.</li>
	 * </ul>
	 *
	 * @return nama paket hasil rakitan (sudah di-{@code trim}), atau {@code null} bila kurikulum
	 *         belum ada dan nama belum pernah terisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		kurikulum = getKurikulum();
		if (kurikulum != null) {
			nama = "Paket kurikulum " + kurikulum.getJurusan().getNama() + " tahun " + kurikulum.getTahun()
					+ " tahun akademik " + tahunAkademik;
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama paket secara manual.
	 *
	 * <p>Praktis tidak dipakai: nilai apa pun yang diisi di sini akan <b>ditimpa</b> pada
	 * pemanggilan {@link #getNama()} berikutnya selama {@link #getKurikulum()} tidak
	 * {@code null}. Disediakan terutama agar Hibernate dapat mengisi field saat memuat baris dari
	 * basis data.</p>
	 *
	 * @param nama nama paket
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan catatan bebas milik paket ini (kolom {@code keterangan}, boleh kosong).
	 *
	 * @return keterangan paket, atau {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi catatan bebas paket ini; berasal dari kolom "Keterangan" pada jendela tambah/ubah.
	 *
	 * @param keterangan catatan bebas
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan kurikulum yang isinya dipakai paket ini (FK {@code kurikulum}, wajib).
	 *
	 * <p>Relasi ini {@code LAZY}, sehingga getter memanggil
	 * {@link GeneralValueObject#check(Object)} lebih dulu dan <b>menugaskan hasilnya kembali ke
	 * field</b> &mdash; pola resolusi proxy standar seluruh entity di paket ini. Efeknya: proxy
	 * yang tadinya belum terinisialisasi (atau sudah <i>detached</i>) diusahakan menjadi objek
	 * nyata, bila perlu dengan membaca ulang dari cache/session/basis data. Lihat javadoc
	 * {@link GeneralValueObject} untuk rincian ketiga tahap resolusi dan biayanya.</p>
	 *
	 * <p>Dari kurikulum inilah diturunkan prodi ({@link Kurikulum#getJurusan()}), fakultas, dan
	 * program &mdash; ketiganya dipakai sebagai penyaring pada layar pencarian paket dan pada
	 * jendela "Generate KRS Paket Mahasiswa Secara Otomatis".</p>
	 *
	 * @return kurikulum paket ini; {@code null} hanya pada objek yang belum lengkap
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kurikulum", nullable = false)
	public Kurikulum getKurikulum() {
		kurikulum = check(kurikulum);
		return kurikulum;
	}

	/**
	 * Mengisi kurikulum paket ini.
	 *
	 * <p>Pada layar CRUD, kombo kurikulum <b>dikunci setelah paket tersimpan</b>
	 * ({@code kurikulum.setDisabled(paketPerkuliahan.getId() != null)}), jadi dalam praktiknya
	 * nilai ini hanya ditetapkan sekali saat pembuatan paket. Mengubahnya lewat kode akan
	 * mengubah pula {@link #getNama()} pada pembacaan berikutnya.</p>
	 *
	 * @param kurikulum kurikulum sumber matakuliah paket
	 */
	public void setKurikulum(Kurikulum kurikulum) {
		this.kurikulum = kurikulum;
	}

	/**
	 * Mengembalikan tahun akademik berlakunya paket, <b>mengisi nilai default bila masih
	 * kosong</b>.
	 *
	 * <p>Bila field {@code tahunAkademik} {@code null}, method ini mengisinya dengan
	 * {@code Common.getCurrentTahunAkademik()} (tahun akademik berjalan menurut konteks sesi) dan
	 * menyimpannya ke field. Pada entity terkelola, pembacaan saja karenanya dapat membuat kolom
	 * {@code tahunAkademik} ikut tersimpan pada flush berikutnya.</p>
	 *
	 * <p>Nilai ini adalah penyaring <b>wajib</b> ({@code Restrictions.eq}) pada ketiga jalur
	 * pencocokan paket: paket tahun akademik lain tidak akan pernah terpilih.</p>
	 *
	 * @return tahun akademik paket; tidak pernah {@code null} setelah pemanggilan ini, sepanjang
	 *         konteks sesi dapat menentukan tahun akademik berjalan
	 */
	public String getTahunAkademik() {
		if (tahunAkademik == null) {
			tahunAkademik = Common.getCurrentTahunAkademik();
		}
		return tahunAkademik;
	}

	/**
	 * Mengisi tahun akademik berlakunya paket; diambil dari kombo "Tahun Akademik" pada jendela
	 * tambah/ubah (kolom wajib di layar).
	 *
	 * @param tahunAkademik tahun akademik, mis. {@code "2025/2026"}
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan penanda semester pendek paket ini; {@code null} berarti paket reguler.
	 *
	 * <p><b>Penting.</b> Kolom ini <b>dibaca</b> sebagai penyaring oleh seluruh jalur pencocokan
	 * paket ({@code isNull} untuk reguler, {@code eq} untuk semester pendek), namun
	 * {@link #setStatusSemesterPendek(Integer)} <b>tidak pernah dipanggil</b> di manapun dan layar
	 * CRUD paket tidak menyediakan isiannya. Kolomnya karena itu selalu {@code NULL}, dan
	 * pencarian paket untuk semester pendek selalu gagal menemukan baris. Rincian ada pada bagian
	 * "Kuirk" di javadoc kelas.</p>
	 *
	 * @return kode status semester pendek, atau {@code null} untuk paket reguler
	 */
	public Integer getStatusSemesterPendek() {
		return statusSemesterPendek;
	}

	/**
	 * Mengisi penanda semester pendek paket ini.
	 *
	 * <p>Tidak ada pemanggil di dalam basis kode saat ini (lihat {@link #getStatusSemesterPendek()});
	 * disediakan agar Hibernate dapat mengisi field saat memuat baris.</p>
	 *
	 * @param statusSemesterPendek kode status semester pendek, atau {@code null} untuk reguler
	 */
	public void setStatusSemesterPendek(Integer statusSemesterPendek) {
		this.statusSemesterPendek = statusSemesterPendek;
	}

	/**
	 * Mengembalikan batas bawah tahun angkatan yang tercakup paket ini (kolom {@code mulai}),
	 * <b>mengisi nilai default bila masih kosong</b>.
	 *
	 * <p>Bila field masih {@code null}, method mengisinya dengan {@link #DEFAULT_ANGKATAN_MULAI}
	 * (0 = "Semua angkatan") dan menyimpannya ke field &mdash; jadi pembacaan dapat mengubah isi
	 * baris pada flush berikutnya. Renderer daftar paket membandingkan hasil method ini dengan
	 * {@link #DEFAULT_ANGKATAN_MULAI} untuk menampilkan teks "Semua" alih-alih angka.</p>
	 *
	 * <p>Perhatikan bahwa penyaring sesungguhnya berjalan di sisi basis data
	 * ({@code <tahunangkatan> between mulai and sampai}), sehingga default yang diisi di sini
	 * tidak menolong baris yang kolomnya sudah terlanjur {@code NULL} di basis data.</p>
	 *
	 * @return tahun angkatan terendah yang tercakup; tidak pernah {@code null} setelah pemanggilan
	 */
	@Column(name = "mulai", nullable = true)
	public Integer getAngkatanMulai() {
		if (angkatanMulai == null) {
			angkatanMulai = DEFAULT_ANGKATAN_MULAI;
		}
		return angkatanMulai;
	}

	/**
	 * Mengisi batas bawah tahun angkatan yang tercakup paket ini.
	 *
	 * <p>Diambil dari kombo "Mulai Tahun Angkatan" yang berisi rentang 20 tahun ke belakang sampai
	 * 20 tahun ke depan, ditambah pilihan "Semua" bernilai {@link #DEFAULT_ANGKATAN_MULAI}.
	 * {@code onSave} menolak menyimpan bila kombo ini belum dipilih.</p>
	 *
	 * @param angkatanMulai tahun angkatan terendah, atau {@link #DEFAULT_ANGKATAN_MULAI} untuk
	 *                      "semua angkatan"
	 */
	public void setAngkatanMulai(Integer angkatanMulai) {
		this.angkatanMulai = angkatanMulai;
	}

	/**
	 * Mengembalikan batas atas tahun angkatan yang tercakup paket ini (kolom {@code sampai}),
	 * <b>mengisi nilai default bila masih kosong</b>.
	 *
	 * <p>Perilakunya cermin dari {@link #getAngkatanMulai()}: field {@code null} diisi
	 * {@link #DEFAULT_ANGKATAN_SAMPAI} (2200 = "Semua angkatan") dan ditulis balik ke field,
	 * dan renderer daftar menampilkan "Semua" bila nilainya sama dengan sentinel tersebut.</p>
	 *
	 * @return tahun angkatan tertinggi yang tercakup; tidak pernah {@code null} setelah pemanggilan
	 */
	@Column(name = "sampai", nullable = true)
	public Integer getAngkatanSampai() {
		if (angkatanSampai == null) {
			angkatanSampai = DEFAULT_ANGKATAN_SAMPAI;
		}
		return angkatanSampai;
	}

	/**
	 * Mengisi batas atas tahun angkatan yang tercakup paket ini.
	 *
	 * <p>Diambil dari kombo "Sampai Tahun Angkatan"; {@code onSave} menolak menyimpan bila kombo
	 * ini belum dipilih. Tidak ada pemeriksaan bahwa nilainya &ge;
	 * {@link #getAngkatanMulai()} &mdash; rentang terbalik akan tersimpan dan membuat paket tidak
	 * pernah cocok dengan siapa pun.</p>
	 *
	 * @param angkatanSampai tahun angkatan tertinggi, atau {@link #DEFAULT_ANGKATAN_SAMPAI} untuk
	 *                       "semua angkatan"
	 */
	public void setAngkatanSampai(Integer angkatanSampai) {
		this.angkatanSampai = angkatanSampai;
	}

	/**
	 * Mengembalikan semester terendah yang boleh mengambil paket ini (kolom {@code minsmt}).
	 *
	 * <p>Bila field {@code null}, method mengembalikan 0 <b>tanpa menulis balik ke field</b>
	 * &mdash; berbeda dari {@link #getMaxSmt()}, {@link #getAngkatanMulai()},
	 * {@link #getAngkatanSampai()}, dan {@link #getTahunAkademik()} yang semuanya menyimpan
	 * defaultnya. Kolom {@code minsmt} di basis data karenanya tetap {@code NULL}, dan penyaring
	 * SQL {@code <semester> between minsmt and maxsmt} tidak akan pernah cocok untuk baris
	 * tersebut.</p>
	 *
	 * <p>Selain di SQL, nilai ini juga dibandingkan di sisi Java oleh
	 * {@code AmbilDataMahasiswaForPaketPerkuliahanHelper}, yang menolak memasukkan mahasiswa ke
	 * paket bila semesternya di luar rentang {@code minSmt..maxSmt}.</p>
	 *
	 * @return semester terendah; 0 bila belum diisi
	 */
	public Integer getMinSmt() {
		return minSmt == null ? 0 : minSmt;
	}

	/**
	 * Mengisi semester terendah yang boleh mengambil paket ini.
	 *
	 * <p>Diambil dari {@code Intbox} "Minimal Semester". {@code onSave} <b>tidak</b> memvalidasi
	 * kolom ini, sehingga isian kosong tersimpan sebagai {@code NULL} &mdash; lihat peringatan di
	 * {@link #getMinSmt()}.</p>
	 *
	 * @param minSmt semester terendah, boleh {@code null}
	 */
	public void setMinSmt(Integer minSmt) {
		this.minSmt = minSmt;
	}

	/**
	 * Mengembalikan semester tertinggi yang boleh mengambil paket ini (kolom {@code maxsmt}),
	 * <b>mengisi nilai default 30 bila masih kosong</b>.
	 *
	 * <p>Berbeda dari {@link #getMinSmt()}, default di sini <b>ditulis balik</b> ke field,
	 * sehingga pembacaan pada entity terkelola dapat menyimpan angka 30 ke basis data pada flush
	 * berikutnya. Angka 30 adalah batas longgar "praktis tak terbatas" (30 semester &asymp; 15
	 * tahun studi).</p>
	 *
	 * @return semester tertinggi; tidak pernah {@code null} setelah pemanggilan
	 */
	public Integer getMaxSmt() {
		if (maxSmt == null) {
			maxSmt = 30;
		}
		return maxSmt;
	}

	/**
	 * Mengisi semester tertinggi yang boleh mengambil paket ini.
	 *
	 * <p>Diambil dari {@code Intbox} "Maksimal Semester" dan, seperti {@link #setMinSmt(Integer)},
	 * tidak divalidasi saat penyimpanan.</p>
	 *
	 * @param maxSmt semester tertinggi, boleh {@code null}
	 */
	public void setMaxSmt(Integer maxSmt) {
		this.maxSmt = maxSmt;
	}
}
