package ais.database.model;

// Generated Dec 12, 2009 3:35:45 PM by Hibernate Tools 3.2.4.CR1

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

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;

/**
 * Master <b>kota/kabupaten</b> &mdash; tingkat ketiga (dan terakhir) hierarki wilayah
 * administratif klasik AIS ({@link Negara} &rarr; {@link Propinsi} &rarr; {@code Kota}).
 * Memetakan tabel {@code public.kota} dan dikelola lewat layar
 * {@code /pages/master/kota.zul} ({@code ais.action.master.KotaAction}, turunan
 * {@code ais.action.master.generic.GenericCrudAction}).
 *
 * <p>Kelas ini adalah pasangan langsung {@link Propinsi}: setiap baris <b>wajib</b> menunjuk
 * satu provinsi induk ({@code propinsi nullable = false}), dan seluruh sinkronisasi ke
 * hierarki {@link Wilayah} selalu menyinkronkan provinsi induknya lebih dulu (lihat
 * {@link #simpanWilayah()}). Baca javadoc kelas {@link Propinsi} untuk gambaran besar kedua
 * hierarki wilayah yang berjalan berdampingan di AIS; di sini hanya diringkas seperlunya.</p>
 *
 * <h3>Siapa yang memakai entity ini</h3>
 *
 * <ul>
 *   <li><b>Relasi {@code @ManyToOne} langsung</b> (komponen alamat): {@code BiodataMahasiswa},
 *   {@code BiodataDosen}, {@code Pegawai}, {@code OrangTua}, {@code RiwayatPendidikanDosen},
 *   {@code BiodataCalonMahasiswa} (3&times;: kota calon, kota sekolah asal, kota orang tua),
 *   {@code recruitment.CalonPegawai} (2&times;), {@code sekolah.CalonSiswa} (2&times;),
 *   {@code asset.PenyediaAsset}, {@code sisdes.Penduduk}, {@code sirs.Pasien}, dan
 *   {@code sirs.Kecamatan}.</li>
 *   <li><b>Perpanjangan hierarki khusus modul rumah sakit</b>: {@code sirs.Kecamatan}
 *   ber-{@code @ManyToOne} ke kelas ini, jadi jalur SIRS ({@code Kota} &rarr;
 *   {@code sirs.Kecamatan} &rarr; {@code sirs.Kelurahan}/Dusun) adalah satu-satunya tempat
 *   hierarki klasik berlanjut di bawah kota. Untuk jalur akademik umum, kecamatan hanya ada
 *   di pohon {@link Wilayah}.</li>
 *   <li><b>Kombo pemilih</b>: {@code Common.insertCombo(..., Kota.class, ...)} dipakai
 *   {@code KotaAction}, {@code AmbilDataKecamatanBanbox},
 *   {@code epsbed.RiwayatPendidikanDosenHelper}, {@code sirs.KecamatanAction},
 *   {@code sirs.KelurahanAction}, {@code sirs.DusunAction} dan
 *   {@code sirs.helper.AmbilDataKotaBanbox}. Kombo-kombo ini menyaring
 *   {@code aktif is null OR aktif = true}, konsisten dengan {@link #getAktif()}.</li>
 *   <li><b>Dashboard &amp; ekspor</b>: {@code dashboard.admin.DashboardMahasiswaKota}
 *   mengelompokkan mahasiswa per jurusan &times; kota.</li>
 *   <li><b>Cache preload</b>: kelas ini terdaftar di {@code InitData} sehingga isinya
 *   dimuat ke cache in-memory saat aplikasi start &mdash; yang membuat resolusi
 *   {@code check(...)} pada getter relasi di bawah biasanya tidak menyentuh database.</li>
 * </ul>
 *
 * <h3>Perbedaan penting dengan {@link Propinsi}</h3>
 *
 * <p>Kedua kelas terlihat kembar, tetapi tidak identik. Yang berbeda:</p>
 *
 * <ol>
 *   <li><b>{@link #getNama()} di sini MENULIS BALIK ke field</b>, sedangkan
 *   {@link Propinsi#getNama()} hanya mengembalikan hasil {@code trim()} tanpa menyentuh
 *   field. Getter ini memampatkan spasi ganda (tiga kali berturut-turut) lalu
 *   men-{@code trim} dan <b>menyimpan hasilnya kembali ke {@code this.nama}</b>. Lihat
 *   javadoc method itu untuk konsekuensinya.</li>
 *   <li><b>Tidak ada fallback {@code null} pada induk.</b> {@link Propinsi#getNegara()}
 *   mengganti {@code null} dengan {@code ConstantValues.INDONESIA};
 *   {@link #getPropinsi()} di sini <b>tidak</b> punya fallback apa pun dan bisa
 *   mengembalikan {@code null} meski kolomnya {@code nullable = false} (baris hasil impor
 *   SQL langsung, atau objek baru yang belum di-set).</li>
 *   <li><b>Ada relasi {@link #getWilayah()} yang sama, tetapi {@link #simpanWilayah()}
 *   mengisi {@code wilayahInduk} (relasi objek ke atas), bukan cuma {@code induk} teks</b>
 *   seperti pada provinsi &mdash; wajar, karena kota bukan simpul akar.</li>
 *   <li><b>Tidak ada {@code kodeEpsbed}.</b> {@link Propinsi} punya kolom mati
 *   {@code kode_epsbed}; kelas ini tidak.</li>
 *   <li><b>Konstruktor pintasnya menerima {@code Long id}</b>
 *   ({@link #Kota(Long)}), bukan {@code String nama} seperti
 *   {@link Propinsi#Propinsi(String)}.</li>
 * </ol>
 *
 * <h3>Hal non-obvious yang perlu diketahui sebelum menyentuh kelas ini</h3>
 *
 * <ol>
 *   <li><b>Layar daftarnya MENULIS ke database saat sekadar di-render.</b> Persis seperti
 *   {@link Propinsi}: {@code KotaAction.KotaRenderer.render(...)} memanggil
 *   {@link #simpanWilayah()} untuk <i>setiap baris</i> grid. Membuka layar master
 *   kota/kabupaten (atau membalik halaman paging-nya) dapat menyisipkan baris
 *   {@link Wilayah} baru dan meng-{@code UPDATE} kolom {@code wilayah} pada baris kota.
 *   Operasi yang terlihat "baca" sebenarnya tidak read-only.</li>
 *   <li><b>Kolom {@code kode} praktis tidak pernah terisi lewat UI.</b> Form
 *   {@code KotaAction.buildFormContent(...)} hanya menyediakan dua isian: Nama Kota dan
 *   Propinsi. Grid-nya <i>menampilkan</i> kolom "Kode Kota"
 *   ({@code sort="auto(kode)"}) tetapi tidak ada jalan untuk mengisinya dari layar itu,
 *   dan unggah Excel-nya pun hanya membaca kolom id/nama/propinsi. Satu-satunya penulis
 *   {@code kode} di seluruh codebase adalah integrasi eksternal
 *   {@code ais.common.PmbArkatama}. Akibatnya {@link #simpanWilayah()} menyalin
 *   {@code kode} yang {@code null} ke {@link Wilayah#setKode(String)}, sehingga baris
 *   {@code Wilayah} level 2 hasil sinkronisasi umumnya <b>tidak berkode</b> &mdash; padahal
 *   kode itulah yang dipakai sebagai {@code induk} tekstual oleh kecamatan di
 *   {@code AmbilDataKecamatanBanbox}.</li>
 *   <li><b>Baris bisa lahir tanpa ada orang membuka layar master.</b>
 *   {@code Common.createKotaPropinsiListenerBerdasarkanKecamatan(...)} &mdash; dipasang di
 *   form biodata mahasiswa, dosen, orang tua, calon mahasiswa (3&times;), calon pegawai,
 *   calon siswa dan penyedia asset &mdash; mencocokkan nama kabupaten dari pohon
 *   {@link Wilayah} ke tabel ini dengan {@code ilike} <b>EXACT</b>, dan
 *   <b>membuat baris {@code Kota} baru</b> bila tidak ada yang persis sama. Perhatikan
 *   asimetrinya: sisi provinsi memakai jarak Levenshtein &lt; 2 (toleran ejaan), sisi kota
 *   memakai pencocokan persis (tidak toleran) &mdash; jadi tabel ini jauh lebih rentan
 *   menumpuk baris kembar ("Kab. Malang" vs "Kabupaten Malang") daripada tabel provinsi.
 *   Baris baru itu disimpan tanpa {@code kode} dan tanpa {@code aktif}.</li>
 *   <li><b>{@code Common.autoSaveDataKotaPadaSaatRegistrasi(...)} dorman.</b> Method itu
 *   juga membuat baris {@code Kota} otomatis dan menyusun kondisi {@code ilike} lewat
 *   {@code Restrictions.sqlRestriction} dengan merangkai nama kota mentah ke dalam string
 *   SQL (hanya karakter {@code . , " ' = ?} yang dibuang lebih dulu). Satu-satunya
 *   pemanggilnya sudah dikomentari, jadi jalur ini tidak aktif &mdash; tetapi jangan
 *   dihidupkan kembali tanpa mengganti perangkaian string itu dengan parameter terikat.</li>
 *   <li><b>Deklarasi ulang {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah}
 *   BUKAN duplikasi yang bisa dihapus.</b> {@link GeneralValueObject} adalah POJO abstrak
 *   biasa &mdash; bukan {@code @Entity} maupun {@code @MappedSuperclass} &mdash; sehingga
 *   Hibernate sama sekali tidak memetakan properti kelas induk. Setiap entity turunan wajib
 *   mendeklarasikan sendiri kolom-kolom itu agar terpetakan.</li>
 *   <li><b>Tidak ada jejak pembuat.</b> Ada {@code @PreUpdate} ({@link #onUpdate()}) tetapi
 *   tidak ada {@code @PrePersist}, sehingga {@code oleh}/{@code olehId} hanya terisi saat
 *   baris di-<i>update</i>, bukan saat pertama dibuat. Riwayat lengkap tetap tersedia lewat
 *   {@code @Audited} (Hibernate Envers).</li>
 *   <li><b>Komentar generator "Jurusan generated by hbm2java" salah nama</b> &mdash; sisa
 *   salin-tempel template hbm2java (Des 2009); tidak ada hubungannya dengan
 *   {@link Jurusan}. Kuirk sejenis juga ada di ZUL-nya: judul popup tambah data tertulis
 *   "Tambah Propinsi", padahal isinya form kota.</li>
 * </ol>
 *
 * <h3>Catatan hak akses layar master</h3>
 *
 * <p>{@code KotaAction} mewarisi gerbang CREATE/UPDATE/DELETE dari
 * {@code GenericCrudAction.doAfterCompose(...)} ({@code CommonPrivilages.checkPrevilages}
 * untuk tombol Tambah serta flag {@code edit}/{@code delete}), dan checkbox "Aktif" di grid
 * ikut dimatikan lewat {@code setDisabled(!edit)} &mdash; jadi <b>tidak</b> ada inversi hak
 * akses yang lazim ditemukan di layar-layar lain. Yang tidak ada adalah gerbang
 * <b>READ</b>: {@code Common.doCheckSecurity()} hanya menegakkan READ untuk halaman yang
 * tercantum di daftar putih {@code CommonPrivilages.MUST_CHECKED}, dan
 * {@code /pages/master/kota.zul} tidak ada di daftar itu (sama seperti
 * {@code propinsi.zul}). Dashboard turunannya,
 * {@code dashboard.admin.DashboardMahasiswaKota}, juga tidak punya pemeriksaan sendiri
 * padahal tautan per-kota di sana mengunduh biodata mahasiswa lengkap (NIM, nama, alamat,
 * RT/RW, kode pos, dusun, kelurahan, IPK).</p>
 *
 * <h3>Pengelompokan anggota kelas</h3>
 *
 * <ul>
 *   <li><b>Jejak audit</b>: {@link #getOleh()}/{@link #setOleh(String)},
 *   {@link #getOlehId()}/{@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 *   <li><b>Identitas</b>: {@link #getId()}/{@link #setId(Long)}, {@link #toString()},
 *   konstruktor {@link #Kota()} dan {@link #Kota(Long)}.</li>
 *   <li><b>Atribut deskriptif</b>: {@link #getKode()}/{@link #setKode(String)},
 *   {@link #getNama()}/{@link #setNama(String)},
 *   {@link #getAktif()}/{@link #setAktif(Boolean)}.</li>
 *   <li><b>Relasi</b>: {@link #getPropinsi()}/{@link #setPropinsi(Propinsi)} (induk hierarki
 *   klasik, wajib) dan {@link #getWilayah()}/{@link #setWilayah(Wilayah)} (kembaran di
 *   hierarki {@link Wilayah}, opsional).</li>
 *   <li><b>Method bisnis</b>: {@link #simpanWilayah()} &mdash; satu-satunya method yang
 *   menulis ke database dari dalam kelas ini.</li>
 * </ul>
 *
 * <p>Tidak ada method query statis di kelas ini; seluruh pencarian/penyaringan dilakukan
 * pemanggil lewat {@code Criteria} masing-masing (lihat {@code KotaDao}, yang murni
 * generik tanpa method tambahan).</p>
 *
 * @see Propinsi
 * @see Negara
 * @see Wilayah
 * @see #simpanWilayah()
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kota")
public class Kota extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = -1414028650710502115L;
	/** Kunci utama baris kota/kabupaten (kolom {@code id}, IDENTITY). */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi oleh {@link #onUpdate()}. */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris ini; diisi oleh {@link #onUpdate()}. */
	private String olehId;

	/** @return ID pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum pernah di-update */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna pengubah. <b>Menolak diam-diam</b> nilai {@code null} maupun string
	 * kosong/spasi: nilai lama dipertahankan alih-alih ditimpa, sehingga jejak audit terakhir
	 * tidak hilang saat interceptor dipanggil tanpa konteks pengguna (mis. proses terjadwal,
	 * impor {@code PmbArkatama}, atau penulisan dari {@link #simpanWilayah()} yang dipicu
	 * renderer grid).
	 *
	 * @param olehId ID pengguna; diabaikan bila {@code null}/kosong
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
	 * Representasi teks baris: {@code "<id>-<nama>"}, mis. {@code "3573-Kota Malang"}.
	 *
	 * <p>Membaca <b>field mentah</b> {@code nama}, bukan {@link #getNama()}, sehingga spasi
	 * ganda/ekor yang belum pernah dinormalkan ikut tercetak apa adanya. Layar master
	 * menampilkan {@link #getNama()} secara eksplisit, bukan hasil method ini.</p>
	 *
	 * @return {@code id} diikuti tanda hubung dan nama kota apa adanya
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Kode kota/kabupaten (kolom {@code kode}). Praktis selalu {@code null}: tidak ada isian
	 * untuk kolom ini di layar master maupun di unggah Excel-nya &mdash; lihat javadoc kelas.
	 */
	private String kode;
	/** Nama kota/kabupaten (kolom {@code nama}, panjang 150); dipakai sebagai kunci pencocokan lintas hierarki. */
	private String nama;
	/** Provinsi induk (kolom {@code propinsi}, wajib / {@code nullable = false}). */
	private Propinsi propinsi;
	/** Kembaran baris ini di hierarki {@link Wilayah} ({@code level "2"}); diisi {@link #simpanWilayah()}. */
	private Wilayah wilayah;
	/** Penanda aktif (kolom {@code aktif}); {@code null} diperlakukan sebagai {@code true}. */
	private Boolean aktif;

	/** Konstruktor tanpa argumen yang diwajibkan Hibernate; seluruh field diisi lewat setter. */
	public Kota() {
	}

	/**
	 * Konstruktor pintasan yang hanya menyetel kunci utama &mdash; berguna untuk membentuk
	 * referensi/kriteria tanpa memuat baris dari database.
	 *
	 * <p><b>Perhatian:</b> objek hasil konstruktor ini <i>tidak</i> terisi {@code nama} maupun
	 * {@code propinsi}, jadi jangan langsung dipakai untuk {@link #simpanWilayah()} atau
	 * ditampilkan di UI. Bandingkan dengan {@link Propinsi#Propinsi(String)} yang justru
	 * menerima nama, bukan id.</p>
	 *
	 * @param id kunci utama baris kota yang diwakili
	 */
	public Kota(Long id) {
		this.id = id;
	}

	/** @return kunci utama baris ini, atau {@code null} bila belum pernah disimpan */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id kunci utama baris; normalnya diisi Hibernate (IDENTITY), bukan kode aplikasi */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama kota/kabupaten yang sudah dinormalkan.
	 *
	 * <p><b>Getter ini MENULIS BALIK ke field</b> &mdash; berbeda dari
	 * {@link Propinsi#getNama()} yang hanya membersihkan nilai kembalian. Bila {@code nama}
	 * tidak {@code null}, method memampatkan spasi ganda menjadi tunggal <b>tiga kali
	 * berturut-turut</b> (sehingga rentetan sampai delapan spasi ikut rapi) lalu
	 * men-{@code trim}-nya, dan hasilnya <i>disimpan kembali</i> ke {@code this.nama}. Nilai
	 * kembalian kemudian di-{@code trim} sekali lagi (redundan setelah langkah di atas).</p>
	 *
	 * <p><b>Konsekuensi yang perlu disadari:</b> pemetaan kelas ini memakai <i>property
	 * access</i> (anotasi menempel pada getter), jadi versi ternormalkan itulah yang dibaca
	 * Hibernate saat dirty-check/flush. Sekadar <i>membaca</i> nama sebuah kota &mdash;
	 * misalnya saat merender grid, mengisi kombo, atau mencetak laporan &mdash; sudah cukup
	 * untuk membuat baris tampak "kotor" dan ter-{@code UPDATE} pada flush berikutnya, tanpa
	 * ada pengguna yang menyunting apa pun. Nama yang sengaja diketik dengan spasi ganda tidak
	 * bisa dipertahankan.</p>
	 *
	 * <p>Normalisasi ini juga menjelaskan mengapa pencocokan {@code ilike EXACT} di
	 * {@code Common.createKotaPropinsiListenerBerdasarkanKecamatan(...)} biasanya cocok
	 * meskipun sumber datanya berspasi berlebih.</p>
	 *
	 * @return nama kota/kabupaten tanpa spasi ganda dan tanpa spasi tepi, atau {@code null}
	 *         bila kolomnya memang kosong
	 */
	@Column(name = "nama", length = 150)
	public String getNama() {
		if (nama != null) {
			nama = org.apache.commons.lang3.StringUtils.replace(nama, "  ", " ");
			nama = org.apache.commons.lang3.StringUtils.replace(nama, "  ", " ");
			nama = org.apache.commons.lang3.StringUtils.replace(nama, "  ", " ");
			nama = nama.trim();
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama kota/kabupaten apa adanya, <b>tanpa</b> normalisasi &mdash; pembersihan
	 * baru terjadi pada pembacaan pertama lewat {@link #getNama()}.
	 *
	 * @param nama nama kota/kabupaten (maksimal 150 karakter)
	 */
	public void setNama(String nama) {
		this.nama = nama;

	}

	/**
	 * Provinsi induk baris ini pada hierarki klasik.
	 *
	 * <p>Sebelum dikembalikan, nilainya dilewatkan {@code check(...)} milik
	 * {@link GeneralValueObject} untuk meresolusi proxy lazy (urutan: flag {@code initData},
	 * cache preload {@code ConstantValues}, inisialisasi lewat session berjalan, lalu reload
	 * lewat session baru) dan hasilnya ditulis balik ke field. Ini <b>bukan</b> getter
	 * destruktif: yang berubah hanya representasi objek yang sama, bukan isinya.</p>
	 *
	 * <p><b>Bisa mengembalikan {@code null}</b> meski kolomnya {@code nullable = false}
	 * &mdash; tidak ada fallback seperti {@link Propinsi#getNegara()}. Pemanggil di seluruh
	 * codebase memang menjaga diri dengan pemeriksaan {@code null} eksplisit (mis.
	 * {@code KotaAction.KotaRenderer}), tetapi {@link #simpanWilayah()} <b>tidak</b> &mdash;
	 * lihat catatan di sana.</p>
	 *
	 * @return provinsi induk, atau {@code null} bila kolomnya kosong/tak terisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "propinsi", nullable = false)
	public Propinsi getPropinsi() {
		propinsi = check(propinsi);
		return this.propinsi;
	}

	/** @param propinsi provinsi induk kota ini; wajib terisi sebelum baris disimpan */
	public void setPropinsi(Propinsi propinsi) {
		this.propinsi = propinsi;
	}

	/**
	 * Kode kota/kabupaten. Tidak dinormalkan dan tidak punya nilai pengganti.
	 *
	 * <p>Nyaris selalu {@code null} pada data yang lahir dari AIS sendiri: layar master tidak
	 * menyediakan isian kode, unggah Excel-nya hanya membaca id/nama/propinsi, dan jalur
	 * pembuatan otomatis (listener kecamatan) juga tidak mengisinya. Satu-satunya penulis
	 * adalah integrasi eksternal {@code ais.common.PmbArkatama}. Nilai ini disalin apa adanya
	 * ke {@link Wilayah#setKode(String)} oleh {@link #simpanWilayah()}.</p>
	 *
	 * @return kode kota, umumnya {@code null}
	 */
	public String getKode() {
		return kode;
	}

	/** @param kode kode kota/kabupaten; dalam praktik hanya diisi integrasi {@code PmbArkatama} */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Kembaran baris ini di hierarki {@link Wilayah} ({@code level "2"} = kota/kabupaten).
	 *
	 * <p>Seperti {@link #getPropinsi()}, nilainya dilewatkan {@code check(...)} untuk
	 * meresolusi proxy lazy dan ditulis balik ke field &mdash; bukan efek destruktif.</p>
	 *
	 * <p>Kolomnya {@code nullable = true}: baris yang belum pernah melewati
	 * {@link #simpanWilayah()} akan mengembalikan {@code null}. Nilai ini dipakai
	 * {@code AmbilDataKecamatanBanbox} sebagai {@code wilayahInduk} saat membuat kecamatan
	 * baru, jadi kota tanpa pasangan {@code Wilayah} tidak bisa punya kecamatan.</p>
	 *
	 * @return baris {@code Wilayah} level 2 pasangan kota ini, atau {@code null} bila belum
	 *         pernah disinkronkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "wilayah", nullable = true)
	public Wilayah getWilayah() {
		wilayah = check(wilayah);
		return wilayah;
	}

	/** @param wilayah baris {@code Wilayah} level 2 pasangan kota ini; normalnya hanya diisi {@link #simpanWilayah()} */
	public void setWilayah(Wilayah wilayah) {
		this.wilayah = wilayah;
	}

	/**
	 * Penanda kota aktif. <b>Mengubah {@code null} menjadi {@code true}</b>: baris lama atau
	 * hasil pembuatan otomatis yang tidak pernah menyetel kolom ini tetap dianggap aktif.
	 *
	 * <p>Nilai pengganti tersebut tidak ditulis balik ke field, tetapi karena pemetaan kelas
	 * ini memakai <i>property access</i>, nilai itulah yang dibaca Hibernate saat flush
	 * &mdash; sehingga kolom {@code aktif} yang semula {@code NULL} akan diam-diam menjadi
	 * {@code true} pada {@code UPDATE} berikutnya.</p>
	 *
	 * <p>Query kombo di seluruh aplikasi memakai
	 * {@code Restrictions.or(isNull("aktif"), eq("aktif", true))} sehingga konsisten dengan
	 * getter ini; namun layar master sendiri <b>tidak</b> menyaring {@code aktif}, jadi kota
	 * nonaktif tetap terlihat di sana.</p>
	 *
	 * @return {@code true} bila kota aktif atau kolomnya kosong; {@code false} hanya bila
	 *         eksplisit dinonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * @param aktif status aktif kota; disetel dari checkbox "Aktif" di grid layar master
	 *              (yang ikut dimatikan bila pengguna tidak punya hak {@code UPDATE})
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Menyinkronkan baris kota ini ke <b>hierarki {@link Wilayah}</b> secara malas
	 * (<i>lazy</i>): memastikan provinsi induknya sudah punya kembaran {@code Wilayah}, lalu
	 * memastikan ada satu baris {@code Wilayah} ber-{@code level "2"} yang mewakili kota ini,
	 * dan menautkannya balik ke kolom {@code wilayah}.
	 *
	 * <p><b>Mengapa perlu.</b> AIS menyimpan wilayah administratif di dua tempat (lihat
	 * javadoc {@link Propinsi}). Tabel {@code propinsi}/{@code kota} adalah master lama,
	 * sedangkan tabel {@code wilayah} adalah pohon bergaya feeder yang satu-satunya menampung
	 * kecamatan. Method ini adalah jembatan arah <i>kota &rarr; wilayah</i>; tanpa dipanggil,
	 * kota yang baru dibuat tidak akan pernah bisa dipilih sebagai induk kecamatan di
	 * {@code AmbilDataKecamatanBanbox}.</p>
	 *
	 * <p><b>Alur kerja</b> (semuanya di dalam {@code HibernateUtil.currentSession()}):</p>
	 * <ol>
	 *   <li>{@link Propinsi#simpanWilayah()} dipanggil lebih dulu, supaya
	 *   {@code propinsi.getWilayah()} dijamin sudah ada dan bisa dipakai sebagai
	 *   {@code wilayahInduk}. Inilah alasan urutan pemanggilan di
	 *   {@code AmbilDataKecamatanBanbox} selalu provinsi dulu, baru kota.</li>
	 *   <li>Bila {@link #getWilayah()} sudah terisi, kandidat langsung dipakai; bila belum,
	 *   dicari baris {@code Wilayah} dengan {@code wilayahInduk = }{@code propinsi.getWilayah()},
	 *   {@code level = "2"} dan {@code nama ILIKE} nama kota ini
	 *   ({@code ConstantValues.simpleObject(...)}, maksimal 1 hasil).</li>
	 *   <li>Bila tetap tidak ketemu, dibuat baris {@code Wilayah} baru:
	 *   {@code induk} = kode {@code Wilayah} provinsi (atau string kosong bila provinsi belum
	 *   punya kembaran), {@code kode}/{@code nama} disalin dari kota,
	 *   {@code wilayahInduk} = {@code Wilayah} provinsi, {@code negara} = kode negara
	 *   provinsi, {@code level = "2"}; lalu {@code session.save(...)} +
	 *   {@code session.flush()}.</li>
	 *   <li>Bila kolom {@code wilayah} kota ini masih kosong, hasil di atas ditautkan lewat
	 *   {@link #setWilayah(Wilayah)} dan baris kota di-UPDATE lewat
	 *   {@code Common.refreshUpdate(session, kota)} (yang juga melakukan flush dan memperbarui
	 *   identity-map).</li>
	 * </ol>
	 *
	 * <p><b>Efek samping: method ini MENULIS ke database</b> &mdash; sisip baris
	 * {@code wilayah} (bisa dua sekaligus: provinsi lewat langkah 1, lalu kota) dan/atau
	 * update baris {@code propinsi} dan {@code kota}. Karena
	 * {@code KotaAction.KotaRenderer.render(...)} memanggilnya untuk setiap baris grid,
	 * sekadar <i>membuka</i> layar master kota sudah bisa memicu penulisan.</p>
	 *
	 * <p><b>Dipanggil dari:</b> {@code ais.action.master.KotaAction} (saat simpan
	 * {@code onSave}, saat setiap baris grid dirender, dan pada setiap baris hasil unggah
	 * Excel) serta {@code ais.action.master.helper.AmbilDataKecamatanBanbox} saat pengguna
	 * menambah kecamatan baru dari dialog pemilih.</p>
	 *
	 * <p><b>Kuirk dan keterbatasan yang perlu diketahui:</b></p>
	 * <ul>
	 *   <li><b>Seluruh exception ditelan</b> &mdash; blok {@code catch} hanya mencetak stack
	 *   trace dan mencatatnya ke {@code ErrorAuditUtil}. Kegagalan sinkronisasi tidak pernah
	 *   sampai ke pemanggil, sehingga kota bisa saja tetap tanpa pasangan {@code Wilayah}
	 *   tanpa ada pesan kesalahan. Ini sekaligus menyembunyikan {@code NullPointerException}
	 *   yang pasti terjadi bila {@link #getPropinsi()} mengembalikan {@code null} (baris
	 *   dipanggil tanpa pemeriksaan) atau bila provinsi induk tidak punya
	 *   {@link Propinsi#getNegara()} yang bisa diresolusi.</li>
	 *   <li><b>{@code kode} yang disalin biasanya {@code null}</b> karena kolom {@code kode}
	 *   kota praktis tidak pernah terisi (lihat javadoc kelas dan {@link #getKode()}).
	 *   Baris {@code Wilayah} level 2 hasil sinkronisasi karena itu umumnya tak berkode,
	 *   sedangkan kecamatan yang dibuat di bawahnya menyalin kode itu ke kolom {@code induk}
	 *   tekstualnya &mdash; menghasilkan {@code induk} kosong.</li>
	 *   <li><b>Pencocokan memakai NAMA, bukan kode.</b> Baris {@code Wilayah} lama akan
	 *   dipungut bila namanya sama (tidak peka besar-kecil huruf) walaupun kodenya berbeda;
	 *   sebaliknya kota yang ejaannya sedikit berbeda akan memunculkan baris {@code Wilayah}
	 *   kembar. Perhatikan pula bahwa {@link #getNama()} menormalkan spasi <i>dan menulis
	 *   balik</i>, jadi pemanggilan method ini bisa ikut memicu {@code UPDATE} pada kolom
	 *   {@code nama}.</li>
	 *   <li><b>Tidak ada penyaringan {@code aktif}</b> pada pencarian kandidat, sehingga baris
	 *   {@code Wilayah} nonaktif pun bisa terpilih.</li>
	 *   <li><b>Pencarian dibatasi ke satu provinsi.</b> Predikat
	 *   {@code wilayahInduk = propinsi.getWilayah()} berarti kota bernama sama di provinsi
	 *   berbeda tetap mendapat baris {@code Wilayah} masing-masing &mdash; benar secara
	 *   semantik, tetapi juga berarti kesalahan pemetaan provinsi akan menggandakan simpul
	 *   pohon wilayah.</li>
	 *   <li><b>Berbeda dari {@link Propinsi#simpanWilayah()}</b>, di sini {@code wilayahInduk}
	 *   (relasi objek ke atas) ikut diisi, bukan hanya {@code induk} bertipe teks &mdash;
	 *   sehingga penelusuran pohon ke atas ({@code Wilayah.getWilayahInduk()}) hanya bekerja
	 *   mulai level 2 ke bawah.</li>
	 * </ul>
	 */
	public void simpanWilayah() {
		try {
			Kota kota = this;
			kota.getPropinsi().simpanWilayah();
			Session session = HibernateUtil.currentSession();
			Wilayah wilayah = kota.getWilayah();
			if (wilayah == null) {
				wilayah = (Wilayah) ConstantValues.simpleObject(session.createCriteria(Wilayah.class)
						.add(Restrictions.eq("wilayahInduk", kota.getPropinsi().getWilayah()))
						.add(Restrictions.eq("level", "2")).add(Restrictions.ilike("nama", kota.getNama()))
						.setMaxResults(1), Wilayah.class);
			}

			if (wilayah == null) {
				wilayah = new Wilayah();
				wilayah.setInduk(
						kota.getPropinsi().getWilayah() == null ? "" : kota.getPropinsi().getWilayah().getKode());
				wilayah.setKode(kota.getKode());
				wilayah.setNama(kota.getNama());
				wilayah.setWilayahInduk(kota.getPropinsi().getWilayah());
				wilayah.setNegara(kota.getPropinsi().getNegara().getKode());
				wilayah.setLevel("2");
				session.save(wilayah);
				session.flush();
			}

			if (kota.getWilayah() == null) {
				kota.setWilayah(wilayah);
				Common.refreshUpdate(session, kota);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Kota.java:189");
		}
	}
}
