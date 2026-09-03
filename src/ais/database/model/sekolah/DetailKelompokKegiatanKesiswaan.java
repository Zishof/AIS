package ais.database.model.sekolah;

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



import ais.database.model.GeneralValueObject;

/**
 * Entity master <b>rincian aspek kegiatan kesiswaan</b> &mdash; tingkat <b>ketiga (terbawah)</b>
 * hierarki master kegiatan kesiswaan, dipetakan ke tabel
 * {@code public.detail_kelompok_kegiatan_kesiswaan}. Satu baris mewakili satu rincian konkret di
 * bawah sebuah aspek, misalnya data bawaan {@code "PHBI"} / {@code "PHBN"} di bawah aspek
 * {@code "Keagamaan dan moral pancasila"}, atau
 * {@code "Diskusi,seminar,workshop,lokakarya,symposium dan ceramah ilmiah"} di bawah aspek
 * {@code "Penalaran dan Ilmiah"} (semuanya disemai {@code ais.common.InitDataHelper}).
 *
 * <p>Selain menjadi label rincian, baris ini juga berfungsi sebagai <b>katalog pilihan</b>: dua
 * koleksi many-to-many miliknya &mdash; {@link #getJabatanKegiatanKesiswaans()} dan
 * {@link #getSkalaKegiatanKesiswaans()} &mdash; menentukan jabatan/status dan skala apa saja yang
 * boleh dipilih ketika sebuah {@link KegiatanKesiswaan} digantungkan ke rincian ini, dan sekaligus
 * menjadi sumbu tabel angka kredit {@link NilaiKegiatanKesiswaan}.</p>
 *
 * <h2>Posisi dalam hierarki (TERVERIFIKASI dari kode, bukan dari nama berkas)</h2>
 *
 * <p>Nama class ini mudah disalahpahami. Induknya <b>bukan</b>
 * {@link JenisKelompokKegiatanKesiswaan} melainkan {@link KelompokKegiatanKesiswaan} &mdash; lihat
 * tipe field {@link #kelompokKegiatanKesiswaan} dan kolom FK {@code kelompok_kegiatan_kesiswaan}
 * pada {@link #getKelompokKegiatanKesiswaan()}. Susunan lengkapnya:</p>
 *
 * <ol>
 *   <li>{@link JenisKelompokKegiatanKesiswaan} &mdash; tingkat 1, tabel
 *       {@code sekolah.jenis_kelompok_kegiatan_kesiswaan};</li>
 *   <li>{@link KelompokKegiatanKesiswaan} &mdash; tingkat 2, tabel
 *       {@code public.kelompok_kegiatan_kesiswaan}; menunjuk tingkat 1 lewat kolom yang
 *       (menyesatkan) bernama {@code skala_kegiatan_kesiswaan};</li>
 *   <li><b>class ini</b> &mdash; tingkat 3, tabel {@code public.detail_kelompok_kegiatan_kesiswaan};
 *       menunjuk tingkat 2 lewat kolom {@code kelompok_kegiatan_kesiswaan}.</li>
 * </ol>
 *
 * <p>Relasi ke induk bersifat <b>satu arah dari anak</b>: {@link KelompokKegiatanKesiswaan}
 * <i>tidak</i> punya koleksi balik, sehingga daftar rincian selalu diambil lewat query
 * {@code Restrictions.eq("kelompokKegiatanKesiswaan", ...)} di
 * {@code ais.action.master.sekolah.helper.DetailKelompokKegiatanKesiswaanHelper}.</p>
 *
 * <h2>PERINGATAN KOSAKATA: label layar tidak sejajar dengan nama class</h2>
 *
 * <p>Jangan menyimpulkan tingkat hierarki dari kata "Kelompok"/"Detail" pada nama class. Yang dipakai
 * di layar:</p>
 *
 * <ul>
 *   <li>entity ini disebut <b>"Rincian Aspek"</b> &mdash; judul kolom grid {@code "Rincian Aspek"},
 *       tombol {@code "Tambah Rincian Aspek"}, judul dialog
 *       {@code "Pendataan Rincian Aspek"}, pesan validasi {@code "Rincian aspek harus diisi"},
 *       label combobox {@code "Rincian Aspek Kegiatan *"} di layar {@link KegiatanKesiswaan}, kolom
 *       filter {@code "Nama Rincian Aspek"} di layar Angka Kredit, dan judul kolom
 *       {@code "Aspek Rinci"} di grid kegiatan kesiswaan;</li>
 *   <li>induknya {@link KelompokKegiatanKesiswaan} disebut <b>"Aspek Kegiatan Kesiswaan"</b>;</li>
 *   <li>kakeknya {@link JenisKelompokKegiatanKesiswaan} justru yang disebut
 *       <b>"Kelompok Kegiatan Kesiswaan"</b> / <b>"Kelompok Aspek"</b> di layar.</li>
 * </ul>
 *
 * <p>Jadi urutan istilah layar ("Kelompok" &rarr; "Aspek" &rarr; "Rincian Aspek") <b>berlawanan
 * arah</b> dengan yang disugestikan nama class ("Jenis&hellip;Kelompok" &rarr; "Kelompok" &rarr;
 * "DetailKelompok").</p>
 *
 * <h2>Entity bernama mirip yang BUKAN entity ini (verifikasi negatif)</h2>
 *
 * <ul>
 *   <li>{@link KelompokKegiatanSiswa} &mdash; <b>domain berbeda</b>, katalog kelompok untuk
 *       {@link KegiatanSiswa} (poin kegiatan yang dicetak di rapor). Nol relasi, nol tabel bersama,
 *       nol pemakai bersama dengan berkas ini;</li>
 *   <li>{@link KegiatanSiswa} &mdash; entity kegiatan versi biodata siswa; terpisah total dari
 *       {@link KegiatanKesiswaan};</li>
 *   <li>{@link KegiatanKesiswaanPunyaSiswa} &mdash; tabel peserta (siswa &harr; kegiatan). Tidak
 *       menunjuk baris ini secara langsung; ia menunjuk {@link KegiatanKesiswaan}, dan pilihan
 *       jabatan/skala pesertanya baru diturunkan dari baris ini lewat query ulang (lihat
 *       {@link #getJabatanKegiatanKesiswaans()});</li>
 *   <li>{@link SkalaKegiatanKesiswaan} dan {@link JabatanKegiatanKesiswaan} &mdash; master datar
 *       yang <b>berelasi</b> dengan baris ini, tetapi sebagai koleksi many-to-many, bukan sebagai
 *       induk/anak.</li>
 * </ul>
 *
 * <h2>Konsumen hilir</h2>
 *
 * <ol>
 *   <li>{@link KegiatanKesiswaan} menyimpan FK <b>wajib</b> ke baris ini (kolom
 *       {@code detail_kelompok_kegiatan_kesiswaan}, {@code nullable = false}) <i>dan</i> FK terpisah
 *       ke induknya {@link KelompokKegiatanKesiswaan}. Tingkat 2 karenanya
 *       <b>di-denormalisasi</b> di baris kegiatan alih-alih ditelusuri lewat baris ini; tidak ada
 *       constraint database yang memaksa kedua FK itu konsisten (lihat catatan
 *       "Hal non-obvious" nomor 6);</li>
 *   <li>{@link NilaiKegiatanKesiswaan} (angka kredit) memakai baris ini sebagai salah satu dari tiga
 *       sumbu kunci gabungan {@code kodeUnik = detailId + "-" + skalaId + "-" + jabatanId};</li>
 *   <li>{@code ais.action.master.SertifikatAction} mencetak {@link #getNama()} sebagai parameter
 *       {@code "detail"} pada sertifikat kegiatan.</li>
 * </ol>
 *
 * <h2>Pengelompokan anggota</h2>
 *
 * <ul>
 *   <li><b>Identitas</b> &mdash; {@link #getId()}, {@link #getNama()}, {@link #toString()};</li>
 *   <li><b>Hierarki</b> &mdash; {@link #getKelompokKegiatanKesiswaan()};</li>
 *   <li><b>Katalog pilihan (many-to-many)</b> &mdash; {@link #getJabatanKegiatanKesiswaans()},
 *       {@link #getSkalaKegiatanKesiswaans()};</li>
 *   <li><b>Kendali tampil/pilih</b> &mdash; {@link #getAktif()}, {@link #getBisaDipilihSiswa()},
 *       {@link #getNomorUrut()};</li>
 *   <li><b>Jejak audit</b> &mdash; {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 * </ul>
 *
 * <h2>Hal non-obvious</h2>
 *
 * <ol>
 *   <li><b>{@link #getBisaDipilihSiswa()} adalah GETTER DESTRUKTIF.</b> Bila induknya menjawab
 *       {@code false}, getter ini <b>menulis {@code false} ke field-nya sendiri</b> lalu
 *       mengembalikannya &mdash; perubahan permanen di tingkat data, bukan sekadar nilai kembalian.
 *       Rinciannya (termasuk mengapa Hibernate sendiri bisa memicunya tanpa kode aplikasi
 *       menyentuhnya) dijelaskan di {@link #getBisaDipilihSiswa()}. Ini instance pola "getter
 *       write-back/destruktif" yang sudah dikenal luas di codebase ini.</li>
 *   <li><b>{@link #getKelompokKegiatanKesiswaan()} juga menulis balik</b>, tetapi varian jinak:
 *       hasil {@link GeneralValueObject#check(Object)} (resolusi proxy lazy) disimpan ke field.
 *       Tidak mengubah nilai kolom, hanya mengganti rujukan objek di memori.</li>
 *   <li><b>Field koleksi diinisialisasi {@code TreeSet} &mdash; hazard kehilangan data diam-diam.</b>
 *       {@link GeneralValueObject#compareTo(GeneralValueObject)} mengurutkan berdasarkan
 *       {@code nomorUrut} dan mengembalikan {@code 0} untuk dua baris ber-{@code nomorUrut} sama,
 *       sehingga {@code TreeSet.add()} <b>menolak</b> anggota kedua. Layar rincian sengaja
 *       menghindarinya (lihat {@link #setJabatanKegiatanKesiswaans(Set)}), tetapi jalur auto-seed
 *       {@code InitDataHelper} justru memakai {@code TreeSet} bawaan ini. <b>Verifikasi negatif:</b>
 *       data bawaan aman karena seluruh {@link SkalaKegiatanKesiswaan} dan
 *       {@link JabatanKegiatanKesiswaan} bawaan disemai dengan {@code nomorUrut} yang berbeda-beda
 *       (skala 1&ndash;5 dan seterusnya, jabatan 1&ndash;7). Hazard baru menyala bila operator
 *       menambah master jabatan/skala dengan {@code nomorUrut} kembar atau {@code null}.</li>
 *   <li><b>Tabel ini tinggal di schema {@code public}, bukan {@code sekolah}</b> &mdash; sama seperti
 *       induknya {@link KelompokKegiatanKesiswaan}, sementara kakeknya
 *       {@link JenisKelompokKegiatanKesiswaan} beserta {@link SkalaKegiatanKesiswaan},
 *       {@link JabatanKegiatanKesiswaan}, {@link KegiatanKesiswaan}, dan
 *       {@link NilaiKegiatanKesiswaan} berada di schema {@code sekolah}. Satu hierarki tiga tingkat
 *       terbelah di dua schema dan foreign key-nya menyeberang schema. Dicatat apa adanya, bukan
 *       anjuran perubahan.</li>
 *   <li><b>FK ke induk {@code nullable = true}</b>, berbeda dari FK induk-ke-kakek yang
 *       {@code nullable = false}. Baris rincian yatim karenanya sah di tingkat database. Layar
 *       rincian selalu mengisinya, tetapi baris yatim (mis. dari impor/CRUD generik) akan
 *       <b>menghilang diam-diam</b> dari layar Angka Kredit &mdash; {@code initCriteria()} di
 *       {@code NilaiKegiatanKesiswaanAction} memakai {@code createAlias("kelompokKegiatanKesiswaan",
 *       ...)} yang berarti {@code INNER JOIN} &mdash; sekaligus meledakkan {@code NullPointerException}
 *       di renderer {@code NilaiKegiatanKesiswaanRenderer} bila sempat terbaca.</li>
 *   <li><b>Tidak ada validasi nama duplikat sama sekali.</b> Induknya punya
 *       {@code checkNamaKelompokKegiatanKesiswaan()}; layar rincian hanya menolak nama kosong.
 *       Tidak ada pula {@code unique constraint} di database, sehingga dua rincian bernama sama di
 *       bawah satu aspek diterima tanpa peringatan.</li>
 *   <li><b>Properti induk sengaja dideklarasikan ulang.</b> {@link GeneralValueObject} adalah POJO
 *       abstrak biasa &mdash; <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} &mdash;
 *       sehingga Hibernate tidak memetakan properti apa pun miliknya. Deklarasi ulang
 *       {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah}/{@code nama}/{@code nomorUrut}
 *       di sini adalah <b>keharusan teknis</b>, bukan duplikasi yang perlu "dibersihkan".</li>
 *   <li><b>Tidak punya kolom {@code keterangan}</b>, berbeda dari induk dan kakeknya. Field
 *       {@code keterangan} milik {@link GeneralValueObject} ada di memori tetapi tidak dipetakan ke
 *       kolom mana pun, sehingga isinya tidak pernah tersimpan.</li>
 * </ol>
 *
 * <h2>Layar, hak akses, dan efek samping yang perlu disadari</h2>
 *
 * <p>Entity ini <b>tidak punya layar sendiri</b>. Ia hanya muncul di tiga tempat:</p>
 *
 * <ol>
 *   <li><b>Panel rincian (CRUD utama)</b> &mdash; baris {@code MyDetail} yang bisa dibuka di dalam
 *       grid layar {@code /pages/master/sekolah/kelompok_kegiatan_kesiswaan.zul}
 *       ({@code KelompokKegiatanKesiswaanAction}), dirender
 *       {@code DetailKelompokKegiatanKesiswaanHelper};</li>
 *   <li><b>Layar Angka Kredit</b> ({@code nilai_kegiatan_kesiswaan.zul} /
 *       {@code NilaiKegiatanKesiswaanAction}, disisipkan sebagai tab
 *       {@code "Angka Kredit"} dari layar aspek) &mdash; setiap baris grid adalah satu baris entity
 *       ini, dengan matriks jabatan &times; skala miliknya sebagai sel nilai;</li>
 *   <li><b>Combobox {@code "Rincian Aspek Kegiatan *"}</b> di form tambah/ubah
 *       {@link KegiatanKesiswaan}, yang disaring ulang setiap kali combobox aspek berubah.</li>
 * </ol>
 *
 * <p><b>Catatan hak akses (dicatat apa adanya, bukan anjuran perubahan di berkas ini):</b></p>
 *
 * <ul>
 *   <li><b>Pewarisan hak lewat menu/layar induk, dan gerbang tulis yang dikomentari mati.</b>
 *       {@code KelompokKegiatanKesiswaanAction} memasang gerbang READ
 *       ({@code Common.doCheckSecurity()} di {@code doBeforeCompose}), tetapi ketiga gerbang
 *       tulisnya <b>dikomentari</b> ({@code CommonPrivilages.CREATE}, {@code UPDATE},
 *       {@code DELETE}). {@code DetailKelompokKegiatanKesiswaanHelper} sendiri (493 baris) <b>tidak
 *       memuat satu pun</b> pemanggilan {@code checkPrevilages}/{@code doCheckSecurity}. Akibatnya
 *       seluruh CRUD atas entity ini &mdash; tombol "Tambah Rincian Aspek", dialog ubah, penyuntingan
 *       langsung {@link #getNomorUrut()}/{@link #getAktif()}/{@link #getBisaDipilihSiswa()} di grid
 *       (tersimpan seketika, tanpa konfirmasi), dan tombol hapus &mdash; terbuka bagi siapa pun yang
 *       berhasil membuka layar aspek. Ini pola "zero-gate + pewarisan hak menu induk" yang sama
 *       dengan temuan pada modul kegiatan kesiswaan lain;</li>
 *   <li>{@code NilaiKegiatanKesiswaanAction} juga hanya memasang {@code Common.doCheckSecurity()}
 *       dan <b>nol</b> {@code checkPrevilages}, sehingga penyuntingan angka kredit serta unduh/unggah
 *       Excel massalnya sama-sama tanpa gerbang tulis;</li>
 *   <li><b>Verifikasi negatif SQL injection.</b> Berbeda dari {@code KegiatanKesiswaanAction} (yang
 *       merakit {@code Restrictions.sqlRestriction} dari nilai textbox NIS/nama siswa),
 *       {@code DetailKelompokKegiatanKesiswaanHelper} memakai Criteria API murni dan
 *       {@code NilaiKegiatanKesiswaanAction.initCriteria()} memakai {@code Restrictions.ilike}
 *       terparameter. Tidak ada perakitan SQL mentah dari input pengguna di jalur entity ini;</li>
 *   <li><b>Cakupan tenant: tidak ada, secara struktural.</b> Entity ini tidak punya kolom
 *       {@code sekolah} maupun {@code yayasan}, jadi seluruh instalasi multi-yayasan berbagi satu
 *       daftar rincian aspek yang sama dan saling menyuntingnya. Ini <b>bukan</b> kasus
 *       <i>fail-open</i> (tidak ada cakupan yang bisa gagal-terbuka) melainkan konsekuensi desain
 *       master bersama &mdash; verdict yang sama dengan induknya. Sebagai akibatnya, pola whitelist
 *       nama properti pada Generic CRUD v2 tidak relevan di sini: tidak ada properti tenant yang
 *       bisa lolos saring;</li>
 *   <li><b>Jalur UI baru.</b> {@code /WEB-INF/new/sekolah/services/helper/detail_kelompok_kegiatan_kesiswaan_helper_service.jsp}
 *       adalah scaffold generator yang mendeklarasikan {@code nuiServiceEntities} berisi
 *       {@code "DetailKelompokKegiatanKesiswaan"}, {@code "JabatanKegiatanKesiswaan"}, dan
 *       {@code "SkalaKegiatanKesiswaan"}, lalu mendelegasikan seluruh kerjanya ke
 *       {@code /WEB-INF/new/_shared/services/dispatcher.jsp}.</li>
 * </ul>
 *
 * <p><b>Efek samping berat pada layar Angka Kredit (bukan masalah keamanan, tetapi perlu
 * disadari):</b> {@code NilaiKegiatanKesiswaanRenderer.tampilRow()} berjalan di <b>jalur render</b>
 * namun <i>menulis ke database</i>: untuk setiap sel (jabatan &times; skala) milik baris entity ini
 * yang belum punya {@link NilaiKegiatanKesiswaan}, ia langsung membuat dan menyimpan barisnya.
 * Sekadar membuka atau mem-paging tab "Angka Kredit" karenanya menerbitkan {@code INSERT} sebanyak
 * hasil kali kartesian jabatan &times; skala untuk setiap rincian yang tampil &mdash; dan data
 * bawaan menggantungkan lima skala pada tiap rincian. Pola yang sama (menulis dari jalur baca)
 * sudah tercatat di beberapa berkas lain di codebase ini.</p>
 *
 * <p><b>Bug salin-tempel pada dasbor rekap (terverifikasi):</b> tab
 * {@code "Rekap Detail Kelompok"} di layar {@link KegiatanKesiswaan} memakai
 * {@code DashboardRekapKegiatanKesiswaanBerdasarDetailKelompok}, yang meneruskan ke konstruktor
 * induknya argumen milik <b>versi perguruan tinggi</b> apa adanya
 * ({@code "detail_kelompok_kegiatan_kemahasiswaan"},
 * {@code "kegiatanKemahasiswaan.detailKelompokKegiatanKemahasiswaan"}) &mdash; identik kata per kata
 * dengan {@code DashboardRekapKegiatanKemahasiswaanBerdasarDetailKelompok}. Kelas induk
 * {@code ais.action.master.dashboard.helper.DashboardRekapKegiatanMahasiswaan} sendiri memang
 * meng-hardcode {@code createCriteria(KegiatanKemahasiswaanPunyaMahasiswa.class)}, sehingga rekap
 * itu <b>tidak pernah melaporkan data entity ini</b> melainkan data kegiatan kemahasiswaan.
 * Dicatat apa adanya sebagai temuan, bukan anjuran perubahan di berkas ini.</p>
 *
 * @see KelompokKegiatanKesiswaan
 * @see JenisKelompokKegiatanKesiswaan
 * @see KegiatanKesiswaan
 * @see NilaiKegiatanKesiswaan
 * @see JabatanKegiatanKesiswaan
 * @see SkalaKegiatanKesiswaan
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "detail_kelompok_kegiatan_kesiswaan")
public class DetailKelompokKegiatanKesiswaan extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya <b>berbeda</b> dari nilai bersama yang dipakai
	 * {@link KelompokKegiatanKesiswaan}, {@link JenisKelompokKegiatanKesiswaan},
	 * {@link SkalaKegiatanKesiswaan}, dan {@link JabatanKegiatanKesiswaan}
	 * ({@code 2463821577548439808L}) &mdash; tanpa makna khusus, sekadar angka yang dihasilkan
	 * terpisah.
	 */
	private static final long serialVersionUID = -7050166125892447098L;

	/** Primary key baris. Lihat {@link #getId()}. */
	private Long id;

	/** Nama pengguna pengubah terakhir. Lihat {@link #getOleh()}. */
	private String oleh;

	/** Identitas (NIS/NIP/username) pengubah terakhir. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna yang terakhir mengubah baris ini (NIS/NIP/username, tergantung
	 * jenis akun), sebagaimana diisi {@code ais.database.hibernate.AuditTimestampInterceptor}.
	 *
	 * @return identitas pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel identitas pengguna pengubah terakhir, dengan <b>penjagaan anti-timpa</b>: nilai
	 * {@code null} atau string kosong/hanya spasi diabaikan diam-diam sehingga jejak audit yang sudah
	 * ada tidak terhapus oleh proses batch atau salinan bean yang tidak membawa konteks pengguna.
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
	 * Kait JPA {@code @PreUpdate}: dipanggil Hibernate <b>tepat sebelum</b> baris ini di-{@code UPDATE},
	 * lalu meneruskan ke {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} untuk
	 * memperbarui {@link #tanggal_dirubah} beserta {@link #oleh}/{@link #olehId} dari konteks pengguna
	 * yang sedang aktif.
	 *
	 * <p>Tidak pernah dipanggil manual dari kode aplikasi. Tidak ada pasangan {@code @PrePersist}: pada
	 * baris baru, stempel waktu berasal dari inisialisasi field {@link #tanggal_dirubah}
	 * ({@code WaktuUtil.getDate()}) yang dieksekusi saat konstruktor berjalan.</p>
	 *
	 * <p><b>Perhatian khusus untuk entity ini:</b> karena {@link #getBisaDipilihSiswa()} bisa
	 * mengubah state baris saat sekadar dibaca, kait ini juga bisa menyala pada alur yang secara kasat
	 * mata hanya menampilkan data &mdash; membuat {@link #getOleh()}/{@link #getTanggal_dirubah()}
	 * mencatat "perubahan" yang tidak pernah diminta pengguna.</p>
	 *
	 * <p><b>Catatan format:</b> deklarasi method ini dan deklarasi field {@code tanggal_dirubah}
	 * sengaja berbagi satu baris fisik &mdash; pola salin-tempel yang sama ditemukan di ratusan entity
	 * paket ini. Jangan dipecah tanpa alasan; perubahan kosmetik pada baris ini memicu konflik di
	 * banyak sesi paralel.</p>
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
	 * Representasi teks baris ini dengan format {@code id + "-" + nama} (mis. {@code "12-PHBI"}).
	 *
	 * <p>Formatnya <b>berbeda dari induknya</b> {@link KelompokKegiatanKesiswaan#toString()} yang
	 * mengembalikan nama saja, tetapi sama dengan {@link JenisKelompokKegiatanKesiswaan}. Untuk baris
	 * yang belum tersimpan hasilnya berawalan {@code "null-"}.</p>
	 *
	 * <p><b>Perhatian:</b> membaca field {@code nama} secara langsung, bukan lewat
	 * {@link #getNama()}. Karena {@link #getNama()} di berkas ini juga tidak melakukan normalisasi apa
	 * pun, keduanya menghasilkan teks yang sama &mdash; berbeda dari sejumlah entity lain di paket ini
	 * yang getter-nya melakukan {@code trim}.</p>
	 *
	 * <p>Nilai ini <b>bukan</b> yang tampil sebagai label combobox rincian aspek: baik
	 * {@code Common.insertCombo} maupun {@code Common.insertComboItems} dipanggil dengan properti
	 * {@code "nama"}, jadi layar menampilkan {@link #getNama()} saja. {@code toString()} baru terpakai
	 * pada log, pesan kesalahan, dan pembanding generik.</p>
	 *
	 * @return {@code id + "-" + nama}; bagian nama bisa {@code null} untuk baris yang belum diisi
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Aspek kegiatan kesiswaan induk dari rincian ini (tingkat 2 hierarki). Lihat
	 * {@link #getKelompokKegiatanKesiswaan()}.
	 */
	private KelompokKegiatanKesiswaan kelompokKegiatanKesiswaan;

	/** Nama rincian aspek. Lihat {@link #getNama()}. */
	private String nama;

	/** Nomor urut tampil di dalam satu aspek induk. Lihat {@link #getNomorUrut()}. */
	private Integer nomorUrut;

	/** Penanda baris masih dipakai. Lihat {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Cache penanda boleh-dipilih-siswa. <b>Bukan sumber kebenaran tunggal</b>: nilai efektif dihitung
	 * ulang di {@link #getBisaDipilihSiswa()}, yang ikut <b>menimpa field ini dengan {@code false}</b>
	 * bila induk {@link #kelompokKegiatanKesiswaan} tidak bisa dipilih siswa.
	 */
	private Boolean bisaDipilihSiswa;

	/**
	 * Jabatan/status/tugas (mis. Peserta, Panitia, Narasumber, Juara I) yang tersedia untuk dipilih
	 * pada rincian aspek ini. Lihat {@link #getJabatanKegiatanKesiswaans()}.
	 */
	private Set<JabatanKegiatanKesiswaan> jabatanKegiatanKesiswaans = new TreeSet<JabatanKegiatanKesiswaan>();

	/**
	 * Mengembalikan katalog jabatan/status/tugas yang boleh dipilih untuk kegiatan di bawah rincian
	 * aspek ini &mdash; di layar berupa kolom checkbox berjudul {@code "Jabatan/Status/Tugas"} pada
	 * dialog "Pendataan Rincian Aspek", dan kolom grid berjudul {@code "Jenis"} pada panel daftar.
	 *
	 * <p>Dipetakan {@code @ManyToMany} lewat tabel penghubung
	 * {@code detail_kelompok_has_jabatan_kegiatan_kesiswaan} ({@code detail_kelompok} &rarr;
	 * {@code jabatan_kegiatan_kesiswaan}). Sisi <b>pemilik</b> relasi ada di sini
	 * ({@link JabatanKegiatanKesiswaan} tidak punya koleksi balik), jadi hanya perubahan pada koleksi
	 * ini yang tersimpan. Cascade-nya {@code MERGE} + {@code PERSIST} saja: <b>tidak ada</b>
	 * {@code REMOVE} maupun {@code orphanRemoval}, sehingga menghapus rincian tidak pernah menghapus
	 * baris master jabatan &mdash; hanya baris tabel penghubungnya.</p>
	 *
	 * <p><b>Pemanggil dan efeknya:</b></p>
	 * <ul>
	 *   <li>{@code KegiatanKesiswaanAction} &mdash; setelah combobox rincian dipilih, koleksi ini
	 *       (didahului {@code session.refresh}) mengisi combobox {@code "Jabatan/Status *"} di form
	 *       kegiatan. Rincian tanpa jabatan berarti combobox kosong;</li>
	 *   <li>{@code KegiatanKesiswaanPunyaSiswaHelper} dan
	 *       {@code ais.action.master.helper.SiswaPunyaKegiatanKesiswaanHelper} &mdash; keduanya
	 *       <b>mengambil ulang</b> baris rincian lewat {@code Restrictions.idEq(...)} (walaupun
	 *       objeknya sudah tersedia dari {@code KegiatanKesiswaan}) semata untuk memaksa koleksi ini
	 *       termuat, lalu memakainya sebagai isi combobox jabatan peserta;</li>
	 *   <li>{@code NilaiKegiatanKesiswaanAction} &mdash; menjadi sumbu baris matriks angka kredit;
	 *       bila koleksi ini kosong, satu baris tanpa jabatan tetap dirender.</li>
	 * </ul>
	 *
	 * <p><b>Perhatian tipe koleksi:</b> defaultnya {@code TreeSet}, sehingga dua jabatan dengan
	 * {@code nomorUrut} sama saling meniadakan (lihat catatan "Hal non-obvious" nomor 3 pada
	 * dokumentasi class). Setiap pemanggil di atas menyalin isinya ke {@code ArrayList} lebih dulu lalu
	 * {@code Collections.sort} &mdash; menghindari {@code TreeSet} kedua pada sisi tampilan.</p>
	 *
	 * @return himpunan jabatan yang tertaut; tidak pernah {@code null}, bisa kosong
	 */
	@ManyToMany(targetEntity = JabatanKegiatanKesiswaan.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@JoinTable(name = "detail_kelompok_has_jabatan_kegiatan_kesiswaan", joinColumns = @JoinColumn(name = "detail_kelompok"), inverseJoinColumns = @JoinColumn(name = "jabatan_kegiatan_kesiswaan"))
	public Set<JabatanKegiatanKesiswaan> getJabatanKegiatanKesiswaans() {
		return jabatanKegiatanKesiswaans;
	}

	/**
	 * Mengganti seluruh katalog jabatan rincian ini. Tanpa validasi dan tanpa penyalinan defensif:
	 * himpunan yang diberikan dipasang apa adanya sebagai koleksi terpetakan.
	 *
	 * <p><b>Kontrak penting soal tipe koleksi.</b> {@code DetailKelompokKegiatanKesiswaanHelper}
	 * sengaja meneruskan {@code LinkedHashSet}, <b>bukan</b> {@code TreeSet} bawaan field, karena
	 * {@link GeneralValueObject#compareTo(GeneralValueObject)} mengembalikan {@code 0} untuk dua
	 * jabatan ber-{@code nomorUrut} sama &mdash; dengan {@code TreeSet}, mencentang dua jabatan
	 * bernomor urut kembar akan membuat centang kedua <b>ditolak diam-diam</b> dan hilang saat
	 * disimpan. Pemanggil baru wajib mengikuti kebiasaan yang sama.</p>
	 *
	 * <p>Pemanggil: tombol "Simpan" pada dialog "Pendataan Rincian Aspek", yang membangun himpunannya
	 * dari {@code LinkedHashMap} berkunci id agar pilihan pengguna tidak menciut.</p>
	 *
	 * @param jabatanKegiatanKesiswaans himpunan jabatan pengganti; sebaiknya {@code LinkedHashSet}
	 * @see #getJabatanKegiatanKesiswaans()
	 */
	public void setJabatanKegiatanKesiswaans(Set<JabatanKegiatanKesiswaan> jabatanKegiatanKesiswaans) {
		this.jabatanKegiatanKesiswaans = jabatanKegiatanKesiswaans;
	}

	/**
	 * Skala kegiatan (mis. Internasional, Nasional, Regional, Institut, Fak./Jur, serta puluhan skala
	 * durasi/peran bawaan lain) yang berlaku untuk rincian aspek ini. Lihat
	 * {@link #getSkalaKegiatanKesiswaans()}.
	 */
	private Set<SkalaKegiatanKesiswaan> skalaKegiatanKesiswaans = new TreeSet<SkalaKegiatanKesiswaan>();

	/**
	 * Mengembalikan katalog skala yang berlaku untuk kegiatan di bawah rincian aspek ini &mdash; di
	 * layar berupa kolom checkbox berjudul {@code "Skala"} pada dialog "Pendataan Rincian Aspek" dan
	 * kolom grid berjudul {@code "Skala"} pada panel daftar.
	 *
	 * <p>Dipetakan {@code @ManyToMany} lewat tabel penghubung
	 * {@code detail_kelompok_has_skala_kegiatan_kesiswaan} ({@code detail_kelompok} &rarr;
	 * {@code skala_kegiatan_kesiswaan}), dengan sifat kepemilikan dan cascade yang persis sama dengan
	 * {@link #getJabatanKegiatanKesiswaans()}.</p>
	 *
	 * <p><b>Catatan penting soal nama kolom:</b> jangan mengacaukan tabel penghubung ini dengan kolom
	 * {@code skala_kegiatan_kesiswaan} pada tabel {@code public.kelompok_kegiatan_kesiswaan}. Kolom
	 * bernama sama di tabel induk itu sebenarnya menyimpan FK ke
	 * {@link JenisKelompokKegiatanKesiswaan} &mdash; bug salin-tempel generator yang menyebar ke tiga
	 * modul (sekolah, perguruan tinggi, dosen). Relasi ke {@link SkalaKegiatanKesiswaan} yang
	 * <i>sungguhan</i> di seluruh hierarki master ini hanya ada di sini.</p>
	 *
	 * <p>Konsumen sama dengan katalog jabatan: combobox {@code "Skala *"} di form
	 * {@link KegiatanKesiswaan}, combobox skala peserta di kedua helper "punya siswa", dan sumbu kolom
	 * matriks angka kredit {@link NilaiKegiatanKesiswaan}. Data bawaan menggantungkan lima skala
	 * (Internasional, Nasional, Regional, Institut, Fak./Jur) pada tiap rincian yang disemai.</p>
	 *
	 * @return himpunan skala yang tertaut; tidak pernah {@code null}, bisa kosong
	 */
	@ManyToMany(targetEntity = SkalaKegiatanKesiswaan.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@JoinTable(name = "detail_kelompok_has_skala_kegiatan_kesiswaan", joinColumns = @JoinColumn(name = "detail_kelompok"), inverseJoinColumns = @JoinColumn(name = "skala_kegiatan_kesiswaan"))
	public Set<SkalaKegiatanKesiswaan> getSkalaKegiatanKesiswaans() {
		return skalaKegiatanKesiswaans;
	}

	/**
	 * Mengganti seluruh katalog skala rincian ini. Tanpa validasi dan tanpa penyalinan defensif,
	 * dengan kontrak tipe koleksi yang sama seperti {@link #setJabatanKegiatanKesiswaans(Set)}:
	 * pemanggil wajib meneruskan {@code LinkedHashSet} (bukan {@code TreeSet}) agar skala
	 * ber-{@code nomorUrut} kembar tidak hilang diam-diam.
	 *
	 * <p>Pemanggil: tombol "Simpan" pada dialog "Pendataan Rincian Aspek".</p>
	 *
	 * @param skalaKegiatanKesiswaans himpunan skala pengganti; sebaiknya {@code LinkedHashSet}
	 * @see #getSkalaKegiatanKesiswaans()
	 */
	public void setSkalaKegiatanKesiswaans(Set<SkalaKegiatanKesiswaan> skalaKegiatanKesiswaans) {
		this.skalaKegiatanKesiswaans = skalaKegiatanKesiswaans;
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * <p>Dihasilkan database dengan strategi {@link javax.persistence.GenerationType#IDENTITY}
	 * (sequence PostgreSQL di balik kolom {@code serial}), karena itu kolomnya dipetakan
	 * {@code insertable = false}. Bernilai {@code null} selama objek belum pernah disimpan &mdash;
	 * kondisi inilah yang dipakai {@code DetailKelompokKegiatanKesiswaanHelper.init()} untuk
	 * membedakan mode "tambah" dari "ubah": pada baris baru ia menghitung nomor urut default
	 * ({@code max(nomorUrut) + 1}) dan melewatkan {@code session.refresh}.</p>
	 *
	 * <p>Id ini juga menjadi bagian pertama kunci gabungan {@code kodeUnik} milik
	 * {@link NilaiKegiatanKesiswaan} ({@code detailId + "-" + skalaId + "-" + jabatanId}) dan kolom
	 * "ID" pada berkas Excel unduh/unggah angka kredit.</p>
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
	 * <p>Umumnya hanya dipanggil Hibernate saat memuat/menyimpan baris; kode aplikasi sebaiknya tidak
	 * menyetel id secara manual.</p>
	 *
	 * @param id primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama rincian aspek (mis. {@code "PHBI"},
	 * {@code "Tulisan ilmiah/karya tulis"}), <b>apa adanya</b> tanpa {@code trim} maupun normalisasi
	 * {@code null}.
	 *
	 * <p>Kolomnya dipetakan {@code columnDefinition = "text"} &mdash; tanpa batas panjang dan
	 * <b>tanpa {@code nullable = false}</b>, berbeda dari induknya {@link KelompokKegiatanKesiswaan}
	 * yang kolom namanya {@code varchar(255) NOT NULL}. Konsisten dengan itu, isian namanya di layar
	 * berupa {@code Textbox} tiga baris.</p>
	 *
	 * <p>Validasi satu-satunya ada di sisi layar: tombol "Simpan" menolak nama kosong dengan pesan
	 * {@code "Rincian aspek harus diisi"} dan menyimpan nilai yang sudah di-{@code trim}. <b>Tidak ada
	 * pemeriksaan duplikat sama sekali</b> (bandingkan induknya yang punya
	 * {@code checkNamaKelompokKegiatanKesiswaan()}), dan tidak ada {@code unique constraint} di
	 * database &mdash; dua rincian bernama sama di bawah satu aspek diterima tanpa peringatan.</p>
	 *
	 * <p>Pembaca: label kolom "Rincian Aspek" di panel daftar, label combobox rincian di form
	 * {@link KegiatanKesiswaan}, kolom "Aspek Rinci" di grid kegiatan, kolom "Rincian Aspek" pada
	 * unduhan Excel angka kredit, dan parameter {@code "detail"} pada cetak sertifikat kegiatan
	 * ({@code ais.action.master.SertifikatAction}).</p>
	 *
	 * @return nama rincian aspek, bisa {@code null} untuk baris yang belum diisi
	 */
	@Column(columnDefinition = "text")
	public String getNama() {
		return nama;
	}

	/**
	 * Menyetel nama rincian aspek. Tanpa validasi dan tanpa {@code trim} &mdash; pembersihan spasi
	 * serta penolakan nama kosong dilakukan di layar sebelum setter ini dipanggil.
	 *
	 * @param nama nama rincian aspek yang baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Menyetel aspek kegiatan kesiswaan induk (tingkat 2 hierarki). Tanpa validasi; {@code null}
	 * diterima baik di sini maupun di database ({@code nullable = true}).
	 *
	 * <p>Pemanggil: tombol "Simpan" pada dialog "Pendataan Rincian Aspek", yang selalu meneruskan
	 * aspek yang panel rinciannya sedang dibuka, dan jalur auto-seed {@code InitDataHelper}. Lihat
	 * {@link #getKelompokKegiatanKesiswaan()} untuk konsekuensi bila kolom ini dibiarkan
	 * {@code null}.</p>
	 *
	 * @param kelompokKegiatanKesiswaan aspek induk baru
	 */
	public void setKelompokKegiatanKesiswaan(KelompokKegiatanKesiswaan kelompokKegiatanKesiswaan) {
		this.kelompokKegiatanKesiswaan = kelompokKegiatanKesiswaan;
	}

	/**
	 * Mengembalikan aspek kegiatan kesiswaan induk dari rincian ini &mdash; tingkat 2 hierarki, di
	 * layar berlabel {@code "Aspek Kegiatan Kesiswaan"} (dan {@code "Nama Aspek"} pada unduhan Excel
	 * angka kredit).
	 *
	 * <p>Relasinya {@code @ManyToOne} dengan {@code CascadeType.PERSIST} + {@code MERGE} dan
	 * {@code FetchType.LAZY}, dipetakan ke kolom {@code kelompok_kegiatan_kesiswaan}. Relasi ini
	 * <b>satu arah</b>: {@link KelompokKegiatanKesiswaan} tidak punya koleksi balik ke rincian.</p>
	 *
	 * <p><b>Getter menulis balik ke field</b> (varian jinak dari pola getter write-back): hasil
	 * {@link GeneralValueObject#check(Object)} disimpan ke {@link #kelompokKegiatanKesiswaan} sebelum
	 * dikembalikan. {@code check()} berfungsi meresolusi proxy lazy &mdash; mencoba cache in-memory,
	 * inisialisasi proxy, lalu memuat ulang lewat session baru; bila keempat tahapnya gagal ia
	 * mengembalikan argumen apa adanya. Penulisan ini tidak mengubah nilai kolom, hanya mengganti
	 * rujukan objek di memori, jadi tidak menerbitkan {@code UPDATE}. Bandingkan dengan
	 * {@link KelompokKegiatanKesiswaan#getJenisKelompokKegiatanKesiswaan()} yang murni baca.</p>
	 *
	 * <p><b>Kolom ini {@code nullable = true}</b> &mdash; berbeda dari FK induk-ke-kakek yang
	 * {@code nullable = false}. Rincian yatim karenanya sah di tingkat database, dan konsekuensinya
	 * tidak ditangani di hilir:</p>
	 * <ul>
	 *   <li>{@code NilaiKegiatanKesiswaanAction.initCriteria()} memakai
	 *       {@code createAlias("kelompokKegiatanKesiswaan", ...)} &mdash; sebuah {@code INNER JOIN}
	 *       &mdash; sehingga baris yatim <b>hilang diam-diam</b> dari layar Angka Kredit tanpa pesan
	 *       apa pun;</li>
	 *   <li>{@code NilaiKegiatanKesiswaanRenderer.render()} memanggil
	 *       {@code getKelompokKegiatanKesiswaan().getNama()} tanpa penjaga {@code null}, begitu pula
	 *       unduhan Excel-nya &mdash; keduanya melempar {@code NullPointerException} bila baris yatim
	 *       sempat lolos ke tampilan.</li>
	 * </ul>
	 *
	 * <p>Perhatikan pula bahwa getter ini <b>tidak</b> dipakai oleh
	 * {@link #getBisaDipilihSiswa()}, yang membaca field mentah &mdash; lihat peringatan proxy lazy di
	 * sana.</p>
	 *
	 * @return aspek induk (proxy sudah diresolusi bila memungkinkan), atau {@code null} untuk baris
	 *         yatim
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_kegiatan_kesiswaan", nullable = true)
	public KelompokKegiatanKesiswaan getKelompokKegiatanKesiswaan() {
		kelompokKegiatanKesiswaan = check(kelompokKegiatanKesiswaan);
		return kelompokKegiatanKesiswaan;
	}

	/**
	 * Mengembalikan nomor urut tampil rincian ini di dalam aspek induknya, dengan <b>default
	 * {@code 1}</b> bila kolomnya masih {@code null}.
	 *
	 * <p>Dipakai sebagai kunci pengurutan di dua tempat: {@code addOrder(Order.asc("nomorUrut"))} pada
	 * panel daftar rincian, dan urutan ketiga
	 * ({@code kelompokKegiatanKesiswaan.nomorUrut} &rarr; {@code nomorUrut} &rarr; {@code nama}) pada
	 * layar Angka Kredit. Perhatikan bahwa pengurutan itu dilakukan di <b>database</b> memakai nilai
	 * kolom apa adanya &mdash; default {@code 1} milik getter ini tidak berlaku di sana, dan baris
	 * ber-{@code NULL} akan diurutkan mengikuti aturan {@code NULLS LAST} bawaan PostgreSQL, yaitu di
	 * <i>akhir</i> daftar, bukan di posisi 1.</p>
	 *
	 * <p><b>Peran kedua yang mudah terlewat:</b> nilai ini juga menjadi kunci pertama
	 * {@link GeneralValueObject#compareTo(GeneralValueObject)}. Karena kedua koleksi many-to-many
	 * entity ini secara default berupa {@code TreeSet}, {@code nomorUrut} kembar pada master
	 * jabatan/skala berakibat anggota kedua ditolak diam-diam &mdash; lihat
	 * {@link #setJabatanKegiatanKesiswaans(Set)}.</p>
	 *
	 * <p>Di panel daftar, nilainya bisa disunting langsung di kolom "Nomor Urut" dan setiap perubahan
	 * langsung disimpan ({@code Common.refreshUpdate}) tanpa konfirmasi. Untuk rincian baru, dialog
	 * tambah mengisinya otomatis dengan {@code max(nomorUrut) + 1} dalam lingkup aspek yang sama (atau
	 * {@code 1} bila aspek itu belum punya rincian).</p>
	 *
	 * @return nomor urut tampil, atau {@code 1} bila belum diisi
	 */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampil. Tanpa validasi; nilai {@code null}, nol, maupun negatif diterima,
	 * dan nomor yang bertabrakan antar-rincian juga tidak dicegah.
	 *
	 * @param nomorUrut nomor urut baru
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Mengembalikan penanda apakah rincian aspek ini masih dipakai, dengan <b>default aman
	 * {@code true}</b>: baris lama yang kolomnya masih {@code null} (termasuk seluruh baris hasil
	 * auto-seed {@code InitDataHelper}, yang tidak pernah menyetel field ini) tetap dianggap aktif.
	 *
	 * <p>Bendera ini <b>benar-benar ditegakkan</b> di satu tempat: {@code KegiatanKesiswaanAction}
	 * menyaring isi combobox {@code "Rincian Aspek Kegiatan *"} dengan
	 * {@code Restrictions.or(isNull("aktif"), eq("aktif", true))} (digabung {@code AND} dengan filter
	 * aspek induk terpilih), sehingga menonaktifkan satu rincian langsung menyembunyikannya dari form
	 * pengajuan kegiatan baru. Penyaringan hanya berlaku untuk <i>pilihan baru</i> &mdash; kegiatan
	 * lama yang sudah terlanjur menunjuk rincian nonaktif tetap tampil apa adanya.</p>
	 *
	 * <p><b>Tempat bendera ini TIDAK ditegakkan (terverifikasi):</b></p>
	 * <ul>
	 *   <li>{@code AmbilDataKegiatanForKegiatanKesiswaanHelper.initCriteria()} &mdash; layar tempat
	 *       siswa memilih sendiri kegiatan yang akan diikuti &mdash; menyaring {@code aktif} dan
	 *       {@code bisaDipilihSiswa} milik <b>aspek induk saja</b>
	 *       ({@code kelompokKegiatanKesiswaan.*}), tidak pernah milik rincian. Kegiatan yang
	 *       digantungkan pada rincian nonaktif tetap muncul di daftar pilihan siswa;</li>
	 *   <li>layar Angka Kredit ({@code NilaiKegiatanKesiswaanAction.initCriteria()}) tidak menyaring
	 *       {@code aktif} sama sekali &mdash; rincian nonaktif tetap tampil dan tetap memicu
	 *       pembuatan baris {@link NilaiKegiatanKesiswaan} baru saat dirender.</li>
	 * </ul>
	 *
	 * <p>Di panel daftar, bendera ini berupa checkbox "Aktif" yang menyimpan perubahannya
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
	 * @param aktif {@code true} bila rincian masih dipakai
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan penanda apakah rincian aspek ini boleh dipilih oleh <b>akun siswa</b> saat
	 * mengajukan kegiatan sendiri &mdash; dan, sebagai efek samping, <b>menegakkan pewarisan bendera
	 * itu dari aspek induk dengan cara MENULIS ke field sendiri</b>.
	 *
	 * <h3>Perilaku</h3>
	 * <ol>
	 *   <li>bila {@link #kelompokKegiatanKesiswaan} tidak {@code null} dan
	 *       {@link KelompokKegiatanKesiswaan#getBisaDipilihSiswa()} miliknya bernilai {@code false},
	 *       field {@link #bisaDipilihSiswa} <b>ditimpa {@code false}</b>;</li>
	 *   <li>nilai kembaliannya adalah field tersebut, dengan default {@code true} bila masih
	 *       {@code null}.</li>
	 * </ol>
	 *
	 * <h3>GETTER DESTRUKTIF &mdash; efeknya permanen di tingkat data</h3>
	 * <p>Langkah 1 bukan sekadar perhitungan nilai kembalian: ia mengubah state entity. Karena baris
	 * ini umumnya berstatus <i>managed</i> saat dibaca, perubahan itu ikut ter-{@code flush} sebagai
	 * {@code UPDATE} senyap. Yang membuatnya sulit dihindari: pemetaan entity ini memakai
	 * <b>property access</b> (anotasi JPA ada di getter), sehingga <b>Hibernate sendiri memanggil
	 * getter ini</b> saat pemeriksaan dirty-state pada setiap {@code flush} &mdash; penulisan bisa
	 * terjadi walaupun tidak ada kode aplikasi yang menyentuh method ini.</p>
	 *
	 * <p>Konsekuensi yang perlu disadari: menonaktifkan bendera boleh-dipilih-siswa di aspek induk
	 * <b>mematikan seluruh rinciannya secara permanen</b>. Mengaktifkan kembali aspek induk
	 * <i>tidak</i> memulihkan rinciannya, karena kolom anak sudah terlanjur tertulis {@code false} dan
	 * harus dicentang ulang satu per satu lewat checkbox "Bisa Dipilih Siswa" di panel daftar. Dicatat
	 * apa adanya sebagai temuan, bukan anjuran perubahan di berkas ini.</p>
	 *
	 * <h3>Jebakan proxy lazy</h3>
	 * <p>Method ini membaca <b>field mentah</b> {@link #kelompokKegiatanKesiswaan}, bukan
	 * {@link #getKelompokKegiatanKesiswaan()} yang meresolusi proxy lewat
	 * {@link GeneralValueObject#check(Object)}. Karena relasi induk dipetakan {@code FetchType.LAZY},
	 * field itu bisa berisi proxy yang belum terinisialisasi; memanggil
	 * {@code getBisaDipilihSiswa()} padanya akan memicu inisialisasi &mdash; dan melempar
	 * {@code LazyInitializationException} bila entity sudah lepas dari session. Penjaga
	 * {@code != null} pada baris pertama juga tidak menolong: proxy selalu bukan {@code null},
	 * termasuk untuk baris yatim yang FK-nya {@code NULL} di database.</p>
	 *
	 * <h3>Penegakan di layar</h3>
	 * <p>{@code KegiatanKesiswaanAction} memasang filter
	 * {@code or(isNull("bisaDipilihSiswa"), eq("bisaDipilihSiswa", true))} pada combobox rincian
	 * <b>hanya bila pengguna yang login adalah siswa</b> ({@code tbmuser.getSiswa() != null}); untuk
	 * operator/admin/guru filternya diganti {@code sqlRestriction("true")} sehingga seluruh rincian
	 * tetap terlihat. Jadi bendera ini membatasi pengajuan mandiri siswa, bukan visibilitas data
	 * secara umum. Sama seperti {@link #getAktif()},
	 * {@code AmbilDataKegiatanForKegiatanKesiswaanHelper} (layar siswa memilih kegiatan yang sudah
	 * ada) <b>tidak</b> menyaring bendera milik rincian &mdash; hanya milik aspek induk.</p>
	 *
	 * @return {@code true} bila siswa boleh memilih rincian ini, atau kolomnya masih {@code null};
	 *         {@code false} bila dinonaktifkan eksplisit atau diwarisi {@code false} dari aspek induk
	 * @see KelompokKegiatanKesiswaan#getBisaDipilihSiswa()
	 */
	public Boolean getBisaDipilihSiswa() {
		if (kelompokKegiatanKesiswaan != null && !kelompokKegiatanKesiswaan.getBisaDipilihSiswa()) {
			bisaDipilihSiswa = false;
		}
		return bisaDipilihSiswa == null ? true : bisaDipilihSiswa;
	}

	/**
	 * Menyetel penanda boleh-dipilih-siswa. Tanpa validasi; {@code null} diterima dan akan terbaca
	 * sebagai {@code true}.
	 *
	 * <p>Di panel daftar berupa checkbox "Bisa Dipilih Siswa" yang menyimpan perubahan seketika
	 * ({@code Common.refreshSaveOrUpdate}) tanpa konfirmasi.</p>
	 *
	 * <p><b>Perhatian:</b> nilai yang disetel di sini <b>tidak bertahan</b> selama aspek induknya
	 * bernilai {@code false} &mdash; {@link #getBisaDipilihSiswa()} akan menimpanya kembali pada
	 * pembacaan berikutnya. Aktifkan bendera di aspek induk lebih dulu sebelum mencentang ulang di
	 * sini.</p>
	 *
	 * @param bisaDipilihSiswa {@code true} bila siswa boleh memilih rincian ini
	 * @see #getBisaDipilihSiswa()
	 */
	public void setBisaDipilihSiswa(Boolean bisaDipilihSiswa) {
		this.bisaDipilihSiswa = bisaDipilihSiswa;
	}

}
