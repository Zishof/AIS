package ais.database.model.sekolah;

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

import ais.database.model.GeneralValueObject;

/**
 * Satu sel matriks <b>angka kredit kegiatan kesiswaan</b> (tabel
 * {@code sekolah.nilai_kegiatan_kesiswaan}). Setiap baris menetapkan berapa poin yang diperoleh
 * untuk satu kombinasi <i>rincian kegiatan</i> &times; <i>skala penyelenggaraan</i> &times;
 * <i>jabatan/peran</i>. Isinya murni master data (tarif/rubrik): baris ini tidak menunjuk siswa
 * mana pun, dan tidak menunjuk peristiwa kegiatan mana pun.
 *
 * <p>Kelas ini adalah <b>tempat angka kredit yang sesungguhnya disimpan</b> untuk modul kesiswaan.
 * Hal itu disebut eksplisit di Javadoc {@link KelompokKegiatanKesiswaan}: kolom
 * {@link KelompokKegiatanKesiswaan#getBobot() bobot} dan
 * {@link KelompokKegiatanKesiswaan#getNilaiMinimal() nilaiMinimal} di lapis kelompok praktis
 * <i>write-only</i> (bisa diisi lewat grid masternya, tetapi tidak ada satu pun pembaca), karena
 * rubrik nilainya justru digantungkan satu lapis lebih dalam &mdash; ke
 * {@link DetailKelompokKegiatanKesiswaan} lewat kelas ini.</p>
 *
 * <p>Jangan tertukar dengan entity bernama mirip di paket yang sama; ketiganya berada di lapis
 * yang berbeda:</p>
 *
 * <ul>
 *   <li>{@link KegiatanKesiswaan} &mdash; <b>peristiwanya</b>: satu lomba, kepanitiaan, atau
 *       kepengurusan yang benar-benar diselenggarakan sekolah;</li>
 *   <li>{@code KegiatanKesiswaanPunyaSiswa} &mdash; <b>keikutsertaan</b>: satu siswa pada satu
 *       kegiatan, lengkap dengan peran dan skala yang diakui baginya;</li>
 *   <li>kelas ini &mdash; <b>tarifnya</b>: "kombinasi seperti ini bernilai sekian poin".</li>
 * </ul>
 *
 * <h2>Kunci gabungan tiga sisi &mdash; mekanisme rapi, bukan {@code ParameterUmum}</h2>
 *
 * <p>Identitas logis sebuah baris rubrik adalah tripel kunci asing berikut:</p>
 *
 * <ul>
 *   <li>{@link #getDetailKelompokKegiatanKesiswaan()} &mdash; rincian/jenis kegiatan di bawah suatu
 *       {@link KelompokKegiatanKesiswaan} (kolom {@code detail_kelompok_kegiatan_kesiswaan});</li>
 *   <li>{@link #getSkalaKegiatanKesiswaan()} &mdash; tingkat penyelenggaraan, mis. sekolah,
 *       kecamatan, provinsi, nasional (kolom {@code skala_kegiatan_kesiswaan});</li>
 *   <li>{@link #getJabatanKegiatanKesiswaan()} &mdash; peran peserta, mis. ketua, anggota, panitia
 *       (kolom {@code jabatan_kegiatan_kesiswaan}). Sisi ini <b>boleh {@code null}</b>: rincian
 *       kegiatan yang tidak membedakan peran hanya punya satu baris rubrik per skala, dengan
 *       jabatan kosong. Perilaku itu terlihat langsung di
 *       {@code NilaiKegiatanKesiswaanAction.NilaiKegiatanKesiswaanRenderer.render()}, yang memanggil
 *       {@code tampilRow(..., null)} ketika koleksi jabatan milik rincian kosong.</li>
 * </ul>
 *
 * <p><b>Hasil verifikasi struktur kunci.</b> Tripel di atas direkam ulang sebagai satu string di
 * {@link #getKodeUnik()}, yang dipetakan {@code @Column(unique = true)}. Jadi kelas ini memakai
 * <b>mekanisme yang rapi</b> &mdash; cerminan persis
 * {@link ais.database.model.NilaiKegiatanKemahasiswaan} versi perguruan tinggi, sampai ke rumus
 * perakitan kodenya. Kelas ini <b>tidak</b> memakai pola rapuh "simpan tarif di
 * {@code ParameterUmum} bertipe teks" seperti varian kegiatan kedosenan. Kesamaan dengan versi PT
 * bahkan sampai ke {@link #serialVersionUID}, yang nilainya identik &mdash; sisa salin-tempel
 * berkas hasil {@code hbm2java}, tidak berdampak apa pun karena identitas serialisasi berlaku per
 * kelas.</p>
 *
 * <h2>Satu-satunya jalur pencarian: {@code kodeUnik}</h2>
 *
 * <p>Berbeda dari versi PT yang punya <i>dua</i> jalur pencarian, versi sekolah hanya punya satu.
 * Seluruh pencarian baris rubrik dilakukan {@code ais.action.master.sekolah
 * .NilaiKegiatanKesiswaanAction} dengan {@code Restrictions.eq("kodeUnik", ...)}, yaitu pencocokan
 * <i>string</i> persis atas tripel di atas, di empat titik: perenderan sel grid, dua cabang
 * unduhan Excel (ber-jabatan dan tanpa jabatan), serta unggahan Excel. Di keempat titik itu string
 * kunci <b>dirakit ulang secara manual</b>, bukan lewat {@link #getKodeUnik()}; rumusnya kebetulan
 * sama persis, tetapi duplikasi tersebut berarti setiap perubahan format di getter ini harus
 * diikutkan ke empat tempat lain atau seluruh pencarian akan meleset diam-diam
 * ({@code uniqueResult()} mengembalikan {@code null} &rarr; sel tampak kosong dan baris duplikat
 * baru dibuat).</p>
 *
 * <h2>Tabel yang diisi tetapi tidak pernah dibaca</h2>
 *
 * <p><b>Temuan penting saat penelusuran.</b> Di versi perguruan tinggi, matriks yang setara dibaca
 * {@code ais.common.Common#hitungAngkaKredit(Mahasiswa)} lewat {@code LEFT JOIN} SQL native, dan
 * hasilnya dipakai enam pemanggil sebagai syarat ujian, skripsi, dan beasiswa. Di versi sekolah
 * <b>padanan itu tidak ada</b>: tidak satu pun kode aplikasi yang menjumlahkan
 * {@link #getNilai() nilai} untuk seorang siswa. Rekap kegiatan kesiswaan yang ada
 * ({@code DashboardRekapKegiatanKesiswaan}) hanya <i>mencacah jumlah baris</i> keikutsertaan, sama
 * sekali tidak menyentuh tabel ini.</p>
 *
 * <p>Jadi entity ini <b>bukan yatim struktural</b> &mdash; ia terdaftar di
 * {@code hibernate.cfg.xml}, punya layar master ZK aktif, halaman UI baru, dan jalur impor/ekspor
 * Excel &mdash; melainkan <b>yatim fungsional</b>: datanya bisa diisi, diunduh, dan diunggah,
 * tetapi belum pernah dikonsumsi logika bisnis apa pun. Porting modul dari PT ke sekolah tampaknya
 * berhenti di layar masternya.</p>
 *
 * <p>Satu detail teknis yang mungkin ikut menjelaskan mengapa perhitungan versi sekolah belum
 * ditulis: keluarga tabel ini <b>terbelah dua skema</b>. Kelas ini,
 * {@link JabatanKegiatanKesiswaan}, {@link SkalaKegiatanKesiswaan}, dan {@link KegiatanKesiswaan}
 * berada di skema {@code sekolah}, sedangkan {@link KelompokKegiatanKesiswaan} dan
 * {@link DetailKelompokKegiatanKesiswaan} berada di skema {@code public}. SQL native gaya
 * {@code hitungAngkaKredit} versi PT &mdash; yang menulis nama tabel tanpa kualifikasi skema
 * &mdash; tidak akan langsung jalan di sini tanpa penyesuaian.</p>
 *
 * <h2>Getter yang menulis balik</h2>
 *
 * <p>Pemetaan kelas ini memakai <i>property access</i> (anotasi JPA menempel pada getter, lihat
 * {@link #getId()}), sehingga Hibernate memanggil getter saat memuat, saat memeriksa perubahan
 * (dirty check), dan saat flush. {@link #getKodeUnik()} memanfaatkan hal itu: getter tersebut
 * <b>menghitung ulang dan menimpa</b> field {@code kodeUnik} dari ketiga relasi setiap kali
 * dipanggil. Konsekuensinya kode unik terpelihara otomatis tanpa ada satu pun pemanggil
 * {@link #setKodeUnik(String)} di jalur normal &mdash; tetapi juga berarti nilai yang disetel
 * manual akan hilang diam-diam pada penyimpanan berikutnya. Rincian lengkap ada di Javadoc getter
 * tersebut.</p>
 *
 * <p>Sebagai pembanding, {@link #getNilai()} hanya <i>mengembalikan</i> {@code 0.0} untuk kolom
 * kosong dan <b>tidak</b> menuliskannya ke field, sehingga kolom yang belum diisi tetap
 * {@code NULL} di basis data. Ketiga getter relasi murni mengembalikan isi field apa adanya: tidak
 * ada getter di kelas ini yang menutup sesi Hibernate, memuat data tambahan, maupun memanggil
 * {@link GeneralValueObject#check(Object)}.</p>
 *
 * <h2>Layar pengelola dan efek sampingnya</h2>
 *
 * <p>Baris rubrik dikelola lewat {@code /pages/master/sekolah/nilai_kegiatan_kesiswaan.zul}
 * ({@code ais.action.master.sekolah.NilaiKegiatanKesiswaanAction}). Layar itu tidak punya menu
 * sendiri: ia dimuat sebagai tab di dalam {@code kelompok_kegiatan_kesiswaan.zul} lewat
 * {@code KelompokKegiatanKesiswaanAction.onNilaiKegiatanKesiswaan()}, sehingga hak aksesnya secara
 * praktis menempel pada menu Kelompok Kegiatan Kesiswaan induk. Tampilannya berupa matriks
 * &mdash; baris = rincian kegiatan (dipecah lagi per jabatan), kolom = skala &mdash; dengan satu
 * kotak angka per sel, plus tombol unduh/unggah berkas Excel untuk penyuntingan massal.</p>
 *
 * <p>Beberapa perilaku layar tersebut berdampak langsung pada isi tabel ini dan sebaiknya
 * diketahui sebelum menafsirkan datanya:</p>
 *
 * <ul>
 *   <li><b>Membuka layar menciptakan baris.</b> Saat merender sel, {@code tampilRow()} mencari
 *       baris rubrik menurut {@code kodeUnik}; bila tidak ada, ia langsung membuat instance baru
 *       dan menyimpannya lewat {@code Common.refreshSaveOrUpdate(session, ...)} dengan
 *       {@link #getNilai() nilai} masih kosong. Jadi sekadar <i>membaca</i> layar sudah menulis ke
 *       basis data, dan jumlah baris tabel ini tumbuh mengikuti kombinasi yang pernah
 *       <i>ditampilkan</i>, bukan yang pernah diisi. Karena kelas ini {@code @Audited}, setiap
 *       baris otomatis itu juga melahirkan revisi Envers.</li>
 *   <li><b>Satu query per sel.</b> Pencarian {@code kodeUnik} dijalankan terpisah untuk setiap sel
 *       grid dan setiap sel Excel (pola N+1); pada rubrik yang lebar, satu kali render bisa
 *       berarti ratusan {@code SELECT} plus {@code INSERT} susulan dari butir sebelumnya.</li>
 *   <li><b>Unggahan tidak bisa mengosongkan nilai.</b> Proses unggah hanya menyimpan sel yang lolos
 *       syarat {@code nilai != null && nilai > 0.1}. Mengisi {@code 0} di berkas Excel tidak
 *       menghapus atau menolkan nilai lama &mdash; barisnya sekadar dilewati &mdash; dan bobot sah
 *       yang kebetulan berada di rentang {@code 0 < nilai <= 0.1} ikut terbuang tanpa peringatan.
 *       Penurunan bobot ke nol hanya bisa dilakukan lewat kotak angka di layar.</li>
 *   <li><b>Tidak ada tombol hapus.</b> Layar hanya menyediakan penyuntingan nilai; baris rubrik
 *       yang sudah lahir tidak pernah dibuang lewat UI, termasuk baris yang rincian atau skalanya
 *       kemudian dinonaktifkan.</li>
 * </ul>
 *
 * <h2>Cakupan data dan kontrol akses</h2>
 *
 * <p><b>Tidak ada kolom tenant di sini, dan itu memang disengaja.</b> Kelas ini tidak punya relasi
 * ke {@code Sekolah}/{@code Yayasan}, dan begitu pula seluruh rantai induknya
 * ({@link DetailKelompokKegiatanKesiswaan}, {@link KelompokKegiatanKesiswaan},
 * {@link SkalaKegiatanKesiswaan}, {@link JabatanKegiatanKesiswaan}). Rubrik angka kredit adalah
 * master tingkat instalasi, dipakai bersama seluruh sekolah. Karena itu absennya penyaring tenant
 * pada {@code NilaiKegiatanKesiswaanAction.initCriteria()} <b>bukan</b> kebocoran data lintas
 * tenant &mdash; tidak ada data per-sekolah yang bisa bocor. Yang perlu disadari justru
 * kebalikannya: pengubahan satu sel oleh admin sekolah mana pun berlaku untuk <b>seluruh</b>
 * sekolah dalam instalasi.</p>
 *
 * <p><b>Catatan gerbang akses.</b> {@code NilaiKegiatanKesiswaanAction} memanggil
 * {@code Common.doCheckSecurity()} di {@code doBeforeCompose()} &mdash; gerbang sesi login &mdash;
 * tetapi <b>tidak memanggil {@code checkPrevilages} sama sekali</b>, sehingga tidak ada pembedaan
 * hak baca/ubah pada layar ini: siapa pun yang bisa mencapai halamannya dapat menyunting seluruh
 * matriks maupun menimpanya massal lewat unggahan Excel. Dampak <i>saat ini</i> rendah justru
 * karena tabel ini belum punya pembaca (lihat bagian sebelumnya) &mdash; angka yang diubah tidak
 * mengubah keputusan apa pun. Sifatnya "bom waktu": begitu perhitungan angka kredit versi sekolah
 * ditulis dan dipakai sebagai syarat (kenaikan kelas, kelulusan, beasiswa, sebagaimana padanan
 * PT-nya), layar tanpa gerbang hak ini langsung menjadi titik manipulasi nilai. Temuan ini dicatat
 * apa adanya; perbaikannya bukan di kelas ini.</p>
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
 * revisi baris rubrik. {@code dynamicInsert}/{@code dynamicUpdate} membuat Hibernate hanya
 * menyertakan kolom yang benar-benar terisi/berubah pada pernyataan SQL-nya. Ketiga relasi memakai
 * {@code @Fetch(FetchMode.SELECT)}, artinya masing-masing diambil lewat {@code SELECT} terpisah,
 * bukan digabung ke query induk.</p>
 *
 * <p>Komentar bawaan hasil pembangkitan pada berkas ini semula berbunyi "Bank generated by
 * hbm2java"; kata "Bank" merupakan sisa salin-tempel dari entity lain dan tidak ada kaitannya
 * dengan tabel ini.</p>
 *
 * @see KelompokKegiatanKesiswaan
 * @see DetailKelompokKegiatanKesiswaan
 * @see JabatanKegiatanKesiswaan
 * @see SkalaKegiatanKesiswaan
 * @see KegiatanKesiswaan
 * @see ais.database.model.NilaiKegiatanKemahasiswaan
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "nilai_kegiatan_kesiswaan")
public class NilaiKegiatanKesiswaan extends GeneralValueObject {

	/**
	 * Versi serialisasi Java.
	 *
	 * <p>Nilainya sama persis dengan milik {@link ais.database.model.NilaiKegiatanKemahasiswaan}
	 * (versi perguruan tinggi) &mdash; sisa salin-tempel berkas hasil {@code hbm2java} saat modul
	 * ini diporting ke konteks sekolah. Tidak berdampak apa pun karena identitas serialisasi
	 * dievaluasi per kelas, bukan per nilai.</p>
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
	 * <p>Bila {@code olehId} bernilai {@code null} atau hanya berisi spasi, method langsung kembali
	 * tanpa mengubah apa pun sehingga jejak audit yang sudah ada tidak terhapus. Pola "setter
	 * tolak-kosong" ini seragam di seluruh entity AIS.</p>
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
	 * <p>Mendelegasikan ke {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang
	 * mengisi {@link #setOleh(String)}, {@link #setOlehId(String)} dan
	 * {@link #setTanggal_dirubah(Date)} dari konteks pengguna yang sedang aktif.</p>
	 *
	 * <p><b>Dipanggil oleh:</b> provider JPA/Hibernate saja &mdash; jangan dipanggil manual.
	 * Perhatikan bahwa hanya {@code @PreUpdate} yang dipasang; pada {@code INSERT} pertama nilai
	 * audit bergantung pada apa yang diisi pemanggil (atau interceptor global), bukan pada method
	 * ini. Untuk kelas ini hal tersebut cukup terasa, karena baris rubrik sering lahir otomatis dari
	 * perenderan layar (lihat Javadoc kelas) tanpa ada pengguna yang secara sadar menekan tombol
	 * simpan. Perubahan lengkap per revisi tetap terekam Envers karena kelas ini
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
	 * @return waktu perubahan terakhir; untuk instance baru bernilai waktu pembuatan objek, bukan
	 *         {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat baris rubrik: {@code "<id>-<rincian kegiatan>"}.
	 *
	 * <p>Bagian kedua adalah hasil {@code toString()} milik
	 * {@link DetailKelompokKegiatanKesiswaan}, dibaca langsung dari field (bukan lewat getter),
	 * sehingga nilainya {@code "null"} bila relasi belum disetel.</p>
	 *
	 * <p><b>Perhatikan:</b> representasi ini <b>tidak menyertakan skala maupun jabatan</b>, padahal
	 * keduanya bagian dari identitas logis baris (lihat {@link #getKodeUnik()}). Semua sel pada
	 * rincian kegiatan yang sama karena itu tercetak dengan teks yang mirip dan hanya dibedakan
	 * oleh {@code id}. Untuk keperluan penelusuran, {@link #getKodeUnik()} jauh lebih informatif.
	 * Method ini menimpa {@link GeneralValueObject#toString()}.</p>
	 *
	 * @return teks gabungan {@code id} dan rincian kelompok kegiatan
	 */
	public String toString() {
		return id + "-" + detailKelompokKegiatanKesiswaan;
	}

	/**
	 * Rincian/jenis kegiatan yang ditarifkan baris ini &mdash; sisi pertama kunci gabungan. Lihat
	 * {@link #getDetailKelompokKegiatanKesiswaan()}.
	 */
	private DetailKelompokKegiatanKesiswaan detailKelompokKegiatanKesiswaan;

	/**
	 * Peran/jabatan peserta yang ditarifkan baris ini &mdash; sisi ketiga kunci gabungan,
	 * <b>boleh {@code null}</b>. Lihat {@link #getJabatanKegiatanKesiswaan()}.
	 */
	private JabatanKegiatanKesiswaan jabatanKegiatanKesiswaan;

	/**
	 * Skala/tingkat penyelenggaraan yang ditarifkan baris ini &mdash; sisi kedua kunci gabungan.
	 * Lihat {@link #getSkalaKegiatanKesiswaan()}.
	 */
	private SkalaKegiatanKesiswaan skalaKegiatanKesiswaan;

	/**
	 * Bobot angka kredit untuk kombinasi kunci baris ini (kolom {@code nilai}). Boleh {@code null}
	 * di basis data; lihat {@link #getNilai()} untuk perlakuan nilai kosong.
	 */
	private Double nilai;

	/**
	 * Bentuk string dari kunci gabungan tiga sisi (kolom {@code kode_unik}, berkendala
	 * {@code unique}). Diturunkan ulang setiap kali {@link #getKodeUnik()} dipanggil.
	 */
	private String kodeUnik;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Menghasilkan baris rubrik tanpa satu pun sisi kunci terisi; pemanggil wajib menyetel
	 * {@link #setDetailKelompokKegiatanKesiswaan(DetailKelompokKegiatanKesiswaan)} dan
	 * {@link #setSkalaKegiatanKesiswaan(SkalaKegiatanKesiswaan)} sebelum menyimpan, karena tanpa
	 * keduanya {@link #getKodeUnik()} tidak akan pernah menghasilkan kode dan baris tersimpan
	 * dengan {@code kode_unik} {@code NULL} sehingga tidak akan pernah ditemukan pencarian layar
	 * masternya.</p>
	 *
	 * <p><b>Dipanggil oleh:</b> Hibernate saat memuat baris, serta
	 * {@code NilaiKegiatanKesiswaanAction} pada jalur pembuatan otomatis saat render sel dan pada
	 * jalur unggahan Excel.</p>
	 */
	public NilaiKegiatanKesiswaan() {
	}

	/**
	 * Mengembalikan kunci utama baris rubrik.
	 *
	 * <p>Kolom {@code id} memakai strategi {@code IDENTITY} (berurutan, ditetapkan basis data) dan
	 * ditandai {@code insertable = false} sehingga tidak pernah disertakan pada {@code INSERT}.</p>
	 *
	 * @return kunci utama, atau {@code null} untuk instance yang belum tersimpan
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
	 * <p>Disediakan untuk Hibernate; kode aplikasi tidak boleh memanggilnya karena {@code id}
	 * dibangkitkan basis data.</p>
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan rincian/jenis kegiatan yang ditarifkan baris ini (kolom
	 * {@code detail_kelompok_kegiatan_kesiswaan}) &mdash; sisi pertama kunci gabungan.
	 *
	 * <p>Rincian inilah yang membawa daftar {@link SkalaKegiatanKesiswaan} dan
	 * {@link JabatanKegiatanKesiswaan} yang berlaku baginya (lewat dua tabel penghubung), sehingga
	 * ia menentukan sel mana saja yang muncul di baris matriks layar master. Induknya,
	 * {@link KelompokKegiatanKesiswaan}, hanya dipakai sebagai label pengelompokan pada grid.</p>
	 *
	 * <p>Relasi dipetakan {@code @ManyToOne} dengan {@code CascadeType.PERSIST} dan
	 * {@code CascadeType.MERGE} &mdash; jadi menyimpan baris rubrik ikut mem-{@code persist}/
	 * {@code merge} rincian yang menempel padanya, tetapi tidak pernah menghapusnya. Pengambilan
	 * memakai {@code FetchMode.SELECT} ({@code SELECT} terpisah).</p>
	 *
	 * <p>Getter ini murni: tidak ada perhitungan ulang maupun penulisan balik.</p>
	 *
	 * @return rincian kelompok kegiatan kesiswaan, atau {@code null} bila belum disetel
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "detail_kelompok_kegiatan_kesiswaan")
	public DetailKelompokKegiatanKesiswaan getDetailKelompokKegiatanKesiswaan() {
		return detailKelompokKegiatanKesiswaan;
	}

	/**
	 * Menetapkan rincian/jenis kegiatan yang ditarifkan baris ini.
	 *
	 * <p>Mengubah sisi kunci ini <b>mengubah identitas logis baris</b>: pembacaan
	 * {@link #getKodeUnik()} berikutnya akan menghitung ulang kode, dan &mdash; karena kolomnya
	 * berkendala {@code unique} &mdash; penyimpanan bisa berbenturan dengan baris rubrik lain yang
	 * sudah memakai kombinasi tersebut.</p>
	 *
	 * <p><b>Dipanggil oleh:</b> Hibernate saat memuat baris, serta
	 * {@code NilaiKegiatanKesiswaanAction} saat membuat baris baru (render sel dan unggahan
	 * Excel).</p>
	 *
	 * @param detailKelompokKegiatanKesiswaan rincian kelompok kegiatan kesiswaan; boleh
	 *        {@code null}, tetapi baris tanpa rincian tidak akan pernah punya kode unik
	 */
	public void setDetailKelompokKegiatanKesiswaan(DetailKelompokKegiatanKesiswaan detailKelompokKegiatanKesiswaan) {
		this.detailKelompokKegiatanKesiswaan = detailKelompokKegiatanKesiswaan;
	}

	/**
	 * Mengembalikan peran/jabatan peserta yang ditarifkan baris ini (kolom
	 * {@code jabatan_kegiatan_kesiswaan}) &mdash; sisi ketiga kunci gabungan.
	 *
	 * <p><b>Boleh {@code null} secara sah.</b> Rincian kegiatan yang tidak membedakan peran hanya
	 * punya satu baris rubrik per skala, dengan jabatan kosong; layar masternya merender baris
	 * seperti itu lewat {@code tampilRow(..., null)}. Pada {@link #getKodeUnik()}, jabatan kosong
	 * menghasilkan sufiks {@code "-"} kosong di ujung kode, sehingga baris "tanpa jabatan" tetap
	 * terbedakan tegas dari baris ber-jabatan.</p>
	 *
	 * <p>Pemetaan dan perilaku {@code cascade}/{@code fetch} sama dengan
	 * {@link #getDetailKelompokKegiatanKesiswaan()}. Getter ini murni.</p>
	 *
	 * @return jabatan kegiatan kesiswaan, atau {@code null} bila rubriknya tidak membedakan peran
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jabatan_kegiatan_kesiswaan")
	public JabatanKegiatanKesiswaan getJabatanKegiatanKesiswaan() {
		return jabatanKegiatanKesiswaan;
	}

	/**
	 * Menetapkan peran/jabatan peserta yang ditarifkan baris ini.
	 *
	 * <p>Seperti dua setter relasi lainnya, mengubah sisi kunci ini mengubah identitas logis baris
	 * dan kode unik yang dihitung ulang pada pembacaan berikutnya. {@code null} diterima apa adanya
	 * dan bermakna "rubrik tanpa pembedaan peran".</p>
	 *
	 * <p><b>Dipanggil oleh:</b> Hibernate saat memuat baris, serta
	 * {@code NilaiKegiatanKesiswaanAction} saat membuat baris baru.</p>
	 *
	 * @param jabatanKegiatanKesiswaan jabatan kegiatan kesiswaan; {@code null} berarti tanpa
	 *        pembedaan peran
	 */
	public void setJabatanKegiatanKesiswaan(JabatanKegiatanKesiswaan jabatanKegiatanKesiswaan) {
		this.jabatanKegiatanKesiswaan = jabatanKegiatanKesiswaan;
	}

	/**
	 * Mengembalikan skala/tingkat penyelenggaraan yang ditarifkan baris ini (kolom
	 * {@code skala_kegiatan_kesiswaan}) &mdash; sisi kedua kunci gabungan.
	 *
	 * <p>Skala inilah yang menjadi <i>kolom</i> pada matriks layar master (mis. tingkat sekolah,
	 * kecamatan, kabupaten, provinsi, nasional). Berbeda dari jabatan, sisi ini <b>wajib</b> terisi
	 * agar {@link #getKodeUnik()} menghasilkan kode.</p>
	 *
	 * <p>Pemetaan dan perilaku {@code cascade}/{@code fetch} sama dengan
	 * {@link #getDetailKelompokKegiatanKesiswaan()}. Getter ini murni.</p>
	 *
	 * @return skala kegiatan kesiswaan, atau {@code null} bila belum disetel
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "skala_kegiatan_kesiswaan")
	public SkalaKegiatanKesiswaan getSkalaKegiatanKesiswaan() {
		return skalaKegiatanKesiswaan;
	}

	/**
	 * Menetapkan skala/tingkat penyelenggaraan yang ditarifkan baris ini.
	 *
	 * <p>Mengubah sisi kunci ini mengubah identitas logis baris dan kode unik yang dihitung ulang
	 * pada pembacaan berikutnya.</p>
	 *
	 * <p><b>Dipanggil oleh:</b> Hibernate saat memuat baris, serta
	 * {@code NilaiKegiatanKesiswaanAction} saat membuat baris baru.</p>
	 *
	 * @param skalaKegiatanKesiswaan skala kegiatan kesiswaan; boleh {@code null}, tetapi baris
	 *        tanpa skala tidak akan pernah punya kode unik
	 */
	public void setSkalaKegiatanKesiswaan(SkalaKegiatanKesiswaan skalaKegiatanKesiswaan) {
		this.skalaKegiatanKesiswaan = skalaKegiatanKesiswaan;
	}

	/**
	 * Mengembalikan bobot angka kredit untuk kombinasi kunci baris ini, dengan default {@code 0.0}
	 * bila kolomnya kosong.
	 *
	 * <p><b>Tidak destruktif.</b> Berbeda dari {@link #getKodeUnik()}, getter ini hanya
	 * <i>mengembalikan</i> {@code 0.0} untuk kolom kosong dan tidak menuliskannya ke field, sehingga
	 * baris yang lahir otomatis dari perenderan layar tetap tersimpan dengan {@code nilai}
	 * {@code NULL} di basis data. Konsekuensinya, "belum pernah diisi" ({@code NULL}) dan "sengaja
	 * dinolkan" ({@code 0}) tidak bisa dibedakan lagi oleh pemanggil Java, meski masih terbedakan
	 * di tingkat SQL.</p>
	 *
	 * <p>Tidak ada validasi rentang di sisi entity: nilai negatif maupun sangat besar diterima.
	 * Kolomnya tidak diberi anotasi {@code @Column} eksplisit sehingga namanya mengikuti nama
	 * properti, yaitu {@code nilai}.</p>
	 *
	 * <p><b>Dibaca oleh:</b> saat ini hanya layar masternya sendiri &mdash; kotak angka pada matriks
	 * dan unduhan Excel. Belum ada perhitungan angka kredit siswa yang menjumlahkan nilai ini
	 * (lihat Javadoc kelas).</p>
	 *
	 * @return bobot angka kredit, atau {@code 0.0} bila kolomnya {@code NULL}
	 */
	public Double getNilai() {
		return nilai == null ? 0.0 : nilai;
	}

	/**
	 * Menetapkan bobot angka kredit untuk kombinasi kunci baris ini.
	 *
	 * <p>Tanpa validasi apa pun; menerima {@code null}, nol, maupun nilai negatif.</p>
	 *
	 * <p><b>Dipanggil oleh:</b> Hibernate saat memuat baris; pendengar {@code onChange} kotak angka
	 * pada layar master (yang langsung menyusulinya dengan {@code Common.refreshUpdate(...)},
	 * sehingga setiap ketikan tersimpan seketika tanpa tombol simpan); dan jalur unggahan Excel
	 * &mdash; yang hanya memanggil setter ini untuk sel yang lolos syarat {@code nilai > 0.1},
	 * sehingga unggahan tidak pernah bisa menolkan nilai lama.</p>
	 *
	 * @param nilai bobot angka kredit baru; boleh {@code null}
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Mengembalikan kunci gabungan tiga sisi dalam bentuk string, <b>sambil menghitung ulang dan
	 * menimpa</b> field {@code kodeUnik}.
	 *
	 * <p>Format kodenya adalah {@code "<idRincian>-<idSkala>-<idJabatan>"}, dengan bagian terakhir
	 * dikosongkan bila jabatan {@code null} (menghasilkan sufiks {@code "-"} kosong di ujung).
	 * Perhitungan ulang hanya terjadi bila {@link #getDetailKelompokKegiatanKesiswaan() rincian}
	 * <b>dan</b> {@link #getSkalaKegiatanKesiswaan() skala} sama-sama terisi; bila salah satunya
	 * {@code null}, method mengembalikan isi field apa adanya (kemungkinan besar {@code null} atau
	 * kode lama yang sudah basi).</p>
	 *
	 * <p><b>Getter destruktif &mdash; ini disengaja dan menjadi mekanisme pemeliharaan kunci.</b>
	 * Karena pemetaan kelas ini memakai <i>property access</i>, Hibernate memanggil getter ini saat
	 * memuat, saat dirty check, dan saat flush; setiap panggilan itu menyegarkan kode dari ketiga
	 * relasi. Berkat itu {@link #setKodeUnik(String)} tidak pernah perlu dipanggil kode aplikasi.
	 * Efek sampingnya:</p>
	 *
	 * <ul>
	 *   <li>nilai apa pun yang disetel manual lewat {@link #setKodeUnik(String)} akan hilang
	 *       diam-diam pada pembacaan/penyimpanan berikutnya;</li>
	 *   <li>mengubah salah satu sisi kunci otomatis memindahkan baris ke kode baru, tanpa jejak
	 *       kode lama;</li>
	 *   <li>karena kolomnya {@code unique = true} tanpa {@code name} eksplisit (nama kolom mengikuti
	 *       nama properti, {@code kode_unik}), pemindahan itu bisa berbenturan dengan baris lain
	 *       yang sudah memakai kombinasi tujuan, dan kegagalannya baru muncul saat flush.</li>
	 * </ul>
	 *
	 * <p><b>Dipakai sebagai satu-satunya kunci pencarian.</b> Perenderan sel grid, dua cabang
	 * unduhan Excel, dan unggahan Excel pada {@code NilaiKegiatanKesiswaanAction} semuanya mencari
	 * baris rubrik dengan {@code Restrictions.eq("kodeUnik", ...)}. Namun keempatnya
	 * <b>merakit ulang string kuncinya sendiri</b> alih-alih memanggil method ini, sehingga rumus di
	 * sini dan di sana harus dijaga tetap identik. Pencarian memakai {@code uniqueResult()}, yang
	 * berarti duplikat &mdash; bila kendala {@code unique} tidak benar-benar ada di basis data
	 * &mdash; akan melempar galat saat layar dibuka, bukan sekadar memilih salah satu baris.</p>
	 *
	 * @return kode unik gabungan rincian, skala, dan jabatan; {@code null} bila rincian atau skala
	 *         belum disetel dan field belum pernah terisi
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		if (detailKelompokKegiatanKesiswaan != null && skalaKegiatanKesiswaan != null) {
			kodeUnik = detailKelompokKegiatanKesiswaan.getId() + "-" + skalaKegiatanKesiswaan.getId() + "-"
					+ (jabatanKegiatanKesiswaan == null ? "" : jabatanKegiatanKesiswaan.getId());
		}
		return kodeUnik;
	}

	/**
	 * Menetapkan kode unik gabungan secara langsung.
	 *
	 * <p><b>Praktis hanya untuk Hibernate.</b> Tidak ada kode aplikasi yang memanggil setter ini,
	 * dan nilai yang disetel manual tidak bertahan: {@link #getKodeUnik()} akan menghitung ulang
	 * dan menimpanya pada pembacaan berikutnya selama rincian dan skala terisi. Untuk mengubah kode
	 * unik secara sah, ubahlah sisi kuncinya lewat
	 * {@link #setDetailKelompokKegiatanKesiswaan(DetailKelompokKegiatanKesiswaan)},
	 * {@link #setSkalaKegiatanKesiswaan(SkalaKegiatanKesiswaan)}, atau
	 * {@link #setJabatanKegiatanKesiswaan(JabatanKegiatanKesiswaan)}.</p>
	 *
	 * @param kodeUnik kode gabungan; akan ditimpa hasil perhitungan ulang pada pembacaan berikutnya
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

}
