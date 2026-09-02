package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

/**
 * Master data <b>jenis/kategori berkas</b> (dokumen) — tabel {@code public.berkas}.
 *
 * <p>Entity ini adalah katalog/taksonomi nama-nama berkas yang dikenal institusi (mis. "KTP",
 * "Ijazah", "Kartu Keluarga", "Berkas Kepegawaian"), disusun sebagai <b>pohon</b> lewat relasi
 * self-reference {@link #getParent()}: satu baris boleh menjadi induk (kategori) dari baris lain
 * (jenis berkas spesifik). Kedalaman pohon tidak dibatasi kode.</p>
 *
 * <p><b>PENTING — entity ini TIDAK menyimpan file apa pun.</b> Tidak ada kolom blob, path
 * penyimpanan, nama file asli, ukuran, maupun content-type di sini; yang disimpan hanyalah
 * <i>nama jenis</i> berkas beserta metadata lingkupnya. Pembawa isi lampiran yang sesungguhnya
 * di AIS adalah {@link ais.database.model.file.LampiranLain} (dan keluarga {@code
 * ais.database.model.file}), bukan kelas ini. Konsekuensinya kelas ini <b>tidak punya jalur
 * unduh</b> sama sekali: tidak ada satu pun servlet, API, atau report yang menyentuhnya (lihat
 * "Konsumen" di bawah), sehingga entity ini <b>bukan</b> vektor bagi kelemahan IDOR pada
 * {@code ais.action.servlet.AmbilLampiran} yang didokumentasikan repo di
 * {@code SECURITY_FINDING_AmbilLampiran_IDOR.md}.</p>
 *
 * <p><b>Bukan bagian dari alur verifikasi kelengkapan dokumen.</b> Alur "kelengkapan berkas"
 * pendaftar (PMB/PSB/rekrutmen pegawai) memakai keluarga entity yang sama sekali terpisah —
 * master syaratnya {@link VerifikasiKelengkapanCalonMahasiswa} (punya flag {@code wajib},
 * {@code aktif}, {@code verifikasi}, {@code wajibUploadSebelumUjian}, dsb.) dan baris per
 * pendaftarnya {@code BiodataCalonMahasiswaPunyaVerifikasiBerkas} /
 * {@code CalonSiswaPunyaVerifikasiBerkas} / {@code CalonPegawaiPunyaVerifikasiBerkas} yang
 * menautkan file lewat {@link ais.database.model.file.LampiranLain}. Kelas {@code Berkas} adalah
 * master "kembar" yang paralel dengan master syarat tersebut namun tidak dirujuk olehnya.</p>
 *
 * <p><b>Konsumen (lingkup pemakaian sangat sempit).</b> Hanya tiga kelas di seluruh codebase yang
 * mengimpor entity ini, dan ketiganya adalah satu layar yang sama:</p>
 * <ul>
 * <li>{@code ais.action.master.BerkasAction} — layar CRUD master ({@code berkas.zul}), dua tab:
 * tab pohon dan tab grid.</li>
 * <li>{@code ais.action.master.helper.AmbilDataBerkasBanbox} — komponen picker (bandbox) berisi
 * pohon berkas + tab "Berkas Sering Dapakai".</li>
 * <li>{@code ais.action.master.helper.util.BerkasTreeModel} — {@code TreeModel} ZK yang memuat
 * anak-anak sebuah node lewat {@code Criteria} atas properti {@code parent}.</li>
 * </ul>
 * <p><b>Fitur yatim:</b> tidak ada entity lain di {@code ais.database.model} yang memiliki
 * foreign key ke tabel ini, dan picker {@code AmbilDataBerkasBanbox} hanya dipakai oleh
 * {@code BerkasAction} sendiri (untuk mengisi kolom "Induk" pada form editnya). Dengan kata lain
 * data yang dikelola layar ini tidak pernah dikonsumsi modul lain; kelihatannya sebuah fitur yang
 * tidak pernah selesai disambungkan atau sudah digantikan oleh master syarat verifikasi di atas.
 * Ini juga membuat {@link #getJmlDipakai()} praktis kehilangan makna aslinya (lihat method
 * tersebut).</p>
 *
 * <p><b>Pengelompokan anggota:</b></p>
 * <ul>
 * <li><i>Identitas &amp; audit</i> — {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()},
 * {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 * <li><i>Atribut deskriptif</i> — {@link #getNama()} (satu-satunya kolom {@code NOT NULL} selain
 * id), {@link #getKeterangan()}.</li>
 * <li><i>Lingkup berlakunya jenis berkas</i> — {@link #getFakultas()}, {@link #getJurusan()},
 * {@link #getProgram()}, {@link #getTahunAkademik()}, {@link #getJenisSemester()}. Semuanya
 * opsional; {@code null}/kosong ditampilkan UI sebagai "Semua". Perlu dicatat: nilai-nilai ini
 * murni deskriptif — tidak ada kode di codebase yang memakainya untuk menyaring atau membatasi
 * apa pun.</li>
 * <li><i>Hierarki</i> — {@link #getParent()}.</li>
 * <li><i>Statistik pemakaian</i> — {@link #getJmlDipakai()}.</li>
 * <li><i>Identitas object</i> — {@link #toString()}, {@link #equals(Object)}.</li>
 * </ul>
 *
 * <p><b>Anotasi kelas.</b> {@code @Audited} (Hibernate Envers) merekam seluruh riwayat perubahan
 * ke tabel audit; {@code dynamicInsert}/{@code dynamicUpdate} membuat Hibernate hanya menyertakan
 * kolom yang benar-benar terisi/berubah pada {@code INSERT}/{@code UPDATE}. Strategi penamaan
 * kolom proyek adalah {@code ais.database.hibernate.MyNamingStrategy} (turunan
 * {@code DefaultNamingStrategy}), sehingga properti tanpa {@code @Column} dipetakan ke kolom
 * bernama sama persis dengan nama properti.</p>
 *
 * <p><b>Redeklarasi {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} BUKAN bug.</b>
 * {@link GeneralValueObject} adalah POJO abstrak biasa — bukan {@code @Entity} maupun
 * {@code @MappedSuperclass} — sehingga Hibernate <b>tidak</b> memetakan properti milik induk.
 * Setiap entity AIS karena itu wajib mendeklarasikan ulang field audit standar tersebut agar
 * ikut tersimpan. Lihat {@link GeneralValueObject} untuk penjelasan lengkap pola ini.</p>
 *
 * <p><b>Catatan/kejanggalan yang diamati (didokumentasikan apa adanya, tidak diperbaiki di
 * sini):</b></p>
 * <ol>
 * <li><b>Komentar generator salah salin-tempel.</b> Javadoc asli hasil hbm2java berbunyi "Bank
 * generated by hbm2java" padahal entity ini bukan {@code Bank} — pola salah salin yang sama
 * pernah ditemukan pada entity AIS lain.</li>
 * <li><b>Properti {@code kode} tidak dipetakan.</b> {@code getKode()}/{@code setKode()} diwarisi
 * dari {@link GeneralValueObject} dan — karena induknya bukan {@code @MappedSuperclass} —
 * <b>tidak</b> menjadi properti Hibernate bagi {@code Berkas}, juga tidak pernah disimpan ke
 * database. Namun {@code BerkasAction.onSearchDefault} masih menyusun query dengan
 * {@code Order.asc("kode")} dan {@code Restrictions.ilike("kode", ...)}, serta membaca komponen
 * {@code searchkode} yang sudah tidak ada lagi di {@code berkas.zul}. Renderer grid-nya juga
 * masih menuliskan satu sel {@code getKode()} tambahan di depan sehingga jumlah sel melebihi
 * jumlah kolom zul. Sisa-sisa kolom "Kode" yang pernah dihapus dari layar tapi tidak ikut
 * dibersihkan dari kode Java.</li>
 * <li><b>{@link #getProgram()} tidak pernah bisa diisi lewat UI.</b> Form tambah/ubah di
 * {@code BerkasAction.init} tidak menyediakan input Program dan {@code onSave} tidak pernah
 * memanggil {@link #setProgram(String)}; kolomnya hanya ditampilkan (selalu "Semua").</li>
 * <li><b>{@link #equals(Object)} di-override tanpa {@code hashCode()}</b> — lihat method tersebut
 * untuk konsekuensinya.</li>
 * <li><b>{@link #toString()} menyertakan id numerik</b>, dan itu berinteraksi buruk dengan
 * pencarian nama-persis di picker — lihat method tersebut.</li>
 * </ol>
 *
 * @see GeneralValueObject
 * @see ais.database.model.file.LampiranLain
 * @see VerifikasiKelengkapanCalonMahasiswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "berkas")

public class Berkas extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi. Nilai ini satu "keluarga" dengan
	 * {@code BiodataCalonMahasiswaPunyaVerifikasiBerkas} dan
	 * {@link VerifikasiKelengkapanCalonMahasiswa} (hanya berbeda satu digit), jejak bahwa
	 * berkas-berkas sumber ini pernah disalin-tempel satu sama lain.
	 */
	private static final long serialVersionUID = 2463821537548439808L;

	/**
	 * Kunci primer, di-generate database (IDENTITY). Dideklarasikan ulang dari
	 * {@link GeneralValueObject} karena induk bukan {@code @MappedSuperclass}.
	 */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris ini; diisi otomatis oleh interceptor audit. */
	private String oleh;

	/** Id pengguna terakhir yang mengubah baris ini; diisi otomatis oleh interceptor audit. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna audit.
	 *
	 * @return id pengguna yang terakhir mengubah baris ini; boleh {@code null}
	 * @see GeneralValueObject#getOlehId()
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna audit.
	 *
	 * <p>Nilai {@code null} atau string kosong/spasi <b>ditolak diam-diam</b> (method langsung
	 * {@code return} tanpa mengubah apa pun dan tanpa melempar exception). Efeknya: sekali terisi,
	 * jejak audit tidak dapat dikosongkan kembali lewat setter ini. Ini pola standar seluruh
	 * entity AIS, bukan kekhususan {@code Berkas}.</p>
	 *
	 * @param olehId id pengguna audit; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks entity: {@code "<id> - <nama>"}.
	 *
	 * <p>Meng-override {@link GeneralValueObject#toString()}. Perhatikan dua hal: (a) nilai yang
	 * dipakai adalah <b>field</b> {@code nama} langsung, bukan {@link #getNama()}, sehingga spasi
	 * di ujung tidak dipangkas di sini; (b) id numerik ikut ditulis di depan.</p>
	 *
	 * <p><b>Konsekuensi yang teramati:</b> {@code AmbilDataBerkasBanbox} mengisi teks bandbox
	 * dengan hasil method ini setelah pengguna memilih dari pohon. Bila pengguna lalu menekan
	 * Enter pada bandbox tersebut, handler {@code onOK} mencari berkas dengan
	 * {@code ilike("nama", <teks bandbox>, EXACT)} — yang isinya kini "12 - KTP", bukan "KTP" —
	 * sehingga pencarian pasti gagal dan muncul peringatan "Berkas dengan nama = ... tidak
	 * ditemukan" untuk berkas yang jelas-jelas baru saja dipilih.</p>
	 *
	 * <p>Bila {@code id} masih {@code null} (entity belum tersimpan) hasilnya berawalan
	 * {@code "null - "}.</p>
	 *
	 * @return teks gabungan id dan nama
	 */
	public String toString() {
		return id + " - " + nama;
	}

	/**
	 * Kesetaraan berdasarkan {@link #getId()} saja: dua object dianggap sama bila keduanya
	 * bertipe {@code Berkas} dan id-nya sama.
	 *
	 * <p>Meng-override {@link GeneralValueObject#equals(Object)}. Tiga batasan yang perlu
	 * disadari pemanggil:</p>
	 * <ul>
	 * <li><b>Berpotensi {@code NullPointerException}.</b> Yang dipanggil adalah
	 * {@code berkas.id.equals(id)}, yaitu id milik <i>argumen</i>. Bila argumen adalah entity
	 * yang belum tersimpan (id {@code null}) — misalnya hasil {@code clone()} lalu
	 * {@code setId(null)} seperti pada tombol "Tambah Data"/"Copy Data" di
	 * {@code BerkasAction} — pemanggilan ini melempar NPE, bukan mengembalikan {@code false}.</li>
	 * <li><b>{@code hashCode()} tidak di-override</b> di kelas ini maupun di
	 * {@link GeneralValueObject}, sehingga kontrak {@code equals}/{@code hashCode} dilanggar.
	 * Praktisnya, {@code Set<Berkas>} (dipakai {@code BerkasTreeModel.generateAllChildren})
	 * tetap memakai identitas object untuk pengelompokan bucket, jadi dua instance berbeda
	 * dengan id sama masih bisa masuk berdua ke dalam set yang sama.</li>
	 * <li>Perbandingan dengan {@code null} atau dengan tipe lain mengembalikan {@code false}
	 * secara aman.</li>
	 * </ul>
	 *
	 * @param object object pembanding; boleh {@code null}
	 * @return {@code true} bila argumen adalah {@code Berkas} dengan id sama
	 */
	public boolean equals(Object object) {
		if (object == null) {
			return false;
		}
		if (object instanceof Berkas) {
			Berkas berkas = (Berkas) object;
			return berkas.id.equals(id);
		}
		return false;
	}

	/**
	 * Menyetel nama pengguna audit.
	 *
	 * <p>Menolak {@code null}/kosong diam-diam dengan alasan yang sama seperti
	 * {@link #setOlehId(String)}.</p>
	 *
	 * @param oleh nama pengguna audit; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna audit.
	 *
	 * @return nama pengguna yang terakhir mengubah baris ini; boleh {@code null}
	 * @see GeneralValueObject#getOleh()
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Implementasi wajib {@link GeneralValueObject#onUpdate()} — satu-satunya method
	 * {@code abstract} yang harus dipenuhi setiap entity AIS. Dipanggil container JPA sebagai
	 * callback {@link javax.persistence.PreUpdate} tepat sebelum {@code UPDATE} dikirim ke
	 * database, lalu meneruskan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang mengisi
	 * {@code tanggal_dirubah}, {@code oleh}, dan {@code olehId} dari pengguna yang sedang login.
	 *
	 * <p><b>Efek samping:</b> mengubah tiga field audit object ini. Callback ini <b>tidak</b>
	 * berjalan pada {@code INSERT}; untuk baris baru nilai awal {@code tanggal_dirubah} berasal
	 * dari inisialisasi field yang dideklarasikan pada baris yang sama dengan method ini
	 * ({@code ais.ui.util.WaktuUtil.getDate()}, yaitu waktu object dibuat di JVM, bukan waktu
	 * database).</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi; menerima {@code null}.
	 *
	 * <p>Umumnya tidak perlu dipanggil manual — {@link #onUpdate()} sudah mengisinya otomatis
	 * sebelum setiap {@code UPDATE}.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir, dipetakan sebagai {@code TIMESTAMP}.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk object baru karena field
	 *         diinisialisasi saat konstruksi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nama jenis berkas, mis. "KTP" atau "Ijazah". Kolom {@code NOT NULL}, maksimal 255 karakter. */
	private String nama;

	/** Keterangan bebas, opsional, tanpa batas panjang eksplisit. */
	private String keterangan;

	/** Fakultas tempat jenis berkas ini berlaku; {@code null} berarti berlaku untuk semua fakultas. */
	private Fakultas fakultas;

	/** Jurusan/prodi tempat jenis berkas ini berlaku; {@code null} berarti berlaku untuk semua prodi. */
	private Jurusan jurusan;

	/** Program (mis. reguler/karyawan) tempat jenis berkas ini berlaku; {@code null}/kosong berarti semua. */
	private String program;

	/** Tahun akademik berlakunya jenis berkas; {@code null}/kosong berarti semua tahun akademik. */
	private String tahunAkademik;

	/** Jenis semester berlakunya ({@code Perkuliahan.GANJIL}/{@code GENAP}); {@code null} berarti semua. */
	private String jenisSemester;

	/** Penghitung berapa kali jenis berkas ini dipilih lewat picker; diinisialisasi 0. */
	private Long jmlDipakai = 0L;

	/** Induk pada hierarki berkas; {@code null} untuk node akar. */
	private Berkas parent;

	/**
	 * Constructor kosong wajib bagi Hibernate/JPA. Juga dipakai {@code BerkasAction} untuk
	 * membuat baris baru dari tombol "Tambah"/"Tambah Berkas".
	 */
	public Berkas() {
	}

	/**
	 * Constructor kemudahan yang langsung mengisi {@link #setNama(String)}.
	 *
	 * <p>Sisa peninggalan generator hbm2java: tidak ada satu pun pemanggil di codebase saat ini.</p>
	 *
	 * @param nama nama jenis berkas
	 */
	public Berkas(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan kunci primer.
	 *
	 * <p>Kolom dipetakan dengan {@code insertable = false} karena nilainya di-generate database
	 * (strategi {@code IDENTITY}), bukan dikirim aplikasi saat {@code INSERT}.</p>
	 *
	 * @return id baris; {@code null} selama entity belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci primer. Tanpa validasi.
	 *
	 * <p>Dipanggil Hibernate saat memuat/menyimpan entity. Kode aplikasi memanggilnya secara
	 * sengaja dengan {@code null} pada tombol "Tambah Data"/"Copy Data" di {@code BerkasAction}
	 * untuk mengubah hasil {@code clone()} menjadi baris baru yang belum tersimpan.</p>
	 *
	 * @param id id baru; boleh {@code null}
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama jenis berkas, sudah dipangkas spasi ujungnya.
	 *
	 * <p><b>Bukan getter destruktif:</b> hasil {@code trim()} dikembalikan sebagai nilai baru dan
	 * <b>tidak</b> ditulis balik ke field, sehingga nilai di database tetap apa adanya. Artinya
	 * {@code getNama()} bisa berbeda dari isi field/kolom bila datanya mengandung spasi ujung —
	 * dan {@link #toString()} (yang memakai field langsung) akan menampilkan versi belum
	 * dipangkas.</p>
	 *
	 * <p>Meng-override {@link GeneralValueObject#getNama()} sekaligus menambahkan pemetaan kolom
	 * {@code nama} yang tidak diwarisi dari induk.</p>
	 *
	 * @return nama jenis berkas, atau {@code null} bila field kosong
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama jenis berkas. Tanpa validasi maupun {@code trim} — pemangkasan hanya terjadi
	 * saat pembacaan lewat {@link #getNama()}.
	 *
	 * <p>Kolom {@code NOT NULL}: menyimpan entity dengan nama {@code null} akan gagal di level
	 * database. Validasi "wajib diisi" dilakukan di layar ({@code BerkasAction.onSave}), bukan di
	 * sini.</p>
	 *
	 * @param nama nama jenis berkas
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas. Tidak dipangkas.
	 *
	 * @return keterangan; boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas. Tanpa validasi.
	 *
	 * @param keterangan keterangan baru; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan penghitung pemakaian jenis berkas ini.
	 *
	 * <p>Tanpa {@code @Column} sehingga dipetakan ke kolom bernama sama oleh
	 * {@code MyNamingStrategy}. Nilai dinaikkan satu oleh {@code AmbilDataBerkasBanbox} setiap
	 * kali pengguna memilih sebuah berkas — baik dari pohon, dari grid "Berkas Sering Dapakai",
	 * maupun lewat pengetikan nama persis + Enter — lalu langsung disimpan via
	 * {@code Common.refreshUpdate}. Grid "Berkas Sering Dapakai" mengurutkan menurun berdasarkan
	 * nilai ini dan menyaring baris yang nilainya {@code null}.</p>
	 *
	 * <p><b>Kejanggalan:</b> karena picker tersebut satu-satunya pemakainya dan picker itu sendiri
	 * hanya dipakai oleh field "Induk" pada layar master {@code Berkas} (lihat Javadoc kelas),
	 * angka ini sebenarnya hanya menghitung "berapa kali berkas ini dipilih sebagai induk saat
	 * mengedit master", bukan seberapa sering jenis berkas ini benar-benar dipakai di alur
	 * dokumen mana pun.</p>
	 *
	 * @return jumlah pemakaian; diinisialisasi {@code 0} untuk object baru, namun boleh
	 *         {@code null} untuk baris lama yang kolomnya belum pernah terisi
	 */
	public Long getJmlDipakai() {
		return jmlDipakai;
	}

	/**
	 * Menyetel penghitung pemakaian. Tanpa validasi (nilai negatif atau {@code null} diterima).
	 *
	 * @param jmlDipakai nilai penghitung baru
	 */
	public void setJmlDipakai(Long jmlDipakai) {
		this.jmlDipakai = jmlDipakai;
	}

	/**
	 * Mengembalikan induk pada hierarki berkas.
	 *
	 * <p>Relasi self-reference {@code @ManyToOne} ke kolom {@code parent} yang boleh
	 * {@code null} (node akar). Dimuat dengan {@code FetchMode.SELECT}, yaitu query terpisah saat
	 * properti diakses. Cascade {@code PERSIST}/{@code MERGE} berarti menyimpan sebuah berkas ikut
	 * menyimpan induk yang belum tersimpan; {@code REMOVE} sengaja tidak diikutkan sehingga
	 * menghapus anak tidak menghapus induknya.</p>
	 *
	 * <p>Tidak ada sisi kebalikan (koleksi {@code children}) di entity ini — daftar anak diambil
	 * lewat query di {@code BerkasTreeModel.getChildren}. Tidak ada pula pencegahan siklus
	 * (A menjadi induk B sekaligus B menjadi induk A) di level entity maupun di
	 * {@code BerkasAction.onSave}; siklus semacam itu akan membuat penelusuran pohon rekursif
	 * tidak berujung.</p>
	 *
	 * @return entity induk, atau {@code null} bila baris ini node akar
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "parent", nullable = true)
	public Berkas getParent() {
		return parent;
	}

	/**
	 * Menyetel induk pada hierarki berkas. Tanpa validasi — tidak memeriksa apakah induk yang
	 * diberikan sama dengan object ini sendiri atau membentuk siklus.
	 *
	 * @param parent entity induk; {@code null} menjadikan baris ini node akar
	 */
	public void setParent(Berkas parent) {
		this.parent = parent;
	}

	/**
	 * Mengembalikan fakultas tempat jenis berkas ini berlaku.
	 *
	 * <p>{@code @ManyToOne} opsional dengan {@code FetchMode.SELECT} dan cascade
	 * {@code PERSIST}/{@code MERGE}. Nilai {@code null} berarti "berlaku untuk semua fakultas"
	 * dan ditampilkan UI sebagai teks "Semua".</p>
	 *
	 * @return fakultas, atau {@code null} bila berlaku umum
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		return fakultas;
	}

	/**
	 * Menyetel fakultas berlakunya jenis berkas. Tanpa validasi konsistensi terhadap
	 * {@link #getJurusan()} — kombinasi fakultas dan prodi yang tidak sejalan tidak dicegah.
	 *
	 * @param fakultas fakultas; {@code null} berarti berlaku untuk semua fakultas
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Mengembalikan jurusan/prodi tempat jenis berkas ini berlaku.
	 *
	 * <p>{@code @ManyToOne} opsional dengan {@code FetchMode.SELECT} dan cascade
	 * {@code PERSIST}/{@code MERGE}. Nilai {@code null} berarti "berlaku untuk semua prodi".</p>
	 *
	 * @return jurusan, atau {@code null} bila berlaku umum
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		return jurusan;
	}

	/**
	 * Menyetel jurusan/prodi berlakunya jenis berkas. Tanpa validasi.
	 *
	 * @param jurusan jurusan; {@code null} berarti berlaku untuk semua prodi
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan program (mis. reguler/karyawan) tempat jenis berkas ini berlaku.
	 *
	 * <p>Tanpa {@code @Column}; dipetakan ke kolom bernama sama. Nilai {@code null}/kosong
	 * ditampilkan UI sebagai "Semua".</p>
	 *
	 * <p><b>Kejanggalan:</b> nilai ini hanya pernah <i>dibaca</i>. Form tambah/ubah di
	 * {@code BerkasAction.init} tidak menyediakan input Program dan {@code onSave} tidak pernah
	 * memanggil {@link #setProgram(String)}, sehingga dalam praktik kolom ini selalu {@code null}
	 * dan kedua kolom "Program" di layar (pohon dan grid) selalu menampilkan "Semua".</p>
	 *
	 * @return program; boleh {@code null}
	 */
	public String getProgram() {
		return program;
	}

	/**
	 * Menyetel program berlakunya jenis berkas. Tanpa validasi.
	 *
	 * <p>Tidak ada pemanggil di codebase saat ini (lihat {@link #getProgram()}).</p>
	 *
	 * @param program program; {@code null}/kosong berarti berlaku untuk semua program
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * Mengembalikan tahun akademik berlakunya jenis berkas.
	 *
	 * <p>Tanpa {@code @Column}; dipetakan ke kolom bernama sama. Diisi dari combobox tahun ajaran
	 * di {@code BerkasAction} yang menyertakan pilihan "Semua" bernilai {@code null}. Disimpan
	 * sebagai teks, bukan relasi.</p>
	 *
	 * @return tahun akademik, atau {@code null}/kosong bila berlaku untuk semua tahun akademik
	 */
	public String getTahunAkademik() {
		return tahunAkademik;
	}

	/**
	 * Menyetel tahun akademik berlakunya jenis berkas. Tanpa validasi format.
	 *
	 * @param tahunAkademik tahun akademik; {@code null}/kosong berarti semua
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan jenis semester berlakunya jenis berkas.
	 *
	 * <p>Tanpa {@code @Column}; dipetakan ke kolom bernama sama. Nilainya berupa konstanta teks
	 * {@code Perkuliahan.GANJIL} atau {@code Perkuliahan.GENAP}; {@code null} berarti "Semua".</p>
	 *
	 * @return jenis semester, atau {@code null}/kosong bila berlaku untuk semua semester
	 */
	public String getJenisSemester() {
		return jenisSemester;
	}

	/**
	 * Menyetel jenis semester berlakunya jenis berkas. Tanpa validasi terhadap daftar konstanta
	 * yang sah.
	 *
	 * @param jenisSemester jenis semester; {@code null}/kosong berarti semua
	 */
	public void setJenisSemester(String jenisSemester) {
		this.jenisSemester = jenisSemester;
	}

}
