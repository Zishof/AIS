package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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
 * Entity MASTER <b>status awal mahasiswa</b> — daftar "jalur masuk" yang dipakai untuk menandai
 * bagaimana seorang mahasiswa pertama kali tercatat di perguruan tinggi: <i>Baru</i>,
 * <i>Pindahan</i>, <i>Alih Prodi</i>, <i>Baru-Beasiswa</i>, <i>Transfer</i>, <i>PKU</i>,
 * <i>Prog. Khusus</i>, dan seterusnya. Satu baris tabel {@code public.status_awal_mahasiswa}
 * mewakili satu jalur masuk.
 *
 * <p>Meskipun isinya hanya beberapa baris teks, entity ini adalah salah satu master paling banyak
 * dirujuk di AIS: penelusuran seluruh source tree menemukan sekitar <b>1.400 rujukan pada ~194
 * berkas</b>. Nilainya ikut menentukan hasil akademik (validasi wajib-isi di layar mahasiswa,
 * ekspor PDDikti/Feeder), keuangan (pemilihan tarif dan diskon), sampai penomoran NIM.</p>
 *
 * <h3>Peran dalam model data</h3>
 *
 * <p>Entity ini adalah sisi "satu" dari sejumlah relasi {@code @ManyToOne}. Pemakai utamanya:</p>
 * <ul>
 *   <li><b>Data mahasiswa</b> — {@link Mahasiswa} menyimpan TIGA slot: status awal utama
 *       ({@code Mahasiswa.getStatusAwalMahasiswa()}) plus dua status susulan yang berlaku mulai
 *       semester tertentu ({@code getStatusAwalMahasiswaSetelahSmtTertentu()} dan
 *       {@code ...Lagi()}).</li>
 *   <li><b>Riwayat status per semester</b> — {@link HistoryStatusMahasiswa} menyimpan hasil
 *       resolusi status awal untuk setiap semester, lewat
 *       {@code HistoryStatusMahasiswa.ambilStatusAwal(Mahasiswa, Integer, StatusAwalMahasiswa)}.</li>
 *   <li><b>Kelompok kebijakan</b> — {@link ais.database.model.KelompokMahasiswa} menyimpan TIGA
 *       pasangan (status awal, semester mulai, semester sampai). Kelompok inilah PRIORITAS
 *       TERTINGGI pada rantai resolusi di atas: bila seorang mahasiswa masuk sebuah kelompok dan
 *       semester yang dinilai berada di dalam rentang salah satu slot, status awal dari kelompok
 *       MENIMPA status awal yang tercatat pada baris mahasiswa itu sendiri.</li>
 *   <li><b>Pendaftaran (PMB)</b> — {@link BiodataCalonMahasiswa} (dua slot: status awal dan
 *       status awal saat diterima), {@link KelompokCalonMahasiswa}, {@link AfiliasiCalonMahasiswa},
 *       serta {@link GelombangPendaftaran} (sebagai nilai default gelombang).</li>
 *   <li><b>Keuangan</b> — {@link SettingBiaya} dan {@link DetailBiaya} memakainya sebagai salah
 *       satu dimensi penentuan tarif; {@link JenisDiskonMahasiswa} memakainya sebagai syarat
 *       kelayakan diskon (dibandingkan berdasarkan {@link #getId()}).</li>
 *   <li><b>Akademik &amp; pelaporan lain</b> — {@link SyaratUjian}, {@link Judisium},
 *       {@link Kegiatan}, {@link RencanaTahunAkademik}, {@link VirtualAccountBank},
 *       {@code FormatNilaiSkripsi}, {@code FormatNilaiProposalSkripsi},
 *       {@code surat.KlasifikasiSuratKeluarUntuk}, serta puluhan dasbor/laporan yang menyisipkan
 *       {@code and a.status_awal_mahasiswa = <id>} langsung ke SQL native-nya.</li>
 * </ul>
 *
 * <h3>Rantai resolusi status awal (penting)</h3>
 *
 * <p>Nilai yang "benar" untuk satu mahasiswa pada satu semester TIDAK selalu berasal dari kolom
 * mahasiswa. Urutan yang dipakai {@code HistoryStatusMahasiswa.ambilStatusAwal(...)} adalah:</p>
 * <ol>
 *   <li>slot 1/2/3 pada {@link ais.database.model.KelompokMahasiswa} yang rentang semesternya cocok;</li>
 *   <li>{@code Mahasiswa.getStatusAwalMahasiswa()};</li>
 *   <li>id yang ditulis di konfigurasi (di-resolve lewat cache {@code ConstantValues.ambil(...)});</li>
 *   <li>{@code Mahasiswa.getStatusAwalMahasiswaSetelahSmtTertentu()} / {@code ...Lagi()} bila
 *       semester yang dinilai sudah melewati ambang yang ditentukan.</li>
 * </ol>
 *
 * <h3>Baris istimewa yang dibuat otomatis</h3>
 *
 * <p>{@code InitDataHelper} MENCARI, dan bila tidak ketemu ikut <b>MENULIS</b>, sejumlah baris
 * tetap saat aplikasi start, lalu mengikatnya ke konstanta global:</p>
 * <table border="1">
 *   <tr><th>Konstanta</th><th>Pencarian</th><th>Dibuat otomatis bila tidak ada?</th></tr>
 *   <tr><td>{@code ConstantValues.BARU}</td>
 *       <td>nama = "Baru" / "Peserta didik baru", lalu prefiks "Baru%"</td>
 *       <td>Ya — {@code kode=""}, {@code nama="Baru"}</td></tr>
 *   <tr><td>{@code ConstantValues.BARU_BEASISWA}</td>
 *       <td>nama = "Baru-Beasiswa" / "Beasiswa"</td>
 *       <td>Ya — {@code kode="1"}, {@code nama="Baru-Beasiswa"}</td></tr>
 *   <tr><td>{@code ConstantValues.PINDAHAN}</td><td>nama = "Pindahan" (persis)</td>
 *       <td>Ya — {@code kode=""}</td></tr>
 *   <tr><td>{@code ConstantValues.ALIH_PRODI}</td><td>nama = "Alih Prodi" (persis)</td>
 *       <td>Ya — {@code kode=""}</td></tr>
 * </table>
 * <p>Konsekuensi yang perlu diketahui sebelum mengganti nama baris di layar master: <b>pengikatan
 * konstanta di atas berbasis NAMA, bukan id</b>. Mengubah "Pindahan" menjadi, misalnya, "Pindahan
 * Kampus" membuat {@code ConstantValues.PINDAHAN} gagal ditemukan pada start berikutnya, dan
 * {@code InitDataHelper} akan <i>membuat baris "Pindahan" BARU</i> — master jadi punya dua baris
 * bermakna sama, sementara {@code CommonPMB} (konversi calon mahasiswa menjadi mahasiswa) memakai
 * baris yang baru itu. {@code ConstantValues.BARU} dipakai juga oleh {@code InitMenuHelper} untuk
 * mengisi kolom {@code detail_biaya.status_awal_mahasiswa} yang masih {@code null} lewat UPDATE
 * SQL native, sehingga baris tarif lama ikut terpaku ke id "Baru" hasil pencarian tersebut.
 * Catatan: {@code ConstantValues.ALIH_PRODI} di-seed tetapi tidak pernah dibaca satu pun kode
 * lain — konstanta mati.</p>
 *
 * <h3>Integrasi Feeder/PDDikti</h3>
 *
 * <p>{@link #getFeeder()} menyimpan {@code id_jns_daftar} versi Feeder, dipakai apa adanya saat
 * ekspor ({@code EksporMahasiswaFeeder} menulis {@code mahasiswa.getStatusAwalMahasiswa().getFeeder()}
 * ke kolom ke-11) dan sebagai kunci pencocokan saat impor
 * ({@code FeederUtil.getDataByFeeder(..., StatusAwalMahasiswa.class)}). Impor referensi
 * ({@code FeederImporter}/{@code FeederJSONImport}) mencocokkan baris lokal berdasarkan
 * {@code feeder} lebih dulu, lalu {@code nama}; bila tetap tidak ketemu, ia menjalankan UPDATE SQL
 * <b>massal</b> — misalnya {@code update status_awal_mahasiswa set feeder = ? where nama ilike
 * '%baru%'} untuk "Peserta didik baru", dan {@code where nama in ('Transfer','PKU','Prog. Khusus')}
 * untuk "Lainnya". Pola {@code '%baru%'} juga mengenai baris "Baru-Beasiswa", jadi dua baris berbeda
 * bisa berakhir dengan kode Feeder yang sama; dan karena ini SQL native, Envers tidak mencatat
 * perubahannya.</p>
 *
 * <h3>Layar dan penjagaan hak akses</h3>
 *
 * <p>Layar CRUD-nya adalah {@code /pages/master/status_awal_mahasiswa.zul} yang di-apply oleh
 * {@code ais.action.master.StatusAwalMahasiswaAction}; layar yang sama juga disisipkan sebagai tab
 * {@code MyInclude} di {@code KelompokMahasiswaAction}, {@code KelompokCalonMahasiswaAction}, dan
 * {@code ProgramMahasiswaAction}. Action-nya memanggil {@code Common.doCheckSecurity()} di
 * {@code doBeforeCompose}, tetapi halaman ini <b>tidak terdaftar</b> pada whitelist
 * {@code CommonPrivilages.MUST_CHECKED} sehingga pemanggilan itu tidak menegakkan apa pun (pola
 * sistemik yang sudah didokumentasikan pada sesi-sesi sebelumnya). Yang benar-benar menjaga adalah
 * pemeriksaan eksplisit di {@code doAfterCompose}: {@code CREATE} untuk tombol Tambah, {@code UPDATE}
 * untuk ketiga checkbox grid dan tombol Ubah, {@code DELETE} untuk tombol Hapus — di layar ini
 * ketiga checkbox memang ikut dinonaktifkan bila hak {@code UPDATE} tidak ada, berbeda dengan
 * beberapa layar master lain yang membiarkan checkbox-nya terbuka.</p>
 *
 * <p>Terpisah dari itu, <b>siapa yang boleh mengganti status awal seorang MAHASISWA</b> (bukan
 * master ini) dibatasi konfigurasi {@code status_awal_mahasiswa_hanya_boleh_diubah_oleh} — daftar
 * {@code roleId} dipisah titik-koma, dibaca di {@code MahasiswaAction} dan
 * {@code TampilStudiMahasiswaHelper}. Kosong berarti semua peran boleh. Pembatasannya bersifat
 * tampilan (combobox diganti {@code Label}), jadi jalur tulis non-ZK tidak terpengaruh.</p>
 *
 * <h3>Pengelompokan anggota kelas</h3>
 * <ul>
 *   <li><b>Identitas &amp; konstruktor</b> — {@link #getId()}, {@link #setId(Long)},
 *       {@link #StatusAwalMahasiswa()}, {@link #StatusAwalMahasiswa(Long)}.</li>
 *   <li><b>Atribut deskriptif</b> — {@link #getKode()}/{@link #setKode(String)},
 *       {@link #getNama()}/{@link #setNama(String)}.</li>
 *   <li><b>Integrasi &amp; penyaringan</b> — {@link #getFeeder()}/{@link #setFeeder(Long)},
 *       {@link #getAktif()}/{@link #setAktif(Boolean)}.</li>
 *   <li><b>Penanda perilaku (turunan nama)</b> — {@link #getPindahan()}/{@link #setPindahan(Boolean)},
 *       {@link #getAlihProdi()}/{@link #setAlihProdi(Boolean)}.</li>
 *   <li><b>Jejak audit</b> — {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()} beserta setter-nya dan callback {@code onUpdate()}.</li>
 *   <li><b>Representasi</b> — {@link #toString()}.</li>
 * </ul>
 *
 * <h3>Hal non-obvious</h3>
 * <ul>
 *   <li><b>Tiga getter menulis balik ke field.</b> {@link #getFeeder()} (default {@code 1}),
 *       {@link #getPindahan()} dan {@link #getAlihProdi()} (deduksi dari nama) tidak sekadar
 *       membaca — mereka menugaskan hasilnya kembali ke field. Karena pemetaan memakai akses
 *       properti (anotasi ada pada getter) dan {@code dynamicUpdate = true}, nilai hasil tulis-balik
 *       itu IKUT ter-UPDATE ke basis data pada flush pertama setelah entity dibaca, tanpa ada aksi
 *       pengguna. {@link #getAktif()} dan {@link #getNama()} sebaliknya hanya mengembalikan nilai
 *       olahan tanpa menulis balik. Tidak ada getter yang menutup session Hibernate maupun bersifat
 *       destruktif di kelas ini.</li>
 *   <li><b>{@code aktif} yang berbeda arti antara grid dan combobox.</b> {@link #getAktif()}
 *       menganggap {@code null} sebagai {@code true}, jadi baris lama tampil "tercentang" di grid;
 *       tetapi tujuh pemakai yang menyaring dengan {@code Restrictions.eq("aktif", true)}
 *       ({@code TampilStudiMahasiswaHelper} dan enam laporan rekap host-to-host) TIDAK akan
 *       mengembalikan baris ber-{@code aktif} {@code null} — SQL {@code = true} tidak cocok dengan
 *       {@code NULL}. Akibatnya sebuah baris bisa terlihat aktif di layar master namun hilang dari
 *       dropdown, sampai seseorang men-toggle checkbox-nya sekali (yang menulis nilai eksplisit).
 *       Perlu dicatat pula bahwa mayoritas dropdown status awal di aplikasi sama sekali tidak
 *       menyaring {@code aktif} — kolom ini praktis hanya berpengaruh di tujuh tempat itu.</li>
 *   <li><b>Kolom "Keterangan" pada layar tidak tersimpan.</b> Grid ZUL punya kolom Keterangan dan
 *       {@code onSave()} memanggil {@code setKeterangan(...)}, tetapi properti {@code keterangan}
 *       hanya ada di {@link ais.database.model.GeneralValueObject} dan TIDAK dideklarasikan ulang di
 *       kelas ini. Karena {@code GeneralValueObject} bukan {@code @Entity}/{@code @MappedSuperclass},
 *       Hibernate tidak memetakan properti induk, sehingga isian Keterangan hanya hidup di memori
 *       objek dan hilang setelah request selesai; kolom di grid selalu tampil kosong.</li>
 *   <li><b>Tidak ada batasan keunikan di basis data.</b> Keunikan {@code nama} hanya divalidasi di
 *       {@code StatusAwalMahasiswaAction.checkNamaStatusAwalMahasiswa()} (SELECT COUNT sebelum
 *       simpan), dan {@code kode} tidak diperiksa sama sekali. Jalur tulis non-ZK — impor Feeder,
 *       auto-seed {@code InitDataHelper} — bisa menambah duplikat.</li>
 *   <li><b>{@code kode} boleh string kosong.</b> {@code @Column(nullable = false)} hanya melarang
 *       {@code NULL}; baris hasil auto-seed sengaja diisi {@code ""}. Validasi "wajib isi" pada kode
 *       dan Feeder hanya ada di layar ZK.</li>
 *   <li><b>Penamaan kolom tidak seragam.</b> Hanya {@code id}, {@code nama}, dan {@code kode} yang
 *       punya {@code @Column}. {@code feeder}, {@code aktif}, {@code pindahan}, dan {@code alihProdi}
 *       memakai {@code MyNamingStrategy} (turunan {@code DefaultNamingStrategy}) yang mengembalikan
 *       nama properti apa adanya, sehingga nama kolom untuk yang terakhir bersifat camelCase
 *       ({@code alihProdi}), bukan {@code alih_prodi} seperti kolom foreign key sejenis di tabel
 *       lain.</li>
 *   <li><b>Field {@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah} sengaja
 *       dideklarasikan ULANG</b> walaupun sudah ada di {@link ais.database.model.GeneralValueObject}.
 *       Ini bukan duplikasi keliru melainkan keharusan teknis: induknya POJO abstrak biasa sehingga
 *       properti audit harus dinyatakan lagi di sini agar Hibernate dan Envers memetakannya.</li>
 *   <li><b>Kloning ke jenjang sekolah.</b> {@code InitDataHelper} menyalin seluruh baris entity ini
 *       menjadi baris {@code StatusAwalSiswa} bila tabel siswa masih kosong, dengan
 *       {@code kode = getFeeder() + ""}. Karena {@link #getFeeder()} mengembalikan {@code 1} untuk
 *       nilai kosong, baris yang belum pernah diberi kode Feeder akan menghasilkan
 *       {@code StatusAwalSiswa} ber-kode "1" semuanya.</li>
 * </ul>
 *
 * <p>Anotasi kelas: {@code @Entity} + {@code @org.hibernate.annotations.Entity(dynamicInsert,
 * dynamicUpdate)} sehingga hanya kolom yang berubah yang dikirim ke basis data, dan {@code @Audited}
 * sehingga setiap perubahan lewat session Hibernate direkam Envers ke tabel bayangan (perubahan
 * lewat SQL native TIDAK terekam).</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.KelompokMahasiswa
 * @see Mahasiswa
 * @see HistoryStatusMahasiswa
 * @see BiodataCalonMahasiswa
 * @see StatusAwalSiswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "status_awal_mahasiswa")

public class StatusAwalMahasiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya dikunci agar objek yang sudah ter-serialisasi (mis. di
	 * session ZK atau cache) tetap kompatibel walau kelas ini ditambahi anggota baru.
	 */
	private static final long serialVersionUID = 2461821577548439808L;

	/**
	 * Kunci primer baris, dipetakan ke kolom {@code id} dengan strategi {@code IDENTITY}
	 * (sequence/serial PostgreSQL). Dideklarasikan ulang dari
	 * {@link ais.database.model.GeneralValueObject} karena induknya tidak dipetakan Hibernate.
	 * Lihat {@link #getId()}.
	 */
	private Long id;

	/**
	 * Nama pengguna terakhir yang mengubah baris ini, diisi otomatis oleh mekanisme audit AIS.
	 * Lihat {@link #getOleh()}.
	 */
	private String oleh;

	/**
	 * Id pengguna terakhir yang mengubah baris ini (pasangan teknis dari {@link #oleh}).
	 * Lihat {@link #getOlehId()}.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah tersentuh mekanisme audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna terakhir yang mengubah baris ini.
	 *
	 * <p><b>Perhatikan:</b> nilai {@code null} maupun string kosong/spasi DIABAIKAN — field tetap
	 * memegang nilai lamanya. Ini disengaja agar jejak audit yang sudah ada tidak terhapus oleh
	 * pemanggil yang kebetulan tidak punya konteks pengguna (mis. proses terjadwal atau impor).</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong diabaikan sehingga
	 * jejak audit lama dipertahankan.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum UPDATE baris ini dikirim ke
	 * basis data, lalu mendelegasikan pembaruan stempel waktu ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}.
	 *
	 * <p><b>Efek samping:</b> mengubah {@link #tanggal_dirubah} (dan, tergantung implementasi
	 * interceptor, kolom {@code oleh}/{@code olehId}) pada objek yang sedang di-flush. Tidak pernah
	 * dipanggil manual dari kode aplikasi; juga tidak berjalan untuk INSERT maupun untuk perubahan
	 * lewat SQL native.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}

	/**
	 * Stempel waktu perubahan terakhir. Diinisialisasi ke waktu server saat objek dibuat
	 * ({@code WaktuUtil.getDate()}) dan diperbarui oleh {@link #onUpdate()} setiap kali baris
	 * di-UPDATE lewat session Hibernate. Lihat {@link #getTanggal_dirubah()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya tidak perlu dipanggil manual: {@link #onUpdate()} sudah mengurusnya. Menyetel nilai
	 * di sini akan tertimpa kembali oleh callback tersebut pada flush berikutnya.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan yang ingin dicatat
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir, dipetakan sebagai {@code TIMESTAMP}.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang dibuat lewat
	 *         konstruktor kelas ini, karena field-nya sudah diinisialisasi saat deklarasi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini dalam bentuk {@code "<id>-<nama>"}.
	 *
	 * <p>Dipakai antara lain oleh {@code ais.database.model.KelompokMahasiswa} (yang menyalin hasil
	 * {@code toString()} status awal ke ringkasan barisnya) dan oleh keluaran debug impor Feeder
	 * ({@code FeederImporter} mencetak objek ini langsung ke konsol).</p>
	 *
	 * <p><b>Catatan:</b> method ini membaca FIELD {@link #nama} secara langsung, bukan lewat
	 * {@link #getNama()}. Jadi hasilnya (a) tidak dipangkas spasinya dan (b) bisa berbentuk
	 * {@code "12-null"} untuk baris yang namanya belum terisi, atau {@code "null-Baru"} untuk objek
	 * baru yang belum tersimpan.</p>
	 *
	 * @return gabungan id dan nama dipisah tanda hubung
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Kode singkat status awal, dipetakan ke kolom {@code kode} ({@code varchar(50)},
	 * {@code NOT NULL}). Wajib diisi menurut validasi layar ZK, tetapi boleh string kosong dan
	 * memang diisi {@code ""} oleh baris hasil auto-seed. Lihat {@link #getKode()}.
	 */
	private String kode;

	/**
	 * Nama status awal seperti yang dilihat pengguna ("Baru", "Pindahan", "Alih Prodi", ...),
	 * dipetakan ke kolom {@code nama} ({@code varchar(255)}, {@code NOT NULL}).
	 *
	 * <p>Secara de facto ini adalah KUNCI ALAMI entity: pengikatan konstanta global di
	 * {@code InitDataHelper}, pencocokan impor Feeder, serta deduksi {@link #getPindahan()} dan
	 * {@link #getAlihProdi()} semuanya bekerja berdasarkan isi kolom ini. Lihat {@link #getNama()}.</p>
	 */
	private String nama;

	/**
	 * Kode jalur masuk versi Feeder/PDDikti ({@code id_jns_daftar}). Tanpa {@code @Column} sehingga
	 * memakai nama properti apa adanya. Lihat {@link #getFeeder()}.
	 */
	private Long feeder;

	/**
	 * Penanda baris masih boleh dipilih. Hanya dihormati oleh sebagian kecil pemakai; lihat
	 * pembahasan pada {@link #getAktif()}.
	 */
	private Boolean aktif;

	/**
	 * Penanda bahwa status awal ini berarti "mahasiswa pindahan dari perguruan tinggi lain".
	 * Nilainya bisa dipaksa dari nama; lihat {@link #getPindahan()}.
	 */
	private Boolean pindahan;

	/**
	 * Penanda bahwa status awal ini berarti "alih program studi di dalam perguruan tinggi yang sama".
	 * Nilainya bisa dipaksa dari nama; lihat {@link #getAlihProdi()}.
	 */
	private Boolean alihProdi;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA, sekaligus dipakai layar master saat menekan
	 * tombol Tambah dan oleh pengurai Feeder ({@code FeederConverter.statusAwalMahasiswa(Node)})
	 * serta auto-seed {@code InitDataHelper}.
	 */
	public StatusAwalMahasiswa() {
	}

	/**
	 * Konstruktor ringkas yang hanya mengisi kunci primer, untuk membentuk objek "stub" tanpa
	 * memuat barisnya dari basis data.
	 *
	 * <p>Satu-satunya pemakai di codebase adalah {@code MahasiswaAction}, yang membangun stub
	 * seperti ini lalu menyerahkannya ke {@code Common.selectComboItem(...)} — helper tersebut
	 * memilih item combobox dengan membandingkan {@link #getId()}, jadi properti lain tidak
	 * diperlukan.</p>
	 *
	 * <p><b>Jangan</b> menyimpan objek hasil konstruktor ini lewat {@code saveOrUpdate}: seluruh
	 * kolom lain masih {@code null}, sehingga baris nyata di basis data bisa tertimpa kosong.</p>
	 *
	 * @param id kunci primer baris yang ingin diwakili
	 */
	public StatusAwalMahasiswa(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kunci primer baris ini.
	 *
	 * <p>Selain sebagai identitas persistence, nilai ini dipakai luas sebagai pembanding kesetaraan
	 * status awal ({@code JenisDiskonMahasiswa}, {@code SyaratUjian}) dan disisipkan langsung ke SQL
	 * native pada banyak dasbor ({@code and a.status_awal_mahasiswa = <id>}).</p>
	 *
	 * @return id baris, atau {@code null} untuk objek yang belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci primer baris ini. Umumnya hanya dipanggil Hibernate; kode aplikasi cukup
	 * memakai {@link #StatusAwalMahasiswa(Long)}.
	 *
	 * @param id kunci primer baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama status awal, sudah dipangkas spasi depan/belakangnya.
	 *
	 * <p>Pemangkasan dilakukan HANYA pada nilai yang dikembalikan — field {@link #nama} tidak ikut
	 * ditulis ulang, jadi nilai yang tersimpan di basis data tetap apa adanya. Akibat yang perlu
	 * disadari: perbandingan nama lewat getter ini bisa berbeda hasil dengan perbandingan lewat SQL
	 * atau lewat {@link #toString()} yang membaca field mentah.</p>
	 *
	 * @return nama status awal tanpa spasi tepi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama status awal apa adanya (tanpa pemangkasan; layar master sudah memangkasnya
	 * sendiri sebelum memanggil method ini).
	 *
	 * <p><b>Berdampak luas.</b> Nama adalah kunci alami entity ini: mengubahnya dapat memutus
	 * pengikatan {@code ConstantValues.BARU}/{@code PINDAHAN}/{@code ALIH_PRODI} pada start
	 * berikutnya, mengubah hasil pencocokan impor Feeder, dan — bila nama barunya mengandung
	 * "pindahan" atau "alih prodi" — memaksa {@link #getPindahan()}/{@link #getAlihProdi()}
	 * mengembalikan {@code true}.</p>
	 *
	 * @param nama nama status awal yang baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Menetapkan kode singkat status awal.
	 *
	 * <p>Tidak ada validasi keunikan maupun format di level entity; layar master hanya memastikan
	 * kode tidak kosong, sedangkan impor Feeder mengisinya dengan kode Feeder yang di-stringkan.</p>
	 *
	 * @param kode kode baru; disimpan apa adanya
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan kode singkat status awal, dipetakan ke kolom {@code kode}.
	 *
	 * <p>Berbeda dengan {@link #getNama()}, getter ini <b>tidak</b> memangkas spasi — asimetri yang
	 * perlu diingat saat membandingkan kode antar-sumber data.</p>
	 *
	 * @return kode status awal; bisa string kosong (baris hasil auto-seed), namun tidak pernah
	 *         {@code NULL} pada baris yang tersimpan karena kolomnya {@code NOT NULL}
	 */
	@Column(name = "kode", nullable = false, length = 50)
	public String getKode() {
		return kode;
	}

	/**
	 * Mengembalikan kode jalur masuk versi Feeder/PDDikti ({@code id_jns_daftar}).
	 *
	 * <p><b>Getter yang MENULIS BALIK.</b> Bila field masih {@code null}, method ini mengisi field
	 * dengan {@code 1L} lebih dulu, lalu mengembalikannya. Karena pemetaan memakai akses properti dan
	 * kelas ini {@code dynamicUpdate}, nilai {@code 1} tersebut ikut ter-UPDATE ke kolom
	 * {@code feeder} pada flush berikutnya — cukup dengan membuka layar yang menampilkan baris ini,
	 * tanpa pengguna menyimpan apa pun. Jadi kolom {@code feeder} praktis tidak pernah bertahan
	 * kosong.</p>
	 *
	 * <p>Konsekuensinya: baris yang belum pernah dipetakan ke Feeder akan diam-diam berperilaku
	 * seperti jalur masuk ber-kode 1 saat ekspor {@code EksporMahasiswaFeeder} maupun saat
	 * dicocokkan {@code FeederUtil.getDataByFeeder(...)}. {@code InitDataHelper} juga memakai nilai
	 * ini sebagai {@code kode} ketika mengkloning master ini menjadi {@code StatusAwalSiswa}.</p>
	 *
	 * @return kode Feeder; tidak pernah {@code null}
	 */
	public Long getFeeder() {
		if (feeder == null) {
			feeder = 1L;
		}
		return feeder;
	}

	/**
	 * Menetapkan kode jalur masuk versi Feeder/PDDikti.
	 *
	 * <p>Dipanggil dari layar master (kotak isian "Feeder", wajib diisi) dan dari pengurai
	 * {@code FeederConverter} saat membaca elemen {@code id_jns_daftar}. Perlu diingat: impor
	 * referensi Feeder juga bisa mengubah kolom ini lewat UPDATE SQL native massal tanpa melewati
	 * setter ini, sehingga perubahan tersebut tidak terekam Envers.</p>
	 *
	 * @param feeder kode Feeder baru
	 */
	public void setFeeder(Long feeder) {
		this.feeder = feeder;
	}

	/**
	 * Mengembalikan penanda "masih aktif dipakai", dengan {@code null} dianggap {@code true}.
	 *
	 * <p>Berbeda dengan {@link #getFeeder()}, method ini <b>tidak</b> menulis balik nilai default ke
	 * field: kolom {@code aktif} tetap {@code NULL} di basis data. Perbedaan halus inilah yang
	 * menimbulkan ketidakcocokan yang sudah dijelaskan pada Javadoc kelas — grid master menampilkan
	 * checkbox tercentang (memakai getter ini) sementara dropdown yang menyaring dengan
	 * {@code Restrictions.eq("aktif", true)} justru menyembunyikan baris yang sama, karena di SQL
	 * {@code NULL = true} tidak pernah benar.</p>
	 *
	 * <p>Penyaring {@code aktif} hanya dipakai di tujuh tempat ({@code TampilStudiMahasiswaHelper}
	 * dan enam jendela laporan rekap host-to-host); puluhan dropdown status awal lainnya menampilkan
	 * seluruh baris tanpa memandang kolom ini, jadi menonaktifkan sebuah status awal TIDAK
	 * menyembunyikannya dari sebagian besar layar.</p>
	 *
	 * @return {@code true} bila aktif atau belum pernah ditentukan; {@code false} bila sengaja
	 *         dinonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan penanda aktif.
	 *
	 * <p>Dipanggil dari listener {@code onCheck} checkbox "Aktif" di grid master, yang langsung
	 * menyusulinya dengan {@code Common.refreshSaveOrUpdate(...)} sehingga perubahan tersimpan
	 * seketika tanpa dialog konfirmasi. Checkbox tersebut dinonaktifkan bila pengguna tidak punya
	 * hak {@code CommonPrivilages.UPDATE}.</p>
	 *
	 * @param aktif {@code true} untuk mengaktifkan, {@code false} untuk menonaktifkan
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan penanda "status awal ini berarti mahasiswa PINDAHAN".
	 *
	 * <p><b>Getter yang MENULIS BALIK dan bersifat memaksa.</b> Sebelum membaca field, method ini
	 * memeriksa {@link #getNama()}: bila nama (huruf kecil, sudah dipangkas) MENGANDUNG substring
	 * {@code "pindahan"}, field {@link #pindahan} ditugasi {@code true}. Hasilnya kemudian
	 * dikembalikan dengan {@code null} dianggap {@code false}.</p>
	 *
	 * <p>Dua konsekuensi nyata yang perlu diketahui sebelum menyentuh data:</p>
	 * <ul>
	 *   <li>Karena penugasan itu terjadi pada objek terkelola dan kelas ini {@code dynamicUpdate},
	 *       nilai {@code true} tersebut IKUT tersimpan ke basis data pada flush berikutnya —
	 *       cukup dengan menampilkan baris tersebut di grid.</li>
	 *   <li>Untuk baris yang namanya mengandung "pindahan", checkbox "Pindahan" di layar master
	 *       efektif <b>tidak bisa dimatikan</b>: {@code setPindahan(false)} memang menulis
	 *       {@code false} ke field, tetapi pembacaan berikutnya (termasuk oleh Hibernate saat
	 *       flush) langsung memaksanya kembali menjadi {@code true}. Satu-satunya cara mematikannya
	 *       adalah mengubah namanya.</li>
	 * </ul>
	 *
	 * <p>Nilai ini merambat jauh: {@code Mahasiswa.getMerupakanPindahan()} dan
	 * {@code BiodataCalonMahasiswa} menurunkannya dari sini, dan hasilnya menentukan antara lain
	 * kolom mana yang wajib diisi di layar mahasiswa (kampus asal, prodi asal, NIM lama, SKS diakui,
	 * tanggal pindah), pilihan {@code sks_diakui} saat ekspor Feeder, aturan validasi di
	 * {@code MahasiswaExistingBusinessRules}, sampai digit kedua NIM pada
	 * {@code pmb.nim.IsmNimGenerator}.</p>
	 *
	 * @return {@code true} bila baris ini menandakan mahasiswa pindahan; tidak pernah {@code null}
	 */
	public Boolean getPindahan() {
		if (getNama() != null && getNama().trim().toLowerCase().contains("pindahan")) {
			pindahan = true;
		}
		return pindahan == null ? false : pindahan;
	}

	/**
	 * Menetapkan penanda "mahasiswa pindahan".
	 *
	 * <p>Dipanggil dari listener {@code onCheck} checkbox "Pindahan" di grid master, diikuti
	 * {@code Common.refreshSaveOrUpdate(...)}. Ingat batasan pada {@link #getPindahan()}: untuk
	 * baris yang namanya mengandung "pindahan", menyetel {@code false} di sini tidak akan bertahan.</p>
	 *
	 * @param pindahan nilai penanda yang diinginkan
	 */
	public void setPindahan(Boolean pindahan) {
		this.pindahan = pindahan;
	}

	/**
	 * Mengembalikan penanda "status awal ini berarti ALIH PROGRAM STUDI".
	 *
	 * <p>Perilakunya kembar dengan {@link #getPindahan()}: bila {@link #getNama()} mengandung
	 * substring {@code "alih prodi"} (huruf kecil), field {@link #alihProdi} ditugasi {@code true}
	 * dan nilai itu ikut tersimpan pada flush berikutnya karena kelas ini {@code dynamicUpdate}.
	 * {@code null} dianggap {@code false}.</p>
	 *
	 * <p>Perhatikan pencocokannya memakai spasi: baris bernama "Alih-Prodi" atau "AlihProdi" TIDAK
	 * terdeteksi, sedangkan "Pindahan Alih Prodi" akan memicu {@link #getPindahan()} sekaligus
	 * method ini menjadi {@code true} bersamaan — kondisi yang tidak diantisipasi pemakai hilirnya,
	 * yang umumnya menuliskan {@code if (pindahan) ... else if (alihProdi) ...} sehingga cabang alih
	 * prodi tidak pernah dijalankan (mis. pemilihan {@code sks_diakui} di
	 * {@code FeederExporterGenerator}).</p>
	 *
	 * <p>Konsumen utama: {@code Mahasiswa.getMerupakanAlihProdi()}, yang mewajibkan pengisian
	 * tanggal pindah prodi di layar mahasiswa dan di {@code MahasiswaExistingBusinessRules}.</p>
	 *
	 * @return {@code true} bila baris ini menandakan alih program studi; tidak pernah {@code null}
	 */
	public Boolean getAlihProdi() {
		if (getNama() != null && getNama().trim().toLowerCase().contains("alih prodi")) {
			alihProdi = true;
		}
		return alihProdi == null ? false : alihProdi;
	}

	/**
	 * Menetapkan penanda "alih program studi".
	 *
	 * <p>Dipanggil dari listener {@code onCheck} checkbox "Alih Prodi" di grid master, diikuti
	 * {@code Common.refreshSaveOrUpdate(...)}. Sama seperti {@link #setPindahan(Boolean)}, nilai
	 * {@code false} tidak akan bertahan untuk baris yang namanya mengandung "alih prodi".</p>
	 *
	 * @param alihProdi nilai penanda yang diinginkan
	 */
	public void setAlihProdi(Boolean alihProdi) {
		this.alihProdi = alihProdi;
	}
}
