package ais.database.model;
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
import org.json.JSONObject;

/**
 * Entity Hibernate untuk tabel {@code public.setting_biaya_detail} — satu baris
 * <b>pengikat</b> (binding) antara sebuah skema biaya {@link SettingBiaya} dengan
 * <b>satu orang tertentu</b>: {@link Mahasiswa} (mahasiswa aktif) atau
 * {@link BiodataCalonMahasiswa} (calon mahasiswa/pendaftar).
 *
 * <p>Baris seperti ini hanya dipakai bila SettingBiaya induknya ditandai
 * {@code khususBuatMahasiswaTertentu} atau {@code batasiMahasiswaTertentu}. Pada mode
 * tersebut daftar penerima tagihan TIDAK lagi dihitung dari kriteria umum SettingBiaya
 * (angkatan, jenjang, program, jurusan, gelombang pendaftaran, dsb), melainkan diambil
 * dari kumpulan baris {@code SettingBiayaDetail} milik SettingBiaya itu — lihat
 * {@code DetailSettingBiayaAction.modeDaftarMahasiswa()} yang memilih renderer
 * "khusus per mahasiswa/calon" berdasar kedua flag tersebut.</p>
 *
 * <h2>Klarifikasi nama: EMPAT entity yang namanya nyaris sama</h2>
 * <p>Ini sumber kebingungan terbesar di modul billing. Keempatnya adalah tabel yang
 * <b>berbeda</b>, bukan alias satu sama lain:</p>
 * <ul>
 *   <li>{@link SettingBiaya} (tabel {@code setting_biaya}) — <i>kepala</i> skema biaya:
 *       "biaya apa, untuk jenis kegiatan mana, angkatan/jenjang/program siapa".</li>
 *   <li>{@link DetailSettingBiaya} (tabel {@code detail_setting_biaya}) — <i>rincian
 *       skema</i>: nominal default per {@link ItemBiaya} dan per termin
 *       ({@code bayarKe}), lengkap dengan tanggal tagihan/deadline default dan override
 *       per {@link Jurusan} dalam kolom JSON {@code biaya_per_prodi}.</li>
 *   <li><b>{@code SettingBiayaDetail} (kelas ini, tabel {@code setting_biaya_detail})</b>
 *       — <i>binding per orang</i>: siapa saja yang kena skema ini, plus override
 *       rentang semester dan "kuota custom" nominal per ItemBiaya untuk orang itu.</li>
 *   <li>{@link DetailBiaya} (tabel {@code detail_biaya}) — <i>tagihan nyata</i> milik
 *       seorang mahasiswa/calon: baris inilah yang muncul di layar pembayaran dan
 *       dilunasi.</li>
 * </ul>
 *
 * <p><b>Jadi: {@code SettingBiayaDetail} BUKAN {@code DetailBiaya}.</b> Rantai
 * {@code SettingBiaya → DetailBiaya → PengaturanPembayaranBulanan →
 * DetailKegiatan/CicilanPembayaran} yang tercatat pada dokumentasi
 * {@link PengaturanPembayaranBulanan} tetap benar; kelas ini adalah simpul <b>samping</b>
 * pada rantai itu, yang dirujuk oleh {@code DetailBiaya.getSettingBiayaDetail()}.
 * Gambaran utuhnya:</p>
 *
 * <pre>
 *   SettingBiaya  ──&lt;  DetailSettingBiaya   (rincian skema: item biaya, termin, nominal default)
 *        │
 *        └────────&lt;  SettingBiayaDetail   ← KELAS INI (binding per orang + kuota custom)
 *                          │
 *   DetailBiaya  ─────────┘   (tagihan nyata; merujuk ketiganya sekaligus:
 *        │                   settingBiaya, detailSettingBiaya, settingBiayaDetail)
 *        └──&lt; PengaturanPembayaranBulanan ──&lt; DetailKegiatan / CicilanPembayaran
 * </pre>
 *
 * <h2>Isi satu baris</h2>
 * <ol>
 *   <li><b>Identitas penerima</b> — {@link #getMahasiswa()} <i>atau</i>
 *       {@link #getBiodataCalonMahasiswa()}. Keduanya {@code nullable}; secara praktik
 *       terisi salah satu saja (XOR de facto), tetapi tidak ada constraint database
 *       maupun validasi di kelas ini yang memaksakannya.</li>
 *   <li><b>Rentang semester berlaku</b> — {@link #getMinSmt()}/{@link #getMaxSmt()}
 *       (kolom {@code min_smt_detail}/{@code max_smt_detail}). Dipakai mesin tagihan
 *       daftar ulang untuk menolak/menyaring skema yang tidak berlaku pada semester
 *       berjalan mahasiswa tsb.</li>
 *   <li><b>Kuota custom per item biaya</b> — {@link #getBiayas()}, kolom {@code text}
 *       berisi JSON.</li>
 * </ol>
 *
 * <h2>Format kolom {@code biayas}</h2>
 * <p>Objek JSON datar dengan <b>key = {@code itemBiaya.id} sebagai string</b> dan
 * <b>value = nominal (double)</b>, mis. {@code {"12":1000000,"15":250000}}. Ditulis dari
 * kotak "Kuota Custom (Template)" pada {@code DetailSettingBiayaAction} dan dibaca
 * kembali oleh {@link DetailBiaya#getNilaiBiaya()} sebagai <b>prioritas kedua</b> saat
 * menentukan nominal tagihan:</p>
 * <ol>
 *   <li>override per jurusan dari {@link DetailSettingBiaya} (bila
 *       {@code settingBiaya.tampilkanPerProdi});</li>
 *   <li><b>nilai dari JSON {@code biayas} milik baris ini</b> (bila
 *       {@code settingBiayaDetail} terpasang dan sudah punya id) — bila key
 *       {@code itemBiaya.id} tidak ada di JSON, jatuh ke
 *       {@code detailSettingBiaya.getDefaultBiaya()};</li>
 *   <li>nominal default DetailSettingBiaya (bila {@code settingBiaya.gunakanBiayaDefault}).</li>
 * </ol>
 * <p>Perlu ditegaskan: nilai di sini adalah nominal <b>default saat tagihan pertama kali
 * diterbitkan</b>. Menurunkan tagihan yang sudah berjalan dilakukan lewat kolom "Tagihan
 * Aktif" pada layar yang sama, yaitu mengubah {@link DetailKegiatan}, bukan kolom ini.</p>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ul>
 *   <li><b>Jejak audit yang dideklarasikan ulang</b> — {@code id}, {@code oleh},
 *       {@code olehId}, {@code tanggal_dirubah} beserta accessor-nya dan
 *       {@link #onUpdate()}. Lihat catatan tentang {@link GeneralValueObject} di bawah:
 *       pengulangan ini KEHARUSAN teknis, bukan duplikasi yang perlu dibersihkan.</li>
 *   <li><b>Relasi {@code @ManyToOne} lazy</b> — {@link #getSettingBiaya()},
 *       {@link #getMahasiswa()}, {@link #getBiodataCalonMahasiswa()}. Ketiganya
 *       memanggil {@code check(...)} milik kelas induk untuk meresolusi proxy lazy.</li>
 *   <li><b>Payload konfigurasi</b> — {@link #getBiayas()}/{@link #setBiayas(String)}.</li>
 *   <li><b>Rentang semester dengan pewarisan</b> — {@link #getMinSmt()}/
 *       {@link #getMaxSmt()} beserta setter-nya.</li>
 * </ul>
 * <p>Tidak ada {@code equals}/{@code hashCode}/{@code toString} yang di-override, tidak
 * ada method query statis, dan tidak ada logika validasi; seluruh query terhadap tabel
 * ini hidup di {@code DetailSettingBiayaAction}, {@code DaftarUlangMahasiswaBaruAction},
 * {@code DaftarUlangMahasiswaLamaAction}, dan {@code DaftarUlangPembayaranHelper}.</p>
 *
 * <h2>Kehalusan dan kuirk yang perlu diketahui</h2>
 * <ul>
 *   <li><b>Pewarisan rentang semester ikut TERTULIS ke kolom.</b>
 *       {@link #getMinSmt()}/{@link #getMaxSmt()} mengembalikan nilai milik
 *       {@link SettingBiaya} induk ketika kolom lokalnya {@code null}. Karena anotasi
 *       pemetaan berada pada getter (Hibernate memakai <i>property access</i>), nilai
 *       hasil pewarisan itulah yang dibaca Hibernate saat insert/dirty-check — sehingga
 *       nilai induk berpotensi <b>tersalin permanen</b> ke kolom
 *       {@code min_smt_detail}/{@code max_smt_detail} pada flush berikutnya. Setelah itu
 *       baris ini berhenti mewarisi: perubahan rentang semester di SettingBiaya induk
 *       tidak lagi ikut berubah untuk orang ini.</li>
 *   <li><b>{@link #getBiayas()} tidak pernah mengembalikan {@code null}</b> — nilai
 *       kosong dipetakan menjadi string {@code "{}"} supaya pemanggil bisa langsung
 *       {@code new JSONObject(...)} tanpa cek null. Konsekuensinya sama seperti di atas:
 *       yang tersimpan ke kolom {@code biayas} adalah {@code "{}"}, bukan {@code NULL}.</li>
 *   <li><b>Field statis {@link #jsonObject}</b> hanyalah sumber string {@code "{}"} itu;
 *       ia tidak pernah dimutasi di kelas ini dan bukan cache bersama.</li>
 *   <li><b>Getter relasi bisa menyentuh database.</b> {@code check(...)} dapat membaca
 *       cache, menginisialisasi proxy, atau membuka session Hibernate baru sebagai upaya
 *       terakhir; jangan asumsikan getter di kelas ini murni membaca memori.</li>
 *   <li><b>{@link #setOleh(String)}/{@link #setOlehId(String)} menolak nilai kosong secara
 *       diam-diam</b> — nilai lama dipertahankan, tanpa exception dan tanpa log.</li>
 *   <li><b>Kriteria pencarian memakai kolom mentah.</b> Query di
 *       {@code DaftarUlangMahasiswa*Action} menyaring dengan
 *       {@code Restrictions.isNull("minSmt")} — yang dievaluasi di SQL atas kolom
 *       {@code min_smt_detail}, sehingga TIDAK melihat pewarisan dari induk. Rentang
 *       induk disaring lewat alias {@code settingBiaya.minSmt} secara terpisah, jadi
 *       hasil akhirnya setara dengan semantik getter — tetapi kesetaraan itu kebetulan
 *       terjaga di query, bukan dijamin oleh model.</li>
 *   <li><b>Javadoc bawaan hbm2java salah kelas.</b> Sebelum revisi ini header file
 *       tertulis "JadwalPelajaran generated by hbm2java" — sisa salin-tempel generator,
 *       tidak ada hubungannya dengan jadwal pelajaran.</li>
 *   <li><b>Tidak ada unique constraint</b> pada pasangan (settingBiaya, mahasiswa) atau
 *       (settingBiaya, biodataCalonMahasiswa) di sisi model; pencegahan baris ganda
 *       sepenuhnya bergantung pada kode pemanggil.</li>
 * </ul>
 *
 * <h2>Catatan kontrol akses</h2>
 * <p>Berbeda dari banyak entity lain di modul ini, jalur masuknya justru <b>terjaga</b>:
 * layar induk {@code /pages/master/setting_biaya.zul} termasuk dalam whitelist
 * {@code CommonPrivilages.MUST_CHECKED}, dan komponen detail yang mengedit baris-baris
 * ini hanya dirender bila {@code CommonPrivilages.checkPrevilages(UPDATE)} bernilai true
 * pada {@code SetingBiayaAction}. Yang perlu dicatat: {@code DetailSettingBiayaAction}
 * sendiri tidak memuat satu pun pemeriksaan hak akses internal, jadi proteksinya
 * sepenuhnya menempel pada layar induk (tidak ada lapis kedua).</p>
 *
 * <h2>Tentang {@link GeneralValueObject}</h2>
 * <p>{@code GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass} — ia POJO abstrak biasa, sehingga Hibernate tidak memetakan
 * properti apa pun miliknya. Karena itu {@code id}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} <b>harus</b> dideklarasikan ulang di setiap entity turunan agar
 * ikut terpetakan. Pengulangan tersebut adalah keharusan teknis, bukan bug atau duplikasi
 * yang perlu dirapikan.</p>
 *
 * @see GeneralValueObject
 * @see SettingBiaya
 * @see DetailSettingBiaya
 * @see DetailBiaya
 * @see PengaturanPembayaranBulanan
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "setting_biaya_detail", schema = "public")
public class SettingBiayaDetail extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai ini dibangkitkan sekali saat kelas dibuat dan harus
	 * dipertahankan apa adanya; mengubahnya memutus kompatibilitas deserialisasi objek
	 * yang tersimpan di session/cache ZK maupun yang dikirim antar node.
	 */
	private static final long serialVersionUID = 7154228487700348608L;
	/**
	 * Primary key baris binding ini (kolom {@code id}, IDENTITY). Dideklarasikan ulang di
	 * sini karena {@link GeneralValueObject} tidak dipetakan Hibernate — lihat Javadoc
	 * kelas.
	 */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi jejak audit aplikasi. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; pasangan teknis dari {@link #oleh}. */
	private String olehId;

	/**
	 * @return id pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum
	 *         pernah terisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatikan:</b> nilai {@code null}, string kosong, atau string yang hanya
	 * berisi spasi <b>diabaikan diam-diam</b> — method langsung {@code return} dan nilai
	 * lama dipertahankan. Ini disengaja agar jejak audit tidak terhapus oleh binding UI
	 * yang mengirim nilai kosong, tetapi berarti field ini tidak bisa dikosongkan lewat
	 * setter.</p>
	 *
	 * @param olehId id pengguna; nilai kosong/blank tidak berefek apa pun.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Sama seperti
	 * {@link #setOlehId(String)}, nilai {@code null}/kosong/blank diabaikan diam-diam
	 * dan nilai lama dipertahankan.
	 *
	 * @param oleh nama pengguna; nilai kosong/blank tidak berefek apa pun.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum
	 *         pernah terisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA yang dijalankan otomatis <b>tepat sebelum UPDATE</b> baris ini,
	 * mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} untuk mengisi
	 * {@link #tanggal_dirubah} (dan jejak pengguna bila konteks tersedia).
	 *
	 * <p>Tidak dipanggil manual dari kode mana pun — pemicunya adalah provider JPA/
	 * Hibernate. Perhatikan tidak ada pasangan {@code @PrePersist}: pada INSERT,
	 * {@code tanggal_dirubah} hanya berisi nilai inisialisasi field.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir baris ini. Diinisialisasi ke waktu sekarang lewat
	 * {@code WaktuUtil.getDate()} saat objek dibuat, lalu diperbarui otomatis oleh
	 * {@link #onUpdate()} pada setiap UPDATE.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah waktu perubahan terakhir; menimpa nilai yang ada tanpa
	 *                        validasi. Umumnya diisi
	 *                        {@code AuditTimestampInterceptor}, bukan kode aplikasi.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini (presisi TIMESTAMP). */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Skema biaya induk yang diikat oleh baris ini. Sekaligus sumber nilai warisan untuk
	 * {@link #getMinSmt()}/{@link #getMaxSmt()} bila kolom lokalnya kosong.
	 */
	private SettingBiaya settingBiaya;
	/**
	 * Mahasiswa aktif penerima skema biaya ini. Terisi pada mode "khusus per mahasiswa";
	 * {@code null} bila baris ini milik calon mahasiswa.
	 */
	private Mahasiswa mahasiswa;
	/**
	 * Calon mahasiswa/pendaftar penerima skema biaya ini. Terisi pada mode "khusus per
	 * calon mahasiswa"; {@code null} bila baris ini milik mahasiswa aktif.
	 */
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	/**
	 * Semester terkecil saat skema ini berlaku bagi orang tsb; {@code null} berarti
	 * "ikuti rentang milik {@link SettingBiaya} induk" — lihat {@link #getMinSmt()}.
	 */
	private Integer minSmt;
	/**
	 * Semester terbesar saat skema ini berlaku bagi orang tsb; {@code null} berarti
	 * "ikuti rentang milik {@link SettingBiaya} induk" — lihat {@link #getMaxSmt()}.
	 */
	private Integer maxSmt;
	/**
	 * Kuota custom per item biaya dalam bentuk string JSON ({@code {"<idItemBiaya>":nominal}}).
	 * Lihat Javadoc kelas untuk format dan urutan prioritasnya saat tagihan diterbitkan.
	 */
	private String biayas;

	/**
	 * Konstruktor tanpa argumen. Diperlukan Hibernate untuk instansiasi, dan dipakai kode
	 * UI saat membuat binding baru untuk seorang mahasiswa/calon.
	 */
	public SettingBiayaDetail() {
	}

	/**
	 * @return primary key baris ini, atau {@code null} bila belum pernah disimpan.
	 *         Kolom ditandai {@code insertable = false} karena nilainya dibangkitkan
	 *         database (IDENTITY/sequence), bukan oleh aplikasi.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id primary key; normalnya hanya diisi Hibernate saat memuat/menyimpan baris. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mahasiswa aktif yang diikat oleh baris ini.
	 *
	 * <p>Memanggil {@code check(...)} milik {@link GeneralValueObject} lebih dulu untuk
	 * meresolusi proxy lazy, dan <b>hasilnya ditugaskan kembali ke field</b> — pola wajib
	 * di seluruh entity repo ini. Konsekuensinya getter ini bukan operasi murni memori:
	 * bila proxy sudah detached, {@code check(...)} dapat membaca cache, menginisialisasi
	 * ulang proxy, atau membuka session Hibernate baru sebagai upaya terakhir.</p>
	 *
	 * @return mahasiswa penerima, atau {@code null} bila baris ini milik calon mahasiswa.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * @param mahasiswa mahasiswa aktif penerima skema biaya ini. Tidak ada validasi
	 *                  saling-eksklusif terhadap {@link #setBiodataCalonMahasiswa}; kode
	 *                  pemanggil yang bertanggung jawab hanya mengisi salah satu.
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Skema biaya {@link SettingBiaya} induk dari binding ini.
	 *
	 * <p>Sama seperti getter relasi lain, memanggil {@code check(...)} dan menugaskan
	 * hasilnya kembali ke field (bisa menyentuh cache/DB). Getter ini juga dipanggil dari
	 * dalam {@link #getMinSmt()}/{@link #getMaxSmt()} untuk mengambil rentang semester
	 * warisan, sehingga pembacaan rentang semester pun berpotensi memicu pemuatan
	 * relasi.</p>
	 *
	 * @return SettingBiaya induk, atau {@code null} bila relasi belum diisi (kolom
	 *         {@code setting_biaya} nullable).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "setting_biaya", nullable = true)
	public SettingBiaya getSettingBiaya() {
		settingBiaya = check(settingBiaya);
		return settingBiaya;
	}

	/** @param settingBiaya skema biaya induk yang diikat baris ini. */
	public void setSettingBiaya(SettingBiaya settingBiaya) {
		this.settingBiaya = settingBiaya;
	}

	/**
	 * Calon mahasiswa/pendaftar yang diikat oleh baris ini. Memanggil {@code check(...)}
	 * dan menugaskan hasilnya kembali ke field, dengan konsekuensi yang sama seperti
	 * {@link #getMahasiswa()}.
	 *
	 * @return biodata calon mahasiswa penerima, atau {@code null} bila baris ini milik
	 *         mahasiswa aktif.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "biodata_calon_mahasiswa", nullable = true)
	public BiodataCalonMahasiswa getBiodataCalonMahasiswa() {
		biodataCalonMahasiswa = check(biodataCalonMahasiswa);
		return biodataCalonMahasiswa;
	}

	/**
	 * @param biodataCalonMahasiswa calon mahasiswa penerima skema biaya ini. Tidak ada
	 *                              validasi saling-eksklusif terhadap
	 *                              {@link #setMahasiswa}.
	 */
	public void setBiodataCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	/**
	 * Objek JSON kosong yang dipakai <b>hanya</b> sebagai sumber string {@code "{}"} pada
	 * {@link #getBiayas()}. Tidak pernah dimutasi di kelas ini dan bukan cache bersama;
	 * karena tidak pernah berubah, statis-nya tidak menimbulkan masalah konkurensi.
	 */
	private static JSONObject jsonObject = new JSONObject();

	/**
	 * Kuota custom per item biaya sebagai string JSON.
	 *
	 * <p><b>Tidak pernah mengembalikan {@code null}:</b> bila field kosong/blank, yang
	 * dikembalikan adalah string {@code "{}"} (dari {@link #jsonObject}), sehingga
	 * pemanggil dapat langsung melakukan {@code new JSONObject(getBiayas())} tanpa cek
	 * null — dan itulah yang dilakukan {@link DetailBiaya#getNilaiBiaya()} serta layar
	 * {@code DetailSettingBiayaAction}.</p>
	 *
	 * <p><b>Efek samping tidak langsung:</b> anotasi pemetaan berada pada getter, jadi
	 * inilah nilai yang dibaca Hibernate saat insert maupun dirty-check. Baris dengan
	 * kolom {@code biayas} bernilai {@code NULL} di database karenanya berpotensi
	 * ter-UPDATE menjadi {@code "{}"} pada flush berikutnya — perubahan yang tidak
	 * berasal dari aksi pengguna mana pun. Field di memori sendiri tetap {@code null}
	 * karena hasil normalisasi ini tidak ditugaskan kembali.</p>
	 *
	 * @return string JSON berisi pasangan {@code "<idItemBiaya>" : nominal}; minimal
	 *         {@code "{}"}.
	 */
	@Column(columnDefinition = "text", name = "biayas")
	public String getBiayas() {
		return biayas == null || biayas.trim().isEmpty() ? jsonObject.toString() : biayas;
	}

	/**
	 * @param biayas string JSON kuota custom. Tidak divalidasi bentuknya di sini — JSON
	 *               rusak baru akan menimbulkan exception saat dibaca pemanggil
	 *               ({@link DetailBiaya#getNilaiBiaya()} menangkapnya dan mencatat ke
	 *               audit error, sehingga nominal bisa diam-diam jatuh ke nilai default).
	 *               Ditulis dari listener {@code onChange} kotak "Kuota Custom
	 *               (Template)" pada {@code DetailSettingBiayaAction}, yang langsung
	 *               memanggil {@code Common.refreshUpdate(...)} sehingga perubahan
	 *               tersimpan seketika per ketikan, tanpa tombol Simpan.
	 */
	public void setBiayas(String biayas) {
		this.biayas = biayas;
	}

	/**
	 * Semester terkecil saat skema biaya ini berlaku bagi orang yang diikat.
	 *
	 * <p><b>Bukan getter trivial — ada pewarisan:</b> bila kolom lokal
	 * {@code min_smt_detail} kosong, nilai diambil dari {@link SettingBiaya} induk
	 * ({@code getSettingBiaya().getMinSmt()}, yang sendiri mengembalikan {@code 0} bila
	 * kosong). Hasil {@code null} hanya mungkin bila relasi ke SettingBiaya juga
	 * {@code null}.</p>
	 *
	 * <p><b>Konsekuensi penting:</b> karena pemetaan memakai <i>property access</i>,
	 * nilai warisan inilah yang dibaca Hibernate saat menyimpan. Sekali baris ini
	 * ter-flush, kolom {@code min_smt_detail} berisi angka konkret dan baris berhenti
	 * mewarisi — perubahan rentang semester di SettingBiaya induk tidak lagi ikut
	 * berlaku untuk orang ini. Perhatikan pula bahwa getter ini memanggil
	 * {@link #getSettingBiaya()}, yang dapat menyentuh cache/database.</p>
	 *
	 * <p>Nilai ini dibandingkan dengan semester berjalan mahasiswa oleh mesin tagihan
	 * daftar ulang ({@code DaftarUlangMahasiswaBaruAction}/
	 * {@code DaftarUlangMahasiswaLamaAction}, method {@code diLuarRangeTagihan}) dan
	 * ditampilkan sebagai combobox "Semester berlaku" di
	 * {@code DetailSettingBiayaAction.renderRentangSemester}.</p>
	 *
	 * @return batas bawah semester berlaku, hasil pewarisan bila kolom lokal kosong.
	 */
	@Column(name = "min_smt_detail")
	public Integer getMinSmt() {
		return minSmt == null ? (getSettingBiaya() == null ? null : getSettingBiaya().getMinSmt()) : minSmt;
	}

	/**
	 * @param minSmt batas bawah semester berlaku khusus untuk orang ini; {@code null}
	 *               mengembalikan perilaku "ikuti rentang SettingBiaya induk" (selama
	 *               baris belum ter-flush dengan nilai warisan). Diisi dari listener
	 *               {@code onSelect} combobox semester yang langsung memanggil
	 *               {@code Common.refreshUpdate(...)} — perubahan tersimpan seketika.
	 */
	public void setMinSmt(Integer minSmt) {
		this.minSmt = minSmt;
	}

	/**
	 * Semester terbesar saat skema biaya ini berlaku bagi orang yang diikat.
	 *
	 * <p>Perilakunya cermin dari {@link #getMinSmt()}: bila kolom lokal
	 * {@code max_smt_detail} kosong, nilai diambil dari {@link SettingBiaya} induk,
	 * berikut seluruh konsekuensi "nilai warisan ikut tersimpan" yang dijelaskan di
	 * sana.</p>
	 *
	 * <p><b>Efek samping berantai yang tidak kasat mata:</b>
	 * {@code SettingBiaya.getMaxSmt()} sendiri <i>menulis balik</i> ke field-nya
	 * ({@code maxSmt = 30}) bila kosong. Jadi sekadar membaca {@code getMaxSmt()} di sini
	 * dapat mengubah state {@link SettingBiaya} induk yang sedang dikelola session, dan
	 * pada flush berikutnya menuliskan {@code 30} ke kolom {@code max_smt} tabel
	 * {@code setting_biaya}.</p>
	 *
	 * @return batas atas semester berlaku, hasil pewarisan bila kolom lokal kosong.
	 */
	@Column(name = "max_smt_detail")
	public Integer getMaxSmt() {
		return maxSmt == null ? (getSettingBiaya() == null ? null : getSettingBiaya().getMaxSmt()) : maxSmt;
	}

	/**
	 * @param maxSmt batas atas semester berlaku khusus untuk orang ini; {@code null}
	 *               mengembalikan perilaku "ikuti rentang SettingBiaya induk". Sama
	 *               seperti {@link #setMinSmt(Integer)}, diisi dari combobox semester
	 *               yang menyimpan seketika lewat {@code Common.refreshUpdate(...)}.
	 */
	public void setMaxSmt(Integer maxSmt) {
		this.maxSmt = maxSmt;
	}

}
