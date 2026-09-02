package ais.database.model;

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

import ais.common.Common;

/**
 * Master <b>kategori/rentang penghasilan</b> (tabel {@code public.penghasilan}) &mdash; daftar acuan
 * kelas pendapatan orang tua/wali yang dipakai untuk mengkategorikan kondisi sosial-ekonomi
 * mahasiswa. Isi tabel ini bukan data pribadi siapa pun: satu baris hanya menyatakan sebuah
 * <i>rentang rupiah</i> (mis. &laquo;0 s.d 499.999&raquo;) plus keterangan bebas. Yang bersifat
 * pribadi adalah <b>relasi masuk</b> ke baris-baris ini dari entity biodata (lihat di bawah).
 *
 * <p>Kelas ini dibangkitkan {@code hbm2java} (16 Apr 2010) dan sejak itu ditambahi sendiri: kolom
 * {@link #getBatasBawah() batasBawah}/{@link #getBatasAtas() batasAtas},
 * {@link #getFeeder() feeder}, {@link #getAktif() aktif}, serta {@link #compareTo(GeneralValueObject)}
 * dan {@link #getNama()} yang di-<i>override</i> berat (lihat bagian kuirk).</p>
 *
 * <h3>Siapa yang menunjuk ke tabel ini</h3>
 *
 * <p>Entity ini adalah sisi <i>many-to-one</i> dari tiga pasang kolom di dua entity biodata
 * (masing-masing tiga slot: ayah, ibu, wali):</p>
 *
 * <ul>
 *   <li>{@link BiodataMahasiswa#getJenisPenghasilanAyah()} /
 *   {@link BiodataMahasiswa#getJenisPenghasilanIbu()} /
 *   {@link BiodataMahasiswa#getJenisPenghasilanWali()} &mdash; kolom
 *   {@code jenis_penghasilan_ayah}/{@code _ibu}/{@code _wali}.</li>
 *   <li>{@link OrangTua#getJenisPenghasilanAyah()} / {@link OrangTua#getJenisPenghasilanIbu()} /
 *   {@link OrangTua#getJenisPenghasilanWali()} &mdash; pasangan kolom kembar pada entity orang tua
 *   yang berdiri sendiri.</li>
 * </ul>
 *
 * <p>Perhatikan bahwa AIS menyimpan penghasilan orang tua dengan <b>dua cara berdampingan</b>:
 * sebagai <i>kategori</i> lewat entity ini, dan sebagai <i>angka</i> lewat kolom
 * {@code penghasilan_orang_tua} ({@link BiodataMahasiswa#getPenghasilanOrangTua()}) maupun entity
 * {@link PendapatanOrangTua}. Ketiganya tidak saling menyinkronkan.</p>
 *
 * <p>Konsumen utama lain:</p>
 * <ul>
 *   <li>{@code ais.action.master.dashboard.admin.DashboardMahasiswaPenghasilanOrtu} &mdash; dasbor
 *   &laquo;Mahasiswa per Penghasilan Orang Tua&raquo;: memuat <b>seluruh</b> baris tabel ini
 *   ({@code createCriteria(Penghasilan.class).list()}, tanpa filter {@code aktif}) untuk membentuk
 *   kolom-kolom rekap, lalu tiap sel bisa di-<i>drill down</i> ke daftar mahasiswa beserta alamat,
 *   RT/RW dan nama ketiga penghasilan orang tuanya.</li>
 *   <li>{@code ais.action.report.CommonReportHelper} &mdash; mencetak {@link #getNama()} sebagai
 *   nilai baris &laquo;Rata-rata penghasilan ayah/ibu/wali&raquo; di laporan biodata.</li>
 *   <li>Jalur Feeder/PDDIKTI (lihat bagian berikutnya).</li>
 * </ul>
 *
 * <h3>Sinkronisasi Feeder/PDDIKTI dan data awal</h3>
 *
 * <p>Kolom {@link #getFeeder()} menyimpan {@code id_penghasilan} milik PDDIKTI dan menjadi kunci
 * pencocokan lintas sistem:</p>
 *
 * <ul>
 *   <li>{@code InitDataHelper} <b>menyemai otomatis 6 baris standar</b> saat inisialisasi aplikasi
 *   (feeder 11..16: &laquo;Kurang dari Rp. 500,000&raquo;, &laquo;Rp. 500,000 - Rp. 999,999&raquo;,
 *   &laquo;Rp. 1,000,000 - Rp. 1,999,999&raquo;, &laquo;Rp. 2,000,000 - Rp. 4,999,999&raquo;,
 *   &laquo;Rp. 5,000,000 - Rp. 20,000,000&raquo;, &laquo;Lebih dari Rp. 20,000,000&raquo;). Baris
 *   hanya dibuat bila belum ada baris dengan {@code feeder} yang sama.</li>
 *   <li>{@code FeederJSONImport.penghasilan(JSONObject)} dan
 *   {@code FeederConverter.penghasilan(Node)} menarik/mengonversi daftar yang sama dari layanan
 *   Feeder. Pencocokan baris lama memakai {@code feeder} lebih dulu, baru {@code ilike} atas
 *   {@code nama}.</li>
 *   <li>{@code EksporMahasiswaFeeder} mengekspor {@link #getFeeder()} (bukan {@link #getId()})
 *   sebagai nilai kolom penghasilan ayah/ibu/wali &mdash; jadi baris buatan tangan yang
 *   {@code feeder}-nya {@code null} akan membuat ekspor ayah melempar {@code NullPointerException}
 *   (baris ayah memanggil {@code getFeeder().toString()} tanpa memeriksa null, berbeda dari baris
 *   ibu dan wali yang memeriksanya).</li>
 * </ul>
 *
 * <h3>Layar pengelolanya</h3>
 *
 * <p>{@code ais.action.master.PenghasilanAction} (turunan {@code GenericCrudAction}) melayani
 * {@code /pages/master/penghasilan.zul}. Layar ini <b>tidak punya menu sendiri</b>: satu-satunya
 * pemuatnya adalah tab &laquo;Penghasilan Ortu&raquo; di dalam {@code MahasiswaAction}
 * ({@code onPenghasilanOrtu(Event)} menyisipkannya sebagai {@code MyInclude}), sehingga hak
 * aksesnya secara praktis menempel pada menu Mahasiswa induk. Catatan keamanan yang seimbang:
 * tombol Tambah/Ubah/Hapus <b>dan</b> checkbox &laquo;Aktif&raquo; di grid semuanya digerbangi
 * ({@code add.setVisible(checkPrevilages(CREATE))}, {@code checkbox.setDisabled(!edit)}) &mdash;
 * jadi layar ini <i>bukan</i> instance pola &laquo;inversi hak akses&raquo; yang berulang di modul
 * lain; namun {@code penghasilan.zul} sendiri tidak terdaftar di whitelist
 * {@code CommonPrivilages.MUST_CHECKED}, sehingga gerbang READ hanya berlaku lewat induknya
 * ({@code mahasiswa.zul} yang memang ada di whitelist itu). Dampak kebocorannya kecil karena isi
 * tabel hanya rentang angka.
 *
 * <h3>Hal non-obvious yang perlu diketahui sebelum menyentuh kelas ini</h3>
 *
 * <ol>
 *   <li><b>{@link #getNama()} adalah getter destruktif &mdash; kolom {@code nama} tidak pernah
 *   benar-benar milik pengguna.</b> Setiap kali dibaca, getter itu <b>menimpa</b> field
 *   {@code nama} dengan string yang dibangkitkan dari {@code batasBawah}/{@code batasAtas}
 *   ({@code "<bawah> s.d <atas>"}). Karena pemetaan kelas ini memakai <i>property access</i>
 *   (anotasi menempel pada getter), nilai bangkitan itulah yang dibaca Hibernate saat
 *   dirty-check/flush &mdash; jadi sekadar <b>membaca</b> nama sebuah baris di dalam sesi yang
 *   ter-flush akan <b>menuliskannya balik ke database</b>. Konsekuensi nyata: label
 *   manusiawi yang diisi lewat form (&laquo;Nama Penghasilan *&raquo;), yang disemai
 *   {@code InitDataHelper} (&laquo;Kurang dari Rp. 500,000&raquo;), maupun yang diimpor dari
 *   Feeder ({@code nm_penghasilan}) <b>tidak pernah bertahan</b> &mdash; semuanya berubah menjadi
 *   bentuk &laquo;0 s.d 499.999&raquo; pada pembacaan pertama. Bandingkan dengan kembaran sisi
 *   sekolah {@link ais.database.model.sekolah.PenghasilanOrangTuaSiswa} yang strukturnya nyaris
 *   identik tetapi {@code getNama()}-nya polos &mdash; perilaku di sini adalah penyimpangan khas
 *   sisi perguruan tinggi, bukan pola bersama.</li>
 *   <li><b>Validasi keunikan nama praktis mandul.</b> {@code PenghasilanAction.onSave} menolak
 *   nama kosong dan memanggil {@code checkNamaPenghasilan()} yang membandingkan <i>teks yang
 *   diketik pengguna</i> dengan kolom {@code nama} di database &mdash; padahal kolom itu selalu
 *   berisi string bangkitan (butir 1). Akibatnya duplikat yang diketik hampir tak pernah
 *   terdeteksi, sementara dua baris dengan rentang yang sama persis (yang benar-benar duplikat)
 *   lolos tanpa peringatan.</li>
 *   <li><b>Baris &laquo;Lebih dari Rp. 20,000,000&raquo; tampil salah dan tersortir paling
 *   depan.</b> Data awal menyandikan rentang terbuka itu sebagai {@code batasAtas = 0},
 *   {@code batasBawah = 20000001}. {@link #getNama()} lalu membangkitkan &laquo;0 s.d
 *   20.000.001&raquo; (batas atas 0 dicetak apa adanya, bukan &laquo;tak terhingga&raquo;), dan
 *   {@link #compareTo(GeneralValueObject)} yang mengurutkan berdasarkan {@code batasAtas}
 *   menempatkan kelas <i>tertinggi</i> ini di urutan <i>paling rendah</i>.</li>
 *   <li><b>{@link #getBatasBawah()} dan {@link #getBatasAtas()} juga menulis balik.</b> Keduanya
 *   mengganti {@code null} menjadi {@code 0.0} <i>pada field</i>, sehingga nilai pengganti itu
 *   ikut ter-flush. Efek gandengannya: baris yang kedua batasnya belum diisi akan bernama
 *   &laquo;0 s.d 0&raquo; &mdash; dan semua baris seperti itu bernama sama.</li>
 *   <li><b>{@link #getAktif()} <i>tidak</i> menulis balik.</b> Berbeda dari dua butir di atas,
 *   getter ini hanya mengembalikan {@code true} untuk {@code null} tanpa menyentuh field, jadi
 *   kolom {@code aktif} tetap {@code NULL} di database. Query penyaring di layar master memang
 *   mengantisipasinya dengan {@code Restrictions.or(isNull("aktif"), eq("aktif", true))}, tetapi
 *   dasbor {@code DashboardMahasiswaPenghasilanOrtu} <b>tidak menyaring {@code aktif} sama
 *   sekali</b> &mdash; kategori yang sudah dinonaktifkan tetap muncul sebagai kolom rekap.</li>
 *   <li><b>Cabang <i>fallback</i> di {@link #compareTo(GeneralValueObject)} adalah kode mati.</b>
 *   Karena {@link #getBatasAtas()} tidak pernah mengembalikan {@code null}, perbandingan antar dua
 *   {@code Penghasilan} selalu berhenti di cabang pertama; cabang {@code nomorUrut}/{@code nim}/
 *   {@code nama}/{@code keterangan} hanya bisa tercapai bila {@code arg0} bertipe lain &mdash;
 *   tetapi dalam kasus itu {@code (Penghasilan) arg0} sudah melempar {@code ClassCastException}
 *   yang ditelan {@code catch} dan membuat method mengembalikan {@code 0}.</li>
 *   <li><b>{@link #toString()} membaca field mentah, bukan getter.</b> Ia mencetak
 *   {@code "<id>-<nama>"} dari field {@code nama} yang bisa masih {@code null} (baris yang baru
 *   dimuat dan belum pernah dibaca namanya) atau masih berisi label lama sebelum ditimpa butir 1.
 *   Jadi {@code toString()} dan {@link #getNama()} bisa memberi jawaban berbeda untuk objek yang
 *   sama.</li>
 *   <li><b>Deklarasi ulang {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} BUKAN
 *   duplikasi yang bisa dihapus.</b> {@link GeneralValueObject} adalah POJO abstrak biasa &mdash;
 *   bukan {@code @Entity} maupun {@code @MappedSuperclass} &mdash; sehingga Hibernate sama sekali
 *   tidak memetakan properti kelas induk. Setiap entity turunan wajib mendeklarasikan sendiri
 *   kolom-kolom itu agar terpetakan.</li>
 *   <li><b>Tidak ada jejak pembuat.</b> Ada {@code @PreUpdate} ({@link #onUpdate()}) tetapi tidak
 *   ada {@code @PrePersist}, jadi {@code oleh}/{@code olehId} hanya terisi saat baris di-UPDATE,
 *   bukan saat dibuat.</li>
 *   <li><b>Unggah massal bisa menimpa baris mana pun.</b> Kolom yang dipertukarkan lewat
 *   {@code Common.cetakData}/{@code Common.uploadData} mencakup {@code id}, sehingga berkas Excel
 *   yang diunggah dapat menyasar baris eksisting berdasarkan id di dalam sheet. Tombolnya memang
 *   hanya tampil bila pengguna punya CREATE+UPDATE+DELETE, tapi penjagaannya sebatas visibilitas
 *   komponen &mdash; pola yang sama dengan layar master lain (mis. {@link Propinsi}).</li>
 * </ol>
 *
 * @see GeneralValueObject
 * @see BiodataMahasiswa#getJenisPenghasilanAyah()
 * @see OrangTua#getJenisPenghasilanAyah()
 * @see PendapatanOrangTua
 * @see ais.database.model.sekolah.PenghasilanOrangTuaSiswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "penghasilan")
public class Penghasilan extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya kebetulan sama persis dengan milik
	 * {@link ais.database.model.sekolah.PenghasilanOrangTuaSiswa} &mdash; sisa salin-tempel saat
	 * kelas sekolah dibuat dari kelas ini, tidak berpengaruh apa pun karena serialisasi selalu
	 * dilakukan per kelas.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama {@code public.penghasilan.id}, {@code IDENTITY} (di-generate database). */
	private Long id;

	/** Nama pengguna terakhir yang meng-UPDATE baris ini; diisi {@link #onUpdate()}. */
	private String oleh;

	/** Id pengguna terakhir yang meng-UPDATE baris ini; diisi {@link #onUpdate()}. */
	private String olehId;

	/** @return id pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum pernah di-update */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah. Nilai {@code null}/kosong <b>diabaikan diam-diam</b> dan nilai
	 * lama dipertahankan &mdash; jejak audit tidak bisa dikosongkan lewat setter ini.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null}/kosong <b>diabaikan</b> dan nilai lama dipertahankan.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum pernah di-update */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mengisi {@code oleh}/{@code olehId}/{@code tanggal_dirubah}
	 * dari pengguna sesi berjalan lewat
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} tepat sebelum baris
	 * di-UPDATE. Tidak ada padanan {@code @PrePersist}, jadi pembuat baris tidak tercatat di
	 * kolom-kolom ini (lihat javadoc kelas). Pada baris deklarasi yang sama juga dideklarasikan
	 * field {@code tanggal_dirubah}, yang diinisialisasi ke waktu server saat objek dibuat
	 * ({@code ais.ui.util.WaktuUtil.getDate()}) sehingga baris baru tetap punya stempel waktu
	 * meski belum pernah di-update.
	 *
	 * <p>Perlu diingat bersama kuirk {@link #getNama()}: karena membaca nama saja sudah membuat
	 * baris menjadi <i>dirty</i>, UPDATE yang tak diniatkan siapa pun tetap akan memicu callback
	 * ini dan mengganti {@code oleh}/{@code tanggal_dirubah}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah stempel waktu perubahan terakhir baris ini */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return stempel waktu perubahan terakhir (kolom {@code tanggal_dirubah}, presisi TIMESTAMP) */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris: {@code "<id>-<nama>"}, mis. {@code "3-0 s.d 499.999"}.
	 *
	 * <p>Berbeda dari {@link #getNama()}, method ini membaca <b>field mentah</b> {@code nama}: ia
	 * tidak membangkitkan ulang label dari batas atas/bawah. Untuk objek yang baru dimuat dari
	 * database dan belum pernah dipanggil {@link #getNama()}-nya, isinya adalah nilai kolom apa
	 * adanya; untuk objek baru hasil {@code new Penghasilan()} isinya {@code null} sehingga
	 * tercetak {@code "null-null"}.</p>
	 *
	 * @return gabungan id dan field {@code nama} dipisah tanda hubung
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Label rentang penghasilan (kolom {@code nama}). <b>Tidak diketik pengguna</b> dalam praktiknya
	 * &mdash; selalu ditimpa {@link #getNama()}; lihat javadoc kelas.
	 */
	private String nama;

	/** Keterangan bebas (kolom {@code keterangan}); satu-satunya kolom teks yang benar-benar milik pengguna. */
	private String keterangan;

	/** Batas bawah rentang dalam rupiah (kolom {@code batasBawah}); {@code null} diperlakukan {@code 0.0}. */
	private Double batasBawah;

	/** Batas atas rentang dalam rupiah (kolom {@code batasAtas}); {@code null} diperlakukan {@code 0.0}. */
	private Double batasAtas;

	/** {@code id_penghasilan} milik PDDIKTI/Feeder (kolom {@code feeder}); kunci pencocokan sinkronisasi. */
	private Long feeder;

	/** Penanda kategori masih dipakai (kolom {@code aktif}); {@code null} dibaca sebagai {@code true}. */
	private Boolean aktif;

	/**
	 * Membandingkan dua kategori penghasilan untuk keperluan pengurutan, meng-<i>override</i>
	 * {@link GeneralValueObject#compareTo(GeneralValueObject)}.
	 *
	 * <p>Urutan utamanya adalah <b>batas atas rentang menaik</b> &mdash; itulah yang membuat daftar
	 * kategori tampil dari penghasilan terkecil ke terbesar. Rantai cadangan
	 * {@code nomorUrut} &rarr; {@code nim} &rarr; {@code nama} &rarr; {@code keterangan} disalin
	 * dari kelas induk tetapi <b>tidak pernah tercapai</b> dalam praktik: {@link #getBatasAtas()}
	 * dijamin tidak {@code null}, sehingga cabang pertama selalu diambil bila {@code arg0} memang
	 * bertipe {@code Penghasilan}; bila bukan, {@code (Penghasilan) arg0} melempar
	 * {@code ClassCastException} yang langsung ditangkap {@code catch} di bawahnya.</p>
	 *
	 * <p><b>Efek samping:</b> pemanggilan {@link #getBatasAtas()} atas kedua objek dapat menuliskan
	 * {@code 0.0} ke field yang sebelumnya {@code null} (lihat getter itu), sehingga
	 * <i>mengurutkan</i> sebuah daftar bisa membuat entity di dalamnya menjadi <i>dirty</i>.</p>
	 *
	 * <p><b>Kuirk data:</b> baris &laquo;Lebih dari Rp. 20,000,000&raquo; disemai dengan
	 * {@code batasAtas = 0}, sehingga kelas penghasilan tertinggi justru tersortir paling awal.</p>
	 *
	 * @param arg0 objek pembanding; diharapkan bertipe {@code Penghasilan}
	 * @return bilangan negatif/nol/positif sesuai kontrak {@link Comparable}; {@code 0} bila terjadi
	 *         exception (termasuk {@code ClassCastException} untuk tipe yang tidak cocok)
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		try {
			if (getBatasAtas() != null && ((Penghasilan) arg0).getBatasAtas() != null) {
				return getBatasAtas().compareTo(((Penghasilan) arg0).getBatasAtas());
			} else if (getNomorUrut() != null && arg0.getNomorUrut() != null) {
				return getNomorUrut().compareTo(arg0.getNomorUrut());
			} else if (getNim() != null && arg0.getNim() != null) {
				return getNim().compareTo(arg0.getNim());
			} else if (getNama() != null && arg0.getNama() != null) {
				return getNama().compareTo(arg0.getNama());
			} else if (getKeterangan() != null && arg0.getKeterangan() != null) {
				return getKeterangan().compareTo(arg0.getKeterangan());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Penghasilan.java:94");

		}

		return 0;
	}

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate. Semua field dibiarkan {@code null} kecuali
	 * {@code tanggal_dirubah} yang langsung diisi waktu server; objek hasil {@code new} ini dipakai
	 * form tambah di {@code PenghasilanAction.createNewEntity()} dan oleh importir Feeder.
	 */
	public Penghasilan() {
	}

	/** @return kunci utama baris ini, atau {@code null} bila belum tersimpan */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id kunci utama; normalnya hanya diisi Hibernate karena kolomnya {@code insertable = false} */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Label rentang penghasilan &mdash; <b>getter destruktif</b>: setiap pemanggilan menimpa field
	 * {@code nama} dengan string yang dibangkitkan dari batas atas/bawah, lalu mengembalikan versi
	 * ter-<i>trim</i>-nya.
	 *
	 * <p>Bentuk yang dibangkitkan adalah {@code "<lebih kecil> s.d <lebih besar>"}: bila
	 * {@link #getBatasBawah()} ternyata &ge; {@link #getBatasAtas()}, kedua nilai ditukar posisinya
	 * sehingga label selalu terbaca menaik. Angkanya diformat
	 * {@code Common.numberFormat} (locale {@code in-ID}, maksimum 3 angka desimal), jadi
	 * {@code 499999.0} tercetak {@code "499.999"}.</p>
	 *
	 * <p><b>Efek samping yang harus disadari.</b> Karena kelas ini dipetakan dengan <i>property
	 * access</i> (anotasi {@code @Id} menempel pada {@link #getId()}), Hibernate memanggil getter
	 * inilah saat dirty-check dan flush &mdash; artinya nilai bangkitan tersebut <b>tersimpan
	 * permanen ke kolom {@code nama}</b>, dan sekadar merender daftar kategori sudah cukup untuk
	 * memicu {@code UPDATE}. Semua label manusiawi (dari form, dari semaian {@code InitDataHelper},
	 * maupun {@code nm_penghasilan} hasil impor Feeder) karena itu tidak pernah bertahan lama.</p>
	 *
	 * <p>Konsekuensi lanjutan: {@code PenghasilanAction.checkNamaPenghasilan()} yang membandingkan
	 * teks ketikan pengguna dengan kolom {@code nama} praktis tidak pernah menemukan duplikat, dan
	 * beberapa baris yang batasnya sama-sama kosong akan bernama identik {@code "0 s.d 0"}.</p>
	 *
	 * <p>Catatan kecil: pemeriksaan {@code this.nama == null} di baris {@code return} adalah cabang
	 * mati &mdash; {@code nama} baru saja diisi tepat di atasnya sehingga tidak mungkin
	 * {@code null}.</p>
	 *
	 * @return label rentang hasil bangkitan, sudah di-<i>trim</i>; tidak pernah {@code null}
	 * @see #getBatasBawah()
	 * @see #getBatasAtas()
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if (getBatasBawah() >= getBatasAtas()) {
			nama = Common.numberFormat.get().format(getBatasAtas()) + " s.d " + Common.numberFormat.get().format(getBatasBawah());
		} else {
			nama = Common.numberFormat.get().format(getBatasBawah()) + " s.d " + Common.numberFormat.get().format(getBatasAtas());
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel label rentang. Praktis <b>tidak berdampak</b>: pembacaan berikutnya lewat
	 * {@link #getNama()} langsung menimpanya. Tetap dipanggil oleh form
	 * {@code PenghasilanAction.onSave}, semaian {@code InitDataHelper}, dan importir Feeder.
	 *
	 * @param nama label rentang; nilainya tidak divalidasi maupun di-<i>trim</i>
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas atas kategori ini (kolom {@code keterangan}, nullable). Berbeda dari
	 * {@link #getNama()}, kolom ini benar-benar menyimpan apa yang diketik pengguna di form dan
	 * ditampilkan apa adanya sebagai satu kolom di grid layar master.
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan bebas; boleh {@code null} */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Id kategori penghasilan menurut PDDIKTI/Feeder ({@code id_penghasilan}). Tanpa {@code @Column}
	 * sehingga jatuh ke penamaan default {@code ais.database.hibernate.MyNamingStrategy} (turunan
	 * {@code DefaultNamingStrategy}: nama kolom = nama properti apa adanya, yaitu {@code feeder}).
	 *
	 * <p>Dipakai sebagai kunci pencocokan dua arah: {@code InitDataHelper} dan
	 * {@code FeederJSONImport} mencari baris lama dengan {@code Restrictions.eq("feeder", ...)}
	 * sebelum membuat yang baru, sedangkan {@code EksporMahasiswaFeeder} mengirim nilai ini (bukan
	 * {@link #getId()}) ke Feeder. Baris yang dibuat manual lewat layar master tidak pernah mendapat
	 * nilai ini &mdash; form tidak menyediakan kolomnya &mdash; sehingga tetap {@code null} dan
	 * dapat menggagalkan ekspor penghasilan ayah dengan {@code NullPointerException}.</p>
	 *
	 * @return id Feeder, atau {@code null} untuk baris buatan sendiri
	 */
	public Long getFeeder() {
		return feeder;
	}

	/** @param feeder id kategori penghasilan menurut PDDIKTI/Feeder */
	public void setFeeder(Long feeder) {
		this.feeder = feeder;
	}

	/**
	 * Batas bawah rentang penghasilan dalam rupiah.
	 *
	 * <p><b>Menulis balik ke field:</b> bila nilainya {@code null}, getter ini mengisi field dengan
	 * {@code 0.0} sebelum mengembalikannya. Karena pemetaan memakai <i>property access</i>, nilai
	 * pengganti itu ikut ter-flush &mdash; baris hasil impor SQL langsung yang kolomnya
	 * {@code NULL} akan diam-diam tersimpan sebagai {@code 0} pada update berikutnya. Jaminan
	 * non-null inilah yang membuat perbandingan {@code >=} di {@link #getNama()} dan
	 * {@link #compareTo(GeneralValueObject)} aman dari {@code NullPointerException} saat
	 * <i>auto-unboxing</i>.</p>
	 *
	 * @return batas bawah rentang; tidak pernah {@code null}
	 */
	public Double getBatasBawah() {
		if (batasBawah == null) {
			batasBawah = 0.0;
		}
		return batasBawah;
	}

	/** @param batasBawah batas bawah rentang dalam rupiah; boleh {@code null} tetapi akan menjadi {@code 0.0} pada pembacaan pertama */
	public void setBatasBawah(Double batasBawah) {
		this.batasBawah = batasBawah;
	}

	/**
	 * Batas atas rentang penghasilan dalam rupiah. Sama seperti {@link #getBatasBawah()}, getter ini
	 * <b>menulis balik</b> {@code 0.0} ke field bila nilainya {@code null}, dan nilai pengganti itu
	 * ikut tersimpan pada flush berikutnya.
	 *
	 * <p>Nilai inilah kunci pengurutan {@link #compareTo(GeneralValueObject)}. Perhatikan kuirk data
	 * bawaan: kategori terbuka &laquo;Lebih dari Rp. 20,000,000&raquo; disemai dengan
	 * {@code batasAtas = 0} (bukan nilai tak terhingga), sehingga ia tersortir sebagai kategori
	 * terendah dan labelnya terbaca &laquo;0 s.d 20.000.001&raquo;.</p>
	 *
	 * @return batas atas rentang; tidak pernah {@code null}
	 */
	public Double getBatasAtas() {
		if (batasAtas == null) {
			batasAtas = 0.0;
		}
		return batasAtas;
	}

	/** @param batasAtas batas atas rentang dalam rupiah; boleh {@code null} tetapi akan menjadi {@code 0.0} pada pembacaan pertama */
	public void setBatasAtas(Double batasAtas) {
		this.batasAtas = batasAtas;
	}

	/**
	 * Penanda kategori masih dipakai. {@code null} dibaca sebagai {@code true} (default
	 * berpihak &laquo;aktif&raquo;), <b>tanpa</b> menuliskannya balik ke field &mdash; berbeda dari
	 * {@link #getBatasBawah()}/{@link #getBatasAtas()}, jadi kolom {@code aktif} tetap {@code NULL}
	 * di database.
	 *
	 * <p>Penyaring di layar master mengantisipasi hal itu dengan
	 * {@code Restrictions.or(isNull("aktif"), eq("aktif", true))}. Sebaliknya dasbor
	 * {@code DashboardMahasiswaPenghasilanOrtu} memuat seluruh baris tanpa menyaring kolom ini,
	 * sehingga kategori yang sudah dinonaktifkan tetap muncul di rekapnya.</p>
	 *
	 * <p>Di grid layar master nilai ini dapat diubah lewat checkbox yang langsung menyimpan
	 * ({@code Common.refreshSaveOrUpdate}) tanpa membuka form; checkbox itu dinonaktifkan bila
	 * pengguna tidak memiliki hak UPDATE.</p>
	 *
	 * @return {@code true} bila kategori aktif atau kolomnya {@code NULL}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif penanda kategori masih dipakai; {@code null} akan dibaca sebagai {@code true} */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}
}
