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

/**
 * Baris penghubung (<i>link row</i>) antara satu {@link GrupPenilaian} dan satu
 * {@link GrupKategoriItemPenilaianSiswa} pada rantai konfigurasi penilaian siswa modul Sekolah.
 * Dipetakan ke tabel <b>{@code sekolah.detail_grup_penilaian_data}</b>, di-audit Envers
 * ({@code @Audited}) dan memakai {@code dynamicInsert}/{@code dynamicUpdate}.
 *
 * <h2>Posisi dalam rantai penilaian (terverifikasi dari kode)</h2>
 *
 * <p>Rantai konfigurasi penilaian siswa tersusun dari lima tabel master dan tiga tabel
 * penghubung. Arah relasi di bawah diverifikasi langsung dari nama kolom {@code @JoinColumn}
 * masing-masing entity, bukan diasumsikan:</p>
 *
 * <pre>
 *   JenisPenilaian                        (master; dipilih per Matapelajaran/KurikulumSekolah)
 *        &darr;  DetailJenisPenilaian      (kolom: jenis_penilaian_id, grup_penilaian_siswa_id)
 *   GrupPenilaian                         (master; pemilik formula, jenisNilaiHuruf, khususTingkat/Semester)
 *        &darr;  <b>DetailGrupPenilaian &mdash; KELAS INI</b>
 *                                         (kolom: grup_penilaian_id, grup_kategori_item_penilaian_siswa)
 *   GrupKategoriItemPenilaianSiswa        (master; punya kode/nama/formula/khususTingkat)
 *        &darr;  DetailGrupKategoriItemPenilaianSiswa
 *                                         (kolom: grup_kategori_item_penilaian_siswa, kategori_item_penilaian_siswa)
 *   KategoriItemPenilaianSiswa            (master)
 *        &darr;  (FK langsung, bukan tabel penghubung)
 *   JenisItemPenilaianSiswa               (butir nilai terkecil; punya kode yang dipakai formula)
 * </pre>
 *
 * <p>Jadi kelas ini adalah <b>simpul ketiga</b> rantai dan satu-satunya jembatan antara lapisan
 * "grup penilaian" (yang punya formula agregat) dan lapisan "grup kategori" (yang menurunkan
 * daftar butir nilai). Menghapus/mematikan baris di sini memutus seluruh cabang di bawahnya:
 * kategori dan butir nilainya tidak lagi muncul di formulir input nilai, rapor, rekap, maupun API.</p>
 *
 * <h2>Bentuk relasi</h2>
 *
 * <ul>
 *   <li>Kedua relasi {@code @ManyToOne} <b>LAZY</b> dan {@code nullable = false} &mdash; sebuah
 *       baris detail selalu memiliki induk grup penilaian dan target grup kategori.</li>
 *   <li>{@code cascade = {PERSIST, MERGE}} pada kedua sisi: menyimpan baris detail dapat ikut
 *       menyimpan master di ujungnya. Tidak ada {@code REMOVE}, jadi menghapus baris detail tidak
 *       pernah menghapus master.</li>
 *   <li>Tidak ada koleksi balik ({@code @OneToMany}) di {@link GrupPenilaian} maupun di
 *       {@link GrupKategoriItemPenilaianSiswa}. Seluruh navigasi dilakukan lewat
 *       {@code Criteria} eksplisit di sisi pemanggil.</li>
 *   <li>Relasi <b>bukan</b> unik: tidak ada {@code unique constraint} pada pasangan
 *       {@code (grup_penilaian_id, grup_kategori_item_penilaian_siswa)}. Duplikat mungkin ada di
 *       basis data; pembaca menetralkannya dengan {@code Projections.groupProperty(...)}, dan
 *       layar master menetralkannya dengan {@code containsKey} saat memuat peta pilihan.</li>
 *   <li>Entity ini <b>tidak</b> punya kolom tenant ({@code sekolah}/{@code yayasan}) sendiri.
 *       Cakupan tenant sepenuhnya diwarisi dari {@link GrupPenilaian#getSekolah()}.</li>
 * </ul>
 *
 * <h2>Siapa yang menulis baris ini</h2>
 *
 * <p>Tidak ada layar master tersendiri untuk entity ini. Satu-satunya penulis adalah
 * {@code ais.action.master.sekolah.GrupPenilaianAction} lewat daftar checkbox
 * <i>"Pilih Grup Kategori"</i> di jendela Tambah/Ubah Grup Penilaian. Alur simpannya
 * (<b>penting, lihat bagian "bom waktu" di bawah</b>):</p>
 *
 * <ol>
 *   <li>Simpan/ubah {@link GrupPenilaian} lalu {@code flush()}.</li>
 *   <li>Muat <b>SEMUA</b> {@code DetailGrupPenilaian} milik grup itu (tanpa filter apa pun) dan
 *       set {@code aktif = false} satu per satu, masing-masing di-{@code flush()}.</li>
 *   <li>Untuk setiap entri peta {@code selectedGrupKategoriItemPenilaianSiswa} (yakni checkbox
 *       yang tercentang saat itu), set {@code aktif = true}, set induk grup penilaian, simpan.</li>
 * </ol>
 *
 * <p>Konsekuensi yang perlu diketahui: (a) baris <b>tidak pernah dihapus fisik</b> &mdash;
 * {@code aktif} adalah <i>soft delete</i>; (b) setiap penyimpanan satu Grup Penilaian
 * menghasilkan sampai <b>2&times;N revisi Envers</b> pada tabel detail (N = jumlah baris detail
 * grup tersebut) plus penulisan ulang {@code oleh}/{@code tanggal_dirubah}, jadi riwayat audit
 * tabel ini berisik dan tidak bisa dipakai untuk menyimpulkan "kapan pemetaan benar-benar
 * berubah"; (c) karena baris lama dipakai ulang (bukan dibuat baru), {@code id} baris detail
 * stabil lintas penyuntingan &mdash; itu yang membuat FK
 * {@code PertemuanPunyaUjian.detail_grup_penilaian} tetap valid meski pemetaan dimatikan.</p>
 *
 * <h2>Siapa yang membaca baris ini</h2>
 *
 * <p>Delapan titik baca ditemukan, dan <b>semuanya memakai idiom yang sama persis</b>: query
 * {@code Criteria} atas {@code DetailGrupPenilaian}, filter {@code aktif} toleran-NULL
 * ({@code isNull("aktif") OR eq("aktif", true)}), {@code isNotNull} pada FK grup kategori, lalu
 * {@code setProjection(Projections.groupProperty("grupKategoriItemPenilaianSiswa.id"))} sehingga
 * yang dikembalikan adalah daftar {@link GrupKategoriItemPenilaianSiswa}, bukan baris detailnya.
 * Titik-titik tersebut:</p>
 *
 * <ul>
 *   <li>{@code ais.action.master.sekolah.PenilaianSiswaAction} &mdash; formulir input nilai per
 *       jadwal pelajaran.</li>
 *   <li>{@code ais.action.master.sekolah.helper.DetailPenilaianSiswaHelper} dan
 *       {@code DetailPenilaianLesSiswaHelper} &mdash; tab detail nilai (reguler dan les).</li>
 *   <li>{@code ais.action.master.sekolah.helper.PertemuanPunyaUjianSiswaHelper} &mdash; nilai ujian
 *       per pertemuan.</li>
 *   <li>{@code ais.action.master.sekolah.helper.TampilStudiSiswaHelper} &mdash; tampilan hasil studi.</li>
 *   <li>{@code ais.action.master.helper.TugasMandiriHelper} dan {@code TugasKelompokHelper} &mdash;
 *       penilaian tugas.</li>
 *   <li>{@code ais.action.report.format1.sekolah.LaporanRaporSiswa} (dua tempat) dan
 *       {@code LaporanRekapTotalNilai} (tiga tempat) &mdash; cetak rapor dan rekap nilai.</li>
 *   <li>{@code ais.action.servlet.api.NilaiSiswaApi} dan {@code ElearningApiUtil} (dua tempat)
 *       &mdash; API mobile/e-learning.</li>
 * </ul>
 *
 * <p>Satu-satunya pembaca yang memakai <b>object</b> {@code DetailGrupPenilaian} (bukan proyeksi
 * id) adalah {@code ais.action.master.sekolah.util.GrupPenilaianUtil#hitung(...)}, mesin evaluasi
 * formula. Di sana baris detail dipakai dua cara: dari peta pilihan layar (tanpa cek
 * {@code aktif}, karena isi peta memang sudah berupa pilihan pengguna), atau &mdash; bila peta
 * kosong &mdash; dengan menyapu cache global {@code ConstantValues.ambilBerdasarClass(
 * DetailGrupPenilaian.class)} lalu menyaring {@code getAktif()} dan kesamaan
 * {@code getGrupPenilaian().getId()}. Cabang cache itu menyapu <b>seluruh tenant</b>, tetapi tetap
 * aman karena id grup penilaian unik global; yang perlu diingat hanyalah biayanya (iterasi
 * O(seluruh baris detail instalasi) per evaluasi formula).</p>
 *
 * <h2>Bom waktu integritas nilai rapor pada {@code aktif} (varian batch 51 di simpul ini)</h2>
 *
 * <p>Pola yang ditemukan pada {@link KategoriItemPenilaianSiswa} <b>berlaku juga di sini, dengan
 * mekanisme yang lebih langsung dan lebih mudah terpicu</b>. Yang membuatnya berbahaya adalah
 * <b>asimetri</b> antara pembaca runtime dan layar master:</p>
 *
 * <ul>
 *   <li><b>Semua pembaca runtime</b> (delapan titik di atas) hanya menyaring
 *       {@code DetailGrupPenilaian.aktif}. <b>Tidak satu pun</b> dari mereka menyaring
 *       {@code GrupKategoriItemPenilaianSiswa.aktif}. Jadi mematikan sebuah Grup Kategori dari
 *       layar masternya <b>tidak</b> mengeluarkannya dari rapor/rekap/API &mdash; ia tetap
 *       terpakai selama baris detail di sini masih {@code aktif}.</li>
 *   <li><b>Layar master Grup Penilaian</b> justru sebaliknya: daftar checkbox hanya diisi Grup
 *       Kategori yang {@code aktif} true/NULL <i>dan</i> lolos filter tenant, dan peta pilihan
 *       hanya diisi baris detail yang grup kategorinya {@code aktif} true/NULL.</li>
 * </ul>
 *
 * <p>Digabungkan dengan alur simpan "matikan semua &rarr; hidupkan yang tercentang", hasilnya:
 * <b>begitu sebuah Grup Kategori dinonaktifkan (atau pindah/berbeda tenant), penyimpanan
 * berikutnya atas Grup Penilaian terkait &mdash; sekadar memperbaiki salah ketik nama pun sudah
 * cukup &mdash; akan mematikan baris penghubungnya secara permanen dan senyap.</b> Mengaktifkan
 * kembali Grup Kategori tidak memulihkan apa pun, karena baris detail sudah {@code aktif=false}
 * dan tidak akan pernah tercentang lagi kecuali pengguna sadar harus mencentangnya ulang. Efek
 * yang terlihat pengguna: satu kolom/kategori nilai hilang dari rapor tanpa pesan apa pun, dan
 * total nilai berubah karena formula kehilangan sukunya.</p>
 *
 * <p>Varian kedua dari bom waktu yang sama ("kategori hantu lintas sekolah"): daftar checkbox
 * disaring dengan {@code isNull(sekolah) OR eq(sekolah, s)}. Baris detail yang menunjuk Grup
 * Kategori milik <b>sekolah lain</b> (bisa terbentuk lewat unggah massal, migrasi data, atau
 * lewat celah fail-open di paragraf berikut) tidak akan pernah muncul sebagai checkbox, sehingga
 * tetap {@code aktif} dan tetap terpakai runtime &mdash; sampai seseorang menyimpan ulang Grup
 * Penilaian itu, yang langsung mematikannya tanpa jejak. Tidak ada layar mana pun yang bisa
 * menampilkan, apalagi melepaskan, baris semacam itu secara terkendali.</p>
 *
 * <p>Varian ketiga (waktu/timing): peta pilihan dibuat kosong saat jendela dirender dan baru
 * diisi ketika listener {@code ubahJenisPenialain} dijalankan lewat
 * {@code Common.createDefaultTimer(...)}. Bila tombol <i>Simpan</i> sempat ditekan sebelum timer
 * itu berjalan, atau listener gagal, langkah "matikan semua" tetap berjalan sedangkan langkah
 * "hidupkan yang tercentang" tidak menemukan apa-apa &mdash; <b>seluruh pemetaan grup penilaian
 * itu hilang sekaligus</b>.</p>
 *
 * <p>Ketiganya adalah bug integritas data, bukan kerentanan akses. Dicatat di sini untuk
 * perbaikan masa depan; perbaikan yang aman harus menyentuh <b>kedua</b> sisi sekaligus (pembaca
 * runtime ikut menyaring {@code aktif} grup kategori, dan layar master berhenti mematikan baris
 * yang tidak pernah ia tampilkan), karena memperbaiki satu sisi saja akan mengubah isi rapor
 * historis.</p>
 *
 * <h2>Catatan cakupan tenant dan hak akses</h2>
 *
 * <ul>
 *   <li><b>Fail-open cakupan tenant (ringan).</b> Pada listener pengisi daftar checkbox, bila
 *       kombo Sekolah (atau Yayasan) belum terpilih, filter diganti
 *       {@code Restrictions.sqlRestriction("1=1")} &mdash; seluruh Grup Kategori <b>semua
 *       sekolah</b> ikut terdaftar dan bisa dicentang, sehingga terbentuk pemetaan lintas tenant.
 *       Dampaknya terbatas pada metadata konfigurasi penilaian (bukan PII), dan penyimpanan tetap
 *       menolak bila Sekolah/Yayasan kosong, tetapi polanya sama dengan keluarga temuan
 *       fail-open yang sudah tercatat pada audit luas &mdash; memperkuat, bukan temuan baru.</li>
 *   <li><b>Pewarisan hak lewat menu induk.</b> Layar {@code grup_penilaian.zul} <b>tidak</b>
 *       punya entri menu sendiri; ia disisipkan sebagai tab ({@code MyInclude}) di dalam
 *       {@code JenisPenilaianAction} lewat {@code onGrupPenilaian(...)}. Karena
 *       {@code CommonPrivilages.checkPrevilages(...)} menguji {@code Common.getCurrentMenu()},
 *       hak CREATE/UPDATE/DELETE yang dipakai layar ini sebenarnya hak menu <i>Jenis
 *       Penilaian</i>. Ini mekanisme yang sama persis dengan yang tercatat pada
 *       {@link KategoriItemPenilaianSiswa} (tab bersaudara dari induk yang sama), jadi memperkuat
 *       instance yang sudah ada, bukan mekanisme baru.</li>
 *   <li><b>Sisi positif:</b> {@code GrupPenilaianAction} sendiri termasuk layar master yang
 *       digerbangi dengan benar &mdash; tombol Tambah digerbangi CREATE, seluruh checkbox grid
 *       di-{@code setDisabled(!edit)}, tombol Ubah/Hapus lewat
 *       {@code Common.copyEditDeleteButtons(edit, delete, ...)}, dan tombol unggah massal
 *       menuntut CREATE&amp;UPDATE&amp;DELETE sekaligus. Tidak ditemukan tombol mutasi massal
 *       tanpa gerbang, dan {@code doBeforeCompose} memanggil {@code Common.doCheckSecurity()}.
 *       Berkas {@code WEB-INF/new/sekolah/services/grup_penilaian_service.jsp} hanyalah
 *       <i>scaffold</i> hasil generator tanpa akses data, jadi bukan jalur pra-otentikasi.</li>
 *   <li><b>Tidak ada</b> getter yang menulis balik data bisnis secara destruktif di kelas ini.
 *       Penugasan {@code x = check(x)} pada kedua getter relasi hanyalah resolusi proxy lazy
 *       standar (lihat {@link GeneralValueObject#check(Object)}), bukan mutasi data.</li>
 * </ul>
 *
 * <h2>Kolom/anggota yang praktis mati</h2>
 *
 * <ul>
 *   <li>{@link #getKeterangan()} &mdash; tidak pernah diisi oleh layar mana pun dan tidak pernah
 *       dibaca oleh konsumen mana pun. Kolom yatim sejak awal.</li>
 *   <li>{@link #getNomorUrut()} &mdash; selalu mengembalikan {@code 1}; lihat Javadoc method-nya.</li>
 *   <li>Kolom {@code detail_grup_penilaian} pada {@code ais.database.model.PertemuanPunyaUjian}
 *       adalah FK ke tabel ini, tetapi {@code getDetailGrupPenilaian()}/{@code setDetailGrupPenilaian(...)}
 *       tidak pernah dipanggil dari mana pun &mdash; relasi yatim.</li>
 * </ul>
 *
 * <h2>Catatan warisan {@code GeneralValueObject}</h2>
 *
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} &mdash;
 * ia POJO abstrak biasa, sehingga Hibernate tidak memetakan properti apa pun miliknya. Karena itu
 * deklarasi ulang {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} di kelas
 * ini <b>bukan duplikasi keliru melainkan keharusan teknis</b>: tanpa deklarasi ulang, kolom-kolom
 * tersebut tidak akan pernah ada di tabel. Pola ini seragam di seluruh entity AIS.</p>
 *
 * <h2>Pengelompokan method</h2>
 *
 * <ol>
 *   <li><b>Jejak audit ringan</b> (deklarasi ulang dari induk): {@link #getOleh()},
 *       {@link #setOleh(String)}, {@link #getOlehId()}, {@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}, {@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 *   <li><b>Identitas</b>: {@link #getId()}, {@link #setId(Long)}.</li>
 *   <li><b>Relasi rantai penilaian</b>: {@link #getGrupPenilaian()},
 *       {@link #setGrupPenilaian(GrupPenilaian)}, {@link #getGrupKategoriItemPenilaianSiswa()},
 *       {@link #setGrupKategoriItemPenilaianSiswa(GrupKategoriItemPenilaianSiswa)}.</li>
 *   <li><b>Atribut</b>: {@link #getAktif()}, {@link #setAktif(Boolean)}, {@link #getKeterangan()},
 *       {@link #setKeterangan(String)}.</li>
 *   <li><b>Turunan</b>: {@link #getNomorUrut()}.</li>
 * </ol>
 *
 * @see GrupPenilaian
 * @see GrupKategoriItemPenilaianSiswa
 * @see DetailGrupKategoriItemPenilaianSiswa
 * @see KategoriItemPenilaianSiswa
 * @see JenisItemPenilaianSiswa
 * @see DetailJenisPenilaian
 * @see JenisPenilaian
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "detail_grup_penilaian_data", schema = "sekolah")
public class DetailGrupPenilaian extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai tetap; jangan diubah tanpa alasan kuat karena object entity
	 * ikut diserialisasi ke sesi/desktop ZK dan ke cache.
	 */
	private static final long serialVersionUID = -9157912161411433979L;
	/** Kunci utama {@code sekolah.detail_grup_penilaian_data.id}; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang menyimpan baris ini; lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang menyimpan baris ini; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang menyimpan baris ini (kolom {@code olehId}).
	 *
	 * @return id pengguna penyimpan terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna penyimpan terakhir.
	 *
	 * <p><b>Perhatikan:</b> nilai {@code null} atau string kosong/spasi <b>diabaikan diam-diam</b>
	 * (method langsung {@code return} tanpa menyentuh field). Jadi nilai lama tidak pernah bisa
	 * dikosongkan lewat setter ini. Pola ini disengaja agar jejak audit tidak terhapus oleh
	 * pemuatan ulang yang belum mengetahui pengguna aktif.</p>
	 *
	 * @param olehId id pengguna baru; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna penyimpan terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: nilai {@code null}/kosong diabaikan diam-diam
	 * sehingga jejak audit tidak pernah terhapus lewat setter.</p>
	 *
	 * @param oleh nama pengguna baru; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang menyimpan baris ini (kolom {@code oleh}).
	 *
	 * @return nama pengguna penyimpan terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang dijalankan tepat sebelum {@code UPDATE}: meneruskan object ini ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(...)} agar {@code oleh},
	 * {@code olehId}, dan {@code tanggal_dirubah} diperbarui dari sesi pengguna aktif.
	 *
	 * <p>Karena {@code GrupPenilaianAction.onSave(...)} menyimpan ulang <b>seluruh</b> baris detail
	 * milik satu grup penilaian dua kali (mematikan lalu menghidupkan), kait ini ikut terpanggil
	 * berkali-kali per aksi simpan tunggal &mdash; itulah sebab {@code tanggal_dirubah} pada tabel
	 * ini bergerak walau pemetaannya tidak benar-benar berubah.</p>
	 *
	 * <p>Baris ini sengaja ditulis rapat dengan deklarasi field {@code tanggal_dirubah} karena
	 * mengikuti bentuk seragam seluruh entity AIS; jangan dirapikan tanpa menyapu semua entity.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi; biasanya diisi otomatis oleh
	 * {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir (kolom {@code tanggal_dirubah},
	 * {@code TIMESTAMP}). Default saat object dibuat adalah waktu server
	 * ({@code ais.ui.util.WaktuUtil.getDate()}), jadi nilainya tidak pernah {@code null} untuk
	 * object baru.
	 *
	 * @return stempel waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Induk grup penilaian (sisi "kiri" penghubung); lihat {@link #getGrupPenilaian()}. */
	private GrupPenilaian grupPenilaian;
	/** Grup kategori yang ditunjuk (sisi "kanan"); lihat {@link #getGrupKategoriItemPenilaianSiswa()}. */
	private GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa;
	/** Keterangan bebas; kolom yatim, lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Penanda pemetaan hidup/mati (soft delete); lihat {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate/JPA. Object hasil konstruktor ini belum punya
	 * induk maupun target, sehingga <b>belum</b> memenuhi {@code nullable = false} pada kedua FK;
	 * pemanggil wajib mengisi {@link #setGrupPenilaian(GrupPenilaian)} dan
	 * {@link #setGrupKategoriItemPenilaianSiswa(GrupKategoriItemPenilaianSiswa)} sebelum menyimpan.
	 *
	 * <p>Satu-satunya pemakaian di kode aplikasi ada di {@code GrupPenilaianAction}, saat merender
	 * checkbox untuk grup kategori yang belum pernah dipetakan.</p>
	 */
	public DetailGrupPenilaian() {

	}

	/**
	 * Mengembalikan kunci utama baris penghubung ini (kolom {@code id}, IDENTITY, tidak ikut
	 * di-{@code INSERT} karena dibangkitkan basis data).
	 *
	 * @return id baris, atau {@code null} untuk object yang belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Tanpa validasi; normalnya hanya dipanggil Hibernate.
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan keterangan bebas baris penghubung ini (kolom {@code keterangan}).
	 *
	 * <p><b>Kolom yatim:</b> penelusuran seluruh kode tidak menemukan satu pun penulis maupun
	 * pembaca. Layar Grup Penilaian punya kotak "Keterangan", tetapi itu milik
	 * {@link GrupPenilaian#getKeterangan()}, bukan milik baris detail ini. Nilainya akan selalu
	 * {@code null} kecuali diisi langsung lewat basis data.</p>
	 *
	 * @return keterangan, praktis selalu {@code null}
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel keterangan bebas. Tanpa validasi. Tidak dipanggil dari mana pun di kode aplikasi.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan penanda hidup/mati pemetaan ini, dengan <b>default toleran-NULL</b>:
	 * {@code null} dianggap {@code true}.
	 *
	 * <p>Default itu penting dan konsisten dengan sisi SQL: seluruh pembaca memakai
	 * {@code Restrictions.or(isNull("aktif"), eq("aktif", true))}, sehingga baris warisan yang
	 * kolom {@code aktif}-nya masih NULL tetap terpakai. Berbeda dengan beberapa entity master
	 * lain di modul ini, di sini <b>tidak ada</b> divergensi antara getter dan SQL &mdash;
	 * kolomnya juga benar-benar ditulis oleh layar master, bukan hanya dibaca.</p>
	 *
	 * <p><b>Peringatan integritas data:</b> kolom inilah yang dimatikan-lalu-dihidupkan pada setiap
	 * penyimpanan {@link GrupPenilaian}. Baris yang grup kategorinya sedang nonaktif atau berbeda
	 * tenant tidak pernah ikut dihidupkan kembali, sehingga pemetaannya hilang permanen dan
	 * senyap. Rinciannya ada di Javadoc kelas, bagian "Bom waktu integritas nilai rapor".</p>
	 *
	 * @return {@code true} bila pemetaan aktif (termasuk saat kolom masih NULL), {@code false} bila
	 *         pemetaan sudah dimatikan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel penanda hidup/mati pemetaan. Tanpa validasi.
	 *
	 * <p>Dipanggil dari {@code GrupPenilaianAction.onSave(...)} pada dua tahap berurutan:
	 * {@code false} untuk semua baris milik grup penilaian yang sedang disimpan, lalu {@code true}
	 * untuk baris yang checkbox-nya tercentang. Tidak ada pemanggil lain.</p>
	 *
	 * @param aktif nilai baru; {@code null} akan dibaca kembali sebagai {@code true} oleh
	 *              {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan {@link GrupPenilaian} induk pemetaan ini (kolom {@code grup_penilaian_id},
	 * wajib terisi), setelah proxy lazy diselesaikan lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * <p>Hasil {@code check(...)} ditugaskan kembali ke field &mdash; itu resolusi proxy standar
	 * agar object tetap terpakai walau sesi Hibernate yang memuatnya sudah tertutup, bukan mutasi
	 * data bisnis.</p>
	 *
	 * <p>Relasi ini juga menjadi satu-satunya penentu tenant baris ini: sekolah/yayasan pemilik
	 * pemetaan dibaca dari {@link GrupPenilaian#getSekolah()}/{@link GrupPenilaian#getYayasan()}.</p>
	 *
	 * @return grup penilaian induk; secara skema tidak boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "grup_penilaian_id", nullable = false)
	public GrupPenilaian getGrupPenilaian() {
		grupPenilaian = check(grupPenilaian);
		return grupPenilaian;
	}

	/**
	 * Menyetel grup penilaian induk. Tanpa validasi &mdash; berbeda dengan beberapa setter relasi
	 * lain di paket ini, {@code null} maupun object tanpa id <b>tidak</b> ditolak, sehingga
	 * kesalahan baru terdeteksi saat {@code INSERT}/{@code UPDATE} melanggar {@code NOT NULL}.
	 *
	 * <p>Dipanggil dari {@code GrupPenilaianAction.onSave(...)} pada tahap menghidupkan kembali
	 * baris terpilih (baris baru dibuat tanpa induk saat render, induk baru diisi saat simpan).</p>
	 *
	 * @param grupPenilaian grup penilaian induk yang baru
	 */
	public void setGrupPenilaian(GrupPenilaian grupPenilaian) {
		this.grupPenilaian = grupPenilaian;
	}

	/**
	 * Mengembalikan {@link GrupKategoriItemPenilaianSiswa} yang dipetakan oleh baris ini (kolom
	 * {@code grup_kategori_item_penilaian_siswa}, wajib terisi), setelah proxy lazy diselesaikan
	 * lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * <p>Inilah nilai yang sesungguhnya dipanen seluruh pembaca runtime &mdash; mereka
	 * memproyeksikan {@code grupKategoriItemPenilaianSiswa.id} lalu memuat masternya, sehingga
	 * baris detail sendiri hampir tidak pernah dihidrasi sebagai object.</p>
	 *
	 * <p>Perhatikan bahwa <b>tidak ada</b> pembaca runtime yang memeriksa
	 * {@code getGrupKategoriItemPenilaianSiswa().getAktif()}; status aktif grup kategori hanya
	 * berpengaruh di layar master. Asimetri itu adalah inti bom waktu yang dijelaskan di Javadoc
	 * kelas.</p>
	 *
	 * @return grup kategori item penilaian siswa yang dipetakan; secara skema tidak boleh
	 *         {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "grup_kategori_item_penilaian_siswa", nullable = false)
	public GrupKategoriItemPenilaianSiswa getGrupKategoriItemPenilaianSiswa() {
		grupKategoriItemPenilaianSiswa = check(grupKategoriItemPenilaianSiswa);
		return grupKategoriItemPenilaianSiswa;
	}

	/**
	 * Menyetel grup kategori yang dipetakan. Tanpa validasi.
	 *
	 * <p>Dipanggil {@code GrupPenilaianAction} saat merender setiap checkbox &mdash; termasuk untuk
	 * baris yang sudah ada di basis data (nilainya disetel ulang ke object yang sama), jadi
	 * pemanggilan itu tidak mengotori data.</p>
	 *
	 * @param grupKategoriItemPenilaianSiswa grup kategori target yang baru
	 */
	public void setGrupKategoriItemPenilaianSiswa(GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa) {
		this.grupKategoriItemPenilaianSiswa = grupKategoriItemPenilaianSiswa;
	}

	/**
	 * Mengembalikan nomor urut tampil baris ini dengan cara <b>meneruskan</b> nomor urut milik grup
	 * kategori yang ditunjuk, menimpa implementasi {@link GeneralValueObject#getNomorUrut()}.
	 *
	 * <p><b>Praktis selalu mengembalikan {@code 1}.</b> Alasannya:
	 * {@link GrupKategoriItemPenilaianSiswa} <b>tidak</b> mendeklarasikan properti
	 * {@code nomorUrut} sendiri; ia hanya mewarisi field POJO dari {@link GeneralValueObject},
	 * yang bukan {@code @Entity}/{@code @MappedSuperclass} sehingga field itu tidak pernah
	 * dipetakan ke kolom mana pun dan selalu bernilai {@code null} untuk object hasil muat dari
	 * basis data. Nilai {@code null} itu lalu diubah menjadi {@code 1} oleh baris terakhir method
	 * ini. Nilai {@code 0} hanya mungkin muncul bila FK grup kategori belum terisi sama sekali
	 * (object baru yang belum disetel).</p>
	 *
	 * <p>Konsekuensi lanjutan: {@link GeneralValueObject#compareTo(GeneralValueObject)} memakai
	 * {@code getNomorUrut()} sebagai kunci urut <b>pertama</b> dan berhenti di situ bila kedua
	 * sisi tidak {@code null}. Karena method ini tidak pernah mengembalikan {@code null},
	 * membandingkan dua {@code DetailGrupPenilaian} selalu menghasilkan {@code 0} dan
	 * {@code Collections.sort(...)} atas daftar baris detail tidak melakukan apa-apa. Tidak ada
	 * kode yang bergantung pada hal ini &mdash; pengurutan yang nyata dilakukan atas daftar
	 * {@link GrupKategoriItemPenilaianSiswa} hasil proyeksi, yang {@code compareTo}-nya memakai
	 * {@code nama} lebih dulu.</p>
	 *
	 * <p><b>Dua kuirk teknis yang perlu diketahui bila method ini akan disentuh:</b></p>
	 * <ol>
	 *   <li>Method membaca <b>field</b> {@code grupKategoriItemPenilaianSiswa} secara langsung,
	 *       bukan lewat {@link #getGrupKategoriItemPenilaianSiswa()}, sehingga melewati
	 *       {@link GeneralValueObject#check(Object)}. Pada object yang sudah lepas dari sesi
	 *       Hibernate, jalur getter akan memulihkan proxy sedangkan jalur ini bisa gagal.</li>
	 *   <li>Karena kelas ini memakai akses properti dan pasangan getter/setter {@code nomorUrut}
	 *       lengkap terbentuk (getter di sini, setter diwarisi), Hibernate memperlakukannya sebagai
	 *       properti persisten &mdash; dengan {@code hbm2ddl.auto=update} kolom {@code nomorUrut}
	 *       akan dibuat pada tabel dan diaudit Envers, padahal isinya hanyalah nilai turunan yang
	 *       tidak pernah dibaca kembali (setter menulis field induk, sedangkan getter ini
	 *       mengabaikannya sama sekali). Bentuk yang sama terdapat pada
	 *       {@link DetailJenisPenilaian} dan {@link DetailGrupKategoriItemPenilaianSiswa}, jadi ini
	 *       kuirk keluarga tabel penghubung, bukan kekhususan berkas ini.</li>
	 * </ol>
	 *
	 * @return nomor urut grup kategori yang ditunjuk; {@code 1} bila nomor urutnya {@code null}
	 *         (kasus normal), atau {@code 0} bila FK grup kategori belum terisi
	 */
	public Integer getNomorUrut() {
		Integer nomorUrut = 0;
		if (grupKategoriItemPenilaianSiswa != null) {
			nomorUrut = grupKategoriItemPenilaianSiswa.getNomorUrut();
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}



}
