package ais.database.model.sekolah;

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
import ais.database.model.GeneralValueObject;

/**
 * Entity master <b>kategori/kelompok "form tambahan" (field kustom) untuk data Catatan Guru</b>,
 * dipetakan ke tabel {@code sekolah.kelompok_parameter_tambahan_catatan_guru}.
 *
 * <h3>Peran dalam modul Catatan Guru</h3>
 * <p>Catatan Guru adalah berkas riwayat/kejadian kepegawaian guru pada modul SEKOLAH (mis. surat
 * peringatan, penghargaan, pembinaan). Karena isi catatan berbeda antar yayasan/sekolah, AIS
 * mengizinkan admin menambah pertanyaan/isian sendiri tanpa mengubah skema. Rantainya
 * <b>empat lapis</b>:</p>
 * <ol>
 *   <li>{@link ais.database.model.ParameterTambahan} &mdash; <b>definisi</b> field kustom generik
 *   (label, tipe input, daftar pilihan, wajib/tidak). Dipakai bersama oleh banyak modul, termasuk
 *   modul non-sekolah.</li>
 *   <li>{@link ParameterTambahanCatatanGuru} &mdash; tabel penghubung yang <b>mengaitkan</b> sebuah
 *   {@code ParameterTambahan} ke satu baris kelas ini.</li>
 *   <li><b>Kelas ini</b> &mdash; <b>kategori/judul kelompok</b> tempat field-field tersebut
 *   ditampilkan berkelompok. Baris kelas ini menjadi heading section pada form isian catatan.</li>
 *   <li>{@link JenisCatatanGuru} &mdash; setiap jenis catatan memilih <i>kelompok mana saja</i> yang
 *   muncul, lewat relasi {@code @ManyToMany} pada
 *   {@code JenisCatatanGuru.getKelompokParameterTambahanCatatanGurus()}. Jadi kategori di sini
 *   <b>tidak</b> otomatis tampil di semua form; ia harus dicentang lebih dulu pada layar Jenis
 *   Catatan Guru ({@code ais.action.master.sekolah.JenisCatatanGuruAction}).</li>
 * </ol>
 * <p>Nilai jawaban tidak disimpan di sini, melainkan sebagai string terserialisasi pada
 * {@link CatatanGuru}; perender form-nya adalah
 * {@link ais.action.master.sekolah.helper.ParameterTambahanCatatanGuruListener}, yang merakit kunci
 * jawaban berformat {@code "<idKelompok>-><idParameter>"}.</p>
 *
 * <h3>Hubungan dengan padanan versi PT</h3>
 * <p>Kelas ini adalah <b>padanan modul SEKOLAH</b> dari
 * {@link ais.database.model.KelompokParameterTambahanCatatanPegawai} (versi perguruan tinggi).
 * Keduanya <b>identik kata-per-kata</b> pada: blok audit {@code oleh}/{@code olehId}/
 * {@code tanggal_dirubah}, {@link #toString()}, {@link #checkCreateDefault()} (termasuk nama
 * variabel lokal yang salah salin-tempel), {@link #getNama()}, {@link #getKeterangan()} (termasuk
 * pembalikan kontrak induk), ketiga getter mutatif {@link #getDefaultData()}/{@link #getAktif()}/
 * {@link #getNomorUrut()} (termasuk ternary mati di getter terakhir), serta bentuk
 * {@link #compareTo(GeneralValueObject)} satu baris tanpa fallback. Bahkan
 * {@code serialVersionUID}-nya bernilai sama persis.</p>
 * <p>Perbedaan yang benar-benar ada hanya <b>tiga</b>:</p>
 * <ul>
 *   <li><b>Skema/tabel:</b> {@code sekolah.kelompok_parameter_tambahan_catatan_guru} (versi PT:
 *   {@code public.kelompok_parameter_tambahan_catatan_pegawai}).</li>
 *   <li><b>Relasi cakupan:</b> versi PT punya satu relasi {@code satuanKerja} yang <b>yatim</b>
 *   (tidak pernah dipanggil dari mana pun). Kelas ini <b>tidak</b> punya {@code satuanKerja};
 *   sebagai gantinya ada sepasang relasi {@link #getYayasan()} + {@link #getSekolah()} yang
 *   <b>benar-benar terpakai</b> sebagai filter pencarian dan isian form pada layar masternya.</li>
 *   <li><b>Kuirk baru yang tidak ada di versi PT:</b> {@link #getYayasan()} <b>menurunkan</b> nilai
 *   yayasan dari sekolah dan menuliskannya balik ke field &mdash; lihat dokumentasi method
 *   tersebut.</li>
 * </ul>
 *
 * <h3>Auto-seed lewat {@link #checkCreateDefault()}</h3>
 * <p>Bila belum ada satu pun baris bertanda {@link #getDefaultData() defaultData}{@code =true},
 * method statis {@link #checkCreateDefault()} membuatnya sendiri dan meng-{@code commit} langsung ke
 * DB. Efeknya, sekadar <b>membuka layar</b> "Parameter Tambahan Catatan Guru" sudah menulis baris
 * master baru. Diverifikasi: <b>satu-satunya</b> pemanggil di seluruh codebase adalah
 * {@code ais.action.master.sekolah.ParameterTambahanCatatanGuruAction.doAfterCompose(Component)}
 * (baris pemanggilan tepat sesudah {@code Common.initLaguage()}). Layar master kelas ini sendiri,
 * {@code ais.action.master.sekolah.KelompokParameterTambahanCatatanGuruAction}, <b>tidak</b>
 * menanam baris bawaan tandingan &mdash; jadi bug balapan "dua kategori bawaan berbeda tergantung
 * urutan klik admin" yang ada pada {@link ais.database.model.KelompokParameterTambahanAlumni}
 * <b>TIDAK berlaku</b> di sini (sama seperti versi Pegawai).</p>
 *
 * <h3>Kuirk paling penting &mdash; koleksi bertipe {@code TreeSet} + {@link #compareTo}</h3>
 * <p>{@link #compareTo(GeneralValueObject)} kelas ini <b>hanya</b> membandingkan
 * {@link #getNomorUrut()} dan mengembalikan {@code 0} untuk nomor urut yang sama. Sementara itu
 * pemegang koleksinya adalah {@code TreeSet} &mdash; diverifikasi di <b>dua</b> tempat:</p>
 * <ul>
 *   <li>{@code JenisCatatanGuru.kelompokParameterTambahanCatatanGurus} diinisialisasi sebagai
 *   {@code new TreeSet<KelompokParameterTambahanCatatanGuru>()};</li>
 *   <li>{@code ais.action.master.sekolah.CatatanGuruAction} menyalin ulang isi relasi ke
 *   {@code new TreeSet<...>()} baru sebelum menyerahkannya ke
 *   {@code ParameterTambahanCatatanGuruListener}.</li>
 * </ul>
 * <p>{@code TreeSet} memakai {@code compareTo} sebagai definisi kesamaan, bukan
 * {@link GeneralValueObject#equals(Object)}. Karena <b>nilai bawaan {@code nomorUrut} adalah
 * {@code 1} untuk SEMUA baris</b> (lihat {@link #getNomorUrut()}) dan layar Tambah/Ubah tidak
 * menyediakan isian nomor urut sama sekali, dua kategori berbeda akan <b>saling menelan secara
 * senyap</b>: hanya satu yang bertahan di himpunan, sehingga seluruh section field kustom milik
 * kategori lainnya <b>hilang dari form</b> tanpa pesan kesalahan apa pun. Ini kondisi
 * <i>default</i>, bukan kasus tepi. Dicatat apa adanya &mdash; tidak diperbaiki di sini.</p>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 *   <li><b>Identitas &amp; audit:</b> {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()}, {@link #onUpdate()}, {@link #toString()}.</li>
 *   <li><b>Atribut kategori:</b> {@link #getNama()}, {@link #getKeterangan()},
 *   {@link #getNomorUrut()}.</li>
 *   <li><b>Penanda perilaku:</b> {@link #getDefaultData()} (penanda baris bawaan, sekaligus
 *   pengunci tombol hapus), {@link #getAktif()} (tampil/tidak pada form).</li>
 *   <li><b>Relasi cakupan:</b> {@link #getYayasan()}, {@link #getSekolah()}.</li>
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
 *   {@link #getNomorUrut()}, dan terutama {@link #getYayasan()}.</li>
 *   <li>{@code defaultData}, {@code aktif}, dan {@code nomorUrut} <b>tidak</b> punya {@code @Column}
 *   eksplisit sehingga nama kolomnya mengikuti strategi penamaan default Hibernate (nama property
 *   apa adanya, dilipat ke huruf kecil oleh PostgreSQL).</li>
 *   <li>{@code @Audited} &mdash; setiap perubahan direkam Envers ke tabel revisi.</li>
 *   <li>Komentar generator di atas deklarasi kelas berbunyi &#8220;Bank generated by hbm2java&#8221;
 *   &mdash; sisa salin-tempel dari {@code ais.database.model.Bank} yang menular ke puluhan entity
 *   AIS; kelas ini tidak ada hubungannya dengan Bank.</li>
 * </ul>
 *
 * <h3>Catatan keamanan pada layar masternya</h3>
 * <p>{@code KelompokParameterTambahanCatatanGuruAction} memasang gerbang hak akses dengan benar pada
 * tombol Tambah ({@code CREATE}), Ubah ({@code UPDATE}), Hapus ({@code DELETE}), checkbox "Aktif"
 * ({@code checkbox.setDisabled(!edit)}), dan gerbang {@code READ} di {@code doAfterCompose}
 * &mdash; <b>kecuali</b> {@code Intbox} nomor urut, yang dibuat tanpa {@code setDisabled}/
 * {@code setReadonly} apa pun lalu langsung memanggil {@code Common.refreshSaveOrUpdate(...)} pada
 * {@code onChange}. Pengguna ber-hak baca saja karena itu tetap dapat <b>mengubah dan menyimpan</b>
 * nomor urut kategori. Berpasangan dengan bug {@code TreeSet} di atas, dampaknya bukan sekadar
 * urutan tampil: menyamakan nomor urut dua kategori dapat <b>melenyapkan satu section form dari
 * tampilan semua pengguna</b>. Dicatat apa adanya &mdash; tidak diperbaiki di sini.</p>
 *
 * <p><b>Konsumen utama:</b>
 * {@code ais.action.master.sekolah.KelompokParameterTambahanCatatanGuruAction} (CRUD master),
 * {@code ais.action.master.sekolah.ParameterTambahanCatatanGuruAction} (pengaitan field ke kategori;
 * satu-satunya pemanggil {@link #checkCreateDefault()}),
 * {@code ais.action.master.sekolah.JenisCatatanGuruAction} (pemilihan kategori per jenis catatan),
 * {@link ais.action.master.sekolah.helper.ParameterTambahanCatatanGuruListener} (perender form
 * isian), {@code ais.action.master.sekolah.CatatanGuruAction} (layar transaksi catatan),
 * {@link CatatanGuru} (pembacaan jawaban), dan
 * {@code ais.action.report.format1.sekolah.LaporanCatatanGuru} (laporan).</p>
 *
 * <p><b>Konkurensi:</b> tidak thread-safe (POJO Hibernate biasa); jangan berbagi instance lintas
 * session/thread. Perhatikan pula bahwa {@code JenisCatatanGuru.mapParameters} adalah
 * {@code HashMap} <b>statis</b> yang meng-cache himpunan kategori per id jenis catatan untuk seluruh
 * JVM &mdash; instance kelas ini karena itu bisa ikut dibagikan lintas sesi pengguna.</p>
 *
 * @see ParameterTambahanCatatanGuru
 * @see JenisCatatanGuru
 * @see CatatanGuru
 * @see ais.database.model.ParameterTambahan
 * @see ais.database.model.KelompokParameterTambahanCatatanPegawai
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "kelompok_parameter_tambahan_catatan_guru")
public class KelompokParameterTambahanCatatanGuru extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai tetap agar instance lama tetap dapat dideserialisasi
	 * (relevan untuk sesi ZK yang di-passivate ke disk).
	 *
	 * <p>Nilainya <b>identik</b> dengan milik
	 * {@link ais.database.model.KelompokParameterTambahanCatatanPegawai} dan saudara sekeluarga
	 * lainnya &mdash; sisa salin-tempel; tidak berpengaruh karena berbeda kelas.</p>
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
	 * <p>Perlu diingat bahwa {@link #getDefaultData()}, {@link #getAktif()},
	 * {@link #getNomorUrut()}, dan {@link #getYayasan()} dapat mengotori field saat baris sekadar
	 * dibaca, sehingga callback ini bisa ikut terpicu pada {@code UPDATE} yang <b>tidak diminta
	 * pengguna mana pun</b> &mdash; jejak audit lalu mencatat pengguna yang kebetulan sedang membuka
	 * layar.</p>
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

	/** Nama kategori; menjadi judul section pada form isian catatan guru. Lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas kategori; ditampilkan sebagai kolom grid master. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Penanda baris bawaan hasil auto-seed. Lihat {@link #getDefaultData()}. */
	private Boolean defaultData;
	/** Penanda kategori masih dipakai/ditampilkan. Lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Nomor urut tampil kategori pada form. Lihat {@link #getNomorUrut()}. */
	private Integer nomorUrut;
	/** Sekolah pemilik kategori; {@code null} berarti berlaku untuk "Semua". Lihat {@link #getSekolah()}. */
	private Sekolah sekolah;
	/** Yayasan pemilik kategori; diturunkan dari {@link #sekolah} bila ada. Lihat {@link #getYayasan()}. */
	private Yayasan yayasan;

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
	 *   sekeluarga dengan auto-tulis default pada {@code ais.database.model.Konfigurasi}.</li>
	 *   <li><b>Menutup session milik pemanggil.</b> {@code closeSession()} mengeluarkan session dari
	 *   ThreadLocal lalu {@code clear}/{@code rollback}/{@code disconnect}/{@code close}. Kode
	 *   sesudahnya di thread yang sama akan mendapat session BARU, dan entity apa pun yang dipegang
	 *   pemanggil sebelum pemanggilan ini menjadi <b>detached</b> (koleksi lazy-nya tidak bisa
	 *   diinisialisasi lagi). Karena itu method ini aman dipanggil hanya di AWAL sebuah request,
	 *   sebagaimana dilakukan pemanggil satu-satunya.</li>
	 *   <li><b>Objek kembalian selalu detached</b> (session sudah ditutup saat {@code return}).</li>
	 *   <li>{@code begin()} akan melempar bila session ThreadLocal sudah punya transaksi aktif;
	 *   tidak ada {@code try}/{@code rollback} di sini.</li>
	 *   <li>Tidak ada penguncian/keunikan di level DB: dua request bersamaan pada instalasi kosong
	 *   dapat membuat dua baris "Form Tambahan".</li>
	 *   <li>Baris hasil seed lahir dengan {@code nomorUrut} dan {@code aktif} bernilai {@code NULL}
	 *   di DB (setter-nya tidak pernah dipanggil di sini); keduanya baru "disembuhkan" menjadi
	 *   {@code 1}/{@code true} saat pertama kali dibaca &mdash; lihat {@link #getNomorUrut()} dan
	 *   {@link #getAktif()}.</li>
	 *   <li>Baris seed juga lahir <b>tanpa</b> {@code sekolah}/{@code yayasan} ({@code NULL}), yang
	 *   pada layar master ditampilkan sebagai "Semua" &mdash; kategori bawaan karena itu berlaku
	 *   lintas seluruh yayasan/sekolah pada instalasi multi-tenant.</li>
	 *   <li>Nama variabel lokal berbunyi {@code kelompokParameterTambahanCatatanSiswa}
	 *   (&#8220;Siswa&#8221;, bukan &#8220;Guru&#8221;) &mdash; sisa salin-tempel; sama persis dengan
	 *   yang terdapat pada versi Pegawai. Tidak berpengaruh pada perilaku.</li>
	 * </ul>
	 *
	 * <h4>Pemanggil</h4>
	 * <p>Diverifikasi: satu-satunya pemanggil di seluruh codebase adalah
	 * {@code ais.action.master.sekolah.ParameterTambahanCatatanGuruAction.doAfterCompose(Component)}
	 * &mdash; layar master "Parameter Tambahan Catatan Guru". Method dipanggil tepat sesudah
	 * {@code Common.initLaguage()} dan sebelum combobox penyaring yayasan/sekolah diinisialisasi,
	 * agar daftar kategori tidak pernah kosong pada instalasi baru.</p>
	 *
	 * @return baris kategori bawaan (yang ditemukan atau yang baru saja dibuat); tidak pernah
	 *         {@code null}, tetapi dalam keadaan <b>detached</b>
	 */
	public static KelompokParameterTambahanCatatanGuru checkCreateDefault() {
		Session session = HibernateUtil.currentNativeSession();
		KelompokParameterTambahanCatatanGuru kelompokParameterTambahanCatatanSiswa = (KelompokParameterTambahanCatatanGuru) session
				.createCriteria(KelompokParameterTambahanCatatanGuru.class).add(Restrictions.eq("defaultData", true))
				.setMaxResults(1).uniqueResult();
		if (kelompokParameterTambahanCatatanSiswa == null) {
			kelompokParameterTambahanCatatanSiswa = new KelompokParameterTambahanCatatanGuru();
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
	 * <p>Dipakai langsung oleh {@link #checkCreateDefault()} dan oleh handler tombol "Tambah"
	 * ({@code onAdd}) pada {@code ais.action.master.sekolah.KelompokParameterTambahanCatatanGuruAction}.</p>
	 */
	public KelompokParameterTambahanCatatanGuru() {
	}

	/**
	 * Mengembalikan primary key baris kategori.
	 *
	 * <p>Kolom {@code id} bersifat {@code insertable=false} &mdash; nilainya dihasilkan sepenuhnya
	 * oleh sequence/identity PostgreSQL saat {@code INSERT}, jadi menyetelnya sebelum simpan tidak
	 * berpengaruh.</p>
	 *
	 * <p>Nilai ini ikut membentuk kunci jawaban pada form isian:
	 * {@link ais.action.master.sekolah.helper.ParameterTambahanCatatanGuruListener} merakit penanda
	 * {@code "<idKelompok>-><idParameter>"} untuk setiap field (penanda yang sama juga dipakai
	 * sebagai {@code jenis} lampiran), sehingga <b>mengganti id kategori berarti memutus tautan ke
	 * jawaban lama</b>.</p>
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
	 * <p>Nama inilah yang dirender sebagai judul section pada form isian catatan guru
	 * ({@code ParameterTambahanCatatanGuruListener}), sebagai label checkbox pada layar Jenis
	 * Catatan Guru, sebagai label revisi Envers pada grid master
	 * ({@code RevisiHelper.createNewRevisi(..., getNama())}), dan sebagai kunci pencarian
	 * {@code ilike ANYWHERE} pada penyaring nama. Kolom {@code nama} {@code NOT NULL} di DB dan
	 * diperiksa keunikannya di lapisan layar
	 * ({@code checkNamaKelompokParameterTambahanCatatanGuru()}).</p>
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
	 * diperiksa di lapisan layar ({@code KelompokParameterTambahanCatatanGuruAction.onSave(Event)}).
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
	 * {@code new Label(kelompokParameterTambahanCatatanGuru.getKeterangan())} tanpa penjagaan,
	 * sehingga baris tanpa keterangan menghasilkan label kosong (ZK menerima {@code null}) &mdash;
	 * bukan kegagalan, tetapi berbeda dari perilaku entity yang memakai versi induk. Dialog
	 * Tambah/Ubah sendiri sudah menjaga {@code null} secara eksplisit
	 * ({@code getKeterangan() == null ? "" : ...}).</p>
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
	 * <p>Menjadi filter nyata di {@code ParameterTambahanCatatanGuruListener}: kriteria pengambilan
	 * {@code ParameterTambahan} menyertakan
	 * {@code kelompokParameterTambahanCatatanGuru.aktif = true}, jadi menonaktifkan kategori
	 * <b>menyembunyikan seluruh field kustom di dalamnya</b> (jawaban lama tetap tersimpan, hanya
	 * tidak ditampilkan). Nilai ini juga menjadi checkbox yang bisa diubah langsung dari grid
	 * master &mdash; checkbox tersebut dijaga hak akses {@code UPDATE}
	 * ({@code checkbox.setDisabled(!edit)}).</p>
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
	 * relasi {@code JenisCatatanGuru.getKelompokParameterTambahanCatatanGurus()}, dan sebagai
	 * satu-satunya kunci {@link #compareTo(GeneralValueObject)}.</p>
	 *
	 * <p><b>Getter mutatif &mdash; efek samping.</b> Field {@code null} <b>ditulisi</b> {@code 1}
	 * sebelum dikembalikan; berlaku catatan dirty-check/Envers yang sama dengan
	 * {@link #getDefaultData()}.</p>
	 *
	 * <p><b>Kuirk kode mati:</b> ekspresi kembalian {@code nomorUrut == null ? 1 : nomorUrut} sudah
	 * tidak mungkin bercabang, karena {@code null} baru saja diganti {@code 1} tepat di atasnya.</p>
	 *
	 * <p><b>Konsekuensi berat:</b> karena default-nya sama untuk semua baris ({@code 1}) dan dialog
	 * Tambah/Ubah tidak menyediakan isian nomor urut sama sekali (hanya Nama, Yayasan, Sekolah,
	 * Keterangan), kategori yang belum pernah disunting nomor urutnya lewat grid akan <b>seri</b>.
	 * Karena koleksi pemegangnya adalah {@code TreeSet}, baris yang seri <b>saling menelan</b> dan
	 * section-nya hilang dari form &mdash; lihat pembahasan pada dokumentasi kelas dan
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
	 * <p>Satu-satunya pemanggil dari UI adalah listener {@code onChange} pada {@code Intbox} nomor
	 * urut di grid master, yang langsung menyusulinya dengan
	 * {@code Common.refreshSaveOrUpdate(...)}.</p>
	 *
	 * <p><b>Catatan keamanan:</b> {@code Intbox} tersebut dibuat <b>tanpa gerbang hak akses apa
	 * pun</b> (tidak ada {@code setDisabled}/{@code setReadonly}), berbeda dari checkbox "Aktif" dan
	 * tombol Ubah/Hapus di baris yang sama. Lihat bagian "Catatan keamanan pada layar masternya"
	 * pada dokumentasi kelas.</p>
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
	 * <h4>Beda dari implementasi induk</h4>
	 * <p>{@link GeneralValueObject#compareTo(GeneralValueObject)} memakai rantai fallback
	 * {@code nomorUrut} &rarr; {@code nim} &rarr; {@code nama} &rarr; {@code keterangan} yang
	 * dibungkus {@code try}/{@code catch}. Versi kelas ini <b>memangkas semuanya</b> menjadi satu
	 * baris &mdash; bentuk yang sama persis dengan
	 * {@link ais.database.model.KelompokParameterTambahanCatatanPegawai}. Konsekuensinya:</p>
	 * <ul>
	 *   <li><b>Tidak ada penjagaan tipe.</b> Cast {@code (KelompokParameterTambahanCatatanGuru)}
	 *   dilakukan tanpa {@code instanceof}, sehingga membandingkan entity ini dengan subclass
	 *   {@link GeneralValueObject} lain melempar {@code ClassCastException}. Aman pada pemakaian
	 *   yang ada sekarang (semua koleksinya homogen), tetapi rapuh bila entity ini kelak masuk
	 *   koleksi campuran.</li>
	 *   <li><b>Tidak ada penangkapan exception.</b> Berbeda dari induk, kegagalan di sini merambat
	 *   ke pemanggil (mis. ke {@code TreeSet.add}).</li>
	 *   <li>Bebas NPE terhadap nomor urut: {@link #getNomorUrut()} tidak pernah {@code null}
	 *   &mdash; tetapi justru karena itu setiap perbandingan <b>memicu efek samping mutatif</b>
	 *   getter tersebut pada kedua objek.</li>
	 * </ul>
	 *
	 * <h4>Tidak konsisten dengan {@code equals}</h4>
	 * <p>Dua kategori dengan {@code nomorUrut} sama menghasilkan {@code 0} walaupun {@code id}-nya
	 * berbeda. {@code compareTo} di sini karena itu <b>tidak konsisten</b> dengan
	 * {@link GeneralValueObject#equals(Object)}, dan entity ini justru dipakai di dalam
	 * {@code TreeSet} pada dua tempat: koleksi relasi
	 * {@code JenisCatatanGuru.kelompokParameterTambahanCatatanGurus} dan salinannya di
	 * {@code ais.action.master.sekolah.CatatanGuruAction}. Akibatnya kategori yang nomor urutnya
	 * sama <b>tercecer secara senyap</b> dan section field kustomnya tidak pernah dirender. Karena
	 * nilai bawaan {@code nomorUrut} adalah {@code 1} untuk semua baris, keadaan ini adalah kasus
	 * <i>default</i>, bukan kasus tepi. Dicatat apa adanya &mdash; tidak diperbaiki di sini.</p>
	 *
	 * @param arg0 entity pembanding; secara praktis WAJIB bertipe
	 *        {@link KelompokParameterTambahanCatatanGuru}
	 * @return negatif/nol/positif hasil {@code Integer.compareTo} atas nomor urut kedua kategori
	 * @throws ClassCastException bila {@code arg0} bukan {@link KelompokParameterTambahanCatatanGuru}
	 * @throws NullPointerException bila {@code arg0} {@code null}
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		return getNomorUrut().compareTo(((KelompokParameterTambahanCatatanGuru) arg0).getNomorUrut());
	}

	/**
	 * Menyetel yayasan pemilik kategori.
	 *
	 * <p><b>Kuirk:</b> objek yang {@code null} <i>atau</i> yang {@code getId()}-nya {@code null}
	 * (mis. item combobox "Semua" yang membawa instance kosong) sama-sama disimpan sebagai
	 * {@code null}. Jadi setter ini tidak pernah menyimpan entity transient/belum tersimpan, dan
	 * "tidak dipilih" terwakili seragam sebagai {@code NULL} di DB.</p>
	 *
	 * <p>Dipanggil dari {@code KelompokParameterTambahanCatatanGuruAction.onSave(Event)} dengan
	 * nilai combobox Yayasan. Perhatikan bahwa nilai yang disimpan bisa <b>tertimpa</b> pada
	 * pembacaan berikutnya bila {@link #getSekolah()} tidak {@code null} &mdash; lihat
	 * {@link #getYayasan()}.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau ber-{@code id} {@code null} disimpan sebagai
	 *        {@code null} ("Semua")
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan yayasan pemilik kategori (kolom {@code yayasan}), setelah <b>menurunkannya dari
	 * sekolah bila sekolah terisi</b>.
	 *
	 * <p>Alurnya dua tahap:</p>
	 * <ol>
	 *   <li>{@link GeneralValueObject#check(Object)} meresolusi proxy lazy (relasi ini
	 *   {@code FetchType.LAZY}) lewat cache/session/reload, lalu hasilnya <b>ditulis balik</b> ke
	 *   field;</li>
	 *   <li>bila {@link #getSekolah()} tidak {@code null}, field {@code yayasan} <b>ditimpa</b>
	 *   dengan {@code getSekolah().getYayasan()} &mdash; yayasan induk sekolah tersebut.</li>
	 * </ol>
	 *
	 * <p><b>Getter mutatif &mdash; kuirk yang TIDAK ADA pada versi PT
	 * ({@link ais.database.model.KelompokParameterTambahanCatatanPegawai} justru tidak punya relasi
	 * ini sama sekali).</b> Karena kelas ini memakai property access + {@code dynamicUpdate=true},
	 * penulisan balik pada tahap 2 terlihat oleh dirty-check: <b>sekadar membuka layar master</b>
	 * (renderer grid memanggil {@code getYayasan()} untuk setiap baris) sudah dapat memicu
	 * {@code UPDATE} kolom {@code yayasan} + revisi Envers untuk baris yang yayasannya tidak cocok
	 * dengan yayasan induk sekolahnya. Sifatnya self-healing/idempoten selama {@code sekolah} tidak
	 * berubah, tetapi tetap mengotori jejak audit &mdash; dan nilai yayasan yang <b>sengaja</b>
	 * disetel berbeda dari yayasan induk sekolah <b>mustahil bertahan</b>.</p>
	 *
	 * <p>{@code cascade = {PERSIST, MERGE}} berarti menyimpan/menggabungkan kategori ikut
	 * menyimpan/menggabungkan {@link Yayasan} yang tertaut &mdash; bukan menghapusnya.</p>
	 *
	 * <p><b>Pemakaian:</b> ditampilkan sebagai kolom pertama grid master (label "Semua" bila
	 * {@code null}) dan menjadi salah satu penyaring pencarian. Penyaringnya <b>permisif</b>:
	 * {@code Restrictions.or(isNull("yayasan"), eq(...))}, jadi kategori lintas-yayasan
	 * ({@code NULL}) selalu ikut tampil; dan bila combobox tidak dipilih, kriterianya menjadi
	 * {@code 1=1} sehingga seluruh baris lintas yayasan ditampilkan.</p>
	 *
	 * @return yayasan pemilik setelah resolusi proxy dan penurunan dari sekolah, atau {@code null}
	 *         bila kategori berlaku untuk semua yayasan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		yayasan = check(yayasan);
		if (getSekolah() != null) {
			yayasan = getSekolah().getYayasan();
		}
		return yayasan;
	}

	/**
	 * Mengembalikan sekolah pemilik kategori (kolom {@code sekolah}).
	 *
	 * <p><b>Efek samping:</b> memanggil {@link GeneralValueObject#check(Object)} lalu <b>menulis
	 * hasilnya kembali ke field</b>. {@code check()} meresolusi proxy lazy (relasi ini
	 * {@code FetchType.LAZY}) lewat cache/session/reload; bila seluruh tahapnya gagal, ia
	 * mengembalikan argumen apa adanya sehingga nilai field tidak berubah. Penulisan balik ini
	 * terlihat oleh dirty-check property access, tetapi &mdash; berbeda dari
	 * {@link #getYayasan()} &mdash; nilainya tidak pernah <i>diturunkan ulang</i> dari relasi lain,
	 * jadi tidak mengubah data.</p>
	 *
	 * <p><b>Penting:</b> method ini juga dipanggil dari dalam {@link #getYayasan()}; nilainya
	 * menentukan apakah kolom {@code yayasan} akan ditimpa. {@code null} berarti kategori berlaku
	 * untuk <b>semua</b> sekolah (ditampilkan sebagai "Semua" di grid master), dan sekaligus
	 * membebaskan {@code yayasan} dari penimpaan tersebut.</p>
	 *
	 * <p>{@code cascade = {PERSIST, MERGE}} berarti menyimpan/menggabungkan kategori ikut
	 * menyimpan/menggabungkan {@link Sekolah} yang tertaut &mdash; bukan menghapusnya.</p>
	 *
	 * <p><b>Pemakaian:</b> kolom kedua grid master dan salah satu penyaring pencarian, dengan pola
	 * permisif yang sama seperti yayasan ({@code isNull} ATAU sama dengan pilihan; {@code 1=1} bila
	 * tidak dipilih).</p>
	 *
	 * @return sekolah pemilik setelah resolusi proxy, atau {@code null} bila kategori berlaku untuk
	 *         semua sekolah
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return sekolah;
	}

	/**
	 * Menyetel sekolah pemilik kategori.
	 *
	 * <p><b>Kuirk:</b> sama seperti {@link #setYayasan(Yayasan)}, objek yang {@code null} <i>atau</i>
	 * yang {@code getId()}-nya {@code null} sama-sama disimpan sebagai {@code null}, sehingga
	 * "Semua" terwakili seragam sebagai {@code NULL} di DB dan entity transient tidak pernah ikut
	 * tersimpan lewat cascade.</p>
	 *
	 * <p>Dipanggil dari {@code KelompokParameterTambahanCatatanGuruAction.onSave(Event)} dengan
	 * nilai combobox Sekolah. Perhatikan bahwa menyetel sekolah <b>secara tidak langsung menentukan
	 * yayasan</b> pada pembacaan berikutnya &mdash; lihat {@link #getYayasan()}.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau ber-{@code id} {@code null} disimpan sebagai
	 *        {@code null} ("Semua")
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}
}
