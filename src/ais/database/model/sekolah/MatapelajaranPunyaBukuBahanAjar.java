package ais.database.model.sekolah;

// Generated Dec 22, 2009 12:14:16 PM by Hibernate Tools 3.2.4.CR1

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.BukuBahanAjar;
import ais.database.model.GeneralValueObject;

/**
 * Baris penghubung <b>daftar buku diktat/bahan ajar</b> milik satu mata pelajaran sekolah:
 * memasangkan sebuah {@link Matapelajaran} (mata pelajaran pada satu sekolah/yayasan) dengan sebuah
 * {@link BukuBahanAjar} — entry <b>katalog buku/diktat karya pengajar</b> pada tabel global
 * {@code public.buku_bahan_ajar} (judul, ISBN/ISSN, penerbit, tahun, tautan repositori, sampai lima
 * penulis dosen + tiga penulis luar). Satu baris = satu buku ajar yang direkomendasikan untuk satu
 * mata pelajaran; entity ini murni <i>association table</i> tanpa muatan data sendiri sama sekali.
 *
 * <h2>Ini BUKAN kerabat paket perpustakaan</h2>
 * <p>Nama kelas ini sangat mirip {@link JadwalPelajaranPunyaItem}, dan keduanya memang lahir dari
 * cetakan {@code hbm2java} yang sama (lihat catatan {@code serialVersionUID} di bawah), tetapi
 * <b>sisi kanannya berbeda paket dan berbeda konsep</b> — sudah diverifikasi dari kode:</p>
 * <ul>
 *   <li>{@link JadwalPelajaranPunyaItem} menunjuk {@code ais.database.model.library.Item}
 *   (tabel {@code library.item}) — <b>katalog perpustakaan</b>, dengan alur "Ambil Google Book",
 *   sampul, hasil pindai per halaman.</li>
 *   <li>Kelas ini menunjuk {@link ais.database.model.BukuBahanAjar} (tabel
 *   {@code public.buku_bahan_ajar}) — <b>katalog buku/diktat karya pengajar</b>, tabel yang sama
 *   yang dipakai menu "Buku Bahan Ajar" sisi perguruan tinggi, komponen BKD <i>"Penulis Buku"</i>
 *   ({@code BkdPenulisHelper}), profil dosen, dan sinkronisasi repositori DSpace.</li>
 * </ul>
 * <p>Pada layar Aktivitas Pembelajaran keduanya bahkan tampil berdampingan sebagai dua sub-tab:
 * "Buku" (referensi perpustakaan) dan "Bahan Ajar"/"Buku Diktat / Ajar" (kelas ini). Jadi tautan
 * ke paket {@code library} <b>tidak ada</b> di sini; kekerabatannya justru dengan katalog karya
 * pengajar milik modul PT.</p>
 *
 * <h2>Kembaran sisi perguruan tinggi</h2>
 * <p>Padanan persis kelas ini untuk jenjang PT adalah {@link ais.database.model.MatakuliahPunyaBukuBahanAjar}
 * (tabel {@code public.matakuliah_punya_buku_bahan_ajar}, helper UI {@code BukuBahanAjarHelper}):
 * struktur field, gaya setter, dan bahkan {@code serialVersionUID}-nya identik — hanya sisi kirinya
 * yang berganti dari {@code Matakuliah} ke {@link Matapelajaran} dan skemanya dari {@code public}
 * ke {@code sekolah}. Keduanya berbagi katalog buku yang <b>sama</b>.</p>
 *
 * <h2>Pemetaan</h2>
 * <ul>
 *   <li>Tabel {@code sekolah.matapelajaran_punya_buku_bahan_ajar}, {@code dynamicInsert}/
 *   {@code dynamicUpdate} aktif (hanya kolom yang berubah yang ikut ke SQL).</li>
 *   <li>{@code @Audited} (Hibernate Envers) — penambahan/penghapusan tautan ikut tercatat di tabel
 *   revisi. Perhatikan tombol "Revisi" pada grid pemakainya justru merevisi
 *   {@link BukuBahanAjar}, bukan baris penghubung ini.</li>
 *   <li>Kolom: {@code id} (IDENTITY), {@code matapelajaran} (FK wajib), {@code buku_bahan_ajar}
 *   (FK wajib), {@code oleh}, {@code oleh_id}, {@code tanggal_dirubah}. <b>Tidak ada kolom muatan
 *   apa pun</b> — bahkan {@code keterangan} tidak dipetakan di sini (berbeda dari
 *   {@link JadwalPelajaranPunyaItem} yang punya catatan bebas).</li>
 *   <li><b>Tidak ada kolom {@code sekolah}/{@code yayasan}.</b> Cakupan tenant sepenuhnya
 *   diturunkan dari {@link #getMatapelajaran()} ({@code Matapelajaran.sekolah_id} wajib,
 *   {@code yayasan_id} opsional); seluruh pembacaan produktif memfilter
 *   {@code Restrictions.eq("matapelajaran", ...)} sehingga isolasi terjaga secara tidak langsung.
 *   Sisi kanannya ({@link BukuBahanAjar}) justru <b>global lintas tenant</b> — katalog buku dipakai
 *   bersama seluruh instalasi, termasuk modul PT.</li>
 *   <li><b>Tidak ada unique constraint</b> pada pasangan ({@code matapelajaran},
 *   {@code buku_bahan_ajar}); pencegahan duplikat hanya dilakukan di UI (dialog pemilihan
 *   mengecualikan buku yang sudah tertaut).</li>
 * </ul>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ol>
 *   <li><b>Identitas</b> — {@link #getId()}/{@link #setId(Long)}, {@link #toString()}.</li>
 *   <li><b>Jejak audit</b> — {@link #getOleh()}/{@link #setOleh(String)},
 *   {@link #getOlehId()}/{@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan kait
 *   {@link #onUpdate()}.</li>
 *   <li><b>Relasi</b> — {@link #getMatapelajaran()} (sisi mata pelajaran, lazy + resolusi proxy)
 *   dan {@link #getBukuBahanAjar()} (sisi katalog buku ajar), keduanya {@code @ManyToOne} wajib.</li>
 * </ol>
 * <p>Tidak ada method dengan logika bisnis di kelas ini. Seluruh perilaku (pengambilan buku,
 * penambahan buku baru, notifikasi email, pencetakan daftar rujukan) berada di pemanggil.</p>
 *
 * <h2>Siapa yang memakai (terverifikasi)</h2>
 * <ul>
 *   <li>{@code ais.action.master.sekolah.helper.BukuBahanAjarMatapelajaranHelper} — layar utamanya:
 *   grid "Daftar buku ajar" (label tab "Buku Diktat / Ajar (n)") dengan tombol <i>Ambil Buku Ajar</i>
 *   (memilih banyak buku dari katalog lewat {@code AmbilDataBukuBahanAjarBanyak}) dan <i>Tambah
 *   Buku Ajar</i> (membuat entry {@link BukuBahanAjar} baru langsung dari layar ini), plus per baris:
 *   unggah/unduh berkas buku &amp; sampul ({@code LampiranLain.BUKU}/{@code COVER_BUKU}), info
 *   pengarang dosen, kutipan sitasi, dan hapus.</li>
 *   <li>{@code ais.action.master.sekolah.helper.AktifitasPembelajaranHelper} — menyisipkan helper di
 *   atas sebagai sub-tab "Bahan Ajar" di dalam tab "Ref." layar Aktivitas Pembelajaran (e-learning).
 *   Layar ini juga dibuka dari kalender mingguan siswa
 *   ({@code CalendarJadwalPelajaranMingguIniComposer}/{@code CalendarPerkuliahanMingguIniComposer})
 *   dan dari {@code DetailKelasLesSiswaHelper}, jadi baris-baris ini ikut terlihat oleh siswa.</li>
 *   <li>{@code ais.common.CommonUiFactoryHelper.getDeskripsiJadwalPelajaranHbox(...)} — ubin
 *   statistik "Buku Ajar (n bundel)" pada kotak deskripsi jadwal pelajaran; mengkliknya membuka
 *   helper yang sama di jendela modal. Ubin ini dipakai {@code PertemuanJadwalPelajaranAction} dan
 *   {@code RekapitulasiJadwalPelajaranHelper}.</li>
 *   <li>{@code JadwalPelajaranAction} dan {@code PertemuanJadwalPelajaranAction} — bagian "Daftar
 *   Rujukan" pada cetak silabus/RPP; keduanya memakai
 *   {@code Projections.groupProperty("bukuBahanAjar")} sehingga buku yang kebetulan terdaftar dua
 *   kali tetap tercetak sekali.</li>
 *   <li>{@code ais.common.CommonEmail#infoAdaBukuAjar(JadwalPelajaran, MatapelajaranPunyaBukuBahanAjar)}
 *   — dipicu setiap kali baris baru dibuat lewat helper di atas <i>dengan konteks jadwal</i>:
 *   mengirim email berjudul "Pengumuman Resmi Sekolah: Penyediaan Modul dan Referensi Buku Bahan
 *   Ajar" ke guru pengampu <b>dan seluruh siswa kelas</b> tersebut. Lihat catatan keamanan di
 *   bawah.</li>
 *   <li>{@code hibernate.cfg.xml} mendaftarkan kelas ini sebagai mapping, dan manifest
 *   {@code general_value_object_inventory} menandainya {@code ELIGIBLE_METADATA_FIRST} (kandidat
 *   layar CRUD generik, masih <i>disabled</i>).</li>
 * </ul>
 *
 * <h2>Hal non-obvious &amp; jebakan</h2>
 * <ul>
 *   <li><b>Menyimpan baris ini bisa MENULIS ke katalog buku global.</b> Kedua relasi memakai
 *   {@code cascade = {PERSIST, MERGE}}. Pada jalur "Tambah Buku Ajar", objek {@link BukuBahanAjar}
 *   hasil {@code BukuBahanAjarAction.onAddExternal(...)} dipasang ke baris ini lalu baris inilah
 *   yang di-{@code session.save}. Jadi "menambah buku ajar pada satu mata pelajaran" adalah operasi
 *   tulis terhadap tabel {@code public.buku_bahan_ajar} yang dipakai bersama seluruh instalasi —
 *   termasuk oleh perhitungan BKD dosen dan sinkronisasi DSpace.</li>
 *   <li><b>Tidak ada {@code @PrePersist}.</b> Hanya {@link #onUpdate()} ({@code @PreUpdate}) yang
 *   ada, sehingga {@code oleh}/{@code oleh_id} masih {@code null} pada INSERT dan baru terisi bila
 *   baris pernah di-UPDATE. Karena kelas ini <b>tidak punya kolom yang bisa disunting sama sekali</b>
 *   (hanya dua FK), praktisnya baris tidak pernah di-UPDATE dan kolom atribusi <b>permanen kosong</b>
 *   — atribusi hanya bisa dibaca dari tabel revisi Envers.</li>
 *   <li><b>{@code compareTo} selalu bernilai 0 — jangan taruh entity ini di koleksi terurut.</b>
 *   {@link GeneralValueObject#compareTo(GeneralValueObject)} mencoba berturut-turut
 *   {@code nomorUrut}, {@code nim}, {@code nama}, lalu {@code keterangan}. Kelas ini tidak memetakan
 *   satu pun dari keempatnya, sehingga tiga kunci pertama selalu {@code null} dan kunci keempat
 *   selalu {@code ""} (normalisasi {@link GeneralValueObject#getKeterangan()}) — hasil
 *   perbandingannya selalu {@code 0}. Akibatnya {@code TreeSet}/{@code TreeMap} berisi entity ini
 *   akan menciut jadi satu elemen. Seluruh kode yang ada memakai {@code List} +
 *   {@code Order.asc("id")}, jadi bug ini <b>laten</b>, bukan aktif.</li>
 *   <li><b>Duplikat mungkin secara skema, dicegah hanya di UI.</b> Jalur "Ambil Buku Ajar" lebih
 *   dulu mengambil daftar buku yang sudah tertaut dan mengoperkannya ke
 *   {@code AmbilDataBukuBahanAjarBanyak} sebagai daftar pengecualian, sedangkan jalur "Tambah Buku
 *   Ajar" selalu membuat buku baru. Cetak silabus tetap aman karena memakai {@code groupProperty}.</li>
 *   <li><b>{@link GeneralValueObject} BUKAN {@code @Entity}/{@code @MappedSuperclass}</b> —
 *   Hibernate tidak memetakan properti induk. Karena itu {@code id}, {@code oleh}, {@code olehId},
 *   dan {@code tanggal_dirubah} <b>sengaja dideklarasikan ulang</b> di kelas ini. Itu keharusan
 *   teknis, bukan duplikasi yang perlu "dibersihkan"; menghapusnya membuat kolom-kolom tersebut
 *   hilang dari skema. Lihat {@link ais.database.model.GeneralValueObject}.</li>
 *   <li><b>Javadoc lama kelas ini berbunyi <i>"BukuBahanAjar generated by hbm2java"</i></b> — teks
 *   salah salin dari cetakan generator tahun 2009 yang menamai kelas ini dengan nama entity lain.
 *   Dokumentasi ini menggantikannya.</li>
 * </ul>
 *
 * <h2>Catatan keamanan pada layar pemakainya</h2>
 * <p>Bukan cacat pada entity ini, tetapi relevan bagi siapa pun yang menyentuhnya. Pada
 * {@code BukuBahanAjarMatapelajaranHelper} <b>seluruh</b> gerbang tampilan kontrol hanya menguji
 * {@code Common.getCurrentUser().getMahasiswa() == null} — akun <b>siswa</b> lolos dari syarat itu,
 * dan tidak ada satu pun panggilan {@code checkPrevilages} di helper tersebut:</p>
 * <ul>
 *   <li>Toolbar <i>Ambil Buku Ajar</i>/<i>Tambah Buku Ajar</i> tampil bagi siswa. "Tambah Buku Ajar"
 *   membuat baris baru di katalog global {@code public.buku_bahan_ajar} lewat cascade dari objek
 *   ini — data yang sama yang dipakai modul BKD/repositori sisi perguruan tinggi.</li>
 *   <li>Tombol <i>Hapus</i> per baris juga hanya disembunyikan untuk mahasiswa, sehingga siswa dapat
 *   menghapus daftar buku ajar milik gurunya; jalur hapus ({@code Common.refreshDelete}) tidak
 *   memeriksa hak apa pun. Ini <b>pola yang sama persis</b> dengan cacat pada
 *   {@code JadwalPelajaranPunyaItemHelper} (tab bersebelahan di layar yang sama) — di sana hanya
 *   tombol Hapus yang salah gerbang, di sini seluruh toolbar penambahan ikut salah gerbang.</li>
 *   <li>Setiap penambahan lewat konteks jadwal memicu {@code CommonEmail.infoAdaBukuAjar(...)},
 *   yang mengirim email <b>atas nama sekolah</b> ke guru pengampu dan seluruh siswa kelas. Dengan
 *   gerbang di atas, seorang siswa dapat memicu pengiriman massal berjudul "Pengumuman Resmi
 *   Sekolah" berisi judul buku yang ia tentukan sendiri.</li>
 *   <li>Layar ini dijangkau siswa lewat tab "Ref." &rarr; "Bahan Ajar" pada Aktivitas Pembelajaran
 *   (dibuka dari kalender mingguan siswa) dan lewat ubin "Buku Ajar" pada kotak deskripsi jadwal
 *   ({@code CommonUiFactoryHelper}), yang tidak punya gerbang tampilan sama sekali.</li>
 * </ul>
 * <p>Catatan tambahan: {@code AmbilDataBukuBahanAjarBanyak} menelusuri katalog buku <b>tanpa filter
 * sekolah/yayasan</b>, dan {@code FileBukuAjarHelper} pada detail baris juga dikonstruksi dengan
 * flag sunting {@code getMahasiswa() == null} yang sama.</p>
 *
 * @see Matapelajaran
 * @see ais.database.model.BukuBahanAjar
 * @see ais.database.model.MatakuliahPunyaBukuBahanAjar
 * @see JadwalPelajaranPunyaItem
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "matapelajaran_punya_buku_bahan_ajar")
public class MatapelajaranPunyaBukuBahanAjar extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya dibangkitkan sekali saat kelas dibuat dan <b>tidak boleh
	 * diubah</b>: instance entity AIS diserialkan ke cache in-memory/MapDB dan dibawa lintas request
	 * ZK, sehingga mengubah nilai ini membuat data cache lama gagal dibaca.
	 *
	 * <p>Nilai {@code 1950126270979098967L} dipakai bersama oleh <b>sepuluh</b> entity penghubung
	 * lain yang lahir dari cetakan {@code hbm2java} yang sama — antara lain
	 * {@link ais.database.model.MatakuliahPunyaBukuBahanAjar},
	 * {@link ais.database.model.DataPunyaBukuBahanAjar}, {@link JadwalPelajaranPunyaItem}, dan
	 * {@link ais.database.model.PerkuliahanPunyaItem}. Kesamaan angka itu <b>tidak bermakna apa
	 * pun</b> secara semantik; ia hanya penanda bahwa berkas-berkas itu disalin dari template yang
	 * sama, dan bukan indikasi bahwa mereka memetakan tabel yang sama.</p>
	 */
	private static final long serialVersionUID = 1950126270979098967L;
	/** Primary key baris penghubung; diisi database (IDENTITY). Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang meng-UPDATE baris ini; diisi otomatis lewat {@link #onUpdate()}. */
	private String oleh;
	/** Id pengguna terakhir yang meng-UPDATE baris ini; diisi otomatis lewat {@link #onUpdate()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah di-UPDATE — pada praktiknya
	 *         hampir selalu {@code null}, karena baris ini tidak punya kolom yang bisa disunting
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah. <b>Menolak diam-diam</b> nilai {@code null} maupun string
	 * kosong/hanya spasi: pada kasus itu method langsung kembali tanpa mengubah apa pun, sehingga
	 * nilai lama dipertahankan dan atribusi audit tidak pernah "terhapus" oleh konteks anonim
	 * (mis. job latar atau permintaan tanpa pengguna login).
	 *
	 * @param olehId id pengguna baru; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null}/kosong <b>diabaikan diam-diam</b> agar atribusi lama tidak tertimpa.
	 *
	 * @param oleh nama pengguna baru; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila baris belum pernah di-UPDATE
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait {@code @PreUpdate} — dipanggil Hibernate/JPA <b>otomatis sebelum setiap UPDATE</b>
	 * terhadap baris ini, lalu meneruskan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@code oleh}/{@code olehId}/{@code tanggal_dirubah} dari konteks pengguna aktif. Tidak pernah
	 * dipanggil manual dari kode aplikasi. Ini juga implementasi wajib dari satu-satunya method
	 * {@code abstract} milik {@link GeneralValueObject}.
	 *
	 * <p><b>Praktisnya tidak pernah berjalan.</b> Kait ini tidak aktif pada INSERT (kelas ini tidak
	 * punya {@code @PrePersist}), sementara baris penghubung ini hanya berisi dua FK dan tidak punya
	 * satu pun kolom yang bisa disunting dari UI — pemakainya hanya menambah dan menghapus baris.
	 * Karena itu {@code oleh}/{@code oleh_id} pada tabel ini umumnya kosong selamanya; riwayat
	 * pelakunya hanya tersedia lewat Envers.</p>
	 *
	 * <p><b>Perhatian pemeliharaan:</b> baris fisik yang sama juga mendeklarasikan field
	 * {@code tanggal_dirubah} (gaya asli berkas, dipertahankan). Nilai awalnya diambil dari
	 * {@code ais.ui.util.WaktuUtil.getDate()} sehingga object baru sudah bertanggal sejak dibuat,
	 * bukan {@code null}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Umumnya tidak dipanggil aplikasi secara langsung —
	 * pengisiannya dilakukan {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah waktu perubahan baru; {@code null} diterima apa adanya
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (kolom {@code timestamp}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada object yang baru dibuat
	 *         karena field-nya sudah diinisialisasi dengan waktu saat ini
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks pasangan matapelajaran&ndash;buku, berformat
	 * {@code "<matapelajaran>_<bukuBahanAjar>"}. Meng-override {@link GeneralValueObject#toString()}
	 * (yang berformat {@code "kode - nama"} dan tidak berarti apa-apa untuk baris penghubung tanpa
	 * kode/nama).
	 *
	 * <p><b>Kedua nilai dibaca lewat getter, bukan lewat field</b>
	 * ({@link #getMatapelajaran()}/{@link #getBukuBahanAjar()}), lalu hasilnya ditulis balik ke
	 * field. Penulisan balik itu <b>bukan efek samping destruktif</b>: nilainya hanya menyalin apa
	 * yang baru saja dikembalikan getter yang bersangkutan (objek setara-identitas, sekadar sudah
	 * terinisialisasi), dan {@link #getMatapelajaran()} memang sudah menulis balik field yang sama.
	 * Baris {@code bukuBahanAjar = getBukuBahanAjar()} bahkan tepat berarti {@code x = x}.
	 * Bandingkan dengan {@link JadwalPelajaranPunyaItem#toString()} yang justru membaca field
	 * mentah; versi di sini <b>lebih aman</b> karena proxy lazy {@code matapelajaran} diresolusi
	 * {@link GeneralValueObject#check(Object)} lebih dulu.</p>
	 *
	 * @return gabungan {@code toString()} mata pelajaran dan buku bahan ajar, dipisah garis bawah
	 */
	public String toString() {
		matapelajaran = getMatapelajaran();
		bukuBahanAjar = getBukuBahanAjar();
		return matapelajaran + "_" + bukuBahanAjar;
	}

	/** Mata pelajaran pemilik daftar buku ajar ini (FK wajib, dimuat lazy). Lihat {@link #getMatapelajaran()}. */
	private Matapelajaran matapelajaran;
	/** Entry katalog buku/diktat bahan ajar yang dirujuk (FK wajib). Lihat {@link #getBukuBahanAjar()}. */
	private BukuBahanAjar bukuBahanAjar;

	/**
	 * Constructor default tanpa argumen. WAJIB ada karena Hibernate memakainya saat menghidrasi
	 * baris dari database, dan juga dipakai langsung oleh kedua jalur penambahan pada
	 * {@code BukuBahanAjarMatapelajaranHelper} ("Ambil Buku Ajar" dan "Tambah Buku Ajar") sebelum
	 * {@link #setBukuBahanAjar(BukuBahanAjar)}/{@link #setMatapelajaran(Matapelajaran)} dipanggil.
	 */
	public MatapelajaranPunyaBukuBahanAjar() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * <p>Dipetakan {@code IDENTITY} dengan {@code insertable = false}: nilainya sepenuhnya
	 * ditentukan sequence/serial database dan tidak pernah dikirim pada INSERT. Kolom ini juga
	 * dasar {@link GeneralValueObject#equals(Object)} — perhatikan bahwa {@code hashCode()} tidak
	 * di-override di hierarki ini, jadi jangan mendeduplikasi entity ini lewat {@code HashSet}.
	 * Seluruh pembacaan daftar mengurutkannya dengan {@code Order.asc("id")}.</p>
	 *
	 * @return id baris, atau {@code null} bila object belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Praktis hanya dipakai Hibernate saat hidrasi, atau untuk membuat object
	 * "penunjuk" berisi id saja sebagai parameter kriteria.
	 *
	 * @param id nilai primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan mata pelajaran pemilik daftar buku ajar ini, dengan <b>resolusi proxy lazy</b>
	 * lebih dulu.
	 *
	 * <p>Relasi {@code @ManyToOne} <b>wajib</b> ({@code nullable = false}) ke kolom
	 * {@code matapelajaran}, dipetakan {@code FetchType.LAZY} dengan cascade
	 * {@code PERSIST}/{@code MERGE}. Karena itu getter memakai pola standar AIS
	 * {@code matapelajaran = check(matapelajaran)}: {@link GeneralValueObject#check(Object)} menukar
	 * proxy Hibernate dengan instance kanonik (identity map / cache / database) sehingga getter
	 * tetap aman dipanggil atas object yang sudah <i>detached</i> — situasi normal di AIS karena
	 * entity hidup lebih lama daripada {@code Session} yang memuatnya. Penulisan balik ke field
	 * bukan varian "getter write-back destruktif": nilai yang ditulis setara-identitas dengan proxy
	 * semula (kelas + id sama), hanya sudah terinisialisasi.</p>
	 *
	 * <p>Inilah <b>satu-satunya pembawa konteks tenant</b> baris ini: sekolah dan yayasan dibaca
	 * dari {@code Matapelajaran.sekolah}/{@code Matapelajaran.yayasan}. Seluruh pembacaan produktif
	 * menyaring properti ini ({@code Restrictions.eq("matapelajaran", ...)}), termasuk grid helper,
	 * cetak daftar rujukan silabus/RPP, dan hitungan ubin "Buku Ajar" pada dasbor jadwal.</p>
	 *
	 * @return mata pelajaran pemilik; secara skema tidak pernah {@code null} untuk baris tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "matapelajaran", nullable = false)
	public Matapelajaran getMatapelajaran() {
		matapelajaran = check(matapelajaran);
		return this.matapelajaran;
	}

	/**
	 * Menyetel mata pelajaran pemilik daftar buku ajar ini. Dipanggil oleh kedua jalur penambahan di
	 * {@code BukuBahanAjarMatapelajaranHelper} sebelum baris disimpan; nilainya di sana selalu
	 * diambil dari {@code jadwalPelajaran.getMatapelajaran()}.
	 *
	 * <p><b>Efek samping cascade:</b> karena relasi ini ber-cascade {@code PERSIST}/{@code MERGE},
	 * menyimpan baris penghubung juga akan mem-persist/merge objek mata pelajaran yang dipasang di
	 * sini.</p>
	 *
	 * <p><b>Perhatikan cakupannya:</b> tautan dibuat ke <i>mata pelajaran</i>, bukan ke jadwal
	 * pelajaran satu kelas. Buku yang ditambahkan dari layar satu kelas otomatis muncul pada semua
	 * jadwal/kelas lain yang memakai mata pelajaran yang sama di sekolah itu.</p>
	 *
	 * @param matapelajaran mata pelajaran pemilik; secara skema tidak boleh {@code null} (INSERT
	 *                      dengan {@code null} akan ditolak constraint kolom)
	 */
	public void setMatapelajaran(Matapelajaran matapelajaran) {
		this.matapelajaran = matapelajaran;
	}

	/**
	 * Mengembalikan entry katalog buku/diktat bahan ajar yang dirujuk.
	 *
	 * <p>Relasi {@code @ManyToOne} <b>wajib</b> ({@code nullable = false}) ke kolom
	 * {@code buku_bahan_ajar} tabel <b>global</b> {@code public.buku_bahan_ajar}, dengan
	 * {@code FetchMode.SELECT} (dimuat lewat query terpisah, bukan {@code join} pada query induk)
	 * dan cascade {@code PERSIST}/{@code MERGE}. Berbeda dari {@link #getMatapelajaran()}, getter
	 * ini <b>tidak</b> memanggil {@link GeneralValueObject#check(Object)}; relasi ini memakai fetch
	 * EAGER bawaan {@code @ManyToOne} sehingga umumnya sudah terisi penuh saat baris dimuat.</p>
	 *
	 * <p>Nilai kembalinya dibaca luas oleh grid buku ajar dan cetakan: judul, pengarang (blok info
	 * dosen bila {@code getPengarangAdalahDosen()}, atau tiga nama penulis luar), ISBN, penerbit,
	 * link, keterangan, tahun, tombol kutipan/sitasi, tombol Revisi, serta unggah/unduh berkas buku
	 * dan sampul lewat {@code LampiranLain.BUKU}/{@code LampiranLain.COVER_BUKU}. Juga dipakai
	 * {@code CommonEmail.infoAdaBukuAjar(...)} untuk mengisi judul pada email pengumuman.</p>
	 *
	 * @return entry katalog buku bahan ajar yang dirujuk; secara skema tidak pernah {@code null}
	 *         untuk baris tersimpan, meski pemanggil di {@code CommonEmail} tetap memeriksanya
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "buku_bahan_ajar", nullable = false)
	public BukuBahanAjar getBukuBahanAjar() {
		return this.bukuBahanAjar;
	}

	/**
	 * Menyetel entry katalog buku/diktat bahan ajar yang dirujuk.
	 *
	 * <p><b>Efek samping paling penting di kelas ini:</b> relasi ber-cascade
	 * {@code PERSIST}/{@code MERGE}, sehingga menyimpan baris penghubung ikut menyimpan
	 * {@link BukuBahanAjar} yang dipasang di sini. Jalur "Tambah Buku Ajar" pada
	 * {@code BukuBahanAjarMatapelajaranHelper} mengandalkan persis perilaku itu — buku baru hasil
	 * {@code BukuBahanAjarAction.onAddExternal(...)} tidak disimpan eksplisit, melainkan lahir di
	 * {@code public.buku_bahan_ajar} lewat cascade dari objek ini. Konsekuensinya menambahkan buku
	 * ajar pada satu mata pelajaran sekolah adalah operasi tulis terhadap katalog buku yang dipakai
	 * bersama seluruh instalasi, termasuk modul perguruan tinggi (BKD "Penulis Buku", profil dosen,
	 * sinkronisasi DSpace).</p>
	 *
	 * @param bukuBahanAjar entry katalog buku bahan ajar; secara skema tidak boleh {@code null}
	 */
	public void setBukuBahanAjar(BukuBahanAjar bukuBahanAjar) {
		this.bukuBahanAjar = bukuBahanAjar;
	}

}
