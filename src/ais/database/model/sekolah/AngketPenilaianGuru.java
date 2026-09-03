package ais.database.model.sekolah;

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
import ais.database.model.GeneralValueObject;

/**
 * Entitas Hibernate untuk tabel {@code sekolah.angket_penilaian_guru} — <b>kepala (header)
 * satu instrumen angket penilaian guru</b> pada modul jenjang sekolah. Satu baris di sini
 * mendefinisikan sebuah angket secara utuh: kodenya, namanya, petunjuk pengisiannya, lebar
 * skala jawabannya, siapa yang boleh/harus mengisinya, dan — yang paling penting —
 * <b>kepada peserta didik mana angket itu ditawarkan</b>.
 *
 * <h2>VERIFIKASI: ini "JENIS ANGKET", BUKAN "periode angket"</h2>
 * <p>Nama kelasnya mudah disalahartikan sebagai wadah periode (per semester/tahun ajaran) yang
 * menaungi grup checklist. Pemeriksaan langsung atas kode menunjukkan <b>bukan</b>:</p>
 * <ul>
 *   <li>Tidak ada satu pun kolom periode pada entity ini — tak ada {@code semester},
 *       {@code tahunAjaran}, {@code tanggalMulai}/{@code tanggalSelesai}, maupun
 *       {@code aktif}. Satu-satunya kolom bernuansa waktu adalah {@code angkatan}, dan itu pun
 *       <b>tahun masuk siswa</b> (kohort responden), bukan periode berlakunya angket.</li>
 *   <li>Label menu resminya adalah <b>"Jenis Angket"</b> ({@code MenuSnapshotData} id
 *       {@code 570701}, di bawah menu induk "Angket Siswa" id {@code 5707}), bersanding dengan
 *       "Grup Angket" ({@code 570702}) dan "Angket Siswa" ({@code 570703}). Judul tab pada
 *       {@code angket_penilaian_guru.zul} adalah "Angket Penilaian", dan judul dialognya
 *       "Tambah/Ubah Angket Penilaian Guru".</li>
 *   <li>Penjadwalan <b>kapan</b> angket harus diisi memang ada di sistem, tetapi tersimpan di
 *       {@link ais.database.model.JadwalChecklistPenilaianUmum} yang digantungkan ke
 *       <i>grup</i> ({@link GrupChecklistPenilaianGuru}), bukan ke baris ini. Gerbang "ada
 *       angket yang harus diisi" ({@code ais.common.ChecklistPenilaianHelper}) pun menjoin
 *       lewat kolom {@code jadwal_checklist_penilaian_umum.grup_checklist_penilaian_guru}.</li>
 * </ul>
 * <p>Jadi peran entity ini adalah <b>definisi + cakupan</b>, bukan periode. Ia berumur panjang:
 * sekali dibuat, dipakai terus-menerus lintas semester sampai diubah/dihapus admin.</p>
 *
 * <h2>Posisi dalam rantai angket guru (4 lapis)</h2>
 * <ol>
 *   <li><b>{@code AngketPenilaianGuru} (kelas ini)</b> — header/jenis angket. <b>Seluruh
 *       cakupan tenant dan audiens rantai ini ada di sini</b>: {@link #getYayasan()},
 *       {@link #getSekolah()}, {@link #getProgram()}, {@link #getAngkatan()},
 *       {@link #getUntukSiswa()}/{@link #getUntukGuru()}, ditambah parameter tampilan
 *       {@link #getJumlahPilihan()}, {@link #getPetunjuk()}, dan
 *       {@link #getTampilKeterangan()}.</li>
 *   <li>{@link GrupChecklistPenilaianGuru} — kelompok/aspek penilaian (mis. "Kedisiplinan"),
 *       dengan saklar {@code aktif} sendiri. FK-nya ke kelas ini
 *       ({@code angket_penilaian_guru}) <b>nullable</b>.</li>
 *   <li>{@link ChecklistPenilaianGuru} — butir pertanyaan yang dijawab dengan skala radio
 *       {@code 1..N}, di mana {@code N} = {@link #getJumlahPilihan()} milik kelas ini.</li>
 *   <li>{@link ChecklistBaruPenilaianGuruOlehSiswa} — baris transaksi jawaban: seluruh jawaban
 *       satu siswa atas satu guru pada satu jadwal pelajaran.</li>
 * </ol>
 * <p><b>Konsekuensi struktural yang perlu dipahami:</b> lapis 2, 3, dan 4 sama sekali tidak
 * punya kolom {@code sekolah}/{@code yayasan}. Setiap penyaringan tenant di seluruh rantai
 * dilakukan dengan join naik ke baris <i>kelas ini</i>
 * ({@code butir → grup → angket}). Kekeliruan pada satu baris di sini karena itu merambat ke
 * seluruh pohon di bawahnya, termasuk ke pertanyaan yang muncul di layar siswa sekolah lain.
 * Lapis 4 bahkan tidak terhubung ke sini sama sekali (jawaban hanya menyimpan id butir di
 * dalam blob teks), sehingga <b>data jawaban tidak punya cakupan tenant apa pun</b> — lihat
 * catatan pada {@link ChecklistBaruPenilaianGuruOlehSiswa}.</p>
 *
 * <h2>Domain: angket guru DIISI OLEH SISWA</h2>
 * <p>Sama seperti yang sudah diverifikasi pada {@link GrupChecklistPenilaianGuru}: meski
 * namanya "penilaian kinerja guru", ini <b>bukan</b> instrumen supervisi kepala sekolah,
 * melainkan kuesioner umpan balik yang diisi <b>siswa</b> atas guru yang mengajarnya. Layar
 * pengisiannya ({@code ais.action.master.helper.generic.AngketGuruWindow}) dibuka dari portal
 * siswa dan menyaring grup memakai identitas siswa yang sedang login.</p>
 *
 * <h2>Pemakai terverifikasi</h2>
 * <ul>
 *   <li>{@code ais.action.master.sekolah.AngketPenilaianGuruAction} — layar master CRUD
 *       (menu "Jenis Angket"), sekaligus tuan rumah tab "Angket Umum".</li>
 *   <li>{@code ais.action.master.helper.generic.AngketGuruWindow} — formulir pengisian siswa;
 *       memakai {@link #getPetunjuk()} (blok petunjuk sekali per angket),
 *       {@link #getJumlahPilihan()} (jumlah radio per pertanyaan), {@link #getIsi()} (judul
 *       section: {@code angket.getIsi() + " - " + grup.getIsi()}), dan
 *       {@link #getTampilKeterangan()}.</li>
 *   <li>{@code ais.action.servlet.api.AngketUtilApi} — endpoint mobile; menyaring grup lewat
 *       alias {@code angketPenilaianGuru} (yayasan, sekolah, program, angkatan,
 *       {@code untukSiswa}).</li>
 *   <li>{@code ais.common.AngketUtil} — pemeriksa "masih ada angket yang belum diisi" pada
 *       jalur login siswa; salinan filter yang sama.</li>
 *   <li>{@code ais.common.ChecklistPenilaianGuruHelper} — gerbang pengingat pengisian.</li>
 *   <li>{@code ais.action.master.sekolah.GrupChecklistPenilaianGuruAction} /
 *       {@code ChecklistPenilaianGuruAction} — combo "Nama Angket" dan filter pencarian di dua
 *       layar master turunannya.</li>
 *   <li>{@code ais.common.InitData} — tipe ini termasuk {@code initClasses} yang dipra-muat
 *       sebagai data master awal.</li>
 * </ul>
 *
 * <h2>VERIFIKASI ANONIMITAS (kaitan dengan temuan privasi lapis jawaban)</h2>
 * <p>Pertanyaan yang perlu dijawab dari sisi header: <b>apakah entity ini punya penanda yang
 * menentukan apakah identitas siswa penilai ditampilkan atau disembunyikan?</b>
 * Jawabannya <b>TIDAK ADA</b>. Seluruh 12 properti bisnis kelas ini sudah ditelusuri satu per
 * satu dan tak satu pun berkaitan dengan anonimitas responden:</p>
 * <ul>
 *   <li>{@code untukSiswa}/{@code untukGuru} menentukan <b>siapa respondennya</b>, bukan
 *       apakah respondennya disembunyikan.</li>
 *   <li>{@code tampilKeterangan} menentukan apakah kotak teks bebas "Keterangan tambahan"
 *       dirender <b>per pertanyaan</b> pada formulir siswa — bukan penanda anonimitas. Justru
 *       sebaliknya: teks bebas adalah kolom yang paling mungkin membuat responden
 *       <i>mengidentifikasi dirinya sendiri</i>. Lihat {@link #getTampilKeterangan()} untuk
 *       kuirk penting: pada varian guru penanda ini <b>tidak dapat dinyalakan lewat UI mana
 *       pun</b>.</li>
 *   <li>Tidak ada kolom semacam {@code anonim}, {@code tampilkanPengisi},
 *       {@code sembunyikanIdentitas}, atau sejenisnya — baik di kelas ini maupun di ketiga
 *       lapis di bawahnya.</li>
 * </ul>
 * <p><b>Artinya anonimitas angket guru sama sekali tidak dapat dikonfigurasi.</b> Ia
 * sepenuhnya ditentukan oleh cara tiap konsumen <i>memilih</i> merender data jawaban. Tidak
 * ada satu pun tombol yang bisa dimatikan administrator bila identitas penilai ternyata bocor
 * di suatu layar; perbaikannya harus di sisi kode konsumen. Ini konteks penting bagi temuan
 * kebocoran identitas penilai pada dasbor rekap
 * ({@code ais.action.report.format1.akademik.LaporanAngketGuruDashboardWindow}) yang dicatat
 * bersama {@link ChecklistBaruPenilaianGuruOlehSiswa}: <b>tidak ada mitigasi konfigurasi yang
 * tersedia dari sisi entity induk ini</b>.</p>
 *
 * <h2>Kuirk, jebakan, dan temuan bagi pembaca kode</h2>
 * <ul>
 *   <li><b>Deklarasi ulang {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} BUKAN
 *       bug.</b> {@link GeneralValueObject} adalah POJO abstrak biasa — bukan {@code @Entity}
 *       maupun {@code @MappedSuperclass} — sehingga Hibernate tidak memetakan properti
 *       induknya sama sekali. Setiap entity konkret <b>wajib</b> mendeklarasikan ulang keempat
 *       properti audit itu agar terpetakan.</li>
 *   <li><b>Dua getter memicu penulisan ke basis data.</b> {@link #getPetunjuk()} dan
 *       {@link #getJumlahPilihan()} memanggil {@link Common#getKonfigurasi(String, String)},
 *       yang bila baris konfigurasi belum ada akan <b>membuka session sendiri, INSERT, dan
 *       COMMIT</b> baris {@code Konfigurasi} baru. Sekadar merender daftar angket karena itu
 *       dapat menerbitkan tulisan ke DB pada instalasi baru. Detail pada masing-masing
 *       getter.</li>
 *   <li><b>Saklar "Untuk Guru" tidak melakukan apa-apa.</b> {@link #getUntukGuru()} hanya
 *       dibaca/ditulis oleh kotak centang di grid layar master dan ikut pada berkas
 *       cetak/impor. <b>Nol query</b> di seluruh repo yang menyaring memakainya — tidak ada
 *       jalur "guru menilai dirinya sendiri" yang benar-benar terpasang. Lihat
 *       {@link #getUntukGuru()}.</li>
 *   <li><b>Angkatan majemuk yang dijanjikan UI tidak bekerja.</b> Layar master menampilkan
 *       petunjuk "…masukkan tahun angkatan yang dipisahkan koma, contoh 2017,2018,2019",
 *       tetapi seluruh konsumen menyaring dengan {@code Restrictions.ilike("…angkatan",
 *       angkatan)} <b>tanpa {@code MatchMode}</b> — di Hibernate itu berarti pencocokan
 *       <b>persis</b>, bukan {@code %…%}. Nilai {@code "2017,2018,2019"} karena itu tidak akan
 *       pernah cocok dengan siswa angkatan {@code "2019"}. Lihat {@link #getAngkatan()}.</li>
 *   <li><b>{@code getKode()} menormalkan, {@code getIsi()} tidak.</b> Yang pertama mengubah
 *       {@code null} menjadi {@code ""} dan mem-{@code trim}; yang kedua mengembalikan field
 *       apa adanya (bisa {@code null}) meski dipetakan {@code nullable = false}.</li>
 *   <li><b>{@link #serialVersionUID}</b> bernilai sama dengan ratusan entity lain di repo ini
 *       (konstanta boilerplate salin-tempel), jadi bukan petunjuk kekerabatan.</li>
 *   <li>Padanan jenjang perguruan tinggi kelas ini adalah
 *       {@link ais.database.model.AngketPenilaianDosen} (dan varian lintas-modul
 *       {@link ais.database.model.AngketPenilaianUmum}). Bentuk kolomnya nyaris identik —
 *       warisan salin-tempel generator, bukan relasi database.</li>
 * </ul>
 *
 * <h2>Catatan keamanan (hasil verifikasi pola berulang)</h2>
 * <ul>
 *   <li><b>Gerbang hak akses layar master: ADA dan cukup benar.</b>
 *       {@code AngketPenilaianGuruAction.doBeforeCompose()} memanggil
 *       {@code Common.doCheckSecurity()}; {@code doAfterCompose()} memaksa logoff bila
 *       {@code READ} tidak dipenuhi; tombol Tambah digerbangi {@code CREATE}; tombol
 *       Ubah/Hapus per baris digerbangi {@code UPDATE}/{@code DELETE}; kedua kotak centang di
 *       grid memakai {@code setDisabled(!edit)}. Penghapusan meminta konfirmasi
 *       ({@code MyMessageboxConfig.OK|CANCEL}).</li>
 *   <li><b>Pewarisan hak lewat menu induk: ADA — dan layar ini sisi PEMBERINYA.</b>
 *       {@code angket_penilaian_guru.zul} punya entri menu sendiri ("Jenis Angket"), tetapi tab
 *       keduanya menyisipkan {@code /pages/master/angket_penilaian_umum.zul} lewat
 *       {@code MyInclude} dari {@code onAngketAngketUmum()}. Berkas itu <b>tidak punya entri
 *       menu sama sekali</b> pada {@code MenuSnapshotData}, sehingga {@code checkPrevilages()}
 *       yang dijalankan {@code AngketPenilaianUmumAction} menguji hak menu <i>induk</i> (Jenis
 *       Angket) ini. Siapa pun yang diberi hak kelola jenis angket guru otomatis memperoleh
 *       CRUD penuh atas master <b>angket UMUM lintas modul</b>. Instance baru dari pola yang
 *       sudah dikenal proyek ini, dan simetris dengan yang sudah dicatat pada
 *       {@link GrupChecklistPenilaianGuru}.</li>
 *   <li><b>Cakupan tenant pada layar master: FAIL-OPEN.</b> {@code initCriteria()} menyaring
 *       {@code sekolah}/{@code yayasan}/{@code program} <i>hanya bila</i> combo pencarian yang
 *       bersangkutan punya item terpilih; bila tidak, yang dipasang adalah
 *       {@code Restrictions.sqlRestriction("1=1")}. Untuk pengguna tanpa konteks sekolah aktif,
 *       <b>seluruh jenis angket milik semua sekolah/yayasan tampil dan dapat
 *       disunting/dihapus</b> — dan karena baris inilah pemegang cakupan seluruh rantai,
 *       kemampuan itu setara dengan mengubah instrumen penilaian guru sekolah lain.</li>
 *   <li><b>Penulisan balik pada objek persisten saat membuka dialog Ubah.</b>
 *       {@code AngketPenilaianGuruAction.init()} menjalankan
 *       {@code angketPenilaianGuru.setSekolah(tbmuser.ambilSekolah())},
 *       {@code setYayasan(tbmuser.ambilYayasan())}, dan {@code setProgram(...)} pada objek yang
 *       berasal dari {@code Criteria.list()} — jadi <b>persisten</b>. Karena dirty-checking
 *       Hibernate, sekadar <i>membuka</i> dialog (bahkan lalu menekan Batal) sudah dapat
 *       memindahkan kepemilikan angket ke sekolah pengguna yang membukanya. Baris ketiga
 *       kelompok itu juga salah kawal: syaratnya menguji {@code ambilYayasan() != null}
 *       sementara badannya memakai {@code ambilProgram()}, sehingga {@code program} ditimpa
 *       {@code ""} untuk pengguna yang punya yayasan tetapi tanpa program.</li>
 *   <li><b>Bug cakupan yang lebih berat: combo "Prodi" pada dialog selalu KOSONG.</b> Di
 *       {@code init()}, kombo sekolah sempat diisi
 *       ({@code Common.insertComboDanSemua(sekolah, …)}), tetapi beberapa baris kemudian field
 *       yang sama <b>ditimpa instance baru</b>: {@code Common.selectComboItem(sekolah = new
 *       Combobox(), …)}. {@code selectComboItem} hanya <i>memilih</i> item yang sudah ada; ia
 *       tidak mengisi daftar. Akibatnya kombo yang benar-benar tampil tidak punya satu pun
 *       item, {@code sekolah.getSelectedItem()} selalu {@code null}, dan {@code onSave()}
 *       menyimpan {@code setSekolah(null)}. <b>Setiap penyuntingan lewat dialog karena itu
 *       melebarkan angket menjadi berlaku untuk SEMUA sekolah</b> — pembatasan per sekolah
 *       praktis tidak dapat dipertahankan dari UI. Bandingkan dengan kombo "Yayasan" yang
 *       memang diisi {@code Common.insertCombo(...)} dan berfungsi normal.</li>
 *   <li><b>Auto-seed angket global.</b> {@code doAfterCompose()} pada layar master ini <i>dan</i>
 *       pada layar Grup membuat baris berkode {@code "001.000"} berjudul {@code "EVALUASI
 *       PENILAIAN PEMBELAJARAN"} bila tabel masih kosong — <b>tanpa</b> mengisi
 *       {@code yayasan}/{@code sekolah}, sehingga angket hasil semaian selalu berlaku GLOBAL.
 *       Penulisan itu terjadi dari jalur RENDER, bukan dari aksi simpan.</li>
 *   <li><b>Keunikan {@code kode} ditegakkan lintas tenant.</b>
 *       {@code checkNamaAngket()} menghitung baris ber-{@code kode} sama <b>tanpa filter
 *       sekolah/yayasan</b>. Satu sekolah dapat "memblokir" pemakaian sebuah kode oleh seluruh
 *       sekolah lain di instalasi yang sama.</li>
 *   <li><b>Whitelist Generic CRUD v2: NEGATIF (aman).</b> Halaman New UI
 *       {@code WEB-INF/new/sekolah/services/angket_penilaian_guru_service.jsp} mendeklarasikan
 *       {@code nuiServiceEntities = {AngketPenilaianGuru, Yayasan, Sekolah}}. Nama properti
 *       tenant entity ini persis {@code sekolah} dan {@code yayasan} — keduanya termasuk 12
 *       nama tetap {@code GenericCrudAutoEntityAdapter.scopeBindings()}, sehingga pola
 *       "cakupan tenant lolos karena nama properti di luar whitelist" <b>tidak</b> berlaku di
 *       sini.</li>
 *   <li><b>Getter destruktif yang mengubah kolom entity ini sendiri: TIDAK ADA.</b> Tidak ada
 *       getter yang menulis balik ke field-nya sendiri (pola {@code check()} pada
 *       {@link #getYayasan()}/{@link #getSekolah()} hanyalah resolusi proxy lazy, tidak mengubah
 *       nilai FK). Efek samping yang ada bersifat ke <i>tabel lain</i> (baris
 *       {@code Konfigurasi}), bukan ke baris angket.</li>
 * </ul>
 *
 * <p>Perubahan (create/update) tercatat historisnya lewat {@link Audited} (Hibernate Envers),
 * dan setiap update memperbarui {@link #getTanggal_dirubah()} lewat callback
 * {@link javax.persistence.PreUpdate} yang memanggil
 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see GrupChecklistPenilaianGuru
 * @see ChecklistPenilaianGuru
 * @see ChecklistBaruPenilaianGuruOlehSiswa
 * @see ais.database.model.AngketPenilaianDosen
 * @see ais.database.model.AngketPenilaianUmum
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sekolah", name = "angket_penilaian_guru")
public class AngketPenilaianGuru extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilainya identik dengan ratusan entity lain di repo ini
	 * (konstanta boilerplate hasil salin-tempel), jadi <b>bukan</b> petunjuk kekerabatan antar
	 * kelas.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/**
	 * Kunci utama baris ({@code id}, {@code bigserial}). Dideklarasikan ulang di sini karena
	 * {@link GeneralValueObject} tidak dipetakan Hibernate — lihat catatan pada Javadoc kelas.
	 */
	private Long id;

	/**
	 * Nama tampil pengguna terakhir yang mengubah baris ini (kolom {@code oleh}), diisi otomatis
	 * oleh {@link ais.database.hibernate.AuditTimestampInterceptor}. Dideklarasikan ulang atas
	 * alasan pemetaan yang sama dengan {@link #id}.
	 */
	private String oleh;

	/**
	 * Id/username pengguna terakhir yang mengubah baris ini (kolom {@code oleh_id}), pasangan
	 * teknis dari {@link #oleh}.
	 */
	private String olehId;

	/**
	 * Cap waktu perubahan terakhir (kolom {@code tanggal_dirubah}). Diinisialisasi ke waktu
	 * pembuatan objek lewat {@link ais.ui.util.WaktuUtil#getDate()} agar baris baru tidak pernah
	 * bernilai {@code null}, lalu diperbarui pada setiap update oleh {@link #onUpdate()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Kode angket (kolom {@code kode}, nullable di DB namun <b>wajib diisi</b> oleh layar
	 * master). Dipakai sebagai kunci bisnis: {@code checkNamaAngket()} menolak kode kembar, dan
	 * seluruh query grup mengurutkan hasilnya dengan {@code Order.asc("angketPenilaianGuru.kode")}
	 * sehingga kode inilah yang menentukan urutan tampil bagian-bagian angket di layar siswa.
	 * Angket hasil auto-seed berkode {@code "001.000"}.
	 */
	private String kode;

	/**
	 * Nama/judul angket (kolom {@code isi}, {@code text}, {@code nullable = false}), mis.
	 * {@code "EVALUASI PENILAIAN PEMBELAJARAN"}. Ditampilkan sebagai bagian pertama judul
	 * section pada formulir siswa ({@code angket.getIsi() + " - " + grup.getIsi()}).
	 */
	private String isi;

	/**
	 * Catatan bebas untuk administrator (kolom {@code keterangan}, {@code text}, opsional).
	 * Hanya tampil pada grid layar master dan berkas cetak/impor; <b>tidak</b> pernah dirender
	 * ke siswa. Jangan dikacaukan dengan {@link #petunjuk} (teks yang memang dibaca siswa)
	 * maupun dengan {@link #tampilKeterangan} (saklar kotak teks bebas per pertanyaan).
	 */
	private String keterangan;

	/**
	 * Petunjuk pengisian yang ditampilkan sekali di kepala formulir siswa (kolom
	 * {@code petunjuk}, {@code text}, opsional). Bila kosong, {@link #getPetunjuk()} jatuh ke
	 * nilai konfigurasi global — lihat getter-nya untuk efek samping penting.
	 */
	private String petunjuk;

	/**
	 * Banyaknya pilihan pada skala Likert tiap pertanyaan (kolom {@code jumlah_pilihan}). Nilai
	 * inilah yang menentukan berapa tombol radio {@code 1..N} dirender per butir
	 * {@link ChecklistPenilaianGuru}. Bila {@code null}/tidak masuk akal, dipakai default dari
	 * konfigurasi — lihat {@link #getJumlahPilihan()}.
	 */
	private Integer jumlahPilihan;

	/**
	 * Yayasan yang menjadi cakupan angket ini (FK {@code yayasan}, nullable). {@code null}
	 * berarti <b>berlaku untuk semua yayasan</b> — grid layar master merendernya sebagai
	 * "Semua". Bersama {@link #sekolah}, inilah satu-satunya pembatas tenant untuk seluruh
	 * rantai angket guru.
	 */
	private Yayasan yayasan;

	/**
	 * Sekolah yang menjadi cakupan angket ini (FK {@code sekolah}, nullable). {@code null}
	 * berarti <b>berlaku untuk semua sekolah</b>. Perhatikan bug kombo "Prodi" yang dicatat pada
	 * Javadoc kelas: penyuntingan lewat dialog master selalu mengembalikan kolom ini ke
	 * {@code null}.
	 */
	private Sekolah sekolah;

	/**
	 * Nama program/peminatan yang menjadi cakupan angket (kolom {@code program}, nullable,
	 * disimpan sebagai <b>teks nama program</b>, bukan FK). Kosong/{@code null} berarti berlaku
	 * untuk semua program.
	 */
	private String program;

	/**
	 * Tahun angkatan (tahun masuk) siswa yang menjadi cakupan angket (kolom {@code angkatan},
	 * nullable). Kosong berarti semua angkatan. Lihat {@link #getAngkatan()} untuk bug
	 * pencocokan angkatan majemuk.
	 */
	private String angkatan;

	/**
	 * Penanda bahwa angket ini ditujukan untuk diisi <b>siswa</b> (kolom {@code untuk_siswa}).
	 * Inilah satu-satunya penanda audiens yang benar-benar dipakai query; lihat
	 * {@link #getUntukSiswa()}.
	 */
	private Boolean untukSiswa;

	/**
	 * Penanda bahwa angket ini ditujukan untuk diisi <b>guru</b> (kolom {@code untuk_guru}).
	 * <b>Tidak pernah dipakai sebagai filter oleh kode mana pun</b> — lihat
	 * {@link #getUntukGuru()}.
	 */
	private Boolean untukGuru;

	/**
	 * Saklar "tampilkan kotak keterangan tambahan per pertanyaan" pada formulir siswa (kolom
	 * {@code tampil_keterangan}). <b>Bukan</b> penanda anonimitas; dan pada varian guru saklar
	 * ini tidak dapat dinyalakan dari UI mana pun — lihat {@link #getTampilKeterangan()}.
	 */
	private Boolean tampilKeterangan;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA untuk instansiasi entity. Dipakai
	 * juga oleh layar master saat menekan tombol Tambah ({@code onAdd()} membuat instance
	 * kosong) dan oleh jalur auto-seed {@code doAfterCompose()} yang membuat angket bawaan
	 * berkode {@code "001.000"} ketika tabel masih kosong.
	 */
	public AngketPenilaianGuru() {
	}

	/**
	 * Mengembalikan kunci utama baris. Bernilai {@code null} selama objek masih transient
	 * (belum tersimpan); layar master memanfaatkan hal itu untuk memilih judul dialog
	 * "Tambah Angket Penilaian Guru" vs "Ubah Angket Penilaian Guru", untuk memutuskan perlu
	 * tidaknya {@code session.load()} sebelum menyimpan, dan sebagai pengecualian pada
	 * pemeriksaan kode kembar {@code checkNamaAngket()}.
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/**
	 * Menetapkan kunci utama baris. Normalnya hanya dipanggil Hibernate setelah {@code INSERT}
	 * (kolom {@code insertable = false}, nilai dihasilkan sequence basis data); kode aplikasi
	 * tidak seharusnya memanggilnya sendiri.
	 *
	 * @param id id baris yang akan dipasang
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan id/username pengguna terakhir yang mengubah baris ini.
	 *
	 * @return isi kolom {@code oleh_id}, boleh {@code null} untuk baris yang belum pernah diaudit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id/username pengguna pengubah terakhir.
	 * <p>
	 * <b>Perilaku non-obvious:</b> setter ini <b>menolak secara senyap</b> nilai {@code null}
	 * maupun string kosong/berisi spasi — dalam kasus itu nilai lama dipertahankan, bukan
	 * ditimpa. Pola ini konsisten di seluruh entity repo agar jejak audit yang sudah ada tidak
	 * hilang ketika sebuah objek disalin/di-merge tanpa membawa konteks pengguna.
	 *
	 * @param olehId id pengguna pengubah; {@code null}/kosong diabaikan tanpa error
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengembalikan nama tampil pengguna terakhir yang mengubah baris ini.
	 *
	 * @return isi kolom {@code oleh}, boleh {@code null} untuk baris yang belum pernah diaudit
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menetapkan nama tampil pengguna pengubah terakhir. Sama seperti
	 * {@link #setOlehId(String)}, nilai {@code null}/kosong <b>diabaikan secara senyap</b>
	 * sehingga jejak audit lama tidak tertimpa.
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/kosong diabaikan tanpa error
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Callback JPA {@link javax.persistence.PreUpdate} yang dijalankan tepat sebelum setiap
	 * {@code UPDATE} baris ini. Mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}, yang memperbarui
	 * {@link #tanggal_dirubah} serta mengisi {@link #oleh}/{@link #olehId} dari konteks pengguna
	 * aktif.
	 * <p>
	 * <b>Efek samping penting:</b> karena callback ini terpasang, setiap perubahan yang
	 * terdeteksi dirty-checking Hibernate — termasuk penulisan balik {@code sekolah}/
	 * {@code yayasan}/{@code program} yang dilakukan {@code AngketPenilaianGuruAction.init()}
	 * saat dialog Ubah dibuka (lihat Javadoc kelas) — akan menghasilkan cap waktu baru
	 * <i>dan</i> revisi Envers baru atas nama pengguna yang sekadar membuka dialog, walaupun ia
	 * kemudian menekan Batal.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir baris ini.
	 *
	 * @return isi kolom {@code tanggal_dirubah}; untuk objek baru bernilai waktu pembuatan
	 *         objek, bukan {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Menetapkan cap waktu perubahan terakhir. Umumnya diisi otomatis lewat {@link #onUpdate()};
	 * pemanggilan manual hanya wajar pada jalur impor/migrasi data yang ingin mempertahankan cap
	 * waktu asli.
	 *
	 * @param tanggal_dirubah cap waktu yang akan dipasang
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan kode angket dalam bentuk <b>ternormalkan</b>: {@code null} diubah menjadi
	 * string kosong dan spasi tepi dibuang. Karena itu getter ini tidak pernah mengembalikan
	 * {@code null} dan aman dipakai langsung di {@link #toString()} maupun sebagai label grid.
	 * <p>
	 * Normalisasi ini hanya berlaku saat <i>membaca</i>; nilai yang tersimpan di kolom tetap apa
	 * adanya (lihat {@link #setKode(String)} yang tidak mem-{@code trim}). Query yang menyaring
	 * langsung ke kolom {@code kode} karena itu tetap dapat melihat spasi tepi yang tidak
	 * terlihat di layar.
	 *
	 * @return kode angket yang sudah di-{@code trim}; string kosong bila kolom {@code null}
	 */
	@Column(name = "kode", nullable = true)
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Menetapkan kode angket. Diisi dari textbox "Kode Angket" pada dialog tambah/ubah
	 * ({@code onSave()} menolak nilai kosong dengan messagebox "Kode Angket harus diisi", lalu
	 * memanggil {@code checkNamaAngket()} yang menolak kode kembar), dari jalur impor Excel
	 * {@code Common.uploadData(...)} yang <b>tidak</b> menjalankan kedua validasi itu, dan dari
	 * auto-seed yang memasang {@code "001.000"}.
	 * <p>
	 * Nilai disimpan <b>apa adanya</b> — tanpa {@code trim}. {@code onSave()} memang mengirim
	 * {@code kode.getValue()} yang belum di-{@code trim} (berbeda dari pemeriksaan
	 * kekembarannya yang justru memakai versi ter-{@code trim}), sehingga kode dengan spasi tepi
	 * dapat lolos sebagai "tidak kembar" padahal tampak identik di layar.
	 *
	 * @param kode kode angket yang akan dipasang
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama/judul angket <b>apa adanya</b> — tanpa normalisasi dan tanpa fallback.
	 * Meskipun kolomnya dipetakan {@code nullable = false}, getter ini tetap dapat mengembalikan
	 * {@code null} untuk objek yang belum diisi; pemanggil di layar master karena itu selalu
	 * menjaganya sendiri (mis. {@code angketPenilaianGuru.getIsi() == null ? "" : …}).
	 * <p>
	 * Nilai inilah yang menjadi bagian pertama judul section pada formulir siswa
	 * ({@code angket.getIsi() + " - " + grup.getIsi()} di {@code AngketGuruWindow}), label
	 * revisi pada grid ({@code RevisiHelper.createNewRevisi}), dan sasaran filter pencarian
	 * "Nama Angket" ({@code ilike ANYWHERE}).
	 *
	 * @return judul angket; boleh {@code null} meski pemetaannya {@code nullable = false}
	 */
	@Column(name = "isi", nullable = false, columnDefinition = "text")
	public String getIsi() {
		return isi;
	}

	/**
	 * Menetapkan nama/judul angket. Dipanggil dari {@code onSave()} layar master setelah
	 * validasi "Nama Angket harus diisi", dari jalur impor Excel (tanpa validasi tersebut), dan
	 * dari auto-seed yang memasang {@code "EVALUASI PENILAIAN PEMBELAJARAN"}.
	 *
	 * @param isi judul angket yang akan dipasang
	 */
	public void setIsi(String isi) {
		this.isi = isi;
	}

	/**
	 * Mengembalikan catatan bebas administrator untuk angket ini (kolom {@code keterangan},
	 * opsional). Getter ini mengembalikan field <b>apa adanya</b> — tanpa fallback, tanpa
	 * normalisasi, dan tanpa penulisan balik (pola "{@code getKeterangan()} membalik kontrak"
	 * yang dikenal pada beberapa entity lain repo ini <b>tidak</b> berlaku di sini).
	 * <p>
	 * <b>Jangan salah kira sebagai petunjuk pengisian.</b> Teks ini tidak pernah dirender pada
	 * formulir angket siswa; petunjuk yang tampil di sana berasal dari {@link #getPetunjuk()}.
	 * Pembacanya hanya kolom "Keterangan" pada grid layar master, isian awal dialog ubah, dan
	 * berkas cetak/impor (properti {@code keterangan} termasuk dalam larik {@code contents}
	 * {@code AngketPenilaianGuruAction}).
	 *
	 * @return catatan administrator, boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menetapkan catatan bebas administrator. Diisi dari textbox "Keterangan" pada dialog
	 * tambah/ubah layar master (dan dari jalur impor Excel); tanpa validasi maupun batasan
	 * panjang di sisi aplikasi (kolom bertipe {@code text}).
	 *
	 * @param keterangan catatan yang akan dipasang; {@code null} diperbolehkan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan petunjuk pengisian yang ditampilkan di kepala formulir angket siswa, dengan
	 * <b>fallback berlapis</b>: bila kolom {@code petunjuk} kosong/hanya spasi, dipakai nilai
	 * konfigurasi global {@code keterangan_checklist_penilaian_guru_oleh_siswa}; bila baris
	 * konfigurasi itu pun belum ada, dipakai teks bawaan yang di-hardcode di method ini
	 * (ajakan menilai secara jujur + keterangan makna skor 1..5).
	 *
	 * <h3>EFEK SAMPING: getter ini dapat MENULIS ke basis data</h3>
	 * <p>{@link Common#getKonfigurasi(String, String)} bukan pembaca murni. Bila kunci
	 * konfigurasi belum ada di cache maupun di tabel, ia <b>membuka session Hibernate sendiri,
	 * meng-{@code INSERT} baris {@code Konfigurasi} baru berisi nilai default di atas, lalu
	 * {@code commit}</b>. Konsekuensinya:</p>
	 * <ul>
	 *   <li>Sekadar merender daftar angket atau membuka formulir siswa pada instalasi baru sudah
	 *       menerbitkan tulisan ke basis data, di luar transaksi layar yang sedang berjalan.</li>
	 *   <li>Teks bawaan di method ini karena itu <b>bukan sekadar default runtime</b>: begitu
	 *       tersemai ia menjadi nilai konfigurasi nyata yang tampil di menu Konfigurasi.
	 *       Mengubah string hardcode di sini <b>tidak</b> akan mengubah perilaku instalasi yang
	 *       sudah pernah berjalan.</li>
	 * </ul>
	 * <p>Pemanggilan konfigurasi dilakukan <b>tanpa syarat</b> — sebelum kolom {@code petunjuk}
	 * diperiksa — sehingga biayanya tetap timbul walaupun angket sudah punya petunjuk sendiri.</p>
	 *
	 * <h3>Jebakan: nilai fallback ikut tersimpan saat menyunting</h3>
	 * <p>Dialog Ubah mengisi textbox "Petunjuk" dengan {@code getPetunjuk()} — yaitu teks
	 * fallback bila kolomnya masih kosong — dan {@code onSave()} menyimpan kembali
	 * {@code petunjuk.getValue()} apa adanya. Membuka lalu menyimpan sebuah angket karena itu
	 * <b>membekukan nilai konfigurasi yang berlaku saat itu ke dalam baris angket</b>;
	 * perubahan konfigurasi global sesudahnya tidak lagi berpengaruh pada angket tersebut.</p>
	 *
	 * @return petunjuk pengisian yang sudah di-{@code trim} bila berasal dari kolom, atau nilai
	 *         konfigurasi/teks bawaan bila kolom kosong. Tidak pernah {@code null}.
	 */
	@Column(name = "petunjuk", columnDefinition = "text")
	public String getPetunjuk() {
		String content = Common.getKonfigurasi("keterangan_checklist_penilaian_guru_oleh_siswa",
				"Sesuai dengan yang Saudara ketahui, berilah penilaian secara jujur, objektif, dan penuh tanggung jawab terhadap guru Saudara. Penilaian dilakukan terhadap aspek-aspek dalam tabel berikut dengan cara memilih angka pada kolom skor.\n 1 = sangat tidak baik/sangat rendah/tidak pernah\n 2 = tidak baik/rendah/jarang\n 3 = biasa/cukup/kadang-kadang\n 4 = baik/tinggi/sering\n 5 = sangat baik/sangat tinggi/selalu")
				.getNilai();
		return petunjuk == null || petunjuk.trim().isEmpty() ? content : petunjuk.trim();
	}

	/**
	 * Menetapkan petunjuk pengisian khusus angket ini. Diisi dari textarea "Petunjuk" (7 baris)
	 * pada dialog tambah/ubah dan dari jalur impor Excel. Nilai {@code null}/kosong berarti
	 * "pakai konfigurasi global" — lihat {@link #getPetunjuk()}, termasuk catatan bahwa jalur
	 * dialog praktis tidak pernah menyimpan nilai kosong.
	 *
	 * @param petunjuk teks petunjuk; {@code null}/kosong berarti memakai fallback konfigurasi
	 */
	public void setPetunjuk(String petunjuk) {
		this.petunjuk = petunjuk;
	}

	/**
	 * Mengembalikan banyaknya pilihan skala Likert per pertanyaan, dengan <b>fallback
	 * berlapis</b>: bila kolom {@code jumlah_pilihan} {@code null} atau {@code <= 0}, dipakai
	 * nilai konfigurasi global {@code jumlah_pilihan_checklist_penilaian_guru_oleh_siswa}; bila
	 * konfigurasi itu tidak ada/tidak dapat diparse menjadi angka, dipakai {@code 5}.
	 * <p>
	 * Nilai inilah yang menentukan berapa tombol radio dirender per butir pertanyaan di
	 * {@code AngketGuruWindow} (perulangan {@code for (i = 1; i &lt;= jumlahPilihan; i++)}) dan
	 * berapa banyak label pilihan dari {@code ChecklistPenilaianGuru.getPilihan()} (JSON
	 * berkunci {@code "1"}..{@code "N"}) yang benar-benar terpakai. Endpoint mobile
	 * {@code AngketUtilApi} memakai logika kembar untuk kunci JSON {@code "jumlahChecklist"}.
	 *
	 * <h3>EFEK SAMPING: getter ini dapat MENULIS ke basis data</h3>
	 * <p>Sama seperti {@link #getPetunjuk()}: {@link Common#getKonfigurasi(String, String)}
	 * meng-{@code INSERT}+{@code commit} baris {@code Konfigurasi} baru bila kunci belum ada,
	 * lewat session-nya sendiri. Blok konfigurasi ini juga dijalankan <b>tanpa syarat</b> —
	 * bahkan ketika kolom {@code jumlah_pilihan} sudah terisi — sehingga tiap pemanggilan
	 * menanggung biayanya. Karena renderer grid layar master memanggil getter ini untuk
	 * <i>setiap baris</i>, biaya itu berlipat sejumlah baris pada halaman.</p>
	 *
	 * <h3>Penanganan galat</h3>
	 * <p>Parsing dibungkus {@code try/catch} yang mencatat lewat
	 * {@code ais.common.ErrorAuditUtil.record(...)} lalu meneruskan dengan default {@code 5} —
	 * fail-safe, bukan fail-open: nilai konfigurasi yang rusak tidak membuat formulir kosong,
	 * hanya kembali ke skala 5.</p>
	 *
	 * @return jumlah pilihan yang efektif; selalu bernilai positif dan tidak pernah {@code null}
	 */
	@Column(name = "jumlah_pilihan")
	public Integer getJumlahPilihan() {
		Integer defaultJumlah = 5;
		try {
			defaultJumlah = Integer.parseInt(Common
					.getKonfigurasi("jumlah_pilihan_checklist_penilaian_guru_oleh_siswa", "5").getNilai().trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/AngketPenilaianGuru.java:145");
		}
		return jumlahPilihan == null || jumlahPilihan.intValue() <= 0 ? defaultJumlah : jumlahPilihan;
	}

	/**
	 * Menetapkan banyaknya pilihan skala Likert. Diisi dari {@code Intbox} "Jumlah Pilihan" pada
	 * dialog tambah/ubah dan dari jalur impor Excel.
	 * <p>
	 * <b>Tidak ada validasi sama sekali</b> di sisi setter maupun di {@code onSave()}: nilai
	 * {@code null}, nol, dan negatif diterima. Ketiganya dinetralkan saat dibaca oleh
	 * {@link #getJumlahPilihan()} (jatuh ke default), tetapi nilai besar yang tak masuk akal
	 * (mis. 500) diterima apa adanya dan akan benar-benar merender sebanyak itu tombol radio
	 * per pertanyaan.
	 *
	 * @param jumlahPilihan jumlah pilihan skala; {@code null}/{@code <= 0} akan dibaca kembali
	 *                      sebagai nilai default
	 */
	public void setJumlahPilihan(Integer jumlahPilihan) {
		this.jumlahPilihan = jumlahPilihan;
	}

	/**
	 * Mengembalikan {@link Yayasan} yang menjadi cakupan angket ini, setelah melewati
	 * {@code check()} milik {@link GeneralValueObject} yang meresolusi proxy lazy Hibernate
	 * menjadi instance nyata (canonical) bila memungkinkan. Penugasan ulang ke field pada baris
	 * pertama adalah bagian dari mekanisme resolusi itu, <b>bukan mutasi data</b> — nilai FK
	 * yang tersimpan tidak berubah.
	 * <p>
	 * <b>{@code null} berarti "berlaku untuk semua yayasan"</b>, bukan "tidak valid". Seluruh
	 * konsumen menuliskannya secara eksplisit sebagai
	 * {@code Restrictions.or(eq("angketPenilaianGuru.yayasan", yayasanSiswa), isNull("angketPenilaianGuru.yayasan"))},
	 * dan grid layar master merendernya sebagai label "Semua". Angket hasil auto-seed selalu
	 * berada dalam keadaan ini.
	 *
	 * @return yayasan cakupan yang sudah teresolusi, atau {@code null} bila angket berlaku untuk
	 *         semua yayasan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		yayasan = check(yayasan);
		return yayasan;
	}

	/**
	 * Menetapkan yayasan cakupan angket. Dipanggil dari {@code onSave()} layar master (nilai
	 * diambil dari kombo "Yayasan"; item "semua" ber-{@code value} {@code null} sehingga memang
	 * dapat mengosongkan cakupan) dan — perlu diwaspadai — dari
	 * {@code AngketPenilaianGuruAction.init()} yang memaksakan yayasan pengguna aktif ke objek
	 * <b>persisten</b> begitu dialog Ubah dibuka; lihat Javadoc kelas.
	 *
	 * @param yayasan yayasan cakupan; {@code null} berarti berlaku untuk semua yayasan
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan;
	}

	/**
	 * Mengembalikan {@link Sekolah} yang menjadi cakupan angket ini, setelah melewati
	 * {@code check()} milik {@link GeneralValueObject} yang meresolusi proxy lazy Hibernate
	 * (penugasan ulang ke field adalah bagian resolusi, bukan mutasi data).
	 * <p>
	 * <b>{@code null} berarti "berlaku untuk semua sekolah"</b>. Bersama {@link #getYayasan()},
	 * inilah satu-satunya pembatas tenant bagi seluruh rantai angket guru: {@code Grup},
	 * {@code Checklist}, maupun baris jawaban tidak punya kolom sekolah sendiri sehingga selalu
	 * menjoin naik ke sini ({@code AngketGuruWindow.buildGrupCriteria()},
	 * {@code buildChecklistCriteria()}, {@code AngketUtilApi}, {@code AngketUtil}).
	 * <p>
	 * <b>Catatan penting soal integritas cakupan:</b> nilai kolom ini sulit dipertahankan dari
	 * UI. Kombo "Prodi" pada dialog master selalu kosong karena instance-nya ditimpa sesudah
	 * diisi, sehingga {@code onSave()} praktis selalu menyimpan {@code null} — setiap
	 * penyuntingan melebarkan angket menjadi berlaku untuk semua sekolah. Rinciannya ada pada
	 * Javadoc kelas.
	 *
	 * @return sekolah cakupan yang sudah teresolusi, atau {@code null} bila angket berlaku untuk
	 *         semua sekolah
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return sekolah;
	}

	/**
	 * Menetapkan sekolah cakupan angket. Tiga jalur pemanggil yang terverifikasi:
	 * {@code onSave()} layar master (praktis selalu mengirim {@code null} karena bug kombo
	 * "Prodi"), {@code AngketPenilaianGuruAction.init()} yang memaksakan sekolah pengguna aktif
	 * ke objek persisten saat dialog dibuka, dan jalur impor Excel.
	 *
	 * @param sekolah sekolah cakupan; {@code null} berarti berlaku untuk semua sekolah
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah;
	}


	/**
	 * Mengembalikan nama program/peminatan yang menjadi cakupan angket, <b>ternormalkan menjadi
	 * {@code null} bila kosong</b> ({@code null} maupun string berisi spasi sama-sama menjadi
	 * {@code null}); selain itu nilainya di-{@code trim}.
	 * <p>
	 * Perhatikan bahwa normalisasi ini hanya berlaku di sisi Java. Query konsumen menyaring
	 * <b>langsung ke kolom</b> dan karena itu harus menangani ketiga bentuk "semua program"
	 * secara eksplisit:
	 * {@code or(eq("…program", ""), or(eq("…program", programSiswa), isNull("…program")))}
	 * — persis seperti yang dilakukan {@code AngketUtilApi} dan {@code AngketUtil}. Bentuk
	 * string kosong memang nyata terjadi karena {@code AngketPenilaianGuruAction.init()} dapat
	 * menuliskan {@code ""} (lihat Javadoc kelas).
	 * <p>
	 * <b>Ketidakselarasan yang perlu diketahui:</b> layar pengisian ZK
	 * ({@code AngketGuruWindow.buildGrupCriteria()}/{@code buildChecklistCriteria()})
	 * <b>tidak menyaring program sama sekali</b>, sedangkan jalur mobile
	 * ({@code AngketUtilApi}) dan pemeriksa login ({@code AngketUtil}) menyaringnya. Angket yang
	 * dibatasi ke satu program karena itu tetap muncul untuk siswa program lain bila ia membuka
	 * portal web, tetapi tidak muncul di aplikasi mobile — perbedaan perilaku yang mudah
	 * disalahartikan sebagai kerusakan data.
	 *
	 * @return nama program cakupan yang sudah di-{@code trim}, atau {@code null} bila angket
	 *         berlaku untuk semua program
	 */
	@Column(name = "program", nullable = true)
	public String getProgram() {
		return program == null || program.trim().isEmpty() ? null : program.trim();
	}

	/**
	 * Menetapkan nama program cakupan. Disimpan sebagai <b>teks nama program</b> (bukan FK),
	 * diambil dari kombo "Program" yang diisi {@code Common.initPrograms(...)}.
	 * <p>
	 * Selain {@code onSave()}, setter ini juga dipanggil {@code AngketPenilaianGuruAction.init()}
	 * dengan nilai {@code ""} pada kasus pengguna yang punya yayasan tetapi tanpa program —
	 * penjaga {@code if} di sana menguji {@code ambilYayasan()} sementara badannya memakai
	 * {@code ambilProgram()} (salah kawal salin-tempel). Karena objeknya persisten, nilai
	 * {@code ""} itu dapat tersimpan dan menimpa pembatasan program yang sudah ada.
	 * <p>
	 * Nilai disimpan apa adanya (tanpa {@code trim}); normalisasi hanya terjadi saat dibaca
	 * lewat {@link #getProgram()}.
	 *
	 * @param program nama program cakupan; {@code null}/kosong berarti semua program
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * Mengembalikan tahun angkatan (tahun masuk siswa) yang menjadi cakupan angket, dengan
	 * {@code null} dinormalkan menjadi string kosong dan spasi tepi dibuang. Tidak pernah
	 * mengembalikan {@code null}; grid layar master merender nilai kosong sebagai "Semua".
	 *
	 * <h3>BUG TERVERIFIKASI: angkatan majemuk yang dijanjikan UI tidak bekerja</h3>
	 * <p>Dialog master menampilkan petunjuk eksplisit: <i>"…jika terdapat banyak tahun angkatan,
	 * masukkan tahun angkatan yang dipisahkan koma, contoh 2017,2018,2019"</i>. Namun seluruh
	 * konsumen menyaring dengan bentuk yang sama:</p>
	 * <pre>
	 * Restrictions.or(eq("angketPenilaianGuru.angkatan", ""),
	 *     Restrictions.or(ilike("angketPenilaianGuru.angkatan", angkatanSiswa),
	 *                     isNull("angketPenilaianGuru.angkatan")))
	 * </pre>
	 * <p>{@code Restrictions.ilike(property, value)} tanpa argumen {@code MatchMode} menghasilkan
	 * {@code lower(kolom) like lower(?)} <b>tanpa wildcard</b> — yaitu pencocokan <b>persis</b>,
	 * sekadar tidak peka huruf besar/kecil. Nilai {@code "2017,2018,2019"} karena itu tidak akan
	 * pernah cocok dengan {@code siswa.getTahunMasuk()} yang bernilai {@code "2019"}. Akibatnya
	 * angket berangkatan majemuk <b>tidak tampil ke siapa pun</b> — gagal senyap, tanpa pesan
	 * galat. Satu-satunya bentuk yang benar-benar bekerja adalah satu angkatan tunggal, atau
	 * dikosongkan (berlaku untuk semua). Pola ini identik di
	 * {@code AngketGuruWindow}, {@code AngketUtilApi}, dan {@code AngketUtil} — jadi bukan salah
	 * ketik di satu tempat, melainkan salinan yang menyebar.</p>
	 *
	 * @return tahun angkatan cakupan yang sudah di-{@code trim}; string kosong berarti berlaku
	 *         untuk semua angkatan
	 */
	@Column(name = "angkatan", nullable = true)
	public String getAngkatan() {
		return angkatan == null ? "" : angkatan.trim();
	}

	/**
	 * Menetapkan tahun angkatan cakupan. Diisi dari textbox "Tahun Angkatan" pada dialog
	 * tambah/ubah — {@code onSave()} mengirim nilai yang sudah di-{@code trim} — dan dari jalur
	 * impor Excel (tanpa {@code trim}). Tidak ada validasi format: teks apa pun diterima,
	 * termasuk daftar berkoma yang secara praktis tidak akan pernah cocok (lihat
	 * {@link #getAngkatan()}).
	 * <p>
	 * Perhatikan bahwa {@code angkatan} bukan penanda periode berlakunya angket, melainkan
	 * penyaring <b>kohort responden</b>.
	 *
	 * @param angkatan tahun angkatan cakupan; kosong berarti semua angkatan
	 */
	public void setAngkatan(String angkatan) {
		this.angkatan = angkatan;
	}

	/**
	 * Mengembalikan penanda "angket ini untuk diisi siswa", dengan <b>{@code null} diperlakukan
	 * sebagai {@code TRUE}</b>. Tidak pernah mengembalikan {@code null}.
	 * <p>
	 * Konvensi "null = untuk siswa" ini <b>konsisten</b> dengan seluruh pembaca runtime, yang
	 * menyaring memakai {@code Restrictions.or(isNull("angketPenilaianGuru.untukSiswa"),
	 * eq("angketPenilaianGuru.untukSiswa", true))} — {@code AngketGuruWindow} (formulir siswa,
	 * baik pada tingkat grup maupun butir), {@code AngketUtilApi} (mobile), {@code AngketUtil}
	 * (pemeriksa saat login), dan {@code ChecklistPenilaianGuruHelper} (gerbang pengingat).
	 * Artinya angket yang baru dibuat lewat impor/auto-seed — yang meninggalkan kolom ini
	 * {@code NULL} — <b>langsung tayang ke siswa</b>.
	 * <p>
	 * Nilai sesungguhnya hanya ditulis lewat kotak centang "Untuk Siswa" pada grid layar master
	 * (digerbangi hak {@code UPDATE}, langsung menyimpan lewat
	 * {@code Common.refreshSaveOrUpdate}) dan lewat impor Excel — dialog tambah/ubah tidak punya
	 * isian ini sama sekali.
	 * <p>
	 * <b>Efek berantai:</b> mematikan penanda ini menyembunyikan <i>seluruh</i> grup dan butir di
	 * bawah angket ini dari formulir siswa sekaligus, karena filter dipasang pada tingkat join
	 * ke angket. Jawaban yang terlanjur tersimpan tidak ikut terhapus.
	 *
	 * @return {@link Boolean#TRUE} bila untuk siswa atau kolomnya {@code NULL};
	 *         {@link Boolean#FALSE} hanya bila kolom benar-benar berisi {@code false}
	 */
	@Column(name = "untuk_siswa")
	public Boolean getUntukSiswa() {
		return untukSiswa == null ? Boolean.TRUE : untukSiswa;
	}

	/**
	 * Menetapkan penanda "angket untuk diisi siswa". Dipanggil dari listener {@code onCheck}
	 * kotak centang "Untuk Siswa" pada grid layar master (yang langsung menyimpan barisnya) dan
	 * dari jalur impor Excel.
	 * <p>
	 * Perhatikan asimetri dengan {@link #getUntukSiswa()}: setter ini menyimpan {@code null} apa
	 * adanya bila diberi {@code null}, sedangkan getter menerjemahkannya menjadi {@code TRUE}.
	 * Membaca kembali nilai yang baru saja di-set karena itu tidak selalu menghasilkan nilai
	 * yang sama.
	 *
	 * @param untukSiswa penanda audiens siswa; {@code null} berarti "belum ditentukan" dan akan
	 *                   dibaca sebagai {@code TRUE}
	 */
	public void setUntukSiswa(Boolean untukSiswa) {
		this.untukSiswa = untukSiswa;
	}

	/**
	 * Mengembalikan penanda "angket ini untuk diisi guru", dengan <b>{@code null} diperlakukan
	 * sebagai {@code FALSE}</b> — kebalikan dari default {@link #getUntukSiswa()}. Tidak pernah
	 * mengembalikan {@code null}.
	 *
	 * <h3>TERVERIFIKASI: penanda ini tidak melakukan apa pun</h3>
	 * <p>Penelusuran seluruh repo menemukan hanya tiga kelompok pemakai: kotak centang "Untuk
	 * Guru" pada grid {@code AngketPenilaianGuruAction} (baca + tulis), larik {@code contents}
	 * layar itu (kolom cetak/impor Excel), dan berkas entity ini sendiri. <b>Nol query</b> —
	 * baik Criteria, HQL, maupun SQL native — yang menyaring memakai properti/kolom ini. Tidak
	 * ada layar "guru mengisi angket atas dirinya sendiri" yang terpasang pada rantai angket
	 * guru jenjang sekolah.</p>
	 * <p>Praktisnya: mencentang "Untuk Guru" <b>tidak</b> membuat angket muncul di portal guru,
	 * dan <b>tidak</b> mengurangi paparannya ke siswa (yang ditentukan hanya oleh
	 * {@link #getUntukSiswa()}). Kotak centang itu murni metadata. Bandingkan dengan padanan
	 * jenjang perguruan tinggi {@link ais.database.model.AngketPenilaianDosen} yang punya
	 * pasangan {@code untukMahasiswa}/{@code untukDosen} dengan bentuk yang sama — kemiripannya
	 * warisan salin-tempel, dan di sisi guru pasangan itu tidak pernah dilengkapi jalur
	 * pemakainya.</p>
	 *
	 * @return {@link Boolean#TRUE} hanya bila kolom benar-benar berisi {@code true};
	 *         {@link Boolean#FALSE} bila {@code false} atau {@code NULL}
	 */
	@Column(name = "untuk_guru")
	public Boolean getUntukGuru() {
		return untukGuru == null ? Boolean.FALSE : untukGuru;
	}

	/**
	 * Menetapkan penanda "angket untuk diisi guru". Dipanggil dari listener {@code onCheck}
	 * kotak centang "Untuk Guru" pada grid layar master dan dari jalur impor Excel. Karena
	 * penanda ini tidak dibaca query mana pun (lihat {@link #getUntukGuru()}), pemanggilan
	 * setter ini tidak berdampak fungsional apa pun selain mengubah tampilan grid, cap waktu
	 * audit, dan revisi Envers.
	 *
	 * @param untukGuru penanda audiens guru; {@code null} akan dibaca sebagai {@code FALSE}
	 */
	public void setUntukGuru(Boolean untukGuru) {
		this.untukGuru = untukGuru;
	}

	/**
	 * Mengembalikan saklar "tampilkan kotak keterangan tambahan per pertanyaan" pada formulir
	 * pengisian, dengan <b>{@code null} diperlakukan sebagai {@code FALSE}</b> (fail-closed:
	 * bawaannya tidak tampil). Tidak pernah mengembalikan {@code null}.
	 * <p>
	 * Satu-satunya pembaca pada rantai guru adalah {@code AngketGuruWindow}: bila bernilai
	 * {@code true}, di bawah setiap pertanyaan ditambahkan label "Keterangan tambahan" beserta
	 * {@code Textbox} dua baris yang isinya ikut tersimpan bersama nilai skor.
	 *
	 * <h3>KUIRK: pada varian guru, saklar ini tidak dapat dinyalakan lewat UI mana pun</h3>
	 * <p>Berbeda dari {@code AngketPenilaianDosenAction} dan {@code AngketPenilaianUmumAction}
	 * yang keduanya menyediakan kotak centang "Tampil Keterangan" di grid, layar master guru
	 * <b>tidak</b> memilikinya, dan properti {@code tampilKeterangan} juga <b>tidak</b>
	 * tercantum pada larik {@code contents} sehingga tidak tersentuh jalur cetak maupun impor
	 * Excel. Tidak ada pula isian untuknya di dialog tambah/ubah. Jadi pada instalasi normal
	 * kolom ini selalu {@code NULL} dan kotak keterangan per pertanyaan <b>tidak pernah
	 * dirender</b>; ia hanya dapat dinyalakan lewat manipulasi basis data langsung. Fitur ini
	 * secara efektif setengah terpasang di jenjang sekolah.</p>
	 * <p>Konsekuensi lanjutannya: karena kotak teksnya tidak pernah masuk ke pohon komponen,
	 * nilai keterangan per pertanyaan yang ikut disimpan {@code AngketGuruWindow.onSave(...)}
	 * selalu string kosong. Kolom "Catatan" pada dasbor rekap yang membacanya karena itu
	 * praktis selalu kosong pula (masukan/saran umum yang tampil di sana berasal dari kolom
	 * terpisah "Masukan/Saran/Komentar", yang memang selalu dirender).</p>
	 * <p><b>Bukan penanda anonimitas.</b> Lihat bagian verifikasi anonimitas pada Javadoc kelas:
	 * saklar ini mengatur ada-tidaknya kolom teks bebas, bukan ditampilkan-tidaknya identitas
	 * pengisi.</p>
	 *
	 * @return {@link Boolean#TRUE} hanya bila kolom benar-benar berisi {@code true};
	 *         {@link Boolean#FALSE} bila {@code false} atau {@code NULL}
	 */
	@Column(name = "tampil_keterangan")
	public Boolean getTampilKeterangan() {
		return tampilKeterangan == null ? Boolean.FALSE : tampilKeterangan;
	}

	/**
	 * Menetapkan saklar "tampilkan kotak keterangan tambahan per pertanyaan".
	 * <p>
	 * <b>Tidak ada satu pun pemanggil di repo ini</b> untuk varian guru — tidak dari layar
	 * master, tidak dari impor Excel, tidak dari API (lihat {@link #getTampilKeterangan()}).
	 * Setter ini praktis hanya dipakai Hibernate saat memuat baris dari basis data.
	 *
	 * @param tampilKeterangan saklar tampil keterangan; {@code null} akan dibaca sebagai
	 *                         {@code FALSE}
	 */
	public void setTampilKeterangan(Boolean tampilKeterangan) {
		this.tampilKeterangan = tampilKeterangan;
	}

	/**
	 * Representasi teks ringkas baris ini, disusun sebagai
	 * {@code "<id>-<kode>-<isi>[-<program>]"} dengan tiap bagian dilewati bila kosong. Contoh:
	 * {@code "3-001.000-EVALUASI PENILAIAN PEMBELAJARAN"}, atau
	 * {@code "7-002.000-Angket Semester Genap-IPA"} bila angket dibatasi ke satu program.
	 * <p>
	 * Detail implementasi yang perlu diperhatikan:
	 * <ul>
	 *   <li>Bagian {@code id} dan {@code kode} memakai getter ternormalkan
	 *       ({@link #getKode()}/{@link #getProgram()}), sedangkan judul dibaca <b>langsung dari
	 *       field</b> {@code isi} — bukan lewat {@link #getIsi()} — dan diganti string kosong
	 *       bila {@code null}. Objek transient yang belum diisi apa pun karena itu menghasilkan
	 *       string kosong, bukan {@link NullPointerException}.</li>
	 *   <li>Pemisahnya adalah tanda hubung yang <b>ditempelkan sesudah</b> tiap bagian, sehingga
	 *       baris tanpa judul menyisakan tanda hubung menggantung di ujung (mis.
	 *       {@code "3-001.000-"}).</li>
	 *   <li>Method ini <b>tidak</b> memanggil {@link #getPetunjuk()} maupun
	 *       {@link #getJumlahPilihan()}, jadi ia bebas dari efek samping penulisan konfigurasi
	 *       yang melekat pada kedua getter itu — aman dipakai di dalam log.</li>
	 * </ul>
	 * <p>
	 * Pemakainya adalah keperluan diagnostik/log serta komponen ZK yang menampilkan objek
	 * angket tanpa properti eksplisit; grid dan kombo layar master tidak memakai method ini
	 * melainkan memformat sendiri dari properti bernama.
	 *
	 * @return gabungan id, kode, judul, dan program angket; tidak pernah {@code null}
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (id != null) {
			sb.append(id).append("-");
		}
		if (getKode().length() > 0) {
			sb.append(getKode()).append("-");
		}
		sb.append(isi == null ? "" : isi);
		if (getProgram() != null) {
			sb.append("-").append(getProgram());
		}
		return sb.toString();
	}
}
