package ais.database.model.penelitiandanpengabdian;

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




import org.hibernate.envers.Audited;



import ais.database.model.GeneralValueObject;

/**
 * Entitas Hibernate/JPA (tabel {@code penelitiandanpengabdian.jurnal_penelitian})
 * yang menjadi MASTER DATA jurnal ilmiah tempat dosen mempublikasikan artikel
 * hasil penelitian/pengabdian. Satu baris = satu jurnal (mis. "Jurnal Teknik
 * Informatika Vol. X"), diidentifikasi ke pengguna lewat {@link #getPath()}
 * (path/slug unik, dipakai sebagai bagian URL publik jurnal) dan direlasikan
 * balik dari {@link ais.database.model.penelitiandanpengabdian.Artikel#getJurnalPenelitian()}
 * lewat FK kolom {@code jurnal_penelitian} (satu jurnal bisa menaungi banyak
 * artikel; relasi @ManyToOne ada di sisi {@code Artikel}, bukan di sini).
 * <p>
 * Kelas ini sekaligus menjadi entitas AKAR (root) dari subsistem manajemen
 * jurnal bergaya OJS (Open Journal Systems) yang tersebar di paket
 * {@code ais.action.master.jurnal.*} (workflow, undangan peran, penugasan
 * tahap review, langganan &amp; rentang IP akses, notifikasi/email, laporan,
 * importer OJS legacy, dsb) — lihat {@link #getRepoCollectionId()} dan
 * {@link #getTenantKey()} untuk detail bagaimana relasi itu bekerja. Entitas
 * ini bukan wrapper lampiran (tidak seperti kelas-kelas di paket
 * {@code ais.database.model.file}); ia murni master data + titik jangkar
 * otorisasi/scoping untuk seluruh submodul jurnal.
 * <p>
 * Diaudit lewat Hibernate Envers ({@code @Audited}); setiap perubahan tercatat
 * ke tabel revisi bawaan Envers untuk keperluan jejak audit.
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "penelitiandanpengabdian", name = "jurnal_penelitian")



public class JurnalPenelitian extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}
	 * antar build (lewat superclass {@link GeneralValueObject}). Nilainya sama
	 * persis dengan konstanta di banyak entitas lain hasil hbm2java pada
	 * generasi kode yang sama — bukan indikasi relasi/warisan apa pun antar
	 * entitas, hanya kebetulan template kode generator.
	 */
	private static final long serialVersionUID = 2463822571548439808L;
	private Long id;
	/** Lihat javadoc {@link #getOleh()}. */
	private String oleh;
	/** Lihat javadoc {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan ID pengguna (userId) yang membuat/terakhir mengelola data
	 * jurnal ini, dipakai sebagai jejak "oleh siapa" terpisah dari nama
	 * tampilan {@link #getOleh()}. Bisa {@code null} jika belum pernah
	 * diset (mis. data lama sebelum field ini ditambahkan).
	 *
	 * @return userId pemilik/pengelola pencatat, atau {@code null}
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel {@link #olehId}. Menolak (no-op, silent) nilai {@code null}
	 * atau string kosong/whitespace — begitu {@link #olehId} pernah terisi
	 * dengan nilai valid, pemanggilan berikutnya dengan nilai kosong TIDAK
	 * akan menghapusnya (bukan bug baru, pola yang sama berulang di banyak
	 * entitas lain sepasang {@code oleh}/{@code olehId} pada aplikasi ini).
	 *
	 * @param olehId userId pengelola; diabaikan jika {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel {@link #oleh} (nama tampilan pengelola). Sama seperti
	 * {@link #setOlehId(String)}, menolak (no-op) nilai {@code null} atau
	 * string kosong/whitespace sehingga nilai lama tidak pernah tertimpa
	 * kosong secara tidak sengaja.
	 *
	 * @param oleh nama tampilan pengelola; diabaikan jika {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama tampilan (bukan userId) pengguna yang
	 * membuat/mengelola data jurnal ini.
	 *
	 * @return nama tampilan pengelola, atau {@code null} jika belum diset
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat
	 * sebelum UPDATE dieksekusi, mendelegasikan pencatatan timestamp
	 * perubahan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}
	 * yang akan mengisi {@link #tanggal_dirubah} dengan waktu saat ini. Field
	 * {@link #tanggal_dirubah} sendiri diinisialisasi eager lewat
	 * {@link ais.ui.util.WaktuUtil#getDate()} pada saat objek dibuat (bukan
	 * lazy) — field audit shadow standar di banyak entitas AIS, KEHARUSAN
	 * TEKNIS untuk penjejakan waktu ubah, bukan bug.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel timestamp terakhir diubah secara manual. Umumnya tidak perlu
	 * dipanggil langsung oleh kode aplikasi karena {@link #onUpdate()} sudah
	 * mengisinya otomatis pada setiap UPDATE lewat Hibernate.
	 *
	 * @param tanggal_dirubah waktu perubahan baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan timestamp terakhir kali record ini diubah (kolom
	 * {@code @Temporal(TIMESTAMP)}), diisi otomatis oleh {@link #onUpdate()}.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi string entitas ini untuk keperluan tampilan ringkas
	 * (mis. dropdown/combobox pemilih jurnal di UI ZK) — mengembalikan
	 * {@link #judul} apa adanya, TANPA null-check (akan mengembalikan
	 * {@code null} literal jika judul belum diisi, bukan string kosong).
	 *
	 * @return judul jurnal, atau {@code null} jika belum diisi
	 */
	public String toString() {
		return judul;
	}

	/** Lihat javadoc {@link #getJudul()}. */
	private String judul;
	/** Lihat javadoc {@link #getPath()}. */
	private String path;
	/** Lihat javadoc {@link #getAktif()}. */
	private Boolean aktif;
	/** Lihat javadoc {@link #getJournalId()}. */
	private Long journalId;
	/** Lihat javadoc {@link #getKorespondensi()}. */
	private String korespondensi;
	/** Lihat javadoc {@link #getKorespondensiGrupPengguna()}. */
	private String korespondensiGrupPengguna;
	/** Lihat javadoc {@link #getRepoCollectionId()}. */
	private Long repoCollectionId;
	/** Lihat javadoc {@link #getTenantKey()}. */
	private String tenantKey;
	/** Lihat javadoc {@link #getDefaultLocale()}. */
	private String defaultLocale;

	/** Konstruktor default (dibutuhkan Hibernate); tidak menyetel field apa pun. */
	public JurnalPenelitian() {
	}

	/**
	 * Primary key auto-increment (identity) tabel {@code jurnal_penelitian}.
	 * Anotasi {@code insertable = false} berarti kolom ini tidak pernah
	 * disertakan Hibernate pada statement INSERT eksplisit — nilainya
	 * sepenuhnya diserahkan ke mekanisme {@code IDENTITY} pada database.
	 *
	 * @return id primary key, {@code null} untuk instance yang belum
	 *         disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel id. Dipakai Hibernate sendiri saat memuat entitas dari DB;
	 * jarang perlu dipanggil manual oleh kode aplikasi karena kolomnya
	 * {@code insertable = false} (lihat {@link #getId()}).
	 *
	 * @param id nilai primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Judul/nama jurnal (mis. "Jurnal Teknik Informatika"), kolom wajib
	 * diisi ({@code nullable = false}) dengan panjang maksimum 1000 karakter.
	 * Dipakai juga sebagai representasi {@link #toString()} entitas ini.
	 *
	 * @return judul jurnal
	 */
	@Column(name = "judul", nullable = false, length = 1000)
	public String getJudul() {
		return this.judul;
	}

	/**
	 * Menyetel judul jurnal.
	 *
	 * @param judul judul baru
	 */
	public void setJudul(String judul) {
		this.judul = judul;
	}

	/**
	 * Status aktif/nonaktif jurnal ini. Lazy-default: jika belum pernah
	 * diset ({@code null}, mis. data lama), method ini MENGISI field
	 * {@link #aktif} dengan {@code true} pada pemanggilan pertama (efek
	 * samping pada getter) sekaligus mengembalikannya — pola "default aktif"
	 * standar yang konsisten arahnya (bukan pola {@code getAktif()} terbalik
	 * yang ditemukan pada {@code InformasiPerpustakaan}/{@code ItemPunyaTerbit}
	 * di sesi javadoc sebelumnya; di sini {@code true} tetap berarti aktif
	 * apa adanya, tidak dibalik).
	 *
	 * @return {@code true} jika jurnal aktif (default bila belum diset),
	 *         {@code false} jika dinonaktifkan eksplisit
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menyetel status aktif/nonaktif jurnal secara eksplisit.
	 *
	 * @param aktif {@code true} untuk mengaktifkan, {@code false} untuk
	 *              menonaktifkan, {@code null} untuk kembali ke default
	 *              (akan dianggap aktif lagi oleh {@link #getAktif()})
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Path/slug unik jurnal ini (kolom {@code unique = true, nullable = false}),
	 * dipakai sebagai identitas berbasis-teks pada URL publik/akses jurnal
	 * (mis. dipetakan ke bagian path OJS-style seperti {@code /index.php/<path>}).
	 * Karena unik, dipakai juga sebagai kunci pencarian jurnal dari luar
	 * (selain {@link #getId()}).
	 *
	 * @return path unik jurnal
	 */
	@Column(name = "path", unique = true, nullable = false)
	public String getPath() {
		return path;
	}

	/**
	 * Menyetel path/slug unik jurnal. Pemanggil bertanggung jawab menjaga
	 * keunikannya sebelum simpan (constraint unique di DB akan menolak
	 * duplikat, tapi sebaiknya divalidasi lebih awal di lapisan UI/Action).
	 *
	 * @param path path/slug baru
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Field {@code journalId} — TIDAK ADA anotasi {@code @Column} eksplisit
	 * (dipetakan Hibernate lewat konvensi nama properti default, bukan
	 * {@code @Transient}), namun berdasarkan penelusuran seluruh basis kode:
	 * getter/setter ini TIDAK PERNAH dipanggil pada instance
	 * {@code JurnalPenelitian} di mana pun (satu-satunya pemanggilan
	 * {@code .getJournalId()} yang ditemukan bertumpu pada variabel bertipe
	 * {@code Journals} — entitas OJS legacy yang berbeda — di
	 * {@code TbmuserAction}/{@code MahasiswaAction}, bukan kelas ini).
	 * Field ini tampak seperti ID eksternal/legacy (mis. sisa migrasi dari
	 * OJS) yang kini YATIM/TIDUR — ada kolomnya, tapi tidak dipakai aplikasi.
	 * Jangan bingung dengan {@link #getId()} (primary key internal AIS yang
	 * justru dipakai luas sebagai parameter "journalId" di layanan-layanan
	 * {@code ais.action.master.jurnal.*}).
	 *
	 * @return nilai {@code journalId} tersimpan, atau {@code null}
	 */
	public Long getJournalId() {
		return journalId;
	}

	/**
	 * Menyetel {@link #journalId}. Lihat catatan yatim/tidur pada
	 * {@link #getJournalId()} — tidak ditemukan pemanggil di basis kode saat
	 * javadoc ini ditulis.
	 *
	 * @param journalId nilai baru
	 */
	public void setJournalId(Long journalId) {
		this.journalId = journalId;
	}

	/**
	 * Daftar userId korespondensi jurnal ini, disimpan sebagai SATU string
	 * dengan userId dipisah koma (parsing dilakukan pemanggil, mis.
	 * {@code JurnalPenelitianAction} men-split dengan {@code ","}) — bukan
	 * relasi many-to-many terstruktur. Kolom {@code text}, boleh null di DB
	 * tapi getter ini menormalkan {@code null} menjadi string kosong
	 * (lazy-default, dengan efek samping menulis balik ke field {@link
	 * #korespondensi} seperti pola {@link #getAktif()}).
	 *
	 * @return string userId korespondensi dipisah koma, tidak pernah
	 *         {@code null} (string kosong bila belum diisi)
	 */
	@Column(name = "korespondensi", nullable = true, columnDefinition = "text")
	public String getKorespondensi() {
		if (korespondensi == null) {
			korespondensi = "";
		}
		return korespondensi;
	}

	/**
	 * Menyetel daftar userId korespondensi (format string dipisah koma,
	 * lihat {@link #getKorespondensi()}).
	 *
	 * @param korespondensi string userId dipisah koma
	 */
	public void setKorespondensi(String korespondensi) {
		this.korespondensi = korespondensi;
	}

	/**
	 * Deskripsi bebas (bukan daftar userId terstruktur, berbeda dari
	 * {@link #getKorespondensi()}) mengenai grup pengguna yang berperan
	 * sebagai korespondensi jurnal ini — murni informasional/tampilan pada
	 * UI pengelolaan jurnal, tidak diparsing lebih lanjut oleh kode yang
	 * ditemukan. TIDAK dianotasi {@code @Column} eksplisit, dipetakan lewat
	 * konvensi nama properti default Hibernate. Getter menormalkan
	 * {@code null} menjadi string kosong dan men-trim whitespace, TANPA
	 * anotasi {@code @Temporal} atau efek samping tulis-balik (berbeda dari
	 * {@link #getKorespondensi()}/{@link #getAktif()}).
	 *
	 * @return teks grup pengguna korespondensi, sudah di-trim, tidak pernah
	 *         {@code null}
	 */
	public String getKorespondensiGrupPengguna() {
		return korespondensiGrupPengguna == null ? "" : korespondensiGrupPengguna.trim();
	}

	/**
	 * Menyetel teks bebas grup pengguna korespondensi.
	 *
	 * @param korespondensiGrupPengguna teks baru (disimpan apa adanya, trim
	 *                                  terjadi hanya saat dibaca lewat
	 *                                  {@link #getKorespondensiGrupPengguna()})
	 */
	public void setKorespondensiGrupPengguna(String korespondensiGrupPengguna) {
		this.korespondensiGrupPengguna = korespondensiGrupPengguna;
	}

	/**
	 * FK (disimpan sebagai id polos, tanpa {@code @ManyToOne}/{@code @JoinColumn})
	 * ke koleksi repositori dokumen (mis. entitas {@code RepoCollection} pada
	 * subsistem repositori mirip DSpace) yang menaungi berkas-berkas
	 * submission/artikel jurnal ini. Field ini adalah JANGKAR UTAMA yang
	 * dipakai berulang kali oleh layanan-layanan {@code ais.action.master.jurnal.*}
	 * (mis. {@code JurnalDiscussionService}, {@code JurnalStageAssignmentService},
	 * {@code JurnalAccessService}, {@code JurnalWorkspaceService}) untuk
	 * MEMVERIFIKASI bahwa suatu item repositori benar-benar milik jurnal ini
	 * sebelum operasi dilanjutkan (bandingkan {@code journal.getRepoCollectionId()}
	 * dengan {@code item.getCollectionId()}) — bagian dari gerbang otorisasi
	 * cakupan (scope) jurnal, bukan sekadar metadata pasif.
	 *
	 * @return id koleksi repositori terkait, atau {@code null} jika jurnal
	 *         belum ditautkan ke koleksi repositori mana pun
	 */
	@Column(name="repo_collection_id") public Long getRepoCollectionId(){return repoCollectionId;}

	/**
	 * Menyetel {@link #repoCollectionId}. Perubahan nilai ini berdampak
	 * langsung pada hasil pemeriksaan cakupan (scope) di semua layanan yang
	 * membandingkannya dengan {@code collectionId} item repositori — lihat
	 * javadoc {@link #getRepoCollectionId()}.
	 *
	 * @param v id koleksi repositori baru
	 */
	public void setRepoCollectionId(Long v){repoCollectionId=v;}

	/**
	 * Kunci penanda tenant (multi-tenant) untuk jurnal ini, dipakai
	 * {@code JurnalAuthorizationService} untuk menolak akses lintas-tenant:
	 * jika jurnal dan item/objek yang diakses sama-sama punya
	 * {@code tenantKey} terisi tapi NILAINYA BERBEDA, akses ditolak. Kolom
	 * string maks. 120 karakter, boleh kosong (mis. instalasi single-tenant
	 * lama sebelum fitur multi-tenant ditambahkan) — banyak pemanggil
	 * memperlakukan nilai kosong/blank sebagai {@code "default"} (lihat
	 * {@code OjsImportExecutionService}, {@code JurnalAccessService}).
	 *
	 * @return kunci tenant jurnal ini, bisa {@code null}/kosong untuk data
	 *         lama atau instalasi single-tenant
	 */
	@Column(name="tenant_key",length=120) public String getTenantKey(){return tenantKey;}

	/**
	 * Menyetel {@link #tenantKey}. Nilai ini disalin/diturunkan ke banyak
	 * entitas anak yang dibuat dalam konteks jurnal ini (undangan peran,
	 * penugasan tahap, checkpoint importer OJS, dsb) — lihat javadoc
	 * {@link #getTenantKey()} untuk peran otorisasinya.
	 *
	 * @param v kunci tenant baru
	 */
	public void setTenantKey(String v){tenantKey=v;}

	/**
	 * Locale bahasa default jurnal ini untuk keperluan tampilan/template
	 * email OJS-style (mis. {@code "id_ID"}, {@code "en_US"}). Lazy-default:
	 * jika belum pernah diset, getter mengembalikan literal {@code "id_ID"}
	 * TANPA menulis balik ke field (berbeda dari {@link #getAktif()}/
	 * {@link #getKorespondensi()} yang menyimpan hasil default ke field-nya).
	 *
	 * @return kode locale (format {@code bahasa_NEGARA}), default
	 *         {@code "id_ID"} bila belum diset
	 */
	@Column(name="default_locale",length=20) public String getDefaultLocale(){return defaultLocale==null?"id_ID":defaultLocale;}

	/**
	 * Menyetel locale bahasa default jurnal.
	 *
	 * @param v kode locale baru (mis. {@code "id_ID"}, {@code "en_US"})
	 */
	public void setDefaultLocale(String v){defaultLocale=v;}

}
