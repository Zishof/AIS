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

import org.hibernate.Hibernate;
import org.hibernate.envers.Audited;

/**
 * Entitas Hibernate untuk tabel {@code public.detail_kelompok_kegiatan_kemahasiswaan}: satu
 * <b>rincian aspek</b> kegiatan kemahasiswaan (mis. "Lomba Karya Tulis Ilmiah", "Pengurus
 * Harian UKM") di bawah satu <i>aspek</i>/kelompok kegiatan yang lebih umum. Ini adalah master
 * data murni pada modul aktivitas/prestasi mahasiswa &mdash; tidak menunjuk mahasiswa maupun
 * kegiatan tertentu.
 *
 * <h2>Posisi dalam rantai master kegiatan kemahasiswaan</h2>
 *
 * <p>Rantai lengkapnya, dari yang paling umum ke yang paling konkret:</p>
 *
 * <ol>
 *   <li><b>{@code KelompokKegiatanKemahasiswaan}</b> (tabel
 *       {@code kelompok_kegiatan_kemahasiswaan}) &mdash; aspek/kelompok terluar, mis.
 *       "Organisasi", "Kompetisi", "Kepanitiaan". Induk langsung baris ini, lihat
 *       {@link #getKelompokKegiatanKemahasiswaan()};</li>
 *   <li><b>kelas ini</b> &mdash; rincian aspek di bawah kelompok tersebut, sekaligus penentu
 *       <i>kombinasi mana saja</i> yang sah untuk rincian itu (lihat bagian berikutnya);</li>
 *   <li>{@link ais.database.model.NilaiKegiatanKemahasiswaan} &mdash; <b>rubrik/tarif</b>: bobot
 *       angka kredit untuk satu kombinasi rincian &times; skala &times; jabatan;</li>
 *   <li>{@link ais.database.model.KegiatanKemahasiswaan} &mdash; peristiwanya: satu lomba atau
 *       kepengurusan yang benar-benar diselenggarakan, menunjuk balik ke satu baris kelas ini
 *       lewat kolom {@code detail_kelompok_kegiatan_kemahasiswaan};</li>
 *   <li>{@link ais.database.model.KegiatanKemahasiswaanPunyaMahasiswa} &mdash; keikutsertaan satu
 *       mahasiswa pada satu kegiatan, lengkap dengan jabatan dan skala yang diakui baginya.</li>
 * </ol>
 *
 * <h2>Bobot/poin TIDAK didefinisikan di sini</h2>
 *
 * <p><b>Kelas ini sama sekali tidak punya kolom poin, bobot, nilai, SKPI, maupun SKKM.</b> Yang
 * disimpannya hanyalah identitas rincian ({@link #getNama()}), urutan tampil
 * ({@link #getNomorUrut()}), dua bendera kelayakan ({@link #getAktif()},
 * {@link #getBisaDipilihMahasiswa()}), dan <b>dua himpunan many-to-many</b> yang berfungsi
 * sebagai <i>sumbu</i> matriks penilaian:</p>
 *
 * <ul>
 *   <li>{@link #getJabatanKegiatanKemahasiswaans()} &rarr; tabel penghubung
 *       {@code detail_kelompok_has_jabatan_kegiatan_kemahasiswaan}: peran/jabatan apa saja yang
 *       relevan untuk rincian ini (ketua, anggota, panitia, peserta&hellip;);</li>
 *   <li>{@link #getSkalaKegiatanKemahasiswaans()} &rarr; tabel penghubung
 *       {@code detail_kelompok_has_skala_kegiatan_kemahasiswaan}: tingkat penyelenggaraan apa saja
 *       yang berlaku (prodi, universitas, nasional, internasional&hellip;).</li>
 * </ul>
 *
 * <p>Angkanya sendiri ada di entity terpisah {@link ais.database.model.NilaiKegiatanKemahasiswaan}
 * (tabel {@code nilai_kegiatan_kemahasiswaan}), satu baris per kombinasi, dengan kunci gabungan
 * string {@code "<idRincian>-<idSkala>-<idJabatan>"}. Jadi kelas ini <b>mendefinisikan bentuk
 * matriksnya</b> (baris = jabatan, kolom = skala), sedangkan isi selnya ditulis di entity rubrik.
 * Layar pengelola {@code NilaiKegiatanKemahasiswaanAction} membangun matriks itu persis dengan
 * cara demikian: iterasi {@code initCriteria()} atas kelas ini, lalu untuk tiap perpotongan
 * jabatan&times;skala yang <i>termasuk</i> dalam kedua himpunan di atas dibuat satu kotak angka.
 * Rincian yang himpunan jabatannya kosong tetap dapat dinilai &mdash; ia menghasilkan satu baris
 * dengan segmen jabatan kosong pada kode gabungan ({@code "42-3-"}), artinya "berlaku untuk semua
 * peran".</p>
 *
 * <p><b>Konsekuensi yang perlu disadari:</b> penjumlah angka kredit
 * ({@code ais.common.Common#hitungAngkaKredit(Mahasiswa)}) adalah SQL native yang mem-<i>join</i>
 * {@code nilai_kegiatan_kemahasiswaan} memakai id rincian (dari kegiatannya) plus jabatan dan
 * skala (dari baris keikutsertaan mahasiswa). Query itu <b>tidak</b> memeriksa apakah jabatan/skala
 * tersebut benar-benar anggota kedua himpunan di kelas ini. Dengan kata lain kedua many-to-many di
 * sini adalah pembatas <i>tampilan/entri</i>, bukan kendala integritas: bila kombinasi di luar
 * daftar sempat masuk (mis. lewat impor, atau karena keanggotaan dicabut setelah kegiatan
 * berjalan), barisnya tidak menyumbang poin apa pun tetapi juga tidak memunculkan galat.</p>
 *
 * <h2>Hal non-obvious lainnya</h2>
 *
 * <ul>
 *   <li><b>{@link #getBisaDipilihMahasiswa()} adalah getter yang menulis balik</b> (lihat
 *       Javadoc method-nya) &mdash; ia dapat mengubah field dan, karena kelas ini memakai
 *       <i>property access</i>, perubahan itu ikut tersimpan ke basis data pada flush berikutnya
 *       tanpa ada yang menekan tombol simpan.</li>
 *   <li><b>Pewarisan bendera tidak berlaku di tingkat query.</b> Penurunan nilai dari kelompok
 *       induk hanya terjadi di Java, di dalam getter tersebut. Penyaring yang dipakai layar entri
 *       kegiatan ({@code KegiatanKemahasiswaanAction}) adalah {@code Restrictions} atas
 *       <i>kolom</i>, sehingga sebuah rincian baru benar-benar hilang dari daftar pilih mahasiswa
 *       setelah kolomnya sempat ditulis-balik.</li>
 *   <li><b>Koleksi diinisialisasi sebagai {@link TreeSet}</b>, yang mengurutkan memakai
 *       {@code compareTo} dari {@link ais.database.model.GeneralValueObject} &mdash; kunci urut
 *       pertamanya {@code nomorUrut}, dan mengembalikan {@code 0} bila dua item bernomor urut
 *       sama. Untuk instance transient (belum tersimpan) itu berarti dua jabatan/skala bernomor
 *       urut identik saling menimpa dan tidak semuanya tersimpan. Kode pemakai sudah mengakalinya:
 *       {@code DetailKelompokKegiatanKemahasiswaanHelper} sengaja memakai
 *       {@link java.util.LinkedHashMap}/{@link java.util.List}, bukan {@code TreeSet}, saat
 *       menyusun pilihan dan urutan tampil.</li>
 *   <li><b>Field {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah}
 *       dideklarasikan ulang di sini padahal ada juga di
 *       {@link ais.database.model.GeneralValueObject}.</b> Itu <b>bukan</b> duplikasi keliru,
 *       melainkan keharusan teknis: kelas induk hanyalah POJO abstrak biasa (bukan
 *       {@code @Entity} maupun {@code @MappedSuperclass}), sehingga Hibernate tidak memetakan
 *       properti miliknya. Tanpa deklarasi ulang, keempat kolom itu tidak akan tersimpan.</li>
 *   <li><b>Tidak ada akses database langsung di kelas ini</b> &mdash; tidak ada method utilitas
 *       maupun query statis. Seluruh pencarian/penyimpanan dilakukan pemanggil (Action dan
 *       Helper) lewat {@code Criteria} Hibernate.</li>
 *   <li>Kelas ini <b>tidak</b> punya properti {@code keterangan}; kolom keterangan ada di entity
 *       kelompok induk serta di entity jabatan dan skala, bukan di sini.</li>
 * </ul>
 *
 * <p>Perubahan (create/update) tercatat historisnya lewat anotasi {@link Audited} (Hibernate
 * Envers), dan setiap update otomatis memperbarui {@link #getTanggal_dirubah()} lewat callback
 * {@link javax.persistence.PreUpdate} yang memanggil
 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.NilaiKegiatanKemahasiswaan
 * @see ais.database.model.KegiatanKemahasiswaan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "detail_kelompok_kegiatan_kemahasiswaan")

public class DetailKelompokKegiatanKemahasiswaan extends GeneralValueObject {
	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Nilainya dibagi bersama sejumlah entity lain di paket ini akibat salin-tempel dari
	 * berkas hasil {@code hbm2java}. Tidak berbahaya: nilai ini hanya dipakai saat
	 * (de)serialisasi Java biasa, dan entity di aplikasi ini tidak pernah dipertukarkan
	 * antar-versi lewat jalur tersebut.</p>
	 */
	private static final long serialVersionUID = -7050166125892447098L;

	/** Kunci utama (kolom {@code id}), dibangkitkan basis data lewat {@code IDENTITY}. */
	private Long id;

	/**
	 * Nama pengguna yang terakhir mengubah baris ini (kolom {@code oleh}).
	 *
	 * <p>Dideklarasikan ulang dari {@link ais.database.model.GeneralValueObject} karena kelas
	 * induk tidak dipetakan Hibernate &mdash; lihat Javadoc kelas.</p>
	 */
	private String oleh;

	/**
	 * Id pengguna yang terakhir mengubah baris ini (kolom {@code olehId}).
	 *
	 * <p>Dideklarasikan ulang dari {@link ais.database.model.GeneralValueObject} karena kelas
	 * induk tidak dipetakan Hibernate &mdash; lihat Javadoc kelas.</p>
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna pengubah terakhir.
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah diubah lewat jalur beraudit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatikan:</b> setter ini <b>mengabaikan diam-diam</b> argumen {@code null} maupun
	 * string kosong/hanya spasi &mdash; nilai lama dipertahankan, bukan ditimpa. Konsekuensinya
	 * jejak audit tidak bisa dikosongkan lewat setter ini, dan proses yang berjalan tanpa konteks
	 * pengguna (job terjadwal, impor) meninggalkan nama pengubah <i>sebelumnya</i> apa adanya.</p>
	 *
	 * <p><b>Dipanggil oleh:</b> {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}, dan
	 * Hibernate saat memuat baris dari basis data.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna pengubah terakhir.
	 *
	 * <p>Berperilaku sama dengan {@link #setOlehId(String)}: argumen {@code null} atau string
	 * kosong/hanya spasi diabaikan diam-diam sehingga nilai lama bertahan.</p>
	 *
	 * <p><b>Dipanggil oleh:</b> {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}, dan
	 * Hibernate saat memuat baris dari basis data.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna pengubah terakhir.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mencatat jejak audit tepat sebelum {@code UPDATE} dikirim.
	 *
	 * <p>Mendelegasikan ke {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)},
	 * yang mengisi {@link #setOleh(String)}, {@link #setOlehId(String)}, dan
	 * {@link #setTanggal_dirubah(Date)} dari konteks pengguna yang sedang aktif.</p>
	 *
	 * <p><b>Dipanggil oleh:</b> provider JPA/Hibernate saja &mdash; jangan dipanggil manual.
	 * Hanya {@code @PreUpdate} yang dipasang; pada {@code INSERT} pertama nilai audit bergantung
	 * pada apa yang diisi pemanggil (atau interceptor global), bukan pada method ini. Untuk kelas
	 * ini hal tersebut cukup terasa, karena {@link #getBisaDipilihMahasiswa()} bisa memicu
	 * {@code UPDATE} yang tidak diminta siapa pun (lihat Javadoc method itu) sehingga baris audit
	 * yang tercipta menunjuk pengguna yang kebetulan sedang membuka layar, bukan yang mengubah
	 * kebijakan. Perubahan lengkap per revisi tetap terekam Envers karena kelas ini
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
	 * Mengembalikan waktu perubahan terakhir baris ini.
	 *
	 * @return waktu perubahan terakhir; untuk instance baru berisi waktu pembuatan object, bukan
	 *         {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Mengembalikan representasi teks {@code "<id>-<nama>"}.
	 *
	 * <p>Membaca <i>field</i> {@code nama} secara langsung, bukan lewat {@link #getNama()}, jadi
	 * aman dipanggil dari mana pun. Untuk instance yang belum tersimpan hasilnya berawalan
	 * {@code "null-"}.</p>
	 *
	 * <p><b>Dipanggil oleh:</b> komponen ZK yang menampilkan entity apa adanya (mis. isi
	 * {@code Combobox}/{@code Listbox} tanpa renderer khusus) dan pesan log/galat.</p>
	 *
	 * @return {@code "<id>-<nama>"}; kedua bagian bisa berupa teks {@code "null"}
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Kelompok/aspek kegiatan induk (kolom FK {@code kelompok_kegiatan_kemahasiswaan}).
	 *
	 * <p>Dipetakan {@code nullable = true} sehingga secara skema baris yatim mungkin ada, walau
	 * seluruh jalur UI selalu membuat rincian dari dalam layar kelompoknya.</p>
	 */
	private KelompokKegiatanKemahasiswaan kelompokKegiatanKemahasiswaan;

	/** Nama rincian aspek (kolom {@code nama} bertipe {@code text}). */
	private String nama;

	/** Nomor urut tampil dalam kelompoknya (kolom {@code nomorUrut}); boleh kosong di basis data. */
	private Integer nomorUrut;

	/** Bendera aktif (kolom {@code aktif}); {@code null} diperlakukan sebagai aktif saat dibaca. */
	private Boolean aktif;

	/**
	 * Bendera boleh-dipilih-mahasiswa (kolom {@code bisaDipilihMahasiswa}).
	 *
	 * <p>Dibaca lewat {@link #getBisaDipilihMahasiswa()}, yang dapat <b>menimpa</b> field ini
	 * dengan {@code false} bila kelompok induknya melarang &mdash; lihat Javadoc method
	 * tersebut.</p>
	 */
	private Boolean bisaDipilihMahasiswa;

	/**
	 * Daftar jabatan/peran yang berlaku untuk rincian ini.
	 *
	 * <p>Diinisialisasi {@link TreeSet}; lihat catatan tentang comparator {@code nomorUrut} di
	 * Javadoc kelas.</p>
	 */
	private Set<JabatanKegiatanKemahasiswaan> jabatanKegiatanKemahasiswaans = new TreeSet<JabatanKegiatanKemahasiswaan>();

	/**
	 * Mengembalikan himpunan jabatan/peran yang berlaku untuk rincian aspek ini.
	 *
	 * <p>Dipetakan many-to-many lewat tabel penghubung
	 * {@code detail_kelompok_has_jabatan_kegiatan_kemahasiswaan} ({@code detail_kelompok} &rarr;
	 * {@code jabatan_kegiatan_kemahasiswaan}), dengan cascade {@code MERGE} dan {@code PERSIST}
	 * saja &mdash; menghapus rincian tidak menghapus master jabatannya, hanya baris
	 * penghubungnya.</p>
	 *
	 * <p><b>Peran fungsional:</b> himpunan ini menjadi <i>sumbu baris</i> matriks rubrik angka
	 * kredit. Untuk setiap anggotanya, layar/unduhan Excel
	 * {@code NilaiKegiatanKemahasiswaanAction} membuat satu baris dan mencari
	 * {@link ais.database.model.NilaiKegiatanKemahasiswaan} berkode
	 * {@code "<idRincian>-<idSkala>-<idJabatan>"}. Bila himpunan ini <b>kosong</b>, rincian tetap
	 * dinilai tetapi dengan satu baris tanpa jabatan (kode berakhiran tanda hubung), yang berarti
	 * "berlaku untuk semua peran".</p>
	 *
	 * <p><b>Perhatikan:</b> keanggotaan di sini <b>tidak</b> ditegakkan saat menghitung angka
	 * kredit mahasiswa &mdash; lihat Javadoc kelas. Perhatikan pula bahwa kembalian dapat berupa
	 * koleksi lazy Hibernate; mengaksesnya di luar sesi aktif memicu
	 * {@code LazyInitializationException}.</p>
	 *
	 * <p><b>Dipanggil oleh:</b> renderer grid dan form pilihan di
	 * {@code DetailKelompokKegiatanKemahasiswaanHelper}, serta penyusun matriks/unduhan Excel di
	 * {@code NilaiKegiatanKemahasiswaanAction}.</p>
	 *
	 * @return himpunan jabatan terkait; tidak pernah {@code null}, tetapi bisa kosong
	 */
	@ManyToMany(targetEntity = JabatanKegiatanKemahasiswaan.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@JoinTable(name = "detail_kelompok_has_jabatan_kegiatan_kemahasiswaan", joinColumns = @JoinColumn(name = "detail_kelompok"), inverseJoinColumns = @JoinColumn(name = "jabatan_kegiatan_kemahasiswaan"))
	public Set<JabatanKegiatanKemahasiswaan> getJabatanKegiatanKemahasiswaans() {
		return jabatanKegiatanKemahasiswaans;
	}

	/**
	 * Mengganti seluruh himpunan jabatan yang berlaku untuk rincian aspek ini.
	 *
	 * <p><b>Efek samping penyimpanan:</b> mengganti himpunan berarti menulis ulang isi tabel
	 * penghubung pada flush berikutnya. Mencabut sebuah jabatan tidak menghapus baris rubrik
	 * {@link ais.database.model.NilaiKegiatanKemahasiswaan} yang sudah terlanjur dibuat untuk
	 * kombinasi itu &mdash; barisnya sekadar berhenti tampil di matriks (menjadi yatim), dan akan
	 * muncul kembali dengan bobot lamanya bila jabatan tersebut kelak dipasang lagi.</p>
	 *
	 * <p><b>Perhatikan:</b> bila pemanggil menyerahkan {@link TreeSet}, comparator
	 * {@link ais.database.model.GeneralValueObject} membuat dua jabatan bernomor urut sama saling
	 * menimpa; gunakan koleksi lain saat menyusun pilihan (pola yang dipakai
	 * {@code DetailKelompokKegiatanKemahasiswaanHelper}).</p>
	 *
	 * <p><b>Dipanggil oleh:</b> form simpan rincian aspek di
	 * {@code DetailKelompokKegiatanKemahasiswaanHelper}, dan Hibernate saat memuat baris.</p>
	 *
	 * @param jabatanKegiatanKemahasiswaans himpunan jabatan baru
	 */
	public void setJabatanKegiatanKemahasiswaans(Set<JabatanKegiatanKemahasiswaan> jabatanKegiatanKemahasiswaans) {
		this.jabatanKegiatanKemahasiswaans = jabatanKegiatanKemahasiswaans;
	}

	/**
	 * Daftar skala/tingkat penyelenggaraan yang berlaku untuk rincian ini.
	 *
	 * <p>Diinisialisasi {@link TreeSet}; lihat catatan tentang comparator {@code nomorUrut} di
	 * Javadoc kelas.</p>
	 */
	private Set<SkalaKegiatanKemahasiswaan> skalaKegiatanKemahasiswaans = new TreeSet<SkalaKegiatanKemahasiswaan>();

	/**
	 * Mengembalikan himpunan skala/tingkat penyelenggaraan yang berlaku untuk rincian aspek ini
	 * (mis. prodi, universitas, nasional, internasional).
	 *
	 * <p>Dipetakan many-to-many lewat tabel penghubung
	 * {@code detail_kelompok_has_skala_kegiatan_kemahasiswaan} ({@code detail_kelompok} &rarr;
	 * {@code skala_kegiatan_kemahasiswaan}), dengan cascade {@code MERGE} dan {@code PERSIST}
	 * saja.</p>
	 *
	 * <p><b>Peran fungsional:</b> himpunan ini menjadi <i>sumbu kolom</i> matriks rubrik angka
	 * kredit. Kolom yang tampil di layar/Excel adalah seluruh skala aktif, tetapi kotak angka
	 * hanya dibuat pada perpotongan yang skalanya termasuk dalam himpunan ini &mdash; perpotongan
	 * lain dibiarkan kosong dan tidak dapat diberi bobot.</p>
	 *
	 * <p><b>Perhatikan:</b> sama seperti sisi jabatan, keanggotaan di sini tidak diperiksa ulang
	 * oleh penjumlah angka kredit; ia sekadar membatasi entri. Kembalian dapat berupa koleksi lazy
	 * Hibernate.</p>
	 *
	 * <p><b>Dipanggil oleh:</b> renderer grid dan form pilihan di
	 * {@code DetailKelompokKegiatanKemahasiswaanHelper}, serta penyusun matriks/unduhan Excel di
	 * {@code NilaiKegiatanKemahasiswaanAction}.</p>
	 *
	 * @return himpunan skala terkait; tidak pernah {@code null}, tetapi bisa kosong
	 */
	@ManyToMany(targetEntity = SkalaKegiatanKemahasiswaan.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@JoinTable(name = "detail_kelompok_has_skala_kegiatan_kemahasiswaan", joinColumns = @JoinColumn(name = "detail_kelompok"), inverseJoinColumns = @JoinColumn(name = "skala_kegiatan_kemahasiswaan"))
	public Set<SkalaKegiatanKemahasiswaan> getSkalaKegiatanKemahasiswaans() {
		return skalaKegiatanKemahasiswaans;
	}

	/**
	 * Mengganti seluruh himpunan skala yang berlaku untuk rincian aspek ini.
	 *
	 * <p><b>Efek samping penyimpanan:</b> identik dengan
	 * {@link #setJabatanKegiatanKemahasiswaans(Set)} &mdash; isi tabel penghubung ditulis ulang
	 * saat flush, dan baris rubrik untuk skala yang dicabut tertinggal yatim (bobotnya tersimpan,
	 * tidak tampil, dan hidup kembali bila skala itu dipasang lagi).</p>
	 *
	 * <p><b>Dipanggil oleh:</b> form simpan rincian aspek di
	 * {@code DetailKelompokKegiatanKemahasiswaanHelper}, dan Hibernate saat memuat baris.</p>
	 *
	 * @param skalaKegiatanKemahasiswaans himpunan skala baru
	 */
	public void setSkalaKegiatanKemahasiswaans(Set<SkalaKegiatanKemahasiswaan> skalaKegiatanKemahasiswaans) {
		this.skalaKegiatanKemahasiswaans = skalaKegiatanKemahasiswaans;
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Kolom dipetakan {@code insertable = false} karena nilainya dibangkitkan basis data
	 * ({@code IDENTITY}); id baru baru tersedia setelah {@code INSERT} ter-flush.</p>
	 *
	 * <p><b>Perhatikan:</b> id ini ikut menjadi segmen pertama kode gabungan rubrik
	 * ({@code "<idRincian>-<idSkala>-<idJabatan>"}) yang disusun manual di beberapa tempat pada
	 * {@code NilaiKegiatanKemahasiswaanAction}.</p>
	 *
	 * @return id baris, atau {@code null} bila entity belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama baris ini.
	 *
	 * <p>Hanya dipakai Hibernate saat memuat/menyimpan entity; kode aplikasi tidak boleh
	 * menetapkannya sendiri.</p>
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama rincian aspek.
	 *
	 * <p>Kolom bertipe {@code text} tanpa kendala {@code nullable}/{@code unique}, jadi nama
	 * kosong maupun nama kembar (bahkan dalam satu kelompok) diterima basis data; kewajiban
	 * mengisinya hanya ditegakkan di form.</p>
	 *
	 * <p><b>Dipanggil oleh:</b> renderer grid rincian aspek, isi {@code Combobox} "Rincian Aspek
	 * Kegiatan" pada layar kegiatan kemahasiswaan, kolom "Rincian Aspek" pada unduhan Excel
	 * rubrik, dan pencarian {@code ilike} pada {@code initCriteria()}.</p>
	 *
	 * @return nama rincian aspek, atau {@code null} bila belum diisi
	 */
	@Column(columnDefinition = "text")
	public String getNama() {
		return nama;
	}

	/**
	 * Menetapkan nama rincian aspek.
	 *
	 * @param nama nama baru; boleh {@code null} sejauh basis data, walau form mewajibkannya
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Menetapkan kelompok/aspek kegiatan induk.
	 *
	 * <p><b>Dipanggil oleh:</b> {@code DetailKelompokKegiatanKemahasiswaanHelper} saat membuat
	 * rincian baru di dalam layar sebuah kelompok, dan Hibernate saat memuat baris.</p>
	 *
	 * @param kelompokKegiatanKemahasiswaan kelompok induk; boleh {@code null} sejauh skema
	 */
	public void setKelompokKegiatanKemahasiswaan(KelompokKegiatanKemahasiswaan kelompokKegiatanKemahasiswaan) {
		this.kelompokKegiatanKemahasiswaan = kelompokKegiatanKemahasiswaan;
	}

	/**
	 * Mengembalikan kelompok/aspek kegiatan induk, <b>sesudah meresolusi proxy lazy</b>.
	 *
	 * <p>Relasi dipetakan {@code fetch = LAZY}, sehingga field bisa berisi proxy Hibernate yang
	 * belum terinisialisasi. Getter ini melewatkannya ke {@code check(...)} milik
	 * {@link ais.database.model.GeneralValueObject} &mdash; utilitas bersama yang mencoba
	 * beberapa sumber berurutan (tanda {@code initData}, cache in-memory, session yang sedang
	 * aktif, lalu pemuatan ulang lewat session baru) dan mengembalikan argumennya apa adanya bila
	 * semuanya gagal. Efeknya relasi ini biasanya tetap terbaca walau sesi asalnya sudah
	 * tertutup.</p>
	 *
	 * <p><b>Efek samping:</b> hasil resolusi <b>ditugaskan balik</b> ke field, jadi pemanggilan
	 * pertama dapat mengubah isi field dari proxy menjadi instance nyata. Perubahan itu tidak
	 * mengubah kolom apa pun (nilai FK-nya sama), tetapi membuat pemeriksaan
	 * {@code Hibernate.isInitialized(...)} pada {@link #getBisaDipilihMahasiswa()} berubah dari
	 * {@code false} menjadi {@code true} &mdash; artinya urutan pemanggilan kedua getter itu
	 * menentukan apakah pewarisan bendera jadi dijalankan atau tidak.</p>
	 *
	 * <p><b>Dipanggil oleh:</b> unduhan Excel rubrik (kolom "Nama Aspek"), pengurutan hasil
	 * {@code initCriteria()} lewat alias, dan berbagai layar yang menampilkan jalur lengkap
	 * aspek &rarr; rincian.</p>
	 *
	 * @return kelompok induk yang sudah diresolusi, atau {@code null} bila FK memang kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_kegiatan_kemahasiswaan", nullable = true)
	public KelompokKegiatanKemahasiswaan getKelompokKegiatanKemahasiswaan() {
		kelompokKegiatanKemahasiswaan = check(kelompokKegiatanKemahasiswaan);
		return kelompokKegiatanKemahasiswaan;
	}

	/**
	 * Mengembalikan nomor urut tampil rincian ini di dalam kelompoknya.
	 *
	 * <p><b>Menyulih {@code null} menjadi {@code 1}</b> saat membaca, tanpa menulis balik ke
	 * field &mdash; kolomnya tetap {@code NULL} di basis data. Perbedaan ini bermakna karena
	 * pengurutan daftar dikerjakan SQL ({@code order by nomorUrut}) yang melihat {@code NULL}
	 * apa adanya, sementara pengurutan di Java (comparator
	 * {@link ais.database.model.GeneralValueObject}) melihat angka {@code 1}. Urutan di layar
	 * karena itu bisa berbeda dari urutan pada unduhan/berkas, dan dua rincian yang sama-sama
	 * belum diberi nomor urut dianggap "seri" oleh comparator.</p>
	 *
	 * <p><b>Dipanggil oleh:</b> kotak nomor urut pada grid rincian aspek (auto-save saat diubah)
	 * dan {@code compareTo} kelas induk.</p>
	 *
	 * @return nomor urut; {@code 1} bila kolomnya kosong (tidak pernah {@code null})
	 */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menetapkan nomor urut tampil.
	 *
	 * <p>Menerima {@code null} apa adanya &mdash; berbeda dari {@link #getNomorUrut()} yang
	 * menyulihnya menjadi {@code 1} hanya saat membaca.</p>
	 *
	 * <p><b>Dipanggil oleh:</b> penyunting {@code onChange} kotak nomor urut pada grid rincian
	 * aspek, yang langsung menyimpan perubahannya.</p>
	 *
	 * @param nomorUrut nomor urut baru; boleh {@code null}
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Mengembalikan bendera aktif rincian aspek ini.
	 *
	 * <p><b>Menyulih {@code null} menjadi {@code true}</b> saat membaca (baris lama yang belum
	 * pernah diberi nilai dianggap aktif), tanpa menulis balik ke field. Penyaring di layar entri
	 * kegiatan mencerminkan aturan yang sama di tingkat SQL dengan
	 * {@code Restrictions.or(isNull("aktif"), eq("aktif", true))}, sehingga kedua sisi
	 * konsisten.</p>
	 *
	 * <p><b>Dipanggil oleh:</b> checkbox "Aktif" pada grid rincian aspek (auto-save saat
	 * dicentang).</p>
	 *
	 * @return {@code true} bila rincian ini masih dipakai; {@code true} pula bila kolomnya kosong
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan bendera aktif.
	 *
	 * <p>Menonaktifkan rincian hanya menyembunyikannya dari daftar pilih kegiatan baru; kegiatan
	 * dan keikutsertaan yang sudah terlanjur menunjuk rincian ini tetap ada dan tetap dihitung
	 * angka kreditnya.</p>
	 *
	 * @param aktif bendera aktif; boleh {@code null}, yang saat dibaca berarti aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan apakah rincian aspek ini boleh dipilih sendiri oleh mahasiswa saat mengajukan
	 * kegiatan, <b>sambil menurunkan larangan dari kelompok induknya</b>.
	 *
	 * <p><b>Aturan.</b> Bila kelompok induk sudah termuat <i>dan</i> induk itu melarang mahasiswa
	 * memilih, maka rincian ini pun dipaksa menjadi {@code false}. Selain itu nilai field
	 * dikembalikan apa adanya, dengan {@code null} disulih menjadi {@code true} (baris lama
	 * dianggap boleh dipilih).</p>
	 *
	 * <p><b>Getter yang menulis balik &mdash; efek samping paling penting di kelas ini.</b>
	 * Penurunan larangan dikerjakan dengan <i>menugaskan</i> {@code false} ke field, bukan sekadar
	 * mengembalikan {@code false}. Karena kelas ini memakai <i>property access</i>, Hibernate
	 * memanggil getter ini saat dirty check dan flush, sehingga perubahan tersebut ikut tersimpan
	 * sebagai {@code UPDATE} nyata pada kolom {@code bisaDipilihMahasiswa} &mdash; lengkap dengan
	 * revisi Envers atas nama pengguna yang kebetulan sedang membuka layar. Akibatnya:</p>
	 *
	 * <ul>
	 *   <li>penurunan bersifat <b>satu arah dan tidak dapat pulih sendiri</b>: bila kelompok induk
	 *       kelak diizinkan kembali, rincian yang sudah terlanjur ditulis {@code false} tetap
	 *       {@code false} sampai ada yang mencentangnya ulang satu per satu;</li>
	 *   <li>karena penyaring di layar entri kegiatan bekerja atas <b>kolom</b>, bukan lewat getter
	 *       ini, pewarisan larangan baru benar-benar terasa oleh mahasiswa <i>setelah</i> kolomnya
	 *       sempat ditulis-balik &mdash; yaitu setelah seseorang membuka layar yang memuat baris
	 *       ini di dalam sesi yang ter-flush;</li>
	 *   <li>bila field {@code bisaDipilihMahasiswa} sudah bernilai {@code false}, penugasan ulang
	 *       tidak mengubah apa pun sehingga tidak ada {@code UPDATE} tambahan.</li>
	 * </ul>
	 *
	 * <p><b>Kenapa proxy diperiksa manual.</b> Kelompok induk sengaja dibaca sebagai <i>field</i>
	 * dan dijaga {@code Hibernate.isInitialized(...)}, bukan lewat
	 * {@link #getKelompokKegiatanKemahasiswaan()}. Memanggil getter itu akan meresolusi proxy dan
	 * dapat melempar {@code LazyInitializationException} ketika baris dibaca dari thread
	 * laporan/latar setelah sesinya tertutup. Konsekuensi yang perlu disadari: aturan pewarisan
	 * <b>hanya dijalankan bila proxy induk kebetulan sudah terinisialisasi</b>, sehingga hasil
	 * getter ini bergantung pada urutan pemanggilan getter lain pada instance yang sama.</p>
	 *
	 * <p><b>Dipanggil oleh:</b> checkbox "Bisa Dipilih Mahasiswa" pada grid rincian aspek di
	 * {@code DetailKelompokKegiatanKemahasiswaanHelper}, dan Hibernate pada setiap dirty
	 * check/flush baris ini.</p>
	 *
	 * @return {@code true} bila mahasiswa boleh memilih rincian ini; {@code true} pula bila
	 *         kolomnya kosong dan induknya tidak melarang. Tidak pernah {@code null}
	 */
	public Boolean getBisaDipilihMahasiswa() {
		// FIX LazyInitializationException: kelompokKegiatanKemahasiswaan bisa berupa
		// proxy Hibernate lazy yang belum di-initialize (mis. diakses dari thread
		// report/background setelah sesi tertutup, lihat ManajemenProperty.safeToString
		// utk pola serupa). Jangan panggil getter proxy sebelum cek isInitialized,
		// karena itu akan memicu load & melempar LazyInitializationException.
		if (kelompokKegiatanKemahasiswaan != null && Hibernate.isInitialized(kelompokKegiatanKemahasiswaan)
				&& !kelompokKegiatanKemahasiswaan.getBisaDipilihMahasiswa()) {
			bisaDipilihMahasiswa = false;
		}
		return bisaDipilihMahasiswa == null ? true : bisaDipilihMahasiswa;
	}

	/**
	 * Menetapkan apakah mahasiswa boleh memilih rincian aspek ini saat mengajukan kegiatan.
	 *
	 * <p>Menerima {@code null} apa adanya. Perlu diingat bahwa nilai {@code true} yang disetel di
	 * sini <b>tidak bertahan</b> selama kelompok induknya masih melarang: pembacaan berikutnya
	 * lewat {@link #getBisaDipilihMahasiswa()} akan menimpanya kembali menjadi {@code false}.</p>
	 *
	 * <p><b>Dipanggil oleh:</b> penyunting {@code onCheck} checkbox "Bisa Dipilih Mahasiswa" pada
	 * grid rincian aspek, yang langsung menyimpan perubahannya.</p>
	 *
	 * @param bisaDipilihMahasiswa bendera baru; boleh {@code null}, yang saat dibaca berarti boleh
	 */
	public void setBisaDipilihMahasiswa(Boolean bisaDipilihMahasiswa) {
		this.bisaDipilihMahasiswa = bisaDipilihMahasiswa;
	}

}
