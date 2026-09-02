package ais.database.model;

// Generated Dec 16, 2009 2:17:42 AM by Hibernate Tools 3.2.4.CR1

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
 * Entity <b>master konversi nilai</b> (tabel {@code public.nilai_huruf}) &mdash; satu baris = satu
 * <b>aturan konversi</b> "rentang angka &rarr; huruf" beserta <b>indeks IPK</b>-nya. Inilah tabel
 * yang menjawab pertanyaan: nilai akhir {@code 81,5} itu huruf apa, dan huruf itu bernilai berapa
 * saat dipakai menghitung IP/IPK, serta apakah huruf itu dianggap <b>lulus</b>.
 *
 * <p><b>Class ini SANGAT SENTRAL.</b> Ia bukan master yang hanya dipakai satu layar: hampir seluruh
 * jalur penilaian akademik bergantung padanya &mdash; input nilai dosen, unggah nilai massal,
 * impor/ekspor Feeder (PDDikti), transkrip/KHS, dasbor OBE, sampai pewarnaan status lulus di layar
 * mahasiswa. Perlakukan setiap perubahan di sini sebagai perubahan berisiko tinggi.</p>
 *
 * <h2>Bentuk satu baris (satu aturan)</h2>
 *
 * <p>Inti aturan hanya tiga properti: {@link #getMulai() mulai} &ndash; {@link #getSampai() sampai}
 * (rentang nilai angka, <b>inklusif di kedua ujung</b>), {@link #getNilaiHuruf() nilaiHuruf} (huruf
 * hasilnya, mis. {@code A}/{@code B+}/{@code E}), dan {@link #getNilaiDiIPK() nilaiDiIPK} (bobot
 * angka huruf tersebut untuk perhitungan IP/IPK, mis. {@code 4.0}).</p>
 *
 * <p>Sisanya adalah <b>penyempit cakupan (scoping)</b>: aturan yang sama bisa berbeda antar
 * program studi, fakultas, angkatan, periode, jenis nilai, bahkan per matakuliah tertentu:</p>
 *
 * <ul>
 *   <li>{@link #getJurusan() jurusan} / {@link #getFakultas() fakultas} &mdash; berlaku hanya untuk
 *       prodi/fakultas itu; kosong = berlaku umum (global).</li>
 *   <li>{@link #getTahunAngkatan() tahunAngkatan} &mdash; berlaku untuk angkatan tersebut
 *       (pencocokan bertingkat: sama persis dulu, baru "angkatan &ge; nilai ini").</li>
 *   <li>{@link #getTahunAkademik() tahunAkademik} + {@link #getSemester() semester}, yang diringkas
 *       menjadi kunci numerik {@link #getTa() ta} &mdash; "berlaku mulai" periode tersebut.</li>
 *   <li>{@link #getKodeMk() kodeMk} &mdash; daftar kode matakuliah khusus (dipisah koma); kosong =
 *       berlaku untuk semua matakuliah.</li>
 *   <li>{@link #getJenisNilaiHuruf() jenisNilaiHuruf} &mdash; skema/tabel huruf alternatif
 *       ({@link JenisNilaiHurufMatakuliah}), mis. skema khusus matakuliah tertentu.</li>
 * </ul>
 *
 * <p>Dua properti terakhir bersifat kebijakan, bukan konversi: {@link #getLulus() lulus} (apakah
 * huruf ini dihitung lulus) dan {@link #getTampilkanStatusLulus() tampilkanStatusLulus} (apakah
 * status lulus/tidak lulus ditampilkan di layar).</p>
 *
 * <h2>Cara data ini dipakai: cache statis, bukan query per pemakaian</h2>
 *
 * <p>Tabel ini <b>tidak</b> di-query ulang setiap kali dibutuhkan. Seluruh isinya dimuat ke cache
 * statis {@code ais.common.ConstantValues.nilaiHurufs} oleh
 * {@code ConstantValues.realoadNilaiHuruf(Session)}, terurut menurun berdasarkan
 * {@code kodeMk}, {@code tahunAngkatan}, {@code ta}, lalu {@code mulai} &mdash; urutan itu
 * <b>bermakna</b>: pencocokan selalu mengambil kandidat pertama yang cocok, sehingga aturan yang
 * lebih spesifik/lebih baru menang. Cache dimuat ulang setiap kali baris disimpan lewat layar
 * master ({@code ais.action.master.NilaiHurufAction.onSave}, dua kali: langsung dan lewat timer
 * susulan). <b>Konsekuensinya:</b> mengubah baris langsung di database tanpa reload cache tidak
 * akan berpengaruh sampai aplikasi memuat ulang.</p>
 *
 * <p>Ada dua jalur pencarian yang berbeda dan keduanya banyak dipakai:</p>
 *
 * <ol>
 *   <li><b>Angka &rarr; huruf</b>: {@code Common.getNilaiHuruf(nilai, tahunAngkatan, jurusan,
 *       fakultas, tahunAkademik, semester, kodeMk, ...)} yang diteruskan ke
 *       {@code CommonAcademicKrsNilaiHelper}. Implementasinya berupa <b>tangga fallback</b>
 *       berlapis: (jurusan+fakultas, angkatan sama persis) &rarr; (jurusan+fakultas, angkatan
 *       &ge;) &rarr; (fakultas saja, angkatan sama) &rarr; (fakultas saja, angkatan &ge;) &rarr;
 *       (global, angkatan sama) &rarr; (global, angkatan &ge;) &rarr; (global, abaikan angkatan);
 *       bila tetap gagal dan {@code jenisNilaiHuruf} terisi, seluruh tangga diulang tanpa
 *       jenis nilai huruf. Setiap lapis juga menyaring {@code ta <= ta periode} dan kecocokan
 *       {@code kodeMk}. Varian {@code Common.getNilaiHurufBerdasarkanIP(...)} memakai tabel yang
 *       sama untuk memetakan IP/IPK menjadi predikat huruf.</li>
 *   <li><b>Huruf &rarr; konfigurasi</b>: {@code ConstantValues.nilaiHurufTerkait(huruf, mahasiswa)}
 *       memakai indeks in-memory {@code huruf UPPERCASE &rarr; {perJurusan, perFakultas, global}}
 *       sehingga lookup-nya O(1) tanpa query. Di atasnya berdiri dua helper yang dipanggil sangat
 *       sering: {@code ConstantValues.lulusDariNilaiHuruf(...)} dan
 *       {@code ConstantValues.tampilkanStatusLulusDariNilaiHuruf(...)}.</li>
 * </ol>
 *
 * <h2>Pola pemakaian umum di seluruh sistem</h2>
 *
 * <p>Sekitar 150 berkas menyebut tipe ini. Alih-alih mendaftar semuanya, berikut pola-polanya:</p>
 *
 * <ul>
 *   <li><b>Penilaian</b> &mdash; setelah nilai akhir dihitung, huruf dan {@code totalIP} diisi dari
 *       hasil pencarian di tabel ini ({@code DetailperkuliahanForPenilaianHelper},
 *       {@code PenilaianUtil}, {@code penilaian.UploadNilaiMahasiswa} dan varian format Epsbed,
 *       serta {@link Detailperkuliahan} sendiri).</li>
 *   <li><b>Arah balik (huruf &rarr; angka)</b> &mdash; bila yang tersedia hanya huruf (impor,
 *       "hanya input nilai huruf"), nilai angka direkonstruksi sebagai <b>titik tengah rentang</b>
 *       {@code (mulai + sampai) / 2.0}. Pola ini muncul identik di banyak berkas impor.</li>
 *   <li><b>Feeder/PDDikti</b> &mdash; {@link #getFeeder() feeder} menyimpan {@code kode_bobot_nilai}
 *       versi Dikti; ekspor mengirim {@code bobot_nilai_min} ({@code mulai}) dan
 *       {@code nilai_indeks} ({@code nilaiDiIPK}), impor mencocokkan baris lewat kolom
 *       {@code feeder}.</li>
 *   <li><b>Status lulus entity lain</b> &mdash; {@code getLulus()} pada {@link Detailperkuliahan},
 *       {@code MahasiswaDapatKelompokKkn}, {@code MahasiswaDapatKelompokPkl}, dan
 *       {@code MahasiswaRequestTugasAkhir} memakai
 *       {@code ConstantValues.lulusDariNilaiHuruf(...)} agar status lulus mengikuti centang di
 *       master ini, bukan tebakan string. Pewarnaannya di
 *       {@code ais.action.master.helper.util.WarnaStatusLulusUtil}.</li>
 *   <li><b>Laporan &amp; dasbor</b> &mdash; transkrip/KHS, {@code LaporanDaftarUjian} (mencetak
 *       legenda "A = 80 s.d 100"), rekap OBE, dan popup analisis
 *       {@code ais.ui.util.NilaiHurufAnalisisPopupHelper}.</li>
 *   <li><b>Sinkronisasi massal</b> &mdash; layar master menyediakan aksi menghitung ulang seluruh
 *       nilai huruf mahasiswa terhadap tabel konversi terkini ({@code Common.synNilaiHuruf}).</li>
 * </ul>
 *
 * <p><b>Jangan tertukar</b> dengan {@code ais.database.model.sekolah.NilaiHurufSekolah} (varian
 * untuk modul sekolah, master terpisah), {@link JenisNilaiHurufMatakuliah} (nama <i>skema</i>
 * huruf, bukan hurufnya), dan {@code NilaiHurufExport} (entity bantu ekspor Feeder).</p>
 *
 * <h2>Perilaku non-obvious yang WAJIB diketahui</h2>
 *
 * <p>Entity ini memakai <b>property access</b> (anotasi di getter) dengan
 * {@code dynamicInsert}/{@code dynamicUpdate} aktif. Beberapa getter <b>menulis balik ke field
 * terpetakan</b>, sehingga sekadar <i>membaca</i> instance yang masih <i>managed</i> dapat membuat
 * entity "kotor" dan memicu {@code UPDATE} saat flush &mdash; tanpa ada aksi simpan:</p>
 *
 * <ul>
 *   <li>{@link #getTahunAngkatan()} &mdash; {@code null} diubah menjadi {@code 0}.</li>
 *   <li>{@link #getTanggalMulaiBerlaku()} &mdash; {@code null} diisi tanggal hari ini.</li>
 *   <li>{@link #getTa()} &mdash; <b>selalu</b> dihitung ulang dan ditimpa dari
 *       {@code tahunAkademik} + {@code semester}.</li>
 *   <li>{@link #getLulus()} &mdash; {@code null} diturunkan dari huruf lewat tebakan substring.</li>
 *   <li>{@link #getKodeMk()} &mdash; menormalkan dan menulis balik bentuk berkoma.</li>
 *   <li>{@link #getJurusan()}, {@link #getFakultas()}, {@link #getJenisNilaiHuruf()} &mdash;
 *       menulis balik hasil {@link GeneralValueObject#check(Object)} (resolusi proxy lazy).</li>
 *   <li>{@link #getFakultas()} bahkan <b>menimpa fakultas dari jurusan</b> bila jurusan terisi
 *       &mdash; fakultas yang tersimpan tidak konsisten dengan jurusan akan diperbaiki diam-diam.
 *       Lihat catatan pada method tersebut.</li>
 * </ul>
 *
 * <p><b>Getter yang menutup sesi Hibernate: TIDAK ADA.</b> Berkas ini tidak menyentuh
 * {@code Session}/{@code HibernateUtil}/{@code Criteria} sama sekali dan <b>tidak memiliki satu pun
 * method query statis</b>; seluruh pencarian dilakukan pihak lain ({@code ConstantValues},
 * {@code Common}, {@code CommonAcademicKrsNilaiHelper}) di atas cache statis. Satu-satunya jalur
 * akses database tidak langsung adalah {@link GeneralValueObject#check(Object)} yang, sebagai upaya
 * terakhir, dapat membuka dan menutup sesinya sendiri.</p>
 *
 * <h2>Hubungan dengan {@link GeneralValueObject}</h2>
 *
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} &mdash;
 * ia POJO abstrak biasa, sehingga Hibernate <b>tidak</b> memetakan properti induknya. Karena itu
 * {@code serialVersionUID}, {@link #getId() id}, {@link #getOleh() oleh},
 * {@link #getOlehId() olehId}, dan {@link #getTanggal_dirubah() tanggal_dirubah} <b>sengaja
 * dideklarasikan ulang</b> di kelas ini. Itu <b>keharusan teknis, bukan duplikasi yang bisa
 * dibersihkan</b>: menghapusnya membuat kolom-kolom tersebut hilang dari pemetaan.</p>
 *
 * <h2>Pengelompokan anggota</h2>
 *
 * <ol>
 *   <li><b>Jejak audit</b> (deklarasi ulang wajib): {@code serialVersionUID}, {@code oleh},
 *       {@code olehId}, {@code tanggal_dirubah}, kait {@link #onUpdate()}.</li>
 *   <li><b>Identitas &amp; representasi</b>: {@link #getId()}, {@link #toString()}.</li>
 *   <li><b>Inti aturan konversi</b>: {@code mulai}, {@code sampai}, {@code nilaiHuruf},
 *       {@code nilaiDiIPK}.</li>
 *   <li><b>Penyempit cakupan</b>: {@code tahunAngkatan}, {@code jurusan}, {@code fakultas},
 *       {@code tahunAkademik}, {@code semester}, {@code ta}, {@code kodeMk},
 *       {@code jenisNilaiHuruf}, {@code tanggalMulaiBerlaku}.</li>
 *   <li><b>Kebijakan kelulusan</b>: {@code lulus}, {@code tampilkanStatusLulus}.</li>
 *   <li><b>Integrasi &amp; catatan</b>: {@code feeder}, {@code keterangan}.</li>
 * </ol>
 *
 * <p>Seluruh perubahan diaudit Envers ({@code @Audited}) ke tabel bayangan
 * {@code new_audit.nilai_huruf__audit}; penambahan kolom di tabel utama <b>wajib</b> diikuti
 * penambahan kolom yang sama di tabel audit, jika tidak penyimpanan akan gagal (rollback).</p>
 *
 * @see GeneralValueObject
 * @see JenisNilaiHurufMatakuliah
 * @see Detailperkuliahan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "nilai_huruf")
public class NilaiHuruf extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya dibangkitkan sekali dan <b>tidak boleh diubah</b>: entity ini
	 * ikut terserialisasi (sesi ZK, cache) sehingga mengubahnya membuat data lama tidak terbaca.
	 */
	private static final long serialVersionUID = -8007233666610291708L;

	/** Kunci primer; lihat {@link #getId()}. Dideklarasikan ulang karena induk tidak dipetakan. */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris; lihat {@link #getOleh()}. */
	private String oleh;

	/** Identitas (login/NIP) pengguna terakhir yang mengubah baris; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Identitas pengguna terakhir yang mengubah baris konversi ini (jejak audit).
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan identitas pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatikan:</b> nilai {@code null} atau berisi spasi saja <b>diabaikan diam-diam</b>
	 * (method langsung {@code return}), sehingga jejak audit yang sudah ada tidak pernah terhapus
	 * oleh penyimpanan yang tidak mengisi kolom ini. Konsekuensinya kolom ini <b>tidak bisa
	 * dikosongkan kembali</b> lewat setter.</p>
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: nilai {@code null}/kosong <b>diabaikan</b> agar
	 * jejak audit tidak tertimpa nilai kosong.</p>
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna terakhir yang mengubah baris konversi ini (jejak audit).
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil Hibernate <b>tepat sebelum</b> {@code UPDATE} baris ini,
	 * meneruskan ke {@code AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #getOleh() oleh}/{@link #getOlehId() olehId}/{@link #getTanggal_dirubah()
	 * tanggal_dirubah} dari pengguna sesi berjalan.
	 *
	 * <p><b>Catatan bentuk kode:</b> baris ini menggabungkan deklarasi method kait <i>dan</i>
	 * deklarasi field {@code tanggal_dirubah} (diinisialisasi ke waktu sekarang) dalam satu baris
	 * fisik. Bentuk itu dipertahankan apa adanya di seluruh entity repo ini &mdash; jangan dipecah
	 * tanpa alasan kuat.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyimpan waktu perubahan terakhir. Umumnya diisi otomatis oleh {@link #onUpdate()}; isi
	 * manual hanya untuk migrasi/impor.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Waktu perubahan terakhir baris konversi ini.
	 *
	 * <p>Tanpa {@code @Column} eksplisit sehingga jatuh ke penamaan default
	 * {@code ais.database.hibernate.MyNamingStrategy} (turunan {@code DefaultNamingStrategy}: nama
	 * kolom = nama properti apa adanya) &mdash; kolom {@code tanggal_dirubah}. Nilai awalnya adalah
	 * waktu pembuatan object, bukan {@code null}.</p>
	 *
	 * @return waktu perubahan terakhir (tidak pernah {@code null} pada instance baru)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas aturan konversi untuk log dan penelusuran, berbentuk
	 * {@code mulai_sampai_tahunAngkatan_nilaiHuruf_nilaiDiIPK} (mis.
	 * {@code 80.0_100.0_2020_A_4.0}).
	 *
	 * <p><b>Penting:</b> method ini membaca <b>field langsung</b>, bukan getter, sehingga
	 * <b>bebas efek samping</b> &mdash; ia tidak memicu resolusi proxy lazy maupun penulisan balik
	 * seperti {@link #getTa()} atau {@link #getKodeMk()}. Karena itu aman dipanggil dari
	 * {@code System.out.println} di jalur impor Feeder. Nilai {@code null} akan tercetak sebagai
	 * teks {@code "null"}.</p>
	 *
	 * @return ringkasan aturan konversi sebagai teks
	 */
	public String toString() {
		return mulai + "_" + sampai + "_" + tahunAngkatan + "_" + nilaiHuruf + "_" + nilaiDiIPK;
	}

	/** Batas bawah rentang nilai angka (inklusif); lihat {@link #getMulai()}. */
	private Double mulai;

	/** Batas atas rentang nilai angka (inklusif); lihat {@link #getSampai()}. */
	private Double sampai;

	/** Angkatan yang dicakup aturan ini; lihat {@link #getTahunAngkatan()}. */
	private Integer tahunAngkatan;

	/** Huruf hasil konversi; lihat {@link #getNilaiHuruf()}. */
	private String nilaiHuruf;

	/** Bobot huruf untuk perhitungan IP/IPK; lihat {@link #getNilaiDiIPK()}. */
	private Double nilaiDiIPK;

	/** Prodi yang dicakup aturan ini ({@code null} = semua); lihat {@link #getJurusan()}. */
	private Jurusan jurusan;

	/** Fakultas yang dicakup aturan ini ({@code null} = semua); lihat {@link #getFakultas()}. */
	private Fakultas fakultas;

	/** Tanggal mulai berlaku (informatif); lihat {@link #getTanggalMulaiBerlaku()}. */
	private Date tanggalMulaiBerlaku;

	/** Tahun akademik mulai berlaku; lihat {@link #getTahunAkademik()}. */
	private String tahunAkademik;

	/** Semester mulai berlaku; lihat {@link #getSemester()}. */
	private String semester;

	/** Kunci periode numerik turunan {@code tahunAkademik}+{@code semester}; lihat {@link #getTa()}. */
	private Integer ta;

	/** Kode bobot nilai versi Feeder/PDDikti; lihat {@link #getFeeder()}. */
	private String feeder;

	/** Apakah huruf ini dianggap lulus; lihat {@link #getLulus()}. */
	private Boolean lulus;

	/** Apakah status lulus ditampilkan di layar; lihat {@link #getTampilkanStatusLulus()}. */
	private Boolean tampilkanStatusLulus;

	/** Catatan bebas admin; lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Daftar kode matakuliah khusus; lihat {@link #getKodeMk()}. */
	private String kodeMk;

	/** Skema/tabel huruf alternatif; lihat {@link #getJenisNilaiHuruf()}. */
	private JenisNilaiHurufMatakuliah jenisNilaiHuruf;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA untuk membuat instance saat memuat
	 * baris. Dipakai juga oleh layar master saat menambah aturan baru; seluruh properti diisi
	 * belakangan lewat setter.
	 */
	public NilaiHuruf() {
	}

	/**
	 * Kunci primer baris aturan konversi ini.
	 *
	 * <p>Dibangkitkan database ({@code IDENTITY}) sehingga bernilai {@code null} sampai baris benar-benar
	 * disimpan. Kolomnya {@code insertable = false} &mdash; nilai yang diisi manual diabaikan saat
	 * {@code INSERT}.</p>
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci primer. Umumnya hanya dipakai Hibernate; kode aplikasi tidak perlu memanggilnya.
	 *
	 * @param id kunci primer baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Batas <b>bawah</b> rentang nilai angka yang dipetakan ke huruf ini (label layar: "Mulai").
	 *
	 * <p>Pencocokan bersifat <b>inklusif</b>: kandidat cocok bila {@code nilai >= mulai} dan
	 * {@code nilai <= sampai}. Nilai ini juga diekspor ke Feeder sebagai {@code bobot_nilai_min},
	 * dipakai membentuk legenda laporan ("A = 80 s.d 100"), dan &mdash; bersama
	 * {@link #getSampai() sampai} &mdash; dipakai merekonstruksi nilai angka dari huruf sebagai
	 * titik tengah {@code (mulai + sampai) / 2.0} pada jalur impor.</p>
	 *
	 * <p><b>Kolom {@code nullable = false}</b>, namun tidak ada validasi di sisi Java: banyak
	 * pemanggil melakukan auto-unboxing ({@code nilai >= nilaiHuruf.getMulai()}) sehingga baris
	 * dengan {@code mulai} kosong dapat memicu {@code NullPointerException} yang lalu ditelan blok
	 * {@code catch} pemanggil.</p>
	 *
	 * @return batas bawah rentang, atau {@code null} bila belum diisi
	 */
	@Column(name = "mulai", precision = 15, nullable = false)
	public Double getMulai() {
		return this.mulai;
	}

	/**
	 * Menyetel batas bawah rentang nilai angka. Dipanggil layar master saat menyimpan.
	 *
	 * @param mulai batas bawah rentang (inklusif)
	 */
	public void setMulai(Double mulai) {
		this.mulai = mulai;
	}

	/**
	 * Batas <b>atas</b> rentang nilai angka yang dipetakan ke huruf ini (label layar: "Sampai").
	 *
	 * <p>Inklusif, sepasang dengan {@link #getMulai()}. Tidak ada pemeriksaan tumpang tindih antar
	 * baris: bila dua aturan beririsan, yang menang adalah yang lebih dulu muncul pada urutan cache
	 * {@code ConstantValues.nilaiHurufs}.</p>
	 *
	 * @return batas atas rentang, atau {@code null} bila belum diisi
	 */
	@Column(name = "sampai", precision = 15, nullable = false)
	public Double getSampai() {
		return this.sampai;
	}

	/**
	 * Menyetel batas atas rentang nilai angka. Dipanggil layar master saat menyimpan.
	 *
	 * @param sampai batas atas rentang (inklusif)
	 */
	public void setSampai(Double sampai) {
		this.sampai = sampai;
	}

	/**
	 * Huruf hasil konversi (label layar: "Huruf") &mdash; mis. {@code A}, {@code B+}, {@code C},
	 * {@code D}, {@code E}, {@code T}.
	 *
	 * <p>Nilainya <b>di-{@code trim()} saat dibaca</b>, tetapi hasil trim <b>tidak</b> ditulis balik
	 * ke field, sehingga getter ini bebas efek samping. Perbandingan di seluruh sistem dilakukan
	 * tanpa memperhatikan besar-kecil huruf ({@code equalsIgnoreCase}) dan indeks
	 * {@code ConstantValues} memakai kunci huruf besar.</p>
	 *
	 * <p><b>Perhatian:</b> huruf inilah yang disalin ke {@code Detailperkuliahan.nilaiHuruf} dan
	 * kemudian dipakai membaca kembali konfigurasi ini lewat
	 * {@code ConstantValues.nilaiHurufTerkait(...)}. Mengubah teks huruf pada baris master yang
	 * sudah dipakai akan memutus keterkaitan itu untuk nilai-nilai lama.</p>
	 *
	 * @return huruf hasil konversi tanpa spasi tepi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nilai_huruf", nullable = false, length = 10)
	public String getNilaiHuruf() {
		return this.nilaiHuruf == null ? null : this.nilaiHuruf.trim();
	}

	/**
	 * Menyetel huruf hasil konversi. Disimpan apa adanya (tanpa trim); pembersihan spasi terjadi
	 * saat dibaca lewat {@link #getNilaiHuruf()}.
	 *
	 * @param nilaiHuruf huruf hasil konversi
	 */
	public void setNilaiHuruf(String nilaiHuruf) {
		this.nilaiHuruf = nilaiHuruf;
	}

	/**
	 * Menyetel bobot huruf untuk perhitungan IP/IPK.
	 *
	 * @param nilaiDiIPK bobot angka huruf ini (umumnya 0,0&ndash;4,0)
	 */
	public void setNilaiDiIPK(Double nilaiDiIPK) {
		this.nilaiDiIPK = nilaiDiIPK;
	}

	/**
	 * Bobot angka huruf ini saat dipakai menghitung IP/IPK (label layar: "Nilai di IPK") &mdash;
	 * mis. {@code A} = {@code 4.0}, {@code B} = {@code 3.0}.
	 *
	 * <p>Inilah nilai yang disalin ke {@code Detailperkuliahan.totalIP} /
	 * {@code totalIPSementara} setiap kali nilai dihitung ulang (penilaian manual, unggah nilai
	 * massal, impor Feeder, rekap OBE), dan yang diekspor ke Feeder sebagai {@code nilai_indeks}.
	 * Pemanggil hampir selalu menuliskan {@code nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK()},
	 * jadi <b>aturan yang tidak ditemukan berakibat IP 0,0</b>, bukan error.</p>
	 *
	 * <p>Kolom {@code nullable = true}: bila dibiarkan kosong, pemanggil yang melakukan
	 * auto-unboxing akan gagal &mdash; isi {@code 0.0} secara eksplisit untuk huruf yang tidak
	 * berbobot.</p>
	 *
	 * @return bobot IPK huruf ini, atau {@code null} bila belum diisi
	 */
	@Column(name = "nilai_di_ipk", precision = 15, nullable = true)
	public Double getNilaiDiIPK() {
		return nilaiDiIPK;
	}

	/**
	 * Menyetel angkatan yang dicakup aturan ini.
	 *
	 * @param tahunAngkatan tahun angkatan (mis. {@code 2020}); {@code 0} berarti berlaku umum
	 */
	public void setTahunAngkatan(Integer tahunAngkatan) {
		this.tahunAngkatan = tahunAngkatan;
	}

	/**
	 * Angkatan mahasiswa yang dicakup aturan ini (label layar: "Tahun Angkatan").
	 *
	 * <p>Pencocokan di {@code CommonAcademicKrsNilaiHelper} berlangsung dua tahap: lapis pertama
	 * menuntut <b>sama persis</b> ({@code tahunAngkatan.equals(...)}), lapis berikutnya melonggar
	 * menjadi <b>angkatan mahasiswa &ge; nilai ini</b>. Karena itu {@code 0} berfungsi sebagai
	 * "berlaku untuk semua angkatan" pada lapis kedua.</p>
	 *
	 * <p><b>Efek samping:</b> bila field masih {@code null}, getter ini <b>menulis {@code 0} ke
	 * field terpetakan</b>. Pada instance yang masih managed dan dengan {@code dynamicUpdate}
	 * aktif, sekadar membaca properti ini dapat memicu {@code UPDATE} saat flush. Penulisan ini
	 * memang disengaja: seluruh pemanggil membandingkan dengan operator relasional yang akan
	 * gagal bila nilainya {@code null}.</p>
	 *
	 * @return tahun angkatan; tidak pernah {@code null} (dinormalkan menjadi {@code 0})
	 */
	@Column(name = "tahun_angkatan", precision = 15, nullable = true)
	public Integer getTahunAngkatan() {
		if (tahunAngkatan == null) {
			tahunAngkatan = 0;
		}
		return tahunAngkatan;
	}

	/**
	 * Menyetel prodi yang dicakup aturan ini.
	 *
	 * @param jurusan prodi pemilik aturan, atau {@code null} agar aturan berlaku lintas prodi
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Program studi yang dicakup aturan ini (label layar: "Program Studi"); {@code null} = aturan
	 * berlaku untuk semua prodi.
	 *
	 * <p>Bersama {@link #getFakultas() fakultas}, properti inilah yang menentukan <b>prioritas</b>
	 * pencarian: aturan ber-jurusan menang atas aturan ber-fakultas, yang menang atas aturan global.
	 * Prioritas yang sama dipakai indeks {@code ConstantValues.nilaiHurufTerkait(...)}.</p>
	 *
	 * <p><b>Efek samping:</b> relasi {@code LAZY}, sehingga getter memanggil
	 * {@link GeneralValueObject#check(Object) check(...)} untuk meresolusi proxy dan
	 * <b>menulis balik hasilnya ke field</b>. Bila keempat tahap resolusi gagal, {@code check}
	 * mengembalikan argumen apa adanya (proxy tetap proxy), bukan melempar exception.</p>
	 *
	 * @return prodi pemilik aturan, atau {@code null} bila aturan berlaku lintas prodi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Menyetel fakultas yang dicakup aturan ini.
	 *
	 * <p><b>Perhatikan:</b> nilai yang disetel di sini dapat <b>ditimpa</b> saat dibaca kembali bila
	 * {@link #getJurusan() jurusan} terisi &mdash; lihat {@link #getFakultas()}.</p>
	 *
	 * @param fakultas fakultas pemilik aturan, atau {@code null} agar aturan berlaku lintas fakultas
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Fakultas yang dicakup aturan ini (label layar: "Fakultas"); {@code null} = aturan berlaku
	 * untuk semua fakultas.
	 *
	 * <p><b>Getter ini melakukan lebih dari sekadar membaca.</b> Urutan kerjanya:</p>
	 * <ol>
	 *   <li>meresolusi proxy lazy lewat {@link GeneralValueObject#check(Object) check(...)} dan
	 *       menulis balik hasilnya ke field {@code fakultas};</li>
	 *   <li>memanggil {@link #getJurusan()} &mdash; yang juga menulis balik field {@code jurusan};</li>
	 *   <li>bila jurusan terisi, <b>menimpa</b> {@code fakultas} dengan
	 *       {@code jurusan.getFakultas()}.</li>
	 * </ol>
	 *
	 * <p><b>Konsekuensi yang perlu disadari:</b> fakultas <b>tidak pernah</b> dapat berbeda dari
	 * fakultas induk prodi selama prodi terisi. Baris yang terlanjur menyimpan pasangan
	 * jurusan/fakultas tidak konsisten akan "diperbaiki" diam-diam saat dibaca, dan karena
	 * penulisan itu mengenai field terpetakan pada entity {@code dynamicUpdate}, perbaikan tersebut
	 * dapat ikut tersimpan ke database pada flush berikutnya tanpa aksi simpan eksplisit. Perilaku
	 * ini juga membuat lapis pencarian "fakultas saja" ({@code jurusan == null}) hanya terpicu oleh
	 * baris yang memang sengaja dibuat tanpa prodi.</p>
	 *
	 * @return fakultas pemilik aturan (diselaraskan dengan fakultas prodi bila prodi terisi), atau
	 *         {@code null} bila aturan berlaku lintas fakultas
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		jurusan = getJurusan();
		if (jurusan != null) {
			fakultas = jurusan.getFakultas();
		}

		return fakultas;
	}

	/**
	 * Kode bobot nilai versi <b>Feeder/PDDikti</b> ({@code kode_bobot_nilai}) untuk baris konversi
	 * ini.
	 *
	 * <p>Dipakai dua arah: saat <b>ekspor</b> nilai ke Feeder, baris dengan {@code feeder} kosong
	 * dilewati/diperlakukan khusus; saat <b>impor</b>, baris dicocokkan lewat kolom ini
	 * ({@code Restrictions.eq("feeder", ...)}) sehingga kode Feeder berfungsi sebagai kunci alami
	 * lintas sistem.</p>
	 *
	 * <p>Getter menormalkan hasil baca menjadi {@code null} bila kosong/berisi spasi saja, dan
	 * mengembalikan versi ter-{@code trim()}; <b>tanpa</b> menulis balik ke field, jadi bebas efek
	 * samping. Tidak tampil di form layar master &mdash; diisi oleh proses integrasi.</p>
	 *
	 * @return kode bobot nilai Feeder tanpa spasi tepi, atau {@code null} bila belum dipetakan
	 */
	public String getFeeder() {
		return feeder == null || feeder.trim().isEmpty() ? null : feeder.trim();
	}

	/**
	 * Menyetel kode bobot nilai versi Feeder/PDDikti.
	 *
	 * @param feeder kode {@code kode_bobot_nilai} dari Feeder
	 */
	public void setFeeder(String feeder) {
		this.feeder = feeder;
	}

	/**
	 * Tanggal mulai berlakunya aturan konversi ini.
	 *
	 * <p><b>Bersifat informatif.</b> Penyaringan periode yang sesungguhnya dilakukan lewat
	 * {@link #getTa() ta} (turunan tahun akademik + semester), bukan lewat tanggal ini; tidak ada
	 * jalur pencocokan yang membandingkan {@code tanggalMulaiBerlaku}. Properti ini juga tidak
	 * tampil di form layar master.</p>
	 *
	 * <p><b>Efek samping:</b> bila masih {@code null}, getter <b>mengisi field dengan tanggal hari
	 * ini</b> dan mengembalikannya. Pada instance managed hal ini dapat memicu {@code UPDATE} saat
	 * flush &mdash; membaca properti ini pada baris lama akan "memindahkan" tanggal berlakunya ke
	 * hari pembacaan.</p>
	 *
	 * @return tanggal mulai berlaku; tidak pernah {@code null} (diisi hari ini bila kosong)
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalMulaiBerlaku() {
		if (tanggalMulaiBerlaku == null) {
			tanggalMulaiBerlaku = ais.ui.util.WaktuUtil.getDate();
		}
		return tanggalMulaiBerlaku;
	}

	/**
	 * Menyetel tanggal mulai berlakunya aturan konversi ini.
	 *
	 * @param tanggalMulaiBerlaku tanggal mulai berlaku
	 */
	public void setTanggalMulaiBerlaku(Date tanggalMulaiBerlaku) {
		this.tanggalMulaiBerlaku = tanggalMulaiBerlaku;
	}

	/**
	 * Tahun akademik mulai berlakunya aturan ini (label layar: "Berlaku Mulai Tahun Akademik"),
	 * berformat {@code "2024/2025"}; {@code null}/kosong = berlaku sejak kapan pun.
	 *
	 * <p>Tidak dibandingkan langsung: nilainya diringkas menjadi kunci numerik {@link #getTa() ta}
	 * yang lalu diuji dengan {@code ta <= ta periode yang sedang dinilai}. Getter ini murni
	 * (tanpa efek samping), tetapi <b>dipanggil oleh {@link #getTa()}</b> yang tidak murni.</p>
	 *
	 * @return tahun akademik mulai berlaku, atau {@code null} bila tanpa batas awal
	 */
	public String getTahunAkademik() {
		return tahunAkademik;
	}

	/**
	 * Menyetel tahun akademik mulai berlaku.
	 *
	 * <p>Nilai ini menjadi masukan {@link #getTa()}; formatnya <b>harus</b> {@code "YYYY/YYYY"}
	 * karena potongan sebelum garis miring diambil sebagai angka tahun.</p>
	 *
	 * @param tahunAkademik tahun akademik berformat {@code "2024/2025"}, atau {@code null}
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Semester mulai berlakunya aturan ini (label layar: "Berlaku Mulai Semester"), bernilai
	 * {@code Perkuliahan.GANJIL} atau {@code Perkuliahan.GENAP}; {@code null}/kosong = tanpa batas
	 * semester.
	 *
	 * <p>Di layar master, kotak pilihan semester bersifat {@code readonly} &mdash; nilainya
	 * mengikuti pilihan tahun akademik. Seperti {@link #getTahunAkademik()}, nilai ini hanya dipakai
	 * sebagai masukan {@link #getTa()}.</p>
	 *
	 * @return semester mulai berlaku, atau {@code null} bila tanpa batas semester
	 */
	public String getSemester() {
		return semester;
	}

	/**
	 * Menyetel semester mulai berlaku.
	 *
	 * @param semester {@code Perkuliahan.GANJIL}, {@code Perkuliahan.GENAP}, atau {@code null}
	 */
	public void setSemester(String semester) {
		this.semester = semester;
	}

	/**
	 * <b>Kunci periode numerik</b> hasil peringkasan {@link #getTahunAkademik() tahunAkademik} +
	 * {@link #getSemester() semester} menjadi satu bilangan yang bisa dibandingkan dengan operator
	 * {@code <=} &mdash; mis. {@code "2024/2025"} + {@code GENAP} menjadi {@code 20242}
	 * ({@code GANJIL} menjadi {@code ...1}).
	 *
	 * <p><b>Cara pembentukan:</b> potongan tahun sebelum {@code "/"} (atau {@code "0"} bila tahun
	 * akademik kosong) disambung dengan digit semester ({@code "2"} untuk genap, {@code "1"} untuk
	 * selain itu, {@code "0"} bila semester kosong), lalu di-parse sebagai {@code Integer}. Bila
	 * parse gagal, kegagalan dicatat ke {@code ErrorAuditUtil} dan nilai {@code ta} sebelumnya
	 * dipertahankan; bila tetap {@code null}, hasilnya {@code 0}.</p>
	 *
	 * <p><b>Peran dalam pencocokan:</b> setiap lapis pencarian di
	 * {@code CommonAcademicKrsNilaiHelper} menyertakan syarat
	 * {@code ta == null || ta <= ta periode || tahunAkademik periode == null}, sehingga aturan hanya
	 * berlaku untuk periode itu <b>dan sesudahnya</b>. Nilai {@code ta} juga menjadi salah satu
	 * kunci pengurutan cache {@code ConstantValues.nilaiHurufs} (menurun), sehingga aturan yang
	 * lebih baru menang atas yang lebih lama.</p>
	 *
	 * <p><b>Efek samping (penting):</b> getter ini <b>selalu menghitung ulang dan menimpa</b> field
	 * terpetakan {@code ta} &mdash; bukan hanya saat {@code null}. Nilai {@code ta} yang tersimpan di
	 * database praktis hanyalah cache turunan; membaca properti ini pada instance managed dapat
	 * memicu {@code UPDATE} saat flush. Karena itu {@link #setTa(Integer)} tidak berpengaruh lama:
	 * hasilnya tertimpa pada pembacaan berikutnya.</p>
	 *
	 * <p>Tanpa {@code @Column} eksplisit &mdash; nama kolom mengikuti nama properti apa adanya
	 * ({@code MyNamingStrategy} turunan {@code DefaultNamingStrategy}).</p>
	 *
	 * @return kunci periode numerik; tidak pernah {@code null} (minimal {@code 0})
	 */
	public Integer getTa() {
		String id_smt = (getTahunAkademik() == null || getTahunAkademik().trim().isEmpty() ? "0"
				: getTahunAkademik().split("/")[0])
				+ (getSemester() == null || getSemester().trim().isEmpty() ? "0"
						: getSemester().equals(Perkuliahan.GENAP) ? "2" : "1");
		try {
			ta = Integer.parseInt(id_smt.trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/NilaiHuruf.java:224");

		}
		if (ta == null) {
			ta = 0;
		}
		return ta;
	}

	/**
	 * Menyetel kunci periode numerik secara langsung.
	 *
	 * <p><b>Praktis tidak berguna dari kode aplikasi:</b> {@link #getTa()} selalu menghitung ulang
	 * dan menimpa nilai ini dari {@code tahunAkademik} + {@code semester}. Setter tetap ada karena
	 * dibutuhkan Hibernate saat memuat baris dari database.</p>
	 *
	 * @param ta kunci periode numerik
	 */
	public void setTa(Integer ta) {
		this.ta = ta;
	}

	/**
	 * Apakah huruf pada aturan ini dianggap <b>LULUS</b> (centang "Lulus" pada master).
	 *
	 * <p>Inilah sumber kebenaran status lulus untuk seluruh sistem: {@code getLulus()} pada
	 * {@link Detailperkuliahan}, {@code MahasiswaDapatKelompokKkn},
	 * {@code MahasiswaDapatKelompokPkl}, dan {@code MahasiswaRequestTugasAkhir} membacanya lewat
	 * {@code ConstantValues.lulusDariNilaiHuruf(huruf, mahasiswa)} agar status lulus mengikuti
	 * konfigurasi admin, bukan tebakan string. Pewarnaan badge-nya di
	 * {@code ais.action.master.helper.util.WarnaStatusLulusUtil}.</p>
	 *
	 * <p><b>Nilai default diturunkan dari hurufnya</b> bila field masih {@code null}: huruf yang
	 * mengandung {@code "D"}, {@code "E"}, atau {@code "T"} (huruf besar) dianggap <b>tidak
	 * lulus</b>, selain itu lulus; bila huruf pun {@code null}, hasilnya {@code true}.</p>
	 *
	 * <p><b>Kuirk &amp; jebakan yang perlu disadari:</b></p>
	 * <ul>
	 *   <li>Pemeriksaannya {@code contains}, bukan kesetaraan &mdash; huruf apa pun yang
	 *       <i>memuat</i> D/E/T ikut dianggap tidak lulus. Untuk skema huruf tidak lazim, periksa
	 *       ulang dan isi centang secara eksplisit.</li>
	 *   <li>Turunan dibaca dari <b>field {@code nilaiHuruf} mentah</b>, bukan
	 *       {@link #getNilaiHuruf()}, sehingga huruf yang tersimpan dengan spasi tepi tetap
	 *       diperiksa apa adanya (untuk {@code contains} hal ini tidak mengubah hasil).</li>
	 *   <li><b>Efek samping:</b> hasil turunan <b>ditulis ke field terpetakan</b>. Pada instance
	 *       managed, sekadar membaca status lulus dapat mem-{@code persist} tebakan tersebut ke
	 *       database &mdash; setelah itu ia menjadi nilai eksplisit dan tidak lagi mengikuti
	 *       perubahan huruf.</li>
	 * </ul>
	 *
	 * <p>Tanpa {@code @Column} eksplisit &mdash; nama kolom mengikuti nama properti apa adanya.</p>
	 *
	 * @return {@code true} bila huruf ini dianggap lulus; tidak pernah {@code null}
	 */
	public Boolean getLulus() {
		if (lulus == null && nilaiHuruf != null) {
			if (nilaiHuruf.toUpperCase().contains("D") || nilaiHuruf.toUpperCase().contains("E")
					|| nilaiHuruf.toUpperCase().contains("T")) {
				lulus = false;
			} else {
				lulus = true;
			}
		} else if (lulus == null) {
			lulus = true;
		}
		return lulus;
	}

	/**
	 * Menyetel status lulus untuk huruf pada aturan ini.
	 *
	 * <p>Menyetel {@code null} mengembalikan properti ke mode "turunkan dari huruf" pada pembacaan
	 * berikutnya (lihat {@link #getLulus()}).</p>
	 *
	 * @param lulus {@code true} bila huruf dianggap lulus, {@code false} bila tidak,
	 *              {@code null} agar diturunkan otomatis dari huruf
	 */
	public void setLulus(Boolean lulus) {
		this.lulus = lulus;
	}

	/**
	 * Flag konfigurasi: apakah status <b>Lulus / Tidak Lulus</b> untuk nilai huruf ini
	 * DITAMPILKAN di halaman-halaman (mis. Dasbor Studi, Daftar Historis Pengambilan MK).
	 *
	 * <p>Default {@code true} = DITAMPILKAN. Admin dapat menyembunyikan status lulus untuk
	 * konfigurasi tertentu dengan MENGOSONGKAN centang ini (mis. selain jenjang S2).</p>
	 *
	 * <p>Dibaca lewat {@code ConstantValues.tampilkanStatusLulusDariNilaiHuruf(huruf, mahasiswa)}
	 * dengan prioritas konfigurasi per Jurusan &rarr; Fakultas &rarr; global. Tanpa konfigurasi yang
	 * cocok pun hasilnya tetap {@code true}.</p>
	 *
	 * <p><b>Tidak ada efek samping:</b> berbeda dari {@link #getLulus()}, nilai default {@code TRUE}
	 * hanya dikembalikan, <b>tidak</b> ditulis balik ke field &mdash; sehingga membaca properti ini
	 * tidak pernah mengotori entity.</p>
	 *
	 * <p><b>Diaudit</b> (bagian dari entitas {@code @Audited}). Karena itu kolom
	 * {@code tampilkan_status_lulus} WAJIB ada juga di tabel audit {@code new_audit.nilai_huruf__audit};
	 * bila tidak, INSERT audit gagal saat menyimpan NilaiHuruf (rollback). Jalankan ALTER kedua tabel.</p>
	 *
	 * @return {@code true} bila status lulus boleh ditampilkan; tidak pernah {@code null}
	 */
	@Column(name = "tampilkan_status_lulus")
	public Boolean getTampilkanStatusLulus() {
		return tampilkanStatusLulus == null ? Boolean.TRUE : tampilkanStatusLulus;
	}

	/**
	 * Menyetel apakah status lulus/tidak lulus ditampilkan di layar untuk huruf ini.
	 *
	 * @param tampilkanStatusLulus {@code false} untuk menyembunyikan status lulus;
	 *                             {@code true}/{@code null} berarti tampil
	 */
	public void setTampilkanStatusLulus(Boolean tampilkanStatusLulus) {
		this.tampilkanStatusLulus = tampilkanStatusLulus;
	}

	/**
	 * Catatan bebas admin untuk baris konversi ini (label layar: "Keterangan").
	 *
	 * <p>Murni dokumentasi &mdash; tidak dipakai logika pencocokan mana pun. Berguna menjelaskan
	 * mengapa sebuah aturan dibuat khusus (mis. "SK Rektor 2021, khusus prodi X").</p>
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel catatan bebas untuk baris konversi ini.
	 *
	 * @param keterangan catatan bebas
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Daftar <b>kode matakuliah khusus</b> yang dicakup aturan ini (label layar: "Khusus untuk kode
	 * matakuliah"), dipisah tanda koma; kosong = aturan berlaku untuk semua matakuliah.
	 *
	 * <p><b>Semantik pencocokan</b> (di {@code CommonAcademicKrsNilaiHelper.cocokKodeMkNilaiHuruf}):
	 * bila kode matakuliah yang dicari kosong, hanya aturan tanpa {@code kodeMk} yang cocok; bila
	 * {@code kodeMk} aturan kosong, aturan cocok untuk matakuliah apa pun; selain itu kode dicari
	 * sebagai elemen utuh di antara koma, tanpa memperhatikan besar-kecil huruf dan spasi. Properti
	 * ini juga menjadi kunci pengurutan <b>pertama</b> (menurun) pada cache
	 * {@code ConstantValues.nilaiHurufs}, sehingga aturan ber-{@code kodeMk} dievaluasi sebelum
	 * aturan umum.</p>
	 *
	 * <p><b>Efek samping &amp; kuirk normalisasi.</b> Getter ini <b>destruktif</b>: ia menormalkan
	 * dan <b>menulis balik</b> ke field terpetakan {@code kodeMk}. Bentuk hasilnya <b>dibungkus
	 * koma</b> di kedua ujung &mdash; {@code "BSC123"} menjadi {@code ",BSC123,"} &mdash; lalu koma
	 * ganda dirapikan lewat tiga kali {@code replaceAll(",,", ",")} berturut-turut (bentuk yang
	 * sudah ternormalkan bersifat idempoten pada pembacaan berikutnya). Rangkaian {@code if}
	 * sesudahnya membersihkan sisa kasus {@code ","}, {@code ",,"}, {@code ",,,"} menjadi string
	 * kosong; pemeriksaan {@code kodeMk == null} pada baris {@code return} sudah tidak mungkin
	 * benar karena field pasti sudah terisi string.</p>
	 *
	 * <p><b>Akibat yang terlihat pengguna:</b> layar master mengisi kotak teks dan label grid dengan
	 * hasil getter ini, dan menyimpan kembali apa yang ada di kotak teks &mdash; sehingga koma
	 * pembungkus ikut tersimpan dan tampil di layar. Pencocokan tetap benar (ia toleran terhadap
	 * koma berlebih), tetapi tampilannya membingungkan. Pada instance managed, membaca properti ini
	 * juga dapat memicu {@code UPDATE} saat flush.</p>
	 *
	 * @return daftar kode matakuliah dalam bentuk ternormalkan berkoma, atau string kosong bila
	 *         aturan berlaku untuk semua matakuliah; tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getKodeMk() {
		kodeMk = (kodeMk == null || kodeMk.trim().equalsIgnoreCase(",") ? "" : "," + kodeMk.trim() + ",")
				.replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (kodeMk.equals(",")) {
			kodeMk = "";
		} else if (kodeMk.equals(",,")) {
			kodeMk = "";
		} else if (kodeMk.equals(",,,")) {
			kodeMk = "";
		}
		return kodeMk == null ? "" : kodeMk.trim();
	}

	/**
	 * Menyetel daftar kode matakuliah khusus.
	 *
	 * <p>Disimpan apa adanya; normalisasi bentuk berkoma terjadi saat dibaca lewat
	 * {@link #getKodeMk()}. Bila diisi lebih dari satu kode, pisahkan dengan koma (mis.
	 * {@code "BSC123,DCFR45,DESW56"}).</p>
	 *
	 * @param kodeMk daftar kode matakuliah dipisah koma, atau {@code null}/kosong agar aturan
	 *               berlaku untuk semua matakuliah
	 */
	public void setKodeMk(String kodeMk) {
		this.kodeMk = kodeMk;
	}

	/**
	 * Skema/tabel huruf alternatif yang dipakai aturan ini (label layar: "Jenis Nilai Huruf");
	 * {@code null} = skema bawaan ("Nilai Huruf Default").
	 *
	 * <p>Memungkinkan satu institusi memiliki beberapa tabel konversi yang berbeda sekaligus &mdash;
	 * mis. skema khusus untuk matakuliah praktikum atau program tertentu. Pada pencarian, syarat
	 * kecocokannya <b>ketat</b>: skema aturan harus sama persis dengan skema yang diminta (keduanya
	 * {@code null}, atau keduanya terisi dengan id sama). Bila tidak ada aturan yang cocok, seluruh
	 * tangga pencarian <b>diulang sekali lagi tanpa jenis nilai huruf</b> sebagai fallback.</p>
	 *
	 * <p><b>Efek samping:</b> relasi {@code LAZY}; getter meresolusi proxy lewat
	 * {@link GeneralValueObject#check(Object) check(...)} dan menulis balik hasilnya ke field.</p>
	 *
	 * @return skema huruf yang dipakai aturan ini, atau {@code null} bila memakai skema bawaan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_nilai_huruf", nullable = true)
	public JenisNilaiHurufMatakuliah getJenisNilaiHuruf() {
		jenisNilaiHuruf = check(jenisNilaiHuruf);
		return jenisNilaiHuruf;
	}

	/**
	 * Menyetel skema/tabel huruf alternatif untuk aturan ini.
	 *
	 * @param jenisNilaiHuruf skema huruf, atau {@code null} untuk skema bawaan
	 */
	public void setJenisNilaiHuruf(JenisNilaiHurufMatakuliah jenisNilaiHuruf) {
		this.jenisNilaiHuruf = jenisNilaiHuruf;
	}

}
