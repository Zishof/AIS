package ais.database.model.sekolah;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

import ais.database.model.GeneralValueObject;

/**
 * <b>Profil jam mengajar seorang guru untuk satu mata pelajaran</b> — tabel
 * {@code sekolah.guru_mengajar}.
 *
 * <h2>Satu baris = satu pasangan (guru, mata pelajaran)</h2>
 * <p>Entity ini menjawab pertanyaan <i>"guru X mengajar mata pelajaran Y pada hari dan jam
 * ke berapa saja?"</i>. Kunci logisnya adalah pasangan {@link #getGuru() guru} +
 * {@link #getMatapelajaran() matapelajaran}: {@code GuruMengajarAction.onSave(Event)} menolak
 * penambahan bila pasangan itu sudah ada (<i>"data Guru ... yang mengajar matapelajaran ...
 * sudah tersedia sebelumnya"</i>), dan alur salin dari jadwal juga mencari baris dengan kriteria
 * yang sama sebelum memutuskan membuat baris baru. Tidak ada kolom kelas, ruang, kurikulum,
 * tahun ajaran, maupun semester di sini.
 *
 * <h2>Anatomi 25 slot</h2>
 * <p>Beban mengajarnya disimpan sebagai <b>25 slot sejajar</b> dalam satu baris (bukan tabel
 * anak). Slot ke-N terdiri atas tiga kolom yang selalu dibaca bersama:</p>
 * <ul>
 *   <li>{@code hari}/{@code hari2} … {@code hari25} — nama hari sebagai teks bebas, diisi dari
 *   daftar {@code Common.haris} = {@code Minggu, Senin, Selasa, Rabu, Kamis, Jum'at, Sabtu};</li>
 *   <li>{@code jam_pelajaran_id}, {@code jam_pelajaran2_id} … {@code jam_pelajaran25_id} — FK ke
 *   {@link JamPelajaran} (rentang jam ke-berapa, mis. "07:00 s.d 07:45");</li>
 *   <li>{@code sub_matapelajaran}, {@code sub_matapelajaran_2} … {@code sub_matapelajaran_25} —
 *   FK opsional ke {@link SubMatapelajaran} (komponen mata pelajaran pada pertemuan itu).</li>
 * </ul>
 * <p>Perhatikan bahwa penamaan kolomnya <b>tidak seragam</b> antar keluarga: hari memakai sufiks
 * tanpa pemisah ({@code hari2}), jam pelajaran memakai {@code N_id} ({@code jam_pelajaran2_id}),
 * sedangkan sub mata pelajaran memakai garis bawah sebelum angka ({@code sub_matapelajaran_2}).
 * Slot pertama tidak bersufiks pada ketiga keluarga. Urutan deklarasi getter di berkas ini juga
 * tidak menaik rapi (mis. {@link #getJamPelajaran5()} dideklarasikan sebelum
 * {@link #getJamPelajaran4()}); itu warisan penyuntingan manual, bukan indikasi makna berbeda.</p>
 *
 * <h2>Bukan SK penugasan, dan bukan jadwal kelas — TERVERIFIKASI</h2>
 * <p>Tiga kelas dengan nama mirip sering tertukar. Hasil penelusuran seluruh repo:</p>
 * <ul>
 *   <li><b>{@link PenugasanGuruMengajar}</b> — kepala <i>surat keputusan</i> mengajar (nomor SK,
 *   tanggal SK, TMT, tahun akademik, semester). <b>Tidak ada satu pun rujukan kode</b> antara
 *   entity ini dan {@code PenugasanGuruMengajar}: tidak ada FK, tidak ada query gabungan, dan
 *   {@code ais.action.report.format1.sekolah.LaporanSKGuru} — pemasok nomor SK untuk kedua mode
 *   cetak — sama sekali tidak menyentuh {@code GuruMengajar}. Pada mode per-jadwal, sumber
 *   barisnya adalah {@code JadwalPelajaran}, bukan entity ini. Jadi kaitan keduanya murni
 *   <i>kemiripan nama</i>.</li>
 *   <li><b>{@link JadwalPelajaran}</b> — jadwal pelajaran satu <i>rombongan belajar</i>: punya
 *   {@code kelas}, {@code ruang}, {@code tahunAjaran}, {@code semester}, {@code abaikanBentrok},
 *   <b>12</b> slot hari/jam/sub-mapel, dan <b>12 slot guru</b> ({@code guru} … {@code guru12}).
 *   Porosnya terbalik dari entity ini: di sana satu baris = satu kelas dengan banyak guru; di
 *   sini satu baris = satu guru dengan banyak pertemuan. Satu-satunya jembatan kode adalah
 *   {@code GuruMengajarAction.onCopyJadwalPelajaran(Event)}, yang berjalan <b>satu arah</b>
 *   (jadwal → guru mengajar) dan hanya menyalin pasangan hari+jam yang belum ada, mengisi slot
 *   kosong pertama yang ditemukannya.</li>
 * </ul>
 *
 * <h2>Tidak terikat periode — konsekuensinya nyata</h2>
 * <p>Konstanta {@link #GANJIL} dan {@link #GENAP} dideklarasikan di kelas ini, tetapi
 * <b>tidak ada field semester maupun tahun ajaran</b> yang memakainya, dan penelusuran repo
 * tidak menemukan satu pun pembaca {@code GuruMengajar.GANJIL}/{@code GuruMengajar.GENAP} —
 * layar penyalinnya pun memakai {@code JadwalPelajaran.GANJIL}/{@code JadwalPelajaran.GENAP}.
 * Keduanya sisa salin-tempel yang tidak berfungsi.</p>
 * <p>Akibat praktis dari ketiadaan kolom periode: baris di sini <b>berumur selamanya</b>. Menu
 * "Copy dari Jadwal Pelajaran" meminta tahun ajaran dan semester untuk memilih sumber, tetapi
 * hasilnya ditumpuk ke baris {@code (guru, mapel)} yang sama tanpa penanda periode. Menyalin dua
 * tahun ajaran berturut-turut akan mengakumulasi slot sampai ke-25 penuh, lalu penyalinan
 * berikutnya <b>berhenti diam-diam</b> — rantai {@code else if} pada penyalin tidak punya cabang
 * terakhir dan tidak melaporkan apa pun ke pengguna (variabel {@code warning} di sana selalu
 * berisi string kosong).</p>
 *
 * <h2>Siapa yang membaca data ini</h2>
 * <ol>
 *   <li><b>Layar master</b> — {@code ais.action.master.sekolah.GuruMengajarAction} atas
 *   {@code webapp/WEB-INF/z/x/y/pages/master/sekolah/guru_mengajar.zul}. Grid berkolom
 *   <i>Mata Pelajaran</i>, <i>Guru</i>, <i>Jadwal</i> (diisi {@link #infoSimple()}),
 *   <i>Keterangan</i>. Formulir popup berjudul <i>"Tambah/Ubah Guru Mengajar"</i> memunculkan
 *   slot satu per satu: baris <i>"Hari dan Jam Pelajaran I"</i> … <i>XXV</i> baru terlihat
 *   setelah slot sebelumnya terisi.</li>
 *   <li><b>Kartu profil</b> — {@code ais.action.master.helper.profile.ProfileSekolahLanjutanDashboard}
 *   menampilkan kartu "Guru Mengajar" berisi {@code COUNT(*)} entity ini <b>tanpa kriteria apa
 *   pun</b> ({@code ProfileUiHelper.hitung(GuruMengajar.class, null)}), sehingga angkanya
 *   mencakup seluruh sekolah dan yayasan pada instalasi.</li>
 *   <li><b>Inisialisasi skema</b> — {@code ais.common.InitData} mendaftarkan kelas ini pada
 *   {@code initClasses(...)} agar tabelnya dibentuk/diselaraskan saat aplikasi start.</li>
 * </ol>
 * <p>Di luar tiga titik itu, tidak ada laporan JasperReports, rapor, REST API, maupun dasbor yang
 * membaca isi 25 slotnya. Kartu "Guru Mengajar" pada dasbor akademik ({@code DasborAkademikSekolah},
 * {@code DasboardJadwalPelajaran}) <b>bukan</b> berasal dari entity ini melainkan dari perhitungan
 * guru terjadwal di {@code JadwalPelajaran} — kesamaan namanya menyesatkan.</p>
 *
 * <h2>Getter yang menulis balik — {@link #getSekolah()} dan {@link #getYayasan()}</h2>
 * <p>Sebagian besar getter relasi hanya menyelesaikan proxy lazy lewat
 * {@link ais.database.model.GeneralValueObject#check(Object)}. Dua di antaranya melangkah lebih
 * jauh dan <b>menimpa nilai kolom</b>:</p>
 * <ul>
 *   <li>{@link #getSekolah()} selalu menulis {@code sekolah = getMatapelajaran().getSekolah()}
 *   bila mata pelajarannya ada — dan {@code matapelajaran} berstatus {@code nullable = false},
 *   jadi praktis selalu. Pilihan sekolah yang disimpan lewat {@link #setSekolah(Sekolah)} dari
 *   formulir karena itu <b>tidak pernah bertahan</b>; kolom {@code sekolah_id} selalu berakhir
 *   mengikuti sekolah pemilik mata pelajaran.</li>
 *   <li>{@link #getYayasan()} memanggil {@link #getSekolah()} lalu menulis
 *   {@code yayasan = sekolah.getYayasan()} — jadi rantai timpa berjenjang: mapel → sekolah →
 *   yayasan.</li>
 * </ul>
 * <p>Karena Hibernate memakai <i>property access</i> (anotasi dipasang pada getter), nilai yang
 * ditulis balik itu ikut terbawa <i>dirty checking</i> dan tersimpan pada {@code flush}
 * berikutnya. Yang membuatnya berdampak luas: {@link #infoSimple()} — dipanggil untuk
 * <b>setiap baris</b> yang dirender di grid — diawali {@code sekolah = getSekolah()}, sehingga
 * sekadar membuka daftar sudah dapat menulis ulang {@code sekolah_id} seluruh baris pada halaman
 * itu. Entity dianotasi {@code @Audited}, jadi penulisan senyap tersebut juga melahirkan revisi
 * Envers yang tampak seperti suntingan sah oleh pengguna yang kebetulan membuka layar.</p>
 *
 * <h2>Cakupan tenant: gagal-terbuka</h2>
 * <p>{@code GuruMengajarAction.initCriteria(boolean)} <b>tidak memiliki penyaring tenant
 * bawaan</b>. Filter sekolah/yayasan hanya diterapkan bila combo {@code searchsekolah}/
 * {@code searchyayasan} kebetulan terpilih; selebihnya dipakai
 * {@code Restrictions.sqlRestriction("1=1")}. Pengisian combo itu diserahkan ke
 * {@code Common.initYayasanDanSekolahDanSemua(...)} yang gagal-terbuka (seluruh badannya
 * dibungkus {@code try/catch}, dan pilihan "Semua" bernilai {@code null}). Pada instalasi
 * multi-sekolah, pengguna yang tidak punya sekolah melekat karena itu melihat — lengkap dengan
 * tombol Salin/Ubah/Hapus per baris — penugasan mengajar seluruh sekolah dan yayasan. Pola
 * identik sudah tercatat pada keluarga temuan fail-open repo ini; keparahan isinya sendiri
 * sedang (pemetaan guru–mapel, bukan data pribadi siswa), tetapi permukaan <b>tulis</b> lintas
 * tenant nyata. Pemeriksaan duplikat di {@code onSave} pun mencari ke seluruh instalasi tanpa
 * batas sekolah.</p>
 *
 * <h2>Hak akses layar — hasil verifikasi</h2>
 * <ul>
 *   <li><b>Pewarisan hak lewat menu induk: NEGATIF.</b> {@code guru_mengajar.zul} tidak pernah
 *   disisipkan sebagai tab/{@code MyInclude} oleh layar lain (penelusuran seluruh {@code webapp}
 *   dan {@code src} tidak menemukan rujukan ke lintasan berkas itu), sehingga
 *   {@code CommonPrivilages.checkPrevilages(...)} di sini benar-benar mengevaluasi hak menu
 *   layar ini sendiri. Ini kontras dengan {@code SubMatapelajaran} yang justru mewarisi hak menu
 *   Mata Pelajaran.</li>
 *   <li><b>Titik masuk lintas layar yang laten</b> — {@code GuruMengajarAction.onAddExternal(...)}
 *   bersifat {@code static} dan membuka formulir entity ini di atas layar lain. Saat ini
 *   <b>tidak ada pemanggilnya</b>, tetapi begitu dipakai, hak yang berlaku akan menjadi hak menu
 *   layar pemanggil — bentuk pewarisan hak yang sama.</li>
 *   <li><b>Tombol Salin digerbangi hak Ubah, bukan hak Tambah</b> —
 *   {@code Common.copyEditDeleteButtons(edit, edit, delete, ...)} meneruskan nilai
 *   {@code checkPrevilages(UPDATE)} ke parameter {@code copy}. Padahal menyalin baris
 *   <i>membuat baris baru</i>, jadi pengguna berhak Ubah tanpa hak Tambah tetap bisa menambah
 *   data.</li>
 *   <li><b>Menu "Copy dari Jadwal Pelajaran" tidak terpasang</b> —
 *   {@code onCopyJadwalPelajaran(Event)} lengkap dan berfungsi, tetapi tidak ada
 *   {@code forward="onClick=onCopyJadwalPelajaran"} di {@code guru_mengajar.zul} maupun
 *   penambahan tombolnya di {@code doAfterCompose}. Alur impor jadwal → guru mengajar karena itu
 *   <b>tidak dapat dijangkau dari UI saat ini</b>; kode aktifnya tetap didokumentasikan di sini
 *   karena sewaktu-waktu tinggal dipasang. Bila dipasang, ia berjalan <b>tanpa gerbang hak sama
 *   sekali</b> (metodenya tidak memanggil {@code checkPrevilages}) dan menulis ke seluruh sekolah
 *   yang combo-nya izinkan.</li>
 *   <li><b>Parameter URL {@code guru_selected}</b> — {@code doAfterCompose} membaca
 *   {@code execution.getParameter("guru_selected")} dan mengunci daftar pada guru dengan id
 *   tersebut, tanpa memeriksa apakah guru itu satu sekolah dengan pengguna. Karena daftar dasarnya
 *   memang sudah lintas tenant, ini bukan eskalasi baru, tetapi menjadikan pengintipan per-guru
 *   cukup satu parameter URL.</li>
 * </ul>
 *
 * <h2>Ekspor/impor Excel — 12 dari 25 slot, plus kolom hantu</h2>
 * <p>{@code doAfterCompose} menyusun satu larik {@code contents} yang dipakai oleh <i>dua</i>
 * tombol sekaligus: {@code Common.cetakData(...)} (ekspor) dan
 * {@code Common.uploadData(this, GuruMengajar.class, contents)} (impor). Larik itu disalin dari
 * {@code JadwalPelajaranAction} dan belum disesuaikan:</p>
 * <ul>
 *   <li>hanya memuat slot <b>1–12</b>; slot 13–25 tidak ikut diekspor maupun diimpor;</li>
 *   <li>memuat nama properti {@code "abaikanBentrok"} yang <b>tidak ada</b> pada entity ini —
 *   properti itu milik {@link JadwalPelajaran};</li>
 *   <li>memuat {@code "id"}, sehingga berkas unggahan dapat menunjuk baris mana pun di instalasi;
 *   tombolnya memang baru tampil bila pengguna punya hak Tambah, Ubah, <i>dan</i> Hapus.</li>
 * </ul>
 *
 * <h2>Bug yang TERVERIFIKASI pada berkas ini</h2>
 * <ol>
 *   <li><b>{@link #infoSimple()} melewatkan slot 19 dan menampilkan slot 18 dua kali.</b> Blok
 *   untuk slot ke-19 disalin dari blok slot ke-18 tanpa mengganti angkanya, sehingga kolom
 *   <i>Jadwal</i> di grid tidak pernah memperlihatkan pertemuan slot 19 dan mengulang pertemuan
 *   slot 18. {@link #populateJamPelajaran()} dan {@link #populateHari()} tidak terkena — keduanya
 *   memeriksa slot 19 dengan benar, jadi datanya utuh di basis data dan hanya tampilannya yang
 *   salah.</li>
 *   <li><b>Kolom sub mata pelajaran ditulis {@code NULL} setiap kali baris disimpan ulang.</b>
 *   {@code GuruMengajarAction.onSave(Event)} tidak menyentuh satu pun
 *   {@code setSubMatapelajaranN(...)}, dan formulirnya tidak pernah memilih ulang nilai tersimpan
 *   ke dalam combo. Rinciannya sudah dicatat pada Javadoc {@link SubMatapelajaran}. Karena
 *   ke-25 slot sub mata pelajaran entity ini tidak punya pembaca hilir, kehilangan itu tidak
 *   terlihat di laporan mana pun.</li>
 *   <li><b>Filter "Hari" hanya menjangkau slot 1–10.</b> Rantai {@code Restrictions.or(...)} di
 *   {@code initCriteria} berhenti pada {@code hari10}. Baris yang hari mengajarnya hanya berada
 *   di slot 11–25 <b>menghilang</b> dari daftar begitu filter hari dipakai.</li>
 *   <li><b>Pencarian baris pasangan tidak deterministik.</b> Baik {@code onSave} maupun penyalin
 *   jadwal memakai {@code .setMaxResults(1)} tanpa {@code addOrder(...)}. Bila baris kembar
 *   terlanjur ada (mis. dibuat lewat unggahan Excel), yang disunting/ditimpa bisa berganti-ganti
 *   antar pemanggilan.</li>
 *   <li><b>Panjang kolom {@code hari} tidak seragam.</b> Hanya slot pertama yang dianotasi
 *   {@code @Column(length = 6)}; slot 2–25 tidak beranotasi sehingga memakai panjang bawaan.
 *   Enam karakter kebetulan pas untuk nilai terpanjang {@code "Jum'at"}/{@code "Minggu"} — tanpa
 *   sisa ruang sama sekali bila daftar hari diperluas.</li>
 * </ol>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ul>
 *   <li><b>Identitas &amp; jejak audit</b> — {@link #getId() id}, {@link #getOleh() oleh},
 *   {@link #getOlehId() olehId}, {@link #getTanggal_dirubah() tanggal_dirubah},
 *   {@link #onUpdate()}.</li>
 *   <li><b>Poros baris (global, bukan per slot)</b> — {@link #getGuru() guru},
 *   {@link #getMatapelajaran() matapelajaran}, {@link #getSekolah() sekolah},
 *   {@link #getYayasan() yayasan}, {@link #getKeterangan() keterangan}.</li>
 *   <li><b>Per slot (25 × 3)</b> — {@code getHariN()}/{@code setHariN()},
 *   {@code getJamPelajaranN()}/{@code setJamPelajaranN()},
 *   {@code getSubMatapelajaranN()}/{@code setSubMatapelajaranN()}.</li>
 *   <li><b>Method bisnis lintas slot</b> — {@link #infoSimple()} (ringkasan untuk grid),
 *   {@link #populateJamPelajaran()} (pasangan jam+hari yang terisi),
 *   {@link #populateHari()} (daftar hari yang terisi), {@link #toString()}.</li>
 *   <li><b>Konstanta mati</b> — {@link #GANJIL}, {@link #GENAP}.</li>
 * </ul>
 *
 * <h2>Catatan pewarisan dari {@link ais.database.model.GeneralValueObject}</h2>
 * <p>Induknya adalah POJO abstrak biasa — <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass} — sehingga Hibernate tidak memetakan satu pun propertinya. Karena
 * itu {@link #id}, {@link #oleh}, {@link #olehId}, {@link #tanggal_dirubah}, dan
 * {@link #keterangan} <b>wajib</b> dideklarasikan ulang di kelas ini agar menjadi kolom;
 * pengulangan itu keharusan teknis, bukan duplikasi yang perlu dibersihkan. Sebaliknya properti
 * induk yang tidak dideklarasikan ulang (mis. {@code nama}, {@code kode}, {@code nomorUrut})
 * tetap dapat dipakai sebagai state in-memory tetapi tidak pernah tersimpan.</p>
 *
 * <p><b>Catatan bentuk kode:</b> komentar generator di atas deklarasi kelas dahulu berbunyi
 * "JadwalPelajaran generated by hbm2java" — nama yang keliru sejak awal dan menjadi salah satu
 * sumber kebingungan yang diluruskan di atas.</p>
 *
 * @see PenugasanGuruMengajar
 * @see JadwalPelajaran
 * @see JamPelajaran
 * @see SubMatapelajaran
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "guru_mengajar", schema = "sekolah")
public class GuruMengajar extends GeneralValueObject {

	/**
	 * Label semester genap, {@code "Genap"}.
	 *
	 * <p><b>Konstanta mati.</b> Entity ini tidak punya kolom semester maupun tahun ajaran, dan
	 * penelusuran seluruh repo tidak menemukan pembaca {@code GuruMengajar.GENAP} — layar
	 * penyalin jadwal sekalipun memakai {@code JadwalPelajaran.GENAP}. Dipertahankan apa adanya
	 * agar berkas ini tidak berubah selain Javadoc.</p>
	 */
	public static final String GENAP = "Genap";
	/**
	 * Label semester ganjil, {@code "Ganjil"}.
	 *
	 * <p><b>Konstanta mati</b> dengan alasan yang sama seperti {@link #GENAP}.</p>
	 */
	public static final String GANJIL = "Ganjil";

	/**
	 * Penanda versi serialisasi, diwarisi dari kontrak {@code Serializable} pada
	 * {@link ais.database.model.GeneralValueObject}. Jangan diubah: instance entity ini ikut
	 * tersimpan di sesi ZK, sehingga perubahan nilainya membuat sesi lama gagal dipulihkan.
	 */
	private static final long serialVersionUID = 7154228487700348608L;
	/**
	 * Kunci utama, kolom {@code id} ({@code IDENTITY}, {@code insertable = false}).
	 *
	 * <p>Dideklarasikan ulang di sini walau {@link ais.database.model.GeneralValueObject} juga
	 * memilikinya, karena induknya bukan {@code @MappedSuperclass} sehingga properti induk tidak
	 * dipetakan Hibernate.</p>
	 */
	private Long id;
	/**
	 * Nama pengguna terakhir yang menyunting baris ini; diisi otomatis oleh
	 * {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}.
	 */
	private String oleh;
	/**
	 * Id pengguna terakhir yang menyunting baris ini; pasangan dari {@link #oleh}.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang menyunting baris ini.
	 *
	 * @return id pengguna penyunting terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna penyunting terakhir.
	 *
	 * <p>Nilai {@code null} atau yang hanya berisi spasi <b>diabaikan diam-diam</b> — nilai lama
	 * dipertahankan dan pemanggil tidak menerima tanda apa pun. Perilaku ini disengaja agar jejak
	 * audit tidak terhapus oleh pemanggil yang meneruskan nilai kosong.</p>
	 *
	 * @param olehId id pengguna penyunting; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna penyunting terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: nilai {@code null} atau kosong/spasi diabaikan
	 * diam-diam.</p>
	 *
	 * @param oleh nama pengguna penyunting; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang menyunting baris ini.
	 *
	 * @return nama pengguna penyunting terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate} yang menyegarkan jejak audit tepat sebelum baris ini
	 * di-{@code UPDATE}.
	 *
	 * <p>Mendelegasikan seluruhnya ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}, yang mengisi
	 * {@link #setOleh(String) oleh}/{@link #setOlehId(String) olehId} dari pengguna yang sedang
	 * login dan memutakhirkan {@link #setTanggal_dirubah(Date) tanggal_dirubah}. Dipanggil oleh
	 * penyedia persistensi, <b>bukan</b> oleh kode aplikasi.</p>
	 *
	 * <p><b>Efek samping:</b> memutasi tiga field audit pada instance ini. Karena entity dianotasi
	 * {@code @Audited}, setiap {@code UPDATE} juga melahirkan satu revisi Envers — termasuk
	 * {@code UPDATE} yang dipicu penulisan balik {@link #getSekolah()} saat daftar dirender, yang
	 * kemudian ditampilkan kembali oleh {@code RevisiHelper} di kolom pertama grid.</p>
	 *
	 * <p><b>Catatan bentuk kode:</b> field {@code tanggal_dirubah} sengaja dibiarkan berbagi baris
	 * fisik dengan deklarasi method ini — bentuk warisan generator yang tidak dirapikan agar diff
	 * berkas ini tetap murni Javadoc. Field tersebut menyimpan waktu perubahan terakhir (kolom
	 * {@code tanggal_dirubah}) dan diberi nilai awal {@code ais.ui.util.WaktuUtil.getDate()} saat
	 * instance dibuat, sehingga baris baru pun sudah punya stempel waktu sebelum disimpan.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya tidak dipanggil kode aplikasi: nilainya diisi {@link #onUpdate()} lewat
	 * {@code AuditTimestampInterceptor}. Tidak ada validasi — nilai {@code null} pun diterima.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini (kolom {@code tanggal_dirubah},
	 * {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada instance yang baru dibuat
	 *         karena field-nya diinisialisasi saat konstruksi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Jam pelajaran pada slot ke-1, kolom FK {@code jam_pelajaran_id}.
	 *
	 * <p>Slot pertama dari 25 slot sejajar. Setiap slot adalah satu pertemuan mengajar dan
	 * terdiri atas tiga kolom sejajar: {@link #hari}, {@link #jamPelajaran}, dan
	 * {@link #subMatapelajaran}. Slot diisi berurutan dari yang pertama; formulir baru
	 * memunculkan baris slot berikutnya setelah slot sebelumnya terisi.</p>
	 */
	private JamPelajaran jamPelajaran;
	/**
	 * Jam pelajaran pada slot ke-2, kolom FK {@code jam_pelajaran2_id}.
	 */
	private JamPelajaran jamPelajaran2;
	/**
	 * Jam pelajaran pada slot ke-3, kolom FK {@code jam_pelajaran3_id}.
	 */
	private JamPelajaran jamPelajaran3;
	/**
	 * Jam pelajaran pada slot ke-4, kolom FK {@code jam_pelajaran4_id}.
	 */
	private JamPelajaran jamPelajaran4;
	/**
	 * Jam pelajaran pada slot ke-5, kolom FK {@code jam_pelajaran5_id}.
	 */
	private JamPelajaran jamPelajaran5;
	/**
	 * Jam pelajaran pada slot ke-6, kolom FK {@code jam_pelajaran6_id}.
	 */
	private JamPelajaran jamPelajaran6;
	/**
	 * Jam pelajaran pada slot ke-7, kolom FK {@code jam_pelajaran7_id}.
	 */
	private JamPelajaran jamPelajaran7;
	/**
	 * Jam pelajaran pada slot ke-8, kolom FK {@code jam_pelajaran8_id}.
	 */
	private JamPelajaran jamPelajaran8;
	/**
	 * Jam pelajaran pada slot ke-9, kolom FK {@code jam_pelajaran9_id}.
	 */
	private JamPelajaran jamPelajaran9;
	/**
	 * Jam pelajaran pada slot ke-10, kolom FK {@code jam_pelajaran10_id}.
	 */
	private JamPelajaran jamPelajaran10;
	/**
	 * Jam pelajaran pada slot ke-11, kolom FK {@code jam_pelajaran11_id}.
	 */
	private JamPelajaran jamPelajaran11;
	/**
	 * Jam pelajaran pada slot ke-12, kolom FK {@code jam_pelajaran12_id}.
	 */
	private JamPelajaran jamPelajaran12;
	/**
	 * Jam pelajaran pada slot ke-13, kolom FK {@code jam_pelajaran13_id}.
	 */
	private JamPelajaran jamPelajaran13;
	/**
	 * Jam pelajaran pada slot ke-14, kolom FK {@code jam_pelajaran14_id}.
	 */
	private JamPelajaran jamPelajaran14;
	/**
	 * Jam pelajaran pada slot ke-15, kolom FK {@code jam_pelajaran15_id}.
	 */
	private JamPelajaran jamPelajaran15;
	/**
	 * Jam pelajaran pada slot ke-16, kolom FK {@code jam_pelajaran16_id}.
	 */
	private JamPelajaran jamPelajaran16;
	/**
	 * Jam pelajaran pada slot ke-17, kolom FK {@code jam_pelajaran17_id}.
	 */
	private JamPelajaran jamPelajaran17;
	/**
	 * Jam pelajaran pada slot ke-18, kolom FK {@code jam_pelajaran18_id}.
	 */
	private JamPelajaran jamPelajaran18;
	/**
	 * Jam pelajaran pada slot ke-19, kolom FK {@code jam_pelajaran19_id}.
	 */
	private JamPelajaran jamPelajaran19;
	/**
	 * Jam pelajaran pada slot ke-20, kolom FK {@code jam_pelajaran20_id}.
	 */
	private JamPelajaran jamPelajaran20;
	/**
	 * Jam pelajaran pada slot ke-21, kolom FK {@code jam_pelajaran21_id}.
	 */
	private JamPelajaran jamPelajaran21;
	/**
	 * Jam pelajaran pada slot ke-22, kolom FK {@code jam_pelajaran22_id}.
	 */
	private JamPelajaran jamPelajaran22;
	/**
	 * Jam pelajaran pada slot ke-23, kolom FK {@code jam_pelajaran23_id}.
	 */
	private JamPelajaran jamPelajaran23;
	/**
	 * Jam pelajaran pada slot ke-24, kolom FK {@code jam_pelajaran24_id}.
	 */
	private JamPelajaran jamPelajaran24;
	/**
	 * Jam pelajaran pada slot ke-25, kolom FK {@code jam_pelajaran25_id}.
	 */
	private JamPelajaran jamPelajaran25;

	/**
	 * Mata pelajaran yang diajarkan, kolom FK {@code matapelajaran_id} ({@code NOT NULL}).
	 *
	 * <p>Bersama {@link #guru} membentuk kunci logis baris ini. Juga menjadi <b>sumber turunan</b>
	 * {@link #sekolah} (lihat {@link #getSekolah()}) dan, lewat sekolah, {@link #yayasan}.</p>
	 */
	private Matapelajaran matapelajaran;
	/**
	 * Guru pengampu, kolom FK {@code guru_id}.
	 *
	 * <p>Secara skema boleh {@code null}, tetapi formulir menolak simpan bila kosong
	 * (<i>"Guru harus diisi"</i>). Dipilih lewat komponen pencari {@code AmbilDataGuruBanbox},
	 * dan dapat dikunci dari luar lewat parameter URL {@code guru_selected}.</p>
	 */
	private Guru guru;
	/**
	 * Sekolah pemilik baris, kolom FK {@code sekolah_id}.
	 *
	 * <p><b>Nilai turunan, bukan masukan.</b> {@link #getSekolah()} selalu menimpanya dengan
	 * {@code matapelajaran.sekolah}, sehingga apa pun yang disimpan lewat
	 * {@link #setSekolah(Sekolah)} tidak bertahan. Kolomnya tetap dipakai sebagai penyaring pada
	 * {@code GuruMengajarAction.initCriteria(boolean)}.</p>
	 */
	private Sekolah sekolah;
	/**
	 * Yayasan pemilik baris, kolom FK {@code yayasan_id}.
	 *
	 * <p>Nilai turunan berjenjang: {@link #getYayasan()} mengambilnya dari
	 * {@code sekolah.yayasan}, sedangkan sekolahnya sendiri turunan dari mata pelajaran.</p>
	 */
	private Yayasan yayasan;
	/**
	 * Nama hari pertemuan slot ke-1, kolom {@code hari}.
	 *
	 * <p>Teks bebas berisi salah satu nilai {@code Common.haris} ({@code Minggu} … {@code Sabtu}).
	 * Hanya kolom slot pertama ini yang dianotasi {@code @Column(length = 6)}; slot 2–25 memakai
	 * panjang bawaan.</p>
	 */
	private String hari;
	/**
	 * Nama hari pertemuan slot ke-2, kolom {@code hari2}.
	 */
	private String hari2;
	/**
	 * Nama hari pertemuan slot ke-3, kolom {@code hari3}.
	 */
	private String hari3;
	/**
	 * Nama hari pertemuan slot ke-4, kolom {@code hari4}.
	 */
	private String hari4;
	/**
	 * Nama hari pertemuan slot ke-5, kolom {@code hari5}.
	 */
	private String hari5;
	/**
	 * Nama hari pertemuan slot ke-6, kolom {@code hari6}.
	 */
	private String hari6;
	/**
	 * Nama hari pertemuan slot ke-7, kolom {@code hari7}.
	 */
	private String hari7;
	/**
	 * Nama hari pertemuan slot ke-8, kolom {@code hari8}.
	 */
	private String hari8;
	/**
	 * Nama hari pertemuan slot ke-9, kolom {@code hari9}.
	 */
	private String hari9;
	/**
	 * Nama hari pertemuan slot ke-10, kolom {@code hari10}.
	 */
	private String hari10;
	/**
	 * Nama hari pertemuan slot ke-11, kolom {@code hari11}.
	 */
	private String hari11;
	/**
	 * Nama hari pertemuan slot ke-12, kolom {@code hari12}.
	 */
	private String hari12;
	/**
	 * Nama hari pertemuan slot ke-13, kolom {@code hari13}.
	 */
	private String hari13;
	/**
	 * Nama hari pertemuan slot ke-14, kolom {@code hari14}.
	 */
	private String hari14;
	/**
	 * Nama hari pertemuan slot ke-15, kolom {@code hari15}.
	 */
	private String hari15;
	/**
	 * Nama hari pertemuan slot ke-16, kolom {@code hari16}.
	 */
	private String hari16;
	/**
	 * Nama hari pertemuan slot ke-17, kolom {@code hari17}.
	 */
	private String hari17;
	/**
	 * Nama hari pertemuan slot ke-18, kolom {@code hari18}.
	 */
	private String hari18;
	/**
	 * Nama hari pertemuan slot ke-19, kolom {@code hari19}.
	 */
	private String hari19;
	/**
	 * Nama hari pertemuan slot ke-20, kolom {@code hari20}.
	 */
	private String hari20;
	/**
	 * Nama hari pertemuan slot ke-21, kolom {@code hari21}.
	 */
	private String hari21;
	/**
	 * Nama hari pertemuan slot ke-22, kolom {@code hari22}.
	 */
	private String hari22;
	/**
	 * Nama hari pertemuan slot ke-23, kolom {@code hari23}.
	 */
	private String hari23;
	/**
	 * Nama hari pertemuan slot ke-24, kolom {@code hari24}.
	 */
	private String hari24;
	/**
	 * Nama hari pertemuan slot ke-25, kolom {@code hari25}.
	 */
	private String hari25;

	/**
	 * Sub mata pelajaran (komponen) yang diajarkan pada slot ke-1, kolom FK {@code sub_matapelajaran}.
	 *
	 * <p>Opsional: combo pemilihnya disembunyikan bila mata pelajaran terpilih tidak punya sub
	 * mata pelajaran. Perhatikan bug hilir yang tercatat pada Javadoc kelas — ke-25 kolom ini
	 * ditulis {@code NULL} setiap kali baris disimpan ulang lewat layar, dan tidak ada pembaca
	 * hilirnya sama sekali.</p>
	 */
	private SubMatapelajaran subMatapelajaran;
	/**
	 * Sub mata pelajaran (komponen) yang diajarkan pada slot ke-2, kolom FK {@code sub_matapelajaran_2}.
	 */
	private SubMatapelajaran subMatapelajaran2;
	/**
	 * Sub mata pelajaran (komponen) yang diajarkan pada slot ke-3, kolom FK {@code sub_matapelajaran_3}.
	 */
	private SubMatapelajaran subMatapelajaran3;
	/**
	 * Sub mata pelajaran (komponen) yang diajarkan pada slot ke-4, kolom FK {@code sub_matapelajaran_4}.
	 */
	private SubMatapelajaran subMatapelajaran4;
	/**
	 * Sub mata pelajaran (komponen) yang diajarkan pada slot ke-5, kolom FK {@code sub_matapelajaran_5}.
	 */
	private SubMatapelajaran subMatapelajaran5;
	/**
	 * Sub mata pelajaran (komponen) yang diajarkan pada slot ke-6, kolom FK {@code sub_matapelajaran_6}.
	 */
	private SubMatapelajaran subMatapelajaran6;
	/**
	 * Sub mata pelajaran (komponen) yang diajarkan pada slot ke-7, kolom FK {@code sub_matapelajaran_7}.
	 */
	private SubMatapelajaran subMatapelajaran7;
	/**
	 * Sub mata pelajaran (komponen) yang diajarkan pada slot ke-8, kolom FK {@code sub_matapelajaran_8}.
	 */
	private SubMatapelajaran subMatapelajaran8;
	/**
	 * Sub mata pelajaran (komponen) yang diajarkan pada slot ke-9, kolom FK {@code sub_matapelajaran_9}.
	 */
	private SubMatapelajaran subMatapelajaran9;
	/**
	 * Sub mata pelajaran (komponen) yang diajarkan pada slot ke-10, kolom FK {@code sub_matapelajaran_10}.
	 */
	private SubMatapelajaran subMatapelajaran10;
	/**
	 * Sub mata pelajaran (komponen) yang diajarkan pada slot ke-11, kolom FK {@code sub_matapelajaran_11}.
	 */
	private SubMatapelajaran subMatapelajaran11;
	/**
	 * Sub mata pelajaran (komponen) yang diajarkan pada slot ke-12, kolom FK {@code sub_matapelajaran_12}.
	 */
	private SubMatapelajaran subMatapelajaran12;
	/**
	 * Sub mata pelajaran (komponen) yang diajarkan pada slot ke-13, kolom FK {@code sub_matapelajaran_13}.
	 */
	private SubMatapelajaran subMatapelajaran13;
	/**
	 * Sub mata pelajaran (komponen) yang diajarkan pada slot ke-14, kolom FK {@code sub_matapelajaran_14}.
	 */
	private SubMatapelajaran subMatapelajaran14;
	/**
	 * Sub mata pelajaran (komponen) yang diajarkan pada slot ke-15, kolom FK {@code sub_matapelajaran_15}.
	 */
	private SubMatapelajaran subMatapelajaran15;
	/**
	 * Sub mata pelajaran (komponen) yang diajarkan pada slot ke-16, kolom FK {@code sub_matapelajaran_16}.
	 */
	private SubMatapelajaran subMatapelajaran16;
	/**
	 * Sub mata pelajaran (komponen) yang diajarkan pada slot ke-17, kolom FK {@code sub_matapelajaran_17}.
	 */
	private SubMatapelajaran subMatapelajaran17;
	/**
	 * Sub mata pelajaran (komponen) yang diajarkan pada slot ke-18, kolom FK {@code sub_matapelajaran_18}.
	 */
	private SubMatapelajaran subMatapelajaran18;
	/**
	 * Sub mata pelajaran (komponen) yang diajarkan pada slot ke-19, kolom FK {@code sub_matapelajaran_19}.
	 */
	private SubMatapelajaran subMatapelajaran19;
	/**
	 * Sub mata pelajaran (komponen) yang diajarkan pada slot ke-20, kolom FK {@code sub_matapelajaran_20}.
	 */
	private SubMatapelajaran subMatapelajaran20;
	/**
	 * Sub mata pelajaran (komponen) yang diajarkan pada slot ke-21, kolom FK {@code sub_matapelajaran_21}.
	 */
	private SubMatapelajaran subMatapelajaran21;
	/**
	 * Sub mata pelajaran (komponen) yang diajarkan pada slot ke-22, kolom FK {@code sub_matapelajaran_22}.
	 */
	private SubMatapelajaran subMatapelajaran22;
	/**
	 * Sub mata pelajaran (komponen) yang diajarkan pada slot ke-23, kolom FK {@code sub_matapelajaran_23}.
	 */
	private SubMatapelajaran subMatapelajaran23;
	/**
	 * Sub mata pelajaran (komponen) yang diajarkan pada slot ke-24, kolom FK {@code sub_matapelajaran_24}.
	 */
	private SubMatapelajaran subMatapelajaran24;
	/**
	 * Sub mata pelajaran (komponen) yang diajarkan pada slot ke-25, kolom FK {@code sub_matapelajaran_25}.
	 */
	private SubMatapelajaran subMatapelajaran25;

	/**
	 * Catatan bebas atas penugasan mengajar ini, kolom {@code keterangan}.
	 *
	 * <p>Dideklarasikan ulang dari {@link ais.database.model.GeneralValueObject} agar ikut
	 * dipetakan. Ditampilkan sebagai kolom terakhir grid dan dapat dicari lewat filter
	 * <i>Keterangan</i> ({@code ilike} ANYWHERE).</p>
	 */
	private String keterangan;

	/**
	 * Merangkai seluruh slot terisi menjadi satu kalimat <i>"Hari: X, HH:mm s.d HH:mm, …"</i>.
	 *
	 * <p>Dipakai sebagai isi kolom <i>Jadwal</i> pada grid layar master
	 * ({@code GuruMengajarAction.GuruMengajarRenderer.render(...)}), yaitu satu kali untuk
	 * <b>setiap baris</b> yang tampil di halaman.</p>
	 *
	 * <p>Untuk tiap slot, hari dicetak bila {@code getHariN() != null}; jam mulai dan jam selesai
	 * diambil dari {@link JamPelajaran#getMulaiS()}/{@link JamPelajaran#getSampaiS()} dan diganti
	 * string kosong bila slot jamnya belum diisi. Pemisah antar slot adalah {@code ", "}, kecuali
	 * slot pertama yang tidak berawalan koma.</p>
	 *
	 * <p><b>Efek samping — penting.</b> Baris pertamanya berbunyi {@code sekolah = getSekolah()}.
	 * Karena {@link #getSekolah()} sendiri menimpa field {@code sekolah} dengan sekolah pemilik
	 * mata pelajaran, memanggil method ini <b>mengubah state instance</b> dan, pada instance yang
	 * terikat sesi Hibernate, dapat ikut tersimpan pada {@code flush} berikutnya. Jadi sekadar
	 * membuka daftar guru mengajar berpotensi menulis ulang kolom {@code sekolah_id} seluruh baris
	 * pada halaman itu beserta revisi Envers-nya.</p>
	 *
	 * <p><b>Bug TERVERIFIKASI:</b> blok untuk slot ke-19 disalin dari slot ke-18 tanpa mengganti
	 * angka, sehingga hasilnya <b>melewatkan slot 19</b> dan <b>mengulang slot 18 dua kali</b>.
	 * Datanya sendiri tetap utuh — {@link #populateJamPelajaran()} dan {@link #populateHari()}
	 * membaca slot 19 dengan benar — jadi kerusakannya murni pada tampilan.</p>
	 *
	 * @return ringkasan hari dan jam seluruh slot terisi; string kosong bila tidak ada slot terisi
	 */
	public String infoSimple() {
		sekolah = getSekolah();

		String harijam = "";

		if (getHari() != null) {
			harijam += ("Hari: " + getHari() + ", " + (getJamPelajaran() == null ? "" : getJamPelajaran().getMulaiS())
					+ " s.d " + (getJamPelajaran() == null ? "" : getJamPelajaran().getSampaiS()));
		}
		if (getHari2() != null) {
			harijam += (", Hari: " + getHari2() + ", "
					+ (getJamPelajaran2() == null ? "" : getJamPelajaran2().getMulaiS()) + " s.d "
					+ (getJamPelajaran2() == null ? "" : getJamPelajaran2().getSampaiS()));
		}

		if (getHari3() != null) {
			harijam += (", Hari: " + getHari3() + ", "
					+ (getJamPelajaran3() == null ? "" : getJamPelajaran3().getMulaiS()) + " s.d "
					+ (getJamPelajaran3() == null ? "" : getJamPelajaran3().getSampaiS()));
		}

		if (getHari4() != null) {
			harijam += (", Hari: " + getHari4() + ", "
					+ (getJamPelajaran4() == null ? "" : getJamPelajaran4().getMulaiS()) + " s.d "
					+ (getJamPelajaran4() == null ? "" : getJamPelajaran4().getSampaiS()));
		}

		if (getHari5() != null) {
			harijam += (", Hari: " + getHari5() + ", "
					+ (getJamPelajaran5() == null ? "" : getJamPelajaran5().getMulaiS()) + " s.d "
					+ (getJamPelajaran5() == null ? "" : getJamPelajaran5().getSampaiS()));
		}

		if (getHari6() != null) {
			harijam += (", Hari: " + getHari6() + ", "
					+ (getJamPelajaran6() == null ? "" : getJamPelajaran6().getMulaiS()) + " s.d "
					+ (getJamPelajaran6() == null ? "" : getJamPelajaran6().getSampaiS()));
		}

		if (getHari7() != null) {
			harijam += (", Hari: " + getHari7() + ", "
					+ (getJamPelajaran7() == null ? "" : getJamPelajaran7().getMulaiS()) + " s.d "
					+ (getJamPelajaran7() == null ? "" : getJamPelajaran7().getSampaiS()));
		}

		if (getHari8() != null) {
			harijam += (", Hari: " + getHari8() + ", "
					+ (getJamPelajaran8() == null ? "" : getJamPelajaran8().getMulaiS()) + " s.d "
					+ (getJamPelajaran8() == null ? "" : getJamPelajaran8().getSampaiS()));
		}

		if (getHari9() != null) {
			harijam += (", Hari: " + getHari9() + ", "
					+ (getJamPelajaran9() == null ? "" : getJamPelajaran9().getMulaiS()) + " s.d "
					+ (getJamPelajaran9() == null ? "" : getJamPelajaran9().getSampaiS()));
		}

		if (getHari10() != null) {
			harijam += (", Hari: " + getHari10() + ", "
					+ (getJamPelajaran10() == null ? "" : getJamPelajaran10().getMulaiS()) + " s.d "
					+ (getJamPelajaran10() == null ? "" : getJamPelajaran10().getSampaiS()));
		}

		if (getHari11() != null) {
			harijam += (", Hari: " + getHari11() + ", "
					+ (getJamPelajaran11() == null ? "" : getJamPelajaran11().getMulaiS()) + " s.d "
					+ (getJamPelajaran11() == null ? "" : getJamPelajaran11().getSampaiS()));
		}

		if (getHari12() != null) {
			harijam += (", Hari: " + getHari12() + ", "
					+ (getJamPelajaran12() == null ? "" : getJamPelajaran12().getMulaiS()) + " s.d "
					+ (getJamPelajaran12() == null ? "" : getJamPelajaran12().getSampaiS()));
		}

		if (getHari13() != null) {
			harijam += (", Hari: " + getHari13() + ", "
					+ (getJamPelajaran13() == null ? "" : getJamPelajaran13().getMulaiS()) + " s.d "
					+ (getJamPelajaran13() == null ? "" : getJamPelajaran13().getSampaiS()));
		}

		if (getHari14() != null) {
			harijam += (", Hari: " + getHari14() + ", "
					+ (getJamPelajaran14() == null ? "" : getJamPelajaran14().getMulaiS()) + " s.d "
					+ (getJamPelajaran14() == null ? "" : getJamPelajaran14().getSampaiS()));
		}

		if (getHari15() != null) {
			harijam += (", Hari: " + getHari15() + ", "
					+ (getJamPelajaran15() == null ? "" : getJamPelajaran15().getMulaiS()) + " s.d "
					+ (getJamPelajaran15() == null ? "" : getJamPelajaran15().getSampaiS()));
		}

		if (getHari16() != null) {
			harijam += (", Hari: " + getHari16() + ", "
					+ (getJamPelajaran16() == null ? "" : getJamPelajaran16().getMulaiS()) + " s.d "
					+ (getJamPelajaran16() == null ? "" : getJamPelajaran16().getSampaiS()));
		}

		if (getHari17() != null) {
			harijam += (", Hari: " + getHari17() + ", "
					+ (getJamPelajaran17() == null ? "" : getJamPelajaran17().getMulaiS()) + " s.d "
					+ (getJamPelajaran17() == null ? "" : getJamPelajaran17().getSampaiS()));
		}

		if (getHari18() != null) {
			harijam += (", Hari: " + getHari18() + ", "
					+ (getJamPelajaran18() == null ? "" : getJamPelajaran18().getMulaiS()) + " s.d "
					+ (getJamPelajaran18() == null ? "" : getJamPelajaran18().getSampaiS()));
		}

		if (getHari18() != null) {
			harijam += (", Hari: " + getHari18() + ", "
					+ (getJamPelajaran18() == null ? "" : getJamPelajaran18().getMulaiS()) + " s.d "
					+ (getJamPelajaran18() == null ? "" : getJamPelajaran18().getSampaiS()));
		}

		if (getHari20() != null) {
			harijam += (", Hari: " + getHari20() + ", "
					+ (getJamPelajaran20() == null ? "" : getJamPelajaran20().getMulaiS()) + " s.d "
					+ (getJamPelajaran20() == null ? "" : getJamPelajaran20().getSampaiS()));
		}

		if (getHari21() != null) {
			harijam += (", Hari: " + getHari21() + ", "
					+ (getJamPelajaran21() == null ? "" : getJamPelajaran21().getMulaiS()) + " s.d "
					+ (getJamPelajaran21() == null ? "" : getJamPelajaran21().getSampaiS()));
		}

		if (getHari22() != null) {
			harijam += (", Hari: " + getHari22() + ", "
					+ (getJamPelajaran22() == null ? "" : getJamPelajaran22().getMulaiS()) + " s.d "
					+ (getJamPelajaran22() == null ? "" : getJamPelajaran22().getSampaiS()));
		}

		if (getHari23() != null) {
			harijam += (", Hari: " + getHari23() + ", "
					+ (getJamPelajaran23() == null ? "" : getJamPelajaran23().getMulaiS()) + " s.d "
					+ (getJamPelajaran23() == null ? "" : getJamPelajaran23().getSampaiS()));
		}

		if (getHari24() != null) {
			harijam += (", Hari: " + getHari24() + ", "
					+ (getJamPelajaran24() == null ? "" : getJamPelajaran24().getMulaiS()) + " s.d "
					+ (getJamPelajaran24() == null ? "" : getJamPelajaran24().getSampaiS()));
		}

		if (getHari25() != null) {
			harijam += (", Hari: " + getHari25() + ", "
					+ (getJamPelajaran25() == null ? "" : getJamPelajaran25().getMulaiS()) + " s.d "
					+ (getJamPelajaran25() == null ? "" : getJamPelajaran25().getSampaiS()));
		}
		return harijam;
	}

	/**
	 * Representasi teks singkat berformat {@code "<id>_<matapelajaran>_<guru>"}.
	 *
	 * <p>Menimpa {@link ais.database.model.GeneralValueObject#toString()} yang berformat
	 * {@code "kode - nama"} — entity ini tidak memetakan {@code kode}/{@code nama} sehingga format
	 * induk tidak berguna di sini. Bagian mata pelajaran dan guru memakai {@code toString()}
	 * masing-masing, jadi keduanya berbentuk {@code "kode - nama"}.</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #getMatapelajaran()} dan {@link #getGuru()}, yang
	 * berarti proxy lazy ikut diselesaikan (dan pada sesi yang sudah tertutup dapat memicu
	 * {@code LazyInitializationException}).</p>
	 *
	 * @return penanda baris untuk keperluan log/debug
	 */
	public String toString() {
		return getId() + "_" + getMatapelajaran() + "_" + getGuru();
	}

	/**
	 * Mengumpulkan seluruh slot yang <b>lengkap</b> menjadi daftar pasangan
	 * {@code [JamPelajaran, String hari]}.
	 *
	 * <p>Sebuah slot ikut terkumpul hanya bila jam pelajarannya bukan {@code null} <b>dan</b>
	 * harinya bukan {@code null} sekaligus tidak kosong. Urutan hasilnya mengikuti nomor slot 1
	 * sampai 25, bukan urutan kronologis hari/jam.</p>
	 *
	 * <p>Satu-satunya pemanggil saat ini adalah
	 * {@code GuruMengajarAction.onCopyJadwalPelajaran(Event)}, yang membandingkan daftar ini dengan
	 * hasil {@code JadwalPelajaran.populateJamPelajaran()} untuk menentukan pertemuan mana yang
	 * belum tercatat, lalu mengisikannya ke slot kosong pertama.</p>
	 *
	 * <p><b>Efek samping:</b> memanggil ke-25 getter jam pelajaran, sehingga seluruh proxy lazi-nya
	 * diselesaikan (25 kemungkinan {@code SELECT} tambahan) dan ditulis balik ke field.</p>
	 *
	 * @return daftar pasangan {@code Object[]{jamPelajaran, hari}} untuk setiap slot yang lengkap;
	 *         daftar kosong bila tidak ada
	 */
	public List<Object[]> populateJamPelajaran() {
		List<Object[]> gurus = new ArrayList<Object[]>();

		if (getJamPelajaran() != null && getHari() != null && !getHari().isEmpty()) {
			gurus.add(new Object[] { getJamPelajaran(), getHari() });
		}
		if (getJamPelajaran2() != null && getHari2() != null && !getHari2().isEmpty()) {
			gurus.add(new Object[] { getJamPelajaran2(), getHari2() });
		}
		if (getJamPelajaran3() != null && getHari3() != null && !getHari3().isEmpty()) {
			gurus.add(new Object[] { getJamPelajaran3(), getHari3() });
		}
		if (getJamPelajaran4() != null && getHari4() != null && !getHari4().isEmpty()) {
			gurus.add(new Object[] { getJamPelajaran4(), getHari4() });
		}
		if (getJamPelajaran5() != null && getHari5() != null && !getHari5().isEmpty()) {
			gurus.add(new Object[] { getJamPelajaran5(), getHari5() });
		}
		if (getJamPelajaran6() != null && getHari6() != null && !getHari6().isEmpty()) {
			gurus.add(new Object[] { getJamPelajaran6(), getHari6() });
		}
		if (getJamPelajaran7() != null && getHari7() != null && !getHari7().isEmpty()) {
			gurus.add(new Object[] { getJamPelajaran7(), getHari7() });
		}
		if (getJamPelajaran8() != null && getHari8() != null && !getHari8().isEmpty()) {
			gurus.add(new Object[] { getJamPelajaran8(), getHari8() });
		}
		if (getJamPelajaran9() != null && getHari9() != null && !getHari9().isEmpty()) {
			gurus.add(new Object[] { getJamPelajaran9(), getHari9() });
		}
		if (getJamPelajaran10() != null && getHari10() != null && !getHari10().isEmpty()) {
			gurus.add(new Object[] { getJamPelajaran10(), getHari10() });
		}

		if (getJamPelajaran11() != null && getHari11() != null && !getHari11().isEmpty()) {
			gurus.add(new Object[] { getJamPelajaran11(), getHari11() });
		}

		if (getJamPelajaran12() != null && getHari12() != null && !getHari12().isEmpty()) {
			gurus.add(new Object[] { getJamPelajaran12(), getHari12() });
		}

		if (getJamPelajaran13() != null && getHari13() != null && !getHari13().isEmpty()) {
			gurus.add(new Object[] { getJamPelajaran13(), getHari13() });
		}

		if (getJamPelajaran14() != null && getHari14() != null && !getHari14().isEmpty()) {
			gurus.add(new Object[] { getJamPelajaran14(), getHari14() });
		}

		if (getJamPelajaran15() != null && getHari15() != null && !getHari15().isEmpty()) {
			gurus.add(new Object[] { getJamPelajaran15(), getHari15() });
		}

		if (getJamPelajaran16() != null && getHari16() != null && !getHari16().isEmpty()) {
			gurus.add(new Object[] { getJamPelajaran16(), getHari16() });
		}

		if (getJamPelajaran17() != null && getHari17() != null && !getHari17().isEmpty()) {
			gurus.add(new Object[] { getJamPelajaran17(), getHari17() });
		}

		if (getJamPelajaran18() != null && getHari18() != null && !getHari18().isEmpty()) {
			gurus.add(new Object[] { getJamPelajaran18(), getHari18() });
		}

		if (getJamPelajaran19() != null && getHari19() != null && !getHari19().isEmpty()) {
			gurus.add(new Object[] { getJamPelajaran19(), getHari19() });
		}

		if (getJamPelajaran20() != null && getHari20() != null && !getHari20().isEmpty()) {
			gurus.add(new Object[] { getJamPelajaran20(), getHari20() });
		}

		if (getJamPelajaran21() != null && getHari21() != null && !getHari21().isEmpty()) {
			gurus.add(new Object[] { getJamPelajaran21(), getHari21() });
		}

		if (getJamPelajaran22() != null && getHari22() != null && !getHari22().isEmpty()) {
			gurus.add(new Object[] { getJamPelajaran22(), getHari22() });
		}

		if (getJamPelajaran23() != null && getHari23() != null && !getHari23().isEmpty()) {
			gurus.add(new Object[] { getJamPelajaran23(), getHari23() });
		}

		if (getJamPelajaran24() != null && getHari24() != null && !getHari24().isEmpty()) {
			gurus.add(new Object[] { getJamPelajaran24(), getHari24() });
		}
		if (getJamPelajaran25() != null && getHari25() != null && !getHari25().isEmpty()) {
			gurus.add(new Object[] { getJamPelajaran25(), getHari25() });
		}
		return gurus;
	}

	/**
	 * Mengumpulkan nama hari dari seluruh slot yang harinya terisi.
	 *
	 * <p>Berbeda dari {@link #populateJamPelajaran()}, method ini <b>tidak</b> mensyaratkan jam
	 * pelajarannya ikut terisi — cukup {@code getHariN() != null}. Getter hari sendiri sudah
	 * menormalkan string kosong menjadi {@code null}, jadi hasilnya bebas dari entri kosong.
	 * Nilai duplikat tidak dibuang: mengajar dua jam berbeda pada hari yang sama menghasilkan dua
	 * entri "Senin".</p>
	 *
	 * <p>Dipakai {@code GuruMengajarAction.init(GuruMengajar)} untuk memutuskan baris slot mana
	 * yang perlu ditampilkan pada formulir: jumlah elemen daftar ini menentukan sampai slot ke
	 * berapa baris <i>"Hari dan Jam Pelajaran"</i> langsung terlihat, dan slot berikutnya baru
	 * muncul setelah tombol <i>"Tambah Jadwal Mengajar"</i> ditekan.</p>
	 *
	 * @return daftar nama hari dari slot 1 sampai 25 yang terisi; daftar kosong bila belum ada
	 */
	public List<String> populateHari() {
		List<String> gurus = new ArrayList<String>();

		if (getHari() != null) {
			gurus.add(getHari());
		}
		if (getHari2() != null) {
			gurus.add(getHari2());
		}
		if (getHari3() != null) {
			gurus.add(getHari3());
		}
		if (getHari4() != null) {
			gurus.add(getHari4());
		}
		if (getHari5() != null) {
			gurus.add(getHari5());
		}
		if (getHari6() != null) {
			gurus.add(getHari6());
		}

		if (getHari7() != null) {
			gurus.add(getHari7());
		}
		if (getHari8() != null) {
			gurus.add(getHari8());
		}
		if (getHari9() != null) {
			gurus.add(getHari9());
		}
		if (getHari10() != null) {
			gurus.add(getHari10());
		}

		if (getHari11() != null) {
			gurus.add(getHari11());
		}

		if (getHari12() != null) {
			gurus.add(getHari12());
		}

		if (getHari13() != null) {
			gurus.add(getHari13());
		}

		if (getHari14() != null) {
			gurus.add(getHari14());
		}

		if (getHari15() != null) {
			gurus.add(getHari15());
		}

		if (getHari16() != null) {
			gurus.add(getHari16());
		}

		if (getHari17() != null) {
			gurus.add(getHari17());
		}

		if (getHari18() != null) {
			gurus.add(getHari18());
		}

		if (getHari19() != null) {
			gurus.add(getHari19());
		}

		if (getHari20() != null) {
			gurus.add(getHari20());
		}

		if (getHari21() != null) {
			gurus.add(getHari21());
		}

		if (getHari22() != null) {
			gurus.add(getHari22());
		}

		if (getHari23() != null) {
			gurus.add(getHari23());
		}

		if (getHari24() != null) {
			gurus.add(getHari24());
		}

		if (getHari25() != null) {
			gurus.add(getHari25());
		}

		return gurus;
	}

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate.
	 *
	 * <p>Juga dipakai langsung oleh {@code GuruMengajarAction.onAdd(Event)} dan oleh penyalin
	 * jadwal ketika pasangan guru+mata pelajaran belum punya baris. Seluruh slot dibiarkan
	 * {@code null}; hanya {@link #tanggal_dirubah} yang sudah terisi lewat inisialisasi field.</p>
	 */
	public GuruMengajar() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Kolom {@code id} dianotasi {@code insertable = false} — nilainya dibangkitkan basis data
	 * ({@code IDENTITY}) dan baru terisi setelah {@code save}/{@code flush}. Layar memakai
	 * {@code getId() == null} sebagai penanda "baris baru" untuk memilih judul dialog dan untuk
	 * memutuskan {@code save} versus {@code update}.</p>
	 *
	 * @return id baris, atau {@code null} bila belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris ini.
	 *
	 * <p>Umumnya hanya dipanggil Hibernate. Perlu diketahui bahwa larik kolom ekspor/impor Excel
	 * layar ini menyertakan {@code "id"}, sehingga berkas unggahan dapat menyetel nilai ini dan
	 * dengan demikian menunjuk baris mana pun di instalasi.</p>
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan jam pelajaran pada <b>slot ke-1</b> (kolom {@code jam_pelajaran_id}).
	 *
	 * <p>Sejajar dengan {@link #getHari() hari} dan {@link #getSubMatapelajaran() subMatapelajaran}: ketiganya membentuk satu pertemuan mengajar.
	 * Proxy lazy diselesaikan lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan hasilnya ditulis kembali
	 * ke field (object yang sama, nilai kolom tidak berubah).</p>
	 *
	 * <p>Slot pertama diisi lebih dulu: penyalin jadwal maupun formulir selalu mencari slot
	 * kosong bernomor terkecil. Dengan kata lain slot yang terpakai selalu rapat dari 1 ke
	 * atas, kecuali bila pengguna mengosongkan slot di tengah lewat formulir.</p>
	 *
	 * @return jam pelajaran slot ke-1, atau {@code null} bila slot belum dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran_id")
	public JamPelajaran getJamPelajaran() {
		jamPelajaran = check(jamPelajaran);
		return this.jamPelajaran;
	}

	/**
	 * Menyetel jam pelajaran pada <b>slot ke-1</b>.
	 *
	 * <p>Tanpa validasi: tidak ada pemeriksaan bentrok dengan slot lain pada baris yang sama,
	 * tidak ada pemeriksaan bahwa jam pelajarannya milik sekolah yang sama, dan tidak ada
	 * keharusan {@link #setHari(String) hari} ikut terisi. Slot yang jamnya terisi tetapi
	 * harinya kosong akan diabaikan {@link #populateJamPelajaran()} dan {@link #infoSimple()}
	 * hanya menampilkan bagian jamnya bila harinya ada.</p>
	 *
	 * @param jamPelajaran jam pelajaran untuk slot ke-1; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran(JamPelajaran jamPelajaran) {
		this.jamPelajaran = jamPelajaran;
	}

	/**
	 * Mengembalikan mata pelajaran yang diajarkan (kolom {@code matapelajaran_id},
	 * {@code NOT NULL}).
	 *
	 * <p>Menyelesaikan proxy lazy lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan menuliskan hasilnya kembali
	 * ke field — object yang sama, jadi nilai kolom tidak berubah.</p>
	 *
	 * <p>Selain sebagai data, nilai ini menjadi <b>sumber</b> {@link #getSekolah()} dan, lewat
	 * sekolah, {@link #getYayasan()}. Juga menjadi kolom pertama grid dan kriteria pencarian
	 * <i>Mata Pelajaran</i> ({@code ilike} atas {@code matapelajaran.nama}).</p>
	 *
	 * @return mata pelajaran yang diajarkan; {@code null} hanya pada instance yang belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "matapelajaran_id", nullable = false)
	public Matapelajaran getMatapelajaran() {
		matapelajaran = check(matapelajaran);
		return this.matapelajaran;
	}

	/**
	 * Menyetel mata pelajaran yang diajarkan.
	 *
	 * <p>Wajib diisi: skema menandainya {@code nullable = false} dan formulir menolak simpan
	 * dengan pesan <i>"Matapelajaran harus diisi"</i>. Mengganti nilainya secara tidak langsung
	 * juga memindahkan baris ini ke sekolah dan yayasan lain, karena kedua kolom itu diturunkan
	 * dari sini pada getter-nya.</p>
	 *
	 * @param matapelajaran mata pelajaran yang diajarkan
	 */
	public void setMatapelajaran(Matapelajaran matapelajaran) {
		this.matapelajaran = matapelajaran;
	}

	/**
	 * Mengembalikan sekolah pemilik baris ini — <b>getter yang menulis balik</b>.
	 *
	 * <p>Alurnya: menyelesaikan proxy {@code sekolah}, lalu — bila {@link #getMatapelajaran()}
	 * tidak {@code null} — <b>menimpa</b> field dengan {@code matapelajaran.getSekolah()}. Karena
	 * {@code matapelajaran} berstatus {@code NOT NULL}, cabang penimpaan itu praktis selalu
	 * dijalankan.</p>
	 *
	 * <p><b>Efek samping:</b> nilai yang ditulis balik bukan sekadar hasil resolusi proxy melainkan
	 * <i>object lain</i>. Karena Hibernate memakai property access, perubahan itu terbaca oleh
	 * <i>dirty checking</i> dan tersimpan pada {@code flush} berikutnya — termasuk ketika getter
	 * dipanggil hanya untuk merender daftar (lewat {@link #infoSimple()}). Konsekuensinya:
	 * {@link #setSekolah(Sekolah)} dari formulir tidak pernah bertahan, dan kolom
	 * {@code sekolah_id} pada baris lama ikut disesuaikan diam-diam bila sekolah pemilik mata
	 * pelajarannya pernah berpindah.</p>
	 *
	 * @return sekolah pemilik mata pelajaran baris ini; {@code null} hanya bila mata pelajaran
	 *         maupun field sekolahnya kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		if (getMatapelajaran() != null) {
			sekolah = getMatapelajaran().getSekolah();
		}

		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik baris ini.
	 *
	 * <p>Menormalkan masukan: {@code null} <b>maupun</b> instance ber-{@code id} {@code null}
	 * (mis. pilihan "Semua" pada combo) sama-sama disimpan sebagai {@code null}, sehingga tidak
	 * ada object setengah jadi yang tersimpan.</p>
	 *
	 * <p><b>Perlu diingat:</b> nilai yang disetel di sini <b>tidak bertahan</b> — pemanggilan
	 * {@link #getSekolah()} berikutnya akan menimpanya dengan sekolah pemilik mata pelajaran.
	 * Formulir tetap memaksa pengguna memilih sekolah (<i>"Sekolah harus diisi"</i>) walau
	 * pilihannya efektif diabaikan.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau tanpa id disimpan sebagai {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik baris ini — <b>getter yang menulis balik, berjenjang</b>.
	 *
	 * <p>Memanggil {@link #getSekolah()} lebih dulu (yang sendirinya menimpa field
	 * {@code sekolah}), lalu bila sekolahnya ada <b>menimpa</b> field {@code yayasan} dengan
	 * {@code sekolah.getYayasan()}, dan terakhir menyelesaikan proxy hasilnya. Jadi satu
	 * pemanggilan getter ini dapat memutasi dua field sekaligus.</p>
	 *
	 * <p><b>Efek samping:</b> sama seperti {@link #getSekolah()} — perubahan terbaca dirty checking
	 * dan dapat tersimpan pada {@code flush} berikutnya, sehingga {@link #setYayasan(Yayasan)} dari
	 * formulir efektif diabaikan.</p>
	 *
	 * @return yayasan pemilik sekolah baris ini, atau {@code null} bila rantai mapel → sekolah →
	 *         yayasan terputus
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
	 * Menyetel yayasan pemilik baris ini.
	 *
	 * <p>Menormalkan masukan sama seperti {@link #setSekolah(Sekolah)}: {@code null} atau instance
	 * ber-{@code id} {@code null} disimpan sebagai {@code null}. Nilai yang disetel tidak bertahan
	 * karena {@link #getYayasan()} menurunkannya kembali dari sekolah.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau tanpa id disimpan sebagai {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan nama hari pertemuan pada <b>slot ke-1</b> (kolom {@code hari}).
	 *
	 * <p><b>Menormalkan hasil:</b> nilai {@code null} maupun yang hanya berisi spasi
	 * dikembalikan sebagai {@code null}, selebihnya dikembalikan dalam bentuk sudah
	 * di-{@code trim()}. Normalisasi ini hanya di sisi baca — {@link #setHari(String)}
	 * menyimpan apa adanya, sehingga isi kolom di basis data bisa saja mengandung spasi
	 * berlebih yang tidak pernah terlihat lewat getter.</p>
	 *
	 * <p>Kolom slot pertama ini satu-satunya yang dianotasi eksplisit:
	 * {@code @Column(name = "hari", nullable = true, length = 6)}. Enam karakter tepat
	 * menampung nilai terpanjang pada {@code Common.haris} ({@code "Jum'at"},
	 * {@code "Minggu"}) tanpa sisa ruang.</p>
	 *
	 * <p>Slot ini termasuk yang dijangkau filter <i>Hari</i> pada layar master (rantai
	 * {@code Restrictions.or(...)} di {@code initCriteria} mencakup {@code hari} sampai
	 * {@code hari10}).</p>
	 *
	 * @return nama hari slot ke-1 hasil {@code trim()}, atau {@code null} bila kosong
	 */
	@Column(name = "hari", nullable = true, length = 6)
	public String getHari() {
		return this.hari == null || hari.trim().isEmpty() ? null : hari.trim();
	}

	/**
	 * Menyetel nama hari pertemuan pada <b>slot ke-1</b>.
	 *
	 * <p>Menyimpan nilai <b>apa adanya</b>, tanpa {@code trim()} dan tanpa validasi bahwa isinya
	 * termasuk daftar {@code Common.haris}. Asimetri dengan {@link #getHari()} yang
	 * menormalkan hasil bacanya disengaja agar nilai lama tidak berubah saat baris disimpan
	 * ulang. Perlu diingat filter <i>Hari</i> di layar membandingkan dengan {@code Restrictions.eq}
	 * atas nilai <b>mentah</b> di kolom, sehingga nilai berspasi tidak akan pernah cocok.</p>
	 *
	 * @param hari nama hari untuk slot ke-1; {@code null} atau string kosong mengosongkan slot
	 */
	public void setHari(String hari) {
		this.hari = hari;
	}

	/**
	 * Mengembalikan guru pengampu (kolom {@code guru_id}).
	 *
	 * <p>Menyelesaikan proxy lazy lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan menuliskannya kembali ke
	 * field; object yang ditulis balik sama, jadi nilai kolom tidak berubah.</p>
	 *
	 * <p>Bersama {@link #getMatapelajaran()} membentuk kunci logis baris. Ditampilkan pada kolom
	 * <i>Guru</i> di grid dan dipakai sebagai kriteria pencarian; pemeriksaan duplikat di
	 * {@code onSave} juga memakai pasangan ini.</p>
	 *
	 * @return guru pengampu, atau {@code null} bila baris belum lengkap
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru_id")
	public Guru getGuru() {
		guru = check(guru);
		return guru;
	}

	/**
	 * Menyetel guru pengampu.
	 *
	 * <p>Tanpa normalisasi maupun validasi di sisi entity; pemeriksaan wajib-isi dilakukan
	 * formulir. Mengganti nilainya memindahkan seluruh 25 slot ke guru lain sekaligus — tidak ada
	 * mekanisme pemisahan riwayat, sehingga jejak siapa mengajar apa sebelum perubahan hanya
	 * tersisa di revisi Envers.</p>
	 *
	 * @param guru guru pengampu
	 */
	public void setGuru(Guru guru) {
		this.guru = guru;
	}

	/**
	 * Mengembalikan catatan bebas atas penugasan mengajar ini (kolom {@code keterangan}).
	 *
	 * <p>Menimpa {@link ais.database.model.GeneralValueObject#getKeterangan()} agar propertinya
	 * dipetakan sebagai kolom pada entity ini. Tidak dianotasi {@code @Column}, sehingga memakai
	 * nama dan panjang bawaan.</p>
	 *
	 * @return catatan bebas, atau {@code null} bila tidak diisi
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel catatan bebas atas penugasan mengajar ini.
	 *
	 * <p>Nilai disimpan apa adanya — string kosong tidak dinormalkan menjadi {@code null},
	 * berbeda dari perlakuan pada kolom hari.</p>
	 *
	 * @param keterangan catatan bebas
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan nama hari pertemuan pada <b>slot ke-2</b> (kolom {@code hari2}).
	 *
	 * <p><b>Menormalkan hasil:</b> nilai {@code null} maupun yang hanya berisi spasi
	 * dikembalikan sebagai {@code null}, selebihnya dikembalikan dalam bentuk sudah
	 * di-{@code trim()}. Normalisasi ini hanya di sisi baca — {@link #setHari2(String)}
	 * menyimpan apa adanya, sehingga isi kolom di basis data bisa saja mengandung spasi
	 * berlebih yang tidak pernah terlihat lewat getter.</p>
	 *
	 * <p>Slot ini termasuk yang dijangkau filter <i>Hari</i> pada layar master (rantai
	 * {@code Restrictions.or(...)} di {@code initCriteria} mencakup {@code hari} sampai
	 * {@code hari10}).</p>
	 *
	 * @return nama hari slot ke-2 hasil {@code trim()}, atau {@code null} bila kosong
	 */
	public String getHari2() {
		return this.hari2 == null || hari2.trim().isEmpty() ? null : hari2.trim();
	}

	/**
	 * Menyetel nama hari pertemuan pada <b>slot ke-2</b>.
	 *
	 * <p>Menyimpan nilai <b>apa adanya</b>, tanpa {@code trim()} dan tanpa validasi bahwa isinya
	 * termasuk daftar {@code Common.haris}. Asimetri dengan {@link #getHari2()} yang
	 * menormalkan hasil bacanya disengaja agar nilai lama tidak berubah saat baris disimpan
	 * ulang. Perlu diingat filter <i>Hari</i> di layar membandingkan dengan {@code Restrictions.eq}
	 * atas nilai <b>mentah</b> di kolom, sehingga nilai berspasi tidak akan pernah cocok.</p>
	 *
	 * @param hari2 nama hari untuk slot ke-2; {@code null} atau string kosong mengosongkan slot
	 */
	public void setHari2(String hari2) {
		this.hari2 = hari2;
	}

	/**
	 * Mengembalikan nama hari pertemuan pada <b>slot ke-3</b> (kolom {@code hari3}).
	 *
	 * <p><b>Menormalkan hasil:</b> nilai {@code null} maupun yang hanya berisi spasi
	 * dikembalikan sebagai {@code null}, selebihnya dikembalikan dalam bentuk sudah
	 * di-{@code trim()}. Normalisasi ini hanya di sisi baca — {@link #setHari3(String)}
	 * menyimpan apa adanya, sehingga isi kolom di basis data bisa saja mengandung spasi
	 * berlebih yang tidak pernah terlihat lewat getter.</p>
	 *
	 * <p>Slot ini termasuk yang dijangkau filter <i>Hari</i> pada layar master (rantai
	 * {@code Restrictions.or(...)} di {@code initCriteria} mencakup {@code hari} sampai
	 * {@code hari10}).</p>
	 *
	 * @return nama hari slot ke-3 hasil {@code trim()}, atau {@code null} bila kosong
	 */
	public String getHari3() {
		return this.hari3 == null || hari3.trim().isEmpty() ? null : hari3.trim();
	}

	/**
	 * Menyetel nama hari pertemuan pada <b>slot ke-3</b>.
	 *
	 * <p>Menyimpan nilai <b>apa adanya</b>, tanpa {@code trim()} dan tanpa validasi bahwa isinya
	 * termasuk daftar {@code Common.haris}. Asimetri dengan {@link #getHari3()} yang
	 * menormalkan hasil bacanya disengaja agar nilai lama tidak berubah saat baris disimpan
	 * ulang. Perlu diingat filter <i>Hari</i> di layar membandingkan dengan {@code Restrictions.eq}
	 * atas nilai <b>mentah</b> di kolom, sehingga nilai berspasi tidak akan pernah cocok.</p>
	 *
	 * @param hari3 nama hari untuk slot ke-3; {@code null} atau string kosong mengosongkan slot
	 */
	public void setHari3(String hari3) {
		this.hari3 = hari3;
	}

	/**
	 * Mengembalikan nama hari pertemuan pada <b>slot ke-4</b> (kolom {@code hari4}).
	 *
	 * <p><b>Menormalkan hasil:</b> nilai {@code null} maupun yang hanya berisi spasi
	 * dikembalikan sebagai {@code null}, selebihnya dikembalikan dalam bentuk sudah
	 * di-{@code trim()}. Normalisasi ini hanya di sisi baca — {@link #setHari4(String)}
	 * menyimpan apa adanya, sehingga isi kolom di basis data bisa saja mengandung spasi
	 * berlebih yang tidak pernah terlihat lewat getter.</p>
	 *
	 * <p>Slot ini termasuk yang dijangkau filter <i>Hari</i> pada layar master (rantai
	 * {@code Restrictions.or(...)} di {@code initCriteria} mencakup {@code hari} sampai
	 * {@code hari10}).</p>
	 *
	 * @return nama hari slot ke-4 hasil {@code trim()}, atau {@code null} bila kosong
	 */
	public String getHari4() {
		return this.hari4 == null || hari4.trim().isEmpty() ? null : hari4.trim();
	}

	/**
	 * Menyetel nama hari pertemuan pada <b>slot ke-4</b>.
	 *
	 * <p>Menyimpan nilai <b>apa adanya</b>, tanpa {@code trim()} dan tanpa validasi bahwa isinya
	 * termasuk daftar {@code Common.haris}. Asimetri dengan {@link #getHari4()} yang
	 * menormalkan hasil bacanya disengaja agar nilai lama tidak berubah saat baris disimpan
	 * ulang. Perlu diingat filter <i>Hari</i> di layar membandingkan dengan {@code Restrictions.eq}
	 * atas nilai <b>mentah</b> di kolom, sehingga nilai berspasi tidak akan pernah cocok.</p>
	 *
	 * @param hari4 nama hari untuk slot ke-4; {@code null} atau string kosong mengosongkan slot
	 */
	public void setHari4(String hari4) {
		this.hari4 = hari4;
	}

	/**
	 * Mengembalikan nama hari pertemuan pada <b>slot ke-5</b> (kolom {@code hari5}).
	 *
	 * <p><b>Menormalkan hasil:</b> nilai {@code null} maupun yang hanya berisi spasi
	 * dikembalikan sebagai {@code null}, selebihnya dikembalikan dalam bentuk sudah
	 * di-{@code trim()}. Normalisasi ini hanya di sisi baca — {@link #setHari5(String)}
	 * menyimpan apa adanya, sehingga isi kolom di basis data bisa saja mengandung spasi
	 * berlebih yang tidak pernah terlihat lewat getter.</p>
	 *
	 * <p>Slot ini termasuk yang dijangkau filter <i>Hari</i> pada layar master (rantai
	 * {@code Restrictions.or(...)} di {@code initCriteria} mencakup {@code hari} sampai
	 * {@code hari10}).</p>
	 *
	 * @return nama hari slot ke-5 hasil {@code trim()}, atau {@code null} bila kosong
	 */
	public String getHari5() {
		return this.hari5 == null || hari5.trim().isEmpty() ? null : hari5.trim();
	}

	/**
	 * Menyetel nama hari pertemuan pada <b>slot ke-5</b>.
	 *
	 * <p>Menyimpan nilai <b>apa adanya</b>, tanpa {@code trim()} dan tanpa validasi bahwa isinya
	 * termasuk daftar {@code Common.haris}. Asimetri dengan {@link #getHari5()} yang
	 * menormalkan hasil bacanya disengaja agar nilai lama tidak berubah saat baris disimpan
	 * ulang. Perlu diingat filter <i>Hari</i> di layar membandingkan dengan {@code Restrictions.eq}
	 * atas nilai <b>mentah</b> di kolom, sehingga nilai berspasi tidak akan pernah cocok.</p>
	 *
	 * @param hari5 nama hari untuk slot ke-5; {@code null} atau string kosong mengosongkan slot
	 */
	public void setHari5(String hari5) {
		this.hari5 = hari5;
	}

	/**
	 * Mengembalikan jam pelajaran pada <b>slot ke-2</b> (kolom {@code jam_pelajaran2_id}).
	 *
	 * <p>Sejajar dengan {@link #getHari2() hari2} dan {@link #getSubMatapelajaran2() subMatapelajaran2}: ketiganya membentuk satu pertemuan mengajar.
	 * Proxy lazy diselesaikan lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan hasilnya ditulis kembali
	 * ke field (object yang sama, nilai kolom tidak berubah).</p>
	 *
	 * @return jam pelajaran slot ke-2, atau {@code null} bila slot belum dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran2_id")
	public JamPelajaran getJamPelajaran2() {
		jamPelajaran2 = check(jamPelajaran2);
		return jamPelajaran2;
	}

	/**
	 * Menyetel jam pelajaran pada <b>slot ke-2</b>.
	 *
	 * <p>Tanpa validasi: tidak ada pemeriksaan bentrok dengan slot lain pada baris yang sama,
	 * tidak ada pemeriksaan bahwa jam pelajarannya milik sekolah yang sama, dan tidak ada
	 * keharusan {@link #setHari2(String) hari2} ikut terisi. Slot yang jamnya terisi tetapi
	 * harinya kosong akan diabaikan {@link #populateJamPelajaran()} dan {@link #infoSimple()}
	 * hanya menampilkan bagian jamnya bila harinya ada.</p>
	 *
	 * @param jamPelajaran2 jam pelajaran untuk slot ke-2; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran2(JamPelajaran jamPelajaran2) {
		this.jamPelajaran2 = jamPelajaran2;
	}

	/**
	 * Mengembalikan jam pelajaran pada <b>slot ke-3</b> (kolom {@code jam_pelajaran3_id}).
	 *
	 * <p>Sejajar dengan {@link #getHari3() hari3} dan {@link #getSubMatapelajaran3() subMatapelajaran3}: ketiganya membentuk satu pertemuan mengajar.
	 * Proxy lazy diselesaikan lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan hasilnya ditulis kembali
	 * ke field (object yang sama, nilai kolom tidak berubah).</p>
	 *
	 * @return jam pelajaran slot ke-3, atau {@code null} bila slot belum dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran3_id")
	public JamPelajaran getJamPelajaran3() {
		jamPelajaran3 = check(jamPelajaran3);
		return jamPelajaran3;
	}

	/**
	 * Menyetel jam pelajaran pada <b>slot ke-3</b>.
	 *
	 * <p>Tanpa validasi: tidak ada pemeriksaan bentrok dengan slot lain pada baris yang sama,
	 * tidak ada pemeriksaan bahwa jam pelajarannya milik sekolah yang sama, dan tidak ada
	 * keharusan {@link #setHari3(String) hari3} ikut terisi. Slot yang jamnya terisi tetapi
	 * harinya kosong akan diabaikan {@link #populateJamPelajaran()} dan {@link #infoSimple()}
	 * hanya menampilkan bagian jamnya bila harinya ada.</p>
	 *
	 * @param jamPelajaran3 jam pelajaran untuk slot ke-3; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran3(JamPelajaran jamPelajaran3) {
		this.jamPelajaran3 = jamPelajaran3;
	}

	/**
	 * Mengembalikan jam pelajaran pada <b>slot ke-5</b> (kolom {@code jam_pelajaran5_id}).
	 *
	 * <p>Sejajar dengan {@link #getHari5() hari5} dan {@link #getSubMatapelajaran5() subMatapelajaran5}: ketiganya membentuk satu pertemuan mengajar.
	 * Proxy lazy diselesaikan lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan hasilnya ditulis kembali
	 * ke field (object yang sama, nilai kolom tidak berubah).</p>
	 *
	 * <p><b>Catatan bentuk kode:</b> getter slot ke-5 dideklarasikan sebelum slot ke-4 pada
	 * berkas ini. Urutan itu tidak berpengaruh apa pun terhadap pemetaan kolom.</p>
	 *
	 * @return jam pelajaran slot ke-5, atau {@code null} bila slot belum dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran5_id")
	public JamPelajaran getJamPelajaran5() {
		jamPelajaran5 = check(jamPelajaran5);
		return jamPelajaran5;
	}

	/**
	 * Menyetel jam pelajaran pada <b>slot ke-5</b>.
	 *
	 * <p>Tanpa validasi: tidak ada pemeriksaan bentrok dengan slot lain pada baris yang sama,
	 * tidak ada pemeriksaan bahwa jam pelajarannya milik sekolah yang sama, dan tidak ada
	 * keharusan {@link #setHari5(String) hari5} ikut terisi. Slot yang jamnya terisi tetapi
	 * harinya kosong akan diabaikan {@link #populateJamPelajaran()} dan {@link #infoSimple()}
	 * hanya menampilkan bagian jamnya bila harinya ada.</p>
	 *
	 * @param jamPelajaran5 jam pelajaran untuk slot ke-5; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran5(JamPelajaran jamPelajaran5) {
		this.jamPelajaran5 = jamPelajaran5;
	}

	/**
	 * Mengembalikan jam pelajaran pada <b>slot ke-4</b> (kolom {@code jam_pelajaran4_id}).
	 *
	 * <p>Sejajar dengan {@link #getHari4() hari4} dan {@link #getSubMatapelajaran4() subMatapelajaran4}: ketiganya membentuk satu pertemuan mengajar.
	 * Proxy lazy diselesaikan lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan hasilnya ditulis kembali
	 * ke field (object yang sama, nilai kolom tidak berubah).</p>
	 *
	 * <p><b>Catatan bentuk kode:</b> getter slot ke-5 dideklarasikan sebelum slot ke-4 pada
	 * berkas ini. Urutan itu tidak berpengaruh apa pun terhadap pemetaan kolom.</p>
	 *
	 * @return jam pelajaran slot ke-4, atau {@code null} bila slot belum dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran4_id")
	public JamPelajaran getJamPelajaran4() {
		jamPelajaran4 = check(jamPelajaran4);
		return jamPelajaran4;
	}

	/**
	 * Menyetel jam pelajaran pada <b>slot ke-4</b>.
	 *
	 * <p>Tanpa validasi: tidak ada pemeriksaan bentrok dengan slot lain pada baris yang sama,
	 * tidak ada pemeriksaan bahwa jam pelajarannya milik sekolah yang sama, dan tidak ada
	 * keharusan {@link #setHari4(String) hari4} ikut terisi. Slot yang jamnya terisi tetapi
	 * harinya kosong akan diabaikan {@link #populateJamPelajaran()} dan {@link #infoSimple()}
	 * hanya menampilkan bagian jamnya bila harinya ada.</p>
	 *
	 * @param jamPelajaran4 jam pelajaran untuk slot ke-4; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran4(JamPelajaran jamPelajaran4) {
		this.jamPelajaran4 = jamPelajaran4;
	}

	/**
	 * Mengembalikan jam pelajaran pada <b>slot ke-6</b> (kolom {@code jam_pelajaran6_id}).
	 *
	 * <p>Sejajar dengan {@link #getHari6() hari6} dan {@link #getSubMatapelajaran6() subMatapelajaran6}: ketiganya membentuk satu pertemuan mengajar.
	 * Proxy lazy diselesaikan lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan hasilnya ditulis kembali
	 * ke field (object yang sama, nilai kolom tidak berubah).</p>
	 *
	 * @return jam pelajaran slot ke-6, atau {@code null} bila slot belum dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran6_id")
	public JamPelajaran getJamPelajaran6() {
		jamPelajaran6 = check(jamPelajaran6);
		return jamPelajaran6;
	}

	/**
	 * Menyetel jam pelajaran pada <b>slot ke-6</b>.
	 *
	 * <p>Tanpa validasi: tidak ada pemeriksaan bentrok dengan slot lain pada baris yang sama,
	 * tidak ada pemeriksaan bahwa jam pelajarannya milik sekolah yang sama, dan tidak ada
	 * keharusan {@link #setHari6(String) hari6} ikut terisi. Slot yang jamnya terisi tetapi
	 * harinya kosong akan diabaikan {@link #populateJamPelajaran()} dan {@link #infoSimple()}
	 * hanya menampilkan bagian jamnya bila harinya ada.</p>
	 *
	 * @param jamPelajaran6 jam pelajaran untuk slot ke-6; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran6(JamPelajaran jamPelajaran6) {
		this.jamPelajaran6 = jamPelajaran6;
	}

	/**
	 * Mengembalikan nama hari pertemuan pada <b>slot ke-6</b> (kolom {@code hari6}).
	 *
	 * <p><b>Menormalkan hasil:</b> nilai {@code null} maupun yang hanya berisi spasi
	 * dikembalikan sebagai {@code null}, selebihnya dikembalikan dalam bentuk sudah
	 * di-{@code trim()}. Normalisasi ini hanya di sisi baca — {@link #setHari6(String)}
	 * menyimpan apa adanya, sehingga isi kolom di basis data bisa saja mengandung spasi
	 * berlebih yang tidak pernah terlihat lewat getter.</p>
	 *
	 * <p>Slot ini termasuk yang dijangkau filter <i>Hari</i> pada layar master (rantai
	 * {@code Restrictions.or(...)} di {@code initCriteria} mencakup {@code hari} sampai
	 * {@code hari10}).</p>
	 *
	 * @return nama hari slot ke-6 hasil {@code trim()}, atau {@code null} bila kosong
	 */
	public String getHari6() {
		return this.hari6 == null || hari6.trim().isEmpty() ? null : hari6.trim();
	}

	/**
	 * Menyetel nama hari pertemuan pada <b>slot ke-6</b>.
	 *
	 * <p>Menyimpan nilai <b>apa adanya</b>, tanpa {@code trim()} dan tanpa validasi bahwa isinya
	 * termasuk daftar {@code Common.haris}. Asimetri dengan {@link #getHari6()} yang
	 * menormalkan hasil bacanya disengaja agar nilai lama tidak berubah saat baris disimpan
	 * ulang. Perlu diingat filter <i>Hari</i> di layar membandingkan dengan {@code Restrictions.eq}
	 * atas nilai <b>mentah</b> di kolom, sehingga nilai berspasi tidak akan pernah cocok.</p>
	 *
	 * @param hari6 nama hari untuk slot ke-6; {@code null} atau string kosong mengosongkan slot
	 */
	public void setHari6(String hari6) {
		this.hari6 = hari6;
	}

	/**
	 * Mengembalikan jam pelajaran pada <b>slot ke-7</b> (kolom {@code jam_pelajaran7_id}).
	 *
	 * <p>Sejajar dengan {@link #getHari7() hari7} dan {@link #getSubMatapelajaran7() subMatapelajaran7}: ketiganya membentuk satu pertemuan mengajar.
	 * Proxy lazy diselesaikan lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan hasilnya ditulis kembali
	 * ke field (object yang sama, nilai kolom tidak berubah).</p>
	 *
	 * @return jam pelajaran slot ke-7, atau {@code null} bila slot belum dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran7_id")
	public JamPelajaran getJamPelajaran7() {
		jamPelajaran7 = check(jamPelajaran7);
		return jamPelajaran7;
	}

	/**
	 * Menyetel jam pelajaran pada <b>slot ke-7</b>.
	 *
	 * <p>Tanpa validasi: tidak ada pemeriksaan bentrok dengan slot lain pada baris yang sama,
	 * tidak ada pemeriksaan bahwa jam pelajarannya milik sekolah yang sama, dan tidak ada
	 * keharusan {@link #setHari7(String) hari7} ikut terisi. Slot yang jamnya terisi tetapi
	 * harinya kosong akan diabaikan {@link #populateJamPelajaran()} dan {@link #infoSimple()}
	 * hanya menampilkan bagian jamnya bila harinya ada.</p>
	 *
	 * @param jamPelajaran7 jam pelajaran untuk slot ke-7; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran7(JamPelajaran jamPelajaran7) {
		this.jamPelajaran7 = jamPelajaran7;
	}

	/**
	 * Mengembalikan jam pelajaran pada <b>slot ke-8</b> (kolom {@code jam_pelajaran8_id}).
	 *
	 * <p>Sejajar dengan {@link #getHari8() hari8} dan {@link #getSubMatapelajaran8() subMatapelajaran8}: ketiganya membentuk satu pertemuan mengajar.
	 * Proxy lazy diselesaikan lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan hasilnya ditulis kembali
	 * ke field (object yang sama, nilai kolom tidak berubah).</p>
	 *
	 * @return jam pelajaran slot ke-8, atau {@code null} bila slot belum dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran8_id")
	public JamPelajaran getJamPelajaran8() {
		jamPelajaran8 = check(jamPelajaran8);
		return jamPelajaran8;
	}

	/**
	 * Menyetel jam pelajaran pada <b>slot ke-8</b>.
	 *
	 * <p>Tanpa validasi: tidak ada pemeriksaan bentrok dengan slot lain pada baris yang sama,
	 * tidak ada pemeriksaan bahwa jam pelajarannya milik sekolah yang sama, dan tidak ada
	 * keharusan {@link #setHari8(String) hari8} ikut terisi. Slot yang jamnya terisi tetapi
	 * harinya kosong akan diabaikan {@link #populateJamPelajaran()} dan {@link #infoSimple()}
	 * hanya menampilkan bagian jamnya bila harinya ada.</p>
	 *
	 * @param jamPelajaran8 jam pelajaran untuk slot ke-8; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran8(JamPelajaran jamPelajaran8) {
		this.jamPelajaran8 = jamPelajaran8;
	}

	/**
	 * Mengembalikan jam pelajaran pada <b>slot ke-9</b> (kolom {@code jam_pelajaran9_id}).
	 *
	 * <p>Sejajar dengan {@link #getHari9() hari9} dan {@link #getSubMatapelajaran9() subMatapelajaran9}: ketiganya membentuk satu pertemuan mengajar.
	 * Proxy lazy diselesaikan lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan hasilnya ditulis kembali
	 * ke field (object yang sama, nilai kolom tidak berubah).</p>
	 *
	 * @return jam pelajaran slot ke-9, atau {@code null} bila slot belum dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran9_id")
	public JamPelajaran getJamPelajaran9() {
		jamPelajaran9 = check(jamPelajaran9);
		return jamPelajaran9;
	}

	/**
	 * Menyetel jam pelajaran pada <b>slot ke-9</b>.
	 *
	 * <p>Tanpa validasi: tidak ada pemeriksaan bentrok dengan slot lain pada baris yang sama,
	 * tidak ada pemeriksaan bahwa jam pelajarannya milik sekolah yang sama, dan tidak ada
	 * keharusan {@link #setHari9(String) hari9} ikut terisi. Slot yang jamnya terisi tetapi
	 * harinya kosong akan diabaikan {@link #populateJamPelajaran()} dan {@link #infoSimple()}
	 * hanya menampilkan bagian jamnya bila harinya ada.</p>
	 *
	 * @param jamPelajaran9 jam pelajaran untuk slot ke-9; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran9(JamPelajaran jamPelajaran9) {
		this.jamPelajaran9 = jamPelajaran9;
	}

	/**
	 * Mengembalikan jam pelajaran pada <b>slot ke-10</b> (kolom {@code jam_pelajaran10_id}).
	 *
	 * <p>Sejajar dengan {@link #getHari10() hari10} dan {@link #getSubMatapelajaran10() subMatapelajaran10}: ketiganya membentuk satu pertemuan mengajar.
	 * Proxy lazy diselesaikan lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan hasilnya ditulis kembali
	 * ke field (object yang sama, nilai kolom tidak berubah).</p>
	 *
	 * @return jam pelajaran slot ke-10, atau {@code null} bila slot belum dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran10_id")
	public JamPelajaran getJamPelajaran10() {
		jamPelajaran10 = check(jamPelajaran10);
		return jamPelajaran10;
	}

	/**
	 * Menyetel jam pelajaran pada <b>slot ke-10</b>.
	 *
	 * <p>Tanpa validasi: tidak ada pemeriksaan bentrok dengan slot lain pada baris yang sama,
	 * tidak ada pemeriksaan bahwa jam pelajarannya milik sekolah yang sama, dan tidak ada
	 * keharusan {@link #setHari10(String) hari10} ikut terisi. Slot yang jamnya terisi tetapi
	 * harinya kosong akan diabaikan {@link #populateJamPelajaran()} dan {@link #infoSimple()}
	 * hanya menampilkan bagian jamnya bila harinya ada.</p>
	 *
	 * @param jamPelajaran10 jam pelajaran untuk slot ke-10; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran10(JamPelajaran jamPelajaran10) {
		this.jamPelajaran10 = jamPelajaran10;
	}

	/**
	 * Mengembalikan nama hari pertemuan pada <b>slot ke-7</b> (kolom {@code hari7}).
	 *
	 * <p><b>Menormalkan hasil:</b> nilai {@code null} maupun yang hanya berisi spasi
	 * dikembalikan sebagai {@code null}, selebihnya dikembalikan dalam bentuk sudah
	 * di-{@code trim()}. Normalisasi ini hanya di sisi baca — {@link #setHari7(String)}
	 * menyimpan apa adanya, sehingga isi kolom di basis data bisa saja mengandung spasi
	 * berlebih yang tidak pernah terlihat lewat getter.</p>
	 *
	 * <p>Slot ini termasuk yang dijangkau filter <i>Hari</i> pada layar master (rantai
	 * {@code Restrictions.or(...)} di {@code initCriteria} mencakup {@code hari} sampai
	 * {@code hari10}).</p>
	 *
	 * @return nama hari slot ke-7 hasil {@code trim()}, atau {@code null} bila kosong
	 */
	public String getHari7() {
		return this.hari7 == null || hari7.trim().isEmpty() ? null : hari7.trim();
	}

	/**
	 * Menyetel nama hari pertemuan pada <b>slot ke-7</b>.
	 *
	 * <p>Menyimpan nilai <b>apa adanya</b>, tanpa {@code trim()} dan tanpa validasi bahwa isinya
	 * termasuk daftar {@code Common.haris}. Asimetri dengan {@link #getHari7()} yang
	 * menormalkan hasil bacanya disengaja agar nilai lama tidak berubah saat baris disimpan
	 * ulang. Perlu diingat filter <i>Hari</i> di layar membandingkan dengan {@code Restrictions.eq}
	 * atas nilai <b>mentah</b> di kolom, sehingga nilai berspasi tidak akan pernah cocok.</p>
	 *
	 * @param hari7 nama hari untuk slot ke-7; {@code null} atau string kosong mengosongkan slot
	 */
	public void setHari7(String hari7) {
		this.hari7 = hari7;
	}

	/**
	 * Mengembalikan nama hari pertemuan pada <b>slot ke-8</b> (kolom {@code hari8}).
	 *
	 * <p><b>Menormalkan hasil:</b> nilai {@code null} maupun yang hanya berisi spasi
	 * dikembalikan sebagai {@code null}, selebihnya dikembalikan dalam bentuk sudah
	 * di-{@code trim()}. Normalisasi ini hanya di sisi baca — {@link #setHari8(String)}
	 * menyimpan apa adanya, sehingga isi kolom di basis data bisa saja mengandung spasi
	 * berlebih yang tidak pernah terlihat lewat getter.</p>
	 *
	 * <p>Slot ini termasuk yang dijangkau filter <i>Hari</i> pada layar master (rantai
	 * {@code Restrictions.or(...)} di {@code initCriteria} mencakup {@code hari} sampai
	 * {@code hari10}).</p>
	 *
	 * @return nama hari slot ke-8 hasil {@code trim()}, atau {@code null} bila kosong
	 */
	public String getHari8() {
		return this.hari8 == null || hari8.trim().isEmpty() ? null : hari8.trim();
	}

	/**
	 * Menyetel nama hari pertemuan pada <b>slot ke-8</b>.
	 *
	 * <p>Menyimpan nilai <b>apa adanya</b>, tanpa {@code trim()} dan tanpa validasi bahwa isinya
	 * termasuk daftar {@code Common.haris}. Asimetri dengan {@link #getHari8()} yang
	 * menormalkan hasil bacanya disengaja agar nilai lama tidak berubah saat baris disimpan
	 * ulang. Perlu diingat filter <i>Hari</i> di layar membandingkan dengan {@code Restrictions.eq}
	 * atas nilai <b>mentah</b> di kolom, sehingga nilai berspasi tidak akan pernah cocok.</p>
	 *
	 * @param hari8 nama hari untuk slot ke-8; {@code null} atau string kosong mengosongkan slot
	 */
	public void setHari8(String hari8) {
		this.hari8 = hari8;
	}

	/**
	 * Mengembalikan nama hari pertemuan pada <b>slot ke-9</b> (kolom {@code hari9}).
	 *
	 * <p><b>Menormalkan hasil:</b> nilai {@code null} maupun yang hanya berisi spasi
	 * dikembalikan sebagai {@code null}, selebihnya dikembalikan dalam bentuk sudah
	 * di-{@code trim()}. Normalisasi ini hanya di sisi baca — {@link #setHari9(String)}
	 * menyimpan apa adanya, sehingga isi kolom di basis data bisa saja mengandung spasi
	 * berlebih yang tidak pernah terlihat lewat getter.</p>
	 *
	 * <p>Slot ini termasuk yang dijangkau filter <i>Hari</i> pada layar master (rantai
	 * {@code Restrictions.or(...)} di {@code initCriteria} mencakup {@code hari} sampai
	 * {@code hari10}).</p>
	 *
	 * @return nama hari slot ke-9 hasil {@code trim()}, atau {@code null} bila kosong
	 */
	public String getHari9() {
		return this.hari9 == null || hari9.trim().isEmpty() ? null : hari9.trim();
	}

	/**
	 * Menyetel nama hari pertemuan pada <b>slot ke-9</b>.
	 *
	 * <p>Menyimpan nilai <b>apa adanya</b>, tanpa {@code trim()} dan tanpa validasi bahwa isinya
	 * termasuk daftar {@code Common.haris}. Asimetri dengan {@link #getHari9()} yang
	 * menormalkan hasil bacanya disengaja agar nilai lama tidak berubah saat baris disimpan
	 * ulang. Perlu diingat filter <i>Hari</i> di layar membandingkan dengan {@code Restrictions.eq}
	 * atas nilai <b>mentah</b> di kolom, sehingga nilai berspasi tidak akan pernah cocok.</p>
	 *
	 * @param hari9 nama hari untuk slot ke-9; {@code null} atau string kosong mengosongkan slot
	 */
	public void setHari9(String hari9) {
		this.hari9 = hari9;
	}

	/**
	 * Mengembalikan nama hari pertemuan pada <b>slot ke-10</b> (kolom {@code hari10}).
	 *
	 * <p><b>Menormalkan hasil:</b> nilai {@code null} maupun yang hanya berisi spasi
	 * dikembalikan sebagai {@code null}, selebihnya dikembalikan dalam bentuk sudah
	 * di-{@code trim()}. Normalisasi ini hanya di sisi baca — {@link #setHari10(String)}
	 * menyimpan apa adanya, sehingga isi kolom di basis data bisa saja mengandung spasi
	 * berlebih yang tidak pernah terlihat lewat getter.</p>
	 *
	 * <p>Slot ini termasuk yang dijangkau filter <i>Hari</i> pada layar master (rantai
	 * {@code Restrictions.or(...)} di {@code initCriteria} mencakup {@code hari} sampai
	 * {@code hari10}).</p>
	 *
	 * @return nama hari slot ke-10 hasil {@code trim()}, atau {@code null} bila kosong
	 */
	public String getHari10() {
		return this.hari10 == null || hari10.trim().isEmpty() ? null : hari10.trim();
	}

	/**
	 * Menyetel nama hari pertemuan pada <b>slot ke-10</b>.
	 *
	 * <p>Menyimpan nilai <b>apa adanya</b>, tanpa {@code trim()} dan tanpa validasi bahwa isinya
	 * termasuk daftar {@code Common.haris}. Asimetri dengan {@link #getHari10()} yang
	 * menormalkan hasil bacanya disengaja agar nilai lama tidak berubah saat baris disimpan
	 * ulang. Perlu diingat filter <i>Hari</i> di layar membandingkan dengan {@code Restrictions.eq}
	 * atas nilai <b>mentah</b> di kolom, sehingga nilai berspasi tidak akan pernah cocok.</p>
	 *
	 * @param hari10 nama hari untuk slot ke-10; {@code null} atau string kosong mengosongkan slot
	 */
	public void setHari10(String hari10) {
		this.hari10 = hari10;
	}

	/**
	 * Mengembalikan jam pelajaran pada <b>slot ke-11</b> (kolom {@code jam_pelajaran11_id}).
	 *
	 * <p>Sejajar dengan {@link #getHari11() hari11} dan {@link #getSubMatapelajaran11() subMatapelajaran11}: ketiganya membentuk satu pertemuan mengajar.
	 * Proxy lazy diselesaikan lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan hasilnya ditulis kembali
	 * ke field (object yang sama, nilai kolom tidak berubah).</p>
	 *
	 * @return jam pelajaran slot ke-11, atau {@code null} bila slot belum dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran11_id")
	public JamPelajaran getJamPelajaran11() {
		jamPelajaran11 = check(jamPelajaran11);
		return jamPelajaran11;
	}

	/**
	 * Menyetel jam pelajaran pada <b>slot ke-11</b>.
	 *
	 * <p>Tanpa validasi: tidak ada pemeriksaan bentrok dengan slot lain pada baris yang sama,
	 * tidak ada pemeriksaan bahwa jam pelajarannya milik sekolah yang sama, dan tidak ada
	 * keharusan {@link #setHari11(String) hari11} ikut terisi. Slot yang jamnya terisi tetapi
	 * harinya kosong akan diabaikan {@link #populateJamPelajaran()} dan {@link #infoSimple()}
	 * hanya menampilkan bagian jamnya bila harinya ada.</p>
	 *
	 * @param jamPelajaran11 jam pelajaran untuk slot ke-11; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran11(JamPelajaran jamPelajaran11) {
		this.jamPelajaran11 = jamPelajaran11;
	}

	/**
	 * Mengembalikan jam pelajaran pada <b>slot ke-12</b> (kolom {@code jam_pelajaran12_id}).
	 *
	 * <p>Sejajar dengan {@link #getHari12() hari12} dan {@link #getSubMatapelajaran12() subMatapelajaran12}: ketiganya membentuk satu pertemuan mengajar.
	 * Proxy lazy diselesaikan lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan hasilnya ditulis kembali
	 * ke field (object yang sama, nilai kolom tidak berubah).</p>
	 *
	 * @return jam pelajaran slot ke-12, atau {@code null} bila slot belum dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran12_id")
	public JamPelajaran getJamPelajaran12() {
		jamPelajaran12 = check(jamPelajaran12);
		return jamPelajaran12;
	}

	/**
	 * Menyetel jam pelajaran pada <b>slot ke-12</b>.
	 *
	 * <p>Tanpa validasi: tidak ada pemeriksaan bentrok dengan slot lain pada baris yang sama,
	 * tidak ada pemeriksaan bahwa jam pelajarannya milik sekolah yang sama, dan tidak ada
	 * keharusan {@link #setHari12(String) hari12} ikut terisi. Slot yang jamnya terisi tetapi
	 * harinya kosong akan diabaikan {@link #populateJamPelajaran()} dan {@link #infoSimple()}
	 * hanya menampilkan bagian jamnya bila harinya ada.</p>
	 *
	 * @param jamPelajaran12 jam pelajaran untuk slot ke-12; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran12(JamPelajaran jamPelajaran12) {
		this.jamPelajaran12 = jamPelajaran12;
	}

	/**
	 * Mengembalikan nama hari pertemuan pada <b>slot ke-11</b> (kolom {@code hari11}).
	 *
	 * <p><b>Menormalkan hasil:</b> nilai {@code null} maupun yang hanya berisi spasi
	 * dikembalikan sebagai {@code null}, selebihnya dikembalikan dalam bentuk sudah
	 * di-{@code trim()}. Normalisasi ini hanya di sisi baca — {@link #setHari11(String)}
	 * menyimpan apa adanya, sehingga isi kolom di basis data bisa saja mengandung spasi
	 * berlebih yang tidak pernah terlihat lewat getter.</p>
	 *
	 * <p><b>Tidak terjangkau filter <i>Hari</i></b> pada layar master: rantai
	 * {@code Restrictions.or(...)} di {@code GuruMengajarAction.initCriteria(boolean)}
	 * berhenti pada {@code hari10}. Baris yang hanya mengisi slot 11–25 hilang dari daftar
	 * begitu filter hari dipakai.</p>
	 *
	 * @return nama hari slot ke-11 hasil {@code trim()}, atau {@code null} bila kosong
	 */
	public String getHari11() {
		return this.hari11 == null || hari11.trim().isEmpty() ? null : hari11.trim();
	}

	/**
	 * Menyetel nama hari pertemuan pada <b>slot ke-11</b>.
	 *
	 * <p>Menyimpan nilai <b>apa adanya</b>, tanpa {@code trim()} dan tanpa validasi bahwa isinya
	 * termasuk daftar {@code Common.haris}. Asimetri dengan {@link #getHari11()} yang
	 * menormalkan hasil bacanya disengaja agar nilai lama tidak berubah saat baris disimpan
	 * ulang. Perlu diingat filter <i>Hari</i> di layar membandingkan dengan {@code Restrictions.eq}
	 * atas nilai <b>mentah</b> di kolom, sehingga nilai berspasi tidak akan pernah cocok.</p>
	 *
	 * @param hari11 nama hari untuk slot ke-11; {@code null} atau string kosong mengosongkan slot
	 */
	public void setHari11(String hari11) {
		this.hari11 = hari11;
	}

	/**
	 * Mengembalikan nama hari pertemuan pada <b>slot ke-12</b> (kolom {@code hari12}).
	 *
	 * <p><b>Menormalkan hasil:</b> nilai {@code null} maupun yang hanya berisi spasi
	 * dikembalikan sebagai {@code null}, selebihnya dikembalikan dalam bentuk sudah
	 * di-{@code trim()}. Normalisasi ini hanya di sisi baca — {@link #setHari12(String)}
	 * menyimpan apa adanya, sehingga isi kolom di basis data bisa saja mengandung spasi
	 * berlebih yang tidak pernah terlihat lewat getter.</p>
	 *
	 * <p><b>Tidak terjangkau filter <i>Hari</i></b> pada layar master: rantai
	 * {@code Restrictions.or(...)} di {@code GuruMengajarAction.initCriteria(boolean)}
	 * berhenti pada {@code hari10}. Baris yang hanya mengisi slot 11–25 hilang dari daftar
	 * begitu filter hari dipakai.</p>
	 *
	 * @return nama hari slot ke-12 hasil {@code trim()}, atau {@code null} bila kosong
	 */
	public String getHari12() {
		return this.hari12 == null || hari12.trim().isEmpty() ? null : hari12.trim();
	}

	/**
	 * Menyetel nama hari pertemuan pada <b>slot ke-12</b>.
	 *
	 * <p>Menyimpan nilai <b>apa adanya</b>, tanpa {@code trim()} dan tanpa validasi bahwa isinya
	 * termasuk daftar {@code Common.haris}. Asimetri dengan {@link #getHari12()} yang
	 * menormalkan hasil bacanya disengaja agar nilai lama tidak berubah saat baris disimpan
	 * ulang. Perlu diingat filter <i>Hari</i> di layar membandingkan dengan {@code Restrictions.eq}
	 * atas nilai <b>mentah</b> di kolom, sehingga nilai berspasi tidak akan pernah cocok.</p>
	 *
	 * @param hari12 nama hari untuk slot ke-12; {@code null} atau string kosong mengosongkan slot
	 */
	public void setHari12(String hari12) {
		this.hari12 = hari12;
	}

	/**
	 * Mengembalikan sub mata pelajaran (komponen) yang diajarkan pada <b>slot ke-1</b>
	 * (kolom {@code sub_matapelajaran}).
	 *
	 * <p>Opsional dan sejajar dengan {@link #getHari() hari} serta
	 * {@link #getJamPelajaran() jamPelajaran} pada slot yang sama. Proxy lazy diselesaikan
	 * lewat {@link ais.database.model.GeneralValueObject#check(Object)} dan ditulis kembali ke
	 * field (object yang sama).</p>
	 *
	 * <p><b>Tanpa pembaca hilir.</b> Penelusuran repo tidak menemukan satu pun laporan, rapor,
	 * cetakan, atau API yang membaca ke-25 slot sub mata pelajaran entity ini; nilainya hanya
	 * diisi lewat formulir. Ditambah bug penulisan {@code NULL} saat penyimpanan ulang yang
	 * tercatat pada Javadoc kelas dan {@link SubMatapelajaran}, lapis ini praktis yatim
	 * fungsional.</p>
	 *
	 * @return sub mata pelajaran slot ke-1, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran")
	public SubMatapelajaran getSubMatapelajaran() {
		subMatapelajaran = check(subMatapelajaran);
		return subMatapelajaran;
	}

	/**
	 * Menyetel sub mata pelajaran (komponen) pada <b>slot ke-1</b>.
	 *
	 * <p>Tanpa validasi bahwa sub mata pelajaran yang disetel benar-benar milik
	 * {@link #getMatapelajaran() matapelajaran} baris ini — pembatasan itu hanya dilakukan
	 * combo di formulir. <b>Tidak pernah dipanggil</b> oleh
	 * {@code GuruMengajarAction.onSave(Event)}, sehingga kolomnya kembali {@code NULL} setiap
	 * kali baris disimpan lewat layar (lihat Javadoc kelas).</p>
	 *
	 * @param subMatapelajaran komponen mata pelajaran untuk slot ke-1; {@code null}
	 *         mengosongkan slot
	 */
	public void setSubMatapelajaran(SubMatapelajaran subMatapelajaran) {
		this.subMatapelajaran = subMatapelajaran;
	}

	/**
	 * Mengembalikan sub mata pelajaran (komponen) yang diajarkan pada <b>slot ke-2</b>
	 * (kolom {@code sub_matapelajaran_2}).
	 *
	 * <p>Opsional dan sejajar dengan {@link #getHari2() hari2} serta
	 * {@link #getJamPelajaran2() jamPelajaran2} pada slot yang sama. Proxy lazy diselesaikan
	 * lewat {@link ais.database.model.GeneralValueObject#check(Object)} dan ditulis kembali ke
	 * field (object yang sama).</p>
	 *
	 * @return sub mata pelajaran slot ke-2, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_2")
	public SubMatapelajaran getSubMatapelajaran2() {
		subMatapelajaran2 = check(subMatapelajaran2);
		return subMatapelajaran2;
	}

	/**
	 * Menyetel sub mata pelajaran (komponen) pada <b>slot ke-2</b>.
	 *
	 * <p>Tanpa validasi bahwa sub mata pelajaran yang disetel benar-benar milik
	 * {@link #getMatapelajaran() matapelajaran} baris ini — pembatasan itu hanya dilakukan
	 * combo di formulir. <b>Tidak pernah dipanggil</b> oleh
	 * {@code GuruMengajarAction.onSave(Event)}, sehingga kolomnya kembali {@code NULL} setiap
	 * kali baris disimpan lewat layar (lihat Javadoc kelas).</p>
	 *
	 * @param subMatapelajaran2 komponen mata pelajaran untuk slot ke-2; {@code null}
	 *         mengosongkan slot
	 */
	public void setSubMatapelajaran2(SubMatapelajaran subMatapelajaran2) {
		this.subMatapelajaran2 = subMatapelajaran2;
	}

	/**
	 * Mengembalikan sub mata pelajaran (komponen) yang diajarkan pada <b>slot ke-3</b>
	 * (kolom {@code sub_matapelajaran_3}).
	 *
	 * <p>Opsional dan sejajar dengan {@link #getHari3() hari3} serta
	 * {@link #getJamPelajaran3() jamPelajaran3} pada slot yang sama. Proxy lazy diselesaikan
	 * lewat {@link ais.database.model.GeneralValueObject#check(Object)} dan ditulis kembali ke
	 * field (object yang sama).</p>
	 *
	 * @return sub mata pelajaran slot ke-3, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_3")
	public SubMatapelajaran getSubMatapelajaran3() {
		subMatapelajaran3 = check(subMatapelajaran3);
		return subMatapelajaran3;
	}

	/**
	 * Menyetel sub mata pelajaran (komponen) pada <b>slot ke-3</b>.
	 *
	 * <p>Tanpa validasi bahwa sub mata pelajaran yang disetel benar-benar milik
	 * {@link #getMatapelajaran() matapelajaran} baris ini — pembatasan itu hanya dilakukan
	 * combo di formulir. <b>Tidak pernah dipanggil</b> oleh
	 * {@code GuruMengajarAction.onSave(Event)}, sehingga kolomnya kembali {@code NULL} setiap
	 * kali baris disimpan lewat layar (lihat Javadoc kelas).</p>
	 *
	 * @param subMatapelajaran3 komponen mata pelajaran untuk slot ke-3; {@code null}
	 *         mengosongkan slot
	 */
	public void setSubMatapelajaran3(SubMatapelajaran subMatapelajaran3) {
		this.subMatapelajaran3 = subMatapelajaran3;
	}

	/**
	 * Mengembalikan sub mata pelajaran (komponen) yang diajarkan pada <b>slot ke-4</b>
	 * (kolom {@code sub_matapelajaran_4}).
	 *
	 * <p>Opsional dan sejajar dengan {@link #getHari4() hari4} serta
	 * {@link #getJamPelajaran4() jamPelajaran4} pada slot yang sama. Proxy lazy diselesaikan
	 * lewat {@link ais.database.model.GeneralValueObject#check(Object)} dan ditulis kembali ke
	 * field (object yang sama).</p>
	 *
	 * @return sub mata pelajaran slot ke-4, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_4")
	public SubMatapelajaran getSubMatapelajaran4() {
		subMatapelajaran4 = check(subMatapelajaran4);
		return subMatapelajaran4;
	}

	/**
	 * Menyetel sub mata pelajaran (komponen) pada <b>slot ke-4</b>.
	 *
	 * <p>Tanpa validasi bahwa sub mata pelajaran yang disetel benar-benar milik
	 * {@link #getMatapelajaran() matapelajaran} baris ini — pembatasan itu hanya dilakukan
	 * combo di formulir. <b>Tidak pernah dipanggil</b> oleh
	 * {@code GuruMengajarAction.onSave(Event)}, sehingga kolomnya kembali {@code NULL} setiap
	 * kali baris disimpan lewat layar (lihat Javadoc kelas).</p>
	 *
	 * @param subMatapelajaran4 komponen mata pelajaran untuk slot ke-4; {@code null}
	 *         mengosongkan slot
	 */
	public void setSubMatapelajaran4(SubMatapelajaran subMatapelajaran4) {
		this.subMatapelajaran4 = subMatapelajaran4;
	}

	/**
	 * Mengembalikan sub mata pelajaran (komponen) yang diajarkan pada <b>slot ke-5</b>
	 * (kolom {@code sub_matapelajaran_5}).
	 *
	 * <p>Opsional dan sejajar dengan {@link #getHari5() hari5} serta
	 * {@link #getJamPelajaran5() jamPelajaran5} pada slot yang sama. Proxy lazy diselesaikan
	 * lewat {@link ais.database.model.GeneralValueObject#check(Object)} dan ditulis kembali ke
	 * field (object yang sama).</p>
	 *
	 * @return sub mata pelajaran slot ke-5, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_5")
	public SubMatapelajaran getSubMatapelajaran5() {
		subMatapelajaran5 = check(subMatapelajaran5);
		return subMatapelajaran5;
	}

	/**
	 * Menyetel sub mata pelajaran (komponen) pada <b>slot ke-5</b>.
	 *
	 * <p>Tanpa validasi bahwa sub mata pelajaran yang disetel benar-benar milik
	 * {@link #getMatapelajaran() matapelajaran} baris ini — pembatasan itu hanya dilakukan
	 * combo di formulir. <b>Tidak pernah dipanggil</b> oleh
	 * {@code GuruMengajarAction.onSave(Event)}, sehingga kolomnya kembali {@code NULL} setiap
	 * kali baris disimpan lewat layar (lihat Javadoc kelas).</p>
	 *
	 * @param subMatapelajaran5 komponen mata pelajaran untuk slot ke-5; {@code null}
	 *         mengosongkan slot
	 */
	public void setSubMatapelajaran5(SubMatapelajaran subMatapelajaran5) {
		this.subMatapelajaran5 = subMatapelajaran5;
	}

	/**
	 * Mengembalikan sub mata pelajaran (komponen) yang diajarkan pada <b>slot ke-6</b>
	 * (kolom {@code sub_matapelajaran_6}).
	 *
	 * <p>Opsional dan sejajar dengan {@link #getHari6() hari6} serta
	 * {@link #getJamPelajaran6() jamPelajaran6} pada slot yang sama. Proxy lazy diselesaikan
	 * lewat {@link ais.database.model.GeneralValueObject#check(Object)} dan ditulis kembali ke
	 * field (object yang sama).</p>
	 *
	 * @return sub mata pelajaran slot ke-6, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_6")
	public SubMatapelajaran getSubMatapelajaran6() {
		subMatapelajaran6 = check(subMatapelajaran6);
		return subMatapelajaran6;
	}

	/**
	 * Menyetel sub mata pelajaran (komponen) pada <b>slot ke-6</b>.
	 *
	 * <p>Tanpa validasi bahwa sub mata pelajaran yang disetel benar-benar milik
	 * {@link #getMatapelajaran() matapelajaran} baris ini — pembatasan itu hanya dilakukan
	 * combo di formulir. <b>Tidak pernah dipanggil</b> oleh
	 * {@code GuruMengajarAction.onSave(Event)}, sehingga kolomnya kembali {@code NULL} setiap
	 * kali baris disimpan lewat layar (lihat Javadoc kelas).</p>
	 *
	 * @param subMatapelajaran6 komponen mata pelajaran untuk slot ke-6; {@code null}
	 *         mengosongkan slot
	 */
	public void setSubMatapelajaran6(SubMatapelajaran subMatapelajaran6) {
		this.subMatapelajaran6 = subMatapelajaran6;
	}

	/**
	 * Mengembalikan sub mata pelajaran (komponen) yang diajarkan pada <b>slot ke-7</b>
	 * (kolom {@code sub_matapelajaran_7}).
	 *
	 * <p>Opsional dan sejajar dengan {@link #getHari7() hari7} serta
	 * {@link #getJamPelajaran7() jamPelajaran7} pada slot yang sama. Proxy lazy diselesaikan
	 * lewat {@link ais.database.model.GeneralValueObject#check(Object)} dan ditulis kembali ke
	 * field (object yang sama).</p>
	 *
	 * @return sub mata pelajaran slot ke-7, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_7")
	public SubMatapelajaran getSubMatapelajaran7() {
		subMatapelajaran7 = check(subMatapelajaran7);
		return subMatapelajaran7;
	}

	/**
	 * Menyetel sub mata pelajaran (komponen) pada <b>slot ke-7</b>.
	 *
	 * <p>Tanpa validasi bahwa sub mata pelajaran yang disetel benar-benar milik
	 * {@link #getMatapelajaran() matapelajaran} baris ini — pembatasan itu hanya dilakukan
	 * combo di formulir. <b>Tidak pernah dipanggil</b> oleh
	 * {@code GuruMengajarAction.onSave(Event)}, sehingga kolomnya kembali {@code NULL} setiap
	 * kali baris disimpan lewat layar (lihat Javadoc kelas).</p>
	 *
	 * @param subMatapelajaran7 komponen mata pelajaran untuk slot ke-7; {@code null}
	 *         mengosongkan slot
	 */
	public void setSubMatapelajaran7(SubMatapelajaran subMatapelajaran7) {
		this.subMatapelajaran7 = subMatapelajaran7;
	}

	/**
	 * Mengembalikan sub mata pelajaran (komponen) yang diajarkan pada <b>slot ke-8</b>
	 * (kolom {@code sub_matapelajaran_8}).
	 *
	 * <p>Opsional dan sejajar dengan {@link #getHari8() hari8} serta
	 * {@link #getJamPelajaran8() jamPelajaran8} pada slot yang sama. Proxy lazy diselesaikan
	 * lewat {@link ais.database.model.GeneralValueObject#check(Object)} dan ditulis kembali ke
	 * field (object yang sama).</p>
	 *
	 * @return sub mata pelajaran slot ke-8, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_8")
	public SubMatapelajaran getSubMatapelajaran8() {
		subMatapelajaran8 = check(subMatapelajaran8);
		return subMatapelajaran8;
	}

	/**
	 * Menyetel sub mata pelajaran (komponen) pada <b>slot ke-8</b>.
	 *
	 * <p>Tanpa validasi bahwa sub mata pelajaran yang disetel benar-benar milik
	 * {@link #getMatapelajaran() matapelajaran} baris ini — pembatasan itu hanya dilakukan
	 * combo di formulir. <b>Tidak pernah dipanggil</b> oleh
	 * {@code GuruMengajarAction.onSave(Event)}, sehingga kolomnya kembali {@code NULL} setiap
	 * kali baris disimpan lewat layar (lihat Javadoc kelas).</p>
	 *
	 * @param subMatapelajaran8 komponen mata pelajaran untuk slot ke-8; {@code null}
	 *         mengosongkan slot
	 */
	public void setSubMatapelajaran8(SubMatapelajaran subMatapelajaran8) {
		this.subMatapelajaran8 = subMatapelajaran8;
	}

	/**
	 * Mengembalikan sub mata pelajaran (komponen) yang diajarkan pada <b>slot ke-9</b>
	 * (kolom {@code sub_matapelajaran_9}).
	 *
	 * <p>Opsional dan sejajar dengan {@link #getHari9() hari9} serta
	 * {@link #getJamPelajaran9() jamPelajaran9} pada slot yang sama. Proxy lazy diselesaikan
	 * lewat {@link ais.database.model.GeneralValueObject#check(Object)} dan ditulis kembali ke
	 * field (object yang sama).</p>
	 *
	 * @return sub mata pelajaran slot ke-9, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_9")
	public SubMatapelajaran getSubMatapelajaran9() {
		subMatapelajaran9 = check(subMatapelajaran9);
		return subMatapelajaran9;
	}

	/**
	 * Menyetel sub mata pelajaran (komponen) pada <b>slot ke-9</b>.
	 *
	 * <p>Tanpa validasi bahwa sub mata pelajaran yang disetel benar-benar milik
	 * {@link #getMatapelajaran() matapelajaran} baris ini — pembatasan itu hanya dilakukan
	 * combo di formulir. <b>Tidak pernah dipanggil</b> oleh
	 * {@code GuruMengajarAction.onSave(Event)}, sehingga kolomnya kembali {@code NULL} setiap
	 * kali baris disimpan lewat layar (lihat Javadoc kelas).</p>
	 *
	 * @param subMatapelajaran9 komponen mata pelajaran untuk slot ke-9; {@code null}
	 *         mengosongkan slot
	 */
	public void setSubMatapelajaran9(SubMatapelajaran subMatapelajaran9) {
		this.subMatapelajaran9 = subMatapelajaran9;
	}

	/**
	 * Mengembalikan sub mata pelajaran (komponen) yang diajarkan pada <b>slot ke-10</b>
	 * (kolom {@code sub_matapelajaran_10}).
	 *
	 * <p>Opsional dan sejajar dengan {@link #getHari10() hari10} serta
	 * {@link #getJamPelajaran10() jamPelajaran10} pada slot yang sama. Proxy lazy diselesaikan
	 * lewat {@link ais.database.model.GeneralValueObject#check(Object)} dan ditulis kembali ke
	 * field (object yang sama).</p>
	 *
	 * @return sub mata pelajaran slot ke-10, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_10")
	public SubMatapelajaran getSubMatapelajaran10() {
		subMatapelajaran10 = check(subMatapelajaran10);
		return subMatapelajaran10;
	}

	/**
	 * Menyetel sub mata pelajaran (komponen) pada <b>slot ke-10</b>.
	 *
	 * <p>Tanpa validasi bahwa sub mata pelajaran yang disetel benar-benar milik
	 * {@link #getMatapelajaran() matapelajaran} baris ini — pembatasan itu hanya dilakukan
	 * combo di formulir. <b>Tidak pernah dipanggil</b> oleh
	 * {@code GuruMengajarAction.onSave(Event)}, sehingga kolomnya kembali {@code NULL} setiap
	 * kali baris disimpan lewat layar (lihat Javadoc kelas).</p>
	 *
	 * @param subMatapelajaran10 komponen mata pelajaran untuk slot ke-10; {@code null}
	 *         mengosongkan slot
	 */
	public void setSubMatapelajaran10(SubMatapelajaran subMatapelajaran10) {
		this.subMatapelajaran10 = subMatapelajaran10;
	}

	/**
	 * Mengembalikan sub mata pelajaran (komponen) yang diajarkan pada <b>slot ke-11</b>
	 * (kolom {@code sub_matapelajaran_11}).
	 *
	 * <p>Opsional dan sejajar dengan {@link #getHari11() hari11} serta
	 * {@link #getJamPelajaran11() jamPelajaran11} pada slot yang sama. Proxy lazy diselesaikan
	 * lewat {@link ais.database.model.GeneralValueObject#check(Object)} dan ditulis kembali ke
	 * field (object yang sama).</p>
	 *
	 * @return sub mata pelajaran slot ke-11, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_11")
	public SubMatapelajaran getSubMatapelajaran11() {
		subMatapelajaran11 = check(subMatapelajaran11);
		return subMatapelajaran11;
	}

	/**
	 * Menyetel sub mata pelajaran (komponen) pada <b>slot ke-11</b>.
	 *
	 * <p>Tanpa validasi bahwa sub mata pelajaran yang disetel benar-benar milik
	 * {@link #getMatapelajaran() matapelajaran} baris ini — pembatasan itu hanya dilakukan
	 * combo di formulir. <b>Tidak pernah dipanggil</b> oleh
	 * {@code GuruMengajarAction.onSave(Event)}, sehingga kolomnya kembali {@code NULL} setiap
	 * kali baris disimpan lewat layar (lihat Javadoc kelas).</p>
	 *
	 * @param subMatapelajaran11 komponen mata pelajaran untuk slot ke-11; {@code null}
	 *         mengosongkan slot
	 */
	public void setSubMatapelajaran11(SubMatapelajaran subMatapelajaran11) {
		this.subMatapelajaran11 = subMatapelajaran11;
	}

	/**
	 * Mengembalikan sub mata pelajaran (komponen) yang diajarkan pada <b>slot ke-12</b>
	 * (kolom {@code sub_matapelajaran_12}).
	 *
	 * <p>Opsional dan sejajar dengan {@link #getHari12() hari12} serta
	 * {@link #getJamPelajaran12() jamPelajaran12} pada slot yang sama. Proxy lazy diselesaikan
	 * lewat {@link ais.database.model.GeneralValueObject#check(Object)} dan ditulis kembali ke
	 * field (object yang sama).</p>
	 *
	 * @return sub mata pelajaran slot ke-12, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_12")
	public SubMatapelajaran getSubMatapelajaran12() {
		subMatapelajaran12 = check(subMatapelajaran12);
		return subMatapelajaran12;
	}

	/**
	 * Menyetel sub mata pelajaran (komponen) pada <b>slot ke-12</b>.
	 *
	 * <p>Tanpa validasi bahwa sub mata pelajaran yang disetel benar-benar milik
	 * {@link #getMatapelajaran() matapelajaran} baris ini — pembatasan itu hanya dilakukan
	 * combo di formulir. <b>Tidak pernah dipanggil</b> oleh
	 * {@code GuruMengajarAction.onSave(Event)}, sehingga kolomnya kembali {@code NULL} setiap
	 * kali baris disimpan lewat layar (lihat Javadoc kelas).</p>
	 *
	 * @param subMatapelajaran12 komponen mata pelajaran untuk slot ke-12; {@code null}
	 *         mengosongkan slot
	 */
	public void setSubMatapelajaran12(SubMatapelajaran subMatapelajaran12) {
		this.subMatapelajaran12 = subMatapelajaran12;
	}

	/**
	 * Mengembalikan jam pelajaran pada <b>slot ke-13</b> (kolom {@code jam_pelajaran13_id}).
	 *
	 * <p>Sejajar dengan {@link #getHari13() hari13} dan {@link #getSubMatapelajaran13() subMatapelajaran13}: ketiganya membentuk satu pertemuan mengajar.
	 * Proxy lazy diselesaikan lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan hasilnya ditulis kembali
	 * ke field (object yang sama, nilai kolom tidak berubah).</p>
	 *
	 * @return jam pelajaran slot ke-13, atau {@code null} bila slot belum dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran13_id")
	public JamPelajaran getJamPelajaran13() {
		jamPelajaran13 = check(jamPelajaran13);
		return jamPelajaran13;
	}

	/**
	 * Menyetel jam pelajaran pada <b>slot ke-13</b>.
	 *
	 * <p>Tanpa validasi: tidak ada pemeriksaan bentrok dengan slot lain pada baris yang sama,
	 * tidak ada pemeriksaan bahwa jam pelajarannya milik sekolah yang sama, dan tidak ada
	 * keharusan {@link #setHari13(String) hari13} ikut terisi. Slot yang jamnya terisi tetapi
	 * harinya kosong akan diabaikan {@link #populateJamPelajaran()} dan {@link #infoSimple()}
	 * hanya menampilkan bagian jamnya bila harinya ada.</p>
	 *
	 * @param jamPelajaran13 jam pelajaran untuk slot ke-13; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran13(JamPelajaran jamPelajaran13) {
		this.jamPelajaran13 = jamPelajaran13;
	}

	/**
	 * Mengembalikan jam pelajaran pada <b>slot ke-14</b> (kolom {@code jam_pelajaran14_id}).
	 *
	 * <p>Sejajar dengan {@link #getHari14() hari14} dan {@link #getSubMatapelajaran14() subMatapelajaran14}: ketiganya membentuk satu pertemuan mengajar.
	 * Proxy lazy diselesaikan lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan hasilnya ditulis kembali
	 * ke field (object yang sama, nilai kolom tidak berubah).</p>
	 *
	 * @return jam pelajaran slot ke-14, atau {@code null} bila slot belum dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran14_id")
	public JamPelajaran getJamPelajaran14() {
		jamPelajaran14 = check(jamPelajaran14);
		return jamPelajaran14;
	}

	/**
	 * Menyetel jam pelajaran pada <b>slot ke-14</b>.
	 *
	 * <p>Tanpa validasi: tidak ada pemeriksaan bentrok dengan slot lain pada baris yang sama,
	 * tidak ada pemeriksaan bahwa jam pelajarannya milik sekolah yang sama, dan tidak ada
	 * keharusan {@link #setHari14(String) hari14} ikut terisi. Slot yang jamnya terisi tetapi
	 * harinya kosong akan diabaikan {@link #populateJamPelajaran()} dan {@link #infoSimple()}
	 * hanya menampilkan bagian jamnya bila harinya ada.</p>
	 *
	 * @param jamPelajaran14 jam pelajaran untuk slot ke-14; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran14(JamPelajaran jamPelajaran14) {
		this.jamPelajaran14 = jamPelajaran14;
	}

	/**
	 * Mengembalikan jam pelajaran pada <b>slot ke-15</b> (kolom {@code jam_pelajaran15_id}).
	 *
	 * <p>Sejajar dengan {@link #getHari15() hari15} dan {@link #getSubMatapelajaran15() subMatapelajaran15}: ketiganya membentuk satu pertemuan mengajar.
	 * Proxy lazy diselesaikan lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan hasilnya ditulis kembali
	 * ke field (object yang sama, nilai kolom tidak berubah).</p>
	 *
	 * @return jam pelajaran slot ke-15, atau {@code null} bila slot belum dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran15_id")
	public JamPelajaran getJamPelajaran15() {
		jamPelajaran15 = check(jamPelajaran15);
		return jamPelajaran15;
	}

	/**
	 * Menyetel jam pelajaran pada <b>slot ke-15</b>.
	 *
	 * <p>Tanpa validasi: tidak ada pemeriksaan bentrok dengan slot lain pada baris yang sama,
	 * tidak ada pemeriksaan bahwa jam pelajarannya milik sekolah yang sama, dan tidak ada
	 * keharusan {@link #setHari15(String) hari15} ikut terisi. Slot yang jamnya terisi tetapi
	 * harinya kosong akan diabaikan {@link #populateJamPelajaran()} dan {@link #infoSimple()}
	 * hanya menampilkan bagian jamnya bila harinya ada.</p>
	 *
	 * @param jamPelajaran15 jam pelajaran untuk slot ke-15; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran15(JamPelajaran jamPelajaran15) {
		this.jamPelajaran15 = jamPelajaran15;
	}

	/**
	 * Mengembalikan jam pelajaran pada <b>slot ke-16</b> (kolom {@code jam_pelajaran16_id}).
	 *
	 * <p>Sejajar dengan {@link #getHari16() hari16} dan {@link #getSubMatapelajaran16() subMatapelajaran16}: ketiganya membentuk satu pertemuan mengajar.
	 * Proxy lazy diselesaikan lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan hasilnya ditulis kembali
	 * ke field (object yang sama, nilai kolom tidak berubah).</p>
	 *
	 * @return jam pelajaran slot ke-16, atau {@code null} bila slot belum dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran16_id")
	public JamPelajaran getJamPelajaran16() {
		jamPelajaran16 = check(jamPelajaran16);
		return jamPelajaran16;
	}

	/**
	 * Menyetel jam pelajaran pada <b>slot ke-16</b>.
	 *
	 * <p>Tanpa validasi: tidak ada pemeriksaan bentrok dengan slot lain pada baris yang sama,
	 * tidak ada pemeriksaan bahwa jam pelajarannya milik sekolah yang sama, dan tidak ada
	 * keharusan {@link #setHari16(String) hari16} ikut terisi. Slot yang jamnya terisi tetapi
	 * harinya kosong akan diabaikan {@link #populateJamPelajaran()} dan {@link #infoSimple()}
	 * hanya menampilkan bagian jamnya bila harinya ada.</p>
	 *
	 * @param jamPelajaran16 jam pelajaran untuk slot ke-16; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran16(JamPelajaran jamPelajaran16) {
		this.jamPelajaran16 = jamPelajaran16;
	}

	/**
	 * Mengembalikan jam pelajaran pada <b>slot ke-17</b> (kolom {@code jam_pelajaran17_id}).
	 *
	 * <p>Sejajar dengan {@link #getHari17() hari17} dan {@link #getSubMatapelajaran17() subMatapelajaran17}: ketiganya membentuk satu pertemuan mengajar.
	 * Proxy lazy diselesaikan lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan hasilnya ditulis kembali
	 * ke field (object yang sama, nilai kolom tidak berubah).</p>
	 *
	 * @return jam pelajaran slot ke-17, atau {@code null} bila slot belum dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran17_id")
	public JamPelajaran getJamPelajaran17() {
		jamPelajaran17 = check(jamPelajaran17);
		return jamPelajaran17;
	}

	/**
	 * Menyetel jam pelajaran pada <b>slot ke-17</b>.
	 *
	 * <p>Tanpa validasi: tidak ada pemeriksaan bentrok dengan slot lain pada baris yang sama,
	 * tidak ada pemeriksaan bahwa jam pelajarannya milik sekolah yang sama, dan tidak ada
	 * keharusan {@link #setHari17(String) hari17} ikut terisi. Slot yang jamnya terisi tetapi
	 * harinya kosong akan diabaikan {@link #populateJamPelajaran()} dan {@link #infoSimple()}
	 * hanya menampilkan bagian jamnya bila harinya ada.</p>
	 *
	 * @param jamPelajaran17 jam pelajaran untuk slot ke-17; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran17(JamPelajaran jamPelajaran17) {
		this.jamPelajaran17 = jamPelajaran17;
	}

	/**
	 * Mengembalikan jam pelajaran pada <b>slot ke-18</b> (kolom {@code jam_pelajaran18_id}).
	 *
	 * <p>Sejajar dengan {@link #getHari18() hari18} dan {@link #getSubMatapelajaran18() subMatapelajaran18}: ketiganya membentuk satu pertemuan mengajar.
	 * Proxy lazy diselesaikan lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan hasilnya ditulis kembali
	 * ke field (object yang sama, nilai kolom tidak berubah).</p>
	 *
	 * @return jam pelajaran slot ke-18, atau {@code null} bila slot belum dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran18_id")
	public JamPelajaran getJamPelajaran18() {
		jamPelajaran18 = check(jamPelajaran18);
		return jamPelajaran18;
	}

	/**
	 * Menyetel jam pelajaran pada <b>slot ke-18</b>.
	 *
	 * <p>Tanpa validasi: tidak ada pemeriksaan bentrok dengan slot lain pada baris yang sama,
	 * tidak ada pemeriksaan bahwa jam pelajarannya milik sekolah yang sama, dan tidak ada
	 * keharusan {@link #setHari18(String) hari18} ikut terisi. Slot yang jamnya terisi tetapi
	 * harinya kosong akan diabaikan {@link #populateJamPelajaran()} dan {@link #infoSimple()}
	 * hanya menampilkan bagian jamnya bila harinya ada.</p>
	 *
	 * @param jamPelajaran18 jam pelajaran untuk slot ke-18; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran18(JamPelajaran jamPelajaran18) {
		this.jamPelajaran18 = jamPelajaran18;
	}

	/**
	 * Mengembalikan jam pelajaran pada <b>slot ke-19</b> (kolom {@code jam_pelajaran19_id}).
	 *
	 * <p>Sejajar dengan {@link #getHari19() hari19} dan {@link #getSubMatapelajaran19() subMatapelajaran19}: ketiganya membentuk satu pertemuan mengajar.
	 * Proxy lazy diselesaikan lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan hasilnya ditulis kembali
	 * ke field (object yang sama, nilai kolom tidak berubah).</p>
	 *
	 * <p><b>Catatan:</b> slot ke-19 tidak pernah muncul pada ringkasan {@link #infoSimple()}
	 * karena bug penomoran di sana (lihat Javadoc method tersebut). Datanya tetap terbaca
	 * normal lewat getter ini dan lewat {@link #populateJamPelajaran()}.</p>
	 *
	 * @return jam pelajaran slot ke-19, atau {@code null} bila slot belum dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran19_id")
	public JamPelajaran getJamPelajaran19() {
		jamPelajaran19 = check(jamPelajaran19);
		return jamPelajaran19;
	}

	/**
	 * Menyetel jam pelajaran pada <b>slot ke-19</b>.
	 *
	 * <p>Tanpa validasi: tidak ada pemeriksaan bentrok dengan slot lain pada baris yang sama,
	 * tidak ada pemeriksaan bahwa jam pelajarannya milik sekolah yang sama, dan tidak ada
	 * keharusan {@link #setHari19(String) hari19} ikut terisi. Slot yang jamnya terisi tetapi
	 * harinya kosong akan diabaikan {@link #populateJamPelajaran()} dan {@link #infoSimple()}
	 * hanya menampilkan bagian jamnya bila harinya ada.</p>
	 *
	 * @param jamPelajaran19 jam pelajaran untuk slot ke-19; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran19(JamPelajaran jamPelajaran19) {
		this.jamPelajaran19 = jamPelajaran19;
	}

	/**
	 * Mengembalikan jam pelajaran pada <b>slot ke-20</b> (kolom {@code jam_pelajaran20_id}).
	 *
	 * <p>Sejajar dengan {@link #getHari20() hari20} dan {@link #getSubMatapelajaran20() subMatapelajaran20}: ketiganya membentuk satu pertemuan mengajar.
	 * Proxy lazy diselesaikan lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan hasilnya ditulis kembali
	 * ke field (object yang sama, nilai kolom tidak berubah).</p>
	 *
	 * @return jam pelajaran slot ke-20, atau {@code null} bila slot belum dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran20_id")
	public JamPelajaran getJamPelajaran20() {
		jamPelajaran20 = check(jamPelajaran20);
		return jamPelajaran20;
	}

	/**
	 * Menyetel jam pelajaran pada <b>slot ke-20</b>.
	 *
	 * <p>Tanpa validasi: tidak ada pemeriksaan bentrok dengan slot lain pada baris yang sama,
	 * tidak ada pemeriksaan bahwa jam pelajarannya milik sekolah yang sama, dan tidak ada
	 * keharusan {@link #setHari20(String) hari20} ikut terisi. Slot yang jamnya terisi tetapi
	 * harinya kosong akan diabaikan {@link #populateJamPelajaran()} dan {@link #infoSimple()}
	 * hanya menampilkan bagian jamnya bila harinya ada.</p>
	 *
	 * @param jamPelajaran20 jam pelajaran untuk slot ke-20; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran20(JamPelajaran jamPelajaran20) {
		this.jamPelajaran20 = jamPelajaran20;
	}

	/**
	 * Mengembalikan jam pelajaran pada <b>slot ke-21</b> (kolom {@code jam_pelajaran21_id}).
	 *
	 * <p>Sejajar dengan {@link #getHari21() hari21} dan {@link #getSubMatapelajaran21() subMatapelajaran21}: ketiganya membentuk satu pertemuan mengajar.
	 * Proxy lazy diselesaikan lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan hasilnya ditulis kembali
	 * ke field (object yang sama, nilai kolom tidak berubah).</p>
	 *
	 * @return jam pelajaran slot ke-21, atau {@code null} bila slot belum dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran21_id")
	public JamPelajaran getJamPelajaran21() {
		jamPelajaran21 = check(jamPelajaran21);
		return jamPelajaran21;
	}

	/**
	 * Menyetel jam pelajaran pada <b>slot ke-21</b>.
	 *
	 * <p>Tanpa validasi: tidak ada pemeriksaan bentrok dengan slot lain pada baris yang sama,
	 * tidak ada pemeriksaan bahwa jam pelajarannya milik sekolah yang sama, dan tidak ada
	 * keharusan {@link #setHari21(String) hari21} ikut terisi. Slot yang jamnya terisi tetapi
	 * harinya kosong akan diabaikan {@link #populateJamPelajaran()} dan {@link #infoSimple()}
	 * hanya menampilkan bagian jamnya bila harinya ada.</p>
	 *
	 * @param jamPelajaran21 jam pelajaran untuk slot ke-21; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran21(JamPelajaran jamPelajaran21) {
		this.jamPelajaran21 = jamPelajaran21;
	}

	/**
	 * Mengembalikan jam pelajaran pada <b>slot ke-22</b> (kolom {@code jam_pelajaran22_id}).
	 *
	 * <p>Sejajar dengan {@link #getHari22() hari22} dan {@link #getSubMatapelajaran22() subMatapelajaran22}: ketiganya membentuk satu pertemuan mengajar.
	 * Proxy lazy diselesaikan lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan hasilnya ditulis kembali
	 * ke field (object yang sama, nilai kolom tidak berubah).</p>
	 *
	 * @return jam pelajaran slot ke-22, atau {@code null} bila slot belum dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran22_id")
	public JamPelajaran getJamPelajaran22() {
		jamPelajaran22 = check(jamPelajaran22);
		return jamPelajaran22;
	}

	/**
	 * Menyetel jam pelajaran pada <b>slot ke-22</b>.
	 *
	 * <p>Tanpa validasi: tidak ada pemeriksaan bentrok dengan slot lain pada baris yang sama,
	 * tidak ada pemeriksaan bahwa jam pelajarannya milik sekolah yang sama, dan tidak ada
	 * keharusan {@link #setHari22(String) hari22} ikut terisi. Slot yang jamnya terisi tetapi
	 * harinya kosong akan diabaikan {@link #populateJamPelajaran()} dan {@link #infoSimple()}
	 * hanya menampilkan bagian jamnya bila harinya ada.</p>
	 *
	 * @param jamPelajaran22 jam pelajaran untuk slot ke-22; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran22(JamPelajaran jamPelajaran22) {
		this.jamPelajaran22 = jamPelajaran22;
	}

	/**
	 * Mengembalikan jam pelajaran pada <b>slot ke-23</b> (kolom {@code jam_pelajaran23_id}).
	 *
	 * <p>Sejajar dengan {@link #getHari23() hari23} dan {@link #getSubMatapelajaran23() subMatapelajaran23}: ketiganya membentuk satu pertemuan mengajar.
	 * Proxy lazy diselesaikan lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan hasilnya ditulis kembali
	 * ke field (object yang sama, nilai kolom tidak berubah).</p>
	 *
	 * @return jam pelajaran slot ke-23, atau {@code null} bila slot belum dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran23_id")
	public JamPelajaran getJamPelajaran23() {
		jamPelajaran23 = check(jamPelajaran23);
		return jamPelajaran23;
	}

	/**
	 * Menyetel jam pelajaran pada <b>slot ke-23</b>.
	 *
	 * <p>Tanpa validasi: tidak ada pemeriksaan bentrok dengan slot lain pada baris yang sama,
	 * tidak ada pemeriksaan bahwa jam pelajarannya milik sekolah yang sama, dan tidak ada
	 * keharusan {@link #setHari23(String) hari23} ikut terisi. Slot yang jamnya terisi tetapi
	 * harinya kosong akan diabaikan {@link #populateJamPelajaran()} dan {@link #infoSimple()}
	 * hanya menampilkan bagian jamnya bila harinya ada.</p>
	 *
	 * @param jamPelajaran23 jam pelajaran untuk slot ke-23; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran23(JamPelajaran jamPelajaran23) {
		this.jamPelajaran23 = jamPelajaran23;
	}

	/**
	 * Mengembalikan jam pelajaran pada <b>slot ke-24</b> (kolom {@code jam_pelajaran24_id}).
	 *
	 * <p>Sejajar dengan {@link #getHari24() hari24} dan {@link #getSubMatapelajaran24() subMatapelajaran24}: ketiganya membentuk satu pertemuan mengajar.
	 * Proxy lazy diselesaikan lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan hasilnya ditulis kembali
	 * ke field (object yang sama, nilai kolom tidak berubah).</p>
	 *
	 * @return jam pelajaran slot ke-24, atau {@code null} bila slot belum dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran24_id")
	public JamPelajaran getJamPelajaran24() {
		jamPelajaran24 = check(jamPelajaran24);
		return jamPelajaran24;
	}

	/**
	 * Menyetel jam pelajaran pada <b>slot ke-24</b>.
	 *
	 * <p>Tanpa validasi: tidak ada pemeriksaan bentrok dengan slot lain pada baris yang sama,
	 * tidak ada pemeriksaan bahwa jam pelajarannya milik sekolah yang sama, dan tidak ada
	 * keharusan {@link #setHari24(String) hari24} ikut terisi. Slot yang jamnya terisi tetapi
	 * harinya kosong akan diabaikan {@link #populateJamPelajaran()} dan {@link #infoSimple()}
	 * hanya menampilkan bagian jamnya bila harinya ada.</p>
	 *
	 * @param jamPelajaran24 jam pelajaran untuk slot ke-24; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran24(JamPelajaran jamPelajaran24) {
		this.jamPelajaran24 = jamPelajaran24;
	}

	/**
	 * Mengembalikan jam pelajaran pada <b>slot ke-25</b> (kolom {@code jam_pelajaran25_id}).
	 *
	 * <p>Sejajar dengan {@link #getHari25() hari25} dan {@link #getSubMatapelajaran25() subMatapelajaran25}: ketiganya membentuk satu pertemuan mengajar.
	 * Proxy lazy diselesaikan lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan hasilnya ditulis kembali
	 * ke field (object yang sama, nilai kolom tidak berubah).</p>
	 *
	 * @return jam pelajaran slot ke-25, atau {@code null} bila slot belum dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran25_id")
	public JamPelajaran getJamPelajaran25() {
		jamPelajaran25 = check(jamPelajaran25);
		return jamPelajaran25;
	}

	/**
	 * Menyetel jam pelajaran pada <b>slot ke-25</b>.
	 *
	 * <p>Tanpa validasi: tidak ada pemeriksaan bentrok dengan slot lain pada baris yang sama,
	 * tidak ada pemeriksaan bahwa jam pelajarannya milik sekolah yang sama, dan tidak ada
	 * keharusan {@link #setHari25(String) hari25} ikut terisi. Slot yang jamnya terisi tetapi
	 * harinya kosong akan diabaikan {@link #populateJamPelajaran()} dan {@link #infoSimple()}
	 * hanya menampilkan bagian jamnya bila harinya ada.</p>
	 *
	 * @param jamPelajaran25 jam pelajaran untuk slot ke-25; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran25(JamPelajaran jamPelajaran25) {
		this.jamPelajaran25 = jamPelajaran25;
	}

	/**
	 * Mengembalikan nama hari pertemuan pada <b>slot ke-13</b> (kolom {@code hari13}).
	 *
	 * <p><b>Menormalkan hasil:</b> nilai {@code null} maupun yang hanya berisi spasi
	 * dikembalikan sebagai {@code null}, selebihnya dikembalikan dalam bentuk sudah
	 * di-{@code trim()}. Normalisasi ini hanya di sisi baca — {@link #setHari13(String)}
	 * menyimpan apa adanya, sehingga isi kolom di basis data bisa saja mengandung spasi
	 * berlebih yang tidak pernah terlihat lewat getter.</p>
	 *
	 * <p><b>Tidak terjangkau filter <i>Hari</i></b> pada layar master: rantai
	 * {@code Restrictions.or(...)} di {@code GuruMengajarAction.initCriteria(boolean)}
	 * berhenti pada {@code hari10}. Baris yang hanya mengisi slot 11–25 hilang dari daftar
	 * begitu filter hari dipakai.</p>
	 *
	 * <p>Slot ini juga di luar jangkauan ekspor/impor Excel layar master, yang larik
	 * kolomnya berhenti pada slot ke-12.</p>
	 *
	 * @return nama hari slot ke-13 hasil {@code trim()}, atau {@code null} bila kosong
	 */
	public String getHari13() {
		return this.hari13 == null || hari13.trim().isEmpty() ? null : hari13.trim();
	}

	/**
	 * Menyetel nama hari pertemuan pada <b>slot ke-13</b>.
	 *
	 * <p>Menyimpan nilai <b>apa adanya</b>, tanpa {@code trim()} dan tanpa validasi bahwa isinya
	 * termasuk daftar {@code Common.haris}. Asimetri dengan {@link #getHari13()} yang
	 * menormalkan hasil bacanya disengaja agar nilai lama tidak berubah saat baris disimpan
	 * ulang. Perlu diingat filter <i>Hari</i> di layar membandingkan dengan {@code Restrictions.eq}
	 * atas nilai <b>mentah</b> di kolom, sehingga nilai berspasi tidak akan pernah cocok.</p>
	 *
	 * @param hari13 nama hari untuk slot ke-13; {@code null} atau string kosong mengosongkan slot
	 */
	public void setHari13(String hari13) {
		this.hari13 = hari13;
	}

	/**
	 * Mengembalikan nama hari pertemuan pada <b>slot ke-14</b> (kolom {@code hari14}).
	 *
	 * <p><b>Menormalkan hasil:</b> nilai {@code null} maupun yang hanya berisi spasi
	 * dikembalikan sebagai {@code null}, selebihnya dikembalikan dalam bentuk sudah
	 * di-{@code trim()}. Normalisasi ini hanya di sisi baca — {@link #setHari14(String)}
	 * menyimpan apa adanya, sehingga isi kolom di basis data bisa saja mengandung spasi
	 * berlebih yang tidak pernah terlihat lewat getter.</p>
	 *
	 * <p><b>Tidak terjangkau filter <i>Hari</i></b> pada layar master: rantai
	 * {@code Restrictions.or(...)} di {@code GuruMengajarAction.initCriteria(boolean)}
	 * berhenti pada {@code hari10}. Baris yang hanya mengisi slot 11–25 hilang dari daftar
	 * begitu filter hari dipakai.</p>
	 *
	 * <p>Slot ini juga di luar jangkauan ekspor/impor Excel layar master, yang larik
	 * kolomnya berhenti pada slot ke-12.</p>
	 *
	 * @return nama hari slot ke-14 hasil {@code trim()}, atau {@code null} bila kosong
	 */
	public String getHari14() {
		return this.hari14 == null || hari14.trim().isEmpty() ? null : hari14.trim();
	}

	/**
	 * Menyetel nama hari pertemuan pada <b>slot ke-14</b>.
	 *
	 * <p>Menyimpan nilai <b>apa adanya</b>, tanpa {@code trim()} dan tanpa validasi bahwa isinya
	 * termasuk daftar {@code Common.haris}. Asimetri dengan {@link #getHari14()} yang
	 * menormalkan hasil bacanya disengaja agar nilai lama tidak berubah saat baris disimpan
	 * ulang. Perlu diingat filter <i>Hari</i> di layar membandingkan dengan {@code Restrictions.eq}
	 * atas nilai <b>mentah</b> di kolom, sehingga nilai berspasi tidak akan pernah cocok.</p>
	 *
	 * @param hari14 nama hari untuk slot ke-14; {@code null} atau string kosong mengosongkan slot
	 */
	public void setHari14(String hari14) {
		this.hari14 = hari14;
	}

	/**
	 * Mengembalikan nama hari pertemuan pada <b>slot ke-15</b> (kolom {@code hari15}).
	 *
	 * <p><b>Menormalkan hasil:</b> nilai {@code null} maupun yang hanya berisi spasi
	 * dikembalikan sebagai {@code null}, selebihnya dikembalikan dalam bentuk sudah
	 * di-{@code trim()}. Normalisasi ini hanya di sisi baca — {@link #setHari15(String)}
	 * menyimpan apa adanya, sehingga isi kolom di basis data bisa saja mengandung spasi
	 * berlebih yang tidak pernah terlihat lewat getter.</p>
	 *
	 * <p><b>Tidak terjangkau filter <i>Hari</i></b> pada layar master: rantai
	 * {@code Restrictions.or(...)} di {@code GuruMengajarAction.initCriteria(boolean)}
	 * berhenti pada {@code hari10}. Baris yang hanya mengisi slot 11–25 hilang dari daftar
	 * begitu filter hari dipakai.</p>
	 *
	 * <p>Slot ini juga di luar jangkauan ekspor/impor Excel layar master, yang larik
	 * kolomnya berhenti pada slot ke-12.</p>
	 *
	 * @return nama hari slot ke-15 hasil {@code trim()}, atau {@code null} bila kosong
	 */
	public String getHari15() {
		return this.hari15 == null || hari15.trim().isEmpty() ? null : hari15.trim();
	}

	/**
	 * Menyetel nama hari pertemuan pada <b>slot ke-15</b>.
	 *
	 * <p>Menyimpan nilai <b>apa adanya</b>, tanpa {@code trim()} dan tanpa validasi bahwa isinya
	 * termasuk daftar {@code Common.haris}. Asimetri dengan {@link #getHari15()} yang
	 * menormalkan hasil bacanya disengaja agar nilai lama tidak berubah saat baris disimpan
	 * ulang. Perlu diingat filter <i>Hari</i> di layar membandingkan dengan {@code Restrictions.eq}
	 * atas nilai <b>mentah</b> di kolom, sehingga nilai berspasi tidak akan pernah cocok.</p>
	 *
	 * @param hari15 nama hari untuk slot ke-15; {@code null} atau string kosong mengosongkan slot
	 */
	public void setHari15(String hari15) {
		this.hari15 = hari15;
	}

	/**
	 * Mengembalikan nama hari pertemuan pada <b>slot ke-16</b> (kolom {@code hari16}).
	 *
	 * <p><b>Menormalkan hasil:</b> nilai {@code null} maupun yang hanya berisi spasi
	 * dikembalikan sebagai {@code null}, selebihnya dikembalikan dalam bentuk sudah
	 * di-{@code trim()}. Normalisasi ini hanya di sisi baca — {@link #setHari16(String)}
	 * menyimpan apa adanya, sehingga isi kolom di basis data bisa saja mengandung spasi
	 * berlebih yang tidak pernah terlihat lewat getter.</p>
	 *
	 * <p><b>Tidak terjangkau filter <i>Hari</i></b> pada layar master: rantai
	 * {@code Restrictions.or(...)} di {@code GuruMengajarAction.initCriteria(boolean)}
	 * berhenti pada {@code hari10}. Baris yang hanya mengisi slot 11–25 hilang dari daftar
	 * begitu filter hari dipakai.</p>
	 *
	 * <p>Slot ini juga di luar jangkauan ekspor/impor Excel layar master, yang larik
	 * kolomnya berhenti pada slot ke-12.</p>
	 *
	 * @return nama hari slot ke-16 hasil {@code trim()}, atau {@code null} bila kosong
	 */
	public String getHari16() {
		return this.hari16 == null || hari16.trim().isEmpty() ? null : hari16.trim();
	}

	/**
	 * Menyetel nama hari pertemuan pada <b>slot ke-16</b>.
	 *
	 * <p>Menyimpan nilai <b>apa adanya</b>, tanpa {@code trim()} dan tanpa validasi bahwa isinya
	 * termasuk daftar {@code Common.haris}. Asimetri dengan {@link #getHari16()} yang
	 * menormalkan hasil bacanya disengaja agar nilai lama tidak berubah saat baris disimpan
	 * ulang. Perlu diingat filter <i>Hari</i> di layar membandingkan dengan {@code Restrictions.eq}
	 * atas nilai <b>mentah</b> di kolom, sehingga nilai berspasi tidak akan pernah cocok.</p>
	 *
	 * @param hari16 nama hari untuk slot ke-16; {@code null} atau string kosong mengosongkan slot
	 */
	public void setHari16(String hari16) {
		this.hari16 = hari16;
	}

	/**
	 * Mengembalikan nama hari pertemuan pada <b>slot ke-17</b> (kolom {@code hari17}).
	 *
	 * <p><b>Menormalkan hasil:</b> nilai {@code null} maupun yang hanya berisi spasi
	 * dikembalikan sebagai {@code null}, selebihnya dikembalikan dalam bentuk sudah
	 * di-{@code trim()}. Normalisasi ini hanya di sisi baca — {@link #setHari17(String)}
	 * menyimpan apa adanya, sehingga isi kolom di basis data bisa saja mengandung spasi
	 * berlebih yang tidak pernah terlihat lewat getter.</p>
	 *
	 * <p><b>Tidak terjangkau filter <i>Hari</i></b> pada layar master: rantai
	 * {@code Restrictions.or(...)} di {@code GuruMengajarAction.initCriteria(boolean)}
	 * berhenti pada {@code hari10}. Baris yang hanya mengisi slot 11–25 hilang dari daftar
	 * begitu filter hari dipakai.</p>
	 *
	 * <p>Slot ini juga di luar jangkauan ekspor/impor Excel layar master, yang larik
	 * kolomnya berhenti pada slot ke-12.</p>
	 *
	 * @return nama hari slot ke-17 hasil {@code trim()}, atau {@code null} bila kosong
	 */
	public String getHari17() {
		return this.hari17 == null || hari17.trim().isEmpty() ? null : hari17.trim();
	}

	/**
	 * Menyetel nama hari pertemuan pada <b>slot ke-17</b>.
	 *
	 * <p>Menyimpan nilai <b>apa adanya</b>, tanpa {@code trim()} dan tanpa validasi bahwa isinya
	 * termasuk daftar {@code Common.haris}. Asimetri dengan {@link #getHari17()} yang
	 * menormalkan hasil bacanya disengaja agar nilai lama tidak berubah saat baris disimpan
	 * ulang. Perlu diingat filter <i>Hari</i> di layar membandingkan dengan {@code Restrictions.eq}
	 * atas nilai <b>mentah</b> di kolom, sehingga nilai berspasi tidak akan pernah cocok.</p>
	 *
	 * @param hari17 nama hari untuk slot ke-17; {@code null} atau string kosong mengosongkan slot
	 */
	public void setHari17(String hari17) {
		this.hari17 = hari17;
	}

	/**
	 * Mengembalikan nama hari pertemuan pada <b>slot ke-18</b> (kolom {@code hari18}).
	 *
	 * <p><b>Menormalkan hasil:</b> nilai {@code null} maupun yang hanya berisi spasi
	 * dikembalikan sebagai {@code null}, selebihnya dikembalikan dalam bentuk sudah
	 * di-{@code trim()}. Normalisasi ini hanya di sisi baca — {@link #setHari18(String)}
	 * menyimpan apa adanya, sehingga isi kolom di basis data bisa saja mengandung spasi
	 * berlebih yang tidak pernah terlihat lewat getter.</p>
	 *
	 * <p><b>Tidak terjangkau filter <i>Hari</i></b> pada layar master: rantai
	 * {@code Restrictions.or(...)} di {@code GuruMengajarAction.initCriteria(boolean)}
	 * berhenti pada {@code hari10}. Baris yang hanya mengisi slot 11–25 hilang dari daftar
	 * begitu filter hari dipakai.</p>
	 *
	 * <p>Slot ini juga di luar jangkauan ekspor/impor Excel layar master, yang larik
	 * kolomnya berhenti pada slot ke-12.</p>
	 *
	 * @return nama hari slot ke-18 hasil {@code trim()}, atau {@code null} bila kosong
	 */
	public String getHari18() {
		return this.hari18 == null || hari18.trim().isEmpty() ? null : hari18.trim();
	}

	/**
	 * Menyetel nama hari pertemuan pada <b>slot ke-18</b>.
	 *
	 * <p>Menyimpan nilai <b>apa adanya</b>, tanpa {@code trim()} dan tanpa validasi bahwa isinya
	 * termasuk daftar {@code Common.haris}. Asimetri dengan {@link #getHari18()} yang
	 * menormalkan hasil bacanya disengaja agar nilai lama tidak berubah saat baris disimpan
	 * ulang. Perlu diingat filter <i>Hari</i> di layar membandingkan dengan {@code Restrictions.eq}
	 * atas nilai <b>mentah</b> di kolom, sehingga nilai berspasi tidak akan pernah cocok.</p>
	 *
	 * @param hari18 nama hari untuk slot ke-18; {@code null} atau string kosong mengosongkan slot
	 */
	public void setHari18(String hari18) {
		this.hari18 = hari18;
	}

	/**
	 * Mengembalikan nama hari pertemuan pada <b>slot ke-19</b> (kolom {@code hari19}).
	 *
	 * <p><b>Menormalkan hasil:</b> nilai {@code null} maupun yang hanya berisi spasi
	 * dikembalikan sebagai {@code null}, selebihnya dikembalikan dalam bentuk sudah
	 * di-{@code trim()}. Normalisasi ini hanya di sisi baca — {@link #setHari19(String)}
	 * menyimpan apa adanya, sehingga isi kolom di basis data bisa saja mengandung spasi
	 * berlebih yang tidak pernah terlihat lewat getter.</p>
	 *
	 * <p><b>Tidak terjangkau filter <i>Hari</i></b> pada layar master: rantai
	 * {@code Restrictions.or(...)} di {@code GuruMengajarAction.initCriteria(boolean)}
	 * berhenti pada {@code hari10}. Baris yang hanya mengisi slot 11–25 hilang dari daftar
	 * begitu filter hari dipakai.</p>
	 *
	 * <p>Slot ini juga di luar jangkauan ekspor/impor Excel layar master, yang larik
	 * kolomnya berhenti pada slot ke-12.</p>
	 *
	 * @return nama hari slot ke-19 hasil {@code trim()}, atau {@code null} bila kosong
	 */
	public String getHari19() {
		return this.hari19 == null || hari19.trim().isEmpty() ? null : hari19.trim();
	}

	/**
	 * Menyetel nama hari pertemuan pada <b>slot ke-19</b>.
	 *
	 * <p>Menyimpan nilai <b>apa adanya</b>, tanpa {@code trim()} dan tanpa validasi bahwa isinya
	 * termasuk daftar {@code Common.haris}. Asimetri dengan {@link #getHari19()} yang
	 * menormalkan hasil bacanya disengaja agar nilai lama tidak berubah saat baris disimpan
	 * ulang. Perlu diingat filter <i>Hari</i> di layar membandingkan dengan {@code Restrictions.eq}
	 * atas nilai <b>mentah</b> di kolom, sehingga nilai berspasi tidak akan pernah cocok.</p>
	 *
	 * @param hari19 nama hari untuk slot ke-19; {@code null} atau string kosong mengosongkan slot
	 */
	public void setHari19(String hari19) {
		this.hari19 = hari19;
	}

	/**
	 * Mengembalikan nama hari pertemuan pada <b>slot ke-20</b> (kolom {@code hari20}).
	 *
	 * <p><b>Menormalkan hasil:</b> nilai {@code null} maupun yang hanya berisi spasi
	 * dikembalikan sebagai {@code null}, selebihnya dikembalikan dalam bentuk sudah
	 * di-{@code trim()}. Normalisasi ini hanya di sisi baca — {@link #setHari20(String)}
	 * menyimpan apa adanya, sehingga isi kolom di basis data bisa saja mengandung spasi
	 * berlebih yang tidak pernah terlihat lewat getter.</p>
	 *
	 * <p><b>Tidak terjangkau filter <i>Hari</i></b> pada layar master: rantai
	 * {@code Restrictions.or(...)} di {@code GuruMengajarAction.initCriteria(boolean)}
	 * berhenti pada {@code hari10}. Baris yang hanya mengisi slot 11–25 hilang dari daftar
	 * begitu filter hari dipakai.</p>
	 *
	 * <p>Slot ini juga di luar jangkauan ekspor/impor Excel layar master, yang larik
	 * kolomnya berhenti pada slot ke-12.</p>
	 *
	 * @return nama hari slot ke-20 hasil {@code trim()}, atau {@code null} bila kosong
	 */
	public String getHari20() {
		return this.hari20 == null || hari20.trim().isEmpty() ? null : hari20.trim();
	}

	/**
	 * Menyetel nama hari pertemuan pada <b>slot ke-20</b>.
	 *
	 * <p>Menyimpan nilai <b>apa adanya</b>, tanpa {@code trim()} dan tanpa validasi bahwa isinya
	 * termasuk daftar {@code Common.haris}. Asimetri dengan {@link #getHari20()} yang
	 * menormalkan hasil bacanya disengaja agar nilai lama tidak berubah saat baris disimpan
	 * ulang. Perlu diingat filter <i>Hari</i> di layar membandingkan dengan {@code Restrictions.eq}
	 * atas nilai <b>mentah</b> di kolom, sehingga nilai berspasi tidak akan pernah cocok.</p>
	 *
	 * @param hari20 nama hari untuk slot ke-20; {@code null} atau string kosong mengosongkan slot
	 */
	public void setHari20(String hari20) {
		this.hari20 = hari20;
	}

	/**
	 * Mengembalikan nama hari pertemuan pada <b>slot ke-21</b> (kolom {@code hari21}).
	 *
	 * <p><b>Menormalkan hasil:</b> nilai {@code null} maupun yang hanya berisi spasi
	 * dikembalikan sebagai {@code null}, selebihnya dikembalikan dalam bentuk sudah
	 * di-{@code trim()}. Normalisasi ini hanya di sisi baca — {@link #setHari21(String)}
	 * menyimpan apa adanya, sehingga isi kolom di basis data bisa saja mengandung spasi
	 * berlebih yang tidak pernah terlihat lewat getter.</p>
	 *
	 * <p><b>Tidak terjangkau filter <i>Hari</i></b> pada layar master: rantai
	 * {@code Restrictions.or(...)} di {@code GuruMengajarAction.initCriteria(boolean)}
	 * berhenti pada {@code hari10}. Baris yang hanya mengisi slot 11–25 hilang dari daftar
	 * begitu filter hari dipakai.</p>
	 *
	 * <p>Slot ini juga di luar jangkauan ekspor/impor Excel layar master, yang larik
	 * kolomnya berhenti pada slot ke-12.</p>
	 *
	 * @return nama hari slot ke-21 hasil {@code trim()}, atau {@code null} bila kosong
	 */
	public String getHari21() {
		return this.hari21 == null || hari21.trim().isEmpty() ? null : hari21.trim();
	}

	/**
	 * Menyetel nama hari pertemuan pada <b>slot ke-21</b>.
	 *
	 * <p>Menyimpan nilai <b>apa adanya</b>, tanpa {@code trim()} dan tanpa validasi bahwa isinya
	 * termasuk daftar {@code Common.haris}. Asimetri dengan {@link #getHari21()} yang
	 * menormalkan hasil bacanya disengaja agar nilai lama tidak berubah saat baris disimpan
	 * ulang. Perlu diingat filter <i>Hari</i> di layar membandingkan dengan {@code Restrictions.eq}
	 * atas nilai <b>mentah</b> di kolom, sehingga nilai berspasi tidak akan pernah cocok.</p>
	 *
	 * @param hari21 nama hari untuk slot ke-21; {@code null} atau string kosong mengosongkan slot
	 */
	public void setHari21(String hari21) {
		this.hari21 = hari21;
	}

	/**
	 * Mengembalikan nama hari pertemuan pada <b>slot ke-22</b> (kolom {@code hari22}).
	 *
	 * <p><b>Menormalkan hasil:</b> nilai {@code null} maupun yang hanya berisi spasi
	 * dikembalikan sebagai {@code null}, selebihnya dikembalikan dalam bentuk sudah
	 * di-{@code trim()}. Normalisasi ini hanya di sisi baca — {@link #setHari22(String)}
	 * menyimpan apa adanya, sehingga isi kolom di basis data bisa saja mengandung spasi
	 * berlebih yang tidak pernah terlihat lewat getter.</p>
	 *
	 * <p><b>Tidak terjangkau filter <i>Hari</i></b> pada layar master: rantai
	 * {@code Restrictions.or(...)} di {@code GuruMengajarAction.initCriteria(boolean)}
	 * berhenti pada {@code hari10}. Baris yang hanya mengisi slot 11–25 hilang dari daftar
	 * begitu filter hari dipakai.</p>
	 *
	 * <p>Slot ini juga di luar jangkauan ekspor/impor Excel layar master, yang larik
	 * kolomnya berhenti pada slot ke-12.</p>
	 *
	 * @return nama hari slot ke-22 hasil {@code trim()}, atau {@code null} bila kosong
	 */
	public String getHari22() {
		return this.hari22 == null || hari22.trim().isEmpty() ? null : hari22.trim();
	}

	/**
	 * Menyetel nama hari pertemuan pada <b>slot ke-22</b>.
	 *
	 * <p>Menyimpan nilai <b>apa adanya</b>, tanpa {@code trim()} dan tanpa validasi bahwa isinya
	 * termasuk daftar {@code Common.haris}. Asimetri dengan {@link #getHari22()} yang
	 * menormalkan hasil bacanya disengaja agar nilai lama tidak berubah saat baris disimpan
	 * ulang. Perlu diingat filter <i>Hari</i> di layar membandingkan dengan {@code Restrictions.eq}
	 * atas nilai <b>mentah</b> di kolom, sehingga nilai berspasi tidak akan pernah cocok.</p>
	 *
	 * @param hari22 nama hari untuk slot ke-22; {@code null} atau string kosong mengosongkan slot
	 */
	public void setHari22(String hari22) {
		this.hari22 = hari22;
	}

	/**
	 * Mengembalikan nama hari pertemuan pada <b>slot ke-23</b> (kolom {@code hari23}).
	 *
	 * <p><b>Menormalkan hasil:</b> nilai {@code null} maupun yang hanya berisi spasi
	 * dikembalikan sebagai {@code null}, selebihnya dikembalikan dalam bentuk sudah
	 * di-{@code trim()}. Normalisasi ini hanya di sisi baca — {@link #setHari23(String)}
	 * menyimpan apa adanya, sehingga isi kolom di basis data bisa saja mengandung spasi
	 * berlebih yang tidak pernah terlihat lewat getter.</p>
	 *
	 * <p><b>Tidak terjangkau filter <i>Hari</i></b> pada layar master: rantai
	 * {@code Restrictions.or(...)} di {@code GuruMengajarAction.initCriteria(boolean)}
	 * berhenti pada {@code hari10}. Baris yang hanya mengisi slot 11–25 hilang dari daftar
	 * begitu filter hari dipakai.</p>
	 *
	 * <p>Slot ini juga di luar jangkauan ekspor/impor Excel layar master, yang larik
	 * kolomnya berhenti pada slot ke-12.</p>
	 *
	 * @return nama hari slot ke-23 hasil {@code trim()}, atau {@code null} bila kosong
	 */
	public String getHari23() {
		return this.hari23 == null || hari23.trim().isEmpty() ? null : hari23.trim();
	}

	/**
	 * Menyetel nama hari pertemuan pada <b>slot ke-23</b>.
	 *
	 * <p>Menyimpan nilai <b>apa adanya</b>, tanpa {@code trim()} dan tanpa validasi bahwa isinya
	 * termasuk daftar {@code Common.haris}. Asimetri dengan {@link #getHari23()} yang
	 * menormalkan hasil bacanya disengaja agar nilai lama tidak berubah saat baris disimpan
	 * ulang. Perlu diingat filter <i>Hari</i> di layar membandingkan dengan {@code Restrictions.eq}
	 * atas nilai <b>mentah</b> di kolom, sehingga nilai berspasi tidak akan pernah cocok.</p>
	 *
	 * @param hari23 nama hari untuk slot ke-23; {@code null} atau string kosong mengosongkan slot
	 */
	public void setHari23(String hari23) {
		this.hari23 = hari23;
	}

	/**
	 * Mengembalikan nama hari pertemuan pada <b>slot ke-24</b> (kolom {@code hari24}).
	 *
	 * <p><b>Menormalkan hasil:</b> nilai {@code null} maupun yang hanya berisi spasi
	 * dikembalikan sebagai {@code null}, selebihnya dikembalikan dalam bentuk sudah
	 * di-{@code trim()}. Normalisasi ini hanya di sisi baca — {@link #setHari24(String)}
	 * menyimpan apa adanya, sehingga isi kolom di basis data bisa saja mengandung spasi
	 * berlebih yang tidak pernah terlihat lewat getter.</p>
	 *
	 * <p><b>Tidak terjangkau filter <i>Hari</i></b> pada layar master: rantai
	 * {@code Restrictions.or(...)} di {@code GuruMengajarAction.initCriteria(boolean)}
	 * berhenti pada {@code hari10}. Baris yang hanya mengisi slot 11–25 hilang dari daftar
	 * begitu filter hari dipakai.</p>
	 *
	 * <p>Slot ini juga di luar jangkauan ekspor/impor Excel layar master, yang larik
	 * kolomnya berhenti pada slot ke-12.</p>
	 *
	 * @return nama hari slot ke-24 hasil {@code trim()}, atau {@code null} bila kosong
	 */
	public String getHari24() {
		return this.hari24 == null || hari24.trim().isEmpty() ? null : hari24.trim();
	}

	/**
	 * Menyetel nama hari pertemuan pada <b>slot ke-24</b>.
	 *
	 * <p>Menyimpan nilai <b>apa adanya</b>, tanpa {@code trim()} dan tanpa validasi bahwa isinya
	 * termasuk daftar {@code Common.haris}. Asimetri dengan {@link #getHari24()} yang
	 * menormalkan hasil bacanya disengaja agar nilai lama tidak berubah saat baris disimpan
	 * ulang. Perlu diingat filter <i>Hari</i> di layar membandingkan dengan {@code Restrictions.eq}
	 * atas nilai <b>mentah</b> di kolom, sehingga nilai berspasi tidak akan pernah cocok.</p>
	 *
	 * @param hari24 nama hari untuk slot ke-24; {@code null} atau string kosong mengosongkan slot
	 */
	public void setHari24(String hari24) {
		this.hari24 = hari24;
	}

	/**
	 * Mengembalikan nama hari pertemuan pada <b>slot ke-25</b> (kolom {@code hari25}).
	 *
	 * <p><b>Menormalkan hasil:</b> nilai {@code null} maupun yang hanya berisi spasi
	 * dikembalikan sebagai {@code null}, selebihnya dikembalikan dalam bentuk sudah
	 * di-{@code trim()}. Normalisasi ini hanya di sisi baca — {@link #setHari25(String)}
	 * menyimpan apa adanya, sehingga isi kolom di basis data bisa saja mengandung spasi
	 * berlebih yang tidak pernah terlihat lewat getter.</p>
	 *
	 * <p><b>Tidak terjangkau filter <i>Hari</i></b> pada layar master: rantai
	 * {@code Restrictions.or(...)} di {@code GuruMengajarAction.initCriteria(boolean)}
	 * berhenti pada {@code hari10}. Baris yang hanya mengisi slot 11–25 hilang dari daftar
	 * begitu filter hari dipakai.</p>
	 *
	 * <p>Slot ini juga di luar jangkauan ekspor/impor Excel layar master, yang larik
	 * kolomnya berhenti pada slot ke-12.</p>
	 *
	 * @return nama hari slot ke-25 hasil {@code trim()}, atau {@code null} bila kosong
	 */
	public String getHari25() {
		return this.hari25 == null || hari25.trim().isEmpty() ? null : hari25.trim();
	}

	/**
	 * Menyetel nama hari pertemuan pada <b>slot ke-25</b>.
	 *
	 * <p>Menyimpan nilai <b>apa adanya</b>, tanpa {@code trim()} dan tanpa validasi bahwa isinya
	 * termasuk daftar {@code Common.haris}. Asimetri dengan {@link #getHari25()} yang
	 * menormalkan hasil bacanya disengaja agar nilai lama tidak berubah saat baris disimpan
	 * ulang. Perlu diingat filter <i>Hari</i> di layar membandingkan dengan {@code Restrictions.eq}
	 * atas nilai <b>mentah</b> di kolom, sehingga nilai berspasi tidak akan pernah cocok.</p>
	 *
	 * @param hari25 nama hari untuk slot ke-25; {@code null} atau string kosong mengosongkan slot
	 */
	public void setHari25(String hari25) {
		this.hari25 = hari25;
	}

	/**
	 * Mengembalikan sub mata pelajaran (komponen) yang diajarkan pada <b>slot ke-13</b>
	 * (kolom {@code sub_matapelajaran_13}).
	 *
	 * <p>Opsional dan sejajar dengan {@link #getHari13() hari13} serta
	 * {@link #getJamPelajaran13() jamPelajaran13} pada slot yang sama. Proxy lazy diselesaikan
	 * lewat {@link ais.database.model.GeneralValueObject#check(Object)} dan ditulis kembali ke
	 * field (object yang sama).</p>
	 *
	 * @return sub mata pelajaran slot ke-13, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_13")
	public SubMatapelajaran getSubMatapelajaran13() {
		subMatapelajaran13 = check(subMatapelajaran13);
		return subMatapelajaran13;
	}

	/**
	 * Menyetel sub mata pelajaran (komponen) pada <b>slot ke-13</b>.
	 *
	 * <p>Tanpa validasi bahwa sub mata pelajaran yang disetel benar-benar milik
	 * {@link #getMatapelajaran() matapelajaran} baris ini — pembatasan itu hanya dilakukan
	 * combo di formulir. <b>Tidak pernah dipanggil</b> oleh
	 * {@code GuruMengajarAction.onSave(Event)}, sehingga kolomnya kembali {@code NULL} setiap
	 * kali baris disimpan lewat layar (lihat Javadoc kelas).</p>
	 *
	 * @param subMatapelajaran13 komponen mata pelajaran untuk slot ke-13; {@code null}
	 *         mengosongkan slot
	 */
	public void setSubMatapelajaran13(SubMatapelajaran subMatapelajaran13) {
		this.subMatapelajaran13 = subMatapelajaran13;
	}

	/**
	 * Mengembalikan sub mata pelajaran (komponen) yang diajarkan pada <b>slot ke-14</b>
	 * (kolom {@code sub_matapelajaran_14}).
	 *
	 * <p>Opsional dan sejajar dengan {@link #getHari14() hari14} serta
	 * {@link #getJamPelajaran14() jamPelajaran14} pada slot yang sama. Proxy lazy diselesaikan
	 * lewat {@link ais.database.model.GeneralValueObject#check(Object)} dan ditulis kembali ke
	 * field (object yang sama).</p>
	 *
	 * @return sub mata pelajaran slot ke-14, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_14")
	public SubMatapelajaran getSubMatapelajaran14() {
		subMatapelajaran14 = check(subMatapelajaran14);
		return subMatapelajaran14;
	}

	/**
	 * Menyetel sub mata pelajaran (komponen) pada <b>slot ke-14</b>.
	 *
	 * <p>Tanpa validasi bahwa sub mata pelajaran yang disetel benar-benar milik
	 * {@link #getMatapelajaran() matapelajaran} baris ini — pembatasan itu hanya dilakukan
	 * combo di formulir. <b>Tidak pernah dipanggil</b> oleh
	 * {@code GuruMengajarAction.onSave(Event)}, sehingga kolomnya kembali {@code NULL} setiap
	 * kali baris disimpan lewat layar (lihat Javadoc kelas).</p>
	 *
	 * @param subMatapelajaran14 komponen mata pelajaran untuk slot ke-14; {@code null}
	 *         mengosongkan slot
	 */
	public void setSubMatapelajaran14(SubMatapelajaran subMatapelajaran14) {
		this.subMatapelajaran14 = subMatapelajaran14;
	}

	/**
	 * Mengembalikan sub mata pelajaran (komponen) yang diajarkan pada <b>slot ke-15</b>
	 * (kolom {@code sub_matapelajaran_15}).
	 *
	 * <p>Opsional dan sejajar dengan {@link #getHari15() hari15} serta
	 * {@link #getJamPelajaran15() jamPelajaran15} pada slot yang sama. Proxy lazy diselesaikan
	 * lewat {@link ais.database.model.GeneralValueObject#check(Object)} dan ditulis kembali ke
	 * field (object yang sama).</p>
	 *
	 * @return sub mata pelajaran slot ke-15, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_15")
	public SubMatapelajaran getSubMatapelajaran15() {
		subMatapelajaran15 = check(subMatapelajaran15);
		return subMatapelajaran15;
	}

	/**
	 * Menyetel sub mata pelajaran (komponen) pada <b>slot ke-15</b>.
	 *
	 * <p>Tanpa validasi bahwa sub mata pelajaran yang disetel benar-benar milik
	 * {@link #getMatapelajaran() matapelajaran} baris ini — pembatasan itu hanya dilakukan
	 * combo di formulir. <b>Tidak pernah dipanggil</b> oleh
	 * {@code GuruMengajarAction.onSave(Event)}, sehingga kolomnya kembali {@code NULL} setiap
	 * kali baris disimpan lewat layar (lihat Javadoc kelas).</p>
	 *
	 * @param subMatapelajaran15 komponen mata pelajaran untuk slot ke-15; {@code null}
	 *         mengosongkan slot
	 */
	public void setSubMatapelajaran15(SubMatapelajaran subMatapelajaran15) {
		this.subMatapelajaran15 = subMatapelajaran15;
	}

	/**
	 * Mengembalikan sub mata pelajaran (komponen) yang diajarkan pada <b>slot ke-16</b>
	 * (kolom {@code sub_matapelajaran_16}).
	 *
	 * <p>Opsional dan sejajar dengan {@link #getHari16() hari16} serta
	 * {@link #getJamPelajaran16() jamPelajaran16} pada slot yang sama. Proxy lazy diselesaikan
	 * lewat {@link ais.database.model.GeneralValueObject#check(Object)} dan ditulis kembali ke
	 * field (object yang sama).</p>
	 *
	 * @return sub mata pelajaran slot ke-16, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_16")
	public SubMatapelajaran getSubMatapelajaran16() {
		subMatapelajaran16 = check(subMatapelajaran16);
		return subMatapelajaran16;
	}

	/**
	 * Menyetel sub mata pelajaran (komponen) pada <b>slot ke-16</b>.
	 *
	 * <p>Tanpa validasi bahwa sub mata pelajaran yang disetel benar-benar milik
	 * {@link #getMatapelajaran() matapelajaran} baris ini — pembatasan itu hanya dilakukan
	 * combo di formulir. <b>Tidak pernah dipanggil</b> oleh
	 * {@code GuruMengajarAction.onSave(Event)}, sehingga kolomnya kembali {@code NULL} setiap
	 * kali baris disimpan lewat layar (lihat Javadoc kelas).</p>
	 *
	 * @param subMatapelajaran16 komponen mata pelajaran untuk slot ke-16; {@code null}
	 *         mengosongkan slot
	 */
	public void setSubMatapelajaran16(SubMatapelajaran subMatapelajaran16) {
		this.subMatapelajaran16 = subMatapelajaran16;
	}

	/**
	 * Mengembalikan sub mata pelajaran (komponen) yang diajarkan pada <b>slot ke-17</b>
	 * (kolom {@code sub_matapelajaran_17}).
	 *
	 * <p>Opsional dan sejajar dengan {@link #getHari17() hari17} serta
	 * {@link #getJamPelajaran17() jamPelajaran17} pada slot yang sama. Proxy lazy diselesaikan
	 * lewat {@link ais.database.model.GeneralValueObject#check(Object)} dan ditulis kembali ke
	 * field (object yang sama).</p>
	 *
	 * @return sub mata pelajaran slot ke-17, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_17")
	public SubMatapelajaran getSubMatapelajaran17() {
		subMatapelajaran17 = check(subMatapelajaran17);
		return subMatapelajaran17;
	}

	/**
	 * Menyetel sub mata pelajaran (komponen) pada <b>slot ke-17</b>.
	 *
	 * <p>Tanpa validasi bahwa sub mata pelajaran yang disetel benar-benar milik
	 * {@link #getMatapelajaran() matapelajaran} baris ini — pembatasan itu hanya dilakukan
	 * combo di formulir. <b>Tidak pernah dipanggil</b> oleh
	 * {@code GuruMengajarAction.onSave(Event)}, sehingga kolomnya kembali {@code NULL} setiap
	 * kali baris disimpan lewat layar (lihat Javadoc kelas).</p>
	 *
	 * @param subMatapelajaran17 komponen mata pelajaran untuk slot ke-17; {@code null}
	 *         mengosongkan slot
	 */
	public void setSubMatapelajaran17(SubMatapelajaran subMatapelajaran17) {
		this.subMatapelajaran17 = subMatapelajaran17;
	}

	/**
	 * Mengembalikan sub mata pelajaran (komponen) yang diajarkan pada <b>slot ke-18</b>
	 * (kolom {@code sub_matapelajaran_18}).
	 *
	 * <p>Opsional dan sejajar dengan {@link #getHari18() hari18} serta
	 * {@link #getJamPelajaran18() jamPelajaran18} pada slot yang sama. Proxy lazy diselesaikan
	 * lewat {@link ais.database.model.GeneralValueObject#check(Object)} dan ditulis kembali ke
	 * field (object yang sama).</p>
	 *
	 * @return sub mata pelajaran slot ke-18, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_18")
	public SubMatapelajaran getSubMatapelajaran18() {
		subMatapelajaran18 = check(subMatapelajaran18);
		return subMatapelajaran18;
	}

	/**
	 * Menyetel sub mata pelajaran (komponen) pada <b>slot ke-18</b>.
	 *
	 * <p>Tanpa validasi bahwa sub mata pelajaran yang disetel benar-benar milik
	 * {@link #getMatapelajaran() matapelajaran} baris ini — pembatasan itu hanya dilakukan
	 * combo di formulir. <b>Tidak pernah dipanggil</b> oleh
	 * {@code GuruMengajarAction.onSave(Event)}, sehingga kolomnya kembali {@code NULL} setiap
	 * kali baris disimpan lewat layar (lihat Javadoc kelas).</p>
	 *
	 * @param subMatapelajaran18 komponen mata pelajaran untuk slot ke-18; {@code null}
	 *         mengosongkan slot
	 */
	public void setSubMatapelajaran18(SubMatapelajaran subMatapelajaran18) {
		this.subMatapelajaran18 = subMatapelajaran18;
	}

	/**
	 * Mengembalikan sub mata pelajaran (komponen) yang diajarkan pada <b>slot ke-19</b>
	 * (kolom {@code sub_matapelajaran_19}).
	 *
	 * <p>Opsional dan sejajar dengan {@link #getHari19() hari19} serta
	 * {@link #getJamPelajaran19() jamPelajaran19} pada slot yang sama. Proxy lazy diselesaikan
	 * lewat {@link ais.database.model.GeneralValueObject#check(Object)} dan ditulis kembali ke
	 * field (object yang sama).</p>
	 *
	 * @return sub mata pelajaran slot ke-19, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_19")
	public SubMatapelajaran getSubMatapelajaran19() {
		subMatapelajaran19 = check(subMatapelajaran19);
		return subMatapelajaran19;
	}

	/**
	 * Menyetel sub mata pelajaran (komponen) pada <b>slot ke-19</b>.
	 *
	 * <p>Tanpa validasi bahwa sub mata pelajaran yang disetel benar-benar milik
	 * {@link #getMatapelajaran() matapelajaran} baris ini — pembatasan itu hanya dilakukan
	 * combo di formulir. <b>Tidak pernah dipanggil</b> oleh
	 * {@code GuruMengajarAction.onSave(Event)}, sehingga kolomnya kembali {@code NULL} setiap
	 * kali baris disimpan lewat layar (lihat Javadoc kelas).</p>
	 *
	 * @param subMatapelajaran19 komponen mata pelajaran untuk slot ke-19; {@code null}
	 *         mengosongkan slot
	 */
	public void setSubMatapelajaran19(SubMatapelajaran subMatapelajaran19) {
		this.subMatapelajaran19 = subMatapelajaran19;
	}

	/**
	 * Mengembalikan sub mata pelajaran (komponen) yang diajarkan pada <b>slot ke-20</b>
	 * (kolom {@code sub_matapelajaran_20}).
	 *
	 * <p>Opsional dan sejajar dengan {@link #getHari20() hari20} serta
	 * {@link #getJamPelajaran20() jamPelajaran20} pada slot yang sama. Proxy lazy diselesaikan
	 * lewat {@link ais.database.model.GeneralValueObject#check(Object)} dan ditulis kembali ke
	 * field (object yang sama).</p>
	 *
	 * @return sub mata pelajaran slot ke-20, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_20")
	public SubMatapelajaran getSubMatapelajaran20() {
		subMatapelajaran20 = check(subMatapelajaran20);
		return subMatapelajaran20;
	}

	/**
	 * Menyetel sub mata pelajaran (komponen) pada <b>slot ke-20</b>.
	 *
	 * <p>Tanpa validasi bahwa sub mata pelajaran yang disetel benar-benar milik
	 * {@link #getMatapelajaran() matapelajaran} baris ini — pembatasan itu hanya dilakukan
	 * combo di formulir. <b>Tidak pernah dipanggil</b> oleh
	 * {@code GuruMengajarAction.onSave(Event)}, sehingga kolomnya kembali {@code NULL} setiap
	 * kali baris disimpan lewat layar (lihat Javadoc kelas).</p>
	 *
	 * @param subMatapelajaran20 komponen mata pelajaran untuk slot ke-20; {@code null}
	 *         mengosongkan slot
	 */
	public void setSubMatapelajaran20(SubMatapelajaran subMatapelajaran20) {
		this.subMatapelajaran20 = subMatapelajaran20;
	}

	/**
	 * Mengembalikan sub mata pelajaran (komponen) yang diajarkan pada <b>slot ke-21</b>
	 * (kolom {@code sub_matapelajaran_21}).
	 *
	 * <p>Opsional dan sejajar dengan {@link #getHari21() hari21} serta
	 * {@link #getJamPelajaran21() jamPelajaran21} pada slot yang sama. Proxy lazy diselesaikan
	 * lewat {@link ais.database.model.GeneralValueObject#check(Object)} dan ditulis kembali ke
	 * field (object yang sama).</p>
	 *
	 * @return sub mata pelajaran slot ke-21, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_21")
	public SubMatapelajaran getSubMatapelajaran21() {
		subMatapelajaran21 = check(subMatapelajaran21);
		return subMatapelajaran21;
	}

	/**
	 * Menyetel sub mata pelajaran (komponen) pada <b>slot ke-21</b>.
	 *
	 * <p>Tanpa validasi bahwa sub mata pelajaran yang disetel benar-benar milik
	 * {@link #getMatapelajaran() matapelajaran} baris ini — pembatasan itu hanya dilakukan
	 * combo di formulir. <b>Tidak pernah dipanggil</b> oleh
	 * {@code GuruMengajarAction.onSave(Event)}, sehingga kolomnya kembali {@code NULL} setiap
	 * kali baris disimpan lewat layar (lihat Javadoc kelas).</p>
	 *
	 * @param subMatapelajaran21 komponen mata pelajaran untuk slot ke-21; {@code null}
	 *         mengosongkan slot
	 */
	public void setSubMatapelajaran21(SubMatapelajaran subMatapelajaran21) {
		this.subMatapelajaran21 = subMatapelajaran21;
	}

	/**
	 * Mengembalikan sub mata pelajaran (komponen) yang diajarkan pada <b>slot ke-22</b>
	 * (kolom {@code sub_matapelajaran_22}).
	 *
	 * <p>Opsional dan sejajar dengan {@link #getHari22() hari22} serta
	 * {@link #getJamPelajaran22() jamPelajaran22} pada slot yang sama. Proxy lazy diselesaikan
	 * lewat {@link ais.database.model.GeneralValueObject#check(Object)} dan ditulis kembali ke
	 * field (object yang sama).</p>
	 *
	 * @return sub mata pelajaran slot ke-22, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_22")
	public SubMatapelajaran getSubMatapelajaran22() {
		subMatapelajaran22 = check(subMatapelajaran22);
		return subMatapelajaran22;
	}

	/**
	 * Menyetel sub mata pelajaran (komponen) pada <b>slot ke-22</b>.
	 *
	 * <p>Tanpa validasi bahwa sub mata pelajaran yang disetel benar-benar milik
	 * {@link #getMatapelajaran() matapelajaran} baris ini — pembatasan itu hanya dilakukan
	 * combo di formulir. <b>Tidak pernah dipanggil</b> oleh
	 * {@code GuruMengajarAction.onSave(Event)}, sehingga kolomnya kembali {@code NULL} setiap
	 * kali baris disimpan lewat layar (lihat Javadoc kelas).</p>
	 *
	 * @param subMatapelajaran22 komponen mata pelajaran untuk slot ke-22; {@code null}
	 *         mengosongkan slot
	 */
	public void setSubMatapelajaran22(SubMatapelajaran subMatapelajaran22) {
		this.subMatapelajaran22 = subMatapelajaran22;
	}

	/**
	 * Mengembalikan sub mata pelajaran (komponen) yang diajarkan pada <b>slot ke-23</b>
	 * (kolom {@code sub_matapelajaran_23}).
	 *
	 * <p>Opsional dan sejajar dengan {@link #getHari23() hari23} serta
	 * {@link #getJamPelajaran23() jamPelajaran23} pada slot yang sama. Proxy lazy diselesaikan
	 * lewat {@link ais.database.model.GeneralValueObject#check(Object)} dan ditulis kembali ke
	 * field (object yang sama).</p>
	 *
	 * @return sub mata pelajaran slot ke-23, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_23")
	public SubMatapelajaran getSubMatapelajaran23() {
		subMatapelajaran23 = check(subMatapelajaran23);
		return subMatapelajaran23;
	}

	/**
	 * Menyetel sub mata pelajaran (komponen) pada <b>slot ke-23</b>.
	 *
	 * <p>Tanpa validasi bahwa sub mata pelajaran yang disetel benar-benar milik
	 * {@link #getMatapelajaran() matapelajaran} baris ini — pembatasan itu hanya dilakukan
	 * combo di formulir. <b>Tidak pernah dipanggil</b> oleh
	 * {@code GuruMengajarAction.onSave(Event)}, sehingga kolomnya kembali {@code NULL} setiap
	 * kali baris disimpan lewat layar (lihat Javadoc kelas).</p>
	 *
	 * @param subMatapelajaran23 komponen mata pelajaran untuk slot ke-23; {@code null}
	 *         mengosongkan slot
	 */
	public void setSubMatapelajaran23(SubMatapelajaran subMatapelajaran23) {
		this.subMatapelajaran23 = subMatapelajaran23;
	}

	/**
	 * Mengembalikan sub mata pelajaran (komponen) yang diajarkan pada <b>slot ke-24</b>
	 * (kolom {@code sub_matapelajaran_24}).
	 *
	 * <p>Opsional dan sejajar dengan {@link #getHari24() hari24} serta
	 * {@link #getJamPelajaran24() jamPelajaran24} pada slot yang sama. Proxy lazy diselesaikan
	 * lewat {@link ais.database.model.GeneralValueObject#check(Object)} dan ditulis kembali ke
	 * field (object yang sama).</p>
	 *
	 * @return sub mata pelajaran slot ke-24, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_24")
	public SubMatapelajaran getSubMatapelajaran24() {
		subMatapelajaran24 = check(subMatapelajaran24);
		return subMatapelajaran24;
	}

	/**
	 * Menyetel sub mata pelajaran (komponen) pada <b>slot ke-24</b>.
	 *
	 * <p>Tanpa validasi bahwa sub mata pelajaran yang disetel benar-benar milik
	 * {@link #getMatapelajaran() matapelajaran} baris ini — pembatasan itu hanya dilakukan
	 * combo di formulir. <b>Tidak pernah dipanggil</b> oleh
	 * {@code GuruMengajarAction.onSave(Event)}, sehingga kolomnya kembali {@code NULL} setiap
	 * kali baris disimpan lewat layar (lihat Javadoc kelas).</p>
	 *
	 * @param subMatapelajaran24 komponen mata pelajaran untuk slot ke-24; {@code null}
	 *         mengosongkan slot
	 */
	public void setSubMatapelajaran24(SubMatapelajaran subMatapelajaran24) {
		this.subMatapelajaran24 = subMatapelajaran24;
	}

	/**
	 * Mengembalikan sub mata pelajaran (komponen) yang diajarkan pada <b>slot ke-25</b>
	 * (kolom {@code sub_matapelajaran_25}).
	 *
	 * <p>Opsional dan sejajar dengan {@link #getHari25() hari25} serta
	 * {@link #getJamPelajaran25() jamPelajaran25} pada slot yang sama. Proxy lazy diselesaikan
	 * lewat {@link ais.database.model.GeneralValueObject#check(Object)} dan ditulis kembali ke
	 * field (object yang sama).</p>
	 *
	 * @return sub mata pelajaran slot ke-25, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_25")
	public SubMatapelajaran getSubMatapelajaran25() {
		subMatapelajaran25 = check(subMatapelajaran25);
		return subMatapelajaran25;
	}

	/**
	 * Menyetel sub mata pelajaran (komponen) pada <b>slot ke-25</b>.
	 *
	 * <p>Tanpa validasi bahwa sub mata pelajaran yang disetel benar-benar milik
	 * {@link #getMatapelajaran() matapelajaran} baris ini — pembatasan itu hanya dilakukan
	 * combo di formulir. <b>Tidak pernah dipanggil</b> oleh
	 * {@code GuruMengajarAction.onSave(Event)}, sehingga kolomnya kembali {@code NULL} setiap
	 * kali baris disimpan lewat layar (lihat Javadoc kelas).</p>
	 *
	 * @param subMatapelajaran25 komponen mata pelajaran untuk slot ke-25; {@code null}
	 *         mengosongkan slot
	 */
	public void setSubMatapelajaran25(SubMatapelajaran subMatapelajaran25) {
		this.subMatapelajaran25 = subMatapelajaran25;
	}

}
