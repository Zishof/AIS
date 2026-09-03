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

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.database.hibernate.HibernateUtil;

/**
 * Entity master <b>kategori/kelompok field kustom</b> ("form tambahan") untuk modul Catatan
 * Administrasi. Satu baris tabel {@code public.kelompok_parameter_tambahan_catatan_administrasi}
 * mewakili satu <i>seksi</i> pada formulir Catatan Administrasi — misalnya "Form Tambahan"
 * (kelompok bawaan), "Data Pelanggaran", "Data Sanksi" — bukan field-nya sendiri.
 *
 * <p><b>Posisi dalam rantai data.</b> Entity ini adalah simpul paling atas dari tiga lapis
 * konfigurasi field kustom Catatan Administrasi:</p>
 * <ol>
 *   <li><b>{@code KelompokParameterTambahanCatatanAdministrasi}</b> (kelas ini) — kategori/seksi.</li>
 *   <li>{@link ais.database.model.ParameterTambahanCatatanAdministrasi} — tabel penghubung yang
 *   menempelkan sebuah {@link ais.database.model.ParameterTambahan} (definisi field: label, tipe
 *   data, daftar nilai, wajib lampiran atau tidak) ke salah satu kelompok di sini, lengkap dengan
 *   nomor urut field di dalam kelompok.</li>
 *   <li>{@link ais.database.model.JenisCatatanAdministrasi} — jenis catatan (mis. "Pelanggaran
 *   Tata Tertib") memilih kelompok mana saja yang muncul, lewat relasi {@code @ManyToMany} ke
 *   kelas ini pada tabel gabungan {@code jenis_catatan_administrasi_has_parameter}.</li>
 * </ol>
 *
 * <p>Nilai yang diisi pengguna pada field-field tersebut TIDAK disimpan di sini; nilainya
 * ditampung {@link ais.database.model.CatatanAdministrasi} (kolom teks gabungan yang diisi
 * {@code populateParameterTambahan}). Kelas ini murni master konfigurasi tampilan.</p>
 *
 * <p><b>Keluarga sejenis.</b> Kelas ini adalah salah satu anggota keluarga besar
 * {@code KelompokParameterTambahan*} yang mengulang pola yang sama untuk domain berbeda:
 * {@link ais.database.model.KelompokParameterTambahanAlumni},
 * {@link ais.database.model.KelompokParameterTambahanMahasiswa}, dan
 * {@link ais.database.model.KelompokParameterTambahanCalonMahasiswa}. Diverifikasi lewat
 * pembandingan otomatis terhadap versi pristine: badan kelas ini <b>identik kata-per-kata</b>
 * dengan varian Mahasiswa kecuali nama tabel/tipe dan satu perbedaan nyata —
 * {@link #compareTo(GeneralValueObject)} di sini adalah versi <i>pendek</i> (cast telanjang tanpa
 * {@code instanceof}, tanpa rantai fallback {@code getNim()}/{@code getNama()}/
 * {@code getKeterangan()} yang dipakai saudara-saudaranya). Terhadap varian Alumni, bedanya
 * tambahan satu: kelas ini tidak punya field {@code digunakanUntukPenggunaAlumni}.</p>
 *
 * <p><b>Auto-seed kelompok bawaan.</b> {@link #checkCreateDefault()} menjamin selalu ada tepat
 * satu kelompok bertanda {@code defaultData = true} bernama "Form Tambahan". Method statis ini
 * dipanggil dari <b>satu titik saja</b> di seluruh codebase:
 * {@code ais.action.master.ParameterTambahanCatatanAdministrasiAction.doAfterCompose()}, yaitu
 * saat layar "Parameter Tambahan Catatan Administrasi" dibuka. Berbeda dengan varian Alumni yang
 * punya mekanisme auto-seed KEDUA di kelas Action-nya (sehingga bisa lahir dua kategori bawaan
 * berbeda tergantung urutan klik admin), layar master kelas ini
 * ({@code KelompokParameterTambahanCatatanAdministrasiAction}) TIDAK menyemai apa pun — jadi
 * <b>tidak ada race dual auto-seed di sini</b>.</p>
 *
 * <p><b>Peringatan pengurutan (bug nyata, bukan sekadar kuirk).</b>
 * {@link #compareTo(GeneralValueObject)} membandingkan HANYA {@code nomorUrut}, sehingga dua
 * kelompok dengan {@code nomorUrut} sama dianggap <i>duplikat</i> oleh struktur data berbasis
 * {@code Comparable}. Padahal:</p>
 * <ul>
 *   <li>{@link #getNomorUrut()} mengembalikan {@code 1} untuk baris yang belum pernah disetel, dan
 *   layar "Tambah/Ubah Kelompok" ({@code onSave}) tidak pernah mengisi kolom ini — satu-satunya
 *   pengisi adalah {@code Intbox} nomor urut di grid daftar;</li>
 *   <li>{@code JenisCatatanAdministrasi.kelompokParameterTambahanCatatanAdministrasis}
 *   diinisialisasi sebagai {@code TreeSet}, dan
 *   {@code CatatanAdministrasiAction} menyalin ulang isinya ke {@code new TreeSet<>()} sebelum
 *   menyerahkannya ke {@code ParameterTambahanCatatanAdministrasiListener}.</li>
 * </ul>
 * <p>Akibatnya, selama admin belum memberi nomor urut yang <i>berbeda-beda</i> lewat grid, semua
 * kelompok yang dipilih pada satu jenis catatan akan menciut menjadi <b>satu</b> di formulir —
 * seksi-seksi lain hilang senyap tanpa pesan error. Ini varian dari pola "penciutan senyap
 * {@code TreeSet}" yang sudah tercatat di modul lain. Perilaku ini <b>tidak diubah</b> pada
 * dokumentasi ini.</p>
 *
 * <p><b>Pengelompokan method.</b> (a) jejak audit manual: {@link #getOleh()}/{@link #setOleh(String)},
 * {@link #getOlehId()}/{@link #setOlehId(String)}, {@link #getTanggal_dirubah()}/
 * {@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}; (b) identitas &amp; label:
 * {@link #getId()}, {@link #getNama()}, {@link #getKeterangan()}, {@link #toString()};
 * (c) tiga getter <i>mutatif self-healing</i> yang menambal nilai {@code null} menjadi default
 * ({@link #getDefaultData()} &rarr; {@code false}, {@link #getAktif()} &rarr; {@code true},
 * {@link #getNomorUrut()} &rarr; {@code 1}); (d) pengurutan: {@link #compareTo(GeneralValueObject)};
 * (e) utilitas statis: {@link #checkCreateDefault()}.</p>
 *
 * <p><b>Catatan pemetaan.</b> Kelas ini {@code extends} {@link ais.database.model.GeneralValueObject},
 * yang <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} melainkan POJO abstrak biasa.
 * Karena itu Hibernate tidak memetakan properti milik induk sama sekali; deklarasi ULANG field
 * {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} di kelas ini <b>bukan
 * duplikasi keliru</b>, melainkan keharusan teknis agar kolom-kolom tersebut ikut terpetakan.
 * Pemetaan memakai <i>property access</i> (anotasi menempel pada getter), sehingga getter mutatif
 * pada butir (c) di atas nilainya ikut tertulis ke DB pada flush berikutnya — sifat
 * <i>self-healing</i>, bukan destruktif, karena hanya mengisi {@code null} dengan default.</p>
 *
 * <p><b>Revisi/audit.</b> {@code @Audited} (Hibernate Envers) merekam setiap perubahan ke tabel
 * revisi; {@code dynamicInsert}/{@code dynamicUpdate} membuat SQL hanya memuat kolom yang benar-benar
 * berubah.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.KelompokParameterTambahanAlumni
 * @see ais.database.model.KelompokParameterTambahanMahasiswa
 * @see ais.database.model.KelompokParameterTambahanCalonMahasiswa
 * @see ais.database.model.ParameterTambahanCatatanAdministrasi
 * @see ais.database.model.JenisCatatanAdministrasi
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "kelompok_parameter_tambahan_catatan_administrasi")
public class KelompokParameterTambahanCatatanAdministrasi extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Instance entity ini dapat ikut terserialisasi karena
	 * disimpan pada atribut komponen/desktop ZK dan pada koleksi cache statis
	 * {@code JenisCatatanAdministrasi.mapParameters}. Nilainya dibangkitkan generator dan
	 * dipertahankan apa adanya agar sesi lama tetap kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Primary key {@code id} (auto-increment/IDENTITY). Lihat {@link #getId()}. */
	private Long id;

	/** Nama/username pengguna yang terakhir mengubah baris ini. Lihat {@link #getOleh()}. */
	private String oleh;

	/** Id pengguna yang terakhir mengubah baris ini. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna terakhir, atau {@code null} bila belum pernah tercatat
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna yang terakhir mengubah baris ini.
	 *
	 * <p><b>Setter defensif:</b> nilai {@code null}, string kosong, atau string berisi spasi saja
	 * <b>diabaikan diam-diam</b> (method langsung {@code return} tanpa menulis apa pun), sehingga
	 * jejak audit yang sudah terisi tidak bisa terhapus oleh pemanggil yang ceroboh. Konsekuensinya
	 * nilai lama sengaja dipertahankan dan tidak ada pesan kesalahan apa pun.</p>
	 *
	 * <p>Umumnya dipanggil oleh {@code ais.database.hibernate.AuditTimestampInterceptor} lewat
	 * {@link #onUpdate()}, bukan oleh kode layar.</p>
	 *
	 * @param olehId id pengguna baru; diabaikan bila {@code null}/kosong/hanya spasi
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama/username pengguna yang terakhir mengubah baris ini.
	 *
	 * <p><b>Setter defensif</b> dengan aturan yang sama persis seperti
	 * {@link #setOlehId(String)}: nilai {@code null}/kosong/hanya spasi diabaikan diam-diam.</p>
	 *
	 * @param oleh nama pengguna baru; diabaikan bila {@code null}/kosong/hanya spasi
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama/username pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna terakhir, atau {@code null} bila belum pernah tercatat
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dijalankan Hibernate tepat sebelum {@code UPDATE} baris ini
	 * dieksekusi. Mendelegasikan pengisian jejak audit ({@code oleh}, {@code olehId},
	 * {@code tanggal_dirubah}) ke {@code ais.database.hibernate.AuditTimestampInterceptor.ubah}.
	 *
	 * <p><b>Efek samping:</b> mengubah state instance ini. Tidak dipanggil pada {@code INSERT}
	 * (hanya {@code UPDATE}); nilai awal {@code tanggal_dirubah} untuk baris baru berasal dari
	 * inisialisasi field. Implementasi wajib dari method {@code abstract} pada
	 * {@link ais.database.model.GeneralValueObject}.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir baris ini. Sengaja diinisialisasi ke waktu server saat instance
	 * dibuat ({@code ais.ui.util.WaktuUtil.getDate()}) agar baris baru pun sudah punya stempel
	 * waktu sebelum {@link #onUpdate()} pertama berjalan. Lihat {@link #getTanggal_dirubah()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Tanpa validasi; nilai {@code null} diterima.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (dipetakan sebagai {@code TIMESTAMP}).
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk instance yang dibuat
	 *         lewat konstruktor, kecuali di-{@code null}-kan secara eksplisit lewat setter
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas berbentuk {@code "<id>-<nama>"}.
	 *
	 * <p>Membaca field {@code nama} secara langsung (bukan lewat {@link #getNama()}), sehingga
	 * nilainya <b>tidak</b> di-{@code trim}. Dipakai antara lain oleh log {@code System.out} pada
	 * layar Jenis Catatan Administrasi saat admin mencentang/melepas kelompok.</p>
	 *
	 * @return gabungan id dan nama; kedua bagian dapat berbunyi {@code "null"} bila belum terisi
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama kelompok/seksi, wajib diisi dan unik menurut validasi layar. Lihat {@link #getNama()}. */
	private String nama;

	/** Keterangan bebas, opsional. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Penanda kelompok bawaan hasil auto-seed. Lihat {@link #getDefaultData()}. */
	private Boolean defaultData;

	/** Penanda kelompok masih dipakai/ditampilkan. Lihat {@link #getAktif()}. */
	private Boolean aktif;

	/** Nomor urut tampil kelompok pada formulir. Lihat {@link #getNomorUrut()}. */
	private Integer nomorUrut;

	/**
	 * Menjamin tersedianya kelompok bawaan bernama <b>"Form Tambahan"</b> ({@code defaultData = true}),
	 * membuatnya bila belum ada. Berfungsi sebagai mekanisme <i>auto-seed</i> agar instalasi baru
	 * langsung punya satu wadah untuk menampung field tambahan yang belum dikategorikan.
	 *
	 * <p><b>Alur:</b> mencari satu baris dengan {@code defaultData = true} lewat {@code Criteria}
	 * pada <i>native session</i>; bila tidak ketemu, membuat instance baru dengan
	 * {@code defaultData = true}, {@code nama = "Form Tambahan"}, {@code keterangan = "Form Tambahan"},
	 * lalu menyimpannya di dalam transaksi eksplisit ({@code begin}/{@code save}/{@code commit}).
	 * Di akhir, session selalu ditutup lewat {@code HibernateUtil.closeSession()} — termasuk pada
	 * jalur "sudah ada", sehingga object yang dikembalikan berstatus <b>detached</b>.</p>
	 *
	 * <p><b>Efek samping:</b> dapat melakukan {@code INSERT} beserta revisi Envers, dan
	 * <b>selalu</b> menutup session Hibernate milik thread saat ini. Pemanggil yang masih
	 * membutuhkan session harus mengambilnya kembali setelah pemanggilan ini.</p>
	 *
	 * <p><b>Dipanggil dari:</b> satu titik saja di seluruh codebase —
	 * {@code ais.action.master.ParameterTambahanCatatanAdministrasiAction.doAfterCompose()}, yakni
	 * ketika layar "Parameter Tambahan Catatan Administrasi" dibuka; jadi baris bawaan baru lahir
	 * saat layar itu pertama kali diakses, bukan saat instalasi. Layar master kelompok
	 * ({@code KelompokParameterTambahanCatatanAdministrasiAction}) sengaja tidak memanggilnya,
	 * sehingga <b>tidak ada</b> mekanisme auto-seed kedua yang bisa berlomba seperti pada varian
	 * Alumni.</p>
	 *
	 * <p><b>Kuirk:</b> tidak ada penguncian maupun batasan {@code unique} pada {@code defaultData},
	 * jadi dua request bersamaan pada instalasi yang benar-benar kosong secara teoretis dapat
	 * menghasilkan dua baris bawaan. Kelompok bawaan hasil method ini juga lahir tanpa
	 * {@code nomorUrut} eksplisit — lihat peringatan pengurutan pada dokumentasi kelas.</p>
	 *
	 * @return kelompok bawaan yang ditemukan atau yang baru saja dibuat; tidak pernah {@code null}
	 */
	public static KelompokParameterTambahanCatatanAdministrasi checkCreateDefault() {
		Session session = HibernateUtil.currentNativeSession();
		KelompokParameterTambahanCatatanAdministrasi kelompokParameterTambahanCatatanAdministrasi = (KelompokParameterTambahanCatatanAdministrasi) session
				.createCriteria(KelompokParameterTambahanCatatanAdministrasi.class)
				.add(Restrictions.eq("defaultData", true)).setMaxResults(1).uniqueResult();
		if (kelompokParameterTambahanCatatanAdministrasi == null) {
			kelompokParameterTambahanCatatanAdministrasi = new KelompokParameterTambahanCatatanAdministrasi();
			kelompokParameterTambahanCatatanAdministrasi.setDefaultData(true);
			kelompokParameterTambahanCatatanAdministrasi.setNama("Form Tambahan");
			kelompokParameterTambahanCatatanAdministrasi.setKeterangan("Form Tambahan");
			session.getTransaction().begin();
			session.save(kelompokParameterTambahanCatatanAdministrasi);
			session.getTransaction().commit();
		}

		HibernateUtil.closeSession();
		return kelompokParameterTambahanCatatanAdministrasi;
	}

	/**
	 * Konstruktor tanpa argumen — wajib bagi Hibernate dan dipakai layar untuk menyiapkan form
	 * "Tambah Kelompok" ({@code onAdd}). Semua field dibiarkan {@code null} kecuali
	 * {@code tanggal_dirubah} yang terisi waktu server lewat inisialisasi field.
	 */
	public KelompokParameterTambahanCatatanAdministrasi() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * <p>Kolom {@code id} ditandai {@code insertable = false} karena nilainya dibangkitkan database
	 * (strategi {@code IDENTITY}). Dipakai luas sebagai kunci pencocokan pada layar Jenis Catatan
	 * Administrasi dan sebagai bagian kunci komponen {@code "<idKelompok>-><idParameter>"} pada
	 * renderer formulir.</p>
	 *
	 * @return id baris, atau {@code null} bila instance belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Praktis hanya dipakai Hibernate saat memuat baris; kode aplikasi tidak
	 * boleh mengubah id baris yang sudah tersimpan.
	 *
	 * @param id primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama kelompok/seksi, sudah di-{@code trim}.
	 *
	 * <p>Meng-override {@link ais.database.model.GeneralValueObject#getNama()} yang mengembalikan
	 * field apa adanya. Nilai inilah yang tampil sebagai judul seksi pada formulir Catatan
	 * Administrasi dan sebagai label checkbox pada layar Jenis Catatan Administrasi.</p>
	 *
	 * <p><b>Perhatikan:</b> pemetaan memakai <i>property access</i>, sehingga hasil {@code trim()}
	 * inilah yang ikut ditulis Hibernate saat flush — spasi di ujung nama akan terpangkas secara
	 * permanen pada penyimpanan berikutnya.</p>
	 *
	 * @return nama kelompok tanpa spasi di kedua ujung, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama kelompok. Tanpa validasi di level entity; kewajiban isi dan pemeriksaan
	 * duplikasi nama dilakukan layar ({@code onSave} beserta
	 * {@code checkNamaKelompokParameterTambahanCatatanAdministrasi()}).
	 *
	 * @param nama nama kelompok baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas kelompok ini, <b>apa adanya</b>.
	 *
	 * <p><b>Membalik kontrak kelas induk:</b>
	 * {@link ais.database.model.GeneralValueObject#getKeterangan()} menormalisasi {@code null}
	 * menjadi string kosong sehingga dijanjikan "tidak pernah {@code null}", sedangkan override ini
	 * mengembalikan field mentah dan <b>bisa</b> {@code null}. Kode pemanggil karenanya tidak boleh
	 * mengandalkan janji induk. Efek nyatanya terlihat pada renderer grid layar master, yang
	 * menyerahkan hasil method ini langsung ke {@code new Label(...)}.</p>
	 *
	 * @return keterangan kelompok, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas kelompok ini. Tanpa validasi; nilai {@code null} diterima.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan penanda apakah baris ini adalah kelompok <b>bawaan</b> hasil
	 * {@link #checkCreateDefault()}.
	 *
	 * <p><b>Getter mutatif (self-healing):</b> bila field masih {@code null}, method menuliskan
	 * {@code false} ke field sebelum mengembalikannya. Karena pemetaan memakai <i>property
	 * access</i>, nilai tambalan itu ikut tersimpan ke DB pada flush berikutnya. Sifatnya menambal
	 * satu arah ({@code null} &rarr; {@code false}) sehingga tidak merusak data yang sudah terisi.</p>
	 *
	 * <p>Dipakai layar master untuk melindungi kelompok bawaan: tombol Hapus disembunyikan bila
	 * method ini mengembalikan {@code true}.</p>
	 *
	 * @return {@code true} bila kelompok bawaan; {@code false} untuk kelompok buatan admin (termasuk
	 *         baris lama yang kolomnya masih {@code null})
	 */
	public Boolean getDefaultData() {
		if (defaultData == null) {
			defaultData = false;
		}
		return defaultData;
	}

	/**
	 * Menyetel penanda kelompok bawaan. Dalam praktik hanya dipanggil
	 * {@link #checkCreateDefault()}; tidak ada layar yang mengeksposnya ke admin.
	 *
	 * @param defaultData {@code true} untuk menandai kelompok bawaan
	 */
	public void setDefaultData(Boolean defaultData) {
		this.defaultData = defaultData;
	}

	/**
	 * Mengembalikan penanda apakah kelompok ini masih aktif dipakai.
	 *
	 * <p><b>Getter mutatif (self-healing):</b> bila field masih {@code null}, method menuliskan
	 * {@code true} ke field sebelum mengembalikannya — jadi baris lama yang kolomnya kosong
	 * dianggap <i>aktif</i> (bawaan permisif), dan nilai tambalan itu ikut tersimpan pada flush
	 * berikutnya lewat <i>property access</i>.</p>
	 *
	 * <p>Dipakai sebagai filter tampil: {@code ParameterTambahanCatatanAdministrasiListener}
	 * menyaring {@code kelompokParameterTambahanCatatanAdministrasi.aktif = true} saat mengambil
	 * field yang akan dirender, dan renderer grid memakainya untuk kondisi checkbox "Aktif".</p>
	 *
	 * @return {@code true} bila kelompok masih ditampilkan pada formulir
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menyetel penanda aktif. Dipanggil dari listener {@code onCheck} checkbox "Aktif" pada grid
	 * layar master, yang langsung menyimpan perubahan lewat {@code Common.refreshSaveOrUpdate}.
	 *
	 * @param aktif {@code true} bila kelompok masih dipakai
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan nomor urut tampil kelompok ini pada formulir Catatan Administrasi.
	 *
	 * <p><b>Getter mutatif (self-healing):</b> bila field masih {@code null}, method menuliskan
	 * {@code 1} ke field sebelum mengembalikannya; nilai tambalan itu ikut tersimpan pada flush
	 * berikutnya lewat <i>property access</i>. Ekspresi ternary pada baris {@code return} sudah
	 * <b>tidak pernah</b> mengambil cabang {@code null} karena field dijamin terisi oleh blok
	 * {@code if} di atasnya — sisa penulisan defensif berlapis, dibiarkan apa adanya.</p>
	 *
	 * <p><b>Penting:</b> nilai ini adalah satu-satunya kunci
	 * {@link #compareTo(GeneralValueObject)}, sehingga beberapa kelompok yang berbagi nomor urut
	 * yang sama akan saling menutupi di dalam {@code TreeSet} yang dipakai
	 * {@code JenisCatatanAdministrasi} dan {@code CatatanAdministrasiAction} — lihat peringatan
	 * pengurutan pada dokumentasi kelas. Karena bawaannya {@code 1} untuk semua baris, kondisi ini
	 * adalah keadaan <i>default</i>, bukan kasus langka.</p>
	 *
	 * @return nomor urut tampil; tidak pernah {@code null} (minimal {@code 1})
	 */
	public Integer getNomorUrut() {
		if (nomorUrut == null) {
			nomorUrut = 1;
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampil kelompok ini.
	 *
	 * <p>Satu-satunya jalur pengisian dari UI adalah {@code Intbox} nomor urut pada grid layar
	 * master, yang langsung menyimpan lewat {@code Common.refreshSaveOrUpdate} pada event
	 * {@code onChange}; form "Tambah/Ubah Kelompok" tidak menyediakan kolom ini sama sekali.</p>
	 *
	 * @param nomorUrut nomor urut baru; {@code null} akan ditambal menjadi {@code 1} oleh
	 *                  {@link #getNomorUrut()}
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Membandingkan dua kelompok <b>hanya</b> berdasarkan {@link #getNomorUrut()}.
	 *
	 * <p>Meng-override {@link ais.database.model.GeneralValueObject#compareTo(GeneralValueObject)}
	 * yang punya rantai fallback berlapis ({@code nomorUrut} &rarr; {@code nim} &rarr; {@code nama}
	 * &rarr; {@code keterangan}) beserta {@code try/catch} pengaman. Versi di sini jauh lebih
	 * pendek daripada saudara-saudaranya di keluarga {@code KelompokParameterTambahan*}, yang masih
	 * memakai pemeriksaan {@code instanceof} plus rantai fallback tersebut.</p>
	 *
	 * <p><b>Dua konsekuensi yang perlu disadari:</b></p>
	 * <ul>
	 *   <li><i>Cast telanjang.</i> Argumen langsung di-cast ke tipe ini tanpa {@code instanceof},
	 *   sehingga membandingkan dengan entity bertipe lain menimbulkan {@code ClassCastException}.
	 *   Pada praktiknya aman karena semua koleksi yang memakainya homogen
	 *   ({@code Set<KelompokParameterTambahanCatatanAdministrasi>}).</li>
	 *   <li><i>Nilai {@code 0} berarti "duplikat" bagi {@code TreeSet}.</i> Karena tidak ada kunci
	 *   pembanding cadangan, dua kelompok berbeda dengan nomor urut sama saling menggantikan di
	 *   dalam {@code TreeSet} — inilah sumber penciutan senyap yang dijelaskan pada dokumentasi
	 *   kelas. {@code compareTo} di sini juga tidak konsisten dengan {@code equals}.</li>
	 * </ul>
	 *
	 * <p>Dipanggil secara implisit oleh {@code TreeSet} pada
	 * {@code JenisCatatanAdministrasi.kelompokParameterTambahanCatatanAdministrasis} dan pada
	 * salinan yang dibangun {@code CatatanAdministrasiAction} sebelum merender formulir.</p>
	 *
	 * @param arg0 kelompok pembanding; harus bertipe
	 *             {@code KelompokParameterTambahanCatatanAdministrasi}
	 * @return negatif/nol/positif sesuai perbandingan nomor urut
	 * @throws ClassCastException bila {@code arg0} bukan instance kelas ini
	 * @throws NullPointerException bila {@code arg0} bernilai {@code null}
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		return getNomorUrut().compareTo(((KelompokParameterTambahanCatatanAdministrasi) arg0).getNomorUrut());
	}
}
