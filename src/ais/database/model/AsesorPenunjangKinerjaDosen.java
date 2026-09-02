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

/**
 * <b>Master peran (slot) asesor pada proses Beban Kerja Dosen (BKD).</b> Satu baris menjawab
 * pertanyaan: <i>"peran penilai apa saja yang ada dalam satu siklus asesmen BKD?"</i> &mdash;
 * bukan <i>"siapa asesornya"</i>. Memetakan tabel {@code public.asesor_penunjang_kinerja_dosen}.
 *
 * <p><b>Nama kelas ini menyesatkan.</b> Meski mengandung kata "Penunjang", entity ini
 * <b>tidak</b> khusus untuk kategori beban "penunjang" (kategori itu diwakili konstanta
 * {@link PenilaianAsesor#PENUNJANG_DAN_LAIN_LAIN} pada kolom {@code spesifikasi} milik
 * {@link AsesemenPenilaian}). Isi nyatanya jauh lebih sederhana: <b>daftar urutan asesor</b>.
 * Auto-seed di dua tempat (lihat di bawah) mengisi tabel ini dengan tepat tiga baris
 * &mdash; {@code A/"Asesor I"}, {@code B/"Asesor II"}, {@code C/"Asesor III"} &mdash; dan
 * seluruh konsumennya memperlakukan baris di sini sebagai <b>kolom penilai ke-<i>n</i></b>
 * pada laporan BKD.</p>
 *
 * <h3>Posisinya dalam rantai BKD</h3>
 *
 * <p>Empat entity membentuk rantai penugasan &rarr; penilaian; kelas ini adalah mata rantai
 * pertama:</p>
 *
 * <ol>
 *   <li><b>{@code AsesorPenunjangKinerjaDosen}</b> (kelas ini) &mdash; <i>peran</i>: "Asesor I",
 *   "Asesor II", &hellip;</li>
 *   <li>{@link Asesor} &mdash; <i>orang</i>: satu {@link Tbmuser} yang menjabat peran tersebut
 *   (FK {@code asesor_penunjang_kinerja_dosen}). Beberapa pengguna boleh menjabat peran yang
 *   sama.</li>
 *   <li>{@link AsesorPegawai} &mdash; <i>penugasan</i>: pasangan asesor &harr; {@link Pegawai}
 *   yang dinilainya (asesi).</li>
 *   <li>{@link PenilaianAsesor} &mdash; <i>hasil</i>: SKS kinerja yang diakui asesor atas satu
 *   butir {@link AsesemenPenilaian}.</li>
 * </ol>
 *
 * <p>Karena itu jumlah baris aktif di tabel ini <b>menentukan berapa kolom nilai</b> yang
 * muncul di laporan kinerja ({@code ais.action.master.bkd.KinerjaAction}) dan berapa baris
 * {@link PenilaianAsesor} yang dilahirkan {@code PenilaianAsesorAction.checkPenilaian(...)}
 * untuk setiap butir beban.</p>
 *
 * <h3>Siapa yang memakai entity ini</h3>
 *
 * <ul>
 *   <li>{@code ais.action.master.bkd.AsesorPenunjangKinerjaDosenAction} &mdash; layar master
 *   ({@code /pages/master/bkd/asesor_penunjang_kinerja_dosen.zul}): CRUD peran + auto-seed.
 *   Tiap baris grid menempelkan panel rincian
 *   {@code ais.action.master.bkd.helper.AssesorAction} untuk mengangkat/mencabut pengguna
 *   sebagai {@link Asesor} pada peran itu.</li>
 *   <li>{@code ais.action.master.bkd.PegawaiAction} &mdash; daftar asesi milik asesor yang
 *   sedang login; peran dipilih lewat combo, lalu dipakai memfilter {@link AsesorPegawai}.</li>
 *   <li>{@code ais.action.master.bkd.PenilaianAsesorAction} &mdash; layar penilaian rinci
 *   ({@code asesor_memberikan_penilaian_rinci.zul}); peran dipakai sebagai filter dan sebagai
 *   argumen {@code prosesUlang(...)}.</li>
 *   <li>{@code ais.action.master.bkd.KinerjaAction} &mdash; laporan rekap: setiap peran aktif
 *   menjadi satu kolom SKS pada SQL native yang dirakit dinamis.</li>
 *   <li>{@code ais.action.master.BiodataDosenAction} &mdash; pada biodata dosen, setiap peran
 *   aktif menjadi satu baris isian "siapa asesor peran ini untuk dosen ini" (menulis
 *   {@link AsesorPegawai}). Hanya muncul bila konfigurasi {@code tampilkan_asesor} aktif.</li>
 *   <li>{@code ais.action.master.DosenAction} &mdash; hanya auto-seed (lihat di bawah).</li>
 *   <li>{@code PenilaianAsesorHelper}, {@code DosenMengajarHelper}, {@code DetailArtikelHelper},
 *   {@code PengajuanPenelitianDanPengabdianHelper}, {@code PenunjangKinerjaDosenAction}
 *   &mdash; hanya membaca {@code kode}/{@code nama} sebagai label kolom penilaian.</li>
 * </ul>
 *
 * <h3>Hal non-obvious yang perlu diketahui sebelum menyentuh kelas ini</h3>
 *
 * <ol>
 *   <li><b>Auto-seed dari DUA layar berbeda, dan hasilnya bergantung siapa yang membukanya
 *   duluan.</b> Blok yang identik ada di
 *   {@code AsesorPenunjangKinerjaDosenAction.doAfterCompose(...)} <i>dan</i> di
 *   {@code DosenAction} (layar master dosen &mdash; sama sekali bukan layar BKD). Keduanya
 *   memeriksa {@code rowCount() == 0} lalu menyisipkan tiga baris A/B/C dengan
 *   {@code fakultas}/{@code jurusan} diambil dari <b>pengguna yang sedang login</b>
 *   ({@code tbmuser.ambilFakultas()}/{@code ambilJurusan()}). Konsekuensinya: pada instalasi
 *   baru, fakultas/jurusan ketiga baris master ini ditentukan oleh kebetulan &mdash; siapa pun
 *   yang pertama kali membuka salah satu dari dua layar itu. Membuka layar master dengan hak
 *   READ saja sudah cukup untuk memicu INSERT.</li>
 *   <li><b>{@code fakultas}/{@code jurusan} tidak pernah ditegakkan sebagai cakupan.</b>
 *   Diverifikasi satu per satu pada seluruh konsumen: {@code AssesorAction.loadData(...)},
 *   {@code PegawaiAction.initCriteria(...)}, {@code PenilaianAsesorAction.initCriteria(...)},
 *   {@code KinerjaAction} dan {@code BiodataDosenAction} memfilter tabel ini <b>hanya</b>
 *   dengan {@code aktif}, tidak pernah dengan {@code fakultas} maupun {@code jurusan}. Kedua
 *   kolom itu praktis dekoratif: dipakai sebagai label kolom grid dan sebagai filter pencarian
 *   di layar masternya sendiri, tidak lebih. Peran yang "milik Fakultas Teknik" tetap tampil
 *   dan tetap dipakai untuk seluruh fakultas.</li>
 *   <li><b>{@link #getAktif()} adalah getter yang MENULIS balik ke field.</b> Bila
 *   {@code aktif} masih {@code null}, getter menetapkannya ke {@code true} sebelum
 *   mengembalikan nilai. Karena kelas ini memakai <i>property access</i> (anotasi menempel
 *   pada getter), sekadar me-<i>render</i> daftar peran dapat membuat Hibernate melihat
 *   perubahan {@code null &rarr; true} pada entity terkelola dan menerbitkan UPDATE. Ini
 *   satu-satunya getter di kelas ini yang mengubah nilai skalar.</li>
 *   <li><b>Dua dialek filter {@code aktif} yang tidak konsisten.</b>
 *   {@code PenilaianAsesorAction} dan {@code PegawaiAction} memakai
 *   {@code Restrictions.eq("aktif", true)} (baris {@code NULL} <b>hilang</b>), sedangkan
 *   {@code BiodataDosenAction} dan {@code AssesorAction} memakai
 *   {@code Restrictions.or(isNull("aktif"), eq("aktif", true))} (baris {@code NULL}
 *   <b>ikut</b>). Selama {@link #getAktif()} sempat dipanggil dan hasilnya ter-flush,
 *   perbedaan ini tidak terlihat; pada baris warisan yang belum pernah dirender lewat
 *   Hibernate, dua layar akan menampilkan daftar peran yang berbeda.</li>
 *   <li><b>Jumlah peran aktif adalah PEMBAGI rata-rata BKD.</b> Di {@code KinerjaAction}
 *   nilai akhir dihitung {@code total / asesorPenunjangKinerjaDosens.size()}, dengan daftar
 *   peran diambil memakai {@code Restrictions.eq("asesorPenunjangKinerjaDosen.aktif", true)}.
 *   Menonaktifkan satu peran (atau membiarkannya {@code NULL}) karena itu <b>mengubah
 *   persentase capaian kinerja seluruh dosen secara surut</b>, bukan sekadar menyembunyikan
 *   satu kolom. Bila seorang dosen tidak punya penugasan asesor sama sekali, daftar itu kosong
 *   dan pembagian menghasilkan {@code NaN}/{@code Infinity} (bukan exception) yang langsung
 *   dirender ke laporan.</li>
 *   <li><b>{@link #getNama()} memangkas spasi saat DIBACA, tetapi
 *   {@link #setNama(String)} tidak.</b> Nilai yang tersimpan di kolom {@code nama} bisa
 *   mengandung spasi di ujung, sementara pemeriksaan duplikat
 *   {@code AsesorPenunjangKinerjaDosenAction.checkNamaAsesorPenunjangKinerjaDosen()}
 *   membandingkan dengan {@code Restrictions.eq("nama", nama.getValue().trim())} langsung ke
 *   kolom. Akibatnya nama yang tersimpan dengan spasi di ujung <b>lolos</b> dari pemeriksaan
 *   duplikat, dan di UI kedua baris tampak bernama sama persis. {@link #toString()} juga
 *   membaca field mentah, bukan getternya, sehingga bisa berbeda dari yang tampil di layar.</li>
 *   <li><b>{@link #getKeterangan()} MEMBALIK kontrak kelas induk.</b>
 *   {@link GeneralValueObject#getKeterangan()} menjamin tidak pernah {@code null} (mengubah
 *   {@code null} menjadi {@code ""}); override di sini mengembalikan field apa adanya sehingga
 *   <b>bisa</b> {@code null}. Efek sampingnya: kunci urut ketiga
 *   {@link GeneralValueObject#compareTo(GeneralValueObject)} (yang bercabang pada
 *   {@code getKeterangan() != null}) diam-diam nonaktif untuk entity ini.</li>
 *   <li><b>Deklarasi ulang {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah}
 *   BUKAN duplikasi yang bisa dihapus.</b> {@link GeneralValueObject} adalah POJO abstrak
 *   biasa &mdash; bukan {@code @Entity} maupun {@code @MappedSuperclass} &mdash; sehingga
 *   Hibernate sama sekali tidak memetakan properti kelas induk. Setiap entity turunan wajib
 *   mendeklarasikan sendiri kolom-kolom itu agar terpetakan. Hal yang sama berlaku untuk
 *   {@code kode}, {@code nama} dan {@code keterangan}: ketiganya juga ada di kelas induk,
 *   dan deklarasi ulang di sini yang membuatnya benar-benar tersimpan.</li>
 *   <li><b>Tidak ada jejak pembuat.</b> Ada {@code @PreUpdate} ({@link #onUpdate()}) tetapi
 *   tidak ada {@code @PrePersist}, sehingga {@code oleh}/{@code olehId} hanya terisi saat
 *   baris di-<i>update</i>. Tiga baris hasil auto-seed karena itu tidak mencatat siapa yang
 *   memicunya. Riwayat lengkap tetap tersedia lewat {@code @Audited} (Hibernate Envers) di
 *   tabel {@code asesor_penunjang_kinerja_dosen_AUD}; layar master menampilkannya lewat
 *   {@code RevisiHelper.createNewRevisi(...)} pada kolom Nama.</li>
 *   <li><b>Konstruktor {@link #AsesorPenunjangKinerjaDosen(Long)} tidak pernah dipakai.</b>
 *   Ketujuh pemanggilan {@code new AsesorPenunjangKinerjaDosen(...)} di seluruh repo memakai
 *   konstruktor tanpa argumen.</li>
 *   <li><b>Komentar generator "Bank generated by hbm2java" salah nama</b> (sisa salin-tempel
 *   generator Apr 2010); tidak ada hubungannya dengan entity {@link Bank}. Nilai
 *   {@code serialVersionUID} pun identik dengan {@link Asesor} dan {@link PenilaianAsesor},
 *   ciri khas berkas hasil generator yang sama.</li>
 * </ol>
 *
 * <h3>Verifikasi pola berulang (diperiksa langsung dari kode kelas ini)</h3>
 *
 * <ul>
 *   <li><b>Getter yang menulis balik ke field:</b> ADA &mdash; {@link #getAktif()}
 *   ({@code null &rarr; true}). Selain itu {@link #getJurusan()} dan {@link #getFakultas()}
 *   juga menetapkan ulang fieldnya dengan hasil
 *   {@link GeneralValueObject#check(Object)}, tetapi yang ditulis balik adalah <i>entity yang
 *   sama secara identitas</i> (proxy yang sudah teresolusi atau instance kanonik dari cache),
 *   sehingga nilai FK-nya tidak berubah dan tidak memicu UPDATE.</li>
 *   <li><b>Getter destruktif</b> (menghapus baris/menihilkan relasi seperti
 *   {@code Komentar.getTbmuser()} atau {@code OrganisasiDosenPunyaDosen}): TIDAK ADA di kelas
 *   ini. Tidak satu pun getter memanggil {@code delete}, {@code setNull} atau sejenisnya.</li>
 *   <li><b>Getter yang menutup sesi Hibernate:</b> TIDAK ADA secara langsung &mdash; kelas ini
 *   tidak pernah menyentuh {@code HibernateUtil}. Perlu dicatat bahwa
 *   {@link GeneralValueObject#check(Object)} yang dipanggil {@link #getJurusan()}/
 *   {@link #getFakultas()} <i>dapat</i> membuka sesi tersendiri ({@code openSession()}) untuk
 *   memuat ulang proxy yang sudah detached lalu menutupnya di blok {@code finally}; sesi
 *   milik request yang sedang berjalan tidak pernah ditutup.</li>
 *   <li><b>Method bisnis / query statis:</b> TIDAK ADA. Kelas ini murni pemegang data;
 *   seluruh aturan bisnis berada di {@code AsesorPenunjangKinerjaDosenAction},
 *   {@code AssesorAction} dan keluarga {@code Bkd*Helper}.</li>
 * </ul>
 *
 * <h3>Catatan kontrol akses</h3>
 *
 * <p>Berbeda dari {@link PenilaianAsesor} &mdash; yang layar {@code PenilaianAsesorAction}-nya
 * menerima parameter URL {@code ?pegawai=<id>} dan memuat {@link Pegawai} mana pun berdasarkan
 * id mentah tanpa memeriksa apakah pengguna berhak atas dosen itu &mdash; layar master kelas
 * ini <b>tidak membaca parameter URL sama sekali</b> dan memasang gerbang lengkap:
 * {@code Common.doCheckSecurity()} di {@code doBeforeCompose}, {@code checkPrevilages(READ)}
 * di {@code doAfterCompose}, serta gerbang CREATE/UPDATE/DELETE terpisah pada tombol Tambah,
 * checkbox Aktif, tombol Ubah dan tombol Hapus. Untuk entity ini jalur ZK-nya adalah
 * <b>contoh positif</b>.</p>
 *
 * <p>Dua hal tetap perlu diketahui:</p>
 *
 * <ul>
 *   <li><b>Entity ini adalah master pemberi wewenang.</b> Panel {@code AssesorAction} pada tiap
 *   baris memungkinkan pemegang hak CREATE di menu ini mengangkat {@link Tbmuser} mana pun
 *   &mdash; tanpa syarat harus pegawai/dosen, dan tanpa dibatasi fakultas/jurusan baris yang
 *   sedang dibuka &mdash; menjadi {@link Asesor}. Status asesor itulah yang kemudian dipakai
 *   {@code PenilaianAsesorHelper.formNilai(...)} sebagai gerbang tulis atas nilai BKD. Jadi
 *   hak CREATE di layar master ini setara dengan kemampuan memberi wewenang menilai BKD.</li>
 *   <li><b>Id baris kelas ini juga dapat dipasok lewat URL.</b>
 *   {@code PenilaianAsesorAction.doAfterCompose(...)} membaca {@code ?asesor=<id>} (di samping
 *   {@code ?pegawai=}, {@code ?ta=} dan {@code ?smt=}) dan memuat baris ini dengan
 *   {@code Restrictions.idEq(Long.parseLong(...))}. Untuk kelas ini sendiri parameter itu tidak
 *   membocorkan apa pun yang sensitif (isi tabel ini memang label peran, dan combonya toh
 *   menampilkan seluruh peran aktif) &mdash; catatannya hanyalah bahwa seluruh dimensi filter
 *   layar penilaian dapat dikendalikan dari URL, sehingga tidak ada penyempitan cakupan yang
 *   dijaga sisi server pada jalur tersebut.</li>
 * </ul>
 *
 * <h3>Pengelompokan anggota kelas</h3>
 *
 * <ul>
 *   <li><b>Jejak audit</b>: {@link #getOleh()}/{@link #setOleh(String)},
 *   {@link #getOlehId()}/{@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 *   <li><b>Identitas</b>: {@link #getId()}/{@link #setId(Long)}, {@link #toString()},
 *   konstruktor {@link #AsesorPenunjangKinerjaDosen()} dan
 *   {@link #AsesorPenunjangKinerjaDosen(Long)}.</li>
 *   <li><b>Isi master</b>: {@link #getKode()}/{@link #setKode(String)},
 *   {@link #getNama()}/{@link #setNama(String)},
 *   {@link #getKeterangan()}/{@link #setKeterangan(String)}.</li>
 *   <li><b>Cakupan (tidak ditegakkan)</b>: {@link #getFakultas()}/{@link #setFakultas(Fakultas)},
 *   {@link #getJurusan()}/{@link #setJurusan(Jurusan)}.</li>
 *   <li><b>Saklar</b>: {@link #getAktif()}/{@link #setAktif(Boolean)}.</li>
 * </ul>
 *
 * @see Asesor
 * @see AsesorPegawai
 * @see PenilaianAsesor
 * @see AsesemenPenilaian
 * @see PenunjangKinerjaDosen
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "asesor_penunjang_kinerja_dosen")
public class AsesorPenunjangKinerjaDosen extends GeneralValueObject {

	/**
	 * Versi serialisasi bawaan generator. Nilainya identik dengan {@link Asesor} dan
	 * {@link PenilaianAsesor} karena ketiganya dihasilkan oleh sesi hbm2java yang sama;
	 * jangan diubah agar sesi ZK yang di-<i>passivate</i> tetap dapat dipulihkan.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci primer baris (kolom {@code id}, {@code IDENTITY}); dideklarasikan ulang karena kelas induk tidak terpetakan Hibernate. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris; diisi {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris; pasangan teknis dari {@link #oleh}. */
	private String olehId;

	/** @return id pengguna terakhir yang mengubah baris, atau {@code null} bila baris belum pernah di-update */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Menolak nilai kosong secara diam-diam:</b> {@code null} maupun string yang hanya
	 * berisi spasi diabaikan (method langsung {@code return}), sehingga nilai lama
	 * dipertahankan. Perilaku ini disengaja agar jejak audit tidak terhapus oleh proses yang
	 * memanggil setter dengan nilai kosong.</p>
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null} atau kosong diabaikan
	 * secara diam-diam sehingga jejak audit sebelumnya tidak tertimpa.</p>
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pengguna terakhir yang mengubah baris, atau {@code null} bila baris belum pernah di-update */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mengisi {@code oleh}/{@code olehId}/{@code tanggal_dirubah}
	 * dari pengguna sesi berjalan lewat
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} tepat sebelum baris
	 * di-UPDATE. Tidak ada padanan {@code @PrePersist}, jadi tiga baris hasil auto-seed
	 * ({@code AsesorPenunjangKinerjaDosenAction}/{@code DosenAction}) tidak mencatat siapa
	 * pembuatnya. Pada baris deklarasi yang sama juga dideklarasikan field
	 * {@code tanggal_dirubah}, yang diinisialisasi ke waktu server saat objek dibuat
	 * ({@code ais.ui.util.WaktuUtil.getDate()}) sehingga baris baru tetap punya stempel waktu
	 * meski belum pernah di-update.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah stempel waktu perubahan terakhir baris ini */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return stempel waktu perubahan terakhir (kolom {@code tanggal_dirubah}, presisi TIMESTAMP) */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini dalam bentuk {@code "<id>-<nama>"}.
	 *
	 * <p><b>Membaca field {@code nama} secara langsung, bukan {@link #getNama()}</b>, sehingga
	 * spasi di ujung <b>tidak</b> dipangkas &mdash; hasilnya bisa berbeda dari nama yang tampil
	 * di layar. Untuk baris yang belum tersimpan, bagian id berisi {@code "null"}. Jangan
	 * dipakai sebagai label UI maupun kunci pencocokan.</p>
	 *
	 * @return gabungan id dan nama mentah, mis. {@code "12-Asesor I"}
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Kode ringkas peran (kolom {@code kode}). Auto-seed mengisi {@code "A"}, {@code "B"},
	 * {@code "C"}. Wajib diisi dan wajib unik menurut validasi UI
	 * ({@code checkKodeAsesorPenunjangKinerjaDosen()}), tetapi keunikan itu <b>tidak</b>
	 * ditegakkan lewat constraint database.
	 */
	private String kode;
	/**
	 * Nama peran yang tampil sebagai judul kolom nilai (kolom {@code nama}, {@code NOT NULL},
	 * maksimal 255 karakter). Auto-seed mengisi {@code "Asesor I"}, {@code "Asesor II"},
	 * {@code "Asesor III"}.
	 */
	private String nama;
	/** Catatan bebas atas peran ini (kolom {@code keterangan}, boleh {@code null}); hanya ditampilkan di grid master. */
	private String keterangan;
	/**
	 * Jurusan pemilik peran (kolom FK {@code jurusan}, boleh {@code null} = "Semua").
	 *
	 * <p><b>Tidak pernah ditegakkan sebagai cakupan</b> oleh konsumen mana pun &mdash; lihat
	 * javadoc kelas. Diisi otomatis dari jurusan pengguna yang memicu auto-seed atau yang
	 * menekan tombol Tambah.</p>
	 */
	private Jurusan jurusan;
	/**
	 * Fakultas pemilik peran (kolom FK {@code fakultas}, boleh {@code null} = "Semua").
	 *
	 * <p>Sama seperti {@link #jurusan}: dipakai sebagai label dan filter pencarian di layar
	 * master saja, tidak pernah membatasi peran mana yang berlaku bagi dosen tertentu.</p>
	 */
	private Fakultas fakultas;
	/**
	 * Penanda peran masih dipakai (kolom {@code aktif}).
	 *
	 * <p>Nilai {@code null} diperlakukan sebagai {@code true} oleh {@link #getAktif()}, yang
	 * sekaligus menulis balik ke field ini. Perhatikan dampaknya pada laporan: jumlah peran
	 * aktif adalah pembagi rata-rata SKS kinerja di {@code KinerjaAction}.</p>
	 */
	private Boolean aktif;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate; ini pula satu-satunya konstruktor
	 * yang benar-benar dipakai kode aplikasi (auto-seed dan tombol Tambah pada layar master).
	 */
	public AsesorPenunjangKinerjaDosen() {
	}

	/**
	 * Konstruktor pintas berisi kunci primer saja, warisan generator hbm2java.
	 *
	 * <p><b>Tidak dipakai di mana pun</b> pada repo ini; seluruh pembuatan instance memakai
	 * konstruktor tanpa argumen. Objek hasil konstruktor ini tidak terkelola Hibernate dan
	 * seluruh properti lainnya {@code null} &mdash; berguna hanya sebagai pembanding
	 * berbasis id.</p>
	 *
	 * @param id kunci primer baris yang diwakili
	 */
	public AsesorPenunjangKinerjaDosen(Long id) {
		this.id = id;
	}

	/** @return kunci primer baris; {@code null} bila entity belum tersimpan */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci primer secara manual. Karena kolomnya {@code IDENTITY} dan
	 * {@code insertable = false}, nilainya normalnya diisi database; setter ini hanya dipakai
	 * Hibernate saat hidrasi.
	 *
	 * @param id kunci primer baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama peran dengan spasi di ujung <b>dipangkas</b>.
	 *
	 * <p>Pemangkasan hanya terjadi saat membaca &mdash; {@link #setNama(String)} menyimpan apa
	 * adanya, dan pemeriksaan duplikat di layar master membandingkan langsung ke kolom, bukan
	 * lewat getter ini. Karena itu dua baris yang di layar terlihat bernama sama persis bisa
	 * saja lolos validasi keunikan (lihat javadoc kelas).</p>
	 *
	 * @return nama peran tanpa spasi di ujung, atau {@code null} bila field belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama peran. Tanpa validasi dan <b>tanpa pemangkasan</b>; kewajiban isi dan
	 * keunikan hanya diperiksa di layar master
	 * ({@code onSave()} / {@code checkNamaAsesorPenunjangKinerjaDosen()}), bukan di sini.
	 *
	 * @param nama nama peran baru; kolomnya {@code NOT NULL}, jadi {@code null} akan gagal saat flush
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan catatan atas peran ini <b>apa adanya</b>.
	 *
	 * <p><b>Membalik kontrak kelas induk:</b> {@link GeneralValueObject#getKeterangan()}
	 * menjamin hasilnya tidak pernah {@code null} (mengubahnya menjadi {@code ""}), sedangkan
	 * override ini dapat mengembalikan {@code null}. Pemanggil yang mengandalkan jaminan kelas
	 * induk perlu memeriksa {@code null} sendiri.</p>
	 *
	 * @return catatan peran, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel catatan atas peran ini. Tanpa validasi; {@code null} diterima dan akan terbaca
	 * kembali sebagai {@code null} (bukan {@code ""}) lewat {@link #getKeterangan()}.
	 *
	 * @param keterangan catatan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan jurusan pemilik peran, setelah proxy lazy diresolusi lewat
	 * {@link GeneralValueObject#check(Object)}.
	 *
	 * <p><b>Efek samping:</b> hasil {@code check(...)} ditulis balik ke field {@link #jurusan}.
	 * Yang ditulis adalah entity yang sama secara identitas (proxy yang sudah teresolusi atau
	 * instance kanonik dari cache), sehingga nilai FK tidak berubah dan UPDATE tidak terpicu.
	 * {@code check(...)} sendiri dapat membuka sesi Hibernate tersendiri untuk memuat ulang
	 * proxy yang sudah <i>detached</i> lalu menutupnya kembali; sesi milik request yang sedang
	 * berjalan tidak pernah disentuh.</p>
	 *
	 * @return jurusan pemilik peran, atau {@code null} yang berarti "Semua jurusan"
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Menyetel jurusan pemilik peran.
	 *
	 * <p>Karena relasinya memakai {@code CascadeType.PERSIST, MERGE}, menyimpan baris ini ikut
	 * mem-persist/merge {@link Jurusan} yang menempel padanya. Nilai {@code null} berarti
	 * "Semua jurusan". Ingat bahwa nilai ini tidak menentukan berlakunya peran bagi dosen mana
	 * pun (lihat javadoc kelas).</p>
	 *
	 * @param jurusan jurusan pemilik, atau {@code null} untuk "Semua"
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan fakultas pemilik peran, setelah proxy lazy diresolusi lewat
	 * {@link GeneralValueObject#check(Object)}.
	 *
	 * <p>Efek sampingnya persis sama dengan {@link #getJurusan()}: hasil resolusi ditulis balik
	 * ke field {@link #fakultas} tanpa mengubah nilai FK. Perhatikan bahwa
	 * {@code AsesorPenunjangKinerjaDosenAction.init(...)} memakai nilai ini untuk membatasi
	 * daftar jurusan pada combo &mdash; satu-satunya tempat kedua kolom cakupan itu benar-benar
	 * berpengaruh, dan itu pun hanya pada pilihan form, bukan pada penegakan data.</p>
	 *
	 * @return fakultas pemilik peran, atau {@code null} yang berarti "Semua fakultas"
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * Menyetel fakultas pemilik peran.
	 *
	 * <p>Sama seperti {@link #setJurusan(Jurusan)}, cascade {@code PERSIST}/{@code MERGE} ikut
	 * menyentuh {@link Fakultas} yang dipasang. Nilai {@code null} berarti "Semua fakultas".</p>
	 *
	 * @param fakultas fakultas pemilik, atau {@code null} untuk "Semua"
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Mengembalikan kode ringkas peran apa adanya (tanpa pemangkasan spasi, berbeda dari
	 * {@link #getNama()}).
	 *
	 * <p>Tidak memiliki anotasi {@code @Column} sendiri sehingga terpetakan secara implisit ke
	 * kolom {@code kode}. Dipakai sebagai kolom "Kode" di grid master dan sebagai label singkat
	 * kolom penilaian di {@code PenilaianAsesorHelper}.</p>
	 *
	 * @return kode peran, atau {@code null} bila belum diisi
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Menyetel kode ringkas peran. Tanpa validasi; kewajiban isi dan keunikan diperiksa di
	 * layar master ({@code checkKodeAsesorPenunjangKinerjaDosen()}), bukan oleh constraint
	 * database.
	 *
	 * @param kode kode peran baru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan status aktif peran, dengan {@code null} dianggap {@code true}.
	 *
	 * <p><b>Getter yang MENULIS balik:</b> bila field {@link #aktif} masih {@code null}, method
	 * ini menetapkannya ke {@code true} lebih dulu. Karena kelas ini memakai <i>property
	 * access</i>, sekadar merender daftar peran dapat membuat Hibernate mendeteksi perubahan
	 * {@code null &rarr; true} pada entity terkelola dan menerbitkan UPDATE &mdash; jadi
	 * membuka layar master bisa menormalkan kolom {@code aktif} baris-baris warisan tanpa ada
	 * yang menekan tombol simpan.</p>
	 *
	 * <p>Konsekuensi hilirnya penting: nilai ini menentukan apakah peran muncul di combo
	 * penilaian, di biodata dosen, dan &mdash; yang paling berdampak &mdash; apakah ia ikut
	 * menjadi pembagi rata-rata SKS kinerja di {@code KinerjaAction}.</p>
	 *
	 * @return {@code true} bila peran masih dipakai; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menyetel status aktif peran. Dipanggil dari checkbox "Aktif" pada grid layar master
	 * (yang langsung menyimpan lewat {@code Common.refreshSaveOrUpdate(...)} pada event
	 * {@code onCheck}), jadi satu klik langsung berdampak ke laporan kinerja seluruh dosen.
	 *
	 * @param aktif status baru; {@code null} akan terbaca kembali sebagai {@code true}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
