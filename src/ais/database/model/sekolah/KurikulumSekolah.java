package ais.database.model.sekolah;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

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

/**
 * Entity Hibernate untuk <b>kurikulum sekolah</b> pada tabel {@code sekolah.kurikulum_sekolah} —
 * satu baris mewakili satu kurikulum yang berlaku pada satu {@link Sekolah} tertentu, misalnya
 * "Kurikulum 2013 Revisi" atau "Kurikulum Merdeka" milik SMP X.
 *
 * <p>Kelas ini adalah <b>padanan versi sekolah</b> dari {@link ais.database.model.Kurikulum} (versi
 * perguruan tinggi). Keduanya menempati posisi arsitektur yang sama — simpul pusat perencanaan
 * akademik yang tidak menyimpan daftar mata pelajaran/kuliahnya sendiri melainkan dirujuk dari
 * sebuah join table — tetapi <b>jauh lebih ramping</b>. Perbandingan yang perlu diketahui pembaca
 * yang datang dari sisi PT:</p>
 * <table border="1" summary="Perbandingan Kurikulum (PT) vs KurikulumSekolah">
 * <tr><th>Aspek</th><th>{@code Kurikulum} (PT)</th><th>{@code KurikulumSekolah}</th></tr>
 * <tr><td>Pemilik/cakupan</td><td>{@code Jurusan} + {@code Program}</td>
 * <td>{@link Sekolah} + {@link Yayasan}</td></tr>
 * <tr><td>Join ke materi ajar</td><td>{@code KurikulumPunyaMatakuliah}</td>
 * <td>{@link KurikulumPunyaMatapelajaran}</td></tr>
 * <tr><td>Masa berlaku (tahun, tahun akademik, jenis semester)</td><td>ada</td>
 * <td><b>tidak ada</b></td></tr>
 * <tr><td>Aturan kelulusan (SKS wajib/pilihan/lulus)</td><td>ada</td><td><b>tidak ada</b></td></tr>
 * <tr><td>Aturan pengambilan per angkatan ({@code bolehAmbil})</td><td>ada</td>
 * <td><b>tidak ada</b></td></tr>
 * <tr><td>Ambang OBE ({@code apakahObe})</td><td>ada</td><td><b>tidak ada</b></td></tr>
 * <tr><td>Integrasi Feeder/PDDikti</td><td>ada</td><td><b>tidak ada</b> (bukan pelaporan Dikti)</td></tr>
 * <tr><td>Skema penilaian</td><td>tidak ada (dipilih per mata kuliah/OBE)</td>
 * <td>{@link JenisPenilaian} sebagai <b>override</b> tingkat kurikulum</td></tr>
 * </table>
 * <p>Dengan kata lain: <b>tidak ada satu pun method bisnis</b> di kelas ini. Seluruh perilakunya
 * adalah accessor; keputusan akademik yang di sisi PT dijawab oleh {@code Kurikulum} sendiri, di
 * sisi sekolah dijawab oleh entity lain (jadwal, kelas, penilaian) yang <i>menunjuk</i> ke sini.</p>
 *
 * <h3>Peran dalam domain akademik sekolah</h3>
 * <p>Kurikulum sekolah adalah <b>simpul pengikat</b> antara katalog mata pelajaran dan seluruh
 * pelaksanaan akademik. Tiga entity menunjuk ke sini lewat kolom FK {@code kurikulum_sekolah_id}:</p>
 * <ul>
 * <li>{@link KurikulumPunyaMatapelajaran} (tabel {@code sekolah.kurikulum_punya_matapelajaran}) —
 * join table kurikulum &harr; {@link Matapelajaran}, satu baris per pasangan, menyimpan
 * {@code jumlahJamPelajaran} dan flag {@code aktif} (apakah mapel itu dipakai kurikulum ini). Ini
 * adalah "isi" kurikulum, dan dirakit seluruhnya oleh {@code KurikulumSekolahAction.ubahKurikulum()}
 * dalam bentuk grid centang + jumlah jam.</li>
 * <li>{@link KelasSiswa} (tabel {@code sekolah.kelas}) — rombongan belajar. Satu kelas menempel
 * pada satu kurikulum, dan lewat rantai itulah rapor tahu mapel apa saja yang harus dicetak.
 * <b>Catatan integritas:</b> {@code cascade.sql} menyetel constraint {@code fk6135e04dbfdda4d}
 * menjadi {@code ON DELETE SET NULL} — menghapus kurikulum <b>tidak</b> menghapus kelasnya,
 * melainkan meninggalkan kelas "yatim kurikulum" yang diam-diam kehilangan daftar mapelnya.</li>
 * <li>{@link JenisNilaiSiswa} (tabel {@code sekolah.jenis_nilai_siswa}) — komponen nilai
 * (tugas/UTS/UAS beserta bobotnya) yang dikonfigurasi per kurikulum.</li>
 * </ul>
 * <p><b>Tidak ada koleksi {@code @OneToMany} balik di kelas ini.</b> Seluruh pembacaan "anak dari
 * kurikulum ini" dilakukan lewat criteria eksplisit {@code Restrictions.eq("kurikulumSekolah",
 * ...)} dari sisi anak. Cascade pada ketiga relasi di atas hanya {@code PERSIST}/{@code MERGE},
 * jadi menghapus kurikulum <b>tidak</b> punya proteksi orphan di level ORM sama sekali —
 * satu-satunya jaring pengaman adalah constraint database di {@code cascade.sql}, dan itu pun hanya
 * untuk {@code sekolah.kelas}.</p>
 * <p>Karena kurikulum boleh {@code null} pada {@link KelasSiswa}, rantai navigasi ke sini adalah
 * sumber {@code NullPointerException} yang berulang. Sebagian pemanggil menjaganya dengan benar
 * (mis. {@code PenilaianSiswaAction} dan {@code TampilStudiSiswaHelper} melewati kelas tanpa
 * kurikulum, {@code JadwalPelajaranAction} dan {@code DetailPenilaianSiswaHelper} menampilkan
 * pesan "Kurikulum belum di setting"), sebagian <b>tidak</b> — {@code DetailKelasSiswaHelper}
 * memanggil {@code ...getKurikulumSekolah().getNama()} tanpa penjaga, dan
 * {@link JadwalPelajaran} menurunkan sekolahnya lewat
 * {@code getKurikulumPunyaMatapelajaran().getKurikulumSekolah().getSekolah()} juga tanpa penjaga.
 * Ingat bahwa instalasi baru dimulai dengan nol kurikulum, sehingga jalur tak terjaga itu adalah
 * jalur yang justru paling mungkin ditempuh saat sistem baru dipasang.</p>
 *
 * <p>Pengguna hilir lain membaca kurikulum lewat rantai di atas, bukan lewat FK langsung:
 * {@link JadwalPelajaran}, laporan
 * {@code LaporanRaporSiswa} dan {@code LaporanRekapTotalNilai}, REST {@code ElearningApiUtil} dan
 * {@code NilaiSiswaApi}, helper penilaian/kelas di {@code ais.action.master.sekolah.helper}, serta
 * beberapa dasbor ({@code DashboardKurikulumSekolah}, {@code DasboardJadwalPelajaran},
 * {@code DasborAkademikSekolah}, {@code DashboardDataSekolah}).</p>
 *
 * <h3>Layar master &amp; siklus hidup baris</h3>
 * <p>Satu-satunya layar CRUD adalah {@code ais.action.master.sekolah.KurikulumSekolahAction}
 * (menu "Kurikulum Sekolah"). Alur simpannya, yang perlu diketahui karena menjelaskan beberapa
 * kuirk di bawah:</p>
 * <ol>
 * <li>{@code onSave()} memvalidasi {@code nama}, {@code yayasan}, dan {@code sekolah} wajib terisi
 * — jadi meskipun kolom {@code sekolah_id}/{@code yayasan_id} secara skema {@code nullable},
 * baris yang lahir dari layar master <b>selalu</b> punya keduanya.</li>
 * <li>Baris kurikulum disimpan lebih dulu, lalu <b>seluruh</b> baris
 * {@link KurikulumPunyaMatapelajaran} pada grid disimpan/di-update — termasuk baris yang tidak
 * dicentang (baris tersebut tetap dibuat, hanya {@code aktif = false}).</li>
 * <li>Tidak ada bidang {@code aktif} di formulir tambah/ubah. Flag {@code aktif} hanya bisa
 * diubah lewat checkbox di <b>grid</b> daftar, yang langsung memanggil
 * {@code Common.refreshSaveOrUpdate()} per klik.</li>
 * </ol>
 * <p><b>Tidak ada baris bawaan.</b> Berbeda dari banyak master lain di modul ini, kurikulum
 * sekolah <b>tidak pernah di-seed otomatis</b>: {@code InitData} hanya menyebut kelas ini dalam
 * daftar pramuat cache (lihat bagian berikut), bukan dalam daftar penyemai, dan satu-satunya
 * {@code new KurikulumSekolah()} di seluruh codebase ada di {@code KurikulumSekolahAction.onAdd()}
 * — yaitu ketika pengguna menekan tombol "Tambah". Instalasi baru karena itu <b>dimulai dengan nol
 * kurikulum</b>, dan itulah sebabnya beberapa layar hilir menampilkan pesan "Kurikulum belum di
 * setting" alih-alih data.</p>
 *
 * <h3>Kurikulum ini di-cache seumur hidup proses</h3>
 * <p>Dua daftar statis menyebut kelas ini, dan gabungannya berarti <b>seluruh isi tabel
 * {@code kurikulum_sekolah} milik semua sekolah</b> berada di memori aplikasi sejak boot sampai
 * proses mati:</p>
 * <ul>
 * <li>{@code InitData.doInitData()} memanggil {@code initClasses(..., KurikulumSekolah.class, ...)}
 * &rarr; {@code InitDataHelper.initData()} — pramuat seluruh baris ke cache in-memory saat
 * startup;</li>
 * <li>{@code DataUtil.CLASS_JANGAN_DIBERSIHKAN} memuat {@code KurikulumSekolah.class.getName()}
 * sehingga entri cache-nya <b>tidak pernah</b> dibuang oleh pembersihan berkala.</li>
 * </ul>
 * <p>Konsekuensi praktis: instance yang dikembalikan {@link GeneralValueObject#check(Object)} sering
 * berasal dari cache dan sudah <i>detached</i> dari {@code Session} yang aktif. Jangan berasumsi
 * object yang Anda pegang terkelola; gunakan {@code session.load()}/{@code merge()} sebelum menulis
 * (persis yang dilakukan {@code KurikulumSekolahAction.onSave()}).</p>
 *
 * <h3>Field jejak audit yang dideklarasikan ulang — keharusan teknis</h3>
 * <p>Kelas ini mendeklarasikan ulang {@code id}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} meskipun {@link GeneralValueObject} sudah punya konsep yang sama. Ini
 * <b>bukan duplikasi yang keliru</b>: {@link GeneralValueObject} adalah POJO abstrak biasa — bukan
 * {@code @Entity} maupun {@code @MappedSuperclass} — sehingga Hibernate <b>tidak memetakan properti
 * apa pun dari kelas induk</b>. Setiap entity konkret wajib mendeklarasikan sendiri kolom-kolom
 * tersebut agar ikut tersimpan. Pola ini seragam di seluruh {@code ais.database.model}.</p>
 *
 * <h3>Kuirk yang wajib diketahui</h3>
 * <ol>
 * <li><b>{@link #getYayasan()} adalah getter penulis-balik yang destruktif.</b> Bila {@code sekolah}
 * terisi, getter ini <b>menimpa</b> field {@code yayasan} dengan {@code sekolah.getYayasan()} —
 * berapa pun nilai yang sebelumnya diberikan lewat {@link #setYayasan(Yayasan)}. Karena Hibernate
 * membaca properti lewat getter saat <i>dirty check</i>, sekadar <b>membaca</b> yayasan dari
 * instance yang masih <i>attached</i> bisa menghasilkan {@code UPDATE} pada {@code yayasan_id} dan
 * — karena kelas ini {@code @Audited} — satu baris revisi Envers baru. Sisi baiknya, kolom
 * {@code yayasan_id} praktis mustahil melenceng dari yayasan pemilik sekolahnya.</li>
 * <li><b>{@link #getAktif()} mengaburkan {@code NULL} menjadi {@code true} — dan divergensi
 * Java-vs-SQL yang ditimbulkannya BENAR-BENAR TERPICU di dua layar.</b> Kolom {@code aktif} di
 * database boleh tetap {@code NULL} sementara getter selalu melaporkan {@code true}. Query
 * Criteria karena itu <b>wajib</b> ditulis
 * {@code Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))}. Mayoritas
 * pemanggil memang menulisnya begitu (ketiga dasbor, {@code GenericCrudRelationLookupService},
 * {@code DynamicJspCrudGenerator}, JSP statistik), <b>tetapi dua combobox tidak</b>:
 * {@code KelasSiswaAction} dan {@code JenisNilaiSiswaAction} memakai
 * {@code Restrictions.and(Restrictions.eq("aktif", true), Restrictions.eq("sekolah", ...))}.
 * Akibatnya kurikulum yang kolom {@code aktif}-nya masih {@code NULL} <b>hilang senyap</b> dari
 * daftar pilihan kurikulum saat membuat kelas atau menyetel komponen nilai — padahal grid master
 * menampilkan checkbox "Aktif" dalam keadaan tercentang dan seluruh dasbor menghitungnya sebagai
 * aktif. Rinciannya di {@link #getAktif()}. Kuirk dasarnya sama persis dengan
 * {@code Kurikulum#getAktif()} versi PT.</li>
 * <li><b>{@code compareTo} tidak dioverride di sini.</b> Kelas ini mewarisi
 * {@link GeneralValueObject#compareTo(GeneralValueObject)} yang mencoba kunci berurutan
 * {@code nomorUrut} &rarr; {@code nim} &rarr; {@code nama} &rarr; {@code keterangan}.
 * {@code nomorUrut} dan {@code nim} adalah properti kelas induk yang <b>tidak dipetakan</b> untuk
 * entity ini, jadi selalu {@code null} &mdash; kunci efektifnya adalah {@link #getNama()}. Karena
 * {@code nama} <b>tidak unik lintas sekolah</b> (dua sekolah boleh sama-sama punya "Kurikulum
 * 2013"), memasukkan kurikulum lintas sekolah ke {@code TreeSet}/{@code TreeMap} akan
 * <b>menciutkan</b> baris yang bernama sama menjadi satu. Saat dokumentasi ini ditulis tidak ada
 * satu pun {@code TreeSet<KurikulumSekolah>} di codebase, jadi risiko ini <b>laten, belum
 * aktif</b> — tapi jangan jadi orang pertama yang membuatnya.</li>
 * <li><b>{@link #getKeterangan()} di kelas ini normal.</b> Berbeda dari sejumlah entity lain di
 * modul ini yang memakai {@code getKeterangan()} untuk mengembalikan sesuatu selain isi kolom
 * {@code keterangan}, di sini getter-nya benar-benar hanya mengembalikan field-nya (dan boleh
 * {@code null}). Perhatikan bahwa kontrak ini <b>berbeda</b> dari
 * {@code GeneralValueObject.getKeterangan()} yang tidak pernah mengembalikan {@code null} — lihat
 * catatan pada {@link #getKeterangan()}.</li>
 * <li><b>Tidak ada penyaringan sekolah/yayasan bawaan.</b> Entity ini tidak menegakkan cakupan
 * tenant apa pun; penyaringan sepenuhnya bergantung pada pemanggil. Lihat bagian berikut.</li>
 * </ol>
 *
 * <h3>Catatan cakupan tenant (fail-open) — pola berulang yang dikenal</h3>
 * <p>{@code KurikulumSekolahAction.initCriteria()} <b>tidak memuat filter cakupan tenant sama
 * sekali</b>. Ia hanya menerjemahkan pilihan combo pencarian; bila {@code searchsekolah} dan
 * {@code searchyayasan} tidak terpilih, kedua klausanya menjadi literal
 * {@code Restrictions.sqlRestriction("1=1")} dan daftar menampilkan kurikulum <b>seluruh sekolah
 * dan yayasan</b>. Yang membatasi hasil semata-mata pra-pemilihan combo oleh
 * {@code InitComboUtil.initYayasanDanSekolahDanSemua()}; ketika pengguna tidak punya konteks
 * sekolah/yayasan aktif <i>dan</i> profilnya tidak menyebut sekolah/yayasan — atau ketika helper
 * itu melempar exception, yang ditelan {@code catch} dan hanya ditampilkan kepada admin — tidak ada
 * yang terpilih dan filternya terbuka penuh. Ini persis bentuk <b>fail-open</b> yang sudah
 * berulang kali tercatat di modul {@code sekolah/}.</p>
 * <p>Satu pemanggil lain bahkan <b>tidak pernah menyaring apa pun</b>, tanpa perlu kondisi
 * fail-open: {@code TimetableJadwalPelajaranWindow} mengisi combobox kurikulumnya dengan
 * {@code createCriteria(KurikulumSekolah.class).addOrder(Order.desc("id")).setMaxResults(200)} —
 * tanpa klausa {@code sekolah}, tanpa klausa {@code yayasan}, tanpa klausa {@code aktif}. Siapa
 * pun yang bisa membuka layar penyusun jadwal melihat 200 kurikulum terbaru <i>seluruh
 * instalasi</i>. Batas 200 itu sendiri juga cacat fungsional tersendiri: pada instalasi
 * multi-sekolah yang besar, kurikulum sekolah sendiri bisa terdorong keluar daftar oleh kurikulum
 * sekolah lain yang id-nya lebih baru.</p>
 * <p><b>Tingkat keparahan kedua temuan di atas rendah</b>: isi tabel ini adalah metadata katalog
 * (nama kurikulum, keterangan, skema penilaian) — bukan PII, bukan data siswa. Dicatat untuk
 * kelengkapan pola, bukan sebagai temuan keamanan mandiri.</p>
 *
 * <h3>Pengelompokan method</h3>
 * <ol>
 * <li><b>Jejak audit</b> — {@link #getOleh()}/{@link #setOleh(String)},
 * {@link #getOlehId()}/{@link #setOlehId(String)},
 * {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, hook {@code @PreUpdate}
 * {@link #onUpdate()}.</li>
 * <li><b>Identitas</b> — {@link #KurikulumSekolah()}, {@link #KurikulumSekolah(long, String)},
 * {@link #getId()}/{@link #setId(Long)}, {@link #getNama()}/{@link #setNama(String)},
 * {@link #getKeterangan()}/{@link #setKeterangan(String)}.</li>
 * <li><b>Cakupan tenant</b> — {@link #getSekolah()}/{@link #setSekolah(Sekolah)},
 * {@link #getYayasan()}/{@link #setYayasan(Yayasan)}.</li>
 * <li><b>Konfigurasi akademik</b> —
 * {@link #getJenisPenilaian()}/{@link #setJenisPenilaian(JenisPenilaian)},
 * {@link #getAktif()}/{@link #setAktif(Boolean)}.</li>
 * </ol>
 *
 * <h3>Catatan pemetaan</h3>
 * <p>Hanya sebagian getter yang diberi {@code @Column} eksplisit ({@code id}, {@code keterangan},
 * {@code nama}); {@code aktif}, {@code oleh}, dan {@code olehId} mengandalkan penamaan kolom
 * default Hibernate dari nama properti. Kelas ini {@code dynamicInsert}/{@code dynamicUpdate},
 * jadi hanya kolom yang benar-benar berubah yang ikut ditulis — yang meredam, tapi tidak
 * menghapus, efek getter penulis-balik {@link #getYayasan()} di atas.</p>
 * <p>Kontrak umum {@code equals}, {@code hashCode}, {@code compareTo}, cache, dan terutama
 * {@link GeneralValueObject#check(Object)} dijelaskan lengkap di kelas induk — jangan diulang di
 * sini.</p>
 *
 * <h3>Catatan: komentar generator yang menyesatkan (digantikan oleh dokumentasi ini)</h3>
 * <p>Sebelum dokumentasi ini ditulis, satu-satunya komentar kelas di sini adalah baris bawaan
 * hbm2java yang berbunyi "{@code JenisGuru generated by hbm2java}" — <b>nama kelas yang salah</b>,
 * sisa salin-tempel template generator. Teks keliru yang sama masih muncul di <b>17 berkas</b> di
 * bawah {@code ais/database/model/}, jadi jangan sekali pun memakai baris "generated by hbm2java"
 * untuk menebak kelas mana yang sedang dibaca. Komentar {@code // Generated ... by Hibernate Tools}
 * pada baris kedua berkas ini tidak diubah.</p>
 *
 * @see GeneralValueObject
 * @see ais.database.model.Kurikulum
 * @see KurikulumPunyaMatapelajaran
 * @see KelasSiswa
 * @see JenisNilaiSiswa
 * @see JenisPenilaian
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "kurikulum_sekolah", schema = "sekolah")
public class KurikulumSekolah extends GeneralValueObject {

	/**
	 * Versi serialisasi Java.
	 *
	 * <p>Catatan: nilai ini <b>tidak unik</b> — angka yang sama dipakai oleh <b>22 kelas</b> di
	 * {@code ais.database.model} (antara lain {@link Apresiasi}, {@link ApresiasiDanPenghargaan},
	 * {@link ApresiasiSiswa}, {@link Hukuman}, {@code employ.HukumanPegawai},
	 * {@code PelanggaranMahasiswa}). Ini jejak salin-tempel, bukan makna. Tidak berdampak
	 * fungsional — {@code serialVersionUID} hanya dicocokkan per kelas, bukan lintas kelas — tapi
	 * jangan sekali pun dipakai sebagai penanda identitas kelas.</p>
	 */
	private static final long serialVersionUID = -7490758846785025664L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini (kolom jejak audit).
	 *
	 * <p>Diisi otomatis oleh {@code AuditTimestampInterceptor} lewat hook {@link #onUpdate()},
	 * bukan oleh layar master.</p>
	 *
	 * @return id pengguna pengubah terakhir; {@code null} bila belum pernah terisi
	 * @see #getOleh()
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah terakhir.
	 *
	 * <p>Setter ini <b>sengaja mengabaikan</b> nilai {@code null} maupun string kosong/spasi:
	 * jejak audit yang sudah ada tidak boleh terhapus oleh pemanggil yang kebetulan tidak tahu
	 * siapa penggunanya. Akibatnya kolom ini tidak pernah bisa dikosongkan lewat setter.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong
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
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong diabaikan diam-diam
	 * sehingga jejak audit lama tetap utuh.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini (kolom jejak audit).
	 *
	 * @return nama pengguna pengubah terakhir; {@code null} bila belum pernah terisi
	 * @see #getOlehId()
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil Hibernate <b>tepat sebelum</b> pernyataan
	 * {@code UPDATE} baris ini dieksekusi.
	 *
	 * <p>Mendelegasikan ke {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)},
	 * yang mengisi {@link #setOleh(String)}, {@link #setOlehId(String)}, dan
	 * {@link #setTanggal_dirubah(Date)} dari konteks pengguna yang sedang aktif. Karena itu ketiga
	 * kolom jejak audit <b>tidak perlu</b> (dan tidak boleh) diisi manual oleh layar/Action.</p>
	 *
	 * <p><b>Hanya berlaku untuk UPDATE.</b> Tidak ada pasangan {@code @PrePersist}, sehingga pada
	 * INSERT pertama {@code oleh}/{@code olehId} tetap {@code null} dan {@code tanggal_dirubah}
	 * mengandalkan nilai awal field (lihat {@link #getTanggal_dirubah()}).</p>
	 *
	 * <p>Perhatikan interaksinya dengan {@link #getYayasan()}: getter penulis-balik itu bisa
	 * membuat entity dianggap kotor sehingga {@code UPDATE} — dan karenanya hook ini — terpicu
	 * oleh operasi yang secara semantik hanya "membaca".</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi waktu perubahan terakhir.
	 *
	 * <p>Umumnya dipanggil {@code AuditTimestampInterceptor} dari {@link #onUpdate()}, bukan oleh
	 * kode layar. Tidak ada validasi: nilai {@code null} diterima apa adanya dan akan menulis
	 * {@code NULL} ke kolom.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini.
	 *
	 * <p>Field-nya diinisialisasi saat object dibuat dengan {@code ais.ui.util.WaktuUtil.getDate()}
	 * — yaitu waktu server yang sudah dinormalkan aplikasi, bukan {@code new Date()} mentah.
	 * Artinya baris yang belum pernah di-{@code UPDATE} pun tetap punya stempel waktu, yakni waktu
	 * instance-nya <i>dibuat di JVM</i> (bukan waktu commit).</p>
	 *
	 * @return waktu perubahan terakhir; secara praktis tidak pernah {@code null} untuk object yang
	 *         dibuat lewat konstruktor
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	private Sekolah sekolah;
	private Yayasan yayasan;
	private String keterangan;
	private String nama;
	private JenisPenilaian jenisPenilaian;
	private Boolean aktif;

	/**
	 * Konstruktor tanpa argumen — wajib ada untuk Hibernate, dan dipakai
	 * {@code KurikulumSekolahAction.onAdd()} untuk membuat baris kosong pada formulir "Tambah
	 * Kurikulum Sekolah".
	 *
	 * <p>Seluruh field dibiarkan {@code null} kecuali {@code tanggal_dirubah} yang terisi waktu
	 * sekarang lewat inisialisasi field. Perhatikan {@link #getAktif()} sudah mengembalikan
	 * {@code true} untuk instance baru ini meskipun field {@code aktif} masih {@code null}.</p>
	 */
	public KurikulumSekolah() {
	}

	/**
	 * Konstruktor ringkas bawaan hbm2java untuk kolom-kolom {@code NOT NULL}.
	 *
	 * <p><b>Peringatan:</b> parameter {@code id} bertipe {@code long} primitif dan langsung
	 * ditanam ke field. Karena kolom {@code id} dipetakan {@code IDENTITY} dengan
	 * {@code insertable = false}, menyetel id secara manual membuat instance dianggap
	 * <i>detached</i> oleh Hibernate — {@code save()} akan gagal atau berperilaku tak terduga.
	 * Gunakan konstruktor ini hanya untuk membangun object pembanding/tampilan, bukan untuk
	 * menyimpan baris baru. Saat dokumentasi ini ditulis, tidak ada pemanggil di codebase.</p>
	 *
	 * @param id nilai primary key yang sudah diketahui
	 * @param nama nama kurikulum (kolom {@code NOT NULL})
	 */
	public KurikulumSekolah(long id, String nama) {
		this.id = id;
		this.nama = nama;
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * <p>Dipetakan {@code IDENTITY} dengan {@code insertable = false}: nilainya dihasilkan database
	 * (sequence/serial PostgreSQL) dan tidak pernah ikut dalam pernyataan {@code INSERT}. Karena
	 * berurutan dan mudah ditebak, id ini <b>tidak boleh</b> dipakai sebagai rahasia atau token.</p>
	 *
	 * <p>Nilai {@code null} adalah penanda yang dipakai luas di layar master untuk membedakan
	 * "baris baru" dari "baris yang sudah tersimpan" — lihat judul dialog di
	 * {@code KurikulumSekolahAction.init()} dan cabang {@code session.load()} di
	 * {@code onSave()}.</p>
	 *
	 * @return primary key; {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Tanpa validasi.
	 *
	 * <p>Praktis hanya dipanggil Hibernate saat memuat/menyimpan baris. Mengubahnya dari kode
	 * aplikasi akan mengacaukan identitas entity di dalam {@code Session}.</p>
	 *
	 * @param id primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan {@link Sekolah} pemilik kurikulum ini — dimensi cakupan tenant utama.
	 *
	 * <p>Relasi {@code @ManyToOne} LAZY ke kolom {@code sekolah_id}. Getter memanggil
	 * {@link GeneralValueObject#check(Object)} lebih dulu untuk meresolusi proxy lazy yang mungkin
	 * sudah <i>detached</i> (kasus yang sering terjadi di sini karena entity ini dipramuat ke cache
	 * app-wide sejak boot). {@code check()} bisa mengembalikan <b>instance lain</b> yang setara,
	 * dan hasilnya <b>ditulis balik</b> ke field — jadi ini bukan getter murni, walau efeknya
	 * jinak (sekadar mengganti proxy dengan object terinisialisasi, bukan mengubah nilai FK).</p>
	 *
	 * <p>Secara skema kolomnya {@code nullable}, tetapi {@code KurikulumSekolahAction.onSave()}
	 * mewajibkan pengguna memilih sekolah, sehingga baris yang lahir lewat layar master selalu
	 * terisi. Jangan jadikan itu jaminan untuk baris hasil impor Excel atau migrasi.</p>
	 *
	 * @return sekolah pemilik; {@code null} bila kolom FK kosong
	 * @see #getYayasan()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik kurikulum.
	 *
	 * <p><b>Menormalkan "sekolah kosong" menjadi {@code null}:</b> bila argumen {@code null} atau
	 * merupakan object {@link Sekolah} yang belum tersimpan ({@code getId() == null} — misalnya
	 * item pembungkus "== Semua ==" dari combobox), field disetel {@code null}. Ini mencegah
	 * Hibernate mencoba meng-{@code cascade} PERSIST object hantu ke tabel {@code sekolah}.</p>
	 *
	 * <p><b>Efek lanjutan penting:</b> nilai yang disetel di sini akan menentukan hasil
	 * {@link #getYayasan()}, yang menimpa field {@code yayasan} dengan yayasan milik sekolah ini.
	 * Menyetel sekolah karenanya secara tidak langsung juga mengubah kolom {@code yayasan_id}.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau object tanpa id diperlakukan sebagai
	 *        "kosong"
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Mengembalikan {@link Yayasan} pemilik — dimensi cakupan tenant tingkat atas.
	 *
	 * <p><b>Ini getter penulis-balik yang destruktif, kuirk terpenting di kelas ini.</b> Alurnya:</p>
	 * <ol>
	 * <li>Bila field {@code sekolah} tidak {@code null}, field {@code yayasan} <b>ditimpa</b>
	 * dengan {@code sekolah.getYayasan()} — apa pun yang sebelumnya diberikan lewat
	 * {@link #setYayasan(Yayasan)} <b>hilang</b>. Yayasan di sini karenanya bukan data yang
	 * berdiri sendiri melainkan <b>nilai turunan</b> dari sekolah.</li>
	 * <li>Hasilnya baru dilewatkan {@link GeneralValueObject#check(Object)} untuk resolusi proxy,
	 * dan hasil {@code check()} juga ditulis balik.</li>
	 * </ol>
	 *
	 * <p><b>Konsekuensi persistensi:</b> Hibernate membaca properti lewat getter saat <i>dirty
	 * check</i>. Bila kolom {@code yayasan_id} di database berbeda dari yayasan sekolahnya (data
	 * lama, hasil impor, atau sekolah yang dipindahkan antar-yayasan), sekadar <b>membaca</b>
	 * entity yang masih <i>attached</i> akan menghasilkan {@code UPDATE} pada {@code yayasan_id},
	 * memicu hook {@link #onUpdate()}, dan — karena kelas ini {@code @Audited} — menciptakan satu
	 * baris revisi Envers baru. Sisi positifnya, kolom ini praktis self-healing dan mustahil
	 * melenceng dalam jangka panjang.</p>
	 *
	 * <p><b>Risiko {@code LazyInitializationException}:</b> field {@code sekolah} diakses
	 * <b>langsung</b> di sini, tanpa melewati {@link #getSekolah()} yang akan meresolusi proxy-nya
	 * lebih dulu. Bila {@code sekolah} masih berupa proxy lazy yang belum terinisialisasi dan
	 * sesinya sudah tertutup, pemanggilan {@code sekolah.getYayasan()} dapat melempar
	 * {@code LazyInitializationException}. Memanggil {@link #getSekolah()} <i>sebelum</i> method
	 * ini menghindarkan masalah tersebut.</p>
	 *
	 * @return yayasan pemilik, diturunkan dari sekolah bila ada; {@code null} bila keduanya kosong
	 * @see #getSekolah()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan_id")
	public Yayasan getYayasan() {
		if (sekolah != null) {
			yayasan = sekolah.getYayasan();
		}
		yayasan = check(yayasan);
		return this.yayasan;
	}

	/**
	 * Menyetel yayasan pemilik, dengan normalisasi "kosong menjadi {@code null}" yang sama seperti
	 * {@link #setSekolah(Sekolah)} (object {@link Yayasan} tanpa id dianggap kosong).
	 *
	 * <p><b>Peringatan:</b> nilai yang disetel di sini <b>tidak bertahan</b> selama field
	 * {@code sekolah} terisi — {@link #getYayasan()} akan menimpanya dengan yayasan milik sekolah
	 * pada pembacaan berikutnya. Setter ini hanya benar-benar berpengaruh untuk baris yang tidak
	 * punya sekolah. {@code KurikulumSekolahAction.onSave()} tetap memanggilnya (dan memvalidasi
	 * yayasan wajib dipilih), tetapi combobox-nya {@code readonly} dan sudah dipaksa mengikuti
	 * sekolah terpilih, sehingga dalam praktik nilainya selalu identik dengan hasil turunannya.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau object tanpa id diperlakukan sebagai
	 *        "kosong"
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Mengembalikan keterangan bebas kurikulum (kolom {@code keterangan}).
	 *
	 * <p>Diisi pengguna lewat {@code Textbox} multi-baris pada formulir master dan ditampilkan apa
	 * adanya sebagai satu kolom grid. Tidak punya makna bisnis apa pun — tidak pernah diurai,
	 * dicocokkan, atau dipakai sebagai kunci.</p>
	 *
	 * <p><b>Berbeda dari kelas induk:</b> {@code GeneralValueObject.getKeterangan()} dirancang
	 * untuk tidak pernah mengembalikan {@code null} (mengembalikan {@code ""}), sedangkan override
	 * di sini mengembalikan isi kolom apa adanya sehingga <b>boleh {@code null}</b>. Konsumen yang
	 * mengandalkan kontrak kelas induk — termasuk cabang terakhir
	 * {@link GeneralValueObject#compareTo(GeneralValueObject)} — akan berperilaku berbeda untuk
	 * entity ini. {@code KurikulumSekolahRenderer} sendiri menyerahkan nilainya langsung ke
	 * {@code new Label(...)}, yang aman terhadap {@code null}.</p>
	 *
	 * @return keterangan; boleh {@code null}
	 */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas. Tanpa validasi maupun pemangkasan spasi.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan nama kurikulum, misalnya "Kurikulum 2013 Revisi" atau "Kurikulum Merdeka".
	 *
	 * <p>Kolomnya {@code nullable = false}. Ini juga <b>label tampil</b> entity: dipakai
	 * {@code GeneralValueObject.toString()}, judul dialog revisi
	 * ({@code RevisiHelper.createNewRevisi}), isi combobox pada {@code JenisNilaiSiswaAction} dan
	 * {@code KelasSiswaAction}, serta menjadi <b>kunci urut efektif</b>
	 * {@link GeneralValueObject#compareTo(GeneralValueObject)} untuk entity ini (lihat catatan
	 * TreeSet pada dokumentasi kelas).</p>
	 *
	 * <p><b>Tidak dijamin unik</b> — tidak ada unique constraint, dan dua sekolah berbeda lazim
	 * memakai nama kurikulum yang sama. Jangan pakai nama sebagai kunci pencocokan lintas
	 * sekolah.</p>
	 *
	 * <p><b>Nilai ini terekspos ke publik.</b> Kelas ini tidak meng-override {@code toString()},
	 * sehingga {@code GeneralValueObject.toString()} yang berlaku — dan karena properti
	 * {@code kode} tidak dipetakan untuk entity ini, hasilnya praktis sama dengan {@code nama}
	 * saja. {@code ais.common.home.WebsitePageService} mencetak hasil {@code toString()} itu ke
	 * bagian "Kurikulum dan pembelajaran" pada halaman program di <b>situs web publik</b> (dengan
	 * teks pengganti "Informasi kurikulum sedang disiapkan." bila kelas belum punya kurikulum).
	 * Jangan pernah menaruh catatan internal pada {@code nama}.</p>
	 *
	 * @return nama kurikulum; secara skema tidak pernah {@code null} untuk baris tersimpan
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menyetel nama kurikulum. Tanpa validasi di level entity.
	 *
	 * <p>Validasi "wajib diisi" ada di {@code KurikulumSekolahAction.onSave()}, bukan di sini —
	 * jalur non-UI (impor Excel, REST, migrasi) bisa menembus dan menabrak constraint
	 * {@code NOT NULL} di database.</p>
	 *
	 * @param nama nama kurikulum
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan status aktif kurikulum, dengan {@code null} dibaca sebagai {@code true}.
	 *
	 * <p>Getter ini <b>tidak menulis balik ke field</b> (berbeda dari {@link #getYayasan()}); ia
	 * hanya menormalkan nilai kembalian. Konsekuensinya kolom {@code aktif} di database boleh tetap
	 * {@code NULL} sementara Java selalu melaporkan {@code true}.</p>
	 *
	 * <p><b>Aturan untuk penulis query:</b> filter SQL/Criteria harus meniru pembacaan ini, yaitu
	 * {@code Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))}.
	 * Menulis {@code eq("aktif", true)} saja akan <b>menghilangkan senyap</b> semua kurikulum yang
	 * kolomnya masih {@code NULL}.</p>
	 *
	 * <h4>Divergensi yang benar-benar terpicu di codebase</h4>
	 * <p>Aturan di atas dipatuhi mayoritas pemanggil — {@code DasborAkademikSekolah},
	 * {@code DasboardJadwalPelajaran}, {@code DashboardKurikulumSekolah},
	 * {@code GenericCrudRelationLookupService}, {@code DynamicJspCrudGenerator}, dan JSP statistik
	 * kurikulum semuanya menulis disjungsi {@code isNull OR eq(true)}. <b>Dua combobox
	 * melanggarnya</b>:</p>
	 * <ul>
	 * <li>{@code KelasSiswaAction} — pemilih kurikulum saat membuat/mengubah rombel;</li>
	 * <li>{@code JenisNilaiSiswaAction} — pemilih kurikulum saat menyetel komponen nilai;</li>
	 * </ul>
	 * <p>keduanya memakai {@code Restrictions.and(Restrictions.eq("aktif", true),
	 * Restrictions.eq("sekolah", ...))} yang dievaluasi sebagai SQL sungguhan. Kurikulum dengan
	 * {@code aktif IS NULL} karena itu <b>tidak muncul</b> di dua dropdown itu, walaupun layar
	 * master menampilkan checkbox "Aktif"-nya tercentang dan semua dasbor menghitungnya aktif.
	 * Gejalanya di lapangan: "kurikulum ada di daftar master tapi tidak bisa dipilih saat membuat
	 * kelas".</p>
	 *
	 * <h4>Mengapa gejala itu sulit direproduksi</h4>
	 * <p>Entity ini memakai <i>property access</i> (anotasi {@code @Id} ada pada getter), sehingga
	 * Hibernate membaca nilai properti lewat method ini — termasuk saat INSERT dan saat <i>dirty
	 * check</i>. Akibatnya: (a) baris yang lahir lewat layar master selalu tersimpan {@code true},
	 * jadi baris {@code NULL} hanya datang dari SQL langsung, impor Excel
	 * ({@code Common.uploadData}), atau migrasi; dan (b) baris {@code NULL} yang terlanjur ada akan
	 * <b>menyembuhkan dirinya sendiri</b> — snapshot muatnya {@code null} sedangkan getter melapor
	 * {@code true}, sehingga flush berikutnya yang menyentuh baris itu memancarkan
	 * {@code UPDATE ... SET aktif = true}. Bug-nya karena itu bisa "hilang sendiri" tanpa ada yang
	 * memperbaiki apa pun.</p>
	 *
	 * <p><b>Divergensi ketiga (tampilan):</b> JSP statistik kurikulum merender status dengan
	 * {@code row.aktif ? "Aktif" : "Tidak Aktif"} di sisi JavaScript, sehingga nilai {@code NULL}
	 * tampil sebagai <b>"Tidak Aktif"</b> — berlawanan dengan getter ini dan dengan grid master.
	 * Tiga lapis (Java, Criteria, JSP) karena itu bisa memberi tiga jawaban berbeda untuk baris
	 * yang sama.</p>
	 *
	 * <p>Kuirk dasarnya identik dengan {@code ais.database.model.Kurikulum#getAktif()} versi PT.</p>
	 *
	 * @return {@code true} bila aktif atau belum pernah diset; {@code false} hanya bila memang
	 *         dinonaktifkan secara eksplisit
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif kurikulum.
	 *
	 * <p>Satu-satunya pemanggil dari UI adalah checkbox "Aktif" pada <b>grid daftar</b>
	 * ({@code KurikulumSekolahRenderer}), yang setiap kali dicentang/dilepas langsung diikuti
	 * {@code Common.refreshSaveOrUpdate(...)} — jadi perubahannya tersimpan seketika tanpa tombol
	 * Simpan. Formulir tambah/ubah tidak memuat bidang ini sama sekali, sehingga kurikulum baru
	 * selalu lahir dengan field {@code aktif} bernilai {@code null} yang dibaca {@code true} oleh
	 * {@link #getAktif()}.</p>
	 *
	 * @param aktif status aktif; {@code null} akan dibaca sebagai {@code true} oleh
	 *        {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan {@link JenisPenilaian} yang dipaksakan kurikulum ini — atau {@code null} bila
	 * kurikulum menyerahkan keputusan ke masing-masing mata pelajaran.
	 *
	 * <p>Relasi {@code @ManyToOne} LAZY ke kolom {@code jenis_penilaian_id}, dengan resolusi proxy
	 * lewat {@link GeneralValueObject#check(Object)} yang hasilnya ditulis balik ke field.</p>
	 *
	 * <p><b>Semantik {@code null} = "ikuti mata pelajaran".</b> Nilai di sini berperan sebagai
	 * <b>override tingkat kurikulum</b> atas skema penilaian yang sudah dipilih per
	 * {@link Matapelajaran}. Pola pemakaiannya seragam di seluruh jalur nilai — mula-mula ambil
	 * skema mata pelajaran, lalu <i>timpa</i> bila kurikulum menentukan sendiri:</p>
	 * <pre>
	 * JenisPenilaian jenisPenilaian = kpm.getMatapelajaran().getJenisPenilaian();
	 * if (kpm != null &amp;&amp; kpm.getKurikulumSekolah() != null
	 *         &amp;&amp; kpm.getKurikulumSekolah().getJenisPenilaian() != null) {
	 *     jenisPenilaian = kpm.getKurikulumSekolah().getJenisPenilaian();
	 * }
	 * </pre>
	 * <p>Blok itu <b>disalin-tempel di sekitar 25 titik</b> — {@code LaporanRekapTotalNilai} (13
	 * titik), {@code LaporanRaporSiswa} (4), {@code ElearningApiUtil} (2),
	 * {@code DetailPenilaianSiswaHelper} (2), plus {@code CommonUiFactoryHelper},
	 * {@code TugasMandiriHelper}, {@code TugasKelompokHelper},
	 * {@code PertemuanPunyaUjianSiswaHelper}, {@code TampilStudiSiswaHelper}, dan
	 * {@code NilaiSiswaApi}. Sesuai label combobox pada layar master:
	 * "{@code ==Ikuti jenis penilaian matapelajaran==}". Tidak ada utilitas bersama yang dipakai
	 * mayoritas pemanggil; bentuk kanoniknya ada di
	 * {@code CommonUiFactoryHelper.getDetailJenisPenilaians(JadwalPelajaran)}, tetapi hampir
	 * semuanya menulis ulang sendiri.</p>
	 *
	 * <p><b>Dua cacat tercatat pada pemanggil (bukan pada kelas ini):</b></p>
	 * <ol>
	 * <li><i>Penjaga null yang tidak pernah menjaga.</i> Pada varian yang dipakai
	 * {@code LaporanRaporSiswa} dan {@code LaporanRekapTotalNilai} — persis seperti potongan di
	 * atas — {@code kpm} sudah di-<i>dereference</i> di baris pertama <b>sebelum</b> diperiksa
	 * {@code kpm != null} di baris kedua. Bila {@code kpm} benar-benar {@code null},
	 * {@code NullPointerException} sudah terjadi lebih dulu. Bentuk kanonik di
	 * {@code CommonUiFactoryHelper} tidak punya cacat ini karena mengambil nilai awalnya dari
	 * {@code jadwalPelajaran}, bukan dari {@code kpm}.</li>
	 * <li><i>Dua jalur resolusi yang bisa berbeda.</i> {@code NilaiSiswaApi} tidak lewat
	 * {@link KurikulumPunyaMatapelajaran} melainkan langsung
	 * {@code kelasSiswa.getKurikulumSekolah()}. Keduanya biasanya menunjuk kurikulum yang sama,
	 * tetapi akan <b>menyimpang</b> bila sebuah {@link JadwalPelajaran} mengacu ke
	 * {@code KurikulumPunyaMatapelajaran} dari kurikulum selain kurikulum kelasnya — REST mobile
	 * dan laporan web bisa memakai skema penilaian berbeda untuk siswa yang sama.</li>
	 * </ol>
	 *
	 * <p>Hasil akhir fallback ini <b>masih boleh {@code null}</b> (bila kurikulum maupun mata
	 * pelajaran sama-sama tidak menentukan). Mayoritas pemanggil langsung menyuapkannya ke
	 * {@code Restrictions.eq("jenisPenilaian", ...)}, yang menghasilkan daftar kosong alih-alih
	 * error; hanya {@code CommonUiFactoryHelper} dan {@code TampilStudiSiswaHelper} yang
	 * memutusnya secara eksplisit lebih dulu.</p>
	 *
	 * <p>Pilihan combobox pada layar master disaring ke {@link JenisPenilaian} milik sekolah yang
	 * sedang dipilih <i>atau</i> yang bersifat global ({@code sekolah IS NULL}), dan hanya yang
	 * aktif. Dasbor {@code DashboardKurikulumSekolah} memperlakukan {@code null} sebagai kategori
	 * tersendiri berlabel "Tanpa Jenis Penilaian" saat membuat tabulasi silang
	 * sekolah &times; jenis penilaian.</p>
	 *
	 * @return skema penilaian yang memaksa seluruh mata pelajaran kurikulum ini; {@code null} bila
	 *         tiap mata pelajaran memakai skemanya masing-masing
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_penilaian_id")
	public JenisPenilaian getJenisPenilaian() {
		jenisPenilaian = check(jenisPenilaian);
		return this.jenisPenilaian;
	}

	/**
	 * Menyetel skema penilaian tingkat kurikulum.
	 *
	 * <p>Berbeda dari {@link #setSekolah(Sekolah)} dan {@link #setYayasan(Yayasan)}, setter ini
	 * <b>tidak</b> menormalkan object tanpa id menjadi {@code null} — nilai disimpan apa adanya.
	 * {@code KurikulumSekolahAction.onSave()} sudah menerjemahkan item combobox "Ikuti jenis
	 * penilaian matapelajaran" menjadi {@code null} sebelum memanggil method ini, tetapi pemanggil
	 * baru (impor, REST, migrasi) harus melakukan normalisasi itu sendiri agar Hibernate tidak
	 * mencoba meng-{@code cascade} PERSIST object hantu ke tabel {@code jenis_penilaian}.</p>
	 *
	 * @param jenisPenilaian skema penilaian; {@code null} berarti "ikuti mata pelajaran"
	 * @see #getJenisPenilaian()
	 */
	public void setJenisPenilaian(JenisPenilaian jenisPenilaian) {
		this.jenisPenilaian = jenisPenilaian;
	}

}
