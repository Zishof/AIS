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

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.SatuanKerja;

/**
 * Entity master <b>kategori/kelompok "form tambahan" (field kustom) untuk data Catatan Pegawai</b>,
 * dipetakan ke tabel {@code public.kelompok_parameter_tambahan_catatan_pegawai}.
 *
 * <h3>Peran dalam modul Catatan Pegawai</h3>
 * <p>Catatan Pegawai adalah berkas riwayat/kejadian kepegawaian (mis. surat peringatan, penghargaan,
 * mutasi). Karena isi catatan sangat berbeda antar perguruan tinggi, AIS mengizinkan admin menambah
 * pertanyaan/isian sendiri tanpa mengubah skema. Rantainya empat lapis &mdash; satu lapis
 * <b>lebih banyak</b> daripada varian Alumni/Mahasiswa/Calon Mahasiswa:</p>
 * <ol>
 *   <li>{@link ParameterTambahan} &mdash; <b>definisi</b> field kustom generik (label, tipe input,
 *   daftar pilihan, wajib/tidak). Dipakai bersama oleh banyak modul.</li>
 *   <li>{@link ParameterTambahanCatatanPegawai} &mdash; tabel penghubung yang <b>mengaitkan</b>
 *   sebuah {@link ParameterTambahan} ke satu baris kelas ini.</li>
 *   <li><b>Kelas ini</b> &mdash; <b>kategori/judul kelompok</b> tempat field-field tersebut
 *   ditampilkan berkelompok. Baris kelas ini menjadi heading section pada form isian catatan.</li>
 *   <li>{@link JenisCatatanPegawai} &mdash; <b>lapis ekstra</b> yang tidak ada pada varian lain:
 *   setiap jenis catatan memilih <i>kelompok mana saja</i> yang muncul, lewat relasi
 *   {@code @ManyToMany} bertabel gabung {@code sekolah.jenis_catatan_pegawai_has_parameter}. Jadi
 *   kategori di sini <b>tidak</b> otomatis tampil di semua form; ia harus dicentang pada layar
 *   Jenis Catatan Pegawai terlebih dahulu.</li>
 * </ol>
 * <p>Nilai jawaban tidak disimpan di sini melainkan sebagai string terserialisasi pada
 * {@link CatatanPegawai} (lihat {@code CatatanPegawai.populateParameterTambahan(...)} dan
 * {@link ais.action.master.helper.ParameterTambahanCatatanPegawaiListener} yang merender form-nya).
 * </p>
 *
 * <h3>Auto-seed lewat {@link #checkCreateDefault()}</h3>
 * <p>Bila belum ada satu pun baris bertanda {@link #getDefaultData() defaultData}{@code =true},
 * method statis {@link #checkCreateDefault()} membuatnya sendiri dan meng-{@code commit} langsung ke
 * DB. Efeknya, sekadar <b>membuka layar</b> "Parameter Tambahan Catatan Pegawai" sudah menulis baris
 * master baru. Lihat dokumentasi method tersebut untuk kuirk transaksi/session-nya.</p>
 * <p><b>Beda dari varian Alumni:</b> di sini hanya ada <b>SATU</b> mekanisme seed. Layar
 * {@code ais.action.master.KelompokParameterTambahanCatatanPegawaiAction.doAfterCompose(...)}
 * <b>tidak</b> menanam baris bawaan sendiri, sehingga bug balapan "dua kategori bawaan berbeda
 * tergantung urutan klik admin" yang ada pada
 * {@link KelompokParameterTambahanAlumni} <b>TIDAK berlaku</b> untuk kelas ini (diverifikasi: satu-
 * satunya pemanggil {@code checkCreateDefault()} di seluruh codebase adalah
 * {@code ParameterTambahanCatatanPegawaiAction}).</p>
 *
 * <h3>Kuirk paling penting &mdash; koleksi bertipe {@code TreeSet} + {@link #compareTo}</h3>
 * <p>{@link #compareTo(GeneralValueObject)} kelas ini <b>hanya</b> membandingkan
 * {@link #getNomorUrut()} dan mengembalikan {@code 0} untuk nomor urut yang sama. Sementara itu
 * pemegang koleksinya adalah {@code TreeSet}:</p>
 * <ul>
 *   <li>{@code JenisCatatanPegawai.kelompokParameterTambahanCatatanPegawais} &mdash; koleksi
 *   {@code @ManyToMany} itu sendiri diinisialisasi sebagai {@code new TreeSet<...>()};</li>
 *   <li>{@code ais.action.master.CatatanPegawaiAction} menyalin ulang isi relasi ke {@code TreeSet}
 *   baru sebelum menyerahkannya ke perender form.</li>
 * </ul>
 * <p>{@code TreeSet} memakai {@code compareTo} sebagai definisi kesamaan, bukan
 * {@link GeneralValueObject#equals(Object)}. Karena <b>nilai bawaan {@code nomorUrut} adalah
 * {@code 1} untuk SEMUA baris</b> (lihat {@link #getNomorUrut()}), dua kategori berbeda yang nomor
 * urutnya kebetulan sama akan <b>saling menelan secara senyap</b>: hanya satu yang bertahan di
 * himpunan, sehingga seluruh section field kustom milik kategori lainnya <b>hilang dari form</b>
 * tanpa pesan kesalahan apa pun. Ini pola penciutan senyap yang sama seperti yang tercatat pada
 * entity lain di codebase, tetapi di sini dampaknya langsung terlihat pengguna. Dicatat apa adanya
 * &mdash; tidak diperbaiki di sini.</p>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 *   <li><b>Identitas &amp; audit:</b> {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()}, {@link #onUpdate()}, {@link #toString()}.</li>
 *   <li><b>Atribut kategori:</b> {@link #getNama()}, {@link #getKeterangan()},
 *   {@link #getNomorUrut()}.</li>
 *   <li><b>Penanda perilaku:</b> {@link #getDefaultData()} (penanda baris bawaan, sekaligus
 *   pengunci tombol hapus), {@link #getAktif()} (tampil/tidak pada form).</li>
 *   <li><b>Relasi:</b> {@link #getSatuanKerja()} &mdash; <b>tidak terpakai</b>, lihat
 *   dokumentasinya.</li>
 *   <li><b>Utilitas statis:</b> {@link #checkCreateDefault()}.</li>
 *   <li><b>Pengurutan:</b> {@link #compareTo(GeneralValueObject)}.</li>
 * </ul>
 *
 * <h3>Catatan warisan &amp; pemetaan (non-obvious)</h3>
 * <ul>
 *   <li>{@link GeneralValueObject} <b>bukan</b> {@code @MappedSuperclass} dan bukan {@code @Entity},
 *   sehingga Hibernate <b>mengabaikan seluruh property-nya</b>. Itulah sebabnya {@code id},
 *   {@code nama}, {@code keterangan}, {@code nomorUrut}, {@code oleh}, {@code olehId} dideklarasikan
 *   ULANG di kelas ini (field kelas ini <i>membayangi</i> field induk yang bernama sama). Ini
 *   keharusan teknis, bukan duplikasi yang keliru.</li>
 *   <li>Anotasi berada di <b>getter</b> ({@code @Id} pada {@link #getId()}), jadi Hibernate memakai
 *   <b>property access</b> untuk SEMUA property. Getter yang memodifikasi field karena itu terlihat
 *   oleh dirty-check; dikombinasikan dengan {@code dynamicUpdate=true}, sekadar membaca baris bisa
 *   memicu {@code UPDATE} + revisi Envers. Lihat {@link #getDefaultData()}, {@link #getAktif()},
 *   {@link #getNomorUrut()}.</li>
 *   <li>{@code defaultData}, {@code aktif}, dan {@code nomorUrut} <b>tidak</b> punya {@code @Column}
 *   eksplisit sehingga nama kolomnya mengikuti strategi penamaan default Hibernate (nama property
 *   apa adanya, dilipat ke huruf kecil oleh PostgreSQL).</li>
 *   <li>{@code @Audited} &mdash; setiap perubahan direkam Envers ke tabel revisi.</li>
 * </ul>
 *
 * <h3>Perbandingan dengan keluarga {@code KelompokParameterTambahan*}</h3>
 * <p>Kelas ini sekeluarga dengan {@link KelompokParameterTambahanAlumni},
 * {@link KelompokParameterTambahanMahasiswa}, dan {@link KelompokParameterTambahanCalonMahasiswa}
 * (ketiganya identik kata-per-kata satu sama lain). Kelas ini adalah anggota keluarga yang
 * <b>paling banyak menyimpang</b>:</p>
 * <ul>
 *   <li><b>Tidak punya</b> cabang fallback {@code nim} &rarr; {@code nama} &rarr;
 *   {@code keterangan} pada {@code compareTo}; implementasinya satu baris tanpa
 *   {@code instanceof} &mdash; lihat {@link #compareTo(GeneralValueObject)}.</li>
 *   <li><b>Tidak punya</b> field bawaan hbm2java yang dikomentari mati
 *   ({@code bolehMengulang}) maupun penanda "portal pengguna"
 *   ({@code digunakanUntukPenggunaAlumni}).</li>
 *   <li><b>Punya tambahan</b> relasi {@link #getSatuanKerja()} yang tidak ada di varian lain
 *   (dan tidak terpakai).</li>
 *   <li><b>Sama persis</b> dalam: blok audit {@code oleh}/{@code olehId}/{@code tanggal_dirubah},
 *   {@link #toString()}, {@link #checkCreateDefault()} (termasuk nama variabel lokal salah
 *   salin-tempel), {@link #getNama()}, {@link #getKeterangan()} (termasuk pembalikan kontrak
 *   induk), serta ketiga getter mutatif {@link #getDefaultData()}/{@link #getAktif()}/
 *   {@link #getNomorUrut()} (termasuk ternary mati di getter terakhir).</li>
 * </ul>
 *
 * <p><b>Konsumen utama:</b>
 * {@code ais.action.master.KelompokParameterTambahanCatatanPegawaiAction} (CRUD master),
 * {@code ais.action.master.ParameterTambahanCatatanPegawaiAction} (pengaitan field ke kategori;
 * satu-satunya pemanggil {@link #checkCreateDefault()}),
 * {@code ais.action.master.JenisCatatanPegawaiAction} (pemilihan kategori per jenis catatan),
 * {@link ais.action.master.helper.ParameterTambahanCatatanPegawaiListener} (perender form isian),
 * {@code ais.action.master.CatatanPegawaiAction} (layar transaksi catatan),
 * {@link CatatanPegawai} (pembacaan jawaban), dan
 * {@code ais.action.report.format1.employ.LaporanCatatanPegawai} (laporan).</p>
 *
 * <p><b>Konkurensi:</b> tidak thread-safe (POJO Hibernate biasa); jangan berbagi instance lintas
 * session/thread. Perhatikan pula bahwa {@code JenisCatatanPegawai.mapParameters} adalah
 * {@code HashMap} <b>statis</b> yang meng-cache himpunan kategori per id jenis catatan untuk seluruh
 * JVM &mdash; instance kelas ini karena itu bisa ikut dibagikan lintas sesi pengguna.</p>
 *
 * @see ParameterTambahanCatatanPegawai
 * @see ParameterTambahan
 * @see JenisCatatanPegawai
 * @see CatatanPegawai
 * @see KelompokParameterTambahanAlumni
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kelompok_parameter_tambahan_catatan_pegawai")
public class KelompokParameterTambahanCatatanPegawai extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai tetap agar instance lama tetap dapat dideserialisasi
	 * (relevan untuk sesi ZK yang di-passivate ke disk).
	 *
	 * <p>Nilainya <b>identik</b> dengan milik {@link KelompokParameterTambahanAlumni} dan saudara
	 * sekeluarga lainnya &mdash; sisa salin-tempel; tidak berpengaruh karena berbeda kelas.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris kategori. Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris; diisi jalur audit. Lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris; diisi jalur audit. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Dideklarasikan ulang di kelas ini karena {@link GeneralValueObject} bukan
	 * {@code @MappedSuperclass}; lihat catatan warisan pada dokumentasi kelas.</p>
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna terakhir yang mengubah baris ini.
	 *
	 * <p><b>Kuirk:</b> nilai {@code null}, kosong, atau hanya spasi <b>diabaikan diam-diam</b>
	 * (method langsung {@code return}), sehingga nilai lama bertahan dan jejak audit tidak pernah
	 * bisa dikosongkan lewat setter ini.</p>
	 *
	 * @param olehId id pengguna pengubah; {@code null}/kosong/spasi diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna terakhir yang mengubah baris ini.
	 *
	 * <p><b>Kuirk:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong/spasi
	 * diabaikan diam-diam sehingga nilai lama bertahan.</p>
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/kosong/spasi diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Dideklarasikan ulang di kelas ini karena {@link GeneralValueObject} bukan
	 * {@code @MappedSuperclass}; lihat catatan warisan pada dokumentasi kelas.</p>
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
	 * <p>Hanya {@code @PreUpdate} yang dipasang; baris BARU ({@code INSERT}) tidak melewati callback
	 * ini. Karena itu baris hasil auto-seed {@link #checkCreateDefault()} masuk <b>tanpa jejak</b>
	 * {@code oleh}/{@code olehId}.</p>
	 *
	 * <p>Perlu diingat bahwa {@link #getDefaultData()}, {@link #getAktif()}, dan
	 * {@link #getNomorUrut()} dapat mengotori field saat baris sekadar dibaca, sehingga callback ini
	 * bisa ikut terpicu pada {@code UPDATE} yang <b>tidak diminta pengguna mana pun</b> &mdash;
	 * jejak audit lalu mencatat pengguna yang kebetulan sedang membuka layar.</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja berada pada baris fisik yang sama dalam
	 * kode aslinya; nilainya diinisialisasi memakai jam aplikasi
	 * ({@code ais.ui.util.WaktuUtil.getDate()}), bukan {@code new Date()}, agar konsisten dengan
	 * zona waktu/penyetelan waktu server yang dipakai seluruh modul.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi.
	 *
	 * <p>Umumnya diisi otomatis lewat {@link #onUpdate()}; pemanggilan manual akan tertimpa pada
	 * {@code UPDATE} berikutnya.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru; {@code null} diterima
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini (presisi {@code TIMESTAMP}).
	 *
	 * <p>Tidak pernah {@code null} pada objek yang baru dibuat di JVM (diinisialisasi saat
	 * konstruksi), tetapi <b>bisa</b> {@code null} untuk baris lama hasil impor/migrasi.</p>
	 *
	 * @return stempel waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat baris: {@code "<id>-<nama>"}.
	 *
	 * <p><b>Beda dari induk:</b> {@link GeneralValueObject#toString()} memakai {@code kode}+
	 * {@code nama}; di sini {@code kode} diganti {@code id}. Method ini juga membaca <b>field</b>
	 * {@code nama} langsung (bukan {@link #getNama()}), sehingga hasilnya <b>tidak di-trim</b> dan
	 * bisa berisi spasi tepi apa adanya dari DB.</p>
	 *
	 * @return {@code id} disambung tanda hubung dan {@code nama}; kedua bagian bisa berbunyi
	 *         {@code "null"} bila belum diisi
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama kategori; menjadi judul section pada form isian catatan pegawai. Lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas kategori; ditampilkan sebagai kolom kedua grid master. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Penanda baris bawaan hasil auto-seed. Lihat {@link #getDefaultData()}. */
	private Boolean defaultData;
	/** Penanda kategori masih dipakai/ditampilkan. Lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Nomor urut tampil kategori pada form. Lihat {@link #getNomorUrut()}. */
	private Integer nomorUrut;
	/** Unit kerja pemilik kategori &mdash; dipetakan tetapi TIDAK terpakai. Lihat {@link #getSatuanKerja()}. */
	private SatuanKerja satuanKerja;

	/**
	 * <b>Auto-seed:</b> memastikan selalu ada satu kategori bawaan bertanda
	 * {@link #getDefaultData() defaultData}{@code =true}, dan mengembalikannya.
	 *
	 * <h4>Cara kerja</h4>
	 * <ol>
	 *   <li>Mengambil session native ThreadLocal lewat
	 *   {@link HibernateUtil#currentNativeSession()}.</li>
	 *   <li>Mencari baris pertama dengan {@code defaultData = true}
	 *   ({@code setMaxResults(1)} + {@code uniqueResult()}).</li>
	 *   <li>Bila <b>tidak</b> ketemu: membuat baris baru dengan {@code defaultData=true},
	 *   {@code nama="Form Tambahan"}, {@code keterangan="Form Tambahan"}, lalu
	 *   {@code begin()} &rarr; {@code save()} &rarr; {@code commit()} transaksi <b>langsung ke
	 *   DB</b>.</li>
	 *   <li>Menutup session ThreadLocal lewat {@link HibernateUtil#closeSession()} &mdash;
	 *   <b>selalu</b>, termasuk pada jalur "sudah ada" yang tidak menulis apa pun.</li>
	 * </ol>
	 *
	 * <h4>Efek samping &amp; kuirk (WAJIB dibaca sebelum memanggil)</h4>
	 * <ul>
	 *   <li><b>Menulis ke DB saat sekadar dibaca.</b> Membuka layar pemanggil sudah cukup untuk
	 *   membuat baris master baru + revisi Envers, tanpa aksi simpan dari pengguna. Ini pola yang
	 *   sekeluarga dengan auto-tulis default pada {@link Konfigurasi}.</li>
	 *   <li><b>Menutup session milik pemanggil.</b> {@code closeSession()} mengeluarkan session dari
	 *   ThreadLocal lalu {@code clear}/{@code rollback}/{@code disconnect}/{@code close}. Kode
	 *   sesudahnya di thread yang sama akan mendapat session BARU, dan entity apa pun yang
	 *   dipegang pemanggil sebelum pemanggilan ini menjadi <b>detached</b> (koleksi lazy-nya tidak
	 *   bisa diinisialisasi lagi). Karena itu method ini aman dipanggil hanya di AWAL sebuah
	 *   request, sebagaimana dilakukan pemanggil satu-satunya.</li>
	 *   <li><b>Objek kembalian selalu detached</b> (session sudah ditutup saat {@code return}).</li>
	 *   <li>{@code begin()} akan melempar bila session ThreadLocal sudah punya transaksi aktif;
	 *   tidak ada {@code try}/{@code rollback} di sini.</li>
	 *   <li>Tidak ada penguncian/keunikan di level DB: dua request bersamaan pada instalasi kosong
	 *   dapat membuat dua baris "Form Tambahan".</li>
	 *   <li>Baris hasil seed lahir dengan {@code nomorUrut} dan {@code aktif} bernilai {@code NULL}
	 *   di DB (setter-nya tidak pernah dipanggil di sini); keduanya baru "disembuhkan" menjadi
	 *   {@code 1}/{@code true} saat pertama kali dibaca &mdash; lihat {@link #getNomorUrut()} dan
	 *   {@link #getAktif()}.</li>
	 *   <li>Nama variabel lokal berbunyi {@code kelompokParameterTambahanCatatanSiswa} (&#8220;Siswa&#8221;,
	 *   bukan &#8220;Pegawai&#8221;) &mdash; sisa salin-tempel dari varian lain; tidak berpengaruh pada
	 *   perilaku.</li>
	 * </ul>
	 *
	 * <h4>Pemanggil</h4>
	 * <p>Satu-satunya pemanggil di codebase adalah
	 * {@code ais.action.master.ParameterTambahanCatatanPegawaiAction.doAfterCompose(Component)}
	 * &mdash; layar master "Parameter Tambahan Catatan Pegawai". Method dipanggil tepat sesudah
	 * {@code Common.initLaguage()} dan sebelum combobox kategori diisi
	 * ({@code Common.insertCombo(..., KelompokParameterTambahanCatatanPegawai.class)}), agar dropdown
	 * tidak pernah kosong pada instalasi baru.</p>
	 * <p><b>Berbeda dari {@link KelompokParameterTambahanAlumni}</b>, tidak ada mekanisme seed KEDUA
	 * di layar {@code KelompokParameterTambahanCatatanPegawaiAction}; jadi hanya method inilah yang
	 * pernah menanam baris bawaan untuk tabel ini.</p>
	 *
	 * @return baris kategori bawaan (yang ditemukan atau yang baru saja dibuat); tidak pernah
	 *         {@code null}, tetapi dalam keadaan <b>detached</b>
	 */
	public static KelompokParameterTambahanCatatanPegawai checkCreateDefault() {
		Session session = HibernateUtil.currentNativeSession();
		KelompokParameterTambahanCatatanPegawai kelompokParameterTambahanCatatanSiswa = (KelompokParameterTambahanCatatanPegawai) session
				.createCriteria(KelompokParameterTambahanCatatanPegawai.class).add(Restrictions.eq("defaultData", true))
				.setMaxResults(1).uniqueResult();
		if (kelompokParameterTambahanCatatanSiswa == null) {
			kelompokParameterTambahanCatatanSiswa = new KelompokParameterTambahanCatatanPegawai();
			kelompokParameterTambahanCatatanSiswa.setDefaultData(true);
			kelompokParameterTambahanCatatanSiswa.setNama("Form Tambahan");
			kelompokParameterTambahanCatatanSiswa.setKeterangan("Form Tambahan");
			session.getTransaction().begin();
			session.save(kelompokParameterTambahanCatatanSiswa);
			session.getTransaction().commit();
		}

		HibernateUtil.closeSession();
		return kelompokParameterTambahanCatatanSiswa;
	}

	/**
	 * Konstruktor tanpa argumen. Wajib ada untuk Hibernate/ZK data-binding; seluruh field dibiarkan
	 * pada nilai awalnya ({@code null}, kecuali {@code tanggal_dirubah} yang diisi jam aplikasi).
	 *
	 * <p>Dipakai langsung oleh {@link #checkCreateDefault()} dan oleh tombol "Tambah" pada
	 * {@code KelompokParameterTambahanCatatanPegawaiAction}.</p>
	 */
	public KelompokParameterTambahanCatatanPegawai() {
	}

	/**
	 * Mengembalikan primary key baris kategori.
	 *
	 * <p>Kolom {@code id} bersifat {@code insertable=false} &mdash; nilainya dihasilkan sepenuhnya
	 * oleh sequence/identity PostgreSQL saat {@code INSERT}, jadi menyetelnya sebelum simpan tidak
	 * berpengaruh.</p>
	 *
	 * <p>Nilai ini ikut membentuk kunci jawaban pada form isian: {@code ParameterTambahanCatatan
	 * PegawaiListener} merakit penanda {@code "<idKelompok>-><idParameter>"} untuk setiap field,
	 * sehingga <b>mengganti id kategori berarti memutus tautan ke jawaban lama</b>.</p>
	 *
	 * @return primary key, atau {@code null} untuk objek yang belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Tanpa validasi; dipakai jalur load/binding, bukan kode bisnis.
	 *
	 * @param id primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama kategori, sudah <b>di-trim</b> spasi tepinya.
	 *
	 * <p>Nama inilah yang dirender sebagai judul section pada form isian catatan pegawai
	 * ({@code ParameterTambahanCatatanPegawaiListener}), sebagai label checkbox pada layar Jenis
	 * Catatan Pegawai, dan sebagai label pilihan combobox pada layar master. Kolom {@code nama}
	 * {@code NOT NULL} di DB.</p>
	 *
	 * <p><b>Catatan:</b> trim hanya terjadi pada pembacaan; nilai di field/DB tetap apa adanya, dan
	 * {@link #toString()} yang membaca field langsung tidak ikut ter-trim.</p>
	 *
	 * @return nama kategori tanpa spasi tepi, atau {@code null} bila field belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama kategori. Tanpa validasi di lapisan entity &mdash; keunikan/kewajiban isi
	 * diperiksa di lapisan layar ({@code KelompokParameterTambahanCatatanPegawaiAction}).
	 *
	 * @param nama nama kategori baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas kategori.
	 *
	 * <p><b>Membalik kontrak kelas induk.</b> {@link GeneralValueObject#getKeterangan()}
	 * menormalkan {@code null} menjadi {@code ""} sehingga pemanggil tidak perlu memeriksa
	 * {@code null}; override di sini mengembalikan field <b>apa adanya</b> dan <b>bisa
	 * {@code null}</b>. Kode yang menerima entity lewat tipe {@link GeneralValueObject} karena itu
	 * tetap wajib memeriksa {@code null} bila baris sesungguhnya bertipe kelas ini.</p>
	 *
	 * <p><b>Akibat nyata:</b> renderer grid master memanggil
	 * {@code new Label(kelompok.getKeterangan())} tanpa penjagaan, sehingga baris tanpa keterangan
	 * menghasilkan label kosong (ZK menerima {@code null}) &mdash; bukan kegagalan, tetapi berbeda
	 * dari perilaku entity yang memakai versi induk.</p>
	 *
	 * @return keterangan kategori, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas kategori. Tanpa validasi; {@code null} diterima.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan penanda "baris bawaan": {@code true} hanya untuk baris hasil auto-seed
	 * {@link #checkCreateDefault()}, yang dipakai sebagai kategori penampung default.
	 *
	 * <p><b>Dipakai sebagai proteksi hapus:</b> renderer grid master menyembunyikan tombol hapus
	 * bila {@code getDefaultData()} bernilai {@code true}
	 * ({@code button.setVisible(delete && !kelompok.getDefaultData())}), sehingga kategori bawaan
	 * tidak bisa dihilangkan lewat UI.</p>
	 *
	 * <p><b>Getter mutatif &mdash; efek samping.</b> Bila field masih {@code null}, method ini
	 * <b>menulis {@code false} ke field</b> sebelum mengembalikannya. Karena kelas ini memakai
	 * property access + {@code dynamicUpdate=true}, perubahan itu terlihat oleh dirty-check
	 * Hibernate: membaca baris lama yang kolomnya {@code NULL} di dalam session aktif dapat memicu
	 * {@code UPDATE} + revisi Envers tanpa aksi pengguna. Sifatnya idempoten/self-healing (nilai
	 * yang ditulis sama dengan nilai yang dikembalikan), tetapi tetap mengotori jejak audit.</p>
	 *
	 * @return {@code true} bila baris ini kategori bawaan; tidak pernah {@code null}
	 */
	public Boolean getDefaultData() {
		if (defaultData == null) {
			defaultData = false;
		}
		return defaultData;
	}

	/**
	 * Menyetel penanda baris bawaan. Tanpa validasi.
	 *
	 * <p>Di codebase hanya {@link #checkCreateDefault()} yang menyetelnya ke {@code true}; layar
	 * master tidak menyediakan kendali untuk field ini.</p>
	 *
	 * @param defaultData penanda baru; {@code null} akan dinormalkan menjadi {@code false} pada
	 *        pembacaan berikutnya
	 */
	public void setDefaultData(Boolean defaultData) {
		this.defaultData = defaultData;
	}

	/**
	 * Mengembalikan penanda kategori masih aktif (field-field di bawahnya ikut dirender pada form).
	 *
	 * <p>Menjadi filter nyata di {@code ParameterTambahanCatatanPegawaiListener}: kriteria
	 * pengambilan {@link ParameterTambahan} menyertakan
	 * {@code kelompokParameterTambahanCatatanPegawai.aktif = true}, jadi menonaktifkan kategori
	 * <b>menyembunyikan seluruh field kustom di dalamnya</b> (jawaban lama tetap tersimpan, hanya
	 * tidak ditampilkan). Nilai ini juga menjadi checkbox yang bisa diubah langsung dari grid
	 * master.</p>
	 *
	 * <p><b>Getter mutatif &mdash; efek samping.</b> Sama seperti {@link #getDefaultData()}: field
	 * {@code null} <b>ditulisi</b> nilai default (di sini {@code true}) sebelum dikembalikan,
	 * sehingga dapat memicu {@code UPDATE} + revisi Envers pada baris yang sekadar dibaca.
	 * Perhatikan default-nya {@code true}, jadi kategori lama tanpa nilai dianggap AKTIF. Perlu
	 * dicatat pula bahwa kriteria Hibernate di listener membandingkan kolom DB secara langsung,
	 * sehingga baris yang kolomnya masih {@code NULL} <b>tidak</b> lolos filter sampai getter ini
	 * sempat menyembuhkannya.</p>
	 *
	 * @return {@code true} bila kategori aktif; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menyetel penanda kategori aktif. Tanpa validasi.
	 *
	 * <p>Dipanggil dari listener {@code onCheck} checkbox "Aktif" pada grid master, yang langsung
	 * menyusulinya dengan {@code Common.refreshSaveOrUpdate(...)} &mdash; jadi perubahan tersimpan
	 * seketika tanpa tombol simpan terpisah.</p>
	 *
	 * @param aktif penanda baru; {@code null} akan dinormalkan menjadi {@code true} pada pembacaan
	 *        berikutnya
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan nomor urut tampil kategori pada form isian; makin kecil makin atas.
	 *
	 * <p>Dipakai di dua tempat: sebagai kunci {@code @OrderBy("nomorUrut asc, nama asc")} pada
	 * relasi {@code JenisCatatanPegawai.getKelompokParameterTambahanCatatanPegawais()}, dan sebagai
	 * satu-satunya kunci {@link #compareTo(GeneralValueObject)}.</p>
	 *
	 * <p><b>Getter mutatif &mdash; efek samping.</b> Field {@code null} <b>ditulisi</b> {@code 1}
	 * sebelum dikembalikan; berlaku catatan dirty-check/Envers yang sama dengan
	 * {@link #getDefaultData()}.</p>
	 *
	 * <p><b>Kuirk kode mati:</b> ekspresi kembalian {@code nomorUrut == null ? 1 : nomorUrut} sudah
	 * tidak mungkin bercabang, karena {@code null} baru saja diganti {@code 1} tepat di atasnya.</p>
	 *
	 * <p><b>Konsekuensi berat khusus kelas ini:</b> karena default-nya sama untuk semua baris
	 * ({@code 1}), kategori yang belum diberi nomor urut akan <b>seri</b>. Pada varian
	 * Alumni/Mahasiswa seri hanya berarti urutan tampil tak bermakna, tetapi di sini koleksinya
	 * adalah {@code TreeSet} sehingga baris yang seri <b>saling menelan</b> dan section-nya hilang
	 * dari form &mdash; lihat pembahasan pada dokumentasi kelas dan
	 * {@link #compareTo(GeneralValueObject)}.</p>
	 *
	 * @return nomor urut tampil; tidak pernah {@code null}
	 */
	public Integer getNomorUrut() {
		if (nomorUrut == null) {
			nomorUrut = 1;
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampil kategori. Tanpa validasi (angka negatif/duplikat diterima).
	 *
	 * <p>Dipanggil dari listener {@code onChange} pada {@code Intbox} nomor urut di grid master,
	 * yang langsung menyusulinya dengan {@code Common.refreshSaveOrUpdate(...)}.</p>
	 *
	 * @param nomorUrut nomor urut baru; {@code null} akan dinormalkan menjadi {@code 1} pada
	 *        pembacaan berikutnya
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Urutan alami kategori, meng-override {@link GeneralValueObject#compareTo(GeneralValueObject)}:
	 * membandingkan {@link #getNomorUrut()} saja.
	 *
	 * <h4>Beda dari saudara sekeluarga</h4>
	 * <p>{@link KelompokParameterTambahanAlumni} (dan varian Mahasiswa/Calon Mahasiswa) memakai
	 * bentuk dua cabang: {@code instanceof} untuk tipe yang sama, plus rantai fallback
	 * {@code nim} &rarr; {@code nama} &rarr; {@code keterangan} untuk tipe lain. Versi kelas ini
	 * <b>memangkas keduanya</b> menjadi satu baris. Konsekuensinya:</p>
	 * <ul>
	 *   <li><b>Tidak ada penjagaan tipe.</b> Cast {@code (KelompokParameterTambahanCatatanPegawai)}
	 *   dilakukan tanpa {@code instanceof}, sehingga membandingkan entity ini dengan subclass
	 *   {@link GeneralValueObject} lain melempar {@code ClassCastException}. Aman pada pemakaian
	 *   yang ada sekarang (semua koleksinya homogen), tetapi rapuh bila entity ini kelak masuk
	 *   koleksi campuran.</li>
	 *   <li><b>Tidak ada {@code getNim()}</b>, jadi pertanyaan klasik soal fallback {@code nim} pada
	 *   varian lain tidak relevan di sini.</li>
	 *   <li>Bebas NPE: {@link #getNomorUrut()} tidak pernah {@code null} &mdash; tetapi justru
	 *   karena itu setiap perbandingan <b>memicu efek samping mutatif</b> getter tersebut pada
	 *   kedua objek.</li>
	 * </ul>
	 *
	 * <h4>Tidak konsisten dengan {@code equals}</h4>
	 * <p>Dua kategori dengan {@code nomorUrut} sama menghasilkan {@code 0} walaupun {@code id}-nya
	 * berbeda. {@code compareTo} di sini karena itu <b>tidak konsisten</b> dengan
	 * {@link GeneralValueObject#equals(Object)}, dan entity ini justru dipakai di dalam
	 * {@code TreeSet} pada dua tempat: koleksi relasi
	 * {@code JenisCatatanPegawai.kelompokParameterTambahanCatatanPegawais} dan salinannya di
	 * {@code ais.action.master.CatatanPegawaiAction}. Akibatnya kategori yang nomor urutnya sama
	 * <b>tercecer secara senyap</b> dan section field kustomnya tidak pernah dirender. Karena nilai
	 * bawaan {@code nomorUrut} adalah {@code 1} untuk semua baris, keadaan ini adalah kasus
	 * <i>default</i>, bukan kasus tepi. Dicatat apa adanya &mdash; tidak diperbaiki di sini.</p>
	 *
	 * @param arg0 entity pembanding; secara praktis WAJIB bertipe
	 *        {@link KelompokParameterTambahanCatatanPegawai}
	 * @return negatif/nol/positif hasil {@code Integer.compareTo} atas nomor urut kedua kategori
	 * @throws ClassCastException bila {@code arg0} bukan {@link KelompokParameterTambahanCatatanPegawai}
	 * @throws NullPointerException bila {@code arg0} {@code null}
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		return getNomorUrut().compareTo(((KelompokParameterTambahanCatatanPegawai) arg0).getNomorUrut());
	}

	/**
	 * Mengembalikan unit kerja ({@link SatuanKerja}) pemilik kategori ini, kolom
	 * {@code satuan_kerja}.
	 *
	 * <p><b>Kolom yatim &mdash; tidak terpakai sama sekali.</b> Penelusuran seluruh codebase
	 * (Java, {@code .zul}, dan {@code .jsp}) menemukan bahwa {@code getSatuanKerja()}/
	 * {@link #setSatuanKerja(SatuanKerja)} <b>tidak pernah dipanggil dari mana pun di luar berkas
	 * ini</b>: layar master tidak menyediakan kendali untuk memilih unit kerja, dan tidak satu pun
	 * kriteria pencarian menyaring berdasarkan kolom ini. Relasi ini karena itu selalu {@code NULL}
	 * pada praktiknya. Field ini juga <b>tidak ada</b> pada saudara sekeluarga
	 * ({@link KelompokParameterTambahanAlumni} dan kawan-kawan) &mdash; tampaknya sisa rencana
	 * multi-unit yang tidak pernah dirampungkan. Dibiarkan apa adanya; menghapusnya menuntut
	 * migrasi skema.</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@link GeneralValueObject#check(Object)} lalu <b>menulis
	 * hasilnya kembali ke field</b>. {@code check()} meresolusi proxy lazy (relasi ini
	 * {@code FetchType.LAZY}) lewat cache/session/reload; bila keempat tahapnya gagal, ia
	 * mengembalikan argumen apa adanya sehingga nilai field tidak berubah. Penulisan balik ini
	 * terlihat oleh dirty-check property access, meski pada praktiknya tidak berpengaruh karena
	 * nilainya {@code NULL}.</p>
	 *
	 * <p>{@code cascade = {PERSIST, MERGE}} berarti menyimpan/menggabungkan kategori ikut
	 * menyimpan/menggabungkan {@link SatuanKerja} yang tertaut &mdash; bukan menghapusnya.</p>
	 *
	 * @return unit kerja pemilik setelah resolusi proxy, atau {@code null} (kasus normal)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Menyetel unit kerja pemilik kategori. Tanpa validasi; {@code null} diterima.
	 *
	 * <p>Tidak ada pemanggil di codebase &mdash; lihat catatan "kolom yatim" pada
	 * {@link #getSatuanKerja()}.</p>
	 *
	 * @param satuanKerja unit kerja pemilik yang baru
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

}
