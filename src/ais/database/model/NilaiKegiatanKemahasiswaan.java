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
 * Entitas Hibernate untuk tabel {@code public.nilai_kegiatan_kemahasiswaan}: satu baris
 * <b>rubrik angka kredit</b> kegiatan kemahasiswaan. Setiap baris memasangkan sebuah kombinasi
 * <i>rincian kegiatan</i> &times; <i>skala/tingkat</i> &times; <i>jabatan/peran</i> dengan sebuah
 * bobot angka ({@link #getNilai()}).
 *
 * <p>Jangan tertukar dengan dua entity bernama mirip &mdash; ketiganya berada pada lapis yang
 * berbeda:</p>
 *
 * <ul>
 *   <li>{@link ais.database.model.KegiatanKemahasiswaan} &mdash; <b>peristiwanya</b>: satu lomba,
 *       kepanitiaan, atau kepengurusan organisasi yang benar-benar diselenggarakan;</li>
 *   <li>{@link ais.database.model.KegiatanKemahasiswaanPunyaMahasiswa} &mdash; <b>keikutsertaan</b>:
 *       satu mahasiswa pada satu kegiatan, lengkap dengan jabatan dan skala yang diakui baginya;</li>
 *   <li>kelas ini &mdash; <b>tarifnya</b>: master data murni, tidak menunjuk mahasiswa maupun
 *       kegiatan tertentu, hanya menetapkan "kombinasi seperti ini bernilai sekian poin".</li>
 * </ul>
 *
 * <h2>Kunci gabungan tiga sisi</h2>
 *
 * <p>Identitas logis sebuah baris rubrik adalah tripel kunci asing berikut:</p>
 *
 * <ul>
 *   <li>{@link #getDetailKelompokKegiatanKemahasiswaan()} &mdash; rincian/jenis kegiatan
 *       (mis. "Lomba Karya Tulis Ilmiah") di bawah suatu
 *       {@link ais.database.model.KelompokKegiatanKemahasiswaan};</li>
 *   <li>{@link #getSkalaKegiatanKemahasiswaan()} &mdash; tingkat penyelenggaraan (prodi,
 *       universitas, nasional, internasional);</li>
 *   <li>{@link #getJabatanKegiatanKemahasiswaan()} &mdash; peran peserta (ketua, anggota, panitia,
 *       peserta). Sisi ini <b>boleh {@code null}</b>: rincian kegiatan yang tidak membedakan peran
 *       hanya punya satu baris rubrik per skala, dengan jabatan kosong.</li>
 * </ul>
 *
 * <p>Tripel itu direkam ulang sebagai string di {@link #getKodeUnik()} dengan kolom
 * {@code unique = true}, sehingga basis data sendiri yang menjaga agar satu kombinasi tidak
 * dientri dua kali.</p>
 *
 * <h2>Dua mekanisme pencarian yang berjalan berdampingan</h2>
 *
 * <p>Nilai rubrik dicari lewat <b>dua jalur berbeda</b>, dan keduanya tidak memakai kunci yang
 * sama &mdash; ini penting saat menelusuri selisih angka:</p>
 *
 * <ol>
 *   <li><b>Jalur UI/Excel</b> ({@code ais.action.master.NilaiKegiatanKemahasiswaanAction}) mencari
 *       dengan {@code Restrictions.eq("kodeUnik", ...)}, yaitu pencocokan <i>string</i> persis atas
 *       tripel di atas. Jabatan kosong menghasilkan sufiks {@code "-"} kosong di ujung kode, jadi
 *       baris "tanpa jabatan" tetap terbedakan tegas dari baris ber-jabatan.</li>
 *   <li><b>Jalur perhitungan</b> ({@code ais.common.Common#hitungAngkaKredit(Mahasiswa)}) sama
 *       sekali tidak menyentuh {@code kodeUnik}; ia melakukan {@code LEFT JOIN} SQL native langsung
 *       atas ketiga kolom kunci asing, lalu {@code sum(h.nilai)} untuk seluruh kegiatan mahasiswa
 *       yang berstatus {@code 'Disetujui'}. Hasilnya dipakai sebagai syarat kelulusan non-akademik.</li>
 * </ol>
 *
 * <p><b>Selisih perilaku yang perlu diketahui.</b> Predikat jabatan pada SQL jalur kedua berbunyi
 * {@code case when a.jabatan is not null then h.jabatan = a.jabatan else a.jabatan is null end}.
 * Cabang {@code else} menguji kolom sisi <i>keikutsertaan</i> ({@code a}) lagi, bukan sisi
 * <i>rubrik</i> ({@code h}); pada cabang itu hasilnya selalu benar, sehingga batasan jabatan
 * praktis hilang. Akibatnya, untuk peserta yang jabatannya kosong, join mencocokkan <b>semua</b>
 * varian jabatan pada pasangan rincian+skala yang sama dan {@code sum()} menjumlahkan
 * seluruhnya, bukan hanya baris rubrik tanpa jabatan. Jalur {@code kodeUnik} tidak punya masalah
 * ini. Perilaku tersebut didokumentasikan di sini apa adanya sebagai temuan; perbaikannya bukan
 * di kelas ini.
 *
 * <h2>Getter yang menulis balik</h2>
 *
 * <p>Pemetaan kelas ini memakai <i>property access</i> (anotasi JPA menempel pada getter, lihat
 * {@link #getId()}), sehingga Hibernate memanggil getter saat memuat, saat memeriksa perubahan
 * (dirty check), dan saat flush. {@link #getKodeUnik()} memanfaatkan hal itu: getter tersebut
 * <b>menghitung ulang dan menimpa</b> field {@code kodeUnik} dari ketiga relasi setiap kali
 * dipanggil. Konsekuensinya kode unik terpelihara otomatis tanpa ada satu pun pemanggil
 * {@link #setKodeUnik(String)} di jalur normal &mdash; tetapi juga berarti nilai yang disetel
 * manual akan hilang diam-diam pada penyimpanan berikutnya. Rincian lengkap ada di Javadoc
 * getter tersebut.
 *
 * <p>Sebagai pembanding, {@link #getNilai()} hanya <i>mengembalikan</i> {@code 0.0} untuk kolom
 * kosong dan <b>tidak</b> menuliskannya ke field, sehingga kolom yang belum diisi tetap
 * {@code NULL} di basis data. Tidak ada getter di kelas ini yang menutup sesi Hibernate,
 * memuat data tambahan, maupun memanggil {@link GeneralValueObject#check(Object)} &mdash; ketiga
 * getter relasi murni mengembalikan isi field apa adanya.
 *
 * <h2>Layar pengelola dan efek sampingnya</h2>
 *
 * <p>Baris rubrik dikelola lewat {@code /pages/master/nilai_kegiatan_kemahasiswaan.zul}
 * ({@code NilaiKegiatanKemahasiswaanAction}). Layar itu tidak punya menu sendiri: ia dimuat
 * sebagai salah satu tab di dalam {@code kelompok_kegiatan_kemahasiswaan.zul}, sehingga hak
 * aksesnya menempel pada menu Kelompok Kegiatan Kemahasiswaan induk. Tampilannya berupa matriks
 * &mdash; baris = jabatan, kolom = skala &mdash; dengan satu kotak angka per sel, plus tombol
 * unduh/unggah berkas Excel untuk penyuntingan massal.</p>
 *
 * <p>Beberapa perilaku layar tersebut berdampak langsung pada isi tabel ini dan sebaiknya
 * diketahui sebelum menafsirkan datanya:</p>
 *
 * <ul>
 *   <li><b>Membuka layar menciptakan baris.</b> Saat merender sel, {@code tampilRow()} mencari
 *       baris rubrik menurut {@code kodeUnik}; bila tidak ada, ia langsung membuat instance baru
 *       dan menyimpannya lewat {@code Common.refreshSaveOrUpdate(session, ...)} dengan
 *       {@link #getNilai() nilai} masih kosong. Jadi sekadar <i>membaca</i> layar sudah menulis
 *       ke basis data, dan jumlah baris tabel ini tumbuh mengikuti kombinasi yang pernah
 *       ditampilkan, bukan yang pernah diisi.</li>
 *   <li><b>Efek lanjutan pada unduhan Excel.</b> Unduhan menulis sel kosong bila baris rubrik
 *       belum ada, dan angka terformat bila ada. Karena butir sebelumnya, sekali layar dibuka,
 *       sel-sel yang tadinya kosong akan muncul sebagai {@code 0} pada unduhan berikutnya.</li>
 *   <li><b>Unggahan tidak bisa mengosongkan nilai.</b> Proses unggah hanya menyimpan sel yang
 *       lolos syarat {@code nilai != null && nilai > 0.1}. Mengisi {@code 0} di berkas Excel tidak
 *       menghapus atau menolkan nilai lama &mdash; barisnya sekadar dilewati &mdash; dan bobot sah
 *       yang kebetulan berada di rentang {@code 0 < nilai <= 0.1} ikut terbuang tanpa peringatan.
 *       Penurunan bobot ke nol hanya bisa dilakukan lewat kotak angka di layar.</li>
 * </ul>
 *
 * <h2>Catatan pemetaan dan audit</h2>
 *
 * <p>Kelas induk {@link ais.database.model.GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass}, melainkan POJO abstrak biasa, sehingga Hibernate tidak memetakan
 * properti apa pun miliknya. Karena itu {@link #id}, {@link #oleh}, {@link #olehId}, dan
 * {@link #tanggal_dirubah} <b>harus</b> dideklarasikan ulang di setiap entity turunan; pengulangan
 * itu keharusan teknis, bukan duplikasi yang perlu dirapikan.</p>
 *
 * <p>Kelas ditandai {@link Audited}, sehingga Hibernate Envers menyimpan riwayat lengkap setiap
 * revisi baris rubrik &mdash; berguna untuk menelusuri kapan sebuah bobot diubah dan oleh siapa,
 * mengingat perubahan bobot berlaku surut bagi seluruh kegiatan yang sudah disetujui.
 * {@code dynamicInsert}/{@code dynamicUpdate} membuat Hibernate hanya menyertakan kolom yang
 * benar-benar terisi/berubah pada pernyataan SQL-nya.</p>
 *
 * <p>Komentar bawaan hasil pembangkitan pada berkas ini semula berbunyi "Bank generated by
 * hbm2java"; kata "Bank" merupakan sisa salin-tempel dari entity lain dan tidak ada kaitannya
 * dengan tabel ini. Hal serupa berlaku untuk {@link #serialVersionUID}, yang nilainya identik
 * dengan milik {@link ais.database.model.JabatanKegiatanKemahasiswaan} dan
 * {@link ais.database.model.SkalaKegiatanKemahasiswaan} &mdash; tidak berbahaya, karena
 * {@code serialVersionUID} berlaku per kelas.</p>
 *
 * @see ais.database.model.KegiatanKemahasiswaan
 * @see ais.database.model.KegiatanKemahasiswaanPunyaMahasiswa
 * @see ais.database.model.DetailKelompokKegiatanKemahasiswaan
 * @see ais.database.model.JabatanKegiatanKemahasiswaan
 * @see ais.database.model.SkalaKegiatanKemahasiswaan
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "nilai_kegiatan_kemahasiswaan")

public class NilaiKegiatanKemahasiswaan extends GeneralValueObject {

	/**
	 * Versi serialisasi Java.
	 *
	 * <p>Nilainya kebetulan sama persis dengan milik
	 * {@link ais.database.model.JabatanKegiatanKemahasiswaan} dan
	 * {@link ais.database.model.SkalaKegiatanKemahasiswaan} &mdash; sisa salin-tempel berkas hasil
	 * {@code hbm2java}. Tidak berdampak apa pun karena identitas serialisasi dievaluasi per kelas.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama baris rubrik (kolom {@code id}, {@code IDENTITY}). Lihat {@link #getId()}. */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris ini. Lihat {@link #getOleh()}. */
	private String oleh;

	/** Identitas (username/ID) pengguna terakhir yang mengubah baris ini. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna terakhir yang mengubah baris rubrik ini.
	 *
	 * @return ID pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan identitas pengguna pengubah terakhir, <b>mengabaikan masukan kosong</b>.
	 *
	 * <p>Bila {@code olehId} bernilai {@code null} atau hanya berisi spasi, method langsung
	 * kembali tanpa mengubah apa pun sehingga jejak audit yang sudah ada tidak terhapus. Pola
	 * "setter tolak-kosong" ini seragam di seluruh entity AIS.</p>
	 *
	 * <p><b>Dipanggil oleh:</b> {@code ais.database.hibernate.AuditTimestampInterceptor} lewat
	 * {@link #onUpdate()}, dan oleh Hibernate saat memuat baris dari basis data.</p>
	 *
	 * @param olehId ID pengguna pengubah; masukan {@code null}/kosong diabaikan diam-diam
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna pengubah terakhir, <b>mengabaikan masukan kosong</b>.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: masukan {@code null} atau berisi spasi saja
	 * diabaikan agar nilai audit sebelumnya tetap utuh.</p>
	 *
	 * @param oleh nama pengguna pengubah; masukan {@code null}/kosong diabaikan diam-diam
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris rubrik ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mencatat jejak audit tepat sebelum {@code UPDATE} dikirim.
	 *
	 * <p>Mendelegasikan ke {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}
	 * yang mengisi {@link #setOleh(String)}, {@link #setOlehId(String)} dan
	 * {@link #setTanggal_dirubah(Date)} dari konteks pengguna yang sedang aktif.</p>
	 *
	 * <p><b>Dipanggil oleh:</b> provider JPA/Hibernate saja &mdash; jangan dipanggil manual.
	 * Perhatikan bahwa hanya {@code @PreUpdate} yang dipasang; pada {@code INSERT} pertama nilai
	 * audit bergantung pada apa yang diisi pemanggil (atau interceptor global), bukan pada method
	 * ini. Untuk kelas ini hal tersebut cukup terasa, karena baris rubrik sering lahir otomatis
	 * dari perenderan layar (lihat Javadoc kelas) tanpa ada pengguna yang secara sadar menekan
	 * tombol simpan. Perubahan lengkap per revisi tetap terekam Envers karena kelas ini
	 * {@code @Audited}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}
	/**
	 * Waktu perubahan terakhir (kolom {@code tanggal_dirubah}).
	 *
	 * <p>Diinisialisasi ke waktu server saat instance dibuat lewat
	 * {@code ais.ui.util.WaktuUtil.getDate()}; untuk entity yang dimuat dari basis data nilai ini
	 * langsung ditimpa Hibernate dengan isi kolomnya. Diperbarui {@link #onUpdate()} pada setiap
	 * {@code UPDATE}.</p>
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan waktu perubahan terakhir.
	 *
	 * <p>Umumnya dipanggil {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}, bukan oleh
	 * kode aplikasi. Berbeda dari {@link #setOleh(String)}, setter ini menerima {@code null} apa
	 * adanya.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (kolom {@code tanggal_dirubah}, presisi
	 * {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; untuk instance baru bernilai waktu pembuatan objek,
	 *         bukan {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat baris rubrik: {@code "<id>-<rincian kegiatan>"}.
	 *
	 * <p>Bagian kedua adalah hasil {@code toString()} milik
	 * {@link ais.database.model.DetailKelompokKegiatanKemahasiswaan}, yang sendirinya berbentuk
	 * {@code "<id>-<nama>"} &mdash; sehingga keluaran akhir menyerupai {@code "17-42-Lomba Karya
	 * Tulis Ilmiah"}. Field dibaca <b>langsung</b>, bukan lewat getter, jadi tidak ada efek samping
	 * penulisan balik seperti pada {@link #getKodeUnik()}.</p>
	 *
	 * <p><b>Perhatian:</b> skala dan jabatan tidak ikut ditampilkan, padahal keduanya bagian dari
	 * identitas logis baris. Dua baris rubrik yang berbeda bobotnya bisa menghasilkan
	 * {@code toString()} yang mirip bila hanya berbeda pada skala/jabatan; jangan pakai keluaran
	 * ini untuk membedakan baris. Bila relasi rincian kegiatan berupa proxy lazy yang belum
	 * terinisialisasi dan sesi sudah tertutup, pemanggilan ini bisa melempar
	 * {@code LazyInitializationException}.</p>
	 *
	 * @return teks gabungan ID baris dan rincian kegiatannya
	 */
	public String toString() {
		return id + "-" + detailKelompokKegiatanKemahasiswaan;
	}

	/**
	 * Rincian/jenis kegiatan yang dinilai. Sisi pertama kunci gabungan.
	 * Lihat {@link #getDetailKelompokKegiatanKemahasiswaan()}.
	 */
	private DetailKelompokKegiatanKemahasiswaan detailKelompokKegiatanKemahasiswaan;

	/**
	 * Jabatan/peran yang dinilai; boleh {@code null}. Sisi ketiga kunci gabungan.
	 * Lihat {@link #getJabatanKegiatanKemahasiswaan()}.
	 */
	private JabatanKegiatanKemahasiswaan jabatanKegiatanKemahasiswaan;

	/**
	 * Skala/tingkat kegiatan yang dinilai. Sisi kedua kunci gabungan.
	 * Lihat {@link #getSkalaKegiatanKemahasiswaan()}.
	 */
	private SkalaKegiatanKemahasiswaan skalaKegiatanKemahasiswaan;

	/** Bobot angka kredit untuk kombinasi ini; boleh {@code null}. Lihat {@link #getNilai()}. */
	private Double nilai;

	/**
	 * Bentuk string kunci gabungan, dihitung ulang otomatis oleh {@link #getKodeUnik()}.
	 * Kolomnya berkendala {@code unique}.
	 */
	private String kodeUnik;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Juga dipakai langsung {@code NilaiKegiatanKemahasiswaanAction} saat menyiapkan baris
	 * rubrik baru, baik dari perenderan matriks di layar maupun dari proses unggah Excel. Semua
	 * properti dibiarkan kosong; ketiga relasi kunci wajib disetel sebelum baris disimpan, karena
	 * {@link #getKodeUnik()} tidak dapat menyusun kode gabungan tanpa rincian kegiatan dan
	 * skala.</p>
	 */
	public NilaiKegiatanKemahasiswaan() {
	}

	/**
	 * Mengembalikan kunci utama baris rubrik.
	 *
	 * <p>Kolom {@code id} dibangkitkan basis data ({@code IDENTITY}) dan ditandai
	 * {@code insertable = false}, sehingga nilainya baru terisi setelah baris benar-benar
	 * di-{@code INSERT}. Anotasi {@link Id} yang menempel pada getter inilah yang menetapkan
	 * seluruh kelas memakai <i>property access</i> &mdash; dasar dari perilaku tulis-balik pada
	 * {@link #getKodeUnik()}.</p>
	 *
	 * @return kunci utama baris; {@code null} untuk instance yang belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama baris rubrik.
	 *
	 * <p>Umumnya hanya dipanggil Hibernate; kode aplikasi tidak perlu menyetel ID sendiri karena
	 * kolomnya dibangkitkan basis data.</p>
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan rincian/jenis kegiatan yang dinilai baris rubrik ini (kolom
	 * {@code detail_kelompok_kegiatan_kemahasiswaan}).
	 *
	 * <p>Sisi pertama kunci gabungan, sekaligus penentu kelompok kegiatan induk (rincian menunjuk
	 * ke {@link ais.database.model.KelompokKegiatanKemahasiswaan}). Nilainya juga menentukan
	 * daftar jabatan dan skala yang boleh dipasangkan, karena
	 * {@link ais.database.model.DetailKelompokKegiatanKemahasiswaan} membawa relasi many-to-many
	 * ke keduanya &mdash; layar pengelola menyusun matriks selnya persis dari kedua koleksi
	 * tersebut.</p>
	 *
	 * <p>Getter ini murni: mengembalikan isi field apa adanya tanpa memuat data tambahan, tanpa
	 * menulis balik, dan tanpa menyentuh sesi Hibernate. Relasi dipetakan {@code ManyToOne} dengan
	 * {@code FetchMode.SELECT}, jadi entity terkait dimuat lewat {@code SELECT} terpisah.</p>
	 *
	 * @return rincian kegiatan yang dinilai; {@code null} bila belum disetel
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "detail_kelompok_kegiatan_kemahasiswaan")
	public DetailKelompokKegiatanKemahasiswaan getDetailKelompokKegiatanKemahasiswaan() {
		return detailKelompokKegiatanKemahasiswaan;
	}

	/**
	 * Menetapkan rincian/jenis kegiatan yang dinilai baris rubrik ini.
	 *
	 * <p>Menyetel properti ini mengubah kunci gabungan, sehingga {@link #getKodeUnik()} akan
	 * menghitung ulang kode dan &mdash; karena {@code kodeUnik} berkendala {@code unique} &mdash;
	 * penyimpanan bisa gagal bila kombinasi barunya sudah ditempati baris lain.</p>
	 *
	 * <p><b>Dipanggil oleh:</b> {@code NilaiKegiatanKemahasiswaanAction} saat menyiapkan baris
	 * rubrik baru dari perenderan matriks maupun dari unggahan Excel.</p>
	 *
	 * @param detailKelompokKegiatanKemahasiswaan rincian kegiatan yang dinilai
	 */
	public void setDetailKelompokKegiatanKemahasiswaan(
			DetailKelompokKegiatanKemahasiswaan detailKelompokKegiatanKemahasiswaan) {
		this.detailKelompokKegiatanKemahasiswaan = detailKelompokKegiatanKemahasiswaan;
	}

	/**
	 * Mengembalikan jabatan/peran yang dinilai baris rubrik ini (kolom
	 * {@code jabatan_kegiatan_kemahasiswaan}).
	 *
	 * <p>Sisi ketiga kunci gabungan, dan satu-satunya sisi yang <b>boleh kosong</b>: rincian
	 * kegiatan yang tidak membedakan peran cukup punya satu baris rubrik per skala dengan jabatan
	 * {@code null}. Perlakuan khusus terhadap kekosongan itulah yang membedakan kedua jalur
	 * pencarian yang diuraikan di Javadoc kelas &mdash; {@link #getKodeUnik()} menandainya dengan
	 * segmen terakhir yang kosong, sedangkan SQL penjumlah angka kredit justru melonggarkan
	 * batasan jabatan sepenuhnya.</p>
	 *
	 * <p>Getter ini murni: tanpa pemuatan tambahan, tanpa tulis balik, tanpa efek pada sesi
	 * Hibernate.</p>
	 *
	 * @return jabatan yang dinilai, atau {@code null} bila baris rubrik ini berlaku tanpa
	 *         membedakan peran
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jabatan_kegiatan_kemahasiswaan")
	public JabatanKegiatanKemahasiswaan getJabatanKegiatanKemahasiswaan() {
		return jabatanKegiatanKemahasiswaan;
	}

	/**
	 * Menetapkan jabatan/peran yang dinilai baris rubrik ini.
	 *
	 * <p>Menerima {@code null} secara sah &mdash; itulah cara menandai baris rubrik yang berlaku
	 * untuk seluruh peran. Sama seperti setter relasi lainnya, perubahan di sini mengubah kunci
	 * gabungan yang dihitung {@link #getKodeUnik()}.</p>
	 *
	 * <p><b>Dipanggil oleh:</b> {@code NilaiKegiatanKemahasiswaanAction}; pada jalur unggah Excel
	 * argumennya berasal dari kolom "Jabatan/Status" berkas dan memang bisa {@code null} bila sel
	 * itu kosong.</p>
	 *
	 * @param jabatanKegiatanKemahasiswaan jabatan yang dinilai; {@code null} berarti berlaku untuk
	 *                                     semua peran
	 */
	public void setJabatanKegiatanKemahasiswaan(JabatanKegiatanKemahasiswaan jabatanKegiatanKemahasiswaan) {
		this.jabatanKegiatanKemahasiswaan = jabatanKegiatanKemahasiswaan;
	}

	/**
	 * Mengembalikan skala/tingkat kegiatan yang dinilai baris rubrik ini (kolom
	 * {@code skala_kegiatan_kemahasiswaan}).
	 *
	 * <p>Sisi kedua kunci gabungan; menentukan kolom mana pada matriks layar pengelola (dan pada
	 * berkas Excel) yang diisi baris ini. Wajib terisi agar {@link #getKodeUnik()} dapat menyusun
	 * kode gabungan.</p>
	 *
	 * <p>Getter ini murni: tanpa pemuatan tambahan, tanpa tulis balik, tanpa efek pada sesi
	 * Hibernate.</p>
	 *
	 * @return skala kegiatan yang dinilai; {@code null} bila belum disetel
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "skala_kegiatan_kemahasiswaan")
	public SkalaKegiatanKemahasiswaan getSkalaKegiatanKemahasiswaan() {
		return skalaKegiatanKemahasiswaan;
	}

	/**
	 * Menetapkan skala/tingkat kegiatan yang dinilai baris rubrik ini.
	 *
	 * <p>Mengubah kunci gabungan yang dihitung {@link #getKodeUnik()}.</p>
	 *
	 * <p><b>Dipanggil oleh:</b> {@code NilaiKegiatanKemahasiswaanAction}; pada jalur unggah Excel
	 * argumennya dipetakan dari judul kolom berkas.</p>
	 *
	 * @param skalaKegiatanKemahasiswaan skala kegiatan yang dinilai
	 */
	public void setSkalaKegiatanKemahasiswaan(SkalaKegiatanKemahasiswaan skalaKegiatanKemahasiswaan) {
		this.skalaKegiatanKemahasiswaan = skalaKegiatanKemahasiswaan;
	}

	/**
	 * Mengembalikan bobot angka kredit untuk kombinasi rincian &times; skala &times; jabatan ini,
	 * dengan {@code null} dianggap {@code 0.0}.
	 *
	 * <p><b>Bukan getter destruktif.</b> Nilai pengganti hanya dikembalikan, <b>tidak</b> dituliskan
	 * ke field, sehingga kolom yang belum pernah diisi tetap {@code NULL} di basis data. Perbedaan
	 * ini bermakna: penjumlah angka kredit memakai {@code sum()} SQL yang mengabaikan {@code NULL},
	 * jadi baris rubrik kosong tidak menyumbang apa pun &mdash; hasilnya kebetulan sama dengan
	 * menganggapnya nol. Namun di layar dan di berkas Excel, baris yang belum diisi tetap tampil
	 * sebagai {@code 0}, sehingga "belum ditetapkan" dan "sengaja bernilai nol" tidak dapat
	 * dibedakan secara visual.</p>
	 *
	 * <p><b>Dipanggil oleh:</b> kotak angka pada matriks layar pengelola, penulisan sel unduhan
	 * Excel, dan Hibernate saat memeriksa perubahan. Perhitungan angka kredit mahasiswa sendiri
	 * ({@code Common#hitungAngkaKredit(Mahasiswa)}) tidak lewat getter ini &mdash; ia membaca kolom
	 * {@code nilai} langsung lewat SQL native.</p>
	 *
	 * @return bobot angka kredit; {@code 0.0} bila kolomnya masih kosong (tidak pernah
	 *         {@code null})
	 */
	public Double getNilai() {
		return nilai == null ? 0.0 : nilai;
	}

	/**
	 * Menetapkan bobot angka kredit untuk kombinasi ini.
	 *
	 * <p>Menerima {@code null} apa adanya &mdash; berbeda dari {@link #getNilai()} yang menyulih
	 * {@code null} menjadi {@code 0.0} hanya saat membaca.</p>
	 *
	 * <p><b>Dipanggil oleh:</b> dua jalur pada {@code NilaiKegiatanKemahasiswaanAction}, yaitu
	 * penyunting {@code onChange} pada kotak angka di matriks layar (langsung diikuti
	 * {@code Common.refreshUpdate}), dan proses unggah Excel. Jalur unggah menyaring nilainya
	 * dengan syarat {@code nilai > 0.1}, sehingga tidak bisa dipakai untuk menolkan bobot.</p>
	 *
	 * <p><b>Efek samping penyimpanan:</b> bobot berlaku <i>surut</i>. Perhitungan angka kredit
	 * dijalankan atas rubrik terkini, bukan atas bobot yang berlaku saat kegiatan disetujui, jadi
	 * mengubah satu angka di sini langsung menggeser total angka kredit seluruh mahasiswa yang
	 * pernah mengikuti kombinasi kegiatan tersebut &mdash; termasuk yang sudah memenuhi syarat
	 * kelulusan non-akademik.</p>
	 *
	 * @param nilai bobot angka kredit; boleh {@code null}
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Mengembalikan kunci gabungan baris rubrik dalam bentuk string, <b>sambil menghitung ulang
	 * dan menimpa</b> field {@code kodeUnik}.
	 *
	 * <p><b>Format.</b> {@code "<idRincian>-<idSkala>-<idJabatan>"}. Bila jabatan kosong, segmen
	 * terakhir dibiarkan kosong sehingga kode berakhir dengan tanda hubung, mis. {@code "42-3-"}
	 * &mdash; bentuk itu sengaja tetap berbeda dari kode ber-jabatan, sehingga baris "berlaku untuk
	 * semua peran" tidak pernah tertukar dengan baris peran tertentu.</p>
	 *
	 * <p><b>Getter yang menulis balik.</b> Perhitungan ulang hanya dijalankan bila rincian kegiatan
	 * <i>dan</i> skala sama-sama terisi; hasilnya ditugaskan ke field sebelum dikembalikan. Karena
	 * kelas ini memakai <i>property access</i>, Hibernate memanggil getter ini saat dirty check dan
	 * flush, sehingga kode gabungan terpelihara otomatis dan ikut tersimpan ke kolom tanpa ada
	 * pemanggil {@link #setKodeUnik(String)} di jalur normal mana pun. Konsekuensi yang perlu
	 * disadari:</p>
	 *
	 * <ul>
	 *   <li>nilai yang disetel manual lewat {@link #setKodeUnik(String)} akan ditimpa diam-diam
	 *       pada pembacaan/penyimpanan berikutnya, selama kedua relasi kunci terisi;</li>
	 *   <li>sebaliknya, bila rincian kegiatan atau skala <i>belum</i> terisi, method mengembalikan
	 *       isi field apa adanya &mdash; yang pada baris hasil suntingan bisa berupa kode
	 *       <b>usang</b> dari kombinasi lama, bukan {@code null};</li>
	 *   <li>relasi dibaca sebagai <i>field</i>, bukan lewat getter, sehingga proxy lazy yang belum
	 *       terinisialisasi tetap dianggap ada dan pemanggilan {@code getId()}-nya dapat melempar
	 *       {@code LazyInitializationException} bila sesi sudah tertutup;</li>
	 *   <li>kolom hasilnya berkendala {@code unique = true} tanpa {@code name} eksplisit (nama
	 *       kolom mengikuti nama properti sesuai strategi penamaan bawaan Hibernate), jadi entri
	 *       ganda untuk kombinasi yang sama gagal di tingkat basis data, bukan lewat validasi
	 *       aplikasi.</li>
	 * </ul>
	 *
	 * <p><b>Dipanggil oleh:</b> Hibernate saat memuat/menyimpan baris, dan secara tidak langsung
	 * oleh seluruh pencarian rubrik di {@code NilaiKegiatanKemahasiswaanAction} &mdash; layar,
	 * unduhan, dan unggahan Excel semuanya mencocokkan {@code Restrictions.eq("kodeUnik", ...)}
	 * dengan string yang mereka susun sendiri memakai formula yang sama persis. Perhatikan bahwa
	 * formula itu <i>disalin</i> di tiga tempat pada Action tersebut, bukan memanggil getter ini,
	 * sehingga perubahan format di sini harus diikutkan ke sana. Penjumlah angka kredit
	 * ({@code Common#hitungAngkaKredit(Mahasiswa)}) tidak memakai kode ini sama sekali.</p>
	 *
	 * @return kode gabungan terkini bila rincian kegiatan dan skala terisi; selain itu isi field
	 *         apa adanya, yang bisa {@code null} atau usang
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		if (detailKelompokKegiatanKemahasiswaan != null && skalaKegiatanKemahasiswaan != null) {
			kodeUnik = detailKelompokKegiatanKemahasiswaan.getId() + "-" + skalaKegiatanKemahasiswaan.getId() + "-"
					+ (jabatanKegiatanKemahasiswaan == null ? "" : jabatanKegiatanKemahasiswaan.getId());
		}
		return kodeUnik;
	}

	/**
	 * Menetapkan kunci gabungan baris rubrik secara manual.
	 *
	 * <p><b>Praktis tidak berguna dipanggil dari kode aplikasi.</b> Tidak ada pemanggil di seluruh
	 * basis kode selain Hibernate saat memuat baris, dan nilai apa pun yang disetel di sini akan
	 * ditimpa {@link #getKodeUnik()} begitu rincian kegiatan dan skala terisi. Untuk mengubah kode
	 * gabungan, ubah ketiga relasi kuncinya, bukan properti ini.</p>
	 *
	 * @param kodeUnik kode gabungan; akan ditimpa hasil perhitungan ulang pada pembacaan berikutnya
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

}
