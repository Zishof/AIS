package ais.database.model;

// Generated Dec 12, 2009 3:35:45 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.io.File;
import java.util.Date;
import java.util.Map;

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
import ais.database.model.file.FileFoto;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;

/**
 * Entity master <b>fakultas</b> (tabel {@code public.fakultas}) — satuan organisasi akademik yang
 * menaungi sekumpulan jurusan/program studi.
 *
 * <h2>Posisi dalam hierarki institusi</h2>
 *
 * <p>Rantai organisasi AIS berbentuk
 * {@link PerguruanTinggi} &rarr; <b>{@code Fakultas}</b> &rarr; {@link Jurusan}. Kelas ini berada
 * persis di tengah: ia menunjuk ke atas lewat {@link #getPerguruanTinggi()}, dan ditunjuk dari
 * bawah oleh {@link Jurusan#getFakultas()} (relasi itu {@code nullable = false}, jadi setiap
 * prodi wajib menempel pada sebuah fakultas).</p>
 *
 * <p><b>Perhatikan arah relasinya.</b> Kelas ini <b>tidak</b> punya koleksi
 * {@code List&lt;Jurusan&gt;}. Relasi fakultas&ndash;jurusan hanya dipetakan satu arah, dari sisi
 * {@link Jurusan}. Untuk memperoleh daftar prodi sebuah fakultas, kode pemanggil harus melakukan
 * query sendiri (mis. {@code Restrictions.eq("fakultas", fakultas)} pada {@code Jurusan}); tidak
 * ada cara "menavigasi ke bawah" lewat object ini. Akibat lain: menghapus fakultas tidak
 * meng-cascade apa pun ke jurusan.</p>
 *
 * <h2>Peran sebagai rujukan lintas modul</h2>
 *
 * <p>Bersama {@link Jurusan}, kelas ini termasuk entity paling banyak dirujuk di seluruh basis
 * kode: sekitar <b>97 kelas entity</b> lain di paket {@code ais.database.model} punya field
 * {@code private Fakultas ...}, dan nama kelasnya disebut di sekitar <b>900+ berkas sumber</b>.
 * Fakultas dipakai antara lain untuk:</p>
 *
 * <ul>
 *   <li><b>Penyaringan data dan hak akses.</b> Banyak layar daftar dan laporan menyaring "per
 *   fakultas", dan hak akses operator kerap dibatasi per fakultas — sehingga mengubah relasi
 *   fakultas sebuah data berdampak pada <i>siapa yang boleh melihatnya</i>, bukan sekadar
 *   tampilan.</li>
 *   <li><b>Pencetakan dokumen.</b> Nama fakultas, nama/NIP dekan dan para pembantu dekan, label
 *   pejabat, kop surat, dan stempel diambil dari sini untuk surat keputusan, transkrip, ijazah,
 *   album wisuda, dan berbagai laporan JasperReports (lihat {@link #putFile(java.util.Map)}).</li>
 *   <li><b>Penganggaran.</b> Lewat {@link #getSatuanKerja()} dan
 *   {@link #getDosenHarusPakaiSatuanKerja()}, fakultas menjadi salah satu tingkat penentu unit
 *   anggaran seorang dosen.</li>
 *   <li><b>Pelaporan PDDIKTI.</b> {@link #getFeeder()} menyimpan pengenal fakultas di sistem
 *   Feeder dan dipakai saat pencocokan data impor.</li>
 * </ul>
 *
 * <p>Konsekuensi praktisnya: perubahan perilaku getter di kelas ini terasa di sangat banyak layar
 * sekaligus. Perlakukan setiap suntingan di sini sebagai perubahan berdampak luas.</p>
 *
 * <h2>Pengelompokan method</h2>
 *
 * <ol>
 *   <li><b>Identitas &amp; penamaan.</b> {@link #getId()}, {@link #getKode()},
 *   {@link #getNama()}, {@link #getNamaEn()} (nama Inggris untuk dokumen dwibahasa),
 *   {@link #getFeeder()} (pengenal di Feeder PDDIKTI), {@link #getDeskripsi()}.</li>
 *   <li><b>Struktur organisasi.</b> {@link #getPerguruanTinggi()} (induk),
 *   {@link #getSatuanKerja()} (unit anggaran), {@link #getDosenHarusPakaiSatuanKerja()}.</li>
 *   <li><b>Pejabat — dua rangkaian terpisah.</b> Rangkaian <i>struktural akademik</i> berisi
 *   {@link Dosen}: {@link #getDekan()}, {@link #getPudek1()}, {@link #getPudek2()},
 *   {@link #getPudek3()} (pembantu/wakil dekan I&ndash;III). Rangkaian <i>bebas</i> berisi
 *   {@link Pegawai}: {@link #getPegawai1()}..{@link #getPegawai3()} dengan judul jabatan yang
 *   bisa diganti pengguna lewat {@link #getLabelPejabat1()}..{@link #getLabelPejabat3()}.
 *   Keduanya berdiri sendiri dan diisi berdampingan pada layar yang sama.</li>
 *   <li><b>Tampilan.</b> {@link #getWarna()} (warna hex penanda fakultas di layar) dan
 *   {@link #getRgb()} (turunannya dalam bentuk {@code "[r,g,b]"}), {@link #getAktif()},
 *   {@link #getWa()} (nomor WhatsApp kontak).</li>
 *   <li><b>Audit.</b> {@link #getOleh()}, {@link #getOlehId()}, {@link #getTanggal_dirubah()},
 *   dan kait {@link #onUpdate()}; seluruh perubahan baris juga direkam Hibernate Envers karena
 *   kelas ini beranotasi {@link org.hibernate.envers.Audited}.</li>
 *   <li><b>Lampiran cetak.</b> {@link #putFile(java.util.Map)} — satu-satunya method non-accessor
 *   di kelas ini.</li>
 * </ol>
 *
 * <h2>Hal-hal non-obvious yang wajib diketahui sebelum menyunting</h2>
 *
 * <p><b>1. Pemetaan memakai akses property.</b> Anotasi JPA ({@link javax.persistence.Id},
 * {@link javax.persistence.Column}, {@link javax.persistence.ManyToOne}) dipasang pada
 * <i>getter</i>, sehingga Hibernate membaca dan menulis kolom lewat getter, bukan lewat field.
 * Setiap pasangan getter/setter publik tanpa {@code @Transient} ikut dipetakan — termasuk yang
 * tidak beranotasi sama sekali ({@code namaEn}, {@code feeder}, {@code warna}, {@code rgb},
 * {@code aktif}, {@code labelPejabat1..3}, {@code wa},
 * {@code dosenHarusPakaiSatuanKerja}), yang memakai nama property sebagai nama kolom. Artinya
 * semua "normalisasi" di dalam getter di bawah bukan sekadar kosmetik tampilan: nilai itulah yang
 * dipakai saat {@code INSERT} dan saat entity <i>detached</i> disimpan ulang.</p>
 *
 * <p><b>2. Getter yang menulis balik ke field ("normalisasi diam-diam").</b> Pola yang sudah
 * dikenali di {@link Jurusan} berulang di sini, dengan empat getter yang <b>mengubah state
 * object</b> saat dipanggil:</p>
 * <ul>
 *   <li>{@link #getNama()} menyulih {@code null} menjadi {@code ""};</li>
 *   <li>{@link #getDeskripsi()} menyulih {@code null} menjadi {@code ""};</li>
 *   <li>{@link #getWarna()} menyulih nilai kosong dengan warna default {@code "#3300ff"};</li>
 *   <li>{@link #getRgb()} <b>selalu</b> menimpa {@code rgb} dengan hasil hitungan dari
 *   {@link #getWarna()} — nilai apa pun yang tersimpan di kolom {@code rgb} akan tergantikan.</li>
 * </ul>
 * <p>Karena pemetaan property-access, nilai sulihan itu ikut tertulis ke database pada
 * penyimpanan berikutnya ({@code dynamicInsert = true} hanya melewatkan nilai {@code null}, jadi
 * {@code ""} dan {@code "#3300ff"} tetap ditulis). Berbeda dengan entity biodata, tidak ada
 * getter di kelas ini yang menjalankan {@code save}/{@code insert} sendiri ke tabel master lain.
 * </p>
 *
 * <p><b>3. Getter yang bisa memicu query dan membuka session Hibernate.</b> Delapan getter relasi
 * ({@link #getDekan()}, {@link #getPudek1()}..{@link #getPudek3()},
 * {@link #getPerguruanTinggi()}, {@link #getSatuanKerja()},
 * {@link #getPegawai1()}..{@link #getPegawai3()}) memanggil
 * {@link GeneralValueObject#check(Object)} untuk meresolusi proxy lazy. Pada kasus terburuk
 * {@code check()} membuka session Hibernate <i>miliknya sendiri</i> dan membaca ulang entity dari
 * database — jadi getter yang tampak sepele dapat menyentuh database. Session milik pemanggil
 * <b>tidak</b> ditutup oleh {@code check()}; session yang dibuka {@code check()} sendiri ditutup
 * olehnya sendiri secara diam-diam. Rinciannya di {@link GeneralValueObject#check(Object)}.</p>
 *
 * <p><b>4. {@link #getPerguruanTinggi()} mengisi dirinya sendiri dari konteks request.</b> Ini
 * perilaku paling mengejutkan di kelas ini dan tidak ada padanannya di {@link Jurusan}: bila
 * relasi induk masih kosong, getter memanggil
 * {@code PerguruanTinggiUtil.getPerguruanTinggi()} yang menebak perguruan tinggi dari pengguna
 * yang sedang login / nama domain request, lalu <b>menugaskan hasilnya ke field</b>. Lihat
 * peringatan lengkap di {@link #getPerguruanTinggi()}.</p>
 *
 * <p><b>5. Field bayangan.</b> {@code id}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} <b>dideklarasikan ulang</b> di kelas ini padahal namanya sama dengan
 * yang ada di {@link GeneralValueObject}. Ini <b>bukan kelalaian</b>: {@link GeneralValueObject}
 * bukan {@code @Entity} maupun {@code @MappedSuperclass}, melainkan POJO abstrak biasa, sehingga
 * Hibernate sama sekali tidak memetakan property milik kelas induk. Deklarasi ulang di sini
 * adalah <b>keharusan teknis</b> supaya kolom-kolom itu ikut dipetakan. Yang perlu diingat: kode
 * di {@link GeneralValueObject} yang membaca field-field itu <i>langsung</i> (tanpa getter) akan
 * membaca salinan milik induk yang selamanya {@code null} untuk instance {@code Fakultas}.</p>
 *
 * <p><b>6. Nilai default yang tidak pernah {@code null}.</b> Sebagian besar getter mengembalikan
 * nilai pengganti saat field kosong ({@link #getAktif()} &rarr; {@code true},
 * {@link #getDosenHarusPakaiSatuanKerja()} &rarr; {@code false}, {@link #getWarna()} &rarr;
 * {@code "#3300ff"}, label pejabat &rarr; "Pejabat I/II/III", {@link #getWa()} &rarr; {@code ""},
 * {@link #getNamaEn()} &rarr; isi {@link #getNama()}). Konsekuensinya pemanggil <b>tidak bisa
 * membedakan</b> "belum pernah diisi" dari "diisi persis sama dengan default"; kalau perbedaan
 * itu penting, bacalah kolomnya lewat query, bukan lewat getter. Kebalikannya berlaku untuk
 * {@link #getFeeder()} dan {@link #getPerguruanTinggi()}, yang justru menormalkan nilai kosong
 * menjadi {@code null}.</p>
 *
 * <p><b>7. {@link #toString()} menyimpang dari induk.</b> Bentuknya hanya nama fakultas (bukan
 * {@code "kode - nama"} seperti {@link GeneralValueObject#toString()}) dan membaca <i>field</i>
 * {@code nama} secara langsung — bukan {@link #getNama()} — sehingga bisa mengembalikan
 * {@code null} pada instance yang {@link #getNama()}-nya belum pernah dipanggil. Beberapa
 * komponen UI dan operasi perangkaian String menoleransi ini (menampilkan "null"), sebagian lain
 * tidak.</p>
 *
 * <p><b>8. Identitas.</b> {@code equals()}/{@code compareTo()} diwarisi dari
 * {@link GeneralValueObject} (berbasis {@code id}, dengan {@code hashCode()} yang tidak
 * di-override di seluruh hierarki) — lihat peringatan lengkapnya di
 * {@link GeneralValueObject#equals(Object)}.</p>
 *
 * @see GeneralValueObject
 * @see PerguruanTinggi
 * @see Jurusan
 * @see ais.action.master.FakultasAction
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "fakultas")

public class Fakultas extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Entity ini ikut diserialisasi ketika disimpan pada session
	 * ZK atau dikirim antar-node, sehingga nilai tetap ini menjaga kecocokan antar-versi kelas.
	 *
	 * <p>Jangan diubah tanpa alasan kuat: mengganti nilainya membuat object yang sudah
	 * terserialisasi (mis. di session yang sedang berjalan) tidak bisa dibaca lagi.</p>
	 */
	private static final long serialVersionUID = 5021327183727932240L;
	/**
	 * Kunci utama baris fakultas (kolom {@code id}, {@code IDENTITY}). Lihat {@link #getId()}.
	 *
	 * <p>Dideklarasikan ulang dari {@link GeneralValueObject} karena kelas induk tidak dipetakan
	 * Hibernate — lihat catatan "field bayangan" pada Javadoc kelas.</p>
	 */
	private Long id;
	/**
	 * Nama pengguna terakhir yang mengubah baris ini. Lihat {@link #getOleh()}.
	 *
	 * <p>Field audit bayangan; lihat catatan "field bayangan" pada Javadoc kelas.</p>
	 */
	private String oleh;
	/**
	 * Pengenal (id) pengguna terakhir yang mengubah baris ini. Lihat {@link #getOlehId()}.
	 *
	 * <p>Field audit bayangan; lihat catatan "field bayangan" pada Javadoc kelas.</p>
	 */
	private String olehId;

	/**
	 * Mengembalikan pengenal pengguna terakhir yang mengubah baris fakultas ini.
	 *
	 * <p>Diisi otomatis oleh {@link ais.database.hibernate.AuditTimestampInterceptor} lewat
	 * {@link #onUpdate()}, bukan oleh layar. Nilainya bisa {@code null} untuk baris lama yang
	 * belum pernah disunting sejak fitur audit aktif.</p>
	 *
	 * @return id pengguna penyunting terakhir, atau {@code null} bila belum pernah terisi
	 * @see #getOleh()
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel pengenal pengguna penyunting terakhir.
	 *
	 * <p><b>Perhatian:</b> setter ini <b>menolak diam-diam</b> nilai {@code null} maupun String
	 * kosong/berisi spasi — nilai lama dipertahankan dan tidak ada exception yang dilempar.
	 * Artinya jejak audit tidak bisa "dikosongkan" lewat setter ini, hanya bisa ditimpa dengan
	 * nilai baru yang tidak kosong.</p>
	 *
	 * @param olehId id pengguna penyunting; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna penyunting terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null} atau kosong <b>diabaikan
	 * diam-diam</b> sehingga nilai lama bertahan.</p>
	 *
	 * @param oleh nama pengguna penyunting; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris fakultas ini.
	 *
	 * @return nama penyunting terakhir, atau {@code null} bila belum pernah terisi
	 * @see #getOlehId()
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@link javax.persistence.PreUpdate}: dipanggil Hibernate <b>tepat sebelum</b>
	 * pernyataan {@code UPDATE} baris fakultas ini dijalankan.
	 *
	 * <p>Seluruh isinya didelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}, yang mengisi
	 * {@link #setOleh(String)}/{@link #setOlehId(String)} dari pengguna yang sedang login dan
	 * memperbarui {@link #setTanggal_dirubah(Date)}. Karena itu ketiga nilai audit tidak perlu
	 * (dan sebaiknya tidak) diisi manual oleh layar.</p>
	 *
	 * <p>Perhatikan bahwa kait ini hanya berjalan pada {@code UPDATE}, tidak pada {@code INSERT};
	 * baris baru mengandalkan nilai awal field {@code tanggal_dirubah} yang disetel saat object
	 * dibuat.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir. Lihat {@link #getTanggal_dirubah()}.
	 *
	 * <p>Diinisialisasi ke waktu saat object dibuat lewat {@code ais.ui.util.WaktuUtil.getDate()}
	 * (waktu server sesuai zona waktu aplikasi), sehingga baris baru selalu punya nilai walau
	 * kait {@link #onUpdate()} belum pernah jalan.</p>
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya dipanggil oleh {@link ais.database.hibernate.AuditTimestampInterceptor} lewat
	 * {@link #onUpdate()}, bukan oleh layar. Tidak ada validasi: nilai {@code null} diterima apa
	 * adanya.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris fakultas ini.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk object yang baru dibuat
	 *         di memori, tetapi bisa {@code null} bila kolomnya kosong di database
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks fakultas berupa <b>nama fakultas saja</b>.
	 *
	 * <p>Meng-override {@link GeneralValueObject#toString()} yang berbentuk
	 * {@code "kode - nama"}. Dipakai luas oleh komponen ZK (combobox/listbox yang menampilkan
	 * object langsung) dan oleh perangkaian String di berbagai laporan.</p>
	 *
	 * <p><b>Jebakan:</b> method ini membaca <i>field</i> {@code nama} secara langsung, bukan
	 * {@link #getNama()}. Pada instance yang {@link #getNama()}-nya belum pernah dipanggil dan
	 * kolom namanya kosong, hasilnya {@code null} — bukan String kosong. Bila hasilnya dipakai
	 * pada operasi yang tidak toleran terhadap {@code null}, pakailah {@link #getNama()}.</p>
	 *
	 * @return nama fakultas apa adanya, mungkin {@code null}
	 */
	public String toString() {
		return nama;
	}

	/** Kode singkat fakultas. Lihat {@link #getKode()}. */
	private String kode;
	/** Nama resmi fakultas. Lihat {@link #getNama()}. */
	private String nama;
	/** Nama fakultas dalam bahasa Inggris untuk dokumen dwibahasa. Lihat {@link #getNamaEn()}. */
	private String namaEn;
	/** Dosen yang menjabat dekan. Lihat {@link #getDekan()}. */
	private Dosen dekan;
	/** Dosen yang menjabat pembantu/wakil dekan I. Lihat {@link #getPudek1()}. */
	private Dosen pudek1;
	/** Dosen yang menjabat pembantu/wakil dekan II. Lihat {@link #getPudek2()}. */
	private Dosen pudek2;
	/** Dosen yang menjabat pembantu/wakil dekan III. Lihat {@link #getPudek3()}. */
	private Dosen pudek3;
	/**
	 * Perguruan tinggi induk. Lihat {@link #getPerguruanTinggi()} — getter-nya bisa mengisi
	 * field ini sendiri dari konteks request.
	 */
	private PerguruanTinggi perguruanTinggi;
	/** Unit anggaran (satuan kerja) fakultas. Lihat {@link #getSatuanKerja()}. */
	private SatuanKerja satuanKerja;
	/** Uraian bebas tentang fakultas. Lihat {@link #getDeskripsi()}. */
	private String deskripsi;

	/** Pengenal fakultas pada Feeder PDDIKTI. Lihat {@link #getFeeder()}. */
	private String feeder;
	/** Warna penanda fakultas dalam notasi hex {@code "#rrggbb"}. Lihat {@link #getWarna()}. */
	private String warna;
	/**
	 * Bentuk {@code "[r,g,b]"} dari {@link #warna}. Nilai turunan yang selalu dihitung ulang oleh
	 * {@link #getRgb()}; isi kolomnya di database tidak pernah dipercaya.
	 */
	private String rgb;

	/** Penanda fakultas masih aktif. Lihat {@link #getAktif()}. */
	private Boolean aktif;

	/** Judul jabatan untuk slot pejabat bebas ke-1. Lihat {@link #getLabelPejabat1()}. */
	private String labelPejabat1;
	/** Judul jabatan untuk slot pejabat bebas ke-2. Lihat {@link #getLabelPejabat2()}. */
	private String labelPejabat2;
	/** Judul jabatan untuk slot pejabat bebas ke-3. Lihat {@link #getLabelPejabat3()}. */
	private String labelPejabat3;

	/** Pegawai pengisi slot pejabat bebas ke-1. Lihat {@link #getPegawai1()}. */
	private Pegawai pegawai1;
	/** Pegawai pengisi slot pejabat bebas ke-2. Lihat {@link #getPegawai2()}. */
	private Pegawai pegawai2;
	/** Pegawai pengisi slot pejabat bebas ke-3. Lihat {@link #getPegawai3()}. */
	private Pegawai pegawai3;
	/** Nomor WhatsApp kontak fakultas. Lihat {@link #getWa()}. */
	private String wa;
	/**
	 * Penanda dosen fakultas ini wajib memakai satuan kerja. Lihat
	 * {@link #getDosenHarusPakaiSatuanKerja()}.
	 */
	private Boolean dosenHarusPakaiSatuanKerja;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA untuk membuat instance saat memuat baris
	 * dari database, dan dipakai layar saat menambah fakultas baru.
	 *
	 * <p>Seluruh field dibiarkan {@code null} kecuali {@link #tanggal_dirubah}, yang langsung
	 * diisi waktu saat ini oleh inisialisasi field.</p>
	 */
	public Fakultas() {
	}

	/**
	 * Konstruktor pintas yang hanya mengisi nama fakultas.
	 *
	 * <p>Bawaan hasil generate hbm2java. Berguna untuk membuat object sementara sebagai penanda
	 * tampilan (mis. baris "Semua Fakultas" pada combobox) tanpa menyentuh database. Object yang
	 * dibentuk begini <b>belum punya {@code id}</b>, sehingga {@code equals()} bawaan
	 * {@link GeneralValueObject} tidak akan menganggapnya sama dengan baris mana pun.</p>
	 *
	 * @param nama nama fakultas; disimpan apa adanya, tanpa normalisasi
	 */
	public Fakultas(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan kunci utama baris fakultas.
	 *
	 * <p>Kolom {@code id} bertipe {@code IDENTITY} dan dipetakan {@code insertable = false} —
	 * nilainya dibangkitkan database saat {@code INSERT} dan baru terisi setelah entity
	 * disimpan/di-flush. Jangan mengisinya sendiri untuk baris baru.</p>
	 *
	 * <p>Nilai inilah yang dipakai {@link GeneralValueObject#equals(Object)} dan
	 * {@code compareTo()}, serta menjadi kunci penyaringan "per fakultas" di seluruh
	 * aplikasi.</p>
	 *
	 * @return id fakultas, atau {@code null} untuk object yang belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama fakultas. Tanpa validasi.
	 *
	 * <p>Hanya boleh dipakai Hibernate atau kode yang sengaja merangkai object detached; mengubah
	 * id object yang sedang dikelola session akan membuat Hibernate memperlakukannya sebagai
	 * baris lain.</p>
	 *
	 * @param id id baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama resmi fakultas dalam bentuk yang sudah dipangkas spasi tepinya.
	 *
	 * <p><b>Efek samping:</b> bila field {@code nama} masih {@code null}, method ini
	 * <b>menuliskan String kosong ke field</b> lebih dulu. Karena kelas ini dipetakan dengan
	 * akses property, nilai kosong itu ikut tersimpan ke kolom {@code nama} pada penyimpanan
	 * berikutnya. Jadi memanggil getter ini pada entity yang dikelola session bukan operasi
	 * baca murni.</p>
	 *
	 * <p><b>Kuirk:</b> pemeriksaan {@code this.nama == null} pada baris {@code return} tidak
	 * pernah bernilai benar, karena baris sebelumnya sudah menjamin field tidak {@code null}.
	 * Sisa kode lama; dipertahankan apa adanya. Konsekuensinya method ini
	 * <b>tidak pernah mengembalikan {@code null}</b>.</p>
	 *
	 * @return nama fakultas terpangkas; String kosong bila belum diisi
	 */
	@Column(name = "nama", length = 150)
	public String getNama() {
		if (nama == null) {
			nama = "";
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama resmi fakultas. Tanpa validasi maupun pemangkasan — spasi tepi yang dikirim
	 * layar tersimpan apa adanya dan baru dipangkas saat dibaca lewat {@link #getNama()}.
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Menyetel kode singkat fakultas. Tanpa validasi maupun normalisasi.
	 *
	 * @param kode kode baru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan kode singkat fakultas apa adanya.
	 *
	 * <p>Berbeda dengan {@link Jurusan#getKode()} yang menyulih kode kosong dengan {@code "--"},
	 * getter ini <b>tidak</b> melakukan normalisasi apa pun: nilai {@code null} dikembalikan
	 * sebagai {@code null} dan spasi tepi tidak dipangkas. Pemanggil harus menyiapkan
	 * penanganannya sendiri.</p>
	 *
	 * @return kode fakultas, mungkin {@code null} atau kosong
	 */
	@Column(name = "kode")
	public String getKode() {
		return kode;
	}

	/**
	 * Menyetel dosen yang menjabat dekan. Tanpa validasi.
	 *
	 * @param dekan dosen dekan yang baru, boleh {@code null} untuk mengosongkan jabatan
	 */
	public void setDekan(Dosen dekan) {
		this.dekan = dekan;
	}

	/**
	 * Mengembalikan {@link Dosen} yang menjabat dekan fakultas ini.
	 *
	 * <p>Relasi {@code @ManyToOne} lazy ke kolom {@code dekan}. Sebelum dikembalikan, nilainya
	 * dilewatkan {@link GeneralValueObject#check(Object)} untuk meresolusi proxy lazy, dan
	 * <b>hasilnya ditugaskan kembali ke field</b> supaya resolusi itu tidak berulang. Pada kasus
	 * terburuk {@code check()} membuka session Hibernate sendiri dan memuat ulang dosen dari
	 * database, jadi getter ini bisa memicu query.</p>
	 *
	 * <p>Dekan adalah salah satu data fakultas yang paling banyak dipakai: namanya, NIP/NIDN-nya,
	 * dan tanda tangannya muncul di transkrip, ijazah, album wisuda, surat keputusan, dan
	 * berbagai laporan. Nilai {@code null} berarti jabatan belum diisi — pemanggil hampir selalu
	 * perlu memeriksanya sebelum memanggil {@code getNama()} pada hasilnya.</p>
	 *
	 * @return dosen dekan, atau {@code null} bila belum diisi atau proxy-nya gagal diresolusi
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dekan", nullable = true)
	public Dosen getDekan() {
		dekan = check(dekan);
		return dekan;
	}

	/**
	 * Mengembalikan {@link PerguruanTinggi} induk fakultas ini, <b>dengan pengisian otomatis dari
	 * konteks request bila relasinya masih kosong</b>.
	 *
	 * <h3>Alur</h3>
	 * <ol>
	 *   <li>Resolusi proxy lazy lewat {@link GeneralValueObject#check(Object)} (bisa membuka
	 *   session Hibernate sendiri dan membaca database).</li>
	 *   <li>Bila hasilnya masih {@code null}, panggil
	 *   {@code ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi()} — yang
	 *   menebak perguruan tinggi dari pengguna yang sedang login, cache {@code HttpSession},
	 *   pencocokan nama domain server, sampai akhirnya {@code perguruanTinggiDefault} statis.
	 *   Hasilnya <b>ditugaskan ke field</b> {@code perguruanTinggi}.</li>
	 *   <li>Kembalikan {@code null} bila hasil akhirnya {@code null} <i>atau</i> object hasil
	 *   tebakan belum punya {@code id} (mis. object default yang belum tersimpan).</li>
	 * </ol>
	 *
	 * <p><b>Peringatan — efek samping yang bisa menembus ke database.</b> Langkah 2 mengubah
	 * state object. Karena kelas ini dipetakan dengan akses property dan getter inilah yang
	 * dibaca Hibernate saat menyimpan, sebuah fakultas <b>baru</b> yang belum diisi induknya akan
	 * ikut menyimpan perguruan tinggi hasil tebakan konteks. Pada pemasangan multi-tenant, itu
	 * berarti fakultas menempel ke tenant yang aktif di request saat itu. Perhatikan juga jalur
	 * di luar request web (batch, penjadwal, impor Feeder): di sana tidak ada request maupun
	 * pengguna login, sehingga yang terpilih adalah <i>default statis</i> — bukan tentu tenant
	 * yang benar. Bila Anda perlu tahu apakah induknya benar-benar sudah diisi, jangan andalkan
	 * getter ini; bacalah kolom {@code perguruan_tinggi} lewat query.</p>
	 *
	 * <p>Perhatikan pula bahwa hasil {@code null} tidak selalu berarti field-nya kosong: bila
	 * tebakan menghasilkan object tanpa {@code id}, field tetap terisi object itu sementara
	 * method mengembalikan {@code null}.</p>
	 *
	 * <p>Blok {@code catch} di dalamnya sengaja senyap dan hanya mencatat lewat
	 * {@code ErrorAuditUtil} (penanda {@code auto-audit(empty-catch)} berasal dari inisiatif
	 * audit blok catch kosong, bukan dari inisiatif Javadoc ini) — kegagalan penebakan tidak
	 * boleh membuat getter entity gagal.</p>
	 *
	 * @return perguruan tinggi induk yang sudah punya {@code id}, atau {@code null}
	 * @see ais.action.master.helper.util.PerguruanTinggiUtil#getPerguruanTinggi()
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "perguruan_tinggi", nullable = true)
	public PerguruanTinggi getPerguruanTinggi() {
		perguruanTinggi = check(perguruanTinggi);
		try {
			if (perguruanTinggi == null) {
				perguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Fakultas.java:175");
		}
		return perguruanTinggi == null || perguruanTinggi.getId() == null ? null : perguruanTinggi;
	}

	/**
	 * Menyetel perguruan tinggi induk. Tanpa validasi.
	 *
	 * <p>Menyetel {@code null} tidak benar-benar "mengosongkan" relasi dari sudut pandang
	 * pembaca: pemanggilan {@link #getPerguruanTinggi()} berikutnya akan mengisinya lagi dari
	 * konteks request.</p>
	 *
	 * @param perguruanTinggi perguruan tinggi induk yang baru
	 */
	public void setPerguruanTinggi(PerguruanTinggi perguruanTinggi) {
		this.perguruanTinggi = perguruanTinggi;
	}

	/**
	 * Mengembalikan uraian bebas tentang fakultas (kolom {@code text}), dipakai pada profil
	 * fakultas di layar dan situs.
	 *
	 * <p><b>Efek samping:</b> bila field masih {@code null}, method ini <b>menulis String kosong
	 * ke field</b> sebelum mengembalikannya, sehingga nilai kosong itu ikut tersimpan pada
	 * penyimpanan berikutnya. Berbeda dengan {@link #getNama()}, isinya tidak dipangkas.</p>
	 *
	 * @return deskripsi fakultas; String kosong bila belum diisi, tidak pernah {@code null}
	 */
	@Column(name = "deskripsi", columnDefinition = "text")
	public String getDeskripsi() {
		if (deskripsi == null) {
			deskripsi = "";
		}
		return deskripsi;
	}

	/**
	 * Menyetel uraian bebas tentang fakultas. Tanpa validasi.
	 *
	 * @param deskripsi deskripsi baru
	 */
	public void setDeskripsi(String deskripsi) {
		this.deskripsi = deskripsi;
	}

	/**
	 * Mengembalikan pengenal fakultas pada <b>Feeder PDDIKTI</b> dalam bentuk sudah terpangkas.
	 *
	 * <p>Berbeda dengan kebanyakan getter di kelas ini, getter ini menormalkan nilai kosong
	 * menjadi {@code null} (bukan sebaliknya) dan <b>tidak</b> menulis balik ke field. Bentuk
	 * itu memang yang dibutuhkan pemanggilnya: proses impor Feeder
	 * ({@code FeederImporter}/{@code FeederJSONImport}) mencocokkan data dengan
	 * {@code Restrictions.eq("feeder", fakultas.getFeeder())}, dan kriteria dengan nilai
	 * {@code null} tidak akan salah mencocokkan fakultas yang sama-sama belum diisi.</p>
	 *
	 * @return pengenal Feeder terpangkas, atau {@code null} bila belum diisi atau berisi spasi
	 *         saja
	 */
	public String getFeeder() {
		return feeder == null || feeder.trim().isEmpty() ? null : feeder.trim();
	}

	/**
	 * Menyetel pengenal fakultas pada Feeder PDDIKTI. Tanpa validasi maupun pemangkasan.
	 *
	 * @param feeder pengenal Feeder yang baru
	 */
	public void setFeeder(String feeder) {
		this.feeder = feeder;
	}

	/**
	 * Mengembalikan {@link Dosen} yang menjabat pembantu/wakil dekan I (biasanya bidang
	 * akademik).
	 *
	 * <p>Sama seperti {@link #getDekan()}: relasi lazy yang diresolusi lewat
	 * {@link GeneralValueObject#check(Object)} dan ditugaskan kembali ke field, sehingga bisa
	 * memicu pembacaan database. Dipakai antara lain oleh pencetakan album wisuda dan surat
	 * resmi yang menampilkan nama, NIP, dan NIDN pejabat.</p>
	 *
	 * @return dosen pembantu dekan I, atau {@code null} bila belum diisi
	 * @see #getDekan()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pudek1", nullable = true)
	public Dosen getPudek1() {
		pudek1 = check(pudek1);
		return pudek1;
	}

	/**
	 * Menyetel dosen pembantu/wakil dekan I. Tanpa validasi.
	 *
	 * @param pudek1 dosen baru, boleh {@code null}
	 */
	public void setPudek1(Dosen pudek1) {
		this.pudek1 = pudek1;
	}

	/**
	 * Mengembalikan {@link Dosen} yang menjabat pembantu/wakil dekan II (biasanya bidang
	 * administrasi dan keuangan).
	 *
	 * <p>Perilaku dan efek sampingnya identik dengan {@link #getPudek1()}.</p>
	 *
	 * @return dosen pembantu dekan II, atau {@code null} bila belum diisi
	 * @see #getPudek1()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pudek2", nullable = true)
	public Dosen getPudek2() {
		pudek2 = check(pudek2);
		return pudek2;
	}

	/**
	 * Menyetel dosen pembantu/wakil dekan II. Tanpa validasi.
	 *
	 * @param pudek2 dosen baru, boleh {@code null}
	 */
	public void setPudek2(Dosen pudek2) {
		this.pudek2 = pudek2;
	}

	/**
	 * Mengembalikan {@link Dosen} yang menjabat pembantu/wakil dekan III (biasanya bidang
	 * kemahasiswaan).
	 *
	 * <p>Perilaku dan efek sampingnya identik dengan {@link #getPudek1()}.</p>
	 *
	 * @return dosen pembantu dekan III, atau {@code null} bila belum diisi
	 * @see #getPudek1()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pudek3", nullable = true)
	public Dosen getPudek3() {
		pudek3 = check(pudek3);
		return pudek3;
	}

	/**
	 * Menyetel dosen pembantu/wakil dekan III. Tanpa validasi.
	 *
	 * @param pudek3 dosen baru, boleh {@code null}
	 */
	public void setPudek3(Dosen pudek3) {
		this.pudek3 = pudek3;
	}

	/**
	 * Mengembalikan warna penanda fakultas dalam notasi hex, dengan default biru
	 * {@code "#3300ff"} bila belum diisi.
	 *
	 * <p>Warna ini dipakai sebagai {@code background-color} pada daftar fakultas di layar master
	 * dan sebagai sumber perhitungan {@link #getRgb()}.</p>
	 *
	 * <p><b>Efek samping:</b> nilai default itu <b>dituliskan ke field</b>, bukan sekadar
	 * dikembalikan — jadi cukup dengan menampilkan sebuah fakultas, warna default bisa ikut
	 * tersimpan ke kolom {@code warna} pada penyimpanan berikutnya.</p>
	 *
	 * <p><b>Tidak ada validasi format.</b> Layar master fakultas menyimpan isi kotak teks apa
	 * adanya ({@code entity.setWarna(warna.getValue())}), sehingga nilai seperti {@code "merah"}
	 * atau {@code "#fff"} bisa masuk ke database dan lolos dari getter ini. Yang akan gagal
	 * belakangan adalah {@link #getRgb()} — lihat peringatan di sana.</p>
	 *
	 * @return warna hex; tidak pernah {@code null} maupun kosong, tetapi belum tentu hex yang
	 *         sah
	 */
	public String getWarna() {
		if (warna == null || warna.trim().isEmpty()) {
			warna = "#3300ff";
		}
		return warna;
	}

	/**
	 * Menyetel warna penanda fakultas. <b>Tanpa validasi format</b> — lihat catatan di
	 * {@link #getWarna()}.
	 *
	 * @param warna warna baru, idealnya dalam bentuk {@code "#rrggbb"}
	 */
	public void setWarna(String warna) {
		this.warna = warna;
	}

	/**
	 * Mengembalikan warna fakultas dalam bentuk teks {@code "[r,g,b]"} (mis. {@code "[51,0,255]"}).
	 *
	 * <p><b>Nilai turunan, bukan nilai tersimpan.</b> Method ini <b>selalu</b> menghitung ulang
	 * dari {@link #getWarna()} lewat {@code Common.hex2Rgb} dan <b>menimpa field</b>
	 * {@code rgb}. Apa pun yang tersimpan di kolom {@code rgb} tidak pernah dibaca; setter
	 * {@link #setRgb(String)} pun praktis tidak berpengaruh karena nilainya langsung tertimpa
	 * pada pembacaan berikutnya. Karena property ini tetap dipetakan (tidak ada
	 * {@code @Transient}), hasil hitungan itu ikut tertulis ke database.</p>
	 *
	 * <p>Rantai efek sampingnya berlapis: memanggil getter ini akan memanggil
	 * {@link #getWarna()}, yang pada gilirannya bisa menuliskan warna default ke field
	 * {@code warna}. Jadi sekadar membaca "rgb" bisa mengubah dua field sekaligus.</p>
	 *
	 * <p><b>Peringatan — bisa melempar exception.</b> {@code Common.hex2Rgb} mengiris String
	 * pada posisi 1&ndash;7 dan mem-parse-nya sebagai bilangan basis 16. Karena tidak ada
	 * validasi format di {@link #setWarna(String)}, warna yang panjangnya kurang dari tujuh
	 * karakter melempar {@link StringIndexOutOfBoundsException} dan warna non-hex melempar
	 * {@link NumberFormatException} — dan karena property ini dibaca Hibernate saat menyimpan,
	 * kegagalannya bisa muncul jauh dari layar yang mengisinya. Tidak diperbaiki di sini
	 * (perubahan ini murni dokumentasi); dicatat supaya diketahui.</p>
	 *
	 * @return representasi {@code "[r,g,b]"} dari warna fakultas
	 * @see ais.common.Common#hex2Rgb(String)
	 */
	public String getRgb() {
		rgb = Common.hex2Rgb(getWarna());
		return rgb;
	}

	/**
	 * Menyetel bentuk {@code "[r,g,b]"} warna fakultas.
	 *
	 * <p>Praktis tidak berguna: {@link #getRgb()} selalu menimpa nilai ini dengan hasil hitungan
	 * dari {@link #getWarna()}. Untuk mengubah warna fakultas, pakailah
	 * {@link #setWarna(String)}. Setter ini tetap ada karena dibutuhkan Hibernate sebagai
	 * pasangan getter yang dipetakan.</p>
	 *
	 * @param rgb nilai yang akan tertimpa pada pembacaan berikutnya
	 */
	public void setRgb(String rgb) {
		this.rgb = rgb;
	}

	/**
	 * Mengembalikan penanda "fakultas masih aktif", dengan default {@code true} bila belum
	 * diisi.
	 *
	 * <p>Default {@code true} berarti fakultas lama yang kolomnya masih kosong tetap ikut tampil
	 * pada layar dan daftar pilihan yang menyaring hanya fakultas aktif. Pemanggil tidak bisa
	 * membedakan "belum pernah diisi" dari "sengaja diaktifkan" lewat getter ini.</p>
	 *
	 * @return {@code true} bila fakultas aktif; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel penanda aktif fakultas. Menyetel {@code null} sama artinya dengan {@code true}
	 * dari sudut pandang {@link #getAktif()}.
	 *
	 * @param aktif penanda baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan {@link ais.database.model.rab.SatuanKerja} (unit anggaran) fakultas ini.
	 *
	 * <p>Relasi lazy yang diresolusi lewat {@link GeneralValueObject#check(Object)} dan
	 * ditugaskan kembali ke field, jadi bisa memicu pembacaan database. Dipakai modul RAB/
	 * anggaran, dan menjadi tingkat kedua pada penelusuran satuan kerja seorang dosen —
	 * lihat {@link #getDosenHarusPakaiSatuanKerja()}.</p>
	 *
	 * @return satuan kerja fakultas, atau {@code null} bila belum diisi
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Menyetel satuan kerja penganggaran fakultas. Tanpa validasi.
	 *
	 * @param satuanKerja satuan kerja baru
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Mengembalikan penanda "dosen fakultas ini wajib memakai satuan kerja", dengan default
	 * {@code false} bila belum diisi.
	 *
	 * <p>Dipakai saat menentukan satuan kerja seorang dosen. Kode pemanggil (mis. layar biodata
	 * pegawai {@code BiodataPegawaiAction}, dan entity keanggotaan koperasi) menelusuri
	 * berjenjang — {@link Jurusan} dulu, lalu <b>{@code Fakultas}</b>, lalu
	 * {@link PerguruanTinggi} — dan memakai satuan kerja dari tingkat <b>pertama</b> yang
	 * penanda ini bernilai {@code true}. Jadi menyalakannya di sini menimpa aturan tingkat
	 * institusi, tetapi tetap kalah oleh penanda tingkat prodi.</p>
	 *
	 * @return {@code true} bila dosen fakultas ini wajib bersatuan kerja; tidak pernah
	 *         {@code null}
	 * @see Jurusan#getDosenHarusPakaiSatuanKerja()
	 */
	public Boolean getDosenHarusPakaiSatuanKerja() {
		return dosenHarusPakaiSatuanKerja == null ? false : dosenHarusPakaiSatuanKerja;
	}

	/**
	 * Menyetel penanda kewajiban satuan kerja bagi dosen fakultas ini. Menyetel {@code null} sama
	 * artinya dengan {@code false}.
	 *
	 * @param dosenHarusPakaiSatuanKerja penanda baru
	 */
	public void setDosenHarusPakaiSatuanKerja(Boolean dosenHarusPakaiSatuanKerja) {
		this.dosenHarusPakaiSatuanKerja = dosenHarusPakaiSatuanKerja;
	}

	/**
	 * Mengembalikan nama fakultas dalam bahasa Inggris, dengan <b>fallback ke nama Indonesia</b>
	 * bila belum diisi.
	 *
	 * <p>Dipakai dokumen dwibahasa (transkrip, ijazah, surat keterangan berbahasa Inggris).
	 * Fallback-nya memakai {@link #getNama()}, sehingga template tidak perlu memeriksa
	 * {@code null} — tetapi juga berarti pemanggil tidak bisa membedakan "terjemahan belum
	 * disiapkan" dari "terjemahannya memang sama".</p>
	 *
	 * <p>Karena fallback-nya memanggil {@link #getNama()}, getter ini ikut membawa efek samping
	 * getter tersebut (penulisan String kosong ke field {@code nama} bila masih {@code null}).
	 * Berbeda dengan getter fallback lain di kelas ini, hasil fallback <b>tidak</b> ditulis balik
	 * ke field {@code namaEn} — kolomnya tetap kosong.</p>
	 *
	 * @return nama Inggris terpangkas bila ada; kalau tidak, hasil {@link #getNama()}
	 */
	public String getNamaEn() {
		return namaEn == null || namaEn.trim().isEmpty() ? getNama() : namaEn.trim();
	}

	/**
	 * Menyetel nama fakultas dalam bahasa Inggris. Tanpa validasi maupun pemangkasan.
	 *
	 * @param namaEn nama Inggris baru; kosongkan untuk kembali memakai nama Indonesia
	 */
	public void setNamaEn(String namaEn) {
		this.namaEn = namaEn;
	}

	/**
	 * Mengembalikan judul jabatan untuk slot pejabat bebas ke-1, dengan default
	 * {@code "Pejabat I"}.
	 *
	 * <p>Tiga slot {@code pegawai1..3} beserta labelnya adalah mekanisme "jabatan bebas" di luar
	 * struktur dekan/pudek: institusi bisa menamainya sendiri (mis. "Kepala Tata Usaha",
	 * "Ketua Gugus Mutu") lewat layar master fakultas. Labelnya ikut tercetak pada dokumen —
	 * mis. {@code CommonReportHelper} menuliskannya sebagai parameter laporan
	 * {@code jenis_pejabat_fakultas_1}.</p>
	 *
	 * <p>Perhatikan default-nya hanya berlaku untuk {@code null}; label yang sengaja dikosongkan
	 * (String kosong) dikembalikan apa adanya sebagai kosong.</p>
	 *
	 * @return judul jabatan slot ke-1; tidak pernah {@code null}
	 * @see #getPegawai1()
	 */
	public String getLabelPejabat1() {
		return labelPejabat1 == null ? "Pejabat I" : labelPejabat1;
	}

	/**
	 * Menyetel judul jabatan untuk slot pejabat bebas ke-1. Tanpa validasi.
	 *
	 * @param labelPejabat1 judul jabatan baru
	 */
	public void setLabelPejabat1(String labelPejabat1) {
		this.labelPejabat1 = labelPejabat1;
	}

	/**
	 * Mengembalikan judul jabatan untuk slot pejabat bebas ke-2, dengan default
	 * {@code "Pejabat II"}.
	 *
	 * @return judul jabatan slot ke-2; tidak pernah {@code null}
	 * @see #getLabelPejabat1()
	 */
	public String getLabelPejabat2() {
		return labelPejabat2 == null ? "Pejabat II" : labelPejabat2;
	}

	/**
	 * Menyetel judul jabatan untuk slot pejabat bebas ke-2. Tanpa validasi.
	 *
	 * @param labelPejabat2 judul jabatan baru
	 */
	public void setLabelPejabat2(String labelPejabat2) {
		this.labelPejabat2 = labelPejabat2;
	}

	/**
	 * Mengembalikan judul jabatan untuk slot pejabat bebas ke-3, dengan default
	 * {@code "Pejabat III"}.
	 *
	 * @return judul jabatan slot ke-3; tidak pernah {@code null}
	 * @see #getLabelPejabat1()
	 */
	public String getLabelPejabat3() {
		return labelPejabat3 == null ? "Pejabat III" : labelPejabat3;
	}

	/**
	 * Menyetel judul jabatan untuk slot pejabat bebas ke-3. Tanpa validasi.
	 *
	 * @param labelPejabat3 judul jabatan baru
	 */
	public void setLabelPejabat3(String labelPejabat3) {
		this.labelPejabat3 = labelPejabat3;
	}

	/**
	 * Mengembalikan {@link Pegawai} pengisi slot pejabat bebas ke-1, yang judul jabatannya
	 * ditentukan {@link #getLabelPejabat1()}.
	 *
	 * <p>Relasi lazy yang diresolusi lewat {@link GeneralValueObject#check(Object)} dan
	 * ditugaskan kembali ke field, jadi bisa memicu pembacaan database.</p>
	 *
	 * <p>Perhatikan bahwa slot ini bertipe {@link Pegawai} (tenaga kependidikan/karyawan),
	 * berbeda dari slot dekan dan pudek yang bertipe {@link Dosen}. Keduanya rangkaian terpisah
	 * dan tidak saling menggantikan.</p>
	 *
	 * @return pegawai pejabat ke-1, atau {@code null} bila belum diisi
	 * @see #getLabelPejabat1()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai1", nullable = true)
	public Pegawai getPegawai1() {
		pegawai1 = check(pegawai1);
		return pegawai1;
	}

	/**
	 * Menyetel pegawai pengisi slot pejabat bebas ke-1. Tanpa validasi.
	 *
	 * @param pegawai1 pegawai baru, boleh {@code null}
	 */
	public void setPegawai1(Pegawai pegawai1) {
		this.pegawai1 = pegawai1;
	}

	/**
	 * Mengembalikan {@link Pegawai} pengisi slot pejabat bebas ke-2.
	 *
	 * <p>Perilaku dan efek sampingnya identik dengan {@link #getPegawai1()}.</p>
	 *
	 * @return pegawai pejabat ke-2, atau {@code null} bila belum diisi
	 * @see #getPegawai1()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai2", nullable = true)
	public Pegawai getPegawai2() {
		pegawai2 = check(pegawai2);
		return pegawai2;
	}

	/**
	 * Menyetel pegawai pengisi slot pejabat bebas ke-2. Tanpa validasi.
	 *
	 * @param pegawai2 pegawai baru, boleh {@code null}
	 */
	public void setPegawai2(Pegawai pegawai2) {
		this.pegawai2 = pegawai2;
	}

	/**
	 * Mengembalikan {@link Pegawai} pengisi slot pejabat bebas ke-3.
	 *
	 * <p>Perilaku dan efek sampingnya identik dengan {@link #getPegawai1()}.</p>
	 *
	 * @return pegawai pejabat ke-3, atau {@code null} bila belum diisi
	 * @see #getPegawai1()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai3", nullable = true)
	public Pegawai getPegawai3() {
		pegawai3 = check(pegawai3);
		return pegawai3;
	}

	/**
	 * Menyetel pegawai pengisi slot pejabat bebas ke-3. Tanpa validasi.
	 *
	 * @param pegawai3 pegawai baru, boleh {@code null}
	 */
	public void setPegawai3(Pegawai pegawai3) {
		this.pegawai3 = pegawai3;
	}

	/**
	 * Mengembalikan nomor WhatsApp kontak fakultas dalam bentuk terpangkas, dengan default String
	 * kosong bila belum diisi.
	 *
	 * <p>Tidak ada normalisasi format nomor (awalan {@code 0} vs {@code 62}, tanda hubung, spasi
	 * di tengah) — yang tersimpan dikembalikan apa adanya selain pemangkasan spasi tepi. Berbeda
	 * dengan getter default lain, hasil default di sini <b>tidak</b> ditulis balik ke field.</p>
	 *
	 * @return nomor WhatsApp fakultas; String kosong bila belum diisi, tidak pernah {@code null}
	 */
	public String getWa() {
		return wa == null ? "" : wa.trim();
	}

	/**
	 * Menyetel nomor WhatsApp kontak fakultas. Tanpa validasi maupun normalisasi format.
	 *
	 * @param wa nomor baru
	 */
	public void setWa(String wa) {
		this.wa = wa;
	}

	/**
	 * Menyisipkan berkas <b>kop surat</b> dan <b>stempel</b> milik fakultas ini ke dalam peta
	 * parameter laporan JasperReports.
	 *
	 * <p>Satu-satunya method non-accessor di kelas ini. Dipanggil dari perakit dokumen — antara
	 * lain {@code ais.action.master.surat.util.SuratUtil} saat mencetak surat dan
	 * {@code ais.common.ManajemenProperty} saat menyiapkan parameter laporan — berpasangan
	 * dengan method senama di {@link Jurusan}, {@link PerguruanTinggi}, {@code Sekolah}, dan
	 * {@code Yayasan}, sehingga template bisa memilih kop tingkat prodi, fakultas, institusi,
	 * atau yayasan.</p>
	 *
	 * <h3>Alur untuk masing-masing dari dua jenis berkas</h3>
	 * <ol>
	 *   <li>Cari berkas jadi di folder berkas aplikasi lewat
	 *   {@code FileFoto.fileAdaDiFolder(jenis, id)} (berkas bernama {@code <jenis>_<id>} dengan
	 *   ekstensi {@code .jpg}/{@code .jpeg}/{@code .png}).</li>
	 *   <li>Bila berkas itu ada dan lolos {@code Common.isGambarLaporanValid} (ada, tidak
	 *   berukuran nol, dan benar-benar gambar), jalur cepat ini yang dipakai.</li>
	 *   <li>Bila tidak, ambil lampiran dari tabel lewat
	 *   {@code LampiranLain.ambil(false, id, jenis)} lalu pakai {@code ambilFile()}-nya bila
	 *   gambarnya valid. Jalur ini <b>membuka session Hibernate sendiri</b> (bukan session
	 *   pemanggil, dan session tersebut ditutupnya sendiri) dan <b>menulis berkas cache</b>
	 *   lokasi lampiran — jadi method ini bisa menyentuh database dan disk, bukan sekadar
	 *   mengisi map.</li>
	 * </ol>
	 *
	 * <h3>Kunci yang ditulis ke {@code parameters}</h3>
	 * <p>Untuk tiap berkas yang ditemukan, ditulis <b>tiga</b> kunci berisi path absolut yang
	 * sama: kunci polos ({@code "KOP_FAKULTAS"} / {@code "STEMPEL_FAKULTAS"}), kunci berimbuhan
	 * id ({@code "KOP_FAKULTAS_<id>"}), dan kunci berimbuhan nama fakultas
	 * ({@code "KOP_FAKULTAS_<nama>"}). Bentuk berimbuhan memungkinkan satu laporan memuat kop
	 * beberapa fakultas sekaligus. Perhatikan konsekuensinya: bila method ini dipanggil untuk
	 * beberapa fakultas pada map yang sama, kunci polosnya akan <b>saling menimpa</b> sehingga
	 * yang bertahan adalah fakultas yang diproses terakhir — template yang harus tepat wajib
	 * memakai varian berimbuhan. Kunci berimbuhan nama juga ikut membawa spasi dan tanda baca
	 * nama fakultas apa adanya, dan memakai {@link #getNama()} sehingga terpengaruh normalisasi
	 * (dan efek samping) getter tersebut.</p>
	 *
	 * <p>Kedua jenis berkas diproses independen: kop bisa ditemukan sementara stempel tidak.
	 * Bila sebuah berkas tidak ditemukan atau tidak valid, method <b>tidak menulis apa pun</b>
	 * untuk berkas itu dan tidak melempar exception — template laporan harus tahan terhadap
	 * parameter kop/stempel yang tidak ada.</p>
	 *
	 * <p>Variabel lokal {@code fakultas} hanyalah alias untuk {@code this} (sisa gaya kode lama);
	 * tidak ada object lain yang terlibat.</p>
	 *
	 * @param parameters peta parameter laporan yang akan diisi; dimodifikasi di tempat, tidak
	 *                   boleh {@code null}. Bertipe {@code Map} mentah mengikuti API
	 *                   JasperReports, karenanya ada {@code @SuppressWarnings} di sini.
	 * @see Jurusan#putFile(java.util.Map)
	 * @see ais.database.model.file.LampiranLain#KOP_FAKULTAS
	 * @see ais.database.model.file.LampiranLain#STEMPEL_FAKULTAS
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void putFile(Map parameters) {
		Fakultas fakultas = this;
		File file = FileFoto.fileAdaDiFolder(LampiranLain.KOP_FAKULTAS, fakultas.getId());
		if (Common.isGambarLaporanValid(file)) {
			parameters.put("KOP_FAKULTAS", file.getAbsolutePath());
			parameters.put("KOP_FAKULTAS_" + fakultas.getId(), file.getAbsolutePath());
			parameters.put("KOP_FAKULTAS_" + fakultas.getNama(), file.getAbsolutePath());
		} else {
			LampiranLain kop = LampiranLain.ambil(false, fakultas.getId(), LampiranLain.KOP_FAKULTAS);
			if (kop != null) {
				File fileKop = kop.ambilFile();
				if (Common.isGambarLaporanValid(fileKop)) {
					parameters.put("KOP_FAKULTAS", fileKop.getAbsolutePath());
					parameters.put("KOP_FAKULTAS_" + fakultas.getId(), fileKop.getAbsolutePath());
					parameters.put("KOP_FAKULTAS_" + fakultas.getNama(), fileKop.getAbsolutePath());
				}
			}
		}

		file = FileFoto.fileAdaDiFolder(LampiranLain.STEMPEL_FAKULTAS, fakultas.getId());
		if (Common.isGambarLaporanValid(file)) {
			parameters.put("STEMPEL_FAKULTAS", file.getAbsolutePath());
			parameters.put("STEMPEL_FAKULTAS_" + fakultas.getId(), file.getAbsolutePath());
			parameters.put("STEMPEL_FAKULTAS_" + fakultas.getNama(), file.getAbsolutePath());
		} else {
			LampiranLain kop = LampiranLain.ambil(false, fakultas.getId(), LampiranLain.STEMPEL_FAKULTAS);
			if (kop != null) {
				File fileKop = kop.ambilFile();
				if (Common.isGambarLaporanValid(fileKop)) {
					parameters.put("STEMPEL_FAKULTAS", fileKop.getAbsolutePath());
					parameters.put("STEMPEL_FAKULTAS_" + fakultas.getId(), fileKop.getAbsolutePath());
					parameters.put("STEMPEL_FAKULTAS_" + fakultas.getNama(), fileKop.getAbsolutePath());
				}
			}
		}
	}
}
