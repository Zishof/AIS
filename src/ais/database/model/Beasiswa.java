package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.common.Common;

/**
 * Entity <b>master program beasiswa</b> &mdash; satu baris tabel {@code public.beasiswa} mewakili
 * satu tawaran beasiswa yang dibuka kampus (mis. "Bidikmisi 2026", "Beasiswa Yayasan Semester
 * Ganjil"), lengkap dengan jendela waktu pendaftarannya, sasaran penerimanya, dan ambang syarat
 * kelayakan akademis/ekonomi yang harus dipenuhi mahasiswa.
 *
 * <p>Kelas ini <b>hanya menyimpan definisi program</b>. Ia tidak menyimpan daftar pendaftar, tidak
 * menyimpan hasil seleksi, dan &mdash; perlu dicatat &mdash; <b>tidak punya field kuota/jumlah
 * penerima</b> sama sekali. Semua itu hidup di entity lain yang menunjuk balik ke sini.</p>
 *
 * <h2>Posisi dalam alur beasiswa</h2>
 * <p>Relasi ke kelas ini seluruhnya <b>searah dari sisi anak</b>: {@code Beasiswa} tidak
 * mendeklarasikan satu pun koleksi {@code @OneToMany}, jadi untuk mengambil "semua pendaftar
 * beasiswa X" kode selalu menjalankan Criteria/HQL sendiri dengan
 * {@code Restrictions.eq("beasiswa", beasiswa)}.</p>
 * <ol>
 * <li><b>Definisi syarat</b> &mdash; {@link ais.database.model.beasiswa.PersyaratanBeasiswa}
 * (katalog butir persyaratan) dikaitkan ke program lewat
 * {@link ais.database.model.beasiswa.BeasiswaPunyaPersyaratan}.</li>
 * <li><b>Pendaftaran mahasiswa</b> &mdash; {@link ais.database.model.beasiswa.MahasiswaDaftarBeasiswa}
 * (baris pendaftaran + kolom {@code terima} sebagai hasil seleksi + {@code totalSkor}), jawaban
 * per butir syarat di {@link ais.database.model.beasiswa.MahasiswaBeasiswaPersyaratan}, dan berkas
 * unggahan di {@link ais.database.model.file.LampiranBeasiswaMahasiswa}.</li>
 * <li><b>Formulir pengajuan rinci</b> &mdash; {@link PengajuanBeasiswa} memuat data sosial-ekonomi
 * keluarga pemohon (penghasilan, kondisi rumah, jarak ke kampus, dsb.) beserta
 * {@link KeadaanKeluargaPengajuanBeasiswa}; lihat Javadoc kelas tersebut, jangan diduplikasi di
 * sini.</li>
 * <li><b>Penetapan penerima</b> &mdash; {@link MahasiswaDapatBeasiswa}.</li>
 * <li><b>Dampak biaya</b> &mdash; {@link BeasiswaPunyaItemBiayaTambahan} menghubungkan program ke
 * {@link ItemBiaya} beserta nominal {@code jumlah}-nya.</li>
 * </ol>
 *
 * <h2>Pemetaan Hibernate</h2>
 * <p>{@code @Entity} + {@code @Table(schema = "public", name = "beasiswa")},
 * {@code dynamicInsert}/{@code dynamicUpdate} aktif (hanya kolom yang benar-benar berubah ikut
 * dalam {@code INSERT}/{@code UPDATE}), dan {@code @Audited} sehingga setiap perubahan direkam
 * Hibernate Envers ke tabel bayangan {@code beasiswa_AUD}.</p>
 * <p>Pemetaan memakai <b>property access</b>: anotasi menempel pada getter, sehingga
 * <b>setiap pasangan getter/setter yang tidak dianotasi {@code @Transient} tetap dipetakan</b>.
 * Karena {@code ais.database.hibernate.MyNamingStrategy} adalah turunan
 * {@code DefaultNamingStrategy} (nama kolom = nama properti apa adanya, tanpa konversi ke
 * {@code under_score}), properti yang tidak diberi {@code @Column} jatuh ke kolom bernama persis
 * seperti propertinya: {@code masihBuka}, {@code batasanSkkp}, {@code batasanSks},
 * {@code semester}, {@code tahunAkademik}, {@code tanggal_dirubah}. Ini penting untuk
 * {@link #getMasihBuka()} &mdash; lihat bagian "Kuirk" di bawah.</p>
 *
 * <h2>Hubungan dengan {@link GeneralValueObject}</h2>
 * <p>Kelas induk <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass}, melainkan POJO
 * abstrak biasa; Hibernate <b>tidak memetakan properti milik induk</b>. Karena itu deklarasi ulang
 * {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} di kelas ini
 * <b>bukan duplikasi yang keliru, melainkan keharusan teknis</b> &mdash; tanpa deklarasi ulang,
 * kolom-kolom tersebut tidak akan pernah dipetakan. Pola yang sama muncul di hampir semua entity
 * repo ini. Manfaat yang benar-benar diwarisi dari induk adalah kumpulan utilitas statis,
 * terutama {@link GeneralValueObject#check(Object)} untuk resolusi proxy lazy.</p>
 *
 * <h2>Pengelompokan method</h2>
 * <ol>
 * <li><b>Jejak audit</b> &mdash; {@link #getOleh()}/{@link #setOleh(String)},
 * {@link #getOlehId()}/{@link #setOlehId(String)},
 * {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan hook {@code @PreUpdate}
 * {@link #onUpdate()}.</li>
 * <li><b>Identitas &amp; deskripsi</b> &mdash; {@link #getId()}, {@link #getNama()},
 * {@link #getInstansi()} (sponsor/pemberi dana), {@link #getKeterangan()},
 * {@link #getTahun()}, {@link #getDate()}, {@link #toString()}.</li>
 * <li><b>Jendela pendaftaran</b> &mdash; {@link #getDibukaUtkMahasiswa()} (saklar utama),
 * {@link #getTanggalBuka()}, {@link #getTanggalTutup()}, {@link #getMasihBuka()} (turunan).</li>
 * <li><b>Penargetan/sasaran</b> &mdash; {@link #getFakultas()}, {@link #getJurusan()},
 * {@link #getJenjang()}, {@link #getJenisPenerimaBeasiswa()}. Ketiga yang pertama bersifat
 * "filter opsional": {@code null} berarti <i>tidak dibatasi</i>.</li>
 * <li><b>Ambang syarat kelayakan</b> &mdash; {@link #getBatasanIP()} (IPK minimal),
 * {@link #getBatasanSks()} (SKS lulus minimal), {@link #getBatasanSkkp()} (angka kredit kegiatan
 * kemahasiswaan minimal), {@link #getPenghasilanOrangTua()} (batas <i>maksimal</i> penghasilan),
 * {@link #getBolehGanda()}, {@link #getHarusBayar()}.</li>
 * <li><b>Periode akademik</b> &mdash; {@link #getTahunAkademik()}, {@link #getSemester()}.</li>
 * <li><b>Relasi biaya warisan</b> &mdash; {@link #getItemBiayas()}/{@link #setItemBiayas(Set)}.</li>
 * </ol>
 *
 * <h2>Pola "getter yang menulis balik" (penting)</h2>
 * <p>Seperti banyak entity lain di repo ini, sejumlah getter di sini <b>bukan getter polos</b>:
 * mereka mengubah state object saat dibaca. Karena entity yang dibaca dari session Hibernate
 * bersifat <i>managed</i>, perubahan itu ikut ter-{@code UPDATE} ke database pada flush berikutnya
 * <b>meskipun tidak ada layar yang secara sadar menyimpan apa pun</b>.</p>
 * <ul>
 * <li>Mengisi hanya saat {@code null}: {@link #getTanggalBuka()}, {@link #getTanggalTutup()},
 * {@link #getDibukaUtkMahasiswa()} ({@code 1}), {@link #getBatasanIP()} ({@code 0.0}),
 * {@link #getPenghasilanOrangTua()} ({@code 0L}), {@link #getHarusBayar()} ({@code false}),
 * {@link #getSemester()}, {@link #getTahunAkademik()}.</li>
 * <li><b>Selalu</b> menimpa, bukan hanya saat {@code null}: {@link #getMasihBuka()}.</li>
 * <li>Menulis balik hasil resolusi proxy: {@link #getFakultas()}, {@link #getJurusan()},
 * {@link #getJenjang()}, {@link #getJenisPenerimaBeasiswa()} &mdash; semuanya berpola
 * {@code x = check(x); return x;}.</li>
 * <li>Getter yang <b>tidak</b> menulis balik (hanya menormalkan nilai kembalian):
 * {@link #getNama()} (trim), {@link #getBatasanSks()} dan {@link #getBatasanSkkp()} (default
 * {@code 0.0} tanpa disimpan). Perhatikan asimetri {@link #getBatasanIP()} (menulis) versus
 * {@link #getBatasanSks()}/{@link #getBatasanSkkp()} (tidak menulis) &mdash; ini tidak disengaja,
 * tapi <b>dibiarkan apa adanya</b> karena perubahan perilakunya berisiko.</li>
 * </ul>
 * <p><b>Sesi Hibernate:</b> tidak ada satu pun method di kelas ini yang membuka atau menutup
 * {@code Session} secara langsung (kelas ini bahkan tidak meng-import {@code HibernateUtil}).
 * Satu-satunya akses database implisit terjadi di dalam {@link GeneralValueObject#check(Object)}
 * yang dipakai keempat getter relasi; pembukaan dan penutupan sesi penyelamat di sana sudah
 * ditangani kelas induk.</p>
 *
 * <h2>Kuirk yang perlu diketahui sebelum menyunting</h2>
 * <ul>
 * <li><b>{@code bolehGanda} artinya kebalikan dari namanya.</b> Label layar pada
 * {@code ais.action.master.BeasiswaAction} berbunyi <i>"Tidak sedang menerima beasiswa dari
 * instansi lain"</i>, dan {@code ais.action.master.beasiswa.BeasiswaUntukMahasiswaAction}
 * <b>menolak</b> pendaftaran ketika {@code getBolehGanda() == true} sementara mahasiswa sudah
 * punya beasiswa lain yang diterima. Jadi nilai {@code true} berarti "beasiswa ini TIDAK boleh
 * ganda". Lihat {@link #getBolehGanda()}.</li>
 * <li><b>{@code masihBuka} adalah kolom nyata, tapi isinya tidak bisa dipercaya.</b> Karena
 * {@link #getMasihBuka()} selalu menghitung ulang dan menimpa, nilai di database hanyalah potret
 * terakhir kali entity kebetulan dibaca lalu di-flush. <b>Jangan</b> memfilter Criteria dengan
 * {@code eq("masihBuka", true)}; kode yang ada memang tidak melakukannya &mdash;
 * {@code BeasiswaUntukMahasiswaAction.initCriteria()} hanya memfilter
 * {@code eq("dibukaUtkMahasiswa", 1)}, dan pemeriksaan "masih buka" dilakukan di lapisan UI.</li>
 * <li><b>Membaca beasiswa baru bisa "mengisi sendiri" tanggalnya.</b> Pada objek yang
 * {@code tanggalBuka}/{@code tanggalTutup}-nya masih {@code null}, memanggil
 * {@link #getMasihBuka()}, {@link #getSemester()}, atau {@link #getTahunAkademik()} akan memicu
 * {@link #getTanggalBuka()}/{@link #getTanggalTutup()} yang mengisi keduanya dengan "sekarang".
 * Akibat lanjutannya: {@link #getMasihBuka()} untuk objek semacam itu mengembalikan
 * {@code false} (lihat penjelasan di method tersebut).</li>
 * <li><b>{@link #toString()} sengaja membaca field langsung, bukan getter.</b> Itu satu-satunya
 * alasan label combobox/log tidak ikut memicu efek samping pengisian tanggal di atas. Jangan
 * "dirapikan" menjadi {@code getTanggalBuka()} dsb.</li>
 * <li><b>Tiga penanda periode yang berdiri sendiri</b> &mdash; {@code tahun} ({@link Integer}),
 * {@code tahunAkademik} ({@link String} "2026/2027"), dan {@code semester}
 * ({@link Perkuliahan#GANJIL}/{@link Perkuliahan#GENAP}). Tidak ada kode yang menjaga ketiganya
 * tetap konsisten satu sama lain.</li>
 * <li><b>Relasi {@link #getItemBiayas()} praktis mati.</b> Tidak ada satu pun pemanggil di seluruh
 * pohon sumber selain kelas ini sendiri; perannya sudah digantikan
 * {@link BeasiswaPunyaItemBiayaTambahan}. Lihat catatan di getter-nya sebelum menghapus.</li>
 * <li>Kolom untuk {@link #getDate()} bernama {@code date_} (dengan garis bawah) karena
 * {@code date} adalah kata terpesan di beberapa basis data.</li>
 * </ul>
 *
 * @see GeneralValueObject
 * @see GeneralValueObject#check(Object)
 * @see PengajuanBeasiswa
 * @see MahasiswaDapatBeasiswa
 * @see ais.database.model.beasiswa.MahasiswaDaftarBeasiswa
 * @see BeasiswaPunyaItemBiayaTambahan
 * @see JenisPenerimaBeasiswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "beasiswa")

public class Beasiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya dipatok tetap supaya sesi ZK yang di-passivate/aktifkan
	 * kembali (atau data yang dikirim antar-node) tetap kompatibel walau field kelas bertambah.
	 * Jangan diubah tanpa alasan kuat.
	 */
	private static final long serialVersionUID = 2463822577548139808L;
	/** Kunci utama baris {@code public.beasiswa}; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna pengubah terakhir (jejak audit); lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna pengubah terakhir (jejak audit); lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir; {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah terakhir.
	 *
	 * <p>Nilai {@code null} atau string kosong <b>diabaikan diam-diam</b> (method langsung
	 * {@code return}) sehingga jejak audit lama tidak tertimpa nilai hampa oleh proses batch atau
	 * importir yang tidak punya konteks pengguna.</p>
	 *
	 * <p>Umumnya diisi otomatis oleh {@code AuditTimestampInterceptor} lewat {@link #onUpdate()},
	 * bukan dipanggil manual dari layar.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong diabaikan diam-diam
	 * supaya jejak audit lama tetap utuh.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir; {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum {@code UPDATE} baris ini
	 * dieksekusi.
	 *
	 * <p>Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@code oleh}/{@code olehId} dari konteks pengguna aktif dan memperbarui
	 * {@link #getTanggal_dirubah()}. Karena hook ini hanya terikat pada {@code @PreUpdate} (bukan
	 * {@code @PrePersist}), baris yang baru pertama kali di-{@code INSERT} mengandalkan nilai awal
	 * field dan setter yang dipanggil layar penyimpan.</p>
	 *
	 * <p>Jangan panggil manual dari kode aplikasi.</p>
	 *
	 * <p>Field {@code tanggal_dirubah} sengaja diinisialisasi ke waktu "sekarang" versi kampus
	 * ({@code WaktuUtil.getDate()}, sudah dikoreksi zona waktu WIB/WITA/WIT) agar baris baru pun
	 * punya stempel waktu yang masuk akal sebelum hook ini pernah berjalan.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya diisi otomatis lewat {@link #onUpdate()}; panggilan manual hanya dipakai importir
	 * yang ingin mempertahankan waktu asal data.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Tanpa {@code @Column}, sehingga jatuh ke penamaan default {@code MyNamingStrategy}
	 * (turunan {@code DefaultNamingStrategy}) &mdash; kolom {@code tanggal_dirubah} apa adanya.</p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada objek yang dibuat lewat
	 *         konstruktor karena field-nya diinisialisasi {@code WaktuUtil.getDate()}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Label ringkas program beasiswa, berformat
	 * <code>&lt;nama&gt;-&lt;tanggal buka&gt;-&lt;tanggal tutup&gt;-&lt;instansi&gt;</code>.
	 *
	 * <p>Dipakai sebagai teks item combobox, isi log, dan nilai {@code toString()} pada entity
	 * turunan yang merangkai labelnya sendiri (mis.
	 * {@link BeasiswaPunyaItemBiayaTambahan#toString()}).</p>
	 *
	 * <p><b>Penting:</b> method ini membaca <b>field langsung</b> ({@code nama},
	 * {@code tanggalBuka}, {@code tanggalTutup}, {@code instansi}), <b>bukan</b> getter-nya. Itu
	 * disengaja: memakai {@link #getTanggalBuka()}/{@link #getTanggalTutup()} akan mengisi kedua
	 * tanggal dengan "sekarang" pada objek yang belum punya tanggal, sehingga sekadar menampilkan
	 * daftar beasiswa di layar bisa mengubah data. Jangan "dirapikan" menjadi pemanggilan getter.</p>
	 *
	 * <p>Tanggal yang {@code null} dirender sebagai string kosong; {@code nama}/{@code instansi}
	 * yang {@code null} akan muncul apa adanya sebagai teks {@code "null"} karena dirangkai lewat
	 * penggabungan string biasa.</p>
	 *
	 * @return label ringkas beasiswa untuk keperluan tampilan
	 */
	public String toString() {
		return nama + "-" + (tanggalBuka == null ? "" : Common.dateFormat.get().format(tanggalBuka)) + "-"
				+ (tanggalTutup == null ? "" : Common.dateFormat.get().format(tanggalTutup)) + "-" + instansi;
	}

	/**
	 * Kumpulan item biaya tambahan versi lama (relasi many-to-many); lihat
	 * {@link #getItemBiayas()} untuk catatan bahwa relasi ini sudah tidak dipakai.
	 */
	private Set<ItemBiaya> itemBiayas = new HashSet<ItemBiaya>();

	/**
	 * Mengembalikan kumpulan {@link ItemBiaya} yang dikaitkan ke program beasiswa ini lewat tabel
	 * perantara {@code beasiswa_has_penambahan_item_biaya}.
	 *
	 * <p><b>Relasi warisan &mdash; tidak dipakai lagi.</b> Penelusuran seluruh pohon sumber
	 * menunjukkan {@code getItemBiayas()}/{@link #setItemBiayas(Set)} <b>tidak pernah dipanggil
	 * dari mana pun</b> selain kelas ini sendiri. Perannya digantikan entity
	 * {@link BeasiswaPunyaItemBiayaTambahan} (tabel {@code beasiswa_punya_item_biaya_tambahan})
	 * yang punya kelebihan penting: menyimpan nominal {@code jumlah} dan
	 * {@code tanggalDitambahkan} per kaitan &mdash; sesuatu yang tidak bisa diwakili tabel
	 * perantara polos ini. Layar pengelolanya adalah {@code BeasiswaAction} bersama
	 * {@code AmbilDataItemBiayaHelper}.</p>
	 *
	 * <p><b>Jangan buru-buru menghapus properti ini.</b> Selama masih dipetakan, Hibernate tetap
	 * mengelola tabel perantaranya, dan {@code cascade = MERGE, PERSIST} berarti menyimpan sebuah
	 * {@code Beasiswa} juga menyimpan {@link ItemBiaya} di dalam koleksi ini. Menghapus pemetaan
	 * tanpa memindahkan/membuang data lama di {@code beasiswa_has_penambahan_item_biaya} berpotensi
	 * meninggalkan tabel yatim, dan menghapus tabelnya akan mematahkan riwayat Envers.</p>
	 *
	 * <p>Koleksi diurutkan {@code @OrderBy("nama asc")} oleh basis data saat dimuat, dan
	 * fetch-nya mengikuti default {@code @ManyToMany} (lazy).</p>
	 *
	 * @return kumpulan item biaya terkait; tidak pernah {@code null} (diinisialisasi
	 *         {@link HashSet}), tetapi biasanya kosong
	 * @see BeasiswaPunyaItemBiayaTambahan
	 */
	@ManyToMany(targetEntity = ItemBiaya.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@OrderBy(value = "nama asc")
	@JoinTable(name = "beasiswa_has_penambahan_item_biaya", joinColumns = @JoinColumn(name = "beasiswa"), inverseJoinColumns = @JoinColumn(name = "item_biaya"))
	public Set<ItemBiaya> getItemBiayas() {
		return itemBiayas;
	}

	/**
	 * Mengganti seluruh isi koleksi item biaya warisan.
	 *
	 * <p>Dipakai Hibernate saat memuat entity. Tidak ada pemanggil aplikasi &mdash; lihat catatan
	 * lengkap di {@link #getItemBiayas()}.</p>
	 *
	 * @param itemBiayas koleksi item biaya baru
	 */
	public void setItemBiayas(Set<ItemBiaya> itemBiayas) {
		this.itemBiayas = itemBiayas;
	}

	/** Nama program beasiswa; lihat {@link #getNama()}. */
	private String nama;
	/** Tanggal pencatatan/penerbitan program, kolom {@code date_}; lihat {@link #getDate()}. */
	private Date date = ais.ui.util.WaktuUtil.getDate();
	/** Nama instansi/sponsor pemberi dana; lihat {@link #getInstansi()}. */
	private String instansi;
	/** Keterangan bebas; lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Awal jendela pendaftaran; lihat {@link #getTanggalBuka()}. */
	private Date tanggalBuka;
	/** Akhir jendela pendaftaran; lihat {@link #getTanggalTutup()}. */
	private Date tanggalTutup;
	/** Cache status "masih buka" yang selalu dihitung ulang; lihat {@link #getMasihBuka()}. */
	private Boolean masihBuka;
	/** Tahun program (angka, bukan tahun akademik); lihat {@link #getTahun()}. */
	private Integer tahun;
	/** Saklar 1/0 apakah program tampil bagi mahasiswa; lihat {@link #getDibukaUtkMahasiswa()}. */
	private Integer dibukaUtkMahasiswa;
	/** Ambang IPK minimal; lihat {@link #getBatasanIP()}. */
	private Double batasanIP;
	/** Ambang angka kredit kegiatan kemahasiswaan minimal; lihat {@link #getBatasanSkkp()}. */
	private Double batasanSkkp;
	/** Ambang SKS lulus minimal; lihat {@link #getBatasanSks()}. */
	private Double batasanSks;
	/** Penanda larangan beasiswa ganda &mdash; artinya terbalik dari namanya; lihat {@link #getBolehGanda()}. */
	private Boolean bolehGanda;
	/** Batas <i>maksimal</i> penghasilan orang tua; lihat {@link #getPenghasilanOrangTua()}. */
	private Long penghasilanOrangTua;
	/** Syarat lunas biaya kuliah sebelum boleh mendaftar; lihat {@link #getHarusBayar()}. */
	private Boolean harusBayar;

	/** Pembatas sasaran fakultas ({@code null} = semua); lihat {@link #getFakultas()}. */
	private Fakultas fakultas;
	/** Pembatas sasaran jenjang ({@code null} = semua); lihat {@link #getJenjang()}. */
	private Jenjang jenjang;
	/** Pembatas sasaran program studi ({@code null} = semua); lihat {@link #getJurusan()}. */
	private Jurusan jurusan;

	/** Kategori/jenis penerima beasiswa; lihat {@link #getJenisPenerimaBeasiswa()}. */
	private JenisPenerimaBeasiswa jenisPenerimaBeasiswa;

	/** Semester berlaku ({@code Ganjil}/{@code Genap}); lihat {@link #getSemester()}. */
	private String semester;
	/** Tahun akademik berlaku, mis. {@code "2026/2027"}; lihat {@link #getTahunAkademik()}. */
	private String tahunAkademik;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA untuk membuat instance saat memuat
	 * baris dari database, sekaligus dipakai layar {@code BeasiswaAction} untuk membuat program
	 * baru.
	 *
	 * <p>Tidak melakukan apa pun secara eksplisit; nilai awal yang berarti hanya datang dari
	 * inisialisasi field ({@code date} dan {@code tanggal_dirubah} diisi waktu "sekarang" versi
	 * kampus, {@code itemBiayas} diisi {@link HashSet} kosong). Seluruh field lain bernilai
	 * {@code null} sampai diisi layar penyimpan &mdash; itulah sebabnya banyak getter di kelas ini
	 * memasang nilai default sendiri.</p>
	 */
	public Beasiswa() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Dibangkitkan basis data ({@code GenerationType.IDENTITY}) dan {@code insertable = false},
	 * jadi bernilai {@code null} pada objek baru sampai Hibernate benar-benar menyimpannya. Beberapa
	 * pemanggil memanfaatkan sifat ini sebagai penanda "belum tersimpan" &mdash; mis.
	 * {@code BeasiswaUntukMahasiswaAction} memakai {@code beasiswa.getId() != null} untuk memutuskan
	 * apakah restriksi {@code ne("beasiswa", beasiswa)} bisa dipasang.</p>
	 *
	 * @return id baris; {@code null} bila entity belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama. Normalnya hanya dipanggil Hibernate.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama program beasiswa (mis. "Bidikmisi 2026").
	 *
	 * <p>Nilai kembalian di-{@code trim()} agar spasi tepi tidak mengganggu tampilan dan
	 * pencocokan, tetapi hasil trim <b>tidak</b> ditulis balik ke field &mdash; nilai di kolom
	 * {@code nama} bisa saja tetap mengandung spasi tepi. Konsekuensinya, query yang mencocokkan
	 * nama secara persis ({@code eq("nama", ...)}) sebaiknya memakai {@code ilike} atau
	 * menormalkan sendiri, jangan mengandalkan hasil getter ini.</p>
	 *
	 * @return nama beasiswa tanpa spasi tepi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 150)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama program beasiswa. Kolom dibatasi 150 karakter dan {@code nullable = false}.
	 *
	 * @param nama nama beasiswa
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas program beasiswa (teks panjang, diisi lewat textbox 3 baris di
	 * {@code BeasiswaAction}).
	 *
	 * @return keterangan; {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas program beasiswa.
	 *
	 * @param keterangan teks keterangan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengisi tanggal pencatatan program.
	 *
	 * @param date tanggal pencatatan
	 */
	public void setDate(Date date) {
		this.date = date;
	}

	/**
	 * Mengembalikan tanggal pencatatan/penerbitan program beasiswa.
	 *
	 * <p>Berbeda dari {@link #getTanggalBuka()}/{@link #getTanggalTutup()} yang menentukan jendela
	 * pendaftaran, field ini murni tanggal administratif dan tidak dipakai logika kelayakan mana
	 * pun; di layar {@code BeasiswaAction} ia hanya muncul sebagai datebox berlabel "Tanggal".</p>
	 *
	 * <p>Kolomnya bernama {@code date_} (dengan garis bawah) karena {@code date} kata terpesan.
	 * Field-nya diinisialisasi {@code WaktuUtil.getDate()} sehingga objek baru sudah berisi waktu
	 * "sekarang" versi kampus tanpa perlu diisi layar.</p>
	 *
	 * @return tanggal pencatatan program
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "date_")
	public Date getDate() {
		return date;
	}

	/**
	 * Mengisi nama instansi/sponsor pemberi dana beasiswa.
	 *
	 * @param instansi nama instansi/sponsor/perusahaan
	 */
	public void setInstansi(String instansi) {
		this.instansi = instansi;
	}

	/**
	 * Mengembalikan nama instansi/sponsor/perusahaan pemberi dana beasiswa.
	 *
	 * <p>Berupa teks bebas, bukan relasi ke entity mana pun &mdash; tidak ada master "instansi
	 * pemberi beasiswa" di sistem ini, sehingga penulisan nama yang sama bisa bervariasi antar
	 * baris. Ikut dirangkai ke dalam {@link #toString()}.</p>
	 *
	 * @return nama instansi pemberi dana; {@code null} bila tidak diisi
	 */
	@Column(name = "instansi")
	public String getInstansi() {
		return instansi;
	}

	/**
	 * Mengembalikan awal jendela pendaftaran beasiswa.
	 *
	 * <p><b>Getter yang menulis:</b> bila {@code tanggalBuka} masih {@code null}, method ini
	 * mengisinya dengan waktu "sekarang" versi kampus ({@code WaktuUtil.getDate()}) dan menyimpan
	 * hasilnya ke field. Pada entity yang sedang <i>managed</i> oleh session Hibernate, nilai itu
	 * ikut ter-{@code UPDATE} ke kolom {@code tanggal_buka} pada flush berikutnya walaupun tidak
	 * ada yang bermaksud menyimpan apa pun.</p>
	 *
	 * <p>Getter ini dipanggil berantai dari {@link #getMasihBuka()}, {@link #getSemester()}, dan
	 * {@link #getTahunAkademik()}, sehingga efek samping di atas mudah terpicu secara tak sengaja.
	 * {@link #toString()} sengaja <i>tidak</i> memakainya.</p>
	 *
	 * <p>Juga dipakai sebagai kunci pengurutan daftar beasiswa untuk mahasiswa
	 * ({@code Order.desc("tanggalBuka")} di {@code BeasiswaUntukMahasiswaAction.initCriteria}).</p>
	 *
	 * @return awal jendela pendaftaran; tidak pernah {@code null} setelah method ini dipanggil
	 *         sekali
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_buka")
	public Date getTanggalBuka() {
		if (tanggalBuka == null) {
			tanggalBuka = ais.ui.util.WaktuUtil.getDate();
		}
		return tanggalBuka;
	}

	/**
	 * Mengisi awal jendela pendaftaran beasiswa.
	 *
	 * <p>Di layar {@code BeasiswaAction}, datebox-nya hanya bisa disunting ketika
	 * {@link #getDibukaUtkMahasiswa()} bernilai {@code 1}.</p>
	 *
	 * @param tanggalBuka awal jendela pendaftaran
	 */
	public void setTanggalBuka(Date tanggalBuka) {
		this.tanggalBuka = tanggalBuka;
	}

	/**
	 * Mengembalikan akhir jendela pendaftaran beasiswa.
	 *
	 * <p><b>Getter yang menulis</b> dengan pola dan risiko yang sama persis dengan
	 * {@link #getTanggalBuka()}: {@code null} diganti waktu "sekarang" dan disimpan ke field
	 * (kolom {@code tanggal_tutup}).</p>
	 *
	 * <p>Perhatikan akibat gabungannya pada beasiswa yang kedua tanggalnya masih kosong: keduanya
	 * terisi dengan waktu yang praktis sama, sehingga program itu langsung dianggap
	 * <b>sudah tutup</b> oleh {@link #getMasihBuka()}.</p>
	 *
	 * @return akhir jendela pendaftaran; tidak pernah {@code null} setelah method ini dipanggil
	 *         sekali
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_tutup")
	public Date getTanggalTutup() {
		if (tanggalTutup == null) {
			tanggalTutup = ais.ui.util.WaktuUtil.getDate();
		}
		return tanggalTutup;
	}

	/**
	 * Mengisi akhir jendela pendaftaran beasiswa.
	 *
	 * <p>Tidak ada validasi bahwa nilainya harus setelah {@link #getTanggalBuka()}; urutan tanggal
	 * yang terbalik hanya akan membuat {@link #getMasihBuka()} selalu {@code false}.</p>
	 *
	 * @param tanggalTutup akhir jendela pendaftaran
	 */
	public void setTanggalTutup(Date tanggalTutup) {
		this.tanggalTutup = tanggalTutup;
	}

	/**
	 * Mengembalikan tahun program beasiswa sebagai angka (mis. {@code 2026}).
	 *
	 * <p>Ini <b>bukan</b> tahun akademik &mdash; untuk itu ada {@link #getTahunAkademik()} yang
	 * berformat {@code "2026/2027"}. Keduanya diisi terpisah di layar dan tidak saling dijaga
	 * konsisten. Field ini murni informatif: dipakai kolom daftar di {@code BeasiswaAction},
	 * {@code BeasiswaUntukMahasiswaAction}, dan {@code SeleksiPenerimaBeasiswaAction}, tidak
	 * dipakai logika kelayakan mana pun.</p>
	 *
	 * <p>Tidak memasang nilai default, jadi bisa {@code null} &mdash; semua pemanggil di atas
	 * karenanya memeriksa {@code null} sebelum memanggil {@code toString()}.</p>
	 *
	 * @return tahun program; {@code null} bila tidak diisi
	 */
	@Column(name = "tahun")
	public Integer getTahun() {
		return tahun;
	}

	/**
	 * Mengisi tahun program beasiswa.
	 *
	 * @param tahun tahun program (angka)
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan saklar apakah program ini ditawarkan langsung kepada mahasiswa:
	 * {@code 1} = ya, nilai lain (atau semula {@code null}) = tidak.
	 *
	 * <p>Ini adalah <b>saklar utama visibilitas</b> program. {@code BeasiswaUntukMahasiswaAction}
	 * memfilter daftarnya dengan {@code Restrictions.eq("dibukaUtkMahasiswa", 1)}, dan
	 * {@code PengajuanBeasiswaAction} menggabungkannya dengan {@link #getMasihBuka()} untuk
	 * menentukan apakah tombol pengajuan boleh aktif. Di layar admin, saklar ini juga
	 * mengaktif/menonaktifkan seluruh kelompok kolom syarat (tanggal buka/tutup, IPK, SKS, SKKP,
	 * beasiswa ganda, penghasilan orang tua).</p>
	 *
	 * <p><b>Getter yang menulis:</b> nilai {@code null} diganti {@code 1} dan disimpan ke field
	 * &mdash; artinya membaca beasiswa lama yang kolomnya masih {@code NULL} akan <b>membukanya
	 * untuk mahasiswa</b> begitu entity ter-flush. Karena itu pemanggil yang berhati-hati
	 * ({@code BeasiswaAction}) tetap menulis pemeriksaan {@code getDibukaUtkMahasiswa() == null ||
	 * getDibukaUtkMahasiswa() != 1} walaupun secara teori getter ini tidak pernah mengembalikan
	 * {@code null}.</p>
	 *
	 * <p>Bertipe {@link Integer} dan bukan {@link Boolean} semata-mata karena warisan skema lama;
	 * satu-satunya nilai bermakna yang dipakai kode adalah {@code 1} dan "bukan 1".</p>
	 *
	 * @return {@code 1} bila program dibuka untuk mahasiswa, selain itu tidak dibuka
	 */
	@Column(name = "dibuka_untuk_mahasiswa")
	public Integer getDibukaUtkMahasiswa() {
		if (dibukaUtkMahasiswa == null) {
			dibukaUtkMahasiswa = 1;
		}
		return dibukaUtkMahasiswa;
	}

	/**
	 * Mengisi saklar visibilitas program bagi mahasiswa ({@code 1} = dibuka).
	 *
	 * @param dibukaUtkMahasiswa {@code 1} untuk membuka, nilai lain untuk menutup
	 */
	public void setDibukaUtkMahasiswa(Integer dibukaUtkMahasiswa) {
		this.dibukaUtkMahasiswa = dibukaUtkMahasiswa;
	}

	/**
	 * Mengembalikan ambang <b>IPK minimal</b> yang harus dicapai mahasiswa untuk boleh mendaftar.
	 *
	 * <p>Nilai {@code 0.0} (juga apa pun {@code <= 0.01}) berarti <b>syarat IPK tidak dipakai</b>:
	 * {@code BeasiswaUntukMahasiswaAction} baru menampilkan dan mengevaluasi baris syarat IPK bila
	 * {@code getBatasanIP() > 0.01}, lalu menilai {@code IPK >= getBatasanIP()}.</p>
	 *
	 * <p><b>Getter yang menulis:</b> {@code null} diganti {@code 0.0} dan disimpan ke field.
	 * Bandingkan dengan {@link #getBatasanSks()} dan {@link #getBatasanSkkp()} yang mengembalikan
	 * {@code 0.0} <b>tanpa</b> menulis balik &mdash; asimetri lama yang dibiarkan apa adanya.</p>
	 *
	 * @return ambang IPK minimal; {@code 0.0} bila syarat IPK tidak dipakai
	 */
	@Column(name = "batasan_ip")
	public Double getBatasanIP() {
		if (batasanIP == null) {
			batasanIP = 0.0;
		}
		return batasanIP;
	}

	/**
	 * Mengisi ambang IPK minimal. Isi {@code 0.0} untuk mematikan syarat ini.
	 *
	 * @param batasanIP ambang IPK minimal
	 */
	public void setBatasanIP(Double batasanIP) {
		this.batasanIP = batasanIP;
	}

	/**
	 * Mengembalikan penanda aturan beasiswa ganda.
	 *
	 * <p><b>PERHATIAN &mdash; artinya kebalikan dari namanya.</b> Meski bernama
	 * {@code bolehGanda}, nilai {@code true} berarti mahasiswa <b>tidak boleh</b> sedang menerima
	 * beasiswa lain. Dua bukti dari kode pemanggil:</p>
	 * <ul>
	 * <li>{@code BeasiswaAction} memberi checkbox ini label
	 * <i>"Tidak sedang menerima beasiswa dari instansi lain"</i>;</li>
	 * <li>{@code BeasiswaUntukMahasiswaAction} <b>menolak</b> pendaftaran bila
	 * {@code getBolehGanda() == true} sementara mahasiswa sudah punya baris
	 * {@code MahasiswaDaftarBeasiswa} lain dengan {@code terima = 1}, dengan pesan "beasiswa ini
	 * tidak diperbolehkan menerima lebih dari satu beasiswa".</li>
	 * </ul>
	 * <p>Pemeriksaan itu sendiri baru berjalan bila konfigurasi
	 * {@code jika_sudah_dapat_beasiswa_mahasiswa_tidak_boleh_mengajukan_beasiswa} (atau varian
	 * "...dalam_satu_tahun") diaktifkan. Nama field ini sengaja <b>tidak diubah</b> karena sudah
	 * menjadi nama kolom {@code boleh_ganda} dan terekam di riwayat Envers.</p>
	 *
	 * <p>Berbeda dari kebanyakan getter di kelas ini, method ini <b>tidak</b> memasang nilai
	 * default, jadi bisa mengembalikan {@code null}. {@code BeasiswaAction} sudah menjaganya
	 * ({@code getBolehGanda() != null &amp;&amp; getBolehGanda() == true}), tetapi
	 * {@code BeasiswaUntukMahasiswaAction} menulis {@code getBolehGanda() == true} tanpa penjagaan
	 * &mdash; perbandingan {@link Boolean} dengan literal {@code boolean} memicu <i>unboxing</i>,
	 * sehingga baris beasiswa lama yang kolom {@code boleh_ganda}-nya masih {@code NULL} berpotensi
	 * melempar {@code NullPointerException} di jalur pendaftaran. Dicatat apa adanya; perbaikannya
	 * ada di kode pemanggil, bukan di kelas ini.</p>
	 *
	 * @return {@code true} bila program melarang penerimaan beasiswa ganda, {@code false}/
	 *         {@code null} bila tidak diatur
	 */
	@Column(name = "boleh_ganda")
	public Boolean getBolehGanda() {
		return bolehGanda;
	}

	/**
	 * Mengisi penanda aturan beasiswa ganda. Ingat pembalikan makna yang dijelaskan di
	 * {@link #getBolehGanda()}: {@code true} berarti <i>melarang</i> beasiswa ganda.
	 *
	 * @param bolehGanda {@code true} untuk melarang penerimaan beasiswa lain
	 */
	public void setBolehGanda(Boolean bolehGanda) {
		this.bolehGanda = bolehGanda;
	}

	/**
	 * Mengembalikan <b>batas maksimal</b> penghasilan orang tua agar mahasiswa dianggap layak.
	 *
	 * <p>Meski namanya netral, ini adalah plafon: {@code BeasiswaUntukMahasiswaAction} hanya
	 * mengevaluasi syarat ini bila {@code getPenghasilanOrangTua() > 0L}, dan label layarnya
	 * berbunyi "Batas maksimal penghasilan orang tua". Nilai {@code 0} = syarat tidak dipakai.</p>
	 *
	 * <p><b>Kuirk pada evaluasinya (di kode pemanggil, bukan di sini):</b>
	 * {@code BeasiswaUntukMahasiswaAction} mengambil batas <i>atas</i> rentang pendapatan orang tua
	 * mahasiswa ({@code PendapatanOrangTua.getSampai()}) lalu menyatakan "memenuhi syarat" bila
	 * {@code sampai >= getPenghasilanOrangTua()} &mdash; arah pembandingan yang justru meloloskan
	 * mahasiswa yang penghasilan orang tuanya <i>melebihi</i> plafon dan menggugurkan yang di
	 * bawahnya. Selain itu, mahasiswa tanpa data {@code PendapatanOrangTua} mendapat
	 * {@code sampai = 0} sehingga otomatis dinyatakan tidak memenuhi syarat. Dicatat apa adanya;
	 * tidak diperbaiki dari kelas ini.</p>
	 *
	 * <p><b>Getter yang menulis:</b> {@code null} diganti {@code 0L} dan disimpan ke field.</p>
	 *
	 * @return batas maksimal penghasilan orang tua dalam rupiah; {@code 0} bila syarat tidak
	 *         dipakai
	 */
	@Column(name = "penghasilan_orang_tua")
	public Long getPenghasilanOrangTua() {
		if (penghasilanOrangTua == null) {
			penghasilanOrangTua = 0L;
		}
		return penghasilanOrangTua;
	}

	/**
	 * Mengisi batas maksimal penghasilan orang tua. Isi {@code 0} untuk mematikan syarat ini.
	 *
	 * @param penghasilanOrangTua batas maksimal penghasilan orang tua
	 */
	public void setPenghasilanOrangTua(Long penghasilanOrangTua) {
		this.penghasilanOrangTua = penghasilanOrangTua;
	}

	/**
	 * Menghitung apakah jendela pendaftaran beasiswa ini sedang terbuka <i>saat ini</i>.
	 *
	 * <p>Rumusnya: {@code sekarang} berada <b>setelah</b> {@link #getTanggalBuka()} <b>dan</b>
	 * <b>sebelum</b> {@link #getTanggalTutup()}, dengan "sekarang" diambil dari
	 * {@code WaktuUtil.getDate()} (waktu server yang sudah dikoreksi ke zona waktu kampus). Kedua
	 * batas bersifat <i>eksklusif</i>.</p>
	 *
	 * <p><b>Getter yang SELALU menulis</b> &mdash; bukan hanya saat {@code null}. Field
	 * {@code masihBuka} ditimpa hasil hitungan setiap kali method ini dipanggil, dan karena
	 * pasangan getter/setter ini <b>ikut dipetakan Hibernate</b> (tanpa {@code @Column} dan tanpa
	 * {@code @Transient}, sehingga jatuh ke kolom bernama {@code masihBuka} sesuai
	 * {@code MyNamingStrategy}), nilai itu bisa ikut ter-{@code UPDATE} ke database pada flush
	 * berikutnya.</p>
	 *
	 * <p><b>Jangan memakai kolom {@code masihBuka} sebagai filter query.</b> Isinya hanyalah
	 * potret terakhir kali entity kebetulan dibaca lalu ter-flush, bukan status sebenarnya. Kode
	 * yang ada memang tidak melakukannya: {@code BeasiswaUntukMahasiswaAction.initCriteria()}
	 * memfilter {@code dibukaUtkMahasiswa} saja, dan {@code PengajuanBeasiswaAction} memanggil
	 * {@code getMasihBuka()} di lapisan UI (digabung dengan
	 * {@code getDibukaUtkMahasiswa().equals(1)}) untuk menentukan apakah pengajuan boleh dilakukan.</p>
	 *
	 * <p><b>Efek berantai:</b> karena memanggil {@link #getTanggalBuka()} dan
	 * {@link #getTanggalTutup()}, method ini juga mengisi kedua tanggal yang masih {@code null}
	 * dengan waktu sekarang. Pada beasiswa yang belum diberi tanggal, itu berarti
	 * {@code tanggalBuka} dan {@code tanggalTutup} terisi nyaris bersamaan dan method ini
	 * mengembalikan {@code false} &mdash; "belum/tidak buka". Aman sebagai default, tapi perlu
	 * disadari bahwa sekadar <i>membaca</i> status sudah mengubah data program.</p>
	 *
	 * @return {@code true} bila saat ini berada di dalam jendela pendaftaran; {@code false} bila
	 *         di luar jendela atau tanggalnya belum pernah diisi
	 */
	public Boolean getMasihBuka() {
		masihBuka = ais.ui.util.WaktuUtil.getDate().after(getTanggalBuka())
				&& ais.ui.util.WaktuUtil.getDate().before(getTanggalTutup());
		return masihBuka;
	}

	/**
	 * Mengisi cache status "masih buka".
	 *
	 * <p>Praktis tidak berguna sebagai API: nilainya akan langsung ditimpa pada pemanggilan
	 * {@link #getMasihBuka()} berikutnya. Setter ini ada semata-mata agar Hibernate bisa memuat
	 * kolom {@code masihBuka} saat membaca baris.</p>
	 *
	 * @param masihBuka status yang akan disimpan (akan segera ditimpa hasil hitung ulang)
	 */
	public void setMasihBuka(Boolean masihBuka) {
		this.masihBuka = masihBuka;
	}

	/**
	 * Mengembalikan apakah mahasiswa <b>wajib sudah melunasi biaya perkuliahan</b> sebelum boleh
	 * mendaftar beasiswa ini.
	 *
	 * <p>Bila {@code true}, {@code BeasiswaUntukMahasiswaAction} menghitung semester berjalan
	 * mahasiswa dari {@link #getTahunAkademik()} + {@link #getSemester()} lewat
	 * {@code Common.getSemester(...)}, lalu memanggil
	 * {@code Common.checkStatusPembayaranMahasiswa(...)}; bila belum lunas, pendaftaran ditolak
	 * dengan pesan yang menyebut NIM dan semester bersangkutan. Perhatikan bahwa syarat ini
	 * bergantung pada {@link #getSemester()}/{@link #getTahunAkademik()} yang sendiri bisa terisi
	 * otomatis &mdash; lihat Javadoc kedua getter tersebut.</p>
	 *
	 * <p><b>Getter yang menulis:</b> {@code null} diganti {@code false} dan disimpan ke field.
	 * Berkat itu, {@code BeasiswaAction} bisa memanggil {@code harusBayar.setChecked(
	 * beasiswa.getHarusBayar())} tanpa penjagaan {@code null}.</p>
	 *
	 * @return {@code true} bila kelunasan biaya kuliah menjadi prasyarat pendaftaran
	 */
	public Boolean getHarusBayar() {
		if (harusBayar == null) {
			harusBayar = false;
		}
		return harusBayar;
	}

	/**
	 * Mengisi syarat kelunasan biaya perkuliahan.
	 *
	 * @param harusBayar {@code true} bila mahasiswa harus sudah melunasi biaya kuliah
	 */
	public void setHarusBayar(Boolean harusBayar) {
		this.harusBayar = harusBayar;
	}

	/**
	 * Mengisi pembatas sasaran program studi.
	 *
	 * @param jurusan program studi sasaran; {@code null} berarti tidak dibatasi program studi
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan program studi yang menjadi sasaran beasiswa ini.
	 *
	 * <p>Bersifat <b>filter opsional</b>: {@code null} berarti program terbuka untuk semua program
	 * studi. {@code BeasiswaUntukMahasiswaAction.initCriteria()} menerjemahkannya menjadi
	 * {@code Restrictions.or(isNull("jurusan"), eq("jurusan", jurusanMahasiswa))}, jadi
	 * <b>jangan</b> menulis filter {@code eq("jurusan", ...)} saja &mdash; beasiswa umum akan
	 * hilang dari daftar mahasiswa.</p>
	 *
	 * <p>Relasi lazy; getter ini memanggil {@link GeneralValueObject#check(Object)} untuk
	 * menyelesaikan proxy dan <b>menugaskan hasilnya kembali ke field</b>. Bila proxy sudah lepas
	 * dari session-nya, {@code check()} dapat mengambil ulang entity dari cache atau bahkan
	 * membuka session baru; penutupannya ditangani kelas induk. Objek yang dikembalikan karena itu
	 * belum tentu instance yang sama dengan yang tersimpan sebelumnya.</p>
	 *
	 * @return program studi sasaran; {@code null} bila beasiswa tidak dibatasi program studi
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan_id", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Mengisi pembatas sasaran fakultas.
	 *
	 * @param fakultas fakultas sasaran; {@code null} berarti tidak dibatasi fakultas
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Mengembalikan fakultas yang menjadi sasaran beasiswa ini.
	 *
	 * <p>Filter opsional dengan pola dan peringatan yang sama seperti {@link #getJurusan()}:
	 * {@code null} = semua fakultas, dan query harus ditulis
	 * {@code or(isNull("fakultas"), eq("fakultas", fakultasMahasiswa))}. Ketiga pembatas
	 * (fakultas, jurusan, jenjang) dipasang bersamaan dengan operator AND antar-klausanya,
	 * sehingga mengisi lebih dari satu berarti mahasiswa harus cocok pada semuanya.</p>
	 *
	 * <p>Relasi lazy; hasil {@link GeneralValueObject#check(Object)} ditulis balik ke field.</p>
	 *
	 * @return fakultas sasaran; {@code null} bila beasiswa tidak dibatasi fakultas
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas_id", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * Mengembalikan kategori/jenis penerima beasiswa (mis. berprestasi, kurang mampu, hafiz).
	 *
	 * <p>Berbeda dari fakultas/jurusan/jenjang yang berperan sebagai filter kelayakan, relasi ini
	 * murni penggolongan program dan wajib diisi di layar admin (label "Jenis Beasiswa *"), dengan
	 * combobox yang hanya menampilkan {@link JenisPenerimaBeasiswa} yang {@code aktif} atau
	 * {@code aktif IS NULL}. Dipakai laporan dan pemetaan pelaporan eksternal (EPSBED/EMIS).</p>
	 *
	 * <p>Relasi lazy; hasil {@link GeneralValueObject#check(Object)} ditulis balik ke field.</p>
	 *
	 * @return jenis penerima beasiswa; {@code null} bila belum diisi (kolom {@code nullable})
	 * @see JenisPenerimaBeasiswa
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_penerima_beasiswa", nullable = true)
	public JenisPenerimaBeasiswa getJenisPenerimaBeasiswa() {
		jenisPenerimaBeasiswa = check(jenisPenerimaBeasiswa);
		return jenisPenerimaBeasiswa;
	}

	/**
	 * Mengisi kategori/jenis penerima beasiswa.
	 *
	 * @param jenisPenerimaBeasiswa jenis penerima beasiswa
	 */
	public void setJenisPenerimaBeasiswa(JenisPenerimaBeasiswa jenisPenerimaBeasiswa) {
		this.jenisPenerimaBeasiswa = jenisPenerimaBeasiswa;
	}

	/**
	 * Mengembalikan ambang <b>angka kredit kegiatan kemahasiswaan (SKKP)</b> minimal.
	 *
	 * <p>Label layarnya "Angka Kredit Kegiatan Kemahasiswaan &gt;=".
	 * {@code BeasiswaUntukMahasiswaAction} baru mengevaluasi syarat ini bila nilainya
	 * {@code > 0.01}, lalu menilai {@code angkaKredit >= getBatasanSkkp()}. Nilai {@code 0.0}
	 * berarti syarat tidak dipakai.</p>
	 *
	 * <p><b>Berbeda dari {@link #getBatasanIP()}, getter ini TIDAK menulis balik</b>: nilai
	 * {@code 0.0} hanya dikembalikan, field dan kolom tetap {@code NULL}. Pasangan getter/setter
	 * ini juga tidak dianotasi {@code @Column}, sehingga kolomnya bernama {@code batasanSkkp} apa
	 * adanya sesuai {@code MyNamingStrategy}.</p>
	 *
	 * @return ambang angka kredit kegiatan kemahasiswaan minimal; {@code 0.0} bila tidak diisi
	 */
	public Double getBatasanSkkp() {
		return batasanSkkp == null ? 0.0 : batasanSkkp;
	}

	/**
	 * Mengisi ambang angka kredit kegiatan kemahasiswaan minimal. Isi {@code 0.0} untuk mematikan
	 * syarat ini.
	 *
	 * @param batasanSkkp ambang SKKP minimal
	 */
	public void setBatasanSkkp(Double batasanSkkp) {
		this.batasanSkkp = batasanSkkp;
	}

	/**
	 * Mengembalikan ambang <b>total SKS lulus</b> minimal yang harus dimiliki mahasiswa.
	 *
	 * <p>{@code BeasiswaUntukMahasiswaAction} mengevaluasinya bila nilainya {@code > 0.1}, dengan
	 * perbandingan {@code sks >= getBatasanSks().intValue()} &mdash; perhatikan pemotongan ke
	 * bilangan bulat di sisi pemanggil, sehingga ambang {@code 100.7} efektif berarti
	 * {@code 100}.</p>
	 *
	 * <p>Seperti {@link #getBatasanSkkp()} dan tidak seperti {@link #getBatasanIP()}, getter ini
	 * <b>tidak</b> menulis balik nilai default {@code 0.0} ke field. Tanpa {@code @Column},
	 * kolomnya bernama {@code batasanSks}.</p>
	 *
	 * @return ambang SKS lulus minimal; {@code 0.0} bila syarat tidak dipakai
	 */
	public Double getBatasanSks() {
		return batasanSks == null ? 0.0 : batasanSks;
	}

	/**
	 * Mengisi ambang total SKS lulus minimal. Isi {@code 0.0} untuk mematikan syarat ini.
	 *
	 * @param batasanSks ambang SKS minimal
	 */
	public void setBatasanSks(Double batasanSks) {
		this.batasanSks = batasanSks;
	}

	/**
	 * Mengembalikan semester berlakunya program beasiswa
	 * ({@link Perkuliahan#GANJIL}/{@link Perkuliahan#GENAP}).
	 *
	 * <p><b>Getter yang menulis:</b> bila {@code semester} masih {@code null}, nilainya
	 * <i>ditebak</i> dari {@link #getTanggalBuka()} lewat
	 * {@code Common.isNowSemensterGanjil(tanggal)} (perhatikan salah eja "Semenster" pada nama
	 * method utilitas itu &mdash; ejaan tersebut memang dipakai di seluruh repo) lalu disimpan ke
	 * field. Efek berantainya: memanggil getter ini pada beasiswa yang belum punya tanggal buka
	 * juga akan <b>mengisi {@code tanggalBuka} dengan waktu sekarang</b>.</p>
	 *
	 * <p>Bersama {@link #getTahunAkademik()}, nilai ini dipakai
	 * {@code BeasiswaUntukMahasiswaAction} untuk menghitung semester berjalan mahasiswa saat
	 * memeriksa syarat {@link #getHarusBayar()}. Tanpa {@code @Column}, kolomnya bernama
	 * {@code semester}.</p>
	 *
	 * @return {@link Perkuliahan#GANJIL} atau {@link Perkuliahan#GENAP}; tidak pernah {@code null}
	 *         setelah method ini dipanggil sekali
	 */
	public String getSemester() {
		if (semester == null) {
			semester = Common.isNowSemensterGanjil(getTanggalBuka()) ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
		}
		return semester;
	}

	/**
	 * Mengisi semester berlaku program beasiswa.
	 *
	 * <p>Di layar admin nilainya dipilih dari combobox berisi {@link Perkuliahan#GANJIL} dan
	 * {@link Perkuliahan#GENAP}; isi dengan salah satu konstanta tersebut, bukan teks bebas.</p>
	 *
	 * @param semester {@link Perkuliahan#GANJIL} atau {@link Perkuliahan#GENAP}
	 */
	public void setSemester(String semester) {
		this.semester = semester;
	}

	/**
	 * Mengembalikan tahun akademik berlakunya program beasiswa, mis. {@code "2026/2027"}.
	 *
	 * <p><b>Getter yang menulis:</b> bila masih {@code null}, nilainya dihitung dari
	 * {@link #getTanggalBuka()} lewat {@code Common.getCurrentTahunAkademik(tanggal)} lalu disimpan
	 * ke field &mdash; dengan efek berantai yang sama seperti {@link #getSemester()} (tanggal buka
	 * ikut terisi bila sebelumnya kosong).</p>
	 *
	 * <p>Jangan dikacaukan dengan {@link #getTahun()} yang bertipe {@link Integer} dan diisi
	 * terpisah oleh admin; tidak ada mekanisme yang menjaga keduanya konsisten. Tanpa
	 * {@code @Column}, kolomnya bernama {@code tahunAkademik}.</p>
	 *
	 * @return tahun akademik berformat {@code "&lt;tahun&gt;/&lt;tahun+1&gt;"}; tidak pernah {@code null}
	 *         setelah method ini dipanggil sekali
	 */
	public String getTahunAkademik() {
		if (tahunAkademik == null) {
			tahunAkademik = Common.getCurrentTahunAkademik(getTanggalBuka());
		}
		return tahunAkademik;
	}

	/**
	 * Mengisi tahun akademik berlaku program beasiswa.
	 *
	 * @param tahunAkademik tahun akademik berformat {@code "&lt;tahun&gt;/&lt;tahun+1&gt;"}
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan jenjang pendidikan yang menjadi sasaran beasiswa ini (S1, S2, D3, dsb.).
	 *
	 * <p>Filter opsional dengan pola dan peringatan yang sama seperti {@link #getJurusan()} dan
	 * {@link #getFakultas()}: {@code null} = semua jenjang, dan query harus ditulis
	 * {@code or(isNull("jenjang"), eq("jenjang", jenjangMahasiswa))}. Jenjang mahasiswa diambil
	 * lewat {@code mahasiswa.getJurusan().getJenjang()}, bukan dari field jenjang milik mahasiswa
	 * secara langsung.</p>
	 *
	 * <p>Relasi lazy; hasil {@link GeneralValueObject#check(Object)} ditulis balik ke field.</p>
	 *
	 * @return jenjang sasaran; {@code null} bila beasiswa tidak dibatasi jenjang
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenjang", nullable = true)
	public Jenjang getJenjang() {
		jenjang = check(jenjang);
		return jenjang;
	}

	/**
	 * Mengisi pembatas sasaran jenjang pendidikan.
	 *
	 * @param jenjang jenjang sasaran; {@code null} berarti tidak dibatasi jenjang
	 */
	public void setJenjang(Jenjang jenjang) {
		this.jenjang = jenjang;
	}
}
