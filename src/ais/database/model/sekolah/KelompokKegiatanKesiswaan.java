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
 * Entity master <b>kategori (aspek) kegiatan kesiswaan</b>, tabel
 * {@code public.kelompok_kegiatan_kesiswaan}. Satu baris mewakili satu rumpun besar kegiatan siswa
 * &mdash; misalnya data bawaan {@code "Keagamaan dan moral pancasila"} yang disemai
 * {@code ais.common.InitDataHelper} &mdash; yang di bawahnya digantungi rincian kegiatan konkret.
 *
 * <p>Class ini adalah <b>padanan versi sekolah</b> dari
 * {@link ais.database.model.KelompokKegiatanKemahasiswaan} (versi perguruan tinggi). Keduanya, plus
 * {@code ais.database.model.KelompokKegiatanKedosenan} (sisi dosen), adalah tiga salinan struktur
 * yang nyaris identik kata-per-kata; perbedaan nyatanya dirangkum di bagian
 * <i>"Perbedaan dari versi PT"</i> di bawah.</p>
 *
 * <h2>Posisi dalam hierarki master kegiatan kesiswaan</h2>
 *
 * <p>Modul kegiatan kesiswaan memakai klasifikasi <b>tiga tingkat</b>, dan class ini berada di
 * tingkat tengah:</p>
 *
 * <ol>
 *   <li>{@link JenisKelompokKegiatanKesiswaan} &mdash; tingkat teratas. Ditunjuk dari sini lewat
 *       {@link #getJenisKelompokKegiatanKesiswaan()} dan bersifat <b>wajib</b>
 *       ({@code nullable = false});</li>
 *   <li><b>class ini</b> &mdash; rumpun/aspek kegiatan;</li>
 *   <li>{@link DetailKelompokKegiatanKesiswaan} &mdash; rincian di bawah baris ini (mis.
 *       {@code "PHBI"}, {@code "PHBN"}). Relasinya <b>satu arah dari anak</b>: entity anak yang
 *       menyimpan properti {@code kelompokKegiatanKesiswaan}; class ini <b>tidak</b> punya koleksi
 *       balik, sehingga daftar anak selalu diambil lewat query
 *       ({@code Restrictions.eq("kelompokKegiatanKesiswaan", ...)}, lihat
 *       {@code ais.action.master.sekolah.helper.DetailKelompokKegiatanKesiswaanHelper}).</li>
 * </ol>
 *
 * <p>Konsumen akhir hierarki ini adalah {@link KegiatanKesiswaan}, yang menyimpan <b>dua</b>
 * referensi sekaligus &mdash; ke class ini
 * ({@code KegiatanKesiswaan.getKelompokKegiatanKesiswaan()}, kolom
 * {@code kelompok_kegiatan_kesiswaan}, wajib) <i>dan</i> ke
 * {@link DetailKelompokKegiatanKesiswaan} &mdash; jadi tingkat 2 di-denormalisasi ulang di baris
 * kegiatan, bukan ditelusuri lewat anaknya. Angka kredit ({@link NilaiKegiatanKesiswaan}) justru
 * digantungkan ke tingkat 3 bersama {@link JabatanKegiatanKesiswaan} dan
 * {@link SkalaKegiatanKesiswaan}, bukan ke baris ini.</p>
 *
 * <h2>PERINGATAN KOSAKATA: label layar membalik istilah class</h2>
 *
 * <p>Nama class dan label UI <b>tidak sejajar</b>; jangan menyimpulkan tingkat hierarki dari kata
 * "Kelompok" pada layar:</p>
 *
 * <ul>
 *   <li>entity ini di layar disebut <b>"Aspek Kegiatan Kesiswaan"</b> &mdash; judul jendela
 *       {@code "Tambah/Ubah Aspek Kegiatan Kesiswaan"}, kolom grid
 *       {@code "Nama Aspek Kegiatan Kesiswaan"}, filter {@code "Nama Aspek Kegiatan"}, dan tab
 *       pertama layar berlabel {@code "Aspek Kegiatan Kesiswaan"};</li>
 *   <li>sebutan <b>"Kelompok Kegiatan Kesiswaan"</b> / <b>"Kelompok Aspek"</b> /
 *       <b>"Kelompok Aspek Kegiatan"</b> di layar justru menunjuk <b>induk</b> baris ini, yaitu
 *       {@link JenisKelompokKegiatanKesiswaan} &mdash; termasuk label combobox pada form
 *       tambah/ubah dan pesan validasi {@code "Kelompok Aspek Kesiswaan harus diisi"};</li>
 *   <li>di {@link KegiatanKesiswaan}, combobox untuk properti ini dan combobox untuk
 *       {@link DetailKelompokKegiatanKesiswaan} dipasang berpasangan (pilihan rincian disaring
 *       ulang setiap kali aspek berubah).</li>
 * </ul>
 *
 * <h2>Pengelompokan anggota</h2>
 *
 * <ul>
 *   <li><b>Identitas</b> &mdash; {@link #getId()}, {@link #getNama()}, {@link #getKeterangan()},
 *       {@link #toString()};</li>
 *   <li><b>Klasifikasi</b> &mdash; {@link #getJenisKelompokKegiatanKesiswaan()};</li>
 *   <li><b>Kendali tampil/pilih</b> &mdash; {@link #getAktif()}, {@link #getBisaDipilihSiswa()},
 *       {@link #getNomorUrut()};</li>
 *   <li><b>Angka penilaian (dorman)</b> &mdash; {@link #getBobot()},
 *       {@link #getNilaiMinimal()};</li>
 *   <li><b>Jejak audit</b> &mdash; {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 * </ul>
 *
 * <h2>Hal non-obvious</h2>
 *
 * <ol>
 *   <li><b>Nama kolom FK menyesatkan &mdash; bug salin-tempel yang sama dengan versi PT dan versi
 *       dosen.</b> Relasi ke {@link JenisKelompokKegiatanKesiswaan} dipetakan ke kolom bernama
 *       {@code skala_kegiatan_kesiswaan} (lihat {@link #getJenisKelompokKegiatanKesiswaan()}).
 *       Padahal ada entity <i>lain</i> yang benar-benar bernama {@link SkalaKegiatanKesiswaan}
 *       (skala lokal/nasional/internasional dan sejenisnya), dan entity itu <b>tidak berelasi sama
 *       sekali</b> dengan class ini &mdash; skala digantungkan ke
 *       {@link DetailKelompokKegiatanKesiswaan} lewat many-to-many
 *       ({@code detail_kelompok_has_skala_kegiatan_kesiswaan}) serta ke
 *       {@link KegiatanKesiswaan}/{@link NilaiKegiatanKesiswaan} lewat kolom terpisah yang
 *       <i>kebetulan bernama sama</i>. Nama kolom di sini murni sisa salin-tempel; jangan dijadikan
 *       petunjuk semantik saat membaca skema database.</li>
 *   <li><b>Tabel ini tinggal di schema {@code public}, bukan {@code sekolah}.</b> Hampir seluruh
 *       kerabatnya di paket ini dipetakan ke {@code schema = "sekolah"} &mdash;
 *       {@link JenisKelompokKegiatanKesiswaan}, {@link SkalaKegiatanKesiswaan},
 *       {@link JabatanKegiatanKesiswaan}, {@link KegiatanKesiswaan},
 *       {@link NilaiKegiatanKesiswaan} &mdash; sedangkan class ini dan
 *       {@link DetailKelompokKegiatanKesiswaan} memakai {@code schema = "public"}, persis mengikuti
 *       versi PT yang memang berada di {@code public}. Akibat praktisnya: satu hierarki tiga
 *       tingkat terbelah di dua schema, dan foreign key-nya menyeberang schema. Dicatat apa adanya,
 *       bukan anjuran perubahan.</li>
 *   <li><b>{@link #getKeterangan()} membalik kontrak base class.</b>
 *       {@link GeneralValueObject#getKeterangan()} menormalkan {@code null} menjadi {@code ""};
 *       override di sini mengembalikan nilai mentah sehingga {@code null} bisa lolos ke pemanggil.
 *       Detail dan dampaknya dijelaskan di {@link #getKeterangan()}.</li>
 *   <li><b>{@link #getBobot()} dan {@link #getNilaiMinimal()} praktis write-only.</b> Keduanya bisa
 *       diisi lewat grid layar masternya (dan lewat impor Excel), tetapi tidak ada satu pun
 *       perhitungan, laporan, dasbor, atau validasi di codebase yang membacanya kembali &mdash;
 *       satu-satunya pembaca adalah grid yang menampilkannya untuk disunting lagi.</li>
 *   <li><b>{@link #getAktif()} dan {@link #getBisaDipilihSiswa()} justru benar-benar ditegakkan</b>
 *       (kebalikan dari dua field di atas) &mdash; keduanya dipakai sebagai {@code Restrictions}
 *       saat mengisi combobox aspek kegiatan di
 *       {@code ais.action.master.sekolah.KegiatanKesiswaanAction}. Contoh positif.</li>
 *   <li><b>Properti induk sengaja dideklarasikan ulang.</b> {@link GeneralValueObject} adalah POJO
 *       abstrak biasa &mdash; <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} &mdash;
 *       sehingga Hibernate tidak memetakan properti apa pun miliknya. Deklarasi ulang
 *       {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} di sini adalah
 *       <b>keharusan teknis</b>, bukan duplikasi yang perlu "dibersihkan".</li>
 * </ol>
 *
 * <h2>Perbedaan dari versi PT ({@link ais.database.model.KelompokKegiatanKemahasiswaan})</h2>
 *
 * <ol>
 *   <li><b>Struktur field/kolom: identik.</b> Sembilan properti yang sama, urutan deklarasi yang
 *       sama, {@code serialVersionUID} yang sama ({@code 2463821577548439808L}), bahkan bug nama
 *       kolom FK yang sama. Satu-satunya penggantian nama adalah {@code bisaDipilihMahasiswa} menjadi
 *       {@link #getBisaDipilihSiswa()}.</li>
 *   <li><b>Auto-seed versi sekolah lebih tahan banting untuk induk.</b> Versi PT mengambil induknya
 *       dari variabel lokal yang <i>hanya</i> terisi bila tabel jenis masih kosong, sehingga pada
 *       basis data yang tabel jenisnya sudah terisi tetapi tabel kelompoknya masih kosong,
 *       penyemaian memanggil setter induk dengan {@code null} dan gagal saat flush. Versi sekolah
 *       <b>selalu</b> mencari baris {@code "Kelompok Utama"} lewat query lebih dulu (dan hanya
 *       membuatnya bila belum ada), jadi jebakan itu tidak berlaku di sini.</li>
 *   <li><b>Tetapi versi sekolah punya bug seed-nya sendiri</b> yang tidak ada di versi PT: lihat
 *       {@link #getJenisKelompokKegiatanKesiswaan()} &mdash; baris jenis
 *       {@code "Kelompok Penunjang"} tidak pernah benar-benar tersemai untuk modul kesiswaan.</li>
 *   <li><b>Layar masternya berdiri sendiri sebagai tabbox</b> (menampung tab bersarang
 *       {@code "Kelompok Aspek"}, {@code "Jabatan/Status/Tugas"}, {@code "Skala"},
 *       {@code "Angka Kredit"}), sementara di versi PT layar aspek adalah tab <i>di dalam</i> layar
 *       kegiatan. Layar sekolah ini pada gilirannya juga disisipkan sebagai tab dari
 *       {@code kegiatan_kesiswaan.zul}, jadi tabbox-nya bersarang dua tingkat.</li>
 * </ol>
 *
 * <h2>Layar dan hak akses</h2>
 *
 * <p>Layar masternya {@code /pages/master/sekolah/kelompok_kegiatan_kesiswaan.zul} dengan action
 * {@code ais.action.master.sekolah.KelompokKegiatanKesiswaanAction}. Ada pula jalur UI baru
 * berbasis JSP ({@code /WEB-INF/new/sekolah/uiux/kelompok_kegiatan_kesiswaan.jsp} beserta
 * {@code _service.jsp}-nya) yang merupakan scaffold hasil generator dan mendelegasikan seluruh
 * kerjanya ke {@code /WEB-INF/new/_shared/services/dispatcher.jsp}.</p>
 *
 * <p><b>Catatan hak akses (dicatat apa adanya, bukan anjuran perubahan di berkas ini):</b></p>
 *
 * <ul>
 *   <li>{@code KelompokKegiatanKesiswaanAction} memasang gerbang READ lewat
 *       {@code Common.doCheckSecurity()} di {@code doBeforeCompose}, tetapi seluruh gerbang
 *       CREATE/UPDATE/DELETE-nya <b>dikomentari mati</b> ({@code CommonPrivilages.CREATE},
 *       {@code UPDATE}, {@code DELETE} semuanya nonaktif) &mdash; sama persis dengan versi PT.
 *       Akibatnya tombol Tambah/Ubah/Hapus, tombol unggah Excel massal, serta penyuntingan langsung
 *       di grid untuk {@link #getNomorUrut()}, {@link #getBobot()}, {@link #getNilaiMinimal()},
 *       {@link #getAktif()}, dan {@link #getBisaDipilihSiswa()} terbuka bagi siapa pun yang bisa
 *       membuka layarnya;</li>
 *   <li>helper rincian {@code DetailKelompokKegiatanKesiswaanHelper} (493 baris) <b>tidak memuat
 *       satu pun</b> pemanggilan {@code checkPrevilages}/{@code doCheckSecurity}, sehingga tombol
 *       "Tambah Rincian Aspek" dan aksi ubah/hapus rincian di panel detail juga tanpa syarat &mdash;
 *       pola yang identik dengan temuan pada helper detail modul lain;</li>
 *   <li>entity ini <b>tidak punya kolom {@code sekolah} maupun {@code yayasan}</b>, jadi daftarnya
 *       global untuk seluruh instalasi. {@code initCriteria()} karenanya juga tidak memfilter
 *       tenant apa pun. Ini bukan kasus <i>fail-open</i> (tidak ada cakupan yang bisa gagal-terbuka)
 *       melainkan konsekuensi desain master bersama: pada instalasi multi-yayasan, seluruh sekolah
 *       memakai dan menyunting daftar aspek yang sama;</li>
 *   <li>sebagai imbangan, pencarian di layar itu memakai {@code Restrictions.ilike} terparameter
 *       &mdash; tidak ada perakitan SQL mentah dari input pengguna.</li>
 * </ul>
 *
 * @see ais.database.model.KelompokKegiatanKemahasiswaan
 * @see JenisKelompokKegiatanKesiswaan
 * @see DetailKelompokKegiatanKesiswaan
 * @see KegiatanKesiswaan
 * @see NilaiKegiatanKesiswaan
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kelompok_kegiatan_kesiswaan")



public class KelompokKegiatanKesiswaan extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya kebetulan sama persis dengan milik
	 * {@link JenisKelompokKegiatanKesiswaan}, {@link SkalaKegiatanKesiswaan},
	 * {@link JabatanKegiatanKesiswaan}, dan padanan PT-nya
	 * {@link ais.database.model.KelompokKegiatanKemahasiswaan} &mdash; hasil salin-tempel generator;
	 * tidak ada makna khusus di balik angka ini.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Primary key baris. Lihat {@link #getId()}. */
	private Long id;

	/** Nama pengguna pengubah terakhir. Lihat {@link #getOleh()}. */
	private String oleh;

	/** Identitas (NIS/NIP/username) pengubah terakhir. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna yang terakhir mengubah baris ini (NIS/NIP/username,
	 * tergantung jenis akun), sebagaimana diisi
	 * {@code ais.database.hibernate.AuditTimestampInterceptor}.
	 *
	 * @return identitas pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel identitas pengguna pengubah terakhir, dengan <b>penjagaan anti-timpa</b>: nilai
	 * {@code null} atau string kosong/spasi diabaikan diam-diam sehingga jejak audit yang sudah ada
	 * tidak terhapus oleh proses batch atau salinan bean yang tidak membawa konteks pengguna.
	 *
	 * <p>Konsekuensinya, nilai kolom ini <b>tidak dapat dikosongkan kembali</b> lewat setter; sekali
	 * terisi, hanya bisa diganti dengan identitas lain yang tidak kosong.</p>
	 *
	 * @param olehId identitas pengubah; diabaikan bila {@code null} atau kosong/hanya spasi
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan penjagaan anti-timpa yang sama seperti
	 * {@link #setOlehId(String)}: nilai {@code null} atau kosong/hanya spasi diabaikan diam-diam.
	 *
	 * @param oleh nama pengubah; diabaikan bila {@code null} atau kosong/hanya spasi
	 * @see #setOlehId(String)
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil Hibernate <b>tepat sebelum</b> baris ini di-{@code
	 * UPDATE}, lalu meneruskan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} untuk memperbarui
	 * {@link #tanggal_dirubah} beserta {@link #oleh}/{@link #olehId} dari konteks pengguna yang
	 * sedang aktif.
	 *
	 * <p>Tidak pernah dipanggil manual dari kode aplikasi. Tidak ada pasangan {@code @PrePersist}:
	 * pada baris baru, stempel waktu berasal dari inisialisasi field {@link #tanggal_dirubah}
	 * ({@code WaktuUtil.getDate()}) yang dieksekusi saat konstruktor berjalan.</p>
	 *
	 * <p><b>Catatan format:</b> deklarasi method ini dan deklarasi field {@code tanggal_dirubah}
	 * sengaja berbagi satu baris fisik &mdash; pola salin-tempel yang sama ditemukan di ratusan
	 * entity paket ini. Jangan dipecah tanpa alasan; perubahan kosmetik pada baris ini memicu
	 * konflik di banyak sesi paralel.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi; nilai {@code null} diterima.
	 *
	 * <p>Umumnya tidak dipanggil manual &mdash; {@link #onUpdate()} yang mengisinya otomatis.</p>
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini. Dipetakan sebagai {@code TIMESTAMP},
	 * dan karena field-nya diinisialisasi {@code WaktuUtil.getDate()} saat konstruktor berjalan,
	 * nilainya tidak pernah {@code null} untuk objek yang dibuat lewat konstruktor.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini: <b>hanya nama</b>, tanpa awalan id.
	 *
	 * <p>Berbeda dari induknya {@link JenisKelompokKegiatanKesiswaan} yang memakai format
	 * {@code id + "-" + nama}, dan berbeda pula dari {@link GeneralValueObject#toString()}. Nilai
	 * inilah yang muncul sebagai label item combobox aspek kegiatan di layar
	 * {@link KegiatanKesiswaan}.</p>
	 *
	 * <p><b>Perhatian:</b> membaca field {@code nama} secara langsung (bukan lewat
	 * {@link #getNama()}), jadi hasilnya <b>tidak di-{@code trim}</b> dan bisa {@code null} untuk
	 * objek baru yang belum diisi &mdash; berbeda dengan nilai yang tampil lewat
	 * {@link #getNama()}.</p>
	 *
	 * @return nama aspek kegiatan apa adanya, bisa {@code null}
	 */
	public String toString() {
		return nama;
	}

	/** Nama aspek/kelompok kegiatan, wajib. Lihat {@link #getNama()}. */
	private String nama;

	/** Keterangan bebas, opsional. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Induk klasifikasi tingkat teratas, wajib. Lihat {@link #getJenisKelompokKegiatanKesiswaan()}. */
	private JenisKelompokKegiatanKesiswaan jenisKelompokKegiatanKesiswaan;

	/** Penanda baris masih dipakai. Lihat {@link #getAktif()}. */
	private Boolean aktif;

	/** Penanda boleh dipilih akun siswa. Lihat {@link #getBisaDipilihSiswa()}. */
	private Boolean bisaDipilihSiswa;

	/** Bobot penilaian (praktis tidak terpakai). Lihat {@link #getBobot()}. */
	private Double bobot;

	/** Nilai minimal (praktis tidak terpakai). Lihat {@link #getNilaiMinimal()}. */
	private Double nilaiMinimal;

	/** Nomor urut tampil. Lihat {@link #getNomorUrut()}. */
	private Integer nomorUrut;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Semua field dibiarkan {@code null}
	 * kecuali {@link #tanggal_dirubah} yang langsung terisi waktu saat ini lewat inisialisasi field.
	 *
	 * <p>Dipanggil langsung oleh {@code KelompokKegiatanKesiswaanAction.onAdd()} untuk menyiapkan
	 * form "Tambah Aspek Kegiatan Kesiswaan", dan oleh {@code InitDataHelper} saat menyemai data
	 * bawaan.</p>
	 */
	public KelompokKegiatanKesiswaan() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * <p>Dihasilkan database dengan strategi {@link javax.persistence.GenerationType#IDENTITY}
	 * (sequence PostgreSQL di balik kolom {@code serial}), karena itu kolomnya dipetakan
	 * {@code insertable = false}. Bernilai {@code null} selama objek belum pernah disimpan &mdash;
	 * kondisi inilah yang dipakai layar masternya untuk membedakan mode "Tambah" dari "Ubah"
	 * (judul jendela) dan untuk memutuskan apakah pemeriksaan duplikat nama perlu mengecualikan
	 * baris yang sedang disunting (lihat {@code checkNamaKelompokKegiatanKesiswaan()}).</p>
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Tanpa validasi.
	 *
	 * <p>Umumnya hanya dipanggil Hibernate saat memuat/menyimpan baris; kode aplikasi sebaiknya
	 * tidak menyetel id secara manual.</p>
	 *
	 * @param id primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama aspek/kelompok kegiatan kesiswaan (mis.
	 * {@code "Keagamaan dan moral pancasila"}), <b>sudah di-{@code trim}</b>.
	 *
	 * <p>Kolom {@code nama} bersifat {@code nullable = false} di database, dan layar masternya juga
	 * memvalidasi di sisi aplikasi: nama kosong ditolak, dan nama yang sudah ada ditolak sebagai
	 * duplikat (pengecekan {@code Restrictions.eq("nama", ...)} pada
	 * {@code checkNamaKelompokKegiatanKesiswaan()} yang <b>case-sensitive</b> dan membandingkan
	 * nilai ter-{@code trim}, sehingga {@code "Olahraga"} dan {@code "olahraga"} tetap lolos sebagai
	 * dua baris berbeda). Tidak ada {@code unique constraint} di tingkat database yang menegakkan
	 * hal ini &mdash; jalur impor Excel massal ({@code Common.uploadData}) karenanya bisa memasukkan
	 * duplikat.</p>
	 *
	 * <p><b>Berbeda dari beberapa entity lain di paket ini, getter ini tidak menulis balik hasil
	 * {@code trim} ke field</b>: pembersihan hanya berlaku pada nilai kembalian, tidak memicu
	 * {@code UPDATE} tersembunyi. Sebagai efek sampingnya, {@link #toString()} (yang membaca field
	 * langsung) bisa mengembalikan teks yang masih berspasi tepi.</p>
	 *
	 * @return nama aspek kegiatan tanpa spasi tepi, atau {@code null} bila field belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama aspek/kelompok kegiatan. Tanpa validasi dan tanpa {@code trim} &mdash;
	 * pemeriksaan wajib-isi serta anti-duplikat dilakukan di layar masternya sebelum setter ini
	 * dipanggil.
	 *
	 * @param nama nama aspek kegiatan yang baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas baris ini (isian textarea 3 baris di layar masternya, kolom
	 * "Keterangan" di grid, dan teks deskripsi pada item combobox aspek kegiatan).
	 *
	 * <p><b>Override ini membalik kontrak base class.</b>
	 * {@link GeneralValueObject#getKeterangan()} menjanjikan hasil <i>tidak pernah</i> {@code null}
	 * ({@code null} dinormalkan menjadi {@code ""}); versi di sini mengembalikan nilai field mentah,
	 * sehingga {@code null} bisa lolos ke pemanggil. Pola yang sama muncul di sejumlah entity
	 * turunan {@code hbm2java} lain di paket ini, jadi ini variasi arsitektural yang dikenal, bukan
	 * anomali terisolasi &mdash; tetapi kode pemanggil tetap tidak boleh mengandalkan jaminan
	 * non-null milik base class saat bekerja dengan tipe ini.</p>
	 *
	 * <p>Dampak praktisnya kecil namun nyata: cabang pembanding {@code keterangan} pada
	 * {@link GeneralValueObject#compareTo(GeneralValueObject)} tidak lagi selalu memenuhi syarat
	 * non-null, sehingga dua baris yang seharusnya diurutkan lewat keterangan bisa jatuh ke hasil
	 * {@code 0} (dianggap setara).</p>
	 *
	 * @return keterangan baris ini, bisa {@code null}
	 * @see GeneralValueObject#getKeterangan()
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas. Tanpa validasi; nilai {@code null} diterima dan &mdash; berbeda
	 * dari base class &mdash; akan terbaca kembali sebagai {@code null} lewat
	 * {@link #getKeterangan()}.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan penanda apakah aspek kegiatan ini masih dipakai, dengan <b>default aman
	 * {@code true}</b>: baris lama yang kolomnya masih {@code null} (termasuk seluruh baris hasil
	 * auto-seed {@code InitDataHelper}, yang tidak pernah menyetel field ini) tetap dianggap aktif.
	 *
	 * <p>Berbeda dari {@link #getBobot()}/{@link #getNilaiMinimal()}, bendera ini <b>benar-benar
	 * ditegakkan</b>: {@code KegiatanKesiswaanAction} menyaring isi combobox aspek kegiatan dengan
	 * {@code Restrictions.or(isNull("aktif"), eq("aktif", true))}, sehingga menonaktifkan satu baris
	 * langsung menyembunyikannya dari form pengajuan kegiatan baru. Filter serupa juga dipasang saat
	 * mengisi combobox rincian ({@link DetailKelompokKegiatanKesiswaan}). Penyaringan hanya berlaku
	 * untuk <i>pilihan baru</i> &mdash; kegiatan lama yang sudah terlanjur menunjuk baris nonaktif
	 * tetap tampil apa adanya.</p>
	 *
	 * <p>Di grid layar masternya, bendera ini berupa checkbox "Aktif" yang menyimpan perubahannya
	 * <b>seketika</b> ({@code Common.refreshSaveOrUpdate}) tanpa dialog konfirmasi.</p>
	 *
	 * @return {@code true} bila aktif atau kolomnya masih {@code null}; {@code false} bila
	 *         dinonaktifkan eksplisit
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel penanda aktif. Tanpa validasi; {@code null} diterima dan akan terbaca sebagai
	 * {@code true} lewat {@link #getAktif()}.
	 *
	 * @param aktif {@code true} bila baris masih dipakai
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan induk klasifikasi baris ini &mdash; tingkat teratas hierarki, di layar berlabel
	 * {@code "Kelompok Kegiatan Kesiswaan"} pada form tambah/ubah dan
	 * {@code "Kelompok Aspek Kegiatan"} pada kolom grid.
	 *
	 * <p>Relasinya {@code @ManyToOne} wajib ({@code nullable = false}) dengan
	 * {@code CascadeType.PERSIST} + {@code MERGE} &mdash; menyimpan baris ini ikut menyimpan induk
	 * yang belum tersimpan &mdash; dan {@code FetchMode.SELECT} sehingga induk dimuat lewat query
	 * terpisah, bukan {@code JOIN}. Getter ini <b>murni baca</b>: tidak ada normalisasi maupun tulis
	 * balik (bandingkan dengan {@code DetailKelompokKegiatanKesiswaan.getKelompokKegiatanKesiswaan()}
	 * yang memanggil {@code check(...)} dan menulis balik hasilnya ke field).</p>
	 *
	 * <p><b>KUIRK NAMA KOLOM (penting, terkonfirmasi identik di tiga modul):</b> foreign key-nya
	 * dipetakan ke kolom bernama {@code skala_kegiatan_kesiswaan}, bukan sesuatu seperti
	 * {@code jenis_kelompok_kegiatan_kesiswaan}. Nama itu <b>menyesatkan</b>: ada entity terpisah
	 * {@link SkalaKegiatanKesiswaan} (skala lokal/nasional/internasional dan sejenisnya) yang tidak
	 * punya relasi apa pun dengan class ini &mdash; skala digantungkan ke
	 * {@link DetailKelompokKegiatanKesiswaan} sebagai koleksi many-to-many. Bug salin-tempel yang
	 * sama persis juga ada di padanan PT
	 * ({@code KelompokKegiatanKemahasiswaan}, kolom {@code skala_kegiatan_kemahasiswaan}) dan di
	 * modul dosen, jadi ini cacat generator yang menyebar ke tiga salinan, bukan kekhasan berkas
	 * ini. Membaca skema database tanpa membaca anotasi ini akan menghasilkan kesimpulan yang
	 * salah.</p>
	 *
	 * <p><b>BUG SEED khas versi sekolah:</b> di {@code InitDataHelper}, baris jenis
	 * {@code "Kelompok Utama"} dicari lebih dulu dan dibuat bila belum ada, lalu variabel
	 * {@code penunjang} dicari dengan literal nama yang <b>salah salin-tempel</b> &mdash;
	 * {@code Restrictions.eq("nama", "Kelompok Utama")} sekali lagi, bukan
	 * {@code "Kelompok Penunjang"}. Karena baris "Kelompok Utama" baru saja disimpan dan
	 * di-{@code flush}, query kedua selalu menemukannya, blok pembuatan tidak pernah dijalankan, dan
	 * akibatnya <b>baris {@code "Kelompok Penunjang"} tidak pernah tersemai</b> untuk modul
	 * kesiswaan (variabel {@code penunjang} malah memegang objek "Kelompok Utama" yang sama, dan
	 * hanya dipakai pada satu {@code System.out.println}). Seluruh aspek bawaan karenanya
	 * digantungkan ke "Kelompok Utama"; admin harus mengetik sendiri jenis "Kelompok Penunjang" bila
	 * memerlukannya. Versi PT tidak punya bug ini (kedua baris dibuat sekaligus di dalam satu blok
	 * {@code if (count == 0)}).</p>
	 *
	 * <p>Di layar masternya, combobox pemilih induk disetel {@code readonly} (hanya boleh dipilih
	 * dari daftar, tidak boleh diketik bebas), diisi tanpa filter apa pun (seluruh baris
	 * {@link JenisKelompokKegiatanKesiswaan} di instalasi), dan wajib terisi sebelum tombol Simpan
	 * mau melanjutkan.</p>
	 *
	 * @return induk klasifikasi, seharusnya tidak {@code null} untuk baris yang tersimpan lewat
	 *         layar masternya
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "skala_kegiatan_kesiswaan", nullable = false)
	public JenisKelompokKegiatanKesiswaan getJenisKelompokKegiatanKesiswaan() {
		return jenisKelompokKegiatanKesiswaan;
	}

	/**
	 * Menyetel induk klasifikasi baris ini. Tanpa validasi &mdash; kewajiban isi ditegakkan di layar
	 * masternya (tombol Simpan menolak dengan pesan
	 * {@code "Kelompok Aspek Kesiswaan harus diisi"} bila combobox belum dipilih) dan oleh constraint
	 * {@code NOT NULL} di database.
	 *
	 * <p>Menyetel {@code null} di sini tidak langsung gagal; kegagalan baru muncul saat
	 * {@code flush}/{@code INSERT} berupa pelanggaran constraint. Pemanggil: {@code onSave()} pada
	 * {@code KelompokKegiatanKesiswaanAction} dan jalur auto-seed {@code InitDataHelper} (yang di
	 * versi sekolah selalu meneruskan baris "Kelompok Utama" hasil query, sehingga tidak pernah
	 * meneruskan {@code null} &mdash; berbeda dari jebakan yang ada di versi PT).</p>
	 *
	 * @param jenisKelompokKegiatanKesiswaan induk klasifikasi tingkat teratas
	 */
	public void setJenisKelompokKegiatanKesiswaan(
			JenisKelompokKegiatanKesiswaan jenisKelompokKegiatanKesiswaan) {
		this.jenisKelompokKegiatanKesiswaan = jenisKelompokKegiatanKesiswaan;
	}

	/**
	 * Mengembalikan nomor urut tampil baris ini, dengan <b>default {@code 1}</b> bila kolomnya masih
	 * {@code null}.
	 *
	 * <p>Dipakai sebagai kunci pengurutan utama di layar masternya
	 * ({@code addOrder(asc("nomorUrut")).addOrder(asc("nama"))}), sehingga baris tanpa nomor urut
	 * eksplisit ikut terurut pada posisi {@code 1}. Perhatikan bahwa pengurutan itu dilakukan di
	 * <b>database</b> memakai nilai kolom apa adanya &mdash; default {@code 1} milik getter ini
	 * tidak berlaku di sana, dan baris ber-{@code NULL} akan diurutkan mengikuti aturan
	 * {@code NULLS LAST} bawaan PostgreSQL, yaitu di <i>akhir</i> daftar, bukan di posisi 1.</p>
	 *
	 * <p>Nilainya bisa disunting langsung di kolom "No Urut" pada grid; setiap perubahan langsung
	 * disimpan ({@code Common.refreshUpdate}) tanpa konfirmasi.</p>
	 *
	 * @return nomor urut tampil, atau {@code 1} bila belum diisi
	 */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampil. Tanpa validasi; nilai {@code null}, nol, maupun negatif diterima,
	 * dan nomor yang bertabrakan antar-baris juga tidak dicegah.
	 *
	 * @param nomorUrut nomor urut baru
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Mengembalikan penanda apakah aspek kegiatan ini boleh dipilih oleh <b>akun siswa</b> saat
	 * mengajukan kegiatan sendiri, dengan default {@code true} bila kolomnya masih {@code null}.
	 *
	 * <p>Ditegakkan di {@code KegiatanKesiswaanAction}: filter
	 * {@code or(isNull("bisaDipilihSiswa"), eq("bisaDipilihSiswa", true))} <b>hanya dipasang bila
	 * pengguna yang login adalah siswa</b> ({@code tbmuser.getSiswa() != null}); untuk
	 * operator/admin/guru filternya diganti {@code sqlRestriction("true")} sehingga seluruh aspek
	 * tetap terlihat. Jadi bendera ini membatasi pengajuan mandiri siswa, bukan visibilitas data
	 * secara umum.</p>
	 *
	 * <p><b>Efek merambat ke bawah &mdash; dan bersifat DESTRUKTIF/permanen:</b>
	 * {@link DetailKelompokKegiatanKesiswaan} punya bendera senama, dan getter di entity anak itu
	 * bukan sekadar membaca: bila induknya (baris ini) mengembalikan {@code false}, getter anak
	 * <b>menulis {@code false} ke field-nya sendiri</b> lalu mengembalikannya. Karena entity anak
	 * biasanya berstatus <i>managed</i> saat dibaca, penulisan itu ikut ter-{@code flush} ke
	 * database sebagai {@code UPDATE} senyap. Konsekuensi yang perlu disadari: menonaktifkan bendera
	 * di baris ini <b>mematikan seluruh rinciannya secara permanen di tingkat data</b> &mdash;
	 * mengaktifkan kembali baris ini <i>tidak</i> memulihkan rincian-rinciannya, karena kolom anak
	 * sudah terlanjur tertulis {@code false} dan harus dicentang ulang satu per satu. Dicatat apa
	 * adanya sebagai temuan, bukan anjuran perubahan di berkas ini.</p>
	 *
	 * @return {@code true} bila siswa boleh memilih aspek ini, atau kolomnya masih {@code null}
	 * @see DetailKelompokKegiatanKesiswaan#getBisaDipilihSiswa()
	 */
	public Boolean getBisaDipilihSiswa() {
		return bisaDipilihSiswa == null ? true : bisaDipilihSiswa;
	}

	/**
	 * Menyetel penanda boleh-dipilih-siswa. Tanpa validasi; {@code null} diterima dan akan terbaca
	 * sebagai {@code true}.
	 *
	 * <p>Di grid layar masternya berupa checkbox berlabel {@code "Bisa Dipilih Siswa"} (judul
	 * kolomnya disingkat {@code "Bisa dplh siswa"}) yang menyimpan perubahan seketika
	 * ({@code Common.refreshSaveOrUpdate}) tanpa konfirmasi. Perhatikan efek merambat permanen ke
	 * entity anak yang dijelaskan di {@link #getBisaDipilihSiswa()} sebelum mematikan bendera
	 * ini.</p>
	 *
	 * @param bisaDipilihSiswa {@code true} bila siswa boleh memilih aspek ini
	 */
	public void setBisaDipilihSiswa(Boolean bisaDipilihSiswa) {
		this.bisaDipilihSiswa = bisaDipilihSiswa;
	}

	/**
	 * Mengembalikan bobot penilaian aspek kegiatan ini, dengan default {@code 0.0} bila kolomnya
	 * masih {@code null}.
	 *
	 * <p><b>Praktis write-only.</b> Nilainya bisa diisi lewat kolom "Bobot" di grid layar masternya
	 * (dan lewat impor Excel {@code Common.uploadData}), tetapi <b>tidak ada satu pun</b>
	 * perhitungan angka kredit, laporan, dasbor, atau validasi di codebase yang membacanya kembali
	 * &mdash; satu-satunya pembaca adalah grid yang menampilkannya untuk disunting lagi.
	 * Perhitungan angka kredit kegiatan kesiswaan yang sesungguhnya memakai
	 * {@link NilaiKegiatanKesiswaan}, yang digantungkan ke {@link DetailKelompokKegiatanKesiswaan}
	 * (tingkat 3) bersama {@link JabatanKegiatanKesiswaan} dan {@link SkalaKegiatanKesiswaan}, bukan
	 * ke baris ini.</p>
	 *
	 * <p>Dicatat apa adanya sebagai temuan, bukan sebagai anjuran perubahan.</p>
	 *
	 * @return bobot penilaian, atau {@code 0.0} bila belum diisi
	 */
	public Double getBobot() {
		return bobot == null ? 0.0 : bobot;
	}

	/**
	 * Menyetel bobot penilaian. Tanpa validasi (nilai negatif maupun sangat besar diterima).
	 * Perubahan lewat grid langsung disimpan ({@code Common.refreshUpdate}) tanpa konfirmasi. Lihat
	 * {@link #getBobot()} untuk catatan bahwa nilai ini tidak pernah dibaca ulang oleh logika bisnis
	 * mana pun.
	 *
	 * @param bobot bobot penilaian baru
	 */
	public void setBobot(Double bobot) {
		this.bobot = bobot;
	}

	/**
	 * Mengembalikan nilai minimal aspek kegiatan ini, dengan default {@code 0.0} bila kolomnya masih
	 * {@code null}.
	 *
	 * <p><b>Praktis write-only</b>, persis seperti {@link #getBobot()}: bisa diisi lewat kolom
	 * "Nilai Min." di grid layar masternya, tetapi tidak ada logika validasi maupun perhitungan di
	 * codebase yang membandingkan capaian siswa terhadap ambang ini. Tidak ada peringatan apa pun
	 * bila seorang siswa berada di bawah nilai minimal yang tercatat di sini.</p>
	 *
	 * @return nilai minimal, atau {@code 0.0} bila belum diisi
	 */
	public Double getNilaiMinimal() {
		return nilaiMinimal == null ? 0.0 : nilaiMinimal;
	}

	/**
	 * Menyetel nilai minimal. Tanpa validasi, dan tanpa pemeriksaan konsistensi terhadap
	 * {@link #getBobot()}. Lihat {@link #getNilaiMinimal()} untuk catatan bahwa nilai ini tidak
	 * pernah ditegakkan.
	 *
	 * @param nilaiMinimal nilai minimal baru
	 */
	public void setNilaiMinimal(Double nilaiMinimal) {
		this.nilaiMinimal = nilaiMinimal;
	}

}
