package ais.database.model;

// Generated Apr 6, 2010 12:57:06 PM by Hibernate Tools 3.2.4.CR1

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

/**
 * Entity <b>satu komentar/catatan bimbingan pada KRS mahasiswa</b> (tabel {@code public.komentar}) —
 * satu baris berisi teks komentar ({@link #getKomentar()}) yang ditempelkan pada Kartu Rencana Studi
 * seorang mahasiswa untuk kombinasi <i>semester</i> + <i>tahapan</i> + <i>tahun akademik</i> +
 * <i>semester pendek</i> tertentu. Dipakai sebagai sarana komunikasi tertulis dua arah antara dosen
 * pembimbing akademik / petugas akademik dengan mahasiswa pada halaman KRS: komentar yang tersimpan
 * tampil di halaman KRS mahasiswa yang bersangkutan <b>dan</b> di halaman dosen pembimbing
 * akademiknya, sekaligus dikirimkan sebagai email notifikasi berlampiran PDF cetak KRS.
 *
 * <h3>Status pemakaian: AKTIF (bukan entity yatim)</h3>
 * <p>Berbeda dari {@link ChatMessage} atau {@link MenuMobile} yang hanya terdaftar di
 * {@code hibernate.cfg.xml} tanpa satu pun pemakai, entity ini <b>benar-benar dipakai</b> di jalur
 * produksi. Peta pemakaiannya:</p>
 * <ul>
 * <li><b>Satu-satunya penulis:</b> {@code ais.action.master.helper.KomentarHelper} — dialog
 * "Masukkan komentar Anda" yang dibuka tombol <i>Komentar</i> pada layar KRS. Ini pula satu-satunya
 * tempat {@code new Komentar()} dipanggil di seluruh <i>source tree</i>.</li>
 * <li><b>Pembaca utama:</b> {@code Common.loadKomentarData(...)} (memuat daftar terurut
 * {@code tanggal} untuk satu mahasiswa/semester/tahapan/tahun ajaran) dan
 * {@code Common.loadKomentarUkuran(...)} (hitung jumlah saja, memakai session Hibernate
 * <i>terpisah</i> karena juga dipanggil dari thread latar).</li>
 * <li><b>Perender layar:</b> {@code Common.KomentarRenderer} — dipasang pada {@code gridKomentar}
 * di {@code KrsHelper}, {@code KrsPaketHelper}, {@code KrsNonPaketHelper},
 * {@code KrsKurikulumHelper} dan {@code StudiMahasiswaHelper}.</li>
 * <li><b>Pembaca lain:</b> {@code AksiKrsMahasiswaHelper} (panel kartu komentar ringkas per
 * mahasiswa) dan {@code ais.action.master.dashboard.admin.DashboardKomentarMahasiswaKRS} (ekspor
 * XLSX komentar KRS seluruh mahasiswa, disaring fakultas/jurusan/program/angkatan/dosen PA).</li>
 * <li><b>Penghapus berantai:</b> {@code DetailperkuliahanAction}, {@code IkutPerkuliahanHelper} dan
 * {@code StudiMahasiswaHelper} menghapus baris {@code Komentar} yang {@link #getDetailperkuliahan()}
 * -nya sama dengan id baris {@code Detailperkuliahan} yang sedang dihapus, sebelum menghapus baris
 * matakuliah itu sendiri (agar tidak menyisakan komentar menggantung).</li>
 * </ul>
 *
 * <h3>JEBAKAN PENAMAAN — tujuh entity berbeda yang sama-sama bernama "…Komentar…"</h3>
 * <p>Nama {@code Komentar} tanpa akhiran <b>khusus komentar KRS</b>. Yang lain punya tabel, layar
 * dan pemilik yang sama sekali berbeda dan <b>tidak boleh</b> tertukar:</p>
 * <ul>
 * <li>{@link KomentarPerkuliahan} (tabel {@code komentar_perkuliahan}) — komentar pada sebuah
 * <i>kelas/perkuliahan</i> hasil penilaian, ditulis {@code KomentarPerkuliahanHelper}. Meski nama
 * helper-nya nyaris sama, entity yang ditulisnya bukan class ini.</li>
 * <li>{@link DiskusiKomentar} — komentar berulir pada modul jurnal/diskusi.</li>
 * <li>{@code ais.database.model.ticket.TicketKomentar}, {@code ais.database.model.sop.KomentarDisposisi},
 * {@code ais.database.model.rab.InformasiRabKomentar}, {@code ais.database.model.library.ItemKomentar}
 * (dan kerabatnya), serta {@code ais.database.model.inventory.ProdukKomentar} — masing-masing milik
 * modul tiket, SOP/disposisi, RAB, perpustakaan dan inventori.</li>
 * </ul>
 * <p>Selain itu {@code ais.common.MateriDanKomentarHelper} sama sekali <b>tidak</b> menyentuh entity
 * ini — ia mengagregasi diskusi pertemuan e-learning.</p>
 *
 * <h3>Relasi dengan {@code GeneralValueObject}</h3>
 * <p>Class ini turunan langsung {@link ais.database.model.GeneralValueObject}, yang <b>bukan</b>
 * {@code @Entity} maupun {@code @MappedSuperclass} melainkan POJO abstrak biasa — Hibernate
 * <b>tidak</b> memetakan properti induknya. Karena itu deklarasi ULANG {@link #id}, {@link #oleh},
 * {@link #olehId} dan {@link #tanggal_dirubah} di sini <b>bukan bug atau duplikasi ceroboh</b>,
 * melainkan keharusan teknis supaya keempat kolom itu ikut terpetakan. Konsekuensinya field-field
 * tersebut <b>membayangi (shadow)</b> field senama milik induk; yang terbaca dari luar selalu versi
 * milik {@code Komentar} ini. Fasilitas induk yang tetap dipakai: {@code check(...)} (resolusi proxy
 * lazy) pada {@link #getTbmuser()}.</p>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ol>
 * <li><b>Identitas &amp; penyajian:</b> {@link #getId()} (PK {@code IDENTITY}), {@link #toString()},
 * konstruktor tanpa argumen {@link #Komentar()}.</li>
 * <li><b>Isi komentar:</b> {@link #getKomentar()} (badan teks, kolom {@code text}),
 * {@link #getTanggal()} (waktu komentar dibuat, diisi eksplisit oleh penulis).</li>
 * <li><b>Penunjuk konteks KRS:</b> {@link #getMahasiswa()} (pemilik KRS),
 * {@link #getSemester()}, {@link #getTahapan()}, {@link #getTahunAkademik()},
 * {@link #getSemesterPendek()} — kelimanya persis kunci penyaring
 * {@code Common.loadKomentarData(...)}.</li>
 * <li><b>Penunjuk sasaran/pembuat:</b> {@link #getDetailperkuliahan()} (id baris matakuliah KRS,
 * lihat catatan sentinel {@code -1} di bawah), {@link #getDosen()},
 * {@link #getTbmuser()} (perhatikan efek samping destruktifnya).</li>
 * <li><b>Jejak audit (deklarasi ulang milik induk):</b> {@link #getOleh()}, {@link #getOlehId()},
 * {@link #getTanggal_dirubah()}, kait {@link #onUpdate()}.</li>
 * </ol>
 * <p>Tidak ada method bisnis, tidak ada method query statis, tidak ada validasi dan tidak ada
 * konstanta di class ini: seluruh logika (validasi "komentar tidak boleh kosong", pengisian
 * sentinel, notifikasi email) berada di {@code KomentarHelper}, dan seluruh query ada di
 * {@code Common}. Class ini murni kantong data + satu getter berefek samping.</p>
 *
 * <h3>Hal non-obvious yang WAJIB diketahui pemelihara</h3>
 * <ul>
 * <li><b>{@link #getTbmuser()} adalah getter DESTRUKTIF — verifikasi langsung atas isi file ini.</b>
 * Bila {@link #mahasiswa} tidak {@code null}, getter ini <b>menulis {@code null} ke field
 * {@code tbmuser}</b> sebelum mengembalikannya. Karena pemetaan entity ini memakai <i>property
 * access</i> (anotasi menempel pada getter, lihat {@code @Id} di {@link #getId()}), Hibernate
 * membaca nilai lewat getter tersebut saat {@code INSERT} maupun saat pemeriksaan <i>dirty</i>
 * ketika flush — sehingga <b>kolom {@code tbmuser} di database ikut dikosongkan secara permanen</b>.
 * Efek nyatanya: meskipun {@code KomentarHelper} memanggil {@code setTbmuser(pengguna_saat_ini)}
 * sebelum menyimpan, komentar KRS (yang selalu punya {@code mahasiswa}) <b>tidak pernah</b> berhasil
 * menyimpan identitas penulisnya di kolom itu. Bukti pendukung dari sisi pembaca:
 * {@code AksiKrsMahasiswaHelper} justru menyaring dengan {@code Restrictions.isNull("tbmuser")} untuk
 * menampilkan komentar — konsisten dengan kenyataan bahwa semua baris komentar KRS berkolom
 * {@code tbmuser} kosong. Hanya baris yang <b>tidak</b> punya {@code mahasiswa} yang bisa
 * mempertahankan {@code tbmuser}-nya.</li>
 * <li><b>Akibat lanjutan: identitas penulis komentar diambil dari kolom AUDIT, bukan dari relasi
 * pengguna.</b> {@code Common.KomentarRenderer} dan ekspor {@code DashboardKomentarMahasiswaKRS}
 * menampilkan {@link #getOleh()} sebagai "penulis komentar". Padahal {@code oleh}/{@code olehId}
 * adalah metadata audit generik yang diisi otomatis
 * {@code ais.database.hibernate.AuditTimestampInterceptor} untuk <i>setiap</i> penyimpanan — bukan
 * data bisnis. Konsekuensinya: bila baris komentar kelak ter-{@code UPDATE} oleh proses/pengguna
 * lain (apa pun sebabnya), nama penulis yang tampil di layar <b>ikut berubah</b> menjadi nama
 * pengguna terakhir yang menyimpan. Ini persis anti-pola yang pernah menimbulkan bug pada
 * {@code SesiKasKasir} dan diperbaiki di sana dengan memberi kolom bisnis tersendiri; di sini
 * pola itu masih berlaku.</li>
 * <li><b>{@link #getDetailperkuliahan()} bukan relasi, dan nilai {@code -1L} adalah SENTINEL.</b>
 * Kolomnya {@code bigint NOT NULL} biasa tanpa {@code @ManyToOne}/{@code @JoinColumn}, jadi tidak
 * ada <i>foreign key</i> maupun jaminan integritas ke {@code Detailperkuliahan}.
 * {@code KomentarHelper} selalu mengisinya {@code -1L}, artinya "komentar level KRS, bukan komentar
 * pada satu matakuliah". Nilai id sungguhan hanya ada pada baris warisan versi lama: <b>tidak ada
 * satu pun kode yang masih menulis</b> {@code Komentar} dengan id {@code Detailperkuliahan} nyata,
 * yang tersisa hanya kode <i>penghapus berantai</i>-nya. Jadi jalur "komentar per matakuliah"
 * praktis sudah mati sebagai fitur, sementara kode pembersihnya tetap dipelihara.</li>
 * <li><b>Tidak ada getter lain yang berefek samping.</b> Diverifikasi langsung atas seluruh isi file
 * ini: selain {@link #getTbmuser()}, tidak ada getter yang menulis balik ke field, tidak ada getter
 * yang membuka/menutup {@code Session} Hibernate, dan tidak ada getter yang menghapus baris DB.
 * {@link #getMahasiswa()} dan {@link #getDosen()} mengembalikan field apa adanya tanpa
 * {@code check(...)} — berbeda dari {@link #getTbmuser()} yang memanggilnya, karena hanya relasi
 * {@code tbmuser} yang dipetakan {@code FetchType.LAZY}.</li>
 * <li><b>Dua setter "menolak diam-diam".</b> {@link #setOleh(String)} dan
 * {@link #setOlehId(String)} langsung {@code return} bila nilai baru {@code null} atau kosong,
 * sehingga jejak audit yang sudah terisi tidak dapat dikosongkan lagi lewat setter. Ini pola
 * berulang di seluruh keluarga {@code GeneralValueObject}, bukan kekhususan file ini.</li>
 * <li><b>{@link #toString()} bisa mengembalikan {@code null}</b> (mengembalikan {@link #komentar}
 * mentah, dan kolom {@code komentar} nullable). Hati-hati bila objek ini dipakai langsung dalam
 * penggabungan string atau sebagai label komponen ZK.</li>
 * <li><b>{@code @Audited} (Hibernate Envers) aktif.</b> Setiap {@code INSERT}/{@code UPDATE}/
 * {@code DELETE} baris komentar direkam ke tabel revisi {@code komentar_AUD}. Artinya komentar yang
 * dihapus dari layar masih dapat direkonstruksi dari tabel audit — penting untuk penelusuran
 * sengketa bimbingan akademik.</li>
 * <li><b>Catatan kontrol akses (bukan milik file ini, tetapi relevan bagi pemakainya).</b> Tombol
 * hapus pada {@code Common.KomentarRenderer} dirender <b>tanpa pemeriksaan hak akses apa pun</b>
 * (langsung {@code Common.refreshDelete(...)}), sedangkan grid komentar itu ikut tampil di layar KRS
 * milik mahasiswa sendiri ({@code NewKrsAction} &rarr; {@code KrsHelper}). Demikian pula badan
 * komentar dirender sebagai HTML mentah lewat {@code ais.ui.util.MyHtml} yang hanya menyaring kata
 * {@code script}, bukan atribut event HTML. Rincian ini didokumentasikan di sini agar pemelihara
 * entity sadar bahwa isi kolom {@link #getKomentar()} adalah <b>masukan pengguna yang dirender
 * sebagai markup</b>, bukan teks polos.</li>
 * </ul>
 *
 * @see ais.database.model.GeneralValueObject
 * @see KomentarPerkuliahan
 * @see Mahasiswa
 * @see Dosen
 * @see Tbmuser
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "komentar")

public class Komentar extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilai tetap dari generator {@code hbm2java}; jangan diubah
	 * agar objek yang pernah diserialisasi (mis. ke sesi ZK) tetap dapat dibaca kembali.
	 */
	private static final long serialVersionUID = 957348988610721296L;

	/**
	 * Kunci primer baris komentar (kolom {@code id}, {@code IDENTITY}). Dideklarasikan ulang di sini
	 * karena {@link GeneralValueObject} bukan {@code @MappedSuperclass}.
	 */
	private Long id;

	/**
	 * Kait JPA {@code @PreUpdate} sekaligus deklarasi field {@link #tanggal_dirubah} (keduanya
	 * ditulis pada satu baris fisik oleh generator; format ini dipertahankan apa adanya).
	 *
	 * <p>{@code onUpdate()} dipanggil <b>otomatis oleh Hibernate/JPA</b> tepat sebelum baris ini
	 * di-{@code UPDATE}, lalu mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang menyegarkan
	 * {@link #tanggal_dirubah} serta {@link #oleh}/{@link #olehId} dari pengguna sesi berjalan.
	 * Tidak ada padanan {@code @PrePersist}: untuk baris <b>baru</b>, ketiga kolom audit itu diisi
	 * lewat jalur lain, yaitu {@code AuditTimestampInterceptor.onSave(...)} pada level interceptor
	 * Hibernate.</p>
	 *
	 * <p>Field {@link #tanggal_dirubah} sendiri diinisialisasi seketika saat objek dibuat dengan
	 * {@code ais.ui.util.WaktuUtil.getDate()} (waktu server, menghormati penyetelan zona/offset
	 * aplikasi), bukan {@code new Date()} langsung.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir baris ini. Umumnya <b>tidak</b> dipanggil kode bisnis —
	 * pengisiannya diurus {@link #onUpdate()}/{@code AuditTimestampInterceptor}.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir yang ingin dicatat
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (kolom {@code tanggal_dirubah}, dipetakan
	 * sebagai {@code TIMESTAMP}).
	 *
	 * <p><b>Jangan disamakan dengan {@link #getTanggal()}</b>: yang ini adalah stempel audit teknis
	 * "kapan baris terakhir disimpan", sedangkan {@link #getTanggal()} adalah tanggal komentar yang
	 * ditampilkan ke pengguna.</p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada objek yang baru dibuat karena
	 *         field-nya diinisialisasi di deklarasi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks objek: mengembalikan isi komentar apa adanya.
	 *
	 * <p><b>Dapat mengembalikan {@code null}</b> bila {@link #komentar} belum terisi (kolomnya
	 * nullable), berbeda dari kebiasaan {@code toString()} yang selalu mengembalikan string. Nilai
	 * yang dikembalikan juga masih berupa markup mentah sebagaimana disimpan.</p>
	 *
	 * @return isi komentar, atau {@code null} bila belum terisi
	 */
	public String toString() {
		return komentar;
	}

	/**
	 * Id baris {@code Detailperkuliahan} yang dikomentari, atau sentinel {@code -1L} untuk komentar
	 * level KRS. Disimpan sebagai angka mentah, bukan relasi.
	 */
	private Long detailperkuliahan;

	/** Badan teks komentar (kolom {@code komentar} bertipe {@code text}). */
	private String komentar;

	/** Metadata audit: nama pengguna terakhir yang menyimpan baris ini. */
	private String oleh;

	/** Metadata audit: id pengguna terakhir yang menyimpan baris ini. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang menyimpan baris ini (metadata audit, kolom
	 * {@code oleh_id}).
	 *
	 * @return id pengguna penyimpan terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna penyimpan terakhir. <b>Menolak diam-diam</b> nilai {@code null} maupun
	 * string kosong/spasi: pada kasus itu method langsung {@code return} sehingga nilai lama tetap
	 * bertahan (jejak audit tidak dapat dikosongkan lewat setter ini).
	 *
	 * @param olehId id pengguna penyimpan; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Tanggal/waktu komentar dibuat, diisi eksplisit oleh {@code KomentarHelper} dan dipakai sebagai
	 * kunci pengurutan tampilan.
	 */
	private Date tanggal;

	/** Mahasiswa pemilik KRS yang dikomentari; menjadi kunci penyaring utama saat pemuatan. */
	private Mahasiswa mahasiswa;

	/** Dosen (biasanya pembimbing akademik) yang tercatat sebagai konteks komentar; boleh kosong. */
	private Dosen dosen;

	/**
	 * Akun pengguna penulis komentar. <b>Praktis selalu berakhir kosong di database</b> untuk
	 * komentar KRS — lihat efek samping destruktif pada {@link #getTbmuser()}.
	 */
	private Tbmuser tbmuser;

	/** Nomor semester KRS yang dikomentari. */
	private Integer semester;

	/** Tahapan pengambilan KRS yang dikomentari ({@code null}/{@code 0} berarti tanpa tahapan). */
	private Integer tahapan;

	/** Tahun akademik KRS yang dikomentari (kolom {@code tahun_akademik}), mis. {@code "2025/2026"}. */
	private String tahunAkademik;

	/**
	 * Penanda semester pendek (kolom {@code semester_pendek}); {@code null} berarti komentar pada KRS
	 * semester reguler.
	 */
	private Integer semesterPendek;

	/**
	 * Konstruktor tanpa argumen. Wajib ada untuk Hibernate (instansiasi saat memuat baris dari
	 * database) sekaligus dipakai {@code KomentarHelper} saat membuat komentar baru.
	 */
	public Komentar() {
	}

	/**
	 * Mengembalikan kunci primer baris ini.
	 *
	 * <p>Kolom dipetakan {@code insertable = false} karena nilainya dibangkitkan database
	 * ({@code IDENTITY}/sequence PostgreSQL), bukan dikirim aplikasi. Kehadiran {@code @Id} pada
	 * getter inilah yang menetapkan seluruh entity memakai <i>property access</i> — fakta yang
	 * membuat efek samping di {@link #getTbmuser()} ikut terbawa sampai ke database.</p>
	 *
	 * @return id baris, atau {@code null} bila objek belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci primer baris ini. Normalnya hanya dipanggil Hibernate.
	 *
	 * @param id kunci primer baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan penunjuk baris matakuliah KRS yang dikomentari (kolom {@code detailperkuliahan},
	 * {@code NOT NULL}).
	 *
	 * <p><b>Bukan relasi.</b> Nilainya angka mentah tanpa {@code @ManyToOne}/{@code @JoinColumn},
	 * sehingga tidak ada <i>foreign key</i> maupun jaminan bahwa id yang tersimpan masih ada di tabel
	 * {@code detailperkuliahan}. Dua makna nilainya:</p>
	 * <ul>
	 * <li>{@code -1L} — <b>sentinel</b> "komentar level KRS" (bukan pada satu matakuliah). Ini
	 * satu-satunya nilai yang masih ditulis kode aktif, yaitu oleh {@code KomentarHelper}.</li>
	 * <li>id {@code Detailperkuliahan} sungguhan — baris warisan versi lama; tidak ada lagi kode
	 * yang menuliskannya. Yang tersisa hanya kode pembersih di {@code DetailperkuliahanAction},
	 * {@code IkutPerkuliahanHelper} dan {@code StudiMahasiswaHelper}, yang menghapus komentar
	 * ber-{@code detailperkuliahan} sama sebelum menghapus baris matakuliahnya.</li>
	 * </ul>
	 *
	 * @return id baris matakuliah yang dikomentari, atau {@code -1L} untuk komentar level KRS
	 */
	@Column(name = "detailperkuliahan", nullable = false)
	public Long getDetailperkuliahan() {
		return this.detailperkuliahan;
	}

	/**
	 * Menyetel penunjuk baris matakuliah KRS yang dikomentari. Kode aktif hanya mengisinya dengan
	 * sentinel {@code -1L}; lihat {@link #getDetailperkuliahan()} untuk arti nilainya.
	 *
	 * @param detailperkuliahan id {@code Detailperkuliahan}, atau {@code -1L} untuk komentar level
	 *                          KRS; tidak boleh {@code null} karena kolomnya {@code NOT NULL}
	 */
	public void setDetailperkuliahan(Long detailperkuliahan) {
		this.detailperkuliahan = detailperkuliahan;
	}

	/**
	 * Mengembalikan badan teks komentar (kolom {@code komentar}, tipe {@code text} sehingga panjang
	 * praktis tak terbatas).
	 *
	 * <p><b>Isinya adalah masukan pengguna dan dirender sebagai HTML.</b> {@code Common.KomentarRenderer}
	 * menyerahkannya ke {@code ais.ui.util.MyHtml} (turunan {@code org.zkoss.zul.Html}), sedangkan
	 * ekspor XLSX {@code DashboardKomentarMahasiswaKRS} justru membuang tag {@code <p>}/{@code </p>}
	 * darinya — dua perlakuan yang sama-sama mengasumsikan nilai di sini berupa markup, bukan teks
	 * polos. Perlakukan sebagai data tak tepercaya bila menambah pemakai baru.</p>
	 *
	 * @return isi komentar, atau {@code null} bila belum terisi
	 */
	@Column(name = "komentar", columnDefinition = "text")
	public String getKomentar() {
		return this.komentar;
	}

	/**
	 * Menyetel badan teks komentar. Tidak ada validasi di sini; pemeriksaan "komentar tidak boleh
	 * kosong" dilakukan di layar ({@code KomentarHelper}), bukan di entity.
	 *
	 * @param komentar isi komentar
	 */
	public void setKomentar(String komentar) {
		this.komentar = komentar;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang menyimpan baris ini (metadata audit, kolom
	 * {@code oleh}).
	 *
	 * <p><b>Dipakai sebagai "nama penulis komentar" di layar</b> oleh {@code Common.KomentarRenderer}
	 * dan ekspor {@code DashboardKomentarMahasiswaKRS}, meskipun secara rancangan ini kolom audit
	 * generik yang ditimpa ulang setiap kali baris disimpan. Lihat catatan pada Javadoc class untuk
	 * konsekuensinya.</p>
	 *
	 * @return nama pengguna penyimpan terakhir, atau {@code null} bila belum pernah terisi
	 */
	@Column(name = "oleh")
	public String getOleh() {
		return this.oleh;
	}

	/**
	 * Menyetel nama pengguna penyimpan terakhir. Sama seperti {@link #setOlehId(String)},
	 * <b>menolak diam-diam</b> nilai {@code null} maupun string kosong/spasi.
	 *
	 * @param oleh nama pengguna penyimpan; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan tanggal/waktu komentar dibuat (kolom {@code tanggal}, dipetakan
	 * {@code TIMESTAMP}).
	 *
	 * <p>Diisi eksplisit oleh {@code KomentarHelper} dengan {@code WaktuUtil.getDate()} saat komentar
	 * disimpan, dan menjadi kunci pengurutan daftar komentar
	 * ({@code Order.asc("tanggal")} pada {@code Common.loadKomentarData}, {@code Order.desc} pada
	 * {@code AksiKrsMahasiswaHelper}). Berbeda dari {@link #getTanggal_dirubah()} yang murni audit.</p>
	 *
	 * <p><b>Catatan pemetaan:</b> atribut {@code length = 0} pada {@code @Column} adalah artefak
	 * generator {@code hbm2java} dan tidak berpengaruh untuk kolom {@code TIMESTAMP}.</p>
	 *
	 * @return waktu komentar dibuat, atau {@code null} bila tidak diisi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal", length = 0)
	public Date getTanggal() {
		return this.tanggal;
	}

	/**
	 * Menyetel tanggal/waktu komentar dibuat.
	 *
	 * @param tanggal waktu komentar dibuat
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengembalikan mahasiswa pemilik KRS yang dikomentari (kolom {@code mahasiswa}, {@code nullable}).
	 *
	 * <p>Relasi {@code ManyToOne} <i>eager</i> (bawaan {@code ManyToOne}) dengan
	 * {@code FetchMode.SELECT}, artinya diambil lewat {@code SELECT} terpisah alih-alih {@code JOIN}
	 * — pola seragam di seluruh entity AIS untuk menghindari kartesius pada query daftar.
	 * {@code cascade = PERSIST, MERGE} membuat mahasiswa yang belum tersimpan ikut disimpan; tidak
	 * ada {@code REMOVE}, jadi menghapus komentar tidak menyentuh baris mahasiswa.</p>
	 *
	 * <p><b>Tidak</b> memanggil {@code check(...)} milik {@link GeneralValueObject} — berbeda dari
	 * {@link #getTbmuser()} — karena relasi ini tidak lazy. Getter ini bebas efek samping.</p>
	 *
	 * <p><b>Perhatikan:</b> nilai non-{@code null} di sini adalah pemicu efek samping destruktif pada
	 * {@link #getTbmuser()}.</p>
	 *
	 * @return mahasiswa pemilik KRS, atau {@code null} bila komentar tidak terikat mahasiswa
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		return mahasiswa;
	}

	/**
	 * Menyetel mahasiswa pemilik KRS yang dikomentari.
	 *
	 * @param mahasiswa mahasiswa pemilik KRS; boleh {@code null}
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Mengembalikan nomor semester KRS yang dikomentari.
	 *
	 * <p>Tanpa {@code @Column} eksplisit, sehingga terpetakan ke kolom bawaan {@code semester}.
	 * Dipakai sebagai penyaring di {@code Common.loadKomentarData} hanya ketika {@link #getTahapan()}
	 * kosong/{@code 0}; bila tahapan terisi, penyaring semester diabaikan dan digantikan penyaring
	 * tahapan.</p>
	 *
	 * @return nomor semester, atau {@code null} bila tidak diisi
	 */
	public Integer getSemester() {
		return semester;
	}

	/**
	 * Menyetel nomor semester KRS yang dikomentari.
	 *
	 * @param semester nomor semester
	 */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	/**
	 * Mengembalikan tahun akademik KRS yang dikomentari (kolom {@code tahun_akademik}).
	 *
	 * <p>Berupa string bebas seperti {@code "2025/2026"} — dibandingkan dengan kesetaraan persis
	 * ({@code Restrictions.eq}) saat pemuatan, jadi perbedaan format penulisan akan menyembunyikan
	 * komentar dari layar.</p>
	 *
	 * @return tahun akademik, atau {@code null} bila tidak diisi
	 */
	@Column(name = "tahun_akademik")
	public String getTahunAkademik() {
		return tahunAkademik;
	}

	/**
	 * Menyetel tahun akademik KRS yang dikomentari.
	 *
	 * @param tahunAkademik tahun akademik, mis. {@code "2025/2026"}
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Menyetel dosen konteks komentar.
	 *
	 * <p>{@code KomentarHelper} mengisinya dengan dosen milik pengguna yang sedang login
	 * ({@code Common.getCurrentUser().getDosen()}), sehingga bernilai {@code null} bila komentar
	 * ditulis pengguna non-dosen (mis. petugas akademik atau mahasiswa itu sendiri).</p>
	 *
	 * @param dosen dosen konteks komentar; boleh {@code null}
	 */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/**
	 * Mengembalikan dosen konteks komentar (kolom {@code dosen}, {@code nullable}).
	 *
	 * <p>Relasi {@code ManyToOne} <i>eager</i> dengan {@code FetchMode.SELECT} dan
	 * {@code cascade = PERSIST, MERGE}. Getter ini bebas efek samping dan tidak memanggil
	 * {@code check(...)}.</p>
	 *
	 * <p><b>Tidak dipakai untuk menyaring apa pun</b> di kode saat ini: pemuatan komentar selalu
	 * berporos pada {@link #getMahasiswa()}, dan layar menampilkan penulis dari {@link #getOleh()}.
	 * Nilainya praktis hanya jejak informasi tambahan.</p>
	 *
	 * @return dosen konteks komentar, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "dosen", nullable = true)
	public Dosen getDosen() {
		return dosen;
	}

	/**
	 * Menyetel akun pengguna penulis komentar.
	 *
	 * <p><b>Peringatan:</b> nilai yang disetel di sini tidak akan bertahan bila {@link #mahasiswa}
	 * terisi — {@link #getTbmuser()} mengosongkannya kembali sebelum Hibernate sempat menuliskannya.
	 * Lihat penjelasan lengkap di {@link #getTbmuser()}.</p>
	 *
	 * @param tbmuser akun pengguna penulis komentar; boleh {@code null}
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * Mengembalikan akun pengguna penulis komentar (kolom {@code tbmuser}, {@code nullable},
	 * {@code FetchType.LAZY}).
	 *
	 * <p><b>GETTER DESTRUKTIF — baca sebelum memakai.</b> Method ini melakukan dua hal sebelum
	 * mengembalikan nilai:</p>
	 * <ol>
	 * <li><b>Mengosongkan field.</b> Bila {@link #getMahasiswa()} tidak {@code null}, field
	 * {@code tbmuser} <b>ditimpa {@code null}</b>. Karena entity ini memakai <i>property access</i>
	 * (lihat {@link #getId()}), Hibernate membaca nilai lewat getter ini saat {@code INSERT} dan saat
	 * pemeriksaan <i>dirty</i> ketika flush — jadi pengosongan tersebut <b>ikut tersimpan permanen ke
	 * kolom database</b>, bukan sekadar efek di memori. Akibatnya komentar KRS (yang selalu punya
	 * mahasiswa) tidak pernah berhasil menyimpan identitas penulisnya di kolom ini; identitas penulis
	 * hanya tersisa di kolom audit {@link #getOleh()}. Bukti dari sisi pembaca:
	 * {@code AksiKrsMahasiswaHelper} menyaring komentar dengan {@code Restrictions.isNull("tbmuser")}.</li>
	 * <li><b>Menyelesaikan proxy lazy.</b> Memanggil {@code check(...)} milik
	 * {@link GeneralValueObject} untuk mengubah proxy Hibernate yang mungkin sudah <i>detached</i>
	 * menjadi objek nyata, lalu menyimpan hasilnya kembali ke field. Ini juga penulisan balik ke
	 * field, meski tidak merusak data.</li>
	 * </ol>
	 *
	 * <p>Konsekuensi praktis: <b>jangan</b> memanggil getter ini hanya untuk "mengintip" nilainya di
	 * dalam sesi Hibernate yang masih akan di-flush, dan jangan berharap kolom {@code tbmuser} bisa
	 * dipakai sebagai penanda kepemilikan komentar.</p>
	 *
	 * @return akun pengguna penulis komentar; hampir selalu {@code null} untuk komentar KRS
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		if (mahasiswa != null) {
			tbmuser = null;
		}
		tbmuser = check(tbmuser);
		return tbmuser;
	}

	/**
	 * Mengembalikan penanda semester pendek KRS yang dikomentari (kolom {@code semester_pendek}).
	 *
	 * <p>Penyaring pemuatan memperlakukannya tiga arah: bila parameter pencarian {@code null},
	 * {@code Common.loadKomentarData} menuntut kolom ini <b>juga</b> {@code null}
	 * ({@code Restrictions.isNull}); bila terisi, dituntut sama persis. Artinya komentar semester
	 * pendek dan komentar semester reguler tidak pernah saling bocor ke layar yang lain.</p>
	 *
	 * @return penanda semester pendek, atau {@code null} untuk KRS semester reguler
	 */
	@Column(name = "semester_pendek")
	public Integer getSemesterPendek() {
		return semesterPendek;
	}

	/**
	 * Menyetel penanda semester pendek KRS yang dikomentari.
	 *
	 * @param semesterPendek penanda semester pendek; {@code null} untuk KRS semester reguler
	 */
	public void setSemesterPendek(Integer semesterPendek) {
		this.semesterPendek = semesterPendek;
	}

	/**
	 * Mengembalikan tahapan pengambilan KRS yang dikomentari.
	 *
	 * <p>Tanpa {@code @Column} eksplisit, sehingga terpetakan ke kolom bawaan {@code tahapan}.
	 * Dalam {@code Common.loadKomentarData}, nilai {@code null} atau {@code 0} berarti "tanpa
	 * tahapan": penyaring beralih memakai {@link #getSemester()}. Bila terisi, penyaringnya
	 * <b>longgar</b> — {@code tahapan = n <b>OR</b> tahapan IS NULL} — sehingga komentar lama yang
	 * belum bertahapan tetap ikut tampil di semua tahapan.</p>
	 *
	 * @return nomor tahapan, atau {@code null} bila komentar tidak terikat tahapan
	 */
	public Integer getTahapan() {
		return tahapan;
	}

	/**
	 * Menyetel tahapan pengambilan KRS yang dikomentari.
	 *
	 * @param tahapan nomor tahapan; {@code null}/{@code 0} berarti tanpa tahapan
	 */
	public void setTahapan(Integer tahapan) {
		this.tahapan = tahapan;
	}

}
