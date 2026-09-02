package ais.database.model;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

/**
 * Entity Hibernate yang memetakan tabel {@code public.detail_kelompok_kegiatan_kedosenan} pada
 * modul <b>Kegiatan Kedosenan</b> (aktivitas Tridharma dosen, dipakai untuk rekap kinerja dan
 * perhitungan Beban Kinerja Dosen/BKD). Satu baris mewakili satu <b>"Rincian Aspek"</b> di bawah
 * satu kelompok/aspek induk {@link KelompokKegiatanKedosenan} &mdash; misalnya rincian
 * "Menjadi narasumber seminar" di bawah aspek "Pendidikan &amp; Pengajaran".
 *
 * <h2>Posisi dalam rantai master modul kegiatan kedosenan</h2>
 * <p>
 * Entity ini adalah mata rantai KEDUA dari rantai master:
 * </p>
 * <pre>
 * KelompokKegiatanKedosenan  (aspek/kelompok, mis. "Pendidikan &amp; Pengajaran")
 *   &rarr; DetailKelompokKegiatanKedosenan  (rincian aspek &mdash; KELAS INI)
 *       &rarr; JabatanKegiatanKedosenan  (peran dosen: ketua/anggota/narasumber/...)
 *       &rarr; SkalaKegiatanKedosenan    (tingkat: fakultas/nasional/internasional/...)
 * </pre>
 * <p>
 * Relasi ke {@link JabatanKegiatanKedosenan} dan {@link SkalaKegiatanKedosenan} bersifat
 * many-to-many dan berfungsi sebagai <b>daftar pilihan yang diizinkan</b> untuk rincian ini:
 * hanya jabatan dan skala yang tercentang di layar master yang boleh muncul sebagai kombinasi
 * bagi rincian tersebut. Lihat {@link #getJabatanKegiatanKedosenans()} dan
 * {@link #getSkalaKegiatanKedosenans()}.
 * </p>
 * <p>
 * Sisi transaksi berada di {@link KegiatanKedosenan} (kegiatan konkret yang menunjuk satu baris
 * kelas ini lewat kolom {@code detail_kelompok_kegiatan_kedosenan}) dan
 * {@link KegiatanKedosenanPunyaDosen} (keikutsertaan satu dosen pada satu kegiatan, yang
 * menyimpan jabatan dan skala pilihannya sendiri).
 * </p>
 *
 * <h2>Di mana bobot/SKS kegiatan sesungguhnya disimpan (PENTING)</h2>
 * <p>
 * <b>Bukan di kelas ini.</b> Kelas ini TIDAK punya kolom poin, bobot, nilai, maupun SKS &mdash;
 * dan begitu pula {@link JabatanKegiatanKedosenan}, {@link SkalaKegiatanKedosenan}, serta
 * {@link KegiatanKedosenanPunyaDosen}. Bobot SKS disimpan <b>di luar model relasional modul
 * ini</b>, sebagai baris {@link ParameterUmum} (tabel key-value konfigurasi) dengan kunci string
 * yang <i>dirakit dari ID-ID master</i>:
 * </p>
 * <pre>
 * pengaturan_beban_sks_kegiatan_dosen_&lt;idKelompok&gt;_&lt;idDetail&gt;_&lt;idSkala&gt;_&lt;idJabatan&gt;
 * </pre>
 * <p>
 * dengan {@code idDetail} adalah {@link #getId()} baris ini dan {@code idKelompok} adalah
 * {@code getKelompokKegiatanKedosenan().getId()}. Bila jabatan tidak diisi, segmen terakhir
 * dibiarkan kosong sehingga kunci berakhir dengan garis bawah.
 * </p>
 * <ul>
 *   <li><b>Penulis nilai</b> &mdash; layar master "Nilai Kegiatan Kedosenan"
 *       ({@code ais.action.master.NilaiKegiatanKedosenanAction}, halaman
 *       {@code nilai_kegiatan_kedosenan.zul}): merender matriks rincian &times; skala per jabatan
 *       dan menyimpan angka yang diketik langsung ke baris {@link ParameterUmum} berkunci di
 *       atas. Layar yang sama juga menyediakan unduh/unggah matriks dalam bentuk Excel.</li>
 *   <li><b>Pembaca nilai</b> &mdash; {@code ais.action.master.bkd.helper.BkdKegiatanDosenHelper}:
 *       saat satu {@link KegiatanKedosenanPunyaDosen} disetujui, helper ini merakit kunci yang
 *       sama, membaca nilainya dengan {@code Common.getParameterUmum(kunci, "0.0")},
 *       mem-parsing-nya menjadi {@code double}, lalu menuliskannya ke
 *       {@code AsesemenPenilaian.setSks(...)} untuk dinilai asesor BKD.</li>
 * </ul>
 * <p>
 * Konsekuensi penting dari desain "kunci dirakit dari ID": bobot <b>tidak ikut ter-cascade dan
 * tidak ikut teraudit Envers</b> bersama baris ini. Memindahkan rincian ini ke kelompok induk
 * lain lewat {@link #setKelompokKegiatanKedosenan(KelompokKegiatanKedosenan)} mengubah segmen
 * {@code idKelompok} pada kunci, sehingga seluruh bobot yang pernah dikonfigurasi menjadi tidak
 * terjangkau dan pembacaan berikutnya jatuh ke default {@code "0.0"} tanpa peringatan apa pun.
 * Menghapus baris ini juga meninggalkan baris {@link ParameterUmum} yatim yang tidak pernah
 * dibersihkan.
 * </p>
 *
 * <h2>Pengelompokan anggota kelas</h2>
 * <ul>
 *   <li><b>Identitas &amp; deskripsi</b>: {@link #getId()}, {@link #getNama()},
 *       {@link #getNomorUrut()}, {@link #toString()}.</li>
 *   <li><b>Relasi induk</b>: {@link #getKelompokKegiatanKedosenan()}.</li>
 *   <li><b>Relasi pilihan (many-to-many)</b>: {@link #getJabatanKegiatanKedosenans()},
 *       {@link #getSkalaKegiatanKedosenans()}.</li>
 *   <li><b>Bendera ketersediaan</b>: {@link #getAktif()}, {@link #getBisaDipilihDosen()}.</li>
 *   <li><b>Jejak audit</b>: {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()}, dan callback {@code @PreUpdate} {@code onUpdate()}.</li>
 * </ul>
 *
 * <h2>Catatan teknis yang tidak kelihatan dari kode</h2>
 * <ul>
 *   <li><b>Field audit sengaja dideklarasikan ulang.</b> {@link GeneralValueObject} adalah POJO
 *       abstrak biasa &mdash; BUKAN {@code @Entity} maupun {@code @MappedSuperclass} &mdash;
 *       sehingga Hibernate tidak memetakan properti milik induk. Deklarasi ulang
 *       {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} (juga {@code nama} dan
 *       {@code nomorUrut}) di kelas ini adalah <b>keharusan teknis pemetaan</b>, bukan duplikasi
 *       yang keliru.</li>
 *   <li><b>Bahaya {@code TreeSet} pada kedua koleksi.</b> Nilai awal kedua field koleksi adalah
 *       {@code new TreeSet<>()}, sedangkan urutan alaminya berasal dari
 *       {@code GeneralValueObject.compareTo(...)} yang membandingkan {@code nomorUrut} LEBIH
 *       DAHULU dan berhenti di situ. Karena {@code getNomorUrut()} pada
 *       {@link JabatanKegiatanKedosenan}/{@link SkalaKegiatanKedosenan} mengembalikan {@code 1}
 *       bila kolomnya kosong, dua master dengan nomor urut sama dianggap "sama" oleh
 *       {@code TreeSet} dan yang kedua DITOLAK diam-diam. Layar master rincian aspek
 *       ({@code DetailKelompokKegiatanKedosenanHelper}) sudah menghindari jebakan ini dengan
 *       memakai {@code LinkedHashMap}/{@code LinkedHashSet}; kode lain yang membungkus ulang
 *       hasil getter ke dalam {@code TreeSet} tidak terlindungi. Lihat catatan pada
 *       {@link #getJabatanKegiatanKedosenans()}.</li>
 *   <li><b>{@link #getBisaDipilihDosen()} bersifat mutatif</b> &mdash; ia menulis {@code false}
 *       ke field saat kelompok induk sudah ditutup. Rinciannya ada di Javadoc method tersebut.</li>
 *   <li>Berbeda dari banyak entity master modul ini, kelas ini <b>tidak</b> punya kolom/getter
 *       {@code keterangan} sendiri.</li>
 *   <li>Perubahan baris diaudit Hibernate Envers ({@code @Audited}); {@code dynamicInsert}/
 *       {@code dynamicUpdate} membuat pernyataan SQL hanya memuat kolom yang benar-benar
 *       berubah.</li>
 * </ul>
 *
 * @see KelompokKegiatanKedosenan
 * @see KegiatanKedosenan
 * @see KegiatanKedosenanPunyaDosen
 * @see JabatanKegiatanKedosenan
 * @see SkalaKegiatanKedosenan
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "detail_kelompok_kegiatan_kedosenan")

public class DetailKelompokKegiatanKedosenan extends GeneralValueObject {
	/** Versi serialisasi kelas; jangan diubah agar objek yang sudah ter-cache tetap terbaca. */
	private static final long serialVersionUID = -7050166125892447098L;
	/** Kunci utama tabel ({@code id}, IDENTITY). Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini. Lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini. Dideklarasikan ulang di sini
	 * (bukan diwarisi) karena {@link GeneralValueObject} bukan {@code @MappedSuperclass}.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir. Nilai {@code null} atau string kosong/whitespace
	 * <b>diabaikan diam-diam</b> sehingga jejak audit yang sudah ada tidak tertimpa nilai hampa.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null}/kosong diabaikan diam-diam.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini. Dideklarasikan ulang dengan
	 * alasan yang sama seperti {@link #getOlehId()}.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil Hibernate TEPAT SEBELUM setiap {@code UPDATE}
	 * baris ini, lalu meneruskan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #setOleh(String)}, {@link #setOlehId(String)}, dan {@link #setTanggal_dirubah(Date)}
	 * dari konteks pengguna yang sedang login.
	 *
	 * <p><b>Efek samping:</b> memodifikasi state objek di tengah siklus flush. Jangan dipanggil
	 * manual dari kode aplikasi &mdash; Hibernate yang memicunya.</p>
	 *
	 * <p>Hanya {@code @PreUpdate} yang dipasang; baris BARU ({@code INSERT}) tidak melewati
	 * callback ini, sehingga rincian aspek yang baru dibuat lewat layar master
	 * ({@code DetailKelompokKegiatanKedosenanHelper}) menyimpan jejak audit hanya bila jalur
	 * penyimpanan bersama ({@code Common.refreshSaveOrUpdate}) yang mengisinya.</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja berada pada baris fisik yang sama dalam
	 * kode aslinya; nilainya diinisialisasi memakai jam aplikasi
	 * ({@code ais.ui.util.WaktuUtil.getDate()}), bukan {@code new Date()}, agar konsisten dengan
	 * zona waktu/offset yang dipakai seluruh modul.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir baris ini. Umumnya dipanggil oleh
	 * {@code AuditTimestampInterceptor} lewat {@code onUpdate()}, bukan oleh kode aplikasi.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (dipetakan sebagai
	 * {@code TIMESTAMP}). Selalu terisi untuk objek baru karena field-nya diinisialisasi saat
	 * konstruksi.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini dalam format {@code "<id>-<nama>"}.
	 *
	 * <p>Meng-override {@code GeneralValueObject.toString()} yang berformat {@code "kode - nama"}.
	 * Dipakai antara lain sebagai label komponen ZK dan pesan diagnostik. Untuk baris yang belum
	 * tersimpan, bagian {@code id} akan tercetak sebagai {@code "null"}.</p>
	 *
	 * @return gabungan id dan nama rincian aspek
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Aspek/kelompok induk pemilik rincian ini. Lihat {@link #getKelompokKegiatanKedosenan()}. */
	private KelompokKegiatanKedosenan kelompokKegiatanKedosenan;
	/** Nama rincian aspek yang tampil di layar. Lihat {@link #getNama()}. */
	private String nama;
	/** Nomor urut tampil di dalam kelompok induk. Lihat {@link #getNomorUrut()}. */
	private Integer nomorUrut;
	/** Bendera baris masih dipakai. Lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Bendera rincian boleh dipilih dosen sendiri. Lihat {@link #getBisaDipilihDosen()}. */
	private Boolean bisaDipilihDosen;

	/**
	 * Daftar jabatan/peran yang diizinkan untuk rincian ini.
	 *
	 * <p><b>Hati-hati:</b> nilai awalnya {@code TreeSet}, yang menciutkan anggota ber-nomor urut
	 * sama &mdash; lihat penjelasan pada Javadoc kelas dan
	 * {@link #getJabatanKegiatanKedosenans()}.</p>
	 */
	private Set<JabatanKegiatanKedosenan> jabatanKegiatanKedosenans = new TreeSet<JabatanKegiatanKedosenan>();

	/**
	 * Mengembalikan himpunan {@link JabatanKegiatanKedosenan} (peran dosen: ketua, anggota,
	 * narasumber, dan seterusnya) yang boleh dipilih untuk rincian aspek ini. Dipetakan
	 * many-to-many lewat tabel penghubung
	 * {@code detail_kelompok_has_jabatan_kegiatan_kedosenan} dengan kolom {@code detail_kelompok}
	 * (sisi ini) dan {@code jabatan_kegiatan_kedosenan} (sisi lawan); cascade
	 * {@code MERGE}/{@code PERSIST} sehingga menyimpan rincian ini ikut menyimpan master jabatan
	 * yang belum tersimpan.
	 *
	 * <p>Himpunan ini dipakai sebagai daftar centang di layar master rincian aspek, sebagai
	 * sumber baris matriks di layar "Nilai Kegiatan Kedosenan", dan &mdash; bersama
	 * {@link #getSkalaKegiatanKedosenans()} &mdash; menentukan kombinasi kunci
	 * {@link ParameterUmum} tempat bobot SKS disimpan (lihat Javadoc kelas).</p>
	 *
	 * <p><b>Kuirk yang harus diketahui pemanggil:</b> jangan membungkus ulang hasil method ini ke
	 * dalam {@code TreeSet}. Urutan alami {@link GeneralValueObject} membandingkan
	 * {@code nomorUrut} lebih dahulu dan mengembalikan {@code 0} bila sama, sedangkan
	 * {@code JabatanKegiatanKedosenan.getNomorUrut()} mengembalikan {@code 1} bila kolomnya
	 * kosong. Akibatnya seluruh jabatan yang nomor urutnya belum diisi akan menciut menjadi SATU
	 * anggota saja dan sisanya hilang diam-diam dari hasil olahan. Gunakan koleksi berbasis urutan
	 * sisip ({@code LinkedHashSet}) atau {@code Map} berkunci id, seperti yang dilakukan
	 * {@code DetailKelompokKegiatanKedosenanHelper}.</p>
	 *
	 * <p>Bila objek ini terlepas dari session Hibernate, mengakses isi himpunan dapat memicu
	 * {@code LazyInitializationException}.</p>
	 *
	 * @return himpunan jabatan yang diizinkan; tidak pernah {@code null}, bisa kosong
	 */
	@ManyToMany(targetEntity = JabatanKegiatanKedosenan.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@JoinTable(name = "detail_kelompok_has_jabatan_kegiatan_kedosenan", joinColumns = @JoinColumn(name = "detail_kelompok"), inverseJoinColumns = @JoinColumn(name = "jabatan_kegiatan_kedosenan"))
	public Set<JabatanKegiatanKedosenan> getJabatanKegiatanKedosenans() {
		return jabatanKegiatanKedosenans;
	}

	/**
	 * Mengganti seluruh himpunan jabatan yang diizinkan untuk rincian ini. Dipanggil Hibernate
	 * saat memuat baris, dan oleh layar master saat tombol "Simpan" ditekan (di sana himpunan
	 * baru dibentuk sebagai {@code LinkedHashSet} untuk menghindari penciutan {@code TreeSet}).
	 *
	 * <p><b>Efek samping:</b> mengganti referensi koleksi pada entity terkelola membuat Hibernate
	 * menyinkronkan tabel penghubung &mdash; baris yang tidak lagi ada akan dihapus. Bobot SKS
	 * pada {@link ParameterUmum} untuk kombinasi yang dibuang TIDAK ikut dihapus dan akan kembali
	 * terpakai apabila jabatan yang sama dicentang lagi.</p>
	 *
	 * @param jabatanKegiatanKedosenans himpunan jabatan yang baru
	 */
	public void setJabatanKegiatanKedosenans(Set<JabatanKegiatanKedosenan> jabatanKegiatanKedosenans) {
		this.jabatanKegiatanKedosenans = jabatanKegiatanKedosenans;
	}

	/**
	 * Daftar skala/tingkat yang diizinkan untuk rincian ini.
	 *
	 * <p>Sama seperti {@link #jabatanKegiatanKedosenans}, nilai awalnya {@code TreeSet} dengan
	 * risiko penciutan yang sama.</p>
	 */
	private Set<SkalaKegiatanKedosenan> skalaKegiatanKedosenans = new TreeSet<SkalaKegiatanKedosenan>();

	/**
	 * Mengembalikan himpunan {@link SkalaKegiatanKedosenan} (tingkat kegiatan: jurusan, fakultas,
	 * institusi, nasional, internasional, dan seterusnya) yang boleh dipilih untuk rincian aspek
	 * ini. Dipetakan many-to-many lewat tabel penghubung
	 * {@code detail_kelompok_has_skala_kegiatan_kedosenan} dengan kolom {@code detail_kelompok}
	 * (sisi ini) dan {@code skala_kegiatan_kedosenan} (sisi lawan); cascade
	 * {@code MERGE}/{@code PERSIST}.
	 *
	 * <p>Di layar "Nilai Kegiatan Kedosenan" himpunan ini menjadi <b>kolom</b> matriks bobot,
	 * sedangkan {@link #getJabatanKegiatanKedosenans()} menjadi barisnya; setiap sel matriks
	 * adalah satu baris {@link ParameterUmum}.</p>
	 *
	 * <p><b>Kuirk yang sama berlaku:</b> jangan membungkus hasilnya ke {@code TreeSet}, karena
	 * {@code SkalaKegiatanKedosenan.getNomorUrut()} juga mengembalikan {@code 1} bila kolomnya
	 * kosong sehingga skala-skala tersebut saling menghapus. Akses di luar session dapat memicu
	 * {@code LazyInitializationException}.</p>
	 *
	 * @return himpunan skala yang diizinkan; tidak pernah {@code null}, bisa kosong
	 */
	@ManyToMany(targetEntity = SkalaKegiatanKedosenan.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@JoinTable(name = "detail_kelompok_has_skala_kegiatan_kedosenan", joinColumns = @JoinColumn(name = "detail_kelompok"), inverseJoinColumns = @JoinColumn(name = "skala_kegiatan_kedosenan"))
	public Set<SkalaKegiatanKedosenan> getSkalaKegiatanKedosenans() {
		return skalaKegiatanKedosenans;
	}

	/**
	 * Mengganti seluruh himpunan skala yang diizinkan untuk rincian ini. Berperilaku dan
	 * berkonsekuensi persis seperti
	 * {@link #setJabatanKegiatanKedosenans(Set)}: Hibernate menyinkronkan tabel penghubung,
	 * sedangkan bobot SKS pada {@link ParameterUmum} untuk kombinasi yang dibuang tetap tertinggal
	 * di basis data.
	 *
	 * @param skalaKegiatanKedosenans himpunan skala yang baru
	 */
	public void setSkalaKegiatanKedosenans(Set<SkalaKegiatanKedosenan> skalaKegiatanKedosenans) {
		this.skalaKegiatanKedosenans = skalaKegiatanKedosenans;
	}

	/**
	 * Mengembalikan kunci utama baris ini (kolom {@code id}, dibangkitkan basis data dengan
	 * strategi {@code IDENTITY} sehingga {@code insertable = false}).
	 *
	 * <p>Nilai ini bukan sekadar identitas teknis: ia ikut dirakit menjadi segmen
	 * {@code <idDetail>} pada kunci {@link ParameterUmum} tempat bobot SKS kegiatan disimpan
	 * (lihat Javadoc kelas), sehingga id baris rincian aspek efektif menjadi bagian dari
	 * konfigurasi BKD.</p>
	 *
	 * @return id baris, atau {@code null} bila objek belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris ini. Umumnya hanya dipanggil Hibernate.
	 *
	 * @param id id baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama rincian aspek apa adanya (tanpa pemangkasan spasi, berbeda dari
	 * {@code getNama()} pada beberapa entity master sekerabat). Dipetakan ke kolom bertipe
	 * {@code text} sehingga panjangnya tidak dibatasi.
	 *
	 * <p>Nama ini tampil di grid layar master, di matriks bobot, di judul kolom rekap dasbor, dan
	 * ikut dirangkai ke dalam kalimat keterangan asesmen BKD.</p>
	 *
	 * @return nama rincian aspek, atau {@code null} bila belum diisi
	 */
	@Column(columnDefinition = "text")
	public String getNama() {
		return nama;
	}

	/**
	 * Menyetel nama rincian aspek. Tanpa validasi di sini; layar master yang memastikan nama tidak
	 * kosong dan sudah dipangkas spasinya sebelum menyimpan.
	 *
	 * @param nama nama rincian aspek yang baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Menyetel aspek/kelompok induk pemilik rincian ini.
	 *
	 * <p><b>Efek samping yang mudah terlewat:</b> id kelompok induk adalah segmen pertama pada
	 * kunci {@link ParameterUmum} tempat bobot SKS disimpan. Memindahkan rincian ini ke kelompok
	 * lain otomatis mengubah seluruh kunci tersebut, sehingga bobot yang sudah dikonfigurasi
	 * menjadi tidak terbaca lagi dan pembacaan berikutnya jatuh ke default {@code 0.0} tanpa
	 * pesan kesalahan. Bobot harus diisi ulang lewat layar "Nilai Kegiatan Kedosenan".</p>
	 *
	 * @param kelompokKegiatanKedosenan kelompok induk yang baru; boleh {@code null} karena kolom
	 *                                   {@code kelompok_kegiatan_kedosenan} nullable
	 */
	public void setKelompokKegiatanKedosenan(KelompokKegiatanKedosenan kelompokKegiatanKedosenan) {
		this.kelompokKegiatanKedosenan = kelompokKegiatanKedosenan;
	}

	/**
	 * Mengembalikan aspek/kelompok induk pemilik rincian ini (relasi many-to-one lazy ke kolom
	 * {@code kelompok_kegiatan_kedosenan}, cascade {@code PERSIST}/{@code MERGE}).
	 *
	 * <p>Sebelum dikembalikan, nilainya dilewatkan {@code check(...)} milik
	 * {@link GeneralValueObject} untuk meresolusi proxy lazy Hibernate menjadi objek nyata, lalu
	 * <b>hasil resolusi itu ditulis balik ke field</b>. Penulisan balik ini hanya mengganti proxy
	 * dengan instance yang setara, jadi tidak mengubah nilai kolom di basis data, tetapi tetap
	 * berarti method ini bukan getter murni.</p>
	 *
	 * <p>Kolom sengaja dibuat {@code nullable = true} pada pemetaan, sehingga pemanggil wajib
	 * bersiap menerima {@code null}. Beberapa jalur pemanggil di modul ini (mis. perakitan kunci
	 * bobot SKS dan renderer matriks nilai) tidak memeriksanya dan akan melempar
	 * {@code NullPointerException} untuk baris rincian yatim.</p>
	 *
	 * @return kelompok kegiatan kedosenan induk, atau {@code null} bila baris tidak terkait
	 *         kelompok mana pun
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_kegiatan_kedosenan", nullable = true)
	public KelompokKegiatanKedosenan getKelompokKegiatanKedosenan() {
		kelompokKegiatanKedosenan = check(kelompokKegiatanKedosenan);
		return kelompokKegiatanKedosenan;
	}

	/**
	 * Mengembalikan nomor urut tampil rincian ini di dalam kelompok induknya, dengan default
	 * {@code 1} bila kolomnya masih kosong (sehingga tidak pernah mengembalikan {@code null}).
	 *
	 * <p>Dipakai sebagai kunci pengurutan grid layar master dan laporan. <b>Sekaligus menjadi
	 * kunci pembanding PERTAMA</b> pada {@code GeneralValueObject.compareTo(...)}: karena default
	 * {@code 1} membuat nilai ini selalu non-null, dua rincian aspek dengan nomor urut sama akan
	 * dianggap identik oleh {@code TreeSet}/{@code TreeMap} dan salah satunya hilang diam-diam.
	 * Layar master mengusulkan nomor urut berikutnya secara otomatis, tetapi nilainya bebas
	 * diubah pengguna sehingga duplikasi tetap mungkin terjadi.</p>
	 *
	 * @return nomor urut tampil; {@code 1} bila kolom kosong
	 */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampil rincian ini. Disimpan apa adanya; tidak ada penjaminan keunikan
	 * di dalam satu kelompok induk.
	 *
	 * @param nomorUrut nomor urut baru; boleh {@code null} (dibaca sebagai {@code 1})
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Mengembalikan bendera "rincian ini masih dipakai", dengan default {@code true} bila kolomnya
	 * masih kosong &mdash; artinya baris lama yang belum pernah disentuh dianggap AKTIF.
	 *
	 * <p>Bendera ini menyaring rincian yang boleh muncul di layar pemilihan kegiatan. Perhatikan
	 * bahwa nilainya tidak menular ke bawah: menonaktifkan rincian tidak menghapus bobot SKS yang
	 * sudah tersimpan pada {@link ParameterUmum} dan tidak membatalkan
	 * {@link KegiatanKedosenanPunyaDosen} yang terlanjur menunjuk rincian ini.</p>
	 *
	 * @return {@code true} bila rincian masih aktif; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel bendera aktif rincian ini. Di layar master, mencentang/melepas kotak ini langsung
	 * memicu penyimpanan baris.
	 *
	 * @param aktif {@code true} bila rincian masih dipakai; {@code null} dibaca sebagai
	 *              {@code true}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan bendera "rincian ini boleh dipilih sendiri oleh dosen" saat mengajukan
	 * kegiatan kedosenan. Default {@code true} bila kolomnya masih kosong.
	 *
	 * <p><b>Bukan getter murni.</b> Sebelum mengembalikan nilai, method ini memeriksa kelompok
	 * induk: bila induknya ada dan sudah tidak bisa dipilih dosen, field
	 * {@code bisaDipilihDosen} pada objek ini <b>ditulis ulang menjadi {@code false}</b>.
	 * Konsekuensinya, sekadar MEMBACA properti ini pada entity yang masih terkelola Hibernate
	 * membuat objek menjadi kotor, sehingga flush berikutnya menerbitkan {@code UPDATE} ke kolom
	 * tersebut &mdash; dan karena kelas ini {@code @Audited}, revisi Envers ikut tercatat seolah
	 * ada pengguna yang mengubahnya. Penurunan nilai ini bersifat searah: menyalakan kembali
	 * kelompok induk TIDAK mengembalikan nilai {@code true} yang sudah tertulis, dan
	 * pengaturan per-rincian yang semula {@code true} hilang permanen.</p>
	 *
	 * <p>Pemeriksaan memakai field {@code kelompokKegiatanKedosenan} secara langsung, bukan lewat
	 * {@link #getKelompokKegiatanKedosenan()}, sehingga proxy lazy tidak diresolusi terlebih
	 * dahulu: pada objek yang sudah terlepas dari session, baris ini dapat melempar
	 * {@code LazyInitializationException}. Sebaliknya, bila relasi induk belum pernah dimuat dan
	 * bernilai {@code null} di memori, penurunan nilai tidak terjadi sama sekali &mdash; hasil
	 * method ini karena itu bisa berbeda antara satu pemuatan dan pemuatan lain untuk baris yang
	 * sama.</p>
	 *
	 * @return {@code true} bila dosen boleh memilih rincian ini sendiri; tidak pernah {@code null}
	 */
	public Boolean getBisaDipilihDosen() {
		if (kelompokKegiatanKedosenan != null && !kelompokKegiatanKedosenan.getBisaDipilihDosen()) {
			bisaDipilihDosen = false;
		}
		return bisaDipilihDosen == null ? true : bisaDipilihDosen;
	}

	/**
	 * Menyetel bendera "boleh dipilih dosen" untuk rincian ini. Nilai yang disimpan di sini dapat
	 * diturunkan kembali menjadi {@code false} oleh {@link #getBisaDipilihDosen()} bila kelompok
	 * induk sudah ditutup.
	 *
	 * @param bisaDipilihDosen {@code true} bila dosen boleh memilih rincian ini; {@code null}
	 *                          dibaca sebagai {@code true}
	 */
	public void setBisaDipilihDosen(Boolean bisaDipilihDosen) {
		this.bisaDipilihDosen = bisaDipilihDosen;
	}

}
