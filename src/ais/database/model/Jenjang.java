package ais.database.model;

// Generated Dec 22, 2009 6:26:59 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

/**
 * Entity <b>MASTER jenjang pendidikan</b> &mdash; daftar referensi S1/S2/S3/D3/D4 dan seterusnya.
 * Dipetakan ke tabel {@code public.jenjang}, beranotasi {@link Audited} (seluruh perubahan direkam
 * Hibernate Envers) serta {@code dynamicInsert}/{@code dynamicUpdate}.
 *
 * <p>Ini salah satu master paling luas pemakaiannya di AIS: ±353 berkas menyebut nama kelas ini,
 * 125 berkas meng-{@code import}-nya langsung, dan ±145 titik memakai {@code Jenjang.class} untuk
 * query/combobox. Hampir seluruh sumbu akademik bergantung padanya &mdash; {@link Jurusan#getJenjang()}
 * (jenjang prodi), {@link Mahasiswa#getJenjang()} (turunan dari prodi), pelaporan EPSBED/PDDIKTI,
 * generator NIM/nomor registrasi PMB, tagihan/{@link DetailBiaya}, {@link Beasiswa},
 * {@link GelombangPendaftaran}, {@link Judisium}, {@link JadwalPembayaran}, dan seterusnya.</p>
 *
 * <h2>PENTING: satu tabel dipakai untuk DUA master yang berbeda</h2>
 *
 * <p>Tabel {@code jenjang} tidak hanya menampung jenjang program studi. Tabel yang sama juga menjadi
 * master <b>"Pendidikan Orang Tua"</b> (SD/SMP/SMA/Paket A-C/Profesi/Sp-1/Non formal/&hellip;). Pemisahnya
 * BUKAN kolom tipe/diskriminator, melainkan <b>dua flag boolean yang berbeda</b>:</p>
 * <ul>
 *   <li>{@link #getAktif()} &mdash; dipakai layar <b>{@code JenjangAction}</b> ("Pendataan Jenjang")
 *   dan combobox jenjang akademik ({@code BeasiswaAction}, {@code PendaftarBeasiswaHelper},
 *   {@code sekolah/JenisSekolahAction});</li>
 *   <li>{@link #getAktifDipilih()} &mdash; dipakai layar <b>{@code PendidikanOrtuAction}</b>
 *   ("Pendataan Pendidikan Orang Tua") dan combobox pendidikan ortu di
 *   {@code OrangTuaAction} untuk {@link BiodataMahasiswa#getJenjangPendidikanAyah()},
 *   {@code jenjangPendidikanIbu}, dan {@code jenjangPendidikanWali}.</li>
 * </ul>
 *
 * <p>Daftar pendidikan ortu di-seed otomatis oleh {@code InitDataHelper} dari string
 * {@code "0;Tidak sekolah|1;PAUD|2;TK / sederajat|&hellip;|99;Lainnya"} langsung ke tabel
 * {@code jenjang} ini (kolom {@link #getFeeder()} diisi kode PDDIKTI {@code id_jenj_didik}).
 * Karena itu satu instalasi normal berisi campuran baris "S1"/"D3" (akademik) dan
 * "SD / sederajat"/"Paket B" (pendidikan ortu) di tabel yang sama.</p>
 *
 * <p><b>Konsekuensi yang mudah terlewat:</b> kedua layar penyunting <i>tidak pernah</i> menyetel
 * flag lawannya. {@code JenjangAction.onSave} tidak mengisi {@code aktifDipilih}, dan
 * {@code PendidikanOrtuAction.onSave} tidak mengisi {@code aktif}. Karena kedua getter
 * mengembalikan {@code true} saat nilainya {@code null}, dan kedua {@code initCriteria} memakai
 * {@code isNull(flag) OR flag = true}, maka <b>setiap baris baru dari salah satu layar otomatis
 * muncul juga di layar dan combobox yang lain</b> sampai ada orang yang secara eksplisit
 * meng-uncheck kotak "Aktif" di layar tersebut. Menambah jenjang "S1 Terapan" akan memunculkannya
 * sebagai pilihan pendidikan ayah/ibu, dan sebaliknya menambah "Paket C" akan memunculkannya
 * sebagai pilihan jenjang program studi.</p>
 *
 * <p>Kekeliruan pemakaian flag juga sudah ada di kode: {@code KelompokMatakuliahAction} (konteks
 * akademik murni) menyaring comboboxnya dengan {@code aktifDipilih} &mdash; flag pendidikan ortu &mdash;
 * bukan {@code aktif}. Dicatat apa adanya, tidak diperbaiki di sini.</p>
 *
 * <h2>Jangan tertukar dengan {@link JenjangProgramStudi}</h2>
 *
 * <p>Meski namanya mirip, {@link JenjangProgramStudi} <b>bukan</b> tabel anak atau tabel detail dari
 * kelas ini dan tidak ada relasi kepemilikan di antara keduanya. {@link JenjangProgramStudi} adalah
 * profil administratif satu {@link Jurusan} (SK pendirian, akreditasi, kontak prodi, kode EPSBED)
 * yang kebetulan <i>menyalin</i> jenjang milik prodi induknya lewat
 * {@link JenjangProgramStudi#getJenjang()}. Master jenjang &mdash; yaitu kelas ini &mdash; tidak menyimpan
 * koleksi balik ke sana; arah relasinya satu arah dari {@link JenjangProgramStudi} ke sini.</p>
 *
 * <p>Kelas ini memang <b>tidak punya satu pun koleksi/relasi keluar</b>. Semua keterkaitan dibuat oleh
 * pihak lain lewat {@code @ManyToOne} ke tabel {@code jenjang}, sehingga menghapus satu baris jenjang
 * berpotensi meninggalkan FK menggantung di puluhan tabel.</p>
 *
 * <h2>Cache statis {@code ConstantValues}</h2>
 *
 * <p>Banyak alur tidak melakukan query, melainkan membaca instance yang sudah di-cache saat startup
 * oleh {@code InitDataHelper.initMaster()}:</p>
 * <ul>
 *   <li>{@code ConstantValues.d3}, {@code s1}, {@code s2}, {@code s3} &mdash; dicari berdasarkan
 *   {@link #getNama()} dengan beberapa ejaan alternatif ("S1", "Strata 1", "Strata Satu (S1)", &hellip;).
 *   Bila nama di database tidak cocok pola tersebut, nilainya tetap {@code null} dan pemanggil
 *   seperti {@code KonfigurasiBkdAction}/{@code LibraryUtil} diam-diam kehilangan default;</li>
 *   <li>{@code ConstantValues.s2T}/{@code s3T} &mdash; baris teknis "S-2T"/"S-3T" yang <b>dibuat otomatis
 *   bila belum ada</b> ({@code aktif=false}, {@code feeder=4L}/{@code 6L}). Jadi startup aplikasi
 *   dapat menulis baris baru ke tabel ini;</li>
 *   <li>{@code ConstantValues.d4} &mdash; <b>dideklarasikan dan dibaca, tetapi tidak pernah diisi
 *   di mana pun</b>. Akibatnya {@code MahasiswaRequestTugasAkhirAction} yang memanggil
 *   {@code tahapanPenyusunanTugasAkhir.setJenjang(ConstantValues.d4)} selalu menyetel {@code null},
 *   dan {@code BkdPengajaranHelper} selalu memasukkan satu elemen {@code null} ke array jenjangnya.
 *   Bug lama; dicatat, tidak diperbaiki di sini.</li>
 * </ul>
 *
 * <h2>Getter yang MENULIS BALIK ke field (auto-normalisasi)</h2>
 *
 * <p>Pemetaan Hibernate kelas ini memakai <b>akses properti</b> (anotasi menempel di getter), sehingga
 * Hibernate memanggil getter-getter di bawah ini saat <i>dirty checking</i> menjelang flush. Beberapa
 * di antaranya mengubah field-nya sendiri, sehingga entity menjadi "kotor" dan memicu {@code UPDATE}
 * &mdash; plus satu revisi Envers &mdash; <b>hanya karena baris tersebut dibaca/ditampilkan</b>:</p>
 * <ul>
 *   <li>{@link #getNama()} &mdash; {@code null} &rarr; ditulis {@code "-"};</li>
 *   <li>{@link #getKode()} &mdash; {@code null}/kosong &rarr; ditulis {@code "-"};</li>
 *   <li>{@link #getKeterangan()} &mdash; kosong &rarr; disalin dari field {@code nama};</li>
 *   <li>{@link #getJumlahSemester()} &mdash; {@code null} &rarr; ditebak dari nama jenjang, jatuh ke {@code 8};</li>
 *   <li>{@link #getJumlahSemesterMaksimal()} &mdash; {@code null} &rarr; {@code jumlahSemester + 2};</li>
 *   <li>{@link #getJumlahSemesterLulus()} &mdash; {@code null} &rarr; {@code jumlahSemester};</li>
 *   <li>{@link #getAktif()} dan {@link #getAktifDipilih()} &mdash; memaksa {@code false} untuk baris
 *   bernama {@code "(tidak diisi)"}; ini <b>menimpa</b> nilai {@code true} yang tersimpan.</li>
 * </ul>
 *
 * <p>Tidak ada getter di kelas ini yang menutup {@code Session} Hibernate, dan tidak ada getter yang
 * <i>menghapus</i> data (destruktif) &mdash; yang ada hanya pengisian nilai default seperti di atas plus
 * pemaksaan {@code false} pada dua flag untuk baris {@code "(tidak diisi)"}.</p>
 *
 * <h2>Pengelompokan properti</h2>
 * <ul>
 *   <li><b>Identitas &amp; label:</b> {@link #getId()}, {@link #getNama()}, {@link #getKode()},
 *   {@link #getKeterangan()}, {@link #getKeteranganEn()};</li>
 *   <li><b>Kode pelaporan luar:</b> {@link #getJenjangEpsbed()} (EPSBED/PDPT, dipakai seluruh
 *   {@code ais.action.master.epsbed.*}), {@link #getFeeder()} (id jenjang pendidikan PDDIKTI Feeder);</li>
 *   <li><b>Aturan masa studi:</b> {@link #getJumlahSemester()}, {@link #getJumlahSemesterLulus()},
 *   {@link #getJumlahSemesterMaksimal()};</li>
 *   <li><b>Flag ketersediaan:</b> {@link #getAktif()} (akademik), {@link #getAktifDipilih()}
 *   (pendidikan ortu);</li>
 *   <li><b>Teks bebas cetakan:</b> {@link #getSyarat()} (dipetakan ke parameter laporan
 *   {@code jenjang_syarat} pada ijazah/album wisuda);</li>
 *   <li><b>Jejak audit warisan induk:</b> {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 * </ul>
 *
 * <h2>Catatan pemetaan</h2>
 *
 * <p>Kelas ini {@code extends} {@link GeneralValueObject}, namun induknya <b>bukan</b> {@code @Entity}
 * maupun {@code @MappedSuperclass} &mdash; hanya POJO abstrak biasa, sehingga Hibernate tidak memetakan
 * properti induk sama sekali. Karena itu {@code id}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} <b>sengaja dideklarasikan ulang</b> di kelas ini; itu keharusan teknis, bukan
 * duplikasi yang salah. Lihat Javadoc {@link GeneralValueObject} untuk penjelasan lengkap.</p>
 *
 * <p>Hanya {@code id}, {@code nama}, {@code kode}, dan {@code jenjang_epsbed} yang memakai
 * {@code @Column} eksplisit. Sisanya jatuh ke {@code ais.database.hibernate.MyNamingStrategy}
 * (turunan {@code DefaultNamingStrategy}), yang memakai nama properti <b>apa adanya</b> tanpa konversi
 * ke {@code snake_case} &mdash; jadi kolomnya benar-benar bernama {@code keterangan}, {@code keteranganEn},
 * {@code syarat}, {@code jumlahSemester}, {@code jumlahSemesterLulus}, {@code jumlahSemesterMaksimal},
 * {@code aktif}, {@code aktifDipilih}, {@code feeder}, dan {@code tanggal_dirubah}.</p>
 *
 * <p>Akses data rutin lewat {@code DaoFactory.getInstance().getJenjangDao()}
 * ({@link ais.database.dao.JenjangDao}, murni CRUD generik tanpa method tambahan) atau lewat
 * {@code session.createCriteria(Jenjang.class)} langsung.</p>
 *
 * @see GeneralValueObject
 * @see JenjangProgramStudi
 * @see Jurusan#getJenjang()
 * @see Mahasiswa#getJenjang()
 * @see ais.database.dao.JenjangDao
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "jenjang")
public class Jenjang extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilainya dipatok agar instance {@link Jenjang} yang tersimpan
	 * di session ZK/HTTP tetap dapat dibaca ulang setelah aplikasi di-redeploy.
	 */
	private static final long serialVersionUID = 1494854764328309834L;

	/**
	 * Kunci primer baris jenjang, kolom {@code id}. Deklarasi ulang dari
	 * {@link GeneralValueObject}; lihat Javadoc kelas.
	 */
	private Long id;

	/**
	 * Nama pengguna terakhir yang mengubah baris ini. Diisi otomatis oleh
	 * {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}. Deklarasi ulang dari induk.
	 */
	private String oleh;

	/**
	 * Id/NIP pengguna terakhir yang mengubah baris ini. Diisi otomatis oleh
	 * {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}. Deklarasi ulang dari induk.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id/NIP pengubah, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah &mdash; <b>menolak nilai kosong</b>.
	 *
	 * <p>Bila argumen {@code null} atau hanya spasi, method langsung {@code return} tanpa mengubah
	 * apa pun, sehingga jejak audit lama tetap terjaga dan tidak bisa dihapus dengan menyetel
	 * string kosong. Umumnya dipanggil {@code AuditTimestampInterceptor}, bukan kode layar.</p>
	 *
	 * @param olehId id/NIP pengguna pengubah; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah &mdash; <b>menolak nilai kosong</b>, sama seperti
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengubah, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum {@code UPDATE} baris ini.
	 *
	 * <p>Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #setOleh(String)}/{@link #setOlehId(String)} dari konteks pengguna aktif serta
	 * memperbarui {@link #setTanggal_dirubah(Date)}. Tidak pernah dipanggil manual dari kode
	 * aplikasi, dan <b>tidak</b> berjalan pada {@code INSERT} (hanya {@code UPDATE}).</p>
	 *
	 * <p>Perhatikan interaksinya dengan getter auto-normalisasi yang didaftar di Javadoc kelas:
	 * pembacaan biasa pun bisa membuat entity kotor, memicu {@code UPDATE}, memanggil kait ini,
	 * dan menghasilkan revisi Envers atas nama pengguna yang kebetulan sedang membuka layar.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir. Diinisialisasi ke jam server saat object dibuat
	 * ({@code WaktuUtil.getDate()}), lalu diperbarui kait {@link #onUpdate()} pada setiap
	 * {@code UPDATE}. Deklarasi ulang dari induk; lihat Javadoc kelas.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir. Umumnya dipanggil
	 * {@code AuditTimestampInterceptor}, bukan kode layar.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir baris jenjang ini.
	 *
	 * <p>Tanpa {@code @Column}, sehingga jatuh ke penamaan default {@code MyNamingStrategy} &mdash;
	 * kolom {@code tanggal_dirubah} apa adanya, bertipe {@code TIMESTAMP}.</p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada object yang baru dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks {@code "<id>-<nama>"}, mis. {@code "3-S1"}.
	 *
	 * <p>Membaca <b>field</b> {@code nama} secara langsung, bukan {@link #getNama()}, sehingga tidak
	 * memicu penulisan balik {@code "-"} dan bisa menghasilkan {@code "3-null"} untuk baris yang
	 * namanya kosong. Dipakai antara lain oleh log diagnostik {@code FeederImporter} dan sebagai
	 * label default beberapa komponen ZK.</p>
	 *
	 * @return gabungan id dan nama, dipisah tanda hubung
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Nama jenjang, kolom {@code nama} (maks. 50 karakter) &mdash; mis. {@code "S1"}, {@code "D3"},
	 * atau (untuk baris pendidikan ortu) {@code "SMA / sederajat"}.
	 *
	 * <p>Ini praktis kunci bisnis: {@code JenjangAction.checkNamaJenjang} dan
	 * {@code PendidikanOrtuAction.checkNamaJenjang} menolak nama duplikat (tanpa memperhatikan
	 * huruf besar/kecil), dan {@code InitDataHelper} mencocokkan cache
	 * {@code ConstantValues.s1/s2/s3/d3} berdasarkan nilai field ini.</p>
	 */
	private String nama;

	/**
	 * Keterangan bebas dalam bahasa Indonesia. Ikut dicetak pada beberapa laporan dan dipakai sebagai
	 * teks deskripsi combobox. Bila kosong, {@link #getKeterangan()} mengisinya dari {@code nama}.
	 */
	private String keterangan;

	/**
	 * Keterangan bebas dalam bahasa Inggris, untuk ijazah/transkrip dwibahasa (parameter laporan
	 * {@code jenjang_en}). Bila {@code null}, {@link #getKeteranganEn()} jatuh ke versi Indonesia.
	 */
	private String keteranganEn;

	/**
	 * Teks persyaratan penerimaan jenjang ini ("Persyaratan Penerimaan" pada layar master).
	 * Diteruskan apa adanya ke parameter laporan {@code jenjang_syarat} pada cetakan album wisuda,
	 * ijazah, dan rekapitulasi mahasiswa.
	 */
	private String syarat;

	/**
	 * Kode singkat jenjang, kolom {@code kode} (maks. 150 karakter).
	 *
	 * <p>Bukan sekadar label: sejumlah generator NIM/nomor registrasi PMB menyusun nomor mahasiswa
	 * dari nilai ini &mdash; {@code PoltekBhaktiKencanaNimGenerator}, {@code StainBatusangkarNimGenerator},
	 * {@code StikomAmbonNimGenerator}, {@code YY_JENJANG_PRODI_URUT_NimGenerator},
	 * {@code TAHUN_SMT_JENJANG_NoRegGenerator} &mdash; dan beberapa laporan EMIS/EPSBED mengekspornya
	 * langsung. Mengubah kode jenjang karena itu berdampak pada nomor mahasiswa yang akan datang.</p>
	 */
	private String kode;

	/**
	 * Kode jenjang menurut format pelaporan EPSBED/PDPT DIKTI ("Kode PDPT/FEEDER" pada layar master),
	 * kolom {@code jenjang_epsbed} (maks. 50 karakter). Dipakai seluruh eksporter di
	 * {@code ais.action.master.epsbed.*} dan {@code StmikPalangkarayaNimGenerator}.
	 */
	private String jenjangEpsbed;

	/**
	 * Jumlah semester normal (masa studi standar) jenjang ini. Bila {@code null},
	 * {@link #getJumlahSemester()} menebaknya dari {@link #getNama()}.
	 */
	private Integer jumlahSemester;

	/**
	 * Jumlah semester minimal sebelum mahasiswa boleh dinyatakan lulus. Dipakai
	 * {@code MahasiswaExistingBusinessRules} dan validasi kelulusan di {@code MahasiswaAction}.
	 * Bila {@code null}, {@link #getJumlahSemesterLulus()} menyalinnya dari {@link #getJumlahSemester()}.
	 */
	private Integer jumlahSemesterLulus;

	/**
	 * Batas maksimal masa studi dalam semester. Dipakai untuk menghitung batas waktu studi/DO pada
	 * {@code Mahasiswa.hitungSmtLulus}, {@code DetailwisudaHelper}, dan beberapa laporan.
	 * Bila {@code null}, {@link #getJumlahSemesterMaksimal()} mengisinya dengan
	 * {@link #getJumlahSemester()}{@code  + 2}.
	 */
	private Integer jumlahSemesterMaksimal;

	/**
	 * Flag aktif untuk konteks <b>jenjang akademik</b> (layar {@code JenjangAction} dan combobox
	 * jenjang program studi). Lihat Javadoc kelas mengenai pemisahan dua master dalam satu tabel.
	 */
	private Boolean aktif;

	/**
	 * Flag aktif untuk konteks <b>pendidikan orang tua</b> (layar {@code PendidikanOrtuAction} dan
	 * combobox pendidikan ayah/ibu/wali). Lihat Javadoc kelas mengenai pemisahan dua master dalam
	 * satu tabel.
	 */
	private Boolean aktifDipilih;

	/**
	 * Id jenjang pendidikan pada PDDIKTI Feeder ({@code id_jenj_didik}). Dipakai
	 * {@code FeederConverter}/{@code FeederImporter}/{@code FeederJSONImport} untuk memetakan data
	 * Feeder ke baris lokal, dan di-seed {@code InitDataHelper} untuk daftar pendidikan ortu
	 * (0 = tidak sekolah &hellip; 99 = lainnya).
	 */
	private Long feeder;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Semua field dibiarkan {@code null} kecuali {@link #tanggal_dirubah} yang langsung diisi jam
	 * server. Perlu diingat: baris hasil konstruktor ini akan "menormalkan diri" saat getter-nya
	 * dibaca (nama &rarr; {@code "-"}, jumlah semester &rarr; {@code 8}, dst.) &mdash; lihat Javadoc kelas.</p>
	 */
	public Jenjang() {
	}

	/**
	 * Konstruktor pintasan yang hanya menetapkan kunci primer.
	 *
	 * <p>Dipakai untuk membuat referensi ringan ke satu baris jenjang tanpa memuatnya dari database
	 * (mis. sebagai nilai filter query). Object hasil konstruktor ini <b>tidak</b> berisi nama, kode,
	 * atau aturan semesternya; jangan diperlakukan sebagai entity terkelola.</p>
	 *
	 * @param id kunci primer baris jenjang yang dirujuk
	 */
	public Jenjang(Long id) {
		this.id = id;
	}

	/**
	 * Kunci primer baris jenjang &mdash; kolom {@code id}, {@code IDENTITY}, unik dan {@code NOT NULL}.
	 *
	 * <p>Beranotasi {@code insertable = false}: nilainya sepenuhnya dibangkitkan database saat
	 * {@code INSERT}, bukan dikirim aplikasi.</p>
	 *
	 * @return id baris, atau {@code null} bila entity belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci primer. Tanpa validasi.
	 *
	 * @param id kunci primer baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama jenjang, sudah dipangkas spasi tepinya.
	 *
	 * <p><b>Menulis balik ke field:</b> bila {@code nama} masih {@code null}, field diisi
	 * {@code "-"} lebih dulu. Karena pemetaan memakai akses properti, penulisan ini terlihat oleh
	 * <i>dirty checking</i> Hibernate dan dapat memicu {@code UPDATE} + revisi Envers walau
	 * pemanggilnya hanya menampilkan data. Setelah pengisian tersebut, cabang
	 * {@code this.nama == null ? null : ...} tidak akan pernah bernilai {@code null} lagi &mdash;
	 * praktis kode mati, dipertahankan apa adanya.</p>
	 *
	 * <p>Nilai kembalian dipangkas ({@code trim()}) tetapi <b>yang tersimpan di field tidak</b>,
	 * sehingga perbandingan langsung terhadap field mentah (mis. di {@link #toString()} atau
	 * {@code Restrictions.eq("nama", ...)}) bisa berbeda hasil dari perbandingan terhadap getter ini.</p>
	 *
	 * @return nama jenjang tanpa spasi tepi; {@code "-"} bila sebelumnya belum diisi
	 */
	@Column(name = "nama", length = 50)
	public String getNama() {
		if (nama == null) {
			nama = "-";
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama jenjang. Tanpa validasi maupun pemangkasan; pengecekan duplikat dilakukan di
	 * layar ({@code JenjangAction}/{@code PendidikanOrtuAction}), bukan di sini.
	 *
	 * @param nama nama jenjang baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Menetapkan kode jenjang. Tanpa validasi maupun pemangkasan.
	 *
	 * @param kode kode jenjang baru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Kode singkat jenjang &mdash; kolom {@code kode}.
	 *
	 * <p><b>Menulis balik ke field:</b> bila kosong atau hanya spasi, field diisi {@code "-"} dan
	 * nilai itulah yang dikembalikan; efek {@code UPDATE}/Envers sama dengan {@link #getNama()}.
	 * Perhatikan bahwa nilai kembalian di sini <b>tidak</b> dipangkas, berbeda dari
	 * {@link #getNama()}; pemakainya yang butuh nilai bersih memangkas sendiri (lihat
	 * {@code PoltekBhaktiKencanaNimGenerator} yang memanggil {@code getKode().trim()}).</p>
	 *
	 * <p>Karena beberapa generator NIM menyusun nomor mahasiswa dari nilai ini, baris jenjang yang
	 * kodenya belum diisi akan menghasilkan komponen NIM berupa tanda hubung.</p>
	 *
	 * @return kode jenjang; {@code "-"} bila sebelumnya kosong
	 */
	@Column(name = "kode", length = 150)
	public String getKode() {
		if (kode == null || kode.trim().isEmpty()) {
			kode = "-";
		}
		return kode;
	}

	/**
	 * Keterangan jenjang dalam bahasa Indonesia, sudah dipangkas spasi tepinya.
	 *
	 * <p><b>Menulis balik ke field:</b> bila keterangan {@code null} atau kosong, field diisi dari
	 * <b>field mentah {@code nama}</b> &mdash; bukan {@link #getNama()} &mdash; sehingga bila nama pun masih
	 * {@code null}, keterangan tetap {@code null} dan method mengembalikan string kosong
	 * ({@code ""}), bukan {@code null}. Sama seperti getter lain, penulisan ini dapat memicu
	 * {@code UPDATE} + revisi Envers pada saat flush.</p>
	 *
	 * @return keterangan tanpa spasi tepi, atau {@code ""} bila tidak ada nama maupun keterangan
	 */
	public String getKeterangan() {
		if (keterangan == null || keterangan.isEmpty()) {
			keterangan = nama;
		}
		return keterangan == null ? "" : keterangan.trim();
	}

	/**
	 * Menetapkan keterangan bahasa Indonesia. Tanpa validasi.
	 *
	 * @param keterangan teks keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan kode jenjang versi EPSBED/PDPT. Tanpa validasi.
	 *
	 * @param jenjangEpsbed kode EPSBED baru
	 */
	public void setJenjangEpsbed(String jenjangEpsbed) {
		this.jenjangEpsbed = jenjangEpsbed;
	}

	/**
	 * Kode jenjang versi pelaporan EPSBED/PDPT &mdash; kolom {@code jenjang_epsbed}.
	 *
	 * <p>Getter murni tanpa efek samping: nilainya dikembalikan apa adanya, termasuk {@code null}.
	 * Seluruh eksporter di {@code ais.action.master.epsbed.*} menulis nilai ini langsung ke sel
	 * Excel, sehingga baris jenjang yang kodenya belum diisi menghasilkan sel kosong (atau
	 * {@code NullPointerException} pada pemanggil yang tidak menjaga {@code null}, mis.
	 * {@code MasterMahasiswa}).</p>
	 *
	 * @return kode EPSBED, atau {@code null} bila belum diisi
	 */
	@Column(name = "jenjang_epsbed", length = 50)
	public String getJenjangEpsbed() {
		return jenjangEpsbed;
	}

	/**
	 * Id jenjang pendidikan pada PDDIKTI Feeder ({@code id_jenj_didik}).
	 *
	 * <p>Getter murni tanpa efek samping. Dipakai {@code FeederImporter}/{@code FeederJSONImport}
	 * untuk mencocokkan baris lokal dengan data Feeder ({@code Restrictions.eq("feeder", ...)});
	 * baris yang nilainya {@code null} dilewati oleh importer.</p>
	 *
	 * @return id jenjang Feeder, atau {@code null} bila belum dipetakan
	 */
	public Long getFeeder() {
		return feeder;
	}

	/**
	 * Menetapkan id jenjang pendidikan PDDIKTI Feeder. Tanpa validasi.
	 *
	 * @param feeder id jenjang pada Feeder
	 */
	public void setFeeder(Long feeder) {
		this.feeder = feeder;
	}

	/**
	 * Jumlah semester normal jenjang ini &mdash; angka yang menjadi dasar hampir semua perhitungan masa
	 * studi di sistem.
	 *
	 * <p><b>Menulis balik ke field &amp; menebak nilai:</b> bila {@code jumlahSemester} masih
	 * {@code null}, method menebaknya dari field {@code nama} (setelah {@code trim()}, tanpa
	 * memperhatikan huruf besar/kecil) menurut tabel berikut:</p>
	 * <ul>
	 *   <li>{@code "S1"} &rarr; 8, {@code "S2"} &rarr; 4, {@code "S3"} &rarr; 6;</li>
	 *   <li>{@code "D4"} &rarr; 8, {@code "D3"} &rarr; 6, {@code "D2"} &rarr; 4, {@code "D1"} &rarr; 2;</li>
	 *   <li>selain itu (termasuk nama panjang seperti {@code "Strata Satu (S1)"}, seluruh baris
	 *   pendidikan ortu, dan nama {@code null}) &rarr; <b>8</b> sebagai default terakhir.</li>
	 * </ul>
	 *
	 * <p>Pencocokan bersifat sama-persis, jadi instalasi yang menamai jenjangnya
	 * {@code "Strata Dua (S2)"} tetap mendapat 8 semester, bukan 4. Hasil tebakan <b>ditulis ke
	 * field</b>, sehingga ikut tersimpan ke database pada flush berikutnya (plus revisi Envers) &mdash;
	 * nilai tebakan menjadi permanen tanpa pernah disetujui operator.</p>
	 *
	 * <p>Dipanggil dari banyak tempat: perulangan pembentukan semester kurikulum
	 * ({@code DetailSemesterKurikulumHelper}, {@code TemplatePerkuliahanDetailHelper},
	 * {@code CapaianLulusanVsKurikulumMatakuliahAction}), batas pilihan semester KRS
	 * ({@code AmbilDataPerkuliahanHelper}, {@code KrsMahasiswa}), dan perhitungan status mahasiswa
	 * ({@code HistoryStatusMahasiswa}, {@code HistoryStatusMahasiswaUtil}).</p>
	 *
	 * @return jumlah semester normal; tidak pernah {@code null} (minimal 8)
	 */
	public Integer getJumlahSemester() {
		if (jumlahSemester == null) {
			if (nama != null && nama.trim().equalsIgnoreCase("S1")) {
				jumlahSemester = 8;
			} else if (nama != null && nama.trim().equalsIgnoreCase("S2")) {
				jumlahSemester = 4;
			} else if (nama != null && nama.trim().equalsIgnoreCase("S3")) {
				jumlahSemester = 6;
			} else if (nama != null && nama.trim().equalsIgnoreCase("D4")) {
				jumlahSemester = 8;
			} else if (nama != null && nama.trim().equalsIgnoreCase("D3")) {
				jumlahSemester = 6;
			} else if (nama != null && nama.trim().equalsIgnoreCase("D2")) {
				jumlahSemester = 4;
			} else if (nama != null && nama.trim().equalsIgnoreCase("D1")) {
				jumlahSemester = 2;
			}
		}
		if (jumlahSemester == null) {
			jumlahSemester = 8;
		}
		return jumlahSemester;
	}

	/**
	 * Menetapkan jumlah semester normal. Tanpa validasi &mdash; nilai {@code null} dari
	 * {@code JenjangAction.onSave} (kotak isian dikosongkan) akan membuat getter menebak ulang.
	 *
	 * @param jumlahSemester jumlah semester normal baru
	 */
	public void setJumlahSemester(Integer jumlahSemester) {
		this.jumlahSemester = jumlahSemester;
	}

	/**
	 * Status aktif jenjang untuk konteks <b>akademik</b> (combobox jenjang program studi, layar
	 * {@code JenjangAction}).
	 *
	 * <p><b>Menulis balik ke field:</b> bila field {@code nama} bernilai {@code "(tidak diisi)"}
	 * (tanpa memperhatikan huruf besar/kecil), flag dipaksa {@code false} &mdash; <b>menimpa</b> nilai
	 * {@code true} yang mungkin tersimpan di database, dan perubahan itu ikut ter-{@code UPDATE}
	 * pada flush. Ini cara sistem menyembunyikan baris placeholder dari daftar pilihan.</p>
	 *
	 * <p>Bila field {@code aktif} masih {@code null}, method mengembalikan {@code true} <b>tanpa</b>
	 * menulis balik &mdash; jadi baris yang belum pernah disetel dianggap aktif. Perilaku ini sejalan
	 * dengan {@code initCriteria} kedua layar master yang memfilter dengan
	 * {@code isNull(aktif) OR aktif = true}, tetapi berarti pula baris baru dari layar
	 * "Pendidikan Orang Tua" otomatis ikut muncul sebagai pilihan jenjang akademik (lihat Javadoc
	 * kelas).</p>
	 *
	 * @return {@code true} bila jenjang boleh dipilih di konteks akademik
	 */
	public Boolean getAktif() {
		if (nama != null && nama.trim().equalsIgnoreCase("(tidak diisi)")) {
			aktif = false;
		}
		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan status aktif konteks akademik. Dipanggil langsung dari checkbox "Aktif" di grid
	 * {@code JenjangAction}, yang setelah itu memanggil {@code Common.refreshSaveOrUpdate}.
	 *
	 * @param aktif status aktif baru; {@code null} akan dibaca sebagai aktif oleh {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Keterangan jenjang dalam bahasa Inggris, untuk cetakan dwibahasa (parameter laporan
	 * {@code jenjang_en} pada ijazah, album wisuda, dan rekapitulasi).
	 *
	 * <p>Bila field {@code keteranganEn} masih {@code null}, method <b>tidak</b> menulis balik apa pun
	 * &mdash; ia hanya jatuh ke {@link #getKeterangan()} sebagai nilai kembalian. Namun perlu dicatat
	 * bahwa {@link #getKeterangan()} sendiri punya efek samping menulis balik, sehingga memanggil
	 * getter ini secara tidak langsung bisa mengubah field {@code keterangan}.</p>
	 *
	 * <p>String kosong ({@code ""}) diperlakukan sebagai nilai sah, jadi baris yang kolom Inggrisnya
	 * pernah disimpan kosong akan mencetak sel kosong, bukan jatuh ke versi Indonesia.</p>
	 *
	 * @return keterangan bahasa Inggris, atau keterangan bahasa Indonesia sebagai cadangan
	 */
	public String getKeteranganEn() {
		return keteranganEn == null ? getKeterangan() : keteranganEn;
	}

	/**
	 * Menetapkan keterangan bahasa Inggris. Tanpa validasi.
	 *
	 * @param keteranganEn teks keterangan bahasa Inggris baru
	 */
	public void setKeteranganEn(String keteranganEn) {
		this.keteranganEn = keteranganEn;
	}

	/**
	 * Batas maksimal masa studi dalam semester.
	 *
	 * <p><b>Menulis balik ke field:</b> bila masih {@code null}, diisi
	 * {@link #getJumlahSemester()}{@code  + 2}. Karena {@link #getJumlahSemester()} sendiri tidak
	 * pernah mengembalikan {@code null}, syarat {@code getJumlahSemester() != null} selalu benar dan
	 * nilai kembalian method ini juga tidak pernah {@code null} &mdash; minimal {@code 10}. Kedua getter
	 * dapat menulis field sekaligus, sehingga satu pembacaan bisa menghasilkan dua kolom baru
	 * ter-{@code UPDATE}.</p>
	 *
	 * <p>Dipakai untuk menghitung batas waktu studi/DO: {@code Mahasiswa.hitungSmtLulus},
	 * {@code MahasiswaAction}, {@code DetailwisudaHelper}, serta laporan album wisuda, prestasi
	 * mahasiswa, dan rekapitulasi PA. Sebagian besar pemanggil tetap menulis
	 * {@code != null} defensif walau secara praktis tak pernah terpicu.</p>
	 *
	 * @return batas maksimal semester; tidak pernah {@code null}
	 */
	public Integer getJumlahSemesterMaksimal() {
		if (jumlahSemesterMaksimal == null && getJumlahSemester() != null) {
			jumlahSemesterMaksimal = getJumlahSemester() + 2;
		}
		return jumlahSemesterMaksimal;
	}

	/**
	 * Menetapkan batas maksimal masa studi. Tanpa validasi terhadap
	 * {@link #getJumlahSemester()}/{@link #getJumlahSemesterLulus()}, sehingga kombinasi tak masuk
	 * akal (maksimal &lt; minimal lulus) tetap dapat disimpan.
	 *
	 * @param jumlahSemesterMaksimal batas maksimal semester baru
	 */
	public void setJumlahSemesterMaksimal(Integer jumlahSemesterMaksimal) {
		this.jumlahSemesterMaksimal = jumlahSemesterMaksimal;
	}

	/**
	 * Teks persyaratan penerimaan jenjang ini.
	 *
	 * <p>Getter murni tanpa efek samping. Nilainya diteruskan apa adanya ke parameter laporan
	 * {@code jenjang_syarat} pada cetakan album wisuda, ijazah, dan rekapitulasi mahasiswa; template
	 * JasperReports yang menerimanya harus siap menghadapi {@code null}.</p>
	 *
	 * @return teks persyaratan, atau {@code null} bila belum diisi
	 */
	public String getSyarat() {
		return syarat;
	}

	/**
	 * Menetapkan teks persyaratan penerimaan. Tanpa validasi.
	 *
	 * @param syarat teks persyaratan baru
	 */
	public void setSyarat(String syarat) {
		this.syarat = syarat;
	}

	/**
	 * Status aktif jenjang untuk konteks <b>pendidikan orang tua</b> (combobox pendidikan
	 * ayah/ibu/wali di {@code OrangTuaAction}, layar {@code PendidikanOrtuAction}).
	 *
	 * <p>Perilakunya kembar persis dengan {@link #getAktif()}: baris bernama {@code "(tidak diisi)"}
	 * dipaksa {@code false} (menulis balik ke field, bisa memicu {@code UPDATE} + revisi Envers),
	 * dan field {@code null} dibaca sebagai {@code true} tanpa penulisan balik. Karena itu setiap
	 * jenjang akademik baru yang dibuat lewat {@code JenjangAction} otomatis ikut muncul sebagai
	 * pilihan pendidikan orang tua &mdash; lihat Javadoc kelas.</p>
	 *
	 * @return {@code true} bila baris boleh dipilih sebagai pendidikan orang tua
	 */
	public Boolean getAktifDipilih() {
		if (nama != null && nama.trim().equalsIgnoreCase("(tidak diisi)")) {
			aktifDipilih = false;
		}
		return aktifDipilih == null ? true : aktifDipilih;
	}

	/**
	 * Menetapkan status aktif konteks pendidikan orang tua. Dipanggil langsung dari checkbox "Aktif"
	 * di grid {@code PendidikanOrtuAction}, yang setelah itu memanggil
	 * {@code Common.refreshSaveOrUpdate}.
	 *
	 * @param aktifDipilih status baru; {@code null} akan dibaca sebagai aktif oleh
	 *        {@link #getAktifDipilih()}
	 */
	public void setAktifDipilih(Boolean aktifDipilih) {
		this.aktifDipilih = aktifDipilih;
	}

	/**
	 * Jumlah semester minimal sebelum seorang mahasiswa boleh dinyatakan lulus pada jenjang ini
	 * ("Jumlah Minimal Semester Lulus"; menurut tooltip layar master, aturan ini tidak berlaku bagi
	 * mahasiswa pindahan dan alih prodi).
	 *
	 * <p><b>Menulis balik ke field:</b> bila masih {@code null}, disalin dari
	 * {@link #getJumlahSemester()} &mdash; yang juga bisa menulis field-nya sendiri &mdash; sehingga nilai
	 * kembalian tidak pernah {@code null}.</p>
	 *
	 * <p>Dipakai {@code MahasiswaExistingBusinessRules} (batas minimum saat impor/validasi data
	 * mahasiswa lama) dan {@code MahasiswaAction} yang menolak semester lulus di bawah angka ini.</p>
	 *
	 * @return jumlah semester minimal untuk lulus; tidak pernah {@code null}
	 */
	public Integer getJumlahSemesterLulus() {
		if (jumlahSemesterLulus == null) {
			jumlahSemesterLulus = getJumlahSemester();
		}
		return jumlahSemesterLulus;
	}

	/**
	 * Menetapkan jumlah semester minimal untuk lulus. Tanpa validasi terhadap
	 * {@link #getJumlahSemesterMaksimal()}.
	 *
	 * @param jumlahSemesterLulus jumlah semester minimal baru
	 */
	public void setJumlahSemesterLulus(Integer jumlahSemesterLulus) {
		this.jumlahSemesterLulus = jumlahSemesterLulus;
	}

}
