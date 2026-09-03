package ais.database.model.sekolah;
// Generated 30 Sep 18 6:57:43 by Hibernate Tools 5.2.3.Final

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

import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;

/**
 * Pemetaan tetap <b>"siswa ini pembinanya siapa"</b> &mdash; satu baris menautkan seorang
 * {@link Siswa} ke satu akun pengguna {@link Tbmuser} yang berperan sebagai <i>pembina</i>-nya,
 * ditambah {@link #getKeterangan() keterangan} bebas dan saklar {@link #getAktif() aktif}.
 *
 * <p>Tabel: {@code sekolah.pembina_siswa} (lihat {@code @Table} di bawah; kelas terdaftar resmi
 * di {@code hibernate.cfg.xml} baris {@code <mapping class="...PembinaSiswa">}, jadi tabelnya
 * benar-benar dibuat/dikelola Hibernate pada instalasi baru).</p>
 *
 * <h2>Domain TERVERIFIKASI: pembina KEGIATAN KESISWAAN, bukan wali kelas/BK</h2>
 *
 * <p>Verifikasi dilakukan dari kode, bukan dari nama kelas. Tiga sumber independen menunjuk
 * domain yang sama, yaitu <b>kegiatan/aktifitas kesiswaan</b> (ekstrakurikuler, organisasi,
 * lomba &mdash; entity {@link KegiatanSiswa} yang dikelompokkan
 * {@link KelompokKegiatanSiswa}):</p>
 * <ol>
 *   <li><b>Letak layarnya.</b> {@code /pages/master/sekolah/pembina_siswa.zul}
 *       ({@code ais.action.master.sekolah.PembinaSiswaAction}) TIDAK punya entri menu sendiri
 *       sama sekali. Satu-satunya rujukannya di seluruh repo adalah tab <b>"Pembina"</b> di
 *       dalam {@code kegiatan_siswa.zul} &mdash; layar "Kegiatan Siswa".</li>
 *   <li><b>Satu-satunya pembaca datanya.</b> {@link KegiatanSiswa#getPembina1()} dan
 *       {@code KegiatanSiswaAction} memakai tabel ini untuk <i>mengisi otomatis</i> kolom
 *       "Pembina 1" pada formulir kegiatan siswa begitu siswanya dipilih &mdash; lihat
 *       "Alur pemakaian" di bawah.</li>
 *   <li><b>Jalur ketiga menuju layar yang sama.</b> {@code SiswaAction.onKegiatanKesiswaan()}
 *       menyisipkan {@code kegiatan_siswa.zul} sebagai tab <b>"Kegiatan Kesiswaan"</b> pada
 *       biodata siswa.</li>
 * </ol>
 * <p>Tidak ada satu pun rujukan dari modul wali kelas, bimbingan konseling, asrama, ataupun
 * pembimbing skripsi/PKL &mdash; dugaan "pembina = wali kelas/BK" TIDAK terbukti. Perlu dicatat
 * juga bahwa kolom {@code pembina} menunjuk {@link Tbmuser} (akun pengguna), <b>bukan</b>
 * {@code Guru} maupun {@code Pegawai}: pemilihnya
 * ({@code ais.action.master.helper.AmbilDataTbmuserBanbox}) menampilkan semua akun aktif
 * KECUALI role Mahasiswa, orang tua, dan penyedia &mdash; jadi seorang pembina secara teknis
 * boleh saja akun admin/pegawai/dosen, tidak wajib guru.</p>
 *
 * <h2>Alur pemakaian (satu penulis, satu pembaca)</h2>
 *
 * <pre>
 * PembinaSiswaAction ("Pembina", tab di layar Kegiatan Siswa)   &lt;-- SATU-SATUNYA PENULIS
 *        |  simpan/ubah/hapus/centang Aktif
 *        v
 * sekolah.pembina_siswa  (KELAS INI: siswa_id -&gt; pembina[Tbmuser])
 *        |
 *        |  InitData.initClasses(..., PembinaSiswa.class, ...) --&gt; preload ke cache memori JVM
 *        v
 * ConstantValues.ambilBerdasarClass(PembinaSiswa.class)   (Map&lt;Long,PembinaSiswa&gt;, CACHE-ONLY)
 *        |
 *        +--&gt; KegiatanSiswaAction  : saat siswa dipilih di formulir kegiatan, isi &amp; KUNCI
 *        |                           bandbox "Pembina 1" dengan pembina siswa tsb.
 *        +--&gt; KegiatanSiswa.getPembina1() : bila kolom pembina1 masih null, isi dari sini
 *                                           (getter menulis balik ke field -- lihat kuirk 3)
 * </pre>
 *
 * <p>Di luar dua titik itu, tabel ini <b>tidak dibaca siapa pun</b>: nol laporan, nol REST API,
 * nol native SQL, nol dasbor. Tidak ada pula data awal/auto-seed &mdash; pada instalasi baru
 * tabel ini kosong dan seluruh isinya diketik manual lewat tab "Pembina".</p>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ul>
 *   <li><b>Jejak audit (wajib dideklarasikan ulang, lihat catatan warisan di bawah):</b>
 *       {@link #getId()}/{@link #setId(Long)}, {@link #getOleh()}/{@link #setOleh(String)},
 *       {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan callback
 *       {@code onUpdate()}.</li>
 *   <li><b>Relasi (inti entity):</b> {@link #getSiswa()}/{@link #setSiswa(Siswa)} dan
 *       {@link #getPembina()}/{@link #setPembina(Tbmuser)} &mdash; keduanya {@code ManyToOne}
 *       LAZY dengan {@code nullable = false}.</li>
 *   <li><b>Atribut bebas:</b> {@link #getKeterangan()}/{@link #setKeterangan(String)}.</li>
 *   <li><b>Saklar:</b> {@link #getAktif()}/{@link #setAktif(Boolean)}.</li>
 *   <li><b>Lain-lain:</b> konstruktor kosong {@link #PembinaSiswa()} dan
 *       {@link #serialVersionUID}.</li>
 * </ul>
 * <p>Berbeda dari kebanyakan kerabatnya, kelas ini <b>tidak</b> punya {@code toString()},
 * {@code equals()}/{@code hashCode()}, {@code compareTo()}, maupun konstruktor berparameter.</p>
 *
 * <h2>Kuirk &amp; temuan (verifikasi dari kode, bukan dugaan)</h2>
 *
 * <ol>
 *   <li><b>Tidak ada kolom tenant sama sekali.</b> Entity ini tidak punya {@code sekolah}
 *       maupun {@code yayasan}; cakupan tenant HANYA bisa disimpulkan lewat
 *       {@code siswa.yayasan}. Konsekuensinya {@code PembinaSiswaAction.initCriteria()}
 *       menyaring per-<b>yayasan</b> saja dan TIDAK PERNAH per-sekolah &mdash; pengguna satu
 *       sekolah melihat (dan, dengan hak UPDATE/DELETE, dapat mengubah) pemetaan pembina
 *       seluruh sekolah lain di yayasan yang sama.</li>
 *   <li><b>Tidak ada unique constraint atas {@code siswa_id}.</b> Satu siswa boleh punya
 *       banyak baris pembina, dan kedua pembacanya menelusuri {@code Map.values()} TANPA
 *       {@code break}: yang menang adalah baris <i>terakhir</i> menurut urutan iterasi
 *       {@code ConcurrentHashMap} &mdash; artinya tidak deterministik dan bisa berubah antar
 *       restart. Tidak ada layar yang memperingatkan duplikasi.</li>
 *   <li><b>Saklar {@link #getAktif() aktif} DIABAIKAN oleh satu-satunya pemakainya.</b>
 *       {@code PembinaSiswaAction} menyaring {@code aktif} di layar daftar (default hanya
 *       menampilkan yang aktif), tetapi baik {@link KegiatanSiswa#getPembina1()} maupun
 *       {@code KegiatanSiswaAction} <b>tidak pernah memeriksa</b> {@code aktif} saat menyisir
 *       cache. Menonaktifkan sebuah baris hanya menyembunyikannya dari layar master; pembina
 *       yang sudah "dimatikan" tetap otomatis terpasang (dan mengunci bandbox) pada setiap
 *       kegiatan siswa baru. Satu-satunya cara benar-benar melepasnya adalah menghapus
 *       barisnya.</li>
 *   <li><b>Fitur isi-otomatis mati senyap pada instalasi besar.</b>
 *       {@code ConstantValues.ambilBerdasarClass(...)} membaca cache memori JVM
 *       ({@code MemoryCacheUtil}) yang <b>tidak punya fallback ke database</b>. Cache itu
 *       hanya terisi penuh saat bootstrap bila jumlah baris tabel di bawah ambang
 *       {@code preload_maks_baris_kecil} (default <b>100</b>) atau bila ada riwayat akses
 *       {@code EntityAccessCache}; di atas ambang itu {@code InitDataHelper} mencetak
 *       "terlalu banyak, skip init memory" dan meninggalkan map kosong, sehingga
 *       {@code ambilBerdasarClass} mengembalikan {@code Collections.EMPTY_MAP}. Akibatnya:
 *       begitu sebuah instalasi melewati ~100 baris pembina, isi-otomatis "Pembina 1"
 *       berhenti bekerja tanpa pesan error dan formulir jatuh ke perilaku cadangan (mengisi
 *       pembina dengan <i>pengguna yang sedang login</i>). Baris yang disimpan/diubah setelah
 *       restart tetap masuk cache satu per satu lewat {@code AuditListener}, sehingga gejalanya
 *       terasa acak: "kadang terisi, kadang tidak".</li>
 *   <li><b>Efek tulis-balik ke {@code KegiatanSiswa.pembina1}.</b>
 *       {@link KegiatanSiswa#getPembina1()} bukan getter murni: ia <i>menugaskan</i> hasil
 *       pencarian ke field {@code pembina1}. Karena pemetaan Hibernate memakai
 *       <i>property access</i> (anotasi ada di getter) dengan {@code dynamicUpdate}, nilai
 *       hasil pencarian itu ikut tertulis ke kolom {@code kegiatan_siswa.pembina1} pada flush
 *       berikutnya &mdash; jadi pemetaan di tabel ini "membeku" ke dalam baris kegiatan siswa.
 *       Ini instance pola "getter write-back" yang sudah dikenal di repo, namun varian
 *       <i>mengisi</i> (bukan varian destruktif yang menimpa data dengan {@code null}).</li>
 *   <li><b>Bandbox "Pembina 1" dikunci bila pemetaan ditemukan.</b> {@code KegiatanSiswaAction}
 *       memanggil {@code pembina1.setDisabled(true)} setelah mengisi otomatis, sehingga
 *       pengisi kegiatan tidak bisa menunjuk pembina lain untuk kegiatan tertentu selama
 *       baris di tabel ini ada.</li>
 *   <li><b>Filter "Nama Pembina" di layar master RUSAK (salah alias).</b>
 *       {@code PembinaSiswaAction.initCriteria()} membuat alias {@code pembina} lalu tidak
 *       pernah memakainya; filternya berbunyi
 *       {@code Restrictions.ilike("siswa.userNama", ...)} &mdash; properti {@code userNama}
 *       milik {@link Tbmuser}, sedangkan {@code Siswa} tidak memilikinya. Setiap pencarian
 *       dengan kotak "Nama Pembina" terisi akan melempar
 *       {@code QueryException: could not resolve property: userNama of ...Siswa}. Yang benar
 *       adalah {@code "pembina.userNama"}. Ini kerabat pola salah-salin yang sama dengan
 *       filter rusak di {@code OrganisasiSiswaAction}, tetapi di sini gagal <i>berisik</i>
 *       (exception), bukan diam-diam menyaring nol baris.</li>
 *   <li><b>Fail-open cakupan tenant yang justru terkunci.</b> {@code doAfterCompose()}
 *       memanggil {@code Common.selectComboItem(searchyayasan, tbmuser.ambilYayasan())} lalu
 *       LANGSUNG {@code searchyayasan.setDisabled(true)}. Combo hanya diisi yayasan
 *       {@code aktif = true / null}; bila yayasan pengguna tidak ada di daftar itu,
 *       pemilihannya gagal DIAM-DIAM, combo tertinggal di "Semua" &mdash; dan karena sudah
 *       dinonaktifkan, pengguna tak bisa memperbaikinya. {@code initCriteria()} kemudian tidak
 *       memasang filter yayasan sama sekali, sehingga seluruh pemetaan pembina lintas yayasan
 *       ikut tampil. Hal yang sama berlaku untuk akun tanpa yayasan
 *       ({@code ambilYayasan() == null}), yang bahkan comboonya tetap aktif dan boleh memilih
 *       "Semua". Varian keluarga fail-open cakupan tenant yang sudah tercatat.</li>
 *   <li><b>Pewarisan hak lewat menu induk.</b> Karena {@code pembina_siswa.zul} tidak punya
 *       entri menu sendiri, {@code CommonPrivilages.checkPrevilages(...)} di
 *       {@code PembinaSiswaAction} menguji hak atas {@code Common.getCurrentMenu()} &mdash;
 *       yaitu menu <b>Kegiatan Siswa</b>/<b>Kegiatan/Aktifitas Siswa</b>
 *       ({@code MenuSnapshotData}) atau menu <b>Siswa</b> bila layar dibuka lewat tab biodata
 *       siswa. Siapa pun yang berhak CREATE/UPDATE/DELETE pada menu-menu itu otomatis berhak
 *       CRUD penuh atas pemetaan pembina. Penyembunyian tab
 *       ({@code pembinaTab.setVisible(...)} untuk akun guru/siswa di
 *       {@code KegiatanSiswaAction}) bersifat kosmetik pada header tab &mdash; isi tabpanel
 *       tetap di-compose ZK, jadi ia bukan kontrol akses.</li>
 *   <li><b>Yang TIDAK ditemukan (verifikasi negatif, menenangkan).</b> Berbeda dengan
 *       {@code OrganisasiSiswaAction} di domain yang bertetangga, di sini <b>TIDAK ADA
 *       SQL injection</b>: seluruh filter memakai {@code Restrictions.ilike(...)}/
 *       {@code CommonSearchFilterHelper} yang terparameterisasi, dan satu-satunya
 *       {@code sqlRestriction} yang dipakai adalah literal konstan {@code "true"}/{@code "1=1"}
 *       tanpa penyambungan input pengguna. Juga <b>TIDAK ADA bug schema salah-salin</b>
 *       {@code public.*} vs {@code sekolah.*}: {@code @Table} sudah
 *       {@code schema = "sekolah"} dan tidak ada satu pun native SQL yang menyebut tabel ini.
 *       Tabel ini juga tidak tersentuh jalur pra-otentikasi mana pun
 *       ({@code pembina_siswa_service.jsp} hanya scaffold metadata tanpa akses data).</li>
 * </ol>
 *
 * <h2>Catatan warisan {@link GeneralValueObject}</h2>
 *
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass}
 * &mdash; ia POJO abstrak biasa, sehingga Hibernate TIDAK memetakan properti apa pun miliknya.
 * Karena itu deklarasi ulang {@code id}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} di kelas ini <b>bukan duplikasi ceroboh melainkan keharusan
 * teknis</b>: tanpa deklarasi ulang itu keempat kolom tersebut tidak akan pernah ada di tabel.
 * Yang tetap diwarisi adalah perilaku non-persisten seperti {@code check(...)} (resolusi proxy
 * lazy/entity kanonik) yang dipakai kedua getter relasi di bawah.</p>
 *
 * @see Siswa
 * @see Tbmuser
 * @see KegiatanSiswa
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "pembina_siswa", schema = "sekolah")
public class PembinaSiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya <b>sama persis</b> dengan
	 * {@link AsramaSiswaPunyaSiswa}, {@code AsramaSiswa}, {@code KelasSiswa},
	 * {@code KelasSiswaPunyaSiswa}, {@code PengaturanBiayaPunyaSiswa}, dan beberapa entity
	 * modul sekolah lainnya, karena seluruh berkas itu lahir dari salin-tempel yang sama
	 * &mdash; bukan penanda kompatibilitas serialisasi yang dirancang. Jangan diubah: objek
	 * entity AIS diserialkan ke cache memori dan ke sesi ZK, sehingga mengubahnya membuat data
	 * ter-cache lama tidak terbaca.
	 */
	private static final long serialVersionUID = -9157912161411433979L;

	/**
	 * Kunci primer baris. Dideklarasikan ulang di sini karena {@link GeneralValueObject} tidak
	 * dipetakan Hibernate. Lihat {@link #getId()} untuk pemetaan kolomnya.
	 */
	private Long id;

	/**
	 * Nama pengguna terakhir yang menyimpan baris ini, diisi otomatis oleh
	 * {@code AuditTimestampInterceptor}. Lihat {@link #getOleh()}.
	 */
	private String oleh;

	/**
	 * User ID pengguna terakhir yang menyimpan baris ini, diisi otomatis oleh
	 * {@code AuditTimestampInterceptor}. Lihat {@link #getOlehId()}.
	 */
	private String olehId;

	/**
	 * Mengembalikan User ID pengguna terakhir yang menyimpan baris ini.
	 *
	 * @return user id penyimpan terakhir, atau {@code null} bila baris belum pernah disimpan
	 *         lewat jalur yang memasang interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi User ID penyimpan terakhir. <b>Sengaja mengabaikan nilai kosong</b>: bila
	 * {@code olehId} {@code null} atau hanya spasi, method langsung {@code return} tanpa
	 * mengubah apa pun, sehingga jejak audit lama tidak terhapus oleh penyimpanan dari jalur
	 * yang tidak mengenali pengguna (mis. proses latar). Dipanggil
	 * {@code AuditTimestampInterceptor}, bukan oleh layar.
	 *
	 * @param olehId user id penyimpan; {@code null}/kosong diabaikan (no-op)
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama penyimpan terakhir. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null}/kosong diabaikan agar jejak audit lama tidak tertimpa.
	 *
	 * @param oleh nama penyimpan; {@code null}/kosong diabaikan (no-op)
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang menyimpan baris ini.
	 *
	 * @return nama penyimpan terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate} yang meneruskan ke
	 * {@code AuditTimestampInterceptor.ubah(this)} agar {@link #getTanggal_dirubah()} (dan
	 * {@code oleh}/{@code olehId}) diperbarui otomatis setiap kali baris ini di-UPDATE.
	 * Dipanggil Hibernate, jangan dipanggil manual.
	 *
	 * <p><b>Catatan pembacaan kode:</b> pada baris yang sama juga dideklarasikan field
	 * {@code tanggal_dirubah} (waktu perubahan terakhir, nilai awal {@code WaktuUtil.getDate()}
	 * sehingga baris baru sudah bertanggal walau belum pernah di-UPDATE). Keduanya menempel di
	 * satu baris sebagai hasil penyisipan otomatis lintas-berkas; formatnya dipertahankan apa
	 * adanya agar diff terhadap entity sejenis tetap bersih.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi waktu perubahan terakhir baris ini.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (kolom {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada objek yang baru dibuat
	 *         karena field-nya diinisialisasi {@code WaktuUtil.getDate()}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Akun pengguna yang ditunjuk sebagai pembina siswa (kolom FK {@code pembina}). Lihat
	 * {@link #getPembina()}.
	 */
	private Tbmuser pembina;

	/**
	 * Siswa yang dibina (kolom FK {@code siswa_id}). Lihat {@link #getSiswa()}.
	 */
	private Siswa siswa;

	/**
	 * Catatan bebas atas penugasan pembina ini. Lihat {@link #getKeterangan()}.
	 */
	private String keterangan;

	/**
	 * Saklar aktif/nonaktif baris. Lihat {@link #getAktif()} untuk normalisasi
	 * {@code null -> true} dan catatan bahwa saklar ini diabaikan pembacanya.
	 */
	private Boolean aktif;

	/**
	 * Konstruktor kosong wajib Hibernate. Dipakai juga oleh {@code PembinaSiswaAction.onAdd()}
	 * saat membuka dialog "Tambah Pembina Siswa". Tidak ada konstruktor berparameter di kelas
	 * ini.
	 */
	public PembinaSiswa() {
	}

	/**
	 * Mengembalikan kunci primer baris ({@code IDENTITY}, {@code insertable = false} karena
	 * nilainya dibangkitkan database).
	 *
	 * @return id baris, atau {@code null} bila objek belum pernah disimpan &mdash;
	 *         {@code PembinaSiswaAction} memakai kondisi {@code getId() == null} untuk
	 *         membedakan judul dialog "Tambah" vs "Ubah" dan untuk memilih
	 *         {@code session.save} vs {@code session.update}
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci primer baris. Umumnya hanya dipanggil Hibernate.
	 *
	 * @param id kunci primer baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan catatan bebas atas penugasan pembina ini (mis. "Pembina Pramuka",
	 * "Pembina OSIS"), sebagaimana diketik pada kotak "Keterangan" dialog tambah/ubah.
	 *
	 * <p><b>Catatan:</b> nilainya hanya ditampilkan sebagai kolom "Keterangan" pada grid layar
	 * master dan ikut kolom ekspor tombol Download; tidak ada logika bisnis yang mengurainya
	 * &mdash; jadi jangan dipakai sebagai penanda jenis kegiatan yang bisa diandalkan mesin.</p>
	 *
	 * @return keterangan bebas, atau {@code null} bila tidak diisi
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Mengisi catatan bebas atas penugasan pembina ini.
	 *
	 * @param keterangan catatan bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif baris, dengan normalisasi <b>{@code null} dianggap
	 * {@code true}</b> (baris lama/hasil sisipan yang belum pernah menyentuh checkbox tetap
	 * dianggap aktif).
	 *
	 * <p><b>Efek samping tak langsung (property access).</b> Karena pemetaan Hibernate kelas ini
	 * berbasis getter, nilai yang sudah dinormalkan inilah yang dibaca saat INSERT/UPDATE
	 * &mdash; sehingga baris baru dari dialog "Tambah Pembina Siswa" (yang tidak pernah
	 * memanggil {@link #setAktif(Boolean)}) tersimpan dengan {@code aktif = true}, bukan
	 * {@code NULL}. Baris yang masuk lewat SQL mentah/migrasi tetap bisa bernilai {@code NULL}
	 * di database; {@code PembinaSiswaAction.initCriteria()} sudah mengantisipasinya dengan
	 * {@code Restrictions.or(isNull("aktif"), eq("aktif", true))}.</p>
	 *
	 * <p><b>Penting:</b> saklar ini <b>tidak dibaca</b> oleh satu-satunya konsumen data ini
	 * ({@link KegiatanSiswa#getPembina1()} dan {@code KegiatanSiswaAction}) &mdash; lihat
	 * kuirk 3 pada Javadoc kelas. Menonaktifkan baris hanya menyembunyikannya dari layar
	 * master.</p>
	 *
	 * @return {@code true} bila aktif atau belum pernah diisi; {@code false} bila sengaja
	 *         dinonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengisi status aktif baris. Satu-satunya pemanggil adalah listener {@code onCheck}
	 * checkbox "Aktif" pada grid {@code PembinaSiswaAction} (yang langsung menyimpan lewat
	 * {@code Common.refreshSaveOrUpdate}); checkbox itu dinonaktifkan bila pengguna tidak
	 * berhak UPDATE.
	 *
	 * @param aktif status aktif baru; {@code null} akan dibaca kembali sebagai {@code true}
	 *            oleh {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan siswa yang dibina (kolom FK wajib {@code siswa_id}).
	 *
	 * <p>Nilai dilewatkan {@code check(...)} milik {@link GeneralValueObject} lebih dulu, yang
	 * meresolusi proxy lazy/objek kanonik dari peta identitas &mdash; ini <b>bukan</b> getter
	 * destruktif: hasilnya adalah entity yang sama, hanya dipastikan terinisialisasi, sehingga
	 * aman dipakai pada objek detached (mis. baris yang dibaca dari cache memori JVM).</p>
	 *
	 * @return siswa yang dibina; secara skema tidak boleh {@code null}
	 *         ({@code nullable = false}), dan {@code PembinaSiswaAction.onSave()} menolak
	 *         menyimpan bila belum dipilih
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa_id", nullable = false)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	/**
	 * Mengisi siswa yang dibina. Dipanggil {@code PembinaSiswaAction.onSave()} dengan nilai
	 * hasil pilihan {@code AmbilDataSiswaBanbox} (bandbox "Siswa *").
	 *
	 * @param siswa siswa yang dibina; wajib diisi sebelum disimpan
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Mengembalikan akun pengguna yang menjadi pembina siswa ini (kolom FK wajib
	 * {@code pembina} &mdash; perhatikan nama kolomnya <b>tanpa</b> sufiks {@code _id},
	 * berbeda dari konvensi {@code siswa_id} di atas).
	 *
	 * <p>Seperti {@link #getSiswa()}, nilai dilewatkan {@code check(...)} untuk meresolusi
	 * proxy lazy/objek kanonik; bukan getter destruktif.</p>
	 *
	 * <p>Tipenya {@link Tbmuser} (akun pengguna), <b>bukan</b> {@code Guru}: pemilih di layar
	 * ({@code AmbilDataTbmuserBanbox}) menampilkan semua akun aktif kecuali role Mahasiswa,
	 * orang tua, dan penyedia, tanpa pembatasan sekolah/yayasan &mdash; sehingga pembina yang
	 * tersimpan bisa saja berasal dari tenant lain. Nilai inilah yang dipasang otomatis ke
	 * {@code KegiatanSiswa.pembina1} (lihat kuirk 5 pada Javadoc kelas).</p>
	 *
	 * @return akun pembina; secara skema tidak boleh {@code null}
	 *         ({@code nullable = false}), dan {@code PembinaSiswaAction.onSave()} menolak
	 *         menyimpan bila belum dipilih
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pembina", nullable = false)
	public Tbmuser getPembina() {
		pembina = check(pembina);
		return pembina;
	}

	/**
	 * Mengisi akun pengguna yang menjadi pembina siswa ini. Dipanggil
	 * {@code PembinaSiswaAction.onSave()} dengan nilai hasil pilihan
	 * {@code AmbilDataTbmuserBanbox} (bandbox "Pembina *").
	 *
	 * @param pembina akun pembina; wajib diisi sebelum disimpan
	 */
	public void setPembina(Tbmuser pembina) {
		this.pembina = pembina;
	}
}
