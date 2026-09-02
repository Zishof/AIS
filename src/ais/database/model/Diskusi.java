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
 * Entity <b>satu utas (thread) diskusi editorial jurnal ilmiah</b> (tabel {@code public.diskusi}) —
 * satu baris mewakili sebuah topik pembicaraan tertutup yang menempel pada sebuah naskah
 * ({@link #getRepoItemId()}) dalam sebuah jurnal ({@link #getJurnalPenelitianId()}), pada tahap
 * alur kerja tertentu ({@link #getStageKey()}). Balasan/isi percakapannya <b>tidak</b> disimpan di
 * sini melainkan sebagai baris-baris {@link DiskusiKomentar} yang menunjuk balik ke baris ini lewat
 * {@link DiskusiKomentar#getDiskusi()}; daftar peserta utas disimpan di entity ketiga,
 * {@code ais.database.model.jurnal.PesertaDiskusiJurnal}.
 *
 * <h3>JEBAKAN TERBESAR — ini BUKAN diskusi pengumuman/perkuliahan</h3>
 * <p>Nama {@code Diskusi} tanpa akhiran sangat mudah disalahartikan sebagai "diskusi" di modul
 * akademik. Berdasarkan verifikasi menyeluruh atas seluruh <i>source tree</i> (hanya tiga berkas
 * yang meng-{@code import ais.database.model.Diskusi}), class ini <b>tidak dipakai sama sekali</b>
 * oleh modul pengumuman, perkuliahan, maupun e-learning. Yang dipakai di sana adalah entity lain
 * yang namanya mirip:</p>
 * <ul>
 * <li>{@link DiskusiPengumumanAkademis}, {@link DiskusiPengumumanPerkuliahan},
 * {@link DiskusiPengumumanPenelitian}, {@link DiskusiPenelitianDanPengabdian} — masing-masing utas
 * tanggapan pada satu pengumuman/pengajuan di modulnya sendiri.</li>
 * <li>{@link PertemuanPunyaDiskusi} — forum tanya-jawab per pertemuan kuliah (e-learning). Inilah
 * "Diskusi" yang muncul di tab {@code PertemuanHelper}, {@code TampilanELearningAction}, seluruh
 * dasbor rekap aktivitas perkuliahan, dan tombol "Ikut Diskusi (n diskusi)" di layar absensi.
 * Semua label berbunyi "Diskusi" pada UI akademik merujuk ke entity itu, bukan ke class ini.</li>
 * <li>{@code ais.database.model.jurnal.PesertaDiskusiJurnal} — tabel keanggotaan utas milik class
 * ini (satu-satunya kerabat yang benar-benar berpasangan dengannya).</li>
 * </ul>
 * <p>Pasangan {@code Diskusi} + {@link DiskusiKomentar} adalah struktur generik lama hasil
 * {@code hbm2java} (Apr 2010) yang <b>dipakai ulang</b> oleh modul jurnal; komentar pada berkas
 * {@code JurnalDiscussionService} menyatakannya secara eksplisit: <i>"Existing Diskusi/
 * DiskusiKomentar extended for journal workflow; only membership needs a new table."</i> Lima field
 * terakhir ({@code jurnal_penelitian_id} … {@code anonymity_mode}) adalah tambahan modern untuk
 * keperluan itu, ditulis dengan gaya satu-baris yang kontras dengan bagian generator di atasnya.</p>
 *
 * <h3>Peta pemakaian (hasil verifikasi, bukan asumsi)</h3>
 * <ul>
 * <li><b>Penulis utama:</b> {@code ais.action.master.jurnal.JurnalDiscussionService#create(...)} —
 * satu-satunya jalur pembuatan utas dari pengguna. Dipanggil dari servlet
 * {@code ais.action.servlet.JurnalAdminApi} lewat aksi JSON {@code createDiscussion}, yang formnya
 * ada di {@code webapp/WEB-INF/baru/modul/jurnal/admin.jsp} pada grup {@code prosesReview}.</li>
 * <li><b>Penulis kedua:</b> {@code ais.action.master.jurnal.importer.OjsDomainTransformService
 * #discussion(...)} — memetakan tabel {@code queries} milik OJS (Open Journal Systems) ke baris
 * {@code Diskusi} saat migrasi data. Lihat catatan kosakata di bawah.</li>
 * <li><b>Pembaca:</b> {@code JurnalDiscussionService#addParticipant(...)} dan {@code #comment(...)}
 * (memuat ulang utas via {@code session.get(Diskusi.class, id)} untuk memeriksa cakupan), serta
 * {@code JurnalInvitationDiscussionSelfTest} (uji mandiri).</li>
 * <li><b>Terdaftar di</b> {@code hibernate.cfg.xml} baris 1007 (tepat sebelum
 * {@link DiskusiKomentar}).</li>
 * </ul>
 *
 * <h3>PERINGATAN: entity ini praktis "tulis-saja" (write-only)</h3>
 * <p>Tidak ada satu pun layar ZK, JSP, atau aksi API di seluruh repo yang <b>menampilkan daftar
 * atau isi</b> utas diskusi jurnal. {@code JurnalAdminApi} hanya menyediakan {@code createDiscussion},
 * {@code addDiscussionParticipant} dan {@code commentDiscussion} — ketiganya menulis;
 * {@code JurnalWorkspaceService} (satu-satunya jalur baca modul jurnal) tidak menyentuh
 * {@code Diskusi} sama sekali. Konsekuensi praktisnya ada dua, dan keduanya penting:</p>
 * <ol>
 * <li>Fitur ini <b>belum selesai</b>: pengguna bisa membuat utas dan mengirim komentar, tetapi tidak
 * bisa membacanya kembali lewat antarmuka resmi mana pun.</li>
 * <li>Karena tidak ada jalur baca resmi, <b>satu-satunya</b> cara membaca isi diskusi peer-review
 * dari luar adalah lewat endpoint reflektif generik {@code /Api} aksi {@code dataRinci}
 * ({@code ElearningApiUtil#dataRinci}) — yang menerima nama kelas apa pun dari klien
 * ({@code Class.forName(request.getString("class"))}), tanpa daftar-putih dan tanpa pemeriksaan
 * kepemilikan apa pun; hanya butuh token login yang sah (mahasiswa mana pun). Lihat bagian
 * keamanan di bawah.</li>
 * </ol>
 *
 * <h3>Relasi dengan {@code GeneralValueObject}</h3>
 * <p>Class ini turunan langsung {@link ais.database.model.GeneralValueObject}, yang <b>bukan</b>
 * {@code @Entity} maupun {@code @MappedSuperclass} melainkan POJO abstrak biasa — Hibernate
 * <b>tidak</b> memetakan properti induknya. Karena itu deklarasi ULANG {@link #id}, {@link #oleh},
 * {@link #olehId} dan {@link #tanggal_dirubah} di sini <b>bukan bug atau duplikasi ceroboh</b>,
 * melainkan keharusan teknis supaya keempat kolom itu ikut terpetakan. Field-field tersebut
 * <b>membayangi (shadow)</b> field senama milik induk; yang terbaca dari luar selalu versi milik
 * {@code Diskusi} ini. Fasilitas induk yang tetap dipakai: {@code check(...)} (resolusi proxy lazy)
 * pada kelima getter relasi warisan generator.
 * <p><b>Catatan penting yang membedakan class ini dari sebagian entity lain:</b> {@code nama},
 * {@code keterangan} dan {@code tanggal} <b>dideklarasikan ulang dan dipetakan</b> di sini (lihat
 * {@link #getNama()}, {@link #getKeterangan()}, {@link #getTanggal()}), jadi entity ini
 * <i>tidak</i> terkena jebakan "properti warisan yang tidak tersimpan" seperti yang pernah merusak
 * data pada {@code KelasPunyaMahasiswaTemporary}. Yang <b>tidak</b> dideklarasikan ulang dan
 * karena itu <b>tidak tersimpan</b> ke database adalah properti induk lain seperti {@code kode},
 * {@code aktif}, dan {@code urutan} — jangan pernah mengandalkannya pada baris {@code Diskusi}.</p>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ol>
 * <li><b>Identitas &amp; penyajian:</b> {@link #getId()} (PK {@code IDENTITY}), {@link #toString()},
 * konstruktor tanpa argumen {@link #Diskusi()}, {@link #serialVersionUID}.</li>
 * <li><b>Isi utas:</b> {@link #getNama()} (judul diskusi, {@code NOT NULL}),
 * {@link #getKeterangan()} (deskripsi pembuka, maks 1000 karakter), {@link #getTanggal()}
 * (waktu utas dibuka).</li>
 * <li><b>Cakupan jurnal (tambahan modern, penunjuk skalar tanpa FK):</b>
 * {@link #getJurnalPenelitianId()}, {@link #getRepoItemId()}, {@link #getStageKey()}.</li>
 * <li><b>Kebijakan keterbukaan (tambahan modern):</b> {@link #getVisibility()},
 * {@link #getAnonymityMode()} — lihat peringatan "kebijakan yang tidak pernah ditegakkan".</li>
 * <li><b>Relasi warisan generator, DORMAN pada jalur jurnal:</b> {@link #getMahasiswa()},
 * {@link #getDosen()}, {@link #getTbmuser()}, {@link #getJurusan()}, {@link #getFakultas()} —
 * kelimanya {@code nullable} dan <b>tidak pernah diisi</b> oleh {@code JurnalDiscussionService}
 * maupun importer OJS (identitas pembuat sengaja disimpan skalar di {@link #getOlehId()}, lihat
 * komentar eksplisit di service tersebut). Tetap terpetakan Hibernate dan tetap ikut terserialisasi
 * oleh endpoint reflektif.</li>
 * <li><b>Jejak audit (deklarasi ulang milik induk):</b> {@link #getOleh()}, {@link #getOlehId()},
 * {@link #getTanggal_dirubah()}, kait {@link #onUpdate()}.</li>
 * </ol>
 * <p>Tidak ada method bisnis, tidak ada method query statis, tidak ada validasi dan tidak ada
 * konstanta di class ini: seluruh validasi kosakata ({@code stage}/{@code visibility}/
 * {@code anonymity}), seluruh pemeriksaan hak akses, dan seluruh query berada di
 * {@code JurnalDiscussionService} dan {@code JurnalAuthorizationService}. Class ini murni kantong
 * data + lima getter yang me-resolusi proxy lazy.</p>
 *
 * <h3>Hal non-obvious yang WAJIB diketahui pemelihara</h3>
 * <ul>
 * <li><b>Kelima getter relasi MENULIS BALIK ke field-nya sendiri — verifikasi langsung atas isi
 * berkas ini.</b> {@link #getDosen()}, {@link #getMahasiswa()}, {@link #getJurusan()},
 * {@link #getFakultas()} dan {@link #getTbmuser()} semuanya berpola
 * {@code x = check(x); return x;} — hasil {@link ais.database.model.GeneralValueObject#check(Object)}
 * ditugaskan kembali ke field. Ini <b>disengaja</b> (mekanisme resolusi proxy lazy yang dijelaskan
 * panjang lebar di base class), bukan efek samping destruktif. Perbedaan penting dengan
 * {@link Komentar#getTbmuser()}: di sana getter mengosongkan field menjadi {@code null} sehingga
 * kolomnya ikut terhapus permanen di database; <b>di sini tidak ada satu pun getter yang
 * mengosongkan field, menutup session Hibernate, atau menulis ke database</b>. Perlu diketahui
 * bahwa {@code check(...)} sendiri <i>bisa</i> membuka session Hibernate baru sebagai penyelamat
 * terakhir (tahap 3) dan menutupnya di {@code finally}, jadi getter-getter ini tidak gratis bila
 * dipanggil di dalam perulangan besar.</li>
 * <li><b>KEBIJAKAN {@code visibility} DAN {@code anonymity_mode} TIDAK PERNAH DITEGAKKAN.</b>
 * Penelusuran seluruh repo menunjukkan {@link #getVisibility()} dan {@link #getAnonymityMode()}
 * <b>tidak pernah dipanggil dari mana pun</b> — nilainya hanya divalidasi saat ditulis lalu
 * mengendap di database. {@code JurnalDiscussionService#comment(...)} memutuskan boleh-tidaknya
 * seseorang berkomentar semata-mata dari keanggotaan {@code PesertaDiskusiJurnal} (atau hak
 * {@code prosesReview.update} + cakupan penugasan), <b>tanpa</b> membaca {@code visibility}. Artinya
 * pembedaan {@code INTERNAL} / {@code REVIEWERS} / {@code AUTHOR_EDITOR} / {@code ALL_PARTICIPANTS}
 * saat ini murni dekoratif, dan {@code DOUBLE_ANONYMOUS} <b>tidak menyembunyikan identitas siapa
 * pun</b>: {@link #getOlehId()} pada utas dan pada setiap {@link DiskusiKomentar} berisi
 * {@code userId} asli penulisnya, tanpa lapisan penyamaran apa pun. Untuk sistem peer-review, ini
 * jebakan serius — jangan berasumsi kolom-kolom ini melindungi apa pun.</li>
 * <li><b>Kosakata {@code stageKey}/{@code visibility} TIDAK KONSISTEN antara dua penulisnya.</b>
 * {@code JurnalDiscussionService} memvalidasi {@code stage} hanya menerima {@code SUBMISSION},
 * {@code REVIEW}, {@code COPYEDITING}, {@code PRODUCTION}, {@code PROOF}, dan {@code visibility}
 * hanya menerima {@code INTERNAL}, {@code REVIEWERS}, {@code AUTHOR_EDITOR},
 * {@code ALL_PARTICIPANTS}. Importer OJS <b>menulis langsung ke setter tanpa melewati validasi
 * itu</b> dan menghasilkan nilai di luar kedua daftar: {@code visibility} diisi konstanta
 * {@code "PARTICIPANTS"} (bukan {@code "ALL_PARTICIPANTS"}), sedangkan {@code stageKey} diisi
 * dari kolom OJS {@code stage_id} yang <b>berupa angka</b> ("1".."5"), dengan nilai bawaan
 * {@code "EDITORIAL"} yang juga tidak ada di daftar valid. Baris hasil import karenanya berpotensi
 * tidak pernah cocok dengan penyaring {@code stageKey} mana pun (mis. predikat {@code stageKey}
 * pada {@code JurnalAuthorizationService#requireJournalScope}). Dicatat apa adanya, tidak
 * diperbaiki.</li>
 * <li><b>{@link #getOleh()} kosong sampai baris pertama kali di-UPDATE.</b> Pengisian
 * {@code oleh}/{@code olehId} otomatis hanya terjadi lewat kait {@link #onUpdate()} yang beranotasi
 * {@code @PreUpdate} — <b>tidak ada</b> {@code @PrePersist} di class ini. Saat
 * {@code JurnalDiscussionService#create(...)} menyimpan utas baru, ia mengisi {@link #setOlehId(String)}
 * secara eksplisit tetapi <b>tidak pernah</b> mengisi {@code oleh}, sehingga kolom {@code oleh}
 * baris baru bernilai {@code null}. Begitu baris itu ter-{@code UPDATE} oleh proses/pengguna lain,
 * {@code oleh} dan {@code olehId} akan <b>tertimpa</b> dengan identitas pengguna terakhir yang
 * menyimpan — persis anti-pola yang sama dengan {@link Komentar}. Jangan pakai {@code oleh} sebagai
 * "pembuat diskusi".</li>
 * <li><b>Tidak ada kolom tenant.</b> Berbeda dari kerabat modern-nya
 * ({@code PesertaDiskusiJurnal} punya {@code tenantKey}), {@code Diskusi} tidak menyimpan penanda
 * tenant sama sekali; isolasi antar-tenant sepenuhnya bergantung pada
 * {@link #getJurnalPenelitianId()} yang bukan <i>foreign key</i>.</li>
 * <li><b>{@link #getJurnalPenelitianId()} dan {@link #getRepoItemId()} bukan relasi.</b> Keduanya
 * kolom {@code bigint} biasa tanpa {@code @ManyToOne}/{@code @JoinColumn}, jadi tidak ada
 * <i>foreign key</i> maupun jaminan integritas ke {@code JurnalPenelitian}/{@code RepoItem}. Nilai
 * {@code null} pada salah satunya membuat utas dianggap "bukan diskusi jurnal" dan ditolak
 * {@code JurnalDiscussionService} ("Diskusi jurnal tidak ditemukan."), meskipun barisnya ada di
 * database — jadi baris {@code Diskusi} lama/generik apa pun otomatis tak terjangkau jalur jurnal.</li>
 * <li><b>{@link #toString()} bisa mengembalikan {@code null}</b> karena membaca field {@code nama}
 * mentah (bukan lewat getter). Kolomnya {@code NOT NULL} di database, tetapi object yang belum
 * disimpan atau baru dibentuk konstruktor kosong akan mengembalikan {@code null} dari
 * {@code toString()} — hati-hati bila dipakai sebagai label komponen UI.</li>
 * <li><b>{@link #setOleh(String)} dan {@link #setOlehId(String)} DIAM-DIAM MENGABAIKAN</b> nilai
 * {@code null} atau yang hanya berisi spasi (bukan menyimpannya sebagai {@code null}). Artinya
 * kolom audit tidak bisa dikosongkan lewat setter, dan pemanggil yang mengira sudah mengubahnya
 * tidak mendapat sinyal kegagalan apa pun.</li>
 * <li><b>Seluruh baris diaudit Envers</b> ({@code @Audited}): setiap versi utas tersalin ke tabel
 * audit dan <b>tetap ada di sana meskipun baris aslinya dihapus</b>. Pertimbangkan ini sebelum
 * menganggap penghapusan utas sebagai penghapusan data.</li>
 * <li><b>{@code dynamicInsert}/{@code dynamicUpdate} aktif</b>: Hibernate hanya menuliskan kolom
 * yang benar-benar berubah, sehingga banyaknya kolom {@code null} pada jalur jurnal tidak
 * membebani pernyataan SQL.</li>
 * </ul>
 *
 * <h3>Catatan keamanan (kontrol akses)</h3>
 * <p><b>Jalur tulis resmi adalah CONTOH POSITIF.</b> Ketiga method
 * {@code JurnalDiscussionService} benar-benar berpagar: {@code create(...)} menuntut hak
 * {@code discussions.create}, memverifikasi bahwa naskah memang berada di koleksi jurnal yang
 * diminta ("Naskah berada di jurnal lain."), lalu memanggil
 * {@code JurnalAuthorizationService#requireJournalScope(...)} yang mensyaratkan penugasan
 * {@code PenugasanTahapJurnal} yang aktif, berstatus {@code ACTIVE}, dan masih dalam rentang
 * tanggal. {@code comment(...)} mensyaratkan keanggotaan {@code PesertaDiskusiJurnal} yang aktif
 * dan belum keluar ({@code leftAt is null}), dengan jalur alternatif yang tetap melewati
 * pemeriksaan cakupan yang sama. Servlet {@code JurnalAdminApi} sendiri menuntut login, memaksa
 * metode {@code POST} dan token CSRF valid untuk semua aksi perubahan. Ini kontras tajam dengan
 * temuan pada {@link Komentar} (komentar bimbingan KRS bisa dihapus mahasiswa tanpa gerbang apa
 * pun): pada {@code Diskusi} <b>tidak ditemukan</b> masalah kontrol akses sejenis di jalur
 * resminya — memang tidak ada aksi hapus/ubah sama sekali yang disediakan.</p>
 * <p><b>Namun jalur BACA-nya bermasalah, dan justru karena jalur resminya tidak ada.</b> Isi
 * diskusi peer-review (termasuk yang ditandai {@code INTERNAL} dan {@code DOUBLE_ANONYMOUS})
 * terjangkau lewat {@code /Api} aksi {@code dataRinci}: pemanggil cukup mengirim {@code class} =
 * {@code "ais.database.model.Diskusi"} (atau {@code DiskusiKomentar}) beserta {@code id} sembarang,
 * dan servlet akan memuat entity itu lalu menyerialisasikan grafnya sedalam <b>6 tingkat</b>
 * secara bawaan. Tidak ada daftar-putih kelas pada jalur itu ({@code Diskusi} memang tidak ada di
 * {@code CLASS_IZINKAN} milik {@code ais.common.DataUtil}, tetapi daftar itu <b>tidak dipakai</b>
 * oleh {@code dataRinci}), tidak ada pemeriksaan kepemilikan, dan tidak ada pemeriksaan
 * keanggotaan {@code PesertaDiskusiJurnal}. Karena {@link DiskusiKomentar#getDiskusi()} memiliki
 * {@code @ManyToOne}, menarik satu id komentar sudah cukup untuk ikut membawa serta metadata
 * utasnya. Ini instance konkret dari masalah IDOR endpoint reflektif yang sudah tercatat
 * sebelumnya, kini dengan sasaran baru berupa <b>korespondensi editorial rahasia dan identitas
 * reviewer</b>.</p>
 *
 * @see DiskusiKomentar
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.jurnal.PesertaDiskusiJurnal
 * @see PertemuanPunyaDiskusi
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "diskusi")

public class Diskusi extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Nilainya <b>sengaja identik</b> dengan milik {@link DiskusiKomentar} (dan sejumlah entity
	 * lain hasil generator yang sama) karena berkas ini disalin dari cetakan {@code hbm2java} yang
	 * sama pada Apr 2010. Kesamaan itu tidak menimbulkan masalah: {@code serialVersionUID} hanya
	 * dibandingkan antar-versi class yang <i>sama</i>, bukan antar-class berbeda.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Kunci primer baris {@code diskusi}, dibangkitkan database ({@code IDENTITY}). Dideklarasikan
	 * ulang di sini karena {@link GeneralValueObject} bukan {@code @MappedSuperclass}. Lihat
	 * {@link #getId()}.
	 */
	private Long id;
	/**
	 * Nama tampil pengguna terakhir yang menyimpan baris ini (kolom audit {@code oleh}). Diisi
	 * otomatis oleh {@link #onUpdate()} — <b>hanya saat UPDATE</b>, tidak saat INSERT. Lihat
	 * {@link #getOleh()}.
	 */
	private String oleh;
	/**
	 * Identitas ({@code userId}) pengguna terkait baris ini (kolom audit {@code oleh_id}). Pada
	 * jalur jurnal inilah <b>satu-satunya</b> tempat identitas pembuat utas disimpan — diisi
	 * eksplisit oleh {@code JurnalDiscussionService#create(...)}. Lihat {@link #getOlehId()}.
	 */
	private String olehId;

	/**
	 * Mengembalikan identitas ({@code userId}) pengguna yang tercatat pada kolom audit
	 * {@code oleh_id}.
	 *
	 * <p><b>Penting pada jalur jurnal:</b> nilai ini bukan sekadar metadata audit, melainkan
	 * <b>penunjuk pembuat utas yang sesungguhnya</b> — {@code JurnalDiscussionService#create(...)}
	 * sengaja menyimpan identitas pembuat di sini alih-alih lewat relasi {@link #getTbmuser()},
	 * dengan alasan yang ditulis eksplisit sebagai komentar di berkas tersebut (menghindari
	 * ketergantungan ORM pada pemetaan {@code Tbmuser} lama yang sangat luas). Nilai yang sama juga
	 * dipakai sebagai {@code userId} peserta pertama ({@code CREATOR}) di
	 * {@code PesertaDiskusiJurnal}.</p>
	 *
	 * <p><b>Hati-hati:</b> nilai ini akan <b>tertimpa</b> identitas pengguna lain begitu baris ini
	 * ter-{@code UPDATE}, karena {@link #onUpdate()} menuliskannya ulang setiap kali. Lihat catatan
	 * di Javadoc class.</p>
	 *
	 * @return {@code userId} pengguna, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi kolom audit {@code oleh_id}.
	 *
	 * <p><b>Efek samping tak terduga:</b> method ini <b>mengabaikan secara diam-diam</b> argumen
	 * {@code null} maupun yang hanya berisi spasi — nilai lama dipertahankan dan pemanggil tidak
	 * menerima sinyal kegagalan apa pun. Konsekuensinya kolom ini tidak pernah bisa dikosongkan
	 * kembali lewat setter.</p>
	 *
	 * <p>Dipanggil dari dua tempat: {@link #onUpdate()} (otomatis, lewat
	 * {@code AuditTimestampInterceptor#ubah(Object)} saat setiap {@code UPDATE}) dan
	 * {@code JurnalDiscussionService#create(...)} / {@code OjsDomainTransformService#discussion(...)}
	 * (eksplisit, saat pembuatan utas).</p>
	 *
	 * @param olehId {@code userId} pengguna; {@code null} atau string kosong/spasi diabaikan.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi kolom audit {@code oleh} (nama tampil pengguna).
	 *
	 * <p><b>Efek samping tak terduga:</b> sama seperti {@link #setOlehId(String)}, argumen
	 * {@code null} atau berisi spasi saja <b>diabaikan diam-diam</b>.</p>
	 *
	 * <p>Pada jalur jurnal method ini <b>tidak pernah dipanggil saat pembuatan</b> — hanya
	 * {@link #onUpdate()} yang memanggilnya, dan kait itu baru aktif pada {@code UPDATE}. Karena
	 * itu utas yang baru dibuat selalu punya {@code oleh} bernilai {@code null}.</p>
	 *
	 * @param oleh nama tampil pengguna; {@code null} atau string kosong/spasi diabaikan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama tampil pengguna terakhir yang menyimpan baris ini (kolom audit
	 * {@code oleh}).
	 *
	 * <p><b>Jangan diperlakukan sebagai "pembuat diskusi".</b> Pada jalur jurnal nilainya
	 * {@code null} untuk utas yang belum pernah di-{@code UPDATE}, dan setelah update pertama
	 * berisi nama pengguna terakhir yang menyimpan — bukan pembuatnya. Untuk pembuat, pakai
	 * {@link #getOlehId()}.</p>
	 *
	 * @return nama tampil pengguna, atau {@code null}.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate} sekaligus deklarasi field {@link #tanggal_dirubah} (keduanya
	 * ditulis pada satu baris fisik oleh generator; format ini dipertahankan apa adanya).
	 *
	 * <p><b>Kait {@code onUpdate()}.</b> Dipanggil <b>oleh Hibernate/JPA sendiri</b>, tepat sebelum
	 * pernyataan {@code UPDATE} atas baris ini dikirim ke database — bukan oleh kode aplikasi. Ia
	 * meneruskan {@code this} ke {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)},
	 * yang mengisi ulang tiga kolom audit sekaligus: {@link #setTanggal_dirubah(Date)} (waktu
	 * sekarang), {@link #setOleh(String)} dan {@link #setOlehId(String)} (identitas pengguna yang
	 * sedang aktif pada thread tersebut).</p>
	 *
	 * <p><b>Tidak ada padanan {@code @PrePersist}</b> di class ini — inilah sebabnya {@code oleh}
	 * kosong pada baris yang baru dibuat, dan sebabnya {@link #getOlehId()} pada utas baru hanya
	 * terisi karena {@code JurnalDiscussionService} mengisinya secara manual.</p>
	 *
	 * <p><b>Field {@code tanggal_dirubah}.</b> Menyimpan waktu penyimpanan terakhir baris ini
	 * (kolom {@code tanggal_dirubah}, tipe {@code TIMESTAMP}). Diinisialisasi saat object dibentuk
	 * dengan {@code ais.ui.util.WaktuUtil.getDate()} — yaitu waktu server yang sudah disesuaikan
	 * konfigurasi aplikasi, bukan {@code new Date()} langsung — sehingga baris yang belum pernah
	 * di-update pun tetap punya nilai yang masuk akal.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi waktu penyimpanan terakhir baris ini (kolom audit {@code tanggal_dirubah}).
	 *
	 * <p>Normalnya <b>tidak dipanggil kode aplikasi</b>; pengisiannya diserahkan kepada
	 * {@link #onUpdate()} lewat {@code AuditTimestampInterceptor}. Berbeda dengan
	 * {@link #setOleh(String)}, method ini <b>tidak</b> menyaring {@code null} — mengirim
	 * {@code null} benar-benar mengosongkan kolomnya.</p>
	 *
	 * @param tanggal_dirubah waktu penyimpanan terakhir; boleh {@code null}.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu penyimpanan terakhir baris ini (kolom {@code tanggal_dirubah}).
	 *
	 * <p>Object {@link Date} yang dikembalikan adalah <b>referensi langsung</b> ke field (tidak
	 * disalin), jadi pemanggil yang memodifikasinya ikut mengubah keadaan entity.</p>
	 *
	 * @return waktu penyimpanan terakhir; untuk baris yang belum pernah di-{@code UPDATE} berisi
	 *         waktu object ini dibentuk di memori, bukan waktu {@code INSERT}-nya.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Mengembalikan judul utas sebagai representasi teks object ini.
	 *
	 * <p><b>Kuirk:</b> membaca field {@link #nama} <b>mentah</b>, bukan lewat {@link #getNama()},
	 * sehingga hasilnya <b>tidak di-{@code trim}</b> dan <b>bisa bernilai {@code null}</b> untuk
	 * object yang belum diisi/disimpan. Method ini melanggar konvensi umum {@code toString()} yang
	 * seharusnya selalu mengembalikan string; berhati-hatilah bila memakainya sebagai label
	 * komponen UI atau kunci {@code Map}.</p>
	 *
	 * @return judul utas apa adanya, atau {@code null}.
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Judul utas diskusi (kolom {@code nama}, {@code NOT NULL}, maks 255 karakter). Lihat
	 * {@link #getNama()}. Pada baris hasil import OJS berisi penanda buatan berbentuk
	 * {@code "OJS query <sourceId>:<queryId>"} yang juga dipakai sebagai kunci idempotensi import.
	 */
	private String nama;
	/**
	 * Deskripsi/pesan pembuka utas (kolom {@code keterangan}, maks 1000 karakter, boleh
	 * {@code null}). Lihat {@link #getKeterangan()}.
	 */
	private String keterangan;
	/**
	 * Waktu utas dibuka (kolom {@code tanggal}). Diinisialisasi saat object dibentuk dengan
	 * {@code ais.ui.util.WaktuUtil.getDate()}, lalu ditimpa nilai eksplisit oleh penulisnya. Lihat
	 * {@link #getTanggal()}.
	 */
	private Date tanggal = ais.ui.util.WaktuUtil.getDate();
	/**
	 * Relasi opsional ke mahasiswa — <b>DORMAN pada jalur jurnal</b>, tidak pernah diisi. Warisan
	 * cetakan generator. Lihat {@link #getMahasiswa()}.
	 */
	private Mahasiswa mahasiswa;
	/**
	 * Relasi opsional ke dosen — <b>DORMAN pada jalur jurnal</b>, tidak pernah diisi. Warisan
	 * cetakan generator. Lihat {@link #getDosen()}.
	 */
	private Dosen dosen;
	/**
	 * Relasi opsional ke akun pengguna — <b>DORMAN pada jalur jurnal</b>, sengaja tidak dipakai
	 * (lihat komentar di {@code JurnalDiscussionService#create(...)}). Identitas pembuat disimpan
	 * skalar di {@link #olehId}. Lihat {@link #getTbmuser()}.
	 */
	private Tbmuser tbmuser;

	/**
	 * Relasi opsional ke jurusan — <b>DORMAN pada jalur jurnal</b>, tidak pernah diisi. Warisan
	 * cetakan generator. Lihat {@link #getJurusan()}.
	 */
	private Jurusan jurusan;
	/**
	 * Relasi opsional ke fakultas — <b>DORMAN pada jalur jurnal</b>, tidak pernah diisi. Warisan
	 * cetakan generator. Lihat {@link #getFakultas()}.
	 */
	private Fakultas fakultas;
	/**
	 * Penunjuk skalar ke jurnal pemilik utas (kolom {@code jurnal_penelitian_id}, <b>tanpa</b>
	 * foreign key). Lihat {@link #getJurnalPenelitianId()}.
	 */
	private Long jurnalPenelitianId;
	/**
	 * Penunjuk skalar ke naskah yang didiskusikan (kolom {@code repo_item_id}, <b>tanpa</b> foreign
	 * key). Lihat {@link #getRepoItemId()}.
	 */
	private Long repoItemId;
	/**
	 * Tahap alur kerja jurnal tempat utas ini berada (kolom {@code stage_key}). Lihat
	 * {@link #getStageKey()} — termasuk catatan ketidakkonsistenan kosakata dengan importer OJS.
	 */
	private String stageKey;
	/**
	 * Kebijakan siapa yang boleh melihat utas (kolom {@code visibility}). <b>Tidak pernah dibaca
	 * kode mana pun.</b> Lihat {@link #getVisibility()}.
	 */
	private String visibility;
	/**
	 * Kebijakan keanoniman peserta utas (kolom {@code anonymity_mode}). <b>Tidak pernah dibaca kode
	 * mana pun.</b> Lihat {@link #getAnonymityMode()}.
	 */
	private String anonymityMode;

	/**
	 * Konstruktor tanpa argumen.
	 *
	 * <p>Wajib ada agar Hibernate dapat meng-instansiasi entity saat membaca baris dari database.
	 * Juga dipakai langsung oleh {@code JurnalDiscussionService#create(...)} dan
	 * {@code OjsDomainTransformService#discussion(...)} — dua-duanya satu-satunya tempat
	 * {@code new Diskusi()} muncul di seluruh <i>source tree</i>.</p>
	 *
	 * <p>Perhatikan bahwa konstruktor ini <b>tidak kosong secara efektif</b>: inisialisasi field
	 * {@link #tanggal} dan {@link #tanggal_dirubah} ke waktu sekarang ikut dijalankan di sini.</p>
	 */
	public Diskusi() {
	}

	/**
	 * Mengembalikan kunci primer baris ini.
	 *
	 * <p>Kolom {@code id} bertipe {@code IDENTITY} dan ditandai {@code insertable = false} —
	 * nilainya <b>dibangkitkan database</b>, jadi mengisi {@link #setId(Long)} sebelum
	 * {@code INSERT} tidak berpengaruh. Nilai baru baru terlihat setelah {@code session.save(...)}
	 * (dan pada jalur jurnal, {@code session.flush()} yang menyusulnya).</p>
	 *
	 * <p>Anotasi {@code @Id} yang menempel pada <i>getter</i> ini menetapkan seluruh pemetaan
	 * entity memakai <b>property access</b> — artinya Hibernate membaca setiap nilai lewat
	 * getter-nya, termasuk kelima getter relasi yang memanggil {@code check(...)}.</p>
	 *
	 * @return kunci primer, atau {@code null} untuk object yang belum disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci primer baris ini.
	 *
	 * <p>Praktis tidak pernah dipanggil kode aplikasi: kolomnya {@code insertable = false} dan
	 * dibangkitkan database, sehingga method ini hanya dipakai Hibernate saat memuat baris.</p>
	 *
	 * @param id kunci primer.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan judul utas diskusi, sudah di-{@code trim}.
	 *
	 * <p>Kolom {@code nama} bersifat {@code NOT NULL} dan maksimal 255 karakter;
	 * {@code JurnalDiscussionService#create(...)} menolak judul kosong ("Judul diskusi wajib
	 * diisi.") sebelum menyimpan. Getter ini mengembalikan {@code null} bila field-nya
	 * {@code null} (object belum disimpan), jika tidak selalu versi yang sudah dipangkas
	 * spasinya.</p>
	 *
	 * <p><b>Perhatikan:</b> {@link #toString()} <b>tidak</b> memakai getter ini melainkan membaca
	 * field mentah, sehingga hasil keduanya bisa berbeda untuk judul berspasi di ujung.</p>
	 *
	 * <p><b>Peran khusus pada import OJS:</b> {@code OjsDomainTransformService} memakai nilai kolom
	 * ini sebagai kunci idempotensi — ia mencari {@code from Diskusi where jurnalPenelitianId=:j
	 * and nama=:n} dengan {@code n} berupa penanda {@code "OJS query <sourceId>:<queryId>"} untuk
	 * memastikan satu baris OJS tidak terimpor dua kali. Mengganti judul baris hasil import lewat
	 * jalur lain akan <b>merusak idempotensi itu</b> dan menyebabkan duplikasi pada import ulang.</p>
	 *
	 * @return judul utas tanpa spasi di ujung, atau {@code null}.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi judul utas diskusi.
	 *
	 * <p>Tidak melakukan validasi maupun pemangkasan apa pun — {@code trim} dan pembatasan panjang
	 * 255 karakter dilakukan pemanggil ({@code JurnalDiscussionService} dan
	 * {@code OjsDomainTransformService} masing-masing punya helper {@code clean}/{@code limit}
	 * sendiri). Mengisi nilai lebih panjang dari 255 karakter lewat jalur lain akan gagal di level
	 * database.</p>
	 *
	 * @param nama judul utas.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan deskripsi/pesan pembuka utas.
	 *
	 * <p>Dikembalikan <b>apa adanya</b> (tanpa {@code trim}, boleh {@code null}) — berbeda dari
	 * {@link #getNama()}. Kolomnya {@code varchar(1000)}; {@code JurnalDiscussionService#create(...)}
	 * memotong argumen yang lebih panjang menjadi tepat 1000 karakter sebelum menyimpan. Bandingkan
	 * dengan {@link DiskusiKomentar#getKeterangan()} yang bertipe {@code text} dan menampung
	 * hingga 262.144 karakter untuk badan komentar.</p>
	 *
	 * <p>Pada baris hasil import OJS, kolom ini berisi kalimat provenance tetap yang menjelaskan
	 * asal-usul baris, bukan isi diskusi sebenarnya.</p>
	 *
	 * @return deskripsi pembuka utas, atau {@code null}.
	 */
	@Column(name = "keterangan", length = 1000, nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi deskripsi/pesan pembuka utas.
	 *
	 * <p>Tanpa validasi panjang; pemotongan ke 1000 karakter dilakukan pemanggil.</p>
	 *
	 * @param keterangan deskripsi pembuka; boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengisi relasi ke dosen.
	 *
	 * <p><b>Tidak pernah dipanggil</b> pada jalur jurnal — field ini dorman. Lihat
	 * {@link #getDosen()}.</p>
	 *
	 * @param dosen dosen terkait; boleh {@code null}.
	 */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/**
	 * Mengembalikan relasi opsional ke dosen, setelah meresolusi proxy lazy-nya.
	 *
	 * <p><b>Efek samping (disengaja, pola berulang seluruh repo — diverifikasi langsung pada
	 * berkas ini):</b> hasil {@link ais.database.model.GeneralValueObject#check(Object)}
	 * <b>ditugaskan kembali ke field {@link #dosen}</b> sebelum dikembalikan. Ini yang membuat
	 * object hasil resolusi dipakai ulang pada pemanggilan berikutnya. Method ini <b>tidak</b>
	 * mengosongkan field dan <b>tidak</b> menulis ke database; namun {@code check(...)} sendiri
	 * bisa membuka session Hibernate baru sebagai penyelamat terakhir bila proxy sudah
	 * <i>detached</i>, lalu menutupnya kembali — hindari memanggilnya di dalam perulangan besar.</p>
	 *
	 * <p><b>Status pada jalur jurnal: DORMAN.</b> Baik {@code JurnalDiscussionService} maupun
	 * importer OJS tidak pernah mengisi relasi ini, jadi nilainya selalu {@code null} untuk baris
	 * diskusi jurnal. Kolomnya tetap terpetakan Hibernate dan tetap ikut ditelusuri endpoint
	 * reflektif {@code /Api dataRinci}.</p>
	 *
	 * @return dosen terkait, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen", nullable = true)
	public Dosen getDosen() {
		dosen = check(dosen);
		return dosen;
	}

	/**
	 * Mengisi relasi ke mahasiswa.
	 *
	 * <p><b>Tidak pernah dipanggil</b> pada jalur jurnal — field ini dorman. Lihat
	 * {@link #getMahasiswa()}.</p>
	 *
	 * @param mahasiswa mahasiswa terkait; boleh {@code null}.
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Mengembalikan relasi opsional ke mahasiswa, setelah meresolusi proxy lazy-nya.
	 *
	 * <p><b>Efek samping:</b> sama dengan {@link #getDosen()} — hasil {@code check(...)} ditulis
	 * balik ke field {@link #mahasiswa}. Tidak destruktif, tidak menyentuh database secara
	 * langsung.</p>
	 *
	 * <p><b>Status pada jalur jurnal: DORMAN</b>, selalu {@code null}. Perhatikan bahwa
	 * ketidak-adaan relasi ini <b>bukan</b> berarti mahasiswa tidak bisa terlibat: peserta diskusi
	 * dicatat sebagai {@code userId} teks di {@code PesertaDiskusiJurnal}, bukan lewat relasi
	 * entity.</p>
	 *
	 * @return mahasiswa terkait, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * Mengisi relasi ke jurusan.
	 *
	 * <p><b>Tidak pernah dipanggil</b> pada jalur jurnal — field ini dorman.</p>
	 *
	 * @param jurusan jurusan terkait; boleh {@code null}.
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan relasi opsional ke jurusan, setelah meresolusi proxy lazy-nya.
	 *
	 * <p><b>Efek samping:</b> hasil {@code check(...)} ditulis balik ke field {@link #jurusan};
	 * lihat penjelasan lengkap di {@link #getDosen()}.</p>
	 *
	 * <p><b>Status pada jalur jurnal: DORMAN</b>, selalu {@code null}. Cakupan utas jurnal
	 * ditentukan {@link #getJurnalPenelitianId()}, bukan oleh struktur fakultas/jurusan.</p>
	 *
	 * @return jurusan terkait, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Mengisi relasi ke fakultas.
	 *
	 * <p><b>Tidak pernah dipanggil</b> pada jalur jurnal — field ini dorman.</p>
	 *
	 * @param fakultas fakultas terkait; boleh {@code null}.
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Mengembalikan relasi opsional ke fakultas, setelah meresolusi proxy lazy-nya.
	 *
	 * <p><b>Efek samping:</b> hasil {@code check(...)} ditulis balik ke field {@link #fakultas};
	 * lihat penjelasan lengkap di {@link #getDosen()}.</p>
	 *
	 * <p><b>Status pada jalur jurnal: DORMAN</b>, selalu {@code null}.</p>
	 *
	 * @return fakultas terkait, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * Mengisi relasi ke akun pengguna.
	 *
	 * <p><b>Sengaja tidak dipanggil</b> pada jalur jurnal. {@code JurnalDiscussionService#create(...)}
	 * mencantumkan alasannya sebagai komentar di kode: identitas pelaku disimpan skalar lewat
	 * {@link #setOlehId(String)} untuk menghindari ketergantungan ORM yang rapuh pada pemetaan
	 * {@code Tbmuser} lama yang sangat luas.</p>
	 *
	 * @param tbmuser akun pengguna terkait; boleh {@code null}.
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * Mengembalikan relasi opsional ke akun pengguna, setelah meresolusi proxy lazy-nya.
	 *
	 * <p><b>Efek samping:</b> hasil {@code check(...)} ditulis balik ke field {@link #tbmuser};
	 * lihat penjelasan lengkap di {@link #getDosen()}.</p>
	 *
	 * <p><b>PENTING — berbeda dari {@link Komentar#getTbmuser()}.</b> Pada {@code Komentar}, getter
	 * senama bersifat <b>destruktif</b> (mengosongkan field menjadi {@code null} sehingga kolomnya
	 * ikut terhapus permanen di database). Verifikasi langsung atas berkas ini memastikan
	 * <b>tidak ada perilaku seperti itu di sini</b>: getter hanya meresolusi proxy dan
	 * mengembalikannya.</p>
	 *
	 * <p><b>Status pada jalur jurnal: DORMAN</b>, selalu {@code null} — identitas pembuat ada di
	 * {@link #getOlehId()}.</p>
	 *
	 * @return akun pengguna terkait, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		tbmuser = check(tbmuser);
		return tbmuser;
	}

	/**
	 * Mengisi waktu utas dibuka.
	 *
	 * <p>Selalu dipanggil eksplisit oleh kedua penulis, menimpa nilai bawaan yang sudah diisi saat
	 * object dibentuk: {@code JurnalDiscussionService#create(...)} mengisinya {@code new Date()},
	 * sedangkan importer OJS memakai kolom {@code date_posted} dari basis data sumber (dengan
	 * {@code new Date()} sebagai cadangan bila tanggal sumber tidak bisa diurai).</p>
	 *
	 * @param tanggal waktu utas dibuka; boleh {@code null} (kolomnya {@code nullable}).
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengembalikan waktu utas dibuka (kolom {@code tanggal}).
	 *
	 * <p>Berbeda dari {@link #getTanggal_dirubah()} yang murni metadata audit, kolom ini adalah
	 * <b>data bisnis</b>: pada baris hasil import OJS ia membawa tanggal asli percakapan di sistem
	 * lama, sehingga nilainya bisa jauh lebih tua dari waktu barisnya diciptakan di AIS.</p>
	 *
	 * <p>Object {@link Date} yang dikembalikan adalah <b>referensi langsung</b> ke field (tidak
	 * disalin).</p>
	 *
	 * @return waktu utas dibuka; untuk object baru yang belum diisi berisi waktu object dibentuk.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal() {
		return tanggal;
	}

	/**
	 * Mengembalikan id jurnal pemilik utas ini (kolom {@code jurnal_penelitian_id}).
	 *
	 * <p><b>Bukan relasi:</b> kolom {@code bigint} biasa tanpa {@code @ManyToOne}/{@code @JoinColumn},
	 * jadi tanpa <i>foreign key</i> maupun jaminan integritas ke
	 * {@code ais.database.model.penelitiandanpengabdian.JurnalPenelitian}. Nilainya dipakai untuk
	 * dua hal:</p>
	 * <ul>
	 * <li><b>Gerbang keabsahan:</b> {@code JurnalDiscussionService} menolak utas yang kolom ini
	 * (atau {@link #getRepoItemId()}) bernilai {@code null} dengan pesan "Diskusi jurnal tidak
	 * ditemukan." — baris {@code Diskusi} generik/lama karenanya otomatis tak terjangkau jalur
	 * jurnal.</li>
	 * <li><b>Cakupan hak akses:</b> diteruskan ke
	 * {@code JurnalAuthorizationService#requireJournalScope(...)} yang mensyaratkan penugasan
	 * {@code PenugasanTahapJurnal} aktif pada jurnal tersebut.</li>
	 * </ul>
	 * <p>Karena tidak ada kolom tenant terpisah pada entity ini, id inilah satu-satunya penanda
	 * isolasi antar-jurnal (dan efektifnya antar-tenant).</p>
	 *
	 * @return id jurnal pemilik, atau {@code null} untuk baris non-jurnal.
	 */
	@Column(name="jurnal_penelitian_id") public Long getJurnalPenelitianId(){return jurnalPenelitianId;}
	/**
	 * Mengisi id jurnal pemilik utas.
	 *
	 * <p>Tanpa validasi keberadaan jurnal — pemanggil ({@code JurnalDiscussionService#create(...)})
	 * sudah memuat dan memverifikasi {@code JurnalPenelitian} lebih dulu, termasuk memastikan
	 * naskahnya berada di koleksi jurnal yang sama ("Naskah berada di jurnal lain.").</p>
	 *
	 * @param v id jurnal pemilik.
	 */
	public void setJurnalPenelitianId(Long v){jurnalPenelitianId=v;}
	/**
	 * Mengembalikan id naskah ({@code RepoItem}) yang didiskusikan (kolom {@code repo_item_id}).
	 *
	 * <p><b>Bukan relasi</b> — kolom {@code bigint} biasa tanpa <i>foreign key</i> ke
	 * {@code ais.database.model.repository.RepoItem}. Sama seperti {@link #getJurnalPenelitianId()},
	 * nilai {@code null} membuat utas dianggap bukan diskusi jurnal dan ditolak service.</p>
	 *
	 * <p><b>Kuirk pada import OJS:</b> {@code OjsDomainTransformService} boleh mengisinya
	 * {@code null} bila baris {@code queries} sumber tidak terkait naskah mana pun — sehingga
	 * import bisa menghasilkan baris {@code Diskusi} yang <b>tidak akan pernah</b> bisa diakses
	 * lewat {@code JurnalDiscussionService} (gerbang {@code repoItemId != null} langsung
	 * menolaknya). Dicatat apa adanya.</p>
	 *
	 * @return id naskah yang didiskusikan, atau {@code null}.
	 */
	@Column(name="repo_item_id") public Long getRepoItemId(){return repoItemId;}
	/**
	 * Mengisi id naskah yang didiskusikan.
	 *
	 * <p>Tanpa validasi; pemanggil sudah memastikan naskah ada dan berada di koleksi jurnal yang
	 * benar sebelum memanggil method ini.</p>
	 *
	 * @param v id {@code RepoItem}; boleh {@code null} (lihat kuirk di {@link #getRepoItemId()}).
	 */
	public void setRepoItemId(Long v){repoItemId=v;}
	/**
	 * Mengembalikan tahap alur kerja jurnal tempat utas ini berada (kolom {@code stage_key}, maks
	 * 80 karakter).
	 *
	 * <p><b>Dipakai sebagai bagian gerbang hak akses:</b> nilainya diteruskan ke
	 * {@code JurnalAuthorizationService#requireJournalScope(...)}, yang menambahkan predikat
	 * {@code (stageKey='ALL' or stageKey='JOURNAL' or stageKey=:stage)} pada pencarian penugasan
	 * aktif pengguna. Artinya seorang pengguna hanya boleh menyentuh utas pada tahap yang memang
	 * ditugaskan kepadanya.</p>
	 *
	 * <p><b>KUIRK — dua kosakata yang tidak kompatibel.</b> Jalur resmi
	 * ({@code JurnalDiscussionService}) hanya menerima lima nilai: {@code SUBMISSION},
	 * {@code REVIEW}, {@code COPYEDITING}, {@code PRODUCTION}, {@code PROOF} (di-{@code
	 * uppercase}, selain itu dilempar "Tahap diskusi tidak valid."). Importer OJS <b>melewati
	 * validasi itu</b> dan menulis nilai mentah kolom {@code stage_id} milik OJS yang berupa
	 * <b>angka</b> ("1".."5"), dengan bawaan {@code "EDITORIAL"} — tidak satu pun ada di daftar
	 * valid. Baris hasil import karenanya berpotensi tidak pernah cocok dengan predikat tahap di
	 * atas, sehingga hanya administrator (yang di-<i>bypass</i> lebih awal oleh
	 * {@code requireJournalScope}) yang dapat menyentuhnya. Bug ini dicatat, tidak
	 * diperbaiki.</p>
	 *
	 * @return kunci tahap alur kerja, atau {@code null}.
	 */
	@Column(name="stage_key",length=80) public String getStageKey(){return stageKey;}
	/**
	 * Mengisi tahap alur kerja jurnal.
	 *
	 * <p><b>Tidak memvalidasi apa pun.</b> Validasi kosakata sepenuhnya ada di pemanggil
	 * ({@code JurnalDiscussionService#validStage(...)}) — dan importer OJS memang tidak
	 * memakainya, lihat kuirk di {@link #getStageKey()}.</p>
	 *
	 * @param v kunci tahap; idealnya salah satu dari {@code SUBMISSION}/{@code REVIEW}/
	 *          {@code COPYEDITING}/{@code PRODUCTION}/{@code PROOF}.
	 */
	public void setStageKey(String v){stageKey=v;}
	/**
	 * Mengembalikan kebijakan keterbukaan utas (kolom {@code visibility}, maks 40 karakter).
	 *
	 * <p><b>PERINGATAN — KEBIJAKAN INI TIDAK PERNAH DITEGAKKAN.</b> Penelusuran seluruh
	 * <i>source tree</i> memastikan method ini <b>tidak dipanggil dari mana pun</b>. Nilainya
	 * divalidasi saat ditulis lalu mengendap di database tanpa pernah dibaca lagi.
	 * {@code JurnalDiscussionService#comment(...)} — satu-satunya tempat keputusan "boleh
	 * berpartisipasi atau tidak" diambil — menentukannya semata-mata dari keanggotaan aktif di
	 * {@code PesertaDiskusiJurnal} (atau hak {@code prosesReview.update} plus cakupan penugasan),
	 * <b>tanpa</b> melihat kolom ini. Karena itu pembedaan {@code INTERNAL} / {@code REVIEWERS} /
	 * {@code AUTHOR_EDITOR} / {@code ALL_PARTICIPANTS} saat ini murni dekoratif. Jangan
	 * mengandalkannya sebagai kontrol akses.</p>
	 *
	 * <p><b>Kuirk tambahan:</b> importer OJS menulis konstanta {@code "PARTICIPANTS"} yang bahkan
	 * tidak ada di daftar nilai valid jalur resmi.</p>
	 *
	 * @return kunci kebijakan keterbukaan, atau {@code null}.
	 */
	@Column(name="visibility",length=40) public String getVisibility(){return visibility;}
	/**
	 * Mengisi kebijakan keterbukaan utas.
	 *
	 * <p>Tidak memvalidasi; {@code JurnalDiscussionService#validVisibility(...)} yang membatasi
	 * nilainya pada jalur resmi. Ingat bahwa nilai yang tersimpan <b>tidak berpengaruh apa pun</b>
	 * terhadap perilaku sistem — lihat {@link #getVisibility()}.</p>
	 *
	 * @param v kunci kebijakan keterbukaan.
	 */
	public void setVisibility(String v){visibility=v;}
	/**
	 * Mengembalikan mode keanoniman utas (kolom {@code anonymity_mode}, maks 30 karakter).
	 *
	 * <p><b>PERINGATAN — SAMA SEPERTI {@link #getVisibility()}, TIDAK PERNAH DIBACA KODE MANA
	 * PUN.</b> Nilai {@code DOUBLE_ANONYMOUS} pada baris ini <b>tidak menyembunyikan identitas
	 * siapa pun</b>: {@link #getOlehId()} pada utas dan pada setiap {@link DiskusiKomentar} berisi
	 * {@code userId} asli penulisnya tanpa lapisan penyamaran, dan daftar peserta di
	 * {@code PesertaDiskusiJurnal} menyimpan {@code userId} apa adanya pula. Untuk sistem
	 * peer-review, ini jebakan serius: kolom yang tampak menjanjikan anonimitas ganda sebenarnya
	 * hanya label.</p>
	 *
	 * <p>Bandingkan dengan {@code ais.database.model.jurnal.PenugasanReviewerJurnal#getAnonymityMode()}
	 * yang membawa kolom senama untuk penugasan reviewer — entity yang berbeda, kolom yang berbeda,
	 * dan tidak saling menyinkronkan.</p>
	 *
	 * @return kunci mode keanoniman, atau {@code null}.
	 */
	@Column(name="anonymity_mode",length=30) public String getAnonymityMode(){return anonymityMode;}
	/**
	 * Mengisi mode keanoniman utas.
	 *
	 * <p>Tidak memvalidasi; {@code JurnalDiscussionService#validAnonymity(...)} yang membatasi
	 * nilainya pada jalur resmi menjadi {@code DOUBLE_ANONYMOUS}/{@code SINGLE_ANONYMOUS}/
	 * {@code OPEN}. Ingat bahwa nilai yang tersimpan <b>tidak berpengaruh apa pun</b> terhadap
	 * perilaku sistem — lihat {@link #getAnonymityMode()}.</p>
	 *
	 * @param v kunci mode keanoniman.
	 */
	public void setAnonymityMode(String v){anonymityMode=v;}

}
