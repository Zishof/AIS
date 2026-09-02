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
 * Surat penunjukan <b>asisten dosen</b> dalam bentuk baris tabel: satu baris menyatakan
 * "mahasiswa X ditunjuk menjadi asisten pada kelas matakuliah {@link Perkuliahan} Y, dengan
 * kewenangan mengisi nilai / mengisi presensi sebagai berikut". Inilah satu-satunya sumber
 * data yang membuat seorang <b>mahasiswa</b> boleh menyentuh data akademik <b>mahasiswa lain</b>
 * (nilai dan presensi) di dalam AIS.
 *
 * <p>Memetakan tabel {@code public.mahasiswa_jadi_asisten}. Di layar, entity ini muncul sebagai
 * tab <i>"Asisten Dosen"</i> pada dialog detail Perkuliahan/Penilaian &mdash; tidak punya menu
 * sendiri, sehingga hak aksesnya sepenuhnya menempel pada menu induknya
 * ({@code PerkuliahanAction} / {@code PenilaianAction}).</p>
 *
 * <h3>Bukan entity finansial</h3>
 *
 * <p>Perlu ditegaskan karena namanya mudah disalahpahami: <b>tidak ada dimensi honor/upah</b> di
 * sini. Kelas ini tidak punya kolom nominal, tarif, jam kerja, maupun periode pembayaran, dan
 * tidak ada satu pun modul penggajian/keuangan di codebase yang membaca tabel ini. Seluruh
 * pembacanya berada di jalur akademik (penilaian, presensi, angket). Dampak salah-isi baris di
 * sini bersifat <b>akademik</b> (siapa boleh mengubah nilai/presensi), bukan finansial.</p>
 *
 * <h3>Tiga kewenangan yang dibawa satu baris</h3>
 *
 * <ul>
 *   <li>{@link #getAktif() aktif} &mdash; saklar induk. Baris tidak-aktif diabaikan oleh
 *   seluruh pemeriksaan kewenangan.</li>
 *   <li>{@link #getInputNilai() inputNilai} &mdash; boleh mengisi/mengubah <b>nilai</b> peserta
 *   kelas tersebut. Ditegakkan lewat {@link Perkuliahan#merupakanAsistenNilai(Mahasiswa)}, yang
 *   dipanggil dari {@code PenilaianAction} dan
 *   {@code DetailperkuliahanForPenilaianHelper}.</li>
 *   <li>{@link #getInputAbsen() inputAbsen} &mdash; boleh mengisi/mengubah <b>presensi</b>
 *   pertemuan kelas tersebut. Ditegakkan lewat
 *   {@link Perkuliahan#merupakanAsistenAbsen(Mahasiswa)}, yang dipanggil dari
 *   {@code AbsensiHelper} dan {@link Pertemuan}.</li>
 * </ul>
 *
 * <p>Selain ketiganya ada {@link Perkuliahan#merupakanAsisten(Mahasiswa)} dan
 * {@link Perkuliahan#ambilAsisten()} yang hanya menanyakan "terdaftar sebagai asisten aktif atau
 * tidak", tanpa melihat kewenangan spesifik.</p>
 *
 * <h3>Daur hidup baris</h3>
 *
 * <ol>
 *   <li><b>Dibuat</b> oleh {@code AmbilDataMahasiswaForAsistenHelper.prosesSave(Mahasiswa)}:
 *   pengguna mencentang mahasiswa pada dialog "Ambil Data Mahasiswa", lalu tiap centangan
 *   menghasilkan satu baris baru yang <b>hanya</b> diisi {@link #setMahasiswa(Mahasiswa)} dan
 *   {@link #setPerkuliahan(Perkuliahan)} &mdash; ketiga kolom kewenangan dibiarkan
 *   {@code null}. Konsekuensinya dibahas pada catatan 1 di bawah.</li>
 *   <li><b>Diubah</b> lewat grid {@code DetailperkuliahanForPenilaianHelper.loadDataDetailAsisten(...)}:
 *   tiga checkbox (Nilai/Absen/Aktif) yang masing-masing langsung memanggil
 *   {@code Common.refreshSaveOrUpdate(...)} pada event {@code onCheck}, kotak Keterangan
 *   ({@code Common.refreshUpdate(...)} pada {@code onChange}), dan tombol hapus.</li>
 *   <li><b>Dihapus</b> lewat tombol tempat sampah di grid yang sama
 *   ({@code Common.refreshDelete(...)}); tidak ada pembatalan lunak selain mematikan
 *   {@link #getAktif() aktif}.</li>
 * </ol>
 *
 * <h3>Dua lapis penyimpanan turunan</h3>
 *
 * <p>Baris tabel ini tidak pernah dibaca langsung oleh layar penegak kewenangan. Ada dua
 * salinan turunan yang harus tetap sinkron:</p>
 *
 * <ul>
 *   <li><b>Flag store per-kelas</b> {@code MahasiswaJadiAsisten_<id perkuliahan>} &mdash; berkas
 *   JSON berisi daftar id baris asisten milik satu {@link Perkuliahan}, dibaca
 *   {@link Perkuliahan#ambilMahasiswaJadiAsisten()} dan dibangun ulang oleh
 *   {@code Perkuliahan.reInitMahasiswaJadiAsisten(...)}. Sinkronisasinya dijaga
 *   {@code ais.database.hibernate.AuditListener}, yang memanggil
 *   {@code populateMahasiswaJadiAsisten(id)} pada simpan/ubah dan
 *   {@code removeMahasiswaJadiAsisten(id)} pada hapus. Perubahan yang menembus jalur SQL native
 *   (bukan lewat session Hibernate) tidak akan memicu listener ini dan membuat flag store
 *   basi.</li>
 *   <li><b>Cache global objek</b> &mdash; kelas ini didaftarkan di
 *   {@code InitData.initClasses(...)} sehingga <b>seluruh</b> barisnya dimuat ke memori saat
 *   aplikasi start, serta terdaftar di {@code DataUtil.CLASS_IZINKAN} (boleh masuk cache MapDB)
 *   dan {@code DataUtil.CLASS_JANGAN_DIBERSIHKAN} (tidak dibuang saat pembersihan berkala).
 *   Cache inilah yang dibaca {@code ChecklistPenilaianHelper.isMahasiswaAsisten(...)} dan
 *   {@code ChecklistPenilaianUmumOlehPesertaAction} lewat
 *   {@code ConstantValues.ambilBerdasarClass(MahasiswaJadiAsisten.class)} untuk menentukan
 *   apakah seorang mahasiswa berhak mengisi angket jalur asisten.</li>
 * </ul>
 *
 * <h3>Hal non-obvious yang perlu diketahui sebelum menyentuh kelas ini</h3>
 *
 * <ol>
 *   <li><b>Nilai bawaan tri-state-nya ASIMETRIS, dan berpihak pada "boleh".</b>
 *   {@link #getAktif()} dan {@link #getInputAbsen()} mengembalikan {@code true} bila kolomnya
 *   {@code null}; hanya {@link #getInputNilai()} yang mengembalikan {@code false}. Karena baris
 *   baru dari {@code prosesSave} tidak pernah mengisi ketiganya &mdash; dan {@code dynamicInsert}
 *   membuat kolom {@code null} tidak ikut di-{@code INSERT} &mdash; seorang mahasiswa yang baru
 *   saja dicentang sebagai asisten <b>langsung berwenang mengubah presensi</b> kelas itu tanpa
 *   ada seorang pun yang pernah mencentang kotak "Absen". Kewenangan nilai tidak ikut terbawa.
 *   Ini bukan kelalaian data entry di satu kampus, melainkan perilaku bawaan kode.</li>
 *   <li><b>{@code null} tidak selalu berarti "aktif" &mdash; tergantung jalur bacanya.</b>
 *   Getter di kelas ini, kriteria Hibernate di {@code NilaiMahasiswaAction}/
 *   {@code NewNilaiMahasiswaAction} ({@code isNull(aktif) OR aktif = true}), dan kriteria di
 *   {@code CommonReportHelper} memperlakukan {@code null} sebagai aktif; sebaliknya filter SQL
 *   native di {@code PenilaianAction} ({@code a.aktif = true}) dan di {@code CommonReportHelper}
 *   ({@code and a.aktif}) <b>membuang</b> baris ber-{@code aktif} {@code null}. Akibat yang bisa
 *   diamati pengguna: asisten yang baru ditunjuk melihat tab "Asisten" terbuka di layar nilainya,
 *   tetapi daftar kelas yang bisa dinilai tampil <b>kosong</b>, sampai ada yang meng-toggle
 *   centang "Aktif" (yang barulah menulis {@code true} eksplisit ke kolom).</li>
 *   <li><b>Penunjukan tidak berbatas waktu.</b> Tidak ada kolom tahun ajaran, semester, tanggal
 *   mulai, maupun tanggal berakhir; periode hanya tersirat dari {@link #getPerkuliahan()
 *   perkuliahan} yang ditunjuk. Kewenangan berlaku sampai barisnya dinonaktifkan atau dihapus
 *   secara manual, sehingga asisten sebuah kelas lampau tetap dapat menyentuh nilai/presensi
 *   kelas tersebut bertahun-tahun kemudian selama {@code Perkuliahan.getDikunci()} masih
 *   {@code null}. Tidak ada proses terjadwal yang menutup penunjukan di akhir semester.</li>
 *   <li><b>Tidak ada kunci unik (mahasiswa, perkuliahan).</b> Duplikat dicegah hanya secara lunak
 *   oleh pemeriksaan {@code merupakanAsisten(...)} di {@code prosesSave}, yang membaca flag store
 *   &mdash; bila flag store basi, baris kembar bisa terbentuk. Saat itu terjadi,
 *   {@link Perkuliahan#merupakanAsistenNilai(Mahasiswa)} dan
 *   {@link Perkuliahan#merupakanAsistenAbsen(Mahasiswa)} berhenti pada baris <b>pertama</b> yang
 *   cocok ({@code break}), sehingga baris kembar yang lebih permisif bisa saja tidak pernah
 *   terpakai &mdash; atau justru menang &mdash; tergantung urutan kunci di berkas JSON flag
 *   store, yang tidak dijamin.</li>
 *   <li><b>{@link #toString()} mengembalikan {@link #getKeterangan() keterangan} mentah</b>,
 *   yang boleh {@code null} dan tidak pernah divalidasi. Hasilnya bukan pengenal yang berguna
 *   (bukan NIM/nama), dan berpotensi {@code null} bila dipakai sebagai label komponen ZK. Grid
 *   asisten sendiri tidak memakainya &mdash; ia merender NIM/nama lewat renderer sendiri.</li>
 *   <li><b>Getter relasi memanggil {@code check(...)}</b>
 *   ({@link GeneralValueObject#check(Object)}) untuk meresolusi proxy lazy lalu
 *   <b>menugaskan hasilnya kembali ke field</b>. Penulisan balik itu mengenai field di memori,
 *   bukan basis data; resolusi yang gagal bersifat senyap dan mengembalikan argumen apa adanya.
 *   Tidak ada getter di kelas ini yang menulis ke basis data, menutup session Hibernate, atau
 *   merusak nilai field lain &mdash; ketiga getter {@code Boolean} pun hanya menormalkan nilai
 *   yang <i>dikembalikan</i>, tanpa menyentuh field.</li>
 *   <li><b>Deklarasi ulang {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} BUKAN
 *   duplikasi yang bisa dihapus.</b> {@link GeneralValueObject} adalah POJO abstrak biasa &mdash;
 *   bukan {@code @Entity} maupun {@code @MappedSuperclass} &mdash; sehingga Hibernate sama sekali
 *   tidak memetakan properti kelas induk. Setiap entity turunan wajib mendeklarasikan sendiri
 *   kolom-kolom itu agar terpetakan.</li>
 *   <li><b>Tidak ada jejak pembuat.</b> Ada {@code @PreUpdate} ({@link #onUpdate()}) tetapi tidak
 *   ada {@code @PrePersist}, sehingga {@code oleh}/{@code olehId} hanya terisi saat baris
 *   di-<i>update</i>, bukan saat penunjukan pertama kali dibuat. Untuk baris yang dibuat lalu
 *   tidak pernah disunting, kolom "oleh" tetap kosong &mdash; jejak pembuatnya hanya ada di tabel
 *   audit Envers ({@code @Audited}).</li>
 *   <li><b>Tiga kolom kewenangan tidak punya {@code @Column}.</b> {@code inputNilai},
 *   {@code inputAbsen} dan {@code aktif} mengandalkan penamaan bawaan
 *   {@code ais.database.hibernate.MyNamingStrategy}, yang tidak mengubah apa pun untuk nama kolom
 *   (hanya meng-override {@code tableName} dan itu pun tanpa efek). Nama kolomnya sama persis
 *   dengan nama properti; mengganti nama properti = mengganti nama kolom.</li>
 *   <li><b>Komentar generator "Bank generated by hbm2java" salah nama</b> (sisa salin-tempel
 *   template generator Apr 2010); tidak ada hubungannya dengan entity {@code Bank}.</li>
 * </ol>
 *
 * <h3>Catatan kontrol akses pada layar pengelolanya</h3>
 *
 * <p>Dicatat di sini karena entity ini adalah pemberi wewenang, bukan sekadar data: berkas
 * {@code ais/action/master/helper/DetailperkuliahanForPenilaianHelper.java} &mdash; tempat baris
 * asisten ditambah, dihapus, dan ketiga kewenangannya dicentang &mdash; <b>tidak memanggil
 * {@code CommonPrivilages}/{@code checkPrevilages}/{@code doCheckSecurity} sama sekali</b> di
 * seluruh isinya. Satu-satunya penjagaan adalah penyembunyian tab bagi pengguna bertipe mahasiswa
 * ({@code tab1AsistenMahasiswa.setVisible(tbmuser.getMahasiswa() == null)}, dan
 * {@code btnTab.setVisibleTombol(1, ...)} di {@code PerkuliahanAction}), sedangkan toolbar
 * "Ambil Mahasiswa" di dalam panelnya hanya disyaratkan {@code tbmuser != null}. Artinya
 * pemberian wewenang ubah-nilai kepada seorang mahasiswa tidak melewati gerbang
 * {@code CommonPrivilages.UPDATE} mana pun. Pola ini sama dengan temuan "inversi hak akses" yang
 * sudah tercatat di inisiatif dokumentasi ini; jangan diperbaiki dari kelas entity.</p>
 *
 * <h3>Pengelompokan anggota kelas</h3>
 *
 * <ul>
 *   <li><b>Jejak audit</b>: {@link #getOleh()}/{@link #setOleh(String)},
 *   {@link #getOlehId()}/{@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 *   <li><b>Identitas</b>: {@link #getId()}/{@link #setId(Long)}, {@link #toString()},
 *   {@link #MahasiswaJadiAsisten()}.</li>
 *   <li><b>Pasangan yang ditunjuk</b>: {@link #getMahasiswa()}/{@link #setMahasiswa(Mahasiswa)}
 *   dan {@link #getPerkuliahan()}/{@link #setPerkuliahan(Perkuliahan)}.</li>
 *   <li><b>Kewenangan</b>: {@link #getAktif()}/{@link #setAktif(Boolean)},
 *   {@link #getInputNilai()}/{@link #setInputNilai(Boolean)},
 *   {@link #getInputAbsen()}/{@link #setInputAbsen(Boolean)}.</li>
 *   <li><b>Catatan bebas</b>: {@link #getKeterangan()}/{@link #setKeterangan(String)}.</li>
 * </ul>
 *
 * <p>Tidak ada method bisnis maupun query statis di kelas ini &mdash; seluruh logika pencarian,
 * penegakan kewenangan, dan penyimpanan berada di {@link Perkuliahan} serta helper-helper UI di
 * atas.</p>
 *
 * @see Perkuliahan#merupakanAsisten(Mahasiswa)
 * @see Perkuliahan#merupakanAsistenNilai(Mahasiswa)
 * @see Perkuliahan#merupakanAsistenAbsen(Mahasiswa)
 * @see Perkuliahan#ambilAsisten()
 * @see Perkuliahan#ambilMahasiswaJadiAsisten()
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "mahasiswa_jadi_asisten")

public class MahasiswaJadiAsisten extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java, diwarisi dari {@link java.io.Serializable} lewat
	 * {@link GeneralValueObject}. Nilainya dibangkitkan generator dan tidak boleh diubah selama
	 * struktur field tidak berubah, agar object yang sudah ter-serialisasi (mis. di sesi ZK yang
	 * di-passivate) tetap dapat dibaca kembali.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama baris, dibangkitkan basis data ({@code IDENTITY}). Lihat {@link #getId()}. */
	private Long id;
	/**
	 * Nama pengguna terakhir yang mengubah baris ini, diisi otomatis oleh
	 * {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}. Kosong untuk baris yang belum
	 * pernah disunting sejak dibuat (tidak ada {@code @PrePersist}).
	 */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; pasangan teknis dari {@link #oleh}. */
	private String olehId;

	/**
	 * Id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah di-<i>update</i>.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna pengubah terakhir.
	 *
	 * <p><b>Menolak nilai kosong secara senyap:</b> {@code null} maupun string berisi spasi saja
	 * diabaikan sepenuhnya sehingga nilai lama dipertahankan. Ini disengaja &mdash; mencegah jejak
	 * audit yang sudah ada tertimpa nilai hampa oleh jalur penyalinan properti generik.</p>
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null} atau kosong.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna pengubah terakhir.
	 *
	 * <p>Berperilaku sama dengan {@link #setOlehId(String)}: nilai {@code null} atau kosong
	 * diabaikan secara senyap agar jejak audit lama tidak terhapus.</p>
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau kosong.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila baris belum pernah di-<i>update</i>.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum baris ini
	 * di-{@code UPDATE}, dan meneruskan object ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #oleh}, {@link #olehId}, dan {@link #tanggal_dirubah} dari pengguna sesi berjalan.
	 *
	 * <p>Tidak ada pasangan {@code @PrePersist}, sehingga ketiga kolom itu tetap kosong untuk
	 * baris yang baru dibuat dan belum pernah disunting.</p>
	 *
	 * <p>Pada baris kode yang sama dideklarasikan field {@code tanggal_dirubah} dengan nilai awal
	 * {@code ais.ui.util.WaktuUtil.getDate()} &mdash; waktu server saat <b>object dibuat di
	 * memori</b>, bukan waktu simpan. Tata letak satu baris ini warisan skrip penyisipan massal;
	 * jangan dipisah tanpa alasan agar diff terhadap entity sejenis tetap sebanding.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan waktu perubahan terakhir baris ini.
	 *
	 * <p>Berbeda dari {@link #setOleh(String)}/{@link #setOlehId(String)}, setter ini
	 * <b>menerima</b> {@code null} tanpa penyaringan.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Waktu perubahan terakhir baris ini, dipetakan sebagai {@code TIMESTAMP}.
	 *
	 * @return waktu perubahan terakhir; untuk object baru berisi waktu pembuatan object di memori.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini, yaitu {@link #getKeterangan() keterangan} mentah.
	 *
	 * <p><b>Bukan pengenal yang berguna</b> &mdash; tidak memuat NIM, nama mahasiswa, maupun kelas
	 * yang ditunjuk, dan <b>boleh {@code null}</b> karena kolom keterangan tidak wajib diisi.
	 * Jangan diandalkan sebagai label komponen ZK; grid asisten merender NIM dan nama lewat
	 * renderer-nya sendiri, bukan lewat method ini.</p>
	 *
	 * @return isi keterangan apa adanya, bisa {@code null}.
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Kelas matakuliah tempat mahasiswa ini ditunjuk menjadi asisten. Wajib terisi
	 * ({@code nullable = false}). Lihat {@link #getPerkuliahan()}.
	 */
	private Perkuliahan perkuliahan;
	/**
	 * Mahasiswa yang ditunjuk menjadi asisten. Wajib terisi ({@code nullable = false}).
	 * Lihat {@link #getMahasiswa()}.
	 */
	private Mahasiswa mahasiswa;
	/**
	 * Catatan bebas atas penunjukan ini (mis. alasan atau ruang lingkup tugas). Tidak dipakai
	 * logika mana pun, tetapi menjadi hasil {@link #toString()}.
	 */
	private String keterangan;

	/**
	 * Kewenangan mengisi/mengubah nilai. Tri-state: {@code null} dibaca sebagai
	 * <b>{@code false}</b> oleh {@link #getInputNilai()}.
	 */
	private Boolean inputNilai;
	/**
	 * Kewenangan mengisi/mengubah presensi. Tri-state: {@code null} dibaca sebagai
	 * <b>{@code true}</b> oleh {@link #getInputAbsen()} &mdash; lihat catatan 1 pada javadoc
	 * kelas.
	 */
	private Boolean inputAbsen;
	/**
	 * Saklar induk penunjukan. Tri-state: {@code null} dibaca sebagai <b>{@code true}</b> oleh
	 * {@link #getAktif()}, tetapi dianggap tidak aktif oleh sebagian filter SQL native &mdash;
	 * lihat catatan 2 pada javadoc kelas.
	 */
	private Boolean aktif;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Seluruh field dibiarkan pada nilai
	 * bawaannya ({@code null}, kecuali {@link #tanggal_dirubah} yang langsung diisi waktu
	 * pembuatan object). Dipakai baik oleh Hibernate saat memuat baris maupun oleh
	 * {@code AmbilDataMahasiswaForAsistenHelper.prosesSave(Mahasiswa)} saat membuat penunjukan
	 * baru.
	 */
	public MahasiswaJadiAsisten() {
	}

	/**
	 * Kunci utama baris penunjukan ini.
	 *
	 * <p>Dibangkitkan basis data ({@code IDENTITY}) dan ditandai {@code insertable = false},
	 * sehingga nilainya baru tersedia setelah {@code INSERT} berhasil. Id inilah yang disimpan ke
	 * flag store {@code MahasiswaJadiAsisten_<id perkuliahan>} oleh
	 * {@code Perkuliahan.populateMahasiswaJadiAsisten(Long)}.</p>
	 *
	 * @return id baris, atau {@code null} untuk object yang belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama baris.
	 *
	 * <p>Umumnya hanya dipanggil Hibernate; pemanggilan manual pada object yang sudah tersimpan
	 * akan membuat Hibernate memperlakukannya sebagai baris lain.</p>
	 *
	 * @param id kunci utama baru.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Catatan bebas atas penunjukan ini.
	 *
	 * <p>Diisi lewat kotak teks kolom "Keterangan" pada grid asisten dan tidak pernah dibaca
	 * logika bisnis mana pun &mdash; kecuali secara tidak langsung sebagai hasil
	 * {@link #toString()}.</p>
	 *
	 * @return keterangan, bisa {@code null}.
	 */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan catatan bebas penunjukan.
	 *
	 * <p>Dipanggil dari event {@code onChange} kotak teks Keterangan di grid asisten, yang
	 * langsung menyusulkan {@code Common.refreshUpdate(...)} sehingga perubahan tersimpan tanpa
	 * tombol Simpan.</p>
	 *
	 * @param keterangan catatan bebas; {@code null} diterima apa adanya.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan kelas matakuliah tujuan penunjukan.
	 *
	 * <p>Dipanggil {@code AmbilDataMahasiswaForAsistenHelper.prosesSave(Mahasiswa)} saat baris
	 * penunjukan dibuat. Mengubah nilai ini pada baris yang sudah tersimpan akan memindahkan
	 * penunjukan ke kelas lain, tetapi flag store kelas <b>lama</b> tidak dibersihkan &mdash;
	 * {@code AuditListener} hanya memanggil {@code populateMahasiswaJadiAsisten} untuk kelas yang
	 * baru.</p>
	 *
	 * @param perkuliahan kelas matakuliah tujuan; wajib terisi karena kolomnya
	 *        {@code nullable = false}.
	 */
	public void setPerkuliahan(Perkuliahan perkuliahan) {
		this.perkuliahan = perkuliahan;
	}

	/**
	 * Kelas matakuliah tempat mahasiswa ini ditunjuk menjadi asisten.
	 *
	 * <p>Relasi lazy, sehingga getter meresolusi proxy lewat
	 * {@link GeneralValueObject#check(Object)} dan <b>menugaskan hasilnya kembali ke field</b>
	 * (penulisan balik ke memori, bukan ke basis data). Bila resolusi gagal &mdash; misalnya
	 * session sudah tertutup dan baris tidak ada di cache &mdash; {@code check} mengembalikan
	 * argumen apa adanya secara senyap, jadi hasilnya masih bisa berupa proxy yang belum
	 * terinisialisasi.</p>
	 *
	 * <p>Dibaca antara lain oleh {@code AuditListener} untuk menentukan flag store kelas mana yang
	 * harus disegarkan, dan oleh {@code ChecklistPenilaianHelper.isMahasiswaAsisten(...)} untuk
	 * mencocokkan tahun ajaran/semester penugasan.</p>
	 *
	 * @return kelas matakuliah yang ditunjuk.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "perkuliahan", nullable = false)
	public Perkuliahan getPerkuliahan() {
		perkuliahan = check(perkuliahan);
		return perkuliahan;
	}

	/**
	 * Menetapkan mahasiswa yang ditunjuk menjadi asisten.
	 *
	 * <p>Dipanggil {@code AmbilDataMahasiswaForAsistenHelper.prosesSave(Mahasiswa)} dengan
	 * mahasiswa yang dicentang di dialog pemilihan. Tidak ada validasi bahwa mahasiswa tersebut
	 * berstatus aktif, terdaftar di kelas ini, atau bukan peserta kelas yang sama &mdash;
	 * penyaringan status hanya dilakukan oleh query pencarian di dialog pemilihan, bukan di
	 * sini.</p>
	 *
	 * @param mahasiswa mahasiswa yang ditunjuk; wajib terisi karena kolomnya
	 *        {@code nullable = false}.
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Mahasiswa yang ditunjuk menjadi asisten.
	 *
	 * <p>Relasi lazy dengan perilaku {@code check(...)} yang sama seperti
	 * {@link #getPerkuliahan()}: proxy diresolusi lalu ditugaskan kembali ke field, dan kegagalan
	 * resolusi bersifat senyap.</p>
	 *
	 * <p>Dibaca oleh seluruh pemeriksaan kewenangan di {@link Perkuliahan} (dicocokkan
	 * berdasarkan {@code getId()}, bukan {@code equals}) serta oleh renderer grid asisten yang
	 * menampilkan NIM dan nama.</p>
	 *
	 * @return mahasiswa yang ditunjuk.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = false)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * Apakah asisten ini berwenang mengisi/mengubah <b>nilai</b> peserta kelas yang ditunjuk.
	 *
	 * <p>Satu-satunya kewenangan yang bawaannya <b>tertutup</b>: kolom {@code null} dibaca sebagai
	 * {@code false}, sehingga baris penunjukan yang baru dibuat tidak memberi akses nilai sampai
	 * ada yang mencentang kotak "Nilai" di grid asisten. Bandingkan dengan
	 * {@link #getInputAbsen()} yang bawaannya terbuka.</p>
	 *
	 * <p>Dibaca {@link Perkuliahan#merupakanAsistenNilai(Mahasiswa)}, yang menjadi gerbang tombol
	 * format nilai di {@code PenilaianAction} dan kolom-kolom nilai di
	 * {@code DetailperkuliahanForPenilaianHelper}. Method ini tidak menulis apa pun; normalisasi
	 * {@code null} hanya berlaku pada nilai yang dikembalikan, field tetap {@code null}.</p>
	 *
	 * @return {@code true} bila boleh mengisi nilai; {@code false} bila tidak, termasuk saat kolom
	 *         masih {@code null}.
	 */
	public Boolean getInputNilai() {
		return inputNilai == null ? false : inputNilai;
	}

	/**
	 * Menetapkan kewenangan mengisi nilai.
	 *
	 * <p>Dipanggil dari event {@code onCheck} checkbox "Nilai" di grid asisten, yang langsung
	 * menyusulkan {@code Common.refreshSaveOrUpdate(...)} &mdash; jadi satu klik centang langsung
	 * memberi/mencabut wewenang ubah nilai, tanpa tombol Simpan dan tanpa gerbang
	 * {@code CommonPrivilages} (lihat catatan kontrol akses pada javadoc kelas).</p>
	 *
	 * @param inputNilai {@code true} untuk memberi wewenang, {@code false} untuk mencabutnya;
	 *        {@code null} diterima apa adanya dan akan dibaca sebagai {@code false}.
	 */
	public void setInputNilai(Boolean inputNilai) {
		this.inputNilai = inputNilai;
	}

	/**
	 * Apakah asisten ini berwenang mengisi/mengubah <b>presensi</b> pertemuan kelas yang ditunjuk.
	 *
	 * <p><b>Bawaannya terbuka:</b> kolom {@code null} dibaca sebagai {@code true}. Karena baris
	 * penunjukan baru tidak pernah mengisi kolom ini, seorang mahasiswa yang baru saja dicentang
	 * sebagai asisten langsung memperoleh wewenang mengubah presensi kelas tersebut tanpa ada yang
	 * pernah mencentang kotak "Absen".</p>
	 *
	 * <p>Dibaca {@link Perkuliahan#merupakanAsistenAbsen(Mahasiswa)}, yang menjadi gerbang
	 * pengubahan presensi di {@code AbsensiHelper} dan {@link Pertemuan}. Method ini tidak menulis
	 * apa pun; field tetap {@code null}.</p>
	 *
	 * @return {@code true} bila boleh mengisi presensi (termasuk saat kolom masih {@code null});
	 *         {@code false} hanya bila kolom berisi {@code false} eksplisit.
	 */
	public Boolean getInputAbsen() {
		return inputAbsen == null ? true : inputAbsen;
	}

	/**
	 * Menetapkan kewenangan mengisi presensi.
	 *
	 * <p>Dipanggil dari event {@code onCheck} checkbox "Absen" di grid asisten dengan penyimpanan
	 * langsung ({@code Common.refreshSaveOrUpdate(...)}), sama seperti
	 * {@link #setInputNilai(Boolean)}.</p>
	 *
	 * @param inputAbsen {@code true} untuk memberi wewenang, {@code false} untuk mencabutnya;
	 *        {@code null} diterima apa adanya dan akan dibaca sebagai {@code true}.
	 */
	public void setInputAbsen(Boolean inputAbsen) {
		this.inputAbsen = inputAbsen;
	}

	/**
	 * Saklar induk penunjukan: apakah baris ini masih berlaku.
	 *
	 * <p>Kolom {@code null} dibaca sebagai {@code true}, sehingga baris penunjukan yang baru dibuat
	 * langsung berlaku. <b>Perhatikan ketidakseragaman jalur baca</b> yang dijelaskan pada catatan
	 * 2 javadoc kelas: kriteria Hibernate di {@code NilaiMahasiswaAction}/
	 * {@code NewNilaiMahasiswaAction} dan {@code CommonReportHelper} mengikuti aturan
	 * "{@code null} = aktif" seperti getter ini, tetapi filter SQL native di
	 * {@code PenilaianAction} ({@code a.aktif = true}) dan {@code CommonReportHelper}
	 * ({@code and a.aktif}) justru membuang baris ber-{@code null}. Getter ini tidak menulis apa
	 * pun; field tetap {@code null} sampai centang "Aktif" di-toggle.</p>
	 *
	 * <p>Dibaca {@link Perkuliahan#merupakanAsisten(Mahasiswa)},
	 * {@link Perkuliahan#merupakanAsistenNilai(Mahasiswa)},
	 * {@link Perkuliahan#merupakanAsistenAbsen(Mahasiswa)},
	 * {@link Perkuliahan#ambilAsisten()}, dan {@code ChecklistPenilaianHelper} (yang memakai
	 * {@code Boolean.TRUE.equals(...)} atas hasil getter ini).</p>
	 *
	 * @return {@code true} bila penunjukan masih berlaku (termasuk saat kolom masih {@code null});
	 *         {@code false} hanya bila kolom berisi {@code false} eksplisit.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan status berlaku/tidaknya penunjukan.
	 *
	 * <p>Dipanggil dari event {@code onCheck} checkbox "Aktif" di grid asisten dengan penyimpanan
	 * langsung ({@code Common.refreshSaveOrUpdate(...)}). Menonaktifkan baris adalah satu-satunya
	 * cara mencabut penunjukan tanpa menghapus barisnya, sekaligus &mdash; karena efek samping
	 * catatan 2 javadoc kelas &mdash; cara paling andal membuat kolom {@code aktif} berisi
	 * {@code true} eksplisit adalah dengan meng-uncheck lalu meng-check ulang kotak ini.</p>
	 *
	 * @param aktif {@code true} untuk mengaktifkan, {@code false} untuk menonaktifkan;
	 *        {@code null} diterima apa adanya dan akan dibaca sebagai {@code true}.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
